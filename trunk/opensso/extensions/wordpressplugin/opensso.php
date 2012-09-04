<?php
/*
Plugin Name: OpenSSO Plugin
Plugin URI: https://opensso.dev.java.net/public/extensions/#authnproviders
Description: OpenSSO Single Sign-on Plugin
Version: 1.0
Author: OpenSSO Team
Author URI: http://opensso.org/
*/
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
 * $Id: opensso.php,v 1.4 2009/07/27 22:55:15 superpat7 Exp $
 *
 */

// WordPress Hooks
add_action( 'init',	   'opensso_init' );
add_action( 'admin_menu', 'opensso_plugin_menu' );

// Options
add_option( 'opensso_enabled',                      0 );
add_option( 'opensso_cookie_name',                  'iPlanetDirectoryPro' );
add_option( 'opensso_base_url',                     'http://demo.example.com:8080/opensso/' );
add_option( 'opensso_wordpress_username_attribute', 'uid' );

define( 'OPENSSO_ENABLED',                      get_option( 'opensso_enabled' ) );
define( 'OPENSSO_COOKIE_NAME',                  get_option( 'opensso_cookie_name' ) );
define( 'OPENSSO_BASE_URL',                     get_option( 'opensso_base_url' ) );
define( 'OPENSSO_WORDPRESS_USERNAME_ATTRIBUTE', get_option( 'opensso_wordpress_username_attribute' ) );

define( 'OPENSSO_LOGIN_URL',      OPENSSO_BASE_URL . 'UI/Login' );
define( 'OPENSSO_LOGOUT_URL',     OPENSSO_BASE_URL . 'UI/Logout' );
define( 'OPENSSO_IS_TOKEN_VALID', OPENSSO_BASE_URL . 'identity/isTokenValid' );
define( 'OPENSSO_ATTRIBUTES',     OPENSSO_BASE_URL . 'identity/attributes' );

if( !function_exists( 'auth_redirect' ) ) :
function auth_redirect() {
	// Checks if a user is logged in, if not redirects them to the login page

	if ( is_ssl() || force_ssl_admin() )
		$secure = true;
	else
		$secure = false;

	// If https is required and request is http, redirect
	if ( $secure && !is_ssl() && false !== strpos( $_SERVER['REQUEST_URI'], 'wp-admin' ) ) {
		if ( 0 === strpos( $_SERVER['REQUEST_URI'], 'http' ) ) {
			wp_redirect( preg_replace( '|^http://|', 'https://', $_SERVER['REQUEST_URI'] ) );
			exit();
		} else {
			wp_redirect( 'https://' . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'] );
			exit();
		}
	}
	
	if ( $user_id = wp_validate_auth_cookie() ) {
		do_action( 'auth_redirect', $user_id );
		// If the user wants ssl but the session is not ssl, redirect.
		if ( !$secure && get_user_option( 'use_ssl', $user_id ) && false !== strpos( $_SERVER['REQUEST_URI'], 'wp-admin' ) ) {
			if ( 0 === strpos($_SERVER['REQUEST_URI'], 'http') ) {
				wp_redirect( preg_replace( '|^http://|', 'https://', $_SERVER['REQUEST_URI'] ) );
				exit();
			} else {
				wp_redirect( 'https://' . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'] );
				exit();
			}
		}

		return;  // The cookie is good so we're done
	}
			
	// The cookie is no good so force login
	nocache_headers();
	
	if ( OPENSSO_ENABLED ) {
		// Redirect to OpenSSO login page then return here
		$login_url = OPENSSO_BASE_URL . '?goto=' . urlencode( opensso_full_url() );
	} else {
		if ( is_ssl() )
			$proto = 'https://';
		else
			$proto = 'http://';
	
		$redirect = ( strpos($_SERVER['REQUEST_URI'], '/options.php') && wp_get_referer() ) ? wp_get_referer() : $proto . $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'];
	
		$login_url = wp_login_url($redirect);	
	}
	
	wp_redirect($login_url);
	exit();	
}
endif;

if ( !function_exists( 'wp_validate_auth_cookie' ) ) :
function wp_validate_auth_cookie( $cookie = '', $scheme = '' ) {
	if ( OPENSSO_ENABLED ) {
		// Quick hack to get round the fact that '+' often gets decoded to ' '
		$ssotoken = str_replace(' ', '+', $_COOKIE[OPENSSO_COOKIE_NAME]);
		
		// Is there an SSO token?
		if ( empty( $ssotoken ) ) {
			return false;
		}
		
		// Is the token valid?  
		switch ( opensso_is_token_valid( $ssotoken ) ) {
			case 0:
				// Session expired
				return false;
			case -1:
				// Error validating token
				do_action( 'auth_cookie_malformed', $cookie, $scheme );
				return false;
		}
		
		$username = opensso_get_name( $ssotoken );
	} else {
		if ( ! $cookie_elements = wp_parse_auth_cookie($cookie, $scheme) ) {
			do_action('auth_cookie_malformed', $cookie, $scheme);
			return false;
		}
	
		extract($cookie_elements, EXTR_OVERWRITE);
	
		$expired = $expiration;
	
		// Allow a grace period for POST and AJAX requests
		if ( defined('DOING_AJAX') || 'POST' == $_SERVER['REQUEST_METHOD'] )
			$expired += 3600;
	
		// Quick check to see if an honest cookie has expired
		if ( $expired < time() ) {
			do_action('auth_cookie_expired', $cookie_elements);
			return false;
		}
	}

	$user = get_userdatabylogin( $username );
	if ( ! $user ) {
		do_action( 'auth_cookie_bad_username', $cookie_elements );
		return false;
	}
	
	if ( ! OPENSSO_ENABLED ) {
		$pass_frag = substr($user->user_pass, 8, 4);
	
		$key = wp_hash($username . $pass_frag . '|' . $expiration, $scheme);
		$hash = hash_hmac('md5', $username . '|' . $expiration, $key);
	
		if ( $hmac != $hash ) {
			do_action('auth_cookie_bad_hash', $cookie_elements);
			return false;
		}
	}

	do_action( 'auth_cookie_valid', $cookie_elements, $user );

	return $user->ID;
}
endif;

if ( !function_exists( 'wp_logout' ) ) :
function wp_logout() {
	if ( OPENSSO_ENABLED ) {
		// Redirect to OpenSSO logout and then to the admin page, where we'll
		// get another login prompt
		do_action( 'wp_logout' );
		wp_redirect( OPENSSO_LOGOUT_URL . '?goto=' . urlencode( admin_url() ) );
		exit();
	} else {
		wp_clear_auth_cookie();
		do_action('wp_logout');
	}
}
endif;

function opensso_init() {
	if ( OPENSSO_ENABLED && strpos($_SERVER['REQUEST_URI'], '/wp-login.php') && $_REQUEST['action'] != 'logout' ) {
		// If the user is trying to go to the login page, just redirect to 
		// admin page - above plugin functions will do the rest
		wp_redirect( admin_url() );
		exit();
	}
}

function opensso_plugin_menu() {
  add_options_page('OpenSSO Plugin Options', 'OpenSSO Plugin', 8, 'opensso', 'opensso_plugin_options');
}

function opensso_plugin_options() {
?>
<div class="wrap">
<div id="icon-options-general" class="icon32"><br /></div>
<h2>OpenSSO Plugin</h2>

<form method="post" action="options.php">
<?php wp_nonce_field('update-options'); ?>

<table class="form-table">

<tr valign="top">
<th scope="row"><?php _e('OpenSSO enabled') ?></th>
<td> <fieldset><legend class="screen-reader-text"><span><?php _e('OpenSSO cookie name') ?></span></legend><label for="opensso_enabled">
<input name="opensso_enabled" type="checkbox" id="opensso_enabled" value="1" <?php checked('1', get_option('opensso_enabled')); ?> />
<?php _e('Only check this once you\'re sure the following parameters are correct!') ?></label>
</fieldset></td>

<tr valign="top">
<th scope="row"><label for="opensso_cookie_name"><?php _e('OpenSSO cookie name') ?></label></th>
<td><input type="text" name="opensso_cookie_name" value="<?php echo get_option('opensso_cookie_name'); ?>" class="regular-text code" /><span class="description"><?php _e('This will be <code>iPlanetDirectoryPro</code> unless you\'ve changed it in OpenSSO.') ?></span>
</td>
</tr>

<tr valign="top">
<th scope="row"><label for="opensso_base_url"><?php _e('OpenSSO server name') ?></label></th>
<td><input type="text" name="opensso_base_url" value="<?php echo get_option('opensso_base_url'); ?>" class="regular-text code" /><span class="description"><?php _e('For example: <code>http://demo.example.com:8080/opensso/</code>') ?></span>
</td>
</tr>

<tr valign="top">
<th scope="row"><label for="opensso_wordpress_username_attribute"><?php _e('OpenSSO WordPress username attribute') ?></label></th>
<td><input type="text" name="opensso_wordpress_username_attribute" value="<?php echo get_option('opensso_wordpress_username_attribute'); ?>" class="regular-text code" /><span class="description"><?php _e('The name of the OpenSSO profile attribute to use as the WordPress username. Use the default, <code>uid</code>, if users have the same username in WordPress and OpenSSO.') ?></span>
</td>
</tr>

</table>

<input type="hidden" name="action" value="update" />
<input type="hidden" name="page_options" value="opensso_enabled,opensso_cookie_name,opensso_base_url,opensso_wordpress_username_attribute" />

<p class="submit">
<input type="submit" class="button-primary" value="<?php _e('Save Changes') ?>" />
</p>

</form>
</div>
<?php
}

/*
 * Validate token. Returns 1 for valid token, 0 for invalid token, -1 for error
 */
function opensso_is_token_valid( $ssotoken ) {
	$headers = array('Cookie' => OPENSSO_COOKIE_NAME . '=' . $ssotoken);

	$http = new WP_Http();
	$response = $http->get( OPENSSO_IS_TOKEN_VALID, array( 'headers' => $headers ) );
  
	if ( $response['response']['code'] != 200 ) {
		return -1;
	}

	// value will be of the form boolean=true
	if ( substr( trim( $response['body'] ), 8 ) == 'true' ) {
		return 1;
	}

	return 0;
}

/*
 * Given an SSO token, return the name
 */
function opensso_get_name( $ssotoken ) {
	$url = OPENSSO_ATTRIBUTES . '?subjectid=' . urlencode( $ssotoken );

	$http = new WP_Http();
	$response = $http->get( $url, array( 'headers' => $headers ) );

	if ( $response['response']['code'] != 200 ) {
		return null;
	}

	// Need to parse name/value pairs, to get value for WordPress username attribute
	$lines = explode( "\n", $response['body'] );
	reset( $lines );
	foreach ( $lines as $line ) {
		if ( $line == ( 'userdetails.attribute.name=' . OPENSSO_WORDPRESS_USERNAME_ATTRIBUTE ) ) {
			// 'current' line holds attribute value
			// 28 points to character after 'userdetails.attribute.value='
			$name = substr( current( $lines ), 28 );
			break;
		}
	}
  
	return $name;
}

/*
 * Returns the full requested URL so we can redirect the user back here after 
 * they authenticate at OpenSSO
 */
function opensso_full_url() {
	$full_url = 'http';

	if ( $_SERVER['HTTPS'] == 'on' ) { 
		$full_url .=  's';
	}

	$full_url .=  '://';

	if ( ( $_SERVER['HTTPS'] != 'on' && $_SERVER['SERVER_PORT'] != '80' ) || ( $_SERVER['HTTPS'] == 'on' && $_SERVER['SERVER_PORT'] != '443' ) ) {
		$full_url .=  $_SERVER['HTTP_HOST'] . ':' . $_SERVER['SERVER_PORT'] . $_SERVER['SCRIPT_NAME'];
	} else {
		$full_url .=  $_SERVER['HTTP_HOST'] . $_SERVER['SCRIPT_NAME'];
	}

	if ( $_SERVER['QUERY_STRING']>' ' ) {
		$full_url .=  '?'.$_SERVER['QUERY_STRING'];
	}
  
	return $full_url;
}
?>