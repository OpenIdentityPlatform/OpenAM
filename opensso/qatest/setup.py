#! /usr/bin/env python

import properties
import sys
import os.path
import re
import libs

def replace(**miles):
    for file_loc in miles.keys():
        pairs = dict(miles[file_loc])
        # If 'output' keyword exists use as output file, otherwise use file_loc
        output = pairs.pop("output", file_loc) 
        if pairs.pop("xml", False):
            content = get_content_xml(file_loc, pairs)
        else:
            content = get_content(file_loc, pairs)
        
        if content is not None:
            # Write Changes         
            print "Writing changes : " + output
            with open(output, "w+") as file:
                file.writelines(content)
                print "    Changes written"
        else:
            print "File \"" + file_loc + "\" cannot be found."
        #print out_buffer
        
def get_content(file_loc, pairs):
    if os.path.exists(file_loc):
        out_buffer = []
        # Read in File
        with open(file_loc) as file:
            for line in file:
                name = line.split("=")[0]
                if name in pairs:
                    try:
                        parts = list(line.partition("="))
                        parts[2] = pairs[name] + "\n"
                        line = ''.join(parts)
                        pairs[name] = "@Complete@"
                    except TypeError:
                        print "Error, value could not be retrieved for : " + name
                #Store lines in output buffer
                out_buffer.append(line)
            for key in pairs.keys():
                if pairs[key] is not "@Complete@":
                    out_buffer.append(key + "=" + pairs[key] + "\n")
            return out_buffer
    else: 
        return None
        
def get_content_xml(file_loc, pairs):
    if os.path.exists(file_loc):
        out_buffer = []
        # Read in File
        with open(file_loc) as file:
            # Should probably be done with XML parsing, however this is easier
            xml_re = re.compile('\s*<property name="(\w+)" value="(\w+)"')
            for line in file:
                matches = xml_re.match(line)
                if matches and matches.group(1) in pairs:
                    line = xml_re.sub('    <property name="' + matches.group(1) + '" value="' + pairs[matches.group(1)] + '"', line)
                    pairs[matches.group(1)] = "@Complete@"
                out_buffer.append(line)
            for key in pairs.keys():
                if pairs[key] is not "@Complete@":
                    out_buffer.insert(len(out_buffer) - 1, '    <property name="{0}" value="{1}"/>\n'.format(key, pairs[key]))
            return out_buffer
    else:
        return None

def get_arg_value(arg):
    try:
        idx = sys.argv.index(arg)
        argVal = sys.argv[idx + 1]
        if argVal.startswith("-"):
            raise IndexError()
        return argVal
    except IndexError:
        # Argument not present
        return "@NoVal@"
    except ValueError:
        # Value not present
        return None

read = properties.Properties()

load = None
loadVal = get_arg_value("-l")
if loadVal is not None and loadVal is not "@NoVal@":
    load = read.load(loadVal)
elif loadVal is "@NoVal@":
    load = read.load()
    
if load is None or "-a" in sys.argv:
    read.ask()
    
saveVal = get_arg_value("-s")
if saveVal is not None and saveVal is not "@NoVal@":
    read.save(saveVal)
elif loadVal:
    read.save()

resources = {
        "resources/authentication/AuthenticationConfig.properties":
            [("ldap.iplanet-am-auth-ldap-server.2", read.lookup("ldap.host")),
            ("ldap.iplanet-am-auth-ldap-server2.2", read.lookup("ldap.host") + ":" + read.lookup("ldap.port")),
            ("ldap.iplanet-am-auth-ldap-base-dn.2", read.lookup("ldap.rootsuffix")),
            ("ldap.iplanet-am-auth-ldap-bind-dn.2", read.lookup("ldap.user")),
            ("ldap.iplanet-am-auth-ldap-bind-passwd.2", read.lookup("ldap.password")),
            ("ad.iplanet-am-auth-ldap-server.2", read.lookup("ad.host")),
            ("ad.iplanet-am-auth-ldap-server2.2", read.lookup("ad.host") + ":" + read.lookup("ad.port")),
            ("ad.iplanet-am-auth-ldap-base-dn.2", "cn=users," + read.lookup("ad.rootsuffix")),
            ("ad.iplanet-am-auth-ldap-bind-dn.2", read.lookup("ad.user")),
            ("ad.iplanet-am-auth-ldap-bind-passwd.2", read.lookup("ad.password"))],
        "resources/authentication/AuthTest.properties":
            [("ad.password", read.lookup("ad.password"))],
        "resources/log/DBConfigInfo.properties":
            [("iplanet-am-logging-location", "jdbc:mysql://" + read.lookup("db.host") + ":" + read.lookup("db.port") + "/IDENTITY"),
            ("iplanet-am-logging-db-user", read.lookup("db.user")),
            ("iplanet-am-logging-db.password", read.lookup("db.user"))],
        "resources/config/AuthenticationConfig.properties":
            [("ldap.iplanet-am-auth-ldap-server", read.lookup("ldap.host") + ":" + read.lookup("ldap.port")),
            ("ldap.iplanet-am-auth-ldap-base-dn", read.lookup("ldap.rootsuffix")),
            ("ldap.iplanet-am-auth-ldap-bind-dn", read.lookup("ldap.user")),
            ("ldap.iplanet-am-auth-ldap-bind-passwd", read.lookup("ldap.password")),
            ("ad.iplanet-am-auth-ldap-server.1", read.lookup("ad.host")),
            ("ad.iplanet-am-auth-ldap-server2.1", read.lookup("ad.host") + ":" + read.lookup("ad.port")),
            ("ad.iplanet-am-auth-ldap-base-dn.1", "cn=users," + read.lookup("ad.rootsuffix")),
            ("ad.iplanet-am-auth-ldap-bind-dn.1", read.lookup("ad.user")),
            ("ad.iplanet-am-auth-ldap-bind-passwd.1", read.lookup("ad.password"))],
        "resources/config/default/ConfiguratorCommon.properties":
            [("umdatastore", read.lookup("openam.usrstore"))],
        "resources/config/UMGlobalDatastoreConfig.properties":
            [("UMGlobalDatastoreConfig1.datastore-root-suffix.0", read.lookup("openam.rootsuffix")),
            ("UMGlobalDatastoreConfig1.sun-idrepo-ldapv3-config-ldap-server.0", read.lookup("ldap.host")),
            ("UMGlobalDatastoreConfig1.sun-idrepo-ldapv3-config-ldap-port.0", read.lookup("ldap.port")),
            ("UMGlobalDatastoreConfig1.datastore-adminid.0", read.lookup("ldap.user")),
            ("UMGlobalDatastoreConfig1.datastore-adminpw.0", read.lookup("ldap.password"))],
        "resources/Configurator-server_name.properties.template":
            [("output", "resources/Configurator-" + re.match("\w+", read.lookup("openam.host")).group(0) + ".properties"),
            ("com.iplanet.am.naming.url", read.lookup("openam.protocol") + "://" + read.lookup("openam.host") + ":" + read.lookup("openam.port") + "/" + read.lookup("openam.uri") + "/namingservice"),
            ("amadmin_password", read.lookup("openam.password")),
            ("com.iplanet.am.service.password", read.lookup("openam.agentpassword")),
            ("config_dir", read.lookup("openam.configdir")),
            ("directory_server", read.lookup("ldap.host")),
            ("directory_port", read.lookup("ldap.port")),
            ("config_root_suffix", read.lookup("openam.rootsuffix")),
            ("ds_dirmgrpasswd", read.lookup("ldap.password")),
            ("directory_jmx_port", read.lookup("openam.jmxport")),
            ("directory_admin_port", read.lookup("openam.diradmport")),
            ("openam_version", "10.0")],
        "build.properties":
            [("xml", "True"),
            ("TEST_MODULE", read.lookup("test.module")),
            ("SERVER_NAME1", re.match("\w+", read.lookup("openam.host")).group(0)),
            ("REPORT_DIR", read.lookup("test.report"))]}

libs.main()
replace(**resources)
