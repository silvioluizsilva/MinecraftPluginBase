-- Execute este arquivo como administrador e troque a senha antes do uso.
CREATE DATABASE IF NOT EXISTS pluginbase
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'pluginbase'@'localhost'
    IDENTIFIED BY 'CHANGE_THIS_STRONG_PASSWORD';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
    ON pluginbase.*
    TO 'pluginbase'@'localhost';
