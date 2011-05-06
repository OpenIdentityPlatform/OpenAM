<?php
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
 * $Id: SessionException.php,v 1.1 2007/03/09 21:13:08 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * A <code>SessionException</code> is thrown if the Naming Service can not
 * find a URL for the session service.
 */
class SessionException extends Exception {

	/**
	 * Constructs an instance of the <code>SessionException</code> class.
	 *
	 * @param msg
	 *            The message provided by the object which is throwing the
	 *            exception
	 */
	public function __construct($msg) {
		parent :: __construct($msg, 0);
	}

}
?>
