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
 * $Id: NameIdentifierMapper.java,v 1.2 2008/06/25 05:47:12 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.plugins;

import com.sun.identity.saml.assertion.NameIdentifier;
import java.util.Map;
import java.util.List;

/**
 * The class <code>NameIdentifierMapper</code> is an interface that is 
 * used to map user's <code>NameIdentifier</code> from one provider 
 * to another.
 * <p>
 * @supported.all.api
 *
 */
public interface NameIdentifierMapper {

    /**
     * Returns mapped <code>NameIdentifier</code> for specified user.   
     * This is used by Discovery Service to generate correct 
     * <code>NameIdentifier</code> when creating credentials for remote
     * service provider. A <code>NameIdentifier</code> in encrypted format 
     * could be returned if the response will be passed through a proxy.
     * @param spProviderID Provider ID of the service provider to which
     *     the <code>NameIdentifier</code> needs to be mapped. 
     * @param idpProviderID Provider ID of the identifier provider.
     * @param nameId The <code>NameIdentifier</code> needs to be mapped. 
     * @param userID The user whose mapped <code>NameIdentifier</code> will 
     *     be returned. The value is the universal identifier of the user.
     * @return the mapped <code>NameIdentifier</code> for specified user, 
     *     return null if unable to map the <code>NameIdentifier</code>,
     *     return original name identifier if no need to map the
     *     <code>NameIdentifier</code>.
     */

    public NameIdentifier getNameIdentifier(String spProviderID, 
        String idpProviderID, NameIdentifier nameId, String userID);
} 
