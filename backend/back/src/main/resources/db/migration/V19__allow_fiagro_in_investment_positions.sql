alter table investment_positions drop constraint if exists investment_positions_asset_type_check;

alter table investment_positions
    add constraint investment_positions_asset_type_check
    check (asset_type in ('ACAO', 'FII', 'FIAGRO', 'CRIPTO', 'RENDA_FIXA'));
