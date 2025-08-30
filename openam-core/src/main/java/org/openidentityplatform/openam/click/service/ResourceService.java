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

import org.apache.click.service.ClickResourceService;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Provides a static resource service interface.
 *
 * <h3>Configuration</h3>
 * The default ResourceService is {@link ClickResourceService}.
 * <p/>
 * However you can instruct Click to use a different implementation by adding
 * the following element to your <tt>click.xml</tt> configuration file.
 *
 * <pre class="codeConfig">
 * &lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
 * &lt;click-app charset="UTF-8"&gt;
 *
 *     &lt;pages package="com.mycorp.page"/&gt;
 *
 *     &lt;<span class="red">resource-service</span> classname="<span class="blue">com.mycorp.service.DynamicResourceService</span>"&gt;
 *
 * &lt;/click-app&gt; </pre>
 */
public interface ResourceService {

    /**
     * Initialize the ResourceService with the given application configuration
     * service instance.
     * <p/>
     * This method is invoked after the ResourceService has been constructed.
     *
     * @param servletContext the application servlet context
     * @throws IOException if an IO error occurs initializing the service
     */
    public void onInit(ServletContext servletContext) throws IOException;

    /**
     * Destroy the ResourceService.
     */
    public void onDestroy();

    /**
     * Return true if the request is for a static resource.
     *
     * @param request the servlet request
     * @return true if the request is for a static resource
     */
    public boolean isResourceRequest(HttpServletRequest request);

    /**
     * Render the resource request to the given servlet resource response.
     *
     * @param request the servlet resource request
     * @param response the servlet response
     * @throws IOException if an IO error occurs rendering the resource
     */
    public void renderResource(HttpServletRequest request, HttpServletResponse response)
            throws IOException;

}
