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
import org.openidentityplatform.openam.click.Control;
import org.openidentityplatform.openam.click.Page;
import org.apache.click.Stateful;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Submit;
import org.openidentityplatform.openam.click.element.CssImport;
import org.openidentityplatform.openam.click.element.Element;
import org.openidentityplatform.openam.click.element.JsImport;
import org.apache.click.service.FileUploadService;
import org.openidentityplatform.openam.click.service.LogService;
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.openidentityplatform.openam.click.util.ContainerUtils;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Provides a Form control: &nbsp; &lt;form method='post'&gt;.
 *
 * <table class='htmlHeader' cellspacing='12'>
 * <tr>
 * <td>
 *
 * <table class='fields'>
 * <tr>
 * <td align='left'><label>Username</label><span class="red">*</span></td>
 * <td align='left'><input type='text' name='username' value='' size='20' maxlength='20' /></td>
 * </tr>
 * <tr>
 * <td align='left'><label>Password</label><span class="red">*</span></td>
 * <td align='left'><input type='password' name='password' value='' size='20' maxlength='20' /></td>
 * </tr>
 * </table>
 * <table class="buttons">
 * <tr><td>
 * <input type='submit' name='ok' value='  OK  '/>&nbsp;<input type='submit' name='cancel' value=' Cancel '/>
 * </td></tr>
 * </table>
 *
 * </td>
 * </tr>
 * </table>
 *
 * When a Form is processed it will process its {@link Field} controls
 * in the order they were added to the form, and then it will process the
 * {@link Button} controls in the added order. Once all the Fields have been
 * processed the form will invoke its action listener if defined.
 *
 * <h3>Form Example</h3>
 *
 * The example below illustrates a Form being used in a login Page.
 *
 * <pre class="prettyprint">
 * public class Login extends Page {
 *
 *     public Form form = new Form();
 *
 *     public Login() {
 *         form.add(new TextField("username", true));
 *         form.add(new PasswordField("password", true));
 *         form.add(new Submit("ok", "  OK  ", this, "onOkClick"));
 *         form.add(new Submit("cancel", this, "onCancelClick"));
 *     }
 *
 *     public boolean onOkClick() {
 *         if (form.isValid()) {
 *             User user = new User();
 *             form.copyTo(user);
 *
 *             if (getUserService().isAuthenticatedUser(user)) {
 *                 getContext().setSessionAttribute("user", user);
 *                 setRedirect(HomePage.class);
 *             } else {
 *                 form.setError(getMessage("authentication-error"));
 *             }
 *         }
 *         return true;
 *     }
 *
 *     public boolean onCancelClick() {
 *         setRedirect(WelcomePage.class);
 *         return false;
 *     }
 * } </pre>
 *
 * The forms corresponding template code is below. Note the form automatically
 * renders itself when Velocity invokes its {@link #toString()} method.
 *
 * <pre class="codeHtml">
 * <span class="blue">$form</span> </pre>
 *
 * If a Form has been posted and processed, if it has an {@link #error} defined or
 * any of its Fields have validation errors they will be automatically
 * rendered, and the {@link #isValid()} method will return false.
 *
 * <a name="data-binding"></a>
 * <h3>Data Binding</h3>
 *
 * To bind value objects to a forms fields use the copy methods:
 * <ul>
 * <li>value object &nbsp; -> &nbsp; form fields  &nbsp; &nbsp; &nbsp;
 * {@link #copyFrom(Object)}</li>
 * <li>form fields &nbsp; -> &nbsp; value object  &nbsp; &nbsp; &nbsp;
 * {@link #copyTo(Object)}</li>
 * </ul>
 * To debug the data binding being performed, use the Click application mode to
 * "<tt>debug</tt>" or use the debug copy methods.
 * <p/>
 * Binding of nested data objects is supported using the
 * <a target="blank" href="http://www.ognl.org">OGNL</a> library. To use
 * nested objects in your form, simply specify the object path as the Field
 * name. Note in the object path you exclude the root object, so the path
 * <tt>customer.address.state</tt> is specified as <tt>address.state</tt>.
 * <p/>
 * For example:
 *
 * <pre class="prettyprint">
 * // The customer.address.state field
 * TextField stateField = new TextField("address.state");
 * form.add(stateField);
 * ..
 *
 * // Loads the customer address state into the form stateField
 * Customer customer = getCustomer();
 * form.copyFrom(customer);
 * ..
 *
 * // Copies form stateField value into the customer address state
 * Customer customer = new Customer();
 * form.copyTo(customer); </pre>
 *
 * When populating an object from a form post Click will automatically create
 * any null nested objects so their properties can be set. To do this Click
 * uses the no-args constructor of the nested objects class.
 * <p/>
 * {@link #copyTo(Object)} and {@link #copyFrom(Object)} also supports
 * <tt>java.util.Map</tt> as an argument. Examples of using
 * <tt>java.util.Map</tt> are shown in the respective method descriptions.
 *
 * <a name="form-validation"></a>
 * <h3>Form Validation</h3>
 *
 * The Form control supports automatic field validation. By default when a POST
 * request is made the form will validate the field values. To disable
 * automatic validation set {@link #setValidate(boolean)} to false.
 * <p/>
 * Form also provides a {@link #validate()} method where subclasses can provide
 * custom cross-field validation.
 * <p/>
 * <b>File Upload Validation</b>
 * <p/>
 * The Form's {@link #validateFileUpload()} provides validation for multipart
 * requests (multipart requests are used for uploading files from the browser).
 * The {@link #validateFileUpload()} method checks that files being uploaded do not exceed the
 * {@link org.apache.click.service.CommonsFileUploadService#sizeMax maximum request size}
 * or the {@link org.apache.click.service.CommonsFileUploadService#fileSizeMax maximum file size}.
 * <p/>
 * <b>Note:</b> if the <tt>maximum request size</tt> or <tt>maximum file size</tt>
 * is exceeded, the request is deemed invalid ({@link #hasPostError hasPostError}
 * will return true), and no further processing is performed on the form or fields.
 * Instead the form will display the appropriate error message for the invalid request.
 * See {@link #validateFileUpload()} for details of the error message properties.
 * <p/>
 * <b>JavaScript Validation</b>
 * <p/>
 * The Form control also supports client side JavaScript validation. By default
 * JavaScript validation is not enabled. To enable JavaScript validation set
 * {@link #setJavaScriptValidation(boolean)} to true. For example:
 *
 * <pre class="prettyprint">
 * Form form = new Form("form");
 * form.setJavaScriptValidation(true);
 *
 * // Add form fields
 * ..
 *
 * form.add(new Submit("ok", " OK ", this, "onOkClicked");
 *
 * Submit cancel = new Submit("cancel", "Cancel", this, "onCancelClicked");
 * cancel.setCancelJavaScriptValidation(true);
 *
 * addControl(form); </pre>
 *
 * Please note in that is this example the cancel submit button has
 * {@link Submit#setCancelJavaScriptValidation(boolean)} set to true. This
 * prevents JavaScript form validation being performed when the cancel button is
 * clicked.
 *
 * <a name="resources"></a>
 * <h3>CSS and JavaScript resources</h3>
 *
 * The Form control makes use of the following resources (which Click automatically
 * deploys to the application directory, <tt>/click</tt>):
 *
 * <ul>
 * <li><tt>click/control.css</tt></li>
 * <li><tt>click/control.js</tt></li>
 * </ul>
 *
 * To import these files and any form control imports simply reference
 * the variables <span class="blue">$headElements</span> and
 * <span class="blue">$jsElements</span> in the page template. For example:
 *
 * <pre class="codeHtml">
 * &lt;html&gt;
 * &lt;head&gt;
 * <span class="blue">$headElements</span>
 * &lt;/head&gt;
 * &lt;body&gt;
 *
 * <span class="red">$form</span>
 *
 * <span class="blue">$jsElements</span>
 * &lt;/body&gt;
 * &lt;/html&gt; </pre>
 *
 * <a name="form-layout"></a>
 * <h3>Form Layout</h3>
 * The Form control supports rendering using automatic and manual layout
 * techniques.
 *
 * <a name="auto-layout"></a>
 * <h4>Auto Layout</h4>
 *
 * If you include a form variable in your template the form will be
 * automatically laid out and rendered. Auto layout, form and field rendering
 * options include:
 *
 * <table style="margin-left: 1em;" cellpadding="3">
 * <tr>
 * <td>{@link #buttonAlign}</td> <td>button alignment: &nbsp; <tt>["left", "center", "right"]</tt></td>
 * </tr><tr>
 * <td>{@link #buttonStyle}</td> <td>button &lt;td&gt; "style" attribute value</td>
 * </tr><tr>
 * <td>{@link #columns}</td> <td>number of form table columns, the default value number is 1</td>
 * </tr><tr>
 * <td>{@link #errorsAlign}</td> <td>validation error messages alignment: &nbsp; <tt>["left", "center", "right"]</tt></td>
 * </tr><tr>
 * <td>{@link #errorsPosition}</td> <td>validation error messages position: &nbsp; <tt>["top", "middle", "bottom"]</tt></td>
 * </tr><tr>
 * <td>{@link #errorsStyle}</td> <td>errors &lt;td&gt; "style" attribute value</td>
 * </tr><tr>
 * <td>{@link #fieldStyle}</td> <td>field &lt;td&gt; "style" attribute value</td>
 * </tr><tr>
 * <td>{@link #labelAlign}</td> <td>field label alignment: &nbsp; <tt>["left", "center", "right"]</tt></td>
 * </tr><tr>
 * <td>{@link #labelsPosition}</td> <td>label position relative to field: &nbsp; <tt>["left", "top"]</tt></td>
 * </tr><tr>
 * <td>{@link #labelStyle}</td> <td>label &lt;td&gt; "style" attribute value</td>
 * </tr><tr>
 * <td>click/control.css</td> <td>control CSS styles, automatically deployed to the <tt>click</tt> web directory</td>
 * </tr><tr>
 * <td>/click-control.properties</td> <td>form and field messages and HTML, located under classpath</td>
 * </tr>
 * </table>
 *
 * <a name="manual-layout"></a>
 * <h4>Manual Layout</h4>
 *
 * You can also manually layout the Form in the page template specifying
 * the fields using the named field notation:
 *
 * <pre class="codeHtml">
 * $form.{@link #getFields fields}.usernameField </pre>
 *
 * Whenever including your own Form markup in a page template or Velocity macro
 * always specify:
 * <ul style="margin-top: 0.5em;">
 *  <li><span class="maroon">method</span>
 *      - the form submission method <tt>["post" | "get"]</tt></li>
 *  <li><span class="maroon">name</span>
 *      - the name of your form, important when using JavaScript</li>
 *  <li><span class="maroon">action</span>
 *      - directs the Page where the form should be submitted to</li>
 *  <li><span class="maroon">form_name</span>
 *      - include a hidden field which specifies the {@link #name} of the Form </li>
 * </ul>
 * The hidden field is used by Click to determine which form was posted on
 * a page which may contain multiple forms.
 * <p/>
 * Alternatively you can use the Form {@link #startTag()} and {@link #endTag()}
 * methods to render this information.
 * <p/>
 * An example of a manually laid out Login form is provided below:
 *
 * <pre class="codeHtml">
 * <span class="blue">$form.startTag()</span>
 *
 *   &lt;table style="margin: 1em;"&gt;
 *
 *     <span class="red">#if</span> (<span class="blue">$form.error</span>)
 *     &lt;tr&gt;
 *       &lt;td colspan="2" style="color: red;"&gt; <span class="blue">$form.error</span> &lt;/td&gt;
 *     &lt;/tr&gt;
 *     <span class="red">#end</span>
 *     <span class="red">#if</span> (<span class="blue">$form.fields.usernameField.error</span>)
 *     &lt;tr&gt;
 *       &lt;td colspan="2" style="color: red;"&gt; <span class="blue">$form.fields.usernameField.error</span> &lt;/td&gt;
 *     &lt;/tr&gt;
 *     <span class="red">#end</span>
 *     <span class="red">#if</span> (<span class="blue">$form.fields.passwordField.error</span>)
 *     &lt;tr&gt;
 *       &lt;td colspan="2" style="color: red;"&gt; <span class="blue">$form.fields.passwordField.error</span> &lt;/td&gt;
 *     &lt;/tr&gt;
 *     <span class="red">#end</span>
 *
 *     &lt;tr&gt;
 *       &lt;td&gt; Username: &lt;/td&gt;
 *       &lt;td&gt; <span class="blue">$form.fields.usernameField</span> &lt;/td&gt;
 *     &lt;/tr&gt;
 *     &lt;tr&gt;
 *       &lt;td&gt; Password: &lt;/td&gt;
 *       &lt;td&gt; <span class="blue">$form.fields.passwordField</span> &lt;/td&gt;
 *     &lt;/tr&gt;
 *
 *     &lt;tr&gt;
 *       &lt;td&gt;
 *         <span class="blue">$form.fields.okSubmit</span>
 *         <span class="blue">$form.fields.cancelSubmit</span>
 *       &lt;/td&gt;
 *     &lt;/tr&gt;
 *
 *   &lt;/table&gt;
 *
 * <span class="blue">$form.endTag()</span> </pre>
 *
 * As you can see in this example most of the code and markup is generic and
 * could be reused. This is where Velocity Macros come in.
 *
 * <a name="velocity-macros"></a>
 * <h4>Velocity Macros</h4>
 *
 * Velocity Macros
 * (<a target="topic" href="../../../../../velocity/user-guide.html#Velocimacros">velocimacros</a>)
 * are a great way to encapsulate customized forms.
 * <p/>
 * To create a generic form layout you can use the Form {@link #getFieldList()} and
 * {@link #getButtonList()} properties within a Velocity macro. If you want to
 * access <em>all</em> Form Controls from within a Velocity template or macro use
 * {@link #getControls()}.
 * <p/>
 * The example below provides a generic <span class="green">writeForm()</span>
 * macro which you could use through out an application. This Velocity macro code
 * would be contained in a macro file, e.g. <tt>macro.vm</tt>.
 *
 * <pre class="codeHtml"> <span class="red">#*</span> Custom Form Macro Code <span class="red">*#</span>
 * <span class="red">#macro</span>( <span class="green">writeForm</span>[<span class="blue">$form</span>] )
 *
 * <span class="blue">$form.startTag()</span>
 *
 * &lt;table width="100%"&gt;
 *
 * <span class="red">#if</span> (<span class="blue">$form.error</span>)
 *   &lt;tr&gt;
 *     &lt;td colspan="2" style="color: red;"&gt; <span class="blue">$form.error</span> &lt;/td&gt;
 *   &lt;/tr&gt;
 * <span class="red">#end</span>
 *
 * <span class="red">#foreach</span> (<span class="blue">$field</span> <span class="red">in</span> <span class="blue">$form.fieldList</span>)
 *   <span class="red">#if</span> (!<span class="blue">$field.hidden</span>)
 *     <span class="red">#if</span> (!<span class="blue">$field.valid</span>)
 *     &lt;tr&gt;
 *       &lt;td colspan="2"&gt; <span class="blue">$field.error</span> &lt;/td&gt;
 *     &lt;/tr&gt;
 *     <span class="red">#end</span>
 *
 *   &lt;tr&gt;
 *     &lt;td&gt; <span class="blue">$field.label</span>: &lt;/td&gt;&lt;td&gt; <span class="blue">$field</span> &lt;/td&gt;
 *   &lt;/tr&gt;
 *   <span class="red">#end</span>
 * <span class="red">#end</span>
 *
 *  &lt;tr&gt;
 *    &lt;td colspan="2"&gt;
 *    <span class="red">#foreach</span> (<span class="blue">$button</span> <span class="red">in </span><span class="blue">$form.buttonList</span>)
 *      <span class="blue">$button</span> &amp;nbsp;
 *    <span class="red">#end</span>
 *    &lt;/td&gt;
 *  &lt;/tr&gt;
 *
 * &lt;/table&gt;
 *
 * <span class="blue">$form.endTag()</span>
 *
 * <span class="red">#end</span> </pre>
 *
 * You would then call this macro in your Page template passing it your
 * <span class="blue">form</span> object:
 *
 * <pre class="codeHtml"> <span class="red">#</span><span class="green">writeForm</span>(<span class="blue">$form</span>) </pre>
 *
 * At render time Velocity will execute the macro using the given form and render
 * the results to the response output stream.
 *
 * <h4>Configuring Macros</h4>
 *
 * To configure your application to use your macros you can:
 * <ul>
 *  <li>
 *   Put your macros if a file called <span class="st"><tt>macro.vm</tt></span>
 *   in your applications root directory.
 *  </li>
 *  <li>
 *   Put your macros in the auto deployed
 *   <span class="st"><tt>click/VM_global_macro.vm</tt></span> file.
 *  </li>
 *  <li>
 *   Create a custom named macro file and reference it in a
 *   <span class="st"><tt>WEB-INF/velocity.properties</tt></span>
 *   file under the property named
 *   <tt>velocimacro.library</tt>.
 *  </li>
 * </ul>
 *
 * <a name="post-redirect"></a>
 * <h3>Preventing Accidental Form Posts</h3>
 *
 * Users may accidentally make multiple form submissions by refreshing a page
 * or by pressing the back button.
 * <p/>
 * To prevent multiple form posts from page refreshes use the Post
 * Redirect pattern. With this pattern once the user has posted a form you
 * redirect to another page. If the user then presses the refresh button, they
 * will making a GET request on the current page. Please see the
 * <a target="blank" href="http://www.theserverside.com/articles/content/RedirectAfterPost/article.html">Redirect After Post</a>
 * article for more information on this topic.
 * <p/>
 * To prevent multiple form posts from use of the browser back button use one
 * of the Form {@link #onSubmitCheck(Page, String)} methods. For example:
 *
 * <pre class="prettyprint">
 * public class Purchase extends Page {
 *     ..
 *
 *     public boolean onSecurityCheck() {
 *         return form.onSubmitCheck(this, "/invalid-submit.html");
 *     }
 * } </pre>
 *
 * The form submit check methods store a special token in the users session
 * and in a hidden field in the form to ensure a form post isn't replayed.
 *
 * <a name="dynamic-forms"></a>
 * <h3>Dynamic Forms and <em>not</em> validating a request</h3>
 *
 * A common use case for web applications is to create Form fields dynamically
 * based upon user selection. For example if a checkbox is ticked another Field
 * is added to the Form. A simple way to achieve this is using JavaScript
 * to submit the Form when the Field is changed or clicked.
 * <p/>
 * When submitting a Form using JavaScript, it is often desirable to <em>not</em>
 * validate the fields since the user is still filling out the form.
 * To cater for this use case, Form provides the {@link #setValidate(boolean)}
 * to switch off form and field validation. For example:
 *
 * <pre class="prettyprint">
 * public void onInit() {
 *     checkbox.setAttribute("onclick", "form.submit()");
 *
 *     // Since onInit occurs before the onProcess event,
 *     // we have to explicitly bind the submit button in the onInit event if we
 *     // want to check if it was clicked.
 *     // If the submit button wasn't clicked it means the Form was submitted
 *     // using JavaScript and we don't want to validate yet
 *     ClickUtils.bind(submit);
 *
 *     // If submit was not clicked, don't validate
 *     if(form.isFormSubmission() && !submit.isClicked()) {
 *         form.setValidate(false);
 *     }
 * } </pre>
 *
 * <p>&nbsp;<p/>
 * See also the W3C HTML reference:
 * <a class="external" target="_blank" title="W3C HTML 4.01 Specification"
 *    href="http://www.w3.org/TR/html401/interact/forms.html#h-17.3">FORM</a>
 *
 * @see org.openidentityplatform.openam.click.control.Field
 * @see Submit
 */
public class Form extends AbstractContainer implements Stateful {

    // Constants --------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    /** The align left, form layout constant: &nbsp; <tt>"left"</tt>. */
    public static final String ALIGN_LEFT = "left";

    /** The align center, form layout constant: &nbsp; <tt>"center"</tt>. */
    public static final String ALIGN_CENTER = "center";

    /** The align right, form layout constant: &nbsp; <tt>"right"</tt>. */
    public static final String ALIGN_RIGHT = "right";

    /**
     * The position top, errors and labels form layout constant: &nbsp;
     * <tt>"top"</tt>.
     */
    public static final String POSITION_TOP = "top";

    /**
     * The position middle, errors in middle form layout constant: &nbsp;
     * <tt>"middle"</tt>.
     */
    public static final String POSITION_MIDDLE = "middle";

    /**
     * The position bottom, errors on bottom form layout constant: &nbsp;
     * <tt>"top"</tt>.
     */
    public static final String POSITION_BOTTOM = "bottom";

    /**
     * The position left, labels of left form layout constant: &nbsp;
     * <tt>"left"</tt>.
     */
    public static final String POSITION_LEFT = "left";

    /**
     * The form name parameter for multiple forms: &nbsp; <tt>"form_name"</tt>.
     */
    public static final String FORM_NAME = "form_name";

    /** The HTTP content type header for multipart forms. */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    /**
     * The submit check reserved request parameter prefix: &nbsp;
     * <tt>SUBMIT_CHECK_</tt>.
     */
    public static final String SUBMIT_CHECK = "SUBMIT_CHECK_";

    /** The Form set field focus JavaScript. */
    protected static final String FOCUS_JAVASCRIPT =
        "<script type=\"text/javascript\"><!--\n"
        + "var field = document.getElementById('$id');\n"
        + "if (field && field.focus && field.type != 'hidden' && field.disabled != true) { field.focus(); };\n"
        + "//--></script>\n";

    // Instance Variables -----------------------------------------------------

    /** The form action URL. */
    protected String actionURL;

    /** The form disabled value. */
    protected boolean disabled;

    /** The form "enctype" attribute. */
    protected String enctype;

    /** The form level error message. */
    protected String error;

    /** The ordered list of fields, excluding buttons. */
    protected final List<Field> fieldList = new ArrayList<>();

    /**
     * The form method <tt>["post, "get"]</tt>, default value: &nbsp;
     * <tt>post</tt>.
     */
    protected String method = "post";

    /** The form is readonly flag. */
    protected boolean readonly;

    /** The form validate fields when processing flag. */
    protected boolean validate = true;

    /** The button align, default value is "<tt>left</tt>". */
    protected String buttonAlign = ALIGN_LEFT;

    /** The ordered list of button values. */
    protected final List<Button> buttonList = new ArrayList<Button>(5);

    /** The button &lt;td&gt; "style" attribute value. */
    protected String buttonStyle;

    /**
     * The number of form layout table columns, default value: <tt>1</tt>.
     * <p/>
     * This property is used to layout the number of table columns the form
     * is rendered with using a flow layout style.
     */
    protected int columns = 1;

    /**
     * The default field size, default value: <tt>0</tt>.
     * <p/>
     * If the form default field size is greater than 0, when fields are added
     * to the form the field's size will be set to the default value.
     */
    protected int defaultFieldSize;

    /** The errors block align, default value is <tt>"left"</tt>. */
    protected String errorsAlign = ALIGN_LEFT;

    /**
     * The form errors position <tt>["top", "middle", "bottom"]</tt> default
     * value: &nbsp; <tt>"top"</tt>.
     */
    protected String errorsPosition = POSITION_TOP;

    /** The error &lt;td&gt; "style" attribute value. */
    protected String errorsStyle;

    /** The field &lt;td&gt; "style" attribute value. */
    protected String fieldStyle;

    /** The map of field width values. */
    protected Map<String, Integer> fieldWidths = new HashMap<String, Integer>();

    /** Flag indicating whether this form was submitted. */
    protected Boolean formSubmission;

    /**
     * The JavaScript client side form fields validation flag. By default
     * JavaScript validation is not enabled.
     */
    protected boolean javaScriptValidation;

    /** The label align, default value is <tt>"left"</tt>. */
    protected String labelAlign = ALIGN_LEFT;

    /**
     * The form labels position <tt>["left", "top"]</tt> default value: &nbsp;
     * <tt>"left"</tt>.
     */
    protected String labelsPosition = POSITION_LEFT;

    /** The label &lt;td&gt; "style" attribute value. */
    protected String labelStyle;

    /**
     * Track the index offset when adding Controls. This ensures HiddenFields
     * added by Form does not interfere with Controls added by users.
     */
    private int insertIndexOffset; // Ensures hiddenFields added by Form are always at the end of the controlList

    // Constructors -----------------------------------------------------------

    /**
     * Create a form with the given name.
     *
     * @param name the name of the form
     * @throws IllegalArgumentException if the form name is null
     */
    public Form(String name) {
        setName(name);
    }

    /**
     * Create a form with no name.
     * <p/>
     * <b>Please note</b> the control's name must be defined before it is valid.
     */
    public Form() {
    }

    // Container Impl ---------------------------------------------------------

    /**
     * Add the control to the form at the specified index, and return the
     * added instance.
     * <p/>
     * <b>Please note</b>: if the form contains a control with the same name as
     * the given control, that control will be
     * {@link #replace(Control, Control) replaced}
     * by the given control. If a control has no name defined it cannot be replaced.
     * <p/>
     * Controls can be retrieved from the Map {@link #getControlMap() controlMap}
     * where the key is the Control name and value is the Control instance.
     * <p/>
     * All controls are available on the {@link #getControls() controls} list
     * at the index they were inserted. If you are only interested in Fields,
     * note that Buttons are available on the {@link #getButtonList() buttonList}
     * while other fields are available on {@link #getFieldList() fieldList}.
     * <p/>
     * The specified index only applies to {@link #getControls() controls}, not
     * {@link #getButtonList() buttonList} or {@link #getFieldList() fieldList}.
     * <p/>
     * <b>Please note</b> if the specified control already has a parent assigned,
     * it will automatically be removed from that parent and inserted into the
     * form.
     *
     * @see org.openidentityplatform.openam.click.control.Container#insert(Control, int)
     *
     * @param control the control to add to the container
     * @param index the index at which the control is to be inserted
     * @return the control that was added to the container
     *
     * @throws IllegalArgumentException if the control is null or if the control
     * and container is the same instance or if the Field name is not defined
     *
     * @throws IndexOutOfBoundsException if index is out of range
     * <tt>(index &lt; 0 || index &gt; getControls().size())</tt>
     */
    @Override
    public Control insert(Control control, int index) {

        // Check if container already contains the control
        String controlName = control.getName();
        if (controlName != null) {
            Control currentControl = getControlMap().get(controlName);

            // If container already contains the control do a replace
            if (currentControl != null
                && !(control instanceof Label)) {

                // Current control and new control are referencing the same object
                // so we exit early
                if (currentControl == control) {
                    return control;
                }

                // If the two controls are different objects, replace the current
                // control with the given control
                return replace(currentControl, control);
            }
        }

        // Adjust index for hidden fields added by Form. CLK-447
        int realIndex = Math.min(index, getControls().size() - insertIndexOffset);

        ContainerUtils.insert(this, control, realIndex, getControlMap());

        if (control instanceof Field) {
            Field field = (Field) control;

            // Add field to either buttonList or fieldList for fast access
            if (field instanceof Button) {
                getButtonList().add((Button) field);

            } else {
                // Adjust index for hidden fields added by Form
                realIndex = Math.min(index, getFieldList().size() - insertIndexOffset);
                getFieldList().add(realIndex, field);
            }

            field.setForm(this);

            if (getDefaultFieldSize() > 0) {
                if (field instanceof TextField) {
                    ((TextField) field).setSize(getDefaultFieldSize());

                } else if (field instanceof FileField) {
                    ((FileField) field).setSize(getDefaultFieldSize());

                } else if (field instanceof TextArea) {
                    ((TextArea) field).setCols(getDefaultFieldSize());
                }
            }
        }

        return control;
    }

    /**
     * Add a Control to the form and return the added instance.
     * <p/>
     * <b>Please note</b>: if the form contains a control with the same name as
     * the given control, that control will be
     * {@link #replace(Control, Control) replaced}
     * by the given control. If a control has no name defined it cannot be replaced.
     * <p/>
     * Controls can be retrieved from the Map {@link #getControlMap() controlMap}
     * where the key is the Control name and value is the Control instance.
     * <p/>
     * All controls are available on the {@link #getControls() controls} list
     * in the order they were added. If you are only interested in Fields,
     * note that Buttons are available on the {@link #getButtonList() buttonList}
     * while other fields are available on {@link #getFieldList() fieldList}.
     *
     * @see org.openidentityplatform.openam.click.control.Container#add(Control)
     *
     * @param control the control to add to the container and return
     * @return the control that was added to the container
     * @throws IllegalArgumentException if the control is null, the Control name
     * is not defined or the container already contains a control with the same
     * name
     */
    @Override
    public Control add(Control control) {
        return super.add(control);
    }

    /**
     * Add the field to the form, and set the fields form property.
     * <p/>
     * <b>Please note</b>: if the form contains a control with the same name as
     * the given control, that control will be
     * {@link #replace(Control, Control) replaced}
     * by the given control. If a control has no name defined it cannot be replaced.
     * <p/>
     * Fields can be retrieved from the Map {@link #getFields() fields} where
     * the key is the Field name and value is the Field instance.
     * <p/>
     * Buttons are available on the {@link #getButtonList() buttonList} while
     * other fields are available on {@link #getFieldList() fieldList}.
     *
     * @see #add(Control)
     *
     * @param field the field to add to the form
     * @return the field added to this form
     * @throws IllegalArgumentException if the field is null, the field name
     * is not defined or the form already contains a control with the same name
     */
    public Field add(Field field) {
        add((Control) field);
        return field;
    }

    /**
     * Add the field to the form and specify the field's width in columns.
     * <p/>
     * <b>Please note</b>: if the form contains a control with the same name as
     * the given control, that control will be
     * {@link #replace(Control, Control) replaced}
     * by the given control. If a control has no name defined it cannot be replaced.
     * <p/>
     * Fields can be retrieved from the Map {@link #getFields() fields} where
     * the key is the Field name and value is the Field instance.
     * <p/>
     * Fields are available on {@link #getFieldList() fieldList}.
     * <p/>
     * Note Button and HiddenField types are not valid arguments for this method.
     *
     * @see #add(Control)
     *
     * @param field the field to add to the form
     * @param width the width of the field in table columns
     * @return the field added to this form
     * @throws IllegalArgumentException if the field is null, field name is
     * not defined, field is a Button or HiddenField, the form already contains
     * a field with the same name or the width &lt; 1
     */
    public Field add(Field field, int width) {
        add((Control) field, width);
        return field;
    }

    /**
     * Add the control to the form and specify the control's width in columns.
     * <p/>
     * <b>Please note</b>: if the form contains a control with the same name as
     * the given control, that control will be
     * {@link #replace(Control, Control) replaced}
     * by the given control. If a control has no name defined it cannot be replaced.
     * <p/>
     * Controls can be retrieved from the Map {@link #getControlMap() controlMap}
     * where the key is the Control name and value is the Control instance.
     * <p/>
     * Controls are available on the {@link #getControls() controls} list.
     * <p/>
     * Note Button and HiddenField types are not valid arguments for this method.
     *
     * @see #add(Control)
     *
     * @param control the control to add to the form
     * @param width the width of the control in table columns
     * @return the control added to this form
     * @throws IllegalArgumentException if the control is null, control is a
     * Button or HiddenField, the form already contains a control with the same
     * name or the width &lt; 1
     */
    public Control add(Control control, int width) {
        if (control instanceof Button || control instanceof HiddenField) {
            String msg = "Not a valid field type: " + control.getClass().getName();
            throw new IllegalArgumentException(msg);
        }
        if (width < 1) {
            throw new IllegalArgumentException("Invalid field width: " + width);
        }

        add(control);

        if (control.getName() != null) {
            getFieldWidths().put(control.getName(), width);
        }
        return control;
    }

    /**
     * Replace the control in the form at the specified index, and return
     * the newly added control.
     *
     * @see org.openidentityplatform.openam.click.control.Container#replace(Control, Control)
     *
     * @param currentControl the control currently contained in the form
     * @param newControl the control to replace the current control contained in
     * the form
     * @return the new control that replaced the current control
     *
     * @deprecated this method was used for stateful pages, which have been deprecated
     *
     * @throws IllegalArgumentException if the currentControl or newControl is
     * null
     * @throws IllegalStateException if the currentControl is not contained in
     * the form
     */
    @Override
    public Control replace(Control currentControl, Control newControl) {
        // Current and new control is the same instance - exit early
        if (currentControl == newControl) {
            return newControl;
        }

        int controlIndex = getControls().indexOf(currentControl);
        Control result = ContainerUtils.replace(this, currentControl, newControl,
            controlIndex, getControlMap());

        if (newControl instanceof Field) {
            Field field = (Field) newControl;

            if (field instanceof Button) {
                // Replace field in buttonList for fast access
                int buttonIndex = getButtonList().indexOf(currentControl);
                getButtonList().set(buttonIndex, (Button) field);

            } else {
                // Replace field in fieldList for fast access
                int fieldIndex = getFieldList().indexOf(currentControl);
                getFieldList().set(fieldIndex, field);
            }

            // Set parent form
            field.setForm(this);

            if (currentControl instanceof Field) {
                // Remove form reference from current control
                ((Field) currentControl).setForm(null);
            }

            if (getDefaultFieldSize() > 0) {
                if (field instanceof TextField) {
                    ((TextField) field).setSize(getDefaultFieldSize());

                } else if (field instanceof FileField) {
                    ((FileField) field).setSize(getDefaultFieldSize());

                } else if (field instanceof TextArea) {
                    ((TextArea) field).setCols(getDefaultFieldSize());
                }
            }
        }

        return result;
    }

    /**
     * @see org.openidentityplatform.openam.click.control.Container#remove(Control)
     *
     * @param control the control to remove from the container
     * @return true if the control was removed from the container
     * @throws IllegalArgumentException if the control is null
     */
    @Override
    public boolean remove(Control control) {

        boolean removed = super.remove(control);

        if (removed && control instanceof Field) {
            Field field = (Field) control;

            field.setForm(null);

            if (field instanceof Button) {
                getButtonList().remove(field);

            } else {
                getFieldList().remove(field);
            }
        }
        getFieldWidths().remove(control.getName());

        return removed;
    }

    /**
     * Remove the named field from the form, returning true if removed
     * or false if not found.
     *
     * @param name the name of the field to remove from the form
     * @return true if the named field was removed or false otherwise
     */
    public boolean removeField(String name) {
        Control control = getControl(name);

        if (control != null) {
            return remove(control);

        } else {
            return false;
        }
    }

    /**
     * Remove the list of named fields from the form.
     *
     * @param fieldNames the list of field names to remove from the form
     * @throws IllegalArgumentException if any of the fields is null
     */
    public void removeFields(List<String> fieldNames) {
        if (fieldNames != null) {
            for (int i = 0; i < fieldNames.size(); i++) {
                removeField(fieldNames.get(i).toString());
            }
        }
    }

    // Public Attributes ------------------------------------------------------

    /**
     * Return the form's html tag: <tt>form</tt>.
     *
     * @see org.apache.click.control.AbstractControl#getTag()
     *
     * @return this controls html tag
     */
    @Override
    public String getTag() {
        return "form";
    }

    /**
     * Return the form "action" attribute URL value. If the action URL attribute
     * has not been explicitly set the form action attribute will target the
     * page containing the form. This is the default behaviour for most scenarios.
     * However if you explicitly specify the form "action" URL attribute, this
     * value will be used instead.
     * <p/>
     * Setting the form action attribute is useful for situations where you want
     * a form to submit to a different page. This can also be used to have a
     * form submit to the J2EE Container for authentication, by setting the
     * action URL to "<tt>j_security_check</tt>".
     * <p/>
     * The action URL will always be encoded by the response to ensure it includes
     * the Session ID if required.
     *
     * @return the form "action" attribute URL value.
     */
    public String getActionURL() {
        Context context = getContext();
        HttpServletResponse response = context.getResponse();
        if (actionURL == null) {
            HttpServletRequest request = context.getRequest();
            return response.encodeURL(ClickUtils.getRequestURI(request));

        } else {
            return response.encodeURL(actionURL);
        }
    }

    /**
     * Return the form "action" attribute URL value. By setting this value you
     * will override the default action URL which points to the page containing
     * the form.
     * <p/>
     * Setting the form action attribute is useful for situations where you want
     * a form to submit to a different page. This can also be used to have a
     * form submit to the J2EE Container for authentication, by setting the
     * action URL to "<tt>j_security_check</tt>".
     *
     * @param value the form "action" attribute URL value
     */
    public void setActionURL(String value) {
        this.actionURL = value;
    }

    /**
     * @see AbstractControl#getControlSizeEst()
     *
     * @return the estimated rendered control size in characters
     */
    @Override
    public int getControlSizeEst() {
        return 400 + (getControls().size() * 350);
    }

    /**
     * Return true if the form is a disabled.
     *
     * @return true if the form is a disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Set the form disabled flag.
     *
     * @param disabled the form disabled flag
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Return the form "enctype" attribute value, or null if not defined.
     *
     * @return the form "enctype" attribute value, or null if not defined
     */
    public String getEnctype() {
        if (enctype == null) {
            for (Field field : ContainerUtils.getInputFields(this)) {
                if (!field.isHidden() && (field instanceof FileField)) {
                    enctype = MULTIPART_FORM_DATA;
                    break;
                }
            }
        }
        return enctype;
    }

    /**
     * Set the form "enctype" attribute value.
     *
     * @param enctype the form "enctype" attribute value, or null if not defined
     */
    public void setEnctype(String enctype) {
        this.enctype = enctype;
    }

    /**
     * Return the form level error message.
     *
     * @return the form level error message
     */
    public String getError() {
        return error;
    }

    /**
     * Set the form level validation error message. If the error message is not
     * null the form is invalid.
     *
     * @param error the validation error message
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Return a list of form fields which are not valid, not hidden and not
     * disabled.
     *
     * @return list of form fields which are not valid, not hidden and not
     *  disabled
     */
    public List<Field> getErrorFields() {
        return ContainerUtils.getErrorFields(this);
    }

    /**
     * Return the named field if contained in the form or null if not found.
     *
     * @param name the name of the field
     * @return the named field if contained in the form
     *
     * @throws IllegalStateException if a non-field control is found with the
     * specified name
     */
    public Field getField(String name) {
        Control control = ContainerUtils.findControlByName(this, name);

        if (control != null && !(control instanceof Field)) {
            throw new IllegalStateException("The control named " + name
                + " is an instance of the class " + control.getClass().getName()
                + ", which is not a " + Field.class.getName() + " subclass.");
        }
        return (Field) control;
    }

    /**
     * Return the field value for the named field, or null if the field is not
     * found.
     *
     * @param name the name of the field
     * @return the field value for the named field
     */
    public String getFieldValue(String name) {
        Field field = getField(name);

        if (field != null) {
            return field.getValue();

        } else {
            return null;
        }
    }

    /**
     * Return the Form HEAD elements to be included in the page.
     * The following resources are returned:
     *
     * <ul>
     * <li><tt>click/control.css</tt></li>
     * <li><tt>click/control.js</tt></li>
     * </ul>
     *
     * @see Control#getHeadElements()
     *
     * @return the form list of HEAD elements to be included in the page
     */
    @Override
    public List<Element> getHeadElements() {
        if (headElements == null) {
            headElements = super.getHeadElements();

            Context context = getContext();
            String versionIndicator = ClickUtils.getResourceVersionIndicator(context);

            headElements.add(new CssImport("/click/control.css", versionIndicator));
            headElements.add(new JsImport("/click/control.js", versionIndicator));
        }
        return headElements;
    }

    /**
     * Return the form method <tt>["post" | "get"]</tt>, default value is
     * <tt>post</tt>.
     *
     * @return the form method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set the form method <tt>["post" | "get"]</tt>.
     *
     * @param value the form method
     */
    public void setMethod(String value) {
        method = value;
    }

    /**
     * Return true if the page request is a submission from this form.
     * <p/>
     * A form submission requires the following criteria:
     * <ul>
     *   <li>the Form name must be present as a request parameter (Form
     *   automatically adds a HiddenField which value is set to the Form name.
     *   This ensures the Form name is present when submitting the form)</li>
     *   <li>the request method must equal the Form {@link #method}, for example
     *   both must be <tt>GET</tt> or <tt>POST</tt></li>
     * </ul>
     *
     * @return true if the page request is a submission from this form
     */
    public boolean isFormSubmission() {
        if (formSubmission == null) {
            Context context = getContext();
            String requestMethod = context.getRequest().getMethod();

            if (!getMethod().equalsIgnoreCase(requestMethod)) {
                return false;
            }

            formSubmission = getName().equals(context.getRequestParameter(FORM_NAME));
        }
        return formSubmission;
    }

    /**
     * Set the name of the form.
     *
     * @see Control#setName(String)
     *
     * @param name of the control
     * @throws IllegalArgumentException if the name is null
     */
    @Override
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }
        this.name = name;

        // TODO: Remove with stateful pages
        HiddenField nameField = (HiddenField) getField(FORM_NAME);
        if (nameField == null) {
            // Create a hidden field that won't be processed and name cannot change
            nameField = new NonProcessedHiddenField(FORM_NAME, String.class);
            add(nameField);
            insertIndexOffset++;
        }
        nameField.setValue(name);
    }

    /**
     * Return true if the form is a readonly.
     *
     * @return true if the form is a readonly
     */
    public boolean isReadonly() {
        return readonly;
    }

    /**
     * Set the form readonly flag.
     *
     * @param readonly the form readonly flag
     */
    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    /**
     * Return true if the fields are valid and there is no form level error,
     * otherwise return false.
     *
     * @return true if the fields are valid and there is no form level error
     */
    public boolean isValid() {
        if (getError() != null) {
            return false;
        }

        for (Field field : ContainerUtils.getInputFields(this)) {
            if (!field.isValid()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Return the ordered list of form fields, excluding buttons.
     * <p/>
     * The order of the fields is the same order they were added to the form.
     * <p/>
     * The returned list includes only fields added directly to the Form.
     *
     * @return the ordered List of form fields, excluding buttons
     */
    public List<Field> getFieldList() {
        return fieldList;
    }

    /**
     * Return the Map of form fields (including buttons), keyed
     * on field name.
     * <p/>
     * The returned map includes only fields added directly to the Form.
     *
     * @see #getControlMap()
     *
     * @return the Map of form fields (including buttons), keyed
     * on field name
     */
    public Map<String, Control> getFields() {
        return getControlMap();
    }

    /**
     * Return true if the Form fields should validate themselves when being
     * processed.
     *
     * @return true if the form fields should perform validation when being
     *  processed
     */
    public boolean getValidate() {
        return validate;
    }

    /**
     * Set the Form field validation flag, telling the Fields to validate
     * themselves when their <tt>onProcess()</tt> method is invoked.
     *
     * @param validate the Form field validation flag
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    /**
     * Return the buttons &lt;td&gt; HTML horizontal alignment: "<tt>left</tt>",
     * "<tt>center</tt>", "<tt>right</tt>".
     *
     * @return the field label HTML horizontal alignment
     */
    public String getButtonAlign() {
        return buttonAlign;
    }

    /**
     * Set the button &lt;td&gt; HTML horizontal alignment: "<tt>left</tt>",
     * "<tt>center</tt>", "<tt>right</tt>".
     * Note the given align is not validated.
     *
     * @param align the field label HTML horizontal alignment
     */
    public void setButtonAlign(String align) {
        buttonAlign = align;
    }

    /**
     * Return the ordered list of {@link Button}s.
     * <p/>
     * The order of the buttons is the same order they were added to the form.
     * <p/>
     * The returned list includes only buttons added directly to the Form.
     *
     * @return the ordered list of {@link Button}s.
     */
    public List<Button> getButtonList() {
        return buttonList;
    }

    /**
     * Return the button &lt;td&gt; "style" attribute value.
     *
     * @return the button &lt;td&gt; "style" attribute value
     */
    public String getButtonStyle() {
        return buttonStyle;
    }

    /**
     * Set the button &lt;td&gt; "style" attribute value.
     *
     * @param value the button &lt;td&gt; "style" attribute value
     */
    public void setButtonStyle(String value) {
        this.buttonStyle = value;
    }

    /**
     * Return the number of form layout table columns. This property is used to
     * layout the number of table columns the form is rendered with.
     *
     * @return the number of form layout table columns
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Set the number of form layout table columns. This property is used to
     * layout the number of table columns the form is rendered with.
     *
     * @param columns the number of form layout table columns
     */
    public void setColumns(int columns) {
        this.columns = columns;
    }

    /**
     * Return the form default field size. If the form default field size is
     * greater than 0, when fields are added to the form the field's size will
     * be set to the default value.
     *
     * @return the form default field size
     */
    public int getDefaultFieldSize() {
        return defaultFieldSize;
    }

    /**
     * Return the form default field size. If the form default field size is
     * greater than 0, when fields are added to the form the field's size will
     * be set to the default value.
     *
     * @param size the default field size
     */
    public void setDefaultFieldSize(int size) {
        this.defaultFieldSize = size;
    }

    /**
     * Return the errors block HTML horizontal alignment: "<tt>left</tt>",
     * "<tt>center</tt>", "<tt>right</tt>".
     *
     * @return the errors block HTML horizontal alignment
     */
    public String getErrorsAlign() {
        return errorsAlign;
    }

    /**
     * Set the errors block HTML horizontal alignment: "<tt>left</tt>",
     * "<tt>center</tt>", "<tt>right</tt>".
     * Note the given align is not validated.
     *
     * @param align the errors block HTML horizontal alignment
     */
    public void setErrorsAlign(String align) {
        errorsAlign = align;
    }

    /**
     * Return the form errors position <tt>["top", "middle", "bottom"]</tt>.
     *
     * @return form errors position
     */
    public String getErrorsPosition() {
        return errorsPosition;
    }

    /**
     * Set the form errors position <tt>["top", "middle", "bottom"]</tt>.
     *
     * @param position the form errors position
     */
    public void setErrorsPosition(String position) {
        if (POSITION_TOP.equals(position)
            || POSITION_MIDDLE.equals(position)
            || POSITION_BOTTOM.equals(position)) {

            errorsPosition = position;

        } else {
            throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    /**
     * Return the error &lt;td&gt; "style" attribute value.
     *
     * @return the error &lt;td&gt; "style" attribute value
     */
    public String getErrorsStyle() {
        return errorsStyle;
    }

    /**
     * Set the errors &lt;td&gt; "style" attribute value.
     *
     * @param value the errors &lt;td&gt; "style" attribute value
     */
    public void setErrorsStyle(String value) {
        this.errorsStyle = value;
    }

    /**
     * Return the field &lt;td&gt; "style" attribute value.
     *
     * @return the field &lt;td&gt; "style" attribute value
     */
    public String getFieldStyle() {
        return fieldStyle;
    }

    /**
     * Set the field &lt;td&gt; "style" attribute value. Fields can override
     * this value by providing a {@link Field#setParentStyleHint(String)}.
     *
     * @see Field#setParentStyleHint(String)
     * @see Field#setParentStyleClassHint(String)
     *
     * @param value the field &lt;td&gt; "style" attribute value
     */
    public void setFieldStyle(String value) {
        this.fieldStyle = value;
    }

    /**
     * Return the map of field width values, keyed on field name.
     *
     * @return the map of field width values, keyed on field name
     */
    public Map<String, Integer> getFieldWidths() {
        return fieldWidths;
    }

    /**
     * Return true if JavaScript client side form validation is enabled.
     *
     * @return true if JavaScript client side form validation is enabled
     */
    public boolean isJavaScriptValidation() {
        return javaScriptValidation;
    }

    /**
     * Return true if JavaScript client side form validation is enabled.
     *
     * @deprecated use {@link #isJavaScriptValidation()} instead
     *
     * @return true if JavaScript client side form validation is enabled
     */
    public boolean getJavaScriptValidation() {
        return javaScriptValidation;
    }

    /**
     * Set the JavaScript client side form validation flag.
     *
     * @param validate the JavaScript client side validation flag
     */
    public void setJavaScriptValidation(boolean validate) {
        javaScriptValidation = validate;
    }

    /**
     * Return the field label HTML horizontal alignment: "<tt>left</tt>",
     * "<tt>center</tt>", "<tt>right</tt>".
     *
     * @return the field label HTML horizontal alignment
     */
    public String getLabelAlign() {
        return labelAlign;
    }

    /**
     * Set the field label HTML horizontal alignment: "<tt>left</tt>",
     * "<tt>center</tt>", "<tt>right</tt>".
     * Note the given align is not validated.
     *
     * @param align the field label HTML horizontal alignment
     */
    public void setLabelAlign(String align) {
        labelAlign = align;
    }

    /**
     * Return the form labels position <tt>["left", "top"]</tt>.
     *
     * @return form labels position
     */
    public String getLabelsPosition() {
        return labelsPosition;
    }

    /**
     * Set the form labels position <tt>["left", "top"]</tt>.
     *
     * @param position the form labels position
     */
    public void setLabelsPosition(String position) {
        if (POSITION_LEFT.equals(position) || POSITION_TOP.equals(position)) {
            labelsPosition = position;
        } else {
            throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    /**
     * Return the label &lt;td&gt; "style" attribute value.
     *
     * @return the label &lt;td&gt; "style" attribute value
     */
    public String getLabelStyle() {
        return labelStyle;
    }

    /**
     * Set the label &lt;td&gt; "style" attribute value.
     * <p/>
     * This value can be overridden by Fields through their
     * {@link Field#setParentStyleHint(String)} property.
     *
     * @param value the label &lt;td&gt; "style" attribute value
     */
    public void setLabelStyle(String value) {
        this.labelStyle = value;
    }

    /**
     * The callback listener will only be called during processing if the field
     * value is valid. If the field has validation errors the listener will not
     * be called.
     *
     * @see Control#setListener(Object, String)
     *
     * @param listener the listener object with the named method to invoke
     * @param method the name of the method to invoke
     */
    @Override
    public void setListener(Object listener, String method) {
        super.setListener(listener, method);
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Clear any form or field errors by setting them to null.
     */
    public void clearErrors() {
        setError(null);

        for (Field field : ContainerUtils.getInputFields(this)) {
            field.setError(null);
        }
    }

    /**
     * Clear all the form field values setting them to null.
     */
    public void clearValues() {
        for (Field field : ContainerUtils.getInputFields(this)) {
            if (!field.getName().equals(FORM_NAME)
                && (!field.getName().startsWith(SUBMIT_CHECK))) {
                field.setValue(null);
            }
        }
    }

    /**
     * Copy the given object's attributes into the Form's field values. In
     * other words automatically populate Form's field values with the
     * given objects attributes.
     * <p/>
     * The following example populates the Form field with Customer
     * attributes:
     *
     * <pre class="prettyprint">
     * public void onGet() {
     *     Long customerId = ..
     *     Customer customer = CustomerDAO.findByPK(customerId);
     *     form.copyFrom(customer);
     * } </pre>
     *
     * copyForm also supports <tt>java.util.Map</tt> as an argument.
     * <p/>
     * By specifying a map, the Form's field values will be populated by
     * matching key/value pairs. A match occurs when the map's key is equal to
     * a field's name.
     * <p/>
     * The following example populates the Form fields with a map's
     * key/value pairs:
     *
     * <pre class="prettyprint">
     * public void onInit() {
     *     form = new Form("form");
     *     form.add(new TextField("name"));
     *     form.add(new TextField("address.street"));
     * }
     *
     * public void onGet() {
     *     Map map = new HashMap();
     *     map.put("name", "Steve");
     *     map.put("address.street", "12 Long street");
     *     form.copyFrom(map);
     * } </pre>
     *
     * For more information on how Fields and Objects are copied see
     * {@link ContainerUtils#copyObjectToContainer(Object,org.openidentityplatform.openam.click.control.Container)}.
     *
     * @param object the object to obtain attribute values from
     * @throws IllegalArgumentException if the object parameter is null
     */
    public void copyFrom(Object object) {
        ContainerUtils.copyObjectToContainer(object, this);
    }

    /**
     * Copy the given object's attributes into the Form's field values. In other
     * words automatically populate Forms field values with the given objects
     * attributes. copyFrom also supports <tt>java.util.Map</tt> as an argument.
     * <p/>
     * If the debug parameter is true, debugging messages will be
     * logged.
     *
     * @see #copyFrom(Object)
     *
     * @param object the object to obtain attribute values from
     * @param debug log debug statements when populating the form
     * @throws IllegalArgumentException if the object parameter is null
     */
    public void copyFrom(Object object, boolean debug) {
        ContainerUtils.copyObjectToContainer(object, this);
    }

    /**
     * Copy the Form's field values into the given object's attributes. In
     * other words automatically populate Object attributes with the Form's
     * field values.
     * <p/>
     * The following example populates the Customer attributes with the
     * Form's field values:
     *
     * <pre class="prettyprint">
     * public void onPost() {
     *     if (form.isValid()) {
     *         Customer customer = new Customer();
     *         form.copyTo(customer);
     *         ..
     *     }
     *     return true;
     * } </pre>
     *
     * copyTo also supports <tt>java.util.Map</tt> as an argument.
     * <p/>
     * By specifying a map, the map's key/value pairs are populated from
     * matching Form field names. A match occurs when a field's name is
     * equal to a map's key.
     * <p/>
     * The following example populates the map with the Form field values:
     *
     * <pre class="prettyprint">
     * public void onInit() {
     *     form = new Form("form");
     *     form.add(new TextField("name"));
     *     form.add(new TextField("address.street"));
     * }
     *
     * public void onGet() {
     *     Map map = new HashMap();
     *     map.put("name", null);
     *     map.put("address.street", null);
     *     form.copyTo(map);
     * } </pre>
     *
     * Note that the map acts as a template to specify which fields to populate
     * from.
     *
     * For more information on how Fields and Objects are copied see
     * {@link ContainerUtils#copyContainerToObject(org.openidentityplatform.openam.click.control.Container, Object)}.
     *
     * @param object the object to populate with field values
     * @throws IllegalArgumentException if the object parameter is null
     */
    public void copyTo(Object object) {
        ContainerUtils.copyContainerToObject(this, object);
    }

    /**
     * Copy the Form's field values into the given object's attributes. In other
     * words automatically populate Object attributes with the Forms field
     * values. copyTo also supports <tt>java.util.Map</tt> as an argument.
     * <p/>
     * If the debug parameter is true, debugging messages will be
     * logged.
     *
     * @see #copyTo(Object)
     *
     * @param object the object to populate with field values
     * @param debug log debug statements when populating the object
     * @throws IllegalArgumentException if the object parameter is null
     */
    public void copyTo(Object object, boolean debug) {
        ContainerUtils.copyContainerToObject(this, object);
    }

    /**
     * Return the form state. The following state is returned:
     *
     * <ul>
     * <li>all the input Field values and FieldSets contained in the Form and
     * child containers.</li>
     * </ul>
     *
     * @return the state of input Fields and FieldSets contained in the form
     */
    public Object getState() {
        List<Field> fields = new ArrayList<>();
        addStatefulFields(this, fields);
        Map<String, Object> stateMap = new HashMap<String, Object>();
        for (Field field : fields) {
            Object state = field.getState();
            if (state != null) {
                stateMap.put(field.getName(), state);
            }
        }

        if (stateMap.isEmpty()) {
            return null;
        }
        return stateMap;
    }

    /**
     * Set the Form state. The state will be applied to all the input Fields
     * and FieldSets contained in the Form or child containers.
     *
     * @param state the Form state to set
     */
    public void setState(Object state) {
        if (state == null) {
            return;
        }

        Map stateMap = (Map) state;
        List<Field> fields = new ArrayList<Field>();
        addStatefulFields(this, fields);

        for (Field field : fields) {
            String fieldName = field.getName();
            if (stateMap.containsKey(fieldName)) {
                Object fieldState = stateMap.get(fieldName);
                field.setState(fieldState);
            }
        }
    }

    /**
     * Process the Form and its child controls only if the Form was submitted
     * by the user.
     * <p/>
     * This method invokes {@link #isFormSubmission()} to check whether the form
     * was submitted or not.
     * <p/>
     * The Forms processing order is:
     * <ol>
     * <li>All {@link Field} controls in the order they were added</li>
     * <li>All {@link Button} controls in the order they were added</li>
     * <li>Invoke the Forms listener if defined</li>
     * </ol>
     *
     * This method delegates validation to {@link #validate()} while
     * file upload validation are delegated to {@link #validateFileUpload()}.
     *
     * @see Context#getRequestParameter(String)
     * @see Context#getFileItemMap()
     *
     * @return true to continue Page event processing or false otherwise
     */
    @Override
    public boolean onProcess() {

        validateFileUpload();

        // If a POST error occurred exit early.
        if (hasPostError()) {
            // Remove exception to ensure other forms on Page do not
            // validate twice for same error.
            getContext().getRequest().removeAttribute(
                FileUploadService.UPLOAD_EXCEPTION);

            return true;
        }

        boolean continueProcessing = true;
        if (isFormSubmission()) {

            for (int i = 0, size = getControls().size(); i < size; i++) {
                Control control = getControls().get(i);
                String controlName = control.getName();
                if (controlName == null || !controlName.startsWith(Form.SUBMIT_CHECK)) {

                    if (!control.onProcess()) {
                        continueProcessing = false;
                    }
                }
            }

            if (getValidate()) {
                validate();
            }

            dispatchActionEvent();
        }

        return continueProcessing;
    }

    /**
     * Destroy the controls contained in the Form and clear any form
     * error message.
     *
     * @see Control#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        formSubmission = null;
        setError(null);
    }

    /**
     * The validate method is invoked by {@link #onProcess()} to validate
     * the request submission. A Form subclass can override this method
     * to implement cross-field validation logic.
     * <p/>
     * If the Form determines that the submission is invalid it should set the
     * {@link #error} property with an appropriate error message. For example:
     *
     * <pre class="prettyprint">
     * public class RegistrationForm extends Form {
     *
     *     // Add validation to ensure the password and confirmPassword fields match
     *     public void validate() {
     *         String password = getFieldValue("password");
     *         String confirmPassword = getFieldValue("confirmPassword");
     *         if (!password.equals(confirmPassword)) {
     *
     *             // Set Form's error property value that will be shown to the user
     *             setError("The passwords do not match.");
     *         }
     *     }
     * } </pre>
     */
    public void validate() {
    }

    /**
     * Perform a form submission check ensuring the user has not replayed the
     * form submission by using the browser's back or refresh buttons or by
     * clicking the Form submit button twice, in quick succession. If the form
     * submit is valid this method will return true, otherwise set the page to
     * redirect to the given redirectPath and return false.
     * <p/>
     * This method will add a token to the user's session and a hidden field
     * to the form to validate future submits.
     * <p/>
     * Form submit checks should be performed before the pages controls are
     * processed in the Page onSecurityCheck method. For example:
     *
     * <pre class="prettyprint">
     * public class Order extends Page {
     *     ..
     *
     *     public boolean onSecurityCheck() {
     *         return form.onSubmitCheck(this, "/invalid-submit.html");
     *     }
     * } </pre>
     *
     * Form submit checks should generally be combined with the Post-Redirect
     * pattern which provides a better user experience when pages are refreshed.
     * <p/>
     * <b>Please note:</b> a call to onSubmitCheck always succeeds for Ajax
     * requests.
     *
     * @param page the page invoking the Form submit check
     * @param redirectPath the path to redirect invalid submissions to
     * @return true if the form submit is OK or false otherwise
     * @throws IllegalArgumentException if the page or redirectPath is null
     */
    public boolean onSubmitCheck(Page page, String redirectPath) {
        if (page == null) {
            throw new IllegalArgumentException("Null page parameter");
        }
        if (redirectPath == null) {
            throw new IllegalArgumentException("Null redirectPath parameter");
        }

        if (performSubmitCheck()) {
            return true;

        } else {
            page.setRedirect(redirectPath);

            return false;
        }
    }

    /**
     * Perform a form submission check ensuring the user has not replayed the
     * form submission by using the browser back button. If the form submit
     * is valid this method will return true, otherwise set the page to
     * redirect to the given Page class and return false.
     * <p/>
     * This method will add a token to the user's session and a hidden field
     * to the form to validate future submits.
     * <p/>
     * Form submit checks should be performed before the pages controls are
     * processed in the Page onSecurityCheck method. For example:
     *
     * <pre class="prettyprint">
     * public class Order extends Page {
     *     ..
     *
     *     public boolean onSecurityCheck() {
     *         return form.onSubmitCheck(this, InvalidSubmitPage.class);
     *     }
     * } </pre>
     *
     * Form submit checks should generally be combined with the Post-Redirect
     * pattern which provides a better user experience when pages are refreshed.
     * <p/>
     * <b>Please note:</b> a call to onSubmitCheck always succeeds for Ajax
     * requests.
     *
     * @param page the page invoking the Form submit check
     * @param pageClass the page class to redirect invalid submissions to
     * @return true if the form submit is OK or false otherwise
     * @throws IllegalArgumentException if the page or pageClass is null
     */
    public boolean onSubmitCheck(Page page, Class<? extends Page> pageClass) {
        if (page == null) {
            throw new IllegalArgumentException("Null page parameter");
        }
        if (pageClass == null) {
            throw new IllegalArgumentException("Null pageClass parameter");
        }

        if (performSubmitCheck()) {
            return true;

        } else {
            page.setRedirect(pageClass);

            return false;
        }
    }

    /**
     * Perform a form submission check ensuring the user has not replayed the
     * form submission by using the browser back button. If the form submit
     * is valid this method will return true, otherwise the given listener
     * object and method will be invoked.
     * <p/>
     * This method will add a token to the users session and a hidden field
     * to the form to validate future submit's.
     * <p/>
     * Form submit checks should be performed before the pages controls are
     * processed in the Page onSecurityCheck method. For example:
     *
     * <pre class="prettyprint">
     * public class Order extends Page {
     *     ..
     *
     *     public boolean onSecurityCheck() {
     *         return form.onSubmitCheck(his, this, "onInvalidSubmit");
     *     }
     *
     *     public boolean onInvalidSubmit() {
     *        getContext().setRequestAttribute("invalidPath", getPath());
     *        setForward("invalid-submit.htm");
     *        return false;
     *     }
     * } </pre>
     *
     * Form submit checks should generally be combined with the Post-Redirect
     * pattern which provides a better user experience when pages are refreshed.
     * <p/>
     * <b>Please note:</b> a call to onSubmitCheck always succeeds for Ajax
     * requests.
     *
     * @param page the page invoking the Form submit check
     * @param submitListener the listener object to call when an invalid submit
     *      occurs
     * @param submitListenerMethod the listener method to invoke when an
     *      invalid submit occurs
     * @return true if the form submit is valid, or the return value of the
     *      listener method otherwise
     * @throws IllegalArgumentException if the page, submitListener or
     *      submitListenerMethod is null
     */
    public boolean onSubmitCheck(Page page, Object submitListener,
            String submitListenerMethod) {

        if (page == null) {
            throw new IllegalArgumentException("Null page parameter");
        }
        if (submitListener == null) {
            throw new IllegalArgumentException("Null submitListener parameter");
        }
        if (submitListenerMethod == null) {
            String msg = "Null submitListenerMethod parameter";
            throw new IllegalArgumentException(msg);
        }

        if (performSubmitCheck()) {
            return true;

        } else {
            return ClickUtils.invokeListener(submitListener, submitListenerMethod);
        }
    }

    /**
     * Remove the Form state from the session for the given request context.
     *
     * @see #saveState(Context)
     * @see #restoreState(Context)
     *
     * @param context the request context
     */
    public void removeState(Context context) {
        ClickUtils.removeState(this, getName(), context);
    }

    /**
     * Restore the Form state from the session for the given request context.
     * <p/>
     * This method delegates to {@link #setState(Object)} to set the
     * form restored state.
     *
     * @see #saveState(Context)
     * @see #removeState(Context)
     *
     * @param context the request context
     */
    public void restoreState(Context context) {
        ClickUtils.restoreState(this, getName(), context);
    }

    /**
     * Save the Form state to the session for the given request context.
     * <p/>
     * * This method delegates to {@link #getState()} to retrieve the form state
     * to save.
     *
     * @see #restoreState(Context)
     * @see #removeState(Context)
     *
     * @param context the request context
     */
    public void saveState(Context context) {
        ClickUtils.saveState(this, getName(), context);
    }

    /**
     * Return the rendered opening form tag and all the forms hidden fields.
     *
     * @return the rendered form start tag and the forms hidden fields
     */
    public String startTag() {
        List<Field> formFields = ContainerUtils.getInputFields(this);

        int bufferSize = getFormSizeEst(formFields);

        HtmlStringBuffer buffer = new HtmlStringBuffer(bufferSize);

        renderHeader(buffer, formFields);

        return buffer.toString();
    }

    /**
     * Return the rendered form end tag and JavaScript for field focus
     * and validation.
     *
     * @return the rendered form end tag
     */
    public String endTag() {
        HtmlStringBuffer buffer = new HtmlStringBuffer();

        List<Field> formFields = ContainerUtils.getInputFields(this);

        renderTagEnd(formFields, buffer);

        return buffer.toString();
    }

    /**
     * Render the HTML representation of the Form.
     * <p/>
     * If the form contains errors after processing, these errors will be
     * rendered.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {
        final boolean process =
            getContext().getRequest().getMethod().equalsIgnoreCase(getMethod());

        List<Field> formFields = ContainerUtils.getInputFields(this);

        renderHeader(buffer, formFields);

        buffer.append("<table class=\"form\" id=\"");
        buffer.append(getId());
        buffer.append("-form\"><tbody>\n");

        // Render fields, errors and buttons
        if (POSITION_TOP.equals(getErrorsPosition())) {
            renderErrors(buffer, process);
            renderFields(buffer);
            renderButtons(buffer);

        } else if (POSITION_MIDDLE.equals(getErrorsPosition())) {
            renderFields(buffer);
            renderErrors(buffer, process);
            renderButtons(buffer);

        } else if (POSITION_BOTTOM.equals(getErrorsPosition())) {
            renderFields(buffer);
            renderButtons(buffer);
            renderErrors(buffer, process);

        } else {
            String msg = "Invalid errorsPosition:" + getErrorsPosition();
            throw new IllegalArgumentException(msg);
        }

        buffer.append("</tbody></table>\n");

        renderTagEnd(formFields, buffer);
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Perform a back button submit check, returning true if the request is
     * valid or false otherwise. This method will add a submit check token
     * to the form as a hidden field, and to the session.
     *
     * @return true if the submit is OK or false otherwise
     */
    protected boolean performSubmitCheck() {

        if (StringUtils.isBlank(getName())) {
            throw new IllegalStateException("Form name is not defined.");
        }

        // CLK-333. Don't regenerate submit tokens for Ajax requests.
        Context context = getContext();
        if (context.isAjaxRequest()) {
            return true;
        }

        String resourcePath = context.getResourcePath();
        int slashIndex = resourcePath.indexOf('/');
        if (slashIndex != -1) {
            resourcePath = resourcePath.replace('/', '_');
        }

        // Ensure resourcePath starts with a '_' separator. If slashIndex == -1
        // or slashIndex > 0, resourcePath does not start with slash.
        if (slashIndex != 0) {
            resourcePath = '_' + resourcePath;
        }

        final HttpServletRequest request = context.getRequest();
        final String submitTokenName =
            SUBMIT_CHECK + getName() + resourcePath;

        boolean isValidSubmit = true;

        // If not this form exit
        String formName = context.getRequestParameter(FORM_NAME);

        // Only test if submit for this form
        if (!context.isForward()
            && request.getMethod().equalsIgnoreCase(getMethod())
            && getName().equals(formName)) {

            Long sessionTime =
                (Long) context.getSessionAttribute(submitTokenName);

            if (sessionTime != null) {
                String value = context.getRequestParameter(submitTokenName);
                if (value == null || value.length() == 0) {
                    // CLK-289. If a session attribute exists for the
                    // SUBMIT_CHECK, but no request parameter, we assume the
                    // submission is a duplicate and therefore invalid.
                    LogService logService = ClickUtils.getLogService();
                    logService.warn("    'Redirect After Post' token called '"
                        + submitTokenName + "' is registered in the session, "
                        + "but no matching request parameter was found. "
                        + "(form name: '" + getName()
                        + "'). To protect against a 'duplicate post', "
                        + "Form.onSubmitCheck() will return false.");
                    isValidSubmit = false;
                } else {
                    Long formTime = Long.valueOf(value);
                    isValidSubmit = formTime.equals(sessionTime);
                }
            }
        }

        // CLK-267: check against adding a duplicate field
        HiddenField field = (HiddenField) getField(submitTokenName);
        if (field == null) {
            field = new NonProcessedHiddenField(submitTokenName, Long.class);
            add(field);
            insertIndexOffset++;
        }

        // Save state info to form and session
        final Long time = System.currentTimeMillis();
        field.setValueObject(time);

        context.setSessionAttribute(submitTokenName, time);

        if (isValidSubmit) {
            return true;

        } else {
            return false;
        }
    }

    /**
     * Return the estimated rendered form size in characters.
     *
     * @param formFields the list of form fields
     * @return the estimated rendered form size in characters
     */
    protected int getFormSizeEst(List<Field> formFields) {
        return 500 + (formFields.size() * 350);
    }

    /**
     * Render the given form start tag and the form hidden fields to the given
     * buffer.
     *
     * @param buffer the HTML string buffer to render to
     * @param formFields the list of form fields
     */
    protected void renderHeader(HtmlStringBuffer buffer, List<Field> formFields) {

        buffer.elementStart(getTag());

        buffer.appendAttribute("method", getMethod());
        buffer.appendAttribute("name", getName());
        buffer.appendAttribute("id", getId());
        buffer.appendAttribute("action", getActionURL());
        buffer.appendAttribute("enctype", getEnctype());

        appendAttributes(buffer);

        if (isJavaScriptValidation()) {
            String javaScript = "return on_" + getId() + "_submit();";
            buffer.appendAttribute("onsubmit", javaScript);
        }
        buffer.closeTag();
        buffer.append("\n");

        // Render hidden fields
        for (Field field : ContainerUtils.getHiddenFields(this)) {
            field.render(buffer);
            buffer.append("\n");
        }
    }

    /**
     * Render the non hidden Form Fields to the string buffer.
     * <p/>
     * This method delegates the rendering of the form fields to
     * {@link #renderControls(HtmlStringBuffer, Container, List, Map, int)}.
     *
     * @param buffer the StringBuffer to render to
     */
    protected void renderFields(HtmlStringBuffer buffer) {

        // If Form contains only the FORM_NAME HiddenField, exit early
        if (getControls().size() == 1) {

            // getControlMap is cheaper than getFieldMap, so check that first
            if (getControlMap().containsKey(FORM_NAME)) {
                return;

            } else {
                Map<String, Field> fieldMap = ContainerUtils.getFieldMap(this);
                if (fieldMap.containsKey(FORM_NAME)) {
                    return;
                }
            }
        }

        buffer.append("<tr><td>\n");

        renderControls(buffer, this, getControls(), getFieldWidths(), getColumns());
        buffer.append("</td></tr>\n");
    }

    /**
     * Render the specified controls of the container to the string buffer.
     * <p/>
     * fieldWidths is a map specifying the width for specific fields contained
     * in the list of controls. The fieldWidths map is keyed on field name.
     *
     * @param buffer the StringBuffer to render to
     * @param container the container which controls to render
     * @param controls the controls to render
     * @param fieldWidths a map of field widths keyed on field name
     * @param columns the number of form layout table columns
     */
    protected void renderControls(HtmlStringBuffer buffer, Container container,
            List<Control> controls, Map<String, Integer> fieldWidths, int columns) {

        buffer.append("<table class=\"fields\"");
        String containerId = container.getId();
        if (containerId != null) {
            buffer.appendAttribute("id", containerId + "-fields");
        }
        buffer.append("><tbody>\n");

        int column = 1;
        boolean openTableRow = false;

        for (Control control : controls) {

            // Buttons are rendered separately
            if (control instanceof Button) {
                continue;
            }

            if (!isHidden(control)) {

                // Control width
                Integer width = fieldWidths.get(control.getName());

                if (column == 1) {
                    buffer.append("<tr class=\"fields\">\n");
                    openTableRow = true;
                }

                if (control instanceof FieldSet) {
                    FieldSet fieldSet = (FieldSet) control;
                    buffer.append("<td class=\"fields");
                    String cellStyleClass = fieldSet.getParentStyleClassHint();
                    if (cellStyleClass != null) {
                        buffer.append(" ");
                        buffer.append(cellStyleClass);
                    }
                    buffer.append("\"");

                    buffer.appendAttribute("style", fieldSet.getParentStyleHint());

                    if (width != null) {
                        int colspan = (width.intValue() * 2);
                        buffer.appendAttribute("colspan", colspan);
                    } else {
                        buffer.appendAttribute("colspan", 2);
                    }

                    buffer.append(">\n");
                    control.render(buffer);
                    buffer.append("</td>\n");

                } else if (control instanceof Label) {
                    Label label = (Label) control;
                    buffer.append("<td align=\"");
                    buffer.append(getLabelAlign());
                    buffer.append("\" class=\"fields");

                    String cellStyleClass = label.getParentStyleClassHint();
                    if (cellStyleClass != null) {
                        buffer.append(" ");
                        buffer.append(cellStyleClass);
                    }
                    buffer.append("\"");

                    buffer.appendAttribute("style", label.getParentStyleHint());

                    if (width != null) {
                        int colspan = (width.intValue() * 2);
                        buffer.appendAttribute("colspan", colspan);
                    } else {
                        buffer.appendAttribute("colspan", 2);
                    }

                    if (label.hasAttributes()) {
                        Map<String, String> labelAttributes = label.getAttributes();
                        for (Map.Entry<String, String> entry : labelAttributes.entrySet()) {
                            String labelAttrName = entry.getKey();
                            if (!labelAttrName.equals("id") && !labelAttrName.equals("style")) {
                                buffer.appendAttributeEscaped(labelAttrName, entry.getValue());
                            }
                        }
                    }
                    buffer.append(">");
                    label.render(buffer);
                    buffer.append("</td>\n");

                } else if (control instanceof Field) {
                    Field field = (Field) control;
                    // Write out label
                    if (POSITION_LEFT.equals(getLabelsPosition())) {
                        buffer.append("<td class=\"fields");
                        String cellStyleClass = field.getParentStyleClassHint();
                        if (cellStyleClass != null) {
                            buffer.append(" ");
                            buffer.append(cellStyleClass);
                        }
                        buffer.append("\"");
                        buffer.appendAttribute("align", getLabelAlign());
                        String cellStyle = field.getParentStyleHint();
                        if (cellStyle == null) {
                            cellStyle = getLabelStyle();
                        }
                        buffer.appendAttribute("style", cellStyle);
                        buffer.append(">");
                    } else {
                        buffer.append("<td valign=\"top\" class=\"fields");
                        String cellStyleClass = field.getParentStyleClassHint();
                        if (cellStyleClass != null) {
                            buffer.append(" ");
                            buffer.append(cellStyleClass);
                        }
                        buffer.append("\"");
                        String cellStyle = field.getParentStyleHint();
                        if (cellStyle == null) {
                            cellStyle = getLabelStyle();
                        }
                        buffer.appendAttribute("style", cellStyle);
                        buffer.append(">");
                    }

                    // Store the field id and label (the values could be null)
                    String fieldId = field.getId();
                    String fieldLabel = field.getLabel();

                    // Only render a label if the fieldId and fieldLabel is set
                    if (fieldId != null && fieldLabel != null) {
                        if (field.isRequired()) {
                            buffer.append(getMessage("label-required-prefix"));
                        } else {
                            buffer.append(getMessage("label-not-required-prefix"));
                        }
                        buffer.elementStart("label");
                        buffer.appendAttribute("for", fieldId);
                        buffer.appendAttribute("style", field.getLabelStyle());
                        if (field.isDisabled()) {
                            buffer.appendAttributeDisabled();
                        }
                        String cellClass = field.getLabelStyleClass();
                        if (field.getError() == null) {
                            buffer.appendAttribute("class", cellClass);
                        } else {
                            buffer.append(" class=\"error");
                            if (cellClass != null) {
                                buffer.append(" ");
                                buffer.append(cellClass);
                            }
                            buffer.append("\"");
                        }
                        buffer.closeTag();
                        buffer.append(fieldLabel);
                        buffer.elementEnd("label");
                        if (field.isRequired()) {
                            buffer.append(getMessage("label-required-suffix"));
                        } else {
                            buffer.append(getMessage("label-not-required-suffix"));
                        }
                    }

                    if (POSITION_LEFT.equals(getLabelsPosition())) {
                        buffer.append("</td>\n");
                        buffer.append("<td");
                        buffer.appendAttribute("class", field.getParentStyleClassHint());
                        buffer.appendAttribute("align", "left");

                        String cellStyle = field.getParentStyleHint();
                        if (cellStyle == null) {
                            cellStyle = getFieldStyle();
                        }
                        buffer.appendAttribute("style", cellStyle);

                        if (width != null) {
                            int colspan = (width.intValue() * 2) - 1;
                            buffer.appendAttribute("colspan", colspan);
                        }

                        buffer.append(">");
                    } else {
                        buffer.append("<br/>");
                    }

                    // Write out field
                    field.render(buffer);
                    buffer.append("</td>\n");

                } else {
                    buffer.append("<td class=\"fields\"");

                    if (width != null) {
                        int colspan = (width.intValue() * 2);
                        buffer.appendAttribute("colspan", colspan);
                    } else {
                        buffer.appendAttribute("colspan", 2);
                    }
                    buffer.append(">\n");

                    control.render(buffer);

                    buffer.append("</td>\n");
                }

                if (width != null) {
                    if (control instanceof Label || !(control instanceof Field)) {
                        column += width.intValue();

                    } else {
                        column += (width.intValue() - 1);
                    }
                }

                if (column >= columns) {
                    buffer.append("</tr>\n");
                    openTableRow = false;
                    column = 1;

                } else {
                    column++;
                }
            }
        }

        if (openTableRow) {
            buffer.append("</tr>\n");
        }

        buffer.append("</tbody></table>\n");
    }

    /**
     * Render the form errors to the given buffer is form processed.
     *
     * @param buffer the string buffer to render the errors to
     * @param processed the flag indicating whether has been processed
     */
    protected void renderErrors(HtmlStringBuffer buffer, boolean processed) {

        if (processed && !isValid()) {

            buffer.append("<tr><td align=\"");
            buffer.append(getErrorsAlign());
            buffer.append("\">\n");
            buffer.append("<table class=\"errors\" id=\"");
            buffer.append(getId());
            buffer.append("-errors\"><tbody>\n");

            if (getError() != null) {
                buffer.append("<tr class=\"errors\">");
                buffer.append("<td class=\"errors\"");
                buffer.appendAttribute("align", getErrorsAlign());
                buffer.appendAttribute("colspan", getColumns() * 2);
                buffer.appendAttribute("style", getErrorsStyle());
                buffer.append(">\n");
                buffer.append("<span class=\"error\">");
                buffer.append(getError());
                buffer.append("</span>\n");
                buffer.append("</td></tr>\n");
            }

            for (Field field : getErrorFields()) {

                // Certain fields might be invalid because
                // one of their contained fields are invalid. However these
                // fields might not have an error message to display.
                // If the outer field's error message is null don't render.
                if (field.getError() == null) {
                    continue;
                }

                buffer.append("<tr class=\"errors\">");
                buffer.append("<td class=\"errors\"");
                buffer.appendAttribute("align", getErrorsAlign());
                buffer.appendAttribute("colspan", getColumns() * 2);
                buffer.appendAttribute("style", getErrorsStyle());
                buffer.append(">");

                buffer.append("<a class=\"error\"");
                buffer.append(" href=\"javascript:");
                buffer.append(field.getFocusJavaScript());
                buffer.append("\">");
                buffer.append(field.getError());
                buffer.append("</a>");
                buffer.append("</td></tr>\n");
            }

            buffer.append("</tbody></table>\n");
            buffer.append("</td></tr>\n");
        }

        // Render JavaScript form validation code
        if (isJavaScriptValidation()) {
            buffer.append("<tr style=\"display:none\" id=\"");
            buffer.append(getId());
            buffer.append("-errorsTr\"><td width='100%' align=\"");
            buffer.append(getErrorsAlign());
            buffer.append("\">\n");
            buffer.append("<div class=\"errors\" id=\"");
            buffer.append(getId());
            buffer.append("-errorsDiv\"></div>\n");
            buffer.append("</td></tr>\n");
        }
    }

    /**
     * Render the given list of Buttons to the string buffer.
     *
     * @param buffer the StringBuffer to render to
     */
    protected void renderButtons(HtmlStringBuffer buffer) {

        List<Button> buttons = getButtonList();

        if (!buttons.isEmpty()) {
            buffer.append("<tr><td");
            buffer.appendAttribute("align", getButtonAlign());
            buffer.append(">\n");

            buffer.append("<table class=\"buttons\" id=\"");
            buffer.append(getId());
            buffer.append("-buttons\"><tbody>\n");
            buffer.append("<tr class=\"buttons\">");

            for (Button button : buttons) {
                buffer.append("<td class=\"buttons\"");
                buffer.appendAttribute("style", getButtonStyle());
                buffer.closeTag();

                button.render(buffer);

                buffer.append("</td>");
            }

            buffer.append("</tr>\n");
            buffer.append("</tbody></table>\n");
            buffer.append("</td></tr>\n");
        }
    }

    /**
     * Close the form tag and render any additional content after the Form.
     * <p/>
     * Additional content includes <tt>javascript validation</tt> and
     * <tt>javascript focus</tt> scripts.
     *
     * @param formFields all fields contained within the form
     * @param buffer the buffer to render to
     */
    protected void renderTagEnd(List<Field> formFields, HtmlStringBuffer buffer) {

        buffer.elementEnd(getTag());
        buffer.append("\n");

        renderFocusJavaScript(buffer, formFields);

        renderValidationJavaScript(buffer, formFields);
    }

    /**
     * Render the Form field focus JavaScript to the string buffer.
     *
     * @param buffer the StringBuffer to render to
     * @param formFields the list of form fields
     */
    protected void renderFocusJavaScript(HtmlStringBuffer buffer, List<Field> formFields) {

        // Set field focus
        boolean errorFieldFound = false;
        for (int i = 0, size = formFields.size(); i < size; i++) {
            Field field = formFields.get(i);

            if (field.getError() != null
                && !field.isHidden()
                && !field.isDisabled()) {

                String focusJavaScript =
                    StringUtils.replace(FOCUS_JAVASCRIPT,
                                        "$id",
                                        field.getId());
                buffer.append(focusJavaScript);
                errorFieldFound = true;
                break;
            }
        }

        if (!errorFieldFound) {
            for (int i = 0, size = formFields.size(); i < size; i++) {
                Field field = formFields.get(i);

                if (field.getFocus()
                    && !field.isHidden()
                    && !field.isDisabled()) {

                    String focusJavaScript =
                        StringUtils.replace(FOCUS_JAVASCRIPT,
                                            "$id",
                                            field.getId());
                    buffer.append(focusJavaScript);
                    break;
                }
            }
        }
    }

    /**
     * Render the Form validation JavaScript to the string buffer.
     *
     * @param buffer the StringBuffer to render to
     * @param formFields the list of form fields
     */
    protected void renderValidationJavaScript(HtmlStringBuffer buffer, List<Field> formFields) {

        // Render JavaScript form validation code
        if (isJavaScriptValidation()) {
            List<String> functionNames = new ArrayList<String>();

            buffer.append("<script type=\"text/javascript\"><!--\n");

            // Render field validation functions & build list of function names
            for (Field field : formFields) {
                String fieldJS = field.getValidationJavaScript();
                if (fieldJS != null) {
                    buffer.append(fieldJS);

                    StringTokenizer tokenizer = new StringTokenizer(fieldJS);
                    tokenizer.nextToken();
                    functionNames.add(tokenizer.nextToken());
                }
            }

            if (!functionNames.isEmpty()) {
                buffer.append("function on_");
                buffer.append(getId());
                buffer.append("_submit() {\n");
                buffer.append("   var msgs = new Array(");
                buffer.append(functionNames.size());
                buffer.append(");\n");
                for (int i = 0; i < functionNames.size(); i++) {
                    buffer.append("   msgs[");
                    buffer.append(i);
                    buffer.append("] = ");
                    buffer.append(functionNames.get(i).toString());
                    buffer.append(";\n");
                }
                buffer.append("   return validateForm(msgs, '");
                buffer.append(getId());
                buffer.append("', '");
                buffer.append(getErrorsAlign());
                buffer.append("', ");
                if (getErrorsStyle() == null) {
                    buffer.append("null");
                } else {
                    buffer.append("'" + getErrorsStyle() + "'");
                }
                buffer.append(");\n");
                buffer.append("}\n");

            } else {
                buffer.append("function on_");
                buffer.append(getId());
                buffer.append("_submit() { return true; }\n");
            }
            buffer.append("//--></script>\n");
        }
    }

    /**
     * Returns true if a POST error occurred, false otherwise.
     *
     * @return true if a POST error occurred, false otherwise
     */
    protected boolean hasPostError() {
        Exception e = (Exception)
            getContext().getRequest().getAttribute(FileUploadService.UPLOAD_EXCEPTION);

        if (e instanceof FileSizeLimitExceededException
            || e instanceof SizeLimitExceededException) {
            return true;
        }

        return false;
    }

    /**
     * Validate the request for any file upload (multipart) errors.
     * <p/>
     * A form error message is displayed if a file upload error occurs.
     * These messages are defined in the resource bundle:
     * <blockquote>
     * <ul>
     *   <li>/click-control.properties
     *     <ul>
     *       <li>file-size-limit-exceeded-error</li>
     *       <li>post-size-limit-exceeded-error</li>
     *     </ul>
     *   </li>
     * </ul>
     * </blockquote>
     */
    protected void validateFileUpload() {
        setError(null);

        Exception exception = (Exception) getContext().getRequest()
            .getAttribute(FileUploadService.UPLOAD_EXCEPTION);

        if (!(exception instanceof FileUploadException)) {
            return;
        }

        FileUploadException fue = (FileUploadException) exception;

        String key = null;
        Object args[] = null;

        if (fue instanceof SizeLimitExceededException) {
            SizeLimitExceededException se =
                (SizeLimitExceededException) fue;

            key = "post-size-limit-exceeded-error";

            args = new Object[2];
            args[0] = se.getPermittedSize();
            args[1] = se.getActualSize();
            setError(getMessage(key, args));

        } else if (fue instanceof FileSizeLimitExceededException) {
            FileSizeLimitExceededException fse =
                (FileSizeLimitExceededException) fue;

            key = "file-size-limit-exceeded-error";

            // Parse the FileField name from the message
            String msg = fue.getMessage();
            int start = 10;
            int end = msg.indexOf(' ', start);
            String fieldName = fue.getMessage().substring(start, end);

            args = new Object[3];
            args[0] = ClickUtils.toLabel(fieldName);
            args[1] = fse.getPermittedSize();
            args[2] = fse.getActualSize();
            setError(getMessage(key, args));
        }
    }

    // Private Methods --------------------------------------------------------

         /**
     * Add fields for the given Container to the specified field list,
     * recursively including any Fields contained in child containers.
     *
     * @param container the container to obtain the fields from
     * @param fields the list of contained fields
     */
    private void addStatefulFields(final Container container, final List<Field> fields) {
        for (Control control : container.getControls()) {
            if (control instanceof Label
                || control instanceof Button
                || control instanceof NonProcessedHiddenField
                ) {
                // Skip buttons and labels and NonProcessedHiddenFields
                continue;
            }

            if (control instanceof Field) {
                fields.add((Field) control);
            } else if (control instanceof org.apache.click.control.Container) {
                Container childContainer = (Container) control;
                addStatefulFields(childContainer, fields);
            }
        }
    }

    /**
     * Return true if the control is hidden, false otherwise.
     *
     * @param control control to check hidden status
     * @return true if the control is hidden, false otherwise
     */
    private boolean isHidden(Control control) {
        if (!(control instanceof Field)) {
            // Non-Field Controls can not be hidden
            return false;
        } else {
            return ((Field) control).isHidden();
        }
    }

    // Inner Classes ----------------------------------------------------------

    /**
     * Provides a HiddenField which does not get processed or bind to its
     * incoming value. In addition the field name cannot be changed once set.
     */
    private static class NonProcessedHiddenField extends HiddenField {

        private static final long serialVersionUID = 1L;

        /**
         * Create a field with the given name and class.
         *
         * @param name the field name
         * @param valueClass the Class of the value Object
         */
        public NonProcessedHiddenField(String name, Class<?> valueClass) {
            super(name, valueClass);
        }

        /**
         * Create a field with the given name and value.
         *
         * @param name the field name
         * @param value the value of the field
         */
        public NonProcessedHiddenField(String name, Object value) {
            super(name, value);
        }

        /**
         * This method is overridden to not change the field name once it is set.
         *
         * @param name the name of the field
         */
        @Override
        public void setName(String name) {
            if (this.name != null) {
                return;
            }
            super.setName(name);
        }

        /**
         * Overridden to not process the field or bind to its request value.
         */
        @Override
        public boolean onProcess() {
            return true;
        }
    }

    /**
     * Provides a HiddenField which name and value cannot be changed, once it
     * is set.
     */
    private static class ImmutableHiddenField extends NonProcessedHiddenField {

        private static final long serialVersionUID = 1L;

       /**
         * Create a field with the given name and value.
         *
         * @param name the field name
         * @param value the value of the field
         */
        public ImmutableHiddenField(String name, Object value) {
            super(name, value);
        }

        /**
         * This method is overridden to not change the field value once it is set.
         *
         * @param value the field value
         */
        @Override
        public void setValue(String value) {
            if (this.value != null) {
                return;
            }
            super.setValue(value);
        }

        /**
         * This method is overridden to not change the field value object once
         * it is set.
         *
         * @param valueObject the field value object
         */
        @Override
        public void setValueObject(Object valueObject) {
            if (this.valueObject != null) {
                return;
            }
            super.setValueObject(valueObject);
        }
    }
}
