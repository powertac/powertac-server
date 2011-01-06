package org.powertac.server

import grails.converters.XML
import org.powertac.common.Timeslot

class TimeslotController {

  def scaffold = Timeslot

  def toXml = {
    def timeslotInstance = Timeslot.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: timeslotInstance as XML)
  }
}
