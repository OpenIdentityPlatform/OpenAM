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
 * $Id: LogSign.java,v 1.4 2008/06/25 05:43:38 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.secure;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Handler;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.Logger;
import com.sun.identity.log.spi.Debug;

/**
 * This class is logging signature that generates with the MAC value for each
 * log entry.
 */
public class LogSign {
    private String name;
    private Logger logger;
    
    /**
     *  Constructor
     *  @param  log A string representing the name of the logger.
     */
    public LogSign(String log) {
        name = log;
    }
    
    /**
     *  Reads the header from the log file and interprets its contents.
     *  It finds out the position of the Signature and MAC fields.
     *  @param recordListHeader A string array that contains the header 
     *         entries as strings.
     */
    public void readHeader(String[] recordListHeader){
        Vector header = new Vector(recordListHeader.length);
        // Extracting the field names as header from the first line of the
        // returned string array.
        header.addAll(Arrays.asList(recordListHeader));
        
        /* Getting the position of the Signature field in the array.
         * This is required to check if the record is a signature or a
         * normal logrecord. If it is a signature then the signature
         * has to be verified, else the MAC on that line has to be
         * verified.
         */
        String signFieldName = LogConstants.SIGNATURE_FIELDNAME;
        for(int j = 0; j < header.size(); j++){
            if((((String)header.get(j))).equalsIgnoreCase(signFieldName)) {
                break;
            }
        }
    }
    
    /**
     *  The actual sign method that creates the signature by taking the last
     *  generated MAC and the last Signature in the log file and then using that
     *  to create the next signature that is to be written to the file.
     *  @return A String repersentation of the actual byte array signature.
     *  @throws Exception if it fails to sign the mac value
     */
    public synchronized String sign()
    throws Exception {
        /*
         * Get instance of the Logger for which the signing operation is 
         * to be done.
         */
        logger = (com.sun.identity.log.Logger)Logger.getLogger(name);
        Handler[] handlers = logger.getHandlers();
        SecureLogHelper helper = 
            ((com.sun.identity.log.handlers.SecureFileHandler)handlers[0]).
             getSecureLogHelper();
        // Get the lastMAC and the last Signature from the secure store.
        // There is a problem since the getLastMAC function returns the MAC
        // of the record prior to the last record.
        byte[] prevMAC = new byte[1];
        prevMAC = helper.getLastMAC();
        if(prevMAC == null){
            if (Debug.warningEnabled()) {
                Debug.warning(name+"Prev MAC = null");
            }
            return null;
        }
        if (Debug.messageEnabled()) {
            Debug.message(name+"prevMAC = " + helper.toHexString(prevMAC));
        }
        byte[] newMAC;
        byte [] prevSign = helper.getLastSignatureBytes();
        if((prevSign == null) || (prevSign.length == 0) ) {
            newMAC = new byte[prevMAC.length];
            System.arraycopy(prevMAC, 0, newMAC, 0, prevMAC.length);
        } else {
            newMAC = new byte[prevMAC.length + prevSign.length];
            System.arraycopy(prevMAC, 0, newMAC, 0, prevMAC.length);
            System.arraycopy(prevSign, 0, newMAC, prevMAC.length,
                prevSign.length);
        }
        // Sign the newly generated MAC
        byte[] curSign = helper.signMAC(newMAC);
        String sign = helper.toHexString(curSign);
        return sign;
    }
}
