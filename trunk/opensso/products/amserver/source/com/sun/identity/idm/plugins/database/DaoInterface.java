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
 * $Id: DaoInterface.java,v 1.1 2009/04/21 20:04:48 sean_brydon Exp $
 */
package com.sun.identity.idm.plugins.database;

import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import java.util.Set;

/**
 * Classes that implement this interface are expected to conatin code that
 * accesses a datastore, such as JBDC code to access a database, and 
 * create, read, update, delete, and search users and user related attributes.
 *
 * No matter what technology is used to access the datastore or what format
 * or type the data may be, it should be converted to Strings before being 
 * returned to the opensso layer and calling code, as indicated
 * in the method interfaces.
 * An IdRepo.java implementatiuon class like JDBCSimpleUserDao.java is an
 * example of the expected client that would be calling these methods.
 */
public interface DaoInterface {
    public void initialize(String jndiName,
            String userDataBaseTableName, 
            String membershipDataBaseTableName,
            Debug idRepoDebugLog)
            throws java.lang.InstantiationException;
    
    public void initialize(String jdbcDriver, String jdbcDriverUrl,
            String jdbcDbUser,String jdbcDbPassword, 
            String userDataBaseTableName, 
            String membershipDataBaseTableName, Debug idRepoDebugLog)
            throws java.lang.InstantiationException;
    
    public void updateUser(String userID, String userIDAttributeName,
            Map<String, Set<String> > attrMap);
    
    public void deleteUser(String userID, String userIDAttributeName);
    
    public String createUser(String userIDAttributeName, 
                                    Map<String, Set<String> > attrMap);
    
    public Map<String, Set<String>> getAttributes(String userID, 
            String userIDAttributeName,
            Set<String> attributesToFetch);
    
    public Map<String, Map<String, Set<String>>>  search(
            String userIDAttributeName, int limit, String idPattern, 
            Set<String> attributesToFetch, String filterOperand, 
            Map<String, Set<String>> avPairs);
    
    /**
     * get the url of the current database.
     * @return a url of the current db connection, should be of the form
     *         jdbc:mysql://localhost:3306/seantestdb1
     * It is used by the IdRepo implementation to provide a fully qualified
     * domain name for users, and this value serves as sort of the prefix.
     */
    public String getDataSourceURL();
    
    public Set<String> getMembers(String groupName, 
            String membershipIdAttributeName);
     
    public Set<String> getMemberships(String userName, 
            String membershipIdAttributeName);
    
    public void deleteGroup(String groupName,
            String membershipIdAttributeName);
    
    public void createGroup(String groupName,
            String membershipIdAttributeName);
    
    public void deleteMembersFromGroup(Set<String> members, String groupName,
            String membershipIdAttributeName);
    
    public void addMembersToGroup(Set<String> members, String groupName,
            String membershipIdAttributeName);
    
    public Map<String, Map<String, Set<String>>>  searchForGroups(
            String membershipIdAttributeName, int limit, String idPattern,
            Set<String> attributesToFetch, String filterOperand, 
            Map<String, Set<String>> avPairs);
    
    public Map<String, Set<String>> getGroupAttributes(String groupName, 
            String membershipIdAttributeName, Set<String> attributesToFetch);
    
}
