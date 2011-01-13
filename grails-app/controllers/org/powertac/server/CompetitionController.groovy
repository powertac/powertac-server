package org.powertac.server

import grails.converters.XML
import org.powertac.common.Competition

class CompetitionController {

  def scaffold = Competition


  /* REST mappings see grails-app/conf/UrlMappings.groovy */

  def get = {
    def competitionInstance = Competition.get(params.id)
    if (!competitionInstance) {
      render(contentType: "text/xml", encoding: "UTF-8", text: "<error type='not found'/>")
    } else {
     render(contentType: "text/xml", encoding: "UTF-8", text: competitionInstance as XML)
    }
  }

  def add = {
    def competition = new Competition(params.competition)
    competition.save()
    if (!competition?.hasErrors()) {
      render competition as XML
    } else {
      render competition.errors as XML
    }
  }
}
