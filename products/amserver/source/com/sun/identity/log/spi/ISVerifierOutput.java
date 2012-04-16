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
 * $Id: ISVerifierOutput.java,v 1.3 2008/06/25 05:43:40 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.spi;

/**
 *  DSAME Implementation of the result of a Verifier Output.
 */
public class ISVerifierOutput implements IVerifierOutput{
    
    /**
     *  Does the action based on the result of the verification process.
     *  @param logName  The name of the log on which verification was carried
     *                  out.
     *  @param  result  The result of the verification process.
     *  @return boolean value as the result of the verifier action taken.
     */
    public boolean doVerifierAction(String logName, boolean result){
        if(result){
            if (Debug.messageEnabled()) {
                Debug.message(logName + ":Verification Successful");
            }
        }else{
            Debug.error(logName + ":Verification Failure");
        }
        return result;
    }
}
