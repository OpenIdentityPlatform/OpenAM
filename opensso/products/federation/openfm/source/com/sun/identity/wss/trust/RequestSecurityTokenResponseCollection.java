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
 * $Id: RequestSecurityTokenResponseCollection.java,v 1.1 2008/09/19 16:00:56 mallas Exp $
 *
 */

package com.sun.identity.wss.trust;

import java.util.List;
import org.w3c.dom.Element;

/**
 * This abstract class <code>RequestSecurityTokenResponseCollection</code> 
 * represents the WS-Trust protocol element
 * RequestSecurityTokenResponseCollection. 
 */
public abstract class RequestSecurityTokenResponseCollection {
    
    protected static final String REQUEST_SECURITY_TOKEN_RESPONSE_COLLECTION =
            "RequestSecurityTokenResponseCollection";
    
    protected List rstResponses = null;    
    
    /**
     * Returns the list of <code>RequestSecurityTokenResponse</code>s.
     * @return the list of <code>RequestSecurityTokenResponse</code>s.
     */
    public List getRequestSecurityTokenResponses() {
        return rstResponses;    
    }
    
    /**
     * Sets the list of <code>RequestSecurityTokenResponse<code>s.
     * @param rstrResponses the list of
     *            <code>RequestSecurityTokenResponse<code>s.
     */
    public void setRequestSecurityTokenResponses(List rstrResponses) {
        this.rstResponses = rstrResponses;
    }
    
    /**
     * Converts into DOM Element.
     * @return the DOM Element for the
     *          <code>RequestSecurityTokenResponseCollection</code>
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public abstract Element toDOMElement() throws WSTException;
    
    /**
     * Converts into XML String.
     * @return the XML String for the
     *                     <code>RequestSecurityTokenResponseCollection</code>
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public abstract String toXMLString() throws WSTException;

}
