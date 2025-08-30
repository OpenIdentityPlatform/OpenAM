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

import org.apache.click.service.DefaultMessagesMapService;

import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletContext;

/**
 * Provides a messages map factory service for the Click runtime.
 *
 * <h3>Configuration</h3>
 * The default {@link org.apache.click.service.MessagesMapService} implementation is {@link DefaultMessagesMapService}.
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
 *     &lt;<span class="red">messages-map-service</span> classname="<span class="blue">com.mycorp.CustomMessagesMapService</span>"/&gt;
 *
 * &lt;/click-app&gt; </pre>
 *
 * The class <tt>com.mycorp.CustomMessagesMapService</tt> might be defined as follows:
 *
 * <pre class="prettyprint">
 * package com.mycorp;
 *
 * public class CustomMessagesMapService implements MessagesMapService {
 *
 *     public Map<String, String> createMessagesMap(Class&lt;?&gt; baseClass, String globalResource, Locale locale) {
 *         return new MyMessagesMap(baseClass, globalResource, locale);
 *     }
 * } </pre>
 */
public interface MessagesMapService {

    /**
     * Initialize the MessagesMapService with the given application servlet context.
     * <p/>
     * This method is invoked after the MessagesMapService has been constructed.
     *
     * @param servletContext the application servlet context
     * @throws Exception if an error occurs initializing the LogService
     */
    public void onInit(ServletContext servletContext) throws Exception;

    /**
     * Destroy the MessagesMapService.
     */
    public void onDestroy();

    /**
     * Return a new messages map for the given baseClass (a page or control)
     * and the given global resource bundle name.
     *
     * @param baseClass the target class
     * @param globalResource the global resource bundle name
     * @param locale the users Locale
     * @return a new messages map with the messages for the target.
     */
    public Map<String, String> createMessagesMap(Class<?> baseClass,
                                                 String globalResource, Locale locale);
}
