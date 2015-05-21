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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.utils;

import com.sun.identity.shared.datastruct.OrderedSet;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;


public class IOUtilsTest {

    @Test
    public void testDeserializeValid() throws Exception {

        // This assumes that the fallback list is in place
        final Map validCollection = new HashMap();

        validCollection.put("key1", "value1");
        OrderedSet value2 = new OrderedSet();
        value2.add("A");
        value2.add("B");
        value2.add("C");
        value2.add("D");
        validCollection.put("key2", value2);
        Set<String> value3 = new HashSet<>();
        value3.add("value3.1");
        value3.add("value3.2");
        value3.add("value3.3");
        validCollection.put("key3", value3);
        List<String> value4 = new ArrayList<>();
        value4.add("value4.1");
        value4.add("value4.2");
        value4.add("value4.3");
        value4.add("value4.4");
        validCollection.put("key4", value4);
        Map value5 = new HashMap();
        value5.put("value2", value2);
        value5.put("value3", value3);
        value5.put("value4", value4);
        validCollection.put("key5", value5);
        validCollection.put("key6", Collections.emptyMap());
        validCollection.put("key7", new Integer(7));
        validCollection.put("key8", new Boolean(true));
        validCollection.put("key9", new String[]{"1", "2"});
        validCollection.put("key10", new Integer[]{1, 2});
        validCollection.put("key11", new byte[]{0, 1});
        validCollection.put("key12", new char[]{0, 1});
        validCollection.put("key13", new short[]{0, 1});
        validCollection.put("key14", new int[]{0, 1});
        validCollection.put("key15", new long[]{0, 1});
        validCollection.put("key16", new float[]{0.0f, 1.0f});
        validCollection.put("key17", new double[]{0.0f, 1.0f});
        validCollection.put("key18", new boolean[]{true, false});
        validCollection.put("key19", new int[][]{{1,1},{2,2}});

        final byte[] bytes = getObjectStreamBytes(validCollection, true);

        // can't use assertEquals here due to the way the primitives are checked for being equal
        Assert.assertNotNull(IOUtils.deserialise(bytes, true));
    }

    @Test(expectedExceptions = IOException.class)
    public void testDeserializeInvalid() throws Exception {

        final Map inValidObject = new HashMap();

        inValidObject.put("key1", new File("/tmp/tmpfile.txt"));

        final byte[] bytes = getObjectStreamBytes(inValidObject, false);

        IOUtils.deserialise(bytes, false);
    }

    private byte[] getObjectStreamBytes(Object in, boolean compress) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = compress
                ? new ObjectOutputStream(new DeflaterOutputStream(byteArrayOutputStream))
                : new ObjectOutputStream(byteArrayOutputStream);

        objectOutputStream.writeObject(in);
        objectOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }
}