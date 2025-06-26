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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.worker.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.Collection;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.worker.process.CTSWorkerDeleteProcess.TokenDeletion;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TokenDeletionTest {

    private TokenDeletion deletion;
    private TaskDispatcher mockQueue;
    private Collection<PartialToken> tokens;

    @BeforeMethod
    public void setUp() throws Exception {
        mockQueue = mock(TaskDispatcher.class);
        deletion = new TokenDeletion(mockQueue);
        tokens = Arrays.asList(partialToken(), partialToken(), partialToken());
    }

    @Test
    public void shouldQueueEachTokenProvided() throws CoreTokenException {
        deletion.deleteBatch(tokens);
        verify(mockQueue, times(3)).delete(anyString(), any(ResultHandler.class));
    }

    @Test
    public void shouldReturnCountDownLatchThatCorrespondsToTokensProvided() throws CoreTokenException {
        assertThat(deletion.deleteBatch(tokens).getCount()).isEqualTo(tokens.size());
    }

    private PartialToken partialToken() {
        return mock(PartialToken.class);
    }
}
