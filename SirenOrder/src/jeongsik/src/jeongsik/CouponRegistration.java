package jeongsik;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

public class CouponRegistration {
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
    public static String Registration(String coffeecupon) {
    	//DB에 연결하는 객체 선언
    	Connection connection = null;
    	
    	//미리 컴파일된 SQL 문을 만드는 객체 선언(쿼리문을 실행하기 전에 준비)
    	PreparedStatement statement = null;
    	
    	// 쿼리문의 결과값을 저장하는 객체를 선언
    	ResultSet resultSet = null;
    	
    	JSONObject jsonResponse = new JSONObject();
    	
    	try {
    		// 해당 클래스 전체 경로에서 드라이버를 로드함
    		Class.forName("oracle.jdbc.driver.OracleDriver");
    		
    		// 드라이버에 연결
    		connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
    		
    		// SQL 쿼리문: COUNT 함수를 사용하여 해당 조건(입력한 ID와 동일한 이름이 있는지 갯수를 셈)에 맞는 레코드의 수를 세어줌
    		// 나중에 '?'에 입력한 쿠폰이 들어감
    		String checkQuery = "SELECT COUNT(*) FROM COFFEECOUPON WHERE COFFEECOUPON = ?";
    		
    		// 위에 작성된 SQL 쿼리를 PreparedStatement 객체를 사용해서 미리 컴파일 시킴
    		statement = connection.prepareStatement(checkQuery);
    		
    		// 위에 첫번째(어차피 '?'는 한개뿐이긴 함)'?'에 입력한 cofffeecoupon을 담음
    		// 'coffeecupon' 변수의 값은 client에서 'Registration' 변수를 호출할때 채워질거임
    		statement.setString(1, coffeecupon);
    		
    		// executeQuery함수를 사용하여 미리 컴파일하여 하였던 check쿼리문을 실행 시킴
    		// 결과를 resultSet에 저장
    		resultSet = statement.executeQuery();
    		
    		// next()를 쓰면 행을 이동하면서 입력한 쿠폰과 같은 이름이 존재하는지 확인
    		if(resultSet.next() && resultSet.getInt(1) > 0) {
    			
    			// 쿠폰 등록이 성공했는지 JSON 형식으로 클라이언트에게 전달하기 위해 사용
    			jsonResponse.put("쿠폰 조회 결과", "실패>>이미 사용된 쿠폰입니다."); 
    			
    			
    			// 클라이언트에게 "쿠폰 조회 결과, "실패>>이미 사용된 쿠폰입니다" 메시지 전송
    			return jsonResponse.toJSONString();
    		}
    		
    		// INSERT문을 사용하여 새로운 사용자를 데이터베이스에 등록하는 SQL 쿼리 정의
    		// '?' 는 입력한 쿠폰이 채워질거임
    		String insertQurery = "INSERT INTO COFFEECOUPON (COFFEECOUPON) VALUES(?)";
    		
    		// 미리 컴파일된 SQL 문이 담겨있는 'insertQuery'라는 변수를 담음
    		statement = connection.prepareStatement(insertQurery);
    		
    		// 위에 첫번째 ?에 내가 입력한 username을 저장
    		statement.setString(1, coffeecupon);
    		
    		// DB에 대한 업데이트를 실행하고 username과 password가 새로 실행 될 시
    		// 행이 늘어나므로 그 행의 갯수를 'rowslnserted' 변수에 저장함
    		int rowslnserted = statement.executeUpdate();
    		
    		// DB안에 coffeecoupon 테이블의 늘어난 행의 갯수 'rowslnserted'이 1개 이상일 경우 0개이하면 실패
    		jsonResponse.put("쿠폰 조회 결과", rowslnserted>0? "성공>> 쿠폰 등록이 완료 되었습니다.": "실패" );
    		
    	// 	register 메서드에서 데이터베이스 연결과 관련된 예외인 ClassNotFoundException과 SQLException을 처리
    	}catch(ClassNotFoundException|SQLException e) {
    		
    	 // 발생한 예외와 회원가입 중 오류발생이라는 메시지와 함께 예외정보(e)를 로그에 저장함
    	 logger.log(Level.SEVERE, "쿠폰 사용 중 오류 발생", e);
    	 
    	 // Json형식으로 client에 오류라고 알려줌
    	 jsonResponse.put("쿠폰 조회 상태", "오류");
    		
    	}finally {
    		// 리소스 해제
            closeResources(resultSet, statement, connection);
    	}
    	
    	// 회원가입 상태를 담고있는 'jsonRespone' 객체를 json형식의 문자열로 변환하여 반환함
    	return jsonResponse.toJSONString();
    }
    
    /**
     * 데이터베이스 관련 리소스를 안전하게 닫는 유틸리티 메서드.
     *
     * @param resultSet 결과 집합
     * @param statement SQL 문
     * @param connection 데이터베이스 연결
     */
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