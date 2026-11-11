ALTER TABLE orders DROP CONSTRAINT orders_fulfillment_method_check;
ALTER TABLE orders ADD CONSTRAINT orders_fulfillment_method_check
    CHECK (fulfillment_method IN ('DELIVERY', 'PICKUP', 'SERVICE', 'UNDECIDED'));

ALTER TABLE vnpay_ipn_events ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'IPN';
ALTER TABLE vnpay_ipn_events ADD CONSTRAINT chk_vnpay_event_source
    CHECK (source IN ('IPN', 'LOCAL_RETURN'));
