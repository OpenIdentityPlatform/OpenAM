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
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;

/**
 * Provides a File Field control: &nbsp; &lt;input type='file'&gt;.
 *
 * <table class='htmlHeader' cellspacing='6'>
 * <tr>
 * <td>File Field</td>
 * <td><input type='file' value='' title='FileField Control'/></td>
 * </tr>
 * </table>
 *
 * The FileField control uses the Jakarta Commons
 * <a href="http://jakarta.apache.org/commons/fileupload/">FileUpload</a>
 * library to provide file processing functionality.
 * <p/>
 * You can control the {@link org.apache.click.service.CommonsFileUploadService#sizeMax maximum request size}
 * and {@link org.apache.click.service.CommonsFileUploadService#fileSizeMax maximum file size}
 * by configuring {@link org.apache.click.service.CommonsFileUploadService}.
 * <p/>
 * Note Browsers enforce the JavaScript <tt>value</tt> property as readonly
 * to prevent script based stealing of users files.
 * <p/>
 * You can make the file field invisible by setting the CSS display attribute, for
 * example:
 *
 * <pre class="codeHtml">
 * &lt;form method="POST" enctype="multipart/form-data"&gt;
 *    &lt;input type="file" name="myfile" <span class='st'>style</span>=<span class='red'>"display:none"</span> onchange="fileName=this.value"&gt;
 *    &lt;input type="button" value="open file" onclick="myfile.click()"&gt;
 *    &lt;input type="button" value="show value" onclick="alert(fileName)"&gt;
 * &lt;/form&gt; </pre>
 *
 * <p/>
 * Please also see the references:
 * <ul>
 * <li><a target="blank" href="http://commons.apache.org/fileupload/using.html">Apache Commons - Using FileUpload</a></li>
 * <li><a target="blank" href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867 - Form-based File Upload in HTML</a></li>
 * <li><a target="blank" href="http://www.cs.tut.fi/~jkorpela/forms/file.html">Jukka Korpela - File input (or "upload") in HTML forms</a></li>
 * <li><a target="blank" href="https://www.sdn.sap.com/sdn/weblogs.sdn?blog=/pub/wlg/684">SDN - INPUT TYPE="FILE" and your options...</a></li>
 * </ul>
 */
public class FileField extends Field {

    // -------------------------------------------------------------- Constants

    private static final long serialVersionUID = 1L;

    /**
     * The field validation JavaScript function template.
     * The function template arguments are: <ul>
     * <li>0 - is the field id</li>
     * <li>1 - is the Field required status</li>
     * <li>2 - is the localized error message</li>
     * </ul>
     */
    protected final static String VALIDATE_FILEFIELD_FUNCTION =
        "function validate_{0}() '{'\n"
        + "   var msg = validateFileField(''{0}'',{1}, [''{2}'']);\n"
        + "   if (msg) '{'\n"
        + "      return msg + ''|{0}'';\n"
        + "   '}' else '{'\n"
        + "      return null;\n"
        + "   '}'\n"
        + "'}'\n";

    // ----------------------------------------------------- Instance Variables

    /** The text field size attribute. The default size is 20. */
    protected int size = 20;

    /**
     * The
     * <a href="http://jakarta.apache.org/commons/fileupload/apidocs/org/apache/commons/fileupload/DefaultFileItem.html">DefaultFileItem</a>
     * after processing a file upload request.
     */
    protected FileItem fileItem;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct the FileField with the given name.
     *
     * @param name the name of the field
     */
    public FileField(String name) {
        super(name);
    }

    /**
     * Construct the FileField with the given name and required status.
     *
     * @param name the name of the field
     * @param required the field required status
     */
    public FileField(String name, boolean required) {
        super(name);
        setRequired(required);
    }

    /**
     * Construct the FileField with the given name and label.
     *
     * @param name the name of the field
     * @param label the label of the field
     */
    public FileField(String name, String label) {
        super(name, label);
    }


    /**
     * Construct the FileField with the given name, label and required status.
     *
     * @param name the name of the field
     * @param label the label of the field
     * @param required the required status
     */
    public FileField(String name, String label, boolean required) {
        super(name, label);
        setRequired(required);
    }

    /**
     * Construct the FileField with the given name, label and size.
     *
     * @param name the name of the field
     * @param label the label of the field
     * @param size the size of the field
     */
    public FileField(String name, String label, int size) {
        this(name, label);
        setSize(size);
    }

    /**
     * Create an FileField with no name defined.
     * <p/>
     * <b>Please note</b> the control's name must be defined before it is valid.
     */
    public FileField() {
        super();
    }

    // ------------------------------------------------------ Public Attributes

    /**
     * Return the FileField's html tag: <tt>input</tt>.
     *
     * @see org.apache.click.control.AbstractControl#getTag()
     *
     * @return this controls html tag
     */
    @Override
    public String getTag() {
        return "input";
    }

    /**
     * Return the <a href="http://jakarta.apache.org/commons/fileupload/apidocs/org/apache/commons/fileupload/FileItem.html">FileItem</a>
     * after processing the request, or null otherwise.
     *
     * @return the <tt>FileItem</tt> after processing a request
     */
    public FileItem getFileItem() {
        return fileItem;
    }

    /**
     * Return the field size.
     *
     * @return the field size
     */
    public int getSize() {
        return size;
    }

    /**
     * Set the field size.
     *
     * @param  size the field size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Return the input type: '<tt>file</tt>'.
     *
     * @return the input type: '<tt>file</tt>'
     */
    public String getType() {
        return "file";
    }

    /**
     * Return the FileField JavaScript client side validation function.
     *
     * @return the field JavaScript client side validation function
     */
    @Override
    public String getValidationJavaScript() {
        if (isRequired()) {
            Object[] args = new Object[3];
            args[0] = getId();
            args[1] = String.valueOf(isRequired());
            args[2] = getMessage("file-required-error", getErrorLabel());

            return MessageFormat.format(VALIDATE_FILEFIELD_FUNCTION, args);

        } else {
            return null;
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Set the {@link #fileItem} property from the multi-part form data
     * submission.
     */
    @Override
    public void bindRequestValue() {
        fileItem = getContext().getFileItem(getName());
    }

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
     * Overrides onProcess to use {@link Context#getFileItem(String)}.
     *
     * @see Field#onProcess()
     *
     * @return true to continue Page event processing or false otherwise
     */
    @Override
    public boolean onProcess() {
        Context context = getContext();

        if (context.getFileItemMap().containsKey(getName())) {
            // Only process field if it participated in the incoming request
            setDisabled(false);

            bindRequestValue();

            if (getValidate()) {
                validate();
            }

            dispatchActionEvent();
        }

        return true;
    }

    /**
     * Render the HTML representation of the FileField.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {
        buffer.elementStart(getTag());

        buffer.appendAttribute("type", getType());
        buffer.appendAttribute("name", getName());
        buffer.appendAttribute("id", getId());
        buffer.appendAttributeEscaped("value", getValue());
        buffer.appendAttribute("size", getSize());
        buffer.appendAttribute("title", getTitle());
        if (isValid()) {
            removeStyleClass("error");
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

        buffer.elementEnd();

        if (getHelp() != null) {
            buffer.append(getHelp());
        }
    }

    /**
     * Validate the FileField request submission.
     * <p/>
     * A field error message is displayed if a validation error occurs.
     * These messages are defined in the resource bundle:
     * <blockquote>
     * <ul>
     *   <li>/click-control.properties
     *     <ul>
     *       <li>file-required-error</li>
     *     </ul>
     *   </li>
     * </ul>
     * </blockquote>
     */
    @Override
    public void validate() {
        setError(null);

        if (isRequired()) {
            FileItem localFileItem = getFileItem();
            if (localFileItem == null || StringUtils.isBlank(localFileItem.getName())) {
                setErrorMessage("file-required-error");
            }
        }
    }

}
