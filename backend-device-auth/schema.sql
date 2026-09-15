-- Run this once against your existing MySQL database (phpMyAdmin -> SQL tab
-- is easiest on shared hosting). Doesn't touch any of your existing tables.

CREATE TABLE IF NOT EXISTS trusted_devices (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  uid           VARCHAR(128) NOT NULL,
  email         VARCHAR(255) NOT NULL,
  device_id     VARCHAR(128) NOT NULL,
  public_key    TEXT NOT NULL,
  device_label  VARCHAR(255) NOT NULL DEFAULT '',
  algorithm     VARCHAR(64)  NOT NULL DEFAULT 'SHA256withECDSA',
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_uid_device (uid, device_id),
  KEY idx_email_device (email, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_challenges (
  id                 INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  trusted_device_id  INT UNSIGNED NOT NULL,
  challenge          VARCHAR(255) NOT NULL,
  used               TINYINT(1)   NOT NULL DEFAULT 0,
  expires_at         DATETIME     NOT NULL,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_device_used (trusted_device_id, used),
  CONSTRAINT fk_device_challenges_device FOREIGN KEY (trusted_device_id)
    REFERENCES trusted_devices (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rate_limit_hits (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bucket_key  VARCHAR(255) NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_bucket_created (bucket_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
