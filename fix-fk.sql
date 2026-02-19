-- Check current FKs
SELECT tc.constraint_name, kcu.column_name, ccu.table_name AS ref_table
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name = 'orders';

-- Drop bad FKs that reference 'users' instead of 'auth_users'
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fkhtx3insd5ge6w486omk4fnk54;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_buyer;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_seller;

-- Recreate FKs pointing to auth_users
ALTER TABLE orders ADD CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES auth_users(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES auth_users(id);
