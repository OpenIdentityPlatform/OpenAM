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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest;

import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.sts.STSInitializationException;

/**
 * This interface encapsulates the concerns of registering a ServiceListener with the ServiceConfigManager. This encapsulation
 * allows this concern to be injected/mocked, thereby facilitating testability.
 *
 * This interface and the corresponding implementation would seem to be candidates for inclusion in openam-common, as
 * it will be used in both the rest-sts and the token-generation-service, but openam-common cannot depend upon
 * openam-core (which is where ServiceListener is defined) because a dependency upon openam-core pulls in all of the
 * archaic web-service libraries, which prevent the cxf-sts from functioning.
 */
public interface ServiceListenerRegistration {
    void registerServiceListener(String serviceName, String serviceVersion, ServiceListener listener)
            throws STSInitializationException;
}
