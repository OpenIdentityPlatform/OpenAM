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
 * $Id: SAML2MetaManager.java,v 1.18 2009/10/28 23:58:58 exu Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.sun.identity.saml2.meta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.COTException;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.AffiliationConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeAuthorityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeQueryConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.AuthnAuthorityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLPDPConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLAuthzDecisionQueryConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.AttributeAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.AuthnAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.jaxb.metadataextquery.AttributeQueryDescriptorElement;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.shared.debug.Debug;

/**
 * The <code>SAML2MetaManager</code> provides methods to manage both the 
 * standard entity descriptor and the extended entity configuration.
 */
public class SAML2MetaManager {
    private static final String ATTR_METADATA = "sun-fm-saml2-metadata";
    private static final String ATTR_ENTITY_CONFIG =
        "sun-fm-saml2-entityconfig";
    private static final String SUBCONFIG_ID = "EntityDescriptor";
    private static final int SUBCONFIG_PRIORITY = 0;

    private static Debug debug = SAML2MetaUtils.debug;
    private static CircleOfTrustManager cotmStatic;
    private static ConfigurationInstance configInstStatic;
    private static final String SAML2 = "SAML2";

    private CircleOfTrustManager cotm;
    private ConfigurationInstance configInst;
    private Object callerSession = null;

    /**
     * Constant used to identify meta alias.
     */
    public static final String NAME_META_ALIAS_IN_URI = "metaAlias";

    static {
        try {
            configInstStatic = ConfigurationManager.getConfigurationInstance(
                SAML2);
        } catch (ConfigurationException ce) {
            debug.error("SAML2MetaManager constructor:", ce);
        }
        if (configInstStatic != null) {
            try {
                configInstStatic.addListener(new SAML2MetaServiceListener());
            } catch (ConfigurationException ce) {
                debug.error(
                    "SAML2MetaManager.static: Unable to add " +
                    "ConfigurationListener for SAML2COT service.",
                    ce);
            }
        }
        try {
            cotmStatic = new CircleOfTrustManager();
        } catch (COTException se) {
            debug.error("SAML2MetaManager constructor:", se);
        }
    }

    /**
     * Constructor for <code>SAML2MetaManager</code>.
     * @throws SAML2MetaException if unable to construct
     *                            <code>SAML2MetaManager</code>
     */
    public SAML2MetaManager() throws SAML2MetaException {
        configInst = configInstStatic;
        if (configInst == null) {
            throw new SAML2MetaException("null_config", null);
        }
        cotm = cotmStatic;
    }

    /**
     * Constructor for <code>SAML2MetaManager</code>.
     * @param callerToken session token for the caller.
     * @throws SAML2MetaException if unable to construct
     *                            <code>SAML2MetaManager</code>
     */
    public SAML2MetaManager(Object callerToken) throws SAML2MetaException {
        try {
            configInst = ConfigurationManager.getConfigurationInstance(
                SAML2, callerToken);
            cotm = new CircleOfTrustManager(callerToken);
        } catch (ConfigurationException ex) {
            throw new SAML2MetaException("null_config", null);
        } catch (COTException cx) {
            throw new SAML2MetaException("null_config", null);
        }
        callerSession = callerToken;
    }

    /**
     * Returns the standard metadata entity descriptor under the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return <code>EntityDescriptorElement</code> for the entity or null if
     *         not found. 
     * @throws SAML2MetaException if unable to retrieve the entity descriptor. 
     */
    public EntityDescriptorElement getEntityDescriptor(
        String realm,
        String entityId
    ) throws SAML2MetaException {
        if (entityId == null) {
            return null;
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { entityId, realm };

        EntityDescriptorElement descriptor = null;
        if (callerSession == null) {
             descriptor = SAML2MetaCache.getEntityDescriptor(realm, entityId);
            if (descriptor != null) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2MetaManager.getEntityDescriptor: got "
                        + "descriptor from SAML2MetaCache " + entityId);
                }
                LogUtil.access(Level.FINE, LogUtil.GOT_ENTITY_DESCRIPTOR,
                    objs, null);
                return descriptor;
            }
        }

        try {
            Map attrs = configInst.getConfiguration(realm, entityId);
            if (attrs == null) {
                return null;
            }
            Set values = (Set)attrs.get(ATTR_METADATA);
            if ((values == null) || values.isEmpty()) {
                return null;
            }

            String value = (String)values.iterator().next();
            Object obj = SAML2MetaUtils.convertStringToJAXB(value);
            if (obj instanceof EntityDescriptorElement) {
                descriptor = (EntityDescriptorElement)obj;
                SAML2MetaCache.putEntityDescriptor(realm, entityId, descriptor);
                if (debug.messageEnabled()) {
                    debug.message("SAML2MetaManager.getEntityDescriptor: got "
                        + "descriptor from SMS " + entityId);
                }
                LogUtil.access(Level.FINE, LogUtil.GOT_ENTITY_DESCRIPTOR,
                    objs, null);
                return descriptor;
            }

            debug.error(
                "SAML2MetaManager.getEntityDescriptor: invalid descriptor");
            LogUtil.error(Level.INFO, LogUtil.GOT_INVALID_ENTITY_DESCRIPTOR,
                objs, null);
            throw new SAML2MetaException("invalid_descriptor", objs);
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.getEntityDescriptor", e);
            String[] data = { e.getMessage(), entityId, realm };
            LogUtil.error(Level.INFO,
                LogUtil.CONFIG_ERROR_GET_ENTITY_DESCRIPTOR, data, null);
            throw new SAML2MetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("SAML2MetaManager.getEntityDescriptor", jaxbe);
            LogUtil.error(Level.INFO, LogUtil.GOT_INVALID_ENTITY_DESCRIPTOR,
                objs, null);
            throw new SAML2MetaException("invalid_descriptor", objs);
        }
    }

    /**
     * Returns first service provider's SSO descriptor in an entity under the
     * realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return <code>SPSSODescriptorElement</code> for the entity or null if
     *         not found. 
     * @throws SAML2MetaException if unable to retrieve the first service 
     *         provider's SSO descriptor.
     */
    public SPSSODescriptorElement getSPSSODescriptor(
        String realm,
        String entityId) 
        throws SAML2MetaException {
        EntityDescriptorElement eDescriptor = getEntityDescriptor(
            realm, entityId);
        return SAML2MetaUtils.getSPSSODescriptor(eDescriptor);
    }

 
    /**
     * Returns attribute authority descriptor in an entity under the
     * realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return an <code>AttributeAuthorityDescriptorElement</code> object for
     *     the entity or null if not found. 
     * @throws SAML2MetaException if unable to retrieve attribute authority
     *     descriptor.
     */
    public AttributeAuthorityDescriptorElement
        getAttributeAuthorityDescriptor(String realm, String entityId)
        throws SAML2MetaException {
        EntityDescriptorElement eDescriptor = getEntityDescriptor(
            realm, entityId);
        return SAML2MetaUtils.getAttributeAuthorityDescriptor(eDescriptor);
    }

    /**
     * Returns attribute query descriptor in an entity under the
     * realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return an <code>AttributeQueryDescriptorElement</code> object for
     *     the entity or null if not found. 
     * @throws SAML2MetaException if unable to retrieve attribute query
     *     descriptor.
     */
    public AttributeQueryDescriptorElement
        getAttributeQueryDescriptor(String realm, String entityId)
        throws SAML2MetaException {
        EntityDescriptorElement eDescriptor = getEntityDescriptor(
            realm, entityId);
        return SAML2MetaUtils.getAttributeQueryDescriptor(eDescriptor);
    }

    /**
     * Returns authentication authority descriptor in an entity under the
     * realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return an <code>AuthnAuthorityDescriptorElement</code> object for
     *     the entity or null if not found. 
     * @throws SAML2MetaException if unable to retrieve authentication
     *     authority descriptor.
     */
    public AuthnAuthorityDescriptorElement getAuthnAuthorityDescriptor(
        String realm, String entityId) throws SAML2MetaException {

        EntityDescriptorElement eDescriptor = getEntityDescriptor(
            realm, entityId);
        return SAML2MetaUtils.getAuthnAuthorityDescriptor(eDescriptor);
    }

    /**
     * Returns first policy decision point descriptor in an entity under the
     * realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return policy decision point descriptor.
     * @throws SAML2MetaException if unable to retrieve the descriptor.
     */
    public XACMLPDPDescriptorElement getPolicyDecisionPointDescriptor(
        String realm, String entityId
    ) throws SAML2MetaException {
        EntityDescriptorElement eDescriptor = getEntityDescriptor(
            realm, entityId);
        return SAML2MetaUtils.getPolicyDecisionPointDescriptor(eDescriptor);
    }

    /**
     * Returns first policy enforcement point descriptor in an entity under the
     * realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return policy enforcement point descriptor.
     * @throws SAML2MetaException if unable to retrieve the descriptor.
     */
    public XACMLAuthzDecisionQueryDescriptorElement 
        getPolicyEnforcementPointDescriptor(
        String realm, String entityId
    ) throws SAML2MetaException {
        EntityDescriptorElement eDescriptor = getEntityDescriptor(
            realm, entityId);
        return SAML2MetaUtils.getPolicyEnforcementPointDescriptor(eDescriptor);
    }
    
    /**
     * Returns first identity provider's SSO descriptor in an entity under the
     * realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return <code>IDPSSODescriptorElement</code> for the entity or null if
     *         not found. 
     * @throws SAML2MetaException if unable to retrieve the first identity 
     *                            provider's SSO descriptor.
     */
    public IDPSSODescriptorElement getIDPSSODescriptor(String realm,
                                                       String entityId) 
        throws SAML2MetaException {

        EntityDescriptorElement eDescriptor = getEntityDescriptor(realm,
                                                                  entityId);
        return SAML2MetaUtils.getIDPSSODescriptor(eDescriptor);
    }

    /**
     * Returns affiliation descriptor in an entity under the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return <code>AffiliationDescriptorType</code> for the entity or
     *     null if not found. 
     * @throws SAML2MetaException if unable to retrieve the affiliation 
     *     descriptor.
     */
    public AffiliationDescriptorType getAffiliationDescriptor(
        String realm,
        String entityId) 
        throws SAML2MetaException {

        EntityDescriptorElement eDescriptor = getEntityDescriptor(realm,
            entityId);

        return (eDescriptor == null ? null :
            eDescriptor.getAffiliationDescriptor());
    }

    /**
     * Sets the standard metadata entity descriptor under the realm.
     * @param realm The realm under which the entity resides.
     * @param descriptor The standard entity descriptor object to be set. 
     * @throws SAML2MetaException if unable to set the entity descriptor.
     */
    public void setEntityDescriptor(
        String realm,
        EntityDescriptorElement descriptor) 
        throws SAML2MetaException {

        String entityId = descriptor.getEntityID();
        if (entityId == null) {
            debug.error(
                "SAML2MetaManager.setEntityDescriptor: entity ID is null");
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                LogUtil.NO_ENTITY_ID_SET_ENTITY_DESCRIPTOR, data, null);
            throw new SAML2MetaException("empty_entityid", null);
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { entityId, realm };
        try {
            Map attrs = SAML2MetaUtils.convertJAXBToAttrMap(
                ATTR_METADATA, descriptor);
            Map oldAttrs = configInst.getConfiguration(realm, entityId);
            oldAttrs.put(ATTR_METADATA, attrs.get(ATTR_METADATA));
            configInst.setConfiguration(realm, entityId, oldAttrs);
            SAML2MetaCache.putEntityDescriptor(realm, entityId, descriptor);
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaManager.setEntityDescriptor: saved "
                    + "entity descriptor for " + entityId);
            }
            LogUtil.access(Level.INFO, LogUtil.SET_ENTITY_DESCRIPTOR,
                objs, null);
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.setEntityDescriptor:", e);
            String[] data = { e.getMessage(), entityId, realm };
            LogUtil.error(Level.INFO,
                LogUtil.CONFIG_ERROR_SET_ENTITY_DESCRIPTOR, data, null);
            throw new SAML2MetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("SAML2MetaManager.setEntityDescriptor:", jaxbe);
            LogUtil.error(Level.INFO, LogUtil.SET_INVALID_ENTITY_DESCRIPTOR,
                objs, null);
            throw new SAML2MetaException("invalid_descriptor", objs);
        }
    } 

    /**
     * Creates the standard metadata entity descriptor under the realm.
     * @param realm The realm under which the entity descriptor will be
     *        created.
     * @param descriptor The standard entity descriptor object to be created. 
     * @throws SAML2MetaException if unable to create the entity descriptor.
     */
    public void createEntityDescriptor(
        String realm,
        EntityDescriptorElement descriptor
    ) throws SAML2MetaException {
        debug.message("SAML2MetaManager.createEntityDescriptor: called.");
        createEntity(realm, descriptor, null);
    }

    /**
     * Creates the standard and extended metadata under the realm.
     * @param realm The realm under which the entity descriptor will be
     *        created.
     * @param descriptor The standard entity descriptor object to be created. 
     * @param config The extended entity config object to be created.
     * @throws SAML2MetaException if unable to create the entity.
     */
    public void createEntity(
        String realm,
        EntityDescriptorElement descriptor,
        EntityConfigElement config
    ) throws SAML2MetaException {
        debug.message("SAML2MetaManager.createEntity: called.");
        if ((descriptor == null) && (config == null)) {
            debug.error(
                "SAML2metaManager.createEntity: no meta to import.");
            return;
        }
        String entityId = null;
        if (descriptor != null) {
           entityId = descriptor.getEntityID();
        } else {
           entityId = config.getEntityID();
        }

        if (realm == null) {
            realm = "/";
        }

        if (entityId == null) {
            debug.error(
                "SAML2MetaManager.createEntity: entity ID is null");
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                LogUtil.NO_ENTITY_ID_CREATE_ENTITY_DESCRIPTOR,
                data, null);
            throw new SAML2MetaException("empty_entityid", null);
        }

        if (debug.messageEnabled()) {
            debug.message("SAML2MetaManager.createEntity: realm=" 
                + realm + ", entityId=" + entityId);
        }
        String[] objs = { entityId, realm };

        try {
            EntityDescriptorElement oldDescriptor = null;
            EntityConfigElement oldConfig = null;
            boolean isCreate = true;
            Map newAttrs = null;
            Map oldAttrs = configInst.getConfiguration(realm, entityId);
            if (oldAttrs != null) {
                // get the entity descriptor if any
                Set values = (Set)oldAttrs.get(ATTR_METADATA);
                if ((values != null) && !values.isEmpty()) {
                    String value = (String)values.iterator().next();
                    Object obj = SAML2MetaUtils.convertStringToJAXB(value);
                    if (obj instanceof EntityDescriptorElement) {
                        oldDescriptor = (EntityDescriptorElement)obj;
                        if (debug.messageEnabled()) {
                            debug.message("SAML2MetaManager.createEntity: "
                                + "got descriptor from SMS " + entityId);
                        }
                    }
                }
                // get the entity config if any
                values = (Set)oldAttrs.get(ATTR_ENTITY_CONFIG);
                if ((values != null) && !values.isEmpty()) {
                    String value = (String)values.iterator().next();
                    Object obj = SAML2MetaUtils.convertStringToJAXB(value);
                    if (obj instanceof EntityConfigElement) {
                        oldConfig = (EntityConfigElement)obj;
                        if (debug.messageEnabled()) {
                            debug.message("SAML2MetaManager.createEntity: "
                                + "got entity config from SMS " + entityId);
                        }
                    }
                }
            }
            if (oldDescriptor != null) {
                if (descriptor != null) {
                    List currentRoles = oldDescriptor.
                        getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
                    Set currentRolesTypes = getEntityRolesTypes(currentRoles);
                    List newRoles = descriptor.
                        getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
                    for (Iterator i = newRoles.iterator(); i.hasNext(); ) {
                        Object role = i.next();
                        if (currentRolesTypes.contains(
                            role.getClass().getName())) 
                        {
                            debug.error("SAML2MetaManager.createEntity: current"
                                + " descriptor contains role " 
                                + role.getClass().getName() 
                                + " already");
                            String[] data = {entityId, realm };
                            LogUtil.error(Level.INFO, 
                                LogUtil.SET_ENTITY_DESCRIPTOR, data, null);
                            String[] param = {entityId};
                            throw new SAML2MetaException("role_already_exists", 
                                param);
                        }
                        currentRoles.add(role);
                    }
                    Map attrs = SAML2MetaUtils.convertJAXBToAttrMap(
                        ATTR_METADATA, oldDescriptor);
                    oldAttrs.put(ATTR_METADATA, attrs.get(ATTR_METADATA));
                    isCreate = false;
                }
            } else {
                if (descriptor != null) {
                    newAttrs = SAML2MetaUtils.convertJAXBToAttrMap(
                        ATTR_METADATA, descriptor);
                }
            }

            if (config != null) {
                if ((oldDescriptor == null) && (descriptor == null)) {
                    debug.error("SAML2MetaManager.createEntity: entity "
                        + "descriptor is null: " + entityId);
                    LogUtil.error(Level.INFO,
                        LogUtil.NO_ENTITY_DESCRIPTOR_CREATE_ENTITY_CONFIG, objs,
                        null);
                    throw new SAML2MetaException("entity_descriptor_not_exist",
                        objs);
                }
                if (oldConfig != null) {
                    List currentRoles = oldConfig.
                        getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                    Set currentRolesTypes = getEntityRolesTypes(currentRoles);
                    List newRoles = config.
                        getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                    for (Iterator i = newRoles.iterator(); i.hasNext(); ) {
                        Object role = i.next();
                        if (currentRolesTypes.contains(
                            role.getClass().getName())) 
                        {
                            debug.error("SAML2MetaManager.createEntity: current"
                                + " entity config contains role " 
                                + role.getClass().getName() 
                                + " already");
                            String[] data = {entityId, realm };
                            LogUtil.error(Level.INFO, 
                                LogUtil.SET_ENTITY_CONFIG, data, null);
                            String[] param = {entityId};
                            throw new SAML2MetaException("role_already_exists", 
                                param);
                        }
                        currentRoles.add(role);
                    }
                    Map attrs = SAML2MetaUtils.convertJAXBToAttrMap(
                        ATTR_ENTITY_CONFIG, oldConfig);
                    oldAttrs.put(ATTR_ENTITY_CONFIG, 
                        attrs.get(ATTR_ENTITY_CONFIG));
                    isCreate = false;
                } else {
                    Map attrs = SAML2MetaUtils.convertJAXBToAttrMap(
                        ATTR_ENTITY_CONFIG, config);
                    if (oldAttrs != null) {
                        oldAttrs.put(ATTR_ENTITY_CONFIG,
                            attrs.get(ATTR_ENTITY_CONFIG));
                        isCreate = false;
                    } else if (newAttrs != null) {
                        newAttrs.put(ATTR_ENTITY_CONFIG, 
                            attrs.get(ATTR_ENTITY_CONFIG));
                    }
                }
            }

            if (isCreate) {
                configInst.createConfiguration(realm, entityId, newAttrs);
                if (descriptor != null) {
                    SAML2MetaCache.putEntityDescriptor(
                        realm, entityId, descriptor);
                    LogUtil.access(Level.INFO,
                        LogUtil.ENTITY_DESCRIPTOR_CREATED, objs, null);
                } else if (config != null) {
                    LogUtil.access(Level.INFO,
                        LogUtil.ENTITY_CONFIG_CREATED, objs, null);
                }
                // Add the entity to cot
                if (config != null) {
                    SAML2MetaCache.putEntityConfig(realm, entityId, config);
                    addToCircleOfTrust(realm, entityId, config);
                }
            } else {
                configInst.setConfiguration(realm, entityId, oldAttrs);
                if (descriptor != null) {
                    LogUtil.access(Level.INFO,
                        LogUtil.SET_ENTITY_DESCRIPTOR, objs, null);
                    SAML2MetaCache.putEntityDescriptor(
                        realm, entityId, oldDescriptor);
                } else if (config != null) {
                    LogUtil.access(Level.INFO,
                        LogUtil.SET_ENTITY_CONFIG, objs, null);
                }
                if (oldConfig != null) {
                    SAML2MetaCache.putEntityConfig(realm, entityId, oldConfig);
                } else if (config != null) {
                    SAML2MetaCache.putEntityConfig(realm, entityId, config);
                    addToCircleOfTrust(realm, entityId, config);
                }
            }
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.createEntity:", e);
            String[] data = { e.getMessage(), entityId, realm };
            LogUtil.error(Level.INFO,
                LogUtil.CONFIG_ERROR_CREATE_ENTITY_DESCRIPTOR, data, null);
            throw new SAML2MetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("SAML2MetaManager.createEntity:", jaxbe);
            LogUtil.error(Level.INFO,
                LogUtil.CREATE_INVALID_ENTITY_DESCRIPTOR, objs, null);
            throw new SAML2MetaException("invalid_descriptor", objs);
        }
    } 

    private static Set getEntityRolesTypes(Collection roles) {
        Set types = new HashSet();
        for (Iterator i = roles.iterator(); i.hasNext(); ) {
            Object o = i.next();
            types.add(o.getClass().getName());
        }
        return types;
    }


    /**
     * Deletes the standard metadata entity descriptor under the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId The ID of the entity for whom the standard entity 
     *                 descriptor will be deleted. 
     * @throws SAML2MetaException if unable to delete the entity descriptor.
     */
    public void deleteEntityDescriptor(String realm, String entityId) 
        throws SAML2MetaException {

        if (entityId == null) {
            return;
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { entityId, realm };
        try {
            // Remove the entity from cot
            removeFromCircleOfTrust(realm, entityId); 
            
            // end of remove entity from cot
            configInst.deleteConfiguration(realm, entityId, null);
            LogUtil.access(Level.INFO,
                           LogUtil.ENTITY_DESCRIPTOR_DELETED,
                           objs,
                           null);
            SAML2MetaCache.putEntityDescriptor(realm, entityId, null);
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.deleteEntityDescriptor:", e);
            String[] data = { e.getMessage(), entityId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_DELETE_ENTITY_DESCRIPTOR,
                          data,
                          null);
            throw new SAML2MetaException(e);
        }
    } 

    /**
     * Returns extended entity configuration under the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>EntityConfigElement</code> object for the entity or null
     *         if not found.
     * @throws SAML2MetaException if unable to retrieve the entity
     *                            configuration.
     */
    public EntityConfigElement getEntityConfig(String realm, String entityId)
        throws SAML2MetaException {
        if (entityId == null) {
            return null;
        }
        if (realm == null) {
            realm = "/";
        }
        String[] objs = { entityId, realm };

        EntityConfigElement config = null;
        if (callerSession == null) {
            config = SAML2MetaCache.getEntityConfig(realm, entityId);
            if (config != null) {
                if (debug.messageEnabled()) {
                    debug.message("SAML2MetaManager.getEntityConfig: got entity"
                        + " config from SAML2MetaCache: " + entityId);
                }
                LogUtil.access(Level.FINE,
                           LogUtil.GOT_ENTITY_CONFIG,
                           objs,
                           null);
                return config;
            }
        }

        try {
            Map attrs = configInst.getConfiguration(realm, entityId);
            if (attrs == null) {
                return null;
            }
            Set values = (Set)attrs.get(ATTR_ENTITY_CONFIG);
            if (values == null || values.isEmpty()) {
                return null;
            }

            String value = (String)values.iterator().next();

            Object obj = SAML2MetaUtils.convertStringToJAXB(value);

            if (obj instanceof EntityConfigElement) {
                config = (EntityConfigElement)obj;
                if (debug.messageEnabled()) {
                    debug.message("SAML2MetaManager.getEntityConfig: got "
                        + "entity config from SMS: " + entityId);
                } 
                SAML2MetaCache.putEntityConfig(
                    realm, entityId, config);
                LogUtil.access(Level.FINE,
                               LogUtil.GOT_ENTITY_CONFIG,
                               objs,
                               null);
                return config;
            }

            debug.error("SAML2MetaManager.getEntityConfig: invalid config");
            LogUtil.error(Level.INFO,
                          LogUtil.GOT_INVALID_ENTITY_CONFIG,
                          objs,
                          null);
            throw new SAML2MetaException("invalid_config", objs);
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.getEntityConfig:", e);
            String[] data = { e.getMessage(), entityId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ENTITY_CONFIG,
                          data,
                          null);
            throw new SAML2MetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("SAML2MetaManager.getEntityConfig:", jaxbe);
            LogUtil.error(Level.INFO,
                          LogUtil.GOT_INVALID_ENTITY_CONFIG,
                          objs,
                          null);
            throw new SAML2MetaException("invalid_config", objs);
        }
    }

    /**
     * Returns first service provider's SSO configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>SPSSOConfigElement</code> for the entity or null if not
     *         found.
     * @throws SAML2MetaException if unable to retrieve the first service
     *                            provider's SSO configuration.
     */
    public SPSSOConfigElement getSPSSOConfig(String realm, String entityId)
        throws SAML2MetaException {

        EntityConfigElement eConfig = getEntityConfig(realm, entityId);
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof SPSSOConfigElement) {
                return (SPSSOConfigElement)obj;
            }
        }

        return null;
    }
    
    /**
     * Returns first policy decision point configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return policy decision point configuration or null if it is not found.
     * @throws SAML2MetaException if unable to retrieve the configuration.
     */
    public XACMLPDPConfigElement getPolicyDecisionPointConfig(
        String realm, String entityId
    ) throws SAML2MetaException {
        XACMLPDPConfigElement elm = null;
        EntityConfigElement eConfig = getEntityConfig(realm, entityId);
        
        if (eConfig != null) {
            List list = 
                eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
            for (Iterator i = list.iterator(); i.hasNext() && (elm == null);) {
                Object obj = i.next();
                if (obj instanceof XACMLPDPConfigElement) {
                    elm = (XACMLPDPConfigElement)obj;
                }
            }
        }
        return elm;
    }
    
    /**
     * Returns first policy enforcement point configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return policy decision point configuration or null if it is not found.
     * @throws SAML2MetaException if unable to retrieve the configuration.
     */
    public XACMLAuthzDecisionQueryConfigElement getPolicyEnforcementPointConfig(
        String realm, String entityId
    ) throws SAML2MetaException {
        XACMLAuthzDecisionQueryConfigElement elm = null;
        EntityConfigElement eConfig = getEntityConfig(realm, entityId);
        
        if (eConfig != null) {
            List list = 
                eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
            for (Iterator i = list.iterator(); i.hasNext() && (elm == null);) {
                Object obj = i.next();
                if (obj instanceof XACMLAuthzDecisionQueryConfigElement) {
                    elm = (XACMLAuthzDecisionQueryConfigElement)obj;
                }
            }
        }
        return elm;
    }

    /**
     * Returns first identity provider's SSO configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>IDPSSOConfigElement</code> for the entity or null if not
     *         found.
     * @throws SAML2MetaException if unable to retrieve the first identity
     *                            provider's SSO configuration.
     */
    public IDPSSOConfigElement getIDPSSOConfig(String realm, String entityId)
        throws SAML2MetaException {
        EntityConfigElement eConfig = getEntityConfig(realm, entityId);
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof IDPSSOConfigElement) {
                return (IDPSSOConfigElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first attribute authority configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>AttributeAuthorityConfigElement</code> for the entity or
     *     null if not found.
     * @throws SAML2MetaException if unable to retrieve the first attribute
     *     authority configuration.
     */
    public AttributeAuthorityConfigElement getAttributeAuthorityConfig(
        String realm, String entityId) throws SAML2MetaException {

        EntityConfigElement eConfig = getEntityConfig(realm, entityId);
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AttributeAuthorityConfigElement) {
                return (AttributeAuthorityConfigElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first attribute query configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>AttributeQueryConfigElement</code> for the entity or
     *     null if not found.
     * @throws SAML2MetaException if unable to retrieve the first attribute
     *     query configuration.
     */
    public AttributeQueryConfigElement getAttributeQueryConfig(
        String realm, String entityId) throws SAML2MetaException {

        EntityConfigElement eConfig = getEntityConfig(realm, entityId);
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AttributeQueryConfigElement) {
                return (AttributeQueryConfigElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns first authentication authority configuration in an entity under
     * the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>AuthnAuthorityConfigElement</code> for the entity or
     *     null if not found.
     * @throws SAML2MetaException if unable to retrieve the first authentication
     *     authority configuration.
     */
    public AuthnAuthorityConfigElement getAuthnAuthorityConfig(
        String realm, String entityId) throws SAML2MetaException {

        EntityConfigElement eConfig = getEntityConfig(realm, entityId);
        if (eConfig == null) {
            return null;
        }

        List list =
            eConfig.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for(Iterator iter = list.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof AuthnAuthorityConfigElement) {
                return (AuthnAuthorityConfigElement)obj;
            }
        }

        return null;
    }

    /**
     * Returns affiliation configuration in an entity under the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>AffiliationConfigElement</code> for the entity or
     *     null if not found.
     * @throws SAML2MetaException if unable to retrieve the affiliation
     *     configuration.
     */
    public AffiliationConfigElement getAffiliationConfig(
        String realm, String entityId) throws SAML2MetaException {

        EntityConfigElement eConfig = getEntityConfig(realm, entityId);
        if (eConfig == null) {
            return null;
        }

        return (AffiliationConfigElement)eConfig.getAffiliationConfig();
    }

    /**
     * Sets the extended entity configuration under the realm.
     * @param realm The realm under which the entity resides.
     * @param config The extended entity configuration object to be set.
     * @throws SAML2MetaException if unable to set the entity configuration.
     */
    public void setEntityConfig(String realm, EntityConfigElement config)
        throws SAML2MetaException {

        String entityId = config.getEntityID();
        if (entityId == null) {
            debug.error("SAML2MetaManager.setEntityConfig: " +
                        "entity ID is null");
            String[] data = { realm };
            LogUtil.error(Level.INFO,
                          LogUtil.NO_ENTITY_ID_SET_ENTITY_CONFIG,
                          data,
                          null);
            throw new SAML2MetaException("empty_entityid", null);
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { entityId, realm };
        try {
            Map attrs = SAML2MetaUtils.convertJAXBToAttrMap(ATTR_ENTITY_CONFIG,
                                                            config);
            Map oldAttrs = configInst.getConfiguration(realm, entityId);
            oldAttrs.put(ATTR_ENTITY_CONFIG, attrs.get(ATTR_ENTITY_CONFIG));
            configInst.setConfiguration(realm, entityId, oldAttrs);
            SAML2MetaCache.putEntityConfig(realm, entityId, config);
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaManager.setEntityConfig: saved "
                    + "entity config for " + entityId);
            }
            LogUtil.access(Level.INFO,
                           LogUtil.SET_ENTITY_CONFIG,
                           objs,
                           null);
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.setEntityConfig:", e);
            String[] data = { e.getMessage(), entityId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_SET_ENTITY_CONFIG,
                          data,
                          null);
            throw new SAML2MetaException(e);
        } catch (JAXBException jaxbe) {
            debug.error("SAML2MetaManager.setEntityConfig:", jaxbe);
            LogUtil.error(Level.INFO,
                          LogUtil.SET_INVALID_ENTITY_CONFIG,
                          objs,
                          null);
            throw new SAML2MetaException("invalid_config", objs);
        }
    }

    /**
     * Creates the extended entity configuration under the realm.
     * @param realm The realm under which the entity configuration will be
     * created.
     * @param config The extended entity configuration object to be created. 
     * @throws SAML2MetaException if unable to create the entity configuration.
     */
    public void createEntityConfig(String realm, EntityConfigElement config)
        throws SAML2MetaException {
        if (debug.messageEnabled()) {
            debug.message("SAML2MetaManager.creatEntityConfig: called.");
        }
        createEntity(realm, null, config);
    }
    
    private void addToCircleOfTrust(
        String realm, String entityId, EntityConfigElement eConfig) 
    {
        try {
            if (eConfig != null) {
                List elist = eConfig.
                    getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                // use first one to add the entity to COT
                BaseConfigType config = (BaseConfigType)elist.iterator().next();
                Map attr = SAML2MetaUtils.getAttributes(config);
                List cotAttr = (List) attr.get(SAML2Constants.COT_LIST);
                List cotList = new ArrayList(cotAttr); 
                if ((cotList != null) && !cotList.isEmpty()) {
                    for (Iterator iter = cotList.iterator(); 
                        iter.hasNext();) {
                        String cotName = ((String) iter.next()).trim();
                        if ((cotName != null) && (!cotName.equals(""))) { 
                            cotm.addCircleOfTrustMember(realm,
                            cotName, COTConstants.SAML2, entityId, false);
                        }
                     }               
                 }
             }
         } catch (Exception e) {
             debug.error("SAML2MetaManager.addToCircleOfTrust:" +
                   "Error while adding entity" + entityId + "to COT.",e);
         }
    }

    /**
     * Deletes the extended entity configuration under the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId The ID of the entity for whom the extended entity
     *                 configuration will be deleted.
     * @throws SAML2MetaException if unable to delete the entity descriptor.
     */
    public void deleteEntityConfig(String realm, String entityId)
        throws SAML2MetaException {

        if (entityId == null) {
            return;
        }
        if (realm == null) {
            realm = "/";
        }

        String[] objs = { entityId, realm };
        try {
            Map oldAttrs = configInst.getConfiguration(realm, entityId);
            Set oldValues = (Set)oldAttrs.get(ATTR_ENTITY_CONFIG);
            if (oldValues == null || oldValues.isEmpty() ) {
                LogUtil.error(Level.INFO,
                              LogUtil.NO_ENTITY_DESCRIPTOR_DELETE_ENTITY_CONFIG,
                              objs,
                              null);
                throw new SAML2MetaException("entity_config_not_exist", objs);
            }

            // Remove the entity from cot              
            removeFromCircleOfTrust(realm, entityId); 
            
            Set attr = new HashSet();
            attr.add(ATTR_ENTITY_CONFIG);
            configInst.deleteConfiguration(realm, entityId, attr);
            LogUtil.access(Level.INFO,
                           LogUtil.ENTITY_CONFIG_DELETED,
                           objs,
                           null);
            SAML2MetaCache.putEntityConfig(realm, entityId, null);
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.deleteEntityConfig:", e);
            String[] data = { e.getMessage(), entityId, realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_DELETE_ENTITY_CONFIG,
                          data,
                          null);
            throw new SAML2MetaException(e);
        }
    } 

    private void removeFromCircleOfTrust(String realm, String entityId) {
        try {
            EntityConfigElement eConfig = getEntityConfig(realm, entityId);
            boolean isAffiliation = false;
            if (getAffiliationDescriptor(realm, entityId) != null) {
                isAffiliation = true;
            }
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaManager.removeFromCircleOfTrust is " 
                    + entityId + " in realm " + realm 
                    + " an affiliation? " + isAffiliation);
            }

            if (eConfig != null) {
                List elist = null; 
                if (isAffiliation) {
                    AffiliationConfigElement affiliationCfgElm =
                        getAffiliationConfig(realm, entityId);
                    elist = new ArrayList();
                    elist.add(affiliationCfgElm);
                } else {
                    elist = eConfig.
                        getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                }

                // use first one to delete the entity from COT
                BaseConfigType config = (BaseConfigType)elist.iterator().next();
                Map attr = SAML2MetaUtils.getAttributes(config);
                List cotAttr = (List) attr.get(SAML2Constants.COT_LIST);
                List cotList = new ArrayList(cotAttr);
                if ((cotList != null) && !cotList.isEmpty()) {
                    for (Iterator iter = cotList.iterator(); iter.hasNext();) {
                        String cotName = ((String) iter.next()).trim();
                        if ((cotName != null) && (!cotName.equals(""))) { 
                            cotm.removeCircleOfTrustMember(realm, 
                            cotName, COTConstants.SAML2, entityId, false);
                        } 
                    }               
                }
            }
        } catch (Exception e) {
            debug.error("SAML2MetaManager.removeFromCircleOfTrust:" +
                "Error while removing entity" + entityId + "from COT.",e);
        }
    }

    /**
     * Returns all hosted entities under the realm.
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllHostedEntities(String realm)
        throws SAML2MetaException {

        List hostedEntityIds = new ArrayList();
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds != null && !entityIds.isEmpty()) {
                for(Iterator iter = entityIds.iterator(); iter.hasNext();) {
                    String entityId = (String)iter.next();
                    EntityConfigElement config =
                                    getEntityConfig(realm, entityId);
                    if (config != null && config.isHosted()) {
                        hostedEntityIds.add(entityId);
                    }
                }
            }
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.getAllHostedEntities:", e);
            String[] data = { e.getMessage(), realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ALL_HOSTED_ENTITIES,
                          data,
                          null);
            throw new SAML2MetaException(e);
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
     * @param realm The realm under which the hosted service provider entities
     *              reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllHostedServiceProviderEntities(String realm)
        throws SAML2MetaException {

        List hostedSPEntityIds = new ArrayList();
        List hostedEntityIds = getAllHostedEntities(realm);

        for(Iterator iter = hostedEntityIds.iterator(); iter.hasNext();) {
            String entityId = (String)iter.next();
            if (getSPSSODescriptor(realm, entityId) != null) {
                hostedSPEntityIds.add(entityId);
            }
        }
        return hostedSPEntityIds;
    }

    /**
     * Returns all hosted policy decision point entities under the realm.
     *
     * @param realm The realm under which the hosted policy decision point 
     *        entities reside.
     * @return a list of entity ID.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllHostedPolicyDecisionPointEntities(String realm)
        throws SAML2MetaException {
        return getHostedPolicyDecisionPointEntities(realm, true);
    }
    
    /**
     * Returns all remote policy decision point entities under the realm.
     *
     * @param realm The realm under which the remote policy decision point 
     *        entities reside.
     * @return a list of entity ID.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllRemotePolicyDecisionPointEntities(String realm)
        throws SAML2MetaException {
        return getHostedPolicyDecisionPointEntities(realm, false);
    }
    
    private List getHostedPolicyDecisionPointEntities(
        String realm, 
        boolean hosted
    ) throws SAML2MetaException {
        List hostedPDPEntityIds = new ArrayList();
        List hostedEntityIds = (hosted) ? getAllHostedEntities(realm) :
            getAllRemoteEntities(realm);

        for(Iterator i = hostedEntityIds.iterator(); i.hasNext();) {
            String entityId = (String)i.next();
            if (getPolicyDecisionPointDescriptor(realm, entityId) != null) {
                hostedPDPEntityIds.add(entityId);
            }
        }
        return hostedPDPEntityIds;
    }
    
    /**
     * Returns all hosted policy enforcement point entities under the realm.
     *
     * @param realm The realm under which the hosted policy enforcement point 
     *        entities reside.
     * @return a list of entity ID.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllHostedPolicyEnforcementPointEntities(String realm)
        throws SAML2MetaException {
        return getAllPolicyEnforcementPointEntities(realm, true);
    }
    
    /**
     * Returns all remote policy enforcement point entities under the realm.
     *
     * @param realm The realm under which the remote policy enforcement point 
     *        entities reside.
     * @return a list of entity ID.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllRemotePolicyEnforcementPointEntities(String realm)
        throws SAML2MetaException {
        return getAllPolicyEnforcementPointEntities(realm, false);
    }
        
    private List getAllPolicyEnforcementPointEntities(
        String realm,
        boolean hosted
    ) throws SAML2MetaException {

        List hostedPEPEntityIds = new ArrayList();
        List hostedEntityIds = (hosted) ? getAllHostedEntities(realm) :
            getAllRemoteEntities(realm);

        for (Iterator i = hostedEntityIds.iterator(); i.hasNext();) {
            String entityId = (String)i.next();
            if (getPolicyEnforcementPointDescriptor(realm, entityId) != null) {
                hostedPEPEntityIds.add(entityId);
            }
        }
        return hostedPEPEntityIds;
    }
    
    /**
     * Returns all hosted identity provider entities under the realm.
     * @param realm The realm under which the hosted identity provider entities
     *              reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllHostedIdentityProviderEntities(String realm)
        throws SAML2MetaException {

        List hostedIDPEntityIds = new ArrayList();
        List hostedEntityIds = getAllHostedEntities(realm);

        for(Iterator iter = hostedEntityIds.iterator(); iter.hasNext();) {
            String entityId = (String)iter.next();
            if (getIDPSSODescriptor(realm, entityId) != null) {
                hostedIDPEntityIds.add(entityId);
            }
        }
        return hostedIDPEntityIds;
    }

    /**
     * Returns all remote entities under the realm.
     * @param realm The realm under which the hosted entities reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllRemoteEntities(String realm)
        throws SAML2MetaException {

        List remoteEntityIds = new ArrayList();
        String[] objs = { realm };
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds != null && !entityIds.isEmpty()) {
                for(Iterator iter = entityIds.iterator(); iter.hasNext();) {
                    String entityId = (String)iter.next();
                    EntityConfigElement config =
                                    getEntityConfig(realm, entityId);
                    if (config == null || !config.isHosted()) {
                        remoteEntityIds.add(entityId);
                    }
                }
            }
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.getAllRemoteEntities:", e);
            String[] data = { e.getMessage(), realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ALL_REMOTE_ENTITIES,
                          data,
                          null);
            throw new SAML2MetaException(e);
        }
        LogUtil.access(Level.FINE,
                       LogUtil.GOT_ALL_REMOTE_ENTITIES,
                       objs,
                       null);
        return remoteEntityIds;
    }

    /**
     * Returns all remote service provider entities under the realm.
     * @param realm The realm under which the remote service provider entities
     *              reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllRemoteServiceProviderEntities(String realm)
        throws SAML2MetaException {

        List remoteSPEntityIds = new ArrayList();
        List remoteEntityIds = getAllRemoteEntities(realm);

        for(Iterator iter = remoteEntityIds.iterator(); iter.hasNext();) {
            String entityId = (String)iter.next();
            if (getSPSSODescriptor(realm, entityId) != null) {
                remoteSPEntityIds.add(entityId);
            }
        }
        return remoteSPEntityIds;
    }

    /**
     * Returns all remote identity provider entities under the realm.
     * @param realm The realm under which the remote identity provider entities
     *              reside.
     * @return a <code>List</code> of entity ID <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public List getAllRemoteIdentityProviderEntities(String realm)
        throws SAML2MetaException {

        List remoteIDPEntityIds = new ArrayList();
        List remoteEntityIds = getAllRemoteEntities(realm);

        for(Iterator iter = remoteEntityIds.iterator(); iter.hasNext();) {
            String entityId = (String)iter.next();
            if (getIDPSSODescriptor(realm, entityId) != null) {
                remoteIDPEntityIds.add(entityId);
            }
        }
        return remoteIDPEntityIds;
    }

    /**
     * Returns entity ID associated with the metaAlias.
     * @param metaAlias The metaAlias.
     * @return entity ID associated with the metaAlias or null if not found.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public String getEntityByMetaAlias(String metaAlias)
        throws SAML2MetaException {

        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds == null || entityIds.isEmpty()) {
                return null;
            }

            for (Iterator iter = entityIds.iterator(); iter.hasNext();) {
                String entityId = (String)iter.next();
                EntityConfigElement config = getEntityConfig(realm, entityId);
                if ((config == null) || !config.isHosted()) {
                    continue;
                }
                List list =
                    config.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                for(Iterator iter2 = list.iterator(); iter2.hasNext();) {
                    BaseConfigType bConfig = (BaseConfigType)iter2.next();
                    String cMetaAlias = bConfig.getMetaAlias();
                    if (cMetaAlias != null && cMetaAlias.equals(metaAlias)) {
                        return entityId;
                    }
                }
            }
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.getEntityByMetaAlias:", e);
            throw new SAML2MetaException(e);
        }

        return null;
    }

    /**
     * Returns all the hosted entity metaAliases for a realm.
     *
     * @param realm The given realm.
     * @return all the hosted entity metaAliases for a realm or an empty arrayList if not found.
     * @throws SAML2MetaException  if unable to retrieve the entity ids.
     */
    public List<String> getAllHostedMetaAliasesByRealm(String realm) throws SAML2MetaException {

        List<String> metaAliases = new ArrayList<String>();
        try {
            Set<String> entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds == null || entityIds.isEmpty()) {
                return metaAliases;
            }
            for (String entityId : entityIds) {
                EntityConfigElement config = getEntityConfig(realm, entityId);
                if (config == null || !config.isHosted()) {
                    continue;
                }
                List<BaseConfigType> configList = config.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                for (BaseConfigType bConfigType : configList) {
                    String curMetaAlias = bConfigType.getMetaAlias();
                    if (curMetaAlias != null && !curMetaAlias.isEmpty()) {
                        metaAliases.add(curMetaAlias);
                    }
                }
            }
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.getAllHostedMetaAliasesByRealm:", e);
            throw new SAML2MetaException(e);
        }
        return metaAliases;
    }

    /**
     * Returns role of an entity based on its metaAlias.
     *
     * @param metaAlias Meta alias of the entity.
     * @return role of an entity either <code>SAML2Constants.IDP_ROLE</code>; or
     *         <code>SAML2Constants.SP_ROLE</code> or 
     *         <code>SAML2Constants.UNKNOWN_ROLE</code>
     * @throws SAML2MetaException if there are issues in getting the entity
     *         profile from the meta alias.
     */
    public String getRoleByMetaAlias(String metaAlias)
        throws SAML2MetaException {
        String role = SAML2Constants.UNKNOWN_ROLE;
        
        String entityId = getEntityByMetaAlias(metaAlias);
        
        if (entityId != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
            IDPSSOConfigElement idpConfig = getIDPSSOConfig(realm, entityId);
            SPSSOConfigElement spConfig = getSPSSOConfig(realm, entityId);
            XACMLPDPConfigElement pdpConfig = getPolicyDecisionPointConfig(
                realm, entityId);
            XACMLAuthzDecisionQueryConfigElement pepConfig = 
                getPolicyEnforcementPointConfig(realm, entityId);
            
            if (idpConfig != null) {
                String m = idpConfig.getMetaAlias();
                if ((m != null) && m.equals(metaAlias)) {
                    role = SAML2Constants.IDP_ROLE;
                }
            } else if (spConfig != null) {
                String m = spConfig.getMetaAlias();
                if ((m != null) && m.equals(metaAlias)) {
                    role = SAML2Constants.SP_ROLE;
                }
            } else if (pdpConfig != null) {
                String m = pdpConfig.getMetaAlias();
                if ((m != null) && m.equals(metaAlias)) {
                    role = SAML2Constants.PDP_ROLE;
                }
            } else if (pepConfig != null) {
                String m = pepConfig.getMetaAlias();
                if ((m != null) && m.equals(metaAlias)) {
                    role = SAML2Constants.PEP_ROLE;
                }
            }
        }
        
        return role;        
    }
    
    /**
     * Returns metaAliases of all hosted identity providers under the realm.
     * @param realm The realm under which the identity provider metaAliases
     *              reside.
     * @return a <code>List</code> of metaAliases <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve meta aliases.
     */
    public List getAllHostedIdentityProviderMetaAliases(String realm)
        throws SAML2MetaException {

        List metaAliases = new ArrayList();
        IDPSSOConfigElement idpConfig = null;
        List hostedEntityIds = getAllHostedIdentityProviderEntities(realm);
        for(Iterator iter = hostedEntityIds.iterator(); iter.hasNext();) {
            String entityId = (String)iter.next();
            if ((idpConfig = getIDPSSOConfig(realm, entityId)) != null) {
                metaAliases.add(idpConfig.getMetaAlias());
            
            }
        }
        return metaAliases;
    }

    /**
     * Returns metaAliases of all hosted service providers under the realm.
     * @param realm The realm under which the service provider metaAliases
     *              reside.
     * @return a <code>List</code> of metaAliases <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve meta aliases.
     */
    public List getAllHostedServiceProviderMetaAliases(String realm)
        throws SAML2MetaException {

        List metaAliases = new ArrayList();
        SPSSOConfigElement spConfig = null;
        List hostedEntityIds = getAllHostedServiceProviderEntities(realm);
        for(Iterator iter = hostedEntityIds.iterator(); iter.hasNext();) {
            String entityId = (String)iter.next();
            if ((spConfig = getSPSSOConfig(realm, entityId)) != null) {
                metaAliases.add(spConfig.getMetaAlias());
            
            }
        }
        return metaAliases;
    }
    
    /**
     * Returns meta aliases of all hosted policy decision point under the realm.
     * @param realm The realm under which the policy decision point resides.
     * @return list of meta aliases 
     * @throws SAML2MetaException if unable to retrieve meta aliases.
     */
    public List getAllHostedPolicyDecisionPointMetaAliases(String realm)
        throws SAML2MetaException {
        List metaAliases = new ArrayList();
        List hostedEntityIds = getAllHostedPolicyDecisionPointEntities(realm);
        
        for (Iterator i = hostedEntityIds.iterator(); i.hasNext();) {
            String entityId = (String)i.next();
            XACMLPDPConfigElement elm = getPolicyDecisionPointConfig(
                realm, entityId);
            if (elm != null) {
                metaAliases.add(elm.getMetaAlias());
            }
        }
        return metaAliases;
    }
    
    /**
     * Returns meta aliases of all hosted policy enforcement point under the 
     * realm.
     *
     * @param realm The realm under which the policy enforcement point resides.
     * @return list of meta aliases 
     * @throws SAML2MetaException if unable to retrieve meta aliases.
     */
    public List getAllHostedPolicyEnforcementPointMetaAliases(String realm)
        throws SAML2MetaException {
        List metaAliases = new ArrayList();
        List hostedEntityIds = getAllHostedPolicyEnforcementPointEntities(
            realm);
        
        for (Iterator i = hostedEntityIds.iterator(); i.hasNext();) {
            String entityId = (String)i.next();
            XACMLAuthzDecisionQueryConfigElement elm = 
                getPolicyEnforcementPointConfig(realm, entityId);
            if (elm != null) {
                metaAliases.add(elm.getMetaAlias());
            }
        }
        return metaAliases;
    }
    
    /**
     * Determines whether two entities are in the same circle of trust
     * under the realm.
     * @param realm The realm under which the entity resides.
     * @param entityId The ID of the entity
     * @param trustedEntityId The ID of the entity 
     * @throws SAML2MetaException if unable to determine the trusted
     *         relationship.
     */
    public boolean isTrustedProvider(String realm, String entityId, 
                                         String trustedEntityId) 
        throws SAML2MetaException {
       
        boolean result=false;  
        SPSSOConfigElement spconfig = getSPSSOConfig(realm,
                                                     entityId);
        if (spconfig != null) {        
            result = isSameCircleOfTrust(spconfig, realm,
                                         trustedEntityId); 
        }
        if (result) {
            return true;
        } 
        IDPSSOConfigElement idpconfig = getIDPSSOConfig(realm,
                                                        entityId);
        if (idpconfig !=null) {
            return (isSameCircleOfTrust(idpconfig, realm,
                        trustedEntityId)); 
        }
        return false;   
    }    
   
    /**
     * Determines whether two entities are in the same circle of trust
     * under the realm. Returns true if entities are in same 
     * circle of trust. The entity can be a PDP or a PEP. If an entity
     * role other then PEP or PDP is specified then a false will be 
     * returned.
     *
     * @param realm The realm under which the entity resides.
     * @param entityId the hosted entity Identifier (PEP or PDP).
     * @param trustedEntityId the remote entity identifier (PEP or PDP).
     * @param role the role of the hosted entity.
     * @throws SAML2MetaException if unable to determine the trusted
     *         relationship.
     */
    public boolean isTrustedXACMLProvider(String realm, String entityId, 
                                         String trustedEntityId,String role) 
        throws SAML2MetaException {
       
        boolean result=false;  
       
        if (role != null) {
            if (role.equals(SAML2Constants.PDP_ROLE)) {
                XACMLPDPConfigElement pdpConfig = 
                        getPolicyDecisionPointConfig(realm,entityId);
                if (pdpConfig != null) {
                    result = isSameCircleOfTrust(pdpConfig,realm,
                                                 trustedEntityId);
                }
            } else if (role.equals(SAML2Constants.PEP_ROLE)) {
                 XACMLAuthzDecisionQueryConfigElement pepConfig = 
                                getPolicyEnforcementPointConfig(realm,entityId);
                 result = isSameCircleOfTrust(pepConfig,realm,trustedEntityId);
            }
        }
        return result;
    }
    
    private boolean isSameCircleOfTrust(BaseConfigType config, String realm,
                 String trustedEntityId) {
        try {
            if (config != null) {
                Map attr = SAML2MetaUtils.getAttributes(config);
                List cotList = (List) attr.get(SAML2Constants.COT_LIST);
                if ((cotList != null) && !cotList.isEmpty()) {
                    for (Iterator iter = cotList.iterator(); 
                        iter.hasNext();) {
                        String a = (String) iter.next(); 
                        if (cotm.isInCircleOfTrust(realm, 
                            a, COTConstants.SAML2, trustedEntityId)) {
                            return true;
                        } 
                     }               
                 }
             } 
             return false;
         } catch (Exception e) {
             debug.error("SAML2MetaManager.isSameCircleOfTrust: Error" +
                   " while determining two entities are in the same COT.");
             return false; 
        }
    }
    
    /**
     * Returns all entities under the realm.
     * @param realm The realm under which the entities reside.
     * @return a <code>Set</code> of entity ID <code>String</code>.
     * @throws SAML2MetaException if unable to retrieve the entity ids.
     */
    public Set getAllEntities(String realm)
        throws SAML2MetaException {

        Set ret = new HashSet();
        String[] objs = { realm };
        try {
            Set entityIds = configInst.getAllConfigurationNames(realm);
            if (entityIds != null && !entityIds.isEmpty()) {
                ret.addAll(entityIds); 
            } 
        } catch (ConfigurationException e) {
            debug.error("SAML2MetaManager.getAllEntities:", e);
            String[] data = { e.getMessage(), realm };
            LogUtil.error(Level.INFO,
                          LogUtil.CONFIG_ERROR_GET_ALL_ENTITIES,
                          data,
                          null);
            throw new SAML2MetaException(e);
        }
        LogUtil.access(Level.FINE,
                       LogUtil.GOT_ALL_ENTITIES,
                       objs,
                       null);
        return ret;
    }

    /**
     * Checks that the provided metaAliases are valid for a new hosted entity in the specified realm.
     * Will verify that the metaAliases do not already exist in the realm and that no duplicates are provided.
     *
     * @param realm The realm in which we are validating the metaAliases.
     * @param newMetaAliases  values we are using to create the new metaAliases.
     * @throws SAML2MetaException if duplicate values found.
     */
    public void validateMetaAliasForNewEntity(String realm, List<String> newMetaAliases) throws SAML2MetaException {

        if (null != newMetaAliases && !newMetaAliases.isEmpty()) {
            if (newMetaAliases.size() > 1) {
                Set checkForDuplicates = new HashSet<String>(newMetaAliases);
                if (checkForDuplicates.size() < newMetaAliases.size()) {
                    debug.error("SAML2MetaManager.validateMetaAliasForNewEntity:Duplicate" +
                    		" metaAlias values provided in list:\n"
                            + newMetaAliases);
                    String[] data = { newMetaAliases.toString() };
                    throw new SAML2MetaException("meta_alias_duplicate", data);
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
                    debug.error("SAML2MetaManager.validateMetaAliasForNewEntity: metaAliases " + sb.toString()
                            + " already exists in the realm: " + realm);
                    String[] data = { sb.toString(), realm };
                    throw new SAML2MetaException("meta_alias_exists", data);
                }
            }
        }
    }
}
