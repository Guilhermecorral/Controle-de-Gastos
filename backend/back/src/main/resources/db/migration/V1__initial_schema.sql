create table if not exists users (
    id bigserial primary key,
    name varchar(100) not null,
    email varchar(150) not null unique,
    password varchar(255) not null,
    role varchar(20) not null check (role in ('USER', 'ADMIN')),
    created_at timestamp not null
);

create table if not exists wishlist_lists (
    id bigserial primary key,
    name varchar(120) not null,
    description varchar(255),
    is_default boolean not null default false,
    user_id bigint not null references users(id) on delete cascade,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists wishlist_items (
    id bigserial primary key,
    description varchar(255) not null,
    original_price numeric(10, 2) not null,
    discount_percent numeric(5, 2) not null default 0,
    final_price numeric(19, 2) not null,
    priority varchar(20) not null check (priority in ('ALTO', 'MEDIA', 'BAIXO')),
    category varchar(30) not null check (category in ('COMPRAS', 'ALIMENTACAO', 'MORADIA', 'SAUDE', 'LAZER', 'EDUCACAO', 'TRANSPORTE', 'OUTROS')),
    notes varchar(500),
    status varchar(20) not null check (status in ('PENDENTE', 'COMPRADO')),
    purchase_date date,
    payment_method varchar(40) check (payment_method in ('PIX', 'CARTAO_DEBITO', 'CARTAO_CREDITO_AVISTA', 'CARTAO_CREDITO_PARCELADO', 'DINHEIRO')),
    installments integer not null default 1,
    first_installment_next_month boolean not null default false,
    archived_after_purchase boolean not null default false,
    user_id bigint not null references users(id) on delete cascade,
    wishlist_list_id bigint not null references wishlist_lists(id) on delete cascade,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists transactions (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    wishlist_item_id bigint references wishlist_items(id) on delete set null,
    type varchar(20) not null check (type in ('RECEITA', 'DESPESA')),
    description varchar(255) not null,
    category varchar(30) not null check (category in ('ALIMENTACAO', 'TRANSPORTE', 'MORADIA', 'SAUDE', 'LAZER', 'EDUCACAO', 'COMPRAS', 'OUTROS')),
    amount numeric(19, 2) not null,
    payment_method varchar(40) not null check (payment_method in ('PIX', 'CARTAO_DEBITO', 'CARTAO_CREDITO_AVISTA', 'CARTAO_CREDITO_PARCELADO', 'DINHEIRO')),
    installments integer,
    transaction_group_id uuid,
    transaction_date date not null,
    created_at timestamp not null
);

create table if not exists wishlist_history_entries (
    id bigserial primary key,
    wishlist_item_id bigint not null references wishlist_items(id) on delete cascade,
    user_id bigint not null references users(id) on delete cascade,
    action_type varchar(30) not null check (action_type in ('CREATED', 'UPDATED', 'MOVED', 'PURCHASED', 'PURCHASE_UNDONE', 'DELETED')),
    description varchar(255) not null,
    final_price_snapshot numeric(19, 2) not null,
    list_name_snapshot varchar(120),
    created_at timestamp not null
);

create table if not exists password_reset_tokens (
    id bigserial primary key,
    token_hash varchar(128) not null unique,
    user_id bigint not null references users(id) on delete cascade,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    created_at timestamp with time zone not null
);

create index if not exists idx_transactions_user_date on transactions(user_id, transaction_date desc);
create index if not exists idx_transactions_group on transactions(transaction_group_id);
create index if not exists idx_wishlist_items_user_status on wishlist_items(user_id, status);
create index if not exists idx_password_reset_tokens_user on password_reset_tokens(user_id);
