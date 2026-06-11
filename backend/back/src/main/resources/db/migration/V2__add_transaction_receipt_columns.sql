ALTER TABLE transactions
    ADD COLUMN receipt_original_filename VARCHAR(255);

ALTER TABLE transactions
    ADD COLUMN receipt_storage_name VARCHAR(255);

ALTER TABLE transactions
    ADD COLUMN receipt_content_type VARCHAR(120);

ALTER TABLE transactions
    ADD COLUMN receipt_size_bytes BIGINT;

ALTER TABLE transactions
    ADD COLUMN receipt_uploaded_at TIMESTAMP;
