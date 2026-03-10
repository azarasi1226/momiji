CREATE TABLE users (
  id varchar(255) NOT NULL,
  email varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  created_at datetime(6) NOT NULL,
  updated_at datetime(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email)
);

CREATE TABLE lookup_email (
    email varchar(255) NOT NULL,
    user_id varchar(255) NOT NULL,
    PRIMARY KEY (email)
);

CREATE TABLE lookup_external_identities (
    oidc_issuer varchar(255) NOT NULL,
    oidc_subject varchar(255) NOT NULL,
    identity_provider varchar(255) NOT NULL,
    user_id varchar(255) NOT NULL,
    PRIMARY KEY (oidc_issuer, oidc_subject)
);