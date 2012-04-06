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
 * $Id: VerifierAction.java,v 1.3 2008/06/25 05:43:40 qcheng Exp $
 *
 */



package com.sun.identity.log.spi;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;

/** A class that contains static  functions used to instantiate the
 *  class that has been configured for VerifierOutput.
 */

public class VerifierAction {
    private static IVerifierOutput vout;
    private static LogManager lmanager;
    static {
        lmanager = (LogManager)LogManagerUtil.getLogManager();
        String voutClass = 
            lmanager.getProperty(LogConstants.VERIFIER_ACTION_CLASS);
        try{
            
            Class c = Class.forName(voutClass);
            vout = (IVerifierOutput)c.newInstance();
        } catch(Exception e) {
            Debug.error("Authorizer : Exception : ", e);
        }
    }
    
    /** 
     *  Call the appropriate function to take the appropriate action
     *  for the result of a log verification.
     *
     *  @param logName The name of the logger on which the verifier action
     *                  is to be taken.
     *  @param result   Boolean value of the result of the log verification.
     *  @return boolean value of the result of the verification process.
     */
    
    public static boolean doVerifierAction(String logName, boolean result) {
        return vout.doVerifierAction(logName, result);
    }
}
