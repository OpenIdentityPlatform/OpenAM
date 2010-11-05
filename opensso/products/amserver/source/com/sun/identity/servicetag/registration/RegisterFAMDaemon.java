/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.identity.servicetag.registration;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Entry point to start Registration Daemon thread.
 * @author Mitesh Meswani
 * 
 */
public class RegisterFAMDaemon {
    // The timer interval in ms defaults to 1HR 1hr*60mins*60sec*1000 ms
    private static final long TIMER_INTERVAL =
	Long.getLong("com.sun.identity.servicetag.registration.TIMER_INTERVAL",
	    24*60*60*1000);

    public static void start(final File serviceTagRegistry) {
        final SysnetRegistrationService regService =
	    new SysnetRegistrationService(serviceTagRegistry);
	final File finalFile = serviceTagRegistry;
	/*
         * Initiate the Daemon thread only if the repository file is writable.
         * Let the thread start even if there are no tags actually in
	 * NOT_TRANSFERRED becaue the check would do the same work as an
	 * attempt to transfer the tags - which is to open the file and
	 * enumerate the tags.
	 */

        if (regService.isRegistrationEnabled() ) {
	    /*
	     * Mark the timer as daemon so that it does not hold up
	     * appserver shutdown
	     */
            final Timer registrationTimer = new Timer("registration", true);
            TimerTask registrationTask = new TimerTask() {
                public void run() {
                    try {
                        regService.transferEligibleServiceTagsToSysNet();
                        // Transfer was succseeful cancel the timer thread
                        registrationTimer.cancel();
			synchronized (finalFile) {
			    finalFile.notify();
			}
                    } catch (RegistrationException e) {
                        //Log exception.  
		    }
                }
            };
            registrationTimer.schedule(registrationTask, 0L, TIMER_INTERVAL);
        }
    }
}

