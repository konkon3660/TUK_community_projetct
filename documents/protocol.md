# 클라이언트-서버 통신 프로토콜 명세서 (확정판)

이 문서는 `documents/datastruct.md`와 같은 성격입니다 — 통신 코드를 AI에게 맡길 때 그대로
붙여넣는 기준 문서입니다. **`model/protocol/`, `server/CT/`, `client/CT/`에는 이미 스켈레톤
코드가 있습니다 (컴파일 확인 완료). AI에게는 새로 설계하게 하지 말고, `server/CT/ClientHandler.java`의
`handleXxx` 메서드 본문만 채우도록 지시하세요.**

---

## 0. 구조 요약

* 클라이언트-서버는 소켓 하나를 계속 열어두는 **영속적 연결** 위에서 `Packet` 객체를
  `ObjectOutputStream`/`ObjectInputStream`으로 직렬화해 주고받습니다.
* `model/`(`User`, `Chat`, `ChatRoom`)과 `model/boards/`(`Post`와 하위 클래스, `Comment`)의
  엔티티는 모두 `Serializable`이라 `Packet.payload`에 그대로 담아 보낼 수 있습니다.
* 프로토콜 클래스(`Packet`, `RequestType`, DTO들)는 `model/protocol/` 패키지 하나에만 있고
  `client/CT`, `server/CT` 양쪽에서 그대로 참조합니다 (빌드 시스템 없이 단일 컴파일 단위).

---

## 1. `model/protocol/Packet.java`

생성자를 직접 쓰지 않고 아래 정적 팩토리 4개로만 만듭니다.

| 팩토리 | 사용 시점 |
|---|---|
| `Packet.request(type, payload)` | 클라이언트가 요청 보낼 때 (`requestId` 자동 채번) |
| `Packet.success(request, payload)` | 서버가 성공 응답 보낼 때 (원래 요청의 `requestId`를 echo) |
| `Packet.error(request, message)` | 서버가 실패 응답 보낼 때 |
| `Packet.push(type, payload)` | 서버가 요청 없이 먼저 보낼 때 (`requestId`가 없음 → `isPush()`가 true) |

---

## 2. `model/protocol/RequestType.java` — 요청/응답 payload 표

| RequestType | 요청 payload | 응답 payload | 비고 |
|---|---|---|---|
| `LOGIN` | `LoginRequest` | `model.User` | |
| `REGISTER` | `model.User` | 없음 | |
| `LOGOUT` | 없음 | 없음 | |
| `USER_UPDATE` | `model.User`(수정 반영본) | 없음 | 관리자 전용 |
| `POST_LIST` | `String`(boardKey) | `List<model.boards.Post>` | 조회 전용 — synchronized 안 함 |
| `POST_CREATE` | `PostCreateOrUpdateRequest` | `model.boards.Post`(저장 결과) | |
| `POST_UPDATE` | `PostCreateOrUpdateRequest` | 없음 | |
| `POST_DELETE` | `PostDeleteRequest` | 없음 | |
| `COMMENT_ADD` | `CommentAddRequest` | 없음 | |
| `COMMENT_DELETE` | `CommentDeleteRequest` | 없음 | |
| `CHATROOM_CREATE` | `model.ChatRoom`(초기값) | `model.ChatRoom`(roomId 채번 결과) | |
| `CHATROOM_JOIN_REQUEST` | `ChatRoomJoinRequest` | 없음 | |
| `CHATROOM_JOIN_APPROVE` | `ChatRoomJoinDecision` | 없음 | 방장 전용 |
| `CHATROOM_JOIN_REJECT` | `ChatRoomJoinDecision` | 없음 | 방장 전용 |
| `CHAT_SEND` | `ChatSendRequest` | 없음 | |
| `CHATROOM_LIST` | 없음 | `List<model.ChatRoom>` | 조회 전용 — synchronized 안 함. GUI의 채팅방 탐색/검색 화면에서 사용 |
| `CHAT_MESSAGE_PUSH` | — | `ChatPushPayload` | 서버 전용 푸시 |
| `NOTICE_PUSH` | — | `model.boards.NoticePost` | 서버 전용 푸시 |
| `DISCONNECT` | 없음 | 없음 | 연결 종료 통지 |

`boardKey`는 `model/protocol/BoardKey.java`에 고정된 문자열 상수입니다(`FreeBoard`류는
`BoardKey.*`, `DepartmentBoard`는 학과명을 그대로 사용). 실제 게시판 레지스트리는
`server/board/DataStore.java`(`getBoard(boardKey)`)에 있습니다.

`COMMENT_DELETE`의 `commentIndex`는 해당 게시글 `comments` 리스트 내 위치(0부터)입니다.

---

## 3. 동기화 정책

`server/CT/ClientHandler.java`의 `SYNCHRONIZED_TYPES`에 있는 타입(회원가입, 회원정보 수정,
게시글/댓글 CUD, 채팅방 생성·가입·채팅 전송)은 정적 락 `DATA_LOCK`으로 감싸서 처리합니다.
`POST_LIST`처럼 순수 조회인 타입은 감싸지 않습니다. 새 RequestType을 추가할 때는 이 목록에도
반영하세요.

---

## 4. 연결 초기화 순서 (반드시 지킬 것)

`ObjectOutputStream`은 생성 시 스트림 헤더를 씁니다. 상대방의 `ObjectInputStream` 생성자는 그
헤더를 읽을 때까지 블로킹됩니다. 그래서 **양쪽 모두** `ObjectOutputStream`을 먼저 만들고
`flush()`를 호출한 뒤에 `ObjectInputStream`을 만들어야 합니다 — 순서가 어긋나면 양쪽 다
상대방을 기다리며 멈추는 데드락이 발생합니다. `ClientHandler.run()`과
`ServerConnection`의 생성자가 이미 이 순서로 구현되어 있으니, 이 부분은 수정하지 않습니다.

---

## 5. 패킷 전송 시 `reset()` 규칙

같은 `ObjectOutputStream`으로 여러 번 `writeObject()`를 호출하면, 자바 직렬화는 이미 보낸
객체를 캐시해뒀다가 참조만 보낼 수 있습니다. `ClientHandler.sendPacket`과
`ServerConnection`의 전송 헬퍼는 매번 `writeObject()` 뒤에 `out.reset()`을 호출해서 이 캐시를
비웁니다 — 새 handleXxx 로직을 작성할 때 별도의 전송 코드를 새로 만들지 말고 이 헬퍼를
그대로 쓰세요.

---

## 6. 남은 작업

* `server/CT/ClientHandler.java`의 `handleXxx` 메서드 16개(`CHATROOM_LIST` 추가로 15→16) —
  전부 `TODO: 구현 필요` 상태. `dataStore` 필드(`server/board/DataStore`)로 게시판/유저/채팅방을
  찾고, `model.User`, `model.boards.*`, `model.ChatRoom`의 기존 메서드를 그대로 호출해서 채우면 됨.
  `handleChatRoomList`는 `dataStore.getAllChatRooms()`를 그대로 반환하면 됨.
* 다른 클라이언트에게 푸시를 뿌리는 로직(`ClientHandler.sendPacket`은 있지만 "다른 클라이언트의
  ClientHandler 인스턴스를 어떻게 찾을지"는 아직 없음 — 접속 중인 ClientHandler 목록을
  관리하는 부분은 이번 스켈레톤 범위 밖).
* `client/GUI`는 `client/CT/ServerConnection`을 사용해서 요청을 보내고 `PushListener`로 실시간
  갱신을 받습니다 — `documents/gui.md`와 `client/GUI/LoginPanel.java` 참고.
