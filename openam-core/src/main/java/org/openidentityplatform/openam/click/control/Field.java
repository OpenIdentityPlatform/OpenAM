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

import org.apache.click.Stateful;
import org.apache.click.control.Checkbox;
import org.apache.click.control.FieldSet;
import org.apache.commons.lang.StringUtils;
import org.openidentityplatform.openam.click.Context;
import org.openidentityplatform.openam.click.Control;
import org.openidentityplatform.openam.click.Page;
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.openidentityplatform.openam.click.util.ContainerUtils;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;

/**
 * Provides an abstract form Field control. Field controls are contained by
 * the {@link Form} control which will orchestrate the processing and
 * rendering of the contained fields. All Form field controls must extend this
 * abstract class.
 *
 * <h3><a name="field-processing"></a>Field Processing</h3>
 *
 * <h4><a name="post-requests"></a>Post Requests</h4>
 *
 * When processing POST requests forms typically invoke the {@link #onProcess()}
 * method on all its fields. The Field <tt>onProcess()</tt> method is used
 * to bind the fields request value, validate the submission and invoke any
 * control listener method. If the <tt>onProcess()</tt> method returns true
 * the form will continue processing fields, otherwise the form will abort
 * further processing.
 * <p/>
 * The body of the Field <tt>onProcess()</tt> method is detailed below.
 *
 * <pre class="codeJava">
 * <span class="kw">public boolean</span> onProcess() {
 *     bindRequestValue();
 *
 *     <span class="kw">if</span> (getValidate()) {
 *         validate();
 *     }
 *
 *     registerActionEvent();
 *
 *     <span class="kw">return true</span>;
 * } </pre>
 *
 * The Field methods called by <tt>onProcess()</tt> include:
 *
 * <dl>
 * <dt>{@link #bindRequestValue()}</dt>
 * <dd>This method will bind the HTTP request value to the Field's value.
 * </dd>
 * <dt>{@link #getValidate()}</dt>
 * <dd>This method will return true if the Field should validate itself. This
 * value is generally inherited from the parent Form, however the Field can
 * override this value and specify whether it should be validated.
 * </dd>
 * <dt>{@link #validate()}</dt>
 * <dd>This method will validate the submitted Field value. If the submitted
 * value is not valid this method should set the Field {@link #error} property,
 * which can be rendered by the Form.
 * </dd>
 * <dt>{@link #dispatchActionEvent()}</dt>
 * <dd>This method will register any Control action listener method which has be
 * defined for the Field.
 * </dd>
 * </dl>
 *
 * Field subclasses generally only have to override the <tt>validate()</tt>
 * method, and possibly the <tt>bindRequestValue()</tt> method, to provide their
 * own behaviour.
 *
 * <h4><a name="get-requests"></a>Get Requests</h4>
 *
 * When processing GET requests a Page's Form will typically perform no
 * processing and simply render itself and its Fields.
 *
 * <h3><a name="rendering"></a>Rendering</h3>
 *
 * Field subclasses must override the {@link #render(org.openidentityplatform.openam.click.util.HtmlStringBuffer)}
 * method to enable themselves to be rendered as HTML. With the increasing use of
 * AJAX, Fields should render themselves as valid XHTML, so that they may be parsed
 * correctly and used as the <tt>innerHtml</tt> in the DOM.
 * <p/>
 * When a Form object renders a Field using autolayout, it renders the
 * Field in a table row using the Field's {@link #label} attribute, its
 * {@link #error} attribute if defined, and the Fields
 * {@link #render(org.openidentityplatform.openam.click.util.HtmlStringBuffer)} method.
 * <p/>
 * To assist with rendering valid HTML Field subclasses can use the
 * {@link org.apache.click.util.HtmlStringBuffer} class.
 *
 * <h3><a name="message-resources"></a>Message Resources and Internationalization (i18n)</h3>
 *
 * Fields support a hierarchy of resource bundles for displaying validation
 * error messages and display messages. These localized messages can be accessed
 * through the methods:
 *
 * <ul>
 * <li>{@link #getMessage(String)}</li>
 * <li>{@link #getMessage(String, Object...)}</li>
 * <li>{@link #getMessages()}</li>
 * <li>{@link #setErrorMessage(String)}</li>
 * <li>{@link #setErrorMessage(String, Object)}</li>
 * </ul>
 *
 * Fields automatically pick up localized messages where applicable. Please see
 * the following methods on how to customize these messages:
 * <ul>
 * <li>{@link #getLabel()}</li>
 * <li>{@link #getTitle()}</li>
 * <li>{@link #getHelp()}</li>
 * <li>{@link #setErrorMessage(String)}</li>
 * <li>{@link #setErrorMessage(String, Object)}</li>
 * </ul>
 *
 * <a name="message-resolve-order" href="#"></a>
 * The order in which localized messages resolve are:
 * <dl>
 * <dt style="font-weight:bold">Page scope messages</dt>
 * <dd>Message lookups are first resolved to the Pages message bundle if it
 * exists. For example a <tt>Login</tt> page may define the message properties:
 *
 * <pre class="codeConfig">
 * /com/mycorp/page/Login.properties </pre>
 *
 * If you want messages to be used only for a specific Page, this is where
 * to place them.
 * </dd>
 *
 * <dt style="font-weight:bold;margin-top:1em;">Global page scope messages</dt>
 * <dd>Next message lookups are resolved to the global pages message bundle if it
 * exists.
 *
 * <pre class="codeConfig">
 * /click-page.properties </pre>
 *
 * If you want messages to be used across your entire application this is where
 * to place them.
 * </dd>
 *
 * <dt style="font-weight:bold">Control scope messages</dt>
 * <dd>Next message lookups are resolved to the Control message bundle if it
 * exists. For example a <tt>CustomTextField</tt> control may define the
 * message properties:
 *
 * <pre class="codeConfig">
 * /com/mycorp/control/CustomTextField.properties </pre>
 * </dd>
 *
 * <dt style="font-weight:bold">Global control scope messages</dt>
 * <dd>Finally message lookups are resolved to the global application control
 * message bundle if the message has not already found. The global control
 * properties file is:
 *
 * <pre class="codeConfig">
 * /click-control.properties </pre>
 *
 * You can modify these properties by copying this file into your applications
 * root class path and editing these properties.
 * <p/>
 * Note when customizing the message properties you must include all the
 * properties, not just the ones you want to override.
 * </dd>
 * </dl>
 */
public abstract class Field extends AbstractControl implements Stateful {

    // Constants --------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // Instance Variables -----------------------------------------------------

    /** The Field disabled value. */
    protected boolean disabled;

    /** The Field error message. */
    protected String error;

    /** The request focus flag. */
    protected boolean focus;

    /** The parent Form. */
    protected Form form;

    /** The Field help text. */
    protected String help;

    /** The Field label. */
    protected String label;

    /** The field label "style" attribute value. */
    protected String labelStyle;

    /** The field label "class" attribute value. */
    protected String labelStyleClass;

    /** The field's parent element "style" attribute hint. */
    protected String parentStyleHint;

    /** The field's parent element "class" attribute hint. */
    protected String parentStyleClassHint;

    /** The Field is readonly flag. */
    protected boolean readonly;

    /** The Field is required flag. */
    protected boolean required;

    /** The Field 'tabindex' attribute. */
    protected int tabindex;

    /** The Field 'title' attribute, which acts as a tooltip help message. */
    protected String title;

    /** The Field is trimmed flag, default value is true. */
    protected boolean trim = true;

    /**
     * The validate Field value <tt>onProcess()</tt> invocation flag.
     */
    protected Boolean validate;

    /** The Field value. */
    protected String value;

    // Constructors -----------------------------------------------------------

    /**
     * Construct a new Field object.
     */
    public Field() {
    }

    /**
     * Construct the Field with the given name.
     *
     * @param name the name of the Field
     */
    public Field(String name) {
        setName(name);
    }

    /**
     * Construct the Field with the given name and label.
     *
     * @param name the name of the Field
     * @param label the label of the Field
     */
    public Field(String name, String label) {
        setName(name);
        setLabel(label);
    }

    // Public Attributes ------------------------------------------------------

    /**
     * Set the parent of the Field.
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
        // Guard against fields without names, as fields would throw
        // exceptions when binding to request value.
        if (StringUtils.isBlank(getName())) {
            String msg = "Field name not defined: " + getClass().getName();
            throw new IllegalArgumentException(msg);
        }
        this.parent = parent;
    }

    /**
     * Return true if the Field is disabled. The Field will also be disabled
     * if the parent FieldSet or Form is disabled.
     *
     * @see #setDisabled(boolean)
     *
     * @return true if the Field is disabled
     */
    public boolean isDisabled() {
        Control control = this;

        // Check parents for instances of either FieldSet or Form
        while (control.getParent() != null && !(control.getParent() instanceof Page)) {
            control = (Control) control.getParent();
            if (control instanceof FieldSet) {
                FieldSet fieldSet = (FieldSet) control;
                if (fieldSet.isDisabled()) {
                    return true;
                } else {
                    return disabled;
                }
            } else if (control instanceof Form) {
                Form localForm = (Form) control;
                if (localForm.isDisabled()) {
                    return true;
                } else {
                    return disabled;
                }
            }
        }
        return disabled;
    }

    /**
     * Set the Field disabled flag. Disabled fields are not processed nor
     * validated and their action event is not fired.
     * <p/>
     * <b>Important Note</b>: an HTML form POST does not submit disabled fields
     * values. Similarly disabled Click fields do not get processed or validated.
     * However, JavaScript is often used to <tt>enable</tt> fields prior to
     * submission. Click is smart enough to recognize when a field was enabled
     * this way by checking if the field has an incoming request parameter.
     * If a field is disabled but has an incoming request parameter, Click will
     * <tt>"enable"</tt> the field and process it.
     * <p/>
     * <b>Caveat</b>: Unfortunately the above behavior does not apply to
     * {@link Checkbox} and {@link Radio} buttons. An HTML form POST for a
     * <tt>disabled</tt> checkbox/radio is the same as for an <tt>unchecked</tt>
     * checkbox/radio. In neither case is a value submitted to the server and
     * Click cannot make the distinction whether the checkbox/radio is disabled
     * or unchecked.
     *
     * @param disabled the Field disabled flag
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Return the validation error message if the Field is not valid, or null
     * if valid.
     *
     * @return the Field validation error message, or null if valid
     */
    public String getError() {
        return error;
    }

    /**
     * Set the Field validation error message. If the error message is not null
     * the Field is invalid, otherwise it is valid.
     *
     * @param error the validation error message
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Return true if the field has requested focus.
     *
     * @return true if the field has requested focus
     */
    public boolean getFocus() {
        return focus;
    }

    /**
     * Set the Field request focus flag.
     *
     * @param focus the request focus flag
     */
    public void setFocus(boolean focus) {
        this.focus = focus;
    }

    /**
     * Return the Field focus JavaScript.
     *
     * @return the Field focus JavaScript
     */
    public String getFocusJavaScript() {
        HtmlStringBuffer buffer = new HtmlStringBuffer(32);

        buffer.append("setFocus('");
        buffer.append(getId());
        buffer.append("');");

        return buffer.toString();
    }

    /**
     * Return the parent Form containing the Field or null if no form is present
     * in the parent hierarchy.
     *
     * @return the parent Form containing the Field
     */
    public Form getForm() {
        if (form == null) {
            // Find form in parent hierarchy
            form = ContainerUtils.findForm(this);
        }
        return form;
    }

    /**
     * Set the Field's the parent <tt>Form</tt>.
     *
     * @param form Field's parent <tt>Form</tt>
     */
    public void setForm(Form form) {
        this.form = form;
    }

    /**
     * Return the field help text.
     * <p/>
     * If the help value is null, this method will attempt to find a
     * localized help message in the parent messages using the key:
     * <blockquote>
     * <tt>getName() + ".help"</tt>
     * </blockquote>
     * If not found then the message will be looked up in the
     * <tt>/click-control.properties</tt> file using the same key.
     * <p/>
     * For example given a <tt>CustomerPage</tt> with the properties file
     * <tt>CustomerPage.properties</tt>:
     *
     * <pre class="codeConfig">
     * <span class="st">name</span>.label=<span class="red">Customer Name</span>
     * <span class="st">name</span>.help=<span class="red">Full name or Business name</span> </pre>
     *
     * The page TextField code:
     * <pre class="codeJava">
     * <span class="kw">public class</span> CustomerPage <span class="kw">extends</span> Page {
     *     TextField nameField = <span class="kw">new</span> TextField(<span class="st">"name"</span>);
     *     ..
     * } </pre>
     *
     * Will render the TextField label and title properties as:
     * <pre class="codeHtml">
     * &lt;td&gt;&lt;label&gt;<span class="red">Customer Name</span>&lt;/label&gt;&lt;/td&gt;
     * &lt;td&gt;&lt;input type="text" name="<span class="st">name</span>"/&gt; &lt;span="<span class="red">Full name or Business name</span>"/&gt;/td&gt; </pre>
     *
     * How the help text is rendered is depends upon the Field subclass.
     *
     * @return the help text of the Field
     */
    public String getHelp() {
        if (help == null) {
            help = getMessage(getName() + ".help");

            if (help != null) {
                if (help.indexOf("$context") != -1) {
                    help = StringUtils.replace(help, "$context", getContext().getRequest().getContextPath());

                } else if (help.indexOf("${context}") != -1) {
                    help = StringUtils.replace(help, "${context}", getContext().getRequest().getContextPath());
                }
            }
        }
        return help;
    }

    /**
     * Set the Field help text.
     *
     * @param help the help text of the Field
     */
    public void setHelp(String help) {
        this.help = help;
    }

    /**
     * Return true if the Field type is hidden (&lt;input type="hidden"/&gt;) or
     * false otherwise. By default this method returns false.
     *
     * @return false
     */
    public boolean isHidden() {
        return false;
    }

    /**
     * Return the Form and Field id appended: &nbsp; "<tt>form-field</tt>"
     * <p/>
     * Use the field the "id" attribute value if defined, or the name otherwise.
     *
     * @see org.apache.click.Control#getId()
     *
     * @return HTML element identifier attribute "id" value
     */
    @Override
    public String getId() {
        if (hasAttributes() && getAttributes().containsKey("id")) {
            return getAttribute("id");

        } else {
            String fieldName = getName();

            if (fieldName == null) {
                // If fieldName is null, exit early
                return null;
            }

            Form parentForm = getForm();
            String formId = (parentForm != null) ? parentForm.getId() + "_" : "";
            String id = formId + fieldName;

            if (id.indexOf('/') != -1) {
                id = id.replace('/', '_');
            }
            if (id.indexOf(' ') != -1) {
                id = id.replace(' ', '_');
            }
            if (id.indexOf('<') != -1) {
                id = id.replace('<', '_');
            }
            if (id.indexOf('>') != -1) {
                id = id.replace('>', '_');
            }
            if (id.indexOf('.') != -1) {
                id = id.replace('.', '_');
            }

            return id;
        }
    }

    /**
     * Return the field display label.
     * <p/>
     * If the label value is null, this method will attempt to find a
     * localized label message in the parent messages using the key:
     * <blockquote>
     * <tt>getName() + ".label"</tt>
     * </blockquote>
     * If not found then the message will be looked up in the
     * <tt>/click-control.properties</tt> file using the same key.
     * If a value is still not found, the Field name will be converted
     * into a label using the method: {@link ClickUtils#toLabel(String)}
     * <p/>
     * For example given a <tt>CustomerPage</tt> with the properties file
     * <tt>CustomerPage.properties</tt>:
     *
     * <pre class="codeConfig">
     * <span class="st">name</span>.label=<span class="red">Customer Name</span>
     * <span class="st">name</span>.title=<span class="red">Full name or Business name</span> </pre>
     *
     * The page TextField code:
     * <pre class="codeJava">
     * <span class="kw">public class</span> CustomerPage <span class="kw">extends</span> Page {
     *     TextField nameField = <span class="kw">new</span> TextField(<span class="st">"name"</span>);
     *     ..
     * } </pre>
     *
     * Will render the TextField label and title properties as:
     * <pre class="codeHtml">
     * &lt;td&gt;&lt;label&gt;<span class="red">Customer Name</span>&lt;/label&gt;&lt;/td&gt;
     * &lt;td&gt;&lt;input type="text" name="<span class="st">name</span>" title="<span class="red">Full name or Business name</span>"/&gt;&lt;/td&gt; </pre>
     *
     * When a label value is not set, or defined in any properties files, then
     * its value will be created from the Fields name.
     * <p/>
     * For example given the TextField code:
     *
     * <pre class="codeJava">
     * TextField nameField = <span class="kw">new</span> TextField(<span class="st">"faxNumber"</span>);  </pre>
     *
     * Will render the TextField label as:
     * <pre class="codeHtml">
     * &lt;td&gt;&lt;label&gt;<span class="red">Fax Number</span>&lt;/label&gt;&lt;/td&gt;
     * &lt;td&gt;&lt;input type="text" name="<span class="st">faxNumber</span>"/&gt;&lt;/td&gt; </pre>
     *
     * @return the display label of the Field
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
     * Set the Field display caption.
     *
     * @param label the display label of the Field
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Return the field label "style" attribute value.
     *
     * @see #setLabelStyle(java.lang.String)
     *
     * @return the field label "style" attribute value
     */
    public String getLabelStyle() {
        return labelStyle;
    }

    /**
     * Set the field label "style" attribute value.
     * <p/>
     * <b>Please note</b>: the label is rendered by the containing Form
     * or container, not the field itself. It is up to the parent Form
     * (or container) on how to apply the label style.
     * <p/>
     * <pre class="prettyprint">
     * nameField.setLabelStyle("color: green; font-weight: bold");</pre>
     *
     * @param value the field label "style" attribute value
     */
    public void setLabelStyle(String value) {
        this.labelStyle = value;
    }

    /**
     * Return the field label "class" attribute value.
     *
     * @see #setLabelStyleClass(java.lang.String)
     *
     * @return the field label "class" attribute value
     */
    public String getLabelStyleClass() {
        return labelStyleClass;
    }

    /**
     * Set the field label "class" attribute value.
     * <p/>
     * <b>Please note</b>: the label is rendered by the containing Form
     * or container, not the field itself. It is up to the parent Form
     * (or container) on how to apply the label style class.
     *
     * @param value the field label "class" attribute value
     */
    public void setLabelStyleClass(String value) {
        this.labelStyleClass = value;
    }

    /**
     * Return the field's parent "style" attribute hint.
     *
     * @see #setParentStyleHint(java.lang.String)
     *
     * @return the field's parent "style" attribute hint
     */
    public String getParentStyleHint() {
        return parentStyleHint;
    }

    /**
     * Set the field's parent "style" attribute hint.
     * <p/>
     * <pre class="prettyprint">
     * nameField.setParentStyleHint("margin-bottom; 10px");</pre>
     *
     * <b>Please note:</b>The field's parent style provides a hint to Forms
     * (or other containers) what style to render, but it is up to the
     * Form (or container) implementation how the style will be applied.
     * For example, Form will render the parent style on the table cells
     * containing the field and label.
     *
     * @param styleHint the field's parent "style" attribute hint
     */
    public void setParentStyleHint(String styleHint) {
        this.parentStyleHint = styleHint;
    }

    /**
     * Return the field's parent "class" attribute hint.
     *
     * @see #setParentStyleClassHint(java.lang.String)
     *
     * @return the field's parent "class" attribute hint
     */
    public String getParentStyleClassHint() {
        return parentStyleClassHint;
    }

    /**
     * Set the field's parent "class" attribute hint.
     * <p/>
     * <b>Please note:</b>The parent style class provides a hint to Forms
     * (or other containers) which CSS class to render, but it is up to the
     * Form (or container) implementation how the style class will be applied.
     * For example, Form will render the parent style class on the table cells
     * containing the field and label.
     *
     * @param styleClassHint the field parent "class" attribute hint
     */
    public void setParentStyleClassHint(String styleClassHint) {
        this.parentStyleClassHint = styleClassHint;
    }

    /**
     * The callback listener will only be called during processing if the Field
     * value is valid. If the field has validation errors the listener will not
     * be called.
     *
     * @see org.apache.click.Control#getName()
     *
     * @param listener the listener object with the named method to invoke
     * @param method the name of the method to invoke
     */
    @Override
    public void setListener(Object listener, String method) {
        super.setListener(listener, method);
    }

    /**
     * Return true if the Field is a readonly. The Field will also be readonly
     * if the parent FieldSet or Form is readonly.
     *
     * @return true if the Field is a readonly
     */
    public boolean isReadonly() {
        Control control = this;

        // Check parents for instances of either FieldSet or Form
        while (control.getParent() != null && !(control.getParent() instanceof Page)) {
            control = (Control) control.getParent();
            if (control instanceof FieldSet) {
                FieldSet fieldSet = (FieldSet) control;
                if (fieldSet.isReadonly()) {
                    return true;
                } else {
                    return readonly;
                }
            } else if (control instanceof Form) {
                Form localForm = (Form) control;
                if (localForm.isReadonly()) {
                    return true;
                } else {
                    return readonly;
                }
            }
        }
        return readonly;
    }

    /**
     * Set the Field readonly flag.
     *
     * @param readonly the Field readonly flag
     */
    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    /**
     * Return true if the Field's value is required.
     *
     * @return true if the Field's value is required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Set the Field required status.
     *
     * @param required set the Field required status
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * Return the field "tabindex" attribute value.
     *
     * @return the field "tabindex" attribute value
     */
    public int getTabIndex() {
        return tabindex;
    }

    /**
     * Set the field "tabindex" attribute value.
     *
     * @param tabindex the field "tabindex" attribute value
     */
    public void setTabIndex(int tabindex) {
        this.tabindex = tabindex;
    }

    /**
     * Return the field CSS "text-align" style, or null if not defined.
     *
     * @return the field CSS "text-align" style, or null if not defined.
     */
    public String getTextAlign() {
        return getStyle("text-align");
    }

    /**
     * Set the field CSS horizontal "text-align" style.
     *
     * @param align the CSS "text-align" value: <tt>["left", "right", "center"]</tt>
     */
    public void setTextAlign(String align) {
        setStyle("text-align", align);
    }

    /**
     * Return the 'title' attribute, or null if not defined. The title
     * attribute acts like tooltip message over the Field.
     * <p/>
     * If the title value is null, this method will attempt to find a
     * localized title message in the parent messages using the key:
     * <blockquote>
     * <tt>getName() + ".title"</tt>
     * </blockquote>
     * If not found then the message will be looked up in the
     * <tt>/click-control.properties</tt> file using the same key. If still
     * not found the title will be left as null and will not be rendered.
     * <p/>
     * For example given a <tt>CustomerPage</tt> with the properties file
     * <tt>CustomerPage.properties</tt>:
     *
     * <pre class="codeConfig">
     * <span class="st">name</span>.label=<span class="red">Customer Name</span>
     * <span class="st">name</span>.title=<span class="red">Full name or Business name</span> </pre>
     *
     * The page TextField code:
     * <pre class="codeJava">
     * <span class="kw">public class</span> CustomerPage <span class="kw">extends</span> Page {
     *     TextField nameField = <span class="kw">new</span> TextField(<span class="st">"name"</span>);
     *     ..
     * } </pre>
     *
     * Will render the TextField label and title properties as:
     * <pre class="codeHtml">
     * &lt;td&gt;&lt;label&gt;<span class="red">Customer Name</span>&lt;/label&gt;&lt;/td&gt;
     * &lt;td&gt;&lt;input type="text" name="<span class="st">name</span>" title="<span class="red">Full name or Business name</span>"/&gt;&lt;/td&gt; </pre>
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
     * @param value the 'title' attribute tooltip message
     */
    public void setTitle(String value) {
        title = value;
    }

    /**
     * Return true if the Field request value should be trimmed, false otherwise.
     * The default value is <tt>"true"</tt>.
     *
     * @return true if the Field request value should be trimmed, false otherwise
     */
    public boolean isTrim() {
        return trim;
    }

    /**
     * Set the trim flag to true if the Field request value should be trimmed,
     * false otherwise.
     *
     * @param trim true if the Field request value should be trimmed, false
     * otherwise
     */
    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    /**
     * Return true if the Field should validate itself when being processed.
     * <p/>
     * If the validate attribute for the Field is not explicitly set, this
     * method will return the validation status of its parent Form, see
     * {@link Form#getValidate()}. If the Field validate attribute is not set
     * and the parent Form is not set this method will return true.
     * <p/>
     * This method is called by the {@link #onProcess()} method to determine
     * whether the the Field {@link #validate()} method should be invoked.
     *
     * @return true if the Field should validate itself when being processed.
     */
    public boolean getValidate() {
        if (validate != null) {
            return validate;

        } else if (getForm() != null) {
            return getForm().getValidate();

        } else {
            return true;
        }
    }

    /**
     * Set the validate Field value when being processed flag.
     *
     * @param validate the field value when processed
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    /**
     * Return the field JavaScript client side validation function.
     * <p/>
     * The function name must follow the format <tt>validate_[id]</tt>, where
     * the id is the DOM element id of the fields focusable HTML element, to
     * ensure the function has a unique name.
     *
     * @return the field JavaScript client side validation function
     */
    public String getValidationJavaScript() {
        return null;
    }

    /**
     * Return true if the Field is valid after being processed, or false
     * otherwise. If the Field has no error message after
     * {@link org.apache.click.Control#onProcess()} has been invoked it is considered to be
     * valid.
     *
     * @return true if the Field is valid after being processed
     */
    public boolean isValid() {
        return (error == null);
    }

    /**
     * Return the Field value.
     *
     * @return the Field value
     */
    public String getValue() {
        return (value != null) ? value : "";
    }

    /**
     * Set the Field value.
     *
     * @param value the Field value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Return the object representation of the Field value. This method will
     * return a string value, or null if the string value is null or is zero
     * length.
     * <p/>
     * Specialized object field subclasses should override this method to
     * return a non string object. For examples a <tt>DoubleField</tt> would
     * return a <tt>Double</tt> value instead.
     *
     * @return the object representation of the Field value
     */
    public Object getValueObject() {
        if (value == null || value.length() == 0) {
            return null;
        } else {
            return value;
        }
    }

    /**
     * Set the value of the field using the given object.
     *
     * @param object the object value to set
     */
    public void setValueObject(Object object) {
        if (object != null) {
            value = object.toString();
        }
    }

    /**
     * Return the width CSS "width" style, or null if not defined.
     *
     * @return the CSS "width" style attribute, or null if not defined
     */
    public String getWidth() {
        return getStyle("width");
    }

    /**
     * Set the the CSS "width" style attribute.
     *
     * @param value the CSS "width" style attribute
     */
    public void setWidth(String value) {
        setStyle("width", value);
    }

    // Public Methods ---------------------------------------------------------

    /**
     * This method binds the submitted request value to the Field's value.
     * <p/>
     * <b>Please note:</b> while it is possible to explicitly bind the field
     * value by invoking this method directly, it is recommended to use the
     * "<tt>bind</tt>" utility methods in {@link org.apache.click.util.ClickUtils}
     * instead. See {@link org.apache.click.util.ClickUtils#bind(org.apache.click.control.Field)}
     * for more details.
     */
    public void bindRequestValue() {
        setValue(getRequestValue());
    }

    /**
     * Return the Field state. The following state is returned:
     * <ul>
     * <li>{@link #getValue() field value}</li>
     * </ul>
     *
     * @return the Field state
     */
    public Object getState() {
        String state = getValue();
        if (StringUtils.isEmpty(state)) {
            return null;
        }
        return state;
    }

    /**
     * Set the Field state.
     *
     * @param state the Field state to set
     */
    public void setState(Object state) {
        if (state == null) {
            return;
        }

        String fieldState = (String) state;
        setValue(fieldState);
    }

    /**
     * This method processes the page request returning true to continue
     * processing or false otherwise. The Field <tt>onProcess()</tt> method is
     * typically invoked by the Form <tt>onProcess()</tt> method when
     * processing POST request.
     * <p/>
     * This method will bind the Field request parameter value to the field,
     * validate the submission and invoke its callback listener if defined.
     * <p/>
     * Below is a typical onProcess implementation:
     *
     * <pre class="codeJava">
     * <span class="kw">public boolean</span> onProcess() {
     *     bindRequestValue();
     *
     *     <span class="kw">if</span> (getValidate()) {
     *         validate();
     *     }
     *
     *     registerActionEvent();
     *
     *     <span class="kw">return true</span>
     * } </pre>
     *
     * @return true to continue Page event processing or false otherwise
     */
    @Override
    public boolean onProcess() {
        Context context = getContext();

        if (context.hasRequestParameter(getName())) {
            // Only process field if it participated in the incoming request
            setDisabled(false);

            bindRequestValue();

            if (getValidate()) {
                validate();
            }

            dispatchActionEvent();
        }

        return true;
    }

    /**
     * Remove the Field state from the session for the given request context.
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
     * Restore the Field state from the session for the given request context.
     * <p/>
     * This method delegates to {@link #setState(java.lang.Object)} to set the
     * field restored state.
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
     * Save the Field state to the session for the given request context.
     * <p/>
     * * This method delegates to {@link #getState()} to retrieve the field state
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

    /**
     * The validate method is invoked by <tt>onProcess()</tt> to validate
     * the request submission. Field subclasses should override this method
     * to implement request validation logic.
     * <p/>
     * If the field determines that the submission is invalid it should set the
     * {@link #error} property with the error message.
     */
    public void validate() {
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Return a normalized label for display in error messages.
     * <p/>
     * The error label is a normalized version of {@link #getLabel()}.
     *
     * @return a normalized label for error message display
     */
    protected String getErrorLabel() {
        String localLabel = getLabel().trim();
        localLabel = (localLabel.endsWith(":"))
                ? localLabel.substring(0, localLabel.length() - 1) : localLabel;
        return localLabel;
    }

    /**
     * Set the error with the a label formatted message specified by the given
     * message bundle key. The message will be formatted with the field label
     * using {@link #getErrorLabel()}.
     * <p/>
     * setErrorMessage will attempt to find a localized error message as
     * described <a href="#message-resolve-order">here</a>, using the following
     * lookup strategy:
     * <ul>
     *   <li>
     *     an error message is looked up for a specific Field instance by
     *     prefixing the <tt>key</tt> with the Field's name:
     *     <blockquote>
     *       <tt>getMessage(getName() + "." + key);</tt>
     *     </blockquote>
     *   </li>
     *   <li>
     *     if no message is found for a specific Field instance, an error
     *     message is looked up for the Field class based on the <tt>key</tt>:
     *     <blockquote>
     *       <tt>getMessage(key);</tt>
     *     </blockquote>
     *   </li>
     * </ul>
     *
     * @param key the key of the localized message bundle string
     */
    protected void setErrorMessage(String key) {
        String errorLabel = getErrorLabel();
        String msg = getMessage(getName() + "." + key, errorLabel);
        if (msg == null) {
            msg = getMessage(key, errorLabel);
        }
        setError(msg);
    }

    /**
     * Set the error with the a label and value formatted message specified by
     * the given message bundle key. The message will be formatted with the
     * field label {0} using {@link #getErrorLabel()} and the given value {1}.
     * <p/>
     * <b>Also see</b> {@link #setErrorMessage(java.lang.String)} on how to
     * specify error messages for specific Field instances.
     *
     * @param key the key of the localized message bundle string
     * @param value the value to format in the message
     */
    protected <T> void setErrorMessage(String key, T value) {
        String msg = getMessage(getName() + "." + key, getErrorLabel(), value);
        if (msg == null) {
            msg = getMessage(key, getErrorLabel(), value);
        }
        setError(msg);
    }

    /**
     * Return the field's value from the request.
     *
     * @return the field's value from the request
     */
    protected String getRequestValue() {
        String requestValue = getContext().getRequestParameter(getName());
        if (requestValue != null) {
            if (isTrim()) {
                return requestValue.trim();
            } else {
                return requestValue;
            }
        } else {
            return "";
        }
    }

    /**
     * Render the Field tag and common attributes including {@link #getName() name},
     * {@link #getId() id}, <tt>class</tt> and <tt>style</tt>.
     *
     * @see org.openidentityplatform.openam.click.control.AbstractControl#renderTagBegin(java.lang.String, HtmlStringBuffer)
     *
     * @param tagName the name of the tag to render
     * @param buffer the buffer to append the output to
     */
    @Override
    protected void renderTagBegin(String tagName, HtmlStringBuffer buffer) {
        if (tagName == null) {
            throw new IllegalStateException("Tag cannot be null");
        }

        buffer.elementStart(tagName);

        String controlName = getName();
        if (controlName != null) {
            buffer.appendAttribute("name", controlName);
        }

        String id = getId();
        if (id != null) {
            buffer.appendAttribute("id", id);
        }

        appendAttributes(buffer);
    }
}
