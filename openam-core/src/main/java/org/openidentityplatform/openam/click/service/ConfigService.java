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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.openidentityplatform.openam.click.Page;
import org.openidentityplatform.openam.click.PageInterceptor;
import org.apache.click.util.Format;

/**
 * Provides a Click application configuration service interface.
 * <p/>
 * A single application ConfigService instance is created by the ClickServlet at
 * startup. Once the ConfigService has been initialized it is stored in the
 * ServletContext using the key {@value #CONTEXT_NAME}.
 *
 * <a href="#" name="config"></a>
 * <h3>Configuration</h3>
 * The default ConfigService is {@link XmlConfigService}.
 * <p/>
 * However it is possible to specify a different implementation.
 * <p/>
 * For example you can subclass XmlConfigService and override methods such as
 * {@link #onInit(jakarta.servlet.ServletContext)} to alter initialization
 * behavior.
 * <p/>
 * For Click to recognize your custom service class you must set the
 * context initialization parameter,
 * {@link org.openidentityplatform.openam.click.ClickServlet#CONFIG_SERVICE_CLASS config-service-class}
 * in your <tt>web.xml</tt> file.
 * <p/>
 * Below is an example of a custom service class
 * <tt>com.mycorp.service.CustomConfigService</tt>:
 *
 * <pre class="prettyprint">
 * package com.mycorp.service;
 *
 * public class CustomConfigService extends XmlConfigService {
 *
 *     public CustomConfigService() {
 *     }
 *
 *     public void onInit(ServletContext servletContext) throws Exception {
 *         // Add your logic here
 *         ...
 *
 *         // Call super to resume initialization
 *         super.onInit(servletContext);
 *     }
 * }
 * </pre>
 *
 * <b>Please note</b> that the custom ConfigService implementation must have a
 * no-argument constructor so Click can instantiate the service.
 * <p/>
 * Also define the new service in your <tt>web.xml</tt> as follows:
 *
 * <pre class="prettyprint">
 * {@code
 * <web-app xmlns="http://java.sun.com/xml/ns/j2ee"
 *   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *   xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
 *   version="2.4">
 *
 * ...
 *
 *     <context-param>
 *         <param-name>config-service-class</param-name>
 *         <param-value>com.mycorp.service.CustomConfigSerivce</param-value>
 *     </context-param>
 *
 * ...
 *
 * </web-app>} </pre>
 */
public interface ConfigService {

    /** The trace application mode. */
    public static final String MODE_TRACE = "trace";

    /** The debug application mode. */
    public static final String MODE_DEBUG = "debug";

    /** The development application mode. */
    public static final String MODE_DEVELOPMENT = "development";

    /** The profile application mode. */
    public static final String MODE_PROFILE = "profile";

    /** The profile application mode. */
    public static final String MODE_PRODUCTION = "production";

    /** The error page file path: &nbsp; "<tt>/click/error.htm</tt>". */
    static final String ERROR_PATH = "/click/error.htm";

    /** The page not found file path: &nbsp; "<tt>/click/not-found.htm</tt>". */
    public static final String NOT_FOUND_PATH = "/click/not-found.htm";

    /** The page auto binding mode. */
    public enum AutoBinding { DEFAULT, ANNOTATION, NONE };

    /**
     * The servlet context attribute name. The ClickServlet stores the
     * application ConfigService instance in the ServletContext using this
     * context attribute name. The value of this constant is {@value}.
     */
    public static final String CONTEXT_NAME = "org.openidentityplatform.openam.click.service.ConfigService";

    /**
     * Initialize the ConfigurationService with the given application servlet context.
     * <p/>
     * This method is invoked after the ConfigurationService has been constructed.
     *
     * @param servletContext the application servlet context
     * @throws Exception if an error occurs initializing the ConfigurationService
     */
    public void onInit(ServletContext servletContext) throws Exception;

    /**
     * Destroy the ConfigurationService. This method will also invoke the
     * <tt>onDestroy()</tt> methods on the <tt>FileUploadService</tt>,
     * <tt>TemplateService</tt>, <tt>ResourceService</tt> and the
     * <tt>LogService</tt> in that order.
     */
    public void onDestroy();

    /**
     * Return the application file upload service, which is used to parse
     * multi-part file upload post requests.
     *
     * @return the application file upload service
     */
    public FileUploadService getFileUploadService();

    /**
     * Return the application log service.
     *
     * @return the application log service.
     */
    public LogService getLogService();

    /**
     * Return the application resource service.
     *
     * @return the application resource service.
     */
    public ResourceService getResourceService();

    /**
     * Return the application templating service.
     *
     * @return the application templating service
     */
    public TemplateService getTemplateService();

    /**
     * Return the application messages map service.
     *
     * @return the application messages Map service
     */
    public MessagesMapService getMessagesMapService();

    /**
     * Return the Click application mode value: &nbsp;
     * <tt>["production", "profile", "development", "debug", "trace"]</tt>.
     *
     * @return the application mode value
     */
    public String getApplicationMode();

    /**
     * Return the Click application charset or null if not defined.
     *
     * @return the application charset value
     */
    public String getCharset();

    /**
     * Return the error handling page <tt>Page</tt> <tt>Class</tt>.
     *
     * @return the error handling page <tt>Page</tt> <tt>Class</tt>
     */
    public Class<? extends Page> getErrorPageClass();

    /**
     * Create and return a new format object instance.
     *
     * @return a new format object instance
     */
    public Format createFormat();

    /**
     * Return true if JSP exists for the given ".htm" path.
     *
     * @param path the Page ".htm" path
     * @return true if JSP exists for the given ".htm" path
     */
    public boolean isJspPage(String path);

    /**
     * Return true if the given resource is a Page class template, false
     * otherwise.
     * <p/>
     * Below is an example showing how to map <tt>.htm</tt> and <tt>.jsp</tt>
     * files as Page class templates.
     *
     * <pre class="prettyprint">
     * public class XmlConfigService implements ConfigService {
     *
     *     ...
     *
     *     public boolean isTemplate(String path) {
     *         if (path.endsWith(".htm") || path.endsWith(".jsp")) {
     *             return true;
     *         }
     *         return false;
     *     }
     *
     *     ...
     * } </pre>
     *
     * @param path the path to check if it is a Page class template or not
     * @return true if the resource is a Page class template, false otherwise
     */
    public boolean isTemplate(String path);

    /**
     * Return the page auto binding mode. If the mode is "PUBLIC" any public
     * Page fields will be auto bound, if the mode is "ANNOTATION" any Page field
     * with the "Bindable" annotation will be auto bound and if the mode is
     * "NONE" no Page fields will be auto bound.
     *
     * @return the Page field auto binding mode { PUBLIC, ANNOTATION, NONE }
     */
    public ConfigService.AutoBinding getAutoBindingMode();

    /**
     * Return true if the application is in "production" mode.
     *
     * @return true if the application is in "production" mode
     */
    public boolean isProductionMode();

    /**
     * Return true if the application is in "profile" mode.
     *
     * @return true if the application is in "profile" mode
     */
    public boolean isProfileMode();

    /**
     * Return the Click application locale or null if not defined.
     *
     * @return the application locale value
     */
    public Locale getLocale();

    /**
     * Return the path for the given page Class.
     *
     * @param pageClass the class of the Page to lookup the path for
     * @return the path for the given page Class
     * @throws IllegalArgumentException if the Page Class is not configured
     * with a unique path
     */
    public String getPagePath(Class<? extends Page> pageClass);

    /**
     * Return the page <tt>Class</tt> for the given path. The path must start
     * with a <tt>"/"</tt>.
     *
     * @param path the page path
     * @return the page class for the given path
     * @throws IllegalArgumentException if the Page Class for the path is not
     * found
     */
    public Class<? extends Page> getPageClass(String path);

    /**
     * Return the list of configured page classes.
     *
     * @return the list of configured page classes
     */
    public List<Class<? extends Page>> getPageClassList();

    /**
     * Return Map of bindable fields for the given page class.
     *
     * @param pageClass the page class
     * @return a Map of bindable fields for the given page class
     */
    public Map<String, Field> getPageFields(Class<? extends Page> pageClass);

    /**
     * Return the bindable field of the given name for the pageClass,
     * or null if not defined.
     *
     * @param pageClass the page class
     * @param fieldName the name of the field
     * @return the bindable field of the pageClass with the given name or null
     */
    public Field getPageField(Class<? extends Page> pageClass, String fieldName);

    /**
     * Return the headers of the page for the given path.
     *
     * @param path the path of the page
     * @return a Map of headers for the given page path
     */
    public Map<String, Object> getPageHeaders(String path);

    /**
     * Return an array bindable for the given page class.
     *
     * @param pageClass the page class
     * @return an array bindable fields for the given page class
     */
    public Field[] getPageFieldArray(Class<? extends Page> pageClass);

    /**
     * Return the list of configured PageInterceptors instances.
     *
     * @return the list of configured PageInterceptors instances
     */
    public List<PageInterceptor> getPageInterceptors();

    /**
     * Return the page not found <tt>Page</tt> <tt>Class</tt>.
     *
     * @return the page not found <tt>Page</tt> <tt>Class</tt>
     */
    public Class<? extends Page> getNotFoundPageClass();

    /**
     * Return the application servlet context.
     *
     * @return the application servlet context
     */
    public ServletContext getServletContext();

}
