package ru.org.openam.cassandra;

import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;

public class CTSConnectionModule extends DataLayerConnectionModule {
	protected CTSConnectionModule(Class<? extends TaskExecutor> executorType, boolean exposesQueueConfiguration) {
        super(executorType, exposesQueueConfiguration);
    }

    public CTSConnectionModule() {
        super();
    }

    @Override
    protected Class<? extends LdapDataLayerConfiguration> getLdapConfigurationType() {
        return DataLayerConfiguration.class;
    }
}
