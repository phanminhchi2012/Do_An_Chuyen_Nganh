ALTER TABLE customer_accounts ADD COLUMN first_purchase_completed_at TIMESTAMPTZ;

UPDATE customer_accounts AS account
SET first_purchase_completed_at = completed.first_at
FROM (
    SELECT customer_account_id, MIN(COALESCE(paid_at, created_at)) AS first_at
    FROM orders
    WHERE customer_account_id IS NOT NULL AND order_type = 'PURCHASE'
      AND (status = 'CONFIRMED' OR payment_status = 'PAID')
    GROUP BY customer_account_id
) AS completed
WHERE account.id = completed.customer_account_id;
