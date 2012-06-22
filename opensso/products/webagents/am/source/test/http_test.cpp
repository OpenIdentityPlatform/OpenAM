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
#include <stdexcept>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include "../internal_exception.h"
#include "../http.h"

using namespace smi;

bool useOld = false;
bool verbose = false;
bool checkEncodedURL = true;

struct {
    const char *url;
    const char *expectedEncodedURL;
    const char *expectedDecodedURL;
} test_cases[] = { 
    {
    "http://a.b.c/d?a%20b",
    "http%3A%2F%2Fa.b.c%2Fd%3Fa%2520b",
    "http://a.b.c/d?a b"
    },
    {
    "http://a.b.c/d?a b",
    "http%3A%2F%2Fa.b.c%2Fd%3Fa+b",
    "http://a.b.c/d?a b"
    },
    {
    "http://a.b.c/d?a+b",
    "http%3A%2F%2Fa.b.c%2Fd%3Fa%2Bb",
    "http://a.b.c/d?a b"
    },
    {
    "http://a.b.c/d?a%20b",
    "http%3A%2F%2Fa.b.c%2Fd%3Fa%2520b",
    "http://a.b.c/d?a b"
    },
    {
    "http%3A%2F%2Fa.b.c%2Fd%3Fa%2520b",
    "http%253A%252F%252Fa.b.c%252Fd%253Fa%252520b",
    "http://a.b.c/d?a%20b"
    },
    // end mark
    {
      NULL, NULL, NULL 
    }
};

int do_test(int round_num, int test_num, 
	    const char *urlInput, 
	    const char *expectedEncodedURL,
	    const char *expectedDecodedURL)
{
    int ok = 1;
    try {
	std::string urlInputStr = urlInput;
	std::string encodedStr = Http::encode(urlInputStr);
	std::string decodedStr = Http::decode(urlInputStr);
	if (checkEncodedURL) {
	    if (expectedEncodedURL == NULL) {
		if (verbose) {
		    printf("Encoded URL: %s\n", encodedStr.c_str());
		    printf("Decoded URL: %s\n", decodedStr.c_str());
		}
	    }
	    else {
		if (strcmp(encodedStr.c_str(), expectedEncodedURL) != 0) {
		    if (round_num > 0) {
			printf("Round %d, Test %d: ", round_num, test_num);
		    }
		    printf("Incorrect encoded output: %s\n"
			   "Expected encoded output: %s\n",
			   encodedStr.c_str(), expectedEncodedURL);
		    ok = 0;
		} 
		if (strcmp(decodedStr.c_str(), expectedDecodedURL) != 0) {
		    if (round_num > 0) {
			printf("Round %d, Test %d: ", round_num, test_num);
		    }
		    printf("Incorrect decoded output: %s\n"
			   "Expected decoded output: %s\n",
			   encodedStr.c_str(), expectedDecodedURL);
		    ok = 0;
		}
	    }
	} 
    }
    catch (InternalException& exi) {
	if (round_num > 0) {
	    printf("Round %d, Test %d: ", round_num, test_num);
	}
	printf("Internal Exception: %s\n", exi.getMessage());
	ok = 0;
    }
    catch (std::exception& exs) {
	if (round_num > 0) {
	    printf("Round %d, Test %d: ", round_num, test_num);
	}
	printf("Exception: %s\n", exs.what());
	ok = 0;
    }
    catch (...) {
	if (round_num > 0) {
	    printf("Round %d, Test %d: ", round_num, test_num);
	}
	printf("Unknown Exception\n");
	ok = 0;
    }
    return !ok;
}

void Usage(char **argv)
{
    fprintf(stderr, "Usage: %s\n", argv[0]);
    fprintf(stderr, 
	    "\t[-l <num-loops>]\trun standard set of tests num_loops times\n"
	    "\t[-t <test-url>]\ttest with the given URL only\n"
	    "\t[-p <test-path-info>]\ttest with the given path-info\n"
	    "\t[-v]\tverbose print a message for every test that succeeded\n"
	   );
}


int
main(int argc, char **argv) 
{
    char *test_url = NULL;
    char *path_info = NULL;
    char *nloops_val = NULL;
    char *bad_nloops_val = NULL;
    char *base_url = NULL;
    unsigned long int nloops = 1;
    int failed = 0;
    int j;
    char c = -1;
    int usage = 0;

    for (j=1; j < argc; j++) {
        if (*argv[j]=='-') {
            c = argv[j][1];
            switch (c) {
	    case 't':
                test_url = (j <= argc-1) ? argv[++j] : NULL;
		verbose = true;
		if (test_url == NULL) {
		    usage++;
		}
		break;
	    case 'v':
                verbose = true;
		break;
	    case 'c':
                checkEncodedURL = false;
		break;
	    case 'l':
                nloops_val = (j <= argc-1) ? argv[++j] : NULL;
		nloops = strtoul(nloops_val, &bad_nloops_val, 10);
		if (bad_nloops_val != NULL && *bad_nloops_val != '\0') {
		    usage++;
		}
		break;
	    default:
		usage++;
		break;
	    }
	    if (usage)
		break;
        }
        else {
            usage++;
            break;
        }
    }
    if (usage) {
	Usage(argv);
	exit(1);
    }

    if (test_url != NULL) {
	if (do_test(-1, -1, test_url, NULL, NULL)) {
	    failed++;
	}
	exit(0);
    }

    for (unsigned long int k = 0; k < nloops; k++) {
	for (int i = 0; ; i++) {
	    if (test_cases[i].url == NULL)
		break;
	    if (do_test(k, i, test_cases[i].url, 
			test_cases[i].expectedEncodedURL,
			test_cases[i].expectedDecodedURL)) {
		failed++;
		if (verbose) 
		    printf("Round %d, Test %d: Failed\n", k, i);
	    }
	    else if (verbose) {
		printf("Round %d, Test %d: Passed\n", k, i);
	    }
	}
    }

    if (!failed) {
	printf("%u round(s) of tests passed.\n", nloops);
    }
return 0;
}
