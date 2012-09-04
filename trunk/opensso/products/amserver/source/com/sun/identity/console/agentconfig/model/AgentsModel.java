/**
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
 * $Id: AgentsModel.java,v 1.12 2008/12/13 07:16:09 veiming Exp $
 *
 */

package com.sun.identity.console.agentconfig.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.idm.AMIdentity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent Configuration Model.
 */
public interface AgentsModel
    extends AMModel 
{
    /**
     * Returns agent names.
     *
     * @param realmName Realm where agents reside.
     * @param setTypes Agent Types.
     * @param pattern Search Pattern.
     * @param results Set to contains the results.
     * @return error code.
     * @throws AMConsoleException if result cannot be returned.
     */
    int getAgentNames(
        String realmName,
        Set setTypes,
        String pattern,
        Set results
    ) throws AMConsoleException;

    /**
     * Returns agent group names.
     *
     * @param realmName Realm where agent groups reside.
     * @param setTypes Agent Types.
     * @param pattern Search Pattern.
     * @param results Set to contains the results.
     * @return error code.
     * @throws AMConsoleException if result cannot be returned.
     */
    int getAgentGroupNames(
        String realmName,
        Set setTypes,
        String pattern,
        Set results
    ) throws AMConsoleException;

    /**
     * Creates localized agent.
     *
     * @param realmName Realm where agent resides.
     * @param name Name of agent.
     * @param type Type of agent.
     * @param password Password of agent.
     * @param agentURL Agent URL.
     * @throws AMConsoleException if agent cannot be created.
     */
    void createAgentLocal(
        String realmName,
        String name,
        String type,
        String password,
        String agentURL
    ) throws AMConsoleException;
    
    /**
     * Creates agent.
     *
     * @param realmName Realm where agent resides.
     * @param name Name of agent.
     * @param type Type of agent.
     * @param password Password of agent.
     * @param choice Choice of type of configuartion.
     * @throws AMConsoleException if agent cannot be created.
     */
    void createAgent(
        String realmName,
        String name,
        String type,
        String password,
        String choice
    ) throws AMConsoleException;

    /**
     * Creates agent.
     *
     * @param realmName Realm where agent resides.
     * @param name Name of agent.
     * @param type Type of agent.
     * @param password Password of agent.
     * @param serverURL Server URL.
     * @param agentURL Agent URL.
     * @throws AMConsoleException if agent cannot be created.
     */
    void createAgent(
        String realmName,
        String name,
        String type,
        String password,
        String serverURL,
        String agentURL
    ) throws AMConsoleException;
    
    /**
     * Creates agent group.
     *
     * @param realmName Realm where agent group resides.
     * @param name Name of agent group.
     * @param type Type of agent group.
     * @throws AMConsoleException if agent group cannot be created.
     */
    void createAgentGroup(
        String realmName,
        String name, 
        String type
    ) throws AMConsoleException;
    
    /**
     * Creates agent group.
     *
     * @param realmName Realm where agent group resides.
     * @param name Name of agent group.
     * @param type Type of agent group.
     * @param serverURL Server URL.
     * @param agentURL Agent URL.
     * @throws AMConsoleException if agent group cannot be created.
     */
    void createAgentGroup(
        String realmName,
        String name,
        String type,
        String serverURL,
        String agentURL) throws AMConsoleException;
    
    /**
     * Deletes agents.
     *
     * @param realmName Realm where agent resides.
     * @param agents Set of agent names to be deleted.
     * @throws AMConsoleException if agents cannot be deleted.
     */
    void deleteAgents(String realmName, Set agents) throws AMConsoleException;
    
    /**
     * Deletes agent groups.
     *
     * @param realmName Realm where agent group resides.
     * @param agentGroups Set of agent group names to be deleted.
     * @throws AMConsoleException if agents cannot be deleted.
     */
    void deleteAgentGroups(String realmName, Set agentGroups) 
        throws AMConsoleException;
    
    
    /**
     * Returns the group of which agent belongs to.
     *
     * @param realmName Realm where agent group resides.
     * @param universalId Universal ID of the agent.
     * @return the group of which agent belongs to.
     * @throws AMConsoleException if object cannot located.
     */
    String getAgentGroup(String realmName, String universalId)
        throws AMConsoleException;
    
    /**
     * Returns the group of which agent belongs to.
     *
     * @param realmName Realm where agent group resides.
     * @param universalId Universal ID of the agent.
     * @return the group of which agent belongs to.
     * @throws AMConsoleException if object cannot located.
     */
    String getAgentGroupId(String realmName, String universalId)
        throws AMConsoleException;
    
    /**
     * Returns attribute values of an agent or agent group.
     *
     * @param realmName Realm where agent or agent group resides.
     * @param universalId Universal ID of the agent/agent group.
     * @param withInheritValues <code>true</code> to include inherited values.
     * @return attribute values of an agent or agent group.
     * @throws AMConsoleException if object cannot located.
     */
    Map getAttributeValues(
        String realmName,
        String universalId, 
        boolean withInheritValues
        ) throws AMConsoleException;
    
    /**
     * Returns attribute values of an agent group.
     *
     * @param realmName Realm where agent group resides.
     * @param groupName agent group.
     * @return attribute values of an agent group.
     * @throws AMConsoleException if object cannot located.
     */
    public Map getGroupAttributeValues(String realmName, String groupName)
        throws AMConsoleException;
        
    /**
     * Modifies agent or agent group attribute values.
     *
     * @param universalId Universal ID of the agent/agent group.
     * @param values attribute values of an agent or agent group.
     * @throws AMConsoleException if object cannot located.
     */
    void setAttributeValues(String universalId, Map values)
        throws AMConsoleException;
    
    /**
     * Modifies agent's group.
     *
     * @param realmName realm where agent resides.
     * @param universalId Universal ID of the agent.
     * @param groupName Name of group.
     * @return <code>true</code> if group is set.
     * @throws AMConsoleException if object cannot located.
     */
    boolean setGroup(String realmName, String universalId, String groupName)
        throws AMConsoleException;
    
    /**
     * Returns all the authentication chains in a realm.
     *
     * @return all the authentication chains in a realm.
     * @throws AMConsoleException if authentication chains cannot be returned.
     */
    Set getAuthenticationChains() throws AMConsoleException;
    
    /**
     * Returns a map of i18n key to supported security mechanism.
     *
     * @param agentType Type of agent.
     * @return a map of i18n key to supported security mechanism.
     */
    Map getSecurityMechanisms(String agentType);

    /**
     * Returns map of secure token service configurations.
     *
     * @return map of secure token service configurations.
     * @throws AMConsoleException if sercure token service configurations cannot 
     *         be returned.
     */
    Map getSTSConfigurations();
    
    /**
     * Returns set of discovery configurations.
     *
     * @return set of discovery configurations.
     * @throws AMConsoleException if discovery configurations cannot be returned.
     */
    Map getDiscoveryConfigurations();
    
    /**
     * Returns display name of an agent/group.
     *
     * @param universalId Universal ID of the agent/agent group. 
     * @return display name of an agent/group.
     * @throws AMConsoleException if object cannot located.
     */
    String getDisplayName(String universalId) throws AMConsoleException;
    
    /**
     * Returns <code>true</code> if the identity if an agent group.
     *
     * @param universalId Universal ID of the agent/agent group. 
     * @return <code>true</code> if the identity if an agent group.
     */
    boolean isAgentGroup(String universalId);

    /**
     * Returns a set of inherited property names.
     *
     * @param realmName Realm where agent resides
     * @param universalId Universal ID of the agent.
     * @return a set of inherited property names
     */
    Set getInheritedPropertyNames(String realmName, String universalId);
    
    /**
     * Returns attribute schemas of a given set of attributes.
     *
     * @param agentType Agent type.
     * @return attribute schemas of a given set of attributes.
     */
    Map getAttributeSchemas(String agentType, Collection attributeNames);
    
    /**
     * Updates inheritance setting.
     *
     * @param universalId Universal ID of the agent.
     * @param inherit Map of attribute name to either "1" or "0". "1" to 
     *        inherit and "0" not.
     * @throws AMConsoleException if update failed.
     */
    void updateAgentConfigInheritance(String universalId, Map inherit)
        throws AMConsoleException;
    
    /**
     * Returns attribute values of agent's group.
     * @param realm Realm where agent group resides
     * @param agentId Universal Id of the agent.
     * @param attrNames Attribute Names of interests.
     * @return attribute values of agent's group.
     * @throws AMConsoleException if unable to get the attribute values.
     */
    Map getAgentGroupValues(String realm, String agentId, Set attrNames) 
        throws AMConsoleException;
    
    /**
     * Returns a set of members of an agent group.
     *
     * @param agentId Universal Id of the agent group.
     * @throws AMConsoleException if members cannot be returned.
     */
    Set getAgentGroupMembers(String agentId)
        throws AMConsoleException;
    
    /**
     * Returns agent type.
     * @param amid Identity Object.
     * @return agent type.
     * @throws AMConsoleException if agent type cannot be obtained.
     */
    String getAgentType(AMIdentity amid) 
        throws AMConsoleException;
    
    /**
     * Returns Token Conversion Types.
     * 
     * @return Token Conversion Types.
     */
    List getTokenConversionTypes();
    
    /**
     * Returns <code>true</code> if repository is centralized.
     * 
     * @param uid Agent's universal ID.
     * @return <code>true</code> if repository is centralized.
     */
    boolean isCentralized(String uid);
}
