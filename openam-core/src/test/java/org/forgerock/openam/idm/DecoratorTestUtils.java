/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.idm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for testing decorator implementations.
 *
 * @since 12.0.0
 */
public final class DecoratorTestUtils {

    private DecoratorTestUtils() { }

    /**
     * Generates some dummy arguments for the given method. Fills in appropriate default values for each argument that
     * the method expects: {@code null} for any object references, {@code false} for primitive booleans, {@code 0} for
     * any primitive numeric types, and {@code ' '} for any chars.
     *
     * @param method the method to generate arguments for.
     * @return an appropriate set of arguments to allow the method to be called.
     */
    public static Object[] generateArguments(Method method) {
        Object[] args = new Object[method.getParameterTypes().length];
        int i = 0;
        for (Class<?> type : method.getParameterTypes()) {
            if (type == Boolean.TYPE) {
                args[i++] = false;
            } else if (type == Integer.TYPE || type == Long.TYPE || type == Short.TYPE || type == Byte.TYPE) {
                args[i++] = 0;
            } else if (type == Double.TYPE || type == Float.TYPE) {
                args[i++] = 0.0f;
            } else if (type == Character.TYPE) {
                args[i++] = ' ';
            } else {
                args[i++] = null;
            }
        }
        return args;
    }

    /**
     * Returns the declared methods on the given interface in a format suitable for use as a TestNG data provider.
     *
     * @param interfaceClass the interface to get declared methods for.
     * @return an iterator of object arrays, each of which contains a single element which is a method from the
     * interface.
     */
    public static Iterator<Object[]> getDeclaredMethods(Class<?> interfaceClass) {
        List<Object[]> result = new ArrayList<Object[]>();
        for (Method method : interfaceClass.getMethods()) {
            result.add(new Object[] { method });
        }
        return result.iterator();
    }
}
