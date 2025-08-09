package org.forgerock.openam.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class TestLogger {

    @Test
    public void testLogger() {
        Logger logger = LoggerFactory.getLogger(TestLogger.class);
        logger.info("test");
        logger.warn("test2");
        logger.error("test3", new Exception("test exception"));
    }

}
