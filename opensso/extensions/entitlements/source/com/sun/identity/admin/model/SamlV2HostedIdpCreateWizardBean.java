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
 * $Id: SamlV2HostedIdpCreateWizardBean.java,v 1.6 2009/07/13 23:22:03 asyhuang Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.AttributeMappingsDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.faces.model.SelectItem;

public class SamlV2HostedIdpCreateWizardBean
        extends SamlV2HostedCreateWizardBean
        implements Serializable {
    
    private List<SelectItem> availableSigningKeyList;
    private String selectedSigningKey;
    private List<ViewAttribute> availableViewAttributes = new ArrayList<ViewAttribute>();
    private List<ViewAttribute> viewAttributes = new ArrayList<ViewAttribute>();  
    private RealmSamlV2HostedIdpCreateSummary realmSamlV2HostedIdpCreateSummary =
            new RealmSamlV2HostedIdpCreateSummary(this);
    private EntityNameSamlV2HostedIdpCreateSummary entityNameSamlV2HostedIdpCreateSummary =
            new EntityNameSamlV2HostedIdpCreateSummary(this);
    private StdMetadataNameSamlV2HostedIdpCreateSummary stdMetadataNameSamlV2HostedIdpCreateSummary =
            new StdMetadataNameSamlV2HostedIdpCreateSummary(this);
    private ExtMetadataNameSamlV2HostedIdpCreateSummary extMetadataNameSamlV2HostedIdpCreateSummary =
            new ExtMetadataNameSamlV2HostedIdpCreateSummary(this);
    private AttributeMappingSamlV2HostedIdpCreateSummary attributeMappingSamlV2HostedIdpCreateSummary =
            new AttributeMappingSamlV2HostedIdpCreateSummary(this);
    private SigningKeySamlV2HostedIdpCreateSummary signingKeySamlV2HostedIdpCreateSummary =
            new SigningKeySamlV2HostedIdpCreateSummary(this);
    private CotSamlV2HostedIdpCreateSummary cotSamlV2HostedIdpCreateSummary = new CotSamlV2HostedIdpCreateSummary(this);

    public SamlV2HostedIdpCreateWizardBean() {
        super();
    }

    @Override
    public void reset() {
        super.reset();
        selectedSigningKey = null;
        viewAttributes.clear();

        realmSamlV2HostedIdpCreateSummary = new RealmSamlV2HostedIdpCreateSummary(this);
        entityNameSamlV2HostedIdpCreateSummary =
                new EntityNameSamlV2HostedIdpCreateSummary(this);
        stdMetadataNameSamlV2HostedIdpCreateSummary =
                new StdMetadataNameSamlV2HostedIdpCreateSummary(this);
        extMetadataNameSamlV2HostedIdpCreateSummary =
                new ExtMetadataNameSamlV2HostedIdpCreateSummary(this);
        attributeMappingSamlV2HostedIdpCreateSummary =
                new AttributeMappingSamlV2HostedIdpCreateSummary(this);
        signingKeySamlV2HostedIdpCreateSummary =
                new SigningKeySamlV2HostedIdpCreateSummary(this);
        cotSamlV2HostedIdpCreateSummary = new CotSamlV2HostedIdpCreateSummary(this);
    }

    public String getSelectedSigningKey() {
        return selectedSigningKey;
    }

    public void setSelectedSigningKey(String selectedSigningKey) {
        this.selectedSigningKey = selectedSigningKey;
    }

    public List<SelectItem> getAvailableSigningKeyList() {
        SigningKeysBean skbean = SigningKeysBean.getInstance();
        availableSigningKeyList = skbean.getSigningKeyBeanItems();
        SelectItem item = new SelectItem("", "-");
        availableSigningKeyList.add(0, item);
        return availableSigningKeyList;
    }

    //for attr mapping
    public List<ViewAttribute> getAvailableViewAttributes() {
        loadAvailableViewAttributes();
        return availableViewAttributes;
    }

    public void loadAvailableViewAttributes() {
        AttributeMappingsDao amdao = new AttributeMappingsDao();
        availableViewAttributes.clear();
        for (SamlV2ViewAttribute va : amdao.getViewAttributes()) {
            availableViewAttributes.add(va);
        }
    }

    public List<ViewAttribute> getViewAttributes() {
        return viewAttributes;
    }

    public String getToString() {
        return getListToString(viewAttributes);
    }

    public String getToFormattedString() {
        return getListToFormattedString(viewAttributes);
    }

    public static String getToFormattedString(List<ViewAttribute> vas) {
        return getListToFormattedString(vas);
    }

    private static String getListToString(List list) {
        StringBuffer b = new StringBuffer();

        for (Iterator<Resource> i = list.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append(",");
            }

        }
        return b.toString();
    }

    public List getToListOfStrings(List list) {
        List newList = new ArrayList();
        for (Iterator<Resource> i = list.iterator(); i.hasNext();) {
            newList.add(String.valueOf(i.next()));
        }
        return newList;
    }

    private static String getListToFormattedString(List list) {
        StringBuffer b = new StringBuffer();

        for (Iterator<Resource> i = list.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append("\n");
            }
        }
        return b.toString();
    }

    public RealmSamlV2HostedIdpCreateSummary getRealmSamlV2HostedIdpCreateSummary() {
        return realmSamlV2HostedIdpCreateSummary;
    }

    public AttributeMappingSamlV2HostedIdpCreateSummary getAttributeMappingSamlV2HostedIdpCreateSummary() {
        return attributeMappingSamlV2HostedIdpCreateSummary;
    }

    public EntityNameSamlV2HostedIdpCreateSummary getEntityNameSamlV2HostedIdpCreateSummary() {
        return entityNameSamlV2HostedIdpCreateSummary;
    }

    public SigningKeySamlV2HostedIdpCreateSummary getSigningKeySamlV2HostedIdpCreateSummary() {
        return signingKeySamlV2HostedIdpCreateSummary;
    }

    public CotSamlV2HostedIdpCreateSummary getCotSamlV2HostedIdpCreateSummary() {
        return cotSamlV2HostedIdpCreateSummary;
    }

    public StdMetadataNameSamlV2HostedIdpCreateSummary getStdMetadataNameSamlV2HostedIdpCreateSummary() {
        return stdMetadataNameSamlV2HostedIdpCreateSummary;
    }

    public ExtMetadataNameSamlV2HostedIdpCreateSummary getExtMetadataNameSamlV2HostedIdpCreateSummary() {
        return extMetadataNameSamlV2HostedIdpCreateSummary;
    }

}
