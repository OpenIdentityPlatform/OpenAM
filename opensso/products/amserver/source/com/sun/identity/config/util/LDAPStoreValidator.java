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
 * $Id: LDAPStoreValidator.java,v 1.3 2008/06/25 05:42:41 qcheng Exp $
 *
 */
package com.sun.identity.config.util;

/**
 * @author Jeffrey Bermudez
 */
public class LDAPStoreValidator {

    private AjaxPage page;


    public LDAPStoreValidator(AjaxPage page) {
        this.page = page;
    }

    public boolean validateStoreName() {
        String storeName = page.toString("storeName");
        String response = "true";
        if (storeName == null) {
            response = "Please specify a user store name.";
        }
        page.writeToResponse(response);
        page.setPath(null);
        return false;
    }

    public boolean validateHost() {
        String host = page.toString("hostName");
        String response = "true";
        if (host == null) {
            response = "Please specify a user store name.";
        }
        page.writeToResponse(response);
        page.setPath(null);
        return false;
    }

    public boolean validatePort() {
        int port  = page.toInt("hostPort");
        String response = "true";
        if ( port < 1 || port > 65535 ) {
            response = "Please use a port from 1 to 65535";
        }
        page.writeToResponse(response);
        page.setPath(null);
        return false;
    }

    public boolean validateLogin() {
        String login = page.toString("login");
        String response = "true";
        if (login == null) {
            response = "Please specify a login ID.";
        }
        page.writeToResponse(response);
        page.setPath(null);
        return false;
    }

    public boolean validatePassword() {
        String password = page.toString("password");
        String response = "true";
        if (password == null) {
            response = "Please specify a password.";
        }
        page.writeToResponse(response);
        page.setPath(null);
        return false;
    }

    public boolean validateBaseDN() {
        String password = page.toString("baseDN");
        String response = "true";
        if (password == null) {
            response = "Please specify a Base DN.";
        }
        page.writeToResponse(response);
        page.setPath(null);
        return false;
    }

}
