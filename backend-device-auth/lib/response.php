<?php
declare(strict_types=1);

/**
 * Sends a JSON response and terminates the script. Business-logic outcomes
 * (deviceTrusted: false, success: false, etc.) intentionally use HTTP 200 so
 * the Android client's Retrofit `Response<T>.body()` is populated — the app
 * decides success/failure from the JSON payload, not the status code, for
 * these three endpoints. Use $status for transport-level problems only
 * (malformed method, missing auth header, unexpected server error).
 */
function json_response(array $data, int $status = 200): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_SLASHES);
    exit;
}

function json_error(string $message, int $status = 400): void
{
    json_response(['error' => $message], $status);
}
