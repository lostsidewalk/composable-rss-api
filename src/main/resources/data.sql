--
-- users
--
insert into users (name, password, email_address, auth_claim, pw_reset_claim, pw_reset_auth_claim, verification_claim, is_verified, auth_provider, auth_provider_id, auth_provider_profile_img_url, auth_provider_username)
values ('me', 'password', 'meh@lostsidewalk.com', 'auth', 'pw_reset', 'pw_reset_auth', 'verification', true, 'LOCAL', 'me', null, null);
--
-- roles
--
insert into roles (name)
values ('admin');
--
-- features_in_roles
--
-- insert into features_in_roles (role, feature_cd)
-- values ('admin', 'login');
--
-- framework_config
--
insert into framework_config(user_id,settings_group,attr_name,attr_value)
values ((select id from users where name = 'me'), 'notifications', 'disabled', 'true');
