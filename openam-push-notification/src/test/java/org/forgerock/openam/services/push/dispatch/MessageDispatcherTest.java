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
package org.forgerock.openam.services.push.dispatch;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.*;

import com.sun.identity.shared.debug.Debug;
import java.util.HashSet;
import org.forgerock.guava.common.cache.Cache;
import org.forgerock.guava.common.cache.CacheBuilder;
import org.forgerock.json.resource.NotFoundException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test
public class MessageDispatcherTest {

    Cache cache = CacheBuilder.newBuilder().build();
    Debug mockDebug = mock(Debug.class);

    MessageDispatcher messageDispatcher;

    @BeforeTest
    public void theSetUp() { //you need this
        messageDispatcher = new MessageDispatcher(cache, mockDebug);
    }

    @Test
    public void shouldReturnEmptyPromiseForexpectdMessageId() {
        //given

        //when
        MessagePromise result = messageDispatcher.expect("expectMessage", new HashSet<Predicate>());

        //then
        assertThat(result.getPromise().isDone()).isFalse();
    }

    @Test
    public void shouldCompletePromiseForhandleedMessageIdWhenexpectd()
            throws NotFoundException, PredicateNotMetException {
        //given
        MessagePromise result = messageDispatcher.expect("completeexpect", new HashSet<Predicate>());

        //when
        messageDispatcher.handle("completeexpect", json(object()));

        //then
        assertThat(result.getPromise().isDone()).isTrue();
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldErrorForHandleMessageIdWhenNotExpected() throws NotFoundException, PredicateNotMetException {
        //given

        //when
        messageDispatcher.handle("notexpected", json(object()));

        //then
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorWhenPrimpedForNullMessageId() {
        //given

        //when
        messageDispatcher.expect(null, null);

        //then
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorWhenPrimpedForEmptyMessageId() {
        //given

        //when
        messageDispatcher.expect("", new HashSet<Predicate>());

        //then
    }

    @Test
    public void shouldReturnTrueAndForgetMessageIdWhenExpected() {
        //given
        messageDispatcher.expect("toForget", new HashSet<Predicate>());

        //when
        boolean result = messageDispatcher.forget("toForget");

        //then
        assertThat(cache.getIfPresent("toForget")).isNull();
        assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnFalseForgetWhenNotExpected() {
        //given

        //when
        boolean result = messageDispatcher.forget("notexpectdForget");

        //then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldReturnFalseForgetWhenAlreadyhandleed() throws NotFoundException, PredicateNotMetException {
        //given
        messageDispatcher.expect("alreadyhandled", new HashSet<Predicate>());
        messageDispatcher.handle("alreadyhandled", json(object()));

        //when
        boolean result = messageDispatcher.forget("alreadyhandleed");

        //then
        assertThat(result).isFalse();
    }
}
