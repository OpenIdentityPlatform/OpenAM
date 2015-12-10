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
package org.forgerock.openam.audit.events.handlers;

import static com.iplanet.am.util.SystemProperties.CONFIG_PATH;
import static com.sun.identity.shared.Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR;
import static com.sun.identity.shared.datastruct.CollectionHelper.*;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandler;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.CsvSecurity;
import org.forgerock.audit.providers.DefaultKeyStoreHandlerProvider;
import org.forgerock.audit.handlers.csv.CsvAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.openam.utils.StringUtils;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This factory is responsible for creating an instance of the {@link CsvAuditEventHandler}.
 *
 * @since 13.0.0
 */
@Singleton
public class CsvAuditEventHandlerFactory implements AuditEventHandlerFactory {

    private static final Debug DEBUG = Debug.getInstance("amAudit");

    @Override
    public AuditEventHandler create(AuditEventHandlerConfiguration configuration) throws AuditException {
        Map<String, Set<String>> attributes = configuration.getAttributes();

        CsvAuditEventHandlerConfiguration csvHandlerConfiguration = new CsvAuditEventHandlerConfiguration();
        String location = getMapAttr(attributes, "location");
        csvHandlerConfiguration.setLogDirectory(location.replaceAll("%BASE_DIR%", SystemProperties.get(CONFIG_PATH)).
                replaceAll("%SERVER_URI%", SystemProperties.get(AM_SERVICES_DEPLOYMENT_DESCRIPTOR)));
        csvHandlerConfiguration.setTopics(attributes.get("topics"));
        csvHandlerConfiguration.setName(configuration.getHandlerName());
        csvHandlerConfiguration.setEnabled(getBooleanMapAttr(attributes, "enabled", true));
        setFileRotationPolicies(csvHandlerConfiguration, attributes);
        setFileRetentionPolicies(csvHandlerConfiguration, attributes);
        csvHandlerConfiguration.setBufferingConfiguration(getBufferingConfiguration(attributes));
        csvHandlerConfiguration.setSecurity(getCsvSecurity(attributes));

        return new CsvAuditEventHandler(csvHandlerConfiguration, configuration.getEventTopicsMetaData(),
                new DefaultKeyStoreHandlerProvider());
    }

    private void setFileRotationPolicies(CsvAuditEventHandlerConfiguration csvHandlerConfiguration,
            Map<String, Set<String>> attributes) throws AuditException {
        boolean enabled = getBooleanMapAttr(attributes, "rotationEnabled", true);
        csvHandlerConfiguration.getFileRotation().setRotationEnabled(enabled);
        long maxFileSize = getLongMapAttr(attributes, "rotationMaxFileSize", 100000000L, DEBUG);
        csvHandlerConfiguration.getFileRotation().setMaxFileSize(maxFileSize);
        String filePrefix = getMapAttr(attributes, "rotationFilePrefix", "");
        csvHandlerConfiguration.getFileRotation().setRotationFilePrefix(filePrefix);
        String fileSuffix = getMapAttr(attributes, "rotationFileSuffix", "-MM.dd.yy-kk.mm");
        csvHandlerConfiguration.getFileRotation().setRotationFileSuffix(fileSuffix);
        String interval = getMapAttr(attributes, "rotationInterval", "-1");
        try {
            Long intervalAsLong = Long.valueOf(interval);
            if (intervalAsLong <= 0) {
                //If interval is 0 or a negative value, then this indicates that the feature is disabled. Change
                //it to a value indicating disablement.
                interval = "disabled";
            } else {
                //If interval is a positive number, add seconds to the end as the time unit.
                interval = interval + " seconds";
            }
        } catch (NumberFormatException nfe) {
            throw new AuditException("Attribute 'rotationInterval' is invalid: " + interval);
        }
        csvHandlerConfiguration.getFileRotation().setRotationInterval(interval);
        List<String> times = new ArrayList<>();
        Set<String> rotationTimesAttribute = attributes.get("rotationTimes");
        if (rotationTimesAttribute != null && !rotationTimesAttribute.isEmpty()) {
            for (String rotationTime : rotationTimesAttribute) {
                times.add(rotationTime + " seconds");
            }
            csvHandlerConfiguration.getFileRotation().setRotationTimes(times);
        }
    }

    private void setFileRetentionPolicies(CsvAuditEventHandlerConfiguration csvHandlerConfiguration,
            Map<String, Set<String>> attributes) {
        int maxNumberOfHistoryFiles = getIntMapAttr(attributes, "retentionMaxNumberOfHistoryFiles", 1, DEBUG);
        csvHandlerConfiguration.getFileRetention().setMaxNumberOfHistoryFiles(maxNumberOfHistoryFiles);
        long maxDiskSpaceToUse = getLongMapAttr(attributes, "retentionMaxDiskSpaceToUse", -1L, DEBUG);
        csvHandlerConfiguration.getFileRetention().setMaxDiskSpaceToUse(maxDiskSpaceToUse);
        long minFreeSpaceRequired = getLongMapAttr(attributes, "retentionMinFreeSpaceRequired", -1L, DEBUG);
        csvHandlerConfiguration.getFileRetention().setMinFreeSpaceRequired(minFreeSpaceRequired);
    }

    private EventBufferingConfiguration getBufferingConfiguration(Map<String, Set<String>> attributes) {
        EventBufferingConfiguration bufferingConfiguration = new EventBufferingConfiguration();
        bufferingConfiguration.setEnabled(getBooleanMapAttr(attributes, "bufferingEnabled", true));
        bufferingConfiguration.setAutoFlush(getBooleanMapAttr(attributes, "bufferingAutoFlush", false));
        return bufferingConfiguration;
    }

    private CsvSecurity getCsvSecurity(Map<String, Set<String>> attributes) {
        CsvSecurity csvSecurity = new CsvSecurity();
        csvSecurity.setEnabled(getBooleanMapAttr(attributes, "securityEnabled", false));
        String filename = getMapAttr(attributes, "securityFilename");
        if (StringUtils.isNotEmpty(filename)) {
            csvSecurity.setFilename(filename.replaceAll("%BASE_DIR%", SystemProperties.get(CONFIG_PATH))
                    .replaceAll("%SERVER_URI%", SystemProperties.get(AM_SERVICES_DEPLOYMENT_DESCRIPTOR)));
        }
        csvSecurity.setPassword(getMapAttr(attributes, "securityPassword"));
        csvSecurity.setSignatureInterval(getMapAttr(attributes, "securitySignatureInterval", "900") + " seconds");
        return csvSecurity;
    }
}
