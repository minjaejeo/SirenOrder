package jeongsik;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ViewOrder {
	// Oracle 데이터베이스 연결 정보
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE"; // Oracle 서버 주소와 포트
    private static final String USER = "c##salmon"; // 데이터베이스 사용자 이름
    private static final String PASSWORD = "1234"; // 데이터베이스 비밀번호
    private static final Logger logger = Logger.getLogger(CoffeeOrder.class.getName());

    // 주문 조회 기능
    public void viewOrders() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            // Oracle JDBC 드라이브 로드
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // 데이터베이스 연결
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            // SQL 쿼리 작성
            String query = "SELECT * FROM ORDERLIST";
            statement = connection.prepareStatement(query);

            // 쿼리 실행 및 결과 처리
            resultSet = statement.executeQuery();

            // 주문 내역 출력
            while (resultSet.next()) {
                String menuName = resultSet.getString("COFFEENAME");
                String size = resultSet.getString("COFFEESIZE");
                int price = resultSet.getInt("PRICE");
                boolean isIced = resultSet.getInt("ISICED") == 1;
                boolean hasSyrup = resultSet.getInt("HASSYRUP") == 1;
                boolean isTakeout = resultSet.getInt("ISTAKEOUT") == 1;
                
                // 주문 내역 출력 형식에 맞게 수정
                System.out.println("---------------------------");
                System.out.println("메뉴 이름: " + menuName);
                System.out.println("크기: " + size);
                System.out.println("가격: " + price + "원");
                System.out.println("아이스: " + (isIced ? "예" : "아니오"));
                System.out.println("시럽 추가: " + (hasSyrup ? "예" : "아니오"));
                System.out.println("테이크아웃: " + (isTakeout ? "예" : "아니오"));
                System.out.println("---------------------------");
                
                CountCoffee countCoffee = new CountCoffee();
                countCoffee.countTotal(menuName);
                
            }
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "주문 조회 중 오류 발생", e);
        } finally {
            // 리소스 해제
            closeResources(resultSet, statement, connection);
        }
    }
    // 리소스 해제 메서드
    private static void closeResources(ResultSet resultSet, PreparedStatement statement, Connection connection) {
        try {
            if (resultSet != null)
                resultSet.close();
            if (statement != null)
                statement.close();
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "리소스 해제 중 오류 발생", e);
        }
    }
}
