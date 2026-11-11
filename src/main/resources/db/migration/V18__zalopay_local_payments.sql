ALTER TABLE orders DROP CONSTRAINT chk_orders_payment_method;
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_method
    CHECK (payment_method IN ('PAY_LATER', 'VNPAY', 'ZALOPAY'));

ALTER TABLE orders DROP CONSTRAINT chk_orders_payment_fields;
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_fields CHECK (
    (payment_method = 'PAY_LATER' AND payment_status = 'NONE' AND payment_reference IS NULL)
    OR (payment_method IN ('VNPAY', 'ZALOPAY') AND payment_status <> 'NONE'
        AND payment_reference IS NOT NULL AND payment_expires_at IS NOT NULL)
);

ALTER TABLE orders ADD COLUMN zalopay_transaction_id VARCHAR(30);
