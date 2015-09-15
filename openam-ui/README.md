# OpenAM XUI

## Building

You can build the package using Maven:

```
$ maven install
```

This will output a zip file in the build artifact directory. For example, `target/openam-ui-ria-<version>-www.zip`.

## Development

Currently, you'll find the majority of the source in the `openam-ui-ria` module. The build process uses
[Grunt](http://gruntjs.com/) to manage the running of other tools, which is installed via [npm](https://www.npmjs.com/).

If you want to develop the UI, we strongly recommend running Grunt to manage syncing the source code with your running
application server. To do this, there are a few prerequisites:

* Ensure you have [Node.js](https://nodejs.org/) installed.
* Add an environment variable, `OPENAM_HOME`, that points to an expanded OpenAM install in your application server's
webapp directory. For example, `OPENAM_HOME=~/tomcat/webapps/openam`.

then do:

```
$ cd openam-ui-ria
$ npm install
$ grunt
```

Grunt will then start watching the source and sync any changed files over to your server's webapp directory.