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
package com.sun.identity.shared.jaxrpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.rmi.RemoteException;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * The class named in a SOAP fault's <code>faultstring</code> comes from the
 * response, so it must not be initialised or constructed unless it is one
 * of the exceptions the JAX-RPC ties send.
 */
public class SOAPClientFaultTest {

    // Kept outside Marker: reading a static field of Marker would itself initialise it.
    static volatile boolean markerInitialised;
    static volatile String markerConstructedWith;

    /** Not an exception, and in the same package as the client. */
    public static class Marker {
        static {
            markerInitialised = true;
        }

        public Marker(String value) {
            markerConstructedWith = value;
        }
    }

    @Test
    public void refusesJdkClassWithSideEffectingConstructor() throws Exception {
        File dir = Files.createTempDirectory("soapfault").toFile();
        File target = new File(dir, "created-by-fault");

        Exception e = parseFault("java.io.FileOutputStream",
            target.getAbsolutePath());

        assertThat(target).doesNotExist();
        assertThat(e.getClass()).isEqualTo(Exception.class);
        assertThat(e.getMessage()).isEqualTo(target.getAbsolutePath());
        dir.delete();
    }

    @Test
    public void refusesNonExceptionClassWithoutInitialisingIt() throws Exception {
        Exception e = parseFault(Marker.class.getName(), "payload");

        assertThat(markerInitialised).isFalse();
        assertThat(markerConstructedWith).isNull();
        assertThat(e.getClass()).isEqualTo(Exception.class);
        assertThat(e.getMessage()).isEqualTo("payload");
    }

    @Test
    public void refusesExceptionNotSentByTies() throws Exception {
        Exception e = parseFault(IllegalStateException.class.getName(), "probe");

        assertThat(e.getClass()).isEqualTo(Exception.class);
        assertThat(e.getMessage()).isEqualTo("probe");
    }

    @Test
    public void buildsExceptionSentByTies() throws Exception {
        Exception e = parseFault(SSOException.class.getName(), "denied");

        assertThat(e).isExactlyInstanceOf(SSOException.class);
        assertThat(e.getMessage()).isEqualTo("denied");
    }

    @Test
    public void buildsRemoteException() throws Exception {
        Exception e = parseFault(RemoteException.class.getName(), "server down");

        assertThat(e).isExactlyInstanceOf(RemoteException.class);
        assertThat(e.getMessage()).isEqualTo("server down");
    }

    @Test
    public void unknownClassFallsBackToPlainException() throws Exception {
        // Allowed, but lives in openam-core, which is not on this classpath.
        Exception e = parseFault("com.sun.identity.sm.SMSException", "gone");

        assertThat(e.getClass()).isEqualTo(Exception.class);
        assertThat(e.getMessage()).isEqualTo("gone");
    }

    private static Exception parseFault(String className, String message)
            throws Exception {
        String xml = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><soap:Fault><faultcode>soap:Server</faultcode>"
            + "<faultstring>" + className + "</faultstring>"
            + "<detail><message>" + message + "</message></detail>"
            + "</soap:Fault></soap:Body></soap:Envelope>";
        SOAPClient.SOAPContentHandler handler =
            new SOAPClient(new String[] {"http://localhost/"}).new SOAPContentHandler(true);
        XMLReader reader = XMLUtils.getSafeSAXParser(false).getXMLReader();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(new StringReader(xml)));
        assertThat(handler.isException()).isTrue();
        return handler.getException();
    }
}
