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
 * $Id: am_properties.cpp,v 1.6 2008/06/25 08:14:28 qcheng Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <stdexcept>

#include "am_properties.h"
#include "internal_macros.h"
#include "properties.h"

USING_PRIVATE_NAMESPACE

struct am_properties_iter {
    am_properties_iter(const Properties::const_iterator& initial,
			  const Properties::const_iterator& endArg)
	: current(initial), end(endArg)
    {
    }

    Properties::const_iterator current;
    Properties::const_iterator end;
};

extern "C" am_status_t
am_properties_create(am_properties_t *properties_ptr)
{
    am_status_t status;

    try {
	*properties_ptr = reinterpret_cast<am_properties_t>(new Properties);

	status = AM_SUCCESS;
    } catch (const std::bad_alloc&) {
	status = AM_NO_MEMORY;
    } catch (const std::exception &ex) {
	status = AM_FAILURE;
    } catch (...) {
	status = AM_FAILURE;
    }

    return status;
}

extern "C" am_status_t
am_properties_copy(am_properties_t source_properties,
		      am_properties_t *properties_ptr)
{
    const Properties *srcPtr = reinterpret_cast<Properties *>(source_properties);
    Properties **destPtrPtr = reinterpret_cast<Properties **>(properties_ptr);
    am_status_t status;

    try {
	*destPtrPtr = new Properties(*srcPtr);

	status = AM_SUCCESS;
    } catch (const std::bad_alloc&) {
	status = AM_NO_MEMORY;
    } catch (const std::exception&) {
	status = AM_FAILURE;
    } catch (...) {
	status = AM_FAILURE;
    }

    return status;
}

extern "C" void am_properties_destroy(am_properties_t properties)
{
    if (properties != NULL)
        delete reinterpret_cast<Properties *>(properties);
}

extern "C" am_status_t
am_properties_load(am_properties_t properties, const char *file_name)
{
    am_status_t status = AM_SUCCESS;
    Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && file_name && *file_name) {
	try {
	    status = propPtr->load(file_name);
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_store(am_properties_t properties, const char *file_name)
{
    am_status_t status;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && file_name && *file_name) {
	try {
	    status = propPtr->store(file_name);
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" boolean_t am_properties_is_set(am_properties_t properties,
					 const char *key)
{
    bool result;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key) {
	try {
	    result = propPtr->isSet(key);
	} catch (const std::exception&) {
	    result = false;
	} catch (...) {
	    result = false;
	}
    } else {
	result = false;
    }

    return (result==true?B_TRUE:B_FALSE);
}

extern "C" am_status_t
am_properties_get(am_properties_t properties, const char *key,
		     const char **value_ptr)
{
    am_status_t status;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value_ptr) {
	try {
	    *value_ptr = propPtr->get(key).c_str();
	    status = AM_SUCCESS;
	} catch (const std::invalid_argument&) {
	    status = AM_NOT_FOUND;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_get_with_default(am_properties_t properties,
				  const char *key, const char *default_value,
				  const char **value_ptr)
{
    am_status_t status = AM_SUCCESS;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value_ptr) {
	try {
	    std::string keyString(key);

	    if (propPtr->isSet(keyString)) {
		*value_ptr = propPtr->get(keyString).c_str();
	    } else {
		*value_ptr = default_value;
	    }
	    status = AM_SUCCESS;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_get_boolean_with_default(am_properties_t properties,
					  const char *key, am_bool_t default_value,
					  am_bool_t *value_ptr)
{
    am_status_t status = AM_SUCCESS;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value_ptr) {
	try {
	    *value_ptr = propPtr->getBool(key, default_value > 0 ? true : false) ? AM_TRUE : AM_FALSE;
	} catch (const std::domain_error &ex) {
	    status = AM_INVALID_VALUE;
	} catch (const std::range_error &ex) {
	    status = AM_INVALID_VALUE;
	} catch (const std::bad_alloc &ex) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception &ex) {
	    status = AM_FAILURE;
	} catch(...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_get_signed(am_properties_t properties,
				  const char *key, long *value_ptr)
{
    am_status_t status;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value_ptr) {
	try {
	    *value_ptr = propPtr->getSigned(key);
	    status = AM_SUCCESS;
	} catch (const std::invalid_argument&) {
	    status = AM_NOT_FOUND;
	} catch (const std::domain_error&) {
	    status = AM_INVALID_VALUE;
	} catch (const std::range_error&) {
	    status = AM_INVALID_VALUE;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_get_signed_with_default(am_properties_t properties,
					 const char *key, long default_value,
					 long *value_ptr)
{
    am_status_t status;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value_ptr) {
	try {
	    *value_ptr = propPtr->getSigned(key, default_value);
	    status = AM_SUCCESS;
	} catch (const std::invalid_argument&) {
	    status = AM_NOT_FOUND;
	} catch (const std::domain_error&) {
	    status = AM_INVALID_VALUE;
	} catch (const std::range_error&) {
	    status = AM_INVALID_VALUE;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_get_unsigned(am_properties_t properties, const char *key,
			      unsigned long *value_ptr)
{
    am_status_t status;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value_ptr) {
	try {
	    *value_ptr = propPtr->getUnsigned(key);
	    status = AM_SUCCESS;
	} catch (const std::invalid_argument&) {
	    status = AM_NOT_FOUND;
	} catch (const std::domain_error&) {
	    status = AM_INVALID_VALUE;
	} catch (const std::range_error&) {
	    status = AM_INVALID_VALUE;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_get_unsigned_with_default(am_properties_t properties,
					   const char *key,
					   unsigned long default_value,
					   unsigned long *value_ptr)
{
    am_status_t status;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value_ptr) {
	try {
	    *value_ptr = propPtr->getUnsigned(key, default_value);
	    status = AM_SUCCESS;
	} catch (const std::domain_error&) {
	    status = AM_INVALID_VALUE;
	} catch (const std::range_error&) {
	    status = AM_INVALID_VALUE;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_get_positive_number(am_properties_t properties,
                                  const char *key,
                                  unsigned long default_value,
                                  unsigned long *value_ptr)
{
    am_status_t status;
    const Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value_ptr) {
        try {
            *value_ptr = propPtr->getPositiveNumber(key, default_value);
            status = AM_SUCCESS;
        } catch (const std::domain_error&) {
            status = AM_INVALID_VALUE;
        } catch (const std::range_error&) {
            status = AM_INVALID_VALUE;
        } catch (const std::bad_alloc&) {
            status = AM_NO_MEMORY;
        } catch (const std::exception&) {
            status = AM_FAILURE;
        } catch (...) {
            status = AM_FAILURE;
        }
    } else {
        status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_set(am_properties_t properties, const char *key,
		     const char *value)
{
    am_status_t status;
    Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key && value) {
	try {
	    propPtr->set(key, value);
	    status = AM_SUCCESS;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_get_entries(am_properties_t properties,
			     am_properties_iter_t *prop_iter_ptr)
{
    am_status_t status;

    if (AM_PROPERTIES_NULL != properties &&
	static_cast<am_properties_iter_t *>(NULL) != prop_iter_ptr) {
	*prop_iter_ptr = AM_PROPERTIES_ITER_NULL;

	try {
	    const Properties *propPtr;

	    propPtr = reinterpret_cast<const Properties *>(properties);
	    if (propPtr->empty()) {
		status = AM_NOT_FOUND;
	    } else {
		*prop_iter_ptr = new am_properties_iter(propPtr->begin(),
							   propPtr->end());
		status = AM_SUCCESS;
	    }
	} catch (std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (std::exception&) {
	    // XXX - Log something here?
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" void
am_properties_iter_destroy(am_properties_iter_t properties_iter)
{
    delete properties_iter;
}

extern "C" const char *
am_properties_iter_get_key(am_properties_iter_t properties_iter)
{
    const char *result = NULL;

    if (AM_PROPERTIES_ITER_NULL != properties_iter) {
	try {
	    if (properties_iter->current != properties_iter->end) {
		result = properties_iter->current->first.c_str();
	    }
	} catch (const std::exception&) {
	    // XXX - log a message here?
	    result = NULL;
	} catch (...) {
	    result = NULL;
	}
    }

    return result;
}

extern "C" const char *
am_properties_iter_get_value(am_properties_iter_t properties_iter)
{
    const char *result = NULL;

    if (AM_PROPERTIES_ITER_NULL != properties_iter) {
	try {
	    if (properties_iter->current != properties_iter->end) {
		result = properties_iter->current->second.c_str();
	    }
	} catch (const std::exception&) {
	    // XXX - log a message here?
	    result = NULL;
	} catch (...) {
	    result = NULL;
	}
    }

    return result;
}

/**
 * This function sets null terminator "\0" to a property. This is
 * needed in certain situations where a property is present, but no
 * value set to it. Particually useful in CAC, where certain properties
 * returned by AM, may not hold a value, but they are present.
 *
*/
extern "C" am_status_t
am_properties_set_null(am_properties_t properties, const char *key,
		     const char *value)
{
    am_status_t status;
    Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key) {
	try {
	    propPtr->set(key, value);
	    status = AM_SUCCESS;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_set_list(am_properties_t properties, 
                       const char *key,
                       const char *valueSep)
{
    am_status_t status;
    Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key) {
	try {
	    propPtr->set_list(key, valueSep);
	    status = AM_SUCCESS;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_properties_set_map(am_properties_t properties, 
                      const char *key,
                      const char *mapSep,
                      const char *valueSep)
{
    am_status_t status;
    Properties *propPtr = reinterpret_cast<Properties *>(properties);

    if (propPtr && key) {
	try {
	    propPtr->set_map(key, mapSep, valueSep);
	    status = AM_SUCCESS;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception&) {
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}
