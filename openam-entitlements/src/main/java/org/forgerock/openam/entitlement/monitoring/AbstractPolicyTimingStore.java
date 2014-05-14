/*
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
*
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.entitlement.monitoring;

import org.forgerock.openam.shared.monitoring.AbstractTimingStore;

/**
 * The abstract policy timing store, which will be extended by implementations which require a duration store.
 */
public abstract class AbstractPolicyTimingStore extends AbstractTimingStore {

    /**
     * Abstract constructor, taking the configuration wrapper used to communicate with OpenAM's
     * current policy monitoring settings.
     *
     * @param wrapper non-static wrapper to the {@link com.sun.identity.entitlement.EntitlementConfiguration}.
     */
    public AbstractPolicyTimingStore(EntitlementConfigurationWrapper wrapper) {
        super(wrapper.getPolicyWindowSize());
    }

}
