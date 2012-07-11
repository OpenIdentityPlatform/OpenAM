/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CLIHandler.java,v 1.3 2009/11/13 21:53:28 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.base.core.ui.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.ToolLogWriter;
import com.sun.identity.diagnostic.base.core.ToolManager;
import com.sun.identity.diagnostic.base.core.service.ServiceRequest;
import com.sun.identity.diagnostic.base.core.service.ServiceResponse;
import com.sun.identity.diagnostic.base.core.service.ToolService;
import com.sun.identity.shared.debug.Debug;

/**
 * This is a supporting class to handle command-line options
 */
public class CLIHandler implements CLIConstants {
    
    private static ToolContext tContext = null;
    private static ResourceBundle rbundle= null;
    private static HashMap serviceCategories = new HashMap();
    private static HashMap typeToService = new HashMap();
    private static Map arguments = new HashMap();
    private static HashSet cmdSet = new HashSet();
    private static HashMap paramMap = new HashMap();
    private static HashMap typeToName = new HashMap();
    private static Set usageSet = null;
    private static ResourceBundle uiRb = 
        ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
    
    private static final int INVALID = 0;
    private static final int TOOLNAME = 1;
    private static final int CFG_DIR = 2;
    private static final int FILENAME = 3;
    private static final int CONTAINER_BASE_DIR = 4;
    private static final int CONTAINER_NAME = 5;
    private static final int CONTAINER_DOMAINDIR = 6;
    private static final int HELP = 7;
    
    static {
        arguments.put("ssodtool", new Integer(TOOLNAME));
        arguments.put("-c", new Integer(CFG_DIR));
        arguments.put("--directory", new Integer(CFG_DIR));
        arguments.put("-s", new Integer(FILENAME));
        arguments.put("--save", new Integer(FILENAME));
        arguments.put("-d", new Integer(CONTAINER_DOMAINDIR));
        arguments.put("--domain", new Integer(CONTAINER_DOMAINDIR));
        arguments.put("-b", new Integer(CONTAINER_BASE_DIR));
        arguments.put("--basedir", new Integer(CONTAINER_BASE_DIR));
        arguments.put("-t", new Integer(CONTAINER_NAME));
        arguments.put("--type", new Integer(CONTAINER_NAME));
        arguments.put("--help", new Integer(HELP));
        arguments.put("-h", new Integer(HELP));
        typeToName.put(uiRb.getString("cli_sun_app_server").trim(), 
            WEB_CONTAINERS[0]);
        typeToName.put(uiRb.getString("cli_sun_web_server").trim(), 
            WEB_CONTAINERS[1]);
        typeToName.put(uiRb.getString("cli_bea_weblogic").trim(), 
            WEB_CONTAINERS[2]);
        typeToName.put(uiRb.getString("cli_ibm_websphere").trim(), 
            WEB_CONTAINERS[3]);
    }
    
    public CLIHandler() {
    }
    
    /**
     * This method is called once during application start up to actually
     * start the application in CLI mode. This method should bring up the
     * application in CLI mode.
     */
    public static void processCLI(ResourceBundle rb) {
        rbundle = rb;
        if (tContext == null) {
            tContext =
                ToolManager.getInstance().getToolContext();
            HashMap map = tContext.getServiceCategories();
            for (Iterator<String> it = map.keySet().iterator(); it.hasNext(); ){
                String key = it.next();
                serviceCategories.put(key.toLowerCase(), map.get(key));
            }
            usageSet = map.keySet();
        }
        createMapping();
        //open up standard input
        BufferedReader br = new BufferedReader(
            new InputStreamReader(System.in));
        String inpCmd = "";
        while (!inpCmd.equalsIgnoreCase("exit")) {
            //  prompt the user to enter their name
            System.out.print(rb.getString("cli-tool-prompt"));
            try {
                cmdSet.clear();
                paramMap.clear();
                ServiceRequest sReq = new ServiceRequest();
                ServiceResponse sRes = new ServiceResponse();
                inpCmd = br.readLine();
                if (inpCmd != null && inpCmd.length() > 0 &&
                    !inpCmd.equalsIgnoreCase("exit")) {
                    boolean valid = parseCmdLine(inpCmd);
                    if (!cmdSet.isEmpty() && valid) {
                        String svcName = getServiceName(cmdSet);
                        ToolService s = tContext.getService(svcName);
                        sReq.setCommand(cmdSet);
                        sReq.setData(paramMap);
                        s.processRequest(sReq, sRes);
                        //check if save option was selected
                        if (paramMap.containsKey(SAVE_FILE_NAME)) {
                            saveToFile(sRes, (String)paramMap.get(
                                SAVE_FILE_NAME));
                        }
                        ToolLogWriter.log(sRes.getMessage());
                        ToolLogWriter.log(sRes.getError());
                        ToolLogWriter.log(sRes.getWarning());
                        ToolLogWriter.log(sRes.getResult(1));
                        ToolLogWriter.log(sRes.getResult(2));
                    }
                }
            } catch (Exception e) {
                printCLIMessage("cli-error-msg", new String[] {e.getMessage()});
                printCLIMessage("cli-error-cmd-exec");
            }
        }
        printCLIMessage("cli-exit-message");
        System.exit(1);
    }
    
    private static void saveToFile(
        ServiceResponse sRes,
        String fName
    ) {
        try {
            ToolCLISave sFile = new ToolCLISave();
            ArrayList opSet = new ArrayList();
            opSet.add(sRes.getResult());
            opSet.add(sRes.getMessage());
            opSet.add(sRes.getError());
            opSet.add(sRes.getWarning());
            sFile.saveToFile(fName, opSet, rbundle);
        } catch (Exception e) {
            printCLIMessage("cli-error-file-save", new String[] {e.getMessage()});
        }
    }
    
    private static int getToken(String arg) {
        try {
            return(((Integer)arguments.get(arg)).intValue());
        } catch(Exception e) {
            return 0;
        }
    }
    
    private static boolean hasCmd(String key) {
        return typeToService.containsKey(key);
    }
    
    private static boolean parseCmdLine(String inpCmd) throws Exception {
        String cmdSelected = null;
        boolean valid = true;
        Vector args = new Vector();
        StringTokenizer st = new StringTokenizer(inpCmd);
        while (st.hasMoreTokens()) {
            args.add(st.nextToken());
        }
        String[] argv = new String[args.size()];
        args.toArray(argv);
        if (!validateArguments(argv)) {
            valid = false;
        } else {
            for (int i = 0; i < argv.length && valid; i++) {
                int opt = getToken(argv[i]);
                switch (opt) {
                    case TOOLNAME:
                        if (i != 0) {
                            printUsage();
                        }
                        break;                
                    case CFG_DIR:
                        i++;
                        if (i >= argv.length) {
                            printCLIMessage("cli-cfg-dir-missing", new String[]
                            {argv[i-1]});
                            valid = false;
                        } else {
                            if (getToken(argv[i].toLowerCase()) != INVALID) {
                                printCLIMessage("cli-cfg-dir-missing",
                                    new String[] {argv[i-1]});
                                valid = false;
                            } else {
                                paramMap.put(CONFIG_DIR, argv[i]);
                            }
                        }
                        break;
                    case FILENAME:
                        i++;
                        if (i >= argv.length) {
                            printCLIMessage("cli-file-save-missing",
                                new String[] {argv[i-1]});
                            valid = false;
                        } else {
                            if (getToken(argv[i].toLowerCase()) != INVALID) {
                                printCLIMessage("cli-file-save-missing", new
                                    String[] {argv[i-1]});
                                valid = false;
                            } else {
                                paramMap.put(SAVE_FILE_NAME, argv[i]);
                            }
                        }
                        break;
                    case CONTAINER_BASE_DIR:
                        i++;
                        if (i >= argv.length) {
                            printCLIMessage("cli-container-dir-missing", new
                                String[] {argv[i-1]});
                            valid = false;
                        } else {
                            if (getToken(argv[i].toLowerCase()) != INVALID) {
                                printCLIMessage("cli-container-dir-missing",
                                    new String[] {argv[i-1]});
                            } else {
                                paramMap.put(CONTAINER_DIR, argv[i]);
                            }
                        }
                        break;
                    case CONTAINER_DOMAINDIR:
                        i++;
                        if (i >= argv.length) {
                            printCLIMessage("cli-container-domain-dir-missing", 
                                new String[] {argv[i-1]});
                            valid = false;
                        } else {
                            if (getToken(argv[i].toLowerCase()) != INVALID) {
                                printCLIMessage(
                                    "cli-container-domain-dir-missing",
                                    new String[] {argv[i-1]});
                            } else {
                                paramMap.put(CONTAINER_DOMAIN_DIR, argv[i]);
                            }
                        }
                        break;
                    case CONTAINER_NAME:
                        i++;
                        if (i >= argv.length) {
                            printCLIMessage("cli-container-type-missing",
                                new String[] {argv[i-1]});
                            valid = false;
                        } else {
                            if (getToken(argv[i].toLowerCase()) != INVALID) {
                                printCLIMessage("cli-container-type-missing",
                                    new String[] {argv[i-1]});
                                valid = false;
                            } else {
                                paramMap.put(CONTAINER_TYPE, 
                                    getContainerType(argv[i]));
                            }
                        }
                        break;
                    case HELP:
                        if (i == 2) {
                            printSubCmdUsage(argv[1], getSubCmds(argv[1]));
                        } else {
                            printUsage();
                        }
                        break;
                    default:
                        if (i == 1 && hasCmd(argv[i])) {
                            cmdSelected = argv[i];
                        } else if (i > 1 && hasSubCmd(cmdSelected, argv[i])) {
                            if (argv[i].equalsIgnoreCase("all")) {
                                addAllCmds(cmdSelected);
                            } else {
                                cmdSet.add(argv[i]);
                            }
                        } else {
                            valid = false;
                        }
                }
            }
        }
        if (!valid) {
            printUsage();
        }
        return valid;
    }
    
    public static void printUsage(){
        StringBuilder sBuff = new StringBuilder();
        int size = usageSet.size();
        int i = 1;
        for (Iterator it = usageSet.iterator(); it.hasNext(); i++) {
            if (i != size) {
                sBuff.append(((String)it.next()).toLowerCase()).append("|");
            } else {
                sBuff.append(((String)it.next()).toLowerCase());
            }
        }
        String[] params = {sBuff.toString()};
        printCLIMessage("cli-usage-main", params);
    }
    
    public static void printSubCmdUsage(String cName, String options){
        String str = null;
        String[] params = {cName, options, 
            uiRb.getString("cli_sun_app_server"),
            uiRb.getString("cli_sun_web_server"),
            uiRb.getString("cli_bea_weblogic"),
            uiRb.getString("cli_ibm_websphere")};

        if (cName.equalsIgnoreCase(uiRb.getString("category_webcontainer"))) {
            printCLIMessage("cli-usage-sub-cmd-3", params);
        } else if(cName.equalsIgnoreCase(uiRb.getString("category_system"))) { 
            printCLIMessage("cli-usage-sub-cmd-2", params);
        } else {
            printCLIMessage("cli-usage-sub-cmd-1", params);
        }
    }
    
    private static boolean validateArguments(String[] argv) {
        boolean retValue = true;
        String arg = null;
        Set setOpts = new HashSet(2);
        int len = argv.length;
        if (len <= 2 ) {
            retValue = false;
        } else if (len == 2) {
            arg = argv[0].toLowerCase();
            String[] param = {arg};
            if (!(arg.equals("ssodtool"))) {
                printCLIMessage("cli-invalid-option", param);
                retValue = false;
            }
            if (retValue){
                arg = argv[1].toLowerCase();
                if (!(arg.equals("--help") ||
                    arg.equals("-h"))) {
                    printCLIMessage("cli-invalid-option", param);
                    retValue = false;
                }
            }
        } else {
            retValue = false;
            if (argv[0].toLowerCase().equals("ssodtool")) {
                if (typeToService.containsKey(argv[1])) {
                    for (int i = 2; (i < len); i++) {
                        arg = argv[i].toLowerCase();
                        if ((arg.equals("--help") ||
                            arg.equals("-h"))) {
                            retValue = true;
                        } else {
                            //process others
                            retValue = hasSubCmd(argv[1], argv[i]);
                            if (!retValue && argv[i].startsWith("-")) {
                                if (arguments.containsKey(argv[i])){
                                    retValue = true;
                                    setOpts.add(argv[i]);
                                    i++;
                                    if (i != len && getToken(
                                        argv[i].toLowerCase()) == INVALID){
                                        retValue &= true;
                                    }
                                    if (i != len && setOpts.contains(argv[i])) {
                                        retValue = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return retValue;
    }
    
    private static boolean hasSubCmd(String key, String opName) {
        HashMap opCodeMap = (HashMap)typeToService.get(key);
        return opCodeMap.containsKey(opName);
    }
    
    private static String getContainerType(String ctype) {
         return (String)typeToName.get(ctype);
    }
    
    private static String getServiceName(Set cmdSet) {
        String svcName = null;
        String opName = null;
        for (Iterator it = cmdSet.iterator(); it.hasNext(); ) {
            opName = (String)it.next();
            break;
        }
        Set keys = typeToService.keySet();
        Iterator keyIter = keys.iterator();
        while (keyIter.hasNext()) {
            Object key = keyIter.next();
            HashMap svcMap = (HashMap)typeToService.get(key);
            if (svcMap.containsKey(opName)) {
                svcName = (String)svcMap.get(opName);
            }
        }
        return svcName;
    }
    
    private static void addAllCmds(String cmdSelected) {
        Map m = (Map) serviceCategories.get(cmdSelected);
        Set keys = m.keySet();
        Iterator keyIter = keys.iterator();
        while (keyIter.hasNext()) {
            Object key = keyIter.next();
            HashMap svcMap = (HashMap)m.get(key);
            Set opKeys = svcMap.keySet();
            Iterator opIter = opKeys.iterator();
            while (opIter.hasNext()) {
                String okey = (String)opIter.next();
                if (!okey.equalsIgnoreCase("all")) {
                    cmdSet.add(okey.toLowerCase());
                }
            }
        }
    }
    
    private static String getSubCmds(String cName) {
        StringBuilder sBuff = new StringBuilder();
        HashMap opCodeMap = null;
        try {
            String keyName = cName.toLowerCase();
            opCodeMap = (HashMap)typeToService.get(keyName);
            if (!opCodeMap.isEmpty()) {
                Set keys = opCodeMap.keySet();
                Iterator keyIter = keys.iterator();
                while (keyIter.hasNext()) {
                    String key = (String)keyIter.next();
                    if (!key.equalsIgnoreCase("all")) {
                        sBuff.append(" ").append(key).append(" ");
                    }
                }
                if (opCodeMap.containsKey("all")) {
                    sBuff.append("|").append(" all ");
                }
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "CLIHandler.getSubCmds: " + e.getMessage());
        }
        return sBuff.toString();
    }
    
    private static void createMapping() {
        try {
            for (Iterator<String> it = serviceCategories.keySet().iterator();
                it.hasNext(); ) {
                HashMap opToService = new HashMap();
                String typeKey = it.next();
                Map m = (Map) serviceCategories.get(typeKey);
                Set keys = m.keySet();
                Iterator keyIter = keys.iterator();
                while (keyIter.hasNext()) {
                    Object key = keyIter.next();
                    HashMap svcMap = (HashMap)m.get(key);
                    Set opKeys = svcMap.keySet();
                    Iterator opIter = opKeys.iterator();
                    while (opIter.hasNext()) {
                        String okey = (String)opIter.next();
                        String value = (String)svcMap.get(okey);
                        opToService.put(okey.toLowerCase(), value.toLowerCase());
                    }
                }
                typeToService.put(typeKey.toLowerCase(), opToService);
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "CLIHandler.createMapping: " + e.getMessage());
        }
    }
    
    private static void printCLIMessage(String str, Object[] params) {
        String msg = MessageFormat.format(rbundle.getString(str),(Object[])params);
        System.out.println(msg);
    }
    
    private static void printCLIMessage(String str) {
        System.out.println(rbundle.getString(str));
    }
}
