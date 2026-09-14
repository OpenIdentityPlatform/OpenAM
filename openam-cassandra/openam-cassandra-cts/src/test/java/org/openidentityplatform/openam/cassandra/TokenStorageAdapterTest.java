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

import org.junit.Test;

public class TokenStorageAdapterTest {

	/**
	 * A CTS token id is a session id or an OAuth2 token: it must never be written
	 * to the log in full, only a short prefix that identifies the record.
	 */
	@Test
	public void maskTokenIdKeepsOnlyAShortPrefix() {
		assertEquals("AQIC***", TokenStorageAdapter.maskTokenId("AQIC5wM2LY4SfczntBcXfFoFJwA6zAV2i4fnU8Sd7ao"));
		assertEquals("***", TokenStorageAdapter.maskTokenId("short"));
		assertEquals("***", TokenStorageAdapter.maskTokenId(""));
		assertEquals("null", TokenStorageAdapter.maskTokenId(null));
	}
}
