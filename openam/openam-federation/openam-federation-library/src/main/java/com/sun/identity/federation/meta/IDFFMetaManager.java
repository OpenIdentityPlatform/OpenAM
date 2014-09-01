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
 * $Id: IDFFMetaManager.java,v 1.9 2009/10/28 23:58:57 exu Exp $
 *
 */


package com.sun.identity.federation.meta;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.AffiliationDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;

import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.bind.JAXBException;

/**
 * The <code>IDFFMetaManager</code> provides methods to manage the Service and
 * Identity Provider and Affiliation Metadata ,
 * the Entity Configuration.
 */

public class IDFFMetaManager {
    
    private static Debug debug = IDFFMetaUtils.debug;
    private static String ROOT_REALM = "/";
    private static String IDFF_METADATA_ATTR="sun-fm-idff-metadata";
    private static String IDFF_ENTITY_CONFIG_ATTR="sun-fm-idff-entityconfig";
    private static CircleOfTrustManager cotManagerStatic = null;
    private static ConfigurationInstance idffMetaConfigInstanceStatic = null;
    private CircleOfTrustManager cotManager = null;
    private ConfigurationInstance idffMetaConfigInstance = null;
    private Object callerSession = null;
    
    /**
     * Constant used to identify meta alias.
     */
    public static final String NAME_META_ALIAS_IN_URI = "metaAlias";

    static {
        try {
            idffMetaConfigInstanceStatic = 
                ConfigurationManager.getConfigurationInstance(
                IDFFMetaUtils.IDFF_META_SERVICE);
        } catch (ConfigurationException ce) {
            debug.error("IDFFMetaManager constructor:", ce);
        }
        if (idffMetaConfigInstanceStatic != null) {
            try {
                idffMetaConfigInstanceStatic.addListener(
                    new IDFFMetaServiceListener());
            } catch (ConfigurationException ce) {
                debug.error("IDFFMetaManager.static: Unable to register "
                        + "ConfigurationListener for IDFF Meta service",ce);
            }
        }

        try {
            cotManagerStatic = new CircleOfTrustManager();
        } catch (COTException ce) {
            debug.error("IDFFMetaManager:static:Error initializing COTManager");
        }
    }
    
    /**
     * Constructor.
     *
     * @param session handle to the session object.
     * @throws IDFFMetaException if there is an error creating this object.
     */
    public IDFFMetaManager(Object session) throws IDFFMetaException {
        String classMethod = "IDFFMetaManger:Constructor : " ;
        if (session == null) {
            idffMetaConfigInstance = idffMetaConfigInstanceStatic;
            cotManager = cotManagerStatic;
        } else {
            try {
                idffMetaConfigInstance =
                    ConfigurationManager.getConfigurationInstance(
                    IDFFMetaUtils.IDFF_META_SERVICE, session);
                cotManager = new CircleOfTrustManager(session);
            } catch (ConfigurationException ce) {
                debug.error("IDFFManager:Constructor : ", ce);
                throw new IDFFMetaException(ce);
            } catch (COTException cex) {
                debug.error("IDFFManager:Constructor : ", cex);
                throw new IDFFMetaException(cex);
            }
            callerSession = session;
        }
        
        if (idffMetaConfigInstance == null) {
            debug.error(classMethod + "Could not get Configuration Instance for"
                    + "IDFF Meta Service");
            LogUtil.error(Level.INFO,
                    LogUtil.ERROR_GET_IDFF_META_INSTANCE, null);
            throw new IDFFMetaException("nullConfig",null);
        }
    }
    
    /**
     * Creates the standard metadata entity descriptor.
     * The metadata is created under the realm.
     *
     * @param realm The realm under which the entity resides.
     * @param entityDescriptor The standard entity descriptor object to
     *        be created.
     * @throws IDFFMetaException if unable to create the entity descriptor.
     */
    public void createEntityDescriptor(
        String realm, EntityDescriptorElement entityDescriptor)
        throws IDFFMetaException 
    {
        String classMethod = "IDFFMetaManager.createEntityDescriptor:";
        String entityId= null;
        if (entityDescriptor == null) {
            LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_DESCRIPTOR, null);
            throw new IDFFMetaException("nullEntityDescriptor",null);
        } else {
            entityId = entityDescriptor.getProviderID();
            if (entityId == null) {
                debug.error(classMethod + "Entity ID is null");
                LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_ID, null);
                throw new IDFFMetaException("nullEntityID", null);
            }
        }
        
        if ((realm == null) || (realm.length() == 0)) {
            realm = ROOT_REALM;
        }
        String[] args = { entityId, realm };
        EntityDescriptorElement descriptor = getEntityDescriptor(
            realm,entityId);
        
        if (descriptor != null) {
            List idps = descriptor.getIDPDescriptor();
            boolean hasIDP = (idps != null) && !idps.isEmpty();
            List sps = descriptor.getSPDescriptor();
            boolean hasSP = (sps != null) && !sps.isEmpty();
            
            List newIDPs = entityDescriptor.getIDPDescriptor();
            List newSPs = entityDescriptor.getSPDescriptor();
            
            if ((newIDPs != null) && !newIDPs.isEmpty() && hasIDP) {
                LogUtil.error(Level.INFO, LogUtil.SET_ENTITY_FAILED, args);
                throw new IDFFMetaException("idpAlreadyExisted", args);
            }
            if ((newSPs != null) && !newSPs.isEmpty() && hasSP) {
                LogUtil.error(Level.INFO, LogUtil.SET_ENTITY_FAILED, args);
                throw new IDFFMetaException("spAlreadyExisted", args);
            }
            
            idps.addAll(newIDPs);
            sps.addAll(newSPs);
            setEntityDescriptor(realm, descriptor);
        } else {
            try {
                Map attrs = IDFFMetaUtils.convertJAXBToAttrMap(
                    IDFF_METADATA_ATTR, entityDescriptor);
                if (debug.messageEnabled()) {
                    debug.message(classMethod + attrs);
                }
                idffMetaConfigInstance.createConfiguration(
                    realm, entityId, attrs);
                LogUtil.access(Level.INFO, LogUtil.CREATE_ENTITY_SUCCEEDED,
                    args);
            } catch (ConfigurationException ce) {
                debug.error("Cannot create entity descriptor",ce);
                LogUtil.error(Level.INFO, LogUtil.CREATE_ENTITY_FAILED, args);
                throw new IDFFMetaException(ce);
            } catch (UnsupportedOperationException uoe) {
                debug.error(
                    "Creating EntityDescriptor : Unsupported operation");
                LogUtil.error(Level.INFO, LogUtil.UNSUPPORTED_OPERATION, null);
                throw new IDFFMetaException("unsupportedOperation",null);
            } catch (JAXBException jaxbe) {
                debug.error(classMethod , jaxbe);
                LogUtil.error(Level.INFO, LogUtil.INVALID_ENTITY_DESCRIPTOR, 
                    args);
                throw new IDFFMetaException("invalidEntityDescriptor",args);
            }
        }
    }
    
    /**
     * Returns the standard metadata entity descriptor under the realm.
     * @param realm The realm under which the entity resides.
     * @param entityID identifier of the entity to be retrieved.
     * @return <code>EntityDescriptorElement</code> for the entity or null if
     *         not found.
     * @throws IDFFMetaException if unable to retrieve the entity descriptor.
     */
    public EntityDescriptorElement getEntityDescriptor(
        String realm, String entityID)
    throws IDFFMetaException {
        String classMethod = "IDFFMetaManager.getEntityDescriptor:";
        if (debug.messageEnabled()) {
            debug.message(classMethod + " Retreiving EntityDescriptor");
        }
        EntityDescriptorElement entityDescriptor = null;
        if (entityID != null) {
            if ((realm == null) || (realm.length() == 0)) {
                realm = ROOT_REALM;
            }
            String[] args = { entityID, realm };
            // retrieve from cache
            if (callerSession == null) {
                entityDescriptor = IDFFMetaCache.getEntityDescriptor(
                    realm, entityID);
            }
            if (entityDescriptor == null)  {
                try {
                    Map attrs = idffMetaConfigInstance.getConfiguration(
                            realm, entityID);
                    if (attrs != null) {
                        Set metaValues = (Set) attrs.get(IDFF_METADATA_ATTR);
                        if (metaValues != null && !metaValues.isEmpty()) {
                            String metaValue =
                                    (String) metaValues.iterator().next();
                            Object object =
                                    IDFFMetaUtils.convertStringToJAXB(
                                    metaValue);
                            if (object instanceof EntityDescriptorElement) {
                                entityDescriptor =
                                        (EntityDescriptorElement) object;
                                IDFFMetaCache.setEntityDescriptor(
                                    realm, entityID, entityDescriptor);
                            } else {
                                debug.error(classMethod + "Invalid standard "
                                        + " meta value for : " + entityID);
                            }
                        }
                    }
                }  catch (ConfigurationException ce) {
                    debug.error("Cannot retrieve entity descriptor",ce);
                    LogUtil.error(Level.INFO, LogUtil.GET_ENTITY_FAILED, args);
                    throw new IDFFMetaException(
                            "cannotRetreiveEntityDescriptor",null);
                } catch (JAXBException jaxbe) {
                    debug.error(classMethod , jaxbe);
                    LogUtil.error(Level.INFO,
                            LogUtil.INVALID_ENTITY_DESCRIPTOR, args);
                    throw new IDFFMetaException(
                            "invalidEntityDescriptor", args);
                }
            }
            if (entityDescriptor != null) {
                LogUtil.access(Level.INFO, LogUtil.GET_ENTITY_SUCCEEDED, args);
            }
        } else {
            LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_ID, null);
            throw new IDFFMetaException("nullEntityID", null);
        }
        return entityDescriptor;
    }
    
    
    /**
     * Sets the standard metadata entity descriptor under the realm.
     * The EntiyDescriptor to be set should exist otherwise an error is
     * thrown.
     *
     * @param realm The realm under which the entity resides.
     * @param entityDescriptor The standard entity descriptor object to be set.
     * @throws IDFFMetaException if there is an error setting the entity
     *         descriptor.
     * @see #createEntityDescriptor(String, EntityDescriptorElement)
     */
    public void setEntityDescriptor(
        String realm, EntityDescriptorElement entityDescriptor)
    throws IDFFMetaException {
        String classMethod = "IDFFMetaManager:setEntityDescriptor";
        if (entityDescriptor != null) {
            String entityID = entityDescriptor.getProviderID();
            if ((realm == null) || (realm.length() == 0)) {
                realm = ROOT_REALM;
            }
            String[] args = { entityID, realm };
            try {
                Map origEntityAttrs = null;
                if (entityID != null) {
                    origEntityAttrs =
                            idffMetaConfigInstance.getConfiguration(realm,
                            entityID);
                    Map newAttrs =
                            IDFFMetaUtils.convertJAXBToAttrMap(
                            IDFF_METADATA_ATTR,entityDescriptor);
                    origEntityAttrs.put(IDFF_METADATA_ATTR,
                            newAttrs.get(IDFF_METADATA_ATTR));
                } else  {
                    LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_ID, args);
                    throw new IDFFMetaException("nullEntityID",null);
                }
                idffMetaConfigInstance.setConfiguration(
                        realm,entityID,origEntityAttrs);
                LogUtil.access(Level.INFO, LogUtil.SET_ENTITY_SUCCEEDED, args);
            }  catch (ConfigurationException ce) {
                debug.error("Error setting Entity Descriptor ",ce);
                LogUtil.error(Level.INFO, LogUtil.SET_ENTITY_FAILED, args);
                throw new IDFFMetaException(ce);
            } catch (JAXBException jaxbe) {
                debug.error(classMethod + "Invalid EntityID" + entityID, jaxbe);
                LogUtil.error(Level.INFO, LogUtil.INVALID_ENTITY_DESCRIPTOR,
                        args);
                throw new IDFFMetaException("invalidEntityDescriptor", args);
            }
        }
    }
    
    /**
     * Deletes the standard metadata entity descriptor under the realm.
     * @param realm The realm under which the entity resides.
     * @param  entityID identifier of the entity to be deleted.
     * @throws IDFFMetaException if there is an error deleting the entity
     *         descriptor.
     */
    public void  deleteEntityDescriptor(String realm, String entityID)
    throws IDFFMetaException {
        if (entityID == null) {
            LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_ID, null);
            throw new IDFFMetaException("nullEntityID",null);
        } else {
            if ((realm == null) || (realm.length() == 0)) {
                realm = ROOT_REALM;
            }
            String[] args = { entityID, realm };
            try {
                Map oldAttrs = idffMetaConfigInstance.getConfiguration(
                        realm, entityID);
                if (oldAttrs == null || oldAttrs.isEmpty() ) {
                    LogUtil.error(Level.INFO,
                            LogUtil.ENTITY_DOES_NOT_EXISTS, args);
                    throw new IDFFMetaException("entityDoesNotExists", args);
                }
                
                removeEntityFromCOT(realm, entityID);
                idffMetaConfigInstance.deleteConfiguration(
                        realm, entityID, null);
                LogUtil.access(Level.INFO,
                        LogUtil.DELETE_ENTITY_SUCCEEDED, args);
                IDFFMetaCache.setEntityDescriptor(realm, entityID, null);
            } catch (ConfigurationException ce) {
                debug.error("Error deleting Entity Descriptor" + entityID,ce);
                LogUtil.error(Level.INFO, LogUtil.DELETE_ENTITY_FAILED, args);
                throw new IDFFMetaException(ce);
            } catch (UnsupportedOperationException uoe) {
                debug.error("Unsupported operation",uoe);
                LogUtil.error(Level.INFO, LogUtil.UNSUPPORTED_OPERATION, null);
                throw new IDFFMetaException("unsupportedOperation",null);
            }
        }
    }
    
    /**
     * Deletes the extended entity configuration.
     * @param realm The realm under which the entity resides.
     * @param entityId The ID of the entity for whom the extended entity
     *                 configuration will be deleted.
     * @throws IDFFMetaException if unable to delete the entity descriptor.
     */
    public void deleteEntityConfig(String realm, String entityId)
    throws IDFFMetaException {
        if (entityId == null) {
            LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_ID, null);
            throw new IDFFMetaException("nullEntityID",null);
        } else {
            if ((realm == null) || (realm.length() == 0)) {
                realm = ROOT_REALM;
            }
            String[] args = {entityId, realm };
            try {
                Map oldAttrs = idffMetaConfigInstance.getConfiguration(
                        realm, entityId);
                if (oldAttrs == null || oldAttrs.isEmpty() ) {
                    LogUtil.error(Level.INFO,
                            LogUtil.ENTITY_DOES_NOT_EXISTS, args);
                    throw new IDFFMetaException("entityDoesNotExists", args);
                }
                Set oldValues = (Set) oldAttrs.get(IDFF_ENTITY_CONFIG_ATTR);
                if (oldValues == null || oldValues.isEmpty() ) {
                    LogUtil.error(Level.INFO,
                            LogUtil.NO_ENTITY_CONFIG_TO_DELETE, args);
                    throw new IDFFMetaException("noEntityConfig", args);
                }
                
                removeEntityFromCOT(realm, entityId);
                
                Set attr = new HashSet();
                attr.add(IDFF_ENTITY_CONFIG_ATTR);
                idffMetaConfigInstance.deleteConfiguration(
                        realm, entityId, attr);
                LogUtil.access(Level.INFO,
                        LogUtil.DELETE_ENTITY_CONFIG_SUCCEEDED, args);
                IDFFMetaCache.setEntityConfig(realm, entityId, null);
            } catch (ConfigurationException e) {
                debug.error("IDFFMetaManager.deleteEntityConfig:", e);
                LogUtil.error(Level.INFO, LogUtil.DELETE_ENTITY_CONFIG_FAILED,
                        args);
                throw new IDFFMetaException(e);
            } catch (UnsupportedOperationException uoe) {
                debug.error("Unsupported operation",uoe);
                LogUtil.error(Level.INFO, LogUtil.UNSUPPORTED_OPERATION, null);
                throw new IDFFMetaException("unsupportedOperation", null);
            }
        }
    }
    
    /**
     * Returns the Service Provider's Descriptor for the
     * entity identifier. If there are more then one Service Providers
     * the first one retrieved is returned.
     * @param realm The realm under which the entity resides.
     * @param entityID Entity Identifier to retrieve Service Provider from.
     * @return <code>SPDescriptorType</code> for the provider. A null is
     *         returned if no Service Provider is found.
     * @throws IDFFMetaException if there is an error retreiving the provider.
     */
    public SPDescriptorType getSPDescriptor(String realm, String entityID)
    throws IDFFMetaException {
        EntityDescriptorElement entityDescriptor =
                getEntityDescriptor(realm, entityID);
        
        return IDFFMetaUtils.getSPDescriptor(entityDescriptor);
    }
    
    /**
     * Returns Identity Provider's Descriptor for the
     * entity identifier. If there are more then one Identity Providers
     * the first one retrieved is returned.
     * @param realm The realm under which the entity resides.
     * @param entityID Entity Identifier to retrieve Identity Provider from.
     * @return <code>IDPDescriptorType</code> for the entity. A null is
     *         returned if no Identity Provider is found.
     * @throws IDFFMetaException if there is an error retreiving the provider.
     */
    public IDPDescriptorType getIDPDescriptor(String realm, String entityID)
    throws IDFFMetaException {
        EntityDescriptorElement entityDescriptor =
                getEntityDescriptor(realm, entityID);
        
        return IDFFMetaUtils.getIDPDescriptor(entityDescriptor);
    }
    
    
    /**
     * Returns the Affiliation Descriptor for the entity identifier.
     *
     * @param realm The realm under which the entity resides.
     * @param entityID Entity Identifier to retrieve Affiliation Descriptor
     *        from.
     * @return <code>AffiliationDescriptorType</code> the Affliation
     *         descriptor.
     * @throws IDFFMetaException if there is an error retreiving the
     *         affiliation.
     */
    public AffiliationDescriptorType getAffiliationDescriptor(
        String realm, String entityID)
    throws IDFFMetaException {
        AffiliationDescriptorType affiliationDescriptor = null;
        EntityDescriptorElement entityDescriptor =
                getEntityDescriptor(realm, entityID);
        if (entityDescriptor != null) {
            affiliationDescriptor = entityDescriptor.getAffiliationDescriptor();
        }
        return affiliationDescriptor;
    }
    
    /**
     * Creates the extended entity configuration under the realm.
     *
     * @param realm The realm under which the entity configuration will be
     * created.
     * @param entityConfig extended entity configuration to be created.
     * @throws IDFFMetaException if unable to create the entity configuration.
     */
    public void createEntityConfig(
        String realm, EntityConfigElement entityConfig)
    throws IDFFMetaException {
        String classMethod = "IDFFMetaManager.createEntityConfig:";
        String entityID = null;
        if (entityConfig == null) {
            LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_CONFIG, null);
            throw new IDFFMetaException("nullEntityConfig",null);
        } else {
            entityID = entityConfig.getEntityID();
            if (entityID == null) {
                LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_ID, null);
                debug.error( classMethod + "entity ID is null");
                throw new IDFFMetaException("nullEntityID", null);
            }
        }

        if ((realm == null) || (realm.length() == 0)) {
            realm = ROOT_REALM;
        }

        String[] args = { entityID, realm };
        try {
            Map attrs = IDFFMetaUtils.convertJAXBToAttrMap(
                    IDFF_ENTITY_CONFIG_ATTR,entityConfig);
            
            Map origAttrs = idffMetaConfigInstance.getConfiguration(realm,
                    entityID);
            if (origAttrs == null) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Entity Descriptor for" +
                            entityID + " does not exist");
                }
                LogUtil.error(Level.INFO,
                        LogUtil.ENTITY_CONFIG_NOT_FOUND, args);
                throw new IDFFMetaException("noEntityDescriptor",args);
            }
            Set origValues = (Set)origAttrs.get(IDFF_ENTITY_CONFIG_ATTR);
            if (!origValues.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Entity Config exists. " +
                            "Use setEntityConfig to set the configuration");
                }
                LogUtil.error(Level.INFO, LogUtil.ENTITY_CONFIG_EXISTS, args);
                throw new IDFFMetaException("entityConfigExists",args);
            }
            
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Entity Config Attrs :" + attrs);
            }
            idffMetaConfigInstance.setConfiguration(realm,entityID,attrs);
            
            // add entity to the circle of trust
            addEntityToCOT(realm, entityID);
            
            LogUtil.access(Level.INFO,
                    LogUtil.CREATE_ENTITY_CONFIG_SUCCEEDED, args);
        } catch (ConfigurationException ce) {
            debug.error(classMethod + "Cannot create entity config",ce);
            LogUtil.error(Level.INFO,
                    LogUtil.CREATE_ENTITY_CONFIG_FAILED, args);
            throw new IDFFMetaException(ce);
        } catch (UnsupportedOperationException uoe) {
            debug.error(classMethod + "Unsupported operation");
            LogUtil.error(Level.INFO, LogUtil.UNSUPPORTED_OPERATION, args);
            throw new IDFFMetaException(uoe);
        } catch (JAXBException jaxbe) {
            debug.error(classMethod , jaxbe);
            LogUtil.error(Level.INFO, LogUtil.INVALID_ENTITY_CONFIG, args);
            throw new IDFFMetaException("invalidEntityConfig", args);
        }
    }
    
    /**
     * Returns extended entity configuration under the realm.
     *
     * @param realm The realm under which the entity resides.
     * @param entityID identifier of the entity whose config is to be
     *        retrieved.
     * @return <code>EntityConfigElement</code> object of the entity or null
     *         if the entity configuration does not exist.
     * @throws IDFFMetaException if unable to retrieve the entity
     *                            configuration.
     */
    public EntityConfigElement getEntityConfig(String realm, String entityID)
    throws IDFFMetaException {
        String classMethod = "IDFFMetaManager:getEntityConfig:";
        EntityConfigElement entityConfig = null;
        if (entityID != null) {
            if ((realm == null) || (realm.length() == 0)) {
                realm = ROOT_REALM;
            }
            String[] args = { entityID, realm };
            if (callerSession == null) {
                // retrieve config from cache
                entityConfig = IDFFMetaCache.getEntityConfig(realm, entityID);
            }
            if (entityConfig == null) {
                try {
                    Map attrs = idffMetaConfigInstance.getConfiguration(
                            realm, entityID);
                    if (attrs != null) {
                        Set cfgValues =
                                (Set) attrs.get(IDFF_ENTITY_CONFIG_ATTR);
                        if (cfgValues != null && !cfgValues.isEmpty()) {
                            String cfgValue =
                                    (String) cfgValues.iterator().next();
                            Object object =
                                    IDFFMetaUtils.convertStringToJAXB(cfgValue);
                            if (object instanceof EntityConfigElement) {
                                entityConfig = (EntityConfigElement) object;
                                IDFFMetaCache.setEntityConfig(
                                        realm, entityID, entityConfig);
                            } else {
                                debug.error(classMethod + "Invalid entityID"
                                        + entityID);
                            }
                        }
                    }
                    
                }  catch (ConfigurationException ce) {
                    debug.error(classMethod+"Cannot retrieve entity config",ce);
                    LogUtil.error(Level.INFO,
                            LogUtil.GET_ENTITY_CONFIG_FAILED, args);
                    throw new IDFFMetaException(
                            "cannotRetreiveEntityConfig",null);
                } catch (JAXBException jaxbe) {
                    debug.error(classMethod , jaxbe);
                    LogUtil.error(Level.INFO,
                            LogUtil.INVALID_ENTITY_CONFIG, args);
                    throw new IDFFMetaException("invalidEntityConfig", args);
                }
            }
            if (entityConfig != null) {
                LogUtil.access(Level.INFO,
                        LogUtil.GET_ENTITY_CONFIG_SUCCEEDED, args);
            }
        } else {
            LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_ID, null);
            throw new IDFFMetaException("nullEntityID",null);
        }
        return entityConfig ;
    }
    
    /**
     * Sets the extended entity configuration under the realm.
     * The EntityConfig should exist in order to set attributes in
     * the EntityConfig.
     *
     * @param realm The realm under which the entity resides in
     * @param entityConfig The extended entity configuration object to be set.
     * @throws IDFFMetaException if unable to set the entity configuration.
     */
    public void setEntityConfig(String realm, EntityConfigElement entityConfig)
    throws IDFFMetaException {
        String classMethod = "IDFFMetaManager:setEntityConfig";
        if (entityConfig != null) {
            String entityID = entityConfig.getEntityID();
            if ((realm == null) || (realm.length() == 0)) {
                realm = ROOT_REALM;
            }
            Map origEntityAttrs = null;
            String[] args = { entityID, realm };
            try {
                if (entityID != null) {
                    origEntityAttrs =
                            idffMetaConfigInstance.getConfiguration(realm,
                            entityID);
                    Map newAttrs =
                            IDFFMetaUtils.convertJAXBToAttrMap(
                            IDFF_ENTITY_CONFIG_ATTR,entityConfig);
                    origEntityAttrs.put(IDFF_ENTITY_CONFIG_ATTR,
                            newAttrs.get(IDFF_ENTITY_CONFIG_ATTR));
                } else  {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod+"Entity Identifier is null");
                    }
                    LogUtil.error(Level.INFO, LogUtil.NULL_ENTITY_ID, null);
                    throw new IDFFMetaException("nullEntityID",null);
                }
                idffMetaConfigInstance.setConfiguration(
                        realm,entityID,origEntityAttrs);
                LogUtil.access(Level.INFO,
                        LogUtil.SET_ENTITY_CONFIG_SUCCEEDED, args);
                
            }  catch (ConfigurationException ce) {
                debug.error("Error setting Entity Descriptor ",ce);
                LogUtil.error(Level.INFO,
                        LogUtil.SET_ENTITY_CONFIG_FAILED, args);
                throw new IDFFMetaException(ce);
            } catch (JAXBException jaxbe) {
                debug.error(classMethod , jaxbe);
                LogUtil.error(Level.INFO, LogUtil.INVALID_ENTITY_CONFIG, args);
                throw new IDFFMetaException("invalidEntityConfig", args);
            }
        }
    }
    
    /**
     * Returns Service Provider's Configuration for the entity identifier.
     * If there are multiple service providers found then the first one
     * retrieved is returned.
     *
     * @param realm The realm under which the entity resides.
     * @param entityID ID of the entity to be retrieved.
     * @return <code>SPDescriptorConfigElement</code> for the entity
     *         identifier .  A null is returned if the configuration
     *         is not found.
     * @throws IDFFMetaException if there is an error retrieving service
     *         provider configuration.
     */
    public SPDescriptorConfigElement getSPDescriptorConfig(
        String realm, String entityID)
    throws IDFFMetaException 
    {
        
        EntityConfigElement entityConfigElement = getEntityConfig(
            realm, entityID);
        
        return IDFFMetaUtils.getSPDescriptorConfig(entityConfigElement);
    }
    
    /**
     * Returns Identity Provider's Configuration for the entity identifier.
     * If there are multiple identity providers found then the first one
     * retrieved is returned.
     * @param realm The realm under which the entity resides.
     * @param entityID ID of the entity to be retrieved.
     * @return <code>IDPDescriptorConfigElement</code> for the entity
     *         identifier .  A null is returned if the configuration
     *         is not found.
     * @throws IDFFMetaException if there is an error retrieving service
     *         provider configuration.
     */
    public IDPDescriptorConfigElement getIDPDescriptorConfig(
        String realm, String entityID)
    throws IDFFMetaException 
    {
        
        EntityConfigElement entityConfigElement = getEntityConfig(
            realm, entityID);
        
        return IDFFMetaUtils.getIDPDescriptorConfig(entityConfigElement);
    }
    
    /**
     * Returns Affiliation Configuration for the entity identifier.
     *
     * @param realm The realm under which the entity resides.
     * @param entityID ID of the entity to be retrieved.
     * @return <code>AffiliationDescriptorConfigElement</code> for the entity
     *         identifier .  A null is returned if the configuration
     *         is not found.
     * @throws IDFFMetaException if there is an error retrieving service
     *         provider configuration.
     */
    public AffiliationDescriptorConfigElement
            getAffiliationDescriptorConfig(String realm, String entityID)
            throws IDFFMetaException {
        AffiliationDescriptorConfigElement affiliationDesConfig = null;
        EntityConfigElement entityConfig = getEntityConfig(realm, entityID);
        if (entityConfig != null) {
            affiliationDesConfig =
                    (AffiliationDescriptorConfigElement)
                    entityConfig.getAffiliationDescriptorConfig();
        }
        return affiliationDesConfig;
    }
    
    /**
     * Returns all entities under the realm.
     *
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>Set</code> of entity ID <code>String</code>.
     * @throws IDFFMetaException if unable to retrieve the entity ids.
     */
    public Set getAllEntities(String realm) throws IDFFMetaException {
        Set entityIDSet = new HashSet() ;
        try {
            Set entityIDs =
                    idffMetaConfigInstance.getAllConfigurationNames(realm);
            if (entityIDs != null && !entityIDs.isEmpty()) {
                entityIDSet.addAll(entityIDs);
            }
            LogUtil.access(Level.INFO,
                    LogUtil.GET_ENTITY_NAMES_SUCCEEDED, null);
        } catch (ConfigurationException e) {
            debug.error("IDFFMetaManager.getAllEntities:", e);
            LogUtil.error(Level.INFO, LogUtil.GET_ENTITY_NAMES_FAILED, null);
            throw new IDFFMetaException(e);
        }
        return entityIDSet;
    }
    
    /**
     * Returns all remote entities under the realm.
     *
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of entity identifiers as Strings.
     * @throws IDFFMetaException if unable to retrieve the remote entity
     *         identifiers.
     */
    public List getAllHostedEntities(String realm) throws IDFFMetaException {
        List hostedEntityList = new ArrayList() ;
        try {
            Set entityIDs =
                    idffMetaConfigInstance.getAllConfigurationNames(realm);
            if (entityIDs != null && !entityIDs.isEmpty()) {
                Iterator entityIterator = entityIDs.iterator();
                while (entityIterator.hasNext()) {
                    String entityID = (String) entityIterator.next();
                    EntityConfigElement entityConfig =
                            getEntityConfig(realm, entityID);
                    if (entityConfig != null && entityConfig.isHosted()) {
                        hostedEntityList.add(entityID);
                    }
                }
            }
            LogUtil.access(Level.INFO,
                    LogUtil.GET_HOSTED_ENTITIES_SUCCEEDED, null);
        } catch (ConfigurationException e) {
            debug.error("IDFFMetaManager.getAllHostedEntities:", e);
            LogUtil.error(Level.INFO, LogUtil.GET_HOSTED_ENTITIES_FAILED, null);
            throw new IDFFMetaException(e);
        }
        return hostedEntityList;
    }
    
    /**
     * Returns all remote entities under the realm.
     *
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of entity identifiers as Strings.
     * @throws IDFFMetaException if unable to retrieve the remote entity
     *         identifiers.
     */
    public List getAllRemoteEntities(String realm) throws IDFFMetaException {
        
        List remoteEntityList = new ArrayList() ;
        try {
            Set entityIDs =
                    idffMetaConfigInstance.getAllConfigurationNames(realm);
            if (entityIDs != null && !entityIDs.isEmpty()) {
                Iterator entityIterator = entityIDs.iterator();
                while (entityIterator.hasNext()) {
                    String entityID = (String) entityIterator.next();
                    EntityConfigElement entityConfig =
                            getEntityConfig(realm, entityID);
                    if (entityConfig != null && !entityConfig.isHosted()) {
                        remoteEntityList.add(entityID);
                    }
                }
            }
            LogUtil.access(Level.INFO,
                    LogUtil.GET_REMOTE_ENTITIES_SUCCEEDED, null);
        } catch (ConfigurationException e) {
            debug.error("IDFFMetaManager.getAllRemoteEntities:", e);
            LogUtil.error(Level.INFO, LogUtil.GET_REMOTE_ENTITIES_FAILED, null);
            throw new IDFFMetaException(e);
        }
        return remoteEntityList;
    }
    
    /**
     * Returns all hosted Service Provider Entity Identifiers under the
     * realm.
     *
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of entity identifiers as Strings.
     * @throws IDFFMetaException if unable to retrieve the entity ids.
     */
    public List getAllHostedServiceProviderEntities(String realm) 
        throws IDFFMetaException {
        List hostedSPEntityList = new ArrayList() ;
        List hostedEntityIds = getAllHostedEntities(realm);
        
        Iterator entityIterator = hostedEntityIds.iterator();
        while (entityIterator.hasNext()) {
            String entityID = (String) entityIterator.next();
            if (getSPDescriptor(realm, entityID) != null) {
                hostedSPEntityList.add(entityID);
            }
        }
        LogUtil.access(Level.INFO,
                LogUtil.GET_HOSTED_SERVICE_PROVIDERS_SUCCEEDED, null);
        return hostedSPEntityList;
    }
    
    /**
     * Returns all remote Service Provider Entity Identifiers under the
     * realm.
     *
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of entity identifiers as Strings.
     * @throws IDFFMetaException if unable to retrieve the entity ids.
     */
    public List getAllRemoteServiceProviderEntities(String realm) 
        throws IDFFMetaException {
        List remoteSPEntityList = new ArrayList();
        List remoteEntitiesList = getAllRemoteEntities(realm);
        Iterator entityIterator = remoteEntitiesList.iterator();
        while (entityIterator.hasNext()) {
            String entityID = (String) entityIterator.next();
            if (getSPDescriptor(realm, entityID) != null) {
                remoteSPEntityList.add(entityID);
            }
        }
        LogUtil.access(Level.INFO,
                LogUtil.GET_REMOTE_SERVICE_PROVIDERS_SUCCEEDED, null);
        return remoteSPEntityList;
    }
    
    /**
     * Returns all hosted Identity Provider Entity Identifiers under the
     * realm.
     *
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of identity provider entity identifiers.
     *        The values in the list are Strings.
     * @throws IDFFMetaException if unable to retrieve the entity ids.
     */
    public List getAllHostedIdentityProviderIDs(String realm) 
        throws IDFFMetaException {
        List hostedIDPEntityList = new ArrayList() ;
        List hostedEntityIds = getAllHostedEntities(realm);
        
        Iterator entityIterator = hostedEntityIds.iterator();
        while (entityIterator.hasNext()) {
            String entityID = (String) entityIterator.next();
            if (getIDPDescriptor(realm, entityID) != null) {
                hostedIDPEntityList.add(entityID);
            }
        }
        LogUtil.access(Level.INFO,
                LogUtil.GET_HOSTED_IDENTITY_PROVIDERS_SUCCEEDED, null);
        return hostedIDPEntityList;
    }
    
    /**
     * Returns all remote Identity Provider Identifiers under the realm.
     *
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of remote identity provider identifiers.
     *         The values in the list are Strings.
     * @throws IDFFMetaException if unable to retrieve the provider identifiers.
     */
    public List getAllRemoteIdentityProviderIDs(String realm) 
        throws IDFFMetaException {
        List remoteIDPEntityList = new ArrayList();
        List remoteEntitiesList = getAllRemoteEntities(realm);
        Iterator entityIterator = remoteEntitiesList.iterator();
        while (entityIterator.hasNext()) {
            String entityID = (String) entityIterator.next();
            if (getIDPDescriptor(realm, entityID) != null) {
                remoteIDPEntityList.add(entityID);
            }
        }
        LogUtil.access(Level.INFO,
                LogUtil.GET_REMOTE_IDENTITY_PROVIDERS_SUCCEEDED, null);
        return remoteIDPEntityList;
    }
    
    /**
     * Checks whether two entities are in the same circle of trust.
     *
     * @param realm The realm under which the entity resides.
     * @param hostedEntityID the hosted entity identifier.
     * @param entityID the identifier of the entity to be checked for trust.
     * @return true if both providers are in the same circle of trust.
     */
    public boolean isTrustedProvider(
        String realm, String hostedEntityID, String entityID) 
    {
        String classMethod = "IDFFMetaManager:isTrustedProvider";
        boolean isTrusted = false;
        try {
            SPDescriptorConfigElement spConfig =
                    getSPDescriptorConfig(realm, entityID);
            if (spConfig != null) {
                isTrusted = isSameCircleOfTrust(spConfig,realm, entityID);
            } else {
                IDPDescriptorConfigElement idpConfig =
                        getIDPDescriptorConfig(realm, entityID);
                if (idpConfig != null) {
                    isTrusted = isSameCircleOfTrust(idpConfig,realm, entityID);
                }
            }
        } catch (IDFFMetaException ide) {
            debug.error(classMethod + "Error retrieving trust relationship"
                    + "between "+ hostedEntityID + "with " + entityID);
        }
        return isTrusted;
    }
    
    /**
     * Returns a set of remote providers trusted by the hosted providers.
     *
     * @param hostedProviderMetaAlias the metaAlias of the hosted provider.
     * @return a set of trusted remote providers. An empty set is returned
     *         if there is an error or no trusted providers are found.
     */
    public Set getAllTrustedProviders(String hostedProviderMetaAlias) {
        String classMethod = "IDFFMetaManager:getAllTrustedProviders:";
        Set trustedProviders = new HashSet();
        try {
            String hostedEntityID =
                    getEntityIDByMetaAlias(hostedProviderMetaAlias);
            String role = getProviderRoleByMetaAlias(hostedProviderMetaAlias);
            String realm = IDFFMetaUtils.getRealmByMetaAlias(
                hostedProviderMetaAlias);
            List hostedEntityIDs = new ArrayList();
            List remoteEntityIDs = new ArrayList();
            if (role!=null && role.equalsIgnoreCase(IFSConstants.SP)) {
                hostedEntityIDs = getAllHostedServiceProviderEntities(realm);
                remoteEntityIDs = getAllRemoteIdentityProviderIDs(realm);
            } else if (role != null && role.equalsIgnoreCase(IFSConstants.IDP)){
                hostedEntityIDs = getAllHostedIdentityProviderIDs(realm);
                remoteEntityIDs = getAllRemoteServiceProviderEntities(realm);
            }
            Map idffCOTs = cotManager.getIDFFCOTProviderMapping(realm);
            
            Set cotSet = idffCOTs.keySet();
            if (hostedEntityIDs.size() > 0 &&
                    hostedEntityIDs.contains(hostedEntityID)) {
                for (Iterator iter = cotSet.iterator(); iter.hasNext();) {
                    String name = (String)iter.next();
                    Set tProviders = (Set) idffCOTs.get(name);
                    if (tProviders.contains(hostedEntityID)) {
                        for (Iterator i = tProviders.iterator() ; i.hasNext();){
                            String trustedProvider = (String) i.next();
                            if (remoteEntityIDs.size() > 0 &&
                                    remoteEntityIDs.contains(trustedProvider)) {
                                trustedProviders.add(trustedProvider);
                            }
                        }
                    }
                }
            }
        } catch (COTException cote) {
            debug.error(classMethod +"Error getting trustedProvider list",cote);
        } catch (IDFFMetaException ide) {
            debug.error(classMethod +"Error getting trustedProvider list", ide);
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + " remote trusted providers :"
                    + trustedProviders);
        }
        return trustedProviders;
    }

    /**
     * Checks if the provider is a member of the Affiliation.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID the provider's identitifer.
     * @param affiliationID the Affiliation identifier.
     *
     * @return true if the provider is a member of the affiliation.
     * @throws IDFFMetaException if there is an error retreiving the affiliate
     *         information.
     */
    public boolean isAffiliateMember(
        String realm, String providerID, String affiliationID)
    throws IDFFMetaException {
        boolean isAffiliateMember = false;
        if ((realm == null) || (realm.length() == 0)) {
            realm = ROOT_REALM;
        }
        if (providerID != null && affiliationID != null) {
            Set entityIDSet = getAllEntities(realm);
            if (entityIDSet != null && !entityIDSet.isEmpty()) {
                Iterator entityIterator = entityIDSet.iterator();
                while (entityIterator.hasNext()) {
                    String entityID = (String)  entityIterator.next();
                    AffiliationDescriptorType affDescriptor =
                            getAffiliationDescriptor(realm, entityID);
                    if (affDescriptor != null) {
                        String affID = affDescriptor.getAffiliationID();
                        if (affID != null && affID.equals(affiliationID))  {
                            List affMemberList =
                                    affDescriptor.getAffiliateMember();
                            if (affMemberList != null &&
                                    !affMemberList.isEmpty() &&
                                    affMemberList.contains(providerID)) {
                                isAffiliateMember = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        String[] args = { providerID , affiliationID, realm };
        LogUtil.access(Level.INFO, LogUtil.IS_AFFILIATE_MEMBER_SUCCEEDED, args);
        return isAffiliateMember;
    }
    
    
    /**
     * Returns a set of Affiliation Entity Descriptors of which the
     * provider identifier is a member of.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID the provider identifier.
     * @return a set of Affliation Entity Descriptors. An empty set is
     *         returned if there are no affiliation descriptors found or
     *         if there is an retrieving the descriptors.
     */
    public Set getAffiliateEntity(String realm, String providerID) {
        String classMethod = "IDFFMetaManager:getAffiliateEntity:";
        Set affDescSet = new HashSet();
        if ((realm == null) || (realm.length() == 0)) {
            realm = ROOT_REALM;
        }
        try {
            Set entityIDs = getAllEntities(realm);
            if (entityIDs != null && !entityIDs.isEmpty()) {
                Iterator eIter = entityIDs.iterator();
                while (eIter.hasNext()) {
                    String entityID = (String) eIter.next();
                    AffiliationDescriptorType affDesc =
                            getAffiliationDescriptor(realm, entityID);
                    if (affDesc != null) {
                        String affId = affDesc.getAffiliationID();
                        if (isAffiliateMember(realm, providerID,affId)) {
                            affDescSet.add(affDesc);
                        }
                        
                    }
                }
            }
        } catch (IDFFMetaException ide) {
            if (debug.messageEnabled()) {
                debug.error(classMethod +
                            "Error getting affiliate entities",ide);
            }
        }
        return affDescSet;
    }
    
    /**
     * Returns entity ID associated with the metaAlias.
     *
     * @param metaAlias The Meta Alias of the provider.
     * @return entity ID associated with the metaAlias or null if not found.
     * @throws IDFFMetaException if unable to retrieve the entity id.
     */
    public String getEntityIDByMetaAlias(String metaAlias)
    throws IDFFMetaException {
        
        try {
            if (metaAlias == null || metaAlias.length() == 0) {
                return null;
            }
            
            // check cache first
            String entityId = IDFFMetaCache.getEntityByMetaAlias(metaAlias);
            if (entityId != null) {
                if (debug.messageEnabled()) {
                    debug.message("IDFFMetaManager.getEntityByMetaAlias :"
                            + " found entity in cache, metaAlias=" + metaAlias
                            + ", ID=" + entityId);
                }
                return entityId;
            }
            String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            Set entityIds =
                    idffMetaConfigInstance.getAllConfigurationNames(realm);
            if (entityIds == null || entityIds.isEmpty()) {
                return null;
            }
            
            for(Iterator iter = entityIds.iterator(); iter.hasNext();) {
                String tmpId = (String)iter.next();
                if (debug.messageEnabled()) {
                    debug.message("IDFFMetaManager.getEntityByMetaAlias :"
                            + " process entity cache for metaAlias=" + metaAlias
                            + ", ID=" + tmpId);
                }
                
                SPDescriptorConfigElement spconfig =
                        getSPDescriptorConfig(realm, tmpId);
                if (spconfig != null) {
                    String tmpMetaAlias = spconfig.getMetaAlias();
                    if (tmpMetaAlias != null && tmpMetaAlias.length() > 0) {
                        if (metaAlias.equals(tmpMetaAlias)) {
                            // remember this and continue to process others,
                            entityId = tmpId;
                        }
                        IDFFMetaCache.setMetaAliasEntityMapping(tmpMetaAlias,
                            tmpId);
                        IDFFMetaCache.setMetaAliasRoleMapping(tmpMetaAlias,
                            IFSConstants.SP);
                        if (debug.messageEnabled()) {
                            debug.message(
                                "IDFFMetaManager.getEntityByMetaAlias :"
                                + " save to cache, metaAlias=" + tmpMetaAlias
                                + ", ID=" + tmpId + ", role=" +IFSConstants.SP);
                        }
                    }
                }
                
                IDPDescriptorConfigElement idpconfig =
                        getIDPDescriptorConfig(realm, tmpId);
                if (idpconfig != null) {
                    String tmpMetaAlias = idpconfig.getMetaAlias();
                    if (tmpMetaAlias != null && tmpMetaAlias.length() > 0) {
                        if (metaAlias.equals(tmpMetaAlias)) {
                            // remember this and continue to process others,
                            entityId = tmpId;
                        }
                        IDFFMetaCache.setMetaAliasEntityMapping(tmpMetaAlias,
                            tmpId);
                        IDFFMetaCache.setMetaAliasRoleMapping(tmpMetaAlias,
                            IFSConstants.IDP);
                        if (debug.messageEnabled()) {
                            debug.message(
                                "IDFFMetaManager.getEntityByMetaAlias :"
                                + " save to cache, metaAlias=" + tmpMetaAlias
                                + ", ID=" + tmpId + ", role=" 
                                + IFSConstants.IDP);
                        }
                    }
                }
            }
            
            return entityId;
        } catch (ConfigurationException e) {
            debug.error("IDFFMetaManager.getEntityByMetaAlias:", e);
            throw new IDFFMetaException(e);
        }
    }
    
    /**
     * Returns entity role associated with the metaAlias.
     *
     * @param metaAlias The Meta Alias of the provider.
     * @return role of the provider with the metaAlias or null if not found.
     * @throws IDFFMetaException if unable to retrieve the entity role.
     */
    public String getProviderRoleByMetaAlias(String metaAlias)
    throws IDFFMetaException {
        String entityID = getEntityIDByMetaAlias(metaAlias);
        if (entityID == null) {
            return null;
        } else {
            return IDFFMetaCache.getRoleByMetaAlias(metaAlias);
        }
    }
    
    /**
     * Returns entity ID associated with the succinct ID.
     *
     * @param realm The realm under which the entity resides.
     * @param succinctId Succinct ID of a IDP contained in the entity
     *        to be retrieved.
     * @return Entity ID associated with the succinct ID, or null if could
     *          not be found.
     * @throws IDFFMetaException if unable to retrieve the entity ID.
     */
    public String getEntityIDBySuccinctID(String realm, String succinctId)
    throws IDFFMetaException {
        
        try {
            if (succinctId == null || succinctId.length() == 0) {
                return null;
            }
            
            // check cache first
            String entityId = IDFFMetaCache.getEntityBySuccinctID(succinctId);
            if (entityId != null) {
                if (debug.messageEnabled()) {
                    debug.message("IDFFMetaManager.getEntityIDBySuccinctID :"
                            + " found entity in cache, succinctId=" + succinctId
                            + ", ID=" + entityId);
                }
                return entityId;
            }
            
            if ((realm == null) || (realm.length() == 0)) {
                realm = ROOT_REALM;
            }
            Set entityIds =
                    idffMetaConfigInstance.getAllConfigurationNames(realm);
            if (entityIds == null || entityIds.isEmpty()) {
                return null;
            }
            
            for(Iterator iter = entityIds.iterator(); iter.hasNext();) {
                String tmpId = (String)iter.next();
                if (debug.messageEnabled()) {
                    debug.message("IDFFMetaManager.getEntityIDBySuccinctID :"
                            + " process entity cache for succinctID="
                            + succinctId + ", ID=" + tmpId);
                }
                
                IDPDescriptorConfigElement idpconfig =
                        getIDPDescriptorConfig(realm, tmpId);
                if (idpconfig != null) {
                    String tmpSuccinctId = FSUtils.generateSourceID(tmpId);
                    if ((tmpSuccinctId != null) &&
                            (succinctId.equals(tmpSuccinctId))) {
                        // remember this and continue to process others,
                        entityId = tmpId;
                    }
                    IDFFMetaCache.setEntitySuccinctIDMapping(
                        tmpSuccinctId, tmpId);
                    if (debug.messageEnabled()) {
                        debug.message("IDFFMetaManager.getEntityBySuccinctID"
                                + ": update cache, succinctId=" + tmpSuccinctId
                                + ", entity ID=" + tmpId);
                    }
                }
            }
            
            return entityId;
        } catch (ConfigurationException e) {
            debug.error("IDFFMetaManager.getEntityIDBySuccinctID:", e);
            throw new IDFFMetaException(e);
        }
    }
    
    /**
     * Checks if the remote entity identifier is in the Entity Config's 
     * circle of trust.
     *
     * @param realm The realm under which the entity resides.
     * @param config the entity config object.
     * @param entityID the entity identifer.
     */
    private boolean isSameCircleOfTrust(
        BaseConfigType config, String realm, String entityID)
    {
        boolean isTrusted = false;
        if (config != null) {
            Map attr = IDFFMetaUtils.getAttributes(config);
            List cotList = (List) attr.get(IDFFCOTUtils.COT_LIST);
            if ((cotList != null) && !cotList.isEmpty()) {
                for (Iterator iter = cotList.iterator(); iter.hasNext();) {
                    String cotName = (String) iter.next();
                    if (cotManager.isInCircleOfTrust(realm,cotName,
                            COTConstants.IDFF,entityID)) {
                        isTrusted = true;
                    }
                }
            }
        }
        return isTrusted;
    }
    
    /**
     * Adds entity identifier to circle of trust.
     *
     * @param realm The realm under which the entity resides.
     * @param config the entity config.
     * @param entityId the entity identifier.
     */
    private void addToCircleOfTrust(BaseConfigType config,
        String realm, String entityId) 
    {
        String classMethod = "COTManager:addToCircleOfTrust :";
        try {
            if (config != null) {
                Map attr = IDFFMetaUtils.getAttributes(config);
                List cotAttr = (List) attr.get(COTConstants.COT_LIST);
                List cotList = new ArrayList(cotAttr);
                if ((cotList != null) && !cotList.isEmpty()) {
                    for (Iterator iter = cotList.iterator();
                    iter.hasNext();) {
                        String cotName = (String) iter.next();
                        try {
                            cotManager.addCircleOfTrustMember(
                                    realm, cotName,
                                    COTConstants.IDFF,entityId);
                        } catch (COTException ce) {
                            debug.error(classMethod + cotName + " cotName");
                        }
                    }
                }
            }
        } catch (Exception e) {
            debug.error(classMethod + "Error while adding entity "
                    + entityId + " to COT.",e);
        }
    }
    
    /**
     * Remove entity identifier from circle of trust.
     *
     * @param realm The realm under which the entity resides.
     * @param config the <code>BaseConfigType</code> object
     *        which is the entity config.
     * @param entityId the entity identifier.
     */
    private void removeFromCircleOfTrust(
        BaseConfigType config, String realm, String entityId) 
    {
        String classMethod = "IDFFMetaManager:removeFromCircleOfTrust:";
        try {
            if (config != null) {
                Map attr = IDFFMetaUtils.getAttributes(config);
                List cotAttr = (List) attr.get(COTConstants.COT_LIST);
                List cotList = new ArrayList(cotAttr) ;
                if ((cotList != null) && !cotList.isEmpty()) {
                    for (Iterator iter = cotList.iterator();
                    iter.hasNext();) {
                        String cotName = (String) iter.next();
                        if (cotName != null && cotName.length() > 0) {
                            try {
                                cotManager.removeCircleOfTrustMember(realm,
                                        cotName,COTConstants.IDFF,entityId);
                            } catch (COTException ce) {
                                if (debug.messageEnabled()) {
                                    debug.message(classMethod + "Invalid COT: "
                                            + cotName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            debug.error("IDFFMetaManager.removeFromCircleOfTrust:" +
                "Error while removing entity " + entityId + " from COT.",e);
        }
    }
    
    /**
     * Adds an entity identifier to circle of trust.
     *
     * @param realm The realm under which the entity resides in
     * @param entityID the entity identifier.
     * @throws IDFFMetaException if there is an error adding entity to circle
     *         of trust.
     */
    private void addEntityToCOT(String realm, String entityID) 
        throws IDFFMetaException 
    {
        IDPDescriptorConfigElement idpConfig =
                getIDPDescriptorConfig(realm, entityID);
        if (idpConfig !=null) {
            addToCircleOfTrust(idpConfig, realm, entityID);
        }
        
        SPDescriptorConfigElement spConfig = getSPDescriptorConfig(
            realm, entityID);
        if (spConfig != null) {
            addToCircleOfTrust(spConfig,realm, entityID);
        }
    }
    
    /**
     * Removes and entity identifier from circle of trust.
     * @param realm The realm under which the entity resides.
     * @param entityID the entity identifier.
     * @throws IDFFMetaException if there is an error remove entity.
     */
    private void removeEntityFromCOT(String realm, String entityID) 
        throws IDFFMetaException 
    {
        IDPDescriptorConfigElement idpConfig =
                getIDPDescriptorConfig(realm, entityID);
        if (idpConfig != null) {
            removeFromCircleOfTrust(idpConfig, realm, entityID);
        }
        
        SPDescriptorConfigElement spConfig = getSPDescriptorConfig(
            realm, entityID);
        if (spConfig != null) {
            removeFromCircleOfTrust(spConfig, realm, entityID);
        }

        AffiliationDescriptorConfigElement affiConfig = 
            getAffiliationDescriptorConfig(realm, entityID);
        if (affiConfig != null) {
            removeFromCircleOfTrust(affiConfig, realm, entityID);
        }
    }
}
