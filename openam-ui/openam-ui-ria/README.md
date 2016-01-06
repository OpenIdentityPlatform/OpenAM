# OpenAM XUI

<!-- TOC depthFrom:2 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Dependencies](#dependencies)
	- [Update Process](#update-process)
- [Building](#building)
- [Development](#development)
	- [ES6 Transpiling](#es6-transpiling)
	- [Unit Tests](#unit-tests)

<!-- /TOC -->

## Dependencies
NPM dependencies for this project are "locked" to specific versions through two mechanisms:
* Explicit version expressions (e.g. `"lodash": "4.1.0"` n.b. no `"^"`) in `package.json`
* Shrink-wrapping of deeper dependencies using `npm shrinkwrap --dev` (e.g. `npm-shrinkwrap.json` contains a full set of explicit dependency versions for the entire dependency tree)

Updates to dependency versions are performed manually in a periodic manner. Interim version updates are allowed but must still follow the update process described below.

### Update Process
1. Explicitly bump dependency versions within `package.json`
  * Use a tool such as [npm-check-updates](https://www.npmjs.com/package/npm-check-updates) to automate this task
  * ***Be mindful of breaking changes!***. Major version changes will most likely require code changes to align with changes in a dependency's API. A major dependency version update should be performed in a separate task.
2. Run `npm shrinkwrap --dev` to regenerate `npm-shrinkwrap.json`
  * Watch out for errors and warnings from executing the shrinkwrapping command.
  * Sometimes a dependencies version requirements are broken making it impossible to perform the operation and an error will occur. In this instance you should identity the problem and contact the dependency author to fix the issue before updating.
3. Commit it!

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

### ES6 Transpiling
* ✅Phase 0 - Babel support (no-op)
* ❎Phase 1 - Arrow functions, `const` and `let`
* ❎Phase 2 - TBA

ES6 is supported via [Babel](https://babeljs.io) transpiling. The following files and directories are transpiled:
* `main-authorize.js`
* `main-device.js`
* `main.js`
* `org/forgerock/openam/**/*`

n.b. `config` is ***NOT*** transpiled due to inter-mixing of commons modules into this directory.

Ensure you have `Enable JavaScript source maps` enabled in Chrome or your preferred browser so see the original source before it was transpiled.

### Unit Tests

To get the unit tests to run automatically when you change source files, do:

```
$ grunt karma:dev
```

This will run karma and show test output as tests are run. _You should run this in addition to running grunt as shown
above_.

If you need to debug test failures, open [http://localhost:9876/debug.html](http://localhost:9876/debug.html) in your
browser of choice and open the development tools. Test failures will be reported in the console.
