package org.powertac.server.converter

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import org.powertac.common.Broker
import org.powertac.common.Competition
import org.powertac.common.Product
import org.powertac.common.Timeslot
import org.powertac.common.command.ShoutDoCreateCmd
import org.powertac.common.enumerations.BuySellIndicator

/**
 * TODO: Add Description
 *
 * @author Carsten Block
 * @version 1.0 , Date: 17.01.11
 */
class ShoutDoCreateCmdConverter implements Converter {

  public boolean canConvert(Class aClass) {
    return aClass.equals(ShoutDoCreateCmd.class);
  }

  void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext marshallingContext) {
    ShoutDoCreateCmd cmd = (ShoutDoCreateCmd) o;

    writer.startNode("competition")
    if (cmd.competition) writer.addAttribute('id', cmd.competition.id)
    writer.endNode() //competition

    writer.startNode('broker')
    if (cmd.broker) writer.addAttribute('id', cmd.broker.id)
    writer.endNode() //broker

    writer.startNode('product')
    if (cmd.product) writer.addAttribute('id', cmd.product.id)
    writer.endNode() //product

    writer.startNode('timeslot')
    if (cmd.timeslot) writer.addAttribute('id', cmd.timeslot.id)
    writer.endNode() //timeslot

    writer.startNode('buySellIndicator')
    if (cmd.buySellIndicator) writer.setValue(cmd.buySellIndicator.toString())
    writer.endNode() //buySellIndicator

    writer.startNode('quantity')
    if (cmd.quantity) writer.setValue(cmd.quantity.toString())
    writer.endNode() //quantity

    writer.startNode('limitPrice')
    if (cmd.limitPrice) writer.setValue(cmd.limitPrice.toString())
    writer.endNode() //limitPrice

    writer.startNode('orderType')
    if (cmd.orderType) writer.setValue(cmd.orderType.toString())
    writer.endNode() //orderType
  }

  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext unmarshallingContext) {
    ShoutDoCreateCmd cmd = new ShoutDoCreateCmd()
    reader.moveDown()
    cmd.competition = Competition.get(reader.getAttribute('id'))

    reader.moveDown()
    cmd.broker = Broker.get(reader.getAttribute('id'))

    reader.moveDown()
    cmd.product = Product.get(reader.getAttribute('id'))

    reader.moveDown()
    cmd.timeslot = Timeslot.get(reader.getAttribute('id'))

    reader.moveDown()
    cmd.buySellIndicator = reader.value as BuySellIndicator

    reader.moveDown()
    cmd.quantity = reader.value as BigDecimal

    reader.moveDown()
    cmd.limitPrice = reader.value as BigDecimal

    reader.moveDown()
    cmd.orderType = reader.value as BigDecimal
  }
}
