/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AgentsRepo.java,v 1.46 2009/09/21 19:47:28 goodearth Exp $
 *
 * Portions Copyrighted 2012-2015 ForgeRock AS.
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.idm.plugins.internal;

import static com.google.inject.name.Names.named;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.CollectionUtils.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.google.inject.Key;
import com.iplanet.services.comm.server.PLLServer;
import com.iplanet.services.comm.server.SendNotificationException;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceAlreadyExistsException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchemaManager;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.notifications.LocalOnly;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.NotificationsConfig;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.openam.utils.CrestQuery;

public class AgentsRepo extends IdRepo implements ServiceListener {

    public static final String NAME =
        "com.sun.identity.idm.plugins.internal.AgentsRepo";

    // Status attribute
    private static final String statusAttribute =
        "sunIdentityServerDeviceStatus";
    private static final String statusActive = "Active";
    private static final String statusInactive = "Inactive";
    private static final String version = "1.0";
    private static final String comma = ",";
    private static final String agentserviceName = IdConstants.AGENT_SERVICE;
    private static final String AGENT_GROUP = "agentgroup";
    private static final String instancesNode = "ou=Instances,";
    private static final String hashAlgStr = "{SHA-1}";
    private static final String oauth2Attribute = "com.forgerock.openam.oauth2provider.clientType";

    IdRepoListener repoListener = null;

    Debug debug = Debug.getInstance("amAgentsRepo");

    private String realmName;

    private Map supportedOps = new HashMap();

    private static ServiceSchemaManager ssm = null;

    private static ServiceConfigManager scm = null;

    ServiceConfig orgConfigCache, agentGroupConfigCache;

    String ssmListenerId, scmListenerId;

    private static String notificationURLname =
        "com.sun.identity.client.notification.url";
    private static String notificationURLenabled =
        "com.sun.identity.agents.config.change.notification.enable";

    public static final String AGENT_CONFIG_SERVICE = "agentconfig";
    static final String AGENT_NOTIFICATION = "AgentConfigChangeNotification";
    static final String AGENT_ID = "agentName";
    static final String AGENT_IDTYPE = "IdType";

    // Initialization exception
    IdRepoException initializationException;

    private final NotificationBroker broker;
    private final NotificationsConfig notificationConfig;

    public AgentsRepo() {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        if (debug.messageEnabled()) {
            debug.message(": AgentsRepo adding Listener");
        }
        try {
            ssm = new ServiceSchemaManager(adminToken, agentserviceName,
                version);
            scm = new ServiceConfigManager(adminToken, agentserviceName,
                version);

            if (ssm != null) {
                ssmListenerId = ssm.addListener(this);
            }

            if (scm != null) {
                scmListenerId = scm.addListener(this);
            }
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AgentsRepo.AgentsRepo: "
                        + "Unable to init ssm and scm due to " + smse);
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AgentsRepo.AgentsRepo: "
                        + "Unable to init ssm and scm due to " + ssoe);
            }
        }

        loadSupportedOps();
        if (debug.messageEnabled()) {
            debug.message("AgentsRepo invoked");
        }
        broker = InjectorHolder.getInstance(Key.get(NotificationBroker.class, LocalOnly.class));
        notificationConfig = InjectorHolder.getInstance(NotificationsConfig.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#addListener(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdRepoListener)
     */
    public int addListener(SSOToken token, IdRepoListener listener)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.addListener().");
        }
        // Listeners are added when AgentsRepo got invoked.
        repoListener = listener;
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#create(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map)
     */
    public String create(SSOToken token, IdType type, String agentName,
        Map attrMap) throws IdRepoException, SSOException {

        if (agentName.startsWith("\"")) {
            agentName = "\\" + agentName ;
        }
        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.create() called: " + type + ": "
                    + agentName);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.create: "
                    + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        if (attrMap == null || attrMap.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("AgentsRepo.create(): Attribute Map is empty ");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ILLEGAL_ARGUMENTS, null);
        }
        String agentType = null;
        ServiceConfig aTypeConfig = null;
        if (attrMap != null && !attrMap.isEmpty()) {
            if ((attrMap.keySet()).contains(IdConstants.AGENT_TYPE)) {
                Set aTypeSet = (HashSet) attrMap.get(IdConstants.AGENT_TYPE);

                if ((aTypeSet != null) && (!aTypeSet.isEmpty())) {
                    agentType = (String) aTypeSet.iterator().next();
                    attrMap.remove(IdConstants.AGENT_TYPE);
                } else {
                    debug.error("AgentsRepo.create():Unable to create agents."
                       + " Agent Type "+aTypeSet+ " is empty");
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ILLEGAL_ARGUMENTS,
                        null);
                }
            } else {
                // To be backward compatible, look for 'AgentType' attribute
                // in the attribute map which is passed as a parameter and if
                // not present/sent, check if the IdType.AGENTONLY and then
                // assume that it is '2.2_Agent' type and create that agent
                // under the 2.2_Agent node.

                if (type.equals(IdType.AGENTONLY) ||
                    type.equals(IdType.AGENT)) {
                    agentType = "2.2_Agent";
                } else {
                    debug.error("AgentsRepo.create():Unable to create agents."
                       + " Agent Type "+agentType+ " is empty");
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ILLEGAL_ARGUMENTS,
                        null);
                }
            }
        }
        try {
            Set<String> vals = (Set) attrMap.get("userpassword");
            if ((vals != null) && !AgentConfiguration.AGENT_TYPE_OAUTH2.equals(agentType)) {
                Set<String> hashedVals = new HashSet<String>(vals.size());
                for (String val : vals) {
                    // If the password is already a hashed value, leave as is.
                    if (val.startsWith(hashAlgStr)) {
                        hashedVals.add(val);
                    } else {
                        hashedVals.add(hashAlgStr + Hash.hash(val));
                    }
                }
                attrMap.remove("userpassword");
                attrMap.put("userpassword", hashedVals);
            }

            if (type.equals(IdType.AGENTONLY) || type.equals(IdType.AGENT)) {
                ServiceConfig orgConfig = getOrgConfig(token);
                if (!orgConfig.getSubConfigNames().contains(agentName)) {
                    /*
                     * While migrating 2.2 agents to new ones, look for the
                     * attribute 'entrydn' and  remove this 'entrydn' while
                     * creating the agent, as it gets added in a
                     * getAttributes() call explicitly to the result set and
                     * returned. Reason:
                     *  When queried with this entrydn/dn the lower level
                     *  api/ ldapjdk does not return this operational attribute.
                     */
                    if (attrMap.containsKey("entrydn")) {
                        attrMap.remove("entrydn");
                    }
                    orgConfig.addSubConfig(agentName, agentType, 0, attrMap);
                    aTypeConfig = orgConfig.getSubConfig(agentName);
                } else {
                    // Agent already found, throw an exception
                    throw IdRepoDuplicateObjectException.identityOfTypeAlreadyExists(agentName, type.getName());
                }
            } else if (type.equals(IdType.AGENTGROUP)) {
                ServiceConfig agentGroupConfig = getAgentGroupConfig(token);
                if (agentGroupConfig==null) {
                	agentGroupConfig = createAgentGroupConfig(token);
                }
                if (!agentGroupConfig.getSubConfigNames().
                    contains(agentName)) {
                    agentGroupConfig.addSubConfig(agentName, agentType, 0,
                        attrMap);
                    aTypeConfig = agentGroupConfig.getSubConfig(agentName);
                } else {
                    // Agent already found, throw an exception
                    throw IdRepoDuplicateObjectException.identityOfTypeAlreadyExists(agentName, type.getName());
                }
            }
        } catch (ServiceAlreadyExistsException saee) {
            debug.error("AgentsRepo.create():Unable to create agents ", saee);
            throw IdRepoDuplicateObjectException.identityOfTypeAlreadyExists(agentName, type.getName());
        } catch (SMSException smse) {
            debug.error("AgentsRepo.create():Unable to create agents ", smse);
            Object args[] = { NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_CREATE_AGENT, args);
        }
        return (aTypeConfig.getDN());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#delete(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public void delete(SSOToken token, IdType type, String name)
        throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.delete() called: " + type + ": "
                    + name);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.delete: "
                + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        ServiceConfig aCfg = null;
        try {
            if (type.equals(IdType.AGENTONLY) || type.equals(IdType.AGENT)) {
                ServiceConfig orgConfig = getOrgConfig(token);
                aCfg = orgConfig.getSubConfig(name);
                if (aCfg != null) {
                    String agentType = orgConfig.getSubConfig(name).getSchemaID();
                    boolean isSharedAgent = agentType.equals("SharedAgent");
                    orgConfig.removeSubConfig(name);
                    if (!isSharedAgent) {
                        removeIdentityFromAgentAuthenticators(name);
                    }
                } else {
                    // Agent not found, throw an exception
                    Object args[] = { name, type.getName() };
                    throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                            IdRepoErrorCode.TYPE_NOT_FOUND, args));
                }
            } else if (type.equals(IdType.AGENTGROUP)) {
                ServiceConfig agentGroupConfig = getAgentGroupConfig(token);
                if (agentGroupConfig==null) {
                	// Agent not found, throw an exception
                    Object args[] = { name, type.getName() };
                    throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                            IdRepoErrorCode.TYPE_NOT_FOUND, args));
                }
                aCfg = agentGroupConfig.getSubConfig(name);
                if (aCfg != null) {
                    // AgentGroup deletion should clear the group memberships of the agents that belong to this group.
                    // Get the members that belong to this group and their config and set the agentgroup attribute to
                    // an empty string.
                    Set<String> members = getMembers(token, type, name, IdType.AGENTONLY);
                    ServiceConfig memberCfg;
                    for (String agent : members) {
                        memberCfg = getOrgConfig(token).getSubConfig(agent);
                        if (memberCfg != null) {
                            memberCfg.removeAttribute(AGENT_GROUP);
                        }
                    }
                    agentGroupConfig.removeSubConfig(name);
                } else {
                    // Agent not found, throw an exception
                    Object args[] = { name, type.getName() };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.TYPE_NOT_FOUND, args);
                }
            }
        } catch (SMSException smse) {
            debug.error("AgentsRepo.delete: Unable to delete agents ", smse);
            Object args[] = { NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_READ_ATTRIBUTES, args);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public Map getAttributes(SSOToken token, IdType type, String name,
        Set attrNames) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getAttributes() with attrNames called: "
                + type + ": " + name);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.getAttributes: "
                + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        CaseInsensitiveHashMap allAtt = new CaseInsensitiveHashMap(
                getAttributes(token, type, name));
        Map resultMap = new HashMap();
        Iterator it = attrNames.iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            if (allAtt.containsKey(attrName)) {
                resultMap.put(attrName, allAtt.get(attrName));
            }
        }
        return resultMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public Map getAttributes(SSOToken token, IdType type, String name)
         throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getAttributes() called: " + type + ": "
                + name);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.getAttributes: "
                + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        if (type.equals(IdType.AGENT) || type.equals(IdType.AGENTONLY) ||
            type.equals(IdType.AGENTGROUP)) {
            Map agentsAttrMap = new HashMap(2);
            try {
                if (type.equals(IdType.AGENTONLY)) {
                    // Return the attributes for the given agent under
                    // default group.
                    ServiceConfig orgConfig = getOrgConfig(token);
                    agentsAttrMap = getAgentAttrs(orgConfig, name, type);
                } else if (type.equals(IdType.AGENTGROUP)) {
                    ServiceConfig agentGroupConfig =
                        getAgentGroupConfig(token);
                    // Return the attributes of agent under specified group.
                    agentsAttrMap =
                        getAgentAttrs(agentGroupConfig, name, type);
                } else if (type.equals(IdType.AGENT)) {
                    // By default return the union of agents under
                    // default group and the agent group.
                    ServiceConfig orgConfig = getOrgConfig(token);
                    agentsAttrMap = getAgentAttrs(orgConfig, name, type);

                    String groupName = getGroupName(orgConfig, name);
                    if ((groupName != null) &&
                        (groupName.trim().length() > 0)) {
                        ServiceConfig agentGroupConfig =
                            getAgentGroupConfig(token);
                        Map agentGroupMap = getAgentAttrs(agentGroupConfig,
                            groupName, type);

                        if ((agentsAttrMap != null) &&
                            (agentGroupMap != null)) {
                            agentGroupMap.putAll(agentsAttrMap);
                            agentsAttrMap = agentGroupMap;
                        }
                    }
                }
                return agentsAttrMap;
            } catch (SMSException e) {
                if (debug.warningEnabled()) {
                    debug.warning("AgentsRepo.getAttributes(): Unable to "+
                        "read/get agent attributes SMSException: " +
                        e.getMessage());
                }
                Object args[] = { NAME };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_READ_ATTRIBUTES,
                    args);
            } catch (IdRepoException idpe) {
                if (debug.warningEnabled()) {
                    debug.warning("AgentsRepo.getAttributes(): Unable to "+
                        "read/get agent attributes IdRepoException: " +
                        idpe.getMessage(), idpe);
                }
                Object args[] = { NAME };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_READ_ATTRIBUTES,
                    args);
            }
        }
        Object args[] = { NAME, IdOperation.READ.getName() };
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);
    }


    private Map getAgentAttrs(ServiceConfig svcConfig, String agentName,
        IdType type)
        throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getAgentAttrs() called: svcConfig=" + svcConfig.getServiceName() +
                    "; agentName=" + agentName + "; type=" + type);
        }
        Map answer = new HashMap(2);
        try {
            // Get the agent's config and then it's attributes.
            ServiceConfig aCfg = svcConfig.getSubConfig(agentName);
            if (aCfg != null) {
                answer = aCfg.getAttributesWithoutDefaults();
                // Send the agenttype of that agent.
                Set vals = new HashSet(2);
                vals.add(aCfg.getSchemaID());
                answer.put(IdConstants.AGENT_TYPE, vals);
            } else {
                // Agent not found, throw an exception
                Object args[] = { agentName, type.getName() };
                throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                        IdRepoErrorCode.TYPE_NOT_FOUND, args));
            }
        } catch (SMSException sme) {
            debug.error("AgentsRepo.getAgentAttrs(): "
                + "Error occurred while getting " + agentName, sme);
            throw new IdRepoException(sme.getMessage());
        }
        return (answer);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getBinaryAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public Map getBinaryAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {

        Object args[] = { NAME, IdOperation.READ.getName() };
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#setBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Map, boolean)
     */
    public void setBinaryAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) throws IdRepoException,
            SSOException {

        Object args[] = { NAME, IdOperation.EDIT.getName() };
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getMembers(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String,
     *      com.sun.identity.idm.IdType)
     */
    public Set getMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {

        /*
         * name would be the name of the agentgroup.
         * membersType would be the IdType of the agent to be retrieved.
         * type would be the IdType of the agentgroup.
         */
        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getMembers called" + type + ": " + name
                    + ": " + membersType);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.getMembers: "
                + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        Set results = new HashSet();
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            debug.error("AgentsRepo.getMembers: Membership operation is "
                    + "not supported for Users or Agents");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.MEMBERSHIP_TO_USERS_AND_AGENTS_NOT_ALLOWED, null);
        }
        if (!membersType.equals(IdType.AGENTONLY) &&
            !membersType.equals(IdType.AGENT)) {
            debug.error("AgentsRepo.getMembers: Cannot get member from a "
                + "non-agent type "+ membersType.getName());
            Object[] args = { NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.MEMBERSHIPS_FOR_NOT_USERS_NOT_ALLOWED, args);
        }
        if (type.equals(IdType.AGENTGROUP)) {
            try {
                // Search and get the serviceconfig of the agents and get the value of the attribute 'agentgroup' and
                // if the agent belongs to the agentgroup, add the agent/member to the result set.
                ServiceConfig orgConfig = getOrgConfig(token);
                for (String agent : orgConfig.getSubConfigNames()) {
                    ServiceConfig agentConfig;
                    agentConfig = orgConfig.getSubConfig(agent);
                    if (agentConfig != null) {
                        String group = CollectionHelper.getMapAttr(agentConfig.getAttributes(), AGENT_GROUP);
                        if (name.equalsIgnoreCase(group)) {
                            results.add(agent);
                        }
                    }
                }
            } catch (SMSException sme) {
                debug.error("AgentsRepo.getMembers: Caught "
                        + "exception while getting agents"
                        + " from groups", sme);
                Object args[] = { NAME, type.getName(), name };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ERROR_SETTING_ATTRIBUTES,
                    args);
            }
        } else {
            Object args[] = { NAME, IdOperation.READ.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED, args);
        }
        return (results);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getMemberships(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String,
     *      com.sun.identity.idm.IdType)
     */
    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType) throws IdRepoException, SSOException {

        /*
         * name would be the name of the agent.
         * membersType would be the IdType of the agentgroup to be retrieved.
         * type would be the IdType of the agent.
         */
        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getMemberships called " + type + ": " +
                name + ": " + membershipType);
        }

        if (initializationException != null) {
            debug.error("AgentsRepo.getMemberships: "
                + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        // Memberships can be returned for agents.
        if (!type.equals(IdType.AGENT) && !type.equals(IdType.AGENTONLY) &&
            !type.equals(IdType.AGENTGROUP)) {
            debug.message(
                    "AgentsRepo:getMemberships supported only for agents");
            Object args[] = { NAME };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.MEMBERSHIPS_OTHER_THAN_AGENTS_NOT_ALLOWED, args));
        }

        // Set to maintain the members
        Set results = new HashSet();
        if (membershipType.equals(IdType.AGENTGROUP)) {
            try {
                // Search and get the serviceconfig of the agent and get the value of the 'agentgroup' attribute and
                // if the agent belongs to the agentgroup, add the agentgroup to the result set.
                ServiceConfig orgConfig = getOrgConfig(token);
                results = getGroupNames(orgConfig, name);
            } catch (SMSException sme) {
                debug.error("AgentsRepo.getMemberships: Caught "
                        + "exception while getting memberships"
                        + " for Agent", sme);
                Object args[] = { NAME, type.getName(), name };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ERROR_SETTING_ATTRIBUTES,
                    args);
            }
        } else {
            // throw unsupported operation exception
            Object args[] = { NAME, IdOperation.READ.getName(),
                membershipType.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED, args);
        }
        return (results);
    }

    private String getGroupName(ServiceConfig orgConfig, String agentName) throws SSOException, SMSException {
        Set<String> groups = getGroupNames(orgConfig, agentName);
        return (groups != null && !groups.isEmpty()) ? groups.iterator().next() : null;
    }

    private Set<String> getGroupNames(ServiceConfig orgConfig, String agentName) throws SSOException, SMSException {
        Set<String> results = new HashSet<String>(2);
        ServiceConfig agentConfig = orgConfig.getSubConfig(agentName);
        if (agentConfig != null) {
            String groupName = CollectionHelper.getMapAttr(agentConfig.getAttributes(), AGENT_GROUP);
            if (groupName != null && !groupName.isEmpty()) {
                results.add(groupName);
            }
        }
        return results;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    public Map getServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set attrNames) throws IdRepoException,
            SSOException {

        Object args[] = {NAME, IdOperation.READ.getName()};
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getBinaryServiceAttributes(
     * com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     * java.lang.String, java.util.Set)
     */
    public Map getBinaryServiceAttributes(SSOToken token, IdType type,
            String name, String serviceName, Set attrNames)
            throws IdRepoException, SSOException {

        Object args[] = {NAME, IdOperation.READ.getName()};
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#isExists(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isExists(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.isExists() called: " + type + ": " +
                    name);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.isExists: "
                + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        boolean exist = false;
        try {
	        Map answer = getAttributes(token, type, name);
	        if (answer != null && !answer.isEmpty()) {
	            exist = true;
	        }
        }catch (Exception e) { //AMSetupServlet.processRequest: errorMessage:Plug-in com.sun.identity.idm.plugins.internal.AgentsRepo: Unable to read attributes.
			return false;
		}
        return (exist);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#modifyMemberShip(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set,
     *      com.sun.identity.idm.IdType, int)
     */
    public void modifyMemberShip(SSOToken token, IdType type, String name, Set<String> members, IdType membersType,
            int operation) throws IdRepoException, SSOException {
        /*
         * name would be the name of the agentgroup.
         * members would include the name of the agents to be added/removed
         * to/from the group.
         * membersType would be the IdType of the agent to be added/removed.
         * type would be the IdType of the agentgroup.
         */

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo: modifyMemberShip called " + type + ": " + name + ": "
                    + members + ": " + membersType);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.modifyMemberShip: Realm " + realmName + " does not exist.");
            throw initializationException;
        }
        if (members == null || members.isEmpty()) {
            debug.error("AgentsRepo.modifyMemberShip: Members set is empty");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ILLEGAL_ARGUMENTS, null);
        }
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            debug.error("AgentsRepo.modifyMembership: Membership to users and agents is not supported");
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.MEMBERSHIP_TO_USERS_AND_AGENTS_NOT_ALLOWED, null);
        }
        if (!membersType.equals(IdType.AGENTONLY)) {
            debug.error("AgentsRepo.modifyMembership: A non-agent type cannot be made a member of any identity "
                    + membersType.getName());
            Object[] args = {NAME};
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.MEMBERSHIPS_FOR_NOT_USERS_NOT_ALLOWED, args);
        }

        if (type.equals(IdType.AGENTGROUP)) {
            try {
                // Search and get the serviceconfig of the agent and set the agentgroup attribute with the value of
                // the agentgroup name eg., 'AgentGroup1'. One agent instance should belong to at most one group.
                ServiceConfig orgConfig = getOrgConfig(token);
                ServiceConfig agentConfig;
                for (String agent : members) {
                    agentConfig = orgConfig.getSubConfig(agent);
                    if (agentConfig != null) {
                        switch (operation) {
                            case ADDMEMBER:
                                Map<String, Set<String>> agentGroup = new HashMap<String, Set<String>>(1);
                                agentGroup.put(AGENT_GROUP, asSet(name));
                                agentConfig.setAttributes(agentGroup);
                                break;
                            case REMOVEMEMBER:
                                agentConfig.removeAttribute(AGENT_GROUP);
                                break;
                        }
                    }
                }
            } catch (SMSException sme) {
                debug.error("AgentsRepo.modifyMembership: Caught exception while adding/removing agents to groups",
                        sme);
                Object args[] = {NAME, type.getName(), name};
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ERROR_SETTING_ATTRIBUTES, args);
            }
        } else {
            // throw an exception
            debug.error("AgentsRepo.modifyMembership: Memberships cannot be modified for type= " + type.getName());
            Object[] args = {NAME, type.getName()};
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.MEMBERSHIP_CANNOT_BE_MODIFIED, args);
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#removeAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.removeAttributes() called: " + type +
                    ": " + name);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.removeAttributes: "
                    + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        if (attrNames == null || attrNames.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("AgentsRepo.removeAttributes(): Attributes " +
                        "are empty");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ILLEGAL_ARGUMENTS, null);
        } else {
            if (debug.messageEnabled()) {
                debug.message("AgentsRepo.removeAttributes(): Attribute " +
                    " names" + attrNames);
            }
        }

        ServiceConfig aCfg = null;
        try {
            if (type.equals(IdType.AGENTONLY)) {
                ServiceConfig orgConfig = getOrgConfig(token);
                aCfg = orgConfig.getSubConfig(name);
                Iterator it = attrNames.iterator();
                while (it.hasNext()) {
                    String attrName = (String) it.next();
                    if (aCfg != null) {
                        aCfg.removeAttribute(attrName);
                    } else {
                        // Agent not found, throw an exception
                        Object args[] = { name, type.getName() };
                        throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                                IdRepoErrorCode.TYPE_NOT_FOUND, args));
                    }
                }
            }
        } catch (SMSException smse) {
            debug.error("AgentsRepo.removeAttributes(): Unable to remove "
                + "agent attributes ",smse);
            Object args[] = { NAME, type.getName(), name };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ERROR_SETTING_ATTRIBUTES, args);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#removeListener()
     */
    public void removeListener() {
        if (scm != null) {
            scm.removeListener(scmListenerId);
        }
        if (ssm != null) {
            ssm.removeListener(ssmListenerId);
        }
    }

    @Override
    public RepoSearchResults search(SSOToken token, IdType type, CrestQuery crestQuery, int maxTime,
                                    int maxResults, Set<String> returnAttrs, boolean returnAllAttrs, int filterOp,
                                    Map<String, Set<String>> avPairs, boolean recursive)
            throws IdRepoException, SSOException {

        if (crestQuery.hasQueryFilter()) {
            throw new IdRepoException("AgentsRepo does not support search by query filter");
        }

        String pattern = crestQuery.getQueryId();
        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.search() called: " + type + ": " +
                pattern);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.search: "
                + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        Set agentRes = new HashSet(2);
        Map agentAttrs = new HashMap();
        int errorCode = RepoSearchResults.SUCCESS;
        try {
            if (type.equals(IdType.AGENTONLY) || type.equals(IdType.AGENT)) {
                // Get the config from 'default' group.
                ServiceConfig orgConfig = getOrgConfig(token);
                agentRes = getAgentPattern(token, type, orgConfig, pattern,
                    avPairs);
            } else if (type.equals(IdType.AGENTGROUP)) {
                // Get the config from specified group.
                ServiceConfig agentGroupConfig = getAgentGroupConfig(token);
                agentRes = getAgentPattern(token, type, agentGroupConfig,
                    pattern, avPairs);
            }
            if (agentRes != null && (!agentRes.isEmpty())) {
                Iterator it = agentRes.iterator();
                while (it.hasNext()) {
                    String agName = (String) it.next();
                    Map attrsMap = getAttributes(token, type, agName);
                    if (attrsMap != null && !attrsMap.isEmpty()) {
                        agentAttrs.put(agName, attrsMap);
                    } else {
                        return new RepoSearchResults(new HashSet(),
                            RepoSearchResults.SUCCESS, Collections.EMPTY_MAP,
                                type);
                    }
                }
            }
        } catch (SSOException sse) {
            debug.error("AgentsRepo.search(): Unable to retrieve entries: ",
                    sse);
            Object args[] = { NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.SEARCH_FAILED, args);
        }
        return new RepoSearchResults(agentRes, errorCode, agentAttrs, type);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map,
     *      boolean)
     */
    public void setAttributes(SSOToken token, IdType type, String name,
        Map attributes, boolean isAdd)
        throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.setAttributes() called: " + type + ": "
                    + name);
        }
        if (initializationException != null) {
            debug.error("AgentsRepo.setAttributes: "
                + "Realm " + realmName + " does not exist.");
            throw (initializationException);
        }
        if (attributes == null || attributes.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("AgentsRepo.setAttributes(): Attributes " +
                        "are empty");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ILLEGAL_ARGUMENTS, null);
        }

        ServiceConfig aCfg = null;
        try {
            if (type.equals(IdType.AGENTONLY) || type.equals(IdType.AGENT)) {
                ServiceConfig orgConfig = getOrgConfig(token);
                aCfg = orgConfig.getSubConfig(name);
            } else if (type.equals(IdType.AGENTGROUP)) {
                ServiceConfig agentGroupConfig = getAgentGroupConfig(token);
                if (agentGroupConfig == null) {
                	Object args[] = { NAME, IdOperation.READ.getName() };
                    throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                            IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED, args);
                }
                aCfg = agentGroupConfig.getSubConfig(name);
            } else {
                Object args[] = { NAME, IdOperation.READ.getName() };
                throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                        IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED, args);
            }

            Set vals = (Set) attributes.get("userpassword");
            if (vals != null && !AgentConfiguration.AGENT_TYPE_OAUTH2.equals(aCfg.getSchemaID())) {
                Set hashedVals = new HashSet();
                Iterator it = vals.iterator();
                while (it.hasNext()) {
                    String val = (String) it.next();
                    if (!val.startsWith(hashAlgStr)) {
                        hashedVals.add(hashAlgStr + Hash.hash(val));
                        attributes.remove("userpassword");
                        attributes.put("userpassword", hashedVals);
                    }
                }
            }

            if (aCfg != null) {
                aCfg.setAttributes(attributes);
            } else {
                // Agent not found, throw an exception
                Object args[] = { name, type.getName() };
                throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                        IdRepoErrorCode.TYPE_NOT_FOUND, args));
            }
        } catch (SMSException smse) {
            debug.error("AgentsRepo.setAttributes(): Unable to set agent"
                + " attributes ",smse);
            Object args[] = { NAME, type.getName(), name };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.ERROR_SETTING_ATTRIBUTES, args);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getSupportedOperations(
     *      com.sun.identity.idm.IdType)
     */
    public Set getSupportedOperations(IdType type) {
        return (Set) supportedOps.get(type);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getSupportedTypes()
     */
    public Set getSupportedTypes() {
        return supportedOps.keySet();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#initialize(java.util.Map)
     */
    public void initialize(Map configParams) throws IdRepoException {
        super.initialize(configParams);
        // Initialize with the realm name
        Set realms = (Set) configParams.get("agentsRepoRealmName");
        if ((realms != null) && !realms.isEmpty()) {
            realmName = DNMapper.orgNameToDN((String) realms.iterator().next());
            // Initalize ServiceConfig with realm names
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            if (getOrgConfig(adminToken) == null) {
                debug.error("AgentsRepo.getAgentGroupConfig: "
                    + "Realm " + realmName + " does not exist.");
                String slashRealmName;
                slashRealmName = DNMapper.orgNameToRealmName(realmName);
                Object[] args = { slashRealmName };
                initializationException =
                    new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.REALM_DOESNT_EXIST, args);
            }
            getAgentGroupConfig(adminToken);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#isActive(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isActive(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {

        Map attributes = getAttributes(token, type, name);
        if (attributes == null) {
            Object[] args = { NAME, name };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.NOT_VALID_ENTRY, args);
        }
        Set activeVals = (Set) attributes.get(statusAttribute);
        if (activeVals == null || activeVals.isEmpty()) {
            return true;
        } else {
            Iterator it = activeVals.iterator();
            String active = (String) it.next();
            return (active.equalsIgnoreCase(statusActive) ? true : false);
        }

    }

    /* (non-Javadoc)
     * @see com.sun.identity.idm.IdRepo#setActiveStatus(
        com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
        java.lang.String, boolean)
     */
    public void setActiveStatus(SSOToken token, IdType type,
        String name, boolean active)
        throws IdRepoException, SSOException {

        Map attrs = new HashMap();
        Set vals = new HashSet(2);
        if (active) {
            vals.add(statusActive);
        } else {
            vals.add(statusInactive);
        }
        attrs.put(statusAttribute, vals);
        setAttributes(token, type, name, attrs, false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#shutdown()
     */
    public void shutdown() {
        if (scm != null) {
            scm.removeListener(scmListenerId);
        }
        if (ssm != null) {
            ssm.removeListener(ssmListenerId);
        }
    }

    private void loadSupportedOps() {
        Set opSet = new HashSet(2);
        opSet.add(IdOperation.EDIT);
        opSet.add(IdOperation.READ);
        opSet.add(IdOperation.CREATE);
        opSet.add(IdOperation.DELETE);

        supportedOps.put(IdType.AGENTONLY, Collections.unmodifiableSet(
            opSet));
        supportedOps.put(IdType.AGENTGROUP, Collections.unmodifiableSet(
            opSet));
        supportedOps.put(IdType.AGENT, Collections.unmodifiableSet(opSet));

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.loadSupportedOps() called: "
                    + "supportedOps Map = " + supportedOps);
        }
    }

    // The following three methods implement ServiceListener interface
    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.sm.ServiceListener#globalConfigChanged(
     *      java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, int)
     */
    public void globalConfigChanged(String serviceName, String version,
        String groupName, String serviceComponent, int type) {
        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.globalConfigChanged..");
        }
        IdType idType;
        if (groupName.equalsIgnoreCase("default")) {
            idType = IdType.AGENTONLY;
        } else {
            idType = IdType.AGENTGROUP;
        }
        String name =
            serviceComponent.substring(serviceComponent.indexOf('/') + 1);

        if (name.isEmpty()) {
            return;
        }

        // If notification URLs are present, send notifications
        sendNotificationSet(type, idType, name);

        if (repoListener != null) {
            repoListener.objectChanged(name, idType, type,
                repoListener.getConfigMap());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.sm.ServiceListener#organizationConfigChanged(
     *      java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, int)
     */
    public void organizationConfigChanged(String serviceName, String version,
        String orgName, String groupName, String serviceComponent, int type)

    {
        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.organizationConfigChanged..");
        }

        // Process notification only if realm name matches and seviceComp
        // is not "" (for org creation) and "/" for "ou=default" creation
        if (orgName.equalsIgnoreCase(realmName) &&
            !serviceComponent.equals("/") && !serviceComponent.equals("")) {
            // Get the Agent name
            String name = serviceComponent.substring(
                serviceComponent.indexOf('/') + 1);

            if (name.isEmpty()) {
                return;
            }

            // Send local notification first
            if (repoListener != null) {
                if (groupName.equalsIgnoreCase("default")) {
                    repoListener.objectChanged(name, IdType.AGENT, type,
                        repoListener.getConfigMap());
                    repoListener.objectChanged(name, IdType.AGENTONLY, type,
                        repoListener.getConfigMap());
                } else {
                    repoListener.objectChanged(name, IdType.AGENTGROUP, type,
                        repoListener.getConfigMap());
                }
            }

            // If notification URLs are present, send notification
            if (groupName.equalsIgnoreCase("default")) {
                sendNotificationSet(type, IdType.AGENTONLY, name);
            } else {
                sendNotificationSet(type, IdType.AGENTGROUP, name);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.sm.ServiceListener#schemaChanged(java.lang.String,
     *      java.lang.String)
     */
    public void schemaChanged(String serviceName, String version) {
        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.schemaChanged..");
        }
        if (repoListener != null) {
            repoListener.allObjectsChanged();
        }
    }

    public String getFullyQualifiedName(SSOToken token, IdType type,
            String name) throws IdRepoException, SSOException {

        CrestQuery crestQuery = new CrestQuery(name);
        RepoSearchResults results = search(token, type, crestQuery, 0, 0, null, true, 0, null, false);
        Set dns = results.getSearchResults();
        if (dns.size() != 1) {
            String[] args = { name };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_FIND_ENTRY, args));
        }
        return ("sms://AgentsRepo/" + dns.iterator().next().toString());
    }

    public boolean supportsAuthentication() {
        return (true);
    }

    public boolean authenticate(Callback[] credentials)
        throws IdRepoException, AuthLoginException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.authenticate() called");
        }
        // Obtain user name and password from credentials and compare
        // with the ones from the agent profile to authorize the agent.
        String username = null;
        String unhashedPassword = null;
        String password = null;
        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof NameCallback) {
                username = ((NameCallback) credentials[i]).getName();
                if (debug.messageEnabled()) {
                    debug.message("AgentsRepo.authenticate() username: "
                            + username);
                }
            } else if (credentials[i] instanceof PasswordCallback) {
                char[] passwd = ((PasswordCallback) credentials[i])
                        .getPassword();
                if (passwd != null) {
                    unhashedPassword = new String(passwd);
                    password = hashAlgStr + Hash.hash(unhashedPassword);
                    if (debug.messageEnabled()) {
                        debug.message("AgentsRepo.authenticate() passwd "
                            + "present");
                    }
                }
            }
        }
        if (username == null || (username.length() == 0) ||
            password == null || unhashedPassword == null) {
            Object args[] = { NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_TO_AUTHENTICATE, args);
        }
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());

        boolean answer = false;
        String userid = username;
        try {
            /* Only agents with IdType.AGENTONLY is used for authentication,
             * not the agents with IdType.AGENTGROUP.
             * AGENTGROUP is for storing common properties.
             */
            if (LDAPUtils.isDN(username)) {
                userid = LDAPUtils.rdnValueFromDn(username);
            }
            Set pSet = new HashSet(2);
            pSet.add("userpassword");
            pSet.add(oauth2Attribute);
            Map ansMap = new HashMap();
            String userPwd = null;
            ansMap = getAttributes(adminToken, IdType.AGENT,
                userid, pSet);
            Set userPwdSet = (Set) ansMap.get("userpassword");
            if ((userPwdSet != null) && (!userPwdSet.isEmpty())) {
                userPwd = (String) userPwdSet.iterator().next();
                if (!(answer = password.equals(userPwd)) && !(answer = oauth2PasswordMatch(ansMap, unhashedPassword, userPwd))) {
                    throw (new InvalidPasswordException("invalid password",
                        userid));
                }
            }
            if (debug.messageEnabled()) {
                debug.message("AgentsRepo.authenticate() result: " + answer);
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AgentsRepo.authenticate(): "
                        + "Unable to authenticate SSOException: " +
                        ssoe.getMessage());
            }
        }
        return (answer);
    }

    private boolean oauth2PasswordMatch(Map attributeMap, String unhashedPassword, String userPassword) {
        return attributeMap.keySet().contains(oauth2Attribute) && unhashedPassword.equals(userPassword);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#modifyService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      com.sun.identity.sm.SchemaType, java.util.Map)
     */
    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {

        Object args[] = { NAME, IdOperation.SERVICE.getName() };
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#unassignService(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap) throws IdRepoException,
            SSOException {

        Object args[] = {
                "com.sun.identity.idm.plugins.specialusers.SpecialRepo",
                IdOperation.SERVICE.getName() };
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#assignService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      com.sun.identity.sm.SchemaType, java.util.Map)
     */
    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap)
            throws IdRepoException, SSOException {

        Object args[] = { NAME, IdOperation.SERVICE.getName() };
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getAssignedServices(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map)
     */
    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServicesAndOCs) throws IdRepoException, SSOException {

        Object args[] = { NAME, IdOperation.SERVICE.getName() };
        throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_OPERATION_NOT_SUPPORTED,
                args);
    }

    private ServiceConfig getOrgConfig(SSOToken token) {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getOrgConfig() called. ");
        }
        try {
            if ((orgConfigCache == null) || !orgConfigCache.isValid()) {
                if (scm == null) {
                    scm = new ServiceConfigManager(token, agentserviceName,
                        version);
                }
                orgConfigCache = scm.getOrganizationConfig(realmName, null);
            }
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AgentsRepo.getOrgConfig(): "
                    + "Unable to get Organization Config due to " +
                    smse.getMessage());
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AgentsRepo.getOrgConfig(): "
                    + "Unable to get Organization Config due to " +
                    ssoe.getMessage());
            }
        }
        return (orgConfigCache);
    }

    private ServiceConfig getAgentGroupConfig(SSOToken token) {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getAgentGroupConfig(): called. ");
        }
        try {
            // Always get from ServiceConfigManager which checks the cache
            // and returns latest values stored in cache.
            agentGroupConfigCache =
                scm.getOrganizationConfig(realmName, AGENT_GROUP);

        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AgentsRepo.getAgentGroupConfig: "
                    + "Unable to get Agent Group Config due to " +
                    smse.getMessage());
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AgentsRepo.getAgentGroupConfig: "
                    + "Unable to get Agent Group Config due to " +
                    ssoe.getMessage());
            }
        }
        return (agentGroupConfigCache);
    }

    public ServiceConfig createAgentGroupConfig(SSOToken token) {
    	if (debug.messageEnabled()) {
            debug.message("createAgentGroupConfig(): called. ");
        }
        try {
            if (scm == null) {
                scm = new ServiceConfigManager(token, agentserviceName,
                    version);
            }
            String agentGroupDN = constructDN(AGENT_GROUP,
                instancesNode, realmName, version, agentserviceName);
            ServiceConfig orgConfig = getOrgConfig(token);
            if (orgConfig != null) {
                orgConfig.checkAndCreateGroup(agentGroupDN,
                    AGENT_GROUP);
            }
            // Always get from ServiceConfigManager which checks the cache
            // and returns latest values stored in cache.
            agentGroupConfigCache =
                scm.getOrganizationConfig(realmName, AGENT_GROUP);

        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("createAgentGroupConfig: "
                    + "Unable to create Agent Group Config due to " +
                    smse.getMessage());
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("createAgentGroupConfig: "
                    + "Unable to create Agent Group Config due to " +
                    ssoe.getMessage());
            }
        }
        return (agentGroupConfigCache);
    }

    private boolean isAgentTypeSearch(ServiceConfig aConfig, String pattern)
        throws IdRepoException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.isAgentTypeSearch() called: " + pattern);
        }
        String agentType = null;
        boolean agentTypeflg = false;

        try {
            // Get the agentType and then compare the pattern sent for Search.
            for (Iterator items = aConfig.getSubConfigNames()
                .iterator(); items.hasNext();) {
                agentType = (String) items.next();
                if (agentType.equalsIgnoreCase(pattern)) {
                    agentTypeflg = true;
                    break;
                }
            }
        } catch (SMSException sme) {
            debug.error("AgentsRepo.isAgentTypeSearch(): Error occurred while "
                + "checking AgentType sent for pattern "+ pattern, sme);
            throw new IdRepoException(sme.getMessage());
        }
        return (agentTypeflg);
    }

    private Set getAgentPattern(SSOToken token, IdType type,
        ServiceConfig aConfig, String pattern, Map avPairs)
        throws IdRepoException {

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getAgentPattern() called: pattern : " +
                pattern + "\navPairs : " + avPairs);
        }

        if (aConfig == null) {
            return (Collections.EMPTY_SET);
        }
        Set agentRes;
        // Get AgentType
        String agentType = null;
        if (avPairs != null && !avPairs.isEmpty()) {
            Set set = (Set) avPairs.get(IdConstants.AGENT_TYPE);
            if (set != null && !set.isEmpty()) {
                agentType = set.iterator().next().toString();
                avPairs.remove(IdConstants.AGENT_TYPE);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("AgentsRepo.getAgentPattern() agentType : " +
                agentType);
        }

        // Search for agents matching the pattern and agenttype
        try {
            if (agentType != null) {
                agentRes = aConfig.getSubConfigNames(pattern, agentType);
            } else {
                agentRes = aConfig.getSubConfigNames(pattern);
            }

            if (debug.messageEnabled()) {
                debug.message("AgentsRepo.getAgentPattern() agentRes : " +
                    agentRes);
            }

            // Check if there are agents and if more attributes are present
            if (agentRes == null || agentRes.isEmpty() ||
                avPairs == null || avPairs.isEmpty()) {
                return (agentRes == null ? Collections.EMPTY_SET : agentRes);
            }

            /* if there are agents matching the pattern and agenttype and
             * if avPairs is not empty, search for other attributes in the
             * avPairs and add that Agent if search results are positive.
             * ie., if avPairs matches with the attributes in store.
            */
            Set agents = new HashSet(2);
            for (Iterator itr = agentRes.iterator(); itr.hasNext();) {
                String name = (String) itr.next();
                Map attrMap = getAttributes(token, type, name);
                if (attrMap == null || attrMap.isEmpty()) {
                    continue;
                }
                for (Iterator it = avPairs.keySet().iterator();
                    it.hasNext();) {
                    String attr = (String) it.next();

                     /* 'attrValues' are values from avPairs sent by client.
                      * 'presentValues' are from Directory Server.
                      * The element in attrValues is compared with the
                      * values from DS, and then the agent name is added to
                      * resultant set to be returned if matches.
                     */

                    Set attrValues = (Set) avPairs.get(attr);
                    Set presentSet = (Set) attrMap.get(attr);
                    if (presentSet != null && !presentSet.isEmpty()) {
                        Set presentValues = new CaseInsensitiveHashSet(
                            presentSet);
                        for (Iterator i = attrValues.iterator();i.hasNext();) {
                            String avName = (String) i.next();
                            if ((presentValues != null) &&
                                (presentValues.contains(avName))) {
                                agents.add(name);
                                break;
                            }
                        }
                    }
                }
            }
            return (agents);
        } catch (SSOException sse) {
            debug.error("AgentsRepo.getAgentPattern(): Error occurred while "
                + "checking AgentName sent for pattern "+ pattern, sse);
            throw new IdRepoException(sse.getMessage());
        } catch (SMSException sme) {
            debug.error("AgentsRepo.getAgentPattern(): Error occurred while "
                + "checking AgentName sent for pattern "+ pattern, sme);
            throw new IdRepoException(sme.getMessage());
        }
    }

    String constructDN(String groupName, String configName, String orgName,
        String version, String serviceName) throws SMSException {

        StringBuilder sb = new StringBuilder(50);
        sb.append("ou=").append(groupName).append(comma).append(
                configName).append("ou=").append(version)
                .append(comma).append("ou=").append(serviceName)
                .append(comma).append("ou=services").append(comma);
        orgName = DNMapper.orgNameToDN(orgName);
        sb.append(orgName);
        return (sb.toString());
    }

    // If notification URLs are present, send notifications to clients/agents.
    private void sendNotificationSet(int type, IdType agentIdTypeforNotificationSet,
            String agentNameforNotificationSet) {

        try {
            // If notification enabled is set to true ,send notifications.
            Set<String> nSet = new HashSet<String>(2);
            nSet.add(notificationURLenabled);
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            Map<String, Set<String>> ansMap = getAttributes(adminToken, agentIdTypeforNotificationSet,
                    agentNameforNotificationSet, nSet);
            Set<String> neSet = ansMap.get(notificationURLenabled);

            if (neSet != null && !neSet.isEmpty() && neSet.iterator().next().equalsIgnoreCase("true")) {
                switch (type) {
                case MODIFIED:
                    if (agentIdTypeforNotificationSet == null) {
                        break;
                    }

                    String modItem;
                    Set<String> aNameSet = new HashSet<String>(2);

                    if (debug.messageEnabled()) {
                        debug.message("AgentsRepo.sendNotificationSet(): agentIdTypeforNotificationSet "
                                + agentIdTypeforNotificationSet);
                        debug.message("AgentsRepo.sendNotificationSet(): agentNameforNotificationSet "
                                + agentNameforNotificationSet);
                    }

                    // This checks if the changes happened to an agentgroup.
                    // If so,it gets all its members/agents and sends
                    // notifications to all its members.
                    if (agentIdTypeforNotificationSet.equals(IdType.AGENTGROUP)) {
                        Set<String> members = getMembers(adminToken, agentIdTypeforNotificationSet,
                                agentNameforNotificationSet, IdType.AGENTONLY);
                        for (String agent : members) {
                            aNameSet.add(agent);
                            //An agent group has been updated, so now we need to notify the internal cache for the
                            //group members so they return the changed inherited values as well.
                            repoListener.objectChanged(agent, IdType.AGENT, type, repoListener.getConfigMap());
                            repoListener.objectChanged(agent, IdType.AGENTONLY, type, repoListener.getConfigMap());
                        }
                   } else {
                       aNameSet.add(agentNameforNotificationSet);
                   }

                   if (debug.messageEnabled()) {
                       debug.message("AgentsRepo.sendNotificationSet(): aNameSet " + aNameSet);
                   }

                   if (!aNameSet.isEmpty()) {
                       for (String agentName : aNameSet) {
                           if (notificationConfig.isAgentsEnabled()) {
                               broker.publish(Topic.of("/agent/config"), json(object(
                                       field("realm", DNMapper.orgNameToRealmName(realmName)),
                                       field("agentName", agentName)
                               )));
                           }

                           agentIdTypeforNotificationSet = IdType.AGENTONLY;

                           // To be consistent and for easy web agent
                           // parsing,the notification set should start with
                           // "AgentConfigChangeNotification"
                           StringBuilder xmlsb = new StringBuilder(1000);
                           xmlsb.append("<")
                                .append(AGENT_NOTIFICATION)
                                .append(" ")
                                .append(AGENT_ID)
                                .append("=\"")
                                .append(agentName)
                                .append("\"")
                                .append(" ")
                                .append(AGENT_IDTYPE)
                                .append("=\"")
                                .append(agentIdTypeforNotificationSet.getName())
                                .append("\"/>");

                           modItem = xmlsb.toString();

                           if (debug.messageEnabled()) {
                               debug.message("AgentsRepo.sendNotificationSet(): modItem " + modItem);
                           }

                           // If notification URLs are present,send
                           // notifications
                           nSet = new HashSet<String>(2);
                           nSet.add(notificationURLname);
                           String nval;
                           ansMap = getAttributes(adminToken, agentIdTypeforNotificationSet, agentName, nSet);
                           Set<String> nvalSet = ansMap.get(notificationURLname);
                           if (nvalSet != null && !nvalSet.isEmpty()) {
                               nval = nvalSet.iterator().next();
                               try {
                                   URL url = new URL(nval);
                                   // Construct NotificationSet to be sent to
                                   // Agents.
                                   Notification notification = new Notification(modItem);
                                   NotificationSet ns = new NotificationSet(AGENT_CONFIG_SERVICE);
                                   ns.addNotification(notification);
                                   try {
                                       PLLServer.send(url, ns);
                                       if (debug.messageEnabled()) {
                                           debug.message("AgentsRepo:sendNotificationSet Sent Notification to URL: "
                                                   + url + " Data: " + ns);
                                       }
                                   } catch (SendNotificationException ne) {
                                       if (debug.warningEnabled()) {
                                           debug.warning("AgentsRepo.sendNotificationSet: failed sending notification"
                                                   + " to: " + url + " " + ne.getMessage());
                                       }
                                   }
                               } catch (MalformedURLException e) {
                                   if (debug.warningEnabled()) {
                                       debug.warning("AgentsRepo.sendNotificationSet:(): invalid URL: "
                                               + e.getMessage());
                                   }
                               }
                            }
                        }
                    }
                }
            }
        } catch (IdRepoException idpe) {
            debug.error("AgentsRepo.sendNotificationSet(): Unable to send notification due to " + idpe);
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AgentsRepo.sendNotificationSet(): Unable to send notification due to "
                        + ssoe.getMessage());
            }
        }
    }

    private void removeIdentityFromAgentAuthenticators(String name) {
       SSOToken superAdminToken = (SSOToken) AccessController.doPrivileged(
           AdminTokenAction.getInstance());
       try {
           Map map = new HashMap(2);
           Set set = new HashSet(2);
           set.add("SharedAgent");
           map.put("AgentType", set);
           CrestQuery crestQuery = new CrestQuery("*");
           RepoSearchResults results = search(
               superAdminToken, IdType.AGENTONLY, crestQuery, 0, 0, null, true, 0, null, false);
           Set res = results.getSearchResults();

           if ((res != null) && !res.isEmpty()) {
               for (Iterator i = res.iterator(); i.hasNext();) {
                   String agentName = (String) i.next();
                   Map attrValues = getAttributes(superAdminToken,
                       IdType.AGENTONLY, agentName);
                   Set agentToReads = (Set) attrValues.get(
                       "AgentsAllowedToRead");

                   if ((agentToReads != null) && !agentToReads.isEmpty()) {
                       if (agentToReads.remove(name)) {
                           Map attrMap = new HashMap(2);
                           attrMap.put("AgentsAllowedToRead", agentToReads);
                           setAttributes(superAdminToken, IdType.AGENTONLY,
                               agentName, attrMap, false);
                       }
                   }
               }
           }
       } catch (IdRepoException e) {
           debug.warning(
               "AgentRepo.removeIdentityFromAgentAuthenticators", e);
       } catch (SSOException e) {
           debug.warning(
               "AgentRepo.removeIdentityFromAgentAuthenticators", e);
       }
    }
}
