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
 * $Id: HotSwapProperties.java,v 1.5 2008/08/29 20:39:29 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.testng.Reporter;

/**
 * This class supports HotSwapping of properties, for 
 * Server version FAM8.0 & above.
 */
public class HotSwapProperties extends TestCommon {
    private int pollingTime;
    private int testIdx;
    private String strGblRB = "agentsGlobal";
    private String strLocRB = "HotSwapProperties";
    private String strPropRB = "AgentPropertieswithAutoGenKeys";
    private String agentId;
    private String strAgentType;    
    private String logoutURL;    
    private String strPropValueOrig;
    private String strPropName;    
    private String strPropValue;
    private String strIDType;
    private boolean executeAgainstOpenSSO;
    private boolean isHotSwap;
    private Map mapProp;
    private ResourceBundle rbg;
    private ResourceBundle rbp;
    private IDMCommon idmc;
    private SSOToken admintoken;
    private SMSCommon smsc;
    private AMIdentity amid;

    /**
     * Class constructor. Instantiates the ResourceBundles and other
     * common class objects needed by the tests.
     */
    public HotSwapProperties() 
    throws Exception {
        super("HotSwapProperties");
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        rbp = ResourceBundle.getBundle("agents" + fileseparator + strLocRB);
        executeAgainstOpenSSO = new Boolean(rbg.getString(strGblRB +
                ".executeAgainstOpenSSO")).booleanValue();
        strAgentType = rbg.getString(strGblRB + ".agentType");
        idmc = new IDMCommon();
        pollingTime = new Integer(rbg.getString(strGblRB +
                ".pollingInterval")).intValue();
        admintoken = getToken(adminUser, adminPassword, basedn);
    }
    
    /**
     * Two argument constructor for the class. Initialises the testId, agentId
     * & creates the AMIdentity object for the Agent being tested.
     */
    public HotSwapProperties(String idType, String tstIdx) 
    throws Exception {
        this();
        strIDType = idType;
        testIdx = new Integer(tstIdx).intValue();
        agentId = rbg.getString(strGblRB + ".agentId");        
        amid = idmc.getFirstAMIdentity(admintoken, agentId, 
                idmc.getIdType("agentonly"), "/");
    }

    /**
     * Checks if Hot Swapping is supported in the current configuration being 
     * tested(Hot Swapping is supported only if server is FAM8.0 or later 
     * & agent configuration is centralized).
     */
    public boolean isHotSwapSupported()
    throws Exception {
        try {
            Set set;
            Map map;
            String strIsCentralized = null;
            String strAgentRepositoryLocation = rbg.getString(strGblRB + 
                ".agentRepositoryLocation");
            isHotSwap = false;
            smsc = new SMSCommon(admintoken);
            if (!strAgentType.contains("3.0") && smsc.isAMDIT()) {
                Reporter.log("HotSwap Test Cases are skipped because" + 
                        "agent type is: " + strAgentType);
                return isHotSwap;
            }
            log(Level.FINE, "isHotSwapSupported", "strAgentType is: " + 
                strAgentType);
            amid = idmc.getFirstAMIdentity(admintoken, agentId, 
                    idmc.getIdType(strIDType), "/");
            log(Level.FINE, "isHotSwapSupported", "Agent name:" + 
                    amid.getName());
            set = amid.getAttribute(strAgentRepositoryLocation);
            Iterator itr = set.iterator();
            while ( itr.hasNext()) {
                strIsCentralized = (String)itr.next();
            }
            if (strIsCentralized == null) {
                log(Level.SEVERE, "isHotSwapSupported", "Property, " + 
                        strAgentRepositoryLocation + 
                        ",which defines if agent configuration is centralized " 
                        + "or local is NOT present.Check configuration.");
                return isHotSwap;
            } else if (strIsCentralized.equals("local")) {
                Reporter.log("HotSwap test cases are skipped because" + 
                        "agent configuration is : " + strIsCentralized);
                return isHotSwap;
            }
            log(Level.FINE, "isHotSwapSupported", "Agent configuration " + 
                    "is : " + strIsCentralized);
            isHotSwap = true;
            exiting("isHotSwapSupported");
            return isHotSwap;
        } catch (Exception e) {
            log(Level.SEVERE, "isHotSwapSupported", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Hot Swaps the property.
     */
    public void hotSwapProperty(String strPropName, String strPropValue)
    throws Exception {
        entering("hotSwapProperty", null);
        Set set;
        Set origSet;
        Map map;
        try {
            map = new HashMap();
            if (mapProp == null) {
                mapProp = new HashMap();
            }
            set = new HashSet();
            origSet = new HashSet();
            origSet = amid.getAttribute(strPropName);
            if (origSet.size() != 0) { 
                SortedSet sortset = new TreeSet();
                Iterator itr = origSet.iterator();                
                log(Level.FINE, "hotSwapProperty", "Original Set size is : " 
                        + origSet.size());
                log(Level.FINE, "hotSwapProperty","Value of set is : " + 
                        origSet.toString());                
                while (itr.hasNext()) {
                    strPropValueOrig = (String)itr.next();
                    sortset.add(strPropValueOrig);
                }
                String maxSetValue = (String)sortset.last();
                mapProp.put(strPropName,origSet);
                idmc.hotSwapProperty(amid, strPropName, strPropValue, "agents" 
                        + fileseparator + strPropRB);
            } else {
                log(Level.SEVERE, "hotSwapProperty", "Property : " + 
                        strPropName + " is NOT present. Check property name.");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "hotSwapProperty", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("hotSwapProperty");
    }
  
    /**
     * Restores the Agent Properties back to its original values.
     */
    public void restoreDefaults(int tIdx)
    throws Exception {
        entering("cleanup", null);
        log(Level.FINE, "cleanup", "Restores the Agent Properties back to " + 
                "its original values.");
        try {
            if (mapProp != null) {
                strPropName = rbp.getString(strLocRB + tIdx +
                    ".profileFetch");
                restoreDefaults(strPropName);
                strPropName = rbp.getString(strLocRB + tIdx +
                    ".sessionFetch");
                restoreDefaults(strPropName);
                strPropName = rbp.getString(strLocRB + tIdx +
                    ".responseFetch");
                restoreDefaults(strPropName);
                
                if (strAgentType.equals("3.0J2EE") ||
                        strAgentType.equals("3.0WEBLOGIC")) {
                    strPropName = rbp.getString(strLocRB + tIdx +
                        ".accessDeniedURI");
                    restoreDefaults(strPropName);
                    strPropName = rbp.getString(strLocRB + tIdx +
                        ".notenfURI");
                    restoreDefaults(strPropName);
                } else if (strAgentType.equals("3.0WEB")) {
                    strPropName = rbp.getString(strLocRB + tIdx +
                        ".accessDeniedURL");
                    restoreDefaults(strPropName);
                    strPropName = rbp.getString(strLocRB + tIdx +
                            ".notenfURL");
                    restoreDefaults(strPropName);
                }
            }
         } catch (Exception e) {
                log(Level.SEVERE, "cleanup", e.getMessage());
                e.printStackTrace();
                throw e;
            } finally {
                destroyToken(admintoken);
            }
        exiting("cleanup");
    }
    
    /**
     * Restores the Properties back to its original values.
     */
    public void restoreDefaults(String strPropName) 
            throws Exception {
        entering("cleanup", null);
        Set setLocal;
        Map map;
        log(Level.FINE, "cleanup", "Property " + strPropName +
                "being set to its original PreSwapped value");                        
        if (mapProp.containsKey(strPropName)) {
            setLocal = (Set)mapProp.get(strPropName);
            map = new HashMap();
            map.put(strPropName,setLocal);
            idmc.modifyIdentity(amid, map);
        }
    }
}
