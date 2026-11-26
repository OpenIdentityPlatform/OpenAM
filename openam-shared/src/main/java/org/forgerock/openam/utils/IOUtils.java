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
 * Copyright 2011-2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.utils;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.zip.InflaterInputStream;

import org.forgerock.util.Reject;

/**
 * Utility class for handling I/O streams.
 */
public final class IOUtils {

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final int BUFFER_SIZE = 8 * 1024; // 8kB

    private IOUtils() {
    }

    /**
     * Reads the file content at the given path, and returns its UTF-8 content
     * in String format.
     *
     * @param path The path to the file
     * @return content of the file at the given path
     * @throws IOException if an error occurs while reading the file
     * @throws IllegalArgumentException if the file does not exists at the given
     * path
     */
    public static String getFileContent(String path) throws IOException {
        return getFileContent(path, DEFAULT_ENCODING);
    }

    /**
     * Reads the file content at the given path, and returns its content in
     * String format using the given encoding.
     *
     * @param path The path to the file
     * @param encoding encoding of the file
     * @return content of the file at the given path
     * @throws IOException if an error occurs while reading the file
     * @throws IllegalArgumentException if the file does not exists at the given
     * path
     */
    public static String getFileContent(String path, String encoding) throws IOException {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException("The supplied file: " + path
                    + " either does not exists, or is not readable");
        }

        return readStream(new FileInputStream(file), encoding);
    }

    /**
     * Reads a files content from the classpath using UTF-8 encoding.
     *
     * @param classInSameModule A class that is in the same module than the resource.
     * @param path the path to the file on the classpath
     * @return the content of the file
     * @throws IOException if an error occured while reading
     * @throws IllegalArgumentException if the file does not exists
     */
    public static String getFileContentFromClassPath(Class classInSameModule, String path) throws IOException {
        return getFileContentFromClassPath(classInSameModule, path, DEFAULT_ENCODING);
    }

    /**
     * Reads a files content from the classpath using the given encoding.
     *
     * @param classInSameModule A class that is in the same module than the resource.
     * @param path the path to the file on the classpath
     * @param encoding encoding of the file
     * @return the content of the file
     * @throws IOException if an error occured while reading
     * @throws IllegalArgumentException if the file does not exists
     */
    public static String getFileContentFromClassPath(Class classInSameModule, String path, String encoding)
            throws IOException {
        InputStream is = classInSameModule.getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("The supplied file: " + path
                    + " is missing from the classpath");
        }

        return readStream(is, encoding);
    }

    /**
     * Reads the InputStream and tries to interpret its content as String using
     * UTF-8 encoding.
     * @param is the inputstream to read
     * @return The string representation of the inputstreams content
     * @throws IOException if there was an error while reading the stream
     */
    public static String readStream(InputStream is) throws IOException {
        return readStream(is, DEFAULT_ENCODING);
    }

    /**
     * Reads the InputStream and tries to interpret its content as String using
     * the given encoding.
     * @param is the inputstream to read
     * @param encoding the encoding to be used
     * @return The string representation of the inputstreams content
     * @throws IOException if there was an error while reading the stream
     */
    public static String readStream(InputStream is, String encoding) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, encoding));
            while ((line = br.readLine()) != null) {
                content.append(line).append('\n');
            }
        } finally {
            closeIfNotNull(br);
        }
        return content.toString();
    }

    /**
     * Writes the provided content to a file with the provided name.
     *
     * @param fileName The name of the file to write to.
     * @param content The contents to write.
     * @throws IOException If the file could be written to.
     */
    public static void writeToFile(String fileName, String content) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(fileName);
            fileWriter.write(content);
        } finally {
            closeIfNotNull(fileWriter);
        }
    }

    /**
     * Closes the passed in resource if not null.
     *
     * @param closeable The resource that needs to be closed.
     */
    public static void closeIfNotNull(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Closes all of the provided {@link Closeable}s and swallows any exceptions.
     *
     * <p>Also performs a {@code null} check on the {@code Closeable}.</p>
     *
     * @param o The {@code Closeable}s.
     */
    public static void closeIfNotNull(Closeable... o) {
        if (o == null) {
            return;
        }
        for (Closeable c : o) {
            closeIfNotNull(c);
        }
    }

    /**
     * Copies the whole input stream to the given output stream. Neither stream is closed after the copy.
     *
     * @param in the stream to copy from.
     * @param out the stream to copy to.
     * @return the number of bytes copied.
     * @throws IOException if an error occurs while copying the data.
     */
    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        Reject.ifNull(in, out);
        final byte[] buffer = new byte[BUFFER_SIZE];
        long bytesCopied = 0L;

        for (int bytesRead = in.read(buffer); bytesRead != -1; bytesRead = in.read(buffer)) {
            out.write(buffer, 0, bytesRead);
            bytesCopied += bytesRead;
        }

        return bytesCopied;
    }

    /**
     * The collection-nesting depth that console and JATO page state legitimately reaches: the page-session or
     * serialized-field map holds per-view attribute-value maps whose values are sets of strings
     * (map -&gt; map -&gt; set). Console callers pass this to
     * {@link #deserialise(byte[], boolean, ClassLoader, long)}; the default limit stays lower so the
     * unauthenticated protocol endpoints keep the tighter bound.
     */
    public static final long CONSOLE_STATE_MAX_COLLECTION_DEPTH = 3L;

    /**
     * Deserialises an object from a byte array to an object of a specified type.
     *
     * @param bytes The bytes that represent the Object to be deserialized. The classes to be loaded must be from the
     *              set specified in the whitelist
     * @param compressed If true, expect that the bytes are compressed.
     * @param <T> The returned object type.
     * @return The Object T representing the deserialized bytes
     * @throws IOException If there was a problem with the ObjectInputStream process.
     * @throws ClassNotFoundException If there was problem loading a class that makes up the bytes to be deserialized.
     */
    public static <T> T deserialise(byte[] bytes, boolean compressed) throws IOException, ClassNotFoundException {
        return deserialise(bytes, compressed, null);
    }

    /**
     * Deserialises an object from a byte array to an object of a specified type.
     *
     * @param bytes The bytes that represent the Object to be deserialized. The classes to be loaded must be from the
     *              set specified in the whitelist maintained in the <code>WhitelistObjectInputStream</code>
     * @param compressed If true, expect that the bytes are compressed.
     * @param classLoader Used in place of the default ClassLoader, default will be used if null.
     * @param <T> The returned object type.
     * @return The Object T representing the deserialized bytes
     * @throws IOException If there was a problem with the ObjectInputStream process.
     * @throws ClassNotFoundException If there was problem loading a class that makes up the bytes to be deserialized.
     */
    public static <T> T deserialise(byte[] bytes, boolean compressed, ClassLoader classLoader) throws IOException,
            ClassNotFoundException {
        return deserialise(bytes, compressed, classLoader, null);
    }

    /**
     * Deserialises an object whose legitimate payload nests collections more deeply than the default
     * {@code maxcollectiondepth} limit allows. The given depth lifts the built-in default for that one limit
     * only: a {@code maxcollectiondepth} configured explicitly through {@link Constants#DESERIALISATION_LIMITS}
     * wins in both directions, and every other limit and the class whitelist apply unchanged. Only callers on
     * authenticated paths whose payload shape is known, such as the console page state, should pass a raised
     * depth.
     *
     * @param bytes The bytes that represent the Object to be deserialized. The classes to be loaded must be from the
     *              set specified in the whitelist maintained in the <code>WhitelistObjectInputStream</code>
     * @param compressed If true, expect that the bytes are compressed.
     * @param classLoader Used in place of the default ClassLoader, default will be used if null.
     * @param maxCollectionDepth The collection-nesting depth this caller's payload legitimately reaches,
     *                           applied as a minimum for the {@code maxcollectiondepth} limit.
     * @param <T> The returned object type.
     * @return The Object T representing the deserialized bytes
     * @throws IOException If there was a problem with the ObjectInputStream process.
     * @throws ClassNotFoundException If there was problem loading a class that makes up the bytes to be deserialized.
     */
    public static <T> T deserialise(byte[] bytes, boolean compressed, ClassLoader classLoader,
            long maxCollectionDepth) throws IOException, ClassNotFoundException {
        return deserialise(bytes, compressed, classLoader, null, maxCollectionDepth);
    }

    /**
     * Deserialises an object, consulting the given upstream filter in addition to the built-in limits and class
     * whitelist. This overload exists so that a test can supply a filter in place of the JVM-wide
     * {@code jdk.serialFilter} (which {@link ObjectInputStream#getObjectInputFilter()} would otherwise return);
     * production callers use the public overloads, which pass {@code null} here so that the stream's configured
     * JVM filter, if any, is honoured.
     *
     * @param bytes The bytes that represent the Object to be deserialized.
     * @param compressed If true, expect that the bytes are compressed.
     * @param classLoader Used in place of the default ClassLoader, default will be used if null.
     * @param upstreamFilter A filter consulted after the built-in limits, or null to fall back to the stream's
     *                       configured JVM filter.
     * @param <T> The returned object type.
     * @return The Object T representing the deserialized bytes.
     * @throws IOException If there was a problem with the ObjectInputStream process.
     * @throws ClassNotFoundException If there was problem loading a class that makes up the bytes to be deserialized.
     */
    static <T> T deserialise(byte[] bytes, boolean compressed, ClassLoader classLoader,
            ObjectInputFilter upstreamFilter) throws IOException, ClassNotFoundException {
        return deserialise(bytes, compressed, classLoader, upstreamFilter, 0L);
    }

    private static <T> T deserialise(byte[] bytes, boolean compressed, ClassLoader classLoader,
            ObjectInputFilter upstreamFilter, long collectionDepthFloor) throws IOException, ClassNotFoundException {

        final DeserialisationLimits limits = DeserialisationLimits.current()
                .withCollectionDepthFloor(collectionDepthFloor);

        // Decompress up front, bounded by maxBytes, so that both the scanner and the deserialiser see the same
        // bounded bytes and a small compressed payload cannot expand into an unbounded stream.
        final byte[] data;
        if (compressed) {
            final ByteArrayOutputStream inflated = new ByteArrayOutputStream();
            try (InputStream in = new SizeLimitedInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)),
                    limits.maxBytes)) {
                copyStream(in, inflated);
            }
            data = inflated.toByteArray();
        } else {
            if ((long) bytes.length > limits.maxBytes) {
                throw new StreamCorruptedException("Serialised data exceeds the limit of " + limits.maxBytes
                        + " bytes");
            }
            data = bytes;
        }

        // Pre-scan the bytes in a single linear pass, before any class is loaded or instantiated. This rejects a
        // back-reference DAG whose reconstruction cost the JEP 290 filter cannot see (the filter is handed a
        // back-reference with serialClass() == null). Like a filter rejection, a rejection is logged as a warning
        // and raised as an IOException.
        try {
            SerialStreamScanner.scan(data, limits.maxArray, limits.maxExpandedRefs, limits.maxWeakHashKeys);
        } catch (IOException e) {
            DeserialisationLimits.DEBUG.warning("IOUtils.deserialise: rejected serialised stream, " + e.getMessage());
            throw e;
        }

        // Defence in depth: deserialise the same bytes through the class whitelist, the JEP 290 filter and every
        // numeric limit, including maxCollectionDepth.
        final ObjectInputStream ois = new WhitelistObjectInputStream(
                new SizeLimitedInputStream(new ByteArrayInputStream(data), limits.maxBytes), classLoader, limits,
                upstreamFilter);

        final T result;
        try {
            result = (T) ois.readObject();
        } finally {
            closeIfNotNull(ois);
        }

        return result;
    }

    /**
     * Resource limits applied to every {@link #deserialise} call, read from
     * {@link Constants#DESERIALISATION_LIMITS}. The property uses the JEP 290 limit syntax, for example
     * {@code maxdepth=20;maxrefs=100000;maxexpandedrefs=100000}; limits that are not set keep their defaults.
     */
    static final class DeserialisationLimits {

        static final long DEFAULT_MAX_DEPTH = 20L;
        static final long DEFAULT_MAX_REFS = 100000L;
        static final long DEFAULT_MAX_ARRAY = 100000L;
        static final long DEFAULT_MAX_BYTES = 10L * 1024 * 1024;
        // The depth to which hash-based collections may be nested inside one another, checked by the JEP 290
        // filter during deserialisation as a secondary bound. It measures the collections open at once in the
        // stream, so it does not see a back-reference DAG; the primary bound on reconstruction cost is
        // maxExpandedRefs, enforced by the up-front SerialStreamScanner. See exceedsCollectionNesting.
        static final long DEFAULT_MAX_COLLECTION_DEPTH = 2L;
        // The maximum size of the object graph once it is expanded into a tree from the root, enforced by
        // SerialStreamScanner before anything is deserialised. It bounds the objects the recursive hashCode()
        // calls visit while hash-based collections are rebuilt, counting each shared or back-referenced node once
        // per path that reaches it; the equals() calls between colliding keys are bounded by maxWeakHashKeys
        // instead. The default is well above the expanded size of legitimate OpenAM payloads: the
        // console page state and the IOUtilsTest fixtures measure in the tens, and a remote-request-shaped header
        // map (the PLL session and authentication-service requests themselves live in openam-core and were
        // approximated) in the low hundreds; it stays far below the millions of nodes a DoS graph expands to.
        static final long DEFAULT_MAX_EXPANDED_REFS = 100000L;
        // The number of keys one HashMap (or elements one HashSet) may have when they are not all of one class
        // that keeps colliding keys ordered (see SerialStreamScanner). Such keys that share a hash code are
        // compared with every key of their bin on each insertion, so the rebuild costs up to this limit times
        // the number of references in the stream. The legitimate unordered sets (the Accept-Language locales and
        // the cookies of a remote authentication request) hold a handful of entries.
        static final long DEFAULT_MAX_WEAK_HASH_KEYS = 32L;

        private static final Debug DEBUG = Debug.getInstance("amUtil");

        final long maxDepth;
        final long maxRefs;
        final long maxArray;
        final long maxBytes;
        final long maxCollectionDepth;
        final long maxExpandedRefs;
        final long maxWeakHashKeys;
        // Whether maxCollectionDepth was set explicitly through the property, in which case a caller's
        // floor must not override the operator's choice.
        final boolean collectionDepthConfigured;

        private DeserialisationLimits(long maxDepth, long maxRefs, long maxArray, long maxBytes,
                long maxCollectionDepth, long maxExpandedRefs, long maxWeakHashKeys,
                boolean collectionDepthConfigured) {
            this.maxDepth = maxDepth;
            this.maxRefs = maxRefs;
            this.maxArray = maxArray;
            this.maxBytes = maxBytes;
            this.maxCollectionDepth = maxCollectionDepth;
            this.maxExpandedRefs = maxExpandedRefs;
            this.maxWeakHashKeys = maxWeakHashKeys;
            this.collectionDepthConfigured = collectionDepthConfigured;
        }

        // Read every time, like the class whitelist, so that a change does not need a restart
        static DeserialisationLimits current() {
            return parse(SystemPropertiesManager.get(Constants.DESERIALISATION_LIMITS));
        }

        // A caller-supplied minimum for maxCollectionDepth. It only lifts the built-in default: a value the
        // operator configured explicitly wins in both directions, and so does a larger default.
        DeserialisationLimits withCollectionDepthFloor(long floor) {
            return collectionDepthConfigured || floor <= maxCollectionDepth ? this
                    : new DeserialisationLimits(maxDepth, maxRefs, maxArray, maxBytes, floor, maxExpandedRefs,
                            maxWeakHashKeys, false);
        }

        static DeserialisationLimits parse(String property) {
            long maxDepth = DEFAULT_MAX_DEPTH;
            long maxRefs = DEFAULT_MAX_REFS;
            long maxArray = DEFAULT_MAX_ARRAY;
            long maxBytes = DEFAULT_MAX_BYTES;
            long maxCollectionDepth = DEFAULT_MAX_COLLECTION_DEPTH;
            long maxExpandedRefs = DEFAULT_MAX_EXPANDED_REFS;
            long maxWeakHashKeys = DEFAULT_MAX_WEAK_HASH_KEYS;
            boolean collectionDepthConfigured = false;
            if (StringUtils.isNotBlank(property)) {
                for (String limit : property.split(";")) {
                    final int eq = limit.indexOf('=');
                    final String name = eq < 0 ? limit.trim() : limit.substring(0, eq).trim();
                    long value;
                    try {
                        value = eq < 0 ? -1L : Long.parseLong(limit.substring(eq + 1).trim());
                    } catch (NumberFormatException e) {
                        value = -1L;
                    }
                    if (value <= 0) {
                        DEBUG.warning("DeserialisationLimits: ignoring invalid limit " + limit);
                    } else if ("maxdepth".equals(name)) {
                        maxDepth = value;
                    } else if ("maxrefs".equals(name)) {
                        maxRefs = value;
                    } else if ("maxarray".equals(name)) {
                        maxArray = value;
                    } else if ("maxbytes".equals(name)) {
                        maxBytes = value;
                    } else if ("maxcollectiondepth".equals(name)) {
                        maxCollectionDepth = value;
                        collectionDepthConfigured = true;
                    } else if ("maxexpandedrefs".equals(name)) {
                        maxExpandedRefs = value;
                    } else if ("maxweakhashkeys".equals(name)) {
                        maxWeakHashKeys = value;
                    } else {
                        DEBUG.warning("DeserialisationLimits: ignoring unknown limit " + limit);
                    }
                }
            }
            return new DeserialisationLimits(maxDepth, maxRefs, maxArray, maxBytes, maxCollectionDepth,
                    maxExpandedRefs, maxWeakHashKeys, collectionDepthConfigured);
        }
    }

    /**
     * Fails the read once more than the given number of bytes has been consumed. Unlike the {@code maxbytes}
     * limit of a JEP 290 filter, which is only checked when an object, array or class is read, this also stops a
     * single huge string or block of data.
     */
    private static final class SizeLimitedInputStream extends FilterInputStream {

        private final long maxBytes;
        private long bytesRead;

        SizeLimitedInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            final int b = super.read();
            if (b >= 0) {
                count(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final int n = super.read(b, off, len);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            final long skipped = super.skip(n);
            if (skipped > 0) {
                count(skipped);
            }
            return skipped;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        private void count(long n) throws IOException {
            bytesRead += n;
            if (bytesRead > maxBytes) {
                throw new StreamCorruptedException("Serialised data exceeds the limit of " + maxBytes + " bytes");
            }
        }
    }

    /**
     * When dealing with Object deserialisation, this class provides protection from classes outside of the Whitelist
     * being loaded unexpectedly, and from object graphs that are too deep, too large or contain too long arrays.
     */
    private static class WhitelistObjectInputStream extends ObjectInputStream {

        private final ClassLoader classLoader;
        private final List<String> classWhitelist;
        private final DeserialisationLimits limits;
        // The filter the JVM configured for this stream (jdk.serialFilter), consulted as well as the limits
        private final ObjectInputFilter jvmFilter;
        // Stream depths at which a still-open hash-based collection was entered, deepest last. Its size is the
        // current hash-collection nesting; frames whose depth is no longer below the current depth have closed.
        private final Deque<Integer> collectionFrames = new ArrayDeque<>();

        private static final Debug DEBUG = Debug.getInstance("amUtil");

        // Any class name that starts with this flag indicates an Object/Primitive array so allow.
        // The Object in the array will trigger a followup validation call.
        private static final String ARRAY_FLAG = "[";

        // These are the bare minimum set of classes that are needed for the JATO framework and TokenRestriction
        private static final List<String> FALLBACK_CLASS_WHITELIST = Arrays.asList(
                    "com.iplanet.dpro.session.DNOrIPAddressListTokenRestriction",
                    "com.sun.identity.console.base.model.SMSubConfig",
                    "com.sun.identity.console.service.model.SMDescriptionData",
                    "com.sun.identity.console.service.model.SMDiscoEntryData",
                    "com.sun.identity.console.session.model.SMSessionData",
                    "com.sun.identity.shared.datastruct.OrderedSet",
                    "java.lang.Boolean",
                    "java.lang.Integer",
                    "java.lang.Number",
                    "java.lang.String",
                    "java.net.InetAddress",
                    "java.util.ArrayList",
                    "java.util.Collections$EmptyMap",
                    "java.util.Collections$SingletonList",
                    // Sorted (tree-based) collections such as TreeSet/TreeMap are deliberately not whitelisted:
                    // their readObject raises no per-instance backing-array check, so neither the collection
                    // nesting limit nor the array-length limit can see them. Only collection types whose
                    // reconstruction validates a backing array (hash- and array-based ones) may be listed here,
                    // and the same rule applies to entries added through the whitelist property.
                    "java.util.HashMap","java.util.LinkedHashMap","java.util.Collections$UnmodifiableMap",
                    "java.util.HashSet",
                    "org.forgerock.openam.dpro.session.NoOpTokenRestriction",
                    "org.apache.catalina.util.ParameterMap","jakarta.servlet.DispatcherType","java.lang.Enum",
                    "org.forgerock.openam.authentication.service.protocol.RemoteHttpServletRequest",
                    "org.forgerock.openam.authentication.service.protocol.RemoteHttpServletResponse",
                    "org.forgerock.openam.authentication.service.protocol.RemoteServletRequest",
                    "org.forgerock.openam.authentication.service.protocol.RemoteServletResponse",
                    "java.util.Locale",
                    "org.forgerock.openam.authentication.service.protocol.RemoteCookie",
                    "com.sun.identity.common.CaseInsensitiveHashSet",
                    "com.sun.identity.common.CaseInsensitiveKey",
                    "com.sun.identity.common.CaseInsensitiveHashMap",
                    "org.forgerock.openam.authentication.service.protocol.RemoteSession",
                    "java.lang.StringBuffer"
                    );

        public WhitelistObjectInputStream(InputStream in, ClassLoader classLoader, DeserialisationLimits limits,
                ObjectInputFilter upstreamFilter) throws IOException {
            super(in);
            this.classLoader = classLoader;
            this.limits = limits;
            // Fall back to whatever the JVM configured for this stream (jdk.serialFilter) when no filter is injected.
            this.jvmFilter = upstreamFilter != null ? upstreamFilter : getObjectInputFilter();
            setObjectInputFilter(this::checkInput);
            // Read this every time to avoid having to do a restart to pick up any changes to the list
            final String property = SystemPropertiesManager.get(Constants.DESERIALISATION_CLASSES_WHITELIST);
            // The list is stored as a comma delimited String, use fallback list if null or empty
            classWhitelist = StringUtils.isEmpty(property)
                    ? FALLBACK_CLASS_WHITELIST
                    : CollectionUtils.asList(property.split(","));
            if (DEBUG.messageEnabled()) {
                DEBUG.message("WhitelistObjectInputStream: using class whitelist:" + classWhitelist);
            }
        }

        private ObjectInputFilter.Status checkInput(ObjectInputFilter.FilterInfo info) {
            // Close collection frames the stream has left on every filter call (classes, arrays and
            // back-references all report their depth), not only when the next backing array arrives:
            // otherwise a closed sibling collection's frame would still be on the stack when a later,
            // deeper branch is measured, and the nesting would be overcounted.
            while (!collectionFrames.isEmpty() && collectionFrames.peek() >= info.depth()) {
                collectionFrames.pop();
            }
            if (info.depth() > limits.maxDepth || info.references() > limits.maxRefs
                    || info.arrayLength() > limits.maxArray) {
                DEBUG.warning("WhitelistObjectInputStream.checkInput: rejected object graph exceeding the limits,"
                        + " depth " + info.depth() + ", references " + info.references()
                        + ", array length " + info.arrayLength());
                return ObjectInputFilter.Status.REJECTED;
            }
            if (exceedsCollectionNesting(info)) {
                return ObjectInputFilter.Status.REJECTED;
            }
            return jvmFilter == null ? ObjectInputFilter.Status.UNDECIDED : jvmFilter.checkInput(info);
        }

        /**
         * A secondary bound on how deeply collections and maps may be nested inside one another, checked as the
         * stream is deserialised. It counts the collections open at the same time in the stream, so it sees the
         * shape of a directly-nested graph but not a back-reference DAG: a repeated collection reaches the filter
         * with {@code serialClass() == null} once its class descriptor has been seen, and a back-reference reaches
         * it with no target at all. The bound that accounts for back-references, and the primary defence against
         * the reconstruction cost of a nested-collection graph, is {@code maxexpandedrefs}, enforced by
         * {@link SerialStreamScanner} before any object is read. This check remains as defence in depth and to
         * keep the directly-nested case tight for the console page-state callers.
         * <p>
         * The nesting is measured from the internal-capacity check that {@code HashMap}, {@code HashSet} and
         * {@code ArrayList} raise from their {@code readObject} (a {@code Map.Entry[]} or {@code Object[]} filter
         * call, present since JEP 290). Unlike the collection's own class, which the stream shares across instances
         * and so presents to the filter only once, that array check fires once per collection instance and carries
         * the collection's stream depth, so it gives a reliable per-instance signal. A frame stays open while later
         * objects are read below it, and closes once the depth returns to or above the depth at which it was
         * entered; {@code checkInput} prunes closed frames on every filter call.
         */
        private boolean exceedsCollectionNesting(ObjectInputFilter.FilterInfo info) {
            final Class<?> clazz = info.serialClass();
            if (clazz == null || !clazz.isArray()) {
                return false;
            }
            final Class<?> component = clazz.getComponentType();
            // The backing arrays that HashMap/HashSet (Map.Entry[]) and ArrayList/Vector (Object[]) validate as
            // they are reconstructed. Sorted and linked collections raise no such per-instance check, which is
            // why the whitelist must not contain them (see FALLBACK_CLASS_WHITELIST): an instance the filter
            // never sees would be counted neither here nor by the array-length limit.
            if (component != java.util.Map.Entry.class && component != Object.class) {
                return false;
            }
            final int depth = (int) info.depth();
            if (collectionFrames.size() + 1L > limits.maxCollectionDepth) {
                DEBUG.warning("WhitelistObjectInputStream.checkInput: rejected collection graph nested deeper than "
                        + limits.maxCollectionDepth + " (" + clazz.getName() + " at depth " + depth + ")");
                return true;
            }
            collectionFrames.push(depth);
            return false;
        }

        @Override
        protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
            // None of the whitelisted types is a dynamic proxy
            DEBUG.warning("WhitelistObjectInputStream.resolveProxyClass: rejected proxy class for "
                    + Arrays.toString(interfaces));
            throw new InvalidClassException("Dynamic proxy classes are not allowed");
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {

            final Class<?> result;
            final String classToLoad = desc.getName();

            if (isValidClass(classToLoad)) {
                result = (classLoader == null)
                    ? Class.forName(classToLoad)
                    : Class.forName(classToLoad, true, classLoader);
            } else {
                DEBUG.warning("WhitelistObjectInputStream.resolveClass:" + classToLoad
                        + " was not in the whitelist of allowed classes");
                throw new InvalidClassException(classToLoad, "Requested ObjectStreamClass was not in the "
                        + "whitelist of allowed classes");
            }

            return result;
        }

        private boolean isValidClass(String classToLoad) {

            final boolean result;

            // All Object/primitive arrays are valid by default, the contents will be validated in future calls
            result = classToLoad.startsWith(ARRAY_FLAG) || classWhitelist.contains(classToLoad);

            if (DEBUG.messageEnabled()) {
                DEBUG.message("WhitelistObjectInputStream.isValidClass:" + classToLoad + " " + result);
            }

            return result;
        }
    }
}
