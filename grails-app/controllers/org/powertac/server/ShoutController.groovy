package org.powertac.server

import grails.converters.XML
import org.powertac.common.Shout

class ShoutController {

  def scaffold = Shout
  def toXml = {
    def shoutInstance = Shout.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: shoutInstance as XML)
  }
}
