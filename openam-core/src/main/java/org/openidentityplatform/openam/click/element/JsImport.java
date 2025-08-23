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
 * Provides a JavaScript HEAD element for importing <tt>external</tt> JavaScript
 * files using the &lt;script&gt; tag.
 * <p/>
 * Example usage:
 * <pre class="prettyprint">
 * public class MyPage extends Page {
 *
 *     public List getHeadElements() {
 *         // We use lazy loading to ensure the JS import is only added the
 *         // first time this method is called.
 *         if (headElements == null) {
 *             // Get the head elements from the super implementation
 *             headElements = super.getHeadElements();
 *
 *             JsImport jsImport = new JsImport("/js/js-library.js");
 *             headElements.add(jsImport);
 *         }
 *         return headElements;
 *     }
 * } </pre>
 *
 * The <tt>jsImport</tt> instance will be rendered as follows (assuming the context
 * path is <tt>myApp</tt>):
 * <pre class="prettyprint">
 * &lt;script type="text/javascript" href="/myApp/js/js-library.js"&gt;&lt;/script&gt; </pre>
 */
public class JsImport extends ResourceElement {

    private static final long serialVersionUID = 1L;

    // Constructors -----------------------------------------------------------

    /**
     * Constructs a new JavaScript import element.
     * <p/>
     * The JsImport {@link #setVersionIndicator(String) version indicator}
     * will automatically be set to the
     * {@link ClickUtils#getApplicationResourceVersionIndicator() application version indicator}.
     */
    public JsImport() {
        this(null);
    }

    /**
     * Construct a new JavaScript import element with the specified
     * <tt>src</tt> attribute.
     * <p/>
     * The JsImport {@link #setVersionIndicator(String) version indicator}
     * will automatically be set to the
     * {@link ClickUtils#getApplicationResourceVersionIndicator() application version indicator}.
     * <p/>
     * <b>Please note</b> if the given <tt>src</tt> begins with a
     * <tt class="wr">"/"</tt> character the src will be prefixed with the web
     * application <tt>context path</tt>.
     *
     * @param src the JavaScript import src attribute
     */
    public JsImport(String src) {
        this(src, true);
    }

    /**
     * Construct a new JavaScript import element with the specified <tt>src</tt>
     * attribute.
     * <p/>
     * If useApplicationVersionIndicator is true the
     * {@link #setVersionIndicator(String) version indicator} will
     * automatically be set to the
     * {@link ClickUtils#getApplicationResourceVersionIndicator() application version indicator}.
     * <p/>
     * <b>Please note</b> if the given <tt>src</tt> begins with a
     * <tt class="wr">"/"</tt> character the src will be prefixed with the web
     * application <tt>context path</tt>.
     *
     * @param src the JavaScript import src attribute
     * @param useApplicationVersionIndicator indicates whether the version
     * indicator will automatically be set to the application version indicator
     */
    public JsImport(String src, boolean useApplicationVersionIndicator) {
        this(src, null);
        if (useApplicationVersionIndicator) {
            setVersionIndicator(ClickUtils.getApplicationResourceVersionIndicator());
        }
    }

    /**
     * Construct a new JavaScript import element with the specified <tt>src</tt>
     * attribute and version indicator.
     * <p/>
     * <b>Please note</b> if the given <tt>src</tt> begins with a
     * <tt class="wr">"/"</tt> character the src will be prefixed with the web
     * application <tt>context path</tt>.
     *
     * @param src the JsImport src attribute
     * @param versionIndicator the version indicator to add to the src path
     */
    public JsImport(String src, String versionIndicator) {
        setSrc(src);
        setAttribute("type", "text/javascript");
        setVersionIndicator(versionIndicator);
    }

    // Public Properties ------------------------------------------------------

    /**
     * Returns the JavaScript import HTML tag: &lt;script&gt;.
     *
     * @return the JavaScript import HTML tag: &lt;script&gt;
     */
    @Override
    public String getTag() {
        return "script";
    }

    /**
     * This method always return true because a JavaScript import must be unique
     * based on its <tt>src</tt> attribute. In other words the Page HEAD should
     * only contain a single JavaScript import for the specific <tt>src</tt>.
     *
     * @see ResourceElement#isUnique()
     *
     * @return true because JavaScript import must unique based on its
     * <tt>src</tt> attribute
     */
    @Override
    public boolean isUnique() {
        return true;
    }

    /**
     * Sets the <tt>src</tt> attribute. If the given src argument is
     * <tt>null</tt>, the <tt>src</tt> attribute will be removed.
     * <p/>
     * If the given <tt>src</tt> begins with a <tt class="wr">"/"</tt> character
     * the src will be prefixed with the web application <tt>context path</tt>.
     * Note if the given src is already prefixed with the <tt>context path</tt>,
     * Click won't add it a second time.
     *
     * @param src the new src attribute
     */
    public void setSrc(String src) {
        if (src != null) {
            if (src.charAt(0) == '/') {
                Context context = getContext();
                String contextPath = context.getRequest().getContextPath();

                // Guard against adding duplicate context path
                if (!src.startsWith(contextPath + '/')) {
                    HtmlStringBuffer buffer =
                        new HtmlStringBuffer(contextPath.length() + src.length());

                    // Append the context path
                    buffer.append(contextPath);
                    buffer.append(src);
                    src = buffer.toString();
                }
            }
        }
        setAttribute("src", src);
    }

    /**
     * Return the <tt>src</tt> attribute.
     *
     * @return the src attribute
     */
    public String getSrc() {
        return getAttribute("src");
    }

    // Package Private Methods ------------------------------------------------

    /**
     * Render the HTML representation of the JsImport element to the specified
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

        String src = getSrc();
        renderResourcePath(buffer, "src", src);

        Map<String, String> localAttributes = getAttributes();
        for (Map.Entry<String, String> entry : localAttributes.entrySet()) {
            String name = entry.getKey();
            if (!name.equals("id") && !name.equals("src")) {
                buffer.appendAttributeEscaped(name, entry.getValue());
            }
        }

        buffer.closeTag();

        buffer.elementEnd(getTag());

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
        //1. Use the == operator to check if the argument is a reference to this object.
        if (o == this) {
            return true;
        }

        //2. Use the instanceof operator to check if the argument is of the correct type.
        if (!(o instanceof JsImport)) {
            return false;
        }

        //3. Cast the argument to the correct type.
        JsImport that = (JsImport) o;

        return (getSrc() == null) ? (that.getSrc() == null)
                                  : (getSrc().equals(that.getSrc()));
    }

    /**
     * @see Object#hashCode()
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getSrc()).toHashCode();
    }
}
