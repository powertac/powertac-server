package org.powertac.server

import grails.converters.XML
import org.powertac.common.PositionUpdate

class PositionUpdateController {

  def scaffold = PositionUpdate

  def get = {
    def positionUpdateInstance = PositionUpdate.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: positionUpdateInstance as XML)
  }
}
