create table tax_opening_balances (
    id bigserial primary key,
    user_id bigint not null unique references users(id) on delete cascade,
    start_date date not null,
    common_loss numeric(19,2) not null,
    day_trade_loss numeric(19,2) not null,
    fund_loss numeric(19,2) not null,
    common_credit numeric(19,2) not null,
    day_trade_credit numeric(19,2) not null,
    pending_tax numeric(19,2) not null,
    source varchar(255) not null
);
create table tax_payments (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    period varchar(7) not null,
    revenue_code varchar(4) not null,
    amount numeric(19,2) not null,
    paid_at date not null,
    due_date date not null,
    account_label varchar(255) not null,
    note varchar(255) not null,
    unique(user_id, period, revenue_code)
);
