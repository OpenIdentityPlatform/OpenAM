/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LdapSPValidator.java,v 1.6 2009/10/29 17:35:07 ericow Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.iplanet.services.cdc;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.dpro.session.DNOrIPAddressListTokenRestriction;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import org.forgerock.openam.dpro.session.NoOpTokenRestriction;


public class LdapSPValidator implements SPValidator {

    private static final String LDAP_ATTR_NAME = 
        "sunIdentityServerDeviceKeyValue";
    private static final String LDAP_STATUS_ATTR_NAME = 
        "sunIdentityServerDeviceStatus";
    private static final String PROVIDER_ID_ATTR_NAME = "agentRootURL";
    private static final int PROVIDER_ID_ATTR_LEN = 13;
    private static final String HOSTNAME_ATTR_NAME = "hostname";
    private static final int HOSTNAME_ATTR_LEN = 9;
    private static final String REALM_NAME_ATTR = "Realm=";
    private static final String HTTPS = "https";
    private static final int HTTPS_DEFAULT_PORT = 443;
    private static final int HTTP_DEFAULT_PORT = 80;    
    
    private AMIdentityRepository amIdRepo = null;
    private Exception exception;

    public LdapSPValidator() {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        amIdRepo = new AMIdentityRepository(null, adminToken);
        if (amIdRepo == null) {
            exception = new IdRepoException(
                IdRepoBundle.getString("32"), "32");
        }
    }

    /**
     * Returns token restriction.
     * The method does the following operations:
     * <ol>
     * <li>Validates the AuthRequest by checking the Provider ID againt the
     *     agent instances in the directory</li>
     * <li>From the agent instance in the directory, checks if the agent is
     *     active and also checks the gotoURL is protected by the agent</li>
     * <li>Combines the hostnames and IP addresses valid for the agent
     *     and sets them as the restriction for the SSO Token</li>
     * </ol>
     *
     * @param request Federation Service Authentication Request.
     * @param gotoURL Goto URL.
     * @return token restriction.
     */
    public TokenRestriction validateAndGetRestriction(
        FSAuthnRequest request, 
        String gotoURL
    ) throws Exception {
        // Check for initialization exceptions
        if (exception != null) {
            throw (exception);
        }
        
        String realm=null;

        /*
         * Search directory for provider ID and if present
         * return DN, valid IP and hostnames as restriction
         */
        URL url = new URL(URLDecoder.decode(request.getProviderId(), "UTF-8"));
        String realmName = url.getQuery();
        if (realmName != null) {
           int idx = realmName.indexOf(REALM_NAME_ATTR);
           if (idx != -1) {
               realm = realmName.substring(idx+REALM_NAME_ATTR.length());
           }
        }
        
        StringBuffer rootPrefix = new StringBuffer(1024);
        rootPrefix.append(url.getProtocol())
            .append("://")
            .append(url.getHost())
            .append(":")
            .append(url.getPort())
            .append("/");

        // Search for agent instances
        try {
            Map agents = searchAgents(rootPrefix, realm);

            // Make sure there is atleast one entry in the directory
            if (agents.isEmpty()) {
                if (CDCServlet.debug.warningEnabled()) {
                    CDCServlet.debug.warning(
                        "LdapSPValidator.validateAndGetRestriction: " +
                        "Invalid Agent Root URL: " + rootPrefix);
                }
                throw new Exception(
                    "Invalid Agent Root URL: " + rootPrefix + " not found.");
            }

            // Obtain the DNs and hostlists from the entries
            StringBuffer agentDN = null;
            ArrayList hostnames = new ArrayList();
            boolean gotoUrlValid = false;
            URL gotoUrl = new URL(gotoURL);
            String gotoHost = gotoUrl.getHost().toLowerCase();
            String gotoProtocol = gotoUrl.getProtocol().toLowerCase();
            int gotoPort = gotoUrl.getPort();
            //use default port when port is not specified explicitly
            if(gotoPort == -1){ 
                if(HTTPS.equalsIgnoreCase(gotoProtocol)){
                    gotoPort = HTTPS_DEFAULT_PORT;
                } else {
                    gotoPort = HTTP_DEFAULT_PORT;
                }
            }

            for (Iterator i = agents.keySet().iterator(); i.hasNext(); ) {
                AMIdentity amid = (AMIdentity)i.next();
                Map attributes = amid.getAttributes();

                if (attributes != null) {
                    if (isAgentActive(attributes)) {
                        Set attrValues = (Set)attributes.get(LDAP_ATTR_NAME);
                        if ((attrValues != null) && !attrValues.isEmpty()) {
                            getHostnames(attrValues, hostnames);
                            if (validateGotoUrl(attrValues,hostnames, gotoHost, 
                                gotoProtocol, gotoPort)
                            ) {
                                if (agentDN == null) {
                                    agentDN = new StringBuffer(50);
                                } else {
                                    agentDN.append("|");
                                }
                                agentDN.append(IdUtils.getDN(amid));
                                gotoUrlValid = true;
                            }
                        }
                    }
                }
            }

            if (!gotoUrlValid) {
                if (CDCServlet.debug.warningEnabled()) {
                    CDCServlet.debug.warning(
                        "LdapSPValidator.validateAndGetRestriction" + 
                        "Invalid GoTo URL: " + gotoURL + " for Agent ID: " + 
                        rootPrefix);
                }
                throw (new Exception(
                    "Goto URL not valid for the agent Provider ID"));
            }

            if (CDCServlet.debug.messageEnabled()) {
                CDCServlet.debug.message(
                    "LdapSPValidator.validateAndGetRestriction: " + 
                    "Restriction string for: " + 
                    rootPrefix + " is: " + agentDN + " " + hostnames);
            }

            if (!Boolean.valueOf(SystemConfigurationUtil.getProperty(Constants.IS_ENABLE_UNIQUE_COOKIE))) {
                return new NoOpTokenRestriction();
            } else {
                return new DNOrIPAddressListTokenRestriction(agentDN.toString(), hostnames);
            }
       } catch (Exception ex) {
            CDCServlet.debug.error(
                    "Invalid Agent: Could not get agent for the realm", ex);
            throw (new Exception(
                         "Invalid Agent: Could not get agent for the realm"));
       }
    }

    private Map searchAgents(StringBuffer rootPrefix, String realm)
        throws Exception {
        /*
         * Search for attribute "sunIdentityServerDeviceKeyValue:
         * sunIdentityServerAgentRootURL=<rootURL>"
         */
        Map searchParams = new HashMap();
                
        Set attrValues = new HashSet(2);
        attrValues.add(PROVIDER_ID_ATTR_NAME + "=" + rootPrefix.toString());
        searchParams.put(LDAP_ATTR_NAME, attrValues);
        
        IdSearchControl idsc = new IdSearchControl();
        idsc.setTimeOut(0);
        idsc.setMaxResults(0);
        idsc.setSearchModifiers(IdSearchOpModifier.AND, searchParams);
        
        Set returnAttrs = new HashSet(4);
        returnAttrs.add(LDAP_ATTR_NAME);
        returnAttrs.add(LDAP_STATUS_ATTR_NAME);
        idsc.setReturnAttributes(returnAttrs);
        
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            IdSearchResults sr = null;
            if ((realm != null) && (realm.trim().length() > 0)) {
                AMIdentityRepository idRepo = new AMIdentityRepository(
                    adminToken, realm);
                sr = idRepo.searchIdentities(IdType.AGENT, "*", idsc);
            } else {
                sr = amIdRepo.searchIdentities(IdType.AGENT, "*", idsc);
            }
            return sr.getResultAttributes();
        } catch (IdRepoException ire) {
            CDCServlet.debug.error("LdapSPValidator.searchAgents", ire);
            throw new Exception(ire);
        } catch (SSOException ssoe) {
            CDCServlet.debug.error("LdapSPValidator.searchAgents", ssoe);
            throw new Exception(ssoe);
        }
    }

    private boolean isAgentActive(Map attributes) {
        boolean agentIsActive = false;
        if (attributes != null) {
            Set attrvalues = (Set)attributes.get(LDAP_STATUS_ATTR_NAME);
            if ((attrvalues != null) && !attrvalues.isEmpty()) {
                String status = (String)attrvalues.iterator().next();
                agentIsActive = status.equalsIgnoreCase("Active");
            }
        }
        return agentIsActive;
    }

    private boolean validateGotoUrl(
        Set attrValues,
        List hostnames,
        String gotoHost,
        String gotoProtocol, 
        int gotoPort
    ) throws MalformedURLException {
        boolean valid = false;
        for (Iterator i = attrValues.iterator(); i.hasNext();) {
            String value = (String)i.next();
            if (value.startsWith(PROVIDER_ID_ATTR_NAME)) {
                URL u = new URL(value.substring(PROVIDER_ID_ATTR_LEN));
                hostnames.add(u.getHost());
                valid |= u.getHost().toLowerCase().equals(gotoHost) &&
                    u.getProtocol().toLowerCase().equals(gotoProtocol) &&
                    (u.getPort() == gotoPort);
            }
        }
        return valid;
    }
    
    private void getHostnames(Set attrValues, List hostnames) {
        if ((attrValues != null) && !attrValues.isEmpty()) {
            for (Iterator i = attrValues.iterator(); i.hasNext();) {
                String value = (String)i.next();
                if (value.startsWith(HOSTNAME_ATTR_NAME)) {
                    hostnames.add(value.substring(HOSTNAME_ATTR_LEN));
                }
            }
        }
    }
}
