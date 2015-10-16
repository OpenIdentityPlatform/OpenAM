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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.guava.common.eventbus.EventBus;
import org.testng.annotations.Test;

public class RadiusServerEventRegistrarTest {

    @Test(enabled = true)
    public void RadiusServerEventRegistrar() {
        // Given
        EventBus eventBus = new EventBus();
        // When
        final RadiusServerEventRegistrator eventRegistrar = new RadiusServerEventRegistrar(eventBus);
        // Then
        assertThat(eventRegistrar).isNotNull();
    }

    @Test(enabled = true)
    public void authRequestAccepted() {
        // Given
        EventBus eventBus = new EventBus();
        final RadiusServerEventRegistrar eventRegistrar = new RadiusServerEventRegistrar(eventBus);
        // When
        eventRegistrar.authRequestAccepted();
        // Then
        assertThat(eventRegistrar.getNumberOfAuthRequestsAccepted()).isEqualTo(1);
    }

    @Test(enabled = true)
    public void authRequestRejected() {
        // Given
        EventBus eventBus = new EventBus();
        final RadiusServerEventRegistrar eventRegistrar = new RadiusServerEventRegistrar(eventBus);
        // When
        eventRegistrar.authRequestRejected();
        // Then
        assertThat(eventRegistrar.getNumberOfAuthRequestsRejected()).isEqualTo(1);
    }

    @Test(enabled = true)
    public void packetAccepted() {
        // Given
        EventBus eventBus = new EventBus();
        final RadiusServerEventRegistrar eventRegistrar = new RadiusServerEventRegistrar(eventBus);
        // When
        eventRegistrar.packetAccepted();
        // Then
        assertThat(eventRegistrar.getNumberOfAcceptedPackets()).isEqualTo(1);
    }

    @Test(enabled = true)
    public void packetProcessed() {
        // Given
        EventBus eventBus = new EventBus();
        final RadiusServerEventRegistrar eventRegistrar = new RadiusServerEventRegistrar(eventBus);
        // When
        eventRegistrar.packetProcessed();
        // Then
        assertThat(eventRegistrar.getNumberOfPacketsProcessed()).isEqualTo(1);
    }

    @Test(enabled = true)
    public void packetReceived() {
        // Given
        EventBus eventBus = new EventBus();
        final RadiusServerEventRegistrar eventRegistrar = new RadiusServerEventRegistrar(eventBus);
        // When
        eventRegistrar.packetReceived();
        // Then
        assertThat(eventRegistrar.getNumberOfPacketsRecieved()).isEqualTo(1);
    }
}
