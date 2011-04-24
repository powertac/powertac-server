import org.powertac.DefaultBroker
import org.powertac.common.Broker
import org.powertac.common.BrokerRole
import org.powertac.common.Competition
import org.powertac.common.Role

class BootStrap {

  def springSecurityService
  def simpleGencoService

  def init = { servletContext ->

    // Create admin role
    def adminRole = Role.findByAuthority('ROLE_ADMIN') ?: new Role(authority: 'ROLE_ADMIN').save(failOnError: true)

    // Create default broker which is admin at the same time
    def defaultBroker = Broker.findByUsername('defaultBroker') ?: new DefaultBroker(
        username: 'defaultBroker', local: true,
        password: springSecurityService.encodePassword('password'),
        enabled: true)
    defaultBroker.save(failOnError: true)

    // Add default broker to admin role
    if (!defaultBroker.authorities.contains(adminRole)) {
      BrokerRole.create defaultBroker, adminRole
    }

    // Initialize the genco service
    simpleGencoService.init()

    // Create default competition
    def competition = new Competition(name: "defaultCompetition").save()

  }
  def destroy = {
  }
}
