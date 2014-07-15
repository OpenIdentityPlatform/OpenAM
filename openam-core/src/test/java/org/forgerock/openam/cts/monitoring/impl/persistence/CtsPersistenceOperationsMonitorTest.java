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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.monitoring.impl.persistence;

import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;

public class CtsPersistenceOperationsMonitorTest {

    private CtsPersistenceOperationsMonitor opsMonitor;

    private CtsPersistenceOperationsDelegate delegate;

    @BeforeMethod
    public void setUp() {

        delegate = mock(CtsPersistenceOperationsDelegate.class);
        opsMonitor = new CtsPersistenceOperationsMonitor(delegate);

    }

    @Test
    public void getTotalCountTest() throws CoreTokenException {

        //given
        int count = 1;
        given(delegate.countTokenEntries(any(TokenType.class))).willReturn(count);

        //when
        final long result = opsMonitor.getTotalCount(TokenType.SESSION);

        //then
        assertThat(result).isEqualTo(count);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getTotalCountTestErrorInvalidToken() throws CoreTokenException {
        //given
        final TokenType tokenType = null;

        //when
        opsMonitor.getTotalCount(tokenType);

        //then
        //throw exception
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void getAverageDurationTestErrorInvalidToken() throws CoreTokenException  {
        //given
        final TokenType tokenType = null;

        //when
        opsMonitor.getAverageDuration(tokenType);

        //then
        //throw exception
    }

    @Test
    public void getAverageDurationTest() throws CoreTokenException {

        //given
        final Long duration = (long) 100;
        final TokenType tokenType = TokenType.SESSION;

        final ArrayList<Long> results = new ArrayList<Long>();
        results.add(duration);

        given(delegate.listDurationOfTokens(tokenType)).willReturn(results);

        //when
        final Long result = opsMonitor.getAverageDuration(tokenType);

        //then
        assertEquals(duration, result);
    }

    @Test
    public void getAverageDurationTestWithMultipleResults() throws CoreTokenException {

        //given
        final Long durationOne = (long) 100;
        final Long durationTwo = (long) 50;
        final Long durationAvg = (long) 75;

        final TokenType tokenType = TokenType.SESSION;

        final ArrayList<Long> results = new ArrayList<Long>();
        results.add(durationOne);
        results.add(durationTwo);

        given(delegate.listDurationOfTokens(tokenType)).willReturn(results);

        //when
        final Long result = opsMonitor.getAverageDuration(tokenType);

        //then
        assertEquals(durationAvg, result);
    }

    @Test
    public void getAverageDurationTestWithZeroResults() throws CoreTokenException {

        //given
        final Long durationAvg = (long) 0;
        final TokenType tokenType = TokenType.SESSION;
        final ArrayList<Long> results = new ArrayList<Long>();

        given(delegate.listDurationOfTokens(tokenType)).willReturn(results);

        //when
        final Long result = opsMonitor.getAverageDuration(tokenType);

        //then
        assertEquals(durationAvg, result);

    }

}