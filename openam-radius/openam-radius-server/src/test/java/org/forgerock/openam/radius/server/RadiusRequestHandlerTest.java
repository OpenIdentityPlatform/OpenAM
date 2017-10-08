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

import org.forgerock.guava.common.eventbus.EventBus;
import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.Utils;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.forgerock.openam.radius.server.spi.handlers.AcceptAllHandler;
import org.forgerock.openam.radius.server.spi.handlers.RejectAllHandler;
import org.testng.annotations.Test;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the <code>RadiusRequestHandler</code> class.
 *
 * @see org.forgerock.openam.radius.server.RadiusRequestHandler
 */
public class RadiusRequestHandlerTest {

    private final String res = "01 00 00 38 0f 40 3f 94 73 97 80 57 bd 83 d5 cb "
            + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d " + "93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8 "
            + "01 10 05 06 00 00 00 03";

    /**
     * Test that when run is called with an AcceptAllHandler that the resultant packet sent via the request
     * context is an ACCESS_ACCEPT packet.
     *
     * @throws InterruptedException - when an interrupt occurs.
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     * @throws UnknownHostException - if the host can't be determined
     */
    @Test(enabled = true)
    public void testRun() throws UnknownHostException, InterruptedException, RadiusProcessingException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);
        String url = "forgerock.org";
        InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getByName(url), 6836);

        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(reqCtx.getSource()).thenReturn(socketAddress);
        when(clientConfig.getName()).thenReturn("TestConfig");

        final ByteBuffer bfr = Utils.toBuffer(res);
        EventBus eventBus = new EventBus();

        AccessRequestHandlerFactory accessRequestHandlerFactory = mock(AccessRequestHandlerFactory.class);
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new AcceptAllHandler());
        final RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus);

        //when
        handler.run();

        // then
        verify(reqCtx, times(1)).send(isA(AccessAccept.class));
    }

    /**
     * Test that when run is called with an RejectAllHandler that the resultant packet sent via the request
     * context is an ACCESS_REJECT packet.
     *
     * @throws InterruptedException - when an interrupt occurs.
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     * @throws UnknownHostException - if the host can't be determined
     */
    @Test(enabled = true)
    public void testRunReject()
            throws UnknownHostException, InterruptedException, RadiusProcessingException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);
        String url = "forgerock.org";
        InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getByName(url), 6836);

        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(reqCtx.getSource()).thenReturn(socketAddress);
        when(clientConfig.getName()).thenReturn("TestConfig");
        when(clientConfig.getAccessRequestHandlerClass()).thenReturn(RejectAllHandler.class);

        final ByteBuffer bfr = Utils.toBuffer(res);
        EventBus eventBus = new EventBus();

        AccessRequestHandlerFactory accessRequestHandlerFactory = mock(AccessRequestHandlerFactory.class);
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new RejectAllHandler());

        final RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus);

        // when
        handler.run();

        // then
        verify(reqCtx, times(1)).send(isA(AccessReject.class));
    }

    /**
     * Test that when run is called with an CatestrophicHandler that the run completes without sending any
     * packet. (ie: the packet is silently dropped since it can't be handled.)
     *
     * @throws InterruptedException - when an interrupt occurs.
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     * @throws UnknownHostException - if the host can't be determined
     */
    @Test(enabled = true)
    public void testRunCatestrophic() throws InterruptedException, RadiusProcessingException, UnknownHostException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);
        String url = "forgerock.org";
        InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getByName(url), 6836);

        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(reqCtx.getSource()).thenReturn(socketAddress);
        when(clientConfig.getName()).thenReturn("TestConfig");
        when(clientConfig.getAccessRequestHandlerClass()).thenReturn(CatastrophicHandler.class);

        final ByteBuffer bfr = Utils.toBuffer(res);
        EventBus eventBus = new EventBus();

        AccessRequestHandlerFactory accessRequestHandlerFactory = mock(AccessRequestHandlerFactory.class);
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new CatastrophicHandler());

        final RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus);

        // when
        handler.run();

        // then
        verify(reqCtx, never()).send(isA(Packet.class));
    }
}
