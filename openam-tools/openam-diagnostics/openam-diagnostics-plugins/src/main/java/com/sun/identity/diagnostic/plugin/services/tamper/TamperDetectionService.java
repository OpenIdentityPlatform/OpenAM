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
 * $Id: TamperDetectionService.java,v 1.3 2009/11/13 21:58:21 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.tamper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.ToolLogWriter;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.base.core.service.ServiceRequest;
import com.sun.identity.diagnostic.base.core.service.ServiceResponse;
import com.sun.identity.diagnostic.base.core.service.ToolService;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;
import com.sun.identity.shared.debug.Debug;

public class TamperDetectionService extends ServiceBase implements ToolService {
    
    private ToolContext tContext;
    private static IToolOutput toolOutWriter;
    private ResourceBundle rb;
    
    /** Creates a new instance of TamperDetectionService */
    public TamperDetectionService() {
    }
    
    /**
     * This is called once during service activation.
     * It caches the given <code>ToolContext</code>.
     *
     * @param tContext ToolContext in which this service runs.
     */
    public void init(ToolContext tContext) {
        this.tContext = tContext;
        toolOutWriter = tContext.getOutputWriter();
        rb = ResourceBundle.getBundle("TamperDetection");
    }
    
    /**
     * This is called once during application start up.
     * Starts up the tamperdetection service.
     */
    public void start() {
    }
      
    /**
     * This is method that processes the given request.
     *
     * @param sReq ServiceRequest object containing input params
     * @param sRes ServiceResponse object containg output results
     * @throws Exception if the exception occurs.
     */
    public void processRequest(
        ServiceRequest sReq,
        ServiceResponse sRes
    ) throws Exception {
        toolOutWriter.init(sRes, rb);
        toolOutWriter.printlnResult("service-start-msg");
        ToolLogWriter.log(rb,Level.INFO,"service-start-msg", null);
        HashSet commandSet = (HashSet)sReq.getCommandSet();
        Map params = (HashMap)sReq.getData();
        try {
            Map validatorMap = (HashMap) getValidators();
            for (Iterator j = commandSet.iterator(); j.hasNext();){
                String cmd = ((String)j.next()).toLowerCase();
                String configPath = (String) params.get(CONFIG_DIR);
                File configDir = new File(configPath);
                if (configDir.exists() && configDir.isDirectory()) {
                    toolOutWriter.printStatusMsg(true, 
                        "tamper-config-dir-check");
                    String backupFileName = configPath.replaceAll(" ", "")
                        .replaceAll("\\\\", "_").replaceAll(":", "_").replaceAll("/", "_");
                    String backupFile = tContext.getApplicationHome() + 
                        "/services/tamperdetection/backup/" + backupFileName + 
                        ".checksum" ;
                    String filtersPath = tContext.getApplicationHome() + 
                        "/services/tamperdetection/config/Filters.properties";
                    File filtersFile = new File(filtersPath);
                    Properties filterProp = new Properties();
                    if (filtersFile.exists() && filtersFile.isFile()) {
                        FileInputStream fin = new FileInputStream(filtersFile);
                        filterProp.load(fin);
                    }
                    String fileFilterString = filterProp.getProperty("file");
                    Set<String> filesFilter = new HashSet<String>();
                    if (fileFilterString != null) {
                        StringTokenizer st = new StringTokenizer(
                            fileFilterString, ",");
                        for (int i = 0; i < st.countTokens(); i++) {
                            filesFilter.add(st.nextToken());
                        }
                    }
                    String dirFilterString = filterProp.getProperty("dir");
                    Set<String> dirsFilter = new HashSet<String>();
                    if (dirFilterString != null) {
                        StringTokenizer st = new StringTokenizer(
                            dirFilterString, ",");
                        while (st.hasMoreTokens()) {
                            dirsFilter.add(st.nextToken());
                        }
                    }
                    ((ITamperDetector)validatorMap.get(cmd)).detectTamper(
                        configPath, backupFile, dirsFilter, filesFilter);
                } else {
                    toolOutWriter.printError("tamper-config-dir-invalid");
                    toolOutWriter.printStatusMsg(false, 
                        "tamper-config-dir-check");
                }
            }
            toolOutWriter.printResult(sRes.getStatus());
            toolOutWriter.printResult("service-done-msg");
        } catch (Exception e) {
            throw new Exception(rb.getString("tamper-fatal-error")
            + "\n" + e.getMessage());
        }
    }
    
    static IToolOutput getToolWriter() {
        return toolOutWriter;
    }
    private Map getValidators() {
        Map opCodeToClass = new HashMap();
        BufferedReader in = null;
        try {
            in = new BufferedReader(
                new FileReader(tContext.getApplicationHome() + 
                "/services/tamperdetection/config/TamperDetectionConfiguration"));
            if (in.ready()) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.indexOf("=") != -1) {
                        StringTokenizer st =
                            new StringTokenizer(line, "=");
                        String op = st.nextToken();
                        String className = st.nextToken();
                        if (op != null && op.trim().length() > 0) {
                            if (className != null &&
                                className.trim().length() > 0){
                                Class clazz = Class.forName(className);
                                opCodeToClass.put(op.toLowerCase(), 
                                    (ITamperDetector)clazz.newInstance());
                            }
                        }
                    }
                }
            }
        } catch (Exception ioe) {
            Debug.getInstance(DEBUG_NAME).error(
                "TamperDetectionService.getValidators: " +
                "Unable to read opcode-to-class cofiguration file",
                ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
        }
        return opCodeToClass;
    }
    
    /**
     * This is called once during application shutdown.
     */
    public void stop() { 
    }
}
