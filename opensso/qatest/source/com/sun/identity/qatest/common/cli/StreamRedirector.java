/* The contents of this file are subject to the terms
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
 * $Id: StreamRedirector.java,v 1.2 2007/06/29 13:49:04 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.qatest.common.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter; 

/**
 * This class redirects input stream to a designated StringBuffer.
 */ 
public class StreamRedirector extends Thread {
    private InputStream inputStream;
    private OutputStream outputStream;
    private StringBuffer buffer;
    
    /**
     * Empty constructor
     */
    public StreamRedirector() {
        buffer = new StringBuffer();
    }
    
    /**
     * Creates a new instance of </code>StreamRedirector</code>.
     *
     * @param is Input Stream.
     */ 
    public StreamRedirector(InputStream is) {
        super();
        inputStream = is;
    } 
    
    /**
     * Creates a new instance of </code>StreamRedirector</code>.
     *
     * @param is Input Stream.
     * @param os Output Stream.
     */ 
    public StreamRedirector(InputStream is, OutputStream os) {
        inputStream = is;
        outputStream = os;
    }
    
    /**
     * Returns the redirecting thread.
     */ 
    public void run() {
        InputStreamReader isr = null;
        BufferedReader buff = null;
        
        try {
            isr = new InputStreamReader(inputStream);
            buff = new BufferedReader(isr);

            String line = buff.readLine();
            while (line != null) {
                if (buffer != null) {
                    buffer.append(line + System.getProperty("line.separator"));
                }
                line = buff.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (isr != null) {
                    isr.close();
                }
                if (buff != null) {
                    buff.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } 
    
    /**
     * Sets the input stream which will be read
     */
    void setInputStream(InputStream is) { inputStream = is; }
    
    /**
     * Returns the buffer containing the contents of the stream.
     */
    public StringBuffer getBuffer() {
        return buffer;
    }   
}
