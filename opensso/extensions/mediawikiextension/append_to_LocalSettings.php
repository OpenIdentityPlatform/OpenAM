# OpenSSO extension
require_once( "extensions/opensso/opensso.php" );

$wgOpenSSO = new OpenSSO();

// Set this to false to disable the OpenSSO Extension
$wgOpenSSO->setOpenssoEnabled( true );

// You will only need to change the cookie name if you have changed it on the 
// OpenSSO server
$wgOpenSSO->setOpenssoCookieName( 'iPlanetDirectoryPro' );

// You MUST set the base URL
$wgOpenSSO->setOpenssoBaseUrl( 'http://demo.example.com:8080/opensso/' );

// Set this to the OpenSSO profile attribute that holds users' MediaWiki names
$wgOpenSSO->setOpenssoMediaWikiUsernameAttribute( 'uid' );
