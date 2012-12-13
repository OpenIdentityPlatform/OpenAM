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
 * $Id: connection.h,v 1.5 2010/03/10 05:09:38 dknab Exp $
 *
 *
 * Abstract:
 *
 * This class encapsulates a TCP socket connection.
 *
 */

#ifndef CONNECTION_H
#define CONNECTION_H

#include <cstdlib>
#include <string>

#include <prio.h>

#include <am_types.h>
#include "internal_macros.h"
#include "nspr_exception.h"
#include "log.h"
#include "properties.h"
#include "server_info.h"

BEGIN_PRIVATE_NAMESPACE

class Connection {
public:
    //
    // Creates connection object that encapsulates a TCP socket connection.
    //
    // Parameters:
    //   server
    //		the end point information for the remote end of the connection
    //
    //   certDBPasswd
    //          certificate database password
    //
    //   alwaysTrustServerCert
    //		if true, then always trust the server's certificate when
    //		establishing an SSL connection.
    //
    // Throws:
    //   NSPRException if the requested connection cannot be established
    //
    Connection(const ServerInfo& server, const std::string &certDBPasswd,
	       const std::string &certNickName,
	       bool alwaysTrustServerCert); 

    //
    // Destroys the connection object and closes any associated socket
    // connection.
    //
    ~Connection();

    //
    // Sends an array of bytes to the remote end of the connection.
    //
    // Parameters:
    //   data	points to the bytes to be sent
    //
    //   len	the length of the data to be sent.  If the length is zero,
    //		then the data array is assumed to be NUL terminated and
    //		strlen is used to determine the length of the data.
    //
    // Returns:
    //   AM_SUCCESS
    //		if all of the data is sent successfully
    //
    //   AM_INVALID_ARGUMENT
    //		if the data parameter is NULL
    //
    //   AM_NSPR_ERROR
    //		if PR_Write returns an error.  PR_GetError() can be used to
    //		retrieve more information about the error.
    //
    am_status_t sendData(const char *data, std::size_t len = 0);

    //
    // Receives data from the remote end of the connection.
    //
    // Parameters:
    //   buffer points to a buffer where the received data should be stored
    //
    //	 bufferLen
    //		On entry, it specifies the length of the buffer pointed to
    //		by the buffer parameter.  On return, it specifies the
    //		actual number bytes of data stored.
    //
    // Returns:
    //   AM_SUCCESS
    //		if no error is detected.  If the method returns
    //		AM_SUCCESS and bufferLen is zero, then the end of the
    //		data stream has been reached for the connection.
    //
    //   AM_INVALID_ARGUMENT
    //		if the buffer parameter is NULL or the bufferLen argument
    //		is 0.
    //
    //   AM_NSPR_ERROR
    //		if PR_Read returns an error.  PR_GetError() can be used to
    //		retrieve more information about the error.
    //
    am_status_t receiveData(char *buffer, std::size_t& bufferLen);

    //
    // Waits for a reply to be received and returns an allocated buffer of
    // memory containing any received data.  The method reads until EOF is
    // encountered or an error occurs.  For the convience of the caller, a
    // NUL character is placed after the received data.  The NUL character
    // is not included in the reported length of the returned data.
    //
    // Parameters:
    //   buffer A reference to the pointer where the address of the
    //		allocated buffer should be stored.
    //
    //   receivedLen
    //		A reference to the variable where the size of the received
    //		data should be stored.
    //
    //	 initialBufferLen
    //		If non-zero, the size of the initial buffer to allocate for
    //		the received data.
    //
    // Returns:
    //   AM_SUCCESS
    //		if no error is detected.  If the method returns
    //		AM_SUCCESS and bufferLen is zero, then the end of the
    //		data stream has been reached for the connection.
    //
    //   AM_NO_MEMORY
    //		if unable to allocate memory for the reply buffer
    //
    //   AM_NSPR_ERROR
    //		if PR_Read returns an error.  PR_GetError() can be used to
    //		retrieve more information about the error.
    //
    am_status_t waitForReply(char *&reply, std::size_t& receivedLen,
				std::size_t initialBufferLen = 0);

    am_status_t waitForReply(char *&reply, std::size_t initialBufferLen,
				std::size_t offset, std::size_t& receivedLen);

    //
    // Initializes the underlying libraries that are used to create
    // SSL connections.
    //
    // Parameters:
    //   properties
    //		A Properties object containing the configuration
    //		information needed to initialize the connection module
    //		including, if necessary, the underlying SSL libraries.
    //
    // Returns:
    //   AM_SUCCESS
    //		if no error is detected.
    //
    //   AM_NSPR_ERROR
    //		if any of the SSL library initialization routines returns
    //		an error.  PR_GetError() can be used to retrieve more
    //		information about the error.
    //
    //   AM_INVALID_ARGUMENT
    //		if the certDir argument is NULL
    //
    // NOTE: This function is called automatically by am_init.
    //
    static am_status_t initialize(const Properties& properties);
    static am_status_t initialize_in_child_process(const Properties& properties);

    //
    // Cleans up the underlying libraries.
    //
    // Parameters:
    //   None.
    //
    // Returns:
    //    AM_SUCCESS
    //		if no error is detected
    //
    // NOTE: This function is called automatically by am_cleanup.
    //
    static am_status_t shutdown(void);
    static am_status_t shutdown_in_child_process(void);

    //
    // Performs SSL handshake on a TCP socket
    //
    PRFileDesc *secureSocket(const std::string &certDBPasswd,
                             const std::string &certNickName,
                             bool alwaysTrustServerCert,
                             PRFileDesc *rawSocket);


private:
    Connection(const Connection&);		// Not implemented
    Connection& operator=(const Connection&);	// Not implemented

   /**
    * Throws NSPRException upon NSPR error
    */
    PRFileDesc *createSocket(const PRNetAddr& address, bool useSSL,
			     const std::string &certDBPasswd,
			     const std::string &certNickName,
			     bool alwaysTrustServerCert); 

    char *growBuffer(char *oldBuffer, std::size_t oldBufferLen,
		     std::size_t newBufferLen);
    am_status_t read(char *buffer, std::size_t& bufferLen);

    static bool initialized;
    PRFileDesc *socket;
    char *certdbpasswd;
    char *certnickname;
};

END_PRIVATE_NAMESPACE

#endif	/* not CONNECTION_H */
