/**
 * Copyright 2013 ForgeRock AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package org.forgerock.openam.cts;

import javax.inject.Inject;
import com.sun.identity.common.configuration.ConfigurationListener;
import org.forgerock.openam.cts.impl.CTSConnectionFactory;
import org.forgerock.openam.cts.impl.LDAPConfig;

/**
 * Responsible for controlling the sequence of updates for the CTS connection configuration.
 * Once either configuration has changed we may or may not need to update the connection.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenConfigListener implements ConfigurationListener {
    private final LDAPConfig ldapConfig;
    private final ExternalTokenConfig externalTokenConfig;
    private final CTSConnectionFactory connectionFactory;

    @Inject
    public CoreTokenConfigListener(LDAPConfig ldapConfig, ExternalTokenConfig externalTokenConfig,
                                   CTSConnectionFactory connectionFactory) {
        this.ldapConfig = ldapConfig;
        this.externalTokenConfig = externalTokenConfig;
        this.connectionFactory = connectionFactory;
    }

    /**
     * {@inheritDoc}
     *
     * Triggers the CoreTokenConfig and the ExternalTokenConfig in a predictable order followed by
     * calling the CTSConnectionFactory to ensure the connection is updated if required.
     */
    public void notifyChanges() {
        ldapConfig.update();
        externalTokenConfig.update();
        connectionFactory.updateConnection();
    }
}
