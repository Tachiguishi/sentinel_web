
drop table if exists sentinel_station;
create table sentinel_station (
    name varchar(64) primary key not null,
    description varchar(255) null,
    ip_address varchar(15) null,
    instant_no int default 3,
    create_at timestamp not null default current_timestamp,
    scd_updated_at timestamp null default null
);
