CREATE TABLE investment_import_batches (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    source_format VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP
);

CREATE TABLE investment_import_items (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES investment_import_batches(id) ON DELETE CASCADE,
    source_row INTEGER NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    asset_type VARCHAR(20) NOT NULL,
    symbol VARCHAR(30),
    name VARCHAR(120) NOT NULL,
    market VARCHAR(10) NOT NULL,
    exchange VARCHAR(30),
    currency VARCHAR(3) NOT NULL,
    quantity NUMERIC(24, 8) NOT NULL,
    unit_price NUMERIC(19, 6) NOT NULL,
    brokerage_fee NUMERIC(19, 2),
    b3_fee NUMERIC(19, 2),
    other_costs NUMERIC(19, 2),
    withheld_tax NUMERIC(19, 2),
    exchange_rate NUMERIC(19, 8) NOT NULL,
    event_date DATE NOT NULL,
    possible_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    warning VARCHAR(500) NOT NULL DEFAULT ''
);

CREATE INDEX idx_investment_import_batches_user ON investment_import_batches(user_id, created_at DESC);
CREATE INDEX idx_investment_import_items_batch ON investment_import_items(batch_id, source_row);
