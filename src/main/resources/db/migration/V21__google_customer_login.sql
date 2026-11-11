ALTER TABLE customer_accounts ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE customer_accounts ADD COLUMN google_subject VARCHAR(255);
CREATE UNIQUE INDEX ux_customer_accounts_google_subject
    ON customer_accounts (google_subject) WHERE google_subject IS NOT NULL;
