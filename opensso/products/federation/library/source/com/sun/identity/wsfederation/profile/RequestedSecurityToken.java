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
 * $Id: RequestedSecurityToken.java,v 1.2 2008/06/25 05:48:08 qcheng Exp $
 * 
 */

package com.sun.identity.wsfederation.profile;

import com.sun.identity.wsfederation.common.WSFederationException;
import java.util.List;
import java.util.Map;

/**
 * This interface encapsulates the WS-Trust &lt;RequestedSecurityToken&gt; 
 * element
 */
public interface RequestedSecurityToken {

    /**
     * @return a list of attributes in the RequestedSecurityToken. The type of 
     * the List content is dependent on the implementation.
     */
    public List getAttributes();

    /**
     * @return the issuer of the RequestedSecurityToken.
     */
    public String getIssuer();
    /**
     * @return the unique identifier of the RequestedSecurityToken.
     */
    public String getTokenId();
    
    /**
     * Verifies the token's validity, checking the signature, validity period 
     * etc.
     * @param realm the realm of the local entity
     * @param hostEntityId the local entity ID
     * @param timeskew permitted skew between service provider and identity 
     * provider clocks, in seconds
     * @return a Map of relevant data including Subject and the List of 
     * Assertions.
     * @throws com.sun.identity.wsfederation.common.WSFederationException in 
     * case of any error - invalid token signature, token expired etc.
     */
    public Map<String,Object> verifyToken(String realm, String hostEntityId, 
        int timeskew) 
        throws WSFederationException;
}
