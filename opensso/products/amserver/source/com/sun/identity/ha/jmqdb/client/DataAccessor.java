/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DataAccessor.java,v 1.3 2008/06/25 05:43:28 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.ha.jmqdb.client;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DataAccessor {
    public DataAccessor(EntityStore store, String initFile)
        throws DatabaseException, ClassNotFoundException,IOException {

        Properties prop = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new java.io.FileInputStream(initFile);
        } catch(FileNotFoundException e) {
            System.out.println("prop file not found:" +initFile+"\n"); 
        }
        prop.load(fileInputStream);

        Enumeration svcNames = prop.propertyNames();
        while (svcNames.hasMoreElements()) {
            String svcp = (String) svcNames.nextElement(); 
            if (!svcp.startsWith("persist_")) {
                continue;
            }
            String svcid = svcp.substring("persist_".length());
            String className = prop.getProperty(svcp);
            Class cl = Class.forName(className);
            classes.put(svcid, cl);

            // Primary Index 
            PrimaryIndex pIndex = 
                store.getPrimaryIndex(String.class, cl);
            primaryIndex.put(svcid, pIndex);

            // Secondary Index 1
            SecondaryIndex sIndex1 = 
                store.getSecondaryIndex(pIndex, Long.class, "expDate");
            secIndex1.put(svcid, sIndex1);

            // Secondary Index 2
            SecondaryIndex sIndex2 = 
                store.getSecondaryIndex(pIndex, String.class, "secondaryKey");
            secIndex2.put(svcid, sIndex2);
        }
    }

    public PrimaryIndex getPrimaryIndex(String svcid) 
    {
        return primaryIndex.get(svcid);
    }
    public SecondaryIndex getSecondaryIndex1(String svcid) 
    {
        return secIndex1.get(svcid);
    }
    public SecondaryIndex getSecondaryIndex2(String svcid) 
    {
        return secIndex2.get(svcid);
    }
    public Class getSvcClass(String svcid) 
    {
        return classes.get(svcid);
    }

    // Primary and Secondary index Accessors
    HashMap<String, PrimaryIndex> primaryIndex = 
                                  new HashMap<String, PrimaryIndex>();    
    HashMap<String, SecondaryIndex> secIndex1 = 
                                  new HashMap<String, SecondaryIndex>();    
    HashMap<String, SecondaryIndex> secIndex2 = 
                                  new HashMap<String, SecondaryIndex>();    
    HashMap<String, Class> classes = 
                                  new HashMap<String, Class>();    
}
