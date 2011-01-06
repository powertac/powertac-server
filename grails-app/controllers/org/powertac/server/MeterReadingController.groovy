package org.powertac.server

import grails.converters.XML
import org.powertac.common.MeterReading

class MeterReadingController {

  def scaffold = MeterReading

  def toXml = {
    def meterReadingInstance = MeterReading.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: meterReadingInstance as XML)
  }

}
