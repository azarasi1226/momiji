-- Create "shipping_addresses" table
CREATE TABLE `shipping_addresses` (
  `id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone_number` varchar(255) NOT NULL,
  `postal_code` varchar(255) NOT NULL,
  `prefecture` varchar(255) NOT NULL,
  `city` varchar(255) NOT NULL,
  `street_address` varchar(255) NOT NULL,
  `building` varchar(255) NOT NULL,
  `delivery_note` varchar(255) NOT NULL,
  `is_default` bool NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_shipping_addresses_user_id` (`user_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
