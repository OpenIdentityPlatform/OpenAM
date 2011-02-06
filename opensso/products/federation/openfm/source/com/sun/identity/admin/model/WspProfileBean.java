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
 * $Id: WspProfileBean.java,v 1.4 2009/11/13 17:16:57 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;

import com.icesoft.faces.context.effects.Effect;

public class WspProfileBean extends WssProfileBean implements Serializable {

    private ArrayList<SecurityMechanismPanelBean> securityMechanismPanels;
    private UserCredentialsTableBean userCredentialsTable;
    private String kerberosKeyTabFile;
    private Effect kerberosKeyTabFileInputEffect;
    private Effect kerberosKeyTabFileMessageEffect;
    private String authenticationChain;
    private String tokenConversionType;

    
    // convenience methods -----------------------------------------------------
    
    public boolean isUsingAnySamlSecurityMechanism() {
        boolean value = false;
        
        if( this.getSecurityMechanismPanels() != null ) {
            
            for(SecurityMechanismPanelBean panel : this.getSecurityMechanismPanels() ) {
                if( panel.isChecked() ) {
                    SecurityMechanism sm = panel.getSecurityMechanism();
                    
                    switch(sm) {
                        case SAML2_HOK:
                        case SAML2_SV:
                        case SAML_HOK:
                        case SAML_SV:
                            value = true;
                            break;
                    }
                    
                    if( value ) {
                        break;
                    }
                }
            }
        }
        
        return value;
    }
    
    // getters / setters -------------------------------------------------------
    
    public void setSecurityMechanismPanels(ArrayList<SecurityMechanismPanelBean> securityMechanismPanels) {
        this.securityMechanismPanels = securityMechanismPanels;
    }

    public ArrayList<SecurityMechanismPanelBean> getSecurityMechanismPanels() {
        return securityMechanismPanels;
    }

    public void setUserCredentialsTable(UserCredentialsTableBean userCredentialsTable) {
        this.userCredentialsTable = userCredentialsTable;
    }

    public UserCredentialsTableBean getUserCredentialsTable() {
        return userCredentialsTable;
    }

    public void setKerberosKeyTabFile(String kerberosKeyTabFile) {
        this.kerberosKeyTabFile = kerberosKeyTabFile;
    }

    public String getKerberosKeyTabFile() {
        return kerberosKeyTabFile;
    }

    public void setKerberosKeyTabFileInputEffect(
            Effect kerberosKeyTabFileInputEffect) {
        this.kerberosKeyTabFileInputEffect = kerberosKeyTabFileInputEffect;
    }

    public Effect getKerberosKeyTabFileInputEffect() {
        return kerberosKeyTabFileInputEffect;
    }

    public void setKerberosKeyTabFileMessageEffect(
            Effect kerberosKeyTabFileMessageEffect) {
        this.kerberosKeyTabFileMessageEffect = kerberosKeyTabFileMessageEffect;
    }

    public Effect getKerberosKeyTabFileMessageEffect() {
        return kerberosKeyTabFileMessageEffect;
    }

    public void setAuthenticationChain(String authenticationChain) {
        this.authenticationChain = authenticationChain;
    }

    public String getAuthenticationChain() {
        return authenticationChain;
    }

    public void setTokenConversionType(String tokenConversionType) {
        this.tokenConversionType = tokenConversionType;
    }

    public String getTokenConversionType() {
        return tokenConversionType;
    }
    
}
