#! /usr/bin/env python

import urllib2
import zipfile
import sys
import os
import os.path

def usage():
    print """
For retrieving and extracting lib directory
    
    Functions: 
        unzip_libs(zip_file, [d=out_folder, v=False])
        get_zip([u=zip_url, i=out_zip_file]) returns zip_file
        main()
    
    unzip_libs 
        zip_file - Zip archive containing the libs
        out_folder - Folder to output lib structure to (default \"lib\")
        verbose - True/False for verbose output (default \"False\")
        
        This function will take a specified zip archive and extract it into a 
        given folder. As the funtion is designed for extracting the lib files
        for OpenAM QATest suite this is the default output location.
    
    get_zip 
        zip_url - URL to download zip archive from 
        out_zip_file - File to save the downloaded archive as 
        
        Retrieves a file from a given url and can output it to a location as
        specified in out_zip_file. It will default to using the name of the 
        file from the web server, and overwrite it without prompting.
    
    main
        Parses any command line inputs and then uses them in calling get_zip and
        unzip_libs. """
            

def unzip_libs(file_loc, **kwargs):
    default_folder = "lib"
    folder = kwargs.pop("d", default_folder)
    
    #default_zip = "qatest_lib.zip"
    #file_loc = kwargs.pop("i", default_zip)
    
    verbose = kwargs.pop("v", False)
    
    if os.path.isdir(folder) and os.path.isfile(file_loc):
        zip = zipfile.ZipFile(file_loc)
        if verbose:
            print "Extracting \"" + file_loc + "\" to " + folder
        for file in zip.namelist():
            if file.endswith('/'):
                if not os.path.isdir(folder + "/" + file):
                    if verbose:
                        print "Making folder : " + file
                    os.mkdir(folder + "/" + file)
            # Security measure... do not extract upwards
            elif ".." not in file:
                if verbose:
                    print "Extracting file : " + file
                zip.extract(file, folder)
    else:
        print "Folder \"" + folder + "\" not found."
        
def get_zip(**kwargs):
    print "Entering get_zip ", kwargs
    default_url = "http://openam.internal.forgerock.com/qatest_lib.zip"
    url = kwargs.pop("u", default_url)
    
    dl_file = urllib2.urlopen(url)
    data = dl_file.info()
    file_size = int(data.getheaders("Content-Length")[0])
    
    file_size_dl = 0
    block = 65536
    file_loc = kwargs.pop("i", url.split('/')[-1])
    print "Opening file to write : " + file_loc
    with open(file_loc, 'w') as f:
        while True:
            buffer = dl_file.read(block)
            if not buffer:
                break
            file_size_dl += len(buffer)
            f.write(buffer)
            status = r"%10d  [%3.2f%%]" % (file_size_dl, file_size_dl * 100. / file_size)
            status = status + chr(8)*(len(status)+1)
            pct = int(file_size_dl * 50. / file_size)
            sys.stdout.write("\r[" + pct*"=" + ">" + (50 - pct)*" " + "] %3.1f%%" % (file_size_dl * 100. / file_size))
        sys.stdout.write("\n")
        return file_loc

def get_arg_value(arg):
    try:
        idx = sys.argv.index("-" + arg)
        argVal = sys.argv[idx + 1]
        if argVal.startswith('-'):
            raise IndexError()
        return argVal
    except (IndexError, ValueError):
        return None
    
def main():
    args = dict()
    inputs = ['i', 'd', 'u']
    switches = ['v']
    
    # Checks for cli args which have values
    for x in inputs:
        val = get_arg_value(x)
        if val:
            args[x]=val
            
    # Checks for cli switches (without values)
    for y in sys.argv:
        if y.startswith('-'):
            for switch in switches:
                if switch in y:
                    args[switch] = True
    
    print args
    
    file = get_zip(**args)
    unzip_libs(file, **args)

#main()