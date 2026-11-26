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
package com.sun.identity.jaxrpc.golden;

import com.iplanet.am.sdk.remote.AMRemoteException;
import com.iplanet.am.sdk.remote.DirectoryManagerIF;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.jaxrpc.golden.GoldenCase;
import com.sun.identity.shared.jaxrpc.golden.GoldenCases;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.jaxrpc.SMSObjectIF;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * The golden cases of the core endpoint interfaces: the generated cases plus edge cases for
 * result runtime classes, nulls, special characters, omitted arguments and undeclared failures.
 */
public final class CoreGoldenCases {

    private static final String SPECIAL_CHARS_RESULT = "<a&b> \"q\" 'a' / é ✓";
    private static final String SPECIAL_CHARS_ARG = "<m&o> \"q\" / é";

    private CoreGoldenCases() {
    }

    /**
     * @param iface a core endpoint interface.
     * @return {@code GoldenCases.forInterface(iface)} plus the edge cases for iface.
     */
    public static List<GoldenCase> forInterface(Class<?> iface) {
        List<GoldenCase> cases = new ArrayList<>(GoldenCases.forInterface(iface));
        if (iface == SMSObjectIF.class) {
            cases.addAll(smsObjectEdges());
        } else if (iface == DirectoryManagerIF.class) {
            cases.addAll(directoryManagerEdges(cases));
        }
        return cases;
    }

    private static List<GoldenCase> smsObjectEdges() {
        Class<?> i = SMSObjectIF.class;
        Object[] read = {"arg1", "arg2"};
        Object[] subEntries = {"arg1", "arg2", "arg3", 4, Boolean.TRUE, Boolean.TRUE};
        List<GoldenCase> edges = new ArrayList<>();

        Map<String, Object> treeMap = new TreeMap<>();
        treeMap.put("a", set("1"));
        treeMap.put("b", "2");
        edges.add(result(i, "read__resultTreeMap", read, treeMap));
        Map<String, Object> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("b", "2");
        linkedHashMap.put("a", set("1"));
        edges.add(result(i, "read__resultLinkedHashMap", read, linkedHashMap));
        Map<String, Object> caseInsensitiveMap = new CaseInsensitiveHashMap<>();
        caseInsensitiveMap.put("Key", set("v"));
        edges.add(result(i, "read__resultCaseInsensitiveHashMap", read, caseInsensitiveMap));
        edges.add(result(i, "read__resultUnmodifiableMap", read,
                Collections.unmodifiableMap(map("k", set("v")))));
        edges.add(result(i, "read__resultEmptyMap", read, Collections.emptyMap()));
        edges.add(result(i, "read__resultNull", read, null));
        Map<String, Object> nestedValues = new HashMap<>();
        nestedValues.put("s", "str");
        nestedValues.put("set", set("x"));
        nestedValues.put("map", map("in", set("y")));
        nestedValues.put("list", new LinkedList<>(Collections.singletonList("z")));
        nestedValues.put("tree", new TreeSet<>(Collections.singleton("t")));
        edges.add(result(i, "read__resultNestedValues", read, nestedValues));
        Map<String, Object> nestedScalars = new HashMap<>();
        nestedScalars.put("al", new ArrayList<>(Collections.singletonList("a")));
        nestedScalars.put("v", new Vector<>(Collections.singletonList("v")));
        nestedScalars.put("n", null);
        nestedScalars.put("b", Boolean.TRUE);
        nestedScalars.put("i", 7);
        nestedScalars.put("lg", 7L);
        edges.add(result(i, "read__resultNestedScalars", read, nestedScalars));
        // The RI writes a List of another class as ns1:arrayList.
        edges.add(result(i, "read__resultNestedUnmodifiableList", read,
                map("ul", Collections.unmodifiableList(new LinkedList<>(Collections.singletonList("u"))))));

        edges.add(result(i, "subEntries__resultTreeSet", subEntries, new TreeSet<>(Arrays.asList("b", "a"))));
        edges.add(result(i, "subEntries__resultLinkedHashSet", subEntries,
                new LinkedHashSet<>(Arrays.asList("b", "a"))));
        Set<String> orderedSet = new OrderedSet();
        orderedSet.add("b");
        orderedSet.add("a");
        edges.add(result(i, "subEntries__resultOrderedSet", subEntries, orderedSet));
        Set<String> caseInsensitiveSet = new CaseInsensitiveHashSet<>();
        caseInsensitiveSet.add("A");
        edges.add(result(i, "subEntries__resultCaseInsensitiveHashSet", subEntries, caseInsensitiveSet));
        edges.add(result(i, "subEntries__resultUnmodifiableSet", subEntries,
                Collections.unmodifiableSet(set("a"))));
        edges.add(result(i, "subEntries__resultEmptySet", subEntries, Collections.emptySet()));
        edges.add(result(i, "subEntries__resultNull", subEntries, null));

        edges.add(result(i, "getRootSuffix__resultSpecialChars", null, SPECIAL_CHARS_RESULT));
        edges.add(result(i, "getRootSuffix__resultEmpty", null, ""));
        edges.add(result(i, "getRootSuffix__resultNull", null, null));
        edges.add(result(i, "entryExists__resultFalse", read, Boolean.FALSE));

        edges.add(result(i, "read__argNullLast", new Object[] {"arg1", null}, map("rk", set("rv"))));
        edges.add(result(i, "subEntries__argNullMiddle",
                new Object[] {"arg1", null, "arg3", 4, Boolean.TRUE, Boolean.TRUE}, set("r1")));
        // The RI passes 0 for the omitted int and false for the omitted boolean.
        edges.add(result(i, "subEntries__argMissingInt",
                new Object[] {"arg1", "arg2", "arg3", null, Boolean.TRUE, Boolean.TRUE}, set("r1")));
        edges.add(result(i, "subEntries__argMissingBoolean",
                new Object[] {"arg1", "arg2", "arg3", 4, null, Boolean.TRUE}, set("r1")));
        edges.add(result(i, "modify__argSpecialChars", new Object[] {"arg1", "arg2", SPECIAL_CHARS_ARG}, null));
        Map<String, Object> nestedMapArg = new HashMap<>();
        nestedMapArg.put("s", "v");
        nestedMapArg.put("set", set("a"));
        nestedMapArg.put("map", map("in", set("b")));
        nestedMapArg.put("list", new LinkedList<>(Collections.singletonList("c")));
        edges.add(result(i, "create__argNestedMap", new Object[] {"arg1", "arg2", nestedMapArg}, null));
        edges.add(new GoldenCase(i, "create__faultNullMessageArg", "create",
                new Object[] {"arg1", "arg2", map("k", set("v"))}, null,
                new SMSException("amUtilMsgs", "userAlreadyExist", new Object[] {"a1", null}), false));
        edges.add(new GoldenCase(i, "read__throwsRuntimeException", "read", read, null,
                new IllegalStateException("boom"), true));
        edges.add(new GoldenCase(i, "read__throwsRemoteException", "read", read, null,
                new RemoteException("remote"), true));
        edges.add(new GoldenCase(i, "noSuchMethod__unknownOperation", "noSuchMethod", new Object[] {"arg1"},
                null, null, true));
        edges.add(result(i, "modify__argEmptyString", new Object[] {"arg1", "arg2", ""}, null));
        edges.add(result(i, "modify__argWhitespace", new Object[] {"arg1", "arg2", " a\n b "}, null));
        edges.add(result(i, "create__argEmptyMap", new Object[] {"arg1", "arg2", new HashMap<>()}, null));
        // SOAPClient.encodeMap writes the key and no value element.
        edges.add(result(i, "create__argMapNullValue", new Object[] {"arg1", "arg2", map("k", null)}, null));
        edges.add(result(i, "validateServiceAttributes__argEmptySet",
                new Object[] {"arg1", "arg2", new HashSet<>()}, Boolean.TRUE));
        edges.add(result(i, "getRootSuffix__resultWhitespace", null, " a\r\n\tb "));
        // The RI writes a nested Date as enc:dateTime in UTC.
        edges.add(result(i, "read__resultNestedDate", read, map("d", new Date(0))));
        return edges;
    }

    private static List<GoldenCase> directoryManagerEdges(List<GoldenCase> generated) {
        Class<?> i = DirectoryManagerIF.class;
        List<GoldenCase> edges = new ArrayList<>();
        edges.add(new GoldenCase(i, "create_idrepo__throwsSubclass", "create_idrepo",
                argumentsOf(generated, "create_idrepo"), null,
                IdRepoDuplicateObjectException.nameAlreadyExists("golden"), false));
        edges.add(result(i, "getGroupFilterAndScope__resultNull",
                argumentsOf(generated, "getGroupFilterAndScope"), null));
        edges.add(new GoldenCase(i, "getGroupFilterAndScope__faultAMRemoteExceptionNullArg",
                "getGroupFilterAndScope", argumentsOf(generated, "getGroupFilterAndScope"), null,
                new AMRemoteException("golden-message", "golden-code", 32, new String[] {"a1", null}), false));
        // DirectoryManagerImpl.getAttributesByteValues1/2 return the byte[][] values of
        // getAttributesByteValues unchanged; the RI answers a JAXRPC.TIE.04 serialization fault.
        edges.add(new GoldenCase(i, "getAttributesByteValues1__resultByteArrayValues", "getAttributesByteValues1",
                argumentsOf(generated, "getAttributesByteValues1"), map("k", new byte[][] {{1, 2}}), null, true));
        return edges;
    }

    /** The arguments of the generated {@code <operation>__ok} case: {@code GoldenValues.argument} per position. */
    private static Object[] argumentsOf(List<GoldenCase> generated, String operation) {
        for (GoldenCase c : generated) {
            if (c.id().equals(operation + "__ok")) {
                return c.clientArgs().clone();
            }
        }
        throw new IllegalArgumentException("no generated case for " + operation);
    }

    private static GoldenCase result(Class<?> iface, String id, Object[] clientArgs, Object result) {
        return new GoldenCase(iface, id, id.substring(0, id.indexOf("__")), clientArgs, result, null, false);
    }

    private static Set<String> set(String value) {
        return new HashSet<>(Collections.singleton(value));
    }

    private static Map<String, Object> map(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
