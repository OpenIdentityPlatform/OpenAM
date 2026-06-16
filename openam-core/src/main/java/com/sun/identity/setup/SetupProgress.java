/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SetupProgress.java,v 1.10 2008/08/31 06:56:18 hengming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems, LLC
 */

package com.sun.identity.setup;

import static com.sun.identity.shared.debug.DebugConstants.DEBUG_DATE_FORMAT;
import static org.forgerock.openam.utils.Time.newDate;

import org.forgerock.openam.utils.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.sun.identity.config.SetupWriter;
import com.sun.identity.shared.DateUtils;

public class SetupProgress {
    static private Writer writer = null;
    /**
     * Set once {@link #closeOutputStream()} has run, i.e. configuration has finished and the
     * progress stream has been closed. Guards against a lost-completion race: on a fast install the
     * configuration request can finish (and close the stream) <em>before</em> the wizard's progress
     * iframe attaches its {@link SetupWriter} via {@link #setWriter(Writer)} and blocks in
     * {@link SetupWriter#realFlush()}. Without this flag that {@code realFlush()} would wait forever
     * for a {@code close()} that already happened, the iframe would never finish loading and the
     * wizard's "#confComplete" link would never appear (observed as a ~20 minute setup hang on CI).
     * Reset at the start of each configuration run via {@link #resetCompletion()}.
     */
    static private volatile boolean configurationComplete = false;
    static private String bundleName = "amConfigurator";

    static private OutputStream out = null;
    static private String encoding = System.getProperty("file.encoding");
    static private ResourceBundle bundle = ResourceBundle.getBundle(
        bundleName, Locale.getDefault());
    static private boolean isTextMode = false;

    /**
     * Returns writer associated with reporting progress to enduser.
     *
     * @return writer associated with reporting progress to enduser.
     */
    public static Writer getWriter() {
        return writer;
    }

    /**
     * Sets locale
     *
     * @param locale Locale.
     */
    static void setLocale(Locale locale) {
        bundle = ResourceBundle.getBundle(bundleName, locale);
    }

    /**
     * Returns OutputStream associated with reporting progress to enduser.
     *
     * @return OutputStream associated with reporting progress to enduser.
     */
    public static OutputStream getOutputStream() {
        return out;
    }

    /**
      * Sets setup writer.
      * @param w Setup writer to be used.
      */
    public static void setWriter(Writer w) {
        writer = w;
        if (writer == null) {
            out = null;
            return;
        }
        // If configuration already finished before this progress stream attached, close it at once
        // so SetupWriter.realFlush() returns immediately instead of blocking forever on a close()
        // that has already been issued (see configurationComplete).
        if (configurationComplete && (writer instanceof SetupWriter)) {
            try {
                writer.close();
            } catch (IOException e) {
                // best effort - closing only flips a flag and notifies the waiting realFlush()
            }
        }
        if (isTextMode) {
            out = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    String text = String.valueOf((char) b);
                    InstallLog.getInstance().write(text);
                    writer.write(URLEncoder.encode(text, encoding));
                    writer.flush();
                }

                @Override
                public void flush() throws IOException {
                    writer.flush();
                }
            };
        } else {
            out = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    String text = String.valueOf((char) b);
                    InstallLog.getInstance().write(text);
                    writer.write("<script>addProgressText(\""
                            + text.replace("\n", "\\\n").replace("\"", "\\\"")
                            + "<br/>\");</script>");
                    writer.flush();
                }

                @Override
                public void write(byte[] b) throws IOException {
                    String text = new String(b, encoding);
                    InstallLog.getInstance().write(text);
                    writer.write("<script>addProgressText(\"");
                    writer.write(text.replace("\n", "\\\n").replace("\"", "\\\""));
                    writer.write("<br/>\");</script>");
                    writer.flush();
                }
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    String text = new String(b, off, len, encoding);
                    InstallLog.getInstance().write(text);
                    writer.write("<script>addProgressText(\"");
                    writer.write(text.replace("\n", "\\\n").replace("\"", "\\\""));
                    writer.write("<br/>\");</script>");
                    writer.flush();
                }
                @Override
                public void flush() throws IOException {
                    writer.flush();
                }
            };
        }
    }

    /**
     * Sets setup OututStream.
     * @param ostr OutputStream  to be used.
     */
    public static void setOutputStream(OutputStream  ostr) {
        out = ostr;
    }

    public static void closeOutputStream() {
        // Mark completion BEFORE closing so a progress stream that attaches concurrently (or just
        // after) is closed immediately by setWriter() rather than blocking in realFlush() forever.
        configurationComplete = true;
        IOUtils.closeIfNotNull(writer);
    }

    /**
     * Reset the completion flag at the start of a configuration run so the progress stream for that
     * run streams normally. See {@link #configurationComplete}.
     */
    public static void resetCompletion() {
        configurationComplete = false;
    }

    /**
     * Sets text mode.
     * @param textMode true if output should be in text format or false in 
           html format.
     */
    public static void setTextMode(boolean textMode) {
        isTextMode = textMode;
    }

    /**
      * Reports beginning of an operation.
      *
      * @param str i18n key to be printed
      * @param param Object to be printed.
      */
    public static void reportStart(String str, Object[] param) {
        writeProgressText(str, param);
    }

    /**
     * Reports end of an operation.
     * @param str i18n key to be printed
     * @param param Object to be printed.
     */
    public static void reportEnd(String str, Object[] param) {
        writeProgressText(str, param);
    }

    private static void writeProgressText(
        String str, Object[] param) {
        String istr = null;
        
        try {
            istr = bundle.getString(str);
            if ((param != null) && (param.length > 0)) {
                istr = MessageFormat.format(istr, param);
            }
        } catch (MissingResourceException e) {
            istr = str;
        }

        reportDebug(istr);
    }

    private static void reportDebug(String istr) {

        synchronized (DEBUG_DATE_FORMAT) {
            istr = DEBUG_DATE_FORMAT.format(newDate()) + ": " + istr;
        }

        try {
                InstallLog.getInstance().write(istr + "\n");

            if (writer != null) {
                if (isTextMode) {
                    istr += "\n";
                    writer.write(istr);
                } else {
                    istr += "<br />";
                    writer.write(
                        "<script>addProgressText('" + istr + "');</script>");
                }
                writer.flush();
            }
        } catch (IOException ex) {
            //ignore
        }
    }
}
