--
-- users table
--
drop table if exists users cascade;

create table users
(
  id serial,
  name varchar(100) unique not null,
  password varchar(256) not null,
  email_address varchar(512) unique not null,
  auth_claim varchar(256),
  pw_reset_claim varchar(256),
  pw_reset_auth_claim varchar(256),
  verification_claim varchar(256),
  is_verified boolean not null default false,
  subscription_status varchar(128),
  subscription_exp_date timestamp with time zone,
  customer_id varchar(256),
  auth_provider varchar(64) not null,
  auth_provider_id varchar(256),
  auth_provider_profile_img_url varchar(1024),
  auth_provider_username varchar(256),

  primary key (id)
);
--
-- roles table
--
drop table if exists roles cascade;

create table roles
(
  id serial,
  name varchar(256) unique not null,

  primary key(name)
);
--
-- features_in_roles table
--
drop table if exists features_in_roles cascade;

create table features_in_roles
(
  id serial,
  feature_cd varchar(100) not null,
  role varchar(100) not null references roles(name) on delete cascade,

  primary key(id)
);
--
-- users_in_roles table
--
drop table if exists users_in_roles cascade;

create table users_in_roles
(
  id serial,
  username varchar(100) not null references users(name) on delete cascade,
  role varchar(100) not null references roles(name) on delete cascade,

  primary key (id)
);
--
-- framework_config table
--
drop table if exists framework_config cascade;

create table framework_config
(
  id serial not null,
  user_id integer not null references users(id) on delete cascade,
  settings_group varchar(256) not null,
  attr_name varchar(256) not null,
  attr_value varchar(4000) not null,
  unique(user_id, settings_group, attr_name),

  primary key (id)
);
--
-- indexes
--
drop index if exists idx_roles_name;
drop index if exists idx_features_in_roles_role;
drop index if exists idx_users_email_address;
drop index if exists idx_users_customer_id;
drop index if exists idx_users_auth_provider;
drop index if exists idx_users_name;
drop index if exists idx_framework_config_user_id;

create index idx_roles_name on roles(name);
create index idx_features_in_roles_role on features_in_roles(role);
create index idx_users_email_address on users(email_address);
create index idx_users_customer_id on users(customer_id);
create index idx_users_auth_provider on users(auth_provider);
create index idx_users_name on users(name);
create index idx_framework_config_user_id on framework_config(user_id);
