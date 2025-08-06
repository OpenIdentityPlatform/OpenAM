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

import java.io.Serializable;

/**
 * Provides a listener interface for receiving control action events.
 * The usage model is similar to the <tt>java.awt.event.ActionListener</tt>
 * interface.
 * <p/>
 * The class that is interested in processing an action event
 * implements this interface, and the object created with that class is
 * registered with a control, using the control's <tt>setActionListener</tt>
 * method. When the action event occurs, that object's <tt>onAction</tt> method
 * is invoked.
 *
 * <h3>Listener Example</h3>
 *
 * An ActionListener example is provided below:
 *
 * <pre class="prettyprint">
 * public MyPage extends Page {
 *
 *    public ActionLink link = new ActionLink();
 *
 *    public MyPage() {
 *
 *       link.setActionListener(new ActionListener() {
 *           public boolean onAction(Control source) {
 *               return onLinkClick();
 *           }
 *        });
 *    }
 *
 *    public boolean onLinkClick() {
 *       ..
 *       return true;
 *    }
 * }
 * </pre>
 */
public interface ActionListener extends Serializable {

    /**
     * Return true if the control and page processing should continue, or false
     * otherwise.
     *
     * @param source the source of the action event
     * @return true if control and page processing should continue or false
     * otherwise.
     */
    public boolean onAction(Control source);

}
