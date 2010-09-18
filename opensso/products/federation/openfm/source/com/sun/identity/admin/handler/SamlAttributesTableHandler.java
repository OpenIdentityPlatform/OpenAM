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
import javax.faces.event.FacesEvent;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.SamlAttributeMapItem;
import com.sun.identity.admin.model.SamlAttributesTableBean;

public class SamlAttributesTableHandler implements Serializable
{
    private MessagesBean messagesBean;

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

    // -------------------------------------------------------------------------

    private SamlAttributesTableBean getSamlAttributesTableBean(FacesEvent event) {
        SamlAttributesTableBean bean 
            = (SamlAttributesTableBean) 
              event.getComponent().getAttributes().get("samlAttributesTable");
        assert (bean != null);
        
        return bean;
    }
    
    private SamlAttributeMapItem getSamlAttributeMapItem(FacesEvent event) {
        SamlAttributeMapItem item 
            = (SamlAttributeMapItem)
            event.getComponent().getAttributes().get("attributeMapItem");

        assert (item != null);

        return item;
    }
    
    private void showErrorPopup(String summaryKey, String detailKey) {
        Resources r = new Resources();
        MessageBean mb = new MessageBean(); 
        mb.setSummary(r.getString(this, summaryKey));
        mb.setDetail(r.getString(this, detailKey));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
    }

    private boolean validAttributeMapItem(String assertionName, String localName) {
        String regExp = "[\\w ]{1,50}?";
        if( assertionName.matches(regExp) && localName.matches(regExp) ) {
            return true;
        } else {
            return false;
        }
    }
    
    // -------------------------------------------------------------------------

    public void editListener(ActionEvent event) {
        SamlAttributeMapItem item = getSamlAttributeMapItem(event);
        item.setEditing(true);
        item.setNewAssertionAttributeName(item.getAssertionAttributeName());
        item.setNewLocalAttributeName(item.getLocalAttributeName());
    }

    public void removeListener(ActionEvent event) {
        SamlAttributesTableBean table = getSamlAttributesTableBean(event);
        SamlAttributeMapItem item = getSamlAttributeMapItem(event);

        if( item.isCustom() ) {
            table.getAttributeMapItems().remove(item);
        } else {
            item.setAssertionAttributeName(null);
        }
    }

    public void saveListener(ActionEvent event) {
        SamlAttributeMapItem item = getSamlAttributeMapItem(event);

        String assertionAttrName = item.getNewAssertionAttributeName();
        String localAttrName = item.getNewLocalAttributeName();

        if( validAttributeMapItem(assertionAttrName, localAttrName) ) {
            
            if( !item.isCustom() ) {
                item.setLocalAttributeName(item.getNewLocalAttributeName());
                item.setAssertionAttributeName(item.getNewAssertionAttributeName());
            } else {
                item.setAssertionAttributeName(item.getNewAssertionAttributeName());
            }

            item.resetInterface();
            
        } else {
            showErrorPopup("invalidMapAttributeSummary", 
                           "invalidMapAttributeDetail");
        }
    }
    
    public void cancelSaveListener(ActionEvent event) {
        SamlAttributeMapItem item = getSamlAttributeMapItem(event);

        item.setEditing(false);
        item.setNewAssertionAttributeName(null);
        item.setNewLocalAttributeName(null);
    }
    
    public void showAddListener(ActionEvent event) {
        SamlAttributesTableBean table = getSamlAttributesTableBean(event);

        table.setShowingAddAttribute(true);
        table.setNewAssertionAttributeName(null);
        table.setNewLocalAttributeName(null);
    }
    
    public void addListener(ActionEvent event) {
        SamlAttributesTableBean table = getSamlAttributesTableBean(event);
        String assertionAttrName = table.getNewAssertionAttributeName();
        String localAttrName = table.getNewLocalAttributeName();

        
        if( validAttributeMapItem(assertionAttrName, localAttrName) ) {
            SamlAttributeMapItem item = new SamlAttributeMapItem();
            item.setAssertionAttributeName(assertionAttrName);
            item.setLocalAttributeName(localAttrName);
            item.setCustom(true);
            item.setEditing(false);
            
            table.getAttributeMapItems().add(item);
            table.setShowingAddAttribute(false);
            table.setNewAssertionAttributeName(null);
            table.setNewLocalAttributeName(null);
        } else {
            showErrorPopup("invalidMapAttributeSummary", 
                           "invalidMapAttributeDetail");
        }
        
    }
 
    public void cancelAddListener(ActionEvent event) {
        SamlAttributesTableBean table = getSamlAttributesTableBean(event);

        table.setShowingAddAttribute(false);
        table.setNewAssertionAttributeName(null);
        table.setNewLocalAttributeName(null);
    }
}
