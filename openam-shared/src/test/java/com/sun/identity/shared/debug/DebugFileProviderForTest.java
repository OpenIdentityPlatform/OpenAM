package com.sun.identity.shared.debug;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
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

    public DebugFileProviderForTest(TimeService accelerateClock) {
        this.clock = accelerateClock;
    }

    public void setClock(TimeService accelerateClock) {
        this.clock = accelerateClock;

    }

    public synchronized DebugFile getInstance(String debugName) {
        DebugFile debugFile = debugMap.get(debugName);
        if (debugFile == null) {
            String debugDirectory = SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_DIRECTORY);
            debugFile = new DebugFileImpl(debugName, debugDirectory, clock);
            debugMap.put(debugName, debugFile);
        }
        return debugFile;
    }


    public DebugFile getStdOutDebugFile() {
        return StdDebugFile.getInstance();
    }
}