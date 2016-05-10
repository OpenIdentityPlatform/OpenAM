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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.examples;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.SubjectAttributesCollector;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.SubjectDecision;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Sample policy subject condition to check the subject user name
 * matches the string provided in the configuration.
 */
public class SampleSubjectType implements EntitlementSubject {

    public static final String NAME_FIELD = "name";
    private String name;

    /**
     * The authorized subject's user name.
     * @return  The authorized subject's user name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the authorized subject's user name.
     * @param name  The authorized subject's user name.
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setState(String state) {
        try {
            final JSONObject json = new JSONObject(state);
            this.setName(json.getString(NAME_FIELD));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getState() {
        try {
            final JSONObject json = new JSONObject();
            json.put(NAME_FIELD, getName());
            return json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Set<String>> getSearchIndexAttributes() {
        return Collections.singletonMap(
                SubjectAttributesCollector.NAMESPACE_IDENTITY,
                Collections.singleton(
                        SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES));
    }

    @Override
    public Set<String> getRequiredAttributeNames() {
        return Collections.emptySet();
    }

    @Override
    public SubjectDecision evaluate(String realm,
                                    SubjectAttributesManager mgr,
                                    Subject subject,
                                    String resourceName,
                                    Map<String, Set<String>> environment)
            throws EntitlementException {

        boolean authorized = false;

        for (Principal principal : subject.getPrincipals()) {

            String userDn = principal.getName();

            int start = userDn.indexOf('=');
            int end   = userDn.indexOf(',');
            if (end <= start) {
                throw new EntitlementException(
                        EntitlementException.CONDITION_EVALUATION_FAILED,
                        "Name is not a valid DN: " + userDn);
            }

            String userName = userDn.substring(start + 1, end);

            if (userName.equals(getName())) {
                authorized = true;
            }

        }

        return new SubjectDecision(authorized, Collections.EMPTY_MAP);
    }

    @Override
    public boolean isIdentity() {
        return true;
    }
}
