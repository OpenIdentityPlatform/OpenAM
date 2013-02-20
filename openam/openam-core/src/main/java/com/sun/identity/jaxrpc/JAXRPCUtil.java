/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: JAXRPCUtil.java,v 1.5 2008/06/25 05:43:34 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.jaxrpc;

import com.sun.identity.shared.jaxrpc.JAXRPCHelper;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.HashMap;
import javax.xml.rpc.Stub;

/**
 * The class <code>JAXRPCUtil</code> provides functions to get JAXRPC stubs to
 * a valid Identity Server. The function
 * <code>getRemoteStub(String serviceName)</code> returns a JAXRPC stub to the
 * service. It is expected that the service caches the stub and re-uses it until
 * the server has failed. Upon server failure, the service needs to call the
 * function <code>serverFailed
 * (String serviceName)</code>, and the next call
 * to <code>
 * getRemoteStub(String serviceName)</code> will check for next valid
 * server and will return a stub that is currently active or throws
 * <code>java.rmi.RemoteException</code> if no servers are available.
 */
public class JAXRPCUtil extends JAXRPCHelper {

    private static HashMap remoteStubs = new HashMap();

    /**
     * Returns a valid JAXRPC end point for the given service name. If no valid
     * servers are found, it throws <code>java.rmi.RemoteException</code>.
     */
    public static Object getRemoteStub(String serviceName)
            throws RemoteException {
        Object answer = null;
        if (serverFailed || (answer = remoteStubs.get(serviceName)) == null) {
            answer = getValidStub(serviceName);
            serverFailed = false;
        }
        return (answer);
    }

    /**
     * Sets the service to be failed.
     */
    public static void serverFailed(String serviceName) {
        if (serviceName.startsWith(validRemoteURL)) {
            serverFailed = true;
        } else {
            // Could be serviceName
            remoteStubs.remove(serviceName);
        }
    }

    protected synchronized static Object getValidStub(String serviceName)
            throws RemoteException {
        Object stub = getServiceEndPoint(getValidURL(serviceName));
        // Add to cache
        remoteStubs.put(serviceName, stub);
        return (stub);
    }

    protected static Object getServiceEndPoint(String iurl) {
        if (debug.messageEnabled()) {
            debug.message("JAXRPCUtil Endpoint URL: " + iurl);
        }

        // Obtaining the stub for JAX-RPC and setting the endpoint URL
        Stub s = null;
        try {
            // Due to compilation errors, this function has been
            // made to use reflections
            Class imsClass = Class.forName(
                "com.sun.identity.jaxrpc.IdentityManagementServices_Impl");
            Object imsImpl = imsClass.newInstance();
            Method method = null;
            if (iurl.endsWith(SMS_SERVICE)) {
                // Obtain the method "getSMSObjectIFPort" and invoke it
                method = imsClass.getMethod("getSMSObjectIFPort",(Class[])null);
            } // %%% Add other service names here

            // Obtain the stub to be returned
            s = (Stub)method.invoke(imsImpl, (Object[])null);
        } catch (ClassNotFoundException cnfe) {
            if (debug.warningEnabled()) {
                debug.warning("JAXRPCUtil: unable to find class "
                        + "IdentityManagementServices_Impl", cnfe);
            }
        } catch (InstantiationException ne) {
            if (debug.warningEnabled()) {
                debug.warning("JAXRPCUtil: unable to instantiate class "
                        + "IdentityManagementServices_Impl", ne);
            }
        } catch (IllegalAccessException iae) {
            if (debug.warningEnabled()) {
                debug.warning("JAXRPCUtil: Illegal access to class "
                        + "IdentityManagementServices_Impl", iae);
            }
        } catch (Throwable t) {
            if (debug.warningEnabled()) {
                debug.warning("JAXRPCUtil:getServiceEndPoint exception", t);
            }
        }

        // Set the remote URL for the service
        if (s != null) {
            s._setProperty(javax.xml.rpc.Stub.ENDPOINT_ADDRESS_PROPERTY, iurl);
        }
        return (s);
    }
}
