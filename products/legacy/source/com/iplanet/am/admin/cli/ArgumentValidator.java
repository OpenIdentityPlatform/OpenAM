/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ArgumentValidator.java,v 1.2 2008/06/25 05:52:24 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;


/**
 * This class provides methods to do argument validation in command line
 * interface.
 */
abstract class ArgumentValidator {

    static Set nonOperatorUnaryOperators = new HashSet(5);
    static Set debugUnaryOperators = new HashSet(4);
    static Set nonOperatorBinaryOperators = new HashSet(4);

    static {
        nonOperatorUnaryOperators.add("--help");
        nonOperatorUnaryOperators.add("-h");
        nonOperatorUnaryOperators.add("--version");
        nonOperatorUnaryOperators.add("-n");
        nonOperatorUnaryOperators.add("--cleanpolicyrules");

        debugUnaryOperators.add("--verbose");
        debugUnaryOperators.add("-v");
        debugUnaryOperators.add("--debug");
        debugUnaryOperators.add("-d");

        nonOperatorBinaryOperators.add("--runasdn");
        nonOperatorBinaryOperators.add("-u");
        nonOperatorBinaryOperators.add("--password");
        nonOperatorBinaryOperators.add("-w");
        nonOperatorBinaryOperators.add("--passwordfile");
        nonOperatorBinaryOperators.add("-f");
    }

    /**
     * Return true if arguments are valid.
     *
     * @param argv Array of arguments.
     * @param bundle Resource Bundle.
     * @return true if arguments are valid.
     */
    static boolean validateArguments(String[] argv, ResourceBundle bundle) {
        boolean error = false;
        int len = argv.length;

        if (len == 0) {
            error = true;
        } else if (len == 1) {
            String arg = argv[0];

            if (!nonOperatorUnaryOperators.contains(arg.toLowerCase())) {
                System.err.println(bundle.getString("invopt") + arg);
                error = true;
            }
        } else {
            if (hasMandatoryArguments(argv, bundle)) {
                if (!hasOperator(argv)) {
                    System.err.println(bundle.getString("nodataschemawarning"));
                    error = true;
                }
            } else {
                error = true;
            }
        }

        return !error;
    }

    /**
     * Return true if arguments have the mandatory arguments.
     *
     * @param argv Array of arguments.
     * @return true if arguments have the mandatory arguments.
     */
    private static boolean hasMandatoryArguments(
        String[] argv, ResourceBundle bundle
    ) {
        boolean hasUserDN = false;
        boolean hasPassword = false;
        int len = argv.length;

        for (int i = 0; (i < (len -1)) && (!hasUserDN || !hasPassword); i++) {
            String arg = argv[i].toLowerCase();

            if (!hasUserDN) {
                if (arg.equals("--runasdn") || arg.equals("-u")) {
                    if (argv[i+1].charAt(0) != '-') {
                        hasUserDN = true;
                        i++;
                    }
                }
            }

            if (!hasPassword) {
                if (arg.equals("--password") || arg.equals("-w") ||
                    arg.equals("--passwordfile") || arg.equals("-f")
                ) {
                    if (argv[i+1].charAt(0) != '-') {
                        hasPassword = true;
                        i++;
                    }
                }
            }
        }

        if (!hasUserDN) {
            System.err.println(bundle.getString("nodnforadmin"));
        }
        if (!hasPassword) {
            System.err.println(bundle.getString("nopwdforadmin"));
        }

        return hasUserDN && hasPassword;
    }

    /**
     * Returns true of arguments contain operator.
     *
     * @param argv Array of arguments.
     * @return true of arguments contain operator.
     */
    private static boolean hasOperator(String[] argv) {
        int len = argv.length;
        List arguments = new ArrayList(len);

        for (int i = 0; i < len; i++) {
            arguments.add(argv[i].toLowerCase());
        }

        for (Iterator iter = arguments.iterator(); iter.hasNext(); ) {
            String arg = (String)iter.next();

            if (debugUnaryOperators.contains(arg)) {
                iter.remove();
            } else if (nonOperatorBinaryOperators.contains(arg)) {
                iter.remove();

                if (iter.hasNext()) {
                    iter.next();
                    iter.remove();
                }
            }
        }

        return !arguments.isEmpty();
    }
}
