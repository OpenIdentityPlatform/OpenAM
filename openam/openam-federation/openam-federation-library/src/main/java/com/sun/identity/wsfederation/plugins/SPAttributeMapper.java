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
 * $Id: SPAttributeMapper.java,v 1.3 2008/06/25 05:48:07 qcheng Exp $
 *
 */


package com.sun.identity.wsfederation.plugins;

import com.sun.identity.wsfederation.common.WSFederationException;


/**
 * This interface <code>SPAttributeMapper</code> is used to map the 
 * SAML <code>Attribute</code>s  to the local user attributes.
 * This mapper will be used by the Service Provider that will read
 * the configured map for the corresponding SAML attributes and
 * supply to the SAML framework.
 * The locally mapped attributes returned by the implementation of
 * this interface will be used by the SAML2 framework to expose
 * through the single sign-on token to the application. 
 * Also, the implementation of this mapper may need to consider the deployment
 * of the WS-Federation implementation base platform for example 
 * <code>AccessManager</code>
 * or the <code>FederationManager</code>.
 * @see com.sun.identity.wsfederation.plugins.IDPAttributeMapper
 *
 * @supported.all.api
 */

public interface SPAttributeMapper {

    /**
     * Returns the map of user attribute values for the corresponding
     * SAML <code>Attribute</code>s. This attribute value pair map will be
     * expose by the <code>SAML</code> framework via the Single Sign On
     * Token. 
     * @param attributes list of SAML <code>Attribute</code>s.
     * @param userID universal identifier or the distinguished name (DN) 
     *        of the user. 
     * @param hostEntityID <code>EntityID</code> of the hosted provider.  
     * @param remoteEntityID <code>EntityID</code> of the remote provider.  
     * @return map of <code>AttributeValuePair</code>s for the given
     *        SAML <code>Attribute</code> list. 
     * @exception WSFederationException if any failure.
     */
    public java.util.Map getAttributes(
        java.util.List attributes,
        java.lang.String userID,
        java.lang.String hostEntityID,
        java.lang.String remoteEntityID,
        java.lang.String realm 
    ) throws WSFederationException;


}
