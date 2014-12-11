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
 * $Id: EntityName.java,v 1.2 2008/06/25 05:46:46 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message.common;

import java.util.List;

/**
 * This class has methods to create <code>EntityName</code>
 * object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class EntityName {
    protected String uri = null;
    protected List otherElements = null;
    
    /**
     * Default Constructor.
     */
    public EntityName() {
    }
    
    /**
     * Constructor create <code>EntityName</code> object.
     *
     * @param uri the <code>URI</code>.
     */
    public EntityName(String uri) {
        this.uri = uri;
    }
    
    /**
     * Constructor create <code>EntityName</code> object.
     *
     * @param uri the <code>URI</code>.
     * @param otherElements list of other elements.
     */
    public EntityName(String uri,List otherElements) {
        this.uri = uri;
        this.otherElements = otherElements;
    }
    
    /**
     * Sets the <code>URI</code>.
     *
     * @param uri the value of <code>URI</code>.
     */
    public void setURI(String uri) {
        this.uri = uri;
    }
    
    /**
     * Returns list of other elements.
     *
     * @return list of other elements.
     * @see #setOtherElements
     */
    public List getOtherElements() {
        return otherElements;
    }
    
    /**
     * Sets list of other elements.
     *
     * @param otherElements list of other elements.
     */
    public void setOtherElements(List otherElements) {
        this.otherElements = otherElements;
    }
}
