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
 * Copyright 2015-2016 ForgeRock AS.
 */
package com.sun.identity.log.service;

import static org.fest.assertions.Assertions.assertThat;

import org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
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

    @DataProvider(name = "webAgentSuccessLogMessages")
    public Object[][] createWebAgentSuccessLogMessages() {
        return new Object[][] {
                { "User   amadmin   was allowed access tohttp://raspi.forrest.org:80/"},
                { "user amadmin (192.168.56.1) was allowed access to http://raspi.forrest.org:80/" },
                { "user amadmin () was allowed access to http://raspi.forrest.org:80/" },
                { "user   amadmin   ()   was allowed access to http://raspi.forrest.org:80/" },
        };
    }

    @DataProvider(name = "webAgentFailureLogMessagesWhenUserIsNull")
    public Object[][] createWebAgentFailureLogMessagesWhenUserIsNull() {
        return new Object[][] {
                { "user (empty) (10.100.23.41) was denied access to http://raspi.forrest.org:80/" },
                { "user (empty) was denied access to http://raspi.forrest.org:80/" },
        };
    }

    @Test
    public void parsesJavaAgentSuccessMessages() {
        // Given
        String message = "Access to http://raspi.forrest.org:8080/examples/index.html allowed for user " +
                "id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org";

        // When
        AgentLogParser.LogExtracts logExtracts = logParser.tryParse(message);

        // Then
        assertThat(logExtracts.getResourceUrl()).isEqualTo("http://raspi.forrest.org:8080/examples/index.html");
        assertThat(logExtracts.getSubjectId()).isEqualTo("id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org");
        assertThat(logExtracts.getStatusCode()).isEqualTo("allowed");
        assertThat(logExtracts.getStatus()).isEqualTo(ResponseStatus.SUCCESSFUL);
    }

    @Test (dataProvider = "webAgentSuccessLogMessages")
    public void parsesWebAgentSuccessMessages(String message) {
        // When
        AgentLogParser.LogExtracts logExtracts = logParser.tryParse(message);

        // Then
        assertThat(logExtracts.getResourceUrl()).isEqualTo("http://raspi.forrest.org:80/");
        assertThat(logExtracts.getSubjectId()).isEqualTo("amadmin");
        assertThat(logExtracts.getStatusCode()).isEqualTo("allowed");
        assertThat(logExtracts.getStatus()).isEqualTo(ResponseStatus.SUCCESSFUL);
    }

    @Test
    public void parsesJavaAgentFailureMessages() {
        // Given
        String message = "Access tohttp://raspi.forrest.org:8080/examples/index.html  denied for user    " +
                "id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org";

        // When
        AgentLogParser.LogExtracts logExtracts = logParser.tryParse(message);

        // Then
        assertThat(logExtracts.getResourceUrl()).isEqualTo("http://raspi.forrest.org:8080/examples/index.html");
        assertThat(logExtracts.getSubjectId()).isEqualTo("id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org");
        assertThat(logExtracts.getStatusCode()).isEqualTo("denied");
        assertThat(logExtracts.getStatus()).isEqualTo(ResponseStatus.FAILED);
    }

    @Test
    public void parsesWebAgentFailureMessages() {
        // Given
        String message = "User amadmin was denied access to http://raspi.forrest.org:80/";

        // When
        AgentLogParser.LogExtracts logExtracts = logParser.tryParse(message);

        // Then
        assertThat(logExtracts.getResourceUrl()).isEqualTo("http://raspi.forrest.org:80/");
        assertThat(logExtracts.getSubjectId()).isEqualTo("amadmin");
        assertThat(logExtracts.getStatusCode()).isEqualTo("denied");
        assertThat(logExtracts.getStatus()).isEqualTo(ResponseStatus.FAILED);
    }

    @Test (dataProvider = "webAgentFailureLogMessagesWhenUserIsNull")
    public void parsesWebAgentFailureMessagesWhenUserIsNull(String message) {
        // When
        AgentLogParser.LogExtracts logExtracts = logParser.tryParse(message);

        // Then
        assertThat(logExtracts.getResourceUrl()).isEqualTo("http://raspi.forrest.org:80/");
        assertThat(logExtracts.getSubjectId()).isEqualTo("(empty)");
        assertThat(logExtracts.getStatusCode()).isEqualTo("denied");
        assertThat(logExtracts.getStatus()).isEqualTo(ResponseStatus.FAILED);
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