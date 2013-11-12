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
 * $Id: hash_table.h,v 1.6 2008/06/25 08:14:32 qcheng Exp $
 *
 * Abstract:
 *
 * Template class for managing a hash tables of typed objects.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef HASH_TABLE_H
#define HASH_TABLE_H
#include <cassert>
#include <algorithm>
#include <list>
#include <stdexcept>
#include "internal_macros.h"
#include "ref_cnt_ptr.h"
#include "scope_lock.h"
#include "utils.h"

/*
 * This factor is added to avoid two threads simultaneously trying to add
 * add delete the same entry from the bucket.  We avoid it by deleting the
 * entries long after it can be accessed.  This is done by skewing the
 * deletion time by 5 seconds ahead.
 */
#define FUDGE_FACTOR 5
BEGIN_PRIVATE_NAMESPACE

template<typename Element>
class HTFunction {
  public:
    virtual void operator()(Element) = 0;
    virtual ~HTFunction() { }
};

template<class Element>
class HashTable {
public:
    typedef RefCntPtr<Element> ElementType;

    HashTable(unsigned int numBuckets,
	      unsigned long entryLifeTimeInMins);

    ~HashTable();

    ElementType find(const std::string& key);
    ElementType find_cac(const std::string& key);
    bool hasElement(const std::string& key) const;
    ElementType insert(const std::string& key,
		       const ElementType value);
    ElementType remove(const std::string& key);
    void for_each(HTFunction<ElementType> &);

    inline size_t size() {
	size_t retVal = 0;
	for(size_t i = 0; i < numBuckets; ++i) {
	    retVal += buckets[i].size();
	}
	return retVal;
    }

    inline std::size_t num_empty_buckets() {
	std::size_t retVal = 0;
	for(size_t i = 0; i < numBuckets; ++i) {
	    if(buckets[i].size() == 0) {
		++retVal;
	    }
	}
	return retVal;
    }

    void cleanup();
    void cleanup_cac(const std::string& key);
private:
    class Entry;
    typedef RefCntPtr<Entry> EntryType;

    class Entry: public RefCntObj {
    public:
	Entry(const std::string& str, const ElementType &entry,
	      time_t lifeTime) : expirationTime(lifeTime),
				 key(str), value(entry)
	{
	}

	Entry(const Entry& rhs)
	    : expirationTime(rhs.expirationTime), key(rhs.key), value(rhs.value)
	{
	}

	virtual ~Entry() {}

	Entry& operator=(const Entry& rhs)
	{
	    if (this != &rhs) {
		expirationTime = rhs.expirationTime;
		key = rhs.key;
		value = rhs.value;
	    }

	    return *this;
	}

	time_t getExpirationTime() const
	{
	    return expirationTime;
	}

	const std::string& getKey() const
	{
	    return key;
	}

	const ElementType &getValue() const
	{
	    return value;
	}

        ElementType &getValue()
	{
	    return value;
	}

	void setExpirationTime(time_t newExpirationTime)
	{
	    expirationTime = newExpirationTime;
	}

	void setValue(const ElementType &newValue)
	{
	    value = newValue;
	}

    private:
	time_t expirationTime;
	std::string key;
	ElementType value;
    };

    class Bucket;
    friend class HashTable<Element>::Bucket;

    class Bucket {
    public:
	Bucket() : lock(), elements() {}
	~Bucket() {}
	inline size_t size() {
	    return elements.size();
	}

	void cleanup() {
	    ScopeLock myLock(lock);
	    time_t now = time(0);
	    typename std::list<EntryType>::iterator iter;
	    for(iter = elements.begin(); iter != elements.end();) {
		if((*iter)->getExpirationTime() + FUDGE_FACTOR < now) {
		    iter = elements.erase(iter);
		} else {
		    ++iter;
		}
	    }
	}

	void for_each(HTFunction<ElementType> &func) {
	    ScopeLock myLock(lock);
	    typename std::list<EntryType>::iterator iter;
	    for(iter = elements.begin(); iter != elements.end(); ++iter) {
		assert((*iter)->getValue() != NULL);
		func((*iter)->getValue());
	    }
	}

        EntryType find(const std::string& key)
	{
            ScopeLock myLock(lock);
	    typename std::list<EntryType>::iterator iter;
	    EntryType retVal;

	    iter = std::find_if(elements.begin(), elements.end(),
				KeyMatch(key));
	    if (iter != elements.end()) {
		retVal = *iter;
	    }

	    return retVal;
	}

	void insert(const EntryType &element)
	{
	    ScopeLock myLock(lock);

	    elements.insert(elements.begin(), element);
	}

	EntryType remove(const std::string& key)
	{
	    typename std::list<EntryType>::iterator iter;
	    EntryType retVal;
	    ScopeLock myLock(lock);
	    iter = std::find_if(elements.begin(), elements.end(),
				KeyMatch(key));
	    if (iter != elements.end()) {
		retVal = *iter;
		elements.erase(iter);
	    }
	    return retVal;
	}

        void remove_cac(const std::string& latestKey)
	{
	    typename std::list<EntryType>::iterator iter;
	    ScopeLock myLock(lock);
            // Except for the latestKey, remove all other old
            // keys and their corresponding agent config objects
            for (iter = elements.begin(); iter != elements.end(); ++iter) {
                const std::string key = (*iter)->getKey();
                // Remove all the keys and its corresponding value which
                // is not the latestKey
                if (!key.empty() && !latestKey.empty()) {
                    if (strcmp(key.c_str(), latestKey.c_str()) != 0 ) {
                        elements.erase(iter);
                        break;
                    }    
                }
            }
        }
        
    private:

	// The following two methods are not implemented.
	Bucket(const Bucket& rhs);
	Bucket& operator=(const Bucket& rhs);

	class KeyMatch {
	public:
	    KeyMatch(const std::string& keyToMatch)
		: key(keyToMatch)
	    {
	    }

	    bool operator()(const EntryType& entry) const
	    {
		return strcmp(entry->getKey().c_str(),key.c_str()) == 0;
	    }

	private:
	    const std::string& key;
	};

	Mutex lock;
	std::list<EntryType> elements;
    };

    typedef unsigned long HashValueType;

    HashTable(const HashTable& rhs); // not implemented
    HashTable& operator=(const HashTable& rhs);	// not implemented

    HashValueType computeHash(const std::string& key);
    EntryType findEntry(const std::string& key);
    EntryType findEntry_cac(const std::string& key);

    const unsigned int numBuckets;
    time_t entryLifeTime;
    Bucket *buckets;
};

template<class Element>
HashTable<Element>::HashTable(unsigned int numberOfBuckets,
			      unsigned long entryLifeTimeInMins)
    : numBuckets(Utils::get_prime(numberOfBuckets)),
      entryLifeTime(entryLifeTimeInMins * 60),
      buckets(NULL)
{
    if (numBuckets == 0) {
	throw std::invalid_argument("HashTable<Element>() zero buckets requested");
    }


    buckets = new Bucket[numBuckets];
}

template<class Element>
inline HashTable<Element>::~HashTable()
{
    delete[] buckets;
}

#define HASHING_SIZE 32
template<class Element>
typename HashTable<Element>::HashValueType
HashTable<Element>::computeHash(const std::string& keyStr)
{
    const char *key = keyStr.c_str();
    unsigned long val = 0;
    int i;
    int k = 0;

    while (*key && k < HASHING_SIZE) {
        i = (int)*key;
        val ^= i;
        val <<= 1;
        key++;
        k++;
    }
    return(val)%numBuckets;
}

template<class Element>
typename HashTable<Element>::EntryType
HashTable<Element>::findEntry(const std::string& key)
{
    HashValueType bucketNumber = computeHash(key);
    const EntryType &entry = buckets[bucketNumber].find(key);

    if (entry && entry->getExpirationTime() < time(0)) {
        #if defined(LINUX) 
	return (typename HashTable<Element>::EntryType)NULL;
        #else
	return (HashTable<Element>::EntryType)NULL;
        #endif
    }

    return entry;
}

template<class Element>
typename HashTable<Element>::ElementType
HashTable<Element>::find(const std::string& key)
{
    EntryType entry = findEntry(key);
    ElementType value;

    if (entry) {
	value = entry->getValue();
    }

    return value;
}

template<class Element>
typename HashTable<Element>::EntryType
HashTable<Element>::findEntry_cac(const std::string& key)
{
    HashValueType bucketNumber = computeHash(key);
    const EntryType &entry = buckets[bucketNumber].find(key);

    return entry;
}

template<class Element>
typename HashTable<Element>::ElementType
HashTable<Element>::find_cac(const std::string& key)
{
    EntryType entry = findEntry_cac(key);
    ElementType value;

    if (entry) {
	value = entry->getValue();
    }

    return value;
}

template<class Element> bool
HashTable<Element>::hasElement(const std::string& key) const
{
    return findEntry(key) != NULL;
}

template<class Element>
typename HashTable<Element>::ElementType
HashTable<Element>::insert(const std::string& key,
			   const ElementType newValue)
{
    HashValueType bucketNumber = computeHash(key);
    EntryType entry = buckets[bucketNumber].find(key);
    ElementType oldValue;

    // Check if entry already exists return the old value.
    if (entry) {
	oldValue = entry->getValue();
	entry->setValue(newValue);
	entry->setExpirationTime(time(0) + entryLifeTime);
    } else {
	buckets[bucketNumber].insert(EntryType(new Entry(key, newValue,
							 time(0) +
							 entryLifeTime)));
    }

    return oldValue;
}

template<class Element>
typename HashTable<Element>::ElementType
HashTable<Element>::remove(const std::string& key)
{
    HashValueType bucketNumber = computeHash(key);
    EntryType entry = buckets[bucketNumber].remove(key);
    ElementType oldValue;

    if (entry) {
	oldValue = entry->getValue();
    }

    return oldValue;
}

template<typename Element> void
HashTable<Element>::for_each(HTFunction<ElementType> &func) {
    for(size_t i = 0; i < numBuckets; ++i) {
	buckets[i].for_each(func);
    }
    return;
}

template<typename Element> void
HashTable<Element>::cleanup() {
    for(size_t i = 0; i < numBuckets; ++i) {
	if(buckets[i].size() != 0) {
	    buckets[i].cleanup();
	}
    }
}

template<typename Element> void
HashTable<Element>::cleanup_cac(const std::string& latestConfigKey) {
    for (size_t i = 0; i < numBuckets; ++i) {
       if (buckets[i].size() != 0) { 
           buckets[i].remove_cac(latestConfigKey);
       }
    }
}

END_PRIVATE_NAMESPACE

#endif	// not HASH_TABLE_H
