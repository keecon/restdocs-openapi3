# Dependabot 7일 Cooldown 작업 계획

> **실행 지침:** `superpowers:executing-plans`를 사용해 아래 체크리스트를 순서대로 수행한다. 사용자의 실행 요청에 따라 로컬 구현과 검증을 완료했다. 원격 반영과 운영 확인은 별도 요청 후 수행한다.

**목표:** 일반 Gradle·GitHub Actions 버전 업데이트에 출시 후 7일 대기를 적용하고, Dependabot 보안 업데이트에는 대기를 추가하지 않는다.

**설계:** 기존 `.github/dependabot.yml`의 두 생태계 항목에 `cooldown.default-days: 7`만 추가한다. 보안 업데이트 제외는 Dependabot의 기본 동작을 사용하며 별도 그룹, 브랜치, 심각도 분류 로직을 만들지 않는다.

**기술:** Dependabot v2 YAML, 기존 Ruby 표준 라이브러리 YAML 파서.

**요구사항 근거:** 현재 대화의 적용 가능성 검토 및 사용자의 수정 작업 계획 작성 요청. 단일 설정 파일의 제한된 변경이므로 별도 SPEC은 만들지 않는다.

## 범위와 제약

- 구현 수정 파일: `.github/dependabot.yml` 하나.
- 계획 파일: `docs/superpowers/plans/2026-09-21-dependabot-cooldown.md`.
- 매주 화요일 10:00 `Asia/Seoul`, PR 제한 5개, 기존 레이블·디렉터리는 유지한다.
- 새 의존성, 테스트 파일, CI 작업, 자동 병합은 추가하지 않는다.
- 커밋·푸시·PR 생성, 저장소 보안 설정 변경, dependency submission 실행은 이 계획의 실행 범위에서 제외한다. 필요하면 별도 사용자 요청을 받는다.
- Gradle 의존성 제출 자동화 구축은 별도 작업이다. 미확인 상태를 보안 대응 보장으로 표현하지 않는다.

## 검토 초점

1. 두 생태계 모두 정수 `7`이 설정되고 YAML 계층이 올바른가.
2. 기존 실행 일정과 PR 제한·레이블에 변화가 없는가.
3. 긴급도와 무관하게 모든 Dependabot security update가 cooldown에서 제외됨을 구분했는가.
4. 보안 수정이 일반 version update로 처리되는 경우의 대기와 수동 대응을 명시했는가.
5. Gradle의 의존성 제출·전이 의존성 제한 및 원격 설정 미확인 상태를 남겼는가.

## 작업 1: 설정 변경과 로컬 검증

**입력:** 기존 두 생태계 설정. **산출물:** 두 곳에 cooldown만 추가한 diff.

- [x] **1. 변경 전 상태 확인**

```bash
git status --short
cat .github/dependabot.yml
```

사용자 변경이 있으면 보존한다. 계획 작성 당시에는 `gradle`, `github-actions` 항목이 각 하나이고 cooldown 설정은 없다.

- [x] **2. 각 생태계의 `directory` 바로 아래에 다음 두 줄 추가**

```yaml
    cooldown:
      default-days: 7
```

`schedule`과 같은 들여쓰기 수준에 배치한다. `include`, `exclude`, `semver-*-days`, `target-branch`, 보안 그룹은 추가하지 않는다.

- [x] **3. YAML 구조 및 기존 설정 보존 검증**

기존 Ruby와 표준 라이브러리를 사용한다. 아래 검사는 계획 작성 시점의 기존 설정을 기준으로 하므로, 실행 전 파일이 달라졌다면 차이의 원인을 확인한다. 검사를 맞추기 위해 사용자 설정을 되돌리지 않는다.

```bash
ruby -ryaml -e '
c = YAML.load_file(".github/dependabot.yml")
abort "unexpected root keys" unless c.keys.sort == %w[updates version]
abort "wrong config version" unless c.fetch("version") == 2
updates = c.fetch("updates")
abort "unexpected ecosystems" unless updates.map { |u| u.fetch("package-ecosystem") }.sort == %w[github-actions gradle]
updates.each do |u|
  ecosystem = u.fetch("package-ecosystem")
  expected = {
    "package-ecosystem" => ecosystem,
    "directory" => "/",
    "cooldown" => {"default-days" => 7},
    "schedule" => {"interval" => "weekly", "day" => "tuesday", "time" => "10:00", "timezone" => "Asia/Seoul"},
    "open-pull-requests-limit" => 5,
    "labels" => ["dependencies", ecosystem == "gradle" ? "gradle" : "actions"]
  }
  abort "unexpected configuration: #{ecosystem}" unless u == expected
end
puts "PASS: both cooldowns are 7 days; existing settings preserved"
'
git diff --check
git diff -- .github/dependabot.yml
```

예상 결과: Ruby 검사와 `git diff --check`가 종료 코드 0, 설정 diff는 총 4줄 추가. 애플리케이션 코드 변경이 없으므로 Gradle 빌드·전체 테스트는 실행하지 않는다. 로컬 YAML 검사는 GitHub 서비스의 설정 수용이나 실제 PR 생성을 증명하지 않는다.

## 작업 2: 보안 경로와 운영 검증 조건 보고

**입력:** 공식 문서와 로컬 확인 증거. **산출물:** 일반/보안 업데이트 구분 및 미확인 항목을 포함한 완료 보고.

- [x] **1. 아래 정책을 공식 문서와 대조해 보고**

| 유형 | 기대 동작 |
|---|---|
| 일반 버전 업데이트 | 출시 후 7일이 지난 버전을 다음 정기 실행에서 검토 |
| Dependabot 보안 업데이트 | 심각도와 관계없이 cooldown 미적용; 보안 기능 활성화 및 수정 가능 조건 필요 |
| 보안 수정이 포함됐지만 일반 업데이트로 처리되는 건 | 7일 대기 대상; 긴급 대응은 수동 버전 수정·검증으로 처리 |

현재 주간 일정에서는 일반 업데이트 후보가 출시 후 약 7~14일에 검토된다. PR 제한·업데이트 오류 등에 따라 PR 생성은 더 늦어질 수 있다. 대기 기간은 PR 생성 후 병합 대기 시간이 아니다.

- [x] **2. 보안 자동화의 확인 범위 기록**

```bash
rg -n 'dependency-submission|dependency-graph|security-updates' .github
```

검색 결과가 없을 때 `rg` 종료 코드 1은 일치 항목 없음이다. 계획 작성 전 조사에서는 로컬 제출 설정을 찾지 못했지만, 이것만으로 GitHub의 자동 제출이나 원격 설정이 비활성화됐다고 결론 내리지 않는다.

보고에 다음을 포함한다.

- dependency graph, Dependabot alerts, security updates의 원격 활성화 여부는 미확인이다.
- Gradle 보안 업데이트에는 dependency submission API로 제출된 의존성 정보가 필요하다. 전이 의존성은 알림이 있어도 보안 PR이 생성되지 않을 수 있다.
- GitHub Actions는 보안 업데이트를 지원하지만 보안 기능 활성화 여부와 해당 취약점의 탐지·수정 가능 여부가 전제다.
- 미확인 항목은 설정 파일 변경의 로컬 완료를 막지 않지만, 긴급 보안 자동 대응이 검증됐다는 주장은 금지한다.

- [x] **3. 반영 후 운영 확인 절차를 인계**

사용자가 별도로 원격 반영을 요청한 뒤, GitHub에서 다음을 확인한다. 이번 로컬 구현 단계에서 PR 생성이나 보안 테스트용 취약 의존성 도입을 수행하지 않는다.

1. 저장소 보안 설정에서 dependency graph·alerts·security updates 활성화 상태 확인.
2. Gradle 의존성 그래프 제출 성공 및 최신 데이터 존재 확인.
3. 다음 정기 실행 로그에서 설정 오류가 없고 출시 7일 미만 버전이 보류되는지 확인.
4. 실제 수정 가능한 보안 알림이 있을 때 연결된 보안 PR과 처리 시점을 확인. 대상 알림이 없으면 보안 PR 실동작 검증은 미실시로 기록.

## 성공 기준

- 두 생태계에 `cooldown.default-days: 7`이 있고 기존 설정을 보존한다.
- 로컬 YAML 검사와 diff 검사가 통과한다.
- 보안 업데이트 제외와 일반 업데이트로 처리되는 보안 수정의 차이를 보고한다.
- 로컬 검증 결과와 향후 GitHub 운영 검증을 구분한다.

## 근거 문서

2026-09-21 대화에서 확인한 GitHub 공식 문서:

- [cooldown 옵션 및 지원 생태계](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#cooldown)
- [버전 업데이트 cooldown 동작](https://docs.github.com/en/code-security/tutorials/secure-your-dependencies/optimizing-pr-creation-version-updates)
- [보안 업데이트 조건과 동작](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-security-updates)
- [Gradle 보안 업데이트 지원 제한](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#gradle)

## 실행 결과 (2026-09-21)

- `.github/dependabot.yml`에 계획한 4줄만 추가했다.
- 계획의 Ruby YAML 구조·기존 설정 보존 검사: PASS.
- `git diff --check`: 통과.
- `.github`의 의존성 제출·보안 설정 키 검색: 일치 없음(`rg` 종료 코드 1). 원격 설정 비활성화를 의미하지 않는다.
- 보안 정책은 앞선 공식 문서 검토 결과와 일치한다. 보안 업데이트는 cooldown 제외, 일반 업데이트로 처리되는 보안 수정은 대기 대상이다.
- 위 운영 확인 절차를 인계했으며 실제 원격 설정 확인·PR 실동작 검증은 수행하지 않았다. Gradle 제출 상태와 전이 의존성 보안 대응은 미검증이다.
- 커밋·푸시·PR 생성과 원격 설정 변경은 수행하지 않았다.
