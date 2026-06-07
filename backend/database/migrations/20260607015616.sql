-- Create "baskets" table
CREATE TABLE `baskets` (
  `user_id` varchar(255) NOT NULL,
  `product_id` varchar(255) NOT NULL,
  `item_quantity` int NOT NULL,
  `added_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`, `product_id`),
  INDEX `idx_baskets_product_id` (`product_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
