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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.CoreTokenAdapter;
import org.forgerock.openam.cts.utils.blob.TokenBlobStrategy;
import org.forgerock.util.Options;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CTSPersistentStoreImplTest {

    private CoreTokenAdapter mockAdapter;
    private CTSPersistentStoreImpl impl;

    @BeforeMethod
    public void setup() {
        mockAdapter = mock(CoreTokenAdapter.class);
        impl = new CTSPersistentStoreImpl(mockAdapter, mock(Debug.class));
    }

    @Test
    public void shouldUseAdapterForRead() throws CoreTokenException {
        String badger = "badger";
        impl.read(badger);
        verify(mockAdapter).read(eq(badger), any(Options.class));
    }

    @Test
    public void shouldReturnNullForTokenNotFound() throws CoreTokenException {
        given(mockAdapter.read(anyString(), any(Options.class))).willReturn(null);
        assertThat(impl.read("")).isNull();
    }
}