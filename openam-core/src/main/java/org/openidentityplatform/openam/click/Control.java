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
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;

import org.openidentityplatform.openam.click.element.Element;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;

/**
 * Provides the interface for Page controls. Controls are also referred to
 * as components or widgets.
 * <p/>
 * When a Page request event is processed Controls may perform server side event
 * processing through their {@link #onProcess()} method. Controls are generally
 * rendered in a Page by calling their <tt>toString()</tt> method.
 * <p/>
 * The Control execution sequence is illustrated below:
 * <p/>
 * <img src="control-post-sequence-diagram.png"/>
 *
 * <h4>HTML HEAD Elements</h4>
 *
 * Control HTML HEAD elements can be included in the Page by overriding the
 * {@link #getHeadElements()} method.
 * <p/>
 * Below is an example of a custom TextField control specifying that the
 * <tt>custom.js</tt> file should be included in the HTML HEADer:
 *
 * <pre class="prettyprint">
 * public class CustomField extends TextField {
 *
 *     <span class="kw">public</span> List getHeadElements() {
 *         if(headElements == null) {
 *             // If headElements is null, create default headElements
 *             headElements = super.getHeadElements();
 *
 *             // Add a new JavaScript Import Element for the "/custom.js" script
 *             headElements.add(new JsImport("/click/custom.js"));
 *         }
 *         return headElements;
 *     }
 *
 *     ..
 * } </pre>
 *
 * <a name="on-deploy"></a>
 * <h4>Deploying Resources</h4>
 *
 * The Click framework uses the Velocity Tools <tt>WebappResourceLoader</tt> for loading templates.
 * This avoids issues associate with using the Velocity <tt>ClasspathResourceLoader</tt> and
 * <tt>FileResourceLoader</tt> on J2EE application servers.
 * To make preconfigured resources (templates, JavaScript, stylesheets, etc.)
 * available to web applications Click automatically deploys configured classpath
 * resources to the <tt class="blue">/click</tt> directory at startup
 * (existing files will not be overwritten).
 * <p/>
 * Click supports two ways of deploying pre-configured resources. The recommended
 * deployment strategy (which also the simplest) relies on packaging resources
 * into a special folder of the JAR, called <tt>'META-INF/resources'</tt>. At
 * startup time Click will scan this folder for resources and deploy them to the
 * web application. This deployment strategy is the same approach taken by the
 * Servlet 3.0 specification. Please see the section
 * <a href="../../../../user-guide/html/ch05s03.html#deploying-custom-resources">Deploying Custom Resources</a>
 * for more details.
 * <p/>
 * An alternative approach to deploying static resources on startup is provided
 * by the Control interface through the {@link #onDeploy(ServletContext)} method.
 * <p/>
 * Continuing our example, the <tt>CustomField</tt> control deploys its
 * <tt>custom.js</tt> file to the <tt>/click</tt> directory:
 *
 * <pre class="codeJava">
 * <span class="kw">public class</span> CustomField <span class="kw">extends</span> TextField {
 *     ..
 *
 *     <span class="kw">public void</span> onDeploy(ServletContext servletContext) {
 *         ClickUtils.deployFile
 *             (servletContext, <span class="st">"/com/mycorp/control/custom.js"</span>, <span class="st">"click"</span>);
 *     }
 * } </pre>
 *
 * Controls using the <tt>onDeploy()</tt> method must be registered in the
 * application <tt>WEB-INF/click.xml</tt> for them to be invoked.
 * For example:
 *
 * <pre class="codeConfig">
 * &lt;click-app&gt;
 *   &lt;pages package="com.mycorp.page" automapping="true"/&gt;
 *
 *   &lt;controls&gt;
 *     &lt;control classname=<span class="st">"com.mycorp.control.CustomField"</span>/&gt;
 *   &lt;/controls&gt;
 * &lt;/click-app&gt; </pre>
 *
 * When the Click application starts up it will deploy any control elements
 * defined in the following files in sequential order:
 * <ul>
 *  <li><tt>/click-controls.xml</tt>
 *  <li><tt>/extras-controls.xml</tt>
 *  <li><tt>WEB-INF/click.xml</tt>
 * </ul>
 *
 * <b>Please note</b> {@link org.apache.click.control.AbstractControl} provides
 * a default implementation of the Control interface to make it easier for
 * developers to create their own controls.
 *
 * @see org.apache.click.util.PageImports
 */
public interface Control extends Serializable {

    /**
     * The global control messages bundle name: &nbsp; <tt>click-control</tt>.
     */
    public static final String CONTROL_MESSAGES = "click-control";

    /**
     * Return the Page request Context of the Control.
     *
     * @deprecated getContext() is now obsolete on the Control interface,
     * but will still be available on AbstractControl:
     * {@link org.apache.click.control.AbstractControl#getContext()}
     *
     * @return the Page request Context
     */
    public Context getContext();

    /**
     * Return the list of HEAD {@link org.apache.click.element.Element elements}
     * to be included in the page. Example HEAD elements include
     * {@link org.apache.click.element.JsImport JsImport},
     * {@link org.apache.click.element.JsScript JsScript},
     * {@link org.apache.click.element.CssImport CssImport} and
     * {@link org.apache.click.element.CssStyle CssStyle}.
     * <p/>
     * Controls can contribute their own list of HEAD elements by implementing
     * this method.
     * <p/>
     * The recommended approach when implementing this method is to use
     * <tt>lazy loading</tt> to ensure the HEAD elements are only added
     * <tt>once</tt> and when <tt>needed</tt>. For example:
     *
     * <pre class="prettyprint">
     * public MyControl extends AbstractControl {
     *
     *     public List getHeadElements() {
     *         // Use lazy loading to ensure the JS is only added the
     *         // first time this method is called.
     *         if (headElements == null) {
     *             // Get the head elements from the super implementation
     *             headElements = super.getHeadElements();
     *
     *             // Include the control's external JavaScript resource
     *             JsImport jsImport = new JsImport("/mycorp/mycontrol/mycontrol.js");
     *             headElements.add(jsImport);
     *
     *             // Include the control's external Css resource
     *             CssImport cssImport = new CssImport("/mycorp/mycontrol/mycontrol.css");
     *             headElements.add(cssImport);
     *         }
     *         return headElements;
     *     }
     * } </pre>
     *
     * Alternatively one can add the HEAD elements in the Control's constructor:
     *
     * <pre class="prettyprint">
     * public MyControl extends AbstractControl {
     *
     *     public MyControl() {
     *
     *         JsImport jsImport = new JsImport("/mycorp/mycontrol/mycontrol.js");
     *         getHeadElements().add(jsImport);
     *
     *         CssImport cssImport = new CssImport("/mycorp/mycontrol/mycontrol.css");
     *         getHeadHeaders().add(cssImport);
     *     }
     * } </pre>
     *
     * One can also add HEAD elements from event handler methods such as
     * {@link #onInit()}, {@link #onProcess()}, {@link #onRender()}
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
    public List<Element> getHeadElements();

    /**
     * Return HTML element identifier attribute "id" value.
     *
     * {@link org.apache.click.control.AbstractControl#getId()}
     *
     * @return HTML element identifier attribute "id" value
     */
    public String getId();

    /**
     * Set the controls event listener.
     * <p/>
     * The method signature of the listener is:<ul>
     * <li>must have a valid Java method name</li>
     * <li>takes no arguments</li>
     * <li>returns a boolean value</li>
     * </ul>
     * <p/>
     * An example event listener method would be:
     *
     * <pre class="codeJava">
     * <span class="kw">public boolean</span> onClick() {
     *     System.out.println(<span class="st">"onClick called"</span>);
     *     <span class="kw">return true</span>;
     * } </pre>
     *
     * @param listener the listener object with the named method to invoke
     * @param method the name of the method to invoke
     *
     * @deprecated this method is now obsolete on the Control interface, but
     * will still be available on AbstractControl:
     * {@link org.apache.click.control.AbstractControl#setListener(java.lang.Object, java.lang.String)}
     */
    public void setListener(Object listener, String method);

    /**
     * Return the localized messages <tt>Map</tt> of the Control.
     *
     * @return the localized messages <tt>Map</tt> of the Control
     */
    public Map<String, String> getMessages();

    /**
     * Return the name of the Control. Each control name must be unique in the
     * containing Page model or the containing Form.
     *
     * @return the name of the control
     */
    public String getName();

    /**
     * Set the name of the Control. Each control name must be unique in the
     * containing Page model or the parent container.
     * <p/>
     * <b>Please note:</b> changing the name of a Control after it has been
     * added to its parent container is undefined. Thus it is  best <b>not</b>
     * to change the name of a Control once its been set.
     *
     * @param name of the control
     * @throws IllegalArgumentException if the name is null
     */
    public void setName(String name);

    /**
     * Return the parent of the Control.
     *
     * @return the parent of the Control
     */
    public Object getParent();

    /**
     * Set the parent of the  Control.
     *
     * @param parent the parent of the Control
     */
    public void setParent(Object parent);

    /**
     * The on deploy event handler, which provides classes the
     * opportunity to deploy static resources when the Click application is
     * initialized.
     * <p/>
     * For example:
     * <pre class="codeJava">
     * <span class="kw">public void</span> onDeploy(ServletContext servletContext) <span class="kw">throws</span> IOException {
     *     ClickUtils.deployFile
     *         (servletContext, <span class="st">"/com/mycorp/control/custom.js"</span>, <span class="st">"click"</span>);
     * } </pre>
     * <b>Please note:</b> a common problem when overriding onDeploy in
     * subclasses is forgetting to call <em>super.onDeploy</em>. Consider
     * carefully whether you should call <em>super.onDeploy</em> or not.
     * <p/>
     * Click also supports an alternative deployment strategy which relies on
     * packaging resource (stylesheets, JavaScript, images etc.) following a
     * specific convention. See the section
     * <a href="../../../../user-guide/html/ch05s03.html#deploying-custom-resources">Deploying Custom Resources</a>
     * for further details.
     *
     * @param servletContext the servlet context
     */
    public void onDeploy(ServletContext servletContext);

    /**
     * The on initialize event handler. Each control will be initialized
     * before its {@link #onProcess()} method is called.
     * <p/>
     * {@link org.apache.click.control.Container} implementations should recursively
     * invoke the onInit method on each of their child controls ensuring that
     * all controls receive this event.
     * <p/>
     * <b>Please note:</b> a common problem when overriding onInit in
     * subclasses is forgetting to call <em>super.onInit()</em>. Consider
     * carefully whether you should call <em>super.onInit()</em> or not,
     * especially for {@link org.apache.click.control.Container}s which by default
     * call <em>onInit</em> on all their child controls as well.
     */
    public void onInit();

    /**
     * The on process event handler. Each control will be processed when the
     * Page is requested.
     * <p/>
     * ClickServlet will process all Page controls in the order they were added
     * to the Page.
     * <p/>
     * {@link org.apache.click.control.Container} implementations should recursively
     * invoke the onProcess method on each of their child controls ensuring that
     * all controls receive this event. However when a control onProcess method
     * return false, no other controls onProcess method should be invoked.
     * <p/>
     * When a control is processed it should return true if the Page should
     * continue event processing, or false if no other controls should be
     * processed and the {@link org.apache.click.Page#onGet()} or {@link Page#onPost()} methods
     * should not be invoked.
     * <p/>
     * <b>Please note:</b> a common problem when overriding onProcess in
     * subclasses is forgetting to call <em>super.onProcess()</em>. Consider
     * carefully whether you should call <em>super.onProcess()</em> or not,
     * especially for {@link org.apache.click.control.Container}s which by default
     * call <em>onProcess</em> on all their child controls as well.
     *
     * @return true to continue Page event processing or false otherwise
     */
    public boolean onProcess();

    /**
     * The on render event handler. This event handler is invoked prior to the
     * control being rendered, and is useful for providing pre rendering logic.
     * <p/>
     * The on render method is typically used to populate tables performing some
     * database intensive operation. By putting the intensive operations in the
     * on render method they will not be performed if the user navigates away
     * to a different page.
     * <p/>
     * {@link org.apache.click.control.Container} implementations should recursively
     * invoke the onRender method on each of their child controls ensuring that
     * all controls receive this event.
     * <p/>
     * <b>Please note:</b> a common problem when overriding onRender in
     * subclasses is forgetting to call <em>super.onRender()</em>. Consider
     * carefully whether you should call <em>super.onRender()</em> or not,
     * especially for {@link org.apache.click.control.Container}s which by default
     * call <em>onRender</em> on all their child controls as well.
     */
    public void onRender();

    /**
     * The on destroy request event handler. Control classes should use this
     * method to add any resource clean up code.
     * <p/>
     * This method is guaranteed to be called before the Page object reference
     * goes out of scope and is available for garbage collection.
     * <p/>
     * {@link org.apache.click.control.Container} implementations should recursively
     * invoke the onDestroy method on each of their child controls ensuring that
     * all controls receive this event.
     * <p/>
     * <b>Please note:</b> a common problem when overriding onDestroy in
     * subclasses is forgetting to call <em>super.onDestroy()</em>. Consider
     * carefully whether you should call <em>super.onDestroy()</em> or not,
     * especially for {@link org.apache.click.control.Container}s which by default
     * call <em>onDestroy</em> on all their child controls as well.
     */
    public void onDestroy();

    /**
     * Render the control's HTML representation to the specified buffer. The
     * control's {@link java.lang.Object#toString()} method should delegate the
     * rendering to the render method for improved performance.
     * <p/>
     * An example implementation:
     * <pre class="prettyprint">
     * public class Border extends AbstractContainer {
     *
     *     public String toString() {
     *         int estimatedSizeOfControl = 100;
     *         HtmlStringBuffer buffer = new HtmlStringBuffer(estimatedSizeOfControl);
     *         render(buffer);
     *         return buffer.toString();
     *     }
     *
     *     &#47;**
     *      * &#64;see Control#render(HtmlStringBuffer)
     *      *&#47;
     *     public void render(HtmlStringBuffer buffer) {
     *         buffer.elementStart("div");
     *         buffer.appendAttribute("name", getName());
     *         buffer.closeTag();
     *         buffer.append(getField());
     *         buffer.elementEnd("div");
     *     }
     * }
     * </pre>
     *
     * @param buffer the specified buffer to render the control's output to
     */
    public void render(HtmlStringBuffer buffer);

    /**
     * Returns <tt>true</tt> if this control has any
     * <tt>Behavior</tt>s registered, <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if this control has any
     * <tt>Behavior</tt>s registered, <tt>false</tt> otherwise
     */
    public boolean hasBehaviors();

    /**
     * Returns the list of behaviors for this control.
     *
     * @return the list with this control behaviors.
     */
    public Set<Behavior> getBehaviors();

    /**
     * Returns <tt>true</tt> if this control is an Ajax target, <tt>false</tt>
     * otherwise.
     * <p/>
     * In order for a Control to be considered as an Ajax target it must be
     * registered through {@link org.apache.click.ControlRegistry#registerAjaxTarget(org.apache.click.Control) ControlRegistry.registerAjaxTarget}.
     * <p/>
     * When the Click handles an Ajax request it iterates the Controls
     * registered with the {@link org.apache.click.ControlRegistry ControlRegistry}
     * and checks if one of them is the Ajax target by calling
     * {@link #isAjaxTarget(org.openidentityplatform.openam.click.Context) isAjaxTarget}. If <tt>isAjaxTarget</tt>
     * returns true, Click will process that Control's {@link #getBehaviors() behaviors}.
     * <p/>
     * <b>Please note:</b> there can only be one target control, so the first
     * Control that is identified as the Ajax target will be processed, the other
     * controls will be skipped.
     * <p/>
     * The most common way to check whether a Control is the Ajax target is to
     * check if its {@link #getId ID} is available as a request parameter:
     *
     * <pre class="prettyprint">
     * public MyControl extends AbstractControl {
     *
     *     ...
     *
     *     public boolean isAjaxTarget(Context context) {
     *         return context.hasRequestParameter(getId());
     *     }
     * } </pre>
     *
     * Not every scenario can be covered through an ID attribute though. For example
     * if an ActionLink is rendered multiple times on the same page, it cannot have an
     * ID attribute, as that would lead to duplicate IDs, which isn't allowed by
     * the HTML specification. Control implementations has to cater for how the
     * control will be targeted. In the case of ActionLink it might check against
     * its <tt>id</tt>, and if that isn't available check against its <tt>name</tt>.
     *
     * @param context the request context
     * @return <tt>true</tt> if this control is an Ajax target, <tt>false</tt>
     * otherwise
     */
    public boolean isAjaxTarget(Context context);
}
