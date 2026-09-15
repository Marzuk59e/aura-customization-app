<?php
declare(strict_types=1);

/**
 * Simple sliding-window rate limiter backed by the rate_limit_hits table.
 * Returns true (and records a hit) if the caller is still under the limit,
 * false if they've hit the cap for this window.
 */
function check_rate_limit(PDO $pdo, string $bucketKey, int $maxAttempts, int $windowSeconds): bool
{
    $pdo->prepare('DELETE FROM rate_limit_hits WHERE created_at < (NOW() - INTERVAL :window SECOND)')
        ->execute([':window' => $windowSeconds]);

    $stmt = $pdo->prepare(
        'SELECT COUNT(*) FROM rate_limit_hits
         WHERE bucket_key = :key AND created_at >= (NOW() - INTERVAL :window SECOND)'
    );
    $stmt->execute([':key' => $bucketKey, ':window' => $windowSeconds]);
    $count = (int) $stmt->fetchColumn();

    if ($count >= $maxAttempts) {
        return false;
    }

    $pdo->prepare('INSERT INTO rate_limit_hits (bucket_key, created_at) VALUES (:key, NOW())')
        ->execute([':key' => $bucketKey]);

    return true;
}
