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
package org.forgerock.openam.auditors;

import javax.security.auth.Subject;
import org.forgerock.openam.audit.AuditConstants.ConfigOperation;

/**
 * An interface for creating filters that block specific config changes from being audited
 *
 * @since 13.0.0
 */
public interface SMSAuditFilter {

    /**
     * An interface for implementing audit filters.
     *
     * @param objectId The id of the element being configured (e.g. the DN)
     * @param realm Realm of the element being configured
     * @param operation The operation being performed (e.g. create, modify..)
     * @param subject The subject the operation is being performed as (not necessarily the logged in user)
     * @return Whether or not an audit entry should be made for this config change
     */
    boolean isAudited(String objectId, String realm, ConfigOperation operation, Subject subject);

}

