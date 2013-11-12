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
 * $Id: SamlV2HostedSpCreateWizardBean.java,v 1.5 2009/06/30 08:30:39 asyhuang Exp $
 */
package com.sun.identity.admin.model;

import java.io.Serializable;

public class SamlV2HostedSpCreateWizardBean
        extends SamlV2HostedCreateWizardBean
        implements Serializable {

    private boolean retrieveFileFromSpServerVisible = true;
    private boolean defAttrMappings;
    private RealmSamlV2HostedSpCreateSummary realmSamlV2CreateSummary = new RealmSamlV2HostedSpCreateSummary(this);
    private EntityNameSamlV2HostedSpCreateSummary entityNameSamlV2CreateSummary = new EntityNameSamlV2HostedSpCreateSummary(this);
    private StdMetadataNameSamlV2HostedSpCreateSummary stdMetadataNameSamlV2CreateSummary = new StdMetadataNameSamlV2HostedSpCreateSummary(this);
    private ExtMetadataNameSamlV2HostedSpCreateSummary extMetadataNameSamlV2CreateSummary = new ExtMetadataNameSamlV2HostedSpCreateSummary(this);
    private CotSamlV2HostedSpCreateSummary cotSamlV2CreateSummary = new CotSamlV2HostedSpCreateSummary(this);
    private DefAttrMappingsSamlV2HostedSpCreateSummary defAttrMappingsSamlV2CreateSummary = new DefAttrMappingsSamlV2HostedSpCreateSummary(this);

    public SamlV2HostedSpCreateWizardBean() {
        super();
    }

    public void reset() {
        super.reset();
        realmSamlV2CreateSummary = new RealmSamlV2HostedSpCreateSummary(this);
        entityNameSamlV2CreateSummary = new EntityNameSamlV2HostedSpCreateSummary(this);
        stdMetadataNameSamlV2CreateSummary = new StdMetadataNameSamlV2HostedSpCreateSummary(this);
        extMetadataNameSamlV2CreateSummary = new ExtMetadataNameSamlV2HostedSpCreateSummary(this);
        cotSamlV2CreateSummary = new CotSamlV2HostedSpCreateSummary(this);
    }

    public boolean getRetrieveFileFromSpServerVisible() {
        return retrieveFileFromSpServerVisible;
    }

    public void setRetrieveFileFromSpServerVisible(boolean retrieveFileFromSpServerVisible) {
        this.retrieveFileFromSpServerVisible = retrieveFileFromSpServerVisible;
    }

    public boolean getDefAttrMappings() {
        return defAttrMappings;
    }

    public void setDefAttrMappings(boolean defAttrMappings) {
        this.defAttrMappings = defAttrMappings;
    }

    public RealmSamlV2HostedSpCreateSummary getRealmSamlV2CreateSummary() {
        return realmSamlV2CreateSummary;
    }

    public EntityNameSamlV2HostedSpCreateSummary getEntityNameSamlV2CreateSummary() {
        return entityNameSamlV2CreateSummary;
    }

    public CotSamlV2HostedSpCreateSummary getCotSamlV2CreateSummary() {
        return cotSamlV2CreateSummary;
    }

    public DefAttrMappingsSamlV2HostedSpCreateSummary getDefAttrMappingsSamlV2CreateSummary() {
        return defAttrMappingsSamlV2CreateSummary;
    }

    public StdMetadataNameSamlV2HostedSpCreateSummary getStdMetadataNameSamlV2CreateSummary() {
        return stdMetadataNameSamlV2CreateSummary;
    }

    public ExtMetadataNameSamlV2HostedSpCreateSummary getExtMetadataNameSamlV2CreateSummary() {
        return extMetadataNameSamlV2CreateSummary;
    }
}
