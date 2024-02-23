package jeongsik;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class PointCharge {
	// Oracle 데이터베이스 연결 정보
	private static final String DB_URL = "jdbc:oracle:thin:@192.168.0.33:1521:XE"; // Oracle 서버 주소와 포트
	private static final String USER = "c##salmon"; // 데이터베이스 사용자 이름
	private static final String PASSWORD = "1234"; // 데이터베이스 비밀번호
	private static final Logger logger = Logger.getLogger(Login.class.getName());

	private Map<String, String> sessions;

	public PointCharge(Map<String, String> sessions) {
		this.sessions = sessions;
	}

	// 로그인 성공한 클라이언트의 아이디가 세션으로 인증되어있고 그아이디를 가져와서 해당 포인트 조회
	public void chargePoint(BufferedReader stdIn, PrintWriter out, String sessionID) throws ParseException {

		try {

			String username = sessionID; // 세션에서 사용자 ID가져오기
			if (username == null) {
				out.println("세션 인증에 실패했습니다.");
				return;
			}

			int currentPoint = getPointFromDatabase(username); // 현재 포인트 조회
			if (currentPoint == -1) {
				out.println("데이터베이스 오류로 인해 포인트를 조회할 수 없습니다.");
				return; // 데이터베이스 오류 발생 시
			}

			// 포인트 충전 여부 확인
			while (true) {
				System.out.println("포인트를 충전하시겠습니까? (예/아니오): ");
				String response = stdIn.readLine();

				if ("예".equals(response)) {
					break; // 충전절차 진행
				} else if ("아니오".equals(response)) {
					System.out.println("포인트 충전을 취소했습니다.");

					JSONObject chargeInfo = new JSONObject();
					chargeInfo.put("type", "chargeResult");
					chargeInfo.put("포인트충전", "취소");
					chargeInfo.put("username", username);
					// chargeInfo.put("chargeAmount", chargeAmount);
					// chargeInfo.put("newPoint", newPoint);
					out.println(chargeInfo.toJSONString());

					out.flush();
					return; // 로그인 메누로 돌아가는 코드 추가 필요
				} else {
					System.out.println("잘못된 입력입니다. 다시 입력해주세요.");
					// 반복문 계속 실행하여 올바른 입력 요청
				}
			}

			int chargeAmount = 0;

			while (true) {

				// 충전할 금액 입력받음
				System.out.println("충전할 금액을 입력하세요: ");
				String input = stdIn.readLine();// 사용자 입력받기
				try {
					chargeAmount = Integer.parseInt(input); // 문자열을 정수로 변환
					// 금액이 양수인지 검증
					if (chargeAmount > 0) {
						break; // 금액이 유효하면 반복문 종료
					} else {
						System.out.println("잘못된 입력입니다. 다시 입력해주세요.");
					}
				} catch (NumberFormatException e) {
					System.out.println("잘못된 입력입니다. 숫자를 입력해주세요.");
				}
			}
			// 사용자로부터 충전 금액 입력받음

			// 새로운 포인트 계산 및 업데이트
			int newPoint = currentPoint + chargeAmount; // 새로운 포인트 계산
			if (updatePointInDatabase(newPoint, username)) {// 데이터베이스 업데이트 성공시
				System.out.println("충전 완료. 현재 포인트 잔액: " + newPoint + "원");

				// 충전 정보를 JSON 객체로 생성하여 로그 또는 서버로 전송
				JSONObject chargeInfo = new JSONObject();
				chargeInfo.put("type", "chargeResult");
				chargeInfo.put("username", username);
				chargeInfo.put("chargeAmount", chargeAmount);
				chargeInfo.put("newPoint", newPoint);
				out.println(chargeInfo.toJSONString());
				// 서버로 충전 완료 정보 전송
				out.flush();

			} else {
				System.out.println("포인트 충전에 실패했습니다.");
			}
		} catch (IOException e) {
			System.out.println("입력 오류가 발생했습니다.");
		} catch (NumberFormatException e) {
			System.out.println("유효하지 않은 금액입니다. 숫자를 입력해주세요.");
		}

	}

	public int getPointFromDatabase(String username) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		int point = 0;

		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
			String query = "SELECT points FROM users WHERE username = ?";
			statement = connection.prepareStatement(query);
			// 사용자명에 따라 포인트를 가져옴
			statement.setString(1, username);

			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				point = resultSet.getInt("points");
				System.out.println("현재 포인트 잔액 : " + point + "원");
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.log(Level.SEVERE, "포인트 조회 중 오류 발생", e);
		} finally {
			closeResources(resultSet, statement, connection);
		}

		return point;
	}

	private boolean updatePointInDatabase(int newPoint, String username) {
		Connection connection = null;
		PreparedStatement statement = null;
		boolean success = false;

		try {

			// 오라클 드라이버 로드
			Class.forName("oracle.jdbc.driver.OracleDriver");
			// 데이터베이스 연결
			connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
			// 포인트 업데이트 쿼리 준비
			String query = "UPDATE users SET points = ? WHERE username = ?";
			statement = connection.prepareStatement(query);

			// 쿼리 매개변수 설정
			// 로그인된 클라이언트 아이디를 확인하고 그에 따라 포인트 업데이트
			statement.setInt(1, newPoint);
			statement.setString(2, username);

			// 쿼리 실행 및 결과 확인
			int rowsUpdated = statement.executeUpdate();
			success = rowsUpdated > 0; // 업데이트된 행이 있으면 성공
		} catch (ClassNotFoundException | SQLException e) {
			logger.log(Level.SEVERE, "포인트 업데이트 중 오류 발생", e);
		} finally {
			closeResources(null, statement, connection);
		}
		return success;
	}

	private void closeResources(ResultSet resultSet, PreparedStatement statement, Connection connection) {
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