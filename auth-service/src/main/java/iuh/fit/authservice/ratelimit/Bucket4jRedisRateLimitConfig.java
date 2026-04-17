package iuh.fit.authservice.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.nio.charset.StandardCharsets;

@Configuration
public class Bucket4jRedisRateLimitConfig {

    @Bean
    public BucketConfiguration loginRegisterBucketConfiguration(AuthLoginRegisterRateLimitProperties properties) {
        Refill refill = Refill.intervally(properties.getRefillTokens(), properties.getRefillDuration());
        Bandwidth bandwidth = Bandwidth.classic(properties.getCapacity(), refill);
        return BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
    }

        @Bean
    public ProxyManager<String> loginRegisterRateLimitProxyManager(
            RedisConnectionFactory redisConnectionFactory,
            AuthLoginRegisterRateLimitProperties properties
    ) {
        if (!(redisConnectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory)) {
            throw new IllegalStateException("Bucket4j rate limit requires Lettuce RedisConnectionFactory");
        }

        AbstractRedisClient nativeClient = lettuceConnectionFactory.getNativeClient();
        if (nativeClient instanceof RedisClient redisClient) {
            return Bucket4jLettuce.casBasedBuilder(redisClient)
                    .expirationAfterWrite(ExpirationAfterWriteStrategy
                            .basedOnTimeForRefillingBucketUpToMax(properties.getRefillDuration()))
                    .build()
                .withMapper(key -> key.getBytes(StandardCharsets.UTF_8));
        }

        //Phuc vu ve sau
        if (nativeClient instanceof RedisClusterClient redisClusterClient) {
            return Bucket4jLettuce.casBasedBuilder(redisClusterClient)
                    .expirationAfterWrite(ExpirationAfterWriteStrategy
                            .basedOnTimeForRefillingBucketUpToMax(properties.getRefillDuration()))
                    .build()
                .withMapper(key -> key.getBytes(StandardCharsets.UTF_8));
        }

        throw new IllegalStateException("Unsupported lettuce Redis client type: " + nativeClient.getClass().getName());
    }
}
