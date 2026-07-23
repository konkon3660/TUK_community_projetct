package model;

public class FreeBoard extends AbstractBoard {
    private static final String DATA_FILE_PATH = "server/data/boards/free_board.dat";

    @Override
    public boolean canAccess(User user) {
        return true;
    }

    @Override
    public String getDataFilePath() {
        return DATA_FILE_PATH;
    }
}
