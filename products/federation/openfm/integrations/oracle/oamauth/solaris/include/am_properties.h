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
 * $Id: am_properties.h,v 1.4 2008/08/19 19:11:37 veiming Exp $
 */

/*
 * Abstract:
 *
 * Properties map for use by clients of the OpenSSO
 * Remote Client SDK.
 *
 */

#ifndef AM_PROPERTIES_H
#define AM_PROPERTIES_H

#include <am_types.h>

AM_BEGIN_EXTERN_C

/*
 * Opaque handle to a properties object.
 */
typedef struct am_properties *am_properties_t;
#define AM_PROPERTIES_NULL	((am_properties_t) 0)
 
/*
 * Opaque handle to an iterator for the entries in a properties object.
 */
typedef struct am_properties_iter *am_properties_iter_t;
#if	defined(__cplusplus)
#define AM_PROPERTIES_ITER_NULL	static_cast<am_properties_iter_t>(0)
#else
#define AM_PROPERTIES_ITER_NULL	((am_properties_iter_t) 0)
#endif

/*
 * Creates an empty properties object.
 *
 * Parameters:
 *   properties_ptr
 *		a pointer to where to store the handle of the new created
 *		properties object
 *
 * Returns:
 *   AM_SUCCESS
 *		if a properties object was successfully created
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the properties object
 *
 *   AM_INVALID_ARGUMENT
 *		if the properties_ptr argument is NULL
 */
AM_EXPORT am_status_t
am_properties_create(am_properties_t *properties_ptr);

/*
 * Makes a copy of a properties object.
 *
 * Parameters:
 *   source_properties
 *		the handle for the properties object to be copied
 *
 *   properties_ptr
 *		a pointer to where to store the handle of the new created
 *		properties object
 *
 * Returns:
 *   AM_SUCCESS
 *		if a properties object was successfully copied
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the new properties object
 *
 *   AM_INVALID_ARGUMENT
 *		if the source_properties or properties_ptr argument is NULL
 */
AM_EXPORT am_status_t
am_properties_copy(am_properties_t source_properties,
		      am_properties_t *properties_ptr);

/*
 * Destroys the properties object referenced by the provided handle.
 *
 * Parameters:
 *   properties
 *		the handle for the properties object to be destroyed
 *
 * Returns:
 *   NONE
 */
AM_EXPORT void am_properties_destroy(am_properties_t properties);

/*
 * Loads property information from the specified file.  The file
 * is expected to use the standard Java Properties file syntax.
 *
 * Parameters:
 *   properties
 *		handle to the properties object to be modified
 *
 *   file_name
 *		name of the file from which to load the property information
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error is detected
 *
 *   AM_NOT_FOUND
 *		if the specified file does not exist
 *
 *   AM_NSPR_ERROR
 *		if there is a problem accessing the file
 *
 *   AM_INVALID_ARGUMENT
 *		if properties or file_name is NULL or file_name points
 * 		to an empty string
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory to store the property information
 */
AM_EXPORT am_status_t
am_properties_load(am_properties_t properties, const char *file_name);

/*
 * Stores the property information in the specified file.
 *
 * Parameters:
 *   properties
 *		handle to the properties object to be stored
 *
 *   file_name
 *		name of the file in which to store the property information
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error is detected
 *
 *   AM_NSPR_ERROR
 *		if there is a problem writing the properties to the file
 *
 *   AM_INVALID_ARGUMENT
 *		if properties or file_name is NULL or file_name points
 * 		to an empty string
 */
AM_EXPORT am_status_t
am_properties_store(am_properties_t properties, const char *file_name);

/*
 * Determine whether the object contains property with the specified name.
 *
 * Parameters:
 *   properties
 *		handle to the properties object to be examined
 *
 *   key	name of the property to look up
 *
 * Returns:
 *   !0		if the property has a value
 *   0		otherwise
 */
AM_EXPORT boolean_t am_properties_is_set(am_properties_t properties,
					 const char *key);

/*
 * The next eight methods retrieve values from the properties map.
 * The following parameters and exceptions are common to the
 * collection of methods.  The return values are specified with
 * the each related pair of methods.
 *
 * Parameters:
 *   properties
 *		handle to the properties object to be examined
 *
 *   key	name of the property to look up
 *
 *   value_ptr
 *		a pointer to where to store the value associated with
 *		the default value.
 *
 *   default_value
 *		default value to use if there is no value associated
 *		with the specified key.
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error is detected
 *
 *   AM_INVALID_ARGUMENT
 *		if the properties, key, or value_ptr argument is NULL
 *
     AM_NOT_FOUND
 *		if the specified key has no associated value and a
 *		default value is not provided.
 *
 *   AM_INVALID_VALUE
 *		if the value associated with the specified key is cannot
 *		be parsed as required by the particular accessor method.
 *
 *   AM_NO_MEMORY
 *		if insufficient memory is available to look up the key
 */

/*
 * Returns:
 *		the (unparsed) string form of the value associated with
 *		the specified key
 */
AM_EXPORT am_status_t
am_properties_get(am_properties_t properties, const char *key,
		     const char **value_ptr);

AM_EXPORT am_status_t
am_properties_get_with_default(am_properties_t properties,
				  const char *key, const char *default_value,
				  const char **value_ptr);
/*
 * Value stored in value_ptr:
 *
 *   !0		if the value associated with the specified key is one
 *		of: true, on, or yes.
 *
 *   0		if the value associated with the specified key is one
 *		of: false, off, or no.
 *
 * NOTE: If the associated value does not match any of the recognized
 * boolean values, then AM_INVALID_VALUE will be returned.
 */
AM_EXPORT am_status_t
am_properties_get_boolean(am_properties_t properties, const char *key,
			     int *value_ptr);
AM_EXPORT am_status_t
am_properties_get_boolean_with_default(am_properties_t properties,
					  const char *key, int default_value,
					  int *value_ptr);

/*
 * Value stored in value_ptr:
 *
 *		the signed integer value associated with the specified key
 *
 * NOTE: If the associated value cannot be parsed as an integer or
 * cannot be represented in the range LONG_MIN to LONG_MAX, then
 * AM_INVALID_VALUE will be returned.
 */
AM_EXPORT am_status_t
am_properties_get_signed(am_properties_t properties,
				  const char *key, long *value_ptr);
AM_EXPORT am_status_t
am_properties_get_signed_with_default(am_properties_t properties,
					 const char *key, long default_value,
					 long *value_ptr);

/*
 * Returns:
 *		the unsigned integer value associated with the specified key
 *
 * NOTE: If the associated value cannot be parsed as an integer or
 * cannot be represented in the range 0 to ULONG_MAX, then
 * AM_INVALID_VALUE will be returned.
 */
AM_EXPORT am_status_t
am_properties_get_unsigned(am_properties_t properties, const char *key,
			      unsigned long *value_ptr);
AM_EXPORT am_status_t
am_properties_get_unsigned_with_default(am_properties_t properties,
					   const char *key,
					   unsigned long default_value,
					   unsigned long *value_ptr);
AM_EXPORT am_status_t
am_properties_get_positive_number(am_properties_t properties,
                                     const char *key,
                                     unsigned long default_value,
                                     unsigned long *value_ptr);

/*
 * Sets the value associated with the specified key.  The specified
 * value will replace any previously existing value.
 *
 * Parameters:
 *   properties
 *		handle to the properties object to be modified
 *
 *   key	the key to modify
 *
 *   value	the value to associate with the specified key
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error is detected
 *
 *   AM_INVALID_ARGUMENT
 *		if the properties, key, or value argument is NULL
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory to store the new key/value.
 */
AM_EXPORT am_status_t
am_properties_set(am_properties_t properties, const char *key,
		     const char *value);


/*
 * Returns an iterator object that can be used to enumerate all of the entries
 * in the specified properties object.
 *
 * Parameters:
 *   properties
 *		the handle for the properties object to be examined
 *
 *   properties_iter_ptr
 *		pointer to where the handle for the new properties
 * 		iterator object should be stored.
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error was detected
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the properties iterator object
 *
 *   AM_INVALID_ARGUMENT
 *		if the properties_iter_ptr argument is NULL
 *
 *   AM_NOT_FOUND
 *		if the specified properties object contains no entries
 *
 * NOTE:
 *   If the properties_iter_ptr argument is non-NULL, then the location that it
 *   refers to will be set to NULL if an error is returned.
 */
AM_EXPORT am_status_t
am_properties_get_entries(am_properties_t properties,
			     am_properties_iter_t *properties_iter_ptr);

/*
 * Destroys the properties iterator object referenced by the provided handle.
 *
 * Parameters:
 *   properties_iter
 *		the handle for the key iterator object to be destroyed.
 *		The handle may be NULL.
 *
 * Returns:
 *   NONE
 */
AM_EXPORT void
am_properties_iter_destroy(am_properties_iter_t properties_iter);

/*
 * Returns the key of the element currently referenced by the
 * specified iterator.
 *
 * Parameters:
 *   properties_iter
 *		the handle for the properties iterator object to be examined
 *
 * Returns:
 *   NULL	if the specified iterator is NULL or does not reference a
 *		valid entry
 *
 *   key	otherwise
 */
AM_EXPORT const char *
am_properties_iter_get_key(am_properties_iter_t properties_iter);

/*
 * Returns the value of the element currently referenced by the
 * specified iterator.
 *
 * Parameters:
 *   properties_iter
 *		the handle for the properties iterator object to be examined
 *
 * Returns:
 *   NULL	if the specified iterator is NULL or does not reference a
 *		valid entry
 *
 *   value	otherwise
 */
AM_EXPORT const char *
am_properties_iter_get_value(am_properties_iter_t properties_iter);

AM_END_EXTERN_C

#endif	/* not AM_PROPERTIES_H */
