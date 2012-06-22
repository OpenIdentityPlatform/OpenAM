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
 * $Id: LDAPUtils.java,v 1.2 2009/08/24 11:37:44 hubertlvg Exp $
 *
 */ 

package com.sun.identity.openid.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class LDAPUtils {

	private static final Logger logger = Logger.getLogger(LDAPUtils.class
			.getName());

	/**
	 * Modifies an attribute.
	 * 
	 * @param ctx
	 * @param dn
	 * @param name
	 * @param value
	 * @throws BackendException
	 */
	public static void modifyAttribute(DirContext ctx, String dn, String name,
			String value) throws BackendException {
		try {
			ctx.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE,
					new BasicAttributes(name, value));
		} catch (NamingException e) {
			throw new BackendException(e.getMessage());
		}
	}

	public static void addAttributeValues(DirContext ctx, String dn,
			String name, ArrayList<String> values) throws NamingException {
		BasicAttributes as = new BasicAttributes();
		BasicAttribute attr = new BasicAttribute(name);
		for (Iterator<String> v = values.iterator(); v.hasNext();) {
			attr.add(v.next());
		}
		as.put(attr);
		ctx.modifyAttributes(dn, DirContext.ADD_ATTRIBUTE, as);
	}

	public static void removeAttributeValues(DirContext ctx, String dn,
			String name, ArrayList<String> values) throws NamingException {
		BasicAttributes as = new BasicAttributes();
		BasicAttribute attr = new BasicAttribute(name);
		for (Iterator<String> v = values.iterator(); v.hasNext();) {
			attr.add(v.next());
		}
		as.put(attr);
		ctx.modifyAttributes(dn, DirContext.REMOVE_ATTRIBUTE, as);
	}

	/**
	 * Modifies multiple attributes.
	 * 
	 * @param ctx
	 * @param dn
	 * @param map
	 * @return status of modify attempt
	 */
	public static boolean modifyAttributes(DirContext ctx, String dn,
			HashMap map) throws NamingException {
		boolean modifyStatus = false;
		BasicAttributes bas = new BasicAttributes();
		Set keys = map.keySet();
		Iterator i = keys.iterator();
		Object keyName = null;
		while (i.hasNext()) {
			keyName = i.next();
			bas.put((String) keyName, (String) map.get(keyName));
		}

		ctx.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, bas);
		modifyStatus = true;
		return modifyStatus;
	}

	/**
	 * Removes the specific value from the named attribute
	 * 
	 * @param ctx
	 *            DirContext
	 * @param dn
	 *            String
	 * @param name
	 *            String
	 * @param value
	 *            String
	 * @throws BackendException
	 */
	public static void deleteAttribute(String dn, String name, String value,
			DirContext ctx) throws BackendException {

		try {

			ctx.modifyAttributes(dn, DirContext.REMOVE_ATTRIBUTE,
					new BasicAttributes(name, value));
		} catch (NamingException e) {
			throw new BackendException(e.getMessage());
		}
	}

	/**
	 * Removes all values from the named attribute
	 * 
	 * @param ctx
	 *            DirContext
	 * @param dn
	 *            String
	 * @param name
	 *            String
	 * 
	 */
	public static void deleteAttribute(String dn, String name, DirContext ctx) {

		try {

			BasicAttributes basicAttributes = new BasicAttributes();
			basicAttributes.put(new BasicAttribute(name));
			ctx.modifyAttributes(dn, DirContext.REMOVE_ATTRIBUTE,
					basicAttributes);
		} catch (NamingException e) {
			logger.info("Name not found " + name);
		}
	}

	/**
	 * Removes all values from the named attributes
	 * 
	 * 
	 * 
	 * @param dn
	 *            String
	 * @param names
	 *            String[]
	 * 
	 */
	public static void deleteAttributes(String dn, String[] names,
			DirContext ctx) {
		for (int i = 0; i < names.length; i++) {
			deleteAttribute(dn, names[i], ctx);
		}
	}

	/**
	 * Close context.
	 * 
	 * @param ctx
	 */
	public static void close(DirContext ctx) {
		if (ctx == null) {
			return;
		}

		try {
			ctx.close();
		} catch (NamingException ignore) {
			logger.log(Level.SEVERE, ignore.getMessage(), ignore);
		}
	}

	/**
	 * Close naming enumeration.
	 * 
	 * @param ne
	 */
	public static void close(NamingEnumeration ne) {
		if (ne == null) {
			return;
		}

		try {
			ne.close();
		} catch (NamingException ignore) {
			logger.log(Level.WARNING, ignore.getMessage(), ignore);
		}
	}

	/**
	 * Get attribute value.
	 * 
	 * @param attrs
	 * @param name
	 * @return the value of the attribute.
	 * @throws BackendException
	 */
	public static String getAttributeValue(Attributes attrs, String name)
			throws BackendException {
		String val = null;
		Attribute attr = attrs.get(name);
		try {
			if (attr != null) {
				val = (String) attr.get();
			}
		} catch (NamingException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new BackendException(e.getMessage());
		}
		return val;
	}

	/**
	 * Get attribute value as an int.
	 * 
	 * @param attrs
	 * @param name
	 * @return The int value.
	 * @throws BackendException
	 */
	public static int getIntAttributeValue(Attributes attrs, String name)
			throws BackendException {
		int val = 0;
		Attribute attr = attrs.get(name);
		try {
			if (attr != null) {
				String tempVal = (String) attr.get();
				if (tempVal != null && !"".equals("")) { // extra check to
					// make sure string
					// isn't empty.
					val = Integer.parseInt(tempVal);
				}
			}
		} catch (NamingException e) {
			// logger.log(Level.SEVEREe.getMessage(), e);
			throw new BackendException(e.getMessage());
		}
		return val;
	}

	/**
	 * A list of values.
	 * 
	 * @param attrs
	 * @param name
	 * @return the list of values.
	 * @throws BackendException
	 */
	public static ArrayList<String> getMultiValueAttribute(Attributes attrs,
			String name) throws BackendException {
		Attribute attr = attrs.get(name);
		ArrayList<String> values = null;
		try {
			if (attr != null) {
				values = new ArrayList<String>();
				NamingEnumeration valuesEnum = attr.getAll();
				while (valuesEnum.hasMore()) {
					values.add((String) valuesEnum.next());
				}
				return values;
			}
		} catch (NamingException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new BackendException(e.getMessage());
		}
		return null;
	}

	/**
	 * Gets a naming enumeration.
	 * 
	 * @param baseDN
	 * @param filter
	 * @param returnAttributes
	 * @return A naming enumeration.
	 * @throws BackendException
	 */
	public static NamingEnumeration<SearchResult> search(String baseDN,
			String filter, String[] returnAttributes, DirContext ctx)
			throws BackendException {
		NamingEnumeration<SearchResult> ne = null;
		boolean found = false;

		try {

			SearchControls sc = new SearchControls();
			sc.setReturningAttributes(returnAttributes);
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ne = ctx.search(baseDN, filter, sc);
			found = ne.hasMore();
			return found ? ne : null;
		} catch (NamingException e) {

			throw new BackendException(e.getMessage());
		} finally {

			// if no records are found close it
			if (!found) {
				close(ne);
			}
		}

	}

	/**
	 * Gets a search result.
	 * 
	 * @param baseDN
	 * @param filter
	 * @param returnAttributes
	 * @return the search result.
	 * @throws BackendException
	 */
	public static SearchResult searchOneRecord(String baseDN, String filter,
			String[] returnAttributes, DirContext ctx) throws BackendException {
		NamingEnumeration ne = null;
		try {
			ne = search(baseDN, filter, returnAttributes, ctx);
			return ne != null ? (ne.hasMore() ? (SearchResult) ne.next() : null)

					: null;
		} catch (NamingException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw new BackendException(e.getMessage());
		} finally {
			close(ne);
		}
	}

	/**
	 * gets a user dn
	 * 
	 * @param baseDN
	 * @param filter
	 * @return the user dn
	 * @throws BackendException
	 */
	public static String getUserDN(String baseDN, String filter, DirContext ctx)
			throws BackendException {

		NamingEnumeration ne = null;
		String d = null;
		try {
			ne = search(baseDN, filter, null, ctx);
			if (ne != null) {
				SearchResult sr = (SearchResult) ne.next();
				// d = sr.getName() + "," + baseDN;
				d = sr.getNameInNamespace();
			}
		} catch (NamingException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			close(ne);
		}
		return d;
	}

	/**
	 * Checks if attribute is populated.
	 * 
	 * @param attr
	 * @return true if not empty or null.
	 */
	public static boolean isValidAttr(String attr) {
		boolean valid = false;
		String attrib = attr;

		if (attrib == null || attrib == "") {
			valid = false;
		} else {
			valid = true;
		}
		return valid;
	}

	/**
	 * Checks if uid is the same.
	 * 
	 * @param uid
	 * @param updateUID
	 * @return true if matching
	 */
	public static boolean isSameUID(String uid, String updateUID) {
		boolean valid = false;
		if (uid.equalsIgnoreCase(updateUID)) {
			valid = true;
		}
		return valid;
	}

	/**
	 * Checks if uid is the same.
	 * 
	 * @param uid
	 * @param updateUID
	 * @return true if matching
	 */
	public static boolean isSameVZID(String uid, String updateUID) {
		boolean valid = false;
		if (uid.equalsIgnoreCase(updateUID)) {
			valid = true;
		}
		return valid;
	}

	/**
	 * Checks for record existence.
	 * 
	 * @param dn
	 * @param filter
	 * @param returnAttributes
	 * @return true if exists.
	 * @throws BackendException
	 */
	public static boolean recordExists(String dn, String filter,
			String[] returnAttributes, DirContext ctx) throws BackendException {
		return (searchOneRecord(dn, filter, returnAttributes, ctx) != null);
	}

	/**
	 * Gets attribute value.
	 * 
	 * @param dn
	 * @param filter
	 * @param key
	 * @return attribute value
	 * @throws BackendException
	 */
	public static String searchOneAttributeValue(String dn, String filter,
			String key, DirContext ctx) throws BackendException {
		String attr = null;
		try {
			String[] ra = { key };
			SearchResult sr = LDAPUtils.searchOneRecord(dn, filter, ra, ctx);
			if (sr != null) {
				Attributes attrs = sr.getAttributes();
				attr = LDAPUtils.getAttributeValue(attrs, key);
			}
		} finally {
		}
		return attr;
	}

	/**
	 * Per http://www.faqs.org/rfcs/rfc2254.html
	 * 
	 * If a value should contain any of the following characters
	 * 
	 * Character ASCII value --------------------------- * 0x2a ( 0x28 ) 0x29 \
	 * 0x5c NUL 0x00
	 * 
	 * the character must be encoded as the backslash '\' character (ASCII 0x5c)
	 * followed by the two hexadecimal digits representing the ASCII value of
	 * the encoded character. The case of the two hexadecimal digits is not
	 * significant.
	 * 
	 * @param filter
	 * @return search filter escaped string
	 */
	public static String escapeSearchFilter(String filter) {
		if (filter == null) {
			return null;
		}
		String data = new String(filter);
		data = data.replaceAll("\\\\", "\\\\5c");
		data = data.replaceAll("\\*", "\\\\2a");
		data = data.replaceAll("\\(", "\\\\28");
		data = data.replaceAll("\\)", "\\\\29");
		data = data.replaceAll("\\" + Character.toString('\u0000'), "\\\\00");
		return data;
	}

	/**
	 * Deletes a record.
	 * 
	 * @param ctx
	 *            DirContext
	 * @param dn
	 * @return true if record was deleted.
	 * 
	 */
	public static boolean deleteRecord(DirContext ctx, String dn) {

		boolean deleteStatus = false;
		try {
			ctx.destroySubcontext(dn);
			deleteStatus = true;
		} catch (NameNotFoundException ex) {
			logger.info("name not found:" + dn);
		} catch (NamingException ex) {
			logger
					.log(Level.SEVERE, "NamingException : " + ex.getMessage(),
							ex);
		}
		return deleteStatus;
	}

	/**
	 * Gets directory context.
	 * 
	 * @return directory context
	 * @throws BackendException
	 */
	public static DirContext getDirContext(String ldapHost, int ldapPort,
			String bindDN, String bindPwd) {

		String tmpStr = null;
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");

		tmpStr = "ldap://" + ldapHost + ":" + ldapPort;

		env.put(Context.PROVIDER_URL, tmpStr);

		if (bindDN != null && !bindDN.equals("")) {
			env.put(Context.SECURITY_PRINCIPAL, bindDN);
			env.put(Context.SECURITY_CREDENTIALS, bindPwd);
		} else {
		}

		// use pooling
		env.put("com.sun.jndi.ldap.connect.pool", "true");

		DirContext dirContext = null;
		try {
			dirContext = new InitialDirContext(env);
		} catch (NamingException e) {
			logger.info("Name not found " + env);
		}
		return dirContext;
	}

	/**
	 * Adds a record.
	 * 
	 * @param name
	 * @param objectClasses
	 * @param attribs
	 * @return true if added.
	 */
	public static boolean addRecord(String name, ArrayList objectClasses,
			Map attribs, DirContext ctx) throws NamingException {

		boolean createStatus = false;
		BasicAttribute oc = new BasicAttribute("objectclass");

		if (objectClasses != null && !objectClasses.isEmpty()) {
			for (Iterator it = objectClasses.iterator(); it.hasNext();) {
				oc.add(it.next());
			}
		}
		BasicAttributes as = new BasicAttributes();
		as.put(oc);
		if (attribs != null && !attribs.isEmpty()) {
			for (Iterator it = attribs.keySet().iterator(); it.hasNext();) {
				String key = (String) it.next();
				String val = (String) attribs.get(key);

				if (key != null && val != null) {
					as.put(key, val);
				}
			}
		}

		DirContext ctx1 = ctx.createSubcontext(name, as);
		createStatus = true;
		LDAPUtils.close(ctx1);

		return createStatus;
	}

	public static boolean addRecord(String name,
			ArrayList<String> objectClasses, Map<String, String> svAttribs,
			Map<String, ArrayList<String>> mvAttribs, DirContext ctx)
			throws NamingException {

		boolean createStatus = false;
		BasicAttribute oc = new BasicAttribute("objectclass");

		if (objectClasses != null && !objectClasses.isEmpty()) {
			for (Iterator<String> it = objectClasses.iterator(); it.hasNext();) {
				oc.add(it.next());
			}
		}
		BasicAttributes as = new BasicAttributes();
		as.put(oc);
		if (svAttribs != null && !svAttribs.isEmpty()) {
			for (Iterator<String> it = svAttribs.keySet().iterator(); it
					.hasNext();) {
				String key = (String) it.next();
				String val = (String) svAttribs.get(key);

				if (key != null && val != null) {
					as.put(key, val);
				}
			}
		}

		if (mvAttribs != null && !mvAttribs.isEmpty()) {
			for (Iterator<String> it = mvAttribs.keySet().iterator(); it
					.hasNext();) {
				String key = (String) it.next();
				ArrayList<String> vals = mvAttribs.get(key);

				if (key != null && vals != null && vals.size() > 0) {
					Attribute attr = new BasicAttribute(key);
					for (Iterator<String> v = vals.iterator(); v.hasNext();) {
						attr.add(v.next());
					}
					as.put(attr);
				}
			}
		}

		DirContext ctx1 = ctx.createSubcontext(name, as);
		createStatus = true;
		LDAPUtils.close(ctx1);
		//LDAPUtils.close(ctx);

		return createStatus;
	}

	public static String genetateGUID() {
		return "random values";

	}
}
