-- Modify "lookup_external_identities" table
ALTER TABLE `lookup_external_identities` ADD COLUMN `identity_provider` varchar(255) NOT NULL AFTER `oidc_subject`;
