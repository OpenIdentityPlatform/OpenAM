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
 * $Id: LDAP.java,v 1.5 2008/06/25 05:42:33 qcheng Exp $
 *
 */
package com.sun.identity.config.authc;

import com.sun.identity.config.pojos.LDAPStore;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.util.LDAPStoreValidator;
import net.sf.click.control.ActionLink;

/**
 * @author Jeffrey Bermudez
 */
public class LDAP extends AjaxPage {

    protected LDAPStoreValidator ldapStoreValidator = new LDAPStoreValidator(this);

    public LDAPStore ldapStore = new LDAPStore();

    public ActionLink validateStoreNameLink = new ActionLink("validateStoreName", ldapStoreValidator, "validateStoreName");
    public ActionLink validateHostLink = new ActionLink("validateHost", ldapStoreValidator, "validateHost");
    public ActionLink validatePortLink = new ActionLink("validatePort", ldapStoreValidator, "validatePort");
    public ActionLink validateLoginLink = new ActionLink("validateLogin", ldapStoreValidator, "validateLogin");
    public ActionLink validatePasswordLink = new ActionLink("validatePassword", ldapStoreValidator, "validatePassword");
    public ActionLink validateBaseDNLink = new ActionLink("validateBaseDN", ldapStoreValidator, "validateBaseDN");


    public void onPost() {
        /*
        ldapStore.getRealm().setName(toString("realmName"));
        ldapStore.setName(toString("user_storeName"));
        ldapStore.setHostName(toString("user_hostName"));
        ldapStore.setHostPort(toInt("user_hostPort"));
        ldapStore.setHostPortSecure(toBoolean("user_hostPortSecure"));
        ldapStore.setUsername(toString("user_login"));
        ldapStore.setPassword(toString("user_password"));
        ldapStore.setBaseDN(toString("user_baseDN"));

        save(ldapStore);
         **/
    }

    protected void save(LDAPStore ldapStore) {
      //  getConfigurator().addAuthenticationStore(ldapStore);
    }

}
