CREATE INDEX IF NOT EXISTS idx_transactions_user_date
    ON transactions (user_id, transaction_date DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_user_type_date
    ON transactions (user_id, type, transaction_date DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_user_category_date
    ON transactions (user_id, category, transaction_date DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_group_user
    ON transactions (transaction_group_id, user_id);

CREATE INDEX IF NOT EXISTS idx_transactions_wishlist_item
    ON transactions (wishlist_item_id);

CREATE INDEX IF NOT EXISTS idx_transactions_receipts_by_period
    ON transactions (user_id, transaction_date DESC, receipt_uploaded_at DESC);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_user_status
    ON wishlist_items (user_id, status);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_user_list_status
    ON wishlist_items (user_id, wishlist_list_id, status);

CREATE INDEX IF NOT EXISTS idx_wishlist_lists_user_default
    ON wishlist_lists (user_id, is_default, created_at);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at
    ON password_reset_tokens (expires_at);
