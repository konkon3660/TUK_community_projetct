# 06. GUI 작성 가이드 (Swing)

`client/GUI/`는 Swing으로 만듭니다 (빌드 시스템이 없어 외부 라이브러리 설치 부담이 없는 게
중요했습니다).

> **화면 15개 전부 클래스와 메서드 시그니처까지 이미 만들어져 있습니다.**
> AI에게 새 화면 클래스를 설계하게 하지 말고, `TODO: 구현 필요`로 표시된 **메서드 본문만**
> 채우도록 지시하세요. 남은 목록은 [08_status.md](08_status.md)에 있습니다.

**레이아웃(`initLayout()`)은 자유입니다. 하지만 "서버와 대화하는 방식"과 "화면 전환 방식"은
아래 컨벤션을 그대로 따르세요.**

---

## 1. `MainFrame` — 셸

`CardLayout`으로 화면(`JPanel`)들을 이름으로 등록해두고 전환합니다.
`ServerConnection`과 로그인한 `User`(세션)를 **하나씩만** 만들어 들고 있고 모든 화면이 공유합니다.

| 메서드 | 용도 |
|---|---|
| `getConnection()` | 서버 요청 (`sendRequest(Packet)`) |
| `getCurrentUser()` / `setCurrentUser(User)` | 로그인 세션 공유. 로그인 성공 시 `LoginPanel`이 설정, 이후 다른 화면은 조회만 |
| `registerScreen(name, panel)` | 화면 등록 (`main()`에 15개 전부 등록되어 있음) |
| `switchTo(name)` | 화면 전환 |
| `getScreen(name)` | 등록된 화면 인스턴스를 이름으로 꺼냄 (§3 `open(...)` 패턴에서 사용) |

`main()`은 **`ServerConnection`을 먼저 만든 뒤** `SwingUtilities.invokeLater`로 창을 띄웁니다.
즉 **서버가 꺼져 있으면 클라이언트가 시작되지 않습니다.**

---

## 2. 로그인 분기 — 학생 / 관리자 완전 분리

관리자는 학생용 화면에 접근하면 안 됩니다(기획 확정). 로그인 성공 시:

```java
mainFrame.setCurrentUser(user);
mainFrame.switchTo(user.isAdmin() ? "admin" : "home");
```

- **학생** → `home`(`HomePanel`): 게시판/채팅/추천/민원
- **관리자** → `admin`(`AdminPanel`): 공지 작성/게시글 관리/민원함/회원정보 수정.
  `HomePanel`로 가는 경로가 아예 없고 **로그아웃만** 있습니다.
- `HomePanel`에 관리자 버튼을 넣지 마세요. `initLayout()`은 생성자에서(= 로그인 전에)
  실행되므로 그 시점의 `getCurrentUser()`는 **항상 `null`** 입니다.

### 학생/관리자가 공유하는 화면은 `backTarget`으로 구분

`PostListPanel`은 양쪽이 같이 씁니다. **뒤로가기 대상**을 열 때 넘겨서 구분합니다:

```java
// 학생이 열 때
((PostListPanel) mainFrame.getScreen("postList")).open(boardKey, "home");
// 관리자가 게시글 관리/민원함 목적으로 열 때
((PostListPanel) mainFrame.getScreen("postList")).open(boardKey, "admin");
```

`backTarget`을 학생용 `"home"`으로 넘기면 **관리자가 학생 화면에 갇힙니다.** 주의하세요.
`PostListPanel`은 `backTarget`이 `"admin"`이면 글쓰기 버튼도 숨깁니다(글쓰기는 학생 행위).

### 로그아웃 규약

`LOGOUT` 요청을 보내고 **성공했을 때만** `setCurrentUser(null)` 후 `switchTo("login")`.
서버 세션이 끊겼는데 클라이언트가 유저를 들고 있으면 이후 요청이 전부 이상하게 동작합니다.

---

## 3. 화면 전환 시 데이터 넘기기 — `open(...)` 컨벤션

`switchTo(name)`은 화면을 **보여주기만** 하고 데이터는 못 넘깁니다. "어떤 게시글을 / 어떤
채팅방을" 보여줄지 정해야 하는 화면은 `public void open(...)`을 갖고 있습니다.

**호출하는 쪽은 항상 이 순서를 지킵니다 — `open()` 먼저, `switchTo()` 나중:**

```java
((PostDetailPanel) mainFrame.getScreen("postDetail")).open(boardKey, post);
mainFrame.switchTo("postDetail");
```

`open(...)`이 서버 조회까지 하는 경우도 있습니다 (`PostListPanel.open`은 `POST_LIST`를
보내고 결과를 렌더링합니다).

### 목록으로 돌아갈 때는 `PostListPanel.refresh()`

`PostListPanel`은 받아온 게시글 목록을 **자기 안에 사본으로** 들고 있습니다. 상세나 에디터에서
글을 저장·삭제하고 `switchTo("postList")`만 하면 **방금 지운 글이 그대로 보입니다.**
그래서 목록으로 되돌아가기 직전에 인자 없는 `refresh()`를 부릅니다:

```java
((PostListPanel) mainFrame.getScreen("postList")).refresh();
mainFrame.switchTo("postList");
```

`refresh()`는 마지막으로 연 `boardKey`를 그대로 다시 조회합니다. `PostDetailPanel` /
`PostEditorPanel` / `GroupBuyPostEditorPanel`은 `backTarget`("home"인지 "admin"인지)을
모르기 때문에 `open(boardKey, backTarget)`을 대신 부를 수 없어서 이 메서드가 필요합니다.
(`NoticePostEditorPanel`만은 관리자 전용이라 `open(BoardKey.NOTICE, "admin")`을 직접 부릅니다.)

취소 버튼처럼 **아무것도 바꾸지 않고** 돌아갈 때는 `switchTo`만 하면 됩니다.

---

## 4. 화면 15개

각 클래스는 같은 형태입니다: `JPanel` 상속 / 생성자가 `MainFrame`을 받아 필드로 저장 /
버튼 액션 리스너는 생성자에서 연결 / 실제 배치는 `initLayout()`.

| `switchTo` 키 | 클래스 | 접근 | 하는 일 |
|---|---|---|---|
| `login` | `LoginPanel` | 공용 | 로그인 → 관리자/학생 분기(§2). **다른 화면 만들 때 참고용 예시** |
| `register` | `RegisterPanel` | 학생 | 회원가입 |
| `home` | `HomePanel` | 학생 | 학생 허브 — 게시판 6종/채팅/추천/민원/로그아웃 |
| `postList` | `PostListPanel` | 공용(`backTarget`) | 게시글 목록 (**모든 게시판 공용**), 글쓰기 버튼 → 게시판에 맞는 에디터 |
| `postDetail` | `PostDetailPanel` | 공용 | 게시글 상세 + 댓글 추가/삭제 + 수정/삭제 |
| `postEditor` | `PostEditorPanel` | 학생 | 글쓰기/수정 (자유·학과·기숙사) |
| `groupBuyPostEditor` | `GroupBuyPostEditorPanel` | 학생 | 공동구매 글쓰기/수정 (최대인원·해시태그·채팅방 연동) |
| `noticePostEditor` | `NoticePostEditorPanel` | 관리자 | 공지 작성/수정 (대상 학과·기숙사 지정) |
| `complaint` | `ComplaintPanel` | 학생 | 민원 접수 + "내 문의 내역" 진입 |
| `chatRoomList` | `ChatRoomListPanel` | 학생 | 채팅방 탐색/검색 |
| `chatRoomCreate` | `ChatRoomCreatePanel` | 학생 | 채팅방 생성 (가입 제한 설정) |
| `chatRoom` | `ChatRoomPanel` | 학생 | 채팅 송수신 + 가입 승인/거절. **`PushListener` 구현체** |
| `userEdit` | `UserEditPanel` | 관리자 | 학번으로 회원 조회 → 학과/기숙사/비밀번호 수정 |
| `admin` | `AdminPanel` | 관리자(진입점) | 관리자 허브 — 공지 작성/게시글 관리/민원함/회원정보 수정/로그아웃 |
| `recommend` | `RecommendPanel` | 학생 | 추천 3종 탭 (서버 통신 없음) |

**서버 요청을 만드는 실제 예시**로는 `LoginPanel.attemptLogin()`, `PostListPanel.open()`,
`UserEditPanel.search()`를 참고하세요.

---

## 5. 서버 요청 패턴 (모든 화면이 동일)

```java
Packet request  = Packet.request(RequestType.POST_LIST, boardKey);
Packet response = mainFrame.getConnection().sendRequest(request);   // 응답까지 블로킹
if (response.getStatus() == ResponseStatus.OK) {
    List<Post> posts = (List<Post>) response.getPayload();
    // 화면 반영
} else {
    JOptionPane.showMessageDialog(this, response.getErrorMessage(),
            "게시글 목록 조회 실패", JOptionPane.ERROR_MESSAGE);
}
```

규약:

1. **실패 시 서버가 준 `getErrorMessage()`를 그대로 보여줍니다.** 서버가 한국어로 보내므로
   클라이언트에서 메시지를 새로 만들지 마세요.
2. `sendRequest`는 응답까지 **최대 10초 블로킹**합니다. 이벤트 디스패치 스레드에서 호출하므로
   그동안 화면이 멈춥니다 — 현재 규모에서는 그대로 두기로 했습니다.
3. **권한 검사는 서버가 합니다.** GUI에서 버튼을 숨기는 건 편의일 뿐입니다.
   비기숙사생이 기숙사 게시판을 눌러도 서버가 거부하고 그 사유가 팝업으로 표시됩니다.
   그러므로 **권한 규칙이 바뀌면 화면이 아니라 `model/`의 `canXxx()`를 고쳐야** 합니다.
   `PostDetailPanel`은 `post.canEdit(me)` / `post.canDelete(me)` 결과를 그대로 따르므로,
   모델만 고치면 버튼은 저절로 맞게 나옵니다. 화면에서 `isAdmin()`을 직접 보고 분기하지 마세요.

> 📌 **2026-07-23 기획 변경**: 관리자는 **남의 글을 수정할 수 없습니다**(삭제만 가능).
> 민원 글은 **관리자가 삭제도 할 수 없고 답변(댓글)만** 할 수 있습니다.
> 상세 규칙은 [02_requirements.md §2.2](02_requirements.md)의 표를 보세요.

---

## 6. 서버 푸시 받기 (`ChatRoomPanel`)

푸시를 받아야 하는 화면은 `PushListener`를 **직접 구현**하고 `open()`에서 등록합니다:

```java
public void open(ChatRoom room) {
    this.room = room;
    mainFrame.getConnection().setPushListener(this);
    renderMessages();
}

@Override
public void onPush(Packet packet) {
    // ⚠️ 네트워크 스레드에서 호출된다 — Swing을 직접 건드리면 안 된다
    SwingUtilities.invokeLater(() -> { /* 화면 갱신 */ });
}
```

- `setPushListener`는 **하나만** 등록됩니다(덮어쓰기). 지금은 `ChatRoomPanel`만 씁니다.
- 공지 푸시(`NOTICE_PUSH`)까지 받으려면 리스너를 `MainFrame` 레벨로 올려서 `packet.getType()`으로
  분기하는 구조가 필요합니다 — 아직 결정되지 않았습니다([08_status.md](08_status.md) 참고).
- 보낸 사람 본인은 자기 채팅을 푸시로 받지 않습니다. 내가 보낸 메시지를 화면에 즉시 띄울지는
  화면에서 결정해야 합니다.

---

## 7. 추천 기능 연동 (`RecommendPanel`)

추천 3종은 **서버 통신이 필요 없습니다.** 로컬 파일만 읽으므로 `*Recommender`를 그대로
호출하면 됩니다:

```java
new MenuRecommender().todayMenuText();                    // 오늘의 학식
new MenuRecommender().recommendTwoRandom();               // 랜덤 식당 2곳
new MenuRecommender().randomTip();                        // 꿀팁 한 줄
new BookRecommender().recommendForDepartment(
        mainFrame.getCurrentUser().getDepartment());      // 학과별 책 1권 (없으면 null)
new ActivityRecommender().recommendNow(timetable, LocalDateTime.now());
```

- `recommendForDepartment`는 그 학과 책이 없으면 **`null`을 반환**합니다 (`Optional` 아님).
  화면에서 `null` 처리를 해야 합니다.
- `recommendNow`는 **수업 중이거나 남은 공강 안에 끝낼 수 있는 할 거가 없으면 `null`**을 반환합니다.
  그 둘을 화면에서 구분하려면 `ActivityRecommender.freeMinutesAt(timetable, now)`를 같이 보세요
  (수업 중이면 `IN_CLASS`, 오늘 남은 수업이 없으면 `UNLIMITED`, 아니면 다음 수업까지 남은 분).
  판단 규칙은 [08_status.md §3-3](08_status.md) 참고.
- 데이터 파일이 비어 있으면 빈 목록이 옵니다 — 할 거/시간표 외에는 아직 전부 비어 있어서,
  `RecommendPanel`의 메뉴·책 탭은 지금 "아직 등록되지 않았습니다" 안내만 나옵니다.
- `RecommendPanel`은 생성자가 **로그인 전에** 돌아 `getCurrentUser()`가 `null`이므로
  (`HomePanel`과 같은 이유), 데이터 로드를 `componentShown`에서 합니다. `HomePanel.openRecommend()`가
  `open(...)` 없이 `switchTo("recommend")`만 해도 되는 건 이 때문입니다.

---

## 8. 새 화면을 추가하는 절차

1. `LoginPanel`을 복사해 이름을 바꾸고, 서버 요청 부분을 원하는 요청으로 교체
2. 데이터를 받아야 하면 `public void open(...)` 추가 (§3 컨벤션)
3. `MainFrame.main()`에서 `registerScreen("이름", new XxxPanel(frame))`
4. **이 문서 §4 표에 한 줄 추가**
