package model.boards;

import model.User;

public class DepartmentBoard extends AbstractBoard {
    private final String restrictedDepartment;
    private final String dataFilePath;

    // 학과별로 인스턴스를 하나씩 생성 (예: class_boards/AI_software_board.dat)
    public DepartmentBoard(String restrictedDepartment, String dataFilePath) {
        this.restrictedDepartment = restrictedDepartment;
        this.dataFilePath = dataFilePath;
    }

    public String getRestrictedDepartment() {
        return restrictedDepartment;
    }

    @Override
    public boolean canAccess(User user) {
        return user.isAdmin() || restrictedDepartment.equals(user.getDepartment());
    }

    @Override
    public String getDataFilePath() {
        return dataFilePath;
    }

    @Override
    protected Post parsePost(String line) {
        return Post.fromDataString(line);
    }
}
