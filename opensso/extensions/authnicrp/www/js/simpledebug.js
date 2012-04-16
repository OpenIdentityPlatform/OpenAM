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
 * $Id: simpledebug.js,v 1.1 2009/07/08 08:59:29 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */


function log (message)
{
	if (! _log_timeout)
		_log_timeout = window.setTimeout(dump_log, 1000);
	
	_log_messages.push(message);


	function dump_log()
	{
		var message = '';
		
		for (var i = 0; i < _log_messages.length; i++)
			message += _log_messages[i] + '\n';
				
		alert(message);
		
		_log_timeout = null;
		delete _log_messages;
		_log_messages = new Array();
	}
}


function inspect (obj)
{
	var message = 'Object possesses these properties:\n';
	
	if (obj)
	{
		for (var i in obj)
		{
			if ((obj[i] instanceof Function) || (obj[i] == null) ||
					(i.toUpperCase() == i))
				continue;
			
			message += i + ', ';
		}
		
		message = message.substr(0, message.length - 2);
	}
	else
		message = 'Object is null';
	
	log(message);
}

function inspectValues (obj)
{
	var message = '';
	
	if (obj)
		for (var i in obj)
		{
			if ((obj[i] instanceof Function) || (obj[i] == null) ||
					(i.toUpperCase() == i))
				continue;
			
			message += i + ': ' + obj[i] + '\n';
		}
	else
		message = 'Object is null';
	
	log(message);
}

var _log_timeout;
var _log_messages = new Array();
