package org.slf4j.impl;
import org.forgerock.openam.slf4j.AMLoggerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/*
TODO: look into the REQUESTED_API_VERSION string to participate in the slf4j version check version. It is currently not
included as we don't want to update the code when we move to a newer version of slf4j. Not having the version string
means that we might skew with slf4j-api updates. See http://slf4j.org/faq.html, version check mechanism section.
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {
    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
    public static final StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    private static final String loggerFactoryClassStr = AMLoggerFactory.class.getName();

    private final ILoggerFactory loggerFactory;

    private StaticLoggerBinder() {
        loggerFactory = new AMLoggerFactory();
    }

    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
        return loggerFactoryClassStr;
    }
}
