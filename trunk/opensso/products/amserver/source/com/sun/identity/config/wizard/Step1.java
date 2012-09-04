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
 * $Id: Step1.java,v 1.9 2009/01/05 23:17:10 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.config.wizard;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.config.util.AjaxPage;
import org.apache.click.control.ActionLink;

/**
 * This is the first step in the advanced configuration flow.
 * The user will be required to add the default admin password and
 * the agent passwords.
 */
public class Step1 extends AjaxPage {
    
    // these two links required for client side validation calls.
    public ActionLink validateLink = 
        new ActionLink("checkAdminPassword", this, "checkAdminPassword" );
    public ActionLink validateAgent = 
        new ActionLink("checkAgentPassword", this, "checkAgentPassword" );
    
    public boolean checkAdminPassword() {
        String adminPassword = toString("admin");
        String adminConfirm = toString("adminConfirm");
 
        if (adminPassword == null || adminConfirm == null) {        
            writeInvalid(getLocalizedString("missing.required.field"));
        } else {
            if (adminPassword.length() < 8) {
                writeInvalid(getLocalizedString("invalid.password.length"));
            } else if (!adminPassword.equals(adminConfirm)) {
                writeInvalid(getLocalizedString("passwords.do.not.match"));
            } else {
                writeValid("OK");
                getContext().setSessionAttribute(
                    SessionAttributeNames.CONFIG_VAR_ADMIN_PWD, adminPassword);
            }         
        }
        setPath(null);
        return false;
    }
    
    public boolean checkAgentPassword() {
        String agentPassword = toString("agent");
        String agentConfirm = toString("agentConfirm");
        String tmpadmin = (String)getContext().getSessionAttribute(
            SessionAttributeNames.CONFIG_VAR_ADMIN_PWD);
         
        if (agentPassword == null || agentConfirm == null) {        
            writeInvalid(getLocalizedString("missing.required.field"));
        } else if (agentPassword.equals(tmpadmin)) {
            writeInvalid(getLocalizedString("agent.admin.passwords.match"));
        } else if (agentPassword.length() < 8) {
            writeInvalid(getLocalizedString("invalid.password.length"));
        } else if (!agentPassword.equals(agentConfirm)) {
            writeInvalid(getLocalizedString("passwords.do.not.match"));             
        } else {
            writeValid("OK");
            getContext().setSessionAttribute(
                SessionAttributeNames.CONFIG_VAR_AMLDAPUSERPASSWD,
                agentPassword);              
        }
        setPath(null);
        return false;
    }    
}

