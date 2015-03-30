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
 * Copyright 2014-2015 ForgeRock AS.
 */
package com.sun.identity.shared.debug.file.impl;


import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.file.DebugConfiguration;
import com.sun.identity.shared.debug.file.DebugFile;
import com.sun.identity.shared.debug.file.DebugFileProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug file provider
 * Manage Debug files controller
 * Keep the following constraint one :
 * - One debugFile instance for One log file
 */
public class DebugFileProviderImpl implements DebugFileProvider {

    private Map<String, DebugFile> debugMap = new HashMap<String, DebugFile>();

    private DebugConfiguration configuration;

    /**
     * Default constructor
     */
    public DebugFileProviderImpl() {

        try {
            this.configuration = new DebugConfigurationFromProperties(DebugConstants.CONFIG_DEBUG_PROPERTIES);

        } catch (InvalidDebugConfigurationException e) {
            StdDebugFile.printError("DebugConfiguration", "'" + DebugConstants.CONFIG_DEBUG_PROPERTIES +
                    "' isn't valid, the default configuration will be used instead: " + e.getMessage(), null);
            this.configuration = DefaultDebugConfiguration.getInstance();
        }
    }

    /**
     * Get debug file instance
     *
     * @param debugName name of the debug instance which will be returned.
     * @return debug file
     */
    public synchronized DebugFile getInstance(String debugName) {
        DebugFile debugFile = debugMap.get(debugName);
        if (debugFile == null) {
            debugFile = new DebugFileImpl(configuration, debugName);
            debugMap.put(debugName, debugFile);
        }
        return debugFile;
    }

    /**
     * Get std out debug file
     *
     * @return debug file
     */
    public DebugFile getStdOutDebugFile() {
        return StdDebugFile.getInstance();
    }
}
