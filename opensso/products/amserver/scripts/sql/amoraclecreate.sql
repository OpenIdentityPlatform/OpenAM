create table SCHEMANAME.sunwam_session 
      (id varchar2(100) not null primary key, 
       blob_chunk LONG RAW, 
       blob_size number not null,
       expiration_time number not null,
       uuid varchar2(256) not null,
       sessionstate number not null,
       version number default 1 not null
) tablespace TABLESPCNAME;

create index SCHEMANAME.sunwam_session_exp_time on SCHEMANAME.sunwam_session(expiration_time) tablespace TABLESPCNAME;
create index SCHEMANAME.sunwam_session_uuid on SCHEMANAME.sunwam_session(uuid) tablespace TABLESPCNAME;
create index SCHEMANAME.sunwam_session_sessionstate on SCHEMANAME.sunwam_session(sessionstate) tablespace TABLESPCNAME;

create table SCHEMANAME.sunwam_session_ext 
      (id varchar2(100) not null,
       blob_chunk_seq number not null, 
       blob_chunk LONG RAW,
       expiration_time number not null,
       primary key(id,blob_chunk_seq)
) tablespace TABLESPCNAME;

create index SCHEMANAME.sunwam_session_ext_exp_time on SCHEMANAME.sunwam_session_ext(expiration_time) tablespace TABLESPCNAME;

exit;
