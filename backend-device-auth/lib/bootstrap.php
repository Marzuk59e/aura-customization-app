<?php
declare(strict_types=1);

/**
 * Every endpoint starts with:
 *   require_once __DIR__ . '/../../../../lib/bootstrap.php';
 *   ['config' => $config, 'pdo' => $pdo] = app_bootstrap();
 *
 * Cached in a static so requiring it from multiple files in one request
 * (shouldn't normally happen, but cheap to guard) doesn't reconnect twice.
 */
function app_bootstrap(): array
{
    static $ctx = null;
    if ($ctx !== null) {
        return $ctx;
    }

    error_reporting(E_ALL);
    ini_set('display_errors', '0'); // never leak stack traces into a JSON response

    $root = dirname(__DIR__);

    require_once $root . '/vendor/autoload.php';
    $config = require $root . '/config.php';

    require_once __DIR__ . '/response.php';
    require_once __DIR__ . '/request.php';
    require_once __DIR__ . '/database.php';
    require_once __DIR__ . '/crypto.php';
    require_once __DIR__ . '/firebase_auth.php';
    require_once __DIR__ . '/rate_limit.php';

    $pdo = get_pdo($config['db']);

    $ctx = ['config' => $config, 'pdo' => $pdo];
    return $ctx;
}
