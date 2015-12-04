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
package com.sun.identity.sm;

import java.util.Iterator;
import javax.security.auth.Subject;

import org.forgerock.openam.audit.AuditConstants.ConfigOperation;
import org.forgerock.openam.auditors.SMSAuditFilter;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.RDN;

/**
 * A filter to stop entitlement index changes being audited
 * @since 13
 */
public class EntitlementIndexConfigFilter implements SMSAuditFilter {

    @Override
    public boolean isAudited(String objectId, String realm, ConfigOperation operation, Subject subject) {
        return !isIndexChange(DN.valueOf(objectId));
    }

    private boolean isIndexChange(DN dn) {
        Iterator<RDN> itr = dn.iterator();
        while (itr.hasNext()) {
            final RDN rdn = itr.next();
            if (rdn.toString().equals("ou=sunEntitlementIndexes")) {
                return true;
            }
        }
        return false;
    }
}
