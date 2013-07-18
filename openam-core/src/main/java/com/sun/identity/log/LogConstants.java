/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LogConstants.java,v 1.16 2008/09/18 22:56:31 veiming Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.log;

import java.util.logging.Level;

/**
 * Defines constants used in the Logging service. <tt> LogConstants </TT>
 * helps in maintaining a central repository of all the constants used in the
 * package.
 * @supported.api
 */
public class LogConstants {
    /**
     * Attribute defining the maximum (flat) file size, above which
     * the files are either archived or rotated.
     * @supported.api
     */
    public static final String MAX_FILE_SIZE =
        "iplanet-am-logging-max-file-size";
    /**
     * Attribute defining the location where the logs go.
     * @supported.api
     */
    public static final String LOG_LOCATION = "iplanet-am-logging-location";
    /**
     * Constant for the default base directory "prefix"
     * for Flat File logging, indicating that the instance's
     * Configuration directory + server URI should be used.
     */
    public static final String DEF_FF_LOG_LOC_BASE = "CONFIG_DIR_SERVER_URI";
    /**
     * Attribute defining whether the logs to be stored in
     * flat-files or to a Database.
     * @supported.api
     */
    public static final String BACKEND = "iplanet-am-logging-type";
    /**
     * Attribute defining the user name of the database (for
     * authenticating to the Database)
     */
    public static final String DB_USER  = "iplanet-am-logging-db-user";
    /**
     * Attribute defining the password for the user of the db
     * (for authenticating to DB)
     */
    public static final String DB_PASSWORD = "iplanet-am-logging-db-password";
    /**
     * Attribute defining the Database Driver.
     * @supported.api
     */
    public static final String DB_DRIVER = "iplanet-am-logging-db-driver";
    /**
     * Attribute defining the currently present list of logs in the Directory.
     */
    public static final String LOG_LIST = "iplanet-am-logging-log-list";
    /**
     * Attribute defining the set of all fields being logged. These fields are
     * predefined to be time, Date, LoginID, Domain, LogLevel, Data, IPAddr,
     * and HostName.
     * @supported.api
     */
    public static final String ALL_FIELDS = "iplanet-am-logging-allfields";
    /**
     * Attribute defining the fields currently selected by the administrator
     * to be logged.
     * @supported.api
     */
    public static final String LOG_FIELDS = "iplanet-am-logging-logfields";
    /**
     * Attribute defining the FileHandler class
     */
    public static final String FILE_HANDLER =
        "iplanet-am-logging-file-handler";
    /**
     * Attribute defining the Database Handler class
     */
    public static final String DB_HANDLER = "iplanet-am-logging-jdbc-handler";
    /**
     * Attribute defining the number of history files a (flat file)
     * log should have.
     * @supported.api
     */
    public static final String NUM_HISTORY_FILES =
        "iplanet-am-logging-num-hist-file";
    /**
     * Attribute defining the RemoteHandler class
     */
    public static final String REMOTE_HANDLER =
        "iplanet-am-logging-remote-handler";
    /**
     * Default value for remote handler class
     */
   public static final String DEFAULT_REMOTE_HANDER =
       "com.sun.identity.log.handlers.RemoteHandler";

    /**
     * Attribute defining the ELFFormatter class.
     * (OpenSSO ELF Formatting. Hence FHandler
     * should be associated with a ELFFormatter).
     */
    public static final String ELF_FORMATTER =
        "iplanet-am-logging-elf-formatter";

    /**
     * Attribute defining the RemoteFormatter class which creates a
     * xml string for a log action and sends it across to the remote
     * OpenSSO.
     */
    public static final String REMOTE_FORMATTER =
        "iplanet-am-logging-remote-formatter";
    /**
     * Default value for remote formatter class
     */
    public static final String DEFAULT_REMOTE_FORMATTER =
        "com.sun.identity.log.handlers.RemoteFormatter";
    /**
     * Attribute defining the DBFormatter class
     */
    public static final String DB_FORMATTER =
        "iplanet-am-logging-db-formatter";
    /**
     * Attribute defining the Authorization class used to see if a particular
     * user is authorized to perform a particular task.
     */
    public static final String AUTHZ = "iplanet-am-logging-authz-class";
    /**
     * Attribute defining the Token Class used to generate a Token internally
     * for use within the logging components.
     */
    public static final String TOKEN_PROVIDER =
        "iplanet-am-logging-token-class";
    /**
     * Attribute defining the Archiver class
     *
     */
    public static final String ARCHIVER = "iplanet-am-logging-archiver-class";
    /**
     * Platform attribute defining the locale
     */
    public static final String LOCALE =
    "iplanet-am-platform-locale";
    /**
     * Naming attribute defining the logging service url.
     */
    public static final String LOGGING_SERVICE_URL =
    "iplanet-am-naming-logging-url";
    /**
     * Defines CarriageReturn/LineFeed characters.
     * @supported.api
     */
    public static final String CRLF = "\r\n";
    /**
     * Defines the LoggingService name.
     * @supported.api
     */
    public static final String LOGGING_SERVICE = "Logging";
    /**
     * Defines the LoggingService name.
     */
    public static final String MAC_FIELDNAME = "MAC";
    /**
     * Defines the LoggingService name.
     */
    public static final String SIGNATURE_FIELDNAME = "Signature";
    /**
     * Defines the secure LoggingService log verify period.
     */
    public static final String LOGVERIFY_PERIODINSECONDS =
        "iplanet-am-logging-verify-period-in-seconds";
    
    public static final long LOGVERIFY_PERIODINSECONDS_DEFAULT = 3600;

    /**
     * Defines the LogSign period in seconds.
     */
    public static final String LOGSIGN_PERIODINSECONDS         =
        "iplanet-am-logging-signature-period-in-seconds";
    
    public static final long LOGSIGN_PERIODINSECONDS_DEFAULT = 600;
    
    /**
     * Attribute defining the Log Security status; ON or OFF.
     */
    public static final String SECURITY_STATUS =
        "iplanet-am-logging-security-status";
    
    /**
     * Defines the File Read Handler class.
     */
    public static final String FILE_READ_HANDLER =
        "iplanet-am-logging-file-read-handler";
    /**
     * Defines the Secure File Handler class.
     */
    public static final String SECURE_FILE_HANDLER =
        "iplanet-am-logging-secure-file-handler";
    /**
     * Defines the Secure ELF Formatter class.
     */
    public static final String SECURE_ELF_FORMATTER =
        "iplanet-am-logging-secure-elf-formatter";
    /**
     * Defines the DB Read Handler class.
     */
    public static final String DB_READ_HANDLER =
        "sun-am-logging-db-read-handler";
    
    /**
     * Attribute defining the maximum records for flat file logs.
     * Also the default number of maximum records to be read.
     */
    public static final String MAX_RECORDS = "iplanet-am-logging-max-records";

    /**
     * Default max records if none entered
     */
    public static final String MAX_RECORDS_DEFAULT = "500";
    /**
     * Default max records, int version
     */
    public static final int MAX_RECORDS_DEFAULT_INT = 500;

    /**
     * Attribute defining the number of files per keystore after which
     * the keystore should be reinitialized.
     */
    public static final String FILES_PER_KEYSTORE =
        "iplanet-am-logging-files-per-keystore";

    /**
     * Attribute defining the Class that implements what the
     * Verifier does when verification fails
     */
    public static final String VERIFIER_ACTION_CLASS =
        "iplanet-am-logging-verifier-action-class";

    /**
     * Attribute defining the Directory where the logger certificate 
     * store is stored.
     */
    public static final String LOGGER_CERT_STORE =
        "iplanet-am-logging-secure-certificate-store";

    /**
     * Attribute defining the Class that implements the custom
     * debug method in the case of log failure.
     * @supported.api
     */
    public static final String DEBUG_IMPL_CLASS =
    "iplanet-am-logging-debug-class";
    /**
     * Logging property prefix.
     */
    public static final String LOG_PROP_PREFIX =
    "iplanet-am-logging";
    /**
     * Attribute defining the Buffer size.
     * @supported.api
     */
    public static final String BUFFER_SIZE = "iplanet-am-logging-buffer-size";
    /**
     * Attribute defining the Maximum LogRecords held in memory if
     * DB logging fails.
     * @supported.api
     */
    public static final String DB_MEM_MAX_RECS =
        "sun-am-logging-db-max-in-mem";
    /**
     * Attribute defining the Buffering time.
     * @supported.api
     */
    public static final String BUFFER_TIME =
        "iplanet-am-logging-buffer-time-in-seconds";
    public static final long BUFFER_TIME_DEFAULT = 3600;
    /**
     * Attribute defining the Time Buffering Status; ON or OFF.
     * @supported.api
     */
    public static final String TIME_BUFFERING_STATUS =
        "iplanet-am-logging-time-buffering-status";
    /**
     * Attribute defining the Filter Class Name
     */
    public static final String FILTER_CLASS_NAME =
        "iplanet-am-logging-filter-class-name";
    
    /**
     * Attribute defining the Secure Timestamp generator class
     */
    public static final String SECURE_TIMESTAMP_GENERATOR =
        "iplanet-am-logging-secure-timestamp-generator";
    
    /**
     * Attribute defining the secure log helper class
     */
    public static final String SECURE_LOG_HELPER =
        "iplanet-am-logging-secure-log-helper-class-name";
    
    /**
     * Attribute defining the default secure log helper class
     */
    public static final String SECURE_DEFAULT_LOG_HELPER =
        "com.sun.identity.log.secure.SecureLogHelperJSSImpl";

    /**
     * Attribute defining the security initializer class name
     */
    public static final String SECURITY_INITIALIZER =
        "iplanet-am-logging-security-initializer-class-name";
    
    /**
     * Attribute defining the security signing algorithm
     */
    public static final String SECURITY_SIGNING_ALGORITHM =
        "iplanet-am-logging-secure-signing-algorithm";
    
    /**
     * Attribute defining the default security signing algorithm
     */
    public static final String DEFAULT_SECURITY_SIGNING_ALGORITHM =
        "SHA1withRSA";
    
    /**
     * Attribute defining the log file logging level
     */
    public static final String LOGGING_LEVEL = "sun-am-log-level";
    
    /**
     * Attribute defining the Oracle DB data type for the DATA field
     */
    public static final String ORA_DBDATA_FIELDTYPE =
        "sun-am-logging-oradbdata-fieldtype";

    /**
     * Attribute defining the MySQL DB data type for the DATA field
     */
    public static final String MYSQL_DBDATA_FIELDTYPE =
        "sun-am-logging-mysqldbdata-fieldtype";

    /**
     * Attribute defining the format for the Oracle DATE/TIME field
     */
    public static final String ORA_DBDATETIME_FORMAT =
        "sun-am-logging-ora-dbdate-format";

    /**
     * Attribute defining the format for the MySQL DATE/TIME field
     */
    public static final String MYSQL_DBDATETIME_FORMAT =
        "sun-am-logging-mysql-dbdate-format";
    
    /**
     * Property defining whether logging is enabled
     * @supported.api
     */
    public static final String LOG_STATUS =
        "com.iplanet.am.logstatus";

    /**
     * Property defining Logging subdirectory.
     */
    public static final String LOG_LOCATION_SUBDIR =
        "com.sun.identity.log.logSubdir";

    public static final String LOG_READ = "READ";
    
    public static final String LOG_WRITE = "WRITE";
    
    public static final String LOG_CREATE = "CREATE";
    
    public static final String LOG_DELETE  = "DELETE";
    
    /*
     *logInfoMap keys
     */

    /**
     * The Date/Time field. This field is one of the two required in
     * any log record.
     * @supported.api
     */
    public static final String TIME = "TIME";
    /**
     * The Data field. This field is one of the two required in
     * any log record.
     * @supported.api
     */
    public static final String DATA = "Data";
    /**
     * The LogLevel field. The level at which the log record was
     * logged.
     * @supported.api
     */
    public static final String LOG_LEVEL = "LogLevel";
    /**
     * The Domain field. The AM Domain pertaining to the log record's
     * Data field.
     * @supported.api
     */
    public static final String DOMAIN = "Domain";
    /**
     * The LoginID field. The AM Login ID pertaining to the log record's
     * Data field.
     * @supported.api
     */
    public static final String LOGIN_ID = "LoginID";
    /**
     * The IPAddr field. The IP Address pertaining to the log record's
     * Data field.
     * @supported.api
     */
    public static final String IP_ADDR = "IPAddr";
    /**
     * The HostName field. The HostName pertaining to the log record's
     * Data field.
     * @supported.api
     */
    public static final String HOST_NAME = "HostName";
    /**
     * The ModuleName field. The AM Module pertaining to the log record's
     * Data field.
     * @supported.api
     */
    public static final String MODULE_NAME = "ModuleName";
    /**
     * The LoggedBy field. The ID associated with who logged the record.
     * @supported.api
     */
    public static final String LOGGED_BY = "LoggedBy";
    /**
     * The ContextID field. The ID associated with the user's session
     * that is the subject of this log record.
     * @supported.api
     */
    public static final String CONTEXT_ID = "ContextID";
    /**
     * The NoSession field. Whether the authentication request was made with the noSession query parameter
     * and that no permanent session was created when the authentication was successful.
     */
    public static final String NO_SESSION = "NoSession";
    /**
     * The MessageID field. The unique Message Identifier associated with
     * this particular log record data field.
     * @supported.api
     */
    public static final String MESSAGE_ID = "MessageID";

    /**
     * The NameID field. The Name Identifier associated with
     * this particular log record data field.
     * @supported.api
     */
    public static final String NAME_ID = "NameID";

    /**
     * The maximum number of fields in a "regular" record.
     * Includes Time, Data, LogLevel, Domain, LoginID, IPAddr,
     * HostName, ModuleName, LoggedBy, ContextID, MessageID, NameID.
     * Be sure to increment if more fields are added.
     * @supported.api
     */
    public static final int MAX_FIELDS = 12;

    public static final String LOGIN_ID_SID = "LoginIDSid";
    public static final String LOGGED_BY_SID = "LoggedBySid";

    public static final String DEFAULT_AGENT_FILE = "amAuthLog";
    public static final String POLICY_ACCESS = "amPolicy.access";
    public static final String CONSOLE_ACCESS = "amConsole.access";
    public static final String AUTHENTICATION_ACCESS =
        "amAuthentication.access";
    public static final String AUTHENTICATION_ERROR =
        "amAuthentication.error";
    public static final String PASSWORDRESET_ACCESS =
        "amPasswordReset.access";
    public static final String SSO_ACCESS = "amSSO.access";
    public static final String ADMIN_ACCESS = "amAdmin.access";
    public static final String ADMIN_ERROR = "amAdmin.error";
    public static final String [] LOGFILENAMES = {
        DEFAULT_AGENT_FILE,
        POLICY_ACCESS,
        CONSOLE_ACCESS,
        AUTHENTICATION_ACCESS,
        AUTHENTICATION_ERROR,
        PASSWORDRESET_ACCESS,
        SSO_ACCESS,
        ADMIN_ACCESS,
        ADMIN_ERROR };

    public static final int NUM_RECORDS = 0;
    public static final int NUM_BYTES = 1;

    /**
     * Key name for enable/disable Hostname resolution
     */
    public static final String LOG_RESOLVE_HOSTNAME =
        "com.sun.identity.log.resolveHostName";

    /**
     * Attribute name for enable/disable Hostname resolution
     */
    public static final String LOG_RESOLVE_HOSTNAME_ATTR =
        "resolveHostName";

    /**
     * Attribute name for logging status
     */
    public static final String LOG_STATUS_ATTR =
        "logstatus";

    /**
     * the "Not Available" String for when a field is not provided
     */
    public static final String NOTAVAIL = "Not Available";

    /**
     * default logging level
     */
    public static final String DEFAULT_LOGGING_LEVEL_STR = "INFO"; 
    public static final Level DEFAULT_LOGGING_LEVEL = Level.INFO;

    /**
     * logging start due to container starting
     */
    public static final String START_LOG_NEW_LOGGER_NAME =
        "LOG_START_NEW_LOGGER";
    /**
     * logging ending due to container terminating
     */
    public static final String END_LOG_NAME = "LOG_END";
    /**
     * logging start due to logging configuration change
     */
    public static final String START_LOG_CONFIG_NAME = "LOG_START_CONFIG";
    /**
     * logging termination due to logging configuration change
     */
    public static final String END_LOG_CONFIG_NAME = "LOG_END_CONFIG";

    /**
     * System properties which specifies the log directory.
     */
    public static final String SYS_PROP_LOG_DIR = "com.sun.identity.log.dir";
    /**
     * Attribute defining the prefix for the logfiles
     */
    public static final String LOGFILE_PREFIX = "openam-logging-file-prefix";
    /**
     * Attribute defining the suffix for the logfiles. This should be a valid
     * dateformat string.
     */
    public static final String LOGFILE_SUFFIX = "openam-logging-file-suffix";
    /**
     * Attribute defining the rotation interval in minutes.
     */
    public static final String LOGFILE_ROTATION = "openam-logging-file-rotation";
}
