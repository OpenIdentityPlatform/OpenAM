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
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;

/**
 * Provides a Radio control: &nbsp; &lt;input type='radio'&gt;.
 *
 * <table class='htmlHeader' cellspacing='6'>
 * <tr>
 * <td><input type='radio' name='header' value='Radio Control'>Radio</input></td>
 * </tr>
 * </table>
 *
 * The Radio control is generally used within a RadioGroup, for an code example
 * please see the {@link RadioGroup} Javadoc example.
 * When used with a RadioGroup the Radio control will derive its name from the
 * parent RadioGroup, if the Radio's name is not defined.
 *
 * <p/>
 * See also W3C HTML reference
 * <a class="external" target="_blank" title="W3C HTML 4.01 Specification"
 *    href="http://www.w3.org/TR/html401/interact/forms.html#h-17.4">INPUT</a>
 *
 * @see RadioGroup
 */
public class Radio extends Field {

    private static final long serialVersionUID = 1L;

    // ----------------------------------------------------- Instance Variables

    /** The field checked value. */
    protected boolean checked;

    // ----------------------------------------------------------- Constructors

    /**
     * Create a radio field with the given value.
     *
     * @param value the label of the field
     */
    public Radio(String value) {
        setValue(value);
    }

    /**
     * Create a radio field with the given value and label.
     *
     * @param value the label of the field
     * @param label the name of the field
     */
    public Radio(String value, String label) {
        setValue(value);
        setLabel(label);
    }

    /**
     * Create a radio field with the given value, label and name.
     *
     * @param value the label of the field
     * @param label the label of the field
     * @param name the name of the field
     */
    public Radio(String value, String label, String name) {
        setValue(value);
        setLabel(label);
        setName(name);
    }

    /**
     * Create an Radio field with no name defined.
     */
    public Radio() {
    }

    // ------------------------------------------------------ Public Attributes

    /**
     * Return the radio's html tag: <tt>input</tt>.
     *
     * @see AbstractControl#getTag()
     *
     * @return this controls html tag
     */
    @Override
    public String getTag() {
        return "input";
    }

    /**
     * Return true if the radio is checked, or false otherwise.
     *
     * @return true if the radio is checked.
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Set the selected value of the radio.
     *
     * @param value the selected value
     */
    public void setChecked(boolean value) {
        checked = value;
    }


    /**
     * Return the Radio field id attribute.
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
            String id = formId + fieldName + "_" + ClickUtils.escapeHtml(getValue());

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
     * Set the parent of the Field.
     *
     * @see org.apache.click.Control#setParent(Object)
     *
     * @param parent the parent of the Control
     * @throws IllegalArgumentException if the given parent instance is
     * referencing <tt>this</tt> object: <tt>if (parent == this)</tt>
     */
    @Override
    public void setParent(Object parent) {
        if (parent == this) {
            throw new IllegalArgumentException("Cannot set parent to itself");
        }
        this.parent = parent;
    }

    /**
     * Return the field display label.
     * <p/>
     * If the label value is null, this method will attempt to find a
     * localized label message in the parent messages using the key:
     * <p/>
     * If the Radio name attribute is not null:
     * <blockquote>
     * <tt>super.getName() + ".label"</tt>
     * </blockquote>
     * If the Radio name attribute is null and the parent of the Radio is the RadioGroup:
     * <blockquote>
     * <tt>parent.getName() + "." + getValue() + ".label"</tt>
     * </blockquote>
     * If not found then the message will be looked up in the
     * <tt>/click-control.properties</tt> file using the same key.
     * If a value still cannot be found then the Field name will be
     * the radio value.
     * <p/>
     * For example given a <tt>CustomerPage</tt> with the properties file
     * <tt>CustomerPage.properties</tt>:
     *
     * <pre class="codeConfig">
     * <span class="st">gender.M</span>.label=<span class="red">Male</span>
     * <span class="st">gender.F</span>.label=<span class="red">Female</span> </pre>
     *
     * The page Radio code:
     * <pre class="codeJava">
     * <span class="kw">public class</span> CustomerPage <span class="kw">extends</span> Page {
     *
     *     <span class="kw">public</span> Form form = <span class="kw">new</span> Form();
     *
     *     <span class="kw">private</span> RadioGroup radioGroup = <span class="kw">new</span> RadioGroup(<span class="st">"gender"</span>);
     *
     *     <span class="kw">public</span> CustomerPage() {
     *         radioGroup.add(<span class="kw">new</span> Radio(<span class="st">"M"</span>));
     *         radioGroup.add(<span class="kw">new</span> Radio(<span class="st">"F"</span>));
     *         form.add(radioGroup);
     *
     *         ..
     *     }
     * } </pre>
     *
     * Will render the Radio label properties as:
     * <pre class="codeHtml">
     * &lt;input type="radio" name="<span class="st">gender</span>" value="<span class="st">M</span>"&gt;&lt;label&gt;<span class="red">Male</span>&lt;/label&gt;&lt;/label&gt;&lt;br/&gt;
     * &lt;input type="radio" name="<span class="st">gender</span>" value="<span class="st">F</span>"&gt;&lt;label&gt;<span class="red">Female</span>&lt;/label&gt;&lt;/label&gt; </pre>
     *
     * @return the display label of the Field
     */
    @Override
    public String getLabel() {
        if (label == null) {
            if (super.getName() != null) {
                label = getMessage(super.getName() + ".label");
            } else {
                Object parent = getParent();
                if (parent instanceof RadioGroup) {
                    RadioGroup radioGroup = (RadioGroup) parent;
                    label = getMessage(
                        radioGroup.getName() + "." + getValue() + ".label");
                }
            }
        }
        if (label == null) {
            label = getValue();
        }
        return label;
    }

    /**
     * Return the name of the Radio field. If the Radio name attribute has not
     * been explicitly set, this method will return its parent RadioGroup's
     * name if defined.
     *
     * @return the name of the control
     */
    @Override
    public String getName() {
        if (super.getName() != null) {
            return super.getName();

        } else {
            Object parent = getParent();
            if (parent instanceof RadioGroup) {
                RadioGroup radioGroup = (RadioGroup) parent;
                return radioGroup.getName();

            } else {
                return null;
            }
        }
    }

    /**
     * Return the input type: 'radio'.
     *
     * @return the input type: 'radio'
     */
    public String getType() {
        return "radio";
    }

    /**
     * Set the radio value, setting the checked status if given value is the
     * same as the radio field value.
     *
     * @see Field#setValue(String)
     *
     * @param value the Field value
     */
    @Override
    public void setValue(String value) {
        setChecked(getValue().equals(value));
        super.setValue(value);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Bind the request submission, setting the Field {@link #checked} property
     * if defined in the request.
     */
    @Override
    public void bindRequestValue() {
        String localValue = getRequestValue();

        setChecked(getValue().equals(localValue));
    }

    /**
     * Process the request Context setting the checked value if selected
     * and invoking the controls listener if defined.
     *
     * @return true to continue Page event processing or false otherwise
     */
    @Override
    public boolean onProcess() {
        if (isDisabled()) {
            Context context = getContext();

            // Switch off disabled property if control has incoming request
            // parameter. Normally this means the field was enabled via JS
            if (context.hasRequestParameter(getName())) {
                setDisabled(false);
            } else {
                // If field is disabled skip process event
                return true;
            }
        }

        bindRequestValue();

        if (isChecked()) {
            dispatchActionEvent();
        }
        return true;
    }

    /**
     * Render a HTML Radio string.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {
        String id = getId();

        buffer.elementStart(getTag());

        buffer.appendAttribute("type", getType());
        buffer.appendAttribute("name", getName());
        buffer.appendAttributeEscaped("value", getValue());
        buffer.appendAttribute("id", id);
        buffer.appendAttribute("title", getTitle());
        if (isValid()) {
            removeStyleClass("error");
        } else {
            addStyleClass("error");
        }
        if (getTabIndex() > 0) {
            buffer.appendAttribute("tabindex", getTabIndex());
        }
        if (isChecked()) {
            buffer.appendAttribute("checked", "checked");
        }

        appendAttributes(buffer);

        if (isDisabled() || isReadonly()) {
            buffer.appendAttributeDisabled();
        }
        if (isReadonly()) {
            buffer.appendAttributeReadonly();
        }

        buffer.elementEnd();

        buffer.elementStart("label");
        buffer.appendAttribute("for", id);
        buffer.closeTag();
        buffer.appendEscaped(getLabel());
        buffer.elementEnd("label");

        // radio element does not support "readonly" element, so as a work around
        // we make the field "disabled" and render a hidden field to submit its value
        if (isReadonly() && isChecked()) {
            buffer.elementStart("input");
            buffer.appendAttribute("type", "hidden");
            buffer.appendAttribute("name", getName());
            buffer.appendAttributeEscaped("value", getValue());
            buffer.elementEnd();
        }
    }
}
