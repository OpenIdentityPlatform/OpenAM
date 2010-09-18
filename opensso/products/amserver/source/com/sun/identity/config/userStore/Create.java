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
 * $Id: Create.java,v 1.3 2008/06/25 05:42:41 qcheng Exp $
 *
 */
package com.sun.identity.config.userStore;

import com.sun.identity.config.pojos.LDAPStore;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.util.LDAPStoreValidator;
import net.sf.click.control.ActionLink;

/**
 * @author Les Hazlewood
 */
public class Create extends AjaxPage {

    protected LDAPStoreValidator ldapStoreValidator = new LDAPStoreValidator(this);

    public LDAPStore userStore = null;

    public ActionLink validateStoreNameLink = new ActionLink("validateStoreName", ldapStoreValidator, "validateStoreName");
    public ActionLink validateHostLink = new ActionLink("validateHost", ldapStoreValidator, "validateHost");
    public ActionLink validatePortLink = new ActionLink("validatePort", ldapStoreValidator, "validatePort");
    public ActionLink validateLoginLink = new ActionLink("validateLogin", ldapStoreValidator, "validateLogin");
    public ActionLink validatePasswordLink = new ActionLink("validatePassword", ldapStoreValidator, "validatePassword");
    public ActionLink validateBaseDNLink = new ActionLink("validateBaseDN", ldapStoreValidator, "validateBaseDN");


    public void onInit() {
        userStore = new LDAPStore();
    }

    public void onPost() {
        userStore.setName(toString("user_storeName"));
        userStore.setHostName(toString("user_hostName"));
        userStore.setHostPort(toInt("user_hostPort"));
        userStore.setHostPortSecure(toBoolean("user_hostPortSecure"));
        userStore.setUsername(toString("user_login"));
        userStore.setPassword(toString("user_password"));
        userStore.setBaseDN(toString("user_baseDN"));
        save(userStore);
    }

    /**
     * Save LDAPStore object in the back-end.
     * TODO We are temporaly saving information only in the Session while a Model tier is created
     * @param userStore
     */
    protected void save( LDAPStore userStore) {
        getContext().setSessionAttribute("UserStore", userStore);
    }

}
