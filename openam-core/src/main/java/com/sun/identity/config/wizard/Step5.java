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
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.config.wizard;

import java.net.MalformedURLException;
import java.net.URL;

import org.openidentityplatform.openam.click.control.ActionLink;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.config.util.ProtectedPage;

/**
 * Wizard Step # 5: Site Name, URL and Session HA Failover indicator.
 * Session Failover indicator new @since 10.1
 *
 * This Step should be skipped when installing a secondary instance,
 * as this information will be replicated by the underlying store.
 *
 */
public class Step5 extends ProtectedPage {

    public ActionLink clearLink = new ActionLink(
            "clear", this, "clear");
    public ActionLink validateLink = new ActionLink(
            "validateURL", this, "validateURL");
    public ActionLink validateSiteLink = new ActionLink(
            "validateSite", this, "validateSite");

    public Step5() {
    }

    public void onInit() {
        String host = (String) getContext().getSessionAttribute(
                SessionAttributeNames.LB_SITE_NAME);
        String port = (String) getContext().getSessionAttribute(
                SessionAttributeNames.LB_PRIMARY_URL);
        if (host != null) {
            addModel("host", host);
        }
        if (port != null) {
            addModel("port", port);
        }
        // Initialize our Parent.
        super.onInit();
    }

    /**
     * Clear all Site / VIP / Load Balancer Settings
     *
     * @return boolean indicator to view.
     */
    public boolean clear() {
        getContext().removeSessionAttribute(SessionAttributeNames.LB_SITE_NAME);
        getContext().removeSessionAttribute(SessionAttributeNames.LB_PRIMARY_URL);
        setPath(null);
        return false;
    }

    /**
     * Validate the Site Name
     *
     * host= site config name
     * port = primary loadURL
     * Just a little confusing!
     */
    public boolean validateSite() {
        boolean returnVal = false;
        String siteName = toString("host");
        if ( (siteName == null) || (siteName.trim().isEmpty()) ) {
            writeInvalid(getLocalizedString("missing.site.name"));
            returnVal = true;
        } else {
            getContext().setSessionAttribute(
                    SessionAttributeNames.LB_SITE_NAME, siteName);
            writeValid("ok.label");
        }
        setPath(null);
        return returnVal;
    }

    /**
     * Validate the Site URL
     *
     * host= site config name
     * port = primary loadURL
     * Just a little confusing!
     */
    public boolean validateURL() {
        boolean returnVal = false;
        String primaryURL = toString("port");
        if ( (primaryURL == null) || (primaryURL.trim().isEmpty()) ) {
            writeToResponse(getLocalizedString("missing.primary.url"));
            returnVal = true;
        } else {
            try {
                URL hostURL = new URL(primaryURL);
                if ( (hostURL.getHost() == null) || (hostURL.getHost().trim().isEmpty()) ) {
                    writeToResponse(getLocalizedString("missing.host.name"));
                    returnVal = true;
                } else if ( (hostURL.getPath() == null) || (hostURL.getPath().trim().isEmpty()) ||
                            (hostURL.getPath().trim().equalsIgnoreCase("/")) ) {
                    writeToResponse(getLocalizedString("primary.url.no.uri"));
                    returnVal = true;
                } else {
                    getContext().setSessionAttribute(
                            SessionAttributeNames.LB_PRIMARY_URL, primaryURL);
                    writeToResponse("ok");
                }
            } catch (MalformedURLException m) {
                writeToResponse(getLocalizedString("primary.url.is.invalid"));
                returnVal = true;
            }
        }
        setPath(null);
        return returnVal;
    }

}
