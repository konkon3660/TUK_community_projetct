package client.recomment_system.menu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.FileStorage;

/** 오늘의 학식 정보 + 맘에 안 들 때의 랜덤 메뉴 추천. 전부 조회/랜덤 선택뿐이라 완전 구현되어 있다. */
public class MenuRecommender {
    private static final Path OPTIONS_PATH = Path.of("client/recommend_data/menu_recomend.dat");
    private static final Path TODAY_MENU_PATH = Path.of("client/recommend_data/e_resterant_menu.txt");
    private static final Path TIP_PATH = Path.of("client/recommend_data/tip_under_menu.txt");

    private final Random random = new Random();

    public List<MenuOption> loadOptions() {
        List<MenuOption> result = new ArrayList<>();
        for (String line : readLines(OPTIONS_PATH)) {
            if (!line.isEmpty()) {
                result.add(MenuOption.fromDataString(line));
            }
        }
        return result;
    }

    /** 오늘의 학식 + E동 정보 텍스트 (한 줄 = 한 줄 그대로 출력) */
    public List<String> todayMenuText() {
        return readLines(TODAY_MENU_PATH);
    }

    /** 메뉴 아래에 붙는 꿀팁 한 줄을 무작위로 고른다 */
    public String randomTip() {
        List<String> tips = readLines(TIP_PATH);
        if (tips.isEmpty()) {
            return "";
        }
        return tips.get(random.nextInt(tips.size()));
    }

    /** 식당/메뉴 목록에서 무작위로 최대 2개를 골라 반환 ("메뉴 추천 다시 받기" 버튼에서 매번 호출) */
    public List<MenuOption> recommendTwoRandom() {
        List<MenuOption> options = new ArrayList<>(loadOptions());
        Collections.shuffle(options, random);
        return options.subList(0, Math.min(2, options.size()));
    }

    private List<String> readLines(Path path) {
        try {
            return FileStorage.readLines(path);
        } catch (IOException e) {
            throw new UncheckedIOException("추천 데이터 로드 실패: " + path, e);
        }
    }
}
