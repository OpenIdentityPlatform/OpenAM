package org.forgerock.openam.slf4j;

import com.sun.identity.shared.debug.Debug;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class AMLoggerFactory implements ILoggerFactory {
    public Logger getLogger(String s) {
        return new AMDebugLogger(Debug.getInstance(s));
    }
}
