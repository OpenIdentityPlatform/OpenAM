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
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a Css HEAD element for including <tt>inline</tt> Cascading
 * Stylesheets using the &lt;style&gt; tag.
 * <p/>
 * Example usage:
 *
 * <pre class="prettyprint">
 * public class MyPage extends Page {
 *
 *     public List getHeadElements() {
 *         // We use lazy loading to ensure the CSS import is only added the
 *         // first time this method is called.
 *         if (headElements == null) {
 *             // Get the header entries from the super implementation
 *             headElements = super.getHeadElements();
 *
 *             CssStyle cssStyle = new CssStyle("body { font: 12px arial; }");
 *             headElements.add(cssStyle);
 *         }
 *         return headElements;
 *     }
 * } </pre>
 *
 * The <tt>cssStyle</tt> instance will render as follows:
 *
 * <pre class="prettyprint">
 * &lt;style type="text/css"&gt;
 * body { font: 12px arial; }
 * &lt;/style&gt; </pre>
 *
 * Below is an example showing how to render inline CSS from a Velocity
 * template.
 * <p/>
 * First we create a Velocity template <tt>(/css/style-template.css)</tt> which
 * contains the variable <tt>$context</tt> that must be replaced at runtime with
 * the application <tt>context path</tt>:
 *
 * <pre class="prettyprint">
 * .blue {
 *     background: #00ff00 url('$context/css/blue.png') no-repeat fixed center;
 * } </pre>
 *
 * Next is the Page implementation:
 *
 * <pre class="prettyprint">
 * public class MyPage extends Page {
 *
 *     public List getHeadElements() {
 *         // We use lazy loading to ensure the CSS is only added the first time
 *         // this method is called.
 *         if (headElements == null) {
 *             // Get the head elements from the super implementation
 *             headElements = super.getHeadElements();
 *
 *             Context context = getContext();
 *
 *             // Create a default template model to pass to the template
 *             Map model = ClickUtils.createTemplateModel(this, context);
 *
 *             // Specify the path to CSS template
 *             String templatePath = "/css/style-template.css";
 *
 *             // Create the inline Css for the given template path and model
 *             CssStyle cssStyle = new CssStyle(templatePath, model);
 *             headElements.add(cssStyle);
 *         }
 *         return headElements;
 *     }
 * } </pre>
 *
 * The <tt>Css</tt> above will render as follows (assuming the context path is
 * <tt>myApp</tt>):
 *
 * <pre class="prettyprint">
 * &lt;style type="text/css"&gt;
 * .blue {
 *     background: #00ff00 url('/myApp/css/blue.png') no-repeat fixed center;
 * }
 * &lt;/style&gt; </pre>
 *
 * <h3>Character data (CDATA) support</h3>
 *
 * Sometimes it is necessary to wrap <tt>inline</tt> {@link CssStyle Css} in
 * CDATA tags. Two use cases are common for doing this:
 * <ul>
 * <li>For XML parsing: When using Ajax one often send back partial
 * XML snippets to the browser, which is parsed as valid XML. However the XML
 * parser will throw an error if the content contains reserved XML characters
 * such as '&amp;', '&lt;' and '&gt;'. For these situations it is recommended
 * to wrap the style content inside CDATA tags.
 * </li>
 * <li>XHTML validation: if you want to validate your site using an XHTML
 * validator e.g: <a target="_blank" href="http://validator.w3.org/">http://validator.w3.org/</a>.</li>
 * </ul>
 *
 * To wrap the CSS Style content in CDATA tags, set
 * {@link #setCharacterData(boolean)} to true. Below is shown how the Css
 * content would be rendered:
 *
 * <pre class="codeHtml">
 * &lt;style type="text/css"&gt;
 *  <span style="color:#3F7F5F">/&lowast;&lt;![CDATA[&lowast;/</span>
 *
 *  div &gt; p {
 *    border: 1px solid black;
 *  }
 *
 *  <span style="color:#3F7F5F">/&lowast;]]&gt;&lowast;/</span>
 * &lt;/style&gt; </pre>
 *
 * Notice the CDATA tags are commented out which ensures older browsers that
 * don't understand the CDATA tag, will ignore it and only process the actual
 * content.
 * <p/>
 * For an overview of XHTML validation and CDATA tags please see
 * <a target="_blank" href="http://javascript.about.com/library/blxhtml.htm">http://javascript.about.com/library/blxhtml.htm</a>.
 */
public class CssStyle extends ResourceElement {

    private static final long serialVersionUID = 1L;

     // Variables -------------------------------------------------------------

    /** The inline Css content. */
    private String content;

    /**
     * Indicates if the HeadElement's content should be wrapped in a CDATA tag.
     */
    private boolean characterData;

    /** The path of the template to render. */
    private String template;

    /** The model of the template to render. */
    private Map<String, Object> model;

    // Constructor ------------------------------------------------------------

    /**
     * Construct a new Css style element.
     */
    public CssStyle() {
        this(null);
    }

    /**
     * Construct a new Css style element with the given content.
     *
     * @param content the Css content
     */
    public CssStyle(String content) {
        if (content != null) {
            this.content = content;
        }
        setAttribute("type", "text/css");
    }

    /**
     * Construct a new Css style element for the given template path
     * and template model.
     * <p/>
     * When the CssStyle is rendered the template and model will be merged and
     * the result will be rendered together with any CssStyle
     * {@link #setContent(String) content}.
     * <p/>
     *
     * For example:
     * <pre class="prettyprint">
     * public class MyPage extends Page {
     *     public void onInit() {
     *         Context context = getContext();
     *
     *         // Create a default template model
     *         Map model = ClickUtils.createTemplateModel(this, context);
     *
     *         // Create CssStyle for the given template path and model
     *         CssStyle style = new CssStyle("/mypage-template.css", model);
     *
     *         // Add style to the Page Head elements
     *         getHeadElements().add(style);
     *     }
     * } </pre>
     *
     * @param template the path of the template to render
     * @param model the template model
     */
    public CssStyle(String template, Map<String, Object> model) {
        this(null);
        setTemplate(template);
        setModel(model);
    }

    // Public Properties ------------------------------------------------------

    /**
     * Returns the Css HTML tag: &lt;style&gt;.
     *
     * @return the Css HTML tag: &lt;style&gt;
     */
    @Override
    public String getTag() {
        return "style";
    }

    /**
     * Return the CssStyle content.
     *
     * @return the CssStyle content
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the CssStyle content.
     *
     * @param content the CssStyle content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Return true if the CssStyle's content should be wrapped in CDATA tags,
     * false otherwise.
     *
     * @return true if the CssStyle's content should be wrapped in CDATA tags,
     * false otherwise
     */
    public boolean isCharacterData() {
        return characterData;
    }

    /**
     * Sets whether the CssStyle's content should be wrapped in CDATA tags or
     * not.
     *
     * @param characterData true indicates that the CssStyle's content should be
     * wrapped in CDATA tags, false otherwise
     */
    public void setCharacterData(boolean characterData) {
        this.characterData = characterData;
    }

    /**
     * Return the path of the template to render.
     *
     * @see #setTemplate(String)
     *
     * @return the path of the template to render
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Set the path of the template to render.
     * <p/>
     * If the {@link #template} property is set, the template and {@link #model}
     * will be merged and the result will be rendered together with any CssStyle
     * {@link #setContent(String) content}.
     *
     * @param template the path of the template to render
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Return the model of the {@link #setTemplate(String) template}
     * to render.
     *
     * @see #setModel(Map)
     *
     * @return the model of the template to render
     */
    public Map<String, Object> getModel() {
        return model;
    }

    /**
     * Set the model of the template to render.
     * <p/>
     * If the {@link #template} property is set, the template and {@link #model}
     * will be merged and the result will be rendered together with any CssStyle
     * {@link #setContent(String) content}.
     *
     * @param model the model of the template to render
     */
    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Render the HTML representation of the CssStyle element to the specified
     * buffer.
     *
     * @param buffer the buffer to render output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {

        // Render IE conditional comment if conditional comment was set
        renderConditionalCommentPrefix(buffer);

        buffer.elementStart(getTag());

        if (isRenderId()) {
            buffer.appendAttribute("id", getId());
        }

        appendAttributes(buffer);

        buffer.closeTag();

        buffer.append("\n");

        // Render CDATA tag if necessary
        renderCharacterDataPrefix(buffer);

        renderContent(buffer);

        renderCharacterDataSuffix(buffer);

        buffer.append("\n");

        buffer.elementEnd(getTag());

        renderConditionalCommentSuffix(buffer);
    }

    /**
     * @see Object#equals(Object)
     *
     * @param o the object with which to compare this instance with
     *
     * @return true if the specified object is the same as this object
     */
    @Override
    public boolean equals(Object o) {
        if (!isUnique()) {
            return super.equals(o);
        }

        //1. Use the == operator to check if the argument is a reference to this object.
        if (o == this) {
            return true;
        }

        //2. Use the instanceof operator to check if the argument is of the correct type.
        if (!(o instanceof CssStyle)) {
            return false;
        }

        //3. Cast the argument to the correct type.
        CssStyle that = (CssStyle) o;

        String id = getId();
        String thatId = that.getId();
        return id == null ? thatId == null : id.equals(thatId);
    }

    /**
     * @see Object#hashCode()
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        if (!isUnique()) {
            return super.hashCode();
        }
        return new HashCodeBuilder(17, 37).append(getId()).toHashCode();
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Render the CssStyle {@link #setContent(String) content}
     * to the specified buffer.
     * <p/>
     * <b>Please note:</b> if the {@link #setTemplate(String) template}
     * property is set, this method will merge the {@link #setTemplate(String) template}
     * and {@link #setModel(Map) model} and the result will be
     * rendered, together with the CssStyle
     * {@link #setContent(String) content},
     * to the specified buffer.
     *
     * @param buffer the buffer to append the output to
     */
    protected void renderContent(HtmlStringBuffer buffer) {
        if (getTemplate() != null) {
            Context context = getContext();

            Map<String, Object> templateModel = getModel();
            if (templateModel == null) {
                templateModel = new HashMap<String, Object>();
            }
            buffer.append(context.renderTemplate(getTemplate(), templateModel));

        }

        if (getContent() != null) {
            buffer.append(getContent());
        }
    }

    // Package Private Methods ------------------------------------------------

    /**
     * Render the CDATA tag prefix to the specified buffer if
     * {@link #isCharacterData()} returns true. The default value is
     * <tt>/&lowast;&lt;![CDATA[&lowast;/</tt>.
     *
     * @param buffer buffer to append the conditional comment prefix
     */
    void renderCharacterDataPrefix(HtmlStringBuffer buffer) {
        // Wrap character data in CDATA block
        if (isCharacterData()) {
            buffer.append("/*<![CDATA[*/ ");
        }
    }

    /**
     * Render the CDATA tag suffix to the specified buffer if
     * {@link #isCharacterData()} returns true. The default value is
     * <tt>/&lowast;]]&gt;&lowast;/</tt>.
     *
     * @param buffer buffer to append the conditional comment prefix
     */
    void renderCharacterDataSuffix(HtmlStringBuffer buffer) {
        if (isCharacterData()) {
            buffer.append(" /*]]>*/");
        }
    }
}
