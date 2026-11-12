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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.shared.jaxrpc.golden;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The generated golden cases of an endpoint interface, and the target that plays them.
 */
public final class GoldenCases {

    /** Classpath directory of the recorded files; one subdirectory per interface simple name. */
    public static final String RESOURCE_ROOT = "com/sun/identity/jaxrpc/golden/";

    private GoldenCases() {
    }

    /**
     * Cases for iface, ordered by method name: one {@code <op>__ok} case per method, one
     * {@code <op>__fault<Simple>} case per declared exception other than {@code RemoteException},
     * and one {@code <op>__fault<Simple>NoArg} case per declared exception type with a public no-arg
     * constructor, on the method with the lexicographically smallest name that declares it.
     *
     * @param iface the endpoint interface.
     * @return the generated cases.
     */
    public static List<GoldenCase> forInterface(Class<?> iface) {
        List<Method> methods = new ArrayList<>(Arrays.asList(iface.getMethods()));
        methods.sort(Comparator.comparing(Method::getName));

        Map<Class<?>, String> noArgOwners = new HashMap<>();
        for (Method m : methods) {
            for (Class<?> type : declaredExceptions(m)) {
                if (hasPublicNoArgConstructor(type)) {
                    noArgOwners.putIfAbsent(type, m.getName());
                }
            }
        }

        List<GoldenCase> cases = new ArrayList<>();
        for (Method m : methods) {
            String op = m.getName();
            cases.add(new GoldenCase(iface, op + "__ok", op, arguments(m),
                    GoldenValues.result(m.getReturnType()), null, false));
            for (Class<?> type : declaredExceptions(m)) {
                cases.add(new GoldenCase(iface, op + "__fault" + type.getSimpleName(), op, arguments(m),
                        null, GoldenValues.exception(type), false));
            }
            for (Class<?> type : declaredExceptions(m)) {
                if (op.equals(noArgOwners.get(type))) {
                    cases.add(new GoldenCase(iface, op + "__fault" + type.getSimpleName() + "NoArg", op,
                            arguments(m), null, GoldenValues.exceptionNoArg(type), false));
                }
            }
        }
        return cases;
    }

    /**
     * Proxy of {@code c.iface()}. For {@code c.operation()}: records the args into received, then
     * returns {@code c.result()} or throws {@code c.thrown()}. equals/hashCode/toString behave as for
     * Object. Any other interface method throws AssertionError.
     *
     * @param c the case to play.
     * @param received collects the arguments of each call of {@code c.operation()}; an empty array
     *        for a method without parameters.
     * @return the proxy.
     */
    public static Object target(GoldenCase c, List<Object[]> received) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                default:
                    return c.iface().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                }
            }
            if (method.getName().equals(c.operation())) {
                received.add(args == null ? new Object[0] : args.clone());
                if (c.thrown() != null) {
                    throw c.thrown();
                }
                return c.result();
            }
            throw new AssertionError("golden case " + c + " does not expect a call of " + method.getName());
        };
        return Proxy.newProxyInstance(c.iface().getClassLoader(), new Class<?>[] {c.iface()}, handler);
    }

    /**
     * @param m an endpoint method.
     * @return the generated arguments for m, {@code GoldenValues.argument} per 1-based position.
     */
    private static Object[] arguments(Method m) {
        Class<?>[] types = m.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            args[i] = GoldenValues.argument(types[i], i + 1);
        }
        return args;
    }

    private static List<Class<?>> declaredExceptions(Method m) {
        List<Class<?>> types = new ArrayList<>();
        for (Class<?> type : m.getExceptionTypes()) {
            if (type != RemoteException.class) {
                types.add(type);
            }
        }
        return types;
    }

    private static boolean hasPublicNoArgConstructor(Class<?> type) {
        try {
            type.getConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
