import org.powertac.common.Broker
import org.powertac.common.Competition
import org.powertac.common.Product
import org.powertac.common.Timeslot
import org.powertac.common.enumerations.ProductType
import org.powertac.common.Role
import org.powertac.common.BrokerRole

class BootStrap {

  def springSecurityService
  def simpleGencoService

  def init = { servletContext ->

    // Create admin role
    def adminRole = Role.findByAuthority('ROLE_ADMIN') ?: new Role(authority: 'ROLE_ADMIN').save(failOnError: true)

    // Create grails sample broker which is admin at the same time
    def adminUser = Broker.findByUsername('grailsDemo') ?: new Broker(
        username: 'grailsDemo',
        password: springSecurityService.encodePassword('password'),
        enabled: true).save(failOnError: true)

    // Add default broker to admin role
    if (!adminUser.authorities.contains(adminRole)) {
      BrokerRole.create adminUser, adminRole
    }
    
    // Initialize the genco service
    simpleGencoService.init()

    // Create default competition
    def competition = new Competition(name: "defaultCompetition").save()

  }
  def destroy = {
  }
}
