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
 * $Id: EditableSelectOneBean.java,v 1.2 2009/09/30 22:01:27 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.SelectItem;

import com.sun.identity.admin.Resources;

public class EditableSelectOneBean implements Serializable
{
    private ArrayList<String> items;
    private String selectedItem;
    private String newItem;
    private boolean showingNewItemInput;
    private boolean editingExisting;
    private String validPattern;
    private String invalidMessageDetail;

    public EditableSelectOneBean() {
        this.items = null;
        this.selectedItem = null;
        this.newItem = null;
        this.showingNewItemInput = false;
        this.editingExisting = false;
        this.validPattern = null;
        this.invalidMessageDetail = null;
    }

    // convenience methods -----------------------------------------------------
    
    public void resetInterface() {
        this.setShowingNewItemInput(false);
        this.setEditingExisting(false);
        this.setNewItem(null);
    }
    
    // lists -------------------------------------------------------------------
    
    public List<SelectItem> getSelectItemList() {
        List<SelectItem> list = new ArrayList<SelectItem>();
        
        if( this.getItems() != null && !this.getItems().isEmpty() ) {
            Iterator<String> i = this.getItems().iterator();
            while( i.hasNext() ) {
                list.add(new SelectItem(i.next()));
            }
        } else {
            Resources r = new Resources();
            list.add(new SelectItem(r.getString(this, "emptyListHelp")));
        }
        
        return list;
    }
    
    // getters / setters -------------------------------------------------------
    
    public void setItems(ArrayList<String> items) {
        this.items = items;
    }
    
    public ArrayList<String> getItems() {
        return items;
    }
    
    public void setSelectedItem(String selectedItem) {
        this.selectedItem = selectedItem;
    }
    
    public String getSelectedItem() {
        return selectedItem;
    }
    
    public void setNewItem(String newItem) {
        this.newItem = newItem;
    }
    
    public String getNewItem() {
        return newItem;
    }
    
    public void setShowingNewItemInput(boolean showingNewItemInput) {
        this.showingNewItemInput = showingNewItemInput;
    }
    
    public boolean isShowingNewItemInput() {
        return showingNewItemInput;
    }
    
    public void setValidPattern(String validPattern) {
        this.validPattern = validPattern;
    }
    
    public String getValidPattern() {
        return validPattern;
    }
    
    public void setInvalidMessageDetail(String invalidMessageDetail) {
        this.invalidMessageDetail = invalidMessageDetail;
    }
    
    public String getInvalidMessageDetail() {
        return invalidMessageDetail;
    }
    
    public void setEditingExisting(boolean editingExisting) {
        this.editingExisting = editingExisting;
    }
    
    public boolean isEditingExisting() {
        return editingExisting;
    }
}
