package jeongsik;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

public class Signup {
	// Oracle 데이터베이스 연결 정보
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE"; // 변경될 수 있는 정보는 외부 설정에서 관리하는 것이 좋습니다.
    private static final String USER = "c##salmon"; // 데이터베이스 사용자 이름
    private static final String PASSWORD = "1234"; // 데이터베이스 비밀번호

    // Logger 설정
    private static final Logger logger = Logger.getLogger(Signup.class.getName());

    /**
     * 사용자 회원가입을 처리하는 메서드.
     * 데이터베이스에 새 사용자를 등록합니다.
     *
     * @param username 사용자 이름
     * @param password 사용자 비밀번호
     * @return 회원가입 결과를 나타내는 JSON 문자열
     */
    public static String register(String username, String password) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        JSONObject jsonResponse = new JSONObject();

        try {
            // Oracle JDBC 드라이버 로드
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // 데이터베이스 연결
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            // 이미 사용 중인 아이디인지 확인하는 쿼리 실행
            String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            statement = connection.prepareStatement(checkQuery);
            statement.setString(1, username);
            resultSet = statement.executeQuery();

            // 이미 사용 중인 아이디가 있으면 회원가입 실패 처리
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                jsonResponse.put("회원가입상태", "실패>>이미 가입된 아이디입니다.");
                
                return jsonResponse.toJSONString();
            }

            // 사용자 등록을 위한 SQL 쿼리 작성
            String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            statement = connection.prepareStatement(insertQuery);
            statement.setString(1, username);
            statement.setString(2, password); // 비밀번호는 해싱하여 저장하는 것이 보안에 좋습니다.

            // 쿼리 실행
            int rowsInserted = statement.executeUpdate();

            // 회원가입 성공 여부에 따른 응답 반환
            jsonResponse.put("회원가입상태", rowsInserted > 0 ? "성공>>로그인해주세요" : "실패");
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "회원가입 중 오류 발생", e);
            jsonResponse.put("회원가입상태", "오류");
        } finally {
            // 리소스 해제
            closeResources(resultSet, statement, connection);
        }

        return jsonResponse.toJSONString();
    }

   
    private static void closeResources(ResultSet resultSet, PreparedStatement statement, Connection connection) {
        try {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "리소스 해제 중 오류 발생", e);
        }
    }
}