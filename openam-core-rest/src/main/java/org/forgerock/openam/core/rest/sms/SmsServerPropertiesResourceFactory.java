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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

/**
 * A Guice factory interface for creating {@link SmsServerPropertiesResource} instances.
 *
 * @since 14.0.0
 */
public interface SmsServerPropertiesResourceFactory {

    /**
     * Create the instance.
     * @param tabName The name of the tab.
     * @param isServerDefault Is this the defaults endpoint.
     * @return The annotated request handler object.
     */
    SmsServerPropertiesResource create(String tabName, boolean isServerDefault);
}
