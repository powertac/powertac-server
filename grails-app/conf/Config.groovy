// import grails.plugins.springsecurity.SecurityConfigType

// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [html: ['text/html', 'application/xhtml+xml'],
    xml: ['text/xml', 'application/xml'],
    text: 'text/plain',
    js: 'text/javascript',
    rss: 'application/rss+xml',
    atom: 'application/atom+xml',
    css: 'text/css',
    csv: 'text/csv',
    all: '*/*',
    json: ['application/json', 'text/json'],
    form: 'application/x-www-form-urlencoded',
    multipartForm: 'multipart/form-data'
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// whether to install the java.util.logging bridge for sl4j. Disable for AppEngine!
grails.logging.jul.usebridge = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []

// set per-environment serverURL stem for creating absolute links
environments {
  production {
    //grails.serverURL = "http://xlarge.rsm.nl:8080/powertac"
    grails.serverURL = "http://localhost:8080/powertac"
  }
  development {
    grails.serverURL = "http://localhost:8080/${appName}"
  }
  test {
    grails.serverURL = "http://localhost:8080/${appName}"
  }

}

def catalinaBase = System.properties.getProperty('catalina.base')
if (!catalinaBase) catalinaBase = '.'   // just in case
def logDirectory = "${catalinaBase}/logs"

// default for all environments
log4j = {
  appenders {
    console name: 'stdout', threshold: org.apache.log4j.Level.WARN
    file name: 'file', file: 'logs/powertac-server.log', append: false
  }

  error 'org.codehaus',
      'org.springframework',
      'org.hibernate',
      'org.activemq',
      'net.sf.ehcache'
      
  info 'grails.app'
  debug 'grails.app.service.org.powertac.server'
        // 'grails.app.domain.org.powertac.genco'
        // 'org.hibernate.SQL'

  root {
    warn 'file', 'stdout'
  }
}

// special settings with development env
environments {
  development {
  }
}

// Added by the Joda-Time plugin:
grails.gorm.default.mapping = {
  "user-type" type: org.joda.time.contrib.hibernate.PersistentDateTime, class: org.joda.time.DateTime
  "user-type" type: org.joda.time.contrib.hibernate.PersistentDuration, class: org.joda.time.Duration
  "user-type" type: org.joda.time.contrib.hibernate.PersistentInstant, class: org.joda.time.Instant
  "user-type" type: org.joda.time.contrib.hibernate.PersistentInterval, class: org.joda.time.Interval
  "user-type" type: org.joda.time.contrib.hibernate.PersistentLocalDate, class: org.joda.time.LocalDate
  "user-type" type: org.joda.time.contrib.hibernate.PersistentLocalTimeAsString, class: org.joda.time.LocalTime
  "user-type" type: org.joda.time.contrib.hibernate.PersistentLocalDateTime, class: org.joda.time.LocalDateTime
  "user-type" type: org.joda.time.contrib.hibernate.PersistentPeriod, class: org.joda.time.Period
}

//graphviz.dot.executable='/usr/local/bin/dot'

//PowerTAC specific settings
powertac {
  jmx.broker.url = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi"
  connector.url = "failover:(tcp://127.0.0.1:61616)"
  broker.url = 'tcp://localhost:61616'
  //jmx.broker.url = "service:jmx:rmi:///jndi/rmi://xlarge.rsm.nl:1099/jmxrmi"
  //connector.url = "failover:(tcp://xlarge.rsm.nl:61616)"
  //broker.url = 'tcp://xlarge.rsm.nl:61616'
}
// Added by the powertac-common plugin:
grails.validateable.packages = ['org.powertac.common.command']

// Added by the Spring Security Core plugin
grails.plugins.springsecurity.userLookup.userDomainClassName = 'org.powertac.common.Broker'
grails.plugins.springsecurity.userLookup.authorityJoinClassName = 'org.powertac.common.BrokerRole'
grails.plugins.springsecurity.authority.className = 'org.powertac.common.Role'

// Permission mapping
//grails.plugins.springsecurity.securityConfigType = SecurityConfigType.InterceptUrlMap
//grails.plugins.springsecurity.interceptUrlMap = [
//    '/*': ["hasRole('ROLE_ADMIN')"]
//]
