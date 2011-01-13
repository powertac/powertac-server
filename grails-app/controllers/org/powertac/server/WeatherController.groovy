package org.powertac.server

import grails.converters.XML
import org.powertac.common.Weather

class WeatherController {

  def scaffold = Weather

  def get = {
    def weatherInstance = Weather.get(params.id)
    render (contentType:"text/xml",encoding:"UTF-8", text: weatherInstance as XML)
  }
}
