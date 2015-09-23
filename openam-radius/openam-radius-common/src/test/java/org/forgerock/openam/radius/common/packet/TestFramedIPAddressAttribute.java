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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for FramedIPAddressAttribute
 */
public class TestFramedIPAddressAttribute {

    @Test
    public void testUserNegotiated() {
        FramedIPAddressAttribute a;
        a = new FramedIPAddressAttribute(FramedIPAddressAttribute.Type.USER_NEGOTIATED, 1, 2, 3, 4);
        Assert.assertTrue(a.isUserNegotiated());
        Assert.assertFalse(a.isNasSelected());
        Assert.assertFalse(a.isSpecified());
    }

    @Test
    public void testIsNasSelected() {
        final FramedIPAddressAttribute a = new FramedIPAddressAttribute(FramedIPAddressAttribute.Type.NAS_ASSIGNED,
                1, 2, 3, 4);
        Assert.assertFalse(a.isUserNegotiated());
        Assert.assertTrue(a.isNasSelected());
        Assert.assertFalse(a.isSpecified());
    }

    @Test
    public void testIsSpecified() {
        FramedIPAddressAttribute a;
        a = new FramedIPAddressAttribute(FramedIPAddressAttribute.Type.SPECIFIED, 192, 168, 1, 3);
        Assert.assertFalse(a.isUserNegotiated());
        Assert.assertFalse(a.isNasSelected());
        Assert.assertTrue(a.isSpecified());
        Assert.assertEquals(a.getAddress()[0], (byte) 192);
        Assert.assertEquals(a.getAddress()[1], (byte) 168);
        Assert.assertEquals(a.getAddress()[2], (byte) 1);
        Assert.assertEquals(a.getAddress()[3], (byte) 3);
    }
}
