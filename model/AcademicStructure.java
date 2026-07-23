package model;

import java.util.ArrayList;
import java.util.List;

/**
 * 단과대 → 학부 → 학과(전공) 조직도(2026-07-23 확정). 회원가입·회원정보수정·공지 대상학과
 * 선택 화면이 전부 이 트리를 기준으로 3단 드롭다운을 그린다 — {@code User.department}에
 * 들어가는 값은 항상 이 트리의 리프(학과/전공) 이름과 정확히 같아야 한다.
 *
 * <p>일부 학부는 세부 전공 구분 없이 그 자체가 학과 역할을 한다(예: 자유전공학부, AI융합대학의
 * 게임공학과). 이런 경우도 트리 구조를 항상 3단으로 통일하기 위해 "학부 이름 = 학과 이름"인
 * 리프 하나짜리 {@link Division}으로 표현한다({@link Division#leaf(String)}).
 *
 * <p>자유전공학부·경영학부·디자인공학부는 소속된 단과대가 없다 — 이 셋은
 * {@link #NO_COLLEGE}라는 이름의 가짜 단과대 아래에 묶어서, 화면의 "단과대" 드롭다운이
 * 항상 먼저 나오는 구조를 깨지 않게 한다.
 *
 * <p>⚠️ 기존 시연 데이터(`server/data/users.dat`의 "컴퓨터공학과", "물리학과")는 이 트리에
 * 없다. 이미 그 값으로 저장된 계정은 그대로 동작하지만(User.department는 자유 문자열이라
 * 서버가 따로 검증하지 않음), 회원정보수정 화면에서 그 계정을 조회하면 드롭다운이 아무것도
 * 선택되지 않은 채로 뜬다 — 관리자가 새로 목록에서 골라야 한다.
 */
public final class AcademicStructure {

    public static final class Department {
        private final String name;

        public Department(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static final class Division {
        private final String name;
        private final List<Department> departments;

        public Division(String name, List<Department> departments) {
            this.name = name;
            this.departments = departments;
        }

        public String getName() {
            return name;
        }

        public List<Department> getDepartments() {
            return departments;
        }

        /** 세부 전공 없이 그 자체가 학과인 학부/학과 노드 (예: 자유전공학부, 게임공학과). */
        private static Division leaf(String name) {
            return new Division(name, List.of(new Department(name)));
        }

        private static Division of(String name, String... departmentNames) {
            List<Department> departments = new ArrayList<>();
            for (String departmentName : departmentNames) {
                departments.add(new Department(departmentName));
            }
            return new Division(name, departments);
        }
    }

    public static final class College {
        private final String name;
        private final List<Division> divisions;

        public College(String name, List<Division> divisions) {
            this.name = name;
            this.divisions = divisions;
        }

        public String getName() {
            return name;
        }

        public List<Division> getDivisions() {
            return divisions;
        }
    }

    /** 자유전공학부·경영학부·디자인공학부처럼 소속 단과대가 없는 학부를 묶는 이름. */
    public static final String NO_COLLEGE = "단과대 미지정";

    public static final List<College> COLLEGES = List.of(
            new College(NO_COLLEGE, List.of(
                    Division.leaf("자유전공학부"),
                    Division.of("경영학부", "경영 자율전공", "경영학전공", "데이터사이언스경영전공", "IT경영전공"),
                    Division.of("디자인공학부", "산업디자인공학전공", "미디어디자인공학전공"))),
            new College("AI융합대학", List.of(
                    Division.of("컴퓨터공학부", "컴퓨터공학전공", "소프트웨어전공"),
                    Division.leaf("게임공학과"),
                    Division.leaf("인공지능학과"))),
            new College("스마트기계융합대학", List.of(
                    Division.leaf("기계공학과"),
                    Division.of("기계설계공학부", "기계설계전공", "지능형모빌리티전공"),
                    Division.of("메카트로닉스공학부", "메카트로닉스공학전공", "AI로봇전공"))),
            new College("IT반도체융합대학", List.of(
                    Division.of("전자공학부", "전자공학전공", "임베디드시스템전공"),
                    Division.of("반도체공학부", "나노반도체공학전공", "반도체시스템전공"))),
            new College("첨단융합대학", List.of(
                    Division.leaf("신소재공학과"),
                    Division.leaf("생명화학공학과"),
                    Division.of("에너지·전기공학부", "전기공학전공", "에너지공학전공"))),
            new College("기업인재대학", List.of(
                    Division.of("조기취업학부", "스마트전자공학과", "AI소프트웨어학과",
                            "IT융합디자인공학과", "스마트그린소재공학과"),
                    Division.of("산학협력학부", "기계제조공학과", "기계설계⋅시스템공학과",
                            "AI컴퓨터전자융합공학과", "환경안전경영학과", "기업경영학과",
                            "반도체기계시스템공학과", "메카트로닉스시스템공학과", "스마트컴퓨터융합공학과"),
                    Division.of("일학습병행학부", "컴퓨터전자공학과", "로봇메카트로닉스공학과"),
                    Division.of("재직자특별전형과정", "산업융합공학과", "디지털경영학과"))));

    /** locate()가 돌려주는 리프 학과의 위치 3단(단과대·학부·학과). */
    public static final class Location {
        public final College college;
        public final Division division;
        public final Department department;

        Location(College college, Division division, Department department) {
            this.college = college;
            this.division = division;
            this.department = department;
        }
    }

    /** 학과(리프) 이름으로 소속 단과대·학부를 역으로 찾는다. 트리에 없는 이름이면 {@code null}
     *  (DepartmentPickerPanel.setSelection이 기존 값을 미리 채울 때 씀). */
    public static Location locate(String departmentName) {
        for (College college : COLLEGES) {
            for (Division division : college.getDivisions()) {
                for (Department department : division.getDepartments()) {
                    if (department.getName().equals(departmentName)) {
                        return new Location(college, division, department);
                    }
                }
            }
        }
        return null;
    }

    private AcademicStructure() {
    }
}
