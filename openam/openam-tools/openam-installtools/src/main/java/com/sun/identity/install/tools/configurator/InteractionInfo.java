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
 * $Id: InteractionInfo.java,v 1.3 2008/08/29 20:23:39 leiming Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;

/**
 * @author ap74890
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class InteractionInfo {

    public String toString() {
        StringBuffer buff = new StringBuffer("InteractionInfo:")
                .append(NEW_LINE);
        buff.append("    LookupKey    : ").append(getLookupKey());
        buff.append(NEW_LINE);
        buff.append("    i18n         : ").append(getI18NInfo());
        buff.append(NEW_LINE);
        buff.append("    Type         : ").append(getType());
        buff.append(NEW_LINE);
        buff.append("    Is-Required  : ").append(getRequiredFlag());
        buff.append(NEW_LINE);
        buff.append("    Is-Persistent: ").append(isPersistent());
        buff.append(NEW_LINE);
        buff.append("    optional-display: ").append(isDisplay());

        if (getSkipIfInfo() != null) {
            buff.append(NEW_LINE);
            buff.append("     skipIf        : ").append(getSkipIfInfo());
        }

        if (getValueNormalizerClass() != null) {
            buff.append(NEW_LINE);
            buff.append("     valueNormalizerClass    : ");
            buff.append(getValueNormalizerClass());
        }

        if (getDefaultValueFinderInfo() != null) {
            buff.append(NEW_LINE);
            buff.append("    DefaultValueFinderInfo: ").append(NEW_LINE);
            buff.append("        ").append(
                    getDefaultValueFinderInfo().toString());
            buff.append(NEW_LINE);
        }

        ValidationInfo[] vInfo = getValidations();
        buff.append("    ValidationInfo: ").append(NEW_LINE);
        for (int i = 0; i < vInfo.length; i++) {
            buff.append("        Validation[").append(i).append("] :");
            buff.append(vInfo[i].toString()).append(NEW_LINE);
        }

        return buff.toString();
    }

    public InteractionInfo(String lookupKey, SkipIfInfo skipIfInfo,
            I18NInfo i18nInfo, DefaultValueFinderInfo defaultValueFinderInfo,
            boolean required, boolean persistent, String type, 
            boolean display, String valueNormalizerClass) {
        setLookupKey(lookupKey);
        setSkipIfInfo(skipIfInfo);
        setI18NInfo(i18nInfo);
        setDefaultValueFinderInfo(defaultValueFinderInfo);
        setRequired(required);
        setPersistent(persistent);
        setType(type);
        setDisplay(display);
        setValueNormalizerClass(valueNormalizerClass);
    }

    public ValidationInfo[] getValidations() {
        ArrayList list = getValidationList();
        ValidationInfo[] result = new ValidationInfo[list.size()];
        System.arraycopy(list.toArray(), 0, result, 0, list.size());

        return result;
    }

    public void addValidationInfo(ValidationInfo info) {
        getValidationList().add(info);
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public I18NInfo getI18NInfo() {
        return i18nInfo;
    }

    public DefaultValueFinderInfo getDefaultValueFinderInfo() {
        return defaultValueFinderInfo;
    }

    public boolean getRequiredFlag() {
        return required;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public String getType() {
        return type;
    }

    public SkipIfInfo getSkipIfInfo() {
        return skipIfInfo;
    }

    public String getValueNormalizerClass() {
        return valueNormalizerClass;
    }

    public boolean isDisplay() {
        return display;
    }
    
    private void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }

    private void setI18NInfo(I18NInfo i18nInfo) {
        this.i18nInfo = i18nInfo;
    }

    private void setDefaultValueFinderInfo(DefaultValueFinderInfo info) {
        defaultValueFinderInfo = info;
    }

    private void setRequired(boolean required) {
        this.required = required;
    }

    private void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    private ArrayList getValidationList() {
        return validations;
    }

    private void setType(String type) {
        this.type = type;
    }

    private void setSkipIfInfo(SkipIfInfo skipIfInfo) {
        this.skipIfInfo = skipIfInfo;
    }

    private void setValueNormalizerClass(String className) {
        valueNormalizerClass = className;
    }
    
    private void setDisplay(boolean display) {
        this.display = display;
    }

    private String lookupKey;

    private I18NInfo i18nInfo;

    private DefaultValueFinderInfo defaultValueFinderInfo;

    private ArrayList validations = new ArrayList();

    private boolean required;

    private boolean persistent;

    private String type;
    
    private String valueNormalizerClass;

    private SkipIfInfo skipIfInfo;
    
    private boolean display;

    public static final String DEFAULT_INTER_TYPE = "install";

    public static final String NEW_LINE = System.getProperty("line.separator",
            "\n");

}
