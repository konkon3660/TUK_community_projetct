# GUI 작성 가이드 (Swing)

`client/GUI/`는 Swing으로 만든다 (빌드 시스템이 없어서 외부 라이브러리 설치 부담이 없는 게
중요했음). **화면 14개 전부 클래스와 메서드 시그니처까지 이미 만들어져 있다** (컴파일 확인
완료) — AI에게 새 화면 클래스를 설계하게 하지 말고, 아래 §3 표에 나온 `TODO: 구현 필요`
메서드 본문만 채우도록 지시할 것. **화면 레이아웃(`initLayout()`) 자체는 자유지만, "화면이
서버와 대화하는 방식"과 "화면 전환 방식"은 아래 패턴을 그대로 따른다.**

---

## 1. `MainFrame`

`CardLayout`으로 화면(JPanel)들을 이름으로 등록해두고 전환하는 셸. `ServerConnection`과 로그인한
`User`(세션)를 하나씩만 만들어서 들고 있고 모든 화면이 공유한다.

- `mainFrame.getConnection()` — 서버로 요청을 보낼 때 사용 (`ServerConnection.sendRequest(Packet)`)
- `mainFrame.getCurrentUser()` / `mainFrame.setCurrentUser(User)` — 로그인한 유저 공유(로그인
  성공 시 `LoginPanel`이 설정, 이후 다른 화면들이 조회만 함)
- `mainFrame.registerScreen(name, panel)` — 화면 등록 (`main()`에 이미 14개 전부 등록되어 있음)
- `mainFrame.switchTo(name)` — 화면 전환
- `mainFrame.getScreen(name)` — 등록된 화면 인스턴스를 이름으로 꺼냄 (아래 §2 `open(...)` 패턴에서 사용)

## 2. 로그인 분기 — 학생 vs 관리자 완전 분리

관리자는 학생용 화면에 접근하면 안 된다(기획 확정). 그래서 `LoginPanel.attemptLogin()`이
성공하면 `user.isAdmin()`에 따라 화면을 완전히 다른 곳으로 보낸다:

```java
mainFrame.switchTo(user.isAdmin() ? "admin" : "home");
```

- 학생: `home`(`HomePanel`) → 게시판/채팅/추천/민원 제출 등 학생 기능만.
- 관리자: `admin`(`AdminPanel`) → 공지 작성/게시글 관리/민원함/회원정보 수정만. `HomePanel`로
  돌아가는 경로가 없고, 로그아웃만 있다.
- `PostListPanel`처럼 학생/관리자가 같이 쓰는 화면은 `open(boardKey, backTarget)`의
  `backTarget`으로 뒤로가기 대상을 구분한다 — 학생이 열 때는 `"home"`, 관리자가 게시글
  관리/민원함 목적으로 열 때는 `"admin"`(아래 §3 표 참고).

## 3. 화면 전환 시 데이터 넘기기 — `open(...)` 컨벤션

`switchTo(name)`은 화면을 보여주기만 하고 데이터는 못 넘긴다. "어떤 게시글을/어떤 채팅방을"
보여줄지 정해야 하는 화면(`PostDetailPanel`, `ChatRoomPanel` 등)은 `public void open(...)`
메서드를 갖고 있다. 호출하는 쪽은 항상 이 순서를 지킨다:

```java
((PostDetailPanel) mainFrame.getScreen("postDetail")).open(boardKey, post);
mainFrame.switchTo("postDetail");
```

## 4. 화면별 구현 상태

각 클래스는 `LoginPanel`과 같은 형태다: `JPanel` 상속, 생성자는 `MainFrame`을 받아 필드로 저장,
버튼 등 액션 리스너는 생성자에서 연결, 실제 배치는 `initLayout()`(항상 TODO — 자유 영역).
서버에 보내는 요청 자체(어떤 `RequestType`/DTO를 쓰는지)는 이미 구현되어 있는 경우가 많고,
"응답을 화면에 어떻게 반영할지"(목록 렌더링, 상세 채우기 등)만 TODO로 남아있다.

| 화면 이름(`switchTo` 키) | 클래스 | 남은 TODO |
|---|---|---|
| 화면 이름(`switchTo` 키) | 클래스 | 접근 | 남은 TODO |
|---|---|---|---|
| `login` | `LoginPanel` | 공용 | `initLayout`, 로그인 성공 후 관리자/학생 분기(§2) |
| `register` | `RegisterPanel` | 학생 | `initLayout`, 가입 성공 후 화면전환 |
| `home` | `HomePanel` | 학생 | `initLayout`, 각 버튼이 여는 화면 |
| `postList` | `PostListPanel` | 공용(`backTarget`으로 구분) | `initLayout`, 목록 렌더링, `openEditor()`(관리자가 열었으면 글쓰기 버튼 숨김) |
| `postDetail` | `PostDetailPanel` | 공용 | `initLayout`, 렌더링, 댓글 삭제 버튼 연결 |
| `postEditor` | `PostEditorPanel` | 학생 | `initLayout`, `generateId()`(id 채번 규칙 미정) |
| `groupBuyPostEditor` | `GroupBuyPostEditorPanel` | 학생 | `initLayout`, `generateId()`, 채팅방 자동 생성 연동 |
| `noticePostEditor` | `NoticePostEditorPanel` | 관리자 | `initLayout`, `generateId()` |
| `complaint` | `ComplaintPanel` | 학생 | `initLayout`, `generateId()`, `openMyComplaints()` |
| `chatRoomList` | `ChatRoomListPanel` | 학생 | `initLayout`, `renderRooms()` |
| `chatRoomCreate` | `ChatRoomCreatePanel` | 학생 | `initLayout`, 생성 성공 후 화면전환 |
| `chatRoom` | `ChatRoomPanel` | 학생 | `initLayout`, `renderMessages()`, `onPush()` 반영, 승인/거절 버튼 연결 |
| `userEdit` | `UserEditPanel` | 관리자 | `initLayout`, `search()`(학번으로 유저 1명 조회하는 RequestType이 아직 없음) |
| `admin` | `AdminPanel` | 관리자(로그인 시 진입점) | `initLayout`, `openPostManagement()`, `logout()` |
| `recommend` | `RecommendPanel` | 학생 | `initLayout`(탭 구성) |

서버 요청을 만드는 부분의 실제 예시는 `LoginPanel.attemptLogin()`, `PostListPanel.open()`,
`ChatRoomListPanel.refresh()`를 참고할 것. 서버 푸시(새 채팅 등)를 받는 화면(`ChatRoomPanel`)은
`PushListener`를 직접 구현하고 `open()`에서 `mainFrame.getConnection().setPushListener(this)`로
등록한다 — 콜백은 네트워크 스레드에서 호출되므로 화면 갱신은 `SwingUtilities.invokeLater`로
감싸야 한다(이미 코드 주석으로 남겨둠).

## 5. 추천 기능 연동

추천 3종(`client/recomment_system/activity`, `menu`, `book`)은 서버 통신이 필요 없다 — 로컬
`client/recommend_data/*` 파일을 그대로 읽는다. 화면에서는 각 `*Recommender` 클래스를 그대로
호출하면 된다 (예: `new MenuRecommender().recommendTwoRandom()`).
