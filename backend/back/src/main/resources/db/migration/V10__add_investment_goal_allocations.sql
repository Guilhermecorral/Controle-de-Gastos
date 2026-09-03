ALTER TABLE investment_goals
    ADD COLUMN initial_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

CREATE TABLE investment_goal_contributions (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES investment_goals(id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL,
    event_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_investment_goal_contribution_goal_date
    ON investment_goal_contributions(goal_id, event_date DESC);
