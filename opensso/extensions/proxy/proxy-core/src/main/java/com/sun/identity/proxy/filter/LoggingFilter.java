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
 * $Id: LoggingFilter.java,v 1.2 2009/10/29 20:32:09 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.filter;

import com.sun.identity.proxy.handler.Filter;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Message;
import com.sun.identity.proxy.http.Request;
import com.sun.identity.proxy.http.Response;
import com.sun.identity.proxy.io.CachedInputStream;
import com.sun.identity.proxy.io.OverflowException;
import com.sun.identity.proxy.io.RecordOutputStream;
import com.sun.identity.proxy.io.Streamer;
import com.sun.identity.proxy.io.TemporaryStorage;
import com.sun.identity.proxy.io.Storage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Logs the content of exchanges passing through the filter to a specified
 * output stream or storage. This class can be useful for debugging as well as
 * to observe the exact traffic with a target application before creating an
 * application-specific filter.
 *
 * @author Marjo van Diem
 * @author Paul C. Bryan
 */
public class LoggingFilter extends Filter
{
    /** Operating system specific line separator. */
    private static final byte[] NEWLINE = bytes(System.getProperty("line.separator"));

    /** Bytes to write to represent null values. */
    private static final byte[] NULL = bytes("null");

    /** Bytes to write to provide space. */
    private static final byte[] SPACE = bytes(" ");

    /** Bytes to write to delimit header name from value. */
    private static final byte[] HEADER_DELIMITER = bytes(": ");

    /** Prefix to write before new log entry heading. */
    private static final byte[] HEADING_PREFIX = bytes("----- ");
    
    /** Suffix to write after new log entry heading. */
    private static final byte[] HEADING_SUFFIX = bytes(" -----");

    /** Open input stream to write log data to. */
    private OutputStream out = null;

    /** Counts exchanges passing through filter to serve as identifier. */
    private long counter = 0L;

    /** Allocates records to store logged data into. */
    private Storage logStorage;

    /** Allocates temporary records for caching request and response entities. */
    private TemporaryStorage tmpStorage;

    /** Represents this logging filter instance when writing to headings or records. */
    public String instance = "Exchange";

    /**
     * Creates a new logging filter to write to the specified output stream.
     *
     * @param out the output stream to log exchanges to.
     * @param tmpStorage allocates temporary records for caching request and response entities.
     */
    public LoggingFilter(OutputStream out, TemporaryStorage tmpStorage) {
        this.tmpStorage = tmpStorage;
        this.out = out;
    }

    /**
     * Creates a new logging filter to write messages to. A new storage record
     * is created for each request and response respectively.
     *
     * @param logStorage allocates records to store logged data into.
     * @param tmpStorage allocates temporary records for caching request and response entities.
     */
    public LoggingFilter(Storage logStorage, TemporaryStorage tmpStorage) {
        this.logStorage = logStorage;
        this.tmpStorage = tmpStorage;
    }

    /**
     * Logs the content of the request and response.
     */
    @Override
    public void handle(Exchange exchange) throws HandlerException, IOException {
        String id = newId();
        request(exchange.request, id);
        next.handle(exchange);
        response(exchange.response, id);
    }

    /**
     * Returns a string as an array of bytes, converted using the default
     * encoding and handing null values.
     *
     * @param s the string to return as a byte array.
     * @return the byte array representing the string.
     */
    private static byte[] bytes(String s) {
        return (s == null ? NULL : s.getBytes());
    }

    /**
     * Logs the content of the request message.
     *
     * @param request the request to be logged.
     * @param id the exchange ID to tag the request in the log.
     * @throws IOException if an I/O exception occurs.
     */
    private synchronized void request(Request request, String id) throws IOException {
        OutputStream out = begin("Request", id);
        out.write(bytes(request.method));
        out.write(SPACE);
        out.write(bytes(request.uri != null ? request.uri.toString() : null));
        out.write(SPACE);
        out.write(bytes(request.version));
        out.write(NEWLINE);
        headers(request, out);
        out.write(NEWLINE);
        entity(request, out);
        end(out);
    }

    /**
     * Logs the content of the response message.
     *
     * @param request the request to be logged.
     * @param id the exchange ID to tag the request in the log.
     * @throws IOException if an I/O exception occurs.
     */
    private synchronized void response(Response response, String id) throws IOException {
        OutputStream out = begin("Response", id);
        out.write(bytes(response.version));
        out.write(SPACE);
        out.write(bytes(Integer.toString(response.status)));
        out.write(SPACE);
        out.write(bytes(response.reason));
        out.write(NEWLINE);
        headers(response, out);
        out.write(NEWLINE);
        entity(response, out);
        end(out);
    }
    
    /**
     * Establishes the output stream to write message content to.
     *
     * @param type the type of message to be written.
     * @param id the exchange ID to tag the request in the log.
     * @return the output stream to write the message content to.
     * @throws IOException if an I/O exception occurs.
     */
    private OutputStream begin(String type, String id) throws IOException {
        if (logStorage != null) { // new record will have message written to it verbatim; no header
            return new RecordOutputStream(logStorage.open(logStorage.create(instance + '.' + id + '.' + type)));
        }
        out.write(NEWLINE); // new header in existing output stream
        out.write(HEADING_PREFIX);
        out.write(bytes(instance));
        out.write(SPACE);
        out.write(bytes(id));
        out.write(SPACE);
        out.write(bytes(type));
        out.write(HEADING_SUFFIX);
        out.write(NEWLINE);
        return out;
    }

    /**
     * Flushes any output to the output stream, and close it if it was allocated
     * by the {@link #begin} method
     *
     * @param out the output stream that was written to.
     * @throws IOException if an I/O exception occurs.
     */
    private void end(OutputStream out) throws IOException {
        out.flush();
        if (out != this.out) { // a record created in the storage class
            out.close();
        }
    }

    /**
     * Allocates a new exchange identifier that can be used to tag requests and
     * responses in filenames or log output.
     *
     * @return new identifier.
     */
    private synchronized String newId() {
        counter++;
        return Long.toString(counter);
    }

    /**
     * Logs the content of message headers.
     *
     * @param message the message containing the headers to be logged.
     * @param out the output stream to log the headers to.
     * @throws IOException if an I/O exception occurs.
     */
    private void headers(Message message, OutputStream out) throws IOException {
        for (String header : message.headers.keySet()) {
            byte[] name = bytes(header);
                for (String value : message.headers.get(header)) {
                out.write(name);
                out.write(HEADER_DELIMITER);
                out.write(bytes(value));
                out.write(NEWLINE);
            }
        }
    }

    /**
     * Logs the content of a message entity, up to the limit that can be held
     * within a cached stream.
     *
     * @param message the message containing the entity to be logged.
     * @param out the output stream to log the entity content to.
     * @throws IOException if an I/O exception occurs.
     */
    private void entity(Message message, OutputStream out) throws IOException {
        if (message.entity != null) {
            CachedInputStream cin = new CachedInputStream(message.entity, tmpStorage.open(tmpStorage.create()));
            try {
                Streamer.stream(cin, out);
            }
            catch (OverflowException oe) { // recover by stopping; no data loss
            }
            message.entity = cin.rewind();
            out.write(NEWLINE);
        }
    }
}
