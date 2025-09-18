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

import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.apache.click.util.MessagesMap;

/**
 * Provides a default MessagesMapService which returns MessagesMap implementations
 * of the messages map.
 */
public class DefaultMessagesMapService implements MessagesMapService {

    /**
     * @see MessagesMapService#onInit(ServletContext)
     *
     * @param servletContext the application servlet context
     * @throws Exception if an error occurs initializing the LogService
     */
    public void onInit(ServletContext servletContext) throws Exception {
    }

    /**
     * @see org.apache.click.service.MessagesMapService#onDestroy()
     */
    public void onDestroy() {
    }

    /**
     * Return a MessagesMap instance for the target baseClass, global resource
     * name and locale.
     *
     * @param baseClass the target class
     * @param globalResource the global resource bundle name
     * @param locale the users Locale
     *
     * @return a MessagesMap instance.
     *
     * @see MessagesMapService#createMessagesMap(java.lang.Class, java.lang.String, java.util.Locale)
     * @see MessagesMap#MessagesMap(Class, String)
     */
    public Map<String, String> createMessagesMap(Class<?> baseClass,
                                                 String globalResource, Locale locale) {
        return new MessagesMap(baseClass, globalResource, locale);
    }
}
