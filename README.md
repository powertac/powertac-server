# Visualizer2

## Getting started

(The following assume you're at the project root.)

## User accounts

Out of the box, two accounts are created: `admin` and `user`. The passwords are 
initially `admin` and `user`, respectively. You may want to change that when
you're up and running.

In the new visualizer, games are owned by the user who logged in to create/start
them. To account for this, the log files are now written to user-specific
directories, e.g. "files/admin/log".

## Modes and Profiles

There are four mode/profile-specific configuration files

You can run the visualizer in two modes, "research" and "tournament". In the first case, you will get some additional tabs in the
GUI to start/stop games, which are run embedded in the visualizer. In the second case, you will be able to passively view the progress
of games run by an external tournament scheduler.

Additionally, you can build the visualizer with "dev" or in "prod" profiles, the latter meaning that the javascript, styles, and html
templates will be concatenated and compressed -- making the pages slightly faster to load.

So you will have to pick one of four combinations. To run the visualizer in, e.g. research mode and production profile, do

```sh
mvn -Pprod,research
```

This will select the default Maven goal, which is `spring-boot:run`. Alternatively, you can select the `package` goal to build a WAR
file, e.g. in tournament mode and production profile:

```sh
mvn -Pprod,tournament package
```

Note, this will create *two* WAR files, in the `target` directory. One contains an embedded Tomcat servlet container, and can be run as
an executable jar:

```sh
java -jar target/xxxxx.war
```

The other WAR file is called `xxxxx.war.original` and can be deployed to external servlet containers like Tomcat 8.

Unless you're actually developing the visualizer itself, you'll probably want to use the "prod" profile. However, this will cause the
build process to take a long time, in particular it will be doing a lot of tests. If you would like to skip these, you can add `-DskipTests`
to your Maven commands.

# JHipster generic documentation

This application was generated using JHipster, you can find documentation and help at [https://jhipster.github.io](https://jhipster.github.io).

## Development

Before you can build this project, you must install and configure the following dependencies on your machine:

1. [Node.js][]: We use Node to run a development web server and build the project.
   Depending on your system, you can install Node either from source or as a pre-packaged bundle.

After installing Node, you should be able to run the following command to install development tools (like
[Bower][] and [BrowserSync][]). You will only need to run this command when dependencies change in package.json.

    npm install

We use [Gulp][] as our build system. Install the Gulp command-line tool globally with:

    npm install -g gulp

Run the following commands in two separate terminals to create a blissful development experience where your browser
auto-refreshes when files change on your hard drive.

    ./mvnw
    gulp

Bower is used to manage CSS and JavaScript dependencies used in this application. You can upgrade dependencies by
specifying a newer version in `bower.json`. You can also run `bower update` and `bower install` to manage dependencies.
Add the `-h` flag on any command to see how you can use it. For example, `bower update -h`.


## Building for production

To optimize the visualizer2 client for production, run:

    ./mvnw -Pprod clean package

This will concatenate and minify CSS and JavaScript files. It will also modify `index.html` so it references
these new files.

To ensure everything worked, run:

    java -jar target/*.war

Then navigate to [http://localhost:8080](http://localhost:8080) in your browser.

## Testing

Unit tests are run by [Karma][] and written with [Jasmine][]. They're located in `src/test/javascript/` and can be run with:

    gulp test



## Continuous Integration

To setup this project in Jenkins, use the following configuration:

* Project name: `visualizer2`
* Source Code Management
    * Git Repository: `git@github.com:xxxx/visualizer2.git`
    * Branches to build: `*/master`
    * Additional Behaviours: `Wipe out repository & force clone`
* Build Triggers
    * Poll SCM / Schedule: `H/5 * * * *`
* Build
    * Invoke Maven / Tasks: `-Pprod clean package`
* Post-build Actions
    * Publish JUnit test result report / Test Report XMLs: `build/test-results/*.xml`

[JHipster]: https://jhipster.github.io/
[Node.js]: https://nodejs.org/
[Bower]: http://bower.io/
[Gulp]: http://gulpjs.com/
[BrowserSync]: http://www.browsersync.io/
[Karma]: http://karma-runner.github.io/
[Jasmine]: http://jasmine.github.io/2.0/introduction.html
[Protractor]: https://angular.github.io/protractor/
