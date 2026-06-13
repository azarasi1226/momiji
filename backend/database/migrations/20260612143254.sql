-- Create "baskets" table
CREATE TABLE "baskets" (
  "user_id" character varying(255) NOT NULL,
  "product_id" character varying(255) NOT NULL,
  "item_quantity" integer NOT NULL,
  "added_at" timestamp NOT NULL,
  PRIMARY KEY ("user_id", "product_id")
);
-- Create index "idx_baskets_product_id" to table: "baskets"
CREATE INDEX "idx_baskets_product_id" ON "baskets" ("product_id");
-- Create "brands" table
CREATE TABLE "brands" (
  "id" character varying(255) NOT NULL,
  "name" character varying(255) NOT NULL,
  "description" text NOT NULL,
  "status" character varying(255) NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY ("id")
);
-- Create "lookup_email" table
CREATE TABLE "lookup_email" (
  "user_id" character varying(255) NOT NULL,
  "email" character varying(255) NOT NULL,
  PRIMARY KEY ("user_id"),
  CONSTRAINT "uk_lookup_email_email" UNIQUE ("email")
);
-- Create "lookup_external_identities" table
CREATE TABLE "lookup_external_identities" (
  "oidc_issuer" character varying(255) NOT NULL,
  "oidc_subject" character varying(255) NOT NULL,
  "identity_provider" character varying(255) NOT NULL,
  "user_id" character varying(255) NOT NULL,
  PRIMARY KEY ("oidc_issuer", "oidc_subject")
);
-- Create "payment_methods" table
CREATE TABLE "payment_methods" (
  "id" character varying(255) NOT NULL,
  "user_id" character varying(255) NOT NULL,
  "brand" character varying(255) NOT NULL,
  "last4" character varying(255) NOT NULL,
  "exp_month" integer NOT NULL,
  "exp_year" integer NOT NULL,
  "is_default" boolean NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY ("id")
);
-- Create index "idx_payment_methods_user_id" to table: "payment_methods"
CREATE INDEX "idx_payment_methods_user_id" ON "payment_methods" ("user_id");
-- Create "products" table
CREATE TABLE "products" (
  "id" character varying(255) NOT NULL,
  "brand_id" character varying(255) NOT NULL,
  "name" character varying(255) NOT NULL,
  "description" text NOT NULL,
  "image_url" character varying(255) NULL,
  "price" integer NOT NULL,
  "status" character varying(255) NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY ("id")
);
-- Create "shipping_addresses" table
CREATE TABLE "shipping_addresses" (
  "id" character varying(255) NOT NULL,
  "user_id" character varying(255) NOT NULL,
  "name" character varying(255) NOT NULL,
  "phone_number" character varying(255) NOT NULL,
  "postal_code" character varying(255) NOT NULL,
  "prefecture" character varying(255) NOT NULL,
  "city" character varying(255) NOT NULL,
  "street_address" character varying(255) NOT NULL,
  "building" character varying(255) NOT NULL,
  "delivery_note" character varying(500) NOT NULL,
  "is_default" boolean NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY ("id")
);
-- Create index "idx_shipping_addresses_user_id" to table: "shipping_addresses"
CREATE INDEX "idx_shipping_addresses_user_id" ON "shipping_addresses" ("user_id");
-- Create "stocks" table
CREATE TABLE "stocks" (
  "product_id" character varying(255) NOT NULL,
  "on_hand" integer NOT NULL,
  "reserved" integer NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY ("product_id")
);
-- Create "users" table
CREATE TABLE "users" (
  "id" character varying(255) NOT NULL,
  "email" character varying(255) NOT NULL,
  "name" character varying(255) NOT NULL,
  "stripe_customer_id" character varying(255) NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uk_users_email" UNIQUE ("email")
);
