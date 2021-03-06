package org.finos.symphony.toolkit.workflow.sources.symphony.handlers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.finos.symphony.toolkit.workflow.content.HashTag;
import org.finos.symphony.toolkit.workflow.content.HashTagDef;
import org.finos.symphony.toolkit.workflow.form.HeaderDetails;
import org.finos.symphony.toolkit.workflow.response.AttachmentResponse;
import org.finos.symphony.toolkit.workflow.response.DataResponse;
import org.finos.symphony.toolkit.workflow.response.ErrorResponse;
import org.finos.symphony.toolkit.workflow.response.FormResponse;
import org.finos.symphony.toolkit.workflow.response.MessageResponse;
import org.finos.symphony.toolkit.workflow.response.Response;
import org.finos.symphony.toolkit.workflow.sources.symphony.TagSupport;
import org.finos.symphony.toolkit.workflow.sources.symphony.room.SymphonyRooms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import com.symphony.api.agent.MessagesApi;

/**
 * This is responsible for taking {@link Response} objects and pushing them back to Symphony.
 * 
 * @author Rob Moffat
 *
 */
public class SymphonyResponseHandler implements ResponseHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(SymphonyResponseHandler.class);

	MessagesApi messagesApi;
	FormMessageMLConverter formConverter;
	EntityJsonConverter jsonConverter;
	SymphonyRooms ru;
	AttachmentHandler ah;
	
	@Value("${symphony.chat-workflow.outputTemplates:true}")
	private boolean outputTemplates;
	
	@Value("${symphony.chat-workflow.header.template:classpath:/templates/response-header.ftl}")
	private Resource responseHeader;
	
	public SymphonyResponseHandler(MessagesApi api, 
			FormMessageMLConverter fmc, 
			EntityJsonConverter ejc, 
			SymphonyRooms ru, 
			AttachmentHandler ah) {
		this.messagesApi = api;
		this.formConverter = fmc;
		this.jsonConverter = ejc;
		this.ru = ru;
		this.ah = ah;
	}
	
	@Override
	public void accept(Response t) {
		if (t instanceof FormResponse) {
			processFormResponse((FormResponse) t);
		} else if (t instanceof ErrorResponse) {
			processErrorResponse((ErrorResponse) t);
		} else if (t instanceof MessageResponse) {
			processMessageResponse((MessageResponse) t);
		} else if (t instanceof AttachmentResponse) {
			processAttachmentResponse((AttachmentResponse) t);
		} else if (t != null) {
			throw new UnsupportedOperationException("Couldn't process response of type "+t.getClass());
		}
	}

	private void processAttachmentResponse(AttachmentResponse t) {
		processDataResponse("", t, ah.formatAttachment(t));
	}

	private void processErrorResponse(MessageResponse t) {
		processDataResponse("!"+t.getMessage(), t, null);
	}

	private void processMessageResponse(MessageResponse t) {
		processDataResponse(t.getMessage(), t, null);
	}

	private void processFormResponse(FormResponse t) {
		String convertedForm = formConverter.convert(t.getFormClass(), t.getFormObject(), t.getButtons(), t.isEditable(), t.getErrors(), t.getData());
		processDataResponse(convertedForm, t, null);
	}


	private void processDataResponse(String messageBody, DataResponse t, Object attachment) {
		String header = createWorkflowHeader(t);
		String outMessage = "<messageML>"+header+messageBody+"</messageML>";
		String json = jsonConverter.writeValue(t.getData());
		String streamId = ru.getStreamFor(t.getAddress());
		if (isOutputTemplates()) {
			LOG.info("JSON: \n"+ json);
			LOG.info("TEMPLATE: \n"+ outMessage);
		}
		try {
			messagesApi.v4StreamSidMessageCreatePost(null, streamId, outMessage, json, null, attachment, null, null);
		} catch (Exception e) {
			LOG.error("Failed to send message: \nTEMPLATE:\n"+outMessage+"\nJSON:\n"+json+"\n", e);
		}
	}
	
	protected String createWorkflowHeader(DataResponse dr)  {
		try {
			HeaderDetails hd = new HeaderDetails(dr.getName(), dr.getInstructions(), getDataTags(dr));
			dr.getData().putIfAbsent("header", hd);
			if (responseHeader != null) {
				return StreamUtils.copyToString(responseHeader.getInputStream(), Charset.forName("UTF-8"));
			} else {
				return "";
			}
		} catch (IOException e) {
			throw new RuntimeException("Couldn't download / parse header template at "+responseHeader.getDescription(), e);
		}
	}
	
	public Set<HashTag> getDataTags(DataResponse d) {
		if (d.getData() == null) {
			return Collections.emptySet();
		}
		Set<HashTag> tags = d.getData().values().stream()
			.filter(v -> v != null)
			.flatMap(v -> TagSupport.classHashTags(v).stream())
			.collect(Collectors.toSet());
		
		tags.add(new HashTagDef(TagSupport.formatTag(d.getWorkflow().getNamespace())));
		tags.add(new HashTagDef("symphony-workflow"));
		
		return tags;
	}
	

	public boolean isOutputTemplates() {
		return outputTemplates;
	}

	public void setOutputTemplates(boolean outputTemplates) {
		this.outputTemplates = outputTemplates;
	}

}
