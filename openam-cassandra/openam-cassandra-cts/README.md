## HowTo setup cassandra CTS

Ininitialize [schema.cqlh](https://github.com/OpenIdentityPlatform/OpenAM/openam-cassandra/openam-cassandra-cts/src/test/resources/schema.cqlsh) and setup java property:
* -Dorg.forgerock.openam.sm.datalayer.module.CTS_ASYNC=org.openidentityplatform.openam.cassandra.CTSAsyncConnectionModule;
* -Dorg.forgerock.openam.sm.datalayer.module.CTS_MAX_SESSION_TIMEOUT_WORKER=org.openidentityplatform.openam.cassandra.CTSConnectionModule;
* -Dorg.forgerock.openam.sm.datalayer.module.CTS_SESSION_IDLE_TIMEOUT_WORKER=org.openidentityplatform.openam.cassandra.CTSConnectionModule;
* -Dorg.forgerock.openam.sm.datalayer.module.CTS_EXPIRY_DATE_WORKER=org.openidentityplatform.openam.cassandra.CTSConnectionModule;
* -Dorg.forgerock.services.cts.store.root.suffix=cts.tokens;