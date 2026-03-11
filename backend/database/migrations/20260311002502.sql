-- Modify "users" table
ALTER TABLE `users` ADD COLUMN `phone_number` varchar(255) NOT NULL AFTER `name`, ADD COLUMN `postal_code` varchar(255) NOT NULL AFTER `phone_number`, ADD COLUMN `address1` varchar(255) NOT NULL AFTER `postal_code`, ADD COLUMN `address2` varchar(255) NOT NULL AFTER `address1`;
