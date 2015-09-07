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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.record;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.debug.DebugLevel;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.file.FileSizeUnit;

/**
 * Properties of a record, imported from a json content
 */
public class RecordProperties {

    //Validate reference ID
    private static final String REFERENCE_ID_REGEX = "^[_A-Za-z0-9-_]+$";
    private static final Pattern REFERENCE_ID_PATTERN = Pattern.compile(REFERENCE_ID_REGEX);

    // Replace password by
    private static final String NOT_SHARED_PASSWORD = "xxxxxx";

    private static final Debug debug = Debug.getInstance(RecordConstants.DEBUG_INSTANCE_NAME);


    // global Properties
    private long issueID;
    private String referenceID;
    private String description;
    private boolean zipEnable;

    //threads dumps properties
    private boolean threadDumpEnable;
    private long threadDumpDelayInSeconds;

    // Config properties
    private boolean configExportEnable;
    private String configExportPassword;
    private boolean configExportSharePassword;

    // Debug logs properties
    private DebugLevel debugLevel;

    private boolean autoStopEnable;

    private boolean autoStopTimeEnable;
    private long autoStopTimeInMS;

    private boolean autoStopFileSizeEnable;
    private long autoStopFileSizedInKB;

    /**
     * Can only be instantiated from a json value
     */
    private RecordProperties() {
    }

    /**
     * Create a record properties from a json content
     *
     * @param jsonProperties a json value
     * @return a record properties
     * @throws IllegalArgumentException if there is an invalid or missing json property
     */
    public static RecordProperties fromJson(JsonValue jsonProperties) {

        RecordProperties recordProperties = new RecordProperties();

        //Global values
        recordProperties.issueID = jsonProperties.get(RecordConstants.ISSUE_ID_LABEL).required().asLong();
        recordProperties.referenceID = jsonProperties.get(RecordConstants.REFERENCE_ID_LABEL).required().asString();
        if (!REFERENCE_ID_PATTERN.matcher(recordProperties.referenceID).matches()) {
            debug.message("{} format is incorrect. Format expected: {}. Value: '{}'",
                    RecordConstants.REFERENCE_ID_LABEL, REFERENCE_ID_REGEX, recordProperties.referenceID);
            throw new IllegalArgumentException(RecordConstants.REFERENCE_ID_LABEL +
                    " format is incorrect. Format expected: " + REFERENCE_ID_REGEX + " . Value: '"
                    + recordProperties.referenceID + "'");
        }
        recordProperties.description = jsonProperties.get(RecordConstants.DESCRIPTION_LABEL).required().asString();

        // Sub json
        fromThreadDumpJson(recordProperties, jsonProperties);
        fromConfigExportJson(recordProperties, jsonProperties);
        fromDebugLogsJson(recordProperties, jsonProperties);

        recordProperties.zipEnable = jsonProperties.get(RecordConstants.ZIP_ENABLE_LABEL).required().asBoolean();

        return recordProperties;
    }


    /**
     * Import the thead dump json content
     *
     * @param recordProperties the result record properties modify by board effect.
     * @param jsonProperties   the sub json content about thread dump
     */
    private static void fromThreadDumpJson(RecordProperties recordProperties, JsonValue jsonProperties) {

        JsonValue jsonThreadDump = jsonProperties.get(RecordConstants.THREAD_DUMP_LABEL).required();
        recordProperties.threadDumpEnable = jsonThreadDump.get(RecordConstants.THREAD_DUMP_ENABLE_LABEL).required()
                .asBoolean();

        // Delay block
        if (recordProperties.threadDumpEnable) {
            JsonValue jsonThreadDumpDelay = jsonThreadDump.get(RecordConstants.THREAD_DUMP_DELAY_LABEL).required();
            TimeUnit timeUnit = jsonThreadDumpDelay.get(RecordConstants.THREAD_DUMP_DELAY_TIME_UNIT_LABEL).required()
                    .asEnum(TimeUnit.class);

            Long timeValue = jsonThreadDumpDelay.get(RecordConstants.THREAD_DUMP_DELAY_VALUE_LABEL).required().asLong();
            recordProperties.threadDumpDelayInSeconds = timeUnit.toSeconds(timeValue);
            if (recordProperties.threadDumpDelayInSeconds < 1) {
                debug.message("For performance reason, {} can't be under the second.",
                        RecordConstants.THREAD_DUMP_DELAY_TIME_UNIT_LABEL);
                throw new IllegalArgumentException("For performance reason, "
                        + RecordConstants.THREAD_DUMP_DELAY_TIME_UNIT_LABEL + " can't be under the second.");
            }
        } else if (jsonThreadDump.isDefined(RecordConstants.THREAD_DUMP_DELAY_LABEL)) {
            debug.message("{} is disabled but {} is defined.", RecordConstants.THREAD_DUMP_ENABLE_LABEL,
                    RecordConstants.THREAD_DUMP_DELAY_LABEL);
            throw new IllegalArgumentException(RecordConstants.THREAD_DUMP_ENABLE_LABEL + " is disabled but " +
                    RecordConstants.THREAD_DUMP_DELAY_LABEL + " is defined.");
        }
    }

    /**
     * Import the config export json content
     *
     * @param recordProperties the result record properties modify by board effect.
     * @param jsonProperties   the sub json content about the config export
     */
    private static void fromConfigExportJson(RecordProperties recordProperties, JsonValue jsonProperties) {

        JsonValue jsonConfigExport = jsonProperties.get(RecordConstants.CONFIG_EXPORT_LABEL).required();
        recordProperties.configExportEnable =
                jsonConfigExport.get(RecordConstants.CONFIG_EXPORT_ENABLE_LABEL).required().asBoolean();
        if (recordProperties.configExportEnable) {
            recordProperties.configExportPassword =
                    jsonConfigExport.get(RecordConstants.CONFIG_EXPORT_PASSWORD_LABEL).required()
                    .asString();
            recordProperties.configExportSharePassword =
                    jsonConfigExport.get(RecordConstants.CONFIG_EXPORT_SHARE_PASSWORD_LABEL)
                    .required().asBoolean();
        } else if (jsonConfigExport.isDefined(RecordConstants.CONFIG_EXPORT_PASSWORD_LABEL)) {
            debug.message("{} is disabled but {} is defined.", RecordConstants.CONFIG_EXPORT_ENABLE_LABEL,
                    RecordConstants.CONFIG_EXPORT_PASSWORD_LABEL);
            throw new IllegalArgumentException(RecordConstants.CONFIG_EXPORT_ENABLE_LABEL + " is disabled but " +
                    RecordConstants.CONFIG_EXPORT_PASSWORD_LABEL + " is defined.");
        }
    }


    /**
     * Import the debug logs json content
     *
     * @param recordProperties the result record properties modify by board effect.
     * @param jsonProperties   the sub json content about debug logs
     */
    private static void fromDebugLogsJson(RecordProperties recordProperties, JsonValue jsonProperties) {

        JsonValue jsonDebugLogs = checkIfExist(jsonProperties, RecordConstants.DEBUG_LOGS_LABEL);

        recordProperties.debugLevel = jsonDebugLogs.get(RecordConstants.DEBUG_LOGS_DEBUG_LEVEL_LABEL).required()
                .asEnum(DebugLevel.class);

        // Auto stop block
        recordProperties.autoStopEnable = jsonDebugLogs.isDefined(RecordConstants.DEBUG_LOGS_AUTOSTOP_LABEL);

        if (recordProperties.autoStopEnable) {
            JsonValue jsonAutoStop = jsonDebugLogs.get(RecordConstants.DEBUG_LOGS_AUTOSTOP_LABEL).required();

            //Time block
            recordProperties.autoStopTimeEnable =
                    jsonAutoStop.isDefined(RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_LABEL);
            if (recordProperties.autoStopTimeEnable) {
                JsonValue jsonAutoStopTime = jsonAutoStop.get(RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_LABEL)
                        .required();
                TimeUnit timeUnit = jsonAutoStopTime.get(RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_UNIT_LABEL).required()
                        .asEnum(TimeUnit.class);

                Long timeValue = jsonAutoStopTime.get(RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_VALUE_LABEL).required()
                        .asLong();
                recordProperties.autoStopTimeInMS = timeUnit.toMillis(timeValue);
            }

            // File Size block
            recordProperties.autoStopFileSizeEnable =
                    jsonAutoStop.isDefined(RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_LABEL);
            if (recordProperties.autoStopFileSizeEnable) {
                JsonValue jsonAutoStopFileSize = jsonAutoStop.get(RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_LABEL)
                        .required();
                FileSizeUnit sizeUnit =
                        jsonAutoStopFileSize.get(RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_SIZEUNIT_LABEL)
                        .required().asEnum(FileSizeUnit.class);
                recordProperties.autoStopFileSizedInKB =
                        sizeUnit.toKB(jsonAutoStopFileSize.get(RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_VALUE_LABEL)
                                .required().asLong());
            }

            // Check if at least one rule is defined
            if (!recordProperties.autoStopFileSizeEnable && !recordProperties.autoStopTimeEnable) {
                debug.message("{} should have a {} and/or a {} rule(s).", RecordConstants.DEBUG_LOGS_AUTOSTOP_LABEL,
                        RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_LABEL,
                        RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_LABEL);
                throw new IllegalArgumentException(RecordConstants.DEBUG_LOGS_AUTOSTOP_LABEL + " should have an " +
                        RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_LABEL + " and/or a " +
                        RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_LABEL + " rule(s).");
            }
        }
    }

    /**
     * Check if a json attribute exist and return the value
     *
     * @param jsonValue
     * @param jsonLabel
     * @return the json value if exist
     * @throws IllegalArgumentException if the json attribute doesn't exist
     */
    private static JsonValue checkIfExist(JsonValue jsonValue, String jsonLabel) {
        try {
            return jsonValue.get(jsonLabel).required();
        } catch (JsonValueException e) {
            debug.message("{} doesn't exist in {}.", jsonLabel, jsonValue);
            throw new IllegalArgumentException(jsonLabel + " doesn't exist in " + jsonValue + ".");
        }
    }

    /**
     * Export a record properties into json
     *
     * @param recordProperties
     * @return json representing the record properties
     */
    public static JsonValue toJson(RecordProperties recordProperties) {

        JsonObject jsonProperties = JsonValueBuilder.jsonValue();

        // Global
        jsonProperties.put(RecordConstants.ISSUE_ID_LABEL, recordProperties.issueID);
        jsonProperties.put(RecordConstants.REFERENCE_ID_LABEL, recordProperties.referenceID);
        jsonProperties.put(RecordConstants.DESCRIPTION_LABEL, recordProperties.description);
        jsonProperties.put(RecordConstants.ZIP_ENABLE_LABEL, recordProperties.zipEnable);

        // Sub json
        jsonProperties.put(RecordConstants.THREAD_DUMP_LABEL, toThreadDumpJson(recordProperties).asMap());
        jsonProperties.put(RecordConstants.CONFIG_EXPORT_LABEL, toConfigExportJson(recordProperties).asMap());
        jsonProperties.put(RecordConstants.DEBUG_LOGS_LABEL, toDebugLogsJson(recordProperties).asMap());

        return jsonProperties.build();
    }

    /**
     * Export the thread dump json block
     *
     * @param recordProperties
     * @return the thread dump json block
     */
    private static JsonValue toThreadDumpJson(RecordProperties recordProperties) {

        JsonObject threadsDumpsProperties = JsonValueBuilder.jsonValue();

        threadsDumpsProperties.put(RecordConstants.THREAD_DUMP_ENABLE_LABEL, recordProperties.threadDumpEnable);
        if (recordProperties.threadDumpEnable) {
            JsonObject threadsDumpsDelayProperties = JsonValueBuilder.jsonValue();
            threadsDumpsDelayProperties.put(RecordConstants.THREAD_DUMP_DELAY_TIME_UNIT_LABEL,
                    TimeUnit.SECONDS.toString());
            threadsDumpsDelayProperties.put(RecordConstants.THREAD_DUMP_DELAY_VALUE_LABEL,
                    recordProperties.threadDumpDelayInSeconds);
            threadsDumpsProperties.put(RecordConstants.THREAD_DUMP_DELAY_LABEL,
                    threadsDumpsDelayProperties.build().asMap());
        }
        return threadsDumpsProperties.build();
    }

    /**
     * Export the config export json block
     *
     * @param recordProperties
     * @return the config export json block
     */
    private static JsonValue toConfigExportJson(RecordProperties recordProperties) {
        JsonObject configExportProperties = JsonValueBuilder.jsonValue();

        configExportProperties.put(RecordConstants.CONFIG_EXPORT_ENABLE_LABEL, recordProperties.configExportEnable);

        if (recordProperties.configExportEnable) {
            if (recordProperties.configExportSharePassword) {
                configExportProperties.put(RecordConstants.CONFIG_EXPORT_PASSWORD_LABEL,
                        recordProperties.configExportPassword);
            } else {
                configExportProperties.put(RecordConstants.CONFIG_EXPORT_PASSWORD_LABEL, NOT_SHARED_PASSWORD);
            }
            configExportProperties.put(RecordConstants.CONFIG_EXPORT_SHARE_PASSWORD_LABEL,
                    recordProperties.configExportSharePassword);
        }
        return configExportProperties.build();
    }

    /**
     * Export the debug logs json block
     *
     * @param recordProperties
     * @return the debug logs json block
     */
    private static JsonValue toDebugLogsJson(RecordProperties recordProperties) {
        JsonObject debugLogsProperties = JsonValueBuilder.jsonValue();

        debugLogsProperties.put(RecordConstants.DEBUG_LOGS_DEBUG_LEVEL_LABEL, recordProperties.debugLevel.toString());

        if (recordProperties.autoStopEnable) {
            JsonObject autoStopProperties = JsonValueBuilder.jsonValue();

            if (recordProperties.autoStopTimeEnable) {
                JsonObject timeProperties = JsonValueBuilder.jsonValue();
                timeProperties.put(RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_UNIT_LABEL,
                        TimeUnit.MILLISECONDS.toString());
                timeProperties.put(RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_VALUE_LABEL,
                        recordProperties.autoStopTimeInMS);
                autoStopProperties.put(RecordConstants.DEBUG_LOGS_AUTOSTOP_TIME_LABEL, timeProperties.build().asMap());
            }

            if (recordProperties.autoStopFileSizeEnable) {
                JsonObject fileSizeProperties = JsonValueBuilder.jsonValue();
                fileSizeProperties.put(RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_SIZEUNIT_LABEL,
                        FileSizeUnit.KB.toString());
                fileSizeProperties.put(RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_VALUE_LABEL,
                        recordProperties.autoStopFileSizedInKB);
                autoStopProperties.put(RecordConstants.DEBUG_LOGS_AUTOSTOP_FILESIZE_LABEL,
                        fileSizeProperties.build().asMap());
            }
            debugLogsProperties.put(RecordConstants.DEBUG_LOGS_AUTOSTOP_LABEL, autoStopProperties.build().asMap());

        }
        return debugLogsProperties.build();
    }

    /**
     * Get Issue ID
     *
     * @return
     */
    public Long getIssueID() {
        return issueID;
    }

    /**
     * Get Reference ID
     *
     * @return
     */
    public String getReferenceID() {
        return referenceID;
    }

    /**
     * Get Description
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * is ThreadDump feature enable
     *
     * @return
     */
    public Boolean isThreadDumpEnabled() {
        return threadDumpEnable;
    }

    /**
     * Get ThreadDump delay between two threads dumps in seconds
     * NB: Thread dump should be enable first
     *
     * @return
     */
    public Long getThreadDumpDelayInSeconds() {
        return threadDumpDelayInSeconds;
    }

    /**
     * is config export feature enable
     *
     * @return
     */
    public Boolean isConfigExportEnabled() {
        return configExportEnable;
    }

    /**
     * is share password enable
     *
     * @return
     */
    public Boolean isConfigExportSharePasswordEnabled() {
        return configExportSharePassword;
    }

    /**
     * Get configuration export password
     * NB: Config export should be enable first
     *
     * @return
     */
    public String getConfigExportPassword() {
        return configExportPassword;
    }

    /**
     * Get debug level
     *
     * @return
     */
    public DebugLevel getDebugLevel() {
        return debugLevel;
    }

    /**
     * Is auto stop feature enable
     *
     * @return
     */
    public Boolean isAutoStopEnabled() {
        return autoStopEnable;
    }

    /**
     * Is auto stop time feature enable
     *
     * @return
     */
    public Boolean isAutoStopTimeEnabled() {
        return autoStopTimeEnable;
    }

    /**
     * Get auto stop time in milli-seconds
     * NB: auto stop time should be enable first
     *
     * @return
     */
    public Long getAutoStopTimeInMS() {
        return autoStopTimeInMS;
    }

    /**
     * is auto stop file size enable
     *
     * @return
     */
    public Boolean isAutoStopFileSizeEnabled() {
        return autoStopFileSizeEnable;
    }

    /**
     * Get auto stop file size in KB
     * NB: auto stop file size should be enable first
     *
     * @return
     */
    public Long getAutoStopFileSizeInKB() {
        return autoStopFileSizedInKB;
    }

    /**
     * is zip enable
     *
     * @return
     */
    public Boolean isZipEnabled() {
        return zipEnable;
    }

    @Override
    public String toString() {
        return "RecordProperties{" +
                "issueID=" + issueID +
                ", description='" + description + '\'' +
                ", threadDumpEnable=" + threadDumpEnable +
                ", threadDumpDelayInSeconds=" + threadDumpDelayInSeconds +
                ", configExportEnable=" + configExportEnable +
                ", debugLevel=" + debugLevel +
                ", autoStopEnable=" + autoStopEnable +
                ", autoStopTimeEnable=" + autoStopTimeEnable +
                ", autoStopTimeInMS=" + autoStopTimeInMS +
                ", autoStopFileSizeEnable=" + autoStopFileSizeEnable +
                ", autoStopFileSizedInKB=" + autoStopFileSizedInKB +
                ", zipEnable=" + zipEnable +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RecordProperties that = (RecordProperties) o;

        return Objects.equals(autoStopEnable, that.autoStopEnable) && Objects.equals(autoStopFileSizeEnable, that
                .autoStopFileSizeEnable) && Objects.equals(autoStopFileSizedInKB, that.autoStopFileSizedInKB) &&
                Objects.equals(autoStopTimeEnable, that.autoStopTimeEnable) && Objects.equals(autoStopTimeInMS, that
                .autoStopTimeInMS) && Objects.equals(configExportEnable, that.configExportEnable) && Objects.equals
                (issueID, that.issueID) && Objects.equals(threadDumpDelayInSeconds, that.threadDumpDelayInSeconds) &&
                Objects.equals(threadDumpEnable, that.threadDumpEnable) && Objects.equals(autoStopEnable, that
                .autoStopEnable) && Objects.equals(configExportPassword, that.configExportPassword) && Objects.equals
                (configExportSharePassword, that.configExportSharePassword) && Objects.equals(debugLevel, that
                .debugLevel) && Objects.equals(description, that.description) && Objects.equals(autoStopEnable, that
                .autoStopEnable) && Objects.equals(referenceID, that.referenceID) && Objects.equals(zipEnable, that
                .zipEnable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueID, referenceID, description, threadDumpEnable, threadDumpDelayInSeconds,
                configExportEnable, configExportPassword, configExportSharePassword, debugLevel, autoStopEnable,
                autoStopTimeEnable, autoStopTimeInMS, autoStopFileSizeEnable, autoStopFileSizedInKB, zipEnable);
    }
}
