DROP TABLE reqtoken;
DROP TABLE acctoken;
DROP TABLE consumer;

-- Service Consumer
CREATE TABLE consumer (
    id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cons_name VARCHAR(128),
    cons_secret VARCHAR(1024),
    cons_rsakey VARCHAR(2048),
    cons_key VARCHAR(1024)
);

-- Request token
CREATE TABLE reqtoken (
    id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reqt_uri VARCHAR(2048),
    reqt_val VARCHAR(8096),
    reqt_secret VARCHAR(1024),
    reqt_ppalid VARCHAR(1024),
    reqt_lifetime TIMESTAMP,
    consumer_id INTEGER NOT NULL REFERENCES consumer(id) ON DELETE CASCADE
);


-- Consumer registration
CREATE TABLE acctoken (
    id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    acct_uri VARCHAR(2048),
    acct_val VARCHAR(8096),
    acct_secret VARCHAR(1024),
    acct_ppalid VARCHAR(1024),
    acct_lifetime TIMESTAMP,
    acct_onetime SMALLINT,
    consumer_id INTEGER NOT NULL REFERENCES consumer(id) ON DELETE CASCADE
);
