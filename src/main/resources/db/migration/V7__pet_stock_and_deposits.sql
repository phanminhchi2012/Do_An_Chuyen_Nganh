-- Existing pets represented one listing each. Keep reservations intact while giving
-- available legacy pets one free unit. Historical sold pets remain hidden.
ALTER TABLE products DROP CONSTRAINT chk_products_catalog;
UPDATE products SET stock_quantity = CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END
WHERE kind = 'PET' AND stock_quantity IS NULL;
ALTER TABLE products ADD CONSTRAINT chk_products_catalog CHECK (
    (kind = 'PET' AND species IS NOT NULL AND btrim(species) <> '' AND category IS NULL AND stock_quantity >= 0)
    OR (kind = 'SUPPLY' AND category IN ('LIGHTING', 'ENCLOSURE', 'FOOD', 'SUPPLEMENT', 'DECOR', 'CARE') AND stock_quantity >= 0)
    OR (kind = 'SERVICE' AND category IN ('PET_CARE', 'HABITAT_SETUP') AND stock_quantity IS NULL)
);
ALTER TABLE orders DROP CONSTRAINT orders_quantity_check;
ALTER TABLE orders ADD CONSTRAINT orders_quantity_check CHECK (quantity > 0);
ALTER TABLE orders ADD COLUMN order_type VARCHAR(20) NOT NULL DEFAULT 'PURCHASE';
ALTER TABLE orders ADD COLUMN email VARCHAR(254);
ALTER TABLE orders ADD COLUMN checkout_group VARCHAR(32);
CREATE INDEX idx_orders_checkout_group ON orders(checkout_group) WHERE checkout_group IS NOT NULL;
ALTER TABLE orders ADD COLUMN deposit_consent_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN hold_expires_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN payment_amount NUMERIC(12, 2);
UPDATE orders SET payment_amount = unit_price * quantity;
ALTER TABLE orders ALTER COLUMN payment_amount SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT chk_orders_type CHECK (order_type IN ('PURCHASE', 'HOLD_DEPOSIT'));
ALTER TABLE orders DROP CONSTRAINT orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check
    CHECK (status IN ('NEW', 'CONFIRMED', 'CANCELLED', 'HOLD_EXPIRED'));
ALTER TABLE orders ADD CONSTRAINT chk_orders_deposit_consent CHECK (
    order_type <> 'HOLD_DEPOSIT' OR (payment_method = 'VNPAY' AND deposit_consent_at IS NOT NULL)
);
CREATE INDEX idx_orders_deposit_expiry ON orders(hold_expires_at)
    WHERE order_type = 'HOLD_DEPOSIT' AND status = 'NEW';
