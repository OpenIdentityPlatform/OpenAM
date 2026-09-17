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
 * Copyright 2026 3A Systems, LLC.
 */
package org.openidentityplatform.openam.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.junit.After;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class TokenStorageAdapterTest {

	@After
	public void forgetPreparedStatement() {
		TokenStorageAdapter.static_statement_update = null;
	}

	/**
	 * A CTS token id is a session id or an OAuth2 token: it must never be written
	 * to the log, only a short digest that identifies the record. Every session id
	 * starts with the same "AQIC" header, so a prefix could not tell two apart.
	 */
	@Test
	public void maskTokenIdReplacesTheIdWithAShortDigest() {
		assertEquals("sha256:983d2944", TokenStorageAdapter.maskTokenId("AQIC5wM2LY4SfczntBcXfFoFJwA6zAV2i4fnU8Sd7ao"));
		assertEquals("sha256:d5989e92", TokenStorageAdapter.maskTokenId("AQIC5wM2LY4Sfczn-expired-session-token"));
		assertEquals("sha256:b8150354", TokenStorageAdapter.maskTokenId("AQIC5wM2LY4Sfczn-fresh-session-token"));
		assertEquals("null", TokenStorageAdapter.maskTokenId(null));
	}

	/**
	 * The warning written when a token field cannot be read during {@code update}
	 * must carry the digest of the token id, never the id itself.
	 */
	@Test
	public void updateLogsTheDigestNotTheIdWhenAFieldCannotBeRead() throws Exception {
		String tokenId = "AQIC5wM2LY4SfczntBcXfFoFJwA6zAV2i4fnU8Sd7ao";
		Token token = mock(Token.class);
		when(token.getTokenId()).thenReturn(tokenId);
		when(token.getType()).thenReturn(TokenType.SESSION);
		when(token.getExpiryTimestamp()).thenReturn(Calendar.getInstance());
		when(token.getAttribute(any(CoreTokenField.class))).thenThrow(new IllegalArgumentException("boom"));

		// The statement is pre-set, so the adapter never reaches Cassandra: the read
		// of the first field fails before the statement is executed.
		BoundStatement bound = mock(BoundStatement.class, RETURNS_SELF);
		PreparedStatement prepared = mock(PreparedStatement.class);
		when(prepared.bind()).thenReturn(bound);
		TokenStorageAdapter.static_statement_update = prepared;
		TokenStorageAdapter adapter = new TokenStorageAdapter(null, null);

		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(TokenStorageAdapter.class)).addAppender(appender);
		try {
			adapter.update(token, true);
			fail("update must propagate the failed field read");
		} catch (DataLayerException expected) {
			// the warning is written before the exception is wrapped
		} finally {
			((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(TokenStorageAdapter.class)).detachAppender(appender);
		}

		List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
		assertTrue(messages.toString(), messages.stream().anyMatch(m -> m.contains("SESSION token sha256:983d2944: java.lang.IllegalArgumentException: boom")));
		assertTrue(messages.toString(), messages.stream().noneMatch(m -> m.contains(tokenId)));
	}
}
