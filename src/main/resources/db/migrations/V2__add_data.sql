--
-- users
--
insert into users (name, password, email_address, auth_claim, pw_reset_claim, pw_reset_auth_claim, verification_claim, is_verified, auth_provider, auth_provider_id, auth_provider_profile_img_url, auth_provider_username, application_id)
values ('me', 'password', 'me@localhost', 'auth', 'pw_reset', 'pw_reset_auth', 'verification', true, 'LOCAL', 'me', null, null, 'COMPOSABLE_RSS');
--
-- api_keys
--
insert into api_keys (user_id, api_key, api_secret, application_id)
values ((select id from users where name = 'me'), '43e6fb30-0960-4b2e-8a27-1d21d4b17b7c', '$2a$10$UIh5ilbT1DxknPwc43v9Oe8kum/cQimOBc2bRkphdnR71.YE37qFS', 'COMPOSABLE_RSS');
--
-- roles
--
insert into roles (name, application_id)
values ('admin', 'COMPOSABLE_RSS');
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
