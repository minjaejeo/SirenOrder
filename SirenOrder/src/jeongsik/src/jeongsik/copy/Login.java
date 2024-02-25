package jeongsik.copy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

public class Login {
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "c##salmon";
    private static final String PASSWORD = "1234";
    private static final Logger logger = Logger.getLogger(Login.class.getName());

    /**
     * 사용자 로그인을 처리하는 메서드.
     * 데이터베이스에서 사용자 이름과 비밀번호를 확인하여 로그인 성공 여부를 JSON 형태로 반환한다.
     *
     * @param username 사용자 이름
     * @param password 사용자 비밀번호
     * @return 로그인 성공 여부를 나타내는 JSON 문자열
     */
    public static String login(String username, String password) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        JSONObject jsonResponse = new JSONObject();

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            String query = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);

            resultSet = statement.executeQuery();
            if (resultSet.next() && resultSet.getInt(1) == 1) {
                jsonResponse.put("로그인상태", "성공");
                jsonResponse.put("userId", username);
            } else {
                jsonResponse.put("로그인상태", "실패");
                jsonResponse.put("메시지", "잘못된 사용자 이름 또는 비밀번호입니다.");
            }
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "로그인 처리 중 오류 발생", e);
            jsonResponse.put("로그인상태", "실패");
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "리소스 해제 중 오류 발생", e);
            }
        }
        return jsonResponse.toJSONString();
    }
}
