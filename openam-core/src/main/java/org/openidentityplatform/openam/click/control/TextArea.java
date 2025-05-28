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

import org.openidentityplatform.openam.click.util.HtmlStringBuffer;

import java.text.MessageFormat;

/**
 * Provides a TextArea control: &nbsp; &lt;textarea&gt;&lt;/textarea&gt;.
 *
 * <table class='htmlHeader' cellspacing='6'>
 * <tr>
 * <td>Text Area</td>
 * <td><textarea title='TextArea Control'>Rather lengthy text</textarea></td>
 * </tr>
 * </table>
 *
 * <h3>TextArea Example</h3>
 *
 * The example below shows how to a TextArea to a Form:
 *
 * <pre class="codeJava">
 * TextArea commentsField = <span class="kw">new</span> TextArea(<span class="st">"comments"</span>);
 * commentsField.setCols(40);
 * commentsField.setRows(6);
 * form.add(commentsField); </pre>
 *
 * The TextArea control will rendered HTML output:
 * <pre class="codeHtml">
 * &lt;textarea name="comments" rows="6" cols="40"/&gt;&lt;/textarea&gt; </pre>
 *
 * See also the W3C HTML reference:
 * <a class="external" target="_blank" title="W3C HTML 4.01 Specification"
 *    href="http://www.w3.org/TR/html401/interact/forms.html#h-17.7">TEXTAREA</a>
 */
public class TextArea extends Field {

    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------- Constants

    /**
     * The field validation JavaScript function template.
     * The function template arguments are: <ul>
     * <li>0 - is the field id</li>
     * <li>1 - is the Field required status</li>
     * <li>2 - is the minimum length</li>
     * <li>3 - is the maximum length</li>
     * <li>4 - is the localized error message for required validation</li>
     * <li>5 - is the localized error message for minimum length validation</li>
     * <li>6 - is the localized error message for maximum length validation</li>
     * </ul>
     */
    protected final static String VALIDATE_TEXTAREA_FUNCTION =
        "function validate_{0}() '{'\n"
        + "   var msg = validateTextField(\n"
        + "         ''{0}'',{1}, {2}, {3}, [''{4}'',''{5}'',''{6}'']);\n"
        + "   if (msg) '{'\n"
        + "      return msg + ''|{0}'';\n"
        + "   '}' else '{'\n"
        + "      return null;\n"
        + "   '}'\n"
        + "'}'\n";

    // ----------------------------------------------------- Instance Variables

    /**
     * The number of text area columns. The default number of columns is twenty.
     */
    protected int cols = 20;

    /**
     * The maximum field length validation constraint. If the value is zero this
     * validation constraint is not applied. The default value is zero.
     */
    protected int maxLength = 0;

    /**
     * The minimum field length validation constraint. If the valid is zero this
     * validation constraint is not applied. The default value is zero.
     */
    protected int minLength = 0;

    /** The number of text area rows. The default number of rows is three. */
    protected int rows = 3;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct the TextArea with the given name. The area will have a
     * default size of 20 cols and 3 rows.
     *
     * @param name the name of the field
     */
    public TextArea(String name) {
        super(name);
    }


    /**
     * Construct the TextArea with the given name and label. The area will have
     * a default size of 20 cols and 3 rows.
     *
     * @param name the name of the field
     * @param label the label of the field
     */
    public TextArea(String name, String label) {
        super(name, label);
    }

    /**
     * Construct the TextArea with the given name and required status. The
     * area will have a default size of 20 cols and 3 rows.
     *
     * @param name the name of the field
     * @param required the field required status
     */
    public TextArea(String name, boolean required) {
        super(name);
        setRequired(required);
    }

    /**
     * Construct the TextArea with the given name, label and required status.
     * The area will have a default size of 20 cols and 3 rows.
     *
     * @param name the name of the field
     * @param label the label of the field
     * @param required the field required status
     */
    public TextArea(String name, String label, boolean required) {
        super(name, label);
        setRequired(required);
    }

    /**
     * Construct the TextArea with the given name, number of columns and
     * number of rows.
     *
     * @param name the name of the field
     * @param cols the number of text area cols
     * @param rows the number of text area rows
     */
    public TextArea(String name, int cols, int rows) {
        super(name);
        setCols(cols);
        setRows(rows);
    }

    /**
     * Construct the TextArea with the given name, label, number of columns and
     * number of rows.
     *
     * @param name the name of the field
     * @param label the label of the field
     * @param cols the number of text area cols
     * @param rows the number of text area rows
     */
    public TextArea(String name, String label, int cols, int rows) {
        super(name, label);
        setCols(cols);
        setRows(rows);
    }

    /**
     * Construct the TextArea with the given name, label, number of columns and
     * number of rows.
     *
     * @param name the name of the field
     * @param label the label of the field
     * @param cols the number of text area cols
     * @param rows the number of text area rows
     * @param required the field required status
     */
    public TextArea(String name, String label, int cols, int rows,
        boolean required) {
        super(name, label);
        setCols(cols);
        setRows(rows);
        setRequired(required);
    }

    /**
     * Create a TextArea with no name defined.
     * <p/>
     * <b>Please note</b> the control's name must be defined before it is valid.
     */
    public TextArea() {
        super();
    }

    // ------------------------------------------------------- Public Attributes

    /**
     * Return the textarea's html tag: <tt>textarea</tt>.
     *
     * @see org.apache.click.control.AbstractControl#getTag()
     *
     * @return this controls html tag
     */
     @Override
     public String getTag() {
         return "textarea";
     }

    /**
     * Return the number of text area columns.
     *
     * @return the number of text area columns
     */
    public int getCols() {
        return cols;
    }

    /**
     * Set the number of text area columns. The default number of columns is 20.
     *
     * @param cols set the number of text area columns.
     */
    public void setCols(int cols) {
        this.cols = cols;
    }

    /**
     * Returns the maximum field length validation constraint. If the
     * {@link #maxLength} property is greater than zero, the Field values length
     * will be validated against this constraint when processed.
     *
     * @return the maximum field length validation constraint
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Sets the maximum field length. If the {@link #maxLength} property is
     * greater than zero, the Field values length will be validated against
     * this constraint when processed.
     *
     * @param maxLength the maximum field length validation constraint
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * Returns the minimum field length validation constraint. If the
     * {@link #minLength} property is greater than zero, the Field values length
     * will be validated against this constraint when processed.
     *
     * @return the minimum field length validation constraint
     */
    public int getMinLength() {
        return minLength;
    }

    /**
     * Sets the minimum field length validation constraint. If the
     * {@link #minLength} property is greater than zero, the Field values length
     * will be validated against this constraint when processed.
     *
     * @param minLength the minimum field length validation constraint
     */
    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    /**
     * Return the number of text area rows.
     *
     * @return the number of text area rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Set the number of text area rows. The default number of rows is 3.
     *
     * @param rows set the number of text area rows
     */
    public void setRows(int rows) {
        this.rows = rows;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * @see AbstractControl#getControlSizeEst()
     *
     * @return the estimated rendered control size in characters
     */
    @Override
    public int getControlSizeEst() {
        return 96;
    }

    /**
     * Render the HTML representation of the TextArea.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {

        buffer.elementStart(getTag());

        buffer.appendAttribute("name", getName());
        buffer.appendAttribute("id", getId());
        buffer.appendAttribute("rows", getRows());
        buffer.appendAttribute("cols", getCols());
        buffer.appendAttribute("title", getTitle());
        if (isValid()) {
            removeStyleClass("error");
            if (isDisabled()) {
                addStyleClass("disabled");
            } else {
                removeStyleClass("disabled");
            }
        } else {
            addStyleClass("error");
        }
        if (getTabIndex() > 0) {
            buffer.appendAttribute("tabindex", getTabIndex());
        }

        appendAttributes(buffer);

        if (isDisabled()) {
            buffer.appendAttributeDisabled();
        }
        if (isReadonly()) {
            buffer.appendAttributeReadonly();
        }

        buffer.closeTag();

        buffer.appendEscaped(getValue());

        buffer.elementEnd(getTag());

        if (getHelp() != null) {
            buffer.append(getHelp());
        }
    }

    /**
     * Validate the TextArea request submission.
     * <p/>
     * A field error message is displayed if a validation error occurs.
     * These messages are defined in the resource bundle: <blockquote>
     * <pre>org.apache.click.control.MessageProperties</pre></blockquote>
     * <p/>
     * Error message bundle key names include: <blockquote><ul>
     * <li>field-maxlength-error</li>
     * <li>field-minlength-error</li>
     * <li>field-required-error</li>
     * </ul></blockquote>
     */
    @Override
    public void validate() {
        setError(null);

        String value = getValue();

        int length = value.length();
        if (length > 0) {
            if (getMinLength() > 0 && length < getMinLength()) {
                setErrorMessage("field-minlength-error", getMinLength());
                return;
            }

            if (getMaxLength() > 0 && length > getMaxLength()) {
                setErrorMessage("field-maxlength-error", getMaxLength());
                return;
            }

        } else {
            if (isRequired()) {
                setErrorMessage("field-required-error");
            }
        }
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
    @Override
    public String getValidationJavaScript() {
        Object[] args = new Object[7];
        args[0] = getId();
        args[1] = String.valueOf(isRequired());
        args[2] = String.valueOf(getMinLength());
        args[3] = String.valueOf(getMaxLength());
        args[4] = getMessage("field-required-error", getErrorLabel());
        args[5] = getMessage("field-minlength-error",
                getErrorLabel(), String.valueOf(getMinLength()));
        args[6] = getMessage("field-maxlength-error",
                getErrorLabel(), String.valueOf(getMaxLength()));
        return MessageFormat.format(VALIDATE_TEXTAREA_FUNCTION, args);
    }
}
