/* -*- Mode: C -*- */
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: am_map.h,v 1.4 2008/08/19 19:11:36 veiming Exp $
 */

/*
 * Abstract:
 *
 * Types and functions for creating, destroying, and manipulating the
 * map objects used by the OpenSSO Access Management SDK.
 *
 */

#ifndef AM_MAP_H
#define AM_MAP_H

#include <am_types.h>

AM_BEGIN_EXTERN_C

/*
 * Opaque handle to a map object.
 */
typedef struct am_map *am_map_t;
#define AM_MAP_NULL	((am_map_t) 0)

/*
 * Opaque handle to an iterator for the entries in a map object.
 */
typedef struct am_map_entry_iter *am_map_entry_iter_t;
#if	defined(__cplusplus)
#define AM_MAP_ENTRY_ITER_NULL	static_cast<am_map_entry_iter_t>(0)
#else
#define AM_MAP_ENTRY_ITER_NULL	((am_map_entry_iter_t) 0)
#endif

/*
 * Opaque handle to an iterator for the values associated with a key.
 */
typedef struct am_map_value_iter *am_map_value_iter_t;
#if	defined(__cplusplus)
#define AM_MAP_VALUE_ITER_NULL	static_cast<am_map_value_iter_t>(0)
#else
#define AM_MAP_VALUE_ITER_NULL	((am_map_value_iter_t) 0)
#endif

/*
 * Create a new, empty, map object.
 *
 * Parameters:
 *   map_ptr	pointer to where the handle for the new map object
 *		should be stored
 *
 * Returns:
 *   AM_SUCCESS
 *		if a map was successfully created
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the map object
 *
 *   AM_INVALID_ARGUMENT
 *		if the map_ptr parameter is NULL
 */
AM_EXPORT am_status_t
am_map_create(am_map_t *map_ptr);

/**
 * Returns the number of elements in the map.
 * 
 * Parameters:
 * map
 *      The map for which size is requested.
 * 
 * Returns
 *        The size whose type is size_t.
 */
AM_EXPORT size_t
am_map_size(const am_map_t map);


/*
 * Makes a copy of a map object.
 *
 * Parameters:
 *   source_map
 *		the handle for the map object to be copied
 *
 *   map_ptr
 *		a pointer to where to store the handle of the new created
 *		map object
 *
 * Returns:
 *   AM_SUCCESS
 *		if a map object was successfully copied
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the new map object
 *
 *   AM_INVALID_ARGUMENT
 *		if the source_map or map_ptr argument is NULL
 */
AM_EXPORT am_status_t
am_map_copy(am_map_t source_map, am_map_t *map_ptr);

/*
 * Destroys the map object referenced by the provided handle.
 *
 * Parameters:
 *   map	the handle for the map object to be destroyed.  The handle
 *		may be NULL.
 *
 * Returns:
 *   NONE
 */
AM_EXPORT void am_map_destroy(am_map_t map);

/*
 * Erase all of the entries in the specified map.
 *
 * Parameters:
 *   map	the handle for the map object to be modified
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error was detected
 *
 *   AM_INVALID_ARGUMENT
 *		if the map argument is NULL
 */
AM_EXPORT am_status_t am_map_clear(am_map_t map);

/*
 * Returns an iterator object that can be used to enumerate all of the entries
 * in the specified map.
 *
 * Parameters:
 *   map	the handle for the map object to be examined
 *
 *   entry_iter_ptr
 *		pointer to where the handle for the new entry iterator object
 *		should be stored.
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error was detected
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the entry iterator object
 *
 *   AM_INVALID_ARGUMENT
 *		if the entry_iter_ptr argument is NULL
 *
 *   AM_NOT_FOUND
 *		if the specified map contains no keys
 *
 * NOTE:
 *   If the entry_iter_ptr argument is non-NULL, then the location that it
 *   refers to will be set to NULL if an error is returned.
 */
AM_EXPORT am_status_t
am_map_get_entries(am_map_t map, am_map_entry_iter_t *entry_iter_ptr);

/*
 * Inserts a new (key, value)-pair into the specified map.  If an entry
 * with the same key already exists, then the existing value is replaced
 * by the new value.
 *
 * NOTE: The map does not retain any references to the provided key or
 * value parameters, i.e. it makes copies of any strings it needs to
 * store.
 *
 * Parameters:
 *   map	the handle for the map object to be modified
 *
 *   key	the key for the entry
 *
 *   value	the (new) value to be associated with the key
 *
 *   replace	if non-zero, then the specifed value replaces all of the
 *		existing values, otherwise the specified value is added
 *		to the list of values associated with the specified key.
 *
 * Returns:
 *   AM_SUCCESS
 *		if the entry was successfully inserted into the map
 *
 *   AM_INVALID_ARGUMENT
 *		if either the map, key, or value argument is NULL
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for value and if necessary
 *		the key
 */
AM_EXPORT am_status_t
am_map_insert(am_map_t map, const char *key, const char *value,
		 int replace);

/*
 * Erase the specified key from the specified map.
 *
 * Parameters:
 *   map	the handle for the map object to be modified
 *
 *   key	the key for the entry to erase
 *
 * Returns:
 *   AM_SUCCESS
 *		if the entry was successfully erased from the map
 *
 *   AM_INVALID_ARGUMENT
 *		if either the map or key argument is NULL
 *
 *   AM_NOT_FOUND
 *		if the specified key is not currently in the map
 */
AM_EXPORT am_status_t am_map_erase(am_map_t map, const char *key);

/*
 * Returns an iterator object that can be used to enumerate all of the values
 * associated with the specified key.
 *
 * Parameters:
 *   map	the handle for the map object to be examined
 *
 *   key	the key for the entry to look up
 *
 *   value_iter_ptr
 *		pointer to where the handle for the new value iterator object
 *		should be stored.
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error was detected
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the value iterator object
 *
 *   AM_INVALID_ARGUMENT
 *		if the value_iter_ptr argument is NULL
 *
 *   AM_NOT_FOUND
 *		if the specified key could not be found in the map
 *
 * NOTE:
 *   If the value_iter_ptr argument is non-NULL, then the location that it
 *   refers to will be set to NULL if an error is returned.
 */
AM_EXPORT am_status_t
am_map_find(am_map_t map, const char *key,
	       am_map_value_iter_t *value_iter_ptr);

/*
 * Returns the first value associated with the specified key in the
 * specified map.
 *
 * Parameters:
 *   map	the handle for the map object to be examined
 *
 *   key	the key for the entry to look up
 *
 * Returns:
 *   NULL	if the specified key could not be found in the map or
 *		the specified key had no associated values
 *
 *   value	otherwise, the first value associated with the key
 */
AM_EXPORT const char *
am_map_find_first_value(am_map_t map, const char *key);

/*
 * Destroys the entry iterator object referenced by the provided handle.
 *
 * Parameters:
 *   entry_iter
 *		the handle for the key iterator object to be destroyed.
 *		The handle may be NULL.
 *
 * Returns:
 *   NONE
 */
AM_EXPORT void
am_map_entry_iter_destroy(am_map_entry_iter_t entry_iter);

/*
 * Returns the key of the element currently referenced by the
 * specified iterator.
 *
 * Parameters:
 *   entry_iter
 *		the handle for the entry iterator object to be examined
 *
 * Returns:
 *   NULL	if the specified iterator is NULL or does not reference a
 *		valid entry
 *
 *   key	otherwise
 */
AM_EXPORT const char *
am_map_entry_iter_get_key(am_map_entry_iter_t entry_iter);

/*
 * Returns the first value of the element currently referenced by the
 * specified iterator.
 *
 * Parameters:
 *   entry_iter
 *		the handle for the entry iterator object to be examined
 *
 * Returns:
 *   NULL	if the specified iterator is NULL or does not reference a
 *		valid entry or the entry does not have any associated values
 *
 *   value	otherwise
 */
AM_EXPORT const char *
am_map_entry_iter_get_first_value(am_map_entry_iter_t entry_iter);

/*
 * Returns an iterator object that can be used to enumerate all of the values
 * associated with the entry referenced by the specified iterator.
 *
 * Parameters:
 *   entry_iter
 *		the handle for the entry iterator object to be examined
 *
 *   value_iter_ptr
 *		pointer to where the handle for the new value iterator object
 *		should be stored.
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error was detected
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the value iterator object
 *
 *   AM_INVALID_ARGUMENT
 *		if the value_iter_ptr argument is NULL
 *
 *   AM_NOT_FOUND
 *		if the specified iterator is NULL or does not reference a
 *		valid entry
 *
 * NOTE:
 *   If the value_iter_ptr argument is non-NULL, then the location that it
 *   refers to will be set to NULL if an error is returned.
 */
AM_EXPORT am_status_t
am_map_entry_iter_get_values(am_map_entry_iter_t entry_iter,
				am_map_value_iter_t *value_iter_ptr);

/*
 * Determines if the specified iterator references a valid entry.
 *
 * Parameters:
 *   entry_iter
 *		the handle for the entry iterator object to be examined
 *
 * Returns:
 *   0		if the specified iterator is NULL or does not reference a
 *		valid entry
 *
 *   !0		otherwise
 */
AM_EXPORT boolean_t
am_map_entry_iter_is_entry_valid(am_map_entry_iter_t entry_iter);

/*
 * Advances the specified iterator to the next entry in the map
 * specified when the iterator was created.
 *
 * Parameters:
 *   entry_iter
 *		the handle for the entry iterator object to be modified
 *
 * Returns:
 *   0		if the specified iterator is NULL or does not reference a
 *		valid entry after being updated
 *
 *   !0		otherwise
 */
AM_EXPORT boolean_t am_map_entry_iter_next(am_map_entry_iter_t entry_iter);

/*
 * Destroys the value iterator object referenced by the provided handle.
 *
 * Parameters:
 *   value_iter
 *		the handle for the value iterator object to be destroyed
 *		The handle may be NULL.
 *
 * Returns:
 *   NONE
 */
AM_EXPORT void am_map_value_iter_destroy(am_map_value_iter_t iter);

/*
 * Returns the value currently referenced by the specified iterator.
 *
 * Parameters:
 *   value_iter
 *		the handle for the value iterator object to be examined
 *
 * Returns:
 *   NULL	if the specified iterator is NULL or does not reference a
 *		valid value
 *
 *   value	otherwise
 */
AM_EXPORT const char *am_map_value_iter_get(am_map_value_iter_t iter);

/*
 * Determines if the specified iterator references a valid value.
 *
 * Parameters:
 *   value_iter
 *		the handle for the value iterator object to be examined
 *
 * Returns:
 *   0		if the specified iterator is NULL or does not reference a
 *		valid value
 *
 *   !0		otherwise
 */
AM_EXPORT boolean_t
am_map_value_iter_is_value_valid(am_map_value_iter_t iter);

/*
 * Advances the specified iterator to the next value associated with the
 * key specified when the iterator was created.
 *
 * Parameters:
 *   value_iter
 *		the handle for the value iterator object to be modified
 *
 * Returns:
 *   0		if the specified iterator is NULL or does not reference a
 *		valid value after being updated
 *
 *   !0		otherwise
 */
AM_EXPORT boolean_t am_map_value_iter_next(am_map_value_iter_t iter);

/*
 * Map iterator on a function pointer.
 * The map function will iterate over the list of key value pairs and
 * call each key value pair.
 *
 * Parameters:
 *    func
 *		Function pointer that needs to be called for each key
 *		value pair.
 *    args
 *		The arguments that needs to be passed to the function
 *		pointer along with the key value pair.
 *
 * Returns:
 *    AM_INVALID_ARGUMENT
 *		If the input parameters is invalid.
 *
 *    Others
 *		If the func returns any status code other than AM_SUCCESS
 *		the iteration will terminate and the same status code will
 *		be returned to the user.
 */
AM_EXPORT am_status_t am_map_for_each(am_map_t,
				      am_status_t (*func)(const char *key,
							  const char *value,
							  void **args),
				      void **args);

AM_END_EXTERN_C

#endif	/* not AM_MAP_H */
