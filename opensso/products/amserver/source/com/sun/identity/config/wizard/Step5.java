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
 * $Id: Step5.java,v 1.9 2009/01/05 23:17:10 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */

package com.sun.identity.config.wizard;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.setup.SetupConstants;
import org.apache.click.control.ActionLink;
import java.net.URL;
import java.net.MalformedURLException;

public class Step5 extends AjaxPage {

    public ActionLink clearLink = new ActionLink(
        "clear", this, "clear");
    public ActionLink validateLink = new ActionLink(
        "validateURL",this,"validateURL");
    public ActionLink validateSiteLink = new ActionLink(
        "validateSite",this,"validateSite");

    public Step5() {}

    public void onInit() {
        String host = (String)getContext().getSessionAttribute(
            SessionAttributeNames.LB_SITE_NAME);
        String port = (String)getContext().getSessionAttribute(
            SessionAttributeNames.LB_PRIMARY_URL); 
        
        if (host != null) {
            addModel("host", host);
        }
        if (port != null) {
            addModel("port", port);
        }
        super.onInit();
    }

    public boolean clear() {
        getContext().removeSessionAttribute(SetupConstants.LB_SITE_NAME);
        getContext().removeSessionAttribute(SetupConstants.LB_PRIMARY_URL);
        setPath(null);
        return false;
    }

    /** 
     * host= site config name
     * port = primary loadURL
     */
    public boolean validateURL() {
        String primaryURL = toString("port");
        if (primaryURL == null) {
            writeToResponse(getLocalizedString("missing.primary.url"));
        } else {
            try {
                URL hostURL =  new URL(primaryURL);
                getContext().setSessionAttribute( 
                    SessionAttributeNames.LB_PRIMARY_URL, primaryURL);
                writeToResponse("ok");
            } catch (MalformedURLException m) {
                writeToResponse(getLocalizedString("primary.url.is.invalid"));
            }
        }

        setPath(null);
        return false;
    }

    public boolean validateSite() {
        boolean returnVal = true;
        String siteName = toString("host");
        if (siteName == null) {
            writeInvalid(getLocalizedString("missing.site.name"));
            returnVal = false;
        } else {
            getContext().setSessionAttribute( 
                SessionAttributeNames.LB_SITE_NAME, siteName);
            writeValid("ok.label");
        }

        setPath(null);
        return returnVal;
    }
}
