# 08. 진행 현황과 남은 작업

**진행 상황은 이 문서에만 적습니다.** 다른 문서에는 사양만 적습니다 — 상태를 여러 곳에
쓰면 반드시 서로 어긋납니다.

> **기준: 2026-07-23, 작업 트리 기준(미커밋 변경 포함)**
> Java 21 / 소스 70개 / **`javac` 에러 0개 확인** / 코드 내 `TODO` **0개**
>
> ⚠️ 이 문서가 오래됐다고 느껴지면 **코드가 정답**입니다:
> ```powershell
> Get-ChildItem -Recurse -Filter *.java | Select-String "TODO: 구현 필요"
> ```

---

## 1. 한눈에 보기

**필수 기능은 전부 구현됐습니다.** 남은 것은 §4의 선택 항목(팀 논의 필요)뿐입니다.

| 영역 | 상태 |
|---|---|
| 데이터 모델 (`model/`, `model/boards/`) | 🟩 **완료** |
| 통신 프로토콜 (`model/protocol/`) | 🟩 **완료** |
| 서버 (`server/`) | 🟩 **완료** — 핸들러 17개 + 푸시까지 동작 |
| 클라이언트 통신 (`client/CT/`) | 🟩 **완료** |
| GUI (`client/GUI/`) | 🟩 **완료** — 화면 17개 전부 (홈 대분류 → 게시판 세부 화면 분리, 시간표 입력 화면 추가) |
| 추천 3종 | 🟩 **완료** — 할 거 추천의 공강 판단 규칙까지 확정, 시간표는 화면에서 직접 입력/수정 |
| **데이터 파일** | 🟩 **완료** — 관리자 1 + 학생 5명, 게시판 6종·채팅방 3개·추천 데이터·민원 FAQ 템플릿 전부 |

검증 결과(2026-07-23): 서버 기동 후 프로토콜 왕복 **50항목 통과**(로그인·권한·게시글 CRUD·댓글·
공지 필터·공동구매 채팅방 자동 생성) + **닉네임/채팅방 이름/민원 권한 15항목 추가 통과**,
GUI **화면 17개 전부 생성 및 전환 확인**,
특수문자·줄바꿈 이스케이프 **82항목 + 서버 재기동 왕복 13항목 통과**(§4-3).

**2026-07-23 시연 후 추가 수정 (1차)**: 홈 화면 대분류 재구성(게시판/공지사항/민원/채팅/추천),
공동구매 상세에 채팅방 진입 버튼, 채팅방 이름·검색, 채팅방 닉네임 설정, 시간표 입력 화면,
민원 FAQ 템플릿, 상세화면 이미지 비율 축소, **관리자의 민원 수정/삭제 차단(버그 수정 — 관리자는
답변만 가능해야 하는데 본인 글처럼 수정/삭제가 가능했음)**.

**2026-07-23 추가 수정 (2차)**: 실제 학과 조직도(단과대/학부/학과)를 받아 `model/AcademicStructure`로
확정하고, 회원가입·회원정보수정·공지 대상학과 3곳 전부를 **단과대→학부→학과 3단 드롭다운**
(`client/GUI/DepartmentPickerPanel`)으로 교체했습니다. 자세한 내용은 [04_data_model.md §7](04_data_model.md).

**2026-07-24 추가 수정**: 채팅방 삭제(방장 전용, `CHATROOM_DELETE` 신설) 추가 — `ChatRoom.canDelete()`,
`DataStore.removeChatRoom()`, `ChatRoomPanel`의 "채팅방 삭제" 버튼(방장에게만 보임), 확인 다이얼로그까지
확인함. `documents/02_requirements.md §4.1`, `05_protocol.md §2`도 같은 커밋에서 갱신.
채팅방 닉네임 설정(§4.2)은 이미 구현되어 있음을 재확인함 — 입장 시 미설정이면 자동으로 물어보고
(`ChatRoomPanel.open()`), 화면 안 "닉네임 설정" 버튼으로 언제든 바꿀 수 있으며 서버가 참여자만
허용하고 저장까지 반영함(`ClientHandler.handleChatRoomSetNickname`).

---

## 2. 완성된 것 (건드릴 필요 없음)

### 모델
- `User`, `Chat`, `ChatRoom`, `Post` 4종, `Comment`, 게시판 6종 — 필드·생성자 전부
- 파일 저장/복원 — 모든 클래스의 `toDataString` / `fromDataString`, `load()` / `save()`
- **특수문자·줄바꿈 이스케이프** — `DataFormat.encode()` / `decode()`.
  사용자가 제목에 `|`를 치거나 본문에서 Enter를 눌러도 파일이 깨지지 않는다.
  규칙과 주의점은 [04_data_model.md §1-1](04_data_model.md)
- 권한 판단 — `canEdit`, `canDelete`, `canAccess`, `canWrite`, `isVisibleTo`
  > ⚠️ **`canEdit`/`canDelete`는 2026-07-23 기획 변경으로 코드가 사양과 어긋난 상태입니다 — §4-9.**
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

### GUI (화면 17개 전부 + 셸)
`MainFrame`, `LoginPanel`, `RegisterPanel`, `HomePanel`, `BoardMenuPanel`(신규 — 홈의 "게시판"
대분류를 누르면 나오는 세부 게시판 선택 화면), `PostListPanel`, `NoticePostEditorPanel`,
`ComplaintPanel`, `UserEditPanel`, `AdminPanel`, `RecommendPanel`, `TimetableEditorPanel`(신규 —
할 거 추천 탭의 "시간표 입력/수정" 버튼으로 진입),
**채팅 3종** `ChatRoomListPanel`, `ChatRoomCreatePanel`, `ChatRoomPanel`,
**게시글 3종** `PostDetailPanel`, `PostEditorPanel`, `GroupBuyPostEditorPanel`

**홈 대분류가 바뀌었습니다.** 기존에는 `HomePanel`이 게시판 6종을 평면으로 나열했는데, 이제
`HomePanel`은 **게시판/공지사항/민원/채팅/추천** 대분류 5개만 갖고 있고, "게시판"을 누르면
`BoardMenuPanel`이 자유/공동구매/학과/기숙사 4종을 보여줍니다. 공지사항은 게시판 하위가 아니라
홈 대분류에 그대로 남겨뒀습니다. `PostListPanel.open(boardKey, backTarget)`의 `backTarget`이
게시판 4종은 `"boardMenu"`, 공지/민원은 `"home"`으로 갈립니다.

게시글 화면 3종에서 확정한 것(코드 주석에도 적어둠):

- **게시글 id는 `UUID.randomUUID().toString()`으로 통일.** 여러 클라이언트가 각자 만들어도
  겹치지 않아야 하고, 서버가 중복 id를 거부하기 때문이다. 에디터 4종이 모두 같은 규칙이다.
- **`PostListPanel.refresh()`를 새로 뒀다.** 목록은 게시글을 사본으로 들고 있어서, 상세·에디터에서
  저장/삭제하고 `switchTo("postList")`만 하면 지운 글이 그대로 보인다. 상세·에디터는 `backTarget`을
  모르기 때문에 `open(boardKey, backTarget)`을 대신 부를 수 없다 ([06_gui.md §3](06_gui.md) 참고).
- **`PostDetailPanel.openEditor()`는 `boardKey`가 아니라 `post`의 실제 타입으로 분기한다.**
  학과 게시판은 `boardKey`가 곧 학과명이라 목록 화면처럼 상수로 가를 수 없다.
- **댓글 등록/삭제 후에는 들고 있는 사본도 직접 고친다.** `COMMENT_ADD`/`COMMENT_DELETE` 응답에는
  payload가 없어서 다시 조회하지 않으면 화면이 갱신되지 않는다. 삭제는 `JList`의 선택 위치가
  곧 서버가 받는 `commentIndex`다.
- **공동구매 상세의 "참여 N / 최대 M명"은 `CHATROOM_LIST`로 방을 찾아 센다.** 방 하나만 받는
  요청이 없어서 전체 목록에서 `chatRoomId`가 같은 것을 고른다. 댓글 작업마다 다시 요청하지
  않도록 `open()`에서 한 번만 받아 둔다.
- **수정할 때 해시태그는 못 바꾼다.** `GroupBuyPost.hashtags`가 `final`이고 서버의 `POST_UPDATE`도
  제목/내용/첨부/최대인원만 반영한다. `NoticePostEditorPanel`이 대상 학과를 막은 것과 같은 방식으로
  입력칸을 비활성화한다.

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

## 3. 남은 작업 — 코드 `TODO` 0개 ✅

`TODO: 구현 필요`는 전부 없어졌습니다. 아래는 마지막에 무엇을 어떻게 정했는지의 기록입니다.

### 3-1. GUI (14곳) — ✅ 완료

`PostDetailPanel`(6) / `PostEditorPanel`(4) / `GroupBuyPostEditorPanel`(4).
확정 사항은 §2의 "게시글 3종" 항목에 정리해 두었습니다.

### 3-2. 모델 (2곳) — ✅ 완료

| 위치 | 메서드 | 확정 내용 |
|---|---|---|
| `model/boards/GroupBuyPost.java` | `getCurrentMemberCount(ChatRoom)` | `linkedRoom.getMemberIds().size()`. 논의 중이던 **`null` 처리는 `-1` 반환으로 확정** — 상수 `GroupBuyPost.UNKNOWN_MEMBER_COUNT`. "0명"과 "아직 모름"을 구분해야 화면에서 `?`로 표시할 수 있습니다 |
| `model/boards/GroupBuyBoard.java` | `filterByHashtag(String)` | `posts`를 `GroupBuyPost`로 캐스팅해 `hashtags.contains(태그)`인 것만. 태그는 정확히 일치해야 합니다 |

> `filterByHashtag`는 **서버에만 있고 이를 부르는 `RequestType`이 없습니다**(§4-8).
> 화면에서 태그로 거르려면 `POST_LIST` 결과를 클라이언트에서 필터링하세요.

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

### 4-2. ~~데이터 파일이 전부 비어 있음~~ ✅ 전부 채움

시연에 바로 쓸 수 있는 분량으로 채웠습니다. **비밀번호는 테스트용 평문입니다.**

**로그인 계정** (`server/data/users.dat`)

| 학번 | 비밀번호 | 학과 | 기숙사 | 관리자 |
|---|---|---|:--:|:--:|
| 2026000001 | `admin01` | 관리팀 | | ✅ |
| 2026591007 | `pass01` | AI소프트웨어학과 | ✅ | |
| 2026591008 | `pass02` | AI소프트웨어학과 | | |
| 2025140012 | `pass03` | 컴퓨터공학전공 | ✅ | |
| 2025140033 | `pass04` | 컴퓨터공학전공 | | |
| 2024310005 | `pass05` | 전기공학전공 | | |

> 학과명은 2026-07-23 `model/AcademicStructure`의 실제 조직도에 맞춰 개편했습니다
> (컴퓨터공학과→컴퓨터공학전공, 물리학과→전기공학전공). 셋 다 트리에 실제로 존재하는
> 리프 이름입니다 — [04_data_model.md §7](04_data_model.md) 참고.

기숙사생/통학생과 학과를 섞어 둔 이유는 `DormBoard.canAccess`, `DepartmentBoard.canAccess`,
`NoticePost.isVisibleTo`가 실제로 갈리는 걸 시연에서 보여주기 위해서입니다.
**관리자는 회원가입으로 만들 수 없습니다**(서버가 `admin`을 강제로 `false`로 둠) — 이 파일에서만 생깁니다.

**그 외 채운 것**: 자유 4개(댓글 포함) · 학과별 각 2개 · 기숙사 2개 · 공지 3개(전체/학과지정/기숙사) ·
공동구매 2개(채팅방 001·002와 연결) · 민원 2개(답변완료 1, 대기 1) · 채팅방 3개(채팅·가입신청·닉네임 포함) ·
책 12권 · 식당 9곳 메뉴 19개 · 학식 텍스트 · 꿀팁 10줄.
(`active_recommend.dat` 40개, `time_table.dat` 16칸은 이전부터 채워져 있었습니다.)

> ⚠️ **데이터 파일을 손으로 고칠 때 주의**
> - 반드시 **UTF-8(BOM 없이)**. Windows에서 `Set-Content -Encoding utf8`은 BOM을 붙이는데,
>   그 BOM이 첫 줄 0번 필드(`id`) 안으로 들어가 파싱이 깨집니다. 메모장 대신 VS Code 등을 쓰세요.
> - 모든 `split`이 limit `-1`이라 **뒤쪽 빈 필드도 구분자를 남겨야 합니다.**
>   파이프(`|`) 개수로 검산하세요: `User` 4개 / 일반 `Post` 7개 / `NoticePost` 9개 /
>   `GroupBuyPost`·`ComplaintPost` 10개 / `ChatRoom` 11개(2026-07-23부터, `name` 필드 추가 —
>   10개짜리 예전 파일도 그대로 읽힘).
> - 본문에 `|` `^` `;` 를 넣으면 안 됩니다 (§4-3, 이스케이프 미구현).

### 4-3. ~~특수문자 이스케이프~~ ✅ (b)안으로 해결

`DataFormat.encode()` / `decode()`를 추가하고, 사용자가 입력한 **말단 값**에 전부 씌웠습니다.
상세 규칙은 [04_data_model.md §1-1](04_data_model.md)에 있습니다.

조사해보니 원래 이 문단의 설명이 실제와 달랐습니다:

| 입력한 곳 | 문자 | 예전 결과 |
|---|---|---|
| 본문 | **줄바꿈(Enter)** | 🔴 파일이 2줄로 쪼개짐 → 다음 기동 시 `ArrayIndexOutOfBoundsException` |
| 제목 | `\|` | 🔴 필드가 밀림 → `DateTimeParseException` |
| **댓글·채팅** 내용 | `^` `;` | 🔴 중첩 구분자라 깨짐 |
| 본문 | `^` `;` `,` `:` | 🟢 멀쩡했음 (본문은 그 문자로 쪼개지 않음) |

- **가장 위험한 건 문서에 없던 "줄바꿈"이었습니다.** 본문이 `JTextArea`라 Enter는 지극히 정상
  행동인데, 한 줄 = 한 레코드라서 글 하나가 파일 두 줄이 되었습니다. `|`보다 훨씬 흔합니다.
- **위험한 문자는 그 값이 중첩 구조의 어디에 있느냐에 따라 달랐습니다.** 그래서 (a)안(입력 거부)은
  필드마다 규칙이 달라져 한 곳만 빠뜨려도 터지고, 본문 줄바꿈을 막으면 문단을 못 나누게 됩니다.

검증: 위 7가지 + 역슬래시·맵 필드 등 엣지 케이스 **82항목 통과**, 실제 서버로 저장 후
**재기동해서 값이 그대로 복원되는 것까지 13항목 확인**. 기존 `.dat` 파일은 변환 없이 그대로 읽힙니다.

### 4-4. ~~민원 템플릿 데이터~~ ✅ 완료

`client/complaint_data/faq_templates.dat`(제목|1차분류|2차분류|내용, 6개)를 추가하고
`ComplaintPanel`에 "자주 묻는 문의" 버튼을 달았습니다. 선택하면 제목/분류/내용이 입력칸에
채워지고, 이어서 사용자가 수정한 뒤 제출합니다. 서버 통신 없이 로컬 파일만 읽습니다
(추천 데이터와 같은 성격).

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

`GroupBuyBoard.filterByHashtag`는 구현됐지만(§3-2) **서버에만 있고 이를 호출하는 `RequestType`이
없습니다.** 화면에 넣는다면 `POST_LIST` 결과를 클라이언트에서 거르는 편이 간단합니다
(`post instanceof GroupBuyPost`로 캐스팅 후 `getHashtags().contains(태그)`).

### 4-9. ~~관리자 권한 축소~~ ✅ 완료 (2026-07-23 시연 중 재확인 후 수정)

시연 중 "관리자가 민원을 수정/삭제할 수 있다"는 버그가 실제로 재현되어 이 항목을 마저 고쳤습니다.

| | 예전 코드 | **현재** |
|---|---|---|
| 관리자가 남의 글 **수정** | 가능 | **불가** |
| 관리자가 남의 글 **삭제** | 가능 | 가능 (변경 없음) |
| 관리자가 **민원** 수정/삭제 | 가능 | **둘 다 불가 — 답변(댓글)만** |

- `model/boards/Post.java`의 `canEdit()`에서 `requester.isAdmin() ||`를 제거 — 본인 글일 때만
  `true`. `canDelete()`는 관리자 권한 그대로 유지.
- `model/boards/ComplaintPost.java`의 `canEdit()`/`canDelete()`를 오버라이드해서 둘 다 본인만
  `true` (canDelete는 Post 기본 구현과 달라야 하므로 필수 오버라이드).
- `server/CT/ClientHandler.java`(`handlePostUpdate`/`handlePostDelete`→`AbstractBoard.removePost`)와
  `client/GUI/PostDetailPanel.java`(버튼 노출)는 이미 `canEdit`/`canDelete`를 호출하고 있어서
  모델만 고치면 그대로 따라옵니다 — 새 분기를 넣지 않았습니다.

검증(스모크 테스트): 관리자의 민원 수정/삭제 요청 → `ERROR`, 관리자 댓글(답변) → `OK` +
자동 답변완료 전환, 작성자 본인의 수정/삭제 → `OK`.

---

## 5. 남은 선택 과제 — 우선순위 순

필수 기능은 끝났으므로, 시간이 남으면 위에서부터 하세요. 서로 파일이 겹치지 않습니다.

| 우선 | 항목 | 건드릴 파일 | 난이도 |
|:--:|---|---|---|
| 1 | **§4-5 공지 푸시 수신** — 서버는 이미 보내는데 받는 화면이 없습니다 | `MainFrame`, `ChatRoomPanel` | 중 |
| 2 | **§4-7 게시글 검색** — 요구사항에는 있는데 화면이 없습니다 | `PostListPanel`만 | 하 |
| 3 | **§4-8 해시태그 필터 UI** | `PostListPanel`만 | 하 |
| 4 | **§4-6 채팅방 초대** — 범위에 넣을지부터 결정 | `ChatRoom`, 프로토콜, 채팅 화면 | 상 |
| — | **§4-1 남은 결정** — 공동구매 글을 지울 때 연결된 채팅방도 지울지 (지금은 남겨둠) | `ClientHandler.handlePostDelete` | 하 |

> ✅ 대상 학과 드롭다운(3단 단과대→학부→학과)은 실제 조직도를 받아 완료했습니다. §7
> ([04_data_model.md §7](04_data_model.md))을 보세요.

> 🔴 시연 중 실제로 터질 수 있던 항목(§4-3, §4-9)은 모두 해결됐습니다. 위 항목들은 전부
> "있으면 더 좋은" 것들이라, 시간이 없으면 손대지 않아도 발표에 지장이 없습니다.

---

## 6. 진행 상황을 갱신하는 법

기능을 완성했다면:

1. 코드에서 `TODO: 구현 필요` 주석을 지운다
2. 이 문서 §3 표에서 해당 줄을 지우고 §2로 옮긴다
3. §1의 요약과 맨 위 `TODO` 개수를 갱신한다
4. 사양 자체가 바뀌었다면 해당 사양 문서(02~07)도 **같은 커밋에서** 함께 고친다
