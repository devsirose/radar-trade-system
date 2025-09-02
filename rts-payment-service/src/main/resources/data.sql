DELETE FROM invoices;
DELETE FROM transactions;
DELETE FROM payment_methods;
DELETE FROM subscriptions;
DELETE FROM subscription_plans;

INSERT INTO subscription_plans (
    id,
    name,
    description,
    price,
    currency,
    billing_cycle,
    trial_days,
    status,
    created_at
)
VALUES (
           'e5d9c8b7-8a3b-4f5e-8d7e-9e4c5b6a7d8c',
           'Gói VIP',
           'Tất cả tính năng cao cấp',
           299000.00,
           'VND',
           'MONTHLY'::billing_cycle_type,
           0,
           'ACTIVE'::plan_status,
           NOW()
       );
