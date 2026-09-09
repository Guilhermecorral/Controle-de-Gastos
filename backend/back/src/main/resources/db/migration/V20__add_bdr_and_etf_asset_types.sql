ALTER TABLE investment_positions DROP CONSTRAINT IF EXISTS investment_positions_asset_type_check;

ALTER TABLE investment_positions
    ADD CONSTRAINT investment_positions_asset_type_check
    CHECK (asset_type IN ('ACAO', 'FII', 'FIAGRO', 'BDR', 'ETF', 'CRIPTO', 'RENDA_FIXA'));
