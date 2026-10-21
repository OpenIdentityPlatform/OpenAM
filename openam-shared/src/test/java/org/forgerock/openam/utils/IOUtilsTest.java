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
 * Copyright 2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.datastruct.OrderedSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DeflaterOutputStream;

import org.forgerock.openam.utils.IOUtils.DeserialisationLimits;
import org.testng.Assert;
import org.testng.annotations.Test;


public class IOUtilsTest {

    @Test
    public void testDeserializeValid() throws Exception {

        // This assumes that the fallback list is in place
        final Map validCollection = new HashMap();

        validCollection.put("key1", "value1");
        OrderedSet value2 = new OrderedSet();
        value2.add("A");
        value2.add("B");
        value2.add("C");
        value2.add("D");
        validCollection.put("key2", value2);
        Set<String> value3 = new HashSet<>();
        value3.add("value3.1");
        value3.add("value3.2");
        value3.add("value3.3");
        validCollection.put("key3", value3);
        List<String> value4 = new ArrayList<>();
        value4.add("value4.1");
        value4.add("value4.2");
        value4.add("value4.3");
        value4.add("value4.4");
        validCollection.put("key4", value4);
        // A map nested inside the root map (collection nesting 2); its values are scalars so that the collection
        // nesting stays within the default maxcollectiondepth. Nested collections themselves are exercised at
        // level 2 through key2/key3/key4 above.
        Map value5 = new HashMap();
        value5.put("value2", "A");
        value5.put("value3", new Integer(3));
        value5.put("value4", "value4.1");
        validCollection.put("key5", value5);
        validCollection.put("key6", Collections.emptyMap());
        validCollection.put("key7", new Integer(7));
        validCollection.put("key8", new Boolean(true));
        validCollection.put("key9", new String[]{"1", "2"});
        validCollection.put("key10", new Integer[]{1, 2});
        validCollection.put("key11", new byte[]{0, 1});
        validCollection.put("key12", new char[]{0, 1});
        validCollection.put("key13", new short[]{0, 1});
        validCollection.put("key14", new int[]{0, 1});
        validCollection.put("key15", new long[]{0, 1});
        validCollection.put("key16", new float[]{0.0f, 1.0f});
        validCollection.put("key17", new double[]{0.0f, 1.0f});
        validCollection.put("key18", new boolean[]{true, false});
        validCollection.put("key19", new int[][]{{1, 1}, {2, 2}});

        final byte[] bytes = getObjectStreamBytes(validCollection, true);

        // can't use assertEquals here due to the way the primitives are checked for being equal
        Assert.assertNotNull(IOUtils.deserialise(bytes, true));
    }

    @Test(expectedExceptions = IOException.class)
    public void testDeserializeInvalid() throws Exception {

        final Map inValidObject = new HashMap();

        inValidObject.put("key1", new File("/tmp/tmpfile.txt"));

        final byte[] bytes = getObjectStreamBytes(inValidObject, false);

        IOUtils.deserialise(bytes, false);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldRejectDeeplyNestedGraph() throws Exception {
        List<Object> root = new ArrayList<>();
        List<Object> current = root;
        for (int i = 0; i < 50; i++) {
            List<Object> child = new ArrayList<>();
            current.add(child);
            current = child;
        }

        IOUtils.deserialise(getObjectStreamBytes(root, false), false);
    }

    @Test(expectedExceptions = IOException.class, timeOut = 10000)
    public void shouldRejectNestedHashSetsWithExponentialHashCode() throws Exception {
        // Each level is referenced twice by the level above, so hashCode() of the root costs 2^levels
        Set<Object> root = new HashSet<>();
        Set<Object> s1 = root;
        Set<Object> s2 = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Set<Object> t1 = new HashSet<>();
            Set<Object> t2 = new HashSet<>();
            t1.add("foo");
            s1.add(t1);
            s1.add(t2);
            s2.add(t1);
            s2.add(t2);
            s1 = t1;
            s2 = t2;
        }

        IOUtils.deserialise(getObjectStreamBytes(root, false), false);
    }

    @Test
    public void shouldRejectGraphDeeperThanConfiguredMaxDepth() throws Exception {
        // Lift the collection-nesting cap so that only maxdepth can reject the chain.
        List<Object> root = new ArrayList<>();
        List<Object> current = root;
        for (int i = 0; i < 10; i++) {
            List<Object> next = new ArrayList<>();
            current.add(next);
            current = next;
        }
        final byte[] bytes = getObjectStreamBytes(root, false);

        try {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS,
                    "maxdepth=5;maxcollectiondepth=100");
            IOUtils.deserialise(bytes, false);
            Assert.fail("Expected maxdepth=5 to reject a 10-deep graph");
        } catch (IOException expected) {
            // Rejected by the depth limit alone.
        } finally {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "");
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldRejectOversizedArray() throws Exception {
        IOUtils.deserialise(getObjectStreamBytes(new Object[(int) DeserialisationLimits.DEFAULT_MAX_ARRAY + 1], false),
                false);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldRejectTooManyReferences() throws Exception {
        List<String> list = new ArrayList<>();
        for (int i = 0; i <= DeserialisationLimits.DEFAULT_MAX_REFS; i++) {
            list.add(Integer.toString(i));
        }
        // Keep the array below its own limit, so that only the reference count is exceeded
        List<Object> root = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 1000) {
            root.add(new ArrayList<>(list.subList(i, Math.min(i + 1000, list.size()))));
        }

        IOUtils.deserialise(getObjectStreamBytes(root, false), false);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldRejectCompressedStreamInflatingBeyondLimit() throws Exception {
        char[] chars = new char[(int) DeserialisationLimits.DEFAULT_MAX_BYTES + 1];
        Arrays.fill(chars, 'a');
        final byte[] bytes = getObjectStreamBytes(new String(chars), true);
        assertThat(bytes.length).isLessThan(100 * 1024);

        IOUtils.deserialise(bytes, true);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*Dynamic proxy.*")
    public void shouldRejectDynamicProxy() throws Exception {
        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Runnable.class},
                new SerializableHandler());

        IOUtils.deserialise(getObjectStreamBytes(proxy, false), false);
    }

    @Test
    public void shouldParseConfiguredLimits() {
        DeserialisationLimits limits = DeserialisationLimits.parse(
                " maxdepth=30 ; maxbytes=1024;maxrefs=x;maxcollectiondepth=4;foo=1");

        assertThat(limits.maxDepth).isEqualTo(30L);
        assertThat(limits.maxBytes).isEqualTo(1024L);
        assertThat(limits.maxRefs).isEqualTo(DeserialisationLimits.DEFAULT_MAX_REFS);
        assertThat(limits.maxArray).isEqualTo(DeserialisationLimits.DEFAULT_MAX_ARRAY);
        assertThat(limits.maxCollectionDepth).isEqualTo(4L);
    }

    @Test
    public void shouldSetMaxArrayAndIgnoreZeroLimit() {
        // A limit of zero, like a negative or non-numeric one, is invalid and must be ignored (not stored),
        // and a maxarray entry must set maxArray and not another field.
        DeserialisationLimits limits = DeserialisationLimits.parse("maxarray=7;maxrefs=0");

        assertThat(limits.maxArray).isEqualTo(7L);
        assertThat(limits.maxRefs).isEqualTo(DeserialisationLimits.DEFAULT_MAX_REFS);
    }

    @Test
    public void shouldUseDefaultLimitsWhenNotConfigured() {
        DeserialisationLimits limits = DeserialisationLimits.parse(null);

        assertThat(limits.maxDepth).isEqualTo(DeserialisationLimits.DEFAULT_MAX_DEPTH);
        assertThat(limits.maxRefs).isEqualTo(DeserialisationLimits.DEFAULT_MAX_REFS);
        assertThat(limits.maxArray).isEqualTo(DeserialisationLimits.DEFAULT_MAX_ARRAY);
        assertThat(limits.maxBytes).isEqualTo(DeserialisationLimits.DEFAULT_MAX_BYTES);
        assertThat(limits.maxCollectionDepth).isEqualTo(DeserialisationLimits.DEFAULT_MAX_COLLECTION_DEPTH);
        assertThat(limits.maxExpandedRefs).isEqualTo(DeserialisationLimits.DEFAULT_MAX_EXPANDED_REFS);
        assertThat(limits.maxWeakHashKeys).isEqualTo(DeserialisationLimits.DEFAULT_MAX_WEAK_HASH_KEYS);
    }

    @Test
    public void shouldParseMaxWeakHashKeys() {
        DeserialisationLimits configured = DeserialisationLimits.parse("maxweakhashkeys=128");
        assertThat(configured.maxWeakHashKeys).isEqualTo(128L);
        assertThat(configured.maxExpandedRefs).isEqualTo(DeserialisationLimits.DEFAULT_MAX_EXPANDED_REFS);
    }

    @Test
    public void shouldParseMaxExpandedRefsAndWarnOnInvalidValue() {
        // A valid value is stored; an invalid one is ignored with a warning and keeps its default. This pins the
        // maxexpandedrefs branch (a mutant that dropped it would leave the value at its default).
        DeserialisationLimits configured = DeserialisationLimits.parse("maxexpandedrefs=25000");
        assertThat(configured.maxExpandedRefs).isEqualTo(25000L);

        DeserialisationLimits invalid = DeserialisationLimits.parse("maxexpandedrefs=nope");
        assertThat(invalid.maxExpandedRefs).isEqualTo(DeserialisationLimits.DEFAULT_MAX_EXPANDED_REFS);
    }

    @Test
    public void shouldApplyConfiguredExpandedRefsLimit() throws Exception {
        // A list that references one small sublist many times. Its reconstruction graph, expanded into a tree,
        // is well under the default but over a small configured maxexpandedrefs. This pins that the scanner's
        // expanded-refs bound is wired into deserialise() and reads the configured value.
        List<Object> sub = new ArrayList<>();
        sub.add("x");
        List<Object> root = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            root.add(sub);
        }
        final byte[] bytes = getObjectStreamBytes(root, false);

        // Accepted under the default limit.
        assertThat((Object) IOUtils.deserialise(bytes, false)).isNotNull();

        try {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "maxexpandedrefs=100");
            IOUtils.deserialise(bytes, false);
            Assert.fail("Expected the configured maxexpandedrefs=100 to reject the shared-subgraph list");
        } catch (IOException expected) {
            // Rejected by the pre-scan under the configured limit, as required.
        } finally {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "");
        }
    }

    @Test
    public void shouldApplyConfiguredWeakHashKeysLimit() throws Exception {
        // Ten locales, the shape of the Accept-Language set of a remote authentication request: accepted under
        // the default, rejected once the configured limit is lower. This pins that the limit reaches the scanner.
        Set<Locale> locales = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            locales.add(new Locale("l" + i));
        }
        final byte[] bytes = getObjectStreamBytes(locales, false);

        assertThat((Object) IOUtils.deserialise(bytes, false)).isNotNull();

        try {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "maxweakhashkeys=5");
            IOUtils.deserialise(bytes, false);
            Assert.fail("Expected the configured maxweakhashkeys=5 to reject ten locales");
        } catch (IOException expected) {
            assertThat(expected).hasMessageContaining("stream scanner");
        } finally {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "");
        }
    }

    @Test(expectedExceptions = IOException.class, timeOut = 2000)
    public void shouldRejectHashSetOfCollidingListKeys() throws Exception {
        // Single-element lists of "Aa"/"BB" strings share one hashCode and are not Comparable, so every insertion
        // into the rebuilt HashSet scans the whole bin: O(n^2) equals() while the expanded tree stays at 2n + 1
        Set<Object> root = new HashSet<>();
        List<List<Object>> keys = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            List<Object> key = new ArrayList<>();
            key.add("k" + i);
            root.add(key);
            keys.add(key);
        }
        for (int i = 0; i < keys.size(); i++) {
            StringBuilder colliding = new StringBuilder();
            for (int b = 0; b < 15; b++) {
                colliding.append(((i >> b) & 1) == 0 ? "Aa" : "BB");
            }
            keys.get(i).set(0, colliding.toString());
        }

        IOUtils.deserialise(getObjectStreamBytes(root, false), false);
    }

    @Test
    public void shouldPreScanBeforeAnyClassIsResolved() throws Exception {
        // java.io.File is not whitelisted, so deserialise() without the pre-scan fails at class resolution. Under
        // a small maxexpandedrefs the rejection must come from the scanner instead: the reconstruction bound is
        // enforced before any class in the stream is resolved or any object is built.
        List<Object> sub = new ArrayList<>();
        sub.add(new File("/tmp/tmpfile.txt"));
        List<Object> root = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            root.add(sub);
        }
        final byte[] bytes = getObjectStreamBytes(root, false);

        try {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "maxexpandedrefs=100");
            IOUtils.deserialise(bytes, false);
            Assert.fail("Expected the pre-scan to reject the stream under maxexpandedrefs=100");
        } catch (IOException expected) {
            assertThat(expected).hasMessageContaining("stream scanner");
        } finally {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "");
        }
    }

    @Test(expectedExceptions = IOException.class, timeOut = 10000)
    public void shouldRejectWideShallowNestedCollectionGraph() throws Exception {
        // A graph that stays inside the depth, reference, array and byte limits, but nests hash-based
        // collections one level deeper than allowed. Reconstructing it recomputes hashCode() over the whole
        // sub-graph at every level (width^nesting), so it must be rejected on its structure, not on its size:
        // the numeric limits alone would let this through.
        Set<Object> root = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            Set<Object> level2 = new HashSet<>();
            Set<Object> level3 = new HashSet<>();
            level3.add("leaf" + i);
            level2.add(level3);
            root.add(level2);
        }

        IOUtils.deserialise(getObjectStreamBytes(root, false), false);
    }

    @Test
    public void shouldAcceptCollectionsNestedToTheLimit() throws Exception {
        // A collection nested exactly maxcollectiondepth (2) deep, with scalar leaves, must be accepted. This
        // pins the boundary from below, so a mutant that tightened the limit to 1 would fail.
        Map<String, Object> root = new HashMap<>();
        Set<String> inner = new HashSet<>();
        inner.add("a");
        inner.add("b");
        root.put("k", inner);

        assertThat((Object) IOUtils.deserialise(getObjectStreamBytes(root, false), false)).isNotNull();
    }

    @Test
    public void shouldConsultUpstreamFilterInAdditionToLimits() throws Exception {
        // Nothing else configures a JVM-wide jdk.serialFilter, so this pins that a supplied upstream filter is
        // still consulted after the built-in limits: a mutant that dropped the delegation would let this through.
        // A whitelisted, filtered class (HashMap) is used because the JVM does not run the filter over bare
        // Strings.
        ObjectInputFilter rejectHashMaps = info ->
                info.serialClass() == HashMap.class
                        ? ObjectInputFilter.Status.REJECTED
                        : ObjectInputFilter.Status.UNDECIDED;
        final byte[] bytes = getObjectStreamBytes(new HashMap<>(), false);

        // Without the upstream filter the same bytes deserialise cleanly.
        assertThat((Object) IOUtils.deserialise(bytes, false)).isNotNull();

        try {
            IOUtils.deserialise(bytes, false, null, rejectHashMaps);
            Assert.fail("Expected the upstream filter to reject java.util.HashMap");
        } catch (IOException expected) {
            // The upstream filter rejected the HashMap, as required.
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldRejectUncompressedStreamBeyondLimit() throws Exception {
        // The byte cap must apply to the uncompressed path as well, not only after inflation.
        char[] chars = new char[(int) DeserialisationLimits.DEFAULT_MAX_BYTES + 1];
        Arrays.fill(chars, 'a');

        IOUtils.deserialise(getObjectStreamBytes(new String(chars), false), false);
    }

    @Test
    public void shouldApplyConfiguredCollectionDepthLimit() throws Exception {
        // Pins that the configured limits are actually wired into deserialise(): lowering maxcollectiondepth to 1
        // must reject a graph (root map -> set) that is accepted under the default of 2.
        Map<String, Object> root = new HashMap<>();
        Set<String> inner = new HashSet<>();
        inner.add("a");
        root.put("k", inner);
        final byte[] bytes = getObjectStreamBytes(root, false);

        try {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "maxcollectiondepth=1");
            try {
                IOUtils.deserialise(bytes, false);
                Assert.fail("Expected the configured maxcollectiondepth=1 to reject the nested collection");
            } catch (IOException expected) {
                // Rejected under the configured limit, as required.
            }
        } finally {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "");
        }

        // With the limit restored to its default, the same graph is accepted again.
        assertThat((Object) IOUtils.deserialise(bytes, false)).isNotNull();
    }

    @Test
    public void shouldNotCountClosedSiblingCollectionFrames() throws Exception {
        // Two sibling values of the same map: a HashSet that closes, then an OrderedSet whose internal
        // ArrayList sits one stream level deeper than the closed HashSet frame. The true collection nesting
        // is 2 everywhere, so this must be accepted under the default limit regardless of which sibling is
        // read first (LinkedHashMap pins the order to the failing one: the HashSet before the OrderedSet).
        // A tracker that only prunes closed frames when the next backing array arrives overcounts this to 3.
        Map<String, Object> root = new LinkedHashMap<>();
        Set<String> first = new HashSet<>();
        first.add("x");
        root.put("a", first);
        OrderedSet second = new OrderedSet();
        second.add("y");
        root.put("b", second);

        assertThat((Object) IOUtils.deserialise(getObjectStreamBytes(root, false), false)).isNotNull();
    }

    @Test
    public void shouldRespectExplicitlyConfiguredTighterCollectionDepthOnConsolePath() throws Exception {
        // The console minimum lifts only the built-in default: an operator who explicitly configured a
        // tighter maxcollectiondepth keeps it on the console path too.
        Map<String, Object> root = new HashMap<>();
        Set<String> inner = new HashSet<>();
        inner.add("a");
        root.put("k", inner);
        final byte[] bytes = getObjectStreamBytes(root, false);

        try {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "maxcollectiondepth=1");
            IOUtils.deserialise(bytes, false, null, IOUtils.CONSOLE_STATE_MAX_COLLECTION_DEPTH);
            Assert.fail("Expected the explicitly configured maxcollectiondepth=1 to win over the console minimum");
        } catch (IOException expected) {
            // The operator's explicit configuration wins, as required.
        } finally {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "");
        }
    }

    @Test
    public void shouldRejectTreeCollectionsAsNotWhitelisted() throws Exception {
        // Sorted (tree-based) collections raise no per-instance backing-array check during their readObject,
        // so neither the collection-nesting limit nor the array-length limit can see them. They must therefore
        // stay off the class whitelist: this pins that a TreeSet is rejected by the whitelist itself rather
        // than silently escaping the limits.
        try {
            IOUtils.deserialise(getObjectStreamBytes(new TreeSet<>(Collections.singleton("a")), false), false);
            Assert.fail("Expected java.util.TreeSet to be rejected as not whitelisted");
        } catch (InvalidClassException expected) {
            assertThat(expected.getMessage()).contains("whitelist");
        }
    }

    @Test
    public void shouldAcceptConsoleStateShapeOnlyWithConsoleCollectionDepth() throws Exception {
        // The console/JATO page state nests page-session map -> attribute-value map -> value set, one level
        // past the default limit. The console callers pass CONSOLE_STATE_MAX_COLLECTION_DEPTH for it, while
        // the default limit must keep rejecting the same shape so the unauthenticated endpoints stay tight.
        Map<String, Object> pageSession = new HashMap<>();
        Map<String, Object> attributeValues = new HashMap<>();
        Set<String> values = new HashSet<>();
        values.add("value");
        attributeValues.put("attribute", values);
        pageSession.put("propertyAttributes", attributeValues);
        final byte[] bytes = getObjectStreamBytes(pageSession, false);

        try {
            IOUtils.deserialise(bytes, false);
            Assert.fail("Expected the default collection-depth limit to reject the console page-session shape");
        } catch (IOException expected) {
            // The default limit stays tight, as required.
        }

        Map<String, Object> restored = IOUtils.deserialise(bytes, false, null,
                IOUtils.CONSOLE_STATE_MAX_COLLECTION_DEPTH);
        assertThat(restored).isEqualTo(pageSession);
    }

    @Test
    public void shouldPreferLargerConfiguredCollectionDepthOverConsoleMinimum() throws Exception {
        // The caller-supplied depth is a minimum, not a replacement: an operator who configured a larger
        // maxcollectiondepth must keep it on the console path too. A four-level graph is accepted with the
        // configured limit of 4 although the console minimum alone (3) would reject it.
        Map<String, Object> level1 = new HashMap<>();
        Map<String, Object> level2 = new HashMap<>();
        Map<String, Object> level3 = new HashMap<>();
        Set<String> level4 = new HashSet<>();
        level4.add("a");
        level3.put("k3", level4);
        level2.put("k2", level3);
        level1.put("k1", level2);
        final byte[] bytes = getObjectStreamBytes(level1, false);

        try {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "maxcollectiondepth=4");
            assertThat((Object) IOUtils.deserialise(bytes, false, null,
                    IOUtils.CONSOLE_STATE_MAX_COLLECTION_DEPTH)).isNotNull();
        } finally {
            SystemPropertiesManager.initializeProperties(Constants.DESERIALISATION_LIMITS, "");
        }

        // Without the configured limit the same graph is over both the default and the console minimum.
        try {
            IOUtils.deserialise(bytes, false, null, IOUtils.CONSOLE_STATE_MAX_COLLECTION_DEPTH);
            Assert.fail("Expected the console minimum of 3 to reject a four-level collection graph");
        } catch (IOException expected) {
            // Rejected once the configured limit is gone, as required.
        }
    }

    private static final class SerializableHandler implements InvocationHandler, Serializable {
        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return null;
        }
    }

    private byte[] getObjectStreamBytes(Object in, boolean compress) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = compress
                ? new ObjectOutputStream(new DeflaterOutputStream(byteArrayOutputStream))
                : new ObjectOutputStream(byteArrayOutputStream);

        objectOutputStream.writeObject(in);
        objectOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    @Test
    public void shouldCopyStreamsFully() throws Exception {
        // Given
        final byte[] input = new byte[8 * 1024 + 1]; // Slightly larger than a typical buffer size
        for (int i = 0; i < input.length; ++i) {
            input[i] = (byte) i;
        }

        // Take a defensive copy in case the implementation modifies it in any way
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Arrays.copyOf(input, input.length));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // When
            long bytesCopied = IOUtils.copyStream(inputStream, outputStream);

            // Then
            assertThat(outputStream.toByteArray()).isEqualTo(input);
            assertThat(bytesCopied).isEqualTo((long) input.length);
        }
    }

    @Test
    public void shouldCopyEmptyStreams() throws Exception {
        // Given
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // When
            long bytesCopied = IOUtils.copyStream(inputStream, outputStream);

            // Then
            assertThat(bytesCopied).isEqualTo(0L);
            assertThat(outputStream.toByteArray()).isEqualTo(new byte[0]);
        }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullInputStreamForCopy() throws Exception {
        IOUtils.copyStream(null, new ByteArrayOutputStream());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullOutputStreamForCopy() throws Exception {
        IOUtils.copyStream(new ByteArrayInputStream(new byte[0]), null);
    }
}