/*
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
 * $Id: Step4.java,v 1.20 2009/10/27 05:31:45 hengming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems LLC.
 */

package com.sun.identity.config.wizard;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.SetupConstants;
import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.openidentityplatform.openam.config.servlet.ConfiguratorAction;
import org.openidentityplatform.openam.config.servlet.ConfiguratorContext;
import org.openidentityplatform.openam.config.servlet.SetupPage;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchScope;

/**
 * Step 4 is the input of the remote user data store properties.
 */
public class Step4 extends SetupPage {
    public static final String LDAP_STORE_SESSION_KEY = "wizardCustomUserStore";

    private String responseString = "ok";

    private static final String ObjectClassFilter = "(objectclass=*)";

    @Override
    public boolean onSecurityCheck() {
        // Ported from the old com.sun.identity.config.util.ProtectedPage: block re-entry once
        // OpenAM has already been configured.
        if (AMSetupServlet.isConfigured()) {
            skipRender();
            return false;
        }
        return true;
    }

    @Override
    public void onInit() {
        super.onInit();
        ConfiguratorContext ctx = getContext();

        if (ctx.getSessionAttribute(SessionAttributeNames.USER_STORE_HOST)
            == null) {
            String val = getAttribute(SetupConstants.CONFIG_VAR_DATA_STORE,
                SetupConstants.SMS_EMBED_DATASTORE);

            if (!val.equals(SetupConstants.SMS_EMBED_DATASTORE)) {

                val = getAttribute("configStoreSSL", "SIMPLE");
                ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_SSL,
                    val);

                val = getAttribute("configStoreHost", getHostName());
                ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_HOST,
                    val);

                val = getAttribute("configStorePort", "389");
                ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_PORT,
                    val);

                val = getAttribute("configStoreLoginId",Wizard.defaultUserName);
                ctx.setSessionAttribute(
                    SessionAttributeNames.USER_STORE_LOGIN_ID, val);

                val = getAttribute("rootSuffix", Wizard.defaultRootSuffix);
                ctx.setSessionAttribute(
                    SessionAttributeNames.USER_STORE_ROOT_SUFFIX, val);
            }

            ctx.setSessionAttribute(SessionAttributeNames.EXT_DATA_STORE,
                "true");
            ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_TYPE, "LDAPv3ForOpenDS");
        }

        String smsType = getAttribute(SetupConstants.CONFIG_VAR_DATA_STORE,
            "embedded");

        if (!smsType.equals("embedded")) {
            ctx.setSessionAttribute(
                SessionAttributeNames.EXT_DATA_STORE, "true");
            addModel("radioDataTypeDisabled", "disabled");
        } else {
            addModel("radioDataTypeDisabled", "");
        }

        String val = getAttribute(SetupConstants.USER_STORE_HOST,getHostName());
        ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_HOST, val);
        addModel("userStoreHost", val);

        val = getAttribute(SetupConstants.USER_STORE_SSL, "SIMPLE");
        ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_SSL, val);
        if (val.equals("SSL")) {
            addModel("selectUserStoreSSL", "checked=\"checked\"");
        } else {
            addModel("selectUserStoreSSL", "");
        }

        val = getAttribute(SetupConstants.USER_STORE_PORT, "389");
        ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_PORT, val);
        addModel("userStorePort", val);

        val = getAttribute(SetupConstants.USER_STORE_LOGIN_ID,
            Wizard.defaultUserName);
        ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_LOGIN_ID, val);
        addModel("userStoreLoginId", val);

        val = getAttribute(SetupConstants.USER_STORE_ROOT_SUFFIX,
            Wizard.defaultRootSuffix);
        ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_ROOT_SUFFIX,
            val);
        addModel("userStoreRootSuffix", val);

        val = getAttribute(SetupConstants.USER_STORE_TYPE, "LDAPv3ForOpenDS");
        if (val.equals("LDAPv3ForAD")) {
            addModel("selectLDAPv3ad", "checked=\"checked\"");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3odsee", "");
            addModel("selectLDAPv3opends", "");
            addModel("selectLDAPv3tivoli", "");
        } else if (val.equals("LDAPv3ForADDC")) {
            addModel("selectLDAPv3addc", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3odsee", "");
            addModel("selectLDAPv3opends", "");
            addModel("selectLDAPv3tivoli", "");
        } else if (val.equals("LDAPv3ForADAM")) {
            addModel("selectLDAPv3adam", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3odsee", "");
            addModel("selectLDAPv3opends", "");
            addModel("selectLDAPv3tivoli", "");
        } else if (val.equals("LDAPv3ForODSEE")) {
            // NOTE: pre-existing bug ported verbatim from the old Click page (verified against
            // source, see docs/migration/click-to-freemarker/04-implementation-notes.md): this
            // branch sets selectLDAPv3odsee to "checked" and then immediately overwrites it back
            // to "" instead of setting selectLDAPv3opends. Net effect (unchanged from Click):
            // neither the ODSEE nor OpenDS radio renders checked when the store type is ODSEE,
            // and selectLDAPv3opends is left unset. The template defaults these vars with !"" so
            // FreeMarker's strict undefined-variable check doesn't turn this cosmetic bug into a
            // 500 error.
            addModel("selectLDAPv3odsee", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3odsee", "");
            addModel("selectLDAPv3tivoli", "");
        } else if (val.equals("LDAPv3ForOpenDS")) {
            addModel("selectLDAPv3opends", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3odsee", "");
            addModel("selectLDAPv3tivoli", "");
        } else {
            addModel("selectLDAPv3tivoli", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3odsee", "");
            addModel("selectLDAPv3opends", "");
        }

        val = getAttribute("EXT_DATA_STORE", "true");
        addModel("EXT_DATA_STORE", val);
        if (val.equals("true")) {
            addModel("selectEmbeddedUM", "");
            addModel("selectExternalUM", "checked=\"checked\"");
        } else {
            addModel("selectEmbeddedUM", "checked=\"checked\"");
            addModel("selectExternalUM", "");
        }
    }

    // Not bound to any Click ActionLink and not called by step4.htm/step4.ftl in the original
    // page either - genuinely unreachable before and after this port, so left un-annotated
    // (no @ConfiguratorAction) to keep it that way. Kept only because it was pre-existing code.
    public boolean setAll() {
        skipRender();
        return false;
    }

    @ConfiguratorAction
    public boolean setSSL() {
        String ssl = toString("ssl");
        if ((ssl != null) && ssl.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_SSL, ssl);
        } else {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_SSL, "SIMPLE");
        }
        writeToResponse(getLocalizedString(responseString));
        return false;
    }

    @ConfiguratorAction
    public boolean setDomainName() {
        String domainname = toString("domainname");
        if ((domainname != null) && domainname.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_DOMAINNAME,
                domainname);
            getContext().setSessionAttribute(
                SessionAttributeNames.EXT_DATA_STORE, "true");
        } else {
            responseString = "missing.domain.name";
        }
        writeToResponse(getLocalizedString(responseString));
        return false;
    }

    @ConfiguratorAction
    public boolean setHost() {
        String host = toString("host");
        if ((host != null) && host.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_HOST, host);
        } else {
            responseString = "missing.host.name";
        }
        writeToResponse(getLocalizedString(responseString));
        return false;
    }

    @ConfiguratorAction
    public boolean setUMEmbedded() {
        getContext().setSessionAttribute(SessionAttributeNames.EXT_DATA_STORE,
            "false");
        skipRender();
        return false;
    }

    @ConfiguratorAction
    public boolean resetUMEmbedded() {
        getContext().setSessionAttribute(SessionAttributeNames.EXT_DATA_STORE,
            "true");
        skipRender();
        return false;
    }

    @ConfiguratorAction
    public boolean setPort() {
        String port = toString("port");

        if ((port != null) && port.length() > 0) {
            int intValue = Integer.parseInt(port);
            if ((intValue > 0) && (intValue < 65535)) {
                getContext().setSessionAttribute(
                    SessionAttributeNames.USER_STORE_PORT, port);
            } else {
                responseString = "invalid.port.number";
            }
        } else {
            responseString = "missing.host.port";
        }
        writeToResponse(getLocalizedString(responseString));
        return false;
    }

    @ConfiguratorAction
    public boolean setLoginID() {
        String dn = toString("dn");
        if ((dn != null) && dn.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_LOGIN_ID, dn);
        } else {
            responseString = "missing.login.id";
        }
        writeToResponse(getLocalizedString(responseString));
        return false;
    }

    @ConfiguratorAction
    public boolean setPassword() {
        String pwd = toString("password");
        if ((pwd != null) && pwd.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_LOGIN_PWD, pwd);
        } else {
            responseString = "missing.password";
        }
        writeToResponse(getLocalizedString(responseString));
        return false;
    }

    @ConfiguratorAction
    public boolean setRootSuffix() {
        String rootsuffix = toString("rootsuffix");

        if ((rootsuffix != null) && rootsuffix.length() > 0) {
            if (LDAPUtils.isDN(rootsuffix)) {
                getContext().setSessionAttribute(
                    SessionAttributeNames.USER_STORE_ROOT_SUFFIX, rootsuffix);
            } else {
                responseString = "invalid.dn";
            }
        } else {
            responseString = "missing.root.suffix";
        }
        writeToResponse(getLocalizedString(responseString));
        return false;
    }

    @ConfiguratorAction
    public boolean setStoreType() {
        String type = toString("type");
        if ((type != null) && type.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_TYPE, type);
        }
        writeToResponse(responseString);
        return false;
    }

    @ConfiguratorAction
    public boolean validateUMHost() {
        ConfiguratorContext ctx = getContext();
        String strSSL = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_SSL);
        boolean ssl = (strSSL != null) && (strSSL.equals("SSL"));

        String host = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_HOST);
        String strPort = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_PORT);
        int port = Integer.parseInt(strPort);
        String bindDN = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_LOGIN_ID);
        String rootSuffix = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_ROOT_SUFFIX);
        String bindPwd = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_LOGIN_PWD);

        try (Connection conn = getConnection(host, port, bindDN, bindPwd.toCharArray(), 5, ssl)) {
            //String filter = "cn=" + "\"" + rootSuffix + "\"";    // NOT SURE Why "cn" is specified. would never work.
            String[] attrs = {""};
            conn.search(LDAPRequests.newSearchRequest(rootSuffix, SearchScope.BASE_OBJECT, ObjectClassFilter, attrs));
            writeToResponse("ok");
        } catch (LdapException lex) {
            ResultCode resultCode = lex.getResult().getResultCode();
            if (!writeErrorToResponse(resultCode)) {
                writeToResponse(getLocalizedString("cannot.connect.to.SM.datastore"));
            }
        } catch (Exception e) {
            writeToResponse(getLocalizedString("cannot.connect.to.SM.datastore"));
        }

        return false;
    }

    @ConfiguratorAction
    public boolean validateUMDomainName() {
        ConfiguratorContext ctx = getContext();
        String strSSL = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_SSL);
        boolean ssl = (strSSL != null) && (strSSL.equals("SSL"));

        String domainName = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_DOMAINNAME);
        String rootSuffixAD = dnsDomainToDN(domainName);
        getContext().setSessionAttribute(
            SessionAttributeNames.USER_STORE_ROOT_SUFFIX,
            rootSuffixAD);
        String[] hostAndPort = {""};
        try {
            hostAndPort = getLdapHostAndPort(domainName);
        } catch (NamingException nex) {
            writeToResponse(
                getLocalizedString("cannot.connect.to.UM.datastore"));
            return false;
        } catch (IOException ioex) {
            writeToResponse(
                getLocalizedString("cannot.connect.to.UM.datastore"));
            return false;
        }
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        String bindDN = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_LOGIN_ID);
        String rootSuffix = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_ROOT_SUFFIX);
        String bindPwd = (String)ctx.getSessionAttribute(
            SessionAttributeNames.USER_STORE_LOGIN_PWD);

        try (Connection conn = getConnection(host, port, bindDN, bindPwd.toCharArray(), 3, ssl)) {
            //String filter = "cn=" + "\"" + rootSuffix + "\"";
            String[] attrs = {""};
            conn.search(LDAPRequests.newSearchRequest(rootSuffix, SearchScope.BASE_OBJECT, ObjectClassFilter, attrs));
            writeToResponse("ok");
        } catch (LdapException lex) {
            ResultCode resultCode = lex.getResult().getResultCode();
            if (!writeErrorToResponse(resultCode)) {
                writeToResponse(getLocalizedString("cannot.connect.to.UM.datastore"));
            }
        } catch (Exception e) {
            writeToResponse(getLocalizedString("cannot.connect.to.UM.datastore"));
        }
        return false;
    }

    // Method to get hostname and port number with the
    // provided Domain Name for Active Directory user data store.
    private String[] getLdapHostAndPort(String domainName)
        throws NamingException, IOException {
        if (!domainName.endsWith(".")) {
            domainName+='.';
        }
        DirContext ictx = null;
        // Check if domain name is a valid one.
        // The resource record type A is defined in RFC 1035.
        try {
            Hashtable env = new Hashtable();
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.dns.DnsContextFactory");
            ictx = new InitialDirContext(env);
            Attributes attributes =
                ictx.getAttributes(domainName, new String[]{"A"});
            Attribute attrib = attributes.get("A");
            if (attrib == null) {
                throw new NamingException();
            }
        } catch (NamingException e) {
            // Failed to resolve domainName to A record.
            // throw exception.
            throw e;
        }

        // then look for the LDAP server
        String serverHostName = null;
        String serverPortStr = null;
        final String ldapServer = "_ldap._tcp." + domainName;
        try {
            // Attempting to resolve ldapServer to SRV record.
            // This is a mechanism defined in MSDN, querying
            // SRV records for _ldap._tcp.DOMAINNAME.
            // and get host and port from domain.
            Attributes attributes =
                ictx.getAttributes(ldapServer, new String[]{"SRV"});
            Attribute attr = attributes.get("SRV");
            if (attr == null) {
                throw new NamingException();
            }
            String[] srv = attr.get().toString().split(" ");
            String hostNam = srv[3];
            serverHostName =
                hostNam.substring(0, hostNam.length() -1);
            if ((serverHostName != null) &&
                serverHostName.length() > 0) {
                getContext().setSessionAttribute(
                    SessionAttributeNames.USER_STORE_HOST,
                    serverHostName);
            }
            serverPortStr = srv[2];
        } catch (NamingException e) {
            // Failed to resolve ldapServer to SRV record.
            // throw exception.
            throw e;
        }

        // try to connect to LDAP port to make sure this machine
        // has LDAP service
        int serverPort = Integer.parseInt(serverPortStr);
        if ((serverPort > 0) && (serverPort < 65535)) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_PORT, serverPortStr);
        }
        try {
            new Socket(serverHostName, serverPort).close();
        } catch (IOException e) {
            throw e;
        }

        String[] hostAndPort = new String[2];
        hostAndPort[0] = serverHostName;
        hostAndPort[1] = serverPortStr;

        return hostAndPort;
    }

    // Method to convert the domain name to the root suffix.
    // eg., Domain Name amqa.test.com is converted to root suffix
    // DC=amqa,DC=test,DC=com
    static String dnsDomainToDN(String domainName) {
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if(token.length()==0)   continue;
            if(buf.length()>0)  buf.append(",");
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }
}
