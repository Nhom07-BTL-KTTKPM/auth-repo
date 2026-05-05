package iuh.fit.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockitoBean
    private ProxyManager<String> proxyManager;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

}
