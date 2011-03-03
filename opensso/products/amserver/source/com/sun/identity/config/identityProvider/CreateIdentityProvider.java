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
 * $Id: CreateIdentityProvider.java,v 1.2 2008/06/25 05:42:35 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.config.identityProvider;

import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.pojos.CircleTrust;
import com.sun.identity.config.pojos.IdentityProvider;

/**
 *
 * @author Les Hazlewood
 */
public class CreateIdentityProvider extends AjaxPage {

    private final String SUCCESS_PATH = "config/identityProvider/createIdentityProviderSuccess.htm";
    public IdentityProvider identityProvider;

    public void onPost(){
        identityProvider = new IdentityProvider();
        String circleName = toString("circleName");
        if (circleName != null){
            CircleTrust circle = getConfigurator().getCircleOfTrust(circleName);
            if (circle != null)
                identityProvider.setCircleTrust(circle);
        }

        String protocolName = toString("federalProtocolName");
        if (protocolName != null){
            identityProvider.setFederalProtocol(getConfigurator().getFederalProtocol(protocolName));
        }

        identityProvider.setMetadata(toBoolean("metadata"));
        if (identityProvider.hasMetadata()){
            identityProvider.setMetadataType(toString("metadataType"));
            identityProvider.setMetadataUrl(toString("metadataUrl"));
            //todo get file content multipart
        }
        identityProvider.setServiceProviderMetadata(toBoolean("serviceProviderMetadata"));
        if (identityProvider.hasServiceProviderMetadata()){
            identityProvider.setServiceProviderMetadataType(toString("serviceProviderMetadataType"));
            identityProvider.setServiceProviderMetadataUrl(toString("serviceProviderMetadataUrl"));
            //todo get file content multipart

            identityProvider.setArtifactBinding(toBoolean("artifactBinding"));
            identityProvider.setPostBinding(toBoolean("postBinding"));
            identityProvider.setDisplayName(toString("displayName"));
            identityProvider.setIncomingAssertionsSignature(toBoolean("incomingAssertionsSignature"));
            identityProvider.setOutgoingAssertionSignature(toBoolean("outgoingAssertionsSignature"));
            identityProvider.setKeyAlias(toString("keyAlias"));
            identityProvider.setMetaAlias(toString("metaAlias"));
        }

        getConfigurator().createIdentityProvider(identityProvider);
        setPath(SUCCESS_PATH);

    }
}
