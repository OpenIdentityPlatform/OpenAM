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

/**
 * Provides a Page life cycle interceptor. Classes implementing this interface
 * can be used to listen for key page life cycle events and abort further page
 * processing if required.
 * <p/>
 * PageInterceptors can be used for many different purposes including:
 * <ul>
 * <li>enforcing application wide page security policies</li>
 * <li>injecting dependencies into page objects</li>
 * <li>logging and profiling page performance</li>
 * </ul>
 *
 * A Click application can define multiple page interceptors that are invoked in
 * the order in which they are returned by the <tt>ConfigService</tt>.
 *
 * <h3><a name="scope"></a>Scope</h3>
 *
 * Page interceptors can be defined with a request level scope, whereby a new
 * page interceptor will be created with each page request providing a thread
 * safe programming model.
 * <p/>
 * Please note, as new interceptor instances are created with each request, care
 * should be taken to ensure that these objects are light weight and do not
 * introduce a performance bottleneck into your application.
 * <p/>
 * Alternatively, page interceptors can be defined with application level scope
 * whereby a single instance is created for the application and is used for
 * all requests.
 * <p/>
 * Note application scope interceptors are more efficient that request scope
 * interceptors, but you are responsible for ensuring that they are thread safe
 * and support reentrant method invocations as multiple page requests are
 * processed at the same time.
 *
 * <h3><a name="configuration"></a>Configuration</h3>
 *
 * Application PageInterceptors are configured in the <tt>click.xml</tt>
 * configuration file. PageInterceptors must support construction using a
 * no-args public constructor.
 * <p/>
 * Page interceptors can have multiple properties configured with their XML
 * definition which are set after the constructor has been called. Properties
 * are set using OGNL via {@link org.apache.click.util.PropertyUtils}.
 * <p/>
 * An example configuration is provided below:
 *
 * <pre class="prettyprint">
 * &lt;page-interceptor classname="com.mycorp.PageSecurityInterceptor" scope="application"&gt;
 *     &lt;property name="notAuthenticatedPath" value="/not-authenticated.htm"/&gt;
 *     &lt;property name="notAuthorizedPath" value="/not-authorized.htm"/&gt;
 * &lt;/page-interceptor&gt; </pre>
 *
 * The default scope for page interceptors is "request", but this can be configured
 * as "application" as is done in the example configuration above.
 *
 * <h3><a name="example"></a>Example</h3>
 *
 * <pre class="prettyprint">
 * public class SecurityInterceptor implements PageInterceptor {
 *
 *    // The request not authenticated redirect path.
 *    private String notAuthenticatedPath;
 *
 *    // The request not authorized redirect path.
 *    private String notAuthorizedPath;
 *
 *    // Public Methods ---------------------------------------------------------
 *
 *    public boolean preCreate(Class<? extends Page> pageClass, Context context) {
 *
 *       // If authentication required, then ensure user is authenticated
 *       Authentication authentication = pageClass.getAnnotation(Authentication.class);
 *
 *       // TODO: user context check.
 *
 *       if (authentication != null && authentication.required()) {
 *          sendRedirect(getNotAuthenticatedPath(), context);
 *          return false;
 *       }
 *
 *       // If authorization permission defined, then ensure user is authorized to access the page
 *       Authorization authorization = pageClass.getAnnotation(Authorization.class);
 *       if (authorization != null) {
 *          if (!UserContext.getThreadUserContext().hasPermission(authorization.permission())) {
 *             sendRedirect(getNotAuthorizedPath(), context);
 *             return false;
 *          }
 *       }
 *
 *       return true;
 *    }
 *
 *    public boolean postCreate(Page page) {
 *       return true;
 *    }
 *
 *    public boolean preResponse(Page page) {
 *       return true;
 *    }
 *
 *    public void postDestroy(Page page) {
 *    }
 *
 *    public String getNotAuthenticatedPath() {
 *       return notAuthenticatedPath;
 *    }
 *
 *    public void setNotAuthenticatedPath(String notAuthenticatedPath) {
 *       this.notAuthenticatedPath = notAuthenticatedPath;
 *    }
 *
 *    public String getNotAuthorizedPath() {
 *       return notAuthorizedPath;
 *    }
 *
 *    public void setNotAuthorizedPath(String notAuthorizedPath) {
 *       this.notAuthorizedPath = notAuthorizedPath;
 *    }
 *
 *    // Protected Methods ------------------------------------------------------
 *
 *    protected void sendRedirect(String location, Context context) {
 *       if (StringUtils.isNotBlank(location)) {
 *          if (location.charAt(0) == '/') {
 *             String contextPath = context.getRequest().getContextPath();
 *
 *             // Guard against adding duplicate context path
 *             if (!location.startsWith(contextPath + '/')) {
 *                location = contextPath + location;
 *             }
 *          }
 *       }
 *
 *       location = context.getResponse().encodeRedirectURL(location);
 *
 *       try {
 *          context.getResponse().sendRedirect(location);
 *
 *       } catch (IOException ioe) {
 *          throw new RuntimeException(ioe);
 *       }
 *   }
 * } </pre>
 *
 * <pre class="prettyprint">
 * // Page class authentication annotation
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * public @interface Authentication {
 *    boolean required() default true;
 * } </pre>
 *
 * <pre class="prettyprint">
 * // Page class authorization annotation
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * public @interface Authorization {
 *    String permission();
 * }
 * </pre>
 */
public interface PageInterceptor {

    /**
     * Provides a before page object creation interceptor method, which is passed
     * the class of the page to be instantiated and the page request context.
     * If this method returns true then the normal page processing is performed,
     * otherwise if this method returns false the page instance is never created
     * and the request is considered to have been handled.
     *
     * @param pageClass the class of the page to be instantiated
     * @param context the page request context
     * @return true to continue normal page processing or false whereby the
     * request is considered to be handled
     */
    public boolean preCreate(Class<? extends Page> pageClass, Context context);

    /**
     * Provides a post page object creation interceptor method, which is passed
     * the instance of the newly created page. This interceptor method is called
     * before the page {@link Page#onSecurityCheck()} method is invoked.
     * <p/>
     * If this method returns true then the normal page processing is performed,
     * otherwise if this method returns false the request is considered to have
     * been handled.
     * <p/>
     * Please note the page {@link Page#onDestroy()} method will still be invoked.
     *
     * @param page the newly instantiated page instance
     * @return true to continue normal page processing or false whereby the
     * request is considered to be handled
     */
    public boolean postCreate(Page page);

    /**
     * Provides a page interceptor before response method. This method is invoked
     * prior to the page redirect, forward or rendering phase.
     * <p/>
     * If this method returns true then the normal page processing is performed,
     * otherwise if this method returns false request is considered to have been
     * handled.
     * <p/>
     * Please note the page {@link Page#onDestroy()} method will still be invoked.
     *
     * @param page the newly instantiated page instance
     * @return true to continue normal page processing or false whereby the
     * request is considered to be handled
     */
    public boolean preResponse(Page page);

    /**
     * Provides a post page destroy interceptor method. This interceptor method
     * is called immediately after the page {@link Page#onDestroy()} method is
     * invoked.
     *
     * @param page the page object which has just been destroyed
     */
    public void postDestroy(Page page);

}
