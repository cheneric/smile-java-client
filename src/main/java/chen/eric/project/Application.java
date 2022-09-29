package chen.eric.project;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.MethodParameter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Executor;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import smile.identity.core.json.SecurityKeyTimestamp;

@SpringBootApplication
@EnableAsync
public class Application implements CommandLineRunner {
	private Log log;
	private ObjectMapper objectMapper;

	@Autowired
	protected void setLog(Log log) {
		this.log = log;
	}

	@Autowired
	protected void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Bean
	@Scope("prototype")
	// https://medium.com/simars/inject-loggers-in-spring-java-or-kotlin-87162d02d068
	public Log provideLog(final InjectionPoint injectionPoint) {
		return LogFactory.getLog(
			of(injectionPoint.getMethodParameter())
				.<Class>map(MethodParameter::getContainingClass)
				.orElseGet(() ->
					ofNullable(injectionPoint.getField())
						.map(Field::getDeclaringClass)
						.orElseThrow(IllegalArgumentException::new)
				)
		);
	}

	@Bean
	public Executor taskExecutor(
		@Value("${project.taskExecutor.corePoolSize}") int taskExecutorCorePoolSize,
		@Value("${project.taskExecutor.maxPoolSize}") int taskExecutorMaxPoolSize,
		@Value("${project.taskExecutor.queueCapacity}") int taskExecutorQueueCapacity,
		@Value("${project.taskExecutor.threadNamePrefix}") String threadNamePrefix)
	{
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(taskExecutorCorePoolSize);
		executor.setMaxPoolSize(taskExecutorMaxPoolSize);
		executor.setQueueCapacity(taskExecutorQueueCapacity);
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.initialize();
		return executor;
	}

	@Override
	public void run(String... args) throws Exception {
		final SecurityKeyTimestamp securityKeyTimestamp = new SecurityKeyTimestamp();
		securityKeyTimestamp.setSecurityKey("hi there");
		securityKeyTimestamp.setTimestamp(123L);
		log.debug("*** "
			+ objectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(securityKeyTimestamp));
	}

	public static void main(String... args) {
		final ApplicationContext context = SpringApplication.run(Application.class, args);
		final Log log = LogFactory.getLog(Application.class);
		log.debug("Beans provided by Spring Boot:");
		final String[] beanNames = context.getBeanDefinitionNames();
		Arrays.sort(beanNames);
		Arrays.stream(beanNames)
			.forEach(log::trace);
	}
}
