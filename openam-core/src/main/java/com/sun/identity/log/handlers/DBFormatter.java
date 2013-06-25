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
 * $Id: DBFormatter.java,v 1.11 2009/08/19 21:12:50 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock, Inc.
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.log.handlers;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.spi.Debug;

/**
 * This Formatter provides support for formatting LogRecords that will help
 * Database Logging.
 * <p>
 * Typically this Formatter will be associated with a DBHandler (a handler meant
 * to handle Database logging). <tt> DBFormatter </TT> takes a LogRecord and
 * converts it to a Formatted string which DBHandler can understand.
 *
 */
public class DBFormatter extends Formatter {

    private LogManager lmanager = LogManagerUtil.getLogManager();

    /**
     * Creates <code>DBFormatter</code> object
     */
    public DBFormatter() {
    }

    /**
     * Returns the set of all fields converted into a COMMA seperated 
     * string. A typical sql query for logging a record looks like this. <p>
     * insert into table "amSSO_access" (time, data, loginid, domain, level,
     * ipAddress, hostname) values('10:10:10', '10th June, 2002',
     * ..., ..., ...)<p>
     * The getHead method returns back the set of all fields converted into a
     * COMMA seperated string. It is the duty of the formatter to fetch the all
     * field set from the LogManager and convert into a COMMA seperated string.
     * By doing this the handler can be kept independent of the all field and
     * selected field set.
     *
     * @param h The target handler (can be null)
     * @return the set of all fields converted into a COMMA seperated string.
     */
    @Override
    public String getHead(Handler h) {
        String retString = lmanager.getProperty(LogConstants.ALL_FIELDS);
        if (Debug.messageEnabled()) {
            Debug.message("DBFormatter: Returned String from getHead is " 
                + retString);
        }
        return retString;
    }
    
    /**
     * Returns a null string whenever called.
     * @param h The target handler (can be null)
     * @return a null string whenever called.
     */
    @Override
    public String getTail(Handler h) {
        return "";
    }

    /**
     * Simply return the value from calling formatMessage. All DBFormatting is now handled directly in DBHandler.
     * @param logRecord The LogRecord to format
     * @return A String that represents the formatted LogRecord
     */
    @Override
    public String format(LogRecord logRecord) {
        return formatMessage(logRecord);
    }
}