package client.recomment_system.menu;

import java.util.regex.Pattern;

import model.DataFormat;

/** 식당 + 메뉴 한 쌍. menu_recomend.dat 한 줄 = 한 쌍. */
public class MenuOption {
    private final String restaurant;
    private final String menuName;

    public MenuOption(String restaurant, String menuName) {
        this.restaurant = restaurant;
        this.menuName = menuName;
    }

    public String getRestaurant() {
        return restaurant;
    }

    public String getMenuName() {
        return menuName;
    }

    public String toDataString() {
        return String.join(DataFormat.FIELD_DELIM, restaurant, menuName);
    }

    public static MenuOption fromDataString(String line) {
        String[] f = line.split(Pattern.quote(DataFormat.FIELD_DELIM), -1);
        return new MenuOption(f[0], f[1]);
    }
}
