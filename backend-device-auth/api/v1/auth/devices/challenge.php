<?php
declare(strict_types=1);

/**
 * POST /auth/devices/challenge
 * Auth: none (user is logged out at this point by definition).
 *
 * Looks up whether an active device registration exists for
 * (email, deviceId). Never reveals whether the email itself exists —
 * "not trusted" is the same response whether the email is unknown, the
 * device was never registered, or it was deactivated.
 */

require_once __DIR__ . '/../../../../lib/bootstrap.php';
['config' => $config, 'pdo' => $pdo] = app_bootstrap();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_error('Method not allowed', 405);
}

$body = read_json_body();
$email = strtolower(trim((string) ($body['email'] ?? '')));
$deviceId = trim((string) ($body['deviceId'] ?? ''));

if ($email === '' || $deviceId === '') {
    json_response(['deviceTrusted' => false]);
}

$rl = $config['rate_limit']['challenge'];
$bucketKey = 'challenge:' . client_ip() . ':' . $email;
if (!check_rate_limit($pdo, $bucketKey, $rl['max'], $rl['window'])) {
    json_response(['deviceTrusted' => false]);
}

$stmt = $pdo->prepare(
    'SELECT id FROM trusted_devices
     WHERE email = :email AND device_id = :device_id AND is_active = 1
     LIMIT 1'
);
$stmt->execute([':email' => $email, ':device_id' => $deviceId]);
$device = $stmt->fetch();

if (!$device) {
    json_response(['deviceTrusted' => false]);
}

$ttl = (int) ($config['challenge_ttl_seconds'] ?? 180);
$challenge = base64_encode(random_bytes(32));

// A device should only ever have one live challenge at a time.
$pdo->prepare('DELETE FROM device_challenges WHERE trusted_device_id = :id AND used = 0')
    ->execute([':id' => $device['id']]);

$stmt = $pdo->prepare(
    'INSERT INTO device_challenges (trusted_device_id, challenge, used, expires_at, created_at)
     VALUES (:id, :challenge, 0, DATE_ADD(NOW(), INTERVAL :ttl SECOND), NOW())'
);
$stmt->execute([
    ':id'        => $device['id'],
    ':challenge' => $challenge,
    ':ttl'       => $ttl,
]);

json_response([
    'deviceTrusted'     => true,
    'deviceId'          => $deviceId,
    'challenge'         => $challenge,
    'expiresInSeconds'  => $ttl,
]);
