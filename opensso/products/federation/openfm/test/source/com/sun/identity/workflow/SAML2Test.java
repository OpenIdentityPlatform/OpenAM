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
 * $Id: SAML2Test.java,v 1.3 2008/06/25 05:50:17 qcheng Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.identity.test.common.TestBase;
import com.sun.identity.test.common.FileHelper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SAML2Test extends TestBase {
    public SAML2Test() {
        super("Workflow");
    }
    
    @BeforeTest(groups = {"saml2"})
    public void suiteSetup() {
    }

    @Test(groups = {"samlv2"})
    public void createHostedIDP()
        throws IOException, WorkflowException {
        entering("createHostedIDP", null);
        CreateHostedIDP task = new CreateHostedIDP();
        Map<String, String> map = new HashMap<String, String>();
        map.put(ParameterKeys.P_REALM, "/");
        map.put(ParameterKeys.P_COT, "cottest");
        map.put(ParameterKeys.P_ENTITY_ID, "http://test.com/samples");
        map.put(ParameterKeys.P_IDP_E_CERT, "test");
        map.put(ParameterKeys.P_IDP_S_CERT, "test");
        exiting("createHostedIDP");
    }

    @Test(groups = {"samlv2"})
    public void createHostedIDPWithFile()
        throws IOException, WorkflowException {
        entering("createHostedIDPWithFile", null);
        createHostedIDPWithFileEx();
        exiting("createHostedIDPWithFile");
    }

    @Test(
        groups = {"samlv2"},
        expectedExceptions = {WorkflowException.class},
        dependsOnMethods = {"createHostedIDPWithFile"}
    )
    public void createHostedIDPWithFileDup()
        throws IOException, WorkflowException {
        entering("createHostedIDPWithFileDup", null);
        createHostedIDPWithFileEx();
        exiting("createHostedIDPWithFileDup");
    }

    private void createHostedIDPWithFileEx()
        throws IOException, WorkflowException {
        String meta = getFileContent("mock/workflow/saml2meta.xml");
        String extended = getFileContent("mock/workflow/saml2extended.xml");

        CreateHostedIDP task = new CreateHostedIDP();
        Map<String, String> map = new HashMap<String, String>();
        map.put(ParameterKeys.P_META_DATA, meta);
        map.put(ParameterKeys.P_EXENDED_DATA, extended);
        map.put(ParameterKeys.P_COT, "cottest");
        map.put(ParameterKeys.P_ATTR_MAPPING,
            "samplesasset1=localattr1|samplesasset2=localattr2");
        task.execute(Locale.getDefault(), map);
    } 

    protected String getFileContent(String filename)
        throws IOException {
        StringBuffer buff = new StringBuffer();
        FileReader input = new FileReader(filename);
        BufferedReader bufRead = new BufferedReader(input);
        String line = bufRead.readLine();
        while (line != null) {
            buff.append(line).append("\n");
            line = bufRead.readLine();
        }
        return buff.toString();
    }
}
