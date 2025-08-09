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

import org.openidentityplatform.openam.click.util.ClickUtils;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Provides a Hidden Field control: &nbsp; &lt;input type='hidden'&gt;.
 * <p/>
 * The HiddenField control is useful for storing state information in a Form,
 * such as object ids, instead of using the Session object. This control is
 * capable of supporting the following classes:<blockquote><ul>
 * <li>Boolean</li>
 * <li>Date</li>
 * <li>Double</li>
 * <li>Float</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Short</li>
 * <li>String</li>
 * <li>Serializable</li>
 * </ul></blockquote>
 * <p/>
 * Serializable non-primitive objects will be serialized, compressed and
 * Base64 encoded, using {@link ClickUtils#encode(Object)}
 * method, and decoded using the corresponding
 * {@link ClickUtils#decode(String)} method.
 *
 * <h3>HiddenField Example</h3>
 *
 * An example is provided below which uses a hidden field to count the number of
 * times a form is consecutively submitted. The count is displayed in the
 * page template using the model "count" value.
 *
 * <pre class="codeJava">
 * <span class="kw">public class</span> CountPage <span class="kw">extends</span> Page {
 *
 *     <span class="kw">public</span> Form form = <span class="kw">new</span> Form();
 *     <span class="kw">public</span> Integer count;
 *
 *     <span class="kw">private</span> HiddenField counterField = <span class="kw">new</span> HiddenField(<span class="st">"counterField"</span>, Integer.<span class="kw">class</span>);
 *
 *     <span class="kw">public</span> CountPage() {
 *         form.add(counterField);
 *         form.add(<span class="kw">new</span> Submit(<span class="st">"ok"</span>, <span class="st">"  OK  "</span>));
 *     }
 *
 *     <span class="kw">public void</span> onGet() {
 *         count = <span class="kw">new</span> Integer(0);
 *         counterField.setValueObject(count);
 *     }
 *
 *     <span class="kw">public void</span> onPost() {
 *         count = (Integer) counterField.getValueObject();
 *         count = <span class="kw">new</span> Integer(count.intValue() + 1);
 *         counterField.setValueObject(count);
 *     }
 * } </pre>
 *
 * See also W3C HTML reference
 * <a class="external" target="_blank" title="W3C HTML 4.01 Specification"
 *    href="http://www.w3.org/TR/html401/interact/forms.html#h-17.4">INPUT</a>
 */
public class HiddenField extends Field {

    private static final long serialVersionUID = 1L;

    // ----------------------------------------------------- Instance Variables

    /** The field value Object. */
    protected Object valueObject;

    /** The field value Class. */
    protected Class<?> valueClass;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a HiddenField with the given name and Class.
     *
     * @param name the name of the hidden field
     * @param valueClass the Class of the value Object
     */
    public HiddenField(String name, Class<?> valueClass) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }
        if (valueClass == null) {
            throw new IllegalArgumentException("Null valueClass parameter");
        }
        this.name = name;
        this.valueClass = valueClass;
    }

    /**
     * Construct a HiddenField with the given name and value object.
     *
     * @param name the name of the hidden field
     * @param value the value object
     */
    public HiddenField(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }
        if (value == null) {
            throw new IllegalArgumentException("Null value parameter");
        }
        this.name = name;
        this.valueClass = value.getClass();
        setValueObject(value);
    }

    /**
     * Create an HiddenField with no name or Class defined. <b>Please note</b>
     * the HiddenField's name and value Class must be defined before it is
     * valid.
     */
    public HiddenField() {
        super();
    }

    // ------------------------------------------------------ Public Attributes

    /**
     * Return the hiddenfield's html tag: <tt>input</tt>.
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
     * Returns true.
     *
     * @see org.apache.click.control.Field#isHidden()
     *
     * @return true
     */
    @Override
    public boolean isHidden() {
        return true;
    }

    /**
     * Return the input type: 'hidden'.
     *
     * @return the input type: 'hidden'
     */
    public String getType() {
        return "hidden";
    }

    /**
     * @see org.apache.click.control.Field#getValue()
     *
     * @return the Field value
     */
    @Override
    public String getValue() {
        return (getValueObject() != null) ? getValueObject().toString() : "";
    }

    /**
     * @see org.apache.click.control.Field#setValue(String)
     *
     * @param value the Field value
     */
    @Override
    public void setValue(String value) {
        setValueObject(value);
    }

    /**
     * Return the registered Class for the Hidden Field value Object.
     *
     * @return the registered Class for the Hidden Field value Object
     */
    public Class<?> getValueClass() {
        return valueClass;
    }

    /**
     * Set the registered Class for the Hidden Field value Object.
     *
     * @param valueClass the registered Class for the Hidden Field value Object
     */
    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    /**
     * Return the value Object of the hidden field.
     *
     * @see org.apache.click.control.Field#getValueObject()
     *
     * @return the object representation of the Field value
     */
    @Override
    public Object getValueObject() {
        return valueObject;
    }

    /**
     * @see Field#setValueObject(Object)
     *
     * @param value the object value to set
     */
    @Override
    public void setValueObject(Object value) {
        if ((value != null) && (value.getClass() != valueClass)) {
            String msg =
                "The value.getClass(): '" + value.getClass().getName()
                + "' must be the same as the HiddenField valueClass: '"
                + ((valueClass != null) ? valueClass.getName() : "null") + "'";

            throw new IllegalArgumentException(msg);
        }

        this.valueObject = value;
    }

    /**
     * Returns null to ensure no client side JavaScript validation is performed.
     *
     * @return null to ensure no client side JavaScript validation is performed
     */
    @Override
    public String getValidationJavaScript() {
        return null;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * This method binds the submitted request value to the Field's value.
     */
    @Override
    public void bindRequestValue() {

        String aValue = getRequestValue();
        Class<?> valueClass = getValueClass();

        if (valueClass == null) {
            throw new IllegalStateException("The value class is not defined."
                + " Please use setValueClass(Class valueClass) to specify"
                + " the HiddenField value type.");
        }

        if (valueClass == String.class) {
            setValue(aValue);

        } else if (aValue != null && aValue.length() > 0) {

             if (valueClass == Integer.class) {
                setValueObject(Integer.valueOf(aValue));

            } else if (valueClass == Boolean.class) {
                setValueObject(Boolean.valueOf(aValue));

            } else if (valueClass == Double.class) {
                setValueObject(Double.valueOf(aValue));

            } else if (valueClass == Float.class) {
                setValueObject(Float.valueOf(aValue));

            } else if (valueClass == Long.class) {
                setValueObject(Long.valueOf(aValue));

            } else if (valueClass == Short.class) {
                setValueObject(Short.valueOf(aValue));

            } else if (valueClass == Timestamp.class) {
                long time = Long.parseLong(aValue);
                setValueObject(new Timestamp(time));

            } else if (valueClass == java.sql.Date.class) {
                long time = Long.parseLong(aValue);
                setValueObject(new java.sql.Date(time));

            } else if (valueClass == Time.class) {
                long time = Long.parseLong(aValue);
                setValueObject(new Time(time));

            } else if (Date.class.isAssignableFrom(valueClass)) {
                long time = Long.parseLong(aValue);
                setValueObject(new Date(time));

            } else if (Serializable.class.isAssignableFrom(valueClass)) {
                try {
                    setValueObject(ClickUtils.decode(aValue));
                } catch (ClassNotFoundException cnfe) {
                    String msg =
                        "could not decode value for hidden field: " + aValue;
                    throw new RuntimeException(msg, cnfe);
                } catch (IOException ioe) {
                    String msg =
                        "could not decode value for hidden field: " + aValue;
                    throw new RuntimeException(msg, ioe);
                }
            } else {
                setValue(aValue);
            }
        } else {
            setValue(null);
        }
    }

    /**
     * Render the HTML representation of the HiddenField.
     *
     * @see org.openidentityplatform.openam.click.Control#render(HtmlStringBuffer)
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {

        buffer.elementStart(getTag());
        buffer.appendAttribute("type", getType());
        buffer.appendAttribute("name", getName());
        buffer.appendAttribute("id", getId());

        Class<?> valueCls = getValueClass();

        if (valueCls == String.class
            || valueCls == Integer.class
            || valueCls == Boolean.class
            || valueCls == Double.class
            || valueCls == Float.class
            || valueCls == Long.class
            || valueCls == Short.class) {

            buffer.appendAttributeEscaped("value", String.valueOf(getValue()));

        } else if (getValueObject() instanceof Date) {
            String dateStr = String.valueOf(((Date) getValueObject()).getTime());
            buffer.appendAttributeEscaped("value", dateStr);

        } else if (getValueObject() instanceof Serializable) {
            try {
                buffer.appendAttribute("value", ClickUtils.encode(getValueObject()));
            } catch (IOException ioe) {
                String msg =
                    "could not encode value for hidden field: "
                    + getValueObject();
                throw new RuntimeException(msg, ioe);
            }
        } else {
            buffer.appendAttributeEscaped("value", getValue());
        }

        buffer.elementEnd();
    }
}
