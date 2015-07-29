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
 * Copyright 2015 ForgeRock AS.
 */
package com.sun.identity.log.service;

import static org.fest.assertions.Assertions.assertThat;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link AgentLogParser}.
 *
 * @since 13.0.0
 */
public class AgentLogParserTest {

    private AgentLogParser logParser;

    @BeforeMethod
    public void setUp() {
        logParser = new AgentLogParser();
    }

    @Test
    public void parsesJavaAgentMessages() {
        // Given
        String message = "Access to http://raspi.forrest.org:8080/examples/index.html denied for user id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org";

        // When
        AgentLogParser.LogExtracts logExtracts = logParser.tryParse(message);

        // Then
        assertThat(logExtracts.getResourceUrl()).isEqualTo("http://raspi.forrest.org:8080/examples/index.html");
        assertThat(logExtracts.getSubjectId()).isEqualTo("id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org");
        assertThat(logExtracts.getStatus()).isEqualTo("denied");
    }

    @Test
    public void parsesWebAgentMessages() {
        // Given
        String message = "User amadmin was allowed access to http://raspi.forrest.org:80/";

        // When
        AgentLogParser.LogExtracts logExtracts = logParser.tryParse(message);

        // Then
        assertThat(logExtracts.getResourceUrl()).isEqualTo("http://raspi.forrest.org:80/");
        assertThat(logExtracts.getSubjectId()).isEqualTo("amadmin");
        assertThat(logExtracts.getStatus()).isEqualTo("allowed");
    }

    @Test
    public void unknownMessage() {
        // Given
        String message = "Fred is not going to be given access to the fridge";

        // When
        AgentLogParser.LogExtracts logExtracts = logParser.tryParse(message);

        // Then
        assertThat(logExtracts).isNull();
    }

}