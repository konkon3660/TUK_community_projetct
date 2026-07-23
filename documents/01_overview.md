# 01. 프로젝트 개요

개발이 처음인 팀원도 읽을 수 있게 용어를 풀어 썼습니다.
진행 상황은 여기 적지 않습니다 — [08_status.md](08_status.md)를 보세요.

---

## 1. 과제 배경

Java 프로그래밍 응용 수업의 조별 과제입니다. 각 조가 교수에게 주제를 심의받아 정하고
프로젝트를 제작합니다.

- 조원: 4명
- 주제: **한국공학대학교 커뮤니티 프로그램**
- 생성형 AI 활용은 허용되지만 **최대한 이해하고 배우는 방향**으로 써야 하며,
  사용한 프롬프트를 통합 제출해야 합니다.
- 그래서 **설계는 다 같이 하고, 설계 문서를 기준으로 각자 구현**하기로 했습니다.
  이 `documents/` 폴더가 그 "설계 문서"입니다.

---

## 2. 한 문장 요약

> **서버 컴퓨터 1대가 모든 글과 채팅을 보관하고, 학생들의 프로그램(클라이언트) 여러 개가
> 네트워크로 서버에 접속해서 글을 읽고 쓰는 프로그램.**

### 식당 비유

| 프로젝트 | 식당 비유 |
|---|---|
| **서버** (`server/`) | 주방. 재료(데이터)를 전부 보관하고, 주문이 오면 요리해서 내보냄 |
| **클라이언트** (`client/`) | 손님 테이블. 메뉴판을 보고 주문하고, 나온 음식을 먹음 |
| **프로토콜** (`model/protocol/`) | 주문서 양식. "무엇을 주문하는지" 적는 정해진 종이 |
| **모델** (`model/`) | 재료 자체 = 회원, 게시글, 댓글, 채팅 |
| **.dat 파일** (`server/data/`) | 창고. 주방이 문을 닫아도 재료가 남아있게 보관하는 곳 |

핵심: **손님은 창고에 직접 못 들어갑니다.** 클라이언트는 `.dat` 파일을 직접 열지 않고
반드시 서버에게 부탁합니다. 그래야 여러 명이 동시에 써도 데이터가 꼬이지 않습니다.

---

## 3. 폴더 구조

```
Java_summer_p/
│
├── model/                    ← [공통] 서버와 클라이언트가 둘 다 쓰는 코드
│   ├── User.java                 회원 1명
│   ├── Chat.java                 채팅 메시지 1개
│   ├── ChatRoom.java             채팅방 1개 (가입 제한·승인 포함)
│   ├── DataFormat.java           파일 저장 규칙 (구분 기호 상수)
│   ├── FileStorage.java          파일 읽기/쓰기 담당
│   │
│   ├── boards/               ← 게시판과 게시글
│   │   ├── Post.java             게시글 1개 (기본형, 그대로 인스턴스화해서 씀)
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
│       ├── RequestType.java      요청 종류 20가지
│       ├── ResponseStatus.java   OK / ERROR
│       ├── BoardKey.java         게시판 이름표 상수
│       └── (요청서 DTO 9종)      LoginRequest, PostDeleteRequest 등
│
├── server/                   ← [서버 전용]
│   ├── CT/
│   │   ├── ServerMain.java       ★ 서버 시작 지점 (포트 5000)
│   │   ├── ClientHandler.java    접속한 학생 1명을 전담 (업무 로직 전부)
│   │   └── SessionRegistry.java  지금 접속 중인 세션 목록 (푸시 대상 찾기용)
│   ├── board/
│   │   └── DataStore.java        모든 데이터를 메모리에 올려두는 창고 관리인
│   └── data/                 ← 실제 저장 파일
│       ├── users.dat             회원 목록
│       ├── boards/*.dat          게시판별 글 목록
│       └── chatrooms/*.dat       채팅방별 기록 (파일 1개 = 방 1개)
│
├── client/                   ← [클라이언트 전용] 학생들이 실행하는 프로그램
│   ├── CT/
│   │   ├── ServerConnection.java 서버와의 연결 + 요청/응답 짝맞춤
│   │   └── PushListener.java     서버가 먼저 말 걸 때 받는 귀 (인터페이스)
│   ├── GUI/                  ← Swing 화면 15개 + MainFrame
│   │   └── (목록은 06_gui.md 참고)
│   ├── recomment_system/     ← 추천 3종 (서버 필요 없음, 로컬 파일만 읽음)
│   │   ├── activity/             할 거 추천기
│   │   ├── book/                 책 추천기
│   │   └── menu/                 메뉴 추천기
│   └── recommend_data/*.dat      추천 기능이 읽는 데이터
│
└── documents/                ← 설계 문서 (지금 보는 곳)
```

### 왜 `model/`은 양쪽이 같이 쓰나요?

서버가 "게시글"을 보낼 때 클라이언트도 "게시글"이 뭔지 알아야 받을 수 있습니다.
서로 같은 `Post.java`를 보고 있어야 말이 통합니다. 그래서 공통 재료는 `model/`에 한 벌만
두고 서버/클라이언트 양쪽에서 가져다 씁니다.

> `model`, `server`, `client`는 **패키지 이름**이기도 합니다. `model.boards.Post`는
> `model/boards/Post.java` 파일을 뜻합니다.

---

## 4. 실행 방법

Java 21 기준. 프로젝트 최상위 폴더(`Java_summer_p`)에서 실행합니다.
**상대 경로로 `server/data/...`를 읽으므로 반드시 최상위 폴더에서 실행해야 합니다.**

### Windows PowerShell

```powershell
# 1. 전체 컴파일 (out 폴더 생성)
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java | % FullName)

# 2. 서버 실행 (터미널 1)
java -cp out server.CT.ServerMain

# 3. 클라이언트 실행 (터미널 2, 여러 개 띄워도 됨)
java -cp out client.GUI.MainFrame
```

### bash / macOS / Linux

```bash
javac -encoding UTF-8 -d out $(find . -name "*.java")
java -cp out server.CT.ServerMain
java -cp out client.GUI.MainFrame
```

- 서버 포트는 `ServerMain.PORT = 5000`, 클라이언트 접속 대상은
  `MainFrame.SERVER_HOST = "localhost"` / `SERVER_PORT = 5000`입니다.
  다른 컴퓨터에서 접속하려면 `MainFrame`의 `SERVER_HOST`를 서버 IP로 바꾸면 됩니다.
- **클라이언트는 서버가 켜져 있어야 시작됩니다** (`MainFrame.main`이 접속부터 합니다).
- `out/`은 `.gitignore`에 있으니 커밋되지 않습니다.

---

## 5. 용어 사전

| 용어 | 뜻 |
|---|---|
| **클래스 (class)** | 물건의 설계도. `User` 클래스 = "회원이란 이런 것" |
| **객체 (object) / 인스턴스** | 설계도로 실제로 찍어낸 물건. 회원 100명 = `User` 객체 100개 |
| **메서드 (method)** | 그 물건이 할 수 있는 동작. "함수"와 거의 같은 말 |
| **필드 / 멤버변수** | 그 물건이 가진 정보. 회원의 학번, 학과 등 |
| **getter / setter** | 필드를 꺼내는 함수(`getId()`) / 바꾸는 함수(`setPassword()`) |
| **상속 (extends)** | 기존 설계도를 물려받아 확장. `GroupBuyPost extends Post` |
| **인터페이스 (interface)** | 동작 목록만 정한 약속. 내용은 각자 채움 |
| **추상 클래스 (abstract)** | 반쯤 완성된 설계도. 그대로는 못 쓰고 물려받아 완성해야 함 |
| **패키지 (package)** | 클래스를 담는 폴더. `model.boards` = `model/boards/` |
| **소켓 (socket)** | 두 컴퓨터를 잇는 전화선 |
| **직렬화 (Serializable)** | 객체를 전선으로 보낼 수 있게 납작하게 만드는 것 |
| **스레드 (thread)** | 동시에 일하는 일꾼 |
| **동기화 (synchronized)** | 한 번에 한 명만 들어가게 문을 잠그는 것 |
| **푸시 (push)** | 요청하지 않았는데 서버가 먼저 보내는 알림 (카톡 알림 같은 것) |
| **payload** | 봉투 안의 실제 내용물 |
| **DTO** | 데이터를 담아 나르기만 하는 상자 클래스 (`LoginRequest` 등) |
| **boardKey** | 게시판을 가리키는 문자열 이름표 (`"free"`, `"notice"`, 학과명 등) |
| **TODO** | "여기 아직 안 만들었음"이라는 코드 표시 |