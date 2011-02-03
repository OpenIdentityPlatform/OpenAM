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
 *
 */ 
#include "properties.h"
#include <stdio.h>
#include <stdexcept>
using namespace smi;
int main(int argc, char **argv) {
    bool pass = true;
    try {
	const char *func;
	Properties::const_iterator iter;
	Properties ip(true);
	ip.set("key1", "value1");
	ip.set("key3", "value3");
	ip.set("key2", "value2");

	if(ip.size() != 3) {
	    func = "sub test-0";
	    pass = false;
	}

	if(pass) {
	    func = "sub test-1";
	    iter = ip.find("key1");
	    if(iter == ip.end()) {
		pass = false;
	    } else {
		pass = ((*iter).second == "value1");
	    }
	}

	if(pass) {
	    func = "sub test-2";
	    iter = ip.find("keY2");
	    if(iter == ip.end()) {
		pass = false;
	    } else {
		pass = ((*iter).second == "value2");
	    }
	}

	if(pass && ip.vfind("value") != ip.end()) {
	    func = "sub test-3";
	    pass = false;
	}

	if(pass && ip.vfind("value5") != ip.end()) {
	    func = "sub test-4";
	    pass = false;
	}

	if(pass && (iter = ip.vfind("value3")) == ip.end() &&
	   (*iter).first != "key3") {
	    func = "sub test-5";
	    pass = false;
	}

	if(pass && (iter = ip.vfind("vAlUe2")) == ip.end() &&
	   (*iter).first != "key2") {
	    func = "sub test-5";
	    pass = false;
	}

	if(!pass) {
	    printf("%s: failed. ", func);
	}

    } catch(std::exception &ex) {
	printf("Threw an exception %s.\n", ex.what());
	pass = false;
    }

    if(pass) {
	printf("Test passed\n");
    } else {
	printf("Test failed\n");
    }
    return (pass != true);
}
