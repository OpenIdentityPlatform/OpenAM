/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DataStoreTest.java,v 1.2 2008/06/25 05:44:16 qcheng Exp $
 *
 */

package com.sun.identity.cli.datastore;

import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.DevNullOutputWriter;
import com.sun.identity.cli.IArgument;
import com.sun.identity.test.common.TestBase;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;


/**
 * This is to test the data store sub commands.
 */
public class DataStoreTest extends TestBase{
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();
    private static final String TEST_DATASTORE_NAME = "samplesdatastore";
    private static final String TEST_DATASTORE_TYPE = "files";

    /**
     * Creates a new instance of <code>DataStoreTest</code>
     */
    public DataStoreTest() {
        super("CLI");
    }
    
    /**
     * Create the CLIManager.
     */
    @BeforeTest(groups = {"cli-datastore"})
    public void suiteSetup()
        throws CLIException {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "amadm");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.cli.AccessManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }


    @Parameters ({"realm"})
    @Test(groups = {"cli-datastore", "ops", "list-datastores"})
    public void listDataStores(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("listDataStores", param);
        String[] args = {
            "list-datastores",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listDataStores");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-datastore", "ops", "create-datastore"})
    public void createDataStore(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("createDataStore", param);
        String[] args = {
            "create-datastore",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + DatastoreOptions.DATASTORE_NAME,
            TEST_DATASTORE_NAME,
            CLIConstants.PREFIX_ARGUMENT_LONG + DatastoreOptions.DATASTORE_TYPE,
            TEST_DATASTORE_TYPE,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "sunIdRepoClass=com.sun.identity.idm.plugins.files.FilesRepo",
            "sunFilesIdRepoDirectory=/tmp/clitestdatastore"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("createDataStore");
    } 

/*
 * The following test will fail under flat file configuration datastore
 * because there is not notification in place. Service Configurations is not
 * made avaiable after they are created.
 * Works OK with Sun DS as configuration datastore.

    @Parameters ({"realm"})
    @Test(groups = {"cli-datastore", "ops", "update-datastore"},
        dependsOnMethods = {"createDataStore"}
    )
    public void updateDataStore(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("updateDataStore", param);
        String[] args = {
            "update-datastore",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + DatastoreOptions.DATASTORE_NAME,
            TEST_DATASTORE_NAME,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "sunFilesIdRepoDirectory=/tmp/clitestdatastoreChanged"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("updateDataStore");
    } */
    
    @Parameters ({"realm"})
    @AfterSuite(groups = {"cli-datastore", "delete-datastores"})
    public void deleteDataStores(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("deleteDataStores", param);
        String[] args = {
            "delete-datastores",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                DatastoreOptions.DATASTORE_NAMES,
            TEST_DATASTORE_NAME
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("deleteDataStores");
    }
}
