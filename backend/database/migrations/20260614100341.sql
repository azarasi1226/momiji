-- Create "order_items" table
CREATE TABLE "order_items" (
  "order_id" character varying(255) NOT NULL,
  "product_id" character varying(255) NOT NULL,
  "name" character varying(255) NOT NULL,
  "unit_price" integer NOT NULL,
  "quantity" integer NOT NULL,
  PRIMARY KEY ("order_id", "product_id")
);
-- Create "orders" table
CREATE TABLE "orders" (
  "id" character varying(255) NOT NULL,
  "user_id" character varying(255) NOT NULL,
  "status" character varying(255) NOT NULL,
  "payment_method_id" character varying(255) NULL,
  "recipient_name" character varying(255) NOT NULL,
  "phone_number" character varying(255) NOT NULL,
  "postal_code" character varying(255) NOT NULL,
  "prefecture" character varying(255) NOT NULL,
  "city" character varying(255) NOT NULL,
  "street_address" character varying(255) NOT NULL,
  "building" character varying(255) NOT NULL,
  "delivery_note" character varying(500) NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY ("id")
);
-- Create index "idx_orders_user_id" to table: "orders"
CREATE INDEX "idx_orders_user_id" ON "orders" ("user_id");
