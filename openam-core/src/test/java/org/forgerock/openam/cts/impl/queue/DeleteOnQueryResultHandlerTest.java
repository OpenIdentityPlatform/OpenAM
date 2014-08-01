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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.queue;

import com.sun.identity.shared.debug.Debug;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.PartialToken;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

public class DeleteOnQueryResultHandlerTest {

    private DeleteOnQueryResultHandler handler;
    private TaskDispatcher mockTaskDispatcher;
    private ResultHandlerFactory mockResultHandlerFactory;
    private Debug mockDebug;

    @BeforeMethod
    public void setup() {
        mockTaskDispatcher = mock(TaskDispatcher.class);
        mockResultHandlerFactory = mock(ResultHandlerFactory.class);
        mockDebug = mock(Debug.class);
        handler = new DeleteOnQueryResultHandler(mockTaskDispatcher, mockResultHandlerFactory, mockDebug);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldThrowUnsupportedOperationExceptionOnGetResults() {
        handler.getResults();
    }

    @Test
    public void shouldInvokeDeleteForEachHit() throws Exception {
        Collection<PartialToken> hits = getHits(5);
        handler.processResults(hits);
        for (PartialToken hit : hits) {
            verify(mockTaskDispatcher).delete(eq(hit.<String>getValue(CoreTokenField.TOKEN_ID)),
                    any(ResultHandler.class));
        }
    }

    @Test
    public void shouldInvokeDeleteForEachHitEvenIfThereWasAnException() throws Exception {
        Collection<PartialToken> hits = getHits(5);
        handler.processResults(hits);

        willThrow(new CoreTokenException("")).willNothing().given(mockTaskDispatcher)
                .delete(anyString(), any(ResultHandler.class));
        for (PartialToken hit : hits) {
            verify(mockTaskDispatcher).delete(eq(hit.<String>getValue(CoreTokenField.TOKEN_ID)),
                    any(ResultHandler.class));
        }
    }

    private Collection<PartialToken> getHits(int count) throws Exception {
        List<PartialToken> tokens = new ArrayList<PartialToken>(count);
        for (int i = 0; i < count; i++) {
            PartialToken token = mock(PartialToken.class);
            given(token.getValue(CoreTokenField.TOKEN_ID)).willReturn(String.valueOf(i));
            tokens.add(token);
        }
        return tokens;
    }
}
