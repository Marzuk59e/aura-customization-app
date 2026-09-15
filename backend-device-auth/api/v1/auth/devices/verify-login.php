<?php
declare(strict_types=1);

/**
 * POST /auth/devices/verify-login
 * Auth: none.
 *
 * Lighter sibling of verify-reset.php: verifies the signature the same way
 * (stored public key for (email, deviceId), exact challenge from /challenge,
 * single-use + unexpired) but never touches the password — it only mints a
 * Firebase custom token so the app can sign the user straight in. Used by
 * the forgot-password flow now that a successful device verification signs
 * the user in directly instead of asking for a new password.
 */

require_once __DIR__ . '/../../../../lib/bootstrap.php';
['config' => $config, 'pdo' => $pdo] = app_bootstrap();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_error('Method not allowed', 405);
}

function fail_login_verification(): void
{
    // Deliberately generic — never leak which specific check failed.
    json_response([
        'success' => false,
        'message' => 'Device verification was unsuccessful. Please try again.',
    ]);
}

$body = read_json_body();
$email = strtolower(trim((string) ($body['email'] ?? '')));
$deviceId = trim((string) ($body['deviceId'] ?? ''));
$challenge = (string) ($body['challenge'] ?? '');
$signatureB64 = (string) ($body['signature'] ?? '');

if ($email === '' || $deviceId === '' || $challenge === '' || $signatureB64 === '') {
    fail_login_verification();
}

$rl = $config['rate_limit']['verify_reset']; // same bucket/limits as verify-reset
$bucketKey = 'verify-login:' . client_ip() . ':' . $email;
if (!check_rate_limit($pdo, $bucketKey, $rl['max'], $rl['window'])) {
    fail_login_verification();
}

$stmt = $pdo->prepare(
    'SELECT id, uid, public_key FROM trusted_devices
     WHERE email = :email AND device_id = :device_id AND is_active = 1
     LIMIT 1'
);
$stmt->execute([':email' => $email, ':device_id' => $deviceId]);
$device = $stmt->fetch();
if (!$device) {
    fail_login_verification();
}

// Atomically consume the challenge — same one-time-use guarantee as verify-reset.
$stmt = $pdo->prepare(
    'UPDATE device_challenges
     SET used = 1
     WHERE trusted_device_id = :id AND challenge = :challenge AND used = 0 AND expires_at > NOW()'
);
$stmt->execute([':id' => $device['id'], ':challenge' => $challenge]);
if ($stmt->rowCount() !== 1) {
    fail_login_verification();
}

if (!verify_ecdsa_signature($device['public_key'], $challenge, $signatureB64)) {
    fail_login_verification();
}

// Signature confirmed — no password change, just mint a sign-in token.
try {
    $auth = get_firebase_auth($config);
    $signInToken = $auth->createCustomToken($device['uid'])->toString();
} catch (\Throwable $e) {
    error_log('[device-auth/verify-login] Firebase error: ' . $e->getMessage());
    fail_login_verification();
}

json_response(['success' => true, 'signInToken' => $signInToken]);
