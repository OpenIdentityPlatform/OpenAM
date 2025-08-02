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

/**
 * Provides a Label display control. The Label control performs no server side
 * processing, and is used primarily to add descriptive labels or horizontal
 * rules to auto rendered forms.
 *
 * <h3>Label Example</h3>
 *
 * A Label example:
 *
 * <pre class="codeJava">
 * Form form = <span class="kw">new</span> Form(<span class="st">"form"</span>);
 * ..
 *
 * form.add(<span class="kw">new</span> Label(<span class="st">"hr"</span>, <span class="st">"&lt;hr/&gt;"</span>)); </pre>
 *
 * HTML output:
 * <pre class="codeHtml">
 * &lt;tr&gt;&lt;td colspan='2' align='left'&gt;&lt;hr/&gt;&lt;/td&gt;&lt;/tr&gt; </pre>
 */
public class Label extends Field {

    private static final long serialVersionUID = 1L;

    // ----------------------------------------------------------- Constructors

    /**
     * Create a Label display control.
     * <p/>
     * Note the Label control will attempt to find a localized label message
     * in the parent messages, and if not found then in the field messages
     * using the key name of <tt>getName() + ".label"</tt>.
     * <p/>
     * If a value cannot be found in the parent or control messages then the
     * Field name will be converted into a label using the
     * {@link org.apache.click.util.ClickUtils#toLabel(String)} method.
     *
     * @param name the name of the Field
     */
    public Label(String name) {
        super(name);
    }

    /**
     * Create a Label display control with the given name and label.
     *
     * @param name the name of the Field
     * @param label the display label caption
     */
    public Label(String name, String label) {
        super(name, label);
    }

    /**
     * Create a Label with no label/name defined.
     * <p/>
     * <b>Please note</b> the control's name must be defined before it is valid.
     */
    public Label() {
        super();
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Returns true.
     *
     * @see Field#onProcess()
     *
     * @return true to continue Page event processing or false otherwise
     */
    @Override
    public boolean onProcess() {
        return true;
    }

    /**
     * Render a label.
     *
     * @see #toString()
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {
        buffer.append(getLabel());
    }

    /**
     * Returns the label.
     *
     * @see Object#toString()
     *
     * @return the label string value
     */
    @Override
    public String toString() {
        return getLabel();
    }
}
