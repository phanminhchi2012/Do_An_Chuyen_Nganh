ALTER TABLE products ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'PET';
ALTER TABLE products ADD COLUMN category VARCHAR(30);
ALTER TABLE products ADD COLUMN stock_quantity INTEGER;
ALTER TABLE products ALTER COLUMN species DROP NOT NULL;
ALTER TABLE products ADD CONSTRAINT chk_products_kind CHECK (kind IN ('PET', 'SUPPLY', 'SERVICE'));
ALTER TABLE products ADD CONSTRAINT chk_products_catalog CHECK (
    (kind = 'PET' AND species IS NOT NULL AND btrim(species) <> '' AND category IS NULL AND stock_quantity IS NULL)
    OR (kind = 'SUPPLY' AND category IN ('LIGHTING', 'ENCLOSURE', 'FOOD', 'SUPPLEMENT', 'DECOR', 'CARE') AND stock_quantity >= 0)
    OR (kind = 'SERVICE' AND category IN ('PET_CARE', 'HABITAT_SETUP') AND stock_quantity IS NULL)
);
CREATE INDEX idx_products_kind_category ON products(kind, category);

ALTER TABLE orders ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity BETWEEN 1 AND 20);
ALTER TABLE orders ADD COLUMN requested_date DATE;
ALTER TABLE orders DROP CONSTRAINT orders_fulfillment_method_check;
ALTER TABLE orders ADD CONSTRAINT orders_fulfillment_method_check
    CHECK (fulfillment_method IN ('DELIVERY', 'PICKUP', 'SERVICE'));
