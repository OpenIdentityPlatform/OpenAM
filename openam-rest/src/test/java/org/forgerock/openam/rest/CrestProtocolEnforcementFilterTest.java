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

package org.forgerock.openam.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CrestProtocolEnforcementFilterTest {

    private CrestProtocolEnforcementFilter filter;

    @BeforeMethod
    public void setup() {
        filter = new CrestProtocolEnforcementFilter();
    }

    @Test
    public void requestWithCorrectProtocolVersionShouldCallHandler() {

        //Given
        Context context = mock(Context.class);
        Request request = new Request();
        Handler next = mock(Handler.class);

        request.getHeaders().put(AcceptApiVersionHeader.valueOf("protocol=1"));

        //When
        filter.filter(context, request, next);

        //Then
        assertThat(AcceptApiVersionHeader.valueOf(request).getProtocolVersion()).isEqualTo(version(1));
        verify(next).handle(context, request);
    }

    @Test
    public void requestWithNoProtocolVersionShouldBeDefaultedAndThenCallHandler() {

        //Given
        Context context = mock(Context.class);
        Request request = new Request();
        Handler next = mock(Handler.class);

        //When
        filter.filter(context, request, next);

        //Then
        assertThat(AcceptApiVersionHeader.valueOf(request).getProtocolVersion()).isEqualTo(version(1));
        verify(next).handle(context, request);
    }

    @Test
    public void requestWithIncorrectProtocolMinorVersionShouldReturnBadRequestResponse() throws IOException {

        //Given
        Context context = mock(Context.class);
        Request request = new Request();
        Handler next = mock(Handler.class);

        request.getHeaders().put(AcceptApiVersionHeader.valueOf("protocol=1.1"));

        //When
        Response response = filter.filter(context, request, next).getOrThrowUninterruptibly();

        //Then
        assertThat(getUnsupportedMinorVersionExceptionJson(version(1, 1))).isEqualTo(response.getEntity().getJson());
        assertThat(AcceptApiVersionHeader.valueOf(request).getProtocolVersion()).isEqualTo(version(1, 1));
        verify(next, never()).handle(context, request);
    }

    @Test
    public void requestWithIncorrectProtocolMajorVersionShouldReturnBadRequestResponse() throws IOException {

        //Given
        Context context = mock(Context.class);
        Request request = new Request();
        Handler next = mock(Handler.class);

        request.getHeaders().put(AcceptApiVersionHeader.valueOf("protocol=2"));

        //When
        Response response = filter.filter(context, request, next).getOrThrowUninterruptibly();

        //Then
        assertThat(getUnsupportedMajorVersionExceptionJson(version(2))).isEqualTo(response.getEntity().getJson());
        assertThat(AcceptApiVersionHeader.valueOf(request).getProtocolVersion()).isEqualTo(version(2));
        verify(next, never()).handle(context, request);
    }

    private Object getUnsupportedMajorVersionExceptionJson(Version version) {
        return new BadRequestException("Unsupported major version: " + version).toJsonValue().getObject();
    }

    private Object getUnsupportedMinorVersionExceptionJson(Version version) {
        return new BadRequestException("Unsupported minor version: " + version).toJsonValue().getObject();
    }
}
