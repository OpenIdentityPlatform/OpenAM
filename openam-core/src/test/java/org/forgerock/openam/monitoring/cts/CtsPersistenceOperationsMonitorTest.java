/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.monitoring.cts;

import java.util.ArrayList;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.monitoring.impl.persistence.CtsPersistenceOperationsDelegate;
import org.forgerock.openam.cts.monitoring.impl.persistence.CtsPersistenceOperationsMonitor;
import org.forgerock.opendj.ldap.Entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
        final TokenType tokenType = TokenType.SESSION;

        ArrayList<Entry> results = new ArrayList<Entry>();
        Entry mockEntry = mock(Entry.class);
        results.add(mockEntry);

        given(delegate.getTokenEntries(tokenType)).willReturn(results);

        //when
        final long result = opsMonitor.getTotalCount(tokenType);

        //then
        assertEquals(result, 1l);
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