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
import org.apache.click.control.Submit;
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;
import org.apache.commons.lang.StringUtils;

/**
 * Provides a Action Link control: &nbsp; &lt;a href=""&gt;&lt;/a&gt;.
 *
 * <table class='htmlHeader' cellspacing='6'>
 * <tr><td>
 * <a href='' title='ActionLink Control'>Action Link</a>
 * </td></tr>
 * </table>
 *
 * This control can render the "href" URL attribute using
 * {@link #getHref()}, or the entire ActionLink anchor tag using
 * {@link #toString()}.
 * <p/>
 * ActionLink support invoking control listeners.
 *
 * <h3>ActionLink Example</h3>
 *
 * An example of using ActionLink to call a logout method is illustrated below:
 *
 * <pre class="codeJava">
 * <span class="kw">public class</span> MyPage <span class="kw">extends</span> Page {
 *
 *     <span class="kw">public</span> MyPage() {
 *         ActionLink link = <span class="kw">new</span> ActionLink(<span class="st">"logoutLink"</span>);
 *         link.setListener(<span class="kw">this</span>, <span class="st">"onLogoutClick"</span>);
 *         addControl(link);
 *     }
 *
 *     <span class="kw">public boolean</span> onLogoutClick() {
 *         <span class="kw">if</span> (getContext().hasSession()) {
 *            getContext().getSession().invalidate();
 *         }
 *         setRedirect(LogoutPage.<span class="kw">class</span>);
 *
 *         <span class="kw">return false</span>;
 *     }
 * } </pre>
 *
 * The corresponding template code is below. Note href is evaluated by Velocity
 * to {@link #getHref()}:
 *
 * <pre class="codeHtml">
 * &lt;a href="<span class="blue">$logoutLink</span>.href" title="Click to Logout"&gt;Logout&lt;/a&gt; </pre>
 *
 * ActionLink can also support a value parameter which is accessible
 * using {@link #getValue()}.
 * <p/>
 * For example a products table could include rows
 * of products, each with a get product details ActionLink and add product
 * ActionLink. The ActionLinks include the product's id as a parameter to
 * the {@link #getHref(Object)} method, which is then available when the
 * control is processed:
 *
 * <pre class="codeHtml">
 * &lt;table&gt;
 * <span class="red">#foreach</span> (<span class="blue">$product</span> <span class="red">in</span> <span class="blue">$productList</span>)
 *   &lt;tr&gt;
 *    &lt;td&gt;
 *      $product.name
 *    &lt;/td&gt;
 *    &lt;td&gt;
 *      &lt;a href="<span class="blue">$detailsLink</span>.getHref(<span class="blue">$product</span>.id)" title="Get product information"&gt;Details&lt;/a&gt;
 *    &lt;/td&gt;
 *    &lt;td&gt;
 *      &lt;a href="<span class="blue">$addLink</span>.getHref(<span class="blue">$product</span>.id)" title="Add to basket"&gt;Add&lt;/a&gt;
 *    &lt;/td&gt;
 *   &lt;/tr&gt;
 * <span class="red">#end</span>
 * &lt;/table&gt; </pre>
 *
 * The corresponding Page class for this template is:
 *
 * <pre class="codeJava">
 * <span class="kw">public class</span> ProductsPage <span class="kw">extends</span> Page {
 *
 *     <span class="kw">public</span> ActionLink addLink = <span class="kw">new</span> ActionLink(<span class="st">"addLink"</span>, <span class="kw">this</span>, <span class="st">"onAddClick"</span>);
 *     <span class="kw">public</span> ActionLink detailsLink  = <span class="kw">new</span> ActionLink(<span class="st">"detailsLink"</span>, <span class="kw">this</span>, <span class="st">"onDetailsClick"</span>);
 *     <span class="kw">public</span> List productList;
 *
 *     <span class="kw">public boolean</span> onAddClick() {
 *         <span class="cm">// Get the product clicked on by the user</span>
 *         Integer productId = addLink.getValueInteger();
 *         Product product = getProductService().getProduct(productId);
 *
 *         <span class="cm">// Add product to basket</span>
 *         List basket = (List) getContext().getSessionAttribute(<span class="st">"basket"</span>);
 *         basket.add(product);
 *         getContext().setSessionAttribute(<span class="st">"basket"</span>, basket);
 *
 *         <span class="kw">return true</span>;
 *     }
 *
 *     <span class="kw">public boolean</span> onDetailsClick() {
 *         <span class="cm">// Get the product clicked on by the user</span>
 *         Integer productId = detailsLink.getValueInteger();
 *         Product product = getProductService().getProduct(productId);
 *
 *         <span class="cm">// Store the product in the request and display in the details page</span>
 *         getContext().setRequestAttribute(<span class="st">"product"</span>, product);
 *         setForward(ProductDetailsPage.<span class="kw">class</span>);
 *
 *         <span class="kw">return false</span>;
 *     }
 *
 *     <span class="kw">public void</span> onRender() {
 *         <span class="cm">// Display the list of available products</span>
 *         productList = getProductService().getProducts();
 *     }
 * } </pre>
 *
 * See also the W3C HTML reference:
 * <a class="external" target="_blank" title="W3C HTML 4.01 Specification"
 *    href="http://www.w3.org/TR/html401/struct/links.html#h-12.2">A Links</a>
 *
 * @see org.apache.click.control.AbstractLink
 * @see Submit
 */
public class ActionLink extends AbstractLink {

    // Constants --------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    /** The action link parameter name: &nbsp; <tt>actionLink</tt>. */
    public static final String ACTION_LINK = "actionLink";

    /** The value parameter name: &nbsp; <tt>value</tt>. */
    public static final String VALUE = "value";

    // Instance Variables -----------------------------------------------------

    /** The link is clicked. */
    protected boolean clicked;

    // Constructors -----------------------------------------------------------

    /**
     * Create an ActionLink for the given name.
     * <p/>
     * Please note the name 'actionLink' is reserved as a control request
     * parameter name and cannot be used as the name of the control.
     *
     * @param name the action link name
     * @throws IllegalArgumentException if the name is null
     */
    public ActionLink(String name) {
        setName(name);
    }

    /**
     * Create an ActionLink for the given name and label.
     * <p/>
     * Please note the name 'actionLink' is reserved as a control request
     * parameter name and cannot be used as the name of the control.
     *
     * @param name the action link name
     * @param label the action link label
     * @throws IllegalArgumentException if the name is null
     */
    public ActionLink(String name, String label) {
        setName(name);
        setLabel(label);
    }

    /**
     * Create an ActionLink for the given listener object and listener
     * method.
     *
     * @param listener the listener target object
     * @param method the listener method to call
     * @throws IllegalArgumentException if the name, listener or method is null
     * or if the method is blank
     */
    public ActionLink(Object listener, String method) {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener parameter");
        }
        if (StringUtils.isBlank(method)) {
            throw new IllegalArgumentException("Blank listener method");
        }
        setListener(listener, method);
    }

    /**
     * Create an ActionLink for the given name, listener object and listener
     * method.
     * <p/>
     * Please note the name 'actionLink' is reserved as a control request
     * parameter name and cannot be used as the name of the control.
     *
     * @param name the action link name
     * @param listener the listener target object
     * @param method the listener method to call
     * @throws IllegalArgumentException if the name, listener or method is null
     * or if the method is blank
     */
    public ActionLink(String name, Object listener, String method) {
        setName(name);
        if (listener == null) {
            throw new IllegalArgumentException("Null listener parameter");
        }
        if (StringUtils.isBlank(method)) {
            throw new IllegalArgumentException("Blank listener method");
        }
        setListener(listener, method);
    }

    /**
     * Create an ActionLink for the given name, label, listener object and
     * listener method.
     * <p/>
     * Please note the name 'actionLink' is reserved as a control request
     * parameter name and cannot be used as the name of the control.
     *
     * @param name the action link name
     * @param label the action link label
     * @param listener the listener target object
     * @param method the listener method to call
     * @throws IllegalArgumentException if the name, listener or method is null
     * or if the method is blank
     */
    public ActionLink(String name, String label, Object listener,
                      String method) {

        setName(name);
        setLabel(label);
        if (listener == null) {
            throw new IllegalArgumentException("Null listener parameter");
        }
        if (StringUtils.isBlank(method)) {
            throw new IllegalArgumentException("Blank listener method");
        }
        setListener(listener, method);
    }

    /**
     * Create an ActionLink with no name defined. <b>Please note</b> the
     * control's name must be defined before it is valid.
     */
    public ActionLink() {
    }

    // Public Attributes ------------------------------------------------------

    /**
     * Returns true if the ActionLink was clicked, otherwise returns false.
     *
     * @return true if the ActionLink was clicked, otherwise returns false.
     */
    public boolean isClicked() {
        return clicked;
    }

    /**
     * Return the ActionLink anchor &lt;a&gt; tag href attribute for the
     * given value. This method will encode the URL with the session ID
     * if required using <tt>HttpServletResponse.encodeURL()</tt>.
     *
     * @param value the ActionLink value parameter
     * @return the ActionLink HTML href attribute
     */
    public String getHref(Object value) {
        Context context = getContext();
        String uri = ClickUtils.getRequestURI(context.getRequest());

        HtmlStringBuffer buffer =
                new HtmlStringBuffer(uri.length() + getName().length() + 40);

        buffer.append(uri);
        buffer.append("?");
        buffer.append(ACTION_LINK);
        buffer.append("=");
        buffer.append(getName());
        if (value != null) {
            buffer.append("&amp;");
            buffer.append(VALUE);
            buffer.append("=");
            buffer.append(ClickUtils.encodeUrl(value, context));
        }

        if (hasParameters()) {
            for (String paramName : getParameters().keySet()) {
                if (!paramName.equals(ACTION_LINK) && !paramName.equals(VALUE)) {
                    Object paramValue = getParameters().get(paramName);

                    // Check for multivalued parameter
                    if (paramValue instanceof String[]) {
                        String[] paramValues = (String[]) paramValue;
                        for (int j = 0; j < paramValues.length; j++) {
                            buffer.append("&amp;");
                            buffer.append(paramName);
                            buffer.append("=");
                            buffer.append(ClickUtils.encodeUrl(paramValues[j],
                                context));
                        }
                    } else {
                        if (paramValue != null) {
                            buffer.append("&amp;");
                            buffer.append(paramName);
                            buffer.append("=");
                            buffer.append(ClickUtils.encodeUrl(paramValue,
                                                               context));
                        }
                    }
                }
            }
        }

        return context.getResponse().encodeURL(buffer.toString());
    }

    /**
     * Return the ActionLink anchor &lt;a&gt; tag href attribute value.
     *
     * @return the ActionLink anchor &lt;a&gt; tag HTML href attribute value
     */
    @Override
    public String getHref() {
        return getHref(getValue());
    }

    /**
     * Set the name of the Control. Each control name must be unique in the
     * containing Page model or the containing Form.
     * <p/>
     * Please note the name 'actionLink' is reserved as a control request
     * parameter name and cannot be used as the name of the control.
     *
     * @see org.apache.click.Control#setName(String)
     *
     * @param name of the control
     * @throws IllegalArgumentException if the name is null
     */
    @Override
    public void setName(String name) {
        if (ACTION_LINK.equals(name)) {
            String msg = "Invalid name '" + ACTION_LINK + "'. This name is "
                + "reserved for use as a control request parameter name";
            throw new IllegalArgumentException(msg);
        }
        super.setName(name);
    }

    /**
     * Set the parent of the ActionLink.
     *
     * @see org.apache.click.Control#setParent(Object)
     *
     * @param parent the parent of the Control
     * @throws IllegalStateException if {@link #name} is not defined
     * @throws IllegalArgumentException if the given parent instance is
     * referencing <tt>this</tt> object: <tt>if (parent == this)</tt>
     */
    @Override
    public void setParent(Object parent) {
        if (parent == this) {
            throw new IllegalArgumentException("Cannot set parent to itself");
        }
        if (getName() == null) {
            String msg = "ActionLink name not defined.";
            throw new IllegalArgumentException(msg);
        }
        this.parent = parent;
    }

    /**
     * Returns the ActionLink value if the action link was processed and has
     * a value, or null otherwise.
     *
     * @return the ActionLink value if the ActionLink was processed
     */
    public String getValue() {
        return getParameter(VALUE);
    }

    /**
     * Returns the action link <tt>Double</tt> value if the action link was
     * processed and has a value, or null otherwise.
     *
     * @return the action link <tt>Double</tt> value if the action link was processed
     *
     * @throws NumberFormatException if the value cannot be parsed into a Double
     */
    public Double getValueDouble() {
        String value = getValue();
        if (value != null) {
            return Double.valueOf(value);
        } else {
            return null;
        }
    }

    /**
     * Returns the ActionLink <tt>Integer</tt> value if the ActionLink was
     * processed and has a value, or null otherwise.
     *
     * @return the ActionLink <tt>Integer</tt> value if the action link was processed
     *
     * @throws NumberFormatException if the value cannot be parsed into an Integer
     */
    public Integer getValueInteger() {
        String value = getValue();
        if (value != null) {
            return Integer.valueOf(value);
        } else {
            return null;
        }
    }

    /**
     * Returns the ActionLink <tt>Long</tt> value if the ActionLink was
     * processed and has a value, or null otherwise.
     *
     * @return the ActionLink <tt>Long</tt> value if the action link was processed
     *
     * @throws NumberFormatException if the value cannot be parsed into a Long
     */
    public Long getValueLong() {
        String value = getValue();
        if (value != null) {
            return Long.valueOf(value);
        } else {
            return null;
        }
    }

    /**
     * Set the ActionLink value.
     *
     * @param value the ActionLink value
     */
    public void setValue(String value) {
        setParameter(VALUE, value);
    }

    /**
     * Set the value of the ActionLink using the given object.
     *
     * @param object the object value to set
     */
    public void setValueObject(Object object) {
        if (object != null) {
            setValue(object.toString());
        } else {
            setValue(null);
        }
    }

    /**
     * This method binds the submitted request value to the ActionLink's
     * value.
     */
    @Override
    public void bindRequestValue() {
        Context context = getContext();
        if (context.isMultipartRequest()) {
            return;
        }

        clicked = getName().equals(context.getRequestParameter(ACTION_LINK));

        if (clicked) {
            String value = context.getRequestParameter(VALUE);
            if (value != null) {
                setValue(value);
            }
            bindRequestParameters(context);
        }
    }

    // Public Methods ---------------------------------------------------------

    /**
     * This method will set the {@link #isClicked()} property to true if the
     * ActionLink was clicked, and if an action callback listener was set
     * this will be invoked.
     *
     * @see org.apache.click.Control#onProcess()
     *
     * @return true to continue Page event processing or false otherwise
     */
    @Override
    public boolean onProcess() {
        bindRequestValue();

        if (isClicked()) {
            dispatchActionEvent();
        }
        return true;
    }
}
