package com.iplanet.jato.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


public class Encoder {
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
        byte[] result;
        if (bytes.length > compressThreshold) {
            result = compress(bytes);
        } else {
            result = new byte[bytes.length + 1];
            result[0] = 0;
            System.arraycopy(bytes, 0, result, 1, bytes.length);
        }
        return Base64.getUrlEncoder().encodeToString(result);
    }

    public static byte[] decodeHttp64(String s) {
        byte[] result = Base64.getUrlDecoder().decode(s);
        return decompress(result);
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
        ObjectOutputStream oos;
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
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        InflaterInputStream iis;
        ObjectInputStream ois;
        if (compressed) {
            iis = new InflaterInputStream(bais);
            ois = new ApplicationObjectInputStream(iis);
        } else {
            ois = new ApplicationObjectInputStream(bais);
        }

        Object result = ois.readObject();
        return result;
    }
}
