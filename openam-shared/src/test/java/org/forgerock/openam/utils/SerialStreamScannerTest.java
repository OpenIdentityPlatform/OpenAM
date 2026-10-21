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

package org.forgerock.openam.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the linear stream pre-scan in {@link SerialStreamScanner}. All fixtures are harmless shapes that pin
 * the rules the back-reference bound relies on: a back-reference weighs as much as a copy of its target, and a
 * cycle adds nothing. Every rejection test is also confirmed to go red against a mutant that disables the
 * corresponding rule.
 */
public class SerialStreamScannerTest {

    // Large enough not to interfere with any test below; the array-length rule is exercised through IOUtilsTest.
    private static final long MAX_ARRAY = 100000L;
    private static final long GENEROUS_EXPANDED = 1_000_000L;
    private static final long WEAK_HASH_KEYS = IOUtils.DeserialisationLimits.DEFAULT_MAX_WEAK_HASH_KEYS;

    private static byte[] serialise(Object o) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        return bytes.toByteArray();
    }

    @Test
    public void shouldAcceptAListThatReferencesOneSmallSublistManyTimes() throws Exception {
        // A DAG: one small sublist referenced 1000 times. Its expanded tree size counts the sublist once per
        // reference, which is well under the generous limit, so it is accepted.
        final List<Object> sub = new ArrayList<>();
        sub.add("x");
        final List<Object> root = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            root.add(sub);
        }
        final byte[] bytes = serialise(root);

        SerialStreamScanner.scan(bytes, MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*expands to.*")
    public void shouldRejectASharedGraphUnderASmallExpandedRefsLimit() throws Exception {
        // A sublist of 10 strings referenced 20 times: 30 edges, which is under the limit of 100, but each
        // back-reference to the sublist counts the whole sublist again, so the expanded size is 221. A mutant that
        // drops the back-reference edge would count the sublist only once (expanded size 12) and wrongly accept,
        // so this pins the back-reference accounting.
        final List<Object> sub = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sub.add("x" + i);
        }
        final List<Object> root = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            root.add(sub);
        }
        final byte[] bytes = serialise(root);

        SerialStreamScanner.scan(bytes, MAX_ARRAY, 100L, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*references in the stream.*")
    public void shouldCapTheRecordedReferencesAtTheExpandedRefsLimit() throws Exception {
        // 200 back-references to one string: 201 nodes would be well within reach of the expanded-size check, but
        // the scanner must stop recording edges as soon as they pass the limit, so that its own memory is bounded
        // by the limit rather than by the size of the stream. A mutant without the edge cap fails with the
        // expanded-size message instead.
        final String shared = "shared";
        final List<Object> root = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            root.add(shared);
        }

        SerialStreamScanner.scan(serialise(root), MAX_ARRAY, 100L, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldCountBackReferencedMapValuesAsFullCopies() throws Exception {
        // HashMap writes its keys and values in its writeObject annotation. A value that is a back-reference to
        // one shared list must weigh exactly as much as a separate copy of that list per key, or a shared
        // sub-graph would be cheaper to the scanner than it is to hashCode()/equals().
        final List<Object> shared = listOf("x", "y");
        final Map<String, Object> sharedValues = new HashMap<>();
        final Map<String, Object> copiedValues = new HashMap<>();
        for (String key : new String[] {"a", "b", "c"}) {
            sharedValues.put(key, shared);
            copiedValues.put(key, listOf("x", "y"));
        }

        assertThat(SerialStreamScanner.expandedNodeCount(serialise(sharedValues), MAX_ARRAY))
                .isEqualTo(SerialStreamScanner.expandedNodeCount(serialise(copiedValues), MAX_ARRAY));
    }

    @Test
    public void shouldCountBackReferencedSetElementsAsFullCopies() throws Exception {
        // The same invariant for HashSet elements: distinct elements that each hold a back-reference to one
        // shared list weigh as much as elements that each hold their own copy.
        final List<Object> shared = listOf("x", "y");
        final Set<Object> sharedElements = new HashSet<>();
        final Set<Object> copiedElements = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            sharedElements.add(listOf(String.valueOf(i), shared));
            copiedElements.add(listOf(String.valueOf(i), listOf("x", "y")));
        }

        assertThat(SerialStreamScanner.expandedNodeCount(serialise(sharedElements), MAX_ARRAY))
                .isEqualTo(SerialStreamScanner.expandedNodeCount(serialise(copiedElements), MAX_ARRAY));
    }

    @Test
    public void shouldNotExpandAReferenceCycle() throws Exception {
        // An edge back to an ancestor that is still being expanded adds nothing, so a self-referencing list
        // costs no more than the same list without the cycle and is accepted under a tight limit.
        final List<Object> cyclic = listOf("x", "y");
        cyclic.add(cyclic);
        final List<Object> acyclic = listOf("x", "y");

        final long cyclicCount = SerialStreamScanner.expandedNodeCount(serialise(cyclic), MAX_ARRAY);
        assertThat(cyclicCount).isEqualTo(SerialStreamScanner.expandedNodeCount(serialise(acyclic), MAX_ARRAY));
        SerialStreamScanner.scan(serialise(cyclic), MAX_ARRAY, cyclicCount, WEAK_HASH_KEYS);
    }

    private static List<Object> listOf(Object... elements) {
        final List<Object> list = new ArrayList<>();
        for (Object element : elements) {
            list.add(element);
        }
        return list;
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*externalizable.*")
    public void shouldRejectExternalizableClass() throws Exception {
        // An Externalizable class has full control of its stream, which the scanner cannot follow, so it is
        // rejected fail-closed. A mutant that removes the SC_EXTERNALIZABLE check would accept it.
        final byte[] bytes = serialise(new AnExternalizable());

        SerialStreamScanner.scan(bytes, MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*truncated.*")
    public void shouldRejectTruncatedStream() throws Exception {
        final byte[] full = serialise(smallConsoleShape());
        final byte[] truncated = new byte[full.length - 3];
        System.arraycopy(full, 0, truncated, 0, truncated.length);

        SerialStreamScanner.scan(truncated, MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*trailing.*")
    public void shouldRejectGarbageAfterRoot() throws Exception {
        final byte[] full = serialise(smallConsoleShape());
        final byte[] withTrailer = new byte[full.length + 1];
        System.arraycopy(full, 0, withTrailer, 0, full.length);
        withTrailer[full.length] = (byte) 0x70; // a stray TC_NULL after the root object

        SerialStreamScanner.scan(withTrailer, MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*nested deeper.*")
    public void shouldRejectALongChainOfSuperclassDescriptors() throws Exception {
        // One object whose class descriptor has 2000 new superclass descriptors, each 15 bytes. The chain must
        // count towards the parse depth: 20000 links (300 KB) otherwise overflowed the call stack with a
        // StackOverflowError instead of an IOException.
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(bytes);
        out.writeShort(ObjectStreamConstants.STREAM_MAGIC);
        out.writeShort(ObjectStreamConstants.STREAM_VERSION);
        out.writeByte(ObjectStreamConstants.TC_OBJECT);
        for (int i = 0; i < 2000; i++) {
            out.writeByte(ObjectStreamConstants.TC_CLASSDESC);
            out.writeUTF("A");
            out.writeLong(1L); // serialVersionUID
            out.writeByte(ObjectStreamConstants.SC_SERIALIZABLE);
            out.writeShort(0); // no fields
            out.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA); // empty classAnnotation
        }
        out.writeByte(ObjectStreamConstants.TC_NULL); // the last superclass
        out.flush();

        SerialStreamScanner.scan(bytes.toByteArray(), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*nested deeper.*")
    public void shouldRejectNestingThatWouldExhaustTheCallStack() throws Exception {
        // 900 object arrays, each holding the next: 9 KB. Nested objects cost the parser about 1 KiB of stack per
        // level, so a depth guard near 1000 levels let such a stream overflow the default 1 MiB thread stack.
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(bytes);
        out.writeShort(ObjectStreamConstants.STREAM_MAGIC);
        out.writeShort(ObjectStreamConstants.STREAM_VERSION);
        out.writeByte(ObjectStreamConstants.TC_ARRAY);
        out.writeByte(ObjectStreamConstants.TC_CLASSDESC);
        out.writeUTF("[Ljava.lang.Object;");
        out.writeLong(1L); // serialVersionUID
        out.writeByte(ObjectStreamConstants.SC_SERIALIZABLE);
        out.writeShort(0); // no fields
        out.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA); // empty classAnnotation
        out.writeByte(ObjectStreamConstants.TC_NULL); // no superclass
        out.writeInt(1);
        for (int i = 1; i < 900; i++) {
            out.writeByte(ObjectStreamConstants.TC_ARRAY);
            out.writeByte(ObjectStreamConstants.TC_REFERENCE);
            out.writeInt(ObjectStreamConstants.baseWireHandle); // the array class descriptor above
            out.writeInt(1);
        }
        out.writeByte(ObjectStreamConstants.TC_NULL); // the innermost element
        out.flush();

        SerialStreamScanner.scan(bytes.toByteArray(), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*malformed.*")
    public void shouldRejectAMalformedClassName() throws Exception {
        // A four-byte UTF-8 lead byte is not modified UTF-8, which ObjectInputStream rejects as well.
        final byte[] bytes = serialise(new ArrayList<>());
        final int at = indexOfName(bytes, "java.util.ArrayList");
        bytes[at + 2] = (byte) 0xF0;

        SerialStreamScanner.scan(bytes, MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldAcceptALeadingReset() throws Exception {
        // Splice a TC_RESET between the stream header and the root object; the scanner must skip it and accept.
        final byte[] full = serialise(smallConsoleShape());
        final byte[] withReset = new byte[full.length + 1];
        System.arraycopy(full, 0, withReset, 0, 4); // magic + version
        withReset[4] = (byte) 0x79; // TC_RESET
        System.arraycopy(full, 4, withReset, 5, full.length - 4);

        SerialStreamScanner.scan(withReset, MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldAcceptAnEnum() throws Exception {
        SerialStreamScanner.scan(serialise(TimeUnit.SECONDS), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldAcceptALongString() throws Exception {
        final char[] chars = new char[70000]; // over the 65535 boundary, so a TC_LONGSTRING is written
        java.util.Arrays.fill(chars, 'a');

        SerialStreamScanner.scan(serialise(new String(chars)), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldAcceptPrimitiveArrays() throws Exception {
        SerialStreamScanner.scan(serialise(new int[] { 1, 2, 3 }), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
        SerialStreamScanner.scan(serialise(new byte[] { 4, 5, 6 }), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
        SerialStreamScanner.scan(serialise(new long[] { 7L, 8L }), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
        SerialStreamScanner.scan(serialise(new double[] { 1.0, 2.0 }), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*array length.*")
    public void shouldRejectPrimitiveArrayOverTheArrayLimit() throws Exception {
        SerialStreamScanner.scan(serialise(new int[50]), 10L, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    // ---- Keys of hash collections --------------------------------------------------------------------------

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*ArrayList is used as a key.*")
    public void shouldRejectACollectionUsedAsAHashSetElement() throws Exception {
        final Set<Object> set = new HashSet<>();
        set.add(new ArrayList<>(Collections.singletonList("a")));

        SerialStreamScanner.scan(serialise(set), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*HashSet is used as a key.*")
    public void shouldRejectACollectionUsedAsAHashMapKey() throws Exception {
        final Map<Object, Object> map = new HashMap<>();
        map.put(new HashSet<>(Collections.singletonList("a")), "value");

        SerialStreamScanner.scan(serialise(map), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldAcceptACollectionAsAHashMapValue() throws Exception {
        // Only keys are hashed while a HashMap is rebuilt, so collections and unordered classes are fine as values.
        final Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            map.put("k" + i, i % 2 == 0 ? new HashSet<>(Collections.singletonList("v" + i)) : new Locale("x" + i));
        }

        SerialStreamScanner.scan(serialise(map), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldAcceptManyKeysOfOneOrderedClass() throws Exception {
        final Set<Object> strings = new HashSet<>();
        final Map<Object, Object> integers = new HashMap<>();
        final Set<Object> caseInsensitive = new CaseInsensitiveHashSet();
        final Set<Object> enums = new HashSet<>(Arrays.asList(Character.UnicodeScript.values()));
        for (int i = 0; i < 1000; i++) {
            strings.add("s" + i);
            integers.put(i, "v");
            caseInsensitive.add("c" + i);
        }
        assertThat(enums.size()).isGreaterThan((int) WEAK_HASH_KEYS);

        SerialStreamScanner.scan(serialise(strings), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
        SerialStreamScanner.scan(serialise(integers), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
        SerialStreamScanner.scan(serialise(caseInsensitive), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
        SerialStreamScanner.scan(serialise(enums), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldAcceptUnorderedKeysUpToTheLimit() throws Exception {
        // The shape of the Accept-Language locales of a remote authentication request, at the limit.
        SerialStreamScanner.scan(serialise(locales((int) WEAK_HASH_KEYS)), MAX_ARRAY, GENEROUS_EXPANDED,
                WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*not all of one ordered.*")
    public void shouldRejectUnorderedKeysOverTheLimit() throws Exception {
        SerialStreamScanner.scan(serialise(locales((int) WEAK_HASH_KEYS + 1)), MAX_ARRAY, GENEROUS_EXPANDED,
                WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*not all of one ordered.*")
    public void shouldCountMixedOrderedKeyClassesAsUnordered() throws Exception {
        // Keys of two different Comparable classes cannot be compared with each other, so a mix of them is
        // treated like unordered keys, however each class orders on its own.
        final Set<Object> mixed = new HashSet<>();
        for (int i = 0; i < WEAK_HASH_KEYS; i++) {
            mixed.add("s" + i);
            mixed.add(i);
        }

        SerialStreamScanner.scan(serialise(mixed), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*not all of one ordered.*")
    public void shouldCheckTheKeysOfAHashSetReachedThroughABackReference() throws Exception {
        // The second copy of the set is a back-reference and is not read again; the rule has to hold on the
        // first one, which the stream reads in full.
        final Set<Object> set = locales((int) WEAK_HASH_KEYS + 1);
        final List<Object> root = new ArrayList<>();
        root.add(set);
        root.add(set);

        SerialStreamScanner.scan(serialise(root), MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*ArrayList is used as a key.*")
    public void shouldRejectACollectionKeyOfAHashSetWhoseNameIsOverlong() throws Exception {
        final Set<Object> set = new HashSet<>();
        set.add(new ArrayList<>(Collections.singletonList("a")));
        final byte[] bytes = withOverlongCharacter(serialise(set), "java.util.HashSet", 'S');

        SerialStreamScanner.scan(bytes, MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*not all of one ordered.*")
    public void shouldCheckTheKeysOfAHashSetWhoseNameIsOverlong() throws Exception {
        final byte[] bytes = withOverlongCharacter(serialise(locales((int) WEAK_HASH_KEYS + 1)),
                "java.util.HashSet", 'S');

        SerialStreamScanner.scan(bytes, MAX_ARRAY, GENEROUS_EXPANDED, WEAK_HASH_KEYS);
    }

    @Test
    public void shouldResolveAnOverlongClassNameLikeObjectInputStream() throws Exception {
        // The fixture of the two tests above: ObjectInputStream decodes the overlong name as modified UTF-8 and
        // still resolves java.util.HashSet, so the scanner has to see the same name.
        final byte[] bytes = withOverlongCharacter(serialise(locales(2)), "java.util.HashSet", 'S');

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertThat(in.readObject()).isInstanceOf(HashSet.class);
        }
    }

    private static int indexOfName(byte[] bytes, String name) {
        // A class name is written as a two-byte length followed by its (here ASCII) bytes.
        final byte[] entry = new byte[name.length() + 2];
        entry[1] = (byte) name.length();
        System.arraycopy(name.getBytes(StandardCharsets.US_ASCII), 0, entry, 2, name.length());
        outer:
        for (int i = 0; i + entry.length <= bytes.length; i++) {
            for (int j = 0; j < entry.length; j++) {
                if (bytes[i + j] != entry[j]) {
                    continue outer;
                }
            }
            return i;
        }
        throw new AssertionError(name + " is not in the stream");
    }

    // Rewrites the first occurrence of a class name with one of its ASCII characters in the overlong two-byte form
    // 110000xx 10xxxxxx, which modified UTF-8 decoders accept and standard UTF-8 decoders reject.
    private static byte[] withOverlongCharacter(byte[] bytes, String name, char c) {
        final int at = indexOfName(bytes, name);
        final int index = name.indexOf(c);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bytes, 0, at);
        out.write(0);
        out.write(name.length() + 1);
        out.write(bytes, at + 2, index);
        out.write(0xC0 | (c >> 6));
        out.write(0x80 | (c & 0x3F));
        out.write(bytes, at + 2 + index + 1, bytes.length - (at + 2 + index + 1));
        return out.toByteArray();
    }

    private static Set<Object> locales(int count) {
        final Set<Object> locales = new HashSet<>();
        for (int i = 0; i < count; i++) {
            locales.add(new Locale("l" + i));
        }
        return locales;
    }

    // ---- Measurements that justify the default maxexpandedrefs ----------------------------------------------

    @Test
    public void shouldMeasureExpandedSizeOfLegitimatePayloadShapes() throws Exception {
        // Console page-session shape: page-session map -> attribute-value map -> value set.
        final long console = SerialStreamScanner.expandedNodeCount(serialise(smallConsoleShape()), MAX_ARRAY);

        // A wide remote-request-like header map: 50 case-insensitive header names, each mapping to a small set of
        // values. This stands in for the RemoteHttpServletRequest header maps (which live in openam-core).
        final Map<Object, Object> headers = new CaseInsensitiveHashMap();
        for (int i = 0; i < 50; i++) {
            final Set<Object> values = new CaseInsensitiveHashSet();
            values.add("value-" + i + "-a");
            values.add("value-" + i + "-b");
            headers.put("Header-" + i, values);
        }
        final long remoteRequest = SerialStreamScanner.expandedNodeCount(serialise(headers), MAX_ARRAY);

        // The IOUtilsTest.testDeserializeValid fixture: a HashMap of mixed scalars, collections and arrays.
        final long fixture = SerialStreamScanner.expandedNodeCount(serialise(ioUtilsTestFixture()), MAX_ARRAY);

        System.out.println("[maxexpandedrefs measurement] console page-state=" + console
                + " remote-request-like=" + remoteRequest + " IOUtilsTest fixture=" + fixture);

        // Every legitimate shape is orders of magnitude below the default of 100000, leaving generous headroom.
        assertThat(console).isLessThan(1000L);
        assertThat(remoteRequest).isLessThan(2000L);
        assertThat(fixture).isLessThan(1000L);
    }

    private static Object smallConsoleShape() {
        final Map<String, Object> pageSession = new HashMap<>();
        final Map<String, Object> attributeValues = new HashMap<>();
        final Set<String> values = new HashSet<>();
        values.add("value");
        attributeValues.put("attribute", values);
        pageSession.put("propertyAttributes", attributeValues);
        pageSession.put("name", "aView");
        return pageSession;
    }

    private static Object ioUtilsTestFixture() {
        final Map<Object, Object> validCollection = new HashMap<>();
        validCollection.put("key1", "value1");
        final Set<String> value3 = new HashSet<>();
        value3.add("value3.1");
        value3.add("value3.2");
        validCollection.put("key3", value3);
        final List<String> value4 = new ArrayList<>();
        value4.add("value4.1");
        value4.add("value4.2");
        validCollection.put("key4", value4);
        validCollection.put("key7", Integer.valueOf(7));
        validCollection.put("key8", Boolean.TRUE);
        validCollection.put("key9", new String[] { "1", "2" });
        validCollection.put("key14", new int[] { 0, 1 });
        validCollection.put("key19", new int[][] { { 1, 1 }, { 2, 2 } });
        return validCollection;
    }

    /** A harmless Externalizable used only to confirm the scanner rejects the SC_EXTERNALIZABLE flag. */
    public static final class AnExternalizable implements Externalizable {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(42);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            in.readInt();
        }
    }
}
