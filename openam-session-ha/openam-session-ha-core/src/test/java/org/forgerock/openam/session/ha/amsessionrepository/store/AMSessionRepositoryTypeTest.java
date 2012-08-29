/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */

package org.forgerock.openam.session.ha.amsessionrepository.store;

import org.forgerock.openam.session.ha.amsessionstore.store.AMSessionRepositoryType;
import org.forgerock.openam.session.ha.amsessionstore.store.opendj.OpenDJPersistentStore;
import org.forgerock.openam.session.ha.amsessionstore.store.plugin.PlugInPersistentStore;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.Map;


/**
 * AMSessionRepositoryType Tester.
 *
 * @author jeff.schenk@forgerock.com
 * @version 10.1
 * @since <pre>Aug 23, 2012</pre>
 */
public class AMSessionRepositoryTypeTest {

    @BeforeClass
    public void before() throws Exception {
    }

    @AfterClass
    public void after() throws Exception {
    }

    /**
     * Method: getAMSessionRepositoryTypes()
     */
    @Test
    public void testGetAMSessionRepositoryTypes() throws Exception {
        Map<String,String> types = AMSessionRepositoryType.getAMSessionRepositoryTypes();
        assertNotNull(types, "Expected non-null Collection of AMSessionRepositoryType(s), very Bad!");
        assertEquals(types.size(), 3, "Wrong number of AmSessionRepositoryTypeS");
        for(Map.Entry<String,String> entry : types.entrySet())
        {
            System.out.println("key:["+entry.getKey()+"], Value:["+entry.getValue()+"]");
        }
    }

    /**
     * Method: getAMSessionRepositoryTypeText(String type)
     */
    @Test
    public void testGetAMSessionRepositoryTypeText() throws Exception {

        assertEquals(AMSessionRepositoryType.getAMSessionRepositoryTypeText(AMSessionRepositoryType.OPENDJ.type()),
                AMSessionRepositoryType.OPENDJ.textDefinition());

        assertEquals(AMSessionRepositoryType.getAMSessionRepositoryTypeText(AMSessionRepositoryType.OPENDJ.displayType()),
                AMSessionRepositoryType.OPENDJ.textDefinition());

    }

    /**
     * Method: getAMSessionRepositoryTypeImplementationClass(String type)
     */
    @Test
    public void testGetAMSessionRepositoryTypeImplementationClass() throws Exception {

        assertEquals(AMSessionRepositoryType.OPENDJ.amSessionRepositoryImplementationClass(),
                OpenDJPersistentStore.class);

        assertEquals(AMSessionRepositoryType.PLUGIN.amSessionRepositoryImplementationClass(),
                PlugInPersistentStore.class);
    }


} 
