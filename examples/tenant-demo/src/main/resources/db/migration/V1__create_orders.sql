create table orders (
    id bigint not null auto_increment primary key,
    tenant_id bigint not null,
    business_key varchar(64) not null,
    creator_id bigint not null,
    department_id bigint not null,
    name varchar(128) not null,
    created_at timestamp not null,
    unique key uk_orders_tenant_business_key (tenant_id, business_key)
);
