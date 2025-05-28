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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import java.util.List;
import java.util.Set;

import org.openidentityplatform.openam.click.ajax.AjaxBehavior;
import org.openidentityplatform.openam.click.service.ConfigService;
import org.openidentityplatform.openam.click.service.LogService;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.Validate;

/**
 * Provides a control ActionListener and AjaxBehavior dispatcher. The
 * ClickServlet will dispatch registered ActionListeners and AjaxBehaviors after
 * page controls have been processed.
 *
 * <h4>Example Usage</h4>
 * The following example shows how to register an ActionListener with a custom
 * Control:
 *
 * <pre class="prettyprint">
 * public class MyControl extends AbstractControl {
 *     ...
 *
 *     public boolean onProcess() {
 *         bindRequestValue();
 *
 *         if (isClicked()) {
 *             // Dispatch an action listener event for invocation after
 *             // control processing has finished
 *             dispatchActionEvent();
 *         }
 *
 *         return true;
 *     }
 * } </pre>
 *
 * When the link is clicked it invokes the method
 * {@link org.openidentityplatform.openam.click.control.AbstractControl#dispatchActionEvent()}.
 * This method registers the Control's action listener with the
 * ActionEventDispatcher. The ClickServlet will subsequently invoke the registered
 * {@link ActionListener#onAction(Control)} method after all the Page controls
 * <tt>onProcess()</tt> method have been invoked.
 */
public class ActionEventDispatcher {

    // Constants --------------------------------------------------------------

    /** The thread local dispatcher holder. */
    private static final ThreadLocal<DispatcherStack> THREAD_LOCAL_DISPATCHER_STACK
            = new ThreadLocal<>();

    // Variables --------------------------------------------------------------

    /** The list of registered event sources. */
    List<Control> eventSourceList;

    /** The list of registered event listeners. */
    List<ActionListener> eventListenerList;

    /** The set of Controls with attached AjaxBehaviors. */
    Set<Control> ajaxBehaviorSourceSet;

    /**
     * The {@link org.apache.click.ActionResult} to render. This action result is
     * returned from the target Behavior.
     */
    ActionResult actionResult;

    /** The application log service. */
    LogService logger;

    // Constructors -----------------------------------------------------------

    /**
     * Construct the ActionEventDispatcher with the given ConfigService.
     *
     * @param configService the click application configuration service
     */
    public ActionEventDispatcher(ConfigService configService) {
        this.logger = configService.getLogService();
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Register the event source and event ActionListener to be fired by the
     * ClickServlet once all the controls have been processed.
     *
     * @param source the action event source
     * @param listener the event action listener
     */
    public static void dispatchActionEvent(Control source, ActionListener listener) {
        Validate.notNull(source, "Null source parameter");
        Validate.notNull(listener, "Null listener parameter");

        ActionEventDispatcher instance = getThreadLocalDispatcher();
        instance.registerActionEvent(source, listener);
    }

    /**
     * Register the source control which AjaxBehaviors should be fired by the
     * ClickServlet.
     *
     * @param source the source control which behaviors should be fired
     */
    public static void dispatchAjaxBehaviors(Control source) {
        Validate.notNull(source, "Null source parameter");

        ActionEventDispatcher instance = getThreadLocalDispatcher();
        instance.registerAjaxBehaviorSource(source);
    }

    /**
     * Return the thread local ActionEventDispatcher instance.
     *
     * @return the thread local ActionEventDispatcher instance.
     * @throws RuntimeException if an ActionEventDispatcher is not available on
     * the thread
     */
    public static ActionEventDispatcher getThreadLocalDispatcher() {
        return getDispatcherStack().peek();
    }

    /**
     * Returns true if an ActionEventDispatcher instance is available on the
     * current thread, false otherwise.
     * <p/>
     * Unlike {@link #getThreadLocalDispatcher()} this method can safely be used
     * and will not throw an exception if an ActionEventDispatcher is not
     * available on the current thread.
     *
     * @return true if an ActionEventDispatcher instance is available on the
     * current thread, false otherwise
     */
    public static boolean hasThreadLocalDispatcher() {
        DispatcherStack dispatcherStack = THREAD_LOCAL_DISPATCHER_STACK.get();
        if (dispatcherStack == null) {
            return false;
        }
        return !dispatcherStack.isEmpty();
    }

    /**
     * Fire all the registered action events after the Page Controls have been
     * processed and return true if the page should continue processing.
     *
     * @param context the request context
     *
     * @return true if the page should continue processing, false otherwise
     */
    public boolean fireActionEvents(Context context) {

        if (!hasActionEvents()) {
            return true;
        }

        return fireActionEvents(context, getEventSourceList(), getEventListenerList());
    }

    /**
     * Fire all the registered AjaxBehaviors and return true if the page should
     * continue processing, false otherwise.
     *
     * @see #fireAjaxBehaviors(Context, java.util.Set)
     *
     * @param context the request context
     *
     * @return true if the page should continue processing, false otherwise
     */
    public boolean fireAjaxBehaviors(Context context) {

        if (!hasAjaxBehaviorSourceSet()) {
            return true;
        }

        return fireAjaxBehaviors(context, getAjaxBehaviorSourceSet());
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Allow the dispatcher to handle the error that occurred.
     *
     * @param throwable the error which occurred during processing
     */
    protected void errorOccurred(Throwable throwable) {
        // Clear the control listeners and behaviors from the dispatcher
        clear();
    }

    /**
     * Fire the actions for the given listener list and event source list which
     * return true if the page should continue processing.
     * <p/>
     * This method can be overridden if you need to customize the way events
     * are fired.
     *
     * @param context the request context
     * @param eventSourceList the list of source controls
     * @param eventListenerList the list of listeners to fire
     *
     * @return true if the page should continue processing or false otherwise
     */
    protected boolean fireActionEvents(Context context,
                                       List<Control> eventSourceList, List<ActionListener> eventListenerList) {

        boolean continueProcessing = true;

        for (int i = 0, size = eventSourceList.size(); i < size; i++) {
            Control source = eventSourceList.remove(0);
            ActionListener listener = eventListenerList.remove(0);

            if (!fireActionEvent(context, source, listener)) {
                continueProcessing = false;
            }
        }

        return continueProcessing;
    }

    /**
     * Fire the action for the given listener and event source which
     * return true if the page should continue processing.
     * <p/>
     * This method can be overridden if you need to customize the way events
     * are fired.
     *
     * @param context the request context
     * @param source the source control
     * @param listener the listener to fire
     *
     * @return true if the page should continue processing, false otherwise
     */
    protected boolean fireActionEvent(Context context, Control source,
                                      ActionListener listener) {
        return listener.onAction(source);
    }

    /**
     * Fire the AjaxBehaviors for the given control set and return true if the page
     * should continue processing, false otherwise.
     * <p/>
     * This method can be overridden if you need to customize the way
     * AjaxBehaviors are fired.
     *
     * @see #fireAjaxBehaviors(Context, Control)
     *
     * @param context the request context
     * @param ajaxBbehaviorSourceSet the set of controls with attached AjaxBehaviors
     *
     * @return true if the page should continue processing, false otherwise
     */
    protected boolean fireAjaxBehaviors(Context context, Set<Control> ajaxBbehaviorSourceSet) {

        boolean continueProcessing = true;

        for (Iterator<Control> it = ajaxBbehaviorSourceSet.iterator(); it.hasNext();) {
            Control source = it.next();

            // Pop the first entry in the set
            it.remove();

            if (!fireAjaxBehaviors(context, source)) {
                continueProcessing = false;
            }
        }

        return continueProcessing;
    }

    /**
     * Fire the AjaxBehaviors for the given control and return true if the
     * page should continue processing, false otherwise. AjaxBehaviors will
     * only fire if their {@link org.openidentityplatform.openam.click.ajax.AjaxBehavior#isAjaxTarget(Context) isAjaxTarget()}
     * method returns true.
     * <p/>
     * This method can be overridden if you need to customize the way
     * AjaxBehaviors are fired.
     *
     * @param context the request context
     * @param source the control which attached behaviors should be fired
     *
     * @return true if the page should continue processing, false otherwise
     */
    protected boolean fireAjaxBehaviors(Context context, Control source) {

        boolean continueProcessing = true;

        if (logger.isTraceEnabled()) {
            String sourceClassName = ClassUtils.getShortClassName(source.getClass());
            HtmlStringBuffer buffer = new HtmlStringBuffer();
            buffer.append("   processing AjaxBehaviors for control: '");
            buffer.append(source.getName()).append("' ");
            buffer.append(sourceClassName);
            logger.trace(buffer.toString());
        }

        for (Behavior behavior : source.getBehaviors()) {

            if (behavior instanceof AjaxBehavior) {
                AjaxBehavior ajaxBehavior = (AjaxBehavior) behavior;

                boolean isAjaxTarget = ajaxBehavior.isAjaxTarget(context);

                if (logger.isTraceEnabled()) {
                    String behaviorClassName = ClassUtils.getShortClassName(
                            ajaxBehavior.getClass());
                    HtmlStringBuffer buffer = new HtmlStringBuffer();
                    buffer.append("      invoked: ");
                    buffer.append(behaviorClassName);
                    buffer.append(".isAjaxTarget() : ");
                    buffer.append(isAjaxTarget);
                    logger.trace(buffer.toString());
                }

                if (isAjaxTarget) {

                    // The first non-null ActionResult returned will be rendered, other
                    // ActionResult instances are ignored
                    ActionResult behaviorActionResult =
                            ajaxBehavior.onAction(source);
                    if (actionResult == null && behaviorActionResult != null) {
                        actionResult = behaviorActionResult;
                    }

                    if (logger.isTraceEnabled()) {
                        String behaviorClassName = ClassUtils.getShortClassName(
                                ajaxBehavior.getClass());
                        String actionResultClassName = null;

                        if (behaviorActionResult != null) {
                            actionResultClassName = ClassUtils.getShortClassName(
                                    behaviorActionResult.getClass());
                        }

                        HtmlStringBuffer buffer = new HtmlStringBuffer();
                        buffer.append("      invoked: ");
                        buffer.append(behaviorClassName);
                        buffer.append(".onAction() : ");
                        buffer.append(actionResultClassName);

                        if (actionResult == behaviorActionResult
                                && behaviorActionResult != null) {
                            buffer.append(" (ActionResult will be rendered)");
                        } else {
                            if (behaviorActionResult == null) {
                                buffer.append(" (ActionResult is null and will be ignored)");
                            } else {
                                buffer.append(" (ActionResult will be ignored since another AjaxBehavior already retuned a non-null ActionResult)");
                            }
                        }

                        logger.trace(buffer.toString());
                    }

                    continueProcessing = false;
                    break;
                }
            }
        }

        if (logger.isTraceEnabled()) {

            // continueProcessing is true if no AjaxBehavior was the target
            // of the request
            if (continueProcessing) {
                HtmlStringBuffer buffer = new HtmlStringBuffer();
                String sourceClassName = ClassUtils.getShortClassName(
                        source.getClass());
                buffer.append("   *no* target AjaxBehavior found for '");
                buffer.append(source.getName()).append("' ");
                buffer.append(sourceClassName);
                buffer.append(" - invoking AjaxBehavior.isAjaxTarget() returned false for all AjaxBehaviors");
                logger.trace(buffer.toString());
            }
        }

        // Ajax requests stops further processing
        return continueProcessing;
    }

    // Package Private Methods ------------------------------------------------

    /**
     * Register the event source and event ActionListener.
     *
     * @param source the action event source
     * @param listener the event action listener
     */
    void registerActionEvent(Control source, ActionListener listener) {
        Validate.notNull(source, "Null source parameter");
        Validate.notNull(listener, "Null listener parameter");

        getEventSourceList().add(source);
        getEventListenerList().add(listener);
    }

    /**
     * Register the AjaxBehavior source control.
     *
     * @param source the AjaxBehavior source control
     */
    void registerAjaxBehaviorSource(Control source) {
        Validate.notNull(source, "Null source parameter");

        getAjaxBehaviorSourceSet().add(source);
    }

    /**
     * Checks if any Action Events have been registered.
     *
     * @return true if the dispatcher has any Action Events registered
     */
    boolean hasActionEvents() {
        if (eventListenerList == null || eventListenerList.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Return the list of event listeners.
     *
     * @return list of event listeners
     */
    List<ActionListener> getEventListenerList() {
        if (eventListenerList == null) {
            eventListenerList = new ArrayList<ActionListener>();
        }
        return eventListenerList;
    }

    /**
     * Return the list of event sources.
     *
     * @return list of event sources
     */
    List<Control> getEventSourceList() {
        if (eventSourceList == null) {
            eventSourceList = new ArrayList<Control>();
        }
        return eventSourceList;
    }

    /**
     * Clear the events and behaviors.
     */
    void clear() {
        if (hasActionEvents()) {
            getEventSourceList().clear();
            getEventListenerList().clear();
        }

        if (hasAjaxBehaviorSourceSet()) {
            getAjaxBehaviorSourceSet().clear();
        }
    }

    /**
     * Return the Behavior's action result or null if no behavior was dispatched.
     *
     * @return the Behavior's action result or null if no behavior was dispatched
     */
    ActionResult getActionResult() {
        return actionResult;
    }

    /**
     * Return true if a control with AjaxBehaviors was registered, false otherwise.
     *
     * @return true if a control with AjaxBehaviors was registered, false otherwise.
     */
    boolean hasAjaxBehaviorSourceSet() {
        if (ajaxBehaviorSourceSet == null || ajaxBehaviorSourceSet.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Return the set of controls with attached AjaxBehaviors.
     *
     * @return set of control with attached AjaxBehaviors
     */
    Set<Control> getAjaxBehaviorSourceSet() {
        if (ajaxBehaviorSourceSet == null) {
            ajaxBehaviorSourceSet = new LinkedHashSet<Control>();
        }
        return ajaxBehaviorSourceSet;
    }

    /**
     * Adds the specified ActionEventDispatcher on top of the dispatcher stack.
     *
     * @param actionEventDispatcher the ActionEventDispatcher to add
     */
    static void pushThreadLocalDispatcher(ActionEventDispatcher actionEventDispatcher) {
        getDispatcherStack().push(actionEventDispatcher);
    }

    /**
     * Remove and return the actionEventDispatcher instance on top of the
     * dispatcher stack.
     *
     * @return the actionEventDispatcher instance on top of the dispatcher stack
     */
    static ActionEventDispatcher popThreadLocalDispatcher() {
        DispatcherStack dispatcherStack = getDispatcherStack();
        ActionEventDispatcher actionEventDispatcher = dispatcherStack.pop();

        if (dispatcherStack.isEmpty()) {
            THREAD_LOCAL_DISPATCHER_STACK.set(null);
        }

        return actionEventDispatcher;
    }

    /**
     * Return the stack data structure where ActionEventDispatchers are stored.
     *
     * @return stack data structure where ActionEventDispatcher are stored
     */
    static ActionEventDispatcher.DispatcherStack getDispatcherStack() {
        DispatcherStack dispatcherStack = THREAD_LOCAL_DISPATCHER_STACK.get();

        if (dispatcherStack == null) {
            dispatcherStack = new ActionEventDispatcher.DispatcherStack(2);
            THREAD_LOCAL_DISPATCHER_STACK.set(dispatcherStack);
        }

        return dispatcherStack;
    }

    // Inner Classes ----------------------------------------------------------

    /**
     * Provides an unsynchronized Stack.
     */
    static class DispatcherStack extends ArrayList<ActionEventDispatcher> {

        /** Serialization version indicator. */
        private static final long serialVersionUID = 1L;

        /**
         * Create a new DispatcherStack with the given initial capacity.
         *
         * @param initialCapacity specify initial capacity of this stack
         */
        private DispatcherStack(int initialCapacity) {
            super(initialCapacity);
        }

        /**
         * Pushes the ActionEventDispatcher onto the top of this stack.
         *
         * @param actionEventDispatcher the ActionEventDispatcher to push onto this stack
         * @return the ActionEventDispatcher pushed on this stack
         */
        private ActionEventDispatcher push(ActionEventDispatcher actionEventDispatcher) {
            add(actionEventDispatcher);

            return actionEventDispatcher;
        }

        /**
         * Removes and return the ActionEventDispatcher at the top of this stack.
         *
         * @return the ActionEventDispatcher at the top of this stack
         */
        private ActionEventDispatcher pop() {
            ActionEventDispatcher actionEventDispatcher = peek();

            remove(size() - 1);

            return actionEventDispatcher;
        }

        /**
         * Looks at the ActionEventDispatcher at the top of this stack without
         * removing it.
         *
         * @return the ActionEventDispatcher at the top of this stack
         */
        private ActionEventDispatcher peek() {
            int length = size();

            if (length == 0) {
                String msg = "No ActionEventDispatcher available on ThreadLocal Dispatcher Stack";
                throw new RuntimeException(msg);
            }

            return get(length - 1);
        }
    }
}
