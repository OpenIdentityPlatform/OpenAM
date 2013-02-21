/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.commons;

import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.util.regex.Pattern;

/**
 * Very simple test to check parsing and validation of URNs.
 *
 * @author jeff.schenk@forgerock.com
 */
public class TestURNValidation {

    public final static String[] testURNs = {XACML3Constants.URN_HTTP,
            XACML3Constants.URN_HOME,
            XACML3Constants.URN_PDP,
            XACML3Constants.URN_HOME_BODY,
            XACML3Constants.URN_HOME_STATUS,
            XACML3Constants.URN_ENTRY_POINT
    };

    @BeforeClass
    public void before() throws Exception {
    }

    @AfterClass
    public void after() throws Exception {
    }

    @Test
    public void testURNValidation() {

        for (String string : testURNs) {
            URN urn = new URN(string);
            assertTrue(urn.isValid());
        }

    }


}
