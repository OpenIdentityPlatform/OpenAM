#!/usr/bin/env python

import re
import os
import getpass

def getConfDefault():
    """Retrieve default install location for OpenAM config directory"""
    home = os.getenv('USERPROFILE') or os.getenv('HOME')
    return os.path.join(home, "openam")
 
class Properties:
    """Whatever this class is for"""

    REGEX_host = '^[a-zA-Z0-9\-]+(\.[a-zA-Z0-9\-]+)+$'
    REGEX_number = '^\d+$'
    REGEX_protocol = '^https?$'
    REGEX_bool = 'yes|no|y|n'
    REGEX_password = '.{8}.*'
    REGEX_all = '.*'

    def __init__(self):
        self.index = {'openam.host':
                ['Hostname for OpenAM instance', 'openam.example.com', Properties.REGEX_host],
            'openam.port':
                ['Port for OpenAM instance', '8080', Properties.REGEX_number],
            'openam.uri':
                ['Context path for OpenAM instance', 'openam', Properties.REGEX_all],
            'openam.protocol':
                ['Protocol on which OpenAM is hosted', 'http', Properties.REGEX_protocol],
            #('openam.username', 'amadmin'),
            'openam.password':
                ['Admin password for OpenAM', 'password', Properties.REGEX_all],
            #('openam.agentuser', 'urlaccessagent'),
            'openam.agentpassword':
                ['OpenAM Agent password for URLAccessAgent', 'passwordua', Properties.REGEX_all],
            'openam.configdir':
                ['Filepath for OpenAM installation files', getConfDefault(), Properties.REGEX_all],
            'openam.usrstore':
                ['Type of OpenAM user store {embedded|dirServer}', 'embedded', 'embedded|dirServer'],
            'openam.dirport':
                ['Port to use for embedded Directory Server', '50389', Properties.REGEX_number],
            'openam.jmxport':
                ['JMX port to use for embedded Directory Server', '1689', Properties.REGEX_number],
            'openam.diradmport':
                ['Administration port for embedded Directory Server', '4444', Properties.REGEX_number],
            'openam.rootsuffix':
                ['Root Suffix for OpenAM Config', 'dc=internal,dc=forgerock,dc=com', Properties.REGEX_all],
            'ad.available':
                ['Is an Active Directory instance available {yes|no}', 'yes', Properties.REGEX_bool],
            'ad.host':
                ['Hostname for Active Directory instance', 'ad.internal.forgerock.com', Properties.REGEX_host],
            'ad.port':
                ['Port for Active Directory', '389', Properties.REGEX_number],
            'ad.rootsuffix':
                ['Root Suffix for Active Directory', 'dc=internal,dc=forgerock,dc=com', Properties.REGEX_all],
            'ad.user':
                ['Administrative User for Active Directory', 'cn=administrator,cn=users,' + repr(self.lookup('ad.rootsuffix')), Properties.REGEX_all],
            'ad.password':
                ['Password for Active Directory User', 'secret123', Properties.REGEX_all],
            'ldap.available':
                ['Is an LDAP server available?', 'yes', Properties.REGEX_bool],
            'ldap.host':
                ['Hostname for LDAP instance', 'opendj.internal.forgerock.com', Properties.REGEX_host],
            'ldap.port':
                ['Port number for LDAP instance', '1389', Properties.REGEX_number],
            'ldap.rootsuffix':
                ['Root Suffix for LDAP instance', 'dc=internal,dc=forgerock,dc=com', Properties.REGEX_all],
            'ldap.user':
                ['Administrative User for LDAP instance', 'cn=Directory Manager', Properties.REGEX_all],
            'ldap.password':
                ['Password for LDAP User', 'secret123', Properties.REGEX_all],
            'db.available':
                ['Is a Database instance available?', 'yes', Properties.REGEX_bool],
            'db.host':
                ['Hostname for Database instance', 'mysql.internal.forgerock.com', Properties.REGEX_host],
            'db.port':
                ['Port for Database instance', '3306', Properties.REGEX_number],
            'db.user':
                ['Username for Database instance', 'dbuser', Properties.REGEX_all],
            'db.password':
                ['Password for Database User', 'secret123', Properties.REGEX_all],
            'test.report':
                ['Location for Test Report', os.getcwd() + '/report', Properties.REGEX_all],
            'test.module':
                ['Test module to run', self.select_module(), Properties.REGEX_all]}


    def select_module(self):
        if self.lookup('ad.available') and self.lookup('ldap.available'):
            return 'all'
        else:
            return 'sanity'

    servers = ('ad', 'ldap', 'db')
        
    init_props = ['openam.host', 
                  'openam.port', 
                  'openam.uri', 
                  'openam.protocol', 
                  'openam.password',
                  'openam.agentpassword',
                  'openam.configdir', 
                  'openam.usrstore',
                  'openam.dirport',
                  'openam.jmxport', 
                  'openam.diradmport',
                  'openam.rootsuffix',
                  'ad.available',
                  'ad.host',
                  'ad.port',
                  'ad.rootsuffix',
                  'ad.user', 
                  'ad.password',
                  'ldap.available', 
                  'ldap.host',
                  'ldap.port',
                  'ldap.rootsuffix',
                  'ldap.user',
                  'ldap.password',
                  'db.available',
                  'db.host',
                  'db.port',
                  'db.user',
                  'db.password',
                  'test.report',
                  'test.module']
            
    
    props = dict()
    loaded = False
    
    def ask(self, *args):
        """ Asks the user for configuration values """
        # If 'Load From Defaults'
        if "default" in args:
            for prop in self.init_props:
                self.props[prop] = self.index[prop][1]
            self.loaded = True
        # Else Load from Console
        else:
            for prop in self.init_props:
                [desc, default, regex] = self.index[prop]
                coffee = True

                # Don't ask for machines which are not available
                prop_parts = prop.split('.')
                #print "prop_parts : " + str(prop_parts)
                if prop_parts[0] in self.servers and prop_parts[1] != 'available':
                    #print "lookup : " + repr(self.lookup(prop_parts[0] + ".available"))
                    if not self.lookup(prop_parts[0] + ".available"):
                        self.props[prop] = default
                        coffee = False

                while coffee:
                    # Get input from Std.in, hiding input if it is a password property
                    if "password" in prop:
                        passwords_match = False
                        while not passwords_match:
                            resp = getpass.getpass(desc + " : ")
                            sec_resp = getpass.getpass("Please re-enter password : ")
                            # Seems to be a bug where sec_resp is resp == False 
                            # when resp != "". Not fixed by strip() or repr().
                            if  sec_resp in resp and resp in sec_resp:
                                passwords_match = True
                            else:
                                print "Passwords do not match, please try again"
                    else:
                        resp = raw_input(desc + " [" + default + "] : ")

                    # If empty use default, if complies with format accept, otherwise ask again
                    if len(resp.strip()) <= 0:
                        self.props[prop] = default
                        coffee = False
                    elif "available" in prop and re.match(regex, resp, re.I):
                        if resp.lower().startswith('y'):
                            self.props[prop] = True
                        else:
                            self.props[prop] = False
                        coffee = False
                    elif re.match(regex, resp):
                        self.props[prop] = resp
                        coffee = False
                    else :
                        print 'Invalid value "' + resp + '", please try again, or accept default value.'
            self.loaded = True
        return self
            
    def lookup(self, prop):
        if prop in self.props:
            return self.props[prop]
            
    def save(self, *args):
        paths = []
        for possible_file in args:
            try:
                file = open(possible_file, 'a')
                file.close()
                paths.append(possible_file)
            except IOError:
                print "Could not output to file " + possible_file
        if len(paths) == 0:
            paths.append(".props.dict")
        
        for filepath in paths:
            with open(filepath, 'w+') as file:
                file.write(repr(self.props))
    
    def load(self, *args):
        if len(args) == 0:
            path = ".props.dict"
        else:
            path = args[0];
        try:
            file = open(path, 'r')
            file.close()
        except IOError:
            print "Could not open file " + path + " to read."
            return None
            
        with open(path) as file:
            input = file.read()
            # Validate input as dictionary prior to eval()
            if re.match("{(?:\s*'[^']+'\s*:\s*'[^']+'\s*,)*\s*'[^']+'\s*:\s*'[^']+'\s*}", str(input)):
                self.props = eval(input)
                return self
            else:
                print "File input not valid"
                return None
            
#tmp = Properties()
#tmp.ask()
#print tmp.props
