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

package com.sun.identity.admin.handler;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.UserCredentialItem;
import com.sun.identity.admin.model.UserCredentialsTableBean;

public class UserCredentialsTableHandler implements Serializable
{
    private MessagesBean messagesBean;

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

    // -------------------------------------------------------------------------

    private UserCredentialItem getUserCredentialItem(ActionEvent event) {
        UserCredentialItem item 
            = (UserCredentialItem) 
              event.getComponent().getAttributes().get("userCredentialItem");
        
        assert (item != null);
        return item;
    }
    
    private UserCredentialsTableBean getUserCredentialsTable(ActionEvent event) {
        UserCredentialsTableBean table 
            = (UserCredentialsTableBean) 
              event.getComponent().getAttributes().get("userCredentialsTable");
    
        assert (table != null);
        return table;
    }
    
    private void showErrorPopup(String summaryKey, String detailKey) {
        Resources r = new Resources();
        MessageBean mb = new MessageBean(); 
        mb.setSummary(r.getString(this, summaryKey));
        mb.setDetail(r.getString(this, detailKey));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
    }
    
    private boolean validUserCredential(String username, String password) {
        String regExp = "[\\w ]{1,50}?";
        if( username.matches(regExp) && password.matches(regExp) ) {
            return true;
        } else {
            return false;
        }
    }
    
    // -------------------------------------------------------------------------

    public void editListener(ActionEvent event) {
        UserCredentialItem item = getUserCredentialItem(event);
        item.setEditing(true);
        item.setNewPassword(null);
        item.setNewUserName(item.getUserName());
    }

    public void removeListener(ActionEvent event) {
        UserCredentialsTableBean table = getUserCredentialsTable(event);
        UserCredentialItem item = getUserCredentialItem(event);
        table.getUserCredentialItems().remove(item);
    }

    public void saveListener(ActionEvent event) {
        UserCredentialItem item = getUserCredentialItem(event);

        if( validUserCredential(item.getNewUserName(), item.getNewPassword()) ) {
            item.setUserName(item.getNewUserName());
            item.setPassword(item.getNewPassword());
            item.setEditing(false);
        } else {
            showErrorPopup("invalidCredentialSummary", 
                           "invalidCredentialDetails");
        }
    }
    
    public void cancelSaveListener(ActionEvent event) {
        UserCredentialItem item = getUserCredentialItem(event);
        item.setEditing(false);
        item.setNewPassword(null);
        item.setNewUserName(null);
    }
    
    public void showAddListener(ActionEvent event) {
        UserCredentialsTableBean table = getUserCredentialsTable(event);
        table.setShowingAddCredential(true);
        table.setNewUserName(null);
        table.setNewPassword(null);
    }
    
    public void cancelAddListener(ActionEvent event) {
        UserCredentialsTableBean table = getUserCredentialsTable(event);
        table.setShowingAddCredential(false);
        table.setNewUserName(null);
        table.setNewPassword(null);
    }

    public void addListener(ActionEvent event) {
        UserCredentialsTableBean table = getUserCredentialsTable(event);        
        String newUserName = table.getNewUserName();
        String newPassword = table.getNewPassword();
        
        if( validUserCredential(newUserName, newPassword) ) {
            UserCredentialItem uci = new UserCredentialItem();
            uci.setUserName(newUserName);
            uci.setPassword(newPassword);
            table.getUserCredentialItems().add(uci);
            
            table.setShowingAddCredential(false);
            table.setNewUserName(null);
            table.setNewPassword(null);
        } else {
            showErrorPopup("invalidCredentialSummary", 
                           "invalidCredentialDetails");
        }
    }
 
}
