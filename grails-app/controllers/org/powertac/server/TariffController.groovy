package org.powertac.server

import grails.converters.XML
import org.powertac.common.Tariff

class TariffController {

  def scaffold = Tariff

  def get = {
    def tariffInstance = Tariff.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: tariffInstance as XML)
  }
}
