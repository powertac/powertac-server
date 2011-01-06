package org.powertac.server

import grails.converters.XML
import org.powertac.common.CashUpdate

class CashUpdateController {

  def scaffold = CashUpdate

  def toXml = {
    def cashUpdateInstance = CashUpdate.get(params.id)
    render (contentType:"text/xml",encoding:"UTF-8", text: cashUpdateInstance as XML)
  }
}
