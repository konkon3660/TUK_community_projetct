# 프로젝트 구조 안내서 (개발 처음인 팀원용)

이 문서는 **코드를 아직 잘 모르는 팀원이 프로젝트 전체가 어떻게 굴러가는지 이해하는 것**을
목표로 합니다. 용어는 최대한 풀어 썼고, 파일 하나하나가 무슨 일을 하는지 표로 정리했습니다.

- 이 문서: **"전체가 어떻게 돌아가는가"** (지금 읽는 문서)
- `documents/datastruct.md`: 데이터가 어떤 모양인지 (필드 목록, 저장 형식)
- `documents/protocol.md`: 서버-클라이언트가 주고받는 메시지 규격
- `documents/class_dragrm.md`: 클래스 다이어그램 그릴 때 쓰는 정리본

> 기준 시점: 2026년 7월 23일 / 전체 Java 파일 61개 / **컴파일 정상 (Java 21에서 에러 0개 확인)**
> GUI 화면 14개 전부 클래스로 만들어져 있음(레이아웃/일부 로직만 TODO) — `documents/gui.md` §3 참고

---

## 1. 한 문장 요약

**"서버 컴퓨터 1대가 모든 글과 채팅을 보관하고, 학생들의 프로그램(클라이언트) 여러 개가
인터넷으로 서버에 접속해서 글을 읽고 쓰는 프로그램"** 입니다.

### 비유로 이해하기

식당으로 비유하면:

| 프로젝트 | 식당 비유 |
|---|---|
| **서버** (`server/`) | 주방. 재료(데이터)를 전부 보관하고, 주문이 오면 요리해서 내보냄 |
| **클라이언트** (`client/`) | 손님 테이블. 메뉴판을 보고 주문하고, 나온 음식을 먹음 |
| **프로토콜** (`model/protocol/`) | 주문서 양식. "무엇을 주문하는지" 적는 정해진 종이 |
| **모델** (`model/`) | 재료 자체. 김치, 고기 같은 것 (= 회원, 게시글, 댓글, 채팅) |
| **.dat 파일** (`server/data/`) | 창고. 주방이 문을 닫아도 재료가 남아있게 보관하는 곳 |

중요한 점: **손님은 창고에 직접 못 들어갑니다.** 학생 프로그램(클라이언트)은 `.dat` 파일을
직접 열지 않고, 반드시 서버에게 "이거 주세요"라고 부탁해야 합니다. 그래야 여러 명이 동시에
써도 데이터가 꼬이지 않습니다.

---

## 2. 폴더 구조

```
Java_summer_p/
│
├── model/                    ← [공통] 서버와 클라이언트 양쪽이 다 쓰는 코드
│   ├── User.java                 회원 1명
│   ├── Chat.java                 채팅 메시지 1개
│   ├── ChatRoom.java             채팅방 1개
│   ├── DataFormat.java           파일 저장 규칙 (구분자 기호)
│   ├── FileStorage.java          파일 읽기/쓰기 담당
│   │
│   ├── boards/               ← 게시판과 게시글
│   │   ├── Post.java             게시글 1개 (기본형)
│   │   ├── GroupBuyPost.java     공동구매 게시글
│   │   ├── NoticePost.java       공지 게시글
│   │   ├── ComplaintPost.java    민원 게시글
│   │   ├── Comment.java          댓글 1개
│   │   ├── Board.java            게시판 설계도 (인터페이스)
│   │   ├── AbstractBoard.java    게시판 공통 기능
│   │   └── FreeBoard / GroupBuyBoard / DepartmentBoard
│   │       / DormBoard / NoticeBoard / ComplaintBoard.java
│   │
│   └── protocol/             ← 서버-클라이언트 대화 규격
│       ├── Packet.java           주고받는 메시지 봉투
│       ├── RequestType.java      요청 종류 19가지 목록
│       ├── ResponseStatus.java   성공/실패
│       ├── BoardKey.java         게시판 이름표 상수
│       └── (요청서 9종)          LoginRequest, PostDeleteRequest 등
│
├── server/                   ← [서버 전용] 혼자 돌아가는 프로그램
│   ├── CT/
│   │   ├── ServerMain.java       ★ 서버 시작 지점 (여기서 실행)
│   │   └── ClientHandler.java    접속한 학생 1명을 담당
│   ├── board/
│   │   └── DataStore.java        모든 데이터를 메모리에 올려두는 창고 관리인
│   └── data/                 ← 실제 저장 파일들
│       ├── users.dat             회원 목록
│       ├── boards/*.dat          게시판별 글 목록
│       └── chatrooms/*.dat       채팅방별 기록
│
├── client/                   ← [클라이언트 전용] 학생들이 쓰는 프로그램
│   ├── CT/
│   │   ├── ServerConnection.java 서버와 전화선 연결
│   │   └── PushListener.java     서버가 먼저 말 걸 때 받는 귀
│   ├── GUI/                  ← 화면 14개 전부 클래스로 존재(레이아웃/일부 로직 TODO)
│   │   ├── MainFrame.java        ★ 클라이언트 시작 지점 (창 + 화면 전환 + 로그인 세션 보관)
│   │   ├── LoginPanel.java       로그인 (다른 화면 만들 때 참고용 예시)
│   │   ├── RegisterPanel.java    회원가입
│   │   ├── HomePanel.java        로그인 후 허브(게시판/채팅/추천/관리자 네비게이션)
│   │   ├── PostListPanel.java    게시글 목록 (모든 게시판 공용)
│   │   ├── PostDetailPanel.java  게시글 상세 + 댓글
│   │   ├── PostEditorPanel.java  글쓰기/수정 (자유·학과·기숙사 게시판용)
│   │   ├── GroupBuyPostEditorPanel.java  공동구매 글쓰기/수정
│   │   ├── NoticePostEditorPanel.java    공지 작성/수정 (관리자 전용)
│   │   ├── ComplaintPanel.java   민원 작성 + 문의 내역 진입
│   │   ├── ChatRoomListPanel.java  채팅방 탐색
│   │   ├── ChatRoomCreatePanel.java  채팅방 생성
│   │   ├── ChatRoomPanel.java    채팅방 (송수신 + 가입 승인/거절)
│   │   ├── UserEditPanel.java    관리자: 회원정보 수정
│   │   ├── AdminPanel.java       관리자 허브
│   │   └── RecommendPanel.java   추천 3종 탭
│   ├── recomment_system/     ← 추천 기능 3종 (서버 필요 없음, 파일만 읽음)
│   │   ├── activity/             할 거 추천기
│   │   ├── book/                 책 추천기
│   │   └── menu/                 메뉴 추천기
│   └── recommend_data/*.dat      추천 기능이 읽는 데이터
│
└── documents/                ← 설계 문서들 (지금 보는 곳)
```

### 왜 `model/`은 양쪽이 같이 쓰나요?

서버가 "게시글"을 보낼 때, 클라이언트도 "게시글"이 뭔지 알아야 받을 수 있습니다.
서로 같은 `Post.java`를 보고 있어야 말이 통합니다. 그래서 공통 재료는 `model/`에 두고
서버 폴더/클라이언트 폴더 양쪽에서 가져다 씁니다.

---

## 3. 프로그램이 실제로 도는 순서

### 3-1. 서버를 켰을 때

```
ServerMain.main() 실행
  ↓
new DataStore()  ← 창고 관리인 생성
  ↓  registerBoards() : 게시판 8개를 만들어 이름표를 붙여 등록
  ↓                     ("free", "groupbuy", "dorm", "notice", "complaint", 학과 3개)
  ↓  loadAll()     : .dat 파일들을 전부 읽어서 메모리에 올림
  ↓
포트 5000번을 열고 대기
  ↓
학생이 접속할 때마다 → new ClientHandler(소켓, dataStore) → 새 스레드로 실행
```

**"스레드"란?** 여러 일을 동시에 처리하기 위한 일꾼입니다. 학생 30명이 동시에 접속하면
일꾼 30명이 각자 1명씩 담당합니다. 그래야 한 명이 느려도 나머지가 안 막힙니다.

### 3-2. 학생이 로그인 버튼을 눌렀을 때 (전체 흐름)

이 흐름 하나만 이해하면 나머지 기능도 다 똑같은 구조입니다.

```
[클라이언트]                              [서버]

LoginPanel.attemptLogin()
  아이디/비번 칸에서 글자 꺼냄
       ↓
  LoginRequest(id, password) 만듦        ← "주문서" 작성
       ↓
  Packet.request(LOGIN, 주문서)          ← 주문서를 "봉투"에 담음
       ↓
  connection.sendRequest(봉투)  ─────인터넷─────→  ClientHandler.run()이 봉투 받음
       ↓                                              ↓
  (응답 올 때까지 기다림)                        dispatch() : LOGIN이네?
                                                      ↓
                                                 handleLogin()
                                                      ↓
                                                 dataStore.getUser(id) 로 회원 찾고
                                                 비밀번호 맞는지 확인
                                                      ↓
                                                 Packet.success(원래봉투, User)
       ↓                                              ↓
  응답 봉투 받음        ←─────인터넷─────────────  sendPacket(응답)
       ↓
  status == OK 면 다음 화면으로 전환
  status == ERROR 면 "로그인 실패" 팝업
```

핵심: **클라이언트는 절대 직접 `users.dat`을 열지 않습니다.** 항상 서버에게 물어봅니다.

### 3-3. 채팅처럼 "서버가 먼저 말 거는" 경우

보통은 클라이언트가 물어보고 서버가 답합니다. 그런데 채팅은 반대가 필요합니다 —
**다른 사람이 채팅을 쳤을 때 내 화면에 자동으로 떠야** 하니까요.

이걸 **푸시(push)** 라고 부릅니다. 카톡 알림과 같은 개념입니다.

```
A학생이 채팅 전송  →  서버가 받음  →  같은 방에 있는 B, C 학생에게 서버가 먼저 보냄
                                          ↓
                                  B의 PushListener.onPush()가 받아서 화면에 추가
```

봉투(`Packet`)에 `requestId`가 없으면 = 아무도 안 물어봤는데 서버가 먼저 보낸 것 =
푸시입니다. 이걸 `Packet.isPush()`로 구분합니다.

---

## 4. 파일별 상세 설명

### 4-1. `model/` — 공통 데이터

| 파일 | 하는 일 | 주요 메서드 |
|---|---|---|
| **User** | 회원 1명의 정보 (학번, 학과, 기숙사 여부, 비번, 관리자 여부) | `isAdmin()` 관리자인지 / `isDormitory()` 기숙사생인지 / `setDepartment()` 전과 처리 (관리자만 호출해야 함) |
| **Chat** | 채팅 메시지 1개 (보낸 사람, 시각, 내용) | 값만 담는 상자. 만들면 못 바꿈 |
| **ChatRoom** | 채팅방 1개 (참여자, 채팅 기록, 각종 가입 제한) | `canJoin()` 가입 자격 확인 / `requestJoin()` 가입 신청 / `approveJoin()` 방장이 수락 / `sendChat()` 채팅 추가 / `setNickname()` 방 안에서 쓸 이름 |
| **DataFormat** | 파일에 저장할 때 쓰는 **구분 기호 모음**. 값만 들어있고 동작은 없음 | `FIELD_DELIM = "\|"`, `LIST_DELIM = ","`, 날짜 형식 등 |
| **FileStorage** | 파일 읽기/쓰기 담당. **모든 파일 입출력은 여기만 통과** | `readLines()` 파일 → 줄 목록 / `writeLines()` 줄 목록 → 파일 |

> **왜 FileStorage 하나로 몰았나요?** 각자 파일 다루는 코드를 짜면 사람마다 방식이 달라져서
> 나중에 저장 형식을 바꿀 때 전부 찾아 고쳐야 합니다. 한 곳으로 모으면 한 번만 고치면 됩니다.

### 4-2. `model/boards/` — 게시판과 게시글

**게시글 계열**

| 파일 | 하는 일 |
|---|---|
| **Post** | 게시글 1개. 제목/글쓴이/내용/첨부파일/이미지/댓글목록/작성시각. **자유·학과·기숙사 게시판은 이 클래스를 그대로 씁니다** |
| **GroupBuyPost** | Post + `최대인원`, `연결된 채팅방 id`, `해시태그들` |
| **NoticePost** | Post + `대상 학과들`(비면 전체 공지), `기숙사 공지 여부` |
| **ComplaintPost** | Post + `문의 1차/2차 카테고리`, `답변완료 여부` |
| **Comment** | 댓글 1개 (글쓴이, 내용, 시각) |

Post의 주요 메서드:

| 메서드 | 하는 일 |
|---|---|
| `canEdit(User)` | 이 사람이 **수정** 가능? → 관리자이거나 본인 글이면 `true`. GUI의 수정 버튼을 켤지 끌지 판단할 때 씀 |
| `canDelete(User)` | 이 사람이 **삭제** 가능? → 위와 같은 조건 |
| `addComment(Comment)` | 댓글 추가 |
| `removeComment(Comment, User)` | 권한 확인 후 댓글 삭제. 권한 없으면 에러 발생 |
| `toDataString()` | 이 게시글을 **파일에 저장할 한 줄 글자**로 변환 |
| `fromDataString(String)` | 반대로, 파일에서 읽은 한 줄을 **게시글 객체로 복원** |

> **`toDataString` / `fromDataString`이 뭔가요?**
> 컴퓨터 메모리 안의 "객체"는 파일에 그대로 못 넣습니다. 그래서 글자로 바꿔서 저장하고,
> 읽을 때 다시 객체로 되돌립니다. 예:
> ```
> 저장:  p001|중고책 팝니다|2026591007|경영학원론 팔아요||||2026-07-23-13:31:31|
> 복원:  Post 객체 (id=p001, title=중고책 팝니다, ...)
> ```
> `|` 기호로 칸을 나눈 게 보이시죠? 그 기호가 `DataFormat`에 정의된 구분자입니다.

**게시판 계열**

| 파일 | 접근 가능한 사람 | 특별한 기능 |
|---|---|---|
| **Board** | (설계도) | "게시판이라면 이런 기능이 있어야 한다"는 목록만 정의 |
| **AbstractBoard** | (공통 기능) | 글 목록 관리, 삭제 권한 확인, 파일 로드/저장을 여기서 한 번에 구현 |
| **FreeBoard** | 전체 | |
| **GroupBuyBoard** | 전체 | `filterByHashtag()` 해시태그로 걸러내기 |
| **DepartmentBoard** | 관리자 + 해당 학과 학생 | 학과마다 인스턴스 1개씩 |
| **DormBoard** | 관리자 + 기숙사생 | |
| **NoticeBoard** | 조회는 전체 / 작성은 관리자만 | `canWrite()` 글쓰기 권한, `getVisiblePosts()` 내 학과 공지만 |
| **ComplaintBoard** | 관리자만 | `getPostsByAuthor()` 일반 학생이 본인 민원만 볼 때 |

AbstractBoard의 주요 메서드:

| 메서드 | 하는 일 |
|---|---|
| `getPosts()` | 글 목록 전체 반환 |
| `addPost(Post)` | 글 추가 |
| `removePost(id, User)` | 글을 찾아서 **삭제 권한 확인 후** 삭제. 권한 없으면 에러 |
| `canAccess(User)` | 이 사람이 이 게시판에 들어올 수 있나? (게시판마다 다르게 구현) |
| `load()` | `.dat` 파일을 읽어서 글 목록을 채움 (서버 켤 때 1번) |
| `save()` | 현재 글 목록을 `.dat` 파일에 덮어씀 (글이 바뀔 때마다) |

> **상속이 왜 필요했나요?** 게시판이 6종류인데 "글 추가, 글 삭제, 파일 저장"은 전부 똑같습니다.
> 6번 복붙하면 나중에 버그 하나를 6군데 고쳐야 합니다. 그래서 공통 부분은 `AbstractBoard`에
> 한 번만 쓰고, **다른 부분(누가 들어올 수 있는가, 어느 파일에 저장하는가)만** 각자 채웁니다.

### 4-3. `model/protocol/` — 대화 규격

| 파일 | 하는 일 |
|---|---|
| **Packet** | 주고받는 모든 메시지의 **봉투**. 안에 "무슨 요청인지(type)", "내용물(payload)", "성공/실패(status)"가 들어감 |
| **RequestType** | 요청 종류 18가지 목록 (LOGIN, POST_CREATE, CHAT_SEND ...) |
| **ResponseStatus** | `OK` 또는 `ERROR` 둘 중 하나 |
| **BoardKey** | 게시판 이름표 상수 (`"free"`, `"notice"` 등). 오타 방지용 |
| **요청서 9종** | 각 요청에 필요한 정보 묶음 (예: `LoginRequest`는 아이디+비번) |

Packet은 생성자를 막아두고 **4가지 방법으로만** 만들 수 있게 했습니다:

| 만드는 법 | 언제 쓰나 |
|---|---|
| `Packet.request(종류, 내용)` | 클라이언트가 요청 보낼 때 |
| `Packet.success(원래요청, 결과)` | 서버가 "성공했어" 답할 때 |
| `Packet.error(원래요청, 메시지)` | 서버가 "실패했어" 답할 때 |
| `Packet.push(종류, 내용)` | 서버가 먼저 알림 보낼 때 (채팅/공지) |

> **왜 이렇게 막아뒀나요?** 자유롭게 만들게 두면 "성공인데 에러 메시지가 들어있는 봉투" 같은
> 이상한 조합이 나옵니다. 4개 통로만 열어두면 그런 실수가 원천 차단됩니다.

### 4-4. `server/` — 서버

| 파일 | 하는 일 | 주요 메서드 |
|---|---|---|
| **ServerMain** | 서버 프로그램의 **시작 지점**. 5000번 포트를 열고 접속을 계속 기다림 | `main()` |
| **ClientHandler** | 접속한 학생 **1명을 전담**. 봉투를 받아서 종류별로 나눠주고 답장 | `run()` 받기 반복 / `dispatch()` 종류별 분배 / `handleXxx()` 실제 처리 15개 / `sendPacket()` 답장 보내기 |
| **DataStore** | 모든 데이터(게시판·회원·채팅방)를 **메모리에 올려두는 창고 관리인** | `getBoard(키)` 게시판 찾기 / `getUser(학번)` 회원 찾기 / `addUser()` 가입 처리+저장 / `getChatRoom()` / `addChatRoom()` / `saveUsers()` |

**ClientHandler의 동시성 처리** — 조금 어렵지만 중요한 부분:

학생 2명이 **똑같은 순간에** 글을 쓰면, 데이터가 섞여서 하나가 사라질 수 있습니다.
그래서 데이터를 **바꾸는** 요청(글쓰기/삭제/가입 등 12종)은 `DATA_LOCK`이라는 자물쇠를
걸어서 **한 번에 한 명씩** 처리합니다. 반대로 그냥 **읽기만** 하는 요청(`POST_LIST`)은
자물쇠를 안 겁니다 — 동시에 읽는 건 문제가 없고, 자물쇠를 걸면 오히려 느려지니까요.

> 새 요청 종류를 추가한다면, 데이터를 바꾸는 요청인지 확인하고 맞으면
> `ClientHandler`의 `SYNCHRONIZED_TYPES` 목록에도 넣어야 합니다.

### 4-5. `client/` — 클라이언트

| 파일 | 하는 일 | 주요 메서드 |
|---|---|---|
| **ServerConnection** | 서버와의 **전화선**. 요청을 보내고 답이 올 때까지 기다려줌 | `sendRequest(봉투)` 보내고 답 받기 (최대 10초 대기) / `setPushListener()` 알림 받을 곳 등록 / `disconnect()` 종료 |
| **PushListener** | 서버가 먼저 보낸 알림을 받는 **귀**. GUI가 이걸 구현해서 등록 | `onPush(봉투)` |
| **MainFrame** | 프로그램 **창** 자체 + 화면 전환 + 로그인 세션 보관. 클라이언트 **시작 지점** | `registerScreen(이름, 화면)` 화면 등록 / `switchTo(이름)` 화면 전환 / `getScreen(이름)` 화면 인스턴스 꺼내기 / `getConnection()` 전화선 공유 / `getCurrentUser()`/`setCurrentUser()` 로그인 세션 |
| **화면 14개** (`LoginPanel` 등) | 로그인/회원가입/게시판 관련 8개/채팅 3개/관리자 2개/추천, 전부 클래스로 존재 | `initLayout()`(화면 배치, 전부 TODO — 자유 영역) / 서버 요청 보내는 메서드(대부분 이미 구현됨) — 화면별 구체적인 TODO 목록은 `documents/gui.md` §3 표 참고 |

> **화면을 새로 만들려면?** `LoginPanel`을 복사해서 이름만 바꾸고, 서버 요청 부분을 원하는
> 요청으로 바꾸면 됩니다. 그리고 `MainFrame.main()`에서 `registerScreen()`으로 등록하세요.
> 다른 화면 데이터를 넘겨야 하면(예: 어떤 게시글을 보여줄지) `mainFrame.getScreen("이름")`으로
> 그 화면을 꺼내 `open(...)`을 먼저 부른 뒤 `switchTo`합니다(`documents/gui.md` §2). 서버와
> 대화하는 방식(봉투 만들기 → `sendRequest` → 성공/실패 확인)은 **모든 화면이 똑같습니다.**

### 4-6. `client/recomment_system/` — 추천 기능 3종

이 3개는 **서버가 필요 없습니다.** 내 컴퓨터의 파일만 읽어서 보여주면 끝이라
가장 쉬운 부분입니다.

| 기능 | 파일 | 하는 일 |
|---|---|---|
| **책 추천** | `BookRecommendation` 책 1권 정보<br>`BookRecommender` | `recommendForDepartment(학과)` 그 학과 책 중 랜덤 1권. **완성됨** |
| **메뉴 추천** | `MenuOption` 식당+메뉴 한 쌍<br>`MenuRecommender` | `todayMenuText()` 오늘 학식 / `recommendTwoRandom()` 랜덤 2개 / `randomTip()` 꿀팁 한 줄. **완성됨** |
| **할 거 추천** | `Activity` 할 거 1개<br>`TimetableEntry` 시간표 한 칸<br>`ClassPeriod` 교시 1개<br>`ClassPeriodTable` 교시 시각 고정표<br>`ActivityRecommender` | `loadActivities()` / `loadTimetable()` 완성됨<br>`recommendNow()` 지금 공강 시간에 맞는 할 거 추천 — **미완성** |

---

## 5. 데이터는 어떻게 저장되나요?

DB(데이터베이스) 없이 **텍스트 파일**에 저장합니다. 한 줄 = 물건 하나입니다.

| 파일 | 들어있는 것 |
|---|---|
| `server/data/users.dat` | 회원 전체 (한 줄 = 회원 1명) |
| `server/data/boards/free_board.dat` | 자유게시판 글 (한 줄 = 글 1개) |
| `server/data/boards/class_boards/*.dat` | 학과별 게시판 (학과마다 파일 1개) |
| `server/data/chatrooms/001.dat` | 채팅방 1개 (파일 1개 = 방 1개) |
| `client/recommend_data/*.dat` | 추천 데이터 (서버 아님, 클라이언트가 직접 읽음) |

저장 예시 (회원 1명):
```
2026591007|AI소프트웨어학과|true|pass01|false
   학번        학과       기숙사 비번   관리자
```

구분 기호는 4가지를 씁니다:

| 기호 | 용도 | 예 |
|---|---|---|
| `\|` | 칸과 칸 사이 | `학번\|학과\|비번` |
| `,` | 목록 안의 항목들 | `기숙사,음식,생필품` |
| `^` | 댓글/채팅 하나의 내부 | `글쓴이^내용^시각` |
| `;` | 댓글/채팅 여러 개 사이 | `댓글1;댓글2;댓글3` |

> ⚠️ **주의:** 글 내용에 `|` 를 넣으면 저장이 깨집니다. 나중에 "사용자가 특수문자를 입력했을 때"
> 처리하는 작업이 필요합니다. (아래 8장 참고)

**저장 시점**: 서버를 켤 때 파일 → 메모리로 전부 읽고, 글이 추가/수정/삭제될 때마다
메모리 → 파일로 다시 씁니다. 그래서 서버를 껐다 켜도 데이터가 남아있습니다.

---

## 6. 실행 방법

Java 21 기준입니다. 프로젝트 폴더(`Java_summer_p`)에서 실행하세요.

```bash
# 1. 전체 컴파일 (out 폴더에 결과물 생성)
javac -encoding UTF-8 -d out $(find . -name "*.java")

# 2. 서버 먼저 실행 (터미널 1)
java -cp out server.CT.ServerMain

# 3. 클라이언트 실행 (터미널 2, 여러 개 띄워도 됨)
java -cp out client.GUI.MainFrame
```

Windows PowerShell에서는 1번을 이렇게 바꿔주세요:
```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java | % FullName)
```

> 지금 상태에서 서버는 켜지지만, 로그인 화면 배치(`initLayout`)와 서버 처리 로직(`handleXxx`)이
> 아직 비어 있어서 실제로 로그인은 안 됩니다. 아래 7장이 그 목록입니다.

---

## 7. 지금 어디까지 됐나요?

### ✅ 완성된 것 (건드릴 필요 없음)

- **데이터 구조 전체** — 회원, 게시글 4종, 댓글, 게시판 6종, 채팅방
- **파일 저장/복원** — `toDataString` / `fromDataString`, 게시판 `load()` / `save()`
- **권한 판단** — `canEdit`, `canDelete`, `canAccess`, `canWrite`
- **통신 배관** — 소켓 연결, 봉투 주고받기, 요청-응답 짝 맞추기, 동시 접속 처리
- **서버 창고 관리** — `DataStore`의 게시판 등록 및 로드
- **책 추천기, 메뉴 추천기** — 완전히 동작
- **GUI 화면 14개 전부** — 클래스/필드/버튼 연결/서버 요청 코드까지 존재(레이아웃과 일부
  화면 갱신 로직만 TODO)

### 🔨 남은 작업

전부 `TODO: 구현 필요` 라고 표시되어 있습니다. 코드에서 이 글자를 검색하면 찾을 수 있습니다.

| 담당 영역 | 위치 | 개수 | 난이도 |
|---|---|---|---|
| **서버 업무 로직** | `server/CT/ClientHandler.java`의 `handleXxx` | 16 | 중 |
| **GUI 화면 배치** | 각 화면의 `initLayout()` (14개 전부) | 14 | 하 |
| **GUI 화면 갱신/네비게이션 세부 로직** | `documents/gui.md` §3 표의 "남은 TODO" 칸 참고 | 다수 | 중 |
| **채팅방 가입 처리** | `model/ChatRoom.java` (`canJoin`, `requestJoin`, `approveJoin`, `rejectJoin`) | 4 | 중 |
| **공지 필터링** | `NoticePost.isVisibleTo`, `NoticeBoard.getVisiblePosts` | 2 | 하 |
| **공동구매** | `GroupBuyPost.getCurrentMemberCount`, `GroupBuyBoard.filterByHashtag` | 2 | 하 |
| **할 거 추천** | `ActivityRecommender.recommendNow` | 1 | 중 |

**`handleXxx` 16개는 대부분 아래 3줄 패턴입니다:**
```java
1. 봉투에서 요청서 꺼내기       →  request.getPayload()
2. DataStore에서 대상 찾기      →  dataStore.getBoard(...) / getUser(...)
3. model의 기존 메서드 호출     →  board.addPost(...) / board.save()
4. 결과를 봉투에 담아 반환      →  Packet.success(request, 결과)
```
즉 **새로 설계할 게 아니라, 이미 만들어둔 메서드를 순서대로 부르기만 하면 됩니다.**

### 📋 아직 설계 안 된 것 (논의 필요)

1. **푸시를 실제로 뿌리는 로직** — `ClientHandler.sendPacket()`으로 보낼 수는 있지만,
   "지금 접속 중인 학생이 누구누구인지" 목록이 없어서 대상을 못 찾음. 접속자 목록을
   관리하는 코드가 추가로 필요함
2. **데이터 파일이 전부 비어 있음** — 테스트하려면 회원, 글, 추천 데이터를 넣어야 함
3. **특수문자 처리** — 글 내용에 `|` `^` `;` 를 쓰면 저장 형식이 깨짐
4. **민원 템플릿 데이터** — "자주 묻는 질문" 탭에 쓸 정적 데이터
5. **게시글/공지 id 채번 규칙** — `PostEditorPanel`류가 새 글을 만들 때 id를 어떻게
   정할지(UUID 등) 아직 미정 (`generateId()` TODO로 표시되어 있음)
6. **학번으로 유저 1명 조회** — 관리자가 회원정보를 수정하려면 학번으로 유저를 찾아야 하는데,
   그런 조회용 `RequestType`이 아직 없음 (`UserEditPanel.search()` TODO로 표시)
7. **공동구매 채팅방 자동 연동** — 공동구매 글을 만들 때 채팅방(`chatRoomId`)을 자동으로
   만들지, 어떻게 연결할지 미정 (`GroupBuyPostEditorPanel.save()` TODO 주석 참고)

---

## 8. 용어 사전

| 용어 | 뜻 |
|---|---|
| **클래스 (class)** | 물건의 설계도. `User` 클래스 = "회원이란 이런 것" |
| **객체 (object) / 인스턴스** | 설계도로 실제로 찍어낸 물건. 회원 100명 = User 객체 100개 |
| **메서드 (method)** | 그 물건이 할 수 있는 동작. "함수"와 거의 같은 말 |
| **필드 / 멤버변수** | 그 물건이 가진 정보. 회원의 학번, 학과 등 |
| **getter / setter** | 필드를 꺼내는 함수(`getId()`) / 바꾸는 함수(`setPassword()`) |
| **상속 (extends)** | 기존 설계도를 물려받아 확장. `GroupBuyPost extends Post` = "게시글인데 추가로 최대인원이 있음" |
| **인터페이스 (interface)** | 동작 목록만 정한 약속. 내용은 각자 채움 |
| **추상 클래스 (abstract)** | 반쯤 완성된 설계도. 그대로는 못 쓰고 물려받아 완성해야 함 |
| **패키지 (package)** | 클래스를 담는 폴더. `model.boards` = `model/boards/` 폴더 |
| **소켓 (socket)** | 두 컴퓨터를 잇는 전화선 |
| **직렬화 (Serializable)** | 객체를 전선으로 보낼 수 있게 납작하게 만드는 것 |
| **스레드 (thread)** | 동시에 일하는 일꾼 |
| **동기화 (synchronized)** | 한 번에 한 명만 들어가게 문을 잠그는 것 |
| **푸시 (push)** | 요청하지 않았는데 서버가 먼저 보내는 알림 |
| **payload** | 봉투 안의 실제 내용물 |
| **TODO** | "여기 아직 안 만들었음"이라는 표시 |
