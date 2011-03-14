package org.powertac.server

import grails.converters.XML
import org.powertac.common.enumerations.BuySellIndicator
import org.powertac.common.enumerations.OrderType
import org.powertac.common.*

class ShoutController {

  def xmlUnMarshaller

  def scaffold = Shout
  def get = {
    def shoutInstance = Shout.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: shoutInstance as XML)
  }

  def doCreate = {
    def competition = Competition.currentCompetition()
    def timeslot = Timeslot.currentTimeslot()
    def broker = Broker.list()
    def product = Product.list()

    def cmd = new Shout(broker: broker, timeslot: timeslot, product: product, buySellIndicator: BuySellIndicator.BUY, quantity: 1.0, limitPrice: 10.0, orderType: OrderType.LIMIT)
    response.contentType = 'text/xml'

    render (contentType: 'text/xml', encoding: 'UTF-8', text: xmlUnMarshaller.getXStream().toXML(cmd))
  }
}
