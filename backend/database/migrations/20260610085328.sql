-- Modify "users" table
ALTER TABLE `users` ADD COLUMN `stripe_customer_id` varchar(255) NULL AFTER `address2`;
-- Create "payment_methods" table
CREATE TABLE `payment_methods` (
  `id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `brand` varchar(255) NOT NULL,
  `last4` varchar(255) NOT NULL,
  `exp_month` int NOT NULL,
  `exp_year` int NOT NULL,
  `is_default` bool NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_payment_methods_user_id` (`user_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
