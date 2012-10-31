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
 * $Id: ImportEntityModelImpl.java,v 1.11 2009/11/10 01:19:49 exu Exp $
 *
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.workflow.WorkflowException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Document;

import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.federation.cli.ImportMetaData;
import com.sun.identity.workflow.Task;
/**
 * This class provides import entity provider related functionality. Currently
 * the supported types are SAMLv2, IDFF, and WSFederation.
 */
public class ImportEntityModelImpl extends AMModelBase
    implements ImportEntityModel 
{
    private static final String IDFF = "urn:liberty:metadata";
    private static final String WSFED = "Federation";
    private static final String DEFAULT_ROOT = "/";
    private static final String SAML2_PROTOCOL = 
            "urn:oasis:names:tc:SAML:2.0:metadata";
    
    private String standardMetaData;
    private String extendedMetaData;
    private String realm;
   
    public ImportEntityModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }
    /**
     * Import one of the following entity types: SAMLv2, IDFF, or WSFed. The
     * parameters are the file names containing the standard and
     * extended metadata. The standard is required, while the extended is  
     * optional.
     *
     * @param requestData is a Map containing the name of the standard meta 
     *  data file name, and the name of the extended meta data file name.
     *
     * @throws AMConsoleException if unable to process this request.
     */
    public void importEntity(Map requestData) 
        throws AMConsoleException 
    {   
        try {
            // standardFile is the name of the file containing the metada. This
            // is a required parameter. If we don't find it in the request throw
            // an exception.
            String standardFile = (String) requestData.get(STANDARD_META);
            if (standardFile == null) {
                throw new AMConsoleException("missing.metadata");
            }
            standardMetaData = Task.getContent(standardFile, getUserLocale());
            String protocol = getProtocol(standardMetaData);

            // try loading the extended metadata, which is optional
            String extendedFile = (String) requestData.get(EXTENDED_META);
            if ((extendedFile != null) && (extendedFile.length() > 0)) {
                extendedMetaData = Task.getContent(extendedFile, getUserLocale());
                String tmp = getProtocol(standardMetaData);

                // the protocols defined in the standard and extended metadata
                // must be the same.
                if (!protocol.equals(tmp)) {
                    throw new AMConsoleException("protocol.mismatch");
                }
            }

            // the realm is used by the createXXX commands for storing the entity
            realm = (String) requestData.get(REALM_NAME);
            if (realm == null) {
                realm = DEFAULT_ROOT;
            }
            if (protocol.equals(SAML2Constants.PROTOCOL_NAMESPACE)) {
                createSAMLv2Entity();
            } else if (protocol.equals(IDFF)) {
                createIDFFEntity();
            } else {
                createWSFedEntity();
            }
        } catch (WorkflowException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }
    
    private void createSAMLv2Entity() throws AMConsoleException {
        try {
            EntityConfigElement configElt = null;
            
            if (extendedMetaData != null) {
                configElt = getEntityConfigElement();

                if (configElt != null && configElt.isHosted()) {
                    List config = 
                       configElt.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                    if (!config.isEmpty()) {
                        BaseConfigType bConfig = (BaseConfigType)
                            config.iterator().next();
                        
                        // get the realm from the extended meta and use 
                        // for import
                        realm = SAML2MetaUtils.getRealmByMetaAlias(
                            bConfig.getMetaAlias());                     
                    }
                }
            }
                        
            SAML2MetaManager metaManager = new SAML2MetaManager();
            if (standardMetaData != null) {
                importSAML2MetaData(metaManager, realm);
            }
            
            if (configElt != null) {
                metaManager.createEntityConfig(realm, configElt);
            }        
        } catch (SAML2MetaException e) {
            throw new AMConsoleException(e.getMessage());
        }
    }
    
    private EntityConfigElement getEntityConfigElement()
        throws SAML2MetaException, AMConsoleException 
    {
        try {
            Object obj = SAML2MetaUtils.convertStringToJAXB(extendedMetaData);
            return (obj instanceof EntityConfigElement) ?
                (EntityConfigElement)obj : null;
        } catch (JAXBException e) {
            debug.error("ImportEntityModel.getEntityConfigElement", e);
            throw new AMConsoleException(e.getMessage());
        } catch (IllegalArgumentException e) {
            debug.error("ImportEntityModel.getEntityConfigElement", e);
            throw new AMConsoleException(e.getMessage());        
        }
    }

    private void importWSFedMetaData()
        throws WSFederationMetaException, AMConsoleException
    {    
        try {
            Object obj = WSFederationMetaUtils.convertStringToJAXB(standardMetaData);
       
            if (obj instanceof com.sun.identity.wsfederation.jaxb.wsfederation.FederationMetadataElement) {
                obj = ((com.sun.identity.wsfederation.jaxb.wsfederation.FederationMetadataElement)obj).getAny().get(0);
            }

            if (obj instanceof com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement) {
                com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement 
                federation =
                    (com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement)obj;
            
                // TBD
                //Document doc = XMLUtils.toDOMDocument(standardMetadata, debug);
                // WSFederationMetaSecurityUtils.verifySignature(doc);
                (new WSFederationMetaManager()).
                    createFederation(realm, federation);
            }
        } catch (JAXBException e) {
            debug.error("ImportEntityModel.importWSFedMetaData", e);
            throw new AMConsoleException(e.getMessage());
        } catch (IllegalArgumentException e) {
            debug.error("ImportEntityModel.importWSFedMetaData", e);
            throw new AMConsoleException(e.getMessage());
        }
    }
            
    private void importSAML2MetaData(SAML2MetaManager metaManager, String realm)
        throws SAML2MetaException, AMConsoleException
    {        
        try {
            Document doc = XMLUtils.toDOMDocument(standardMetaData, debug);
            ImportMetaData importmetadata = new ImportMetaData();
                   importmetadata.workaroundAbstractRoleDescriptor(doc);
            Object obj = SAML2MetaUtils.convertNodeToJAXB(doc); 

            if (obj instanceof EntityDescriptorElement) {
                EntityDescriptorElement descriptor =
                    (EntityDescriptorElement)obj;
             
                SAML2MetaSecurityUtils.verifySignature(doc);
                metaManager.createEntityDescriptor(realm, descriptor);             
            }
        } catch (JAXBException e) {
            debug.warning("ImportEntityModel.importSAML2MetaData", e);
            throw new AMConsoleException(e.getMessage());
        } catch (IllegalArgumentException e) {
            debug.warning("ImportEntityModel.importSAML2MetaData", e);
            throw new AMConsoleException(e.getMessage());
        } 
    }    
    
    private void createIDFFEntity() throws AMConsoleException {
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(null);

            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement
                configElt = null;
            
            if (extendedMetaData != null) {
                configElt = getIDFFEntityConfigElement();
                
                if ((configElt != null) && configElt.isHosted()) {
                    IDPDescriptorConfigElement idpConfig = 
                        IDFFMetaUtils.getIDPDescriptorConfig(configElt);
                    if (idpConfig != null) {
                        SAML2MetaUtils.getRealmByMetaAlias(
                            idpConfig.getMetaAlias());
                    } else {
                        SPDescriptorConfigElement spConfig =
                            IDFFMetaUtils.getSPDescriptorConfig(configElt);
                        if (spConfig != null) {
                            SAML2MetaUtils.getRealmByMetaAlias(
                                spConfig.getMetaAlias());
                        }
                    }
                }
            }
                       
            importIDFFMetaData(metaManager);            
            if (configElt != null) {
                metaManager.createEntityConfig(realm, configElt);                
            }

        } catch (IDFFMetaException e) {
            throw new AMConsoleException(e.getMessage());
        } 
    }

    private void importIDFFMetaData(IDFFMetaManager metaManager)
        throws IDFFMetaException, AMConsoleException
    {        
        if (standardMetaData == null) {
            if (debug.warningEnabled()) {
                debug.warning("ImportEntityModel.importIDFFMetaData - " +
                    "metaData value was null, skipping import");
            }
            return;
        }
        
        try {
            Object obj = IDFFMetaUtils.convertStringToJAXB(standardMetaData);

            if (obj instanceof
                com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement) {
                com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement
                    descriptor =
                 (com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement)
                    obj;

                //TODO: signature
                //SAML2MetaSecurityUtils.verifySignature(doc);
                //
                metaManager.createEntityDescriptor(realm, descriptor);
            }
    
        } catch (JAXBException e) {
            debug.warning("ImportEntityModel.importIDFFMetaData", e);
            throw new AMConsoleException(e.getMessage());
        } catch (IllegalArgumentException e) {
            debug.warning("ImportEntityModel.importIDFFMetaData", e);
            throw new AMConsoleException(e.getMessage());
        } 
    }
           
    private void createWSFedEntity() throws AMConsoleException {
        try {
            com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement 
                configElt = null;
            
            if (extendedMetaData != null) {              
                configElt = getWSFedEntityConfigElement();
                /*
                 * see note at the end of this class for how we decide
                 * the realm value
                 */
                if (configElt != null && configElt.isHosted()) {
                    List config = configElt.getIDPSSOConfigOrSPSSOConfig();
                    if (!config.isEmpty()) {
                        com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType bConfig = 
                            (com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType)
                            config.iterator().next();
                        realm = WSFederationMetaUtils.getRealmByMetaAlias(
                            bConfig.getMetaAlias());
                    }
                }
            }
            
            if (standardMetaData != null) {
                importWSFedMetaData();
            }
            
            if (configElt != null) {
                (new WSFederationMetaManager()).createEntityConfig(
                    realm, configElt);
            }
        } catch (WSFederationMetaException e) {
            debug.error("ImportEntityModel.createWSFedEntity", e);
            throw new AMConsoleException(e);
        }
    }    
    
    private com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement 
        getWSFedEntityConfigElement()
        throws WSFederationMetaException, AMConsoleException
    {
        try {            
            Object obj = WSFederationMetaUtils.convertStringToJAXB(extendedMetaData);        
            return (obj instanceof 
                com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement) ?
                (com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement)obj : 
                null;
        } catch (JAXBException e) {
            debug.error("ImportEntityModel.getWSFedEntityConfigElement", e);
            throw new AMConsoleException(e);
        } catch (IllegalArgumentException e) {
            debug.error("ImportEntityModel.getWSFedEntityConfigElement", e);
            throw new AMConsoleException(e);
        }
        

    }
    
    private com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement
        getIDFFEntityConfigElement() throws IDFFMetaException, AMConsoleException {

        try {
            Object obj = IDFFMetaUtils.convertStringToJAXB(extendedMetaData);
            return (obj instanceof 
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement) ?
             (com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement)
                obj : null;
        } catch (JAXBException e) {            
            throw new AMConsoleException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new AMConsoleException(e.getMessage());
        }
    }

    
    // returns the type provider defined by the meta data.
    private String getProtocol(String metaData) {       
        String protocol = WSFED; 
        
        if (metaData.contains(SAML2Constants.PROTOCOL_NAMESPACE) || 
                (metaData.contains(SAML2_PROTOCOL))) {                                              
            protocol = SAML2Constants.PROTOCOL_NAMESPACE;         
        } else if (metaData.contains(IDFF)) {
            protocol = IDFF; 
        }
        
        return protocol;           
    }
}
