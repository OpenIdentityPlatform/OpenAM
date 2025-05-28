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
 * $Id: Step2.java,v 1.15 2010/01/04 19:08:36 veiming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.config.wizard;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.config.util.ProtectedPage;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.setup.SetupConstants;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openidentityplatform.openam.click.control.ActionLink;
import org.forgerock.openam.utils.StringUtils;

public class Step2 extends ProtectedPage {
    public ActionLink validateConfigDirLink = 
        new ActionLink("validateConfigDir", this, "validateConfigDir");
    public ActionLink validateCookieDomainLink = 
        new ActionLink("validateCookieDomain", this, "validateCookieDomain");

    @Override
    public void onInit() {
        String val = (String)getContext().getSessionAttribute(
            SessionAttributeNames.SERVER_URL);
        if (val == null) {
            val = getServerURL();
        }
        add("serverURL", val);

        val = (String)getContext().getSessionAttribute(
            SessionAttributeNames.COOKIE_DOMAIN);
        if (val == null) {
            val = getCookieDomain();
        }
        add("cookieDomain", val);

        val = (String)getContext().getSessionAttribute(
            SessionAttributeNames.PLATFORM_LOCALE);
        if (val == null) {
            val = SetupConstants.DEFAULT_PLATFORM_LOCALE;
        }
        add("platformLocale", val);

        String baseDir = null;
        String presetDir = AMSetupServlet.getPresetConfigDir();
        if ((presetDir == null) || (presetDir.trim().length() == 0)) {
            val = (String)getContext().getSessionAttribute(
                SessionAttributeNames.CONFIG_DIR);
            if (val == null) {
                val = getBaseDir(getContext().getRequest());
            }
            add("configDirectory", val);
            baseDir = val;
        } else {
            add("fixDir", "disabled");
            add("configDirectory", presetDir);
            baseDir = presetDir;
        }

        if (!hasWritePermission(baseDir)) {
            String deployURI = getContext().getServletContext().getContextPath();
            add("initialCheck", "<small><img class=\"pointer\" src=\"" + deployURI + 
                    "/assets/images/error.jpg\">" +
                    getLocalizedString("configuration.wizard.step2.no.write.permission.to.basedir") + 
                    "</small>");
        } else if (alreadyHasContent(baseDir)) {
            String deployURI = getContext().getServletContext().getContextPath();
            add("initialCheck", "<small><img class=\"pointer\" src=\"" + deployURI +
                    "/assets/images/error.jpg\">" +
                    getLocalizedString(
                "configuration.wizard.step2.basedir.already.has.content") + "</small>");
        } else {
            add("initialCheck", "");
        }
        
        super.onInit();
    }
    
    private static boolean alreadyHasContent(String dirName) {
        File f = new File(dirName); 
        
        if (f.exists() && f.isDirectory()) {
            return (f.list().length > 0);
        }
        
        return false;
    }

    private static boolean hasWritePermission(String dirName) {
        File f = new File(dirName);
        while ((f != null) && !f.exists()) {
            f = f.getParentFile();
        }
        return (f == null) ? false : f.isDirectory() && f.canWrite();
    }
    
    public boolean validateConfigDir() {
        String configDir = toString("dir");
        
        if (configDir == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else if (!hasWritePermission(configDir)) {
            writeToResponse(getLocalizedString(
                "configuration.wizard.step2.no.write.permission.to.basedir"));
        } else if (alreadyHasContent(configDir)) {
            writeToResponse(getLocalizedString(
                "configuration.wizard.step2.basedir.already.has.content"));
        } else {
            getContext().setSessionAttribute(
                SessionAttributeNames.CONFIG_DIR, configDir);
            writeToResponse("true");
        }
        setPath(null);        
        return false;    
    }

    public boolean validateCookieDomain() {
        String serverUrl = toString("serverurl");
        String domain = toString("domain");

        if (!ServicesDefaultValues.isCookieDomainValid(domain)) {
            writeToResponse(getLocalizedString("configurator.invalidcookiedomain"));
        } else if (mismatchedCookieDomain(serverUrl, domain)) {
             writeToResponse("warning" + getLocalizedString("configuration.wizard.step2.mismatched.cookie.domain"));
        } else {
            getContext().setSessionAttribute(SessionAttributeNames.COOKIE_DOMAIN, domain);
            writeToResponse("true");
        }
        setPath(null);
        return false;
    }
    
    private boolean mismatchedCookieDomain(String serverUrl, String domain) {
        if (StringUtils.isNotEmpty(serverUrl) && StringUtils.isNotEmpty(domain)) {
            try {
                URL url = new URL(serverUrl);
                return !url.getHost().endsWith(domain);
            } catch (MalformedURLException mue) {
                return false;
            }
        }

        return false;
    }

    private String getServerURL() {
        String hostname = (String)getContext().getRequest().getServerName();
        int portnum  = (int)getContext().getRequest().getServerPort();
        String protocol = (String)getContext().getRequest().getScheme();
        return protocol + "://" + hostname + ":" + portnum;
    }

    /**
     * used to add the key to the page and to the session so it can 
     * be retrieved when the final store is done
     */
    private void add(String key, String value) {
        addModel(key, value);
        getContext().setSessionAttribute(key, value);
    }
}
