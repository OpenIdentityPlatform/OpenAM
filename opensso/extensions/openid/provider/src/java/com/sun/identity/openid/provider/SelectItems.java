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
 * $Id: SelectItems.java,v 1.1 2009/04/24 21:01:58 rparekh Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan
 */

package com.sun.identity.openid.provider;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

/**
 * A managed bean used in JavaServer Faces to provide localization functions.
 * 
 * This bean is intended to be application-scope, as it caches any lists it
 * generates for reuse in subsequent requests from different users.
 * 
 * @author pbryan
 */
public class SelectItems {
	/** Defines the sort order of resources to return. */
	private enum OrderBy {
		KEY, VALUE
	};

	/** The basename of the resource bundle file. */
	private static final String BASENAME = "Messages";

	/** Number of years to display in year select item list. */
	private static final int YEARS = 100;

	/**
	 * Map of select item lists, keyed by string concatenation of list type and
	 * locale.
	 */
	private static final HashMap<String, ArrayList<SelectItem>> itemsMap = new HashMap<String, ArrayList<SelectItem>>();

	/**
	 * Constructs a new select items bean, per the JavaBeans specification.
	 */
	public SelectItems() {
	}

	/**
	 * Returns the resource bundle that is the closest match to the locale of
	 * the current root view.
	 * 
	 * This method is intended to provide the exact same result as an
	 * f:loadBundle JSF tag invocation.
	 * 
	 * @return the resource bundle for the current root view's locale.
	 */
	private static ResourceBundle getBundle() {
		return ResourceBundle.getBundle(BASENAME, FacesContext
				.getCurrentInstance().getViewRoot().getLocale());
	}

	/**
	 * Returns a List of items in a range, in the order based on the specified
	 * start and end of the range.
	 * 
	 * @param name
	 *            name used to retrieve empty_name value from bundle.
	 * @param start
	 *            start point in the range (inclusive).
	 * @param end
	 *            end point in the range (inclusive).
	 * @return a List of SelectItem objects.
	 */
	private static ArrayList<SelectItem> range(String name, int start, int end) {
		// retrieve resource bundle that matches the current root view locale
		ResourceBundle bundle = getBundle();

		// use bundle's locale, to prevent generating duplicate lists for same
		// bundle
		Locale locale = bundle.getLocale();

		// this key will be used to store and retreive already-generated lists
		String listKey = name + "_" + start + "_" + end + "_"
				+ locale.toString();

		// attempt to fetch an already-generated list
		ArrayList<SelectItem> selectItems = itemsMap.get(listKey);

		// list already generated; just use it
		if (selectItems != null) {
			return selectItems;
		}

		// begin lazy initialization
		selectItems = new ArrayList<SelectItem>();

		// try to add the range default value if in the bundle
		try {
			selectItems.add(new SelectItem("", bundle
					.getString("empty_" + name)));
		}

		// no resource means no default (unspecified) value for list; keep going
		catch (MissingResourceException mre) {
		}

		// establish the direction of iteration
		int step = (start <= end ? 1 : -1);

		for (int n = start;; n += step) {
			String s = Integer.toString(n);
			selectItems.add(new SelectItem(s, s));

			if ((step >= 0 && n >= end) || (step <= 0 && n <= end)) {
				break;
			}
		}

		// remember list for next time
		itemsMap.put(listKey, selectItems);

		return selectItems;
	}

	/**
	 * Returns a List of items from the localization resource bundle, localized
	 * for the locale of the current root view, and sorted according the same
	 * locale.
	 * 
	 * Note: Making this method synchronized was considered and rejected,
	 * because the end result of a race condition will only be a temporary
	 * duplication of work, with no other obvious deleterious effects.
	 * 
	 * @param prefix
	 *            the prefix in the resource bundle to group items by.
	 * @param order
	 *            defines the sort order of the items.
	 * @return a List of SelectItem objects.
	 */
	private static ArrayList<SelectItem> resources(String prefix, OrderBy order) {
		int prefixLength = prefix.length();
		assert (prefixLength != 0);

		// retrieve resource bundle that matches the current root view locale
		ResourceBundle bundle = getBundle();

		// use bundle's locale, to prevent generating duplicate lists for same
		// bundle
		Locale locale = bundle.getLocale();

		// this key will be used to store and retreive already-generated lists
		String listKey = prefix + locale.toString();

		// attempt to fetch an already-generated list
		ArrayList<SelectItem> selectItems = itemsMap.get(listKey);

		// list already generated; just use it
		if (selectItems != null) {
			return selectItems;
		}

		// begin lazy initialization
		selectItems = new ArrayList<SelectItem>();

		// stores items keyed by specified sort attribute
		HashMap<String, String> map = new HashMap<String, String>();

		for (Enumeration<String> keys = bundle.getKeys(); keys
				.hasMoreElements();) {
			String key = keys.nextElement();

			// ignore property that doesn't match grouping prefix
			if (!key.startsWith(prefix)) {
				continue;
			}

			String value = bundle.getString(key);

			// use key sans prefix from here on
			key = key.substring(prefixLength);

			// put empty key (if specified) at top of select items list
			if (key.length() == 0) {
				selectItems.add(new SelectItem("", value));
				continue;
			}

			if (order == OrderBy.KEY) {
				map.put(key, value);
			}

			else {
				map.put(value, key);
			}
		}

		// sort the items that are going to be placed in the list
		ArrayList<String> sorted = new ArrayList<String>(map.keySet());

		// if sorting by key, sort by natural order
		if (order == OrderBy.KEY) {
			Collections.sort(sorted);
		}

		// if sorting by value, sort according to locale
		else {
			Collections.sort(sorted, Collator.getInstance(locale));
		}

		// place sorted items into select items list
		for (String next : sorted) {
			if (order == OrderBy.KEY) {
				selectItems.add(new SelectItem(next, map.get(next)));
			}

			else {
				selectItems.add(new SelectItem(map.get(next), next));
			}
		}

		// remember list for next time
		itemsMap.put(listKey, selectItems);

		return selectItems;
	}

	/**
	 * Returns a List of countries, localized for the locale of the current root
	 * view.
	 * 
	 * @return a List of SelectItem objects.
	 */
	public List getCountries() {
		return resources("country_", OrderBy.VALUE);
	}

	/**
	 * Returns a List of days, in ascending order.
	 * 
	 * @return a List of SelectItem objects.
	 */
	public List getDays() {
		return range("days", 1, 31);
	}

	/**
	 * Returns a List of genders, localized for the locale of the current root
	 * view.
	 * 
	 * @return a List of SelectItem objects.
	 */
	public List getGenders() {
		return resources("gender_", OrderBy.VALUE);
	}

	/**
	 * Returns a List of languages, localized for the locale of the current root
	 * view.
	 * 
	 * @return a List of SelectItem objects.
	 */
	public List getLanguages() {
		return resources("language_", OrderBy.VALUE);
	}

	/**
	 * Returns a List of months, localized for the locale of the current root
	 * view.
	 * 
	 * @return a List of SelectItem objects.
	 */
	public List getMonths() {
		return resources("month_", OrderBy.KEY);
	}

	/**
	 * Returns a List of time zones, localized for the locale of the current
	 * root view.
	 * 
	 * @return a List of SelectItem objects.
	 */
	public List getTimezones() {
		return resources("timezone_", OrderBy.KEY);
	}

	/**
	 * Returns a List of years, ordered in reverse chronological order.
	 * 
	 * @return a List of SelectItem objects.
	 */
	public List getYears() {
		// get the current year
		GregorianCalendar calendar = new GregorianCalendar();
		int year = calendar.get(calendar.YEAR);

		// produce a descending list of years starting with the current
		return range("years", year, year - YEARS);
	}
}
