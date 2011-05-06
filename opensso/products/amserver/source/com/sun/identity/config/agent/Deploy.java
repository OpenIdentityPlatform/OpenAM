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
 * $Id: Deploy.java,v 1.3 2008/06/25 05:42:32 qcheng Exp $
 *
 */
package com.sun.identity.config.agent;

import com.sun.identity.config.util.AjaxPage;

import java.util.List;

import net.sf.click.control.ActionLink;

/**
 * @author Les Hazlewood
 */
public class Deploy extends AjaxPage {
    public List agentTypes = getConfigurator().getAgentTypes();

    public ActionLink checkFAMServerURLLink = new ActionLink("checkFAMServerURL", this, "checkFAMServerURL");
    public ActionLink checkCredentialsLink = new ActionLink("checkCredentials", this, "checkCredentials");

    protected String getTitle() {
        return "deployAgent.title";
    }

    public boolean checkFAMServerURL() {
        try {
            getConfigurator().writeConfiguration();
            writeToResponse(Boolean.valueOf(getConfigurator().checkFAMServerURL(toString("url"))).toString());
        } catch( Exception e ) {
            writeToResponse(e.getMessage());
        }
        setPath(null);
        return false;
    }

    public boolean checkCredentials() {
        try {
            getConfigurator().writeConfiguration();
            writeToResponse(Boolean.valueOf(getConfigurator().checkCredentials(toString("profileName"), toString("profilePassword"))).toString());
        } catch( Exception e ) {
            writeToResponse(e.getMessage());
        }
        setPath(null);
        return false;
    }
}
