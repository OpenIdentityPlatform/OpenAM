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
import java.util.ArrayList;

import javax.faces.application.FacesMessage;
import javax.faces.component.UISelectOne;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.ValueChangeEvent;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.EditableSelectOneBean;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;

import edu.emory.mathcs.backport.java.util.Collections;

public class EditableSelectOneHandler implements Serializable
{
    private MessagesBean messagesBean;

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

    // -------------------------------------------------------------------------

    private EditableSelectOneBean getEditableSelectOneBean(FacesEvent event) {
        EditableSelectOneBean bean 
            = (EditableSelectOneBean) 
              event.getComponent().getAttributes().get("editableSelectOneBean");
        assert (bean != null);
        
        return bean;
    }

    private boolean newItemInvalid(EditableSelectOneBean bean) {
        if( bean.getNewItem() != null 
            && bean.getValidPattern() != null 
            && !bean.getNewItem().matches(bean.getValidPattern()) ) {

            Resources r = new Resources();
            showErrorMessage(r.getString(this, "invalidSummary"), 
                             bean.getInvalidMessageDetail());
            return true;	
        } else {
            return false;
        }
    }

    private boolean newItemIsDuplicate(EditableSelectOneBean bean) {
        if( bean.getItems() != null
            && bean.getNewItem() != null
            && bean.getItems().contains(bean.getNewItem()) ) {

            Resources r = new Resources();
            showErrorMessage(r.getString(this, "duplicateSummary"), 
                             r.getString(this, "duplicateDetail"));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds the bean's new item to the bean's item list.
     * @param bean
     * @return True if a change was made, false otherwise.
     */
    private boolean addNewItemToList(EditableSelectOneBean bean) {

        if( bean.getNewItem() == null || bean.getNewItem().length() <= 0 ) {
            // do nothing with "empty" strings...
            return false;
        } else if( newItemInvalid(bean) ) {
            // show the error message and return...
            return false;
        } else if( newItemIsDuplicate(bean) ) {
            // show the error message and return...
            return false;
        }
        
        if( bean.getItems() == null ) {
            bean.setItems(new ArrayList<String>());
        }
        bean.getItems().add(bean.getNewItem());
        Collections.sort(bean.getItems());
    	return true;
    }

    /**
     * Modify the bean's existing item in the bean's item list.
     * @param bean
     * @return True if a change was made, false otherwise.
     */
    private boolean modifyExistingItemInList(EditableSelectOneBean bean) {

        if( bean.getNewItem() == null || bean.getNewItem().length() <= 0 ) {
            // do nothing with "empty" strings...
            return false;
        } else if( bean.getSelectedItem() == null ) {
            // do nothing with "empty" selection...
            return false;
        } else if( bean.getSelectedItem().equals(bean.getNewItem()) ) {
            // no changes needed since they are the same
            return true;
        } 
        
        // "Add" the change and "remove" the old value from the list.
        if( addNewItemToList(bean) ) {
            removeSelectedItemFromList(bean);
            return true;
        } else {
            return false;
        }
    }

    private void removeSelectedItemFromList(EditableSelectOneBean bean) {
        if( bean.getItems() != null && bean.getSelectedItem() != null ) {
            ArrayList<String> newList = new ArrayList<String>();
            for(String s : bean.getItems()) {
                if( !bean.getSelectedItem().equals(s)){
                    newList.add(s);
                }
            }
            bean.setItems(newList);
            bean.setSelectedItem(null);
        }		
    }

    private void showErrorMessage(String summary, String detail) {
        MessageBean mb = new MessageBean(); 
        mb.setSummary(summary);
        mb.setDetail(detail);
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
    }

    // Listeners ---------------------------------------------------------------

    public void madeSelectionListener(ValueChangeEvent event) {
        if( event.getSource() instanceof UISelectOne ) {
            UISelectOne ui = (UISelectOne)event.getSource();
            if( ui.getValue() != null ) {
                EditableSelectOneBean bean = getEditableSelectOneBean(event);
                bean.setSelectedItem(ui.getValue().toString());
                bean.resetInterface();
            }
        }
    }

    public void addNewListener(ActionEvent event) {
        EditableSelectOneBean bean = getEditableSelectOneBean(event);
        bean.setShowingNewItemInput(true);
        bean.setEditingExisting(false);
        bean.setNewItem(null);
    }

    public void editSelectionListener(ActionEvent event) {
        EditableSelectOneBean bean = getEditableSelectOneBean(event);
        if( bean.getItems() != null && bean.getSelectedItem() != null ) {
            bean.setShowingNewItemInput(true);
            bean.setEditingExisting(true);
            bean.setNewItem(bean.getSelectedItem());
        }
    }

    public void removeSelectionListener(ActionEvent event) {
        EditableSelectOneBean bean = getEditableSelectOneBean(event);
        removeSelectedItemFromList(bean);
    }

    public void addItemListener(ActionEvent event) {
        EditableSelectOneBean bean = getEditableSelectOneBean(event);
        if( addNewItemToList(bean) ) {
            bean.resetInterface();
        }
    }

    public void editItemListener(ActionEvent event) {
        EditableSelectOneBean bean = getEditableSelectOneBean(event);
        if( modifyExistingItemInList(bean) ) {
            bean.resetInterface();
        }
    }

    public void cancelItemListener(ActionEvent event) {
        EditableSelectOneBean bean = getEditableSelectOneBean(event);
        bean.resetInterface();
    }

}
