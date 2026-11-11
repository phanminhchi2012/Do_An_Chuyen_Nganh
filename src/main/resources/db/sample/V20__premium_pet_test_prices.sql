-- Local demo prices for testing higher-value pet purchases. Production does not load db/sample.
-- Match the original sample price and skip pets with orders so existing shop edits stay intact.
WITH sample_prices (individual_code, old_price, new_price) AS (
    VALUES
        ('DEMO-DRAGON-04', 1825000, 5200000),
        ('DEMO-DRAGON-07', 2250000, 5600000),
        ('DEMO-DRAGON-08', 2275000, 6200000),
        ('DEMO-DRAGON-09', 2100000, 6800000),
        ('DEMO-LIZARD-01', 2300000, 5000000),
        ('DEMO-LIZARD-02', 2325000, 5500000),
        ('DEMO-LIZARD-03', 2350000, 6000000),
        ('DEMO-LIZARD-04', 2375000, 6500000),
        ('DEMO-MONITOR_LIZARD-01', 4800000, 8500000),
        ('DEMO-MONITOR_LIZARD-02', 4825000, 9000000),
        ('DEMO-MONITOR_LIZARD-03', 4850000, 9500000),
        ('DEMO-MONITOR_LIZARD-04', 4875000, 10000000),
        ('DEMO-MONITOR_LIZARD-05', 4000000, 7200000),
        ('DEMO-MONITOR_LIZARD-06', 4025000, 7800000),
        ('DEMO-MONITOR_LIZARD-07', 4050000, 8400000),
        ('DEMO-MONITOR_LIZARD-08', 5675000, 11000000),
        ('DEMO-MONITOR_LIZARD-09', 5700000, 12000000),
        ('DEMO-MONITOR_LIZARD-10', 5725000, 13000000),
        ('DEMO-PYTHON-01', 2200000, 14000000),
        ('DEMO-PYTHON-02', 2225000, 15000000)
)
UPDATE products AS p
SET price = sample_prices.new_price
FROM sample_prices
WHERE p.individual_code = sample_prices.individual_code
  AND p.price = sample_prices.old_price
  AND p.kind = 'PET'
  AND p.status = 'AVAILABLE'
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.product_id = p.id);
