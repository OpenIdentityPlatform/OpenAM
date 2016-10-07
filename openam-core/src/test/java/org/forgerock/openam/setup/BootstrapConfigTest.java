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
package org.forgerock.openam.setup;

import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.assertThat;


import java.io.IOException;

/**
 * Test for bootstrap config
 *
 */
public class BootstrapConfigTest {


    @Test
    public void envVarExpansion() {
        String home = System.getenv("HOME");
        String user_home = System.getProperty("user.home");

        String instance =  "http://${env.HOME}/bar and ${user.home}";
        String expected = "http://" + home + "/bar and " + user_home;

        assertThat( BootstrapConfig.expandEnvironmentVariables(instance).equals(expected));

    }

    @Test
    public void testBasicBootConfig() throws IOException {
        String home = System.getenv("HOME");

        String instance =  "http://${env.HOME}/bar";
        String expected = "http://" + home + "/bar";

        BootstrapConfig bs = new BootstrapConfig();
        bs.setInstance(instance);

        String s = bs.toJson();

        String env = BootstrapConfig.expandEnvironmentVariables(s);

        // test marshall back in
        BootstrapConfig bs2= BootstrapConfig.fromJson(env);

        assertThat(bs2.getInstance().equals(expected));
    }
}
