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
import org.apache.click.util.PropertyUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a RadioGroup control.
 *
 * <table class='htmlHeader' cellspacing='6'>
 * <tr>
 * <td>Radio Group</td>
 * <td>
 * <input type='radio' name='header' value='R'>Red </input>
 * <input type='radio' name='header' checked value='G'>Green </input>
 * <input type='radio' name='header' value='B'>Blue</input>
 * </td>
 * </tr>
 * </table>
 *
 * The RadioGroup control provides a Field for containing grouped Radio buttons.
 * Radio controls added to a RadioGroup will have their name set to that of
 * the RadioGroup. This will ensure the buttons will toggle together so that
 * only one button is selected at a time.
 *
 * <h3>RadioGroup Example</h3>
 *
 * The example below illustrates a RadioGroup being added to a form.
 *
 * <pre class="codeJava">
 * <span class="kw">public class</span> Purchase <span class="kw">extends</span> Page {
 *
 *     <span class="kw">public</span> Form form = <span class="kw">new</span> Form();
 *
 *     <span class="kw">private</span> RadioGroup radioGroup = <span class="kw">new</span> RadioGroup(<span class="st">"packaging"</span>);
 *
 *     <span class="kw">public</span> Purchase() {
 *         radioGroup.add(<span class="kw">new</span> Radio(<span class="st">"STD"</span>, <span class="st">"Standard "</span>));
 *         radioGroup.add(<span class="kw">new</span> Radio(<span class="st">"PRO"</span>, <span class="st">"Protective "</span>));
 *         radioGroup.add(<span class="kw">new</span> Radio(<span class="st">"GFT"</span>, <span class="st">"Gift Wrap "</span>));
 *         radioGroup.setValue(<span class="st">"STD"</span>);
 *         radioGroup.setVerticalLayout(<span class="kw">true</span>);
 *         form.add(radioGroup);
 *
 *         ..
 *     }
 * } </pre>
 *
 * This radio group field would be render as:
 *
 * <table class='htmlExample'>
 *  <tr>
 *   <td>Packaging</td>
 *   <td>
 *    <input type='radio' name='group' checked value='STD'>Standard</input><br>
 *    <input type='radio' name='group' value='PRO'>Protective</input><br>
 *    <input type='radio' name='group' value='GFT'>Gift Wrap</input>
 *   </td>
 *  </tr>
 * </table>
 *
 * See also W3C HTML reference
 * <a class="external" target="_blank" title="W3C HTML 4.01 Specification"
 *    href="http://www.w3.org/TR/html401/interact/forms.html#h-17.4">INPUT</a>
 *
 * @see Radio
 */
public class RadioGroup extends Field {

    private static final long serialVersionUID = 1L;

    /**
     * The field validation JavaScript function template.
     * The function template arguments are: <ul>
     * <li>0 - is the field id</li>
     * <li>1 - is the name of the radio button</li>
     * <li>2 - is the id of the form</li>
     * <li>3 - is the Field required status</li>
     * <li>4 - is the localized error message</li>
     * <li>5 - is the first radio id to select</li>
     * </ul>
     */
    protected final static String VALIDATE_RADIOGROUP_FUNCTION =
        "function validate_{0}() '{'\n"
        + "   var msg = validateRadioGroup(''{1}'', ''{2}'', {3}, [''{4}'']);\n"
        + "   if (msg) '{'\n"
        + "      return msg + ''|{5}'';\n"
        + "   '}' else '{'\n"
        + "      return null;\n"
        + "   '}'\n"
        + "'}'\n";

    // Instance Variables -----------------------------------------------------

    /** The list of Radio controls. */
    protected List<Radio> radioList;

    /**
     * The layout is vertical flag (default false). If the layout is vertical
     * each Radio controls is rendered on a new line using the &lt;br&gt;
     * tag.
     */
    protected boolean isVerticalLayout = true;

    // Constructors -----------------------------------------------------------

    /**
     * Create a RadioGroup with the given name.
     *
     * @param name the name of the field
     */
    public RadioGroup(String name) {
        super(name);
    }

    /**
     * Create a RadioGroup with the given name and required status.
     *
     * @param name the name of the field
     * @param required the field required status
     */
    public RadioGroup(String name, boolean required) {
        super(name);
        setRequired(required);
    }

    /**
     * Create a RadioGroup with the given name and label.
     *
     * @param name the name of the field
     * @param label the label of the field
     */
    public RadioGroup(String name, String label) {
        super(name, label);
    }

    /**
     * Create a RadioGroup with the given name, label and required status.
     *
     * @param name the name of the field
     * @param label the label of the field
     * @param required the field required status
     */
    public RadioGroup(String name, String label, boolean required) {
        super(name, label);
        setRequired(required);
    }

    /**
     * Create a RadioGroup field with no name.
     * <p/>
     * <b>Please note</b> the control's name must be defined before it is valid.
     */
    public RadioGroup() {
        super();
    }

    // Public Attributes ------------------------------------------------------

    /**
     * Add the given radio to the radio group. When the radio is added to the
     * group it will use its parent RadioGroup's name when rendering if it
     * has not already been set.
     *
     * @param radio the radio control to add to the radio group
     * @throws IllegalArgumentException if the radio parameter is null
     */
    public void add(Radio radio) {
        if (radio == null) {
            throw new IllegalArgumentException("Null radio parameter");
        }

        radio.setParent(this);
        getRadioList().add(radio);
        if (getForm() != null) {
            radio.setForm(getForm());
        }
    }

    /**
     * Add the given collection Radio item options to the RadioGroup.
     *
     * @param options the collection of Radio items to add
     * @throws IllegalArgumentException if options is null
     */
    public void addAll(Collection<Radio> options) {
        if (options == null) {
            String msg = "options parameter cannot be null";
            throw new IllegalArgumentException(msg);
        }
        for (Radio radio : options) {
            add(radio);
        }
    }

    /**
     * Add the given Map of radio values and labels to the RadioGroup.
     * The Map entry key will be used as the radio value and the Map entry
     * value will be used as the radio label.
     * <p/>
     * It is recommended that <tt>LinkedHashMap</tt> is used as the Map
     * parameter to maintain the order of the radio items.
     *
     * @param options the Map of radio option values and labels to add
     * @throws IllegalArgumentException if options is null
     */
    public void addAll(Map<?, ?> options) {
        if (options == null) {
            String msg = "options parameter cannot be null";
            throw new IllegalArgumentException(msg);
        }
        for (Map.Entry<?, ?> entry : options.entrySet()) {
            Radio radio = new Radio(entry.getKey().toString(),
                                    entry.getValue().toString());
            add(radio);
        }
    }

    /**
     * Add the given collection of objects to the RadioGroup, creating new
     * Radio instances based on the object properties specified by value and
     * label.
     *
     * <pre class="codeJava">
     * RadioGroup radioGroup = <span class="kw">new</span> RadioGroup(<span class="st">"type"</span>, <span class="st">"Type:"</span>);
     * radioGroup.addAll(getCustomerService().getCustomerTypes(), <span class="st">"id"</span>, <span class="st">"name"</span>);
     * form.add(select); </pre>
     *
     * @param objects the collection of objects to render as radio options
     * @param value the name of the object property to render as the Radio value
     * @param label the name of the object property to render as the Radio label
     * @throws IllegalArgumentException if options, value or label parameter is null
     */
    public void addAll(Collection<?> objects, String value, String label) {
        if (objects == null) {
            String msg = "objects parameter cannot be null";
            throw new IllegalArgumentException(msg);
        }
        if (value == null) {
            String msg = "value parameter cannot be null";
            throw new IllegalArgumentException(msg);
        }
        if (label == null) {
            String msg = "label parameter cannot be null";
            throw new IllegalArgumentException(msg);
        }

        if (objects.isEmpty()) {
            return;
        }

        Map<?, ?> cache = new HashMap<Object, Object>();

        for (Object object : objects) {
            try {
                Object valueResult = PropertyUtils.getValue(object, value, cache);
                Object labelResult = PropertyUtils.getValue(object, label, cache);

                Radio radio = null;

                if (labelResult != null) {
                    radio = new Radio(valueResult.toString(), labelResult.toString());

                } else {
                    radio = new Radio(valueResult.toString());
                }

                add(radio);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Return the RadioGroup focus JavaScript.
     *
     * @return the RadioGroup focus JavaScript
     */
    @Override
    public String getFocusJavaScript() {
        String id = "";

        if (!getRadioList().isEmpty()) {
            Radio radio = getRadioList().get(0);
            id = radio.getId();
        }

        HtmlStringBuffer buffer = new HtmlStringBuffer(32);
        buffer.append("setFocus('");
        buffer.append(id);
        buffer.append("');");

        return buffer.toString();
    }

    /**
     * @see Field#setForm(Form)
     *
     * @param form Field's parent <tt>Form</tt>
     */
    @Override
    public void setForm(Form form) {
        super.setForm(form);
        if (hasRadios()) {
            for (int i = 0, size = getRadioList().size(); i < size; i++) {
                Radio radio = getRadioList().get(i);
                radio.setForm(getForm());
            }
        }
    }

    /**
     * Return true if the radio control layout is vertical.
     *
     * @return true if the radio control layout is vertical
     */
    public boolean isVerticalLayout() {
        return isVerticalLayout;
    }

    /**
     * Set the vertical radio control layout flag.
     *
     * @param vertical the vertical layout flag
     */
    public void setVerticalLayout(boolean vertical) {
        isVerticalLayout = vertical;
    }

    /**
     * Return the list of radio controls.
     *
     * @return the list of radio controls
     */
    public List<Radio> getRadioList() {
        if (radioList == null) {
            radioList = new ArrayList<Radio>();
        }
        return radioList;
    }

    /**
     * Return true if RadioGroup has Radio controls, or false otherwise.
     *
     * @return true if RadioGroup has Radio controls, or false otherwise
     */
    public boolean hasRadios() {
        return radioList != null && !radioList.isEmpty();
    }

    /**
     * Return the RadioGroup JavaScript client side validation function.
     *
     * @return the field JavaScript client side validation function
     */
    @Override
    public String getValidationJavaScript() {
        Object[] args = new Object[6];
        args[0] = getId();
        args[1] = getName();
        args[2] = getForm().getId();
        args[3] = String.valueOf(isRequired());
        args[4] = getMessage("select-error", getErrorLabel());

        if (!getRadioList().isEmpty()) {
            Radio radio = getRadioList().get(0);
            args[5] = radio.getId();
        } else {
            args[5] = "";
        }

        return MessageFormat.format(VALIDATE_RADIOGROUP_FUNCTION, args);
    }

    // Public Methods ---------------------------------------------------------

    /**
     * @see org.apache.click.Control#onInit()
     */
    @Override
    public void onInit() {
        super.onInit();
        for (int i = 0, size = getRadioList().size(); i < size; i++) {
            Radio radio = getRadioList().get(i);
            radio.onInit();
        }
    }

    /**
     * Process the request Context setting the checked value and invoking
     * the controls listener if defined.
     *
     * @see org.apache.click.Control#onProcess()
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

        boolean continueProcessing = true;
        for (int i = 0, size = getRadioList().size(); i < size; i++) {
            Radio radio = getRadioList().get(i);
            if (!radio.onProcess()) {
                continueProcessing = false;
            }
        }

        if (getValidate()) {
            validate();
        }

        dispatchActionEvent();

        return continueProcessing;
    }

    /**
     * @see org.apache.click.Control#onDestroy()
     */
    @Override
    public void onDestroy() {
        for (int i = 0, size = getRadioList().size(); i < size; i++) {
            Radio radio = getRadioList().get(i);
            try {
                radio.onDestroy();
            } catch (Throwable t) {
                ClickUtils.getLogService().error("onDestroy error", t);
            }
        }
    }

    /**
     * @see AbstractControl#getControlSizeEst()
     *
     * @return the estimated rendered control size in characters
     */
    @Override
    public int getControlSizeEst() {
        return getRadioList().size() * 30;
    }

    /**
     * Render the HTML representation of the RadioGroup.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {
        buffer.elementStart("span");
        buffer.appendAttribute("id", getId());
        appendAttributes(buffer);
        buffer.closeTag();

        String localValue = getValue();

        final int size = getRadioList().size();

        for (int i = 0; i < size; i++) {
            Radio radio = getRadioList().get(i);

            if (isReadonly() && !radio.isReadonly()) {
                radio.setReadonly(true);
            }
            if (isDisabled() && !radio.isDisabled()) {
                radio.setDisabled(true);
            }

            if (localValue != null && localValue.length() > 0) {
                if (radio.getValue().equals(localValue)) {
                    radio.setChecked(true);
                } else {
                    radio.setChecked(false);
                }
            }

            radio.render(buffer);

            if (isVerticalLayout() && (i < size - 1)) {
                buffer.append("<br/>");
            }
        }

        buffer.elementEnd("span");
    }

    /**
     * Return the HTML rendered RadioGroup string.
     *
     * @see Object#toString()
     *
     * @return the HTML rendered RadioGroup string
     */
    @Override
    public String toString() {
        HtmlStringBuffer buffer = new HtmlStringBuffer(getControlSizeEst());
        render(buffer);
        return buffer.toString();
    }

    /**
     * Validate the RadioGroup request submission.
     * <p/>
     * A field error message is displayed if a validation error occurs.
     * These messages are defined in the resource bundle: <blockquote>
     * <pre>org.apache.click.control.MessageProperties</pre></blockquote>
     * <p/>
     * Error message bundle key names include: <blockquote><ul>
     * <li>select-error</li>
     * </ul></blockquote>
     */
    @Override
    public void validate() {
        setError(null);

        if (isRequired() && getValue().length() == 0) {
            setErrorMessage("select-error");
        }
    }
}
