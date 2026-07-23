# GUI 작성 가이드 (Swing)

`client/GUI/`는 Swing으로 만든다 (빌드 시스템이 없어서 외부 라이브러리 설치 부담이 없는 게
중요했음). `MainFrame`/`LoginPanel`에 이미 스켈레톤이 있다 — **화면 레이아웃 자체는 자유지만,
"화면이 서버와 대화하는 방식"과 "화면 전환 방식"은 아래 패턴을 그대로 따를 것.**

---

## 1. `MainFrame`

`CardLayout`으로 화면(JPanel)들을 이름으로 등록해두고 전환하는 셸. `ServerConnection`을 하나만
만들어서 들고 있고 모든 화면이 공유한다.

- `mainFrame.getConnection()` — 서버로 요청을 보낼 때 사용 (`ServerConnection.sendRequest(Packet)`)
- `mainFrame.registerScreen(name, panel)` — 화면 등록
- `mainFrame.switchTo(name)` — 화면 전환

## 2. 새 화면 만드는 법 (`LoginPanel` 패턴)

1. `JPanel`을 상속한다.
2. 생성자는 `MainFrame mainFrame`을 받아서 필드로 저장한다.
3. Swing 컴포넌트(버튼, 텍스트필드 등)는 필드로 선언한다.
4. 버튼 클릭 등 액션 리스너는 생성자에서 바로 연결한다 (`addActionListener`).
5. 실제 배치는 `initLayout()`처럼 별도 메서드로 분리해도 되고 자유롭게 해도 된다 — 여기는 팀원
   재량.
6. 서버에 요청을 보낼 때는 `model/protocol`의 DTO + `Packet.request(RequestType, payload)`를
   만들어서 `mainFrame.getConnection().sendRequest(request)`로 보낸다 (블로킹으로 응답이 옴).
   `response.getStatus()`가 `OK`인지 확인하고, `ERROR`면 `response.getErrorMessage()`를 보여준다.
7. 응답을 받은 뒤 다음 화면으로 넘어가야 하면 `mainFrame.switchTo("화면이름")`을 호출한다.
8. 서버 푸시(새 채팅, 새 공지 등)를 실시간으로 받아야 하는 화면(채팅방 등)은 화면이 보여질 때
   `mainFrame.getConnection().setPushListener(packet -> ...)`으로 자기 자신을 등록한다. 화면
   전환 시점마다 리스너가 바뀌는 셈이므로, 화면이 사라질 때 리스너를 다시 해제할 필요는 없고
   다음 화면이 자기 걸로 덮어쓰면 된다.

`LoginPanel.java` 전체가 위 패턴의 실제 예시다.

## 3. 앞으로 만들어야 할 화면 (회의록 기준 체크리스트)

- [x] 로그인 (`LoginPanel`)
- [ ] 회원가입
- [ ] 게시판 목록/네비게이션 (자유/공동구매/학과/기숙사/공지/민원)
- [ ] 게시글 목록 (boardKey로 `POST_LIST` 요청)
- [ ] 게시글 상세 + 댓글
- [ ] 게시글 작성/수정 (파일/이미지 첨부 포함)
- [ ] 채팅방 탐색/검색, 가입한 채팅방 목록
- [ ] 채팅방 화면 (실시간 수신은 `PushListener`로)
- [ ] 민원 작성 + 문의 내역(답변 대기/완료 표시)
- [ ] 관리자 화면(게시글 삭제, 문의 답변, 회원정보 수정, 공지 작성)
- [ ] 추천 3종 탭 (할거/메뉴/책 — `client/recomment_system/*` 사용)

## 4. 추천 기능 연동

추천 3종(`client/recomment_system/activity`, `menu`, `book`)은 서버 통신이 필요 없다 — 로컬
`client/recommend_data/*` 파일을 그대로 읽는다. 화면에서는 각 `*Recommender` 클래스를 그대로
호출하면 된다 (예: `new MenuRecommender().recommendTwoRandom()`).
