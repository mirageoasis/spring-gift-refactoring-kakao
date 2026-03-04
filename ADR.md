# Architecture Decision Records

## ADR-001: Admin 컨트롤러에 서비스 계층 도입 방식

### 상태
채택됨

### 배경
`AdminMemberController`와 `AdminProductController`가 Repository를 직접 호출하고 있었다.
서비스 계층을 거치지 않아 트랜잭션 관리가 누락되고, 비즈니스 로직이 컨트롤러에 분산되는 문제가 있었다.

### 결정
별도 AdminService를 만들지 않고, 기존 `ProductService` / `MemberService` / `CategoryService`에 메서드를 추가한다.

### 근거
- Admin 작업은 별도 도메인이 아니라 같은 도메인의 CRUD이다. 서비스를 분리할 만큼 관심사가 다르지 않다.
- `AdminProductService`를 만들면 같은 Repository를 주입받는 서비스가 2개가 되고, 검증 로직이 중복되거나 서비스 간 호출이 필요하다.
- `AdminService`로 통합하면 Product·Member·Category를 한 클래스에 몰아넣게 되어 SRP를 위반하고, 패키지-바이-피처 구조와 맞지 않는다.

### 고려했던 대안

| 대안 | 기각 이유 |
|------|-----------|
| `AdminProductService` / `AdminMemberService` | 같은 Repository 중복 주입, 검증 로직 중복 |
| 통합 `AdminService` | SRP 위반, 패키지-바이-피처 구조와 불일치 |

---

## ADR-002: 서비스 계층 반환 타입 — 엔티티 vs DTO

### 상태
방향 확정, 점진적 전환 중

### 배경
기존 서비스 계층은 DTO를 반환하는 메서드가 대다수(13개)였고, 엔티티를 반환하는 메서드는 소수(4개)였다.
Admin 컨트롤러 리팩토링 과정에서 서비스가 DTO를 반환하면 같은 비즈니스 로직임에도 Admin용 메서드를 중복 생성해야 하는 문제가 드러났다.

### 결정
- 서비스는 엔티티를 반환하는 것이 바람직하다.
- 단, 전체 변경은 영향 범위가 크므로 우선 Admin 리팩토링에 필요한 엔티티 반환 메서드를 추가한다.
- 기존 DTO 반환 메서드는 그대로 유지하고, 추후 별도 작업으로 통일한다.

### 근거
- DTO 변환은 프레젠테이션 관심사이지 서비스의 역할이 아니다.
- 서비스가 DTO를 반환하면 두 번째 소비자(Admin 컨트롤러)가 생겼을 때 같은 로직인데 메서드를 중복 생성해야 한다.
- 엔티티를 반환하면 컨트롤러마다 `ProductResponse.from(entity)` 등 기존 팩토리 메서드로 자유롭게 변환할 수 있다.
- OSIV가 꺼져 있어도 서비스에 `@Transactional`이 있으므로 필요한 연관관계를 fetch join으로 로딩하면 된다.

### 기존 코드가 DTO를 반환했던 이유
REST API 컨트롤러만 있던 시점에서 서비스와 컨트롤러가 1:1이었다.
단일 소비자 상황에서는 서비스에서 DTO를 바로 반환하는 것이 편리하고 단점이 보이지 않았다.
Admin 컨트롤러(두 번째 소비자)가 생기면서 재사용 불가 문제가 드러났다.
