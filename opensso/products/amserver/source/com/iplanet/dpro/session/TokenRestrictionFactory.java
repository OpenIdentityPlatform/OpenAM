/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: TokenRestrictionFactory.java,v 1.3 2008/06/25 05:41:29 qcheng Exp $
 *
 */

package com.iplanet.dpro.session;

import com.sun.identity.shared.encode.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Helper class to marshal/unmarshal a Token Restriction object.
 */

public class TokenRestrictionFactory {
    /**
     * Serializes the restriction object.
     * 
     * @param tr Token Restriction object to be serialized.
     * @return a serialized form of the restriction object
     * @throws Exception if the there was an error.
     */
    public static String marshal(TokenRestriction tr) throws Exception {
        // perform general Java serialization
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bs);
        os.writeObject(tr);
        os.flush();
        os.close();
        return Base64.encode(bs.toByteArray());
    }

    /**
     * Deserialize the string into Token Restriction object.
     * 
     * @param data Token Restriction object in the string format
     * @return a Token Restriction object.
     * @throws Exception if the there was an error.
     */
    public static TokenRestriction unmarshal(String data) throws Exception {
        // perform general Java deserialization
        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(
                Base64.decode(data)));
        return (TokenRestriction) is.readObject();
    }
}
