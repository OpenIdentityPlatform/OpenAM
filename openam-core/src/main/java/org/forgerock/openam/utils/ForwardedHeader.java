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

package org.forgerock.openam.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.util.Pair;

/**
 * A utility class for handling the RFC 7239 Forwarded header.
 * @see <a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>
 */
public class ForwardedHeader {

    private static final String HOST_FIELD = "host";
    private static final String PROTO_FIELD = "proto";
    private static final String FOR_FIELD = "for";
    private static final String BY_FIELD = "by";
    private static final String HEADER_NAME = "Forwarded";

    private List<String> forValues = new ArrayList<String>();
    private List<String> byValues = new ArrayList<String>();
    private List<String> hostValues = new ArrayList<String>();
    private List<String> protoValues = new ArrayList<String>();

    private ForwardedHeader() { }

    public List<String> getForValues() {
        return Collections.unmodifiableList(forValues);
    }

    public List<String> getByValues() {
        return Collections.unmodifiableList(byValues);
    }

    public List<String> getHostValues() {
        return Collections.unmodifiableList(hostValues);
    }

    public List<String> getProtoValues() {
        return Collections.unmodifiableList(protoValues);
    }

    public static ForwardedHeader parse(HttpServletRequest request) {
        final Enumeration<String> headers = request.getHeaders(HEADER_NAME);
        ForwardedHeader header = new ForwardedHeader();
        if (headers != null) {
            while (headers.hasMoreElements()) {
                for (Pair<String, String> attribute : new HeaderAttributeIterable(headers.nextElement().toCharArray())) {
                    if (attribute.getFirst().equalsIgnoreCase(FOR_FIELD)) {
                        header.forValues.add(attribute.getSecond());
                    } else if (attribute.getFirst().equalsIgnoreCase(BY_FIELD)) {
                        header.byValues.add(attribute.getSecond());
                    } else if (attribute.getFirst().equalsIgnoreCase(HOST_FIELD)) {
                        header.hostValues.add(attribute.getSecond());
                    } else if (attribute.getFirst().equalsIgnoreCase(PROTO_FIELD)) {
                        header.protoValues.add(attribute.getSecond());
                    } else {
                        throw new IllegalArgumentException("Unknown Forwarded header attribute: " + attribute.getFirst());
                    }
                }
            }
        }
        return header;
    }

    static class HeaderAttributeIterable implements Iterable<Pair<String, String>> {

        private char[] header;

        HeaderAttributeIterable(char[] header) {
            this.header = header;
        }

        @Override
        public Iterator<Pair<String, String>> iterator() {
            return new Iterator<Pair<String, String>>() {

                private int counter = 0;

                @Override
                public boolean hasNext() {
                    return counter < header.length;
                }

                @Override
                public Pair<String, String> next() {
                    String key = null;
                    boolean quoted = false;
                    char lastChar = '\0';

                    while (header[counter] == ' ') {
                        counter++;
                    }

                    StringBuilder next = new StringBuilder();
                    while (counter < header.length) {
                        char currentChar = header[counter];
                        if (currentChar == '=' && key == null) {
                            key = next.toString();
                            next = new StringBuilder();
                            lastChar = '=';
                        } else if (currentChar == '"' && lastChar == '=') {
                            quoted = true;
                            lastChar = '\0';
                        } else if (currentChar == '\\' && quoted) {
                            lastChar = '\\';
                        } else if (currentChar == '"' && lastChar != '\\' && quoted) {
                            quoted = false;
                            lastChar = '\0';
                        } else if ((currentChar == ';' || currentChar == ',') && !quoted) {
                            counter++;
                            break;
                        } else {
                            next.append(currentChar);
                            lastChar = '\0';
                        }
                        counter++;
                    }
                    return Pair.of(key, next.toString());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
