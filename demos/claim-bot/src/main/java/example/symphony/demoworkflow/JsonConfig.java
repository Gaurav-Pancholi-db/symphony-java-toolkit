package example.symphony.demoworkflow;

import org.finos.symphony.toolkit.workflow.sources.symphony.handlers.EntityJsonConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JsonConfig implements InitializingBean {

	@Autowired
	EntityJsonConverter ejc;
		
	@Override
	public void afterPropertiesSet() throws Exception {
		ejc.getObjectMapper().registerModule(new JavaTimeModule());
		ejc.getObjectMapper()
    	.enable(SerializationFeature.INDENT_OUTPUT)
    	.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}
}
