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
 * $Id: LinkTrapGenerator.java,v 1.1 2009/06/19 02:23:15 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.monitoring;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * The LinkTrapGenerator class is used by the agent to trigger the 
 * emission of SNMP v1 or v3 traps at a specific rate and according 
 * to a specific entry from the "IfTable".
 */

public class LinkTrapGenerator extends Thread 
    implements LinkTrapGeneratorMBean, MBeanRegistration {

    // MBean properties.
    //
    private int ifIndex = -1;
    private int nbTraps = -1;
    private int interval = 2000; /* 2 seconds */
    private int successes = 0;
    private int errors = 0;

    /**
     * Constructors.
     */
    public LinkTrapGenerator(int nbTraps) {

        super();
        this.nbTraps = nbTraps;
        java.lang.System.out.println("LinkTrapGenerator() called");
        java.lang.System.out.println("LinkTrapGenerator() returned\n");
    }

    public LinkTrapGenerator(int ifIndex, int nbTraps) {

        java.lang.System.out.println("LinkTrapGenerator(int ifIndex) called");

        this.ifIndex = ifIndex;
        this.nbTraps = nbTraps;

        java.lang.System.out.println("LinkTrapGenerator(int ifIndex) " +
                                     "returned\n");
    }
    
    /**
     * MBean registration interface implementation.
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name) 
        throws java.lang.Exception {
        
        java.lang.System.out.println("LinkTrapGenerator.preRegister() called");

        // Create an object name if it was not given.
        //
        if (name == null) {
            name = new ObjectName("trapGenerator:class=" + 
                                  this.getClass().getName() + ",ifIndex=" + 
                                  ifIndex);
        }

        // Extract the value of ifIndex from the object name.
        //
        try {
            ifIndex=(new Integer(name.getKeyProperty("ifIndex"))).intValue();
        } catch (Exception e){
            ifIndex = 1;
            java.lang.System.out.println("Use default ifIndex = " + ifIndex);
        }
        
        java.lang.System.out.println("LinkTrapGenerator.preRegister() " +
                                     "returned\n");
        return name;
    }
            
    public void postRegister (Boolean registrationDone) {
    } 

    public void preDeregister() throws java.lang.Exception {
        
        // Stop the thread.
        //
        this.interrupt();
        try {
            // Wait until the thread die.
            //
            this.join();
        } catch (InterruptedException e) {
            // Ignore...
        }
    }

    public void postDeregister() {
    }
    
    public Integer getIfIndex() {
        return new Integer(ifIndex);
    }

    public void setIfIndex(Integer x) {
        ifIndex = x.intValue();
    }

    public Integer getSuccesses() {
        return new Integer(successes);
    }

    public Integer getErrors() {
        return new Integer(errors);
    }

    public Integer getInterval() {
        return new Integer(interval);
    }

    public void setInterval(Integer val) {
        interval = val.intValue();
    }

    public void run() {
        
        // If nbTraps = -1, the link trap generator will trigger traps 
        // continuously.
        // Otherwise, the link trap generator will trigger traps nbTraps
        // times.
        //
        int remainingTraps = nbTraps;
        while ((nbTraps == -1) || (remainingTraps > 0)) {
            try {
                sleep(interval);
            } catch (Exception e) {
                e.printStackTrace();
            }
            triggerTrap();
            remainingTraps--;
        }
    }

    public void triggerTrap() {
        // TODO
        //IfEntryImpl ifEntryImpl = InterfacesImpl.find(ifIndex);
        //if (ifEntryImpl == null) {
            //java.lang.System.err.println(
                                 //"ERROR: LinkTrapGenerator.triggerTrap(): " +
                                 //"ifIndex " + ifIndex + " not found");
            //errors++;
            //return;
        //}
        //ifEntryImpl.switchifOperStatus();
        successes++;
    }
}
