/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: OpenIDPageParser.java,v 1.2 2009/01/27 22:58:45 rsoika Exp $
 */
package com.sun.security.sam.openid;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.message.AuthException;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is for parsing an OpenID response page. The Parser is able to
 * parse a XML content and also to parse HTML content.
 * 
 * @author rsoika
 * @author monzillo
 */
public class OpenIDPageParser {

    protected static final Logger defaultLogger =
            Logger.getLogger(OpenIDPageParser.class.getName());

    // hidden constructor
    private OpenIDPageParser() {
    }

    /**
     * This method is responsible for parsing the content of an openID Page.
     * 
     * @param connection - the connection on which the response to the GET of 
     * the OpenID page may be read.
     * @param expectedContentType value of Accept header of request.
     * @param logger - may be used to define the logger used by this class. If a
     * null value is passed for this param, default Logger is used. 
     * @param debug when true, parser will log debugging info
     * @return a property map containing the values of the OpenID properties 
     * read from the page.
     * @throws IOException
     */
    public static Properties parse(HttpURLConnection connection,
            Logger logger, boolean debug) throws AuthException {

        if (logger == null) {
            logger = defaultLogger;
        }
        ElementQuery[] queries = new ElementQuery[]{
            new ElementQuery("link",
            new KeyValuePair[]{
                new KeyValuePair(HTML.Attribute.REL, "openid.server"),
                new KeyValuePair(HTML.Attribute.HREF, null)
            }),
            new ElementQuery("link",
            new KeyValuePair[]{
                new KeyValuePair(HTML.Attribute.REL, "openid.delegate"),
                new KeyValuePair(HTML.Attribute.HREF, null)
            })
        };
        String contentType = connection.getContentType().toLowerCase();
        if (debug) {
            String msg = "openid.id_page_content_type_" + contentType;
            logger.info(msg);
        }

        InputStream stream;
        try {
            stream = connection.getInputStream();
        } catch (IOException ex) {
            String msg = "openid.idpage_connection_failure";
            logger.log(Level.WARNING, msg, ex);
            AuthException ae = new AuthException(msg);
            ae.initCause(ex);
            throw ae;
        }

        Properties rvalue;
        // check if content can be paresed with xml parser
        if (contentType.contains("html")) {
            rvalue = parseHTMLPage(stream, queries, logger, debug);
        } else if (contentType.toLowerCase().contains("xml")) {
            rvalue = parseXMLPage(stream, queries, logger, debug);
        } else {
            String msg = "openid.unsupported_content_type" + contentType;
            logger.log(Level.WARNING, msg);
            // try xml parser
            rvalue = parseXMLPage(stream, queries, logger, debug);
        }
        if (rvalue == null || rvalue.getProperty("openid.server") == null) {
            String msg = "openid.no_openid_server";
            logger.log(Level.WARNING, msg);
            AuthException ae = new AuthException(msg);
            throw ae;
        }
        return rvalue;
    }

    private static Properties addProperty(
            Properties properties, String key, String value) {
        if (value != null) {
            if (properties == null) {
                properties = new Properties();
            }
            properties.put(key, value);
        }
        return properties;
    }

	/**
	 * 
	 * @param stream
	 * @param queries
	 * @param logger
	 * @param debug
	 * @return
	 * @throws javax.security.auth.message.AuthException
	 */
	private static Properties parseHTMLPage(InputStream stream,
			ElementQuery[] queries, Logger logger, boolean debug)
			throws AuthException {

		if (debug) {
			String msg = "openid.parsing_html_id_page";
			logger.info(msg);
		}

		HTMLParserCallback callback = new HTMLParserCallback(queries, logger,
				debug);
		try {
			// Create an InputStreamReader to read the HTML document.
			// The reader uses the default character set for decoding bytes into characters.
			
			InputStreamReader reader = new InputStreamReader(stream);

			try {
				// use the reader to create a parser and parse the document

				new ParserDelegator().parse(reader, callback, false);
				
			} catch (ChangedCharSetException e) {
				// parser throws a ChangedCharSetException if it encounters a <meta>
				// tag with a charset attribute that specifies a character set other
				// than the default.

				// Extract the new character set name from the exception.

				String csspec = e.getCharSetSpec();
				Pattern p = Pattern.compile("charset=\"?(.+)\"?\\s*;?",
						Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(csspec);
				String charset = m.find() ? m.group(1) : "ISO-8859-1";

				// Create a new reader that uses the new charset

				reader = new InputStreamReader(stream, charset);

				// use the reader to create a parser and reparse the document.
				// pass true for the ignoreCharSet parameter, to cause the
				// parser to ignore the <meta>tag with its charset attribute.

				new ParserDelegator().parse(reader, callback, true);
				
			} finally {
				reader.close();
			}
		} catch (Throwable t) {
			String msg = "openid.failed_parsing_id_page";
			logger.log(Level.WARNING, msg, t);
			AuthException ae = new AuthException(msg);
			ae.initCause(t);
			throw ae;
		}

		Properties rvalue = null;
		rvalue = addProperty(rvalue, "openid.server", queries[0]
				.getAttributeValue("href"));
		rvalue = addProperty(rvalue, "openid.delegate", queries[1]
				.getAttributeValue("href"));
		return rvalue;
	}

    /**
     * 
     * @param doc
     * @param queries
     * @param logger
     * @param debug
     * @return
     * @throws javax.security.auth.message.AuthException
     */
    private static Properties parseXMLPage(InputStream doc,
            ElementQuery[] queries, Logger logger, boolean debug) throws AuthException {

        if (debug) {
            String msg = "openid.parsing_xml_id_page";
            logger.info(msg);
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {

            SAXParser parser = factory.newSAXParser();
            parser.parse(doc, new XMLParserHandler(queries, logger, debug));

        } catch (Throwable t) {
            String msg = "openid.failed_parsing_id_page";
            logger.log(Level.WARNING, msg, t);
            AuthException ae = new AuthException(msg);
            ae.initCause(t);
            throw ae;

        }
        Properties rvalue = null;
        rvalue = addProperty(rvalue, "openid.server", queries[0].getAttributeValue("href"));
        rvalue = addProperty(rvalue, "openid.delegate", queries[1].getAttributeValue("href"));
        return rvalue;
    }

    private static class HTMLParserCallback extends HTMLEditorKit.ParserCallback {

        ElementQuery[] queries;
        Logger logger;
        boolean debug;

        private HTMLParserCallback(ElementQuery[] queries, Logger logger, boolean debug) {
            this.queries = queries;
            this.logger = logger;
            this.debug = debug;
        }

        @Override
        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            for (ElementQuery query : queries) {

                // check if this element matches quesry
                if (t.toString().equals(query.eName)) {
                    if (debug) {
                        String msg = "HTMLParserCallback.found_simple_element" + query.eName;
                        logger.info(msg);
                    }
                    // check that elememt contains suitable attribute
                    // to match query
                    for (int i = 0; i < query.keyPairs.length; i++) {
                        Object value = a.getAttribute(query.keyPairs[i].key);
                        if (value != null) {
                            // if query is value specific,
                            // check that element value matches
                            String keyValue = query.keyPairs[i].value;
                            if (keyValue != null && !keyValue.equals(value.toString())) {
                                return;
                            }
                        } else {
                            return;
                        }
                    }

                    // assign attribute values to query if element fully satisfies query.
                    if (debug) {
                        logger.info("HTMLParserCallback.element_satisfies_query");
                    }
                    Enumeration e = a.getAttributeNames();
                    while (e.hasMoreElements()) {
                        HTML.Attribute name = (HTML.Attribute) e.nextElement();
                        String value = a.getAttribute(name).toString();
                        if (query.attributes == null) {
                            query.attributes = new HashMap();
                        }
                        query.attributes.put(name.toString(), value);
                        if (debug) {
                            logger.info("XMLParserCallback.attribute: " + name +
                                    " Value: " + value);
                        }

                    }
                }
            }
        }
    }

    private static class XMLParserHandler extends DefaultHandler {

        ElementQuery[] queries;
        Logger logger;
        boolean debug;

        XMLParserHandler(ElementQuery[] queries, Logger logger, boolean debug) {
            this.queries = queries;
            this.logger = logger;
            this.debug = debug;
        }

        @Override
        public void startElement(String nameSpaceURI, String localName,
                String qName, Attributes atts) throws SAXException {

            for (ElementQuery query : queries) {

                // check if this element matches quesry
                if (qName.equals(query.eName)) {

                    if (debug) {
                        String msg = "XMLParserHandler.found_element" + query.eName;
                        logger.info(msg);
                    }

                    // check that elememt contains suitable attribute
                    // to match query
                    for (int i = 0; i < query.keyPairs.length; i++) {

                        String value = atts.getValue(query.keyPairs[i].key.toString());

                        if (value != null) {
                            // if query is value specific,
                            // check that element value matches
                            String keyValue = query.keyPairs[i].value;

                            if (keyValue != null && !keyValue.equals(value)) {
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                    // assign attribute values to query if element fully satisfies query.
                    query.attributes = new HashMap();
                    for (int i = 0; i < atts.getLength(); i++) {

                        String attribute = atts.getQName(i);
                        String value = atts.getValue(i);
                        query.attributes.put(attribute, value);
                        if (debug) {
                            if (i == 0) {
                                logger.info("XMLParserHandler.element_satisfies_query");
                            }
                            logger.info("XMLParserHandler.attribute: " + attribute +
                                    " Value: " + value);
                        }
                    }
                }
            }
        }
    }

    static class ElementQuery {

        String eName;
        KeyValuePair[] keyPairs;
        HashMap attributes;

        ElementQuery(String eName, KeyValuePair[] keyPairs) {
            this.eName = eName;
            this.keyPairs = keyPairs;
            this.attributes = null;
        }

        String getAttributeValue(String key) {
            String rvalue = null;
            if (attributes != null) {
                rvalue = (String) attributes.get(key);
            }
            return rvalue;
        }
    }

    static class KeyValuePair {

        HTML.Attribute key;
        String value;

        KeyValuePair(HTML.Attribute key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}

