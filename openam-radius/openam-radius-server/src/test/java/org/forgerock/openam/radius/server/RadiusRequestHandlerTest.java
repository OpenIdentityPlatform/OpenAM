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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
/*
 * Portions copyright 2015 ForgeRock AS
 */

package org.forgerock.openam.radius.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.Utils;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.forgerock.openam.radius.server.spi.handlers.AcceptAllHandler;
import org.forgerock.openam.radius.server.spi.handlers.RejectAllHandler;
import org.forgerock.util.promise.PromiseImpl;
import org.testng.annotations.Test;

public class RadiusRequestHandlerTest {

    private final String res = "01 00 00 38 0f 40 3f 94 73 97 80 57 bd 83 d5 cb "
            + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d " + "93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8 "
            + "01 10 05 06 00 00 00 03";

    /**
     * Test that when run is called with an AcceptAllHandler then the RadiusAuthResponse contains a success code and an
     * AcceptResponse is sent.
     *
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     * @throws RadiusProcessingException
     */
    @Test
    public void testRun() throws UnsupportedEncodingException, InterruptedException, RadiusProcessingException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);

        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(clientConfig.getName()).thenReturn("TestConfig");
        when(clientConfig.getAccessRequestHandlerClass()).thenReturn(AcceptAllHandler.class);

        final ByteBuffer bfr = Utils.toBuffer(res);
        final PromiseImpl<RadiusAuthResult, RadiusProcessingException> promise = PromiseImpl.create();
        final RadiusRequestHandler handler = new RadiusRequestHandler(reqCtx, bfr, promise, promise);
        handler.run();

        // when
        final RadiusAuthResult result = promise.getOrThrow();

        // then
        assertThat(result.getRequestResult().equals(RadiusAuthResultStatus.COMPLETED));
        verify(reqCtx, times(1)).send(isA(AccessAccept.class));
  }

    @Test
    public void testRunReject() throws UnsupportedEncodingException, InterruptedException, RadiusProcessingException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);
        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(clientConfig.getName()).thenReturn("TestConfig");
        when(clientConfig.getAccessRequestHandlerClass()).thenReturn(RejectAllHandler.class);


        final ByteBuffer bfr = Utils.toBuffer(res);
        final PromiseImpl<RadiusAuthResult, RadiusProcessingException> promise = PromiseImpl.create();
        final RadiusRequestHandler handler = new RadiusRequestHandler(reqCtx, bfr, promise, promise);
        handler.run();

        // when
        final RadiusAuthResult result = promise.getOrThrow();

        // then
        assertThat(result.getRequestResult().equals(RadiusAuthResultStatus.FAILED));
        verify(reqCtx, times(1)).send(isA(AccessReject.class));
    }

    @Test(expectedExceptions = RadiusProcessingException.class)
    public void testRunCatestrophic() throws UnsupportedEncodingException, InterruptedException,
            RadiusProcessingException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);

        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(clientConfig.getName()).thenReturn("TestConfig");
        when(clientConfig.getAccessRequestHandlerClass()).thenReturn(CatastrophicHandler.class);

        final ByteBuffer bfr = Utils.toBuffer(res);
        final PromiseImpl<RadiusAuthResult, RadiusProcessingException> promise = PromiseImpl.create();
        final RadiusRequestHandler handler = new RadiusRequestHandler(reqCtx, bfr, promise, promise);
        handler.run();

        // when
        final RadiusAuthResult result = promise.getOrThrow();

        // then
        assertThat(result.getRequestResult().equals(RadiusAuthResultStatus.FAILED));
        verify(reqCtx, never()).send(isA(Packet.class));
    }
}
