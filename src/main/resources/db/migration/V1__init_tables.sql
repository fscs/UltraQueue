CREATE TABLE song_logs
(
    log_id    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    song_id   uuid,
    played_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE songs
(
    song_id      uuid PRIMARY KEY,
    title       varchar(500),
    artist      varchar(500),
    language    varchar(100),
    year        integer,
    length      integer,
    genre       varchar(500),
    title_artist varchar(1002)
);

CREATE TABLE queue_entries
(
    id        uuid PRIMARY KEY,
    song_id    uuid REFERENCES songs (song_id),
    user_id    varchar(40),
    username  varchar(100),
    user_color varchar(50),
    position  integer
);
