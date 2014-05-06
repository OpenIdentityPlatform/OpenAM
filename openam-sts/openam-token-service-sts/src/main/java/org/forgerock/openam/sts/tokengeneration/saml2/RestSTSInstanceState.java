package org.forgerock.openam.sts.tokengeneration.saml2;

import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProvider;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceState
 */
public class RestSTSInstanceState implements STSInstanceState {
    private final RestSTSInstanceConfig restStsInstanceConfig;
    private final STSKeyProvider stsKeyProvider;

    /*
    Ctor not guice-injected, as instances created by the RestInstanceStateFactoryImpl.
     */
    RestSTSInstanceState(RestSTSInstanceConfig stsInstanceConfig, STSKeyProvider stsKeyProvider) {
        this.restStsInstanceConfig = stsInstanceConfig;
        this.stsKeyProvider = stsKeyProvider;
    }

    public STSInstanceConfig getConfig() {
        return restStsInstanceConfig;
    }

    public STSKeyProvider getKeyProvider() {
        return stsKeyProvider;
    }
}
