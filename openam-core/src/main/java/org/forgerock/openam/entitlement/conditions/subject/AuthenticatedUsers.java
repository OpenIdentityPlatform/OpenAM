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
 * Copyright 2006 Sun Microsystems Inc
 */
/*
 * Portions Copyright 2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.subject;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.SubjectAttributesCollector;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.SubjectDecision;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This subject applies to all users with valid <code>SSOToken</code>.
 */
public class AuthenticatedUsers implements EntitlementSubject {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        //this object has no state
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return new JSONObject().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY,
                new HashSet<String>(Arrays.asList(SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES)));
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getRequiredAttributeNames() {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubjectDecision evaluate(String realm, SubjectAttributesManager mgr, javax.security.auth.Subject subject,
            String resourceName, Map<String, Set<String>> environment) throws EntitlementException {
        SSOToken token = SubjectUtils.getSSOToken(subject);
        try {
            return new SubjectDecision(SSOTokenManager.getInstance().isValidToken(token),
                    Collections.<String, Set<String>>emptyMap());
        } catch (SSOException e) {
            throw new EntitlementException(508, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIdentity() {
        return true;
    }

}
