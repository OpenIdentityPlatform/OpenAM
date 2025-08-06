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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import jakarta.servlet.ServletContext;

import jakarta.servlet.http.HttpServletRequest;
import ognl.Ognl;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.openidentityplatform.openam.click.Control;
import org.openidentityplatform.openam.click.Page;
import org.openidentityplatform.openam.click.PageInterceptor;
import org.apache.click.service.CommonsFileUploadService;
import org.apache.click.util.Bindable;
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.apache.click.util.Format;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.click.util.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides a Click XML configuration service class.
 * <p/>
 * This class reads Click configuration information from a file named
 * <tt>click.xml</tt>. The service will first lookup the <tt>click.xml</tt>
 * under the applications <tt>WEB-INF</tt> directory, and if not found
 * attempt to load the configuration file from the classpath root.
 * <p/>
 * Configuring Click through the <tt>click.xml</tt> file is the most common
 * technique.
 * <p/>
 * However you can instruct Click to use a different service implementation.
 * Please see {@link org.apache.click.service.ConfigService} for more details.
 */
public class XmlConfigService implements ConfigService, EntityResolver {

    /** The name of the Click logger: &nbsp; "<tt>org.apache.click</tt>". */
    static final String CLICK_LOGGER = "org.apache.click";

    /** The click deployment directory path: &nbsp; "/click". */
    static final String CLICK_PATH = "/click";

    /** The default common page headers. */
    static final Map<String, Object> DEFAULT_HEADERS;

    /**
     * The default velocity properties filename: &nbsp;
     * "<tt>/WEB-INF/velocity.properties</tt>".
     */
    static final String DEFAULT_VEL_PROPS = "/WEB-INF/velocity.properties";

    /** The click DTD file name: &nbsp; "<tt>click.dtd</tt>". */
    static final String DTD_FILE_NAME = "click.dtd";

    /**
     * The resource path of the click DTD file: &nbsp;
     * "<tt>/org/apache/click/click.dtd</tt>".
     */
    static final String DTD_FILE_PATH = "/org/apache/click/" + DTD_FILE_NAME;

    /**
     * The user supplied macro file name: &nbsp; "<tt>macro.vm</tt>".
     */
    static final String MACRO_VM_FILE_NAME = "macro.vm";

    /** The production application mode. */
    static final int PRODUCTION = 0;

    /** The profile application mode. */
    static final int PROFILE = 1;

    /** The development application mode. */
    static final int DEVELOPMENT = 2;

    /** The debug application mode. */
    static final int DEBUG = 3;

    /** The trace application mode. */
    static final int TRACE = 4;

    static final String[] MODE_VALUES =
            { "production", "profile", "development", "debug", "trace" };

    private static final Object PAGE_LOAD_LOCK = new Object();

    /**
     * The name of the Velocity logger: &nbsp; "<tt>org.apache.velocity</tt>".
     */
    static final String VELOCITY_LOGGER = "org.apache.velocity";

    /**
     * The global Velocity macro file name: &nbsp;
     * "<tt>VM_global_library.vm</tt>".
     */
    static final String VM_FILE_NAME = "VM_global_library.vm";

    /** Initialize the default headers. */
    static {
        DEFAULT_HEADERS = new HashMap<String, Object>();
        DEFAULT_HEADERS.put("Pragma", "no-cache");
        DEFAULT_HEADERS.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        DEFAULT_HEADERS.put("Expires", new Date(1L));
    }

    private static final String GOOGLE_APP_ENGINE = "Google App Engine";

    // ------------------------------------------------ Package Private Members

    /** The Map of global page headers. */
    Map commonHeaders;

    /** The page automapping override page class for path list. */
    final List excludesList = new ArrayList();

    /** The map of ClickApp.PageElm keyed on path. */
    final Map pageByPathMap = new HashMap();

    /** The map of ClickApp.PageElm keyed on class. */
    final Map pageByClassMap = new HashMap();

    /** The list of page packages. */
    final List pagePackages = new ArrayList();

    // -------------------------------------------------------- Private Members

    /** The automatically bind controls, request parameters and models flag. */
    private ConfigService.AutoBinding autobinding;

    /** The Commons FileUpload service class. */
    private FileUploadService fileUploadService;

    /** The format class. */
    private Class<? extends Format> formatClass;

    /** The character encoding of this application. */
    private String charset;

    /** The default application locale.*/
    private Locale locale;

    /** The application log service. */
    private LogService logService;

    /**
     * The application mode:
     * [ PRODUCTION | PROFILE | DEVELOPMENT | DEBUG | TRACE ].
     */
    private int mode;

    /** The list of application page interceptor instances. */
    private List<XmlConfigService.PageInterceptorConfig> pageInterceptorConfigList
            = new ArrayList<>();

    /** The ServletContext instance. */
    private ServletContext servletContext;

    /** The application ResourceService. */
    private ResourceService resourceService;

    /** The application TemplateService. */
    private TemplateService templateService;

    /** The application TemplateService. */
    private MessagesMapService messagesMapService;

    /** Flag indicating whether Click is running on Google App Engine. */
    private boolean onGoogleAppEngine = false;

    // --------------------------------------------------------- Public Methods

    /**
     * @see org.openidentityplatform.openam.click.service.ConfigService#onInit(ServletContext)
     *
     * @param servletContext the application servlet context
     * @throws Exception if an error occurs initializing the application
     */
    public void onInit(ServletContext servletContext) throws Exception {

        Validate.notNull(servletContext, "Null servletContext parameter");

        this.servletContext = servletContext;

        onGoogleAppEngine = servletContext.getServerInfo().startsWith(GOOGLE_APP_ENGINE);

        // Set default logService early to log errors when services fail.
        logService = new ConsoleLogService();
        messagesMapService = new DefaultMessagesMapService();

        InputStream inputStream = ClickUtils.getClickConfig(servletContext);

        try {
            Document document = ClickUtils.buildDocument(inputStream, this);

            Element rootElm = document.getDocumentElement();

            // Load the log service
            loadLogService(rootElm);

            // Load the application mode and set the logger levels
            loadMode(rootElm);

            if (logService.isInfoEnabled()) {
                logService.info("***  Initializing Click " + ClickUtils.getClickVersion()
                        + " in " + getApplicationMode() + " mode  ***");

                String msg = "initialized LogService: " + logService.getClass().getName();
                getLogService().info(msg);
            }

            // Deploy click resources
            deployFiles(rootElm);

            // Load the format class
            loadFormatClass(rootElm);

            // Load the common headers
            loadHeaders(rootElm);

            // Load the pages
            loadPages(rootElm);

            // Load the error and not-found pages
            loadDefaultPages();

            // Load the charset
            loadCharset(rootElm);

            // Load the locale
            loadLocale(rootElm);

            // Load the File Upload service
            loadFileUploadService(rootElm);

            // Load the Templating service
            loadTemplateService(rootElm);

            // Load the Resource service
            loadResourceService(rootElm);

            // Load the Messages Map service
            loadMessagesMapService(rootElm);

            // Load the PageInterceptors
            loadPageInterceptors(rootElm);

        } finally {
            ClickUtils.close(inputStream);
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#onDestroy()
     */
    public void onDestroy() {
        if (getFileUploadService() != null) {
            getFileUploadService().onDestroy();
        }
        if (getTemplateService() != null) {
            getTemplateService().onDestroy();
        }
        if (getResourceService() != null) {
            getResourceService().onDestroy();
        }
        if (getMessagesMapService() != null) {
            getMessagesMapService().onDestroy();
        }
        if (getLogService() != null) {
            getLogService().onDestroy();
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return the application mode String value: &nbsp; <tt>["production",
     * "profile", "development", "debug"]</tt>.
     *
     * @return the application mode String value
     */
    public String getApplicationMode() {
        return MODE_VALUES[mode];
    }

    /**
     * @see org.apache.click.service.ConfigService#getCharset()
     *
     * @return the application character encoding
     */
    public String getCharset() {
        return charset;
    }

    /**
     * @return the FileUpload service
     * @see org.openidentityplatform.openam.click.service.ConfigService#getFileUploadService()
     */
    public org.openidentityplatform.openam.click.service.FileUploadService getFileUploadService() {
        return fileUploadService;
    }

    /**
     * @see org.apache.click.service.ConfigService#getLogService()
     *
     * @return the application log service.
     */
    public LogService getLogService() {
        return logService;
    }

    /**
     * @return the resource service
     * @see org.apache.click.service.ConfigService#getResourceService()
     */
    public org.openidentityplatform.openam.click.service.ResourceService getResourceService() {
        return resourceService;
    }

    /**
     * @see org.apache.click.service.ConfigService#getTemplateService()
     *
     * @return the template service
     */
    public TemplateService getTemplateService() {
        return templateService;
    }

    /**
     * @see org.apache.click.service.ConfigService#getMessagesMapService()
     *
     * @return the messages map service
     */
    public MessagesMapService getMessagesMapService() {
        return messagesMapService;
    }

    /**
     * @see org.apache.click.service.ConfigService#createFormat()
     *
     * @return a new format object
     */
    public Format createFormat() {
        try {
            return formatClass.newInstance();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#getLocale()
     *
     * @return the application locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @return the Page field auto binding mode { PUBLIC, ANNOTATION, NONE }
     * @see org.apache.click.service.ConfigService#getAutoBindingMode()
     */
    public ConfigService.AutoBinding getAutoBindingMode() {
        return autobinding;
    }

    /**
     * @see org.apache.click.service.ConfigService#isProductionMode()
     *
     * @return true if the application is in "production" mode
     */
    public boolean isProductionMode() {
        return (mode == PRODUCTION);
    }

    /**
     * @see org.apache.click.service.ConfigService#isProfileMode()
     *
     * @return true if the application is in "profile" mode
     */
    public boolean isProfileMode() {
        return (mode == PROFILE);
    }

    /**
     * @see org.apache.click.service.ConfigService#isJspPage(String)
     *
     * @param path the Page ".htm" path
     * @return true if JSP exists for the given ".htm" path
     */
    public boolean isJspPage(String path) {
        HtmlStringBuffer buffer = new HtmlStringBuffer();
        int index = StringUtils.lastIndexOf(path, ".");
        if (index > 0) {
            buffer.append(path.substring(0, index));
        } else {
            buffer.append(path);
        }
        buffer.append(".jsp");
        return pageByPathMap.containsKey(buffer.toString());
    }

    /**
     * Return true if the given path is a Page class template, false
     * otherwise. By default this method returns true if the path has a
     * <tt>.htm</tt> or <tt>.jsp</tt> extension.
     * <p/>
     * If you want to map alternative templates besides <tt>.htm</tt> and
     * <tt>.jsp</tt> files you can override this method and provide extra
     * checks against the given path whether it should be added as a
     * template or not.
     * <p/>
     * Below is an example showing how to allow <tt>.xml</tt> paths to
     * be recognized as Page class templates.
     *
     * <pre class="prettyprint">
     * public class MyConfigService extends XmlConfigService {
     *
     *     protected boolean isTemplate(String path) {
     *         // invoke default implementation
     *         boolean isTemplate = super.isTemplate(path);
     *
     *         if (!isTemplate) {
     *             // If path has an .xml extension, mark it as a template
     *             isTemplate = path.endsWith(".xml");
     *         }
     *         return isTemplate;
     *     }
     * } </pre>
     *
     * Here is an example <tt>web.xml</tt> showing how to configure a custom
     * ConfigService through the context parameter <tt>config-service-class</tt>.
     * We also map <tt>*.xml</tt> requests to be routed through ClickServlet:
     *
     * <pre class="prettyprint">
     * &lt;web-app xmlns="http://java.sun.com/xml/ns/j2ee"
     *   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *   xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
     *   version="2.4"&gt;
     *
     *   &lt;!-- Specify a custom ConfigService through the context param 'config-service-class' --&gt;
     *   &lt;context-param&gt;
     *     &lt;param-name&gt;config-service-class&lt;/param-name&gt;
     *     &lt;param-value&gt;com.mycorp.service.MyConfigSerivce&lt;/param-value&gt;
     *   &lt;/context-param&gt;
     *
     *   &lt;servlet&gt;
     *     &lt;servlet-name&gt;ClickServlet&lt;/servlet-name&gt;
     *     &lt;servlet-class&gt;org.apache.click.ClickServlet&lt;/servlet-class&gt;
     *     &lt;load-on-startup&gt;0&lt;/load-on-startup&gt;
     *   &lt;/servlet&gt;
     *
     *   &lt;!-- NOTE: we still map the .htm extension --&gt;
     *   &lt;servlet-mapping&gt;
     *     &lt;servlet-name&gt;ClickServlet&lt;/servlet-name&gt;
     *     &lt;url-pattern&gt;*.htm&lt;/url-pattern&gt;
     *   &lt;/servlet-mapping&gt;
     *
     *   &lt;!-- NOTE: we also map .xml extension in order to route xml requests to the ClickServlet --&gt;
     *   &lt;servlet-mapping&gt;
     *     &lt;servlet-name&gt;ClickServlet&lt;/servlet-name&gt;
     *     &lt;url-pattern&gt;*.xml&lt;/url-pattern&gt;
     *   &lt;/servlet-mapping&gt;
     *
     *   ...
     *
     * &lt;/web-app&gt; </pre>
     *
     * <b>Please note</b>: even though you can add extra template mappings by
     * overriding this method, it is still recommended to keep the default
     * <tt>.htm</tt> mapping by invoking <tt>super.isTemplate(String)</tt>.
     * The reason being that Click ships with some default templates such as
     * {@link org.apache.click.service.ConfigService#ERROR_PATH} and {@link org.apache.click.service.ConfigService#NOT_FOUND_PATH}
     * that must be mapped as <tt>.htm</tt>.
     * <p/>
     * Please see the ConfigService <a href="#config">javadoc</a> for details
     * on how to configure a custom ConfigService implementation.
     *
     * @see org.apache.click.service.ConfigService#isTemplate(String)
     *
     * @param path the path to check if it is a Page class template or not
     * @return true if the path is a Page class template, false otherwise
     */
    public boolean isTemplate(String path) {
        if (path.endsWith(".htm") || path.endsWith(".jsp")) {
            return true;
        }
        return false;
    }

    /**
     * @see org.apache.click.service.ConfigService#getPageClass(String)
     *
     * @param path the page path
     * @return the page class for the given path or null if no class is found
     */
    public Class<? extends Page> getPageClass(String path) {

        // If in production or profile mode.
        if (mode <= PROFILE) {
            XmlConfigService.PageElm page = (XmlConfigService.PageElm) pageByPathMap.get(path);
            if (page == null) {
                String jspPath = StringUtils.replace(path, ".htm", ".jsp");
                page = (XmlConfigService.PageElm) pageByPathMap.get(jspPath);
            }

            if (page != null) {
                return page.getPageClass();
            } else {
                return null;
            }

            // Else in development, debug or trace mode
        } else {

            synchronized (PAGE_LOAD_LOCK) {
                XmlConfigService.PageElm page = (XmlConfigService.PageElm) pageByPathMap.get(path);
                if (page == null) {
                    String jspPath = StringUtils.replace(path, ".htm", ".jsp");
                    page = (XmlConfigService.PageElm) pageByPathMap.get(jspPath);
                }

                if (page != null) {
                    return page.getPageClass();
                }

                Class pageClass = null;

                try {
                    URL resource = servletContext.getResource(path);
                    if (resource != null) {
                        for (int i = 0; i < pagePackages.size(); i++) {
                            String pagesPackage = pagePackages.get(i).toString();

                            pageClass = getPageClass(path, pagesPackage);

                            if (pageClass != null) {
                                page = new XmlConfigService.PageElm(path,
                                        pageClass,
                                        commonHeaders,
                                        autobinding);

                                pageByPathMap.put(page.getPath(), page);
                                addToClassMap(page);

                                if (logService.isDebugEnabled()) {
                                    String msg = path + " -> " + pageClass.getName();
                                    logService.debug(msg);
                                }

                                break;
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    //ignore
                }
                return pageClass;
            }
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#getPagePath(Class)
     *
     * @param pageClass the page class
     * @return path the page path or null if no path is found
     * @throws IllegalArgumentException if the Page Class is not configured
     * with a unique path
     */
    public String getPagePath(Class<? extends Page> pageClass) {
        Object object = pageByClassMap.get(pageClass);

        if (object instanceof XmlConfigService.PageElm) {
            XmlConfigService.PageElm page = (XmlConfigService.PageElm) object;
            return page.getPath();

        } else if (object instanceof List) {
            HtmlStringBuffer buffer = new HtmlStringBuffer();
            buffer.append("Page class resolves to multiple paths: ");
            buffer.append(pageClass.getName());
            buffer.append(" -> [");
            for (Iterator it = ((List) object).iterator(); it.hasNext();) {
                XmlConfigService.PageElm pageElm = (XmlConfigService.PageElm) it.next();
                buffer.append(pageElm.getPath());
                if (it.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append("]\nUse Context.createPage(String), or Context.getPageClass(String) instead.");
            throw new IllegalArgumentException(buffer.toString());

        } else {
            return null;
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#getPageClassList()
     *
     * @return the list of configured page classes
     */
    public List getPageClassList() {
        List classList = new ArrayList(pageByClassMap.size());

        Iterator i = pageByClassMap.keySet().iterator();
        while (i.hasNext()) {
            Class pageClass = (Class) i.next();
            classList.add(pageClass);
        }

        return classList;
    }

    /**
     * @see org.apache.click.service.ConfigService#getPageHeaders(String)
     *
     * @param path the path of the page
     * @return a Map of headers for the given page path
     */
    public Map<String, Object> getPageHeaders(String path) {
        XmlConfigService.PageElm page = (XmlConfigService.PageElm) pageByPathMap.get(path);
        if (page == null) {
            String jspPath = StringUtils.replace(path, ".htm", ".jsp");
            page = (XmlConfigService.PageElm) pageByPathMap.get(jspPath);
        }

        if (page != null) {
            return page.getHeaders();
        } else {
            return null;
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#getNotFoundPageClass()
     *
     * @return the page not found <tt>Page</tt> <tt>Class</tt>
     */
    public Class<? extends Page> getNotFoundPageClass() {
        XmlConfigService.PageElm page = (XmlConfigService.PageElm) pageByPathMap.get(NOT_FOUND_PATH);

        if (page != null) {
            return page.getPageClass();

        } else {
            return Page.class;
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#getErrorPageClass()
     *
     * @return the error handling page <tt>Page</tt> <tt>Class</tt>
     */
    public Class<? extends Page> getErrorPageClass() {
        PageElm page = (XmlConfigService.PageElm) pageByPathMap.get(ERROR_PATH);

        if (page != null) {
            return page.getPageClass();

        } else {
            return org.openidentityplatform.openam.click.util.ErrorPage.class;
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#getPageField(Class, String)
     *
     * @param pageClass the page class
     * @param fieldName the name of the field
     * @return the public field of the pageClass with the given name or null
     */
    public Field getPageField(Class<? extends Page> pageClass, String fieldName) {
        return getPageFields(pageClass).get(fieldName);
    }

    /**
     * @see org.apache.click.service.ConfigService#getPageFieldArray(Class)
     *
     * @param pageClass the page class
     * @return an array public fields for the given page class
     */
    public Field[] getPageFieldArray(Class<? extends Page> pageClass) {
        Object object = pageByClassMap.get(pageClass);

        if (object instanceof XmlConfigService.PageElm) {
            XmlConfigService.PageElm page = (XmlConfigService.PageElm) object;
            return page.getFieldArray();

        } else if (object instanceof List) {
            List list = (List) object;
            XmlConfigService.PageElm page = (XmlConfigService.PageElm) list.get(0);
            return page.getFieldArray();

        } else {
            return null;
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#getPageFields(Class)
     *
     * @param pageClass the page class
     * @return a Map of public fields for the given page class
     */
    public Map<String, Field> getPageFields(Class<? extends Page> pageClass) {
        Object object = pageByClassMap.get(pageClass);

        if (object instanceof XmlConfigService.PageElm) {
            XmlConfigService.PageElm page = (XmlConfigService.PageElm) object;
            return page.getFields();

        } else if (object instanceof List) {
            List list = (List) object;
            XmlConfigService.PageElm page = (XmlConfigService.PageElm) list.get(0);
            return page.getFields();

        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * @see org.apache.click.service.ConfigService#getPageInterceptors()
     *
     * @return the list of configured PageInterceptor instances
     */
    public List<PageInterceptor> getPageInterceptors() {

        if (pageInterceptorConfigList.isEmpty()) {
            return Collections.emptyList();
        }

        List<PageInterceptor> interceptorList =
                new ArrayList<PageInterceptor>(pageInterceptorConfigList.size());

        for (XmlConfigService.PageInterceptorConfig pageInterceptorConfig : pageInterceptorConfigList) {
            interceptorList.add(pageInterceptorConfig.getPageInterceptor());
        }

        return interceptorList;
    }

    /**
     * @see ConfigService#getServletContext()
     *
     * @return the application servlet context
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * This method resolves the click.dtd for the XML parser using the
     * classpath resource: <tt>/org/apache/click/click.dtd</tt>.
     *
     * @see EntityResolver#resolveEntity(String, String)
     *
     * @param publicId the DTD public id
     * @param systemId the DTD system id
     * @return resolved entity DTD input stream
     * @throws SAXException if an error occurs parsing the document
     * @throws IOException if an error occurs reading the document
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {

        InputStream inputStream = ClickUtils.getResourceAsStream(DTD_FILE_PATH, getClass());

        if (inputStream != null) {
            return new InputSource(inputStream);
        } else {
            throw new IOException("could not load resource: " + DTD_FILE_PATH);
        }
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Find and return the page class for the specified pagePath and
     * pagesPackage.
     * <p/>
     * For example if the pagePath is <tt>'/edit-customer.htm'</tt> and
     * package is <tt>'com.mycorp'</tt>, the matching page class will be:
     * <tt>com.mycorp.EditCustomer</tt> or <tt>com.mycorp.EditCustomerPage</tt>.
     * <p/>
     * If the page path is <tt>'/admin/add-customer.htm'</tt> and package is
     * <tt>'com.mycorp'</tt>, the matching page class will be:
     * <tt>com.mycorp.admin.AddCustomer</tt> or
     * <tt>com.mycorp.admin.AddCustomerPage</tt>.
     *
     * @param pagePath the path used for matching against a page class name
     * @param pagesPackage the package of the page class
     * @return the page class for the specified pagePath and pagesPackage
     */
    protected Class<? extends  Page> getPageClass(String pagePath, String pagesPackage) {
        // To understand this method lets walk through an example as the
        // code plays out. Imagine this method is called with the arguments:
        // pagePath='/pages/edit-customer.htm'
        // pagesPackage='org.apache.click'

        String packageName = "";
        if (StringUtils.isNotBlank(pagesPackage)) {
            // Append period after package
            // packageName = 'org.apache.click.'
            packageName = pagesPackage + ".";
        }

        String className = "";

        // Strip off extension.
        // path = '/pages/edit-customer'
        String path = pagePath.substring(0, pagePath.lastIndexOf("."));

        // If page is excluded return the excluded class
        Class excludePageClass = getExcludesPageClass(path);
        if (excludePageClass != null) {
            return excludePageClass;
        }

        // Build complete packageName.
        // packageName = 'org.apache.click.pages.'
        // className = 'edit-customer'
        if (path.indexOf("/") != -1) {
            StringTokenizer tokenizer = new StringTokenizer(path, "/");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (tokenizer.hasMoreTokens()) {
                    packageName = packageName + token + ".";
                } else {
                    className = token;
                }
            }
        } else {
            className = path;
        }

        // CamelCase className.
        // className = 'EditCustomer'
        StringTokenizer tokenizer = new StringTokenizer(className, "_-");
        className = "";
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            token = Character.toUpperCase(token.charAt(0)) + token.substring(1);
            className += token;
        }

        // className = 'org.apache.click.pages.EditCustomer'
        className = packageName + className;

        Class pageClass = null;
        try {
            // Attempt to load class.
            pageClass = ClickUtils.classForName(className);

            if (!Page.class.isAssignableFrom(pageClass)) {
                String msg = "Automapped page class " + className
                        + " is not a subclass of org.apache.click.Page";
                throw new RuntimeException(msg);
            }

        } catch (ClassNotFoundException cnfe) {

            boolean classFound = false;

            // Append "Page" to className and attempt to load class again.
            // className = 'org.apache.click.pages.EditCustomerPage'
            if (!className.endsWith("Page")) {
                String classNameWithPage = className + "Page";
                try {
                    // Attempt to load class.
                    pageClass = ClickUtils.classForName(classNameWithPage);

                    if (!Page.class.isAssignableFrom(pageClass)) {
                        String msg = "Automapped page class " + classNameWithPage
                                + " is not a subclass of org.apache.click.Page";
                        throw new RuntimeException(msg);
                    }

                    classFound = true;

                } catch (ClassNotFoundException cnfe2) {
                }
            }

            if (!classFound) {
                if (logService.isDebugEnabled()) {
                    logService.debug(pagePath + " -> CLASS NOT FOUND");
                }
                if (logService.isTraceEnabled()) {
                    logService.trace("class not found: " + className);
                }
            }
        }

        return pageClass;
    }

    /**
     * Returns true if Click resources (JavaScript, CSS, images etc) packaged
     * in jars can be deployed to the root directory of the webapp, false
     * otherwise.
     * <p/>
     * By default this method will return false in restricted environments where
     * write access to the underlying file system is disallowed. Example
     * environments where write access is not allowed include the WebLogic JEE
     * server and Google App Engine. (Note: WebLogic provides the property
     * <tt>"Archived Real Path Enabled"</tt> that controls whether web
     * applications can access the file system or not. See the Click user manual
     * for details).
     *
     * @return true if resources can be deployed, false otherwise
     */
    protected boolean isResourcesDeployable() {
        // Only deploy if writes are allowed
        if (onGoogleAppEngine) {
            // Google doesn't allow writes
            return false;
        }
        return ClickUtils.isResourcesDeployable(servletContext);
    }

    // ------------------------------------------------ Package Private Methods

    /**
     * Loads all Click Pages defined in the <tt>click.xml</tt> file, including
     * manually defined Pages, auto mapped Pages and excluded Pages.
     *
     * @param rootElm the root xml element containing the configuration
     * @throws java.lang.ClassNotFoundException if the specified Page class can
     * not be found on the classpath
     */
    void loadPages(Element rootElm) throws ClassNotFoundException {
        List pagesList = ClickUtils.getChildren(rootElm, "pages");

        if (pagesList.isEmpty()) {
            String msg = "required configuration 'pages' element missing.";
            throw new RuntimeException(msg);
        }

        List templates = getTemplateFiles();

        for (int i = 0; i < pagesList.size(); i++) {

            Element pagesElm = (Element) pagesList.get(i);

            // Determine whether to use automapping
            boolean automap = true;
            String automapStr = pagesElm.getAttribute("automapping");
            if (StringUtils.isBlank(automapStr)) {
                automapStr = "true";
            }

            if ("true".equalsIgnoreCase(automapStr)) {
                automap = true;
            } else if ("false".equalsIgnoreCase(automapStr)) {
                automap = false;
            } else {
                String msg = "Invalid pages automapping attribute: " + automapStr;
                throw new RuntimeException(msg);
            }

            // Determine whether to use autobinding.
            String autobindingStr = pagesElm.getAttribute("autobinding");
            if (StringUtils.isBlank(autobindingStr)) {
                autobinding = AutoBinding.DEFAULT;
            } else {

                if ("annotation".equalsIgnoreCase(autobindingStr)) {
                    autobinding = AutoBinding.ANNOTATION;

                } else if ("public".equalsIgnoreCase(autobindingStr)) {
                    autobinding = AutoBinding.DEFAULT;

                } else if ("default".equalsIgnoreCase(autobindingStr)) {
                    autobinding = AutoBinding.DEFAULT;

                } else if ("none".equalsIgnoreCase(autobindingStr)) {
                    autobinding = AutoBinding.NONE;

                    // Provided for backward compatibility
                } else if ("true".equalsIgnoreCase(autobindingStr)) {
                    autobinding = AutoBinding.DEFAULT;

                    // Provided for backward compatibility
                } else if ("false".equalsIgnoreCase(autobindingStr)) {
                    autobinding = AutoBinding.NONE;

                } else {
                    String msg = "Invalid pages autobinding attribute: "
                            + autobindingStr;
                    throw new RuntimeException(msg);
                }
            }

            // TODO: if autobinding is set to false an there are multiple pages how should this be handled
            // Perhaps autobinding should be moved to <click-app> and be a application wide setting?
            // However the way its implemented above is probably fine for backward compatibility
            // purposes, meaning the last defined autobinding wins

            String pagesPackage = pagesElm.getAttribute("package");
            if (StringUtils.isBlank(pagesPackage)) {
                pagesPackage = "";
            }

            pagesPackage = pagesPackage.trim();
            if (pagesPackage.endsWith(".") && pagesPackage.length() > 1) {
                pagesPackage =
                        pagesPackage.substring(0, pagesPackage.length() - 2);
            }

            // Add the pages package to the list of page packages
            pagePackages.add(pagesPackage);

            buildManualPageMapping(pagesElm, pagesPackage);

            if (automap) {
                buildAutoPageMapping(pagesElm, pagesPackage, templates);
            }
        }

        buildClassMap();
    }

    /**
     * Add manually defined Pages to the {@link #pageByPathMap}.
     *
     * @param pagesElm the xml element containing manually defined Pages
     * @param pagesPackage the pages package prefix
     *
     * @throws java.lang.ClassNotFoundException if the specified Page class can
     * not be found on the classpath
     */
    void buildManualPageMapping(Element pagesElm, String pagesPackage) throws ClassNotFoundException {

        List pageList = ClickUtils.getChildren(pagesElm, "page");

        if (!pageList.isEmpty() && logService.isDebugEnabled()) {
            logService.debug("click.xml pages:");
        }

        for (int i = 0; i < pageList.size(); i++) {
            Element pageElm = (Element) pageList.get(i);

            XmlConfigService.PageElm page =
                    new PageElm(pageElm,
                            pagesPackage,
                            commonHeaders,
                            autobinding);

            pageByPathMap.put(page.getPath(), page);

            if (logService.isDebugEnabled()) {
                String msg =
                        page.getPath() + " -> " + page.getPageClass().getName();
                logService.debug(msg);
            }
        }
    }

    /**
     * Build the {@link #pageByPathMap} by associating template files with
     * matching Java classes found on the classpath.
     * <p/>
     * This method also rebuilds the {@link #excludesList}. This list contains
     * URL paths that should not be auto-mapped.
     *
     * @param pagesElm the xml element containing the excluded URL paths
     * @param pagesPackage the pages package prefix
     * @param templates the list of templates to map to Page classes
     */
    void buildAutoPageMapping(Element pagesElm, String pagesPackage, List templates) throws ClassNotFoundException {

        // Build list of automap path page class overrides
        excludesList.clear();
        for (Iterator i = ClickUtils.getChildren(pagesElm, "excludes").iterator();
             i.hasNext();) {

            excludesList.add(new XmlConfigService.ExcludesElm((Element) i.next()));
        }

        if (logService.isDebugEnabled()) {
            logService.debug("automapped pages:");
        }

        for (int i = 0; i < templates.size(); i++) {
            String pagePath = (String) templates.get(i);

            if (!pageByPathMap.containsKey(pagePath)) {

                Class pageClass = getPageClass(pagePath, pagesPackage);

                if (pageClass != null) {
                    XmlConfigService.PageElm page =
                            new XmlConfigService.PageElm(pagePath,
                                    pageClass,
                                    commonHeaders,
                                    autobinding);

                    pageByPathMap.put(page.getPath(), page);

                    if (logService.isDebugEnabled()) {
                        String msg =
                                pagePath + " -> " + pageClass.getName();
                        logService.debug(msg);
                    }
                }
            }
        }
    }

    /**
     * Build the {@link #pageByClassMap} from the {@link #pageByPathMap} and
     * delegate to {@link #addToClassMap(org.openidentityplatform.openam.click.service.XmlConfigService.PageElm)}.
     */
    void buildClassMap() {
        // Build pages by class map
        for (Iterator i = pageByPathMap.values().iterator(); i.hasNext();) {
            XmlConfigService.PageElm page = (XmlConfigService.PageElm) i.next();
            addToClassMap(page);
        }
    }

    /**
     * Add the specified page to the {@link #pageByClassMap} where the Map's key
     * holds the Page class and value holds the {@link XmlConfigService.PageElm}.
     *
     * @param page the PageElm containing metadata about a specific page
     */
    void addToClassMap(XmlConfigService.PageElm page) {
        Object value = pageByClassMap.get(page.pageClass);
        if (value == null) {
            pageByClassMap.put(page.pageClass, page);

        } else if (value instanceof List) {
            ((List) value).add(page);

        } else if (value instanceof XmlConfigService.PageElm) {
            List list = new ArrayList();
            list.add(value);
            list.add(page);
            pageByClassMap.put(page.pageClass, list);

        } else {
            // should never occur
            throw new IllegalStateException();
        }
    }

    /**
     * Load the Page headers from the specified xml element.
     *
     * @param parentElm the element to load the headers from
     * @return the map of Page headers
     */
    static Map loadHeadersMap(Element parentElm) {
        Map headersMap = new HashMap();

        List headerList = ClickUtils.getChildren(parentElm, "header");

        for (int i = 0, size = headerList.size(); i < size; i++) {
            Element header = (Element) headerList.get(i);

            String name = header.getAttribute("name");
            String type = header.getAttribute("type");
            String propertyValue = header.getAttribute("value");

            Object value = null;

            if ("".equals(type) || "String".equalsIgnoreCase(type)) {
                value = propertyValue;
            } else if ("Integer".equalsIgnoreCase(type)) {
                value = Integer.valueOf(propertyValue);
            } else if ("Date".equalsIgnoreCase(type)) {
                value = new Date(Long.parseLong(propertyValue));
            } else {
                value = null;
                String message =
                        "Invalid property type [String|Integer|Date]: "
                                + type;
                throw new IllegalArgumentException(message);
            }

            headersMap.put(name, value);
        }

        return headersMap;
    }

    // -------------------------------------------------------- Private Methods

    private Element getResourceRootElement(String path) throws IOException {
        Document document = null;
        InputStream inputStream = null;
        try {
            inputStream = ClickUtils.getResourceAsStream(path, getClass());

            if (inputStream != null) {
                document = ClickUtils.buildDocument(inputStream, this);
            }

        } finally {
            ClickUtils.close(inputStream);
        }

        if (document != null) {
            return document.getDocumentElement();

        } else {
            return null;
        }
    }

    private void deployControls(Element rootElm) throws Exception {

        if (rootElm == null) {
            return;
        }

        Element controlsElm = ClickUtils.getChild(rootElm, "controls");

        if (controlsElm == null) {
            return;
        }

        List deployableList = ClickUtils.getChildren(controlsElm, "control");

        for (int i = 0; i < deployableList.size(); i++) {
            Element deployableElm = (Element) deployableList.get(i);

            String classname = deployableElm.getAttribute("classname");
            if (StringUtils.isBlank(classname)) {
                String msg =
                        "'control' element missing 'classname' attribute.";
                throw new RuntimeException(msg);
            }

            Class deployClass = ClickUtils.classForName(classname);
            Control control = (Control) deployClass.newInstance();

            control.onDeploy(servletContext);
        }
    }

    private void deployControlSets(Element rootElm) throws Exception {
        if (rootElm == null) {
            return;
        }

        Element controlsElm = ClickUtils.getChild(rootElm, "controls");

        if (controlsElm == null) {
            return;
        }

        List controlSets = ClickUtils.getChildren(controlsElm, "control-set");

        for (int i = 0; i < controlSets.size(); i++) {
            Element controlSet = (Element) controlSets.get(i);
            String name = controlSet.getAttribute("name");
            if (StringUtils.isBlank(name)) {
                String msg =
                        "'control-set' element missing 'name' attribute.";
                throw new RuntimeException(msg);
            }
            deployControls(getResourceRootElement("/" + name));
        }
    }

    /**
     * Deploy files from jars and Controls.
     *
     * @param rootElm the click.xml configuration DOM element
     * @throws java.lang.Exception if files cannot be deployed
     */
    private void deployFiles(Element rootElm) throws Exception {

        boolean isResourcesDeployable = isResourcesDeployable();

        if (isResourcesDeployable) {
            if (getLogService().isTraceEnabled()) {
                String deployTarget = servletContext.getRealPath("/");
                getLogService().trace("resource deploy folder: "
                        + deployTarget);
            }

            deployControls(getResourceRootElement("/click-controls.xml"));
            deployControls(getResourceRootElement("/extras-controls.xml"));
            deployControls(rootElm);
            deployControlSets(rootElm);
            deployResourcesOnClasspath();
        }

        if (!isResourcesDeployable) {

            HtmlStringBuffer buffer = new HtmlStringBuffer();
            if (onGoogleAppEngine) {
                buffer.append("Google App Engine does not support deploying");
                buffer.append(" resources to the 'click' web folder.\n");

            } else {
                buffer.append("could not deploy Click resources to the 'click'");
                buffer.append(" web folder.\nThis can occur if the call to");
                buffer.append(" ServletContext.getRealPath(\"/\") returns null, which means");
                buffer.append(" the web application cannot determine the file system path");
                buffer.append(" to deploy files to. This issue also occurs if the web");
                buffer.append(" application is not allowed to write to the file");
                buffer.append(" system.\n");
            }

            buffer.append("To resolve this issue please see the Click user-guide:");
            buffer.append(" http://click.apache.org/docs/user-guide/html/ch05s03.html#deploying-restricted-env");
            buffer.append(" \nIgnore this warning once you have settled on a");
            buffer.append(" deployment strategy");
            getLogService().warn(buffer.toString());
        }
    }

    /**
     * Deploy from the classpath all resources found under the directory
     * 'META-INF/resources/'. For backwards compatibility resources under the
     * directory 'META-INF/web/' are also deployed.
     * <p/>
     * Only jars and folders available on the classpath are scanned.
     *
     * @throws IOException if the resources cannot be deployed
     */
    private void deployResourcesOnClasspath() throws IOException {
        long startTime = System.currentTimeMillis();

        // Find all jars and directories on the classpath that contains the
        // directory "META-INF/resources/", and deploy those resources
        String resourceDirectory = "META-INF/resources";

        List<String> resources = new DeployUtils(logService).findResources(resourceDirectory).getResources();
        for (String resource : resources) {
            deployFile(resource, resourceDirectory);
        }

        // For backward compatibility, find all jars and directories on the
        // classpath that contains the directory "META-INF/web/", and deploy those
        // resources
        resourceDirectory = "META-INF/web";
        resources = new DeployUtils(logService).findResources(resourceDirectory).getResources();
        for (String resource : resources) {
            deployFile(resource, resourceDirectory);
        }

        logService.trace("deployed files from jars and folders - "
                + (System.currentTimeMillis() - startTime) + " ms");
    }

    /**
     * Deploy the specified file.
     *
     * @param file the file to deploy
     * @param prefix the file prefix that must be removed when the file is
     * deployed
     */
    private void deployFile(String file, String prefix) {
        // Only deploy resources containing the prefix
        int pathIndex = file.indexOf(prefix);
        if (pathIndex == 0) {
            pathIndex += prefix.length();

            // By default deploy to the web root dir
            String targetDir = "";

            // resourceName example -> click/table.css
            String resourceName = file.substring(pathIndex);
            int index = resourceName.lastIndexOf('/');

            if (index != -1) {
                // targetDir example -> click
                targetDir = resourceName.substring(0, index);
            }

            // Copy resources to web folder
            ClickUtils.deployFile(servletContext,
                    file,
                    targetDir);
        }
    }

    private void loadMode(Element rootElm) {
        Element modeElm = ClickUtils.getChild(rootElm, "mode");

        String modeValue = "development";

        if (modeElm != null) {
            if (StringUtils.isNotBlank(modeElm.getAttribute("value"))) {
                modeValue = modeElm.getAttribute("value");
            }
        }

        modeValue = System.getProperty("click.mode", modeValue);

        if (modeValue.equalsIgnoreCase("production")) {
            mode = PRODUCTION;
        } else if (modeValue.equalsIgnoreCase("profile")) {
            mode = PROFILE;
        } else if (modeValue.equalsIgnoreCase("development")) {
            mode = DEVELOPMENT;
        } else if (modeValue.equalsIgnoreCase("debug")) {
            mode = DEBUG;
        } else if (modeValue.equalsIgnoreCase("trace")) {
            mode = TRACE;
        } else {
            logService.error("invalid application mode: '" + modeValue
                    + "' - defaulted to '" + MODE_VALUES[DEBUG] + "'");
            mode = DEBUG;
        }

        // Set log levels
        if (logService instanceof ConsoleLogService) {
            int logLevel = ConsoleLogService.INFO_LEVEL;

            if (mode == PRODUCTION) {
                logLevel = ConsoleLogService.WARN_LEVEL;

            } else if (mode == DEVELOPMENT) {

            } else if (mode == DEBUG) {
                logLevel = ConsoleLogService.DEBUG_LEVEL;

            } else if (mode == TRACE) {
                logLevel = ConsoleLogService.TRACE_LEVEL;
            }

            ((ConsoleLogService) logService).setLevel(logLevel);
        }
    }

    private void loadDefaultPages() throws ClassNotFoundException {

        if (!pageByPathMap.containsKey(ERROR_PATH)) {
            XmlConfigService.PageElm page =
                    new XmlConfigService.PageElm("org.openidentityplatform.openam.click.util.ErrorPage", ERROR_PATH);

            pageByPathMap.put(ERROR_PATH, page);
        }

        if (!pageByPathMap.containsKey(NOT_FOUND_PATH)) {
            XmlConfigService.PageElm page =
                    new XmlConfigService.PageElm("org.openidentityplatform.openam.click.Page", NOT_FOUND_PATH);

            pageByPathMap.put(NOT_FOUND_PATH, page);
        }
    }

    private void loadHeaders(Element rootElm) {
        Element headersElm = ClickUtils.getChild(rootElm, "headers");

        if (headersElm != null) {
            commonHeaders =
                    Collections.unmodifiableMap(loadHeadersMap(headersElm));
        } else {
            commonHeaders = Collections.unmodifiableMap(DEFAULT_HEADERS);
        }
    }

    private void loadFormatClass(Element rootElm)
            throws ClassNotFoundException {

        Element formatElm = ClickUtils.getChild(rootElm, "format");

        if (formatElm != null) {
            String classname = formatElm.getAttribute("classname");

            if (classname == null) {
                String msg = "'format' element missing 'classname' attribute.";
                throw new RuntimeException(msg);
            }

            formatClass = ClickUtils.classForName(classname);

        } else {
            formatClass = org.apache.click.util.Format.class;
        }
    }

    private void loadFileUploadService(Element rootElm) throws Exception {

        Element fileUploadServiceElm = ClickUtils.getChild(rootElm, "file-upload-service");

        if (fileUploadServiceElm != null) {
            Class fileUploadServiceClass = CommonsFileUploadService.class;

            String classname = fileUploadServiceElm.getAttribute("classname");

            if (StringUtils.isNotBlank(classname)) {
                fileUploadServiceClass = ClickUtils.classForName(classname);
            }

            fileUploadService = (FileUploadService) fileUploadServiceClass.newInstance();

            Map propertyMap = loadPropertyMap(fileUploadServiceElm);

            for (Iterator i = propertyMap.keySet().iterator(); i.hasNext();) {
                String name = i.next().toString();
                String value = propertyMap.get(name).toString();

                Ognl.setValue(name, fileUploadService, value);
            }

        } else {
            //throw new RuntimeException("unsupported file upload service");
            //fileUploadService = new CommonsFileUploadService();
            fileUploadService = new FileUploadService() {
                @Override
                public void onInit(ServletContext servletContext) {

                }

                @Override
                public void onDestroy() {

                }

                @Override
                public List<FileItem> parseRequest(HttpServletRequest request) throws FileUploadException {
                    return null;
                }
            };
        }

        if (getLogService().isDebugEnabled()) {
            String msg = "initializing FileLoadService: "
                    + fileUploadService.getClass().getName();
            getLogService().debug(msg);
        }

        fileUploadService.onInit(servletContext);
    }

    private void loadLogService(Element rootElm) throws Exception {
        Element logServiceElm = ClickUtils.getChild(rootElm, "log-service");

        if (logServiceElm != null) {
            Class logServiceClass = ConsoleLogService.class;

            String classname = logServiceElm.getAttribute("classname");

            if (StringUtils.isNotBlank(classname)) {
                logServiceClass = ClickUtils.classForName(classname);
            }

            logService = (LogService) logServiceClass.newInstance();

            Map propertyMap = loadPropertyMap(logServiceElm);

            for (Iterator i = propertyMap.keySet().iterator(); i.hasNext();) {
                String name = i.next().toString();
                String value = propertyMap.get(name).toString();

                Ognl.setValue(name, logService, value);
            }
        } else {
            logService = new ConsoleLogService();
        }

        logService.onInit(getServletContext());
    }

    private void loadMessagesMapService(Element rootElm) throws Exception {
        Element messagesMapServiceElm = ClickUtils.getChild(rootElm, "messages-map-service");

        if (messagesMapServiceElm != null) {
            Class messagesMapServiceClass = DefaultMessagesMapService.class;

            String classname = messagesMapServiceElm.getAttribute("classname");

            if (StringUtils.isNotBlank(classname)) {
                messagesMapServiceClass = ClickUtils.classForName(classname);
            }

            messagesMapService = (MessagesMapService) messagesMapServiceClass.newInstance();

            Map propertyMap = loadPropertyMap(messagesMapServiceElm);

            for (Iterator i = propertyMap.keySet().iterator(); i.hasNext();) {
                String name = i.next().toString();
                String value = propertyMap.get(name).toString();

                Ognl.setValue(name, messagesMapService, value);
            }
        }

        if (getLogService().isDebugEnabled()) {
            String msg = "initializing MessagesMapService: "
                    + messagesMapService.getClass().getName();
            getLogService().debug(msg);
        }

        messagesMapService.onInit(servletContext);
    }

    private void loadPageInterceptors(Element rootElm) throws Exception {
        List<Element> interceptorList =
                ClickUtils.getChildren(rootElm, "page-interceptor");

        for (Element interceptorElm : interceptorList) {
            String classname = interceptorElm.getAttribute("classname");

            String scopeValue = interceptorElm.getAttribute("scope");
            boolean applicationScope = "application".equalsIgnoreCase(scopeValue);

            Class interceptorClass = ClickUtils.classForName(classname);

            Map propertyMap = loadPropertyMap(interceptorElm);
            List<XmlConfigService.Property> propertyList = new ArrayList<XmlConfigService.Property>();

            for (Iterator i = propertyMap.keySet().iterator(); i.hasNext();) {
                String name = i.next().toString();
                String value = propertyMap.get(name).toString();

                propertyList.add(new XmlConfigService.Property(name, value));
            }

            XmlConfigService.PageInterceptorConfig pageInterceptorConfig =
                    new XmlConfigService.PageInterceptorConfig(interceptorClass, applicationScope, propertyList);

            pageInterceptorConfigList.add(pageInterceptorConfig);
        }
    }

    private void loadResourceService(Element rootElm) throws Exception {

        Element resourceServiceElm = ClickUtils.getChild(rootElm, "resource-service");

        if (resourceServiceElm != null) {
            Class resourceServiceClass = ClickResourceService.class;

            String classname = resourceServiceElm.getAttribute("classname");

            if (StringUtils.isNotBlank(classname)) {
                resourceServiceClass = ClickUtils.classForName(classname);
            }

            resourceService = (ResourceService) resourceServiceClass.newInstance();

            Map propertyMap = loadPropertyMap(resourceServiceElm);

            for (Iterator i = propertyMap.keySet().iterator(); i.hasNext();) {
                String name = i.next().toString();
                String value = propertyMap.get(name).toString();

                Ognl.setValue(name, resourceService, value);
            }

        } else {
            resourceService = new ClickResourceService();
        }

        if (getLogService().isDebugEnabled()) {
            String msg = "initializing ResourceService: "
                    + resourceService.getClass().getName();
            getLogService().debug(msg);
        }

        resourceService.onInit(servletContext);
    }

    private void loadTemplateService(Element rootElm) throws Exception {
        Element templateServiceElm = ClickUtils.getChild(rootElm, "template-service");

        if (templateServiceElm != null) {
            Class templateServiceClass = VelocityTemplateService.class;

            String classname = templateServiceElm.getAttribute("classname");

            if (StringUtils.isNotBlank(classname)) {
                templateServiceClass = ClickUtils.classForName(classname);
            }

            templateService = (TemplateService) templateServiceClass.newInstance();

            Map propertyMap = loadPropertyMap(templateServiceElm);

            for (Iterator i = propertyMap.keySet().iterator(); i.hasNext();) {
                String name = i.next().toString();
                String value = propertyMap.get(name).toString();

                Ognl.setValue(name, templateService, value);
            }

        } else {
            templateService = new VelocityTemplateService();
        }

        if (getLogService().isDebugEnabled()) {
            String msg = "initializing TemplateService: "
                    + templateService.getClass().getName();
            getLogService().debug(msg);
        }

        templateService.onInit(servletContext);
    }

    private static Map loadPropertyMap(Element parentElm) {
        Map propertyMap = new HashMap();

        List propertyList = ClickUtils.getChildren(parentElm, "property");

        for (int i = 0, size = propertyList.size(); i < size; i++) {
            Element property = (Element) propertyList.get(i);

            String name = property.getAttribute("name");
            String value = property.getAttribute("value");

            propertyMap.put(name, value);
        }

        return propertyMap;
    }

    private void loadCharset(Element rootElm) {
        String localCharset = rootElm.getAttribute("charset");
        if (localCharset != null && localCharset.length() > 0) {
            this.charset = localCharset;
        }
    }

    private void loadLocale(Element rootElm) {
        String value = rootElm.getAttribute("locale");
        if (value != null && value.length() > 0) {
            StringTokenizer tokenizer = new StringTokenizer(value, "_");
            if (tokenizer.countTokens() == 1) {
                String language = tokenizer.nextToken();
                locale = new Locale(language);
            } else if (tokenizer.countTokens() == 2) {
                String language = tokenizer.nextToken();
                String country = tokenizer.nextToken();
                locale = new Locale(language, country);
            }
        }
    }

    /**
     * Return the list of templates within the web application.
     *
     * @return list of all templates within the web application
     */
    private List getTemplateFiles() {
        List fileList = new ArrayList();

        Set resources = servletContext.getResourcePaths("/");
        if (onGoogleAppEngine) {
            // resources could be immutable so create copy
            Set tempResources = new HashSet();

            // Load the two GAE preconfigured automapped folders
            tempResources.addAll(servletContext.getResourcePaths("/page"));
            tempResources.addAll(servletContext.getResourcePaths("/pages"));
            tempResources.addAll(resources);

            // assign copy to resources
            resources = Collections.unmodifiableSet(tempResources);
        }

        // Add all resources within web application
        for (Iterator i = resources.iterator(); i.hasNext();) {
            String resource = (String) i.next();

            if (isTemplate(resource)) {
                fileList.add(resource);

            } else if (resource.endsWith("/")) {
                if (!resource.equalsIgnoreCase("/WEB-INF/")) {
                    processDirectory(resource, fileList);
                }
            }
        }

        Collections.sort(fileList);

        return fileList;
    }

    private void processDirectory(String dirPath, List fileList) {
        Set resources = servletContext.getResourcePaths(dirPath);

        if (resources != null) {
            for (Iterator i = resources.iterator(); i.hasNext();) {
                String resource = (String) i.next();

                if (isTemplate(resource)) {
                    fileList.add(resource);

                } else if (resource.endsWith("/")) {
                    processDirectory(resource, fileList);
                }
            }
        }
    }

    private Class<? extends Page> getExcludesPageClass(String path) {
        for (int i = 0; i < excludesList.size(); i++) {
           XmlConfigService.ExcludesElm override =
                    (XmlConfigService.ExcludesElm) excludesList.get(i);

            if (override.isMatch(path)) {
                return override.getPageClass();
            }
        }

        return null;
    }

    /**
     * Return an array of bindable fields for the given page class based on
     * the binding mode.
     *
     * @param pageClass the page class
     * @param mode the binding mode
     * @return the field array of bindable fields
     */
    private static Field[] getBindablePageFields(Class pageClass, AutoBinding mode) {
        if (mode == AutoBinding.DEFAULT) {

            // Get @Bindable fields
            Map<String, Field> fieldMap = getAnnotatedBindableFields(pageClass);

            // Add public fields
            Field[] publicFields = pageClass.getFields();
            for (Field field : publicFields) {
                fieldMap.put(field.getName(), field);
            }

            // Copy the field map values into a field list
            Field[] fieldArray = new Field[fieldMap.size()];

            int i = 0;
            for (Field field : fieldMap.values()) {
                fieldArray[i++] = field;
            }

            return fieldArray;

        } else if (mode == AutoBinding.ANNOTATION) {

            Map<String, Field> fieldMap = getAnnotatedBindableFields(pageClass);

            // Copy the field map values into a field list
            Field[] fieldArray = new Field[fieldMap.size()];

            int i = 0;
            for (Field field : fieldMap.values()) {
                fieldArray[i++] = field;
            }

            return fieldArray;

        } else {
            return new Field[0];
        }
    }

    /**
     * Return the fields annotated with the Bindable annotation.
     *
     * @param pageClass the page class
     * @return the map of bindable fields
     */
    private static Map getAnnotatedBindableFields(Class pageClass) {

        List<Class> pageClassList = new ArrayList<Class>();
        pageClassList.add(pageClass);

        Class parentClass = pageClass.getSuperclass();
        while (parentClass != null) {
            // Include parent classes up to but excluding Page.class
            if (parentClass.isAssignableFrom(Page.class)) {
                break;
            }
            pageClassList.add(parentClass);
            parentClass = parentClass.getSuperclass();
        }

        // Reverse class list so parents are processed first, with the
        // actual page class fields processed last. This will enable the
        // page classes fields to override parent class fields
        Collections.reverse(pageClassList);

        Map<String, Field> fieldMap = new TreeMap<String, Field>();

        for (Class aPageClass : pageClassList) {

            for (Field field : aPageClass.getDeclaredFields()) {

                if (field.getAnnotation(Bindable.class) != null) {
                    fieldMap.put(field.getName(), field);

                    // If field is not public set accessibility true
                    if (!Modifier.isPublic(field.getModifiers())) {
                        field.setAccessible(true);
                    }
                }
            }
        }

        return fieldMap;
    }

    // ---------------------------------------------------------- Inner Classes

    /**
     * Provide an Excluded Page class.
     * <p/>
     * <b>PLEASE NOTE</b> this class is <b>not</b> for public use, and can be
     * ignored.
     */
    public static class ExcludePage extends Page {

        static final Map HEADERS = new HashMap();

        static {
            HEADERS.put("Cache-Control", "max-age=3600, public");
        }

        /**
         * @see Page#getHeaders()
         *
         * @return the map of HTTP header to be set in the HttpServletResponse
         */
        @Override
        public Map getHeaders() {
            return HEADERS;
        }
    }

    static class PageElm {

        final Map<String, Field> fields;

        final Field[] fieldArray;

        final Map headers;

        final Class<? extends Page> pageClass;

        final String path;

        public PageElm(Element element,
                       String pagesPackage,
                       Map commonHeaders,
                       AutoBinding autobinding)
                throws ClassNotFoundException {

            // Set headers
            Map aggregationMap = new HashMap(commonHeaders);
            Map pageHeaders = loadHeadersMap(element);
            aggregationMap.putAll(pageHeaders);
            headers = Collections.unmodifiableMap(aggregationMap);

            // Set path
            String pathValue = element.getAttribute("path");
            if (pathValue.charAt(0) != '/') {
                path = "/" + pathValue;
            } else {
                path = pathValue;
            }

            // Retrieve page classname
            String classname = element.getAttribute("classname");

            if (classname == null) {
                String msg = "No classname defined for page path " + path;
                throw new RuntimeException(msg);
            }

            Class tmpPageClass = null;
            String classnameFound = null;

            try {

                // First, lookup classname as provided
                tmpPageClass = ClickUtils.classForName(classname);
                classnameFound = classname;

            } catch (ClassNotFoundException cnfe) {

                if (pagesPackage.trim().length() > 0) {
                    // For backward compatibility prefix classname with package name
                    String prefixedClassname = pagesPackage + "." + classname;

                    try {
                        // CLK-704
                        // For backward compatibility, lookup classname prefixed with the package name

                        tmpPageClass =
                                ClickUtils.classForName(prefixedClassname);
                        classnameFound = prefixedClassname;

                    } catch (ClassNotFoundException cnfe2) {
                        // Throw original exception which used the given classname
                        String msg = "No class was found for the Page classname: '"
                                + classname + "'.";
                        throw new RuntimeException(msg, cnfe);
                    }
                }
            }

            pageClass = tmpPageClass;

            if (!Page.class.isAssignableFrom(pageClass)) {
                String msg = "Page class '" + classnameFound
                        + "' is not a subclass of org.apache.click.Page";
                throw new RuntimeException(msg);
            }


            fieldArray = XmlConfigService.getBindablePageFields(pageClass, autobinding);

            fields = new HashMap<String, Field>();
            for (Field field : fieldArray) {
                fields.put(field.getName(), field);
            }
        }

        private PageElm(String path,
                        Class pageClass,
                        Map commonHeaders,
                        AutoBinding mode) {

            headers = Collections.unmodifiableMap(commonHeaders);
            this.pageClass = pageClass;
            this.path = path;

            fieldArray = getBindablePageFields(pageClass, mode);

            fields = new HashMap<String, Field>();
            for (Field field : fieldArray) {
                fields.put(field.getName(), field);
            }
        }

        public PageElm(String classname, String path)
                throws ClassNotFoundException {

            this.fieldArray = new Field[0];
            this.fields = Collections.emptyMap();
            this.headers = Collections.emptyMap();
            pageClass = ClickUtils.classForName(classname);
            this.path = path;
        }

        public Field[] getFieldArray() {
            return fieldArray;
        }

        public Map<String, Field> getFields() {
            return fields;
        }

        public Map getHeaders() {
            return headers;
        }

        public Class<? extends Page> getPageClass() {
            return pageClass;
        }

        public String getPath() {
            return path;
        }
    }

    static class ExcludesElm {

        final Set<String> pathSet = new HashSet<String>();
        final Set<String> fileSet = new HashSet<String>();

        public ExcludesElm(Element element) throws ClassNotFoundException {

            String pattern = element.getAttribute("pattern");

            if (StringUtils.isNotBlank(pattern)) {
                StringTokenizer tokenizer = new StringTokenizer(pattern, ", ");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();

                    if (token.charAt(0) != '/') {
                        token = "/" + token;
                    }

                    int index = token.lastIndexOf(".");
                    if (index != -1) {
                        token = token.substring(0, index);
                        fileSet.add(token);

                    } else {
                        index = token.indexOf("*");
                        if (index != -1) {
                            token = token.substring(0, index);
                        }
                        pathSet.add(token);
                    }
                }
            }
        }

        public Class<? extends Page> getPageClass() {
            return XmlConfigService.ExcludePage.class;
        }

        public boolean isMatch(String resourcePath) {
            if (fileSet.contains(resourcePath)) {
                return true;
            }

            for (String path : pathSet) {
                if (resourcePath.startsWith(path)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String toString() {
            return getClass().getName()
                    + "[fileSet=" + fileSet + ",pathSet=" + pathSet + "]";
        }
    }

    static class PageInterceptorConfig {

        final Class<? extends PageInterceptor> interceptorClass;
        final boolean applicationScope;
        final List<XmlConfigService.Property> properties;
        PageInterceptor pageInterceptor;

        PageInterceptorConfig(Class<? extends PageInterceptor> interceptorClass,
                              boolean applicationScope,
                              List<XmlConfigService.Property> properties) {

            this.interceptorClass = interceptorClass;
            this.applicationScope = applicationScope;
            this.properties = properties;
        }

        public PageInterceptor getPageInterceptor() {
            PageInterceptor listener = null;

            // If cached interceptor not already created (application scope)
            // or is scope request then create a new interceptor
            if (pageInterceptor == null || !applicationScope) {
                try {
                    listener = interceptorClass.newInstance();

                    Map ognlContext = new HashMap();

                    for (XmlConfigService.Property property : properties) {
                        PropertyUtils.setValueOgnl(listener,
                                property.getName(),
                                property.getValue(),
                                ognlContext);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (applicationScope) {
                    pageInterceptor = listener;
                }

            } else {
                listener = pageInterceptor;
            }

            return listener;
        }
    }

    static class Property {
        final String name;
        final String value;

        Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

}
