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
#include "../url.h"
#include "../internal_exception.h"

using namespace smi;

bool useOld = false;
bool verbose = false;
bool checkParsedURL = true;

struct {
    const char *url;
    const char *pathInfo;
    const char *expectedOutput;
    const char *expectedBaseURL;
} test_cases[] = { 
    { 
      "ftp://piras.red.iplanet.com", 
      NULL,
      "ftp://piras.red.iplanet.com:21",
      NULL
    }, 
    { 
      "http://host.red.iplanet.com/", 
      NULL,
      "http://host.red.iplanet.com:80/",
      NULL
    },
    { 
      "https://piras.red.iplanet.com:80/index.html", 
      NULL,
      "https://piras.red.iplanet.com:80/index.html",
      NULL
    }, 
    { 
      "http://piras.red.iplanet.com/index.html?x=c", 
      NULL,
      "http://piras.red.iplanet.com:80/index.html?x=c",
      NULL
    }, 
    { 
      "https://piras.red.iplanet.com/index.html?a2=b1&a2=b2&a1=b1&a1=b2",
      NULL,
      "https://piras.red.iplanet.com:443/index.html?a1=b1&a1=b2&a2=b1&a2=b2",
      NULL
    }, 
    { 
      "http://xyz.sun.com:80/index.html?v123=123&va=a&va=A&VA=A&v=xyz&v3=3",
      NULL,
      NULL,
      NULL
    }, 
    { 
      "http://whatever.red.iplanet.com:88/some/app/path/info",
      "/path/info",
      "http://whatever.red.iplanet.com:88/some/app/path/info",
      NULL
    }, 
    { 
      "http://whatever.red.iplanet.com:88/some/app/path/info?with=query&str=",
      "/path/info",
      "http://whatever.red.iplanet.com:88/some/app/path/info?str=&with=query",
      "http://whatever.red.iplanet.com:88/some/app"
    }, 
    // end mark
    {
      NULL, NULL, NULL, NULL
    }
};

int do_test(int round_num, int test_num, 
	    const char *urlInput, const char *pathInfo, 
	    const char *expectedOutput, const char *expectedBaseURL)
{
    int ok = 1;
    try {
	std::string urlInputStr = urlInput;
	std::string pathInfoStr = (pathInfo==NULL)?"":pathInfo;
	URL urlObj(urlInputStr, pathInfoStr, true, useOld);
	if (checkParsedURL) {
	    if (expectedOutput == NULL) {
		if (verbose) {
		    std::string parsed_str;
		    urlObj.getURLString(parsed_str);
		    printf("URL output: %s\n", parsed_str.c_str());
		}
	    }
	    else {
		std::string parsed_str;
	       	urlObj.getURLString(parsed_str);
		if (strcmp(parsed_str.c_str(), expectedOutput) != 0) {
		    if (round_num > 0) {
			printf("Round %d, Test %d: ", round_num, test_num);
		    }
		    printf("Incorrect output: %s\nExpected output: %s\n",
			    parsed_str.c_str(), expectedOutput);
		    ok = 0;
		} 
	    }
	    if (expectedBaseURL == NULL) {
		if (verbose) {
		    std::string base_url;
		    urlObj.getBaseURL(base_url);
		    printf("Base URL: %s\n", base_url.c_str());
		}
	    }
	    else {
		std::string base_url;
	       	urlObj.getBaseURL(base_url);
		if (strcmp(base_url.c_str(), expectedBaseURL) != 0) {
		    if (round_num > 0) {
			printf("Round %d, Test %d: ", round_num, test_num);
		    }
		    printf("Incorrect base URL: %s\nExpected base URL: %s\n",
			    base_url.c_str(), expectedBaseURL);
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
	    "\t[-b <expected base url>]\texpected base url to check\n"
	    "\t[-v]\tverbose print a message for every test that succeeded\n"
	    "\t[-c]\ttest URL constructor only - do not check output\n"
	    "\t[-o]\tuse old parse URL algorithm (for performance testing)\n"
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
	    case 'p':
                path_info = (j <= argc-1) ? argv[++j] : NULL;
		if (path_info == NULL) {
		    usage++;
		}
		break;
	    case 'b':
                base_url = (j <= argc-1) ? argv[++j] : NULL;
		if (base_url == NULL) {
		    usage++;
		}
		break;
	    case 'o':
                useOld = true;  // use old
		break;
	    case 'v':
                verbose = true;
		break;
	    case 'c':
                checkParsedURL = false;
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
	if (do_test(-1, -1, test_url, path_info, NULL, base_url)) {
	    failed++;
	}
	exit(0);
    }

    for (unsigned long int k = 0; k < nloops; k++) {
	for (int i = 0; ; i++) {
	    if (test_cases[i].url == NULL)
		break;
	    if (do_test(k, i, test_cases[i].url, 
			test_cases[i].pathInfo,
			test_cases[i].expectedOutput,
			test_cases[i].expectedBaseURL)) {
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
