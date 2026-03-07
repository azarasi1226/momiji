CREATE TABLE users (
  id varchar(255) NOT NULL,
  email varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  created_at datetime(6) NOT NULL,
  updated_at datetime(6) NOT NULL,
  PRIMARY KEY (id)
  UNIQUE KEY uk_users_email (email)
);

CREATE TABLE external_identities (
    oidc_issuer varchar(255) NOT NULL COMMENT 'OIDCのissuerクレーム',
    oidc_subject varchar(255) NOT NULL COMMENT 'OIDCのsubjectクレーム',
    user_id varchar(255) NOT NULL COMMENT 'システム内のuserId' ,
    PRIMARY KEY (oidc_issuer, oidc_subject)
)