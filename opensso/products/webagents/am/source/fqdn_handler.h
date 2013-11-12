/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: fqdn_handler.h,v 1.4 2008/09/13 01:11:53 robertis Exp $
 *
 * Abstract:
 *
 * Post Cache functionality is maintained by this class. It
 * keeps a handle to the thread, hash table for POST Cache
 *
*/
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef FQDN_HANDLER_H
#define FQDN_HANDLER_H

#include <string>
#include <stdio.h>
#include <stdexcept>

#include "internal_macros.h"
#include "log.h"
#include "properties.h"

BEGIN_PRIVATE_NAMESPACE

/*
 * Class: FqdnHandler
 *
 * This class provides utility functions used by web-agents to
 * ensure that malformed URLs can be corrected by the agent in
 * order to avoid related cookie-masking, infinite-auth-redirection
 * and other such problems.
 *
 * The FqdnHandler can provide conclusive information indicating if
 * the correct FQDN has been used in the resource URL or not. In the
 * case when the correct FQDN has not been used in the resource URL,
 * the FqdnHandler can provide the alternate resource URL that the
 * agent can redirect the user to in order to ensure that the
 * subsequent request from the user is not malformed.
 *
 * This class relies on the following configuration keys read from
 * the agent configuration property file:
 *
 * 1) com.sun.am.policy.am.fqdnMap:
 *    This key contains a map that provides a simple mapping between
 *    various potentially malformed FQDNs with the actual correct
 *    FQDNs that must be used. For example:
 *
 *       foo -> foo.somecompany.com
 *       foo.somecompany -> foo.somecompany.com
 *
 *    The exact format for specifying this property is detailed out
 *    in the property file comments.
 *
 * 2) com.sun.am.policy.am.fqdnDefault:
 *    This key contains a value of the default FQDN to be used in
 *    the particular case where the resource URL's FQDN is malformed
 *    and not correctable based on the values specified in the
 *    com.sun.am.policy.am.fqdnMap property.
 *
 * To evaluate if a given resource URL is malformed, the FqdnHandler
 * first compares it with the valid FQDN entries in the map (such as
 * foo.somecompay.com) and also the value specified for the fqdnDefault
 * property. If there is a match, no further action is taken.
 * However, if there is no match, the incoming resource URL's FQDN
 * is compared with the invalid FQDN entires in the map (such as
 * foo, or foo.somecompany). If a match is found, the corresponding valid
 * FQDN from the map is used. If no match is found, then the fqdnDefault
 * property value is used.
 *
 * Special Case:
 * - In case this class determines that the user supplied URL does not
 *   contain a valid FQDN and at the same time no valid value can be
 *   determined, the redirect URL will then be that of access denied page
 *   or 403 status code.
 *
 */
class FqdnHandler {

public:
    
    /*
     * Constructor
     *
     * Parameters:
     *     - properties: The properties object associated with the
     *                   configuration file used by the agent.
     *
     *     - module_id:  The Log::ModuleID to be used by the FQDN
     *                   Handler for all subsequent logging purposes.
     *
     */
    FqdnHandler(const Properties& properties,
		bool icase,
		Log::ModuleId module_id);
    

    /* Destructor */
    ~FqdnHandler() { }

    /*
     * Returns a flag indicating if a valid FQDN has been used
     *
     * Parameters:
     *     - resName: a string representing the resource URL as
     *                typed in by the end user.
     *                
     * Returns:
     *     - true : if the URL used is valid
     *     - false: if the URL used is malformed and redirect is
     *              needed
     *
     **/
    bool isValidFqdnResource(const char *resName);

    /*
     * Returns a URL string that must be used for redirecting the
     * user in case the call to isValidFqdnAccess() was negetive.
     *
     * Parameters:
     *     - resName: a string representing the resource URL as
     *                typed in by the end user.
     *
     * Returns:
     *     - A string representing the URL that user should be redirected
     *       to, in order to ensure that the malformed URL is not used.
     *
     */
    const std::string getValidFqdnResource(const char *resName);

private:
    Log::ModuleId logID;                  // log module to use
    bool ignore_case; // resource traits for URL comp.
    Properties fqdnMap;   // Map as read from config file
    std::string fqdnDefault;              // Default FQDN from config file
    std::vector<std::string> validFqdns;  // other valid FQDN's.
    void parsePropertyKeyValue(const std::string&, char, char);
};

END_PRIVATE_NAMESPACE



#endif // not FQDN_HANDLER_H
