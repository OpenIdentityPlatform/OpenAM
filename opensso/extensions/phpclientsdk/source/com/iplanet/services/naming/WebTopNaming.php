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
 * $Id: WebTopNaming.php,v 1.1 2007/03/09 21:13:13 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * The <code>WebtopNaming</code> class is used to get URLs for various
 * services such as session, profile, logging etc. The lookup is based on the
 * service name and the host name. The Naming Service shall contain URLs for all
 * services on all servers. For instance, two machines might host session
 * services. The Naming Service profile may look like the following:
 *
 * <pre>
 *      host1.session.URL=&quot;http://host1:8080/SessionServlet&quot;
 *      host2.session.URL=&quot;https://host2:9090/SessionServlet&quot;
 * </pre>
 */
class WebtopNaming {

	const IGNORE_NAMING_SERVICE = "com.iplanet.am.naming.ignoreNamingService";
	const NAMING_SERVICE = "com.iplanet.am.naming";
	const NODE_SEPARATOR = "|";

	private static $amServer;
	private static $amServerPort;
	private static $amServerProtocol;

	private static $ignoreNaming = true;

	private static $serverMode = false;

	private static $namingTable = null;

	private static $serverIdTable = null;

	private static $siteIdTable = null;

	private static $platformServers = array ();

	private static $namingServiceURL = null;

	public static function __init() {
		WebtopNaming :: $amServer = SystemProperties :: get(Constants :: AM_SERVER_HOST);
		WebtopNaming :: $amServerPort = SystemProperties :: get(Constants :: AM_SERVER_PORT);
		WebtopNaming :: $amServerProtocol = SystemProperties :: get(Constants :: AM_SERVER_PROTOCOL);
	}

	public static function isServerMode() {
		return WebtopNaming :: $serverMode;
	}

    public static function getAMServerID() {
        return WebtopNaming::getServerID(WebtopNaming::$amServerProtocol, WebtopNaming::$amServer, WebtopNaming::$amServerPort);
    }

        /**
     * This function gets the server id that is there in the platform server
     * list for a corresponding server. One use of this function is to keep this
     * server id in our session id.
     */
    public static function getServerID($protocol, $host, $port) {
            // check before the first naming table update to avoid deadlock
            if ($protocol == null || $host == null || $port == null
                    || strlen($protocol) == 0 || strlen($host) == 0
                    || strlen($port) == 0)
                throw new Exception("No Server ID");

            $server = $protocol . ":" . "//" . $host . ":" . $port;
            $serverID = null;
            if (WebtopNaming::$serverIdTable != null)
                $serverID = WebtopNaming::getValueFromTable(WebtopNaming::$serverIdTable, $server);

            // update the naming table and as well as server id table
            // if it can not find it
            if ($serverID == null) {
                WebtopNaming::getNamingProfile(true);
                $serverID = WebtopNaming::getValueFromTable(WebtopNaming::$serverIdTable, $server);
            }
            if ($serverID == null)
                throw new Exception("No Server ID");

            return $serverID;
    }

	/**
	* This method returns the URL of the specified service on the specified
	* host.
	*
	* @param service
	*            The name of the service.
	* @param protocol
	*            The service protocol
	* @param host
	*            The service host name
	* @param port
	*            The service listening port
	* @return The URL of the specified service on the specified host.
	*/
	public static function getServiceURL($service, $protocol, $host, $port) {
		$validate = WebtopNaming :: isServerMode();

		return WebtopNaming :: getServiceURLAndValidate($service, $protocol, $host, $port, $validate);
	}

	/**
	 * This method returns the URL of the specified service on the specified
	 * host.
	 *
	 * @param service
	 *            The name of the service.
	 * @param protocol
	 *            The service protocol
	 * @param host
	 *            The service host name
	 * @param port
	 *            The service listening port
	 * @param validate
	 *            Validate the protocol, host and port of AM server
	 * @return The URL of the specified service on the specified host.
	 */
	public static function getServiceURLAndValidate($service, $protocol, $host, $port, $validate) {
		// check before the first naming table update to avoid deadlock
		if ($protocol == null || $host == null || $port == null || strlen($protocol) == 0 || strlen($host) == 0 || strlen($port) == 0)
			throw new Exception("No service URL: " . $service);

		if (WebtopNaming :: $ignoreNaming) {
			$protocol = WebtopNaming :: $amServerProtocol;
			$host = WebtopNaming :: $amServer;
			$port = WebtopNaming :: $amServerPort;
		}

		if (WebtopNaming :: $namingTable == null)
			WebtopNaming :: getNamingProfile(false);

		$url = null;
		$name = "iplanet-am-naming-" . strtolower($service) . "-url";
		$url = WebtopNaming :: $namingTable[$name];
		if ($url != null) {
			// If replacement is required, the protocol, host, and port
			// validation is needed against the server list
			// (iplanet-am-platform-server-list)
			if ($validate && strpos($url, "%"))
				WebtopNaming :: validate($protocol, $host, $port);

			// %protocol processing
			$url = str_replace("%protocol", $protocol, $url);

			// %host processing
			$url = str_replace("%host", $host, $url);

			// %port processing
			$url = str_replace("%port", $port, $url);

			return $url;
		} else {
			throw new Exception("No service URL: " . $service);
		}
	}

	/**
	 * This method returns key value from a hashtable, ignoring the case of the
	 * key.
	 */
	private static function getValueFromTable($table, $key) {
		if (array_key_exists($key, $table))
			return $table[$key];

		foreach (array_keys($table) as $tmpKey) {
			if (strcmp($tmpKey, $key))
				return $table[$tmpKey];
		}

		return null;
	}

	private static function getNamingProfile($update) {
		if ($update || WebtopNaming :: $namingTable == null)
			WebtopNaming :: updateNamingTable();
	}

	/**
	 * This method returns the list URL of the naming service url
	 *
	 * @return Array of naming service url.
	 * @throws Exception -
	 *             if there is no configured urls or any problem in getting urls
	 */
	public static function getNamingServiceURL() {
		if (WebtopNaming :: $namingServiceURL == null) {
			// Initilaize the list of naming URLs
			$urlList = array ();

			// Check for naming service URL in system propertied
			//            $systemNamingURL = SystemProperties::get(Constants::AM_NAMING_URL);
			//            if ($systemNamingURL != null)
			//                $urlList[] = $systemNamingURL;

			// Get the naming service URLs from properties files
			$configURLListString = SystemProperties :: get(Constants :: AM_NAMING_URL);
			if ($configURLListString != null) {
				$stok = explode(" ", $configURLListString);
				foreach ($stok as $nextURL) {
					if (array_key_exists($nextURL, $urlList)) {
						//                        if (debug.warningEnabled()) {
						//                            debug.warning("Duplicate naming service URL " +
						//                                  "specified "+ nextURL + ", will be ignored.");
						//                        }
					} else
						$urlList[] = $nextURL;
				}

				if (count($urlList) == 0)
					throw new Exception("No NamingService URL");
				//			else {
				//				if (debug . messageEnabled()) {
				//					debug . message("Naming service URL list: " + urlList);
				//				}
			}

			WebtopNaming :: $namingServiceURL = $urlList;

			// Start naming service monitor if more than 1 naming URLs are found
			//			if (!isServerMode() && (urlList . size() > 1)) {
			//				Thread monitorThread = new Thread(new SiteMonitor());
			//				monitorThread . setDaemon(true);
			//				monitorThread . setPriority(Thread . MIN_PRIORITY);
			//				monitorThread . start();
			//			} else {
			//				if (debug . messageEnabled()) {
			//					debug . message("Only one naming service URL specified." + " NamingServiceMonitor will be disabled.");
			//				}
			//			}
		}

		return WebtopNaming::$namingServiceURL;
	}

	private static function initializeNamingService() {
		WebtopNaming :: $ignoreNaming = (SystemProperties :: get(WebtopNaming :: IGNORE_NAMING_SERVICE) === "true" ? true : false);

//		try {
			// Initilaize the list of naming URLs
			WebtopNaming :: getNamingServiceURL();
//		} catch (Exception $ex) {
//			debug . error("Failed to initialize naming service", ex);
//		}
	}

	/*
	* this method is to update the servers and their ids in a seprate hash and
	* will get updated each time when the naming table gets updated note: this
	* table will have all the entries in naming table but in a reverse order
	* except the platform server list We can just as well keep only server id
	* mappings, but we need to exclude each other entry which is there in.
	*/
	private static function updateServerIdMappings() {
		WebtopNaming :: $serverIdTable = array ();
		foreach (WebtopNaming :: $namingTable as $key => $value) {
			if ($key == null || value == null)
				continue;

			// If the key is server list skip it, since it would
			// have the same value
			if ($key == Constants :: PLATFORM_LIST)
				continue;

			WebtopNaming :: $serverIdTable[$value] = $key;
		}
	}

	private static function updateSiteIdMappings() {
		WebtopNaming :: $siteIdTable = array ();
		$serverSet = WebtopNaming :: $namingTable[Constants :: SITE_ID_LIST];
		if ($serverSet == null || strlen($serverSet) == 0)
			return;

		$st = explode($serverSet, ",");
		foreach ($st as $serverId) {
			$siteid = $serverId;
			$idx = strpos($serverId, NODE_SEPARATOR);
			if ($idx) {
				$siteid = substr($serverId, $idx +1, strlen($serverId));
				$serverId = substr($serverId, 0, $idx);
			}
			WebtopNaming :: $siteIdTable[$serverId] = $siteid;
		}
		//        if (debug.messageEnabled())
		//            debug.message("SiteID table -> " + siteIdTable.toString());
	}

	private static function getNamingTable($nameurl) {
        $nametbl = null;
        $nrequest = new NamingRequest(NamingRequest::reqVersion);
        $request = new Request($nrequest->toXMLString());
        $set = new RequestSet(WebtopNaming::NAMING_SERVICE);
        $set->addRequest($request);

//        try {
            $responses = PLLClient::sendURLRequestSet($nameurl, $set);
            if (count($responses) != 1)
                throw new Exception("Unexpected response");

            $res = $responses[0];
            $nres = NamingResponse::parseXML($res->getContent());
            if ($nres->getException() != null)
                throw new Exception($nres->getException());

            $nametbl = $nres->getNamingTable();
//        } catch (SendRequestException sre) {
//            debug.error("Naming service connection failed for " + nameurl, sre);
//        } catch (Exception e) {
//            debug.error("getNamingTable: ", e);
//        }

        return $nametbl;
    }

	private static function updateNamingTable() {
		if (!WebtopNaming :: isServerMode()) {
			if (WebtopNaming :: $namingServiceURL == null)
				WebtopNaming :: initializeNamingService();

			// Try for the primary server first, if it fails and then
			// for the second server. We get connection refused error
			// if it doesn't succeed.
			WebtopNaming :: $namingTable = null;
			$tempNamingURL = null;
			for ($i = 0;((WebtopNaming :: $namingTable == null) && ($i < count(WebtopNaming :: $namingServiceURL))); $i++) {
				$tempNamingURL = WebtopNaming :: $namingServiceURL[$i];
				WebtopNaming :: $namingTable = WebtopNaming :: getNamingTable($tempNamingURL);
			}

			if (WebtopNaming::$namingTable == null) {
				//                debug.error("updateNamingTable : "
				//                        + NamingBundle.getString("noNamingServiceAvailable"));
				throw new Exception("No naming service available");
			}

			WebtopNaming :: updateServerProperties($tempNamingURL);
		}
		//		else
		//			WebtopNaming :: $namingTable = NamingService->getNamingTable();

		$servers = WebtopNaming :: $namingTable[Constants :: PLATFORM_LIST];
		if ($servers != null) {
			$st = explode($servers, ",");
			WebtopNaming :: $platformServers = array ();
			foreach ($st as $token)
				WebtopNaming :: $platformServers[] = strtolower($token);
		}
		WebtopNaming :: updateServerIdMappings();
		WebtopNaming :: updateSiteIdMappings();

		//        if (debug.messageEnabled()) {
		//            debug.message("Naming table -> " + namingTable.toString());
		//            debug.message("Platform Servers -> " + platformServers.toString());
		//        }
	}

	/**
	* This function returns server from the id.
	*/
	public static function getServerFromID($serverID) {
		$server = null;
		// refresh local naming table in case the key is not found
		if (WebtopNaming :: $namingTable != null)
			$server = WebtopNaming :: getValueFromTable(WebtopNaming :: $namingTable, $serverID);

		if ($server == null) {
			WebtopNaming :: getNamingProfile(true);
			$server = WebtopNaming :: getValueFromTable(WebtopNaming :: $namingTable, $serverID);
		}
		if ($server == null) {
			throw new Exception("No server found: " . $serverID);
		}

		return $server;
	}

	/**
	 * Extended ServerID syntax : localserverID | lbID-1 | lbID-2 | lbID-3 | ...
	 * It returns lbID-2 | lbID-3 | ...
	 */
	public static function getSecondarySites($serverid) {
		$sitelist = null;
		$secondarysites = null;

		if (WebtopNaming :: $siteIdTable == null)
			return null;

		$sitelist = WebtopNaming :: $siteIdTable[$serverid];
		if ($sitelist == null)
			return null;

		$index = strpos($sitelist, NODE_SEPARATOR);
		if ($index)
			$secondarysites = substr($sitelist, $index +1, strlen($sitelist));

		//        if (debug.messageEnabled()) {
		//            debug.message("WebtopNaming : SecondarySites for " + serverid
		//                    + " is " + secondarysites);
		//        }

		return $secondarysites;
	}

    private static function updateServerProperties($url) {
    	$bits = parse_url($url);
		WebtopNaming::$amServer = $bits['host'];
		WebtopNaming::$amServerPort = isset ($bits['port']) ? $bits['port'] : 80;
		WebtopNaming::$amServerProtocol = isset ($bits['protocol']) ? $bits['protocol'] : "http";

        SystemProperties::set(Constants::AM_SERVER_HOST,
                WebtopNaming::$amServer);
        SystemProperties::set(Constants::AM_SERVER_PORT,
                WebtopNaming::$amServerPort);
        SystemProperties::set(Constants::AM_SERVER_PROTOCOL,
                WebtopNaming::$amServerProtocol);
    }

}
?>
