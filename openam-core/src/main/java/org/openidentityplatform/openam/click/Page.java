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
package org.openidentityplatform.openam.click;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openidentityplatform.openam.click.element.Element;
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.apache.click.util.Format;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;
import org.openidentityplatform.openam.click.util.PageImports;
import org.apache.commons.lang.StringUtils;

/**
 * Provides the Page request event handler class.
 * <p/>
 * The Page class plays a central role in Click applications defining how the
 * application's pages are processed and rendered. All application pages
 * must extend the base Page class, and provide a no arguments constructor.
 *
 * <h4>Page Execution Sequence</h4>
 *
 * The default Page execution path for a GET request is:
 * <ol>
 * <li class="spaced">
 *   no-args constructor invoked to create a new Page instance.
 *   At this point no dependencies have been injected into the Page, and any
 *   request information is not available. You should put any "static"
 *   page initialization code, which doesn't depend upon request information,
 *   in the constructor. This will enable subclasses to have this code
 *   automatically initialized when they are created.
 * </li>
 * <li class="spaced">
 *   {@link #format} property is set
 * </li>
 * <li class="spaced">
 *   {@link #headers} property is set
 * </li>
 * <li class="spaced">
 *   {@link #path} property is set
 * </li>
 * <li class="spaced">
 *   {@link #onSecurityCheck()} method called to check whether the page should
 *   be processed. This method should return true if the Page should continue
 *   to be processed, or false otherwise.
 * </li>
 * <li class="spaced">
 *   {@link #onInit()} method called to complete the initialization of the page
 *   after all the dependencies have been set. This is where you should put
 *   any "dynamic" page initialization code which depends upon the request or any
 *   other dependencies.
 *   <p/>
 *   Form and field controls must be fully initialized by the time this method
 *   has completed.
 * </li>
 * <li class="spaced">
 *   ClickServlet processes all the page {@link #controls}
 *   calling their {@link org.apache.click.Control#onProcess()} method. If any of these
 *   controls return false, continued control and page processing will be aborted.
 * </li>
 * <li class="spaced">
 *   {@link #onGet()} method called for any additional GET related processing.
 *   <p/>
 *   Form and field controls should <b>NOT</b> be created or initialized at this
 *   point as the control processing stage has already been completed.
 * </li>
 * <li class="spaced">
 *   {@link #onRender()} method called for any pre-render processing. This
 *   method is often use to perform database queries to load information for
 *   rendering tables.
 *   <p/>
 *   Form and field controls should <b>NOT</b> be created or initialized at this
 *   point as the control processing stage has already been completed.
 * </li>
 * <li class="spaced">
 *   ClickServlet renders the page merging the {@link #model} with the
 *   Velocity template defined by the {@link #getTemplate()} property.
 * </li>
 * <li class="spaced">
 *   {@link #onDestroy()} method called to clean up any resources. This method
 *   is guaranteed to be called, even if an exception occurs. You can use
 *   this method to close resources like database connections or Hibernate
 *   sessions.
 * </li>
 * </ol>
 *
 * For POST requests the default execution path is identical, except the
 * {@link #onPost()} method is called instead of {@link #onGet()}. The POST
 * request page execution sequence is illustrated below:
 * <p/>
 * <img src="post-sequence-diagram.png"/>
 *
 * <p/>
 * A good way to see the page event execution order is to view the log when
 * the application mode is set to <tt>trace</tt>:
 *
 * <pre class="codeConfig" style="padding:1em;background-color:#f0f0f0;">
 * [Click] [debug] GET http://localhost:8080/quickstart/home.htm
 * [Click] [trace]    invoked: HomePage.&lt;&lt;init&gt;&gt;
 * [Click] [trace]    invoked: HomePage.onSecurityCheck() : true
 * [Click] [trace]    invoked: HomePage.onInit()
 * [Click] [trace]    invoked: HomePage.onGet()
 * [Click] [trace]    invoked: HomePage.onRender()
 * [Click] [info ]    renderTemplate: /home.htm - 6 ms
 * [Click] [trace]    invoked: HomePage.onDestroy()
 * [Click] [info ] handleRequest:  /home.htm - 24 ms  </pre>
 *
 * <h4>Rendering Pages</h4>
 *
 * When a Velocity template is rendered the ClickServlet uses Pages:
 * <ul>
 * <li>{@link #getTemplate()} to find the Velocity template.</li>
 * <li>{@link #model} to populate the Velocity Context</li>
 * <li>{@link #format} to add to the Velocity Context</li>
 * <li>{@link #getContentType()} to set as the HttpServletResponse content type</li>
 * <li>{@link #headers} to set as the HttpServletResponse headers</li>
 * </ul>
 *
 * These Page properties are also used when rendering JSP pages.
 */
public class Page implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The global page messages bundle name: &nbsp; <tt>click-page</tt>.
     */
    public static final String PAGE_MESSAGES = "click-page";

    /**
     * The Page action request parameter: &nbsp; "<tt>pageAction</tt>".
     */
    public final static String PAGE_ACTION = "pageAction";

    // Instance Variables -----------------------------------------------------

    /** The list of page controls. */
    protected List<Control> controls;

    /**
     * The list of page HTML HEAD elements including: Javascript imports,
     * Css imports, inline Javascript and inline Css.
     */
    protected List<Element> headElements;

    /** The Velocity template formatter object. */
    protected Format format;

    /** The forward path. */
    protected String forward;

    /** The HTTP response headers. */
    protected Map<String, Object> headers;

    /** The map of localized page resource messages. **/
    protected transient Map<String, String> messages;

    /**
     * The page model. For Velocity templates the model is used to populate the
     * Velocity context. For JSP pages the model values are set as named
     * request attributes.
     */
    protected Map<String, Object> model = new HashMap<String, Object>();

    /** The Page header imports. */
    protected transient PageImports pageImports;

    /** The path of the page template to render. */
    protected String path;

    /** The redirect path. */
    protected String redirect;

    /**
     * The page is stateful and should be saved to the users HttpSession
     * between requests, default value is false.
     *
     * @deprecated stateful pages are not supported anymore, use stateful
     * Controls instead
     */
    protected boolean stateful;

    /** The path of the page border template to render.*/
    protected String template;

    /**
     * Indicates whether Control head elements should be included in the
     * page template, default value is true.
     */
    protected boolean includeControlHeadElements = true;

    // Event Handlers ---------------------------------------------------------

    /**
     * The on Security Check event handler. This event handler is invoked after
     * the pages constructor has been called and all the page properties have
     * been set.
     * <p/>
     * Security check provides the Page an opportunity to check the users
     * security credentials before processing the Page.
     * <p/>
     * If security check returns true the Page is processed as
     * normal. If the method returns then no other event handlers are invoked
     * (except <tt>onDestroy()</tt> and no page controls are processed.
     * <p/>
     * If the method returns false, the forward or redirect property should be
     * set to send the request to another Page.
     * <p/>
     * By default this method returns true, subclass may override this method
     * to provide their security authorization/authentication mechanism.
     *
     * @return true by default, subclasses may override this method
     */
    public boolean onSecurityCheck() {
        return true;
    }

    /**
     * The on Initialization event handler. This event handler is invoked after
     * the {@link #onInit()} method has been called.
     * <p/>
     * Subclasses should place any initialization code which has dependencies
     * on the context or other properties in this method. Generally light
     * weight initialization code should be placed in the Pages constructor.
     * <p/>
     * Time consuming operations such as fetching the results of a database
     * query should not be placed in this method. These operations should be
     * performed in the {@link #onRender()}, {@link #onGet()} or
     * {@link #onPost()} methods so that other event handlers may take
     * alternative execution paths without performing these expensive operations.
     * <p/>
     * <b>Please Note</b> however the qualifier for the previous statement is
     * that all form and field controls must be fully initialized before they
     * are processed, which is after the <tt>onInit()</tt> method has
     * completed. After this point their <tt>onProcess()</tt> methods will be
     * invoked by the <tt>ClickServlet</tt>.
     * <p/>
     * Select controls in particular must have their option list values populated
     * before the form is processed otherwise field validations cannot be performed.
     * <p/>
     * For initializing page controls the best practice is to place all the
     * control creation code in the pages constructor, and only place any
     * initialization code in the <tt>onInit()</tt> method which has an external
     * dependency to the context or some other object. By following this practice
     * it is easy to see what code is "design time" initialization code and what
     * is "runtime initialization code".
     * <p/>
     * When subclassing pages which also use the <tt>onInit()</tt> method is
     * is critical you call the <tt>super.onInit()</tt> method first, for
     * example:
     * <pre class="javaCode">
     * <span class="kw">public void</span> onInit() {
     *     <span class="kw">super</span>.onInit();
     *
     *     // Initialization code
     *     ..
     * } </pre>
     */
    public void onInit() {
    }

    /**
     * The on Get request event handler. This event handler is invoked if the
     * HTTP request method is "GET".
     * <p/>
     * The event handler is invoked after {@link #onSecurityCheck()} has been
     * called and all the Page {@link #controls} have been processed. If either
     * the security check or one of the controls cancels continued event
     * processing the <tt>onGet()</tt> method will not be invoked.
     *
     * <h4>Important Note</h4>
     *
     * Form and field controls should <b>NOT</b> be created
     * or initialized at this point as the control processing stage has already
     * been completed. Select option list values should also be populated
     * before the control processing stage is performed so that they can
     * validate the submitted values.
     */
    public void onGet() {
    }

    /**
     * The on Post request event handler. This event handler is invoked if the
     * HTTP request method is "POST".
     * <p/>
     * The event handler is invoked after {@link #onSecurityCheck()} has been
     * called and all the Page {@link #controls} have been processed. If either
     * the security check or one of the controls cancels continued event
     * processing the <tt>onPost()</tt> method will not be invoked.
     *
     * <h4>Important Note</h4>
     *
     * Form and field controls should <b>NOT</b> be created
     * or initialized at this point as the control processing stage has already
     * been completed. Select option list values should also be populated
     * before the control processing stage is performed so that they can
     * validate the submitted values.
     */
    public void onPost() {
    }

    /**
     * The on render event handler. This event handler is invoked prior to the
     * page being rendered.
     * <p/>
     * This method will not be invoked if either the security check or one of
     * the controls cancels continued event processing.
     * <p/>
     * The on render method is typically used to populate tables performing some
     * database intensive operation. By putting the intensive operations in the
     * on render method they will not be performed if the user navigates away
     * to a different page.
     * <p/>
     * If you have code which you are using in both the <tt>onGet()</tt> and
     * <tt>onPost()</tt> methods, use the <tt>onRender()</tt> method instead.
     *
     * <h4>Important Note</h4>
     *
     * Form and field controls should <b>NOT</b> be created
     * or initialized at this point as the control processing stage has already
     * been completed. Select option list values should also be populated
     * before the control processing stage is performed so that they can
     * validate the submitted values.
     */
    public void onRender() {
    }

    /**
     * The on Destroy request event handler. Subclasses may override this method
     * to add any resource clean up code.
     * <p/>
     * This method is guaranteed to be called before the Page object reference
     * goes out of scope and is available for garbage collection.
     */
    public void onDestroy() {
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Add the control to the page. The control will be added to the page model
     * using the control name as the key. The Controls parent property will
     * also be set to the page instance.
     * <p/>
     * <b>Please note</b>: if the page contains a control with the same name as
     * the given control, that control will be replaced by the given control.
     * If a control has no name defined it cannot be replaced.
     *
     * @param control the control to add to the page
     * @throws IllegalArgumentException if the control is null or if the name
     * of the control is not defined
     */
    public void addControl(Control control) {
        if (control == null) {
            throw new IllegalArgumentException("Null control parameter");
        }
        if (StringUtils.isBlank(control.getName())) {
            throw new IllegalArgumentException("Control name not defined: "
                    + control.getClass());
        }

        // Check if page already contains a named value
        Object currentValue = getModel().get(control.getName());
        if (currentValue != null && currentValue instanceof Control) {
            Control currentControl = (Control) currentValue;
            replaceControl(currentControl, control);
            return;
        }

        // Note: set parent first as setParent might veto further processing
        control.setParent(this);

        getControls().add(control);
        addModel(control.getName(), control);
    }

    /**
     * Remove the control from the page. The control will be removed from the
     * pages model and the control parent property will be set to null.
     *
     * @param control the control to remove
     * @throws IllegalArgumentException if the control is null, or if the name
     *      of the control is not defined
     */
    public void removeControl(Control control) {
        if (control == null) {
            throw new IllegalArgumentException("Null control parameter");
        }
        if (StringUtils.isBlank(control.getName())) {
            throw new IllegalArgumentException("Control name not defined");
        }

        getControls().remove(control);
        getModel().remove(control.getName());

        control.setParent(null);
    }

    /**
     * Return the list of page Controls.
     *
     * @return the list of page Controls
     */
    public List<Control> getControls() {
        if (controls == null) {
            controls = new ArrayList<Control>();
        }
        return controls;
    }

    /**
     * Return true if the page has any controls defined.
     *
     * @return true if the page has any controls defined
     */
    public boolean hasControls() {
        return (controls != null) && !controls.isEmpty();
    }

    /**
     * Return the request context of the page.
     *
     * @return the request context of the page
     */
    public Context getContext() {
        return Context.getThreadLocalContext();
    }

    /**
     * Return the HTTP response content type. By default this method returns
     * <tt>"text/html"</tt>.
     * <p/>
     * If the request specifies a character encoding via
     * If {@link jakarta.servlet.ServletRequest#getCharacterEncoding()}
     * then this method will return <tt>"text/html; charset=encoding"</tt>.
     * <p/>
     * The ClickServlet uses the pages content type for setting the
     * HttpServletResponse content type.
     *
     * @return the HTTP response content type
     */
    public String getContentType() {
        String charset = getContext().getRequest().getCharacterEncoding();

        if (charset == null) {
            return "text/html";

        } else {
            return "text/html; charset=" + charset;
        }
    }

    /**
     * Return the Velocity template formatter object.
     * <p/>
     * The ClickServlet adds the format object to the Velocity context using
     * the key <tt>"format"</tt> so that it can be used in the page template.
     *
     * @return the Velocity template formatter object
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Set the Velocity template formatter object.
     *
     * @param value the Velocity template formatter object.
     */
    public void setFormat(Format value) {
        format = value;
    }

    /**
     * Return the path to forward the request to.
     * <p/>
     * If the {@link #forward} property is not null it will be used to forward
     * the request to in preference to rendering the template defined by the
     * {@link #path} property. The request is forwarded using the
     * RequestDispatcher.
     * <p/>
     * See also {@link #getPath()}, {@link #getRedirect()}
     *
     * @return the path to forward the request to
     */
    public String getForward() {
        return forward;
    }

    /**
     * Set the path to forward the request to.
     * <p/>
     * If the {@link #forward} property is not null it will be used to forward
     * the request to in preference to rendering the template defined by the
     * {@link #path} property. The request is forwarded using the Servlet
     * RequestDispatcher.
     * <p/>
     * If forward paths start with a <span class="wr">"/"</span>
     * character the forward path is
     * relative to web applications root context, otherwise the path is
     * relative to the requests current location.
     * <p/>
     * For example given a web application deployed to context <tt>mycorp</tt>
     * with the pages:
     * <pre class="codeConfig" style="color:navy">
     *  /index.htm
     *  /customer/search.htm
     *  /customer/details.htm
     *  /customer/management/add-customer.htm </pre>
     *
     * To forward to the customer <tt class="wr">search.htm</tt> page from
     * the web app root you could set forward as
     * <tt>setForward(<span class="navy">"/customer/search.htm"</span>)</tt>
     * or <tt>setForward(<span class="navy">"customer/search.htm"</span>)</tt>.
     * <p/>
     * If a user was currently viewing the <tt class="wr">add-customer.htm</tt>
     * to forward to customer <span class="wr">details.htm</span> you could
     * set forward as
     * <tt>setForward(<span class="navy">"/customer/details.htm"</span>)</tt>
     * or <tt>setForward(<span class="navy">"../details.htm"</span>)</tt>.
     * <p/>
     * See also {@link #setPath(String)}, {@link #setRedirect(String)}
     *
     * @param value the path to forward the request to
     */
    public void setForward(String value) {
        forward = value;
    }

    /**
     * The Page instance to forward the request to. The given Page object
     * must have a valid {@link #path} defined, as the {@link #path} specifies
     * the location to forward to.
     *
     * @see #setForward(java.lang.String)
     *
     * @param page the Page object to forward the request to.
     */
    public void setForward(Page page) {
        if (page == null) {
            throw new IllegalArgumentException("Null page parameter");
        }
        if (page.getPath() == null) {
            throw new IllegalArgumentException("Page has no path defined");
        }
        setForward(page.getPath());
        getContext().setRequestAttribute(ClickServlet.FORWARD_PAGE, page);
    }

    /**
     * Set the request to forward to the given page class.
     *
     * @see #setForward(java.lang.String)
     *
     * @param pageClass the class of the Page to forward the request to
     * @throws IllegalArgumentException if the Page Class is not configured
     * with a unique path
     */
    public void setForward(Class<? extends Page> pageClass) {
        String target = getContext().getPagePath(pageClass);

        // If page class maps to a jsp, convert to htm which allows ClickServlet
        // to process the redirect
        if (target != null && target.endsWith(".jsp")) {
            target = StringUtils.replaceOnce(target, ".jsp", ".htm");
        }
        setForward(target);
    }

    /**
     * Return the map of HTTP header to be set in the HttpServletResponse.
     *
     * @return the map of HTTP header to be set in the HttpServletResponse
     */
    public Map<String, Object> getHeaders() {
        if (headers == null) {
            headers = new HashMap<String, Object>();
        }
        return headers;
    }

    /**
     * Return true if the page has headers, false otherwise.
     *
     * @return true if the page has headers, false otherwise
     */
    public boolean hasHeaders() {
        return headers != null && !headers.isEmpty();
    }

    /**
     * Set the named header with the given value. Value can be either a String,
     * Date or Integer.
     *
     * @param name the name of the header
     * @param value the value of the header, either a String, Date or Integer
     */
    public void setHeader(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Null header name parameter");
        }

        getHeaders().put(name, value);
    }

    /**
     * Set the map of HTTP header to be set in the HttpServletResponse.
     *
     * @param value the map of HTTP header to be set in the HttpServletResponse
     */
    public void setHeaders(Map<String, Object> value) {
        headers = value;
    }

    /**
     * @deprecated use the new {@link #getHeadElements()} instead
     *
     * @return the HTML includes statements for the control stylesheet and
     * JavaScript files
     */
    public final String getHtmlImports() {
        throw new UnsupportedOperationException("Use getHeadElements instead");
    }

    /**
     * Return the list of HEAD {@link org.apache.click.element.Element elements}
     * to be included in the page. Example HEAD elements include
     * {@link org.apache.click.element.JsImport JsImport},
     * {@link org.apache.click.element.JsScript JsScript},
     * {@link org.apache.click.element.CssImport CssImport} and
     * {@link org.apache.click.element.CssStyle CssStyle}.
     * <p/>
     * Pages can contribute their own list of HEAD elements by overriding
     * this method.
     * <p/>
     * The recommended approach when overriding this method is to use
     * <tt>lazy loading</tt> to ensure the HEAD elements are only added
     * <tt>once</tt> and when <tt>needed</tt>. For example:
     *
     * <pre class="prettyprint">
     * public MyPage extends Page {
     *
     *     public List getHeadElements() {
     *         // Use lazy loading to ensure the JS is only added the
     *         // first time this method is called.
     *         if (headElements == null) {
     *             // Get the head elements from the super implementation
     *             headElements = super.getHeadElements();
     *
     *             // Include the page's external Javascript resource
     *             JsImport jsImport = new JsImport("/mycorp/js/mypage.js");
     *             headElements.add(jsImport);
     *
     *             // Include the page's external Css resource
     *             CssImport cssImport = new CssImport("/mycorp/js/mypage.css");
     *             headElements.add(cssImport);
     *         }
     *         return headElements;
     *     }
     * } </pre>
     *
     * Alternatively one can add the HEAD elements in the Page constructor:
     *
     * <pre class="prettyprint">
     * public MyPage extends Page {
     *
     *     public MyPage() {
     *         JsImport jsImport = new JsImport("/mycorp/js/mypage.js");
     *         getHeadElements().add(jsImport);
     *
     *         CssImport cssImport = new CssImport("/mycorp/js/mypage.css");
     *         getHeadElements().add(cssImport);
     *     }
     * } </pre>
     *
     * One can also add HEAD elements from event handler methods such as
     * {@link #onInit()}, {@link #onGet()}, {@link #onPost()}, {@link #onRender()}
     * etc.
     * <p/>
     * The order in which JS and CSS files are included will be preserved in the
     * page.
     * <p/>
     * <b>Note:</b> this method must never return null. If no HEAD elements
     * are available this method must return an empty {@link java.util.List}.
     * <p/>
     * <b>Also note:</b> a common problem when overriding getHeadElements in
     * subclasses is forgetting to call <em>super.getHeadElements</em>. Consider
     * carefully whether you should call <em>super.getHeadElements</em> or not.
     *
     * @return the list of HEAD elements to be included in the page
     */
    public List<Element> getHeadElements() {
        if (headElements == null) {
            headElements = new ArrayList<Element>(2);
        }
        return headElements;
    }

    /**
     * Return the localized Page resource message for the given resource
     * name or null if not found. The resource message returned will use the
     * Locale obtained from the Context.
     * <p/>
     * Pages can define text properties files to store localized messages. These
     * properties files must be stored on the Page class path with a name
     * matching the class name. For example:
     * <p/>
     * The page class:
     * <pre class="codeJava">
     *  <span class="kw">package</span> com.mycorp.pages;
     *
     *  <span class="kw">public class</span> Login <span class="kw">extends</span> Page {
     *     .. </pre>
     *
     * The page class property filenames and their path:
     * <pre class="codeConfig">
     *  /com/mycorp/pages/Login.properties
     *  /com/mycorp/pages/Login_en.properties
     *  /com/mycorp/pages/Login_fr.properties </pre>
     *
     * Page messages can also be defined in the optional global messages
     * bundle:
     *
     * <pre class="codeConfig">
     *  /click-page.properties </pre>
     *
     * To define global page messages simply add <tt>click-page.properties</tt>
     * file to your application's class path. Message defined in this properties
     * file will be available to all of your application pages.
     * <p/>
     * Note messages in your page class properties file will override any
     * messages in the global <tt>click-page.properties</tt> file.
     * <p/>
     * Page messages can be accessed directly in the page template using
     * the <span class="st">$messages</span> reference. For examples:
     *
     * <pre class="codeHtml">
     * <span class="blue">$messages.title</span> </pre>
     *
     * Please see the {@link org.apache.click.util.MessagesMap} adaptor for more
     * details.
     *
     * @param name resource name of the message
     * @return the named localized message for the page or null if no message
     * was found
     */
    public String getMessage(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }
        return getMessages().get(name);
    }

    /**
     * Return the formatted page message for the given resource name and
     * message format arguments or null if no message was found. The resource
     * message returned will use the Locale obtained from the Context.
     * <p/>
     * {@link #getMessage(java.lang.String)} is invoked to retrieve the message
     * for the specified name.
     *
     * @param name resource name of the message
     * @param args the message arguments to format
     * @return the named localized message for the page or null if no message
     * was found
     */
    public String getMessage(String name, Object... args) {
        String value = getMessage(name);

        return MessageFormat.format(value, args);
    }

    /**
     * Return a Map of localized messages for the Page. The messages returned
     * will use the Locale obtained from the Context.
     *
     * @see #getMessage(String)
     *
     * @return a Map of localized messages for the Page
     * @throws IllegalStateException if the context for the Page has not be set
     */
    public Map<String, String> getMessages() {
        if (messages == null) {
            if (getContext() != null) {
                messages = getContext().createMessagesMap(getClass(), PAGE_MESSAGES);

            } else {
                String msg = "Context not set cannot initialize messages";
                throw new IllegalStateException(msg);
            }
        }
        return messages;
    }

    /**
     * Add the named object value to the Pages model map.
     * <p/>
     * <b>Please note</b>: if the Page contains an object with a matching name,
     * that object will be replaced by the given value.
     *
     * @param name the key name of the object to add
     * @param value the object to add
     * @throws IllegalArgumentException if the name or value parameters are
     * null
     */
    public void addModel(String name, Object value) {
        if (name == null) {
            String msg = "Cannot add null parameter name to "
                    + getClass().getName() + " model";
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "Cannot add null " + name + " parameter "
                    + "to " + getClass().getName() + " model";
            throw new IllegalArgumentException(msg);
        }

        getModel().put(name, value);
    }

    /**
     * Return the Page's model map. The model is used populate the
     * Velocity Context with is merged with the page template before rendering.
     *
     * @return the Page's model map
     */
    public Map<String, Object> getModel() {
        return model;
    }

    /**
     * Return the Page header imports.
     * <p/>
     * PageImports are used define the CSS and JavaScript imports and blocks
     * to be included in the page template.
     * <p/>
     * The PageImports object will be included in the Page template when the
     * following methods are invoked:
     * <ul>
     * <li>{@link ClickServlet#createTemplateModel(Page)} - for template pages</li>
     * <li>{@link ClickServlet#setRequestAttributes(Page)} - for JSP pages</li>
     * </ul>
     * <p/>
     * If you need to tailor the page imports rendered, override this method
     * and modify the PageImports object returned.
     * <p/>
     * If you need to create a custom PageImports, override the method
     * {@link ClickServlet#createPageImports(org.openidentityplatform.openam.click.Page)}
     *
     * @deprecated use the new {@link #getHeadElements()} instead
     *
     * @return the Page header imports
     */
    public PageImports getPageImports() {
        return pageImports;
    }

    /**
     * Set the Page header imports.
     * <p/>
     * PageImports are used define the CSS and JavaScript imports and blocks
     * to be included in the page template.
     * <p/>
     * The PageImports references will be included in the Page model when the
     * following methods are invoked:
     * <ul>
     * <li>{@link ClickServlet#createTemplateModel(Page)} - for template pages</li>
     * <li>{@link ClickServlet#setRequestAttributes(Page)} - for JSP pages</li>
     * </ul>
     * <p/>
     * If you need to tailor the page imports rendered, override the
     * {@link #getPageImports()} method and modify the PageImports object
     * returned.
     * <p/>
     * If you need to create a custom PageImports, override the method
     * {@link ClickServlet#createPageImports(org.openidentityplatform.openam.click.Page)}
     *
     * @deprecated use the new {@link #getHeadElements()} instead
     *
     * @param pageImports the new pageImports instance to set
     */
    public void setPageImports(PageImports pageImports) {
        this.pageImports = pageImports;
    }

    /**
     * Return the path of the Template or JSP to render.
     * <p/>
     * If this method returns <tt>null</tt>, Click will not perform any rendering.
     * This is useful when you want to stream or write directly to the
     * HttpServletResponse.
     * <p/>
     * See also {@link #getForward()}, {@link #getRedirect()}
     *
     * @return the path of the Template or JSP to render
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the path of the Template or JSP to render.
     * <p/>
     * By default Click will set the path to the requested page url. Meaning
     * if the url <tt>/edit-customer.htm</tt> is requested, path will be set
     * to <tt>/edit-customer.htm</tt>.
     * <p/>
     * Here is an example if you want to change the path to a different Template:
     * <p/>
     * <pre class="prettyprint">
     * public void onGet() {
     *     setPath("/some-other-template.htm");
     * }</pre>
     * And here is an example if you want to change the path to a different JSP.
     * <pre class="prettyprint">
     * public void onGet() {
     *     setPath("/some-other-jsp.jsp");
     * }</pre>
     * <p/>
     * If path is set to <tt>null</tt>, Click will not perform any rendering.
     * This is useful when you want to stream or write directly to the
     * HttpServletResponse.
     * <p/>
     * See also {@link #setForward(String)}, {@link #setRedirect(String)}
     *
     * @param value the path of the Template or JSP to render
     */
    public void setPath(String value) {
        path = value;
    }

    /**
     * Return the path to redirect the request to.
     * <p/>
     * If the {@link #redirect} property is not null it will be used to redirect
     * the request in preference to {@link #forward} or {@link #path} properties.
     * The request is redirected to using the HttpServletResponse.setRedirect()
     * method.
     * <p/>
     * See also {@link #getForward()}, {@link #getPath()}
     *
     * @return the path to redirect the request to
     */
    public String getRedirect() {
        return redirect;
    }

    /**
     * Return true if the page is stateful and should be saved in the users
     * HttpSession between requests, default value is false.
     *
     * @deprecated stateful pages are not supported anymore, use stateful
     * Controls instead
     *
     * @return true if the page is stateful and should be saved in the users
     * session
     */
    public boolean isStateful() {
        return stateful;
    }

    /**
     * Set whether the page is stateful and should be saved in the users
     * HttpSession between requests.
     * <p/>
     * Click will synchronize on the page instance. This ensures that if
     * multiple requests arrive from the same user for the page, only one
     * request can access the page at a time.
     * <p/>
     * Stateful pages are stored in the HttpSession using the key
     * <tt>page.getClass().getName()</tt>.
     * <p/>
     * It is worth noting that Click checks a Page's stateful property after
     * each request. Thus it becomes possible to enable a stateful Page for a
     * number of requests and then setting it to <tt>false</tt> again at which
     * point Click will remove the Page from the HttpSession, freeing up memory
     * for the server.
     *
     * @deprecated stateful pages are not supported anymore, use stateful
     * Controls instead
     *
     * @param stateful the flag indicating whether the page should be saved
     *         between user requests
     */
    public void setStateful(boolean stateful) {
        this.stateful = stateful;
        if (isStateful()) {
            getContext().getSession();
        }
    }

    /**
     * Return true if the Control head elements should be included in the page
     * template, false otherwise. Default value is true.
     *
     * @see #setIncludeControlHeadElements(boolean)
     *
     * @return true if the Control head elements should be included in the page
     * template, false otherwise
     */
    public boolean isIncludeControlHeadElements() {
        return includeControlHeadElements;
    }

    /**
     * Set whether the Control head elements should be included in the page
     * template.
     * <p/>
     * By setting this value to <tt>false</tt>, Click won't include Control's
     * {@link #getHeadElements() head elements}, however the Page head elements
     * will still be included.
     * <p/>
     * This allows one to create a single JavaScript and CSS resource file for
     * the entire Page which increases performance, since the browser only has
     * to load one resource, instead of multiple resources.
     * <p/>
     * Below is an example:
     *
     * <pre class="prettyprint">
     * public class HomePage extends Page {
     *
     *     private Form form = new Form("form");
     *
     *     public HomePage() {
     *         // Indicate that Controls should not import their head elements
     *         setIncludeControlHeadElements(false);
     *
     *         form.add(new EmailField("email");
     *         addControl(form);
     *     }
     *
     *     // Include the Page JavaScript and CSS resources
     *     public List getHeadElements() {
     *         if (headElements == null) {
     *             headElements = super.getHeadElements();
     *
     *             // Include the Page CSS resource. This resource should combine
     *             // all the CSS necessary for the page
     *             headElements.add(new CssImport("/assets/css/home-page.css"));
     *
     *             // Include the Page JavaScript resource. This resource should
     *             // combine all the JavaScript necessary for the page
     *             headElements.add(new JsImport("/assets/js/home-page.js"));
     *         }
     *         return headElements;
     *     }
     * } </pre>
     *
     * @param includeControlHeadElements flag indicating whether Control
     * head elements should be included in the page
     */
    public void setIncludeControlHeadElements(boolean includeControlHeadElements) {
        this.includeControlHeadElements = includeControlHeadElements;
    }

    /**
     * Set the location to redirect the request to.
     * <p/>
     * If the {@link #redirect} property is not null it will be used to redirect
     * the request in preference to the {@link #forward} and {@link #path}
     * properties. The request is redirected using the HttpServletResponse.setRedirect()
     * method.
     * <p/>
     * If the redirect location begins with a <tt class="wr">"/"</tt>
     * character the redirect location will be prefixed with the web applications
     * <tt>context path</tt>. Note if the given location is already prefixed
     * with the <tt>context path</tt>, Click won't add it a second time.
     * <p/>
     * For example if an application is deployed to the context
     * <tt class="wr">"mycorp"</tt> calling
     * <tt>setRedirect(<span class="navy">"/customer/details.htm"</span>)</tt>
     * will redirect the request to:
     * <tt class="wr">"/mycorp/customer/details.htm"</tt>
     * <p/>
     * If the redirect location does not begin with a <tt class="wr">"/"</tt>
     * character the redirect location will be used as specified. Thus if the
     * location is <tt class="wr">http://somehost.com/myapp/customer.jsp</tt>,
     * Click will redirect to that location.
     * <p/>
     * <b>JSP note:</b> when redirecting to a JSP template keep in mind that the
     * JSP template won't be processed by Click, as ClickServlet is mapped to
     * <em>*.htm</em>. Instead JSP templates are processed by the Servlet
     * container JSP engine.
     * <p/>
     * So if you have a situation where a Page Class
     * (<span class="navy">Customer.class</span>) is mapped to the JSP
     * (<span class="navy">"/customer.jsp"</span>) and you want to redirect to
     * Customer.class, you could either redirect to
     * (<span class="navy">"/customer<span class="red">.htm</span>"</span>) or
     * use the alternative redirect utility {@link #setRedirect(java.lang.Class)}.
     * <p/>
     * <b>Please note</b> that Click will url encode the location by invoking
     * <tt>response.encodeRedirectURL(location)</tt> before redirecting.
     * <p/>
     * See also {@link #setRedirect(java.lang.String, java.util.Map)},
     * {@link #setForward(String)}, {@link #setPath(String)}
     *
     * @param location the path to redirect the request to
     */
    public void setRedirect(String location) {
        setRedirect(location, null);
    }

    /**
     * Set the request to redirect to the give page class.
     *
     * @see #setRedirect(java.lang.String)
     *
     * @param pageClass the class of the Page to redirect the request to
     * @throws IllegalArgumentException if the Page Class is not configured
     * with a unique path
     */
    public void setRedirect(Class<? extends Page> pageClass) {
        setRedirect(pageClass, null);
    }

    /**
     * Set the request to redirect to the given <code>location</code> and append
     * the map of request parameters to the location URL.
     * <p/>
     * The map keys will be used as the request parameter names and the map
     * values will be used as the request parameter values. For example:
     *
     * <pre class="prettyprint">
     * public boolean onSave() {
     *     // Specify redirect parameters
     *     Map parameters = new HashMap();
     *     parameters.put("customerId", getCustomerId());
     *
     *     // Set redirect to customer.htm page
     *     setRedirect("/customer.htm", parameters);
     *
     *     return false;
     * } </pre>
     *
     * To render multiple parameter values for the same parameter name, specify
     * the values as a String[] array. For example:
     *
     * <pre class="prettyprint">
     * public boolean onSave() {
     *
     *     // Specify an array of customer IDs
     *     String[] ids = {"123", "456", "789"};
     *
     *     // Specify redirect parameters
     *     Map parameters = new HashMap();
     *     parameters.put("customerIds", ids);
     *
     *     // Set redirect to customer.htm page
     *     setRedirect("/customer.htm", parameters);
     *
     *     return false;
     * } </pre>
     *
     * @see #setRedirect(java.lang.String)
     *
     * @param location the path to redirect the request to
     * @param params the map of request parameter name and value pairs
     */
    public void setRedirect(String location, Map<String, ?> params) {
        Context context = getContext();
        if (StringUtils.isNotBlank(location)) {
            if (location.charAt(0) == '/') {
                String contextPath = context.getRequest().getContextPath();

                // Guard against adding duplicate context path
                if (!location.startsWith(contextPath + '/')) {
                    location = contextPath + location;
                }
            }
        }

        if (params != null && !params.isEmpty()) {
            HtmlStringBuffer buffer = new HtmlStringBuffer();

            Iterator<? extends Map.Entry<String, ?>> i = params.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, ?> entry = i.next();
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();

                // Check for multivalued parameter
                if (paramValue instanceof String[]) {
                    String[] paramValues = (String[]) paramValue;
                    for (int j = 0; j < paramValues.length; j++) {
                        buffer.append(paramName);
                        buffer.append("=");
                        buffer.append(ClickUtils.encodeUrl(paramValues[j], context));
                        if (j < paramValues.length - 1) {
                            buffer.append("&");
                        }
                    }
                } else {
                    if (paramValue != null) {
                        buffer.append(paramName);
                        buffer.append("=");
                        buffer.append(ClickUtils.encodeUrl(paramValue, context));
                    }
                }
                if (i.hasNext()) {
                    buffer.append("&");
                }
            }

            if (buffer.length() > 0) {
                if (location.contains("?")) {
                    location += "&" + buffer.toString();
                } else {
                    location += "?" + buffer.toString();
                }
            }
        }

        redirect = location;
    }

    /**
     * Set the request to redirect to the given page class and and append
     * the map of request parameters to the page URL.
     * <p/>
     * The map keys will be used as the request parameter names and the map
     * values will be used as the request parameter values.
     *
     * @see #setRedirect(java.lang.String, java.util.Map)
     * @see #setRedirect(java.lang.String)
     *
     * @param pageClass the class of the Page to redirect the request to
     * @param params the map of request parameter name and value pairs
     * @throws IllegalArgumentException if the Page Class is not configured
     * with a unique path
     */
    public void setRedirect(Class<? extends Page> pageClass,
                            Map<String, ?> params) {

        String target = getContext().getPagePath(pageClass);

        // If page class maps to a jsp, convert to htm which allows ClickServlet
        // to process the redirect
        if (target != null && target.endsWith(".jsp")) {
            target = StringUtils.replaceOnce(target, ".jsp", ".htm");
        }

        setRedirect(target, params);
    }

    /**
     * Return the path of the page border template to render, by default this
     * method returns {@link #getPath()}.
     * <p/>
     * Pages can override this method to return an alternative border page
     * template. This is very useful when implementing an standardized look and
     * feel for a web site. The example below provides a BorderedPage base Page
     * which other site templated Pages should extend.
     *
     * <pre class="codeJava">
     * <span class="kw">public class</span> BorderedPage <span class="kw">extends</span> Page {
     *     <span class="kw">public</span> String getTemplate() {
     *         <span class="kw">return</span> <span class="st">"border.htm"</span>;
     *     }
     * } </pre>
     *
     * The BorderedPage returns the page border template <span class="st">"border.htm"</span>:
     *
     * <pre class="codeHtml">
     * &lt;html&gt;
     *   &lt;head&gt;
     *     &lt;title&gt; <span class="blue">$title</span> &lt;/title&gt;
     *     &lt;link rel="stylesheet" type="text/css" href="style.css" title="Style"/&gt;
     *   &lt;/head&gt;
     *   &lt;body&gt;
     *
     *     &lt;h1&gt; <span class="blue">$title</span> &lt;/h1&gt;
     *     &lt;hr/&gt;
     *
     *     <span class="red">#parse</span>( <span class="blue">$path</span> )
     *
     *   &lt;/body&gt;
     * &lt;/html&gt; </pre>
     *
     * Other pages insert their content into this template, via their
     * {@link #path} property using the Velocity
     * <a href="../../../../velocity/vtl-reference-guide.html#parse">#parse</a>
     * directive. Note the <span class="blue">$path</span> value is automatically
     * added to the VelocityContext by the ClickServlet.
     *
     * @return the path of the page template to render, by default returns
     * {@link #getPath()}
     */
    public String getTemplate() {
        return template == null ? getPath() : template;
    }

    /**
     * Set the page border template path.
     * <p/>
     * <b>Note:</b> if this value is not set, {@link #getTemplate()} will default
     * to {@link #getPath()}.
     *
     * @param template the border template path
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    // Private methods --------------------------------------------------------

    /**
     * Replace the current control with the new control.
     *
     * @param currentControl the control currently contained in the page
     * @param newControl the control to replace the current control container in
     * the page
     *
     * @throws IllegalStateException if the currentControl is not contained in
     * the page
     */
    private void replaceControl(Control currentControl, Control newControl) {

        // Current control and new control are referencing the same object
        // so we exit early
        if (currentControl == newControl) {
            return;
        }

        int controlIndex = getControls().indexOf(currentControl);
        if (controlIndex == -1) {
            throw new IllegalStateException("Cannot replace the given control"
                    + " because it is not present in the page");
        }

        // Note: set parent first since setParent might veto further processing
        newControl.setParent(this);
        currentControl.setParent(null);

        // Set control to current control index
        getControls().set(controlIndex, newControl);

        addModel(newControl.getName(), newControl);
    }
}
