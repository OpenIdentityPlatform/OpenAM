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
 * Copyright 2022 Open Identity Platform Community.
 */

package org.openidentityplatform.openam.cassandra;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;

public class SSHA256Test {

    @Test
    public void test() throws NoSuchAlgorithmException {
    	assertTrue(SSHA256.verifySaltedPassword("1".getBytes(), SSHA256.getSaltedPassword("1".getBytes())));
    	assertTrue(SSHA256.verifySaltedPassword("p@ssw0rd".getBytes(), "{SSHA256}LGkJJV6e7wPDKEr3BKSg0K0XDllewz9tvSNSaslDmIfPFmyuI5blUK/QsTXjvgFKLlMQm1jPC7K7z/KaD4zoHQ=="));
    }
}
