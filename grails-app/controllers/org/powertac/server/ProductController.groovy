package org.powertac.server

import grails.converters.XML
import org.powertac.common.Product

class ProductController {

  def scaffold = Product

  def toXml = {
    def productInstance = Product.get(params.id)
    render(contentType: "text/xml", encoding: "UTF-8", text: productInstance as XML)
  }
}
