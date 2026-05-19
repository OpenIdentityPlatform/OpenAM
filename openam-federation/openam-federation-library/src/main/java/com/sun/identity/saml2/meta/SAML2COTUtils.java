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
 * $Id: SAML2COTUtils.java,v 1.8 2009/10/28 23:58:58 exu Exp $
 * 
 * Portions Copyrighted 2026 3A Systems LLC.
 *
 */


package com.sun.identity.saml2.meta;

import com.sun.identity.saml2.jaxb.entityconfig.AttributeElement;
import jakarta.xml.bind.JAXBException;
import java.util.Iterator;
import java.util.List;

import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.AffiliationConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AttributeAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.AuthnAuthorityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.jaxb.metadataextquery.AttributeQueryDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import jakarta.xml.bind.JAXBElement;

import java.util.ArrayList;

/**
 * The <code>SAML2COTUtils</code> provides utility methods to update
 * the SAML2 Entity Configuration <code>cotlist</code> attributes
 * in the Service and Identity Provider configurations.
 */
public class SAML2COTUtils {
    
    private static Debug debug = SAML2MetaUtils.debug;
    private Object callerSession = null;    
    /**
     * Default Constructor.
     */
    public SAML2COTUtils()  {
    }
    
    /**
     * Constructor.
     * @param callerToken session token of the caller.
     */
    public SAML2COTUtils(Object callerToken) {
        callerSession = callerToken;
    }

    /**
     * Updates the entity config to add the circle of turst name to the
     * <code>cotlist</code> attribute. The Service Provider and Identity
     * Provider Configuration are updated.
     *
     * @param realm the realm name where the entity configuration is.
     * @param name the circle of trust name.
     * @param entityId the name of the Entity identifier.
     * @throws SAML2MetaException if there is a configuration error when
     *         updating the configuration.
     * @throws JAXBException is there is an error updating the entity
     *          configuration.
     */
    public void updateEntityConfig(String realm, String name, String entityId)
    throws SAML2MetaException, JAXBException {
        String classMethod = "SAML2COTUtils.updateEntityConfig: ";
        SAML2MetaManager metaManager = null;
        if (callerSession == null) {
            metaManager = new SAML2MetaManager();
        } else {
            metaManager = new SAML2MetaManager(callerSession);
        } 
        ObjectFactory objFactory = new ObjectFactory();
        // Check whether the entity id existed in the DS
        EntityDescriptorElement edes = metaManager.getEntityDescriptor(
                realm, entityId);
        if (edes == null) {
            debug.error(classMethod +"No such entity: " + entityId);
            String[] data = {realm, entityId};
            throw new SAML2MetaException("entityid_invalid", data);
        }
        boolean isAffiliation = false;
        if (metaManager.getAffiliationDescriptor(realm, entityId) != null) {
            isAffiliation = true;
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + "is " + entityId + " in realm " 
                + realm + " an affiliation? " + isAffiliation);
        }

        EntityConfigElement eConfig = metaManager.getEntityConfig(
            realm, entityId);
        if (eConfig == null) {
            BaseConfigType bctype = null;
            AttributeType atype = objFactory.createAttributeType();
            atype.setName(SAML2Constants.COT_LIST);
            atype.getValue().add(name);
            // add to eConfig
            EntityConfigElement ele = objFactory.createEntityConfigElement(objFactory.createEntityConfigType());
            ele.getValue().setEntityID(entityId);
            ele.getValue().setHosted(false);
            if (isAffiliation) {
                // handle affiliation case
                bctype = new BaseConfigType() {};
                bctype.getAttribute().add(objFactory.createAttributeElement(atype));
                ele.getValue().setAffiliationConfig(objFactory.createAffiliationConfigElement(bctype));
            } else {
                List<JAXBElement<BaseConfigType>> ll =
                    ele.getValue().getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                // Decide which role EntityDescriptorElement includes
                List<JAXBElement<? extends RoleDescriptorType>> list =
                    edes.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

                for(Iterator<JAXBElement<? extends RoleDescriptorType>> iter = list.iterator(); iter.hasNext();) {
                    Object obj = iter.next();
                    if (obj instanceof SPSSODescriptorElement) {
                        bctype = new BaseConfigType() {};
                        bctype.getAttribute().add(objFactory.createAttributeElement(atype));
                        ll.add(objFactory.createSPSSOConfigElement(bctype));
                    } else if (obj instanceof IDPSSODescriptorElement) {
                        bctype = new BaseConfigType() {};
                        bctype.getAttribute().add(objFactory.createAttributeElement(atype));
                        ll.add(objFactory.createIDPSSOConfigElement(bctype));
                    } else if (obj instanceof XACMLPDPDescriptorElement) {
                        bctype = new BaseConfigType() {};
                        bctype.getAttribute().add(objFactory.createAttributeElement(atype));
                        ll.add(objFactory.createXACMLPDPConfigElement(bctype));
                    } else if (obj instanceof 
                        XACMLAuthzDecisionQueryDescriptorElement) 
                    {
                        bctype = new BaseConfigType() {};

                        bctype.getAttribute().add(objFactory.createAttributeElement(atype));
                        ll.add(objFactory.createXACMLAuthzDecisionQueryConfigElement(bctype));
                    } else if (obj instanceof AttributeAuthorityDescriptorElement) {
                        bctype = new BaseConfigType() {};
                        bctype.getAttribute().add(objFactory.createAttributeElement(atype));
                        ll.add(objFactory.createAttributeAuthorityConfigElement(bctype));
                    } else if (obj instanceof  AttributeQueryDescriptorElement){
                        bctype = new BaseConfigType() {};
                        bctype.getAttribute().add(objFactory.createAttributeElement(atype));
                        ll.add(objFactory.createAttributeQueryConfigElement(bctype));
                    } else if (obj instanceof AuthnAuthorityDescriptorElement) {
                        bctype = new BaseConfigType() {};
                        bctype.getAttribute().add(objFactory.createAttributeElement(atype));
                        ll.add(objFactory.createAuthnAuthorityConfigElement(bctype));
                    }
                }
            }
            metaManager.setEntityConfig(realm,ele);
        } else {
            boolean needToSave = true;
            List<JAXBElement<BaseConfigType>> elist = null;
            if (isAffiliation) {
                AffiliationConfigElement affiliationCfgElm =
                    metaManager.getAffiliationConfig(realm, entityId);
                elist = new ArrayList<>();
                elist.add(affiliationCfgElm);
            } else {
                elist = eConfig.getValue().
                    getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
            }
            for (Iterator<JAXBElement<BaseConfigType>> iter = elist.iterator(); iter.hasNext();) {
                boolean foundCOT = false;
                BaseConfigType bConfig = iter.next().getValue();
                List<AttributeElement> list = bConfig.getAttribute();
                for (Iterator<AttributeElement> iter2 = list.iterator(); iter2.hasNext();) {
                    AttributeType avp = iter2.next().getValue();
                    if (avp.getName().trim().equalsIgnoreCase(
                            SAML2Constants.COT_LIST)) {
                        foundCOT = true;
                        List<String> avpl = avp.getValue();
                        if (avpl.isEmpty() ||!containsValue(avpl,name)) {
                            avpl.add(name);
                            needToSave = true;
                            break;
                        }
                    }
                }
                // no cot_list in the original entity config
                if (!foundCOT) {
                    AttributeType atype = objFactory.createAttributeType();
                    atype.setName(SAML2Constants.COT_LIST);
                    atype.getValue().add(name);
                    list.add(objFactory.createAttributeElement(atype));
                    needToSave = true;
                }
            }
            if (needToSave) {
                metaManager.setEntityConfig(realm,eConfig);
            }
        }
    }
    
    private boolean containsValue(List<String> list, String name) {
        for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
            if (iter.next().trim().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Removes the circle trust name passed from the <code>cotlist</code>
     * attribute in the Entity Config. The Service Provider and Identity
     * Provider Entity Configuration are updated.
     *
     * @param name the circle of trust name to be removed.
     * @param entityId the entity identifier of the provider.
     * @throws SAML2MetaException if there is an error updating the entity
     *          config.
     * @throws JAXBException if there is an error updating the entity config.
     */
    
    public void removeFromEntityConfig(String realm,String name,String entityId)
    throws SAML2MetaException, JAXBException {
        String classMethod = "SAML2COTUtils.removeFromEntityConfig: ";
        SAML2MetaManager metaManager = null;
        if (callerSession == null) {
            metaManager = new SAML2MetaManager();
        } else {
            metaManager = new SAML2MetaManager(callerSession);
        }
        // Check whether the entity id existed in the DS
        EntityDescriptorElement edes = metaManager.getEntityDescriptor(
                realm, entityId);
        if (edes == null) {
            debug.error(classMethod +"No such entity: " + entityId);
            String[] data = {realm, entityId};
            throw new SAML2MetaException("entityid_invalid", data);
        }
        EntityConfigElement eConfig = metaManager.getEntityConfig(
                realm, entityId);

        boolean isAffiliation = false;
        if (metaManager.getAffiliationDescriptor(realm, entityId) != null) {
            isAffiliation = true;
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + "is " + entityId + " in realm " 
                + realm + " an affiliation? " + isAffiliation);
        }

        if (eConfig != null) {
            List<JAXBElement<BaseConfigType>> elist = null;
            if (isAffiliation) {
                AffiliationConfigElement affiliationCfgElm =
                    metaManager.getAffiliationConfig(realm, entityId);
                elist = new ArrayList<>();
                elist.add(affiliationCfgElm);
            } else {
               elist = eConfig.getValue().
                    getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
            }

            boolean needToSave = false;
            for (Iterator<JAXBElement<BaseConfigType>> iter = elist.iterator(); iter.hasNext();) {
                BaseConfigType bConfig = iter.next().getValue();
                List<AttributeElement> list = bConfig.getAttribute();
                for (Iterator<AttributeElement> iter2 = list.iterator(); iter2.hasNext();) {
                    AttributeType avp = iter2.next().getValue();
                    if (avp.getName().trim().equalsIgnoreCase(
                            SAML2Constants.COT_LIST)) {
                        List<String> avpl = avp.getValue();
                        if (avpl != null && !avpl.isEmpty() &&
                                containsValue(avpl,name)) {
                            avpl.remove(name);
                            needToSave = true;
                            break;
                        }
                    }
                }
            }
            if (needToSave) {
                metaManager.setEntityConfig(realm, eConfig);
            }
        }
    }
}
