/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: UpgradeUtils.java,v 1.18 2009/09/30 17:35:24 goodearth Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package org.forgerock.openam.upgrade;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that deals with determining and comparing versions of OpenAM.
 *
 * @since 13.0.0
 */
public class VersionUtils {

    private static final Debug DEBUG = Debug.getInstance("amUpgrade");
    private static final Pattern VERSION_FORMAT_PATTERN = Pattern.compile("^(?:.*?(\\d+\\.\\d+\\.?\\d*).*)?\\((.*)\\)");
    private static volatile boolean evaluatedUpgradeVersion = false;
    private static boolean isVersionNewer = false;

    private VersionUtils() {
    }

    /**
     * Returns true if the OpenAM version of the war file is newer than the one
     * currently deployed.
     *
     * @return true if the war file version is newer than the deployed version
     */
    public static boolean isVersionNewer() {

        if (!evaluatedUpgradeVersion) {
            // Cache result to avoid repeated evaluations
            isVersionNewer = isVersionNewer(getCurrentVersion(), getWarFileVersion());
            evaluatedUpgradeVersion = true;
        }

        return isVersionNewer;
    }

    public static boolean isVersionNewer(String currentVersion, String warVersion) {
        String[] current = parseVersion(currentVersion);
        String[] war = parseVersion(warVersion);
        if (current == null || war == null) {
            return false;
        }
        if (SystemProperties.get("org.forgerock.donotupgrade") != null) return false;

        SimpleDateFormat versionDateFormat = new SimpleDateFormat(Constants.VERSION_DATE_FORMAT, Locale.UK);
        Date currentVersionDate = null;
        Date warVersionDate = null;

        try {
            currentVersionDate = versionDateFormat.parse(current[1]);
            warVersionDate = versionDateFormat.parse(war[1]);
        } catch (ParseException pe) {
            DEBUG.error("Unable to parse date strings; current:" + currentVersion +
                    " war version: " + warVersion, pe);
        }

        if (currentVersionDate == null || warVersionDate == null) {
            // stop upgrade if we cannot check
            return false;
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Current version: " + currentVersionDate);
            DEBUG.message("War version: " + warVersionDate);
        }
        boolean isBefore = currentVersionDate.before(warVersionDate);
        if (isBefore) {
            return Integer.valueOf(current[0]) <= Integer.valueOf(war[0]);
        } else {
            return Integer.valueOf(current[0]) < Integer.valueOf(war[0]);
        }
    }

    public static String getCurrentVersion() {
        return SystemProperties.get(Constants.AM_VERSION);
    }

    public static String getWarFileVersion() {
        return ServerConfiguration.getWarFileVersion();
    }

    private static String[] parseVersion(String version) {
        //Handle the special case when we were unable to determine the current or the new version, this can happen for
        //example when the configuration store is not available and the current version cannot be retrieved.
        if (version == null) {
            return null;
        }
        Matcher matcher = VERSION_FORMAT_PATTERN.matcher(version);
        if (matcher.matches()) {
            String ver = matcher.group(1);
            if (ver == null) {
                ver = "-1";
            } else {
                ver = ver.replace(".", "");
            }
            return new String[]{ver, matcher.group(2)};
        }
        return null;
    }

    /**
     * Checks if the currently deployed OpenAM has the same version number as the expected version number.
     *
     * @param expectedVersion The version we should match OpenAM's current version against.
     * @return <code>false</code> if the version number cannot be detected or if the local version does not match,
     * <code>true</code> otherwise.
     */
    public static boolean isCurrentVersionEqualTo(Integer expectedVersion) {
        String[] parsedVersion = parseVersion(getCurrentVersion());
        if (parsedVersion == null) {
            //unable to determine current version, we can't tell if it matches the expected.
            return false;
        }
        return expectedVersion.equals(Integer.valueOf(parsedVersion[0]));
    }

    /**
     * Checks to see if the currently installed OpenAM version is less than the specified version.
     * @param expectedVersion The version to test for.
     * @param notParsed The value to return if the current version cannot be parsed.
     */
    public static boolean isCurrentVersionLessThan(int expectedVersion, boolean notParsed) {
        String[] parsedVersion = parseVersion(getCurrentVersion());
        if (parsedVersion == null) {
            //unable to determine current version, we can't tell if it matches the expected.
            return notParsed;
        }
        return Integer.valueOf(parsedVersion[0]).intValue() < expectedVersion;
    }
}
