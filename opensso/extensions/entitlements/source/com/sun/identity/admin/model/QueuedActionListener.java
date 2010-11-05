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
 * $Id: QueuedActionListener.java,v 1.3 2009/06/04 11:49:17 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.util.ArrayList;
import java.util.List;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class QueuedActionListener implements PhaseListener {
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    public void beforePhase(PhaseEvent pe) {
        checkForOperations(true, pe);
    }

    public void afterPhase(PhaseEvent pe) {
        checkForOperations(false, pe);
    }

    private void checkForOperations(boolean doBeforePhase, PhaseEvent evt) {
        FacesContext fc = FacesContext.getCurrentInstance();
        QueuedActionBean qab = (QueuedActionBean)fc.getApplication().createValueBinding("#{queuedActionBean}").getValue(fc);
        List<PhaseEventAction> invoked = new ArrayList<PhaseEventAction>();
        for (PhaseEventAction pea : qab.getPhaseEventActions()) {
            if (pea.getPhaseId() == evt.getPhaseId()) {
                if (pea.isDoBeforePhase() == doBeforePhase) {
                    javax.faces.application.Application a = fc.getApplication();
                    MethodBinding mb = a.createMethodBinding(pea.getAction(), pea.getParameters());
                    if (mb != null) {
                        mb.invoke(fc, pea.getArguments());
                        invoked.add(pea);
                    }
                }
            }
        }
        qab.getPhaseEventActions().removeAll(invoked);
    }
}