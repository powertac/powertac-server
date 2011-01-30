import org.powertac.common.Broker
import org.powertac.common.Competition
import org.powertac.common.Product
import org.powertac.common.Timeslot
import org.powertac.common.enumerations.ProductType

class BootStrap {

  def init = { servletContext ->
    environments {
      development {
        def competition = new Competition(name: 'testCompetition', enabled: true, current: true).save()
        new Broker(competition: competition, userName: 'testUser', apiKey: 'testApiKey-which-needs-to-be-longer-than-32-characters').save()
        new Product(competition: competition, productType: ProductType.Future).save()
        new Timeslot(competition: competition, serialNumber: 0).save()
      }
    }
  }
  def destroy = {
  }
}
