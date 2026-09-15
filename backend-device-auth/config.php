<?php
declare(strict_types=1);

/**
 * Fill in your real values below, then make sure this file (and the whole
 * project) sits OUTSIDE the public webroot if at all possible. If your
 * hosting only gives you one public_html-style folder, at minimum keep
 * config.php and the secure/ folder protected (see secure/.htaccess and
 * README.md).
 */
return [
    'db' => [
        'host'    => '127.0.0.1',
        'name'    => 'your_db_name',
        'user'    => 'your_db_user',
        'pass'    => 'your_db_password',
        'charset' => 'utf8mb4',
    ],

    'firebase' => [
        // Absolute path to the Firebase service-account JSON you download
        // from Project Settings -> Service Accounts -> Generate new private
        // key. Keep it inside secure/ (already blocked from web access).
        'service_account_path' => __DIR__ . '/secure/service-account.json',
    ],

    // How long a device-verification challenge stays valid.
    'challenge_ttl_seconds' => 180,

    // Basic abuse protection. Tune to taste.
    'rate_limit' => [
        'challenge'    => ['max' => 8,  'window' => 300], // 8 requests / 5 min per (ip+email)
        'verify_reset' => ['max' => 8,  'window' => 300],
    ],
];
