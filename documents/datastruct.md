# 팀 프로젝트 데이터 모델 명세서 (확정판)

이 문서는 팀원들이 AI에게 "이 명세대로 구현해줘"라고 그대로 붙여넣는 기준 문서입니다.
필드 타입, 메서드 시그니처, 파일 저장 포맷은 여기 적힌 대로 고정하며, 실제 스켈레톤 코드는
`model/`(공용 엔티티: User/Chat/ChatRoom/DataFormat/FileStorage)과
`model/boards/`(게시판·게시글: Post/Comment/Board 계열) 패키지에 이미 생성되어 있습니다
(컴파일 확인 완료). **AI에게 새 구현을 맡길 때는 새로 설계하게 하지 말고, `model/`이나
`model/boards/` 아래 해당 클래스를 열어서 `TODO: 구현 필요`라고 표시된 메서드 본문만 채우도록
지시하세요.**

---

## 0. 이전 버전과 달라진 점

* 기존 `interface/` 폴더는 `package interface;`로 되어 있었는데, `interface`는 Java 예약어라
  **컴파일이 되지 않는 상태**였습니다. `model/` 패키지로 교체했습니다.
* 기존 문서는 "게시판(Board)"과 "게시글"을 같은 뜻으로 혼용해서 썼습니다. 아래처럼 분리했습니다.
  * **게시글 = `Post`** (자유/공동구매/공지/민원 글 하나하나)
  * **게시판 = `Board`** (게시글 목록을 들고 있고, 접근 권한을 판단하는 컨테이너)
* 게시판/게시글 관련 클래스(`Post`, `Comment`, `Board`, `AbstractBoard`, 6개 게시판 구현체,
  3개 Post 하위 클래스)는 전부 `model.boards` 패키지로 옮겼습니다. `User`/`Chat`/`ChatRoom`/
  `DataFormat`/`FileStorage`는 계속 `model` 패키지에 있습니다.
* `Post`는 더 이상 `abstract`가 아닙니다 — 자유/학과별/기숙사 게시글처럼 추가 필드가 없는
  경우 `Post`를 직접 인스턴스화합니다.

---

## 1. 데이터 저장 포맷 (모든 클래스 공통, `model/DataFormat.java`)

DB 없이 파일(`.dat`)로 저장합니다. 프로그램 시작 시 파일을 읽어 메모리에 올리고, 생성/수정/삭제 시
다시 파일에 씁니다. 파일 입출력은 `model/FileStorage.java`(`readLines`/`writeLines`)만 사용합니다.

한 줄 = 객체 하나. 필드는 아래 4단계 구분자로 인코딩합니다. **이 구분자 외의 다른 구분자를
임의로 쓰지 않습니다.**

| 용도 | 구분자 | 예시 |
|---|---|---|
| 필드와 필드 사이 | `\|` | `id\|title\|content` |
| 리스트 필드 내부 원소 (해시태그, 학과 목록 등) | `,` | `기숙사,음식` |
| 중첩 객체(댓글/채팅) 1개의 내부 필드 | `^` | `authorId^content^createdAt` |
| 중첩 객체가 여러 개일 때 그 사이 | `;` | `댓글1;댓글2` |
| Map 필드의 key:value (채팅방 가입신청 등) | `:` | `userId:message` |

* 날짜/시간: `yyyy-MM-dd-HH:mm:ss` (예: `2026-07-23-13:31:31`)
* boolean: 저장은 `true`/`false` 문자열 (회원가입 화면 등 UI에서 O/X로 보여주는 것은 표시 방식일 뿐,
  모델/파일 계층에서는 O/X를 쓰지 않음)
* 값이 없는 필드(첨부파일 없음 등)는 빈 문자열로 남겨둠

각 클래스는 `toDataString()` / `fromDataString(String)`을 제공합니다. `Post`의 하위 클래스들은
공통 필드를 `Post.baseDataString()`으로 만들고 그 뒤에 자기 필드를 이어붙이는 방식이라, 한 줄의
필드 순서는 항상 "공통 필드 8개 → 하위 클래스 고유 필드"입니다.

---

## 2. 회원 (`model/User.java`)

| 필드 | 타입 | 설명 |
|---|---|---|
| id | String | 학번, 예: `2026591007` |
| department | String | 학과명 |
| dormitory | boolean | 기숙사생 여부 |
| password | String | 비밀번호 |
| admin | boolean | 관리자 여부 |

* 이미 등록된 학번으로 재가입 불가 (가입 로직에서 체크, User 클래스 자체 책임 아님)
* `department`, `dormitory`, `password`는 setter 있음 — **관리자만 호출**해야 함 (전과/기숙사
  입퇴실/비밀번호 재설정). 권한 검증은 이 setter를 호출하는 쪽(서비스 로직)의 책임.

---

## 3. 게시글 (`model/boards/Post.java` 및 하위 클래스)

### 3.1 공통 필드 (`Post`, 일반 클래스 — 추가 필드 없는 게시글은 직접 인스턴스화)

| 필드 | 타입 |
|---|---|
| id | String |
| title | String |
| authorId | String |
| content | String |
| filePath | String (nullable, 최대 5MB) |
| imagePath | String (nullable) |
| comments | List\<Comment\> |
| createdAt | LocalDateTime |

공통 권한 규칙 (모든 게시글 타입 동일):
* `canEdit(User)` / `canDelete(User)`: 관리자이거나 본인 글일 때만 true

### 3.2 댓글 (`model/boards/Comment.java`)

| 필드 | 타입 |
|---|---|
| authorId | String |
| content | String |
| createdAt | LocalDateTime |

`canDelete(User)`: 관리자이거나 본인 댓글일 때만 true

### 3.3 게시글 타입별 추가 필드

* **자유 게시글**: 추가 필드 없음 → `Post`를 그대로 사용 (별도 서브클래스 없음)
* **공동구매 게시글** (`model/boards/GroupBuyPost.java extends Post`)

  | 필드 | 타입 | 비고 |
  |---|---|---|
  | maxMembers | int | 최대 인원 수 |
  | chatRoomId | String | 자동 생성되어 연결된 채팅방 id |
  | hashtags | List\<String\> | 예: 기숙사, 대량구매, 음식, 생필품 |

  현재 인원수는 `getCurrentMemberCount(ChatRoom)`로 연결된 채팅방의 인원수를 그대로 반영해야 함
  (별도 카운터를 두지 않음).

* **공지 게시글** (`model/boards/NoticePost.java extends Post`)

  | 필드 | 타입 | 비고 |
  |---|---|---|
  | targetDepartments | List\<String\> | 비어있으면 전체 공지 |
  | dormNotice | boolean | 기숙사 공지 여부 |

  작성은 관리자만 (`NoticeBoard.canWrite`), 조회는 `isVisibleTo(User)`로 대상 학과/기숙사 여부 필터링.

* **민원 게시글** (`model/boards/ComplaintPost.java extends Post`)

  | 필드 | 타입 | 비고 |
  |---|---|---|
  | category1 | String | 문의 1차 카테고리 |
  | category2 | String | 문의 2차 카테고리 |
  | answered | boolean | 답변 대기(false, 회색) / 답변완료(true, 파란색) |

  관리자가 `addComment()`로 답변을 등록할 때 반드시 `markAnswered()`도 함께 호출해서 상태를
  동기화해야 함. 자주 묻는 질문 탭의 민원 템플릿은 별도 정적 데이터(이 클래스 범위 밖).

---

## 4. 게시판 (`model/boards/Board.java` 인터페이스 + `model/boards/AbstractBoard.java` + 구현체)

`Board`: `getPosts()`, `addPost(Post)`, `removePost(String, User)`, `canAccess(User)`,
`getDataFilePath()`, `load()`, `save()`. 공통 동작(목록 관리, 파일 로드/저장)은 `AbstractBoard`가
구현하므로, 새 게시판 타입을 추가할 때는 `canAccess`/`getDataFilePath`/`parsePost`(자기 타입에
맞는 `fromDataString` 호출 한 줄)만 채우면 됩니다.

| 게시판 클래스 | 접근 조건 | 데이터 파일 |
|---|---|---|
| `FreeBoard` | 전체 허용 | `server/data/boards/free_board.dat` |
| `GroupBuyBoard` | 전체 허용 (해시태그 필터링: `filterByHashtag`) | `server/data/boards/group_buying_board.dat` |
| `DepartmentBoard` | 관리자 또는 해당 학과 유저만 (`restrictedDepartment` 생성자 주입) | `server/data/boards/class_boards/*.dat` (학과별 1개씩 인스턴스화) |
| `DormBoard` | 관리자 또는 기숙사생만 | `server/data/boards/dormitory_board.dat` |
| `NoticeBoard` | 조회는 전체 허용, 작성은 관리자만(`canWrite`) | `server/data/boards/notice_board.dat` |
| `ComplaintBoard` | 관리자만 (`canAccess`). 일반 사용자는 `getPostsByAuthor(String)`로 본인 민원만 조회 | `server/data/boards/complaint_board.dat` (신규 생성) |

### boardKey

통신 프로토콜에서 게시판을 가리킬 때 쓰는 문자열은 `model/protocol/BoardKey.java`에 고정되어
있습니다: `BoardKey.FREE`, `GROUP_BUY`, `DORM`, `NOTICE`, `COMPLAINT`. 학과별 게시판은 별도
상수 없이 `User.getDepartment()`와 정확히 같은 문자열을 boardKey로 씁니다.

### 서버 레지스트리 (`server/board/DataStore.java`)

서버 시작 시 한 번 생성되어 모든 `Board`/`User`/`ChatRoom`을 메모리에 올려두고 파일과 동기화하는
역할을 합니다 (`getBoard(boardKey)`, `getUser(id)`, `getChatRoom(roomId)`, `addUser`,
`addChatRoom`, `saveUsers`, `saveChatRoom` 전부 완전 구현). `server/CT/ClientHandler`가 이
인스턴스를 생성자로 받아 `handleXxx` TODO를 채울 때 사용합니다. 새 학과가 생기면
`DataStore.registerBoards()`에 한 줄만 추가하면 됩니다.

---

## 5. 채팅 (`model/Chat.java`, `model/ChatRoom.java`)

### Chat

| 필드 | 타입 |
|---|---|
| senderId | String |
| sentAt | LocalDateTime |
| content | String |

### ChatRoom

| 필드 | 타입 | 비고 |
|---|---|---|
| roomId | String | |
| ownerId | String | 방장 |
| memberIds | List\<String\> | |
| chats | List\<Chat\> | |
| maxMembers | int | -1 = 무제한 |
| admissionYearLimit | Integer (nullable) | 학번 앞 4자리 제한, null이면 제한 없음 |
| departmentLimit | List\<String\> | 비어있으면 학과 제한 없음 |
| dormOnlyLimit | boolean | 기숙사생 전용 여부 |
| inviteBypassesLimit | boolean | true면 초대로 들어온 사람은 위 제한을 무시 |
| pendingJoinRequests | Map\<String, String\> | userId → 가입지원 메세지 |
| nicknames | Map\<String, String\> | userId → 이 채팅방에서의 프로필 이름 |

가입 절차: `requestJoin(User, message)` → 방장이 `approveJoin(userId)` / `rejectJoin(userId)`.
입장 시 `setNickname(userId, nickname)`으로 프로필 이름 설정.

---

## 6. 추천 기능 (`client/recomment_system/{activity,book,menu}`)

시간표 기반 할 거 추천기 / 메뉴 추천기 / 책 추천기는 서버 통신 없이 `client/recommend_data/*`
파일을 그대로 읽는 로컬 기능입니다. 파일 포맷과 로더 클래스는 이미 스켈레톤이 있습니다
(`ActivityRecommender`/`MenuRecommender`/`BookRecommender`) — 대부분 완전 구현되어 있고,
"지금 이 순간 공강 시간에 맞는 할 거리 추천"(`ActivityRecommender.recommendNow`)만 판단 규칙이
아직 팀 논의가 없어 TODO로 남아 있습니다. (배경은 `documents/project_idea.md`의 07/22 회의록 참고)

---

## 7. 앞으로 남은 설계 범위

* `client/GUI`의 화면 14개는 전부 클래스로 존재함 — 남은 건 `initLayout`(레이아웃, 자유)과
  일부 화면 갱신/네비게이션 로직뿐 (`documents/gui.md` §3 표 참고)
* `server/CT/ClientHandler`의 `handleXxx` TODO 16개 (`CHATROOM_LIST` 추가로 15→16, 이제
  `DataStore`를 참조할 수 있음)
* `ActivityRecommender.recommendNow`의 공강 판단 규칙
* 관리자 세부 기능(게시글 삭제/문의 답변/회원정보 수정/공지 작성 버튼 등)은 위 모델의 메서드를
  그대로 사용하면 되므로 별도 클래스 불필요. UI/서비스 계층에서 `canEdit`/`canDelete`/`canWrite`
  등을 호출해서 버튼 활성화 여부만 판단.
