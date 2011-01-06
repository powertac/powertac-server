package org.powertac.server

import grails.converters.XML
import org.powertac.common.Competition

class CompetitionController {

  def scaffold = Competition

  def toXml = {
    def competitionInstance = Competition.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: competitionInstance as XML)
  }
}
