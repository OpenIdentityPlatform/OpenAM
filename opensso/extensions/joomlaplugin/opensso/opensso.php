<?php
/**
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
 * $Id: opensso.php,v 1.1 2009/08/01 22:55:00 superpat7 Exp $
 *
 */
 
// no direct access
defined( '_JEXEC' ) or die( 'Restricted access' );

jimport( 'joomla.plugin.plugin' );
jimport( 'joomla.user.authentication' );
jimport( 'joomla.user.helper' );

/**
 * Example system plugin
 */
class plgSystemOpensso extends JPlugin
{
	protected $opensso_enabled;
	protected $opensso_cookie_name;
	protected $opensso_base_url;
	protected $opensso_joomla_username_attribute;
	protected $opensso_login_url;
	protected $opensso_logout_url;
	protected $opensso_is_token_valid;
	protected $opensso_attributes;
	
	/**
	 * Constructor
	 *
	 * For php4 compatibility we must not use the __constructor as a constructor for plugins
	 * because func_get_args ( void ) returns a copy of all passed arguments NOT references.
	 * This causes problems with cross-referencing necessary for the observer design pattern.
	 *
	 * @access	protected
	 * @param	object	$subject The object to observe
	 * @param 	array   $config  An array that holds the plugin configuration
	 * @since	1.0
	 */
	function plgSystemTest( &$subject, $config )
	{
		parent::__construct( $subject, $config );

		// load plugin parameters
		$this->_plugin =& JPluginHelper::getPlugin( 'system', 'opensso' );
		$this->_params = new JParameter( $this->_plugin->params );
	}

	/**
	 * Do something onAfterInitialise 
	 */
	function onAfterInitialise()
	{
		global $mainframe;
		
		$this->opensso_enabled = $this->params->get('opensso_enabled', '0');

		if (!$this->opensso_enabled) {
			return;
		}
		
		$this->opensso_cookie_name               = $this->params->get('opensso_cookie_name', 'iPlanetDirectoryPro');
		$this->opensso_base_url                  = $this->params->get('opensso_base_url', 'http://demo.example.com:8080/opensso/');
		$this->opensso_joomla_username_attribute = $this->params->get('opensso_joomla_username_attribute', 'uid');
		
		$this->opensso_login_url      = $this->opensso_base_url . 'UI/Login';
		$this->opensso_logout_url     = $this->opensso_base_url . 'UI/Logout';
		$this->opensso_is_token_valid = $this->opensso_base_url . 'identity/isTokenValid';
		$this->opensso_attributes     = $this->opensso_base_url . 'identity/attributes';
		
		// Quick hack to get round the fact that '+' often gets decoded to ' '
		$ssotoken = str_replace(' ', '+', $_COOKIE[$this->opensso_cookie_name]);
		
		$app = & JFactory::getApplication();
		
		$option = $_REQUEST['option'];

		// Is there a user logged in already?
		$user =& JFactory::getUser();
		if ( $user->guest ) {
			// Munge the login form
			$this->alterLoginForm();
		}

		if ( $option == 'com_user' || $option == 'com_login' ) {
			if ( $_REQUEST['task'] == 'logout' ) {
				if (!empty($ssotoken)) {
					// We're still logged in to OpenSSO
					// Redirect to OpenSSO logout - need to come back here to complete
					// Joomla logout process
					$url = $this->fullUrl();
					
					if ( $_SERVER['REQUEST_METHOD'] == 'POST' ) {
						$url .= "?option=$option&task=logout";
						$return = $_REQUEST['return'];
						if ( ! empty($return) ) {
							$url .= "&return=$return";
						}
					}
					
					$app->redirect($this->opensso_logout_url . '?goto=' . urlencode($url));
					exit();
				}
				// We're logged out from OpenSSO
				// Allow Joomla's logout processing to take place
				return;
			} else if ( $_REQUEST['task'] != 'login' ) {
				// Not logging in or out
				// Allow Joomla's normal processing to take place
				return;
			}
		} else {
			// Allow Joomla's logout processing to take place
			return;
		}

		// Is there an SSO token?
		if (empty($ssotoken)) {
			// Redirect to OpenSSO login page then return here
			$this->redirectToOpensso($option);
		}

		// Is the token valid?  
		switch ($this->isTokenValid($ssotoken)) {
			case 0:
				// Session expired
				$this->redirectToOpensso($option);
			case -1:
				// Error validating token
				// TODO - report error!
				return;
		}
		
		// OK - if we get here then we have a valid session cookie
		$name = $this->getJoomlaName($ssotoken);
	
		if (empty($name)) {
			return;
		}
	
		// Locate the user in Joomla
		$userid = JUserHelper::getUserId($name);
	
		if (empty($userid)) {
			return;
		}
	  
		// Complete the login process
		// Get a reference to the global user object
		$user = JUser::getInstance($userid);

		// Dreadful hack to get a JAuthenticationResponse object, since we 
		// can't simply instantiate one from here		
		$authenticate = & JAuthentication::getInstance();
		$response	  = $authenticate->authenticate(array('username' => $name), array());
		
		// Populate the authentication response object
		$response->username	  = $user->username;
		$response->email		 = $user->email;
		$response->fullname	  = $user->name;
		$response->status		= JAUTHENTICATE_STATUS_SUCCESS;
		$response->error_message = '';		
		
		// Now, import the user plugin group
		JPluginHelper::importPlugin('user');
		
		// Fire the onLogin event
		$results = $app->triggerEvent('onLoginUser', array((array)$response, array()));
		
		// Lastly, redirect to wherever the user was trying to get to
		if ($return = JRequest::getVar('return', '', 'method', 'base64')) {
			$return = base64_decode($return);
			if (!JURI::isInternal($return)) {
				$return = '';
			}
		}
		if ( ! $return ) {
			$return	= "index.php";
		}
		$app->redirect($return);
		exit();		
	}

	function redirectToOpensso($option) {
		$url = $this->urlNoQuery();
		
		$url .= "?option=$option&task=login";
		$return = $_REQUEST['return'];
		if ( ! empty($return) ) {
			$url .= "&return=$return";
		}
		
		$app = & JFactory::getApplication();
		$app->redirect($this->opensso_login_url . '?goto=' . urlencode($url));
		exit();
	}
	
	function alterLoginForm() {
		$javascript  = 'function alterLoginForm()' . "\n";
		$javascript .= '{' . "\n";
		$javascript .= '	var formLoginUsername = $("form-login-username");' . "\n";
		$javascript .= '	if ( formLoginUsername ) {' . "\n";
		$javascript .= '		formLoginUsername.innerHTML = "Click the Login button below to login via OpenSSO";' . "\n";
		$javascript .= '		$("form-login-password").setStyle("display", "none");' . "\n";
		$javascript .= '		$("form-login-remember").setStyle("display", "none");' . "\n";
		$javascript .= '	}' . "\n";
		$javascript .= '}' . "\n";
		$javascript .= 'window.addEvent("domready", alterLoginForm);' . "\n";

		$doc =& JFactory::getDocument();
		$doc->addScriptDeclaration( $javascript );
	}		

  	/*
	 * Validate token. Returns 1 for valid token, 0 for invalid token, -1 for error
	 */
	function isTokenValid($ssotoken) {
	  $cookies = array($this->opensso_cookie_name =>  $ssotoken);
	  $options = array('cookies' => $cookies, 'encodecookies' => false);
	  
	  $response = http_parse_message(http_get($this->opensso_is_token_valid, $options));
	  if ($response->responseCode != 200) {
		return -1;
	  }
				
	  // value will be of the form boolean=true
	  if (substr(trim($response->body), 8) == 'true') {
		return 1;
	  }
	
	  return 0;
	}
	
	/*
	 * Given an SSO token, return the username in Joomla
	 */
	function getJoomlaName($ssotoken) {
	  $url = $this->opensso_attributes . '?subjectid=' . urlencode($ssotoken);
	  $response = http_parse_message(http_get($url));
	
	  if ($response->responseCode != 200) {
		return;
	  }
	
	  // Need to parse name/value pairs, to get value for Drupal username attribute
	  $lines = explode("\n", $response->body);
	  reset($lines);
	  foreach ($lines as $line) {
		if ($line == ('userdetails.attribute.name=' . $this->opensso_joomla_username_attribute)) {
		  // 'current' line holds attribute value
		  // 28 points to character after 'userdetails.attribute.value='
		  $name = substr(current($lines), 28);
		  break;
		}
	  }
	  
	  return $name;
	}
	
	/*
	 * Returns the requested URL without any query parameters
	 */
	function urlNoQuery() {
		$url = 'http';
	
		if ( $_SERVER['HTTPS'] == 'on' ) { 
			$url .=  's';
		}
	
		$url .=  '://';
	
		if ( ( $_SERVER['HTTPS'] != 'on' && $_SERVER['SERVER_PORT'] != '80' ) || ( $_SERVER['HTTPS'] == 'on' && $_SERVER['SERVER_PORT'] != '443' ) ) {
			$url .=  $_SERVER['HTTP_HOST'] . ':' . $_SERVER['SERVER_PORT'] . $_SERVER['SCRIPT_NAME'];
		} else {
			$url .=  $_SERVER['HTTP_HOST'] . $_SERVER['SCRIPT_NAME'];
		}
		return $url;
	}
		
	/*
	 * Returns the full requested URL so we can redirect the user back here after 
	 * they authenticate at OpenSSO
	 */
	function fullUrl() {
		$full_url = $this->urlNoQuery();
	
		if ( $_SERVER['QUERY_STRING']>' ' ) {
			$full_url .=  '?'.$_SERVER['QUERY_STRING'];
		}
	  
		return $full_url;
	}
}