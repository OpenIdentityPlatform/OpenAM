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
 * $Id: SecurityMechanismPanelBean.java,v 1.2 2009/10/16 19:39:20 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

public class SecurityMechanismPanelBean implements Serializable {

    private static final String STYLE_CLASS_SUFFIX_ACTIVE = "Active";
    private static final String STYLE_CLASS_SUFFIX_CHECKED = "Checked";
    private static final String STYLE_CLASS_SUFFIX_DEFAULT = "Default";
    
    private SecurityMechanism securityMechanism;
    private boolean checked;
    private boolean expanded;
    
    // Convenience methods -----------------------------------------------------
    
    public String getConfigValue() {
        return securityMechanism != null
            ? securityMechanism.toConfigString() : null;
    }
    
    public String getLabel() {
        return securityMechanism != null 
            ? securityMechanism.toLocaleString() : null;
    }

    public String getSettingsTemplate() {
        String template;
        
        switch(this.getSecurityMechanism()) {
            case KERBEROS_TOKEN:
                template = "wss-kerberos-provider.xhtml";
                break;
            case USERNAME_TOKEN:
            case USERNAME_TOKEN_PLAIN:
                template = "wss-usernames.xhtml";
                break;
            case X509_TOKEN:
                template = "wss-x509.xhtml";
                break;
            default:
                template = null;
                break;
        }
        
        return template;
    }
    
    public String getStyleClass() {
        
        if( isCollapsible() ) {
            return STYLE_CLASS_SUFFIX_ACTIVE;
        } else if( isExpandable() ) {
            return STYLE_CLASS_SUFFIX_CHECKED;
        } else {
            return STYLE_CLASS_SUFFIX_DEFAULT;
        }
    }
    
    public boolean isExpandable() {
        return getSettingsTemplate() != null && !expanded && checked
            ? true : false;
    }
    
    public boolean isCollapsible() {
        return getSettingsTemplate() != null && expanded && checked
            ? true : false;
    }
    
    
    // Getters / Setters -------------------------------------------------------
    
    public void setSecurityMechanism(SecurityMechanism securityMechanism) {
        this.securityMechanism = securityMechanism;
    }

    public SecurityMechanism getSecurityMechanism() {
        return securityMechanism;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

}
