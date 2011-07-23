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
 * $Id: RegisterProduct.java,v 1.4 2008/08/19 21:17:57 bigfatrat Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.scn.client.util.InventoryEnvironmentTarget;
import com.sun.scn.client.comm.RegistrationWrapper;
import com.sun.scn.dao.Domain;
import com.sun.scn.servicetags.AuthenticationCredential;

import com.sun.identity.servicetag.registration.RegistrationAccount;
import com.sun.identity.servicetag.registration.RegistrationAccountConfig;
import com.sun.identity.servicetag.registration.RegistrationAccountFactory;
import com.sun.identity.servicetag.registration.RegistrationException;
import com.sun.identity.servicetag.registration.RegistrationService;
import com.sun.identity.servicetag.registration.RegistrationServiceConfig;
import com.sun.identity.servicetag.registration.RegistrationServiceFactory;
import com.sun.identity.servicetag.util.RegistrationUtil;

import com.sun.identity.shared.debug.Debug;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegisterProduct extends Task {

    public RegisterProduct() {
    }
    
    public String execute(Locale locale, Map params) throws WorkflowException
    {
        validateParameters(params);
        String newAccount = getString(params, "newAccount");
        if (newAccount.equals("true")) {
            String userName = getString(params, "tfUserName");
            String emailAddr = getString(params, "tfEmailAddr");
            if ((userName == null) || (userName.length() == 0)) {
                userName = emailAddr;
            }
            String pswd = getString(params, "tfPswd");
            String confirmPswd = getString(params, "tfCfrmPswd");
            String proxyHost = getString(params, "tfProxyHost");
            String proxyPort = getString(params, "tfProxyPort");
            String firstName = getString(params, "tfFirstName");
            String lastName = getString(params, "tfLastName");
            String country = getString(params, "tfCountry");

            Map map = new HashMap();
            map.put(RegistrationAccount.EMAIL, emailAddr);
            map.put(RegistrationAccount.PASSWORD, pswd);
            map.put(RegistrationAccount.COUNTRY, country);
            map.put(RegistrationAccount.USERID, userName);
            map.put(RegistrationAccount.FIRSTNAME, firstName);
            map.put(RegistrationAccount.LASTNAME, lastName);
            Object[] accountParams = { map };
            try {
                RegistrationService regService =
                    getRegServiceForRegister(proxyHost, proxyPort);
                if (regService == null) {
                    throw new WorkflowException(
                        "reg-no-service", null);
                }
                RegistrationAccountConfig accountConfig =
                    new RegistrationAccountConfig(
                        "com.sun.identity.servicetag.registration.SOAccount",
                        accountParams);
                RegistrationAccount account =
                    RegistrationAccountFactory.getInstance().
                        getRegistrationAccount(accountConfig);
                regService.createRegistrationAccount(account);
                regService.register(account);
            } catch (RegistrationException re) {
                Object[] param = {re.getMessage()};
                throw new WorkflowException("reg-create-soa-err", param);
            } catch (Exception ex) {
                Object[] param = {ex.getMessage()};
                throw new WorkflowException("reg-process-exc", param);
            }
            //  should be done/successful here
            return "localized msg1: new user account; product is registered.";
        } else {
            String userName = getString(params, "tfExistUserName");
            String pswd = getString(params, "tfExistPswd");
            String proxyHost = getString(params, "tfExistProxyHost");
            String proxyPort = getString(params, "tfExistProxyPort");
            String domain = getString(params, "tfDomain");
            if ((domain != null) && (domain.length() > 0)) {
                domain = domain.trim();
            }
            Map map = new HashMap();
            map.put (RegistrationAccount.USERID, userName);
            map.put (RegistrationAccount.PASSWORD, pswd);
            Object[] accountParams = { map };
            StringBuffer domainStrb = new StringBuffer();

            /*
             *  authenticate and retrieve the domains/teams for this
             *  userid.  if more than one, admin needs to select one.
             *  if only one, then use the "default".
             */

            List domains = null;
            RegistrationAccount account = null;
            RegistrationService regService = null;
            try {
                String REGISTRATOR_ID = "FederatedAccessManager";
                RegistrationWrapper regWrapper = 
                    new RegistrationWrapper(REGISTRATOR_ID);
                RegistrationAccountConfig accountConfig =
                    new RegistrationAccountConfig(
                        "com.sun.identity.servicetag.registration.SOAccount",
                        accountParams);
                account =
                    RegistrationAccountFactory.getInstance().
                        getRegistrationAccount(accountConfig);
                regService = getRegServiceForRegister (proxyHost, proxyPort);
                if (regService == null) {
                    throw new WorkflowException("reg-no-service-waccount");
                }
                regWrapper.setInventoryEnvironmentTarget(
                    InventoryEnvironmentTarget.valueOf("PROD"));
		    // 8.0: "DEVALPHA22" for dev, "BETA" for EA, "PROD" for RR
                AuthenticationCredential ac =
                    regService.getAuthCredential(account);
                domains = regWrapper.getDomains(ac);
            } catch (Exception ex) {
                Object[] param = {ex.getMessage()};
                throw new WorkflowException(
                    "reg-execute-registration-exc", param);
            }

            if ((domains == null) || (domains.isEmpty())) {
                throw new WorkflowException("reg-execute-no-domains");
            }

            /*
             *  if didn't select a domain, it means it's the first
             *  pass through the i-have-a-userid registration screen.
             *  if there are >1 domains for this userid, have the user
             *  select one.  if only one, then register to the "default"
             *  domain/team.
             */
            if ((domain != null) && (domain.length() > 0)) {
                /*
                 *  user selected a domain name.
                 *  have to find the Domain object that goes with this
                 *  (String) domain name... unfortunately.
                 */
                Domain dom = null;
                boolean foundDomain = false;
                if ((domains != null) && (!domains.isEmpty())) {
                    for (Iterator it = domains.iterator(); it.hasNext(); ) {
                        dom = (Domain)it.next();
                        if (domain.equals(dom.getDomainName())) {
                            foundDomain = true;
                            break;
                        }
                    }
                    if (foundDomain) {
                        // register in this domain
                        try {
                            regService.register (account, dom);
                        } catch (Exception ex) {
                            Object[] param = {domain};
                            throw new WorkflowException(
                                "reg-to-domain-error", param);
                        }
                        return (
                            "localized msg2: Product has been registered to " +
                            "the Domain '" + domain + "'.");
                    } else {
                        // oh-oh...
                        Object[] param = {domain};
                        throw new WorkflowException(
                            "reg-domain-not-found", param);
                    }
                } else {
                    // should not be!
                    throw new WorkflowException("reg-no-domains");
                }
            } else {
                /*
                 * no domain selected... that means this is the first
                 * pass through the i-have-a-userid screen.  check the
                 * list of domains; see if only one, or more than 1.
                 * if >1, return a tag to enable domain selection.
                 */
                if (domains.size() == 1) {
                    // if only one domain available (default one), just do it
                    try {
                        regService.register (account);
                    } catch (Exception ex) {
                        Object[] param = {ex.getMessage()};
                        throw new WorkflowException(
                            "reg-to-def-domain-error", param);
                    }
                    return "localized msg: the product is registered.";
                } else {
                    // otherwise, present list to user
                    for (Iterator it = domains.iterator(); it.hasNext(); ){
                        Domain dom = (Domain)it.next();
                        domainStrb.append(dom.getDomainName());
                        if (it.hasNext()) {
                            domainStrb.append("|");
                        }
                    }
                }
                return "<selectdomain>" + domainStrb.toString();
            }
        }
    }

    private void validateParameters(Map params) throws WorkflowException {
        //do your validation here, throw WorkflowException if something is wrong

        String newAccount = getString(params, "newAccount");
        if (newAccount.equals("false")) {
            String userName = getString(params, "tfExistUserName");
            String pswd = getString(params, "tfExistPswd");
            if ((userName == null) || (userName.length() == 0) ||
                (pswd == null) || (pswd.length() == 0))
            {
                throw new WorkflowException("reg-usrname-pswd-rqd");
            }
        } else {
            String userName = getString(params, "tfUserName");
            String pswd = getString(params, "tfPswd");
            String emailAddr = getString(params, "tfEmailAddr");
            String confirmPswd = getString(params, "tfCfrmPswd");
            String country = getString(params, "tfCountry");
            if (((userName == null) || (userName.length() == 0)) &&
                ((emailAddr == null) || (emailAddr.length() == 0)))
            {
                throw new WorkflowException("reg-emailadr-no-username");
            }
            if ((pswd == null) || (pswd.length() == 0)) {
                throw new WorkflowException("reg-pswd-req");
            } else if ((confirmPswd == null) || (confirmPswd.length() == 0)) {
                throw new WorkflowException("reg-cfm-pswd-req");
            } else if (!pswd.equals(confirmPswd)) {
                throw new WorkflowException("reg-pswd-no-match");
            }
        }
    }

    private RegistrationService getRegServiceForRegister (
        String proxy, String port) throws WorkflowException
    {
        if ((proxy != null) && (proxy.length() > 0) &&
            (port != null) && (port.length() > 0))
        {
            return getRegistrationService(proxy, Integer.valueOf(port));
        } else {
            return getRegistrationService();
        }
    }

    private RegistrationService getRegistrationService(String proxyHost,
        Integer proxyPort) throws WorkflowException
    {
        try {
            File registryFile = RegistrationUtil.getServiceTagRegistry();
            Object params[] =
                new Object[] {registryFile, proxyHost, proxyPort};
            /*
             * the first param used to be:
             * "com.sun.identity.servicetag.registration.\
             * SysnetRegistrationService"
             */

            RegistrationServiceConfig config =
                new RegistrationServiceConfig(
                    "com.sun.identity.servicetag.registration.SysnetRegistrationService",
                    params);
            RegistrationService registrationService =
                RegistrationServiceFactory.getInstance().
                    getRegistrationService(config);
            return registrationService;
        } catch (Exception ex) {
            Object[] param = {proxyHost, ex.getMessage()};
            throw new WorkflowException("reg-get-reg-svc-proxy-error", param);
        }
    }

    private RegistrationService getRegistrationService()
        throws WorkflowException
    {
        try {
            File registryFile = RegistrationUtil.getServiceTagRegistry();
            Object params[] = new Object[] { registryFile };
            RegistrationServiceConfig config =
                new RegistrationServiceConfig(
                   "com.sun.identity.servicetag.registration.SysnetRegistrationService",
                    params);
            RegistrationService registrationService =
                RegistrationServiceFactory.getInstance().
                    getRegistrationService(config);
            return registrationService;
        } catch (Exception ex) {
            Object[] param = {ex.getMessage()};
            throw new WorkflowException("reg-get-reg-svc-error", param);
        }
    }
}

