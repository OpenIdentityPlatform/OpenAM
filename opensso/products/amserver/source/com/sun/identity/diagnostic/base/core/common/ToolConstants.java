/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ToolConstants.java,v 1.2 2009/07/24 22:01:21 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.common;

import java.util.ResourceBundle;

/**
 * This defines the constants used by the entire tool.
 */
public interface ToolConstants {

  /**
   * Tool Debug name.
   */
   public String DEBUG_NAME = "amDiagnosticTool";

  /**
   * Tool resourcebundle name.
   */
   public String RESOURCE_BUNDLE_NAME = 
       "com.sun.identity.diagnostic.base.resources.locale.DiagnosticTool";

  /**
   * Tool deafult log file name.
   */
   public String DEF_LOGFILE_NAME = "DiagnosticTool.log";

  /**
   * Tool config properties name.
   */
   public String TOOL_PROPERTIES = "DTConfig.properties";

  /**
   * Name of disable logging argument/option.
   */
   public String PROPERTY_LOG_ENABLED = "odt.log.enabled";

  /**
   * Name of logging level.
   */
   public String PROPERTY_LOG_LEVEL = "odt.log.level";

  /**
   * Name of log file name.
   */
   public String PROPERTY_LOG_FILENAME = "odt.log.filename";

  /**
   * Base directory for the tool.
   */
   public String TOOL_BASE_DIR = "odt.application.home";

  /**
   * Specified run mode for the tool.
   */
   public String TOOL_RUN_MODE = "odt.application.runmode";

   /**
    * Holds the folder name of the default service repository. This
    * is defined relative to TOOL_HOME.
    */
   public final String SERVICES_REPOSITORY_FOLDER = "services";

   /**
    * Constant that represents a GUI run mode.
    */
   public final String GUI_MODE = "GUI";

   /**
    * Constant that represents CLI run mode.
    */
   public final String CLI_MODE = "CLI";

   /**
    * Name of configuration directory.
    */
   public String CONFIG_DIR = "cfgPath" ;

   /**
    * Name of container base directory.
    */
   public String CONTAINER_DIR = "containerPath";

   /**
    * Type of container.
    */
   public String CONTAINER_TYPE = "containerType";

   /**
    * Name of container domain directory.
    */
   public String CONTAINER_DOMAIN_DIR = "containerDomainPath";

   /**
    * Name of file to save.
    */
   public String SAVE_FILE_NAME = "savePathName";

   /**
    * Name of supported web containers.
    */
   public static final String[] WEB_CONTAINERS = {
       ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME).getString("dd_sun_app_server"),
       ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME).getString("dd_sun_web_server"),
       ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME).getString("dd_bea_weblogic"),
       ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME).getString("dd_ibm_websphere")};


   /**
    * Locations of web containers' icon.
    */
   public static final String[] WEB_CONTAINERS_ICON = {
       "/com/sun/identity/diagnostic/base/core/ui/gui/images/GlassfishIcon.gif",
       "/com/sun/identity/diagnostic/base/core/ui/gui/images/SunIcon.gif",
       "/com/sun/identity/diagnostic/base/core/ui/gui/images/BEAIcon.gif",
       "/com/sun/identity/diagnostic/base/core/ui/gui/images/IBMIcon.gif"};
}

