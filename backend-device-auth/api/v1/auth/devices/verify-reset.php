<?php
declare(strict_types=1);

/**
 * POST /auth/devices/verify-reset
 * Auth: none.
 *
 * Only changes the password if the signature verifies against the stored
 * public key for (email, deviceId), over the exact challenge issued by
 * /challenge, and that challenge is unexpired and single-use. A "trusted"
 * response from /challenge is never treated as permission by itself — this
 * signature check is the only thing that can actually trigger a password
 * change.
 */

require_once __DIR__ . '/../../../../lib/bootstrap.php';
['config' => $config, 'pdo' => $pdo] = app_bootstrap();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_error('Method not allowed', 405);
}

function fail_verification(): void
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
$newPassword = (string) ($body['newPassword'] ?? '');

if ($email === '' || $deviceId === '' || $challenge === '' || $signatureB64 === '' || $newPassword === '') {
    fail_verification();
}
if (strlen($newPassword) < 6) {
    // Firebase Auth's own minimum password length.
    fail_verification();
}

$rl = $config['rate_limit']['verify_reset'];
$bucketKey = 'verify:' . client_ip() . ':' . $email;
if (!check_rate_limit($pdo, $bucketKey, $rl['max'], $rl['window'])) {
    fail_verification();
}

$stmt = $pdo->prepare(
    'SELECT id, public_key FROM trusted_devices
     WHERE email = :email AND device_id = :device_id AND is_active = 1
     LIMIT 1'
);
$stmt->execute([':email' => $email, ':device_id' => $deviceId]);
$device = $stmt->fetch();
if (!$device) {
    fail_verification();
}

// Atomically consume the challenge: this UPDATE only affects a row that is
// still unused and unexpired, so rowCount() === 1 doubles as the check.
$stmt = $pdo->prepare(
    'UPDATE device_challenges
     SET used = 1
     WHERE trusted_device_id = :id AND challenge = :challenge AND used = 0 AND expires_at > NOW()'
);
$stmt->execute([':id' => $device['id'], ':challenge' => $challenge]);
if ($stmt->rowCount() !== 1) {
    fail_verification();
}

// Signature must be over the exact challenge STRING as issued (the Android
// client signs challenge.toByteArray(Charsets.UTF_8) — see
// DeviceCredentialManager.signChallenge), not the raw decoded bytes.
if (!verify_ecdsa_signature($device['public_key'], $challenge, $signatureB64)) {
    fail_verification();
}

// Only now — signature confirmed — touch Firebase.
try {
    $auth = get_firebase_auth($config);
    $userRecord = $auth->getUserByEmail($email);
    $auth->changeUserPassword($userRecord->uid, $newPassword);
    $signInToken = $auth->createCustomToken($userRecord->uid)->toString();
} catch (\Throwable $e) {
    error_log('[device-auth/verify-reset] Firebase error: ' . $e->getMessage());
    fail_verification();
}

json_response(['success' => true, 'signInToken' => $signInToken]);
