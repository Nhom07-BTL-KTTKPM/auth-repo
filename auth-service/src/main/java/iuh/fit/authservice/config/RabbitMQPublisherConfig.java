package iuh.fit.authservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQPublisherConfig {

    @Bean
    public MessageConverter authJacksonMessageConverter() {
        var converter = new Jackson2JsonMessageConverter(new ObjectMapper().findAndRegisterModules());
        var mapper = new DefaultJackson2JavaTypeMapper();
        mapper.setTrustedPackages("iuh.fit.*");
        converter.setJavaTypeMapper(mapper);
        return converter;
    }

    @Bean
    public RabbitTemplate authRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(authJacksonMessageConverter());
        return template;
    }
}
