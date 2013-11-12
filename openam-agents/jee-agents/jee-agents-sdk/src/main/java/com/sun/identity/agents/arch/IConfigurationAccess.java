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
 * $Id: IConfigurationAccess.java,v 1.2 2008/06/25 05:51:35 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.arch;

import java.util.Map;
import java.util.Set;

/**
 * This interface defines all the access APIs available for a given subsystem to
 * access system and Agent configuration.
 */
public interface IConfigurationAccess {

   /**
    * Returns the configuration value using the given <code>id</code> without
    * creating any module or global namespace qualifiers. This method will
    * return the value from the active configuration if that value is
    * identified exactly by the given <code>id</code>. If no value is found,
    * the supplied <code>defaultValue</code> is returned.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the client SDK configuration value associated with the given
    *         <code>id</code> or <code>defaultValue</code> if no
    *         configuration entry is found.
    * 
    * @throws <code>IllegalArgumentException</code> if the given <code>id</code>
    *             belongs to the Agent configuration namespace.
    */
    public String getSystemConfiguration(String id, String defaultValue);

   /**
    * Returns the configuration value using the given <code>id</code> without
    * creating any module or global namespace qualifiers. This method will
    * return the value from the active configuration if that value is
    * identified exactly by the given <code>id</code>.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the client SDK configuration value associated with the given
    *         <code>id</code> or <code>null</code> if no configuration
    *         entry is found.
    * 
    * @throws <code>IllegalArgumentException</code> if the given <code>id</code>
    *             belongs to the Agent's configuration namespace.
    */
    public String getSystemConfiguration(String id);

   /**
    * Returns a <code>java.util.Map<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or an empty 
    * <code>java.util.Map</code> if not available.
    */
    public Map getConfigurationMap(String id);

   /**
    * Returns a <code>String[]<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or an empty 
    * <code>String[]</code> if not available.
    */
    public String[] getConfigurationStrings(String id);

   /**
    * Returns a <code>String<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value. If 
    * still no value is found, the supplied <code>defaultValue</code> is 
    * returned.
    * 
    * @param id for which the configuration value must be looked up.
    * @param defaultValue the default value to be used in case no configuration
    * entry is found.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or 
    * <code>defaultValue</code> if not available.
    * 
    * @see #getConfiguration(String, String)
    */
    public String getConfigurationString(String id, String defaultValue);

   /**
    * Returns a <code>String<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or <code>0</code> 
    * if not available.
    * 
    * @see #getConfiguration(String)
    */
    public String getConfigurationString(String id);

   /**
    * Returns a <code>long<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value. If 
    * still no value is found, the supplied <code>defaultValue</code> is 
    * returned.
    * 
    * @param id for which the configuration value must be looked up.
    * @param defaultValue the default value to be used in case no configuration
    * entry is found.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or 
    * <code>defaultValue</code> if not available.
    */
    public long getConfigurationLong(String id, long defaultValue);

   /**
    * Returns a <code>long<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or <code>0</code> 
    * if not available.
    */
    public long getConfigurationLong(String id);

   /**
    * Returns a <code>int<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value. If 
    * still no value is found, the supplied <code>defaultValue</code> is 
    * returned.
    * 
    * @param id for which the configuration value must be looked up.
    * @param defaultValue the default value to be used in case no configuration
    * entry is found.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or 
    * <code>defaultValue</code> if not available.
    */
    public int getConfigurationInt(String id, int defaultValue);

   /**
    * Returns a <code>int<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or <code>0</code> 
    * if not available.
    */
    public int getConfigurationInt(String id);

   /**
    * Returns a <code>boolean<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value. If 
    * still no value is found, the supplied <code>defaultValue</code> is 
    * returned.
    * 
    * @param id for which the configuration value must be looked up.
    * @param defaultValue the default value to be used in case no configuration
    * entry is found.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or <code>false</code> 
    * if not available.
    */
    public boolean getConfigurationBoolean(String id, boolean defaultValue);

   /**
    * Returns a <code>boolean<code> value associated with the given
    * <code>id</code> in the <code>Module</code>'s namespace. If no such value
    * is found, the global namespace is queried for an associated value.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the configuration value associated with the given <code>id</code>
    * in the <code>Module</code> or global namespace, or <code>false</code> 
    * if not available.
    */
    public boolean getConfigurationBoolean(String id);

   /**
    * Returns the configuration value associated with the given <code>id</code>
    * in the <code>Module</code>'s configuration namespace. If no such value
    * is found, the global namespace is queried for an associated value. If
    * still no value is found, the supplied <code>defaultValue</code> is
    * returned.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @param defaultValue
    *            the default value to be used in case no configuration entry is
    *            found.
    * 
    * @return the configuration value associated with the given <code>id</code>
    *         in the <code>Module</code> or global namespace, or
    *         <code>defaultValue</code> if not available.
    */
    public String getConfiguration(String id, String defaultValue);

   /**
    * Returns the configuration value associated with the given <code>id</code>
    * in the <code>Module</code>'s configuration namespace. If no such value
    * is found, the global namespace is queried for an associated value.
    * 
    * @param id for which the configuration value must be looked up.
    * 
    * @return the configuration value associated with the given <code>id</code>
    *         in the <code>Module</code> or global namespace, or
    *         <code>null</code> if not available.
    */
    public String getConfiguration(String id);

    /**
     * Return a map of domain - conditional login URLs. The format of the
     * conditional URL property is as follows:
     * <pre>
     * a.b.c|http://a.b.c/openam/UI/Login,http://a2.b.c/openam/UI/Login
     * a.b.c|http://e.d.f/openam/cdcservlet
     * </pre>
     * So the domain is separated by <code>|</code> from the login URLs, and the
     * URLs are using <code>,</code> as delimiter.
     *
     * @param id The parameter name for the given conditional URL property
     * @return a Map of domain - ordered list of login URLs
     */
    public Map<String, Set<String>> getParsedConditionalUrls(String id);
}
