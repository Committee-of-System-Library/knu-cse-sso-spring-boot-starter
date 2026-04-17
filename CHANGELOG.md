# Changelog

## 1.2.0 (2026-04-17)

### Fixes

- `JwtDecoder` 에 `macAlgorithm(MacAlgorithm.HS256)` 명시. 기존엔 알고리즘 명시 없이 `NimbusJwtDecoder.withSecretKey(...)` 만 사용해서, auth-server 가 시크릿 길이에 따라 HS384 로 발급(`Keys.hmacShaKeyFor` 가 ≥48바이트 키에서 HS384 자동 선택)한 토큰을 받으면 `"Another algorithm expected, or no matching key(s) found"` 로 401 거부되던 케이스가 있었음. 양쪽에서 HS256 을 강제하도록 starter 도 명시 고정.

### Migration from 1.1.0

깨지는 변경 없음. consumer `application.yml` / `SecurityConfig` 그대로. `build.gradle(.kts)` 의존 버전만 1.2.0 으로 올리면 됨.

```gradle
implementation 'kr.ac.knu.cse:knu-cse-sso-spring-boot-starter:1.2.0'
```

auth-server 가 기존에 HS384 토큰을 발급 중이었다면 starter 1.2.0 이전에는 우연히(혹은 다른 이유로) 통과되던 흐름이 명시 거부될 수 있음. 정상 동작하는 환경이면 영향 없음.

## 1.1.0 (2026-04-17)

### Breaking changes

- `KnuCseSsoProperties` 단순화: `issuerUri`, `jwksUri` 필드 제거. `clientId`, `clientSecret` 만 남김. auth-server 가 HMAC-SHA256 대칭키로 서명하므로 JWKS 기반 검증은 이 starter 의 관심사가 아님. 자체 JWKS 검증을 원하면 `JwtDecoder` 빈을 직접 등록하면 override 됨.
- `KnuCseSsoAutoConfiguration` 의 `JwtDecoder` 빈이 **대칭키 전용**(`NimbusJwtDecoder.withSecretKey`). `client-secret` 미설정 시 기동 실패.
- `KnuCseJwtAuthenticationConverter` 엄격화:
  - `sub` 또는 `user_type` claim 누락·공란 → `OAuth2AuthenticationException(invalid_token)` (종전에는 묵시적 `EXTERNAL` 폴백)
  - `role` claim 값이 `Role` enum 에 없으면 예외 (종전에는 `null` 로 다운그레이드)
  - `role` claim 누락 시에만 `null` 허용 (EXTERNAL 사용자 로그인 지원)

### Fixes

- `UserType` enum 에 `KNU_OTHER_DEPT` 추가. 1.0.0 은 `CSE_STUDENT, EXTERNAL` 만 있었는데 auth-server 는 세 값을 발급 중이라 타 학부 사용자가 `EXTERNAL` 로 잘못 분류되던 버그.

### Infra

- `.github/workflows/publish.yml` 추가: `main` push 시 GitHub Packages 로 자동 publish.

### Migration from 1.0.0

Consumer `application.yml`:

```yaml
# 제거
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ...

# 추가
knu-cse:
  sso:
    client-id: ${KNU_CSE_SSO_CLIENT_ID}
    client-secret: ${KNU_CSE_SSO_CLIENT_SECRET}
```

Consumer `build.gradle(.kts)`:

```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/Committee-of-System-Library/knu-cse-sso-spring-boot-starter")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'kr.ac.knu.cse:knu-cse-sso-spring-boot-starter:1.1.0'
}
```

Consumer `SecurityConfig`: starter autoconfig 에 위임. `KnuCseJwtAuthenticationConverter`, `KnuCseRoleInterceptor` 를 직접 Bean 등록하지 말 것. `WebMvcConfigurer.addInterceptors` 에서도 등록 금지 (이중 등록 위험).

## 1.0.0

초기 릴리즈.
