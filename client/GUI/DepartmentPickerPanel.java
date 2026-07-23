package client.GUI;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import model.AcademicStructure;
import model.AcademicStructure.College;
import model.AcademicStructure.Department;
import model.AcademicStructure.Division;

/**
 * 단과대 → 학부 → 학과 3단 연동 드롭다운 (AttachmentPicker와 같은 공용 위젯 패턴 —
 * 그 자체가 JPanel이라 add(picker) 한 줄이면 폼에 끼워 넣을 수 있다).
 *
 * <p>두 가지 용도로 재사용한다:
 * <ul>
 *   <li>회원가입/회원정보수정 — 학과 하나만 정확히 골라야 함 ({@code includeAllOption=false}).
 *       이때는 {@link #getSelectedDepartmentName()}을 쓴다.</li>
 *   <li>공지 대상학과 — 단과대·학부 단위로 "모두"를 골라 그 밑 전체 학과를 한 번에 지정할 수
 *       있어야 함 ({@code includeAllOption=true}). 이때는 {@link #getTargetDepartments()}를 쓴다.</li>
 * </ul>
 */
public class DepartmentPickerPanel extends JPanel {
    private static final String ALL = "모두";

    private final boolean includeAllOption;
    private final JComboBox<String> collegeCombo = new JComboBox<>();
    private final JComboBox<String> divisionCombo = new JComboBox<>();
    private final JComboBox<String> departmentCombo = new JComboBox<>();
    private boolean loading; // 콤보를 코드로 채우는 동안 리스너가 다시 반응하지 않게 막는 플래그

    public DepartmentPickerPanel(boolean includeAllOption) {
        this.includeAllOption = includeAllOption;
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        add(new JLabel("단과대"));
        add(collegeCombo);
        add(new JLabel("학부"));
        add(divisionCombo);
        add(new JLabel("학과"));
        add(departmentCombo);

        collegeCombo.addActionListener(e -> {
            if (!loading) {
                onCollegeChanged();
            }
        });
        divisionCombo.addActionListener(e -> {
            if (!loading) {
                onDivisionChanged();
            }
        });

        populateColleges();
    }

    private void populateColleges() {
        loading = true;
        collegeCombo.removeAllItems();
        if (includeAllOption) {
            collegeCombo.addItem(ALL);
        }
        for (College college : AcademicStructure.COLLEGES) {
            collegeCombo.addItem(college.getName());
        }
        loading = false;
        onCollegeChanged(); // 첫 항목 기준으로 학부/학과 콤보도 채움
    }

    /** 단과대가 바뀌면 학부 콤보를 다시 채운다. "모두"를 골랐으면 아래 두 단계는 의미가 없어 잠근다. */
    private void onCollegeChanged() {
        String selectedCollege = (String) collegeCombo.getSelectedItem();
        boolean collegeIsAll = ALL.equals(selectedCollege);
        divisionCombo.setEnabled(!collegeIsAll);
        departmentCombo.setEnabled(!collegeIsAll);

        loading = true;
        divisionCombo.removeAllItems();
        if (!collegeIsAll) {
            if (includeAllOption) {
                divisionCombo.addItem(ALL);
            }
            College college = findCollege(selectedCollege);
            if (college != null) {
                for (Division division : college.getDivisions()) {
                    divisionCombo.addItem(division.getName());
                }
            }
        }
        loading = false;
        onDivisionChanged();
    }

    /** 학부가 바뀌면 학과 콤보를 다시 채운다. */
    private void onDivisionChanged() {
        String selectedCollege = (String) collegeCombo.getSelectedItem();
        String selectedDivision = (String) divisionCombo.getSelectedItem();
        boolean skip = ALL.equals(selectedCollege) || ALL.equals(selectedDivision) || selectedDivision == null;

        loading = true;
        departmentCombo.removeAllItems();
        if (!skip) {
            College college = findCollege(selectedCollege);
            Division division = college == null ? null : findDivision(college, selectedDivision);
            if (includeAllOption) {
                departmentCombo.addItem(ALL);
            }
            if (division != null) {
                for (Department department : division.getDepartments()) {
                    departmentCombo.addItem(department.getName());
                }
            }
        }
        loading = false;
    }

    private static College findCollege(String name) {
        for (College college : AcademicStructure.COLLEGES) {
            if (college.getName().equals(name)) {
                return college;
            }
        }
        return null;
    }

    private static Division findDivision(College college, String name) {
        for (Division division : college.getDivisions()) {
            if (division.getName().equals(name)) {
                return division;
            }
        }
        return null;
    }

    /**
     * 회원가입/회원정보수정 용도: 항상 학과(리프) 이름 하나를 돌려준다.
     * "모두"가 있는 화면(includeAllOption=true)에서는 값이 하나로 안 좁혀질 수 있으므로 쓰지 않는다
     * — 그런 화면은 {@link #getTargetDepartments()}를 쓴다.
     */
    public String getSelectedDepartmentName() {
        if (includeAllOption) {
            throw new IllegalStateException("\"모두\"가 있는 화면은 getTargetDepartments()를 써야 합니다");
        }
        return (String) departmentCombo.getSelectedItem();
    }

    /**
     * 공지 대상학과 용도: 단과대/학부/학과 중 어느 단계에서 "모두"를 골랐느냐에 따라 그 아래
     * 전체 리프 학과 이름을 모아서 돌려준다. 단과대에서 "모두"를 고르면 전체 공지라는 뜻이라
     * 빈 리스트를 돌려준다 — {@code NoticePost.targetDepartments}가 원래 "비어 있으면 전체 공지"로
     * 해석하므로 그대로 맞아떨어진다.
     */
    public List<String> getTargetDepartments() {
        String selectedCollege = (String) collegeCombo.getSelectedItem();
        if (ALL.equals(selectedCollege)) {
            return new ArrayList<>();
        }
        College college = findCollege(selectedCollege);
        if (college == null) {
            return new ArrayList<>();
        }

        String selectedDivision = (String) divisionCombo.getSelectedItem();
        if (ALL.equals(selectedDivision)) {
            List<String> all = new ArrayList<>();
            for (Division division : college.getDivisions()) {
                for (Department department : division.getDepartments()) {
                    all.add(department.getName());
                }
            }
            return all;
        }
        Division division = findDivision(college, selectedDivision);
        if (division == null) {
            return new ArrayList<>();
        }

        String selectedDepartment = (String) departmentCombo.getSelectedItem();
        if (ALL.equals(selectedDepartment)) {
            List<String> all = new ArrayList<>();
            for (Department department : division.getDepartments()) {
                all.add(department.getName());
            }
            return all;
        }
        List<String> single = new ArrayList<>();
        if (selectedDepartment != null) {
            single.add(selectedDepartment);
        }
        return single;
    }

    /**
     * Swing의 {@code JPanel.setEnabled}는 자식 컴포넌트에 자동으로 전파되지 않으므로 오버라이드한다.
     * 다시 켤 때는 onCollegeChanged()를 다시 불러 "단과대=모두" 상태였다면 학부/학과가
     * 잠긴 채로 돌아오게 한다(단과대 콤보만 무조건 켜고 나머지는 캐스케이딩 규칙을 따름).
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        collegeCombo.setEnabled(enabled);
        if (enabled) {
            onCollegeChanged();
        } else {
            divisionCombo.setEnabled(false);
            departmentCombo.setEnabled(false);
        }
    }

    /**
     * 기존 학과 값으로 3단을 미리 채운다 (회원정보수정에서 조회한 유저의 학과에 맞춰둘 때 사용).
     * 트리에 없는 값(과거 자유 입력 시절 데이터)이면 아무것도 선택하지 않고 {@code false}를
     * 돌려준다 — 호출한 쪽에서 안내 문구를 보여줘야 한다.
     */
    public boolean setSelection(String departmentName) {
        AcademicStructure.Location location = AcademicStructure.locate(departmentName);
        if (location == null) {
            return false;
        }
        loading = true;
        collegeCombo.setSelectedItem(location.college.getName());
        loading = false;
        onCollegeChanged();

        loading = true;
        divisionCombo.setSelectedItem(location.division.getName());
        loading = false;
        onDivisionChanged();

        departmentCombo.setSelectedItem(location.department.getName());
        return true;
    }
}
