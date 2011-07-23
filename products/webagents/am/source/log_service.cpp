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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 *
 */ 

#include "log_service.h"
#include "log_record.h"
#include "xml_tree.h"
#include "utils.h"
#include "scope_lock.h"
#include "http.h"

USING_PRIVATE_NAMESPACE

namespace {

    const char requestPrefix[] = {
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        "<RequestSet vers=\"1.0\" svcid=\"Logging\" reqid=\""
    };

    const char logRequestPrefix[] = {
        "\">\n"
          "<Request><![CDATA[\n"
            "<logRecWrite reqid=\""
    };

    const char additionalRequestPrefix[] = {
          "<Request><![CDATA[\n"
            "<logRecWrite reqid=\""
    };

    const char logLogPrefix[] = {
        "\">"
        "<log logName=\""
    };

    const char logSidPrefix[] = {
        "\" sid=\""
    };

    const char logLogSuffix[] = {
        "\"></log>"
    };

    const char logRecordPrefix[] = {
        "<logRecord>"
    };

    const char logRecordType[] = {
        "<recType>Agent</recType>"
    };

    const char logLevelPrefix[] = {
        "<level>"
    };

    const char logLevelSuffix[] = {
        "</level>"
    };

    const char logRecMsgPrefix[] = {
        "<recMsg>"
    };

    const char logRecMsgSuffix[] = {
        "</recMsg>"
    };

    const char logInfoMapPrefix[] = {
        "<logInfoMap>"
    };

    const char logInfoKeyPrefix[] = {
        "<logInfo><infoKey>"
    };

    const char logInfoKeySuffix[] = {
        "</infoKey>"
    };

    const char logInfoValuePrefix[] = {
        "<infoValue>"
    };

    const char logInfoValueSuffix[] = {
        "</infoValue></logInfo>"
    };

    const char logInfoMapSuffix[] = {
        "</logInfoMap>"
    };

    const char logRecordSuffix[] = {
        "</logRecord>"
    };

    const char requestSuffix[] = {
        "</logRecWrite>]]></Request>\n"
    };

    const char requestSetSuffix[] = {
        "</RequestSet>\n\n"
    };

}

const LogService::BodyChunk
LogService::requestPrefixChunk(requestPrefix, sizeof(requestPrefix) - 1);

const LogService::BodyChunk
LogService::logRequestPrefixChunk(logRequestPrefix,
                                  sizeof(logRequestPrefix) - 1);

const LogService::BodyChunk
LogService::additionalRequestPrefixChunk(additionalRequestPrefix,
                                  sizeof(additionalRequestPrefix) - 1);

const LogService::BodyChunk
LogService::logLogPrefixChunk(logLogPrefix, sizeof(logLogPrefix) - 1);

const LogService::BodyChunk
LogService::logSidPrefixChunk(logSidPrefix, sizeof(logSidPrefix) - 1);

const LogService::BodyChunk
LogService::logLogSuffixChunk(logLogSuffix, sizeof(logLogSuffix) - 1);

const LogService::BodyChunk
LogService::logRecordPrefixChunk(logRecordPrefix,
                                 sizeof(logRecordPrefix) - 1);
const LogService::BodyChunk
LogService::logRecordTypeChunk(logRecordType,
                                 sizeof(logRecordType) - 1);
const LogService::BodyChunk
LogService::logRecordSuffixChunk(logRecordSuffix, sizeof(logRecordSuffix) - 1);

const LogService::BodyChunk
LogService::logLevelPrefixChunk(logLevelPrefix, sizeof(logLevelPrefix) - 1);

const LogService::BodyChunk
LogService::logLevelSuffixChunk(logLevelSuffix, sizeof(logLevelSuffix) - 1);

const LogService::BodyChunk
LogService::logRecMsgPrefixChunk(logRecMsgPrefix, sizeof(logRecMsgPrefix) - 1);

const LogService::BodyChunk
LogService::logRecMsgSuffixChunk(logRecMsgSuffix, sizeof(logRecMsgSuffix) - 1);

const LogService::BodyChunk
LogService::logInfoMapPrefixChunk(logInfoMapPrefix, sizeof(logInfoMapPrefix) - 1);

const LogService::BodyChunk
LogService::logInfoMapSuffixChunk(logInfoMapSuffix, sizeof(logInfoMapSuffix) - 1);

const LogService::BodyChunk
LogService::logInfoKeyPrefixChunk(logInfoKeyPrefix, sizeof(logInfoKeyPrefix) - 1);

const LogService::BodyChunk
LogService::logInfoKeySuffixChunk(logInfoKeySuffix, sizeof(logInfoKeySuffix) - 1);

const LogService::BodyChunk
LogService::logInfoValuePrefixChunk(logInfoValuePrefix, sizeof(logInfoValuePrefix) - 1);

const LogService::BodyChunk
LogService::logInfoValueSuffixChunk(logInfoValueSuffix, sizeof(logInfoValueSuffix) - 1);

const LogService::BodyChunk
LogService::requestSuffixChunk(requestSuffix, sizeof(requestSuffix) - 1);

const LogService::BodyChunk
LogService::requestSetSuffixChunk(requestSetSuffix, sizeof(requestSetSuffix) - 1);

LogService::LogService(const ServiceInfo &svcInfo, const SSOToken &ssoToken,
		       const Http::CookieList &ckieList,
		       const std::string &logFileName,
		       const Properties &svcParams,
                       const std::string &cert_passwd,
                       const std::string &cert_nick_name,
                       bool trustServerCert):
    BaseService("LogService", svcParams, cert_passwd, cert_nick_name,
		trustServerCert), 
    serviceInfo(svcInfo),
    loggedByToken(ssoToken), 
    cookieList(ckieList), 
    remoteLogName(logFileName),
    bufferSize(1),
    bufferCount(0),
    remoteBodyChunkListInitialized(false)
{ 
}

LogService::LogService(const ServiceInfo &svcInfo,
		               const Properties &svcParams,
                       const std::string &cert_passwd,
                       const std::string &cert_nick_name,
                       bool trustServerCert,
                       unsigned int buffSize):
    BaseService("LogService", svcParams, cert_passwd, cert_nick_name,
		trustServerCert),
    serviceInfo(svcInfo),
    loggedByToken(SSOToken()),
    cookieList(Http::CookieList()),
    remoteLogName(""),
    bufferSize(buffSize), 
    bufferCount(0), 
    remoteBodyChunkListInitialized(false) 
{ 
}

LogService::~LogService()
{
}


// this does not throw any exceptions since it returns a status. 
am_status_t LogService::logMessage(const std::string & message) 
    throw()
{
    am_status_t retVal = AM_SUCCESS;

    if(loggedByToken.isValid()) {
	retVal = logMessage(serviceInfo, loggedByToken,
			    cookieList, message, remoteLogName);
    } else {
	Log::log(logModule, Log::LOG_ERROR,
		 "LogService::logMessage() "
 		 "loggedBy SSOTokenID is invalid.");
	retVal = AM_REMOTE_LOG_FAILURE;
    }

    return retVal;
}

// this does not throw any exceptions since it returns a status. 
am_status_t LogService::logMessage(const ServiceInfo& service,
				      const SSOToken& logged_by_token,
				      const Http::CookieList& ckieList,
				      const std::string& message,
				      const std::string& logname)
    throw() 
{
    am_status_t status = AM_SUCCESS;
    encodedMessage = NULL;
    //The encoded log message needs to be in multiple of 4 bytes.
    encodedMessage = (char *)malloc(((message.size() * 4/3 + 1)/4 + 1)*4 + 4);
    if(encodedMessage != NULL) {
	encode_base64(message.c_str(), message.size(), encodedMessage);
    } else {
	status = AM_NO_MEMORY;
    }


    if (status == AM_SUCCESS) {
	if(logged_by_token.isValid()) {
	const std::size_t NUM_EXTRA_CHUNKS = 10;
	Request request(*this, requestPrefixChunk, logRequestPrefixChunk,
			NUM_EXTRA_CHUNKS);
	Http::Response response;

        BodyChunkList& bodyChunkList = request.getBodyChunkList();
	bodyChunkList.push_back(logLogPrefixChunk);
        bodyChunkList.push_back(BodyChunk(logname));
	bodyChunkList.push_back(logSidPrefixChunk);
        bodyChunkList.push_back(BodyChunk(logged_by_token.getString()));
	bodyChunkList.push_back(logLogSuffixChunk);
	bodyChunkList.push_back(logRecordPrefixChunk);
	bodyChunkList.push_back(logRecordTypeChunk);
	bodyChunkList.push_back(logRecMsgPrefixChunk);

	/* Ensuring message is correctly entity ref'ed. */
	std::string t_string(encodedMessage);
	free(encodedMessage);
	Utils::expandEntityRefs(t_string);
        bodyChunkList.push_back(BodyChunk(t_string));

	bodyChunkList.push_back(logRecMsgSuffixChunk);
        bodyChunkList.push_back(logRecordSuffixChunk);
	bodyChunkList.push_back(requestSuffixChunk);
	bodyChunkList.push_back(requestSetSuffixChunk);

	status = doHttpPost(service, std::string(), ckieList,
			    bodyChunkList, response);

	if (AM_SUCCESS == status) {
            try {
	        std::vector<std::string> loggingResponses;
                // don't know if the log response xml shares the basic/generic
                // format.
	        loggingResponses = parseGenericResponse(response,
						       request.getGlobalId());
                status = AM_ERROR_PARSING_XML;
                if (loggingResponses.empty()) {
                    status = AM_ERROR_PARSING_XML;
                } else {
		    status = AM_SUCCESS;
                    // return success only if all responses are successful
                    for (std::size_t i = 0; i < loggingResponses.size(); ++i) {
                        if (strstr(loggingResponses[i].c_str(), "OK")) {
			    continue;
                        }
                        else if (strstr(loggingResponses[i].c_str(),
                                        "INVALID_SESSION")) {
                            status = AM_ACCESS_DENIED;
			    break;
                        }
                        else if (strstr(loggingResponses[i].c_str(),
                                        "UNAUTHORIZED")) {
                            status = AM_ACCESS_DENIED;
			    break;
			}
                        else if (strstr(loggingResponses[i].c_str(), "ERROR")) {
                            status = AM_REMOTE_LOG_FAILURE;
			    break;
                        }
			else {
			    // unrecognized response
                    	    status = AM_ERROR_PARSING_XML;
			    break;
			}
                    }
                }
            } catch (const XMLTree::ParseException& exc) {
	        Log::log(logModule, Log::LOG_ERROR,
		         "LogService::logMessage() caught exception: %s",
                         exc.getMessage().c_str());
                status = AM_ERROR_PARSING_XML;
            } catch (std::exception& exs) {
	        Log::log(logModule, Log::LOG_ERROR,
		         "LogService::logMessage() caught exception: %s",
                         exs.what());
                status = AM_ERROR_PARSING_XML;
	    } catch (...) {
	        Log::log(logModule, Log::LOG_ERROR,
		         "LogService::logMessage() caught unknown exception");
                status = AM_ERROR_PARSING_XML;
	    }
        }
    } else {
	status = AM_INVALID_ARGUMENT;
    }
    } else {
	status = AM_NO_MEMORY;
    }
    return status;
}

// this does not throw exceptions since it returns a status.
am_status_t LogService::sendLog(const std::string& logName,
                                const LogRecord& record,
                                const std::string& loggedByTokenID)
    throw()
{
    am_status_t status = AM_FAILURE;
    const std::string& theLogName = 
	logName.empty() ? remoteLogName : logName;
    const std::string& theLoggedByTokenID = 
	loggedByTokenID.empty() ? loggedByToken.getString() : loggedByTokenID;

    try {
	if (theLogName.empty() || theLoggedByTokenID.empty()) {
	    status = AM_INVALID_ARGUMENT;
	}
	else {
	    status = addLogDetails(theLogName, record, theLoggedByTokenID);

	    if (status == AM_SUCCESS && bufferCount >= bufferSize) {
	       status = flushBuffer();
            }
	}
    }
    catch (InternalException& exi) {
	Log::log(logModule, Log::LOG_ERROR,
	    "LogService::sendLog() caught exception: %s",
	     exi.getMessage());
	status = exi.getStatusCode();
    }
    catch (std::exception& exs) {
	Log::log(logModule, Log::LOG_ERROR,
	    "LogService::sendLog() caught exception: %s",
	     exs.what());
	status = AM_FAILURE;
    }
    catch (...) {
	Log::log(logModule, Log::LOG_ERROR,
	    "LogService::sendLog() caught unknown exception");
	status = AM_FAILURE;
    }
    return status;
}

am_status_t LogService::addLogDetails(const std::string& logName,
                   const LogRecord& record,
                   const std::string& loggedByTokenID)
{
    ScopeLock scopeLock(mLock);
    char logLevel[32];
    am_status_t status = AM_SUCCESS;

    char *msg = NULL;
    std::string message = record.getLogMessage();
    //The encoded log message needs to be in multiple of 4 bytes.
    msg = (char *)malloc(((message.size() * 4/3 + 1)/4+1)*4 + 4);
    if(msg != NULL) {
	encode_base64(message.c_str(), message.size(), msg);
    } else {
	status = AM_NO_MEMORY;
    }

   if (status == AM_SUCCESS) { 
      if(!remoteBodyChunkListInitialized) {

        const std::size_t NUM_EXTRA_CHUNKS = 50;

        remoteRequest = new Request(*this, requestPrefixChunk, 
				    logRequestPrefixChunk, NUM_EXTRA_CHUNKS);
        if (remoteRequest != NULL) {
           remoteBodyChunkList = remoteRequest->getBodyChunkList();
           remoteBodyChunkListInitialized = true;
        }
     }

    if(bufferCount >= 1) {
	remoteBodyChunkList.push_back(additionalRequestPrefixChunk);

	BodyChunk temp;
	char serviceIdBuf[1024];
	if (remoteRequest != NULL) {
	   remoteRequest->getNextServiceRequestIdAsString(
			serviceIdBuf,  sizeof(serviceIdBuf));
        }
	temp.data = serviceIdBuf;
	remoteBodyChunkList.push_back(temp);
    }

    sprintf(logLevel, "%d", record.getLogLevel());

    remoteBodyChunkList.push_back(logLogPrefixChunk);
    remoteBodyChunkList.push_back(BodyChunk(logName));
    remoteBodyChunkList.push_back(logSidPrefixChunk);
    remoteBodyChunkList.push_back(BodyChunk(loggedByTokenID));
    remoteBodyChunkList.push_back(logLogSuffixChunk);
    remoteBodyChunkList.push_back(logRecordPrefixChunk);
    remoteBodyChunkList.push_back(logLevelPrefixChunk);
    remoteBodyChunkList.push_back(BodyChunk(std::string(logLevel)));
    remoteBodyChunkList.push_back(logLevelSuffixChunk);
    remoteBodyChunkList.push_back(logRecMsgPrefixChunk);

    std::string t_mesg(msg);
    free(msg);
    Utils::expandEntityRefs(t_mesg);

    remoteBodyChunkList.push_back(BodyChunk(t_mesg));
    remoteBodyChunkList.push_back(logRecMsgSuffixChunk);
    remoteBodyChunkList.push_back(logInfoMapPrefixChunk);

    const Properties &properties = record.getLogInfo();

    Properties::const_iterator iter = properties.begin();

    for(; iter != properties.end(); iter++) {
        const Properties::key_type &k_iter = iter->first;
        const Properties::mapped_type &v_iter = iter->second;
        std::string keyStr("");
        keyStr = k_iter.c_str();
        std::string valueStr("");
        valueStr = v_iter.c_str();
        remoteBodyChunkList.push_back(logInfoKeyPrefixChunk);
        remoteBodyChunkList.push_back(BodyChunk(keyStr));
        remoteBodyChunkList.push_back(logInfoKeySuffixChunk);
        remoteBodyChunkList.push_back(logInfoValuePrefixChunk);
        remoteBodyChunkList.push_back(BodyChunk(valueStr));
        remoteBodyChunkList.push_back(logInfoValueSuffixChunk);
    }

    remoteBodyChunkList.push_back(logInfoMapSuffixChunk);
    remoteBodyChunkList.push_back(logRecordSuffixChunk);
    remoteBodyChunkList.push_back(requestSuffixChunk);

    bufferCount++;
   } else {
     status = AM_NO_MEMORY;
   }
   return status;
}

am_status_t LogService::flushBuffer()
    throw()
{
    ScopeLock scopeLock(mLock);
    if(bufferCount <= 0 || !remoteBodyChunkListInitialized) {
        return AM_SUCCESS;
    }
    am_status_t status = AM_FAILURE;

    remoteBodyChunkList.push_back(requestSetSuffixChunk);
    Http::Response response;
    
    status = doHttpPost(serviceInfo, std::string(), cookieList,
        remoteBodyChunkList, response);

    if (status == AM_SUCCESS) {
        try {
            std::vector<std::string> loggingResponses;
	    if (remoteRequest != NULL) {
                loggingResponses =
                    parseGenericResponse(response, 
					 remoteRequest->getGlobalId());
            }

            status = AM_ERROR_PARSING_XML;

            if (loggingResponses.empty()) {
                status = AM_ERROR_PARSING_XML;
                // logging response is empty
            } else {
		status = AM_SUCCESS;
		// What if there are more than one logging response ? 
 		// status is success only if all responses are success.
		// otherwise set status to the first error encountered.
                for (std::size_t i = 0; i < loggingResponses.size(); ++i) {
                    if (strstr(loggingResponses[i].c_str(), "OK")) {
			continue;
                    }
                    else if (strstr(loggingResponses[i].c_str(), "ERROR")) {
			status = AM_REMOTE_LOG_FAILURE;
			break;
		    }
                    else if (strstr(loggingResponses[i].c_str(), 
				    "INVALID_SESSION")) {
			status = AM_ACCESS_DENIED;
			break;
		    }
                    else if (strstr(loggingResponses[i].c_str(), 
			 	    "UNAUTHORIZED")) {
			status = AM_ACCESS_DENIED;
			break;
		    }
		    else {
			// unknown response.
                 	status = AM_ERROR_PARSING_XML;
			break;
		    }
                }
            }
        } catch (const XMLTree::ParseException& exc) {
            Log::log(logModule, Log::LOG_ERROR,
                "LogService::flushBuffer() caught exception: %s",
                 exc.getMessage().c_str());
            status = AM_ERROR_PARSING_XML;
        } catch (std::exception& exs) {
            Log::log(logModule, Log::LOG_ERROR,
                "LogService::flushBuffer() caught exception: %s",
                 exs.what());
            status = AM_ERROR_PARSING_XML;
	} catch (...) {
            Log::log(logModule, Log::LOG_ERROR,
                "LogService::flushBuffer() caught unknown exception.");
            status = AM_ERROR_PARSING_XML;
	}
    }
    bufferCount = 0;
    remoteBodyChunkListInitialized = false;
    remoteBodyChunkList.clear();
    if (remoteRequest != NULL) {
        delete remoteRequest;
        remoteRequest = NULL;
    }
    return status;
}
