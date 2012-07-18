# A configuration change is necessary in order to use the session failover mechanism to store tokens.
# The change is an additional line in the amsfo.conf file; cut and paste this into amsfo.conf before starting.
persist_CTS=com.sun.identity.ha.jmqdb.client.SessionRecord
