# The build for OpenAM 11.0.0 *server* docs and Javadoc...
# Publication URL: http://docs.forgerock.org/en/openam/11.0.0/

# Actually publishing these is another story, see
# https://github.com/markcraig/docs.forgerock.org

$ svn info

URL: https://svn.forgerock.org/openam/fr-branches/11.x.x/openam

# This should be a tag instead for the release build.

$ mvn -Pbinary-licensing -Dcheckstyle.skip=true -DskipTests=true -DisDraftMode=no -DserverDocTargetVersion=11.0.0 -DagentsDocTargetVersion=3.3.0 -DjavadocBase=http://docs.forgerock.org/en/openam/11.0.0/apidocs/ -D"releaseDate=Software release date: November 08, 2013" clean install site
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 22:26.554s
[INFO] Finished at: Fri Oct 11 07:59:42 CEST 2013
[INFO] Final Memory: 227M/854M
[INFO] ------------------------------------------------------------------------

$ cd openam-documentation/openam-doc-source
$ mvn -Dcheckstyle.skip=true -DskipTests=true -DisDraftMode=no -DreleaseVersion="11.0.0" -D"gaId=UA-23412190-14" -D"releaseDate=Software release date: November 08, 2013" org.forgerock.commons:forgerock-doc-maven-plugin:release
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building OpenAM Core Documentation Sources 11.0.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- forgerock-doc-maven-plugin:1.2.0:release (default-cli) @ openam-doc-source ---
[INFO] Laying out release...
[debug] execute contextualize
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 602 resources
[INFO] Copying 13 resources
[INFO] Adding index.html file...
[INFO] Renaming .pdfs...
[INFO] Replacing CSS...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 17.830s
[INFO] Finished at: Fri Oct 11 08:46:22 CEST 2013
[INFO] Final Memory: 8M/125M
[INFO] ------------------------------------------------------------------------

# Inspect content here, found in target/release/11.0.0.
# There should be no DRAFT watermarks.
# The files should be correctly named.
# The cover pages should specify both Software release date (the one you set),
# and also Publication date (the day you built the docs).

# Also check and fix the Javadoc (if necessary). For example:

$ cd ../../target/site/apidocs/
$ for file in `find . -type f -exec grep -l "11.0.0-RC3" {} \;` ; do sed -e "s/11.0.0-RC3/11.0.0/g" $file > $file.new ; mv $file.new $file; done
$ open index.html



# The build for OpenAM 3.3.0 *policy agent* docs...
# Publication URL: http://docs.forgerock.org/en/openam-pa/3.3.0/

# First, Java EE policy agent docs...

# Iff the difference between 3.3 Java EE agents and trunk Java EE
# agents is bug fixes, then we probably should just build those
# from the 11.x.x branch/doc tag. *In general, we have not been
# backporting changes to Java EE agent docs.*

# In that case, just change the releaseVersion and do another release
# goal of the docs built for OpenAM server.

$ svn info

URL: https://svn.forgerock.org/openam/fr-branches/11.x.x/openam/openam-documentation/openam-doc-source

$ mvn -Dcheckstyle.skip=true -DskipTests=true -DisDraftMode=no -DreleaseVersion="3.3.0" -D"gaId=UA-23412190-14" -D"releaseDate=Software release date: November 08, 2013" org.forgerock.commons:forgerock-doc-maven-plugin:release
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building OpenAM Core Documentation Sources 11.0.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- forgerock-doc-maven-plugin:1.2.0:release (default-cli) @ openam-doc-source ---
[INFO] Laying out release...
[debug] execute contextualize
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 602 resources
[INFO] Copying 13 resources
[INFO] Adding index.html file...
[INFO] Renaming .pdfs...
[INFO] Replacing CSS...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 9.576s
[INFO] Finished at: Fri Oct 11 09:36:46 CEST 2013
[INFO] Final Memory: 8M/125M
[INFO] ------------------------------------------------------------------------

# Inspect content for Java EE agent docs, found in target/release/3.3.0.


# ...second, web policy agent docs. These are from the branch.

$ svn info

URL: https://svn.forgerock.org/openam/fr-branches/Agents-3.3.0/openam/openam-documentation/openam-doc-source
# This should be a tag instead for the release build.

$ mvn -Dcheckstyle.skip=true -DskipTests=true -DisDraftMode=no -DserverDocTargetVersion=11.0.0 -DagentsDocTargetVersion=3.3.0 -DjavadocBase=http://docs.forgerock.org/en/openam/11.0.0/apidocs/ -D"releaseDate=Software release date: November 08, 2013" clean site
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2:35.368s
[INFO] Finished at: Fri Oct 11 10:06:10 CEST 2013
[INFO] Final Memory: 46M/512M
[INFO] ------------------------------------------------------------------------

$ mvn -Dcheckstyle.skip=true -DskipTests=true -DisDraftMode=no -DreleaseVersion="3.3.0" -D"gaId=UA-23412190-14" -D"releaseDate=Software release date: November 08, 2013" org.forgerock.commons:forgerock-doc-maven-plugin:release
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building OpenAM Core Documentation Sources 11.0.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- forgerock-doc-maven-plugin:1.2.0:release (default-cli) @ openam-doc-source ---
[INFO] Laying out release...
[debug] execute contextualize
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 490 resources
[INFO] Copying 12 resources
[INFO] Adding index.html file...
[INFO] Renaming .pdfs...
[INFO] Replacing CSS...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 4.429s
[INFO] Finished at: Fri Oct 11 11:21:45 CEST 2013
[INFO] Final Memory: 9M/225M
[INFO] ------------------------------------------------------------------------

# Inspect content for web agent docs, found in target/release/3.3.0.
