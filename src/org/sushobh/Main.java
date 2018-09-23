package org.sushobh;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Base64;

public class Main {

    public static void main(String[] args) {
	    SQLiteServer sqLiteServer = new SQLiteServer(3000);
        try {
            sqLiteServer.startServer();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
