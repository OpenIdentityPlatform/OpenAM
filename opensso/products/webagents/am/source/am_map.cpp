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
 * $Id: am_map.cpp,v 1.3 2008/06/25 08:14:27 qcheng Exp $
 *
 */ 
#include <algorithm>
#include <cstdlib>
#include <functional>
#include <map>
#include <string>

#include "am_map.h"
#include "key_value_map.h"
#include "string_util.h"
#include "xml_element.h"
#include "internal_macros.h"

using std::string;
USING_PRIVATE_NAMESPACE

typedef KeyValueMap::const_iterator ConstEntryIter;
typedef KeyValueMap::mapped_type::const_iterator ConstValueIter;

struct am_map_entry_iter {
    am_map_entry_iter(const ConstEntryIter& initial,
			 const ConstEntryIter& endArg)
	: current(initial), end(endArg)
    {
    }

    ConstEntryIter current;
    ConstEntryIter end;
};

struct am_map_value_iter {
    am_map_value_iter(const ConstValueIter& initial,
			 const ConstValueIter& endArg)
	: current(initial), end(endArg)
    {
    }

    ConstValueIter current;
    ConstValueIter end;
};


extern "C"
am_status_t am_map_create(am_map_t *map_ptr_ptr)
{
    am_status_t status = AM_SUCCESS;

    if (map_ptr_ptr != static_cast<am_map_t *>(NULL)) {
	try {
	    *map_ptr_ptr = reinterpret_cast<am_map_t>(new KeyValueMap);
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    status = AM_FAILURE;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_create(): Unknown exception encountered.");
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C"
size_t am_map_size(am_map_t this_map) {
    size_t size = 0;
    if(AM_MAP_NULL != this_map) {
	const KeyValueMap *srcMapPtr;
	srcMapPtr = reinterpret_cast<const KeyValueMap *>(this_map);
	size = srcMapPtr->size();
    }
    return size;
}

extern "C"
am_status_t am_map_copy(am_map_t source_map, am_map_t *new_map_ptr)
{
    am_status_t status;

    if (AM_MAP_NULL != source_map) {
	try {
	    const KeyValueMap *srcMapPtr;
	    KeyValueMap *newMapPtr;

	    srcMapPtr = reinterpret_cast<const KeyValueMap *>(source_map);
	    newMapPtr = new KeyValueMap(*srcMapPtr);
	    *new_map_ptr = reinterpret_cast<am_map_t>(newMapPtr);

	    status = AM_SUCCESS;
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    status = AM_FAILURE;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_copy(): Unknown exception encountered");
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }
	    
    return status;
}

extern "C" void am_map_destroy(am_map_t map_ptr)
{
    delete reinterpret_cast<KeyValueMap *>(map_ptr);
}

extern "C" am_status_t am_map_clear(am_map_t map_ptr)
{
    am_status_t status;

    if (AM_MAP_NULL != map_ptr) {
	try {
	    reinterpret_cast<KeyValueMap *>(map_ptr)->clear();
	    status = AM_SUCCESS;
	} catch (std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    status = AM_FAILURE;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_clear(): Unknown exception encountered");
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C"
am_status_t am_map_get_entries(am_map_t map_ptr,
				     am_map_entry_iter_t *entry_iter_ptr)
{
    am_status_t status;

    if (AM_MAP_NULL != map_ptr &&
	static_cast<am_map_entry_iter_t *>(NULL) != entry_iter_ptr) {
	*entry_iter_ptr = AM_MAP_ENTRY_ITER_NULL;

	try {
	    const KeyValueMap *mapPtr;

	    mapPtr = reinterpret_cast<const KeyValueMap *>(map_ptr);
	    if (mapPtr->empty()) {
		status = AM_NOT_FOUND;
	    } else {
		*entry_iter_ptr = new am_map_entry_iter(mapPtr->begin(),
							   mapPtr->end());
		status = AM_SUCCESS;
	    }
	} catch (std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    status = AM_FAILURE;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_get_entries(): Unknown exception encountered");
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C"
am_status_t am_map_insert(am_map_t map_ptr, const char *key,
				const char *value, int replace_values)
{
    am_status_t status = AM_SUCCESS;

    if (AM_MAP_NULL != map_ptr &&
        static_cast<const char *>(NULL) != key &&
	static_cast<const char *>(NULL) != value) {
	try {
	    KeyValueMap *mapPtr = reinterpret_cast<KeyValueMap *>(map_ptr);
	    KeyValueMap::iterator iter = mapPtr->find(key);

	    if (iter != mapPtr->end()) {
		if (replace_values) {
		    KeyValueMap::mapped_type newEntry;

		    newEntry.push_back(value);
		    newEntry.swap(iter->second);
		} else {
		    iter->second.push_back(value);
		}
	    } else {
		KeyValueMap::mapped_type newEntry;

		newEntry.push_back(value);
		newEntry.swap((*mapPtr)[key]);
	    }
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    status = AM_FAILURE;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_insert(): Unknown exception encountered");
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t am_map_erase(am_map_t map_ptr, const char *key)
{
    am_status_t status = AM_SUCCESS;

    if (AM_MAP_NULL != map_ptr &&
        static_cast<const char *>(NULL) != key) {
	try {
	    KeyValueMap *mapPtr = reinterpret_cast<KeyValueMap *>(map_ptr);
	    KeyValueMap::iterator iter = mapPtr->find(key);

	    if (iter != mapPtr->end()) {
		mapPtr->erase(iter);
	    } else {
		status = AM_NOT_FOUND;
	    }
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    status = AM_FAILURE;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_erase(): Unknown exception encountered");
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" am_status_t
am_map_find(am_map_t map_ptr, const char *key,
	       am_map_value_iter_t *value_iter_ptr)
{
    am_status_t status;

    if (static_cast<am_map_value_iter_t *>(NULL) != value_iter_ptr) {
	*value_iter_ptr = AM_MAP_VALUE_ITER_NULL;
	try {
	    const KeyValueMap *mapPtr;

	    mapPtr = reinterpret_cast<const KeyValueMap *>(map_ptr);
	    KeyValueMap::const_iterator iter = mapPtr->find(key);

	    if (iter != mapPtr->end()) {
		const KeyValueMap::mapped_type& valueRef = iter->second;

		*value_iter_ptr = new am_map_value_iter(valueRef.begin(),
							   valueRef.end());
		status = AM_SUCCESS;
	    } else {
		status = AM_NOT_FOUND;
	    }
	} catch (const std::bad_alloc&) {
	    status = AM_NO_MEMORY;
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    status = AM_FAILURE;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_find(): Unknown exception encountered");
	    status = AM_FAILURE;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" const char *am_map_find_first_value(am_map_t map_ptr,
						  const char *key)
{
    const char *result = NULL;

    if (AM_MAP_NULL != map_ptr && static_cast<const char *>(NULL) != key) {
	try {
	    const KeyValueMap *mapPtr;

	    mapPtr = reinterpret_cast<const KeyValueMap *>(map_ptr);
	    KeyValueMap::const_iterator iter = mapPtr->find(key);

	    if (iter != mapPtr->end() && iter->second.size() > 0) {
		result = iter->second[0].c_str();
	    }
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    result = NULL;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_find(): Unknown exception encountered");
	    result = NULL;
	}
    }

    return result;
}

extern "C" void am_map_entry_iter_destroy(am_map_entry_iter_t entry_iter)
{
    delete entry_iter;
}

extern "C" const char *
am_map_entry_iter_get_key(am_map_entry_iter_t entry_iter)
{
    const char *result = NULL;

    if (AM_MAP_ENTRY_ITER_NULL != entry_iter) {
	try {
	    if (entry_iter->current != entry_iter->end) {
		result = entry_iter->current->first.c_str();
	    }
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    result = NULL;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_entry_iter_get_key(): "
		     "Unknown exception encountered");
	    result = NULL;
	}
    }

    return result;
}

extern "C" const char *
am_map_entry_iter_get_first_value(am_map_entry_iter_t entry_iter)
{
    const char *result = NULL;

    if (AM_MAP_ENTRY_ITER_NULL != entry_iter) {
	try {
	    if (entry_iter->current != entry_iter->end &&
		entry_iter->current->second.size() > 0) {
		result = entry_iter->current->second[0].c_str();
	    }
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    result = NULL;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_entry_iter_get_first_value(): "
		     "Unknown exception encountered");
	    result = NULL;
	}
    }

    return result;
}

extern "C" am_status_t
am_map_entry_iter_get_values(am_map_entry_iter_t entry_iter,
				am_map_value_iter_t *value_iter_ptr)
{
    am_status_t status;

    if (static_cast<am_map_value_iter_t *>(NULL) != value_iter_ptr) {
	*value_iter_ptr = AM_MAP_VALUE_ITER_NULL;

	if (AM_MAP_ENTRY_ITER_NULL != entry_iter) {
	    try {
		if (entry_iter->current != entry_iter->end) {
		    const KeyValueMap::mapped_type& valueRef =
			entry_iter->current->second;
		    *value_iter_ptr = new am_map_value_iter(valueRef.begin(),
							       valueRef.end());
		    status = AM_SUCCESS;
		} else {
		    status = AM_NOT_FOUND;
		}
	    } catch (const std::bad_alloc&) {
		status = AM_NO_MEMORY;
	    } catch (const std::exception &ex) {
		Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
		status = AM_FAILURE;
	    } catch (...) {
		Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
			 "am_map_entry_iter_get_values(): "
			 "Unknown exception encountered");
		status = AM_FAILURE;
	    }
	} else {
	    status = AM_NOT_FOUND;
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

extern "C" boolean_t
am_map_entry_iter_is_entry_valid(am_map_entry_iter_t entry_iter)
{
    bool result = false;

    if (AM_MAP_ENTRY_ITER_NULL != entry_iter) {
	try {
	    result = (entry_iter->current != entry_iter->end);
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    result = false;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_entry_iter_is_entry_valid(): "
		     "Unknown exception encountered");
	    result = false;
	}
    }

    return (result==true?B_TRUE:B_FALSE);
}

extern "C" boolean_t am_map_entry_iter_next(am_map_entry_iter_t entry_iter)
{
    bool result = false;

    if (AM_MAP_ENTRY_ITER_NULL != entry_iter) {
	try {
	    if (entry_iter->current != entry_iter->end) {
		++(entry_iter->current);
	    }
	    result = (entry_iter->current != entry_iter->end);
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    result = false;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_entry_iter_next(): Unknown exception encountered");
	    result = false;
	}
    }

    return (result==true?B_TRUE:B_FALSE);
}


extern "C" void am_map_value_iter_destroy(am_map_value_iter_t value_iter)
{
    delete value_iter;
}

extern "C" boolean_t
am_map_value_iter_is_value_valid(am_map_value_iter_t value_iter)
{
    bool result = false;

    if (AM_MAP_VALUE_ITER_NULL != value_iter) {
	try {
	    result = (value_iter->current != value_iter->end);
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    result = false;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_value_iter_is_value_valid(): "
		     "Unknown exception encountered");
	    result = false;
	}
    }

    return (result==true?B_TRUE:B_FALSE);
}

extern "C" const char *
am_map_value_iter_get(am_map_value_iter_t value_iter)
{
    const char *result = NULL;

    if (AM_MAP_VALUE_ITER_NULL != value_iter) {
	try {
	    if (value_iter->current != value_iter->end) {
		result = value_iter->current->c_str();
	    }
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    result = NULL;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_value_iter_get(): Unknown exception encountered");
	    result = NULL;
	}
    }

    return result;
}

extern "C" boolean_t am_map_value_iter_next(am_map_value_iter_t value_iter)
{
    bool result = false;

    if (AM_MAP_VALUE_ITER_NULL != value_iter) {
	try {
	    if (value_iter->current != value_iter->end) {
		++(value_iter->current);
	    }
	    result = (value_iter->current != value_iter->end);
	} catch (const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	    result = false;
	} catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		     "am_map_value_iter_next(): Unknown exception encountered");
	    result = false;
	}
    }

    return (result==true?B_TRUE:B_FALSE);
}

extern "C" am_status_t
am_map_for_each(am_map_t this_map, am_status_t (*func)(const char *,
						  const char *,
						  void **args),
		void **args) {
    am_status_t retVal = AM_SUCCESS;
    try {
	const KeyValueMap *srcMapPtr = NULL;
	srcMapPtr = reinterpret_cast<const KeyValueMap *>(this_map);
	retVal = srcMapPtr->for_each(func, args);
    } catch(std::exception &ex) {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
	retVal = AM_FAILURE;
    } catch(...) {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		 "am_map_for_each(): Unknown exception.");
	retVal = AM_FAILURE;
    }
    return retVal;
}
