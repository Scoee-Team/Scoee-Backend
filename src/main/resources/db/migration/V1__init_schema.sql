create table users (
    id bigserial primary key,
    email varchar(255) unique,
    nickname varchar(20) not null,
    status_message varchar(40),
    profile_image_url varchar(255),
    role varchar(20) not null,
    provider varchar(20) not null,
    provider_subject varchar(255) not null,
    created_at timestamp,
    updated_at timestamp
);

create index idx_user_provider_subject on users (provider, provider_subject);

create table refresh_token (
    id bigserial primary key,
    user_id bigint references users(id),
    token_hash varchar(64) not null unique,
    expires_at timestamp not null,
    revoked boolean not null
);

create index idx_refresh_token_hash on refresh_token (token_hash);
create index idx_refresh_token_user on refresh_token (user_id);

create table league (
    id bigserial primary key,
    external_league_id bigint,
    name varchar(255),
    country varchar(255),
    logo_url varchar(255),
    season integer,
    round varchar(255)
);

create index idx_league_external on league (external_league_id);

create table team (
    id bigserial primary key,
    external_team_id bigint,
    name varchar(255),
    short_name varchar(255),
    logo_url varchar(255),
    league_id bigint references league(id)
);

create index idx_team_external on team (external_team_id);

create table football_match (
    id bigserial primary key,
    external_match_id bigint,
    league_id bigint references league(id),
    home_team_id bigint references team(id),
    away_team_id bigint references team(id),
    kickoff_time timestamp not null,
    status varchar(30) not null,
    home_score integer,
    away_score integer,
    venue varchar(255),
    created_at timestamp,
    updated_at timestamp
);

create index idx_match_external on football_match (external_match_id);
create index idx_match_kickoff on football_match (kickoff_time);

create table favorite_league (
    id bigserial primary key,
    user_id bigint references users(id),
    league_id bigint references league(id),
    constraint uk_favorite_league unique (user_id, league_id)
);

create table favorite_team (
    id bigserial primary key,
    user_id bigint references users(id),
    team_id bigint references team(id),
    constraint uk_favorite_team unique (user_id, team_id)
);

create table prediction_room (
    id bigserial primary key,
    title varchar(40) not null,
    type varchar(30) not null,
    host_id bigint references users(id),
    invite_code varchar(12) not null unique,
    deadline_type varchar(30) not null,
    status varchar(30) not null,
    capacity integer not null,
    created_at timestamp,
    updated_at timestamp
);

create index idx_room_invite_code on prediction_room (invite_code);

create table prediction_room_match (
    id bigserial primary key,
    room_id bigint references prediction_room(id),
    football_match_id bigint references football_match(id),
    prediction_deadline timestamp not null
);

create index idx_room_match_room on prediction_room_match (room_id);

create table prediction_room_participant (
    id bigserial primary key,
    room_id bigint references prediction_room(id),
    user_id bigint references users(id),
    joined_at timestamp not null,
    constraint uk_room_participant unique (room_id, user_id)
);

create index idx_participant_room on prediction_room_participant (room_id);
create index idx_participant_user on prediction_room_participant (user_id);

create table score_prediction (
    id bigserial primary key,
    room_id bigint references prediction_room(id),
    user_id bigint references users(id),
    football_match_id bigint references football_match(id),
    predicted_home_score integer not null,
    predicted_away_score integer not null,
    deviation integer,
    submitted_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_prediction_room_user_match unique (room_id, user_id, football_match_id)
);

create index idx_prediction_room on score_prediction (room_id);
create index idx_prediction_user on score_prediction (user_id);
create index idx_prediction_match on score_prediction (football_match_id);

create table prediction_room_result (
    id bigserial primary key,
    room_id bigint unique references prediction_room(id),
    loser_id bigint references users(id),
    loser_total_deviation integer,
    calculated_at timestamp not null
);

create table notification (
    id bigserial primary key,
    user_id bigint references users(id),
    type varchar(50) not null,
    title varchar(255) not null,
    body varchar(500) not null,
    read boolean not null,
    deep_link varchar(255),
    created_at timestamp,
    updated_at timestamp
);

create index idx_notification_user on notification (user_id);

create table notification_setting (
    id bigserial primary key,
    user_id bigint unique references users(id),
    deadline_reminder boolean not null,
    match_result boolean not null,
    friend_joined boolean not null
);
