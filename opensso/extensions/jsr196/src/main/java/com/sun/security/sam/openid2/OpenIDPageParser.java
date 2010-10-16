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
 * $Id: OpenIDPageParser.java,v 1.3 2009/04/05 07:57:54 rsoika Exp $
 */
package com.sun.security.sam.openid2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
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
 * parse a XRDS Document, and also to parse HTML content.
 * 
 * The parser supports OpenID 1.1 and OpenID 2.0
 * 
 * the xml parsing was removed as this is replaced now by the XRDS Parser
 * 
 * @author rsoika
 * @author monzillo
 */
public class OpenIDPageParser {
	private static final int DEBUG_TRUST = 32;

	protected static final Logger defaultLogger = Logger
			.getLogger(OpenIDPageParser.class.getName());

	// hidden constructor
	private OpenIDPageParser() {
	}

	/**
	 * This method is responsible for parsing the content of an openID Page.
	 * 
	 * @param connection
	 *            - the connection on which the response to the GET of the
	 *            OpenID page may be read.
	 * @param expectedContentType
	 *            value of Accept header of request.
	 * @param logger
	 *            - may be used to define the logger used by this class. If a
	 *            null value is passed for this param, default Logger is used.
	 * @param debug
	 *            when true, parser will log debugging info
	 * @return a property map containing the values of the OpenID properties
	 *         read from the page.
	 * @throws IOException
	 * @throws IOException
	 */
	public static Properties parse(URL url, HostnameVerifier hostnameVerifier,
			Logger logger, boolean debug) throws AuthException, IOException {

		if (logger == null) {
			logger = defaultLogger;
		}
		// support parsing openid 1.1 and 2.0
		ElementQuery[] queries = new ElementQuery[] {
				new ElementQuery("link", new KeyValuePair[] {
						new KeyValuePair(HTML.Attribute.REL, "openid.server"),
						new KeyValuePair(HTML.Attribute.HREF, null) }),
				new ElementQuery("link",
						new KeyValuePair[] {
								new KeyValuePair(HTML.Attribute.REL,
										"openid.delegate"),
								new KeyValuePair(HTML.Attribute.HREF, null) }),
				new ElementQuery("link",
						new KeyValuePair[] {
								new KeyValuePair(HTML.Attribute.REL,
										"openid2.provider"),
								new KeyValuePair(HTML.Attribute.HREF, null) }),
				new ElementQuery("link",
						new KeyValuePair[] {
								new KeyValuePair(HTML.Attribute.REL,
										"openid2.local_id"),
								new KeyValuePair(HTML.Attribute.HREF, null) })

		};

		Properties rvalue;

		// Check first for XRDS Location header
		// if available proceed with the new URI location found in the
		// header field 'X-XRDS-Location'
		URL urlXRDS = getXRDSLocation(url, hostnameVerifier, logger, debug);
		if (urlXRDS != null) {
			// change URL to XRDS Location
			url = urlXRDS;
		}
		// debugURLContent(url);

		// now try to find OpenID Service Elements in a XRDS Document
		try {
			rvalue = parseXRDSPage(url.openStream(), queries, logger, debug);
		} catch (Exception xrdse) {
			rvalue = null;
		}
		// if no service element was found proceed with HTML-Based Discovery...
		if (rvalue == null || (rvalue.getProperty("openid.server") == null)
				&& (rvalue.getProperty("openid2.provider") == null)) {
			rvalue = parseHTMLPage(url, queries, logger, debug);
		}
		// if still no provider element found - throw exception....
		if (rvalue == null || (rvalue.getProperty("openid.server") == null)
				&& (rvalue.getProperty("openid2.provider") == null)) {
			String msg = "openid.no_openid_server";
			logger.log(Level.WARNING, msg);
			AuthException ae = new AuthException(msg);
			throw ae;
		}

		// determine version and add to property list
		if (rvalue.getProperty("openid2.provider") != null)
			rvalue.setProperty("openid.version", "2.0");
		else
			rvalue.setProperty("openid.version", "1.1");
		return rvalue;
	}

	/**
	 * This method determines the Content Type of url
	 * 
	 * @return
	 * @throws Exception
	 */
	private static String getContentType(URL url,
			HostnameVerifier hostnameVerifier, Logger logger, boolean debug)
			throws AuthException {
		HttpURLConnection connection = null;
		String contentType = null;
		try {

			// should use https
			connection = (HttpURLConnection) url.openConnection();

			if (connection instanceof HttpsURLConnection) {
				if (debug)
					logger.log(Level.INFO, "openid.setting_hostname_verifier:",
							hostnameVerifier.toString());
				((HttpsURLConnection) connection)
						.setHostnameVerifier(hostnameVerifier);
			}

			// should ensure that connection is using ssl
			// should we set a connection timeout?

			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-type",
					"application/x-www-form-urlencoded");

			connection.connect();

			contentType = connection.getContentType().toLowerCase();
			if (debug) {
				String msg = "openid.id_page_content_type_" + contentType;
				logger.info(msg);
			}

		} catch (Exception ex) {
			// throw exception....
			String msg = "openid.idpage_connection_failure";
			logger.log(Level.WARNING, msg, ex);
			AuthException ae = new AuthException(msg);
			ae.initCause(ex);
			throw ae;

		} finally {
			connection.disconnect();
		}
		return contentType;

	}

	/**
	 * This method looks for a XRDS Header
	 * 
	 * 'X-XRDS-Location'
	 * 
	 * For example see : http://yahoo.com
	 * 
	 * The Method returns null if no X-XRDS-Location Header is included.
	 * Otherwise the method return the X-XRDS-Location URI
	 * 
	 * @return
	 * @throws Exception
	 */
	private static URL getXRDSLocation(URL url,
			HostnameVerifier hostnameVerifier, Logger logger, boolean debug)
			throws AuthException {
		HttpURLConnection connection = null;
		URL xrdsLocation = null;
		try {

			// should use https
			connection = (HttpURLConnection) url.openConnection();

			if (connection instanceof HttpsURLConnection) {
				if (debug)
					logger.log(Level.INFO, "openid.setting_hostname_verifier:",
							hostnameVerifier.toString());
				((HttpsURLConnection) connection)
						.setHostnameVerifier(hostnameVerifier);
			}

			// should ensure that connection is using ssl
			// should we set a connection timeout?
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-type",
					"application/x-www-form-urlencoded");
			connection.connect();

			// analyse header for xrd document
			String xrdsURL = connection.getHeaderField("X-XRDS-Location");
			if (xrdsURL != null) {
				xrdsLocation = new URL(xrdsURL);
				if (debug) {
					String msg = "openid.X-XRDS-Location:" + xrdsURL;
					logger.info(msg);
				}
			}

		} catch (Exception ex) {
			// throw exception....
			String msg = "openid.idpage_connection_failure";
			logger.log(Level.WARNING, msg, ex);
			AuthException ae = new AuthException(msg);
			ae.initCause(ex);
			throw ae;

		} finally {
			connection.disconnect();
		}
		return xrdsLocation;

	}

	private static Properties addProperty(Properties properties, String key,
			String value) {
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
	private static Properties parseHTMLPage(URL url, ElementQuery[] queries,
			Logger logger, boolean debug) throws AuthException {

		if (debug) {
			String msg = "openid.parsing_html_id_page";
			logger.info(msg);
		}

		HTMLParserCallback callback = new HTMLParserCallback(queries, logger,
				debug);
		try {
			// Create an InputStreamReader to read the HTML document.
			// The reader uses the default character set for decoding bytes into
			// characters.
			InputStreamReader reader = new InputStreamReader(url.openStream());

			try {
				// use the reader to create a parser and parse the document

				new ParserDelegator().parse(reader, callback, false);

			} catch (ChangedCharSetException e) {
				// parser throws a ChangedCharSetException if it encounters a
				// <meta>
				// tag with a charset attribute that specifies a character set
				// other
				// than the default.

				// Extract the new character set name from the exception.

				String csspec = e.getCharSetSpec();
				Pattern p = Pattern.compile("charset=\"?(.+)\"?\\s*;?",
						Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(csspec);
				String charset = m.find() ? m.group(1) : "ISO-8859-1";

				// Create a new reader that uses the new charset

				reader = new InputStreamReader(url.openStream(), charset);

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
	 * parses a xml document
	 * 
	 * @param doc
	 * @param queries
	 * @param logger
	 * @param debug
	 * @return
	 * @throws javax.security.auth.message.AuthException
	 */
	private static Properties parseXRDSPage(InputStream doc,
			ElementQuery[] queries, Logger logger, boolean debug)
			throws AuthException {

		if (debug) {
			String msg = "openid.parsing_xrds_id_page";
			logger.info(msg);
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {

			SAXParser parser = factory.newSAXParser();
			parser.parse(doc, new XRDSParserHandler(queries, logger, debug));

		} catch (Throwable t) {
			String msg = "openid.failed_parsing_id_page";
			logger.log(Level.WARNING, msg, t);
			AuthException ae = new AuthException(msg);
			ae.initCause(t);
			throw ae;

		}

		Properties rvalue = null;
		rvalue = addProperty(rvalue, "openid2.provider", queries[0]
				.getAttributeValue("href"));
		// rvalue = addProperty(rvalue, "openid2.local_id", queries[1]
		// .getAttributeValue("href"));
		return rvalue;
	}

	/**
	 * parses a xml document
	 * 
	 * @param doc
	 * @param queries
	 * @param logger
	 * @param debug
	 * @return
	 * @throws javax.security.auth.message.AuthException
	 */
	private static Properties parseXMLPage(InputStream doc,
			ElementQuery[] queries, Logger logger, boolean debug)
			throws AuthException {

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
		rvalue = addProperty(rvalue, "openid.server", queries[0]
				.getAttributeValue("href"));
		rvalue = addProperty(rvalue, "openid.delegate", queries[1]
				.getAttributeValue("href"));
		return rvalue;
	}

	private static class HTMLParserCallback extends
			HTMLEditorKit.ParserCallback {

		ElementQuery[] queries;
		Logger logger;
		boolean debug;

		private HTMLParserCallback(ElementQuery[] queries, Logger logger,
				boolean debug) {
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
						String msg = "HTMLParserCallback.found_simple_element"
								+ query.eName;
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
							if (keyValue != null
									&& !keyValue.equals(value.toString())) {
								return;
							}
						} else {
							return;
						}
					}

					// assign attribute values to query if element fully
					// satisfies query.
					if (debug) {
						logger
								.info("HTMLParserCallback.element_satisfies_query");
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
							logger.info("XMLParserCallback.attribute: " + name
									+ " Value: " + value);
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
						String msg = "XMLParserHandler.found_element"
								+ query.eName;
						logger.info(msg);
					}

					// check that elememt contains suitable attribute
					// to match query
					for (int i = 0; i < query.keyPairs.length; i++) {

						String value = atts.getValue(query.keyPairs[i].key
								.toString());

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
					// assign attribute values to query if element fully
					// satisfies query.
					query.attributes = new HashMap();
					for (int i = 0; i < atts.getLength(); i++) {

						String attribute = atts.getQName(i);
						String value = atts.getValue(i);
						query.attributes.put(attribute, value);
						if (debug) {
							if (i == 0) {
								logger
										.info("XMLParserHandler.element_satisfies_query");
							}
							logger.info("XMLParserHandler.attribute: "
									+ attribute + " Value: " + value);
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

	private static class XRDSParserHandler extends DefaultHandler {

		boolean uriTag = false;
		Logger logger;
		boolean debug;
		ElementQuery[] queries;
		boolean forcequit = false;

		XRDSParserHandler(ElementQuery[] queries, Logger logger, boolean debug) {
			this.queries = queries;
			this.logger = logger;
			this.debug = debug;
		}

		@Override
		public void startElement(String nameSpaceURI, String localName,
				String qName, Attributes attrs) throws SAXException {

			uriTag = qName.equals("URI");
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {

			if (forcequit)
				return;

			// simply break if first URI tag is found
			// this is a simple first version - no full support of spec 2.0
			if (uriTag) {
				String str = new String(ch, start, length);
				queries[0].attributes = new HashMap();
				queries[0].attributes.put("href", str);
				if (debug) {
					String msg = "XRDSParserHandler.found_uri_element" + str;
					logger.info(msg);
				}
				uriTag = false;
				forcequit = true;
			}
		}

	}

	/**
	 * Helper method to analyse url content...
	 * 
	 * @param url
	 */
	private static void debugURLContent(URL url) {

		/* DEBUG HTML CONTENT */

		InputStreamReader debugreader = null;
		try {
			debugreader = new InputStreamReader(url.openStream());
			// Den InputStreamReader in einem BufferedReader verpacken.
			BufferedReader br = new BufferedReader(debugreader);
			// Zeilenweise einlesen
			String zeile = br.readLine();
			while (zeile != null) {
				System.out.println(zeile);
				zeile = br.readLine();
			}
			// BufferedReader schliessen
			br.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			try {
				debugreader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}