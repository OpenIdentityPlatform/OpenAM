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
 * $Id: UserCredentialsTableBean.java,v 1.1 2009/10/08 16:16:21 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;

public class UserCredentialsTableBean implements Serializable {
    
    private ArrayList<UserCredentialItem> userCredentialItems;
    private String newUserName;
    private String newPassword;
    private boolean showingAddCredential;
    
    //--------------------------------------------------------------------------
    
    public void resetInterface() {
        
        setShowingAddCredential(false);
        setNewPassword(null);
        setNewUserName(null);
        
        for(UserCredentialItem item : getUserCredentialItems()) {
            item.resetInterface();
        }
    }
    
    //--------------------------------------------------------------------------
    
    public void setUserCredentialItems(ArrayList<UserCredentialItem> userCredentialItems) {
        this.userCredentialItems = userCredentialItems;
    }
    public ArrayList<UserCredentialItem> getUserCredentialItems() {
        return userCredentialItems;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public void setNewUserName(String newUserName) {
        this.newUserName = newUserName;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public boolean isShowingAddCredential() {
        return showingAddCredential;
    }

    public void setShowingAddCredential(boolean showingAddCredential) {
        this.showingAddCredential = showingAddCredential;
    }
}
