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

/**
 * Behaviors provide a mechanism for changing how Controls behave at runtime.
 * Behaviors are added to a Control and provides interceptor methods to decorate
 * and enhance the source Control.
 * <p/>
 * Behaviors provide interceptor methods for specific Control life cycle events.
 * These interceptor methods can be implemented to further process and decorate
 * the control or its children.
 * <p/>
 * The following interceptor methods are defined:
 *
 * <ul>
 * <li>preResponse - occurs before the control markup is written to the response</li>
 * <li>preRenderHeadElements - occurs after <tt>preResponse</tt> but before the control
 * {@link Control#getHeadElements() HEAD elements} are written to the response</li>
 * <li>preDestroy - occurs before the Control {@link Control#onDestroy() onDestroy}
 * event handler.</li>
 * </ul>
 *
 * These interceptor methods allow the Behavior to <tt>decorate</tt> a control,
 * for example:
 *
 * <ul>
 * <li>add or remove Control HEAD elements such as JavaScript and CSS dependencies
 * and setup scripts</li>
 * <li>add or remove Control attributes such as <tt>"class"</tt>, <tt>"style"</tt> etc.</li>
 * </ul>
 */
public interface Behavior {

    /**
     * This event occurs before the markup is written to the HttpServletResponse.
     *
     * @param source the control the behavior is registered with
     */
    public void preResponse(Control source);

    /**
     * This event occurs after {@link #preResponse(Control)},
     * but before the Control's {@link Control#getHeadElements()} is called.
     *
     * @param source the control the behavior is registered with
     */
    public void preRenderHeadElements(Control source);

    /**
     * This event occurs before the Control {@link Control#onDestroy() onDestroy}
     * event handler. This event allows the behavior to cleanup or store Control
     * state in the Session.
     *
     * @param source the control the behavior is registered with
     */
    public void preDestroy(Control source);
}
