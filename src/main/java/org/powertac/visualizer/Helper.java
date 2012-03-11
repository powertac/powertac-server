package org.powertac.visualizer;

import java.math.BigDecimal;


import org.apache.log4j.Logger;
import org.powertac.common.TariffTransaction;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

public class Helper {
	
	private static Logger log=Logger.getLogger(Helper.class);

	/**
	 * @param tariffTransaction
	 * @return positive number for new customers, negative number for ex customers. Otherwise, zero is returned.
	 */
	public static int getCustomerCount(TariffTransaction tariffTransaction){
		// add new customers
		
		switch (tariffTransaction.getTxType()) {
		case SIGNUP:
			return tariffTransaction.getCustomerCount();			
		case REVOKE:
		case WITHDRAW:		
			return (-1) * tariffTransaction.getCustomerCount();
		case CONSUME:
		case PERIODIC:
		case PRODUCE:
		case PUBLISH:
		default:
			return 0;
		}
	}
	
	public static void updateJSON(JSONArray array, int x, double y) {
		int decimal_points = 2;

		JSONArray point = new JSONArray();
		BigDecimal bd = new BigDecimal(y);
		bd.setScale(decimal_points, BigDecimal.ROUND_HALF_UP);
		try {
			point.put(x).put(bd.doubleValue());
			array.put(x, point);
		} catch (JSONException e) {
			log.warn("Problems with JSON");
		}

	
	}
	
	public static double roundNumberTwoDecimal(double number){
		return new BigDecimal(number).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	

		
	  static public Object dispatch (Object target, 
	                                 String methodName, 
	                                 Object... args)
	  {
	    Logger log = Logger.getLogger(target.getClass().getName());
	    Object result = null;
	    try {
	      Class[] classes = new Class[args.length];
	      for (int index = 0; index < args.length; index++) {
	        //log.debug("arg class: " + args[index].getClass().getName());
	        classes[index] = (args[index].getClass());
	      }
	      // see if we can find the method directly
	      Method method = target.getClass().getMethod(methodName, classes);
	      log.debug("found method " + method);
	      result = method.invoke(target, args);
	    }
	    catch (NoSuchMethodException nsm) {
	      log.debug("Could not find exact match: " + nsm.toString());
	    }
	    catch (InvocationTargetException ite) {
	      Throwable thr = ite.getTargetException();
	      
	      if(thr.getStackTrace().length>3){
	      log.error("Cannot call " + methodName
	                   + ": " + thr + "\n  ..at "
	                   + thr.getStackTrace()[0] + "\n  ..at "
	                   + thr.getStackTrace()[1] + "\n  ..at "
	                   + thr.getStackTrace()[2] + "\n  ..at "
	                   + thr.getStackTrace()[3] + "\n  ..at "
	                   );      
	      } else {
	    	  log.error("Cannot call " + methodName + ", StackTrace size is:"+thr.getStackTrace().length);
	      }
	    }
	    catch (Exception ex) {
	      log.error("Exception calling message processor: " + ex.toString());
	    }
	    return result;
	  }
	}
	
	

