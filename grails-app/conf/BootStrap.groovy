import org.powertac.common.Broker
import org.powertac.common.Competition
import org.powertac.common.Product
import org.powertac.common.Timeslot
import org.powertac.common.enumerations.ProductType
import org.powertac.common.Role
import org.powertac.common.BrokerRole

class BootStrap {

  def springSecurityService

  def init = { servletContext ->
    // Create admin role
    def adminRole = Role.findByAuthority('ROLE_ADMIN') ?: new Role(authority: 'ROLE_ADMIN').save(failOnError: true)
    // Create default broker
    def adminUser = Broker.findByUsername('defaultBroker') ?: new Broker(
        username: 'defaultBroker',
        password: springSecurityService.encodePassword('password'),
        enabled: true).save(failOnError: true)

    // Add default broker to admin role
    if (!adminUser.authorities.contains(adminRole)) {
      BrokerRole.create adminUser, adminRole
    }

//    def competition = new Competition(name: 'testCompetition').save()
//    new Broker(userName: 'testUser', apiKey: 'testApiKey-which-needs-to-be-longer-than-32-characters').save()
//    //new Product(productType: ProductType.Future).save()
//    new Timeslot(serialNumber: 0).save()

  }
  def destroy = {
  }
}
