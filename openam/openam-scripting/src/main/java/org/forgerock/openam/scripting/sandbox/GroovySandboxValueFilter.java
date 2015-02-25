/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.scripting.sandbox;

import groovy.lang.Closure;
import groovy.lang.Script;
import org.forgerock.util.Reject;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;
import org.mozilla.javascript.ClassShutter;

import java.lang.reflect.Method;

/**
 * Applies a sandbox to Groovy script execution. Delegates to a Rhino {@link org.mozilla.javascript.ClassShutter} for
 * the actual allow/deny decision for consistency with Rhino Javascript.
 */
public final class GroovySandboxValueFilter extends GroovyValueFilter {
    private static final String ERROR_MESSAGE = "Access to Java class \"%s\" is prohibited.";
    private static final String CLOSURE_CALL_METHOD = "call";

    private final ClassShutter classShutter;

    public GroovySandboxValueFilter(final ClassShutter classShutter) {
        Reject.ifNull(classShutter);
        this.classShutter = classShutter;
    }

    /**
     * Filters all objects according to the configured ClassShutter.
     *
     * @param target the object or class that is the target of a scripted operation (method call, constructor,
     *               static method call, property/field access).
     * @return the target if access is allowed.
     * @throws java.lang.SecurityException if access if forbidden by the sandbox.
     */
    @Override
    public Object filter(final Object target) {
        if (target == null) {
            return null;
        }
        // For a static call or constructor then the target will be the class, otherwise it will be an object instance
        Class<?> clazz = target instanceof Class ? (Class<?>) target : target.getClass();

        // OPENAM-4347: Treat array types as their component type for the purposes of sandboxing.
        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }

        final String className = clazz.getName();

        if (classShutter.visibleToScripts(className)) {
            return target;
        } else {
            throw new SecurityException(String.format(ERROR_MESSAGE, className));
        }
    }

    @Override
    public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
        if (receiver instanceof Script || receiver instanceof Closure) {
            // Always allow the script to set/get its own properties as this is how global variables are implemented
            return filterReturnValue(invoker.call(receiver, property));
        } else {
            return super.onGetProperty(invoker, receiver, property);
        }
    }

    @Override
    public Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
        if (receiver instanceof Script || receiver instanceof Closure) {
            // Always allow the script to set/get its own properties as this is how global variables are implemented
            return filterReturnValue(invoker.call(receiver, property, value));
        } else {
            return super.onSetProperty(invoker, receiver, property, value);
        }
    }

    @Override
    public Object onMethodCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
        if (isClosureCall(receiver, method) || isScriptOwnMethodCall(receiver, method)) {
            // OPENAM-4278: Allow calls to closures and methods defined by the script itself. Note: we must be careful
            // here *not* to allow a script to call inherited methods (e.g., Script#evaluate) as these allow bypassing
            // the sandbox
            return doCall(invoker, receiver, method, args);
        }
        return super.onMethodCall(invoker, receiver, method, args);
    }

    /**
     * Determines if this method call is a call to a closure (anonymous method) defined within the script itself.
     *
     * @param receiver the object that is the target of the method call being checked.
     * @param method the method that is being invoked. Note: we only know the name of the method, not the signature.
     * @return true if this is a call to a Groovy closure.
     */
    private boolean isClosureCall(Object receiver, String method) {
        return receiver instanceof Closure && CLOSURE_CALL_METHOD.equals(method);
    }

    /**
     * Determines if this is a method call to a method defined by a Groovy script itself.
     *
     * @param receiver the object that is the target of the method call being checked.
     * @param method the method that is being invoked. Note: we only know the name of the method, not the signature.
     * @return true if this is a call to a method defined by the script itself.
     */
    private boolean isScriptOwnMethodCall(Object receiver, String method) {
        if (!(receiver instanceof Script)) {
            return false;
        }
        final Method[] scriptOwnMethods = receiver.getClass().getDeclaredMethods();
        for (Method declaredMethod : scriptOwnMethods) {
            if (declaredMethod.getName().equals(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs an actual call to the given method on the given receiver object using the given invoker. The arguments
     * and return value are filtered according to the sandbox, but the receiver is not.
     *
     * @param invoker the invoker to use to invoke the method.
     * @param receiver the receiver object to invoke the method on.
     * @param method the method to call.
     * @param args the arguments to the method. Will be filtered by the sandbox.
     * @return the (filtered) result of the method call.
     * @throws Throwable if the method throws an exception or if any of the arguments or result is blocked by the
     * sandbox.
     */
    private Object doCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
        return filterReturnValue(invoker.call(receiver, method, filterArgs(args)));
    }

    /**
     * Copied from super-class as private.
     * @param args the arguments to filter. May not be null.
     * @return the filtered arguments array (updated in place).
     */
    private Object[] filterArgs(Object[] args) {
        for (int i = 0; i < args.length; ++i) {
            args[i] = filterArgument(args[i]);
        }
        return args;
    }
}
