-- Local/demo sample stock for testing quantity selection. Preserve reserved and sold items.
UPDATE products
SET stock_quantity = GREATEST(COALESCE(stock_quantity, 0), 25)
WHERE status = 'AVAILABLE'
  AND kind IN ('PET', 'SUPPLY')
  AND COALESCE(stock_quantity, 0) < 25;
