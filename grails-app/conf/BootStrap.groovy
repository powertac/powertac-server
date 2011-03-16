import org.powertac.common.Broker
import org.powertac.common.Competition
import org.powertac.common.Product
import org.powertac.common.Timeslot
import org.powertac.common.enumerations.ProductType

class BootStrap {

  def init = { servletContext ->
    environments {
      development {
        def competition = new Competition(name: 'testCompetition').save()
        new Broker(userName: 'testUser', apiKey: 'testApiKey-which-needs-to-be-longer-than-32-characters').save()
        //new Product(productType: ProductType.Future).save()
        new Timeslot(serialNumber: 0).save()
      }
    }
  }
  def destroy = {
  }
}
