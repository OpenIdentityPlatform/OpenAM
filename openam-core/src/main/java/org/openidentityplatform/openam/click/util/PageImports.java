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

import org.openidentityplatform.openam.click.Control;
import org.openidentityplatform.openam.click.Page;
import org.openidentityplatform.openam.click.control.Container;
import org.apache.click.control.Panel;
import org.openidentityplatform.openam.click.control.Radio;
import org.openidentityplatform.openam.click.control.RadioGroup;
import org.openidentityplatform.openam.click.control.Table;
import org.openidentityplatform.openam.click.element.CssImport;
import org.openidentityplatform.openam.click.element.CssStyle;
import org.openidentityplatform.openam.click.element.Element;
import org.openidentityplatform.openam.click.element.JsImport;
import org.openidentityplatform.openam.click.element.JsScript;
import org.openidentityplatform.openam.click.service.LogService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides a utility object for rendering a Page's HEAD elements and
 * control HEAD elements.
 * <p/>
 * PageImports automatically makes the HEAD elements available to Velocity
 * templates and JSP pages through the following variables:
 * <ul>
 * <li><span class="st">$headElements</span> - this variable includes all HEAD
 * elements except JavaScript elements</li>
 * <li><span class="st">$jsElements</span> - this variable includes only
 * JavaScript elements</li>
 * </ul>
 * By splitting JavaScript elements from other HEAD elements allows you to place
 * JavaScript elements at the bottom of the Page which allows the HTML content
 * to be rendered faster.
 * <p/>
 * To use the HEAD elements simply reference them in your page template. For
 * example:
 *
 * <pre class="codeHtml">
 * &lt;html&gt;
 *  &lt;head&gt;
 *   <span class="blue">$headElements</span>
 *  &lt;/head&gt;
 *  &lt;body&gt;
 *   <span class="red">$form</span>
 *
 *  <span class="blue">$jsElements</span>
 *  &lt;body&gt;
 * &lt;/html&gt; </pre>
 *
 * Its not always possible to move the JavaScript elements to the bottom of
 * the Page, for example there might be JavaScript scoping issues. In those
 * situations you can simply place the JavaScript elements variable in the Page
 * HEAD section:
 *
 * <pre class="codeHtml">
 * &lt;html&gt;
 *  &lt;head&gt;
 *   <span class="blue">$headElements</span>
 *   <span class="blue">$jsElements</span>
 *  &lt;/head&gt;
 *  &lt;body&gt;
 *   <span class="red">$form</span>
 *  &lt;body&gt;
 * &lt;/html&gt; </pre>
 *
 * <b>Please note: </b>the variables <span class="blue">$headElements</span> and
 * <span class="blue">$jsElements</span> are new in Click 2.1.0. For backwards
 * compatibility the HEAD elements are also available through the following
 * variables:
 *
 * <ul>
 * <li><span class="st">$imports</span> - this variable includes all HEAD
 * elements including JavaScript and Css elements</li>
 * <li><span class="st">$cssImports</span> - this variable includes only Css elements</li>
 * <li><span class="st">$jsImports</span> - this variable includes only Javascript elements</li>
 * </ul>
 *
 * Please also see {@link Page#getHeadElements()},
 * {@link Control#getHeadElements()}.
 */
public class PageImports {

    /** The page imports initialized flag. */
    protected boolean initialized = false;

    /** The list of head elements. */
    protected List<Element> headElements = new ArrayList<Element>(5);

    /** The list of CSS import lines. */
    protected List<CssImport> cssImports = new ArrayList<CssImport>(2);

    /** The list of JS import lines. */
    protected List<JsImport> jsImports = new ArrayList<JsImport>(2);

    /** The list of JS script block lines. */
    protected List<JsScript> jsScripts = new ArrayList<JsScript>(2);

    /** The list of CSS styles. */
    protected List<CssStyle> cssStyles = new ArrayList<CssStyle>(2);

    /** The page instance. */
    protected final Page page;

    // Constructor ------------------------------------------------------------

    /**
     * Create a page control HTML includes object.
     *
     * @param page the page to provide HTML includes for
     */
    public PageImports(Page page) {
        this.page = page;
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Add the given Element to the Page HEAD elements.
     *
     * @param element the Element to add
     * @throws IllegalArgumentException if the Element is null
     */
    public void add(Element element) {
        if (element == null) {
            throw new IllegalArgumentException("Null element parameter");
        }

        if (element instanceof JsImport) {
            if (jsImports.contains(element)) {
                return;
            }
            jsImports.add((JsImport) element);

        } else if (element instanceof JsScript) {
            if (((JsScript) element).isUnique()) {
                if (jsScripts.contains(element)) {
                    return;
                }
            }
            jsScripts.add((JsScript) element);

        } else if (element instanceof CssImport) {
            if (cssImports.contains(element)) {
                return;
            }
            cssImports.add((CssImport) element);

        } else if (element instanceof CssStyle) {
            if (((CssStyle) element).isUnique()) {
                if (cssStyles.contains(element)) {
                    return;
                }
            }
            cssStyles.add((CssStyle) element);

        } else {
            headElements.add(element);
        }
    }

    /**
     * Add the given list of Elements to the Page HEAD elements.
     *
     * @param elements the list of Elements to add to the Page HEAD elements
     * @throws IllegalArgumentException is the list of Elements are null
     */
    public void addAll(List<Element> elements) {
        if (elements == null) {
            throw new IllegalArgumentException("Null elements parameter");
        }

        for (Element element : elements) {
            add(element);
        }
    }

    /**
     * Return true if the page imports have been initialized.
     *
     * @return true if the page imports have been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Set whether the page imports have been initialized.
     *
     * @param initialized the page imports have been initialized flag
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Populate the specified model with html import keys.
     *
     * @param model the model to populate with html import keys
     */
    public void populateTemplateModel(Map<String, Object> model) {
        LogService logger = ClickUtils.getLogService();

        Object pop = model.put("headElements", new HeadElements());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                         + " model contains an object keyed with reserved "
                         + "name \"headElements\". The page model object "
                         + pop + " has been replaced with a PageImports object";
            logger.warn(msg);
        }

        pop = model.put("jsElements", new JsElements());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                         + " model contains an object keyed with reserved "
                         + "name \"jsElements\". The page model object "
                         + pop + " has been replaced with a PageImports object";
            logger.warn(msg);
        }

        // For backwards compatibility
        pop = model.put("imports", new Imports());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                         + " model contains an object keyed with reserved "
                         + "name \"imports\". The page model object "
                         + pop + " has been replaced with a PageImports object";
            logger.warn(msg);
        }

        // For backwards compatibility
        pop = model.put("cssImports", new CssElements());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
            + " model contains an object keyed with reserved "
            + "name \"cssImports\". The page model object "
            + pop + " has been replaced with a PageImports object";
            logger.warn(msg);
        }

        // For backwards compatibility
        pop = model.put("jsImports", new JsElements());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
            + " model contains an object keyed with reserved "
            + "name \"jsImports\". The page model object "
            + pop + " has been replaced with a PageImports object";
            logger.warn(msg);
        }
    }

    /**
     * Populate the specified request with html import keys.
     *
     * @param request the http request to populate
     * @param model the model to populate with html import keys
     */
    public void populateRequest(HttpServletRequest request, Map<String, Object> model) {
        LogService logger = ClickUtils.getLogService();

        request.setAttribute("headElements", new HeadElements());
        if (model.containsKey("headElements")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                             + " model contains an object keyed with reserved "
                             + "name \"headElements\". The request attribute "
                             + "has been replaced with a PageImports object";
            logger.warn(msg);
        }

        request.setAttribute("jsElements", new JsElements());
        if (model.containsKey("jeElements")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                             + " model contains an object keyed with reserved "
                             + "name \"jsElements\". The request attribute "
                             + "has been replaced with a PageImports object";
            logger.warn(msg);
        }

        // For backwards compatibility
        request.setAttribute("imports", new Imports());
        if (model.containsKey("imports")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                             + " model contains an object keyed with reserved "
                             + "name \"imports\". The request attribute "
                             + "has been replaced with a PageImports object";
            logger.warn(msg);
        }

        // For backwards compatibility
        request.setAttribute("cssImports", new CssElements());
        if (model.containsKey("cssImports")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                             + " model contains an object keyed with reserved "
                             + "name \"cssImports\". The request attribute "
                             + "has been replaced with a PageImports object";
            logger.warn(msg);
        }

        // For backwards compatibility
        request.setAttribute("jsImports", new JsElements());
        if (model.containsKey("jsImports")) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                             + " model contains an object keyed with reserved "
                             + "name \"jsImports\". The request attribute "
                             + "has been replaced with a PageImports object";
            logger.warn(msg);
        }
    }

    /**
     * Process the HEAD elements of the given list of Controls. You can retrieve
     * the processed HEAD elements through {@link #getHeadElements} and
     * {@link #getJsElements()}.
     * <p/>
     * This method delegates to {@link #processControl(Control)}
     * to add the given Control's HEAD elements to the Page imports.
     *
     * @param controls the list of Controls which HEAD elements to process
     */
    public void processControls(List<Control> controls) {
        for (Control control : controls) {

            // import from getHeadElement
            processControl(control);
        }
    }

    /**
     * Process the given control HEAD elements. This method will recursively
     * process Containers and all child controls. You can retrieve
     * the processed HEAD elements through {@link #getHeadElements} and
     * {@link #getJsElements()}.
     * <p/>
     * This method delegates to {@link #processHeadElements(List)}
     * to add the HEAD elements to the Page imports.
     *
     * @param control the control to process
     */
    public void processControl(Control control) {
        // Don't process inactive panels
        if (control instanceof Panel) {
            Panel panel = (Panel) control;
            if (!panel.isActive()) {
                return;
            }
        }

        processHeadElements(control.getHeadElements());

        if (control instanceof Container) {
            Container container = (Container) control;
            if (container.hasControls()) {
                for (Control childControl : container.getControls()) {
                    processControl(childControl);
                }
            }

        } else if (control instanceof Table) {
            Table table = (Table) control;
            if (table.hasControls()) {
                for (Control childControl : table.getControls()) {
                    processControl(childControl);
                }
            }

        } else if (control instanceof RadioGroup) {
            RadioGroup radioGroup = (RadioGroup) control;
            if (radioGroup.hasRadios()) {
                for (Radio radio : radioGroup.getRadioList()) {
                    processControl(radio);
                }
            }
        }
    }

    /**
     * Return the list of processed HEAD elements, excluding any JavaScript
     * elements. To retrieve JavaScript elements please see
     * {@link #getJsElements()}.
     *
     * @return the list of processed HEAD elements
     */
    public final List<Element> getHeadElements() {
        List<Element> result = new ArrayList<Element>(headElements);
        result.addAll(cssImports);
        result.addAll(cssStyles);
        return result;
    }

    /**
     * Return the list of processed JavaScript elements.
     *
     * @return the list of processed JavaScript elements
     */
    public final List<Element> getJsElements() {
        List<Element> result = new ArrayList<Element>(jsImports);
        result.addAll(jsScripts);
        return result;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Render an HTML representation of all the page's HTML head elements,
     * including: CSS imports, CSS styles, Title and Meta elements.
     *
     * @param buffer the specified buffer to render the page's HTML imports to
     */
    protected void renderHeadElements(HtmlStringBuffer buffer) {
        // First include miscellaneous elements e.g. Title and Meta elements.
        for (Element element : headElements) {
            element.render(buffer);
            buffer.append('\n');
        }

        // Next include all CSS imports and styles.
        renderCssElements(buffer);
    }

    /**
     * Render an HTML representation of all the page's HTML imports,
     * including: CSS imports, CSS styles, JS imports and JS scripts.
     *
     * @deprecated rather use {@link #renderHeadElements(HtmlStringBuffer)}
     * and {@link #renderJsElements(HtmlStringBuffer)}
     *
     * @param buffer the specified buffer to render the page's HTML imports to
     */
    protected void renderAllIncludes(HtmlStringBuffer buffer) {
        renderCssElements(buffer);
        renderJsElements(buffer);
    }

    /**
     * Render an HTML representation of all all the page's HTML CSS
     * {@link #cssImports imports} and {@link #cssStyles styles}.
     *
     * @param buffer the specified buffer to render the page's HTML imports to
     */
    protected void renderCssElements(HtmlStringBuffer buffer) {
        // First include all the imports e.g. <link href="...">
        for (CssImport cssImport : cssImports) {
            cssImport.render(buffer);
            buffer.append('\n');
        }

        // Then include all the styles e.g. <style>...</style>
        for (CssStyle cssStyle : cssStyles) {
            cssStyle.render(buffer);
            buffer.append('\n');
        }
    }

    /**
     * Render an HTML representation of all the page's HTML JavaScript
     * {@link #jsImports imports} and {@link #jsScripts scripts}.
     *
     * @param buffer the specified buffer to render the page's HTML imports to
     */
    protected void renderJsElements(HtmlStringBuffer buffer) {
        // First include all the imports e.g. <script src="...">
        for (JsImport jsImport : jsImports) {
            jsImport.render(buffer);
            buffer.append('\n');
        }

        // Then include all the scripts e.g. <script>...</script>
        for (JsScript jsScript : jsScripts) {
            jsScript.render(buffer);
            buffer.append('\n');
        }
    }

    /**
     * Process the Page's set of control HEAD elements.
     */
    protected void processPageControls() {
        if (isInitialized()) {
            return;
        }

        setInitialized(true);

        if (page.isIncludeControlHeadElements()) {
            if (page.hasControls()) {
                processControls(page.getControls());
            }
        }

        processHeadElements(page.getHeadElements());
    }

    /**
     * Process the given list of HEAD elements.
     * <p/>
     * This method invokes {@link #add(Element)} for
     * every <tt>Element</tt> entry in the specified list.
     *
     * @param elements the list of HEAD elements to process
     */
    protected void processHeadElements(List<Element> elements) {
        if (elements != null) {
            for (Element element : elements) {
                add(element);
            }
        }
    }

    // Internal Classes -------------------------------------------------------

    /**
     * This class enables lazy, on demand importing for
     * {@link #renderHeadElements(HtmlStringBuffer)}.
     */
    class HeadElements {

        /**
         * @see Object#toString()
         *
         * @return a string representing miscellaneous head and CSS elements
         */
        @Override
        public String toString() {
            processPageControls();
            HtmlStringBuffer buffer = new HtmlStringBuffer(
                80 * cssImports.size()
                + 80 * cssStyles.size()
                + 80 * headElements.size());
            PageImports.this.renderHeadElements(buffer);
            return buffer.toString();
        }
    }

    /**
     * This class enables lazy, on demand importing for
     * {@link #renderAllIncludes(HtmlStringBuffer)}.
     *
     * @deprecated rather use {@link HeadElements} and {@link JsImport}
     */
    class Imports {

        /**
         * @see Object#toString()
         *
         * @return a string representing all includes
         */
        @Override
        public String toString() {
            processPageControls();
            HtmlStringBuffer buffer = new HtmlStringBuffer(
                80 * jsImports.size()
                + 80 * jsScripts.size()
                + 80 * cssImports.size()
                + 80 * cssStyles.size());
            PageImports.this.renderAllIncludes(buffer);
            return buffer.toString();
        }
    }

    /**
     * This class enables lazy, on demand importing for
     * {@link #renderJsElements(HtmlStringBuffer)}.
     */
    class JsElements {

        /**
         * @see Object#toString()
         *
         * @return a string representing all JavaScript elements
         */
        @Override
        public String toString() {
            processPageControls();
            HtmlStringBuffer buffer = new HtmlStringBuffer(
                80 * jsImports.size() + 80 * jsScripts.size());

            PageImports.this.renderJsElements(buffer);
            return buffer.toString();
        }
    }

    /**
     * This class enables lazy, on demand importing for
     * {@link #renderCssElements(HtmlStringBuffer)}.
     *
     * @deprecated use {@link HeadElements} instead
     */
    class CssElements {

        /**
         * @see Object#toString()
         *
         * @return a string representing all CSS elements
         */
        @Override
        public String toString() {
            processPageControls();
            HtmlStringBuffer buffer = new HtmlStringBuffer(
                80 * cssImports.size() + 80 * cssStyles.size());

            PageImports.this.renderCssElements(buffer);
            return buffer.toString();
        }
    }
}
