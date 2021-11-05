CREATE KEYSPACE IF NOT EXISTS test WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1}  AND durable_writes = true;
GRANT SELECT ON KEYSPACE test TO cassandra;
GRANT MODIFY ON KEYSPACE test TO cassandra;