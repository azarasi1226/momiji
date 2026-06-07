-- Create "stocks" table
CREATE TABLE `stocks` (
  `product_id` varchar(255) NOT NULL,
  `on_hand` int NOT NULL,
  `reserved` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`product_id`)
) CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
