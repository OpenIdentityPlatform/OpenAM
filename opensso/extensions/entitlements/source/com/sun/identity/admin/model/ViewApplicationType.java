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
 * $Id: ViewApplicationType.java,v 1.6 2009/08/10 15:18:38 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import com.sun.identity.entitlement.ApplicationType;
import java.io.Serializable;
import java.util.List;

public class ViewApplicationType implements Serializable {
    private String name;
    private List<Action> actions;
    private String resourceTemplate;
    private String applicationResourceTemplate;
    private String resourceClassName;
    private String entitlementApplicationType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public String getResourceTemplate() {
        return resourceTemplate;
    }

    public void setResourceTemplate(String resourceTemplate) {
        this.resourceTemplate = resourceTemplate;
    }

    public String getResourceClassName() {
        return resourceClassName;
    }

    public void setResourceClassName(String resourceClassName) {
        this.resourceClassName = resourceClassName;
    }

    public String getEntitlementApplicationType() {
        return entitlementApplicationType;
    }

    public void setEntitlementApplicationType(String entitlementApplicationType) {
        this.entitlementApplicationType = entitlementApplicationType;
    }

    public String getTitle() {
        Resources r = new Resources();
        String title = r.getString(this, "title." + name);
        if (title == null) {
            title = name;
        }
        return title;
    }

    public String getApplicationResourceTemplate() {
        return applicationResourceTemplate;
    }

    public void setApplicationResourceTemplate(String applicationResourceTemplate) {
        this.applicationResourceTemplate = applicationResourceTemplate;
    }
}
