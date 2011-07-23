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
 * $Id: CreateServiceProvider.java,v 1.2 2008/06/25 05:42:41 qcheng Exp $
 *
 */
package com.sun.identity.config.serviceProvider;

import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.pojos.ServiceProvider;
import com.sun.identity.config.pojos.CircleTrust;

/**
 * @author Javier
 */
public class CreateServiceProvider extends AjaxPage {
    
    private final String SUCCESS_PATH = "config/serviceProvider/createServiceProviderSuccess.htm";
    public ServiceProvider serviceProvider;

    public void onPost(){
        serviceProvider = new ServiceProvider();
        String circleName = toString("circleName");
        if (circleName != null){
            CircleTrust circle = getConfigurator().getCircleOfTrust(circleName);
            if (circle != null)
                serviceProvider.setCircleTrust(circle);
        }

        String protocolName = toString("federalProtocolName");
        if (protocolName != null){
            serviceProvider.setFederalProtocol(getConfigurator().getFederalProtocol(protocolName));
        }

        serviceProvider.setMetadata(toBoolean("metadata"));
        if (serviceProvider.hasMetadata()){
            serviceProvider.setMetadataType(toString("metadataType"));
            serviceProvider.setMetadataUrl(toString("metadataUrl"));
            //todo get file content multipart
        }
        serviceProvider.setIdentityProviderMetadata(toBoolean("identityProviderMetadata"));
        if (serviceProvider.hasIdentityProviderMetadata()){
            serviceProvider.setIdentityProviderMetadataType(toString("identityProviderMetadataType"));
            serviceProvider.setIdentityProviderMetadataUrl(toString("identityProviderMetadataUrl"));
            //todo get file content multipart

            serviceProvider.setArtifactBinding(toBoolean("artifactBinding"));
            serviceProvider.setPostBinding(toBoolean("postBinding"));
            serviceProvider.setDisplayName(toString("displayName"));
            serviceProvider.setOutgoingAssertionSignature(toBoolean("outgoingAssertionsSignature"));
            serviceProvider.setKeyAlias(toString("keyAlias"));
            serviceProvider.setMetaAlias(toString("metaAlias"));
        }

        getConfigurator().createServiceProvider(serviceProvider);
        setPath(SUCCESS_PATH);

    }

}
