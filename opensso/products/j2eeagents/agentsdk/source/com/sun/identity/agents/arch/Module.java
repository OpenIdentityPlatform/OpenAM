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
 * $Id: Module.java,v 1.3 2008/06/25 05:51:36 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

import java.util.Locale;
import java.util.ResourceBundle;

import com.sun.identity.shared.debug.Debug;

/**
 * <p>
 * A <code>Module</code> is the root of a logical subsystem of classes, 
 * together which provide a set of services that are orthogonal to those 
 * provided by other <code>Module</code>s. Any such subsystem of classes is
 * expected to use a single <code>Module</code> instance throughout the life
 * time of all classes within itself. An instance of <code>Module</code> is
 * associated with a unique <code>id</code>, a unique <code>resource</code> and
 * a unique <code>offset</code>. The unique <code>resource</code> is used as
 * a configuration namespace qualifier that provides uniquely visible 
 * configuration to the subsystem if needed. This <code>resource</code> is also
 * used as the Debug name in case the subsystem represented by this module
 * must record debug messages, warnings and errors.
 * </p><p>
 * The correct way to use this class is to create static wrappers which define
 * the <code>init()</code> methods and register the <code>Module</code> instance
 * with the <code>ModuleList</code> class.
 * </p>
 */
public class Module implements IConfigurationKeyConstants, 
        IConfigurationDefaultValueConstants, IModuleAccess
{    
    
   /**
    * Returns a <code>IModuleAccess</code> instance that allows the caller
    * to obtain <code>Module</code> specific services without being directly
    * in possession of the underlying <code>Module</code> instance.
    * 
    * @return a <code>IModuleAccess</code> instance
    */
    public IModuleAccess newModuleAccess() {
        return new ModuleAccess(this);
    }
    
   /**
    * Returns the String representation of this <code>Module</code>.
    * 
    * @return the String representation of this <code>Module</code>.
    */
    public String toString() {
        return("Module " + getModuleFixedName() + " (code: " + getModuleCode()
               + ", stringId: " + getModuleStringId() + ") ");
    }    
    
   
   /* (non-Javadoc)
    * @see com.sun.identity.agents.arch.IModuleAccess#makeLocalizableString(int)
    */
    public LocalizedMessage makeLocalizableString(int id) {
        return new LocalizedMessage(getModuleCode(), id);
    }

   /* (non-Javadoc)
    * @see IModuleAccess#makeLocalizableString(int, java.lang.Object[])
    */
    public LocalizedMessage makeLocalizableString(int id, Object[] params) {
        return new LocalizedMessage(getModuleCode(), id, params);
    }
    
   /* (non-Javadoc)
    * @see com.sun.identity.agents.arch.IModuleAccess#getResource(int)
    */
    public String getResource(int id) {
        return getResource(new Integer(id));
    }

   
   /* (non-Javadoc)
    * @see IModuleAccess#getResource(int, java.util.Locale)
    */
    public String getResource(int id, Locale locale) {
        return getResource(new Integer(id), locale);
    }    
        
   /**
    * Tests for equality of this <code>Module</code> instance with the given
    * <code>Object</code>. Returns <code>true</code> if and only if the
    * given <code>Object</code> is an instance of <code>Module</code> with
    * the same module code as this <code>Module</code> instance.
    * 
    * @param object the object to be compared against.
    * 
    * @return true if the given <code>Object</code> is an instance of 
    * <code>Module</code> with the same module code as this instance.
    */
    public boolean equals(Object object) {
        boolean result = false;
        if (object != null && (object instanceof Module)) {
            result = (getModuleCode() == ((Module) object).getModuleCode());
        }

        return result;
    }    

   /**
    * Creates a <code>Module</code> instance and initializes the associated
    * localization and debug resources.
    * 
    * @param code the module code to be used for creating this module.
    * @param moduleStringId the string identifier for the localized module name
    * to be used.
    * @param fixedName the fixed name of the module to be used for finding or
    * creating the associated localization, configuration and debug resources.
    */
    public Module(byte code, int moduleStringId, String fixedName) {

        setModuleCode(code);
        setModuleStringId(moduleStringId);
        setModuleFixedName(fixedName);
        setDebug(Debug.getInstance(fixedName));
        setModuleLocale(fixedName);
        setModuleResourceBundle(ResourceBundle.getBundle(fixedName, 
                getModuleLocale()));
    }
    
   /**
    * Returns the module code associated with this instance of a 
    * <code>Module</code>.
    * 
    * @return the module code.
    */
    byte getModuleCode() {
        return _moduleCode;
    }
    
   /**
    * Returns the fixed name associated with this instance of 
    * <code>Module</code>.
    * 
    * @return the fixed name of this <code>Module</code>.
    */
    String getModuleFixedName() {
        return _moduleFixedName;
    }    
    
   /* (non-Javadoc)
    * @see com.sun.identity.agents.arch.IDebugAccess#isLogMessageEnabled()
    */
    public boolean isLogMessageEnabled() {
        return getDebug().messageEnabled();
    }
 
   /* (non-Javadoc)
    * @see IDebugAccess#logMessage(java.lang.String)
    */
    public void logMessage(String msg) {
        getDebug().message(msg);
    }
    
   /* (non-Javadoc)
    * @see IDebugAccess#logMessage(java.lang.String, java.lang.Throwable)
    */
    public void logMessage(String msg, Throwable th) {
        getDebug().message(msg, th);
    }
    
   /* (non-Javadoc)
    * @see IDebugAccess#isLogWarningEnabled()
    */
    public boolean isLogWarningEnabled() {
        return getDebug().warningEnabled();
    }
    
   /* (non-Javadoc)
    * @see IDebugAccess#logWarning(java.lang.String)
    */
    public void logWarning(String msg) {
        getDebug().warning(msg);
    }
    
   /* (non-Javadoc)
    * @see IDebugAccess#logWarning(java.lang.String, java.lang.Throwable)
    */
    public void logWarning(String msg, Throwable th) {
        getDebug().warning(msg, th);
    }
    
   /* (non-Javadoc)
    * @see IDebugAccess#logError(java.lang.String)
    */
    public void logError(String msg) {
        getDebug().error(msg);
    }
    
   /* (non-Javadoc)
    * @see IDebugAccess#logError(java.lang.String, java.lang.Throwable)
    */
    public void logError(String msg, Throwable th) {
        getDebug().error(msg, th);
    }    
    
   /**
    * Returns the <code>Locale</code> of this <code>Module</code>.
    * @return the <code>Locale</code> of this <code>Module</code>.
    */
    public Locale getModuleLocale() {
        return _moduleLocale;
    }    
    
    /**
     * Returns the <code>com.iplanet.am.util.Debug</code> instance associated
     * with this <code>Module</code>. 
     * 
     * @return the <code>com.iplanet.am.util.Debug</code> instance associated 
     * with this <code>Module</code>.
     */
     Debug getDebug() {
         return _debug;
     }    
     
     private String getResource(Object id) {
         if (isLogMessageEnabled()) {
             logMessage("Module: getResource for id: " + id);
         }

         return getModuleResourceBundle().getString(id.toString());
     }

     private String getResource(Object id, Locale locale) {
         if(isLogMessageEnabled()) {
             logMessage("getResource: id = " + id + ", locale = "
                     + locale);
         }

         return ResourceBundle.getBundle(getModuleFixedName(),
                                        locale).getString(id.toString());
     }    
       
     private ResourceBundle getModuleResourceBundle() {
         return _moduleResourceBundle;
     }
    
     private void setModuleResourceBundle(ResourceBundle moduleResourceBundle) {
         _moduleResourceBundle = moduleResourceBundle;
     }
    
     private void setModuleLocale(String fixedName) {
        
         String languageConfigKey = AGENT_CONFIG_PREFIX  + fixedName 
                                    + "." + CONFIG_SUBKEY_LOCALE_LANG;
            
         String languageGlobalConfigKey = CONFIG_LOCALE_LANG;
        
         String countryConfigKey = AGENT_CONFIG_PREFIX  + fixedName 
                                   + "." + CONFIG_SUBKEY_LOCALE_COUNTRY;
        
         String coungryGlobalConfigKey = CONFIG_LOCALE_COUNTRY;
        
         String languageConfigValue =
            AgentConfiguration.getProperty(languageConfigKey);

         if((languageConfigValue == null)
                || (languageConfigValue.trim().length() == 0)) {
             if(getDebug().messageEnabled()) {
                 getDebug().message("No configuration found for "
                                   + languageConfigKey + ", trying "
                                   + languageGlobalConfigKey);
             }

             languageConfigValue =
                AgentConfiguration.getProperty(languageGlobalConfigKey);

             if((languageConfigValue == null)
                    || (languageConfigValue.trim().length() == 0)) {
                 if(getDebug().messageEnabled()) {
                     getDebug().message("No configuration found for "
                                       + languageGlobalConfigKey
                                       + ", using default value: "
                                       + DEFAULT_LOCALE_LANG);
                 }

                 languageConfigValue = DEFAULT_LOCALE_LANG;
             }
         }

         String countryConfigValue = 
            AgentConfiguration.getProperty(countryConfigKey);

         if((countryConfigValue == null)
                || (countryConfigValue.trim().length() == 0)) {
             if(getDebug().messageEnabled()) {
                 getDebug().message("No configuration found for "
                                   + countryConfigKey + ", trying "
                                   + coungryGlobalConfigKey);
             }

             countryConfigValue =
                 AgentConfiguration.getProperty(coungryGlobalConfigKey);

             if((countryConfigValue == null)
                    || (countryConfigValue.trim().length() == 0)) {
                 if(getDebug().messageEnabled()) {
                     getDebug().message("No configuration found for "
                                       + coungryGlobalConfigKey
                                       + ", using default value: "
                                       + DEFAULT_LOCALE_COUNTRY);
                 }

                 countryConfigValue = DEFAULT_LOCALE_COUNTRY;
             }
         }

         _moduleLocale = new Locale(languageConfigValue, countryConfigValue);
     }
    
    
     private void setDebug(Debug debug) {
         _debug = debug;
     }
        
     private void setModuleFixedName(String moduleFixedName) {
         _moduleFixedName = moduleFixedName;
     }
    
     private int getModuleStringId() {
         return _moduleStringId;
     }
    
     private void setModuleStringId(int moduleStringId) {
         _moduleStringId = moduleStringId;
     }
    
     private void setModuleCode(byte moduleCode) {
         _moduleCode = moduleCode;
     }
    
     private byte _moduleCode;
     private int _moduleStringId;
     private String _moduleFixedName;
     private Debug _debug;
     private Locale _moduleLocale;
     private ResourceBundle _moduleResourceBundle;
     
     static {
         AgentConfiguration.initialize();
     }
}
