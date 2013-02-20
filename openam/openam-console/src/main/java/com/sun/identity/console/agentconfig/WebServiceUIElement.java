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
 * $Id: WebServiceUIElement.java,v 1.3 2008/06/25 05:49:33 qcheng Exp $
 *
 */

package com.sun.identity.console.agentconfig;

/**
 * Stub of WSS UI Element.
 */
public class WebServiceUIElement {
    static final String TYPE_BOOL = "boolean";
    static final String TYPE_TEXT = "text";
    
    String childName;
    String attrName;
    String attrType;
    
    /**
     * Creates a new instance of <code>WebServiceUIElement</code>.
     *
     * @param childName View (Child) name.
     * @param attrName Attribute name in agent profile.
     * @param attrType Attribute type such as boolean and text.
     */
    public WebServiceUIElement(
        String childName, 
        String attrName, 
        String attrType
    ) {
        this.childName = childName;
        this.attrName = attrName;
        this.attrType = attrType;
    }
}
