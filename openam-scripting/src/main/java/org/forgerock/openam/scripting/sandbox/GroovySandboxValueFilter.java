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

import groovy.lang.Script;
import org.forgerock.util.Reject;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;
import org.mozilla.javascript.ClassShutter;

/**
 * Applies a sandbox to Groovy script execution. Delegates to a Rhino {@link org.mozilla.javascript.ClassShutter} for
 * the actual allow/deny decision for consistency with Rhino Javascript.
 */
public final class GroovySandboxValueFilter extends GroovyValueFilter {
    private static final String ERROR_MESSAGE = "Access to Java class \"%s\" is prohibited.";

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
        final Class<?> clazz = target instanceof Class ? (Class<?>) target : target.getClass();
        final String className = clazz.getName();

        if (classShutter.visibleToScripts(className)) {
            return target;
        } else {
            throw new SecurityException(String.format(ERROR_MESSAGE, className));
        }
    }

    @Override
    public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
        if (receiver instanceof Script) {
            // Always allow the script to set/get its own properties as this is how global variables are implemented
            return filterReturnValue(invoker.call(receiver, property));
        } else {
            return super.onGetProperty(invoker, receiver, property);
        }
    }

    @Override
    public Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
        if (receiver instanceof Script) {
            // Always allow the script to set/get its own properties as this is how global variables are implemented
            return filterReturnValue(invoker.call(receiver, property, value));
        } else {
            return super.onSetProperty(invoker, receiver, property, value);
        }
    }
}
