alter table investment_positions add column fixed_income_yield_type varchar(20);
alter table investment_positions add column fixed_income_indexer varchar(30);
alter table investment_positions add column daily_liquidity boolean not null default false;
