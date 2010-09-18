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
 * $Id: SamlV2RemoteIdpCreateWizardBean.java,v 1.3 2009/06/30 08:30:39 asyhuang Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.handler.SamlV2RemoteIdpCreateWizardHandler;
import java.io.Serializable;

public class SamlV2RemoteIdpCreateWizardBean
        extends SamlV2RemoteCreateWizardBean
        implements Serializable {

    private String metaUrl;
    private SamlV2RemoteIdpCreateWizardHandler
            samlV2RemoteIdpCreateWizardHandler;
    private RealmSamlV2RemoteIdpCreateSummary realmSamlV2RemoteIdpCreateSummary =
            new RealmSamlV2RemoteIdpCreateSummary(this);
    private StdMetadataNameSamlV2RemoteIdpCreateSummary
            stdMetadataNameSamlV2RemoteIdpCreateSummary =
            new StdMetadataNameSamlV2RemoteIdpCreateSummary(this);
    private CotSamlV2RemoteIdpCreateSummary cotSamlV2RemoteIdpCreateSummary =
            new CotSamlV2RemoteIdpCreateSummary(this);
    private MetaUrlSamlV2RemoteIdpCreateSummary
            metaUrlSamlV2RemoteIdpCreateSummary =
            new MetaUrlSamlV2RemoteIdpCreateSummary(this);

    public SamlV2RemoteIdpCreateWizardBean() {
        super();
    }

    @Override
    public void reset() {
        super.reset();
        metaUrl = null;
        realmSamlV2RemoteIdpCreateSummary =
                new RealmSamlV2RemoteIdpCreateSummary(this);
        stdMetadataNameSamlV2RemoteIdpCreateSummary =
                new StdMetadataNameSamlV2RemoteIdpCreateSummary(this);
        cotSamlV2RemoteIdpCreateSummary =
                new CotSamlV2RemoteIdpCreateSummary(this);
        metaUrlSamlV2RemoteIdpCreateSummary =
                new MetaUrlSamlV2RemoteIdpCreateSummary(this);
    }

    public String getMetaUrl() {
        return metaUrl;
    }

    public void setMetaUrl(String metaUrl) {
        this.metaUrl = metaUrl;
    }

    public SamlV2RemoteIdpCreateWizardHandler
            getSamlV2RemoteIdpCreateWizardHandler() {
        return samlV2RemoteIdpCreateWizardHandler;
    }

    public void setSamlV2RemoteIdpCreateWizardHandler(
            SamlV2RemoteIdpCreateWizardHandler
            samlV2RemoteIdpCreateWizardHandler) {
        this.samlV2RemoteIdpCreateWizardHandler =
                samlV2RemoteIdpCreateWizardHandler;
    }

    public RealmSamlV2RemoteIdpCreateSummary
            getRealmSamlV2RemoteIdpCreateSummary() {
        return realmSamlV2RemoteIdpCreateSummary;
    }

    public CotSamlV2RemoteIdpCreateSummary
            getCotSamlV2RemoteIdpCreateSummary() {
        return cotSamlV2RemoteIdpCreateSummary;
    }

    public StdMetadataNameSamlV2RemoteIdpCreateSummary
            getStdMetadataNameSamlV2RemoteIdpCreateSummary() {
        return stdMetadataNameSamlV2RemoteIdpCreateSummary;
    }

    public MetaUrlSamlV2RemoteIdpCreateSummary
            getMetaUrlSamlV2RemoteIdpCreateSummary() {
        return metaUrlSamlV2RemoteIdpCreateSummary;
    }
}
