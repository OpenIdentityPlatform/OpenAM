/*
 * Copyright (c) 1996-2005, Oracle All Rights Reserved.
 *
 * authn_api.c
 *
 * Custom Authentication Plugin Skeleton
 * This sample code provides the minimum implementation of a NetPoint
 * Authentication Plugin. It may be used as a skeleton to build
 * functioning plugins.
 */

#include <authn_api.h>
#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include <am_sso.h> 

OBDLLEXPORT const char*
ObAnPluginGetVersion(void)
{
	/*
	 * Called by the Access Server to check for compatible versions of the
	 * plugin interface.
	 */
	return OB_AN_PLUGIN_VERSION;  /* defined in authn_api.h */
}

OBDLLEXPORT void
ObAnPluginDeallocStatusMsg(ObAnPluginStatusMsg_t pResult)
{
	/*
	 * Called by the Access Server to delete any result messages returned by
	 * the plugin methods.
	 */
    if(pResult != NULL && *pResult != NULL) {
		free(*pResult);
		*pResult = NULL;
	}
}

OBDLLEXPORT ObAnPluginStatus_t
ObAnPluginInit (ObAnServerContext_t pContext, ObAnPluginStatusMsg_t pResult)
{
	/*
	 * Called by the Access Server when it starts up to perform any
	 * initialization required by the plugin. Example: open a database.
	 */
	*pResult = strdup("Success");
    return ObAnPluginStatusContinue;
}

OBDLLEXPORT ObAnPluginStatus_t
ObAnPluginTerminate (ObAnServerContext_t pServerContext, ObAnPluginStatusMsg_t pResult)
{
	/*
	 * Called by the Access Server when it terminates to perform any cleanup
	 * required by the plugin. Example: close a database.
	 */
	*pResult = strdup("Success");
    return ObAnPluginStatusContinue;
}

OBDLLEXPORT ObAnPluginStatus_t
ObAnPluginFn (ObAnServerContext_t pContext, ObAnPluginFns_t pFnBlock,
              ObAnPluginInfo_t pInfo, ObAnPluginStatusMsg_t pResult)
{
	const char* famsession = NULL;
        const char* principal = NULL;
        am_sso_token_handle_t sso_handle = NULL;
        am_status_t status = AM_FAILURE;
        am_properties_t prop = AM_PROPERTIES_NULL;
        const char* prop_file = "/export/oam/access/oblix/sdk/authentication/samples/authn_api/AMAgent.properties"; 
        
       
        //Read the FAM session first.
        famsession = pFnBlock->GetCredFn(pInfo->Creds, "famsession");
        if(famsession != NULL) {
           printf("%s\n", famsession);
        } else {
           printf("No FAM session found.");
 	   *pResult = strdup("Failure");
	   return ObAnPluginStatusContinue;
        }
        
         
        status = am_properties_create(&prop);
/*
        fail_on_error(status, "am_properties_create");
*/

        status = am_properties_load(prop, prop_file);
/*
        fail_on_error(status, "am_properties_load");
*/

        status = am_sso_init(prop);
/*
        fail_on_error(status, "am_sso_init");
*/

        status = am_sso_create_sso_token_handle(&sso_handle, 
                 famsession, B_FALSE);
        if(status == AM_FAILURE) {
           printf("Authentication failed.");
 	   *pResult = strdup("Failure");
	   return ObAnPluginStatusContinue;
        }
       
        principal = am_sso_get_principal(sso_handle);
        if(principal != NULL) {
           printf("%s\n", principal);
           pFnBlock->SetCredFn(pInfo->Creds, ObAnPluginRequesterDN, 
                  principal);
        }

 	*pResult = strdup("Success");
	return ObAnPluginStatusContinue;
}
