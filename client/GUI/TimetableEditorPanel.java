package client.GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import client.recomment_system.activity.ActivityRecommender;
import client.recomment_system.activity.ClassPeriodTable;
import client.recomment_system.activity.TimetableEntry;

/**
 * 시간표 입력/수정 화면 (요구사항 §5.1 "직접 입력하는 화면을 따로 두고 언제든 수정").
 * 서버 통신은 없고 client/recommend_data/time_table.dat만 읽고 쓴다.
 * 요일(월~금) × 교시(1~9) 격자에 수업명을 치면 되고, 빈칸 = 그 교시에 수업 없음(공강).
 */
public class TimetableEditorPanel extends JPanel {
    private static final String[] DAY_NAMES = { "월", "화", "수", "목", "금" };
    private static final int DAYS = DAY_NAMES.length; // TimetableEntry의 요일 1=월 ~ 5=금과 대응
    private static final int PERIODS = ClassPeriodTable.PERIODS.size();

    private final MainFrame mainFrame;
    private final ActivityRecommender activityRecommender = new ActivityRecommender();
    // cells[요일-1][교시-1] = 그 칸의 수업명 입력칸
    private final JTextField[][] cells = new JTextField[DAYS][PERIODS];
    private final JButton saveButton = new JButton("저장");
    private final JButton backButton = new JButton("뒤로");

    public TimetableEditorPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        saveButton.addActionListener(e -> save());
        // 취소는 저장하지 않고 추천 화면으로만 돌아간다 (다음에 열면 파일 값으로 다시 채워짐).
        backButton.addActionListener(e -> mainFrame.switchTo("recommend"));
        initLayout();
    }

    /** 요일 헤더 + 교시 행으로 이루어진 격자를 배치하는 부분 — 디자인은 자유. */
    private void initLayout() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        JLabel title = new JLabel("시간표 입력");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel hint = new JLabel("※ 칸에 수업명을 입력하세요. 빈칸은 공강으로 봅니다. 교시 시각은 1교시 09:30부터 1시간 단위.");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(Color.GRAY);
        JPanel head = new JPanel(new BorderLayout(0, 4));
        head.add(title, BorderLayout.NORTH);
        head.add(hint, BorderLayout.CENTER);
        add(head, BorderLayout.NORTH);

        // (1 + 교시 9)행 × (1 + 요일 5)열: 첫 행은 요일 이름, 첫 열은 "N교시" 라벨.
        JPanel grid = new JPanel(new GridLayout(1 + PERIODS, 1 + DAYS, 4, 4));
        grid.add(new JLabel("")); // 왼쪽 위 모서리
        for (String dayName : DAY_NAMES) {
            grid.add(new JLabel(dayName, SwingConstants.CENTER));
        }
        for (int period = 0; period < PERIODS; period++) {
            grid.add(new JLabel((period + 1) + "교시", SwingConstants.CENTER));
            for (int day = 0; day < DAYS; day++) {
                cells[day][period] = new JTextField();
                grid.add(cells[day][period]);
            }
        }
        add(grid, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(backButton);
        add(buttons, BorderLayout.SOUTH);
    }

    /** mainFrame.switchTo("timetableEditor") 전에 호출한다. 파일의 현재 시간표로 격자를 채운다. */
    public void open() {
        for (JTextField[] dayColumn : cells) {
            for (JTextField cell : dayColumn) {
                cell.setText("");
            }
        }
        List<TimetableEntry> timetable;
        try {
            timetable = activityRecommender.loadTimetable();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "시간표 읽기 실패", JOptionPane.ERROR_MESSAGE);
            return; // 빈 격자에서 새로 입력할 수는 있게 화면은 그대로 둔다
        }
        for (TimetableEntry entry : timetable) {
            // 범위 밖 값(손으로 고친 파일)은 조용히 건너뛴다 — 편집기가 못 그리는 칸일 뿐이다.
            if (entry.getDayOfWeek() >= 1 && entry.getDayOfWeek() <= DAYS
                    && entry.getPeriodIndex() >= 1 && entry.getPeriodIndex() <= PERIODS) {
                cells[entry.getDayOfWeek() - 1][entry.getPeriodIndex() - 1].setText(entry.getClassName());
            }
        }
    }

    /** 빈칸이 아닌 칸만 모아 time_table.dat 전체를 덮어쓴다. */
    private void save() {
        List<TimetableEntry> timetable = new ArrayList<>();
        for (int day = 0; day < DAYS; day++) {
            for (int period = 0; period < PERIODS; period++) {
                String className = cells[day][period].getText().trim();
                if (!className.isEmpty()) {
                    timetable.add(new TimetableEntry(day + 1, period + 1, className));
                }
            }
        }
        try {
            activityRecommender.saveTimetable(timetable);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "시간표 저장 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 추천 화면은 다시 보여질 때(componentShown) 알아서 새로 읽으므로 전환만 하면 된다.
        JOptionPane.showMessageDialog(this, "시간표를 저장했습니다.", "시간표 저장", JOptionPane.INFORMATION_MESSAGE);
        mainFrame.switchTo("recommend");
    }
}
