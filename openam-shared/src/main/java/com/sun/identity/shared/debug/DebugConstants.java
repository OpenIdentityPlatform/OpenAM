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
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.shared.debug;

/**
 * Debug constant
 * Having in one place, every global constants used for the debug logging
 */
public final class DebugConstants {

    public static final int MAX_BUFFER_SIZE_EXCEPTION = 300;

    public static final String CONFIG_DEBUG_PROPERTIES_VARIABLE = "com.iplanet.services.debug.properties";

    public static final String CONFIG_DEBUG_PROPERTIES = "/debugconfig.properties";

    public static final String CONFIG_DEBUG_FILEMAP_VARIABLE = "com.iplanet.services.debug.filesmap";

    public static final String CONFIG_DEBUG_FILEMAP = "/debugfiles.properties";

    public static final String CONFIG_DEBUG_LOGFILE_PREFIX = "org.forgerock.openam.debug.prefix";

    public static final String CONFIG_DEBUG_LOGFILE_SUFFIX = "org.forgerock.openam.debug.suffix";

    public static final String CONFIG_DEBUG_LOGFILE_ROTATION = "org.forgerock.openam.debug.rotation";

    public static final String DEFAULT_DEBUG_SUFFIX_FORMAT = "-MM.dd.yyyy-kk.mm";

    public static final String CONFIG_DEBUG_LEVEL = "com.iplanet.services.debug.level";

    public static final String CONFIG_DEBUG_MERGEALL = "com.sun.services.debug.mergeall";

    public static final String CONFIG_DEBUG_MERGEALL_FILE = "debug.out";

    public static final String CONFIG_DEBUG_DIRECTORY = "com.iplanet.services.debug.directory";

    /**
     * Constant string used as property key to look up the debug provider class
     * name.
     */
    public static final String CONFIG_DEBUG_PROVIDER = "com.sun.identity.util.debug.provider";

    private DebugConstants() {

    }
}
