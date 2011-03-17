grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"

// development plugin locations
grails.plugin.location.PowertacCommon = "../powertac-common"
grails.plugin.location.PowertacServerInterface = "../powertac-server-interface"
grails.plugin.location.PowertacAccountingService = "../powertac-accounting-service"
grails.plugin.location.PowertacWebApp = "../powertac-web-app"

grails.project.dependency.resolution = {
  // inherit Grails' default dependencies
  inherits("global") {
    // uncomment to disable ehcache
    // excludes 'ehcache'
  }
  log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
  repositories {
    grailsPlugins()
    grailsHome()
    mavenRepo "http://ibwstinger.iw.uni-karlsruhe.de/artifactory/plugins-release-local/"
    mavenRepo "http://ibwstinger.iw.uni-karlsruhe.de/artifactory/plugins-snapshot-local/"
    mavenRepo "http://ibwstinger.iw.uni-karlsruhe.de/artifactory/libs-release-local/"
    grailsCentral()

    // uncomment the below to enable remote dependency resolution
    // from public Maven repositories
    mavenLocal()
    mavenCentral()
    mavenRepo "http://snapshots.repository.codehaus.org"
    mavenRepo "http://repository.codehaus.org"
    mavenRepo "http://download.java.net/maven/2/"
    mavenRepo "http://repository.jboss.com/maven2/"
    mavenRepo "http://repository.apache.org"
    mavenRepo "http://repository.apache.org/snapshots"
  }

  plugins {
    //compile "org.powertac.plugins:powertac-common:0.9 > *"
    //compile "powertac-common"
    
  }

  dependencies {
    // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
    // runtime 'mysql:mysql-connector-java:5.1.13'
	runtime 'com.thoughtworks.xstream:xstream:1.3.1'
    compile('org.apache.activemq:activemq-core:5.4.1') {
      excludes 'commons-collections',  'commons-logging', 'junit', 'log4j', 'spring-context', 'spring-parent', 'spring-aop', 'spring-asm', 'spring-beans', 'spring-expression'
    }

    compile('org.apache.activemq:activemq-pool:5.4.1') {
      excludes 'commons-collections', 'commons-pool', 'commons-logging',  'junit',  'log4j',  'spring-context',  'spring-parent',  'spring-aop',  'spring-asm',  'spring-beans',  'spring-expression'
    }
  }
}

coverage {
  xml = true
  nopost = true

  exclusions = [
      'org/grails/**',
      '**BuildConfig**']
}
