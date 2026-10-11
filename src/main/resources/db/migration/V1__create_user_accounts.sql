create table user_accounts (
    id bigint generated always as identity primary key,
    full_name text not null,
    email text not null,
    password_hash text not null,
    role text not null,
    constraint user_accounts_name_length check (char_length(full_name) between 2 and 100),
    constraint user_accounts_email_normalized check (email = lower(btrim(email))),
    constraint user_accounts_email_unique unique (email),
    constraint user_accounts_role_valid check (role in ('ADMIN', 'TEACHER', 'STUDENT'))
);
