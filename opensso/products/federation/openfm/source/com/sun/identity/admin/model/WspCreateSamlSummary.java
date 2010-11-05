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
 * $Id: WspCreateSamlSummary.java,v 1.3 2009/10/19 22:51:24 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;

public class WspCreateSamlSummary extends WspCreateWizardSummary {

    public WspCreateSamlSummary(WspCreateWizardBean wizardBean) {
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
        WspCreateWizardBean wizardBean = getWspCreateWizardBean();
        WspProfileBean profileBean = wizardBean.getWspProfileBean();
        Resources r = new Resources();
        SamlAttributesTableBean samlAttributeTable
            = profileBean.getSamlAttributesTable();
        int count = 0;
        String value = null;
        
        
        if( samlAttributeTable != null ) {

            for(SamlAttributeMapItem item : samlAttributeTable.getAttributeMapItems() ) {

                if( item.isCustom() ) {
                    count++;
                } else if( !item.isCustom() && item.getAssertionAttributeName() != null ) {
                    count++;
                }
            }
        }
        
        if( count == 0 ) {
            value = r.getString(this, "valueFormatNone");
        } else if( count == 1 ) {
            value = r.getString(this, "valueFormatSingle");
        } else {
            value = r.getString(this, "valueFormatMulti");
            value = value.replaceAll("\\{0\\}", String.valueOf(count));
        }

        return value;
   }

    @Override
    public String getTemplate() {
        return null;
    }

    @Override
    public String getIcon() {
        return "../image/edit.png";
    }

    @Override
    public boolean isExpandable() {
        return false;
    }

    @Override
    public int getGotoStep() {
        return StsManageWizardStep.SAML_CONFIG.toInt();
    }

}
