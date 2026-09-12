ALTER TABLE users ADD COLUMN IF NOT EXISTS tax_profile_type VARCHAR(10);

CREATE TABLE tax_obligations (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    issuing_authority VARCHAR(120),
    category VARCHAR(24) NOT NULL,
    due_date DATE NOT NULL,
    estimated_amount NUMERIC(19,2) NOT NULL,
    paid_amount NUMERIC(19,2),
    paid_date DATE,
    status VARCHAR(24) NOT NULL,
    document_stage VARCHAR(24) NOT NULL,
    recurrence VARCHAR(24) NOT NULL,
    competence_year INTEGER NOT NULL,
    competence_month INTEGER,
    notes VARCHAR(1000),
    origin VARCHAR(24) NOT NULL,
    payment_account_description VARCHAR(255),
    receipt_reference VARCHAR(255),
    linked_transaction_id BIGINT UNIQUE REFERENCES transactions(id) ON DELETE SET NULL,
    linked_darf_id BIGINT UNIQUE REFERENCES tax_payments(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT tax_obligation_paid_together CHECK ((paid_amount IS NULL AND paid_date IS NULL) OR (paid_amount IS NOT NULL AND paid_date IS NOT NULL)),
    CONSTRAINT tax_obligation_amount_nonnegative CHECK (estimated_amount >= 0 AND (paid_amount IS NULL OR paid_amount > 0))
);

CREATE INDEX idx_tax_obligations_user_due ON tax_obligations(user_id, due_date);

INSERT INTO tax_obligations (id, user_id, name, issuing_authority, category, due_date,
    estimated_amount, paid_amount, paid_date, status, document_stage, recurrence,
    competence_year, competence_month, notes, origin, payment_account_description,
    linked_transaction_id, linked_darf_id, created_at, updated_at)
SELECT gen_random_uuid(), p.user_id, CONCAT('DARF ', p.revenue_code, ' - ', p.period),
    'Receita Federal', 'DARF', p.due_date, p.amount, p.amount, p.paid_at,
    'PAGA', 'GUIA_EMITIDA', 'UNICA', CAST(SUBSTRING(p.period, 1, 4) AS INTEGER),
    CAST(SUBSTRING(p.period, 6, 2) AS INTEGER), p.note, 'INVESTIMENTO', p.account_label,
    t.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tax_payments p
LEFT JOIN transactions t ON t.user_id = p.user_id AND t.managed_reference = CONCAT('tax-payment:', p.id);
