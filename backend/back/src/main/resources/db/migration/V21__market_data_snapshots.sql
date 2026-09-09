CREATE TABLE market_data_snapshots (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(40) NOT NULL,
    request_key VARCHAR(160) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_market_snapshot_lookup ON market_data_snapshots(source, request_key, fetched_at DESC);
