/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 */
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

package org.forgerock.identity.authentication.modules.adaptivedeviceprint;

import org.codehaus.jstestrunner.junit.JSTestSuiteRunner;
import org.codehaus.jstestrunner.junit.JSTestSuiteRunner.Include;
import org.codehaus.jstestrunner.junit.JSTestSuiteRunner.ResourceBase;
import org.junit.runner.RunWith;

@RunWith(JSTestSuiteRunner.class)
@ResourceBase({"target/test-classes", "target/classes"})
@Include({ "AdaptiveAuthInfoGatheringTest.html" })
public class AdaptiveAuthInfoGatheringTest {
}
