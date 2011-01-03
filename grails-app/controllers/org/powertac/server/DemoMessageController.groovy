package org.powertac.server

class DemoMessageController {

  def demoMessagingGateway

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
}
