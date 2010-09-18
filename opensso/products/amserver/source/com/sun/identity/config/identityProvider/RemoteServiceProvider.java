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
 * $Id: RemoteServiceProvider.java,v 1.3 2008/06/25 05:42:35 qcheng Exp $
 *
 */
package com.sun.identity.config.identityProvider;

import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.pojos.ServiceProvider;
import com.sun.identity.config.pojos.IdentityProvider;

/**
 * @author Les Hazlewood
 */
public class RemoteServiceProvider extends AjaxPage {

    private final String SUCCESS_PATH = "config/serviceProvider/remoteIdentityProviderSuccess.htm";
    public IdentityProvider identityProvider;


    public void onGet(){
        identityProvider = getConfigurator().getIdentityProvider(toInt("identityProviderId"));
    }

   public void onPost(){
       ServiceProvider serviceProvider = new ServiceProvider();

       identityProvider = getConfigurator().getIdentityProvider(toInt("identityProviderId"));


       serviceProvider.setMetadata(toBoolean("metadata"));
       if (serviceProvider.hasMetadata()){
           serviceProvider.setMetadataType(toString("metadataType"));
           serviceProvider.setMetadataUrl(toString("metadataUrl"));
           //todo get file content multipart
       }

       serviceProvider.setDisplayName(toString("displayName"));
       serviceProvider.setAssertionConsumerUrl(toString("assertionConsumerUrl"));
       serviceProvider.setAssertionSigningCertificate(toString("assertionSigningCertificate"));
       serviceProvider.setEntityId(toString("entityId"));

       getConfigurator().createServiceProvider(identityProvider, serviceProvider);
       setPath(SUCCESS_PATH);
   }

}
