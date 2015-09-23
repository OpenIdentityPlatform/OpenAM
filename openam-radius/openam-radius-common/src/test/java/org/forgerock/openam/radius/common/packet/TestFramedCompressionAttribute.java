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

import org.forgerock.openam.radius.common.AttributeType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the FramedCompressionAttribute class.
 */
public class TestFramedCompressionAttribute {

    @Test
    public void testNoCompression() throws IOException {
        final FramedCompressionAttribute a = new FramedCompressionAttribute(0);
        Assert.assertEquals(a.getCompression(), FramedCompressionAttribute.NONE);
        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_COMPRESSION.getTypeCode());
        Assert.assertEquals(bytes[1], 6);
        Assert.assertEquals(bytes[2], 0);
        Assert.assertEquals(bytes[3], 0);
        Assert.assertEquals(bytes[4], 0);
        Assert.assertEquals(bytes[5], 0);
    }

    @Test
    public void testVjTcpIpCompression() throws IOException {
        final FramedCompressionAttribute a = new FramedCompressionAttribute(1);
        Assert.assertEquals(a.getCompression(), FramedCompressionAttribute.VJ_TCP_IP_HEADER);
        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_COMPRESSION.getTypeCode());
        Assert.assertEquals(bytes[1], 6);
        Assert.assertEquals(bytes[2], 0);
        Assert.assertEquals(bytes[3], 0);
        Assert.assertEquals(bytes[4], 0);
        Assert.assertEquals(bytes[5], 1);
    }

    @Test
    public void testIpxFromRawBytes() throws IOException {
        final byte[] raw = new byte[] {13, 6, 0, 0, 0, 2};
        final FramedCompressionAttribute a = new FramedCompressionAttribute(raw);
        Assert.assertEquals(a.getCompression(), FramedCompressionAttribute.IPX_HEADER);
        Assert.assertEquals(a.getType(), AttributeType.FRAMED_COMPRESSION);
    }

    @Test
    public void testSlzsFromRawBytes() throws IOException {
        final byte[] raw = new byte[] {13, 6, 0, 0, 0, 3};
        final FramedCompressionAttribute a = new FramedCompressionAttribute(raw);
        Assert.assertEquals(a.getCompression(), FramedCompressionAttribute.STAC_LZS);
        Assert.assertEquals(a.getType(), AttributeType.FRAMED_COMPRESSION);
    }
}
