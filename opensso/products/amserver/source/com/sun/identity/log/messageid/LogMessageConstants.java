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
 * $Id: LogMessageConstants.java,v 1.3 2008/06/25 05:43:37 qcheng Exp $
 *
 */



package com.sun.identity.log.messageid;

public interface LogMessageConstants {
    /**
     * Root element tagname of Log Message XML.
     */
    String XML_ROOT_TAG_NAME = "logmessages";

    /**
     * Prefix attribute name of root element in Log Message XML.
     */
    String XML_ATTRNAME_PREFIX = "prefix";

    /**
     * log message element tag name in Log Message XML.
     */
    String XML_LOG_MESSAGE_TAG_NAME = "logmessage";

    /**
     * Name attribute name of log message element name in Log Message XML.
     */
    String XML_ATTRNAME_LOG_MESSAGE_NAME = "name";

    /**
     * Log level attribute name of log message element name in Log Message XML.
     */
    String XML_ATTRNAME_LOG_LEVEL = "loglevel";

    /**
     * ID attribute name of log message element name in Log Message XML.
     */
    String XML_ATTRNAME_LOG_MESSAGE_ID = "id";

    /**
     * Description attribute name of log message element name in Log Message
     * XML.
     */
    String XML_ATTRNAME_LOG_MESSAGE_DESCRIPTION = "description";

    /**
     * Data information element tag name in Log Message XML.
     */
    String XML_DATAINFO_TAG_NAME = "datainfo";

    /**
     * Triggers element tag name in Log Message XML.
     */
    String XML_TRIGGERS_TAG_NAME = "triggers";

    /**
     * Actions element tag name in Log Message XML.
     */
    String XML_ACTIONS_TAG_NAME = "actions";

    /**
     * Separator of data information.
     */
    String SEPARATOR_DATA = "|";
}
