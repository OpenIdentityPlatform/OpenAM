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
package org.forgerock.openam.core.rest;

import static com.sun.identity.idsvcs.opensso.IdentityServicesImpl.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.selfservice.core.SelfServiceContext;
import org.forgerock.services.context.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IdentityRestUtils {

    public static final String USER_TYPE = "user";
    public static final String GROUP_TYPE = "group";
    public static final String AGENT_TYPE = "agent";

    public static final String UNIVERSAL_ID = "universalid";
    public static final String FIELD_MAIL = "mail";
    public static final String REALM = "realm";

    public static final String USER_KBA_ATTRIBUTE = "kbaInformation";

    public static final String USERNAME = "username";

    private static final Debug debug = Debug.getInstance("frRest");

    private IdentityRestUtils() {
    }

    public static void changePassword(Context serverContext, String realm, String username, String oldPassword,
            String newPassword) throws ResourceException {
        try {
            SSOToken token = serverContext.asContext(SSOTokenContext.class).getCallerSSOToken();
            AMIdentity userIdentity = new AMIdentity(token, username, IdType.USER, realm, null);
            userIdentity.changePassword(oldPassword, newPassword);
        } catch (SSOException ssoe) {
            debug.warning("IdentityRestUtils.changePassword() :: SSOException occurred while changing "
                    + "the password for user: " + username, ssoe);
            throw new PermanentException(401, "An error occurred while trying to change the password", ssoe);
        } catch (IdRepoException ire) {
            if (IdRepoBundle.ACCESS_DENIED.equals(ire.getErrorCode())) {
                throw new ForbiddenException("The user is not authorized to change the password");
            } else {
                debug.warning("IdentityRestUtils.changePassword() :: IdRepoException occurred while "
                        + "changing the password for user: " + username, ire);
                throw new InternalServerErrorException("An error occurred while trying to change the password", ire);
            }
        }
    }

    public static Map<String, Set<String>> getIdentityServicesAttributes(String realm, String objectType) {
        Map<String, Set<String>> identityServicesAttributes = new HashMap<>();
        identityServicesAttributes.put("objecttype", Collections.singleton(objectType));
        identityServicesAttributes.put("realm", Collections.singleton(realm));
        return identityServicesAttributes;
    }

    public static SSOToken getSSOToken(String ssoTokenId) throws SSOException {
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        return mgr.createSSOToken(ssoTokenId);
    }

    /**
     * Convert an {@link IdentityDetails} object into a {@link JsonValue}.  Package private for IdentityResourceV2.
     *
     * @param details The IdentityDetails
     * @return The JsonValue
     */
    public static JsonValue identityDetailsToJsonValue(IdentityDetails details) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>());
        try {
            result.put(USERNAME, details.getName());
            result.put("realm", details.getRealm());
            Map<String, Set<String>> attrs = asMap(details.getAttributes());

            for (Map.Entry<String, Set<String>> entry : attrs.entrySet()) {

                // Handle the KBA attribute especially.  This originally came from "outside" OpenAM and was passed to
                // us as JSON.  We took the JSON and (via toString) turned it into text.  Now we take the text and
                // turn it back into JSON.  This is all ok because we're not required to understand it, just pass it
                // back and forth.
                if (entry.getKey().equals(USER_KBA_ATTRIBUTE)) {
                    List<Object> kbaChildren = new ArrayList<>();
                    for (String kbaString : entry.getValue()) {
                        JsonValue kbaValue = JsonValueBuilder.toJsonValue(kbaString);
                        kbaChildren.add(kbaValue.getObject());
                    }
                    result.put(USER_KBA_ATTRIBUTE, kbaChildren);
                } else {
                    result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
            return result;
        } catch (final Exception e) {
            throw new JsonValueException(result);
        }
    }

    /**
     * When an instance of a user is created via self service, we impose additional rules for security purposes.
     * Namely, we strictly apply a whitelist of valid attribute names to each attribute in the incoming JSON
     * representation of the user object.  This ensures a hacker can't manipulate the request and thereby pretend
     * to be a manager, demigod or individual they are not.
     *
     * There is no return value.  If you survive calling this function without an exception being thrown, there
     * are no illegal values in the incoming JSON
     *
     * @param context The context
     * @param request The request
     * @param objectType The type of object we're creating, user, group, etc.
     * @param validUserAttributes The set of valid user attributes
     * @throws BadRequestException If any attribute is found in the JSON representation of the user object containing
     * an attribute that is not in our whitelist
     */
    public static void enforceWhiteList(final Context context, final JsonValue jsonValue,
                                        final String objectType, final Set<String> validUserAttributes)
            throws BadRequestException {

        if (!context.containsContext(SelfServiceContext.class) || !objectType.equals(USER_TYPE)) {
            return;
        }

        final String realm = RealmContext.getRealm(context);

        if (validUserAttributes == null || validUserAttributes.isEmpty()) {
            throw new BadRequestException("Null/empty whitelist of valid attributes for self service user creation");
        }

        IdentityDetails identityDetails = jsonValueToIdentityDetails(objectType, jsonValue, realm);
        Attribute[] attributes = identityDetails.getAttributes();
        for (Attribute attribute : attributes) {
            if (!validUserAttributes.contains(attribute.getName())) {
                throw new BadRequestException("User attribute "
                        + attribute.getName()
                        + " is not valid for self service creation");
            }
        }
    }

    /**
     * Returns an IdentityDetails from a JsonValue.
     *
     * @param objectType the object type, eg. user, group, etc.
     * @param jVal The JsonValue Object to be converted
     * @param realm The realm
     * @return The IdentityDetails object
     */
    public static IdentityDetails jsonValueToIdentityDetails(final String objectType,
                                                             final JsonValue jVal,
                                                             final String realm) {

        IdentityDetails identity = new IdentityDetails();
        Map<String, Set<String>> identityAttrList = new HashMap<>();

        try {
            identity.setType(objectType); //set type ex. user
            identity.setRealm(realm); //set realm
            identity.setName(jVal.get(USERNAME).asString());//set name from JsonValue object

            if (AGENT_TYPE.equals(objectType)) {
                jVal.remove(USERNAME);
                jVal.remove(REALM);
                jVal.remove(UNIVERSAL_ID);
            }

            try {
                for (String s : jVal.keys()) {
                    JsonValue childValue = jVal.get(s);
                    if (childValue.isString()) {
                        identityAttrList.put(s, Collections.singleton(childValue.asString()));
                    } else if (childValue.isList()) {
                        List<String> list = new ArrayList<>();
                        for (Object item : childValue.asList()) {
                            if (item instanceof Map) {
                                JsonValue json = new JsonValue(item);
                                list.add(json.toString());
                            } else {
                                list.add(item.toString());
                            }
                        }
                        identityAttrList.put(s, new HashSet<>(list));
                    }
                }
            } catch (Exception e) {
                debug.error("IdentityResource.jsonValueToIdentityDetails() :: Cannot Traverse JsonValue. ", e);
            }
            identity.setAttributes(asAttributeArray(identityAttrList));

        } catch (final Exception e) {
            debug.error("IdentityResource.jsonValueToIdentityDetails() :: Cannot convert JsonValue to IdentityDetails" +
                    ".", e);
            //deal with better exceptions
        }
        return identity;
    }
}
