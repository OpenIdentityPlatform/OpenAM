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
package org.forgerock.openam.uma;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.openam.audit.AuditConstants.ConfigOperation;
import org.forgerock.openam.auditors.SMSAuditFilter;
import java.security.AccessController;
import java.util.Collections;
import java.util.Iterator;
import javax.security.auth.Subject;
import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.openam.entitlement.rest.PolicyStore;
import org.forgerock.openam.entitlement.rest.PrivilegePolicyStoreProvider;
import org.forgerock.openam.entitlement.rest.query.QueryAttribute;
import org.forgerock.openam.entitlement.service.DefaultPrivilegeManagerFactory;
import org.forgerock.openam.entitlement.service.PrivilegeManagerFactory;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.RDN;

/**
 * A filter to stop UMA config changes being audited
 * @since 13.0.0
 */
public class UmaAuditFilter implements SMSAuditFilter {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAudited(String objectId, String realm, ConfigOperation operation, Subject subject) {
        try {
            DN dn = DN.valueOf(objectId);
            if (isPolicy(dn)) {
                return !isUmaPolicy(realm, getPolicyName(dn));
            } else {
                return true;
            }
        } catch (LocalizedIllegalArgumentException | EntitlementException e) {
            return true;
        }
    }

    private String getPolicyName(DN dn) {
        return LDAPUtils.getName(dn);
    }

    private boolean isUmaPolicy(String realm, String policyName) throws EntitlementException {
        return getPrivilege(realm, policyName).getEntitlement().getResourceName().startsWith("uma://");
    }

    private Privilege getPrivilege(String realm, String policyName) throws EntitlementException {
        return getPolicyStore(realm).read(policyName);
    }

    private PolicyStore getPolicyStore(String realm) {
        return getPrivilegePolicyStoreProvider().getPolicyStore(getAdminSubject(), realm);
    }

    private Subject getAdminSubject() {
        return SubjectUtils.createSubject(AccessController.doPrivileged(AdminTokenAction.getInstance()));
    }

    private PrivilegePolicyStoreProvider getPrivilegePolicyStoreProvider() {
        PrivilegeManagerFactory factory = new DefaultPrivilegeManagerFactory();
        return new PrivilegePolicyStoreProvider(factory,
                Collections.<String, QueryAttribute>emptyMap());
    }

    private boolean isPolicy(DN dn) {
        final Iterator<RDN> itr = dn.iterator();
        while (itr.hasNext()) {
            final RDN rdn = itr.next();
            if (rdn.toString().equalsIgnoreCase("ou=Policies")) {
                return true;
            }
        }
        return false;
    }
}
