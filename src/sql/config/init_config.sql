CREATE DATABASE IF NOT EXISTS obsidiangate_db;

CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED WITH mysql_native_password BY 'admin';

GRANT ALL PRIVILEGES ON obsidiangate_db.* TO 'admin'@'localhost';

FLUSH PRIVILEGES;
