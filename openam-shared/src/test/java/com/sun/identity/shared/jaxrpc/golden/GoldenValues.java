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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The fixed argument, result and exception values of the generated golden cases. Exceptions
 * are built by reflection, so this class has no compile-time reference to core or federation
 * classes.
 */
public final class GoldenValues {

    private static final List<Class<?>[]> CONSTRUCTOR_SHAPES = Arrays.asList(
            new Class<?>[] {String.class, String.class, Object[].class},
            new Class<?>[] {String.class, String.class, int.class, String[].class},
            new Class<?>[] {String.class});

    private GoldenValues() {
    }

    /**
     * @param type a parameter type of an endpoint method.
     * @param position the 1-based parameter position.
     * @return the value the generated cases pass at that position.
     */
    public static Object argument(Class<?> type, int position) {
        if (type == String.class) {
            return "arg" + position;
        } else if (type == int.class) {
            return position;
        } else if (type == boolean.class) {
            return Boolean.TRUE;
        } else if (type == Set.class) {
            return new HashSet<>(Collections.singleton("set" + position));
        } else if (type == Map.class) {
            Map<String, Object> map = new HashMap<>();
            map.put("key" + position, new HashSet<>(Collections.singleton("val" + position)));
            return map;
        } else if (type == List.class || type == LinkedList.class) {
            return new LinkedList<>(Collections.singleton("list" + position));
        } else if (type == String[].class) {
            // SOAPClient cannot send a String[].
            return null;
        }
        throw new IllegalArgumentException("no golden argument for " + type.getName());
    }

    /**
     * @param returnType the declared return type of an endpoint method.
     * @return the value the target returns in an {@code __ok} case.
     */
    public static Object result(Class<?> returnType) {
        if (returnType == void.class) {
            return null;
        } else if (returnType == String.class) {
            return "result";
        } else if (returnType == boolean.class) {
            return Boolean.TRUE;
        } else if (returnType == int.class) {
            return 42;
        } else if (returnType == Set.class) {
            return new HashSet<>(Collections.singleton("r1"));
        } else if (returnType == Map.class) {
            Map<String, Object> map = new HashMap<>();
            map.put("rk", new HashSet<>(Collections.singleton("rv")));
            return map;
        } else if (returnType == LinkedList.class) {
            return new LinkedList<>(Arrays.asList("l1", "l2"));
        }
        throw new IllegalArgumentException("no golden result for " + returnType.getName());
    }

    /**
     * @param type a declared exception type.
     * @return an instance built with the first public constructor of the shapes
     *         {@code (String,String,Object[])}, {@code (String,String,int,String[])}, {@code (String)}.
     */
    public static Exception exception(Class<?> type) {
        for (int i = 0; i < CONSTRUCTOR_SHAPES.size(); i++) {
            Constructor<?> constructor;
            try {
                constructor = type.getConstructor(CONSTRUCTOR_SHAPES.get(i));
            } catch (NoSuchMethodException e) {
                continue;
            }
            return newInstance(constructor, constructorArgs(i));
        }
        throw new IllegalArgumentException("no golden constructor shape on " + type.getName());
    }

    private static Object[] constructorArgs(int shape) {
        switch (shape) {
        case 0:
            // amUtilMsgs.properties in openam-shared has this key; bundle-based exceptions resolve it.
            return new Object[] {"amUtilMsgs", "userAlreadyExist", new Object[] {"a1", "a2"}};
        case 1:
            return new Object[] {"golden-message", "golden-code", 32, new String[] {"a1", "a2"}};
        default:
            return new Object[] {"golden-message"};
        }
    }

    /**
     * @param type a declared exception type.
     * @return an instance built with the public no-arg constructor of type: no message, error code
     *         or message args. Defaults the constructor sets stay: {@code SMSException()} carries
     *         resourceBundleName {@code amSDK} and exceptionCode -1.
     */
    public static Exception exceptionNoArg(Class<?> type) {
        try {
            return newInstance(type.getConstructor());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("no public no-arg constructor on " + type.getName(), e);
        }
    }

    /**
     * @param args the arguments a target received.
     * @return {@code [<runtime class name>:<String.valueOf(v)>, null, ...]}.
     */
    public static String describe(Object[] args) {
        if (args == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Object v = args[i];
            if (v == null) {
                sb.append("null");
            } else {
                sb.append(v.getClass().getName()).append(':').append(String.valueOf(v));
            }
        }
        return sb.append(']').toString();
    }

    private static Exception newInstance(Constructor<?> constructor, Object... args) {
        try {
            return (Exception) constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("cannot construct " + constructor, e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot construct " + constructor, e);
        }
    }
}
