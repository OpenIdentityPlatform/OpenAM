# OpenAM XUI

<!-- TOC depthFrom:2 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Dependencies](#dependencies)
	- [Updating](#updating)
- [Building](#building)
- [Development](#development)
	- [ES6 Support](#es6-support)
	- [Unit Tests](#unit-tests)

<!-- /TOC -->

## Dependencies
NPM dependencies for this project are "locked" to specific versions through two mechanisms:
* Explicit version expressions (e.g. `"lodash": "4.1.0"` n.b. no `"^"`) in `package.json`
* Shrink-wrapping of deeper dependencies using `npm shrinkwrap --dev` (e.g. `npm-shrinkwrap.json` contains a full set of explicit dependency versions for the entire dependency tree)

Updates to dependency versions are performed manually in a periodic manner. Interim version updates are allowed but must still follow the update process described below.

### Updating
1. Ensure you have [npm-check-updates](https://www.npmjs.com/package/npm-check-updates) installed globally. e.g. `npm i -g npm-check-updates`
2. Run `ncu` and observe the updates availalble
  * ***Be mindful of breaking changes!***. Major version changes will most likely require code changes to align with changes in a dependency's API. A major dependency version update should be performed in a separate task.
3. Run `npm run deps` to automatically update ***all*** dependencies
    * Using this automated process, ***all dependencies will be updated***. If you wish to update only a subset of the dependencies, perform the automated steps manually instead (as listed in `package.json`).
4. Commit it!

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
$ npm start
```

Grunt will then start watching the source and sync any changed files over to your server's webapp directory.

### ES6 Support
ES6 is supported via [Babel](https://babeljs.io) transpiling. All `.js` and `.jsm` files are transpiled *except* for `libs` and any commons source (layer on afterwards at build time).

Ensure you have `Enable JavaScript source maps` enabled in Chrome or your preferred browser so see the original source before it was transpiled.

### Unit Tests
To get the unit tests to run automatically when you change source files, do:

```
npm test
```

This will run karma and show test output as tests are run. _You should run this in addition to running `npm start` as shown
above_.

If you need to debug test failures, open [http://localhost:9876/debug.html](http://localhost:9876/debug.html) in your
browser of choice and open the development tools. Test failures will be reported in the console.
