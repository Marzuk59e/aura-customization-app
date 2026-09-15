<?php
declare(strict_types=1);

/** Parses the raw JSON request body into an associative array (empty array on any problem). */
function read_json_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || $raw === '') {
        return [];
    }
    $data = json_decode($raw, true);
    return is_array($data) ? $data : [];
}

/**
 * Extracts the bearer token from the Authorization header.
 *
 * NOTE: some shared-hosting setups (PHP-CGI/FastCGI) strip the Authorization
 * header before PHP ever sees it. If bearer_token() always returns null even
 * though the app is sending the header, add `CGIPassAuth On` to your
 * .htaccess (see README.md).
 */
function bearer_token(): ?string
{
    $auth = null;

    if (function_exists('getallheaders')) {
        foreach (getallheaders() as $key => $value) {
            if (strcasecmp($key, 'Authorization') === 0) {
                $auth = $value;
                break;
            }
        }
    }

    if ($auth === null) {
        $auth = $_SERVER['HTTP_AUTHORIZATION']
            ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
            ?? null;
    }

    if (!$auth || stripos($auth, 'Bearer ') !== 0) {
        return null;
    }

    return trim(substr($auth, 7));
}

/**
 * IMPORTANT: X-Forwarded-For is a client-controllable header — trusting it
 * blindly lets an attacker send a different fake value on every request and
 * bypass the IP part of rate limiting entirely. Only trust it if this app
 * genuinely sits behind a reverse proxy/load balancer that overwrites it
 * (Cloudflare, nginx in front of PHP-FPM, etc). On typical shared hosting
 * where Apache talks to the client directly, REMOTE_ADDR is the honest
 * value and X-Forwarded-For should be ignored.
 *
 * Flip TRUST_FORWARDED_FOR to true only if you've confirmed you're behind
 * such a proxy.
 */
const TRUST_FORWARDED_FOR = false;

function client_ip(): string
{
    if (TRUST_FORWARDED_FOR && !empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
        // Leftmost entry is the original client when the proxy chain is trusted.
        $parts = explode(',', $_SERVER['HTTP_X_FORWARDED_FOR']);
        return trim($parts[0]);
    }

    return $_SERVER['REMOTE_ADDR'] ?? 'unknown';
}
