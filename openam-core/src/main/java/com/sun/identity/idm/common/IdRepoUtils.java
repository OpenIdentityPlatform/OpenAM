/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: IdRepoUtils.java,v 1.3 2010/01/06 22:31:55 veiming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.idm.common;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.*;
import static org.forgerock.openam.ldap.LDAPConstants.*;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.StringUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.ldap.LdifUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;

/**
 * This class provides common utility functions for IdRepo.
 */
public class IdRepoUtils {

    private static final String LDAPv3ForODSEE = "LDAPv3ForAMDS";
    private static final String LDAPv3ForOpenDS = "LDAPv3ForOpenDS";
    private static final String LDAPv3ForAD = "LDAPv3ForAD";
    private static final String LDAPv3ForADAM = "LDAPv3ForADAM";
    private static final String LDAPv3ForTivoli = "LDAPv3ForTivoli";

    private static final String ODSEE_LDIF = "odseeSchema";
    private static final String OpenDS_LDIF = "opendsUserSchema";
    private static final String AD_LDIF = "adUserSchema";
    private static final String ADAM_LDIF = "adamUserSchema";
    private static final String TIVOLI_LDIF = "tivoliUserSchema";

    private static final String SCHEMA_PROPERTY_FILENAME = "schemaNames";
    private static final String LDAPv3_LDAP_SERVER = "sun-idrepo-ldapv3-config-ldap-server";
    private static final Set<String> defaultPwdAttrs = new HashSet<String>(2);
    private static Debug DEBUG = Debug.getInstance("IdRepoUtils");

    static {
        defaultPwdAttrs.add("userpassword");
        defaultPwdAttrs.add("unicodepwd");
    }

    /**
     * Returns an attribute map with all the password attributes being masked.
     * 
     * @param attrMap an attribute map
     * @param pwdAttrs a set of password attribute names
     *
     * @return an attribute map with all the password attributes being masked.
     */
    public static Map<String, ?> getAttrMapWithoutPasswordAttrs(Map<String, ?> attrMap, Set<String> pwdAttrs) {
        if (attrMap == null || attrMap.isEmpty()) {
            return attrMap;
        }

        //the attrmap needs to be case-insensitive in order to detect password attributes correctly
        attrMap = new CaseInsensitiveHashMap(attrMap);
        Set<String> allPwdAttrs = new HashSet<String>(defaultPwdAttrs);
        if (pwdAttrs != null) {
            allPwdAttrs.addAll(pwdAttrs);
        }

        AMHashMap returnAttrMap = null;
        for (String pwdAttr : allPwdAttrs) {
            if (attrMap.containsKey(pwdAttr)) {
                if (returnAttrMap == null) {
                    returnAttrMap = new AMHashMap();
                    returnAttrMap.copy(attrMap);
                }
                returnAttrMap.put(pwdAttr, "xxx...");
            }
        }

        return (returnAttrMap == null ? attrMap : returnAttrMap);
    }

    private static String getSchemaFiles(String idRepoType) {
        ResourceBundle rb = ResourceBundle.getBundle(
            SCHEMA_PROPERTY_FILENAME);
        String schemaFiles = null;

        if (idRepoType.equals(LDAPv3ForODSEE)) {
            schemaFiles = rb.getString(ODSEE_LDIF);
        } else if (idRepoType.equals(LDAPv3ForOpenDS)) {
            schemaFiles = rb.getString(OpenDS_LDIF);
        } else if (idRepoType.equals(LDAPv3ForAD)) {
            schemaFiles = rb.getString(AD_LDIF);
        } else if (idRepoType.equals(LDAPv3ForADAM)) {
            schemaFiles = rb.getString(ADAM_LDIF);
        } else if (idRepoType.equals(LDAPv3ForTivoli)) {
            schemaFiles = rb.getString(TIVOLI_LDIF);
        }

        return schemaFiles;
    }

    /**
     * Return true if specified IdRepo type has schemas.
     * 
     * @param idRepoType IdRepo type
     * @return true if specified IdRepo type has schemas
     */
    public static boolean hasIdRepoSchema(String idRepoType) {
        if (idRepoType == null) {
            return false;
        }

        String schemaFiles = getSchemaFiles(idRepoType);
        return ((schemaFiles != null) && (schemaFiles.trim().length() > 0));
            
    }

    /**
     * Loads schema to specified IdRepo.
     * 
     * @param ssoToken single sign on token of authenticated user identity
     * @param idRepoName IdRepo name
     * @param realm the realm
     * @param servletCtx the servlet context
     *
     * @throws IdRepoException If schema can't be loaded or there are
     *     repository related error conditions.
     */
    public static void loadIdRepoSchema(SSOToken ssoToken, String idRepoName,
        String realm, ServletContext servletCtx) throws IdRepoException {

        if (servletCtx == null) {
            return;
        }

        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, ssoToken);
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(realm, null);
            ServiceConfig ss = cfg.getSubConfig(idRepoName);
            if (ss == null) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("IdRepoUtils.loadIdRepoSchema: data store " +
                    idRepoName + " for realm " + realm + " doesn't exist.");
                }
                Object args[] = { idRepoName, realm };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.PLUGIN_DOESNT_EXIST_FOR_REALM,
                    args);
            }

            String idRepoType = ss.getSchemaID();

            Map attrValues = ss.getAttributes();

            String schemaFiles = getSchemaFiles(idRepoType);
            if ((schemaFiles == null) || (schemaFiles.trim().length() == 0)) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("IdRepoUtils.loadIdRepoSchema: data store " +
                    idRepoName + " for realm " + realm + " doesn't have " +
                    "schema files.");
                }
                return;
            }

            StringTokenizer st = new StringTokenizer(schemaFiles);
            while (st.hasMoreTokens()) {
                String schemaFile = st.nextToken();
                tagSwapAndImportSchema(schemaFile, attrValues, servletCtx,
                    idRepoType);
            }
        } catch (SMSException smsex) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("IdRepoUtils.loadIdRepoSchema:", smsex);
            }
            Object args[] = { idRepoName, realm };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_READ_PLUGIN_FOR_REALM, args);
        } catch (SSOException ssoex) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("IdRepoUtils.loadIdRepoSchema:", ssoex);
            }
            Object args[] = { idRepoName, realm };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.UNABLE_READ_PLUGING_FOR_REALM_SSOTOKEN_NOT_VALID, args);
        } catch (Exception ex) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("IdRepoUtils.loadIdRepoSchema:", ex);
            }
            Object args[] = { idRepoName, realm, ex.getMessage() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                    IdRepoErrorCode.UNABLE_LOAD_SCHEMA_FOR_PLUGIN_FOR_REALM, args);
       }
    }

    private static void tagSwapAndImportSchema(String schemaFile,
        Map attrValues, ServletContext servletCtx, String idRepoType)
        throws Exception {

        DataInputStream dis = null;
        try (ConnectionFactory factory = getLDAPConnection(attrValues);
             Connection ld = factory.getConnection();
             InputStreamReader fin = new InputStreamReader(servletCtx.getResourceAsStream(schemaFile))) {

            StringBuilder sbuf = new StringBuilder();
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
            String schemaStr = sbuf.toString();

            String suffix = CollectionHelper.getMapAttr(attrValues,
                "sun-idrepo-ldapv3-config-organization_name");
            if (suffix != null) {
                schemaStr = StringUtils.strReplaceAll(schemaStr, 
                    "@userStoreRootSuffix@", suffix);
                String dbName = LDAPUtils.getDBName(suffix, ld);
                schemaStr = StringUtils.strReplaceAll(schemaStr, "@DB_NAME@",
                    dbName);
            }

            if (idRepoType.equals(LDAPv3ForADAM)) {
                String adamInstanceGUID = getADAMInstanceGUID(attrValues);
                if (adamInstanceGUID != null) {
                    schemaStr = StringUtils.strReplaceAll(schemaStr, 
                        "@INSTANCE_GUID@", adamInstanceGUID);
                }
            }
	        schemaStr = ServicesDefaultValues.tagSwap(schemaStr);

            dis = new DataInputStream(new ByteArrayInputStream(schemaStr.getBytes()));
            LdifUtils.createSchemaFromLDIF(dis, ld);
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (Exception ex) {
                    //No handling requried
                }
            }
        }
    }

    private static String getADAMInstanceGUID(Map attrValues) throws Exception {
        try (ConnectionFactory factory = getLDAPConnection(attrValues);
             Connection ld = factory.getConnection()){
            String attrName = "schemaNamingContext";
            String[] attrs = { attrName };
            ConnectionEntryReader res = ld.search(LDAPRequests.newSearchRequest("", SearchScope.BASE_OBJECT,
                    "(objectclass=*)"));
            if (res.hasNext()) {
                SearchResultEntry entry = res.readEntry();
                Attribute ldapAttr = entry.getAttribute(attrName);
                if (ldapAttr != null) {
                    String value = ldapAttr.firstValueAsString();
                    int index = value.lastIndexOf("=");
                    if (index != -1) {
                        return value.substring(index + 1).trim();
                    }
                }
            }
        }

        return null;
    }

    private static ConnectionFactory getLDAPConnection(Map attrValues)
        throws Exception {
        Options options = Options.defaultOptions()
                .set(CONNECT_TIMEOUT, new Duration((long) 300, TimeUnit.MILLISECONDS));
        String connectionMode = CollectionHelper.getMapAttr(attrValues, LDAP_CONNECTION_MODE);
        if (LDAP_CONNECTION_MODE_LDAPS.equalsIgnoreCase(connectionMode) ||
                LDAP_CONNECTION_MODE_STARTTLS.equalsIgnoreCase(connectionMode)){
            String defaultProtocolVersion = SystemProperties.get(Constants.LDAP_SERVER_TLS_VERSION, "TLS");
            options = options.set(SSL_CONTEXT,
                    new SSLContextBuilder().setProtocol(defaultProtocolVersion).getSSLContext());
          if (LDAP_CONNECTION_MODE_STARTTLS.equalsIgnoreCase(connectionMode)) {
              options = options.set(SSL_USE_STARTTLS, true);
          }
        }

        Set<LDAPURL> ldapUrls = getLDAPUrls(attrValues);
        if (CollectionUtils.isEmpty(ldapUrls)) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("IdRepoUtils.getLDAPConnection: No LDAPURLs found");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_AUTHENTICATE_LDAP_SERVER, null);
        }

        LDAPURL ldapUrl = ldapUrls.iterator().next();
        if (org.forgerock.openam.utils.StringUtils.isEmpty(ldapUrl.getHost())) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("IdRepoUtils.getLDAPConnection: No LDAP host found");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_AUTHENTICATE_LDAP_SERVER, null);
        }

        // All connections will use authentication
        String bindDn = CollectionHelper.getMapAttr(attrValues, "sun-idrepo-ldapv3-config-authid");
        if (org.forgerock.openam.utils.StringUtils.isBlank(bindDn)) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("IdRepoUtils.getLDAPConnection: No LDAP bindDN found");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_AUTHENTICATE_LDAP_SERVER, null);
        }
        String bindPwd = CollectionHelper.getMapAttr(attrValues, "sun-idrepo-ldapv3-config-authpw");
        if (org.forgerock.openam.utils.StringUtils.isBlank(bindPwd)) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("IdRepoUtils.getLDAPConnection: No LDAP bindPW found");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, IdRepoErrorCode.UNABLE_AUTHENTICATE_LDAP_SERVER, null);
        }
        options = options.set(AUTHN_BIND_REQUEST, LDAPRequests.newSimpleBindRequest(bindDn, bindPwd.toCharArray()));

        return new LDAPConnectionFactory(ldapUrl.getHost(), ldapUrl.getPort(), options);
    }

    /**
     * Parses the incoming types and operations in string format to their domain object equivalent. For example:
     * <code>user=read,edit,service,delete</code>
     * will be turned into USER identity type with READ, EDIT, SERVICE, DELETE operations.
     *
     * @param typesAndOperations The parsable type and operation lines (each line represents an identity type.
     * @return The parsed identity types and operation mappings.
     */
    public static Map<IdType, Set<IdOperation>> parseSupportedTypesAndOperations(Set<String> typesAndOperations) {
        Map<IdType, Set<IdOperation>> supportedTypesAndOperations = new HashMap<IdType, Set<IdOperation>>(6);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Parsing supported types and operations:" + typesAndOperations);
        }
        for (String typeAndOperation : typesAndOperations) {
            int idx = typeAndOperation.indexOf('=');
            if (idx == -1) {
                DEBUG.error("Invalid supported type/operation configuration: " + typeAndOperation);
                continue;
            }
            String type = typeAndOperation.substring(0, idx).trim();
            IdType idType = null;
            boolean supportsService = false;
            if (type.equalsIgnoreCase(IdType.USER.getName())) {
                idType = IdType.USER;
                supportsService = true;
            } else if (type.equalsIgnoreCase(IdType.GROUP.getName())) {
                idType = IdType.GROUP;
            } else if (type.equalsIgnoreCase(IdType.ROLE.getName())) {
                idType = IdType.ROLE;
            } else if (type.equalsIgnoreCase(IdType.FILTEREDROLE.getName())) {
                idType = IdType.FILTEREDROLE;
            } else if (type.equalsIgnoreCase(IdType.AGENT.getName())) {
                idType = IdType.AGENT;
            } else if (type.equalsIgnoreCase(IdType.REALM.getName())) {
                idType = IdType.REALM;
                supportsService = true;
            }
            supportedTypesAndOperations.put(idType,
                    parseSupportedOperations(typeAndOperation.substring(idx + 1), supportsService));
        }
        //we need to make sure that realm=service is always present
        Set<IdOperation> realmOps = supportedTypesAndOperations.get(IdType.REALM);
        if (realmOps == null) {
            realmOps = new HashSet<IdOperation>(1);
        }
        realmOps.add(IdOperation.SERVICE);
        supportedTypesAndOperations.put(IdType.REALM, realmOps);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Parsed supported types and operations are: " + supportedTypesAndOperations);
        }
        return supportedTypesAndOperations;
    }

    private static Set<IdOperation> parseSupportedOperations(String operations, boolean supportsService) {
        String[] ops = operations.split(",");
        Set<IdOperation> idOperations = new HashSet<IdOperation>(5);
        for (String op : ops) {
            op = op.trim();
            if (op.equalsIgnoreCase(IdOperation.CREATE.getName())) {
                idOperations.add(IdOperation.CREATE);
            } else if (op.equalsIgnoreCase(IdOperation.DELETE.getName())) {
                idOperations.add(IdOperation.DELETE);
            } else if (op.equalsIgnoreCase(IdOperation.EDIT.getName())) {
                idOperations.add(IdOperation.EDIT);
            } else if (op.equalsIgnoreCase(IdOperation.READ.getName())) {
                idOperations.add(IdOperation.READ);
            } else if (op.equalsIgnoreCase(IdOperation.SERVICE.getName())) {
                if (supportsService) {
                    idOperations.add(IdOperation.SERVICE);
                }
            }
        }
        return idOperations;
    }

    /**
     * Parses attribute mapping settings in the format of "from=to". If there is no mapping provided, then this will be
     * a mapping to itself.
     *
     * @param mappings A set of mappings that needs to be parsed
     * @return The processed mappings returned as a map.
     */
    public static Map<String, String> parseAttributeMapping(Set<String> mappings) {
        Map<String, String> map = new CaseInsensitiveHashMap();
        if (mappings == null) {
            return map;
        }

        for (String mapping : mappings) {
            int idx = mapping.indexOf('=');
            if (idx == -1) {
                map.put(mapping, mapping);
            } else {
                String from = mapping.substring(0, idx);
                String to = mapping.substring(idx + 1);
                map.put(from, to);
            }
        }
        return map;
    }

    private static Set<LDAPURL> getLDAPUrls( Map<String, Set<String>> attrValues) {
        // Get the prioritised set of ldap servers
        Set<String> ldapServers = attrValues.get(LDAPv3_LDAP_SERVER);
        Set<LDAPURL> ldapUrls = null;
        if (null != ldapServers) {
            String hostServerId = null;
            String hostSiteId = "";
            try {
                hostServerId = WebtopNaming.getAMServerID();
                hostSiteId = WebtopNaming.getSiteID(hostServerId);
            } catch (ServerEntryNotFoundException senfe) {
                if (DEBUG.warningEnabled()) {
                    DEBUG.warning("ServerEntryNotFoundException, hostServerId=" + hostServerId + ", hostSiteId="
                            + hostSiteId);
                }
            }
            ldapUrls = org.forgerock.openam.ldap.LDAPUtils.prioritizeServers(ldapServers, hostServerId, hostSiteId);
        }
        return ldapUrls;
    }
}
