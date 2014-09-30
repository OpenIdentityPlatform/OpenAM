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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap;

import com.iplanet.am.util.Cache;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoFatalException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.idm.common.IdRepoUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.sm.SchemaType;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.forgerock.openam.idrepo.ldap.LDAPConstants.*;
import org.forgerock.openam.idrepo.ldap.helpers.ADAMHelper;
import org.forgerock.openam.idrepo.ldap.helpers.ADHelper;
import org.forgerock.openam.idrepo.ldap.helpers.DirectoryHelper;
import org.forgerock.openam.idrepo.ldap.psearch.DJLDAPv3PersistentSearch;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ErrorResultIOException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.Function;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.LDAPUrl;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.Modification;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.forgerock.opendj.ldap.schema.ObjectClass;
import org.forgerock.opendj.ldap.schema.ObjectClassType;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldap.schema.UnknownSchemaElementException;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

/**
 * This is an IdRepo implementation that utilizes the LDAP protocol via OpenDJ LDAP SDK to access directory servers.
 */
public class DJLDAPv3Repo extends IdRepo implements IdentityMovedOrRenamedListener {

    private static final String CLASS_NAME = DJLDAPv3Repo.class.getName();
    private static final Debug DEBUG = Debug.getInstance("DJLDAPv3Repo");
    /**
     * Maps psearchids to persistent search connections, so different datastore instances can share the same psearch
     * connection when appropriate.
     */
    private static final Map<String, DJLDAPv3PersistentSearch> pSearchMap =
            new HashMap<String, DJLDAPv3PersistentSearch>();
    private static final String AM_AUTH = "amAuth";
    private static final Filter DEFAULT_ROLE_SEARCH_FILTER =
            Filter.valueOf("(&(objectclass=ldapsubentry)(objectclass=nsmanagedroledefinition))");
    private static final Filter DEFAULT_FILTERED_ROLE_SEARCH_FILTER =
            Filter.valueOf("(&(objectclass=ldapsubentry)(objectclass=nsfilteredroledefinition))");
    private Set<LDAPURL> ldapServers;
    private IdRepoListener idRepoListener;
    private Map<IdType, Set<IdOperation>> supportedTypesAndOperations;
    private Map<String, String> creationAttributeMapping;
    private int heartBeatInterval = 10;
    private String heartBeatTimeUnit;
    private String rootSuffix;
    private String userStatusAttr;
    private boolean alwaysActive = false;
    private String activeValue;
    private String inactiveValue;
    private String userSearchAttr;
    private String userNamingAttr;
    private String groupNamingAttr;
    private String roleNamingAttr;
    private String filteredRoleNamingAttr;
    private Set<String> userObjectClasses;
    private Set<String> groupObjectClasses;
    private Set<String> roleObjectClasses;
    private Set<String> filteredRoleObjectClasses;
    private Set<String> userAttributesAllowed;
    private Set<String> groupAttributesAllowed;
    private Set<String> roleAttributesAllowed;
    private Set<String> filteredRoleAttributesAllowed;
    private String memberOfAttr;
    private String uniqueMemberAttr;
    private String memberURLAttr;
    private String defaultGroupMember;
    private Filter userSearchFilter;
    private Filter groupSearchFilter;
    private Filter roleSearchFilter;
    private Filter filteredRoleSearchFilter;
    private String peopleContainerName;
    private String peopleContainerValue;
    private String groupContainerName;
    private String groupContainerValue;
    private String roleAttr;
    private String roleDNAttr;
    private String roleFilterAttr;
    private SearchScope defaultScope;
    private SearchScope roleScope;
    private int defaultSizeLimit;
    private int defaultTimeLimit;
    private DirectoryHelper helper;
    //although there is a max pool size, we are currently doubling that in order to be able to authenticate users
    private ConnectionFactory connectionFactory;
    private ConnectionFactory bindConnectionFactory;
    //holds service attributes for the current realm
    private Map<String, Map<String, Set<String>>> serviceMap;
    //holds the directory schema
    private volatile Schema schema;
    //provides a cache for DNs (if enabled), because an entry tends to be requested in bursts.
    private Cache dnCache;
    // provides a switch to enable/disable the dnCache
    private boolean dnCacheEnabled = false;

    /**
     * Initializes the IdRepo instance, basically within this method we process
     * the configuration settings and set up the connection factories that will
     * be used later in the lifetime of the IdRepo plugin.
     *
     * @param configParams The IdRepo configuration as defined in the service
     * configurations.
     * @throws IdRepoException Shouldn't be thrown.
     */
    @Override
    public void initialize(Map<String, Set<String>> configParams) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("initialize invoked");
        }
        super.initialize(configParams);
        String hostServerId = null;
        String hostSiteId = "";
        try {
            hostServerId = WebtopNaming.getAMServerID();
            hostSiteId = WebtopNaming.getSiteID(hostServerId);
        } catch (ServerEntryNotFoundException senfe) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("ServerEntryNotFoundException, hostServerId=" + hostServerId
                        + ", hostSiteId=" + hostSiteId);
            }
        }
        boolean dnCacheEnabled = CollectionHelper.getBooleanMapAttr(configMap, LDAP_DNCACHE_ENABLED, true);
        if (dnCacheEnabled) {
            dnCache = new Cache(CollectionHelper.getIntMapAttr(configParams, LDAP_DNCACHE_SIZE, 1500, DEBUG));
        }
        ldapServers = LDAPUtils.prioritizeServers(configParams.get(LDAP_SERVER_LIST), hostServerId, hostSiteId);

        defaultSizeLimit = CollectionHelper.getIntMapAttr(configParams, LDAP_MAX_RESULTS, 100, DEBUG);
        defaultTimeLimit = CollectionHelper.getIntMapAttr(configParams, LDAP_TIME_LIMIT, 5, DEBUG);
        int maxPoolSize = CollectionHelper.getIntMapAttr(configParams, LDAP_CONNECTION_POOL_MAX_SIZE, 10, DEBUG);

        String username = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_USER_NAME);
        char[] password = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_PASSWORD, "").toCharArray();
        heartBeatInterval = CollectionHelper.getIntMapAttr(configParams, LDAP_SERVER_HEARTBEAT_INTERVAL, "10",
                DEBUG);
        heartBeatTimeUnit = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_HEARTBEAT_TIME_UNIT, "SECONDS");
        bindConnectionFactory = createConnectionFactory(null, null, maxPoolSize);
        connectionFactory = createConnectionFactory(username, password, maxPoolSize);

        supportedTypesAndOperations =
                IdRepoUtils.parseSupportedTypesAndOperations(configParams.get(LDAP_SUPPORTED_TYPES_AND_OPERATIONS));
        userStatusAttr = CollectionHelper.getMapAttr(configParams, LDAP_USER_STATUS_ATTR_NAME);
        if (userStatusAttr == null || userStatusAttr.isEmpty()) {
            alwaysActive = true;
            userStatusAttr = DEFAULT_USER_STATUS_ATTR;
        }
        activeValue = CollectionHelper.getMapAttr(configParams, LDAP_STATUS_ACTIVE, STATUS_ACTIVE);
        inactiveValue = CollectionHelper.getMapAttr(configParams, LDAP_STATUS_INACTIVE, STATUS_INACTIVE);
        creationAttributeMapping = IdRepoUtils.parseAttributeMapping(configParams.get(LDAP_CREATION_ATTR_MAPPING));
        userNamingAttr = CollectionHelper.getMapAttr(configParams, LDAP_USER_NAMING_ATTR);
        groupNamingAttr = CollectionHelper.getMapAttr(configParams, LDAP_GROUP_NAMING_ATTR);
        roleNamingAttr = CollectionHelper.getMapAttr(configParams, LDAP_ROLE_NAMING_ATTR);
        filteredRoleNamingAttr = CollectionHelper.getMapAttr(configParams, LDAP_FILTERED_ROLE_NAMING_ATTR);
        userSearchAttr = CollectionHelper.getMapAttr(configParams, LDAP_USER_SEARCH_ATTR);
        userAttributesAllowed = new CaseInsensitiveHashSet();
        Set<String> allowAttrs = configParams.get(LDAP_USER_ATTRS);
        if (allowAttrs != null) {
            userAttributesAllowed.addAll(allowAttrs);
        }
        groupAttributesAllowed = new CaseInsensitiveHashSet();
        allowAttrs = configParams.get(LDAP_GROUP_ATTRS);
        if (allowAttrs != null) {
            groupAttributesAllowed.addAll(allowAttrs);
        }
        roleAttributesAllowed = new CaseInsensitiveHashSet();
        allowAttrs = configParams.get(LDAP_ROLE_ATTRS);
        if (allowAttrs != null) {
            roleAttributesAllowed.addAll(allowAttrs);
        }
        filteredRoleAttributesAllowed = new CaseInsensitiveHashSet();
        allowAttrs = configParams.get(LDAP_FILTERED_ROLE_ATTRS);
        if (allowAttrs != null) {
            filteredRoleAttributesAllowed.addAll(allowAttrs);
        }
        userObjectClasses = getNonNullSettingValues(LDAP_USER_OBJECT_CLASS);
        groupObjectClasses = getNonNullSettingValues(LDAP_GROUP_OBJECT_CLASS);
        roleObjectClasses = getNonNullSettingValues(LDAP_ROLE_OBJECT_CLASS);
        filteredRoleObjectClasses = getNonNullSettingValues(LDAP_FILTERED_ROLE_OBJECT_CLASS);
        defaultGroupMember = CollectionHelper.getMapAttr(configParams, LDAP_DEFAULT_GROUP_MEMBER);
        uniqueMemberAttr = CollectionHelper.getMapAttr(configParams, LDAP_UNIQUE_MEMBER, UNIQUE_MEMBER_ATTR);
        memberURLAttr = CollectionHelper.getMapAttr(configParams, LDAP_MEMBER_URL);
        memberOfAttr = CollectionHelper.getMapAttr(configParams, LDAP_MEMBER_OF);
        peopleContainerName = CollectionHelper.getMapAttr(configParams, LDAP_PEOPLE_CONTAINER_NAME);
        peopleContainerValue = CollectionHelper.getMapAttr(configParams, LDAP_PEOPLE_CONTAINER_VALUE);
        groupContainerName = CollectionHelper.getMapAttr(configParams, LDAP_GROUP_CONTAINER_NAME);
        groupContainerValue = CollectionHelper.getMapAttr(configParams, LDAP_GROUP_CONTAINER_VALUE);
        roleAttr = CollectionHelper.getMapAttr(configParams, LDAP_ROLE_ATTR, ROLE_ATTR);
        roleDNAttr = CollectionHelper.getMapAttr(configParams, LDAP_ROLE_DN_ATTR, ROLE_DN_ATTR);
        roleFilterAttr = CollectionHelper.getMapAttr(configParams, LDAP_ROLE_FILTER_ATTR, ROLE_FILTER_ATTR);
        rootSuffix = CollectionHelper.getMapAttr(configParams, LDAP_SERVER_ROOT_SUFFIX);
        userSearchFilter = LDAPUtils.parseFilter(
                CollectionHelper.getMapAttr(configParams, LDAP_USER_SEARCH_FILTER), Filter.objectClassPresent());
        groupSearchFilter = LDAPUtils.parseFilter(
                CollectionHelper.getMapAttr(configParams, LDAP_GROUP_SEARCH_FILTER), Filter.objectClassPresent());
        roleSearchFilter = LDAPUtils.parseFilter(
                CollectionHelper.getMapAttr(configParams, LDAP_ROLE_SEARCH_FILTER), DEFAULT_ROLE_SEARCH_FILTER);
        filteredRoleSearchFilter = LDAPUtils.parseFilter(
                CollectionHelper.getMapAttr(configParams, LDAP_FILTERED_ROLE_SEARCH_FILTER),
                DEFAULT_FILTERED_ROLE_SEARCH_FILTER);
        String serviceInfo = CollectionHelper.getMapAttr(configParams, LDAP_SERVICE_ATTRS);
        serviceMap = new HashMap<String, Map<String, Set<String>>>(new SOAPClient("dummy").decodeMap(serviceInfo));
        defaultScope = LDAPUtils.getSearchScope(
                CollectionHelper.getMapAttr(configParams, LDAP_SEARCH_SCOPE), SearchScope.WHOLE_SUBTREE);
        roleScope = LDAPUtils.getSearchScope(
                CollectionHelper.getMapAttr(configParams, LDAP_ROLE_SEARCH_SCOPE), SearchScope.WHOLE_SUBTREE);
        if (configParams.containsKey(LDAP_ADAM_TYPE)) {
            helper = new ADAMHelper();
        } else if (configParams.containsKey(LDAP_AD_TYPE)) {
            helper = new ADHelper();
        } else {
            helper = new DirectoryHelper();
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("IdRepo configuration:\n"
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(configMap, asSet(LDAP_SERVER_PASSWORD)));
        }
    }

    protected ConnectionFactory createConnectionFactory(String username, char[] password, int maxPoolSize) {
        LDAPOptions ldapOptions = new LDAPOptions();
        ldapOptions.setTimeout(defaultTimeLimit, TimeUnit.SECONDS);
        boolean sslMode = Boolean.valueOf(CollectionHelper.getMapAttr(configMap, LDAP_SSL_ENABLED)).booleanValue();

        if (sslMode) {
            try {
                ldapOptions.setSSLContext(new SSLContextBuilder().getSSLContext());
            } catch (GeneralSecurityException gse) {
                DEBUG.error("An error occurred while setting the SSLContext", gse);
            }
        }
        if (maxPoolSize == 1) {
            return LDAPUtils.newFailoverConnectionFactory(ldapServers, username, password, heartBeatInterval,
                    heartBeatTimeUnit, ldapOptions);
        } else {
            return LDAPUtils.newFailoverConnectionPool(ldapServers, username, password, maxPoolSize, heartBeatInterval,
                    heartBeatTimeUnit, ldapOptions);
        }
    }

    /**
     * Tells whether this identity repository supports authentication.
     *
     * @return <code>true</code> since this repository supports authentication.
     */
    @Override
    public boolean supportsAuthentication() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("supportsAuthentication invoked");
        }
        return true;
    }

    /**
     * Tries to bind as the user with the credentials passed in via callbacks. This authentication mechanism does not
     * handle password policies, nor password expiration.
     *
     * @param credentials The username/password combination.
     * @return <code>true</code> if the bind operation was successful.
     * @throws IdRepoException If the passed in username/password was null, or if the specified user cannot be found.
     * @throws AuthLoginException If an LDAP error occurs during authentication.
     * @throws InvalidPasswordException If the provided password is not valid, so Account Lockout can be triggered.
     */
    @Override
    public boolean authenticate(Callback[] credentials) throws IdRepoException, AuthLoginException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("authenticate invoked");
        }
        String userName = null;
        char[] password = null;
        for (Callback callback : credentials) {
            if (callback instanceof NameCallback) {
                userName = ((NameCallback) callback).getName();
            } else if (callback instanceof PasswordCallback) {
                password = ((PasswordCallback) callback).getPassword();
            }
        }
        if (userName == null || password == null) {
            throw newIdRepoException("221", CLASS_NAME);
        }
        String dn = findDNForAuth(IdType.USER, userName);
        Connection conn = null;
        try {
            BindRequest bindRequest = Requests.newSimpleBindRequest(dn, password);
            conn = bindConnectionFactory.getConnection();
            BindResult bindResult = conn.bind(bindRequest);
            return bindResult.isSuccess();
        } catch (ErrorResultException ere) {
            ResultCode resultCode = ere.getResult().getResultCode();
            if (DEBUG.messageEnabled()) {
                DEBUG.message("An error occurred while trying to authenticate a user: " + ere.toString());
            }
            if (resultCode.equals(ResultCode.INVALID_CREDENTIALS)) {
                throw new InvalidPasswordException(AM_AUTH, "InvalidUP", null, userName, null);
            } else if (resultCode.equals(ResultCode.UNWILLING_TO_PERFORM)
                    || resultCode.equals(ResultCode.CONSTRAINT_VIOLATION)) {
                throw new AuthLoginException(AM_AUTH, "FAuth", null);
            } else if (resultCode.equals(ResultCode.INAPPROPRIATE_AUTHENTICATION)) {
                throw new AuthLoginException(AM_AUTH, "InappAuth", null);
            } else {
                throw new AuthLoginException(AM_AUTH, "LDAPex", null);
            }
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
    }

    /**
     * Changes password for the given identity by binding as the user first (i.e. this is not password reset). In case
     * of Active Directory the password will be encoded first. This will issue a DELETE for the old password and an ADD
     * for the new password value.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER.
     * @param name The name of the identity.
     * @param attrName The name of the password attribute, usually "userpassword" or "unicodepwd".
     * @param oldPassword The current password of the identity.
     * @param newPassword The new password of the idenity.
     * @throws IdRepoException If the identity type is invalid, or the entry cannot be found, or some other LDAP error
     * occurs while changing the password (like password policy related errors).
     */
    @Override
    public void changePassword(SSOToken token, IdType type, String name, String attrName, String oldPassword,
            String newPassword) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("changePassword invoked");
        }
        if (!type.equals(IdType.USER)) {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "229", new Object[]{CLASS_NAME});
        }
        String dn = getDN(type, name);
        BindRequest bindRequest = Requests.newSimpleBindRequest(dn, oldPassword.toCharArray());
        ModifyRequest modifyRequest = Requests.newModifyRequest(dn);

        byte[] encodedOldPwd = helper.encodePassword(oldPassword);
        byte[] encodedNewPwd = helper.encodePassword(newPassword);

        modifyRequest.addModification(ModificationType.DELETE, attrName, encodedOldPwd);
        modifyRequest.addModification(ModificationType.ADD, attrName, encodedNewPwd);
        Connection conn = null;
        try {
            conn = bindConnectionFactory.getConnection();
            conn.bind(bindRequest);
            conn.modify(modifyRequest);
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to change password for identity: " + name, ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
    }

    /**
     * Returns a fully qualified name of the identity, which should be unique per data store.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @return Fully qualified name of this identity or <code>null</code> if the identity cannot be found.
     * @throws IdRepoException If there was an error while looking up the user.
     */
    @Override
    public String getFullyQualifiedName(SSOToken token, IdType type, String name) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getFullyQualifiedName invoked");
        }
        try {
            return ldapServers + "/" + getDN(type, name);
        } catch (IdentityNotFoundException infe) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Unable to find identity with name " + name + " and type " + type);
            }
            return null;
        }
    }

    /**
     * Returns the set of supported operations for a given identity type.
     *
     * @param type The identity type for which we want to get the supported operations.
     * @return The set of supported operations for this identity type.
     */
    @Override
    public Set<IdOperation> getSupportedOperations(IdType type) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getSupportedOperations invoked");
        }
        return supportedTypesAndOperations.get(type);
    }

    /**
     * Returns the set of supported identity types.
     *
     * @return The set of supported identity types.
     */
    @Override
    public Set<IdType> getSupportedTypes() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getSupportedTypes invoked");
        }
        return supportedTypesAndOperations.keySet();
    }

    /**
     * Sets the user status to the value provided for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER.
     * @param name The name of the identity.
     * @param active The new status of the identity.
     * @throws IdRepoException If the identity type is invalid, or either the previous status retrieval failed (AD), or
     * there was a failure while setting the status.
     */
    @Override
    public void setActiveStatus(SSOToken token, IdType type, String name, boolean active)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("setActiveStatus invoked");
        }
        if (!type.equals(IdType.USER)) {
            throw newIdRepoException("206", CLASS_NAME);
        }
        String status = helper.getStatus(this, name, active, userStatusAttr, activeValue, inactiveValue);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Setting user status to: " + status);
        }
        Map<String, Set<String>> attr = new HashMap<String, Set<String>>(1);
        attr.put(userStatusAttr, asSet(status));
        setAttributes(token, type, name, attr, false);
    }

    /**
     * Tells whether the given identity is considered as "active" or not. In case the user status attribute is not
     * configured, this method will always return <code>true</code>. In case of Active Directory the returned
     * userAccountControl attribute will be masked with 0x2 to detect whether the given account is disabled or not.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER.
     * @param name The name of the identity.
     * @return <code>true</code> if user status attribute is not configured, or decision based on the status
     * attribute value. If there was any error while retrieving the status attribute this method will return
     * <code>false</code>.
     * @throws IdRepoException If the identity type is invalid.
     */
    @Override
    public boolean isActive(SSOToken token, IdType type, String name) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("isActive invoked");
        }
        if (!type.equals(IdType.USER)) {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "305",
                    new Object[]{CLASS_NAME, IdOperation.READ.getName(), type.getName()});
        }
        if (alwaysActive) {
            try {
                return isExists(token, type, name);
            } catch (IdRepoException ide) {
                return false;
            }
        }
        Map<String, Set<String>> attrMap;
        try {
            attrMap = getAttributes(token, type, name, asSet(userStatusAttr));
            attrMap = new CaseInsensitiveHashMap(attrMap);
        } catch (IdRepoException ire) {
            return false;
        }
        String status = CollectionHelper.getMapAttr(attrMap, userStatusAttr);
        if (status != null) {
            return helper.isActive(status, inactiveValue);
        } else {
            return true;
        }
    }

    /**
     * Tells whether a given identity exists or not.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @return <code>true</code> if the identity exists.
     * @throws IdRepoException Shouldn't be thrown.
     */
    @Override
    public boolean isExists(SSOToken token, IdType type, String name) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("isExists invoked");
        }
        try {
            //get the user DN, if there is no such entry, this will already fail.
            getDN(type, name);
        } catch (IdentityNotFoundException infe) {
            return false;
        }
        return true;
    }

    /**
     * Creates a new identity using the passed in attributes. The following steps will be performed with the passed in
     * data:
     * <ul>
     *  <li>The password will be encoded in case we are dealing with AD.</li>
     *  <li>If the attribute map contains the default status attribute, then it will be converted to the status values
     *      specified in the configuration.</li>
     *  <li>Performing creation attribute mapping, so certain attributes can have default values (coming from other
     *      attributes, or from the identity name if there is no mapping for the attribute).</li>
     *  <li>Removes all attributes that are not defined in the configuration.</li>
     * </ul>
     * If the default group member setting is being used and a new group identity is being created, the newly created
     * group will also have the default group member assigned.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrMap The attributes of the new identity, that needs to be stored.
     * @return The DN of the newly created identity
     * @throws IdRepoException If there is an error while creating the new identity, or if it's a group and there is a
     * problem while adding the default group member.
     */
    @Override
    public String create(SSOToken token, IdType type, String name, Map<String, Set<String>> attrMap)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Create invoked on " + type + ": " + name + " attrMap = "
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(attrMap, null));
        }
        String dn = generateDN(type, name);
        Set<String> objectClasses = getObjectClasses(type);
        //First we should make sure that we wrap the attributes with a case insensitive hashmap.
        attrMap = new CaseInsensitiveHashMap(attrMap);
        byte[] encodedPwd = helper.encodePassword(type, attrMap.get(AD_UNICODE_PWD_ATTR));
        //Let's set the userstatus as it is configured in the datastore.
        mapUserStatus(type, attrMap);
        //In case some attributes are missing use the create attribute mapping to get those values.
        mapCreationAttributes(type, name, attrMap);
        //and lastly we should make sure that we get rid of the attributes that are not known by the datastore.
        attrMap = removeUndefinedAttributes(type, attrMap);

        Set<String> ocs = attrMap.get(OBJECT_CLASS_ATTR);
        if (ocs != null) {
            ocs.addAll(objectClasses);
        } else {
            attrMap.put(OBJECT_CLASS_ATTR, objectClasses);
        }
        attrMap.put(getSearchAttribute(type), asSet(name));

        Entry entry = new LinkedHashMapEntry(dn);
        Set<String> attributeValue;
        for (Map.Entry<String, Set<String>> attr : attrMap.entrySet()) {
            // Add only attributes whose values are not empty or null
            attributeValue = attr.getValue();
            if(attributeValue != null  && !attributeValue.isEmpty()) {
                entry.addAttribute(attr.getKey(), attributeValue.toArray());
            }
        }

        if (type.equals(IdType.GROUP) && defaultGroupMember != null) {
            entry.addAttribute(uniqueMemberAttr, defaultGroupMember);
        }

        if (encodedPwd != null) {
            entry.replaceAttribute(AD_UNICODE_PWD_ATTR, encodedPwd);
        }

        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            conn.add(Requests.newAddRequest(entry));
            if (type.equals(IdType.GROUP) && defaultGroupMember != null) {
                if (memberOfAttr != null) {
                    ModifyRequest modifyRequest = Requests.newModifyRequest(defaultGroupMember);
                    modifyRequest.addModification(ModificationType.ADD, memberOfAttr, dn);
                    conn.modify(modifyRequest);
                }
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("Unable to add a new entry: " + name + " attrMap: "
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(attrMap, null), ere);
            if (ResultCode.ENTRY_ALREADY_EXISTS.equals(ere.getResult().getResultCode())) {
                throw IdRepoDuplicateObjectException.nameAlreadyExists(name);
            } else {
                handleErrorResult(ere);
            }
        } finally {
            IOUtils.closeIfNotNull(conn);
        }

        return dn;
    }

    /**
     * Returns all the attributes that are defined in the configuration for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @return The attributes of this identity.
     * @throws IdRepoException If there is an error while retrieving the identity attributes.
     */
    @Override
    public Map<String, Set<String>> getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getAttributes invoked");
        }

        return getAttributes(token, type, name, null);
    }

    /**
     * Returns all the requested attributes that are defined in the configuration for this given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrNames The names of the requested attributes or <code>null</code> to retrieve all the attributes.
     * @return The requested attributes of this identity.
     * @throws IdRepoException If there is an error while retrieving the identity attributes.
     */
    @Override
    public Map<String, Set<String>> getAttributes(SSOToken token, IdType type, String name, Set<String> attrNames)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getAttributes2 invoked");
        }

        return getAttributes(type, name, attrNames, new StringAttributeExtractor());
    }

    /**
     * Returns all the requested binary attributes that are defined in the configuration for this given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrNames The names of the requested binary attributes or <code>null</code> to retrieve all the
     * attributes.
     * @return The requested attributes of this identity in binary format.
     * @throws IdRepoException If there is an error while retrieving the identity attributes.
     */
    @Override
    public Map<String, byte[][]> getBinaryAttributes(SSOToken token, IdType type, String name, Set<String> attrNames)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getBinaryAttributes invoked");
        }
        return getAttributes(type, name, attrNames, new BinaryAttributeExtractor());
    }

    /**
     * Returns all the requested attributes either in binary or in String format. Only the attributes defined in the
     * configuration will be returned for this given identity. In case the default "inetUserStatus" attribute has been
     * requested, it will be converted to the actual status attribute during query, and while processing it will be
     * mapped back to standard "inetUserStatus" values as well (rather than returning the configuration/directory
     * specific values). If there is an attempt to read a realm identity type's objectclass attribute, this method will
     * return an empty map right away (legacy handling). If the dn attribute has been requested, and it's also defined
     * in the configuration, then the attributemap will also contain the dn in the result.
     *
     * @param <T>
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrNames The names of the requested attributes or <code>null</code> to retrieve all the attributes.
     * @param function A function that can extract String or byte array values from an LDAP attribute.
     * @return The requested attributes in string or binary format.
     * @throws IdRepoException If there is an error while retrieving the identity attributes.
     */
    private <T> Map<String, T> getAttributes(IdType type, String name, Set<String> attrNames,
            Function<Attribute, T, Void> function) throws IdRepoException {
        Set<String> attrs = attrNames == null
                ? new CaseInsensitiveHashSet(0) : new CaseInsensitiveHashSet(attrNames);

        if (type.equals(IdType.REALM)) {
            if (attrs.contains(OBJECT_CLASS_ATTR)) {
                return new HashMap(0);
            }
        }
        Map<String, T> result = new HashMap<String, T>();
        String dn = getDN(type, name);
        if (type.equals(IdType.USER)) {
            if (attrs.contains(DEFAULT_USER_STATUS_ATTR)) {
                attrs.add(userStatusAttr);
            }
        }
        Connection conn = null;
        Set<String> definedAttributes = getDefinedAttributes(type);
        if (attrs.isEmpty() || attrs.contains("*")) {
            attrs.clear();
            if (definedAttributes.isEmpty()) {
                attrs.add("*");
            } else {
                attrs.addAll(definedAttributes);
            }
        } else {
            if (!definedAttributes.isEmpty()) {
                attrs.retainAll(definedAttributes);
            }
            if (attrs.isEmpty()) {
                //there were only non-defined attributes requested, so we shouldn't return anything here.
                return new HashMap<String, T>(0);
            }
        }
        try {
            conn = connectionFactory.getConnection();
            SearchResultEntry entry = conn.readEntry(dn, attrs.toArray(new String[attrs.size()]));
            for (Attribute attribute : entry.getAllAttributes()) {
                String attrName = attribute.getAttributeDescriptionAsString();
                if (!definedAttributes.isEmpty() && !definedAttributes.contains(attrName)) {
                    continue;
                }
                result.put(attribute.getAttributeDescriptionAsString(), function.apply(attribute, null));
                if (attrName.equalsIgnoreCase(userStatusAttr) && attrs.contains(DEFAULT_USER_STATUS_ATTR)) {
                    String converted = helper.convertToInetUserStatus(attribute.firstValueAsString(), inactiveValue);
                    result.put(DEFAULT_USER_STATUS_ATTR,
                            function.apply(new LinkedAttribute(DEFAULT_USER_STATUS_ATTR, converted), null));
                }
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while getting user attributes", ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        if (attrs.contains(DN_ATTR)) {
            result.put(DN_ATTR, function.apply(new LinkedAttribute(DN_ATTR, dn), null));
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("getAttributes returning attrMap: "
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(result, null));
        }
        return result;
    }

    /**
     * Sets the provided attributes for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attributes The attributes that needs to be set for the entry.
     * @param isAdd <code>true</code> if the attributes should be ADDed, <code>false</code> if the attributes should be
     * REPLACEd instead.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>the identity cannot be found,</li>
     *  <li>there was a problem while retrieving the current user status from the directory (AD),</li>
     *  <li>there are no modifications to actually perform,</li>
     *  <li>there was an error while retrieving the objectClass attribute,</li>
     *  <li>there was an error while trying to read the directory schema,</li>
     *  <li>there was an error while trying to perform the modifications.</li>
     * </ul>
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setAttributes(SSOToken token, IdType type, String name, Map<String, Set<String>> attributes,
            boolean isAdd) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("setAttributes invoked");
        }
        setAttributes(token, type, name, (Map) attributes, isAdd, true, true);
    }

    /**
     * Sets the provided binary attributes for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attributes The binary attributes that needs to be set for the entry.
     * @param isAdd <code>true</code> if the attributes should be ADDed, <code>false</code> if the attributes should be
     * REPLACEd instead.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>the identity cannot be found,</li>
     *  <li>there was a problem while retrieving the current user status from the directory (AD),</li>
     *  <li>there are no modifications to actually perform,</li>
     *  <li>there was an error while retrieving the objectClass attribute,</li>
     *  <li>there was an error while trying to read the directory schema,</li>
     *  <li>there was an error while trying to perform the modifications.</li>
     * </ul>
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setBinaryAttributes(SSOToken token, IdType type, String name, Map<String, byte[][]> attributes,
            boolean isAdd) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("setBinaryAttributes invoked");
        }
        setAttributes(token, type, name, (Map) attributes, isAdd, false, true);
    }

    /**
     * Sets the provided attributes (string or binary) for the given identity. The following steps will be performed
     * prior to modification:
     * <ul>
     *  <li>The password will be encoded in case we are dealing with AD.</li>
     *  <li>Anything related to undefined attributes will be ignored.</li>
     *  <li>If the attribute map contains the default status attribute, then it will be converted to the status value
     *      specified in the configuration.</li>
     *  <li>In case changeOCs is set to <code>true</code>, the method will traverse through all the defined
     *      objectclasses to see if there is any attribute in the attributes map that is defined by that objectclass.
     *      These objectclasses will be collected and will be part of the modificationset with the other changes.</li>
     * </ul>
     * The attributes will be translated to modifications based on the followings:
     * <ul>
     *  <li>If the attribute has no values in the map, it will be considered as an attribute DELETE.</li>
     *  <li>In any other case based on the value of isAdd parameter, it will be either ADD, or REPLACE.</li>
     * </ul>
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attributes The attributes that needs to be set for the entry.
     * @param isAdd <code>true</code> if the attributes should be ADDed, <code>false</code> if the attributes should be
     * REPLACEd instead.
     * @param isString Whether the provided attributes are in string or binary format.
     * @param changeOCs Whether the module should adjust the objectclasses for the entry or not.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>the identity cannot be found,</li>
     *  <li>there was a problem while retrieving the current user status from the directory (AD),</li>
     *  <li>there are no modifications to actually perform,</li>
     *  <li>there was an error while retrieving the objectClass attribute,</li>
     *  <li>there was an error while trying to read the directory schema,</li>
     *  <li>there was an error while trying to perform the modifications.</li>
     * </ul>
     */
    private void setAttributes(SSOToken token, IdType type, String name, Map attributes,
            boolean isAdd, boolean isString, boolean changeOCs) throws IdRepoException {
        ModifyRequest modifyRequest = Requests.newModifyRequest(getDN(type, name));
        attributes = removeUndefinedAttributes(type, attributes);

        if (type.equals(IdType.USER)) {
            Object status = attributes.get(DEFAULT_USER_STATUS_ATTR);
            if (status != null && !attributes.containsKey(userStatusAttr)) {
                String value = null;
                if (status instanceof Set) {
                    value = ((Set<String>) status).iterator().next();
                } else if (status instanceof byte[][]) {
                    value = new String(((byte[][]) status)[0], Charset.forName("UTF-8"));
                }
                value = helper.getStatus(this, name, !STATUS_INACTIVE.equals(value),
                        userStatusAttr, activeValue, inactiveValue);
                attributes.remove(DEFAULT_USER_STATUS_ATTR);
                if (isString) {
                    attributes.put(userStatusAttr, asSet(value));
                } else {
                    byte[][] binValue = new byte[1][];
                    binValue[0] = value.getBytes(Charset.forName("UTF-8"));
                    attributes.put(userStatusAttr, binValue);
                }
            }
        }

        for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>) attributes.entrySet()) {
            Object values = entry.getValue();
            String attrName = entry.getKey();
            Attribute attr = new LinkedAttribute(attrName);
            if (AD_UNICODE_PWD_ATTR.equalsIgnoreCase(attrName)) {
                if (values instanceof byte[][]) {
                    attr.add(ByteString.valueOf(helper.encodePassword(IdType.USER, (byte[][]) values)));
                } else {
                    attr.add(ByteString.valueOf(helper.encodePassword(IdType.USER, (Set) values)));
                }
            } else if (values instanceof byte[][]) {
                for (byte[] bytes : (byte[][]) values) {
                    attr.add(ByteString.valueOf(bytes));
                }
            } else if (values instanceof Set) {
                for (String value : (Set<String>) values) {
                    attr.add(ByteString.valueOf(value));
                }
            }
            if (attr.isEmpty()) {
                modifyRequest.addModification(new Modification(ModificationType.REPLACE, attr));
            } else {
                modifyRequest.addModification(
                        new Modification(isAdd ? ModificationType.ADD : ModificationType.REPLACE, attr));
            }
        }
        if (modifyRequest.getModifications().isEmpty()) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("setAttributes: there are no modifications to perform");
            }
            throw newIdRepoException("201");
        }
        if (type.equals(IdType.USER) && changeOCs) {
            Set<String> missingOCs = new CaseInsensitiveHashSet();
            Map<String, Set<String>> attrs = getAttributes(token, type, name, asSet(OBJECT_CLASS_ATTR));
            Set<String> ocs = attrs.get(OBJECT_CLASS_ATTR);

            //if the user has objectclasses that are defined in the config, but not defined in the entry then add those
            //to missingOCs
            if (ocs != null) {
                missingOCs.addAll(getObjectClasses(type));
                missingOCs.removeAll(ocs);
            }

            //if the missingOCs is not empty (i.e. there are objectclasses that are not present in the entry yet)
            if (!missingOCs.isEmpty()) {
                Object obj = attributes.get(OBJECT_CLASS_ATTR);
                //if the API user has also added some of his objectclasses, then let's remove those from missingOCs
                if (obj != null && obj instanceof Set) {
                    missingOCs.removeAll((Set<String>) obj);
                }
                //for every single objectclass that needs to be added, let's check if they contain an attribute that we
                //wanted to add to the entry.
                Set<String> newOCs = new HashSet<String>(2);
                Schema dirSchema = getSchema();
                for (String objectClass : missingOCs) {
                    try {
                        ObjectClass oc = dirSchema.getObjectClass(objectClass);
                        //we should never add new structural objectclasses, see RFC 4512
                        if (!oc.getObjectClassType().equals(ObjectClassType.STRUCTURAL)) {
                            for (String attrName : (Set<String>) attributes.keySet()) {
                                //before we start to add too many objectclasses here...
                                if (!attrName.equalsIgnoreCase(OBJECT_CLASS_ATTR)
                                        && oc.isRequiredOrOptional(dirSchema.getAttributeType(attrName))) {
                                    newOCs.add(objectClass);
                                    break;
                                }
                            }
                        }
                    } catch (UnknownSchemaElementException usee) {
                        if (DEBUG.warningEnabled()) {
                            DEBUG.warning("Unable to find a schema element: " + usee.getMessage());
                        }
                    }
                }
                missingOCs = newOCs;
                //it is possible that none of the missing objectclasses are actually covering any new attributes
                if (!missingOCs.isEmpty()) {
                    //based on these let's add the extra objectclasses to the modificationset
                    modifyRequest.addModification(new Modification(ModificationType.ADD,
                            new LinkedAttribute(OBJECT_CLASS_ATTR, missingOCs)));
                }
            }
        }

        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            conn.modify(modifyRequest);
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occured while setting attributes for identity: " + name, ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
    }

    /**
     * Removes the specified attributes from the identity.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @param attrNames The set of attribute names that needs to be removed from the identity.
     * @throws IdRepoException If there is no attribute name provided, or if the identity cannot be found, or there is
     * an error while modifying the entry.
     */
    @Override
    public void removeAttributes(SSOToken token, IdType type, String name, Set<String> attrNames)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("removeAttributes invoked");
        }
        attrNames = removeUndefinedAttributes(type, attrNames);
        if (attrNames.isEmpty()) {
            throw newIdRepoException("201");
        }
        String dn = getDN(type, name);
        ModifyRequest modifyRequest = Requests.newModifyRequest(dn);
        for (String attr : attrNames) {
            modifyRequest.addModification(ModificationType.DELETE, attr);
        }
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            conn.modify(modifyRequest);
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while removing attributes from identity: " + name
                    + " attributes: " + attrNames, ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
    }

    /**
     * Performs a search in the directory based on the provided parameters.
     * Using the pattern and avPairs parameters an example search filter would look something like:
     * <code>(&(|(attr1=value1)(attr2=value2))(searchAttr=pattern)(objectclassfilter))</code>.
     * 
     * @param token Not used.
     * @param type The type of the identity.
     * @param pattern The pattern to be used in the search filter as the search attribute value.
     * @param maxTime The time limit for this search (in seconds). When maxTime &lt; 1, the default time limit will
     * be used.
     * @param maxResults The number of maximum results we should receive for this search. When maxResults &lt; 1 the
     * default sizelimit will be used.
     * @param returnAttrs The attributes that should be returned from the "search hits".
     * @param returnAllAttrs <code>true</code> if all user attribute should be returned.
     * @param filterOp When avPairs is provided, this logical operation will be used between them. Use
     * {@link IdRepo#AND_MOD} or {@link IdRepo#OR_MOD}.
     * @param avPairs Attribute-value pairs based on the search should be performed.
     * @param recursive Deprecated setting, not used.
     * @return The search results based on the provided parameters.
     * @throws IdRepoException Shouldn't be thrown as the returned RepoSearchResults will contain the error code.
     */
    @Override
    public RepoSearchResults search(SSOToken token, IdType type, String pattern, int maxTime, int maxResults,
            Set<String> returnAttrs, boolean returnAllAttrs, int filterOp, Map<String, Set<String>> avPairs, boolean recursive)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("search invoked with type: " + type + " pattern: " + pattern + " avPairs: " + avPairs
                    + " maxTime: " + maxTime + " maxResults: " + maxResults + " returnAttrs: " + returnAttrs
                    + " returnAllAttrs: " + returnAllAttrs + " filterOp: " + filterOp + " recursive: " + recursive);
        }
        DN baseDN = getBaseDN(type);
        //Recursive is a deprecated setting on IdSearchControl, hence we should use the searchscope defined in the
        //datastore configuration.
        SearchScope scope = defaultScope;

        String searchAttr = getSearchAttribute(type);
        String[] attrs;

        Filter filter = Filter.and(Filter.valueOf(searchAttr + "=" + pattern), getObjectClassFilter(type));
        Filter tempFilter = constructFilter(filterOp, avPairs);
        if (tempFilter != null) {
            filter = Filter.and(tempFilter, filter);
        }
        if (returnAllAttrs || (returnAttrs != null && returnAttrs.contains("*"))) {
            attrs = new String[]{"*"};
            returnAllAttrs = true;
        } else if (returnAttrs != null && !returnAttrs.isEmpty()) {
            returnAttrs.add(searchAttr);
            attrs = returnAttrs.toArray(new String[returnAttrs.size()]);
        } else {
            attrs = new String[]{searchAttr};
        }
        SearchRequest searchRequest = Requests.newSearchRequest(baseDN, scope, filter, attrs);
        searchRequest.setSizeLimit(maxResults < 1 ? defaultSizeLimit : maxResults);
        searchRequest.setTimeLimit(maxTime < 1 ? defaultTimeLimit : maxTime);
        Connection conn = null;
        Set<String> names = new HashSet<String>();
        Map<String, Map<String, Set<String>>> entries = new HashMap<String, Map<String, Set<String>>>();
        int errorCode = RepoSearchResults.SUCCESS;
        try {
            conn = connectionFactory.getConnection();
            ConnectionEntryReader reader = conn.search(searchRequest);
            while (reader.hasNext()) {
                Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();
                if (reader.isEntry()) {
                    SearchResultEntry entry = reader.readEntry();
                    String name = entry.parseAttribute(searchAttr).asString();
                    names.add(name);
                    if (returnAllAttrs) {
                        for (Attribute attribute : entry.getAllAttributes()) {
                            LDAPUtils.addAttributeToMapAsString(attribute, attributes);
                        }
                        entries.put(name, attributes);
                    } else if (returnAttrs != null && !returnAttrs.isEmpty()) {
                        for (String attr : returnAttrs) {
                            Attribute attribute = entry.getAttribute(attr);
                            if (attribute != null) {
                                LDAPUtils.addAttributeToMapAsString(attribute, attributes);
                            }
                        }
                        entries.put(name, attributes);
                    } else {
                        //there is no attribute to return, don't populate the entries map
                    }
                } else {
                    //ignore search result references
                    reader.readReference();
                }
            }
        } catch (ErrorResultException ere) {
            ResultCode resultCode = ere.getResult().getResultCode();
            if (resultCode.equals(ResultCode.NO_SUCH_OBJECT)) {
                return new RepoSearchResults(new HashSet<String>(0), RepoSearchResults.SUCCESS,
                        Collections.EMPTY_MAP, type);
            } else {
                DEBUG.error("Unexpected error occurred during search", ere);
                errorCode = resultCode.intValue();
            }
        } catch (ErrorResultIOException erioe) {
            ErrorResultException ere = erioe.getCause();
            if (ere != null) {
                ResultCode resultCode = ere.getResult().getResultCode();
                if (resultCode.equals(ResultCode.TIME_LIMIT_EXCEEDED)
                        || resultCode.equals(ResultCode.CLIENT_SIDE_TIMEOUT)) {
                    errorCode = RepoSearchResults.TIME_LIMIT_EXCEEDED;
                } else if (resultCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                    errorCode = RepoSearchResults.SIZE_LIMIT_EXCEEDED;
                } else {
                    DEBUG.error("Unexpected error occurred during search", ere);
                }
            } else {
                DEBUG.error("Unexpected error occurred during search", erioe);
            }
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException("219", CLASS_NAME);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        return new RepoSearchResults(names, errorCode, entries, type);
    }

    /**
     * Deletes the identity from the directory.
     *
     * @param token Not used.
     * @param type The type of the identity.
     * @param name The name of the identity.
     * @throws IdRepoException If the identity cannot be found, or there is an error while deleting the identity.
     */
    @Override
    public void delete(SSOToken token, IdType type, String name) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("delete invoked");
        }
        String dn = getDN(type, name);
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            conn.delete(dn);
        } catch (ErrorResultException ere) {
            DEBUG.error("Unable to delete entry: " + dn, ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        if (dnCacheEnabled) {
            dnCache.remove(generateDNCacheKey(name, type));
        }
    }

    /**
     * Gets membership data for a given group/role/filtered role.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always GROUP, ROLE or FILTEREDROLE.
     * @param name The name of the identity.
     * @param membersType The type of the member identity, this should be always USER.
     * @return The DNs of the members.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>the identity type is not GROUP/ROLE/FILTEREDROLE,</li>
     *  <li>the membersType is not USER,</li>
     *  <li>the identity cannot be found,</li>
     *  <li>there was an error while retrieving the members.</li>
     * </ul>
     */
    @Override
    public Set<String> getMembers(SSOToken token, IdType type, String name, IdType membersType)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getMembers invoked");
        }
        if (type.equals(IdType.USER)) {
            throw newIdRepoException("203");
        }
        if (!membersType.equals(IdType.USER)) {
            throw newIdRepoException("204", CLASS_NAME, membersType.getName(), type.getName());
        }
        String dn = getDN(type, name);

        if (type.equals(IdType.GROUP)) {
            return getGroupMembers(dn);
        } else if (type.equals(IdType.ROLE)) {
            return getRoleMembers(dn);
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return getFilteredRoleMembers(dn);
        }
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "305",
                new Object[]{CLASS_NAME, IdOperation.READ.getName(), type.getName()});
    }

    /**
     * Returns the DNs of the members of this group. If the MemberURL attribute has been configured, then this
     * will also try to retrieve dynamic group members using the memberURL.
     *
     * @param dn The DN of the group to query.
     * @return The DNs of the members.
     * @throws IdRepoException If there is an error while trying to retrieve the members.
     */
    private Set<String> getGroupMembers(String dn) throws IdRepoException {
        Set<String> results = new HashSet<String>();
        Connection conn = null;
        String[] attrs;
        if (memberURLAttr != null) {
            attrs = new String[]{uniqueMemberAttr, memberURLAttr};
        } else {
            attrs = new String[]{uniqueMemberAttr};
        }
        try {
            conn = connectionFactory.getConnection();
            SearchResultEntry entry = conn.readEntry(dn, attrs);
            Attribute attr = entry.getAttribute(uniqueMemberAttr);
            if (attr != null) {
                results.addAll(LDAPUtils.getAttributeValuesAsStringSet(attr));
            } else if (memberURLAttr != null) {
                attr = entry.getAttribute(memberURLAttr);
                if (attr != null) {
                    for (ByteString byteString : attr) {
                        LDAPUrl url = LDAPUrl.valueOf(byteString.toString());
                        SearchRequest searchRequest = Requests.newSearchRequest(
                                url.getName(), url.getScope(), url.getFilter(), DN_ATTR);
                        searchRequest.setTimeLimit(defaultTimeLimit);
                        searchRequest.setSizeLimit(defaultSizeLimit);
                        ConnectionEntryReader reader = conn.search(searchRequest);
                        while (reader.hasNext()) {
                            if (reader.isEntry()) {
                                results.add(reader.readEntry().getName().toString());
                            } else {
                                //ignore search result references
                                reader.readReference();
                            }
                        }
                    }
                }
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while retrieving group members for " + dn, ere);
            handleErrorResult(ere);
        } catch (ErrorResultIOException erioe) {
            handleIOError(erioe, "#getGroupMembers");
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException("219", CLASS_NAME);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        return results;
    }

    /**
     * Returns the DNs of the members of this role. To do that this will execute an LDAP search with a filter looking
     * for nsRoleDN=roleDN.
     *
     * @param dn The DN of the role to query.
     * @return The DNs of the members.
     * @throws IdRepoException If there is an error while trying to retrieve the role members.
     */
    private Set<String> getRoleMembers(String dn) throws IdRepoException {
        Set<String> results = new HashSet<String>();
        DN roleBase = getBaseDN(IdType.ROLE);
        Filter filter = Filter.equality(roleDNAttr, dn);
        SearchRequest searchRequest = Requests.newSearchRequest(roleBase, roleScope, filter, DN_ATTR);
        searchRequest.setTimeLimit(defaultTimeLimit);
        searchRequest.setSizeLimit(defaultSizeLimit);
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            ConnectionEntryReader reader = conn.search(searchRequest);
            while (reader.hasNext()) {
                if (reader.isEntry()) {
                    results.add(reader.readEntry().getName().toString());
                } else {
                    //ignore search result references
                    reader.readReference();
                }
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to retrieve filtered role members for " + dn, ere);
            handleErrorResult(ere);
        } catch (ErrorResultIOException erioe) {
            handleIOError(erioe, "#getRoleMembers");
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException("219", CLASS_NAME);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        return results;
    }

    /**
     * Returns the DNs of the members of this filtered role. To do that this will execute a read on the filtered role
     * entry to get the values of the nsRoleFilter attribute, and then it will perform searches using the retrieved
     * filters.
     *
     * @param dn The DN of the filtered role to query.
     * @return The DNs of the members.
     * @throws IdRepoException If there is an error while trying to retrieve the filtered role members.
     */
    private Set<String> getFilteredRoleMembers(String dn) throws IdRepoException {
        Set<String> results = new HashSet<String>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            SearchResultEntry entry = conn.readEntry(dn, roleFilterAttr);
            Attribute filterAttr = entry.getAttribute(roleFilterAttr);
            if (filterAttr != null) {
                for (ByteString byteString : filterAttr) {
                    Filter filter = Filter.valueOf(byteString.toString());
                    //TODO: would it make sense to OR these filters and run a single search?
                    SearchRequest searchRequest =
                            Requests.newSearchRequest(rootSuffix, defaultScope, filter.toString(), DN_ATTR);
                    searchRequest.setTimeLimit(defaultTimeLimit);
                    searchRequest.setSizeLimit(defaultSizeLimit);
                    ConnectionEntryReader reader = conn.search(searchRequest);
                    while (reader.hasNext()) {
                        if (reader.isEntry()) {
                            results.add(reader.readEntry().getName().toString());
                        } else {
                            //ignore search result references
                            reader.readReference();
                        }
                    }
                }
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to retrieve filtered role members for " + dn, ere);
            handleErrorResult(ere);
        } catch (ErrorResultIOException erioe) {
            handleIOError(erioe, "#getFilteredRoleMembers");
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException("219", CLASS_NAME);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        return results;
    }

    /**
     * Returns the membership information of a user for the given membership type.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER.
     * @param name The name of the identity.
     * @param membershipType The type of the membership identity, this should be always GROUP/ROLE/FILTEREDROLE.
     * @return The DNs of the groups/roles/filtered roles this user is member of.
     * @throws IdRepoException if the identity type is not USER, or if there was an error while retrieving the
     * membership data.
     */
    @Override
    public Set<String> getMemberships(SSOToken token, IdType type, String name, IdType membershipType)
            throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getMemberships called");
        }
        if (!type.equals(IdType.USER)) {
            throw newIdRepoException("206", CLASS_NAME);
        }
        String dn = getDN(IdType.USER, name);
        if (membershipType.equals(IdType.GROUP)) {
            return getGroupMemberships(dn);
        } else if (membershipType.equals(IdType.ROLE)) {
            return getRoleMemberships(dn);
        } else if (membershipType.equals(IdType.FILTEREDROLE)) {
            return getFilteredRoleMemberships(dn);
        }
        throw newIdRepoException("204", CLASS_NAME, type.getName(), membershipType.getName());
    }

    /**
     * Returns the group membership informations for this given user. In case the memberOf attribute is configured,
     * this will try to query the user entry and return the group DNs found in the memberOf attribute. Otherwise a
     * search request will be issued using the uniqueMember attribute looking for matches with the user DN.
     *
     * @param dn The DN of the user identity.
     * @return The DNs of the groups that the provided user is member of.
     * @throws IdRepoException If there was an error while retrieving the group membership information.
     */
    private Set<String> getGroupMemberships(String dn) throws IdRepoException {
        Set<String> results = new HashSet<String>();
        if (memberOfAttr == null) {
            Filter filter = Filter.and(groupSearchFilter, Filter.equality(uniqueMemberAttr, dn));
            SearchRequest searchRequest =
                    Requests.newSearchRequest(getBaseDN(IdType.GROUP), defaultScope, filter, DN_ATTR);
            searchRequest.setTimeLimit(defaultTimeLimit);
            searchRequest.setSizeLimit(defaultSizeLimit);
            Connection conn = null;
            try {
                conn = connectionFactory.getConnection();
                ConnectionEntryReader reader = conn.search(searchRequest);
                while (reader.hasNext()) {
                    if (reader.isEntry()) {
                        results.add(reader.readEntry().getName().toString());
                    } else {
                        //ignore search result references
                        reader.readReference();
                    }
                }
            } catch (ErrorResultException ere) {
                DEBUG.error("An error occurred while trying to retrieve group memberships for " + dn
                        + " using " + uniqueMemberAttr, ere);
                handleErrorResult(ere);
            } catch (ErrorResultIOException erioe) {
                handleIOError(erioe, "#getGroupMemberships@uniqueMember");
            } catch (SearchResultReferenceIOException srrioe) {
                //should never ever happen...
                DEBUG.error("Got reference instead of entry", srrioe);
                throw newIdRepoException("219", CLASS_NAME);
            } finally {
                IOUtils.closeIfNotNull(conn);
            }
        } else {
            Connection conn = null;
            try {
                conn = connectionFactory.getConnection();
                SearchResultEntry entry = conn.readEntry(dn, memberOfAttr);
                Attribute attr = entry.getAttribute(memberOfAttr);
                if (attr != null) {
                    results.addAll(LDAPUtils.getAttributeValuesAsStringSet(attr));
                }
            } catch (ErrorResultException ere) {
                DEBUG.error("An error occurred while trying to retrieve group memberships for " + dn
                        + " using " + memberOfAttr + " attribute", ere);
                handleErrorResult(ere);
            } finally {
                IOUtils.closeIfNotNull(conn);
            }
        }
        return results;
    }

    /**
     * Return the role membership informations for this given user. This will execute a read on the user entry to
     * retrieve the nsRoleDN attribute. The values of the attribute will be returned.
     *
     * @param dn The DN of the user identity.
     * @return The DNs of the roles this user is member of.
     * @throws IdRepoException If there was an error while retrieving the role membership information.
     */
    private Set<String> getRoleMemberships(String dn) throws IdRepoException {
        Set<String> results = new HashSet<String>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            SearchResultEntry entry = conn.readEntry(dn, roleDNAttr);
            Attribute attr = entry.getAttribute(roleDNAttr);
            if (attr != null) {
                results.addAll(LDAPUtils.getAttributeValuesAsStringSet(attr));
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to retrieve role memberships for " + dn
                    + " using " + roleDNAttr + " attribute", ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        return results;
    }

    /**
     * Returns the filtered and non-filtered role memberships for this given user. This will execute a read on the user
     * entry to retrieve the nsRole attribute. The values of the attribute will be returned along with the non-filtered
     * role memberships.
     *
     * @param dn The DN of the user identity.
     * @return The DNs of the filtered roles this user is member of.
     * @throws IdRepoException If there was an error while retrieving the filtered or non-filtered role membership
     * information.
     */
    private Set<String> getFilteredRoleMemberships(String dn) throws IdRepoException {
        Set<String> results = new CaseInsensitiveHashSet();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            SearchResultEntry entry = conn.readEntry(dn, roleAttr);
            Attribute attr = entry.getAttribute(roleAttr);
            if (attr != null) {
                results.addAll(LDAPUtils.getAttributeValuesAsStringSet(attr));
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to retrieve filtered role memberships for " + dn
                    + " using " + roleAttr + " attribute", ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        results.addAll(getRoleMemberships(dn));

        return results;
    }

    /**
     * Adds or removes members to the provided group/role.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always GROUP or ROLE.
     * @param name The name of the identity.
     * @param members The set of members that needs to be added/removed.
     * @param membersType The type of the member, this should be always USER.
     * @param operation The operation that needs to be performed with the provided members. Use {@link
     * IdRepo#ADDMEMBER} and {@link IdRepo#REMOVEMEMBER}.
     * @throws IdRepoException Can be thrown in the following cases:
     * <ul>
     *  <li>there are no members provided,</li>
     *  <li>the provided type/membersType are invalid,</li>
     *  <li>the identity to be modified cannot be found,</li>
     *  <li>one of the members cannot be found,</li>
     *  <li>there was an error while trying to modify the membership data.</li>
     * </ul>
     */
    @Override
    public void modifyMemberShip(SSOToken token, IdType type, String name, Set<String> members, IdType membersType,
            int operation) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("modifymembership invoked");
        }
        if (members == null || members.isEmpty()) {
            throw newIdRepoException("201");
        }
        if (type.equals(IdType.USER)) {
            throw newIdRepoException("203");
        }
        if (!membersType.equals(IdType.USER)) {
            throw newIdRepoException("206", CLASS_NAME);
        }
        String dn = getDN(type, name);
        Set<String> memberDNs = new HashSet<String>(members.size());
        for (String member : members) {
            memberDNs.add(getDN(membersType, member));
        }
        if (type.equals(IdType.GROUP)) {
            modifyGroupMembership(dn, memberDNs, operation);
        } else if (type.equals(IdType.ROLE)) {
            modifyRoleMembership(dn, memberDNs, operation);
        } else {
            throw newIdRepoException("209", CLASS_NAME, type.getName());
        }
    }

    /**
     * Modifies group membership data in the directory. In case the memberOf attribute is configured, this will also
     * iterate through all the user entries and modify those as well. Otherwise this will only modify the uniquemember
     * attribute on the group entry based on the operation.
     *
     * @param groupDN The DN of the group.
     * @param memberDNs The DNs of the group members.
     * @param operation Whether the members needs to be added or removed from the group. Use {@link IdRepo#ADDMEMBER}
     * or {@link IdRepo#REMOVEMEMBER}.
     * @throws IdRepoException If there was an error while modifying the membership data.
     */
    private void modifyGroupMembership(String groupDN, Set<String> memberDNs, int operation) throws IdRepoException {
        ModifyRequest modifyRequest = Requests.newModifyRequest(groupDN);
        Attribute attr = new LinkedAttribute(uniqueMemberAttr, memberDNs);
        ModificationType modType;
        if (ADDMEMBER == operation) {
            modType = ModificationType.ADD;
        } else {
            modType = ModificationType.DELETE;
        }
        modifyRequest.addModification(new Modification(modType, attr));
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            conn.modify(modifyRequest);
            if (memberOfAttr != null) {
                for (String member : memberDNs) {
                    ModifyRequest userMod = Requests.newModifyRequest(member);
                    userMod.addModification(modType, memberOfAttr, groupDN);
                    conn.modify(userMod);
                }
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to modify group membership. Name: " + groupDN
                    + " memberDNs: " + memberDNs + " Operation: " + modType, ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }

    }

    /**
     * Modifies role membership data in the directory. This will add/remove the corresponding nsRoleDN attribute from
     * the user entry.
     *
     * @param roleDN The DN of the role.
     * @param memberDNs The DNs of the role members.
     * @param operation Whether the members needs to be added or removed from the group. Use {@link IdRepo#ADDMEMBER}
     * or {@link IdRepo#REMOVEMEMBER}.
     * @throws IdRepoException If there was an error while modifying the membership data.
     */
    private void modifyRoleMembership(String roleDN, Set<String> memberDNs, int operation) throws IdRepoException {
        Attribute attr = new LinkedAttribute(roleDNAttr, roleDN);
        Modification mod;
        if (ADDMEMBER == operation) {
            mod = new Modification(ModificationType.ADD, attr);
        } else {
            mod = new Modification(ModificationType.DELETE, attr);
        }
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            for (String memberDN : memberDNs) {
                ModifyRequest modifyRequest = Requests.newModifyRequest(memberDN);
                modifyRequest.addModification(mod);
                conn.modify(modifyRequest);
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to modify role membership. Name: " + roleDN
                    + " memberDNs: " + memberDNs, ere);
            handleErrorResult(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
    }

    /**
     * Assigns a service to the provided identity.
     * In case of a USER if the attribute map contains objectclasses, then
     * the existing set of objectclasses will be retrieved, and added to those. These settings will override the
     * existing values if any present.
     * In case of a REALM the service attributes will be persisted by the {@link IdRepoListener} implementation.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity. Only used when identity type is USER.
     * @param serviceName The name of the service that needs to be assigned to the identity.
     * @param stype The schema type of the service that needs to be assigned.
     * @param attrMap The service configuration that needs to be saved for the identity.
     * @throws IdRepoException If there was an error while retrieving the user objectclasses, or when the settings were
     * being saved to the identity.
     */
    @Override
    public void assignService(SSOToken token, IdType type, String name, String serviceName, SchemaType stype,
            Map<String, Set<String>> attrMap) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("assignService invoked");
        }
        if (type.equals(IdType.USER)) {
            Set<String> ocs = attrMap.get(OBJECT_CLASS_ATTR);
            if (stype.equals(SchemaType.USER)) {
                if (ocs != null) {
                    Map<String, Set<String>> attrs = getAttributes(token, type, name, asSet(OBJECT_CLASS_ATTR));
                    ocs = new CaseInsensitiveHashSet(ocs);
                    ocs.addAll(attrs.get(OBJECT_CLASS_ATTR));
                    attrMap.put(OBJECT_CLASS_ATTR, ocs);
                }
                setAttributes(token, type, name, (Map) attrMap, false, true, false);
            }
        } else if (type.equals(IdType.REALM)) {
            if (serviceName != null && !serviceName.isEmpty() && attrMap != null) {
                Map<String, Set<String>> copyMap = new HashMap<String, Set<String>>(attrMap);
                serviceMap.put(serviceName, copyMap);
            }
            if (idRepoListener != null) {
                idRepoListener.setServiceAttributes(serviceName, serviceMap);
            }
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "213", new Object[]{CLASS_NAME});
        }
    }

    /**
     * Returns the currently assigned to the given identity.
     * In case of a USER this will retrieve the objectclasses defined for this user, and based on the provided
     * mapOfServicesAndOCs if all of the objectclasses mapped to a service is present, only then will the service be
     * added to the resulting list.
     * In case of a REALM the locally stored serviceMap's keySet will be returned, since that contains all the different
     * service names defined within this realm.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity. Only used when identity type is USER.
     * @param mapOfServicesAndOCs A mapping between the names of services and the corresponding objectclasses.
     * @return The list of services that are currently assigned to the identity.
     * @throws IdRepoException If the identity type was invalid, or if there was an error while retrieving the
     * objectclasses.
     */
    @Override
    public Set<String> getAssignedServices(SSOToken token, IdType type, String name,
            Map<String, Set<String>> mapOfServicesAndOCs) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getAssignedServices invoked");
        }
        Set<String> results = new HashSet<String>();
        if (type.equals(IdType.USER)) {
            Set<String> attrs = asSet("objectclass");
            Set<String> objectClasses = getAttributes(token, type, name, attrs).get(OBJECT_CLASS_ATTR);
            if (objectClasses != null) {
                objectClasses = new CaseInsensitiveHashSet(objectClasses);
            }
            for (Map.Entry<String, Set<String>> entry : mapOfServicesAndOCs.entrySet()) {
                String serviceName = entry.getKey();
                Set<String> serviceOCs = entry.getValue();
                if (objectClasses != null && objectClasses.containsAll(serviceOCs)) {
                    results.add(serviceName);
                }
            }
        } else if (type.equals(IdType.REALM)) {
            results.addAll(serviceMap.keySet());
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "213", new Object[]{CLASS_NAME});
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Assigned services returned: " + results);
        }
        return results;
    }

    /**
     * Returns the service attributes in string format for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity to query. Only used when identity type is USER.
     * @param serviceName The name of the service, which in case of USER may be null.
     * @param attrNames The name of the service attributes that needs to be queried. In case of USER this may NOT be
     * null. In case of REALM, when null this will return all attributes for the service.
     * @return The matching service attributes.
     * @throws IdRepoException If there was an error while retrieving the service attributes from the user, or if the
     * identity type was invalid.
     */
    @Override
    public Map<String, Set<String>> getServiceAttributes(SSOToken token, IdType type, String name, String serviceName,
            Set<String> attrNames) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getServiceAttributes invoked");
        }
        return getServiceAttributes(type, name, serviceName, attrNames, new StringAttributeExtractor(), new StringToStringConverter());
    }

    /**
     * Returns the service attributes in binary format for the given identity.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity to query. Only used when identity type is USER.
     * @param serviceName The name of the service, which in case of USER may be null.
     * @param attrNames The name of the service attributes that needs to be queried. In case of USER this may NOT be
     * null. In case of REALM, when null this will return all attributes for the service.
     * @return The matching service attributes.
     * @throws IdRepoException If there was an error while retrieving the service attributes from the user, or if the
     * identity type was invalid.
     */
    @Override
    public Map<String, byte[][]> getBinaryServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set<String> attrNames) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getBinaryServiceAttributes invoked");
        }
        return getServiceAttributes(type, name, serviceName, attrNames, new BinaryAttributeExtractor(), new StringToBinaryConverter());
    }

    /**
     * Returns the service attributes in binary or string format for the given identity.
     * In case of a USER this will retrieve first the service attributes from the user entry, and later it will also
     * query the service attributes of the current realm. When a user-specific setting is missing the realm-specific one
     * will be returned instead.
     * In case of a REALM it will return a defensive copy of the service attributes stored locally.
     *
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity to query. Only used when identity type is USER.
     * @param serviceName The name of the service, which in case of USER may be null.
     * @param attrNames The name of the service attributes that needs to be queried. In case of USER this may NOT be
     * null. In case of REALM, when null this will return all attributes for the service.
     * @param extractor The attribute extractor to use.
     * @param converter The attribute filter to use.
     * @return The matching service attributes.
     * @throws IdRepoException If there was an error while retrieving the service attributes from the user, or if the
     * identity type was invalid.
     */
    private <T> Map<String, T> getServiceAttributes(IdType type, String name, String serviceName,
            Set<String> attrNames, Function<Attribute, T, Void> extractor,
            Function<Map<String, Set<String>>, Map<String, T>, Void> converter) throws IdRepoException {
        if (type.equals(IdType.USER)) {
            Map<String, T> attrsFromUser = getAttributes(type, name, attrNames, extractor);
            if (serviceName == null || serviceName.isEmpty()) {
                return attrsFromUser;
            }
            Map<String, Set<String>> attrsFromRealm = serviceMap.get(serviceName);
            Map<String, Set<String>> filteredAttrsFromRealm = new HashMap<String, Set<String>>();
            if (attrsFromRealm == null || attrsFromRealm.isEmpty()) {
                return attrsFromUser;
            } else {
                attrNames = new CaseInsensitiveHashSet(attrNames);
                for (Map.Entry<String, Set<String>> entry : attrsFromRealm.entrySet()) {
                    String attrName = entry.getKey();
                    if (attrNames.contains(attrName)) {
                        filteredAttrsFromRealm.put(attrName, entry.getValue());
                    }
                }
            }

            Map<String, T> filteredAttrsFromRealm2 = converter.apply(filteredAttrsFromRealm, null);
            Set<String> attrNameSet = new CaseInsensitiveHashSet(attrsFromUser.keySet());
            for (Map.Entry<String, T> entry : filteredAttrsFromRealm2.entrySet()) {
                String attrName = entry.getKey();
                if (!attrNameSet.contains(attrName)) {
                    attrsFromUser.put(attrName, entry.getValue());
                }
            }
            return attrsFromUser;
        } else if (type.equals(IdType.REALM)) {
            Map<String, T> attrs = converter.apply(serviceMap.get(serviceName), null);
            Map<String, T> results = new HashMap<String, T>();
            if (attrs == null || attrs.isEmpty()) {
                return results;
            }
            if (attrNames == null || attrNames.isEmpty()) {
                results.putAll(attrs);
                return results;
            } else {
                Set<String> attributeNames = new CaseInsensitiveHashSet(attrs.keySet());
                for (String attrName : attrNames) {
                    if (attributeNames.contains(attrName)) {
                        results.put(attrName, attrs.get(attrName));
                    }
                }
                return results;
            }
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "213", new Object[]{CLASS_NAME});
        }
    }

    /**
     * Modifies the service attributes based on the incoming attributeMap.
     * In case of a USER the attributes will be saved in case the schema type is not DYNAMIC.
     * In case of a REALM this will only modify the locally stored Map structure by making sure that non-modified
     * attributes are kept.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity. Only used when identity type is USER.
     * @param serviceName The name of the service that needs to be modified.
     * @param sType The type of the service schema.
     * @param attrMap The attributes that needs to be set for the service.
     * @throws IdRepoException If the type was invalid, or if there was an error while setting the service attributes. 
     */
    @Override
    public void modifyService(SSOToken token, IdType type, String name, String serviceName,
            SchemaType sType, Map<String, Set<String>> attrMap) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("modifyService invoked");
        }
        if (type.equals(IdType.USER)) {
            if (sType.equals(SchemaType.DYNAMIC)) {
                throw newIdRepoException("214", CLASS_NAME, sType.toString(), type.getName());
            } else {
                setAttributes(token, type, name, (Map) attrMap, false, true, false);
            }
        } else if (type.equals(IdType.REALM)) {
            Map<String, Set<String>> previousAttrs = serviceMap.get(serviceName);
            if (previousAttrs == null || previousAttrs.isEmpty()) {
                serviceMap.put(serviceName, new HashMap<String, Set<String>>(attrMap));
            } else {
                Set<String> previousAttrNames = new CaseInsensitiveHashSet(previousAttrs.keySet());
                for (Map.Entry<String, Set<String>> entry : attrMap.entrySet()) {
                    String attrName = entry.getKey();
                    Set<String> values = entry.getValue();
                    if (previousAttrNames.contains(attrName)) {
                        Set<String> current = previousAttrs.get(attrName);
                        current.clear();
                        current.addAll(values);
                    } else {
                        previousAttrs.put(attrName, values);
                    }
                }
                serviceMap.put(serviceName, previousAttrs);
            }
            if (idRepoListener != null) {
                idRepoListener.setServiceAttributes(serviceName, serviceMap);
            }
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "213", new Object[]{CLASS_NAME});
        }
    }

    /**
     * Unassigns a service from the provided identity.
     * In case of a USER this will traverse through all the existing user attributes and will remove those that are
     * currently present in the entry. This will also remove the objectclass corresponding to the service.
     * In case of a REALM this will remove the service from the locally cached serviceMap, and will notify the
     * registered {@link IdRepoListener}.
     *
     * @param token Not used.
     * @param type The type of the identity, this should be always USER or REALM.
     * @param name The name of the identity. Only used when identity type is USER.
     * @param serviceName The name of the service to remove from the identity.
     * @param attrMap Holds the objectclasses relevant for this service removal.
     * @throws IdRepoException If the identity type was invalid or if there was an error while removing the service.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void unassignService(SSOToken token, IdType type, String name, String serviceName,
            Map<String, Set<String>> attrMap) throws IdRepoException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("unassignService invoked");
        }
        if (type.equals(IdType.USER)) {
            Set<String> removeOCs = attrMap.get(OBJECT_CLASS_ATTR);
            if (removeOCs != null) {
                Schema dirSchema = getSchema();
                Map attrs = new CaseInsensitiveHashMap();
                for (String oc : removeOCs) {
                    try {
                        ObjectClass oc2 = dirSchema.getObjectClass(oc);
                        for (AttributeType optional : oc2.getOptionalAttributes()) {
                            attrs.put(optional.getNameOrOID(), Collections.EMPTY_SET);
                        }
                        for (AttributeType required : oc2.getRequiredAttributes()) {
                            attrs.put(required.getNameOrOID(), Collections.EMPTY_SET);
                        }
                    } catch (UnknownSchemaElementException usee) {
                        DEBUG.error("Unable to unassign " + serviceName + " service from identity: " + name, usee);
                        throw newIdRepoException("102", serviceName);
                    }
                }
                Set<String> requestedAttrs = new CaseInsensitiveHashSet(attrs.keySet());
                //if the service objectclass is auxiliary (which it should be), then the objectclass attribute may not
                //be present if top is not defined as superior class.
                requestedAttrs.add(OBJECT_CLASS_ATTR);
                Map<String, Set<String>> attributes = new CaseInsensitiveHashMap(
                        getAttributes(token, type, name, requestedAttrs));
                Set<String> OCValues = new CaseInsensitiveHashSet(attributes.get(OBJECT_CLASS_ATTR));
                OCValues.removeAll(removeOCs);
                attrs.put(OBJECT_CLASS_ATTR, OCValues);
                //we need to only change existing attributes, removal of a non-existing attribute results in failure.
                //implementing retainAll here for CaseInsensitiveHashMap's keySet
                for (String string : (Set<String>) attrs.keySet()) {
                    if (!attributes.containsKey(string)) {
                        attrs.remove(string);
                    }
                }
                setAttributes(token, type, name, attrs, false, true, false);
            }
        } else if (type.equals(IdType.REALM)) {
            if (serviceName != null && !serviceName.isEmpty()) {
                serviceMap.remove(serviceName);
            }
            if (idRepoListener != null) {
                idRepoListener.setServiceAttributes(serviceName, serviceMap);
            }
        } else {
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, "213", new Object[]{CLASS_NAME});
        }
    }

    /**
     * Registers an IdRepoListener, which will be notified of realm level service changes and persistent search results.
     * If persistent search is not yet established with the current settings, this will create a new persistent search
     * against the configured directory.
     *
     * @param token Not used.
     * @param idRepoListener The IdRepoListener that will be used to notify about service changes and persistent search
     * results.
     * @return Always returns <code>0</code>.
     */
    @Override
    public int addListener(SSOToken token, IdRepoListener idRepoListener) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("addListener invoked");
        }
        String psearchBaseDN = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_BASE_DN);
        if (psearchBaseDN == null || psearchBaseDN.isEmpty()) {
            DEBUG.error("Persistent search base DN is missing, persistent search is disabled.");
            return 0;
        }
        String pSearchId = getPSearchId();

        //basically we should have only one idRepoListener per IdRepo
        if (this.idRepoListener != null) {
            throw new IllegalStateException("There is an idRepoListener already registered within this IdRepo");
        }
        this.idRepoListener = idRepoListener;
        synchronized (pSearchMap) {
            DJLDAPv3PersistentSearch pSearch = pSearchMap.get(pSearchId);
            String username = CollectionHelper.getMapAttr(configMap, LDAP_SERVER_USER_NAME);
            char[] password = CollectionHelper.getMapAttr(configMap, LDAP_SERVER_PASSWORD, "").toCharArray();
            if (pSearch == null) {
                pSearch = new DJLDAPv3PersistentSearch(configMap, createConnectionFactory(username, password, 1));
                if (dnCacheEnabled) {
                    pSearch.addMovedOrRenamedListener(this);
                }
                pSearch.addListener(idRepoListener, getSupportedTypes());
                pSearch.startPSearch();
                pSearchMap.put(pSearchId, pSearch);
            } else {
                pSearch.addListener(idRepoListener, getSupportedTypes());
                if (dnCacheEnabled) {
                    pSearch.addMovedOrRenamedListener(this);
                }
            }
        }
        return 0;
    }

    /**
     * This method will be called by the end of the IdRepo's lifetime, and makes sure that persistent search is properly
     * terminated for this IdRepo.
     */
    @Override
    public void removeListener() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("removelistener invoked");
        }
        String pSearchId = getPSearchId();
        synchronized (pSearchMap) {
            DJLDAPv3PersistentSearch pSearch = pSearchMap.get(pSearchId);
            if (pSearch == null) {
                DEBUG.error("PSearch is already removed, unable to unregister");
            } else {
                pSearch.removeMovedOrRenamedListener(this);
                pSearch.removeListener(idRepoListener);
                if (!pSearch.hasListeners()) {
                    pSearch.stopPSearch();
                    pSearchMap.remove(pSearchId);
                }
            }
        }
    }

    /**
     * This method is being invoked during OpenAM shutdown and also when the configuration changes and OpenAM needs to
     * reload the Data Store configuration. This mechanism will make sure that the connection pools are closed, and the
     * persistent search is also terminated (if there is no other data store implementation using the same psearch
     * connection.
     */
    @Override
    public void shutdown() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("shutdown invoked");
        }
        super.shutdown();
        removeListener();
        IOUtils.closeIfNotNull(connectionFactory);
        IOUtils.closeIfNotNull(bindConnectionFactory);
        idRepoListener = null;
    }

    /**
     * Called if an identity has been renamed or moved within the identity store.
     * @param previousDN The DN of the identity before the move or rename
     */
    public void identityMovedOrRenamed(DN previousDN) {

        if (dnCacheEnabled) {
            String name = LDAPUtils.getName(previousDN);
            for (IdType idType : getSupportedTypes()) {
                String previousId =  generateDNCacheKey(name, idType);
                Object previousDn = dnCache.remove(previousId);
                if (DEBUG.messageEnabled() && previousDn != null) {
                    DEBUG.message("Removed " + previousId + " from DN Cache");
                }
            }
        }
    }

    /**
     * This method constructs a persistent search "key", which will be used to
     * figure out whether there is an existing persistent search for the same
     * ldap server, base DN, filter, scope combination. By doing this we can
     * "reuse" the results of other datastore implementations without the need
     * of two or more persistent search connections with the same parameters.
     *
     * @return a unique ID based on the LDAP URLs, psearch base DN, filter and
     * scope settings.
     */
    private String getPSearchId() {
        String psearchBase = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_BASE_DN);
        String pfilter = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_FILTER);
        String scope = CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_SCOPE);
        //creating a natural order of the ldap servers, so the "key" should be always the same regardless of the server
        //order in the configuration.
        LDAPURL[] servers = ldapServers.toArray(new LDAPURL[ldapServers.size()]);
        Arrays.sort(servers);
        String psIdKey = Arrays.toString(servers) + psearchBase + pfilter + scope;
        return psIdKey;
    }

    private void mapUserStatus(IdType type, Map<String, Set<String>> attributes) {
        if (type.equals(IdType.USER)) {
            String userStatus = CollectionHelper.getMapAttr(attributes, DEFAULT_USER_STATUS_ATTR);
            Set<String> value = new HashSet<String>(1);
            if (userStatus == null) {
                value.add(activeValue);
            } else if (userStatus.equalsIgnoreCase(STATUS_INACTIVE)) {
                value.add(inactiveValue);
            } else {
                value.add(activeValue);
            }
            attributes.put(userStatusAttr, value);
        }
    }

    private void mapCreationAttributes(IdType type, String name, Map<String, Set<String>> attributes) {
        if (type.equals(IdType.USER)) {
            for (Map.Entry<String, String> mapping : creationAttributeMapping.entrySet()) {
                String from = mapping.getKey();
                if (!attributes.containsKey(from)) {
                    String to = mapping.getValue();
                    //if the attrname is same as the attrvalue, use the username as the value of the attribute.
                    if (from.equalsIgnoreCase(to)) {
                        attributes.put(from, asSet(name));
                    } else {
                        Set<String> value = attributes.get(to);
                        if (value != null) {
                            attributes.put(from, value);
                        }
                    }
                }
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("After adding creation attributes: attrMap =  "
                    + IdRepoUtils.getAttrMapWithoutPasswordAttrs(attributes, null));
        }
    }

    @SuppressWarnings("rawtypes")
    private Map removeUndefinedAttributes(IdType type, Map attributes) {
        Set<String> predefinedAttrs = getDefinedAttributes(type);

        Map filteredMap = new CaseInsensitiveHashMap(attributes);
        for (String key : (Set<String>) attributes.keySet()) {
            if (!predefinedAttrs.contains(key)) {
                filteredMap.remove(key);
            }
        }
        return filteredMap;
    }

    private Set<String> removeUndefinedAttributes(IdType type, Set<String> attributes) {
        Set<String> predefinedAttrs = getDefinedAttributes(type);
        Set<String> filteredSet = Collections.EMPTY_SET;
        if (attributes != null) {
            filteredSet = new CaseInsensitiveHashSet(attributes);
        }
        filteredSet.retainAll(predefinedAttrs);
        return filteredSet;
    }

    private Set<String> getDefinedAttributes(IdType type) {
        Set<String> predefinedAttrs = Collections.EMPTY_SET;
        if (type.equals(IdType.USER)) {
            predefinedAttrs = userAttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            predefinedAttrs = groupAttributesAllowed;
        } else if (type.equals(IdType.ROLE)) {
            predefinedAttrs = roleAttributesAllowed;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            predefinedAttrs = filteredRoleAttributesAllowed;
        }
        return predefinedAttrs;
    }

    private String getNamingAttribute(IdType type) {
        if (type.equals(IdType.USER)) {
            return userNamingAttr;
        } else if (type.equals(IdType.GROUP)) {
            return groupNamingAttr;
        } else if (type.equals(IdType.ROLE)) {
            return roleNamingAttr;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return filteredRoleNamingAttr;
        } else {
            return userNamingAttr;
        }
    }

    private String getSearchAttribute(IdType type) {
        if (type.equals(IdType.USER)) {
            return userSearchAttr;
        } else {
            return getNamingAttribute(type);
        }
    }

    private Set<String> getNonNullSettingValues(String setting) {
        Set<String> value = configMap.get(setting);
        if (value == null) {
            return Collections.EMPTY_SET;
        } else {
            return value;
        }
    }

    private String getDN(IdType type, String name) throws IdRepoException {
        return getDN(type, name, false, null);
    }

    private String generateDN(IdType type, String name) throws IdRepoException {
        return getDN(type, name, true, null);
    }

    private String findDNForAuth(IdType type, String name) throws IdRepoException {
        return getDN(type, name, false, userNamingAttr);
    }

    private String getDN(IdType type, String name, boolean shouldGenerate, String searchAttr) throws IdRepoException {

        Object cachedDn = null;
        if (dnCacheEnabled) {
            cachedDn = dnCache.get(generateDNCacheKey(name, type));
        }
        if (cachedDn != null) {
            return cachedDn.toString();
        }
        String dn = null;
        DN searchBase = getBaseDN(type);

        if (shouldGenerate) {
            return searchBase.child(getSearchAttribute(type), name).toString();
        }

        if (searchAttr == null) {
            searchAttr = getSearchAttribute(type);
        }
        Filter filter = Filter.and(Filter.equality(searchAttr, name), getObjectClassFilter(type));
        SearchRequest searchRequest = Requests.newSearchRequest(searchBase, defaultScope, filter, DN_ATTR);
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            ConnectionEntryReader reader = conn.search(searchRequest);
            SearchResultEntry entry = null;
            while (reader.hasNext()) {
                if (reader.isEntry()) {
                    if (entry != null) {
                        throw newIdRepoException(ResultCode.CLIENT_SIDE_UNEXPECTED_RESULTS_RETURNED, "306", CLASS_NAME,
                                ResultCode.CLIENT_SIDE_UNEXPECTED_RESULTS_RETURNED.intValue());
                    }
                    entry = reader.readEntry();
                } else {
                    //ignore references
                    reader.readReference();
                }
            }
            if (entry == null) {
                DEBUG.message("Unable to find entry with name: " + name + " under searchbase: " + searchBase
                        + " with scope: " + defaultScope);
                throw new IdentityNotFoundException(ResultCode.CLIENT_SIDE_NO_RESULTS_RETURNED, "223", name,
                        type.getName());
            }
            dn = entry.getName().toString();
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while querying entry DN", ere);
            handleErrorResult(ere);
        } catch (ErrorResultIOException erioe) {
            handleIOError(erioe, "#getDN");
        } catch (SearchResultReferenceIOException srrioe) {
            //should never ever happen...
            DEBUG.error("Got reference instead of entry", srrioe);
            throw newIdRepoException("219", CLASS_NAME);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }

        if (dnCacheEnabled && !shouldGenerate) {
            dnCache.put(generateDNCacheKey(name, type), dn);
        }
        return dn;
    }

    private Filter getObjectClassFilter(IdType type) {
        if (type.equals(IdType.USER)) {
            return userSearchFilter;
        } else if (type.equals(IdType.GROUP)) {
            return groupSearchFilter;
        } else if (type.equals(IdType.ROLE)) {
            return roleSearchFilter;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return filteredRoleSearchFilter;
        } else {
            return userSearchFilter;
        }
    }

    private Set<String> getObjectClasses(IdType type) {
        if (type.equals(IdType.USER)) {
            return userObjectClasses;
        } else if (type.equals(IdType.GROUP)) {
            return groupObjectClasses;
        } else if (type.equals(IdType.ROLE)) {
            return roleObjectClasses;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            return filteredRoleObjectClasses;
        } else {
            //should never happen
            return Collections.EMPTY_SET;
        }
    }

    private DN getBaseDN(IdType type) {
        DN dn = DN.valueOf(rootSuffix);
        if (type.equals(IdType.USER) && peopleContainerName != null && !peopleContainerName.isEmpty()
                && peopleContainerValue != null && !peopleContainerValue.isEmpty()) {
            dn = dn.child(peopleContainerName, peopleContainerValue);
        } else if (type.equals(IdType.GROUP) && groupContainerName != null && !groupContainerName.isEmpty()
                && groupContainerValue != null && !groupContainerValue.isEmpty()) {
            dn = dn.child(groupContainerName, groupContainerValue);
        }

        return dn;
    }

    protected Filter constructFilter(int operation, Map<String, Set<String>> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        Set<Filter> filters = new LinkedHashSet<Filter>(attributes.size());
        for (Map.Entry<String, Set<String>> entry : attributes.entrySet()) {
            for (String value : entry.getValue()) {
                filters.add(Filter.valueOf(entry.getKey() + "=" + partiallyEscapeAssertionValue(value)));
            }
        }
        Filter filter;
        switch (operation) {
            case OR_MOD:
                filter = Filter.or(filters);
                break;
            case AND_MOD:
                filter = Filter.and(filters);
                break;
            default:
                //falling back to AND
                filter = Filter.and(filters);
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("constructFilter returned filter: " + filter.toString());
        }
        return filter;
    }

    /**
     * Escapes the provided assertion value according to the LDAP standard. As a special case this method does not
     * escape the '*' character, in order to be able to use wildcards in filters.
     *
     * @param assertionValue The filter assertionValue that needs to be escaped.
     * @return The escaped assertionValue.
     */
    private String partiallyEscapeAssertionValue(String assertionValue) {
        StringBuilder sb = new StringBuilder(assertionValue.length());
        for (int j = 0; j < assertionValue.length(); j++) {
            char c = assertionValue.charAt(j);
            if (c == '*') {
                sb.append(c);
            } else {
                sb.append(Filter.escapeAssertionValue(String.valueOf(c)));
            }
        }
        return sb.toString();
    }

    protected Schema getSchema() throws IdRepoException {
        if (schema == null) {
            synchronized (this) {
                if (schema == null) {
                    Connection conn = null;
                    try {
                        conn = connectionFactory.getConnection();
                        schema = Schema.readSchemaForEntry(conn, DN.valueOf(rootSuffix)).asStrictSchema();
                    } catch (ErrorResultException ere) {
                        DEBUG.error("Unable to read the directory schema", ere);
                        throw new IdRepoException("Unable to read the directory schema");
                    } finally {
                        IOUtils.closeIfNotNull(conn);
                    }
                }
            }
        }
        return schema;
    }

    private void handleErrorResult(ErrorResultException ere) throws IdRepoException {
        ResultCode resultCode = ere.getResult().getResultCode();
        if (ResultCode.CONSTRAINT_VIOLATION.equals(resultCode)) {
            throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "313",
                    new Object[]{CLASS_NAME, resultCode.intValue(), ere.getResult().getDiagnosticMessage()});
        } else if (ResultCode.NO_SUCH_OBJECT.equals(resultCode)) {
            throw new IdentityNotFoundException(resultCode, "220", CLASS_NAME, ere.getResult().getDiagnosticMessage());
        } else {
            throw newIdRepoException(resultCode, "306", CLASS_NAME, resultCode.intValue());
        }
    }

    private void handleIOError(ErrorResultIOException erioe, String method) throws IdRepoException {
        ErrorResultException cause = erioe.getCause();
        if (cause != null) {
            ResultCode resultCode = cause.getResult().getResultCode();
            if (resultCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                DEBUG.warning("Size limit exceeded in " + method);
            } else if (resultCode.equals(ResultCode.TIME_LIMIT_EXCEEDED)) {
                DEBUG.warning("Time limit exceeded in " + method);
            } else {
                DEBUG.error("Unexpected IO error occurred in " + method, erioe);
                throw newIdRepoException(resultCode, "311", erioe.getMessage());
            }
        } else {
            DEBUG.error("An IO problem occurred in" + method, erioe);
            throw newIdRepoException("311", erioe.getMessage());
        }
    }

    private IdRepoException newIdRepoException(String key, Object... args) {
        return new IdRepoException(IdRepoBundle.BUNDLE_NAME, key, args);
    }

    private IdRepoException newIdRepoException(ResultCode resultCode, String key, Object... args) {
        return new IdRepoException(IdRepoBundle.BUNDLE_NAME, key, String.valueOf(resultCode.intValue()), args);
    }

    private static class StringAttributeExtractor implements Function<Attribute, Set<String>, Void> {

            public Set<String> apply(Attribute value, Void p) {
                return LDAPUtils.getAttributeValuesAsStringSet(value);
            }
    }

    private static class BinaryAttributeExtractor implements Function<Attribute, byte[][], Void> {

        public byte[][] apply(Attribute attr, Void arg) {
            byte[][] values = new byte[attr.size()][];
            int counter = 0;
            for (ByteString byteString : attr) {
                byte[] bytes = byteString.toByteArray();
                values[counter++] = bytes;
            }
            return values;
        }
    }

    private static class StringToStringConverter implements Function<Map<String, Set<String>>, Map<String, Set<String>>, Void> {

        public Map<String, Set<String>> apply(Map<String, Set<String>> value, Void p) {
            return value;
        }
    }

    private static class StringToBinaryConverter implements Function<Map<String, Set<String>>, Map<String, byte[][]>, Void> {

        public Map<String, byte[][]> apply(Map<String, Set<String>> value, Void p) {
            Map<String, byte[][]> result = new HashMap<String, byte[][]>(value.size());
            for (Map.Entry<String, Set<String>> entry : value.entrySet()) {
                Set<String> values = entry.getValue();
                byte[][] binary = new byte[values.size()][];
                int counter = 0;
                for (String val : values) {
                    binary[counter++] = val.getBytes(Charset.forName("UTF-8"));
                }
                result.put(entry.getKey(), binary);
            }
            return result;
        }
    }

    private String generateDNCacheKey(String name, IdType idType) {
        return name + "," + idType;
    }
}
