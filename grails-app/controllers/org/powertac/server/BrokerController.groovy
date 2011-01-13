package org.powertac.server

import grails.converters.XML
import org.powertac.common.Broker

class BrokerController {

  def scaffold = Broker

  def get = {
    def brokerInstance = Broker.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: brokerInstance as XML)
  }
}
