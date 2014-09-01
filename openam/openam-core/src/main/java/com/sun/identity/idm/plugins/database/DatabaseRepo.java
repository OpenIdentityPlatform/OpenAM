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
 * $Id: DatabaseRepo.java,v 1.1 2009/04/21 20:04:48 sean_brydon Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.idm.plugins.database;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SchemaType;

/**
 * This class stores identity information in a database
 */
public class DatabaseRepo extends IdRepo {
    
    //??can change later to another logfile but in merge list so keep for now ??
    private static Debug debug = Debug.getInstance("amIdRepoDatabase");
    
       // Class name used in exception messages
    private static final String PLUGIN_CLASS_NAME = 
        "com.sun.identity.idm.plugins.database.DatabaseRepo";
            
    // SMS Configurations, names of schema elements in idRepoService.xml. 
    // Each of these schema element names is the element that holds the actual
    // value for some configuration info, such as DB connection info.
    
    // idRepoService.xml schema element name for list of IdTypes and the
    //opeartions each is allowed to do    
     private static final String SUPPORTED_OPERATIONS_SCHEMA_NAME =
            "sun-opensso-database-sunIdRepoSupportedOperations";
    
    // idRepoService.xml schema element name for user data access object DAO
    // class name. Used by factory method to bind to a DAO implementation
    private static final String DAO_PLUGIN_CLASS_NAME_SCHEMA_NAME = 
            "sun-opensso-database-dao-class-name";
    
    // idRepoService.xml schema element name for database data source name
    //determines whether to use JNDI of JBDC to get connetcions to datasoure
    private static final String JDBC_CONNECTION_TYPE_SCHEMA_NAME = 
            "sun-opensso-database-dao-JDBCConnectionType";
    
    // idRepoService.xml schema element name for database data source name
    //this code uses a DataSource so does not need the DB username and password
    //in code for connections, since it is in config of appserver env in web.xml
    private static final String DATASOURCE_SCHEMA_NAME = 
            "sun-opensso-database-DataSourceJndiName";
    
    // idRepoService.xml schema element name for JDBC class to use to get 
    // connections to the db
    private static final String JDBC_DRIVER_SCHEMA_NAME = 
            "sun-opensso-database-JDBCDriver";
    
    // idRepoService.xml schema element name for url of JDBC driver 
    private static final String JDBC__DRIVER_URL_SCHEMA_NAME = 
            "sun-opensso-database-JDBCUrl";
    
    // idRepoService.xml schema element name for username for JDBC driver 
    private static final String JDBC_USER_NAME_SCHEMA_NAME = 
            "sun-opensso-database-JDBCDbuser";
    
    // idRepoService.xml schema element name for password for JDBC driver 
    private static final String JDBC__DRIVER_PASSWORD_SCHEMA_NAME = 
            "sun-opensso-database-JDBCDbpassword";
      
 
    //wont use for now
    //idRepoService.xml schema element name for list of attributes to be hashed
    //private static final String ATTRIBUTES_TO_BE_HASHED_SCHEMA_NAME = "sun-opensso-database-HashAttrs";

    //wont use for now
    // idRepoService.xml schema element name for list of attributes to be encrypted
    //private static final String ATTRIBUTES_TO_BE_ENCRYPTED_SCHEMA_NAME = "sun-opensso-database-EncryptAttrs";
    
     // idRepoService.xml schema element name for user database table name
    private static final String USER_DB_TABLE_NAME_SCHEMA_NAME = 
            "sun-opensso-database-UserTableName";
    // idRepoService.xml schema element name for password attribute
    private static final String USER_PASSWORD_SCHEMA_NAME = 
            "sun-opensso-database-UserPasswordAttr";
    // idRepoService.xml schema element name for userID attribute
    private static final String USER_ID_SCHEMA_NAME = 
            "sun-opensso-database-UserIDAttr";
       
        // ???? Status attribute   ??? is this status used for users, roles, groups,    
    private static final String USER_STATUS_SCHEMA_NAME =
            "sun-opensso-database-UserStatusAttr";  
    // idRepoService.xml schema element name for user status active value
    //for example the value could be "Active" or whatever is specified in
    //idReposervices.xml under this attribute
    private static final String USER_STATUS_ACTIVE_VALUE_SCHEMA_NAME =
            "sun-opensso-database-activeValue";  
    // idRepoService.xml schema element name for user status in-active value
    //for example the value could be "Inactive"
    private static final String USER_STATUS_INACTIVE_VALUE_SCHEMA_NAME =
            "sun-opensso-database-inactiveValue";
    
    // idRepoService.xml schema element name for max search results value
    private static final String SEARCH_MAX_RESULT =
            "sun-opensso-database-config-max-result";
    
    // idRepoService.xml schema element name for user db attr name to use
    //  when doing search queries
    private static final String USERS_SEARCH_ATTRIBUTE_SCHEMA_NAME =
            "sun-opensso-database-config-users-search-attribute";
    
     // idRepoService.xml schema element name for set of user attribute names
     private static final String SET_OF_USER_ATTRIBUTES_SCHEMA_NAME =
             "sun-opensso-database-UserAttrs";
     
     // idRepoService.xml schema element name of the table in the DB for 
     // membership info, like a groups table which holds group info
     private static final String MEMBERSHIP_TABLE_NAME_SCHEMA_NAME = 
             "sun-opensso-database-MembershipTableName";
     
     // idRepoService.xml schema element name of the column in the DB within the
     // membership table which is  the unique column identifying the membership
     // id, like a group_name column which holds a name that uniquely identifies
     // a group
     private static final String MEMBERSHIP_ID_ATTRIBUTE_NAME_SCHEMA_NAME =
             "sun-opensso-database-MembershipIDAttr";
     
     // idRepoService.xml schema element name of the column in the DB within the 
     // membership table and that column name is used for searches
     private static final String MEMBERSHIP_SEARCH_ATTRIBUTE_NAME_SCHEMA_NAME =
             "sun-opensso-database-membership-search-attribute";
             
    // Fields that represent the actual values that were retrieved from 
    // idRepoService.xml schema element names
     
    //Used by factory method to bind to a DAO implementation. Value is
    //obtained from idRepoService.xml but a default is assigned.
    // example value com.sun.identity.idm.plugins.jdbc.JdbcSimpleUserDao  
    private String daoClassName;
    
    //Data Access Object for accessing a DB datastore
    private DaoInterface dao;
    
    private String userDataBaseTableName;
    
    // Password attribute used in authenticate method
    private String passwordAttributeName;
    
    //attribute name of the userid column/attr in DB, passed into DAO calls
    private String userIDAttributeName;
    
    //list of IdTpes and associated operations each is allowd to do
    private static Map supportedOps = new CaseInsensitiveHashMap();
    
    //list of attribute/column names for users
    private Set<String> userAtttributesAllowed;
    
    //name of user status attribute column
    private String statusAttributeName;  
    
    //in some conditions, like there is no statusAttributeName available in
    //idRepoService.xml, then may set the users status as always active
    //since there is no attribute column to check for a user's status value
    private boolean alwaysActive = false;
    
    //these values are used to compare with values retreived from user db table 
    //status attribute column to see if user is active or not
    private static final String DEFAULT_USER_STATUS_ACTIVE_COMPARISON_VALUE = "Active";
    private static final String DEFAULT_USER_STATUS_INACTIVE_COMPARISON_VALUE = "Inactive";    
    private String statusActiveComparisonValue = 
            DEFAULT_USER_STATUS_ACTIVE_COMPARISON_VALUE;
    private String statusInActiveComparisonValue =
            DEFAULT_USER_STATUS_INACTIVE_COMPARISON_VALUE;
    
    //determine the deafult for number of search results to fetch
    private int defaultSearchMaxResults = 100;
    
    //attribute column name to be used in searches fro users
    private String userSearchNamingAttr = null;
    
    // name of the table in the DB for membership info, like a groups table
    private String membershipTableName = null;
     
    // name of the column in the DB within the membership table which is the
    // unique column identifying the membership id, like a group_name column
    // which holds a name that uniquely identifies a group
    private String membershipIdAttributeName = null;
     
     // name of the column in the DB within the membership table and that
    // column name is used for searches
    private String membershipSearchAttributeName = null;

    // Initialization exception
    IdRepoException initializationException;

    public DatabaseRepo() {
        if (debug == null) {
            debug = Debug.getInstance("amIdRepoDatabase");
        }
        //load some default ops, later they are changed in initialize
        //based on user provided list of types and allowed ops
        loadDefaultSupportedOps();
    }

    /*
     * Initialization of parameters as configured for a given plugin.
     * 
     * @see com.sun.identity.idm.IdRepo#initialize(java.util.Map)
     */
    public void initialize(Map configParams) throws IdRepoException {
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.initialize called.");
        }
        super.initialize(configParams);
        
        //helper for parsing config info
        RepoConfigHelper configHelper=
                new RepoConfigHelper(debug);
        
        daoClassName = configHelper.getPropertyStringValue(configParams,
                DAO_PLUGIN_CLASS_NAME_SCHEMA_NAME);        
        try {
            //validate
            if(daoClassName == null || daoClassName.trim().length()== 0) {
                String badDaoMsg = "DatabaseRepo.initialize: daoClassName obtained"
                        + " from IdRepoService.xml can not be null or empty."
                        + " daoClassName=" + daoClassName;               
                initializationException = new IdRepoException(badDaoMsg);               
                debug.error(badDaoMsg);
                 return;
            } else { 
                dao = (DaoInterface) Class.forName(daoClassName).newInstance();
            }
        } catch (ClassNotFoundException cnfe) {            
            initializationException = new IdRepoException(cnfe.getMessage());               
            debug.error("DatabaseRepo.initialize: exception trying to create a new"
                    + " DAO class. Can not configure this datastore", cnfe);
             return;
        } catch (InstantiationException ie) {           
            initializationException = new IdRepoException(ie.getMessage());               
            debug.error("DatabaseRepo.initialize: exception trying to create a new"
                    + " DAO class. Can not configure this datastore", ie);
             return;
        } catch (IllegalAccessException iae) {          
            initializationException = new IdRepoException(iae.getMessage());               
            debug.error("DatabaseRepo.initialize: exception trying to create a new"
                    + " DAO class. Can not configure this datastore", iae);
             return;
        } catch (Exception noDAOex) {
            initializationException = new IdRepoException(noDAOex.getMessage());               
            debug.error("DatabaseRepo.initialize: exception trying to create a new"
                    + " DAO class. Can not configure this datastore", noDAOex);
             return;
        }
        
        //determines whether to use JNDI or JDBC driver manager for connections
        String connectionType = 
                configHelper.getPropertyStringValue(configParams,
                JDBC_CONNECTION_TYPE_SCHEMA_NAME);

        boolean useJNDI;
        if (connectionType != null && connectionType.equals("JNDI")) {
            useJNDI = true;
        } else {
            //unless JNDI is specified, then assume JDBC
            useJNDI = false;
        }
              
        //Get the name of the database table for users
        userDataBaseTableName = 
                configHelper.getPropertyStringValue(configParams,
                USER_DB_TABLE_NAME_SCHEMA_NAME);
        if (userDataBaseTableName == null 
                || userDataBaseTableName.trim().length()==0) {
            String errorMessage = "DatabaseRepo.initialize: validation failed"
                    + " on User DataBase Table Name config info, value must be"
                    + " non-null and not empty for"
                    + " userDataBaseTableName=" + userDataBaseTableName;
            if (debug.errorEnabled()) {
                debug.error(errorMessage);
            }
            initializationException = new IdRepoException(errorMessage);
            //consider returning and not continuing ??          
        }       

        //now get membership info, for example to support groups
        membershipTableName = configHelper.getPropertyStringValue(configParams,
                MEMBERSHIP_TABLE_NAME_SCHEMA_NAME);
        membershipIdAttributeName = configHelper.getPropertyStringValue(configParams,
                MEMBERSHIP_ID_ATTRIBUTE_NAME_SCHEMA_NAME);
        membershipSearchAttributeName = configHelper.getPropertyStringValue(configParams,
                MEMBERSHIP_SEARCH_ATTRIBUTE_NAME_SCHEMA_NAME);  
        //validate membership config info
        if(membershipTableName == null || membershipIdAttributeName==null
                || membershipSearchAttributeName == null) {
            //no need to validate against length==0 ,can be blank since optional
            //RFE: use the supportedOps to see if groups is allowed and if so
            //     then make sure values are not blank since they will be used
            String errorMessage = "DatabaseRepo.initialize: validation failed"
                    + " on membership config info, values must be non-null for"
                    + " membershipTableName=" + membershipTableName
                    + " membershipIdAttributeName=" + membershipIdAttributeName
                    + " membershipSearchAttributeName="
                    + membershipSearchAttributeName;
            if (debug.errorEnabled()) {
                debug.error(errorMessage);
            }
            initializationException = new IdRepoException(errorMessage);
            //consider returning and not continuing ??          
        }        
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.initialize: "
                    + " membershipTableName=" + membershipTableName
                    + " membershipIdAttributeName=" + membershipIdAttributeName
                    + " membershipSearchAttributeName="
                    + membershipSearchAttributeName);
        }
        
        if (useJNDI) {
            //name to use to lookup DataSource for database connections, 
            //for example java:comp/env/jdbc/mysqltest
            String datasourceName = 
                    configHelper.getPropertyStringValue(configParams,
                    DATASOURCE_SCHEMA_NAME);
            if (datasourceName != null && !(datasourceName.length()==0)
                    && userDataBaseTableName != null 
                    && !(userDataBaseTableName.length()==0) ) {
                if (debug.messageEnabled()) {
                    debug.message("DatabaseRepo.initialize, about to call"
                            + "DAO initialize, for useJNDI=" + useJNDI);
                }
                try {
                    dao.initialize(datasourceName, userDataBaseTableName,
                            membershipTableName, debug);
                } catch (Exception ex) {
                    //this exception is used as a flag to determine whether this 
                    //idRepo has been connected to its data store or not
                    //and sometimes thrown from other methods if error on initialize
                    initializationException = 
                            new IdRepoException(ex.getMessage());               
                    debug.error("DatabaseRepo.initialize: exception trying to"
                            + " set up DB datasource connection.", ex);
                }
            } else {
                String errorMessage = "DatabaseRepo.initialize: datasourceName"
                    + " and userDataBaseTableName must be not null and not"
                    + " empty. So initialize can not succeed."
                    + " datasourceName=" + datasourceName
                    + " userDataBaseTableName" + userDataBaseTableName;
                debug.error(errorMessage);
                initializationException = new IdRepoException(errorMessage);
                //consider returning and not continuing ??
            }
        } else { //use JDBC DriverManager params to initialize DAO
            
            //if connection type is JDBC ...    
            //if JDBCConnectionType is JDBC then it needs the DriverManager
            //class name, plus the url, dbUserName, dbPassword to get connections
            String jdbcDriver = 
                    configHelper.getPropertyStringValue(configParams,
                    JDBC_DRIVER_SCHEMA_NAME);
            //url of JDBC driver 
            String jdbcDriverUrl = 
                    configHelper.getPropertyStringValue(configParams,
                    JDBC__DRIVER_URL_SCHEMA_NAME);   
            // username for JDBC driver 
            String jdbcDbUser = 
                    configHelper.getPropertyStringValue(configParams,
                    JDBC_USER_NAME_SCHEMA_NAME);
            // password for JDBC driver 
            String jdbcDbPassword = 
                    configHelper.getPropertyStringValue(configParams,
                    JDBC__DRIVER_PASSWORD_SCHEMA_NAME);
        
            if (jdbcDriver != null && !(jdbcDriver.length()==0)
                && jdbcDriverUrl != null && !(jdbcDriverUrl.length()==0)
                && jdbcDbUser != null && !(jdbcDbUser.length()==0)
                && jdbcDbPassword != null && !(jdbcDbPassword.length()==0)
                && userDataBaseTableName != null 
                && !(userDataBaseTableName.length()==0) ) {
                if (debug.messageEnabled()) {
                    debug.message("DatabaseRepo.initialize, about to call"
                            + "DAO initialize, for useJNDI=" + useJNDI);
                }
                try {
                    dao.initialize(jdbcDriver, jdbcDriverUrl, jdbcDbUser,
                            jdbcDbPassword, userDataBaseTableName,
                            membershipTableName, debug);
                } catch (Exception ex) {
                    //this exception is used as a flag to determine whether this 
                    //idRepo has been connected to its data store or not
                    //and sometimes thrown from other methods if error on initialize
                    initializationException = 
                            new IdRepoException(ex.getMessage());              
                    debug.error("DatabaseRepo.initialize: exception trying to"
                            + " set up DB datasource connection.", ex);
                }
            } else {
                String errorMessage = "DatabaseRepo.initialize: using " 
                    + " useJNDI=" + useJNDI + " . The config parameters"
                    + " jdbcDriver, jdbcDriverUrl, jdbcDbUser, jdbcDbPassword,"
                    + " and userDataBaseTableName must be not null and not"
                    + " empty. So initialize can not succeed."
                    + " jdbcDriver=" + jdbcDriver
                    + " jdbcDriverUrl=" + jdbcDriverUrl
                    + " jdbcDbUser=" + jdbcDbUser
                    + " jdbcDbPassword=" + jdbcDbPassword
                    + " userDataBaseTableName" + userDataBaseTableName;
                debug.error(errorMessage);
                initializationException = new IdRepoException(errorMessage);
                //consider returning and not continuing ??
            }
        }
        
        
        // Get password attribute name
        passwordAttributeName = configHelper.getPropertyStringValue(configParams,
                USER_PASSWORD_SCHEMA_NAME);
         // Get userID attribute name
        userIDAttributeName = configHelper.getPropertyStringValue(configParams,
                USER_ID_SCHEMA_NAME);
                
        //get the set of operations for each IdType allowed
        Set userSpecifiedOpsSet = null;
        userSpecifiedOpsSet = new HashSet((Set) configParams
                .get(SUPPORTED_OPERATIONS_SCHEMA_NAME));
        supportedOps = configHelper.parsedUserSpecifiedOps(userSpecifiedOpsSet);
         
        //get set of attribute/column names for users
        userAtttributesAllowed = new HashSet((Set) configParams
                .get(SET_OF_USER_ATTRIBUTES_SCHEMA_NAME));
        
        // Get name of status attribute  from idRepoService.xml config
        statusAttributeName = configHelper.getPropertyStringValue(configParams,
                USER_STATUS_SCHEMA_NAME);
        if (statusAttributeName == null || statusAttributeName.length() == 0) {
            //if nothing specified then each user is always active
            alwaysActive = true;
        }
        
        // Get value of status attribute  from idRepoService.xml config. This
        //value is used to compare with values retreived from db to test if user
        //status value is set to active, so need to find value that means active.
        statusActiveComparisonValue = 
                configHelper.getPropertyStringValue(configParams,
                USER_STATUS_ACTIVE_VALUE_SCHEMA_NAME, 
                DEFAULT_USER_STATUS_ACTIVE_COMPARISON_VALUE);
        
        statusInActiveComparisonValue = 
                configHelper.getPropertyStringValue(configParams,
                USER_STATUS_INACTIVE_VALUE_SCHEMA_NAME, 
                DEFAULT_USER_STATUS_INACTIVE_COMPARISON_VALUE);

        defaultSearchMaxResults = configHelper.getPropertyIntValue(configParams,
                SEARCH_MAX_RESULT, defaultSearchMaxResults);
        
        userSearchNamingAttr = configHelper.getPropertyStringValue(configParams,
                USERS_SEARCH_ATTRIBUTE_SCHEMA_NAME);
        
        
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.initialize: "
                + "\n\t Password Attr name: " + passwordAttributeName 
                + "\n\t User ID Attr name: " + userIDAttributeName                   
                + "\n\t userAtttributesAllowed: "+ userAtttributesAllowed
                + "\n\tStatus Attr name: " + statusAttributeName
                + "\n\t defaultSearchMaxResults:" + defaultSearchMaxResults
                + "\n\t userSearchNamingAttr:" + userSearchNamingAttr
                + "\n\tsupportedOps Map Attr: " +    supportedOps);
        }
    }


    /*
     * (non-Javadoc)
     * 
     *  Creates an identity.
     *
     * @param token
     *     Single sign on token of identity performing the task. This is not
     *     used.
     * @param type
     *     Identity type of this object. For example user or agent etc.
     * @param name
     *     Name of the object of interest, usually a user id.
     * @param attrMap
     *     Map of attribute-values assoicated with this object. The values would
     *     be stored in the database in the corresponding columns for example.
     *     If it contains attribute names which dont correspond to table coulmn
     *     names the an error coccurs. Similarly if the values are bad for 
     *     example.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     *
     * @see com.sun.identity.idm.IdRepo#create(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map)
     */
    public String create(SSOToken token, IdType type, String name, Map attrMap)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error(
                "DatabaseRepo.create: throwing initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.create called with:" 
                    + " token=" + token + " IdType=" + type + " name=" + name
                    + "\n\tattrMap=" + attrMap);
        }
        if(name==null || name.length()==0) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.create will not be executed since name"
                    + " is null or empty." +  " name=" + name);
            }
        }
        if (type.equals(IdType.GROUP)) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.createtype is GROUP, so will"
                        + " attempt to create group of" +  " name=" + name
                        + "\n\tattrMap=" + attrMap);
            }
            //RFE: wrap this in a try/catch clause in case already exists
            dao.createGroup(name, membershipIdAttributeName);
            return name;
        }
        if(attrMap == null) {
            attrMap = new HashMap<String, Set<String> >();
        } else if (attrMap.isEmpty() ||
                !attrMap.containsKey(userIDAttributeName)) {
            //add name to the attrMap and try to add it
           Set<String> nameVals = new HashSet<String>();
           nameVals.add(name);
           attrMap.put(userIDAttributeName, nameVals);
        } else {
            //contains userIDAttributeName key but now check value is ok
            Set<String> nameVals = (Set<String>)attrMap.get(userIDAttributeName);
            if (nameVals == null || nameVals.isEmpty()) {
                nameVals.add(name);
            }
        }                
        //throw exception if this type user not allowed to do this
        isValidType(type, "create");
        
        String createdName = null;
        //FIX: dont use isExists since it is extra query. Plus to be consistent
        //would need the two queries in a transaction
        if (!isExists(token, type, name)) {
                //put info into DB
                //FIX: should validate that attrMap is Ok, has required
                //      attributes, proper types for values etc
                createdName = dao.createUser(userIDAttributeName, attrMap);    
        } else {
                //Name already exists
                String args[] = { name };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "310",
                    args);
        }
        if (createdName == null) {
            //later, need to throw IdRepoException..need to add proper message to amIdrepo.properties
            return ""; //empty string ?
        } else {
            return createdName;
        }
    }

    /*
     * (non-Javadoc)
     * Deletes an identity.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     *
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     *
     * @see com.sun.identity.idm.IdRepo#delete(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public void delete(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {      
        if (initializationException != null) {
            debug.error(
                "DatabaseRepo.delete: throwing initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.delete called with parameters:" 
                    + " token="+ token + " type=" + type
                    + " name=" + name);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "delete");
    
        if (name != null && name.length() != 0) {
            if (type.equals(IdType.USER)) {
                dao.deleteUser(name, userIDAttributeName);
            } else if (type.equals(IdType.GROUP)) {            
                dao.deleteGroup(name, membershipIdAttributeName);
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.delete: parameter name is null or"
                        + "empty so delete will not be executed. name=" + name);
            }  
        }        
    }

    /*
     * Returns just --requested-- attributes and values of name object.
     * Allows user to provide a set of attributes that should be returned, 
     * and this set of attributes to retrieve is provided in parameter
     * Set attrNames.
     * 
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error(
                "DatabaseRepo.getAttributes: throwing initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getAttributes called with: "
                    + " token="+ token + " type=" + type + " name=" + name
                    + "\n\treturn attributes=" + attrNames);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "getAttributes");

        // Get all the attributes and return the subset
        //Map answer = (attrNames == null) ? null : new HashMap();
        Map answer = (attrNames == null) ? null : new CaseInsensitiveHashMap();
        //get all attributes and their corresponding values Sets
        Map map = getAttributes(token, type, name);
        if (attrNames == null) {
            answer = map;
        } else {
            //make a map that only includes the attributes specified in input
            //paramater Set attrNames, 
            for (Iterator items = attrNames.iterator(); items.hasNext();) {
                Object key = items.next();
                Object value = map.get(key);
                if (value != null) {
                    answer.put(key, value);
                }
            }
        }      
        return (answer);
    }

    /*
     * Returns --all-- attributes and values of name object
     * so whole row of DB table for this user for example
     *
     * I think it makes a map where map contains the attribute/column name as 
     * a String key and a Set as the value where Set can be the String values
     * for that attribute.  Does the case matter????
     * Should the values to a CaseSenstiveHashMap ???? or all lowercase?
     * should keys be lowercase ???
     * What should I put in the case of when an attribute has no values???
     * 
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public Map getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.getAttributes: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getAttributes: get all attrs called: "
               + " token="+ token + " type=" + type.getName()+ " name=" + name);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "getAttributes");
        
        Map users = Collections.EMPTY_MAP;
        if (type.equals(IdType.USER)) {
            users = dao.getAttributes(name, userIDAttributeName, userAtttributesAllowed);
        } else  if (type.equals(IdType.GROUP)) {
            //RFE: consider making the groups allowed to be fetched set a
            //config option in UI  as with userAtttributesAllowed
            Set<String> groupAttrsAllowed = new HashSet<String>();
            groupAttrsAllowed.add(membershipIdAttributeName);
            users = dao.getGroupAttributes(name, 
                    membershipIdAttributeName, groupAttrsAllowed);
        }
        //not sure this is how case insensitive map works???
        //are keys insensitive or set of values or ????
        Map answer = new CaseInsensitiveHashMap(users);       
        return answer;
    }
    
    /**
     * Set the values of attributes of the identity.
     *
     * @param token
     *     Single sign on token of identity performing the task.
     * @param type
     *     Identity type of this object.
     * @param name
     *     Name of the object of interest.
     * @param attributes
     *     Map of attribute-values to set or add.
     * @param isAdd
     *     if <code>true</code> add the attribute-values; otherwise
     *     replaces the attribute-values.
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     *
     * @see com.sun.identity.idm.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map,
     *      boolean)
     */
    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) 
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.setAttributes: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.setAttributes called with:" 
                    + " token=" + token + " type="+ type.getName() 
                    + " name=" + name + " isAdd=" + isAdd 
                    + "\n\tAttributes=" + attributes);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "getAttributes");
               
        //for now set isAdd to false since I will just replace the 
        //current values with the new values
        //FIX: later add support for multi-valued attributes
        isAdd = false;
        
        if (name != null && name.length() != 0) {
            dao.updateUser(name, userIDAttributeName, attributes);
        } else {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.setAttributes: input parameter"
                        + "  name is null or empty so delete will not be"
                        + " executed. name=" + name);
            }  
        }         
    }
    
    /*
     * Removes the attributes from the identity.
     *
     * @see com.sun.identity.idm.IdRepo#removeAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Set)
     */
    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.removeAttributes: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.removeAttributes called with:" 
                    + " token=" + token + " type="+ type.getName() 
                    + " name=" + name + "\n\t attrNames=" + attrNames);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "removeAttributes");
         //TO DO LATER...Not sure this is needed for a database repo?
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Set)
     */
    public Map getBinaryAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {        
        if (initializationException != null) {
            debug.error("DatabaseRepo.getBinaryAttributes: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getBinaryAttributes called with:" 
                    + " token=" + token + " type="+ type.getName() 
                    + " name=" + name + "\n\t attrNames=" + attrNames);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "getBinaryAttributes");
        //Map stringAttributes = getAttributes(token, type, name, attrNames);

              //TO DO LATER
        return Collections.EMPTY_MAP;
    }

    /*
     * (non-Javadoc)
     * 
     * Set the values of binary attributes the identity.
     *
     * @see com.sun.identity.idm.IdRepo#setBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Map, boolean)
     */
    public void setBinaryAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.setBinaryAttributes: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.setBinaryAttributes called with:"
                    + " token=" + token + " type="+ type.getName()
                    + " name=" + name + " isAdd=" + isAdd
                    + "\n\t attributes=" + attributes);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "setBinaryAttributes");
        
        //TODO LATER
    }

    /*
     * Returns members of an identity. Applicable if identity is a
     * group or a role.
     * @see com.sun.identity.idm.IdRepo#getMembers(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String,
     *      com.sun.identity.idm.IdType)
     */
    public Set getMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.getMembers: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getMembers: "
                    + "token=" + token + "IdType=" + type 
                    + ": name=" + name + ": membersType=" + membersType);
        }
        if(name==null || type==null || membersType==null) {
            debug.message("DatabaseRepo.getMembers: parameters type, name,"
                    + "membersTypeare can not be null, so returning empty set."
                    +  "IdType=" + type + ": name=" + name 
                    + ": membersType=" + membersType);
            return Collections.EMPTY_SET;
        }
        if (!membersType.equals(IdType.USER)) {
            debug.error("DatabaseRepo.getMembers: Groups do not support"
                        + " membership for " + membersType.getName());
             Object[] args = { PLUGIN_CLASS_NAME, membersType.getName(),
                        type.getName() };
             throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
         }
        //throw exception if this type user not allowed to do this
        //isValidType(type, "getMembers");
        
        Set members = null;
        if (type.equals(IdType.USER)) {
            debug.error("DatabaseRepo.getMembers: Membership operation is not"
                    + " supported for Users");
            throw new IdRepoException(IdRepoBundle.getString("203"), "203");
        } else if (type.equals(IdType.GROUP)) {           
            members = dao.getMembers(name, membershipIdAttributeName);
        } else {
            Object[] args = { PLUGIN_CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        if(members == null) {
            members = Collections.EMPTY_SET;
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getMembers: returning members=" 
                    + members);
        }
        return members;
    }

    /*
     * Receive a name and the idType of that name, plus the type of memberships
     * that you are interested in. For example, for a "user" named "chris" 
     * get all the groups that he is a member of.
     *
     * @return  Set of objects that <code>name</code> is a member of.
     *
     * @see com.sun.identity.idm.IdRepo#getMemberships(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, com.sun.identity.idm.IdType)
     */
    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.getMemberships: throwing initialization"
                    + " exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getMemberships called " 
                    + " token=" + token + " type=" + type
                    + " name=" + name + "membershipType=" + membershipType);
        }
        //throw exception if this type user not allowed to do this
        //isValidType(type, "getMemberships");
        
        if(name==null || type==null || membershipType==null) {
            debug.message("DatabaseRepo.getMemberships: parameters type, name,"
                    + "membersTypeare can not be null, so returning empty set."
                    +  "IdType=" + type + ": name=" + name 
                    + ": membershipType=" + membershipType);
            return Collections.EMPTY_SET;
        }
        
        Set groups = null;
        if (!type.equals(IdType.USER)) {
            debug.error("DatabaseRepo.getMemberships: Membership for identities"
                    + " other than Users is not allowed ");
            Object[] args = { PLUGIN_CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);
        } else {
            if (membershipType.equals(IdType.GROUP)) {
                groups = dao.getMemberships(name, membershipIdAttributeName);
            } else { // Memberships of any other types not supported for
                debug.error("DatabaseRepo.getMemberships: Membership for other"
                        + " types of entities not supported for Users");
                Object args[] = { PLUGIN_CLASS_NAME, type.getName(),
                        membershipType.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        }
        if(groups == null) {
            groups = Collections.EMPTY_SET;
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getMemberships: returning groups=" 
                    + groups);
        }
        return groups;
    }

    /*
     * (non-Javadoc)
     *
     * Modify membership of the identity. Set of members is
     * a set of unique identifiers of other identities.
     *
     * @see com.sun.identity.idm.IdRepo#modifyMemberShip(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Set, com.sun.identity.idm.IdType, int)
     */
    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.modifyMemberShip: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.modifyMemberShip called: " 
                + " token=" + token + " type=" + type +
                " name=" + name + " members=" + members +
                " membersType= " + membersType + " operation=" + operation);
        }
        //throw exception if this type user not allowed to do this
        //isValidType(type, "modifyMemberShip"); 
        
        if (type==null || name==null) {
            if (debug.messageEnabled()) {
              debug.message("DatabaseRepo.modifyMemberShip: parameters type and" 
                + " name can not be null. type=" + type
                + " name=" + name );
            }
            return; //maybe should throw exception instead?
        }
        if( !(operation==ADDMEMBER || operation==REMOVEMEMBER) ) {          
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.modifyMemberShip: parameter"
                        + " operation must have value equivalent to ADD or"
                        + " REMOVE. operation="   + operation);
            }
            return; //maybe should throw exception instead?
        }  
        if (members == null || members.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.modifyMemberShip: Members set"
                        + " is empty");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }
        if (type.equals(IdType.USER)) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.modifyMemberShip: Memberhsip" +
                        " to users is not supported");
            }
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "203", null);
        }
        if (!membersType.equals(IdType.USER)) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.modifyMemberShip: A non-user" +
                        " type cannot  be made a member of any identity"
                                + membersType.getName());
            }
            Object[] args = { PLUGIN_CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);
        }

        if (type.equals(IdType.GROUP)) {
            switch (operation) {
                    case ADDMEMBER:
                        dao.addMembersToGroup(members, name, membershipIdAttributeName);
                        break;
                    case REMOVEMEMBER:
                        dao.deleteMembersFromGroup(members, name, membershipIdAttributeName);
            }
        } else {
            debug.error("DatabaseRepo.modifyMemberShip: Memberships cannot be"
                    + "modified for type= " + type.getName());
            Object[] args = { PLUGIN_CLASS_NAME, type.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "209", args);
        }
    }

    /*
     *  Search for specific type of identities.
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, int, int,
     *      java.util.Set, boolean, int, java.util.Map, boolean)
     *
     * @param token
     *     Single signon token of identity performing the task.  (Not Using)
     * @param type
     *     Identity type of this object.
     * @param pattern
     *     pattern to search for. The pattern can either be an id, for example
     *     a user's id. Or pattern can be just a * which means all. Or pattern 
     *     can be a string that contains * such as searching for any ids that
     *     match *ea* like 'sean' , or it could be pattern with just one * in 
     *     front or back 
     *     if pattern is NULL or empty or "*" then they all mean pattern = "*"
     *     which is wildcard char. Note, wildcard searches can be modified as
     *     they are affected by other params like avPairs whxi add other 
     *     conditions to the seacrhes.
     * @param maxTime
     *     maximum wait time for search. (Not Using)
     * @param maxResults
     *     maximum records to return.
     * @param returnAttrs
     *     Set of attribute names to return. If this is null, then all 
     *     atrributes will be fetched and returned. If empty then no attributes
     *     will be fetched and returned, and just the set of ids will be 
     *     returned, and for each id it will have an empty set for values.
     * @param returnAllAttrs
     *     flag specifies if  should return all attributes for each id 
     *     that matches search. This overrides the setting of returnAttrs, so if
     *     this flag is true then all attributes will be fecthed and returned
     *     no matter what the value of returnAttrs parameter.
     * @param filterOp
     *     filter condition. For example IdRepo.OR_MOD or IdRepo.AND_MOD and 
     *     then the WHERE clause of SQL search will use this operand between
     *     the avPairs comparisons
     * @param avPairs
     *     additional search conditions. For example, these would be added to 
     *     the search query WHERE clause, like WHERE last_name = 'Jones' and 
     *     you could use the attribute-value in the map for column last_name
     *     and value 'Jones'.
     * @param recursive
     *     boolean to indicate recursive search? (Not Using)
     *
     * @return RepoSearchResults
     * @throws IdRepoException If there are repository related error conditions.
     * @throws SSOException If identity's single sign on token is invalid.
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, int maxTime, int maxResults, Set returnAttrs,
            boolean returnAllAttrs, int filterOp, Map avPairs, 
            boolean recursive) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.search: throwing"
                    + " initialization exception");
            throw (initializationException);
        }             
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo:search called with :"
                + " token=" + token + " IdType=" + type
                + " pattern=" + pattern + " maxTime=" + maxTime
                + " maxResults=" + maxResults + " returnAttrs=" + returnAttrs
                + " filter= " + filterOp + " avPairs= " + avPairs
                + " recursive=" + recursive);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "search");
        
        if (maxResults < 1) {
            maxResults = defaultSearchMaxResults;
            if (debug.messageEnabled()) {
              debug.message("DatabaseRepo:search changing value of maxResults"
                      + " to deafult, so now maxResults=" + maxResults);
            }
        }
        pattern = pattern.trim();       
        
        //?? SHOULD THE RETURNED LIST BE ORDERED ????
        
        //a set of Maps where each map is a user and their attributes
        Map<String, Map<String, Set<String>>> users = 
                              new HashMap<String, Map<String, Set<String>>>();
        
        //determine the set of attributes to fetch from the database
        Set<String> attributesToFetch = null;
        if (returnAttrs == null){
            //to fetch all user attributes, need to pass in all attr names
            if (type.equals(IdType.USER)) {
                attributesToFetch = userAtttributesAllowed;
            } else if (type.equals(IdType.GROUP)) {
                //RFE: treat groupAttrsAllowed in same way as userAtttributesAllowed
                Set<String> groupAttrsAllowed = new HashSet<String>();
                groupAttrsAllowed.add(membershipIdAttributeName);
                attributesToFetch = groupAttrsAllowed;
            }
        } else if(returnAttrs.isEmpty())  {
            //fetch just userIDs
            attributesToFetch = new HashSet<String>();
            if (type.equals(IdType.USER)) {
                attributesToFetch.add(userIDAttributeName);
            } else if (type.equals(IdType.GROUP)) {
                attributesToFetch.add(membershipIdAttributeName);
            }
        } else {       
                attributesToFetch = returnAttrs;
        }
        
        String filterOpString = "NONE"; //IdRepo.NO_MOD default is NONE
        if (filterOp == IdRepo.OR_MOD) {
            filterOpString = "OR";
        } else if (filterOp == IdRepo.AND_MOD) {
            filterOpString = "AND";
        }
        
        //what if pattern or values in avPairs contain wildcard chars of SQL
        //in SQL % allows you to match any string of any length
        //in SQL _ allows you to match on a single character
        //later consider if this case matters?
        
        if( (pattern == null || pattern.length()==0 || pattern.equals("*"))
                      && (avPairs==null || avPairs.isEmpty()) )  { 
            //get all users
            if (type.equals(IdType.USER)) {
                users = dao.search(userIDAttributeName, maxResults, "", attributesToFetch, filterOpString, avPairs);
            } else if (type.equals(IdType.GROUP)) {
                users = dao.searchForGroups(membershipIdAttributeName, maxResults, "", attributesToFetch, filterOpString, avPairs);
            }
        } else {
            //get users that match with the pattern
            //not sure if we need to differentiate between case where 
            // avPairs==null or empty ??? vs when avPairs has attrs and values??
            //AFAIK the searches on a pattern all include something in avPairs
            //  and those attrs/vals are used to searh for pattern matches
            
            //substitute % for * for sql LIKE query
            String searchPattern = pattern.replaceAll("\\*","%");
   
            //avPairs with values having wilcard chars replaced
            Map<String, Set<String>> avPairsChanged = 
                                     new HashMap<String, Set<String>>();         
            //need to replace % for * in all avPairs too
            if(avPairs!=null && !avPairs.isEmpty()) {
                Iterator KeysIt = avPairs.keySet().iterator();
                while(KeysIt.hasNext()) {
                    String key = (String)KeysIt.next();
                    if (key != null) {
                      Set<String> values = (Set<String>)avPairs.get(key);
                      Set<String> changedValues = new HashSet<String>();
                      if(values!= null && !values.isEmpty()) {                     
                        Iterator<String> valSetIt = values.iterator();
                        //modify each value to replace any wildcard chars
                        while(valSetIt.hasNext()) {
                          String attrValue = valSetIt.next();
                          if(attrValue!=null && attrValue.contains("*")) {
                            attrValue = attrValue.replaceAll("\\*","%");    
                          }
                          changedValues.add(attrValue);
                        }
                      }
                      //now that Set values has each value with new wildcard
                      //replace it in the changed avPairsMap
                      avPairsChanged.put(key, changedValues);
                    }
                }
            }
            if (type.equals(IdType.USER)) {
                users = dao.search(userIDAttributeName, maxResults, searchPattern, attributesToFetch, filterOpString, avPairsChanged);
            } else if (type.equals(IdType.GROUP)) {
                users = dao.searchForGroups(membershipIdAttributeName, maxResults, searchPattern, attributesToFetch, filterOpString, avPairsChanged);
            }
        }
        
        if (users == null) {
            return new RepoSearchResults(Collections.EMPTY_SET,
                    RepoSearchResults.SUCCESS, Collections.EMPTY_MAP, type);
        }
        if (users.isEmpty()) {
            return new RepoSearchResults(Collections.EMPTY_SET,
                    RepoSearchResults.SUCCESS, Collections.EMPTY_MAP, type);
        }       
        
        Set allUserIds = users.keySet();
        
        if(returnAttrs!= null && returnAttrs.isEmpty()){
            //I believe that is this case, we should only return the userids
            // and each Map is empty??? 
            //Or should it be the user id and for 
            //each user id the Set of just useridattrname=value ????
            //for now, just return userids and empty map
            
            //throw away any fetched attrs for each userid, if any
            users = new HashMap<String, Map<String, Set<String>>>();
            //now set each id's value set to an empty set
            for(Iterator<String> usersIt = allUserIds.iterator(); usersIt.hasNext(); ) {
                users.put(usersIt.next(), Collections.EMPTY_MAP);
            }
        }
        
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.search: returning users= " + users);
        }
        return (new RepoSearchResults(allUserIds, RepoSearchResults.SUCCESS,
                users, type));   
    
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedOperations(
     *      com.sun.identity.idm.IdType)
     */
    public Set getSupportedOperations(IdType type) {
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getSupportedOperations: supportedOps= "
                    + supportedOps);
        }
        return (Set) supportedOps.get(type);
    }
    
    /*
     * Load some default operations for different id types. This just sets up
     * each IdType with almost all possible opeartions being allowed.
     * Later they can be made more fine grained and they are changed in 
     * initialize method based on user provided list of types and allowed ops.
     *
     * If nothing is provided in idRepoService.xml so nothing is fetched in and
     * initialize hence nothing is set, then these defaults will still apply.
     *
     * When finished it has set this info in the class field Map supportedOps
     */
    private static void loadDefaultSupportedOps() {
        Set opSet = new HashSet();
        opSet.add(IdOperation.CREATE);
        opSet.add(IdOperation.DELETE);
        opSet.add(IdOperation.EDIT);
        opSet.add(IdOperation.READ);
        opSet.add(IdOperation.SERVICE);
        
        supportedOps.put(IdType.USER, Collections.unmodifiableSet(opSet));
        //supportedOps.put(IdType.REALM, Collections.unmodifiableSet(opSet));

        Set op2Set = new HashSet(opSet);
        op2Set.remove(IdOperation.SERVICE);
        supportedOps.put(IdType.GROUP, Collections.unmodifiableSet(op2Set));
        supportedOps.put(IdType.ROLE, Collections.unmodifiableSet(op2Set));
        //supportedOps.put(IdType.AGENT, Collections.unmodifiableSet(op2Set));       
        //supportedOps.put(IdType.FILTEREDROLE, Collections.unmodifiableSet(op2Set));
        
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.loadDefaultSupportedOps: load defaults" +
                "\n\t supportedOps=" + supportedOps);
        }
    }

    /*
     * Returns the supported types of identities for this
     * plugin. If a plugin does not override this method, it
     * returns an empty set.
     *
     * @return a Set of IdTypes supported by this plugin.
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedTypes()
     */
    public Set getSupportedTypes() {
        if (debug.messageEnabled()){
            debug.message("DatabaseRepo.getSupportedTypes: supported types"
                    + " supportedOps.keySet=" + supportedOps.keySet());
        }
        return supportedOps.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * Returns true if the <code> name </code> object exists in the data store.
     *
     * @see com.sun.identity.idm.IdRepo#isExists(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isExists(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.isExists: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.isExists:"
                + " token=" + token +  " IdType=" + type + " name= " + name);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "isExists");
        
        boolean entryExists = true;      
        Map userDetails = getAttributes(token, type, name);            
        if(userDetails.isEmpty()) {
            entryExists = false;
        }       
        return entryExists;
    }
    
    /**
     * Returns true if the <code> name </code> object is active
     * The convention is that a user is only considered inactive if the user 
     * active attribute is explicitly set to be inactive. 
     * If the user does not exist then also is considered inactive.
     * Otherwise if user exists and is not set to inactive, then user is active.
     * 
     * @see com.sun.identity.idm.IdRepo#isActive(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isActive(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {       
        if (initializationException != null) {
            debug.error("DatabaseRepo.isActive: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.isActive:"
                + " token=" + token +  " IdType=" + type + " name= " + name);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "isActive");       
        
        //get the row of this user's data and pull out their status column value
        Map attrMap = null;
        HashSet attrNameSet = new HashSet();
        attrNameSet.add(statusAttributeName);
        try {
            attrMap = getAttributes(token, type, name, attrNameSet);           
        } catch (IdRepoException idrepoerr) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.isActive calling getAttributes"
                        + " got IdRepoException=" + idrepoerr);
            }
            return false; //can't determine user existence so inactive
        }
        
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.isActive: query results fecthed for name="
                    + name + " retrieved attrMap=" + attrMap);
        }
        if(attrMap == null || attrMap.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.isActive: the fetching of attributes "
                    + " for user name=" + name 
                    + " got no results, either null or empty, which indicates"
                    + " user does not exists, so considered inactive.");
            }
            return false;
        }
        //Since user exists, now check if active
        
        //if alwaysActive flag is set and user exists then active 
        if (alwaysActive) {
            return true;
        }
        //check value of the active attribute for the user
        Set<String> activeValueSet = (Set<String>)(attrMap.get(statusAttributeName));        
        if (activeValueSet == null || activeValueSet.isEmpty()) {   
            return true; //no value specified for active attr, means active
        }    
        //in most cases this is not multi-valued, but just in case later it is
        //we will iterate thru values.
        //only if ALL values are INACTIVE, is user considered inactive
        //otherwise any other value means active
        boolean allValuesInactive = true;
        //check if ALL values are INACTIVE
        for(Iterator<String> it = activeValueSet.iterator(); it.hasNext(); ) {
            String activeVal = it.next();
            if (activeVal == null){
                allValuesInactive = false; //null means active
            } else if(!activeVal.equalsIgnoreCase(statusInActiveComparisonValue)) {
                //if value is anything other than "InActive" then its active
                allValuesInactive = false; ///
            }              
        }
        if (allValuesInactive) {
            return false;
        } else {
            return true;
        }
    }
    
    /*  
     * Sets the object's status to <code>active</code>.
     * @see com.sun.identity.idm.IdRepo#setActiveStatus(
     *  com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *   java.lang.String, boolean)
     */
    public void setActiveStatus(SSOToken token, IdType type, String name,
            boolean active) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.setActiveStatus: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.setActiveStatus method called with"
                + " token=" + token +  " IdType=" + type + " name= " + name
                + " active=" + active);
        }
        if(name==null || (name.length() == 0)) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.setActiveStatus: name is null or empty"
                        + " so can not set active status. name=" + name);
            }
            return;
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "setActiveStatus");
        Map attrs = new HashMap();
        Set vals = new HashSet();

        if (active) {
            vals.add(statusActiveComparisonValue);
        } else {
            vals.add(statusInActiveComparisonValue);
        }
        attrs.put(statusAttributeName, vals);
        setAttributes(token, type, name, attrs, false);
    }

    /* 
     * Returns the fully qualified name for the identity. It is expected that
     * the fully qualified name would be unique, hence it is recommended to
     * prefix the name with the data store name or protocol. Used by IdRepo
     * framework to check for equality of two identities
     */
    public String getFullyQualifiedName(SSOToken token, IdType type, 
            String name) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.getFullyQualifiedName: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo:getFullyQualifiedName: "
                    + " token=" + token +" IdType=" + type + " name=" + name);
        }       
        if ((name == null) || (name.length() == 0)) {
            Object[] args = { PLUGIN_CLASS_NAME, "" };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, 
                "220", args);
        }
        isValidType(type, "getFullyQualifiedName");

        //need to search for name and then make the url of datasource db
        
        RepoSearchResults results = search(token, type, name, 0, 2, null, true, 
                IdRepo.NO_MOD, null, false);
        
        Set dns = results.getSearchResults();
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo:getFullyQualifiedName: " +
                " search results dns=" + dns);
        }
        if (dns == null || dns.size() != 1) {
            String[] args = { PLUGIN_CLASS_NAME, name };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "220", args));
        }
        // example url is jdbc:mysql://localhost:3306/openssousersdb
        String dbURL = dao.getDataSourceURL();
        String fqdn = dbURL + "/" + type.getName() + "/" 
                + dns.iterator().next().toString();
        fqdn = fqdn.toLowerCase();
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo:getFullyQualifiedName: " +
                " about to return fqdn=" + fqdn);
        }
        return (fqdn);               
    }

    /*
     * Returns <code>true</code> if the data store supports authentication of
     * identities. Used by IdRepo framework to authenticate identities.
     */
    public boolean supportsAuthentication() {
        final boolean AUTHN_ENABLED = true;
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo:supportsAuthentication: " +
                " authenticationEnabled=" + AUTHN_ENABLED);
        }
        return (AUTHN_ENABLED);
    }

    /*
     * Returns <code>true</code> if the data store successfully authenticates
     * the identity with the provided credentials. In case the data store
     * requires additional credentials, the list would be returned via the
     * <code>IdRepoException</code> exception.
     */
    public boolean authenticate(Callback[] credentials) throws IdRepoException,
            AuthLoginException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.authenticate: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.authenticate method called with " +
                " credentials=" + credentials);
        }
              
        //Obtain user name and password from credentials and authenticate
        String username = null;
        String password = null;
        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof NameCallback) {
                username = ((NameCallback) credentials[i]).getName();
                if (debug.messageEnabled()) {
                    debug.message("DatabaseRepo.authenticate: username: " +
                        username);
                }
            } else if (credentials[i] instanceof PasswordCallback) {
                char[] passwd =((PasswordCallback)credentials[i]).getPassword();
                if (passwd != null) {
                    password = new String(passwd);
                    debug.message("DatabaseRepo.authenticate:authN passwd present");
                }
            }
        }
        if (username == null || password == null) {
            return (false);
        }

        // Get user's password attribute
        Map attrs = searchForAuthN(IdType.USER, username);       
        if ((attrs == null) || attrs.isEmpty() ||
                !attrs.containsKey(passwordAttributeName)) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.authenticate: did not found user.");
            }
            return (false);
        }
        Set storedPasswords = (Set) attrs.get(passwordAttributeName);
        if (storedPasswords == null || storedPasswords.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.authenticate: no stored password");
            }
            return (false);
        }
        String storedPassword = (String) storedPasswords.iterator().next();
        /**if (hashAttributes.contains(passwordAttributeName)) {
            password = Hash.hash(password);
        }
        */
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.authenticate: AuthN of " + username + "=" +
                password.equals(storedPassword));
        }
        return (password.equals(storedPassword));
    }
    
    private Map searchForAuthN(IdType type, String userName) 
        throws IdRepoException 
    {        
        Map attributes = null;
        try {
            attributes = getAttributes(null, type, userName);
            if(attributes !=null || !attributes.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message("DatabaseRepo.searchForAuthN: found " +
                            type.getName() + " entry: " + userName);
                }
            } else {
                 if (debug.messageEnabled()) {
                    debug.message("DatabaseRepo.searchForAuthN: did not find " + 
                        type.getName() + " entry: " + userName);
                } 
            }          
        } catch (SSOException ssoe) {
            // Can ignore this as this won't happen. No token was passed.
        }      
        return attributes;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#addListener(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdRepoListener)
     */
    public int addListener(SSOToken token, IdRepoListener listener)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.addListener called");
        }       
        //TO DO LATER   
        return 0;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#removeListener()
     */
    public void removeListener() {
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.removeListener called");
        }
        //TO DO LATER
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#assignService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      com.sun.identity.sm.SchemaType, java.util.Map)
     */
    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.assignService: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.assignService called with: " 
                    + " token=" + token + " type=" + type.getName()
                    + " name=" + name +  " serviceName=" + serviceName
                    + "\n\tSchema Type stype=" + stype
                    + "\n\t attrMap="  + "=" + attrMap);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "assignService");               
        //TO DO LATER      
    }

    /*
     * If the service is already assigned to the identity then
     * this method unassigns the service and removes the related
     * attributes from the entry.
     * 
     * @see com.sun.identity.idm.IdRepo#unassignService(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.lang.String, java.util.Map)
     */
    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap) throws IdRepoException,
            SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.unassignService: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.unassignService called with: " 
                    + " token=" + token + " type=" + type.getName()
                    + " name=" + name +  " serviceName=" + serviceName
                    + "\n\t attrMap="  + "=" + attrMap);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "unassignService");
        //TO DO LATER
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAssignedServices(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Map)
     */
    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServicesAndOCs) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.getAssignedServices: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getAssignedService called with:"
                    + " token=" + token + " type =" + type.getName()
                    + " name=" + name
                    + " mapOfServicesAndOCs=" + mapOfServicesAndOCs);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "getAssignedServices");
        
        //TO DO LATER
        return Collections.EMPTY_SET;
    }
    
    /*
     * (non-Javadoc)
     * 
     * Modifies the attribute values of the service attributes.
     *
     * @see com.sun.identity.idm.IdRepo#modifyService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      com.sun.identity.sm.SchemaType, java.util.Map)
     */
    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.modifyService: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.modifyService called with:"
                    + " token=" + token + " IdType=" + type
                    + " name=" + name + " serviceName=" + serviceName 
                    + " SchemaType stype=" + sType + "\n\t attrMap=" + attrMap);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "modifyService");
        //TO DO LATER       
    }
    
    /* 
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    public Map getServiceAttributes(SSOToken token, IdType type, String name,
        String serviceName, Set attrNames) throws IdRepoException,
        SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.getServiceAttributes: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getServiceAttributes called with:"
                    + " token=" + token + " type =" + type.getName()
                    + " name=" + name + " serviceName=" + serviceName
                    + " attrNames=" + attrNames);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "getServiceAttributes");        
        //TO DO LATER     
        return Collections.EMPTY_MAP;
        
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    public Map getBinaryServiceAttributes(SSOToken token, IdType type,
        String name, String serviceName, Set attrNames)
        throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("DatabaseRepo.getBinaryServiceAttributes: throwing"
                    + " initialization exception");
            throw (initializationException);
        }
        if (debug.messageEnabled()) {
            debug.message("DatabaseRepo.getBinaryServiceAttributes called with:"
                    + " token=" + token + " type =" + type.getName()
                    + " name=" + name + " serviceName=" + serviceName
                    + " attrNames=" + attrNames);
        }
        //throw exception if this type user not allowed to do this
        isValidType(type, "getBinaryServiceAttributes");        
        //TO DO LATER
        return Collections.EMPTY_MAP;
    }
    
    //throw exception if this type user not allowed to do this
    //@param methodName may be used if need to log any debug messages
    private void isValidType(IdType type, String methodName)
                                          throws IdRepoUnsupportedOpException {
        //if not a user type then should not execute
        // if (!type.equals(IdType.USER))
        if (type==null || !supportedOps.keySet().contains(type)) {
            if (debug.messageEnabled()) {
                debug.message("DatabaseRepo.isValidType: method " + methodName
                  + " was called with type=" 
                  + ( type ==null ? null: type.getName() )
                  + " but this is an unsupported operation for this type of"                        
                  + " user. So operation cannot be executed. Exception will be" 
                  + " thrown.");
            }
            Object args[] = { PLUGIN_CLASS_NAME, IdOperation.SERVICE.getName(),
                ( type ==null ? null: type.getName()) };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                "305", args);
        }
    }
}
