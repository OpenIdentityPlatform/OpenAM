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
 */
package com.iplanet.dpro.session;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for modelling the extensions which contain information about the platform
 * environment the SessionID was created in. A SessionID is closely tied to the Server ID
 * and Site ID which issued the Session.
 *
 * This logic has been specifically encapsulated to ensure that changes to the server/site
 * environment can be accounted for in the SessionID extensions. Therefore
 * SessionIDExtensions will manage the Server ID, Site ID and Storage Key concepts
 * which are used by a SessionID.
 */
public class LegacySessionIDExtensions implements SessionIDExtensions {
    // prefix "S" is reserved to be used by session framework-specific
    // extensions for session id format
    public static final String PRIMARY_ID = "S1";
    public static final String STORAGE_KEY = "SK";
    public static final String SITE_ID = "SI";

    private static final Debug debug = Debug.getInstance("amSession");
    private final Map<String, String> extensionsMap;

    public LegacySessionIDExtensions(Map<String, String> extensionsMap) {
        this.extensionsMap = extensionsMap;
    }

    public LegacySessionIDExtensions(String primary, String site, String storage) {
        this();
        extensionsMap.put(PRIMARY_ID, primary);
        extensionsMap.put(SITE_ID, site);
        extensionsMap.put(STORAGE_KEY, storage);
    }

    public LegacySessionIDExtensions() {
        this(new HashMap<String, String>());
    }

    /**
     * Given a SessionID encoded extension string, parse the contents and generate
     * the extension map.
     *
     * Note: Optimised alternative to DataInputStream#readUTF (which is very slow).
     *
     * @param extensionPart the encoded extension map part of SessionID.
     * @throws IOException if there is an error decoding the extensions
     */
    public LegacySessionIDExtensions(String extensionPart) throws IOException {
        // The bytes are actually written via DataOutputStream#writeUTF which uses "modified UTF-8". We decode this
        // as normal UTF-8. This should only cause an issue with 'null' characters (\u0000), which should never
        // appear anyway.
        final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        extensionsMap = new HashMap<>();
        final byte[] bytes = Base64.decode(extensionPart);

        if (bytes == null) {
            debug.message("SessionID.readExtensions: Invalid extension data {}", extensionPart);
            throw new IllegalArgumentException("Invalid Base64-encoded data");
        }

        for (int i = 0; i < bytes.length;) {
            // Data is encoded as a 2-byte unsigned short length, followed by 'length' bytes of UTF-8
            int length = parseUnsignedShort(bytes, i);
            i += 2;
            String key = decoder.decode(ByteBuffer.wrap(bytes, i, length)).toString();
            i += length;
            length = parseUnsignedShort(bytes, i);
            i += 2;
            String val = decoder.decode(ByteBuffer.wrap(bytes, i, length)).toString();
            i += length;
            extensionsMap.put(key, val);
        }
    }

    /**
     * See {@link SessionID#validate()} for an explanation of this value.
     * @return Possibly null Primary ID
     */
    @Override
    public String getPrimaryID() {
        return extensionsMap.get(PRIMARY_ID);
    }

    /**
     * See {@link SessionID#validate()} for an explanation of this value.
     * @return Possibly null Site ID
     */
    @Override
    public String getSiteID() {
        return extensionsMap.get(SITE_ID);
    }

    /**
     * Storage key is used primarily in Internal Request Routing.
     * @return Possibly null Storage Key ID
     */
    @Override
    public String getStorageKey() {
        return extensionsMap.get(STORAGE_KEY);
    }

    /**
     * @param key Non null key
     * @return Value retrieved from the extensions map.
     */
    @Override
    public String get(String key) {
        return extensionsMap.get(key);
    }

    /**
     * @param key Key to store in extensions map.
     * @param value Value to store in extensions map.
     */
    @Override
    public void add(String key, String value) {
        extensionsMap.put(key, value);
    }

    @Override
    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(extensionsMap);
    }

    /**
     * Parses the next two bytes from the given byte array as an unsigned short value. As Java does not support
     * unsigned types, we return the value as the least-significant bits of a signed integer. The most-significant
     * 16 bits of the result will always be 0. Assumes Big-Endian format as in DataInput#readUnsignedShort.
     *
     * @param bytes the byte array to read the unsigned short from.
     * @param i the offset into the byte array of the start of the unsigned short.
     * @return the unsigned short as a (positive) signed integer.
     */
    private static int parseUnsignedShort(final byte[] bytes, final int i) {
        return ((bytes[i] & 0xFF) << 8) | (bytes[i+1] & 0xFF);
    }

    @Override
    public String toString() {
        return MessageFormat.format("S1:{0}, SI:{1}, SK:{2}", getPrimaryID(), getSiteID(), getStorageKey());
    }
}
