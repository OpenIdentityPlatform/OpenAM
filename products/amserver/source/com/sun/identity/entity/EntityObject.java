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
 * $Id: EntityObject.java,v 1.5 2008/06/25 05:43:26 qcheng Exp $
 *
 */

package com.sun.identity.entity;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import com.iplanet.dpro.session.Session;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.jaxrpc.SOAPClient;

public class EntityObject {

    // Service Name
    private static String SERVICE_NAME = "EntityObjectIF";

    private SSOToken token;

    private String tokenString;

    protected String entityLocation;

    protected SOAPClient client = null;

    public EntityObject(SSOToken token, String entityLocation)
            throws EntityException {
        this.token = token;
        tokenString = token.getTokenID().toString();
        this.entityLocation = entityLocation;
        if (client == null) {
            client = new SOAPClient(SERVICE_NAME);
        }
    }

    /**
     * Creates entity.
     * 
     * @param entityName
     *            Name of the entity.eg.cn=websphereAgent.
     * @param entityType
     *            Type of entity being created. eg. Agent The types supported by
     *            SDK are configured in the list of Managed Objects in the
     *            <code>DAI</code> service.
     * @param attributes
     *            Map where the key is the name of the entity, and the value is
     *            a Map to represent Attribute-Value Pairs
     * @return Set of <code>AMEntity</code> objects created
     * @throws EntityException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createEntity(String entityName, String entityType, 
            Map attributes) throws EntityException, SSOException {
        try {
            Object[] objs = { tokenString, entityName, entityType,
                    entityLocation, attributes };
            return ((Set) client.send(client
                    .encodeMessage("createEntity", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));

        } catch (RemoteException rex) {
            EntityUtils.debug.warning(
                    "EntityObject:createEntity->RemoteException", rex);
            throw new EntityException(rex.getMessage(), "1000");
        } catch (Exception ex) {
            EntityUtils.debug.warning("EntityObject:createEntity->Exception",
                    ex);
            throw new EntityException(ex.getMessage(), "1000");
        }
    }

    /**
     * Deletes entities.
     * 
     * @param entityName
     *            Name of the entity.eg.cn=websphereAgent
     * @param entityType
     *            Type of entity being created. eg. Agent The types supported by
     *            SDK are configured in the list of Managed Objects in the
     *            <code>DAI</code> service.
     * @throws EntityException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteEntity(String entityName, String entityType)
            throws EntityException, SSOException {
        try {
            Object[] objs = { tokenString, entityName, entityType,
                    entityLocation };
            client.send(client.encodeMessage("deleteEntity", objs), 
        	    Session.getLBCookie(token.getTokenID().toString()),  null);
        } catch (RemoteException rex) {
            EntityUtils.debug.warning(
                    "EntityObject:deleteEntity->RemoteException", rex);
            throw new EntityException(rex.getMessage(), "1000");
        } catch (Exception ex) {
            EntityUtils.debug.warning("EntityObject:deleteEntity->Exception",
                    ex);
            throw new EntityException(ex.getMessage(), "1000");
        }
    }

    /**
     * Returns the entity object for each entity given the entityType and
     * entityName(s) in that particular organization.
     * 
     * @param entityName Name of the entity. example
     *        <code>cn=websphereAgent</code>.
     * @param entityType Type of entity being created. eg. Agent The types
     *        supported by SDK are configured in the list of Managed Objects
     *        in the <code>DAI</code> service.
     * @return Set of Entity objects.
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     */
    public Map getEntity(String entityName, String entityType)
            throws EntityException, SSOException {
        try {
            Object[] objs = { tokenString, entityName, entityType,
                    entityLocation };
            return ((Map) client.send(client.encodeMessage("getEntity", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (RemoteException rex) {
            EntityUtils.debug.warning(
                    "EntityObject:getEntity->RemoteException", rex);
            throw new EntityException(rex.getMessage(), "1000");
        } catch (Exception ex) {
            EntityUtils.debug.warning("EntityObject:getEntity->Exception", ex);
            throw new EntityException(ex.getMessage(), "1000");
        }
    }

    /**
     * Returns a set of Entity Names given the Entity Type for that particular
     * organization.
     * 
     * @param entityName Name of the entity. example
     *        <code>cn=websphereAgent</code>.
     * @param entityType Type of entity being created. eg. Agent The types
     *        supported by SDK are configured in the list of Managed Objects
     *        in the <code>DAI</code> service.
     * @param entityFilter
     * @return Set of Entity Names.
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     */
    public Set getEntityNames(
            String entityName,
            String entityType,
            String entityFilter
    ) throws EntityException, SSOException {
        try {
            Object[] objs = { tokenString, entityType, entityLocation,
                    entityFilter };
            return ((Set) client.send(client.encodeMessage("getEntityNames",
                    objs), Session.getLBCookie(token.getTokenID().toString()),
                    null));
        } catch (RemoteException rex) {
            EntityUtils.debug.warning(
                    "EntityObject:getEntityNames->RemoteException", rex);
            throw new EntityException(rex.getMessage(), "1000");
        } catch (Exception ex) {
            EntityUtils.debug.warning("EntityObject:getEntityNames->Exception",
                    ex);
            throw new EntityException(ex.getMessage(), "1000");
        }
    }

    /**
     * Sets or replaces attribute values with the new values supplied and Stores
     * the changes to directory server.
     * 
     * @param entityName Name of the entity. example
     *        <code>cn=websphereAgent</code>.
     * @param entityType Type of entity being created. eg. Agent The types
     *        supported by SDK are configured in the list of Managed Objects
     *        in the <code>DAI</code> service.
     * @param attributes
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     */
    public void modifyEntity(
            String entityName,
            String entityType,
            Map attributes
    ) throws EntityException, SSOException {
        try {
            Object[] objs = { tokenString, entityName, entityType,
                    entityLocation, attributes };
            client.send(client.encodeMessage("modifyEntity", objs), 
        	    Session.getLBCookie(token.getTokenID().toString()),  null);
        } catch (RemoteException rex) {
            EntityUtils.debug.warning(
                    "EntityObject:modifyEntity->RemoteException", rex);
            throw new EntityException(rex.getMessage(), "1000");
        } catch (Exception ex) {
            EntityUtils.debug.warning("EntityObject:modifyEntity->Exception",
                    ex);
            throw new EntityException(ex.getMessage(), "1000");
        }
    }
}
