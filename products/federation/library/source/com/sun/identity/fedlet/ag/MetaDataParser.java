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
 * 
 *
 */

package com.sun.identity.fedlet.ag;


import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SingleSignOnServiceElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;


import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *This class is used to parse the metadata from the xml files
 *
 */ 

public class MetaDataParser {

    String fedletHomeDir;
    public MetaDataParser() {
    
        fedletHomeDir = System.getProperty("com.sun.identity.fedlet.home");
        if ((fedletHomeDir == null) || (fedletHomeDir.trim().length() == 0)) {
            if (System.getProperty("user.home").equals(File.separator)) {
                fedletHomeDir = File.separator + "fedlet";
            } else {
                fedletHomeDir = System.getProperty("user.home") +
                    File.separator + "fedlet";
            }
        }
    }

    /**
 *get SP Meta Alias
 *
 */ 
    public String getSPMetaAlias()
    {
        String spMetaAlias = null;
        try {
            SAML2MetaManager manager = new SAML2MetaManager();

            List spMetaAliases = manager.getAllHostedServiceProviderMetaAliases("/");
            
            if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                // get first one
                spMetaAlias = (String) spMetaAliases.get(0);
            }

            return spMetaAlias;
        } catch (SAML2MetaException ex) {
            Logger.getLogger(MetaDataParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return spMetaAlias;
    }
    /**
 *get SP Entity ID
 *
 */  
    public String getSPEntityID()
    {
        String spEntityID = null;
        try {
            SAML2MetaManager manager = new SAML2MetaManager();

            List spEntities = 
                    manager.getAllHostedServiceProviderEntities("/");
                if ((spEntities != null) && !spEntities.isEmpty()) {
                    
                    spEntityID = (String) spEntities.get(0);
                }

            return spEntityID;
        } catch (SAML2MetaException ex) {
            Logger.getLogger(MetaDataParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return spEntityID;
    }
     /**
 *get IDP Entity ID
 *
 */ 
    public String getIDPEntityID()
    {
        String idpEntityID = null;
        try {
            SAML2MetaManager manager = new SAML2MetaManager();

            List idpEntities = 
                    manager.getAllRemoteIdentityProviderEntities("/");
                if ((idpEntities != null) && !idpEntities.isEmpty()) {
                    
                    idpEntityID = (String) idpEntities.get(0);
                }

            return idpEntityID;
        } catch (SAML2MetaException ex) {
            Logger.getLogger(MetaDataParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return idpEntityID;
    }
     /**
 *get IDP Meta Alias
 *
 */ 
    public String getIDPMetaAlias()
    {
        String idpMetaAlias = null;
        String ssoURL = getSSOUrl();
        int loc = ssoURL.indexOf("/metaAlias/");
        String idpBaseUrl = null;
        if(ssoURL != null)
        {
           idpMetaAlias = ssoURL.substring(loc + 10);        
        } 
        return idpMetaAlias;
    }
     /**
 *get IDP base URL 
 *
 */ 
    public String getIDPBaseUrl()
    {
        String ssoURL = getSSOUrl();
        int loc = ssoURL.indexOf("/metaAlias/");
        String idpBaseUrl = null;
        if(ssoURL != null)
        {
           String tmp = ssoURL.substring(0, loc);
           loc = tmp.lastIndexOf("/");
           idpBaseUrl = tmp.substring(0, loc);          
        } 
        return idpBaseUrl;
    }
  /**
 *get SSO URL
 *
 */
    private String getSSOUrl()
    {
        
        try {
            SAML2MetaManager manager = new SAML2MetaManager();

            IDPSSODescriptorElement idp = 
                        manager.getIDPSSODescriptor("/", getIDPEntityID());
                    List ssoServiceList = idp.getSingleSignOnService();
                    if ((ssoServiceList != null) 
                        && (!ssoServiceList.isEmpty())) {
                        Iterator i = ssoServiceList.iterator();
                        while (i.hasNext()) {
                            SingleSignOnServiceElement sso =
                                (SingleSignOnServiceElement) i.next();
                            if ((sso != null) && (sso.getBinding() != null)) {
                                String ssoURL = sso.getLocation();
                                int loc = ssoURL.indexOf("/metaAlias/");
                                if (loc == -1) {
                                    continue;
                                } else {
                                    return ssoURL;
                                }
                            }
                        }
                    }
            return null;
        } catch (SAML2MetaException ex) {
            Logger.getLogger(MetaDataParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public String getSPbaseUrl()
    {
    
         
        try {
            SAML2MetaManager manager = new SAML2MetaManager();
            SPSSODescriptorElement sp = manager.getSPSSODescriptor("/", getSPEntityID());
            
            
            
            List ssoServiceList = sp.getAssertionConsumerService();
            
            
            AssertionConsumerServiceElement acs = null;
            for (int i = 0; i < ssoServiceList.size(); i++) {
               acs = (AssertionConsumerServiceElement)ssoServiceList.get(i);

               return acs.getLocation();
           
            }
            
            return null;
        } catch (SAML2MetaException ex) {
            Logger.getLogger(MetaDataParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    
    }
  /**
 *get a COT 
 *
 */
    public String getCOT(){
        
        List spCOTList = null;
        
        try {

            SAML2MetaManager manager = new SAML2MetaManager();
            SPSSOConfigElement spEntityCfg = manager.getSPSSOConfig("/", getSPEntityID());
            Map spConfigAttrsMap = null;
            if (spEntityCfg != null) {
                spConfigAttrsMap = SAML2MetaUtils.getAttributes(spEntityCfg);
            }
            spCOTList = (List) spConfigAttrsMap.get(SAML2Constants.COT_LIST);

            return (String)spCOTList.get(0);
        } catch (SAML2MetaException ex) {
            Logger.getLogger(MetaDataParser.class.getName()).log(Level.SEVERE, null, ex);
        
        }
        
        return (String)spCOTList.get(0);
    }     
    /**
 *create cot file
 *
 */  
     public void createCOT(){
        try {

           

            Writer output = null;
            String text = "cot-name=" + getCOT();
            File file = new File(fedletHomeDir + File.separator + "fedlet.cot");
            output = new BufferedWriter(new FileWriter(file));
            output.write(text);
            output.write("\n");
            output.write("sun-fm-cot-status=Active");
            output.write("\n");
            output.write("sun-fm-trusted-providers=" + getIDPEntityID() + "," + getSPEntityID());
            output.close();
        } catch (IOException ex) {
            Logger.getLogger(MetaDataParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
 
     }
}



