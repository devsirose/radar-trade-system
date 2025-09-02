CREATE TYPE billing_cycle_type AS ENUM ('MONTHLY', 'QUARTERLY', 'YEARLY');
CREATE TYPE subscription_status AS ENUM ('ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED');
CREATE TYPE transaction_type   AS ENUM ('PAYMENT', 'REFUND', 'CHARGEBACK');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED');
CREATE TYPE invoice_status     AS ENUM ('DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE');
CREATE TYPE payment_method_type AS ENUM ('CARD', 'BANK_ACCOUNT', 'WALLET');
CREATE TYPE plan_status        AS ENUM ('ACTIVE', 'INACTIVE');

-- ================================
-- Extension cho gen_random_uuid()
-- ================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ================================
-- USERS
-- ================================
CREATE TABLE users (
                       id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username   VARCHAR(255) UNIQUE NOT NULL,
                       email      VARCHAR(255) UNIQUE NOT NULL,
                       created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ================================
-- SUBSCRIPTION_PLANS
-- ================================
CREATE TABLE subscription_plans (
                                    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    name          VARCHAR NOT NULL,
                                    description   TEXT,
                                    price         DECIMAL(10,2) NOT NULL,
                                    currency      VARCHAR(3) NOT NULL,
                                    billing_cycle billing_cycle_type,
                                    trial_days    INTEGER DEFAULT 0,
                                    status        plan_status DEFAULT 'ACTIVE',
                                    created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ================================
-- SUBSCRIPTIONS
-- ================================
CREATE TABLE subscriptions (
                               id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id               UUID REFERENCES users(id) ON DELETE SET NULL,
                               plan_id               UUID REFERENCES subscription_plans(id),
                               status                subscription_status DEFAULT 'ACTIVE',
                               current_period_start  TIMESTAMPTZ,
                               current_period_end    TIMESTAMPTZ,
                               cancelled_at          TIMESTAMPTZ,
                               created_at            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                               updated_at            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ================================
-- PAYMENT_METHODS
-- ================================
CREATE TABLE payment_methods (
                                 id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 user_id       UUID REFERENCES users(id) ON DELETE SET NULL,
                                 type          payment_method_type NOT NULL,
                                 provider      VARCHAR,
                                 provider_id   VARCHAR,
                                 last4         VARCHAR(4),
                                 expiry_month  INTEGER,
                                 expiry_year   INTEGER,
                                 created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ================================
-- TRANSACTIONS (đã sửa)
-- ================================
CREATE TABLE transactions (
                              id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id                UUID,
                              subscription_id        UUID NOT NULL,
                              payment_method_id      UUID NULL,
                              amount                 DECIMAL(10,2) NOT NULL,
                              currency               VARCHAR(3) NOT NULL,
                              type                   transaction_type   NOT NULL,
                              status                 transaction_status NOT NULL DEFAULT 'PENDING',
                              gateway                VARCHAR,
                              gateway_transaction_id VARCHAR,
                              failure_reason         TEXT,
                              processed_at           TIMESTAMPTZ,
                              created_at             TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- FK cho transactions
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE SET NULL;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_subscription
        FOREIGN KEY (subscription_id)
            REFERENCES subscriptions(id)
            ON DELETE CASCADE;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_payment_method
        FOREIGN KEY (payment_method_id)
            REFERENCES payment_methods(id)
            ON DELETE SET NULL;

-- ================================
-- INVOICES
-- ================================
CREATE TABLE invoices (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          subscription_id UUID REFERENCES subscriptions(id),
                          invoice_number  VARCHAR UNIQUE NOT NULL,
                          amount_subtotal DECIMAL(10,2),
                          tax_amount      DECIMAL(10,2),
                          amount_total    DECIMAL(10,2),
                          currency        VARCHAR(3),
                          status          invoice_status DEFAULT 'DRAFT',
                          due_date        DATE,
                          paid_at         TIMESTAMPTZ,
                          created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);