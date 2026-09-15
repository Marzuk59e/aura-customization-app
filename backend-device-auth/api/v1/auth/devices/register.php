<?php
declare(strict_types=1);

/**
 * POST /auth/devices/register
 * Auth: required — Authorization: Bearer <FirebaseIdToken>
 *
 * Stores this device's public key against the caller's uid/email so a
 * later /challenge + /verify-reset can trust it. Never stores anything but
 * the public key — no private key, no biometric data, ever reaches here.
 */

require_once __DIR__ . '/../../../../lib/bootstrap.php';
['config' => $config, 'pdo' => $pdo] = app_bootstrap();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_error('Method not allowed', 405);
}

$token = bearer_token();
if ($token === null) {
    json_error('Unauthorized', 401);
}

$claims = verify_firebase_token($token, $config);
if ($claims === null || $claims['uid'] === '') {
    json_error('Unauthorized', 401);
}
$uid = $claims['uid'];
$email = $claims['email'];

$body = read_json_body();
$deviceId = trim((string) ($body['deviceId'] ?? ''));
$publicKey = trim((string) ($body['publicKey'] ?? ''));
$deviceLabel = trim((string) ($body['deviceLabel'] ?? ''));
$algorithm = trim((string) ($body['algorithm'] ?? 'SHA256withECDSA'));

if ($deviceId === '' || $publicKey === '') {
    json_error('deviceId and publicKey are required', 400);
}
if ($deviceLabel === '') {
    $deviceLabel = 'Unknown device';
}

// Reject obviously-bad keys up front rather than storing garbage.
$pem = pem_from_base64_spki($publicKey);
if ($pem === null || openssl_pkey_get_public($pem) === false) {
    json_error('Invalid public key', 400);
}

$stmt = $pdo->prepare(
    'INSERT INTO trusted_devices (uid, email, device_id, public_key, device_label, algorithm, is_active, created_at)
     VALUES (:uid, :email, :device_id, :public_key, :device_label, :algorithm, 1, NOW())
     ON DUPLICATE KEY UPDATE
        public_key   = VALUES(public_key),
        device_label = VALUES(device_label),
        algorithm    = VALUES(algorithm),
        is_active    = 1,
        updated_at   = NOW()'
);
$stmt->execute([
    ':uid'          => $uid,
    ':email'        => $email,
    ':device_id'    => $deviceId,
    ':public_key'   => $publicKey,
    ':device_label' => $deviceLabel,
    ':algorithm'    => $algorithm,
]);

json_response(['registered' => true, 'deviceId' => $deviceId]);
