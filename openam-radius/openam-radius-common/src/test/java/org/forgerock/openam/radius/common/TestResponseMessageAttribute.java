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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.forgerock.openam.radius.common.packet.TestFilterIdAttribute;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestResponseMessageAttribute {

    @Test
    public void test() throws IOException {
        final ReplyMessageAttribute r = new ReplyMessageAttribute("hello");
        Assert.assertEquals(r.getType(), AttributeType.REPLY_MESSAGE, "should be a reply message");
        Assert.assertEquals(r.getMessage(), "hello", "message should be 'hello'");
        final byte[] data = r.getOctets();
        final String hex = Utils.toSpacedHex(ByteBuffer.wrap(data));
        System.out.println("data: " + hex);
        Assert.assertEquals(hex, "12 07 68 65 6c 6c 6f", "should have proper wire format");
    }

    @Test
    public void testMaxSize() throws IOException {
        final String maxS = TestFilterIdAttribute.get253CharString();
        final ReplyMessageAttribute r = new ReplyMessageAttribute(maxS);
        Assert.assertEquals(r.getType(), AttributeType.REPLY_MESSAGE, "should be a reply message");
        Assert.assertEquals(r.getMessage(), maxS, "message value should be unchanged");
    }
}
