/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Streamer.java,v 1.4 2009/10/15 07:08:28 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A utility class that can stream to and from various streams and records.
 *
 * @author Paul C. Bryan
 */
public class Streamer
{
    /** Size of buffer to use during streaming. */
    private static final int BUF_SIZE = 8 * 1024; // 8 KiB buffer

    /**
     * Streams all data from an input stream to an output stream.
     *
     * @param in the input stream to stream the data from.
     * @param out the output stream to stream the data to.
     * @throws IOException if an I/O exception occurs.
     */
    public static void stream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while ((n = in.read(buf, 0, BUF_SIZE)) != -1) {
            out.write(buf, 0, n);
        }
    }

    /**
     * Streams data from an input stream to an output stream, up to a specified
     * length.
     *
     * @param in the input stream to stream the data from.
     * @param out the output stream to stream the data to.
     * @param len the number of bytes to stream.
     * @return the actual number of bytes streamed.
     * @throws IOException if an I/O exception occurs.
     */
    public static int stream(InputStream in, OutputStream out, int len) throws IOException {
        int remain = len;
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while (remain > 0 && (n = in.read(buf, 0, Math.min(remain, BUF_SIZE))) >= 0) {
            out.write(buf, 0, n);
            remain -= n;
        }
        return len - remain;
    }

    /**
     * Streams all data from a record to an output stream.
     *
     * @param in the record to stream the data from.
     * @param out the output stream to stream the data to.
     * @throws IOException if an I/O exception occurs.
     */
    public static void stream(Record in, OutputStream out) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while ((n = in.read(buf, 0, BUF_SIZE)) != -1) {
            out.write(buf, 0, n);
        }
    }

    /**
     * Streams data from a record to an output stream, up to a specified
     * length.
     *
     * @param in the record to stream the data from.
     * @param out the output stream to stream the data to.
     * @param len the number of bytes to stream.
     * @return the actual number of bytes streamed.
     * @throws IOException if an I/O exception occurs.
     */
    public static int stream(Record in, OutputStream out, int len) throws IOException {
        int remain = len;
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while (remain > 0 && (n = in.read(buf, 0, Math.min(remain, BUF_SIZE))) >= 0) {
            out.write(buf, 0, n);
            remain -= n;
        }
        return len - remain;
    }

    /**
     * Streams all data from an input stream to a record.
     *
     * @param in the input stream to stream the data from.
     * @param out the record to stream the data to.
     * @throws IOException if an I/O exception occurs.
     */
    public static void stream(InputStream in, Record out) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while ((n = in.read(buf, 0, BUF_SIZE)) != -1) {
            out.write(buf, 0, n);
        }
    }
    
    /**
     * Streams data from an input stream to a record, up to a specified length.
     *
     * @param in the input stream to stream the data from.
     * @param out the record to stream the data to.
     * @param len the number of bytes to stream.
     * @return the actual number of bytes streamed.
     * @throws IOException if an I/O exception occurs.
     */
    public static int stream(InputStream in, Record out, int len) throws IOException {
        int remain = len;
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while (remain > 0 && (n = in.read(buf, 0, Math.min(remain, BUF_SIZE))) >= 0) {
            out.write(buf, 0, n);
            remain -= n;
        }
        return len - remain;
    }

    /**
     * Streams all data from one record to another.
     *
     * @param in the record to stream the data from.
     * @param out the record to stream the data to.
     * @throws IOException if an I/O exception occurs.
     */
    public static void stream(Record in, Record out) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while ((n = in.read(buf, 0, BUF_SIZE)) != -1) {
            out.write(buf, 0, n);
        }
    }

    /**
     * Streams data from an one record to another, up to a specified length.
     *
     * @param in the record to stream the data from.
     * @param out the record to stream the data to.
     * @param len the number of bytes to stream.
     * @return the actual number of bytes streamed.
     * @throws IOException if an I/O exception occurs.
     */
    public static int stream(Record in, Record out, int len) throws IOException {
        int remain = len;
        byte[] buf = new byte[BUF_SIZE];
        int n;
        while (remain > 0 && (n = in.read(buf, 0, Math.min(remain, BUF_SIZE))) >= 0) {
            out.write(buf, 0, n);
            remain -= n;
        }
        return len - remain;
    }
}

