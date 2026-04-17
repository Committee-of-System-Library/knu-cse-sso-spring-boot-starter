package kr.ac.knu.cse.sso.autoconfigure;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnClass(HttpSecurity.class)
@ConditionalOnProperty(prefix = "knu-cse.sso", name = "client-id")
@EnableConfigurationProperties(KnuCseSsoProperties.class)
public class KnuCseSsoAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(KnuCseSsoProperties properties) {
        Assert.hasText(
                properties.getClientSecret(),
                "knu-cse.sso.client-secret must be set — auth-server issues HMAC-SHA256 signed JWTs"
        );
        SecretKeySpec key = new SecretKeySpec(
                properties.getClientSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public KnuCseJwtAuthenticationConverter knuCseJwtAuthenticationConverter() {
        return new KnuCseJwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public KnuCseRoleInterceptor knuCseRoleInterceptor() {
        return new KnuCseRoleInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_EXECUTIVE
                ROLE_ADMIN > ROLE_FINANCE
                ROLE_ADMIN > ROLE_PLANNING
                ROLE_ADMIN > ROLE_PR
                ROLE_ADMIN > ROLE_CULTURE
                ROLE_EXECUTIVE > ROLE_STUDENT
                ROLE_FINANCE > ROLE_STUDENT
                ROLE_PLANNING > ROLE_STUDENT
                ROLE_PR > ROLE_STUDENT
                ROLE_CULTURE > ROLE_STUDENT
                """);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(knuCseRoleInterceptor());
    }
}
