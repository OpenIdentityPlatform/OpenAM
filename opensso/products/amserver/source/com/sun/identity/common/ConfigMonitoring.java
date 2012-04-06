/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConfigMonitoring.java,v 1.6 2009/12/23 23:50:21 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.common;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.SSOServerRealmInfo;
import com.sun.identity.monitoring.SSOServerMonConfig;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class gathers the configuration information for the
 * monitoring service, which is initially started in WebtopNaming.java
 * Configuration information can be gathered after Session services
 * have started up.
 */

public class ConfigMonitoring {
    Debug debug;
    SSOToken ssoToken;
    private List<String> realmList;
    /*
     *  in AMLoginModule.java, the requested realm/org (i.e., in the
     *  "?realm=xxx" parameter) is not available, so realm-specific
     *  auth module statistics can't be updated.  leave the code
     *  to gather the realms' auth module instances in, but don't
     *  call it for now.
     */
    private boolean skipGettingAuthModules = true;

    public ConfigMonitoring() {
    }

    /*
     *  this method is called by AMSetupServlet, when it's done
     *  configuring the OpenSSO server after deployment.  it's also
     *  called by the MonitoringConfiguration load-on-startup servlet
     *  when the OpenSSO server is restarted any time after being
     *  configured.  it completes the configuring of the monitoring
     *  agent with the config information that requires an SSOToken
     *  to retrieve.  there is another part of the configuration supplied
     *  to the agent by WebtopNaming.
     */
    public void configureMonitoring() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date1 = new Date();
        String startDate = sdf.format(date1);
        debug = Debug.getInstance("amMonitoring");
        String classMethod = "ConfigMonitoring.configureMonitoring: ";

        try {
            ssoToken = getSSOToken();
        } catch (SSOException ssoe) {
            debug.error(classMethod + "Could not get proper SSOToken", ssoe);
            return;
        }

        boolean isSessFOEnabled = false;
        try {
            SessionService ssvc = SessionService.getSessionService();
            if (ssvc != null) {
                isSessFOEnabled = ssvc.isSessionFailoverEnabled();
            } else {
                debug.error(classMethod + "unable to get session service");
            }
        } catch (Exception ex) {
            debug.error(classMethod + "exception getting session service; " +
                ex.getMessage());
        }

        Agent.setSFOStatus(isSessFOEnabled);

        /*
         * if monitoring disabled, go no further.  any error
         * from getMonServiceAttrs() or Agent.startAgent()
         * will result in monitoring getting disabled.
         */
        int i = getMonServiceAttrs();
        if (i != 0) {
            debug.error(classMethod + "getMonServiceAttrs returns " + i +
                ", monitoring disabled");
            Agent.setMonitoringDisabled();
            return;
        }

        HashMap<String, String> puMap = new HashMap<String, String>(); // sitename -> primary URL
        HashMap<String, String> siteMap = new HashMap<String, String>(); // primary URL -> sitename
        try {
            Set<String> siteNames = SiteConfiguration.getSites(ssoToken);
            // get primary url for each site
            if (siteNames.size() > 0) {
                for (Iterator<String> it = siteNames.iterator(); it.hasNext(); ) {
                    String site = it.next();
                    String purl =
                        SiteConfiguration.getSitePrimaryURL(ssoToken, site);
                    puMap.put(site, purl);
                    siteMap.put(purl, site);
                }
            }
        } catch (SMSException smex) {
            debug.error(classMethod + "SMS exception: " + smex.getMessage());
            Agent.stopRMI();
            Agent.setMonitoringDisabled();
            return;
        } catch (SSOException ssoex) {
            debug.error(classMethod + "SSO exception: " + ssoex.getMessage());
            Agent.stopRMI();
            Agent.setMonitoringDisabled();
            return;
        }
        Agent.siteNames(puMap, siteMap);

        getRealmsList("/");
        if (Agent.realmsConfig(realmList) != 0) {
            debug.error(classMethod + "no realm mbeans; monitoring disabled.");
            Agent.stopRMI();
            Agent.setMonitoringDisabled();
            return;
        }

        /*
         *  probably could combine getAllRealms() and getAllRealmsSpecific()
         *  to do auth modules, and agents and groups, when auth modules'
         *  statistics can be handled per realm.
         */
        if (!skipGettingAuthModules) {
            getAllRealms("/");
        }
        getAllRealmsSpecific("/");
        if (debug.messageEnabled()) {
            doSubRealms("/");  // start with the root realm ("/")
        }
        date1 = new Date();
        if (debug.messageEnabled()) {
            debug.message(classMethod + "\n" +
                "    Start time " + startDate + "\n" +
                "    End time = " + sdf.format(date1));
        }
    }

    private SSOToken getSSOToken() throws SSOException {
        return (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    }

    private void getRealmsList(String startRealm) {
        String classMethod = "ConfigMonitoring.getRealmsList: ";
        try {
            int rlmCnt = 1;  // for startRealm
            OrganizationConfigManager orgMgr =
                new OrganizationConfigManager(ssoToken, startRealm);
            Set orgs = orgMgr.getSubOrganizationNames("*", true);
            rlmCnt += orgs.size();
            realmList = new ArrayList<String>(rlmCnt);
            realmList.add(startRealm);
            for (Iterator it = orgs.iterator(); it.hasNext(); ) {
                String ss = "/" + (String)it.next();
                realmList.add(ss);
            }
        } catch (SMSException e) {
            debug.error(classMethod +
                "SMSException getting OrgConfigMgr: " + e.getMessage());
        }
    }


    private void getAllRealms(String startRealm) {
        String classMethod = "ConfigMonitoring.getAllRealms: ";
        StringBuilder sb = new StringBuilder(classMethod);
        if (debug.messageEnabled()) {
            sb.append("orgnames starting from ").append(startRealm).
            append(":\n").append("  ").append(startRealm).append("\n");
        }
        try {
            OrganizationConfigManager orgMgr =
                new OrganizationConfigManager(ssoToken, startRealm);
            Set orgs = orgMgr.getSubOrganizationNames("*", true);

            /*
             *  the orgs Set of realms seems to have some sort of
             *  ordering to it, going through each of "/"'s realms.
             *  don't know that we need to count on it, but it's
             *  nice.
             */
            // do the top-level realm first
            HashMap authHM = getRealmAuthMods("/");

            /*
             *  get agent information... just for info, not processing
             */
            if (debug.messageEnabled()) {
                getAgentTypes();
            }

            SSOServerRealmInfo srInfo =
                new SSOServerRealmInfo.SSOServerRealmInfoBuilder("/").
                   authModules(authHM).build();
            int i = Agent.realmConfigMonitoringAgent(srInfo);

            /*
             *  if realmConfigMonitoringAgent() had a problem with
             *  this realm, there's not much point in processing its
             *  subrealms...
             */
            if (i != 0) {
                debug.error(classMethod + "error processing root realm; " +
                    "skip subrealms.");
                return;
            }

            // then all the subrealms; they have leading "/"
            for (Iterator it = orgs.iterator(); it.hasNext(); ) {
                String ss = "/" + (String)it.next();
                if (debug.messageEnabled()) {
                    sb.append("  ").append(ss).append("\n");
                }
                /* get this realm's auth modules */
                try {
                    AMIdentityRepository idRepo =
                        new AMIdentityRepository(ssoToken, ss);
                    AMIdentity thisRealmAMId = idRepo.getRealmIdentity();
                    String currentRealmAMIdName = thisRealmAMId.getRealm();
                    Set s1 = getAuthModules(currentRealmAMIdName);
                    authHM = new HashMap();
                    if (!s1.isEmpty()) {
                        for(Iterator it2=s1.iterator(); it2.hasNext(); ) {
                            AMAuthenticationInstance ai =
                                (AMAuthenticationInstance)it2.next();
                            String stname = ai.getName();
                            String sttype = ai.getType();
                            authHM.put(stname, sttype);
                        }
                        /*
                         *  all get an "Application" instance/type by default
                         */
                        authHM.put("Application", "Application");
                    }

                    /*
                     *  get agent information
                     *  don't need with the *Specific versions... just
                     *  needed to see what attributes there were (and values)
                     */
                    srInfo =
                        new SSOServerRealmInfo.SSOServerRealmInfoBuilder(ss).
                           authModules(authHM).build();
                    i = Agent.realmConfigMonitoringAgent(srInfo);
                    /*
                     *  problem with this subrealm, but at least the
                     *  root realm was added.  just output error and do next
                     *  subrealm.
                     */
                    if (i != 0) {
                        debug.error(classMethod +
                            "error processing realm " + ss);
                    }
                } catch (IdRepoException ire) {
                    debug.error(classMethod +
                        "IdRepoException getting AMIdentityRepository" +
                        " object for realm: " + ss + ": " + ire.getMessage());
                } catch (SSOException ssoe) {
                    debug.error(classMethod +
                        "SSOException getting info for realm " + ss +
                        ": " + ssoe.getMessage());
                }
            }
            if (debug.messageEnabled()) {
                debug.message(sb.toString());
            }
        } catch (SMSException e) {
            debug.error(classMethod +
                "SMSException getting OrgConfigMgr: " + e.getMessage());
        }
    }


    /*
     *  this is like getAllRealms("/"), but refined to get the specific
     *  attributes needed.  probably the eventual version... for agents
     *  and agent groups, anyway.
     */
    private void getAllRealmsSpecific(String startRealm) {
        String classMethod = "ConfigMonitoring.getAllRealmsSpecific: ";
        StringBuilder sb = new StringBuilder(classMethod);
        if (debug.messageEnabled()) {
            sb.append("orgnames starting from ").append(startRealm).
                append(":\n").append("  ").append(startRealm).append("\n");
        }
        try {
            OrganizationConfigManager orgMgr =
                new OrganizationConfigManager(ssoToken, startRealm);
            Set orgs = orgMgr.getSubOrganizationNames("*", true);

            /*
             *  the orgs Set of realms seems to have some sort of
             *  ordering to it, going through each of "/"'s realms.
             *  don't know that we need to count on it, but it's
             *  nice.
             */
            /*
             *  get agent and agent group information
             */
            
            AMIdentityRepository idRepo = null;
            AMIdentity thisRealmAMId = null;
            String currentRealmAMIdName = null;
            try {
                idRepo = new AMIdentityRepository(ssoToken, "/");
                thisRealmAMId = idRepo.getRealmIdentity();
                currentRealmAMIdName = thisRealmAMId.getRealm();
                /*
                 *  get agents and agent groups information
                 */
                getAgentsAndGroupsInfo("/", idRepo, thisRealmAMId);
            } catch (IdRepoException ire) {
                 debug.error(classMethod +
                     "IdRepoException getting AMIdentityRepository" +
                     " object for realm: /: " + ire.getMessage());
                /*
                 *  if we can't get the AMIdentityRepository, there's
                 *  not much we can do
                 */
                return;
            } catch (SSOException ssoe) {
                 debug.error(classMethod +
                     "SSOException getting info for realm /: "
                     + ssoe.getMessage());
                /*
                 *  likewise, if there's an issue with our SSOToken...
                 *  there's not much we can do
                 */
                return;
            }

            // then all the subrealms; they have leading "/"
            for (Iterator it = orgs.iterator(); it.hasNext(); ) {
                String ss = "/" + (String)it.next();
                if (debug.messageEnabled()) {
                    sb.append("  ").append(ss).append("\n");
                }
                try {
                    idRepo = new AMIdentityRepository(ssoToken, ss);
                    thisRealmAMId = idRepo.getRealmIdentity();
                    currentRealmAMIdName = thisRealmAMId.getRealm();
                    /*
                     *  get agents and agent groups information
                     */
                    getAgentsAndGroupsInfo(ss, idRepo, thisRealmAMId);
                } catch (IdRepoException ire) {
                    debug.error(classMethod +
                        "IdRepoException getting AMIdentityRepository" +
                        " object for realm: " + ss + ": " + ire.getMessage());
                } catch (SSOException ssoe) {
                    debug.error(classMethod +
                        "SSOException getting info for realm " + ss +
                        ": " + ssoe.getMessage());
                }
            }
            if (debug.messageEnabled()) {
                debug.message(sb.toString());
            }
        } catch (SMSException e) {
            debug.error(classMethod +
                "SMSException getting OrgConfigMgr: " + e.getMessage());
        }
    }

    HashMap getRealmAuthMods(String realmName) {
        String classMethod = "ConfigMonitoring.getRealmAuthMods: ";
        HashMap aMods = new HashMap();
        try {
            AMAuthenticationManager mgr =
                new AMAuthenticationManager (ssoToken, realmName);
            Set insts = mgr.getAuthenticationInstances();
            for (Iterator it = insts.iterator(); it.hasNext(); ){
                AMAuthenticationInstance ai =
                    (AMAuthenticationInstance)it.next();
                String stname = ai.getName();
                String sttype = ai.getType();
                aMods.put(stname, sttype);
            }
            /*
             *  all get an "Application" instance/type by default
             */
            aMods.put("Application", "Application");
        } catch (AMConfigurationException e) {
            debug.error(classMethod + "getting auth instances; " +
                e.getMessage());
        }
        return aMods;
    }

    /*
     *  recursively process subrealms.
     *  gather per-realm configuration items:
     *    authentication modules
     *    2.2 agents
     *    J2EE agents
     *    J2EE agent groups
     *    Web agents
     *    Web agent groups
     *    COTs
     *    each COT's members
     *    IDPs
     *    SPs
     */
    private void doSubRealms(String realm) {
        String classMethod = "ConfigMonitoring.doSubRealms: ";
        try {
            // get this realm's identity
            AMIdentityRepository idRepo =
                new AMIdentityRepository(ssoToken, realm);
            AMIdentity thisRealmAMId = idRepo.getRealmIdentity();
            String currentRealmAMIdName = thisRealmAMId.getRealm();
            String currentAMIdName = thisRealmAMId.getName();

            // currentRealmAMIdName is fql; currentAMIdName is just realmname
            if (debug.messageEnabled()) {
                debug.message(classMethod + "this realm name = '" +
                    currentRealmAMIdName + "', name = '" +
                    currentAMIdName + "'");
            }

            // get this realm's subrealms
            Set subRealms = (idRepo.searchIdentities(IdType.REALM,
                "*", new IdSearchControl())).getSearchResults();
            if (subRealms.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + currentAMIdName +
                        " has no subrealms");
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message(classMethod + currentAMIdName + " has " +
                        subRealms.size() + " subrealms");
                }
                int num = 0;
                for (Iterator it = subRealms.iterator(); it.hasNext(); ) {
                    AMIdentity amid = (AMIdentity)it.next();
                    String ss = amid.getName();
                    // get assigned services
                    Set svcs = amid.getAssignedServices();
                    StringBuffer sb2 = new StringBuffer(ss);
                    if (debug.messageEnabled()) {
                        sb2.append(" has ").append(svcs.size()).
                            append(" assigned services:\n");
                        for (Iterator it3 = svcs.iterator(); it3.hasNext(); ) {
                            sb2.append("    ").append(it3.next()).append("\n");
                        }
                        debug.message(classMethod + sb2.toString());
                    }
                    // get auth modules
                    Set insts = getAuthModules(currentRealmAMIdName);
                    if (debug.messageEnabled()) {
                        StringBuilder sb3 = new StringBuilder(ss);
                        sb3.append(" has ").append(insts.size()).
                            append(" auth modules:\n");
                        for (Iterator it4 = insts.iterator(); it4.hasNext(); ){
                            AMAuthenticationInstance ai =
                                (AMAuthenticationInstance)it4.next();
                            sb3.append("    ").append(ai.getName()).
                                append("\n");
                        }
                        debug.message(classMethod + sb3.toString());
                    }

                    insts = getSupportedEntityTypes(currentRealmAMIdName);

                    if (debug.messageEnabled()) {
                        sb2 =
                            new StringBuffer("Supported Entity types for ");
                        sb2.append(currentAMIdName).append(":\n");
                        for (Iterator it4 = insts.iterator(); it4.hasNext(); ){
                            IdType type = (IdType)it4.next();
                            String stype = type.getName();
                            sb2.append("    ").append(stype);
                        }
                        debug.message(classMethod + sb2.toString());

                        debug.message(classMethod + currentAMIdName +
                            "'s subrealm #" + num++ + " is " + ss);
                    }
                    doSubRealms(amid.getRealm());
                }
            }
        } catch (IdRepoException ire) {
            debug.error(classMethod +
                "IdRepoException getting AMIdentityRepository" +
                " object for root realm: " + ire.getMessage());
        } catch (SSOException ssoe) {
            debug.error(classMethod +
                "SSOException getting subrealms for root realm: " +
                ssoe.getMessage());
        }
        return;
    }

    /*
     *  gather the auth modules for this (sub)"realm".  "realm" is
     *  "currentRealmAMIdName" from:
     *
     *    AMIdentityRepository idRepo =
     *      new AMIdentityRepository(ssoToken, realm);
     *    AMIdentity thisRealmAMId = idRepo.getRealmIdentity();
     *    String currentRealmAMIdName = thisRealmAMId.getRealm();
     */
    private Set getAuthModules(String realm) {
        String classMethod = "ConfigMonitoring.getAuthModules: ";
        Set insts = Collections.EMPTY_SET;
        try {
            AMAuthenticationManager mgr =
                new AMAuthenticationManager (ssoToken, realm);
            insts = mgr.getAuthenticationInstances();
        } catch (AMConfigurationException e) {
            debug.error(classMethod + "getting auth instances; " +
                e.getMessage());
        }
        return insts;
    }

    private Set getSupportedEntityTypes(String realm) {
        String classMethod = "ConfigMonitoring.getSupportedEntityTypes: ";
        Set supportedTypes = Collections.EMPTY_SET;
        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, realm);
            supportedTypes = repo.getSupportedIdTypes();
        } catch (IdRepoException e) {
            debug.error(classMethod +
                "idrepo exception getting supported entity types; " +
                e.getMessage());
        } catch (SSOException e) {
            debug.error(classMethod +
                "sso exception getting supported entity types; " +
                e.getMessage());
        }
        return supportedTypes;
    }

    private void getAgentTypes() {
        String classMethod = "ConfigMonitoring.getAgentTypes: ";
        /*
         *  agent types are:
         *    AgentConfiguration.AGENT_TYPE_J2EE = "J2EEAgent"
         *    AgentConfiguration.AGENT_TYPE_WEB = "WebAgent"
         *    AgentConfiguration.AGENT_TYPE_2_DOT_2_AGENT = "2.2_Agent"
         *    AgentConfiguration.AGENT_TYPE_AGENT_AUTHENTICATOR =
         *      "SharedAgent"
         *
         *  these are in the federation tree, so not accessible
         *    com.sun.identity.wss.provider.ProviderConfig.WSC = "WSCAgent"
         *    com.sun.identity.wss.provider.ProviderConfig.WSP = "WSPAgent"
         *    com.sun.identity.wss.provider.TrustAuthorityConfig.\
         *      DISCOVERY_TRUST_AUTHORITY = "DiscoveryAgent"
         */
        Set agents = null;
        try {
            agents = AgentConfiguration.getAgentTypes();
        } catch (SMSException sme) {
            debug.error(classMethod + "sms exception: " + sme.getMessage());
            return;
        } catch (SSOException sse) {
            debug.error(classMethod + "sso exception: " + sse.getMessage());
            return;
        }

        StringBuilder sb = new StringBuilder(classMethod);
        if (debug.messageEnabled()) {
            sb.append("number of AgentTypes = ").append(agents.size()).
                append(":\n");
        }
        
        for (Iterator it = agents.iterator(); it.hasNext(); ) {
            String s = (String)it.next();
            sb.append("  ").append(s).append("\n");
        }
        if (debug.messageEnabled()) {
            debug.error(sb.toString());
        }
    }

    private void getAgents (String realm) {
        String classMethod = "ConfigMonitoring.getAgents: ";

        StringBuffer sb = new StringBuffer(classMethod);
        /*
         *  given a realm, can search the AMIdentityRepository for
         *  IdType.AGENTONLY and IdType.AGENTGROUP.  or IdType.AGENT
         *  to get both.  this is similar
         *  to AgentsModelImpl.java:getAgentNames(...)
         */
        
        try {
            IdSearchControl isc = new IdSearchControl();
            isc.setMaxResults(0);
            isc.setTimeOut(3000); // should use set value, but for now...
            isc.setAllReturnAttributes(false);
            AMIdentityRepository airepo =
                new AMIdentityRepository(ssoToken, realm);
            IdSearchResults isr = airepo.searchIdentities(
                IdType.AGENT, "*", isc);
            Set results = isr.getSearchResults(); // set of AMIdentitys
            sb = new StringBuffer("Agents for realm ");
            sb.append(realm).append("; size = ").append(results.size()).
                append(":\n");
            for (Iterator it = results.iterator(); it.hasNext(); ) {
                AMIdentity aid = (AMIdentity)it.next();
                processAgentIdentity(aid, sb);
            }
            debug.error(classMethod + sb.toString());
        } catch (IdRepoException e) {
            debug.error(classMethod + "idrepo error getting agents: " +
                e.getMessage());
        } catch (SSOException e) {
            debug.error(classMethod + "sso error getting agents: " +
                e.getMessage());
        }
    }

    private void getAgentGroups (String realm) {
        String classMethod = "ConfigMonitoring.getAgentGroups: ";
        /*
         *  given a realm, search the AMIdentityRepository for
         *  IdType.AGENTGROUP.
         *  this is similar to AgentsModelImpl.java:getAgentGroupNames(...)
         */
        
        StringBuffer sb = new StringBuffer(classMethod);
        try {
            IdSearchControl isc = new IdSearchControl();
            isc.setMaxResults(0);
            isc.setTimeOut(3000); // should use set value, but for now...
            isc.setAllReturnAttributes(false);
            AMIdentityRepository airepo =
                new AMIdentityRepository(ssoToken, realm);
            IdSearchResults isr = airepo.searchIdentities(
                IdType.AGENTGROUP, "*", isc);
            Set results = isr.getSearchResults(); // set of AMIdentitys
            sb = new StringBuffer("AgentGroups for realm ");
            sb.append(realm).append("; size = ").append(results.size()).
                append(":\n");
            for (Iterator it = results.iterator(); it.hasNext(); ) {
                AMIdentity aid = (AMIdentity)it.next();
                processAgentIdentity(aid, sb);
            }
            debug.error(classMethod + sb.toString());
        } catch (IdRepoException e) {
            debug.error(classMethod + "idrepo error getting agents: " +
                e.getMessage());
        } catch (SSOException e) {
            debug.error(classMethod + "sso error getting agents: " +
                e.getMessage());
        }
    }

    private void getAgentsAndGroupsInfo (
        String realm,
        AMIdentityRepository airepo,
        AMIdentity amid)
    {
        String classMethod = "ConfigMonitoring.getAgentsAndGroupsInfo: ";

        StringBuffer sb = new StringBuffer(classMethod);
        /*
         *  given a realm, can search the AMIdentityRepository for
         *  IdType.AGENTONLY and IdType.AGENTGROUP.  or IdType.AGENT
         *  to get both.  this is similar
         *  to AgentsModelImpl.java:getAgentNames(...)
         */
        
        try {
            IdSearchControl isc = new IdSearchControl();
            isc.setMaxResults(0);
            isc.setTimeOut(3000); // should use set value, but for now...
            isc.setAllReturnAttributes(false);
            IdSearchResults isr = airepo.searchIdentities(
                IdType.AGENTONLY, "*", isc);
            Set results = isr.getSearchResults(); // set of AMIdentitys
            sb = new StringBuffer("\n  Agents for realm ");
            if (debug.messageEnabled()) {
                sb.append(realm).append("; quantity = ").
                    append(results.size()).append(":\n");
            }
            // results has all the agents (only, not agent groups)
            Map raMap = new HashMap();
            for (Iterator it = results.iterator(); it.hasNext(); ) {
                AMIdentity aid = (AMIdentity)it.next();
                Map m = processAgentIdentitySpecific(realm, aid, sb, true);
                raMap.put(aid.getName(), m);
            }
            if (!raMap.isEmpty()) {
                Agent.configAgentsOnly(realm, raMap);
            }

            /*
             *  now the agent groups
             */
            isc = new IdSearchControl();
            isc.setMaxResults(0);
            isc.setTimeOut(3000); // should use set value, but for now...
            isc.setAllReturnAttributes(false);
            isr = airepo.searchIdentities(IdType.AGENTGROUP, "*", isc);
            results = isr.getSearchResults(); // set of AMIdentitys
            if (debug.messageEnabled()) {
                sb.append("\n  Agent Groups for realm ");
                sb.append(realm).append("; quantity = ").
                    append(results.size()).append(":\n");
            }
            raMap = new HashMap();
            for (Iterator it = results.iterator(); it.hasNext(); ) {
                AMIdentity aid = (AMIdentity)it.next();
                Map m = processAgentIdentitySpecific(realm, aid, sb, false);
                raMap.put(aid.getName(), m);
            }
            if (!raMap.isEmpty()) {
                Agent.configAgentGroups(realm, raMap);
            }
            if (debug.messageEnabled()) {
                debug.message(classMethod + sb.toString());
            }
        } catch (IdRepoException e) {
            debug.error(classMethod + "idrepo error getting agents: " +
                e.getMessage());
        } catch (SSOException e) {
            debug.error(classMethod + "sso error getting agents: " +
                e.getMessage());
        }
    }

    private void processAgentIdentity(AMIdentity aid, StringBuffer sb) {
        String classMethod = "ConfigMonitoring.processAgentIdentity: ";

        /*
         *  aid.getName() => name of the agent/agentgroup
         *  aid.getType().toString() => "agent"/"agentgroup"
         *  aid.getAttributes([CLIConstants.ATTR_NAME_AGENT_TYPE]) =>
         *   for "agent":
         *    "SharedAgent"
         *    "2.2_Agent"
         *    "WSCAgent"
         *    "SharedAgent"
         *    "STSAgent"
         *    "DiscoveryAgent"
         *    "J2EEAgent"
         *   for "agentgroup":
         *    "STSAgent"
         *    "WSPAgent"
         *    "WSCAgent"
         *    "WebAgent"
         *    "J2EEAgent"
         *    "DiscoveryAgent"
         *  aid.getMemberships(IdType.AGENTGROUP) for agents
         *    gives the agent group name(s) it's a member of
         *  aid.getAttribute(com.sun.identity.agents.config.login.url) =>
         *    agent's server URL (plus trailing "/UI/Login")
         *  aid.getAttribute(com.sun.identity.agents.config.agenturi.prefix) =>
         *    agent's URL (plus trailing "/amagent")
         */

        sb.append("  name = ").append(aid.getName()).
          append(", type = ").append(aid.getType().toString());
        Map attrv = null;
        Set attrs = null;
        try {
            attrv = aid.getAttributes();
            attrs = (Set)attrv.get(CLIConstants.ATTR_NAME_AGENT_TYPE);
        } catch (IdRepoException e) {
            debug.error(classMethod + "idrepo error getting attrs");
            return;
        } catch (SSOException e) {
            debug.error(classMethod + "sso error getting attrs");
            return;
        }
        String atype = "UNKNOWN";
        if ((attrs != null) && !attrs.isEmpty()) {
            atype = (String)attrs.iterator().next();
        }
        sb.append(", agent type = ").append(atype).
            append(",\n         memberships =");
        Set mships = null;
        try {
            mships = aid.getMemberships(IdType.AGENTGROUP);
        } catch (IdRepoException e) {
            debug.error(classMethod + "idrepo error getting agentgroups");
            return;
        } catch (SSOException e) {
            debug.error(classMethod + "sso error getting agentgroups");
            return;
        }
        if ((mships != null) && !mships.isEmpty()) {
            for (Iterator i2 = mships.iterator(); i2.hasNext(); ) {
                AMIdentity amd = (AMIdentity)i2.next();
                sb.append(" ").append(amd.getName());
            }
        } else {
            sb.append(" NONE");
        }
        sb.append("\n");
        try {
            Map amap = aid.getAttributes();
            sb.append("         Attributes:\n");
            if (amap.size() < 1) {
                sb.append("          NONE\n");
            } else {
                for (Iterator it = amap.keySet().iterator(); it.hasNext(); ) {
                    String attrName = (String)it.next();
                    sb.append("           ").append(attrName).append(" = ");
                    Set attrv2 = (Set)amap.get(attrName);
                    if (attrv2.isEmpty()) {
                        sb.append("NONE");
                    } else {
                        Iterator iu = attrv2.iterator();
                        String attrval = (String)iu.next();
                        sb.append(attrval);
                        if (attrv2.size() > 1) // there are more?
                        while (iu.hasNext()) {
                            attrval = (String)iu.next();
                            sb.append(", ").append(attrval);
                        }
                    }
                        sb.append("\n");
                }
            }

            Set iattrs =
                AgentConfiguration.getInheritedAttributeNames(aid);
            Iterator it3 = iattrs.iterator();
            sb.append("         Inherited Attribute names:\n");
            if (iattrs.size() < 1) {
                sb.append("          NONE");
            } else {
                while (it3.hasNext()) {
                    String attrName = (String)it3.next();
                    sb.append("           ").append(attrName).append("\n");
                }
            }
            sb.append("\n");
        } catch (IdRepoException ex) {
            debug.error(classMethod + "idrepo error getting attrs");
        } catch (SSOException ex) {
            debug.error(classMethod + "sso error getting attrs");
        } catch (SMSException ex) {
            debug.error(classMethod + "sms error getting attrs");
        }
    }

    private Map processAgentIdentitySpecific(
        String realm,
        AMIdentity aid,
        StringBuffer sb,
        boolean isAgentOnly)
    {
        String classMethod = "ConfigMonitoring.processAgentIdentitySpecific: ";

        /*
         *  aid.getName() => name of the agent/agentgroup
         *  aid.getType().toString() => "agent"/"agentgroup"
         *  aid.getAttributes([CLIConstants.ATTR_NAME_AGENT_TYPE]) =>
         *   for "agent":
         *    "SharedAgent" -> skip these (Agent Authenticator)
         *    "2.2_Agent" -> only have name
         *    "WSCAgent" -> get wspendpoint and wspproxyendpoint
         *    "STSAgent" -> get stsendpoint
         *    "DiscoveryAgent" -> get discoveryendpoint, authnserviceendpoint
         *    "J2EEAgent" get com.sun.identity.client.notification.url (minus
         *                 "/notification) and
         *                com.sun.identity.agents.config.login.url (already)
         *   for "agentgroup":
         *    "STSAgent" -> get stsendpoint
         *    "WSPAgent" -> get wspendpoint and wspproxyendpoint (can be opt)
         *    "WSCAgent" -> get wspendpoint and wspproxyendpoint (can be opt)
         *    "WebAgent" -> get com.sun.identity.agents.config.login.url (ok)
         *    "J2EEAgent" -> get com.sun.identity.agents.config.login.url (ok)
         *                   and com.sun.identity.client.notification.url
         *                     minus the "/notification"
         *    "DiscoveryAgent" -> get discoveryendpoint and authnserviceendpoint
         *
         *  aid.getMemberships(IdType.AGENTGROUP) for agents
         *    gives the agent group name(s) it's a member of
         *  aid.getAttribute(com.sun.identity.agents.config.login.url) =>
         *    agent's server URL (plus trailing "/UI/Login")
         *  aid.getAttribute(com.sun.identity.agents.config.agenturi.prefix) =>
         *    agent's URL (plus trailing "/amagent")
         */

        /*
         *  these are same for both agents and agent groups
         *  agents can be in only one agent group
         */
        
        String agentOrGroup = aid.getType().toString();
        sb.append("  name = ").append(aid.getName());
        sb.append(", type = ").append(agentOrGroup).append(", ");
        Map attrv = null;
        Set attrs = null;
        Set attrsToGet = new HashSet();

        /*
         *  have to get the agenttype before knowing which
         *  attributes we really want, so try to get them all
         */
        attrsToGet.add(CLIConstants.ATTR_NAME_AGENT_TYPE);
        attrsToGet.add("com.sun.identity.agents.config.agenturi.prefix");
        attrsToGet.add("com.sun.identity.agents.config.login.url");
        attrsToGet.add("wspendpoint");
        attrsToGet.add("wspproxyendpoint");
        attrsToGet.add("stsendpoint");
        attrsToGet.add("discoveryendpoint");
        attrsToGet.add("authnserviceendpoint");
        attrsToGet.add("com.sun.identity.client.notification.url");

        try {
            attrv = aid.getAttributes(attrsToGet);
        } catch (IdRepoException e) {
            debug.error(classMethod + "idrepo error getting attrs");
            return null;
        } catch (SSOException e) {
            debug.error(classMethod + "sso error getting attrs");
            return null;
        }

        /*
         *  depending on if agent or agent group, and what type
         *  see if the corresponding attribute(s) have values.
         *  the attribute's value comes as a Set... should only
         *  have/need one value
         */
        String atype =
            getValFromSet (attrv, CLIConstants.ATTR_NAME_AGENT_TYPE);
        attrsToGet = new HashSet();

        /*
         *  now to get just the ones we want for the given agent/agent group
         */
        if (atype.equalsIgnoreCase("2.2_Agent")) { // agentonly
            // only agent's name
        } else if (atype.equalsIgnoreCase("WSCAgent")) { // both
            // wspendpoint and wspproxyendpoint for both
            attrsToGet.add("wspendpoint");
            attrsToGet.add("wspproxyendpoint");
        } else if (atype.equalsIgnoreCase("STSAgent")) { // both
            // stsendpoint for both
            attrsToGet.add("stsendpoint");
        } else if (atype.equalsIgnoreCase("DiscoveryAgent")) { // both
            // discoveryendpoint and authnserviceendpoint for both
            attrsToGet.add("discoveryendpoint");
            attrsToGet.add("authnserviceendpoint");
        } else if (atype.equalsIgnoreCase("J2EEAgent")) {  // both
            /*
             * com.sun.identity.client.notification.url (minus "/notification")
             *  and com.sun.identity.agents.config.login.url for both
             */
            attrsToGet.add("com.sun.identity.agents.config.login.url");
            attrsToGet.add("com.sun.identity.client.notification.url");
        } else if (atype.equalsIgnoreCase("WSPAgent")) {  // both
            // wspendpoint and wspproxyendpoint (both)
            attrsToGet.add("wspendpoint");
            attrsToGet.add("wspproxyendpoint");
        } else if (atype.equalsIgnoreCase("WebAgent")) {  // both
            attrsToGet.add("com.sun.identity.agents.config.agenturi.prefix");
            attrsToGet.add("com.sun.identity.agents.config.login.url");
        } // don't process "SharedAgent" or "NONE" type

        Map attrMap = new HashMap(); // attributes for this agent
        Map agtMap = new HashMap(); // realm -> agents/attrMap
        attrMap.put(CLIConstants.ATTR_NAME_AGENT_TYPE, atype);

        sb.append("agent type = ").append(atype).append("\n");
        if (attrsToGet.size() > 0) {
            sb.append("    RETRIEVED Attributes/values:\n");
            Iterator ii = attrsToGet.iterator();
            while (ii.hasNext()) {
                String key = (String)ii.next();
                String val = getValFromSet (attrv, key);
                if (key.equalsIgnoreCase(
                    "com.sun.identity.client.notification.url"))
                {
                    int ind = val.lastIndexOf("/notification");
                    if (ind > -1) {
                        val = val.substring(0, ind);
                    }
                }
                sb.append("      attr = ").append(key).append("\n      ").
                    append("value = ").
                    append(val).append("\n");
                attrMap.put(key, val);
            }
        } else {
            sb.append("    No attributes to display\n");
        }

        if (isAgentOnly) {
            sb.append("    MEMBERSHIP(s) =");
            Set mships = null;
            try {
                mships = aid.getMemberships(IdType.AGENTGROUP);
            } catch (IdRepoException e) {
                debug.error(classMethod + "idrepo error getting agentgroups");
                sb.append("ERROR");
            } catch (SSOException e) {
                debug.error(classMethod + "sso error getting agentgroups");
                sb.append("ERROR");
            }
            if ((mships != null) && !mships.isEmpty()) {
                for (Iterator i2 = mships.iterator(); i2.hasNext(); ) {
                    AMIdentity amd = (AMIdentity)i2.next();
                    String grp = amd.getName();
                    sb.append(" ").append(grp);
                    attrMap.put("groupmembership", grp);
                }
            } else {
                sb.append(" NONE");
            }
        }
        sb.append("\n");
        return attrMap;
    }

    private String getValFromSet (Map values, String valAttr) {
        Set set = (Set)values.get(valAttr);
        if (set == null) {
            if (debug.warningEnabled()) {
                debug.warning("ConfigMonitoring.getValFromSet: " +
                    "Null return for attribute " + valAttr);
            }
            return "NONE";
        }
        if (set.size() > 0) {
            return ((String)set.iterator().next());
        } else {
            return "NONE";
        }
    }

    private int getMonServiceAttrs() {
        String classMethod = "ConfigMonitoring.getMonServiceAttrs: ";
        try {
            ServiceSchemaManager schemaManager =
                new ServiceSchemaManager(
                    "iPlanetAMMonitoringService", ssoToken);
            ServiceSchema smsMonSchema = schemaManager.getGlobalSchema();
            Map monAttrs = smsMonSchema.getAttributeDefaults();
            boolean monEna =
                Boolean.valueOf(CollectionHelper.getMapAttr(monAttrs,
                    "iplanet-am-monitoring-enabled")).booleanValue();
            if (!monEna) {
                if (debug.warningEnabled()) {
                    debug.warning(classMethod + "monitoring is disabled");
                }
                return -1;
            }
            boolean httpEna =
                Boolean.valueOf(CollectionHelper.getMapAttr(monAttrs,
                    "iplanet-am-monitoring-http-enabled")).booleanValue();
            int httpPort = 
                Integer.valueOf(CollectionHelper.getMapAttr(monAttrs,
                    "iplanet-am-monitoring-http-port"));
            String authFilePath = CollectionHelper.getMapAttr(monAttrs,
                "iplanet-am-monitoring-authfile-path");
            int rmiPort = 
                Integer.valueOf(CollectionHelper.getMapAttr(monAttrs,
                    "iplanet-am-monitoring-rmi-port"));
            boolean rmiEna =
                Boolean.valueOf(CollectionHelper.getMapAttr(monAttrs,
                    "iplanet-am-monitoring-rmi-enabled")).booleanValue();
            int snmpPort = 
                Integer.valueOf(CollectionHelper.getMapAttr(monAttrs,
                    "iplanet-am-monitoring-snmp-port"));
            boolean snmpEna =
                Boolean.valueOf(CollectionHelper.getMapAttr(monAttrs,
                    "iplanet-am-monitoring-snmp-enabled")).booleanValue();
            
            if (debug.messageEnabled()) {
                debug.message(classMethod + "\n" +
                    "     monitoring enabled = " + monEna + "\n" +
                    "     monitoring auth filepath = " + authFilePath + "\n" +
                    "     httpPort = " + httpPort + "\n" +
                    "     httpPort enabled = " + httpEna + "\n" +
                    "     rmiPort = " + rmiPort + "\n" +
                    "     rmiPort enabled = " + rmiEna + "\n" +
                    "     snmpPort = " + snmpPort + "\n" +
                    "     snmpPort enabled = " + snmpEna + "\n"
                    );
            }

            SSOServerMonConfig sMonInfo =
                new SSOServerMonConfig.SSOServerMonInfoBuilder(monEna).
                    htmlPort(httpPort).
                    htmlAuthFile(authFilePath).
                    snmpPort(snmpPort).
                    rmiPort(rmiPort).
                    monHtmlEnabled(httpEna).
                    monRmiEnabled(rmiEna).
                    monSnmpEnabled(snmpEna).build();

            int i = Agent.startAgent(sMonInfo);
            if (i != 0) {
                if (debug.warningEnabled()) {
                    debug.warning(classMethod +
                        "Monitoring Agent not started (" + i + ")");
                }
                return (i);
            }
        } catch (Exception ex) {
            debug.error(classMethod +
                "error reading Monitoring attributes: ", ex);
            return (Agent.MON_READATTRS_PROBLEM);
        }
        return 0;
    }
}
