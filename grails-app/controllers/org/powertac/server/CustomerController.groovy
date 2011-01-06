package org.powertac.server

import grails.converters.XML
import org.powertac.common.Customer

class CustomerController {

  def scaffold = Customer

  def toXml = {
    def customerInstance = Customer.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: customerInstance as XML)
  }
}
