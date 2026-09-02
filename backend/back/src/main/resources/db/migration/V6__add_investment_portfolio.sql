create table if not exists investment_positions (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    asset_type varchar(20) not null check (asset_type in ('ACAO', 'FII', 'CRIPTO', 'RENDA_FIXA')),
    symbol varchar(30),
    external_id varchar(80),
    name varchar(120) not null,
    quantity numeric(24, 8),
    average_price numeric(19, 6),
    principal numeric(19, 2),
    annual_rate numeric(8, 4),
    purchase_date date not null,
    maturity_date date,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_investment_positions_user on investment_positions(user_id, asset_type);
create index if not exists idx_investment_positions_symbol on investment_positions(symbol);

create table if not exists investment_movements (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    position_id bigint not null references investment_positions(id) on delete cascade,
    movement_type varchar(20) not null check (movement_type in ('APORTE', 'RESGATE', 'DIVIDENDO', 'RENDIMENTO')),
    amount numeric(19, 2) not null,
    event_date date not null,
    automatic boolean not null default false,
    external_reference varchar(120),
    created_at timestamp not null
);

create index if not exists idx_investment_movements_user_date on investment_movements(user_id, event_date desc);
create unique index if not exists uk_investment_movement_external_reference
    on investment_movements(user_id, external_reference)
    where external_reference is not null;
