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
 * $Id: IDPAttributeMapper.java,v 1.3 2008/06/25 05:47:51 qcheng Exp $
 *
 */


package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Exception;


/**
 * This interface <code>IDPAttributeMapper</code> is used to map the 
 * authenticated user configured attributes to SAML <code>Attribute</code>s
 * so that the SAML framework may insert these attribute information as SAML 
 * <code>AttributeStatement</code>s in SAML <code>Assertion</code>.
 * The implementation of this interface can read the configured
 * attributes or the attributes that are available through the Single
 * Sign On Token and returns the SAML <code>Attribute</code>s.
 * @see com.sun.identity.saml2.plugins.SPAttributeMapper
 *
 * @supported.all.api
 */

public interface IDPAttributeMapper {

    /**
     * Returns list of SAML <code>Attribute</code> objects for an 
     * authenticated user local attributes.
     * @param session single sign on session of an authenticated user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.  
     * @param remoteEntityID <code>EntityID</code> of the remote provider.  
     * @param realm realm name.
     * @return list of <code>Attributes</code>s of an authenticated user. 
     * @exception SAML2Exception if any failure.
     */
    public java.util.List getAttributes(
        Object session,
        java.lang.String hostEntityID,
        java.lang.String remoteEntityID, 
        java.lang.String realm
    ) throws SAML2Exception;


}
