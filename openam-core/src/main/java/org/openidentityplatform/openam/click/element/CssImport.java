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
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Map;

/**
 * Provides a Css HEAD element for importing <tt>external</tt> Cascading
 * Stylesheet files using the &lt;link&gt; tag.
 * <p/>
 * Example usage:
 * <pre class="prettyprint">
 * public class MyPage extends Page {
 *
 *     public List getHeadElements() {
 *         // We use lazy loading to ensure the CSS import is only added the
 *         // first time this method is called.
 *         if (headElements == null) {
 *             // Get the head elements from the super implementation
 *             headElements = super.getHeadElements();
 *
 *             CssImport cssImport = new CssImport("/css/style.css");
 *             headElements.add(cssImport);
 *         }
 *         return headElements;
 *     }
 * } </pre>
 *
 * The <tt>cssImport</tt> instance will be rendered as follows (assuming the
 * context path is <tt>myApp</tt>):
 * <pre class="prettyprint">
 * &lt;link type="text/css" rel="stylesheet" href="/myApp/css/style.css"/&gt; </pre>
 */
public class CssImport extends ResourceElement {

    private static final long serialVersionUID = 1L;

    // Constructors -----------------------------------------------------------

    /**
     * Constructs a new Css import element.
     * <p/>
     * The CssImport {@link #setVersionIndicator(String) version indicator}
     * will automatically be set to the
     * {@link ClickUtils#getApplicationResourceVersionIndicator() application version indicator}.
     */
    public CssImport() {
        this(null);
    }

    /**
     * Construct a new Css import element with the specified <tt>href</tt>
     * attribute.
     * <p/>
     * The CssImport {@link #setVersionIndicator(String) version indicator}
     * will automatically be set to the
     * {@link ClickUtils#getApplicationResourceVersionIndicator() application version indicator}.
     * <p/>
     * <b>Please note</b> if the given <tt>href</tt> begins with a
     * <tt class="wr">"/"</tt> character the href will be prefixed with the web
     * application <tt>context path</tt>.
     *
     * @param href the Css import href attribute
     */
    public CssImport(String href) {
        this(href, true);
    }

    /**
     * Construct a new Css import element with the specified <tt>href</tt>
     * attribute.
     * <p/>
     * If useApplicationVersionIndicator is true the
     * CssImport {@link #setVersionIndicator(String) version indicator}
     * will automatically be set to the
     * {@link ClickUtils#getApplicationResourceVersionIndicator() application version indicator}.
     * <p/>
     * <b>Please note</b> if the given <tt>href</tt> begins with a
     * <tt class="wr">"/"</tt> character the href will be prefixed with the web
     * application <tt>context path</tt>.
     *
     * @param href the Css import href attribute
     * @param useApplicationVersionIndicator indicates whether the version
     * indicator will automatically be set to the application version indicator
     */
    public CssImport(String href, boolean useApplicationVersionIndicator) {
        this(href, null);
        if (useApplicationVersionIndicator) {
            setVersionIndicator(ClickUtils.getApplicationResourceVersionIndicator());
        }
    }

    /**
     * Construct a new Css import element with the specified <tt>href</tt>
     * attribute and version indicator.
     * <p/>
     * <b>Please note</b> if the given <tt>href</tt> begins with a
     * <tt class="wr">"/"</tt> character the href will be prefixed with the web
     * application <tt>context path</tt>.
     *
     * @param href the Css import href attribute
     * @param versionIndicator the version indicator to add to the href path
     */
    public CssImport(String href, String versionIndicator) {
        setHref(href);
        setAttribute("type", "text/css");
        setAttribute("rel", "stylesheet");
        setVersionIndicator(versionIndicator);
    }

    // Public Properties ------------------------------------------------------

    /**
     * Returns the Css import HTML tag: &lt;link&gt;.
     *
     * @return the Css import HTML tag: &lt;link&gt;
     */
    @Override
    public String getTag() {
        return "link";
    }

    /**
     * This method always return true because Css import must be unique based on
     * its <tt>href</tt> attribute. In other words the Page HEAD should only
     * contain a single CSS import for the specific <tt>href</tt>.
     *
     * @see ResourceElement#isUnique()
     *
     * @return true because Css import must unique based on its <tt>href</tt>
     * attribute
     */
    @Override
    public boolean isUnique() {
        return true;
    }

    /**
     * Sets the <tt>href</tt> attribute. If the given href argument is
     * <tt>null</tt>, the <tt>href</tt> attribute will be removed.
     * <p/>
     * If the given <tt>href</tt> begins with a <tt class="wr">"/"</tt> character
     * the href will be prefixed with the web applications <tt>context path</tt>.
     * Note if the given href is already prefixed with the <tt>context path</tt>,
     * Click won't add it a second time.
     *
     * @param href the new href attribute
     */
    public void setHref(String href) {
        if (href != null) {
            if (href.charAt(0) == '/') {
                Context context = getContext();
                String contextPath = context.getRequest().getContextPath();

                // Guard against adding duplicate context path
                if (!href.startsWith(contextPath + '/')) {
                    HtmlStringBuffer buffer =
                        new HtmlStringBuffer(contextPath.length() + href.length());

                    // Append the context path
                    buffer.append(contextPath);
                    buffer.append(href);
                    href = buffer.toString();
                }
            }
        }
        setAttribute("href", href);
    }

    /**
     * Return the <tt>href</tt> attribute.
     *
     * @return the href attribute
     */
    public String getHref() {
        return getAttribute("href");
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Render the HTML representation of the CssImport element to the specified
     * buffer.
     *
     * @param buffer the buffer to render output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {
        renderConditionalCommentPrefix(buffer);

        buffer.elementStart(getTag());

        if (isRenderId()) {
            buffer.appendAttribute("id", getId());
        }

        String href = getHref();
        renderResourcePath(buffer, "href", href);

        Map<String, String> localAttributes = getAttributes();
        for (Map.Entry<String, String> entry : localAttributes.entrySet()) {
            String name = entry.getKey();
            if (!name.equals("id") && !name.equals("href")) {
                buffer.appendAttributeEscaped(name, entry.getValue());
            }
        }

        buffer.elementEnd();

        renderConditionalCommentSuffix(buffer);
    }

    /**
     * @see Object#equals(Object)
     *
     * @param o the object with which to compare this instance with
     * @return true if the specified object is the same as this object
     */
    @Override
    public boolean equals(Object o) {
        if (getHref() == null) {
            throw new IllegalStateException("'href' attribute is not defined.");
        }

        //1. Use the == operator to check if the argument is a reference to this object.
        if (o == this) {
            return true;
        }

        //2. Use the instanceof operator to check if the argument is of the correct type.
        if (!(o instanceof CssImport)) {
            return false;
        }

        //3. Cast the argument to the correct type.
        CssImport that = (CssImport) o;

        return getHref() == null ? that.getHref() == null
            : getHref().equals(that.getHref());
    }

    /**
     * @see Object#hashCode()
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getHref()).toHashCode();
    }

}
