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
package org.openidentityplatform.openam.click;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

import org.openidentityplatform.openam.click.util.ClickUtils;

/**
 * Provides an ActionResult that is returned by Page Actions and AjaxBehaviors.
 * ActionResults are often used to return a partial response to the browser
 * instead of the full page content.
 * <p/>
 * An ActionResult can consist of a String (HTML, JSON, XML, plain text) or a byte
 * array (jpg, gif, png, pdf or excel documents). The ActionResult {@link #contentType}
 * must be set appropriately in order for the browser to recognize the action result.
 * <p/>
 * ActionResults are returned by {@link org.apache.click.ajax.AjaxBehavior Ajax Behaviors}
 * and <tt>Page Action</tt> methods.
 *
 * <h3>Ajax Behavior</h3>
 *
 * Ajax requests are handled by adding an {@link org.apache.click.ajax.AjaxBehavior Ajax Behavior}
 * to a control. The AjaxBehavior {@link org.apache.click.ajax.AjaxBehavior#onAction(org.apache.click.Control) onAction}
 * method will handle the request and return a ActionResult instance that contains
 * the response, thus bypassing the rendering of the Page template. For example:
 *
 * <pre class="prettyprint">
 * private ActionLink link = new ActionLink("link");
 *
 * public void onInit() {
 *     addControl(link);
 *
 *     link.addBehavior(new AjaxBehavior() {
 *
 *         // The onAction method must return a ActionResult
 *         public ActionResult onAction(Control source) {
 *             // Create a new action result containing an HTML snippet and HTML content type
 *             ActionResult actionResult = new ActionResult("&lt;span&gt;Hello World&lt;/span&gt;", ActionResult.HTML);
 *             return actionResult;
 *         }
 *     });
 * } </pre>
 *
 * <h3>Page Action</h3>
 *
 * A <tt>Page Action</tt> is a method on a Page that can be invoked directly
 * from the browser. The Page Action method returns an ActionResult instance that
 * is rendered to the browser, thus bypassing the rendering of the Page template.
 *
 * <pre class="prettyprint">
 * private ActionLink link = new ActionLink("link");
 *
 * public void onInit() {
 *     link.addControl(link);
 *
 *     // A "pageAction" is set as a parameter on the link. The "pageAction"
 *     // value is set to the Page method: "renderHelloWorld"
 *     link.setParameter(PAGE_ACTION, "renderHelloWorld");
 * }
 *
 * &#47;**
 *  * This is a "pageAction" method that will render an HTML response.
 *  *
 *  * Note the signature of the pageAction: a public, no-argument method
 *  * returning a ActionResult instance.
 *  *&#47;
 * public ActionResult renderHelloWorld() {
 *     ActionResult actionResult = new ActionResult("&lt;span&gt;Hello World&lt;/span&gt;", ActionResult.HTML);
 *     return actionResult;
 * } </pre>
 *
 * <h3>Content Type</h3>
 *
 * The {@link #contentType} of the ActionResult must be set to the appropriate type
 * in order for the client to recognize the response. ActionResult provides constants
 * for some of the common <tt>content types</tt>, including: {@link #XML text/xml},
 * {@link #HTML text/html}, {@link #JSON application/json}, {@link #TEXT text/plain}.
 * <p/>
 * For example:
 * <pre class="prettyprint">
 * ActionResult actionResult = new ActionResult("alert('hello world');", ActionResult.JAVASCRIPT);
 *
 * ...
 *
 * // content type can also be set through the setContentType method
 * actionResult.setContentType(ActionResult.JAVASCRIPT);
 *
 * ...
 * </pre>
 *
 * More content types can be retrieved through {@link org.apache.click.util.ClickUtils#getMimeType(java.lang.String)}:
 * <pre class="prettyprint">
 * // lookup content type for PNG
 * String contentType = ClickUtils.getMimeType("png");
 * actionResult.setContentType(contentType); </pre>
 */
public class ActionResult {

    // Constants --------------------------------------------------------------

    /** The plain text content type constant: <tt>text/plain</tt>. */
    public static final String TEXT = "text/plain";

    /** The html content type constant: <tt>text/html</tt>. */
    public static final String HTML = "text/html";

    /** The The xhtml content type constant: <tt>application/xhtml+xml</tt>. */
    public static final String XHTML = "application/xhtml+xml";

    /** The json content type constant: <tt>text/json</tt>. */
    public static final String JSON = "application/json";

    /** The javascript content type constant: <tt>text/javascript</tt>. */
    public static final String JAVASCRIPT = "text/javascript";

    /** The xml content type constant: <tt>text/xml</tt>. */
    public static final String XML = "text/xml";

    /** The ActionResult writer buffer size. */
    private static final int WRITER_BUFFER_SIZE = 256; // For text, set a small response size

    /** The ActionResult output buffer size. */
    private static final int OUTPUT_BUFFER_SIZE = 4 * 1024; // For binary, set a a large response size

    // Variables --------------------------------------------------------------

    /** The content to render. */
    private String content;

    /** The content as a byte array. */
    private byte[] bytes;

    /** The servlet response reader. */
    private Reader reader;

    /** The servlet response input stream. */
    private InputStream inputStream;

    /** The response content type. */
    private String contentType;

    /** The response character encoding. */
    private String characterEncoding;

    /** Indicates whether the ActionResult should be cached by browser. */
    private boolean cacheActionResult = false;

    /** The path of the actionResult template to render. */
    private String template;

    /** The model for the ActionResult {@link #template}. */
    private Map<String, Object> model;

    // Constructors -----------------------------------------------------------

    /**
     * Construct the ActionResult for the given template and model.
     * <p/>
     * When the ActionResult is rendered the template and model will be merged and
     * the result will be streamed back to the client.
     * <p/>
     * For example:
     * <pre class="prettyprint">
     * public class MyPage extends Page {
     *     public void onInit() {
     *
     *         Behavior behavior = new DefaultAjaxBehavior() {
     *
     *             public ActionResult onAction() {
     *
     *                 Map model = new HashMap();
     *                 model.put("id", "link");
     *
     *                 // Note: we set XML as the content type
     *                 ActionResult actionResult = new ActionResult("/js/actionResult.xml", model, ActionResult.XML);
     *
     *                 return actionResult;
     *             }
     *         }
     *     }
     * } </pre>
     *
     * @param template the template to render and stream back to the client
     * @param model the template data model
     * @param contentType the response content type
     */
    public ActionResult(String template, Map<String, Object> model, String contentType) {
        this.template = template;
        this.model = model;
        this.contentType = contentType;
    }

    /**
     * Construct the ActionResult for the given reader and content type.
     *
     * @param reader the reader which characters must be streamed back to the
     * client
     * @param contentType the response content type
     */
    public ActionResult(Reader reader, String contentType) {
        this.reader = reader;
        this.contentType = contentType;
    }

    /**
     * Construct the ActionResult for the given inputStream and content type.
     *
     * @param inputStream the input stream to stream back to the client
     * @param contentType the response content type
     */
    public ActionResult(InputStream inputStream, String contentType) {
        this.inputStream = inputStream;
        this.contentType = contentType;
    }

    /**
     * Construct the ActionResult for the given String content and content type.
     *
     * @param content the String content to stream back to the client
     * @param contentType the response content type
     */
    public ActionResult(String content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    /**
     * Construct the ActionResult for the given byte array and content type.
     *
     * @param bytes the byte array to stream back to the client
     * @param contentType the response content type
     */
    public ActionResult(byte[] bytes, String contentType) {
        this.bytes = bytes;
        this.contentType = contentType;
    }

    /**
     * Construct the ActionResult for the given content. The
     * <tt>{@link jakarta.servlet.http.HttpServletResponse#setContentType(java.lang.String) response content type}</tt>
     * will default to {@link #TEXT}, unless overridden.
     *
     * @param content the content to stream back to the client
     */
    public ActionResult(String content) {
        this.content = content;
    }

    /**
     * Construct a new empty ActionResult. The
     * <tt>{@link jakarta.servlet.http.HttpServletResponse#setContentType(java.lang.String) response content type}</tt>
     * will default to {@link #TEXT}, unless overridden.
     */
    public ActionResult() {
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Set whether the action result should be cached by the client browser or
     * not.
     * <p/>
     * If false, Click will set the following headers to prevent browsers
     * from caching the result:
     * <pre class="prettyprint">
     * response.setHeader("Pragma", "no-cache");
     * response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
     * response.setDateHeader("Expires", new Date(1L).getTime()); </pre>
     *
     * @param cacheActionResult indicates whether the action result should be cached
     * by the client browser or not
     */
    public void setCacheActionResult(boolean cacheActionResult) {
        this.cacheActionResult = cacheActionResult;
    }

    /**
     * Return true if the action result should be cached by the client browser,
     * defaults to false. It is highly unlikely that you would turn action result
     * caching on.
     *
     * @return true if the action result should be cached by the client browser,
     * false otherwise
     */
    public boolean isCacheActionRestul() {
        return cacheActionResult;
    }

    /**
     * Return the action result character encoding. If no character encoding is specified
     * the request character encoding will be used.
     *
     * @return the action result character encoding
     */
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    /**
     * Set the action result character encoding. If no character encoding is set the
     * request character encoding will be used.
     *
     * @param characterEncoding the action result character encoding
     */
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    /**
     * Set the action result response content type. If no content type is set it will
     * default to {@value #TEXT}.
     *
     * @param contentType the action result response content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Return the action result content type, default is {@value #TEXT}.
     *
     * @return the response content type
     */
    public String getContentType() {
        if (contentType == null) {
            contentType = TEXT;
        }
        return contentType;
    }

    /**
     * Set the content String to stream back to the client.
     *
     * @param content the content String to stream back to the client
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Return the content String to stream back to the client.
     *
     * @return the content String to stream back to the client
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the byte array to stream back to the client.
     *
     * @param bytes the byte array to stream back to the client
     */
    public void setBytes(byte[] bytes, String contentType) {
        this.bytes = bytes;
        this.contentType = contentType;
    }

    /**
     * Return the byte array to stream back to the client.
     *
     * @return the byte array to stream back to the client
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Set the content to stream back to the client.
     *
     * @param inputStream the inputStream to stream back to the client
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Return the inputStream to stream back to the client.
     *
     * @return the inputStream to stream back to the client
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Set the reader which characters are streamed back to the client.
     *
     * @param reader the reader which characters are streamed back to the client.
     */
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    /**
     * Return the reader which characters are streamed back to the client.
     *
     * @return the reader which characters are streamed back to the client.
     */
    public Reader getReader() {
        return reader;
    }

    /**
     * Return the data model for the ActionResult {@link #template}.
     *
     * @return the data model for the ActionResult template
     */
    public Map<String, Object> getModel() {
        if (model == null) {
            model = new HashMap<String, Object>();
        }
        return model;
    }

    /**
     * Set the model of the ActionResult template to render.
     * <p/>
     * If the {@link #template} property is set, the template and {@link #model}
     * will be merged and the result will be streamed back to the client.
     *
     * @param model the model of the template to render
     */
    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    /**
     * Return the template to render for this ActionResult.
     *
     * @return the template to render for this ActionResult
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Set the template to render for this ActionResult.
     *
     * @param template the template to render for this ActionResult
     */
    public void setTemplate(String template) {
        this.template = template;
    }

    /**
     * Render the ActionResult to the client.
     *
     * @param context the request context
     */
    public final void render(Context context) {
        prepare(context);
        renderActionResult(context);
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Render the ActionResult to the client. This method can be overridden
     * by subclasses if custom rendering or direct access to the
     * HttpServletResponse is required.
     *
     * @param context the request context
     */
    protected void renderActionResult(Context context) {

        HttpServletResponse response = context.getResponse();

        Reader localReader = getReader();
        InputStream localInputStream = getInputStream();

        try {
            String localContent = getContent();
            byte[] localBytes = getBytes();

            String localTemplate = getTemplate();
            if (localTemplate != null) {
                Map<String, Object> templateModel = getModel();
                if (templateModel == null) {
                    templateModel = new HashMap<String, Object>();
                }
                String result = context.renderTemplate(localTemplate, templateModel);
                localReader = new StringReader(result);

            } else if (localContent != null) {
                localReader = new StringReader(localContent);
            } else if (localBytes != null) {
                localInputStream = new ByteArrayInputStream(localBytes);
            }

            if (localReader != null) {
                Writer writer = response.getWriter();
                char[] buffer = new char[WRITER_BUFFER_SIZE];
                int len = 0;
                while (-1 != (len = localReader.read(buffer))) {
                    writer.write(buffer, 0, len);
                }
                writer.flush();
                writer.close();

            } else if (localInputStream != null) {
                byte[] buffer = new byte[OUTPUT_BUFFER_SIZE];
                int len = 0;
                OutputStream outputStream = response.getOutputStream();
                while (-1 != (len = localInputStream.read(buffer))) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
                outputStream.close();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            ClickUtils.close(localInputStream);
            ClickUtils.close(localReader);
        }
    }

    // Private Methods --------------------------------------------------------

    /**
     * Prepare the ActionResult for rendering.
     *
     * @param context the request context
     */
    private void prepare(Context context) {
        HttpServletResponse response = context.getResponse();

        if (!isCacheActionRestul()) {
            // Set headers to disable cache
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
            response.setDateHeader("Expires", new Date(1L).getTime());
        }

        String localContentType = getContentType();

        if (getCharacterEncoding() == null) {

            // Fallback to request character encoding
            if (context.getRequest().getCharacterEncoding() != null) {
                response.setContentType(localContentType + "; charset="
                        + context.getRequest().getCharacterEncoding());
            } else {
                response.setContentType(localContentType);
            }

        } else {
            response.setContentType(localContentType + "; charset=" + getCharacterEncoding());
        }
    }
}
