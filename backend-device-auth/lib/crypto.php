<?php
declare(strict_types=1);

/**
 * Converts the Base64 X.509 SubjectPublicKeyInfo the Android app sends
 * (PublicKey.encoded, Base64-encoded — see DeviceCredentialManager.kt) into
 * a PEM string OpenSSL can load. A PEM "PUBLIC KEY" block IS just the DER
 * SPKI bytes, base64-wrapped at 64 chars with BEGIN/END markers — no other
 * transformation needed.
 */
function pem_from_base64_spki(string $base64Spki): ?string
{
    $der = base64_decode($base64Spki, true);
    if ($der === false || $der === '') {
        return null;
    }
    $wrapped = chunk_split(base64_encode($der), 64, "\n");
    return "-----BEGIN PUBLIC KEY-----\n{$wrapped}-----END PUBLIC KEY-----\n";
}

/**
 * Verifies an ECDSA (SHA256withECDSA) signature against a stored Base64 SPKI
 * public key. $signedData must be the EXACT bytes that were signed on the
 * client — the Android side signs the UTF-8 bytes of the challenge string
 * itself (see DeviceCredentialManager.signChallenge), not the raw decoded
 * challenge bytes, so callers here must pass the challenge string as-is.
 *
 * Returns true only on a confirmed valid signature — every other outcome
 * (bad key, bad signature, OpenSSL error) returns false.
 */
function verify_ecdsa_signature(string $base64PublicKey, string $signedData, string $base64Signature): bool
{
    $pem = pem_from_base64_spki($base64PublicKey);
    if ($pem === null) {
        return false;
    }

    $publicKey = openssl_pkey_get_public($pem);
    if ($publicKey === false) {
        return false;
    }

    $signature = base64_decode($base64Signature, true);
    if ($signature === false || $signature === '') {
        return false;
    }

    $result = openssl_verify($signedData, $signature, $publicKey, OPENSSL_ALGO_SHA256);
    return $result === 1;
}
