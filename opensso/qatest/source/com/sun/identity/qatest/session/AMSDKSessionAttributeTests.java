/* The contents of this file are subject to the terms
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
 * $Id: AMSDKSessionAttributeTests.java,v 1.2 2009/01/27 00:16:37 nithyas Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.session;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests session attributes with various levels and conflict
 * resolution between global/realm/role/user level for amsdk plugin.
 */
public class AMSDKSessionAttributeTests extends TestCommon {
    
    private ResourceBundle rb;
    private AuthenticationCommon ac;
    private SSOToken admintoken;
    private SSOToken usertoken;
    private IDMCommon idmc;
    private String description;
    private String maxSessionTime;
    private String maxIdleTime;
    private List testUserList;
    private AMIdentity role1;
    private AMIdentity role2;
    private AMIdentity role3;
    private List testRoleList;
    private List roleAList;
    private List roleBList;
    private List roleCList;
    private String tempValues;
    private List resultList;
   
    public AMSDKSessionAttributeTests() 
    throws Exception {
        super("AMSDKSessionAttributeTests");
        admintoken = getToken(adminUser, adminPassword, basedn);
        idmc = new IDMCommon();
        ac = new AuthenticationCommon();
    }

    /**
     * Initialization method. This method:
     * (a) Creates a user and three test roles.
     * (b) Reads resource bundle AMSDKSessionAttributeTests.properties
     * (c) Parameter testName will be name of the test that is executed.
     * (d) Parameter inheritancelevel can be Global/Realm.
     * (e) Skips tests if AMSDK plugin is not configured.
     */
    @Parameters({"testName"})
    @BeforeClass(groups = {"amsdk","amsdk_sec"})
    public void setup(String testName)
            throws Exception {
        Object[] params = {testName};
        entering("setup", params);
        testUserList = new ArrayList<String>();
        testRoleList = new ArrayList<String>();
        roleAList = new ArrayList();
        roleBList = new ArrayList();
        roleCList = new ArrayList();
        try {
            rb = ResourceBundle.getBundle("session" + fileseparator +
                    "AMSDKSessionAttributeTests");
            description = ((String) rb.getString(testName +
                    ".description"));
            log(Level.FINEST, "setup", "TestCase Description: " + description);
            String [] rbStrings = {rb.getString(testName + 
                    ".maxsessiontime"), rb.getString(testName + ".maxidletime"),
                    rb.getString(testName + ".maxcachingtime"), 
                    rb.getString(testName + ".activeusersessions"),
                    rb.getString(testName + ".cospriority")};
            for (int i = 0; i < rbStrings.length; i++) {
                String[] s = rbStrings[i].split(";");
                roleAList.add(s[0]);
                roleBList.add(s[1]);
                roleCList.add(s[2]);
            }
            log(Level.FINEST, "setup", "RoleA Session Attributes :" + roleAList);
            log(Level.FINEST, "setup", "RoleB Session Attributes :" + roleBList);
            log(Level.FINEST, "setup", "RoleC Session Attributes :" + roleCList);
            // Check the list with high cospriority.
            tempValues = rb.getString(testName + ".cospriority");
            String [] s = tempValues.split(";");
            if (Integer.parseInt(s[0]) <= Integer.parseInt(s[1])) {
                if (Integer.parseInt(s[0]) <= Integer.parseInt(s[2])) {
                    resultList = roleAList;
                    log(Level.FINEST, "setup" ,"Result List A: " + resultList);
                } else {
                    resultList = roleCList;
                    log(Level.FINEST, "setup" ,"Result List C: " + resultList);
                }
            } else {
                if (Integer.parseInt(s[1]) <= Integer.parseInt(s[2])) {
                    resultList = roleBList;
                    log(Level.FINEST, "setup" ,"Result List B: " + resultList);
                } else {
                    resultList = roleCList;
                    log(Level.FINEST, "setup" ,"Result List C: " + resultList);
                }
            }
            // Check if AMSDK plugin is configured.
            if (!ac.getSMSCommon().isPluginConfigured(
                    SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMSDK, realm)) {
                log(Level.SEVERE, "setup", "AMSDK Plugin not configured");
                Reporter.log("Error: AMSDK Plugin not configured");
                assert false;
            }
            // Create User 
            testUserList.add("amsdktestuser");
            Map map = new HashMap();
            Set set = new HashSet();
            set.add(testUserList.get(0).toString());
            map.put("sn", set);
            map.put("cn", set);
            map.put("userpassword", set);
            set = new HashSet();
            set.add("Active");
            map.put("inetuserstatus", set);
            AMIdentity user = idmc.createIdentity(admintoken, realm,
                    IdType.USER, testUserList.get(0).toString(), map);
            log(Level.FINEST, "setup", "Created user: " + 
                    testUserList.get(0).toString());
            user.assignService(SessionConstants.SESSION_SRVC, null);
            log(Level.FINEST, "setup", "Assigned " +
                    SessionConstants.SESSION_SRVC +
                    " service to user: " + testUserList.get(0).toString());
            //Create 3 different roles            
            AMIdentityRepository idrepo =
                    new AMIdentityRepository(admintoken,"/");
            role1 = idrepo.createIdentity(IdType.ROLE, "roleA", new HashMap());
            role2 = idrepo.createIdentity(IdType.ROLE, "roleB", new HashMap());
            role3 = idrepo.createIdentity(IdType.ROLE, "roleC", new HashMap());
            testRoleList.add("roleA");
            testRoleList.add("roleB");
            testRoleList.add("roleC");
            log(Level.FINEST, "setup", "Created roles role1, role2, role3");
            idmc.addUserMember(admintoken, testUserList.get(0).toString(), 
            		testRoleList.get(0).toString(), IdType.ROLE);
            idmc.addUserMember(admintoken, testUserList.get(0).toString(),
            		testRoleList.get(1).toString(), IdType.ROLE);
            idmc.addUserMember(admintoken, testUserList.get(0).toString(),
            		testRoleList.get(2).toString(), IdType.ROLE);
            log(Level.FINEST, "setup", "Assigned roles to the user");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            cleanup();
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
   /**
    * (a) Modifies the role session service for 3 different roles with different
    *     schema attributes.
    * (b) Creates token for user with session service which has all the three
    *     different roles assigned. 
    * (c) Validates max session time, max idle time set for user with service
    *     attributes of role session service. The role that is taken affect 
    *     is the one having the highest 'cospriority' value. In case if the 
    *     cospriorities are equal the order of role is taken into account.
     */
    @Test(groups={"amsdk","amsdk_sec"})
    public void testAMSDKInhertianceRole()
    throws Exception {
        entering("testAMSDKInhertianceRole", null);
        try {
            role1.assignService("iPlanetAMSessionService", null);
            log(Level.FINEST, "testAMSDKInhertianceRole", 
                    "Reading role (service) attrs after assigning " 
                    + role1.getServiceAttributes("iPlanetAMSessionService"));
            role2.assignService("iPlanetAMSessionService", null);
            log(Level.FINEST, "testAMSDKInhertianceRole", 
                    "Reading role (service) attrs after assigning " 
                    + role2.getServiceAttributes("iPlanetAMSessionService"));
            role3.assignService("iPlanetAMSessionService", null);
            log(Level.FINEST, "testAMSDKInhertianceRole",
                    "Reading role (service) attrs after assigning " 
                    + role3.getServiceAttributes("iPlanetAMSessionService"));
            modifyRoleAssignedService(role1, roleAList);
            log(Level.FINEST, "testAMSDKInhertianceRole", "Reading service " +
                    "attrs after modifying: " 
                    + role1.getServiceAttributes("iPlanetAMSessionService"));
            modifyRoleAssignedService(role2, roleBList);
            log(Level.FINEST, "testAMSDKInhertianceRole",
                    "Reading service attrs after modifying: " +
                    role2.getServiceAttributes("iPlanetAMSessionService"));
            modifyRoleAssignedService(role3, roleCList);
            log(Level.FINEST, "testAMSDKInhertianceRole", 
                    "Reading service attrs after modifying: " +
                    role3.getServiceAttributes("iPlanetAMSessionService"));
            usertoken = getToken(testUserList.get(0).toString(), 
            		testUserList.get(0).toString(), basedn);
            maxSessionTime = 
                    Integer.toString((int) usertoken.getMaxSessionTime());
            maxIdleTime = 
                    Integer.toString((int) usertoken.getMaxIdleTime());
            if (!maxSessionTime.equals(getRoleMaxSessionTime(resultList))) {
                log(Level.SEVERE, "testAMSDKInhertianceRole", 
                        "User max session time: " + maxSessionTime + 
                        " doesn't match role max session time: " 
                        + getRoleMaxSessionTime(resultList));
                log(Level.SEVERE, "testAMSDKInhertianceRole",
                        "Max session time is not taken affect");
                assert false;
            }
            if (!maxIdleTime.equals(getRoleMaxIdleTime(resultList))) {
                log(Level.SEVERE, "testAMSDKInhertianceRole", 
                        "User max session time: " + maxIdleTime + 
                        "doesn't match role max session time: " 
                        + getRoleMaxIdleTime(resultList));
                log(Level.SEVERE, "testAMSDKInhertianceRole",
                        "Max idle time is not taken affect");
                assert false;
            }
        } catch(Exception e) {
            log(Level.SEVERE, "testAMSDKInhertianceRole", e.getMessage());
            cleanup();
            e.printStackTrace();
            throw e;
        }
        finally {
            destroyToken(usertoken);
        }
        exiting("testAMSDKInhertianceRole");
    }

    /**
     * Cleanup method. This method:
     * (a) Delete user
     * (b) Delete roles from the realm.
     */
    @AfterClass(groups={"amsdk","amsdk_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            for (int i=0; i < testUserList.size(); i++) {
                if (idmc.searchIdentities(admintoken, 
                        testUserList.get(i).toString(),
                        IdType.USER, realm).size() != 0) {
                    idmc.deleteIdentity(admintoken, realm, IdType.USER, 
                        testUserList.get(i).toString());
                    log(Level.FINEST, "cleanup", "User:" + 
                        testUserList.get(i).toString());
                    Reporter.log("cleanup user :" +
                            testUserList.get(i).toString());
                }
            }
            for (int i=0; i < testRoleList.size(); i++) {
                if (idmc.searchIdentities(admintoken, 
                        testRoleList.get(i).toString(),
                        IdType.ROLE, realm).size() != 0) {
                    idmc.deleteIdentity(admintoken, realm, IdType.ROLE, 
                        testRoleList.get(i).toString());
                    log(Level.FINEST, "cleanup", "Role:" + 
                		testRoleList.get(i).toString());
                    Reporter.log("cleanup user :" + 
                            testRoleList.get(i).toString());
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (admintoken != null) {
                destroyToken(admintoken);
            }
        }
        exiting("cleanup");
    }
    
   /**
    * Modifies the attributes of the service that is assigned to the role.
    * @param role
    * @param roleListAttr
    */ 
   private Map modifyRoleAssignedService(AMIdentity role, 
           List roleListAttr)  throws Exception {
        Map tmpMap = new HashMap();
        Set val = new HashSet();
        val.add(roleListAttr.get(0));
        tmpMap.put("iplanet-am-session-max-session-time", val);
        val = new HashSet();
        val.add(roleListAttr.get(1));
        tmpMap.put("iplanet-am-session-max-idle-time", val);
        val = new HashSet();
        val.add(roleListAttr.get(2));
        tmpMap.put("iplanet-am-session-max-caching-time", val);
        val = new HashSet();
        val.add(roleListAttr.get(3));
        tmpMap.put("iplanet-am-session-quota-limit", val);
        val = new HashSet();
        val.add(roleListAttr.get(4));
        tmpMap.put("cospriority", val);
        role.modifyService("iPlanetAMSessionService", tmpMap);        
        return tmpMap;
   }

    /**
     * Returns max session time stored in list.
     * @param attrList
     * @return
     */
    private String getRoleMaxSessionTime(List attrList) {
        String maxTime = "";
        maxTime = (String)attrList.get(0);
        return maxTime;
    }
    
    /**
     * Returns max idle time stored in list.
     * @param attrList
     * @return
     */
    private String getRoleMaxIdleTime(List attrList) {
        String maxTime = "";
        maxTime = (String)attrList.get(1);
        return maxTime;
    }    
}
