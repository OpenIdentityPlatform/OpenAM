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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;

import org.forgerock.http.Context;
import org.forgerock.http.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.selfservice.SelfServiceGuiceModule.InterimConfig;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ForgottenPasswordRequestHandler}.
 *
 * @since 13.0.0
 */
public final class ForgottenPasswordRequestHandlerTest {

    private RequestHandler forgottenPassword;

    @Mock
    private ProgressStageFactory stageFactory;
    @Mock
    private SnapshotTokenHandlerFactory tokenHandlerFactory;
    @Mock
    private SnapshotTokenHandler tokenHandler;
    @Mock
    private ProcessStore processStore;
    @Mock
    private ProgressStage<InterimConfig> progressStage;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        given(tokenHandlerFactory.get(SelfServiceGuiceModule.INTERIM_TYPE)).willReturn(tokenHandler);
        forgottenPassword = new ForgottenPasswordRequestHandler(stageFactory, tokenHandlerFactory, processStore);
    }

    @Test
    public void initialReadReturnsBasicRequirements() throws ResourceException {
        // When
        Context context = new RootContext();
        ReadRequest request = Requests.newReadRequest("/forgottenPassword");

        given(stageFactory.get(isA(InterimConfig.class))).willReturn(progressStage);
        JsonValue initialRequirements = json(object());
        given(progressStage.gatherInitialRequirements(isA(ProcessContext.class), isA(InterimConfig.class)))
                .willReturn(initialRequirements);

        // Given
        Promise<ResourceResponse, ResourceException> promise = forgottenPassword.handleRead(context, request);

        // Then
        assertThat(promise).succeeded().withContent().integerAt("stage").isEqualTo(0);
        assertThat(promise).succeeded().withContent().hasObject("requirements").isEmpty();
    }

    @Test
    public void initialActionReturnsCompletion() throws ResourceException {
        // When
        Context context = new RootContext();

        ActionRequest request = Requests.newActionRequest("/forgottenPassword", "submitRequirements");
        request.setContent(json(object(field("input", object()))));

        given(stageFactory.get(isA(InterimConfig.class))).willReturn(progressStage);

        StageResponse response = StageResponse.newBuilder().build();
        given(progressStage.advance(isA(ProcessContext.class), isA(InterimConfig.class)))
                .willReturn(response);

        // Given
        Promise<ActionResponse, ResourceException> promise = forgottenPassword.handleAction(context, request);

        // Then
        assertThat(promise).succeeded().withContent().stringAt("stage").isEqualTo("end");
        assertThat(promise).succeeded().withContent().booleanAt("status/success").isTrue();
    }

}