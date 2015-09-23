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

/**
 * Test for FramedIPNetmaskAttribute
 */
public class TestFramedIPNetmaskAttribute {

    @Test
    public void testFromOctets() {
        byte[] o = new byte[6];
        o[0] = (byte) AttributeType.FRAMED_IP_NETMASK.getTypeCode();
        o[1] = 6;
        o[2] = 5;
        o[3] = 4;
        o[4] = 3;
        o[5] = 2;

        FramedIPNetmaskAttribute a = new FramedIPNetmaskAttribute(o);

        Assert.assertEquals(a.getMask()[0], 5);
        Assert.assertEquals(a.getMask()[1], 4);
        Assert.assertEquals(a.getMask()[2], 3);
        Assert.assertEquals(a.getMask()[3], 2);
    }

    @Test
    public void testFromAddress() throws IOException {
        FramedIPNetmaskAttribute a = new FramedIPNetmaskAttribute(5, 4, 3, 2);

        Assert.assertEquals(a.getMask()[0], 5);
        Assert.assertEquals(a.getMask()[1], 4);
        Assert.assertEquals(a.getMask()[2], 3);
        Assert.assertEquals(a.getMask()[3], 2);
        Assert.assertEquals(a.getOctets()[0], AttributeType.FRAMED_IP_NETMASK.getTypeCode());
        Assert.assertEquals(a.getOctets()[1], 6);
        Assert.assertEquals(a.getOctets()[2], 5);
        Assert.assertEquals(a.getOctets()[3], 4);
        Assert.assertEquals(a.getOctets()[4], 3);
        Assert.assertEquals(a.getOctets()[5], 2);
    }
}
