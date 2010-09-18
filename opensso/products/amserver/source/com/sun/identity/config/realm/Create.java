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
 * $Id: Create.java,v 1.4 2008/06/25 05:42:38 qcheng Exp $
 *
 */
package com.sun.identity.config.realm;

import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.util.LDAPStoreValidator;
import com.sun.identity.config.pojos.Realm;
import com.sun.identity.config.pojos.RealmRole;
import com.sun.identity.config.pojos.RealmUser;
import net.sf.click.control.ActionLink;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Jeffrey Bermudez
 */
public class Create extends AjaxPage {

    protected LDAPStoreValidator ldapStoreValidator = new LDAPStoreValidator(this);
    
    public ActionLink validateRealmNameLink = new ActionLink("validateRealmName", this, "validateRealmName");
    public ActionLink validateHostNameLink = new ActionLink("validateHostName", this, "validateHostName");

    public ActionLink validateStoreNameLink = new ActionLink("validateStoreName", ldapStoreValidator, "validateStoreName");
    public ActionLink validateHostLink = new ActionLink("validateHost", ldapStoreValidator, "validateHost");
    public ActionLink validatePortLink = new ActionLink("validatePort", ldapStoreValidator, "validatePort");
    public ActionLink validateLoginLink = new ActionLink("validateLogin", ldapStoreValidator, "validateLogin");
    public ActionLink validatePasswordLink = new ActionLink("validatePassword", ldapStoreValidator, "validatePassword");
    public ActionLink validateBaseDNLink = new ActionLink("validateBaseDN", ldapStoreValidator, "validateBaseDN");


    public boolean validateRealmName() {
        String realmName = toString("realmName");
        if (realmName == null) {
            writeJsonResponse(false, "Field required");
        }
        else {
            Realm realm = getConfigurator().getRealm(realmName);
            boolean isValid = (realm == null);
            writeJsonResponse(isValid, (isValid) ? "Unique Realm name" : "The Realm name is already in use");
        }
        setPath(null);
        return false;
    }

    public boolean validateHostName() {
        String hostName = toString("hostName");
        if (hostName == null) {
            writeJsonResponse(false, "Field required");
        }
        else {
            boolean isValid = getConfigurator().validateHostName(hostName);
            if (isValid) {
                boolean isResolvable = getConfigurator().resolveHostName(hostName);
                writeJsonResponse(isResolvable, (isResolvable) ? "Valid and resolvable" : "Valid but not resolvable");
            }
            else {
                writeJsonResponse(isValid, "Invalid hostname");
            }
        }
        setPath(null);
        return false;
    }

    public void onPost() {
        String roleName = toString("roleName");
        String adminsArray = toString("adminsArray");
        String realmUsersArray = toString("realmUsersArray");
        String realmName = toString("realmName");

        if (adminsArray != null) {
            List adminsList = convertToRealmUserList(adminsArray, roleName);
            assignAdministrators(realmName, adminsList);
        }

        if (realmUsersArray != null) {
            List realmUsersList = convertToRealmUserList(realmUsersArray, null);
            removeAdministrators(realmName, realmUsersList);
        }
    }

    protected void assignAdministrators(String realmName, List administrators) {
        Realm realm = new Realm();
        realm.setName(realmName);
        getConfigurator().assignAdministrators(realm, administrators);
    }

    protected void removeAdministrators(String realmName, List administrators) {
        Realm realm = new Realm();
        realm.setName(realmName);
        getConfigurator().removeAdministrators(realm, administrators);
    }

    protected List convertToRealmUserList(String array, String roleName) {
        List result = new ArrayList();
        String[] values = array.split(",");
        for (int i = 0; i < values.length; i++) {
            String[] data = values[i].split("\\.");
            RealmRole realmRole = null;

            if (roleName != null) {
                realmRole = new RealmRole();
                realmRole.setName(roleName);
            }

            RealmUser realmUser = new RealmUser();
            realmUser.setFirstName(data[0]);
            realmUser.setLastName(data[1]);
            realmUser.setRealmRole(realmRole);
            result.add(realmUser);
        }
        return result;
    }

}
