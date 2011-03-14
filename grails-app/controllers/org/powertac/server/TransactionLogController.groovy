package org.powertac.server

import grails.converters.XML
import org.powertac.common.MarketTransaction

class TransactionLogController {

  def scaffold = MarketTransaction

  def get = {
    def transactionLogInstance = MarketTransaction.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: transactionLogInstance as XML)
  }
}
