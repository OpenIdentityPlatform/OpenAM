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
 * $Id: SampleService.java,v 1.1 2008/11/22 02:41:21 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.sample;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.base.core.service.ServiceRequest;
import com.sun.identity.diagnostic.base.core.service.ServiceResponse;
import com.sun.identity.diagnostic.base.core.service.ToolService;


/**
 * This class contains the implementation of a sample service that
 * can be realized in the Diagnostic tool application. All the services
 * should implement ToolService interface.
 *
 */
public class SampleService implements ToolService {
    
    private ToolContext tContext;
    private static IToolOutput toolOutWriter;
    private ResourceBundle rb;
    
    /** Creates a new instance of SampleService */
    public SampleService() {
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
        //Initialize the rb with YOUR_RESOURCE_BUNDLE
        //rb = ResourceBundle.getBundle(YOUR_RESOURCE_BUNDLE);
    }
    
    /**
     * This is called once during application start up.
     * Starts up the sample service.
     */
    public void start() {
        //Print the service started messsage
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
        boolean error = false;
        //Initialize the output writer with YOUR_RESOURCE_BUNDLE
        //toolOutWriter.init(sRes, rb);
        
        //Remove this line once tool writer is initialized with
        //YOUR_RESOURCE_BUNDLE
        toolOutWriter.init(sRes, null);
        
        HashSet commandSet = (HashSet)sReq.getCommandSet();
        Map inpParams = (HashMap)sReq.getData();
        
        toolOutWriter.printMessage(
            "SampleService: Processing Request ...\n");
        toolOutWriter.printMessage(
            "Command : " + commandSet.toString());
        toolOutWriter.printMessage("Data : " + inpParams.toString() + "\n");
        
        for (Iterator j = commandSet.iterator(); j.hasNext();){
            String command = (String)j.next();
            if (command.equalsIgnoreCase("YOURCMDSTR")) {
                //Do your processing
            }
        }     
        if (error) {
             toolOutWriter.printError("Some error occured");
        }
        toolOutWriter.printResult(sRes.getStatus());
        toolOutWriter.printMessage("SampleService: Done ...");
    }
    
    /**
     * This is called once during application shutdown. Any processing
     * that needs to be done when the service ends can be done here.
     */
    public void stop() {
    }
}
