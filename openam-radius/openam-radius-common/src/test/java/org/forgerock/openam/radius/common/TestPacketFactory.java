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
package org.forgerock.openam.radius.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for PacketFactory. Created by markboyd on 6/19/14.
 */
public class TestPacketFactory {

    /**
     * Test to ensure conformity with <a href="https://tools.ietf.org/html/rfc2865#section-7.1">IETF RFC 2865 section
     * 7.1</a>
     *
     * @throws UnknownHostException
     */
    @Test
    public void testRfc2865Sec7dot1Example() throws UnknownHostException {
        final String hex = "01 00 00 38 0f 40 3f 94 73 97 80 57 bd 83 d5 cb"
                + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d" + "93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8"
                + "01 10 05 06 00 00 00 03";

        final ByteBuffer bfr = Utils.toBuffer(hex);
        dumpBfr(bfr);
        final Packet pkt = PacketFactory.toPacket(bfr);
        Assert.assertNotNull(pkt.getAuthenticator(), "authenticator should be defined");
        Assert.assertEquals(pkt.getType(), PacketType.ACCESS_REQUEST, "Incorrect type code");
        Assert.assertEquals(pkt.getIdentifier(), 0, "packet identifier should have been 0");
        Assert.assertEquals(pkt.getAttributeSet().size(), 4, "packet attributes contained");

        Assert.assertEquals(pkt.getAttributeAt(0).getClass().getSimpleName(), UserNameAttribute.class.getSimpleName(),
                "0 attribute");
        Assert.assertEquals(((UserNameAttribute) pkt.getAttributeAt(0)).getName(), "nemo", "user name");

        Assert.assertEquals(pkt.getAttributeAt(1).getClass().getSimpleName(),
                UserPasswordAttribute.class.getSimpleName(), "1 attribute");

        Assert.assertEquals(pkt.getAttributeAt(2).getClass().getSimpleName(),
                NASIPAddressAttribute.class.getSimpleName(), "2 attribute");
        Assert.assertEquals(((NASIPAddressAttribute) pkt.getAttributeAt(2)).getIpAddress(),
                InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168, 1, 16 }), "NAS IP address");

        Assert.assertEquals(pkt.getAttributeAt(3).getClass().getSimpleName(), NASPortAttribute.class.getSimpleName(),
                "3 attribute");
        Assert.assertEquals(((NASPortAttribute) pkt.getAttributeAt(3)).getPort(), 3, "NAS port");

    }

    /**
     * dumps to std out in sets of 16 hex bytes separated by spaces and prefixed with '0' for bytes having value less
     * than 0x10. The buffer is returned as was meaning ready to read from the same point as when it was passed to this
     * method.
     *
     * @param bfr
     */
    private void dumpBfr(ByteBuffer bfr) {
        System.out.println("Packet contents: ");

        bfr.mark();
        int i = 0;

        for (; bfr.hasRemaining();) {
            if (i == 16) {
                System.out.println();
                i = 0;
            }
            i++;
            final byte b = bfr.get();
            final int j = (b) & 0xFF; // trim off sign-extending bits
            String bt = Integer.toHexString(j);
            if (bt.length() == 1) { // prefix single chars with '0'
                bt = "0" + bt;
            }

            System.out.print(bt + " ");

        }
        bfr.reset();
        System.out.println();
    }

    /**
     * Test username attribute reading
     */
    @Test
    private void testUserNameAtt() {
        final String hex = "01 06 6e 65 6d 6f";
        final ByteBuffer bfr = Utils.toBuffer(hex);
        final Attribute att = PacketFactory.nextAttribute(bfr);
        Assert.assertEquals(att.getClass().getSimpleName(), UserNameAttribute.class.getSimpleName(),
                "wrong attribute class instantiated");
        final UserNameAttribute una = (UserNameAttribute) att;
        Assert.assertEquals(una.getName(), "nemo");
    }

    // dumps out to console different views of a byte including as a short, int, and byte value and the hexadecimal
    // and signed interpretation.
    // @Test
    private void testBytes() {
        byte b = 0x00;
        final byte[] bytes = new byte[1];

        for (int i = 0; i < 256; i++) {
            bytes[0] = b;
            final int j = (b) & 0xFF;
            final short k = (short) j;

            System.out.println((b > 0 ? " " : "") + b + " " + Utils.toSpacedHex(ByteBuffer.wrap(bytes)) + " " + j + " "
                    + k + " - " + (bytes[0] & 0xFF));
            b++;
        }
    }
}
