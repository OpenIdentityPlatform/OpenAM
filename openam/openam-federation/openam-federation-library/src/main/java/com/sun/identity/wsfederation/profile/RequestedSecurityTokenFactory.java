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
 * $Id: RequestedSecurityTokenFactory.java,v 1.2 2008/06/25 05:48:08 qcheng Exp $
 * 
 */

package com.sun.identity.wsfederation.profile;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import org.w3c.dom.Node;

/**
 *
 * @author pat
 */
public class RequestedSecurityTokenFactory {

    /*
     * Private constructor ensure that no instance is ever created
     */
    private RequestedSecurityTokenFactory() {
    }
    
    /**
     * Creates a RequestedSecurityToken based on the supplied DOM Node. This is 
     * the extension point for adding new token formats.
     * @param element RequestedSecurityToken <code>Node</code>
     * @return a RequestedSecurityToken
     */
    public static RequestedSecurityToken createToken(Node element)
        throws WSFederationException
    {
        if ( ! element.getLocalName().
            equals(WSFederationConstants.RST_TAG_NAME) ){
            WSFederationUtils.debug.error("Got node " + 
                element.getLocalName() + " (expecting " + 
                WSFederationConstants.RST_TAG_NAME + ")");
            throw new WSFederationException(WSFederationUtils.bundle.
                getString("invalidToken"));
        }

        Node token = element.getFirstChild();

        if (token.getNamespaceURI().
                equals(SAMLConstants.assertionSAMLNameSpaceURI)
            && token.getLocalName().equals(SAMLConstants.TAG_ASSERTION))
        {
            return new SAML11RequestedSecurityToken(element);
        }
        // Extension point for new token types

        return null;
    }    
}
