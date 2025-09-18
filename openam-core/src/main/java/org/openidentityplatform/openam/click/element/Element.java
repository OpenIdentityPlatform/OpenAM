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
package org.openidentityplatform.openam.click.element;

import org.openidentityplatform.openam.click.Context;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a base class for rendering HTML elements, for example
 * JavaScript (&lt;script&gt;) and Cascading Stylesheets
 * (&lt;link&gt; / &lt;style&gt;).
 * <p/>
 * Subclasses should override {@link #getTag()} to return a specific HTML tag.
 */
public class Element implements Serializable {

    private static final long serialVersionUID = 1L;

    // Variables --------------------------------------------------------------

    /** The Element attributes Map. */
    private Map<String, String> attributes;

    // Public Properties ------------------------------------------------------

    /**
     * Returns the Element HTML tag, the default value is <tt>null</tt>.
     * <p/>
     * Subclasses should override this method and return the correct tag.
     *
     * @return this Element HTML tag
     */
    public String getTag() {
        return null;
    }

    /**
     * Return the HTML attribute with the given name, or null if the
     * attribute does not exist.
     *
     * @param name the name of link HTML attribute
     * @return the link HTML attribute
     */
     public String getAttribute(String name) {
        if (hasAttributes()) {
            return getAttributes().get(name);
        }
        return null;
    }

    /**
     * Set the Element attribute with the given attribute name and value.
     *
     * @param name the attribute name
     * @param value the attribute value
     * @throws IllegalArgumentException if name parameter is null
     */
    public void setAttribute(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }

        if (value != null) {
            getAttributes().put(name, value);
        } else {
            getAttributes().remove(name);
        }
    }

    /**
     * Return the Element attributes Map.
     *
     * @return the Element attributes Map.
     */
    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<String, String>();
        }
        return attributes;
    }

    /**
     * Return true if the Element has attributes or false otherwise.
     *
     * @return true if the Element has attributes on false otherwise
     */
    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    /**
     * Returns true if specified attribute is defined, false otherwise.
     *
     * @param name the specified attribute to check
     * @return true if name is a defined attribute
     */
    public boolean hasAttribute(String name) {
        return hasAttributes() && getAttributes().containsKey(name);
    }

    /**
     * Return the "id" attribute value or null if no id is defined.
     *
     * @return HTML element identifier attribute "id" value or null if no id
     * is defined
     */
    public String getId() {
        return getAttribute("id");
    }

    /**
     * Set the HTML id attribute for the with the given value.
     *
     * @param id the element HTML id attribute value to set
     */
    public void setId(String id) {
        if (id != null) {
            setAttribute("id", id);
        } else {
            getAttributes().remove("id");
        }
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Return the thread local Context.
     *
     * @return the thread local Context
     */
    public Context getContext() {
        return Context.getThreadLocalContext();
    }

    /**
     * Render the HTML representation of the Element to the specified buffer.
     * <p/>
     * If {@link #getTag()} returns null, this method will return an empty
     * string.
     *
     * @param buffer the specified buffer to render the Element output to
     */
    public void render(HtmlStringBuffer buffer) {
        if (getTag() == null) {
            return;
        }
        renderTagBegin(getTag(), buffer);
        renderTagEnd(getTag(), buffer);

    }

    /**
     * Return the HTML string representation of the Element.
     *
     * @return the HTML string representation of the Element
     */
    @Override
    public String toString() {
        if (getTag() == null) {
            return "";
        }
        HtmlStringBuffer buffer = new HtmlStringBuffer(getElementSizeEst());
        render(buffer);
        return buffer.toString();
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Append all the Element attributes to the specified buffer.
     *
     * @param buffer the specified buffer to append all the attributes
     */
    protected void appendAttributes(HtmlStringBuffer buffer) {
        if (hasAttributes()) {
            buffer.appendAttributes(attributes);
        }
    }

    // Package Private Methods ------------------------------------------------

    /**
     * Render the specified {@link #getTag() tag} and {@link #getAttributes()}.
     * <p/>
     * <b>Please note:</b> the tag will not be closed by this method. This
     * enables callers of this method to append extra attributes as needed.
     * <p/>
     * For example the result of calling:
     * <pre class="prettyprint">
     * Field field = new TextField("mytext");
     * HtmlStringBuffer buffer = new HtmlStringBuffer();
     * field.renderTagBegin("div", buffer);
     * </pre>
     * will be:
     * <pre class="prettyprint">
     * &lt;div name="mytext" id="mytext"
     * </pre>
     * Note that the tag is not closed.
     *
     * @param tagName the name of the tag to render
     * @param buffer the buffer to append the output to
     */
    void renderTagBegin(String tagName, HtmlStringBuffer buffer) {
        if (tagName == null) {
            throw new IllegalStateException("Tag cannot be null");
        }

        buffer.elementStart(tagName);

        buffer.appendAttribute("id", getId());
        appendAttributes(buffer);
    }

    /**
     * Closes the specified {@link #getTag() tag}.
     *
     * @param tagName the name of the tag to close
     * @param buffer the buffer to append the output to
     */
    void renderTagEnd(String tagName, HtmlStringBuffer buffer) {
        buffer.elementEnd();
    }

    /**
     * Return the estimated rendered element size in characters.
     *
     * @return the estimated rendered element size in characters
     */
    int getElementSizeEst() {
        int size = 0;
        if (getTag() != null && hasAttributes()) {
            //length of the markup -> </> == 3
            //1 * tag.length()
            size += 3 + getTag().length();
            //using 20 as an estimate
            size += 20 * getAttributes().size();
        }
        return size;
    }
}
