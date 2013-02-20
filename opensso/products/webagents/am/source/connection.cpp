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
 * $Id: connection.cpp,v 1.10 2010/03/10 05:09:38 dknab Exp $
 *
 */
/*
* Portions Copyrighted [2011] [ForgeRock AS]
*/
#include <stdexcept>
#include <prinit.h>
#include <plstr.h>
#include <nss.h>
extern "C" {
    // The header files included by this header file do not correctly wrap
    // all of their declarations with extern "C".  In particular, the
    // typedef in secmodt.h for the function argument to
    // PK11_SetPasswordFunc is not protected.
#   include <pk11func.h>
}
#include <prnetdb.h>
#include <ssl.h>
#include <sslproto.h>
#include <string>
#include <secmod.h>
#include <secerr.h>

#include "am.h"

#include "connection.h"
#include "log.h"
#include "nspr_exception.h"
#include "string_util.h"

USING_PRIVATE_NAMESPACE

namespace {
    enum {
	MAX_READ_WRITE_LEN = 0x7fffffff, // 2^31 - 1, max signed 32-bit integer
	MIN_BYTES_TO_READ = 512,
	DEFAULT_BUFFER_LEN = 1024,
	INCREMENT_LEN = 8192
    };

    extern "C" SECStatus acceptAnyCert(void *, PRFileDesc *, PRBool, PRBool)
    {
	return SECSuccess;
    }

    extern "C" char *getPasswordFromArg(PK11SlotInfo *, PRBool retry,
			     void *arg) {
	char *password = NULL;
	if (static_cast<void *>(NULL) != arg && ! retry) {
	    password = PL_strdup(static_cast<char *>(arg));

	    if (static_cast<char *>(NULL) == password) {
		Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
			 "unable to copy password in SSL callback");
	    }
	}

	return password;
    }

    SECStatus finishedHandshakeHandler(PRFileDesc *socket,
						  void *arg) {
	Log::log(Log::ALL_MODULES, Log::LOG_INFO,
		 "Successfully completed SSL handshake.");
	return SECSuccess;
    }

}

bool Connection::initialized;
PRIntervalTime receive_timeout;
PRIntervalTime connect_timeout;
bool tcp_nodelay_is_enabled = false;

std::string defaultHostName(" ");

am_status_t Connection::initialize(const Properties& properties)
{
    am_status_t status = AM_SUCCESS;
    SECStatus secStatus;

    if (! initialized) {
	const char *nssMethodName = "NSS_Initialize";
	std::string certDir = properties.get(AM_COMMON_SSL_CERT_DIR_PROPERTY, 
					     "");
	std::string dbPrefix = properties.get(AM_COMMON_CERT_DB_PREFIX_PROPERTY,
					      "");

	unsigned long timeout = properties.getUnsigned(AM_COMMON_RECEIVE_TIMEOUT_PROPERTY, 0);
	if (timeout > 0) {
	    receive_timeout = PR_MillisecondsToInterval(static_cast<PRInt32>(timeout));
    } else {
	    receive_timeout = PR_INTERVAL_NO_TIMEOUT;
	}

	unsigned long socket_timeout = properties.getUnsigned(AM_COMMON_CONNECT_TIMEOUT_PROPERTY, 0);
	if (socket_timeout > 0) {
	    connect_timeout = PR_MillisecondsToInterval(static_cast<PRInt32>(socket_timeout));
	} else {
	    connect_timeout = PR_INTERVAL_NO_TIMEOUT;
	}

	tcp_nodelay_is_enabled = properties.getBool(AM_COMMON_TCP_NODELAY_ENABLE_PROPERTY, false);
		
	//
	// Initialize the NSS libraries and enable use of all of the
	// cipher suites.  According to the NSS 3.3 API documentation
	// NSS_SetDomesticPolicy enables all of the cipher suites except
	// two: SSL_RSA_WITH_NULL_MD5 and SSL_FORTEZZA_DMS_WITH_NULL_SHA.
	// (It does not explain why those two are not enabled.)  The SSL
	// code in Agent Pack 1.0 explicitly enabled the first of the
	// disabled cipher suites.
	// We do not enable them here since null or no encryption is a 
	// potential security risk.
	//
	Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::initialize() "
		 "Connection timeout when receiving data = %i milliseconds",timeout);
	if (tcp_nodelay_is_enabled) {
		Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::initialize() "
		   "Socket option TCP_NODELAY is enabled");
	}
        
        PR_Init(PR_USER_THREAD, PR_PRIORITY_NORMAL, 0);

        if (certDir.length() != 0) {
            Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::initialize() "
		 "calling NSS_Initialize() with directory = \"%s\" and "
		 "prefix = \"%s\"", certDir.c_str(), dbPrefix.c_str());
	    nssMethodName = "NSS_Initialize";
            secStatus = NSS_Initialize(certDir.c_str(), dbPrefix.c_str(),
				   dbPrefix.c_str(), "secmod.db",
				   NSS_INIT_READONLY|NSS_INIT_FORCEOPEN);
        } else {
            Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::initialize() "
		 "CertDir and dbPrefix EMPTY -- Calling NSS_NoDB_Init");
	    nssMethodName = "NSS_NoDB_Init";
            secStatus = NSS_NoDB_Init(NULL);
        }

	if (SECSuccess == secStatus) {
	    nssMethodName = "NSS_SetDomesticPolicy";
	    secStatus = NSS_SetDomesticPolicy();
	}
        
        PK11_ConfigurePKCS11(NULL, NULL, NULL, "internal                         ", NULL, NULL, NULL, NULL, 8, 1);
        SSL_ShutdownServerSessionIDCache();

        SSL_ConfigMPServerSIDCache(NULL, 100, 86400L, NULL);
        SSL_ConfigServerSessionIDCache(NULL, 100, 86400L, NULL);
        SSL_ClearSessionCache();
        
	if (SECSuccess == secStatus) {
	    nssMethodName = "PK11_SetPasswordFunc";
	    PK11_SetPasswordFunc(getPasswordFromArg);

	    initialized = true;
	}
        if (secStatus != SECSuccess) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                     "Connection::initialize() unable to initialize SSL "
                     "libraries: %s returned %d", nssMethodName,
                     PR_GetError());
            status = AM_NSPR_ERROR;
        }
    }

    return status;
}

am_status_t Connection::initialize_in_child_process(const Properties& properties) {
    am_status_t status = AM_SUCCESS;
    PRErrorCode errcode = 1;
    SECStatus secStatus;

    Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::initialize_in_child_process() "
            "Restarting PKCS #11 module");

    SSL_InheritMPServerSIDCache(NULL);
    
    if (SECFailure == (secStatus = SECMOD_RestartModules(PR_FALSE))) {
        errcode = PORT_GetError();
        if (errcode != SEC_ERROR_NOT_INITIALIZED) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Could not restart TLS security modules: %d:%s",
                    errcode, PR_ErrorToString(errcode, PR_LANGUAGE_I_DEFAULT));
            status = AM_NSPR_ERROR;
        }
    }

    std::string certDir = properties.get(AM_COMMON_SSL_CERT_DIR_PROPERTY, "");
    std::string dbPrefix = properties.get(AM_COMMON_CERT_DB_PREFIX_PROPERTY, "");
    unsigned long timeout = properties.getUnsigned(AM_COMMON_RECEIVE_TIMEOUT_PROPERTY, 0);
    if (timeout > 0) {
        receive_timeout = PR_MillisecondsToInterval(static_cast<PRInt32> (timeout));
    }
    unsigned long socket_timeout = properties.getUnsigned(AM_COMMON_CONNECT_TIMEOUT_PROPERTY, 0);
    if (socket_timeout > 0) {
        connect_timeout = PR_MillisecondsToInterval(static_cast<PRInt32> (socket_timeout));
    } else {
        connect_timeout = PR_INTERVAL_NO_TIMEOUT;
    }
    tcp_nodelay_is_enabled = properties.getBool(AM_COMMON_TCP_NODELAY_ENABLE_PROPERTY, false);

    if (certDir.length() != 0) {
        Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::initialize_in_child_process() "
                "calling NSS_Initialize() with directory = \"%s\" and "
                "prefix = \"%s\"", certDir.c_str(), dbPrefix.c_str());
        secStatus = NSS_Initialize(certDir.c_str(), dbPrefix.c_str(),
                dbPrefix.c_str(), "secmod.db",
                NSS_INIT_READONLY | NSS_INIT_FORCEOPEN);
    } else {
        Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "Connection::initialize_in_child_process() "
                "CertDir and dbPrefix EMPTY -- Calling NSS_NoDB_Init");
        secStatus = NSS_NoDB_Init(NULL);
    }

    if (SECSuccess == secStatus) {
        secStatus = NSS_SetDomesticPolicy();
    }
    
    if (SECSuccess == secStatus) {
        PK11_SetPasswordFunc(getPasswordFromArg);
    }
    if (secStatus != SECSuccess) {
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                "Connection::initialize_in_child_process() unable to initialize SSL "
                "libraries: %d", PR_GetError());
        status = AM_NSPR_ERROR;
    }
    
    return status;
}

am_status_t Connection::shutdown(void) {
    SSL_ShutdownServerSessionIDCache();
    NSS_Shutdown();
    PR_Cleanup();
    return AM_SUCCESS;
}

am_status_t Connection::shutdown_in_child_process(void) {
    SSL_ClearSessionCache();
    NSS_Shutdown();
    return AM_SUCCESS;
}

/**
 * Throws NSPRException upon NSPR error
 */
PRFileDesc *Connection::createSocket(const PRNetAddr& address, bool useSSL,
				     const std::string &certDBPasswd,
				     const std::string &certNickName,
				     bool alwaysTrustServerCert) 
{
    PRFileDesc *rawSocket = PR_NewTCPSocket();
    
    if (tcp_nodelay_is_enabled) {
	   // set the TCP_NODELAY option to disable Nagle algorithm
	   PRSocketOptionData socket_opt;
	   socket_opt.option = PR_SockOpt_NoDelay;
	   socket_opt.value.no_delay = PR_TRUE;
	   PRStatus prStatus = PR_SetSocketOption(rawSocket, &socket_opt);
	   if (PR_SUCCESS != prStatus) {
	       throw NSPRException("Connection::Connection", "PR_SetSocketOption");
	   }
    }
    
    PRFileDesc *sslSocket;
    if (certDBPasswd.size() > 0) {
        certdbpasswd = strdup(certDBPasswd.c_str());
     }

    if (static_cast<PRFileDesc *>(NULL) != rawSocket) {
	if (useSSL) {
		sslSocket = secureSocket(certDBPasswd, certNickName, alwaysTrustServerCert, rawSocket);
	} else {
	    sslSocket = rawSocket;
	}
    } else {
	throw NSPRException("Connection::createSocket", "PR_NewTCPSocket");
    }

    return sslSocket;
}

/**
 * Performs SSL handshake on a TCP socket.
 */
PRFileDesc *Connection::secureSocket(const std::string &certDBPasswd,
	const std::string &certNickName,
	bool alwaysTrustServerCert,
	PRFileDesc *rawSocket) {
	bool upgradeExisting = false;
	// Use object's socket if none passed
	if (rawSocket == static_cast<PRFileDesc *>(NULL)) {
		rawSocket = socket;
		upgradeExisting = true;
	}
	PRFileDesc *sslSocket = SSL_ImportFD(NULL, rawSocket);
	if (static_cast<PRFileDesc *>(NULL) != sslSocket) {
	    SECStatus secStatus = SECSuccess;
	    const char *sslMethodName;
	
	    // In case there was any communication on the socket
	    // before the upgrade we should call a reset
	    if (upgradeExisting) {
                sslMethodName = "SSL_ResetHandshake";
                secStatus = SSL_ResetHandshake(sslSocket, false);
	    }

	    if (SECSuccess == secStatus) {
                sslMethodName = "SSL_OptionSet";
                {
                    PRBool state;
                    secStatus = SSL_OptionGet(sslSocket,SSL_SECURITY, &state);
                    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::secureSocket() " " SSL SSL_Security = %s",(state)?"true":"false");
                    secStatus = SSL_OptionGet(sslSocket,SSL_ENABLE_SSL3, &state);
                    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::secureSocket() " " SSL SSL_ENABLE_SSL3 = %s",(state)?"true":"false");
                    secStatus = SSL_OptionGet(sslSocket,SSL_ENABLE_SSL2, &state);
                    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, "Connection::secureSocket() " " SSL SSL_ENABLE_SSL2 = %s",(state)?"true":"false");
                }
                secStatus = SSL_OptionSet(sslSocket, SSL_SECURITY, PR_TRUE);
	    }

	    if (SECSuccess == secStatus) {
                secStatus = SSL_OptionSet(sslSocket,
                                      SSL_HANDSHAKE_AS_CLIENT, PR_TRUE);
                if (SECSuccess == secStatus && alwaysTrustServerCert) {
                    sslMethodName = "SSL_AuthCertificateHook";
                    secStatus = SSL_AuthCertificateHook(sslSocket, acceptAnyCert,
                                                    NULL);
                }

                if (SECSuccess == secStatus && certDBPasswd.size() > 0) {
                    sslMethodName = "SSL_SetPKCS11PinArg";
                    secStatus = SSL_SetPKCS11PinArg(sslSocket, certdbpasswd);
                }

                if (SECSuccess == secStatus) {
		    if (certNickName.size() > 0) {
                        certnickname = strdup(certNickName.c_str());
                    }
                    sslMethodName = "SSL_GetClientAuthDataHook";
                    secStatus = SSL_GetClientAuthDataHook(sslSocket,
                                                   NSS_GetClientAuthData,
                                                   static_cast<void *>(certnickname));
                }

                if (SECSuccess == secStatus) {
                    sslMethodName = "SSL_HandshakeCallback";
                    secStatus = SSL_HandshakeCallback(sslSocket,
                                (SSLHandshakeCallback)finishedHandshakeHandler,
                                 NULL);
		}
	    }

	    if (SECSuccess != secStatus) {
		PRErrorCode error = PR_GetError();

		PR_Close(sslSocket);
		throw NSPRException("Connection::secureSocket",
				sslMethodName, error);
	    }
	} else {
	    PRErrorCode error = PR_GetError();

	    PR_Close(rawSocket);
	    throw NSPRException("Connection::secureSocket", "SSL_ImportFD",
				error);
	}

	socket = sslSocket;
	return sslSocket;
}

/**
 * Throws NSPRException upon NSPR error
 */
Connection::Connection(const ServerInfo& server,
		       const std::string &certDBPasswd,
		       const std::string &certNickName,
		       bool alwaysTrustServerCert) 
    : socket(NULL), certdbpasswd(NULL), certnickname(NULL)
{
    char      buffer[PR_NETDB_BUF_SIZE];
    PRNetAddr address;
    PRHostEnt hostEntry;
    PRIntn    hostIndex;
    PRStatus	prStatus;
    SECStatus	secStatus;

    prStatus = PR_GetHostByName(server.getHost().c_str(), buffer,
				sizeof(buffer), &hostEntry);
    if (PR_SUCCESS != prStatus) {
        throw NSPRException("Connection::Connection", "PR_GetHostByName");
    }

    hostIndex = PR_EnumerateHostEnt(0, &hostEntry, server.getPort(), &address);
    if (hostIndex < 0) {
        throw NSPRException("Connection::Connection", "PR_EnumerateHostEnt");
    }

    socket = createSocket(address, server.useSSL(),
			  certDBPasswd,
			  certNickName,
			  alwaysTrustServerCert);

    if (server.useSSL()) {
	secStatus = SSL_SetURL(socket, server.getHost().c_str());
	if (SECSuccess != secStatus) {
	    PRErrorCode error = PR_GetError();

	    PR_Shutdown(socket, PR_SHUTDOWN_BOTH);
	    PR_Close(socket);
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		     "SSL_SetURL() returned error: %s",
		     PR_ErrorToString(error, PR_LANGUAGE_I_DEFAULT));
	    throw NSPRException("Connection::Connection",
				"SSL_SetURL", error);
	}
    }

    prStatus = PR_Connect(socket, &address, connect_timeout);

    if (prStatus != PR_SUCCESS) {
	PRErrorCode error = PR_GetError();

	PR_Shutdown(socket, PR_SHUTDOWN_BOTH);
	PR_Close(socket);
	throw NSPRException("Connection::Connection PR_Connect", "PR_Connect", error);
    }
}

Connection::~Connection()
{
    if (static_cast<PRFileDesc *>(NULL) != socket) {
	PR_Shutdown(socket, PR_SHUTDOWN_BOTH);
	PRStatus prStatus = PR_Close(socket);
	if(prStatus != PR_SUCCESS) {
	    PRErrorCode error = PR_GetError();
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		     "Connection::~Connection(): NSPR Error "
		     "while calling PR_Close(): %d.", error);
	}
	socket = static_cast<PRFileDesc *>(NULL);
    }
    if (NULL != certdbpasswd) {
        free(certdbpasswd);
        certdbpasswd = NULL;
    }
    if (NULL != certnickname) {
        free(certnickname);
        certnickname = NULL;
    }
}

am_status_t Connection::sendData(const char *data, std::size_t len)
{
    am_status_t status = AM_SUCCESS;

    if (static_cast<const char *>(NULL) != data) {
	if (0 == len) {
	    len = std::strlen(data);
	}

	// The NSPR code only allows passing a PRInt32 to specify the amount
	// of data to be sent.  A std::size_t can specify more than that
	// amount, especially on a 64-bit machine, so we put the
	// PR_Write call into a loop.

	PR_SetConcurrency(4);

	while (len > 0) {
	    PRInt32 bytesToWrite;
	    PRInt32 bytesWritten;

	    if (len > static_cast<std::size_t>(MAX_READ_WRITE_LEN)) {
		bytesToWrite = MAX_READ_WRITE_LEN;
	    } else {
		bytesToWrite = static_cast<PRInt32>(len);
	    }

	    bytesWritten = PR_Write(socket, data, bytesToWrite);
	    if (bytesWritten < 0) {
		status = AM_NSPR_ERROR;
		break;
	    }
	    data += bytesWritten;
	    len -= static_cast<std::size_t>(bytesWritten);
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

am_status_t Connection::receiveData(char *buffer, std::size_t& bufferLen)
{
    am_status_t status = AM_SUCCESS;

    if (static_cast<const char *>(NULL) != buffer && bufferLen > 0) {
	status = read(buffer, bufferLen);
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

am_status_t Connection::waitForReply(char *&reply, std::size_t& receivedLen,
					std::size_t bufferLen)
{
    reply = NULL;	// Make sure that this field is initialized.
    return waitForReply(reply, 0, bufferLen, receivedLen);
}

am_status_t Connection::waitForReply(char *&buffer,
					std::size_t bufferLen,
					std::size_t totalBytesRead,
					std::size_t& receivedLen)
{
    am_status_t status = AM_SUCCESS;

    if (static_cast<char *>(NULL) == buffer || 0 == bufferLen) {
	if (0 == bufferLen) {
	    bufferLen = DEFAULT_BUFFER_LEN;
	}
	totalBytesRead = 0;	// Ignore any value that was passed in.
	buffer = new (std::nothrow) char[bufferLen];
    }

    if (static_cast<char *>(NULL) != buffer) {
	std::size_t bytesToRead = bufferLen - totalBytesRead;

	status = read(&buffer[totalBytesRead], bytesToRead);
	while (status == AM_SUCCESS && bytesToRead > 0) {
	    totalBytesRead += bytesToRead;
	    bytesToRead = bufferLen - totalBytesRead;
	    if (bytesToRead <= MIN_BYTES_TO_READ) {
		// If the new buffer size is smaller than the INCREMENT_LEN
		// we grow the buffer by doubling the size.  If the current
		// size is less than the INCREMENT_LEN, but the new size is
		// greater than we use INCREMENT_LEN as the new buffer size,
		// otherwise add INCREMENT_LEN.  The order of the tests is
		// rearranged from the prose to reflect the expected
		// probability of each situation occurring.
		if ((2 * bufferLen) <= INCREMENT_LEN) {
		    bufferLen *= 2;
		} else if (bufferLen >= INCREMENT_LEN) {
		    bufferLen += INCREMENT_LEN;
		} else {
		    bufferLen = INCREMENT_LEN;
		}

		buffer = growBuffer(buffer, totalBytesRead, bufferLen);
		if (static_cast<char *>(NULL) == buffer) {
		    status = AM_NO_MEMORY;
		    break;
		}

		bytesToRead = bufferLen - totalBytesRead;
	    }

	    status = read(&buffer[totalBytesRead], bytesToRead);
	}

	if (AM_SUCCESS == status) {
	    if (totalBytesRead == bufferLen) {
		buffer = growBuffer(buffer, totalBytesRead,
				    totalBytesRead + 1);
		if (static_cast<char *>(NULL) == buffer) {
		    status = AM_NO_MEMORY;
		}
	    }

	    if (AM_SUCCESS == status) {
		buffer[totalBytesRead] = '\0';
		receivedLen = totalBytesRead;
	    }
	} 
    } else {
	status = AM_NO_MEMORY;
    }

    Log::log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG,
	     "Connection::waitForReply(): returns with status %s.",
	     am_status_to_string(status));

    return status;
}

char *Connection::growBuffer(char *oldBuffer, std::size_t oldBufferLen,
			     std::size_t newBufferLen)
{
    char *newBuffer = new (std::nothrow) char[newBufferLen];

    if (static_cast<char *>(NULL) != newBuffer) {
	std::memcpy(newBuffer, oldBuffer, oldBufferLen);
    }
    delete[] oldBuffer;

    return newBuffer;
}

am_status_t Connection::read(char *buffer, std::size_t& bufferLen)
{
    am_status_t status = AM_SUCCESS;
    PRInt32 bytesRead;
    PRInt32 bytesToRead;

    if (bufferLen > static_cast<std::size_t>(MAX_READ_WRITE_LEN)) {
	bytesToRead = MAX_READ_WRITE_LEN;
    } else {
	bytesToRead = static_cast<PRInt32>(bufferLen);
    }

    bytesRead = PR_Recv(socket, buffer, bytesToRead, 0, receive_timeout);
    if (bytesRead < 0) {
	status = AM_NSPR_ERROR;
	PRErrorCode error = PR_GetError();
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		 "Connection::read(): NSPR Error while reading data:%d", 
		 PR_GetError());
	if (error == PR_IO_TIMEOUT_ERROR || error == PR_CONNECT_RESET_ERROR ||
	    error == PR_CONNECT_REFUSED_ERROR ||
            error == PR_NETWORK_DOWN_ERROR ||
            error == PR_NETWORK_UNREACHABLE_ERROR || 
            error == PR_HOST_UNREACHABLE_ERROR) {
                throw NSPRException("Connection::read()", "PR_Recv");
	}
    } else {
	bufferLen = static_cast<std::size_t>(bytesRead);
    }

    return status;
}
