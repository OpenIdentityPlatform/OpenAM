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
 * $Id: Step3.java,v 1.39 2009/12/17 17:43:39 goodearth Exp $
 *
 */

/*
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 */

package com.sun.identity.config.wizard;

import com.iplanet.am.util.SSLSocketFactoryManager;
import com.iplanet.services.util.Crypt;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.BootstrapData;
import com.sun.identity.setup.ConfiguratorException;
import com.sun.identity.setup.SetupConstants;
import java.util.Map;
import java.net.InetAddress;
import java.io.IOException;
import java.net.UnknownHostException;

import com.sun.identity.shared.Constants;
import org.apache.click.control.ActionLink;
import org.apache.click.Context;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.util.DN;

/**
 * Step 3 is for selecting the embedded or external configuration store 
 */
public class Step3 extends LDAPStoreWizardPage {

    public static final String LDAP_STORE_SESSION_KEY =
        "wizardCustomConfigStore";

    public ActionLink validateSMHostLink =
        new ActionLink("validateSMHost", this, "validateSMHost");
    public ActionLink validateRootSuffixLink = 
        new ActionLink("validateRootSuffix", this, "validateRootSuffix");
    public ActionLink setReplicationLink =
        new ActionLink("setReplication", this, "setReplication");
    public ActionLink validateHostNameLink = 
        new ActionLink("validateHostName", this, "validateHostName");
    public ActionLink validateConfigStoreHost =
        new ActionLink("validateConfigStoreHost", this, "validateConfigStoreHost");    
    public ActionLink setConfigType = 
        new ActionLink("setConfigType", this, "setConfigType");
    public ActionLink validateLocalPortLink = 
        new ActionLink("validateLocalPort", this, "validateLocalPort");
    public ActionLink validateLocalAdminPortLink =
        new ActionLink("validateLocalAdminPort", this, "validateLocalAdminPort");
    public ActionLink validateLocalJmxPortLink =
        new ActionLink("validateLocalJmxPort", this, "validateLocalJmxPort");
    public ActionLink validateEncKey =
        new ActionLink("validateEncKey", this, "validateEncKey");
    
    private static final String QUOTE = "\"";
    private static final String SEPARATOR = "\" : \"";
    private String localRepPort;
    
    public Step3() {
    }
    
    public void onInit() {
        String val = getAttribute("rootSuffix", Wizard.defaultRootSuffix);
        addModel("rootSuffix", val);

        val = getAttribute("encryptionKey", AMSetupServlet.getRandomString());
        addModel("encryptionKey", val);
        
        val = getAttribute("configStorePort", getAvailablePort(50389));
        addModel("configStorePort", val);
        addModel("localConfigPort", val);

        val = getAttribute("configStoreAdminPort", getAvailablePort(4444));
        addModel("configStoreAdminPort", val);
        addModel("localConfigAdminPort", val);

        val = getAttribute("configStoreJmxPort", getAvailablePort(1689));
        addModel("configStoreJmxPort", val);
        addModel("localConfigJmxPort", val);

        localRepPort = getAttribute("localRepPort", getAvailablePort(58989));
        addModel("localRepPort", localRepPort);

        val = getAttribute("existingPort", getAvailablePort(50389));        
        addModel("existingPort", val);

        val = getAttribute("existingRepPort", getAvailablePort(58990));
        addModel("existingRepPort", val);

        val = getAttribute("configStoreSSL", "SIMPLE");
        addModel("configStoreSSL", val);
        
        if (val.equals("SSL")) {
            addModel("selectConfigStoreSSL", "checked=\"checked\"");
        } else {
            addModel("selectConfigStoreSSL", "");
        }

        // initialize the data store type being used
        val = getAttribute(
            SetupConstants.CONFIG_VAR_DATA_STORE, 
            SetupConstants.SMS_EMBED_DATASTORE);
        addModel(SetupConstants.CONFIG_VAR_DATA_STORE, val);
               
        if (val.equals(SetupConstants.SMS_EMBED_DATASTORE)) {
            addModel("selectEmbedded", "checked=\"checked\"");
            addModel("selectExternal", "");
        } else {
            addModel("selectEmbedded", "");
            addModel("selectExternal", "checked=\"checked\"");
        }

        val = getAttribute("configStoreHost", "localhost");
        addModel("configStoreHost", val);

        val = getAttribute("configStorePassword", Wizard.defaultPassword);
        addModel("configStorePassword", val);

        val = getAttribute("configStoreLoginId", Wizard.defaultUserName);
        addModel("configStoreLoginId", val);

         val = getAttribute(SetupConstants.DS_EMB_REPL_FLAG, "");
         if (val.equals(SetupConstants.DS_EMP_REPL_FLAG_VAL)) {
             addModel("FIRST_INSTANCE", "1");
             addModel("selectFirstSetup", "");
             addModel("selectExistingSetup", "checked=\"checked\"");
         } else {
             addModel("FIRST_INSTANCE", "0");
             addModel("selectFirstSetup", "checked=\"checked\"");
             addModel("selectExistingSetup", "");
         }

        super.onInit();
    }       

    public boolean setConfigType() {
        String type = toString("type");
        if (type.equals("remote")) {
            type = SetupConstants.SMS_DS_DATASTORE;
        } else {
            type = SetupConstants.SMS_EMBED_DATASTORE;
            getContext().setSessionAttribute(
                    SessionAttributeNames.CONFIG_STORE_HOST, "localhost");
        }
        getContext().setSessionAttribute( 
            SessionAttributeNames.CONFIG_VAR_DATA_STORE, type);
        return true;
    }

    public boolean setReplication() {
        String type = toString("multi");
        if (type.equals("enable")) {
            type = SetupConstants.DS_EMP_REPL_FLAG_VAL;
        } 
        getContext().setSessionAttribute(
            SessionAttributeNames.DS_EMB_REPL_FLAG, type);
        return true;
    }

    public boolean validateRootSuffix() {
        String rootsuffix = toString("rootSuffix");
        if ((rootsuffix == null) || (rootsuffix.trim().length() == 0)) {
            writeToResponse(getLocalizedString("missing.required.field"));
        }
        // Determine if we have the minimal number of high order naming attributes.
        String[] containers = rootsuffix.split(Constants.COMMA);
        int namedDomainContainers = 0;
        int namedOrganizationContainers = 0;
        for(String container : containers)
        {
            if (container.startsWith(Constants.DEFAULT_ROOT_NAMING_ATTRIBUTE+Constants.EQUALS))
                { namedDomainContainers++; }
            else if (container.startsWith(Constants.ORGANIZATION_NAMING_ATTRIBUTE+Constants.EQUALS))
                { namedOrganizationContainers++; }
        }
        if ((namedDomainContainers+namedOrganizationContainers) <= 1) {
            writeToResponse(getLocalizedString("invalid.naming.suffix"));
        } else if (!DN.isDN(rootsuffix)) {
            writeToResponse(getLocalizedString("invalid.dn"));
        } else if (!rootsuffix.startsWith(Constants.DEFAULT_ROOT_NAMING_ATTRIBUTE+Constants.EQUALS)) {
                writeToResponse(getLocalizedString("invalid.naming.attribute"));
        } else {
            writeToResponse("true");
            getContext().setSessionAttribute(
                SessionAttributeNames.CONFIG_STORE_ROOT_SUFFIX, rootsuffix);
        }
        setPath(null);
        return false;    
    }
    
    public boolean validateLocalPort() {
        String port = toString("port");
        
        if (port == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else {
            try {
                int val = Integer.parseInt(port);
                if (val < 1 || val > 65535) {
                    writeToResponse(getLocalizedString("invalid.port.number"));
                } else {
                    boolean ok = false;
                    String type = (String) getContext().getSessionAttribute(
                            SetupConstants.CONFIG_VAR_DATA_STORE);

                    if ((type == null) || type.equals(
                            SetupConstants.SMS_EMBED_DATASTORE)) {
                        String host = (String) getContext().getSessionAttribute(
                                "configStoreHost");
                        if (host == null) {
                            host = "localhost";
                        }
                        if (AMSetupServlet.canUseAsPort(host, val)) {
                            ok = true;
                        } else {
                            writeToResponse(getLocalizedString("invalid.port.used"));
                        }
                    } else {
                        ok = true;
                    }
                    if (ok) {
                        getContext().setSessionAttribute("configStorePort", port);
                        writeToResponse("ok");
                    }
                }

            } catch (NumberFormatException e) {
                 writeToResponse(getLocalizedString("invalid.port.number"));
            } catch (NullPointerException ne) {
                writeToResponse(getLocalizedString("invalid.port.number"));
            }
        }
        setPath(null);        
        return false;    
    }

        public boolean validateLocalAdminPort() {
        String port = toString("port");

        if (port == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else {
            try {
                int val = Integer.parseInt(port);
                if (val < 1 || val > 65535) {
                    writeToResponse(getLocalizedString("invalid.port.number"));
                } else {
                    boolean ok = false;
                    String type = (String) getContext().getSessionAttribute(
                            SetupConstants.CONFIG_VAR_DATA_STORE);

                    if ((type == null) || type.equals(
                            SetupConstants.SMS_EMBED_DATASTORE)) {
                        String host = (String) getContext().getSessionAttribute(
                                "configStoreHost");
                        if (host == null) {
                            host = "localhost";
                        }
                        if (AMSetupServlet.canUseAsPort(host, val)) {
                            ok = true;
                        } else {
                            writeToResponse(getLocalizedString("invalid.port.used"));
                        }
                    } else {
                        ok = true;
                    }
                    if (ok) {
                        getContext().setSessionAttribute("configStoreAdminPort", port);
                        writeToResponse("ok");
                    }
                }

            } catch (NumberFormatException e) {
                 writeToResponse(getLocalizedString("invalid.port.number"));
            } catch (NullPointerException ne) {
                writeToResponse(getLocalizedString("invalid.port.number"));
            }
        }
        setPath(null);
        return false;
    }

    public boolean validateLocalJmxPort() {
        String port = toString("port");

        if (port == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else {
            try {
                int val = Integer.parseInt(port);
                if (val < 1 || val > 65535) {
                    writeToResponse(getLocalizedString("invalid.port.number"));
                } else {
                    boolean ok = false;
                    String type = (String) getContext().getSessionAttribute(
                            SetupConstants.CONFIG_VAR_DATA_STORE);

                    if ((type == null) || type.equals(
                            SetupConstants.SMS_EMBED_DATASTORE)) {
                        String host = (String) getContext().getSessionAttribute(
                                "configStoreHost");
                        if (host == null) {
                            host = "localhost";
                        }
                        if (AMSetupServlet.canUseAsPort(host, val)) {
                            ok = true;
                        } else {
                            writeToResponse(getLocalizedString("invalid.port.used"));
                        }
                    } else {
                        ok = true;
                    }
                    if (ok) {
                        getContext().setSessionAttribute("configStoreJmxPort", port);
                        writeToResponse("ok");
                    }
                }

            } catch (NumberFormatException e) {
                 writeToResponse(getLocalizedString("invalid.port.number"));
            } catch (NullPointerException ne) {
                writeToResponse(getLocalizedString("invalid.port.number"));
            }
        }
        setPath(null);
        return false;
    }

    /**
     * Returns <code>false</code> always. Length of encryption key
     * must be at least 10 chars.
     */
    public boolean validateEncKey() {
        String key = toString("encKey");

        if (key == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else {
            getContext().setSessionAttribute(
                SessionAttributeNames.ENCRYPTION_KEY, key);
            if (key.length() < 10) {
                writeToResponse(getLocalizedString("enc.key.need.10.chars"));
            } else {
                writeToResponse("true");
            }
        }
        setPath(null);
        return false;
    } 

    
   public boolean validateConfigStoreHost() {
       String host = toString("configStoreHost");

       if (host == null) {
           writeToResponse(
                getLocalizedString("missing.required.field"));
           setPath(null);
           return false;
       } else {
           getContext().setSessionAttribute(
               "configStoreHost", host);
       }

       try {
           InetAddress address = InetAddress.getByName(host);
           if (address.isReachable(300)) {
               writeToResponse("ok");
           } else {
               writeToResponse(getLocalizedString("contact.host.failed"));
           }
       } catch (UnknownHostException uhe) {
           writeToResponse(getLocalizedString("contact.host.unknown"));
       } catch (IOException ioe) {
           writeToResponse(getLocalizedString("contact.host.unreachable"));
       } catch (NullPointerException ne) {
           writeToResponse(getLocalizedString("contact.host.failed"));           
       }
       setPath(null);
       return false;
   }
   
   
    /*
     * a call is made to the OpenAM url entered in the browser. If
     * the OpenAM server
     * exists a <code>Map</code> of data will be returned which contains the
     * information about the existing servers data store, including any 
     * replication ports if its embedded.
     * Information to control the UI is returned in a JSON object of the form
     * { 
     *   "param1" : "value1", 
     *   "param2" : "value2"
     * }
     * The JS on the browser will interpret the above and make the necessary
     * changes to prompt the user for any more details required.
     */
    public boolean validateHostName() {
        StringBuffer sb = new StringBuffer();
        String hostName = toString("hostName");
        
        if (hostName == null) {            
            addObject(sb, "code", "100");
            addObject(sb, "message",
                getLocalizedString("missing.required.field"));
        } else {
            // try to retrieve the remote OpenAM information
            String admin = "amadmin";
            String password = (String)getContext().getSessionAttribute(
                SessionAttributeNames.CONFIG_VAR_ADMIN_PWD);
            
            try { 
                String dsType;
                Map data = AMSetupServlet.getRemoteServerInfo(
                    hostName, admin, password);
                
                // data returned from existing OpenAM server
                if (data != null && !data.isEmpty()) {                    
                    addObject(sb, "code", "100");
                    addObject(sb, "message", getLocalizedString("ok.string"));
                    
                    setupDSParams(data);
                    
                    String key = (String)data.get("enckey");
                    getContext().setSessionAttribute(
                        SessionAttributeNames.ENCRYPTION_KEY, key);

                    getContext().setSessionAttribute(
                        SessionAttributeNames.ENCLDAPUSERPASSWD,
                        (String)data.get("ENCLDAPUSERPASSWD"));
                    
                    // true for embedded, false for ODSEE
                    String embedded = 
                        (String)data.get(BootstrapData.DS_ISEMBEDDED);
                    addObject(sb, "embedded", embedded);           
                    String host = (String)data.get(BootstrapData.DS_HOST);

                    if (embedded.equals("true")) {
                        getContext().setSessionAttribute(
                            SessionAttributeNames.CONFIG_STORE_HOST, getHostName());
                        addObject(sb, "configStoreHost", getHostName());
                        
                        // set the multi embedded flag 
                        getContext().setSessionAttribute(
                            SessionAttributeNames.CONFIG_VAR_DATA_STORE, 
                            SetupConstants.SMS_EMBED_DATASTORE); 
                        
                        getContext().setSessionAttribute(
                            SessionAttributeNames.DS_EMB_REPL_FLAG,
                            SetupConstants.DS_EMP_REPL_FLAG_VAL); 
      
                        // get the existing replication ports if any
                        String replAvailable = (String)data.get(
                            BootstrapData.DS_REPLICATIONPORT_AVAILABLE);
                        if (replAvailable == null) {
                            replAvailable = "false";
                        }
                        addObject(sb, "replication", replAvailable);
                        String existingRep = (String)data.get(
                            BootstrapData.DS_REPLICATIONPORT);
                        getContext().setSessionAttribute(
                            SessionAttributeNames.EXISTING_REPL_PORT,
                            existingRep);
                        addObject(sb, "replicationPort", existingRep);

                        String existingServerid = (String)data.get(
                            "existingserverid");
                        getContext().setSessionAttribute(
                            SessionAttributeNames.EXISTING_SERVER_ID,
                            existingServerid);
                        addObject(sb, "existingserverid", existingServerid);

                        // dsmgr password is same as amadmin for embedded
                        getContext().setSessionAttribute(
                            SessionAttributeNames.CONFIG_STORE_PWD, password);
                    } else {
                        getContext().setSessionAttribute(
                            SessionAttributeNames.CONFIG_STORE_PORT, 
                            (String)data.get(BootstrapData.DS_PORT));
                        getContext().setSessionAttribute(
                            SessionAttributeNames.CONFIG_STORE_HOST, host);   
                        addObject(sb, "configStoreHost", host);

                        String dsprot = (String)data.get(
                            BootstrapData.DS_PROTOCOL);
                        String dsSSL = ("ldaps".equals(dsprot)) ?
                           "SSL" : "SIMPLE";
                        getContext().setSessionAttribute(
                            SessionAttributeNames.CONFIG_STORE_SSL, dsSSL);
                        addObject(sb, "configStoreSSL", dsSSL);
                        
                        String dspwd = (String)data.get(BootstrapData.DS_PWD);
                        getContext().setSessionAttribute(
                            SessionAttributeNames.CONFIG_STORE_PWD,
                            Crypt.decode(dspwd, Crypt.getHardcodedKeyEncryptor()
                            ));
                    }

                    // set the replication ports pulled from the remote
                    // server in the session and pass back to the client
                    String existing = (String)data.get(
                        SetupConstants.DS_EMB_REPL_ADMINPORT2);
                    getContext().setSessionAttribute(
                        SessionAttributeNames.EXISTING_PORT, existing);
                    addObject(sb, "existingPort", existing);

                    // set the configuration store port
                    String ds_existingStorePort = (String)data.get(BootstrapData.DS_PORT);
                    getContext().setSessionAttribute(
                            SessionAttributeNames.EXISTING_STORE_PORT, ds_existingStorePort);
                    addObject(sb, "existingStorePort", ds_existingStorePort);
                    
                    getContext().setSessionAttribute(
                        SessionAttributeNames.EXISTING_HOST, host);

                    // set the configuration store host
                    getContext().setSessionAttribute(
                        SessionAttributeNames.EXISTING_STORE_HOST, host);
                    addObject(sb, "existingStoreHost", host);

                    // set the configuration store port
                    getContext().setSessionAttribute(
                        SessionAttributeNames.LOCAL_REPL_PORT, localRepPort);
                }
            } catch (ConfigurationException c) {
                String code = c.getErrorCode();
                String message = getLocalizedString(code);
                if (code == null) {
                    code = "999";
                    message = c.getMessage();
                }
                addObject(sb, "code", code);
                addObject(sb, "message", message);
            } catch (ConfiguratorException c) {
                String code = c.getErrorCode();
                String message = getLocalizedString(code);
                if (code == null) {
                    code = "999";
                    message = c.getMessage();
                }
                addObject(sb, "code", code);
                addObject(sb, "message", message);
            }
        }
        sb.append(" }");           
        writeToResponse(sb.toString());
        setPath(null);
        return false;
    }
        
    private void addObject(StringBuffer sb, String key, String value) {
        if (sb.length() < 1) {
            // add first object
            sb.append("{ ");
        } else {
            sb.append(",");
        }
        sb.append(QUOTE)
          .append(key)
          .append(SEPARATOR)
          .append(value)
          .append(QUOTE);                         
    }
    
    /*
     * the following value have been pulled from an existing OpenAM
     * server which was configured to use an external DS. We need to set the DS 
     * values in the request so they can be used to configure the existing
     * OpenAM server.
     */
    private void setupDSParams(Map data) {             
        String tmp = (String)data.get(BootstrapData.DS_BASE_DN);
        getContext().setSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_ROOT_SUFFIX, tmp);

        tmp = (String)data.get(BootstrapData.DS_MGR);
        getContext().setSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_LOGIN_ID, tmp);
        
        tmp = (String)data.get(BootstrapData.DS_PWD);
        getContext().setSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_PWD, tmp);
       
        getContext().setSessionAttribute(
            SessionAttributeNames.CONFIG_VAR_DATA_STORE, 
            SetupConstants.SMS_DS_DATASTORE);    
    }

    public boolean validateSMHost() {
        Context ctx = getContext();
        String strSSL = (String)ctx.getSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_SSL);
        boolean ssl = (strSSL != null) && (strSSL.equals("SSL"));

        String host = (String)ctx.getSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_HOST);
        if (host == null) {
            host = "localhost";
        }

        String strPort = (String)ctx.getSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_PORT);
        if (strPort == null) {
            strPort = getAvailablePort(50389);
        }

        int port = Integer.parseInt(strPort);

        String bindDN = (String)ctx.getSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_LOGIN_ID);
        String rootSuffix = (String)ctx.getSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_ROOT_SUFFIX);
        String bindPwd = (String)ctx.getSessionAttribute(
            SessionAttributeNames.CONFIG_STORE_PWD);

        if (bindDN == null) {
            bindDN = "cn=Directory Manager";
        }
        if (rootSuffix == null) {
            rootSuffix = Constants.DEFAULT_ROOT_SUFFIX;
        }

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
}
