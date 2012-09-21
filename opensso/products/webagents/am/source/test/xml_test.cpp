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

#include <cstdio>
#include <cstdlib>

#if	!defined(WINNT)
#include <unistd.h>
#endif
#include <sys/types.h>
#include <sys/stat.h>

#include "am.h"

#include "xml_tree.h"
#include "utilities.h"

USING_PRIVATE_NAMESPACE

int main(int argc, char **argv)
{
    if (argc > 1) {
	FILE *inputFile;

	inputFile = fopen(argv[1], "r");
	if (inputFile) {
	    struct stat statBuf;
	    int rc;

	    rc = fstat(fileno(inputFile), &statBuf);
	    if (rc == 0) {
		char *buffer = new char[statBuf.st_size + 1];
		std::size_t readCount;

		readCount = fread(buffer, 1, statBuf.st_size, inputFile);
		if (readCount == static_cast<std::size_t>(statBuf.st_size)) {
		    buffer[readCount] = '\0';

		    XMLTree::initialize();
		    XMLTree tree(false, buffer, readCount);

		    tree.log(Log::addModule(argv[0]), Log::LOG_ALWAYS);

		    XMLTree::shutdown();
		} else {
		    fatal("%s: short read %u != %ld", argv[0], readCount,
			  statBuf.st_size);
		}
	    } else {
		fatal("%s: unable to fstat %s", argv[0], argv[1]);
	    }

	    fclose(inputFile);
	} else {
	    fatal("%s: unable to open %s\n", argv[0], argv[1]);
	}
    } else {
	fatal("Usage: %s <xml-file-name>", argv[0]);
    }

    return EXIT_SUCCESS;
}
