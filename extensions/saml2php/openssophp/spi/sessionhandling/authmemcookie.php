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
 * $Id $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

/* Stores the session data to avoid loading it from the server every time
 * we need it.
 */
$AuthMemCookie_Session = NULL;

/* Gets a configuration option for this session handler
 *
 * Parameter: $name      The name of the option
 * Parameter: $default   The default value for the option
 *
 * Returns:              The value of the option or $default if the
                         option is unset.
 */
function AuthMemCookie_getOption($name, $default) {
  global $LIGHTBULB_CONFIG;

  if(!isset($LIGHTBULB_CONFIG['authmemcookie'])) {
    return $default;
  }

  if(!isset($LIGHTBULB_CONFIG['authmemcookie'][$name])) {
    return $default;
  }

  return $LIGHTBULB_CONFIG['authmemcookie'][$name];
}


/* Gets the current instance of the Memcache object.
 * Creates a new Memcache object and initializes it if we haven't done
 * that yet.
 *
 * Returns:              A properly initialized Memcache object.
 */
function AuthMemCookie_getMemCache() {
  static $memcache;

  /* Return now if we already have a Memcache object. */
  if($memcache) {
    return $memcache;
  }


  $memcache = new Memcache;

  /* In the config we store memcache servers as a string of host:port-pairs
   * separated by ','. The port number is optional if the server runs at the
   * default port (11211).
   * Example: 'localhost:23122,remote1,10.0.0.71:43232'
   */
  $servers = AuthMemCookie_getOption('memcache_servers', '127.0.0.1');

  foreach(explode(',', $servers) as $server) {
    $hostport = explode(':', $server);
    $host = $hostport[0];
    $port = (int)$hostport[1];
    if(!$port) {
      $port = 11211;
    }

    /* Add server to pool. Sets a weight of 1 and a 10-second timeout. */
    $memcache->addServer($host, $port, TRUE, 1, 10);
  }

  return $memcache;
}


/* Creates a new session id.
 *
 * Returns:              A unique session id made up of 32 hexadecimal
 *                       characters.
 */
function AuthMemCookie_createSessionId()
{
  $memcache = AuthMemCookie_getMemCache();

  /* Generate 32-byte unique session id. */
  do {
    $pattern = "1234567890abcdef";
    $sessionId = $pattern{rand(0,16)};
    for($i=1; $i<32; $i++) {
      $sessionId .= $pattern{rand(0,16)};
    }
    /* Make sure that the session id is unique.
     * $memcache->add(...) should fail if the session id already
     * exists in memcache.
     */
  } while(!$memcache->add($sessionId, '', 0, 30));

  return $sessionId;
}

/* Gets the current session id.
 *
 * Parameter: $create    Create a new session id if we don't have a session.
 * Returns:              The current session id.
 */
function AuthMemCookie_getSessionId($create) {
  static $sessionId;

  if($sessionId) {
    /* If we already have a session id we can return it without more
     * processing. */
    return $sessionId;
  }

  $sessionId =  $_COOKIE[AuthMemCookie_getOption('cookie_name',
						 'AuthMemCookie')];

  if(!$sessionId) {
    /* The cookie is unset. */
    if(!$create) {
      /* Return NULL since we shouldn't create a new session. */
      return NULL;
    }

    /* Create a new session. */
    $sessionId = AuthMemCookie_createSessionId();
    setcookie(AuthMemCookie_getOption('cookie_name', 'AuthMemCookie'),
	      $sessionId, 0,
	      AuthMemCookie_getOption('cookie_path', '/'),
	      AuthMemCookie_getOption('cookie_domain', ''));
  }

  return $sessionId;
}

/* This function parses session data stored in memcache.
 * The dataformat is '<key>=<value>\r\n'.
 * The value is urlencoded.
 *
 * Returns:              An associative array containing all the
 *                       key-value pairs.
 */
function AuthMemCookie_parseSessionData($sessionData) {
  $values = array();
  preg_match_all("/([^=\n\r]*)=([^\r\n]*)\r\n/", $sessionData,
		 $values, PREG_SET_ORDER);
  foreach($values as $kv) {
    $k = $kv[1];
    $v = $kv[2];

    /* The data is stored urlencoded. */
    $session[$k] = urldecode($v);
  }

  return $session;
}

/* Loads sessiondata from memcache.
 *
 * Parameter: $create    Create a new session if we don't have a session
 *                       already.
 *
 * Returns:              An associative array containing the session data.
 */
function AuthMemCookie_getSession($create)
{
  global $AuthMemCookie_Session;

  if($AuthMemCookie_Session) {
    /* Return the cached session data if it is cached. */
    return $AuthMemCookie_Session;
  }

  $sessionId = AuthMemCookie_getSessionId($create);
  if(!$sessionId) {
    /* We don't have a valid session. */
    return NULL;
  }

  $memcache = AuthMemCookie_getMemCache();
  $sessionData = $memcache->get($sessionId);

  if(!$sessionData) {
    /* Session does not exist in memcache. Should we create a new session? */
    if($create) {
      /* Create new session.
       * We add the RemoteIP-key here. This key is required by AutMemCookie
       * unless Auth_memCookie_MatchIP is set to 'no'.
       */
      $AuthMemCookie_Session = array('id' => $sessionId,
				     'RemoteIP' => $_SERVER['REMOTE_ADDR']);
      return $AuthMemCookie_Session;
    } else {
      /* No data from memcache - the session does'nt exist. */
      return NULL;
    }
  }

  $AuthMemCookie_Session = AuthMemCookie_parseSessionData($sessionData);
  $AuthMemCookie_Session['id'] = $sessionId;

  return $AuthMemCookie_Session;
}

/* Stores sessiondata in memcache.
 *
 * Parameter: $session   An associative array containing the session state.
 */
function AuthMemCookie_saveSession($session)
{
  global $AuthMemCookie_Session;

  /* Update $AuthMemCookie_Session to reflect changes to the session state. */
  $AuthMemCookie_Session = $session;

  $memcache = AuthMemCookie_getMemCache();

  $sessionData = '';

  /* We set UserName to the local UserID if the namemapping plugin
   * has set a local username. We set UserName to NameID (the name we get from
   * the IdP) if we dont have a local username.
   */
  if($session['UserID'] && $session['UserID'] != '') {
    $UserName = $session['UserID'];
  } else if($session['NameID'] && $session['NameID'] != '') {
    $UserName = $session['NameID'];
  } else {
    error_log("Saving session without a username because neither" .
	      " UserID or NameID is set");
    $UserName = NULL;
  }

  if($UserName) {
    $sessionData .= 'UserName=' . $UserName . "\r\n";
  }

  /* TODO: Groups-implementation?  */
  $sessionData .= 'Groups=' . "\r\n";

  /* We store all key-value pairs we get from the IdP in memcache.
   * The value will be urlencoded.
   */
  foreach($session as $k => $v) {
    if($k == 'id' || $k == 'Groups' || $k == 'UserName') {
      /* We ignore the 'id'-key because it is only used internally.
       * We also ignore the 'Groups' and 'UserName' keys, since they are
       * treated specially.
       */
      continue;
    }

    $sessionData .= $k . '=' . urlencode($v) . "\r\n";
  }

  /* Save the session data. */
  $memcache->set($session['id'], $sessionData, 0, 86400) or
    die('Failed to store session data in memcache.');
}

/* Deletes the current session. */
function AuthMemCookie_deleteSession() {
  global $AuthMemCookie_Session;

  $sessionId = AuthMemCookie_getSessionId(FALSE);
  if(!$sessionId) {
    /* No current session */
    return;
  }

  /* Delete session from memcache. */
  $memcache = AuthMemCookie_getMemCache();
  $memcache->delete($SessionId);

  /* Delete cookie */
  setcookie(AuthMemCookie_getOption('cookie_name', 'AuthMemCookie'),
	    NULL, 0,
	    AuthMemCookie_getOption('cookie_path', '/'),
	    AuthMemCookie_getOption('cookie_domain', ''));

  /* Remove cached session data. */
  $AuthMemCookie_Session = NULL;
}

/* Stores the UserID from the namemapping plugin in the session.
 * This field will be used as the UserName to Auth MemCookie if it is set.
 * If this field isn't set, then we will use the NameID as the UserName.
 *
 * Parameter: $userID    The UserID.
 */
function spi_sessionhandling_setUserID($userID)
{
  $session = AuthMemCookie_getSession(TRUE);
  $session['UserID'] = $userID;
  AuthMemCookie_saveSession($session);
}

/* Retrieves the UserID from the session.
 *
 * Returns:              The UserID or NULL if we don't have a session.
 */
function spi_sessionhandling_getUserID()
{
  $session = AuthMemCookie_getSession(FALSE);
  if(!$session) {
    error_log('spi_sessionhandling_getUserID() without a valid session.');
    return NULL;
  }

  return $session['UserID'];
}

/* Sets the NameID in the session. This is the name this user has on the IdP.
 * This field will be used as the UserName for Auth MemCookie if the
 * namemapping plugin haven't set a UserID.
 *
 * Parameter: $nameID    The NameID.
 */
function spi_sessionhandling_setNameID($nameID) {	
  $session = AuthMemCookie_getSession(TRUE);
  $session['NameID'] = $nameID;
  AuthMemCookie_saveSession($session);
}

/* Retrieves the NameID from the session.
 *
 * Returns:              The NameID or NULL if we don't have a session.
 */
function spi_sessionhandling_getNameID() {
  $session = AuthMemCookie_getSession(FALSE);
  if(!$session) {
    error_log('spi_sessionhandling_getNameID() without a valid session.');
    return NULL;
  }

  return $session['NameID'];
}

/* Deletes the current session. */
function spi_sessionhandling_clearUserId()
{
  AuthMemCookie_deleteSession();
}

/* Stores the response from the login in the session.
 * We also extract the attributes passed to us from the IdP.
 */
function spi_sessionhandling_setResponse($token)
{
  $session = AuthMemCookie_getSession(TRUE);
  $session['SamlResponse'] = $token->saveXML();

  /* Store attributes in the session for easy access through Auth MemCookie. */
  $attributes = getAttributes($token);
  foreach ($attributes as $key => $a) {
    $session[$key] = $a;
  }

  AuthMemCookie_saveSession($session);
}

/* Retrieves the response from the login operation from the session.
 *
 * Returns:              The response to the login operation, or NULL in case
 *                       of an error.
 */
function spi_sessionhandling_getResponse()
{
  $session = AuthMemCookie_getSession(FALSE);
  if(!$session) {
    error_log('spi_sessionhandling_getResponse() without a valid session.');
    return NULL;
  }

  if($session['SamlResponse']) {
    $token = new DOMDocument();
    $token->loadXML($session['SamlResponse']);
    return $token;
  } else {
    error_log('spi_sessionhandling_getResponse() without ' .
	      'SamlResponse in session.');

    return NULL;
  }
}

/* Checks if we have logged in.
 *
 * Returns:              TRUE if we have logged in, FALSE otherwise.
 */
function spi_sessionhandling_federatedLogin()
{
  $session = AuthMemCookie_getSession(FALSE);
  if(!$session) {
    return FALSE;
  }

  return isset($session['SamlResponse']);
}

?>
