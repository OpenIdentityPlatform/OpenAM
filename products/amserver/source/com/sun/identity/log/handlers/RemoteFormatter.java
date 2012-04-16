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
 * $Id: RemoteFormatter.java,v 1.9 2009/08/19 21:12:50 ww203982 Exp $
 *
 */


package com.sun.identity.log.handlers;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Formatter;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.ILogRecord;
import com.sun.identity.log.spi.Debug;

/**
 * The formatter which prepares the xml string to be sent to a remote log
 * service. This xml string conforms to the logDTD.xml file which is given
 * below.
 *
 * <?XML version="1.0">
 *
 * <!--This DTD is used by Logging operation-->
 * <!DOCTYPE logging [
 *
 * <!ELEMENT logRecWrite (log, logRecord*)>
 * <!ELEMENT log>
 * <!ATTLIST log
 *  logName          CDATA #REQUIRED
 *  sid              CDATA #REQUIRED
 * >
 * <!ELEMENT logRecord (level ,msg ,logType? , logInfo? ,parameters?)>
 * <!ELEMENT level (#PCDATA)>
 * <!ELEMENT recMsg (#PCDATA)>
 * <!ELEMENT logType (#PCDATA)>
 * <!ELEMENT logInfoMap (logInfo*)>
 * <!ELEMENT logInfo (key ,infoValue)>
 * <!ELEMENT key (#PCDATA)>
 * <!ELEMENT infoValue (#PCDATA)>
 * <!ELEMENT parameters (parameter*)>
 * <!ELEMENT parameter (index ,paramValue)>
 * <!ELEMENT index (#PCDATA)>
 * <!ELEMENT paramValue (#PCDATA)>
 * ]
 */
public class RemoteFormatter extends Formatter {

    /**
      * The method which does the actual formatting of the LogRecord.
      * @param logRecord The logRecord to be formatted
      * @return The string formed by formatting the logRecord
      */
    public String format(java.util.logging.LogRecord logRecord) {
        Map logInfo = null;
        if ((LogManagerUtil.isAMLoggingMode()) &&
            (logRecord instanceof ILogRecord)) {
            logInfo = ((com.sun.identity.log.ILogRecord) logRecord)
                .getLogInfoMap();
        }
        String logName = logRecord.getLoggerName();
        String loggedBySid = null;

        if (logInfo != null) {
            loggedBySid = (String)logInfo.get(
                LogConstants.LOGGED_BY_SID);
            if (loggedBySid == null) {
                if (Debug.warningEnabled()) {
                    Debug.warning("RemoteFormatter : returning null" +
                        " because logRecord doesn't have loggedBySid");
                }
                return null;
            }
        }

        Object [] parameters = logRecord.getParameters();
        StringBuffer xml = new StringBuffer();
        xml.append("<logRecWrite><log logName=\"");
        xml.append(logName);
        xml.append("\" sid=\"");
        xml.append(loggedBySid);
        xml.append("\"></log><logRecord><level>");
        xml.append(logRecord.getLevel().intValue());
        xml.append("</level><recMsg>");

        String msg = formatMessage(logRecord);
        if ((msg == null) || (msg.length() == 0)) {
            msg = LogConstants.NOTAVAIL;
        }
        msg = com.sun.identity.shared.encode.Base64.encode(msg.getBytes());

        xml.append(msg);
        xml.append("</recMsg>");

        if (logInfo != null) {
            Set keys = logInfo.keySet();
            Iterator keysIter = keys.iterator();
            xml.append("<logInfoMap>");
            while (keysIter.hasNext()) {
                String key = (String)keysIter.next();
                xml.append("<logInfo> <infoKey>");
                xml.append(key);
                xml.append("</infoKey><infoValue>");
                String infoValue = (String)logInfo.get(key);
                if (key.equalsIgnoreCase(LogConstants.DATA)) {
                    infoValue = com.sun.identity.shared.encode.Base64.encode(
                        infoValue.getBytes());
                }
                xml.append(infoValue);
                xml.append("</infoValue></logInfo>");
            }
            xml.append("</logInfoMap>");
        }
        if ((parameters != null) && (parameters.length > 0)) {
            xml.append("<parameters>");
            for (int i=0; i<parameters.length; i++) {
                xml.append("<parameter><paramIndex>");
                xml.append(String.valueOf(i));
                xml.append("</paramIndex><paramValue>");
                xml.append(parameters[i].toString());
                xml.append("</paramValue></parameter>");
            }
            xml.append("</parameters>");
        }
        xml.append("</logRecord></logRecWrite>");
        if (Debug.messageEnabled()) {
            Debug.message("RemoteFormatter: XML Req string = " + xml);
        }
        return xml.toString();
    }

}
