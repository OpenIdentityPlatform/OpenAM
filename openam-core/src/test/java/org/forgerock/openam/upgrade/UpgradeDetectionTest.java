/*
 * Copyright 2013-2014 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.upgrade;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class UpgradeDetectionTest {

    @DataProvider(name = "versions")
    public Object[][] getVersions() {
        return new Object[][]{
            {"OpenAM 10.0.0 (2012-April-13 10:24)", "OpenAM 9.5.5-RC1 (2012-March-05 03:13)", false},
            {"OpenAM 9.5.5-RC1 (2012-March-05 00:14)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"Snapshot Build 9.5.1_RC1(2010-June-30 20:23)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"Snapshot Build 9.5.1_RC2(2010-September-16 12:02)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"Release 9.5.1 Build 9.5.1(2010-November-04 13:03)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"Release 9.5.2_RC1 Build 563 (2011-February-03 20:48)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"(2011-March-02 18:42)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"OpenAM 10.0.0 (2012-April-13 10:24)", "(2011-March-02 18:42)", false},
            {"9.5.3_RC1 Build 753 (2011-May-06 10:55)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"9.5.3 Build 934 (2011-July-29 00:15)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"9.5.4_RC1 Build 1419 (2011-November-11 09:53)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"9.5.4 Build 1516 (2011-December-07 09:55)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"OpenAM 10.0.0-EA (2012-February-07 00:14)", "OpenAM 10.0.0 (2012-April-13 10:24)", true},
            {"OpenAM 10.0.0 (2012-April-13 10:24)", "OpenAM 10.0.0-EA (2012-February-07 00:14)", false},
            {"OpenAM 10.0.0 (2012-April-13 10:24)", "OpenAM 11.0.0-EA (2012-July-04 00:14)", true},
            {"OpenAM 10.0.0 (2012-April-13 10:24)", "OpenAM 10.0.1 (2012-May-04 00:14)", true},
            {"OpenAM 10.0.1 (2012-May-04 00:14)", "OpenAM 10.0.0 (2012-April-13 10:24)", false},
            {"OpenAM 11.0.0 (2012-July-05 00:12)", "OpenAM 10.0.0 (2012-April-13 10:24)", false},
            {"OpenAM 9.5.5 Build 3685 (2012-November-23 14:23)", "OpenAM 11.0.0 (2013-November-08 10:40)", true},
            {"OpenAM 10.0.0 (2012-April-13 10:24)", "OpenAM 11.0.0 (2013-November-08 10:40)", true},
            {"OpenAM 11.0.0 (2013-November-08 10:40)", "OpenAM 10.0.2 (2013-December-08 18:52)", false},
            // SVN revision number should not count in comparisons:
            {"OpenAM 12.0.0-SNAPSHOT Build 2000 (2013-March-13 08:43)", "OpenAM 12.0.1-SNAPSHOT Build 1000 (2013-March-13 08:43)", true},
            {"OpenAM 12.0.0-SNAPSHOT Build 2000 (2013-March-13 08:43)", "OpenAM 12.0.0-SNAPSHOT Build 1000 (2013-March-13 08:43)", false}
        };
    }

    @Test(dataProvider = "versions")
    public void testUpgradeDetectionCorrectness(String currentVersion, String newVersion, boolean result) {
        assertEquals(UpgradeUtils.isVersionNewer(currentVersion, newVersion), result);
    }
}
