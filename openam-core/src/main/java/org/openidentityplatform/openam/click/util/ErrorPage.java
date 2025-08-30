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

package org.openidentityplatform.openam.click.util;

import org.openidentityplatform.openam.click.Context;
import org.openidentityplatform.openam.click.Page;

/**
 * Provides the base error handling Page. The ErrorPage handles any
 * unexpected Exceptions. When the application is not in "production" mode the
 * ErrorPage will provide diagnostic information.
 * <p/>
 * The ErrorPage template "<span class="blue">click/error.htm</span>" can be
 * customized to your needs.
 * <p/>
 * Applications which require additional error handling logic must subclass
 * the ErrorPage. For example to rollback a Connection if an SQLException occurred:
 *
 * <pre class="codeJava">
 * <span class="kw">package</span> com.mycorp.util;
 *
 * <span class="kw">import</span> java.sql.Connection;
 * <span class="kw">import</span> java.sql.SQLException;
 * <span class="kw">import</span> org.apache.click.util.ErrorPage;
 *
 * <span class="kw">public class</span> MyCorpErrorPage <span class="kw">extends</span> ErrorPage {
 *
 *     <span class="jd">/**
 *      * @see Page#onDestroy()
 *      * /</span>
 *     <span class="kw">public void</span> onDestroy() {
 *         Exception error = getError();
 *
 *         <span class="kw">if</span> (error <span class="kw">instanceof</span> SQLException ||
 *             error.getCause() <span class="kw">instanceof</span> SQLException) {

 *             Connection connection =
 *                 ConnectionProviderThreadLocal.getConnection();
 *
 *             <span class="kw">if</span> (connection != <span class="kw">null</span>) {
 *                 <span class="kw">try</span> {
 *                     connection.rollback();
 *                 }
 *                 <span class="kw">catch</span> (SQLException sqle) {
 *                 }
 *                 <span class="kw">finally</span> {
 *                     <span class="kw">try</span> {
 *                         connection.close();
 *                     }
 *                     <span class="kw">catch</span> (SQLException sqle) {
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * } </pre>
 *
 * The ClickServlet sets the following ErrorPage properties in addition to
 * the normal Page properties:<ul>
 * <li>{@link #error} - the error causing exception</li>
 * <li>{@link #mode} - the Click application mode</li>
 * <li>{@link #pageClass} - the Page class which cause the error</li>
 * </ul>
 */
public class ErrorPage extends Page {
    private static final long serialVersionUID = 1L;

    /** The number of lines to display. */
    protected static final int NUMB_LINES = 8;

    /** The error causing exception. */
    protected Throwable error;

    /**
     * The application mode: &nbsp;
     * ["production", "profile", "development", "debug", "trace"].
     */
    protected String mode;

    /** The page class in error. */
    protected Class<? extends Page> pageClass;

    // --------------------------------------------------------- Event Handlers

    /**
     * This method initializes the ErrorPage, populating the model with error
     * diagnostic information.
     * <p/>
     * The following values are added to ErrorPage model for rendering by the
     * error page template:
     *
     * <ul style="margin-top: 0.5em;">
     * <li><tt>errorReport</tt> &nbsp; - &nbsp; the detailed error report
     * &lt;div&gt; element, with an id of 'errorReport'</li>
     * <li><tt>mode</tt> &nbsp; - &nbsp; the application mode</li>
     * </ul>
     *
     * @see Page#onInit()
     */
    @Override
    public void onInit() {
        addModel("mode", getMode());
        Context context = getContext();
        if (getError() != null) {
            ErrorReport errorReport =
                    new ErrorReport(error,
                            getPageClass(),
                            false,
                            context.getRequest(),
                            context.getServletContext());

            addModel("errorReport", errorReport);
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return the causing error.
     *
     * @return the causing error
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Set the causing error.
     *
     * @param cause the causing error
     */
    public void setError(Throwable cause) {
        this.error = cause;
    }

    /**
     * Return the application mode: <tt>["production", "profile", "development",
     * debug", "trace"]</tt>.
     *
     * @return the application mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Set the application mode: <tt>["production", "profile", "development",
     * debug", "trace"]</tt>
     * <p/>
     * The application mode is added to the model by the {@link #onInit()} method.
     * This property is used to determines whether the error page template
     * should display error diagnostic information. The default "error.htm" will
     * display error diagnostic information so long as the application mode is
     * not "production".
     *
     * @param value the application mode.
     */
    public void setMode(String value) {
        mode = value;
    }

    /**
     * Return the page class in error.
     *
     * @return the page class in error
     */
    public Class<? extends Page> getPageClass() {
        return pageClass;
    }

    /**
     * Set the page class in error.
     *
     * @param pageClass the page class in error
     */
    public void setPageClass(Class<? extends Page> pageClass) {
        this.pageClass = pageClass;
    }
}
