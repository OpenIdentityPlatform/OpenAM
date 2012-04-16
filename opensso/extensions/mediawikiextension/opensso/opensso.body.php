<?php 
function openssoLoginForm($template) {
    global $wgOpenSSO;
    
	if (!$wgOpenSSO->mOpenssoEnabled) {
		return true;
	}
	
    $wgOpenSSO->setUrls();
    
    return $wgOpenSSO->login($template);
}

function openssoUserLogoutComplete( &$user, &$inject_html, $old_name ) {
    global $wgOpenSSO;
    
	if (!$wgOpenSSO->mOpenssoEnabled) {
		return true;
	}
	
    $wgOpenSSO->setUrls();
    
    return $wgOpenSSO->logout($user, $inject_html, $old_name);
}

    
class OpenSSO {
	var $mOpenssoEnabled    = false;
	var $mOpenssoCookieName = 'iPlanetDirectoryPro';
	var $mOpenssoBaseUrl    = 'http://demo.example.com:8080/opensso/';
	var $mOpenssoMediaWikiUsernameAttribute = 'uid';

	var $mOpenssoLoginUrl;
	var $mOpenssoLogoutUrl;
	var $mOpenssoIsTokenValid;
	var $mOpenssoAttributes;

	function setOpenssoEnabled($openssoEnabled) {
	   $this->mOpenssoEnabled = $openssoEnabled;
	}

	function setOpenssoBaseUrl($openssoBaseUrl) {
	   $this->mOpenssoBaseUrl = $openssoBaseUrl;
	}

	function setOpenssoCookieName($openssoCookieName) {
	   $this->mOpenssoCookieName = $openssoCookieName;
	}

	function setOpenssoMediaWikiUsernameAttribute($openssoMediaWikiUsernameAttribute) {
	   $this->mOpenssoMediaWikiUsernameAttribute = $openssoMediaWikiUsernameAttribute;
	}

	function setUrls() {
	   // Append a '/' to the base URL if there isn't already one there
	   if ( substr( $this->mOpenssoBaseUrl, strlen( $this->mOpenssoBaseUrl ) - 1 ) !== '/' ) {
	       $this->mOpenssoBaseUrl .= '/';
	   }

	   $this->mOpenssoLoginUrl     = $this->mOpenssoBaseUrl . 'UI/Login';
	   $this->mOpenssoLogoutUrl    = $this->mOpenssoBaseUrl . 'UI/Logout';
	   $this->mOpenssoIsTokenValid = $this->mOpenssoBaseUrl . 'identity/isTokenValid';
	   $this->mOpenssoAttributes   = $this->mOpenssoBaseUrl . 'identity/attributes';
	}

	function login($template) {
    	global $wgOut;

    	// Is there a user logged in already?
    	$wgUser = User::newFromSession();
    	if ($wgUser && $wgUser->isLoggedin()) {
    		return false;
    	}
    	
		// Quick hack to get round the fact that '+' often gets decoded to ' '
		$ssotoken = str_replace(' ', '+', $_COOKIE[$this->mOpenssoCookieName]);
		
		// Is there an SSO token?
		if (empty($ssotoken)) {
			// Redirect to OpenSSO login page then return here
			$wgOut->redirect( $this->mOpenssoLoginUrl . '?goto=' . urlencode( $this->fullUrl() ) );
			return false;
		}
		
		// Is the token valid?  
		switch ($this->isTokenValid($ssotoken)) {
			case 0:
				// Session expired
				$wgOut->redirect( $this->mOpenssoLoginUrl . '?goto=' . urlencode( $this->fullUrl() ) );
				return false;
			case -1:
				// Error validating token
    		    $wgOut->showErrorPage('error','listusers-noresult');
				return true;
		}

		// OK - if we get here then we have a valid session cookie
		$name = $this->getMediaWikiName($ssotoken);
		
		if (empty($name)) {
		    $wgOut->showErrorPage('error','listusers-noresult');
			return true;
		}
	
		// Locate the user in MediaWiki
    	$wgUser = User::newFromName($name);
    	if (! $wgUser || ! $wgUser->isLoggedin()){
		   $wgOut->showErrorPage('error','listusers-noresult');
	       return true;
    	}
    	
    	// Force reload of page
    	$wgUser->invalidateCache();

		$wgUser->setCookies();
		
		// Redirect to wherever the user clicked from
    	$action = $template->data['action'];
    	
		$wgOut->redirect( $this->getReturnToUrl($action) );
		return false;
	}
	
    function logout( &$user, &$inject_html, $old_name ) {
    	global $wgOut;

		$wgOut->redirect( $this->mOpenssoLogoutUrl . '?goto=' . urlencode($this->getReturnToUrl($this->fullUrl())) );
		return false;
    }
    
    function getReturnToUrl($url)
    {
    	$pageStart = strpos( $url, '&returnto=' ) + 10;

    	if ( $pageStart !== false ) {
        	$pageEnd = strpos( $url, '&', $pageStart );
        	
        	if ( $pageEnd === false ) {
                $page = substr( $url, $pageStart );
        	} else {
                $page = substr( $url, $pageStart, $pageEnd - $pageStart );
        	}
    	}

        $titleObj = Title::newFromText( $page );
		if ( !$titleObj instanceof Title ) {
			$titleObj = Title::newMainPage();
		}

		return $titleObj->getFullUrl();
    }
	
  	/*
	 * Validate token. Returns 1 for valid token, 0 for invalid token, -1 for error
	 */
	function isTokenValid($ssotoken) {
	  $cookies = array($this->mOpenssoCookieName =>  $ssotoken);
	  $options = array('cookies' => $cookies, 'encodecookies' => false);
	  
	  $response = http_parse_message(http_get($this->mOpenssoIsTokenValid, $options));
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
	 * Given an SSO token, return the username in MediaWiki
	 */
	function getMediaWikiName($ssotoken) {
	  $url = $this->mOpenssoAttributes . '?subjectid=' . urlencode($ssotoken);
	  $response = http_parse_message(http_get($url));
	
	  if ($response->responseCode != 200) {
		return;
	  }
	
	  // Need to parse name/value pairs, to get value for Drupal username attribute
	  $lines = explode("\n", $response->body);
	  reset($lines);
	  foreach ($lines as $line) {
		if ($line == ('userdetails.attribute.name=' . $this->mOpenssoMediaWikiUsernameAttribute)) {
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
	}}
?>