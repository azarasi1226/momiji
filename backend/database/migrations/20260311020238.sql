-- Modify "lookup_email" table
ALTER TABLE `lookup_email` DROP PRIMARY KEY, ADD PRIMARY KEY (`user_id`), ADD UNIQUE INDEX `uk_lookup_email_email` (`email`);
