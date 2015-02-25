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
 * $Id: JDBC.java,v 1.5 2008/08/28 21:56:45 madan_ranganath Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.authentication.modules.jdbc;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.sql.DataSource;

public class JDBC extends AMLoginModule {
    private String userTokenId;
    private String userName;
    private String password;
    private String resultPassword;
    private char[] passwordCharArray;
    private java.security.Principal userPrincipal = null;
    private String errorMsg = null;
    
    private static final String amAuthJDBC = "amAuthJDBC";
    private static Debug debug = Debug.getInstance(amAuthJDBC);
    private ResourceBundle bundle = null;
    
    private Map options;
    
    private static String CONNECTIONTYPE =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "JDBCConnectionType";
    private static String JNDINAME = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCJndiName";
    private static String DRIVER = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCDriver";
    private static String URL =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "JDBCUrl";
    private static String DBUSER = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCDbuser";
    private static String DBPASSWORD = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCDbpassword";
    private static String PASSWORDCOLUMN =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "JDBCPasswordColumn";
    private static String STATEMENT = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCStatement";
    private static String TRANSFORM =ISAuthConstants.AUTH_ATTR_PREFIX_NEW +  
        "JDBCPasswordSyntaxTransformPlugin";
    private static String AUTHLEVEL = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCAuthLevel";  
    private static String DEFAULT_TRANSFORM =
        "com.sun.identity.authentication.modules.jdbc.ClearTextTransform";
    
    private String driver;
    private String connectionType;
    private String jndiName;
    private String url;
    private String dbuser;
    private String dbpassword;
    private String passwordColumn;
    private String statement;
    private String transform;
    private Map sharedState;
    private boolean getCredentialsFromSharedState = false;
    private static final int MAX_NAME_LENGTH = 80;
    
    private boolean useJNDI = false;
    
    /**
     * Constructor.
     */
    public JDBC() {
        debug.message("JDBC()");
    }

    /**
     * Initializes parameters.
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    public void init(Subject subject, Map sharedState, Map options) {
        debug.message("in initialize...");
        java.util.Locale locale  = getLoginLocale();
        bundle = amCache.getResBundle(amAuthJDBC, locale);
        
        if (debug.messageEnabled()) {
            debug.message("amAuthJDBC Authentication resource bundle locale="+
                          locale);
        }
        
        this.options = options;
        this.sharedState = sharedState;
        
        if(options != null) {
            try {
                // First, figure out the type of connection
                connectionType = CollectionHelper.getMapAttr(
                    options, CONNECTIONTYPE);
                if (connectionType == null) {
                    debug.message("No CONNECTIONTYPE for configuring");
                    errorMsg ="noCONNECTIONTYPE";
                    return;
                } else { 
                    if (debug.messageEnabled()) {
                        debug.message("Found config for CONNECTIONTYPE: " + 
                                      connectionType);
                    }
               
                    if (connectionType.equals("JNDI")) { 
                        useJNDI = true;
                    }
                    
                    // If its pooled, get the JNDI name
                    if ( useJNDI ) {
                        debug.message("Using JNDI Retrieved Connection pool");
                        jndiName = CollectionHelper.getMapAttr(
                            options, JNDINAME);
                        if (jndiName == null) {
                            debug.message("No JNDINAME for configuring");
                            errorMsg ="noJNDINAME";
                            return;
                        } else  { 
                            if (debug.messageEnabled()) {
                                debug.message("Found config for JNDINAME: " + 
                                              jndiName);
                            }
                        }

                    // If its a non-pooled, then get the JDBC config
                    } else {
                        debug.message("Using non pooled JDBC");
                        driver = CollectionHelper.getMapAttr(options, DRIVER);
                        if (driver == null) {
                            debug.message("No DRIVER for configuring");
                            errorMsg ="noDRIVER";
                            return;
                        } else  {
                            if (debug.messageEnabled()) 
                                debug.message("Found config for DRIVER: " + 
                                              driver);
                        }

                        url = CollectionHelper.getMapAttr(options, URL);
                        if (url == null) {
                            debug.message("No URL for configuring");
                            errorMsg ="noURL";
                            return;
                        } else {
                            if (debug.messageEnabled()) {
                                debug.message("Found config for URL: " + url);
                            }
                        }
                        dbuser = CollectionHelper.getMapAttr(options, DBUSER);
                        if (dbuser == null) {
                            debug.message("No DBUSER for configuring");
                            errorMsg = "noDBUSER";
                            return;
                        } else  {
                            if (debug.messageEnabled()) {
                                debug.message("Found config for DBUSER: " +
                                              dbuser);
                            }
                        }

                        dbpassword = CollectionHelper.getMapAttr(
                            options, DBPASSWORD, "");
                        if (dbpassword == null) {
                            debug.message("No DBPASSWORD for configuring");
                            errorMsg = "noDBPASSWORD";
                            return;
                        }
                    }
                }
                
                // and get the props that apply to both connection types 
                passwordColumn = CollectionHelper.getMapAttr(
                    options, PASSWORDCOLUMN);
                if (passwordColumn == null) {
                    debug.message("No PASSWORDCOLUMN for configuring");
                    errorMsg = "noPASSWORDCOLUMN";
                    return;
                } else {
                    if (debug.messageEnabled()) { 
                        debug.message("Found config for PASSWORDCOLUMN: " +
                                      passwordColumn);
                    }
                }          
                statement = CollectionHelper.getMapAttr(options, STATEMENT);
                if (statement == null) {
                    debug.message("No STATEMENT for configuring");
                    errorMsg = "noSTATEMENT";
                }                   
                transform = CollectionHelper.getMapAttr(options, TRANSFORM);
                if (transform == null) {
                    if (debug.messageEnabled()) {
                        debug.message("No TRANSFORM for configuring."+
                                      "Using clear text");
                    }
                    transform = DEFAULT_TRANSFORM;
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("Plugin for TRANSFORM: " + transform);
                    }
                }
                                        
                String authLevel = CollectionHelper.getMapAttr(
                    options, AUTHLEVEL);
                if (authLevel != null) {
                    try {
                        setAuthLevel(Integer.parseInt(authLevel));
                    } catch (Exception e) {
                        debug.error("Unable to set auth level " +
                                    authLevel,e);
                    }
                }
                      
            } catch(Exception ex) {
                debug.error("JDBC Init Exception", ex);
            }
        }
    }
    
    /**
     * Processes the authentication request.
     *
     * @return <code>ISAuthConstants.LOGIN_SUCCEED</code> as succeeded; 
     *         <code>ISAuthConstants.LOGIN_IGNORE</code> as failed.
     * @exception AuthLoginException upon any failure. login state should be 
     *            kept on exceptions for status check in auth chaining.
     */
    public int process(Callback[] callbacks, int state) 
        throws AuthLoginException {
        // return if this module is already done
        if (errorMsg != null) {
            throw new AuthLoginException(amAuthJDBC, errorMsg, null);
        }
        if (debug.messageEnabled()) {
            debug.message("State: " + state);
        }
        
        if (state != ISAuthConstants.LOGIN_START) {
            throw new AuthLoginException(amAuthJDBC, "invalidState", null);
        }

        if (callbacks != null && callbacks.length == 0) {
            userName = (String) sharedState.get(getUserKey());
                 password = (String) sharedState.get(getPwdKey());
                 if (userName == null || password == null) {
                 return ISAuthConstants.LOGIN_START;
            }
            getCredentialsFromSharedState = true;
        } else {        
            userName = ((NameCallback) callbacks[0]).getName();
            if (debug.messageEnabled()) {
                debug.message("Authenticating this user: " + userName);
            }
                
            passwordCharArray = ((PasswordCallback) callbacks[1]).getPassword();
            password = new String(passwordCharArray);
                
            if (userName == null || userName.length() == 0) {
                throw new AuthLoginException(amAuthJDBC, "noUserName", null);
            }
        }
        
        storeUsernamePasswd(userName, password);

        // Check if they'return being a bit malicious with their UID.
        // SQL attacks will be handled by prepared stmt escaping.
        if (userName.length() > MAX_NAME_LENGTH ) {
            throw new AuthLoginException(amAuthJDBC, "userNameTooLong", null);
        } 
        Connection database = null;
        PreparedStatement thisStatement = null;
        ResultSet results = null;
        try {
            if ( useJNDI ) {        
                Context initctx = new InitialContext();
                DataSource ds = (DataSource)initctx.lookup(jndiName);

                if (debug.messageEnabled()) { 
                    debug.message("Datasource Acquired: " + ds.toString());
                }
                database = ds.getConnection();
                debug.message("Using JNDI Retrieved Connection pool");
                
            } else {
                Class.forName (driver);
                database = DriverManager.getConnection(url,dbuser,dbpassword);
            }          
            if (debug.messageEnabled()) {
                debug.message("Connection Acquired: " + database.toString());
            }           
            //Prepare the statement for execution
            if (debug.messageEnabled()) {
                debug.message("PreparedStatement to build: " + statement);
            }
            thisStatement = 
                database.prepareStatement(statement);
            thisStatement.setString(1,userName);
            if (debug.messageEnabled()) {
                    debug.message("Statement to execute: " + thisStatement);
            }
            
            // execute the query
            results = thisStatement.executeQuery();
            
            if (results == null) {
                debug.message("returned null from executeQuery()");
                throw new AuthLoginException(amAuthJDBC, "nullResult", null);
            }
            
            //parse the results.  should only be one item in one row.
            int index = 0;
            while (results.next()) {
                // do normal processing..its the first and last row
                index ++;
                if (index > 1) {
                    if (debug.messageEnabled()) {
                        debug.message("Too many results."+
                                "UID should be a primary key");
                    }
                    throw new AuthLoginException(amAuthJDBC, "multiEntry",null);
                }
                resultPassword = results.getString(passwordColumn).trim();
            } 
            if (index == 0) {
                // no results
                if (debug.messageEnabled()) {
                    debug.message("No results from your SQL query."+
                            "UID should be valid");
                }
                throw new AuthLoginException(amAuthJDBC, "nullResult", null);
             }
        } catch (Throwable e) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return ISAuthConstants.LOGIN_START;
            }
            if (debug.messageEnabled()) {
                debug.message("JDBC Exception:",  e);
            }
            throw new AuthLoginException(e);
        } finally {
            // close the resultset
            if (results != null) {
                  try {
                    results.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            // close the statement
            if (thisStatement != null) {
                  try {
                    thisStatement.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            // close the connection when done
            if (database != null) {
                  try {
                    database.close();
                } catch (Exception dbe) {
                    debug.error("Error in closing database connection: " + 
                        dbe.getMessage());
                    if (debug.messageEnabled()) {
                        debug.message("Fail to close database:", dbe);
                    }
                }
            }
        }  
            
        if (!transform.equals(DEFAULT_TRANSFORM)) {
            try {
                  JDBCPasswordSyntaxTransform syntaxTransform = 
                    (JDBCPasswordSyntaxTransform)Class.forName(transform)
                        .newInstance();
                if (debug.messageEnabled()) {
                    debug.message("Got my Transform Object" + 
                            syntaxTransform.toString() );
                }
                password = syntaxTransform.transform(password);

                if (debug.messageEnabled()) {
                    debug.message("Password transformed by: " + transform );
                }
            } catch (Throwable e) {
                if (debug.messageEnabled()) {
                    debug.message("Syntax Transform Exception:"+ e.toString());
                  }
                throw new AuthLoginException(e);               
            }
        }
        // see if the passwords match
        if (password != null && password.equals(resultPassword)) {
            userTokenId = userName;
            return ISAuthConstants.LOGIN_SUCCEED;
        } else {           
            debug.message("password not match. Auth failed.");
            setFailureID(userName);
            throw new InvalidPasswordException(amAuthJDBC, "loginFailed",
                null, userName, null);
        }
    }

    /**
     * Returns principal of the authenticated user.
     *
     * @return Principal of the authenticated user.
     */
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new JDBCPrincipal(userTokenId);
            return userPrincipal;   
        } else {
            return null;
        }
    }

    /**
     * Cleans up the login state.
     */
    public void destroyModuleState() {
        userTokenId = null;
        userPrincipal = null;
    }

    public void nullifyUserdVars() {
        userName = null;
        password = null;
        resultPassword = null;
        passwordCharArray = null;
        errorMsg = null;
        bundle = null;
        options = null;
        driver = null;
        connectionType = null;
        jndiName = null;
        url = null;
        dbuser = null;
        dbpassword = null;
        passwordColumn = null;
        statement = null;
        transform = null;
        sharedState = null;
    }
}
