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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectStreamConstants;
import java.io.StreamCorruptedException;
import java.io.UTFDataFormatException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A single, linear pass over a Java serialisation stream that rejects it before {@link java.io.ObjectInputStream}
 * loads or instantiates anything. It exists because a JEP 290 {@link java.io.ObjectInputFilter} cannot see the
 * shape of the reconstructed object graph: a back-reference ({@code TC_REFERENCE}) reaches the filter with
 * {@code serialClass() == null} and its target unavailable, so the filter cannot bound the cost of the recursive
 * {@code hashCode()}/{@code equals()} that the JDK runs while it rebuilds hash-based collections. A small stream
 * whose objects fan out through shared back-references can therefore expand into an exponentially large tree of
 * hashing work while every JEP 290 count (depth, references, array length, bytes) stays tiny.
 *
 * <p>The scanner walks the stream grammar of chapter 6 of the Java Object Serialization Specification without
 * instantiating or loading any class. It assigns a graph node to each object, array, string and enum handle, and
 * an edge from a node to every object it references through an object field, an object array element, or a
 * {@code writeObject} annotation; a back-reference is an edge to a node that already exists. It then computes the
 * size of the graph <em>expanded into a tree</em> from the root, memoised per node and with saturating arithmetic
 * capped at the limit. That expanded size bounds the objects a recursive {@code hashCode()} visits while the JDK
 * rebuilds hash-based collections, counting a shared node once per path that reaches it, which the raw handle
 * count of a back-reference DAG never shows. An edge back to an ancestor still being read (a cycle) adds nothing
 * to the sum. The JDK does not break such a cycle: a {@code hashCode()} that recursed into it would overflow the
 * stack. It only costs work when a descendant hashes a collection that is still being rebuilt, and that needs a
 * hash key whose {@code hashCode()} reaches the collection, which the hash-key rule below rejects or caps.
 *
 * <p>The expanded size does not bound the {@code equals()} calls between keys of one hash collection that share
 * a hash code. {@code HashMap} (and so {@code HashSet}) orders such keys in a tree bin only when they are
 * mutually {@code Comparable}, and otherwise scans the whole bin on every insertion, which is quadratic in the
 * number of keys. The scanner therefore also checks the keys of every {@code HashMap} and the elements of every
 * {@code HashSet}, including their subclasses, as it reads them:
 * <ul>
 *     <li>a collection or map used as a key is rejected: its hash code is chosen by its contents, and no OpenAM
 *     payload hashes one;</li>
 *     <li>keys that all share one class from {@link #ORDERED_HASH_KEY_CLASSES}, or that are all constants of one
 *     enum, are unrestricted: each of those classes is either {@code Comparable} consistently with
 *     {@code equals}, or gives distinct values distinct hash codes;</li>
 *     <li>any other keys, including a mix of classes, are allowed up to {@code maxWeakHashKeys} per collection,
 *     which keeps the bin scans linear in the size of the stream.</li>
 * </ul>
 * Other hash-based collections ({@code Hashtable}, {@code ConcurrentHashMap}) write a different layout and are
 * not checked, so they must not be added to {@code openam.deserialisation.classes.whitelist}.
 *
 * <p>These rules, and the parsing of arrays, depend on class names, so the scanner decodes a name exactly as
 * {@link java.io.ObjectInputStream} does: as modified UTF-8, which also accepts overlong byte sequences. A
 * standard UTF-8 decoder would turn an overlong {@code java.util.HashSet} into a name the scanner does not
 * recognise, while {@code ObjectInputStream} still resolves it to the class whose keys had to be checked.
 *
 * <p>The scanner fails closed. It rejects a dynamic-proxy descriptor ({@code TC_PROXYCLASSDESC}), an exception
 * placeholder ({@code TC_EXCEPTION}), an externalizable class ({@code SC_EXTERNALIZABLE}), an unknown tag, a
 * handle outside the table, a truncated stream, and any trailing byte after the root object. It also enforces the
 * array-length limit on primitive arrays as it skips them, and caps the references it records at the expanded-size
 * limit, so its own memory is bounded by the limit rather than by the size of the stream. If it accepts the
 * stream, the same bytes are then
 * deserialised through {@code WhitelistObjectInputStream}, so the class whitelist, the JEP 290 filter and every
 * numeric limit still apply as defence in depth.
 *
 * <p>For a class that declares a {@code writeObject} ({@code SC_WRITE_METHOD}) the scanner assumes the stream
 * convention that {@code defaultWriteObject}/{@code putFields} writes the declared serialisable fields first and
 * that any hand-written data then follows as an {@code objectAnnotation} terminated by {@code TC_ENDBLOCKDATA}.
 * Every class on the default whitelist follows this convention. A class whose {@code writeObject} deviates from it
 * would be misparsed and rejected here even though its own bytes are well formed; such a class must therefore not
 * be added to {@code openam.deserialisation.classes.whitelist}.
 */
final class SerialStreamScanner {

    // A frame stack this deep is far past any legitimate OpenAM payload; the numeric maxdepth limit (default 20)
    // rejects deep graphs at deserialisation time. The cap here only stops a hostile stream from overflowing the
    // parser's own call stack before it is measured. Each level costs up to five frames, about 1 KiB of stack, so
    // 1000 levels already overflowed the default 1 MiB thread stack; 200 levels fit in a quarter of it.
    private static final int MAX_PARSE_DEPTH = 200;

    private static final int STREAM_MAGIC = ObjectStreamConstants.STREAM_MAGIC & 0xFFFF;
    private static final int STREAM_VERSION = ObjectStreamConstants.STREAM_VERSION;
    private static final int BASE_HANDLE = ObjectStreamConstants.baseWireHandle;

    private static final String STRING_CLASS = "java.lang.String";

    // The classes whose writeObject annotation carries the keys of a hash table, and how many objects each entry
    // writes: HashMap writes key, value pairs and HashSet writes elements. Subclasses (LinkedHashMap,
    // CaseInsensitiveHashMap, CaseInsensitiveHashSet, ...) inherit the annotation of these levels.
    private static final String HASH_MAP_CLASS = "java.util.HashMap";
    private static final String HASH_SET_CLASS = "java.util.HashSet";

    /**
     * Key classes that keep {@code HashMap} insertion logarithmic however their hash codes collide, as long as
     * every key of the collection has the same class: they are {@code Comparable} consistently with
     * {@code equals} ({@code String}, {@code CaseInsensitiveKey}, {@code Integer}, {@code Long}, {@code Boolean}),
     * or distinct values have distinct hash codes ({@code InetAddress}, which deserialises as an IPv4 address).
     */
    static final Set<String> ORDERED_HASH_KEY_CLASSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            STRING_CLASS,
            "com.sun.identity.common.CaseInsensitiveKey",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Boolean",
            "java.net.InetAddress")));

    // Collection and map classes, whose hash code is chosen by their contents. None may be a hash key.
    private static final Set<String> COLLECTION_CLASSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "java.util.ArrayList",
            "java.util.LinkedList",
            "java.util.Vector",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.Hashtable",
            "java.util.TreeMap",
            "java.util.HashSet",
            "java.util.LinkedHashSet",
            "java.util.TreeSet",
            "java.util.concurrent.ConcurrentHashMap",
            "com.sun.identity.common.CaseInsensitiveHashMap",
            "com.sun.identity.common.CaseInsensitiveHashSet",
            "com.sun.identity.shared.datastruct.OrderedSet",
            "com.sun.xml.bind.util.ListImpl",
            "com.sun.xml.bind.util.ProxyListImpl",
            "org.apache.catalina.util.ParameterMap")));

    // Prefixes of the JDK's collection wrappers (Collections$UnmodifiableMap, Collections$SingletonList, ...).
    private static final List<String> COLLECTION_CLASS_PREFIXES = Arrays.asList(
            "java.util.Collections$", "java.util.Arrays$", "java.util.ImmutableCollections$");

    private SerialStreamScanner() {
    }

    /**
     * Scans the given serialisation bytes and throws if the stream is malformed, uses a forbidden construct, has
     * hash keys whose reconstruction would degrade to repeated bin scans, or would expand into more than
     * {@code maxExpandedRefs} nodes when reconstructed as a tree.
     *
     * @param data The uncompressed serialisation bytes.
     * @param maxArray The array-length limit, applied to primitive arrays as they are skipped.
     * @param maxExpandedRefs The maximum size of the object graph expanded into a tree from the root, which also
     *                        caps the number of references the scanner records.
     * @param maxWeakHashKeys The maximum number of keys of one hash collection whose class does not keep bin
     *                        lookups logarithmic.
     * @throws IOException If the stream is rejected.
     */
    static void scan(byte[] data, long maxArray, long maxExpandedRefs, long maxWeakHashKeys) throws IOException {
        new Parser(data, maxArray, maxExpandedRefs, maxWeakHashKeys, true).run();
    }

    /**
     * Visible for testing: parses the stream with the limits and the hash-key rule disabled and returns the
     * expanded tree size of the graph, so that the default limit can be justified against real payload shapes.
     */
    static long expandedNodeCount(byte[] data, long maxArray) throws IOException {
        final Parser parser = new Parser(data, maxArray, Long.MAX_VALUE, Long.MAX_VALUE, false);
        parser.run();
        return parser.lastExpandedSize;
    }

    /** A class descriptor recovered from the stream: the flags and the field layout needed to skip an instance. */
    private static final class ClassDesc {
        final String name;
        final int flags;
        // For each field: the number of bytes to skip for a primitive field, or 0 for an object/array field
        // whose value must be read as an object.
        final int[] fieldPrimSize;
        final ClassDesc superDesc;

        ClassDesc(String name, int flags, int[] fieldPrimSize, ClassDesc superDesc) {
            this.name = name;
            this.flags = flags;
            this.fieldPrimSize = fieldPrimSize;
            this.superDesc = superDesc;
        }
    }

    private static final class Parser {

        private final byte[] buf;
        private final int len;
        private int pos;

        private final long maxArray;
        private final long maxExpandedRefs;
        private final long maxWeakHashKeys;
        private final boolean checkHashKeys;

        // The stream handle table. Each entry is a ClassDesc (for class descriptors) or an Integer node id (for
        // objects, arrays, strings and enums). TC_RESET clears it.
        private final List<Object> handles = new ArrayList<>();

        // Outgoing edges per graph node, indexed by node id.
        private final List<List<Integer>> children = new ArrayList<>();
        // The class name of each graph node, indexed by node id, and the nodes that are enum constants.
        private final List<String> nodeClasses = new ArrayList<>();
        private final BitSet enumNodes = new BitSet();
        private long edgeCount;
        private int rootNode = -1;
        private long lastExpandedSize;

        Parser(byte[] buf, long maxArray, long maxExpandedRefs, long maxWeakHashKeys, boolean checkHashKeys) {
            this.buf = buf;
            this.len = buf.length;
            this.maxArray = maxArray;
            this.maxExpandedRefs = maxExpandedRefs;
            this.maxWeakHashKeys = maxWeakHashKeys;
            this.checkHashKeys = checkHashKeys;
        }

        void run() throws IOException {
            if (readU2() != STREAM_MAGIC) {
                throw corrupt("bad stream magic");
            }
            if (readU2() != STREAM_VERSION) {
                throw corrupt("unsupported stream version");
            }
            // ObjectInputStream.readObject skips a TC_RESET before the root object, clearing the handle table.
            while (pos < len && (buf[pos] & 0xFF) == ObjectStreamConstants.TC_RESET) {
                pos++;
                handles.clear();
            }
            rootNode = readObjectValue(0);
            if (pos != len) {
                throw corrupt("trailing bytes after the root object");
            }
            final long expanded = expandedSize();
            lastExpandedSize = expanded;
            if (expanded > maxExpandedRefs) {
                throw corrupt("object graph expands to " + expanded + " nodes, over the limit of " + maxExpandedRefs);
            }
        }

        // ---- graph -------------------------------------------------------------------------------------------

        private int newNode(String className) throws IOException {
            final int id = children.size();
            // The expanded size is at least the number of distinct nodes, so a node count over the limit can
            // never pass; rejecting here also bounds the memory the scanner itself uses.
            if ((long) id >= maxExpandedRefs) {
                throw corrupt("more than " + maxExpandedRefs + " objects in the stream");
            }
            children.add(new ArrayList<Integer>());
            nodeClasses.add(className);
            return id;
        }

        private void addEdge(int from, int to) throws IOException {
            if (to >= 0) {
                // Every edge except one back to an ancestor adds at least one node to the expanded size, so a
                // legitimate stream never records more edges than the limit; capping them here bounds the
                // scanner's own memory by the limit rather than by the size of the stream.
                if (++edgeCount > maxExpandedRefs) {
                    throw corrupt("more than " + maxExpandedRefs + " references in the stream");
                }
                children.get(from).add(to);
            }
        }

        // Rejects the keys of one hash collection whose reconstruction would degrade into repeated scans of a
        // hash bin: see the class Javadoc.
        private void checkHashKeys(String collectionClass, List<Integer> keys) throws IOException {
            String keyClass = null;
            boolean oneClass = true;
            boolean allEnums = true;
            for (int key : keys) {
                final String name = nodeClasses.get(key);
                if (isCollectionClass(name)) {
                    throw corrupt("a " + name + " is used as a key of a " + collectionClass);
                }
                if (keyClass == null) {
                    keyClass = name;
                } else if (!keyClass.equals(name)) {
                    oneClass = false;
                }
                allEnums &= enumNodes.get(key);
            }
            final boolean ordered = keyClass == null
                    || oneClass && (allEnums || ORDERED_HASH_KEY_CLASSES.contains(keyClass));
            if (!ordered && keys.size() > maxWeakHashKeys) {
                throw corrupt(keys.size() + " keys of a " + collectionClass + " are not all of one ordered"
                        + " class, over the limit of " + maxWeakHashKeys);
            }
        }

        private static boolean isCollectionClass(String name) {
            if (COLLECTION_CLASSES.contains(name)) {
                return true;
            }
            for (String prefix : COLLECTION_CLASS_PREFIXES) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        // Size of the graph expanded into a tree from the root, memoised per node, saturating at the limit.
        // A back-edge to an ancestor still on the DFS stack (a cycle) contributes nothing.
        private long expandedSize() {
            final int n = children.size();
            if (rootNode < 0 || n == 0) {
                return 0;
            }
            // Saturate one past the limit; anything at cap fails the check. Guard against overflow when the
            // configured limit is at Long.MAX_VALUE.
            final long cap = maxExpandedRefs >= Long.MAX_VALUE ? Long.MAX_VALUE : maxExpandedRefs + 1;
            final long[] memo = new long[n];
            final byte[] state = new byte[n]; // 0 = new, 1 = on stack, 2 = done
            final int[] iter = new int[n];    // next child index per frame
            final Deque<Integer> stack = new ArrayDeque<>();

            stack.push(rootNode);
            state[rootNode] = 1;
            memo[rootNode] = 1;
            while (!stack.isEmpty()) {
                final int node = stack.peek();
                final List<Integer> kids = children.get(node);
                if (iter[node] < kids.size()) {
                    final int child = kids.get(iter[node]++);
                    if (state[child] == 2) {
                        memo[node] = saturatingAdd(memo[node], memo[child], cap);
                    } else if (state[child] == 0) {
                        state[child] = 1;
                        memo[child] = 1;
                        iter[child] = 0;
                        stack.push(child);
                    }
                    // state[child] == 1 is a cycle back to an ancestor: it contributes nothing.
                } else {
                    state[node] = 2;
                    stack.pop();
                    if (!stack.isEmpty()) {
                        final int parent = stack.peek();
                        memo[parent] = saturatingAdd(memo[parent], memo[node], cap);
                    }
                }
            }
            return memo[rootNode];
        }

        private static long saturatingAdd(long a, long b, long cap) {
            final long sum = a + b;
            return sum >= cap || sum < 0 ? cap : sum;
        }

        // ---- grammar -----------------------------------------------------------------------------------------

        // Reads one object in a value position (field value, array element, class-name string, enum name) and
        // returns its node id, or -1 for a null reference.
        private int readObjectValue(int depth) throws IOException {
            guardDepth(depth);
            final int tag = readU1();
            switch (tag) {
            case ObjectStreamConstants.TC_NULL:
                return -1;
            case ObjectStreamConstants.TC_REFERENCE:
                return resolveObjectReference();
            case ObjectStreamConstants.TC_STRING:
                return newStringNode(readU2());
            case ObjectStreamConstants.TC_LONGSTRING:
                return newStringNode(readLong());
            case ObjectStreamConstants.TC_OBJECT:
                return readNewObject(depth);
            case ObjectStreamConstants.TC_ARRAY:
                return readNewArray(depth);
            case ObjectStreamConstants.TC_ENUM:
                return readNewEnum(depth);
            case ObjectStreamConstants.TC_CLASS:
                // TC_CLASS classDesc newHandle: a java.lang.Class instance, a leaf with no outgoing edges.
                readClassDesc(depth);
                final int node = newNode("java.lang.Class");
                handles.add(node);
                return node;
            case ObjectStreamConstants.TC_PROXYCLASSDESC:
                throw corrupt("Dynamic proxy class descriptor is not allowed");
            case ObjectStreamConstants.TC_EXCEPTION:
                throw corrupt("TC_EXCEPTION is not allowed");
            default:
                throw corrupt("unexpected tag 0x" + Integer.toHexString(tag) + " in object position");
            }
        }

        private int newStringNode(long utfLength) throws IOException {
            if (utfLength < 0) {
                throw corrupt("negative string length");
            }
            skip(utfLength);
            final int node = newNode(STRING_CLASS);
            handles.add(node);
            return node;
        }

        private int resolveObjectReference() throws IOException {
            final Object entry = handleAt(readInt());
            if (!(entry instanceof Integer)) {
                throw corrupt("reference does not point to an object");
            }
            return (Integer) entry;
        }

        private int readNewObject(int depth) throws IOException {
            final ClassDesc desc = readClassDesc(depth);
            if (desc == null) {
                throw corrupt("object with a null class descriptor");
            }
            final int node = newNode(desc.name);
            handles.add(node);
            // Class data is written highest superclass first; the descriptor chain runs the other way.
            final List<ClassDesc> chain = new ArrayList<>();
            for (ClassDesc d = desc; d != null; d = d.superDesc) {
                chain.add(d);
            }
            for (int i = chain.size() - 1; i >= 0; i--) {
                readClassData(chain.get(i), node, depth);
            }
            return node;
        }

        private void readClassData(ClassDesc desc, int node, int depth) throws IOException {
            if ((desc.flags & ObjectStreamConstants.SC_SERIALIZABLE) != 0) {
                for (int primSize : desc.fieldPrimSize) {
                    if (primSize > 0) {
                        skip(primSize);
                    } else {
                        addEdge(node, readObjectValue(depth + 1));
                    }
                }
                if ((desc.flags & ObjectStreamConstants.SC_WRITE_METHOD) != 0) {
                    // After their block data, HashMap writes key, value pairs and HashSet writes its elements.
                    final int keyStride = !checkHashKeys ? 0
                            : HASH_MAP_CLASS.equals(desc.name) ? 2
                            : HASH_SET_CLASS.equals(desc.name) ? 1 : 0;
                    final List<Integer> keys = keyStride > 0 ? new ArrayList<Integer>() : null;
                    readAnnotation(node, depth, keys, keyStride);
                    if (keys != null) {
                        checkHashKeys(desc.name, keys);
                    }
                }
            }
            // A non-serializable, non-externalizable descriptor in the chain carries no data. Externalizable
            // descriptors were already rejected while the descriptor was parsed.
        }

        private int readNewArray(int depth) throws IOException {
            final ClassDesc desc = readClassDesc(depth);
            if (desc == null) {
                throw corrupt("array with a null class descriptor");
            }
            final int node = newNode(desc.name);
            handles.add(node);
            final int size = readInt();
            if (size < 0) {
                throw corrupt("negative array length");
            }
            if (size > maxArray) {
                throw corrupt("array length " + size + " over the limit of " + maxArray);
            }
            final int elemPrim = desc.fieldPrimSize.length == 1 ? desc.fieldPrimSize[0] : -1;
            if (elemPrim < 0) {
                throw corrupt("array descriptor did not describe its element type");
            }
            if (elemPrim > 0) {
                skip((long) size * elemPrim);
            } else {
                for (int i = 0; i < size; i++) {
                    addEdge(node, readObjectValue(depth + 1));
                }
            }
            return node;
        }

        private int readNewEnum(int depth) throws IOException {
            final ClassDesc desc = readClassDesc(depth);
            if (desc == null) {
                throw corrupt("enum with a null class descriptor");
            }
            final int node = newNode(desc.name);
            enumNodes.set(node);
            handles.add(node);
            addEdge(node, readObjectValue(depth + 1)); // the enum constant name
            return node;
        }

        // Reads a class descriptor (TC_CLASSDESC, a reference to one, or TC_NULL) and returns it, or null for
        // TC_NULL. An array descriptor's fieldPrimSize is a single element describing the array component.
        private ClassDesc readClassDesc(int depth) throws IOException {
            guardDepth(depth);
            final int tag = readU1();
            switch (tag) {
            case ObjectStreamConstants.TC_NULL:
                return null;
            case ObjectStreamConstants.TC_REFERENCE:
                final Object entry = handleAt(readInt());
                if (!(entry instanceof ClassDesc)) {
                    throw corrupt("reference does not point to a class descriptor");
                }
                return (ClassDesc) entry;
            case ObjectStreamConstants.TC_PROXYCLASSDESC:
                throw corrupt("Dynamic proxy class descriptor is not allowed");
            case ObjectStreamConstants.TC_CLASSDESC:
                return readNewClassDesc(depth);
            default:
                throw corrupt("unexpected tag 0x" + Integer.toHexString(tag) + " in class-descriptor position");
            }
        }

        private ClassDesc readNewClassDesc(int depth) throws IOException {
            final String className = readUtf();
            skip(8); // serialVersionUID
            // Reserve the handle before parsing the body, matching ObjectInputStream, so a field type string or
            // the superclass descriptor can back-reference this descriptor.
            final int handleIndex = handles.size();
            handles.add(null);
            final int flags = readU1();
            if ((flags & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0) {
                throw corrupt("externalizable class " + className + " is not allowed");
            }
            final int fieldCount = readU2();
            final int[] fieldPrimSize;
            if (className.startsWith("[")) {
                // An array descriptor: its single "component" is described by the class name, not by fieldDesc
                // entries. Arrays declare no fields, so fieldCount is 0 here.
                fieldPrimSize = new int[] { arrayComponentPrimSize(className) };
            } else {
                fieldPrimSize = new int[fieldCount];
            }
            for (int i = 0; i < fieldCount; i++) {
                final int typeCode = readU1();
                readUtf(); // field name
                final int primSize = primitiveSize(typeCode);
                if (primSize > 0) {
                    if (!className.startsWith("[")) {
                        fieldPrimSize[i] = primSize;
                    }
                } else if (typeCode == 'L' || typeCode == '[') {
                    // className1: the field type as a (String)object.
                    readObjectValue(depth + 1);
                    // fieldPrimSize[i] stays 0 to mark an object field, unless this is an array descriptor.
                } else {
                    throw corrupt("unknown field type code 0x" + Integer.toHexString(typeCode));
                }
            }
            readAnnotation(-1, depth, null, 0); // classAnnotation; its objects are not graph edges of any instance
            // One level deeper, as in ObjectInputStream: a chain of new superclass descriptors recurses here, and
            // must reach the depth guard before it overflows the call stack.
            final ClassDesc superDesc = readClassDesc(depth + 1);
            final ClassDesc desc = new ClassDesc(className, flags, fieldPrimSize, superDesc);
            handles.set(handleIndex, desc);
            return desc;
        }

        // Reads a contents-until-TC_ENDBLOCKDATA sequence (a classAnnotation or objectAnnotation). Objects read
        // here are edges from the given node, or ignored when node is -1 (a class annotation). When keys is not
        // null, every keyStride-th object, starting with the first, is a hash key and its node is added to keys.
        private void readAnnotation(int node, int depth, List<Integer> keys, int keyStride) throws IOException {
            guardDepth(depth);
            int objectIndex = 0;
            while (true) {
                if (pos >= len) {
                    throw corrupt("truncated annotation");
                }
                final int tag = buf[pos] & 0xFF;
                if (tag == ObjectStreamConstants.TC_ENDBLOCKDATA) {
                    pos++;
                    return;
                } else if (tag == ObjectStreamConstants.TC_BLOCKDATA) {
                    pos++;
                    skip(readU1());
                } else if (tag == ObjectStreamConstants.TC_BLOCKDATALONG) {
                    pos++;
                    final int blockLen = readInt();
                    if (blockLen < 0) {
                        throw corrupt("negative long block-data length");
                    }
                    skip(blockLen);
                } else if (tag == ObjectStreamConstants.TC_RESET) {
                    pos++;
                    handles.clear();
                } else {
                    final int child = readObjectValue(depth + 1);
                    if (node >= 0) {
                        addEdge(node, child);
                    }
                    if (keys != null && objectIndex % keyStride == 0 && child >= 0) {
                        keys.add(child);
                    }
                    objectIndex++;
                }
            }
        }

        // ---- primitive types ---------------------------------------------------------------------------------

        private static int primitiveSize(int typeCode) {
            switch (typeCode) {
            case 'B': // byte
            case 'Z': // boolean
                return 1;
            case 'C': // char
            case 'S': // short
                return 2;
            case 'I': // int
            case 'F': // float
                return 4;
            case 'J': // long
            case 'D': // double
                return 8;
            default:
                return 0;
            }
        }

        private int arrayComponentPrimSize(String arrayClassName) throws IOException {
            if (arrayClassName.length() < 2) {
                throw corrupt("malformed array class name " + arrayClassName);
            }
            final char component = arrayClassName.charAt(1);
            if (component == 'L' || component == '[') {
                return 0; // object or nested-array elements
            }
            final int size = primitiveSize(component);
            if (size == 0) {
                throw corrupt("malformed array class name " + arrayClassName);
            }
            return size;
        }

        // ---- low-level reads ---------------------------------------------------------------------------------

        private void guardDepth(int depth) throws IOException {
            if (depth > MAX_PARSE_DEPTH) {
                throw corrupt("stream nested deeper than " + MAX_PARSE_DEPTH);
            }
        }

        private Object handleAt(int handle) throws IOException {
            final int index = handle - BASE_HANDLE;
            if (index < 0 || index >= handles.size() || handles.get(index) == null) {
                throw corrupt("reference to handle outside the table");
            }
            return handles.get(index);
        }

        private int readU1() throws IOException {
            if (pos >= len) {
                throw corrupt("truncated stream");
            }
            return buf[pos++] & 0xFF;
        }

        private int readU2() throws IOException {
            return (readU1() << 8) | readU1();
        }

        private int readInt() throws IOException {
            return (readU1() << 24) | (readU1() << 16) | (readU1() << 8) | readU1();
        }

        private long readLong() throws IOException {
            long v = 0;
            for (int i = 0; i < 8; i++) {
                v = (v << 8) | readU1();
            }
            return v;
        }

        // Decodes modified UTF-8 with the rules ObjectInputStream applies to class and field names, overlong
        // sequences included, so that every decision taken on a name sees the name that is later resolved.
        private String readUtf() throws IOException {
            final int start = pos;
            final int utfLen = readU2();
            require(utfLen);
            pos += utfLen;
            try {
                return DataInputStream.readUTF(new DataInputStream(new ByteArrayInputStream(buf, start, 2 + utfLen)));
            } catch (UTFDataFormatException e) {
                throw corrupt("malformed modified UTF-8 name");
            }
        }

        private void skip(long n) throws IOException {
            if (n < 0) {
                throw corrupt("negative length");
            }
            require(n);
            pos += (int) n;
        }

        private void require(long n) throws IOException {
            if (n > (long) (len - pos)) {
                throw corrupt("truncated stream");
            }
        }

        private static StreamCorruptedException corrupt(String message) {
            return new StreamCorruptedException("Serialised data rejected by the stream scanner: " + message);
        }
    }
}
