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
 * $Id: WssCreateWizardSummarySignEncrypt.java,v 1.2 2009/07/23 20:46:53 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ListFormatter;
import com.sun.identity.admin.Resources;
import java.io.Serializable;
import java.util.ArrayList;

public class WssCreateWizardSummarySignEncrypt
        extends WssCreateWizardSummary
        implements Serializable
{

    public WssCreateWizardSummarySignEncrypt(WssCreateWizardBean wssCreateWizardBean)
    {
        super(wssCreateWizardBean);
    }

    @Override
    public int getGotoStep() {
        return WssCreateWizardStep.WSP_SIGN_ENCRYPT.toInt();
    }

    @Override
    public String getIcon() {
        return "../image/edit.png";
    }

    @Override
    public String getLabel() {
        Resources r = new Resources();
        String label = r.getString(this, "label");
        return label;
    }

    @Override
    public String getTemplate() {
        return null;
    }

    @Override
    public String getValue() {
        WssCreateWizardBean wizardBean
                = (WssCreateWizardBean) getWssCreateWizardBean();
        Resources r = new Resources();
        ArrayList a = new ArrayList();

        // yeah, ugly
        if( wizardBean.isResponseSigned() ) {
            a.add(" " + r.getString(this, "responseSigned"));
        }
        if( wizardBean.isResponseEncrypted() ) {
            a.add(" " + r.getString(this, "responseEncrypted"));
        }
        if( wizardBean.isRequestSignatureVerified() ) {
            a.add(" " + r.getString(this, "requestSignatureVerified"));
        }
        if( wizardBean.isRequestHeaderDecrypted() ) {
            a.add(" " + r.getString(this, "requestHeaderDecrypted"));
        }
        if( wizardBean.isRequestDecrypted() ) {
            a.add(" " + r.getString(this, "requestDecrypted"));
        }

        if( a.isEmpty() ) {
            a.add(r.getString(this, "noSignEncrypt"));
        }

        ListFormatter lf = new ListFormatter(a);
        return lf.toString();
    }

    @Override
    public boolean isExpandable() {
        return false;
    }

}
