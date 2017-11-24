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
 * $Id: ClaimType.java,v 1.1 2009/10/13 23:19:49 mallas Exp $
 *
 */

package com.sun.identity.wss.trust;

import org.w3c.dom.Element;

/**
 * This class <code>ClaimType</code> represents Identity 
 * ClaimType Element.
 */

public class ClaimType {
    
    public static final String IDENTITY_NS = 
                   "http://schemas.xmlsoap.org/ws/2005/05/identity";
    public static final String CLAIMS_NS = IDENTITY_NS + "/Claims";
    private String name = null;
    private String nameSpaceURI = null;
    private boolean optional = false;
    
    /**
     * Constructor
     * @param nameSpaceURI the name space for the claim type.
     */
    public ClaimType(String nameSpaceURI) {
        this.nameSpaceURI = nameSpaceURI;
    }
    
    /**
     * Constructor
     * @param element the claimtype element
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public ClaimType(Element element) throws WSTException {
        if(element == null) {
           throw new WSTException("nullElement"); 
        }
        if(!"ClaimType".equals(element.getLocalName())) {
           throw new WSTException("invalidElement"); 
        }
        String uri = element.getAttribute("Uri");                
        if(uri.startsWith(CLAIMS_NS)) {           
           name = uri.substring(CLAIMS_NS.length() +1, uri.length()); 
        }
        
        String opt =   element.getAttribute("Optional");
        if(opt != null && opt.equals("true")) {
           optional = true; 
        }
    }
   
    /**
     * Returns the name for the claim.
     * @return the name of the claim.
     */
    public String getName() {
        return name;    
    }
    
    /**
     * Sets the name of the claim.
     * @param name name of the claim.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns true if the claim is optional.
     * @return true if the clam is optional.
     */
    public boolean isOptional() {
        return optional;
    }
    
    /**
     * Sets the optional flag.
     * @param optional true if the claim is optional.
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * Returns to an XML String for the ClaimType.
     * @return the XML String for the ClaimType.
     */
    public String toXMLString() {
        StringBuffer sb = new StringBuffer(50);
        sb.append("<ic:ClaimType ").append("Uri=\"").append(CLAIMS_NS)
          .append("/").append(name).append("\"");
        if(optional) {
           sb.append(" Optional=\"true\""); 
        }
        sb.append(" xmlns:ic=").append("\"").append(IDENTITY_NS)
                .append("\"").append("/>");       
        return sb.toString();
    }
}
