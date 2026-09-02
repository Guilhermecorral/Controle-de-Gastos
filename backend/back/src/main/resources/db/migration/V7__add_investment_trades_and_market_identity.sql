alter table investment_positions add column if not exists market varchar(10) not null default 'BR';
alter table investment_positions add column if not exists exchange varchar(30);
alter table investment_positions add column if not exists currency varchar(3) not null default 'BRL';

update investment_positions
set market = 'GLOBAL', exchange = 'CRYPTO'
where asset_type = 'CRIPTO' and market = 'BR';

alter table investment_movements drop constraint if exists investment_movements_movement_type_check;
alter table investment_movements add constraint investment_movements_movement_type_check
    check (movement_type in ('COMPRA', 'VENDA', 'APORTE', 'RESGATE', 'DIVIDENDO', 'RENDIMENTO'));
alter table investment_movements add column if not exists quantity numeric(24, 8);
alter table investment_movements add column if not exists unit_price numeric(19, 6);
alter table investment_movements add column if not exists fees numeric(19, 2) not null default 0;

insert into investment_movements (
    user_id, position_id, movement_type, amount, quantity, unit_price, fees,
    event_date, automatic, external_reference, created_at
)
select
    p.user_id,
    p.id,
    'COMPRA',
    round(p.quantity * p.average_price, 2),
    p.quantity,
    p.average_price,
    0,
    p.purchase_date,
    true,
    'OPENING_POSITION:' || p.id,
    p.created_at
from investment_positions p
where p.asset_type <> 'RENDA_FIXA'
  and p.quantity > 0
  and p.average_price is not null
  and not exists (
      select 1 from investment_movements m
      where m.user_id = p.user_id and m.external_reference = 'OPENING_POSITION:' || p.id
  );

create index if not exists idx_investment_positions_identity
    on investment_positions(user_id, asset_type, market, symbol);
create index if not exists idx_investment_movements_position_date
    on investment_movements(position_id, event_date desc, created_at desc);
