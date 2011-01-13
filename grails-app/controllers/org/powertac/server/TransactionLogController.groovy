package org.powertac.server

import grails.converters.XML
import org.powertac.common.TransactionLog

class TransactionLogController {

  def scaffold = TransactionLog

  def get = {
    def transactionLogInstance = TransactionLog.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: transactionLogInstance as XML)
  }
}
