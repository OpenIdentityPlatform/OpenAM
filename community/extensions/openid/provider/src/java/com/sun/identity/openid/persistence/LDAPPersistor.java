/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LDAPPersistor.java,v 1.1 2009/04/24 21:01:57 rparekh Exp $
 *
 */ 

package com.sun.identity.openid.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * 
 * @author Robert Nguyen
 */
public class LDAPPersistor implements AttributePersistor {

	private static final Logger logger = Logger.getLogger(LDAPPersistor.class
			.getName());
	private static String peopleBase = null;
	private static String ldapHost = "localhost";
	private static int ldapPort = 389;
	private static String bindDN = null;
	private static String bindPwd = null;
	private static String searchAttr = null;
	private static String opAttributeName;
	private static String[] returnAttributes = null;
	private static String[] attrNode;
	private static String ROOT_ELEMENT = "Openid-Attribute-Exhange";
	private static String SUB_ROOT_ELEMENT = "Openid-Attribute-Exhange-SubRoot";
	private static String OPENID_RP_ELEMENT = "openid-rp-url";
	private static String PROPERTY_FILE_NAME = "ldap.properties";

	static {
		loadConfig();
	}

	public LDAPPersistor() {

	}

	/****************************************************
	 * get a map of attributes stored in Directory for populating the view
	 * 
	 * @param uid
	 *            : user id
	 * @param rp
	 *            : reply party url
	 * @return: Map of attributes
	 * 
	 *************************/
	public Map<String, String> getAttributes(String uid, String rp)
			throws BackendException {
		DirContext ctx = LDAPUtils.getDirContext(ldapHost, ldapPort, bindDN,
				bindPwd);

		Map<String, String> returnMap = null;
		try {
			SearchResult sr = LDAPUtils.searchOneRecord(peopleBase,
					makeFilter(uid), returnAttributes, ctx);
			String opAttributesXML = LDAPUtils.getAttributeValue(sr
					.getAttributes(), opAttributeName);
			returnMap = readResult(rp, opAttributesXML);
		} catch (BackendException ex) {
			Logger.getLogger(LDAPPersistor.class.getName()).log(Level.SEVERE,
					null, ex);
			throw ex;
		} catch (JDOMException e) {
			Logger.getLogger(LDAPPersistor.class.getName()).log(Level.SEVERE,
					null, e);
			BackendException ex = new BackendException(e.getMessage());
			ex.fillInStackTrace();
			throw ex;
		} catch (IOException e) {
			BackendException ex = new BackendException(e.getMessage());
			ex.fillInStackTrace();
			throw ex;
		} finally {
			LDAPUtils.close(ctx);
		}
		return returnMap;
	}

	/**********************************************************
	 * Save attributes to Directory
	 * 
	 * @param uid
	 *            : user id
	 * @param rp
	 *            : reply party url
	 * @param attributes
	 * 
	 * ****************************************************************/
	public void setAttributes(String uid, String rp,
			Map<String, String> attributes) throws BackendException {
		
		Logger.getLogger(LDAPPersistor.class.getName()).log(Level.INFO,
				"setAttributes() called");
		DirContext ctx = LDAPUtils.getDirContext(ldapHost, ldapPort, bindDN,
				bindPwd);

		try {
			String dn = LDAPUtils.getUserDN(peopleBase, makeFilter(uid), ctx);
			SearchResult sr = LDAPUtils.searchOneRecord(peopleBase,
					makeFilter(uid), returnAttributes, ctx);
			String opAttributesXML;

			opAttributesXML = writeResult(rp, LDAPUtils.getAttributeValue(sr
					.getAttributes(), opAttributeName), attributes);
			
			Logger.getLogger(LDAPPersistor.class.getName()).log(Level.INFO,
			"Persisting XML " + opAttributesXML);
			if (null != opAttributesXML)
				LDAPUtils.modifyAttribute(ctx, dn, opAttributeName,
						opAttributesXML);

		} catch (JDOMException ex) {
			Logger.getLogger(LDAPPersistor.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (IOException ex) {
			Logger.getLogger(LDAPPersistor.class.getName()).log(Level.SEVERE,
					null, ex);
		} catch (BackendException ex) {
			Logger.getLogger(LDAPPersistor.class.getName()).log(Level.SEVERE,
					null, ex);
			throw ex;
		} finally {
			LDAPUtils.close(ctx);
		}
	}

	private String makeFilter(String uid) {
		return searchAttr + "=" + uid;
	}

	/** XML parser */
	private static Map<String, String> readResult(String rp, String opAttrsXML)
			throws JDOMException, IOException {

		if(opAttrsXML==null){
			return null;
		}
		Map<String, String> attrMap = new HashMap<String, String>();
		StringReader sr = new StringReader(opAttrsXML);
		BufferedReader br = new BufferedReader(sr);
		SAXBuilder builder = new SAXBuilder();
		Document d = builder.build(br);
		Iterator<Element> i = ((List<Element>) d.getRootElement().getChildren())
				.iterator();
		while (i.hasNext()) {
			Element e = (Element) i.next();
			if (e.getChildText(OPENID_RP_ELEMENT).equals(rp)) {
				for (int j = 0; j < attrNode.length; j++) {
					attrMap.put(attrNode[j], e.getChildText(attrNode[j]));
				}
				break;
			}

		}
		return attrMap;
	}

	/* XML builder */
	private String writeResult(String rp, String currentAttrXML,
			Map<String, String> attrs) throws JDOMException, IOException {
		StringBuilder sb = new StringBuilder();
		if (currentAttrXML == null || currentAttrXML.equals("")) {
			sb.append("<" + ROOT_ELEMENT + ">");
			sb.append(addMap2OpAttrs(rp, attrs));
			sb.append("</" + ROOT_ELEMENT + ">");
		} else if (currentAttrXML.indexOf(rp) == -1) {
			sb.append(currentAttrXML.substring(0, currentAttrXML.indexOf("</"
					+ ROOT_ELEMENT + ">")));
			sb.append((addMap2OpAttrs(rp, attrs)));
			sb.append("</" + ROOT_ELEMENT + ">");
		} else {
			StringReader sr = new StringReader(currentAttrXML);
			BufferedReader br = new BufferedReader(sr);
			SAXBuilder builder = new SAXBuilder();
			Document d = builder.build(br);
			sb.append("<" + ROOT_ELEMENT + ">");
			Iterator<Element> i = d.getRootElement().getChildren().iterator();
			while (i.hasNext()) {
				Element e = (Element) i.next();
				sb.append("<" + SUB_ROOT_ELEMENT + ">");
				sb.append("<" + OPENID_RP_ELEMENT + ">");
				sb.append(e.getChildText(OPENID_RP_ELEMENT));
				sb.append("</" + OPENID_RP_ELEMENT + ">");
				if (e.getChildText(OPENID_RP_ELEMENT).equals(rp)) {
					addMap(attrs, sb);
				} else {
					for (int j = 0; j < attrNode.length; j++) {
						sb.append("<" + attrNode[j] + ">");
						sb.append(e.getChildText(attrNode[j]));
						sb.append("</" + attrNode[j] + ">");
					}

				}
				sb.append("</" + SUB_ROOT_ELEMENT + ">");
			}
			sb.append("</" + ROOT_ELEMENT + ">");

		}
		return sb.toString();

	}

	/* build a string from a map */
	private void addMap(Map<String, String> attrs, StringBuilder sb) {
		for (String name : attrs.keySet()) {
			sb.append("<" + name + ">");
			sb.append(attrs.get(name));
			sb.append("</" + name + ">");
		}
	}

	/* create a node for new reply party */
	private StringBuilder addMap2OpAttrs(String rp, Map<String, String> attrs) {
		StringBuilder sb = new StringBuilder();

		sb.append("<" + SUB_ROOT_ELEMENT + ">");
		sb.append("<" + OPENID_RP_ELEMENT + ">");
		sb.append(rp);
		sb.append("</" + OPENID_RP_ELEMENT + ">");
		addMap(attrs, sb);
		sb.append("</" + SUB_ROOT_ELEMENT + ">");

		return sb;
	}

	private static void loadConfig() {
		Properties props = new Properties();
		InputStream in = null;
		try {
			in = LDAPPersistor.class.getClassLoader().getResourceAsStream(
					PROPERTY_FILE_NAME);
			if (in == null) {
				logger.log(Level.INFO,
						"LDAPPersistor.class.getClassLoader().getResourceAsStream("
								+ PROPERTY_FILE_NAME + ") returned null");
				in = LDAPPersistor.class
						.getResourceAsStream(PROPERTY_FILE_NAME);
			}
			if (in == null) {
				logger.log(Level.SEVERE,
						"LDAPPersistor.class.getResourceAsStream("
								+ PROPERTY_FILE_NAME + ") returned null");
				throw new RuntimeException("ldap.properties file not found");
			}
			props.load(in);
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(Level.SEVERE, "Cannot load ldap.properties", e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(Level.SEVERE, "Cannot load ldap.properties", e);
		}

		ldapHost = props.getProperty("ldap.host");
		String p = props.getProperty("ldap.port");
		if (p != null) {
			ldapPort = Integer.parseInt(p);
		}
		bindDN = props.getProperty("ldap.bind.dn");
		bindPwd = props.getProperty("ldap.bind.pwd");
		searchAttr = props.getProperty("ldap.people.search.attribute");
		peopleBase = props.getProperty("ldap.people.base");
		opAttributeName = props.getProperty("ldap.people.return.attribute");
		returnAttributes = new String[1];
		returnAttributes[0] = opAttributeName;

		String nodes = props.getProperty("ldap.people.attribute.nodes");
		if (nodes != null) {
			attrNode = nodes.split(",");
		}
		logger.log(Level.FINE, "Done loading proiperties");
	}

}
