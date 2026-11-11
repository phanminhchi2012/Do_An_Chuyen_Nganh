ALTER TABLE orders ADD COLUMN unit_price NUMERIC(12, 2);
UPDATE orders SET unit_price = products.price FROM products WHERE orders.product_id = products.id;
ALTER TABLE orders ALTER COLUMN unit_price SET NOT NULL;
ALTER TABLE orders ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'PAY_LATER';
ALTER TABLE orders ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'NONE';
ALTER TABLE orders ADD COLUMN payment_reference VARCHAR(32) UNIQUE;
ALTER TABLE orders ADD COLUMN payment_expires_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN vnp_transaction_no VARCHAR(30);
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_method CHECK (payment_method IN ('PAY_LATER', 'VNPAY'));
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_status CHECK (payment_status IN ('NONE', 'PENDING', 'PAID', 'FAILED', 'EXPIRED', 'REVIEW'));
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_fields CHECK (
    (payment_method = 'PAY_LATER' AND payment_status = 'NONE' AND payment_reference IS NULL)
    OR (payment_method = 'VNPAY' AND payment_status <> 'NONE' AND payment_reference IS NOT NULL AND payment_expires_at IS NOT NULL)
);
CREATE INDEX idx_orders_pending_payment_expiry ON orders(payment_expires_at) WHERE payment_status = 'PENDING';
