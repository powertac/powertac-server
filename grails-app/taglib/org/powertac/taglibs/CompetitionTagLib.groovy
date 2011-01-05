/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 *
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.taglibs

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import org.apache.commons.lang.RandomStringUtils
import org.powertac.common.Competition
import org.powertac.common.Orderbook
import org.powertac.common.Product
import org.powertac.common.TransactionLog
import org.powertac.common.enumerations.CompetitionStatus

class CompetitionTagLib {

  def ifCompetitionRunning = { attrs, body ->
    if (Competition.findByCompetitionStatus(CompetitionStatus.Running)) {
      out << body()
    }
  }

  def ifCompetitionNotRunning = { attrs, body ->
    if (!Competition.findByCompetitionStatus(CompetitionStatus.Running)) {
      out << body()
    }
  }

  def running = {attrs, body ->
    def competition = Competition.findByCompetitionStatus(CompetitionStatus.Running)
    if (competition) {
      def model = [:]
      model.competition = competition
      model.productList = Product.findAllByEnabledAndCompetition(true, competition)
      model.transactionLogList = TransactionLog.findAllByCompetition(competition, [sort: 'dateCreated', order: 'desc', max: 5])
      out << render(template: "currentCompetition", model: model)
    }
  }


  def participantReadyList = {attrs, body ->
    if (attrs.competition) {
      attrs.competition?.competitionParticipants?.each {personCompetition ->
        out << render(template: "participantReadyStatus", model: [personCompetition: personCompetition])
      }
    }
  }

  def nextCompetitions = {attrs, body ->
    def competitions = Competition.withCriteria {
      'in'('competitionStatus', [CompetitionStatus.Initialized, CompetitionStatus.Created, CompetitionStatus.Scheduled, CompetitionStatus.Ready])
      maxResults(10)
    }
    if (!competitions) {
      out << render(template: "noNextCompetitions")
    } else {
      out << render(template: "nextCompetitions", model: [competitionList: competitions.sort()])
    }
  }

  def announcements = {attrs, body ->
    def now = new Date()
    def announcements = Announcement.findAllByDisplayFromLessThanEqualsAndDisplayUntilGreaterThan(now,now, [cache: true])
    if (announcements) {
      out << render(template: "/announcement", model: [announcements: announcements])
    }
  }

    def productAndOrderbook = {attrs, body ->
        Product orderbookProduct = Product.get(attrs.product.id)

        if (orderbookProduct) {
            def orderbookInstance = Orderbook.withCriteria(uniqueResult: true) {
                maxResults(1)
                eq('product', orderbookProduct)
                eq('outdated', false)
            }

            out << render(template: "currentCompetitionProduct", model: [productInstance: orderbookProduct, orderbookInstance: orderbookInstance])
        }
    }

    def orderbookSafeFormatter = {attrs, body ->
        def number = attrs.value

        // needs to exist and be > 0
        if (number) {
            out << number
        } else {
            out << "&nbsp;"
        }
    }

    def orderbookSafeNumberFormatter = {attrs, body ->
        def format = "###,##0.00 EUR"
        def number = attrs.value

        if (number) {
            DecimalFormatSymbols dcfs = new DecimalFormatSymbols()
            DecimalFormat decimalFormat = new java.text.DecimalFormat(format, dcfs)
            decimalFormat.setParseBigDecimal(true)

            if (!(number instanceof Number)) {
                number = decimalFormat.parse(number as String)
            }

            def formatted = decimalFormat.format(number)

            out << formatted
        } else {
            out << "&nbsp;"
        }
    }

    def plotCash = {attrs, body ->
        def competitionInstance = Competition.findByCurrent(true, [cache: true])

        if (!competitionInstance) {
            out << "Error: No competition found"
        } else {
            def personInstance = Person.get(attrs.person)
            def cashPositions = CashPosition.findAllByPerson(personInstance, [sort: 'dateCreated', order: 'asc'])

            if (!cashPositions) {
                if (attrs.type != "small") {
                    out << "Error: No cash positions found"
                }
            } else {
                def cashValues = []
                def baseline = []
                def needsBaseline = false
                def i = 1

                cashPositions?.each {cashPosition ->
                    cashValues.add([i++, cashPosition.balance])
                    if (cashPosition.balance < 0) needsBaseline = true
                }

                if (needsBaseline) {
                    baseline.add([0,0])
                    baseline.add([i,0])
                }

                def templateName = (attrs.type == "small") ? "currentCompetitionCashGraphSmall" : "currentCompetitionCashGraph"
                def randomId = RandomStringUtils.randomNumeric(10)

                out << render(template: templateName, model: [competitionInstance: competitionInstance,
                        personInstance: personInstance,
                        cashValues: cashValues,
                        baseline: baseline,
                        randomId: randomId
                ])
            }
        }
    }

    def plotDepot = {attrs, body ->
        def competitionInstance = Competition.findByCurrent(true, [cache: true])

        if (!competitionInstance) {
            out << "Error: No competition found"
        } else {
            Locale clientLocale = request.getLocale()
            Calendar calendar = Calendar.getInstance(clientLocale)
            TimeZone clientTimeZone = calendar.getTimeZone()
            Integer timezoneOffset = null

            def personInstance = Person.get(attrs.person)
            def products = Product.findAllByCompetition(competitionInstance, [sort: 'serialNumber', order: 'asc'])

            if (!products) {
                if (attrs.type != "small") {
                    out << "Error: No products found"
                }
            } else {
                List forecastList = Forecast.withCriteria {
                    eq('person', personInstance)
                    eq('competition', competitionInstance)
                    eq('competitionTime', competitionInstance.currentCompetitionTime)
                }
                def depotPositionList = DepotPosition.withCriteria {
                    eq('competition', competitionInstance)
                    eq('person', personInstance)
                    eq('latest', true)
                }
                def forecastDemand = []
                def realDemand = []
                def acquiredDemand = []

                products?.each {product ->
                    long startDateTime = product.startDateTime.getTime()
                    if (timezoneOffset == null) timezoneOffset = clientTimeZone.getOffset(startDateTime)
                    startDateTime += timezoneOffset
                    def forecastInstance = forecastList.find {it.product == product}

                    if (forecastInstance) {
                        forecastDemand.add([startDateTime, forecastInstance.forecastValue])
                        realDemand.add([startDateTime, forecastInstance.trueValue])
                        forecastList.remove(forecastInstance)
                    }

                    def depotPosition = depotPositionList.find {it.product == product}
                    if (depotPosition) {
                        acquiredDemand.add([startDateTime, depotPosition.balance])
                        depotPositionList.remove(depotPosition)
                    }
                }

                def templateName = (attrs.type == "small") ? "currentCompetitionDepotGraphSmall" : "currentCompetitionDepotGraph"
                def randomId = RandomStringUtils.randomNumeric(10)
                out << render(template: templateName, model: [competitionInstance: competitionInstance,
                        personInstance: personInstance,
                        forecastDemand: forecastDemand,
                        reaLDemand: realDemand,
                        acquiredDemand: acquiredDemand,
                        timezoneOffset: timezoneOffset,
                        randomId: randomId
                ])
            }
        }
    }

    def cashAccount = {attrs, body ->
        def competitionInstance = Competition.findByCurrent(true, [cache: true])
        if (competitionInstance) {

            def personInstance = Person.get(attrs.person)
            def cashPosition = CashPosition.findByPersonAndLatest(personInstance, true)

            if (cashPosition) {
                def colorClass = (cashPosition.balance < 0) ? "color-red" : "color-green"
                out << '<span class="'
                out << colorClass
                out << '">'
                out << cashPosition.balance
                out << "</span>"
            } else {
                out << '<span class="color-green">0.0</span>'
            }
        }
    }
}
