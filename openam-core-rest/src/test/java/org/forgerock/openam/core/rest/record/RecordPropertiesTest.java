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


import com.sun.identity.shared.debug.DebugLevel;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.file.FileSizeUnit;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

public class RecordPropertiesTest extends DebugTestTemplate {

    public static final String RECORD_DIRECTORY = "/record/";


    @Test
    public void simpleRecord() throws Exception {
        RecordProperties recordProperties = RecordProperties.fromJson(JsonValueBuilder.toJsonValue(IOUtils
                .getFileContentFromClassPath(RecordPropertiesTest.class, RECORD_DIRECTORY + "SimpleRecord.json")));
        assertEquals(recordProperties.getIssueID(), (Long) 1l);
        assertEquals(recordProperties.getReferenceID(), "successing_story");
        assertEquals(recordProperties.getDescription(), "Simple record");
        assertFalse(recordProperties.isThreadDumpEnabled());
        assertFalse(recordProperties.isConfigExportEnabled());
        assertEquals(recordProperties.getDebugLevel(), DebugLevel.MESSAGE);
    }

    @Test
    public void simpleRecordWithAutoStopFileSize() throws Exception {
        RecordProperties recordProperties = RecordProperties.fromJson(JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class, RECORD_DIRECTORY +
                        "SimpleRecordWithAutoStopFileSize.json")));
        assertTrue(recordProperties.isAutoStopEnabled());
        assertTrue(recordProperties.isAutoStopFileSizeEnabled());
        assertEquals(recordProperties.getAutoStopFileSizeInKB(), (Long) 200l);
    }

    @Test
    public void simpleRecordWithAutoStopTime() throws Exception {
        RecordProperties recordProperties = RecordProperties.fromJson(JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class, RECORD_DIRECTORY +
                        "SimpleRecordWithAutoStopTime.json")));
        assertTrue(recordProperties.isAutoStopEnabled());
        assertTrue(recordProperties.isAutoStopTimeEnabled());
        assertEquals(recordProperties.getAutoStopTimeInMS(), (Long) (3l * 1000));
    }

    @Test
    public void simpleRecordWithConfigExport() throws Exception {
        RecordProperties recordProperties = RecordProperties.fromJson(JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class, RECORD_DIRECTORY +
                        "SimpleRecordWithConfigExport.json")));
        assertTrue(recordProperties.isConfigExportEnabled());
        assertEquals(recordProperties.getConfigExportPassword(), "changeit");
    }

    @Test
    public void simpleRecordWithThreadDump() throws Exception {
        RecordProperties recordProperties = RecordProperties.fromJson(JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class, RECORD_DIRECTORY +
                        "SimpleRecordWithThreadDump.json")));
        assertTrue(recordProperties.isThreadDumpEnabled());
        assertEquals(recordProperties.getThreadDumpDelayInSeconds(), (Long) 1l);
    }

    @Test
    public void allOnRecord() throws Exception {
        RecordProperties recordProperties = RecordProperties.fromJson(JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class, RECORD_DIRECTORY + "AllOnRecord" +
                        ".json")));
        checkJsonAllOnRecord(recordProperties);
    }

    @Test
    public void checkExportOnAllOnRecord() throws Exception {
        RecordProperties recordPropertiesBeforeExport = RecordProperties.fromJson(JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class, RECORD_DIRECTORY + "AllOnRecord" +
                        ".json")));
        checkJsonAllOnRecord(recordPropertiesBeforeExport);

        RecordProperties recordPropertiesAfterExport = RecordProperties.fromJson(RecordProperties
                .toJson(recordPropertiesBeforeExport));

        checkJsonAllOnRecord(recordPropertiesAfterExport);
    }

    private void checkJsonAllOnRecord(RecordProperties recordProperties) throws Exception {

        assertEquals(recordProperties.getIssueID(), (Long) 8339l);
        assertEquals(recordProperties.getReferenceID(), "OpenID_connect");
        assertEquals(recordProperties.getDescription(), "all ON record");

        assertTrue(recordProperties.isThreadDumpEnabled());
        assertEquals(recordProperties.getThreadDumpDelayInSeconds(), (Long) TimeUnit.MINUTES.toSeconds(2));

        assertTrue(recordProperties.isConfigExportEnabled());
        assertEquals(recordProperties.getConfigExportPassword(), "changeit");

        assertEquals(recordProperties.getDebugLevel(), DebugLevel.MESSAGE);

        assertTrue(recordProperties.isAutoStopEnabled());
        assertTrue(recordProperties.isAutoStopFileSizeEnabled());
        assertEquals(recordProperties.getAutoStopFileSizeInKB(), (Long) FileSizeUnit.GB.toKB(1));

        assertTrue(recordProperties.isAutoStopEnabled());
        assertTrue(recordProperties.isAutoStopTimeEnabled());
        assertEquals(recordProperties.getAutoStopTimeInMS(), (Long) TimeUnit.MINUTES.toMillis(10));
    }

    @Test
    public void checkConfigExportSharePassword() throws Exception {

        RecordProperties recordPropertiesSharePassword = RecordProperties.fromJson(
                JsonValueBuilder.toJsonValue(IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class,
                        RECORD_DIRECTORY + "OpenAMConfigExportSharePassword.json")));
        assertTrue(recordPropertiesSharePassword.isConfigExportEnabled());
        assertTrue(recordPropertiesSharePassword.isConfigExportSharePasswordEnabled());

        RecordProperties recordPropertiesNotSharePassword = RecordProperties.fromJson(JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class,
                        RECORD_DIRECTORY + "OpenAMConfigExportNotSharePassword.json")));
        assertTrue(recordPropertiesNotSharePassword.isConfigExportEnabled());
        assertFalse(recordPropertiesNotSharePassword.isConfigExportSharePasswordEnabled());

        RecordProperties recordPropertiesSharePasswordAfter =
                RecordProperties.fromJson(RecordProperties.toJson(recordPropertiesSharePassword));
        assertEquals(recordPropertiesSharePassword, recordPropertiesSharePasswordAfter);

        RecordProperties recordPropertiesNotSharePasswordAfter =
                RecordProperties.fromJson(RecordProperties.toJson(recordPropertiesNotSharePassword));
        assertNotEquals(recordPropertiesNotSharePassword, recordPropertiesNotSharePasswordAfter);

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void wrongReferenceID() throws Exception {
        RecordProperties.fromJson(JsonValueBuilder.toJsonValue(
                IOUtils.getFileContentFromClassPath(RecordPropertiesTest.class,
                        RECORD_DIRECTORY + "WrongReferenceID.json")));
    }
}
