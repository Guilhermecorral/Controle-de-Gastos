CREATE TABLE corporate_events (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(30) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    payer_cnpj VARCHAR(18),
    amount_per_unit NUMERIC(19, 8) NOT NULL,
    tax_rate NUMERIC(8, 4) NOT NULL DEFAULT 0,
    ex_date DATE NOT NULL,
    payment_date DATE NOT NULL,
    source VARCHAR(40) NOT NULL,
    source_reference VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_corporate_events_symbol_payment ON corporate_events(symbol, payment_date);

CREATE TABLE wallet_earnings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    position_id BIGINT NOT NULL REFERENCES investment_positions(id) ON DELETE CASCADE,
    corporate_event_id BIGINT NOT NULL REFERENCES corporate_events(id) ON DELETE CASCADE,
    quantity_eligible NUMERIC(24, 8) NOT NULL,
    gross_amount NUMERIC(19, 2) NOT NULL,
    withheld_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    investment_movement_id BIGINT UNIQUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_wallet_earning_user_event UNIQUE (user_id, corporate_event_id)
);

CREATE INDEX idx_wallet_earnings_user_status ON wallet_earnings(user_id, status);
