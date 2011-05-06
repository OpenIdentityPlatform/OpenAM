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
 * $Id: Certificate.java,v 1.4 2008/06/25 05:42:32 qcheng Exp $
 *
 */
package com.sun.identity.config.authc;

import com.sun.identity.config.pojos.CertificateStore;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.util.LDAPStoreValidator;
import net.sf.click.control.ActionLink;

/**
 * @author Jeffrey Bermudez
 */
public class Certificate extends AjaxPage {

    protected LDAPStoreValidator ldapStoreValidator = new LDAPStoreValidator(this);
    public CertificateStore certificateStore = new CertificateStore();

    public ActionLink validateStoreNameLink = new ActionLink("validateStoreName", ldapStoreValidator, "validateStoreName");
    public ActionLink validateHostLink = new ActionLink("validateHost", ldapStoreValidator, "validateHost");
    public ActionLink validatePortLink = new ActionLink("validatePort", ldapStoreValidator, "validatePort");
    public ActionLink validateLoginLink = new ActionLink("validateLogin", ldapStoreValidator, "validateLogin");
    public ActionLink validatePasswordLink = new ActionLink("validatePassword", ldapStoreValidator, "validatePassword");
    public ActionLink validateBaseDNLink = new ActionLink("validateBaseDN", ldapStoreValidator, "validateBaseDN");


    public void onPost() {
        certificateStore.getRealm().setName(toString("realmName"));
        certificateStore.setUserId(toString("userId"));
        certificateStore.setCheckAgainstLDAP(toBoolean("checkAgainstLDAP"));
        certificateStore.setCheckAgainstCRL(toBoolean("checkAgainstCRL"));
        certificateStore.setSearchAttribute(toString("searchAttribute"));
        certificateStore.setCheckAgainstOSCP(toBoolean("checkAgainstOSCP"));
        certificateStore.getUserStore().setName(toString("user_storeName"));
        certificateStore.getUserStore().setHostName(toString("user_hostName"));
        certificateStore.getUserStore().setHostPort(toInt("user_hostPort"));
        certificateStore.getUserStore().setHostPortSecure(toBoolean("user_hostPortSecure"));
        certificateStore.getUserStore().setUsername(toString("user_login"));
        certificateStore.getUserStore().setPassword(toString("user_password"));
        certificateStore.getUserStore().setBaseDN(toString("user_baseDN"));

        save(certificateStore);
    }

    protected void save(CertificateStore certificateStore) {
        getConfigurator().addAuthenticationStore(certificateStore);
    }
}
