/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LDAPv3Repo.java,v 1.74 2010/01/20 01:08:36 goodearth Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS 
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.idm.plugins.ldapv3;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.StringTokenizer;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPCache;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPControl;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPModificationSet;
import com.sun.identity.shared.ldap.LDAPObjectClassSchema;
import com.sun.identity.shared.ldap.LDAPRebind;
import com.sun.identity.shared.ldap.LDAPRebindAuth;
import com.sun.identity.shared.ldap.LDAPReferralException;
import com.sun.identity.shared.ldap.LDAPSchema;
import com.sun.identity.shared.ldap.LDAPSearchConstraints;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPUrl;
import com.sun.identity.shared.ldap.LDAPv2;
import com.sun.identity.shared.ldap.LDAPv3;
import com.sun.identity.shared.ldap.controls.LDAPPasswordExpiringControl;
import com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl;
import com.sun.identity.shared.ldap.factory.JSSESocketFactory;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.LDAPAddRequest;
import com.sun.identity.shared.ldap.LDAPDeleteRequest;
import com.sun.identity.shared.ldap.LDAPRequestParser;
import com.sun.identity.shared.ldap.LDAPModifyRequest;
import com.sun.identity.shared.ldap.LDAPSearchRequest;

import com.iplanet.am.sdk.AMCommonUtils;
import com.iplanet.am.sdk.AMHashMap;
//import com.iplanet.am.util.AMURLEncDec;
import com.sun.identity.shared.encode.URLEncDec;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoFatalException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.idm.common.IdRepoUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.shared.ldap.util.LDAPUtilException;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SchemaType;

public class LDAPv3Repo extends IdRepo {

    private static final String AM_AUTH = "amAuth";

    private Map supportedOps = new HashMap();

    // config is part of IdRepo.java superclass and
    // set in superclass's initialize method.
    // private Map configMap = new AMHashMap();
    private Map myConfigMap = null;

    private Map myServiceMap = null;

    private boolean hasShutdown = false;
    
    private IdRepoListener myListener = null;

    private static SOAPClient mySoapClient = new SOAPClient("dummy");

    private List<String> ldapServers = new ArrayList();

    private String ldapServerName = null;

    private String ldapHost = null;

    private int ldapPort = 389;

    private String firstHostAndPort = "";

    // password control  states
    private final static int NO_PASSWORD_CONTROLS = 0;

    private final static int PASSWORD_EXPIRED = -1;

    private final static int activeMask = 0xfffffffD;

    private final static int disableMask = 0x2;

    private String orgDN = "";

    private static Debug debug;

    private LDAPConnectionPool connPool;

    private boolean sslMode = false;

    private String ldapConnError = "91";

    private int connNumRetry = 3;

    private int connRetryInterval = 1000;

    private int timeLimit = 5000;

    private int defaultMaxResults = 100;

    private int roleSearchScope = LDAPv2.SCOPE_SUB;

    private int searchScope = LDAPv2.SCOPE_SUB;

    private String userSearchFilter = null;

    private String groupSearchFilter = null;

    private String roleSearchFilter = 
        "(&(objectclass=ldapsubentry)(objectclass=nsmanagedroledefinition))";

    private String filterroleSearchFilter =
        "(&(objectclass=ldapsubentry)(objectclass=nsfilteredroledefinition))";

    private String agentSearchFilter = null;

    private String userAuthNamingAttr = null;

    private String userSearchNamingAttr = null;

    private String agentSearchNamingAttr = null;

    private String groupSearchNamingAttr = null;

    private String roleSearchNamingAttr = null;

    private String filterroleSearchNamingAttr = null;

    private String peopleCtnrNamingAttr = null;

    private String agentCtnrNamingAttr = null;

    private String groupCtnrNamingAttr = null;

    private String peopleCtnrValue = null;

    private String agentCtnrValue = null;

    private String groupCtnrValue = null;

    private String nsRoleAttr = null;

    private String nsRoleDNAttr = null;

    private String nsRoleFilterAttr = null;

    private String memberOfAttr = null;

    private String uniqueMemberAttr = null;

    private String memberURLAttr = null;

    private String defaultGrpMem = null;

    private String isActiveAttrName = null;

    private boolean alwaysActive = false;

    private String inetUserActive = null;

    private String inetUserInactive = null;

    private Set filterroleObjClassSet = null;

    private Set roleObjClassSet = null;

    private Set groupObjClassSet = null;

    private Set userObjClassSet = null;

    private Set agentObjClassSet = null;

    private Map createUserAttrMap = null;

    private CaseInsensitiveHashSet userAtttributesAllowed = null;

    private CaseInsensitiveHashSet groupAtttributesAllowed = null;

    private CaseInsensitiveHashSet agentAtttributesAllowed = null;

    private CaseInsensitiveHashSet roleAtttributesAllowed = null;

    private CaseInsensitiveHashSet filteredroleAtttributesAllowed = null;

    private Set userSpecifiedOpsSet = null;

    private CaseInsensitiveHashSet authenticatableSet = null;

    private boolean cacheEnabled = false;

    private long cacheTTL = 600; // in seconds

    private long cacheSize = 10240; // in bytes

    private LDAPCache ldapCache = null;

    private final int MIN_CONNECTION_POOL_SIZE = 1;

    private final int MAX_CONNECTION_POOL_SIZE = 10;

    private final int PS_OP = LDAPPersistSearchControl.ADD 
                | LDAPPersistSearchControl.MODIFY
                | LDAPPersistSearchControl.DELETE
                | LDAPPersistSearchControl.MODDN;

    private ShutdownListener shutdownListener = null;
    private LDAPRebind reBind = null;

    // access to the _eventsMgr and _eventsMgr needs to be sync.
    protected static Hashtable _eventsMgr = new Hashtable();

    protected static Hashtable _numRequest = new Hashtable();

    protected static Map listOfPS = 
        Collections.synchronizedMap(new HashMap());

    private boolean hasListener = false;

    private String dsType = "";

    static final String LDAP_OBJECT_CLASS = "objectclass";

    static final String LDAP_SCOPE_BASE = "SCOPE_BASE";

    static final String LDAP_SCOPE_ONE = "SCOPE_ONE";

    static final String LDAP_SCOPE_SUB = "SCOPE_SUB";

    private static final String LDAPv3Config_LDAPV3GENERIC =
        "sun-idrepo-ldapv3-ldapv3Generic";

    private static final String LDAPv3Config_LDAPV3AMDS =
        "sun-idrepo-ldapv3-ldapv3AMDS";

    private static final String LDAPv3Config_LDAPV3AD =
        "sun-idrepo-ldapv3-ldapv3AD";

    private static final String LDAPv3Config_LDAPV3ADAM =
        "sun-idrepo-ldapv3-ldapv3ADAM";

    private static final String LDAPv3Config_LDAPV3OpenDS =
        "sun-idrepo-ldapv3-ldapv3OpenDS";

    private static final String LDAPv3Config_LDAPV3Tivoli =
        "sun-idrepo-ldapv3-ldapv3Tivoli";

    private static final String LDAPv3Config_LDAP_SERVER = 
        "sun-idrepo-ldapv3-config-ldap-server";

    private static final String LDAPv3Config_AUTHID =
        "sun-idrepo-ldapv3-config-authid";

    private static final String LDAPv3Config_AUTHPW = 
        "sun-idrepo-ldapv3-config-authpw";

    private static final String LDAPv3Config_LDAP_SSL_ENABLED = 
        "sun-idrepo-ldapv3-config-ssl-enabled";

    private static final String LDAPv3Config_LDAP_CONNECTION_POOL_MIN_SIZE = 
        "sun-idrepo-ldapv3-config-connection_pool_min_size";

    private static final String LDAPv3Config_LDAP_CONNECTION_POOL_MAX_SIZE = 
        "sun-idrepo-ldapv3-config-connection_pool_max_size";

    private static final String LDAPv3Config_ORGANIZATION_NAME = 
        "sun-idrepo-ldapv3-config-organization_name";

    private static final String LDAPv3Config_LDAP_GROUP_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-groups-search-filter";

    private static final String LDAPv3Config_LDAP_USERS_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-users-search-filter";

    private static final String LDAPv3Config_LDAP_ROLES_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-roles-search-filter";

    private static final String LDAPv3Config_LDAP_FILTERROLES_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-filterroles-search-filter";

    private static final String LDAPv3Config_LDAP_AGENT_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-agent-search-filter";

    private static final String LDAPv3Config_LDAP_ROLES_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-roles-search-attribute";

    private static final String LDAPv3Config_LDAP_FILTERROLES_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-filterroles-search-attribute";

    private static final String LDAPv3Config_LDAP_GROUPS_SEARCH_ATTRIBUTE = 
        "sun-idrepo-ldapv3-config-groups-search-attribute";

    private static final String LDAPv3Config_LDAP_USERS_SEARCH_ATTRIBUTE = 
        "sun-idrepo-ldapv3-config-users-search-attribute";

    private static final String LDAPv3Config_LDAP_USERS_NAMING_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-auth-naming-attr";

    private static final String LDAPv3Config_LDAP_AGENT_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-agent-search-attribute";

    private static final String LDAPv3Config_LDAP_ROLES_SEARCH_SCOPE = 
        "sun-idrepo-ldapv3-config-role-search-scope";

    private static final String LDAPv3Config_LDAP_SEARCH_SCOPE =
        "sun-idrepo-ldapv3-config-search-scope";

    private static final String LDAPv3Config_LDAP_GROUP_CONTAINER_NAME = 
        "sun-idrepo-ldapv3-config-group-container-name";

    private static final String LDAPv3Config_LDAP_AGENT_CONTAINER_NAME = 
        "sun-idrepo-ldapv3-config-agent-container-name";

    private static final String LDAPv3Config_LDAP_PEOPLE_CONTAINER_NAME = 
        "sun-idrepo-ldapv3-config-people-container-name";

    private static final String LDAPv3Config_LDAP_GROUP_CONTAINER_VALUE =
        "sun-idrepo-ldapv3-config-group-container-value";

    private static final String LDAPv3Config_LDAP_PEOPLE_CONTAINER_VALUE = 
        "sun-idrepo-ldapv3-config-people-container-value";

    private static final String LDAPv3Config_LDAP_AGENT_CONTAINER_VALUE =
        "sun-idrepo-ldapv3-config-agent-container-value";

    private static final String LDAPv3Config_LDAP_TIME_LIMIT = 
        "sun-idrepo-ldapv3-config-time-limit";

    private static final String LDAPv3Config_LDAP_MAX_RESULT = 
        "sun-idrepo-ldapv3-config-max-result";

    private static final String LDAPv3Config_REFERRALS = 
        "sun-idrepo-ldapv3-config-referrals";

    private static final String LDAPv3Config_ROLE_OBJECT_CLASS = 
        "sun-idrepo-ldapv3-config-role-objectclass";

    private static final String LDAPv3Config_FILTERROLE_OBJECT_CLASS =
        "sun-idrepo-ldapv3-config-filterrole-objectclass";

    private static final String LDAPv3Config_GROUP_OBJECT_CLASS = 
        "sun-idrepo-ldapv3-config-group-objectclass";

    private static final String LDAPv3Config_USER_OBJECT_CLASS = 
        "sun-idrepo-ldapv3-config-user-objectclass";

    private static final String LDAPv3Config_AGENT_OBJECT_CLASS = 
        "sun-idrepo-ldapv3-config-agent-objectclass";

    private static final String LDAPv3Config_GROUP_ATTR =
        "sun-idrepo-ldapv3-config-group-attributes";

    private static final String LDAPv3Config_USER_ATTR = 
        "sun-idrepo-ldapv3-config-user-attributes";

    private static final String LDAPv3Config_AGENT_ATTR = 
        "sun-idrepo-ldapv3-config-agent-attributes";

    private static final String LDAPv3Config_ROLE_ATTR =
        "sun-idrepo-ldapv3-config-role-attributes";

    private static final String LDAPv3Config_FILTERROLE_ATTR =
        "sun-idrepo-ldapv3-config-filterrole-attributes";

    private static final String LDAPv3Config_NSROLE = 
        "sun-idrepo-ldapv3-config-nsrole";

    private static final String LDAPv3Config_NSROLEDN = 
        "sun-idrepo-ldapv3-config-nsroledn";

    private static final String LDAPv3Config_NSROLEFILTER = 
        "sun-idrepo-ldapv3-config-nsrolefilter";

    private static final String LDAPv3Config_MEMBEROF =
        "sun-idrepo-ldapv3-config-memberof";

    private static final String LDAPv3Config_UNIQUEMEMBER = 
        "sun-idrepo-ldapv3-config-uniquemember";

    private static final String LDAPv3Config_DEFAULTGROUPMEMBER = 
        "sun-idrepo-ldapv3-config-dftgroupmember";

    private static final String LDAPv3Config_MEMBERURL =
        "sun-idrepo-ldapv3-config-memberurl";

    private static final String LDAPv3Config_LDAP_IDLETIMEOUT = 
        "sun-idrepo-ldapv3-config-idletimeout";

    private static final String LDAPv3Config_LDAP_PSEARCHBASE =
        "sun-idrepo-ldapv3-config-psearchbase";

    private static final String LDAPv3Config_LDAP_PSEARCHFILTER =
        "sun-idrepo-ldapv3-config-psearch-filter";
    
    private static final String LDAPv3Config_LDAP_PSEARCHSCOPE =
        "sun-idrepo-ldapv3-config-psearch-scope";

    private static final String LDAPv3Config_LDAP_ISACTIVEATTRNAME =
        "sun-idrepo-ldapv3-config-isactive";

    private static final String LDAPv3Config_LDAP_INETUSERACTIVE =
        "sun-idrepo-ldapv3-config-active";

    private static final String LDAPv3Config_LDAP_INETUSERINACTIVE =
        "sun-idrepo-ldapv3-config-inactive";

    private static final String LDAPv3Config_LDAP_CREATEUSERMAPPING =
        "sun-idrepo-ldapv3-config-createuser-attr-mapping";

    private static final String LDAPv3Config_LDAP_AUTHENTICATABLE =
        "sun-idrepo-ldapv3-config-authenticatable-type";

    private static final String LDAPv3Config_LDAP_CACHEENABLED =
        "sun-idrepo-ldapv3-config-cache-enabled";

    private static final String LDAPv3Config_LDAP_CACHETTL =
        "sun-idrepo-ldapv3-config-cache-ttl";

    private static final String LDAPv3Config_LDAP_CACHESIZE =
        "sun-idrepo-ldapv3-config-cache-size";

    private static final String LDAPv3Config_LDAP_NUM_RETRIES =
        "sun-idrepo-ldapv3-config-numretires";

    private static final String LDAPv3Config_LDAP_RETRY_INTERVAL =
        "com.iplanet.am.ldap.connection.delay.between.retries";

    private static final String LDAPv3Config_LDAP_ERROR_CODES =
        "sun-idrepo-ldapv3-config-errorcodes";

    private static final String AUTH_USER = IdType.USER.getName();

    private static final String AUTH_AGENT = IdType.AGENT.getName();

    private static final String AUTH_GROUP = IdType.GROUP.getName();

    private static final String defaultStatusAttribute =
        "inetUserStatus";

    private static final String statusActive = "Active";

    private static final String statusInactive = "Inactive";

    private static final String unicodePwd = "unicodePwd";

    private static final String userPassword = "userPassword";

    private static final String sunIdentityServerDeviceStatus =
        "sunIdentityServerDeviceStatus";

    private static SSOToken internalToken = null;

    private static final String SCHEMA_BUG_PROPERTY = 
        "com.sun.identity.shared.ldap.schema.quoting";

    private static final String VAL_STANDARD = "standard";

    private static final String CLASS_NAME = 
        "com.sun.identity.idm.plugins.ldapv3.LDAPv3Repo";

    protected String LDAPv3Repo = "LDAPv3Repo";

    protected String amAuthLDAP = "amAuthLDAP";

    public LDAPv3Repo() {
        if (debug == null) {
            debug = Debug.getInstance(LDAPv3Repo);
        }
        loadSupportedOps();
    }

    private void enableCache(LDAPConnection ld) {
        if ((cacheEnabled) && (ld.getCache() != ldapCache)) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists. ldapcache is null.");
                }
                ld.setCache(ldapCache);
        }
    }

    private void getLDAPServerName(Map configParams) {
        String siteID = "";
        String serverID = "";
        try {
            serverID = WebtopNaming.getAMServerID();
            siteID = WebtopNaming.getSiteID(serverID);
        } catch (ServerEntryNotFoundException senf) {
            if (debug.messageEnabled()) {
                debug.warning("ServerEntryNotFoundException error: siteID="
                        + siteID + "; serverID=" + serverID);
            }
        }

        Set ldapServerSet = new HashSet((Set) configParams
                .get(LDAPv3Config_LDAP_SERVER));
        String ldapServer = "";
        String endOfList = "";
        List firstChoiceList = new ArrayList();
        List secondChoiceList = new ArrayList();
        // put ldapServer from list into a string seperated by space for
        // failover purposes. LDAPConnection will automatcially handle failover.
        // hostname:portnumber | severID | siteID
        // serverID is optional. if omitted, it means any(don't care).
        // siteID is optional. if omitted, it means any(don't care).
        // host whose siteID and serverID matches webtop naming's serverid and
        // siteid are put in the front of the list as well as those host which
        // did not specify a siteid/serverid because of backward compatibliity.
        // otherwise it will go to the end of the list.
        Iterator it = ldapServerSet.iterator();
        while (it.hasNext()) {
            String curr = (String) it.next();
            StringTokenizer tk = new StringTokenizer(curr, "|");
            String hostAndPort = tk.nextToken().trim();
            String hostServerID = "";
            if (tk.hasMoreTokens()) {
                hostServerID = tk.nextToken();
                hostServerID = hostServerID.trim();
            }
            String hostSiteID = "";
            if (tk.hasMoreTokens()) {
                hostSiteID = tk.nextToken();
                hostSiteID = hostSiteID.trim();
            }

            if (hostSiteID.length() == 0) {
                hostSiteID = siteID;
            }
            if (hostServerID.length() == 0) {
                hostServerID = serverID;
            }
            if (siteID.equals(hostSiteID) && serverID.equals(hostServerID)) {
                firstChoiceList.add(hostAndPort);
                if (ldapServer.length() == 0) {
                    ldapServer = hostAndPort;
                } else {
                    ldapServer = ldapServer + " " + hostAndPort;
                }
            } else {
                secondChoiceList.add(hostAndPort);
                if (endOfList.length() == 0) {
                    endOfList = hostAndPort;
                } else {
                    endOfList = endOfList + " " + hostAndPort;
                }
            }
        } // end of while
        if (ldapServer.length() == 0) {
            ldapServer = endOfList;
        } else {
            if (endOfList.length() != 0) {
                ldapServer = ldapServer + " " + endOfList;
            }
        }

        if (firstChoiceList.isEmpty()) {
            firstChoiceList = secondChoiceList;
        } else {
            if (!secondChoiceList.isEmpty()) {
                firstChoiceList.addAll(secondChoiceList);
            }
        }

        ldapServerName = ldapServer;
        ldapServers = firstChoiceList;

        if (debug.messageEnabled()) {
            debug.message("getLDAPServerName:LDAPv3Config_LDAP_SERVER"
                    + "; ldapServer:" + ldapServer + "; endOfList:" + endOfList
                    + "; siteID:" + siteID + "; serverID:" + serverID
                    + "; ldapServers:" + ldapServers);
        }

        ldapHost = ldapServer;
        int index = ldapServer.indexOf(':');

        if (index > -1) {
            ldapHost = ldapServer.substring(0, index);
            try {
                String portSub = ldapServer.substring(index+1);
                int portindex = portSub.indexOf(' ');
                if (portindex > -1) {
                    String portStr = portSub.substring(0, portindex); 
                    ldapPort = Integer.parseInt(portStr);
                } else {
                    ldapPort = Integer.parseInt(portSub);
                }
            } catch(NumberFormatException e) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo:authenticate use default 389");
                }
            }
        }
    }
    
    private String getLDAPServerList() {
    	StringBuilder sb = new StringBuilder();
    	for (String ldapServerHost : ldapServers) {
    		sb.append(ldapServerHost);
    		sb.append(" ");
    	}
    	return sb.toString();
    }

    
    private void initConnectionPool(Map configParams) {

        // connOptions has the default options set for failover and 
        // fallback features.
        HashMap connOptions = new HashMap();
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: initConnectionPool ");
        }

        if (ldapServerName == null) {
            getLDAPServerName(configParams);
        }
        // ldapServerName is list of server names separated by space for
        // failover purposes. LDAPConnection will automatically handle failover.

        // port will not be used since ldapserver is in the following format:
        // nameOfLDAPhost:portNumber.
        String authid = getPropertyStringValue(configParams,
                LDAPv3Config_AUTHID);
        String authpw = getPropertyStringValue(configParams,
                LDAPv3Config_AUTHPW);
        String referrals = getPropertyStringValue(configParams,
                LDAPv3Config_REFERRALS);
        String ssl = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_SSL_ENABLED);

        int minPoolSize = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_CONNECTION_POOL_MIN_SIZE,
                MIN_CONNECTION_POOL_SIZE);
        int maxPoolSize = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_CONNECTION_POOL_MAX_SIZE,
                MAX_CONNECTION_POOL_SIZE);
        if (minPoolSize < 1) {
            minPoolSize = MIN_CONNECTION_POOL_SIZE;
        }
        if (maxPoolSize < 1) {
            maxPoolSize = MAX_CONNECTION_POOL_SIZE;
        }

        LDAPConnection ldc = null;

            try {
                if (ssl != null && ssl.equalsIgnoreCase("true")) {
                    ldc = new LDAPConnection(new JSSESocketFactory(null));
                    sslMode = true;
                } else {
                    ldc = new LDAPConnection();
                    sslMode = false;
                }
            } catch (Exception ex) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: initConnectionPool "
                            + "LDAPConnection failed", ex);
                }

                connPool = null;
            }

            try {
                ldc.setOption(LDAPv3.PROTOCOL_VERSION, new Integer(3));
                ldc.setOption(LDAPv2.REFERRALS, Boolean.valueOf(referrals));
                ldc.setOption(LDAPv2.TIMELIMIT, new Integer(timeLimit));
                ldc.setOption(LDAPv2.SIZELIMIT, new Integer(defaultMaxResults));
                setDefaultReferralCredentials(ldc);
                LDAPSearchConstraints constraints = ldc.getSearchConstraints();
                constraints.setMaxResults(defaultMaxResults);
                constraints.setServerTimeLimit(timeLimit);
                ldc.setSearchConstraints(constraints);
                connOptions.put("searchconstraints", constraints);

                if (cacheEnabled) {
                    ldapCache = new LDAPCache(cacheTTL, cacheSize);
                    ldc.setCache(ldapCache);
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: cacheTTL=" + cacheTTL
                            + "; cacheSize=" + cacheSize );
                    }
                }
            } catch (LDAPException lde) {
                int resultCode = lde.getLDAPResultCode();

                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: initConnectionPool setOption " +
                            "failed: " + resultCode);
                }
            }

            try {
                int tosec = timeLimit / 1000;

                if (tosec > 0) {
                    ldc.setConnectTimeout(timeLimit / 1000);
                } else  {
                    ldc.setConnectTimeout(3);
                }

                String ldapServerList = getLDAPServerList();
                ldc.connect(ldapServerList, 389, authid, authpw);
                connOptions.put("referrals", Boolean.valueOf(referrals));
                
                ldapHost = ldc.getHost();
                ldapPort = ldc.getPort();

                // Construct the pool by cloning the successful connection
                ShutdownManager shutdownMan = ShutdownManager.getInstance();

                if (shutdownMan.acquireValidLock()) {
                    try {
                        connPool = new LDAPConnectionPool("LDAPv3Repo", minPoolSize,
                            maxPoolSize, ldapServerList, ldapPort,
                            ldc.getAuthenticationDN(),
                                ldc.getAuthenticationPassword(),
                                ldc, connOptions);

                        // create the shutdown hook
                        shutdownListener = new ShutdownListener() {
                            public void shutdown() {
                                if (connPool != null) {
                                    connPool.destroy();
                                }
                            }
                        };
                        // Register the shutdown hook
                        shutdownMan.addShutdownListener(shutdownListener);
                    } finally {
                        shutdownMan.releaseLockAndNotify();
                    }
                }
            } catch (LDAPException lex) {
                int resultCode = lex.getLDAPResultCode();
                ldapConnError = Integer.toString(resultCode);
                debug.error("LDAPv3Repo: initConnectionPool ConnectionPool failed: " +
                            resultCode + "; to server " + ldapHost + ":" + ldapPort,lex);
                connPool = null;

                try {
                    ldc.disconnect();
                } catch (LDAPException lex1) {
                    debug.message("LDAPv3Repo: ldc.disconnect exception. "
                        + lex1.getLDAPResultCode());
                }
            }

        // if we get here and connPool is still null, all servers failed
        if (connPool == null) {
            debug.error("LDAPv3Repo: initConnectionPool: all servers offline: " +
                        ldapServerName + " connection pool create failed");
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit initConnectionPool ");
        }
    }

    protected void setDefaultReferralCredentials(LDAPConnection conn) {
        final LDAPConnection mConn = conn;
        
        reBind = new LDAPRebind() {
            public LDAPRebindAuth getRebindAuthentication(String host, int port)
            {
                return new LDAPRebindAuth(mConn.getAuthenticationDN(), mConn
                        .getAuthenticationPassword());
            }
        };
        LDAPSearchConstraints cons = conn.getSearchConstraints();
        cons.setRebindProc(reBind);
        conn.setSearchConstraints(cons);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#initialize(java.util.Map)
     */
    public void initialize(Map configParams) throws IdRepoException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: Initializing configuration()");
        }
        super.initialize(configParams);
        myConfigMap = configParams;

        String myServiceStr = getPropertyStringValue(configParams,
                IdConstants.SERVICE_ATTRS);
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: initialize: myServiceStr: "
                    + myServiceStr);
        }
        if ((myServiceStr != null) && (myServiceStr.length() != 0)) {
            myServiceMap = new HashMap(mySoapClient.decodeMap(myServiceStr));
        } else {
            myServiceMap = new HashMap();
        }
        if (debug.messageEnabled()) {
            if (myServiceMap != null) {
                debug.message("LDAPv3Repo: initialize: myServiceMap: "
                        + myServiceMap);
            } else {
                debug.message("LDAPv3Repo: initialize: myServiceMap = null");
            }
        }

        // find out which configuration/DS this is: AMDS, AD, generic DS
        setDSType(configParams);

        // get the organization name
        orgDN = getPropertyStringValue(configParams,
                LDAPv3Config_ORGANIZATION_NAME);

        timeLimit = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_TIME_LIMIT, timeLimit) * 1000;
        defaultMaxResults = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_MAX_RESULT, defaultMaxResults);

        cacheEnabled = getPropertyBooleanValue(configParams,
                LDAPv3Config_LDAP_CACHEENABLED);

        cacheTTL = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_CACHETTL, 600);  // in seconds

        cacheSize = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_CACHESIZE, 10240); // in bytes

        String scope = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_ROLES_SEARCH_SCOPE);
        if (scope != null && scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            roleSearchScope = LDAPv2.SCOPE_BASE;
        } else if (scope != null && scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            roleSearchScope = LDAPv2.SCOPE_ONE;
        } else {
            roleSearchScope = LDAPv2.SCOPE_SUB;
        }

        String searchScopeStr = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_SEARCH_SCOPE, LDAP_SCOPE_SUB);
        if (searchScopeStr.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            searchScope = LDAPv2.SCOPE_BASE;
        } else if (searchScopeStr.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            searchScope = LDAPv2.SCOPE_ONE;
        } else {
            searchScope = LDAPv2.SCOPE_SUB;
        }
        roleSearchScope = searchScope;

        userSearchFilter = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_USERS_SEARCH_FILTER);
        groupSearchFilter = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_GROUP_SEARCH_FILTER);
        roleSearchFilter = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_ROLES_SEARCH_FILTER, roleSearchFilter);
        filterroleSearchFilter = getPropertyStringValue(configParams,
           LDAPv3Config_LDAP_FILTERROLES_SEARCH_FILTER, filterroleSearchFilter);
        agentSearchFilter = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_AGENT_SEARCH_FILTER);
        userAuthNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_USERS_NAMING_ATTRIBUTE);
        userSearchNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_USERS_SEARCH_ATTRIBUTE);
        agentSearchNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_AGENT_SEARCH_ATTRIBUTE);
        groupSearchNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_GROUPS_SEARCH_ATTRIBUTE);
        roleSearchNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_ROLES_SEARCH_ATTRIBUTE);
        filterroleSearchNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_FILTERROLES_SEARCH_ATTRIBUTE, "cn");
        peopleCtnrNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_PEOPLE_CONTAINER_NAME);
        agentCtnrNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_AGENT_CONTAINER_NAME);
        groupCtnrNamingAttr = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_GROUP_CONTAINER_NAME);
        peopleCtnrValue = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_PEOPLE_CONTAINER_VALUE);
        agentCtnrValue = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_AGENT_CONTAINER_VALUE);
        groupCtnrValue = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_GROUP_CONTAINER_VALUE);

        Set tmpOC = (Set) configParams.get(LDAPv3Config_ROLE_OBJECT_CLASS);
        if (tmpOC == null) {
            roleObjClassSet = Collections.EMPTY_SET;
        } else {
            roleObjClassSet = new HashSet((Set) tmpOC);
        }
        
        tmpOC = (Set) configParams.get(LDAPv3Config_FILTERROLE_OBJECT_CLASS);
        if (tmpOC == null) {
            filterroleObjClassSet = Collections.EMPTY_SET;
        } else {
            filterroleObjClassSet = new HashSet((Set) tmpOC);
        }

        tmpOC = (Set) configParams.get(LDAPv3Config_GROUP_OBJECT_CLASS);
        if (tmpOC == null) {
            groupObjClassSet = Collections.EMPTY_SET;
        } else {
            groupObjClassSet = new HashSet((Set) tmpOC);
        }

        tmpOC = (Set) configParams.get(LDAPv3Config_USER_OBJECT_CLASS);
        if (tmpOC == null) {
            userObjClassSet = Collections.EMPTY_SET;
        } else {
            userObjClassSet = new HashSet((Set) tmpOC);
        }

        tmpOC = (Set) configParams.get(LDAPv3Config_AGENT_OBJECT_CLASS);
        if (tmpOC == null) {
            agentObjClassSet = Collections.EMPTY_SET;
        } else {
            agentObjClassSet = new HashSet((Set) tmpOC);
        }

        nsRoleAttr = getPropertyStringValue(configParams,
                LDAPv3Config_NSROLE, "nsrole");
        nsRoleDNAttr = getPropertyStringValue(configParams,
                LDAPv3Config_NSROLEDN, "nsRoleDN");
        nsRoleFilterAttr = getPropertyStringValue(configParams,
                LDAPv3Config_NSROLEFILTER, "nsRoleFilter");
        memberOfAttr = getPropertyStringValue(configParams,
                LDAPv3Config_MEMBEROF);
        uniqueMemberAttr = getPropertyStringValue(configParams,
                LDAPv3Config_UNIQUEMEMBER);
        defaultGrpMem = getPropertyStringValue(configParams,
                LDAPv3Config_DEFAULTGROUPMEMBER);
        memberURLAttr = getPropertyStringValue(configParams,
                LDAPv3Config_MEMBERURL);
        userAtttributesAllowed = new CaseInsensitiveHashSet();
        Set allowAttr = (Set) configParams.get(LDAPv3Config_USER_ATTR);
        if (allowAttr != null) {
            userAtttributesAllowed.addAll(allowAttr);
        }
        groupAtttributesAllowed = new CaseInsensitiveHashSet();
        allowAttr = (Set) configParams.get(LDAPv3Config_GROUP_ATTR);
        if (allowAttr != null) {
            groupAtttributesAllowed.addAll(allowAttr);
        }
        agentAtttributesAllowed = new CaseInsensitiveHashSet();
        allowAttr = (Set) configParams.get(LDAPv3Config_AGENT_ATTR);
        if (allowAttr != null) {
            agentAtttributesAllowed.addAll(allowAttr);
        }
        roleAtttributesAllowed = new CaseInsensitiveHashSet();
        allowAttr = (Set) configParams.get(LDAPv3Config_ROLE_ATTR);
        if (allowAttr != null) {
            roleAtttributesAllowed.addAll(allowAttr);
        }
        filteredroleAtttributesAllowed = new CaseInsensitiveHashSet();
        allowAttr = (Set) configParams.get(LDAPv3Config_FILTERROLE_ATTR);
        if (allowAttr != null) {
            filteredroleAtttributesAllowed.addAll(allowAttr);
        }
        userSpecifiedOpsSet = new HashSet((Set) configParams
                .get(IdConstants.SUPPORTED_OP));
        parsedUserSpecifiedOps(userSpecifiedOpsSet);
        isActiveAttrName = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_ISACTIVEATTRNAME);
        if (isActiveAttrName == null || isActiveAttrName.length() == 0) {
            alwaysActive = true;
            isActiveAttrName = defaultStatusAttribute;
        }
        inetUserActive = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_INETUSERACTIVE, statusActive);
        inetUserInactive = getPropertyStringValue(configParams,
            LDAPv3Config_LDAP_INETUSERINACTIVE, statusInactive);
        createUserAttrMap = getCreateUserAttrMapping(configParams);

        authenticatableSet = new CaseInsensitiveHashSet();
        Set tmpAuthSet = (Set)configParams.get(
            LDAPv3Config_LDAP_AUTHENTICATABLE);
        if (tmpAuthSet != null) {
            authenticatableSet.addAll(tmpAuthSet);
        }

        initConnectionPool(configParams);
        
        // check if connection pool is initialized properly
        checkConnPool(); 

        if (debug.messageEnabled()) {
            debug.message("    userObjClassSet: " + userObjClassSet);
            debug.message("    agentObjClassSet: " + agentObjClassSet);
            debug.message("    groupObjClassSet:" + groupObjClassSet);
            debug.message("    roleObjClassSet:" + roleObjClassSet);
            debug.message("    filterroleObjClassSet: "
                    + filterroleObjClassSet);
            debug.message("    userAtttributesAllowed: "
                    + userAtttributesAllowed);
            debug.message("    groupAtttributesAllowed: "
                    + groupAtttributesAllowed);
            debug.message("    agentAtttributesAllowed: "
                    +  agentAtttributesAllowed);
            debug.message("    roleAtttributesAllowed: "
                +  roleAtttributesAllowed);
            debug.message("    filteredroleAtttributesAllowed: "
                    +  filteredroleAtttributesAllowed);
            debug.message( "LDAPv3Repo: exit Initializing. "
                    + "timeLimit =" + timeLimit
                    + "; maxResults =" + defaultMaxResults
                    + "; roleSearchScope=" + roleSearchScope
                    + "; orgDN=" + orgDN
                    + "; createUserAttrMap=" + createUserAttrMap);
        }

    }

    @Override
    public void shutdown() {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: shutdown");
        }
        hasShutdown = true;
        super.shutdown();
        if (connPool != null) {
            connPool.destroy();
            connPool = null;
        }
        
        removeListener();
        
        if (shutdownListener != null) {
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    shutdownMan.removeShutdownListener(shutdownListener);
                    shutdownListener = null;                   
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: removed shutdown listener");
                    }
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
        }        
        
        reBind = null;
        myListener = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getSupportedOperations(
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getSupportedOperations(IdType type) {
        return (Set) supportedOps.get(type);
    }

    /*
     * (non-Javadoc
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedTypes()
     */
    public Set getSupportedTypes() {
        return supportedOps.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#isActive(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */    
    public boolean isActive(SSOToken token, IdType type, String name)
            throws IdRepoUnsupportedOpException, SSOException {
        /*
         * not every ldap has user user status attribute for active/inactive If
         * an attribute is configured, we check for "active/inactive". if the
         * attribute is not configured or attribute is configured but does not
         * exist in user entry or does not contain the word "active" then it is
         * assume active.
         */
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: isActive called: type:" + type
                    + "; name:" + name);
        }
        if (!type.equals(IdType.USER) && !type.equals(IdType.AGENT)) {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }

        if (alwaysActive) {
            try {
                boolean found = isExists(token, type, name);
                return found;
            } catch (IdRepoException ide) {
                return false;
            }
        }
        // agents sunIdentityServerDeviceStatus: Active
        String tmpActiveAttrName = null;
        String tmpInactiveAttrVal = null;
        if (type.equals(IdType.USER)) {
            tmpActiveAttrName = isActiveAttrName;
            tmpInactiveAttrVal = inetUserInactive;
        } else {
            tmpActiveAttrName = sunIdentityServerDeviceStatus;
            tmpInactiveAttrVal = statusInactive;
        }

        Map attrMap = null;
        HashSet attrNameSet = new HashSet();
        attrNameSet.add(tmpActiveAttrName);
        try {
            attrMap = getAttributes(token, type, name, attrNameSet);
            attrMap = new CaseInsensitiveHashMap(attrMap);
        } catch (IdRepoException idrepoerr) {
            if (debug.messageEnabled()) {
                debug.message("  LDAPv3Repo: isActive idrepoerr=" + idrepoerr);
            }
            return (false); // we can't determine user status.
        }

        if (debug.messageEnabled()) {
            debug.message("  LDAPv3Repo: isActive attrMap=" + attrMap);
        }

        Set attrSet = (Set)(attrMap.get(tmpActiveAttrName));
        if ((attrSet != null) && (attrSet.size() == 1)) {
            // in case of AD, we need to check if ADS_UF_ACCOUNTDISABLE
            // bits is on. 0x00000002 => account is disabled.
            if (dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD) &&
                type.equals(IdType.USER)) {
                int attrValue = Integer.parseInt(
                            (String) attrSet.iterator().next());
                boolean disable = (attrValue & disableMask) != 0;
                return (!disable);
            } else {
                String attrValue = (String) attrSet.iterator().next();
                return !attrValue.equalsIgnoreCase(tmpInactiveAttrVal);
            }
        } else {
            return (true);
        }

    }

    /* (non-Javadoc)
     * @see com.sun.identity.idm.IdRepo#setActiveStatus(
        com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
        java.lang.String, boolean)
     */
    public void setActiveStatus(SSOToken token, IdType type,
        String name, boolean active)
        throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: setActiveStatus. name=" + name +
                "; type=" + type + "; active=" + active);
        }
        if (!type.equals(IdType.USER) && !type.equals(IdType.AGENT)) {
            debug.error("LDAPv3Repo: setActiveStatus for identities other than"
                + " Users and Agent are not allowed ");
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);
        }
        String tmpActiveAttrName = null;
        String tmpActiveAttrVal = null;
        String tmpInactiveAttrVal = null;
        if (type.equals(IdType.USER)) {
            tmpActiveAttrName = isActiveAttrName;
            tmpActiveAttrVal = inetUserActive;
            tmpInactiveAttrVal = inetUserInactive;
        } else {
            // agents sunIdentityServerDeviceStatus: Active
            tmpActiveAttrName = sunIdentityServerDeviceStatus;
            tmpActiveAttrVal = statusActive;
            tmpInactiveAttrVal = statusInactive;
        }
        Map attrs = new HashMap();
        Set vals = new HashSet();

        if (dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD) &&
                type.equals(IdType.USER)) {
            // AD the active status is the second bit of
            // userAccountControl.
            HashSet attrNameSet = new HashSet();
            attrNameSet.add(tmpActiveAttrName);
            Map attrMap = getAttributes(token, type, name, attrNameSet);
            attrMap = new CaseInsensitiveHashMap(attrMap);
            Set attrSet = (Set)(attrMap.get(tmpActiveAttrName));
            int userAccountControl = Integer.parseInt(
                (String) attrSet.iterator().next());
            if (active) {
                userAccountControl = (userAccountControl & activeMask);
            } else {
                userAccountControl = (userAccountControl | disableMask);
            }
            vals.add(Integer.toString(userAccountControl));
        } else {
            if (active) {
                vals.add(tmpActiveAttrVal);
            } else {
                vals.add(tmpInactiveAttrVal);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: setActiveStatus value=" + vals);
        }
        attrs.put(tmpActiveAttrName, vals);
        setAttributes(token, type, name, attrs, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#isExists(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public boolean isExists(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: isExists called " + type + ": " + name);
        }
        checkConnPool();

        String dn;
        LDAPEntry foundEntry = null;
        try {
            dn = getDN(type, name);
        } catch (IdRepoUnsupportedOpException ide) {
            return false;
        } catch (IdRepoException idrepoerr) {
            return false;
        }
        LDAPConnection ld = null;
        int resultCode = 0;
        try {
            LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(dn,
                 timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
                 defaultMaxResults);
            ld = connPool.getConnection();
            enableCache(ld);
            foundEntry = ld.read(request);
        } catch (LDAPException e) {
            connPool.close(ld, e.getLDAPResultCode());
            ld = null;
            switch (e.getLDAPResultCode()) {
            case LDAPException.NO_SUCH_OBJECT:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: The specified entry " +
                            "does not exist.");
                }
                break;
            case LDAPException.LDAP_PARTIAL_RESULTS:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: Entry served by a " +
                            "different LDAP server.");
                }
                break;
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: You do not have the " +
                            "access rights to perform this operation.");
                }
                break;
            default:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: Error number: "
                            + e.getLDAPResultCode());
                    debug.message("LDAPv3Repo: isExists: "
                            + "Could not read the specified entry.");
                }
                break;
            }
            resultCode = e.getLDAPResultCode();
            if ((resultCode == 80) || (resultCode == 81) ||
                (resultCode == 82)) {
                String ldapError = Integer.toString(resultCode);
                Object[] args = {CLASS_NAME, LDAPv3Bundle.getString(ldapError)};
                IdRepoFatalException ide = new IdRepoFatalException(
                        IdRepoBundle.BUNDLE_NAME, "311", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide;
            }
            return false;
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        return true;
    }
    
    private boolean isExists(IdType type, String dn)
            throws IdRepoException, SSOException {
      if (debug.messageEnabled()) {
          debug.message("LDAPv3Repo: isExists called " + type 
              + ": " + dn);
      }
      checkConnPool();

      LDAPEntry foundEntry = null;

        LDAPConnection ld = null;
        int resultCode = 0;
        try {
            LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(dn,
                timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
                defaultMaxResults);
            ld = connPool.getConnection();
            enableCache(ld);
            foundEntry = ld.read(request);
        } catch (LDAPException e) {
            connPool.close(ld, e.getLDAPResultCode());
            ld = null;
            switch (e.getLDAPResultCode()) {
            case LDAPException.NO_SUCH_OBJECT:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: " +
                        "The specified entry does not exist.");
                }
                break;
            case LDAPException.LDAP_PARTIAL_RESULTS:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: Entry served by a " +
                            "different LDAP server.");
                }
                break;
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: You do not have the " +
                            "access rights to perform this operation.");
                }
                break;
            default:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: Error number: "
                            + e.getLDAPResultCode());
                    debug.message("LDAPv3Repo: isExists: "
                            + "Could not read the specified entry.");
                }
                break;
            }

            resultCode = e.getLDAPResultCode();
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82)){
                String ldapError = Integer.toString(resultCode);
                Object[] args = {CLASS_NAME, LDAPv3Bundle.getString(ldapError)};
                IdRepoFatalException ide = new IdRepoFatalException(
                        IdRepoBundle.BUNDLE_NAME, "311", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide;
            }
            return false;
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        return true;
    }
    

    public void removeListener() {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: removeListener called ");
        }
        HashSet tobeRemoveListener = new HashSet();
        if (ldapServerName == null) {
            getLDAPServerName(myConfigMap);
        }
        if (ldapServerName == null) {
            debug.error("LDAPv3Repo: removeListener failed. missing ldap " +
                    "server name.");
        }
        LDAPv3EventService eventService;
        
        // keep a count with eventsMgr.ldapServerName of number of listener.
        // when the count reaches 0. remove the ldapServerName entry.
        // see if we already have an event service for this server.
        eventService = (LDAPv3EventService) _eventsMgr
                .get(ldapServerName);
        if (eventService != null) {
        	//OPENAM-1007 synchronizing with listOfPS caused deadlock
        	//synchronizing with eventservice instead
        	synchronized (eventService) {
        	if (hasListener) {
                // find the listener idrepo.
                // if this is the last, then remove the listener.
                // if not, just remove teh idrepo from list.
                // need to change findrequest.

                String psIdKey = getPSKey(myConfigMap);
                Map listOfRepo = (Map) listOfPS.get(psIdKey);
                if (listOfRepo != null) { 
                    HashSet listOfDS = (HashSet) listOfRepo.get("listOfDS");
                    listOfDS.remove(this);
                    if (listOfDS.isEmpty()) { 
                        listOfPS.remove(psIdKey);
                        tobeRemoveListener.add(psIdKey);
                        Integer requestNum = (Integer) _numRequest.get(
                            ldapServerName);
                        if (requestNum != null) {
                            int requestInt = requestNum.intValue();
                            if (requestInt <= 1) {
                                _eventsMgr.remove(ldapServerName);
                                eventService.finalize();
                                _numRequest.remove(ldapServerName);
                            } else {
                                _numRequest.remove(ldapServerName);
                                _numRequest.put(ldapServerName, new Integer(
                                    requestInt - 1));
                            }
                            if (debug.messageEnabled()) {
                                debug.message("LDAPv3Repo: removeListener. " +
                                    "requestInt=" + requestInt);
                            }
                        }
                   }
                }
            } else { // listener was not added
                Integer requestNum = (Integer) _numRequest.get(ldapServerName);
                if (requestNum == null) {
                    _eventsMgr.remove(ldapServerName);
                }
            }
        	} // end of synchronized(eventService)
        }
        Iterator iter = tobeRemoveListener.iterator();
        while (iter.hasNext()) {
            String psIdKeyToRemove = (String) iter.next();
            eventService.removeListener(psIdKeyToRemove);
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#addListener(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.AMObjectListener, java.util.Map)
     */
    public int addListener(SSOToken token, IdRepoListener listener)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: addListener called"
                    + "  LDAPv3Config_LDAP_SERVER="
                    + configMap.get(LDAPv3Config_LDAP_SERVER)
                    + "; LDAPv3Config_LDAP_PSEARCHBASE="
                    + configMap.get(LDAPv3Config_LDAP_PSEARCHBASE)
                    + "; LDAPv3Config_LDAP_PSEARCHFILTER="
                    + configMap.get(LDAPv3Config_LDAP_PSEARCHFILTER)
                    + "; LDAPv3Config_LDAP_IDLETIMEOUT="
                    + configMap.get(LDAPv3Config_LDAP_IDLETIMEOUT));
        }
        if (connPool == null) {
            debug.error("LDAPv3Repo: addListener failed. Incorrect ldap "+
                "server configuration." + 
                configMap.get(LDAPv3Config_LDAP_SERVER)); 
            return 0;
        }

            checkConnPool();
            // TODO Auto-generated method stub
            // listener.setConfigMap(configMap);
            myListener = listener;

            // If CLI, do not have persistent search for notifications.
            if (!SystemProperties.isServerMode()) {
                return 0;
            }
            // see if we already have an event service for this server.
            if (ldapServerName == null) {
                getLDAPServerName(myConfigMap);
            }
            if (ldapServerName == null) {
                debug.error("LDAPv3Repo: addListener failed. missing "+
                    "ldap server name.");
                return 0;
            }

            String searchBase = getPropertyStringValue(myConfigMap,
                LDAPv3Config_LDAP_PSEARCHBASE);
            if (searchBase == null) {
                debug.error("LDAPv3Repo: addListener failed. "
                    + "missing persistence search base. Not "
                        + "starting persistent search.");
                return 0;
            }

            // if the listener shares the same host, we tried to use the same
            // event_service to minialize threads and polling and monitors.  
            LDAPv3EventService eventService = (LDAPv3EventService) _eventsMgr
                .get(ldapServerName);
            if (eventService == null) {
                int idleTimeOut = getPropertyIntValue(myConfigMap,
                    LDAPv3Config_LDAP_IDLETIMEOUT, 0);
                try {
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo.addListener: "  +
                            "eventService is null. idleTimeOut=" +idleTimeOut);
                    }
                    if (idleTimeOut == 0) {
                        eventService = new LDAPv3EventService(myConfigMap,
                            ldapServerName);
                    } else {
                        eventService = new LDAPv3EventServicePolling(
                            myConfigMap, ldapServerName);
                    }
                    _eventsMgr.put(ldapServerName, eventService);
                } catch (LDAPException le) {
                    debug.error("LDAPv3Repo: addListener failed. " +
                        "new eventService failed. LDAPException=", le);
                    String ldapError = 
                        Integer.toString(le.getLDAPResultCode());
                    Object[] args = { CLASS_NAME };
                    IdRepoException ide = new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "218", args);
                    ide.setLDAPErrorCode(ldapError);
                    throw ide;
                }
            }
        
        //OPENAM-1007 synchronizing with listOfPS caused deadlock
        //synchronizing with eventservice instead
        synchronized (eventService) {
            String psIdKey = getPSKey(myConfigMap);
            Map listOfRepo = (Map) listOfPS.get(psIdKey);
            if (listOfRepo == null) { // no ps found for this host.
                // add the ps/listener.
                String filter = getPropertyStringValue(myConfigMap,
                    LDAPv3Config_LDAP_PSEARCHFILTER, "(objectclass=*)");
                String psearchScopeStr = getPropertyStringValue(myConfigMap,
                    LDAPv3Config_LDAP_PSEARCHSCOPE, LDAP_SCOPE_SUB);
                int psearchScope = LDAPv2.SCOPE_SUB;
                if (psearchScopeStr.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
                    psearchScope = LDAPv2.SCOPE_BASE;
                } else if (psearchScopeStr.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
                    psearchScope = LDAPv2.SCOPE_ONE;
                } else {
                    psearchScope = LDAPv2.SCOPE_SUB;
                }
                try {
                    // might have to change/add/delete params.  psIdKey
                    eventService.addListener(token, listener, searchBase,
                        psearchScope, filter, PS_OP, myConfigMap, this,
                            ldapServerName, psIdKey);
                    Integer numRequest = 
                        (Integer) _numRequest.get(ldapServerName);
                    int requestNum;
                    if (numRequest == null) {
                        requestNum = 1;
                    } else {
                        requestNum = numRequest.intValue() + 1;
                    }
                    _numRequest.put(ldapServerName, new Integer(requestNum));
                } catch (IdRepoException idrepoex) {
                    debug.error("LDAPv3Repo: addListener failed. persistant "+
                        "search not supported");
                } catch (LDAPException ldapex) {
                    debug.error("LDAPv3Repo: addListener failed. " +
                             "eventService.addListener.LDAPException", ldapex);
                    Object[] args = { CLASS_NAME };
                    IdRepoException ide = new IdRepoException(
                       IdRepoBundle.BUNDLE_NAME, "218", args);
                    ide.setLDAPErrorCode(Integer.toString(
                        ldapex.getLDAPResultCode()));
                    throw ide;
                }    
                
                // create an entry in the list.
                HashSet listOfDS = new HashSet();
                listOfDS.add(this);
                listOfRepo = new HashMap();
                listOfRepo.put("eventServices", eventService); 
                listOfRepo.put("listOfDS", listOfDS);
                listOfPS.put(psIdKey, listOfRepo);
            } else {
                //found a ps to this host with same characteristic. share it.
                HashSet listOfDS = (HashSet) listOfRepo.get("listOfDS");
                listOfDS.add(this);
            }
        
            // probably should save the reqID with our listener.
            // once we have our listener we can get at our config 
            // listener.getconfig
            // will this id change? what happens it timeout and have to be
            // restarted.
            hasListener = true;
            return 0;
        }
    }

    /*
     * given a Map of attributes(attrMap), return a Map with only attributes
     * that were predefined to be permited or allowed.
     */
    private Map getAllowedAttrs(IdType type, Map attrMap) {
        Set predefinedAttr = Collections.EMPTY_SET;
        if (type.equals(IdType.USER)) {
            predefinedAttr = userAtttributesAllowed;
        } else if (type.equals(IdType.AGENT)) {
            predefinedAttr = agentAtttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            predefinedAttr = groupAtttributesAllowed;
        } else if (type.equals(IdType.ROLE)) {
            predefinedAttr = roleAtttributesAllowed;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            predefinedAttr = filteredroleAtttributesAllowed;
        }
        
        Map allowedAttr = new CaseInsensitiveHashMap();

        Iterator itr = predefinedAttr.iterator();
        while (itr.hasNext()) {
            String attrName = (String) itr.next();
            Set attrNameValue = (Set) attrMap.get(attrName);
            if (attrNameValue != null) {
                allowedAttr.put(attrName, attrNameValue);
            }
        }
        return (allowedAttr);
    }

    private Map doUserStatusMapping(IdType type,
        String name, Map attrMap) {
        // replace the default user status attribute name and value
        // with what's configured on datastore config page.
        // add the attribute if it does not exist.
        if (!type.equals(IdType.USER) && !type.equals(IdType.AGENT)) {
            return attrMap;
        }
        Set newAttrValue = new HashSet();
        if (attrMap.containsKey(isActiveAttrName)) {
            // the inetuserstatus attribute exist.
            Set previousVal = (Set) attrMap.get(isActiveAttrName);
            String previousStatus = (previousVal.iterator().hasNext()) ?
                (String) previousVal.iterator().next() : null;
            attrMap.remove(isActiveAttrName);
            if ((previousStatus != null) &&
                previousStatus.equalsIgnoreCase(statusInactive)) {
                newAttrValue.add(inetUserInactive);
            } else {
                newAttrValue.add(inetUserActive);
            }
            attrMap.put(isActiveAttrName, newAttrValue);
        } else {
           newAttrValue.add(inetUserActive);
           attrMap.put(isActiveAttrName, newAttrValue);
        }
        return attrMap;
    }

    private Map addAttrMapping(IdType type, String name, Map attrMap) {
        // add missing required atttr and its default value.
        if (debug.messageEnabled()) {
            debug.message("enter addAttrMapping: createUserAttrMap="
                    + createUserAttrMap + ", attrMap = " +
                    IdRepoUtils.getAttrMapWithoutPasswordAttrs(attrMap, null));
        }
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            Iterator itr = createUserAttrMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String)itr.next();
                 if (!attrMap.containsKey(attrName)) {
                     String mapAttrName =
                         (String) createUserAttrMap.get(attrName);
                     // if the attrname is same as the attrvalue.
                     //  see getCreateUserAttrMapping
                     // special case, use the username as the value of
                     // the attribute..
                     if (mapAttrName.equalsIgnoreCase(attrName)) {
                         Set mapAttrValue = new HashSet();
                         mapAttrValue.add(name);
                         attrMap.put(attrName, mapAttrValue);
                     } else {
                         Set mapAttrValue = (Set) attrMap.get(mapAttrName);
                         if (mapAttrValue != null) {
                             attrMap.put(attrName, mapAttrValue);
                         }
                     }
                 }
            }
        }
        if (type.equals(IdType.GROUP)) {
            String rdn = getNamingAttr(type);
            Set rdnValue = (Set) attrMap.get(rdn);
            if ((rdnValue == null) || rdnValue.isEmpty()) {
                Set cnAttrValue = new HashSet();
                cnAttrValue.add(name);
                attrMap.put(rdn, cnAttrValue);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("exit addAttrMapping: attrMap = " +
                IdRepoUtils.getAttrMapWithoutPasswordAttrs(attrMap, null));
        }
        return attrMap;
    }

    private Set convertInetStatus(Set attrValueSet, String attrName) {
        // in AD mode, and the user requested inetuserstatus, and the
        // current attribute is userAccountControl. set inetuserstatus
        // according to userAccountControl ADS_UF_ACCOUNTDISABLE bits.
        if (debug.messageEnabled()) {
            debug.message("convertInetStatus: attrName=" + attrName
                + ";  attrValueSet=" + attrValueSet);
        }
        Set newAttrVal = new HashSet();
        if (dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD)) {
            int attrValue = Integer.parseInt(
                (String) attrValueSet.iterator().next());
            if ((attrValue & 0x2) != 0) {
                newAttrVal.add(statusInactive);
            } else {
                newAttrVal.add(statusActive);
            }
        } else {
            // ADAM
            String value = (String)attrValueSet.iterator().next();
            if (value.equalsIgnoreCase(inetUserInactive)) {
                newAttrVal.add(statusInactive);
            } else {
                newAttrVal.add(statusActive);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("convertInetStatus: newAttrVal=" +
                newAttrVal);
        }
        return newAttrVal;
    }

    private byte[][]  convertInetStatus(LDAPAttribute ldapAttr) {
        // in AD mode, and the user requested inetuserstatus, and the
        // current attribute is userAccountControl. set
        // inetuserstatus according to userAccountControl bits.
        if (debug.messageEnabled()) {
            debug.message("convertInetStatus: attrName: " +
                ldapAttr.getName() + "; values=" + ldapAttr.getStringValues());
        }
        byte[][] newAttrVal = new byte[1][];
        if (dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD)) {

            Set attrValueSet = new HashSet();
            Enumeration enumVals = ldapAttr.getStringValues();
            while ((enumVals != null) && enumVals.hasMoreElements()) {
                String value = (String) enumVals.nextElement();
                    attrValueSet.add(value);
            }
            int attrValue = Integer.parseInt(
                (String) attrValueSet.iterator().next());
            if ((attrValue & 0x2) != 0) {
                newAttrVal[0] = statusInactive.getBytes();
            } else {
                newAttrVal[0] = statusActive.getBytes();
            }
        } else {
            // ADAM
            String value = (String)ldapAttr.getStringValues().nextElement();
            if (value.equalsIgnoreCase(inetUserInactive)) {
                newAttrVal[0] = statusInactive.getBytes();
            } else {
                newAttrVal[0] = statusActive.getBytes();
            }
        }
        if (debug.messageEnabled()) {
            debug.message("convertInetStatus exit: newAttrVal: "
                + newAttrVal);
        }
        return newAttrVal;
    }

    private void appendInetUser(SSOToken token, IdType type,
            String name, Map attributes, boolean isString)
            throws IdRepoException, SSOException {
        /*
            if he is setting userAccountControl then don't have
            to check for inetuserstatus. can assume he will take
            care of the account_disable bit.
            if he is setting inetuserstatus and not userAccountControl,
            then we should read userAccountControl and set/mask
            userAccountControl according to inetuserstatus and remove
            inetuserstatus for the list of attributes and add
            userAccountControl to the list of attributes.
        */

        if (attributes.containsKey(defaultStatusAttribute) &&
            (!attributes.containsKey(isActiveAttrName))) {
            if (dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD)) {
                // read userAccountControl so we can mask the active bit.
                HashSet attrNameSet = new HashSet();
                attrNameSet.add(isActiveAttrName);
                Map attrRead = getAttributes(token, type, name, attrNameSet);
                attrRead = new CaseInsensitiveHashMap(attrRead);
                Set userAcctSet = (Set)(attrRead.get(isActiveAttrName));
                int userAccountControl = Integer.parseInt(
                    (String) userAcctSet.iterator().next());
                String userStatus = null;
                if (isString) {
                    Set attrSet =
                        (Set) attributes.get(defaultStatusAttribute);
                    userStatus = (String) attrSet.iterator().next();
                } else {
                    byte[][] attrBytes =
                        (byte[][]) attributes.get(defaultStatusAttribute);
                    userStatus = new String(attrBytes[0]);
                }
                if (userStatus.equalsIgnoreCase(statusInactive)) {
                    userAccountControl = (userAccountControl | disableMask);
                } else {
                    userAccountControl = (userAccountControl & activeMask);
                }
                attributes.remove(defaultStatusAttribute);
                if (isString) {
                    HashSet usrCtrlSet = new HashSet();
                    usrCtrlSet.add(Integer.toString(userAccountControl));
                    attributes.put(isActiveAttrName, usrCtrlSet);
                } else {
                    byte [][] usrCtlBin = new byte[1][];
                    usrCtlBin[0] =
                        Integer.toString(userAccountControl).getBytes();
                    attributes.put(isActiveAttrName, usrCtlBin);
                }

            } else if (dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3ADAM)) {

                Set attrSet = (Set)attributes.get(defaultStatusAttribute);
                if ((attrSet != null) && (!attrSet.isEmpty())) {
                    String userStatus = (String) attrSet.iterator().next();
                    String attrValue = 
                        userStatus.equalsIgnoreCase(statusInactive) ?
                        inetUserInactive : inetUserActive;
                    attributes.remove(defaultStatusAttribute);
                    Set attrValues = new HashSet();
                    attrValues.add(attrValue);
                    attributes.put(isActiveAttrName, attrValues);
                }

            }
        }
    }

    private byte[] encodeADPwd(String passwd) {
        // encode the AD password. 
        // the password must be enclosed in double quotes
        // and converted byte array with encoding "UTF-16LE"
        byte[] encodedPwd = null;
        try {
            encodedPwd = ("\"" + passwd + "\"").getBytes("UTF-16LE");
        } catch (UnsupportedEncodingException uex) {
            if (debug.warningEnabled()) {
                debug.warning("LDAPv3Repo.encodeADPwd:", uex);
            }
        }
        return encodedPwd;
    }

    private byte[] doPasswordEncode(IdType type,
        String name, Map attrMap, String attrName) {
        // replace the default user status attribute name and value
        // with what's configured on datastore config page.
        // add the attribute if it does not exist.
        if (type.equals(IdType.USER) && 
            (dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD) ||
            dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3ADAM))) {
            Set previousVal = (Set) attrMap.remove(attrName);
            
            if ((previousVal != null) && (!previousVal.isEmpty())) {
                Set newAttrValue = new HashSet();
                String previousPass = (String) previousVal.iterator().next();
                if (previousPass != null) {
                    return encodeADPwd(previousPass);
                }
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#create(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map)
     */
    public String create(SSOToken token, IdType type, String name, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: Create called on " + type + ": " + name +
            " attrMap = " + IdRepoUtils.getAttrMapWithoutPasswordAttrs(attrMap,
            null));

        }
 
        checkConnPool();
        if (name.startsWith("#")) {
            name = "\\" + name;
        }
        String eDN = getDN(type, name);
        LDAPConnection ld = null;
        int resultCode = 0;
        byte[] encodedPwd = null;
        Map origAttrMap = null;
        Set theOC = null;
        if (type.equals(IdType.USER)) {
            theOC = userObjClassSet;
        } else if (type.equals(IdType.AGENT)) {
            theOC = agentObjClassSet;
        } else if (type.equals(IdType.GROUP)) {
            theOC = groupObjClassSet;
        } else if (type.equals(IdType.ROLE)) {
            theOC = roleObjClassSet;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            theOC = filterroleObjClassSet;
        } else {
            Object[] args = { CLASS_NAME, IdOperation.CREATE.getName(),
                type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        }

        origAttrMap = attrMap;
        attrMap = new CaseInsensitiveHashMap(attrMap);
        encodedPwd = doPasswordEncode(type, name, attrMap, unicodePwd);
        attrMap = doUserStatusMapping(type, name, attrMap);
        attrMap = addAttrMapping(type, name, attrMap);
        attrMap = getAllowedAttrs(type, attrMap);

        LDAPAttributeSet ldapAttrSet = new LDAPAttributeSet();
        if (attrMap != null && !attrMap.isEmpty()) {
            // add the default objectclass to the attrMap passed in.
            boolean addedOC = false;
            Map privAttrMap = new HashMap();

            Iterator itr = attrMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                if (attrName.equalsIgnoreCase(LDAP_OBJECT_CLASS)) {
                    Set attrNameValue = ((Set) attrMap.get(attrName));
                    HashSet newNameValue = new HashSet(attrNameValue);
                    newNameValue.addAll(theOC);
                    privAttrMap.put(attrName, newNameValue);
                    addedOC = true;
                } else {
                    Set attrNameValue = (Set) attrMap.get(attrName);
                // ignore empty set because some servers can't handle it.
                    if ((attrNameValue != null) &&
                        (!attrNameValue.isEmpty())) {

                        if (dsType.equalsIgnoreCase(
                            LDAPv3Config_LDAPV3AD) ||
                            dsType.equalsIgnoreCase(
                            LDAPv3Config_LDAPV3ADAM)) {

                            attrNameValue.remove("");
                            if (attrNameValue.isEmpty()) {
                                continue;
                            }
                        } 
                        privAttrMap.put(attrName, attrNameValue);
                    }
                }
            }

            if (!addedOC) {// object class not in attrMap passed in, add it.
                privAttrMap.put(LDAP_OBJECT_CLASS, theOC);
            }

            String namingAttr = getNamingAttr(type);
            Set values = new HashSet();
            values.add(name);
            privAttrMap.put(namingAttr, values);
            itr = privAttrMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                Set set = (Set) (privAttrMap.get(attrName));
                String attrValues[] = (set == null ? null : (String[]) set
                    .toArray(new String[set.size()]));

                if (attrName.equals(namingAttr)) {
                    for (int i = 0; i < attrValues.length; i++) {
                        if (attrValues[i].startsWith("#")) {
                            String s = "\\"+attrValues[i];
                            attrValues[i] = s;
                        }
                    }
                }

                if (debug.messageEnabled()) {
                    if (attrName.equalsIgnoreCase("userpassword")) {
                        debug.message("    : attrName= " + attrName);
                    } else {
                        debug.message("    : attrName= " + attrName +
                            " set:" + set);
                    }
                }
                ldapAttrSet.add(new LDAPAttribute(attrName, attrValues));
            } // null
        } else {
            String attrValues[] = (theOC == null ? null : (String[]) theOC
                .toArray(new String[theOC.size()]));
            ldapAttrSet.add(new LDAPAttribute(LDAP_OBJECT_CLASS,
                attrValues));

        }
        if (type.equals(IdType.GROUP) && (defaultGrpMem != null)) {  
            // add default user to group.
            ldapAttrSet.add(new LDAPAttribute(uniqueMemberAttr,
                defaultGrpMem));
        }
        if (debug.messageEnabled()) {
            debug.message("    : before ld.add: eDN=" + eDN);
        }

        if (encodedPwd != null) {
            ldapAttrSet.add(new LDAPAttribute(unicodePwd, encodedPwd));
        }

        try {
            LDAPEntry theEntry = new LDAPEntry(eDN, ldapAttrSet);
            LDAPAddRequest request = LDAPRequestParser.parseAddRequest(
                theEntry);
            ld = connPool.getConnection();
            enableCache(ld);
            ld.add(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            debug.error("LDAPv3Repo.create failed. errorCode=" +
                lde.getLDAPResultCode() + "  " + lde.getLDAPErrorMessage());
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.create failed", lde);
            }
            attrMap = origAttrMap;
            handleLDAPException(lde, eDN);
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }

        if (type.equals(IdType.GROUP) && (defaultGrpMem != null)) {
            if (memberOfAttr != null) {
                checkConnPool();
                try {
                    LDAPAttribute mbrOf = new LDAPAttribute(memberOfAttr, eDN);
                    LDAPModification modMemberOf = 
                        new LDAPModification(LDAPModification.ADD, mbrOf);
                    LDAPModifyRequest request =
                        LDAPRequestParser.parseModifyRequest(defaultGrpMem,
                        modMemberOf);
                    ld = connPool.getConnection();
                    ld.modify(request);
                } catch (LDAPException lde) {
                    resultCode = lde.getLDAPResultCode();
                    connPool.close(ld, resultCode);
                    ld = null;
                    debug.error("LDAPv3Repo.create failed mod user. errorCode="
                        + lde.getLDAPResultCode() + "  " +
                        lde.getLDAPErrorMessage());
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo.create failed unable to " +
                            "mod user", lde);
                    }
                    attrMap = origAttrMap;
                    handleLDAPException(lde, eDN);
                } finally {
                    if (ld != null) {
                        connPool.close(ld);
                    }
                }
                if (cacheEnabled) {
                    ldapCache.flushEntries(defaultGrpMem,
                        LDAPv2.SCOPE_BASE);
                }
            }
        }
        attrMap = origAttrMap;
        return eDN;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#delete(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public void delete(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: delete called on " + type + ": " + name);
        }

        checkConnPool();
        String eDN = getDN(type, name);
        LDAPConnection ld = null;
        int resultCode = 0;
        try {
            LDAPDeleteRequest request = LDAPRequestParser.parseDeleteRequest(
                eDN);

            ld = connPool.getConnection();
            enableCache(ld);
            ld.delete(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            String ldeErrMsg = lde.getLDAPErrorMessage();
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: delete, error: " +
                    resultCode + "errmsg=" + ldeErrMsg, lde);
            }
            handleLDAPException(lde, eDN);
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set)
     */
    public Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAttributes 1 called " + ": " + type
                    + ": " + name + " ; attrName=" + attrNames);
        }

        Map myMap = getAttributes(token, type, name, attrNames, true);
        return myMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set)
     */
    private Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames, boolean isString) throws IdRepoException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAttributes 2 called" + ": " + type
                    + ": " + name + " ; attrName=" + attrNames);
        }

        Map theAttrMap = new HashMap();
        CaseInsensitiveHashSet attrNamesCase = (attrNames == null) ?
            new CaseInsensitiveHashSet(Collections.EMPTY_SET) :
            new CaseInsensitiveHashSet(attrNames);
        if (type.equals(IdType.REALM)) {
            if ((attrNames != null) &&
                attrNamesCase.contains("objectclass")) {
                return theAttrMap;
            }
        }

        checkConnPool();
        String dn = getDN(type, name);
        LDAPConnection ld = null;
        int resultCode = 0;

            boolean addActiveAttrName = false;
            CaseInsensitiveHashSet predefinedAttr = null;
            if (type.equals(IdType.USER)) {
                if (!userAtttributesAllowed.contains("nsrole")) {
                    predefinedAttr = new CaseInsensitiveHashSet();
                    Iterator itr = userAtttributesAllowed.iterator();
                    while (itr.hasNext()) {
                        predefinedAttr.add(itr.next());
                    }
                    predefinedAttr.add("nsrole");
                } else {
                    predefinedAttr = userAtttributesAllowed;
                }
                if ((dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD) ||
                    dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3ADAM))
                    && attrNamesCase.contains(defaultStatusAttribute)) {

                    if (!attrNamesCase.contains(isActiveAttrName)) {
                        addActiveAttrName = true;
                        attrNamesCase.add(isActiveAttrName);
                    }
                }
            } else if (type.equals(IdType.AGENT)) {
                predefinedAttr = agentAtttributesAllowed;
            } else if (type.equals(IdType.GROUP)) {
                predefinedAttr = groupAtttributesAllowed;
            } else if (type.equals(IdType.ROLE)) {
                predefinedAttr = roleAtttributesAllowed;
            } else if (type.equals(IdType.FILTEREDROLE)) {
                predefinedAttr = filteredroleAtttributesAllowed;
            }

            if (debug.messageEnabled()) {
                debug.message("  LDAPv3Repo: predefinedAttr=" + predefinedAttr
                    + "; attrNames=" + attrNames);
            }

            LDAPEntry foundEntry = null;
            if ((attrNames == null) || (attrNames.contains("*"))) {
                if ((predefinedAttr == null) ||
                    (predefinedAttr.isEmpty()))  {
                    LDAPSearchRequest request =
                        LDAPRequestParser.parseReadRequest(dn, timeLimit,
                        LDAPRequestParser.DEFAULT_DEREFERENCE,
                        defaultMaxResults);
                    try {
                        ld = connPool.getConnection();
                        enableCache(ld);
                        foundEntry = ld.read(request);
                    } catch (LDAPException lde) {                        
                        resultCode = lde.getLDAPResultCode();
                        connPool.close(ld, resultCode);
                        ld = null;
                        String ldeErrMsg = lde.getLDAPErrorMessage();
                        if (debug.warningEnabled()) {
                            debug.warning("LDAPv3Repo.getAttributes failed. errorCode=" +
                                lde.getLDAPResultCode() + "  " + ldeErrMsg);
                        }
                        handleLDAPException(lde, dn);                           
                    } finally {
                        if (ld != null) {
                            connPool.close(ld);
                        }
                    }
                } else {
                    LDAPSearchRequest request =
                        LDAPRequestParser.parseReadRequest(dn,
                        (String[])predefinedAttr.toArray(
                        new String[predefinedAttr.size()]), timeLimit,
                        LDAPRequestParser.DEFAULT_DEREFERENCE,
                        defaultMaxResults);
                    try {
                        ld = connPool.getConnection();
                        enableCache(ld);
                        foundEntry = ld.read(request);
                        attrNamesCase = predefinedAttr;
                    } catch (LDAPException lde) {
                        resultCode = lde.getLDAPResultCode();
                        connPool.close(ld, resultCode);
                        ld = null;
                        String ldeErrMsg = lde.getLDAPErrorMessage();
                        if (debug.warningEnabled()) {
                            debug.warning("LDAPv3Repo.getAttributes failed. errorCode=" +
                                lde.getLDAPResultCode() + "  " + ldeErrMsg);
                        }
                        handleLDAPException(lde, dn);       
                    } finally {
                        if (ld != null) {
                            connPool.close(ld);
                        }
                    }
                }
            } else {
                if (predefinedAttr != null) {
                    CaseInsensitiveHashSet allowedAttrNames =
                        new CaseInsensitiveHashSet();
                    Iterator itr = attrNamesCase.iterator();
                    while (itr.hasNext()) {
                        String attrName = (String) itr.next();
                        if (predefinedAttr.contains(attrName)) {
                            allowedAttrNames.add(attrName);
                        }
                    }
                    attrNamesCase = allowedAttrNames;
                    if (attrNamesCase.isEmpty()) {
                        return theAttrMap;  // nothing to read.
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("  LDAPv3Repo: before read: attrNames="
                            + attrNames);
                }
                LDAPSearchRequest request =
                    LDAPRequestParser.parseReadRequest(dn,
                    (String[])attrNamesCase.toArray(
                        new String[attrNamesCase.size()]), timeLimit,
                        LDAPRequestParser.DEFAULT_DEREFERENCE,
                        defaultMaxResults);
                try {
                    ld = connPool.getConnection();
                    enableCache(ld);
                    foundEntry = ld.read(request);
                } catch (LDAPException lde) {
                    resultCode = lde.getLDAPResultCode();
                    connPool.close(ld, resultCode);
                    ld = null;
                    String ldeErrMsg = lde.getLDAPErrorMessage();
                    if (debug.warningEnabled()) {
                        debug.warning("LDAPv3Repo.getAttributes failed. errorCode=" +
                            lde.getLDAPResultCode() + "  " + ldeErrMsg);
                    }
                    handleLDAPException(lde, dn);
                } finally {
                    if (ld != null) {
                        connPool.close(ld);
                    }
                }
            }
            if (foundEntry == null) {
                debug.error("getAttributes: unable to find dn:" + dn
                        + " to retrieve its attributes.");
                Object[] args = { CLASS_NAME, dn };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "211", args);
            }
            // convert from LDAPAttributeSet to Map.
            LDAPAttributeSet ldapAttrSet = foundEntry.getAttributeSet();
            int size = ldapAttrSet.size();
            for (int i = 0; i < size; i++) {
                LDAPAttribute ldapAttr = ldapAttrSet.elementAt(i);
                if (ldapAttr != null) {
                    String attrName = ldapAttr.getName();

                    if (debug.messageEnabled()) {
                        debug.message("  LDAPv3Repo: after read: attrName="
                                + attrName);
                    }

                    if ((predefinedAttr != null)
                            && !predefinedAttr.contains(attrName.toLowerCase()))
                    {
                        continue;
                    }
                    Set attrValueSet = new HashSet();
                    if (isString) {
                        Enumeration enumVals = ldapAttr.getStringValues();
                        while ((enumVals != null) && enumVals.hasMoreElements()
                        ) {
                            String value = (String) enumVals.nextElement();
                            attrValueSet.add(value);
                        }
                        if ((dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD) ||
                            dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3ADAM))
                            && attrName.equalsIgnoreCase(isActiveAttrName)
                            && ((attrNames == null) ||
                            attrNamesCase.contains(defaultStatusAttribute))) {

                            if (!addActiveAttrName) {
                                // we didn't add this. requested by caller.
                                theAttrMap.put(attrName.toLowerCase(),
                                    attrValueSet);
                            }
                            attrValueSet = convertInetStatus(attrValueSet,
                                attrName);
                            theAttrMap.put(defaultStatusAttribute,
                                attrValueSet);
                        } else {
                            theAttrMap.put(attrName.toLowerCase(),
                                attrValueSet);
                        }
                    } else {
                        byte[][] values = ldapAttr.getByteValueArray();

                        if ((dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD) ||
                            dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3ADAM))
                            && attrName.equalsIgnoreCase(isActiveAttrName)
                            && ((attrNames == null) ||
                            attrNamesCase.contains(defaultStatusAttribute))) {

                            if (!addActiveAttrName) {
                                theAttrMap.put(attrName, values);
                            }
                            values = convertInetStatus(ldapAttr);
                            theAttrMap.put(defaultStatusAttribute, values);
                        } else {
                            theAttrMap.put(attrName, values);
                        }

                        if (debug.messageEnabled()) {
                            debug.message("   getAttribute binary: values=" +
                                values);
                        }
                    }
                }
            }

        /*
         * Add this 'dn' explicitly to the result set and return. reason:
         * when queried with this entrydn/dn the lower level api/ ldapjdk
         * does not return this attribute, but returns other ones.
         */
        if (attrNamesCase.contains("dn") && (!theAttrMap.containsKey("dn"))) {
            Set values = new HashSet();
            values.add(dn);
            theAttrMap.put("dn", values);
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAttributes returns theAttrMap = " +
            IdRepoUtils.getAttrMapWithoutPasswordAttrs(theAttrMap, null));
        }
        return (theAttrMap);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public Map getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAttributes 3 called" + ": " + type
                    + ": " + name);
        }

        return (getAttributes(token, type, name, null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getBinaryAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public Map getBinaryAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("getBinaryAttributes: ...");
        }
        return (getAttributes(token, type, name, attrNames, false));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#setBinaryAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map,
     *      boolean)
     */
    public void setBinaryAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) throws IdRepoException, SSOException 
    {

        if (debug.messageEnabled()) {
            debug.message("setBinaryAttributes: type:" + type + "; name="
                + name + "; attributes=" + attributes + "; isAdd:" + isAdd);
        }
        setAttributes(token, type, name, attributes, isAdd, false, false);
    }

    /**
     * Finds the dynamic group member DNs
     * 
     * @param url
     *            the url to be used for the group member search
     * @return the set of group member DNs satisfied the search url
     */
    private Set findDynamicGroupMembersByUrl(LDAPUrl url)
            throws IdRepoException {

        checkConnPool();
        LDAPConnection ld = null;
        int resultCode = 0;
        Set groupMemberDNs = null;
        groupMemberDNs = new HashSet();
        if (debug.messageEnabled()) {
            debug.message("search filter in LDAPGroups : "
                    + url.getFilter());
        }
        LDAPSearchResults res = null;
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            url.getDN(), url.getScope(), url.getFilter(), null, false,
            timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
            defaultMaxResults);
        try {
            ld = connPool.getConnection();
            enableCache(ld);
            res = ld.search(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            String ldeErrMsg = lde.getLDAPErrorMessage();
            debug.error("LDAPv3Repo: findDynamicGroupMembersByUrl. "
                + "ld.search error: " + resultCode);
            if (debug.messageEnabled()) {
                debug.error("LDAPv3Repo: findDynamicGroupMembersByUrl failed",
                   lde);
            }
            handleLDAPException(lde, url.getDN());           
            return groupMemberDNs; 
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        while (res.hasMoreElements()) {
            try {
                LDAPEntry entry = res.next();
                if (entry != null) {
                    groupMemberDNs.add(entry.getDN());
                }
            } catch (LDAPReferralException lre) {
                // ignore referrals
                continue;
            } catch (LDAPException le) {
                String ldapError = Integer.toString(le.getLDAPResultCode());
                Object[] args =
                    { CLASS_NAME, LDAPv3Bundle.getString(ldapError)};
                IdRepoException ide = new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "311", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide;
            }
        }
        return groupMemberDNs;
    }

    private Set getGroupMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {

        checkConnPool();
        // returns all members of the group named name.
        Set resultSet = new HashSet();
        String dn = null;
        try {
            dn = getDN(type, name);
        } catch (IdRepoUnsupportedOpException ide) {
            return null;
        } catch (IdRepoException idrepoerr) {
            return null;
        }
        LDAPEntry groupEntry = null;
        LDAPConnection ld = null;
        int resultCode = 0;
        try {
            LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(dn,
                 timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
                 defaultMaxResults);
            ld = connPool.getConnection();
            enableCache(ld);
            groupEntry = ld.read(request);
        } catch (LDAPException e) {
            resultCode = e.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            debug.error("LDAPGroups: invalid group name " + name);
            if (debug.messageEnabled()) {
                debug.message("LDAPGroups: invalid group name " + name, e);
            }
            String ldapError = Integer.toString(resultCode);
            Object[] args = { CLASS_NAME, LDAPv3Bundle.getString(ldapError)};
            if ((resultCode == 80) || (resultCode == 81) ||
                (resultCode == 82)) {
                IdRepoFatalException ide = new IdRepoFatalException(
                        IdRepoBundle.BUNDLE_NAME, "311", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide;
            }
            return null;
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        LDAPAttribute attribute = groupEntry.getAttribute(uniqueMemberAttr);
        if (attribute != null) {
            Enumeration enumVals = attribute.getStringValues();
            while ((enumVals != null) && enumVals.hasMoreElements()) {
                String memberDNStr = (String) enumVals.nextElement();
                resultSet.add(memberDNStr);
            }
        } else if (memberURLAttr != null) { // see if this is a dynamic group.
            attribute = groupEntry.getAttribute(memberURLAttr);
            if (attribute != null) {
                Enumeration enumVals = attribute.getStringValues();
                while ((enumVals != null) && enumVals.hasMoreElements()) {
                    String memberUrl = (String) enumVals.nextElement();
                    try {
                        LDAPUrl ldapUrl = new LDAPUrl(
                                URLEncDec.encodeLDAPUrl(memberUrl));
                        Set dynMembers = findDynamicGroupMembersByUrl(ldapUrl);
                        resultSet.addAll(dynMembers);
                    } catch (java.net.MalformedURLException e) {
                        throw (new IdRepoException("MalformedURLException"));
                    }
                }
            }
        }
        return resultSet;
    }

    private Set getManagedRoleMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {

        checkConnPool();
        LDAPConnection ld = null;
        int resultCode = 0;
        Set roleMemberDNs = null;
        roleMemberDNs = new HashSet();
        LDAPSearchResults res = null;
        String filter = "(" + nsRoleDNAttr + "=" + getDN(type, name) + ")";
        if (debug.messageEnabled()) {
            debug.message("search filter in getManagedRoleMembers: "
                + filter + "; roleSearchScope =" + roleSearchScope
                + ";  orgDN=" + orgDN);
        }
        // all entries which has nsRoleDN=managedRoleName
        String baseDN = getBaseDN(membersType);
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            baseDN, roleSearchScope, filter, null, false, timeLimit,
            LDAPRequestParser.DEFAULT_DEREFERENCE, defaultMaxResults);
        try {
            ld = connPool.getConnection();
            enableCache(ld);
            res = ld.search(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            debug.error("LDAPv3Repo: getManagedRoleMembers, ld.search error"
                + resultCode);
            if (debug.messageEnabled()) {
                debug.error(
                    "LDAPv3Repo: getManagedRoleMembers, ld.search error", lde);
            }
            handleLDAPException(lde, getDN(type, name));       
            return roleMemberDNs; 
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        while (res.hasMoreElements()) {
            try {
                LDAPEntry entry = res.next();
                if (entry != null) {
                    roleMemberDNs.add(entry.getDN());
                }
            } catch (LDAPReferralException lre) {
                // ignore referrals
                continue;
            } catch (LDAPException le) {
                resultCode = le.getLDAPResultCode();
                // If time or size limit has reached, return the results
                if (resultCode == LDAPException.TIME_LIMIT_EXCEEDED ||
                    resultCode == LDAPException.LDAP_TIMEOUT ||
                    resultCode == LDAPException.SIZE_LIMIT_EXCEEDED) {
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Plugin: getManagedRoleMembers"
                           + "search iteration size/time limit reached: "
                           + le.getMessage()
                           + "  roleMembersDNs=" + roleMemberDNs);
                    }
                    return (roleMemberDNs);
                }
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Plugin: getManagedRoleMembers " +
                        "search iteration exception", le);
                }
                handleLDAPException(le, name);
            }
        }
        if (debug.messageEnabled()) {
            debug.message(
                " exit getManagedRoleMembers. roleMembersDNs=" + roleMemberDNs);
        }
        if (cacheEnabled && roleMemberDNs.isEmpty()) {
            // to work around a bug in ldapcache of not being able
            // to flush the search if the search of role return empty.
            clearCache();
        }

        return roleMemberDNs;
    }

    private Set getFilteredRoleMembers(SSOToken token, IdType type,
            String name, IdType membersType) throws IdRepoException,
            SSOException {

        checkConnPool();
        Set roleMemberDNs = null;
        LDAPConnection ld = null;
        int resultCode = 0;
        String dn = null;
        String getAttrs[] = { nsRoleFilterAttr };
        roleMemberDNs = new HashSet();
        dn = getDN(type, name);
        if (debug.messageEnabled()) {
            debug.message("getFilteredRoleMembers: name="
                + name + "; type=" + type
                + "; membersType=" + membersType );
        }
        LDAPEntry foundEntry = null;
        LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(dn,
            getAttrs, timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
            defaultMaxResults);
        try {
            ld = connPool.getConnection();
            enableCache(ld);
            foundEntry = ld.read(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            debug.error("LDAPv3Repo: getFilteredRoleMembers, ld.read"
                + resultCode);
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: getFilteredRoleMembers,"
                     + " ld.read", lde);
            }
            handleLDAPException(lde, dn);              
            return roleMemberDNs; 
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        LDAPAttribute ldapAttr = foundEntry.getAttribute(nsRoleFilterAttr);
        if (ldapAttr != null) {
            Enumeration enumVals = ldapAttr.getStringValues();
            while ((enumVals != null) && enumVals.hasMoreElements()) {
                String roleFilter = (String) enumVals.nextElement();
                if (debug.messageEnabled()) {
                    debug.message(
                        "    inside while. roleFilter=" + roleFilter);
                }
                // Use search scope as SCOPE_SUB to get members from
                // filtered role.
                LDAPSearchResults res = null;
                request =
                    LDAPRequestParser.parseSearchRequest(orgDN, LDAPv2.SCOPE_SUB,
                    roleFilter, null, false, timeLimit,
                    LDAPRequestParser.DEFAULT_DEREFERENCE,
                    defaultMaxResults);
                try {
                    ld = connPool.getConnection();
                    enableCache(ld);
                    res = ld.search(request);
                } catch (LDAPException lde) {
                    resultCode = lde.getLDAPResultCode();
                    connPool.close(ld, resultCode);
                    debug.error("LDAPv3Repo: getFilteredRoleMembers, ld.read"
                        + resultCode);
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: getFilteredRoleMembers,"
                            + " ld.read", lde);
                    }
                    handleLDAPException(lde, dn);               
                    return roleMemberDNs;
                } finally {
                    if (ld != null) {
                        connPool.close(ld);
                    }
                }
                while (res.hasMoreElements()) {
                    try {
                        LDAPEntry entry = res.next();
                        if (entry != null) {
                            roleMemberDNs.add(entry.getDN());
                        }
                    } catch (LDAPReferralException lre) {
                        // ignore referrals
                    } catch (LDAPException le) {
                        resultCode = le.getLDAPResultCode();
                        /*
                         * If time or size limit has reached, return
                         * the results.
                         */
                        if (
                            (resultCode == LDAPException.TIME_LIMIT_EXCEEDED) ||
                            (resultCode == LDAPException.LDAP_TIMEOUT) ||
                            (resultCode == LDAPException.SIZE_LIMIT_EXCEEDED)
                            ) {
                            if (debug.messageEnabled()) {
                                debug.message("LDAPv3Plugin: " +
                                    "getFilteredRoleMembers search " +
                                    "iteration size/time limit reached: " +
                                    le.getMessage() +
                                    " ; roleMemberDNs=" + roleMemberDNs);
                            }
                            return (roleMemberDNs);
                        }
                        if (debug.messageEnabled()) {
                            debug.message(
                                "LDAPv3Repo: getFilteredRoleMembers " +
                                "iteration exception", le);
                        }
                        handleLDAPException(le, dn);
                    }
                } // inner while
            } // outer while
        }
        if (debug.messageEnabled()) {
            debug.message("exit getFilteredRoleMembers roleMemberDNs=" +
                roleMemberDNs);
        }
        return roleMemberDNs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getMembers(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String,
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getMembers called" + type + ": " + name
                    + ": " + membersType);
        }
        Set results = null;
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            debug.error("LDAPv3Repo: Membership operation is not supported "
                    + " for Users or Agents");
            throw new IdRepoException(IdRepoBundle.getString("203"), "203");
        } else if (type.equals(IdType.GROUP)) {
            if (membersType.equals(IdType.USER)) {
                results = getGroupMembers(token, type, name, membersType);
            } else {
                debug.error(
                    "LDAPv3Repo: Groups do not supported membership for " +
                        membersType.getName());
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        } else if (type.equals(IdType.ROLE)) {
            if (membersType.equals(IdType.USER)) {
                results = getManagedRoleMembers(token, type, name, membersType);
            } else {
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        } else if (type.equals(IdType.FILTEREDROLE)) {
            if (membersType.equals(IdType.USER)) {
                results = getFilteredRoleMembers(
                                token, type, name, membersType);
            } else {
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        if (debug.messageEnabled()) {
            debug.message("exit  getMembers results=" + results);
        }
        return results;
    }

    private Set getGroupMemberFromUser(
        SSOToken token,
        IdType type,
        String name,
        IdType membershipType)
        throws IdRepoException, SSOException {

        checkConnPool();
        LDAPConnection ld = null;
        int resultCode = 0;
        String dn = null;
        Set groupDNs = null;
        String getAttrs[] = { memberOfAttr };
        groupDNs = new HashSet();
        dn = getDN(type, name);

        if (debug.messageEnabled()) {
            debug.message("  getGroupMemberFromUser: dn=" + dn +
                ";  memberOfAttr=" + memberOfAttr);
        }
        LDAPEntry foundEntry = null;
        LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(dn,
            getAttrs, timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
            defaultMaxResults);
        try {
            ld = connPool.getConnection();
            enableCache(ld);
            foundEntry = ld.read(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            debug.error("LDAPv3Repo: getGroupMemberShips. ld.read error: "
                + resultCode);
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: getGroupMemberShips. ld.read error",
                   lde);
            }
            handleLDAPException(lde, dn);
            return groupDNs;
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        LDAPAttribute ldapAttr = foundEntry.getAttribute( memberOfAttr);
        if (ldapAttr != null) {
            Enumeration enumVals = ldapAttr.getStringValues();
            while ((enumVals != null) && enumVals.hasMoreElements()) {
                String groupDN = (String) enumVals.nextElement();
                groupDNs.add(groupDN);
            }
        }
        return  groupDNs;
    }
    
    private String replaceEscapeChars(String dn) {
        StringBuilder result = new StringBuilder(dn.length());
        for (int cursor=0; cursor < dn.length(); cursor++) {
            char nextChar = dn.charAt(cursor);
            switch (nextChar) {
                case '*' :
                    result.append("\\2a");
                    break;
                case '(' :
                    result.append("\\28");
                    break;
                case ')' :
                    result.append("\\29");
                    break;
                case '/' :
                    result.append("\\2f");
                    break;
                case '\\' :
                    //assuming backslash wasn't escaped before this method
                    result.append("\\5c");
                    break;
                case 'N' :
                	String NULSTR = dn.substring(cursor, cursor+3);
                    if (NULSTR.equals("NUL")) {
                        result.append("\\00");
                        cursor = cursor+2;
                        break;
                    }
                default  :
                    result.append(nextChar);
                    break;
            }
        }
    	return result.toString();
    }

    private Set getGroupMemberSearch(
        SSOToken token,
        IdType type,
        String name,
        IdType membershipType)
        throws IdRepoException, SSOException {

        checkConnPool();
        Set groupDNs = null;
        LDAPConnection ld = null;
        int ldapResultCode = 0;
        groupDNs = new HashSet();
        String dn = getDN(type, name);
        
        //replace any special search filter char defined in rfc2254
        dn = replaceEscapeChars(dn);

        String baseDN = getBaseDN(IdType.GROUP);
        String attrs[] = { "dn" };
        int searchGroupScope = searchScope;
        String grpMembershipFilter = "(&" + groupSearchFilter +
            "(" + uniqueMemberAttr + "=" + dn + "))";

        if (debug.messageEnabled()) {
              debug.message("getGroupMemberSearch: dn=" + dn +
                  "; basedn=" + baseDN +
                  "; scope=" + searchGroupScope +
                  "\n  grpMembershipFilter=" + grpMembershipFilter);
        }
        LDAPSearchResults results = null;
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            baseDN, searchGroupScope, grpMembershipFilter, attrs, false,
            timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
            defaultMaxResults);
        try {

            ld = connPool.getConnection();
            enableCache(ld);
            results = ld.search(request);
        } catch (LDAPException e) {
            ldapResultCode = e.getLDAPResultCode();
            connPool.close(ld, ldapResultCode);
            if (debug.messageEnabled()) {
                debug.message("  Search for User error: ", e);
                debug.message("resultCode: " + ldapResultCode);
            }
            String ldapError = Integer.toString(ldapResultCode);
            Object[] args = { CLASS_NAME, LDAPv3Bundle.getString(ldapError)};
            IdRepoException ide = new IdRepoException(
                IdRepoBundle.BUNDLE_NAME, "311", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        try {
            LDAPEntry entry = null;
            while (results.hasMoreElements()) {
                try {
                    entry = results.next();
                    String groupdn = entry.getDN();
                    groupDNs.add(groupdn);
                    if (debug.messageEnabled()) {
                        debug.message("getGroupMemberSearch: groupdn=" +
                            groupdn + "; entry=" + entry);
                    }
                } catch (LDAPReferralException refe) {
                    debug.message("LDAPReferral Detected.");
                    continue;
                }
            }
        } catch (LDAPException e) {
            ldapResultCode = e.getLDAPResultCode();
            if (debug.messageEnabled()) {
                debug.message("  Search for User error: ", e);
                debug.message("resultCode: " + ldapResultCode);
            }
            String ldapError = Integer.toString(ldapResultCode);
            Object[] args = { CLASS_NAME, LDAPv3Bundle.getString(ldapError)};
            IdRepoException ide = new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "311", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;
        }
        return  groupDNs;
    }

    private Set getGroupMemberShips(
        SSOToken token,
        IdType type,
        String name,
        IdType membershipType)
        throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo. getGroupMemberShips: type=" +
                type + ";  name=" + name +
                ";  membershipType=" + membershipType);
        }
        Set groupDNs = (memberOfAttr == null) ?
                getGroupMemberSearch(token, type, name, membershipType) :
                getGroupMemberFromUser(token, type, name, membershipType);
        return  groupDNs;
    }

    private Set getManagedRoleMemberShips(SSOToken token, IdType type,
            String name, IdType membershipType) throws IdRepoException,
            SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getManagedRoleMemberShips." +
                    " type=" + type + " name=" + name +
                    " membershipType=" + membershipType);
        }

        checkConnPool();
        Set roleDNs = null;
        LDAPConnection ld = null;
        int resultCode = 0;
        String dn = null;
        String getAttrs[] = { nsRoleDNAttr };
        roleDNs = new HashSet();
        dn = getDN(type, name);
        LDAPEntry foundEntry = null;
        LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(dn,
            getAttrs, timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
            defaultMaxResults);
        try {
            ld = connPool.getConnection();
            enableCache(ld);
            foundEntry = ld.read(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            debug.error("LDAPv3Repo: getManagedRoleMemberShips. ld.read error"
                + resultCode + ";  dn=" + dn);
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: getManagedRoleMemberShips. " +
                    "ld.read error dn=" + dn, lde);
            }
            handleLDAPException(lde, dn);
            return roleDNs;
        } finally {
            if (ld != null) {
                connPool.close(ld, resultCode);
            }
        }
        if (foundEntry != null) {
            LDAPAttribute ldapAttr = foundEntry.getAttribute(nsRoleDNAttr);
            if (ldapAttr != null) {
                Enumeration enumVals = ldapAttr.getStringValues();
                while ((enumVals != null) && enumVals.hasMoreElements()) {
                    String roleDN = (String) enumVals.nextElement();
                    roleDNs.add(roleDN);
                }
            }
        }
        return roleDNs;
    }

    private Set getFilteredRoleMemberShips(SSOToken token, IdType type,
            String name, IdType membershipType) throws IdRepoException,
            SSOException {

        checkConnPool();
        Set allRoleDNs = null;
        LDAPConnection ld = null;
        int resultCode = 0;
        String dn = null;
        String getAttrs[] = { nsRoleAttr };
        allRoleDNs = new HashSet();
        dn = getDN(type, name);
        // nsRole returns both managedRole and filteredRole.
        // there is no way to just get the filtererRole.
        // so get all the roles(managedRole and filteredRole) then
        // remove managedRole from all the roles to get the filteredRole.
        LDAPEntry foundEntry = null;
        LDAPSearchRequest request = LDAPRequestParser.parseReadRequest(dn,
            getAttrs, timeLimit, LDAPRequestParser.DEFAULT_DEREFERENCE,
            defaultMaxResults);
        try {
            ld = connPool.getConnection();
            enableCache(ld);
            foundEntry = ld.read(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: getFilteredRoleMemberShips: " +
                    "ld.read: error", lde);
            }
            handleLDAPException(lde, dn);
            return allRoleDNs;
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        LDAPAttribute ldapAttr = foundEntry.getAttribute(nsRoleAttr);
        if (ldapAttr != null) {
            Enumeration enumVals = ldapAttr.getStringValues();
            while ((enumVals != null) && enumVals.hasMoreElements()) {
                String roleDN = (String) enumVals.nextElement();
                allRoleDNs.add(roleDN);
            }
        }
        Set managedRoleDNs = getManagedRoleMemberShips(token, type, name,
            membershipType);
        if (debug.messageEnabled()) {
            debug.message("    managedRoleDNs=" + managedRoleDNs);
            debug.message("    allRoleDNs=" + allRoleDNs);
        }
        // need to convert to lowercaes before comparision because
        // ds is case insensitive and will sometime return all lowercase
        // and sometime return mix case for filtered and static role.
        Set normManagedRoleDNs = new HashSet();
        Iterator iter = managedRoleDNs.iterator();
        while(iter.hasNext()) {
            normManagedRoleDNs.add((
                new DN((String)iter.next())).toRFCString().toLowerCase());
        }
        Set result = new HashSet();
        iter = allRoleDNs.iterator();
        while (iter.hasNext()) {
            String nsroleName = (String)iter.next();
            if (!normManagedRoleDNs.contains(
                new DN(nsroleName).toRFCString().toLowerCase())) {
                result.add(nsroleName);
            }
        }
        allRoleDNs = result;
        return allRoleDNs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getMemberships(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String,
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getMemberships called" + type + ": "
                    + name + ": " + membershipType);
        }

        Set result = null;

        if (!type.equals(IdType.USER) && !type.equals(IdType.AGENT)) {
            debug.error("LDAPv3Repo: Membership for identities other than "
                    + " Users is not allowed ");
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);
        } else {
            if (membershipType.equals(IdType.GROUP)) {
                result = getGroupMemberShips(token, type, name, membershipType);
            } else if (membershipType.equals(IdType.ROLE)) {
                result = getManagedRoleMemberShips(token, type, name,
                        membershipType);
            } else if (membershipType.equals(IdType.FILTEREDROLE)) {
                result = getFilteredRoleMemberShips(token, type, name,
                        membershipType);
            } else { // Memberships of any other types not supported for
                debug.error("LDAPv3Repo: Membership for other types of "
                        + "entities not supported for Users");
                Object args[] = { CLASS_NAME, type.getName(),
                        membershipType.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        }
        return result;
    }

    private void modifyGroupMembership(SSOToken token, IdType type,
            String name, Set usersSet, IdType membersType, int operation)
            throws IdRepoException, SSOException {
        checkConnPool();
        String groupDN = getDN(type, name);
        LDAPConnection ld = null;
        int resultCode = 0;
        Iterator it = usersSet.iterator();
        while (it.hasNext()) {
            String userDN = (String) it.next();
            LDAPAttribute mbr1 = new LDAPAttribute(uniqueMemberAttr,
                userDN);
            LDAPAttribute mbrOf = null;
            if (memberOfAttr != null) {
                mbrOf = new LDAPAttribute(memberOfAttr, groupDN);
            }

            LDAPModification mod = null;
            LDAPModification modMemberOf = null;
            switch (operation) {
                case ADDMEMBER:
                    mod = new LDAPModification(
                        LDAPModification.ADD, mbr1);
                    if (mbrOf != null) {
                        modMemberOf = new LDAPModification(
                            LDAPModification.ADD, mbrOf);
                    }
                    break;
                case REMOVEMEMBER:
                    mod = new LDAPModification(
                        LDAPModification.DELETE, mbr1);
                    if (mbrOf != null) {
                        modMemberOf = new LDAPModification(
                            LDAPModification.DELETE, mbrOf);
                    }
            }
            if (cacheEnabled) {
                ldapCache.flushEntries(userDN, LDAPv2.SCOPE_BASE);
                ldapCache.flushEntries(groupDN, LDAPv2.SCOPE_BASE);
            }
            if ( !isExists(IdType.GROUP, groupDN) ) {
                String ldapError = Integer.toString(
                    LDAPException.NO_SUCH_OBJECT);
                Object[] args = { CLASS_NAME, groupDN, ""};
                IdRepoException ide = new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "220", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide;
            }
            LDAPModifyRequest request =
                LDAPRequestParser.parseModifyRequest(groupDN,  mod);
            try {
                ld = connPool.getConnection();
                enableCache(ld);
                ld.modify(request);
            } catch (LDAPException lde) {
                resultCode = lde.getLDAPResultCode();
                connPool.close(ld, resultCode);
                ld = null;
                debug.error("LDAPv3Repo: modifyGroupMembership ld.modify: "
                    + resultCode + " groupDN = " + groupDN
                    + " userDN= " + userDN );
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: modifyGroupMembership " +
                        "ld.modify", lde);
                }
                handleLDAPException(lde, groupDN);                
            } finally {
                if (ld != null) {
                    connPool.close(ld);
                }
            }
            if ( !isExists(IdType.USER, userDN) ) {
                String ldapError = Integer.toString(
                    LDAPException.NO_SUCH_OBJECT);
                Object[] args = { CLASS_NAME, userDN, ""};
                IdRepoException ide = new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "220", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide; 
            }
            if (mbrOf != null) {
                request =
                    LDAPRequestParser.parseModifyRequest(userDN,
                    modMemberOf);
                try {
                    ld = connPool.getConnection();
                    enableCache(ld);                        
                    ld.modify(request);
                } catch (LDAPException lde) {
                    resultCode = lde.getLDAPResultCode();
                    connPool.close(ld, resultCode);
                    ld = null;
                    debug.error("LDAPv3Repo: modifyGroupMembership ld.modify: "
                        + resultCode + " groupDN = " + groupDN
                        + " userDN= " + userDN );
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: modifyGroupMembership " +
                            "ld.modify", lde);
                    }
                    handleLDAPException(lde, groupDN);
                } finally {
                    if (ld != null) {
                        connPool.close(ld);
                    }
                }
            }
        }
    }

    private void modifyRoleMembership(SSOToken token, IdType type, String name,
            Set usersSet, IdType membersType, int operation)
            throws IdRepoException, SSOException {

        checkConnPool();
        // to add just put nsRoleDN into the user entry.
        // there is nothing we can for filtered role since membership
        // is controlled by a filtered.
        String roleDN = getDN(type, name);
        LDAPConnection ld = null;
        int resultCode = 0;
        Iterator it = usersSet.iterator();
        while (it.hasNext()) {
            LDAPModification mod = null;
            String userDN = (String) it.next();
            LDAPAttribute mbr1 = new LDAPAttribute(nsRoleDNAttr, roleDN);
            switch (operation) {
                case ADDMEMBER:
                    mod = new LDAPModification(LDAPModification.ADD, mbr1);
                    break;
                case REMOVEMEMBER:
                    mod = new LDAPModification(LDAPModification.DELETE,
                        mbr1);
            }
            LDAPModifyRequest request =
                LDAPRequestParser.parseModifyRequest(userDN, mod);
            try {
                ld = connPool.getConnection();
                enableCache(ld);
                ld.modify(request);
            } catch (LDAPException lde) {
                resultCode = lde.getLDAPResultCode();
                connPool.close(ld, resultCode);
                ld = null;
                debug.error("LDAPv3Repo: modifyRoleMembership ld.modify: "
                    + resultCode + " userDN= " + userDN + " roleDN= " +
                    roleDN);
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: modifyRoleMembership " +
                        "ld.modify: ", lde);
                }
                handleLDAPException(lde, userDN);                    
            } finally {
                if (ld != null) {
                    connPool.close(ld);
                }
            }
            if (cacheEnabled) {
                ldapCache.flushEntries(userDN, LDAPv2.SCOPE_BASE);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#modifyMemberShip(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set,
     *      com.iplanet.am.sdk.IdType, int)
     */
    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: modifyMemberShip called " + type
                    + "; name= " + name + "; members= " + members
                    + "; membersType= " + membersType + "; operation= "
                    + operation);
        }
        if (members == null || members.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.modifyMemberShip: Members set " +
                        "is empty");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.modifyMembership: Memberhsip " +
                        "to users and agents is not supported");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "203", null);
        }
        if (!membersType.equals(IdType.USER)) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.modifyMembership: A non-user " +
                        "type cannot  be made a member of any identity"
                                + membersType.getName());
            }
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);
        }

        checkConnPool();
        Set usersSet = new HashSet();
        Iterator it = members.iterator();
        while (it.hasNext()) {
            String curr = (String) it.next();
            String dn = getDN(membersType, curr);
            usersSet.add(dn);
        }

        if (type.equals(IdType.GROUP)) {
            modifyGroupMembership(token, type, name, usersSet, membersType,
                    operation);
        } else if (type.equals(IdType.ROLE)) {
            modifyRoleMembership(token, type, name, usersSet, membersType,
                    operation);
        } else {
            debug.error("LDAPv3Repo.modifyMembership: Memberships cannot be"
                    + "modified for type= " + type.getName());
            Object[] args = { CLASS_NAME, type.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "209", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#removeAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set)
     */
    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: removeAttributes called " + type + ": "
                    + name + attrNames);
        }
        if (attrNames == null || attrNames.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        Set predefinedAttr = null;
        if (type.equals(IdType.USER)) {
            predefinedAttr = userAtttributesAllowed;
        } else if (type.equals(IdType.AGENT)) {
            predefinedAttr = agentAtttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            predefinedAttr = groupAtttributesAllowed;
        } else if (type.equals(IdType.ROLE)) {
            predefinedAttr = roleAtttributesAllowed;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            predefinedAttr = filteredroleAtttributesAllowed;
        }

        String eDN = getDN(type, name);
        if (attrNames != null && attrNames.isEmpty()) {
            LDAPModificationSet ldapModSet = new LDAPModificationSet();
            Iterator itr = attrNames.iterator();
            while (itr.hasNext()) {
                String attrName = (String) (itr.next());
                if ((predefinedAttr != null)
                        && (!predefinedAttr.contains(attrName))) {
                    continue;
                }
                LDAPAttribute theAttr = new LDAPAttribute(attrName);
                ldapModSet.add(LDAPModification.REPLACE, theAttr);
            } // while
            LDAPConnection ld = null;
            int resultCode = 0;
            LDAPModifyRequest request =
                LDAPRequestParser.parseModifyRequest(eDN, ldapModSet);
            try {
                ld = connPool.getConnection();
                enableCache(ld);
                ld.modify(request);
            } catch (LDAPException lde) {
                resultCode = lde.getLDAPResultCode();
                connPool.close(ld, resultCode);
                ld = null;
                debug.error("LDAPv3Repo: setAttributes, ld.modify error: " +
                    resultCode);
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: setAttributes, ld.modify error",
                        lde);
                }
                handleLDAPException(lde, eDN); 
            } finally {
                if (ld != null) {
                    connPool.close(ld);
                }
            }
            if (cacheEnabled) {
                ldapCache.flushEntries(eDN, LDAPv2.SCOPE_BASE);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, int, int,
     *      java.util.Set, boolean, int, java.util.Map)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, int maxTime, int maxResults, Set returnAttrs,
            boolean returnAllAttrs, int filterOp, Map avPairs, 
            boolean recursive)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: new search called:" + "type:" + type
                    + " ;pattern:" + pattern + " ;avPairs: " + avPairs);
            debug.message("  cont LDAPv3Repo: search: " + "maxTime:" + maxTime
                    + " ;maxResults:" + maxResults + " ;returnAttrs: "
                    + returnAttrs);
            debug.message("  cont LDAPv3Repo: search:" + "returnAllAttrs:"
                    + returnAllAttrs + " ;filterOp:" + filterOp
                    + " ;recursive:" + recursive + " ;returnAttrs: "
                    + returnAttrs);
        }

        checkConnPool();
        // String base = orgDN;
        String base = getBaseDN(type);
        int scope = LDAPv2.SCOPE_SUB;
        if (!recursive) {
            scope = LDAPv2.SCOPE_ONE;
        }
        boolean attrsOnly = false;
        LDAPConnection ld = null;
        int ldapErrCode = 0;
        Map allEntryMap = null;
        Set allEntries = null;
        int errorCode;
        LDAPSearchConstraints searchConstraints = new
            LDAPSearchConstraints();
        if (maxResults < 1) {
            searchConstraints.setMaxResults(defaultMaxResults);
        } else {
            searchConstraints.setMaxResults(maxResults);
        }

        if (maxTime < 1) {
            searchConstraints.setServerTimeLimit(timeLimit);
        } else {
            searchConstraints.setServerTimeLimit(maxTime * 1000);
        }

        String namingAttr = getNamingAttr(type);
        String[] theAttr = null;
        if ((returnAllAttrs) ||
            (returnAttrs != null && returnAttrs.contains("*"))) {
            theAttr = new String[] { "*" };
        } else if (returnAttrs != null && !returnAttrs.isEmpty()) {
            returnAttrs.add(namingAttr);
            theAttr = (String[]) returnAttrs.toArray(new String[returnAttrs
                .size()]);
        } else { // don't return any attr it will be faster.
            // Need to get back the naming attribute
            theAttr = new String[] { namingAttr };
        }

        LDAPSearchResults myResults = null;
        String objectClassFilter = getObjClassFilter(type);

        StringBuffer filterSB = new StringBuffer();

        filterSB.append("(&").append(
                constructFilter(
                    namingAttr, objectClassFilter, pattern)); // note A

        if ((avPairs != null) && (avPairs.size() > 0)) {
            filterSB.append(constructFilter(filterOp, avPairs));
        }

        filterSB.append(")"); // matches "(" in note A above

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: before ld.search call:" + "filterSB:"
                + filterSB + " ; base:" + base);
            if (theAttr != null) {
                debug.message("          theAttr[0]: " + theAttr[0]);
            } else {
                debug.message("          theAttr[0]:=null");
            }
        }
        LDAPSearchRequest request =
            LDAPRequestParser.parseSearchRequest(base, searchScope,
            filterSB.toString(), theAttr, attrsOnly,
            searchConstraints);
        try {
            ld = connPool.getConnection();
            enableCache(ld);
            myResults = ld.search(request, searchConstraints);
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            if (debug.messageEnabled()) {
                debug.message(
                    "LDAPv3Repo: search, ld.search error: " + resultCode);
            }
            String ldapError = Integer.toString(resultCode);
            Object[] args = { CLASS_NAME, LDAPv3Bundle.getString(
                ldapError)};
            if ((resultCode == 80) || (resultCode == 81) ||
                (resultCode == 82)){
                IdRepoFatalException ide = new IdRepoFatalException(
                    IdRepoBundle.BUNDLE_NAME, "311", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide;
            } else if (resultCode == 32) {
                // return empty set for entry not found error.
                return new RepoSearchResults(new HashSet(),
                    RepoSearchResults.SUCCESS, Collections.EMPTY_MAP, type);
            } else {
                IdRepoException ide = new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "311", args);
                ide.setLDAPErrorCode(ldapError);
                throw ide;
            }
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }

        errorCode = RepoSearchResults.SUCCESS;
        allEntryMap = new HashMap();
        allEntries = new HashSet();
        try {
            while (myResults.hasMoreElements()) {
                LDAPEntry entry = myResults.next();
                String entryDN = entry.getDN();
                Map attrEntryMap = new HashMap();
                if (debug.messageEnabled()) {
                    debug.message("    in search: entryDN=" + entryDN
                        + "; returnAllAttrs=" + returnAllAttrs
                        + "; allEntries=" + allEntries
                        + "; allEntryMap=" + allEntryMap);
                }

                if (returnAllAttrs) {
                    // return all the attributes
                    LDAPAttributeSet ldapAttrSet = entry.getAttributeSet();
                    int size = ldapAttrSet.size();
                    for (int i = 0; i < size; i++) {
                        LDAPAttribute ldapAttr = ldapAttrSet.elementAt(i);
                        if (ldapAttr != null) {
                            String attrName = ldapAttr.getName();
                            Set attrValueSet = new HashSet();
                            Enumeration enumVals =
                                ldapAttr.getStringValues();
                            while ((enumVals != null)
                                && enumVals.hasMoreElements()) {
                                String value = (String)
                                    enumVals.nextElement();
                                attrValueSet.add(value);
                            }
                            attrEntryMap.put(attrName, attrValueSet);
                        }
                    }
                    // Get the naming attribute value
                    Set idNameValue = (Set) attrEntryMap.get(namingAttr);
                    String idName = entryDN;
                    DN edn = new DN(entryDN);
                    if (entryDN != null && edn.isDN() &&
                        entryDN.toLowerCase().startsWith(
                            namingAttr.toLowerCase())) {
                        String[] dns = edn.explodeDN(true);
                        idName = dns[0];
                    } else
                        if (idNameValue != null && !idNameValue.isEmpty()) {
                            idName = (String) idNameValue.iterator().next();
                        }
                    allEntries.add(idName);
                    allEntryMap.put(idName, attrEntryMap);
                    if (debug.messageEnabled()) {
                        debug.message("  search1 idName=" + idName +
                            ";  attrEntryMap=" + attrEntryMap);
                    }
                } else if (returnAttrs != null && !returnAttrs.isEmpty()) {
                    // return the attributes specified by caller.
                    Iterator itr = returnAttrs.iterator();
                    while (itr.hasNext()) {
                        String attrName = (String) itr.next();
                        LDAPAttribute ldapAttr = entry.getAttribute(
                            attrName);
                        // return empty set if attribute does not exist.
                        Set attrValueSet = new HashSet();
                        if (ldapAttr != null) {
                            Enumeration enumVals =
                                ldapAttr.getStringValues();
                            while ((enumVals != null)
                                && enumVals.hasMoreElements()) {
                                String value = (String) 
                                    enumVals.nextElement();
                                attrValueSet.add(value);
                            }
                        }
                        attrEntryMap.put(attrName, attrValueSet);
                    }
                    // Get the naming attribute value
                    Set idNameValue = (Set) attrEntryMap.get(namingAttr);
                    String idName = entryDN;
                    DN edn = new DN(entryDN);
                    if (entryDN != null && edn.isDN() &&
                        entryDN.toLowerCase().startsWith(
                        namingAttr.toLowerCase())) {
                        String[] dns = edn.explodeDN(true);
                        idName = dns[0];
                    } else
                        if (idNameValue != null && !idNameValue.isEmpty()) {    
                            idName = (String) idNameValue.iterator().next();
                        }
                    allEntries.add(idName);
                    allEntryMap.put(idName, attrEntryMap);
                    if (debug.messageEnabled()) {
                        debug.message("  search2 idName=" + idName +
                            ";  attrEntryMap=" + attrEntryMap);
                    }
                } else {
                    /*
                     * returnAllAttrs is false and list of attr to return is
                     * null do not return any attribute.
                     * Get the naming attribute for results
                     * return entry DN if empty
                     */
                    String idName = entryDN;
                    LDAPAttribute ldapAttr = entry.getAttribute(namingAttr);
                    DN edn = new DN(entryDN);
                    if (entryDN != null && edn.isDN() &&
                        entryDN.toLowerCase().startsWith(
                            namingAttr.toLowerCase())) {
                        String[] dns = edn.explodeDN(true);
                        idName = dns[0];
                    } else if (ldapAttr != null ) {
                        Enumeration enumVals = ldapAttr.getStringValues();
                        if ((enumVals != null) &&
                            enumVals.hasMoreElements()) {
                            idName = (String) enumVals.nextElement();
                        }
                    }
                    allEntries.add(idName);
                    if (debug.messageEnabled()) {
                        debug.message("  search3 idName=" + idName +
                            ";  allEntries=" + allEntries);
                    }
                }
            } // while
        } catch (LDAPException e) {
            ldapErrCode = e.getLDAPResultCode();
            switch (errorCode) {
                case LDAPException.TIME_LIMIT_EXCEEDED:
                case LDAPException.LDAP_TIMEOUT:
                {
                    errorCode = RepoSearchResults.TIME_LIMIT_EXCEEDED;
                    break;
                }
                case LDAPException.SIZE_LIMIT_EXCEEDED: {
                    errorCode = RepoSearchResults.SIZE_LIMIT_EXCEEDED;
                    break;
                }
                default:
                    errorCode = ldapErrCode;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit search " + "allEntryDN:"
                    + allEntries + " ;allEntries:" + allEntryMap);
        }

        return new RepoSearchResults(allEntries, errorCode, allEntryMap, type);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map, boolean,
     *      int, int, java.util.Set)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, Map avPairs, boolean recursive, int maxResults,
            int maxTime, Set returnAttrs)
    throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: old search called" + type + ": "
                    + pattern + ": " + avPairs);
        }
        return search(token, type, pattern, maxTime, maxResults, returnAttrs,
                true, IdRepo.NO_MOD, avPairs, recursive);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map, boolean)
     */
    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd)
    throws IdRepoException, SSOException {
        setAttributes(token, type, name, attributes, isAdd, true, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map, boolean)
     */
    private void setAttributes(SSOToken token, IdType type, String name,
        Map attributes, boolean isAdd, boolean isString, 
        boolean dontChangeOCs)
        throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: setAttributes called: " + type + ": "
                + name + " attributes = " +
                IdRepoUtils.getAttrMapWithoutPasswordAttrs(attributes, null));
        }
        if (attributes == null || attributes.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: setAttributes. Attributes " +
                        "are empty");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }
        checkConnPool();
        String eDN = getDN(type, name);

        Set predefinedAttr = null;
        if (type.equals(IdType.USER)) {
            predefinedAttr = userAtttributesAllowed;
        } else if (type.equals(IdType.AGENT)) {
            predefinedAttr = agentAtttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            predefinedAttr = groupAtttributesAllowed;
        } else if (type.equals(IdType.ROLE)) {
            predefinedAttr = roleAtttributesAllowed;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            predefinedAttr = filteredroleAtttributesAllowed;
        }

        Map attributesCase = new CaseInsensitiveHashMap(attributes);
        byte[] encodedPwd = doPasswordEncode(type, name, attributesCase,
            unicodePwd);
        appendInetUser(token, type, name, attributesCase, isString);

        LDAPModificationSet ldapModSet = new LDAPModificationSet();
        if (encodedPwd != null) {
            LDAPAttribute theAttr = new LDAPAttribute(unicodePwd);
            theAttr.addValue(encodedPwd);

            if (isAdd) {
                ldapModSet.add(LDAPModification.ADD, theAttr);
            } else {
                ldapModSet.add(LDAPModification.REPLACE, theAttr);
            }
            
        }

        Iterator itr = attributesCase.keySet().iterator();
        while (itr.hasNext()) {
            LDAPAttribute theAttr = null;
            String attrName = (String) itr.next();

            if ((predefinedAttr != null) 
                    && (!predefinedAttr.contains(attrName))) {
                if (debug.messageEnabled()) {
                    debug.message(
                        "    setAttributes: not in predefinedAttr list"
                        + " predefinedAttr=" + predefinedAttr
                        + " attrName=" + attrName);
                }
                continue;
            }

            if (isString) {
                Set set = (Set)(attributesCase.get(attrName));
                String attrValues[] = (set == null ? null : (String[]) set
                        .toArray(new String[set.size()]));
                if (set == null || set.isEmpty()) {
                    // delete the attribute from entry by setting value to
                    // empty.
                    theAttr = new LDAPAttribute(attrName);
                    ldapModSet.add(LDAPModification.REPLACE, theAttr);
                } else {
                    theAttr = new LDAPAttribute(attrName, attrValues);
                    if (isAdd) {
                        ldapModSet.add(LDAPModification.ADD, theAttr);
                    } else {
                        ldapModSet.add(LDAPModification.REPLACE, theAttr);
                    }
                }
            } else {
                byte[][] attrBytes = (byte[][]) (attributesCase.get(attrName));
                theAttr = new LDAPAttribute(attrName);
                int size = attrBytes.length;
                for (int i = 0; i < size; i++) {
                    if (debug.messageEnabled()) {
                        debug.message("setAttributes binary:" + attrBytes[i]);
                    }
                    theAttr.addValue(attrBytes[i]);
                }
                if (isAdd) {
                    ldapModSet.add(LDAPModification.ADD, theAttr);
                } else {
                    ldapModSet.add(LDAPModification.REPLACE, theAttr);
                }
                if (debug.messageEnabled()) {
                    debug.message("setAttribute binary attrBytes:" + attrBytes);
                }
            }
        } // while
        // Check if LdapModSet is empty
        if (ldapModSet.size() == 0) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: setAttributes. LdapModSet is empty");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        // For user objects, need to check if all objectclasses are present
        // If not, they must be added (for account lockout atleast)
        if (type.equals(IdType.USER) && !dontChangeOCs) {
            Set ocsToBeAdded = new CaseInsensitiveHashSet();
            Set ocAttrName = new HashSet();
            ocAttrName.add(LDAP_OBJECT_CLASS);
            Map attrs = getAttributes(token, type, name, ocAttrName);
            if (attrs != null && !attrs.isEmpty()) {
                Set ocValues = (Set) attrs.values().iterator().next();
                if (ocValues != null && !ocValues.isEmpty()) {
                    for (Iterator items = userObjClassSet.iterator(); items
                            .hasNext();) {
                        String oc = (String) items.next();
                        boolean found = false;
                        // Check if present in ocValues
                        for (Iterator ocs = ocValues.iterator(); ocs.hasNext();)
                        {
                            String occ = (String) ocs.next();
                            if (oc.equalsIgnoreCase(occ)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            ocsToBeAdded.add(oc);
                        }
                    }
                }
            }
            if (!ocsToBeAdded.isEmpty()) {
                // see if user is adding objectclass if so remove the one
                // he is adding from our list.
                Set ocAdded = (Set) attributesCase.get(LDAP_OBJECT_CLASS);
                if (ocAdded != null && !ocAdded.isEmpty()) {
                    for (Iterator items = ocAdded.iterator();
                        items.hasNext();) {
                        String ocname = (String) items.next();
                        ocsToBeAdded.remove(ocname);
                    }
                }
                // For all the OCs in the ocsToBeAdded Set, compare the 
                // attributes with the incoming attributes to be set and
                // then if present, consider the OC to be added. This is 
                // to avoid unnecessary adding of all OCs inspite of any
                // operation.
                Set tmpOCSet = new HashSet(2);
                try {
                    Iterator iterAttr = ocsToBeAdded.iterator();
                    while (iterAttr.hasNext()) {
                        String attrOC = (String) iterAttr.next();
                        Set OCAttrs = new HashSet(getOCAttributes(attrOC));
                        for (Iterator OCitems = OCAttrs.iterator();
                            OCitems.hasNext();) {
                            String ocName = (String) OCitems.next();
                            for (Iterator aKey = attributesCase.keySet().
                                iterator(); aKey.hasNext();) {
                                String aName = (String) aKey.next();
                                if (aName.startsWith(ocName)) {
                                    tmpOCSet.add(attrOC);
                                    break;
                                }
                            }
                        }
                    }
                } catch (LDAPException ldx) {
                    int resCode = ldx.getLDAPResultCode();
                    if (debug.warningEnabled()) {
                        debug.warning("LDAPv3Repo: setAttributes : " +
                            resCode, ldx);
                    }
                }
                ocsToBeAdded = tmpOCSet;
                // Add to ldapModSet
                if (!ocsToBeAdded.isEmpty()) {
                    ldapModSet.add(LDAPModification.ADD, new LDAPAttribute(
                        LDAP_OBJECT_CLASS, (String[]) ocsToBeAdded
                                .toArray(new String[ocsToBeAdded.size()])));
                }
            }
        }
        LDAPConnection ld = null;
        int resultCode = 0;
        LDAPModifyRequest request = LDAPRequestParser.parseModifyRequest(
            eDN, ldapModSet);
        try {
            ld = connPool.getConnection();
            enableCache(ld);
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: setAttributes. Calling ld.modify");
            }
            ld.modify(request);
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            connPool.close(ld, resultCode);
            ld = null;
            if (debug.warningEnabled()) {
                debug.warning("LDAPv3Repo: setAttributes, ld.modify error: " +
                    resultCode, lde);
            }
            handleLDAPException(lde, eDN);
        } finally {
            if (ld != null) {
                connPool.close(ld);
            }
        }
        if (cacheEnabled) {
            ldapCache.flushEntries(eDN, LDAPv2.SCOPE_BASE);
        }
    }

    public void changePassword(SSOToken token, IdType type,
        String name, String attrName, String oldPassword, String newPassword)
        throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo.changePassword: " + type + ": " + name);
        }
        if (!type.equals(IdType.USER)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "229", args);
        }

        LDAPModificationSet ldapModSet = new LDAPModificationSet();
        LDAPAttribute theOldAttr = new LDAPAttribute(attrName);
        LDAPAttribute theAttr = new LDAPAttribute(attrName);
        if (attrName.equals(unicodePwd) &&
            (dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3AD) ||
            dsType.equalsIgnoreCase(LDAPv3Config_LDAPV3ADAM))) {

            byte[] encodedOldPwd = encodeADPwd(oldPassword);
            theOldAttr.addValue(encodedOldPwd);
            byte[] encodedNewPwd = encodeADPwd(newPassword);
            theAttr.addValue(encodedNewPwd);
        } else {
            theOldAttr.addValue(oldPassword);
            theAttr.addValue(newPassword);
        }

        String eDN = getDN(type, name);
        ldapModSet.add(LDAPModification.DELETE, theOldAttr);
        ldapModSet.add(LDAPModification.ADD, theAttr);

        LDAPConnection ldc = null;
        int resultCode = 0;
        try {
            if (sslMode) {
                ldc = new LDAPConnection(new JSSESocketFactory(null));
            } else {
                ldc = new LDAPConnection();
            }
            ldc.connect(ldapServerName, ldapPort, eDN, oldPassword);

            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.changePassword: Calling ld.modify");
            }
            ldc.modify(eDN, ldapModSet);
            if (cacheEnabled) {
                ldapCache.flushEntries(eDN, LDAPv2.SCOPE_BASE);
            }
        } catch (LDAPException lde) {
            resultCode = lde.getLDAPResultCode();
            if (debug.warningEnabled()) {
                debug.warning("LDAPv3Repo.changePassword: ld.modify error: " +
                    resultCode, lde);
            }
            handleLDAPException(lde, eDN);
        } finally {
            if (ldc != null) {
                try {
                    ldc.disconnect();
                } catch (LDAPException lde) {
                }
            }
        }
    }

   /**
    * Called by assignService() and modifyService(). Apart from
    * seting mixed types of attributes (string & binary), it does not
    * modify the objectclasses.
    */
    private void setMixAttributes(SSOToken token, IdType type, String name,
        Map attrMap, boolean isAdd) throws IdRepoException, SSOException{

        // check for binary attributes.
        HashMap binAttrMap = null;
        HashMap strAttrMap = null;
        boolean foundBin = false;
        Iterator itr = attrMap.keySet().iterator();
        while (itr.hasNext()) {
            String tmpAttrName = (String) itr.next();
            if (attrMap.get(tmpAttrName) instanceof byte[][]) {
                if (!foundBin) {
                     // need to seperate into binary and string
                     // attribute map
                     strAttrMap = new HashMap(attrMap);
                     binAttrMap = new HashMap();
                }
                foundBin = true;
                binAttrMap.put(tmpAttrName, attrMap.get(tmpAttrName));
                strAttrMap.remove(tmpAttrName);
            }
        }
        if (foundBin) {
            setAttributes(token, type, name, strAttrMap, false, true, true);
            // Set the binary attributes
            setAttributes(token, type, name, binAttrMap, false, false, true);
        } else {
            setAttributes(token, type, name, attrMap, false, true, true);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#assignService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: assignService called. IdType=" + type
                    + "; name=" + name + "; serviceName=" + serviceName
                    + "; SchemaType=" + sType + "; attrMap=" + attrMap);
        }

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) 
                || type.equals(IdType.FILTEREDROLE)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            Set OCs = (Set) attrMap.get("objectclass");
            Set attrName = new HashSet(1);
            attrName.add("objectclass");
            Map tmpMap = getAttributes(token, type, name, attrName);
            Set oldOCs = (Set) tmpMap.get("objectclass");
            OCs = AMCommonUtils.combineOCs(OCs, oldOCs);
            attrMap.put("objectclass", OCs);
            if (sType.equals(SchemaType.USER)) {
                setMixAttributes(token, type, name, attrMap, false);
            } else if (sType.equals(SchemaType.DYNAMIC)) {
                // setAttributes(token, type, name, attrMap, false);
                return;
            }
        } else if (type.equals(IdType.REALM)) {
            // add the serviceName and attrMap to myServiceMap
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: assignService: before myServiceMap:"
                        + myServiceMap);
            }
            if ((serviceName != null) && (serviceName.length() > 0)
                    && (attrMap != null)) {
                Map myAttrMap = new HashMap(attrMap);
                myServiceMap.put(serviceName, myAttrMap);
            } else {
                debug.message("LDAPv3Repo: assignService: not stored. " +
                        "null or 0");
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: assignService: after myServiceMap:"
                        + myServiceMap);
            }
            if (myListener != null) {
                myListener.setServiceAttributes(serviceName, myServiceMap);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        if (debug.messageEnabled()) {
            debug.message("  exit assignService.  myServiceMap:"
                            + myServiceMap);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#unassignService(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap) throws IdRepoException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: unassignService called. IdType=" + type
                    + "; name=" + name + "; serviceName=" + serviceName
                    + "; attrMap=" + attrMap);
        }
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE))
        {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.REALM)) {
            // remove the serviceName and attrMap from myServiceMap
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: unassignService: before " +
                        "myServiceMap:" + myServiceMap);
            }
            if ((serviceName != null) && (serviceName.length() > 0)) {
                myServiceMap.remove(serviceName);
            } else {
                debug.message("LDAPv3Repo: unassignService: serviceName is " +
                        "null or 0");
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: unassignService: after myServiceMap:"
                                + myServiceMap);
            }
            if (myListener != null) {
                myListener.setServiceAttributes(serviceName, myServiceMap);
            }
        } else if (type.equals(IdType.USER)) {
            // Get the object classes that need to be remove from Service Schema
            Set removeOCs = (Set) attrMap.get("objectclass");
            Set attrNameSet = new HashSet();
            attrNameSet.add("objectclass");
            Map objectClassesMap = getAttributes(
                    token, type, name, attrNameSet);
            Set OCValues = (Set) objectClassesMap.get("objectclass");
            removeOCs = AMCommonUtils.updateAndGetRemovableOCs(OCValues,
                    removeOCs);
            // Get the attributes that need to be removed
            Set removeAttrs = new CaseInsensitiveHashSet();
            Iterator iter1 = removeOCs.iterator();
            while (iter1.hasNext()) {
                String oc = (String) iter1.next();
                Set attrs = null;
                try {
                    attrs = new HashSet(getOCAttributes(oc));
                } catch (LDAPException lde) {
                    int resultCode = lde.getLDAPResultCode();
                    debug.error("LDAPv3Repo: unassignService. " +
                        "get Object Attributes failed: "
                        + resultCode);
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: unassignService.", lde);
                    }
                    handleLDAPException(lde, name);
                }
                Iterator iter2 = attrs.iterator();
                while (iter2.hasNext()) {
                    String attrName = (String) iter2.next();
                    removeAttrs.add(attrName.toLowerCase());
                }
            }

            Map avPair = getAttributes(token, type, name);
            Iterator itr = avPair.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                if (removeAttrs.contains(attrName)) {
                    try {
                        // remove attribute one at a time, so if the first
                        // one fails, it will keep continue to remove
                        // other attributes.
                        Map tmpMap = new AMHashMap();
                        tmpMap.put(attrName, Collections.EMPTY_SET);
                        setAttributes(token, type, name, tmpMap, false);
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message("unassignService failed. error " +
                                    "occurred while removing attribute: "
                                            + attrName);
                        }
                    } // catch
                } else {
                   /*
                    * Basically, when the service is unassigned, it should 
                    * remove the service related attributes first and the OCs.
                    * While getting all objectclasses that is for services and 
                    * the relevant user attributes that are associated with 
                    * the service to remove, somehow getOCAttributes() api 
                    * returns the following eventhough there is no string 
                    * manipulation in that api.
                    * removeAttrs [iplanet-am-user-federation-info-key, 
                    * sunidentityserverdiscoentrie, 
                    * iplanet-am-user-federation-info]
                    * But the attrName is like
                    * attrName sunidentityserverdiscoentries 
                    * So added this startsWith check here.
                    */
                    for (Iterator OCitems = removeAttrs.iterator();
                        OCitems.hasNext();) {
                        String ocName = (String) OCitems.next();
                        if (attrName.startsWith(ocName)) {
                            try {
                                Map tmpMap = new AMHashMap();
                                tmpMap.put(attrName, Collections.EMPTY_SET);
                                setAttributes(token, type, name, tmpMap, 
                                    false);
                            } catch (Exception ex) {
                                if (debug.messageEnabled()) {
                                    debug.message("unassignService failed. "+
                                        "else part: error " +
                                        "occurred while removing attribute: "
                                            + attrName);
                                }
                            } // catch
                        }
                    }
                }
            } // while

            // Now update the object class attribute
            Map tmpMap = new AMHashMap();
            tmpMap.put("objectclass", OCValues);
            setAttributes(token, type, name, tmpMap, false, true, true);
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAssignedServices(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServiceNamesandOCs) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAssignedServices. IdType=" + type
                    + "; Name=" + name + "; mapOfServiceNamesandOCs="
                    + mapOfServiceNamesandOCs);
            debug.message("     getAssignedServices. myServiceMap="
                    + myServiceMap);
        }
        Set resultsSet = new HashSet();

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE))
        {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            Set OCs = readObjectClass(token, type, name);
            OCs = convertToLowerCase(OCs);
            Iterator iter = mapOfServiceNamesandOCs.keySet().iterator();
            while (iter.hasNext()) {
                String sname = (String) iter.next();
                Set ocSet = (Set) mapOfServiceNamesandOCs.get(sname);
                ocSet = convertToLowerCase(ocSet);
                if ((OCs != null) && OCs.containsAll(ocSet)) {
                    resultsSet.add(sname);
                }
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: getAssignedServices returns " +
                        "resultsSet: " + resultsSet);
            }
        } else if (type.equals(IdType.REALM)) {
            resultsSet = myServiceMap.keySet();
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: getAssignedServices: resultsSet: "
                        + resultsSet + "; myServiceMap:" + myServiceMap);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        return resultsSet;
    }

    /* (non-Javadoc)
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     * com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType, java.lang.String,
     * java.lang.String, java.util.Set)
     */
    public Map getServiceAttributes(
        SSOToken token,
        IdType type,
        String name,
        String serviceName,
        Set attrNames
    ) throws IdRepoException,   SSOException {
        return(getServiceAttributes(token, type, name, serviceName, attrNames,
           true));
    }


    /* (non-Javadoc)
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     * com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     * java.lang.String, java.lang.String, java.util.Set)
     */
    public Map getBinaryServiceAttributes(
        SSOToken token,
        IdType type,
        String name,
        String serviceName,
        Set attrNames
    ) throws IdRepoException,   SSOException {
        return(getServiceAttributes(token, type, name, serviceName, attrNames,
            false));
    }


    private Map getServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName,  Set attrNames, boolean isString)
        throws IdRepoException,   SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getServiceAttributes. IdType=" + type
                + "; Name=" + name + "; serviceName=" + serviceName
                + "; attrNames=" + attrNames + "; isString=" + isString);

        }

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE))
        {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            // get the user attributes from ldap.
            Map userAttrs = (isString ?
                getAttributes(token, type, name, attrNames)
                : getBinaryAttributes(token, type, name, attrNames));

            // find the attributes in service map.
            if ((serviceName == null) || (serviceName.length() == 0)) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: getServiceAttribute. userAttrs="
                            + userAttrs);
                }
                return (userAttrs);
            }
            Map srvCfgAttrMap = (Map) myServiceMap.get(serviceName);
            Map mySrvAttrMap = new HashMap();
            if ((srvCfgAttrMap == null) || (srvCfgAttrMap.isEmpty())) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: getServiceAttribute: return " +
                            "userAttrs:" + userAttrs);
                }
                return (userAttrs);
            } else {
                Set attrNamesCase = new CaseInsensitiveHashSet(attrNames);
                // find the attrs requested from the realm service map.
                Iterator itr = srvCfgAttrMap.keySet().iterator();
                while (itr.hasNext()) {
                    String attrName = (String) itr.next();
                    if (attrNamesCase.contains(attrName)) {
                        mySrvAttrMap.put(attrName, srvCfgAttrMap.get(attrName));
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("    mySrvAttrMap=" + mySrvAttrMap);
                    debug.message("    srvCfgAttrMap=" + srvCfgAttrMap);
                    debug.message("    userAttrs=" + userAttrs);
                }
            }

            // merge the attributes found from user and service map.
            Set userAttrsNameSet = new CaseInsensitiveHashSet(
                userAttrs.keySet());
            Iterator itr = mySrvAttrMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                // use values from user first then the default template.
                if (!userAttrsNameSet.contains(attrName)) {
                    // convert to binary if necessary
                    Object tmpAttrSet = mySrvAttrMap.get(attrName);
                    if (tmpAttrSet != null) {
                        if ((!isString) && (tmpAttrSet instanceof Set)) {
                            // convert to binary
                            Iterator keyset = ((Set) tmpAttrSet).iterator();
                            int i =0;
                            byte [][] resultArr =
                                new byte[((Set) tmpAttrSet).size()][];
                            while (keyset.hasNext()) {
                                String thisAttr = (String) keyset.next();
                                resultArr[i] = thisAttr.getBytes();
                                i++;
                            }
                            userAttrs.put(attrName, resultArr);
                        } else {
                            userAttrs.put(attrName, mySrvAttrMap.get(attrName));
                        }
                    }
                }
            }
            if (debug.messageEnabled()) {
                debug.message("    on exit: userAttrs= " + userAttrs);
            }
            return (userAttrs);

        } else if (type.equals(IdType.REALM)) {
            Map srvCfgAttrMap = (Map) myServiceMap.get(serviceName);
            if ((srvCfgAttrMap == null) || srvCfgAttrMap.isEmpty()) {
                debug.message("LDAPv3Repo: getServiceAttributes. REALM " +
                        "returns empty");
                return (new HashMap());
            }
            if ((attrNames == null) || attrNames.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: getServiceAttributes. REALM: "
                            + "attrNames is null or empty. srvCfgAttrMap="
                            + srvCfgAttrMap);
                }
                return (new HashMap(srvCfgAttrMap));
            } else {
                Map resultMap = new HashMap();
                Set srvCfgAttrNameSet = new CaseInsensitiveHashSet(
                    srvCfgAttrMap.keySet());
                Iterator itr = attrNames.iterator();
                while (itr.hasNext()) {
                    String attrName = (String) itr.next();
                    if (srvCfgAttrNameSet.contains(attrName)) {
                        resultMap.put(attrName, srvCfgAttrMap.get(attrName));
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: getServiceAttributes. " +
                            "REALM resultMap=" + resultMap);
                }
                return (resultMap);
            }

        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#modifyService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: modifyService. IdType=" + type
                    + "; Name=" + name + "; serviceName=" + serviceName
                    + "; SchemaType=" + sType + "; attrMap=" + attrMap);
        }

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE))
        {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.REALM)) {
            // modify my map by doing an add and replace of existing value.
            // call listener.
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: modifyService. REALM before. " +
                        "myServiceMap" + myServiceMap);
            }
            Map srvCfgAttrMap = (Map) myServiceMap.get(serviceName);
            if ((srvCfgAttrMap == null) || (srvCfgAttrMap.isEmpty())) {
                myServiceMap.put(serviceName, new HashMap(attrMap));
            } else {
                Set myServiceNameSet =
                    new CaseInsensitiveHashSet(srvCfgAttrMap.keySet());
                Iterator itr = attrMap.keySet().iterator();
                while (itr.hasNext()) {
                    String attrName = (String) itr.next();
                    if (myServiceNameSet.contains(attrName)) {
                        Set attrNamedSet = (Set) attrMap.get(attrName);
                        Set srvCfgAttrNamedSet = (Set) srvCfgAttrMap
                                .get(attrName);
                        srvCfgAttrNamedSet.clear();
                        srvCfgAttrNamedSet.addAll(attrNamedSet);
                        srvCfgAttrMap.put(attrName, srvCfgAttrNamedSet);
                    } else {
                        srvCfgAttrMap.put(attrName, attrMap.get(attrName));
                    }
                }
                myServiceMap.put(serviceName, srvCfgAttrMap);
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: modifyService. REALM after. " +
                        "myServiceMap" + myServiceMap);
            }
            if (myListener != null) {
                myListener.setServiceAttributes(serviceName, myServiceMap);
                debug.message("LDAPv3Repo: modifyService calls " +
                        "setServiceAttributes:" + myServiceMap);
            }
        } else if (type.equals(IdType.USER)) {
            if (sType.equals(SchemaType.DYNAMIC)) {
                Object args[] = { this.getClass().getName(), sType.toString(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "214", args);
            } else {
                setMixAttributes(token, type, name, attrMap, false);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
    }

    
    /**
     * Returns the fully qualified name for the identity. It is expected
     * that the fully qualified name would be unique, hence it is recommended
     * to prefix the name with the data store name or protocol.
     * Used by IdRepo framework to check for equality of two identities
     *
     * @param token administrator SSOToken that can be used by the datastore
     * to determine the fully qualified name
     * @param type type of the identity
     * @param name name of the identity
     *
     * @return fully qualified name for the identity within the
     * data store
     */
    public String getFullyQualifiedName(SSOToken token,
        IdType type, String name) 
        throws IdRepoException, SSOException {
        // given the idtype and the name, we will do search to get its FDN.
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getFullyQualifiedName. IdType=" + type
                + ";  name=" + name);
        }

        if ((name == null) || (name.length() == 0)) {
            Object[] args = { CLASS_NAME, "" };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, 
                "220", args);
        }

        String userDN = searchForName(type, name);
        if (userDN == null || (userDN.length() == 0)) {
            return null;
        } else {
            if (firstHostAndPort.length() == 0) {
                StringTokenizer tk = new StringTokenizer(ldapServerName);
                firstHostAndPort = tk.nextToken();
            }
            return ("ldap://" +  firstHostAndPort + "/" + userDN);
        }
    }


    /* 
     *  search for the "name" in DS and return DN if found 
     *  ruturns empty string otherwise.
     */

    private String searchForName(IdType type, String name)
        throws IdRepoException, SSOException {
        // given the idtype and the name, we will do search to get its FDN.
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo:searchForName. IdType=" + type
                + ";  name=" + name);
        }
        String userDN = "";
        String baseDN = orgDN;
        int searchNameScope = LDAPv2.SCOPE_SUB;
        String searchFilter = "";
        String [] attrs = new String[2];
        attrs[0] = "dn";
        String namingAttr = getNamingAttr(type);
        String objectClassFilter = getObjClassFilter(type);
        searchFilter = constructFilter(namingAttr,objectClassFilter, name);
        attrs[1] = namingAttr;
        int userMatches = 0;

        checkConnPool();
        LDAPConnection ldc = null;
        int ldapResultCode = 0;
        LDAPSearchResults results = null;
        LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
            baseDN, searchNameScope, searchFilter, attrs, false,  timeLimit,
            LDAPRequestParser.DEFAULT_DEREFERENCE, defaultMaxResults);
        try {
            ldc = connPool.getConnection();
            enableCache(ldc);
            if (debug.messageEnabled()) {
                debug.message("Connecting to " + firstHostAndPort + ":" +
                "\nSearching " + baseDN + " for " +
                searchFilter + "\nscope = " + searchNameScope);
            }
            results = ldc.search(request);
        } catch (LDAPException e) {
            ldapResultCode = e.getLDAPResultCode();
            connPool.close(ldc, ldapResultCode);
            ldc = null;
            if (debug.messageEnabled()) {
                debug.message("Search for User error: ", e);
                debug.message("resultCode: " + ldapResultCode);
            }
            String ldapError = Integer.toString(ldapResultCode);
            Object[] args = { CLASS_NAME, LDAPv3Bundle.getString(ldapError)};
            IdRepoException ide = new IdRepoException(
                IdRepoBundle.BUNDLE_NAME, "311", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;    
        } finally {
            if (ldc != null) {
                connPool.close(ldc);
            }
        }
        try {
            LDAPEntry entry = null;
            boolean userNamingValueSet=false;
            while (results.hasMoreElements()) {
                try {
                    entry = results.next();
                    userDN = entry.getDN();
                    userMatches ++;
                    if (debug.messageEnabled()) {
                        debug.message("searchForName: userDN=" + 
                            userDN + "; entry=" + entry);
                    }
                } catch (LDAPReferralException refe) {
                    debug.message("LDAPReferral Detected.");
                    continue;
                }
            }        
        } catch (LDAPException e) {
            ldapResultCode = e.getLDAPResultCode();
            if (debug.messageEnabled()) {
                debug.message("Search for User error: ", e);
                debug.message("resultCode: " + ldapResultCode);
            }
            String ldapError = Integer.toString(ldapResultCode);
            Object[] args = { CLASS_NAME, LDAPv3Bundle.getString(ldapError)};
            IdRepoException ide = new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "311", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;    
        }
        if (userMatches > 1) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: searchForName return "
                    + " found more than match.");
            }
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "222", args);
        }
        return userDN;
    }
    

    /**
     * Returns <code>true</code> if the data store supports authentication
     * of identities. Used by IdRepo framework to authenticate identities.
     *
     * @return <code>true</code> if data store supports authentication of
     * of identities; else <code>false</code>
     */
    public boolean supportsAuthentication() {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo:supportsAuthentication." +
                " authenticationEnabled=" + true);
        }
        return (true);
    }

    
    /**
     * Returns <code>true</code> if the successfully authenticates
     * the identity with the provided credentials. In case it
     * requires additional credentials, the list would be returned via the
     * <code>IdRepoException</code> exception.
     *
     * @param credentials Array of callback objects containing information
     * such as username and password.
     *
     * @return <code>true</code> if it authenticates the identity;
     * else <code>false</code>
     */
    public boolean authenticate(Callback[] credentials) 
        throws IdRepoException, AuthLoginException {
        debug.message("LDAPv3Repo: authenticate. ");

        // Obtain user name and password from credentials and authenticate
        String username = null;
        String password = null;
        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof NameCallback) {
                username = ((NameCallback) credentials[i]).getName();
                if (debug.messageEnabled()) {
                    debug.message("LDPv3Repo:authenticate username: " +
                                  username);
                }
            } else if (credentials[i] instanceof PasswordCallback) {
                char[] passwd = ((PasswordCallback) credentials[i])
                    .getPassword();
                if (passwd != null) {
                    password = new String(passwd);
                    debug.message(
                        "LDAPv3Repo:authenticate passwd present: XXX"); 
                } else {
                    password = new String();
                }
            }
        }
        if (username == null || password == null) {
            Object args[] = { CLASS_NAME };
            throw new IdRepoException(
                IdRepoBundle.BUNDLE_NAME, "221", args);
        }
        boolean success = false;
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo:authenticate: username="
                     + username);
        }
        String sslStr = getPropertyStringValue(myConfigMap,
                LDAPv3Config_LDAP_SSL_ENABLED);
        boolean ssl = ((sslStr != null) && sslStr.equalsIgnoreCase("true"));

        // ldapServerName is list of server names seperated by sapce for
        // failover purposes. LDAPConnection will automatcially
        // handle failover.
        // port will not be used since ldapserver is in the following format:
        // nameOfLDAPhost:portNumber.
        if (ldapServerName == null) {
            getLDAPServerName(myConfigMap);
        }

        LDAPAuthUtils ldapAuthUtil = null;
        try {
            ldapAuthUtil = new LDAPAuthUtils(ldapServerName, ldapPort,
                    ssl, Locale.getDefaultLocale(), debug);
        } catch (LDAPUtilException ldapUtilEx) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo:authenticate" +
                    " LDAPUtilException: " +
                    ldapUtilEx.getMessage());
            }
            Object[] args = { CLASS_NAME, username};
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    "211", args);
        }

        String authid = getPropertyStringValue(myConfigMap,
                LDAPv3Config_AUTHID);
        String authpw = getPropertyStringValue(myConfigMap,
                LDAPv3Config_AUTHPW);
        ldapAuthUtil.setAuthDN(authid);
        ldapAuthUtil.setAuthPassword(authpw);
        ldapAuthUtil.setScope(searchScope);

        if (authenticatableSet.contains(AUTH_USER)) {
            if (authenticateIt(ldapAuthUtil, IdType.USER,
                    username, password)) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo:authenticate " +
                        "IdType.USER authenticateIt=true");
                }
                return(true);
            }
        }

        if (authenticatableSet.contains(AUTH_AGENT)) {
            if (authenticateIt(ldapAuthUtil, IdType.AGENT,
                    username, password)) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo:authenticate " +
                        "IdType.AGENT authenticateIt=true");
                }
                return(true);
            }
        }

        if (authenticatableSet.contains(AUTH_GROUP)) {
            if (authenticateIt(ldapAuthUtil, IdType.GROUP,
                    username, password)) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo:authenticate " +
                        "IdType.GROUP authenticateIt=true");
                }
                return(true);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit authenticate failed for" +
                    username);
        }
        return(false);
    }


    private boolean authenticateIt(LDAPAuthUtils ldapAuthUtil,
        IdType type, String username, String password)
        throws IdRepoException,
               AuthLoginException {

        if (!type.equals(IdType.USER) && !type.equals(IdType.AGENT) &&
            !type.equals(IdType.GROUP) ) {
            return (false);
        }

        String userid = username;
        String baseDN = getBaseDN(type);
        String namingAttr = getNamingAttr(type);
        if (type.equals(IdType.USER)) {
            namingAttr = userAuthNamingAttr;
        }

        try {
            ldapAuthUtil.setUserNamingAttribute(namingAttr);
            Set userSearchAttr = new HashSet();
            userSearchAttr.add(namingAttr);
            ldapAuthUtil.setUserSearchAttribute(userSearchAttr);
            ldapAuthUtil.setBase(baseDN);
            // need to reset filter otherwise it appends 
            // new filter to previous filter.
            ldapAuthUtil.setFilter("");
            String [] attrs = new String[2];
            attrs[0] ="dn";
            attrs[1]=namingAttr;
            ldapAuthUtil.setUserAttrs(attrs);
            DN edn = new DN(username);
            if (edn.isDN()) {
                userid = edn.explodeDN(true)[0];
            }
            ldapAuthUtil.authenticateUser(userid, password);
        } catch (LDAPUtilException ldapUtilEx) {
            switch (ldapUtilEx.getLDAPResultCode()) {
                case LDAPUtilException.NO_SUCH_OBJECT:
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo:authenticateIt. " +
                            "The specified user does not exist. " +
                            "username=" + username);
                    }
                    throw new AuthLoginException(AM_AUTH,
                            "NoUser", null);
                case LDAPUtilException.INVALID_CREDENTIALS:
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo:authenticateIt." +
                            " Invalid password. username=" + username);
                    }
                    String failureUserID = ldapAuthUtil.getUserId();
                    throw new InvalidPasswordException(AM_AUTH,
                        "InvalidUP", null, failureUserID, null);
                case LDAPUtilException.UNWILLING_TO_PERFORM:
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo:authenticateIt. " +
                            "Unwilling to perform. Account inactivated." +
                             " username" + username);
                    }
                    throw new AuthLoginException(amAuthLDAP,
                        "FConnect", null);
                case LDAPUtilException.INAPPROPRIATE_AUTHENTICATION:
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo:authenticateIt. " +
                            "Inappropriate authentication. username="
                            + username);
                    }
                    throw new AuthLoginException(AM_AUTH, "InappAuth",
                        null);
                case LDAPUtilException.CONSTRAINT_VIOLATION:
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo:authenticateIt. " +
                            "Exceed password retry limit. username"
                            + username);
                    }
                    throw new AuthLoginException(amAuthLDAP,
                            "ExceedRetryLimit", null);
                default:
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo:authenticateIt. " +
                            "default exception. username=" + username);
                    }
                    throw new AuthLoginException(AM_AUTH, "LDAPex", null);
            }
        }
        return ((ldapAuthUtil.getState() == LDAPAuthUtils.SUCCESS) || 
            (ldapAuthUtil.getState() == LDAPAuthUtils.PASSWORD_EXPIRING));
    }


    /*
     * returns the LDPACache handle for this instance of the plugin.
     */
    public LDAPCache GetCache() {
        return(ldapCache);
    }

    /*
     * flush the entire cache starting from orgDN.
     */
    public void clearCache() {
       if (debug.messageEnabled()) {
           debug.message("clearCache");
       }
       if ((!cacheEnabled) || (ldapCache == null)) {
           return;
       }
       boolean status = ldapCache.flushEntries(null, LDAPv2.SCOPE_SUB);
       if (debug.messageEnabled()) {
           debug.message("clearCache: flushed return " + status);
       }
    }

    /*
     * removed the dn from the cache.
     */
    public void objectChanged(String dn, int changeType) {
        if (debug.messageEnabled()) {
            debug.message("objectChanged:  dn=" + dn);
        }
        boolean flushStatus;
        if (hasShutdown || (!cacheEnabled) || (ldapCache == null)) {
            return;
        }

        if (changeType == LDAPPersistSearchControl.ADD) {
            flushStatus = ldapCache.flushEntries(null, LDAPv2.SCOPE_SUB);
        } else if (changeType == LDAPPersistSearchControl.MODIFY) {
            flushStatus = ldapCache.flushEntries(null, LDAPv2.SCOPE_SUB);
            /*
            DN fqdn = new DN(dn);
            DN parentDN = fqdn.getParent();
            String parent = parentDN.toString();
            DN grandParentDN = parentDN.getParent();
            String grandParent = grandParentDN.toString();

            do {
                flushStatus = ldapCache.flushEntries(
                    dn, LDAPv2.SCOPE_BASE);
                if (debug.messageEnabled()) {
                    debug.message("objectChanged " +
                        "LDAPPersistSearchControl.MODIFY " + dn +
                        " dn scope_base flushStatus= " + flushStatus);
                }
            } while (flushStatus);

            do {
                flushStatus = ldapCache.flushEntries(
                    parent, LDAPv2.SCOPE_BASE);
                if (debug.messageEnabled()) {
                    debug.message("objectChanged " +
                        "LDAPPersistSearchControl.MODIFY " + parent +
                        " parent scope_base flushStatus= " + flushStatus);
                }
            } while (flushStatus);

            do {
                flushStatus = ldapCache.flushEntries(
                    parent, LDAPv2.SCOPE_ONE);
                if (debug.messageEnabled()) {
                    debug.message("objectChanged " +
                        "LDAPPersistSearchControl.MODIFY " + parent +
                        " parent scope_one flushStatus= " + flushStatus);
                }
            } while (flushStatus);

            do {
                flushStatus = ldapCache.flushEntries(
                    parent, LDAPv2.SCOPE_SUB);
                if (debug.messageEnabled()) {
                    debug.message("objectChanged" +
                        " LDAPPersistSearchControl.MODIFY " + parent +
                        " parent scope_sub flushStatus= " +flushStatus);
                }
            } while (flushStatus);
            // we need to do grandparent because of role membership.
            // the base of role membershp is 2 levels above user.
            do {
                flushStatus = ldapCache.flushEntries(grandParent,
                    LDAPv2.SCOPE_ONE);
                if (debug.messageEnabled()) {
                    debug.message("objectChanged " +
                        "LDAPPersistSearchControl.MODIFY " +
                        "grandParent scope_one flushStatus= " +
                        grandParent + " " + flushStatus);
                }
            } while (flushStatus);

            do {
                flushStatus = ldapCache.flushEntries(
                    grandParent, LDAPv2.SCOPE_SUB);
                if (debug.messageEnabled()) {
                    debug.message("objectChanged " +
                        "LDAPPersistSearchControl.MODIFY " +
                        "grandParent scope_sub flushStatus= " +
                        grandParent + " " + flushStatus);
                }
            } while (flushStatus);
            */

        } else if (changeType == LDAPPersistSearchControl.MODDN) {
            DN fqdn = new DN(dn);
            DN parentDN = fqdn.getParent();
            String parent = parentDN.toString();
            do {
                flushStatus = ldapCache.flushEntries(parent, LDAPv2.SCOPE_ONE);
                     // this includes self.
                if (debug.messageEnabled()) {
                    debug.message("objectChanged " +
                        "LDAPPersistSearchControl.MODDN" +
                        " parent scope_one: flushStatus= " +
                        parent + " " + flushStatus);
                }
            } while (flushStatus);

            do {
                flushStatus = ldapCache.flushEntries(parent, LDAPv2.SCOPE_BASE);
                if (debug.messageEnabled()) {
                    debug.message("objectChanged LDAPPersistSearchControl.MODDN"
                        + " parent scope_base: flushStatus= " +
                       parent + " " + flushStatus);
                }
            } while (flushStatus);
        } else { // assume LDAPPersistSearchControl.DELETE is the only one left.
            flushStatus = ldapCache.flushEntries(null, LDAPv2.SCOPE_SUB);
        }
    }

    /**
     * find all the datastore that are interested in this change
     * and notify its listener and update the data store's cache.
     */
    protected static void objectChanged(String dn, int changeType, 
        Request request, String psIdKey, boolean allObjChanged,
            boolean clearCache) {
        
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo.objectChanged: dn=" + dn 
                + "; changeType" + changeType + "; psIdKey=" + psIdKey 
                + "; allObjChanged=" + allObjChanged + "; clearCache=" 
                + clearCache);
        }
        //Map listOfRepo = (Map) listOfPS.get(psIdKey);
        HashMap listOfRepo = (HashMap) listOfPS.get(psIdKey);
        if (listOfRepo != null) { 
            //HashSet listOfDS = (HashSet) listOfRepo.get("listOfDS");
            HashMap clonedListOfRepo = (HashMap) listOfRepo.clone();
            HashSet listOfDS = (HashSet) clonedListOfRepo.get("listOfDS");
            Iterator iter = listOfDS.iterator();
            while (iter.hasNext()) {
                LDAPv3Repo v3Repo = (LDAPv3Repo) iter.next();
                if (!v3Repo.hasShutdown) {
                    if (allObjChanged) {
                        clearCache = true;
                        v3Repo.myListener.allObjectsChanged();
                    } else {
                        v3Repo.objectChanged(dn, changeType);
                        Set supportedTypes = v3Repo.getSupportedTypes();
                        Iterator supTypeIter = supportedTypes.iterator();
                        while (supTypeIter.hasNext()) {
                            IdType idType = (IdType) supTypeIter.next();
                            v3Repo.myListener.objectChanged(dn, idType, 
                                changeType, v3Repo.myListener.getConfigMap());
                        }
                    }
                    if (clearCache) {
                        v3Repo.clearCache();
                    }
                }
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.objectChanged: "
                    + "did not find any datastore for this ps.");
            }
        }
    }

    /**
     * checks for  an LDAP v3 server whether the  control has returned
     * if a password has expired or password is expiring and password
     * policy is enabled on the server.
     * @return PASSWOR_EXPIRED if password has expired
     * Return number of seconds until expiration if password is going to expire
     */
    private int checkControls(LDAPConnection ld) {
        LDAPControl[] controls = ld.getResponseControls();
        int status = NO_PASSWORD_CONTROLS;
        if ((controls != null) && (controls.length >= 1)) {
            LDAPPasswordExpiringControl expgControl = null;
            for (int i = 0; i < controls.length; i++) {
                if (controls[i].getType() ==
                    LDAPControl.LDAP_PASSWORD_EXPIRED_CONTROL) {
                    return PASSWORD_EXPIRED;
                }
                if (controls[i].getType() ==
                    LDAPControl.LDAP_PASSWORD_EXPIRING_CONTROL) {
                    expgControl = (LDAPPasswordExpiringControl)controls[i];
                }
            }
            if (expgControl != null) {
                try {
                        /* Return the number of seconds until expiration */
                    return expgControl.getSecondsToExpiration();
                } catch(NumberFormatException e) {
                    if (debug.messageEnabled()) {
                        debug.message( "Unexpected message <" +
                        expgControl.getMessage() +
                        "> in password expiring control" );
                    }
                }
            }
        }
        return NO_PASSWORD_CONTROLS;
    }

    private Collection getOCAttributes( String objClassName )
        throws LDAPException, IdRepoException {
        LDAPSchema dirSchema = getLDAPSchema();
        Collection attributes = getRequiredAttributes(dirSchema,
            objClassName );
        attributes.addAll(getOptionalAttributes(dirSchema,
            objClassName ) );
        return attributes;
    }

    private Collection getRequiredAttributes(LDAPSchema dirSchema,
        String objClassName )
        throws LDAPException, IdRepoException  {
        Collection attributeNames = new ArrayList();
        LDAPObjectClassSchema objClass =
            dirSchema.getObjectClass( objClassName );
        if ( objClass != null ) {
            Enumeration en = objClass.getRequiredAttributes();
            while( en.hasMoreElements() ) {
                attributeNames.add( (String)en.nextElement() );
            }
        }
        return attributeNames;
    }

    private Collection getOptionalAttributes(LDAPSchema dirSchema,
        String objClassName )
        throws LDAPException, IdRepoException {
        Collection attributeNames = new ArrayList();
        LDAPObjectClassSchema objClass =
            dirSchema.getObjectClass( objClassName );
        if( objClass != null ) {
            Enumeration en = objClass.getOptionalAttributes();
            while( en.hasMoreElements() ) {
                attributeNames.add( (String)en.nextElement() );
            }
        }
        return attributeNames;
    }

    private LDAPSchema getLDAPSchema()
        throws LDAPException, IdRepoException {
        LDAPSchema dirSchema = new LDAPSchema();
        checkConnPool();
        LDAPConnection conn = null;
        try {
            conn = connPool.getConnection();
            enableCache(conn);
            dirSchema.fetchSchema(conn);
        } finally {
            if (conn != null) {
                connPool.close(conn);
            }
        }
        return (dirSchema);

    }

    private Set readObjectClass(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        Set attrNameSet = new HashSet();
        attrNameSet.add("objectclass");
        Map objectClassesMap = getAttributes(token, type, name, attrNameSet);
        Set OCValues = (Set) objectClassesMap.get("objectclass");
        return OCValues;
    }

    private Set convertToLowerCase(Set vals) {
        if (vals == null || vals.isEmpty()) {
            return vals;
        } else {
            Set tSet = new HashSet();
            Iterator it = vals.iterator();
            while (it.hasNext()) {
                tSet.add(((String) it.next()).toLowerCase());
            }
            return tSet;
        }
    }

    private Set parseInputedOps(StringTokenizer st, boolean supportService) {
        // read op from st.
        Set opsReadSet = new HashSet();
        while (st.hasMoreTokens()) {
            String idOpToken = st.nextToken();
            if (idOpToken.equalsIgnoreCase("read")) {
                opsReadSet.add(IdOperation.READ);
            } else if (idOpToken.equalsIgnoreCase("edit")) {
                opsReadSet.add(IdOperation.EDIT);
            } else if (idOpToken.equalsIgnoreCase("create")) {
                opsReadSet.add(IdOperation.CREATE);
            } else if (idOpToken.equalsIgnoreCase("delete")) {
                opsReadSet.add(IdOperation.DELETE);
            } else if (idOpToken.equalsIgnoreCase("service")) {
                if (supportService) {
                    opsReadSet.add(IdOperation.SERVICE);
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("parseInputedOps exit: opsReadSet:" + opsReadSet);
        }
        return opsReadSet;
    }

    private void parsedUserSpecifiedOps(Set userSpecifiedOpsSet) {
        // FIXME Is the field this.userSpecifiedOpsSet to be used in this?

        // parse each entry, string, based syntax:
        // idType=idOperation,idOperation ...
        // if the idType is within my type and op then add it.
        if (debug.messageEnabled()) {
            debug.message("parsedUserSpecifiedOps entry: userSpecifiedOpsSet:"
                    + userSpecifiedOpsSet);
        }
        IdType idTypeRead = null;
        Set opsREAD = null;
        Map oldSupportedOps = new HashMap(supportedOps);
        supportedOps.clear();
        Iterator it = userSpecifiedOpsSet.iterator();
        while (it.hasNext()) {
            idTypeRead = null;
            Set opsRead = null;
            String curr = (String) it.next();
            StringTokenizer st = new StringTokenizer(curr, "= ,");
            if (st.hasMoreTokens()) {
                String idtypeToken = st.nextToken(); // read the type.
                if (debug.messageEnabled()) {
                    debug.message("    idtypeToken:" + idtypeToken);
                }
                if (idtypeToken.equalsIgnoreCase("user")) {
                    idTypeRead = IdType.USER;
                    opsRead = parseInputedOps(st, true);
                } else if (idtypeToken.equalsIgnoreCase("group")) {
                    idTypeRead = IdType.GROUP;
                    opsRead = parseInputedOps(st, false);
                } else if (idtypeToken.equalsIgnoreCase("agent")) {
                    idTypeRead = IdType.AGENT;
                    opsRead = parseInputedOps(st, false);
                } else if (idtypeToken.equalsIgnoreCase("role")) {
                    idTypeRead = IdType.ROLE;
                    opsRead = parseInputedOps(st, false);
                } else if (idtypeToken.equalsIgnoreCase("filteredrole")) {
                    idTypeRead = IdType.FILTEREDROLE;
                    opsRead = parseInputedOps(st, false);                    
                } else if (idtypeToken.equalsIgnoreCase("realm")) {
                    idTypeRead = IdType.REALM;
                    opsRead = parseInputedOps(st, true);
                } else {
                    idTypeRead = null; // unknown or unsupported type.
                }
            } // else a blank line.

            if ((idTypeRead != null) && (opsRead != null)
                    && (!opsRead.isEmpty())) {
                supportedOps.put(idTypeRead, opsRead);
                if (debug.messageEnabled()) {
                    debug.message("parsedUserSpecifiedOps called supportedOps:"
                            + supportedOps + "; idTypeRead:" + idTypeRead
                            + "; opsRead:" + opsRead);
                }
            }

        } // while
        // always added the "realm=service" so services can be added to realm.
        Set realmSrv = (Set) supportedOps.get(IdType.REALM);
        if (realmSrv == null) {
            realmSrv = new HashSet();
        }
        realmSrv.add(IdOperation.SERVICE);
        supportedOps.put(IdType.REALM, realmSrv);
    }

    private void loadSupportedOps() {
        Set opSet = new HashSet();
        opSet.add(IdOperation.CREATE);
        opSet.add(IdOperation.DELETE);
        opSet.add(IdOperation.EDIT);
        opSet.add(IdOperation.READ);
        opSet.add(IdOperation.SERVICE);

        supportedOps.put(IdType.USER, Collections.unmodifiableSet(opSet));
        supportedOps.put(IdType.REALM, Collections.unmodifiableSet(opSet));

        Set op2Set = new HashSet(opSet);
        op2Set.remove(IdOperation.SERVICE);
        supportedOps.put(IdType.GROUP, Collections.unmodifiableSet(op2Set));
        supportedOps.put(IdType.AGENT, Collections.unmodifiableSet(op2Set));
        supportedOps.put(IdType.ROLE, Collections.unmodifiableSet(op2Set));
        supportedOps.put(IdType.FILTEREDROLE, 
                Collections.unmodifiableSet(op2Set));
        if (debug.messageEnabled()) {
            debug.message("loadSupportedOps: supportedOps: " + supportedOps);
        }
    }

    private String getObjClassFilter(IdType type) {
        String objClassFilter = null;
        if (type.equals(IdType.USER)) {
            objClassFilter = userSearchFilter;
        } else if (type.equals(IdType.GROUP)) {
            objClassFilter = groupSearchFilter;
        } else if (type.equals(IdType.ROLE)) {
            objClassFilter = roleSearchFilter;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            objClassFilter = filterroleSearchFilter;
        } else if (type.equals(IdType.AGENT)) {
            objClassFilter = agentSearchFilter;
        } else {
            // should we just throw an exception
            objClassFilter = userSearchFilter;
        }
        if (debug.messageEnabled()) {
            debug.message("getObjClassFilter returns: objClassFilter="
                    + objClassFilter);
        }
        return objClassFilter;
    }

    private String getNamingAttr(IdType type) {
        String namingAttr = null; 

        if (type.equals(IdType.USER)) {
            namingAttr = userSearchNamingAttr; 
        } else if (type.equals(IdType.GROUP)) {
            namingAttr = groupSearchNamingAttr;
        } else if (type.equals(IdType.ROLE)) {
            namingAttr = roleSearchNamingAttr;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            namingAttr = filterroleSearchNamingAttr; 
        } else if (type.equals(IdType.AGENT)) {
            namingAttr = agentSearchNamingAttr;
        } else {
            // should we just throw an exception
            namingAttr = userSearchNamingAttr; 
        }

        return namingAttr;
    }

    private String getDN(IdType type, String name) throws IdRepoException {
        String dn;

        String origName = name;
        if (name == null) {
            name = "";
        } else if (name.length() > 0) {
            name = name + ",";
        }

        checkConnPool();
        if (type.equals(IdType.USER)) {
            if ((peopleCtnrValue == null) || (peopleCtnrValue.length() == 0)
                    || (peopleCtnrNamingAttr == null)
                    || (peopleCtnrNamingAttr.length() == 0)) {
                // Since people container is not specified, do a sub-tree
                // search to find the user DN
                // Auto-construct the DN in case search failed
                dn = userSearchNamingAttr + "=" + name + orgDN;
                String filter = constructFilter(userSearchNamingAttr,
                        getObjClassFilter(IdType.USER), origName);
                LDAPConnection ld = null;
                try {
                    LDAPSearchConstraints searchConstraints =
                        new LDAPSearchConstraints();
                    searchConstraints.setMaxResults(2);
                    searchConstraints.setServerTimeLimit(timeLimit);
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo.getDN. before search. name="
                            + name + "; filter=" + filter + ";  orgDN=" + orgDN);
                    }
                    LDAPSearchResults results = null;
                    if ((userAtttributesAllowed == null) ||
                        (userAtttributesAllowed.isEmpty()))  {
                        LDAPSearchRequest request =
                            LDAPRequestParser.parseSearchRequest(
                            orgDN, LDAPv2.SCOPE_SUB, filter, null, false,
                            searchConstraints);
                        try {
                            ld = connPool.getConnection();
                            if (ld == null) {
                                debug.error("LDAPv3Repo: getDN. ld is null");
                            } 
                            enableCache(ld);
                            results = ld.search(request, searchConstraints);
                        } finally {
                            if (ld != null) {
                                connPool.close(ld);
                            }
                        }
                    } else {
                        String[] attrArr =
                            (String[]) userAtttributesAllowed.toArray(
                            new String[userAtttributesAllowed.size()]);
                        LDAPSearchRequest request =
                            LDAPRequestParser.parseSearchRequest(orgDN,
                            LDAPv2.SCOPE_SUB, filter,  attrArr, false,
                            searchConstraints);
                        try {
                            ld = connPool.getConnection();
                            if (ld == null) {
                                debug.error("LDAPv3Repo: getDN. ld is null");
                            }
                            enableCache(ld);
                            results = ld.search(request, searchConstraints);
                        } finally {
                            if (ld != null) {
                                connPool.close(ld);
                            }
                        }
                    }
                    if (results != null && results.hasMoreElements()) {
                        // Take the first DN
                        LDAPEntry myEntry = results.next();
                        if (myEntry != null) {                        
                            dn = myEntry.getDN();
                        } else {
                            if (debug.messageEnabled()) {
                                debug.message("LDAPv3Repo: getDN search again.");
                            }
                            clearCache();
                            LDAPSearchRequest request =
                                LDAPRequestParser.parseSearchRequest(orgDN,
                                LDAPv2.SCOPE_SUB, filter, null, false,
                                searchConstraints);
                            try {
                                ld = connPool.getConnection();
                                if (ld == null) {
                                    debug.error("LDAPv3Repo: getDN. ld is null");
                                }
                                enableCache(ld);
                                results = ld.search(request,
                                    searchConstraints);
                            } finally {
                                if (ld != null) {
                                    connPool.close(ld);
                                }
                            }
                            if (results != null && results.hasMoreElements()) {
                                myEntry = results.next(); 
                                dn = myEntry.getDN(); 
                            } else {
                                if (debug.messageEnabled()) {
                                    debug.message("LDAPv3Repo: 2nd getDN " +
                                        "user search null.");  
                                } 
                            }
 
                        }
                        if (debug.messageEnabled()) {
                            debug.message(
                                "LDAPv3Repo.getDN. search return dn=" + dn);
                        }
                    } else {
                        if (debug.warningEnabled()) {
                            debug.message("LDAPv3Repo.getDN. user not found");
                        }
                    }
                } catch (Exception lde) {
                    // Debug the exception and return the auto-constructed DN
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: getDN user search", lde);
                    }
                }
            } else {
                dn = userSearchNamingAttr + "=" + name + peopleCtnrNamingAttr
                        + "=" + peopleCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.AGENT)) {
            if ((agentCtnrValue == null) || (agentCtnrValue.length() == 0)
                    || (agentCtnrNamingAttr == null)
                    || (agentCtnrNamingAttr.length() == 0)) {
                dn = agentSearchNamingAttr + "=" + name + orgDN;
            } else {
                dn = agentSearchNamingAttr + "=" + name + agentCtnrNamingAttr
                        + "=" + agentCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.GROUP)) {
            if ((groupCtnrValue == null) || (groupCtnrValue.length() == 0)
                    || (groupCtnrNamingAttr == null)
                    || (groupCtnrNamingAttr.length() == 0)) {
                dn = groupSearchNamingAttr + "=" + name + orgDN;
                String filter = constructFilter(groupSearchNamingAttr,
                    getObjClassFilter(IdType.GROUP), origName);
                LDAPConnection ld = null;
                try {
                    LDAPSearchResults results = null;
                    LDAPSearchRequest request =
                        LDAPRequestParser.parseSearchRequest(orgDN,
                        LDAPv2.SCOPE_SUB, filter, null, false,  timeLimit,
                        LDAPRequestParser.DEFAULT_DEREFERENCE,
                        defaultMaxResults);
                    try {
                        ld = connPool.getConnection();
                        enableCache(ld);
                        results = ld.search(request);
                    } finally {
                        if (ld != null) {
                            connPool.close(ld);
                        }
                    }
                    if (results != null && results.hasMoreElements()) {
                        // Take the first DN
                        dn = results.next().getDN();
                    }
                } catch (Exception lde) {
                    // Debug the exception and return the auto-constructed DN
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: getDN user search", lde);
                    }
                }
            } else {
                dn = groupSearchNamingAttr + "=" + name + groupCtnrNamingAttr
                        + "=" + groupCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.ROLE)) {
            dn = roleSearchNamingAttr + "=" + name + orgDN;
        } else if (type.equals(IdType.FILTEREDROLE)) {
            dn = filterroleSearchNamingAttr + "=" + name + orgDN;
        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return dn;
    }

    private String getBaseDN(IdType type) {
        String dn;

        if (type.equals(IdType.USER)) {
            if ((peopleCtnrValue == null) || (peopleCtnrValue.length() == 0)
                    || (peopleCtnrNamingAttr == null)
                    || (peopleCtnrNamingAttr.length() == 0)) {
                dn = orgDN;
            } else {
                dn = peopleCtnrNamingAttr + "=" + peopleCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.AGENT)) {
            if ((agentCtnrValue == null) || (agentCtnrValue.length() == 0)
                    || (agentCtnrNamingAttr == null)
                    || (agentCtnrNamingAttr.length() == 0)) {
                dn = orgDN;
            } else {
                dn = agentCtnrNamingAttr + "=" + agentCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.GROUP)) {
            if ((groupCtnrValue == null) || (groupCtnrValue.length() == 0)
                    || (groupCtnrNamingAttr == null)
                    || (groupCtnrNamingAttr.length() == 0)) {
                dn = orgDN;
            } else {
                dn = groupCtnrNamingAttr + "=" + groupCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.ROLE) || 
            type.equals(IdType.FILTEREDROLE)) {
            dn = orgDN;
        } else {
            dn = orgDN;
        }
        return dn;
    }

    private String constructFilter(int filterModifier, Map avPairs) {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: constructFilter: avPairs=" + avPairs 
                + "filterModifier=" + filterModifier);
        }
        if (avPairs == null || filterModifier == IdRepo.NO_MOD) {
            return null;
        }

        StringBuffer filterSB = new StringBuffer();

        if (filterModifier == IdRepo.OR_MOD) {
            filterSB.append("(|");
        } else if (filterModifier == IdRepo.AND_MOD) {
            filterSB.append("(&");
        }

        Iterator iter = avPairs.keySet().iterator();
        while (iter.hasNext()) {
            String attributeName = (String) iter.next();
            Iterator iter2 = ((Set) (avPairs.get(attributeName))).iterator();

            while (iter2.hasNext()) {
                String attributeValue = (String) iter2.next();
                filterSB.append("(").append(attributeName).append("=").append(
                        attributeValue).append(")");
            }
        }
        filterSB.append(")");
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit constructFilter: " + "filterSB= "
                    + filterSB);
        }
        return filterSB.toString();
    }

    private String constructFilter(String namingAttr, String objectClassFilter,
            String wildcard) {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: constructFilter: "
                    + "objectClassFilter=" + objectClassFilter + "; wildcard="
                    + wildcard + "; namingAttr=" + namingAttr);
        }
        StringBuffer filterSB = new StringBuffer();
        int index = objectClassFilter.indexOf("%U");
        int vIndex = objectClassFilter.indexOf("%V");

        if ((index == -1) && (vIndex == -1)) {
            if ((namingAttr == null) || (namingAttr.length() == 0)) {
                filterSB.append(objectClassFilter);
            } else {
                filterSB.append("(&(").append(namingAttr).append("=").append(
                        wildcard).append(")").append(objectClassFilter).append(
                        ")");
            }
            objectClassFilter = filterSB.toString();

            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: exit 1 constructFilter . "
                        + "objectClassFilter=" + objectClassFilter);
            }
            return (objectClassFilter);
        } else {
            String uPart;
            String vPart;
            int indexat = wildcard.indexOf("@");

            if (indexat == -1) {
                uPart = wildcard;
                vPart = "*";
            } else {
                uPart = wildcard.substring(0, indexat);
                vPart = wildcard.substring(indexat + 1);
            }
        
            while (index != -1) {
                filterSB.append(objectClassFilter.substring(0, index)).append(
                        wildcard)
                        .append(objectClassFilter.substring(index + 2));
                objectClassFilter = filterSB.toString();
                filterSB = new StringBuffer();
                index = objectClassFilter.indexOf("%U");
            }

            // int index2 = objectClassFilter.indexOf("%V");
            while (vIndex != -1) {
                filterSB.append(objectClassFilter.substring(0, vIndex)).append(
                        wildcard).append(
                        objectClassFilter.substring(vIndex + 2));
                objectClassFilter = filterSB.toString();
                filterSB = new StringBuffer();
                vIndex = objectClassFilter.indexOf("%V");
            }
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit constructFilter. "
                    + "objectClassFilter=" + objectClassFilter);
        }

        return objectClassFilter;
    }

    private Map getCreateUserAttrMapping(Map configParams) {
        Set createUserAttrMappingSet = ((Set) configParams
                .get(LDAPv3Config_LDAP_CREATEUSERMAPPING));

        Map createAttrMap;
        if (createUserAttrMappingSet == null
                || createUserAttrMappingSet.isEmpty()) {
            createAttrMap = Collections.EMPTY_MAP;
        } else {
            if (debug.messageEnabled()) {
                debug.message("in getCreateUserAttrMapping: "
                        + "createUserAttrMappingSet="
                        + createUserAttrMappingSet);
            }
            int size = createUserAttrMappingSet.size();
            createAttrMap = new CaseInsensitiveHashMap(size);
            Iterator it = createUserAttrMappingSet.iterator();
            while (it.hasNext()) {
                String mapString = (String) it.next();
                int eqIndex = mapString.indexOf('=');
                if (eqIndex > -1) {
                    String first = mapString.substring(0, eqIndex);
                    String second = mapString.substring(eqIndex + 1);
                    createAttrMap.put(first, second);
                } else {
                    // this is a special case to denote use the user name for
                    // attr value.
                    createAttrMap.put(mapString, mapString);
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("exit getCreateUserAttrMapping: createAttrMap="
                    + createAttrMap);
        }
        return createAttrMap;
    }

    private void setDSType(Map configParams) {
        if (configParams.containsKey(LDAPv3Config_LDAPV3AD)) {
            dsType = LDAPv3Config_LDAPV3AD;
        } else if (configParams.containsKey(LDAPv3Config_LDAPV3ADAM)) {
            dsType = LDAPv3Config_LDAPV3ADAM;
        } else if (configParams.containsKey(LDAPv3Config_LDAPV3AMDS)) {
            dsType = LDAPv3Config_LDAPV3AMDS;
        } else if (configParams.containsKey(LDAPv3Config_LDAPV3OpenDS)) {
            dsType = LDAPv3Config_LDAPV3OpenDS;
        } else if (configParams.containsKey(LDAPv3Config_LDAPV3Tivoli)) {
            dsType = LDAPv3Config_LDAPV3Tivoli;
        } else {
            dsType = LDAPv3Config_LDAPV3GENERIC;
        }
    }

    private String getPSKey(Map configParams) {
        String psearchBase = getPropertyStringValue(myConfigMap,
                LDAPv3Config_LDAP_PSEARCHBASE);
        String pfilter = getPropertyStringValue(myConfigMap,
            LDAPv3Config_LDAP_PSEARCHFILTER);
       
        // the key is made up of ... 
        // we might not need _idleTimeOut, _numRetries, _retryInterval
        // and _retryErrorCodes since these values will likely be same 
        // for a given host. might be true for ssl also.
        String psIdKey = ldapServerName + orgDN + psearchBase + pfilter; 
        return psIdKey;
    }


    private String getPropertyRetryErrorCodes(Map configParams, String key) {
        TreeSet codes = new TreeSet();
        Set retryErrorSet = (Set) configParams.get(key);
        Iterator itr = retryErrorSet.iterator();
        while (itr.hasNext()) {
            codes.add(itr.next());
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo.getPropertyRetryErrorCodes: "
                    + key + "retryErrorSet=" + retryErrorSet 
                    + " ; codes=" + codes);
        }
        String sortedCodes = "";
        itr = codes.iterator();
        while (itr.hasNext()) {
            if (sortedCodes.length() == 0) {
                sortedCodes = sortedCodes + (String) (itr.next());
            } else {
                sortedCodes = sortedCodes + "," + (String) (itr.next());
            }
        }
        return sortedCodes;
    }


    private int getPropertyIntValue(Map configParams, String key,
            int defaultValue) {
        int value = defaultValue;
        try {
            Set valueSet = (Set) configParams.get(key);
            if (valueSet != null && !valueSet.isEmpty()) {
                value = Integer.parseInt((String) valueSet.iterator().next());
            }
        } catch (NumberFormatException nfe) {
            value = defaultValue;
        }
        if (debug.messageEnabled()) {
            debug.message("    LDAPv3Repo.getPropertyIntValue(): " + key
                    + " = " + value);
        }
        return value;
    }

    private String getPropertyStringValue(
        Map configParams, String key, String defaultVal) {
        String value = getPropertyStringValue(configParams, key);
        if (value == null) {
            value = defaultVal;
        }
        return value;
    }

    private String getPropertyStringValue(Map configParams, String key) {
        String value = null;
        Set valueSet = (Set) configParams.get(key);
        if (valueSet != null && !valueSet.isEmpty()) {
            value = (String) valueSet.iterator().next();
        } else {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.getPropertyStringValue failed:"
                                + key);
            }
        }
        if (debug.messageEnabled()) {
            if (!key.equals(LDAPv3Config_AUTHPW)) {
                debug.message("    LDAPv3Repo.getPropertyStringValue(): " + key
                        + " = " + value);
            } else {
                if ((value == null) || (value.length() == 0)) {
                    debug.message("    LDAPv3Repo.getPropertyStringValue(): "
                            + key + " = NULL or ZERO LENGTH");
                } else {
                    debug.message("    LDAPv3Repo.getPropertyStringValue(): "
                            + key + " = has value XXX");
                }
            }
        }
        return value;
    }

    private boolean getPropertyBooleanValue(Map configParams, String key) {
        String value = getPropertyStringValue(configParams, key);
        return ((value != null) && value.equalsIgnoreCase("true"));
    }

    private void checkConnPool()
        throws IdRepoException {
        if (connPool == null) {
            Object[] args = {CLASS_NAME, LDAPv3Bundle.getString(ldapConnError)};
            IdRepoException ide = new IdRepoException(
                IdRepoBundle.BUNDLE_NAME, "311", args);
            ide.setLDAPErrorCode(ldapConnError);
            throw ide;
        }
    }

    private void handleLDAPException(LDAPException lde, String eDN)
        throws IdRepoException, IdRepoFatalException {
        int resultCode = lde.getLDAPResultCode();
        String ldapError = Integer.toString(resultCode);
        String errorMessage = lde.getLDAPErrorMessage();

        Object[] args = { CLASS_NAME, LDAPv3Bundle.getString(ldapError), ""};
        if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82)) {
            IdRepoFatalException ide = new IdRepoFatalException(
                    IdRepoBundle.BUNDLE_NAME, "311", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;
        } else if (resultCode == LDAPException.CONSTRAINT_VIOLATION) {
            // Throw Fatal exception for errCode 19 (Password too short)
            // as it breaks password policy for password length.
            args[0] = CLASS_NAME;
            args[1] = ldapError;
            args[2] = errorMessage;
            IdRepoFatalException ide = new IdRepoFatalException(
                    IdRepoBundle.BUNDLE_NAME, "313", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;
        } else if (errorMessage !=null && errorMessage.length() > 0 ) {
            IdRepoException ide = new IdRepoException(
                IdRepoBundle.BUNDLE_NAME, "311", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;
        } else if (resultCode == LDAPException.NO_SUCH_OBJECT) {
            args[0]  = CLASS_NAME;
            if (eDN == null) {
                eDN = "";
            }
            args[1]  = eDN;
            IdRepoException ide = new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "220", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;
        } else {
            IdRepoException ide = new IdRepoException(
                    IdRepoBundle.BUNDLE_NAME, "311", args);
            ide.setLDAPErrorCode(ldapError);
            throw ide;
        }
    }

}
