package org.powertac.visualizer.wholesale;

import java.util.Iterator;
import java.util.SortedSet;

import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;

/**
 * Represents the wholesale market per-timeslot snapshot for one timeslot.
 * @author Jurica Babic
 *
 */
public class WholesaleSnapshot {
	
	//filled with actual received Orders that are previously converted to OrderbookOrder.
	private Orderbook orders;
	//cleared trade, null if there is no trade
	private ClearedTrade clearedTrade;
	//standard orderbook:
	private Orderbook orderbook;
	/* indicates whether the observed snapshot is closed. 
	Snapshot can be closed when there is null price in orderbook, or when the clearedTrade object is set. 
	*/
	private boolean closed;
	
	public void addOrder(Order order){
		//lazy initialization of orders orderbook, so we can use timeslot in its constructor.
		if(orders==null){
			orders= new Orderbook(order.getTimeslot(), null, null);
		}
		
		//bid:
		if(order.getMWh()>0){
			orders.addBid(new OrderbookOrder(order.getMWh(), order.getLimitPrice()));
		} else {
			orders.addAsk(new OrderbookOrder(order.getMWh(), order.getLimitPrice()));
		}
	}
	
	
	public Orderbook getOrders() {
		return orders;
	}
	public void setOrders(Orderbook orders) {
		this.orders = orders;
	}
	public ClearedTrade getClearedTrade() {
		return clearedTrade;
	}
	public void setClearedTrade(ClearedTrade clearedTrade) {
		this.clearedTrade = clearedTrade;
		// cleared trade is optional. If set, it will be the last received message for this snapshot so it can be closed:
		closed = true;
	}
	public Orderbook getOrderbook() {
		return orderbook;
	}
	public void setOrderbook(Orderbook orderbook) {
		this.orderbook = orderbook;
		
		//should we close the snapshot?
		if(orderbook.getClearingPrice()==null){
			closed=true;
		}
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		
		StringBuilder builder = new StringBuilder();
		
		if(orders!=null){
			builder.append("ORDERS:\nASKS:");
			SortedSet<OrderbookOrder> asks = orders.getAsks();
			for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
				builder.append(orderbookOrder).append("\n");
			}			
			SortedSet<OrderbookOrder> bids = orders.getBids();
			for (Iterator iterator = bids.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
				builder.append(orderbookOrder).append("\n");
			}
		}
		
		if(orderbook!=null){
			builder.append("\nORDERBOOK:\nASKS:");
			SortedSet<OrderbookOrder> asks = orderbook.getAsks();
			for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
				builder.append(orderbookOrder).append("\n");
			}			
			SortedSet<OrderbookOrder> bids = orderbook.getBids();
			for (Iterator iterator = bids.iterator(); iterator.hasNext();) {
				OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
				builder.append(orderbookOrder).append("\n");
			}
		}
		
		return ""+builder+"\n\n"+clearedTrade;
	}
	
	public boolean isClosed() {
		return closed;
	}
		
}
