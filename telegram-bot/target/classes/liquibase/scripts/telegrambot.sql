-- liquibase formatted sql

-- changeset i.gatin:1

create table notification_task (
                                   id serial not null primary key,
                                   notification_chat_id bigint not null,
                                   notification_date_time timestamp not null,
                                   notification_text varchar(255) not null,
                                   status varchar(255) not null default 'SCHEDULED',
                                   send_date timestamp
);