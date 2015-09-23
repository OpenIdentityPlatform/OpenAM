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
package org.forgerock.openam.radius.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.Properties;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.ReplyMessageAttribute;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;

/**
 * Implements a console based RADIUS client that enables testing of a radius server. Looks for a radius.properties in
 * the current directory and if not found indicates it is missing and what keys and values are needed to run then asks
 * for username and password to start the authentication process and presents access-accept as a SUCCESS output and
 * access-reject as a FAILURE output before exiting. For access-challenge responses it presents the message field and
 * prompts for answer. Following the submitted value it issues a new access-request including the answer in the password
 * field and returning any state field that was in the original access-challenge response.
 */
public class ConsoleClient implements Runnable {
    /**
     * The name of the config file that will be used by the radius command line client.
     */
    public static final String CONFIG_FILE = "radius.properties";

    /**
     * Name of the property containing the host name of the RADIUS server to communicate with.
     */
    public static final String HOST_PROP = "host";

    /**
     * Name of the property containing the port number to be used in communication with the RADIUS server.
     */
    public static final String PORT_PROP = "port";

    /**
     * The name of the property containing the shared secret to be used in communication with the server.
     */
    public static final String SECRET_PROP = "secret";

    /**
     * The name of the property containing the boolean value indicating whether to log RADIUS messages tx/rx'd to the
     * RADIUS server.
     */
    public static final String LOG_TRAFFIC_PROP = "show-traffic";

    private int port = -1;
    private String host = null;
    private String secret = null;
    private boolean logTraffic = false;

    /**
     * Constructor.
     *
     * @param props
     *            a properties class representing the configuration properties to be used by the RADIUS command line
     *            client.
     */
    public ConsoleClient(Properties props) {
        if (!props.containsKey(SECRET_PROP) || !props.containsKey(PORT_PROP) || !props.containsKey(HOST_PROP)) {
            usage();
        }
        this.secret = props.getProperty(SECRET_PROP);
        this.host = props.getProperty(HOST_PROP);
        this.port = Integer.parseInt(props.getProperty(PORT_PROP));
        this.logTraffic = Boolean.parseBoolean(props.getProperty(LOG_TRAFFIC_PROP));
    }

    /**
     * Print the usage to system out.
     */
    private static void usage() {
        System.out.println("Missing required config file '" + CONFIG_FILE + "' in current directory "
                + new File("./").getAbsolutePath());
        System.out.println("Must Contain: ");
        System.out.println(" secret=<shared-secret-with-server>");
        System.out.println(" host=<hostname-or-ip-address>");
        System.out.println(" port=<port-on-target-host>");
        System.out.println();
        System.out.println("May Contain:");
        System.out.println(" show-traffic=true");
        System.exit(1);
    }

    /**
     * Main entry point.
     *
     * @param args
     *            - arguments to the program.
     * @throws IOException
     *             if the properties file can't be found or loaded.
     */
    public static void main(String[] args) throws IOException {
        final File cfg = new File("./" + CONFIG_FILE);

        if (!cfg.exists() || !cfg.isFile()) {
            usage();
        }

        final Properties props = new Properties();
        props.load(new FileReader(cfg));

        final ConsoleClient client = new ConsoleClient(props);
        client.run();
    }

    /**
     * Obtain input from the user.
     *
     * @param label
     *            the label describing the input value.
     * @param message
     *            - a message describing what input is being requested.
     * @return the user input.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private String getUserInputFor(String label, String message) throws IOException {
        if (message != null) {
            System.out.println("---> " + message);
        }
        System.out.print("? " + label + ": ");
        System.out.flush();
        return new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    /**
     * Calls the server in a thread.
     */
    @Override
    public void run() {

        try {
            final DatagramChannel chan = DatagramChannel.open();
            short reqId = 1; // request id
            final SecureRandom random = new SecureRandom();
            final InetSocketAddress serverAddr = new InetSocketAddress(this.host, this.port);
            final NASIPAddressAttribute nasAddr = new NASIPAddressAttribute(InetAddress.getLocalHost());
            final NASPortAttribute nasPort = new NASPortAttribute(chan.socket().getLocalPort());
            StateAttribute state = null;

            // String username = "boydmr"; // TODO: restore
            final String username = getUserInputFor("Username", null);
            // String passwordOrAnswer = "password"; // TODO: restore
            String passwordOrAnswer = getUserInputFor("Password", null);
            System.out.println();

            boolean finished = false;
            final ByteBuffer bufIn = ByteBuffer.allocate(4096); // ready for writing

            while (!finished) {
                final RequestAuthenticator reqAuthR = new RequestAuthenticator(random, this.secret);
                final AccessRequest req = new AccessRequest(reqId++, reqAuthR);
                req.addAttribute(new UserNameAttribute(username));
                req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(), this.secret, passwordOrAnswer));
                req.addAttribute(nasAddr);
                req.addAttribute(nasPort);
                if (state != null) {
                    req.addAttribute(state);
                }
                final ByteBuffer reqBuf = ByteBuffer.wrap(req.getOctets());

                if (logTraffic) {
                    System.out.println("Packet To " + host + ":" + port);
                    System.out.println(RadiusRequestContext.getPacketRepresentation(req));
                }
                chan.send(reqBuf, serverAddr);

                // now handle responses possibly sending additional requests
                chan.receive(bufIn);
                bufIn.flip(); // prepare buffer for reading out
                final Packet res = PacketFactory.toPacket(bufIn);
                bufIn.clear(); // prepare buffer for next response

                if (logTraffic) {
                    System.out.println("Packet From " + host + ":" + port);
                    System.out.println(RadiusRequestContext.getPacketRepresentation(res));
                }

                if (res instanceof AccessReject) {
                    System.out.println("---> Sorry. Not Authenticated.");
                    System.out.println();
                    finished = true;
                } else if (res instanceof AccessAccept) {
                    System.out.println("---> SUCCESS! You've Authenticated!");
                    System.out.println();
                    finished = true;
                } else if (res instanceof AccessChallenge) {
                    final AccessChallenge chng = (AccessChallenge) res;
                    state = (StateAttribute) getAttribute(StateAttribute.class, res);
                    final ReplyMessageAttribute msg = (ReplyMessageAttribute) getAttribute(ReplyMessageAttribute.class,
                            res);
                    String message = null;

                    if (msg != null) {
                        message = msg.getMessage();
                    }
                    passwordOrAnswer = getUserInputFor("Answer", message);
                    System.out.println();
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create an attribute from a packet.
     *
     * @param clazz
     *            - the class of the attribute to create.
     * @param res
     *            - the packet
     * @return - an Attribute
     */
    private Attribute getAttribute(Class clazz, Packet res) {
        final AttributeSet atts = res.getAttributeSet();

        for (int i = 0; i < atts.size(); i++) {
            final Attribute att = atts.getAttributeAt(i);
            if (att.getClass() == clazz) {
                return att;
            }
        }
        return null;
    }
}
