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
 * Copyright 2023-2026 3A Systems LLC.
 */

package com.iplanet.jato.util;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

public class Encoder {

    private final static Debug debug = Debug.getInstance("amConsole");
    private Encoder() {
    }

    public static String encode(byte[] bytes) {
        return encodeHttp64(bytes, Integer.MAX_VALUE);
    }

    public static byte[] decode(String s) {
        return decodeHttp64(s);
    }

    public static String encodeBase64(byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    public static byte[] decodeBase64(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    public static String encodeHttp64(byte[] bytes, int compressThreshold) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    public static byte[] decodeHttp64(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    public static byte[] compress(byte[] in) {
        if (in.length == 0) {
            return in;
        } else {
            Deflater deflater = new Deflater(9);
            byte[] result = new byte[in.length + 1];
            deflater.setInput(in, 0, in.length);
            deflater.finish();
            int compressedLength = deflater.deflate(result, 1, in.length);
            if (compressedLength > 0 && deflater.finished()) {
                result[0] = (byte)((in.length + 1024 - 1) / 1024);
                return subBuffer(result, 0, compressedLength + 1);
            } else {
                result[0] = 0;
                System.arraycopy(in, 0, result, 1, in.length);
                return result;
            }
        }
    }

    public static byte[] decompress(byte[] in) {
        if (in.length < 2) {
            return in;
        } else if (in[0] == 0) {
            return subBuffer(in, 1, in.length - 1);
        } else {
            int bufferSize = 1024 * in[0];
            byte[] buffer = new byte[bufferSize];

            try {
                Inflater inflater = new Inflater();
                inflater.setInput(in, 1, in.length - 1);
                bufferSize = inflater.inflate(buffer, 0, bufferSize);
                if (bufferSize == 0 || !inflater.finished()) {
                    throw new RuntimeException("Decompression failed");
                }
            } catch (DataFormatException var4) {
                throw new WrapperRuntimeException(var4);
            }

            return subBuffer(buffer, 0, bufferSize);
        }
    }

    private static byte[] subBuffer(byte[] in, int idx, int len) {
        byte[] out = new byte[len];
        System.arraycopy(in, idx, out, 0, len);
        return out;
    }

    public static byte[] serialize(Serializable o, boolean compress) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        DeflaterOutputStream dos = null;
        ObjectOutputStream oos = null;
        if (compress) {
            dos = new DeflaterOutputStream(baos, new Deflater(9));
            oos = new ObjectOutputStream(dos);
        } else {
            oos = new ObjectOutputStream(baos);
        }

        oos.writeObject(o);
        oos.flush();
        oos.close();
        if (dos != null) {
            dos.finish();
            dos.close();
        }

        return baos.toByteArray();
    }

    public static Object deserialize(byte[] b, boolean compressed) throws IOException, ClassNotFoundException {
        if(debug.messageEnabled()) {
            String trace = StackWalker.getInstance()
                .walk(frames -> frames
                        .skip(1).limit(3)
                        .map(f -> String.format("%s.%s(%s:%d)",
                                f.getClassName(), f.getMethodName(),
                                f.getFileName(), f.getLineNumber()))
                        .collect(Collectors.joining("; ")));
            debug.message("Encoder:deserialize callers trace: " + trace);
        }
        return IOUtils.deserialise(b, compressed);
    }
}

