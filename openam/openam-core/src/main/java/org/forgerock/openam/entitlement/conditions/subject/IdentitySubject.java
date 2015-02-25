/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AMIdentitySubject.java,v 1.3 2008/06/25 05:43:50 qcheng Exp $
 *
 */
/*
 * Portions Copyright 2011-2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.subject;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.SubjectAttributesCollector;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.SubjectDecision;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.SubjectEvaluationCache;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.json.JSONArray;
import org.json.JSONException;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class represents an Identity to be used in the entitlements policy engine
 */
public class IdentitySubject implements EntitlementSubject {

    private Set<String> subjectValues = new HashSet<String>();

    private static Debug debug = Debug.getInstance(
            PolicyManager.POLICY_DEBUG_NAME);

    public IdentitySubject() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        Set<String> newState = new HashSet<String>();
        try {
            JSONArray jo = new JSONArray(state);
            for (int i = 0; i < jo.length(); i++) {
                newState.add(jo.getString(i));
            }
        } catch (JSONException e) {
            debug.error("IdentitySubject.setState", e);
        }

        subjectValues = newState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return new JSONArray(subjectValues).toString();
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
    public SubjectDecision evaluate(String realm, SubjectAttributesManager mgr, Subject subject, String resourceName,
            Map<String, Set<String>> environment) throws EntitlementException {
        String tokenID = null;
        String userDN = null;

        SSOToken token = SubjectUtils.getSSOToken(subject);

        if (token != null) {
            Object tokenIDObject = token.getTokenID();
            if (tokenIDObject != null) {
                tokenID = tokenIDObject.toString();
            }
        }

        if (tokenID == null) {
            if (debug.warningEnabled()) {
                debug.warning("IdentitySubject.isMember():"
                        + "tokenID is null");
                debug.warning("IdentitySubject.isMember():"
                        + "returning false");
            }
            return new SubjectDecision(false, Collections.EMPTY_MAP);
        } else {
            Principal principal = null;
            try {
                principal = token.getPrincipal();
            } catch (SSOException e) {
                throw new EntitlementException(508, e);
            }
            if (principal != null) {
                userDN = principal.getName();
            }
            if (userDN == null) {
                if (debug.warningEnabled()) {
                    debug.warning("IdentitySubject.isMember():"
                            + "userDN is null");
                    debug.warning("IdentitySubject.isMember():"
                            + "returning false");
                }
                return new SubjectDecision(false, Collections.EMPTY_MAP);
            }
        }

        boolean listenerAdded = false;
        boolean subjectMatch = false;

        if (debug.messageEnabled()) {
            debug.message("AMIndentitySubject.isMember(): "
                    + "entering with userDN = " + userDN);
        }

        if (subjectValues.size() > 0) {
            Iterator valueIter = subjectValues.iterator();
            while (valueIter.hasNext()) {
                Boolean matchFound = null;

                /* Actually this is universal id of AMIdentity object
                 *
                 */
                String subjectValue = (String) valueIter.next();

                if (debug.messageEnabled()) {
                    debug.message("AMIndentitySubject.isMember(): "
                            + "checking membership with userDN = " + userDN
                            + ", subjectValue = " + subjectValue);
                }

                if ((matchFound = SubjectEvaluationCache.isMember(
                        tokenID, "IdentitySubject", subjectValue)) != null) {
                    if (debug.messageEnabled()) {
                        debug.message("IdentitySubject.isMember():"
                                + "got membership from SubjectEvaluationCache "
                                + " for userDN = " + userDN
                                + ", subjectValue = " + subjectValue
                                + ", result = " + matchFound.booleanValue());
                    }
                    boolean result = matchFound.booleanValue();
                    if (result) {
                        if (debug.messageEnabled()) {
                            debug.message("AMIndentitySubject.isMember(): "
                                    + " returning membership status = "
                                    + result);
                        }
                        return new SubjectDecision(result, Collections.EMPTY_MAP);
                    } else {
                        continue;
                    }
                }

                // got here so entry not in subject evalauation cache
                if (debug.messageEnabled()) {
                    debug.message("IdentitySubject:isMember():entry for "
                            + subjectValue + " not in subject evaluation "
                            + "cache, so compute using IDRepo api");
                }

                try {
                    AMIdentity subjectIdentity = null;
                    subjectIdentity = IdUtils.getIdentity(
                            EntitlementUtils.getAdminToken(), subjectValue);
                    if (subjectIdentity == null) {
                        if (debug.messageEnabled()) {
                            debug.message("IdentitySubject.isMember():"
                                    + "subjectIdentity is null for "
                                    + "subjectValue = " + subjectValue);
                            debug.message("IdentitySubject.isMember():"
                                    + "returning false");
                        }
                        return new SubjectDecision(false, Collections.EMPTY_MAP);
                    }


                    AMIdentity tmpIdentity = IdUtils.getIdentity(token);
                    String univId = IdUtils.getUniversalId(tmpIdentity);
                    AMIdentity userIdentity = IdUtils.getIdentity(
                            EntitlementUtils.getAdminToken(), univId);
                    if (userIdentity == null) {
                        if (debug.messageEnabled()) {
                            debug.message("IdentitySubject.isMember():"
                                    + "userIdentity is null");
                            debug.message("IdentitySubject.isMember():"
                                    + "returning false");
                        }
                        return new SubjectDecision(false, Collections.EMPTY_MAP);
                    }

                    if (debug.messageEnabled()) {
                        debug.message("IdentitySubject.isMember():"
                                + "user uuid = "
                                + IdUtils.getUniversalId(userIdentity)
                                + ", subject uuid = "
                                + IdUtils.getUniversalId(subjectIdentity));
                    }

                    IdType userIdType = userIdentity.getType();
                    IdType subjectIdType = subjectIdentity.getType();
                    Set allowedMemberTypes = null;
                    if (userIdentity.equals(subjectIdentity)) {
                        if (debug.messageEnabled()) {
                            debug.message("IdentitySubject.isMember():"
                                    + "userIdentity equals subjectIdentity:"
                                    + "membership=true");
                        }
                        subjectMatch = true;
                    } else if (
                            ((allowedMemberTypes
                                    = subjectIdType.canHaveMembers()) != null)
                                    && allowedMemberTypes.contains(userIdType)) {
                        subjectMatch = userIdentity.isMember(subjectIdentity);
                        if (debug.messageEnabled()) {
                            debug.message("IdentitySubject.isMember():"
                                    + "userIdentity type " + userIdType +
                                    " can be a member of "
                                    + "subjectIdentityType " + subjectIdType
                                    + ":membership=" + subjectMatch);
                        }
                    } else {
                        subjectMatch = false;
                        if (debug.messageEnabled()) {
                            debug.message("IdentitySubject.isMember():"
                                    + "userIdentity type " + userIdType +
                                    " can not be a member of "
                                    + "subjectIdentityType " + subjectIdType
                                    + ":membership=" + subjectMatch);
                        }
                    }

                    if (debug.messageEnabled()) {
                        debug.message("IdentitySubject.isMember: adding "
                                + "entry in SubjectEvaluationCache for "
                                + ", for userDN = " + userDN
                                + ", subjectValue = " + subjectValue
                                + ", subjectMatch = " + subjectMatch);
                    }
                    SubjectEvaluationCache.addEntry(tokenID,
                            "IdentitySubject", subjectValue, subjectMatch);
                    if (!listenerAdded) {
                        if (!PolicyEvaluator.ssoListenerRegistry.containsKey(
                                tokenID)) {
                            token.addSSOTokenListener(
                                    PolicyEvaluator.ssoListener);
                            PolicyEvaluator.ssoListenerRegistry.put(
                                    tokenID, PolicyEvaluator.ssoListener);
                            if (debug.messageEnabled()) {
                                debug.message("IdentitySubject.isMember():"
                                        + " sso listener added ");
                            }
                            listenerAdded = true;
                        }
                    }
                    if (subjectMatch) {
                        break;
                    }
                } catch (IdRepoException ire) {
                    debug.warning("IdentitySubject.isMember():"
                            + "can not check membership for user "
                            + userDN + ", subject "
                            + subjectValue, ire);
                    String[] args = {userDN, subjectValue};
                    throw new EntitlementException(508, ire);
                } catch (SSOException e) {
                    throw new EntitlementException(508, e);
                }
            }
        }
        if (debug.messageEnabled()) {
            if (!subjectMatch) {
                debug.message("IdentitySubject.isMember(): user " + userDN
                        + " is not a member of this subject");
            } else {
                debug.message("IdentitySubject.isMember(): User " + userDN
                        + " is a member of this subject");
            }
        }
        return new SubjectDecision(subjectMatch, Collections.<String, Set<String>>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIdentity() {
        return true;
    }

    public Set<String> getSubjectValues() {
        return subjectValues;
    }

    public void setSubjectValues(Set<String> subjectValues) {
        this.subjectValues = subjectValues;
    }
}
