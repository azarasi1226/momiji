-- プロフィールは email と name のみ（Amazon 式）。 住所・電話は shipping_addresses にだけ存在する。
CREATE TABLE users (
  id varchar(255) NOT NULL,
  email varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  -- Stripe Customer ID (cus_)。 lazy 作成（初回カード登録まで NULL）。 1 ユーザー = 1 Customer の 1:1。
  stripe_customer_id varchar(255),
  created_at timestamp(6) NOT NULL,
  updated_at timestamp(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE lookup_email (
    user_id varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_lookup_email_email UNIQUE (email)
);

CREATE TABLE lookup_external_identities (
    oidc_issuer varchar(255) NOT NULL,
    oidc_subject varchar(255) NOT NULL,
    identity_provider varchar(255) NOT NULL,
    user_id varchar(255) NOT NULL,
    PRIMARY KEY (oidc_issuer, oidc_subject)
);

CREATE TABLE brands (
  id varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  description text NOT NULL,
  status varchar(255) NOT NULL,
  created_at timestamp(6) NOT NULL,
  updated_at timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE products (
  id varchar(255) NOT NULL,
  brand_id varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  description text NOT NULL,
  image_url varchar(255),
  price int NOT NULL,
  status varchar(255) NOT NULL,
  created_at timestamp(6) NOT NULL,
  updated_at timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE baskets (
  user_id varchar(255) NOT NULL,
  product_id varchar(255) NOT NULL,
  item_quantity int NOT NULL,
  added_at timestamp(6) NOT NULL,
  PRIMARY KEY (user_id, product_id)
);

-- 商品が生産終了になった時に product_id で対象行を一括削除する用
CREATE INDEX idx_baskets_product_id ON baskets (product_id);

-- ユーザーが消されたら user_id で一括削除する処理があるが、複合インデックスの左側が user_id なので定義する必要なし

CREATE TABLE stocks (
  product_id varchar(255) NOT NULL,
  on_hand int NOT NULL,
  reserved int NOT NULL,
  updated_at timestamp(6) NOT NULL,
  PRIMARY KEY (product_id)
);

-- 生カード番号は持たず、 表示用の brand / 下 4 桁 / 有効期限のみ
CREATE TABLE payment_methods (
  id varchar(255) NOT NULL,
  user_id varchar(255) NOT NULL,
  brand varchar(255) NOT NULL,
  last4 varchar(255) NOT NULL,
  exp_month int NOT NULL,
  exp_year int NOT NULL,
  is_default boolean NOT NULL,
  created_at timestamp(6) NOT NULL,
  updated_at timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);

-- ユーザーのカード一覧取得・ユーザー削除時の user_id 一括削除用
CREATE INDEX idx_payment_methods_user_id ON payment_methods (user_id);

-- 配送先 ReadModel。 配送先はユーザー本人の住所とは限らない（受取人氏名・ドライバー連絡用電話を持つ）。
CREATE TABLE shipping_addresses (
  id varchar(255) NOT NULL,
  user_id varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  phone_number varchar(255) NOT NULL,
  postal_code varchar(255) NOT NULL,
  prefecture varchar(255) NOT NULL,
  city varchar(255) NOT NULL,
  street_address varchar(255) NOT NULL,
  building varchar(255) NOT NULL,
  delivery_note varchar(500) NOT NULL,
  is_default boolean NOT NULL,
  created_at timestamp(6) NOT NULL,
  updated_at timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);

-- ユーザーの配送先一覧取得・ユーザー削除時の user_id 一括削除用
CREATE INDEX idx_shipping_addresses_user_id ON shipping_addresses (user_id);

-- 注文 ReadModel（ヘッダ）。 配送先・カードは注文時点のスナップショット（商品/住所/カードが後で変わっても不変）。
CREATE TABLE orders (
  id varchar(255) NOT NULL,
  user_id varchar(255) NOT NULL,
  -- OrderStatus の name（STARTED / PAID / SHIPPED / COMPLETED / FAILED）
  status varchar(255) NOT NULL,
  -- 決済に使うカード（Stripe pm_）の参照。 決済準備で埋まる（注文開始時点は NULL）。
  payment_method_id varchar(255),
  -- 決済の PaymentIntent（Stripe pi_）。 決済準備で埋まる（返金・相関用。 注文開始時点は NULL）。
  payment_intent_id varchar(255),
  -- 配送先スナップショット（注文時点の宛先を固定）
  recipient_name varchar(255) NOT NULL,
  phone_number varchar(255) NOT NULL,
  postal_code varchar(255) NOT NULL,
  prefecture varchar(255) NOT NULL,
  city varchar(255) NOT NULL,
  street_address varchar(255) NOT NULL,
  building varchar(255) NOT NULL,
  delivery_note varchar(500) NOT NULL,
  created_at timestamp(6) NOT NULL,
  updated_at timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);

-- ユーザーの注文一覧取得用
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- 注文明細 ReadModel。 商品名・単価は注文時点のスナップショット（商品が後でリネーム・廃番・改価されても不変）。
CREATE TABLE order_items (
  order_id varchar(255) NOT NULL,
  product_id varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  unit_price int NOT NULL,
  quantity int NOT NULL,
  PRIMARY KEY (order_id, product_id)
);
