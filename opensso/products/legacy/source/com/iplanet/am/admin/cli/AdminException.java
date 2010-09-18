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
 * $Id: AdminException.java,v 1.2 2008/06/25 05:52:23 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;

/**
 * A <code>AdminException</code> is thrown when there are errors related to
 * amadmin operations.
 */

public class AdminException     
    extends Exception
{
    private boolean isFatal = false;
    
    /**
     * Constructs an instance of the <code>AdminException</code> class.
     * @param msg The message provided by the object which is throwing the
     * exception
     */
    public AdminException(java.lang.String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of the <code>AdminException</code> class.
     * @param The Throwable object provided by the object which is throwing 
     *  the exception
     */
    public AdminException(java.lang.Throwable t) { 
        super(t.getMessage());

        if (t instanceof AMException) {
            AMException ame = (AMException)t;

            try {
                int errCode = Integer.parseInt(ame.getErrorCode());

                switch (errCode) {
                    case 80:
                    case 81:
                    case 91:
                    case 51:
                        isFatal = true;
                        break;
                    default:
                        isFatal = false;
                        break;
                }
            } catch (NumberFormatException nfe) {
                // ignore this error code if it is not a number.
            }
        }
    }

    /**
     * Returns true if exception is fatal.
     *
     * @return true if exception is fatal.
     */
    public boolean isFatal() {
        return isFatal;
    }
}
