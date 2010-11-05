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
 * $Id: OrSubject.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * EntitlementSubject wrapper on a set of EntitlementSubject(s) to provide 
 * boolean OR logic Membership is of OrSubject is satisfied if the user is
 * a member of any of the wrapped EntitlementSubject
 */
public class OrSubject extends LogicalSubject {

    /**
     * Constructs OrSubject
     */
    public OrSubject() {
        super();
    }

    /**
     * Constructs OrSubject
     * @param eSubjects wrapped EntitlementSubject(s)
     */
    public OrSubject(Set<EntitlementSubject> eSubjects) {
        super(eSubjects);
    }

    /**
     * Constructs OrSubject
     * @param eSubjects wrapped EntitlementSubject(s)
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when UserESubject was created from
     * OpenSSO policy Subject
     */
    public OrSubject(Set<EntitlementSubject> eSubjects, String pSubjectName) {
        super(eSubjects, pSubjectName);
    }

    /**
     * Returns <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     *
     * @param realm Realm name.
     * @param subject EntitlementSubject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     * @throws EntitlementException if any errors occur.
     */
    public SubjectDecision evaluate(
        String realm,
        SubjectAttributesManager mgr,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        SubjectDecision result = new SubjectDecision(false,
            Collections.EMPTY_MAP);
        
        Set<EntitlementSubject> eSubjects = getESubjects();
        if ((eSubjects != null) && !eSubjects.isEmpty()) {
            for (EntitlementSubject e : eSubjects) {
                SubjectDecision decision = e.evaluate(realm, mgr, subject,
                    resourceName, environment);
                if (decision.isSatisfied()) {
                    return decision;
                }
                if (result == null) {
                    result = decision;
                } else {
                    Map advices = result.getAdvices();
                    Map dAdvices = decision.getAdvices();

                    if ((dAdvices != null) && !dAdvices.isEmpty()) {
                        if ((advices == null) || advices.isEmpty()) {
                            result = new SubjectDecision(false, dAdvices);
                        } else {
                            advices.putAll(dAdvices);
                            result = new SubjectDecision(false, advices);
                        }
                    }
                 }
            }
        }
        return result;
    }
}
