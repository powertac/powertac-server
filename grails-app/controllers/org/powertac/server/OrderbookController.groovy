package org.powertac.server

import grails.converters.XML
import org.powertac.common.Orderbook

class OrderbookController {

  def scaffold = Orderbook

  def toXml = {
    def orderbookInstance = Orderbook.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: orderbookInstance as XML)
  }
}
