/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EntityObjectIF.java,v 1.3 2008/06/25 05:43:26 qcheng Exp $
 *
 */

package com.sun.identity.entity;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * JAX-RPC interface for Entity Object and Services to make it remotable.
 */
public interface EntityObjectIF extends Remote {

    /**
     * Creates entities.
     * 
     * @param ssoToken
     *            String representing user's SSO Token.
     * @param entityName
     *            Name of the entity.eg.cn=websphereAgent
     * @param entityType
     *            Type of entity being created. eg. Agent The types supported by
     *            SDK are configured in the list of Managed Objects in the
     *            <code>DAI</code> service.
     * @param entityLocation
     *            Location of the entity creation.eg.www.abc.com
     * @param attributes
     *            Map to represent Attribute-Value Pairs
     * @return Returns a set of Entity DNs created.
     * @throws EntityException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createEntity(String ssoToken, String entityName,
            String entityType, String entityLocation, Map attributes)
            throws EntityException, SSOException, RemoteException;

    /**
     * Sets or replaces attribute values with the new values supplied and Stores
     * the changes to directory server.
     * 
     * @param ssoToken User's Single Sign On Token.
     * @param entityName Name of the entity. example
     *        <code>cn=websphereAgent</code>
     * @param entityType Type of entity being created. eg. Agent The types
     *        supported by SDK are configured in the list of Managed Objects
     *        in the <code>DAI</code> service.
     * @param entityLocation Location of the entity creation. example
     *        <code>www.abc.com</code>
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     */
    public void modifyEntity(String ssoToken, String entityName,
            String entityType, String entityLocation, Map attributes)
            throws EntityException, SSOException, RemoteException;

    /**
     * Deletes entities.
     * 
     * @param ssoToken
     *            String representing user's SSO Token.
     * @param entityName
     *            Name of the entity.eg.cn=websphereAgent
     * @param entityType
     *            Type of entity being created. eg. Agent The types supported by
     *            SDK are configured in the list of Managed Objects in the
     *            <code>DAI</code> service.
     * @param entityLocation
     *            Location of the entity creation.eg.www.abc.com
     * @throws EntityException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteEntity(String ssoToken, String entityName,
            String entityType, String entityLocation) throws EntityException,
            SSOException, RemoteException;

    /**
     * Returns the entity object for each entity given the entityType and
     * entityName(s) in that particular organization.
     * 
     * @param ssoToken
     *            String representing user's SSO Token.
     * @param entityName
     *            Name of the entity.eg.cn=websphereAgent
     * @param entityType
     *            Type of entity being created. eg. Agent The types supported by
     *            SDK are configured in the list of Managed Objects in the
     *            <code>DAI</code> service.
     * @param entityLocation
     *            Location of the entity creation.eg.www.abc.com
     * @return Returns a set of Entity objects.
     * @throws EntityException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Map getEntity(String ssoToken, String entityName, String entityType,
            String entityLocation) throws EntityException, SSOException,
            RemoteException;

    /**
     * Returns a set of Entity Names given the Entity Type for that particular
     * organization.
     * 
     * @param ssoToken
     *            String representing user's SSO Token.
     * @param entityType
     *            Type of entity being created. eg. Agent The types supported by
     *            SDK are configured in the list of Managed Objects in the
     *            <code>DAI</code> service.
     * @param entityLocation
     *            Location of the entity creation.eg.www.abc.com
     * @param entityFilter
     * @return Set of Entity Names.
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     */
    public Set getEntityNames(String ssoToken, String entityType,
            String entityLocation, String entityFilter) throws EntityException,
            SSOException, RemoteException;

}
