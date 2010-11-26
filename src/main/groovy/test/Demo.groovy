package test

import org.powertac.server.core.domain.Competition
import org.powertac.server.core.domain.Broker

class Demo {
  public static void main(String[] args) {
    def comp1 = new Competition(name: 'test competition1').persist()
    def comp2 = new Competition(name: 'test competition2').persist()
    def comp3 = new Competition(name: 'test competition3').persist()

    Set<Competition> competitions = Competition.findAllCompetitions()
    competitions.each {competition ->
      println "Competition ${competition.id}: ${competition.name}"
    }
  }
}
