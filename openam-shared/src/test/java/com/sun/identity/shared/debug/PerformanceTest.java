/**
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
package com.sun.identity.shared.debug;


import com.sun.identity.shared.configuration.SystemPropertiesManager;

import java.io.File;
import java.io.IOException;

public class PerformanceTest {

    protected static final String DEFAULT_DEBUG_LEVEL = DebugLevel.MESSAGE.getName();
    protected static final String MERGE_ALL_ON = "on";
    protected static final String MERGE_ALL_OFF = "off";
    protected static final String DEFAULT_MERGE_ALL = MERGE_ALL_OFF;

    protected static final String DEBUG_FILEMAP_FOR_TEST = "/debug_config_test/debugfiles.properties";
    protected static final String DEBUG_CONFIG_FOR_TEST = "/debug_config_test/debugconfig.properties";
    //use the current test directory. You can modify it for debugging purpose
    protected static final String DEBUG_BASE_DIRECTORY = "/tmp/logs";

    protected String debugDirectory;

    private static int count = 0;
    protected String logName;
    protected IDebugProvider provider;
    protected DebugFileProviderForTest debugFileProvider;

    public void setUp() throws Exception {

        org.testng.Assert.assertNotNull(getClass().getResource(DEBUG_CONFIG_FOR_TEST), "Can't read properties");
        logName = "DebugTest-" + count;
        debugDirectory = DEBUG_BASE_DIRECTORY + File.separator + logName;
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_LEVEL, DEFAULT_DEBUG_LEVEL);
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_MERGEALL, DEFAULT_MERGE_ALL);
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_PROPERTIES_VARIABLE,
                DEBUG_CONFIG_FOR_TEST);
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_FILEMAP_VARIABLE,
                DEBUG_FILEMAP_FOR_TEST);
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_DIRECTORY, debugDirectory);

    }

    public static void main(String[] args) throws Exception {
        PerformanceTest test = new PerformanceTest();
        test.test();
    }

    public void test() throws Exception {
        setUp();
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_PROPERTIES_VARIABLE,
                DEBUG_CONFIG_FOR_TEST);

        Debug debug = Debug.getInstance("Tl");
        debug.setDebug("message");
        System.out.println("start test");


        for (long k = 0; k < 10; k++) {

            long startTime = System.currentTimeMillis();
            for (long i = 0; i < 20000000; i++) {
                debug.message("k");
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println(k + " - Duration in ms : " + duration);
        }
        clean();
    }


    public void clean() throws IOException {
        delete(new File(debugDirectory));
        count++;
    }

    /**
     * Delete a file
     *
     * @param file
     * @throws IOException
     */
    protected static void delete(File file) throws IOException {

        if (file.isDirectory()) {

            //directory is empty, then delete it
            if (file.list().length == 0) {

                file.delete();
                System.out.println("Directory is deleted : " + file.getAbsolutePath());

            } else {

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    delete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete();
                    System.out.println("Directory is deleted : " + file.getAbsolutePath());
                }
            }

        } else {
            //if file, then delete it
            file.delete();
            System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }

}
