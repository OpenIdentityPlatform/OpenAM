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
 * $Id: WebContainerService.java,v 1.3 2009/11/13 21:58:52 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.webcontainer;

import static com.sun.identity.diagnostic.plugin.services.webcontainer.WebContainerConstant.*;
import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.ToolLogWriter;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.base.core.service.ServiceRequest;
import com.sun.identity.diagnostic.base.core.service.ServiceResponse;
import com.sun.identity.diagnostic.base.core.service.ToolService;
import com.sun.identity.shared.debug.Debug;
import java.util.ResourceBundle;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public class WebContainerService implements ToolService, ToolConstants {
    
    private ToolContext tContext;
    private static IToolOutput toolOutWriter;
    private ResourceBundle rb;
    private static HashMap<String, String> nameToType = new HashMap();
    
    /** Creates a new instance of WebContainerService */
    public WebContainerService() {
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
        rb = ResourceBundle.getBundle(WEBCONTAINER_RESOURCE_BUNDLE);
        initStaticMap();
    }
    
    /**
     * This is called once during application start up.
     * Starts up the web-container service.
     */  
    public void start () {
    }        
    
    /**
     * This is method that processes the given request.
     *
     * @param sReq ServiceRequest object containing input params
     * @param sRes ServiceResponse object containg output results
     * @throws Exception if the exception occurs.
     */
    public void processRequest(ServiceRequest sReq, ServiceResponse sRes) 
        throws Exception 
    {
        toolOutWriter.init(sRes, rb);
        toolOutWriter.printlnResult("service-start-msg");
        ToolLogWriter.log(rb, Level.INFO,"service-start-msg", null);
        HashSet commandSet = (HashSet) sReq.getCommandSet();
        Map params = (HashMap) sReq.getData();        
        try {
            Map validatorMap = (HashMap) getValidators();
            for (Iterator j = commandSet.iterator(); j.hasNext();){
                String cmd = ((String)j.next()).toLowerCase();
                String containerPath = (String) params.get(CONTAINER_DIR);                                               
                String containerDomainPath = (String) params.get(
                    CONTAINER_DOMAIN_DIR);
                String containerType = (String) params.get(CONTAINER_TYPE);
                containerType = nameToType.get(containerType);
                if (containerType != null && containerType.length() > 0 ) {
                    String containerTypePath = 
                        containerType.replaceAll("\\s", "") + ".properties";
                    containerTypePath = tContext.getApplicationHome() +
                        "/services/webcontainer/config/" + containerTypePath;
                    File containerPropFile = new File(containerTypePath);
                    FileInputStream fin = new FileInputStream(containerPropFile);
                    Properties containerProp = new Properties();
                    containerProp.load(fin);                                                                                                
                    String jvmoptionsPath = containerType.replaceAll("\\s", "") 
                        + ".jvmoptions";
                    jvmoptionsPath = tContext.getApplicationHome() +
                        "/services/webcontainer/config/" + jvmoptionsPath;
                    containerProp.put(
                        WebContainerConstant.JVMOPTIONS_PATTERN_FILE,
                        jvmoptionsPath);
                    String policiesPath = containerType.replaceAll("\\s", "") +
                        ".policy";
                    policiesPath = tContext.getApplicationHome() +
                        "/services/webcontainer/config/" + policiesPath;
                    containerProp.put(
                        WebContainerConstant.POLICIES_PATTERN_FILE,
                        policiesPath);
                    cmd = containerType.replaceAll("\\s", "-").toLowerCase() + 
                         "-" + cmd;
                    ((IWebContainerValidate)validatorMap.get(cmd)).validate(
                         containerProp, containerPath, containerDomainPath);
                } else {
                     toolOutWriter.printError(
                         "webcontainer-container-type-invalid");
                     toolOutWriter.printStatusMsg(false, 
                         "webcontainer-container-type");
                }
            }
            toolOutWriter.printResult(sRes.getStatus());
            toolOutWriter.printResult("service-done-msg");
        } catch (Exception e) {
            throw new Exception(rb.getString("webcontainer-fatal-error") 
                + "\n" + e.getMessage());
        }
    }
    
    static IToolOutput getToolWriter() {
        return toolOutWriter;
    }

    /**
     * The reverse mapping is required to obtaine the actual webcontainer 
     * specific properties file. 
     */
    private void initStaticMap() {
        nameToType.put(WEB_CONTAINERS[0], "Sun Application Server");
        nameToType.put(WEB_CONTAINERS[1], "Sun WebServer");
        nameToType.put(WEB_CONTAINERS[2], "BEA Weblogic");
        nameToType.put(WEB_CONTAINERS[3], "IBM WebSphere");
    }
    
    /**
     * This is called once during application shutdown.
     */
    public void stop() { 
    }
    
     private Map getValidators() {
        Map opCodeToClass = new HashMap();
        FileInputStream fin = null;
        try {
            File validatorFile = new File(tContext.getApplicationHome() +
                "/services/webcontainer/config/WebContainerConfiguration");
            if (validatorFile.exists() && validatorFile.isFile()) {
                try {
                    fin = new FileInputStream(validatorFile);
                    Properties validatorProp = new Properties();
                    validatorProp.load(fin);
                    for (Enumeration names = validatorProp.propertyNames();
                        names.hasMoreElements();) {
                        String op = (String) names.nextElement();
                        String className = validatorProp.getProperty(op);
                        if (className != null &&
                            className.trim().length() > 0){
                            Class clazz = Class.forName(className);
                            opCodeToClass.put(op, (IWebContainerValidate)
                            clazz.newInstance());
                        }
                    }
                } catch (IOException ex) {
                    Debug.getInstance(DEBUG_NAME).error(
                        "WebContainerService.getValidators: " +
                        "Unable to read opcode-to-class cofiguration file", 
                     ex);
                } finally {
                    if (fin != null) {
                        try {
                            fin.close();
                        } catch (IOException ignored) {
                        }
                    }
                }  
            }
        } catch (Exception ioe) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigService.getValidators: " +
                    "Unable to read opcode-to-class cofiguration file", 
                     ioe);
        }
        return opCodeToClass;
    }
}
