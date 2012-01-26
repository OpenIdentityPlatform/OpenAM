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
 * $Id: IDRepoNotificationTests.java,v 1.7 2009/01/27 00:08:40 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.notification;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdEventListener;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests IDrepo create, modify & delete events notifications for
 * all the IDTypes, such as User, Group Agent, Role, Filtered Role & Realm. 
 */
public class IDRepoNotificationTests extends TestCommon implements
        IdEventListener {
    
    int listenerID;
    int eventID;
    private Map<String, String> configMap;
    String strIdType;
    SSOToken token;
    AMIdentity amid;
    AMIdentityRepository idrepo;
    String strID;
    Map attrMap;
    String attrToModify;
    String valToModify;
    boolean result = false;
    private IDMCommon idmc;

    /**
     * Creates a new instance of IDRepoNotificationTests
     */
    public IDRepoNotificationTests() {
        super("IDRepoNotificationTests");
    }
    
    /**
     * This is setup method. It registers the listener
     */
    @Parameters({"idtype"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String Idtype)
    throws Exception {
        Object[] params = {Idtype};
        entering("setup", params);
        try {
            strIdType = Idtype;
            token = getToken(adminUser, adminPassword, realm);
            idmc = new IDMCommon();
            log(Level.FINEST, "setup", "Getting AMIdentityRepository object");
            idrepo = new AMIdentityRepository(token, realm);
            Set types = idrepo.getSupportedIdTypes();
            log(Level.FINEST, "setup", "Supported IdTypes are: " + types);
            Iterator iter = types.iterator();
            //If the idtype is not supported, exit the setup. 
            //e.g. roles are not supported on AD. 
            boolean supportsIDType = false;
            while (iter.hasNext()) {
                IdType type =(IdType)iter.next();
                if (type.getName().equalsIgnoreCase(strIdType)) {
                    supportsIDType = true;
                }
            }
            assert supportsIDType;
            log(Level.FINEST, "setup", "Adding listener to IdRepo");
            listenerID = idrepo.addEventListener(this);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup();
            throw e;
        } 
    }
    
    /**
     * It tests notification generation for identity creation event
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void identityCreationTest()
    throws Exception {
        result = false;
        try {
            eventID = 0;
            Map attrs = attrMap;
            strID = "notification" + strIdType;
            Map map = new HashMap();
            Set set = new HashSet();
            if (strIdType.equalsIgnoreCase("USER")) {
                set.add(strID);
                map.put("sn", set);
                map.put("cn", set);
                map.put("userpassword", set);
                set = new HashSet();
                set.add("active");
                map.put("inetuserstatus",set);
                log(Level.FINEST, "identityCreationTest", "Create the user " +
                    "with ID " + strID);
                idmc.createIdentity(token, realm, IdType.USER, strID, map);
                log(Level.FINEST, "identityCreationTest", "Get the user " +
                    "to check successful creation ");
                amid = idmc.getFirstAMIdentity(token, strID, IdType.USER, 
                        realm);
                attrToModify = "inetuserstatus";
                valToModify = "inactive";
            } else if (strIdType.contains("AGENT")) {
                map = new HashMap();
                set = new HashSet();
                set = new HashSet();
                set.add(strID);
                map.put("userpassword", set);
                set = new HashSet();
                set.add("Active");
                map.put("sunIdentityServerDeviceStatus", set);
                set = new HashSet();
                SMSCommon smsC = new SMSCommon(token);
                if ((smsC.isAMDIT())) {
                     idmc.createIdentity(token, realm, IdType.AGENT, strID, 
                             map);
               } else {
                    log(Level.FINE, "identityCreationTest", "This is FAM " +
                            "DIT, Agents are part of SM node");
                    set.add("WebAgent");
                    map.put("AgentType", set);
                    /* In case the agent already exists, the createIdentity fails 
                     * with error message as "Generic Message", which doesnt help 
                     * in debugging. So we are performing an additional search to 
                     * log if this is the case.
                     */
                    if (setValuesHasString(idmc.searchIdentities(token,
                        strID, IdType.AGENTONLY), strID)) {
                            log(Level.SEVERE, "identityCreationTest", "Agent " +
                                    "with ID " + strID + ", already exists.");
                    }
                    idmc.createIdentity(token, realm, IdType.AGENTONLY, strID,
                            map);
                }
                log(Level.FINEST, "identityCreationTest", "Create the agent " +
                    "with ID " + strID);
                log(Level.FINEST, "identityCreationTest", "Get the agent " +
                    "to check successful creation ");
                amid = idmc.getFirstAMIdentity(token, strID, IdType.AGENTONLY, 
                        realm);
                attrToModify = "sunIdentityServerDeviceStatus";
                valToModify = "Inactive";
            } else if (strIdType.equalsIgnoreCase("ROLE")) {
                //datastore should have description attribute under role
                set.add("Role Description");
                map.put("description", set);
                log(Level.FINEST, "identityCreationTest", "Create the role " +
                        "with ID " + strID);
                idmc.createIdentity(token, realm, IdType.ROLE, strID, map);
                log(Level.FINEST, "identityCreationTest", "Get the role " +
                        "to check successful creation ");
                amid = idmc.getFirstAMIdentity(token, strID, IdType.ROLE, 
                        realm);
                attrToModify = "description";
                valToModify = "modified description";
            } else if (strIdType.equalsIgnoreCase("GROUP")) {
                //datastore should have description attribute under group
                set.add("Group Description");
                map.put("description", set);
                log(Level.FINEST, "identityCreationTest", "Create the group " +
                        "with ID " + strID);
                idmc.createIdentity(token, realm, IdType.GROUP, strID, map);
                log(Level.FINEST, "identityCreationTest", "Get the group " +
                        "to check successful creation ");
                amid = idmc.getFirstAMIdentity(token, strID, IdType.GROUP, 
                        realm);
                attrToModify = "description";
                valToModify = "Modified group description";
            } else if (strIdType.equalsIgnoreCase("FILTEREDROLE")) {
                set.add("(objectclass=person)");
                map.put("nsRoleFilter", set);
                log(Level.FINEST, "identityCreationTest", "Create the " +
                        "filtered role with ID " + strID);
                idmc.createIdentity(token, realm, IdType.FILTEREDROLE, strID,
                        map);
                log(Level.FINEST, "identityCreationTest",
                        "Get the filtered role to check successful creation ");
                amid = idmc.getFirstAMIdentity(token, strID,
                        IdType.FILTEREDROLE, realm);
                attrToModify = "nsRoleFilter";
                valToModify = "(mail=aaa@sun.com)";
            } else if (strIdType.equalsIgnoreCase("REALM")) {
                set.add("active");
                map.put("sunOrganizationStatus", set);
                amid = idmc.createIdentity(token, realm, IdType.REALM, strID,
                        new HashMap());
                amid = idmc.getFirstAMIdentity(token, strID, IdType.REALM, 
                        realm);
                attrToModify = "sunOrganizationStatus";
                valToModify = "inactive";
            } 
            assert (amid.getName().equals(strID));
            log(Level.FINEST, "identityCreationTest", "Wait for notification");
            Thread.sleep(notificationSleepTime); //Wait for notifications
        } catch (Exception e) {
            log(Level.SEVERE, "identityCreationTest", e.getMessage());
            e.printStackTrace();
            log(Level.SEVERE, "identityCreationTest", "Delete the identity");
            identityDeletionTest();
            throw e;
        } finally {
            assert (result);
        }
    }
    
    /**
     * It tests notification generation for identity modification event
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods="identityCreationTest")
    public void identityModificationTest()
    throws Exception {
        result = false;
        try {
            eventID = 1;
            log(Level.FINEST, "identityModificationTest", "Modifying " +
                    attrToModify + "attribute to value " + valToModify);
            log(Level.FINEST, "identityModificationTest", "Reading ATTRS: " +
                    amid.getAttribute(attrToModify));
            Set oriRetVals = amid.getAttribute(attrToModify);
            //Change the attribute value
            Map attrs = new HashMap();
            Set vals = new HashSet();
            vals.add(valToModify);
            attrs.put(attrToModify, vals);
            idmc.modifyIdentity(amid, attrs);
            
            log(Level.FINEST, "identityModificationTest",
                    "Wait for notification");
            Thread.sleep(notificationSleepTime); //Wait for notifications
            
            //Read the attribute value & make sure the value is updated
            log(Level.FINEST, "identityModificationTest",
                    "Reading ATTRS AGAIN: " + amid.getAttribute(attrToModify));
            Set retVals = amid.getAttribute(attrToModify);
            if ((retVals != null) && (!retVals.isEmpty())) {
                Iterator iter = retVals.iterator();
                while (iter.hasNext()) {
                    String retValue = (String)iter.next();
                    if (retValue.equalsIgnoreCase(valToModify)) {
                        log(Level.FINEST, "identityModificationTest",
                                "Attribute value returned is same. Set the" +
                                " result to true ");
                        result = true;
                    } else {
                        log(Level.FINEST, "identityModificationTest",
                                "Attribute value returned is different from " +
                                valToModify + ". Set the result to false");
                        result = false;
                    }
                }
            } else {
                 log(Level.FINEST, "identityModificationTest", "Attribute " +
                        "value returned is empty or null. Set the result to" +
                        " false");
               result = false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "identityModificationTest", e.getMessage());
            e.printStackTrace();
            log(Level.SEVERE, "identityModificationTest", "Delete the " +
                    "identity");
            identityDeletionTest();
            throw e;
        } finally {
            assert (result);
        }
    }
    
    /**
     * It tests notification generation for identity deletion event
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods="identityModificationTest")
    public void identityDeletionTest()
    throws Exception {
        result = false;
        try {
            eventID = 2;
            log(Level.FINEST, "identityDeletionTest", "Deleting the entry");
            Set idDelete = new HashSet();
            idDelete.add(amid);
            idrepo.deleteIdentities(idDelete);
            log(Level.FINEST, "identityDeletionTest", "Wait for notification");
            Thread.sleep(notificationSleepTime);
        } catch (Exception e) {
            log(Level.SEVERE, "identityDeletionTest", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            assert (result);
        }
    }
    
    /**
     * Removes the Event listener & destroys the token. 
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        try {
            idrepo.removeEventListener(listenerID);
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(token);
        }
    }
    
    /** 
     * Implementation of allIdentitiesChanged()
     */
    public void allIdentitiesChanged() {
        log(Level.FINEST, "allIdentitiesChanged", "Recvd notification: all " +
                "changed" );
    }

    /** 
     * Implementation of identityChanged(java.lang.String)
     */
    public void identityChanged(String universalId) {
        log(Level.FINEST, "identityChanged", "Recvd notification:" +
                " identityChanged:" + universalId + " eventID is " + eventID);
        if ((eventID == 0) || (eventID == 1)) {
            log(Level.FINEST, "identityChanged", "strID: " + strID );
            String uid = universalId.toUpperCase().toLowerCase();
            String struID = strID.toUpperCase().toLowerCase();
            int index = uid.indexOf(struID);
            log(Level.FINEST, "identityChanged", "index of strID " + struID + 
                    "is " + index);
            if (index != -1)
                result = true;
        }
    }
    
    /** 
     * Implementation of identityDeleted(java.lang.String)
     */
    public void identityDeleted(String universalId) {
        log(Level.FINEST, "identityDeleted", "Recvd notification:" +
                " identityDeleted:" + universalId + " eventID is " + eventID);
        if (eventID == 2) {
            log(Level.FINEST, "identityDeleted", "strID: " + strID );
            String uid = universalId.toUpperCase().toLowerCase();
            String struID = strID.toUpperCase().toLowerCase();
            int index = uid.indexOf(struID);
            log(Level.FINEST, "identityDeleted", "index of strID " + struID + 
                    "is " + index);
            if (index != -1)
                result = true;
        }
    }
    
    /** 
     *  Implementation of identityRenamed(java.lang.String)
     */
    public void identityRenamed(String universalId) {
        log(Level.FINEST, "identityRenamed", "Recvd notification:" +
                " identityRenamed:" + universalId);
    }
}
