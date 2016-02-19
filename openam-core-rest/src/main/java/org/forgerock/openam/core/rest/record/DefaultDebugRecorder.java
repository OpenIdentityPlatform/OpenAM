/*
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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package org.forgerock.openam.core.rest.record;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.services.util.ConfigurableKey;
import com.iplanet.services.util.JCEEncryption;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.sm.ServiceManager;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.file.ZipUtils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manage the records.
 *
 * Implement 3 actions:
 * Start: Start recording OpenAM.
 * Status: Get the status of the current recording
 * Stop: Stop the current recording if exist.
 *
 * Note that you can't have two records at the same time, even if it is not running.
 */
@Singleton
public class DefaultDebugRecorder implements DebugRecorder {

    private static final String DATE_FORMAT_UID = "yyyy-MM-dd-hh-mm-ss-SSS-zzz";

    private static final int AUTOSTOP_FILE_SIZE_CHECK_PERIOD_IN_MS = 200;

    private final Debug debug = Debug.getInstance(RecordConstants.DEBUG_INSTANCE_NAME);
    private final ScheduledExecutorService scheduledExecutorService;

    private String previousDebugDirectory;

    //record under recording. If null, it means that we are not recording an issue.
    private volatile Record currentRecord;

    //Threads
    private ScheduledFuture<?> currentScheduledThreadDump;
    private ScheduledFuture<?> currentScheduledAutoStopFileSize;
    private ScheduledFuture<?> currentScheduledAutoStopTime;

    // The previous debug levels
    private Map<Debug, Integer> previousDebugLevel;

    private RecordReport recordReport;

    /**
     * Initialize the RecordDebugController.
     */
    @Inject
    public DefaultDebugRecorder(ExecutorServiceFactory executorServiceFactory) {
        scheduledExecutorService = executorServiceFactory.createScheduledService(2);
        recordReport = new RecordReport();
    }

    /**
     * Start recording an issue
     *
     * @param jsonProperties
     * @throws RecordException throw a RecordException if the issueID doesn't exist
     */
    public synchronized void startRecording(JsonValue jsonProperties) throws RecordException {

        RecordProperties recordProperties = RecordProperties.fromJson(jsonProperties);
        Record record = createRecord(recordProperties);

        if (isRecording()) {
            debug.message("Issue '{}' was recording, we stop it.", currentRecord);
            stopRecording();
        }

        previousDebugDirectory = SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_DIRECTORY);

        debug.message("Start recording issue '{}'.", record);

        //we switch the OpenAM debug folder with the issue folder
        changeOpenAMDebugFolder(record.getFolderPath() + File.separator + RecordConstants.DEBUG_FOLDER_NAME);

        currentRecord = record;
        currentRecord.startRecord();

        //We will switch the debug level with the one required for the issue
        previousDebugLevel = new HashMap<Debug, Integer>();
        List<Debug> debugInstances = new ArrayList<Debug>(Debug.getInstances());
        for (Debug debug : debugInstances) {
            previousDebugLevel.put(debug, debug.getState());
            debug.setDebug(record.getRecordProperties().getDebugLevel().getName());
        }

        //Export Config export
        exportConfigExport();

        //We start the different threads
        try {
            startThreadDump();
        } catch (IOException e) {
            debug.error("Thread dump can't be enable", e);
            throw new RecordException("Thread dump can't be enable", e);
        }
        startAutoStopRecording();
    }

    @Override
    public Record getCurrentRecord() {
        return currentRecord;
    }

    /**
     * Stop recording the current issue.
     */
    public synchronized Record stopRecording() throws RecordException {

        if (currentRecord == null) {
            debug.message("Ask for stopping the record but we are not recording.");
            return null;
        }

        debug.message("Stop recording Issue '{}'.", currentRecord);

        //Stop different threads
        stopThreadDump();
        stopAutoStopRecording();

        currentRecord.stopRecord();

        //Change back to the default debug logs folder
        changeOpenAMDebugFolder(previousDebugDirectory);

        //Set back the debug level
        for (Map.Entry<Debug, Integer> entry : previousDebugLevel.entrySet()) {
            entry.getKey().setDebug(entry.getValue());
        }
        previousDebugLevel = new HashMap<Debug, Integer>();

        //We archive the record
        try {
            archiveRecord(currentRecord);
        } catch (RecordException e) {
            // Not archiving the record is an important failure, but not a blocking one.
            debug.warning("Can't archive the issue '{}'", currentRecord, e);
        }
        Record stoppedRecord = currentRecord;
        currentRecord = null;
        return stoppedRecord;
    }

    /**
     * Create a new RecordDebug
     *
     * @param recordProperties record properties
     * @throws RecordException throw a RecordException if the issueID already exist or if the info.json can't be
     *                         initialized
     */
    private Record createRecord(RecordProperties recordProperties) throws RecordException {

        String debugDirectory;
        if (isRecording()) {
            debugDirectory = previousDebugDirectory;
        } else {
            debugDirectory = SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_DIRECTORY);
        }

        String recordDirectory = debugDirectory + File.separator + RecordConstants.RECORD_FOLDER_NAME + File.separator +
                recordProperties.getIssueID() + File.separator + recordProperties.getReferenceID();

        //Create the record
        Record record = new Record(recordProperties, recordDirectory);

        try {
            // We create the temporary folder where we will save all the logging file.
            createFolder(record.getFolderPath());

            PrintWriter printWriterInfo = getPrintWriterForFile(record, RecordConstants.INFO_FILE_NAME);
            JsonValue info = recordReport.infoReport(record);
            try {
                JSONObject json = new JSONObject(info.toString()); // convert it to JSON object
                printWriterInfo.println(json.toString(4));
            } catch (JSONException e) {
                debug.warning("Can't indent json '{}'", info, e);
                printWriterInfo.println(info);
            }
            printWriterInfo.flush();
            return record;

        } catch (IOException e) {
            debug.error("Info report can't be initialized for issue '{}'", recordProperties, e);
            throw new RecordException("Info report can't be initialized for issue '" + recordProperties.getIssueID()
                    + "'", e);
        }
    }

    /**
     * Check if we are recording
     *
     * @return
     */
    private boolean isRecording() {
        return currentRecord != null;
    }

    /**
     * Start the thread in charge of the thread dump
     *
     * @throws IOException
     */
    private void startThreadDump() throws IOException {

        if (currentRecord.getRecordProperties().isThreadDumpEnabled()) {
            createFolder(currentRecord.getFolderPath() + File.separator + RecordConstants.THREAD_DUMP_FOLDER_NAME);
            currentScheduledThreadDump = scheduledExecutorService.scheduleWithFixedDelay(new ThreadsDumpRunnable
                    (currentRecord), 0, currentRecord.getRecordProperties().getThreadDumpDelayInSeconds(), TimeUnit
                    .SECONDS);
        }
    }

    /**
     * Stop the thread in charge of the thread dump
     */
    private void stopThreadDump() {
        if (currentRecord.getRecordProperties().isThreadDumpEnabled()) {
            currentScheduledThreadDump.cancel(false);
        }
    }

    /**
     * Export the OpenAM config export
     */
    private void exportConfigExport() {
        if (currentRecord.getRecordProperties().isConfigExportEnabled()) {
            SSOToken adminSSOToken = AccessController.doPrivileged(AdminTokenAction.getInstance());

            try {
                ServiceManager sm = new ServiceManager(adminSSOToken);
                AMEncryption encryptObj = new JCEEncryption();
                ((ConfigurableKey) encryptObj).setPassword(currentRecord.getRecordProperties()
                        .getConfigExportPassword());

                String resultXML = sm.toXML(encryptObj);
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_UID);

                String xmlName = RecordConstants.OPENAM_CONFIG_EXPORT_FILE_NAME.replace("$DATE$", dateFormat.format
                        (newDate()));

                File file = new File(currentRecord.getFolderPath() + File.separator + xmlName);
                PrintWriter printWriter = new PrintWriter(new FileWriter(file, false), true);
                printWriter.println(resultXML);
                printWriter.flush();
            } catch (Exception e) {
                debug.error("Can't export OpenAM configuration", e);
            }
        }
    }

    /**
     * Start threads in charge of stopping the recording
     */
    private void startAutoStopRecording() {
        if (currentRecord.getRecordProperties().isAutoStopEnabled()) {

            if (currentRecord.getRecordProperties().isAutoStopTimeEnabled()) {
                currentScheduledAutoStopTime = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                           @Override
                           public void run() {
                               try {
                                   stopRecording();
                               } catch (RecordException e) {
                                   debug.error("Can't stop recording", e);
                               } catch (Exception e) {
                                   debug.error("Thread 'AutoStopTime' throws an unexpected error", e);
                               }
                           }
                       }, currentRecord.getRecordProperties().getAutoStopTimeInMS(), currentRecord.getRecordProperties()
                        .getAutoStopTimeInMS(), TimeUnit.MILLISECONDS);
            }

            if (currentRecord.getRecordProperties().isAutoStopFileSizeEnabled()) {
                currentScheduledAutoStopFileSize = scheduledExecutorService.scheduleWithFixedDelay(new
                        AutoStopFileSizeRecord(currentRecord), AUTOSTOP_FILE_SIZE_CHECK_PERIOD_IN_MS,
                        AUTOSTOP_FILE_SIZE_CHECK_PERIOD_IN_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Stop threads in charge of stopping the recording
     */
    private void stopAutoStopRecording() {
        if (currentRecord.getRecordProperties().isAutoStopEnabled()) {
            if (currentRecord.getRecordProperties().isAutoStopTimeEnabled()) {
                currentScheduledAutoStopTime.cancel(false);
            }
            if (currentRecord.getRecordProperties().isAutoStopFileSizeEnabled()) {
                currentScheduledAutoStopFileSize.cancel(false);
            }
        }
    }

    /**
     * Delete the record
     *
     * @param recordToDelete
     * @throws RecordException throw a RecordException if we can't remove the issue folder
     */
    private void deleteRecord(Record recordToDelete) throws RecordException {

        if (recordToDelete == currentRecord) {
            debug.message("We are recording this issue, so we stop recording it first.");
            stopRecording();
        }

        // We delete the issue folder
        try {
            delete(recordToDelete.getFolderPath());
        } catch (IOException e) {
            debug.error("Issue '{}' can't be delete due to an IO issue.", recordToDelete, e);
            throw new RecordException("Issue '" + recordToDelete.getRecordProperties().getIssueID() + "' can't be " +
                    "delete due to an IO issue.", e);
        }
    }

    /**
     * Change the debug logs folder
     *
     * @param newOpenAMDebugFolder the new destination folder for the logs
     */
    private void changeOpenAMDebugFolder(String newOpenAMDebugFolder) {

        //we switch the OpenAM debug folder with the issue folder
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_DIRECTORY, newOpenAMDebugFolder);
    }

    /**
     * Archive a record
     *
     * @param record record to be archived
     * @throws RecordException Throw an RecordException if there is an issue during the archive
     */
    private void archiveRecord(Record record) throws RecordException {

        try {
            PrintWriter printWriter = getPrintWriterForFile(record, RecordConstants.HISTORY_FILE_NAME);
            printWriter.println("********* OPENAM RECORD **********");
            printWriter.println(recordReport.recordHistoryReport(record));
            printWriter.println("**********************************");
            printWriter.flush();
        } catch (IOException e) {
            debug.warning("Issue '{}' can't be archived due to an IO issue.", record, e);
            throw new RecordException("Issue '" + record + "' can't be archived due to an IO issue.", e);
        } catch (Exception e) {
            debug.warning("Record '{}' can't be archived", record.getFolderPath(), e);
        }

        if (record.getRecordProperties().isZipEnabled()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_UID);
            String zipArchiveName = record.getFolderPath() + "_" + dateFormat.format(newDate()) + ".zip";
            try {
                ZipUtils.generateZip(record.getFolderPath(), zipArchiveName);
                delete(record.getFolderPath());
            } catch (IOException e) {
                debug.warning("Issue '{}' can't be zipped due to an IO issue.", record, e);
                try {
                    delete(zipArchiveName);
                } catch (IOException e1) {
                    debug.warning("Zip file '{}' can't be delete.", zipArchiveName, e);
                }
                throw new RecordException("Issue '" + record + "' can't be zipped due to an IO issue.", e);
            } catch (URISyntaxException e) {
                debug.warning("Record '{}' can't be deleted", record.getFolderPath(), e);
            }
        }
    }

    /**
     * Get the file print writer for the record
     *
     * @param record   the record debug
     * @param fileName the file name to initialize
     * @return the print writer
     * @throws IOException
     */
    private PrintWriter getPrintWriterForFile(Record record, String fileName) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(Paths.get(record.getFolderPath(), fileName), StandardCharsets
                .UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
    }

    /**
     * Create the record directory
     *
     * @param debugDirectory the record directory
     * @return true if it succeed to create this folder
     */
    private boolean createFolder(String debugDirectory) {
        File dir = new File(debugDirectory);
        if (dir.exists()) {
            Date previousRecordDate;
            try {
                JsonValue infoJson = JsonValueBuilder.toJsonValue(IOUtils.getFileContent(debugDirectory + File
                        .separator + RecordConstants.INFO_FILE_NAME));

                previousRecordDate = recordReport.getDateFromInfoReport(infoJson);
            } catch (IOException | ParseException | JsonValueException e) {
                debug.error("Can't extract starting date from previous record. We will use the current date instead",
                        e);
                previousRecordDate = newDate();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_UID);
            dir.renameTo(new File(debugDirectory + "_" + dateFormat.format(previousRecordDate)));
        }
        return dir.mkdirs();
    }


    /**
     * Delete a file
     *
     * @param file to delete
     * @throws IOException
     */
    private void delete(String file) throws IOException {

        Path start = Paths.get(file);
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }
        });
    }

    @VisibleForTesting
    protected class ThreadsDumpRunnable implements Runnable {

        private Record currentRecord;

        private boolean isIOExFirstTime = true;

        public ThreadsDumpRunnable(Record currentRecord) {
            this.currentRecord = currentRecord;
        }

        @Override
        public void run() {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_UID);
                String threadDumpFileName = RecordConstants.THREAD_DUMP_FOLDER_NAME + File.separator +
                        RecordConstants.TREADS_DUMP_FILE_NAME.replace("$DATE$", dateFormat.format(newDate()));

                try {
                    PrintWriter printWriterThreadsDump = getPrintWriterForFile(currentRecord, threadDumpFileName);

                    printWriterThreadsDump.println("***********************************");
                    printWriterThreadsDump.println(recordReport.getThreadDump());
                    printWriterThreadsDump.println("***********************************");
                    printWriterThreadsDump.flush();

                } catch (IOException e) {
                    if (isIOExFirstTime) {
                        debug.error("Can't print threads dump for record '{}'", currentRecord, e);
                    } else {
                        debug.warning("Can't print threads dump for record '{}'", currentRecord);
                        isIOExFirstTime = false;
                    }
                }
            } catch (Exception e) {
                debug.error("Thread '{}' throws an unexpected error", this.getClass().getName(), e);
            }
        }
    }

    @VisibleForTesting
    protected class AutoStopFileSizeRecord implements Runnable {

        private Record currentRecord;

        public AutoStopFileSizeRecord(Record currentRecord) {
            this.currentRecord = currentRecord;
        }

        @Override
        public void run() {

            try {
                if (!currentRecord.getRecordProperties().isAutoStopFileSizeEnabled()) {
                    debug.error("Should never happen. '{}' should not be running is auto stop file size is not enable.",
                            this.getClass().getSimpleName());

                    throw new RuntimeException("Should never happen. '" + this.getClass().getSimpleName() +
                            "' should not be running is auto stop file size is not enable.");
                }

                if (DefaultDebugRecorder.this.currentRecord != currentRecord) {
                    debug.error("We are not recording '{}' anymore. This thread should have been killed before",
                            currentRecord);
                    return;
                }

                String path = currentRecord.getFolderPath() + File.separator + RecordConstants.DEBUG_FOLDER_NAME;
                long size = getFileSize(new File(path));
                if (size > currentRecord.getRecordProperties().getAutoStopFileSizeInKB() * 1024) {
                    try {
                        stopRecording();
                    } catch (RecordException e) {
                        debug.error("Can't stop recording", e);
                    }
                } else {
                    debug.message("Size of the {} directory: {} octets. Size limit={} KB", path, size, currentRecord
                            .getRecordProperties().getAutoStopFileSizeInKB());
                }
            } catch (Exception e) {
                debug.error("Thread '{}' throws an unexpected error", this.getClass().getName(), e);
            }
        }

        private long getFileSize(File folder) {
            long folderSize = 0;
            File[] fileList = folder.listFiles();
            for (int i = 0;
                    i < fileList.length;
                    i++) {
                if (fileList[i].isDirectory()) {
                    folderSize += getFileSize(fileList[i]);
                } else {
                    folderSize += fileList[i].length();
                }
            }
            return folderSize;
        }
    }
}
