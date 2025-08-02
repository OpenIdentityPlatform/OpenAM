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

package org.openidentityplatform.openam.click.ajax;


import org.openidentityplatform.openam.click.ActionResult;
import org.openidentityplatform.openam.click.Behavior;
import org.openidentityplatform.openam.click.Context;
import org.openidentityplatform.openam.click.Control;

/**
 * AjaxBehavior extends the basic Behavior functionality to allow Controls to
 * handle and process incoming Ajax requests.
 * <p/>
 * To handle an Ajax request, AjaxBehavior exposes the listener method:
 * {@link #onAction(org.openidentityplatform.openam.click.Control) onAction}.
 * The <tt>onAction</tt> method returns an ActionResult that is rendered back
 * to the browser.
 * <p/>
 * Before Click invokes the <tt>onAction</tt> method it checks whether the request
 * is targeted at the AjaxBehavior by invoking the method
 * {@link #isAjaxTarget(org.openidentityplatform.openam.click.Context) Behavior.isAjaxTarget()}.
 * Click will only invoke <tt>onAction</tt> if <tt>isAjaxTarget</tt> returns true.
 */
public interface AjaxBehavior extends Behavior {

    /**
     * This method can be implemented to handle and respond to an Ajax request.
     * For example:
     *
     * <pre class="prettyprint">
     * public void onInit() {
     *     ActionLink link = new ActionLink("link");
     *     link.addBehaior(new DefaultAjaxBehavior() {
     *
     *         public ActionResult onAction(Control source) {
     *             ActionResult result = new ActionResult("&lt;h1&gt;Hello world&lt;/h1&gt;", ActionResult.HTML);
     *             return result;
     *         }
     *     });
     * } </pre>
     *
     * @param source the control the behavior is attached to
     * @return the action result instance
     */
    public ActionResult onAction(Control source);

    /**
     * Return true if the behavior is the request target, false otherwise.
     * <p/>
     * This method is queried by Click to determine if the behavior's
     * {@link #onAction(org.openidentityplatform.openam.click.Control)} method should be called in
     * response to a request.
     * <p/>
     * By exposing this method through the Behavior interface it provides
     * implementers with fine grained control over whether the Behavior's
     * {@link #onAction(org.openidentityplatform.openam.click.Control)} method should be invoked or not.
     * <p/>
     * Below is an example implementation:
     *
     * <pre class="prettyprint">
     * public CustomBehavior implements Behavior {
     *
     *     private String eventType;
     *
     *     public CustomBehavior(String eventType) {
     *         // The event type of the behavior
     *         super(eventType);
     *     }
     *
     *     public boolean isAjaxTarget(Context context) {
     *         // Retrieve the eventType parameter from the incoming request
     *         String eventType = context.getRequestParameter("type");
     *
     *         // Check if this Behavior's eventType matches the request
     *         // "type" parameter
     *         return StringUtils.equalsIgnoreCase(this.eventType, eventType);
     *     }
     *
     *     public ActionResult onAction(Control source) {
     *         // If isAjaxTarget returned true, the onAction method will be
     *         // invoked
     *         ...
     *     }
     * } </pre>
     *
     * @param context the request context
     * @return true if the behavior is the request target, false otherwise
     */
    public boolean isAjaxTarget(Context context);
}
