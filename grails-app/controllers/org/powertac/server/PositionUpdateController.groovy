package org.powertac.server

import grails.converters.XML
import org.powertac.common.MarketPosition

class PositionUpdateController {

  def scaffold = MarketPosition

  def get = {
    def positionUpdateInstance = MarketPosition.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: positionUpdateInstance as XML)
  }
}
