set autocommit on;
create table sunwam_session (
		    id varchar(100) not null primary key,
		    blob_chunk varbinary(7800),
		    blob_size integer not null,
		    expiration_time double integer not null,
		    uuid varchar(256) not null,
		    sessionstate integer not null,
		    version integer not null default 1
			);
create index sunwam_session_id on sunwam_session(id);
create index sunwam_session_exp_time on sunwam_session(expiration_time);
create index sunwam_session_uuid on sunwam_session(uuid);
create index sunwam_session_sessionstate on sunwam_session(sessionstate);

create table sunwam_session_ext (
         id varchar(100) not null,
         blob_chunk_seq integer not null,
         blob_chunk varbinary(7800),
         expiration_time double integer not null,
         primary key(id,blob_chunk_seq)
		  );
create index sunwam_session_ext_id on sunwam_session_ext(id);
create index sunwam_session_ext_exp_time on sunwam_session_ext(expiration_time);
