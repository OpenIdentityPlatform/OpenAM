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
 * $Id: Main.java,v 1.9 2009/01/28 05:35:11 ww203982 Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.services.util.Crypt;
import com.sun.identity.common.ISResourceBundle;

/* Federation: Commented out
import com.sun.identity.federation.alliance.FSAllianceManagementException;
import com.sun.identity.liberty.ws.meta.LibertyMetaHandler;
import com.sun.identity.liberty.ws.meta.MetaException;
*/

import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSMigration70;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.setup.Bootstrap;
import com.sun.identity.tools.bundles.VersionCheck;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import com.sun.identity.shared.ldap.LDAPException;

/**
 * The <code>Main </code> class provides methods to parse the
 * commandline arguments, execute them accordingly for the declarative
 * interface/command line tool - amadmin.
 * Based on the commandline argument, this class
 * calls the DPRO SDK for user management and SMS API for 
 * service management.
 *
 */


public class Main
{
    static final String AUTH_CORE_SERVICE = "iPlanetAMAuthService";

    private static ResourceBundle bundle = null;
    private static final int INVALID=0;
    private static final int RUN_AS_DN=1;
    private static final int PASSWORD=2;
    private static final int VERBOSE=3;
    private static final int DEBUG=4;
    private static final int SCHEMA=5;
    private static final int DATA=6;
    private static final int LOCALE_NAME=7;
    private static final int HELP=8;
    private static final int DELETE=9;
    private static final int VERSION=10;
    private static final int PASSWORDFILE=11;
    private static final int SESSION=12;
    private static final int CONTINUE=13;
    private static final int IMPORT_REMOTE=14;
    private static final int IMPORT_HOSTED=15;
    private static final int ADD_ATTRIBUTES=16;
    private static final int NOLOG= 17; 
    private static final int LIBERTY_DATA= 18;         
    private static final int ENTITY_NAME=19;
    private static final int OUTPUT=20; 
    private static final int XML_SIG=21; 
    private static final int VERIFY_SIG=22; 
    private static final int DEFAULT_URL_PREFIX=23;
    private static final int META_ALIAS=24;
    private static final int ADD_RESOURCE_BUNDLE = 25;
    private static final int RESOURCE_BUNDLE_FILE = 26;
    private static final int GET_RESOURCE_STRING = 27;
    private static final int DELETE_RESOURCE_BUNDLE = 28;
    private static final int RESOURCE_LOCALE = 29;
    private static final int DELETE_POLICY_RULE = 30;
    private static final int MIGRATE70TOREALMS = 31;
    private static final int OUTPUTFILENAME = 32;
    
    private static Map arguments = new HashMap();
    private List infileNames = Collections.synchronizedList(new ArrayList());
    private String outfileName;
    private static String bindDN = null;
    private static String bindPW = null;
    private String passwordfile = null;
    private String localeName = null;
    private static String inUserId = null;
    private String smUserId = null;
    private int operation = 0;
    private int comptype;
    private int debugFlg = 0;
    private int verboseFlg = 0;
    private boolean continueFlag = false;
    private AMStoreConnection connec = null;
    private SSOToken ssot;
    private String entityName = null; 
    private String serverName = null;
    private String defaultUrlPrefix;
    private String addServiceName = null;
    private String addSchemaType = null;
    private boolean xmlSig = false; 
    private boolean verifySig = false; 
    private String metaPrefix = null; 
    private List metaAlias = Collections.synchronizedList(new ArrayList());
    String sprotocol;
    String _sserver;
    String sserver;
    String sport;
    private String resourceBundleName;
    private String resourceFileName;
    private String resourceLocale;
    private int deletePolicyRuleFlg = 0;
    private Map sessionStoreInfo = null;
    private String entryDN;
    private String outputFileName = null;
    private Authenticator auth = null;
    private static String libertyDN;
    private static final String DEBUGDIR =
        "com.iplanet.services.debug.directory";
    private static String debugDir;
    private static String admDbugDir;

    static {
        try {
            Bootstrap.load();

            if (VersionCheck.isValid() == 1) {
                System.exit(1);
            }

            debugDir = SystemProperties.get(DEBUGDIR);
            admDbugDir = debugDir + Constants.FILE_SEPARATOR + "amadmincli";
            libertyDN = SystemProperties.get("com.iplanet.am.defaultOrg");
            
            arguments.put("--debug", new Integer(DEBUG));
            arguments.put("-d", new Integer(DEBUG));
            arguments.put("--verbose", new Integer(VERBOSE));
            arguments.put("-v", new Integer(VERBOSE));
            arguments.put("--nolog", new Integer(NOLOG));
            arguments.put("-O", new Integer(NOLOG));
            arguments.put("--schema", new Integer(SCHEMA));
            arguments.put("-s", new Integer(SCHEMA));
            arguments.put("--data", new Integer(DATA));
            arguments.put("-t", new Integer(DATA));
            arguments.put("--runasdn", new Integer(RUN_AS_DN));
            arguments.put("-u", new Integer(RUN_AS_DN));
            arguments.put("--password", new Integer(PASSWORD));
            arguments.put("-w", new Integer(PASSWORD));
            arguments.put("--passwordfile", new Integer(PASSWORDFILE));
            arguments.put("-f", new Integer(PASSWORDFILE));
            arguments.put("--locale", new Integer(LOCALE_NAME));
            arguments.put("-l", new Integer(LOCALE_NAME));
            arguments.put("--help", new Integer(HELP));
            arguments.put("-h", new Integer(HELP));
            arguments.put("--deleteservice", new Integer(DELETE));
            arguments.put("-r", new Integer(DELETE));
            arguments.put("--version", new Integer(VERSION));
            arguments.put("-n", new Integer(VERSION));
            arguments.put("--session", new Integer(SESSION));
            arguments.put("-m", new Integer(SESSION));
            arguments.put("--continue", new Integer(CONTINUE));
            arguments.put("-c", new Integer(CONTINUE));
            arguments.put("--importRemote", new Integer(IMPORT_REMOTE));
            arguments.put("-I", new Integer(IMPORT_REMOTE));
            arguments.put("--importHosted", new Integer(IMPORT_HOSTED));
            arguments.put("-p", new Integer(IMPORT_HOSTED));
            arguments.put("--addAttribute", new Integer(ADD_ATTRIBUTES));
            arguments.put("-a", new Integer(ADD_ATTRIBUTES));
            arguments.put("--import", new Integer(LIBERTY_DATA));
            arguments.put("-g", new Integer(LIBERTY_DATA));
            arguments.put("--entityname", new Integer(ENTITY_NAME));
            arguments.put("-e", new Integer(ENTITY_NAME));
            arguments.put("--verifysig", new Integer(VERIFY_SIG));
            arguments.put("-y", new Integer(VERIFY_SIG)); 
            arguments.put("--export", new Integer(OUTPUT));
            arguments.put("-o", new Integer(OUTPUT));
            arguments.put("--xmlsig", new Integer(XML_SIG));
            arguments.put("-x", new Integer(XML_SIG));
            arguments.put("--defaulturlprefix", new Integer(DEFAULT_URL_PREFIX));
            arguments.put("-k", new Integer(DEFAULT_URL_PREFIX)); 
            arguments.put("--metaalias", new Integer(META_ALIAS));
            arguments.put("-q", new Integer(META_ALIAS));
            arguments.put("--addresourcebundle", new Integer(ADD_RESOURCE_BUNDLE));
            arguments.put("-b", new Integer(ADD_RESOURCE_BUNDLE));
            arguments.put("--resourcebundlefilename",
                    new Integer(RESOURCE_BUNDLE_FILE));
            arguments.put("-i", new Integer(RESOURCE_BUNDLE_FILE));
            arguments.put("--resourcelocale", new Integer(RESOURCE_LOCALE));
            arguments.put("-R", new Integer(RESOURCE_LOCALE));
            arguments.put("--getresourcestrings", new Integer(GET_RESOURCE_STRING));
            arguments.put("-z", new Integer(GET_RESOURCE_STRING));
            arguments.put("--deleteresourcebundle",
                    new Integer(DELETE_RESOURCE_BUNDLE));
            arguments.put("-j", new Integer(DELETE_RESOURCE_BUNDLE));
            arguments.put("-C",new Integer(DELETE_POLICY_RULE));
            arguments.put("--cleanpolicyrules",new Integer(DELETE_POLICY_RULE));
            arguments.put("-M", new Integer(MIGRATE70TOREALMS));
            arguments.put("--migrate70torealms", new Integer(MIGRATE70TOREALMS));
            arguments.put("-ofilename", new Integer(OUTPUTFILENAME));
            arguments.put("--ofilename", new Integer(OUTPUTFILENAME));
            arguments.put("-F", new Integer(OUTPUTFILENAME));
            SystemProperties.initializeProperties (DEBUGDIR, admDbugDir);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Main() {
    }

/**
  Command line is parsed and appropriate values are stored
  **/
    private String readFromPassword(String passwordfile) throws AdminException {

        String line =null;
        BufferedReader in =null;
        try{
            in = new BufferedReader(new FileReader(passwordfile));
            if(in.ready())  {
                line = in.readLine();
            } 
            return line;
        } catch(IOException e){
            if(AdminUtils.logEnabled()) {
                AdminUtils.log("Could not open file " + e.getMessage() );
            }
        } finally {
            if (in !=null ) {
                try {
                    in.close();
                }
                catch (Exception e) {
                    if(AdminUtils.logEnabled()) {
                        AdminUtils.log(
                            "Unable to close the file: " + e.getMessage());
                    }
                }
            }
        }
        return null;
    }

    public void parseCommandLine(String[] argv) throws AdminException {
	if (bundle == null) {
	    AdminResourceBundle.setLocale(null);
	    bundle = AdminResourceBundle.getResources();
	}

        if (!ArgumentValidator.validateArguments(argv, bundle)) {
            System.err.println(bundle.getString("usage"));
            System.exit(1);
        }

        for (int i = 0; i < argv.length; i++) {
            int opt = getToken(argv[i]);
            switch(opt) {
              case IMPORT_REMOTE :            
                  operation = opt;
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }     
                  //Populate the list of files to be loaded.
                  for (int j=i;j<argv.length;j++,i++) {            
                      infileNames.add(argv[j]);
                  }                  
                  break;
                   
              case IMPORT_HOSTED :            
                  operation = opt;
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }              
                  //The first parameter that corresponds to the 
                  //value to be used for autopopulation.                  
                  defaultUrlPrefix = argv[i]; i++;
                   
                  //Capture the list of files to be imported
                  for (int j=i;j<argv.length;j++,i++) {                         
                      infileNames.add(argv[j]);
                  }                  
                  break;                                         
                                
              case SESSION :
                operation = opt;
                i++;

                if ((i >= argv.length) ||
                    (arguments.containsKey(argv[i].toString()))
                ) {
                    System.err.println(bundle.getString("usage"));
                    System.exit(1);
                }

                serverName = (argv[i] == null) ? "*" : argv[i];
                i++;
                smUserId = (i >= argv.length) ? "*" : argv[i];
                break;

              case RUN_AS_DN :
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                  bindDN = argv[i];
                  if ((comptype = getToken(bindDN.toLowerCase())) != INVALID) {
                      throw new AdminException(
                        bundle.getString("nodnforadmin"));
                  } else {
                      StringTokenizer DNToken = new StringTokenizer(bindDN,",");

                      if (DNToken.hasMoreTokens()) {
                          String uidString = DNToken.nextToken();
                          StringTokenizer uidToken = new StringTokenizer(
                            uidString, "=");

                          if (uidToken.hasMoreTokens()) {
                              String s1 = uidToken.nextToken();

                              if ( s1.equals("uid")) {
                                  if (uidToken.hasMoreTokens()) {
                                      inUserId = uidToken.nextToken();
                                  } 
                              }
                          }
                      }
                  }
                  break;
              case PASSWORD :
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                  bindPW = argv[i];
                  if ((comptype = getToken(bindPW.toLowerCase())) != INVALID)
                      throw new AdminException(
                        bundle.getString("nopwdforadmin"));
                  break;
              case PASSWORDFILE :
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                  passwordfile = argv[i];
                  bindPW = readFromPassword(passwordfile);

                  if ((bindPW == null) ||
                    ((comptype = getToken(bindPW.toLowerCase())) != INVALID)
                  ) {
                      throw new AdminException(
                        bundle.getString("nopwdforadmin"));
                  }

                  bindPW = bindPW.trim();
                  break;
              case ENTITY_NAME: 
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                  entityName = argv[i];
                  if (((comptype = getToken(entityName.toLowerCase()))
                    != INVALID)
                  ) {
                      throw new AdminException(bundle.getString(
                        "noentityname"));
                  }
                  break;
              case DATA :
              case SCHEMA :
              case LIBERTY_DATA :
                  operation = opt;
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                  String tmp = argv[i].toLowerCase();
                  if (!tmp.endsWith(".xml"))
                      throw new AdminException(bundle.getString("nofile"));
                  for (int j=i;j<argv.length;j++) {
                      tmp = argv[j].toLowerCase();
                      if (tmp.endsWith(".xml")) {
                          infileNames.add(argv[j]);
                          i = i+j;
                      } else {
                          i = j-1;
                          break;
                      }
                  }
                  break;
              case VERIFY_SIG:
                  verifySig = true; 
                  break;   
              case XML_SIG: 
                  xmlSig = true; 
                  break;
              case DEFAULT_URL_PREFIX:
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                  metaPrefix = argv[i];
                  if ((comptype = getToken(metaPrefix.toLowerCase()))
                    != INVALID
                  ) {
                      throw new AdminException(bundle.getString(
                        "nodefaulturlprefix"));
                  }
                  break;
              case META_ALIAS:
                  int startIndx = i; 
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                
                  //Populate the list of meta alias.
                  for (int j=i;j<argv.length;j++,i++) {      
                      if ((comptype = getToken(argv[j].toLowerCase())) != INVALID) {
                          i--; 
                          break;
                      }     
                      metaAlias.add(argv[j]);
                  }    
                 
                  if (startIndx == i) { 
                      throw new AdminException(bundle.getString("nometaalias"));        
                  }
                  break;  
              case OUTPUT:          
                  operation = opt;
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                  outfileName = argv[i];
                  if ((comptype=getToken(outfileName.toLowerCase())) != INVALID)
                      throw new AdminException(bundle.getString(
                                                            "nooutfilename"));
                  break;   
              case LOCALE_NAME :
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }
                  localeName = argv[i];
                  if ((localeName == null) ||
                    ((comptype = getToken(localeName.toLowerCase())) != INVALID)
                  ) {
                      throw new AdminException(
                        bundle.getString("nolocalename"));
                  }

                  // Set the locale to English eventhough localeName is given,
                  // to write all messages in the debug file.

                  if (debugFlg == 1 ) {
                      AdminResourceBundle.setLocale("en_US");
                      bundle = AdminResourceBundle.getResources();
                  } else {
                      AdminResourceBundle.setLocale(localeName);
                      bundle = AdminResourceBundle.getResources();
                  }
                  break;
              case DEBUG :
                  debugFlg = 1;
                  if (verboseFlg == 1 ) {
                      System.out.println(bundle.getString("dbgerror") +
                                         " --verbose|--debug");
                      System.exit(1);
                  } else {
                      AdminUtils.setDebug(AdminReq.debug);
                      AdminUtils.setDebugStatus(Debug.ON);
                      AdminUtils.enableDebug(true);
                      AdminResourceBundle.setLocale("en_US");
                      bundle = AdminResourceBundle.getResources();
                  }
                  break;
              case NOLOG:
                  AdminUtils.setLog(false);
                  break;
              case VERBOSE :
                  verboseFlg = 1;
                  if (debugFlg == 1 ) {
                      System.out.println(bundle.getString("dbgerror") +
                                         " --verbose|--debug");
                      System.exit(1);
                  } else {
                      AdminUtils.setDebug(AdminReq.debug);
                      AdminUtils.enableVerbose(true);
                  }
                  break;
              case HELP :
                  printHelp();
                  System.exit(0);
                  break;
              case VERSION :
                  printVersion();
                  System.exit(0);
                  break;
              case CONTINUE :
                  continueFlag=true;
                  break;
              case DELETE :
                  operation = opt;
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }

                  if ((argv[i] == null) ||
                    (arguments.containsKey(argv[i].toString()))
                  ) {
                      throw new AdminException(
                        bundle.getString("noservicename"));
                  }

                  for (int j=i;j<argv.length;j++) {
                      infileNames.add(argv[j]);
                      i++;
                      if (arguments.containsKey(argv[j].toString())) {
                          i = j-1;
                          infileNames.remove(argv[j]);
                          break;
                      }
                  }
                  break;

              case ADD_ATTRIBUTES :            
                  operation = opt;
                  i++;
                  if (i >= argv.length) {
                      System.err.println(bundle.getString("usage"));
                      System.exit(1);
                  }     
                  addServiceName = argv[i];
                  i++;
                  addSchemaType = argv[i];
                  i++;
                  //Populate the list of files to be loaded.
                  for (int j=i;j<argv.length;j++,i++) {            
                      infileNames.add(argv[j]);
                  }                  
                  break;
            case GET_RESOURCE_STRING:
            case ADD_RESOURCE_BUNDLE:
            case DELETE_RESOURCE_BUNDLE:
                if ((++i >= argv.length) || (operation != 0)) {
                    System.err.println(bundle.getString("usage"));
                    System.exit(1);
                }

                operation = opt;
                resourceBundleName = argv[i];
                break;
            case RESOURCE_BUNDLE_FILE:
                if (++i >= argv.length) {
                    System.err.println(bundle.getString("usage"));
                    System.exit(1);
                }

                resourceFileName = argv[i];
                break;
            case RESOURCE_LOCALE:
                if (++i >= argv.length) {
                    System.err.println(bundle.getString("usage"));
                    System.exit(1);
                }

                resourceLocale = argv[i];
                break;
            case DELETE_POLICY_RULE:
                deletePolicyRuleFlg = 1;
                break;
            case MIGRATE70TOREALMS:
                if (++i >= argv.length) {
                    System.err.println(bundle.getString("usage"));
                    System.exit(1);
                }
                operation = opt;
                entryDN = argv[i];
                break;
            case OUTPUTFILENAME:
                i++;
                if (i >= argv.length) {
                    System.err.println(bundle.getString("usage"));
                    System.exit(1);
                }
                outputFileName = argv[i];
                break;
            default :
                AdminUtils.setDebug(AdminReq.debug);
                AdminUtils.setDebugStatus(Debug.OFF);
                System.err.println(bundle.getString("usage"));
                System.err.println(bundle.getString("invopt")+argv[i]);
                System.exit(1);
            }
        }
        if ((bindDN == null) || (bindPW == null)) {
            AdminUtils.setDebug(AdminReq.debug);
            AdminUtils.setDebugStatus(Debug.OFF);
            System.err.println(bundle.getString("usage"));
            System.exit(1);
        }

    }

    /**
     * Actual execution of the operations are performed here
     */
    public void runCommand()
        throws AdminException, LDAPException
    {
        boolean bError = false;
        if (operation != SESSION) {
            auth = new Authenticator(bundle);
            auth.ldapLogin(bindDN, bindPW);
            ssot = auth.getSSOToken();
        }

        switch (operation) {
/* Federation: Commented out
        case IMPORT_REMOTE:
            processImportRemoteRequests();
            break;

        case IMPORT_HOSTED:
            processImportHostedRequests();
            break;
*/
        case DATA:
            processDataRequests();
            break;

        case SCHEMA:
            processSchemaRequests();
            break;

        case DELETE:
            processDeleteRequests();
            break;

        case SESSION:
            processSessionRequest();
            break;

        case ADD_ATTRIBUTES:
            processAddAttributesRequests();
            break;

/* Federation: Commented out
        case LIBERTY_DATA:
            processLibertyDataRequests();
            break;
        case OUTPUT: 
            outputLibertyData(); 
            break;    
*/
        case ADD_RESOURCE_BUNDLE:
            addResourceBundle();
            break;
        case GET_RESOURCE_STRING:
            getResourceStrings();
            break;
        case DELETE_RESOURCE_BUNDLE:
            deleteResourceBundle();
            break;
        case MIGRATE70TOREALMS:
            migrate70ToRealms();
            break;
        default:
            bError = true;
        }

        if (auth != null) {
            auth.destroySSOToken();
        }

        if (bError) {
            AdminUtils.setDebug(AdminReq.debug);
            AdminUtils.setDebugStatus(Debug.OFF);
            System.err.println(bundle.getString("nodataschemawarning"));
            System.err.println(bundle.getString("usage"));
            System.exit(1);
        }
    }

    private void processSessionRequest()
        throws AdminException
    {
        Authenticator auth = new Authenticator(bundle);
        String bindUser = (inUserId != null) ? inUserId : bindDN;
        AuthContext lc = auth.sessionBasedLogin(bindUser, bindPW);
        SSOToken ssot = auth.getSSOToken();
        AdminUtils.setSSOToken(ssot);
        SessionRequest req = new SessionRequest(ssot, serverName, bundle);
        req.displaySessions(smUserId);
        try {
            lc.logout();
        } catch (AuthLoginException e) {
                   handleRunCommandException(e);
        }

    }
    /* Federation: Commented out
    private void processImportRemoteRequests() {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg1"));
        }

        for (Iterator iter = infileNames.iterator(); iter.hasNext(); ) {
            String infile = (String)iter.next();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg2") + infile);
            }

            try {
                convertImportMetaData(infile, ssot, "remote", null); 
            } catch (AdminException e) {
                handleRunCommandException(e);
            }
        }
    }

    private void processImportHostedRequests() {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg1"));
        }

        for (Iterator iter = infileNames.iterator(); iter.hasNext(); ) {
            String infile = (String)iter.next();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg2") + infile);
            }

            try {
                convertImportMetaData(infile, ssot, "hosted", defaultUrlPrefix);
            } catch (AdminException e) {
                handleRunCommandException(e);
            }
        }
    } */

    private void processDataRequests()
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg1"));
        }

        try {
            connec = new AMStoreConnection(ssot);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        for (Iterator iter = infileNames.iterator(); iter.hasNext(); ) {
            String infile = (String)iter.next();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg2") + infile);
            }

            try {
                processDataRequests(infile, connec, ssot, continueFlag);
            } catch (AdminException e) {
                handleRunCommandException(e);
            }
        }
    }

/* Federation: Commented out
    private void processLibertyDataRequests()
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg1"));
        }

        if ((infileNames == null) || infileNames.isEmpty()) {
            throw new AdminException(
                bundle.getString("missingLibertyMetaInputFile"));
        }

        LibertyMetaHandler metaHandler = getLibertyMetaHandler();

        for (Iterator iter = infileNames.iterator(); iter.hasNext(); ) {
            String infile = (String)iter.next();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg2") + infile);
            }

            try { 
                metaHandler.metaToSM(infile, verifySig, metaPrefix,
                                     metaAlias); 
            } catch (MetaException me) { 
                throw new AdminException(
                    bundle.getString("failLoadLibertyMeta") + 
                    me.getMessage()); 
            }                             
        }
    }
    
    private void outputLibertyData()       
         throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg40") + outfileName);
        }

        if (entityName == null) {
            throw new AdminException(bundle.getString("missingEntityName"));
        }

        if (outfileName == null) {
            throw new AdminException(
                bundle.getString("missingLibertyMetaOutputFile")); 
        }

        LibertyMetaHandler metaHandler = getLibertyMetaHandler(); 
        try { 
            metaHandler.SMToMeta(entityName, xmlSig, outfileName);
        } catch (MetaException me) { 
            throw new AdminException(me); 
        }     
    }

    private LibertyMetaHandler getLibertyMetaHandler()
        throws AdminException
    {
        LibertyMetaHandler metaHandler = null;

        try {
            metaHandler = new LibertyMetaHandler(ssot, libertyDN);

            if (metaHandler == null) {
                throw new AdminException(
                    bundle.getString("cannotObtainMetaHandler"));
            }
        } catch (FSAllianceManagementException fe) {
            throw new AdminException(fe);
        }

        return metaHandler;
    }
*/
    
    private void processSchemaRequests() {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg3"));
        }

        for (Iterator iter = infileNames.iterator(); iter.hasNext(); ) {
            String infile = (String)iter.next();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg4") + infile);
            }

            try {
                Set names = registerServiceSchema(infile);

                if ((names != null) && !names.isEmpty()) {
                    String[] params = {(String)names.iterator().next()};
                    AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                        Level.INFO, AdminUtils.LOAD_SERVICE, params);
                }
            } catch (AdminException e) {
                handleRunCommandException(e);
            }
        }
    }

    private void processDeleteRequests() {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg28"));
        }

        for (Iterator iter = infileNames.iterator(); iter.hasNext(); ) {
            String infile = (String)iter.next();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg29") + infile);
            }

            try {
                processDeleteService(infile);
                String[] params = {infile};
//                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                    MessageFormat.format(bundle.getString("service-deleted"),
//                    params));
                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                   Level.INFO, AdminUtils.DELETE_SERVICE, params);
            } catch (AdminException e) {
                handleRunCommandException(e);
            }
        }
    }

    private void processAddAttributesRequests() {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg34"));
        }

        for (Iterator iter = infileNames.iterator(); iter.hasNext(); ) {
            String infile = (String)iter.next();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg35") + infile);
            }

            try {
                processAddAttributes(addServiceName, addSchemaType, infile);
                String[] params = {infile};
//                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                    MessageFormat.format(bundle.getString("addAttributes"),
//                    params));
                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                    Level.INFO, AdminUtils.ADD_ATTRIBUTES, params);
            } catch (AdminException e) {
                handleRunCommandException(e);
            }
        }
    }

    private void handleRunCommandException(Exception e) {
        if (debugFlg != -1) {
            System.err.println(bundle.getString("execfailed") + "\n" +
                e.getLocalizedMessage());
        }

        //
        //  leave this one (for now, anyway), as it has
        //  the exception stacktrace in it
        //
        AdminUtils.logOperation(AdminUtils.LOG_ERROR,
            bundle.getString("execfailed") + " " + e);

        if (!continueFlag) {
            if (auth != null) {
                auth.destroySSOToken();
            }
            System.exit(1);
        }
    }

/**
  Method to print the descriptive information about the utility, such as its
  version, legal notices, license keys, and other similar information, 
  if 'amadmin --version' is executed.
  **/

    void printVersion() {
        System.out.println();
        ResourceBundle rb = ResourceBundle.getBundle("AMConfig");
        String[] arg = {rb.getString("com.iplanet.am.version")};
        System.out.println(MessageFormat.format(
            bundle.getString("version"), arg));
        System.out.println();
    }


/**
  Method to print the definitions of the mandatory and optional arguments
  and their values, if 'amadmin --help' is executed.
  **/

    void printHelp() {
        System.out.println(bundle.getString("usage"));
    }

/**
  Method to process Data Requests from the commandline.
  **/


    void processDataRequests(String dataXML, AMStoreConnection connection,
        SSOToken ssotkn, boolean continueFlag) 
        throws AdminException 
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("processingDataXML") +
                " " + dataXML);
            AdminUtils.log(bundle.getString("statusmsg5"));
        }

        AdminXMLParser dpxp = new AdminXMLParser();
        dpxp.processAdminReqs(dataXML, connection, ssotkn, continueFlag,
            outputFileName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("doneProcessingXML") +
                " " + dataXML);
        }
    }

    /* Federation: Commented Out
     * Method to process the metadata pertaining to the Liberty Spec.
     * The input file would have xml that would stick to the liberty.xsd
     * This needs to be transformed into xml that sticks to amAdmin.dtd.
     * This xml can then be loaded/imported like any other XML file that
     * sticks to amAdmin.dtd.
     *
    void convertImportMetaData(
        String dataXML,
        SSOToken ssotkn, 
        String provType,
        String defaultUrlPrefix
    ) throws AdminException {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("processingDataXML") +
                " " + dataXML);
            AdminUtils.log(bundle.getString("statusmsg5"));
        }

        AdminXMLParser dpxp = new AdminXMLParser();
        dpxp.processLibertyMetaData(
            dataXML, ssotkn, provType, defaultUrlPrefix );

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("doneProcessingXML") + " " +
                dataXML);
        }
    } */

    public static boolean isInstallTime() {
        String s = System.getProperty("installTime");
        return (s != null) && s.toLowerCase().startsWith("true");
    }

    static public void main(String[] args) throws AdminException {
        /*
         * Set the property to inform AdminTokenAction that
         * "amadmin" CLI is executing the program
         */
        if (isInstallTime()) {
            SystemProperties.initializeProperties(
                AdminTokenAction.AMADMIN_MODE, "true");
        }

        /*if the locale is set to null it will 
         *take the OS default locale. 
         */
        AdminResourceBundle.setLocale(null);
        bundle = AdminResourceBundle.getResources();

        // Initialize Crypt class
        Crypt.checkCaller();

        Main dpa = new Main();
        try {
            dpa.parseCommandLine(args);
            dpa.runCommand();
            System.out.println(bundle.getString("successful"));
            System.exit(0);
        } catch (Exception eex) {
            System.err.println(bundle.getString("oprfailed") + " " +
                eex.getLocalizedMessage());
            System.exit(1);
        } finally {
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    shutdownMan.shutdown();
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
        }

    }
        
    int getToken(String arg) {
        try {
            return(((Integer)arguments.get(arg)).intValue());
        } catch(Exception e) {
            return 0;
        }
    }


/**
 * Supports LDAP Authentication in amadmin.
 * Calls AuthContext class to get LDAP authenticated by passing 
 * principal,password
 * Also gets the SSO token from the AuthContext class to have 
 * AMStoreConnection. 
 */



    /**
     * Registers a service schema.
     *
     * @param schemaFile name of XML file that contains service schema
     *        information.
     * @return names of registered services.
     * @throws AdminException if service schema cannot registered.
     */
    Set registerServiceSchema(String schemaFile)
        throws AdminException
    {
        Set serviceNames = Collections.EMPTY_SET;

        if (AdminUtils.logEnabled()) {
            AdminUtils.log("\n" + bundle.getString("loadingServiceSchema") +
                " " + schemaFile);
        }

        System.out.println(bundle.getString("loadServiceSchema") + " " +
            schemaFile);

        FileInputStream fis = null;

        try {
            ServiceManager ssm = new ServiceManager(ssot);
            fis = new FileInputStream(schemaFile);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg12") + schemaFile);
            }

            serviceNames = ssm.registerServices(fis);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("doneLoadingServiceSchema") +
                    " " + serviceNames.toString());
            }
        } catch (IOException ioe) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("file"), ioe);
            }
            throw new AdminException(bundle.getString("file"));
        } catch (SSOException sse) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg13"), sse);
            }
            throw new AdminException(sse);
        } catch (SMSException sme) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg14"), sme);
            }
            throw new AdminException(sme);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ie) {
                    if (AdminUtils.logEnabled()) {
                        AdminUtils.log(bundle.getString("statusmsg15"),ie);
                    }
                }
            }
        }

        return serviceNames;
    }

    /**
     * Adds  the attributes to the XML with the default value
     *
     **/


    void processAddAttributes(String serviceName, String schemaType, 
                             String inputFile) throws AdminException
    {
        if (AdminUtils.logEnabled())
            AdminUtils.log("\n"+bundle.getString("addAttributes") + 
                           " " +serviceName + " " + schemaType );

        try {
            ServiceSchemaManager ssm =  new ServiceSchemaManager(
                serviceName, ssot);
            ServiceSchema ss = null;
            FileInputStream fis = null;


            if (schemaType.equalsIgnoreCase("global")) {
                ss = ssm.getGlobalSchema();
            } else if (schemaType.equalsIgnoreCase("organization")) {
                ss = ssm.getOrganizationSchema();
            } else if (schemaType.equalsIgnoreCase("dynamic")) {
                ss = ssm.getDynamicSchema();
            } else if (schemaType.equalsIgnoreCase("user")) {
                ss = ssm.getUserSchema();
            } else if (schemaType.equalsIgnoreCase("policy")) {
                ss = ssm.getPolicySchema();
            } 
            fis = new FileInputStream(inputFile);
            ss.addAttributeSchema(fis);

            if (AdminUtils.logEnabled())
                AdminUtils.log(bundle.getString("doneAddingAttributes")+ " " + 
                               serviceName);
        } catch (IOException ioe) {
            if (AdminUtils.logEnabled() && (debugFlg == 1))
                AdminUtils.log(bundle.getString("file"),ioe);
            throw new AdminException(bundle.getString("file"));
        } catch (SSOException sse) {
            if (AdminUtils.logEnabled() && (debugFlg == 1))
                AdminUtils.log(bundle.getString("statusmsg13"),sse);
            throw new AdminException("\n"+bundle.getString("smsdelexception") 
                                     +"\n\n"+sse.getLocalizedMessage()+"\n");
        } catch (SMSException sme) {
            if (AdminUtils.logEnabled())
                AdminUtils.log(bundle.getString("statusmsg14"),sme);
            throw new AdminException("\n"+bundle.getString("smsdelexception") 
                                     +"\n\n"+sme.getLocalizedMessage()+"\n");
        }
    }


    /**
     * Deletes the service configuration, and all the sub-configuration,
     * including all the organization-based configurations
     *
     * @param serviceName to be deleted
     * @throws AdminException if service schema cannot be deleted.
     **/
    void processDeleteService(String serviceName)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log("\n" + bundle.getString("deletingService") + " " +
                serviceName);
        }


        System.out.println(bundle.getString("deleteServiceSchema") + " " +
            serviceName);

        try {
            ServiceManager ssm = new ServiceManager(ssot);
            ServiceConfigManager scm = new ServiceConfigManager(
                    serviceName, ssot);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg12") + serviceName);
            }

            if(deletePolicyRuleFlg == 1){
               ServiceSchemaManager ssmgr = new ServiceSchemaManager(
                       serviceName, ssot);
   
               if (ssmgr == null) {
                   if (AdminUtils.logEnabled() && (debugFlg == 1)) {
                       String[] params = { serviceName };
//                     AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                     MessageFormat.format(
//                             bundle.getString("noPolicyPriviliges"), params));
                       AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                           Level.INFO, AdminUtils.NO_POLICY_PRIVILEGES, params);
                    }
               }else {
                  if (ssmgr.getPolicySchema() == null) {
                      if (AdminUtils.logEnabled() && (debugFlg == 1)) {
                          String[] params = { serviceName };
//                        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                        MessageFormat.format(
//                                bundle.getString("serviceNameNotFound"), 
//                                params));
                          AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                              Level.INFO, AdminUtils.SERVICE_NOT_FOUND,
                              params);
                       }
                  } else {
                      processCleanPolicies(serviceName) ;
                  }
               }
            }

            if (scm.getGlobalConfig(null) != null) {
                scm.removeGlobalConfiguration(null);
            }

            if (serviceName.equalsIgnoreCase(AUTH_CORE_SERVICE)) {
                ssm.deleteService(serviceName);
            } else {
                Set versions = ssm.getServiceVersions(serviceName);

                for (Iterator iter = versions.iterator(); iter.hasNext(); ) {
                    ssm.removeService(serviceName, (String)iter.next());
                }
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("doneDeletingService") + " " +
                    serviceName);
            }
        } catch (SSOException sse) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg13"), sse);
            }
            throw new AdminException(sse);
        } catch (SMSException sme) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg14"),sme);
            }
            throw new AdminException(sme); 
        }
    }

 
    private void processCleanPolicies(String serviceName) 
            throws AdminException {
        if (AdminUtils.logEnabled()) {
             String[] params = { serviceName };
//           AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//           MessageFormat.format(bundle.getString("startDeletingRules"),params));
             AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                 Level.INFO, AdminUtils.START_DELETING_RULES, params);
        }
        try {
           PolicyUtils.removePolicyRules(ssot,serviceName);

           if (AdminUtils.logEnabled()) {
               String[] params = { serviceName };
//             AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//             MessageFormat.format(
//             bundle.getString("doneDeletingRules"),params));
               AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                   Level.INFO, AdminUtils.DONE_DELETING_RULES, params);
           }

       } catch (SSOException ssoe) {
          throw new AdminException(ssoe);
       } catch (AMException ame) {
           throw new AdminException(ame);
       }

     }


    /**
     * Adds resource bundle to directory server.
     *
     * @throws AdminException if resource bundle cannot be added.
     */
    private void addResourceBundle() 
        throws AdminException
    {
        if ((resourceBundleName == null) || (resourceBundleName.length() == 0))
        {
            throw new AdminException(
                bundle.getString("missingResourceBundleName"));
        }

        if ((resourceFileName == null) || (resourceFileName.length() == 0)) {
            throw new AdminException(
                bundle.getString("missingResourceFileName"));
        }

        try {
            Map mapStrings = getResourceStringsMap(resourceFileName);
            ISResourceBundle.storeResourceBundle(ssot, resourceBundleName,
                resourceLocale, mapStrings);
            String message = null;
            String message1 = null;
            String[] params;

            if (resourceLocale != null) {
                params = new String[2];
                params[0] = resourceBundleName;
                params[1] = resourceLocale;
                message1 = MessageFormat.format(
                    bundle.getString("add-resource-bundle-to-directory-server"),
                    params);
                message = AdminUtils.ADD_RESOURCE_BUNDLE_TO_DIRECTORY_SERVER;
            } else {
                params = new String[1];
                params[0] = resourceBundleName;
                message1 = MessageFormat.format(
                    bundle.getString(
                        "add-default-resource-bundle-to-directory-server"),
                    params);
                message =
                    AdminUtils.ADD_DEFAULT_RESOURCE_BUNDLE_TO_DIRECTORY_SERVER;
            }

//            AdminUtils.logOperation(AdminUtils.LOG_ACCESS, message);
            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                Level.INFO, message, params);
            System.out.println(message1);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private Map getResourceStringsMap(String resourceFileName)
        throws AdminException
    {
        Map resourceStrings = new HashMap();
        BufferedReader in = null;

        try {
            int commented = 0;
            in = new BufferedReader(new FileReader(resourceFileName));
            String line = in.readLine();

            while (line != null) {
                line = line.trim();

                if (line.startsWith("/*")) {
                    commented++;
                } else if (line.endsWith("*/")) {
                    commented--;
                } else if (line.startsWith("#")) {
                    // ignore this line
                } else if (commented == 0) {
                    int idx = line.indexOf('=');
                    if (idx != -1) {
                        String key = line.substring(0, idx).trim();
                        if (key.length() > 0) {
                            Set tmp = new HashSet(2);
                            String value = line.substring(idx+1).trim();
                            tmp.add(value);
                            resourceStrings.put(key, tmp);
                        }
                    }
                }

                line = in.readLine();
            }
        } catch(IOException e){
            throw new AdminException(e);
        } finally {
            if (in !=null ) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new AdminException(e);
                }
            }
        }

        return resourceStrings;
    }

    /**
     * Gets resource strings from directory server.
     *
     * @throws AdminException if failed to get resource strings.
     */
    private void getResourceStrings() 
        throws AdminException
    {
        if ((resourceBundleName == null) || (resourceBundleName.length() == 0))
        {
            throw new AdminException(
                bundle.getString("missingResourceBundleName"));
        }

        try {
            ResourceBundle rb = ISResourceBundle.getResourceBundle(ssot,
                resourceBundleName, resourceLocale);

            if (rb != null) {
                for (Enumeration e = rb.getKeys(); e.hasMoreElements(); )
                {
                    String key = (String)e.nextElement();
                    String value = rb.getString(key);
                    System.out.println(key + "=" + value);
                }
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        } catch (MissingResourceException mre) {
            throw new AdminException(mre);
        }
    }

    /**
     * Deletes resource bundle from directory server.
     *
     * @throws AdminException if fails to delete resource bundle.
     */
    private void deleteResourceBundle() 
        throws AdminException
    {
        if ((resourceBundleName == null) || (resourceBundleName.length() == 0))
        {
            throw new AdminException(
                bundle.getString("missingResourceBundleName"));
        }

        try {
            ISResourceBundle.deleteResourceBundle(ssot, resourceBundleName,
                resourceLocale);
            String message = null;
            String message1 = null;
            String[] params;

            if (resourceLocale != null) {
                params = new String[2];
                params[0] = resourceBundleName;
                params[1] = resourceLocale;
                message1 = MessageFormat.format(
                    bundle.getString(
                        "delete-resource-bundle-from-directory-server"),
                    params);
                message =
                    AdminUtils.DELETE_RESOURCE_BUNDLE_FROM_DIRECTORY_SERVER;
            } else {
                params = new String[1];
                params[0] = resourceBundleName;
                message1 = MessageFormat.format(
                    bundle.getString(
                        "delete-default-resource-bundle-from-directory-server"),
                    params);
                message =
                    AdminUtils.DELETE_DEFAULT_RESOURCE_BUNDLE_FROM_DIRECTORY_SERVER;
            }

//            AdminUtils.logOperation(AdminUtils.LOG_ACCESS, message);
            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                Level.INFO, message, params);
            System.out.println(message);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private void migrate70ToRealms() 
        throws AdminException
    {
        try {
            SMSMigration70.migrate63To70(ssot, entryDN);
        } catch (Exception ex) {
            throw new AdminException(ex);
        }
    }
}
