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
 * $Id: WSFederationMetaManager.java,v 1.8 2009/10/28 23:58:59 exu Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */


package com.sun.identity.wsfederation.meta;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerEndpointElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerNameElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenSigningKeyInfoElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.UriNamedClaimTypesOfferedElement;
import com.sun.identity.wsfederation.logging.LogUtil;
import com.sun.identity.wsfederation.jaxb.wsse.SecurityTokenReferenceType;
import com.sun.identity.wsfederation.jaxb.xmlsig.X509DataType;
import com.sun.identity.wsfederation.jaxb.xmlsig.X509DataType.X509Certificate;

/**
 * The <code>WSFederationMetaManager</code> provides methods to manage both the 
 * standard entity descriptor and the extended entity configuration.
 */
public class WSFederationMetaManager {
    private static final String ATTR_METADATA = "sun-fm-wsfederation-metadata";
    private static final String ATTR_ENTITY_CONFIG =
                                            "sun-fm-wsfederation-entityconfig";
    private static final String SUBCONFIG_ID = "Federation";
    private static final int SUBCONFIG_PRIORITY = 0;

    private static Debug debug = WSFederationMetaUtils.debug;
    private static CircleOfTrustManager cotmStatic;
    private static ConfigurationInstance configInstStatic;
    private static final String WSFEDERATION = "WS-FEDERATION";
    private CircleOfTrustManager cotm;
    private ConfigurationInstance configInst;
    private Object callerSession = null;

    static {
        try {
            configInstStatic = 
                ConfigurationManager.getConfigurationInstance(WSFEDERATION);
            if (configInstStatic != null) {
                configInstStatic.addListener(
                    new WSFederationMetaServiceListener());
            } 
        } catch (ConfigurationException ce) {
            debug.error(
                "WSFederationMetaManager.static: Unable to add " +
                "ConfigurationListener for WSFederationCOT service.",
                ce);
            throw new ExceptionInInitializerError(ce);
        }
        try {
            cotmStatic = new CircleOfTrustManager();
        } catch (COTException se) {
            debug.error("WSFederationMetaManager constructor:", se);
            throw new ExceptionInInitializerError(se);
        }
    }

    /*
     * Constructor.
     * @exception WSFederationMetaException if an instance cannot be 
     *    instantiated.
     */
    public WSFederationMetaManager() throws WSFederationMetaException {
        configInst = configInstStatic;
        cotm = cotmStatic;
        if ((configInst == null) || (cotm == null)) {
            throw new WSFederationMetaException("nullConfig", null);
        }
    }

    /*
     * Constructor.
     * @param callerToken sesion token for the caller.
     * @exception WSFederationMetaException if an instance cannot be 
     *    instantiated.
     */
    public WSFederationMetaManager(Object callerToken) 
        throws WSFederationMetaException
    {
        try {
            configInst = ConfigurationManager.getConfigurationInstance(
                WSFEDERATION, callerToken);
            cotm = new CircleOfTrustManager(callerToken);
            if ((configInst == null) || (cotm == null)) {
                throw new WSFederationMetaException("nullConfig", null);
            }
        } catch (ConfigurationException ce) {
            throw new WSFederationMetaException(ce);
        } catch (COTException cex) {
            throw new WSFederationMetaException(cex);
        }
        callerSession = callerToken;
    }

    /**
     * Returns the standard metadata federation element under the realm.
     * 
     * @param realm The realm under which the federation resides.
     * @param entityId ID of the federation to be retrieved.
     * @return <code>FederationElement</code> for the entity or null if
     *         not found.
     * @throws WSFederationMetaException if unable to retrieve the entity 
     * descriptor.
     */
    public FederationElement getEntityDescriptor(String realm, 
        String entityId) 
        throws WSFederationMetaException {
        if (entityId == null) {
            return null;
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { entityId, realm  };

        FederationElement federation = null;
        if (callerSession == null) {
            federation = WSFederationMetaCache.getFederation(realm, entityId);
            if (federation != null) {
                LogUtil.access(Level.FINE,
                           LogUtil.GOT_FEDERATION,
                           objs,
                           null);
                return federation;
            }
        }

        try {
            Map attrs = configInst.getConfiguration(realm, entityId);
            if (attrs == null) {
                return null;
            }
            Set values = (Set)attrs.get(ATTR_METADATA);
            if (values == null || values.isEmpty()) {
                return null;
            }

            String value = (String)values.iterator().next();

            Object obj = WSFederationMetaUtils.convertStringToJAXB(value);
            if (obj instanceof FederationElement) {
                federation = (FederationElement)obj;
                WSFederationMetaCache.putFederation(realm, entityId,
                                                   federation);
                LogUtil.access(Level.FINE,
                               LogUtil.GOT_FEDERATION,
                               objs,
                               null);
                return federation;
            }

            debug.error("WSFederationMetaManager.getFederation: " +
                        "invalid descriptor");
            LogUtil.error(Level.INFO,
                          LogUtil.GOT_INVALID_ENTITY_DESCRIPTOR,
                          objs,
                          null);
            throw new WSFederationMetaException("invalid_descriptor", objs);
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.getFederation:", e);
            String[] data = {  e.getMessage(), entityId, realm  };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ENTITY_DESCRIPTOR,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("WSFederationMetaManager.getFederation:", jaxbe);
            LogUtil.error(Level.INFO,
                          LogUtil.GOT_INVALID_ENTITY_DESCRIPTOR,
                          objs,
                          null);
            throw new WSFederationMetaException("invalid_descriptor", objs);
        }
    }

    /**
     * Sets the standard metadata entity descriptor under the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param federation Federation object.
     * @throws WSFederationMetaException if unable to set the entity descriptor.
     */
    public void setFederation(String realm, FederationElement federation) 
        throws WSFederationMetaException {

        String federationId = federation.getFederationID();
        if (federationId == null) {
            federationId = WSFederationConstants.DEFAULT_FEDERATION_ID;
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { federationId, realm };
        try {
            Map attrs = 
                WSFederationMetaUtils.convertJAXBToAttrMap(ATTR_METADATA,
                federation);
            Map oldAttrs = configInst.getConfiguration(realm, federationId);
            oldAttrs.put(ATTR_METADATA, attrs.get(ATTR_METADATA));
            configInst.setConfiguration(realm, federationId, oldAttrs);
            LogUtil.access(Level.INFO,
                           LogUtil.SET_ENTITY_DESCRIPTOR,
                           objs,
                           null);
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.setFederation:", e);
            String[] data = { e.getMessage(), federationId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_SET_ENTITY_DESCRIPTOR,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("WSFederationMetaManager.setFederation:", jaxbe);
            LogUtil.error(Level.INFO,
                          LogUtil.SET_INVALID_ENTITY_DESCRIPTOR,
                          objs,
                          null);
            throw new WSFederationMetaException("invalid_descriptor", objs);
        }
    } 

    /**
     * Creates the standard metadata entity descriptor under the realm.
     * 
     * @param realm The realm under which the entity descriptor will be
     *              created.
     * @param federation The standard entity descriptor object to be created.
     * @throws WSFederationMetaException if unable to create the entity 
     *         descriptor.
     */
    public void createFederation(String realm, 
        FederationElement federation)
        throws WSFederationMetaException {

        String federationId = federation.getFederationID();
        if (federationId == null) {
            federationId = WSFederationConstants.DEFAULT_FEDERATION_ID;
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { federationId, realm };
        try {
            Map attrs = 
                WSFederationMetaUtils.convertJAXBToAttrMap(ATTR_METADATA,
                federation);
            configInst.createConfiguration(realm, federationId, attrs);
            LogUtil.access(Level.INFO,
                           LogUtil.ENTITY_DESCRIPTOR_CREATED,
                           objs,
                           null);
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.createFederation:", e);
            String[] data = { e.getMessage(), federationId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_CREATE_ENTITY_DESCRIPTOR,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("WSFederationMetaManager.createFederation:", jaxbe);
            LogUtil.error(Level.INFO,
                          LogUtil.CREATE_INVALID_ENTITY_DESCRIPTOR,
                          objs,
                          null);
            throw new WSFederationMetaException("invalid_descriptor", objs);
        }
    } 

    /**
     * Deletes the standard metadata entity descriptor under the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param federationId The ID of the entity for whom the standard entity 
     *                 descriptor will be deleted.
     * @throws WSFederationMetaException if unable to delete the entity 
     * descriptor.
     */
    public void deleteFederation(String realm, String federationId) 
        throws WSFederationMetaException {

        if (federationId == null) {
            return;
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { federationId, realm };
        try {
            // Remove the entity from cot              
            IDPSSOConfigElement idpconfig = getIDPSSOConfig(realm,
                                                         federationId);
            if (idpconfig !=null) {
                removeFromCircleOfTrust(idpconfig, realm, federationId); 
            }   
            
            SPSSOConfigElement spconfig = getSPSSOConfig(realm,
                                                        federationId);
            if (spconfig != null) { 
                removeFromCircleOfTrust(spconfig, realm, federationId); 
            }   
            // end of remove entity from cot
            configInst.deleteConfiguration(realm, federationId, null);
            LogUtil.access(Level.INFO,
                           LogUtil.ENTITY_DESCRIPTOR_DELETED,
                           objs,
                           null);
            WSFederationMetaCache.putFederation(realm, federationId, null);
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.deleteFederation:", e);
            String[] data = { e.getMessage(), federationId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_DELETE_ENTITY_DESCRIPTOR,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        }
    } 

    /**
     * Returns extended entity configuration under the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param federationId ID of the entity to be retrieved.
     * @return <code>FederationConfigElement</code> object for the entity or 
     * null if not found.
     * @throws WSFederationMetaException if unable to retrieve the entity
     *                            configuration.
     */
    public FederationConfigElement getEntityConfig(String realm, 
        String federationId)
        throws WSFederationMetaException {

        if (federationId == null) {
            return null;
        }
        if (realm == null) {
            realm = "/";
        }
        String[] objs = { federationId, realm };

        FederationConfigElement config = null;
        if (callerSession == null) {
            config = WSFederationMetaCache.getEntityConfig(realm, federationId);
            if (config != null) {
                LogUtil.access(Level.FINE,
                           LogUtil.GOT_ENTITY_CONFIG,
                           objs,
                           null);
                return config;
            }
        }

        try {
            Map attrs = configInst.getConfiguration(realm, federationId);
            if (attrs == null) {
                return null;
            }
            Set values = (Set)attrs.get(ATTR_ENTITY_CONFIG);
            if (values == null || values.isEmpty()) {
                return null;
            }

            String value = (String)values.iterator().next();

            Object obj = WSFederationMetaUtils.convertStringToJAXB(value);

            if (obj instanceof FederationConfigElement) {
                config = (FederationConfigElement)obj;
                WSFederationMetaCache.putEntityConfig(
                    realm, federationId, config);
                LogUtil.access(Level.FINE,
                               LogUtil.GOT_ENTITY_CONFIG,
                               objs,
                               null);
                return config;
            }

            debug.error("WSFederationMetaManager.getEntityConfig: " +
                        "invalid config");
            LogUtil.error(Level.INFO,
                          LogUtil.GOT_INVALID_ENTITY_CONFIG,
                          objs,
                          null);
            throw new WSFederationMetaException("invalid_config", objs);
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.getEntityConfig:", e);
            String[] data = { e.getMessage(), federationId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ENTITY_CONFIG,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("WSFederationMetaManager.getEntityConfig:", jaxbe);
            LogUtil.error(Level.INFO,
                          LogUtil.GOT_INVALID_ENTITY_CONFIG,
                          objs,
                          null);
            throw new WSFederationMetaException("invalid_config", objs);
        }
    }

    /**
     * Returns first service provider's SSO configuration in an entity under
     * the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param federationId ID of the entity to be retrieved.
     * @return <code>SPSSOConfigElement</code> for the entity or null if not
     *         found.
     * @throws WSFederationMetaException if unable to retrieve the first service
     *                            provider's SSO configuration.
     */
    public SPSSOConfigElement getSPSSOConfig(String realm, 
        String federationId)
        throws WSFederationMetaException {

        FederationConfigElement eConfig = getEntityConfig(realm, federationId);
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof SPSSOConfigElement) {
                return (SPSSOConfigElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first identity provider's SSO configuration in an entity under
     * the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param federationId ID of the entity to be retrieved.
     * @return <code>IDPSSOConfigElement</code> for the entity or null if not
     *         found.
     * @throws WSFederationMetaException if unable to retrieve the first 
     * identity provider's SSO configuration.
     */
    public IDPSSOConfigElement getIDPSSOConfig(String realm, 
        String federationId)
        throws WSFederationMetaException {
        FederationConfigElement eConfig = getEntityConfig(realm, federationId);
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof IDPSSOConfigElement) {
                return (IDPSSOConfigElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first identity provider's SSO configuration in an entity under
     * the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param federationId ID of the entity to be retrieved.
     * @return <code>BaseConfigElement</code> for the entity or null if not
     *         found.
     * @throws WSFederationMetaException if unable to retrieve the first 
     * identity provider's SSO configuration.
     */
    public BaseConfigType getBaseConfig(String realm, 
        String federationId)
        throws WSFederationMetaException {
        FederationConfigElement eConfig = getEntityConfig(realm, federationId);
        if (eConfig == null) {
            return null;
        }

        return (BaseConfigType)eConfig.getIDPSSOConfigOrSPSSOConfig().get(0);
    }
    
    /**
     * Sets the extended entity configuration under the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param config The extended entity configuration object to be set.
     * @throws WSFederationMetaException if unable to set the entity 
     * configuration.
     */
    public void setEntityConfig(String realm, 
        FederationConfigElement config)
        throws WSFederationMetaException {

        String federationId = config.getFederationID();
        if (federationId == null) {
            debug.error("WSFederationMetaManager.setEntityConfig: " +
                        "entity ID is null");
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                          LogUtil.NO_ENTITY_ID_SET_ENTITY_CONFIG,
                          data,
                          null);
            throw new WSFederationMetaException("empty_entityid", null);
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { federationId, realm };
        try {
            Map attrs = 
                WSFederationMetaUtils.convertJAXBToAttrMap(ATTR_ENTITY_CONFIG,
                config);
            Map oldAttrs = configInst.getConfiguration(realm, federationId);
            oldAttrs.put(ATTR_ENTITY_CONFIG, attrs.get(ATTR_ENTITY_CONFIG));
            configInst.setConfiguration(realm, federationId, oldAttrs);
            LogUtil.access(Level.INFO,
                           LogUtil.SET_ENTITY_CONFIG,
                           objs,
                           null);
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.setEntityConfig:", e);
            String[] data = { e.getMessage(), federationId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_SET_ENTITY_CONFIG,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("WSFederationMetaManager.setEntityConfig:", jaxbe);
            LogUtil.error(Level.INFO,
                          LogUtil.SET_INVALID_ENTITY_CONFIG,
                          objs,
                          null);
            throw new WSFederationMetaException("invalid_config", objs);
        }
    }

    /**
     * Creates the extended entity configuration under the realm.
     * 
     * @param realm The realm under which the entity configuration will be
     * created.
     * @param config The extended entity configuration object to be created.
     * @throws WSFederationMetaException if unable to create the entity 
     * configuration.
     */
    public void createEntityConfig(String realm, 
        FederationConfigElement config)
        throws WSFederationMetaException {

        String federationId = config.getFederationID();
        if (federationId == null) {
            debug.error("WSFederationMetaManager.createEntityConfig: " +
                        "entity ID is null");
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                          LogUtil.NO_ENTITY_ID_CREATE_ENTITY_CONFIG,
                          data,
                          null);
            throw new WSFederationMetaException("empty_entityid", null);
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { federationId, realm };
        try {
            Map attrs = 
                WSFederationMetaUtils.convertJAXBToAttrMap(ATTR_ENTITY_CONFIG,
                config);
            Map oldAttrs = configInst.getConfiguration(realm, federationId);
            if (oldAttrs == null) {
                LogUtil.error(Level.INFO,
                              LogUtil.NO_ENTITY_DESCRIPTOR_CREATE_ENTITY_CONFIG,
                              objs,
                              null);
                throw new WSFederationMetaException(
                    "entity_descriptor_not_exist", objs);
            }
            Set oldValues = (Set)oldAttrs.get(ATTR_ENTITY_CONFIG);
            if (oldValues != null && !oldValues.isEmpty() ) {
                LogUtil.error(Level.INFO,
                              LogUtil.ENTITY_CONFIG_EXISTS,
                              objs,
                              null);
                throw new WSFederationMetaException("entity_config_exists", 
                    objs);
            }
            configInst.setConfiguration(realm, federationId, attrs);
            LogUtil.access(Level.INFO,
                           LogUtil.ENTITY_CONFIG_CREATED,
                           objs,
                           null);
            // Add the entity to cot              
            SPSSOConfigElement spconfig = getSPSSOConfig(realm,
                                                        federationId);
            if (spconfig != null) {                                        
                addToCircleOfTrust(spconfig, realm, federationId); 
            }
            IDPSSOConfigElement idpconfig = getIDPSSOConfig(realm,
                                                         federationId);
            if (idpconfig !=null) {
                addToCircleOfTrust(idpconfig, realm, federationId); 
            }                                         
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.createEntityConfig:", e);
            String[] data = { e.getMessage(), federationId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_CREATE_ENTITY_CONFIG,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("WSFederationMetaManager.createEntityConfig:", jaxbe);
            LogUtil.error(Level.INFO,
                          LogUtil.CREATE_INVALID_ENTITY_CONFIG,
                          objs,
                          null);
            throw new WSFederationMetaException("invalid_config", objs);
        }
    }
    
    private void addToCircleOfTrust(BaseConfigType config, String realm,
                 String federationId) {
        try {
            if (config != null) {
                Map attr = WSFederationMetaUtils.getAttributes(config);
                List cotAttr = (List) attr.get(SAML2Constants.COT_LIST);
                List cotList = new ArrayList(cotAttr); 
                if ((cotList != null) && !cotList.isEmpty()) {
                    for (Iterator iter = cotList.iterator(); 
                        iter.hasNext();) {
                        cotm.addCircleOfTrustMember(realm, (String)iter.next(),
                                COTConstants.WS_FED, federationId); 
                     }               
                 }
             }
         } catch (Exception e) {
             debug.error("WSFederationMetaManager.addToCircleOfTrust:" +
                   "Error while adding entity" + federationId + "to COT.",e);
         }
    }

    /**
     * Deletes the extended entity configuration under the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param federationId The ID of the entity for whom the extended entity
     *                 configuration will be deleted.
     * @throws WSFederationMetaException if unable to delete the entity 
     * descriptor.
     */
    public void deleteEntityConfig(String realm, String federationId)
        throws WSFederationMetaException {

        if (federationId == null) {
            return;
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { federationId, realm };
        try {
            Map oldAttrs = configInst.getConfiguration(realm, federationId);
            Set oldValues = (Set)oldAttrs.get(ATTR_ENTITY_CONFIG);
            if (oldValues == null || oldValues.isEmpty() ) {
                LogUtil.error(Level.INFO,
                              LogUtil.NO_ENTITY_DESCRIPTOR_DELETE_ENTITY_CONFIG,
                              objs,
                              null);
                throw new WSFederationMetaException("entity_config_not_exist", 
                    objs);
            }

            // Remove the entity from cot              
            IDPSSOConfigElement idpconfig = getIDPSSOConfig(realm,
                                                federationId);
            if (idpconfig !=null) {
                removeFromCircleOfTrust(idpconfig, realm, federationId); 
            }   
            
            SPSSOConfigElement spconfig = getSPSSOConfig(realm,
                                                        federationId);
            if (spconfig != null) { 
                removeFromCircleOfTrust(spconfig, realm, federationId); 
            }
            
            Set attr = new HashSet();
            attr.add(ATTR_ENTITY_CONFIG);
            configInst.deleteConfiguration(realm, federationId, attr);
            LogUtil.access(Level.INFO,
                           LogUtil.ENTITY_CONFIG_DELETED,
                           objs,
                           null);
            WSFederationMetaCache.putEntityConfig(realm, federationId, null);
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.deleteEntityConfig:", e);
            String[] data = { e.getMessage(), federationId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_DELETE_ENTITY_CONFIG,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        }
    } 
    
    /**
     * Checks that the provided metaAliases are valid for a new hosted entity in the specified realm.
     * Will verify that the metaAliases do not already exist in the realm and that no duplicates are provided.
     *
     * @param realm The realm in which we are validating the metaAliases.
     * @param newMetaAliases  values we are using to create the new metaAliases.
     * @throws WSFederationMetaException if duplicate values found.
     */
    public void validateMetaAliasForNewEntity(String realm, List<String> newMetaAliases) throws WSFederationMetaException {

        if (null != newMetaAliases && !newMetaAliases.isEmpty()) {
            if (newMetaAliases.size() > 1) {
                Set checkForDuplicates = new HashSet<String>(newMetaAliases);
                if (checkForDuplicates.size() < newMetaAliases.size()) {
                    debug.error("WSFederationMetaManager.validateMetaAliasForNewEntity:Duplicate"
                            + " metaAlias values provided in list:\n" + newMetaAliases);
                    String[] data = { newMetaAliases.toString() };
                    throw new WSFederationMetaException("meta_alias_duplicate", data);
                }
            }
            List<String> allRealmMetaAliaes = getAllHostedMetaAliasesByRealm(realm);
            // only check if we have existing aliases
            if (!allRealmMetaAliaes.isEmpty()) {
                List<String> duplicateMetaAliases = new ArrayList<String>();
                for (String metaAlias : newMetaAliases) {
                    if (allRealmMetaAliaes.contains(metaAlias)) {
                        duplicateMetaAliases.add(metaAlias);
                    }
                }
                if (!duplicateMetaAliases.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String value : duplicateMetaAliases) {
                        sb.append(value);
                        sb.append("\t");
                    }
                    debug.error("WSFederationMetaManager.validateMetaAliasForNewEntity: metaAliases " + sb.toString()
                            + " already exists in the realm: " + realm);
                    String[] data = { sb.toString(), realm };
                    throw new WSFederationMetaException("meta_alias_exists", data);
                }
            }
        }
    }

    /**
     * Returns all the hosted entity metaAliases for a realm.
     *
     * @param realm The given realm.
     * @return all the hosted entity metaAliases for a realm or an empty arrayList if not found.
     * @throws WSFederationMetaException  if unable to retrieve the entity ids.
     */
    public List<String> getAllHostedMetaAliasesByRealm(String realm) throws WSFederationMetaException {

        List<String> metaAliases = new ArrayList<String>();
        try {
            Set<String> entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds == null || entityIds.isEmpty()) {
                return metaAliases;
            }
            for (String entityId : entityIds) {
                FederationConfigElement config = getEntityConfig(realm, entityId);
                if (config == null || !config.isHosted()) {
                    continue;
                }
                List<BaseConfigType> configList = config.getIDPSSOConfigOrSPSSOConfig();
                for (BaseConfigType bConfigType : configList) {
                    String curMetaAlias = bConfigType.getMetaAlias();
                    if (curMetaAlias != null && !curMetaAlias.isEmpty()) {
                        metaAliases.add(curMetaAlias);
                    }
                }
            }
        } catch (ConfigurationException e) {
            debug.error(
                    "WSFederationMetaManager.getAllHostedMetaAliasesByRealm: Error getting "
                            + "hostedMetaAliases for realm: "+ realm, e);
            throw new WSFederationMetaException(e);
        }
        return metaAliases;
    }

    private void removeFromCircleOfTrust(BaseConfigType config, 
        String realm, String federationId) {
        try {
            if (config != null) {
                Map attr = WSFederationMetaUtils.getAttributes(config);
                List cotAttr = (List) attr.get(SAML2Constants.COT_LIST);
                List cotList = new ArrayList(cotAttr) ; 
                if ((cotList != null) && !cotList.isEmpty()) {
                    for (Iterator iter = cotList.iterator(); 
                        iter.hasNext();) {
                        String a = ((String) iter.next()).trim();
                        if (a.length() > 0) {
                            cotm.removeCircleOfTrustMember(realm, 
                                       a, COTConstants.WS_FED,federationId);
                        }
                     }               
                 }
             }
         } catch (Exception e) {
             debug.error("WSFederationMetaManager.removeFromCircleOfTrust:" +
                "Error while removing entity" + federationId + "from COT.",
                e);
         }
    }

    /**
     * Returns all hosted entities under the realm.
     * 
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public List<String> getAllHostedEntities(String realm)
        throws WSFederationMetaException {

        List<String> hostedEntityIds = new ArrayList<String>();
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds != null && !entityIds.isEmpty()) {
                for(Iterator iter = entityIds.iterator(); iter.hasNext();) {
                    String federationId = (String)iter.next();
                    FederationConfigElement config =
                                    getEntityConfig(realm, federationId);
                    if (config != null && config.isHosted()) {
                        hostedEntityIds.add(federationId);
                    }
                }
            }
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.getAllHostedEntities:", e);
            String[] data = { e.getMessage(), realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ALL_HOSTED_ENTITIES,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        }
        String[] objs = { realm };
        LogUtil.access(Level.FINE,
                       LogUtil.GOT_ALL_HOSTED_ENTITIES,
                       objs,
                       null);
        return hostedEntityIds;
    }

    /**
     * Returns all hosted service provider entities under the realm.
     * 
     * @param realm The realm under which the hosted service provider entities
     *              reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public List getAllHostedServiceProviderEntities(String realm)
        throws WSFederationMetaException {

        List<String> hostedSPEntityIds = new ArrayList<String>();
        List<String> hostedEntityIds = getAllHostedEntities(realm);

        for(String federationId : hostedEntityIds) {
            if (getSPSSOConfig(realm, federationId) != null) {
                hostedSPEntityIds.add(federationId);
            }
        }
        return hostedSPEntityIds;
    }

    /**
     * Returns all hosted identity provider entities under the realm.
     * 
     * @param realm The realm under which the hosted identity provider entities
     *              reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public List<String> getAllHostedIdentityProviderEntities(
        String realm)
        throws WSFederationMetaException {

        List<String> hostedIDPEntityIds = new ArrayList<String>();
        List<String> hostedEntityIds = getAllHostedEntities(realm);

        for(String federationId : hostedEntityIds) {
            if (getIDPSSOConfig(realm, federationId) != null) {
                hostedIDPEntityIds.add(federationId);
            }
        }
        return hostedIDPEntityIds;
    }

    /**
     * Returns all remote entities under the realm.
     * 
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public List<String> getAllRemoteEntities(String realm)
        throws WSFederationMetaException {

        List<String> remoteEntityIds = new ArrayList();
        String[] objs = { realm };
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds != null && !entityIds.isEmpty()) {
                for(Iterator iter = entityIds.iterator(); iter.hasNext();) {
                    String federationId = (String)iter.next();
                    FederationConfigElement config =
                                    getEntityConfig(realm, federationId);
                    if (config == null || !config.isHosted()) {
                        remoteEntityIds.add(federationId);
                    }
                }
            }
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.getAllRemoteEntities:", e);
            String[] data = { e.getMessage(), realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ALL_REMOTE_ENTITIES,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        }
        LogUtil.access(Level.FINE,
                       LogUtil.GOT_ALL_REMOTE_ENTITIES,
                       objs,
                       null);
        return remoteEntityIds;
    }

    /**
     * Returns all remote service provider entities under the realm.
     * 
     * @param realm The realm under which the remote service provider entities
     *              reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public List<String> getAllRemoteServiceProviderEntities(String realm)
        throws WSFederationMetaException {

        List<String> remoteSPEntityIds = new ArrayList();
        List<String> remoteEntityIds = getAllRemoteEntities(realm);

        for(String federationId : remoteEntityIds) {
            if (getSPSSOConfig(realm, federationId) != null) {
                remoteSPEntityIds.add(federationId);
            }
        }
        return remoteSPEntityIds;
    }

    /**
     * Returns all remote identity provider entities under the realm.
     * 
     * @param realm The realm under which the remote identity provider entities
     *              reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public List<String> getAllRemoteIdentityProviderEntities(
        String realm)
        throws WSFederationMetaException {

        List<String> remoteIDPEntityIds = new ArrayList();
        List<String> remoteEntityIds = getAllRemoteEntities(realm);

        for(String federationId : remoteEntityIds) {
            if (getIDPSSOConfig(realm, federationId) != null) {
                remoteIDPEntityIds.add(federationId);
            }
        }
        return remoteIDPEntityIds;
    }

    /**
     * Returns entity ID associated with the metaAlias.
     * 
     * @param metaAlias The metaAlias.
     * @return entity ID associated with the metaAlias or null if not found.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public String getEntityByMetaAlias(String metaAlias)
        throws WSFederationMetaException {

        String realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds == null || entityIds.isEmpty()) {
                return null;
            }

            for (Iterator iter = entityIds.iterator(); iter.hasNext();) {
                String federationId = (String)iter.next();
                FederationConfigElement config = getEntityConfig(realm, 
                    federationId);
                if (config == null) {
                    continue;
                }
                List list =
                    config.getIDPSSOConfigOrSPSSOConfig();
                for(Iterator iter2 = list.iterator(); iter2.hasNext();) {
                    BaseConfigType bConfig = (BaseConfigType)iter2.next();
                    String cMetaAlias = bConfig.getMetaAlias();
                    if (cMetaAlias != null && cMetaAlias.equals(metaAlias)) {
                        return federationId;
                    }
                }
            }
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.getEntityByMetaAlias:", e);
            throw new WSFederationMetaException(e);
        }

        return null;
    }

    /**
     * Returns entity ID associated with the token issuer name.
     * 
     * @param issuer Token issuer name.
     * @return entity ID associated with the metaAlias or null if not found.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public String getEntityByTokenIssuerName(String realm,String issuer)
        throws WSFederationMetaException {
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds == null || entityIds.isEmpty()) {
                return null;
            }

            for (Iterator iter = entityIds.iterator(); iter.hasNext();) {
                String federationId = (String)iter.next();
                FederationElement fed = getEntityDescriptor(realm, 
                    federationId);
                if ( issuer.equals(getTokenIssuerName(fed)))
                {
                    return federationId;
                }
            }
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.getEntityByMetaAlias:", e);
            throw new WSFederationMetaException(e);
        }

        return null;
    }

    /**
     * Returns role of an entity based on its metaAlias.
     * 
     * @param metaAlias Meta alias of the entity.
     * @return role of an entity either <code>SAML2Constants.IDP_ROLE</code>; or
     *         <code>SAML2Constants.SP_ROLE</code> or 
     *         <code>SAML2Constants.UNKNOWN_ROLE</code>
     * @throws WSFederationMetaException if there are issues in getting the 
     * entity profile from the meta alias.
     */
    public String getRoleByMetaAlias(String metaAlias)
        throws WSFederationMetaException {
        String role = SAML2Constants.UNKNOWN_ROLE;
        
        String federationId = getEntityByMetaAlias(metaAlias);
        
        if (federationId != null) {
            String realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
            IDPSSOConfigElement idpConfig = getIDPSSOConfig(realm, 
                federationId);
            SPSSOConfigElement spConfig = getSPSSOConfig(realm, federationId);
            
            if (idpConfig == null) {
                String m = spConfig.getMetaAlias();
                if ((m != null) && m.equals(metaAlias)) {
                    role = SAML2Constants.SP_ROLE;
                }
            } else if (spConfig == null) {
                String m = idpConfig.getMetaAlias();
                if ((m != null) && m.equals(metaAlias)) {
                    role = SAML2Constants.IDP_ROLE;
                }
            } else {
                //Assuming that sp and idp cannot have the same metaAlias
                String m = spConfig.getMetaAlias();
                if ((m != null) && m.equals(metaAlias)) {
                    role = SAML2Constants.SP_ROLE;
                } else {
                    m = idpConfig.getMetaAlias();
                    if ((m != null) && m.equals(metaAlias)) {
                        role = SAML2Constants.IDP_ROLE;
                    }
                }
            }
        }
        
        return role;        
    }
    
    /**
     * Returns metaAliases of all hosted identity providers under the realm.
     * 
     * @param realm The realm under which the identity provider metaAliases
     *              reside.
     * @return a <code>List</code> of metaAliases <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve meta aliases.
     */
    public List<String> getAllHostedIdentityProviderMetaAliases(
        String realm)
        throws WSFederationMetaException {

        List<String> metaAliases = new ArrayList<String>();
        IDPSSOConfigElement idpConfig = null;
        List<String> hostedEntityIds 
            = getAllHostedIdentityProviderEntities(realm);
        for(String federationId : hostedEntityIds) {
            if ((idpConfig = getIDPSSOConfig(realm, federationId)) != null) {
                metaAliases.add(idpConfig.getMetaAlias());
            }
        }
        return metaAliases;
    }

    /**
     * Returns metaAliases of all hosted service providers under the realm.
     * 
     * @param realm The realm under which the service provider metaAliases
     *              reside.
     * @return a <code>List</code> of metaAliases <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve meta aliases.
     */
    public List<String> getAllHostedServiceProviderMetaAliases(
        String realm)
        throws WSFederationMetaException {

        List<String> metaAliases = new ArrayList<String>();
        SPSSOConfigElement spConfig = null;
        List<String> hostedEntityIds = getAllHostedServiceProviderEntities(
            realm);
        for(String federationId : hostedEntityIds) {
            if ((spConfig = getSPSSOConfig(realm, federationId)) != null) {
                metaAliases.add(spConfig.getMetaAlias());
            }
        }
        return metaAliases;
    }

    /**
     * Determines whether two entities are in the same circle of trust
     * under the realm.
     * 
     * @param realm The realm under which the entity resides.
     * @param federationId The ID of the entity
     * @param trustedEntityId The ID of the entity
     * @throws WSFederationMetaException if unable to determine the trusted
     *         relationship.
     */
    public boolean isTrustedProvider(String realm, String federationId, 
                                         String trustedEntityId) 
        throws WSFederationMetaException {
       
        boolean result=false;  
        SPSSOConfigElement spconfig = getSPSSOConfig(realm,
                                                     federationId);
        if (spconfig != null) {        
            result = isSameCircleOfTrust(spconfig, realm,
                                         trustedEntityId); 
        }
        if (result) {
            return true;
        } 
        IDPSSOConfigElement idpconfig = getIDPSSOConfig(realm,
                                                        federationId);
        if (idpconfig !=null) {
            return (isSameCircleOfTrust(idpconfig, realm,
                        trustedEntityId)); 
        }
        return false;   
    }    
   
    private boolean isSameCircleOfTrust(BaseConfigType config, 
        String realm, String trustedEntityId) {
        try {
            if (config != null) {
                Map<String,List<String>> attr 
                    = WSFederationMetaUtils.getAttributes(config);
                List<String> cotList = attr.get(SAML2Constants.COT_LIST);
                if ((cotList != null) && !cotList.isEmpty()) {
                    for (String cot : cotList) {
                       if (cotm.isInCircleOfTrust(realm, cot, 
                            COTConstants.WS_FED, trustedEntityId)) {
                            return true;
                        } 
                     }               
                 }
             } 
             return false;
         } catch (Exception e) {
             debug.error("WSFederationMetaManager.isSameCircleOfTrust: Error" +
                   " while determining two entities are in the same COT.");
             return false; 
        }
    }
    
    /**
     * Returns all entities under the realm.
     * 
     * @param realm The realm under which the entities reside.
     * @return a <code>Set</code> of entity ID <code>String</code>.
     * @throws WSFederationMetaException if unable to retrieve the entity ids.
     */
    public Set<String> getAllEntities(String realm)
        throws WSFederationMetaException {

        Set<String> ret = new HashSet<String>();
        String[] objs = { realm };
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds != null && !entityIds.isEmpty()) {
                ret.addAll(entityIds); 
            } 
        } catch (ConfigurationException e) {
            debug.error("WSFederationMetaManager.getAllEntities:", e);
            String[] data = { e.getMessage(), realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ALL_ENTITIES,
                          data,
                          null);
            throw new WSFederationMetaException(e);
        }
        LogUtil.access(Level.FINE,
                       LogUtil.GOT_ALL_ENTITIES,
                       objs,
                       null);
        return ret;
    }

    /**
     * Returns the value of the <code>&lt;TokenIssuerEndpoint&gt;</code> element
     * for the given entity.
     * @param fed The standard metadata for the entity.
     * @return the value of the <code>&lt;TokenIssuerEndpoint&gt;</code> element
     */
    public String getTokenIssuerEndpoint(FederationElement fed)
    {
        // Just return first TokenIssuerEndpoint in the Federation
        for ( Object o: fed.getAny() )
        {
            if ( o instanceof TokenIssuerEndpointElement )
            {
                return ((TokenIssuerEndpointElement)o).getAddress().getValue();
            }
        }
        
        return null;
    }

    /**
     * Returns the value of the <code>&lt;TokenIssuerName&gt;</code> element
     * for the given entity.
     * @param fed The standard metadata for the entity.
     * @return the value of the <code>&lt;TokenIssuerName&gt;</code> element
     */
    public String getTokenIssuerName(FederationElement fed)
    {
        // Just return first TokenIssuerName in the Federation
        for ( Object o: fed.getAny() )
        {
            if ( o instanceof TokenIssuerNameElement )
            {
                return ((TokenIssuerNameElement)o).getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Returns the value of the <code>&lt;TokenSigningCertificate&gt;</code> 
     * element for the given entity.
     * @param fed The standard metadata for the entity.
     * @return byte array containing the decoded value of the 
     * <code>&lt;TokenSigningCertificate&gt;</code> element
     */
    public byte[] getTokenSigningCertificate(FederationElement fed)
    {
        // Just return first TokenIssuerName in the Federation
        for ( Object o: fed.getAny() )
        {
            if ( o instanceof TokenSigningKeyInfoElement )
            {
                SecurityTokenReferenceType str =
                    ((TokenSigningKeyInfoElement)o).getSecurityTokenReference();
                for ( Object o1: str.getAny() )
                {
                    if ( o1 instanceof X509DataType )
                    {
                        for ( Object o2: 
                            ((X509DataType)o1).
                            getX509IssuerSerialOrX509SKIOrX509SubjectName())
                        {
                            if ( o2 instanceof X509Certificate )
                            {
                                return ((X509Certificate)o2).getValue();
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Returns the value of the <code>&lt;UriNamedClaimTypesOffered&gt;</code> 
     * element for the given entity.
     * @param fed The standard metadata for the entity.
     * @return <code>UriNamedClaimTypesOfferedElement</code> containing the
     * offered claim types.
     * <code>&lt;UriNamedClaimTypesOffered&gt;</code> element
     */
    public UriNamedClaimTypesOfferedElement getUriNamedClaimTypesOffered(
        FederationElement fed)
    {
        // Just return first TokenIssuerName in the Federation
        for ( Object o: fed.getAny() )
        {
            if ( o instanceof UriNamedClaimTypesOfferedElement )
            {
                return (UriNamedClaimTypesOfferedElement)o;
            }
        }
        
        return null;
    }
}
