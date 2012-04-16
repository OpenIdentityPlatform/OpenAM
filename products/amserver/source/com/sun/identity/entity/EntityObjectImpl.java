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
 * $Id: EntityObjectImpl.java,v 1.3 2008/06/25 05:43:26 qcheng Exp $
 *
 */

package com.sun.identity.entity;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMEntityType;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.Cache;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

/**
 * Implementation class for the interface EntityObjectIF
 */

public class EntityObjectImpl implements EntityObjectIF {

    protected static Cache oCache;

    protected static Object lock = new Object();

    protected SSOToken token;

    protected AMStoreConnection amsc;

    protected AMOrganization entity = null;

    private static SSOTokenManager tokenManager;

    /**
     * Creates entities.
     * 
     * @param ssotoken
     *            String representing user's SSO Token.
     * @param entityName
     *            Name of this entity.eg.cn=websphereAgent
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
    public Set createEntity(String ssotoken, String entityName,
            String entityType, String entityLocation, Map attributes)
            throws EntityException, SSOException {

        Set entitySet = new HashSet();
        initializeObject(ssotoken, entityLocation);
        try {
            int type = 0;

            type = getIntTypeFromStr(entityType);
            Map input = new HashMap(2);
            input.put(entityName, attributes);
            if (entity != null) {
                Set entityObjs = entity.createEntities(type, input);
                Iterator it = entityObjs.iterator();
                while (it.hasNext()) {
                    entitySet.add(((AMEntity) it.next()).getDN());
                }
            }
        } catch (AMException amex) {
            EntityUtils.debug.error("EntityObjectImpl.createEntity() : "
                    + "Create Entity Failed. " + amex);
            throw EntityUtils.convertException(amex);
        }
        return entitySet;
    }

    /**
     * Deletes entities.
     * 
     * @param ssoToken User's Single Sign On Token.
     * @param entityName Name of this entity. example
     *        <code>cn=websphereAgent</code>.
     * @param entityType Type of entity being created. eg. Agent The types
     *        supported by SDK are configured in the list of Managed Objects
     *        in the <code>DAI</code> service.
     * @param entityLocation Location of the entity creation. example
     *        <code>www.abc.com</code>.
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     */
    public void deleteEntity(
            String ssoToken,
            String entityName,
            String entityType,
            String entityLocation
    ) throws EntityException, SSOException {
        initializeObject(ssoToken, entityLocation);
        Set entityNameSet = new HashSet();
        try {
            int type = 0;

            type = getIntTypeFromStr(entityType);
            String entDN = getEntityDN(entityName, type, entityLocation);
            entityNameSet.add(entDN);

            if (entity != null) {
                entity.deleteEntities(type, entityNameSet);
                if (EntityUtils.debug.messageEnabled()) {
                    EntityUtils.debug
                            .message("EntityObjectImpl.deleteEntity():"
                                    + " Deleted " + entityNameSet.toString());
                }
            }
        } catch (AMException amex) {
            EntityUtils.debug.error("EntityObjectImpl.deleteEntity() : "
                    + "Delete Entity Failed. " + amex);
            throw EntityUtils.convertException(amex);
        }
    }

    /**
     * Returns the entity object for each entity given the entityType and
     * entityName(s) in that particular organization.
     * 
     * @param ssoToken User's Single Sign On Token.
     * @param entityName Name of this entity. example
     *        <code>cn=websphereAgent</code>.
     * @param entityType Type of entity being created. eg. Agent The types
     *        supported by SDK are configured in the list of Managed Objects
     *        in the <code>DAI</code> service.
     * @param entityLocation Location of the entity creation. example
     *        <code>www.abc.com</code>.
     * @return Set of Entity objects.
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     * @throws RemoteException
     */
    public Map getEntity(
            String ssoToken,
            String entityName,
            String entityType,
            String entityLocation
    ) throws EntityException, SSOException, RemoteException {
        initializeObject(ssoToken, entityLocation);
        Map entityMap = new HashMap();
        return entityMap;
    }

    /**
     * Returns a set of Entity Names given the Entity Type for that particular
     * organization.
     * 
     * @param ssoToken User's Single Sign On Token.
     * @param entityType Type of entity being created. eg. Agent The types
     *        supported by SDK are configured in the list of Managed Objects
     *        in the <code>DAI</code> service.
     * @param entityLocation Location of the entity creation. example
     *        <code>www.abc.com</code>.
     * @param entityFilter
     * @return Set of Entity Names.
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     * @throws RemoteException
     */
    public Set getEntityNames(
            String ssoToken,
            String entityType,
            String entityLocation,
            String entityFilter
    ) throws EntityException, SSOException, RemoteException {
        initializeObject(ssoToken, entityLocation);
        Set entitySet = new HashSet();
        return entitySet;
    }

    /**
     * Sets or replaces attribute values with the new values supplied and Stores
     * the changes to directory server.
     * 
     * @param ssoToken User's Single Sign Token.
     * @param entityName Name of this entity. example
     *        <code>cn=websphereAgent</code>
     * @param entityType Type of entity being created. eg. Agent The types
     *        supported by SDK are configured in the list of Managed Objects
     *        in the <code>DAI</code> service.
     * @param entityLocation Location of the entity creation. example 
     *        <code>www.abc.com</code>.
     * @throws EntityException if there is an internal error in the AM Store.
     * @throws SSOException if the sign on is no longer valid.
     * @throws RemoteException
     */
    public void modifyEntity(
            String ssoToken,
            String entityName,
            String entityType,
            String entityLocation,
            Map attributes
    ) throws EntityException, SSOException, RemoteException {
        initializeObject(ssoToken, entityLocation);
        AMEntity amEntity = getAMEntity(ssoToken, entityName, entityType,
                entityLocation);
        try {
            if (amEntity != null) {
                amEntity.setAttributes(attributes);
                amEntity.store();
            }
        } catch (AMException amex) {
            EntityUtils.debug.error("EntityObjectImpl.modifyEntity() : "
                    + "Modify Entity Failed. " + amex);
            throw EntityUtils.convertException(amex);
        }
    }

    /**
     * Method to get the token manager handle.
     */
    protected static void checkInitialization() throws SSOException {
        if (tokenManager == null) {
            synchronized (lock) {
                if (tokenManager == null) {
                    try {
                        tokenManager = SSOTokenManager.getInstance();
                        oCache = new Cache(1000);
                    } catch (SSOException ssoe) {
                        EntityUtils.debug.error(
                                "EntityObjectImpl:checkInitialization() " +
                                ": Unable to get SSOTokenManager",
                                        ssoe);
                        throw (ssoe);
                    }
                }
            }
        }
    }

    /**
     * Method to get the AMEntity object from the storeconnection.
     */
    protected AMEntity getAMEntity(String ssoToken, String entityName,
            String entityType, String entityLocation) throws EntityException,
            SSOException {

        checkInitialization();
        AMEntity amEntity;
        try {
            int type = 0;
            type = getIntTypeFromStr(entityType);

            String entDN = getEntityDN(entityName, type, entityLocation);

            String key = ssoToken + "/" + entDN;
            amEntity = (AMEntity) oCache.get(key);
            if (amEntity == null) {
                amEntity = amsc.getEntity(entDN);
                oCache.put(key, amEntity);
            }
        } catch (SSOException ssoe) {
            EntityUtils.debug.error("EntityObjectImpl.getAMEntity(): "
                    + "Unable to convert SSOToken: " + ssoToken, ssoe);
            throw ssoe;
        }
        return amEntity;
    }

    /**
     * Method to initialize the object. The AMStoreConnection handle is obtained
     * by creating a valid SSOToken.
     */
    protected void initializeObject(String ssoToken, String entityLocation)
            throws EntityException, SSOException {
        checkInitialization();
        try {
            token = tokenManager.createSSOToken(ssoToken);
            amsc = new AMStoreConnection(token);
            String orgDN = amsc.getOrganizationDN(entityLocation, null);
            entity = amsc.getOrganization(orgDN);
        } catch (AMException amex) {
            EntityUtils.debug.error("EntityObjectImpl.initializeObject() : "
                    + "Unable to get Organization DN " + amex);
            throw EntityUtils.convertException(amex);
        } catch (SSOException ssoe) {
            EntityUtils.debug.error("EntityObjectImpl.initializeObject() : "
                    + "Unable to convert SSOToken: " + ssoToken, ssoe);
            throw ssoe;
        }
        if (EntityUtils.debug.messageEnabled()) {
            EntityUtils.debug.message("EntityObjectImpl.getAMEntity(): "
                    + "Obtained ssotoken: " + ssoToken);
            EntityUtils.debug.message("EntityObjectImpl.getAMEntity(): "
                    + "Obtained AMSToreConnection object for SSOToken: "
                    + ssoToken);
        }
    }

    /**
     * Method to convert the entity type from string to integer recognizable by
     * SDK.
     */
    private int getIntTypeFromStr(String entityType) {

        int type = 0;
        Set supportedTypes = amsc.getEntityTypes();
        Iterator iter = supportedTypes.iterator();
        while (iter.hasNext()) {
            AMEntityType amEntityType = (AMEntityType) iter.next();
            if (amEntityType.getName().equalsIgnoreCase(entityType)) {
                type = amEntityType.getType();
                break;
            }
        }
        return type;
    }

    /**
     * Method to get the DN of the entity based on the search results for the
     * entityName from the entityLocation.
     */
    private String getEntityDN(String entityName, int entityType,
            String entityLocation) throws EntityException, SSOException {

        String entDN = null;
        try {
            Set entityResults = entity.searchEntities(entityType, "*",
                    AMConstants.SCOPE_SUB, new HashMap());
            Iterator iter = entityResults.iterator();
            while (iter.hasNext()) {
                entDN = (String) iter.next();
                if (entDN.indexOf(entityName) >= 0) {
                    break;
                }
            }
        } catch (AMException amex) {
            EntityUtils.debug.error("EntityObjectImpl.getEntityDN() : "
                    + "Unable to get DN for the Entity " + amex);
            throw EntityUtils.convertException(amex);
        }
        return entDN;
    }
}
