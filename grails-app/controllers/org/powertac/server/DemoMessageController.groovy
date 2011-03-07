package org.powertac.server

import org.powertac.common.msg.ShoutDoCreateCmd
import org.powertac.common.enumerations.BuySellIndicator
import org.powertac.common.enumerations.OrderType

class DemoMessageController {

  def demoMessagingGateway
  def commandEncode

  def scaffold = DemoMessage

  def send = {
    if (!params.id) {
      flash.message = "No message id given"
      redirect(action: 'list')
    } else {
      def demoMessage = DemoMessage.get(params.id)
      if (!demoMessage) {
        flash.message = "Message with id ${params.id} not found."
        redirect(action: 'list')
      } else {
        flash.message = demoMessagingGateway.sendMessage(demoMessage.contents)
        redirect(action: 'show', id: params.id)
      }
    }
  }

  def encodeShout = {
    ShoutDoCreateCmd cmd = new ShoutDoCreateCmd(competitionId: 'testCompetition', userName: 'testUser', apiKey: 'testKey', productId: 'testProductId', timeslotId: 'testTimeslotId', buySellIndicator: BuySellIndicator.BUY, quantity: 1.0298, limitPrice: 93.0235, orderType: OrderType.LIMIT)
    flash.message = commandEncode.encodeShoutDoCreateCommand(cmd)
    redirect action: 'list'
  }
}
