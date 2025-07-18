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

import java.util.Map;

/**
 * Provides a HTML element StringBuffer for rendering, automatically
 * escaping string values. HtmlStringBuffer is used by Click controls
 * for HTML rendering.
 * <p/>
 * For example the following code:
 * <pre class="codeJava">
 * <span class="kw">public</span> String toString() {
 *     HtmlStringBuffer buffer = <span class="kw">new</span> HtmlStringBuffer();
 *
 *     buffer.elementStart(<span class="st">"input"</span>);
 *     buffer.appendAttribute(<span class="st">"type"</span>, <span class="st">"text"</span>);
 *     buffer.appendAttribute(<span class="st">"name"</span>, getName());
 *     buffer.appendAttribute(<span class="st">"value"</span>, getValue());
 *     buffer.elementEnd();
 *
 *     <span class="kw">return</span> buffer.toString();
 * } </pre>
 *
 * Would render:
 *
 * <pre class="codeHtml">
 * &lt;input type="text" name="address" value="23 Holt's Street"/&gt; </pre>
 *
 * <h4>Synchronization</h4>
 *
 * To improve performance in Click's thread safe environment this
 * class does not synchronize append operations. Internally this class uses
 * a character buffer adapted from the JDK 1.5 <tt>AbstractStringBuilder</tt>.
 */
public class HtmlStringBuffer {

    // -------------------------------------------------------------- Constants

    /** JavaScript attribute names. */
    static final String[] JS_ATTRIBUTES = {
            "onload", "onunload", "onclick", "ondblclick", "onmousedown",
            "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onfocus",
            "onblur", "onkeypress", "onkeydown", "onkeyup", "onsubmit", "onreset",
            "onselect", "onchange"
    };

    // ----------------------------------------------------- Instance Variables

    /** The character storage array. */
    protected char[] characters;

    /** The count is the number of characters used. */
    protected int count;

    // ----------------------------------------------------------- Constructors

    /**
     * Create a new HTML StringBuffer with the specified initial
     * capacity.
     *
     * @param length the initial capacity
     */
    public HtmlStringBuffer(int length) {
        characters = new char[length];
    }

    /**
     * Create a new HTML StringBuffer with an initial capacity of 128
     * characters.
     */
    public HtmlStringBuffer() {
        characters = new char[128];
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Append the double value to the buffer.
     *
     * @param value the double value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer append(double value) {
        append(String.valueOf(value));

        return this;
    }

    /**
     * Append the char value to the buffer.
     *
     * @param value the char value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer append(char value) {
        int newcount = count + 1;
        if (newcount > characters.length) {
            expandCapacity(newcount);
        }
        characters[count++] = value;

        return this;
    }

    /**
     * Append the integer value to the buffer.
     *
     * @param value the integer value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer append(int value) {
        append(String.valueOf(value));

        return this;
    }

    /**
     * Append the long value to the buffer.
     *
     * @param value the long value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer append(long value) {
        append(String.valueOf(value));

        return this;
    }

    /**
     * Append the raw object value of the given object to the buffer.
     *
     * @param value the object value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer append(Object value) {
        String string = String.valueOf(value);
        int length = string.length();

        int newCount = count + length;
        if (newCount > characters.length) {
            expandCapacity(newCount);
        }
        string.getChars(0, length, characters, count);
        count = newCount;

        return this;
    }

    /**
     * Append the raw string value of the given object to the buffer.
     *
     * @param value the string value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer append(String value) {
        String string = (value != null) ? value : "null";
        int length = string.length();

        int newCount = count + length;
        if (newCount > characters.length) {
            expandCapacity(newCount);
        }
        string.getChars(0, length, characters, count);
        count = newCount;

        return this;
    }

    /**
     * Append the given value to the buffer and escape its value. The following
     * characters are escaped: &lt;, &gt;, &quot;, &#039;, &amp;.
     *
     * @param value the object value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     * @throws IllegalArgumentException if the value is null
     */
    public HtmlStringBuffer appendEscaped(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Null value parameter");
        }

        String string = value.toString();

        boolean requiresEscape = false;
        for (int i = 0, size = string.length(); i < size; i++) {
            if (ClickUtils.requiresEscape(string.charAt(i))) {
                requiresEscape = true;
                break;
            }
        }

        if (requiresEscape) {
            ClickUtils.appendEscapeString(string, this);

        } else {
            append(string);
        }

        return this;
    }

    /**
     * Append the given HTML attribute name and value to the string buffer, and
     * do not escape the attribute value.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    appendAttribute(<span class="st">"size"</span>, 10)  <span class="green">-&gt;</span>  <span class="st">size="10"</span> </pre>
     *
     * @param name the HTML attribute name
     * @param value the HTML attribute value
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     * @throws IllegalArgumentException if name is null
     */
    public HtmlStringBuffer appendAttribute(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }

        if (value != null) {
            append(" ");
            append(name);
            append("=\"");
            append(value);
            append("\"");
        }

        return this;
    }

    /**
     * Append the given attribute name and value to the buffer, if the value
     * is not null.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    appendAttribute(<span class="st">"class"</span>, <span class="st">"required"</span>)  <span class="green">-></span>  <span class="st">class="required"</span> </pre>
     *
     * The attribute value will be escaped. The following characters are escaped:
     * &lt;, &gt;, &quot;, &#039;, &amp;.
     * <p/>
     * If the attribute name is a JavaScript event handler the value will
     * not be escaped.
     *
     * @param name the HTML attribute name
     * @param value the object value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     * @throws IllegalArgumentException if name is null
     */
    public HtmlStringBuffer appendAttributeEscaped(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }
        if (value != null) {
            append(" ");
            append(name);
            append("=\"");
            if (isJavaScriptAttribute(name)) {
                append(value);
            } else {
                appendEscaped(value.toString());
            }
            append("\"");
        }

        return this;
    }

    /**
     * Append the given HTML attribute name and value to the string buffer.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    appendAttribute(<span class="st">"size"</span>, 10)  <span class="green">-&gt;</span>  <span class="st">size="10"</span> </pre>
     *
     * @param name the HTML attribute name
     * @param value the HTML attribute value
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     * @throws IllegalArgumentException if name is null
     */
    public HtmlStringBuffer appendAttribute(String name, int value) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }
        append(" ");
        append(name);
        append("=\"");
        append(value);
        append("\"");

        return this;
    }

    /**
     * Append the HTML "disabled" attribute to the string buffer.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    appendAttributeDisabled()  <span class="green">-></span>  <span class="st">disabled="disabled"</span> </pre>
     *
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer appendAttributeDisabled() {
        append(" disabled=\"disabled\"");

        return this;
    }

    /**
     * Append the HTML "readonly" attribute to the string buffer.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    appendAttributeReadonly()  <span class="green">-></span>  <span class="st">readonly="readonly"</span> </pre>
     *
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer appendAttributeReadonly() {
        append(" readonly=\"readonly\"");

        return this;
    }

    /**
     * Append the given map of attribute names and values to the string buffer.
     *
     * @param attributes the map of attribute names and values
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     * @throws IllegalArgumentException if attributes is null
     */
    public HtmlStringBuffer appendAttributes(Map<String, String> attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("Null attributes parameter");
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String name = entry.getKey();
            if (!name.equals("id")) {
                appendAttributeEscaped(name, entry.getValue());
            }
        }

        return this;
    }

    /**
     * Append the given map of CSS style name and value pairs as a style
     * attribute to the string buffer.
     *
     * @param attributes the map of CSS style names and values
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     * @throws IllegalArgumentException if attributes is null
     */
    public HtmlStringBuffer appendStyleAttributes(Map<String, String> attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("Null attributes parameter");
        }

        if (!attributes.isEmpty()) {
            append(" style=\"");

            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                append(entry.getKey());
                append(":");
                append(entry.getValue());
                append(";");
            }

            append("\"");
        }

        return this;
    }

    /**
     * Append a HTML element end to the string buffer.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    elementEnd(<span class="st">"textarea"</span>)  <span class="green">-></span>  <span class="st">&lt;/textarea&gt;</span> </pre>
     *
     * @param name the HTML element name to end
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     * @throws IllegalArgumentException if name is null
     */
    public HtmlStringBuffer elementEnd(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }
        append("</");
        append(name);
        append(">");

        return this;
    }

    /**
     * Append a HTML element end to the string buffer.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    closeTag()  <span class="green">-></span>  <span class="st">&gt;</span> </pre>
     *
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer closeTag() {
        append(">");

        return this;
    }

    /**
     * Append a HTML element end to the string buffer.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    elementEnd()  <span class="green">-></span>  <span class="st">/&gt;</span> </pre>
     *
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer elementEnd() {
        append("/>");

        return this;
    }

    /**
     * Append a HTML element start to the string buffer.
     * <p/>
     * For example:
     * <pre class="javaCode">
     *    elementStart(<span class="st">"input"</span>)  <span class="green">-></span>  <span class="st">&lt;input</span> </pre>
     *
     * @param name the HTML element name to start
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     */
    public HtmlStringBuffer elementStart(String name) {
        append("<");
        append(name);

        return this;
    }

    /**
     * Return true if the given attribute name is a JavaScript attribute,
     * or false otherwise.
     *
     * @param name the HTML attribute name to test
     * @return true if the HTML attribute is a JavaScript attribute
     */
    public boolean isJavaScriptAttribute(String name) {
        if (name.length() < 6 || name.length() > 11) {
            return false;
        }

        if (!name.startsWith("on")) {
            return false;
        }

        for (String jsAttribute : JS_ATTRIBUTES) {
            if (jsAttribute.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the length of the string buffer.
     *
     * @return the length of the string buffer
     */
    public int length() {
        return count;
    }

    /**
     * @see Object#toString()
     *
     * @return a string representation of the string buffer
     */
    @Override
    public String toString() {
        return new String(characters, 0, count);
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Ensures that the capacity is at least equal to the specified minimum.
     * If the current capacity is less than the argument, then a new internal
     * array is allocated with greater capacity. The new capacity is the
     * larger of:
     * <ul>
     * <li>The <code>minimumCapacity</code> argument.
     * <li>Twice the old capacity, plus <code>2</code>.
     * </ul>
     * If the <code>minimumCapacity</code> argument is non-positive, this method
     * takes no action and simply returns.
     *
     * @param minimumCapacity the minimum desired capacity
     */
    protected void expandCapacity(int minimumCapacity) {
        int newCapacity = (characters.length + 1) * 2;

        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        } else if (minimumCapacity > newCapacity) {
            newCapacity = minimumCapacity;
        }

        char newValue[] = new char[newCapacity];
        System.arraycopy(characters, 0, newValue, 0, count);
        characters = newValue;
    }

    // Private Package Methods ------------------------------------------------

    /**
     * Append the given value to the buffer and HTML escape its value.
     *
     * @param value the object value to append
     * @return a reference to this <tt>HtmlStringBuffer</tt> object
     * @throws IllegalArgumentException if the value is null
     */
    HtmlStringBuffer appendHtmlEscaped(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Null value parameter");
        }

        String string = value.toString();

        boolean requiresEscape = false;
        for (int i = 0, size = string.length(); i < size; i++) {
            if (ClickUtils.requiresHtmlEscape(string.charAt(i))) {
                requiresEscape = true;
                break;
            }
        }

        if (requiresEscape) {
            ClickUtils.appendHtmlEscapeString(string, this);

        } else {
            append(value);
        }

        return this;
    }
}
