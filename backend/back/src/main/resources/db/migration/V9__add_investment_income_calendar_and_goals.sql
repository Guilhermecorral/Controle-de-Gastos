CREATE TABLE investment_income_schedules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    position_id BIGINT NOT NULL REFERENCES investment_positions(id) ON DELETE CASCADE,
    income_type VARCHAR(20) NOT NULL,
    amount_per_unit NUMERIC(19, 8) NOT NULL,
    tax_rate NUMERIC(8, 4) NOT NULL DEFAULT 0,
    ex_date DATE,
    payment_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AGUARDANDO',
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_investment_income_schedule_user_payment
    ON investment_income_schedules(user_id, payment_date, status);

CREATE TABLE investment_goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    target_amount NUMERIC(19, 2) NOT NULL,
    initial_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    monthly_contribution NUMERIC(19, 2) NOT NULL DEFAULT 0,
    annual_growth_rate NUMERIC(8, 4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_investment_goal_user_active
    ON investment_goals(user_id, active);

CREATE TABLE investment_goal_contributions (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES investment_goals(id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL,
    event_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_investment_goal_contribution_goal_date
    ON investment_goal_contributions(goal_id, event_date DESC);
