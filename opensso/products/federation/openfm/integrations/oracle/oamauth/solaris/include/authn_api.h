#ifndef _ObAuthnAPI_h_
#define _ObAuthnAPI_h_

/* Copyright (c) 1996-2005, Oracle All Rights Reserved.
 *
 * authn_api.h
 *
 * Defines the Oracle NetPoint Authentication Plugin API v2.0.
 */

#ifdef _WIN32
# ifndef OBDLLEXPORT
#    define OBDLLEXPORT __declspec (dllexport)
# endif
#elif defined(__hpux)
# ifndef OBDLLEXPORT
#    define OBDLLEXPORT extern "C" 
# endif
#else
# ifndef OBDLLEXPORT
#    define OBDLLEXPORT
# endif
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define OB_AN_PLUGIN_VERSION "8.0"


/*********************************************************************/
/****************  Definitions of various constants  *****************/
/*********************************************************************/

/*  An enumeration of expected return values from plugin functions: 
 *
 *  ObAnPluginStatusContinue
 *       Authentication processing will continue after the function.
 *       If all plugins in an authentication scheme return continue,
 *       authentication is implicitely allowed.
 *
 *  ObAnPluginStatusAllowed
 *       Credentials were processed and authentication succeeded.
 *	 No further authentication plugins will be processed.
 *
 *  ObAnPluginStatusDenied
 *       Credentials were processed and authentication failed.  
 *       Processing will not continue after the function and
 *       authentication will fail.
 *
 *  ObAnPluginStatusAbort
 *       A fatal error occurred while processing authentication.
 *       Processing will not continue after the function.
 *       If returned during initialization, Access Server will log
 *       it, but will not terminate.
 *
 */
typedef enum {
  ObAnPluginStatusContinue = 0,
  ObAnPluginStatusAllowed = 1,
  ObAnPluginStatusDenied = 2,
  ObAnPluginStatusAbort = 3
} ObAnPluginStatus_t;


/* Enumerated return values from Access Server. When plugin calls a 
 * method supported by Access Server, Access Server will return one 
 * of the following statuses.
 *
 * ObAnASStatusSuccess
 *        This status implies the operation the plugin asked the 
 *        Access Server to perform was successful.
 *
 * ObAnASStatusFailed
 *        This status implies the operation the plugin asked the
 *        Access Server to perform was not successful.
 */
typedef enum {
  ObAnASStatusSuccess = 0,
  ObAnASStatusFailed
} ObAnASStatus_t;


/* An enumeration of Action Info types:
 *
 * ObAnSuccessRedirect
 *	This Action type sets the redirection URL upon
 *	successfull authentication.
 *
 * ObAnFailRedirect
 *	This Action type sets the redirection URL upon
 *	failed authentication.
 *
 * ObAnSuccessProfileAttrs
 *	This Action type sets the profile attributes upon 
 *	successfull authentication.
 *
 * ObAnFailProfileAttrs
 *	This Action type sets the profile attributes upon 
 *	failed authentication.
 *
 * ObAnSuccessFixedVals
 *	This Action type sets the fixed values upon
 *	successfull authentication.
 *
 * ObAnFailFixedVals
 *	This Action type sets the fixed values upon
 *	failed authentication.
 *
 */
typedef enum {
  ObAnSuccessRedirect = 0,
  ObAnFailRedirect = 1,
  ObAnSuccessProfileAttrs = 2,
  ObAnFailProfileAttrs = 3,
  ObAnSuccessFixedVals = 4,
  ObAnFailFixedVals = 5
} ObAnActionType_t;


/* Authentication plugin request specific information constants.
 *
 *	This information can be retrieved in the ObAnPluginFn
 *  function by calling the GetCredFn function of pFnBlock.
 *
 *  ObAnPluginRequestResource
 *  The resource string, for example: "www.oracle.com/index.html".
 *
 *  ObAnPluginRequestOperation
 *  The operation being performed on the resource.
 *
 *  ObAnPluginRequesterIP
 *  The IP address of the client that issued this request.
 *
 *  ObAnPluginRequesterDN
 *  If an authentication plugin has set the DN, this is where
 *  other plugins can access that DN.  The plugin,
 *  credential_mapping, always sets the DN.  Custom
 *  authentication plugins can set the DN by calling SetAuthnUidFn.
 */
#define ObAnPluginRequestResource "Resource"
#define ObAnPluginRequestOperation "Operation"
#define ObAnPluginRequesterIP "RequesterIP"
#define ObAnPluginRequesterDN "RequesterDN"
#define ObAnHostTarget "HostTarget"

/*********************************************************************/
/***  Definitions of various data passed to and handled by plugin  ***/
/*********************************************************************/

/*  A handle to an opaque data structure containing single value
 *  data. Contents can be retrieved using ObAnPluginGetCred or 
 *  ObAnPluginGetAction.  Contents can be set using ObAnPluginSetCred
 *  or ObAnPluginSetAction methods.
 */
typedef const void* ObAnPluginSVData_t;


/*  A handle to an opaque data structure containing multi value
 *  data. Contents can be retrieved using ObAnPluginGetData
 *  and modified using ObAnPluginSetData.
 */
typedef void* ObAnPluginMVData_t;


/*  A null-terminated string that a plugin function can return to
 *  report on the result of the function.  This will be logged by
 *  the Oracle Access Server product.
 */
typedef char** ObAnPluginStatusMsg_t;


/* Access Server related information the plugin may need to use.
 *
 * AccessServerInstallDir:         Installation directory of Netpoint
 *                                 Access Server. Does not include oblix/.
 * AccessServerAnPluginAPIVersion: Lowest authn plugin API version the
 *                                 Access Server currently supports.
 */
struct ObAnServerContext {
  char *AccessServerInstallDir;
  char *AccessServerAnPluginAPIVersion;
};
typedef struct ObAnServerContext const *ObAnServerContext_t;


/* A handle to an opaque data structure containing all the information
 * the plugin may need or modify.
 *
 * Creds	  Creds is all information submitted by the entity (user
 *                or application) trying to access a resource.  Also
 *		  included is any requester specific information such
 *                as ResourceType, Resource, Operation, RequesterDN,
 *                and RequesterIP.  The plugin may add or replace this
 *                data.
 *
 * Params         Parameters specified in the plugin configuration.
 *                The plugins should not modify or add to this data.
 *
 * Context        Plugin specific data. This data is passed to the
 *                next plugin in sequence and hence can be used by 
 *		  the plugin to send information to plugins following 
 *		  it. The plugin may add or replace this data.
 *
 * ActionInfo     Action information passed from the plugin to Access 
 *		  Server.  The plugin may add or replace this data.
 */
struct ObAnPluginInfo {
  ObAnPluginSVData_t  Creds;
  ObAnPluginMVData_t  Params;
  ObAnPluginMVData_t  Context;
  ObAnPluginSVData_t  ActionInfo;
};
typedef struct ObAnPluginInfo* ObAnPluginInfo_t;




/*********************************************************************/
/******************  Functions to manipulate data  *******************/
/*********************************************************************/

/*  A handle to an opaque data structure containing information as a
 *  list of values.
 */
typedef void const* ObAnPluginList_t;


/*  A handle to an opaque data structure containing one item from a
 *  list of values.
 */
typedef void const* ObAnPluginListItem_t;


/* Function Pointers */
typedef ObAnPluginList_t (*ObAnPluginGetData_t) (ObAnPluginMVData_t, const char*);
typedef ObAnASStatus_t (*ObAnPluginSetData_t) (ObAnPluginMVData_t, const char*, const char*, const int);
typedef ObAnPluginListItem_t (*ObAnPluginGetFirstItem_t) (ObAnPluginList_t);
typedef const char* (*ObAnPluginGetValue_t) (ObAnPluginListItem_t);
typedef ObAnPluginListItem_t (*ObAnPluginGetNext_t) (ObAnPluginListItem_t);

typedef const char* (*ObAnPluginGetCred_t) (ObAnPluginSVData_t, const char*);
typedef ObAnASStatus_t (*ObAnPluginSetCred_t) (ObAnPluginSVData_t, const char*, const char*);

typedef const char* (*ObAnPluginGetAction_t) (ObAnPluginSVData_t, const char*, ObAnActionType_t);
typedef ObAnASStatus_t (*ObAnPluginSetAction_t) (ObAnPluginSVData_t, const char*, const char*, ObAnActionType_t);

typedef ObAnASStatus_t (*ObAnPluginSetAuthnUid_t) (char*);


/*  ObAnPluginGetData_t
 *
 *  Function to retrieve a list of multi value data (ObAnPluginMVData_t).
 *
 *  Parameters:
 *  pRequesterInfo  Handle to multi value data passed to the plugin.
 *  pName           Key/name for the information to retrieve.
 *
 *  Returns:
 *  A list of values for the given key. Plugin must use the list
 *  manipulation methods: GetFirstItemFn, GetNextFn and GetValueFn,
 *  to retreive information from this list.
 */
ObAnPluginList_t AnGetData(ObAnPluginMVData_t data, const char* name);


/*  ObAnPluginSetData_t
 *
 *  Function to set information in the multi value data
 *  (ObAnPluginMVData_t) passed to the plugin.
 *
 *  Parameters:
 *  pRequestContext  Handle to data passed to the plugin.
 *  pName            Key/name for the information to set.
 *  pValue           Value of the key.
 *  replace          Specifies whether to replace or add on to
 *                   existing values of key. A value of '0',
 *                   indicates addition, all other values indicate
 *                   a replace.
 *
 *  Returns:
 *  ObAnASStatusSuccess:  Returned if set was successful.
 *  ObAnASStatusFailed:   Returned if set was not successful.
 */
ObAnASStatus_t AnSetData(ObAnPluginMVData_t data, const char* name, const char* value, const int replace);


/*  Function to get a handle to the first item in a list of values.
 *
 *  Parameter:
 *  IN  pList    List of values.
 *
 *  Returns:
 *  Handle to an item. The plugin must use GetNextFn and GetValueFn
 *  to get all the values.
 */
ObAnPluginListItem_t AnGetFirstItem(ObAnPluginList_t list);


/* Function to get value from the item.
 *
 * Parameter:
 * IN  pItem  Handle to an Item returned by GetFirstItemFn or GetNextFn.
 *
 * Returns:
 * Value of the item.
 */
const char* AnGetValue(ObAnPluginListItem_t item);


/* Function to get a handle to the next item in the list.
 *
 * Parameter:
 * IN  pItem    Handle to the current item in the list.
 *
 * Returns:
 * Handle to the next item in the list.
 */
ObAnPluginListItem_t AnGetNext(ObAnPluginListItem_t item);


/*  ObAnPluginGetCred_t
 *
 *  Function to retrieve credential information.
 *
 *  Parameters:
 *  pCreds	Handle to credential info passed to the plugin.
 *  pName       Key/name for the credential info to retrieve.
 *
 *  Returns:
 *  The value for the given key/name or null if not found.
 *  Multiple values for a given key/name are not allowed in credentials.
 */
const char *AnGetCred(ObAnPluginSVData_t pCreds, const char* pName);


/*  ObAnPluginSetCred_t
 *
 *  Function to set information in the credentials passed to the
 *  plugin.  If the key/name already exists, the corresponding
 *  value will be replaced with pValue.
 *
 *  Parameters:
 *  pCreds	Handle to credentials passed to the plugin.
 *  pName       Key/name for the information to set.
 *  pValue      Value of the key.
 *
 *  Returns:
 *  ObAnASStatusSuccess:  Returned if set was successful.
 *  ObAnASStatusFailed:   Returned if set was not successful.
 */
ObAnASStatus_t AnSetCred (ObAnPluginSVData_t pCreds, const char* pName, const char* pValue);


/*  ObAnPluginGetAction_t
 *
 *  Function to retrieve action information.
 *
 *  Parameters:
 *  pActionInfo  Handle to action info passed to the plugin.
 *  pName        Key/name for the action info to retrieve.
 *  pActionType  Type of action info to set.
 *
 *  Returns:
 *  The value for the given key/name or null if not found.
 *  Multiple values for a given key/name are not allowed in action info.
 */
const char *AnGetAction(ObAnPluginSVData_t pActionInfo, const char* pName, ObAnActionType_t pActionType);


/*  ObAnPluginSetAction_t
 *
 *  Function to set information in the action info passed to the
 *  plugin.  If the key/name already exists, the corresponding
 *  value will be replaced with pValue.
 *
 *  Parameters:
 *  pCreds	 Handle to credentials passed to the plugin.
 *  pName        Key/name for the information to set.
 *  pValue       Value of the key.
 *  pActionType  Type of action info to set.
 *
 *  Returns:
 *  ObAnASStatusSuccess:  Returned if set was successful.
 *  ObAnASStatusFailed:   Returned if set was not successful.
 */
ObAnASStatus_t AnSetAction (ObAnPluginSVData_t pCreds, const char* pName, const char* pValue, ObAnActionType_t pActionType);


/*  ObAnPluginSetAuthnUid_t
 *
 *  Function to set the uid that is internal to authentication
 *  for the current user.  If the uid has already been set, the 
 *  corresponding value will be replaced with pUid.
 *
 *  Parameters:
 *  pUid	The new uid to be set
 *
 *  Returns:
 *  ObAnASStatusSuccess:  Returned if set was successful.
 *  ObAnASStatusFailed:   Returned if set was not successful.
 */
ObAnASStatus_t AnSetAuthnUid(char* pUid);


/* Block of functions supported by Access Server that the plugin may
 * need to use to manipulate data in opaque structures.
 *
 * GetDataFn:      Pointer to a function to get values for a given "name".
 * SetDataFn:      Pointer to a function to set values for a given "name".
 * GetFirstItemFn: Pointer to a function to get first item from a list.
 * GetValueFn:     Pointer to a function to get value from an item.
 * GetNextFn:      Pointer to a function to get next item on the list.
 *
 * GetCredFn:      Pointer to a function to get a credential value 
 *                 for a given "name".
 * SetCredFn:      Pointer to a function to set a credential for a 
 *                 given "name".
 * GetActionFn:    Pointer to a function to get an action value for a
 *                 given "name" and action type.
 * SetActionFn:    Pointer to a function to set an action value for a
 *                 given "name" and action type.
 * SetAuthnUidFn:  Pointer to a function to set the internal uid for
 *                 the current user to the given "uid".
 */
struct ObAnPluginFns {
  ObAnPluginGetData_t GetDataFn;
  ObAnPluginSetData_t SetDataFn;
  ObAnPluginGetFirstItem_t GetFirstItemFn;
  ObAnPluginGetValue_t GetValueFn;
  ObAnPluginGetNext_t GetNextFn;
  ObAnPluginGetCred_t GetCredFn;
  ObAnPluginSetCred_t SetCredFn;
  ObAnPluginGetAction_t GetActionFn;
  ObAnPluginSetAction_t SetActionFn;
  ObAnPluginSetAuthnUid_t SetAuthnUidFn;
};
typedef struct ObAnPluginFns* ObAnPluginFns_t;



/*********************************************************************/
/******************  Definitions of API functions  *******************/
/*********************************************************************/

/* Function pointers */
typedef const char* (*ObAnPluginGetVersion_t) (void);
typedef ObAnPluginStatus_t (*ObAnPluginInit_t) (ObAnServerContext_t, ObAnPluginStatusMsg_t);
typedef ObAnPluginStatus_t (*ObAnPluginTerminate_t) (ObAnServerContext_t, ObAnPluginStatusMsg_t);
typedef ObAnPluginStatus_t (*ObAnPluginFn_t) (ObAnServerContext_t, ObAnPluginFns_t, ObAnPluginInfo_t, ObAnPluginStatusMsg_t);
typedef void (*ObAnPluginDeallocStatusMsg_t) (ObAnPluginStatusMsg_t);


/* ObAnPluginGetVersion
 *  Returns version of an authentication plugin.
 *  The Access Server may use this version to determine if it can 
 *  support this version of the plugin.
 *
 *  Returns:
 *  Version of the authentication plugin.
 */
OBDLLEXPORT const char* ObAnPluginGetVersion(void);


/*  ObAnPluginInit
 *
 *  Defines an initialization function for an authentication 
 *  plugin. It will be called when the NetPoint Access Server
 *  product starts up.
 *
 *  Parameters:
 *  IN   pServerContext Context information of the Access Server.
 *  OUT  pResult        Result message reported by the function;
 *                      will be logged.
 *
 *  Returns:
 *  ObAnPluginStatusContinue: on successful initialization.
 *  ObAnPluginStatusAbort:    on failure.
 */
OBDLLEXPORT ObAnPluginStatus_t ObAnPluginInit (ObAnServerContext_t, ObAnPluginStatusMsg_t);


/*  ObAnPluginTerminate
 *
 *  Defines a termination function for an authentication plugin
 *  to be called when the NetPoint Access Server terminates.
 *  The termination function can clean up as needed for the custom 
 *  plugin.
 *
 *  Parameters:
 *  IN   pServerContext Context information of the Access Server.
 *  OUT  pResult        Result message reported by the function;
 *                      will be logged.
 *
 *  Returns:
 *  ObAnPluginStatusContinue: on successful termination.
 *  ObAnPluginStatusAbort:    on failure.
 */
OBDLLEXPORT ObAnPluginStatus_t ObAnPluginTerminate (ObAnServerContext_t, ObAnPluginStatusMsg_t);


/* ObAnPluginFn
 *
 *  Defines a custom authentication function, to be called during the 
 *  authentication process. A custom function can perform additional 
 *  authentication processing utilizing the plugin information and
 *  server context.  This function can also modify plugin information 
 *  for subsequent authentication plugins.
 *
 *  Parameters:
 *  IN  pServerContext  Context information of the Access Server.
 *  IN  pFuncBlock      Handle to a block of functions the plugin 
 *		        may need to use to manipulate data.
 *  IN/OUT  pData       Handle to data passed to the plugin and 
 *                      modified by the plugin.
 *  OUT  pResult        Result message reported by the function;
 *                      will be logged.
 *
 *  Returns:
 *  ObAnPluginStatusContinue: Signals the Access Server to move on 
 *  to the next plugin in sequence. This means that the plugin
 *  did not explicitly allow or deny access to requester.
 *
 *  ObAnPluginStatusAllowed: Requester is allowed access to target.
 *  Access Server stops evaluating authentication plugins.
 *
 *  ObAnPluginStatusDenied: Requester is denied access to target.
 *  Access Server stops evaluating authentication plugins.
 *
 *  ObAnPluginStatusAbort: Processing will not continue after the
 *  function and authentication will fail.
 */
OBDLLEXPORT ObAnPluginStatus_t ObAnPluginFn (ObAnServerContext_t, ObAnPluginFns_t, ObAnPluginInfo_t, ObAnPluginStatusMsg_t);


/* ObAnPluginDeallocStatusMsg
 *   Deallocates memory for status message. This is called by the
 *   Access Server to delete the memory allocated by the plugin 
 *   for the status message.
 *
 *   Parmeters:
 *   IN  pStatusMsg    Status Message to be deallocated.
 *
 *   Return: None
 *
 */
OBDLLEXPORT void ObAnPluginDeallocStatusMsg(ObAnPluginStatusMsg_t);

#ifdef __cplusplus
} /* extern C */

#endif

#endif
