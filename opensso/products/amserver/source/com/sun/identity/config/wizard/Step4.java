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
 * $Id: Step4.java,v 1.20 2009/10/27 05:31:45 hengming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */

package com.sun.identity.config.wizard;
import com.iplanet.am.util.SSLSocketFactoryManager;
import com.sun.identity.config.SessionAttributeNames;
import org.apache.click.control.ActionLink;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.setup.SetupConstants;
import org.apache.click.Context;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.util.DN;

import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * Step 4 is the input of the remote user data store properties.
 */
public class Step4 extends AjaxPage {
    public static final String LDAP_STORE_SESSION_KEY = "wizardCustomUserStore";
    public ActionLink validateUMHostLink = 
        new ActionLink("validateUMHost", this, "validateUMHost");
    public ActionLink validateUMDomainNameLink = 
        new ActionLink("validateUMDomainName", this, 
            "validateUMDomainName");
    public ActionLink setSSLLink = 
        new ActionLink("setSSL", this, "setSSL");
    public ActionLink setUMEmbedded = 
        new ActionLink("setUMEmbedded", this, "setUMEmbedded");
    public ActionLink resetUMEmbedded = 
        new ActionLink("resetUMEmbedded", this, "resetUMEmbedded");
    public ActionLink setHostLink = 
        new ActionLink("setHost", this, "setHost");
    public ActionLink setDomainNameLink = 
        new ActionLink("setDomainName", this, "setDomainName");
    public ActionLink setPortLink = 
        new ActionLink("setPort", this, "setPort");
    public ActionLink setRootSuffixLink = 
        new ActionLink("setRootSuffix", this, "setRootSuffix");
    public ActionLink setLoginIDLink = 
        new ActionLink("setLoginID", this, "setLoginID");
    public ActionLink setPasswordLink = 
        new ActionLink("setPassword", this, "setPassword");
    public ActionLink setStoreTypeLink = 
        new ActionLink("setStoreType", this, "setStoreType");    

    private String responseString = "ok";
    
    public Step4() {
    }
    
    public void onInit() {
        super.onInit();
        Context ctx = getContext();
        
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
            ctx.setSessionAttribute(SessionAttributeNames.USER_STORE_TYPE,
                "LDAPv3ForSUNDS");
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

        val = getAttribute(SetupConstants.USER_STORE_TYPE, "LDAPv3ForSUNDS");
        if (val.equals("LDAPv3ForAD")) {
            addModel("selectLDAPv3ad", "checked=\"checked\"");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3sunds", "");
            addModel("selectLDAPv3opends", "");
            addModel("selectLDAPv3tivoli", "");
        } else if (val.equals("LDAPv3ForADDC")) {
            addModel("selectLDAPv3addc", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3sunds", "");
            addModel("selectLDAPv3opends", "");
            addModel("selectLDAPv3tivoli", "");
        } else if (val.equals("LDAPv3ForADAM")) {
            addModel("selectLDAPv3adam", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3sunds", "");
            addModel("selectLDAPv3opends", "");
            addModel("selectLDAPv3tivoli", "");
        } else if (val.equals("LDAPv3ForSUNDS")) {
            addModel("selectLDAPv3sunds", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3opends", "");
            addModel("selectLDAPv3tivoli", "");
        } else if (val.equals("LDAPv3ForOpenDS")) {
            addModel("selectLDAPv3opends", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3sunds", "");
            addModel("selectLDAPv3tivoli", "");
        } else {
            addModel("selectLDAPv3tivoli", "checked=\"checked\"");
            addModel("selectLDAPv3ad", "");
            addModel("selectLDAPv3addc", "");
            addModel("selectLDAPv3adam", "");
            addModel("selectLDAPv3sunds", "");
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
    
    public boolean setAll() {     
        setPath(null);
        return false;
    }
    
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
        setPath(null);
        return false;
    }

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
        setPath(null);
        return false;
    }

    public boolean setHost() {
        String host = toString("host");
        if ((host != null) && host.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_HOST, host);
        } else {
            responseString = "missing.host.name";            
        }
        writeToResponse(getLocalizedString(responseString));
        setPath(null);
        return false;
    }

    public boolean setUMEmbedded() {
        getContext().setSessionAttribute(SessionAttributeNames.EXT_DATA_STORE,
            "false");
        setPath(null);
        return false;
    }

    public boolean resetUMEmbedded() {
        getContext().setSessionAttribute(SessionAttributeNames.EXT_DATA_STORE,
            "true");
        setPath(null);
        return false;
    }
        
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
        setPath(null);
        return false;
    }
    
    public boolean setLoginID() {
        String dn = toString("dn");
        if ((dn != null) && dn.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_LOGIN_ID, dn);
        } else {
            responseString = "missing.login.id";            
        }
        writeToResponse(getLocalizedString(responseString));
        setPath(null);
        return false;
    }
    
    public boolean setPassword() {
        String pwd = toString("password");
        if ((pwd != null) && pwd.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_LOGIN_PWD, pwd);
        } else {
            responseString = "missing.password";            
        }
        writeToResponse(getLocalizedString(responseString));
        setPath(null);
        return false;
    }
    
    public boolean setRootSuffix() {
        String rootsuffix = toString("rootsuffix");

        if ((rootsuffix != null) && rootsuffix.length() > 0) {
            if (DN.isDN(rootsuffix)) {
                getContext().setSessionAttribute(
                    SessionAttributeNames.USER_STORE_ROOT_SUFFIX, rootsuffix);
            } else {
                responseString = "invalid.dn";            
            }
        } else {
            responseString = "missing.root.suffix";            
        }
        writeToResponse(getLocalizedString(responseString));
        setPath(null);
        return false;
    }
    
    public boolean setStoreType() {
        String type = toString("type");
        if ((type != null) && type.length() > 0) {
            getContext().setSessionAttribute(
                SessionAttributeNames.USER_STORE_TYPE, type);
        } 
        writeToResponse(responseString);
        setPath(null);
        return false;
    }
    
    public boolean validateUMHost() {
        Context ctx = getContext();
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
        
        LDAPConnection ld = null;
        try {
            ld = (ssl) ? new LDAPConnection(
                SSLSocketFactoryManager.getSSLSocketFactory()) :
                new LDAPConnection();
            ld.setConnectTimeout(5);
            ld.connect(3, host, port, bindDN, bindPwd);
            
            String filter = "cn=" + "\"" + rootSuffix + "\"";
            String[] attrs = {""};
            ld.search(rootSuffix, LDAPConnection.SCOPE_BASE, filter, 
                attrs, false);
            writeToResponse("ok");
        } catch (LDAPException lex) {
            switch (lex.getLDAPResultCode()) {
                case LDAPException.CONNECT_ERROR:
                    writeToResponse(getLocalizedString("ldap.connect.error")); 
                    break;
                case LDAPException.SERVER_DOWN:
                    writeToResponse(getLocalizedString("ldap.server.down"));   
                    break;
                case LDAPException.INVALID_DN_SYNTAX:
                    writeToResponse(getLocalizedString("ldap.invalid.dn"));  
                    break;
                case LDAPException.NO_SUCH_OBJECT:
                    writeToResponse(getLocalizedString("ldap.nosuch.object"));
                    break;
                case LDAPException.INVALID_CREDENTIALS:
                    writeToResponse(
                            getLocalizedString("ldap.invalid.credentials"));
                    break;
                case LDAPException.UNWILLING_TO_PERFORM:
                    writeToResponse(getLocalizedString("ldap.unwilling"));
                    break;
                case LDAPException.INAPPROPRIATE_AUTHENTICATION:
                    writeToResponse(getLocalizedString("ldap.inappropriate"));
                    break;
                case LDAPException.CONSTRAINT_VIOLATION:
                    writeToResponse(getLocalizedString("ldap.constraint"));
                    break;
                default:
                    writeToResponse(
                        getLocalizedString("cannot.connect.to.SM.datastore"));                                              
            }           
        } catch (Exception e) {
            writeToResponse(
                getLocalizedString("cannot.connect.to.SM.datastore"));
        } finally {
            if (ld != null) {
                try {
                    ld.disconnect();
                } catch (LDAPException ex) {
                    //ignore
                }
            }
        }

        
        setPath(null);
        return false;
    }

    public boolean validateUMDomainName() {
        setPath(null);
        Context ctx = getContext();
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
        
        LDAPConnection ld = null;
        try {
            ld = (ssl) ? new LDAPConnection(
                SSLSocketFactoryManager.getSSLSocketFactory()) :
                new LDAPConnection();
            ld.setConnectTimeout(5);
            ld.connect(3, host, port, bindDN, bindPwd);
            
            String filter = "cn=" + "\"" + rootSuffix + "\"";
            String[] attrs = {""};
            ld.search(rootSuffix, LDAPConnection.SCOPE_BASE, filter, 
                attrs, false);
            writeToResponse("ok");
        } catch (LDAPException lex) {
            switch (lex.getLDAPResultCode()) {
                case LDAPException.CONNECT_ERROR:
                    writeToResponse(getLocalizedString(
                        "ldap.connect.error")); 
                    break;
                case LDAPException.SERVER_DOWN:
                    writeToResponse(getLocalizedString(
                        "ldap.server.down"));   
                    break;
                case LDAPException.INVALID_DN_SYNTAX:
                    writeToResponse(getLocalizedString(
                        "ldap.invalid.dn"));  
                    break;
                case LDAPException.NO_SUCH_OBJECT:
                    writeToResponse(getLocalizedString(
                        "ldap.nosuch.object"));
                    break;
                case LDAPException.INVALID_CREDENTIALS:
                    writeToResponse(getLocalizedString(
                        "ldap.invalid.credentials"));
                    break;
                case LDAPException.UNWILLING_TO_PERFORM:
                    writeToResponse(getLocalizedString(
                        "ldap.unwilling"));
                    break;
                case LDAPException.INAPPROPRIATE_AUTHENTICATION:
                    writeToResponse(getLocalizedString(
                        "ldap.inappropriate"));
                    break;
                case LDAPException.CONSTRAINT_VIOLATION:
                    writeToResponse(getLocalizedString(
                        "ldap.constraint"));
                    break;
                default:
                    writeToResponse(getLocalizedString(
                        "cannot.connect.to.UM.datastore"));
            }           
        } catch (Exception e) {
            writeToResponse(getLocalizedString(
                "cannot.connect.to.UM.datastore"));
        } finally {
            if (ld != null) {
                try {
                    ld.disconnect();
                } catch (LDAPException ex) {
                    //ignore
                }
            }
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
