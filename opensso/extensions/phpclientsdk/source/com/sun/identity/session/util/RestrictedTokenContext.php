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
 * $Id: RestrictedTokenContext.php,v 1.1 2007/03/09 21:13:18 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * Utility to attach the context for token restriction checking to the current
 * thread and marshalling/unmarshalling context value
 */
class RestrictedTokenContext {

	/* The Object prefix*/
	const OBJECT_PREFIX = "object:";

	/*The Token Prefix*/
	const TOKEN_PREFIX = "token:";

	// TODO: useful??
	public static function getCurrent() {
		return gethostbyname("127.0.0.1");
	}

	/**
	* Serialize the current context to a string
	*
	* @param context
	*            to be serialized
	* @return string containing the serialized object
	* @throws Exception if the there was an error.
	*/
	public static function marshal($context) {
		if ($context instanceof SSOToken)
			return RestrictedTokenContext :: TOKEN_PREFIX . $context->getTokenID()->__toString();

		return RestrictedTokenContext :: OBJECT_PREFIX . serialize($context);
	}

}
?>
