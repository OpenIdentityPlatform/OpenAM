/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MetadataRequest.java,v 1.2 2009/10/28 23:59:00 exu Exp $
 *
 */

package com.sun.identity.wsfederation.servlet;

import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;


/**
 * This class implements a WS-Federation metadata request.
 */
public class MetadataRequest extends WSFederationAction {
    private static Debug debug = WSFederationUtils.debug;

    public MetadataRequest(HttpServletRequest request,
        HttpServletResponse response) {
        super(request,response);
    }
    
    public void process() throws WSFederationException, IOException {
        String classMethod = "MetadataRequest.process: ";

        String realm = "/";
        String entityId = null;
        
        int prefixLength = (request.getContextPath() + 
            WSFederationConstants.METADATA_URL_PREFIX).length();
        String suffix = request.getRequestURI().substring(prefixLength);
        
        WSFederationMetaManager metaManager = 
            WSFederationUtils.getMetaManager();
        if ( suffix.equals(WSFederationConstants.METADATA_URL_SUFFIX) ) {
            // No entity ID in request - return first defined
            List providers = metaManager.getAllHostedEntities(null);
            
            if ((providers != null) && !providers.isEmpty()) {
                entityId = (String)providers.iterator().next();
            } else {
                throw new WSFederationException(WSFederationUtils.bundle.
                    getString("noHostedEntities"));
            }
        } else {
            // Request URL is of the form METADATA_URL_PREFIX + metaalias + 
            // + METADATA_URL_SUFFIX
            // e.g. /FederationMetadata/2006-12/red/idp/FederationMetadata.xml
            int metaAliasLength = suffix.length() - 
                WSFederationConstants.METADATA_URL_SUFFIX.length();
            String metaAlias = suffix.substring(0, metaAliasLength);
            
            realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
            
            entityId = 
                metaManager.getEntityByMetaAlias(metaAlias);        
            
            if ( entityId==null || entityId.length()==0 )
            {
                String[] args = {metaAlias, realm};
                throw new WSFederationException(
                    WSFederationConstants.BUNDLE_NAME,"invalidMetaAlias", args);
            }
        }

        FederationElement fedElem = 
            metaManager.getEntityDescriptor(realm, entityId);

        String metaXML = null;
        try {
            metaXML = WSFederationMetaUtils.convertJAXBToString(fedElem);
        } catch (JAXBException ex) {
            throw new WSFederationException(ex);
        }

        response.setContentType("text/xml");
        response.setHeader("Pragma", "no-cache");
        response.getWriter().print(metaXML);
    }
}
