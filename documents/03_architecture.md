# 03. 아키텍처 — 프로그램이 실제로 도는 방식

"이 코드가 어떻게 굴러가는가"를 설명합니다. 필드 목록은 [04_data_model.md](04_data_model.md),
패킷 규격은 [05_protocol.md](05_protocol.md)를 보세요.

---

## 1. 계층 구조

```
┌──────────────────────────────────────────────────────────┐
│  client/GUI          Swing 화면 15개 + MainFrame(셸)       │
│       ↕                                                   │
│  client/CT           ServerConnection / PushListener      │
└───────────────────────────┬──────────────────────────────┘
                            │  Packet 객체 직렬화 (TCP 5000)
┌───────────────────────────┴──────────────────────────────┐
│  server/CT           ServerMain → ClientHandler(스레드/1인) │
│                      SessionRegistry (접속자 목록)          │
│       ↕                                                   │
│  server/board        DataStore (메모리 창고)                │
│       ↕                                                   │
│  server/data/*.dat   실제 저장 파일                          │
└──────────────────────────────────────────────────────────┘
              ↑ 양쪽이 공유 ↑
┌──────────────────────────────────────────────────────────┐
│  model/ , model/boards/ , model/protocol/                 │
│  엔티티 + 권한 판단 + 직렬화 포맷 + 패킷 규격                 │
└──────────────────────────────────────────────────────────┘
```

**책임 분리 원칙**

| 계층 | 하는 일 | 하지 않는 일 |
|---|---|---|
| `model/` | 데이터 보관, **권한 판단 규칙**(`canEdit` 등), 파일 문자열 변환 | 파일 경로 결정, 네트워크 |
| `server/board/DataStore` | 파일 ↔ 메모리 동기화, 게시판/유저/채팅방 찾아주기 | 권한 검사 (업무 로직 없음) |
| `server/CT/ClientHandler` | **업무 로직 전부** — 세션 확인, 권한 검사 호출, 저장, 푸시 | 데이터 구조 정의 |
| `client/CT` | 연결·직렬화·요청응답 짝맞춤 배관 | 화면, 업무 판단 |
| `client/GUI` | 화면 그리기, 입력 받기, 요청 보내기 | 데이터 파일 직접 접근 (**절대 금지**) |

> **권한 검사는 반드시 서버에서 한다.** GUI에서 버튼을 숨기는 것은 편의일 뿐이고,
> 실제 차단은 `ClientHandler`가 `requireLogin()` / `requireAdmin()` / `requireAccess()` /
> `canEdit()` 등으로 수행합니다. 클라이언트가 보낸 `admin` 값이나 작성자 학번은 믿지 않습니다.

---

## 2. 서버를 켰을 때

```
ServerMain.main()
  ↓
new DataStore()
  ├ registerBoards() : 게시판 8개를 boardKey와 함께 등록
  │                    ("free", "groupbuy", "dorm", "notice", "complaint", 학과 3개)
  └ loadAll()        : 모든 게시판 .dat + users.dat + chatrooms/*.dat 를 메모리로 로드
  ↓
new SessionRegistry()    ← 접속자 목록 (처음엔 비어 있음)
  ↓
ServerSocket(5000) 열고 대기
  ↓
접속할 때마다 → new Thread(new ClientHandler(소켓, dataStore, sessionRegistry)).start()
```

`DataStore`와 `SessionRegistry`는 **서버 전체에 딱 하나씩**이고 모든 `ClientHandler`가
같은 인스턴스를 공유합니다.

> **"스레드"란?** 여러 일을 동시에 처리하는 일꾼입니다. 학생 30명이 접속하면 일꾼 30명이
> 각자 1명씩 담당합니다. 그래야 한 명이 느려도 나머지가 막히지 않습니다.

---

## 3. 요청-응답 흐름 (로그인 예시)

**이 흐름 하나만 이해하면 나머지 기능도 전부 같은 구조입니다.**

```
[클라이언트]                                    [서버]

LoginPanel.attemptLogin()
  아이디/비번 칸에서 글자 꺼냄
       ↓
  new LoginRequest(id, password)              ← "주문서"(DTO) 작성
       ↓
  Packet.request(LOGIN, 주문서)                ← 주문서를 "봉투"에 담음
       ↓                                          (requestId 자동 채번)
  connection.sendRequest(봉투) ──── TCP ────→  ClientHandler.run()이 봉투 받음
       ↓                                              ↓
  (응답 올 때까지 최대 10초 블로킹)               handleRequest() : 락 필요한 타입인가?
                                                      ↓
                                                 dispatch() : LOGIN이네?
                                                      ↓
                                                 handleLogin()
                                                   dataStore.getUser(id)
                                                   비밀번호 대조
                                                   currentUser = user
                                                   sessionRegistry.register(...)
                                                      ↓
                                                 Packet.success(원래봉투, User)
       ↓                                              ↓
  응답 봉투 받음      ←──────── TCP ─────────  sendPacket(응답)
       ↓
  status == OK  → setCurrentUser(user)
                  switchTo(user.isAdmin() ? "admin" : "home")
  status == ERROR → errorMessage를 그대로 팝업
```

### 요청과 응답은 어떻게 짝을 맞추나?

`ServerConnection`은 리더 스레드 1개를 따로 돌립니다. 응답이 아무 때나 도착할 수 있으므로:

1. `sendRequest`가 `requestId` → 대기열(`BlockingQueue`)을 `pendingResponses`에 등록
2. 요청을 보내고 그 대기열에서 **최대 10초** 블로킹으로 기다림
3. 리더 스레드가 패킷을 받으면 `requestId`로 대기열을 찾아 넣어줌
4. `sendRequest`가 깨어나서 응답을 반환
5. 10초가 지나면 `"서버 응답 시간 초과"` 예외

### 에러 처리 규약

`handleXxx`에서 **검증에 실패하면 그냥 예외를 던지면 됩니다.** `handleRequest`가 잡아서
`Packet.error(request, e.getMessage())`로 바꿔 보냅니다.

**예외 메시지는 사용자에게 그대로 팝업으로 보이므로 한국어로 씁니다.**

```java
throw new IllegalStateException("공지는 관리자만 작성할 수 있습니다");
```

---

## 4. 푸시 — 서버가 먼저 말 거는 경우

보통은 클라이언트가 물어보고 서버가 답합니다. 그런데 채팅과 공지는 반대가 필요합니다.
**다른 사람이 친 채팅이 내 화면에 자동으로 떠야** 하니까요.

```
A학생이 CHAT_SEND
   ↓
ClientHandler.handleChatSend()
   ├ 방 참여자인지 확인 → Chat 생성 → room.sendChat() → 파일 저장
   └ 같은 방의 다른 멤버 각각에 대해
        sessionRegistry.sendTo(멤버학번, Packet.push(CHAT_MESSAGE_PUSH, ...))
             ↓ (그 사람을 담당하는 다른 ClientHandler를 찾아서)
        그 handler.sendPacket(푸시)
             ↓
   B학생의 ServerConnection 리더 스레드가 받음
             ↓  packet.isPush() == true → requestId가 없음
        pushListener.onPush(packet)
             ↓
   ChatRoomPanel.onPush()가 화면에 메시지 추가
```

### `SessionRegistry`가 하는 일

푸시를 보내려면 **"지금 접속 중인 사람이 누구고, 그 사람을 담당하는 `ClientHandler`가
어느 것인지"** 를 알아야 합니다. 그 목록이 `SessionRegistry`입니다.

| 메서드 | 언제 |
|---|---|
| `register(userId, handler)` | 로그인 성공 시 |
| `unregister(userId, handler)` | 로그아웃 / 연결 끊김 시 |
| `sendTo(userId, packet)` | 특정 1명에게 푸시 (채팅) — **접속 중이 아니면 조용히 무시** |
| `sendToAll(packet, filter)` | 조건에 맞는 접속자 전원에게 (공지 — `NoticePost::isVisibleTo`) |

- `ConcurrentHashMap`이라 여러 스레드가 동시에 써도 안전합니다.
- 같은 학번으로 다시 로그인하면 **마지막 연결이 그 유저의 세션**이 됩니다.
- 오프라인 사용자에게는 푸시가 가지 않습니다. 그 사람은 다음에 목록을 새로 조회할 때
  내용을 보게 됩니다 (오프라인 보관함은 만들지 않습니다).

### 푸시를 받는 화면이 지켜야 할 것

`onPush`는 **네트워크 스레드**에서 호출됩니다. Swing 컴포넌트를 그 스레드에서 직접 건드리면
안 되므로 반드시 감싸야 합니다:

```java
SwingUtilities.invokeLater(() -> { /* 화면 갱신 */ });
```

---

## 5. 동시성 — 데이터가 꼬이지 않게 하는 법

학생 2명이 **똑같은 순간에** 글을 쓰면 데이터가 섞여 하나가 사라질 수 있습니다.
그래서 데이터를 **바꾸는** 요청은 `ClientHandler.DATA_LOCK`이라는 정적 자물쇠로 감싸
**한 번에 한 명씩** 처리합니다.

```java
if (SYNCHRONIZED_TYPES.contains(request.getType())) {
    synchronized (DATA_LOCK) { response = dispatch(request); }
} else {
    response = dispatch(request);   // 순수 조회는 락 없이
}
```

| 락을 거는 요청 (`SYNCHRONIZED_TYPES`, 12종) | 락을 걸지 않는 요청 |
|---|---|
| `REGISTER`, `USER_UPDATE` | `LOGIN`, `LOGOUT` |
| `POST_CREATE`, `POST_UPDATE`, `POST_DELETE` | `USER_LOOKUP` (조회) |
| `COMMENT_ADD`, `COMMENT_DELETE` | `POST_LIST` (조회) |
| `CHATROOM_CREATE`, `CHATROOM_JOIN_REQUEST` | `CHATROOM_LIST` (조회) |
| `CHATROOM_JOIN_APPROVE`, `CHATROOM_JOIN_REJECT` | `DISCONNECT` |
| `CHAT_SEND` | |

> ⚠️ **새 `RequestType`을 추가할 때**: 데이터를 바꾸는 요청이면 반드시
> `SYNCHRONIZED_TYPES`에도 넣으세요. 안 넣으면 아주 가끔, 재현이 안 되는 버그가 납니다.

### 조회 결과를 복사해서 보내는 이유

`handlePostList`는 게시판의 리스트를 그대로 넘기지 않고 `new ArrayList<>(visible)`로
복사해서 넘깁니다. 서버가 들고 있는 리스트를 그대로 직렬화하면, 그 사이 다른 스레드가
리스트를 수정해서 직렬화가 깨질 수 있기 때문입니다.

### 소켓 쓰기도 동기화한다

`sendPacket`은 `synchronized (this)`로 감쌉니다. 응답을 보내는 스레드와, 다른 사람의 채팅을
푸시하는 스레드가 **같은 `ObjectOutputStream`에 동시에 쓰면** 스트림이 깨지기 때문입니다.

---

## 6. 데이터 저장

DB 없이 **텍스트 파일**에 저장합니다. **한 줄 = 객체 하나**입니다.

| 파일 | 들어있는 것 |
|---|---|
| `server/data/users.dat` | 회원 전체 (한 줄 = 회원 1명) |
| `server/data/boards/free_board.dat` | 자유게시판 글 (한 줄 = 글 1개) |
| `server/data/boards/group_buying_board.dat` | 공동구매 |
| `server/data/boards/dormitory_board.dat` | 기숙사 |
| `server/data/boards/notice_board.dat` | 공지 |
| `server/data/boards/complaint_board.dat` | 민원 |
| `server/data/boards/class_boards/*.dat` | 학과별 (학과마다 파일 1개) |
| `server/data/chatrooms/001.dat` | 채팅방 (**파일 1개 = 방 1개, 내용은 한 줄**) |
| `client/recommend_data/*` | 추천 데이터 (서버 아님 — 클라이언트가 직접 읽음) |

### 저장 시점

```
서버 시작   : 파일 → 메모리 (DataStore.loadAll)
글이 바뀔 때 : 메모리 → 파일 (board.save() — 그 게시판 전체를 덮어씀)
채팅/가입   : 메모리 → 파일 (dataStore.saveChatRoom(room) — 그 방 파일만 덮어씀)
회원가입/수정: 메모리 → 파일 (dataStore.saveUsers())
```

즉 **변경 즉시 저장**이므로 서버를 껐다 켜도 데이터가 남습니다.
파일 입출력은 전부 `model/FileStorage`(`readLines` / `writeLines`)만 통과합니다 —
저장 방식을 바꿀 때 한 곳만 고치면 되도록 하기 위해서입니다.

### 구분 기호 (`model/DataFormat`)

| 상수 | 기호 | 용도 | 예 |
|---|---|---|---|
| `FIELD_DELIM` | `\|` | 칸과 칸 사이 | `학번\|학과\|비번` |
| `LIST_DELIM` | `,` | 목록 안의 항목들 | `기숙사,음식,생필품` |
| `SUBOBJECT_DELIM` | `^` | 댓글/채팅 하나의 내부 | `글쓴이^내용^시각` |
| `SUBLIST_DELIM` | `;` | 댓글/채팅 여러 개 사이 | `댓글1;댓글2;댓글3` |
| `MAP_ENTRY_DELIM` | `:` | Map의 key:value | `2026591007:저 껴주세요` |

예 (회원 1명):

```
2026591007|AI소프트웨어학과|true|pass01|false
   학번          학과       기숙사  비번  관리자
```

> ⚠️ **알려진 한계:** 글 내용에 `|` `^` `;` `:` 를 넣으면 저장 형식이 깨집니다.
> 이스케이프 처리는 아직 없습니다 — [08_status.md](08_status.md)의 미해결 항목 참고.

### 게시판별 글 형식이 섞이면 안 되는 이유

각 게시판은 자기 타입 전용 `parsePost()`로 한 줄을 복원합니다. 공지 게시판 파일에 일반
`Post` 줄이 섞이면 **다음 서버 시작 때 `load()`가 깨집니다.** 그래서 `ClientHandler`의
`requirePostType()`이 저장 **전에** 타입을 검사해서 막습니다.

| 게시판 | 허용되는 게시글 타입 |
|---|---|
| `NoticeBoard` | `NoticePost` |
| `ComplaintBoard` | `ComplaintPost` |
| `GroupBuyBoard` | `GroupBuyPost` |
| `FreeBoard`, `DormBoard`, `DepartmentBoard` | `Post` (정확히 이 타입) |
