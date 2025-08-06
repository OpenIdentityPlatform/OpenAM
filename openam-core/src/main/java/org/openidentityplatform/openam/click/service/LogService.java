/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.openidentityplatform.openam.click.service;

import jakarta.servlet.ServletContext;

/**
 * Provides a logging service for the Click runtime.
 *
 * <h3>Configuration</h3>
 * The default {@link org.apache.click.service.LogService} implementation is {@link ConsoleLogService}.
 * <p/>
 * You can instruct Click to use a different implementation by adding
 * the following element to your <tt>click.xml</tt> configuration file.
 *
 * <pre class="codeConfig">
 * &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
 * &lt;click-app charset="UTF-8"&gt;
 *
 *     &lt;pages package="org.apache.click.examples.page"/&gt;
 *
 *     &lt;<span class="red">log-service</span> classname="<span class="blue">com.mycorp.CustomLogService</span>"/&gt;
 *
 * &lt;/click-app&gt; </pre>
 *
 * The class <tt>com.mycorp.CustomLogService</tt> might be defined as follows:
 *
 * <pre class="prettyprint">
 * package com.mycorp;
 *
 * public class CustomLogService extends ConsoleLogService {
 *
 *     protected void log(int level, String message, Throwable error) {
 *         // Add custom logic
 *         ...
 *
 *         super.log(level, message, error);
 *     }
 * } </pre>
 */
public interface LogService {

    /**
     * Initialize the LogService with the given application servlet context.
     * <p/>
     * This method is invoked after the LogService has been constructed.
     *
     * @param servletContext the application servlet context
     * @throws Exception if an error occurs initializing the LogService
     */
    public void onInit(ServletContext servletContext) throws Exception;

    /**
     * Destroy the LogService.
     */
    public void onDestroy();

    /**
     * Log the given message at [debug] logging level.
     *
     * @param message the message to log
     */
    public void debug(Object message);

    /**
     * Log the given message and error at [debug] logging level.
     *
     * @param message the message to log
     * @param error the error to log
     */
    public void debug(Object message, Throwable error);

    /**
     * Log the given message at [error] logging level.
     *
     * @param message the message to log
     */
    public void error(Object message);

    /**
     * Log the given message and error at [error] logging level.
     *
     * @param message the message to log
     * @param error the error to log
     */
    public void error(Object message, Throwable error);

    /**
     * Log the given message at [info] logging level.
     *
     * @param message the message to log
     */
    public void info(Object message);

    /**
     * Log the given message and error at [info] logging level.
     *
     * @param message the message to log
     * @param error the error to log
     */
    public void info(Object message, Throwable error);

    /**
     * Log the given message at [trace] logging level.
     *
     * @param message the message to log
     */
    public void trace(Object message);

    /**
     * Log the given message and error at [trace] logging level.
     *
     * @param message the message to log
     * @param error the error to log
     */
    public void trace(Object message, Throwable error);

    /**
     * Log the given message at [warn] logging level.
     *
     * @param message the message to log
     */
    public void warn(Object message);

    /**
     * Log the given message and error at [warn] logging level.
     *
     * @param message the message to log
     * @param error the error to log
     */
    public void warn(Object message, Throwable error);

    /**
     * Return true if [debug] level logging is enabled.
     *
     * @return true if [debug] level logging is enabled
     */
    public boolean isDebugEnabled();

    /**
     * Return true if [info] level logging is enabled.
     *
     * @return true if [info] level logging is enabled
     */
    public boolean isInfoEnabled();

    /**
     * Return true if [trace] level logging is enabled.
     *
     * @return true if [trace] level logging is enabled
     */
    public boolean isTraceEnabled();

}
