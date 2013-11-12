<?php 
$wgHooks['UserLoginForm'][] = 'openssoLoginForm';
$wgHooks['UserLogoutComplete'][] = 'openssoUserLogoutComplete';

$wgAutoloadClasses['OpenSSO'] = dirname(__FILE__) . '/opensso.body.php';

$wgExtensionCredits['other'][] = array(
    'path' => __FILE__,
    'name' => 'OpenSSO',
    'description' => 'OpenSSO Single Sign-on Extension',
    'author' =>'OpenSSO Project', 
    'url' => 'https://opensso.dev.java.net/public/extensions/#authnproviders'
    );
?>