/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SamlAttributesTableBean.java,v 1.1 2009/10/07 20:00:49 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;

public class SamlAttributesTableBean implements Serializable {

    private ArrayList<SamlAttributeMapItem> attributeMapItems;
    private boolean showingAddAttribute;
    private String newLocalAttributeName;
    private String newAssertionAttributeName;
    
    // -------------------------------------------------------------------------
    
    public void resetInterface() {
        
        this.setShowingAddAttribute(false);
        this.setNewAssertionAttributeName(null);
        this.setNewLocalAttributeName(null);
        
        for(SamlAttributeMapItem item : this.getAttributeMapItems()) {
            item.resetInterface();
        }
    }

    // getters / setters -------------------------------------------------------

    public void setAttributeMapItems(ArrayList<SamlAttributeMapItem> attributeMapItems) {
        this.attributeMapItems = attributeMapItems;
    }

    public ArrayList<SamlAttributeMapItem> getAttributeMapItems() {
        return attributeMapItems;
    }

    public void setShowingAddAttribute(boolean showingAddAttribute) {
        this.showingAddAttribute = showingAddAttribute;
    }

    public boolean isShowingAddAttribute() {
        return showingAddAttribute;
    }

    public void setNewLocalAttributeName(String newLocalAttributeName) {
        this.newLocalAttributeName = newLocalAttributeName;
    }

    public String getNewLocalAttributeName() {
        return newLocalAttributeName;
    }

    public void setNewAssertionAttributeName(String newAssertionAttributeName) {
        this.newAssertionAttributeName = newAssertionAttributeName;
    }

    public String getNewAssertionAttributeName() {
        return newAssertionAttributeName;
    }
    
}
