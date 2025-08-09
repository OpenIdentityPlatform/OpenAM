/* Copyright 2005-2006 Tim Fennell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openidentityplatform.openam.click.service;

import org.openidentityplatform.openam.click.util.ClickUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * <p>ResolverUtil is used to locate classes that are available in the/a class path and meet
 * arbitrary conditions. The two most common conditions are that a class implements/extends
 * another class, or that is it annotated with a specific annotation. However, through the use
 * of the {@link org.openidentityplatform.openam.click.service.DeployUtils.Test} class it is possible to search using arbitrary conditions.</p>
 *
 * <p>A ClassLoader is used to locate all locations (directories and jar files) in the class
 * path that contain classes within certain packages, and then to load those classes and
 * check them. By default the ClassLoader returned by
 *  {@code Thread.currentThread().getContextClassLoader()} is used, but this can be overridden
 * by calling {@link #setClassLoader(ClassLoader)} prior to invoking any of the {@code find()}
 * methods.</p>
 *
 * <p>General searches are initiated by calling the
 * {@link #find(net.sourceforge.stripes.util.ResolverUtil.Test, String)} ()} method and supplying
 * a package name and a Test instance. This will cause the named package <b>and all sub-packages</b>
 * to be scanned for classes that meet the test. There are also utility methods for the common
 * use cases of scanning multiple packages for extensions of particular classes, or classes
 * annotated with a specific annotation.</p>
 *
 * <p>The standard usage pattern for the ResolverUtil class is as follows:</p>
 *
 *<pre>
 *ResolverUtil&lt;ActionBean&gt; resolver = new ResolverUtil&lt;ActionBean&gt;();
 *resolver.findImplementation(ActionBean.class, pkg1, pkg2);
 *resolver.find(new CustomTest(), pkg1);
 *resolver.find(new CustomTest(), pkg2);
 *Collection&lt;ActionBean&gt; beans = resolver.getClasses();
 *</pre>
 *
 * This class was copied and adapted from the Stripes Framework -
 * <a href="http://www.stripesframework.org">Stripes</a>.
 *
 * @author Tim Fennell
 */
class DeployUtils<T> {

    // -------------------------------------------------------------- Constants

    /** The magic header that indicates a JAR (ZIP) file. */
    private static final byte[] JAR_MAGIC = { 'P', 'K', 3, 4 };

    // -------------------------------------------------------------- Variables

    /** The set of matches being accumulated. */
    private List<String> matches = new ArrayList<String>();

    /** The log service to log output to. */
    private LogService logService;

    /**
     * The ClassLoader to use when looking for classes. If null then the ClassLoader
     * returned by Thread.currentThread().getContextClassLoader() will be used.
     */
    private ClassLoader classloader;

    // ----------------------------------------------------------- Constructors

    /**
     * Create a new DeployUtils instance.
     *
     * @param logService the logService to log output to
     */
    public DeployUtils(LogService logService) {
        this.logService = logService;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Provides access to the resources discovered so far. If no calls have been
     * made to any of the {@code find()} methods, this list will be empty.
     *
     * @return the list of resources that have been discovered.
     */
    public List<String> getResources() {
        return matches;
    }

    /**
     * Returns the classloader that will be used for scanning for classes. If no explicit
     * ClassLoader has been set by the calling, the context class loader will be used.
     *
     * @return the ClassLoader that will be used to scan for classes
     */
    public ClassLoader getClassLoader() {
        return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
    }

    /**
     * Sets an explicit ClassLoader that should be used when scanning for classes. If none
     * is set then the context classloader will be used.
     *
     * @param classloader a ClassLoader to use when scanning for classes
     */
    public void setClassLoader(ClassLoader classloader) {
        this.classloader = classloader;
    }

    /**
     * Attempt to discover resources inside the given directory. Accumulated
     * resources can be accessed by calling {@link #getResources()}.
     *
     * @param dirs one or more directories to scan (including sub-directories)
     * for resources
     * @return instance of DeployUtils allows for chaining calls
     */
    public org.openidentityplatform.openam.click.service.DeployUtils<T> findResources(String... dirs) {
        if (dirs == null) {
            return this;
        }

        org.openidentityplatform.openam.click.service.DeployUtils.Test test = new org.openidentityplatform.openam.click.service.DeployUtils.IsDeployable();
        for (String dir : dirs) {
            find(test, dir);
        }

        return this;
    }

    /**
     * Scans for classes starting at the package provided and descending into subpackages.
     * Each class is offered up to the Test as it is discovered, and if the Test returns
     * true the class is retained.  Accumulated classes can be fetched by calling
     * {@link #getClasses()}.
     *
     * @param test an instance of {@link org.openidentityplatform.openam.click.service.DeployUtils.Test} that will be used to filter classes
     * @param packageName the name of the package from which to start scanning for
     *        classes, e.g. {@code net.sourceforge.stripes}
     */
    public org.openidentityplatform.openam.click.service.DeployUtils<T> find(org.openidentityplatform.openam.click.service.DeployUtils.Test test, String packageName) {
        String path = getPackagePath(packageName);

        try {
            List<URL> urls = Collections.list(getClassLoader().getResources(path));
            for (URL url : urls) {
                List<String> children = listClassResources(url, path);
                for (String child : children) {
                    addIfMatching(test, child);
                }
            }

        } catch (IOException ioe) {
            logService.error("could not read package: " + packageName + " -- " + ioe);
        }

        return this;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Recursively list all resources under the given URL that appear to define a Java class.
     * Matching resources will have a name that ends in ".class" and have a relative path such that
     * each segment of the path is a valid Java identifier. The resource paths returned will be
     * relative to the URL and begin with the specified path.
     *
     * @param url The URL of the parent resource to search.
     * @param path The path with which each matching resource path must begin, relative to the URL.
     * @return A list of matching resources. The list may be empty.
     * @throws IOException
     */
    protected List<String> listClassResources(URL url, String path) throws IOException {
        if (logService.isDebugEnabled()) {
            logService.debug("listing classes in " + url);
        }

        InputStream is = null;
        try {
            List<String> resources = new ArrayList<String>();

            // First, try to find the URL of a JAR file containing the requested
            // resource. If a JAR file is found, then we'll list child resources
            // by reading the JAR.
            URL jarUrl = findJarForResource(url, path);
            if (jarUrl != null) {
                // example jarUrl : jar:c:/dev/mylib.jar
                is = jarUrl.openStream();
                resources = listClassResources(new JarInputStream(is), path);

            } else {
                List<String> children = new ArrayList<String>();
                try {
                    if (isJar(url)) {
                        // example url : jar:c:/dev/mylib.jar/META-INF/resources

                        // Some versions of JBoss VFS might give a JAR stream even
                        // if the resource referenced by the URL isn't actually a JAR
                        is = url.openStream();
                        JarInputStream jarInput = new JarInputStream(is);
                        for (JarEntry entry; (entry = jarInput.getNextJarEntry()) != null;) {
                            if (logService.isTraceEnabled()) {
                                logService.trace("jar entry: " + entry.getName());
                            }

                            if (isRelevantResource(entry.getName())) {
                                children.add(entry.getName());
                            }
                        }
                        jarInput.close();
                    } else {
                        // Some servlet containers allow reading from "directory"
                        // resources like text file, listing the child resources
                        // one per line.
                        is = url.openStream();

                        // There is the possibility that a file doesn't have an
                        // extension and would be seen as a directory. Guard against
                        // that by checking that the url is a file and adding it
                        // to the list of resources
                        File file = new File(url.getFile());
                        if (file.isFile()) {
                            if (isRelevantResource(file.getName())) {
                                resources.add(path);
                            }
                        } else {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            for (String line; (line = reader.readLine()) != null;) {
                                if (logService.isTraceEnabled()) {
                                    logService.trace("reader entry: " + line);
                                }
                                if (isRelevantResource(line)) {
                                    children.add(line);
                                }
                            }
                            reader.close();
                        }
                    }

                } catch (FileNotFoundException e) {
                    /*
                     * For file URLs the openStream() call might fail, depending on the servlet
                     * container, because directories can't be opened for reading. If that happens,
                     * then list the directory directly instead.
                     */
                    if ("file".equals(url.getProtocol())) {
                        File file = new File(url.getFile());
                        if (file.isDirectory()) {
                            children = Arrays.asList(file.list(new FilenameFilter() {
                                public boolean accept(File dir, String name) {
                                    return isRelevantResource(name);
                                }
                            }));
                        }
                    } else {
                        // No idea where the exception came from so log it
                        logService.error("could not deploy the resources from"
                                + " the url '" + url + "'. You will need to"
                                + " manually included resources from this url in"
                                + " your application.");
                    }
                }

                // The URL prefix to use when recursively listing child resources
                String prefix = url.toExternalForm();
                if (!prefix.endsWith("/")) {
                    prefix = prefix + "/";
                }

                // Iterate over each immediate child, adding classes and recursing into directories
                for (String child : children) {
                    String resourcePath = path + "/" + child;
                    if (child.indexOf(".") != -1) {
                        if (logService.isTraceEnabled()) {
                            logService.trace("found deployable resource: " + resourcePath);
                        }
                        resources.add(resourcePath);

                    } else {
                        URL childUrl = new URL(prefix + child);
                        resources.addAll(listClassResources(childUrl, resourcePath));
                    }
                }
            }

            return resources;

        } finally {
            ClickUtils.close(is);
        }
    }

    /**
     * List the names of the entries in the given {@link JarInputStream} that begin with the
     * specified {@code path}. Entries will match with or without a leading slash.
     *
     * @param jar The JAR input stream
     * @param path The leading path to match
     * @return The names of all the matching entries
     * @throws IOException
     */
    protected List<String> listClassResources(JarInputStream jar, String path) throws IOException {
        // Include the leading and trailing slash when matching names
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        // Iterate over the entries and collect those that begin with the requested path
        List<String> resources = new ArrayList<String>();
        for (JarEntry entry; (entry = jar.getNextJarEntry()) != null;) {
            if (!entry.isDirectory()) {
                // Add leading slash if it's missing
                String name = entry.getName();
                if (!name.startsWith("/")) {
                    name = "/" + name;
                }

                // Check resource name
                if (name.startsWith(path)) {
                    resources.add(name.substring(1)); // Trim leading slash
                }
            }
        }

        return resources;
    }

    /**
     * Attempts to deconstruct the given URL to find a JAR file containing the resource referenced
     * by the URL. That is, assuming the URL references a JAR entry, this method will return a URL
     * that references the JAR file containing the entry. If the JAR cannot be located, then this
     * method returns null.
     *
     * @param url The URL of the JAR entry.
     * @param path The path by which the URL was requested from the class loader.
     * @return The URL of the JAR file, if one is found. Null if not.
     * @throws MalformedURLException
     */
    protected URL findJarForResource(URL url, String path) throws MalformedURLException {
        if (logService.isTraceEnabled()) {
            logService.trace("find jar url: " + url);
        }

        // If the file part of the URL is itself a URL, then that URL probably points to the JAR
        try {
            for (;;) {
                url = new URL(url.getFile());
                if (logService.isTraceEnabled()) {
                    logService.trace("inner url: " + url);
                }
            }

        } catch (MalformedURLException e) {
            // This will happen at some point and serves a break in the loop
        }

        // Look for the .jar extension and chop off everything after that
        StringBuilder jarUrl = new StringBuilder(url.toExternalForm());
        int index = jarUrl.lastIndexOf(".jar");
        if (index >= 0) {
            jarUrl.setLength(index + 4);
            if (logService.isTraceEnabled()) {
                logService.trace("extracted jar url: " + jarUrl);
            }
        } else {
            if (logService.isTraceEnabled()) {
                logService.trace("not a jar: " + jarUrl);
            }
            return null;
        }

        // Try to open and test it
        try {
            URL testUrl = new URL(jarUrl.toString());
            if (isJar(testUrl)) {
                return testUrl;

            } else {
                // WebLogic fix: check if the URL's file exists in the filesystem.
                if (logService.isTraceEnabled()) {
                    logService.trace("not a jar: " + jarUrl);
                }

                jarUrl.replace(0, jarUrl.length(), testUrl.getFile());

                // File name might be URL-encoded
                File file = new File(ClickUtils.decodeURL(jarUrl.toString()));
                if (file.exists()) {
                    if (logService.isTraceEnabled()) {
                        logService.trace("trying real file: " + file.getAbsolutePath());
                    }
                    testUrl =  file.toURI().toURL();
                    if (isJar(testUrl)) {
                        return testUrl;
                    }
                }
            }

        } catch (MalformedURLException e) {
            logService.warn("invalid jar url: " + e.getMessage());
        }

        if (logService.isTraceEnabled()) {
            logService.trace("not a jar: " + jarUrl);
        }
        return null;
    }

    /**
     * Converts a Java package name to a path that can be looked up with a call to
     * {@link ClassLoader#getResources(String)}.
     *
     * @param packageName The Java package name to convert to a path
     */
    protected String getPackagePath(String packageName) {
        return packageName == null ? null : packageName.replace('.', '/');
    }

    /**
     * Returns true if the name of a resource (file or directory) is one that matters in the search
     * for classes. Relevant resources would be class files themselves (file names that end with
     * ".class") and directories that might be a Java package name segment (java identifiers).
     *
     * @param resourceName The resource name, without path information
     */
    protected boolean isRelevantResource(String resourceName) {
        return resourceName != null && !resourceName.equals("");
    }

    /**
     * Returns true if the resource located at the given URL is a JAR file.
     *
     * @param url The URL of the resource to test.
     */
    protected boolean isJar(URL url) {
        return isJar(url, new byte[JAR_MAGIC.length]);
    }

    /**
     * Returns true if the resource located at the given URL is a JAR file.
     *
     * @param url The URL of the resource to test.
     * @param buffer A buffer into which the first few bytes of the resource are read. The buffer
     *            must be at least the size of {@link #JAR_MAGIC}. (The same buffer may be reused
     *            for multiple calls as an optimization.)
     */
    protected boolean isJar(URL url, byte[] buffer) {
        InputStream is = null;
        try {
            is = url.openStream();
            if (is.read(buffer, 0, JAR_MAGIC.length) != JAR_MAGIC.length) {
                return false;
            }
            if (Arrays.equals(buffer, JAR_MAGIC)) {
                if (logService.isInfoEnabled()) {
                    logService.info("found jar: " + url);
                }
                return true;
            }

        } catch (Exception e) {
            // Failure to read the stream means this is not a JAR

        } finally {
            ClickUtils.close(is);
        }

        return false;
    }

    /**
     * Add the class designated by the fully qualified class name provided to the set of
     * resolved classes if and only if it is approved by the Test supplied.
     *
     * @param test the test used to determine if the class matches
     * @param fqn the fully qualified name of a class
     */
    protected void addIfMatching(org.openidentityplatform.openam.click.service.DeployUtils.Test test, String fqn) {
        try {
            if (test.matches(fqn)) {
                matches.add(fqn);
            }
        } catch (Throwable t) {
            logService.error("could not examine class '" + fqn + "'" + " due to a "
                    + t.getClass().getName() + " with message: " + t.getMessage());
        }
    }

    // ---------------------------------------------------------- Inner classes

    /**
     * A simple interface that specifies how to test classes to determine if they
     * are to be included in the results produced by the ResolverUtil.
     */
    static interface Test {
        /**
         * Will be called repeatedly with candidate classes. Must return True if a class
         * is to be included in the results, false otherwise.
         */
        boolean matches(String resource);
    }

    /**
     * This test matches deployable resources.
     */
    static class IsDeployable implements org.openidentityplatform.openam.click.service.DeployUtils.Test {

        /**
         * Default constructor.
         */
        IsDeployable() {
        }

        /**
         * Return true if the given resource should be included in the results,
         * false otherwise.
         *
         * @param resource the resource to be included in the results
         * @return true if the resource should be included in the results, false
         * otherwise
         */
        public boolean matches(String resource) {
            // If a resource is found, it must be deployed
            return true;
        }
    }
}
