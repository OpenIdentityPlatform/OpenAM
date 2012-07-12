/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MockCLI.java,v 1.7 2008/06/25 05:44:18 qcheng Exp $
 *
 */

package com.sun.identity.cli.definition;

import com.sun.identity.cli.annotation.DefinitionClassInfo;
import com.sun.identity.cli.annotation.ResourceStrings;
import com.sun.identity.cli.annotation.SubCommandInfo;


public class MockCLI {
    @DefinitionClassInfo(
        productName="Mock Product",
        logName="amadm",
        resourceBundle="MockCLI")
    private String product;

    @ResourceStrings(
        string={}
    )
    private String resourcestrings;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.DummyCommand",
        description="A dummy command for testing purposes.",
        webSupport="true",
        mandatoryOptions={
            "mandatory|m|s|Mandatory option."},
        optionAliases={},
        macro="",
        optionalOptions={
            "optional|o|s|Optional option.",
            "testmatch|t|u|Set this flag to test options"},
        resourceStrings={
            "mandatory=Mandatory."})
    private String test_command;
}
