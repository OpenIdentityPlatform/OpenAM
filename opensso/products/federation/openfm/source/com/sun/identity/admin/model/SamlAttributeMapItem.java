/**
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
 * $Id $
 */
 
 package com.sun.identity.admin.model;

import java.io.Serializable;

public class SamlAttributeMapItem implements Serializable {

    private String localAttributeName;
    private String assertionAttributeName;
    private boolean custom;
    private boolean editing;
    private String newLocalAttributeName;
    private String newAssertionAttributeName;
    
    // convenience methods -----------------------------------------------------
    
    public void resetInterface() {
        this.setEditing(false);
        this.setNewAssertionAttributeName(null);
        this.setNewLocalAttributeName(null);
    }
    
    // getters / setters -------------------------------------------------------

    public void setLocalAttributeName(String localAttributeName) {
        this.localAttributeName = localAttributeName;
    }

    public String getLocalAttributeName() {
        return localAttributeName;
    }

    public void setAssertionAttributeName(String assertionAttributeName) {
        this.assertionAttributeName = assertionAttributeName;
    }

    public String getAssertionAttributeName() {
        return assertionAttributeName;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public boolean isEditing() {
        return editing;
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
