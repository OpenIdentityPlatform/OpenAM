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
 * $Id: 
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.wss;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.wss.provider.DiscoveryConfig;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.WSSConstants;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.testng.Reporter;


public class resetWSSSetup  extends TestCommon{


     private SSOToken admintoken;
     private SMSCommon smsc;
    Map notificationMap;
    
/**
     * Default constructor. 
     */
    public resetWSSSetup() throws Exception{
     super("ConfigUnconfig"); 
     admintoken = getToken(adminUser, adminPassword, basedn);
     smsc = new SMSCommon(admintoken);
    
    } 
     /**
     * Start the notification (jetty) server for getting notifications from the
     * server.
     */
    @BeforeSuite(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void startServer()
    throws Exception {
        entering("startServer", null);
        notificationMap = startNotificationServer();
        exiting("startServer");
    }

    /**
     * Stop the jetty server. This basically undeploys the war.
     */
    
    private void createProfile ()throws Exception { 

       try {
        //creates the WSP profile
        ProviderConfig wspPc = null ;
        wspPc = ProviderConfig.getProvider("wsp", ProviderConfig.WSP);

        System.out.println ("sec mechanism before modification" +
               wspPc.getSecurityMechanisms());

        List listSec = new ArrayList();
        listSec.add("urn:liberty:security:2005-02:null:Bearer");
        listSec.add("urn:liberty:security:2005-02:null:SAML");
        listSec.add("urn:liberty:security:2005-02:null:X509");
        listSec.add("urn:sun:wss:security:null:SAMLToken-HK"); 
        listSec.add("urn:sun:wss:security:null:SAML2Token-HK"); 
        listSec.add("urn:sun:wss:security:null:SAML2Token-SV"); 
        listSec.add("urn:sun:wss:security:null:SAMLToken-SV"); 
        listSec.add("urn:sun:wss:security:null:UserNameToken"); 
        listSec.add("urn:sun:wss:security:null:Anonymous"); 
        listSec.add("urn:sun:wss:security:null:X509Token"); 
        listSec.add("urn:sun:wss:security:null:UserNameToken-Plain"); 
        wspPc.setSecurityMechanisms(listSec); 
        wspPc.setRequestSignEnabled(true);
        ProviderConfig.saveProvider(wspPc);
        wspPc = null ;
        wspPc = ProviderConfig.getProvider("wsp", ProviderConfig.WSP);

        System.out.println ("sec mechanism after modification " +
               wspPc.getSecurityMechanisms()); 

        // check the provider is saved correctly
        if (!ProviderConfig.isProviderExists("wsp",
                        ProviderConfig.WSP)) {
        System.out.println (  "WSP provider config is not available");
        }
       
        //creates the WSC profile 
        ProviderConfig wscPc = null ; 
        wscPc = ProviderConfig.getProvider("StockService", ProviderConfig.WSC);
        listSec = new ArrayList(); 
        listSec.add("urn:sun:wss:security:null:SAML2Token-SV");
        wscPc.setSecurityMechanisms(listSec); 
        wscPc.setRequestSignEnabled(true);
        wscPc.setPreserveSecurityHeader(true);
        wscPc.setDefaultKeyStore(true);
        wscPc.setWSPEndpoint("default");
        ProviderConfig.saveProvider(wscPc);
        wscPc = null;
        wscPc = ProviderConfig.getProvider("StockService", ProviderConfig.WSC);   
        System.out.println ("sec mechanism for Stockservice are" +
               wscPc.getSecurityMechanisms());
        if (!ProviderConfig.isProviderExists("StockService",
                        ProviderConfig.WSC)) {
        System.out.println (  "StockService provider config is not available");
        }
        }catch (Exception e) {
           e.printStackTrace();
        }
    }//createProfile

    /**
    * Resets the STS service running locally in the server
    */
    @AfterSuite(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void resetSTSService()
            throws Exception {
        entering("resetSTSService", null);

        Map<String, Set> map = new HashMap<String, Set>();
        Set setSec = new HashSet();
        setSec.add("urn:sun:wss:security:null:X509Token");
        map.put("SecurityMech", setSec); 
        Set trueSet = new HashSet();
        trueSet.add ("true");
        map. put(WSSConstants.KEY_STS_IS_REQ_SIGNED, trueSet);
 
        Set falseSet= new HashSet();
        falseSet.add ("false");
        map. put(WSSConstants.KEY_STS_IS_RESP_SIGNED, falseSet); 
        map. put(WSSConstants.KEY_STS_IS_REQ_ENCRYPTED, falseSet); 
        map. put(WSSConstants.KEY_STS_IS_RESP_ENCRYPTED, falseSet); 
        
        log(Level.FINEST, "updateSTSServiceLocal", "After creating map: " + map);
        smsc.updateSvcSchemaAttribute("sunFAMSTSService", map, "Global");
        log(Level.FINEST, "updateSTSServiceLocal", "After updating service: "
                + smsc.getAttributeValueFromSchema("sunFAMSTSService",
            "SecurityMech", "Global"));
        createProfile ();
        
       //write code for checking if updation is proper
                stopNotificationServer(notificationMap);

    }


}
