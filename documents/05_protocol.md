# 05. 통신 프로토콜 명세

서버-클라이언트가 주고받는 메시지 규격입니다. [04_data_model.md](04_data_model.md)와 같은
성격 — **통신 코드를 AI에게 맡길 때 그대로 붙여넣는 기준 문서**입니다.

---

## 0. 구조 요약

- 클라이언트-서버는 소켓 하나를 계속 열어두는 **영속적 연결** 위에서 `Packet` 객체를
  `ObjectOutputStream` / `ObjectInputStream`으로 직렬화해 주고받습니다.
- `model/`(`User`, `Chat`, `ChatRoom`)과 `model/boards/`(`Post` 계열, `Comment`)의 엔티티는
  전부 `Serializable`이라 `Packet.payload`에 그대로 담을 수 있습니다.
  (`Board` 계열은 **서버에만 있으므로 `Serializable`이 아닙니다** — 게시판 객체 자체는
  절대 전송하지 않고, 안의 `Post` 목록만 보냅니다.)
- 프로토콜 클래스는 `model/protocol/` 한 곳에만 있고 `client/CT`, `server/CT` 양쪽이
  그대로 참조합니다 (빌드 시스템 없이 단일 컴파일 단위).

---

## 1. `Packet` — 봉투

생성자는 `private`입니다. 아래 **정적 팩토리 4개로만** 만듭니다.

| 팩토리 | 사용 시점 |
|---|---|
| `Packet.request(type, payload)` | 클라이언트가 요청 보낼 때 (`requestId` 자동 채번) |
| `Packet.success(request, payload)` | 서버가 성공 응답 보낼 때 (원래 요청의 `requestId`를 echo) |
| `Packet.error(request, message)` | 서버가 실패 응답 보낼 때 |
| `Packet.push(type, payload)` | 서버가 요청 없이 먼저 보낼 때 (**`requestId`가 없음** → `isPush()`가 true) |

필드: `type`, `requestId`, `status`(`OK`/`ERROR`), `errorMessage`, `payload`

> **왜 생성자를 막아뒀나?** 자유롭게 만들게 두면 "성공인데 에러 메시지가 들어있는 봉투" 같은
> 이상한 조합이 나옵니다. 통로를 4개만 열어두면 그런 실수가 원천 차단됩니다.

---

## 2. `RequestType` — 요청 23종과 payload 표

| RequestType | 요청 payload | 응답 payload | 락 | 비고 |
|---|---|---|:---:|---|
| `LOGIN` | `LoginRequest` | `model.User` | | 성공 시 `SessionRegistry`에 등록 |
| `REGISTER` | `model.User` | 없음 | ✔ | **`admin` 값은 무시하고 항상 일반 유저로 생성** |
| `LOGOUT` | 없음 | 없음 | | 세션 해제 |
| `USER_UPDATE` | `model.User`(수정 반영본) | 없음 | ✔ | 관리자 전용. 학과/기숙사/비번만 반영 |
| `USER_LOOKUP` | `String`(학번) | `model.User` | | 관리자 전용. 조회 전용 |
| `POST_LIST` | `String`(boardKey) | `List<model.boards.Post>` | | 조회 전용. 게시판별로 필터링됨(§2.1) |
| `POST_CREATE` | `PostCreateOrUpdateRequest` | `model.boards.Post`(저장 결과) | ✔ | |
| `POST_UPDATE` | `PostCreateOrUpdateRequest` | 없음 | ✔ | 댓글 목록은 서버 것이 정본 |
| `POST_DELETE` | `PostDeleteRequest` | 없음 | ✔ | |
| `COMMENT_ADD` | `CommentAddRequest` | 없음 | ✔ | 민원+관리자면 자동 `markAnswered()` |
| `COMMENT_DELETE` | `CommentDeleteRequest` | 없음 | ✔ | |
| `FILE_UPLOAD` | `FileTransfer` | `String`(서버 저장 경로) | | 첨부 등록. 게시글 저장 **전에** 먼저 보냄(§2.3) |
| `FILE_DOWNLOAD` | `String`(저장 경로) | `FileTransfer` | | 첨부 열람/저장 |
| `CHATROOM_CREATE` | `model.ChatRoom`(초기값) | `model.ChatRoom`(채번 결과) | ✔ | **roomId는 서버가 채번** |
| `CHATROOM_JOIN_REQUEST` | `ChatRoomJoinRequest` | 없음 | ✔ | |
| `CHATROOM_JOIN_APPROVE` | `ChatRoomJoinDecision` | 없음 | ✔ | 방장 전용 |
| `CHATROOM_JOIN_REJECT` | `ChatRoomJoinDecision` | 없음 | ✔ | 방장 전용 |
| `CHAT_SEND` | `ChatSendRequest` | 없음 | ✔ | 다른 멤버에게 푸시 발생 |
| `CHATROOM_SET_NICKNAME` | `ChatRoomNicknameRequest` | 없음 | ✔ | 참여자 전용(2026-07-23 추가) |
| `CHATROOM_LIST` | 없음 | `List<model.ChatRoom>` | | 조회 전용. 채팅방 탐색/검색 화면용 |
| `CHAT_MESSAGE_PUSH` | — | `ChatPushPayload` | | **서버 전용 푸시** |
| `NOTICE_PUSH` | — | `model.boards.NoticePost` | | **서버 전용 푸시** |
| `DISCONNECT` | 없음 | 없음 | | 연결 종료 통지 (응답 없이 루프 종료) |

"락" 열이 ✔ 인 것이 `ClientHandler.SYNCHRONIZED_TYPES`(13종)입니다.
자세한 내용은 [03_architecture.md §5](03_architecture.md)를 보세요.

### 2.1 `POST_LIST`의 게시판별 응답 차이

같은 요청이지만 게시판에 따라 서버가 다르게 응답합니다:

| 대상 | 응답 |
|---|---|
| `NoticeBoard` | `getVisiblePosts(currentUser)` — **내게 해당하는 공지만** |
| `ComplaintBoard` + 일반 유저 | `getPostsByAuthor(내 학번)` — **내가 넣은 민원만** |
| `ComplaintBoard` + 관리자 | 전체 민원 |
| 그 외 | `canAccess` 검사 후 전체 목록 |

그래서 "내 문의 내역" 화면과 관리자 "민원함"이 **같은 화면을 재사용**할 수 있습니다.

### 2.2 서버가 클라이언트 값을 믿지 않는 지점

| 요청 | 무시/검증하는 값 |
|---|---|
| `REGISTER` | `admin` — 무조건 `false`로 덮어씀 |
| `POST_CREATE` | `authorId`가 로그인 세션과 다르면 거부 / 게시판에 맞는 게시글 타입인지 검사 / 중복 id 거부 |
| `COMMENT_ADD` | `authorId`가 로그인 세션과 다르면 거부 |
| `CHATROOM_CREATE` | `roomId` — 서버가 다시 채번 |
| `USER_UPDATE` | `id`, `admin` — `final`이라 반영 불가 |
| `POST_UPDATE` | `comments` — 서버 것이 정본이라 통째로 갈아끼우지 않음 |
| `FILE_UPLOAD` | 파일명 — 경로 성분(`../`)과 특수문자를 걸러내고 앞에 UUID를 붙여 다시 지음 |
| `FILE_DOWNLOAD` | 경로 — 파일명만 취해 `server/data/files` 안으로 한정 (다른 파일 열람 방지) |

### 2.3 첨부파일/이미지 주고받기

`Post.filePath` / `imagePath`에 들어가는 값은 **서버 기준 저장 경로**이지 클라이언트 PC의 경로가
아닙니다. 클라이언트가 고른 파일을 서버가 직접 읽을 수는 없으므로, 순서가 정해져 있습니다:

```
① 파일 선택  → FILE_UPLOAD (FileTransfer: 원본 파일명 + byte[])
② 응답으로 "server/data/files/<UUID>_<원본이름>" 을 받음
③ 그 문자열을 Post.filePath / imagePath 에 넣어 POST_CREATE / POST_UPDATE
④ 읽는 쪽은 그 경로로 FILE_DOWNLOAD → byte[] 를 이미지로 표시하거나 파일로 저장
```

- **게시글 저장이 첨부를 옮기지 않습니다.** 반드시 ①을 먼저 끝내고 ③을 보냅니다.
- 게시글 목록(`POST_LIST`)에는 경로만 실려 옵니다 — 목록을 열 때마다 5MB짜리 첨부가
  전부 따라오지 않게 하기 위해서입니다. 내용은 필요한 화면에서 ④로 따로 받습니다.
- 크기 제한은 **5MB**(`FileTransfer.MAX_BYTES`)이고 클라이언트·서버 양쪽에서 검사합니다.
- 저장 이름 앞에 UUID를 붙이는 이유는 서로 다른 사람이 같은 이름(`사진.png`)을 올려도
  덮어쓰지 않게 하기 위해서입니다. 사용자에게 보여줄 때는 첫 `_` 뒤의 원본 이름만 씁니다.
- 파일명에 `|` `^` `;` 같은 `DataFormat` 구분자가 들어가면 이 경로를 담은 게시글 `.dat`
  한 줄이 통째로 깨지므로, 서버가 영숫자/`_`/`.`/`-`/한글만 남기고 전부 `_`로 바꿉니다.
- `FILE_UPLOAD`는 `SYNCHRONIZED_TYPES`에 **넣지 않았습니다** — 매번 새 이름으로만 저장해
  서로 겹치지 않고 메모리 상의 게시판/유저도 건드리지 않는데, 5MB 쓰기를 락 안에서 하면
  그동안 다른 모든 CUD가 멈추기 때문입니다.
- 게시글을 지워도 첨부 파일은 남습니다(고아 파일). 이번 범위에서는 문제되지 않지만,
  정리 기능이 필요하면 `POST_DELETE` 쪽에 추가하면 됩니다.

> 클라이언트에서는 `client/CT/FileTransferClient`(왕복 헬퍼)와
> `client/GUI/AttachmentPicker`(에디터 공용 첨부 위젯)를 쓰세요. 패널마다 `Packet`을
> 직접 조립하지 않습니다.

---

## 3. payload DTO 11종 (`model/protocol/`)

전부 `Serializable` + 불변입니다.

| 클래스 | 필드 | 사용 RequestType |
|---|---|---|
| `LoginRequest` | `id`, `password` | `LOGIN` |
| `PostCreateOrUpdateRequest` | `boardKey`, `post: Post` | `POST_CREATE`, `POST_UPDATE` |
| `PostDeleteRequest` | `boardKey`, `postId` | `POST_DELETE` |
| `CommentAddRequest` | `boardKey`, `postId`, `comment: Comment` | `COMMENT_ADD` |
| `CommentDeleteRequest` | `boardKey`, `postId`, `commentIndex: int` | `COMMENT_DELETE` |
| `ChatRoomJoinRequest` | `roomId`, `message` | `CHATROOM_JOIN_REQUEST` |
| `ChatRoomJoinDecision` | `roomId`, `userId` | `CHATROOM_JOIN_APPROVE`, `CHATROOM_JOIN_REJECT` |
| `ChatSendRequest` | `roomId`, `content` | `CHAT_SEND` |
| `ChatRoomNicknameRequest` | `roomId`, `nickname` | `CHATROOM_SET_NICKNAME` |
| `ChatPushPayload` | `roomId`, `chat: Chat` | `CHAT_MESSAGE_PUSH` (서버 푸시) |
| `FileTransfer` | `fileName`, `data: byte[]` | `FILE_UPLOAD`(요청), `FILE_DOWNLOAD`(응답) |

`CommentDeleteRequest.commentIndex`는 해당 게시글 `comments` 리스트 내 위치(**0부터**)입니다.
목록이 그 사이 바뀌면 엉뚱한 댓글이 지워질 수 있으므로, 삭제 직후에는 목록을 다시 조회하는 것이 안전합니다.

---

## 4. 게시글 id 채번 규칙 (확정)

**`UUID.randomUUID().toString()`** 을 씁니다.

여러 클라이언트가 각자 만들어도 겹치지 않아야 하는데, 클라이언트는 서버의 기존 id 목록을
모르기 때문입니다. 서버는 중복 id를 거부하므로 만에 하나 겹치면 저장 자체가 실패합니다.

**모든 에디터(`PostEditorPanel`, `GroupBuyPostEditorPanel`, `NoticePostEditorPanel`,
`ComplaintPanel`)가 같은 규칙을 씁니다.**

채팅방 id는 예외로 **서버가 채번**합니다 (`001`, `002`, ... 3자리 0채움).
`server/data/chatrooms/001.dat` 파일명과 그대로 대응됩니다.

---

## 5. 연결 초기화 순서 (반드시 지킬 것)

`ObjectOutputStream`은 생성 시 스트림 헤더를 씁니다. 상대방의 `ObjectInputStream` 생성자는
그 헤더를 읽을 때까지 **블로킹**됩니다. 그래서 **양쪽 모두** 아래 순서를 지켜야 합니다:

```java
out = new ObjectOutputStream(socket.getOutputStream());
out.flush();                                    // ← 이게 없으면 데드락
in  = new ObjectInputStream(socket.getInputStream());
```

순서가 어긋나면 양쪽 다 상대방을 기다리며 영원히 멈춥니다.
`ClientHandler.run()`과 `ServerConnection` 생성자가 이미 이 순서로 되어 있으니
**이 부분은 수정하지 마세요.**

---

## 6. 전송 시 `reset()` 규칙

같은 `ObjectOutputStream`으로 여러 번 `writeObject()`를 호출하면, 자바 직렬화는 이미 보낸
객체를 캐시해뒀다가 **참조만** 보냅니다. 그러면 그 객체가 그 사이 바뀌어도 **예전 상태가
그대로 나갑니다** (게시글을 수정했는데 옛 내용이 가는 식).

`ClientHandler.sendPacket`과 `ServerConnection`의 전송 헬퍼는 매번 `writeObject()` 뒤에
`out.reset()`을 호출해 이 캐시를 비웁니다.

> **새 로직을 작성할 때 별도의 전송 코드를 만들지 말고 이 헬퍼를 그대로 쓰세요.**

`sendPacket`은 `synchronized`이기도 합니다 — 응답 스레드와 푸시 스레드가 같은 스트림에
동시에 쓰는 것을 막기 위해서입니다.

---

## 7. 새 `RequestType`을 추가하는 절차

빠뜨리기 쉬운 순서라 그대로 따르세요.

1. `model/protocol/RequestType.java`에 상수 추가 + **javadoc에 요청/응답 payload 타입 명시**
2. 필요하면 `model/protocol/`에 payload DTO 추가 (`Serializable` + 불변)
3. `ClientHandler.dispatch()`에 `case` 추가
4. `ClientHandler`에 `handleXxx(Packet)` 작성
   — 시작은 `requireLogin()` 또는 `requireAdmin()`, 실패는 한국어 메시지로 `throw`
5. **데이터를 바꾸는 요청이면 `SYNCHRONIZED_TYPES`에 추가**
6. **이 문서 §2 표에 한 줄 추가**
7. 클라이언트에서 호출 (`Packet.request(...)` → `sendRequest`)

1~6번을 같은 커밋에서 함께 하세요.
