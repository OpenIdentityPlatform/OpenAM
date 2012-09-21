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
 * $Id: StatusBean.java,v 1.4 2009/08/10 19:31:28 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.Token;
import com.sun.identity.common.DNUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMSystemConfig;
import java.io.Serializable;
import javax.faces.context.FacesContext;

public class StatusBean implements Serializable {
    public String getHelpLink() {
        FacesContext fc = FacesContext.getCurrentInstance();
        String vid = fc.getViewRoot().getViewId();
        Resources r = new Resources();
        String hl = r.getString(vid + ".help");
        if (hl == null) {
            hl = r.getString("_unknown.help");
        }

        return hl;
    }

    public String getLogoutLink() {
        String ll = AMSystemConfig.serverDeploymentURI + AMAdminConstants.URL_LOGOUT;
        return ll;
    }

    public String getShortUserName() {
        Token t = new Token();
        String un;
        try {
            String name = t.getSSOToken().getPrincipal().getName();
            un = DNUtils.DNtoName(name);
        } catch (SSOException ssoe) {
            throw new RuntimeException(ssoe);
        }

        return un;
    }

    public String getServerName() {
        String sn = AMSystemConfig.serverHost;
        return sn;
    }

    public String getVersion() {
        return AMSystemConfig.version;
    }

    public String getStandardConsoleLink() {
        return LinkBean.COMMON_TASKS.getRedirect();
    }
}
