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
 * Tests for the FramedAppleTalkLinkAttribute class.
 */
public class TestFramedMTUAttribute {

    @Test
    public void testHighest() throws IOException {
        final FramedMTUAttribute a = new FramedMTUAttribute(65535);
        Assert.assertEquals(a.getMtu(), 65535, "mtu should be 65535");
        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_MTU.getTypeCode());
        Assert.assertEquals(bytes[1], 6);

        final FramedMTUAttribute b = new FramedMTUAttribute(bytes);
        Assert.assertEquals(b.getMtu(), 65535, "mtu created from octets should be 65535");
    }

    @Test
    public void testHighestFromOctets() throws IOException {
        final byte[] octets = new byte[] {(byte) AttributeType.FRAMED_MTU.getTypeCode(), 6, 0, 0, (byte) 255, (byte) 255};
        final FramedMTUAttribute a = new FramedMTUAttribute(octets);
        Assert.assertEquals(a.getMtu(), 65535, "mtu should be 65535");
    }

    @Test
    public void testLowest() throws IOException {
        final FramedMTUAttribute a = new FramedMTUAttribute(64);
        Assert.assertEquals(a.getMtu(), 64, "mtu should be 64");
        final byte[] bytes = a.getOctets();
        Assert.assertEquals(bytes[0], AttributeType.FRAMED_MTU.getTypeCode());
        Assert.assertEquals(bytes[1], 6);

        final FramedMTUAttribute b = new FramedMTUAttribute(bytes);
        Assert.assertEquals(b.getMtu(), 64, "mtu created from octets should be 64");
    }

    @Test
    public void testLowestFromOctets() throws IOException {
        final byte[] octets = new byte[] {(byte) AttributeType.FRAMED_MTU.getTypeCode(), 6, 0, 0, 0, 64};
        final FramedMTUAttribute a = new FramedMTUAttribute(octets);
        Assert.assertEquals(a.getMtu(), 64, "mtu should be 64");
    }

}
