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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2023-2026 3A Systems LLC
 */
package org.forgerock.openam.core.rest;

import static com.sun.identity.idsvcs.opensso.IdentityServicesImpl.asAttributeArray;
import static com.sun.identity.idsvcs.opensso.IdentityServicesImpl.asMap;
import static org.forgerock.openam.core.rest.UserAttributeInfo.UserAttributeInfoBuilder;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.ListWrapper;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.collections4.CollectionUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.errors.IdentityResourceExceptionMappingHandler;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.selfservice.core.SelfServiceContext;
import org.forgerock.services.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class IdentityRestUtils {

    public static final String USER_TYPE = "user";
    public static final String GROUP_TYPE = "group";
    public static final String AGENT_TYPE = "agent";

    public static final String UNIVERSAL_ID = "universalid";
    public static final String FIELD_MAIL = "mail";
    public static final String REALM = "realm";

    public static final String USER_KBA_ATTRIBUTE = "kbaInfo";

    public static final String USERNAME = "username";

    private static final List<String> PASSWORD_ATTRIBUTES = Arrays.asList("userpassword", "unicodepwd");

    private static final List<String> MEMBEROF_ATTRIBUTES = Arrays.asList("memberof");
    private static final Debug debug = Debug.getInstance("frRest");

    private static final IdentityResourceExceptionMappingHandler RESOURCE_MAPPING_HANDLER =
            InjectorHolder.getInstance(IdentityResourceExceptionMappingHandler.class);

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
            throw RESOURCE_MAPPING_HANDLER.handleError(ire);
        }
    }

    public static void changeMemberships(Context serverContext, String realm, String username, Set<String> groupNames)
            throws ResourceException {
        try {
            SSOToken token = serverContext.asContext(SSOTokenContext.class).getCallerSSOToken();

            //check if groups exist
            for(String gn : groupNames) {
                AMIdentity groupIdentity = new AMIdentity(token, gn, IdType.GROUP, realm, null);
                if(!groupIdentity.isExists()) {
                    throw new SSOException("group " + gn + " does not exist");
                }
            }

            AMIdentity userIdentity = new AMIdentity(token, username, IdType.USER, realm, null);

            Set<String> membershipsToAdd = new HashSet<>(groupNames);
            Set<String> membershipsToRemove = new HashSet<>();
            Set<AMIdentity> currentMembershipsIdentity = userIdentity.getMemberships(IdType.GROUP);
            final Set<String> currentMemberships = CollectionUtils.isNotEmpty(currentMembershipsIdentity)
                    ? currentMembershipsIdentity.stream().map(AMIdentity::getName).collect(Collectors.toSet())
                    : null;

            if (!CollectionUtils.isEmpty(currentMemberships)) {
                membershipsToRemove = currentMemberships.stream().filter(cm -> !groupNames.contains(cm)).collect(Collectors.toSet());
                membershipsToAdd = groupNames.stream().filter(m -> !currentMemberships.contains(m)).collect(Collectors.toSet());
            }

            if (!CollectionUtils.isEmpty(membershipsToRemove)) {
                for (String idName : membershipsToRemove) {
                    AMIdentity groupIdentity = new AMIdentity(token, idName, IdType.GROUP, realm, null);
                    groupIdentity.removeMember(userIdentity);
                }
            }

            if (!CollectionUtils.isEmpty(membershipsToAdd)) {
                for (String idName : membershipsToAdd) {
                    AMIdentity groupIdentity = new AMIdentity(token, idName, IdType.GROUP, realm, null);
                    groupIdentity.addMember(userIdentity);
                }
            }

        } catch (SSOException ssoe) {
            debug.warning("IdentityRestUtils.changeMemberships() :: SSOException occurred while changing "
                    + "the groups for user: " + username, ssoe);
            throw new PermanentException(401, "An error occurred while trying to change the user groups", ssoe);
        } catch (IdRepoException ire) {
            throw RESOURCE_MAPPING_HANDLER.handleError(ire);
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
            result.put("realm", new CoreWrapper().convertOrgNameToRealmName(details.getRealm()));
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
                } else if (isPasswordAttribute(entry.getKey())) {
                    continue;
                } else {
                    result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
            return result;
        } catch (final Exception e) {
            throw new JsonValueException(result);
        }
    }

    private static boolean isPasswordAttribute(String attributeName) {
        return PASSWORD_ATTRIBUTES.contains(attributeName.toLowerCase(Locale.ROOT));
    }

    /**
     * When an instance of a user is created via self service, we impose additional rules for security purposes.
     * Namely, we strictly apply a whitelist of valid attribute names to each attribute, and to any group
     * membership, in the incoming JSON representation of the user object.  This ensures a hacker can't manipulate
     * the request and thereby pretend to be a manager, demigod or individual they are not.
     *
     * There is no return value.  If you survive calling this function without an exception being thrown, there
     * are no illegal values in the incoming JSON
     *
     * @param context The context
     * @param jsonValue The request
     * @param objectType The type of object we're creating, user, group, etc.
     * @param validUserAttributes The set of valid user attributes
     * @throws BadRequestException If the JSON representation of the user object cannot be converted into an
     * identity, or contains an attribute or a group membership that is not in our whitelist
     */
    public static void enforceWhiteList(final Context context, final JsonValue jsonValue,
                                        final String objectType, final Set<String> validUserAttributes)
            throws BadRequestException {

        if (!appliesTo(context)) {
            return;
        }

        enforceWhiteList(RealmContext.getRealm(context).asPath(), jsonValue, objectType, validUserAttributes);
    }

    /**
     * The whitelist applies to what the self service framework creates on behalf of an anonymous user, which is what
     * the {@link SelfServiceContext} marks.  An authenticated caller creating an identity is restricted by its own
     * privileges instead.
     *
     * @param context The context of the request
     * @return Whether the whitelist has anything to say about this request
     */
    private static boolean appliesTo(final Context context) {
        return context.containsContext(SelfServiceContext.class);
    }

    /**
     * Removes every spelling of an attribute from a request.
     *
     * The identity repository matches attribute names case insensitively - {@literal DJLDAPv3Repo} wraps them in a
     * {@link com.sun.identity.common.CaseInsensitiveHashMap}, and the directory does the same - so removing only the
     * key as spelled here would leave a case variant of it behind to be created under the very name that was
     * supposed to have been taken out.
     *
     * @param jsonValue The request content
     * @param attributeName The attribute to remove, in any casing
     */
    public static void removeIgnoringCase(final JsonValue jsonValue, final String attributeName) {
        if (!jsonValue.isMap()) {
            return;
        }

        for (String key : new ArrayList<>(jsonValue.keys())) {
            if (key.equalsIgnoreCase(attributeName)) {
                jsonValue.remove(key);
            }
        }
    }

    /**
     * Applies the whitelist of valid attribute names regardless of the calling context.
     *
     * The context based overload above only applies the whitelist to requests carrying a
     * {@link SelfServiceContext}, which the self service framework adds to the requests it makes on behalf of an
     * anonymous user.  Endpoints that are anonymous in their own right, such as the legacy
     * {@literal anonymousCreate} action, never carry that context, so they have to ask for the whitelist explicitly.
     *
     * @param realm The realm the identity is being created in
     * @param jsonValue The JSON representation of the identity to create
     * @param objectType The type of object we're creating, user, group, etc.
     * @param validUserAttributes The set of valid user attributes
     * @throws BadRequestException If the JSON representation of the user object cannot be converted into an
     * identity, or contains an attribute or a group membership that is not in our whitelist
     */
    public static void enforceWhiteList(final String realm, final JsonValue jsonValue, final String objectType,
                                        final Set<String> validUserAttributes) throws BadRequestException {

        if (!objectType.equals(USER_TYPE)) {
            return;
        }

        // Not reachable through the SMS supplier - selfService.xml ships seven default values for the setting and
        // the config handler will not hand back a whitelist without them - but this method takes the set from its
        // caller, and an empty one would otherwise let a body with no attributes at all through: the per attribute
        // loop below has nothing to reject when jsonValueToIdentityDetails produced none.
        if (validUserAttributes == null || validUserAttributes.isEmpty()) {
            throw new BadRequestException("Null/empty whitelist of valid attributes for self service user creation");
        }

        // The identity repository matches attribute names case insensitively, so a case sensitive whitelist would
        // accept or reject one and the same attribute depending on how the caller happened to spell it.
        Set<String> whiteList = new HashSet<>(validUserAttributes.size());
        for (String validUserAttribute : validUserAttributes) {
            whiteList.add(validUserAttribute.toLowerCase(Locale.ROOT));
        }

        // Group membership does not turn up as an attribute - jsonValueToIdentityDetails diverts an exactly
        // lower case memberof into the identity's group list - so it has to be checked here or it would bypass the
        // whitelist altogether.  Checking it first, and case insensitively, means every spelling of memberof is
        // answered by this check rather than by whichever of the two happens to see it first.
        if (jsonValue.isMap()) {
            for (String key : jsonValue.keys()) {
                String attributeName = key.toLowerCase(Locale.ROOT);
                if (MEMBEROF_ATTRIBUTES.contains(attributeName) && !whiteList.contains(attributeName)) {
                    throw new BadRequestException("Group membership "
                            + key
                            + " is not valid for self service creation");
                }
            }
        }

        IdentityDetails identityDetails = jsonValueToIdentityDetails(objectType, jsonValue, realm);
        Attribute[] attributes = identityDetails.getAttributes();

        // jsonValueToIdentityDetails swallows a malformed user object and returns before it sets the attributes, so
        // a null here means there is nothing left to check rather than that there is nothing to check.  Refusing the
        // request keeps an unconvertible body from passing the whitelist by having no attributes at all.
        if (attributes == null) {
            throw new BadRequestException("Invalid user object for self service creation");
        }

        for (Attribute attribute : attributes) {
            if (!whiteList.contains(attribute.getName().toLowerCase(Locale.ROOT))) {
                throw new BadRequestException("User attribute "
                        + attribute.getName()
                        + " is not valid for self service creation");
            }
        }
    }

    /**
     * Applies the self service whitelist of valid creation attributes to an anonymous self-registration request.
     *
     * Shared by both versions of the users endpoint on purpose: the gap this closes was itself duplicated across
     * them, so keeping the check in one place is what stops the two from drifting apart again.
     *
     * @param configHandler The handler the whitelist is configured through
     * @param realm The realm the user is being registered in
     * @param jsonValue The request content, with the action parameters already stripped out
     * @param objectType The type of object we're creating, user, group, etc.
     * @throws ResourceException If the request sets an attribute or a group membership that is not whitelisted, or
     * the whitelist itself cannot be read
     */
    public static void enforceSelfRegistrationWhiteList(final ConsoleConfigHandler configHandler, final String realm,
            final JsonValue jsonValue, final String objectType) throws ResourceException {

        enforceWhiteList(realm, jsonValue, objectType, validCreationAttributes(configHandler, realm));
    }

    /**
     * Applies the self service whitelist of valid creation attributes to a request that carries the calling context,
     * which is what decides whether the whitelist applies at all.
     *
     * @param configHandler The handler the whitelist is configured through
     * @param context The context of the request
     * @param jsonValue The JSON representation of the identity to create
     * @param objectType The type of object we're creating, user, group, etc.
     * @throws ResourceException If the request sets an attribute or a group membership that is not whitelisted, or
     * the whitelist itself cannot be read
     */
    public static void enforceSelfRegistrationWhiteList(final ConsoleConfigHandler configHandler,
            final Context context, final JsonValue jsonValue, final String objectType) throws ResourceException {

        // The overload below owns the rule; asking the same questions here is only about not paying for them, which
        // keeps a create the whitelist has nothing to say about from depending on the self service configuration at
        // all - the base code read it on every create, so a realm that cannot read it failed every one of them.
        // Both have to be asked before the call rather than left to the overload: the whitelist is an argument, so
        // it is read before the callee gets to return.
        if (!appliesTo(context) || !USER_TYPE.equals(objectType)) {
            return;
        }

        String realm = RealmContext.getRealm(context).asPath();
        enforceWhiteList(realm, jsonValue, objectType, validCreationAttributes(configHandler, realm));
    }

    /**
     * Reads the whitelist of attributes a self registering user may set.
     *
     * The read goes to the self service SMS service, which fails with an unchecked exception when it cannot be read
     * for the realm - {@literal IllegalArgumentException} for an attribute the service does not carry,
     * {@literal ConfigRetrievalException} or a {@literal NullPointerException} for a realm it is not configured for.
     * Turning that into an internal error of our own keeps the whitelist fail closed and says which check failed,
     * rather than letting the service configuration's own text escape to a caller with no session.
     *
     * @param configHandler The handler the whitelist is configured through
     * @param realm The realm the identity is being created in
     * @return The set of attribute names a self registering user may set, possibly empty
     * @throws InternalServerErrorException If the whitelist cannot be read
     */
    private static Set<String> validCreationAttributes(ConsoleConfigHandler configHandler, String realm)
            throws InternalServerErrorException {

        try {
            return configHandler.getConfig(realm, UserAttributeInfoBuilder.class).getValidCreationAttributes();
        } catch (RuntimeException re) {
            debug.error("IdentityRestUtils.validCreationAttributes() :: Cannot read the self service user "
                    + "registration whitelist in realm={}", realm, re);
            throw new InternalServerErrorException(
                    "Self service user registration is not configured for this realm", re);
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
                    Set<String> attrSet = identityAttributeJsonToSet(jVal.get(s));
                    if(MEMBEROF_ATTRIBUTES.contains(s)) {
                        identity.setGroupList(new ListWrapper(attrSet.toArray(new String[]{})));
                    } else {
                        identityAttrList.put(s, attrSet);
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

    /**
     * Transforms json representing an identity attribute into a representative set of strings.
     * <p/>
     * If the json is a single value, that value is serialized into a string and returned as a single entry set.
     * Otherwise if it is a list of values, each value is serialized into a string and returned as a collective set.
     *
     * @param identityAttributeJson
     *         identity attribute json to be transformed
     *
     * @return identity attribute value represented as a set of strings
     */
    static Set<String> identityAttributeJsonToSet(JsonValue identityAttributeJson) {

        if (identityAttributeJson.isString()) {
            return Collections.singleton(identityAttributeJson.asString());
        }

        if (identityAttributeJson.isList()) {
            Set<String> attributeValues = new HashSet<>();

            for (JsonValue value : identityAttributeJson) {
                if (value.isString()) {
                    attributeValues.add(value.asString());
                } else {
                    attributeValues.add(value.toString());
                }
            }

            return attributeValues;
        }

        return Collections.singleton(identityAttributeJson.toString());
    }

    /**
     * Uses an amAdmin SSOtoken to create an AMIdentity from the UID provided and checks
     * whether the AMIdentity in context is active/inactive
     *
     * @param uid the universal identifier of the user
     * @return true is the user is active, false otherwise
     * @throws NotFoundException invalid SSOToken, invalid UID
     */
    public static boolean isUserActive(String uid) throws NotFoundException {
        try {
            AMIdentity userIdentity = new AMIdentity(RestUtils.getToken(), uid);
            if (debug.messageEnabled()) {
                debug.message("IdentityResource.isUserActive() : UID={} isActive={}", uid, userIdentity.isActive());
            }
            return userIdentity.isActive();
        } catch (IdRepoException idre) {
            if (debug.errorEnabled()) {
                debug.error("IdentityResource.isUserActive() : Invalid UID={}", uid , idre);
            }
            throw new NotFoundException("Invalid UID, could not retrieved " + uid, idre);
        } catch (SSOException ssoe) {
            if (debug.errorEnabled()) {
                debug.error("IdentityResource.isUserActive() : Invalid SSOToken", ssoe);
            }
            throw new NotFoundException("Invalid SSOToken " + ssoe.getMessage(), ssoe);
        }
    }

}
