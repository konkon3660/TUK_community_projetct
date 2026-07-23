# 04. 데이터 모델 명세

**AI에게 구현을 맡길 때 그대로 붙여넣는 기준 문서입니다.** 필드 타입, 메서드 시그니처,
파일 저장 포맷은 여기 적힌 대로 고정합니다.

> **AI에게 새 클래스를 설계하게 하지 마세요.** `model/` 또는 `model/boards/` 아래 해당
> 클래스를 열어서 `TODO: 구현 필요`로 표시된 **메서드 본문만** 채우도록 지시하세요.

---

## 1. 저장 포맷 (`model/DataFormat.java`)

DB 없이 `.dat` 파일로 저장합니다. **한 줄 = 객체 하나.** 파일 입출력은
`model/FileStorage`(`readLines` / `writeLines`)만 사용합니다.

| 용도 | 상수 | 기호 | 예시 |
|---|---|---|---|
| 필드와 필드 사이 | `FIELD_DELIM` | `\|` | `id\|title\|content` |
| 리스트 필드 내부 원소 | `LIST_DELIM` | `,` | `기숙사,음식` |
| 중첩 객체(댓글/채팅) 1개의 내부 필드 | `SUBOBJECT_DELIM` | `^` | `authorId^content^createdAt` |
| 중첩 객체 여러 개 사이 | `SUBLIST_DELIM` | `;` | `댓글1;댓글2` |
| Map 필드의 key:value | `MAP_ENTRY_DELIM` | `:` | `userId:message` |

- 날짜/시간: `yyyy-MM-dd-HH:mm:ss` (예: `2026-07-23-13:31:31`) — `DataFormat.DATETIME_FORMATTER`
- boolean: `true` / `false` 문자열
  (회원가입 화면에서 O/X로 보여주는 것은 **표시 방식일 뿐**, 모델·파일 계층은 O/X를 쓰지 않음)
- 값이 없는 필드(첨부파일 없음 등)는 **빈 문자열**. 복원 시 `emptyToNull()`로 `null`이 됨
- **이 5개 외의 구분자를 임의로 쓰지 않습니다.**

모든 엔티티는 `toDataString()` / `static fromDataString(String)` 한 쌍을 제공합니다.
`split`은 항상 `-1` 리밋으로 호출해서 **뒤쪽 빈 칸이 잘리지 않게** 합니다.

### 1-1. 이스케이프 — `DataFormat.encode()` / `decode()`

사용자가 제목에 `|`를 치거나 본문에서 Enter를 누르면 위 포맷이 그대로 깨집니다.
그래서 **사용자가 입력한 값은 저장할 때 `encode()`, 복원할 때 `decode()`를 반드시 거칩니다.**

| 실제 값 | 저장된 모습 |
|---|---|
| `\|` | `\p` |
| `^` | `\c` |
| `;` | `\s` |
| `,` | `\l` |
| `:` | `\m` |
| 줄바꿈 | `\n` |
| `\` | `\\` |

> **규칙은 하나뿐입니다 — 말단 값에만 씌운다.**
> 저장 포맷은 중첩 구조(게시글 안에 댓글, 채팅방 안에 채팅)라서, 구조를 만드는 `join`이 넣는
> 구분자까지 치환하면 구조 자체가 사라집니다. 반대로 말단에 구분자가 남아있지 않기만 하면
> **모든 단계의 `split`이 자동으로 안전해집니다.**
>
> 그래서 `Post.splitFields()`는 공통 필드 0~5번만 decode하고, **7번(댓글 목록)과 리스트 필드는
> 일부러 그대로 둡니다.** 그 안의 값들은 각자 인코딩되어 있어서, 여기서 미리 풀면
> `Comment.fromDataString`이 `^`로 쪼갤 때 구분자가 되살아나 깨집니다.

새 필드를 추가할 때:

- 사용자가 입력하는 문자열 → `DataFormat.encode(값)` / `DataFormat.decode(f[n])`
- 리스트 필드 → **원소 하나하나를** encode한 뒤 `LIST_DELIM`으로 이어붙임
- Map 필드 → **키와 값 모두** encode
- 숫자·boolean·작성시각 → 그대로 둠 (기계가 만든 값이라 구분자가 들어갈 일이 없고,
  파일을 눈으로 읽을 수 있게 남겨둠)

`decode()`는 역슬래시가 없는 문자열을 그대로 돌려주므로, **이스케이프 도입 전에 손으로 쓴
기존 `.dat` 파일도 변환 없이 그대로 읽힙니다.**

⚠️ `decode()`를 `replace` 체인으로 구현하면 안 됩니다. 사용자가 실제로 입력한 역슬래시
(저장된 모습은 `\\p`)를 `|`로 잘못 복원합니다. 반드시 왼쪽부터 한 번만 훑어야 합니다.

> 추천 데이터(`client/recommend_data/*`)는 앱이 **읽기만 하고 쓰지 않는** 손으로 만든 파일이라
> 이스케이프를 적용하지 않았습니다. 나중에 이 파일들을 앱에서 저장하는 기능이 생기면
> 그때 같은 규칙을 적용해야 합니다.

---

## 2. 회원 — `model/User.java`

| 필드 | 타입 | 가변 | 설명 |
|---|---|---|---|
| `id` | String | `final` | 학번. 예: `2026591007`. 앞 4자리 = 입학년도 |
| `department` | String | setter | 학과명 |
| `dormitory` | boolean | setter | 기숙사생 여부 |
| `password` | String | setter | 비밀번호 |
| `admin` | boolean | `final` | 관리자 여부 |

```
저장: 2026591007|AI소프트웨어학과|true|pass01|false
```

- **`id`와 `admin`은 `final`이라 바꿀 수 없습니다.** 관리자 승격은 `users.dat`을 직접 고쳐야 합니다.
- setter 3개는 **관리자만 호출**해야 합니다 (전과/기숙사 입퇴실/비밀번호 재설정).
  권한 검증은 호출하는 쪽(`ClientHandler.handleUserUpdate`의 `requireAdmin()`) 책임입니다.
- 학번 중복 검사는 `DataStore.hasUser(id)`로 합니다 (`getUser`는 없으면 예외를 던지므로 검사에 쓰지 않음).

---

## 3. 게시글 — `model/boards/Post.java` 계층

### 3.1 `Post` (공통 필드 8개)

**`Post`는 `abstract`가 아닙니다.** 추가 필드가 없는 게시글(자유·학과별·기숙사)은
`Post`를 그대로 인스턴스화해서 씁니다.

| # | 필드 | 타입 | 가변 |
|---|---|---|---|
| 0 | `id` | String | `final` |
| 1 | `title` | String | setter |
| 2 | `authorId` | String | `final` |
| 3 | `content` | String | setter |
| 4 | `filePath` | String (nullable, 파일 최대 5MB) | setter |
| 5 | `imagePath` | String (nullable) | setter |

> 두 경로는 **서버 기준 경로**(`server/data/files/<UUID>_<원본이름>`)이지 클라이언트 PC의
> 경로가 아닙니다. 값을 채우려면 파일을 먼저 `FILE_UPLOAD`로 올려야 하고, 내용을 보려면
> `FILE_DOWNLOAD`로 받아야 합니다 — [05_protocol.md §2.3](05_protocol.md) 참고.
| 6 | `createdAt` | LocalDateTime | `final` |
| 7 | `comments` | List\<Comment\> | 리스트 자체는 `final` |

> ⚠️ **저장 순서와 필드 선언 순서가 다릅니다.** `.dat` 한 줄에서는
> `id|title|authorId|content|filePath|imagePath|createdAt|comments` 순으로,
> **인덱스 6이 `createdAt`, 7이 `comments`** 입니다. 하위 클래스의 고유 필드는 **인덱스 8부터**입니다.

주요 메서드:

| 메서드 | 하는 일 |
|---|---|
| `canEdit(User)` | **본인 글일 때만** `true`. 관리자라도 남의 글은 수정 못 한다 (2026-07-23 기획 변경, [02_requirements.md §2.2](02_requirements.md)) |
| `canDelete(User)` | 관리자이거나 본인 글이면 `true`. **`ComplaintPost`는 이걸 오버라이드해서 관리자도 못 지우게 한다** (§3.3) |
| `addComment(Comment)` | 댓글 추가 |
| `removeComment(Comment, User)` | `canDelete` 확인 후 삭제. 권한 없으면 예외 |
| `toDataString()` | 파일에 저장할 한 줄 문자열로 변환 |
| `static fromDataString(String)` | 한 줄을 `Post` 객체로 복원 |
| `baseDataString()` (protected) | 공통 필드 8개만 이어붙임 — **하위 클래스가 앞부분으로 사용** |
| `splitFields` / `parseComments` / `emptyToNull` (protected static) | 하위 클래스의 복원용 헬퍼 |

하위 클래스 구현 패턴 (반드시 이 형태를 지킬 것):

```java
@Override
public String toDataString() {
    return String.join(DataFormat.FIELD_DELIM, baseDataString(), 내필드1, 내필드2);
}

public static XxxPost fromDataString(String line) {
    String[] f = splitFields(line);
    XxxPost post = new XxxPost(f[0], f[1], f[2], f[3], emptyToNull(f[4]), emptyToNull(f[5]),
            LocalDateTime.parse(f[6], DataFormat.DATETIME_FORMATTER), f[8], f[9]);
    post.getComments().addAll(parseComments(f[7]));
    return post;
}
```

### 3.2 `Comment`

| 필드 | 타입 |
|---|---|
| `authorId` | String |
| `content` | String |
| `createdAt` | LocalDateTime |

- 전부 `final` — 만들면 못 바꿉니다.
- `canDelete(User)`: 관리자이거나 본인 댓글일 때만 `true`
- 저장: `글쓴이^내용^시각` (`SUBOBJECT_DELIM`), 여러 개는 `;`(`SUBLIST_DELIM`)로 이어붙임

### 3.3 게시글 타입별 추가 필드

| 게시글 | 클래스 | 인덱스 8~ |
|---|---|---|
| 자유 / 학과 / 기숙사 | `Post` **(서브클래스 없음)** | — |
| 공동구매 | `GroupBuyPost extends Post` | 8=`maxMembers`, 9=`chatRoomId`, 10=`hashtags` |
| 공지 | `NoticePost extends Post` | 8=`targetDepartments`, 9=`dormNotice` |
| 민원 | `ComplaintPost extends Post` | 8=`category1`, 9=`category2`, 10=`answered` |

**`GroupBuyPost`**

| 필드 | 타입 | 비고 |
|---|---|---|
| `maxMembers` | int | 최대 인원. setter 있음 |
| `chatRoomId` | String | 연결된 채팅방 id. setter 있음 |
| `hashtags` | List\<String\> | 예: 기숙사, 대량구매, 음식, 생필품 |

- 현재 인원수는 별도 카운터를 두지 않고 `getCurrentMemberCount(ChatRoom linkedRoom)`이
  **연결된 채팅방의 참여자 수를 그대로 반영**해야 합니다.

**`NoticePost`**

| 필드 | 타입 | 비고 |
|---|---|---|
| `targetDepartments` | List\<String\> | **비어 있으면 전체 공지** |
| `dormNotice` | boolean | 기숙사 공지 여부 |

- `isVisibleTo(User)` 판정 규칙:
  1. 관리자면 무조건 `true`
  2. 기숙사 공지인데 기숙사생이 아니면 `false`
  3. 대상 학과가 비어 있으면 `true`, 아니면 그 목록에 유저 학과가 있어야 `true`

**`ComplaintPost`**

| 필드 | 타입 | 비고 |
|---|---|---|
| `category1` | String | 문의 1차 카테고리 |
| `category2` | String | 문의 2차 카테고리 |
| `answered` | boolean | 답변 여부. `markAnswered()`로 켠다 |

- **`canEdit(User)` / `canDelete(User)`를 오버라이드해서 관리자 예외를 없앱니다** — 둘 다
  **작성자 본인일 때만** `true`입니다. 관리자가 민원에 할 수 있는 것은 댓글로 답변하는 것뿐이고,
  민원은 사용자↔관리자 1:1 기록이라 접수 원문이 그대로 남아야 합니다
  ([02_requirements.md §3.5](02_requirements.md)).
- 관리자가 민원에 댓글을 달면 서버가 `markAnswered()`를 불러 답변 완료로 바꿉니다.

**`ComplaintPost`**

| 필드 | 타입 | 비고 |
|---|---|---|
| `category1` | String | 문의 1차 카테고리. `final` |
| `category2` | String | 문의 2차 카테고리. `final` |
| `answered` | boolean | 답변 대기(false, 회색) / 답변완료(true, 파란색) |

- 생성 시 `answered`는 항상 `false`입니다.
- 관리자가 답변 댓글을 달면 **`addComment()`와 `markAnswered()`를 함께 호출**해서 상태를
  동기화해야 합니다. 서버가 `handleCommentAdd`에서 자동으로 처리합니다.
- 자주 묻는 질문 탭의 민원 템플릿은 별도 정적 데이터입니다 (이 클래스 범위 밖).

---

## 4. 게시판 — `model/boards/Board.java` 계층

### 4.1 인터페이스와 공통 구현

`Board` 인터페이스: `getPosts()`, `addPost(Post)`, `removePost(String, User)`,
`canAccess(User)`, `getDataFilePath()`, `load()`, `save()`

`AbstractBoard`가 목록 관리·삭제 권한 검사·파일 로드/저장을 전부 구현합니다.
**새 게시판 타입을 추가할 때 채울 것은 3개뿐입니다:**

| 메서드 | 채울 내용 |
|---|---|
| `canAccess(User)` | 이 게시판에 들어올 수 있는 조건 |
| `getDataFilePath()` | `.dat` 파일 경로 (프로젝트 최상위 기준 상대경로) |
| `parsePost(String)` | 자기 타입의 `fromDataString` 호출 **한 줄** |

`AbstractBoard`가 제공하는 것:

| 메서드 | 하는 일 |
|---|---|
| `getPosts()` | 글 목록 전체 (원본 리스트 — 서버는 응답 시 복사해서 보냄) |
| `addPost(Post)` | 글 추가 |
| `removePost(id, User)` | 글을 찾아 **`canDelete` 확인 후** 삭제. 권한 없으면 예외 |
| `findPost(id)` (protected) | `Optional<Post>` 반환 |
| `load()` | `.dat`을 읽어 목록을 채움 (기존 목록은 비우고 다시 채움) |
| `save()` | 현재 목록을 `.dat`에 **통째로 덮어씀** |

> **상속이 왜 필요했나?** 게시판이 6종류인데 "글 추가/삭제/파일 저장"은 전부 같습니다.
> 6번 복붙하면 버그 하나를 6군데 고쳐야 합니다.

### 4.2 게시판 구현체 6종

| 클래스 | `canAccess` 조건 | 데이터 파일 | 추가 메서드 |
|---|---|---|---|
| `FreeBoard` | 전체 허용 | `server/data/boards/free_board.dat` | |
| `GroupBuyBoard` | 전체 허용 | `server/data/boards/group_buying_board.dat` | `filterByHashtag(String)` |
| `DepartmentBoard` | 관리자 또는 해당 학과 | `server/data/boards/class_boards/*.dat` | 생성자로 `restrictedDepartment`·경로 주입 |
| `DormBoard` | 관리자 또는 기숙사생 | `server/data/boards/dormitory_board.dat` | |
| `NoticeBoard` | 전체 허용(조회) | `server/data/boards/notice_board.dat` | `canWrite(User)` 관리자만, `getVisiblePosts(User)` |
| `ComplaintBoard` | **관리자만** | `server/data/boards/complaint_board.dat` | `getPostsByAuthor(String)` |

> `ComplaintBoard.canAccess`는 **"전체 민원함 열람"** 기준입니다. 민원 *접수*는 누구나 할 수
> 있으므로 `handlePostCreate`는 민원 게시판에 `canAccess`를 적용하지 않습니다.
> 일반 유저의 "내 문의 내역"은 `getPostsByAuthor()`로 처리합니다.

### 4.3 boardKey

통신에서 게시판을 가리키는 문자열입니다. `model/protocol/BoardKey.java`에 고정:

```java
BoardKey.FREE      = "free"
BoardKey.GROUP_BUY = "groupbuy"
BoardKey.DORM      = "dorm"
BoardKey.NOTICE    = "notice"
BoardKey.COMPLAINT = "complaint"
```

**학과별 게시판은 별도 상수가 없습니다.** `User.getDepartment()`와 **정확히 같은 문자열**을
boardKey로 씁니다 (학과명은 코드가 아니라 데이터이므로).

### 4.4 서버 레지스트리 — `server/board/DataStore.java`

서버 시작 시 하나만 생성되어 모든 `Board`/`User`/`ChatRoom`을 메모리에 올려두고 파일과
동기화합니다. **권한 검사 등 업무 로직은 넣지 않습니다** — `model/`의 기존 메서드를 씁니다.

| 메서드 | 하는 일 |
|---|---|
| `getBoard(boardKey)` | 없으면 `NoSuchElementException` |
| `getUser(id)` | 없으면 `NoSuchElementException` |
| `hasUser(id)` | 중복 검사용 (예외 없이 boolean) |
| `addUser(User)` | 추가 + 즉시 `saveUsers()` |
| `saveUsers()` | `users.dat` 덮어쓰기 |
| `getChatRoom(roomId)` | 없으면 `NoSuchElementException` |
| `addChatRoom(ChatRoom)` | 추가 + 즉시 저장 |
| `getAllChatRooms()` | 복사본 리스트 반환 |
| `saveChatRoom(ChatRoom)` | 그 방의 파일 하나만 덮어쓰기 |

> **학과 게시판은 손으로 등록하지 않습니다(2026-07-23부터).** `registerBoards()`가
> `AcademicStructure.COLLEGES`를 순회하며 리프(학과/전공) 전체를 자동으로 등록합니다 — 파일
> 경로는 `server/data/boards/class_boards/<학과명>.dat`로 통일되어 있어 이름만 보고 유추할
> 수 있습니다. **새 학과가 생기면 `AcademicStructure.COLLEGES`에 한 줄만 추가하면 됩니다**
> (`DataStore`는 건드릴 필요 없음). §7을 보세요.

---

## 5. 채팅 — `model/Chat.java`, `model/ChatRoom.java`

### 5.1 `Chat`

| 필드 | 타입 |
|---|---|
| `senderId` | String |
| `sentAt` | LocalDateTime |
| `content` | String |

전부 `final`. 저장: `보낸사람^시각^내용` (`SUBOBJECT_DELIM`)

> ⚠️ `Comment`는 `글쓴이^내용^시각`, `Chat`은 `보낸사람^시각^내용` 으로 **가운데 두 항목의
> 순서가 서로 다릅니다.** 파싱 코드를 복사할 때 주의하세요.

### 5.2 `ChatRoom`

| # | 필드 | 타입 | 비고 |
|---|---|---|---|
| 0 | `roomId` | String | `final`. 서버가 채번 (`001`, `002`, ...) |
| 1 | `ownerId` | String | `final`. 방장 |
| 2 | `maxMembers` | int | **`-1` = 무제한** |
| 3 | `admissionYearLimit` | Integer (nullable) | 학번 앞 4자리 제한. `null`이면 제한 없음 |
| 4 | `departmentLimit` | List\<String\> | 비어 있으면 학과 제한 없음 |
| 5 | `dormOnlyLimit` | boolean | 기숙사생 전용 여부 |
| 6 | `inviteBypassesLimit` | boolean | true면 초대로 들어온 사람은 위 제한 무시 |
| 7 | `memberIds` | List\<String\> | 참여자 학번 |
| 8 | `chats` | List\<Chat\> | 채팅 기록 |
| 9 | `pendingJoinRequests` | Map\<String, String\> | userId → 가입지원 메세지 (`LinkedHashMap` = 신청 순서 유지) |
| 10 | `nicknames` | Map\<String, String\> | userId → 이 방에서의 프로필 이름 |
| 11 | `name` | String | 검색용 방 이름(2026-07-23 추가). 비어 있어도 됨. 공동구매 글에 딸린 방은 글 제목이 자동으로 들어감 |

가입 절차와 규칙:

| 메서드 | 규칙 |
|---|---|
| `canJoin(User)` | 이미 멤버면 `false` / 정원 찼으면 `false` / 입학년도·학과·기숙사 제한 검사 |
| `requestJoin(User, message)` | 이미 멤버거나 `canJoin`이 `false`면 **예외**. 통과하면 신청 목록에 등록 |
| `approveJoin(userId)` | 신청 내역이 없으면 예외, 정원 찼으면 예외. 통과하면 신청 제거 + 멤버 추가 |
| `rejectJoin(userId)` | 신청 내역이 없으면 예외. 통과하면 신청만 제거 |
| `setNickname(userId, nickname)` / `getNickname(userId)` | 방 안에서 쓸 프로필 이름 |
| `sendChat(Chat)` | 기록에 추가 (권한 검사는 서버가 함) |

- **정원 검사를 신청 시점과 승인 시점 양쪽에서 합니다.** 신청 후 승인 전에 방이 찰 수 있기 때문입니다.
- 학번이 4자리 미만이거나 숫자가 아니면 입학년도 제한을 **불통과**로 봅니다.
- `inviteBypassesLimit`은 **값만 보관하고 판정에는 쓰지 않습니다** — 초대 기능 자체가 아직 없습니다.

---

## 6. 추천 데이터 — `client/recomment_system/`

서버 통신 없이 `client/recommend_data/*` 파일을 그대로 읽는 로컬 기능입니다.

| 기능 | 데이터 파일 | 한 줄 형식 | 로더 |
|---|---|---|---|
| 할 거 | `active_recommend.dat` | `내용\|소요분` (예: `노래방에 가기\|30`) | `ActivityRecommender.loadActivities()` |
| 시간표 | `time_table.dat` | `요일\|교시\|과목명` (요일 1=월 ~ 5=금) | `ActivityRecommender.loadTimetable()` |
| 메뉴 | `menu_recomend.dat` | `식당\|메뉴` | `MenuRecommender.loadOptions()` |
| 오늘 학식 | `e_resterant_menu.txt` | 한 줄 = 화면 한 줄 (그대로 출력) | `MenuRecommender.todayMenuText()` |
| 꿀팁 | `tip_under_menu.txt` | 한 줄 = 팁 하나 | `MenuRecommender.randomTip()` |
| 책 | `book_recommend.dat` | `학과\|책이름\|설명` | `BookRecommender.loadAll()` |

교시 시각은 `ClassPeriodTable.PERIODS`에 고정되어 있습니다 —
**1교시 09:30부터 60분 단위로 9교시(17:30)까지.** 실제 시간표에 맞게 값만 조정하고
**구조는 바꾸지 마세요.**

| 클래스 | 필드 |
|---|---|
| `Activity` | `content`, `durationMinutes` |
| `TimetableEntry` | `dayOfWeek`(1=월~5=금), `periodIndex`, `className` |
| `ClassPeriod` | 교시 번호, 시작 시각, 지속 분 |
| `MenuOption` | `restaurant`, `menuName` |
| `BookRecommendation` | `department`, `title`, `description` |

---

## 7. 학과 조직도 — `model/AcademicStructure.java` (2026-07-23 추가)

`User.department`(회원가입/회원정보수정)와 `NoticePost.targetDepartments`(공지 대상학과)에
들어가는 학과 이름은 이제 **자유 텍스트가 아니라 이 클래스가 정의한 트리에서만** 고를 수
있습니다. 화면 3곳(`RegisterPanel`, `UserEditPanel`, `NoticePostEditorPanel`)이 전부
`client/GUI/DepartmentPickerPanel`(단과대→학부→학과 3단 연동 `JComboBox`)을 재사용합니다.

- **단과대(`College`) → 학부(`Division`) → 학과(`Department`)** 3단 고정 트리.
  `AcademicStructure.COLLEGES`가 단일 출처입니다.
- 세부 전공 없이 그 자체가 학과인 학부(예: 자유전공학부)나, 단과대 밑에 학부 없이 바로
  달린 학과(예: AI융합대학의 게임공학과)도 **"학부 이름 = 학과 이름"인 리프 하나짜리
  `Division`** 으로 표현해서 화면의 3단 드롭다운이 항상 같은 구조로 동작합니다
  (`Division.leaf(name)`).
- 자유전공학부·경영학부·디자인공학부는 소속 단과대가 없어서 `AcademicStructure.NO_COLLEGE`
  ("단과대 미지정")라는 가짜 단과대 아래에 묶여 있습니다.
- `AcademicStructure.locate(departmentName)`으로 리프 이름 → 소속 단과대/학부를 역으로
  찾을 수 있습니다 (`UserEditPanel`이 기존 유저의 학과로 드롭다운을 미리 채울 때 씀).

`DepartmentPickerPanel`은 두 가지 모드로 재사용됩니다:

| 모드 | 쓰는 화면 | 반환 메서드 |
|---|---|---|
| `includeAllOption=false` | `RegisterPanel`, `UserEditPanel` | `getSelectedDepartmentName()` — 항상 학과 하나 |
| `includeAllOption=true` | `NoticePostEditorPanel` | `getTargetDepartments()` — 각 단계 "모두" 선택에 따라 리프 학과 이름 목록 반환. 단과대에서 "모두"를 고르면 빈 리스트(=전체 공지, 기존 규칙과 동일) |

> ✅ **시연 데이터 개편 완료(2026-07-23)**: 트리에 없던 "컴퓨터공학과"·"물리학과"는
> `server/data/users.dat`·`server/data/boards/notice_board.dat`(n002 공지의 대상 학과)에서
> 각각 "컴퓨터공학전공"(AI융합대학>컴퓨터공학부)·"전기공학전공"(첨단융합대학>에너지·전기공학부)
> 으로 바꿔서 트리와 일치시켰습니다. 게시판 파일도 트리의 리프 이름과 똑같이
> `class_boards/컴퓨터공학전공.dat`·`class_boards/전기공학전공.dat`(옛 `physics.dat` — 내용은
> 그대로 두고 이름만 바꿨습니다. "일반물리 실험"·"전자기학 재수강"은 전기공학 전공생에게도
> 자연스러운 내용입니다)로 이름을 맞췄습니다.
>
> **`DataStore.registerBoards()`가 `AcademicStructure.COLLEGES`를 순회해서 40개 학과 게시판을
> 전부 자동 등록**하므로, 위 3곳(AI소프트웨어학과·컴퓨터공학전공·전기공학전공)만 시연용
> 게시글이 채워져 있고 나머지 37개는 등록은 됐지만 빈 게시판입니다.
