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
 * $Id: JdbcSimpleUserDao.java,v 1.2 2009/12/22 19:11:54 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2012 ForgeRock Inc 
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation 
 */
package com.sun.identity.idm.plugins.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.DriverManager;

import com.sun.identity.shared.debug.Debug;

/**
 * This class encapsulates all the JDBC code used to access identity
 * information in a database.
 */
public class JdbcSimpleUserDao implements DaoInterface {
    
    //member fields are protected scope, so that classes can extend this class
    //and inherit the fields and use them in their own methods if needed
    //for example an implementation could extend this and override the
    //membership methods and add an implementation for groups
    String userTableName;
    String membershipTableName;
    
    //determines whether to use JNDI or JBDC to get Connections to DB
    boolean useJNDI;
    String datasourceName; //for JNDI style connections
    DataSource datasource; //for JNDI style connections
    String jdbcDbDriver; //for JDBC style connections
    String jdbcDriverDbUrl; //for JDBC style connections
    String jdbcDbUser; //for JDBC style connections
    String jdbcDbPassword; //for JDBC style connections
    private static Debug debug;
    private boolean isMySQL = false;
    
    //used to identity this datasource by IdRepo layer code
    String databaseURL = null;
    
    static final String SPACE = " ";
    static final String COMMA = ",";
    
    public JdbcSimpleUserDao() {
    }
    
    /**
     * This class must be called before using the methods since the datasource
     * must be set up.
     * This version of initialize is used when connections are retreived using
     * a Java EE datasource resource which is configured through the application
     * server. For example if you use your server's ability to configure and
     * pool connections. This also requires a java:comp/env resource-ref 
     * in web.xml
     *
     * @throws java.lang.InstantiationException when not able to instantiate
     *      this class, for example if parameters are bad or null or can not
     *      make a connection to datasource.
     */
    public void initialize(String jndiName,
            String userDataBaseTableName, String membershipDataBaseTableName,
            Debug idRepoDebugLog) 
            throws java.lang.InstantiationException {
        
        useJNDI = true;
        //validate input parameters
        if( jndiName==null || jndiName.trim().length()==0
                || userDataBaseTableName==null 
                || userDataBaseTableName.trim().length()==0 
                || idRepoDebugLog == null
                || membershipDataBaseTableName == null) {
            String msg = "JdbcSimpleUserDao.initialize"
                 + " validation failed to make and make a new instance"
                 + " with paramaters: jndiName="+ jndiName 
                 + " userDataBaseTableName=" + userDataBaseTableName
                 + " membershipDataBaseTableName=" + membershipDataBaseTableName
                 + " debug="
                 + idRepoDebugLog==null ? null : idRepoDebugLog.getName();
            if (idRepoDebugLog!=null && idRepoDebugLog.messageEnabled()) {
                idRepoDebugLog.message(msg);
            }          
            throw new java.lang.InstantiationException(msg);
        }
        
        //set class fields to input paramater
        debug = idRepoDebugLog;
        datasourceName = jndiName.trim();
        userTableName = userDataBaseTableName.trim();
        //input value for membership table can be empty, but null is not allowed
        if (membershipDataBaseTableName != null) {            
            membershipTableName = membershipDataBaseTableName.trim();
        }
        
        //set the datasource class field        
        try {
            Context ctx = new InitialContext();
            //java:comp/env requires a resource-ref in web.xml
            datasource = (DataSource) ctx.lookup(datasourceName);            
        } catch (Exception ex) {
            String msg = "JdbcSimpleUserDao.getInstance:"
                        + " Not able to initialize the datasource through JNDI"
                        + " for datasourceName=" + datasourceName;
            if(debug.errorEnabled()) {
                debug.error(msg + " exception =" + ex);
            }
            //reset to un-initialized state
            datasourceName = null;
            datasource     = null;
            userTableName  = null;
            debug = null;
            throw new java.lang.InstantiationException(msg + ex.getMessage());
        }
               
        Connection con = null;
        try {
            //test out and log database info to debug log
            con = getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            if (debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.initialize: DB Meta Data:"
                        +  " name="
                        + (dbmd==null ? "Not Available" : dbmd.getUserName() )
                        + " url="
                        + (dbmd==null ? "Not Available" : dbmd.getURL() ));
            }
            databaseURL = (dbmd==null ? null : dbmd.getURL() );
            isMySQL = isMySQL(databaseURL);
            
        } catch (Exception ex) {
            String msg = "JdbcSimpleUserDao.getInstance:"
                        + " Not able to connect the datasource and get the meta"
                        + " data such as DB url";
            if(debug.errorEnabled()) {
                debug.error(msg + " exception =" + ex);
            }
            //reset to un-initialized state
            datasourceName = null;
            datasource     = null;
            userTableName  = null;
            membershipTableName = null;
            throw new java.lang.InstantiationException(msg + ex.getMessage());
        } finally {
            closeConnection(con);
            //do this last since debug is used by close connection
            //reset to un-initialized state
            if(datasourceName==null) {
                debug =null;
            }
        }
    }
    
    /**
     * This class must be called before using the methods since the datasource
     * must be set up.
     * This version of initialize is used when connections are retreived using
     * JDBC driver classes directly.
     *
     * @throws java.lang.InstantiationException when not able to instantiate
     *      this class, for example if parameters are bad or null or can not
     *      make a connection to datasource.
     */
    public void initialize(String jdbcDriver, String jdbcDriverUrl,
            String jdbcUser,String jdbcPassword, 
            String userDataBaseTableName, String membershipDataBaseTableName,
            Debug idRepoDebugLog)
            throws java.lang.InstantiationException {
        
         useJNDI = false;//will use JDBC DriverManager to get connections
         
         //validate input parameters
        if( jdbcDriver==null || jdbcDriver.trim().length()==0
                 || jdbcDriverUrl==null || jdbcDriverUrl.trim().length()==0
                 || jdbcUser==null || jdbcUser.trim().length()==0                
                 || jdbcPassword==null || jdbcPassword.trim().length()==0
                 || userDataBaseTableName==null 
                 || userDataBaseTableName.trim().length()==0
                 || idRepoDebugLog == null
                 || membershipDataBaseTableName == null) {
            String msg = "JdbcSimpleUserDao.initialize:"
                        + "  validation failed for paramaters:"
                        + " jdbcDriver=" + jdbcDriver 
                        + " jdbcDriverUrl=" + jdbcDriverUrl
                        + " jdbcUser=" + jdbcUser
                        + " jdbcPassword=" + jdbcPassword
                        + " userDataBaseTableName=" + userDataBaseTableName
                        + " membershipDataBaseTableName=" 
                        + membershipDataBaseTableName
                        + " debug="
                        + idRepoDebugLog==null ? null : idRepoDebugLog.getName(); 
            if (idRepoDebugLog!=null && idRepoDebugLog.messageEnabled()) {
                idRepoDebugLog.message(msg);
            }          
            throw new java.lang.InstantiationException(msg);
        }
         
        if (idRepoDebugLog.messageEnabled()) {
             idRepoDebugLog.message("JdbcSimpleUserDao.initialize: called with"
                      + "  the following paramaters:"
                      + " jdbcDriver=" + jdbcDriver 
                      + " jdbcDriverUrl=" + jdbcDriverUrl
                      + " jdbcUser=" + jdbcUser
                      + " jdbcPassword=" + jdbcPassword
                      + " userDataBaseTableName=" + userDataBaseTableName
                      + " membershipDataBaseTableName=" 
                      + membershipDataBaseTableName
                      + " debug="
                      + idRepoDebugLog==null ? null : idRepoDebugLog.getName()); 
        }
                                      
        //set class field to input paramater
        debug = idRepoDebugLog;        
        jdbcDbDriver = jdbcDriver.trim();
        jdbcDriverDbUrl = jdbcDriverUrl.trim();
        jdbcDbUser = jdbcUser.trim();
        jdbcDbPassword = jdbcPassword.trim();
        userTableName = userDataBaseTableName.trim();
        //input value for membership table can be empty, but null is not allowed
        if (membershipDataBaseTableName != null) {            
            membershipTableName = membershipDataBaseTableName.trim();
        }
        isMySQL = isMySQL(jdbcDriverDbUrl);
        
        try {
            Class.forName(jdbcDriver);
        } catch(ClassNotFoundException cnfe) {
            String msg = "JdbcSimpleUserDao.initialize: failed to load driver" 
                    + " class jdbcDriver=" + jdbcDriver
                    + " exception=" + cnfe.getMessage();
            if(debug.errorEnabled()) {
                debug.error(msg);
            }
            throw new java.lang.InstantiationException(msg);
        }
        
        //set the datasource class field
        Connection con = null;
        try {         
            //test it and print database info to debug log
            con = getConnection();
            DatabaseMetaData dbmd = con.getMetaData();
            if (debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.initialize: DB Meta Data:"
                        + " name="
                        + (dbmd==null ? "Not Available" : dbmd.getUserName() )
                        + " url="
                        + (dbmd==null ? "Not Available" : dbmd.getURL() ));
            }
            //set the databaseURL which is used to indentify the datastore
            databaseURL = (dbmd==null ? null : dbmd.getURL() );
        } catch (Exception ex) {
            String msg = "JdbcSimpleUserDao.getInstance: Not able to connect"
                    + " to the jdbc db and get the meta data such as DB url" 
                    + " exception =" + ex.getMessage();
            if(debug.errorEnabled()) {
                debug.error(msg);
            }                      
            //reset to un-initialized state                      
            userTableName = null;
            membershipTableName = null;
            throw new java.lang.InstantiationException(msg);
        } finally {
            closeConnection(con);
            //do this last since debug is used by close connection
            //reset to un-initialized state
            if(userTableName == null) {
                //if an exception occurred and userTableName was set to null
                //indicating an error on init.
                debug = null;
                jdbcDbDriver = null;                 
                jdbcDriverDbUrl = null;
                jdbcDbUser = null;
                jdbcDbPassword = null;
            }
        }
        
    }
    
    /**
     *
     * @param userID is user id
     * @param attrMap is a Map that contains attribute/column names as keys
     *        and values are Sets of values
     */
    public void updateUser(String userID, String userIDAttributeName, 
            Map<String, Set<String> > attrMap) {        
                             
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.updateUser: called with params"
                    + " id=" + userID 
                    + " userIDAttributeName=" + userIDAttributeName
                    + " attrMap =\n" + attrMap);
        }        
        if (userID == null || userID.trim().length() == 0
                || userIDAttributeName == null 
                || userIDAttributeName.trim().length() == 0
                || attrMap == null || attrMap.isEmpty() ) {
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.updateUser: one or more of"
                    + " parameters is null or empty, so not executing update"
                    + " command. userID=" + userID + " userIDAttributeName="
                    + userIDAttributeName + " attrMap=" + attrMap);
            }
            return;
        }
        userID = userID.trim();
        userIDAttributeName = userIDAttributeName.trim();
        
        /**
         *  RFE: make sure update does not mess up referential integrity...
        //dont want to change primary key id uid so lets just get rid of 
        if (attrMap.containsKey(primaryKetAttrname)){
            attrMap.remove("uid");
        }
        **/
        
        //FIX: need to consider multi-valued attributes later
        //FIX: Need to make sure all required(non null) attrs/columns has values
        //matches the column names in DB and in prepared statement
        Map<Integer, String> positionMap = new HashMap<Integer, String>();
        String updateUserStmt = "UPDATE" + SPACE + userTableName + SPACE
                                + "SET" + SPACE;
         //build update statement from attrMap
        Set<String> attrKeySet = attrMap.keySet();
        Iterator<String> attrs = attrKeySet.iterator();              
        //query will look like 
        //   UPDATE userTable SET givenname = ?, sn = ? WHERE uid = ?"
        for(int position=1; attrs.hasNext(); position++) {
            String attr = attrs.next();
            positionMap.put(position, attr);
            if (isMySQL) {
                updateUserStmt = updateUserStmt + SPACE + "`" + attr + "`";
            } else {
                updateUserStmt = updateUserStmt + SPACE + attr;
            }
            updateUserStmt = updateUserStmt + SPACE + "= ?";
            if(attrs.hasNext()) {
                updateUserStmt = updateUserStmt + COMMA;
            }
        }
        updateUserStmt= updateUserStmt + SPACE + "WHERE" + SPACE 
                + userIDAttributeName + SPACE + "= ?";     
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.create: SQL update statement = "
                    + updateUserStmt);
        }
        
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            stmt = con.prepareStatement(updateUserStmt);
            //FIX: later deal better with various types and multi-valued attrs
            for (int i=1; i<=positionMap.size(); i++){                
                String keyAtPosition = positionMap.get(i);
                Set<String> valSet = attrMap.get(keyAtPosition);
                if (valSet != null && !valSet.isEmpty()) {
                  Iterator<String> it = valSet.iterator();
                  String value = null;//null may be a valid value if not required column
                  if(it.hasNext()) {
                    value = it.next();                                
                  }
                  //what if value == null, should I use setNull() ???
                  stmt.setString(i, value);
                }
            }
            int uidIndexPosition = positionMap.size() + 1;
            stmt.setString(uidIndexPosition, userID); //add uid for where clause         
            stmt.executeUpdate();
        } catch (Exception ex1) {
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.updateUser:" + ex1);
            }
            throw new RuntimeException(ex1);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }
    
    /*
     * @param userID is user id of user to delete from DB
     * @param userIDAttributeName is the attribute/column name of the user id
     *         in the DB table 
     *
     */
    public void deleteUser(String userID, String userIDAttributeName) {
        //would be good to put a limit LIMIT=1 on this SQL statement to ensure
        //at most one user is deleted and avoid accidently deleting more
        //but LIMIT is not really portable SQL
        final String DELETE_USER_STMT = "DELETE FROM" + SPACE + userTableName 
                + SPACE + "WHERE" + SPACE + userIDAttributeName
                + SPACE + "= ?";
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.delete: called parameters"
                    + " userID=" + userID
                    + " userIDAttributeName=" + userIDAttributeName);
            
            debug.message("JdbcSimpleUserDao.delete: SQL delete statement = "
                    + DELETE_USER_STMT);
        }
        if (userID == null || userID.trim().length() == 0
                || userIDAttributeName==null 
                || userIDAttributeName.trim().length() == 0) {
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.delete: parameters userID and"
                    + " userIDAttributeName can not be null or empty"
                    + " so not executing delete command. userID=" + userID
                    + " userIDAttributeName=" + userIDAttributeName);
            }
            return;
        }
        userID = userID.trim();
        userIDAttributeName = userIDAttributeName.trim();

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            stmt = con.prepareStatement(DELETE_USER_STMT);           
            //what if value == null, should I use setNull() ???
            stmt.setString(1, userID); 
            stmt.executeUpdate();
        } catch (Exception ex1) {
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.deleteUser:" + ex1);
            }
            throw new RuntimeException(ex1);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }
    
    /*
     * @param userIDAttributeName is attribute name of the userid. Could be 
     *                      used to check if user exists before trying to create
     *              
     * @param attrMap is a Map that contains attribute/column names as keys
     *        and values are Sets ??? of values
     *
     * @return userID string for newly created user if successful
     *                or returns null if user already exists or unsuccessful.
     *
     */
    public String createUser(String userIDAttributeName, Map<String, Set<String> > attrMap) {        
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.create: called with parameters"
                    + "userIDAttributeName=" + userIDAttributeName 
                    + " attrMap =\n" + attrMap);
        }       
        if (attrMap == null || attrMap.isEmpty()) {
            if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.createUser: attrMap is null or"
                    + " empty so not executing create command. attrMap="
                    + attrMap);
            }
            return null;
        }
        if (userIDAttributeName == null || userIDAttributeName.length() == 0) {
            if(debug.messageEnabled()) {
               debug.message("JdbcSimpleUserDao.createUser: userIDAttributeName"
                    + "  is null or empty so not executing create command."
                    + "  userIDAttributeName" + userIDAttributeName);
            }
            return null;
        }
        //in future, can use userID to check if user exists already so do not 
        //execute a create statement on db, or could just throw a duplicate
        //exception if user alredy exists
        //for now just use to return the name of user created
        //make sure that for create we at least have the required columns, which
        //at this time are at least the userIDAttributeName
        String userID = null;
        Set<String> userNameVals = attrMap.get(userIDAttributeName);
        if (userNameVals != null || !userNameVals.isEmpty()) {
            Iterator<String> nameIt = userNameVals.iterator();
            userID = nameIt.next();
        }                
        if(userID == null || userID.length()==0) {
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.createUser: the unique user id"
                    + "userIDAttributeName in attrMap is present but the value"
                    + "  mapped to key is null or empty so not executing create"
                    + "   command. Value of userID=" + userID);
            }
            return null;
        }
                
         //build query from attrMap
        Set<String> attrKeySet = attrMap.keySet();
        Iterator<String> attrs = attrKeySet.iterator();
        Map<Integer, String> positionMap_2 = new HashMap<Integer, String>();
        
        String createUserStmt = "INSERT INTO " + userTableName + SPACE + "(";
        
        for(int position=1; attrs.hasNext(); position++) {
            String attr = attrs.next();
            positionMap_2.put(position, attr);
            if (isMySQL) {
                createUserStmt = createUserStmt + "`" + attr + "`";
            } else {
                createUserStmt = createUserStmt + attr;
            }
            if(attrs.hasNext()) {
                createUserStmt = createUserStmt + COMMA;
            }
        }
        createUserStmt = createUserStmt + ") VALUES (";
        //add a question mark to sql string for each attribute/parameter
        for(int i =0;i<positionMap_2.size();i++) {
            createUserStmt += "?";
            if(i != (positionMap_2.size()-1)) {
                createUserStmt += ","; //add comma to all except last
            }
        }
        createUserStmt = createUserStmt + ")";//end the sql statement
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.create: SQL create statement = "
                    + createUserStmt);
        }
        
        //RFE: nice to check that user does not already exist, give a message
        
        //RFE: need to consider multi-valued attributes later
        //RFE: Need to make sure all required(non null) attrs/columns have values
        //RFE: what if attrMap == null or empty, throw exception??
        //now need to create a list of all attributes and make sure they have
        //an assigned value or null
        //so go thru attrMap input param, pull out all values and set them?                      
        //convert to a Map where values are just strings and not Sets
        //check null values
        //assume they wont put in any unused keys, since they will be ignored
        String val = null;
        String key = null;
        Map<String,String> fullAttrMap = new HashMap<String,String>();
        for(int i=1; i<= positionMap_2.size(); i++) {
            //reset to null each loop, null is a valid value
            val = null;             
            //key will not be null since iterating thru count of 
            //positionMaps elements
            key = positionMap_2.get(i);
            //when value for key is not found ...just leave as null            
            Set valSet = null;
            valSet = attrMap.get(key);                  
            //get the value of the attribute
            if (valSet != null) {
                Iterator<String> it = valSet.iterator();
                //assume single value, not multi-valued for now
                if (it.hasNext()) {
                    val = it.next();
                }
            }           
            fullAttrMap.put(key, val);
        }
        
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.create: attributes and values to be"
                    + " created for user =\n" + fullAttrMap);
        }       
             
        Connection con = null;
        PreparedStatement stmt = null;
        //RFE: later deal better with various types
        try {
            con = getConnection();
            stmt = con.prepareStatement(createUserStmt);           
            //prepared statements start counting at 1
            int startingIndexPosition = 1;
            for (int i=startingIndexPosition; i<=positionMap_2.size(); i++){
                //get key at index position i
                String keyAtPosition = positionMap_2.get(i);
                String value = fullAttrMap.get(keyAtPosition);              
                //what if value == null, should I use setNull() ???
                stmt.setString(i, value); 
            }
            stmt.executeUpdate();
        } catch (Exception ex1) {
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.createUser:" + ex1);
            }
            throw new RuntimeException(ex1);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
        
        return userID;
    }
  
    /**
     * gets values for the user attributes specified in the atributesToFetch
     * set. Normalizes the types by converting all the individual values into
     * Strings.
     *
     * @param userID is String of the users unique identifier
     * @param userIDAttributeName is the column name of the id field so is used
     *                            for example in the where clause of SQL
     *                            WHERE userIDAttributeName = userID                          
     * @param attributesToFetch is the set of column names for the attributes 
     *         that should be fetched from the DB table.
     *
     * @return user a map of Maps where each map is a user and their attributes
     */
    public Map<String, Set<String>> getAttributes(String userID, 
            String userIDAttributeName,
            Set<String> attributesToFetch) {

        Map<String, Set<String>> user = Collections.EMPTY_MAP;
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.getAttributes method called with"
                    + " parameters: userID="+ userID
                    + " userIDAttributeName=" + userIDAttributeName
                    + " attributesToFetch=" + attributesToFetch);
        }
        //validate input
        if (userID==null || userID.length()==0
               || userIDAttributeName==null || userIDAttributeName.length()==0
               || attributesToFetch==null || attributesToFetch.isEmpty() ) {
            if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.getAttributes: userID or"
                    + " userIDAttributeNameis null or empty"
                    + " so not executing getAttributes command. userID="+ userID
                    + " and userIDAttributeName=" + userIDAttributeName
                    + " and attributesToFetch=" + attributesToFetch);
            }
            return user;
        }
        userID = userID.trim();
        userIDAttributeName = userIDAttributeName.trim();        
        
        //build prepared statement query
        String selectUserStmt = "SELECT " + SPACE;  
        //note, keep this positionMap since will use it again to assign values
        //in result set fecthing
        ArrayList<String> positionMap  = new ArrayList<String>();
        positionMap.addAll(attributesToFetch);
         //add attribute names from attributesToFetch to SQL query, so query
        //will look like, SELECT uid,cn,fn,etc,etc for all attr names
        for(int i = 0; i< positionMap.size(); i++){
            String attr = positionMap.get(i);
            if (attr != null && (attr.length() != 0)) {
                if (isMySQL) {
                    selectUserStmt = selectUserStmt + "`" + attr + "`";
                } else {
                    selectUserStmt = selectUserStmt + attr;
                }
                if(i < (positionMap.size()-1) ) {
                    selectUserStmt = selectUserStmt + COMMA + SPACE;
                }
            }
        }
        selectUserStmt = selectUserStmt + SPACE + "FROM" + SPACE 
                + userTableName + SPACE
                + "WHERE" + SPACE + userIDAttributeName + "= ?";        
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.getAttributes: SQL select statement = "
                    + selectUserStmt);
        }
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        
        user = new HashMap<String, Set<String>>();
        try {
            con = getConnection();
            stmt = con.prepareStatement(selectUserStmt);         
            stmt.setString(1, userID);
            result = stmt.executeQuery();
            
            //actually the result set should only have ONE record,
            //do I need to check if more than one row then what??? maybe not?
            if (result.next() ) {
                Set values = null;
                String v = null;     
                //should I put in a check to make sure that 
                //  postionMap.size == resultSet number columns?
                for(int i = 0; i< positionMap.size(); i++){
                    String attrName = positionMap.get(i);
                    if (attrName != null && (attrName.length() != 0)) {
                        values = new HashSet();
                        //result set counting starts at 1 so add 1
                        v = result.getString(i + 1); 
                        if (v !=null) values.add(v);
                        user.put(attrName, values);
                    }
                }                                    
                if(debug.messageEnabled()) {
                    debug.message("JdbcSimpleUserDao.getAttributes: user details"
                        + " fetched from DB for user="+ userID + "::\n" + user);
                }
            }
            
        } catch (Exception ex1) {
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.getAttributes:" + ex1);
            }
            throw new RuntimeException(ex1);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
        return user;
    }

  /**
     * Fetch the set of users and including for each user all their attributes
     * specified in attributesToFetch.
     *
     * If there is an attribute being asked for in attributesToFetch but that
     * attribute is not present in the DB table then currently that attribute
     * is not included in the returned Map of each user. For example, if the
     * attributesToFetch includes "address" but there is no address column in
     * the DB table and no "address" included in the result set for each row,
     * then neither the attribute "address" of any value is put into the set of
     * attributes for each user. Is this correct ????
     *
     * @param idPattern which is used in an SQL LIKE query on the id attribute
     *          usually contains some SQL search chars like % to broaden matches
     *          WHERE id_attribute LIKE 'the_pattern_value'
     *          If idPattern is empty or null then it means to get all users
     *          so do not need to use a LIKE clause on query
     * @param attributesToFetch is set of desired attributes to fetch for
     *         each user.
     *         If EMPTY or NULL, then will not do anything, return empty map.
     *         Callers should be sure to specify which attributes to fetch.
     * @param limit is maximum number of results to return. This is added to the
     *          END of the WHERE clause using mysql LIMIT on a query.
     *          Default if no limit(if limit<0) is specified is LIMIT=1
     *          Limit is ignored in this implementation since it is not
     *          portable SQL
     * @param filterOperand is a string of AND, or OR and is used to add
     *         extra attributes and values to the WHERE search clause, and it is
     *         applied between attribute = 'value' pairs in the parameter
     *         for avPairs.
     * @param avPairs is a map of attribute names as the keys and the associated
     *         values are a SET for each attribute name, and would be added to
     *         the WHERE clause after the isPattern part of clause and before
     *         the LIMIT part of the where clause, for example
     *         WHERE id_attribute LIKE 'the_pattern_value' AND
     *           (attr_1='value1' ...filterOperand/AND/OR... attr_2='value2' )
     *                LIMIT 2
     * @return a set of Maps where each map is a user and their attributes
     */
    public Map<String, Map<String, Set<String>>>  search(
            String userIDAttributeName, int limit, String idPattern, 
            Set<String> attributesToFetch,
            String filterOperand, Map<String, Set<String>> avPairs) {
        
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.search called with: " 
                    +  " userIDAttributeName=" + userIDAttributeName 
                    +  " limit=" + limit + " idPattern=" + idPattern
                    +  " attributesToFetch=" + attributesToFetch
                    +  " filterOperand=" + filterOperand
                    +  " avPairs=" + avPairs);
        }
                  
        if(limit <=0) limit = 1; //default to 1 user 
        if (idPattern != null) idPattern = idPattern.trim();
        if (filterOperand != null) filterOperand = filterOperand.trim();       
        if (userIDAttributeName == null || userIDAttributeName.length()==0) {
            //just log it and return empty
            //this really should not happen, caller should avoid this
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.search: will not execute any"
                    + " SQL queries since userIDAttributeName is null or empty." 
                    + " search method should always be called with the "
                    + " userIDAttributeName specified so that it always fetches"
                    + " that attribute as part of return value."
                    +  " userIDAttributeName=" + userIDAttributeName);
            }
            return Collections.EMPTY_MAP;
        }        
        userIDAttributeName = userIDAttributeName.trim();
        
        String selectQueryString = "SELECT" + SPACE;
        //position map to be used later in result set fetching
        Map<Integer, String> rsPositionMap = new HashMap<Integer, String>();
        
        if(attributesToFetch == null || attributesToFetch.isEmpty()) { 
            //just log it and return empty
            //this really should not happen, caller should avoid this
            if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.search: will not execute any"
                    + " SQL queries since attributesToFetch is null or empty." 
                    + " search method should always be called with the set of "
                    + " attributes to fetch specified."
                    +  " attributesToFetch=" + attributesToFetch);
            }
            return Collections.EMPTY_MAP;
        }
        
        //always be sure that uid column is included so that it is 
        // always fetched
        if (!attributesToFetch.contains(userIDAttributeName)) {
                attributesToFetch.add(userIDAttributeName);
        }                     

        //just fetch the selected attributes
        //now build the query
        Iterator<String> attrs = attributesToFetch.iterator();            
        for(int position=1; attrs.hasNext(); position++) {
                String attr = attrs.next();
                rsPositionMap.put(position, attr);
                selectQueryString = selectQueryString + attr;               
                if(attrs.hasNext()) {
                    selectQueryString = selectQueryString + COMMA;
                }
        }                   
        
        selectQueryString = selectQueryString + SPACE + "FROM" + SPACE 
                + userTableName + SPACE;    
        
          //need to change to a  ? for prepared statement
        String LIMIT_CLAUSE = " LIMIT " + limit;
       
        //default query
        String queryToRun = selectQueryString;
        
        //maybe should validate pattern to make sure it is a valid SQL Like
        //   pattern containing only % or - or other LIKE characters?
 
        //position map to be used in binding query params
        //  will need to add to this each time I add a question mark ?
        Map<Integer, String> avPairsBindingPositionMap =
                                            new HashMap<Integer, String>();
        int avPairsBindPosMapCount = 0;
      
        String QUERY_NO_PATTERN_TYPE = "no_pattern";
        String QUERY_LIKE_TYPE = "like";
        String QUERY_LITERAL_TYPE = "literal";
        String queryType = null;
            
        final String WHERE_ID_EQUALS_PATTERN_QUERY_STR =
                SPACE + "WHERE" + SPACE + userIDAttributeName + SPACE
               + "=" + SPACE + "?";
      
        if(idPattern == null || idPattern.length()==0) {
            //no pattern so no LIKE clause, so select all users
            queryType = QUERY_NO_PATTERN_TYPE;            
        } else if(idPattern.contains("%")) {
            //LIKE query ...
            queryType = QUERY_LIKE_TYPE;
        } else { //pattern is a literal id of a user
            queryType = QUERY_LITERAL_TYPE;
            queryToRun += WHERE_ID_EQUALS_PATTERN_QUERY_STR;           
            //later value needs to be set to idPattern, for preparedstatement
            //         userIDAttributeName = idPattern         
        }
        
        //add on avPairs with filterop
        if (filterOperand != null &&
                (filterOperand.equals("AND") || filterOperand.equals("OR"))
                                   && avPairs!=null && !avPairs.isEmpty() ) {            
       
            //build string representation of avPairs map to be used as an extra
            //on the WHERE part of the clause of the query that will be run.
            //Result for example might look like :
            // (last_name='Brydon')
            // (first_name='Sean' AND last_name='Brydon')
            //  CHANGED to PreparedStatements would be 
            //  (first_name=? AND last_name=?)
            //     and avPairsBindingPositionMap would contain the 2 values Sean and Brydon
            StringBuilder sb = new StringBuilder();
            for(Iterator<Map.Entry<String, Set<String>>> avPairsIt = avPairs.entrySet().iterator(); avPairsIt.hasNext(); ){
                Map.Entry<String, Set<String>> me = avPairsIt.next();
                String attrName = me.getKey();
                if (attrName != null || attrName.length()!=0) {
                    Set<String> values =  me.getValue();
                    for(Iterator<String> valuesIt = values.iterator(); valuesIt.hasNext(); ){
                        String v = valuesIt.next();
                        //not sure what to do if v==emptystring
                        //so for now will just add empty string as a value 
                        //also NULL could be a valid value in some cases?
                        if (v != null) {
                            if(sb.length()==0 ) {
                                sb.append(SPACE + "(");
                            } else {
                                sb.append(SPACE).append(filterOperand).append(SPACE);
                            }
                            //save the value to later set in perparedstatement ?
                            avPairsBindPosMapCount++;
                            avPairsBindingPositionMap.put(avPairsBindPosMapCount, v);
                            //add the attr to be set attr=? for preparedstatement
                            if (queryType.equals(QUERY_LIKE_TYPE)) {
                                sb.append(SPACE).append(attrName).append(" LIKE ? ");
                            } else {
                                sb.append(SPACE).append(attrName).append("= ? ");
                            }
                        }
                    }
                }
            }
            if(sb.length()!=0) sb.append(")");
            String s = sb.toString();      
       
            if(s!=null && s.length()!=0) {
                if (queryType.equals(QUERY_NO_PATTERN_TYPE)) {
                    queryToRun += " WHERE " + s;
                } else if (queryType.equals(QUERY_LIKE_TYPE)) {
                    //queryToRun += " AND " + s;
                    queryToRun += " WHERE " + s;
                } else if (queryType.equals(QUERY_LITERAL_TYPE)) {
                    queryToRun += " AND " + s;
                }
            } 
        }
        // currently ignored. Limit is uppoted on MySQL but not all DBs
        //queryToRun += LIMIT_CLAUSE;       
        if(debug.messageEnabled()) {
            debug.message("JdbcSimpleUserDao.search: the query string =\n"
                    + queryToRun);
        }     

        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
                
        Map<String, Map<String, Set<String>>> allUsers = 
                           new HashMap<String, Map<String, Set<String>>>();
        try {
            con = getConnection();
            stmt = con.prepareStatement(queryToRun);       
            //set avPair values   
            //prepared statements start counting at 1
            int startingIndexPosition = 0;
            /**if ( queryType.equals(QUERY_LIKE_TYPE) 
                  || queryType.equals(QUERY_LITERAL_TYPE)) {
             ***/
            if (queryType.equals(QUERY_LITERAL_TYPE)) {
                //set first "?" to idPattern as value
                startingIndexPosition++;    
                stmt.setString(startingIndexPosition, idPattern);                
            }
            for (int i= 1; i<=avPairsBindingPositionMap.size(); i++){
                //do I need this or can I just stick that value in bindmap ???        
                    //get value at index position i
                    String value = avPairsBindingPositionMap.get(i);            
                    //what if value == null, should I use setNull() ??? 
                    stmt.setString(i+ startingIndexPosition, value); 
            }          
            result = stmt.executeQuery();
            
            while( result.next() ) {
                //for each user (row returned from DB) make a map of each
                //attribute and its set of values
                Map<String, Set<String>> user = new HashMap<String, Set<String>>();
                String userID = null;
                Set<String> values = null;
                String v = null;     
                //should I put in a check to make sure that 
                //  rsPositionMap.size == resultSet number columns?
                for(int i = 0; i< rsPositionMap.size(); i++){
                    String attrName = rsPositionMap.get(i + 1);
                    if (attrName != null && (attrName.length() != 0)) {
                        values = new HashSet();
                        //result set counting starts at 1 so add 1
                        v = result.getString(i + 1); 
                        if (v !=null) {
                            values.add(v);
                            //need to save uid for later
                            if(attrName.equals(userIDAttributeName)) userID = v;
                        }
                        user.put(attrName, values);
                    }
                }
                if(debug.messageEnabled()) {
                    debug.message("JdbcSimpleUserDao.search: user details"
                        + " fetched from DB for user="+ userID + "::\n" + user);
                }
                //add the userid as key and the map of all the users attribute
                //names and values as the value
                allUsers.put(userID, user);
            }
            
        } catch (Exception ex1) {
            if(debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.search:" + ex1);
            }
            throw new RuntimeException(ex1);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
               
        if (allUsers == null ) {
            return Collections.EMPTY_MAP;
        }
        if (allUsers.isEmpty()) {
            return Collections.EMPTY_MAP;
        }

        return allUsers;
    }
    
    /**
     * get the url of the current database.
     * @return a url of the current db connection, should be of the form
     *         jdbc:mysql://localhost:3306/seantestdb1
     * It is used by the IdRepo implementation to provide a fully qualified
     * domain name for users, and this value serves as sort of the prefix.
     */
    public String getDataSourceURL() {
        return databaseURL;
    }
    
    public Set<String> getMembers(String groupName, String membershipIdAttributeName) {
        return Collections.EMPTY_SET;
    }
     
    public Set<String> getMemberships(String userName, String membershipIdAttributeName) {
        return Collections.EMPTY_SET;
    }
    
    public void deleteGroup(String groupName, String membershipIdAttributeName) {        
    }
    
    public void createGroup(String groupName, String membershipIdAttributeName){
    }
    
    public void deleteMembersFromGroup(Set<String> members, String groupName, String membershipIdAttributeName) {
    }
    
    public void addMembersToGroup(Set<String> members, String groupName, String membershipIdAttributeName) {
    }
    
    public Map<String, Map<String, Set<String>>>  searchForGroups(
            String membershipIdAttributeName, int limit, String idPattern,
            Set<String> attributesToFetch, String filterOperand, 
            Map<String, Set<String>> avPairs) {
        
        return Collections.EMPTY_MAP;
    }
    
    public Map<String, Set<String>> getGroupAttributes(String groupName, 
            String membershipIdAttributeName, Set<String> attributesToFetch) {
        return Collections.EMPTY_MAP;
    }
    
    
    private Connection getConnection() throws SQLException {
        Connection conn = null;
        if (useJNDI) {
            if (debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.getConnection, about to try"
                        + " to get a JNDI datastore connection to DB.");
            }
            conn = datasource.getConnection();
            //} 
        }  else { //use JDBC DriverManager to get connections
             if (debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.getConnection, about to try"
                        + " to get a JDBC driver connection to DB.");
            }
            conn = DriverManager.getConnection(jdbcDriverDbUrl, jdbcDbUser,
                        jdbcDbPassword);            
        }
        return conn;
    }
    
    //I could move these methods below to a helper class later if desired,
    //since they are not db-specific or table-specific or mysql-specific...
    //just general jdbc. Would make them static if in another helper class
    //but it would require the debug class
    
    //For Now they are PRIVATE so I can make sure debug has already been
    //set before calling and I can leave them as static methods
    
    //should I catch all Exceptions instead of just SQL ????? I think so
    private static void closeConnection(Connection dbConnection) {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException se) {
            if (debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.closeConnection: SQL Exception"
                        + " while closing DB connection: \n" + se);
            }
        }
    }
    
    //should I catch all Exceptions instead of just SQL ????? I think so
    private static void closeResultSet(ResultSet result) {
        try {
            if (result != null) {
                result.close();
            }
        } catch (SQLException se) {
            if (debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.closeResultSet: SQL Exception"
                        + " while closing Result Set: \n" + se);
            }
        }
    }
    
    //should I catch all Exceptions instead of just SQL ????? I think so
    private static void closeStatement(PreparedStatement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException se) {
            if (debug.messageEnabled()) {
                debug.message("JdbcSimpleUserDao.closeStatement: SQL Exception"
                        + " while closing Statement : \n" + se);
            }
        }
    }

    // return true if a parameter url includes "mysql"
    private boolean isMySQL(String url) {
        if (url != null && url.toLowerCase().indexOf("oracle") != -1) {
            return false;
        } else if (url != null && url.toLowerCase().indexOf("mysql") != -1) {
            return true;
        } else {
            if (debug.warningEnabled()) {
                debug.warning(
                        "JdbcSimpleUserDao.isMySQL: Unknown jdbc driver url:" + url);
            }
            return false;
        }
    }
}
