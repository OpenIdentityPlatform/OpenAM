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
 * $Id: StsCreateWizardSummarySignEncrypt.java,v 1.2 2009/08/03 22:25:31 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ListFormatter;
import com.sun.identity.admin.Resources;
import java.io.Serializable;
import java.util.ArrayList;

public class StsCreateWizardSummarySignEncrypt
        extends StsCreateWizardSummary
        implements Serializable
{

    public StsCreateWizardSummarySignEncrypt(StsCreateWizardBean stsCreateWizardBean)
    {
        super(stsCreateWizardBean);
    }

    @Override
    public int getGotoStep() {
        return StsCreateWizardStep.SIGN_ENCRYPT.toInt();
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
        StsCreateWizardBean wizardBean
                = (StsCreateWizardBean) getStsCreateWizardBean();
        Resources r = new Resources();
        ArrayList a = new ArrayList();

        // yeah, ugly
        if( wizardBean.isRequestSigned() ) {
            a.add(" " + r.getString(this, "requestSigned"));
        }
        if( wizardBean.isRequestHeaderEncrypted() ) {
            a.add(" " + r.getString(this, "requestHeaderEncrypted"));
        }
        if( wizardBean.isRequestEncrypted() ) {
            a.add(" " + r.getString(this, "requestEncrypted"));
        }
        if( wizardBean.isResponseSignatureVerified() ) {
            a.add(" " + r.getString(this, "responseSignatureVerified"));
        }
        if( wizardBean.isResponseDecrypted() ) {
            a.add(" " + r.getString(this, "responseDecrypted"));
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
