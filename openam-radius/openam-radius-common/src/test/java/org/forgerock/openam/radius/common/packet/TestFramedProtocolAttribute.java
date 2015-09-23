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
 * Tests for the FramedProtocolAttribute class.
 */
public class TestFramedProtocolAttribute {

    @Test
    public void testLowest() throws IOException {
        final FramedProtocolAttribute a = new FramedProtocolAttribute(0);
        Assert.assertEquals(a.getFraming(), 0, "frame should be 0");
        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_PROTOCOL.getTypeCode());
        Assert.assertEquals(bytes[1], 6);
        Assert.assertEquals(bytes[2], 0);
        Assert.assertEquals(bytes[3], 0);
        Assert.assertEquals(bytes[4], 0);
        Assert.assertEquals(bytes[5], 0);
    }

    @Test
    public void testHighest() throws IOException {
        final FramedProtocolAttribute a = new FramedProtocolAttribute(65535);
        Assert.assertEquals(a.getFraming(), 65535, "should be 65535");
        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_PROTOCOL.getTypeCode());
        Assert.assertEquals(bytes[1], 6);
        Assert.assertEquals(bytes[2], 0);
        Assert.assertEquals(bytes[3], 0);
        Assert.assertEquals(bytes[4], (byte) 255);
        Assert.assertEquals(bytes[5], (byte) 255);
    }

    @Test
    public void testMaxFromRawBytes() throws IOException {
        final byte[] raw = new byte[] {(byte) AttributeType.FRAMED_PROTOCOL.getTypeCode(), 6, 0, 0, (byte) 255, (byte) 255};
        final FramedProtocolAttribute a = new FramedProtocolAttribute(raw);
        Assert.assertEquals(a.getFraming(), 65535);
        Assert.assertEquals(a.getType(), AttributeType.FRAMED_PROTOCOL);
    }

    @Test
    public void testMinFromRawBytes() throws IOException {
        final byte[] raw = new byte[] {(byte) AttributeType.FRAMED_PROTOCOL.getTypeCode(), 6, 0, 0, 0, 0};
        final FramedProtocolAttribute a = new FramedProtocolAttribute(raw);
        Assert.assertEquals(a.getFraming(), 0);
        Assert.assertEquals(a.getType(), AttributeType.FRAMED_PROTOCOL);
    }

    @Test
    public void testPPP() throws IOException {
        final byte type = (byte) AttributeType.FRAMED_PROTOCOL.getTypeCode();
        final byte[] raw = new byte[] {type, 6, 0, 0, 0, FramedProtocolAttribute.PPP};
        final FramedProtocolAttribute a = new FramedProtocolAttribute(raw);
        Assert.assertEquals(a.getFraming(), FramedProtocolAttribute.PPP);
        Assert.assertEquals(a.getType(), AttributeType.FRAMED_PROTOCOL);
    }
}
