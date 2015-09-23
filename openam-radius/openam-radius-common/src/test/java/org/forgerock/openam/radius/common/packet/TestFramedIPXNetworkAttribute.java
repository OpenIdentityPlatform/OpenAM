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
 * Portions Copyrighted 2011 ForgeRock AS
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common.packet;

import java.io.IOException;

import org.forgerock.openam.radius.common.AttributeType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestFramedIPXNetworkAttribute {

    @Test
    public void testFromOctets() {
        final byte [] testPacket = new byte[6];
        testPacket[0] = (byte) AttributeType.FRAMED_IPX_NETWORK.getTypeCode();
        testPacket[1] = 6;
        testPacket[2] = 5;
        testPacket[3] = 4;
        testPacket[4] = 3;
        testPacket[5] = 2;

        final FramedIPXNetworkAttribute testIPXpacket = new FramedIPXNetworkAttribute(testPacket);

        Assert.assertEquals(testIPXpacket.getIPXNetworkAddress()[0], 5);
        Assert.assertEquals(testIPXpacket.getIPXNetworkAddress()[1], 4);
        Assert.assertEquals(testIPXpacket.getIPXNetworkAddress()[2], 3);
        Assert.assertEquals(testIPXpacket.getIPXNetworkAddress()[3], 2);

        Assert.assertEquals(testIPXpacket.getOctets()[0], AttributeType.FRAMED_IPX_NETWORK.getTypeCode());
        Assert.assertEquals(testIPXpacket.getOctets()[1], 6);
        Assert.assertEquals(testIPXpacket.getOctets()[2], 5);
        Assert.assertEquals(testIPXpacket.getOctets()[3], 4);
        Assert.assertEquals(testIPXpacket.getOctets()[4], 3);
        Assert.assertEquals(testIPXpacket.getOctets()[5], 2);
    }

    @Test
    public void testFromAddress() throws IOException {
        final FramedIPXNetworkAttribute testIPXAddress = new FramedIPXNetworkAttribute(5, 4, 3 , 2);

        Assert.assertEquals(testIPXAddress.getOctets()[0], AttributeType.FRAMED_IPX_NETWORK.getTypeCode());
        Assert.assertEquals(testIPXAddress.getOctets()[1], 6);
        Assert.assertEquals(testIPXAddress.getOctets()[2], 5);
        Assert.assertEquals(testIPXAddress.getOctets()[3], 4);
        Assert.assertEquals(testIPXAddress.getOctets()[4], 3);
        Assert.assertEquals(testIPXAddress.getOctets()[5], 2);
    }

}
