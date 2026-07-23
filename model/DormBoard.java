package model;

public class DormBoard extends AbstractBoard {
    private static final String DATA_FILE_PATH = "server/data/boards/dormitory_board.dat";

    @Override
    public boolean canAccess(User user) {
        return user.isAdmin() || user.isDormitory();
    }

    @Override
    public String getDataFilePath() {
        return DATA_FILE_PATH;
    }
}
