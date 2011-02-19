package org.powertac.server

import grails.converters.XML
import org.powertac.common.CustomerInfo

class CustomerController {

  def scaffold = CustomerInfo

  def get = {
    def customerInstance = CustomerInfo.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: customerInstance as XML)
  }
}
