# 08. 진행 현황과 남은 작업

**진행 상황은 이 문서에만 적습니다.** 다른 문서에는 사양만 적습니다 — 상태를 여러 곳에
쓰면 반드시 서로 어긋납니다.

> **기준: 2026-07-23, 작업 트리 기준(미커밋 변경 포함)**
> Java 21 / 소스 62개 / **`javac` 에러 0개 확인** / 코드 내 `TODO` **16개**
>
> ⚠️ 이 문서가 오래됐다고 느껴지면 **코드가 정답**입니다:
> ```powershell
> Get-ChildItem -Recurse -Filter *.java | Select-String "TODO: 구현 필요"
> ```

---

## 1. 한눈에 보기

| 영역 | 상태 |
|---|---|
| 데이터 모델 (`model/`, `model/boards/`) | 🟩 **거의 완료** — 남은 것 2개 |
| 통신 프로토콜 (`model/protocol/`) | 🟩 **완료** |
| 서버 (`server/`) | 🟩 **완료** — 핸들러 17개 + 푸시까지 동작 |
| 클라이언트 통신 (`client/CT/`) | 🟩 **완료** |
| GUI (`client/GUI/`) | 🟨 **15개 중 12개 완료** — 남은 것 3개 화면 15곳 |
| 추천 3종 | 🟩 **완료** — 할 거 추천의 공강 판단 규칙까지 확정 (§3-3) |
| **데이터 파일** | 🟥 **할 거/시간표 외 전부 비어 있음 (0바이트)** — 테스트하려면 반드시 채워야 함 |

---

## 2. 완성된 것 (건드릴 필요 없음)

### 모델
- `User`, `Chat`, `ChatRoom`, `Post` 4종, `Comment`, 게시판 6종 — 필드·생성자 전부
- 파일 저장/복원 — 모든 클래스의 `toDataString` / `fromDataString`, `load()` / `save()`
- 권한 판단 — `canEdit`, `canDelete`, `canAccess`, `canWrite`, `isVisibleTo`
- **채팅방 가입 절차 전체** — `canJoin`, `requestJoin`, `approveJoin`, `rejectJoin`, `setNickname`
- **공지 필터링** — `NoticePost.isVisibleTo`, `NoticeBoard.getVisiblePosts`

### 통신
- `Packet` 정적 팩토리 4종, `RequestType` 20종, DTO 9종, `BoardKey`
- 소켓 연결, 봉투 직렬화, 요청-응답 `requestId` 짝맞춤, 10초 타임아웃
- `ObjectOutputStream` 선생성 + `flush` 데드락 회피, `reset()` 캐시 비우기

### 서버
- `ServerMain` — 포트 5000, 접속마다 스레드
- `DataStore` — 게시판 8개 등록, 파일 ↔ 메모리 동기화 전부
- **`ClientHandler` — `handleXxx` 17개 전부 구현 완료**
  (`LOGIN`, `REGISTER`, `LOGOUT`, `USER_UPDATE`, `USER_LOOKUP`, `POST_LIST`, `POST_CREATE`,
  `POST_UPDATE`, `POST_DELETE`, `COMMENT_ADD`, `COMMENT_DELETE`, `CHATROOM_CREATE`,
  `CHATROOM_JOIN_REQUEST`, `CHATROOM_JOIN_APPROVE`, `CHATROOM_JOIN_REJECT`, `CHAT_SEND`,
  `CHATROOM_LIST`)
- 서버측 검증 — 세션 확인, 관리자 확인, 게시판 접근 확인, **게시글 타입 일치 검사**,
  본인 명의 검사, 중복 id 검사
- 동시성 — `DATA_LOCK`으로 CUD 12종 직렬화, 조회는 락 없이, `sendPacket` 동기화
- **`SessionRegistry` — 실시간 푸시 동작**
  - 채팅 전송 시 같은 방의 다른 접속자에게 `CHAT_MESSAGE_PUSH`
  - 공지 등록 시 대상 접속자에게 `NOTICE_PUSH`

### GUI (완성된 화면 12개 + 셸)
`MainFrame`, `LoginPanel`, `RegisterPanel`, `HomePanel`, `PostListPanel`, `NoticePostEditorPanel`,
`ComplaintPanel`, `UserEditPanel`, `AdminPanel`, `RecommendPanel`,
**채팅 3종** `ChatRoomListPanel`, `ChatRoomCreatePanel`, `ChatRoomPanel`

채팅 화면에서 확정한 것(코드 주석에도 적어둠):

- **내가 보낸 메시지는 화면에 직접 넣는다.** 서버가 보낸 사람에게는 `CHAT_MESSAGE_PUSH`를
  보내지 않으므로(`ClientHandler.handleChatSend`) 기다려도 오지 않는다.
- **가입 신청(`CHATROOM_JOIN_REQUEST`)은 `ChatRoomListPanel`에서 보낸다.** 참여자가 아닌 방을
  열면 서버가 `CHAT_SEND`를 거부하므로, 목록에서 "들어가기"를 눌렀을 때 참여자가 아니면
  가입 신청 메세지를 받아 보낸다. 승인/거절은 방장이 채팅방 화면 오른쪽에서 처리한다.
- **`ChatRoomPanel`에 새로고침을 뒀다.** 들고 있는 `ChatRoom`은 `CHATROOM_LIST` 시점의
  사본이라 그 뒤 들어온 가입 신청·채팅이 없다. 방 하나만 받는 요청이 없어서 `CHATROOM_LIST`에서
  같은 `roomId`를 다시 찾는다.

### 추천
- **책 추천기** — 완전 동작
- **메뉴 추천기** — 완전 동작 (오늘의 학식 / 랜덤 2개 / 꿀팁)
- 할 거 추천기의 `loadActivities()`, `loadTimetable()`, `ClassPeriodTable`

---

## 3. 남은 작업 — 코드 `TODO` 16개

전부 `TODO: 구현 필요`로 표시되어 있습니다.

### 3-1. GUI (14개) — 난이도 하~중

| 화면 | 파일 | 남은 것 | 개수 |
|---|---|---|:---:|
| `postDetail` | `PostDetailPanel` | `initLayout`, `renderPost()`, `openEditor()`, 삭제 후 전환, 댓글 추가 후 갱신, 댓글 삭제 후 갱신 | 6 |
| `postEditor` | `PostEditorPanel` | `initLayout`, `open()`의 기존 값 채우기, 저장 후 전환, `generateId()` | 4 |
| `groupBuyPostEditor` | `GroupBuyPostEditorPanel` | `initLayout`, `open()` 채우기, 저장 후 전환, `generateId()` | 4 |

> ⚠️ 남은 3개 화면의 `initLayout`이 아직 `UnsupportedOperationException`을 던집니다.
> `MainFrame.main()`이 화면 15개를 **전부 생성해서** 등록하므로, 하나라도 남아 있으면
> **클라이언트가 아예 뜨지 않습니다.** 채팅 화면을 실제 화면에서 확인하려면 이 3개가 먼저
> 끝나야 합니다(§4-2 데이터 파일도 함께).

> **`generateId()`는 규칙이 이미 정해졌습니다** — `UUID.randomUUID().toString()`.
> `NoticePostEditorPanel` / `ComplaintPanel`에 구현되어 있으니 그대로 복사하세요.
>
> **`initLayout`은 참고할 완성본이 많습니다** — `LoginPanel`, `RegisterPanel`, `AdminPanel`,
> `HomePanel`, `PostListPanel`, `NoticePostEditorPanel`, `UserEditPanel`, `ComplaintPanel`.

### 3-2. 모델 (2개) — 난이도 하

| 위치 | 메서드 | 해야 할 일 |
|---|---|---|
| `model/boards/GroupBuyPost.java` | `getCurrentMemberCount(ChatRoom)` | 연결된 채팅방의 참여자 수를 그대로 반환. `linkedRoom`이 `null`일 때 처리 결정 필요 |
| `model/boards/GroupBuyBoard.java` | `filterByHashtag(String)` | `posts`에서 `GroupBuyPost`로 캐스팅해 `hashtags`에 해당 태그가 있는 것만 |

### 3-3. 추천 — ✅ 완료

`ActivityRecommender.recommendNow(timetable, now)` 구현 완료. 논의 전이던 **공강 판단 규칙은
아래와 같이 확정**했습니다 (바꿀 거면 `ActivityRecommender.freeMinutesAt`만 고치면 됩니다):

- 공강 = **다음 수업 시작 전까지 남은 시간**. "연속된 빈 교시 전체"로 보지 않는 이유는, 중간에
  수업이 끼면 어차피 그 전까지밖에 못 놀기 때문입니다.
- 지금이 수업 중인 교시 안이면 공강이 아니므로 추천하지 않습니다(`null`).
- 오늘 남은 수업이 없으면(하교 후 · 오늘 수업 없음 · 주말) **시간 제한 없음** — 모든 할 거가 후보.
- 조건에 맞는 할 거가 없으면 예외 대신 **`null`** (`BookRecommender.recommendForDepartment`와 같은 컨벤션).

같이 추가된 `freeMinutesAt(timetable, now)`는 남은 공강 분을 그대로 돌려줍니다
(수업 중이면 `ActivityRecommender.IN_CLASS`, 오늘 수업이 없으면 `UNLIMITED`).
화면에서 "다음 수업까지 N분" 안내를 띄우거나, 추천이 `null`일 때 그 이유가
**수업 중이라서인지 / 할 게 없어서인지** 구분하는 데 씁니다.

---

## 4. 미해결 설계 항목 (논의 필요)

### 4-1. ~~공동구매 ↔ 채팅방 자동 연동~~ ✅ B안으로 결정·구현 완료

**서버가 `handlePostCreate` 안에서 채팅방까지 만듭니다** (요청 한 번 = `DATA_LOCK` 한 번이라
중간에 끊겨도 고아 채팅방이 남지 않음). 클라이언트가 `CHATROOM_CREATE`를 따로 보내면 안 됩니다.

확정된 규칙:

| 항목 | 규칙 |
|---|---|
| roomId | 서버가 `nextRoomId()`로 채번. 클라이언트가 보낸 `chatRoomId`는 **무시하고 덮어쓴다** |
| 방장 | 글쓴이. 생성 즉시 참여자로도 들어간다 |
| 정원 | 글의 `maxMembers`를 그대로 방 정원으로 (§3.1 "현재 인원수 = 채팅방 참여자 수") |
| 가입 제한 | 걸지 않는다 — 게시판을 볼 수 있으면 누구나 참여 가능 |
| `maxMembers` 검증 | `-1`(무제한) 또는 **2 이상**. 글쓴이가 이미 1명이라 1이면 만들자마자 가득 참 |
| 저장 순서 | `board.save()` 성공 → `dataStore.addChatRoom()`. 게시글 저장이 실패하면 방은 등록 자체가 안 됨 |
| `POST_UPDATE` | `chatRoomId`는 **클라이언트 값으로 덮어쓰지 않는다**(남의 방 가리키기/연결 끊기 방지). `maxMembers`를 바꾸면 연결된 방의 정원도 같이 바뀐다 |

클라이언트는 `chatRoomId`를 `null`로 두고 `POST_CREATE`만 보내면 되고, 응답으로 오는
`GroupBuyPost`에 서버가 채운 `chatRoomId`가 들어 있습니다.

> 남은 것: **공동구매 글을 지울 때 연결된 채팅방을 어떻게 할지**는 아직 안 정했습니다.
> 현재 `handlePostDelete`는 방을 그대로 둡니다(채팅 기록 보존). 같이 지울지 팀에서 결정하세요.

### 4-2. 데이터 파일이 전부 비어 있음 🔴 우선순위 높음

**`server/data/**` 와 `client/recommend_data/**` 의 파일이 (할 거/시간표 2개를 뺀) 전부 0바이트입니다.**
지금 서버를 켜면 회원이 한 명도 없어서 **로그인 자체가 안 됩니다.**

최소한 필요한 것:

| 파일 | 필요한 내용 |
|---|---|
| `server/data/users.dat` | **관리자 계정 1개** + 테스트 학생 몇 명. 관리자는 여기 직접 넣어야만 만들 수 있음 |
| `client/recommend_data/book_recommend.dat` | 학과별 책 (`학과\|책이름\|설명`) |
| `client/recommend_data/menu_recomend.dat` | 식당 9곳의 메뉴 (`식당\|메뉴`) |
| `client/recommend_data/e_resterant_menu.txt` | 오늘의 학식 텍스트 |
| `client/recommend_data/tip_under_menu.txt` | 꿀팁 여러 줄 |
| ~~`client/recommend_data/active_recommend.dat`~~ | ✅ 채움 — 할 거 40개 (10~45분) |
| ~~`client/recommend_data/time_table.dat`~~ | ✅ 채움 — 월~금 테스트용 시간표 16칸 |

관리자 계정 예시 (`users.dat`에 한 줄):
```
2026000001|관리팀|false|admin01|true
```
게시판 `.dat`은 비어 있어도 정상 동작합니다 (글이 0개인 상태).

### 4-3. 특수문자 이스케이프 🟡

글 내용에 `|` `^` `;` `:` 를 입력하면 저장 포맷이 깨져 **다음 서버 시작 때 `load()`가
실패합니다.** 선택지:

- (a) 입력 단계에서 해당 문자를 거부/치환
- (b) 저장 시 이스케이프, 복원 시 되돌리기 (`DataFormat`에 인코더/디코더 추가)

`FileStorage`와 `DataFormat` 한 곳만 고치면 되도록 설계돼 있으니 (b)도 어렵지 않습니다.
발표 시연에서 사용자가 `|`를 칠 가능성이 낮지 않으므로 **최소한 (a)라도 하는 것을 권장합니다.**

### 4-4. 민원 템플릿 데이터 🟡

"자주 묻는 질문" 탭에서 불러올 민원 템플릿(정적 데이터)이 아직 없습니다.
[02_requirements.md §3.5](02_requirements.md) 참고. 저장 위치와 형식부터 정해야 합니다.

### 4-5. 공지 푸시를 받는 화면이 없음 🟡

서버는 `NOTICE_PUSH`를 이미 보내고 있지만, `setPushListener`는 **하나만 등록**되고
현재 `ChatRoomPanel`만 등록합니다. 즉 **공지 푸시가 도착해도 아무도 받지 않습니다.**

해결하려면 리스너를 `MainFrame` 레벨로 올리고 `packet.getType()`으로 분기해야 합니다.
(채팅방에 들어가 있는 동안 공지가 오면 채팅방 화면이 처리하려 해서 꼬입니다.)

### 4-6. 채팅방 초대 기능 🟢

`ChatRoom.inviteBypassesLimit`은 **값만 보관하고 판정에 쓰이지 않습니다** — 초대 기능
자체가 없기 때문입니다. 이번 범위에 넣을지 결정 필요.

### 4-7. 게시글 검색 🟢

[02_requirements.md §2.2](02_requirements.md)의 "게시글 검색"에 해당하는 `RequestType`이
없습니다. `POST_LIST` 결과를 클라이언트에서 거르는 방식이면 서버 수정 없이 됩니다.

### 4-8. 해시태그 필터 UI 🟢

`GroupBuyBoard.filterByHashtag`는 **서버에만 있고 이를 호출하는 `RequestType`이 없습니다.**
§3-2에서 구현하더라도 클라이언트에서 거르는 편이 간단할 수 있습니다.

---

## 5. 작업 분담 제안

서로 안 겹치도록 파일 단위로 나눈 안입니다.

| 담당 | 작업 | 예상 난이도 |
|---|---|---|
| **A** | `PostDetailPanel` + `PostEditorPanel` (10곳) | 중 |
| **B** | 채팅 3종: `ChatRoomListPanel`, `ChatRoomCreatePanel`, `ChatRoomPanel` (10곳) | 중 |
| **C** | `GroupBuyPostEditorPanel` + `GroupBuyPost`/`GroupBuyBoard` + **§4-1 채팅방 연동 결정** (7곳) | 중 |
| **D** | `RecommendPanel` + `ActivityRecommender.recommendNow` + **§4-2 데이터 파일 전부 채우기** (2곳 + 데이터) | 하~중 |
| 공통 | `HomePanel` 2곳은 누구든 먼저 (`switchTo` 한 줄씩) | 하 |

> **§4-2(데이터 파일)를 가장 먼저 하세요.** 이게 없으면 나머지 작업을 **화면에서 확인할 수
> 없습니다** — 로그인조차 안 되기 때문입니다.

---

## 6. 진행 상황을 갱신하는 법

기능을 완성했다면:

1. 코드에서 `TODO: 구현 필요` 주석을 지운다
2. 이 문서 §3 표에서 해당 줄을 지우고 §2로 옮긴다
3. §1의 요약과 맨 위 `TODO` 개수를 갱신한다
4. 사양 자체가 바뀌었다면 해당 사양 문서(02~07)도 **같은 커밋에서** 함께 고친다
