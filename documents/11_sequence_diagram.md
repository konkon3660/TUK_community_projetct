# 11. 시퀀스 다이어그램 작성용 정리

제출용 시퀀스 다이어그램(draw.io, StarUML 등)을 그릴 때 쓰는 정리본입니다. Mermaid는
시퀀스 다이어그램을 정식으로 지원하므로 아래 초안은 거의 그대로 옮겨 그릴 수 있습니다.
참여 객체 이름은 실제 클래스명을 그대로 썼습니다 ([03_architecture.md](03_architecture.md),
[05_protocol.md](05_protocol.md) 기준).

**공통 규약(모든 다이어그램에 적용, 반복 표기 생략):**

- 클라이언트 → 서버 화살표는 실제로는 `ServerConnection.sendRequest(Packet)` 한 번
  호출이지만, 가독성을 위해 "패널 → ClientHandler"로 직접 그렸습니다.
- 요청/응답은 전부 `Packet`으로 감싸져 있습니다 (`Packet.request` / `Packet.success` /
  `Packet.error`). 매 메시지마다 "Packet(...)" 이라고 다 적으면 지저분해지므로 페이로드
  이름만 표기했습니다.
- 데이터를 바꾸는 요청(✔ 표시)은 서버 쪽에서 `synchronized(DATA_LOCK)` 블록 안에서
  처리됩니다 ([03_architecture.md §5](03_architecture.md)). 다이어그램에 `activate`/`note`로
  한 번만 표시해도 충분합니다.

---

## 1. 로그인 ([03_architecture.md §3](03_architecture.md) 원문의 정식 다이어그램화)

```mermaid
sequenceDiagram
    actor U as 사용자
    participant LP as LoginPanel
    participant CH as ClientHandler
    participant DS as DataStore
    participant SR as SessionRegistry

    U->>LP: 학번/비밀번호 입력, 로그인 클릭
    LP->>CH: Packet.request(LOGIN, LoginRequest)
    CH->>DS: getUser(id)
    DS-->>CH: User
    CH->>CH: 비밀번호 대조
    alt 일치
        CH->>SR: register(id, this)
        CH-->>LP: Packet.success(User)
        LP->>LP: setCurrentUser(user)
        alt user.isAdmin()
            LP->>LP: switchTo("admin")
        else
            LP->>LP: switchTo("home")
        end
    else 불일치/미가입
        CH-->>LP: Packet.error("아이디 또는 비밀번호가 올바르지 않습니다")
        LP->>U: 에러 팝업
    end
```

---

## 2. 게시글 작성 (이미지 첨부 포함) ([05_protocol.md §2.3](05_protocol.md))

첨부가 있는 경우 `FILE_UPLOAD`가 `POST_CREATE`보다 **먼저** 끝나야 합니다.

```mermaid
sequenceDiagram
    actor U as 학생
    participant EP as PostEditorPanel
    participant CH as ClientHandler
    participant DS as DataStore
    participant B as Board(boardKey)

    U->>EP: 제목/내용 입력 + 이미지 선택
    EP->>CH: Packet.request(FILE_UPLOAD, FileTransfer)
    CH->>CH: 5MB 검사, 파일명 정제 + UUID 접두
    CH-->>EP: Packet.success("server/data/files/<UUID>_원본이름")
    EP->>EP: post.imagePath ← 받은 경로
    EP->>CH: Packet.request(POST_CREATE, PostCreateOrUpdateRequest)
    activate CH
    note right of CH: synchronized(DATA_LOCK)
    CH->>CH: authorId == 세션 학번? 게시판에 맞는 타입인가?
    CH->>DS: getBoard(boardKey)
    DS-->>CH: Board
    CH->>B: addPost(post)
    B->>B: save() — 게시판 파일 전체 덮어쓰기
    CH-->>EP: Packet.success(저장된 Post)
    deactivate CH
    EP->>EP: PostListPanel.refresh() 후 switchTo("postList")
```

---

## 3. 채팅방 가입 신청 → 승인 ([02_requirements.md §4.2](02_requirements.md))

```mermaid
sequenceDiagram
    actor A as 신청자
    actor B as 방장
    participant CLP as ChatRoomListPanel
    participant CRP as ChatRoomPanel(방장 화면)
    participant CH as ClientHandler
    participant CR as ChatRoom

    A->>CLP: "가입 신청" + 가입지원 메시지
    CLP->>CH: Packet.request(CHATROOM_JOIN_REQUEST, ChatRoomJoinRequest)
    activate CH
    CH->>CR: canJoin(user) — 정원/입학년도/학과/기숙사 검사
    alt 조건 불충족
        CH-->>CLP: Packet.error("가입 조건에 맞지 않습니다")
    else 조건 충족
        CH->>CR: requestJoin(user, message)
        CR->>CR: pendingJoinRequests에 추가
        CH-->>CLP: Packet.success()
    end
    deactivate CH

    B->>CRP: 신청 목록에서 확인
    B->>CRP: "승인" 클릭
    CRP->>CH: Packet.request(CHATROOM_JOIN_APPROVE, ChatRoomJoinDecision)
    activate CH
    CH->>CR: approveJoin(userId)
    CR->>CR: memberIds에 추가, pendingJoinRequests에서 제거
    CH->>CH: dataStore.saveChatRoom(room)
    CH-->>CRP: Packet.success()
    deactivate CH
    CRP->>A: (다음 목록 조회 시) 참여자로 표시됨
```

---

## 4. 실시간 채팅 전송 + 푸시 ([03_architecture.md §4](03_architecture.md) 원문의 정식 다이어그램화)

```mermaid
sequenceDiagram
    actor A as A학생
    participant CRPa as ChatRoomPanel(A)
    participant CHa as ClientHandler(A 담당)
    participant CR as ChatRoom
    participant SR as SessionRegistry
    participant CHb as ClientHandler(B 담당)
    participant CRPb as ChatRoomPanel(B)
    actor B as B학생(같은 방, 접속 중)

    A->>CRPa: 메시지 입력, 전송
    CRPa->>CHa: Packet.request(CHAT_SEND, ChatSendRequest)
    activate CHa
    CHa->>CHa: 참여자인지 확인
    CHa->>CR: sendChat(new Chat(A, 내용, now))
    CR->>CR: chats에 추가, 파일 저장
    CHa-->>CRPa: Packet.success()
    CHa->>SR: sendTo(B의 학번, Packet.push(CHAT_MESSAGE_PUSH, ChatPushPayload))
    deactivate CHa
    SR->>CHb: (B를 담당하는 핸들러를 찾아) sendPacket(푸시)
    CHb->>CRPb: onPush(packet) — 네트워크 스레드
    CRPb->>CRPb: SwingUtilities.invokeLater(화면 갱신)
    CRPb-->>B: 새 메시지 표시
```

---

## 5. 공지 작성 + 대상자 실시간 푸시 ([02_requirements.md §3.4](02_requirements.md))

```mermaid
sequenceDiagram
    actor Adm as 관리자
    participant NEP as NoticePostEditorPanel
    participant CH as ClientHandler
    participant NB as NoticeBoard
    participant SR as SessionRegistry
    actor S as 대상 학생(접속 중)
    participant HP as HomePanel/MainFrame(S)

    Adm->>NEP: 대상 학과(복수)/기숙사 여부 지정 + 작성
    NEP->>CH: Packet.request(POST_CREATE, PostCreateOrUpdateRequest(NoticePost))
    activate CH
    CH->>CH: requireAdmin()
    CH->>NB: addPost(noticePost) → save()
    CH-->>NEP: Packet.success(저장된 Post)
    CH->>SR: sendToAll(Packet.push(NOTICE_PUSH, noticePost), isVisibleTo 필터)
    deactivate CH
    SR->>HP: 대상자만 골라 sendPacket(푸시)
    HP->>HP: onPush(packet) → SwingUtilities.invokeLater
    HP-->>S: 공지 알림 표시
```

---

## 6. 민원 접수 및 관리자 답변 ([02_requirements.md §3.5](02_requirements.md))

```mermaid
sequenceDiagram
    actor S as 학생
    participant CP as ComplaintPanel
    participant CH as ClientHandler
    participant CB as ComplaintBoard
    actor Adm as 관리자
    participant PDP as PostDetailPanel(민원함)

    S->>CP: (선택)템플릿 불러오기 → 이어서 작성
    CP->>CH: Packet.request(POST_CREATE, PostCreateOrUpdateRequest(ComplaintPost, answered=false))
    CH->>CB: addPost(post) → save()
    CH-->>CP: Packet.success(저장된 Post)

    Adm->>PDP: Packet.request(POST_LIST, "complaint") → 전체 민원 조회
    PDP-->>Adm: 목록 표시(답변대기: 회색)
    Adm->>PDP: 민원 열람 후 답변 작성
    PDP->>CH: Packet.request(COMMENT_ADD, CommentAddRequest(comment))
    activate CH
    CH->>CH: authorId == 관리자 세션 확인
    CH->>CB: post.addComment(comment)
    CH->>CH: 작성자가 관리자 → post.markAnswered()
    CH-->>PDP: Packet.success()
    deactivate CH
    PDP->>PDP: 상태 표시를 답변완료(파란색)로 갱신
```
