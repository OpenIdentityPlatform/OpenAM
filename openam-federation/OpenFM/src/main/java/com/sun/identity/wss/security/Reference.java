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
 * $Id: Reference.java,v 1.4 2008/06/25 05:50:07 qcheng Exp $
 *
 */

package com.sun.identity.wss.security;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import java.util.ResourceBundle;
import com.sun.identity.shared.debug.Debug;

/**
 * This class <code>Reference</code> represents the Direct Reference
 * of the security token.
 */
public class Reference {
    
    private String uri = null;
    private String valueType = null;
    private static ResourceBundle bundle = WSSUtils.bundle;
    private static Debug debug = WSSUtils.debug;

    /**
     * Constructor
     * @param uri the <code>URI</code> of the security token.
     */
    public Reference(String uri) {
        this.uri = uri;
    }

    /**
     * Constructor
     * @param element the reference element
     * @exception SecurityException if parsing is failed.
     */
    public Reference(Element element) throws SecurityException {
        if(element == null) {
           throw new IllegalArgumentException(
                 bundle.getString("nullInputParameter"));
        }

        if( (!WSSConstants.TAG_REFERENCE.equals(element.getLocalName())) ||
              (!WSSConstants.WSSE_NS.equals(element.getNamespaceURI())) ) {
           debug.error("Reference: Invalid element");
           throw new SecurityException(
                 bundle.getString("invalidElement"));
        }

        uri = element.getAttribute("URI");
        valueType = element.getAttribute("ValueType");

    }

    /**
     * Returns the <code>URI</code> set in the Reference.
     *
     * @return the <code>URI</code>
     */
    public String getURI() {
        return uri;
    }

    /**
     * Sets the <code>URI</code> to the Reference.
     *
     * @param uri the refence <code>URI</code>
     */
    public void setURI(String uri) {
        this.uri = uri;
    }

    /**
     * Returns the value type of the security token.
     *
     * @return the value type of the security token.
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * Sets the value type of the token.
     *
     * @param valueType value type of the token.
     */
    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    /**
     * Adds the reference to parent element.
     * @param parent the parent node that reference needs to be added.
     * @exception SecurityException if there is a failure.
     */
    public void addToParent(Element parent) throws SecurityException {
        try {
            if(parent == null) {
               throw new IllegalArgumentException(
                     bundle.getString("nullInputParameter"));
            }
            Document doc = parent.getOwnerDocument();
            Element reference = doc.createElementNS(WSSConstants.WSSE_NS, 
                    WSSConstants.TAG_REFERENCE);
            reference.setPrefix(WSSConstants.WSSE_TAG);
            if(uri == null) {
               throw new SecurityException(
                     bundle.getString("invalidReference"));
            }
            reference.setAttributeNS(null,  WSSConstants.TAG_URI, uri);
            if(valueType != null) {
               reference.setAttributeNS(null, WSSConstants.TAG_VALUETYPE, 
                                        valueType);
            }
            parent.appendChild(reference);
        } catch (Exception ex) {
            debug.error("Reference.addToParent:: can not add to parent", ex);
            throw new SecurityException(
                  bundle.getString("cannotAddElement"));
        }
    }
}
