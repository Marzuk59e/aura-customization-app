<?php
declare(strict_types=1);

use Kreait\Firebase\Factory;
use Kreait\Firebase\Auth as FirebaseAuthClient;

function get_firebase_auth(array $config): FirebaseAuthClient
{
    static $auth = null;
    if ($auth !== null) {
        return $auth;
    }

    $factory = (new Factory())->withServiceAccount($config['firebase']['service_account_path']);
    $auth = $factory->createAuth();
    return $auth;
}

/**
 * Verifies a Firebase ID token (sent by the app's AuthInterceptor as
 * `Authorization: Bearer <token>`, same as /auth/me) and returns
 * ['uid' => ..., 'email' => ...] from its claims, or null if the token is
 * missing, expired, or invalid. The email comes straight from the token's
 * own claims — no extra Admin SDK round trip needed.
 */
function verify_firebase_token(string $idToken, array $config): ?array
{
    try {
        $auth = get_firebase_auth($config);
        $verifiedToken = $auth->verifyIdToken($idToken, false, 300);
        $claims = $verifiedToken->claims();

        $uid = $claims->get('sub');
        $email = strtolower(trim((string) $claims->get('email', '')));

        if (!$uid) {
            return null;
        }

        return ['uid' => $uid, 'email' => $email];
    } catch (\Throwable $e) {
        error_log('[device-auth] Firebase token verification failed: ' . $e->getMessage());
        return null;
    }
}
