/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: RadiusConn.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.radius.client;

import java.util.*;
import java.security.*;
import java.net.*;
import java.io.*;

/**
 * This class implements RFC2865 - Remote Authentication Dial In
 * User Service (RADIUS), June 2000.
 */
public class RadiusConn {
    public static final int MAX_RETRIES = 10;
    public static final int RETRIES = 5;
    public static int OFFICAL_PORT = 1812;
    public static int DEFAULT_PORT = 1645;
    public static int DEFAULT_TIMEOUT = 5;
    
    public static String OPTION_DEBUG = "OPTION_DEBUG";
    
    private Properties _options = null;
    private boolean _traceOn = true;
    private String _host[] = new String[2];
    private int _port[] = new int[2];
    private static int _selected = 0;
    private String _secret = null;
    private DatagramSocket _socket = null;
    private short _id = (short)System.currentTimeMillis();
    private SecureRandom _rand = null;
    
    public RadiusConn(String host1, String host2, int port, String secret,
    int timeout) throws SocketException {
        this(host1, port, host2, port, secret, timeout, null, null);
    }
    
    public RadiusConn(String host, int port, String secret, byte seed[],
    Properties options)
    throws SocketException {
        this(host, port, host, port, secret, DEFAULT_TIMEOUT, seed, options);
    }
    
    public RadiusConn(String host1, int port1, String host2, int port2,
    String secret, int timeout, byte seed[], Properties options)
    throws SocketException {
        _host[0] = host1;
        _port[0] = port1;
        _host[1] = host2;
        _port[1] = port2;
        _secret = secret;
        _options = options;
        _socket = new DatagramSocket();
        _socket.setSoTimeout(timeout * 1000);
        if (seed == null) {
            _rand = new SecureRandom();
        } else {
            _rand = new SecureRandom(seed);
        }
    }
    
    
    public void disconnect() throws IOException {
        _socket.disconnect();
    }
    
    public void authenticate(String name, String password)
    throws IOException, NoSuchAlgorithmException,
    RejectException, ChallengeException {
        int retries = 0;
        Packet res = null;
        AccessRequest req = createAccessRequest();
        req.addAttribute(new UserNameAttribute(name));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
        _secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(_socket.getLocalPort()));
        do {
            send(req, _host[_selected], _port[_selected]);
            try {
                retries++;
                res = receive();
                if (res instanceof AccessReject) {
                    throw new RejectException((AccessReject)res);
                } else if (res instanceof AccessChallenge) {
                    throw new ChallengeException((AccessChallenge)res);
                }
            } catch (InterruptedIOException e) {
                if (retries >= MAX_RETRIES) {
                    // both servers are down, throw exception
                    retries = 0;
                    throw e;
                }
                if (retries == RETRIES) {
                    // switch server if Retries reaches limit
                    if (_selected == 0) {
                        _selected = 1;
                    } else {
                        _selected = 0;
                    }
                }
                
            }
        } while (res == null);
    }
    
    public void replyChallenge(String password, ChallengeException ce)
    throws IOException, NoSuchAlgorithmException,
    RejectException, ChallengeException {
        replyChallenge(null, password, ce);
    }
    
    public void replyChallenge(String name, String password,
    ChallengeException ce)
    throws IOException, NoSuchAlgorithmException,
    RejectException, ChallengeException {
        StateAttribute state = (StateAttribute)
        ce.getAttributeSet().getAttributeByType(Attribute.STATE);
        if (state == null)
            throw new IOException("State not found in challenge");
        AccessRequest req = createAccessRequest();
        req.addAttribute(state); // needed in challenge
        if (name != null) {
            req.addAttribute(new UserNameAttribute(name));
        }
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
        _secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(_socket.getLocalPort()));
        
        send(req, _host[_selected], _port[_selected]);
        Packet res = receive();
        if (res instanceof AccessReject) {
            throw new RejectException((AccessReject)res);
        } else if (res instanceof AccessChallenge) {
            throw new ChallengeException((AccessChallenge)res);
        }
    }
    
    public void replyChallenge(String name, String password, String state)
    throws IOException, NoSuchAlgorithmException,
    RejectException, ChallengeException {
        if (state == null)
            throw new IOException("State not found in challenge");
        AccessRequest req = createAccessRequest();
        req.addAttribute(new StateAttribute(state)); // needed in challenge
        req.addAttribute(new UserNameAttribute(name));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
        _secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(_socket.getLocalPort()));
        
        send(req, _host[_selected], _port[_selected]);
        Packet res = receive();
        if (res instanceof AccessReject) {
            throw new RejectException((AccessReject)res);
        } else if (res instanceof AccessChallenge) {
            throw new ChallengeException((AccessChallenge)res);
        }
    }
    
    private short getIdentifier() {
        return _id++;
    }
    
    private void send(NASPacket packet, String host, int port)
    throws IOException {
        DatagramPacket dp = new DatagramPacket(new byte[4096], 4096);
        dp.setPort(port);
        dp.setAddress(InetAddress.getByName(host));
        byte data[] = packet.getData();
        dp.setLength(data.length);
        dp.setData(data);
        _socket.send(dp);
        if (_traceOn)
            trace("Sent " + packet);
    }
    
    private ServerPacket receive()
    throws IOException {
        DatagramPacket dp = new DatagramPacket(new byte[4096], 4096);
        _socket.receive(dp);
        byte data[] = dp.getData();
        ServerPacket p = PacketFactory.createServerPacket(data);
        if (_traceOn)
            trace("Received " + p + " size=" + p.getAttributeSet().size());
        return p;
    }
    
    private AccessRequest createAccessRequest() throws 
        NoSuchAlgorithmException {
        RequestAuthenticator ra = new RequestAuthenticator(_rand, _secret);
        AccessRequest req = new AccessRequest(getIdentifier(), ra);
        return req;
    }
    
    private void trace(String msg) {
        System.out.println("TRACE: " + msg);
        System.out.flush();
    }
}
