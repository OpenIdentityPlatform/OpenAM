#!/bin/bash
#
# Converts LDIF data to CASSANDRA CQL
#
# /opt/opendj/bin/ldapsearch -p 1389 -b ou=users,dc=am,dc=domain,dc=com -D "cn=Directory manager" -w password "(uid=*)" > export.ldif
# cat export.ldif | keyspace=realm_test bash ldif2cassandra.sh > import.cql
# cat import.cql | /opt/cassandra/bin/cqlsh

keyspace=${keyspace:=realm_test}

get_uid ()
{
	res=`echo -e $1`
    if [[ $res =~ ^cn=(.[^,]+) ]] ; then #uid from DN
        res="${BASH_REMATCH[1]}"
    elif [[ $res =~ ^uid=(.[^,]+) ]] ; then #uid from DN
        res="${BASH_REMATCH[1]}"
    fi
	res=`echo -e $res  | awk '{print tolower($0)}'`
}

block="";
while read line; do
	if [[ $line =~ (.+):(.*) ]] ; then  #data line
		block="$block$line\n"
		continue
	elif [[ $line =~ ^\s+# ]] ; then   #comment
			continue
	elif [[ $line =~ ^\s* ]]  ; then   #new line
		type=""
		if [[ $block =~ 'objectClass: person' ]]  ; then   
			type='user'
		elif [[ $block =~ 'objectClass: groupofuniquenames' ]]  ; then   
			type='group'
		fi
		if [ ! -z "$type" ] ; then  
			uid=""

			if [ -z "$uid" ] ; then #uid from dn
				if [[ $block =~ dn:(.+) ]] ; then
					get_uid "${BASH_REMATCH[1]}"
					uid=$res
				fi
			fi
            echo "insert into \"$keyspace\".\"values\" (type,uid,field,value,change) VALUES ('$type',\$\$$uid\$\$,'uid',\$\$$uid\$\$,toTimestamp(now()));"
            while read line; do
                if [[ $line =~ ^dn: ]] ; then
					continue
				elif [[ $line =~ ^objectClass: ]] ; then 
					continue
				elif [[ $line =~ ^uid: ]] ; then
					continue
                elif [[ ! $line =~ ^\s*$ ]] ; then
                    if [[ $line == *::* ]]
                    then
                        attr=${line%%:*}
                        value=`echo ${line#*: } | base64 --decode`
                    else
                        attr=${line%%:*}
                        value=${line#*: }
                    fi
                    attr=`echo -e $attr  | awk '{print tolower($0)}'`
                    if [ $attr == 'uniquemember' ] || [ $attr == 'memberof' ] ; then
                        get_uid "$value"
                        value=$res
                    fi
                    echo "insert into \"$keyspace\".\"values\" (type,uid,field,value,change) VALUES ('$type',\$\$$uid\$\$,'$attr',\$\$$value\$\$,toTimestamp(now()));"
				fi
			done < <(echo -e $block)
            echo ""
		fi
		block="";
	fi  
done


