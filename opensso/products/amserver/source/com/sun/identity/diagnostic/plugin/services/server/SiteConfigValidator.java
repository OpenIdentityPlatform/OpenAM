/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SiteConfigValidator.java,v 1.1 2008/11/22 02:41:22 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;

/**
 * This is a supporting class to validate the site configuration
 * properties.
 */
public class SiteConfigValidator extends ServerConfigBase {
    
    private IToolOutput toolOutWriter;
    
    public SiteConfigValidator() {
    }
    
    /**
     * Validate the site configuration.
     */
    public void validate(String path) {
        SSOToken ssoToken = null;
        toolOutWriter = ServerConfigService.getToolWriter();
        if (loadConfig(path)) {
            ssoToken = getAdminSSOToken();
            if (ssoToken != null) {
                processSites(ssoToken);
            } else {
                toolOutWriter.printError("svr-auth-msg");
            }
        } else {
            toolOutWriter.printStatusMsg(false, "site-validate-cfg-prop");
        }
    }
    
    private boolean loadConfig(String path) {
        boolean loaded = false;
        try {
            if (!loadConfigFromBootfile(path).isEmpty()) {
                loaded = true;
            }
        } catch (Exception e) {
            toolOutWriter.printError("cannot-load-properties",
                new String[] {path});
        }
        return loaded;
    }
    
    private void processSites(SSOToken ssoToken) {
        Set<String> allSites = new HashSet<String>();
        toolOutWriter.printMessage("site-validate-cfg-prop");
        try {
            Set sites = SiteConfiguration.getSites(ssoToken);
            for (Iterator items = sites.iterator(); items.hasNext();) {
                String item = (String) items.next();
                allSites.add(SiteConfiguration.getSitePrimaryURL(ssoToken,item));
                Set secondaryURLs = SiteConfiguration.getSiteSecondaryURLs(
                    ssoToken, item);
                if ((secondaryURLs != null) && !secondaryURLs.isEmpty()) {
                    for (Iterator<String> i = secondaryURLs.iterator();
                        i.hasNext(); ) {
                        allSites.add(i.next());
                    }
                }
            }
            if ((allSites != null) && !allSites.isEmpty()) {
                for (Iterator<String> i = allSites.iterator();
                    i.hasNext(); ) {
                    String site = i.next();
                    toolOutWriter.printMessage("\n");
                    toolOutWriter.printMessage("site-url-process-entry",
                        new String[] {site});
                    checkOrganizationAlias(ssoToken, site);
                    validateSiteUrlSyntax(site);
                }
                validateSiteServerEntries(ssoToken);
            } else {
                toolOutWriter.printMessage("site-not-configured");
            }
        } catch (Exception sms) {
            Debug.getInstance(DEBUG_NAME).error(
                "SiteConfigValidator.processSites: " +
                "SMS Exception", sms);
        }
    }
    
    private void validateSiteServerEntries(SSOToken ssoToken) {
        Set<String> allSites = new HashSet<String>();
        boolean valid = true;
        try {
            Set sites = SiteConfiguration.getSites(ssoToken);
            for (Iterator items = sites.iterator(); items.hasNext();) {
                String item = (String) items.next();
                allSites.add(new URL(SiteConfiguration.getSitePrimaryURL(
                    ssoToken, item)).getHost());
                Set failoverURLs =
                    SiteConfiguration.getSiteSecondaryURLs(ssoToken, item);
                if ((failoverURLs != null) && !failoverURLs.isEmpty()) {
                    for (Iterator<String> i = failoverURLs.iterator();
                        i.hasNext(); ) {
                        allSites.add((new URL(i.next())).getHost());
                    }
                }
            }
            Set<String> allServers = ServerConfiguration.getServers(ssoToken);
            if ((allServers != null) && !allServers.isEmpty()) {
                for (Iterator<String> i = allServers.iterator(); i.hasNext();) {
                    String serverURL = new URL(i.next()).getHost();
                    if (allSites.contains(serverURL)) {
                        toolOutWriter.printError("site-svr-url-in-site",
                            new String[] {serverURL});
                        valid = false;
                    }
                }
            }
        } catch (MalformedURLException mfe) {
            Debug.getInstance(DEBUG_NAME).error(
                "SiteConfigValidator.validateSiteServerEntries: " +
                "Invalid URL syntax Exception", mfe);
        } catch (Exception sms) {
            Debug.getInstance(DEBUG_NAME).error(
                "SiteConfigValidator.validateSiteServerEntries: " +
                "SMS Exception", sms);
        }
        toolOutWriter.printStatusMsg(valid, "site-in-svr-list");
    }
    
    private void validateSiteUrlSyntax(String site){
        boolean valid = true;
        try {
            String[] params = {site};
            toolOutWriter.printMessage("site-url-syntax");
            URL url = new URL(site);
            if (!isValidHost(url.getHost())) {
                toolOutWriter.printError("site-invalid-site-host",
                    new String[] {url.getHost()});
                valid = false;
            }
            toolOutWriter.printStatusMsg(valid, "site-host-validate");
            String deploymentURI = SystemProperties.get(
                AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
            valid = false;
            if (deploymentURI != null && deploymentURI.length()>0){
                int idx = site.indexOf(deploymentURI);
                if (idx != -1) {
                    if ((site.length() - deploymentURI.length()) == (idx -1)){
                        toolOutWriter.printError("site-uri-misplace", params);
                    } else {
                        valid = true;
                    }
                } else {
                    toolOutWriter.printError("site-url-syntax-err", params);
                }
            } else {
                toolOutWriter.printError("site-missing-svr-uri");
            }
            toolOutWriter.printStatusMsg(valid, "site-url-validate");
        } catch (MalformedURLException e) {
            Debug.getInstance(DEBUG_NAME).error(
                "SiteConfigValidator.validateSiteUrlSyntax: " +
                "Invalid URL syntax Exception", e);         
        }
    }
    
    private void checkOrganizationAlias(
        SSOToken ssoToken,
        String siteURL
    ) throws SMSException {
        String[] params = {siteURL};
        toolOutWriter.printMessage("site-org-alias-check");
        try {
            URL url = new URL(siteURL);
            if (!existsInOrganizationAlias(ssoToken, url.getHost())) {
                toolOutWriter.printError("site-org-alias-fail",
                    new String[] {url.getHost()});
            }
        } catch (MalformedURLException e) {
            toolOutWriter.printError("site-invalid-url", params);
        }
    }
}
