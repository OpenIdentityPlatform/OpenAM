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
 * $Id: JSSProxy.java,v 1.6 2009/02/19 05:29:04 beomsuk Exp $
 *
 */
/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.https;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.StringTokenizer;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

public class JSSProxy implements Runnable {
    public static HashMap connectHashMap = new HashMap();
    public static int serverPort;
    private static Thread thread;
    private static int threadPoolSize;
    public static JSSThreadPool threadPool;
    private static ServerSocket sconnection = null;
    private static Debug debug = Debug.getInstance("amJSS");
    private volatile boolean isShutdownCalled = false;

    static {
        try {
             sconnection = new ServerSocket(0, 50);
             try {
                threadPoolSize = Integer.parseInt(SystemPropertiesManager.get(
                    "com.iplanet.am.jssproxy.threadpoolSize"));
            } catch (Exception ex) {
                threadPoolSize = JSSThreadPool.DEFAULT_THREAD_POOL_SIZE;
            }

            if (debug.messageEnabled()) {
                debug.message("JSSThreadPool size = " + threadPoolSize);
            }

            threadPool = new JSSThreadPool("amJSS", threadPoolSize, false,
                debug);
        } catch (IOException e) {
            debug.error("JSSProxy: Unable to create server socket", e);
            sconnection = null;
        }
 
        if (sconnection != null) {
            serverPort = sconnection.getLocalPort();
            try {
                JSSProxy tempInstance = null;
                thread = null;
                ShutdownManager shutdownMan = ShutdownManager.getInstance();
                if (shutdownMan.acquireValidLock()) {
                    try {
                        tempInstance = new JSSProxy();
                        thread = new Thread(tempInstance);
                        final JSSProxy finalInstance = tempInstance;
                        shutdownMan.addShutdownListener(new
                            ShutdownListener() {

                            public void shutdown() {
                                if (finalInstance != null) {
                                    finalInstance.shutdown();
                                }
                                if (threadPool != null) {
                                    threadPool.shutdown();
                                }
                            }

                        });
                    } finally {
                        shutdownMan.releaseLockAndNotify();
                    }
                }
                final JSSProxy instance = tempInstance;
                if (thread != null) {
                    thread.start();
                }
            }
            catch (Exception e) {
                debug.error("JSSProxy: Unable to run JSSProxy", e);

                try {
                    sconnection.close();
                }
                catch (IOException ee) {
                }
            }
        }
    }

    private JSSProxy() {
    }

    public void shutdown() {
        try {
            isShutdownCalled = true;
            sconnection.close();
        } catch (Exception ex) {
            //ignored
        }
    }
    
    public void run() {
        Socket inconnection;

        while (true) {
            try {
                inconnection = (Socket) java.security.AccessController.
                    doPrivileged(new java.security.
                        PrivilegedExceptionAction(){
                            public Object run() throws IOException {
                                return sconnection.accept();
                            }
                        });
                try {
                    inconnection.setTcpNoDelay(true);
                }
                catch (SocketException e) {
                    debug.error("JSSProxy: Unable to  TcpNoDelay.",e);
                    try {
                        inconnection.close();
                    } 
                    catch (IOException ee) {
                    } 
                    finally {
                        inconnection = null;
                    }
                    continue;
                }
                JSSProxy.JSSProxySessionRunnable p =
                            new JSSProxySessionRunnable(inconnection);
                threadPool.run(p);
            } catch (SecurityException ex) {
                debug.error("JSSProxy: SecurityException:", ex);
            } catch (RuntimeException ex) {
                debug.error("JSSProxy: RuntimeException:", ex);
                // decide what to log here
            } catch (Exception ex) {
                // don't need to rethrow
                debug.error("JSSProxy: Exception:", ex);
                if (isShutdownCalled) {
                    if (ex.getClass().getName().equals(
                        SocketException.class.getName())) {
                        break;
                    }
                }
            } catch (Throwable e) {
                debug.error("JSSProxy: Uncaught exception:", e);
                // decide what to log here
                // rethrow Error here
                throw new Error(e);
	    }
        }
    }

    private class JSSProxySessionRunnable implements Runnable {
        private Socket inconnection = null;
        private Socket toProxySocket = null;

        JSSProxySessionRunnable(Socket socket) {
            inconnection = socket;
        }

        public void run() {
            Integer remotePort = new Integer(inconnection.getPort());
            byte[] prebuffer = new byte[1];

            DataInputStream inFrom;
            try{
                inFrom = new DataInputStream(inconnection.getInputStream());
                inFrom.readFully(prebuffer, 0, 1);
            } catch (IOException e) {
                debug.error(
                    "JSSProxySessionRunnable: Unable to open input stream on "
                    + inconnection, e);

                connectHashMap.remove(remotePort);
                closeSockets();
                return;
            }

            String info = (String)connectHashMap.remove(remotePort);

            StringTokenizer st = new StringTokenizer(info);

            String host = st.nextToken();
            int    port;
            try {
                 port = Integer.parseInt(st.nextToken());
            }
            catch (Exception ex) {
                 port = 8080;
            }

            try {
                toProxySocket = new Socket(host, port);
                toProxySocket.setTcpNoDelay(true);
            } 
            catch(Exception ex) {
                toProxySocket = null;
                if (debug.messageEnabled()) {
                    debug.message("JSSProxySessionRunnable: " +
                        "Unable to connect to " + host + ":" + port + ". ",ex);
                }
            } 

            if (toProxySocket == null) {
                closeSockets();
                return;
            }

            String desthost = st.nextToken();
            String destport = st.nextToken();

            OutputStream out = null;
            InputStream in = null;
            byte reply[] = new byte[200];
            int replyLen = 0;
            int newlinesSeen = 0;
            boolean headerDone = false;     // Done on first newline


            try {
                out = toProxySocket.getOutputStream();
                String msg =
                     "CONNECT " + desthost + ":" + destport + " HTTP/1.0\n" +
                     "User-Agent: " +
                     sun.net.www.protocol.http.HttpURLConnection.userAgent +
                     "\r\n\r\n";
                debug.message(msg);

                byte b[];
                try {
                    b = msg.getBytes("ASCII7");
                } 
                catch (UnsupportedEncodingException ignored) {
                    // If ASCII7 isn't there, something serious is wrong, but
                    // Paranoia Is Good (tm)
                    b = msg.getBytes();
                }

                out.write(b);
                out.flush();

                // We need to store the reply so we can create a detailed
                // error message to the user.

                in = toProxySocket.getInputStream();
        
                boolean         error = false;

                while (newlinesSeen < 2) {
                    int i = in.read();
                    if (i < 0) {
                        debug.error("JSSProxySessionRunnable: " + 
                                "Unexpected EOF from proxy");
                        closeSockets();
                        return;
                    }

                    if (i == '\n') {
                        headerDone = true;
                        ++newlinesSeen;
                    }
                    else if (i != '\r') {
                        newlinesSeen = 0;
                        if (!headerDone && replyLen < reply.length) {
                            reply[replyLen++] = (byte) i;
                        }
                    }
                }
            }
            catch (IOException ioe) {
                debug.error("JSSProxySessionRunnable: " +
                                     "Unable to get OutputStream", ioe);
                closeSockets();
                return;
            }


            // Converting the byte array to a string is slightly wasteful
            // in the case where the connection was successful, but it's
            // insignificant compared to the network overhead.
            String replyStr;
            try {
                replyStr = new String(reply, 0, replyLen, "ASCII7");
            }
            catch (UnsupportedEncodingException ignored) {
                replyStr = new String(reply, 0, replyLen);
            }

            // We asked for HTTP/1.0, so we should get that back
            if (!replyStr.startsWith("HTTP/1.0 200")) {
                debug.error(
                    "JSSProxySessionRunnable: Unable to tunnel through ");
                closeSockets();
                return;
            }

            try {
                out.write(prebuffer);
                out.flush();
            }
            catch (IOException ioe) {
                debug.error("JSSProxySessionRunnable: " +
                                     "Unable to write prebuffer.", ioe);
                closeSockets();
                return;
            }

            new RWGroupJSSProxy(inconnection, toProxySocket);
        }

        void closeSockets() {
            if (inconnection != null) {
                try {
                    inconnection.close();

                }
                catch (Exception e1) {
                }
            }
            if (toProxySocket != null) {
                try {
                    toProxySocket.close();
                }
                catch (Exception e1) {
                }
            }
        }
    }
}
