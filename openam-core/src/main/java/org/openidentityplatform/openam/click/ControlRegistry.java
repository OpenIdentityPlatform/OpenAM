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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openidentityplatform.openam.click.service.ConfigService;
import org.openidentityplatform.openam.click.service.LogService;
import org.apache.commons.lang.Validate;

/**
 * Provides a centralized registry where Controls can be registered and interact
 * with the Click runtime.
 * <p/>
 * The primary use of the ControlRegistry is for Controls to register themselves
 * as potential <tt>targets</tt> of Ajax requests
 * (If a control is an Ajax request target, it's <tt>onProcess()</tt>
 * method is invoked while other controls are not processed).
 * <p/>
 * Registering controls as Ajax targets serves a dual purpose. In addition to
 * being potential Ajax targets, these controls will have all their Behaviors
 * processed by the Click runtime.
 * <p/>
 * Thus the ControlRegistry provides the Click runtime  with easy access to Controls
 * that want to be processed for Ajax requests. It also provides quick access
 * to Controls that have Behaviors, and particularly AjaxBehaviors that want to
 * handle and respond to Ajax requests.
 *
 * <h3>Register Control as an Ajax Target</h3>
 * Below is an example of a Control registering itself as an Ajax target:
 *
 * <pre class="prettyprint">
 * public class AbstractControl implements Control {
 *
 *     public void addBehavior(Behavior behavior) {
 *         getBehaviors().add(behavior);
 *         // Adding a behavior also registers the Control as an Ajax target
 *         ControlRegistry.registerAjaxTarget(this);
 *     }
 * } </pre>
 *
 * <h3>Register Interceptor</h3>
 * Below is an example of a Container registering a Behavior in order to intercept
 * and decorate its child controls:
 *
 * <pre class="prettyprint">
 * public class MyContainer extends AbstractContainer {
 *
 *     public void onInit() {
 *         Behavior controlInterceptor = getInterceptor();
 *         ControlRegistry.registerInterceptor(this, controlInterceptor);
 *     }
 *
 *     private Behavior getInterceptor() {
 *         Behavior controlInterceptor = new Behavior() {
 *
 *             // This method is invoked before the controls are rendered to the client
 *             public void preResponse(Control source) {
 *                 // Here we can add a CSS class attribute to each child control
 *                 addCssClassToChildControls();
 *             }
 *
 *             // This method is invoked before the HEAD elements are retrieved for each Control
 *             public void preRenderHeadElements(Control source) {
 *             }
 *
 *             // This method is invoked before the Control onDestroy event
 *             public void preDestroy(Control source) {
 *             }
 *         };
 *         return controlInterceptor;
 *     }
 * } </pre>
 */
public class ControlRegistry {

    // Constants --------------------------------------------------------------

    /** The thread local registry holder. */
    private static final ThreadLocal<ControlRegistry.RegistryStack> THREAD_LOCAL_REGISTRY_STACK =
            new ThreadLocal<>();

    // Variables --------------------------------------------------------------

    /** The set of Ajax target controls. */
    Set<Control> ajaxTargetControls;

    /** The list of registered interceptors. */
    List<ControlRegistry.InterceptorHolder> interceptors;

    /** The application log service. */
    LogService logger;

    // Constructors -----------------------------------------------------------

    /**
     * Construct the ControlRegistry with the given ConfigService.
     *
     * @param configService the click application configuration service
     */
    public ControlRegistry(ConfigService configService) {
        this.logger = configService.getLogService();
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Return the thread local ControlRegistry instance.
     *
     * @return the thread local ControlRegistry instance.
     * @throws RuntimeException if a ControlRegistry is not available on the
     * thread
     */
    public static ControlRegistry getThreadLocalRegistry() {
        return getRegistryStack().peek();
    }

    /**
     * Returns true if a ControlRegistry instance is available on the current
     * thread, false otherwise.
     * <p/>
     * Unlike {@link #getThreadLocalRegistry()} this method can safely be used
     * and will not throw an exception if a ControlRegistry is not available on
     * the current thread.
     *
     * @return true if an ControlRegistry instance is available on the
     * current thread, false otherwise
     */
    public static boolean hasThreadLocalRegistry() {
        ControlRegistry.RegistryStack registryStack = THREAD_LOCAL_REGISTRY_STACK.get();
        if (registryStack == null) {
            return false;
        }
        return !registryStack.isEmpty();
    }

    /**
     * Register the control to be processed by the Click runtime if the control
     * is the Ajax target. A control is an Ajax target if the
     * {@link Control#isAjaxTarget(Context)} method returns true.
     * Once a target control is identified, Click invokes its
     * {@link Control#onProcess()} method.
     * <p/>
     * This method serves a dual purpose as all controls registered here
     * will also have their Behaviors (if any) processed. Processing
     * {@link Behavior Behaviors}
     * means their interceptor methods will be invoked during the request
     * life cycle, passing the control as the argument.
     *
     * @param control the control to register as an Ajax target
     */
    public static void registerAjaxTarget(Control control) {
        if (control == null) {
            throw new IllegalArgumentException("control cannot be null");
        }

        ControlRegistry instance = getThreadLocalRegistry();
        instance.internalRegisterAjaxTarget(control);
    }

    /**
     * Register a control event interceptor for the given Control and Behavior.
     * The control will be passed as the source control to the Behavior
     * interceptor methods:
     * {@link org.apache.click.Behavior#preRenderHeadElements(org.apache.click.Control) preRenderHeadElements(Control)},
     * {@link org.apache.click.Behavior#preResponse(org.apache.click.Control) preResponse(Control)} and
     * {@link org.apache.click.Behavior#preDestroy(org.apache.click.Control) preDestroy(Control)}.
     *
     * @param control the interceptor source control
     * @param controlInterceptor the control interceptor to register
     */
    public static void registerInterceptor(Control control, Behavior controlInterceptor) {
        if (control == null) {
            throw new IllegalArgumentException("control cannot be null");
        }
        if (controlInterceptor == null) {
            throw new IllegalArgumentException("control interceptor cannot be null");
        }

        ControlRegistry instance = getThreadLocalRegistry();
        instance.internalRegisterInterceptor(control, controlInterceptor);
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Allow the registry to handle the error that occurred.
     *
     * @param throwable the error which occurred during processing
     */
    protected void errorOccurred(Throwable throwable) {
        clear();
    }

    // Package Private Methods ------------------------------------------------

    /**
     * Remove all interceptors and ajax target controls from this registry.
     */
    void clear() {
        if (hasInterceptors()) {
            getInterceptors().clear();
        }

        if (hasAjaxTargetControls()) {
            getAjaxTargetControls().clear();
        }
    }

    /**
     * Register the AJAX target control.
     *
     * @param control the AJAX target control
     */
    void internalRegisterAjaxTarget(Control control) {
        Validate.notNull(control, "Null control parameter");
        getAjaxTargetControls().add(control);
    }

    /**
     * Register the source control and associated interceptor.
     *
     * @param source the interceptor source control
     * @param controlInterceptor the control interceptor to register
     */
    void internalRegisterInterceptor(Control source, Behavior controlInterceptor) {
        Validate.notNull(source, "Null source parameter");
        Validate.notNull(controlInterceptor, "Null interceptor parameter");

        ControlRegistry.InterceptorHolder interceptorHolder = new ControlRegistry.InterceptorHolder(source, controlInterceptor);

        // Guard against adding duplicate interceptors
        List<ControlRegistry.InterceptorHolder> localInterceptors = getInterceptors();
        if (!localInterceptors.contains(interceptorHolder)) {
            localInterceptors.add(interceptorHolder);
        }
    }

    void processPreResponse(Context context) {
        if (hasAjaxTargetControls()) {
            for (Control control : getAjaxTargetControls()) {
                for (Behavior behavior : control.getBehaviors()) {
                    behavior.preResponse(control);
                }
            }
        }

        if (hasInterceptors()) {
            for (ControlRegistry.InterceptorHolder interceptorHolder : getInterceptors()) {
                Behavior interceptor = interceptorHolder.getInterceptor();
                Control control = interceptorHolder.getControl();
                interceptor.preResponse(control);
            }
        }
    }

    void processPreRenderHeadElements(Context context) {
        if (hasAjaxTargetControls()) {
            for (Control control : getAjaxTargetControls()) {
                for (Behavior behavior : control.getBehaviors()) {
                    behavior.preRenderHeadElements(control);
                }
            }
        }

        if (hasInterceptors()) {
            for (ControlRegistry.InterceptorHolder interceptorHolder : getInterceptors()) {
                Behavior interceptor = interceptorHolder.getInterceptor();
                Control control = interceptorHolder.getControl();
                interceptor.preRenderHeadElements(control);
            }
        }
    }

    void processPreDestroy(Context context) {
        if (hasAjaxTargetControls()) {
            for (Control control : getAjaxTargetControls()) {
                for (Behavior behavior : control.getBehaviors()) {
                    behavior.preDestroy(control);
                }
            }
        }

        if (hasInterceptors()) {
            for (ControlRegistry.InterceptorHolder interceptorHolder : getInterceptors()) {
                Behavior interceptor = interceptorHolder.getInterceptor();
                Control control = interceptorHolder.getControl();
                interceptor.preDestroy(control);
            }
        }
    }

    /**
     * Checks if any AJAX target control have been registered.
     */
    boolean hasAjaxTargetControls() {
        if (ajaxTargetControls == null || ajaxTargetControls.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Return the set of potential Ajax target controls.
     *
     * @return the set of potential Ajax target controls
     */
    Set<Control> getAjaxTargetControls() {
        if (ajaxTargetControls == null) {
            ajaxTargetControls = new LinkedHashSet<Control>();
        }
        return ajaxTargetControls;
    }

    /**
     * Checks if any control interceptors have been registered.
     */
    boolean hasInterceptors() {
        if (interceptors == null || interceptors.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Return the set of registered control interceptors.
     *
     * @return set of registered interceptors
     */
    List<ControlRegistry.InterceptorHolder> getInterceptors() {
        if (interceptors == null) {
            interceptors = new ArrayList<>();
        }
        return interceptors;
    }

    /**
     * Adds the specified ControlRegistry on top of the registry stack.
     *
     * @param controlRegistry the ControlRegistry to add
     */
    static void pushThreadLocalRegistry(ControlRegistry controlRegistry) {
        getRegistryStack().push(controlRegistry);
    }

    /**
     * Remove and return the controlRegistry instance on top of the
     * registry stack.
     *
     * @return the controlRegistry instance on top of the registry stack
     */
    static ControlRegistry popThreadLocalRegistry() {
        ControlRegistry.RegistryStack registryStack = getRegistryStack();
        ControlRegistry controlRegistry = registryStack.pop();

        if (registryStack.isEmpty()) {
            THREAD_LOCAL_REGISTRY_STACK.set(null);
        }

        return controlRegistry;
    }

    static ControlRegistry.RegistryStack getRegistryStack() {
        ControlRegistry.RegistryStack registryStack = THREAD_LOCAL_REGISTRY_STACK.get();

        if (registryStack == null) {
            registryStack = new ControlRegistry.RegistryStack(2);
            THREAD_LOCAL_REGISTRY_STACK.set(registryStack);
        }

        return registryStack;
    }

    /**
     * Provides an unsynchronized Stack.
     */
    static class RegistryStack extends ArrayList<ControlRegistry> {

        /** Serialization version indicator. */
        private static final long serialVersionUID = 1L;

        /**
         * Create a new RegistryStack with the given initial capacity.
         *
         * @param initialCapacity specify initial capacity of this stack
         */
        private RegistryStack(int initialCapacity) {
            super(initialCapacity);
        }

        /**
         * Pushes the ControlRegistry onto the top of this stack.
         *
         * @param controlRegistry the ControlRegistry to push onto this stack
         * @return the ControlRegistry pushed on this stack
         */
        private ControlRegistry push(ControlRegistry controlRegistry) {
            add(controlRegistry);

            return controlRegistry;
        }

        /**
         * Removes and return the ControlRegistry at the top of this stack.
         *
         * @return the ControlRegistry at the top of this stack
         */
        private ControlRegistry pop() {
            ControlRegistry controlRegistry = peek();

            remove(size() - 1);

            return controlRegistry;
        }

        /**
         * Looks at the ControlRegistry at the top of this stack without
         * removing it.
         *
         * @return the ControlRegistry at the top of this stack
         */
        private ControlRegistry peek() {
            int length = size();

            if (length == 0) {
                String msg = "No ControlRegistry available on ThreadLocal Registry Stack";
                throw new RuntimeException(msg);
            }

            return get(length - 1);
        }
    }

    static class InterceptorHolder {

        private Behavior interceptor;

        private Control control;

        public InterceptorHolder(Control control, Behavior interceptor) {
            this.control = control;
            this.interceptor = interceptor;
        }

        public Behavior getInterceptor() {
            return interceptor;
        }

        public void setInterceptor(Behavior interceptor) {
            this.interceptor = interceptor;
        }

        public Control getControl() {
            return control;
        }

        public void setControl(Control control) {
            this.control = control;
        }

        /**
         * @see Object#equals(java.lang.Object)
         *
         * @param o the reference object with which to compare
         * @return true if this object equals the given object
         */
        @Override
        public boolean equals(Object o) {

            //1. Use the == operator to check if the argument is a reference to this object.
            if (o == this) {
                return true;
            }

            //2. Use the instanceof operator to check if the argument is of the correct type.
            if (!(o instanceof ControlRegistry.InterceptorHolder)) {
                return false;
            }

            //3. Cast the argument to the correct type.
            ControlRegistry.InterceptorHolder that = (ControlRegistry.InterceptorHolder) o;

            boolean equals = this.control == null ? that.control == null : this.control.equals(that.control);
            if (!equals) {
                return false;
            }

            return this.interceptor == null ? that.interceptor == null : this.interceptor.equals(that.interceptor);
        }

        /**
         * @see java.lang.Object#hashCode()
         *
         * @return the InterceptorHolder hashCode
         */
        @Override
        public int hashCode() {
            int result = 17;
            result = 37 * result + (control == null ? 0 : control.hashCode());
            result = 37 * result + (interceptor == null ? 0 : interceptor.hashCode());
            return result;
        }
    }
}
