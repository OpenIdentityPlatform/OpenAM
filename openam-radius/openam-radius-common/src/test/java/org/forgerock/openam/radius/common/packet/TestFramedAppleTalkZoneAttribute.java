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
 * Tests for the FramedAppleTalkZoneAttribute class.
 */
public class TestFramedAppleTalkZoneAttribute {

    @Test
    public void testNormalUse() throws IOException {
        final FramedAppleTalkZoneAttribute a = new FramedAppleTalkZoneAttribute("filter");
        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_APPLETALK_ZONE.getTypeCode());
        Assert.assertEquals(bytes[1], "filter".length() + 2);
        Assert.assertEquals(new String(bytes, 2, bytes.length - 2, Charset.forName("utf-8")), "filter");
    }

    @Test
    public void testTruncationOfString() throws IOException {
        // create a 256 byte string
        final String fifty = "---------1---------2---------3---------4---------5"; // 50 chars
        final String f253 = new StringBuilder()
                .append(fifty).append(fifty).append(fifty).append(fifty).append(fifty).append("123").toString();

        final String f259 = new StringBuilder().append(f253).append("456789").toString();

        final FramedAppleTalkZoneAttribute a = new FramedAppleTalkZoneAttribute(f259);
        Assert.assertEquals(a.getZone(), f253, "filter id string should have been truncated to 254 chars");

        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_APPLETALK_ZONE.getTypeCode());
        Assert.assertEquals(bytes[1], ((byte) 255)); // bytes are signed. therefore to get unsigned byte with
        // 1111 1111 we use 255 and cast to byte
        Assert.assertEquals(new String(bytes, 2, 253, Charset.forName("utf-8")), f253);
    }

    @Test
    public void test253StringFits() throws IOException {
        // create a 256 byte string
        final String fifty = "---------1---------2---------3---------4---------5"; // 50 chars
        final String f253 = new StringBuilder()
                .append(fifty).append(fifty).append(fifty).append(fifty).append(fifty).append("123").toString();

        final FramedAppleTalkZoneAttribute a = new FramedAppleTalkZoneAttribute(f253);
        Assert.assertEquals(a.getZone(), f253, "253 byte filter id string should have been allowed");

        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_APPLETALK_ZONE.getTypeCode());
        Assert.assertEquals(bytes[1], ((byte) 255)); // bytes are signed. therefore to get unsigned byte with
        // 1111 1111 we use 255 and cast to byte
        Assert.assertEquals(new String(bytes, 2, 253, Charset.forName("utf-8")), f253);
    }

    @Test
    public void testFromRawBytes() throws IOException {
        final byte type = (byte) AttributeType.FRAMED_APPLETALK_ZONE.getTypeCode();
        final byte[] octets = new byte[] {type, 8, 102, 105, 108, 116, 101, 114};
        final FramedAppleTalkZoneAttribute a = new FramedAppleTalkZoneAttribute(octets);
        final byte[] bytes = a.getOctets();
        System.out.println("hAp2=" + Utils.toHexAndPrintableChars(bytes));
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_APPLETALK_ZONE.getTypeCode());
        Assert.assertEquals(bytes[1], 8);
        Assert.assertEquals(new String(bytes, 2, 6, Charset.forName("utf-8")), "filter");
    }
}
