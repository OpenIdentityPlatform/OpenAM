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
 * $Id: AjaxPage.java,v 1.24 2010/01/04 19:15:16 veiming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.config.util;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.*;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.AMSetupUtils;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.click.Page;
import org.apache.click.control.ActionLink;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;

public abstract class AjaxPage extends Page {

    public ActionLink checkPasswordsLink = 
        new ActionLink("checkPasswords", this, "checkPasswords");
    
    public ActionLink validateInputLink =
        new ActionLink("validateInput", this, "validateInput" );
    
    public ActionLink resetSessionAttributesLink =
        new ActionLink("resetSessionAttributes", this,
        "resetSessionAttributes");
    
    public static final String RESPONSE_TEMPLATE = "{\"valid\":\"${valid}\", \"body\":\"${body}\"}";
    public static final String OLD_RESPONSE_TEMPLATE = "{\"isValid\":${isValid}, \"errorMessage\":\"${errorMessage}\"}";
   
    private static int MIN_PASSWORD_SIZE = 8;
    private boolean rendering = false;
    private String hostName;
    protected java.util.Locale configLocale = null;
    
    // localization properties
    protected ResourceBundle rb = null;
    protected static final String RB_NAME = "amConfigurator";
    
    public String responseString = "true";
    public static Debug debug = Debug.getInstance("amConfigurator");
    
    public AjaxPage() {
    }

    @Override
    public void onInit() {
        super.onInit();
        initializeResourceBundle();
        addModel("page", this);
    }

    public boolean isRendering() {
        return rendering;
    }

    protected String toString( String paramName ) {
        String value = getContext().getRequest().getParameter( paramName );
        value = ( value != null ? value.trim() : null );
        value = ("".equals(value) ? null : value );
        return value;
    }

    protected boolean toBoolean(String paramName) {
        String value = toString(paramName);
        if (value == null) {
            return false;
        }
        if ((value.equalsIgnoreCase("on")) ||
            (value.equalsIgnoreCase("true")) ||
            (value.equalsIgnoreCase("yes")) ||
            (value.equalsIgnoreCase("checked"))) {
            return true;
        } else {
            return false;
        }
    }

    protected int toInt( String paramName ) {
        int intValue = 0;
        String value = toString( paramName );
        if ( value != null ) {
            try {
                intValue = Integer.parseInt( value );
            } catch ( NumberFormatException e ) {}
        }
        return intValue;
    }

    protected void writeValid() {
        writeValid( null );
    }
    protected void writeValid( String message ) {
        String out = ( message != null ? message : "" );
        writeJsonResponse(true, out);
    }
    protected void writeInvalid( String message ) {
        String out = ( message != null ? message : "" );
        writeJsonResponse( false, out );
    }

    protected void writeJsonResponse(String valid, String responseBody) {
        String response = RESPONSE_TEMPLATE;
        response = response.replaceFirst("\\$\\{" + "valid" +  "\\}", valid);
        response = response.replaceFirst("\\$\\{" + "body" +  "\\}", responseBody);
        writeToResponse(response);
    }
    protected void writeJsonResponse(boolean valid, String responseBody) {
        String response = RESPONSE_TEMPLATE;
        response = response.replaceFirst("\\$\\{" + "valid" +  "\\}", String.valueOf(valid));
        response = response.replaceFirst("\\$\\{" + "body" +  "\\}", responseBody);
        writeToResponse(response);
    }

    protected Connection getConnection(String host, int port, String bindDN, char[] bindPwd, int timeout, boolean isSSl)
            throws GeneralSecurityException, LdapException {
        Options ldapOptions = Options.defaultOptions()
                .set(CONNECT_TIMEOUT, new Duration((long)timeout, TimeUnit.SECONDS))
                .set(AUTHN_BIND_REQUEST, LDAPRequests.newSimpleBindRequest(bindDN, bindPwd));

        if (isSSl) {
            String defaultProtocolVersion = SystemProperties.get(Constants.LDAP_SERVER_TLS_VERSION, "TLS");
            ldapOptions = ldapOptions.set(SSL_CONTEXT,
                    new SSLContextBuilder().setProtocol(defaultProtocolVersion).getSSLContext());
        }

        ConnectionFactory factory = new LDAPConnectionFactory(host, port, ldapOptions);
        return factory.getConnection();
    }

    protected boolean writeErrorToResponse(ResultCode resultCode) {
        String msg = getMessage(resultCode);
        if (msg != null) {
            writeToResponse(getLocalizedString(msg));
            return true;
        }
        return false;
    }

    private String getMessage(ResultCode resultCode) {
        if (ResultCode.CLIENT_SIDE_CONNECT_ERROR.equals(resultCode)) {
            return "ldap.connect.error";
        } else if (ResultCode.CLIENT_SIDE_SERVER_DOWN.equals(resultCode)) {
            return "ldap.server.down";
        } else if (ResultCode.INVALID_DN_SYNTAX.equals(resultCode)) {
            return "ldap.invalid.dn";
        } else if (ResultCode.NO_SUCH_OBJECT.equals(resultCode)) {
            return "ldap.nosuch.object";
        } else if (ResultCode.INVALID_CREDENTIALS.equals(resultCode)) {
            return "ldap.invalid.credentials";
        } else if (ResultCode.UNWILLING_TO_PERFORM.equals(resultCode)) {
            return "ldap.unwilling";
        } else if (ResultCode.INAPPROPRIATE_AUTHENTICATION.equals(resultCode)) {
            return "ldap.inappropriate";
        } else if (ResultCode.CONSTRAINT_VIOLATION.equals(resultCode)) {
            return "ldap.constraint";
        } else {
            return null;
        }
    }

    protected void writeToResponse( String text ) {
        try {
            // Note: this writer is obtained from the Apache Click context. Should this be triggered by a JSP this
            // may not be in compliance with the JSP spec regarding mandatory use of the buffered JspWriter.
            getContext().getResponse().getWriter().write( text );
            this.rendering = true;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void initializeResourceBundle() {
        HttpServletRequest req = 
            (HttpServletRequest)getContext().getRequest();
        HttpServletResponse res = 
            (HttpServletResponse)getContext().getResponse();

        setLocale(req);
        res.setContentType("text/html; charset=UTF-8");
    }

    private void setLocale (HttpServletRequest request) {
        if (request != null) {
            String superLocale = request.getParameter("locale");
            if (superLocale != null && superLocale.length() > 0) {
                configLocale = Locale.getLocaleObjFromAcceptLangHeader(
                    superLocale); 
            } else {
                String acceptLangHeader =
                    (String)request.getHeader("Accept-Language");
                if ((acceptLangHeader !=  null) &&
                    (acceptLangHeader.length() > 0)) 
                {
                    configLocale = Locale.getLocaleObjFromAcceptLangHeader(
                        acceptLangHeader); 
                } else {
                    configLocale = java.util.Locale.getDefault();
                }
            }
            try {
                rb = ResourceBundle.getBundle(RB_NAME, configLocale);
            } catch (MissingResourceException mre) {
                // do nothing
            }
       }
    }

    public String getQuoteEscapedLocalizedString(String i18nKey) {
        String value = getLocalizedString(i18nKey);
        return value.replace("'", "\\'");
    }

    public String getLocalizedString(String i18nKey) {
        if (rb == null) {
            initializeResourceBundle();
        }
        
        String localizedValue = null;     
        try {
            localizedValue = Locale.getString(rb, i18nKey, debug);
        } catch (MissingResourceException mre) {
            // do nothing
        }
        return (localizedValue == null) ? i18nKey : localizedValue;
    }
    
    public String getHostName() { 
        if (hostName == null) {
            hostName = getContext().getRequest().getServerName();
        }        
        return hostName;
    }
    
    public String getHostName(String serverUrl, String defaultHostName) {
        URL url = null;
        
        try {
            url = new URL(serverUrl);
        } catch (MalformedURLException mue) {
            return defaultHostName;
        }
        
        return url.getHost();
    }
    
    public int getServerPort(String serverUrl, int defaultPort) {
        URL url = null;
        
        try {
            url = new URL(serverUrl);
        } catch (MalformedURLException mue) {
            return defaultPort;
        }
        
        return url.getPort();        
    }
    
    public String getBaseDir(HttpServletRequest req) {
        String basedir = AMSetupServlet.getPresetConfigDir();
        if ((basedir == null) || (basedir.length() == 0)) {
            String tmp = System.getProperty("user.home");
            if (File.separatorChar == '\\') {
                tmp = tmp.replace('\\', '/');
            }
            String uri = req.getRequestURI();
            int idx = uri.indexOf("/", 1);
            if (idx != -1) {
                uri = uri.substring(0, idx);
            }
            
            basedir = (tmp.endsWith("/")) ? tmp.substring(0, tmp.length()-1) :
                tmp;
            basedir += uri;
        } 

        return basedir;
    }
    
    public String getCookieDomain() {
        return getHostName();
    }

    public boolean validateInput() {
        String key = toString("key");
        String value = toString("value");

        if (value == null) {
            responseString = "missing.required.field";
        } else {
            getContext().setSessionAttribute(key, value);            
        }
        
        writeToResponse(getLocalizedString(responseString));
        setPath(null);
        return false;
    }
    
    public boolean resetSessionAttributes() {
        try {
            Field[] fields = SessionAttributeNames.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                try {
                    getContext().removeSessionAttribute(
                        (String)fields[i].get(null));
                } catch (IllegalAccessException e) {
                    //ingore
                }
            }
        } catch (SecurityException e) {
            writeToResponse(e.getMessage());
        }
        setPath(null);
        return false;
    }
    
    public String getAttribute(String attr, String defaultValue) {
        String value = (String)getContext().getSessionAttribute(attr);
        return (value != null) ? value : defaultValue;
    }
          
    public String getAvailablePort(int portNumber) {
        return Integer.toString(
            AMSetupUtils.getFirstUnusedPort(getHostName(), portNumber, 1000));
    }  
    
    public boolean checkPasswords() {
        String confirm = toString("confirm");
        String password = toString("password");
        String otherPassword = toString("otherPassword");
        String type = toString("type");
        
        if (password == null) {
            responseString = getLocalizedString("missing.password");
        } else if (password.length() < MIN_PASSWORD_SIZE) {
            responseString = getLocalizedString("password.size.invalid");
        } else if (confirm == null) {
            responseString = getLocalizedString("missing.confirm.password");
        } else if (confirm.length() < MIN_PASSWORD_SIZE) {
            responseString = getLocalizedString("password.size.invalid");
        } else if (!password.equals(confirm)) {
            responseString = getLocalizedString("password.dont.match");
        } else if ((otherPassword != null) && (otherPassword.equals(password))) {
            responseString = getLocalizedString("agent.admin.password.same");
        } else {            
            if (type.equals("agent")) {
                type = SessionAttributeNames.CONFIG_VAR_AMLDAPUSERPASSWD;
            } else {
                type = SetupConstants.CONFIG_VAR_ADMIN_PWD;
            }
            getContext().setSessionAttribute(type, password);
        }
        
        writeToResponse(responseString);
        setPath(null);
        return false;
    }
}
