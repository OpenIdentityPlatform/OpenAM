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
package org.forgerock.openam.sm.datalayer.impl.tasks;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.continuous.ContinuousQuery;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ContinuousQueryTaskTest {

    ContinuousQueryTask task;
    TokenFilter mockFilter;
    ContinuousQueryListener mockListener;

    @BeforeMethod
    public void theSetup() { //you need this
        mockFilter = mock(TokenFilter.class);
        mockListener = mock(ContinuousQueryListener.class);

        task = new ContinuousQueryTask(mockFilter, mockListener);
    }

    @Test
    public void shouldHandleResult() throws DataLayerException, ExecutionException, InterruptedException {
        //given
        TokenStorageAdapter mockAdapter = mock(TokenStorageAdapter.class);
        ContinuousQuery mockQuery = mock(ContinuousQuery.class);

        given(mockAdapter.startContinuousQuery(mockFilter, mockListener)).willReturn(mockQuery);

        //when
        task.execute(mockAdapter);

        //then
        Promise<ContinuousQuery, NeverThrowsException> promise = task.getQuery();

        assertThat(promise.isDone()).isTrue();
        assertThat(promise.isCancelled()).isFalse();
        assertThat(promise.get()).isEqualTo(mockQuery);
    }

    @Test
    public void shouldProcessError() {
        //given
        DataLayerException exception = new DataLayerException("Test exception.");

        //when
        task.processError(exception);

        //then
        verify(mockListener, times(1)).processError(exception);
    }

}
