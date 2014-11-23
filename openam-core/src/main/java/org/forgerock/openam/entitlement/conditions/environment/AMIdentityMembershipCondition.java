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
 * Copyright 2007 Sun Microsystems Inc
 */
/*
 * Portions Copyright 2011-2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.entitlement.EntitlementException.*;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.AM_IDENTITY_NAME;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.INVOCATOR_PRINCIPAL_UUID;

/**
 * An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that will check whether the
 * principal has the specified memberships.
 *
 * @since 12.0.0
 */
public class AMIdentityMembershipCondition extends EntitlementConditionAdaptor {

    private final Debug debug;
    private final CoreWrapper coreWrapper;

    private Set<String> amIdentityName = new HashSet<String>();

    /**
     * Constructs a new AMIdentityMembershipCondition instance.
     */
    public AMIdentityMembershipCondition() {
        this(PrivilegeManager.debug, new CoreWrapper());
    }

    /**
     * Constructs a new AMIdentityMembershipCondition instance.
     *
     * @param debug A Debug instance.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    AMIdentityMembershipCondition(Debug debug, CoreWrapper coreWrapper) {
        this.debug = debug;
        this.coreWrapper = coreWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            JSONArray values = jo.getJSONArray(AM_IDENTITY_NAME);
            for (int i = 0; i < values.length(); i++) {
                amIdentityName.add(values.getString(i));
            }
        } catch (JSONException e) {
            debug.message("AMIdentityMembershipCondition: Failed to set state", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return toString();
    }

    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        if (debug.messageEnabled()) {
            debug.message("At AMIdentityMembershipCondition.getConditionDecision(): entering, names:" + amIdentityName);
            debug.message("At AMIdentityMembershipCondition.getConditionDecision(): environment.invocatorPrincipalUud:"
                    + env.get(INVOCATOR_PRINCIPAL_UUID));
        }
        boolean isMember = false;
        Set<String> invocatorUuidSet = env.get(INVOCATOR_PRINCIPAL_UUID);
        if (invocatorUuidSet != null && !invocatorUuidSet.isEmpty()) {
            String invocatorUuid = invocatorUuidSet.iterator().next();
            isMember = isMember(invocatorUuid);
        } else {
            debug.message("At AMIdentityMembershipCondition.getConditionDecision(): invocatorUuidSet is null or empty");
        }
        return new ConditionDecision(isMember, Collections.<String, Set<String>>emptyMap());
    }


    /**
     * Determines if the user is a member of this instance of the {@code Subject} object.
     *
     * @param invocatorUuid UUID of the user.
     * @return {@code true} if the user is member of this subject.
     * @throws EntitlementException If SSO token is not valid or if an error occured while checking if the user is a
     * member of this subject
     */
    private boolean isMember(String invocatorUuid) throws EntitlementException {

        boolean subjectMatch = false;

        if (invocatorUuid == null) {
            debug.warning("AMIdentityMembershipCondition.isMember():invocatorUuid is null");
            debug.warning("AMIdentityMembershipCondition.isMember():returning false");
            return false;
        }

        if (debug.messageEnabled()) {
            debug.warning("AMIdentityMembershipCondition.isMember():invocatorUuid:" + invocatorUuid);
        }

        if (!amIdentityName.isEmpty()) {
            for (String nameValue : amIdentityName) {

                if (debug.messageEnabled()) {
                    debug.message("AMIndentityMembershipCondition.isMember(): checking membership with nameValue = "
                            + nameValue + ", invocatorUuid = " + invocatorUuid);
                }

                try {
                    AMIdentity invocatorIdentity = coreWrapper.getIdentity(getAdminToken(), invocatorUuid);
                    if (invocatorIdentity == null) {
                        if (debug.messageEnabled()) {
                            debug.message("AMidentityMembershipCondition.isMember():invocatorIdentity is null for "
                                            + "invocatorUuid = " + invocatorUuid);
                            debug.message("AMidentityMembershipCondition.isMember():returning false");
                        }
                        return false;
                    }

                    AMIdentity nameValueIdentity = coreWrapper.getIdentity(getAdminToken(), nameValue);
                    if (nameValueIdentity == null) {
                        if (debug.messageEnabled()) {
                            debug.message("AMidentityMembershipCondition.isMember():nameValueidentity is null for "
                                            + "nameValue = " + nameValue);
                            debug.message("AMidentityMembershipCondition.isMember():returning false");
                        }
                        return false;
                    }

                    IdType invocatorIdType = invocatorIdentity.getType();
                    IdType nameValueIdType = nameValueIdentity.getType();
                    Set allowedMemberTypes;
                    if (invocatorIdentity.equals(nameValueIdentity)) {
                        if (debug.messageEnabled()) {
                            debug.message("AMidentityMembershipCondition.isMember():invocatorIdentity equals "
                                            + " nameValueIdentity:membership=true");
                        }
                        subjectMatch = true;
                    } else if ((allowedMemberTypes = nameValueIdType.canHaveMembers()) != null
                            && allowedMemberTypes.contains(invocatorIdType)) {
                        subjectMatch = invocatorIdentity.isMember(nameValueIdentity);
                        if (debug.messageEnabled()) {
                            debug.message("AMIdentityMembershipCondition.isMember():invocatorIdentityType "
                                            + invocatorIdType + " can be a member of nameValueIdentityType "
                                            + nameValueIdType + ":membership=" + subjectMatch);
                        }
                    } else {
                        subjectMatch = false;
                        if (debug.messageEnabled()) {
                            debug.message("AMIdentityMembershipCondition.isMember():invocatoridentityType "
                                            + invocatorIdType + " can be a member of nameValueIdentityType "
                                            + nameValueIdType + ":membership=" + subjectMatch);
                        }
                    }
                    if (subjectMatch) {
                        break;
                    }
                } catch (IdRepoException ire) {
                    if (debug.warningEnabled()) {
                        debug.warning("AMIdentityMembershipCondition.isMember():can not check membership for invocator "
                                + invocatorUuid + ", nameValue " + nameValue, ire);
                    }
                    String[] args = {invocatorUuid, nameValue};
                    throw new EntitlementException(AM_ID_SUBJECT_MEMBERSHIP_EVALUATION_ERROR, args);
                } catch (SSOException e) {
                    debug.error("AMIdentityMembershipCondition: Condition evaluation failed", e);
                    throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("AMIdentityMembershipCondition.isMember():invocatorUuid=" + invocatorUuid + ",amIdentityName="
                    + amIdentityName + ",subjectMatch=" + subjectMatch);
        }
        return subjectMatch;
    }

    private SSOToken getAdminToken() throws EntitlementException {
        SSOToken token = coreWrapper.getAdminToken();
        if (token == null) {
            throw new EntitlementException(INVALID_ADMIN);
        }
        return token;
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        JSONArray values = new JSONArray();
        for (String value : amIdentityName) {
            values.put(value);
        }
        jo.put(AM_IDENTITY_NAME, values);
        return jo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("AMIdentityMembershipCondition.toString()", e);
        }
        return s;
    }

    public Set<String> getAmIdentityName() {
        return amIdentityName;
    }

    public void setAmIdentityNames(Set<String> nameValues) {
        this.amIdentityName = nameValues;
    }

    @Override
    public void validate() throws EntitlementException {
        if (amIdentityName == null || amIdentityName.isEmpty()) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, AM_IDENTITY_NAME);
        }
        if (StringUtils.isAnyBlank(amIdentityName)) {
            throw new EntitlementException(EntitlementException.PROPERTY_CONTAINS_BLANK_VALUE, AM_IDENTITY_NAME);
        }
    }
}
