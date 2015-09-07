/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.record;


import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.DebugLevel;
import com.sun.identity.shared.debug.file.impl.InvalidDebugConfigurationException;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DefaultDebugRecorderTest extends DebugTestTemplate {
    protected static final String DEBUG_CONFIG_FOR_TEST = "/record/debugconfig.properties";

    private DefaultDebugRecorderForTest recordDebugController;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        initializeProvider(DEBUG_CONFIG_FOR_TEST);
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_LEVEL, DebugLevel.MESSAGE.getName());
        recordDebugController = new DefaultDebugRecorderForTest(new ExecutorServiceFactory(Mockito.mock
                (org.forgerock.util.thread.listener.ShutdownManager.class)));
    }

    @Test
    public void tryOneRecord() throws RecordException, InvalidDebugConfigurationException, IOException {

        int issueID = 1;
        String referenceID = "first_record";
        //message ID, it helps to check which message are printed on logs
        int messageNb = 0;

        //Maps for checking the test results. We store every message ID by category
        Set<String> shouldBeInRootLogName = new HashSet<String>();
        Set<String> shouldBeInIssueIDLogName = new HashSet<String>();

        Set<String> shouldBeNotPrint = new HashSet<String>();

        JsonValue jsonRecordProperties = JsonValueBuilder.toJsonValue(IOUtils.getFileContentFromClassPath(DefaultDebugRecorderTest.class, RecordPropertiesTest.RECORD_DIRECTORY + "oneRecordFirstRecord.json"));

        //Initialize the debugger
        Debug debugTest = Debug.getInstance(logName);
        debugTest.setDebug(DebugLevel.ERROR.toString());

        // try some logs first, that should not be recorded
        shouldBeInRootLogName.add(messageNb + " -");
        debugTest.error(messageNb++ + " - Error not recorded");

        shouldBeNotPrint.add(messageNb + " -");
        debugTest.message(messageNb++ + " - message not recorded");

        //We start recording the issue
        recordDebugController.startRecording(jsonRecordProperties);

        //try some logs that should be recorded
        shouldBeInIssueIDLogName.add(messageNb + " -");
        debugTest.error(messageNb++ + " - Error recorded");
        shouldBeInIssueIDLogName.add(messageNb + " -");
        debugTest.message(messageNb++ + " - message recorded");

        // We stop recording
        recordDebugController.stopRecording();

        // try some logs after, that should not be recorded
        shouldBeInRootLogName.add(messageNb + " -");
        debugTest.error(messageNb++ + " - Error not recorded");

        shouldBeNotPrint.add(messageNb + " -");
        debugTest.message(messageNb++ + " - message not recorded");

        //Check everything is correctly generated
        Assert.assertTrue(checkRecordFolderIsCreated(issueID + ""), "Record folder '" + issueID + "' doesn't exist.");

        //We check now that every messages are in the right log file.
        String issueIDDebuglogFile = RecordConstants.RECORD_FOLDER_NAME + File.separator + issueID + File
                .separator + referenceID + File.separator + RecordConstants.DEBUG_FOLDER_NAME + File
                .separator + logName;

        checkLogMessagesAreInTheRightLogFiles(shouldBeInRootLogName, new String[]{logName}, new
                String[]{issueIDDebuglogFile});
        checkLogMessagesAreInTheRightLogFiles(shouldBeInIssueIDLogName, new String[]{issueIDDebuglogFile}, new
                String[]{logName});
        checkLogMessagesAreInTheRightLogFiles(shouldBeNotPrint, new String[]{}, new String[]{logName,
                issueIDDebuglogFile});

    }

    @Test
    public void tryTwoSuccessiveRecords() throws RecordException, InvalidDebugConfigurationException, IOException {

        //message ID, it helps to check which message are printed on logs
        int messageNb = 0;
        int issueID = 2;
        String referenceID1 = "record_1";
        String referenceID2 = "record_2";

        //Maps for checking the test results. We store every message ID by category
        Set<String> shouldBeInIssueID1LogName = new HashSet<String>();
        Set<String> shouldBeInIssueID2LogName = new HashSet<String>();

        Set<String> shouldBeNotPrint = new HashSet<String>();

        JsonValue jsonRecordProperties1 = JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(DefaultDebugRecorderTest.class, RecordPropertiesTest
                        .RECORD_DIRECTORY + "twoRecordsFirstRecord.json"));
        JsonValue jsonRecordProperties2 = JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(DefaultDebugRecorderTest.class, RecordPropertiesTest
                        .RECORD_DIRECTORY + "twoRecordsSecondRecord.json"));

        //Initialize the debugger
        Debug debugTest = Debug.getInstance(logName);
        debugTest.setDebug(DebugLevel.ERROR.toString());

        //We start recording the issue
        recordDebugController.startRecording(jsonRecordProperties1);

        //try some logs that should be recorded in issueID1

        shouldBeInIssueID1LogName.add(messageNb + " -");
        debugTest.error(messageNb++ + " - Error recorded");
        shouldBeInIssueID1LogName.add(messageNb + " -");
        debugTest.message(messageNb++ + " - message recorded");

        // We start recording issueID2 without stopping issueID1
        recordDebugController.startRecording(jsonRecordProperties2);

        //try some logs that should be recorded in issueID2
        shouldBeInIssueID2LogName.add(messageNb + " -");
        debugTest.error(messageNb++ + " - Error recorded");
        shouldBeInIssueID2LogName.add(messageNb + " -");
        debugTest.warning(messageNb++ + " - message recorded");
        // message level shouldn't not be printed on this record
        shouldBeNotPrint.add(messageNb + " -");
        debugTest.message(messageNb++ + " - message recorded but not printed");

        //We stop recording
        recordDebugController.stopRecording();

        //Check everything is correctly generated
        Assert.assertTrue(checkRecordFolderIsCreated(issueID + ""), "Record folder '" + issueID + "' doesn't exist.");
        Assert.assertTrue(checkRecordFolderIsCreated(issueID + File.separator + referenceID1), "Record folder '" +
                issueID + "/" + referenceID1 + "' doesn't exist.");
        Assert.assertTrue(checkRecordFolderIsCreated(issueID + File.separator + referenceID2), "Record folder '" +
                issueID + "/" + referenceID2 + "' doesn't exist.");

        //We check now that every messages are in the right log file.
        String issueID1DebuglogFile = RecordConstants.RECORD_FOLDER_NAME + File.separator + issueID + File
                .separator + referenceID1 + File.separator + RecordConstants.DEBUG_FOLDER_NAME + File
                .separator + logName;
        String issueID2DebuglogFile = RecordConstants.RECORD_FOLDER_NAME + File.separator + issueID + File
                .separator + referenceID2 + File.separator + RecordConstants.DEBUG_FOLDER_NAME + File
                .separator + logName;

        checkLogMessagesAreInTheRightLogFiles(shouldBeInIssueID1LogName, new String[]{issueID1DebuglogFile}, new
                String[]{issueID2DebuglogFile});
        checkLogMessagesAreInTheRightLogFiles(shouldBeInIssueID2LogName, new String[]{issueID2DebuglogFile}, new
                String[]{issueID1DebuglogFile});
        checkLogMessagesAreInTheRightLogFiles(shouldBeNotPrint, new String[]{}, new String[]{issueID1DebuglogFile,
                issueID2DebuglogFile});
    }

    @Test
    public void tryAutoStopFileSize() throws RecordException, InvalidDebugConfigurationException, IOException,
            InterruptedException {

        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_LEVEL, DebugLevel.MESSAGE.getName());

        JsonValue jsonRecordProperties = JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(DefaultDebugRecorderTest.class, RecordPropertiesTest
                        .RECORD_DIRECTORY + "autoStopFileSize.json"));

        Debug debugTest = Debug.getInstance(logName);
        debugTest.setDebug(DebugLevel.ERROR.toString());

        recordDebugController.startRecording(jsonRecordProperties);

        int debugDirectoryTheoricMinSize = 0;
        while (debugDirectoryTheoricMinSize < 2048) {
            String message = "Forgerock!";
            debugTest.message(message);
            debugDirectoryTheoricMinSize += message.getBytes("UTF-8").length;
        }

        // lets our thread the time to detect the file size limit
        recordDebugController.getAutoStopFileSizeRecord().run();
        Record record = recordDebugController.getCurrentRecord();
        if(record != null) {
            Assert.fail("Record wasn't stop by the autostop thread.");
        }

    }

    @Test
    public void shouldCreateThreadDumpFiles() throws RecordException, InvalidDebugConfigurationException, IOException,
            InterruptedException {

        JsonValue jsonRecordProperties = JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(DefaultDebugRecorderTest.class,
                        RecordPropertiesTest.RECORD_DIRECTORY + "threadDump.json"));

        recordDebugController.startRecording(jsonRecordProperties);
        Record currentRecord = recordDebugController.getCurrentRecord();
        recordDebugController.getThreadsDumpRunnable().run();
        recordDebugController.stopRecording();

        String threadDumpFolder = currentRecord.getFolderPath() + File.separator +
                RecordConstants.THREAD_DUMP_FOLDER_NAME + File.separator;

        File[] threadDumpFiles = new File(threadDumpFolder).listFiles();

        Assert.assertTrue(threadDumpFiles != null && threadDumpFiles.length > 0, "No thread dump file created.");
    }

    @Test(enabled = false)
    public void tryZip() throws RecordException, InvalidDebugConfigurationException, IOException,
            InterruptedException {

        int issueID = 9;
        String referenceID = "test_zip";
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_LEVEL, DebugLevel.MESSAGE.getName());

        JsonValue jsonRecordProperties = JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(DefaultDebugRecorderTest.class, RecordPropertiesTest
                        .RECORD_DIRECTORY + "ZipRecord.json"));


        recordDebugController.startRecording(jsonRecordProperties);
        recordDebugController.stopRecording();

        String issueIDFolder = debugDirectory + File.separator + RecordConstants.RECORD_FOLDER_NAME +
                File.separator + issueID + File.separator;
        File[] issueIDFiles = new File(issueIDFolder).listFiles();

        if(issueIDFiles == null) {
            Assert.fail("No sub file in '" + issueIDFolder + "' folder.");
        }

        boolean isZipGenerated = false;
        for(File file: issueIDFiles) {
            if(file.getName().contains(referenceID)) {
                if(file.getName().equals(referenceID)) {
                    Assert.fail("Folder '" + referenceID + "' should be deleted.");
                }
                isZipGenerated = true;
            }
        }
        if(!isZipGenerated) {
            Assert.fail("Zip file '" + referenceID + "' was not generated.");
        }
    }

    /**
     * Check if a record folder exist
     *
     * @param issueID
     * @return
     */
    private boolean checkRecordFolderIsCreated(String issueID) {
        return isDirectoryExist(RecordConstants.RECORD_FOLDER_NAME + File.separator + issueID);
    }

    /**
     * Check if a LogMessage is contains in a log file
     *
     * @param fileName        the log file path
     * @param patternToSearch a part of the message that you want to see in the log
     * @return
     * @throws IOException
     */
    private boolean checkLogMessageExist(String fileName, String patternToSearch) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(debugDirectory + File.separator + fileName));
        String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null) {
            if (sCurrentLine.contains(patternToSearch)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generic test that assert if the test failed.
     * We check that a set of messages are on the right set of logs
     *
     * @param messages                           a set of messages
     * @param filesListContainsTheMessages       list of files where the messages should be in
     * @param filesListDoesntContainsTheMessages list of files where the message should not be in
     * @throws IOException
     */
    private void checkLogMessagesAreInTheRightLogFiles(Set<String> messages, String[] filesListContainsTheMessages,
            String[] filesListDoesntContainsTheMessages) throws IOException {

        for (String message : messages) {

            //Should contains the message
            for (String file : filesListContainsTheMessages) {
                Assert.assertTrue(checkLogMessageExist(file, message), "message starting with '" + message + "' " +
                        "should be in the file '" + file + "'");
            }

            //Should NOT contains the message
            for (String file : filesListDoesntContainsTheMessages) {
                Assert.assertFalse(checkLogMessageExist(file, message), "message starting with '" + message + "' " +
                        "should not be in the file '" + file + "'");
            }
        }
    }

    public static class DefaultDebugRecorderForTest extends DefaultDebugRecorder {

        /**
         * Initialize the RecordDebugController.
         *
         * @param executorServiceFactory
         */
        public DefaultDebugRecorderForTest(ExecutorServiceFactory executorServiceFactory) {
            super(executorServiceFactory);
        }

        public ThreadsDumpRunnable getThreadsDumpRunnable() {
            return new ThreadsDumpRunnable(getCurrentRecord());
        }

        public AutoStopFileSizeRecord getAutoStopFileSizeRecord() {
            return new AutoStopFileSizeRecord(getCurrentRecord());
        }
    }

}
