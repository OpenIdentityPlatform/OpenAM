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
package org.openidentityplatform.openam.click.control;

import org.openidentityplatform.openam.click.Context;
import org.apache.click.Stateful;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Submit;
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;
import org.apache.commons.lang.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provides a Abstract Link control: &nbsp; &lt;a href=""&gt;&lt;/a&gt;.
 * <p/>
 * See also the W3C HTML reference:
 * <a class="external" target="_blank" title="W3C HTML 4.01 Specification"
 *    href="http://www.w3.org/TR/html401/struct/links.html#h-12.2">A Links</a>
 *
 * @see ActionLink
 * @see Submit
 */
public abstract class AbstractLink extends AbstractControl implements Stateful {

    private static final long serialVersionUID = 1L;

    // Instance Variables -----------------------------------------------------

    /** The Field disabled value. */
    protected boolean disabled;

    /**
     * The image src path attribute.  If the image src is defined then a
     * <tt>&lt;img/&gt;</tt> element will rendered inside the anchor link when
     * using the AbstractLink {@link #toString()} method.
     * <p/>
     * If the image src value is prefixed with '/' then the request context path
     * will be prefixed to the src value when rendered by the control.
     */
    protected String imageSrc;

    /** The link display label. */
    protected String label;

    /** The link parameters map. */
    protected Map<String, Object> parameters;

    /** The link 'tabindex' attribute. */
    protected int tabindex;

    /** The link title attribute, which acts as a tooltip help message. */
    protected String title;

    /** Flag to set if both icon and text are rendered, default value is false. */
    protected boolean renderLabelAndImage = false;

    // Constructors -----------------------------------------------------------

    /**
     * Create an AbstractLink for the given name.
     *
     * @param name the page link name
     * @throws IllegalArgumentException if the name is null
     */
    public AbstractLink(String name) {
        setName(name);
    }

    /**
     * Create an AbstractLink with no name defined.
     * <p/>
     * <b>Please note</b> the control's name must be defined before it is valid.
     */
    public AbstractLink() {
    }

    // Public Attributes ------------------------------------------------------

    /**
     * Return the link html tag: <tt>a</tt>.
     *
     * @see AbstractControl#getTag()
     *
     * @return this controls html tag
     */
    @Override
    public String getTag() {
        return "a";
    }

    /**
     * Return true if the AbstractLink is a disabled.  If the link is disabled
     * it will be rendered as &lt;span&gt; element with a HTML class attribute
     * of "disabled".
     *
     * @return true if the AbstractLink is a disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Set the disabled flag. If the link is disabled it will be rendered as
     * &lt;span&gt; element with a HTML class attribute of "disabled".
     *
     * @param disabled the disabled flag
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Return the AbstractLink anchor &lt;a&gt; tag href attribute.
     * This method will encode the URL with the session ID
     * if required using <tt>HttpServletResponse.encodeURL()</tt>.
     *
     * @return the AbstractLink HTML href attribute
     */
    public abstract String getHref();

    /**
     * Return the image src path attribute. If the image src is defined then a
     * <tt>&lt;img/&gt;</tt> element will be rendered inside the anchor link
     * when using the AbstractLink {@link #toString()} method.
     * <p/>
     * <b>Note:</b> the label will not be rendered in this case (default behavior),
     * unless the {@link #setRenderLabelAndImage(boolean)} flag is set to true.
     * <p/>
     * If the src value is prefixed with '/' then the request context path will
     * be prefixed to the src value when rendered by the control.
     *
     * @return the image src path attribute
     */
    public String getImageSrc() {
        return imageSrc;
    }

    /**
     * Set the image src path attribute. If the src value is prefixed with
     * '/' then the request context path will be prefixed to the src value when
     * rendered by the control.
     * <p/>
     * If the image src is defined then an <tt>&lt;img/&gt;</tt> element will
     * be rendered inside the anchor link when using the AbstractLink
     * {@link #toString()} method.
     * <p/>
     * <b>Note:</b> the label will not be rendered in this case (default behavior),
     * unless the {@link #setRenderLabelAndImage(boolean)} flag is set to true.
     *
     * @param src the image src path attribute
     */
    public void setImageSrc(String src) {
        this.imageSrc = src;
    }

    /**
     * Return the "id" attribute value if defined, or null otherwise.
     *
     * @see org.apache.click.Control#getId()
     *
     * @return HTML element identifier attribute "id" value
     */
    @Override
    public String getId() {
        if (hasAttributes()) {
            return getAttribute("id");
        } else {
            return null;
        }
    }

    /**
     * Return the label for the AbstractLink.
     * <p/>
     * If the label value is null, this method will attempt to find a
     * localized label message in the parent messages using the key:
     * <blockquote>
     * <tt>getName() + ".label"</tt>
     * </blockquote>
     * If not found then the message will be looked up in the
     * <tt>/click-control.properties</tt> file using the same key.
     * If a value still cannot be found then the ActionLink name will be converted
     * into a label using the method: {@link ClickUtils#toLabel(String)}
     * <p/>
     * For example given a <tt>OrderPage</tt> with the properties file
     * <tt>OrderPage.properties</tt>:
     *
     * <pre class="codeConfig">
     * <span class="st">checkout</span>.label=<span class="red">Checkout</span>
     * <span class="st">checkout</span>.title=<span class="red">Proceed to Checkout</span> </pre>
     *
     * The page ActionLink code:
     * <pre class="codeJava">
     * <span class="kw">public class</span> OrderPage <span class="kw">extends</span> Page {
     *     ActionLink checkoutLink = <span class="kw">new</span> ActionLink(<span class="st">"checkout"</span>);
     *     ..
     * } </pre>
     *
     * Will render the AbstractLink label and title properties as:
     * <pre class="codeHtml">
     * &lt;a href=".." title="<span class="red">Proceed to Checkout</span>"&gt;<span class="red">Checkout</span>&lt;/a&gt; </pre>
     *
     * When a label value is not set, or defined in any properties files, then
     * its value will be created from the Fields name.
     * <p/>
     * For example given the ActionLink code:
     *
     * <pre class="codeJava">
     * ActionLink nameField = <span class="kw">new</span> ActionLink(<span class="st">"deleteItem"</span>);  </pre>
     *
     * Will render the ActionLink label as:
     * <pre class="codeHtml">
     * &lt;a href=".."&gt;<span class="red">Delete Item</span>&lt;/a&gt; </pre>
     *
     * Note the ActionLink label can include raw HTML to render other elements.
     * <p/>
     * For example the configured label:
     *
     * <pre class="codeConfig">
     * <span class="st">edit</span>.label=<span class="red">&lt;img src="images/edit.png" title="Edit Item"/&gt;</span> </pre>
     *
     * Will render the ActionLink label as:
     * <pre class="codeHtml">
     * &lt;a href=".."&gt;<span class="red">&lt;img src="images/edit.png" title="Edit Item"/&gt;</span>&lt;/a&gt; </pre>
     *
     * @return the label for the ActionLink
     */
    public String getLabel() {
        if (label == null) {
            label = getMessage(getName() + ".label");
        }
        if (label == null) {
            label = ClickUtils.toLabel(getName());
        }
        return label;
    }

    /**
     * Set the label for the ActionLink.
     *
     * @see #getLabel()
     *
     * @param label the label for the ActionLink
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Return the link request parameter value for the given name, or null if
     * the parameter value does not exist.
     *
     * @param name the name of request parameter
     * @return the link request parameter value
     */
    public String getParameter(String name) {
        if (hasParameters()) {
            Object value = getParameters().get(name);

            if (value instanceof String) {
                return (String) value;
            }

            if (value instanceof String[]) {
                String[] array = (String[]) value;
                if (array.length >= 1) {
                    return array[0];
                } else {
                    return null;
                }
            }

            return (value == null ? null : value.toString());
        } else {
            return null;
        }
    }

    /**
     * Set the link parameter with the given parameter name and value. You would
     * generally use parameter if you were creating the entire AbstractLink
     * programmatically and rendering it with the {@link #toString()} method.
     * <p/>
     * For example given the ActionLink:
     *
     * <pre class="codeJava">
     * PageLink editLink = <span class="kw">new</span> PageLink(<span class="st">"editLink"</span>, EditCustomer.<span class="kw">class</span>);
     * editLink.setLabel(<span class="st">"Edit Customer"</span>);
     * editLink.setParameter(<span class="st">"customerId"</span>, customerId); </pre>
     *
     * And the page template:
     * <pre class="codeHtml">
     * $<span class="red">editLink</span> </pre>
     *
     * Will render the HTML as:
     * <pre class="codeHtml">
     * &lt;a href="/mycorp/edit-customer.htm?<span class="st">customerId</span>=<span class="red">13490</span>"&gt;<span class="st">Edit Customer</span>&lt;/a&gt; </pre>
     *
     * @param name the attribute name
     * @param value the attribute value
     * @throws IllegalArgumentException if name parameter is null
     */
    public void setParameter(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }

        if (value != null) {
            getParameters().put(name, value);
        } else {
            getParameters().remove(name);
        }
    }

    /**
     * Return the link request parameter values for the given name, or null if
     * the parameter values does not exist.
     *
     * @param name the name of request parameter
     * @return the link request parameter values
     */
    public String[] getParameterValues(String name) {
        if (hasParameters()) {
            Object values = getParameters().get(name);
            if (values instanceof String) {
                return new String[] { values.toString() };
            }
            if (values instanceof String[]) {
                return (String[]) values;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Set the link parameter with the given parameter name and values. If the
     * values are null, the parameter will be removed from the {@link #parameters}.
     *
     * @see #setParameter(String, Object)
     *
     * @param name the attribute name
     * @param values the attribute values
     * @throws IllegalArgumentException if name parameter is null
     */
    public void setParameterValues(String name, Object[] values) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }

        if (values != null) {
            getParameters().put(name, values);
        } else {
            getParameters().remove(name);
        }
    }

    /**
     * Return the AbstractLink parameters Map.
     *
     * @return the AbstractLink parameters Map
     */
    public Map<String, Object> getParameters() {
        if (parameters == null) {
            parameters = new HashMap<String, Object>(4);
        }
        return parameters;
    }

    /**
     * Set the AbstractLink parameter map.
     *
     * @param parameters the link parameter map
     */
    public void setParameters(Map parameters) {
        this.parameters = parameters;
    }

    /**
     * Defines a link parameter that will have its value bound to a matching
     * request parameter. {@link #setParameter(String, Object) setParameter}
     * implicitly defines a parameter as well.
     * <p/>
     * <b>Please note:</b> parameters need only be defined for Ajax requests.
     * For non-Ajax requests, <tt>all</tt> incoming request parameters
     * are bound, whether they are defined or not. This behavior may change in a
     * future release.
     * <p/>
     * <b>Also note:</b> link parameters are bound to request parameters
     * during the {@link #onProcess()} event, so link parameters must be defined
     * in the Page constructor or <tt>onInit()</tt> event.
     *
     * @param name the name of the parameter to define
     */
    public void defineParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }

        Map<String, Object> localParameters = getParameters();
        if (!localParameters.containsKey(name)) {
            localParameters.put(name, null);
        }
    }

    /**
     * Return true if the AbstractLink has parameters, false otherwise.
     *
     * @return true if the AbstractLink has parameters, false otherwise
     */
    public boolean hasParameters() {
        return parameters != null && !parameters.isEmpty();
    }

    /**
     * Return the link "tabindex" attribute value.
     *
     * @return the link "tabindex" attribute value
     */
    public int getTabIndex() {
        return tabindex;
    }

    /**
     * Set the link "tabindex" attribute value.
     *
     * @param tabindex the link "tabindex" attribute value
     */
    public void setTabIndex(int tabindex) {
        this.tabindex = tabindex;
    }

    /**
     * Return the 'title' attribute, or null if not defined. The title
     * attribute acts like tooltip message over the link.
     * <p/>
     * If the title value is null, this method will attempt to find a
     * localized label message in the parent messages using the key:
     * <blockquote>
     * <tt>getName() + ".title"</tt>
     * </blockquote>
     * If not found then the message will be looked up in the
     * <tt>/click-control.properties</tt> file using the same key.
     * <p/>
     * For examle given a <tt>ItemsPage</tt> with the properties file
     * <tt>ItemPage.properties</tt>:
     *
     * <pre class="codeConfig">
     * <span class="st">edit</span>.label=<span class="red">Edit</span>
     * <span class="st">edit</span>.title=<span class="red">Edit Item</span> </pre>
     *
     * The page ActionLink code:
     * <pre class="codeJava">
     * <span class="kw">public class</span> ItemsPage <span class="kw">extends</span> Page {
     *     ActionLink editLink = <span class="kw">new</span> ActionLink(<span class="st">"edit"</span>);
     *     ..
     * } </pre>
     *
     * Will render the ActionLink label and title properties as:
     * <pre class="codeHtml">
     * &lt;a href=".." title="<span class="red">Edit Item</span>"&gt;<span class="red">Edit</span>&lt;/a&gt; </pre>
     *
     * @return the 'title' attribute tooltip message
     */
    public String getTitle() {
        if (title == null) {
            title = getMessage(getName() + ".title");
        }
        return title;
    }

    /**
     * Set the 'title' attribute tooltip message.
     *
     * @see #getTitle()
     *
     * @param value the 'title' attribute tooltip message
     */
    public void setTitle(String value) {
        title = value;
    }

    /**
     * Returns <code>true</code> if both {@link #setImageSrc(String) icon}
     * and {@link #setLabel(String) label} are rendered,
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if both icon and text are rendered,
     * <code>false</code> otherwise
     */
    public boolean isRenderLabelAndImage() {
        return renderLabelAndImage;
    }

    /**
     * Sets whether both {@link #setLabel(String) label} and
     * {@link #setImageSrc(String) icon} are rendered for this
     * link.
     *
     * @param renderLabelAndImage sets the rendering type of the link.
     */
    public void setRenderLabelAndImage(boolean renderLabelAndImage) {
        this.renderLabelAndImage = renderLabelAndImage;
    }

    @Override
    public boolean isAjaxTarget(Context context) {
        String id = getId();
        if (id != null) {
            return context.getRequestParameter(id) != null;
        } else {
            String localName = getName();
            if (localName != null) {
                return localName.equals(context.getRequestParameter(ActionLink.ACTION_LINK));
            }
        }
        return false;
    }

    // Public Methods ---------------------------------------------------------

    /**
     * This method does nothing by default since AbstractLink does not bind to
     * request values.
     */
    public void bindRequestValue() {
    }

    /**
     * Return the link state. The following state is returned:
     * <ul>
     * <li>{@link #getParameters() link parameters}</li>
     * </ul>
     *
     * @return the link state
     */
    public Object getState() {
        if (hasParameters()) {
            return getParameters();
        }
        return null;
    }

    /**
     * Set the link state.
     *
     * @param state the link state to set
     */
    public void setState(Object state) {
        if (state == null) {
            return;
        }

        Map linkState = (Map) state;
        setParameters(linkState);
    }

    /**
     * Render the HTML representation of the anchor link. This method
     * will render the entire anchor link including the tags, the label and
     * any attributes, see {@link #setAttribute(String, String)} for an
     * example.
     * <p/>
     * If the image src is defined then a <tt>&lt;img/&gt;</tt> element will
     * rendered inside the anchor link instead of the label property.
     * <p/>
     * This method invokes the abstract {@link #getHref()} method.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {

        if (isDisabled()) {

            buffer.elementStart("span");
            buffer.appendAttribute("id", getId());
            addStyleClass("disabled");
            buffer.appendAttribute("class", getAttribute("class"));

            if (hasAttribute("style")) {
                buffer.appendAttribute("style", getAttribute("style"));
            }

            buffer.closeTag();

            if (StringUtils.isBlank(getImageSrc())) {
                buffer.append(getLabel());

            } else {
                renderImgTag(buffer);
                if (isRenderLabelAndImage()) {
                    buffer.elementStart("span").closeTag();
                    buffer.append(getLabel());
                    buffer.elementEnd("span");
                }
            }

            buffer.elementEnd("span");

        } else {
            buffer.elementStart(getTag());
            removeStyleClass("disabled");
            buffer.appendAttribute("href", getHref());
            buffer.appendAttribute("id", getId());
            buffer.appendAttributeEscaped("title", getTitle());
            if (getTabIndex() > 0) {
                buffer.appendAttribute("tabindex", getTabIndex());
            }

            appendAttributes(buffer);

            buffer.closeTag();

            if (StringUtils.isBlank(getImageSrc())) {
                buffer.append(getLabel());

            } else {
                renderImgTag(buffer);
                if (isRenderLabelAndImage()) {
                    buffer.elementStart("span").closeTag();
                    buffer.append(getLabel());
                    buffer.elementEnd("span");
                }
            }

            buffer.elementEnd(getTag());
        }
    }

    /**
     * Remove the link state from the session for the given request context.
     *
     * @see #saveState(Context)
     * @see #restoreState(Context)
     *
     * @param context the request context
     */
    public void removeState(Context context) {
        ClickUtils.removeState(this, getName(), context);
    }

    /**
     * Restore the link state from the session for the given request context.
     * <p/>
     * This method delegates to {@link #setState(Object)} to set the
     * link restored state.
     *
     * @see #saveState(Context)
     * @see #removeState(Context)
     *
     * @param context the request context
     */
    public void restoreState(Context context) {
        ClickUtils.restoreState(this, getName(), context);
    }

    /**
     * Save the link state to the session for the given request context.
     * <p/>
     * * This method delegates to {@link #getState()} to retrieve the link state
     * to save.
     *
     * @see #restoreState(Context)
     * @see #removeState(Context)
     *
     * @param context the request context
     */
    public void saveState(Context context) {
        ClickUtils.saveState(this, getName(), context);
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Render the Image tag to the buffer.
     *
     * @param buffer the buffer to render the image tag to
     */
    protected void renderImgTag(HtmlStringBuffer buffer) {
        buffer.elementStart("img");
        buffer.appendAttribute("border", 0);
        buffer.appendAttribute("hspace", 2);
        buffer.appendAttribute("class", "link");

        if (getTitle() != null) {
            buffer.appendAttributeEscaped("alt", getTitle());
        } else {
            buffer.appendAttributeEscaped("alt", getLabel());
        }

        String src = getImageSrc();
        if (StringUtils.isNotBlank(src)) {
            if (src.charAt(0) == '/') {
                src = getContext().getRequest().getContextPath() + src;
            }
            buffer.appendAttribute("src", src);
        }

        buffer.elementEnd();
    }

    /**
     * Render the given link parameters to the buffer.
     * <p/>
     * The parameters will be rendered as URL key/value pairs e.g:
     * "<tt>firstname=john&lastname=smith</tt>".
     * <p/>
     * Multivalued parameters will be rendered with each value sharing the same
     * key e.g: "<tt>name=john&name=susan&name=mary</tt>".
     * <p/>
     * The parameter value will be encoded through
     * {@link ClickUtils#encodeUrl(Object, Context)}.
     *
     * @param buffer the buffer to render the parameters to
     * @param parameters the parameters to render
     * @param context the request context
     */
    protected void renderParameters(HtmlStringBuffer buffer, Map<String, Object> parameters,
        Context context) {

        Iterator<String> i = parameters.keySet().iterator();
        while (i.hasNext()) {
            String paramName = i.next();
            Object paramValue = getParameters().get(paramName);

            // Check for multivalued parameter
            if (paramValue instanceof String[]) {
                String[] paramValues = (String[]) paramValue;
                for (int j = 0; j < paramValues.length; j++) {
                    buffer.append(paramName);
                    buffer.append("=");
                    buffer.append(ClickUtils.encodeUrl(paramValues[j], context));
                    if (j < paramValues.length - 1) {
                        buffer.append("&amp;");
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
                buffer.append("&amp;");
            }
        }
    }

    /**
     * This method binds the submitted request parameters to the link
     * parameters.
     * <p/>
     * For non-Ajax requests this method will bind <tt>all</tt> incoming request
     * parameters to the link. For Ajax requests this method will only bind
     * the parameters already defined on the link.
     *
     * @param context the request context
     */
    @SuppressWarnings("unchecked")
    protected void bindRequestParameters(Context context) {
        HttpServletRequest request = context.getRequest();

        Set<String> parameterNames = null;

        if (context.isAjaxRequest()) {
            parameterNames = getParameters().keySet();
        } else {
            parameterNames = request.getParameterMap().keySet();
        }

        for (String param : parameterNames) {
            String[] values = request.getParameterValues(param);
            // Do not process request parameters that return null values. Null
            // values are only returned if the request parameter is not present.
            // A null value can only occur for Ajax requests which processes
            // parameters defined on the link, not the incoming parameters.
            // The reason for not processing the null value is because it would
            // nullify parametesr that was set during onInit
            if (values == null) {
                continue;
            }

            if (values.length == 1) {
                getParameters().put(param, values[0]);
            } else {
                getParameters().put(param, values);
            }
        }
    }
}
