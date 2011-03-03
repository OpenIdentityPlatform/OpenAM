/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * $Id: WscCreateSignEncryptSummary.java,v 1.3 2009/10/16 19:39:19 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.ArrayList;

import com.sun.identity.admin.ListFormatter;
import com.sun.identity.admin.Resources;

public class WscCreateSignEncryptSummary extends WscCreateWizardSummary {

    public WscCreateSignEncryptSummary(WscCreateWizardBean wizardBean) {
        super(wizardBean);
    }

    @Override
    public String getLabel() {
        Resources r = new Resources();
        String label = r.getString(this, "label");
        return label;
    }

    @Override
    public String getValue() {
        Resources r = new Resources();
        return r.getString(this, "value");
    }

    public String getStsPrivateKeyAlias() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        return wizardBean.getStsClientProfileBean().getPrivateKeyAlias();
    }
    
    public String getStsPublicKeyAlias() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        return wizardBean.getStsClientProfileBean().getPublicKeyAlias();
    }    

    public String getWscPrivateKeyAlias() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        return wizardBean.getWscProfileBean().getPrivateKeyAlias();
    }
    
    public String getWscPublicKeyAlias() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        return wizardBean.getWscProfileBean().getPublicKeyAlias();
    }    

    public String getFormattedStsMessageFlags() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        return getFormattedMessageFlags(wizardBean.getStsClientProfileBean());
    }

    public String getFormattedWscMessageFlags() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        return getFormattedMessageFlags(wizardBean.getWscProfileBean());
    }

    public String getFormattedStsEncryption() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        return getFormattedEncryption(wizardBean.getStsClientProfileBean());
    }

    public String getFormattedWscEncryption() {
        WscCreateWizardBean wizardBean = getWscCreateWizardBean();
        return getFormattedEncryption(wizardBean.getWscProfileBean());
    }

    private String getFormattedEncryption(WssProfileBean profile) {
        EncryptionAlgorithm ea = null;
        
        if( profile.getEncryptionAlgorithm() != null ) {
            ea = EncryptionAlgorithm.valueOf(profile.getEncryptionAlgorithm());
        }

        return (ea == null) ? null : ea.toLocaleString();
    }

    private String getFormattedMessageFlags(WssProfileBean profile) {
        ArrayList<String> a = new ArrayList<String>();
        Resources r = new Resources();

        if( profile.isRequestSigned() ) {
            a.add(" " + r.getString(this, "requestSigned"));
        }
        if( profile.isRequestHeaderEncrypted() ) {
            a.add(" " + r.getString(this, "requestHeaderEncrypted"));
        }
        if( profile.isRequestEncrypted() ) {
            a.add(" " + r.getString(this, "requestEncrypted"));
        }
        if( profile.isResponseSignatureVerified() ) {
            a.add(" " + r.getString(this, "responseSignatureVerified"));
        }
        if( profile.isResponseDecrypted() ) {
            a.add(" " + r.getString(this, "responseDecrypted"));
        }
        
        if( a.size() == 0 ) {
            a.add(r.getString(this, "none"));
        }

        ListFormatter lf = new ListFormatter(a);
        return lf.toString();
    }

    @Override
    public String getTemplate() {
        return "/admin/facelet/template/wsc-summary-sign-encrypt.xhtml";
    }

    @Override
    public String getIcon() {
        return "../image/edit.png";
    }

    @Override
    public boolean isExpandable() {
        return true;
    }

    @Override
    public int getGotoStep() {
        return WscCreateWizardStep.WSC_SIGN_ENCRYPT.toInt();
    }

}
