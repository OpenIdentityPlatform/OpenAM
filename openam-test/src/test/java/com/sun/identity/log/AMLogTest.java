/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMLogTest.java,v 1.3 2008/06/25 05:44:19 qcheng Exp $
 *
 */

package com.sun.identity.log;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.log.AMLogException;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.LogQuery;
import com.sun.identity.log.LogReader;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.QueryElement;
import com.sun.identity.shared.test.CollectionUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.test.common.FileHelper;
import com.sun.identity.test.common.TestBase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests the
 *    <code>com.sun.identity.log.LogRecord</code> and
 *    <code>com.sun.identity.log.Logger</code> classes.
 */
public class AMLogTest extends TestBase {
    
    public AMLogTest() {
        super("LogTest");
    }

    /**
     *  before running test:
     *    clean out the logfile (or remove it) - manual task
     *
     * @throws SSOException if the super administrator Single Sign On is 
     *         invalid.
     */

    private String theRealm = null;

    // msgDatPrefixData is the "data" part of msgDataPrefix
    private final String msgDataPrefixData = "data #";
    private final String msgDataPrefix = "AMLogTest " + msgDataPrefixData;
    private final String defaultLogName = "AMLogTestLog";

    private String loggingLocation = null;
    private String logName = null;
    private String logPath = null;
    private Logger logger = null;

    private java.util.logging.LogManager lmgr = null;


    /**
     *  the logging testng program currently consists of
     *  logging attempts by amadmin.
     *  logwrite-number-of-records records should be written.
     *
     *  the log reading tests exercise the LogReader formats, with
     *  LogQuery and QueryElement classes.
     */


    /**
     *  suiteSetup
     *  For setup, get:
     *    o a logger instance
     */

    @Parameters({"logtest-realm"})
    @BeforeSuite(groups = {"api-adminwrite", "api-adminread"})
    public void suiteSetup (String realm) throws Exception {
        Object[] params = {realm};
        entering("suiteSetup", params);

        try {
            lmgr = LogManagerUtil.getLogManager();  // NOTE5
        } catch (Exception e) {
            log(Level.SEVERE, "suiteSetup", e.getMessage(), params);
            e.printStackTrace();
            throw e;
        }
        theRealm = realm;
        exiting("suiteSetup");
    }



    /**
     * Before running the test(s), ensure:
     *   o the log svc config is set with buffer timer off, buff size = 1
     *
     * @throws Exception if an AMLogException occurs
     */
    @Parameters({"logtest-log-location", "logtest-logname"})
    @BeforeTest(groups = {"api-adminwrite", "api-adminread"})
    public void setup(String logLoc, String logFName)
        throws Exception {
        Object[] params = {theRealm, logLoc, logFName};
        entering("setup", params);
        setbufferSizer("OFF", "1");

        try {
            lmgr.readConfiguration();
            String tlogLoc = lmgr.getProperty(LogConstants.LOG_LOCATION);
            if ((tlogLoc == null) || (tlogLoc.length() == 0)) {
                tlogLoc = logLoc;
            }
            if ((logFName != null) && (logFName.length() > 0)) {
                logName = logFName;
            } else {
                logName = defaultLogName;
            }
            loggingLocation = tlogLoc;
            logPath = loggingLocation + "/" + logName;
            File f1 = new File(logPath);
            if (f1.exists() && (f1.length() > 0)) {
                f1.delete();
            }

            logger = (Logger)Logger.getLogger(logFName);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), params);
            e.printStackTrace();
            throw e;
        }

        exiting("setup");
    }

    private void setbufferSizer(String statusVal, String buffSize)
        throws SSOException, SMSException {
        SSOToken adminSSOToken = getAdminSSOToken();
        ServiceSchemaManager mgr = new ServiceSchemaManager(
            "iPlanetAMLoggingService", adminSSOToken);
        ServiceSchema globalSchema = mgr.getSchema(SchemaType.GLOBAL);
        {
            AttributeSchema status = globalSchema.getAttributeSchema(
                "iplanet-am-logging-time-buffering-status");
            Set<String> set = new HashSet<String>(2);
            set.add(statusVal);
            status.setDefaultValues(set);
        }
        {
            AttributeSchema bufferSize = globalSchema.getAttributeSchema(
                "iplanet-am-logging-buffer-size");
            Set<String> set = new HashSet<String>(2);
            set.add(buffSize);
            bufferSize.setDefaultValues(set);
        }
    }


    /**
     *  need:
     *    only the amadmin SSOToken (used in LogRecord), since
     *      can't (currently) use user SSOTokens.  also means
     *      the log delegation can't be tested.
     *    column values
     *      data
     *      module_name
     *      domain
     *      log_level
     *      login_id
     *      ip_addr
     *      host_name
     *      message_id
     *    number of records to write
     *
     */

    @Parameters({"logwrite-data",
                 "logwrite-modulename",
                 "logwrite-domain",
                 "logwrite-log-level",
                 "logwrite-login-id",
                 "logwrite-ip-addr",
                 "logwrite-host-name",
                 "logwrite-message-id",
                 "logwrite-number-of-records"})
    @Test(groups = {"api-adminwrite"})
    public void writeAdminLogRecord(
        String rData,
        String rModuleName,
        String rDomain,
        String rLogLevel,
        String rLoginId,
        String rIPAddr,
        String rHostName,
        String rMsgId,
        String rNumRecs
    ) throws AMLogException {
        LogRecord lR = null;
        Level llevel = null;
        int numRecs = 0;

        if ((rNumRecs != null) && (rNumRecs.length() > 0)) {
            try {
                numRecs = Integer.parseInt(rNumRecs);
            } catch (NumberFormatException nfe) {
                log(Level.WARNING, "writeAdminLogRecord",
                    nfe.getMessage());
                numRecs = 1;
            }
        }

        llevel = getLogLevel(rLogLevel);

        /**
         *  DOMAIN, LOGIN_ID, IP_ADDR, and HOST_NAME are extracted from the
         *  SSOToken and added by the LogRecord handling.  if any values are
         *  provided to the test, then they'll be added.
         */

        int totalRecs = 0;

        SSOToken adminToken = getAdminSSOToken();
        /*
         *  put variable data in ("msgDataPrefix + i") reverse
         *  order, so we can test sortBy in the read test.
         */
        for (int i = (numRecs-1); i >= 0; i-- ) {
            lR = new LogRecord(llevel,
                msgDataPrefix + i + "|" + rData, adminToken);
            if ((rDomain != null) && (rDomain.length() > 0)) {
                lR.addLogInfo(LogConstants.DOMAIN, rDomain);
            }
            // ignore rLoginId parameter; use "amAdmin"
            lR.addLogInfo(LogConstants.LOGIN_ID, "amAdmin");
            if ((rIPAddr != null) && (rIPAddr.length() > 0)) {
                lR.addLogInfo(LogConstants.IP_ADDR, rIPAddr);
            }
            if ((rHostName != null) && (rHostName.length() > 0)) {
                lR.addLogInfo(LogConstants.HOST_NAME, rHostName);
            }
            if ((rModuleName != null) && (rModuleName.length() > 0)) {
                lR.addLogInfo(LogConstants.MODULE_NAME, rModuleName);
            }
            if ((rMsgId != null) && (rMsgId.length() > 0)) {
                String msgid = rMsgId + i;
                lR.addLogInfo(LogConstants.MESSAGE_ID, msgid);
            }
            try {
                logger.log(lR, adminToken);
                totalRecs++;
            } catch (AMLogException alex) {
                //  unexpected exception
                log(Level.SEVERE, "writeAdminLogRecord",
                    alex.getMessage());
                throw alex;
            }
        }
    }


    /**
     *  the amadmin read log record test.  test reads as amadmin only.
     *  
     *  test 1: retrieve all records.  *should* be
     *          logwrite-number-of-records
     *          if the logtest-logname exists before the whole run,
     *          then this test will fail (more than expected records
     *          retrieved).
     */
    @Parameters({"logtest-logname",
                 "logwrite-number-of-records"})
    @Test(groups = {"api-adminread"},
          dependsOnMethods = {"writeAdminLogRecord"})
    public void readAdminLogRecord(String rLogName, String rNumRecs)
        throws AMLogException {

        /*
         *  since "regular" user SSOToken stuff doesn't seem to work,
         *  just read as amadmin.
         *
         *  there are a bunch of types of read tests to run...
         *    1.  read all
         *    2.  read selective (of various varieties)
         */

        SSOToken adminToken = getAdminSSOToken();
        int numRecs = 0;

        if ((rNumRecs != null) && (rNumRecs.length() > 0)) {
            try {
                numRecs = Integer.parseInt(rNumRecs);
            } catch (NumberFormatException nfe) {
                numRecs = 1;
            }
        }
        readAllRecords(rLogName, adminToken, numRecs);

        readLogQuery(rLogName, adminToken, numRecs);
    }

    private void readAllRecords(String fileName, SSOToken ssot, int numRecsExp)
        throws AMLogException {
        int numRecs = 0;
        try {
            String[][] result = LogReader.read(fileName, ssot);
            numRecs = Array.getLength(result);
        } catch (AMLogException ale) {
            throw new AMLogException("readAllRecords:AMLogException: " +
                ale.getMessage());
        } catch (NoSuchFieldException nsfe) {
            throw new AMLogException("readAllRecords:NoSuchField: " +
                nsfe.getMessage());
        } catch (IOException ioe) {
            throw new AMLogException("readAllRecords:IOException: " +
                ioe.getMessage());
        } catch (Exception e) {
            throw new AMLogException("readAllRecords:Exception: " +
                e.getMessage());
        }

        // first record has the column names
        if (numRecs != (numRecsExp + 2)) {
            throw new AMLogException("Number of records read (" +
                numRecs + ") doesn't match expected (" +
                (numRecsExp+1) + ")");
        }
    }


    private void readLogQuery(String fileName, SSOToken ssot, int recsWritten)
        throws AMLogException
    {

        /*
         *  can have:
         *    LogQuery(), which sets:
         *      maxRecord = LogQuery.MOST_RECENT_MAX_RECORDS
         *      globalOperand = LogQuery.MATCH_ANY_CONDITION
         *      queries = null
         *      sortBy = null
         *    LogQuery(int max_record), which sets:
         *      maxRecord = max_record
         *      globalOperand = LogQuery.MATCH_ANY_CONDITION
         *      queries = null
         *      sortBy = null
         *    LogQuery(int max_record,
         *             int matchCriteria,
         *             String sortingBy), which sets:
         *      maxRecord = max_record
         *      globalOperand = matchCriteria
         *      sortBy = sortingBy
         *      queries = null
         *      
         *    use lq.addQuery(QueryElement qryElement) to
         *    add query elements to the LoqQuery's list of queries
         *      QueryElement(String fld, String val, int rel)
         *          fieldName = fld
         *          fieldValue = val
         *          relation = rel
         *      QueryElement()
         *          fieldName = new String()
         *          fieldValue = new String()
         *          relation = QueryElement.EQ
         *      use:
         *          QueryElement.setFieldName(String field)
         *          QueryElement.setFieldValue(String value)
         *          QueryElement.setRelation(int value)
         */


        int totalRecsRead = 0;

        /*
         *  test 1:
         *    all records, any condition, no sorting
         *  should be same as read all.
         */

        LogQuery lq = new LogQuery (LogQuery.ALL_RECORDS,
                                    LogQuery.MATCH_ANY_CONDITION,
                                    null);

        int numRecs = 0;
        String[][] result = new String[1][1];
        try {
            result = LogReader.read(fileName, lq, ssot);
            numRecs = Array.getLength(result);
        } catch (AMLogException ale) {
            throw new AMLogException("readLogQuery:AMLogException: " +
                ale.getMessage());
        } catch (NoSuchFieldException nsfe) {
            throw new AMLogException("readLogQuery:NoSuchField: " +
                nsfe.getMessage());
        } catch (IllegalArgumentException iaex) {
            throw new AMLogException("readLogQuery:IllegalArgumentException: "+
                iaex.getMessage());
        } catch (IOException ioe) {
            throw new AMLogException("readLogQuery:IOException: " +
                ioe.getMessage());
        } catch (Exception e) {
            throw new AMLogException("readLogQuery:Exception: " +
                e.getMessage());
        }
        if (numRecs != (recsWritten + 2)) {
            throw new AMLogException("Number of records read test 1 (" +
                numRecs + ") doesn't match expected (" +
                (recsWritten+1) + ")");
        }

        /*
         *  test 2:
         *  only read 2 most recent records
         */

        lq = new LogQuery (2, LogQuery.MATCH_ANY_CONDITION, null);

        result = new String[1][1];
        numRecs = 0;
        try {
            result = LogReader.read(fileName, lq, ssot);
            numRecs = Array.getLength(result);
        } catch (AMLogException ale) {
            throw new AMLogException("readLogQuery:AMLogException: " +
                ale.getMessage());
        } catch (NoSuchFieldException nsfe) {
            throw new AMLogException("readLogQuery:NoSuchField: " +
                nsfe.getMessage());
        } catch (IllegalArgumentException iaex) {
            throw new AMLogException("readLogQuery:IllegalArgumentException: "+
                iaex.getMessage());
        } catch (IOException ioe) {
            throw new AMLogException("readLogQuery:IOException: " +
                ioe.getMessage());
        } catch (Exception e) {
            throw new AMLogException("readLogQuery:Exception: " +
                e.getMessage());
        }
        if (numRecs != 3) {
            throw new AMLogException("Number of records read test 2 (" +
                (numRecs-1) + ") doesn't match expected (2)");
        }

        //  two most recent should be "...data #1" and "...data #0"
        String temp = result[1][1];
        if (!temp.contains(msgDataPrefixData+"1")) {
            throw new AMLogException("Read test 2: second most recent = " +
                temp + ", not " + msgDataPrefixData + "1");
        }
        temp = result[2][1];
        if (!temp.contains(msgDataPrefixData+"0")) {
            throw new AMLogException("Read test 2: most recent = " +
                temp + ", not " + msgDataPrefixData + "0");
        }

        /*
         *  test 3:
         *  get records that end with "XX2" in the MessageID field.
         *  should only be one.
         */

        lq = new LogQuery (LogQuery.ALL_RECORDS,
                                    LogQuery.MATCH_ALL_CONDITIONS,
                                    null);
        QueryElement qe = new QueryElement("MessageID",
                                           "XX2",
                                           QueryElement.EW);
        lq.addQuery(qe);

        result = new String[1][1];
        numRecs = 0;
        try {
            result = LogReader.read(fileName, lq, ssot);
            numRecs = Array.getLength(result);
        } catch (AMLogException ale) {
            throw new AMLogException("readLogQuery:AMLogException: " +
                ale.getMessage());
        } catch (NoSuchFieldException nsfe) {
            throw new AMLogException("readLogQuery:NoSuchField: " +
                nsfe.getMessage());
        } catch (IllegalArgumentException iaex) {
            throw new AMLogException("readLogQuery:IllegalArgumentException: "+
                iaex.getMessage());
        } catch (IOException ioe) {
            throw new AMLogException("readLogQuery:IOException: " +
                ioe.getMessage());
        } catch (Exception e) {
            throw new AMLogException("readLogQuery:Exception: " +
                e.getMessage());
        }
        if (numRecs != 2) {
            throw new AMLogException("Number of records read test 3 (" +
                (numRecs-1) + ") doesn't match expected (1)");
        }

        // assume MessageID is in column 8 (starting from 0)
        temp = result[1][8];
        if (!temp.contains("XX2")) {
            throw new AMLogException("Read test 3: record = " +
                temp + ", not XX2");
        }

        /*
         *  test 4:
         *  get records that contain "data #4", "data #0" or "data #2"
         *  in the Data field.  specify them in that order, with
         *  sort by Data field.
         *  msgDataPrefixData = "data #"
         */

        lq = new LogQuery (LogQuery.ALL_RECORDS,
                                    LogQuery.MATCH_ANY_CONDITION,
                                    "Data");
        qe = new QueryElement("Data", msgDataPrefixData+"4", QueryElement.CN);
        lq.addQuery(qe);
        qe = new QueryElement("Data", msgDataPrefixData+"0", QueryElement.CN);
        lq.addQuery(qe);
        qe = new QueryElement("Data", msgDataPrefixData+"2", QueryElement.CN);
        lq.addQuery(qe);

        result = new String[1][1];
        numRecs = 0;
        try {
            result = LogReader.read(fileName, lq, ssot);
            numRecs = Array.getLength(result);
        } catch (AMLogException ale) {
            throw new AMLogException("readLogQuery:AMLogException: " +
                ale.getMessage());
        } catch (NoSuchFieldException nsfe) {
            throw new AMLogException("readLogQuery:NoSuchField: " +
                nsfe.getMessage());
        } catch (IllegalArgumentException iaex) {
            throw new AMLogException("readLogQuery:IllegalArgumentException: "+
                iaex.getMessage());
        } catch (IOException ioe) {
            throw new AMLogException("readLogQuery:IOException: " +
                ioe.getMessage());
        } catch (Exception e) {
            throw new AMLogException("readLogQuery:Exception: " +
                e.getMessage());
        }
        if (numRecs != 4) {
            throw new AMLogException("Number of records read test 4 (" +
                (numRecs-1) + ") doesn't match expected (3)");
        }
        /*
         *  gonna expect Data column is in result[x][1].  also
         *  that with sorting, result[1][1] contains "...data #0",
         *  result[2][1] contains "...data #2", and result[3][1]
         *  contains "...data #4".
         */
        temp = result[1][1];
        if (!temp.contains(msgDataPrefixData+"0")) {
            throw new AMLogException("Read test 4: first sorted record = " +
                temp + ", not " + msgDataPrefixData + "0");
        }
        temp = result[2][1];
        if (!temp.contains(msgDataPrefixData+"2")) {
            throw new AMLogException("Read test 4: second sorted record = " +
                temp + ", not " + msgDataPrefixData + "2");
        }
        temp = result[3][1];
        if (!temp.contains(msgDataPrefixData+"4")) {
            throw new AMLogException("Read test 4: third sorted record = " +
                temp + ", not " + msgDataPrefixData + "4");
        }
    }


    private void printResults(String [][] results) {

        System.out.println("size of results = " +
            Array.getLength(results));
        int numRecords = Array.getLength(results);
        /*
         *  first record contains column names
         */
        System.out.println ("Row 0 (Column names) =");
        int ii = Array.getLength(results[0]);
        String tempS;
        for (int i = 0; i < ii; i ++) {
            tempS = results[0][i];
            System.out.print ("  " + tempS + "\t");
        }

        for (int i = 1; i < numRecords; i++) {
            System.out.println("\nsize of row " + i + " = " + ii +
                "\nrecord " + i + " =");
            for (int j = 0; j < ii; j++) {
                System.out.println ("  " + results[0][j] +
                    ":\t" + results[i][j]);
            }
            System.out.println("");
        }
        return;
    }


    private Level getLogLevel (String level) {
        // default to INFO
        Level logLevel = java.util.logging.Level.INFO;

        if (level.equalsIgnoreCase("OFF")) {
            logLevel = Level.OFF;
        } else if (level.equalsIgnoreCase("SEVERE")) {
            logLevel = Level.SEVERE;
        } else if (level.equalsIgnoreCase("WARNING")) {
            logLevel = Level.WARNING;
        } else if (level.equalsIgnoreCase("INFO")) {
            logLevel = Level.INFO;
        } else if (level.equalsIgnoreCase("CONFIG")) {
            logLevel = Level.CONFIG;
        } else if (level.equalsIgnoreCase("FINE")) {
            logLevel = Level.FINE;
        } else if (level.equalsIgnoreCase("FINER")) {
            logLevel = Level.FINER;
        } else if (level.equalsIgnoreCase("FINEST")) {
            logLevel = Level.FINEST;
        } else if (level.equalsIgnoreCase("ALL")) {
            logLevel = Level.ALL;
        }
        return logLevel;
    }


    /**
     *  tearDown
     *  undo what setup did
     */
    @Parameters({})
    @AfterTest(groups = {"api-adminwrite", "api-adminread"})
    public void tearDown()
        throws Exception {
        setbufferSizer("ON", "60");
    }

    /**
     *  suiteTearDown
     *  undo what suiteSetup did:
     *   o set the realm's logging service's config back, if necessary
     */
    @Parameters({"logtest-realm"})
    @AfterSuite(groups = {"api-adminwrite", "api-adminread"})
    public void suiteTearDown (String realm) {
    }
}

