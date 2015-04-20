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

package com.sun.identity.setup;

import static org.forgerock.openam.utils.IOUtils.closeIfNotNull;
import static org.forgerock.openam.utils.IOUtils.readStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletContext;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Map;

import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;

/**
 * Utility methods that are used by various setup/startup classes.
 *
 * @since 13.0.0
 */
public final class AMSetupUtils {

    private static final Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
    private static final String HTTPS = "https";
    private static final String RANDOM_STRING_ALGORITHM = "SHA1PRNG";

    private AMSetupUtils() {
    }

    /**
     * Reads the contents of the provided {@code File}.
     *
     * <p>The file is located using the provided {@code ServletContext}.</p>
     *
     * @param servletContext The {@code ServletContext} to use to find the file.
     * @param file The {@code File} to read.
     * @return The contents of the file.
     * @throws IOException If the file could not be found or read.
     */
    public static String readFile(ServletContext servletContext, String file) throws IOException {
        InputStream inputStream;
        if ((inputStream = getResourceAsStream(servletContext, file)) == null) {
            throw new IOException(file + " not found");
        }
        return readStream(inputStream);
    }

    /**
     * Creates a new secure random string.
     *
     * @return A secure random string.
     */
    public static String getRandomString() {
        try {
            byte[] bytes = new byte[24];
            SecureRandom random = SecureRandom.getInstance(RANDOM_STRING_ALGORITHM);
            random.nextBytes(bytes);
            return Base64.encode(bytes);
        } catch (Exception e) {
            debug.message("AMSetupUtils.getRandomString:Exception in generating encryption key.", e);
        }
        return SetupConstants.CONFIG_VAR_DEFAULT_SHARED_KEY;
    }

    /**
     * Returns the next unused port on a given host.
     *
     * @param hostname The name of the host, (eg localhost).
     * @param start The starting port number to check, (eg 389).
     * @param increment The port number increments to check, (eg 1000).
     * @return The next available port number, or -1 if no available ports found.
     */
    public static int getFirstUnusedPort(String hostname, int start, int increment) {
        for (int i = start; i < 65500; i += increment) {
            if (isPortInUse(hostname, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks whether the given host and port is currently under use.
     *
     * @param hostname The name of the host, (eg localhost).
     * @param port The port number to check.
     * @return {@code true} if in use, {@code false} if not in use.
     */
    public static boolean isPortInUse(String hostname, int port) {
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(hostname, port);
            return !isPortBound(socketAddress) && !isPortBeingListenedOn(socketAddress);
        } catch (NullPointerException e) {
            return false;
        }
    }

    private static boolean isPortBound(InetSocketAddress socketAddress) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(socketAddress);
            return false;
        } catch (IOException e) {
            return true;
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static boolean isPortBeingListenedOn(InetSocketAddress socketAddress) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(socketAddress, 1000);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Obtains misc config data from a remote OpenAM server:
     * <ul>
     *     <li>OpendDJ admin port</li>
     *     <li>config basedn</li>
     *     <li>replication ready flag</li>
     *     <li>OpenDJ replication port or OpenDJ suggested port</li>
     * </ul>
     *
     * @param serverUrl URL string representing the remote OpenAM server.
     * @param userId The admin user id on remote server, (only amadmin).
     * @param password The admin password.
     * @return A {@code Map} of config parameters.
     * @throws ConfigurationException for the following error code:
     * <ul>
     *     <li>400=Bad Request - user id/password param missing</li>
     *     <li>401=Unauthorized - invalid credentials</li>
     *     <li>405=Method Not Allowed - only POST is honored</li>
     *     <li>408=Request Timeout - requested timed out</li>
     *     <li>500=Internal Server Error</li>
     *     <li>701=File Not Found - incorrect deploy/server uri</li>
     *     <li>702=Connection Error - failed to connect</li>
     * </ul>
     */
    public static Map<String, String> getRemoteServerInfo(String serverUrl, String userId, String password)
            throws ConfigurationException {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(serverUrl + "/getServerInfo.jsp");
            writeToConnection(connection, "IDToken1=" + URLEncoder.encode(userId, "UTF-8")
                    + "&IDToken2=" + URLEncoder.encode(password, "UTF-8"));
            return BootstrapData.queryStringToMap(readFromConnection(connection));
        } catch (IllegalArgumentException e) {
            debug.warning("AMSetupUtils.getRemoteServerInfo()", e);
            throw newConfigurationException("702");
        } catch (IOException e) {
            debug.warning("AMSetupUtils.getRemoteServerInfo()", e);
            if (e instanceof FileNotFoundException) {
                throw newConfigurationException("701");
            } else if (e instanceof SSLHandshakeException || e instanceof MalformedURLException
                    || e instanceof UnknownHostException || e instanceof ConnectException) {
                throw newConfigurationException("702");
            } else {
                int status = 0;
                if (connection != null) {
                    try {
                        status = connection.getResponseCode();
                    } catch (Exception ignored) {
                    }
                }
                if (status == 400 || status == 401 || status == 405 || status == 408) {
                    throw newConfigurationException(String.valueOf(status));
                } else {
                    throw new ConfiguratorException(e.getMessage());
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (url.getProtocol().equals(HTTPS)) {
            HttpsURLConnection sslConnection = (HttpsURLConnection) connection;
            sslConnection.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
        return connection;
    }

    private static void writeToConnection(URLConnection connection, String data) throws IOException {
        connection.setDoOutput(true);
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(data);
            wr.flush();
        } finally {
            closeIfNotNull(wr);
        }
    }

    private static String readFromConnection(URLConnection connection) throws IOException {
        return readStream(connection.getInputStream());
    }

    private static ConfiguratorException newConfigurationException(String errorCode) {
        return new ConfiguratorException(errorCode, null, java.util.Locale.getDefault());
    }

    /**
     * Gets the file contents as an {@code InputStream}.
     *
     * @param servletContext  The {@code ServletContext} to use to find the file.
     * @param file The {@code File} to retrieve.
     * @return An {@code InputStream} of the files contents.
     */
    public static InputStream getResourceAsStream(ServletContext servletContext, String file) {
        if (servletContext == null) {
            if (file.startsWith("/")) {
                file = file.substring(1);
            }
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
        } else {
            return servletContext.getResourceAsStream(file);
        }
    }
}
