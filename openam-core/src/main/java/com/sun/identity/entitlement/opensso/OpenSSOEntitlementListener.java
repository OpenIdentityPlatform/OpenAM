/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OpenSSOEntitlementListener.java,v 1.4 2009/12/15 00:44:19 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementListener;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.interfaces.IEntitlementListenerRegistry;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.security.auth.Subject;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenSSOEntitlementListener 
    implements IEntitlementListenerRegistry {
    private static final String ATTR_NAME = "listeners";
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();

    public void addListener(Subject adminSubject, EntitlementListener l)
        throws EntitlementException {
        for (String applName : l.getMapAppToRes().keySet()) {
            if (!doesApplicationExist(applName)) {
                String[] params = {applName};
                throw new EntitlementException(431, params);
            }
        }
        List<EntitlementListener> listeners = getListeners();
        boolean combined = false;

        for (EntitlementListener listener : listeners) {
            if (listener.combine(l)) {
                combined = true;
                break;
            }
        }
        
        if (!combined) {
            listeners.add(l);
        }

        storeListeners(listeners);
    }

    public boolean removeListener(Subject adminSubject, String url)
        throws EntitlementException {

        if (url == null) {
            throw new EntitlementException(436);
        }

        try {
            URL urlObj = new URL(url);

            boolean removed = false;
            List<EntitlementListener> listeners = getListeners();

            for (int i = listeners.size() - 1; i >= 0; --i) {
                EntitlementListener l = listeners.get(i);
                if (l.getUrl().equals(urlObj)) {
                    listeners.remove(l);
                    removed = true;
                    break;
                }
            }

            storeListeners(listeners);
            return removed;
        } catch (MalformedURLException e) {
            throw new EntitlementException(435);
        }
    }

    private void storeListeners(List<EntitlementListener> listeners) 
        throws EntitlementException {
        rwlock.writeLock().lock();
        try {
            AttributeSchema as = getAttributeSchema();
            Set<String> values = new HashSet<String>();

            for (EntitlementListener l : listeners) {
                values.add(l.toJSON().toString());
            }

            as.setDefaultValues(values);
        } catch (JSONException e) {
            throw new EntitlementException(426, e);
        } catch (SMSException e) {
            throw new EntitlementException(426, e);
        } catch (SSOException e) {
            throw new EntitlementException(427, e);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private List<EntitlementListener> getListeners()
        throws EntitlementException {
        List<EntitlementListener> listeners = 
            new ArrayList<EntitlementListener>();

        try {
            AttributeSchema as = getAttributeSchema();
            Set<String> values = as.getDefaultValues();

            if (values != null) {
                for (String v : values) {
                    listeners.add(new EntitlementListener(new JSONObject(v)));
                }
            }
        } catch (JSONException e) {
            throw new EntitlementException(426, e);
        } catch (SMSException e) {
            throw new EntitlementException(426, e);
        } catch (SSOException e) {
            throw new EntitlementException(427, e);
        }

        return listeners;
    }

    private AttributeSchema getAttributeSchema()
        throws SMSException, SSOException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        ServiceSchemaManager mgr = new ServiceSchemaManager(
            EntitlementService.SERVICE_NAME, adminToken);
        ServiceSchema globalSchema = mgr.getGlobalSchema();
        return globalSchema.getAttributeSchema(ATTR_NAME);
    }

    private boolean doesApplicationExist(String applName) 
        throws EntitlementException {
        Set<String> names = ApplicationManager.getApplicationNames(
            PrivilegeManager.superAdminSubject, "/");
        return names.contains(applName);
    }

    public Set<EntitlementListener> getListeners(Subject adminSubject)
        throws EntitlementException {
        rwlock.readLock().lock();

        try {
            Set<EntitlementListener> listeners =
                new HashSet<EntitlementListener>();
            listeners.addAll(getListeners());
            return listeners;
        } finally {
            rwlock.readLock().unlock();
        }
    }
}
