/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s): 
 */
package com.sun.identity.shared.ldap.ber.stream;

import java.util.*;
import java.io.*;

/**
 * This class is for the Sequence object. A sequence object can
 * contains a sequence of BER Elements.
 *
 * <pre>
 * ENCODING RULE:
 *  tag    = 0x30 (always constructed)
 * </pre>
 *
 * @version 1.0
 * seeAlso CCITT X.209
 */
public class BERSequence extends BERConstruct {
    /**
     * Constructs a sequence element.
     */
    public BERSequence() {
        super();
    }

    /**
     * Constructs a sequence element from an input stream.
     * @param decoder application-specific BER decoder
     * @param stream input stream from which to read BER
     * @param bytes_read array of 1 int; value is incremented by
     * number of bytes read from stream
     * @exception IOException failed to construct
     */
    public BERSequence(BERTagDecoder decoder, InputStream stream,
        int[] bytes_read) throws IOException {

        super(decoder, stream, bytes_read);
    }

    public void addElement(int contentLength, BERTagDecoder decoder,
        InputStream stream, int[] bytes_read) throws IOException {
        int contents_length = contentLength;
        int[] component_length = new int[1];
        if (contents_length == -1) {
            /* Constructed - indefinite length */
            BERElement element = null;
            do {
                component_length[0] = 0;
                element = getElement(decoder, stream, component_length);
                if (element != null) {
                    addElement(element);
                }
            } while (element != null);
        } else {
            /* Constructed - definite length */
            bytes_read[0] += contents_length;
            while (contents_length > 0) {
                component_length[0] = 0;
                addElement(getElement(decoder, stream,component_length));
                contents_length -= component_length[0];
            }
        }
    }

    /**
     * Gets the element type.
     * @return element type.
     */
    public int getType() {
        return BERElement.SEQUENCE;
    }

    /**
     * Gets the string representation.
     * @return string representation of tag.
     */
    public String toString() {
        String elements = "";
        for (int i = 0; i < super.size(); i++) {
            if (i != 0)
                elements = elements + ", ";
            elements = elements + ((BERElement)super.elementAt(i)).toString();
        }
        return "Sequence {" + elements + "}";
    }
}
