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
 * $Id: CLIException.java,v 1.4 2009/11/10 19:01:05 veiming Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.cli;

import com.sun.identity.shared.locale.L10NMessage;
import java.util.Locale;

/**
 * Commandline Interface Exception.
 */
public class CLIException extends Exception {
    private int exitCode = 0;
    private String subcommandName;

    public CLIException(String string, String[] param,
        int REQUEST_CANNOT_BE_PROCESSED) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Constructs a CLI Exception.
     *
     * @param message Exception message.
     * @param exitCode Exit code.
     * @param subcommandName Sub Command Name.
     */
    public CLIException(String message, int exitCode, String subcommandName) {
        super(message);
        this.exitCode = exitCode;
        this.subcommandName = subcommandName;
    }

    /**
     * Constructs a CLI Exception.
     *
     * @param message Exception message.
     * @param exitCode Exit code.
     */
    public CLIException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * Constructs a CLI Exception.
     *
     * @param cause Throwable object.
     * @param exitCode Exit code.
     */
    public CLIException(Throwable cause, int exitCode) {
        super(cause);
        this.exitCode = exitCode;
    }

    /**
     * Constructs a CLI Exception.
     *
     * @param cause Throwable object.
     * @param exitCode Exit code.
     * @param subcommandName Sub Command Name.
     */
    public CLIException(Throwable cause, int exitCode, String subcommandName) {
        super(cause);
        this.exitCode = exitCode;
        this.subcommandName = subcommandName;
    }

    /**
     * Returns exit code.
     *
     * @return exit code.
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Returns sub command name.
     *
     * @return sub command name.
     */
    public String getSubcommandName() {
        return subcommandName;
    }

    /**
     * Returns localized message.
     *
     * @param locale Locale.
     * @return localized message.
     */
    public String getL10NMessage(Locale locale) {
        if ((getCause() != null) && (getCause() instanceof L10NMessage)) {
            return ((L10NMessage)getCause()).getL10NMessage(locale);
        }
        return getMessage();
    }
}
