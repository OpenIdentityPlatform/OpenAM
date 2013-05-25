/*
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
 * $Id: FedletLogger.cs,v 1.1 2009/06/11 18:37:59 ggennaro Exp $
 */

using System.Configuration;
using System.Diagnostics;

namespace Sun.Identity.Common
{
    /// <summary>
    /// Simple class for logging events to the Windows Application Log. The
    /// &lt;appSettings/&gt; section of the Web.config would be the place to
    /// specify the logging level (either ERROR, WARNING, or INFO).  An
    /// example Web.config file would have the following:
    /// <para>
    ///     &lt;appSettings&gt;
    ///         &lt;add key="fedletLogLevel" value="info" /&gt;
    ///     &lt;/appSettings&gt;
    /// </para>
    /// </summary>
    public static class FedletLogger
    {
        #region Members
        /// <summary>
        /// Parameter key in the &lt;appSettings/&gt; section of the 
        /// Web.config file of the desired .NET application for specifying 
        /// the log level. 
        /// </summary>
        public const string AppSettingParameter = "fedletLogLevel";

        /// <summary>
        /// Constant for the ERROR log level.
        /// </summary>
        public const string LogLevelError = "ERROR";

        /// <summary>
        /// Constant for the INFO log level.
        /// </summary>
        public const string LogLevelInfo = "INFO";

        /// <summary>
        /// Constant for the WARNING log level.
        /// </summary>
        public const string LogLevelWarning = "WARNING";

        /// <summary>
        /// Constant that specifies the Windows event log to use, in this
        /// case, the Application log.
        /// </summary>
        public const string Log = "Application";

        /// <summary>
        /// Constant that specifies the source of the log entry, in this
        /// case, the Fedlet.
        /// </summary>
        public const string LogSource = "Fedlet";
        #endregion

        #region Methods
        /// <summary>
        /// Method to write an error message to the event log.
        /// </summary>
        /// <param name="message">Message to be written.</param>
        public static void Error(string message)
        {
            FedletLogger.LogMessage(message, EventLogEntryType.Error);
        }

        /// <summary>
        /// Method to write an information message to the event log.
        /// </summary>
        /// <param name="message">Message to be written.</param>
        public static void Info(string message)
        {
            FedletLogger.LogMessage(message, EventLogEntryType.Information);
        }

        /// <summary>
        /// Method to write a warning message to the event log.
        /// </summary>
        /// <param name="message">Message to be written.</param>
        public static void Warning(string message)
        {
            FedletLogger.LogMessage(message, EventLogEntryType.Warning);
        }

        /// <summary>
        /// Method to write a message with the given entry type.  Currently
        /// only Info, Warning, and Error are supported from the default
        /// messages available from the framework.
        /// </summary>
        /// <see cref="System.Diagnostics.EventLogEntryType"/>
        /// <param name="message">Message to be written.</param>
        /// <param name="entryType">
        /// EventLogEntryType to associate with message.
        /// </param>
        private static void LogMessage(string message, EventLogEntryType entryType)
        {
            string logLevel = ConfigurationManager.AppSettings[FedletLogger.AppSettingParameter];

            if (!string.IsNullOrEmpty(logLevel))
            {
                logLevel = logLevel.ToUpperInvariant();

                if ((logLevel == FedletLogger.LogLevelError && entryType == EventLogEntryType.Error)
                    || (logLevel == FedletLogger.LogLevelWarning && entryType <= EventLogEntryType.Warning)
                    || (logLevel == FedletLogger.LogLevelInfo && entryType <= EventLogEntryType.Information))
                {
                    EventLog.WriteEntry(FedletLogger.LogSource, message, entryType);
                }
            }
        }
        #endregion
    }
}
