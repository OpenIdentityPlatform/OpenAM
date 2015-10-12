# OpenAM XUI

## Building

You can build the package using Maven:

```
$ maven install
```

This will output a zip file in the build artifact directory. For example, `target/openam-ui-ria-<version>-www.zip`.

## Development

We use [Grunt](http://gruntjs.com/) to build the project. Build dependencies are installed via
[npm](https://www.npmjs.com/).

If you want to develop the UI, we strongly recommend running Grunt to perform the syncing of the source code with your
running application server. To do this, there are a few prerequisites:

* Ensure you have [Node.js](https://nodejs.org/) installed.
* Add an environment variable, `OPENAM_HOME`, that points to an expanded OpenAM install in your application server's
webapp directory. For example, `OPENAM_HOME=~/tomcat/webapps/openam`.

then do:

```
$ npm install
$ grunt
```

Grunt will then start watching the source and sync any changed files over to your server's webapp directory.

### Unit Tests

To get the unit tests to run automatically when you change source files, do:

```
$ grunt karma:dev
```

This will run karma and show test output as tests are run. _You should run this in addition to running grunt as shown
above_.

If you need to debug test failures, open [http://localhost:9876/debug.html](http://localhost:9876/debug.html) in your
browser of choice and open the development tools. Test failures will be reported in the console.