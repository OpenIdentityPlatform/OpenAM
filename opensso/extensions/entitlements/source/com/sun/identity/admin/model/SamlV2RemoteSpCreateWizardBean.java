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
 * $Id: SamlV2RemoteSpCreateWizardBean.java,v 1.6 2009/06/30 08:30:39 asyhuang Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.AttributeMappingsDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SamlV2RemoteSpCreateWizardBean
        extends SamlV2RemoteCreateWizardBean
        implements Serializable {

    private String metaUrl;
    private List<ViewAttribute> availableViewAttributes = new ArrayList<ViewAttribute>();
    private List<ViewAttribute> viewAttributes = new ArrayList<ViewAttribute>();
    private RealmSamlV2RemoteSpCreateSummary realmSamlV2RemoteSpCreateSummary = new RealmSamlV2RemoteSpCreateSummary(this);
    private MetaUrlSamlV2RemoteSpCreateSummary metaUrlSamlV2RemoteSpCreateSummary = new MetaUrlSamlV2RemoteSpCreateSummary(this);
    private StdMetadataNameSamlV2RemoteSpCreateSummary stdMetadataNameSamlV2RemoteSpCreateSummary = new StdMetadataNameSamlV2RemoteSpCreateSummary(this);
    private CotSamlV2RemoteSpCreateSummary cotSamlV2RemoteSpCreateSummary = new CotSamlV2RemoteSpCreateSummary(this);
    private AttributeMappingSamlV2RemoteSpCreateSummary attributeMappingSamlV2RemoteSpCreateSummary = new AttributeMappingSamlV2RemoteSpCreateSummary(this);   

    public SamlV2RemoteSpCreateWizardBean() {
        super();
    }

    @Override
    public void reset() {
        super.reset();
        metaUrl = null;
        realmSamlV2RemoteSpCreateSummary = new RealmSamlV2RemoteSpCreateSummary(this);
        metaUrlSamlV2RemoteSpCreateSummary = new MetaUrlSamlV2RemoteSpCreateSummary(this);
        stdMetadataNameSamlV2RemoteSpCreateSummary = new StdMetadataNameSamlV2RemoteSpCreateSummary(this);
        cotSamlV2RemoteSpCreateSummary = new CotSamlV2RemoteSpCreateSummary(this);
        attributeMappingSamlV2RemoteSpCreateSummary = new AttributeMappingSamlV2RemoteSpCreateSummary(this);
    }

    public String getMetaUrl() {
        return metaUrl;
    }

    public void setMetaUrl(String metaUrl) {
        this.metaUrl = metaUrl;
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

    public RealmSamlV2RemoteSpCreateSummary getRealmSamlV2RemoteSpCreateSummary() {
        return realmSamlV2RemoteSpCreateSummary;
    }

    public MetaUrlSamlV2RemoteSpCreateSummary getMetaUrlSamlV2RemoteSpCreateSummary() {
        return metaUrlSamlV2RemoteSpCreateSummary;
    }

    public CotSamlV2RemoteSpCreateSummary getCotSamlV2RemoteSpCreateSummary() {
        return cotSamlV2RemoteSpCreateSummary;
    }

    public StdMetadataNameSamlV2RemoteSpCreateSummary getStdMetadataNameSamlV2RemoteSpCreateSummary() {
        return stdMetadataNameSamlV2RemoteSpCreateSummary;
    }

    public AttributeMappingSamlV2RemoteSpCreateSummary getAttributeMappingSamlV2RemoteSpCreateSummary() {
        return attributeMappingSamlV2RemoteSpCreateSummary;
    }
}
