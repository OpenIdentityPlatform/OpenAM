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
 * Copyright 2016 ForgeRock AS.
 */

package com.iplanet.services.naming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import com.iplanet.services.naming.NamingTableConfigurationFactory.NamingTableConfiguration;

public class NamingTableConfigurationFactoryTest {

    private final static String MOCK_NAMINGTABLE = "/mocknamingtable.properties";
    private final static String serverURL = "http://openam.example.com:8080/openam";

    /**
     * serverIDMapping for a server URL should return only the serverID
     * and not any other ID like siteID. The local server id should be 01.
     */
    @Test
    public void testServerIDMappingMapsURLOnlytoServerID_0() throws Exception {
        // Case 0: Normal standalone server
        // Should pass
        Hashtable<String, String> namingTable = getMockedNamingTable();
        namingTable.put("iplanet-am-platform-lb-cookie-value-list","01|01");
        namingTable.put("iplanet-am-platform-site-id-list", "01");
        namingTable.put("openam-am-platform-site-names-list","");
        namingTable.put("iplanet-am-platform-server-list",serverURL);
        namingTable.put("01",serverURL);
        testServerIDMapping(namingTable);
    }

    @Test
    public void testServerIDMappingMapsURLOnlytoServerID_1() throws Exception {
        // Case 1: Site and serverURL setup to be same
        // serverURL is part of the SITE
        // Given serverID = "01" & siteID = "02"
        Hashtable<String, String> namingTable = getMockedNamingTable();
        namingTable.put("iplanet-am-platform-lb-cookie-value-list","01|01");
        namingTable.put("iplanet-am-platform-site-id-list", "01|02,02");
        namingTable.put("openam-am-platform-site-names-list","SITE|02");
        namingTable.put("iplanet-am-platform-server-list",serverURL);
        namingTable.put("01",serverURL);
        namingTable.put("02",serverURL);
        testServerIDMapping(namingTable);
    }

    @Test
    public void testServerIDMappingMapsURLOnlytoServerID_2() throws Exception {
        // Case 2: serverURL setup and a site with same URL is setup
        // with same URL. However the SITE is not used.
        // Given serverID = "01" & siteID = "02"
        Hashtable<String, String> namingTable = getMockedNamingTable();
        namingTable.put("iplanet-am-platform-lb-cookie-value-list","01|01");
        namingTable.put("iplanet-am-platform-site-id-list", "01,02");
        namingTable.put("openam-am-platform-site-names-list","UNUSEDSITE|02");
        namingTable.put("iplanet-am-platform-server-list",serverURL);
        namingTable.put("01",serverURL);
        namingTable.put("02",serverURL);
        testServerIDMapping(namingTable);
    }

    private void testServerIDMapping(Hashtable namingTable) throws Exception {
        // When
        NamingTableConfigurationFactory factory = new NamingTableConfigurationFactory();
        NamingTableConfiguration config = factory.getConfiguration(namingTable);
        Map<String, String> serverIDTbl = config.getServerIDTable();
        String result = serverIDTbl.get(serverURL);

        // Then
        // The serverURL shoud be the main serverID and not the siteID.
        assertTrue(config.getServerIDs().contains("01"),
             "ServerID not in "+config.getServerIDs().toString());
        assertThat(result).isEqualTo("01");
    }

    /**
     * Get a NamingTable hashmap from a configuration file
     *
     * @param propfile Namingtable property file
     *
     * @return Non null Hashtable of NamingTable
     */
    private Hashtable<String, String> getMockedNamingTable() throws IOException {
        Properties prop = new Properties();
        try (InputStream is = this.getClass().getResourceAsStream(MOCK_NAMINGTABLE)) {
           if (is == null) {
              throw new IOException(MOCK_NAMINGTABLE+" missing");
           }
           prop.load(is);
        }
        Map<String, String> map = (Map)prop;
        return new Hashtable<String, String>(map);
    }
}
