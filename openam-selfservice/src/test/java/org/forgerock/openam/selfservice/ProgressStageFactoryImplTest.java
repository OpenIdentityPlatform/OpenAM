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
package org.forgerock.openam.selfservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.config.StageConfig;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ProgressStageFactoryImpl}.
 *
 * @since 13.0.0
 */
public final class ProgressStageFactoryImplTest {

    private ProgressStageFactoryImpl stageFactory;

    @Mock
    private ProgressStage<MockStageConfig1> progressStage1;
    @Mock
    private ProgressStage<MockStageConfig2> progressStage2;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        stageFactory = new ProgressStageFactoryImpl();
    }

    @Test
    public void putReturnsSameValue() {
        // Given
        stageFactory.safePut(MockStageConfig1.class, progressStage1);
        stageFactory.safePut(MockStageConfig2.class, progressStage2);

        // When
        ProgressStage<?> progressStage1 = stageFactory.get(new MockStageConfig1());
        ProgressStage<?> progressStage2 = stageFactory.get(new MockStageConfig2());

        // Then
        assertThat(progressStage1).isSameAs(this.progressStage1);
        assertThat(progressStage2).isSameAs(this.progressStage2);
    }

    private static final class MockStageConfig1 implements StageConfig {

        @Override
        public String getName() {
            return "Stage config 1";
        }

    }

    private static final class MockStageConfig2 implements StageConfig {

        @Override
        public String getName() {
            return "Stage config 2";
        }

    }

}