package org.powertac.server

import grails.converters.XML
import org.powertac.common.TariffTransaction

class MeterReadingController {

  def scaffold = TariffTransaction

  def get = {
    def meterReadingInstance = TariffTransaction.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: meterReadingInstance as XML)
  }

}
