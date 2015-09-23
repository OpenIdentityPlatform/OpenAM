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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common.packet;

import java.io.IOException;
import java.nio.charset.Charset;

import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.Utils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the FilterIdAttribute class.
 */
public class TestFilterIdAttribute {

    @Test
    public void testNormalUse() throws IOException {
        FilterIdAttribute a = new FilterIdAttribute("filter");
        byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FILTER_ID.getTypeCode());
        Assert.assertEquals(bytes[1], "filter".length() + 2);
        Assert.assertEquals(new String(bytes, 2, bytes.length - 2, Charset.forName("utf-8")), "filter");
    }

    /**
     * Creates a 253 string of single byte characters which is the maximum length string that can be carried by any
     * radius attribute packet whose payload is a single string. Strictly speaking, the characters are java
     * characters which have two bytes. But since all are from the ascii set the upper byte has a value of zero.
     *
     * @return the maximum length string of characters that can be contained in a single radius string attribute
     */
    public static final String get253CharString() {
        String fifty = "---------1---------2---------3---------4---------5"; // 50 chars
        return new StringBuilder()
                .append(fifty).append(fifty).append(fifty).append(fifty).append(fifty).append("123").toString();
    }

    /**
     * Creates a 259 string of ascii characters made up of the 253 character version plus the characters "456789".
     *
     * @return
     */
    public static final String get259CharString() {
        return new StringBuilder().append(get253CharString()).append("456789").toString();
    }

    @Test
    public void testTruncationOfString() throws IOException {
        String f253 = get253CharString();
        String f259 = get259CharString();

        FilterIdAttribute a = new FilterIdAttribute(f259);
        Assert.assertEquals(a.getFilterId(), f253, "filter id string should have been truncated to 254 chars");

        byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FILTER_ID.getTypeCode());
        Assert.assertEquals(bytes[1], ((byte) 255)); // bytes are signed. therefore to get unsigned byte with
        // 1111 1111 we use 255 and cast to byte
        Assert.assertEquals(new String(bytes, 2, 253, Charset.forName("utf-8")), f253);
    }

    @Test
    public void test253StringFits() throws IOException {
        String f253 = get253CharString();

        FilterIdAttribute a = new FilterIdAttribute(f253);
        Assert.assertEquals(a.getFilterId(), f253, "253 byte filter id string should have been allowed");

        byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FILTER_ID.getTypeCode());
        Assert.assertEquals(bytes[1], ((byte) 255)); // bytes are signed. therefore to get unsigned byte with
        // 1111 1111 we use 255 and cast to byte
        Assert.assertEquals(new String(bytes, 2, 253, Charset.forName("utf-8")), f253);
    }

    @Test
    public void testFromRawBytes() throws IOException {
        byte[] raw = new byte[] {11, 8, 102, 105, 108, 116, 101, 114};
        FilterIdAttribute a = new FilterIdAttribute(raw);
        byte[] bytes = a.getOctets();
        System.out.println("hAp2=" + Utils.toHexAndPrintableChars(bytes));
        Assert.assertEquals(bytes[0], AttributeType.FILTER_ID.getTypeCode());
        Assert.assertEquals(bytes[1], 8);
        Assert.assertEquals(new String(bytes, 2, 6, Charset.forName("utf-8")), "filter");
    }
}
