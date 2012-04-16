/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CCMapListModel.java,v 1.2 2008/07/28 23:43:36 veiming Exp $
 */

package com.sun.identity.console.ui.model;

import com.iplanet.jato.view.html.OptionList;
import com.sun.web.ui.model.CCEditableListModel;

/**
 * Model for <code>CCMapListView</code>.
 */
public class CCMapListModel extends CCEditableListModel {
    private String keyLabel;
    private String valueLabel;
    private String msgInvalidKey;
    private String msgInvalidValue;
    private String msgInvalidNoKey;
    private String msgInvalidEntry;

    /**
     * Returns invalid entry message.
     *
     * @return invalid entry message.
     */
    public String getMsgInvalidEntry() {
        return msgInvalidEntry;
    }

    /**
     * Set invalid entry message.
     *
     * @param msgInvalidEntry Invalid entry message.
     */
    public void setMsgInvalidEntry(String msgInvalidEntry) {
        this.msgInvalidEntry = msgInvalidEntry;
    }

    /**
     * Returns invalid key message.
     *
     * @return invalid key message.
     */
    public String getMsgInvalidKey() {
        return msgInvalidKey;
    }

    /**
     * Set invalid key message.
     *
     * @param msgInvalidKey Invalid key message.
     */
    public void setMsgInvalidKey(String msgInvalidKey) {
        this.msgInvalidKey = msgInvalidKey;
    }

    /**
     * Returns invalid value message.
     *
     * @return invalid value message.
     */
    public String getMsgInvalidValue() {
        return msgInvalidValue;
    }

    /**
     * Returns invalid value (no key in map)  message.
     *
     * @return invalid value (no key in map)  message.
     */
    public String getMsgMapListInvalidNoKey() {
        return msgInvalidNoKey;
    }

    /**
     * Set invalid value message.
     *
     * @param msgInvalidValue Invalid value message.
     */
    public void setMsgInvalidValue(String msgInvalidValue) {
        this.msgInvalidValue = msgInvalidValue;
    }

    /**
     * Set invalid value (no key in map) message.
     *
     * @param msgInvalidNoKey Invalid value (no key in map) message.
     */
    public void setMsgInvalidNoKey(String msgInvalidNoKey) {
        this.msgInvalidNoKey = msgInvalidNoKey;
    }
    
    
    /**
     * Returns label for Key text box.
     *
     * @return label for Key text box.
     */
    public String getKeyLabel() {
        return keyLabel;
    }

    /**
     * Set label for Key text box.
     *
     * @param keyLabel label for Key text box.
     */
    public void setKeyLabel(String keyLabel) {
        this.keyLabel = keyLabel;
    }

    /**
     * Returns label for Value text box.
     *
     * @return label for Value text box.
     */
    public String getValueLabel() {
        return valueLabel;
    }

    /**
     * Set label for Value text box.
     *
     * @param valueLabel label for Value text box.
     */
    public void setValueLabel(String valueLabel) {
        this.valueLabel = valueLabel;
    }

    /**
     * Returns option list.
     *
     * @return option list.
     */
    public OptionList getOptionList() {
        return super.getOptionList();
    }
}
