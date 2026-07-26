CREATE TABLE IF NOT EXISTS pluginbase_settings (
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO pluginbase_settings (setting_key, setting_value)
VALUES ('schema.owner', 'PluginBase')
ON DUPLICATE KEY UPDATE setting_value = setting_value;
