/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.license;

import com.sun.identity.shared.DateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores acceptance of license terms in a {@link java.util.Properties} file on disk. Uses a separate properties file
 * for each license file with an entry for each user that has accepted the license, of the form:
 * <pre>
 *     user=date-accepted
 * </pre>
 * Where {@code user} is the user name of the user (usually from {@code user.name} system property), and
 * {@code date-accepted} is the ISO-8601 formatted timestamp of when they accepted that license. Each license log file
 * is placed in the given log directory and named by taking the root name of the license and changing the extension
 * to {@code .log}. For example, {@code license.txt} will produce a {@code license.log} file.
 *
 * @since 12.0.0
 */
public class PropertiesFileLicenseLog implements LicenseLog {
    private static final String LOG_HEADER = "Copyright %TY ForgeRock AS. DO NOT EDIT.";
    private static final String LOG_EXTENSION = ".log";
    private static final Logger logger = Logger.getLogger(PropertiesFileLicenseLog.class.getName());

    private final File logDirectory;

    /**
     * Constructs the license log with the given log directory and log date format (see {@link SimpleDateFormat} for
     * allowed format strings).
     *
     * @param logDirectory the directory to store log files in.
     * @throws NullPointerException if either parameter is null.
     * @throws IllegalArgumentException if the log directory does not exist, is not a directory or is not writeable or
     * if the date format is invalid.
     */
    public PropertiesFileLicenseLog(File logDirectory) {
        if (logDirectory == null) {
            throw new NullPointerException("log directory is null");
        }
        if (!logDirectory.exists()) {
            throw new IllegalArgumentException("log directory does not exist");
        }
        if (!logDirectory.isDirectory()) {
            throw new IllegalArgumentException("not a directory: " + logDirectory.getPath());
        }
        if (!logDirectory.canWrite()) {
            throw new IllegalArgumentException("log directory is not writeable");
        }

        this.logDirectory = logDirectory;
    }

    /**
     * {@inheritDoc}
     */
    public void logLicenseAccepted(License license, String user, Date acceptedDate) {
        Properties log = loadLogFile(license, true);

        String acceptedDateStr = DateUtils.toUTCDateFormat(acceptedDate);

        log.setProperty(user, acceptedDateStr);
        saveLogFile(license, log);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLicenseAccepted(License license, String user) {
        Properties log = loadLogFile(license, false);

        Date logDate = getLogDate(log, user);
        Date now = new Date();

        // Ignore acceptance dates that are in the future (invalid data).
        return logDate != null && now.after(logDate);
    }

    /**
     * Returns the date recorded against the given user in the given log, or null if no date recorded.
     *
     * @param log the log to check.
     * @param user the user to check.
     * @return the date recorded for this user in the log, or null if no date is present.
     */
    private Date getLogDate(Properties log, String user) {
        Date result = null;
        String logDateStr = log.getProperty(user);
        if (logDateStr != null) {
            try {
                result = DateUtils.stringToDate(logDateStr);
            } catch (ParseException e) {
                logger.warning("Invalid log date: " + logDateStr);
            }
        }
        return result;
    }

    /**
     * Loads the log file for the given license and returns it.
     *
     * @param license the license to find the log file for.
     * @param create whether to create the log file if it does not yet exist.
     * @return the contents of the log file or an empty properties map if it does not exist.
     */
    private Properties loadLogFile(License license, boolean create) {
        Properties log = new Properties();
        File logFile = getLogFile(license);

        try {
            // Attempt to create the log file if it doesn't exist yet. Will do nothing if it already exists.
            if (create && logFile.createNewFile()) {
                logger.fine("Created license log file: " + logFile.getAbsolutePath());
            }

            final InputStream in = new FileInputStream(logFile);
            try {
                log.load(in);
            } finally {
                in.close();
            }

        } catch (FileNotFoundException ex) {
            // Only exceptional if we were told to create the log file, otherwise it might well not exist yet.
            if (create) {
                logger.log(Level.SEVERE, "Unable to create license log file: " + logFile.getAbsolutePath(), ex);
            } else {
                logger.fine("Log file does not yet exist");
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Unable to access license log file: " + logFile.getAbsolutePath(), ex);
        }
        return log;
    }

    /**
     * Save the log file for the given license, overwriting any existing log file.
     *
     * @param license the license to write the log file for.
     * @param props the contents of the log file.
     */
    private void saveLogFile(License license, Properties props) {
        File logFile = getLogFile(license);

        logger.fine("Writing log: " + props);
        try {
            final OutputStream out = new FileOutputStream(logFile);
            try {
                props.store(out, String.format(LOG_HEADER, new Date()));
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to store license log file: " + logFile.getAbsolutePath(), ex);
        }
    }

    /**
     * Get the file to use for logging acceptance of the given license. This is the root name of the license itself,
     * with a {@code .log} extension, within the configured log directory. For example, if the log directory is
     * {@code /tmp/logs} and the license file is {@code foo/bar.txt} then the returned log file will be
     * {@code /tmp/logs/bar.log}. Note that any path segment of the license filename is ignored so license names are
     * assumed to be unique.
     *
     * @param license the license to get the log file for.
     * @return the file to use for logging acceptance of this license.
     */
    private File getLogFile(License license) {
        String rootname = new File(license.getFilename()).getName();
        int index = rootname.lastIndexOf('.');
        if (index >= 0) {
            rootname = rootname.substring(0, index);
        }
        return new File(logDirectory, rootname + LOG_EXTENSION);
    }
}
