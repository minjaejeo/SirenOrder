package jeongsik;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

public class Login {
	// Oracle 데이터베이스 연결 정보
    private static final String DB_URL = "jdbc:oracle:thin:@192.168.0.33:1521:XE"; // Oracle 서버 주소와 포트
    private static final String USER = "c##salmon"; // 데이터베이스 사용자 이름
    private static final String PASSWORD = "1234"; // 데이터베이스 비밀번호
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
            // Oracle JDBC 드라이버 로드
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // 데이터베이스 연결
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            // SQL 쿼리 작성: 사용자 이름과 비밀번호를 기준으로 사용자 존재 여부 확인
            String query = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
            
            // PreparedStatement 사용하여 SQL 쿼리 실행
            statement = connection.prepareStatement(query);
            statement.setString(1, username);
            // 보안 주의: 실제 애플리케이션에서는 비밀번호를 평문으로 저장하거나 비교해서는 안 됩니다.
            // 비밀번호는 해시화하여 저장하고, 로그인 시 해시된 비밀번호를 비교해야 합니다.
            statement.setString(2, password);

            // 쿼리 실행 및 결과 처리
            resultSet = statement.executeQuery();
            if (resultSet.next() && resultSet.getInt(1) == 1) {
                jsonResponse.put("로그인상태", "성공");
            } else {
                jsonResponse.put("로그인상태", "실패");
            }
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "로그인 처리 중 오류 발생", e);
            jsonResponse.put("로그인상태", "실패");
        } finally {
            // 리소스 정리
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