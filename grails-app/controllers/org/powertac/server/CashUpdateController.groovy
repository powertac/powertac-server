package org.powertac.server

import grails.converters.XML
import org.powertac.common.CashPosition

class CashUpdateController {

  def scaffold = CashPosition

  def get = {
    def cashUpdateInstance = CashPosition.get(params.id)
    render (contentType:"text/xml",encoding:"UTF-8", text: cashUpdateInstance as XML)
  }
}
