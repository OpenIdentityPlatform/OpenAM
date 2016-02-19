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
 */
package org.forgerock.openam.core.rest.record;

import static org.forgerock.openam.core.rest.record.RecordStatus.*;
import static org.forgerock.openam.utils.Time.*;

import org.forgerock.json.JsonValue;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Recording the OpenAM debug logs in a separate folder to investigate an issue.
 */
public class Record {

    private static final String STATUS_LABEL = "status";
    private static final String FOLDER_LABEL = "folder";

    private RecordProperties recordProperties;
    private Map<Date, RecordStatus> recordHistory = new TreeMap<Date, RecordStatus>();
    private RecordStatus recordStatus;
    private volatile String folderPath = "";

    /**
     * Create a RecordDebug instance
     *
     * @param recordProperties
     * @param folderPath
     */
    public Record(RecordProperties recordProperties, String folderPath) {
        this.recordProperties = recordProperties;
        this.recordHistory.put(newDate(), INITIALIZED);
        this.recordStatus = RecordStatus.INITIALIZED;
        this.folderPath = folderPath;
    }

    /**
     * Get the record properties
     *
     * @return
     */
    public RecordProperties getRecordProperties() {
        return recordProperties;
    }

    /**
     * Get the record folder path
     *
     * @return
     */
    public String getFolderPath() {
        return this.folderPath;
    }

    /**
     * Get record status
     *
     * @return
     */
    public RecordStatus getRecordStatus() {
        return recordStatus;
    }

    /**
     * Start recording
     */
    public void startRecord() {
        if (recordStatus == RecordStatus.RUNNING) {
            throw new IllegalStateException("Record '" + this + "' is already running.");
        }
        recordHistory.put(newDate(), RUNNING);
        recordStatus = RecordStatus.RUNNING;
    }

    /**
     * Stop recording
     */
    public void stopRecord() {
        if (recordStatus != RecordStatus.RUNNING) {
            return;
        }
        recordHistory.put(newDate(), STOPPED);
        recordStatus = RecordStatus.STOPPED;
    }


    /**
     * Get the history of the records
     *
     * @return
     */
    public Map<Date, RecordStatus> getRecordsHistory() {
        return recordHistory;
    }

    /**
     * Export into json.
     *
     * @return
     */
    public JsonValue exportJson() {
        JsonValue properties = RecordProperties.toJson(this.getRecordProperties());
        properties.put(STATUS_LABEL, getRecordStatus().toString());
        properties.put(FOLDER_LABEL, getFolderPath() + File.separator);
        return properties;
    }


    @Override
    public String toString() {
        return "RecordDebug{" +
                "recordProperties=" + recordProperties +
                ", recordHistory=" + recordHistory +
                ", recordStatus=" + recordStatus +
                ", folderPath='" + folderPath + "'}";
    }
}
