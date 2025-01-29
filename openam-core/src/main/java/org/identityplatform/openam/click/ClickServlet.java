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
package org.identityplatform.openam.click;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import ognl.DefaultMemberAccess;
import ognl.MemberAccess;
import ognl.Ognl;
import ognl.OgnlException;
import ognl.TypeConverter;

import org.apache.click.PageInterceptor;
import org.apache.click.service.ConfigService;
import org.apache.click.service.LogService;
import org.apache.click.service.ResourceService;
import org.apache.click.service.TemplateException;
import org.apache.click.service.XmlConfigService;
import org.apache.click.service.ConfigService.AutoBinding;
import org.apache.click.util.ClickUtils;
import org.apache.click.util.ErrorPage;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.click.util.PageImports;
import org.apache.click.util.PropertyUtils;
import org.apache.click.util.RequestTypeConverter;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Provides the Click application HttpServlet.
 * <p/>
 * Generally developers will simply configure the <tt>ClickServlet</tt> and
 * will not use it directly in their code. For a Click web application to
 * function the <tt>ClickServlet</tt> must be configured in the web
 * application's <tt>/WEB-INF/web.xml</tt> file. A simple web application which
 * maps all <tt>*.htm</tt> requests to a ClickServlet is provided below.
 *
 * <pre class="codeConfig">
 * &lt;web-app&gt;
 *    &lt;servlet&gt;
 *       &lt;servlet-name&gt;<font color="blue">click-servlet</font>&lt;/servlet-name&gt;
 *       &lt;servlet-class&gt;<font color="red">org.apache.click.ClickServlet</font>&lt;/servlet-class&gt;
 *       &lt;load-on-startup&gt;<font color="red">0</font>&lt;/load-on-startup&gt;
 *    &lt;/servlet&gt;
 *    &lt;servlet-mapping&gt;
 *       &lt;servlet-name&gt;<font color="blue">click-servlet</font>&lt;/servlet-name&gt;
 *       &lt;url-pattern&gt;<font color="red">*.htm</font>&lt;/url-pattern&gt;
 *    &lt;/servlet-mapping&gt;
 * &lt;/web-app&gt; </pre>
 *
 * By default the <tt>ClickServlet</tt> will attempt to load an application
 * configuration file using the path: &nbsp; <tt>/WEB-INF/click.xml</tt>
 *
 * <h4>Servlet Mapping</h4>
 * By convention all Click page templates should have a .htm extension, and
 * the ClickServlet should be mapped to process all *.htm URL requests. With
 * this convention you have all the static HTML pages use a .html extension
 * and they will not be processed as Click pages.
 *
 * <h4>Load On Startup</h4>
 * Note you should always set <tt>load-on-startup</tt> element to be 0 so the
 * servlet is initialized when the server is started. This will prevent any
 * delay for the first client which uses the application.
 * <p/>
 * The <tt>ClickServlet</tt> performs as much work as possible at startup to
 * improve performance later on. The Click start up and caching strategy is
 * configured with the Click application mode in the "<tt>click.xml</tt>" file.
 * See the User Guide for information on how to configure the application mode.
 *
 * <h4>ConfigService</h4>
 *
 * A single application {@link ConfigService} instance is created by the ClickServlet at
 * startup. Once the ConfigService has been initialized it is stored in the
 * ServletContext using the key {@value org.apache.click.service.ConfigService#CONTEXT_NAME}.
 */
public class ClickServlet extends HttpServlet {

    // -------------------------------------------------------------- Constants

    private static final long serialVersionUID = 1L;

    /**
     * The <tt>mock page reference</tt> request attribute: key: &nbsp;
     * <tt>mock_page_reference</tt>.
     * <p/>
     * This attribute stores the each Page instance as a request attribute.
     * <p/>
     * <b>Note:</b> a page is <tt>only</tt> stored as a request attribute
     * if the {@link #MOCK_MODE_ENABLED} attribute is set.
     */
    static final String MOCK_PAGE_REFERENCE = "mock_page_reference";

    /**
     * The <tt>mock mode</tt> request attribute: key: &nbsp;
     * <tt>mock_mode_enabled</tt>.
     * <p/>
     * If this attribute is set (the value does not matter) certain features
     * will be enabled which is needed for running Click in a mock environment.
     */
    static final String MOCK_MODE_ENABLED = "mock_mode_enabled";

    /**
     * The click application configuration service classname init parameter name:
     * &nbsp; "<tt>config-service-class</tt>".
     */
    protected final static String CONFIG_SERVICE_CLASS = "config-service-class";

    /**
     * The custom TypeConverter classname as an init parameter name:
     * &nbps; "<tt>type-converter-class</tt>".
     */
    protected final static String TYPE_CONVERTER_CLASS = "type-converter-class";

    /**
     * The forwarded request marker attribute: &nbsp; "<tt>click-forward</tt>".
     */
    protected final static String CLICK_FORWARD = "click-forward";

    /**
     * The Page to forward to request attribute: &nbsp; "<tt>click-page</tt>".
     */
    protected final static String FORWARD_PAGE = "forward-page";

    // ----------------------------------------------------- Instance Variables

    /** The click application configuration service. */
    protected ConfigService configService;

    /** The application log service. */
    protected LogService logger;

    /** The OGNL member access handler. */
    protected MemberAccess memberAccess;

    /** The application resource service. */
    protected ResourceService resourceService;

    /** The request parameters OGNL type converter. */
    protected TypeConverter typeConverter;

    /** The thread local page listeners. */
    private static final ThreadLocal<List<PageInterceptor>>
            THREAD_LOCAL_INTERCEPTORS = new ThreadLocal<List<PageInterceptor>>();

    // --------------------------------------------------------- Public Methods

    /**
     * Initialize the Click servlet and the Velocity runtime.
     *
     * @see jakarta.servlet.GenericServlet#init()
     *
     * @throws ServletException if the application configuration service could
     * not be initialized
     */
    @Override
    public void init() throws ServletException {

        try {

            // Create and initialize the application config service
            configService = createConfigService(getServletContext());
            initConfigService(getServletContext());
            logger = configService.getLogService();

            if (logger.isInfoEnabled()) {
                logger.info("Click " + ClickUtils.getClickVersion()
                        + " initialized in " + configService.getApplicationMode()
                        + " mode");
            }

            resourceService = configService.getResourceService();

        } catch (Throwable e) {
            // In mock mode this exception can occur if click.xml is not
            // available.
            if (getServletContext().getAttribute(MOCK_MODE_ENABLED) != null) {
                return;
            }

            e.printStackTrace();

            String msg = "error while initializing Click servlet; throwing "
                    + "jakarta.servlet.UnavailableException";

            log(msg, e);

            throw new UnavailableException(e.toString());
        }
    }

    /**
     * @see jakarta.servlet.GenericServlet#destroy()
     */
    @Override
    public void destroy() {

        try {

            // Destroy the application config service
            destroyConfigService(getServletContext());

        } catch (Throwable e) {
            // In mock mode this exception can occur if click.xml is not
            // available.
            if (getServletContext().getAttribute(MOCK_MODE_ENABLED) != null) {
                return;
            }

            e.printStackTrace();

            String msg = "error while destroying Click servlet, throwing "
                    + "jakarta.servlet.UnavailableException";

            log(msg, e);

        } finally {
            // Dereference the application config service
            configService = null;
        }

        super.destroy();
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Handle HTTP GET requests. This method will delegate the request to
     * {@link #handleRequest(HttpServletRequest, HttpServletResponse, boolean)}.
     *
     * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
     *
     * @param request the servlet request
     * @param response the servlet response
     * @throws ServletException if click app has not been initialized
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        handleRequest(request, response, false);
    }

    /**
     * Handle HTTP POST requests. This method will delegate the request to
     * {@link #handleRequest(HttpServletRequest, HttpServletResponse, boolean)}.
     *
     * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
     *
     * @param request the servlet request
     * @param response the servlet response
     * @throws ServletException if click app has not been initialized
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        handleRequest(request, response, true);
    }

    /**
     * Handle the given servlet request and render the results to the
     * servlet response.
     * <p/>
     * If an exception occurs within this method the exception will be delegated
     * to:
     * <p/>
     * {@link #handleException(HttpServletRequest, HttpServletResponse, boolean, Throwable, Class)}
     *
     * @param request the servlet request to process
     * @param response the servlet response to render the results to
     * @param isPost determines whether the request is a POST
     * @throws IOException if resource request could not be served
     */
    protected void handleRequest(HttpServletRequest request,
                                 HttpServletResponse response, boolean isPost) throws IOException {

        // Handle requests for click resources, i.e. CSS, JS and image files
        if (resourceService.isResourceRequest(request)) {
            resourceService.renderResource(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer(200);
            buffer.append(request.getMethod());
            if (ServletFileUpload.isMultipartContent(request)) {
                buffer.append(" (multipart) ");
            } else {
                buffer.append(" ");
            }
            buffer.append(request.getRequestURL());
            logger.debug(buffer);
        }

        // Handle click page requests
        Page page = null;
        try {

            ActionEventDispatcher eventDispatcher = createActionEventDispatcher();
            // Bind ActionEventDispatcher to current thread
            ActionEventDispatcher.pushThreadLocalDispatcher(eventDispatcher);

            ControlRegistry controlRegistry = createControlRegistry();
            // Bind ControlRegistry to current thread
            ControlRegistry.pushThreadLocalRegistry(controlRegistry);

            Context context = createContext(request, response, isPost);
            // Bind context to current thread
            Context.pushThreadLocalContext(context);

            // Check for fatal error that occurred while creating Context
            Throwable error = (Throwable) request.getAttribute(Context.CONTEXT_FATAL_ERROR);

            if (error != null) {
                // Process exception through Click's exception handler.
                if (error instanceof Exception) {
                    throw (Exception) error;
                }
                // Errors are not handled by Click, let the server handle it
                if (error instanceof Error) {
                    throw (Error) error;
                } else {
                    // Throwables are not handled by Click, let the server handle it
                    throw new RuntimeException(error);
                }
            }

            page = createPage(context);

            // If no page created, then an PageInterceptor has aborted processing
            if (page == null) {
                return;
            }

            if (page.isStateful()) {
                synchronized (page) {
                    processPage(page);
                    processPageOnDestroy(page, startTime);
                    // Mark page as already destroyed for finally block
                    page = null;
                }

            } else {
                processPage(page);
            }

        } catch (Exception e) {
            Class<? extends Page> pageClass =
                    configService.getPageClass(ClickUtils.getResourcePath(request));

            handleException(request, response, isPost, e, pageClass);

        } catch (ExceptionInInitializerError eiie) {
            Throwable cause = eiie.getException();
            cause = (cause != null) ? cause : eiie;

            Class<? extends Page> pageClass =
                    configService.getPageClass(ClickUtils.getResourcePath(request));

            handleException(request, response, isPost, cause, pageClass);

        } finally {

            try {
                if (page != null) {
                    if (page.isStateful()) {
                        synchronized (page) {
                            processPageOnDestroy(page, startTime);
                        }

                    } else {
                        processPageOnDestroy(page, startTime);
                    }
                }

                for (PageInterceptor interceptor : getThreadLocalInterceptors()) {
                    interceptor.postDestroy(page);
                }

                setThreadLocalInterceptors(null);

            } finally {
                // Only clear the context when running in normal mode.
                if (request.getAttribute(MOCK_MODE_ENABLED) == null) {
                    Context.popThreadLocalContext();
                }
                ControlRegistry.popThreadLocalRegistry();
                ActionEventDispatcher.popThreadLocalDispatcher();
            }
        }
    }

    /**
     * Provides the application exception handler. The application exception
     * will be delegated to the configured error page. The default error page is
     * {@link ErrorPage} and the page template is "click/error.htm" <p/>
     * Applications which wish to provide their own customized error handling
     * must subclass ErrorPage and specify their page in the
     * "/WEB-INF/click.xml" application configuration file. For example:
     *
     * <pre class="codeConfig">
     *  &lt;page path=&quot;<span class="navy">click/error.htm</span>&quot; classname=&quot;<span class="maroon">com.mycorp.util.ErrorPage</span>&quot;/&gt;
     * </pre>
     *
     * If the ErrorPage throws an exception, it will be logged as an error and
     * then be rethrown nested inside a RuntimeException.
     *
     * @param request the servlet request with the associated error
     * @param response the servlet response
     * @param isPost boolean flag denoting the request method is "POST"
     * @param exception the error causing exception
     * @param pageClass the page class with the error
     */
    protected void handleException(HttpServletRequest request,
                                   HttpServletResponse response, boolean isPost, Throwable exception,
                                   Class<? extends Page> pageClass) {

        if (isAjaxRequest(request)) {
            handleAjaxException(request, response, isPost, exception, pageClass);
            // Exit after handling ajax exception
            return;
        }

        if (exception instanceof TemplateException) {
            TemplateException te = (TemplateException) exception;
            if (!te.isParseError()) {
                logger.error("handleException: ", exception);
            }

        } else {
            logger.error("handleException: ", exception);
        }

        ErrorPage finalizeRef = null;
        try {
            final ErrorPage errorPage = createErrorPage(pageClass, exception);

            finalizeRef = errorPage;

            errorPage.setError(exception);
            if (errorPage.getFormat() == null) {
                errorPage.setFormat(configService.createFormat());
            }
            errorPage.setHeaders(configService.getPageHeaders(ConfigService.ERROR_PATH));
            errorPage.setMode(configService.getApplicationMode());
            errorPage.setPageClass(pageClass);
            errorPage.setPath(ConfigService.ERROR_PATH);

            processPageFields(errorPage, new FieldCallback() {
                public void processField(String fieldName, Object fieldValue) {
                    if (fieldValue instanceof Control) {
                        Control control = (Control) fieldValue;
                        if (control.getName() == null) {
                            control.setName(fieldName);
                        }

                        if (!errorPage.getModel().containsKey(control.getName())) {
                            errorPage.addControl(control);
                        }
                    }
                }
            });

            if (errorPage.isStateful()) {
                synchronized (errorPage) {
                    processPage(errorPage);
                    processPageOnDestroy(errorPage, 0);
                    // Mark page as already destroyed for finally block
                    finalizeRef = null;
                }

            } else {
                processPage(errorPage);
            }

        } catch (Exception ex) {
            String message =
                    "handleError: " + ex.getClass().getName()
                            + " thrown while handling " + exception.getClass().getName()
                            + ". Now throwing RuntimeException.";

            logger.error(message, ex);

            throw new RuntimeException(ex);

        } finally {
            if (finalizeRef != null) {
                if (finalizeRef.isStateful()) {
                    synchronized (finalizeRef) {
                        processPageOnDestroy(finalizeRef, 0);
                    }

                } else {
                    processPageOnDestroy(finalizeRef, 0);
                }
            }
        }
    }

    /**
     * Process the given page invoking its "on" event callback methods
     * and directing the response.
     * <p/>
     * This method does not invoke the "onDestroy()" callback method.
     *
     * @see #processPageEvents(org.apache.click.Page, org.apache.click.Context)
     *
     * @param page the Page to process
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("deprecation")
    protected void processPage(Page page) throws Exception {

        final Context context = page.getContext();

        PageImports pageImports = createPageImports(page);
        page.setPageImports(pageImports);

        if (context.isAjaxRequest()) {
            processAjaxPageEvents(page, context);
        } else {
            processPageEvents(page, context);
        }
    }

    /**
     * Process the given page events, invoking the "on" event callback methods
     * and directing the response.
     * <p/>
     * This method does not invoke the "onDestroy()" callback method.
     *
     * @param page the Page which events to process
     * @param context the request context
     * @throws Exception if an error occurs
     */
    protected void processPageEvents(Page page, Context context) throws Exception {

        ActionEventDispatcher eventDispatcher = ActionEventDispatcher.getThreadLocalDispatcher();
        ControlRegistry controlRegistry = ControlRegistry.getThreadLocalRegistry();

        boolean errorOccurred = page instanceof ErrorPage;
        // Support direct access of click-error.htm
        if (errorOccurred) {
            ErrorPage errorPage = (ErrorPage) page;
            errorPage.setMode(configService.getApplicationMode());

            // Notify the eventDispatcher and controlRegistry of the error
            eventDispatcher.errorOccurred(errorPage.getError());
            controlRegistry.errorOccurred(errorPage.getError());
        }

        boolean continueProcessing = performOnSecurityCheck(page, context);

        ActionResult actionResult = null;
        if (continueProcessing && !errorOccurred) {
            // Handle page method
            String pageAction = context.getRequestParameter(Page.PAGE_ACTION);
            if (pageAction != null) {
                // Returned actionResult could be null
                actionResult = performPageAction(page, pageAction, context);
                continueProcessing = false;
            }
        }

        if (continueProcessing) {
            performOnInit(page, context);

            continueProcessing = performOnProcess(page, context, eventDispatcher);

            if (continueProcessing) {
                performOnPostOrGet(page, context, context.isPost());

                performOnRender(page, context);
            }
        }

        controlRegistry.processPreResponse(context);
        controlRegistry.processPreRenderHeadElements(context);
        performRender(page, context, actionResult);
    }

    /**
     * Perform the onSecurityCheck event callback for the specified page,
     * returning true if processing should continue, false otherwise.
     *
     * @param page the page to perform the security check on
     * @param context the request context
     * @return true if processing should continue, false otherwise
     */
    protected boolean performOnSecurityCheck(Page page, Context context) {
        boolean continueProcessing = page.onSecurityCheck();

        if (logger.isTraceEnabled()) {
            logger.trace("   invoked: "
                    + ClassUtils.getShortClassName(page.getClass())
                    + ".onSecurityCheck() : " + continueProcessing);
        }
        return continueProcessing;
    }

    /**
     * Perform the page action for the given page and return the action result.
     *
     * @param page the page which action to perform
     * @param pageAction the name of the page action
     * @param context the request context
     * @return the page action ActionResult instance
     */
    protected ActionResult performPageAction(Page page, String pageAction, Context context) {
        ActionResult actionResult = ClickUtils.invokeAction(page, pageAction);

        if (logger.isTraceEnabled()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer();
            String pageClassName = ClassUtils.getShortClassName(page.getClass());
            buffer.append("   invoked: ");
            buffer.append(pageClassName);
            buffer.append(".").append(pageAction).append("() : ");
            if (actionResult == null) {
                buffer.append("null (*no* ActionResult was returned by PageAction)");
            } else {
                buffer.append(ClassUtils.getShortClassName(actionResult.getClass()));
            }
            logger.trace(buffer.toString());
        }
        return actionResult;
    }

    /**
     * Perform the onInit event callback for the specified page.
     *
     * @param page the page to initialize
     * @param context the request context
     */
    protected void performOnInit(Page page, Context context) {
        page.onInit();

        if (logger.isTraceEnabled()) {
            logger.trace("   invoked: "
                    + ClassUtils.getShortClassName(page.getClass()) + ".onInit()");
        }

        if (page.hasControls()) {
            List<Control> controls = page.getControls();

            for (int i = 0, size = controls.size(); i < size; i++) {
                Control control = controls.get(i);
                control.onInit();

                if (logger.isTraceEnabled()) {
                    String controlClassName = control.getClass().getName();
                    controlClassName = controlClassName.substring(
                            controlClassName.lastIndexOf('.') + 1);
                    String msg = "   invoked: '" + control.getName() + "' "
                            + controlClassName + ".onInit()";
                    logger.trace(msg);
                }
            }
        }
    }

    /**
     * Perform onProcess event callback for the specified page, returning true
     * if processing should continue, false otherwise.
     *
     * @param page the page to process
     * @param context the request context
     * @param eventDispatcher the action event dispatcher
     * @return true if processing should continue, false otherwise
     */
    protected boolean performOnProcess(Page page, Context context,
                                       ActionEventDispatcher eventDispatcher) {

        boolean continueProcessing = true;

        // Make sure don't process a forwarded request
        if (page.hasControls() && !context.isForward()) {
            List<Control> controls = page.getControls();

            for (int i = 0, size = controls.size(); i < size; i++) {
                Control control = controls.get(i);

                int initialListenerCount = 0;
                if (logger.isTraceEnabled()) {
                    initialListenerCount = eventDispatcher.getEventSourceList().size();
                }

                boolean onProcessResult = control.onProcess();
                if (!onProcessResult) {
                    continueProcessing = false;
                }

                if (logger.isTraceEnabled()) {
                    String controlClassName = ClassUtils.getShortClassName(control.getClass());

                    String msg = "   invoked: '" + control.getName() + "' "
                            + controlClassName + ".onProcess() : " + onProcessResult;
                    logger.trace(msg);

                    if (initialListenerCount != eventDispatcher.getEventSourceList().size()) {
                        logger.trace("   listener was registered while processing control");
                    }
                }
            }

            if (continueProcessing) {
                // Fire registered action events
                continueProcessing = eventDispatcher.fireActionEvents(context);

                if (logger.isTraceEnabled()) {
                    String msg = "   invoked: Control listeners : "
                            + continueProcessing;
                    logger.trace(msg);
                }
            }
        }

        return continueProcessing;
    }

    /**
     * Perform onPost or onGet event callback for the specified page.
     *
     * @param page the page for which the onGet or onPost is performed
     * @param context the request context
     * @param isPost specifies whether the request is a post or a get
     */
    protected void performOnPostOrGet(Page page, Context context, boolean isPost) {
        if (isPost) {
            page.onPost();

            if (logger.isTraceEnabled()) {
                logger.trace("   invoked: "
                        + ClassUtils.getShortClassName(page.getClass()) + ".onPost()");
            }

        } else {
            page.onGet();

            if (logger.isTraceEnabled()) {
                logger.trace("   invoked: "
                        + ClassUtils.getShortClassName(page.getClass()) + ".onGet()");
            }
        }
    }

    /**
     * Perform onRender event callback for the specified page.
     *
     * @param page page to render
     * @param context the request context
     */
    protected void performOnRender(Page page, Context context) {
        page.onRender();

        if (logger.isTraceEnabled()) {
            logger.trace("   invoked: "
                    + ClassUtils.getShortClassName(page.getClass()) + ".onRender()");
        }

        if (page.hasControls()) {
            List<Control> controls = page.getControls();

            for (int i = 0, size = controls.size(); i < size; i++) {
                Control control = controls.get(i);
                control.onRender();

                if (logger.isTraceEnabled()) {
                    String controlClassName = control.getClass().getName();
                    controlClassName = controlClassName.substring(controlClassName.
                            lastIndexOf('.') + 1);
                    String msg = "   invoked: '" + control.getName() + "' "
                            + controlClassName + ".onRender()";
                    logger.trace(msg);
                }
            }
        }
    }

    /**
     * Performs rendering of the specified page.
     *
     * @param page page to render
     * @param context the request context
     * @throws java.lang.Exception if error occurs
     */
    protected void performRender(Page page, Context context) throws Exception {
        performRender(page, context, null);
    }

    /**
     * Performs rendering of the specified page.
     *
     * @param page page to render
     * @param context the request context
     * @param actionResult the action result
     * @throws java.lang.Exception if error occurs
     */
    protected void performRender(Page page, Context context, ActionResult actionResult)
            throws Exception {

        // Process page interceptors, and abort rendering if specified
        for (PageInterceptor interceptor : getThreadLocalInterceptors()) {
            if (!interceptor.preResponse(page)) {
                return;
            }
        }

        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        if (StringUtils.isNotBlank(page.getRedirect())) {
            String url = page.getRedirect();

            url = response.encodeRedirectURL(url);

            if (logger.isTraceEnabled()) {
                logger.debug("   redirect: " + url);

            } else if (logger.isDebugEnabled()) {
                logger.debug("redirect: " + url);
            }

            response.sendRedirect(url);

        } else if (StringUtils.isNotBlank(page.getForward())) {
            // Indicates the request is forwarded
            request.setAttribute(CLICK_FORWARD, CLICK_FORWARD);

            if (logger.isTraceEnabled()) {
                logger.debug("   forward: " + page.getForward());

            } else if (logger.isDebugEnabled()) {
                logger.debug("forward: " + page.getForward());
            }

            if (page.getForward().endsWith(".jsp")) {
                renderJSP(page);

            } else {
                RequestDispatcher dispatcher =
                        request.getRequestDispatcher(page.getForward());

                dispatcher.forward(request, response);
            }

        } else if (actionResult != null) {
            renderActionResult(actionResult, page, context);

        } else if (page.getPath() != null) {
            // Render template unless the request was a page action. This check
            // guards against the scenario where the page action returns null
            // instead of a action result
            if (context.getRequestParameter(Page.PAGE_ACTION) == null) {
                String pagePath = page.getPath();

                // Check if request is a JSP page
                if (pagePath.endsWith(".jsp") || configService.isJspPage(pagePath)) {
                    // CLK-141. Set pagePath as the forward value.
                    page.setForward(StringUtils.replace(pagePath, ".htm", ".jsp"));

                    // Indicates the request is forwarded
                    request.setAttribute(CLICK_FORWARD, CLICK_FORWARD);
                    renderJSP(page);

                } else {
                    renderTemplate(page);
                }
            }

        } else {
            if (logger.isTraceEnabled()) {
                logger.debug("   path not defined for " + page.getClass().getName());

            } else if (logger.isDebugEnabled()) {
                logger.debug("path not defined for " + page.getClass().getName());
            }
        }
    }

    /**
     * Render the Velocity template defined by the page's path.
     * <p/>
     * This method creates a Velocity Context using the Page's model Map and
     * then merges the template with the Context writing the result to the
     * HTTP servlet response.
     * <p/>
     * This method was adapted from org.apache.velocity.servlet.VelocityServlet.
     *
     * @param page the page template to merge
     * @throws Exception if an error occurs
     */
    protected void renderTemplate(Page page) throws Exception {

        long startTime = System.currentTimeMillis();

        final Map<String, Object> model = createTemplateModel(page);

        Context context = page.getContext();
        HttpServletResponse response = context.getResponse();

        response.setContentType(page.getContentType());

        Writer writer = getWriter(response);

        if (page.hasHeaders()) {
            setPageResponseHeaders(response, page.getHeaders());
        }

        configService.getTemplateService().renderTemplate(page, model, writer);

        if (!configService.isProductionMode()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer(50);
            if (logger.isTraceEnabled()) {
                buffer.append("   ");
            }
            buffer.append("renderTemplate: ");
            if (!page.getTemplate().equals(page.getPath())) {
                buffer.append(page.getPath());
                buffer.append(",");
            }
            buffer.append(page.getTemplate());
            buffer.append(" - ");
            buffer.append(System.currentTimeMillis() - startTime);
            buffer.append(" ms");
            logger.info(buffer);
        }
    }

    /**
     * Render the given page as a JSP to the response.
     *
     * @param page the page to render
     * @throws Exception if an error occurs rendering the JSP
     */
    protected void renderJSP(Page page) throws Exception {

        long startTime = System.currentTimeMillis();

        HttpServletRequest request = page.getContext().getRequest();

        HttpServletResponse response = page.getContext().getResponse();

        setRequestAttributes(page);

        RequestDispatcher dispatcher = null;

        String forward = page.getForward();

        // As "getTemplate" returns the page.getPath() by default, which is *.htm
        // we need to change to *.jsp in order to compare to the page.getForward()
        String jspTemplate = StringUtils.replace(page.getTemplate(), ".htm", ".jsp");

        if (forward.equals(jspTemplate)) {
            dispatcher = request.getRequestDispatcher(forward);

        } else {
            dispatcher = request.getRequestDispatcher(page.getTemplate());
        }

        dispatcher.forward(request, response);

        if (!configService.isProductionMode()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer(50);
            buffer.append("renderJSP: ");
            if (!page.getTemplate().equals(page.getForward())) {
                buffer.append(page.getTemplate());
                buffer.append(",");
            }
            buffer.append(page.getForward());
            buffer.append(" - ");
            buffer.append(System.currentTimeMillis() - startTime);
            buffer.append(" ms");
            logger.info(buffer);
        }
    }

    /**
     * Render the given ActionResult. If the action result is null, nothing is
     * rendered.
     *
     * @param actionResult the action result to render
     * @param page the requested page
     * @param context the request context
     */
    protected void renderActionResult(ActionResult actionResult, Page page, Context context) {
        if (actionResult == null) {
            return;
        }

        long startTime = System.currentTimeMillis();

        actionResult.render(context);

        if (!configService.isProductionMode()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer(50);
            if (logger.isTraceEnabled()) {
                buffer.append("   ");
            }

            buffer.append("renderActionResult (");
            buffer.append(actionResult.getContentType());
            buffer.append(")");
            String template = actionResult.getTemplate();
            if (template != null) {
                buffer.append(": ");
                buffer.append(template);
            }
            buffer.append(" - ");
            buffer.append(System.currentTimeMillis() - startTime);
            buffer.append(" ms");
            logger.info(buffer);
        }
    }

    /**
     * Return a new Page instance for the given request context. This method will
     * invoke {@link #initPage(String, Class, HttpServletRequest)} to create
     * the Page instance and then set the properties on the page.
     *
     * @param context the page request context
     * @return a new Page instance for the given request, or null if an
     * PageInterceptor has aborted page creation
     */
    protected Page createPage(Context context) {

        HttpServletRequest request = context.getRequest();

        // Log request parameters
        if (logger.isTraceEnabled()) {
            logger.trace("   is Ajax request: " + context.isAjaxRequest());
            logRequestParameters(request);
        }

        String path = context.getResourcePath();

        if (request.getAttribute(FORWARD_PAGE) != null) {
            Page forwardPage = (Page) request.getAttribute(FORWARD_PAGE);

            if (forwardPage.getFormat() == null) {
                forwardPage.setFormat(configService.createFormat());
            }

            request.removeAttribute(FORWARD_PAGE);

            return forwardPage;
        }

        Class<? extends Page> pageClass = configService.getPageClass(path);

        if (pageClass == null) {
            pageClass = configService.getNotFoundPageClass();
            path = ConfigService.NOT_FOUND_PATH;
        }
        // Set thread local app page listeners
        List<PageInterceptor> interceptors = configService.getPageInterceptors();
        setThreadLocalInterceptors(interceptors);

        for (PageInterceptor listener : interceptors) {
            if (!listener.preCreate(pageClass, context)) {
                return null;
            }
        }

        final Page page = initPage(path, pageClass, request);

        if (page.getFormat() == null) {
            page.setFormat(configService.createFormat());
        }

        for (PageInterceptor listener : interceptors) {
            if (!listener.postCreate(page)) {
                return null;
            }
        }

        return page;
    }

    /**
     * Process the given pages controls <tt>onDestroy</tt> methods, reset the pages
     * navigation state and process the pages <tt>onDestroy</tt> method.
     *
     * @param page the page to process
     * @param startTime the start time to log if greater than 0 and not in
     * production mode
     */
    @SuppressWarnings("deprecation")
    protected void processPageOnDestroy(Page page, long startTime) {
        Context context = page.getContext();
        if (page.hasControls()) {

            // notify callbacks of destroy event
            // TODO check that exceptions don't unnecessarily trigger preDestroy
            ControlRegistry.getThreadLocalRegistry().processPreDestroy(context);

            List<Control> controls = page.getControls();

            for (int i = 0, size = controls.size(); i < size; i++) {
                try {
                    Control control = controls.get(i);
                    control.onDestroy();

                    if (logger.isTraceEnabled()) {
                        String controlClassName = control.getClass().getName();
                        controlClassName = controlClassName.substring(controlClassName.lastIndexOf('.') + 1);
                        String msg =  "   invoked: '" + control.getName()
                                + "' " + controlClassName + ".onDestroy()";
                        logger.trace(msg);
                    }
                } catch (Throwable error) {
                    logger.error(error.toString(), error);
                }
            }
        }

        // Reset the page navigation state
        try {
            // Reset the path
            String path = context.getResourcePath();
            page.setPath(path);

            // Reset the forward
            if (configService.isJspPage(path)) {
                page.setForward(StringUtils.replace(path, ".htm", ".jsp"));
            } else {
                page.setForward((String) null);
            }

            // Reset the redirect
            page.setRedirect((String) null);

        } catch (Throwable error) {
            logger.error(error.toString(), error);
        }

        try {
            page.onDestroy();

            if (page.isStateful()) {
                context.setSessionAttribute(page.getClass().getName(), page);
            } else {
                context.removeSessionAttribute(page.getClass().getName());
            }

            if (logger.isTraceEnabled()) {
                String shortClassName = page.getClass().getName();
                shortClassName =
                        shortClassName.substring(shortClassName.lastIndexOf('.') + 1);
                logger.trace("   invoked: " + shortClassName + ".onDestroy()");
            }

            if (!configService.isProductionMode() && startTime > 0) {
                logger.info("handleRequest:  " + page.getPath() + " - "
                        + (System.currentTimeMillis() - startTime)
                        + " ms");
            }

        } catch (Throwable error) {
            logger.error(error.toString(), error);

        } finally {
            // Nullify PageImports
            page.setPageImports(null);
        }
    }

    /**
     * Initialize a new page instance using
     * {@link #newPageInstance(String, Class, HttpServletRequest)} method and
     * setting format, headers and the forward if a JSP.
     * <p/>
     * This method will also automatically register any public Page controls
     * in the page's model. When the page is created any public visible
     * page Control variables will be automatically added to the page using
     * the method {@link Page#addControl(Control)} method. If the controls name
     * is not defined it is set to the member variables name before it is added
     * to the page.
     * <p/>
     * This feature saves you from having to manually add the controls yourself.
     * If you don't want the controls automatically added, simply declare them
     * as non public variables.
     * <p/>
     * An example auto control registration is provided below. In this example
     * the Table control is automatically added to the model using the name
     * <tt>"table"</tt>, and the ActionLink controls are added using the names
     * <tt>"editDetailsLink"</tt> and <tt>"viewDetailsLink"</tt>.
     *
     * <pre class="codeJava">
     * <span class="kw">public class</span> OrderDetailsPage <span class="kw">extends</span> Page {
     *
     *     <span class="kw">public</span> Table table = <span class="kw">new</span> Table();
     *     <span class="kw">public</span> ActionLink editDetailsLink = <span class="kw">new</span> ActionLink();
     *     <span class="kw">public</span>ActionLink viewDetailsLink = <span class="kw">new</span> ActionLink();
     *
     *     <span class="kw">public</span> OrderDetailsPage() {
     *         ..
     *     }
     * } </pre>
     *
     * @param path the page path
     * @param pageClass the page class
     * @param request the page request
     * @return initialized page
     */
    protected Page initPage(String path, Class<? extends Page> pageClass,
                            HttpServletRequest request) {

        try {
            Page newPage = null;

            // Look up the page in the users session.
            HttpSession session = request.getSession(false);
            if (session != null) {
                newPage = (Page) session.getAttribute(pageClass.getName());
            }

            if (newPage == null) {
                newPage = newPageInstance(path, pageClass, request);

                if (logger.isTraceEnabled()) {
                    String shortClassName = pageClass.getName();
                    shortClassName =
                            shortClassName.substring(shortClassName.lastIndexOf('.') + 1);
                    logger.trace("   invoked: " + shortClassName + ".<<init>>");
                }
            }

            activatePageInstance(newPage);

            Map<String, Object> defaultHeaders = configService.getPageHeaders(path);
            if (newPage.hasHeaders()) {

                // Don't override existing headers
                Map pageHeaders = newPage.getHeaders();
                for (Map.Entry entry : defaultHeaders.entrySet()) {
                    if (!pageHeaders.containsKey(entry.getKey())) {
                        pageHeaders.put(entry.getKey(), entry.getValue());
                    }
                }

            } else {
                newPage.getHeaders().putAll(defaultHeaders);
            }

            newPage.setPath(path);

            // Bind to final variable to enable callback processing
            final Page page = newPage;

            if (configService.getAutoBindingMode() != AutoBinding.NONE) {

                processPageFields(newPage, new FieldCallback() {
                    public void processField(String fieldName, Object fieldValue) {
                        if (fieldValue instanceof Control) {
                            Control control = (Control) fieldValue;
                            if (control.getName() == null) {
                                control.setName(fieldName);
                            }

                            if (!page.getModel().containsKey(control.getName())) {
                                page.addControl(control);
                            }
                        }
                    }
                });

                processPageRequestParams(page);
            }

            // In mock mode add the Page instance as a request attribute.
            if (request.getAttribute(MOCK_MODE_ENABLED) != null) {
                request.setAttribute(MOCK_PAGE_REFERENCE, page);
            }

            return newPage;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Process the page binding any request parameters to any public Page
     * fields with the same name which are "primitive" types. These types
     * include string, numbers and booleans.
     * <p/>
     * Type conversion is performed using the <tt>TypeConverter</tt>
     * returned by the {@link #getTypeConverter()} method.
     *
     * @param page the page whose fields are to be processed
     * @throws OgnlException if an error occurs
     */
    protected void processPageRequestParams(Page page) throws OgnlException {

        if (configService.getPageFields(page.getClass()).isEmpty()) {
            return;
        }

        Map<?, ?> ognlContext = null;

        boolean customConverter =
                ! getTypeConverter().getClass().equals(RequestTypeConverter.class);

        HttpServletRequest request = page.getContext().getRequest();

        for (Enumeration<?> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement().toString();
            String value = request.getParameter(name);

            if (StringUtils.isNotBlank(value)) {

                Field field = configService.getPageField(page.getClass(), name);

                if (field != null) {
                    Class<?> type = field.getType();

                    if (customConverter
                            || (type.isPrimitive()
                            || String.class.isAssignableFrom(type)
                            || Number.class.isAssignableFrom(type)
                            || Boolean.class.isAssignableFrom(type))) {

                        if (ognlContext == null) {
                            ognlContext = Ognl.createDefaultContext(
                                    page, null, getTypeConverter(), getMemberAccess());
                        }

                        PropertyUtils.setValueOgnl(page, name, value, ognlContext);

                        if (logger.isTraceEnabled()) {
                            logger.trace("   auto bound variable: " + name + "=" + value);
                        }
                    }
                }
            }
        }
    }

    /**
     * Return a new Page instance for the given page path, class and request.
     * <p/>
     * The default implementation of this method simply creates new page
     * instances:
     * <pre class="codeJava">
     * <span class="kw">protected</span> Page newPageInstance(String path, Class pageClass,
     *     HttpServletRequest request) <span class="kw">throws</span> Exception {
     *
     *     <span class="kw">return</span> (Page) pageClass.newInstance();
     * } </pre>
     *
     * This method is designed to be overridden by applications providing their
     * own page creation patterns.
     * <p/>
     * A typical example of this would be with Inversion of Control (IoC)
     * frameworks such as Spring or HiveMind. For example a Spring application
     * could override this method and use a <tt>ApplicationContext</tt> to instantiate
     * new Page objects:
     * <pre class="codeJava">
     * <span class="kw">protected</span> Page newPageInstance(String path, Class pageClass,
     *     HttpServletRequest request) <span class="kw">throws</span> Exception {
     *
     *     String beanName = path.substring(0, path.indexOf(<span class="st">"."</span>));
     *
     *     <span class="kw">if</span> (applicationContext.containsBean(beanName)) {
     *         Page page = (Page) applicationContext.getBean(beanName);
     *
     *     } <span class="kw">else</span> {
     *         page = (Page) pageClass.newInstance();
     *     }
     *
     *     <span class="kw">return</span> page;
     * } </pre>
     *
     * @param path the request page path
     * @param pageClass the page Class the request is mapped to
     * @param request the page request
     * @return a new Page object
     * @throws Exception if an error occurs creating the Page
     */
    protected Page newPageInstance(String path, Class<? extends Page> pageClass,
                                   HttpServletRequest request) throws Exception {

        return pageClass.newInstance();
    }

    /**
     * Provides an extension point for ClickServlet sub classes to activate
     * stateful page which may have been deserialized.
     * <p/>
     * This method does nothing and is designed for extension.
     *
     * @param page the page instance to activate
     */
    protected void activatePageInstance(Page page) {
    }

    /**
     * Return a new VelocityContext for the given pages model and Context.
     * <p/>
     * The following values automatically added to the VelocityContext:
     * <ul>
     * <li>any public Page fields using the fields name</li>
     * <li>context - the Servlet context path, e.g. /mycorp</li>
     * <li>format - the {@link org.apache.click.util.Format} object for formatting
     * the display of objects</li>
     * <li>imports - the {@link org.apache.click.util.PageImports} object</li>
     * <li>messages - the page messages bundle</li>
     * <li>path - the page of the page template to render</li>
     * <li>request - the pages servlet request</li>
     * <li>response - the pages servlet request</li>
     * <li>session - the {@link org.apache.click.util.SessionMap} adaptor for the
     * users HttpSession</li>
     * </ul>
     *
     * @see org.apache.click.util.ClickUtils#createTemplateModel(org.apache.click.Page, org.apache.click.Context)
     *
     * @param page the page to create a VelocityContext for
     * @return a new VelocityContext
     */
    @SuppressWarnings("deprecation")
    protected Map<String, Object> createTemplateModel(final Page page) {

        if (configService.getAutoBindingMode() != AutoBinding.NONE) {

            processPageFields(page, new FieldCallback() {
                public void processField(String fieldName, Object fieldValue) {
                    if (fieldValue instanceof Control == false) {
                        page.addModel(fieldName, fieldValue);

                    } else {
                        // Add any controls not already added to model
                        Control control = (Control) fieldValue;
                        if (!page.getModel().containsKey(control.getName())) {
                            page.addControl(control);
                        }
                    }
                }
            });
        }

        final Context context = page.getContext();
        final Map<String, Object> model = ClickUtils.createTemplateModel(page, context);

        PageImports pageImports = page.getPageImports();
        pageImports.populateTemplateModel(model);

        return model;
    }

    /**
     * Set the HTTP headers in the servlet response. The Page response headers
     * are defined in {@link Page#getHeaders()}.
     *
     * @param response the response to set the headers in
     * @param headers the map of HTTP headers to set in the response
     */
    protected void setPageResponseHeaders(HttpServletResponse response,
                                          Map<String, Object> headers) {

        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String strValue = (String) value;
                if (!strValue.equalsIgnoreCase("Content-Encoding")) {
                    response.setHeader(name, strValue);
                }

            } else if (value instanceof Date) {
                long time = ((Date) value).getTime();
                response.setDateHeader(name, time);

            } else if (value instanceof Integer) {
                int intValue = (Integer) value;
                response.setIntHeader(name, intValue);

            } else if (value != null) {
                throw new IllegalStateException("Invalid Page header value type: "
                        + value.getClass() + ". Header value must of type String, Date or Integer.");
            }
        }
    }

    /**
     * Set the page model, context, format, messages and path as request
     * attributes to support JSP rendering. These request attributes include:
     * <ul>
     * <li>any public Page fields using the fields name</li>
     * <li>context - the Servlet context path, e.g. /mycorp</li>
     * <li>format - the {@link org.apache.click.util.Format} object for
     * formatting the display of objects</li>
     * <li>forward - the page forward path, if defined</li>
     * <li>imports - the {@link org.apache.click.util.PageImports} object</li>
     * <li>messages - the page messages bundle</li>
     * <li>path - the page of the page template to render</li>
     * </ul>
     *
     * @param page the page to set the request attributes on
     */
    @SuppressWarnings("deprecation")
    protected void setRequestAttributes(final Page page) {
        final HttpServletRequest request = page.getContext().getRequest();

        processPageFields(page, new FieldCallback() {
            public void processField(String fieldName, Object fieldValue) {
                if (fieldValue instanceof Control == false) {
                    request.setAttribute(fieldName, fieldValue);
                }  else {
                    // Add any controls not already added to model
                    Control control = (Control) fieldValue;
                    if (!page.getModel().containsKey(control.getName())) {
                        page.addControl(control);
                    }
                }
            }
        });

        Map<String, Object> model = page.getModel();
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            request.setAttribute(name, value);
        }

        request.setAttribute("context", request.getContextPath());
        if (model.containsKey("context")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"context\". The request attribute "
                    + "has been replaced with the request "
                    + "context path";
            logger.warn(msg);
        }

        request.setAttribute("format", page.getFormat());
        if (model.containsKey("format")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"format\". The request attribute "
                    + "has been replaced with the format object";
            logger.warn(msg);
        }

        request.setAttribute("forward", page.getForward());
        if (model.containsKey("forward")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"forward\". The request attribute "
                    + "has been replaced with the page path";
            logger.warn(msg);
        }

        request.setAttribute("path", page.getPath());
        if (model.containsKey("path")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"path\". The request attribute "
                    + "has been replaced with the page path";
            logger.warn(msg);
        }

        request.setAttribute("messages", page.getMessages());
        if (model.containsKey("messages")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"messages\". The request attribute "
                    + "has been replaced with the page messages";
            logger.warn(msg);
        }

        PageImports pageImports = page.getPageImports();
        pageImports.populateRequest(request, model);
    }

    /**
     * Return the request parameters OGNL <tt>TypeConverter</tt>. This method
     * performs a lazy load of the TypeConverter object, using the classname
     * defined in the Servlet init parameter <tt>type-converter-class</tt>,
     * if this parameter is not defined this method will return a
     * {@link RequestTypeConverter} instance.
     *
     * @return the request parameters OGNL <tt>TypeConverter</tt>
     * @throws RuntimeException if the TypeConverter instance could not be created
     */
    @SuppressWarnings("unchecked")
    protected TypeConverter getTypeConverter() throws RuntimeException {
        if (typeConverter == null) {
            Class<? extends TypeConverter> converter = RequestTypeConverter.class;

            try {
                String classname = getInitParameter(TYPE_CONVERTER_CLASS);
                if (StringUtils.isNotBlank(classname)) {
                    converter = ClickUtils.classForName(classname);
                }

                typeConverter = converter.newInstance();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return typeConverter;
    }

    /**
     * Creates and returns a new Context instance for this path, class and
     * request.
     * <p/>
     * The default implementation of this method simply creates a new Context
     * instance.
     * <p/>
     * Subclasses can override this method to provide a custom Context.
     *
     * @param request the page request
     * @param response the page response
     * @param isPost true if this is a post request, false otherwise
     * @return a Context instance
     */
    protected Context createContext(HttpServletRequest request,
                                    HttpServletResponse response, boolean isPost) {

        Context context = new Context(getServletContext(),
                getServletConfig(),
                request,
                response,
                isPost,
                this);
        return context;
    }

    /**
     * Creates and returns a new ActionEventDispatcher instance.
     *
     * @return the new ActionEventDispatcher instance
     */
    protected ActionEventDispatcher createActionEventDispatcher() {
        return new ActionEventDispatcher(configService);
    }

    /**
     * Creates and returns a new ControlRegistry instance.
     *
     * @return the new ControlRegistry instance
     */
    protected ControlRegistry createControlRegistry() {
        return new ControlRegistry(configService);
    }

    /**
     * Creates and returns a new ErrorPage instance.
     * <p/>
     * This method creates the custom page as specified in <tt>click.xml</tt>,
     * otherwise the default ErrorPage instance.
     * <p/>
     * Subclasses can override this method to provide custom ErrorPages tailored
     * for specific exceptions.
     * <p/>
     * <b>Note</b> you can safely use {@link org.apache.click.Context} in this
     * method.
     *
     * @param pageClass the page class with the error
     * @param exception the error causing exception
     * @return a new ErrorPage instance
     */
    protected ErrorPage createErrorPage(Class<? extends Page> pageClass, Throwable exception) {
        try {
            return (ErrorPage) configService.getErrorPageClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the application configuration service instance.
     *
     * @return the application configuration service instance
     */
    protected ConfigService getConfigService() {
        return configService;
    }

    /**
     * Return a new Page instance for the given path. The path must start with
     * a <tt>"/"</tt>.
     *
     * @param path the path which maps to a Page class
     * @param request the Page request
     * @return a new Page object
     * @throws IllegalArgumentException if the Page is not found
     */
    @SuppressWarnings("unchecked")
    protected <T extends Page> T createPage(String path, HttpServletRequest request) {
        Class<? extends Page> pageClass = getConfigService().getPageClass(path);

        if (pageClass == null) {
            String msg = "No Page class configured for path: " + path;
            throw new IllegalArgumentException(msg);
        }

        return (T) initPage(path, pageClass, request);
    }

    /**
     * Return a new Page instance for the page Class.
     *
     * @param pageClass the class of the Page to create
     * @param request the Page request
     * @return a new Page object
     * @throws IllegalArgumentException if the Page Class is not configured
     * with a unique path
     */
    @SuppressWarnings("unchecked")
    protected <T extends Page> T createPage(Class<T> pageClass, HttpServletRequest request) {
        String path = getConfigService().getPagePath(pageClass);

        if (path == null) {
            String msg =
                    "No path configured for Page class: " + pageClass.getName();
            throw new IllegalArgumentException(msg);
        }

        return (T) initPage(path, pageClass, request);
    }

    /**
     * Creates and returns a new PageImports instance for the specified page.
     *
     * @param page the page to create a new PageImports instance for
     * @return the new PageImports instance
     */
    protected PageImports createPageImports(Page page) {
        return new PageImports(page);
    }

    // TODO refactor Page events into its a separate Livecycle class. This will
    // take some of the responsibility off ClickServlet and remove code duplication

    /**
     * Process the given page events, invoking the "on" event callback methods
     * and directing the response.
     *
     * @param page the page which events to process
     * @param context the request context
     * @throws Exception if an error occurs
     */
    protected void processAjaxPageEvents(Page page, Context context) throws Exception {

        ActionEventDispatcher eventDispatcher = ActionEventDispatcher.getThreadLocalDispatcher();

        ControlRegistry controlRegistry = ControlRegistry.getThreadLocalRegistry();

        // TODO Ajax requests shouldn't reach this code path since errors
        // are rendered directly
        if (page instanceof ErrorPage) {
            ErrorPage errorPage = (ErrorPage) page;
            errorPage.setMode(configService.getApplicationMode());

            // Notify the dispatcher and registry of the error
            eventDispatcher.errorOccurred(errorPage.getError());
            controlRegistry.errorOccurred(errorPage.getError());
        }

        boolean continueProcessing = performOnSecurityCheck(page, context);

        ActionResult actionResult = null;
        if (continueProcessing) {

            // Handle page method
            String pageAction = context.getRequestParameter(Page.PAGE_ACTION);
            if (pageAction != null) {
                continueProcessing = false;

                // Returned action result could be null
                actionResult = performPageAction(page, pageAction, context);

                controlRegistry.processPreResponse(context);
                controlRegistry.processPreRenderHeadElements(context);

                renderActionResult(actionResult, page, context);
            }
        }

        if (continueProcessing) {
            performOnInit(page, context);

            // TODO: Ajax doesn't support forward. Is it still necessary to
            // check isForward?
            if (controlRegistry.hasAjaxTargetControls() && !context.isForward()) {

                // Perform onProcess for regsitered Ajax target controls
                processAjaxTargetControls(context, eventDispatcher, controlRegistry);

                // Fire AjaxBehaviors registered during the onProcess event
                // The target AjaxBehavior will set the eventDispatcher action
                // result instance to render
                eventDispatcher.fireAjaxBehaviors(context);

                // Ensure we execute the beforeResponse and beforeGetHeadElements
                // for Ajax requests
                controlRegistry.processPreResponse(context);
                controlRegistry.processPreRenderHeadElements(context);

                actionResult = eventDispatcher.getActionResult();

                // Render the actionResult
                renderActionResult(actionResult, page, context);

            } else {

                // If no Ajax target controls have been registered fallback to
                // the old behavior of processing and rendering the page template
                if (logger.isTraceEnabled()) {
                    String msg = "   *no* Ajax target controls have been registered."
                            + " Will process the page as a normal non Ajax request.";
                    logger.trace(msg);
                }

                continueProcessing = performOnProcess(page, context, eventDispatcher);

                if (continueProcessing) {
                    performOnPostOrGet(page, context, context.isPost());

                    performOnRender(page, context);
                }

                // If Ajax request does not target a valid page, return a 404
                // repsonse status, allowing JavaScript to display a proper message
                if (ConfigService.NOT_FOUND_PATH.equals(page.getPath())) {
                    HttpServletResponse response = context.getResponse();
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                controlRegistry.processPreResponse(context);
                controlRegistry.processPreRenderHeadElements(context);
                performRender(page, context);
            }
        } else {
            // If security check fails for an Ajax request, Click returns without
            // any rendering. It is up to the user to render an ActionResult
            // in the onSecurityCheck event
            // Note: this code path is also followed if a pageAction is invoked
        }
    }

    /**
     * Process all Ajax target controls and return true if the page should continue
     * processing, false otherwise.
     *
     * @param context the request context
     * @param eventDispatcher the event dispatcher
     * @param controlRegistry the control registry
     * @return true if the page should continue processing, false otherwise
     */
    protected boolean processAjaxTargetControls(Context context,
                                                ActionEventDispatcher eventDispatcher, ControlRegistry controlRegistry) {

        boolean continueProcessing = true;

        // Resolve the Ajax target control for this request
        Control ajaxTarget = resolveAjaxTargetControl(context, controlRegistry);

        if (ajaxTarget != null) {

            // Process the control
            if (!ajaxTarget.onProcess()) {
                continueProcessing = false;
            }

            // Log a trace if no behavior was registered after processing the control
            if (logger.isTraceEnabled()) {

                HtmlStringBuffer buffer = new HtmlStringBuffer();
                String controlClassName = ClassUtils.getShortClassName(ajaxTarget.getClass());
                buffer.append("   invoked: '");
                buffer.append(ajaxTarget.getName());
                buffer.append("' ").append(controlClassName);
                buffer.append(".onProcess() : ").append(continueProcessing);
                logger.trace(buffer.toString());

                if (!eventDispatcher.hasAjaxBehaviorSourceSet()) {
                    logger.trace("   *no* AjaxBehavior was registered while processing the control");
                }
            }
        }

        return continueProcessing;
    }

    /**
     * Provides an Ajax exception handler. Exceptions are wrapped inside a
     * <tt>div</tt> element and streamed back to the browser. The response status
     * is set to an {@link jakarta.servlet.http.HttpServletResponse#SC_INTERNAL_SERVER_ERROR HTTP 500 error}
     * which allows the JavaScript that initiated the Ajax request to handle
     * the error as appropriate.
     * <p/>
     * If Click is running in <tt>development</tt> modes the exception stackTrace
     * will be rendered, in <tt>production</tt> modes an error message is
     * rendered.
     * <p/>
     * Below is an example error response:
     *
     * <pre class="prettyprint">
     * &lt;div id='errorReport' class='errorReport'&gt;
     * The application encountered an unexpected error.
     * &lt;/div&gt;
     * </pre>
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param isPost determines whether the request is a POST
     * @param exception the error causing exception
     * @param pageClass the page class with the error
     */
    protected void handleAjaxException(HttpServletRequest request,
                                       HttpServletResponse response, boolean isPost, Throwable exception,
                                       Class<? extends Page> pageClass) {

        // If an exception occurs during an Ajax request, stream
        // the exception instead of creating an ErrorPage
        try {

            PrintWriter writer = null;

            try {
                writer = getPrintWriter(response);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                // TODO: use an ErrorReport instance instead?
                writer.write("<div id='errorReport' class='errorReport'>\n");

                if (configService.isProductionMode() || configService.isProfileMode()) {
                    writer.write("The application encountered an unexpected error.");
                } else {
                    exception.printStackTrace(writer);
                }

                writer.write("\n</div>");
            } finally {
                if (writer != null) {
                    writer.flush();
                }
            }
        } catch (Throwable error) {
            logger.error(error.getMessage(), error);
            throw new RuntimeException(error);
        }
        logger.error("handleException: ", exception);
    }

    // ------------------------------------------------ Package Private Methods

    /**
     * Return the OGNL <tt>MemberAccess</tt>. This method performs a lazy load
     * of the MemberAccess object, using a {@link DefaultMemberAccess} instance.
     *
     * @return the OGNL <tt>MemberAccess</tt>
     */
    MemberAccess getMemberAccess() {
        if (memberAccess == null) {
            memberAccess = new DefaultMemberAccess(true);
        }
        return memberAccess;
    }

    /**
     * Create a Click application ConfigService instance.
     *
     * @param servletContext the Servlet Context
     * @return a new application ConfigService instance
     * @throws Exception if an initialization error occurs
     */
    @SuppressWarnings("unchecked")
    ConfigService createConfigService(ServletContext servletContext)
            throws Exception {

        Class<? extends ConfigService> serviceClass = XmlConfigService.class;

        String classname = servletContext.getInitParameter(CONFIG_SERVICE_CLASS);
        if (StringUtils.isNotBlank(classname)) {
            serviceClass = ClickUtils.classForName(classname);
        }

        return serviceClass.newInstance();
    }

    /**
     * Initialize the Click application <tt>ConfigService</tt> instance and bind
     * it as a ServletContext attribute using the key
     * "<tt>org.apache.click.service.ConfigService</tt>".
     * <p/>
     * This method will use the configuration service class specified by the
     * {@link #CONFIG_SERVICE_CLASS} parameter, otherwise it will create a
     * {@link org.apache.click.service.XmlConfigService} instance.
     *
     * @param servletContext the servlet context to retrieve the
     * {@link #CONFIG_SERVICE_CLASS} from
     * @throws RuntimeException if the configuration service cannot be
     * initialized
     */
    void initConfigService(ServletContext servletContext) {

        if (configService != null) {
            try {

                // Note this order is very important as components need to lookup
                // the configService out of the ServletContext while the service
                // is being initialized.
                servletContext.setAttribute(ConfigService.CONTEXT_NAME, configService);

                // Initialize the ConfigService instance
                configService.onInit(servletContext);

            } catch (Exception e) {

                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Destroy the application configuration service instance and remove
     * it from the ServletContext attribute.
     *
     * @param servletContext the servlet context
     * @throws RuntimeException if the configuration service cannot be
     * destroyed
     */
    void destroyConfigService(ServletContext servletContext) {

        if (configService != null) {

            try {
                configService.onDestroy();

            } catch (Exception e) {

                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            } finally {
                servletContext.setAttribute(ConfigService.CONTEXT_NAME, null);
            }
        }
    }

    /**
     * Process all the Pages public fields using the given callback.
     *
     * @param page the page to obtain the fields from
     * @param callback the fields iterator callback
     */
    void processPageFields(Page page, FieldCallback callback) {

        Field[] fields = configService.getPageFieldArray(page.getClass());

        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];

                try {
                    Object fieldValue = field.get(page);

                    if (fieldValue != null) {
                        callback.processField(field.getName(), fieldValue);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    List<PageInterceptor> getThreadLocalInterceptors() {
        List<PageInterceptor> listeners =
                THREAD_LOCAL_INTERCEPTORS.get();

        if (listeners != null) {
            return listeners;
        } else {
            return Collections.emptyList();
        }
    }

    void setThreadLocalInterceptors(List<PageInterceptor> listeners) {
        THREAD_LOCAL_INTERCEPTORS.set(listeners);
    }

    /**
     * Retrieve a writer instance for the given context.
     *
     * @param response the servlet response
     * @return a writer instance
     * @throws IOException if an input or output exception occurred
     */
    Writer getWriter(HttpServletResponse response) throws IOException {
        try {

            return response.getWriter();

        } catch (IllegalStateException ignore) {
            // If writer cannot be retrieved fallback to OutputStream. CLK-644
            return new OutputStreamWriter(response.getOutputStream(),
                    response.getCharacterEncoding());
        }
    }

    /**
     * Return a PrintWriter instance for the given response.
     *
     * @param response the servlet response
     * @return a PrintWriter instance
     */
    PrintWriter getPrintWriter(HttpServletResponse response) throws IOException {
        Writer writer = getWriter(response);
        if (writer instanceof PrintWriter) {
            return (PrintWriter) writer;
        }
        return new PrintWriter(writer);
    }

    /**
     * Return true if this is an ajax request, false otherwise.
     *
     * @param request the servlet request
     * @return true if this is an ajax request, false otherwise
     */
    boolean isAjaxRequest(HttpServletRequest request) {
        boolean isAjaxRequest = false;
        if (Context.hasThreadLocalContext()) {
            Context context = Context.getThreadLocalContext();
            if (context.isAjaxRequest()) {
                isAjaxRequest = true;
            }
        } else {
            isAjaxRequest = ClickUtils.isAjaxRequest(request);
        }
        return isAjaxRequest;
    }

    // ---------------------------------------------------------- Inner Classes

    /**
     * Field iterator callback.
     */
    static interface FieldCallback {

        /**
         * Callback method invoked for each field.
         *
         * @param fieldName the field name
         * @param fieldValue the field value
         */
        public void processField(String fieldName, Object fieldValue);

    }

    // Private methods --------------------------------------------------------

    /**
     * Resolve and return the Ajax target control for this request or null if no
     * Ajax target was found.
     *
     * @param context the request context
     * @param controlRegistry the control registry
     * @return the target Ajax target control or null if no Ajax target was found
     */
    private Control resolveAjaxTargetControl(Context context, ControlRegistry controlRegistry) {

        Control ajaxTarget = null;

        if (logger.isTraceEnabled()) {
            logger.trace("   the following controls have been registered as potential Ajax targets:");
            if (controlRegistry.hasAjaxTargetControls()) {
                for (Control control : controlRegistry.getAjaxTargetControls()) {
                    HtmlStringBuffer buffer = new HtmlStringBuffer();
                    String controlClassName = ClassUtils.getShortClassName(control.getClass());
                    buffer.append("      ").append(controlClassName);
                    buffer.append(": name='").append(control.getName()).append("'");
                    logger.trace(buffer.toString());
                }
            } else {
                logger.trace("      *no* control has been registered");
            }
        }

        for (Control control : controlRegistry.getAjaxTargetControls()) {

            if (control.isAjaxTarget(context)) {
                ajaxTarget = control;
                // The first matching control will be processed. Multiple matching
                // controls are not supported
                break;
            }
        }

        if (logger.isTraceEnabled()) {
            if (ajaxTarget == null) {
                String msg = "   *no* target control was found for the Ajax request";
                logger.trace(msg);

            } else {
                HtmlStringBuffer buffer = new HtmlStringBuffer();
                buffer.append("   invoked: '");
                buffer.append(ajaxTarget.getName()).append("' ");
                String className = ClassUtils.getShortClassName(ajaxTarget.getClass());
                buffer.append(className);
                buffer.append(".isAjaxTarget() : true (Ajax target control found)");
                logger.trace(buffer.toString());
            }
        }

        return ajaxTarget;
    }

    /**
     * Log the request parameter names and values.
     *
     * @param request the HTTP servlet request
     */
    private void logRequestParameters(HttpServletRequest request) {

        Map<String, String[]> requestParams = new TreeMap<String, String[]>();

        Enumeration<?> e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement().toString();
            String[] values = request.getParameterValues(name);
            requestParams.put(name, values);
        }

        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();

            HtmlStringBuffer buffer = new HtmlStringBuffer(40);
            buffer.append("   request param: " + name + "=");

            if (values == null) {
                // ignore
            } else if (values.length == 1) {

                buffer.append(ClickUtils.limitLength(values[0], 40));
            } else {

                for (int i = 0; i < values.length; i++) {
                    if (i == 0) {
                        buffer.append('[');
                    } else {
                        buffer.append(", ");
                    }
                    buffer.append(ClickUtils.limitLength(values[i], 40));
                }
                buffer.append("]");
            }

            logger.trace(buffer.toString());
        }
    }
}