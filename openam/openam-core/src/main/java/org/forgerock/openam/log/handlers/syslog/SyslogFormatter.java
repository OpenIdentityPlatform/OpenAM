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
 * Copyright 2013 Cybernetica AS
 * Portions copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.log.handlers.syslog;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.log.ILogRecord;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.DateUtils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Format LogRecord as a <a href="http://tools.ietf.org/html/rfc5424#section-6">RFC5424-compliant syslog message</a>.
 * Fields of syslog message are:
 * <ul>
 * <li>priority (facility * 8 + priority)</li>
 * <li>version (constant "1" for now)</li>
 * <li>timestamp (format derived from RFC3339, we use LogRecord's timestamp)</li>
 * <li>hostname (InetAddress.getLocalHost().getHostName() with AM_SERVER_HOST system property as a default)</li>
 * <li>app-name (device or application id, we use the constant "OpenAM" value)</li>
 * <li>proc-id (change of this value indicates discontinuity in logs and groups records from the same process, we use
 * hash code of the SyslogFormatter class)</li>
 * <li>msg id (type of message, LogConstants.MESSAGE_ID from info map or - (nil-value))</li>
 * <li>structured data (keys and values from info map, expressed as one SD-ELEMENT with "logRecord@36733" as SD-ID.
 * 36733 is IANA enterpriseId of Forgerock AS)</li>
 * <li>log message.</li>
 * </ul>
 *
 * As the structured data field is capable of expressing all the key-value pairs from info map, we include all keys
 * except those that are listed in allFields configuration option and not in selectedFields. If
 * iplanet-am-logging-syslog-add-unlisted-fields is not checked, only fields in allFields are included.
 *
 * NB: Current syslog daemons (both rsyslog and syslog-ng) need additional configuration to allow logging via UDP (or
 * TCP) and to log structured data.
 */
public class SyslogFormatter extends Formatter {

    private static final String APP_NAME = "OpenAM";
    private static final int LOCAL5_FACILITY = 21;
    private final String hostname;
    private final String procId;
    private int facility = LOCAL5_FACILITY;
    private String[] allFields = null;
    private Set<String> allowed = null;
    private Set<String> allFieldsSet = null;

    private final static List<String> FACILITIES = Arrays.asList(
        "kern", "user", "mail", "daemon", "auth",
        "syslog", "lpr", "news", "uucp", "cron",
        "authpriv", "ftp", "ntp", "logaudit", "logalert", "clockd",
        "local0", "local1", "local2", "local3",
        "local4", "local5", "local6", "local7");

    /**
     * Initialize a new formatter. Reads and caches constant fields to use in log messages and initializes collections
     * for field-filtering.
     */
    public SyslogFormatter() {
        hostname = getLocalHostName();
        procId = String.valueOf(SyslogFormatter.class.hashCode());

        allFields = LogManagerUtil.getLogManager().getAllFields();
        allowed = LogManagerUtil.getLogManager().getSelectedFieldSet();
        allFieldsSet = new HashSet<String>();
        allFieldsSet.addAll(Arrays.asList(allFields));

        String facilityStr = LogManagerUtil.getLogManager().getProperty(LogConstants.SYSLOG_FACILITY);
        if (facilityStr != null) {
            facility = FACILITIES.indexOf(facilityStr.toLowerCase());
            if (facility == -1) {
                Debug.error("Invalid facility '" + facilityStr + "', defaulting to local5");
                facility = LOCAL5_FACILITY;
            }
        }

        if (Debug.messageEnabled()) {
            Debug.message("Syslog formatter initialized. Configuration:"
                    + "\nallFields = " + Arrays.toString(allFields)
                    + "\nallowed = " + allowed
                    + "\nhostname = " + hostname
                    + "\nfacility = " + facility + ", " + FACILITIES.get(facility));
        }
    }

    /**
     * Format a LogRecord as a RFC5424 structured syslog message. Most of interesting bits are expressed as key-value
     * pairs in structured data element with id logRecord@36733.
     *
     * @param record A standard LogRecord to be formatted.
     * @return Formatted string, suitable to be sent as a syslog message.
     */
    @Override
    public String format(final LogRecord record) {
        Map<String, String> info = (record instanceof ILogRecord ? ((ILogRecord) record).getLogInfoMap()
                : Collections.emptyMap());

        return "<" + String.valueOf(getPriorityValue(record.getLevel())) + ">" // PRI
                + "1 " // VERSION
                + DateUtils.toFullLocalDateFormat(new Date(record.getMillis())) + " " // TIMESTAMP
                + hostname + " " // HOSTNAME
                + APP_NAME + " " // APP-NAME
                + procId + " " // PROCID
                + ((info.containsKey(LogConstants.MESSAGE_ID)) ? info.get(LogConstants.MESSAGE_ID) : "-") + " " // MSGID
                + getStructuredData(info) + " " // STRUCTURED-DATA
                + formatMessage(record); // MSG
    }

    private int getSeverity(Level l) {
        if (l == null) {
            return 3;
        }

        if (Level.SEVERE.intValue() <= l.intValue()) {
            return 0; // Emergency: System is unusable
        }
        // 1 Alert: action must be taken immediately
        // 2 Critical: critical conditions
        // 3 Error: error conditions
        if (Level.WARNING.intValue() <= l.intValue()) {
            return 4; // Warning: warning conditions
        }
        // 5 Notice: normal but significant condition
        if (Level.INFO.intValue() <= l.intValue()) {
            return 6; // Informational: informational messages
        }

        return 7; // Debug: debug-level messages
    }

    private int getPriorityValue(final Level l) {
        return (facility << 3) + getSeverity(l);
    }

    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            Debug.error("Cannot resolve localhost's name", uhe);
            return SystemProperties.get(Constants.AM_SERVER_HOST);
        }
    }

    private String getStructuredData(final Map<String, String> info) {
        if (info.isEmpty()) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();
        // 36733 is enterpriseId (OID suffix) of ForgeRock AS
        sb.append("[logRecord@36733");

        for (String k : allFields) {
            if (allowed.contains(k)) {
                sb.append(" ");
                addParam(sb, k, info.get(k));
            }
        }

        sb.append("]");

        return sb.toString();
    }

    private void addParam(final StringBuilder sb, final String key, final String value) {
        sb.append(key);
        sb.append("=\"");
        if (value != null) {
            for (char c : value.toCharArray()) {
                switch (c) {
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '"':
                        sb.append("\\\"");
                        break;
                    case ']':
                        sb.append("\\]");
                        break;
                    default:
                        sb.append(c);
                }
            }
        }
        sb.append("\"");
    }
}
