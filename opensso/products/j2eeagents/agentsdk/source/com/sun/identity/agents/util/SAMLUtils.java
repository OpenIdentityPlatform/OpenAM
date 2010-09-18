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
 * $Id: SAMLUtils.java,v 1.2 2008/06/25 05:52:00 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;

import java.security.SecureRandom;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.iplanet.am.util.SecureRandomManager;

import com.sun.identity.agents.arch.AgentException;

public class SAMLUtils implements IUtilConstants {
    
    private SecureRandom secureRandom;

    public SAMLUtils() throws AgentException {        
        try {        
            secureRandom = SecureRandomManager.getSecureRandom();
        } catch (Exception e) {
            throw new AgentException("Initialization of SecureRandom failed: ", 
                e);
        }
    }

    public String generateID() {

        byte bytes[] = new byte[INT_REQUEST_ID_LENGTH];
        secureRandom.nextBytes(bytes);
        String encodedID = STR_REQUEST_ID_PREFIX + getHexString(bytes);

        return encodedID;
    }

    private String getHexString(byte[] byteArray) {
        int readBytes = byteArray.length;
        StringBuffer hexData = new StringBuffer();
        for (int i=0; i < readBytes; i++) {
          int onebyte = ((0x000000ff & byteArray[i]) | 0xffffff00);
          hexData.append(Integer.toHexString(onebyte).substring(6));
        }

        return hexData.toString();
    }

    public String dateToUTCString(Date date) throws ParseException {
        StringBuffer sb = new StringBuffer(100);

        SimpleDateFormat utcDateFormat = new SimpleDateFormat(
            STR_UTC_DATE_FORMAT);
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String utcStr =  utcDateFormat.format(date, sb,
            new FieldPosition(0)).toString();

        return utcStr;
    }
}
