/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Window.java,v 1.5 2008/06/25 05:42:33 qcheng Exp $
 *
 */
package com.sun.identity.config.authc;

import com.sun.identity.config.pojos.WindowStore;
import com.sun.identity.config.util.AjaxPage;
import net.sf.click.control.ActionLink;

/**
 * @author Jeffrey Bermudez
 */
public class Window extends AjaxPage {

    public WindowStore windowStore = new WindowStore();

    public ActionLink validateServicePrincipalLink = new ActionLink("validateServicePrincipal", this, "validateServicePrincipal");
    public ActionLink validateFileNameLink = new ActionLink("validateFileName", this, "validateFileName");    
    public ActionLink validateRealmLink = new ActionLink("validateRealm", this, "validateRealm");
    public ActionLink validateServiceNameLink = new ActionLink("validateServiceName", this, "validateServiceName");
    public ActionLink validateDomainNameLink = new ActionLink("validateDomainName", this, "validateDomainName");


    public boolean validateServicePrincipal() {
        return validateRequiredField("kerberosServicePrincipal");
    }

    public boolean validateFileName() {
        return validateRequiredField("kerberosFileName");
    }

    public boolean validateRealm() {
        return validateRequiredField("kerberosRealm");
    }

    public boolean validateServiceName() {
        return validateRequiredField("kerberosServiceName");
    }

    public boolean validateDomainName() {
        return validateRequiredField("domainName");
    }

    protected boolean validateRequiredField(String fieldName) {
        String servicePrincipal = toString(fieldName);
        boolean isValid = (servicePrincipal != null);
        writeJsonResponse(isValid, (isValid) ? "" : "Field required.");
        setPath(null);
        return false;
    }

    public void onPost() {
        windowStore.getRealm().setName(toString("realmName"));
        windowStore.setKerberosServicePrincipal(toString("kerberosServicePrincipal"));
        windowStore.setKerberosFileName(toString("kerberosFileName"));
        windowStore.setKerberosRealm(toString("kerberosRealm"));
        windowStore.setKerberosServiceName(toString("kerberosServiceName"));
        windowStore.setDomainName(toString("domainName"));

        save(windowStore);
    }

    protected void save(WindowStore windowStore) {
        getConfigurator().addAuthenticationStore(windowStore);
    }
}
