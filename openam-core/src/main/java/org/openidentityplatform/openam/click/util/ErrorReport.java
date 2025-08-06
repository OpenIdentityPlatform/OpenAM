/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.openidentityplatform.openam.click.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TreeMap;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.openidentityplatform.openam.click.Page;
import org.apache.click.service.TemplateException;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.commons.lang.StringUtils;

/**
 * Provides an HTML &lt;div&gt; error report for the display of page error
 * information. This class is used by ErrorPage and ClickServlet for the
 * display of error information.
 */
public class ErrorReport {

    /** The Java language keywords. Used to render Java source code. */
    private static final String[] JAVA_KEYWORDS = { "package", "import",
            "class", "public", "protected", "private", "extends", "implements",
            "return", "if", "while", "for", "do", "else", "try", "new", "void",
            "catch", "throws", "throw", "static", "final", "break", "continue",
            "super", "finally", "true", "false", "true;", "false;", "null",
            "boolean", "int", "char", "long", "float", "double", "short" };

    /** The column number of the error, or -1 if not defined. */
    protected int columnNumber;

    /** The cause of the error. */
    protected final Throwable error;


    /** The line number of the error, or -1 if not defined. */
    protected int lineNumber;

    /** The error is Velocity parsing exception. */
    protected final boolean isParseError;

    /** The application is in "production" mode flag. */
    protected final boolean isProductionMode;

    /** The page class which caused the error. */
    protected final Class<? extends Page> pageClass;

    /** The servlet request. */
    protected final HttpServletRequest request;

    /** The servlet context. */
    protected final ServletContext servletContext;

    /** The name of the error source. */
    protected final String sourceName;

    /** The error source LineNumberReader. */
    protected LineNumberReader sourceReader;

    // Constructor ------------------------------------------------------------

    /**
     * Create a ErrorReport instance from the given error and page.
     *
     * @param error the cause of the error
     * @param pageClass the Page class which caused the error
     * @param isProductionMode the application is in "production" mode
     * @param request the page request
     * @param servletContext the servlet context
     */
    public ErrorReport(Throwable error, Class<? extends Page> pageClass,
                       boolean isProductionMode, HttpServletRequest request,
                       ServletContext servletContext) {

        this.error = error;
        this.pageClass = pageClass;
        this.isProductionMode = isProductionMode;
        this.request = request;
        this.servletContext = servletContext;

        if (error instanceof TemplateException
                && ((TemplateException) error).isParseError()) {

            TemplateException te = (TemplateException) error;

            if (te.getTemplateName().charAt(0) == '/') {
                sourceName = te.getTemplateName();
            } else {
                sourceName =  '/' + te.getTemplateName();
            }
            lineNumber = te.getLineNumber();
            columnNumber = te.getColumnNumber();

            InputStream is =
                    servletContext.getResourceAsStream(sourceName);

            sourceReader = new LineNumberReader(new InputStreamReader(is));
            isParseError = true;

        } else {
            isParseError = false;
            sourceName = null;
            columnNumber = -1;

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            getCause().printStackTrace(pw);

            StringTokenizer tokenizer =
                    new StringTokenizer(sw.toString(), "\n");

            try {
                tokenizer.nextToken();
                String line = tokenizer.nextToken();
                if (getCause() instanceof IllegalArgumentException) {
                    line = tokenizer.nextToken();

                    if (line.indexOf("org.apache.commons.lang.Validate") != -1) {
                        line = tokenizer.nextToken();
                    }
                }

                int nameStart = line.indexOf("at ");
                int nameEnd = line.indexOf("(");
                nameEnd = line.lastIndexOf(".", nameEnd);
                if (line.indexOf("$") != -1) {
                    nameEnd = line.indexOf("$");
                }

                if (nameStart != -1 && nameEnd != -1) {
                    String classname = line.substring(nameStart + 3, nameEnd);

                    int lineStart = line.indexOf(":");
                    if (lineStart != -1) {
                        int lineEnd = line.indexOf(")");
                        String lineNumber = line.substring(lineStart + 1, lineEnd);

                        this.lineNumber = Integer.parseInt(lineNumber);

                        String filename =
                                "/" + classname.replace('.', '/') + ".java";

                        sourceReader = getJavaSourceReader(filename);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Return a error report HTML &lt;div&gt; element for the given error and
     * page. The HTML &lt;div&gt; element 'id' and 'class' attribute values are
     * 'errorReport'.
     *
     * @return a error HTML display string
     */
    @SuppressWarnings("unchecked")
    public String toString() {

        if (isProductionMode()) {
            Locale locale = request.getLocale();
            ResourceBundle bundle = org.apache.click.util.ClickUtils.getBundle("click-control", locale);
            return bundle.getString("production-error-message");
        }

        HtmlStringBuffer buffer = new HtmlStringBuffer(10 * 1024);

        Throwable cause = getCause();

        buffer.append("<div id='errorReport' class='errorReport'>\n");

        // Exception table
        buffer.append("<table border='1' cellspacing='1' cellpadding='4' width='100%' style='background-color: white;'>");
        if (isParseError()) {
            buffer.append("<tr><td colspan='2' style='color:white; background-color: navy; font-weight: bold'>Page Parsing Error</td></tr>");
            buffer.append("<tr><td width='12%'><b>Source</b></td><td>");
            buffer.append(getSourceName());
            buffer.append("</td></tr>");
        } else {
            buffer.append("<tr><td colspan='2' style='color:white; background-color: navy; font-weight: bold'>Exception</td></tr>");
            buffer.append("<tr><td width='12%'><b>Class</b></td><td>");
            buffer.append(cause.getClass().getName());
            buffer.append("</td></tr>");
        }
        buffer.append("<tr><td valign='top' width='12%'><b>Message</b></td><td>");
        buffer.append(getMessage());
        buffer.append("</td></tr>");
        if (getSourceReader() != null) {
            buffer.append("<tr><td valign='top' colspan='2'>\n");
            buffer.append(getRenderedSource());
            buffer.append("</td></tr>");
        }
        if (!isParseError()) {
            buffer.append("<tr><td valign='top' colspan='2'>\n");
            buffer.append(getStackTrace());
            buffer.append("</td></tr>");
        }
        buffer.append("</table>");
        buffer.append("<br/>");

        // Page table
        buffer.append("<table border='1' cellspacing='1' cellpadding='4' width='100%' style='background-color: white;'>");
        buffer.append("<tr><td colspan='2' style='color:white; background-color: navy; font-weight: bold'>Page</td></tr>");
        buffer.append("<tr><td width='12%'><b>Classname</b></td><td>");
        if (pageClass != null) {
            buffer.append(pageClass.getName());
        }
        buffer.append("</td></tr>");
        buffer.append("<tr><td width='12%'><b>Path</b></td><td>");
        buffer.append(ClickUtils.getResourcePath(request));
        buffer.append("</td></tr>");
        buffer.append("</table>");
        buffer.append("<br/>");

        // Request table
        buffer.append("<table border='1' cellspacing='1' cellpadding='4' width='100%' style='background-color: white;'>");
        buffer.append("<tr><td colspan='2' style='color:white; background-color: navy; font-weight: bold'>Request</td></tr>");

        Map<String, Object> requestAttributes = new TreeMap<String, Object>();
        Enumeration attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement().toString();
            requestAttributes.put(name, request.getAttribute(name));
        }
        buffer.append("<tr><td width='12%' valign='top'><b>Attributes</b></td><td>");
        writeMap(requestAttributes, buffer);
        buffer.append("</td></tr>");

        buffer.append("<tr><td width='12%'><b>Auth Type</b></td><td>");
        buffer.append(request.getAuthType());
        buffer.append("</td></tr>");

        buffer.append("<tr><td width='12%'><b>Context Path</b></td><td>");
        buffer.append("<a href='");
        buffer.append(request.getContextPath());
        buffer.append("'>");
        buffer.append(request.getContextPath());
        buffer.append("</a>");
        buffer.append("</td></tr>");

        Map<String, Object> requestHeaders = new TreeMap<String, Object>();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement().toString();
            requestHeaders.put(name, request.getHeader(name));
        }
        buffer.append("<tr><td width='12%' valign='top'><b>Headers</b></td><td>");
        writeMap(requestHeaders, buffer);
        buffer.append("</td></tr>");

        buffer.append("<tr><td width='12%'><b>Query</b></td><td>");
        buffer.append(request.getQueryString());
        buffer.append("</td></tr>");

        buffer.append("<tr><td width='12%'><b>Method</b></td><td>");
        buffer.append(request.getMethod());
        buffer.append("</td></tr>");

        Map<String, Object> requestParams = new TreeMap<String, Object>();
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement().toString();
            requestParams.put(name, request.getParameter(name));
        }
        buffer.append("<tr><td width='12%' valign='top'><b>Parameters</b></td><td>");
        writeMap(requestParams, buffer);
        buffer.append("</td></tr>");

        buffer.append("<tr><td width='12%'><b>Remote User</b></td><td>");
        buffer.append(request.getRemoteUser());
        buffer.append("</td></tr>");

        buffer.append("<tr><td width='12%' valign='top'><b>URI</b></td><td>");
        buffer.append("<a href='");
        String requestURI = ClickUtils.getRequestURI(request);
        buffer.append(requestURI);
        buffer.append("'>");
        buffer.append(requestURI);
        buffer.append("</a>");
        buffer.append("</td></tr>");

        buffer.append("<tr><td><b width='12%'>URL</b></td><td>");
        buffer.append("<a href='");
        buffer.append(request.getRequestURL());
        buffer.append("'>");
        buffer.append(request.getRequestURL());
        buffer.append("</a>");
        buffer.append("</td></tr>");

        Map<String, Object> sessionAttributes = new TreeMap<String, Object>();
        if (request.getSession(false) != null) {
            HttpSession session = request.getSession();
            attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement().toString();
                sessionAttributes.put(name, session.getAttribute(name));
            }
        }
        buffer.append("<tr><td width='12%' valign='top'><b>Session</b></td><td>");
        writeMap(sessionAttributes, buffer);
        buffer.append("</td></tr>");
        buffer.append("</table>\n");

        buffer.append("</div>\n");

        return buffer.toString();
    }

    // Protected Methods ------------------------------------------------------

    /**
     * Return the cause of the error.
     *
     * @return the cause of the error
     */
    protected Throwable getCause() {
        Throwable cause = null;
        if (error instanceof ServletException) {
            cause = ((ServletException) error).getRootCause();
            if (cause == null) {
                cause = error.getCause();
            }
        } else {
            cause = error.getCause();
        }
        if (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause == null) {
            cause = error;
        }
        return cause;
    }

    /**
     * Return the error source column number, or -1 if not determined.
     *
     * @return the error source column number, or -1 if not determined
     */
    protected int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Return true if the error was a Velocity parsing exception.
     *
     * @return true if the error was a Velocity parsing exception
     */
    protected boolean isParseError() {
        return isParseError;
    }

    /**
     * Return true if the application is in "production" mode.
     *
     * @return true if the application is in "production" mode
     */
    protected boolean isProductionMode() {
        return isProductionMode;
    }

    /**
     * Return the error source line number, or -1 if not determined.
     *
     * @return the error source line number, or -1 if not determined
     */
    protected int getLineNumber() {
        return lineNumber;
    }

    /**
     * Return the error message.
     *
     * @return the error message
     */
    protected String getMessage() {
        if (isParseError()) {
            String message = error.getMessage();

            String parseMsg = message;
            int startIndex = message.indexOf('\n');
            int endIndex = message.lastIndexOf("...");

            if (startIndex != -1 && endIndex > startIndex) {
                parseMsg = message.substring(startIndex + 1, endIndex);
            }

            parseMsg = org.apache.click.util.ClickUtils.escapeHtml(parseMsg);

            parseMsg = StringUtils.replace(parseMsg, "...", ", &#160;");

            return parseMsg;

        } else {
            Throwable cause = getCause();

            String value =
                    (cause.getMessage() != null) ? cause.getMessage() : "null";

            return org.apache.click.util.ClickUtils.escapeHtml(value);
        }
    }

    /**
     * Return the error source name.
     *
     * @return the error source name
     */
    protected String getSourceName() {
        return sourceName;
    }

    /**
     * Return a LineNumberReader for the error source file, or null if not
     * defined.
     *
     * @return LineNumberReader for the error source file, or null if not
     *      defined
     */
    protected LineNumberReader getSourceReader() {
        return sourceReader;
    }

    /**
     * Return Java Source LineNumberReader for the given filename, or null if
     * not found.
     *
     * @param filename the name of the Java source file, e.g. /examples/Page.java
     * @return LineNumberReader for the given source filename, or null if
     *      not found
     * @throws FileNotFoundException if file could not be found
     */
    protected LineNumberReader getJavaSourceReader(String filename)
            throws FileNotFoundException {

        if (pageClass == null) {
            return null;
        }

        // Look for source file on classpath
        InputStream is = pageClass.getResourceAsStream(filename);
        if (is != null) {
            return new LineNumberReader(new InputStreamReader(is));
        }

        // Else search for source file under WEB-INF
        String rootPath = servletContext.getRealPath("/");

        String webInfPath = rootPath + File.separator + "WEB-INF";

        File sourceFile = null;

        File webInfDir = new File(webInfPath);
        if (webInfDir.isDirectory() && webInfDir.canRead()) {
            File[] dirList = webInfDir.listFiles();
            if (dirList != null) {
                for (File file : dirList) {
                    if (file.isDirectory() && file.canRead()) {
                        String sourcePath = file.toString() + filename;
                        sourceFile = new File(sourcePath);
                        if (sourceFile.isFile() && sourceFile.canRead()) {

                            FileInputStream fis = new FileInputStream(sourceFile);
                            return new LineNumberReader(new InputStreamReader(fis));
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Return a HTML rendered section of the source error with the error
     * line highlighted.
     *
     * @return a HTML rendered section of the parsing error page template
     */
    protected String getRenderedSource() {

        HtmlStringBuffer buffer = new HtmlStringBuffer(5 * 1024);

        buffer.append("<pre style='font-family: Courier New, courier;'>");

        if (sourceReader == null) {
            buffer.append("Source is not available.</span>");
            return buffer.toString();
        }

        final String errorLineStyle =
                "style='background-color:yellow;'";

        final String errorCharSpan =
                "<span style='color:red;text-decoration:underline;font-weight:bold;'>";

        try {
            String line = sourceReader.readLine();

            while (line != null) {
                if (skipLine()) {
                    line = sourceReader.readLine();
                    continue;
                }

                boolean isErrorLine =
                        sourceReader.getLineNumber() == lineNumber;

                // Start div tag
                buffer.append("<div ");
                if (isErrorLine) {
                    buffer.append(errorLineStyle);
                }
                buffer.append(">");

                // Write out line number
                String lineStr = "" + sourceReader.getLineNumber();
                int numberSpace = 3 - lineStr.length();
                for (int i = 0; i < numberSpace; i++) {
                    buffer.append(" ");
                }
                if (isErrorLine) {
                    buffer.append("<b>");
                }
                buffer.append(sourceReader.getLineNumber());
                if (isErrorLine) {
                    buffer.append("</b>");
                }
                buffer.append(":  ");

                // Write out line content
                if (isErrorLine) {
                    if (isParseError()) {
                        HtmlStringBuffer htmlLine =
                                new HtmlStringBuffer(line.length() * 2);

                        for (int i = 0; i < line.length(); i++) {
                            if (i == getColumnNumber() - 1) {
                                htmlLine.append(errorCharSpan);
                                htmlLine.append(line.charAt(i));
                                htmlLine.append("</span>");
                            } else {
                                htmlLine.append(line.charAt(i));
                            }
                        }
                        buffer.append(htmlLine.toString());

                    } else {
                        buffer.append(getRenderJavaLine(line));
                    }

                } else {
                    if (isParseError()) {
                        buffer.append(org.apache.click.util.ClickUtils.escapeHtml(line));
                    } else {
                        buffer.append(getRenderJavaLine(line));
                    }
                }

                // Close div tag
                buffer.append("</div>");

                line = sourceReader.readLine();
            }

        } catch (IOException ioe) {
            buffer.append("Could not load page source: ");
            buffer.append(org.apache.click.util.ClickUtils.escapeHtml(ioe.toString()));
        } finally {
            try {
                sourceReader.close();
            } catch (IOException ioe) {
                // do nothing
            }
        }

        buffer.append("</pre>");

        return buffer.toString();
    }

    /**
     * Return a HTML encode stack trace string from the given error.
     *
     * @return a HTML encode stack trace string.
     */
    protected String getStackTrace() {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        getCause().printStackTrace(pw);

        HtmlStringBuffer buffer
                = new HtmlStringBuffer(sw.toString().length() + 80);
        buffer.append("<pre><tt style='font-size:10pt;'>");
        buffer.append(org.apache.click.util.ClickUtils.escapeHtml(sw.toString().trim()));
        buffer.append("</tt></pre>");

        return buffer.toString();
    }

    /**
     * Write out the map name value pairs as name=value lines to the string
     * buffer.
     *
     * @param map the Map of name value pairs
     * @param buffer the string buffer to write out the values to
     */
    protected void writeMap(Map<String, Object> map, HtmlStringBuffer buffer) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            buffer.append(key);
            buffer.append("=");
            if (value != null) {
                buffer.append(org.apache.click.util.ClickUtils.escapeHtml(value.toString()));
            } else {
                buffer.append("null");
            }
            buffer.append("<br/>");
        }
        if (map.isEmpty()) {
            buffer.append("&#160;");
        }
    }

    /**
     * Return a HTML rendered Java source line with keywords highlighted
     * using the given line.
     *
     * @param line the Java source line to render
     * @return HTML rendered Java source line
     */
    protected String getRenderJavaLine(String line) {
        line = ClickUtils.escapeHtml(line);

        for (String keyword : JAVA_KEYWORDS) {
            line = renderJavaKeywords(line, keyword);
        }

        return line;
    }

    /**
     * Render the HTML rendered Java source line with the given keyword
     * highlighted.
     *
     * @param line the given Java source line to render
     * @param keyword the Java keyword to highlight
     * @return the HTML rendered Java source line with the given keyword
     *      highlighted
     */
    protected String renderJavaKeywords(String line, String keyword) {
        String markupToken =
                "<span style='color:#7f0055;font-weight:bold;'>" + keyword + "</span>";

        line = StringUtils.replace(line,
                " " + keyword + " ",
                " " + markupToken + " ");

        if (line.startsWith(keyword)) {
            line = markupToken + line.substring(keyword.length());
        }

        if (line.endsWith(keyword)) {
            line = line.substring(0, line.length() - keyword.length())
                    + markupToken;
        }

        return line;
    }

    /**
     * Return true if the current line read from the source line reader, should
     * be skipped, or false if the current line should be rendered.
     *
     * @return true if the current line from the source reader should be skipped
     */
    protected boolean skipLine() {
        final int currentLine = getSourceReader().getLineNumber();
        final int errorLine = getLineNumber();

        if (Math.abs(currentLine - errorLine) <= 10) {
            return false;
        } else {
            return true;
        }
    }
}
