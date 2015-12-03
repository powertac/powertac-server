package org.powertac.visualizer.beans.backing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

//import org.primefaces.application.PrimeResource;
import org.primefaces.model.StreamedContent;
import org.primefaces.util.Constants;

public class CustomResourceHandler extends ResourceHandlerWrapper {

	 private final static Logger logger = LogManager.getLogger(CustomResourceHandler.class.getName());
     
	    public static final String DYNAMIC_CONTENT_PARAM = "pfdrid";
	    
	    private ResourceHandler wrapped;

	    public CustomResourceHandler(ResourceHandler wrapped) {
	        this.wrapped = wrapped;
	    }

	    @Override
	    public ResourceHandler getWrapped() {
	        return this.wrapped;
	    }

	  
	    @Override
	    public void handleResourceRequest(FacesContext context) throws IOException {
	        Map<String,String> params = context.getExternalContext().getRequestParameterMap();
	        String library = params.get("ln");
	        String dynamicContentId = params.get(DYNAMIC_CONTENT_PARAM);
	        
	        if(dynamicContentId != null && library != null && library.equals("primefaces")) {
	            Map<String,Object> session = context.getExternalContext().getSessionMap();
	            
	            try {
	                String dynamicContentEL = (String) session.get(dynamicContentId);
	                ELContext eLContext = context.getELContext();
	                ValueExpression ve = context.getApplication().getExpressionFactory().createValueExpression(context.getELContext(), dynamicContentEL, StreamedContent.class);
	                StreamedContent content = (StreamedContent) ve.getValue(eLContext);
	                HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

	                response.setContentType(content.getContentType());

	                byte[] buffer = new byte[2048];

	                int length;
	                InputStream inputStream = content.getStream();
	                while ((length = (inputStream.read(buffer))) >= 0) {
	                    response.getOutputStream().write(buffer, 0, length);
	                }
	                response.setStatus(200);
	                
	              //  response.getOutputStream().flush();
	               OutputStream outputStream = response.getOutputStream();
	                context.responseComplete();

	            } catch(Exception e) {
	                logger.error("Error in streaming dynamic resource.\n"+e.toString());
	            } finally {
	                session.remove(dynamicContentId);
	            }
	        }
	        else {
	           super.handleResourceRequest(context); 
	        }
	    }


}
