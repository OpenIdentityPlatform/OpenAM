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
package com.sun.identity.shared.debug;

import com.sun.identity.shared.debug.file.DebugConfiguration;
import com.sun.identity.shared.debug.file.DebugFile;
import com.sun.identity.shared.debug.file.DebugFileProvider;
import com.sun.identity.shared.debug.file.impl.DebugFileImpl;
import com.sun.identity.shared.debug.file.impl.StdDebugFile;
import org.forgerock.util.time.TimeService;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug file provider with an accelerate clock
 */
public class DebugFileProviderForTest implements DebugFileProvider {

    private Map<String, DebugFile> debugMap = new HashMap<String, DebugFile>();
    private TimeService clock;
    private DebugConfiguration configuration;

    public DebugFileProviderForTest(DebugConfiguration configuration, TimeService accelerateClock) {
        this.configuration = configuration;
        this.clock = accelerateClock;
    }

    public void setClock(TimeService accelerateClock) {
        this.clock = accelerateClock;
    }

    public synchronized DebugFile getInstance(String debugName) {
        DebugFile debugFile = debugMap.get(debugName);
        if (debugFile == null) {
            debugFile = new DebugFileImpl(configuration, debugName, clock);
            debugMap.put(debugName, debugFile);
        }
        return debugFile;
    }


    public DebugFile getStdOutDebugFile() {
        return StdDebugFile.getInstance();
    }
}