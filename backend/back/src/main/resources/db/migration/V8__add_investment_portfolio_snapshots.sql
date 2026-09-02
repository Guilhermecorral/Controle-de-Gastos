CREATE TABLE investment_portfolio_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    invested_amount NUMERIC(19, 2) NOT NULL,
    current_value NUMERIC(19, 2) NOT NULL,
    income_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    CONSTRAINT uk_investment_snapshot_user_date UNIQUE (user_id, snapshot_date)
);

CREATE INDEX idx_investment_snapshot_user_date
    ON investment_portfolio_snapshots(user_id, snapshot_date DESC);
