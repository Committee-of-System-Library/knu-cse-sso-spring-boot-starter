# KNU CSE SSO Spring Boot Starter

경북대학교 컴퓨터학부 SSO 인증 시스템을 Spring Boot 서비스에 연동하기 위한 스타터 라이브러리입니다.

## 연동 절차

### 1. 앱 등록 및 승인

1. [개발자 포털](https://chcse.knu.ac.kr/developer/apps)에서 애플리케이션을 등록합니다.
2. 관리자 승인 후 다음 정보를 발급받습니다:
   - **Client ID** — SSO 로그인 요청 시 사용 (예: `cse-a1b2c3d4`)
   - **Client Secret** — JWT 토큰 검증 + 클라이언트 인증용 (안전하게 보관)

> 이 정보는 승인 시 1회만 표시됩니다. 분실 시 재발급이 필요합니다.

### 2. 의존성 추가

**build.gradle**

```gradle
repositories {
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

### 3. 설정

**application.yml**

```yaml
knu-cse:
  sso:
    client-id: cse-a1b2c3d4              # 발급받은 Client ID
    client-secret: <발급받은 Client Secret>  # JWT 검증용 HMAC-SHA256 키
```

이 설정만으로 Spring Security의 JWT 인증이 자동 구성됩니다.

> auth-server 가 발급하는 JWT 는 **HMAC-SHA256 대칭키** 서명 (서비스별 Client Secret). 자체 `JwtDecoder` 빈을 등록하면 override 됨.

### 4. Security 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            KnuCseJwtAuthenticationConverter converter
    ) throws Exception {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

---

## SSO 로그인 플로우

### 프론트엔드에서 로그인 시작

사용자를 SSO 서버의 `/login` 엔드포인트로 리다이렉트합니다:

```
https://chcse.knu.ac.kr/appfn/api/login
    ?client_id=cse-a1b2c3d4
    &redirect_uri=https://myapp.example.com/callback
    &state=<랜덤_문자열>
```

| 파라미터 | 설명 |
|---------|------|
| `client_id` | 발급받은 Client ID |
| `redirect_uri` | 앱 등록 시 지정한 Redirect URI 중 하나 |
| `state` | CSRF 방지용 랜덤 문자열 (콜백에서 검증) |

### 인증 완료 후 콜백

Google 인증이 완료되면 SSO 서버가 다음 URL로 리다이렉트합니다:

```
https://myapp.example.com/callback?state=<원본_state>&token=<JWT>
```

프론트엔드에서 해야 할 일:
1. `state` 파라미터가 요청 시 보낸 값과 일치하는지 검증
2. `token`(JWT)을 저장 (localStorage, 쿠키 등)
3. 이후 API 요청 시 `Authorization: Bearer <token>` 헤더에 포함

---

## 사용자 정보 접근

### Controller에서 사용자 정보 가져오기

```java
@RestController
@RequestMapping("/api")
public class MyController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal KnuCseUser user) {
        return Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "studentNumber", user.getStudentNumber(),
            "major", user.getMajor(),
            "userType", user.getUserType(),
            "role", user.getRole()
        );
    }
}
```

### KnuCseUser 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `String` | 사용자 고유 ID (SSO 시스템 내부 student_id) |
| `studentNumber` | `String` | 학번 (예: `2023012780`) |
| `name` | `String` | 이름 |
| `email` | `String` | 이메일 |
| `major` | `String` | 전공 (예: `심화컴퓨팅 전공`) |
| `userType` | `UserType` | `CSE_STUDENT`, `KNU_OTHER_DEPT`, `EXTERNAL` |
| `role` | `Role` | `ADMIN`, `EXECUTIVE`, `FINANCE`, `PLANNING`, `PR`, `CULTURE`, `STUDENT` 또는 `null` |

### 유틸 메서드

```java
user.isCseStudent()     // 컴퓨터학부 학생 여부
user.hasRole(Role.STUDENT)  // 특정 역할 이상인지 확인 (계층적)
```

---

## 역할 기반 접근 제어

### 어노테이션으로 엔드포인트 보호

```java
@RequireRole(Role.ADMIN)        // ADMIN만 접근 가능
@GetMapping("/admin/dashboard")
public String adminOnly() { ... }

@RequireCseStudent              // 컴학 학부생만 접근 가능
@GetMapping("/cse/resources")
public String cseOnly() { ... }
```

클래스 레벨에도 적용 가능:

```java
@RestController
@RequireRole(Role.STUDENT)      // 전체 컨트롤러에 적용
@RequestMapping("/api/student")
public class StudentController { ... }
```

### 역할 계층

```
ADMIN > EXECUTIVE, FINANCE, PLANNING, PR, CULTURE > STUDENT
```

`ADMIN`은 모든 역할의 권한을 포함합니다.

---

## JWT 토큰 구조

```json
{
  "sub": "7",
  "student_number": "2023012780",
  "name": "이상민",
  "email": "lsmin3388@knu.ac.kr",
  "major": "심화컴퓨팅 전공",
  "user_type": "CSE_STUDENT",
  "role": "STUDENT",
  "aud": "cse-a1b2c3d4",
  "iss": "https://chcse.knu.ac.kr/appfn/api",
  "iat": 1711065600,
  "exp": 1711069200
}
```

- 서명 알고리즘: **HMAC-SHA256**
- 토큰 유효 시간: **1시간**
- 서비스별 고유 Client Secret으로 서명됨 (서비스 A의 토큰은 서비스 B에서 검증 불가)

---

## 전체 연동 예시

### 1. Spring Boot 백엔드 (이 스타터 사용)

```yaml
# application.yml
knu-cse:
  sso:
    client-id: cse-a1b2c3d4
    client-secret: <발급받은 Client Secret>
```

### 2. React 프론트엔드

```typescript
// 로그인 시작
function login() {
    const state = crypto.randomUUID();
    sessionStorage.setItem('oauth_state', state);

    const params = new URLSearchParams({
        client_id: 'cse-a1b2c3d4',
        redirect_uri: 'https://myapp.example.com/callback',
        state,
    });

    window.location.href =
        `https://chcse.knu.ac.kr/appfn/api/login?${params}`;
}

// 콜백 처리
function handleCallback() {
    const params = new URLSearchParams(window.location.search);
    const state = params.get('state');
    const token = params.get('token');

    if (state !== sessionStorage.getItem('oauth_state')) {
        throw new Error('State mismatch');
    }

    localStorage.setItem('sso_token', token);

    // 이후 API 요청
    fetch('/api/me', {
        headers: { Authorization: `Bearer ${token}` },
    });
}
```

### 3. 다른 언어/프레임워크

Client Secret을 사용하여 HMAC-SHA256으로 토큰을 직접 검증할 수 있습니다:

**Python (PyJWT)**
```python
import jwt

payload = jwt.decode(
    token,
    "발급받은_Client_Secret",
    algorithms=["HS256"],
    audience="cse-a1b2c3d4"
)
print(payload["name"], payload["email"])
```

**Node.js (jsonwebtoken)**
```javascript
const jwt = require('jsonwebtoken');

const payload = jwt.verify(token, '발급받은_Client_Secret', {
    algorithms: ['HS256'],
    audience: 'cse-a1b2c3d4',
});
console.log(payload.name, payload.email);
```
