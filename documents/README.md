# 문서 인덱스

한국공학대학교 커뮤니티 프로그램(TUK Community) 팀 프로젝트 설계 문서 모음입니다.

> **기준 시점: 2026-07-23 (작업 트리 기준, 미커밋 변경 포함)**
> Java 21 / 소스 62개 / **컴파일 에러 0개 확인** / 남은 `TODO` 31개

## 읽는 순서

| 문서 | 무엇이 들어있나 | 이럴 때 읽는다 |
|---|---|---|
| [01_overview.md](01_overview.md) | 프로젝트 배경, 한 문장 요약, 폴더 구조, 실행 방법, 용어 사전 | **처음 합류했을 때 이것부터** |
| [02_requirements.md](02_requirements.md) | 기능 요구사항 확정본 (무엇을 만들기로 했는가) | "이 기능 사양이 뭐였지?" |
| [03_architecture.md](03_architecture.md) | 계층 구조, 요청 흐름, 푸시, 동시성, 파일 저장 | "이게 어떻게 굴러가지?" |
| [04_data_model.md](04_data_model.md) | 엔티티 필드/메서드/파일 포맷 명세 | 모델 클래스를 건드릴 때 |
| [05_protocol.md](05_protocol.md) | Packet / RequestType 20종 / DTO 명세 | 서버-클라이언트 통신을 건드릴 때 |
| [06_gui.md](06_gui.md) | 화면 15개 목록, 화면 전환·서버 요청 컨벤션 | GUI를 만들 때 |
| [07_class_diagram.md](07_class_diagram.md) | 클래스 다이어그램 작성용 정리 (제출물용) | 다이어그램을 그릴 때 |
| [08_status.md](08_status.md) | **진행 현황과 남은 작업 (단일 출처)** | "나 뭐 하면 돼?" |
| [09_usecase_diagram.md](09_usecase_diagram.md) | 유스케이스 다이어그램 작성용 정리 (제출물용) | 다이어그램을 그릴 때 |
| [10_activity_diagram.md](10_activity_diagram.md) | 액티비티 다이어그램 작성용 정리 (제출물용) | 다이어그램을 그릴 때 |
| [11_sequence_diagram.md](11_sequence_diagram.md) | 시퀀스 다이어그램 작성용 정리 (제출물용) | 다이어그램을 그릴 때 |
| [99_meeting_notes.md](99_meeting_notes.md) | 07/22·07/23 회의 원문 기록 | 결정 배경이 궁금할 때 |

## 문서 규칙

1. **진행 상황(무엇이 끝났고 무엇이 남았는지)은 `08_status.md`에만 적는다.**
   다른 문서에 상태를 중복해서 쓰면 반드시 서로 어긋난다 — 실제로 이전 버전에서 그랬다.
   나머지 문서는 "무엇을 어떻게 만들기로 했는가"(사양)만 적는다.
2. `RequestType`을 추가하면 `05_protocol.md`의 표와 `ClientHandler.SYNCHRONIZED_TYPES`를
   **같은 커밋에서** 함께 고친다.
3. 화면을 추가하면 `06_gui.md`의 표와 `MainFrame.main()`의 `registerScreen`을 함께 고친다.
4. AI에게 구현을 맡길 때는 **새로 설계하게 하지 말고**, 해당 문서를 붙여넣은 뒤
   `TODO: 구현 필요`로 표시된 메서드 본문만 채우게 한다.

## 남은 작업을 직접 확인하는 법

`08_status.md`가 오래됐다고 느껴지면 코드가 정답이다:

```powershell
Get-ChildItem -Recurse -Filter *.java | Select-String "TODO"
```