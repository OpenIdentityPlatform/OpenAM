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

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.testng.annotations.Test;

/**
 * Test for bootstrap config
 */
public class BootstrapConfigTest {

    @Test
    public void testEnvVarExpansion() {
        String home = System.getenv("HOME");
        String user_home = System.getProperty("user.home");

        String instance = "http://${env.HOME}/bar and ${user.home}";
        String expected = "http://" + home + "/bar and " + user_home;

        assertThat(BootstrapConfig.expandEnvironmentVariables(instance).equals(expected));

        // test variable not found
        String s = "This is ${baz.property} is ${env.NOTFOUND}";
        assertThat(BootstrapConfig.expandEnvironmentVariables(s).equals(s));
    }

    @Test
    public void testBasicBootConfig() throws IOException {
        String home = System.getenv("HOME");

        String instance = "http://${env.HOME}/bar";
        String expected = "http://" + home + "/bar";

        BootstrapConfig bs = new BootstrapConfig();
        bs.setInstance(instance);

        String s = bs.toJson();

        String env = BootstrapConfig.expandEnvironmentVariables(s);

        // test marshall back in
        BootstrapConfig bs2 = BootstrapConfig.fromJson(env);

        assertThat(bs2.getInstance().equals(expected));
    }

    @Test
    public void testJsonInit() throws IOException {
        String json = "{\n" +
                "  \"instance\" : \"${env.HOME}\",\n" +
                "  \"dsameUser\" : \"cn=dsameuser,ou=DSAME Users,dc=openam,dc=openidentityplatform,dc=org\",\n" +
                "  \"keystores\" : {\n" +
                "    \"default\" : {\n" +
                "      \"keyStorePasswordFile\" : \"${env.OPENAM_SECRETS}/.storepass\",\n" +
                "      \"keyPasswordFile\" : \"${env.OPENAM_SECRETS}/.keypass\",\n" +
                "      \"keyStoreType\" : \"JCEKS\",\n" +
                "      \"keyStoreFile\" : \"${env.OPENAM_SECRETS}/keystore.jceks\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"configStoreList\" : [ {\n" +
                "    \"baseDN\" : \"dc=openam,dc=openidentityplatform,dc=org\",\n" +
                "    \"dirManagerDN\" : \"cn=Directory Manager\",\n" +
                "    \"ldapHost\" : \"${env.OPENAM_CONFIG_STORE_LDAP_HOST}\",\n" +
                "    \"ldapPort\" : 389,\n" +
                "    \"ldapProtocol\" : \"ldap\"\n" +
                "  } ]\n" +
                "}";

        BootstrapConfig config = BootstrapConfig.fromJson(json);
        assertThat(config.getInstance().equals(System.getenv("HOME")));

        ConfigStoreProperties p = config.getConfigStoreList().get(0);
        assertThat(p.getLdapPort() == 389);
    }


}
