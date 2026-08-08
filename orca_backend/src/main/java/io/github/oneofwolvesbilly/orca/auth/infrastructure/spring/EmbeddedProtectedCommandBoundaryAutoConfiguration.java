package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestMappingHandlerMapping.class)
public class EmbeddedProtectedCommandBoundaryAutoConfiguration {

    @Bean
    EmbeddedProtectedCommandStartupValidator embeddedProtectedCommandStartupValidator(
            RequestMappingHandlerMapping handlerMapping,
            ListableBeanFactory beanFactory
    ) {
        return new EmbeddedProtectedCommandStartupValidator(handlerMapping, beanFactory);
    }
}
