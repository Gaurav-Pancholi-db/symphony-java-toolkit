package org.finos.symphony.toolkit.workflow.java.resolvers;

import java.util.Optional;

import org.finos.symphony.toolkit.workflow.Workflow;
import org.finos.symphony.toolkit.workflow.content.Addressable;
import org.finos.symphony.toolkit.workflow.content.Author;
import org.finos.symphony.toolkit.workflow.content.Room;
import org.finos.symphony.toolkit.workflow.content.User;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResolverConfig {

	@Autowired
	Workflow wf;
	
	@Autowired
	BeanFactory context;
	
	/**
	 * Allows you to put any beans you like as parameters of the workflow
	 */
	@Bean
	public WorkflowResolverFactory springBeanResolver() {
		return action -> {
			return new WorkflowResolver() {

				@Override
				public boolean canResolve(Class<?> c) {
					try {
						context.getBean(c);
						return true;
					} catch (NoSuchBeanDefinitionException e) {
						return false;
					}
				}

				@Override
				public Optional<Object> resolve(Class<?> c, Addressable a, boolean isTarget) {
					if (isTarget) {
						return Optional.empty();
					}
					
					try {
						Object bean = context.getBean(c);
						return Optional.of(bean);
					} catch (NoSuchBeanDefinitionException e) {
						return Optional.empty();
					}
				}
			};
		};
	}
	
	/**
	 * Allows parameters to include {@link Room} or {@link Author} .
	 */
	@Bean 
	public WorkflowResolverFactory userRoomOrAddressableResolver() {
		
		return action -> new WorkflowResolver() {

			@Override
			public boolean canResolve(Class<?> c) {
				return (c.isAssignableFrom(Room.class))  || (c.isAssignableFrom(Author.class)) ;
			}

			@Override
			public Optional<Object> resolve(Class<?> cl, Addressable a, boolean isTarget) {
				if (isTarget) {
					return Optional.empty();
				}
				
				// handle room arg
				if ((cl.isAssignableFrom(Room.class)) && (action.getAddressable() instanceof Room)) {
					return Optional.of((Room) action.getAddressable());
				}
				
				// handle user arg
				if (cl.isAssignableFrom(Author.class)) {
					return Optional.of(action.getUser());
				}
				
				return Optional.empty();
			}
			
		};
	}
	
	@Bean
	public WorkflowResolversFactory workflowResolversFactory() {
		return new WorkflowResolversFactory();
	}
	
}
