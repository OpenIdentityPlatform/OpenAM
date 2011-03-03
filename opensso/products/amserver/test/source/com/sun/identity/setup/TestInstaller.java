/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TestInstaller.java,v 1.5 2008/08/19 19:09:34 veiming Exp $
 *
 */

package com.sun.identity.setup;

import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.UI.LoginLogoutMapping;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.SMSException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.security.AccessController;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is the first class to get loaded by the Servlet container. 
 * It has helper methods to determine the status of OpenSSO
 * configuration when deployed as a single web-application. If 
 * OpenSSO server is not deployed as single web-application then the 
 * configured status returned is always true.   
 */
public class TestInstaller {
    private ServletConfig config = null;
    private static ServletContext servletCtx = null;
    private final static String AMCONFIG = "AMConfig";
    private final static String SMS_STR = "sms";
    private final static String AMCONFIG_PROPERTIES = "AMConfig.properties";
    private static SSOToken adminToken = null;
    

    void install() {
        setServiceDefaultValues();

        try {
            initializeConfigProperties();
            reInitConfigProperties();
            SSOToken adminSSOToken = getAdminSSOToken();
                
            RegisterServices regService = new RegisterServices();
            regService.registers(adminSSOToken, true);
            Map map = ServicesDefaultValues.getDefaultValues();
            String hostname = (String)map.get("SERVER_HOST");
            ConfigureData configData = new ConfigureData(
                "config/template/sms", null, hostname, adminSSOToken);
            configData.configure();
        } catch (FileNotFoundException e) {
            System.err.println("TestInstaller.processRequest: " +
                "File not found Exception occured");
            e.printStackTrace();
        } catch (SecurityException e) {
            System.err.println("TestInstaller.processRequest");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("TestInstaller.processRequest");
            e.printStackTrace();
        } catch (SMSException e) {
            System.err.println("TestInstaller.processRequest");
            e.printStackTrace();
        } catch (PolicyException e) {
            System.err.println("TestInstaller.processRequest");
            e.printStackTrace();
        } catch (SSOException e) {
            System.err.println("TestInstaller.processRequest");
            e.printStackTrace();
        }
    }
    
    private static void setServiceDefaultValues() {
        ResourceBundle res = ResourceBundle.getBundle("defaultProperties");

        String hostname = res.getString("host.name");
        int portnum = Integer.parseInt(res.getString("port"));
        String protocol = res.getString("protocol");
        String deployuri = res.getString("deploy.uri");
        String basedir = System.getProperty("java.io.tmpdir") + 
            "/openssounittest";
        String cookieDomain = res.getString("cookie.domain");
        String adminPwd = res.getString("admin.password");
        String platformLocale = res.getString("locale");
        
        String encryptAdminPwd = Crypt.encrypt(adminPwd);
        String hashAdminPwd = Hash.hash(adminPwd);
        
        Map<String, String> map = ServicesDefaultValues.getDefaultValues();
        ServicesDefaultValues.setDeployURI(deployuri, map);

        String port = Integer.toString(portnum);
        map.put("SERVER_PROTO", protocol);
        map.put("SERVER_HOST", hostname);
        map.put("SERVER_PORT", port);
        
        map.put("IS_INSTALL_VARDIR", basedir);
        map.put("BASE_DIR", basedir);
        map.put("COOKIE_DOMAIN", cookieDomain);
        map.put("SUCCESS_REDIRECT_URL", protocol + "://" + hostname + ":" + 
            port + deployuri + "/base/AMAdminFrame");

        map.put("HASHADMINPASSWD", hashAdminPwd);
        map.put("HASHLDAPUSERPASSWD", hashAdminPwd);
        map.put("ENCADMINPASSWD", encryptAdminPwd);
        map.put("OUTPUT_DIR", basedir + "/" + deployuri);
        
        if (platformLocale != null) {
            map.put("PLATFORM_LOCALE", platformLocale);
            map.put("CURRENT_PLATFORM_LOCALE", platformLocale);
            map.put("AVAILABLE_LOCALES", platformLocale);
        }
    }
    
    private static void reInitConfigProperties()
        throws FileNotFoundException, IOException {
        Map<String, String> map = ServicesDefaultValues.getDefaultValues();
        String basedir = map.get("BASE_DIR");
        reInitConfigProperties(basedir, true);
    }
                                                                                              
    /**
     * Reinitializes the system with the new properties values.
     *
     * @throws FileNotFoundException if config file is missing.
     * @throws IOException if config file cannot be read.
     */
    private static void reInitConfigProperties(
        String basedir,
        boolean initAMConfig
    ) throws FileNotFoundException, IOException
    {
        if (initAMConfig) {
            reInitAMConfigProperties(basedir);
        }
    }
    
    private static void reInitAMConfigProperties(String baseDir)
        throws FileNotFoundException, IOException
    {
        // Read config file and initialize
        String fileName = baseDir + "/" + AMCONFIG_PROPERTIES;
        try {
            FileInputStream FInpStr = new FileInputStream(fileName);
            if (FInpStr != null) {
                Properties oprops = new Properties();
                oprops.load(FInpStr);
                SystemProperties.initializeProperties(oprops);
                FInpStr.close();
            } else {
                System.err.println(
                    "TestInstaller.reInitAMConfigProperties: Unable to open: " +
                        fileName);
            }
        } catch (FileNotFoundException e) {
            System.err.println("TestInstaller.reInitAMConfigProperties: " +
                "Unable to re-initialize properties");
            throw e;
        } catch (IOException e) {
            System.err.println("TestInstaller.reInitAMConfigProperties: " +
                "Unable to load properties");
            throw e;
        }
    }

    /**
     * Helper method to return Admin token
     * @return Admin Token
     */
    private static SSOToken getAdminSSOToken() {
        if (adminToken == null) {
            adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        }
        return adminToken;
    }

    /**
     * Initialize AMConfig.propeties with host specific values
     */
    private static void initializeConfigProperties()
        throws SecurityException, IOException {
        List<String> dataFiles = getTagSwapConfigFiles();
        
        String origpath = "@BASE_DIR@";
        Map map = ServicesDefaultValues.getDefaultValues();
        String basedir = (String)map.get("BASE_DIR");
        String deployuri = (String)map.get("SERVER_URI");
        String newpath = basedir;
        
        try {
            File fhm = new File(basedir + deployuri + "/" + SMS_STR);
            fhm.mkdirs();
        } catch (SecurityException e){
            System.err.println("TestInstaller.initializeConfigProperties");
            throw e;
        }
        
        for (String file : dataFiles) {
            InputStreamReader fin = new InputStreamReader(
                new FileInputStream(file));
            
            StringBuffer sbuf = new StringBuffer();
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
            FileWriter fout = null;
            
            int idx = file.lastIndexOf("/");
            String absFile = (idx != -1) ? file.substring(idx+1) : file;
            
            try {
                fout = new FileWriter(basedir + "/" + absFile);
                String inpStr = sbuf.toString();
                fout.write(ServicesDefaultValues.tagSwap(inpStr));
            } catch (IOException e) {
                System.err.println("TestInstaller.initializeConfigProperties");
                throw e;
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
            }
        }
    }

    private static List<String> getTagSwapConfigFiles()
        throws MissingResourceException
    {
        List<String> fileNames = new ArrayList<String>();
        ResourceBundle rb = ResourceBundle.getBundle("configuratorTagSwap");
        String strFiles = rb.getString("tagswap.files");
        StringTokenizer st = new StringTokenizer(strFiles);
        while (st.hasMoreTokens()) {
            String f = st.nextToken();
            fileNames.add(f.replaceAll("WEB-INF/", "config/"));
        }
        return fileNames;
    }
    
    public static void main(String[] args) {
        TestInstaller installer = new TestInstaller();
        installer.install();
    }
}
