package org.powertac.server

import org.powertac.common.Competition

class TimeslotManagementService {

  static transactional = true

  def randomSeedService


  def store (xml) {
    if (!xml) return "No message to store provided."
    def msg = new DemoMessage(contents: xml.toString())
    if (!msg.validate()) return "Error storing message: ${msg.errors}"
    if (!msg.save()) return "Failed to save message to db."
    return "Message saved to db. Random Value: ${randomSeedService.nextGaussian(Competition.currentCompetition?.id?: 'none', this.class.name, null, 'test')}"
  }


  def handleShout(shoutXml) {
    if (!shoutXml) return "No shout to store."
    def msg = new DemoMessage(contents: shoutXml)
    if (!msg.validate()) return "Error storing message: ${msg.errors}"
    if (!msg.save()) return "Failed to save message to db."
    return "Message saved to db."
  }
}
