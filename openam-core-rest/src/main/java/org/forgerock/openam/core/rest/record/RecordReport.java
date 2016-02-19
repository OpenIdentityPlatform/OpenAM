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

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.utils.JsonArray;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.file.FileSizeUnit;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Create reports for the logs
 */
public class RecordReport {

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS a zzz");

    private final static String GLOBAL_INFO_LABEL = "GlobalInformation";
    private final static String OPENAM_VERSION_LABEL = "OpenAMVersion";
    private final static String OPENAM_PROPERTIES_LABEL = "OpenAMProperties";
    private final static String DATE_LABEL = "Date";

    // The input record
    private final static String RECORD_LABEL = "Record";

    // JVM
    private final static String JVM_LABEL = "JVM";
    private final static String JVM_ARGUMENTS_LABEL = "Arguments";
    private final static String JVM_PROPERTIES_LABEL = "properties";
    private final static String JVM_JAVA_VERSION_LABEL = "JavaVersion";
    private final static String JVM_MEMORY_LABEL = "Memory";
    private final static String JVM_UNIT_MEMORY_LABEL = "Unit";
    private final static String JVM_USED_MEMORY_LABEL = "Used";
    private final static String JVM_FREE_MEMORY_LABEL = "Free";
    private final static String JVM_TOTAL_MEMORY_LABEL = "Total";
    private final static String JVM_MAX_MEMORY_LABEL = "Max";
    private final static String JVM_BIT_SIZE_GNU_LABEL = "BitSizeGNUSystem";
    private final static String JVM_BIT_SIZE_LABEL = "BitSizeGNUSystem";

    private final static String SYSTEM_PROPERTIES_LABEL = "SystemProperties";

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();

    /**
     * Create the infoReport
     *
     * @param record
     * @return
     */
    public JsonValue infoReport(Record record) {
        JsonObject report = JsonValueBuilder.jsonValue();

        report.put(GLOBAL_INFO_LABEL, globalInformationReport(record).asMap());
        report.put(RECORD_LABEL, RecordProperties.toJson(record.getRecordProperties()).asMap());
        report.put(JVM_LABEL, getJVMInformation().asMap());
        report.put(SYSTEM_PROPERTIES_LABEL, getSystemProperties().asMap());

        return report.build();
    }

    /**
     * Create the global information report
     * * @return
     */
    private JsonValue globalInformationReport(Record record) {

        JsonObject report = JsonValueBuilder.jsonValue();
        synchronized (dateFormat) {
            report.put(DATE_LABEL, dateFormat.format(newDate()));
        }
        report.put(OPENAM_VERSION_LABEL, SystemPropertiesManager.get(Constants.AM_VERSION));

        // OpenAM properties contain a part of the OpenAM configuration, we need to have the config export enable
        if (record.getRecordProperties().isConfigExportEnabled()) {
            JsonObject openAMPropertiesJson = JsonValueBuilder.jsonValue();
            Properties sysProps = SystemProperties.getProperties();
            for (String propertyName : sysProps.stringPropertyNames()) {
                report.put(propertyName, sysProps.getProperty(propertyName));
            }
            report.put(OPENAM_PROPERTIES_LABEL, openAMPropertiesJson.build().asMap());
        }

        return report.build();
    }

    /**
     * Get Date from info report
     *
     * @param jsonRecordProperties
     * @return
     * @throws ParseException
     */
    public Date getDateFromInfoReport(JsonValue jsonRecordProperties) throws ParseException, JsonValueException {
        JsonValue globalInfo = jsonRecordProperties.get(GLOBAL_INFO_LABEL).required();
        return dateFormat.parse(globalInfo.get(DATE_LABEL).required().asString());
    }


    /**
     * Create the JVM information
     *
     * @return
     */
    private JsonValue getJVMInformation() {
        JsonObject report = JsonValueBuilder.jsonValue();

        //Arguments
        List<String> arguments = runtimeMxBean.getInputArguments();
        JsonArray argumentsJson = report.array(JVM_ARGUMENTS_LABEL);
        for (String argument : arguments) {
            argumentsJson.add(argument);
        }

        // some useful jvm properties
        JsonObject propertiesJson = JsonValueBuilder.jsonValue();

        propertiesJson.put("java.vm.info", System.getProperty("java.vm.info"));
        propertiesJson.put("java.vm.name", System.getProperty("java.vm.info"));
        propertiesJson.put("java.vm.specification.name", System.getProperty("java.vm.specification.name"));
        propertiesJson.put("java.vm.specification.vendor", System.getProperty("java.vm.specification.vendor"));
        propertiesJson.put("java.vm.specification.version", System.getProperty("java.vm.specification.version"));
        propertiesJson.put("java.vm.vendor", System.getProperty("java.vm.vendor"));
        propertiesJson.put("java.vm.version", System.getProperty("java.vm.version"));

        report.put(JVM_PROPERTIES_LABEL, propertiesJson.build().asMap());

        report.put(JVM_JAVA_VERSION_LABEL, System.getProperty("java.version"));

        //Memory
        JsonObject memoryJson = JsonValueBuilder.jsonValue();
        memoryJson.put(JVM_UNIT_MEMORY_LABEL, FileSizeUnit.MB);

        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        //Print used memory
        memoryJson.put(JVM_USED_MEMORY_LABEL, FileSizeUnit.B.toMB(runtime.totalMemory() - runtime.freeMemory()));

        //Print free memory
        memoryJson.put(JVM_FREE_MEMORY_LABEL, FileSizeUnit.B.toMB(runtime.freeMemory()));

        //Print total available memory
        memoryJson.put(JVM_TOTAL_MEMORY_LABEL, FileSizeUnit.B.toMB(runtime.totalMemory()));

        //Print Maximum available memory
        memoryJson.put(JVM_MAX_MEMORY_LABEL, FileSizeUnit.B.toMB(runtime.maxMemory()));

        //GNU systems don't support the "sun.arch.data.model" property, so we print both
        memoryJson.put(JVM_BIT_SIZE_GNU_LABEL, System.getProperty("sun.arch.data.model"));
        memoryJson.put(JVM_BIT_SIZE_LABEL, System.getProperty("os.arch"));


        report.put(JVM_MEMORY_LABEL, memoryJson.build().asMap());

        return report.build();
    }

    /**
     * Get the system properties
     *
     * @return
     */
    private JsonValue getSystemProperties() {
        JsonObject report = JsonValueBuilder.jsonValue();
        Properties sysProps = System.getProperties();
        for (String propertyName : sysProps.stringPropertyNames()) {
            report.put(propertyName, sysProps.getProperty(propertyName));
        }
        return report.build();
    }

    /**
     * Create the history report
     *
     * @param record
     * @return
     */
    public String recordHistoryReport(Record record) {
        StringBuilder report = new StringBuilder();
        report.append("--- Records history\n");
        for (Map.Entry<Date, RecordStatus> entry : record.getRecordsHistory().entrySet()) {
            report.append(dateFormat.format(entry.getKey())).append(" -> ").append(entry.getValue()).append("\n");
        }
        return report.toString();
    }


    public String getThreadDump() {
        StringBuilder report = new StringBuilder();

        report.append("DATE : ").append(dateFormat.format(newDate())).append("\n");

        final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
        for (ThreadInfo threadInfo : threadInfos) {
            report.append(threadInfo);
        }
        return report.toString();
    }
}
