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
 * $Id: MACGenerator.java,v 1.3 2008/06/25 05:43:38 qcheng Exp $
 *
 */



package com.sun.identity.log.secure;

import com.sun.identity.log.handlers.SecureFileHandler;
import com.sun.identity.log.spi.IGenerator;

/**
 *  MACGenerator class implements the IGenerator interface and provides 
 *  the mechanism to generate MAC for the data which is then used for 
 *  checking for tampering.
 **/
public class MACGenerator implements IGenerator {
    /** 
     * Computes the MAC of the given data and converts it to string and 
     * returns it back.
     *
     * @param data is the data on which the MAC is to be calculated.
     * @param params is an array of required objects for computing the MAC
     * @return a String representing the generated MAC value for the given data.
     * @throws Exception if it fails to generate mac value for log entry
     */
    public String generateLogField(String data, Object[] params)
    throws Exception {
        // The params array contains the log name(i.e. logname.type).
        String log = (String)params[0];

        SecureLogHelper helper = SecureFileHandler.getSecureLogHelper(log);
        byte[] MAC = helper.generateLogEntryMAC(data);
        String sMAC = helper.toHexString(MAC);
        return(sMAC);
    }
}

