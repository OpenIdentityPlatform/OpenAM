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
 * $Id: Users.java,v 1.3 2008/06/25 05:42:38 qcheng Exp $
 *
 */
package com.sun.identity.config.realm;

import com.sun.identity.config.pojos.Realm;
import com.sun.identity.config.pojos.RealmRole;
import com.sun.identity.config.util.AjaxPage;

import java.util.List;

/**
 * @author Jeffrey Bermudez
 */
public class Users extends AjaxPage {

    public List users;
    public int totalUsers;

    public void onGet() {
        String realmName = toString("realmName");
        String filter = toString("filter");
        boolean getAdmins = toBoolean("getAdmins");
        String roleName = toString("roleName");
        Realm realm = new Realm();
        realm.setName(realmName);
        RealmRole realmRole = new RealmRole();
        realmRole.setName(roleName);

        if (getAdmins) {
            users = getConfigurator().getAdministrators(realm, realmRole);
        }
        else {
            users = getConfigurator().getUsers(realm, filter);
        }

        totalUsers = users.size();
    }

}
