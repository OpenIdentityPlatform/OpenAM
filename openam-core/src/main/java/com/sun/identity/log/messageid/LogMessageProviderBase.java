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
 * $Id: LogMessageProviderBase.java,v 1.7 2009/10/22 21:04:37 veiming Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.log.messageid;

/**
 * This is the base class for all Log Message Provider class. It provides
 * methods to generate XML for documenting log message.
 */

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class LogMessageProviderBase
    implements LogMessageProvider
{
    private List messageIDs = new ArrayList();
    private Map hashMessageIDs = new HashMap();
    private String xmlDefinitionFilename;

    protected LogMessageProviderBase(String xmlDef)
        throws IOException {
        xmlDefinitionFilename = xmlDef;
        registerMessageIDs();
    }

    /**
     * Returns all message IDs.
     *
     * @return all message IDs.
     */
    public List getAllMessageIDs() {
        return messageIDs;
    }

    /**
     * Returns Log Record. <code>null</code> is returned if there are no
     * corresponding entries in the XML definition file match with the
     * <code>messageIDName</code>.
     *
     * @param messageIDName Name of Message ID.
     * @param dataInfo Array of dataInfo.
     * @param ssoToken Single sign on token which will be used to fill in
     *        details like client IP address into the log record.
     * @return Log Record.
     */
    public LogRecord createLogRecord(
        String messageIDName,
        String[] dataInfo,
        Object ssoToken
    ) {
        LogRecord logRec = null;
        LogMessageID logMsgId = (LogMessageID)hashMessageIDs.get(messageIDName);

        if (logMsgId != null) {
            logRec = (ssoToken != null) ?
                new LogRecord(logMsgId.getLogLevel(),
                    formatMessage(dataInfo, logMsgId), ssoToken):
                new LogRecord(logMsgId.getLogLevel(),
                    formatMessage(dataInfo, logMsgId));

            logRec.addLogInfo(
                LogConstants.MESSAGE_ID,
                logMsgId.getPrefix() + "-" + logMsgId.getID());
        } else {
            Debug.error("LogMessageProviderBase.createLogRecord: " +
                "unable to locate message ID object for " + messageIDName);
        }

        return logRec;
    }

    /**
     * Returns Log Record. <code>null</code> is returned if there are no
     * corresponding entries in the XML definition file match with the
     * <code>messageIDName</code>.
     *
     * @param messageIDName Name of Message ID.
     * @param dataInfo Array of dataInfo.
     * @param ssoProperties Hashtable which will be used to fill in
     *        details like client IP address into the log record.
     * @return Log Record.
     */
    public LogRecord createLogRecord(
        String messageIDName,
        String[] dataInfo,
        Hashtable ssoProperties
    ) {
        LogRecord logRec = null;
        LogMessageID logMsgId = (LogMessageID)hashMessageIDs.get(messageIDName);

        if (logMsgId != null) {
            logRec = new LogRecord(logMsgId.getLogLevel(),
                formatMessage(dataInfo, logMsgId), ssoProperties);
            logRec.addLogInfo(
                LogConstants.MESSAGE_ID,
                logMsgId.getPrefix() + "-" + logMsgId.getID());
        } else {
            Debug.error("LogMessageProviderBase.createLogRecord: " +
                "unable to locale message ID object for " + messageIDName);
        }

        return logRec;
    }

    private String formatMessage(String[] dataInfo, LogMessageID logMsgId) {
        StringBuilder buff = new StringBuilder();

        if (dataInfo != null) {
            int sz = dataInfo.length;

            if (logMsgId.getNumberOfEntriesInDataColumn() != sz) {
                Debug.error("LogMessageProviderBase.formatMessage: " +
                    logMsgId.getName() + 
                    " mismatch in number of elements in string array with " +
                    "that is defined in message ID XML file");
            }

            for (int i = 0; i < sz; i++) {
                if (i > 0) {
                    buff.append(LogMessageConstants.SEPARATOR_DATA);
                }
                buff.append(dataInfo[i]);
            }
        }

        return buff.toString();
    }

    protected void registerMessageIDs()
        throws IOException {
        Document doc = getXMLDoc();
        if (doc != null) {
            Element topElement = doc.getDocumentElement();
            String tagName = topElement.getNodeName();

            if (tagName.equals(LogMessageConstants.XML_ROOT_TAG_NAME)) {
                String prefix = topElement.getAttribute(
                    LogMessageConstants.XML_ATTRNAME_PREFIX);
                NodeList children = topElement.getChildNodes();
                int numChildren = children.getLength();

                for (int i = 0; i < numChildren; i++) {
                    LogMessageID id =
                        LogMessageID.createInstance(prefix, children.item(i));

                    if (id != null) {
                        messageIDs.add(id);
                        hashMessageIDs.put(id.getName(), id);
                    }
                }
            }
        }
    }

    private Document getXMLDoc()
        throws IOException {
        Document xmlDoc = null;

        try {
            DocumentBuilder builder = XMLUtils.getSafeDocumentBuilder(true);
            builder.setErrorHandler(new ValidationErrorHandler());
            InputStream is = getClass().getClassLoader().getResourceAsStream(
                xmlDefinitionFilename);

            if (is != null) {
                xmlDoc = builder.parse(is);
            } else {
                throw new IOException(xmlDefinitionFilename +
                    " cannot be found.");
            }
        } catch (SAXParseException e) {
            Debug.error("LogMessageProviderBase.getXMLDoc", e);
        } catch (SAXException e) {
            Debug.error("LogMessageProviderBase.getXMLDoc", e);
        } catch (ParserConfigurationException e) {
            Debug.error("LogMessageProviderBase.getXMLDoc", e);
        }

        return xmlDoc;
    }

    class ValidationErrorHandler implements ErrorHandler {
        public void fatalError(SAXParseException e)
            throws SAXParseException {
            System.err.println(xmlDefinitionFilename +
                "\n" + e.getMessage() +
                "\nLine Number in XML file : " + e.getLineNumber() +
                "\nColumn Number in XML file : " + e.getColumnNumber());
        }
                                                                               
        public void error(SAXParseException e)
            throws SAXParseException {
            System.err.println(xmlDefinitionFilename +
                "\n" + e.getMessage() +
                "\nLine Number in XML file : " + e.getLineNumber() +
                "\nColumn Number in XML file : " + e.getColumnNumber());
            throw e;
        }

        public void warning(SAXParseException e)
            throws SAXParseException
        {
            System.err.println(xmlDefinitionFilename +
                "\n" + e.getMessage() +
                "\nLine Number in XML file : " + e.getLineNumber() +
                "\nColumn Number in XML file : " + e.getColumnNumber());
        }
    }
}
