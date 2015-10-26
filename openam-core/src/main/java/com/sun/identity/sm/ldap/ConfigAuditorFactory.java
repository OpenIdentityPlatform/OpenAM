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
 * Copyright 2015 ForgeRock AS.
 */
package com.sun.identity.sm.ldap;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOToken;
import java.util.Map;
import org.forgerock.openam.auditors.SMSAuditor;

/**
 * Guice factory for creating {@code SMSAuditor} objects
 * @since 13.0.0
 */
public interface ConfigAuditorFactory {
    /**
     * Creates an SMSAuditor object for auditing a config change
     * @param runAs The ssoToken representing the user making the change
     * @param realm The realm the change is happening in
     * @param objectId The objectId of the config being altered
     * @param initialState The state before the change has been made
     * @return An SMS Auditor
     */
    SMSAuditor create(@Assisted SSOToken runAs, @Assisted("realm") String realm, @Assisted("objectId") String
            objectId, @Assisted Map<String, Object> initialState);
}
