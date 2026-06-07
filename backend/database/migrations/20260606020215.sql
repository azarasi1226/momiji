-- Modify "brands" table
ALTER TABLE `brands` ADD COLUMN `status` varchar(255) NOT NULL AFTER `description`;
-- Modify "products" table
ALTER TABLE `products` ADD COLUMN `status` varchar(255) NOT NULL AFTER `price`;
