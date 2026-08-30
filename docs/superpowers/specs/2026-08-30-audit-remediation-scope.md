# Audit Remediation Scope

## 배경

`e71dfe594b430485196bee19f722777b3e5a4fe9..4c67b60d7aab69dbc70b9540e6e4f2166767b64e`
범위의 코드, 테스트, 빌드 구성 및 의존성을 점검했다. 전체 JDK 17/21/25 빌드는 통과했지만
OpenAPI 보안 정의, JSON 예제 처리, 출력 경로 검증, HTTP 인증 스킴 처리와 빌드 공급망에서
수정할 항목을 확인했다.

## 반드시 처리할 항목

1. OAuth2 Security Requirement가 존재하지만 OAuth2 flow가 구성되지 않은 경우 명확히 실패시킨다.
2. 모든 유효한 JSON 예제(객체, 배열, 앞쪽 공백, 스칼라, `null`)를 표준 JVM 값으로 처리한다.
3. `outputFileNamePrefix`가 출력 디렉터리 밖의 파일을 쓰거나 삭제하지 못하게 한다.
4. HTTP `Basic` 및 `Bearer` 인증 스킴을 대소문자와 무관하게 인식한다.
5. 공개 Gradle 플러그인 POM에서 사용하지 않는 Kotlin Gradle Plugin runtime 의존성을 제거한다.
6. Gradle dependency verification과 GitHub Actions 최소 권한·불변 SHA 고정을 적용한다.

## 이번 계획에서 제외할 항목

- Gradle dependency lockfile: 현재 모든 직접 버전이 고정되고 Spring Boot BOM을 사용하므로 필수 보안
  조치가 아니다. 엄격한 재현성 정책이 필요할 때 별도 변경으로 검토한다.
- Configuration Cache 기본 활성화: 효과는 확인했지만 기능 수정과 분리해 CI에서 별도로 도입한다.
- Isolated Projects: `axion-release`와 `allprojects` 구성이 호환되지 않고 기능도 incubating 상태다.
- `google()` 제거, Kotlin 플러그인 적용 범위 축소, 예제의 native BOM 전환: 성능 최적화이며 결함 수정과
  분리한다.
- WebTestClient wrapper 추가 리팩터링 및 Spring REST Docs private API reflection 제거: 확인된 회귀가
  없으므로 별도 호환성 작업으로 다룬다.

## 호환성 및 품질 기준

- 새 의존성을 추가하지 않는다.
- 공개 패키지, Gradle 플러그인 ID 및 DSL 프로퍼티 이름을 변경하지 않는다.
- Java 17 바이트코드를 유지하고 JDK 17, 21, 25에서 전체 빌드를 검증한다.
- OAuth2 구성 누락은 조용히 잘못된 문서를 만드는 대신 설명 가능한 예외로 실패한다.
- 출력 경로 검증은 `/`와 `\\`를 모두 거부하고 canonical parent를 다시 확인한다.
- 영어 및 한국어 README의 동작 설명과 코드 예제를 동기화한다.
- 사용자가 별도로 요청하기 전에는 커밋, 푸시 또는 외부 가시 작업을 수행하지 않는다.

## 완료 기준

- 각 결함에 대한 회귀 테스트가 수정 전 실패하고 수정 후 통과한다.
- 공개 플러그인 POM에 `org.jetbrains.kotlin:kotlin-gradle-plugin`이 존재하지 않는다.
- Dependency Verification strict 모드에서 전체 빌드가 성공한다.
- GitHub Actions 참조가 전체 commit SHA로 고정되고 읽기 전용 job은 `contents: read`만 가진다.
- JDK 17/21/25의 생성 OpenAPI 해시가 동일하고 클래스 major version가 61이다.
