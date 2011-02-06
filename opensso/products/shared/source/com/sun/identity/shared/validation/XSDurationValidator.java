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
 * $Id: XSDurationValidator.java,v 1.2 2008/06/25 05:53:07 qcheng Exp $
 *
 */

package com.sun.identity.shared.validation;

import com.sun.identity.shared.DateUtils;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

/**
 * Validator for <code>xs:duration</code> format.
 * The validation of the cache duration is done based on the following:
 *     If the number of years, months, days, hours, minutes, or seconds in
 *     any expression equals zero, the number and its corresponding
 *     designator may be omitted. However, at least one number and its
 *     designator must be present.
 *     The seconds part may have a decimal fraction.
 *     The designator 'T' must be absent if and only if all of the
 *     time items are absent.
 *     The designator 'P' must always be present.
 */
public class XSDurationValidator
    extends ValidatorBase
{
    private static XSDurationValidator instance = new XSDurationValidator();
    private static boolean debug = false;

    private XSDurationValidator() {
    }

    /**
     * Returns an instance of this validator.
     *
     * @return an instance of this validator.
     */
    public static XSDurationValidator getInstance() {
        return instance;
    }

    /* validate the cache duration string */
    protected void performValidation(String strData)
        throws ValidationException
    {
        // if the string is null or empty or less then 3 charactors
        // then throw error 
        if ((strData == null) || (strData.trim().length() == 0)  
                || (strData.trim().length() < 3)) {
            throw new ValidationException(resourceBundleName, "errorCode4"); 
        }

        // expecting this format P1Y2M4DT9H8M20S

        // first character should be a P
        if (strData.charAt(0) != 'P') {
            throw new ValidationException(resourceBundleName, "errorCode4"); 
        }

        // time data validation
        String timeStr = null;
        String dateStr = null;
        if (strData.indexOf('T') != -1) {
            // T without time elements H M S error
            if (strData.charAt(strData.length()-1) == 'T' ) {
                throw new ValidationException(resourceBundleName,"errorCode4"); 
            } 

            int strIndex = strData.indexOf("T");
            timeStr = strData.substring(strIndex+1,strData.length());
            dateStr = strData.substring(1,strIndex);
        } else {
            // time elements without T not allowed.
            if (strData.indexOf("H") != -1 || strData.indexOf("S") != -1) {
                throw new ValidationException(resourceBundleName,"errorCode4"); 
            }    
            dateStr = strData.substring(1);
        }            
        int start = 0;
        processTimeStr(timeStr);
        processDateStr(dateStr);
                
        return;
    }

    private int getIntegerValue(String strData, int start, int end)
        throws ValidationException
    {
        int value = 0;
        String str = strData.substring(start, end);


        try {
            value = Integer.parseInt(str);

            if (value < 0) {
                throw new ValidationException(resourceBundleName, "errorCode4");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException(resourceBundleName, "errorCode4");
        }

        return value;
    }

    private double getDoubleValue(String strData, int start, int end)
        throws ValidationException
    {
        double value = 0;
        String str = strData.substring(start, end);

        try {
            value = Double.parseDouble(str);

            if (value < 0) {
                throw new ValidationException(resourceBundleName, "errorCode4");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException(resourceBundleName, "errorCode4");
        }

        return value;
    }


    /* Process the Time String in the Duration */
    private void processTimeStr(String timeStr) throws ValidationException {
        if (timeStr != null) {
            if ((timeStr.indexOf("Y") != -1) || (timeStr.indexOf("D") != -1)) {
                throw new ValidationException(resourceBundleName, "errorCode4");
            }
            // Seconds should be preceded by a decimal number , if there
            // is a "." then should have some number after it.
            int idxH = timeStr.indexOf('H');
            int idxM = timeStr.indexOf('M');
            int idxS = timeStr.indexOf('S');
            if ((idxH != -1) && ((idxM != -1 && idxM < idxH) 
                            || (idxS != -1 && idxS < idxH))) {
                throw new ValidationException(resourceBundleName,"errorCode4"); 
            }

            if ((idxM != -1) && (idxS != -1 && idxS < idxM)) {
                throw new ValidationException(resourceBundleName,"errorCode4"); 
            }
        
            int start=0;
            if (idxH != -1) {
                int a = getIntegerValue(timeStr,start,idxH);
                
                idxM = timeStr.indexOf('M',idxH+1);
                start = idxH+1;
            } 
 
            if (idxM != -1) {
                    int b = getIntegerValue(timeStr,start,idxM);
                idxS = timeStr.indexOf('S',idxM +1);
                start = idxM + 1;
            } 

            if (idxS != -1) {
                if (timeStr.charAt(idxS-1) == '.') {
                    throw new ValidationException(
                        resourceBundleName, "errorCode4"); 
                }
                getDoubleValue(timeStr,start,idxS);
            } 
        }
    }

    /* Process the Date String in the Duration */
    private void processDateStr(String dateStr) throws ValidationException {
        if (dateStr != null) {
            if ((dateStr.indexOf("H") != 1) && (dateStr.indexOf("S") != -1)) {
                throw new ValidationException(resourceBundleName,"errorCode4"); 
            }
            int idxY = dateStr.indexOf('Y');
            int idxM = dateStr.indexOf('M');
            int idxD = dateStr.indexOf('D');
            if ((idxY != -1) && ((idxM != -1 && idxM < idxY) 
                            || (idxD != -1 && idxD < idxY))) {
                throw new ValidationException(resourceBundleName,"errorCode4"); 
            }
            if ((idxM != -1) && (idxD != -1 && idxD < idxM)) {
                throw new ValidationException(resourceBundleName,"errorCode4"); 
            }
            int start = 0;
            if (idxY != -1) {
                getIntegerValue(dateStr, start, idxY);
                idxM = dateStr.indexOf('M',idxY+1);
                start = idxY +1;
            } 
            if (idxM != -1) {
                getIntegerValue(dateStr,start,idxM);
                idxD = dateStr.indexOf('D',idxM+1);
                start = idxM + 1;
            } 
            if (idxD != -1) {
                getIntegerValue(dateStr, start, idxD);
            }
        }
        return;
    }
}
