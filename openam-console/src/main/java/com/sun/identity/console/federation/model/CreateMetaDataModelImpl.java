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
 * $Id: CreateMetaDataModelImpl.java,v 1.7 2010/01/06 23:11:25 veiming Exp $
 *
 */

package com.sun.identity.console.federation.model;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.workflow.CreateIDFFMetaDataTemplate;
import com.sun.identity.workflow.CreateSAML2HostedProviderTemplate;
import com.sun.identity.workflow.CreateWSFedMetaDataTemplate;
import com.sun.identity.workflow.ImportSAML2MetaData;
import com.sun.identity.workflow.WorkflowException;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.security.cert.CertificateEncodingException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;

public class CreateMetaDataModelImpl extends AMModelBase
    implements CreateMetaDataModel 
{
    private String requestURL;
    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public CreateMetaDataModelImpl(HttpServletRequest req,  Map map) {
        super(req, map);
        String uri = req.getRequestURI().toString();
        int idx = uri.indexOf('/', 1);
        uri = uri.substring(0, idx);
        if (AMSystemConfig.isConsoleRemote) {
            requestURL = SystemProperties.getServerInstanceName();
        } else {
            requestURL = req.getScheme() + "://" + req.getServerName() +
                ":" + req.getServerPort() + uri;
        }
    }

    /**
     * Creates a SAMLv2 provider.
     *
     * @param realm Realm Name.
     * @param entityId Entity Id.
     * @param values   Map of property name to values.
     */
    public void createSAMLv2Provider(String realm, String entityId, Map values)
        throws AMConsoleException {
        try {
            String metadata = CreateSAML2HostedProviderTemplate.
                buildMetaDataTemplate(entityId, values, requestURL);
            String extendedData = CreateSAML2HostedProviderTemplate.
                createExtendedDataTemplate(entityId, values, requestURL);
            ImportSAML2MetaData.importData(realm, metadata, extendedData);
        } catch (WorkflowException ex) {
            throw new AMConsoleException(getErrorString(ex));
        } catch (SAML2MetaException ex) {
            throw new AMConsoleException(getErrorString(ex));
        }
    }

    /**
     * Creates a IDFF provider.
     *
     * @param realm Realm Name.
     * @param entityId Entity Id.
     * @param values   Map of property name to values.
     */
    public void createIDFFProvider(String realm, String entityId, Map values)
        throws AMConsoleException {
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(null);
            String metadata = CreateIDFFMetaDataTemplate.
                createStandardMetaTemplate(entityId, values, requestURL);
            String extendedData = CreateIDFFMetaDataTemplate.
                createExtendedMetaTemplate(entityId, values);
            EntityDescriptorElement descriptor = (EntityDescriptorElement)
                IDFFMetaUtils.convertStringToJAXB(metadata);
            EntityConfigElement configElt = (EntityConfigElement)
                IDFFMetaUtils.convertStringToJAXB(extendedData);
            metaManager.createEntityDescriptor(realm, descriptor);
            metaManager.createEntityConfig(realm, configElt);
        } catch (JAXBException ex) {
            throw new AMConsoleException(ex.getMessage());
        } catch (IDFFMetaException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }

    /**
     * Creates a WS Federation provider.
     *
     * @param realm Realm Name.
     * @param entityId Entity Id.
     * @param values   Map of property name to values.
     */
    public void createWSFedProvider(String realm, String entityId, Map values)
        throws AMConsoleException {
        try {
            String metadata = 
                CreateWSFedMetaDataTemplate.createStandardMetaTemplate(
                entityId, values, requestURL);
            String extendedData = 
                CreateWSFedMetaDataTemplate.createExtendedMetaTemplate(
                entityId, values);
        
            FederationElement elt = (FederationElement) 
                WSFederationMetaUtils.convertStringToJAXB(metadata);
            String federationID = elt.getFederationID();
            if (federationID == null) {
                federationID = WSFederationConstants.DEFAULT_FEDERATION_ID;
            }
            WSFederationMetaManager metaManager = 
                new WSFederationMetaManager();
            metaManager.createFederation(realm, elt);
            
            FederationConfigElement cfg = (FederationConfigElement)
                WSFederationMetaUtils.convertStringToJAXB(extendedData);
            metaManager.createEntityConfig(realm, cfg);
        } catch (WSFederationMetaException ex) {
            throw new AMConsoleException(ex.getMessage());
        } catch (JAXBException ex) {
            throw new AMConsoleException(ex.getMessage());
        } catch (CertificateEncodingException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }
}
