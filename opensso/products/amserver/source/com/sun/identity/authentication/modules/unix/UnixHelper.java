/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: UnixHelper.java,v 1.3 2008/10/23 22:41:06 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.unix;

import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.shared.debug.Debug;

public class UnixHelper {
    protected static final int DAEMON_TIMEOUT_mS = 7500;
    private final int MAXLOOP = 200; // maximum loop allowed for do...while

    private Socket sock = null;
    private BufferedReader reader = null;
    private PrintWriter writer = null;
    private static final String charSet = "ISO8859_1";
    public static Debug debug;

    public UnixHelper(int port, String bundleName)
        throws AuthenticationException
    {
        debug = Debug.getInstance("amUnixHelper");
        debug.message("unix helper...init");
        try {
            sock = new Socket("127.0.0.1", port);
            sock.setSoTimeout(DAEMON_TIMEOUT_mS);
            reader = new BufferedReader(new InputStreamReader(
                sock.getInputStream(), charSet));
            writer = new PrintWriter (new BufferedWriter (
                new OutputStreamWriter(sock.getOutputStream(), charSet)));
        } catch (UnknownHostException e) {
            throw new AuthenticationException(bundleName, 
                "UnixHelperLocalhost", null);
        } catch (IOException ex) {
            throw new AuthenticationException(bundleName,
                "UnixHelperIOEx", null);
        }
    }

    protected synchronized int do_write(String cmd) {
        writer.println(cmd);                // send the command
        writer.flush();                        // flush buffer
        return cmd.length();
    }

    public String do_read (int readsize, ResourceBundle bundle)
        throws IOException
    {
        int i;
        char buf[] = new char[254];
        String readstring;

        debug.message("in do_read...");
        try {
            i = reader.read(buf, 0, readsize);
        } catch (IOException ioex) {
            throw ioex;
        }
        readstring = new String (buf);
        try {
            if (!readstring.equals(
                new String(readstring.getBytes("ASCII"), "ASCII")))
            {
                throw new IOException(bundle.getString (
                    "UnixHelperInputNotASCII"));
            }
        } catch (UnsupportedEncodingException ueex) {
            debug.message("Unsupported coding ...");
            throw new IOException(bundle.getString (
                "UnixHelperInputEncodingException"));
        }
        debug.message("returng... readString... " + readstring);
        return readstring;
    }


    public int configHelper (String helper_port, String helper_timeout,
        String helper_threads,
        com.sun.identity.shared.debug.Debug debug,
        ResourceBundle bundle)
    {
        String instring;
        int i;

        /*
         *  should get this sequence:
         *  Enter Unix Helper Listen Port [7946]:  
         *  Enter Unix Helper Session Timeout [3]:  
         *  Enter Unix Helper Max Sessions [5]:  
         *  get_config_info: amunixd configured successfully
         */
        try {
            instring = do_read(254, bundle);
        } catch (IOException ex) {
            return -1;
        }

        if (instring.startsWith("Enter Unix Helper Listen Port")) {
            i = do_write (helper_port);
        } else {
            return -2;
        }

        try {
            instring = do_read(254, bundle);
        } catch (IOException ex) {
            return -3;
        }

        if (instring.startsWith("Enter Unix Helper Session Timeout")) {
            i = do_write (helper_timeout);
        } else {
            return -4;
        }

        try {
            instring = do_read(254, bundle);
        } catch (IOException ex) {
            return -5;
        }

        if (instring.startsWith("Enter Unix Helper Max Sessions")) {
            i = do_write (helper_threads);
        } else {
            return -6;
        }

        try {
            instring = do_read(254, bundle);
        } catch (IOException ex) {
            return -7;
        }

        if (instring.startsWith(
            "get_config_info: amunixd configured successfully"))
        {
        } else {
            return -8;
        }

        return 0;
    }

    /*
     * first screen of the Unix login
     * authenticate user
     * return result:
     *        0  : authenticate pass
     *        -1 : failed
     *         k  : goto corresponding next screen
     */
    public int authenticate (String userlogin, 
         String userpass, 
         String serviceModule,
         String clientIPAddr,
         ResourceBundle bundle)
    {
        int i, k;                
        String instring;
        final int MAXSCREEN = 1000;
        int maxloop = MAXLOOP;

        k = MAXSCREEN;
        if (debug.messageEnabled()) {
           debug.message("authenticate.....userlogin" + userlogin);
           debug.message("authenticate.....serviceModule" + serviceModule);
        }
        do {
            instring = "";
            try {
                debug.message("calling do_read");
                instring = do_read(254, bundle);
                debug.message("after do_read");
            } catch (IOException ex) {
                return -1;
            }

            if ( instring.length() == 0 ) {
                return -1;
            }
            debug.message("Instring is.. : " + instring);
            if (instring.startsWith("Enter Unix login:")) {
                i = do_write (userlogin);
                k = MAXSCREEN;
            } else if (instring.startsWith("Enter password:")) {
                i = do_write (userpass);
                k = MAXSCREEN;
            } else if (instring.startsWith("Enter Service Name :")) {
                debug.message("writing service name");
                i=do_write(serviceModule);
                debug.message("after writing service name" + i);
                k=MAXSCREEN;
            } else if (instring.startsWith("Enter Client IP Address:")) {
                if (clientIPAddr != null) {
                    i = do_write (clientIPAddr);
                } else {
                    i = do_write("0.0.0.0");
                }
                k = MAXSCREEN;
            } else if (instring.startsWith("Authentication passed")){
                k = 0;
            } else if (instring.startsWith("Access denied")) {
                k = -1;
            } else if (instring.startsWith("unknown return code ")) {
                k = -1;
            } else if (instring.startsWith("Processing timed-")) {
                k = -1;
            } else if (instring.startsWith("Processing erro")) {
                k = -1;
            } else if (instring.startsWith("Authentication Failed")) {
                k = -1;
                if (instring.indexOf("Password Expired") != -1) {
                    debug.message ("password expired for " + userlogin);
                    k = 2;
                }
            } else {
                k = MAXSCREEN;
            }

            maxloop--;
            if (maxloop == 0) {
                k = -1;
            }
        } while (k == MAXSCREEN);

        debug.message("returning... k from authenticate" + k);
        return k;
    }

    protected synchronized void destroy(ResourceBundle bundle) {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (sock != null) {
                sock.close();
                sock = null;
            }
        } catch (IOException e) {
            System.err.println(bundle.getString("UnixDestroyIOEx"));
            System.exit(1);
        } catch (Exception ee) {
            System.err.println(bundle.getString("UnixDestroyEx") +
                ee.getMessage());
        }
    }
}

