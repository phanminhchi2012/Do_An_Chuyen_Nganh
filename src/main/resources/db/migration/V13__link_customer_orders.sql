ALTER TABLE orders ADD COLUMN customer_account_id BIGINT REFERENCES customer_accounts(id);
CREATE INDEX idx_orders_customer_account_created ON orders (customer_account_id, created_at DESC, id DESC)
    WHERE customer_account_id IS NOT NULL;
