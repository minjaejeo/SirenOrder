package jeongsik;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

public class CoffeeOrder {
	// Oracle 데이터베이스 연결 정보
	private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE"; // Oracle 서버 주소와 포트
	private static final String USER = "c##salmon"; // 데이터베이스 사용자 이름
	private static final String PASSWORD = "1234"; // 데이터베이스 비밀번호
	private static final Logger logger = Logger.getLogger(CoffeeOrder.class.getName());

	// 데이터베이스에서 커피 메뉴를 가져오는 메서드
	public List<CoffeeMenu> getCoffeeMenuFromDatabase() {
		List<CoffeeMenu> coffeeMenuList = new ArrayList<>();

		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;

		try {
			// Oracle JDBC 드라이브 로드
			Class.forName("oracle.jdbc.driver.OracleDriver");

			// 데이터베이스 연결
			connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);

			// SQL 쿼리 작성
			String query = "SELECT * FROM COFFEEMENU";
			statement = connection.prepareStatement(query);

			// 쿼리 실행 및 결과 처리
			resultSet = statement.executeQuery();

			int number = 1; // 커피 메뉴의 순서를 나타내는 변수

			while (resultSet.next()) {

				String name = resultSet.getString("COFFEENAME");
				String size = resultSet.getString("COFFEESIZE");
				int price = resultSet.getInt("PRICE");
				boolean isIced = resultSet.getInt("ICEORHOT") == 1;
				boolean hasSyrup = resultSet.getInt("SYRUPADDED") == 1;
				boolean isTakeout = resultSet.getInt("STOREORTAKEOUT") == 1;
				
				

				// CoffeeMenu 객체 생성 및 리스트에 추가

				CoffeeMenu coffeeMenu = new CoffeeMenu(name, size, price, isIced, hasSyrup, isTakeout, number);
				coffeeMenuList.add(coffeeMenu);
				
				
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.log(Level.SEVERE, "커피 메뉴 조회 중 오류 발생", e);
		} finally {
			// 리소스 해제
			closeResources(resultSet, statement, connection);
		}

		return coffeeMenuList;
	}

	// CoffeeMenu 클래스 정의
	public class CoffeeMenu {

		private String name;
		private String size;
		private int price;
		private boolean isIced;
		private boolean hasSyrup;
		private boolean isTakeout;
		private int number;

		// CoffeeMenu 생성자
		public CoffeeMenu(String name, String size, int price, boolean isIced, boolean hasSyrup, boolean isTakeout,
				int number) {

			this.name = name;
			this.size = size;
			this.price = price;
			this.isIced = isIced;
			this.hasSyrup = hasSyrup;
			this.isTakeout = isTakeout;
			this.number = number;
		}

		// name 필드에 대한 getter 메서드
		public String getName() {
			return name;

		}

		// size 필드에 대한 getter 메서드
		public String getSize() {
			return size;
		}

		// price 필드에 대한 getter 메서드
		public int getPrice() {
			return price;
		}

		// isIced 필드에 대한 getter 메서드
		public boolean isIced() {
			return isIced;
		}

		// hasSyrup 필드에 대한 getter 메서드
		public boolean hasSyrup() {
			return hasSyrup;
		}

		// isTakeout 필드에 대한 getter 메서드
		public boolean isTakeout() {
			return isTakeout;
		}

		// number 필드에 대한 getter 메서드
		public int getNumber() {
			return number;
		}

	}

	// 서버와의 통신을 위한 메서드
	public void communicateWithServer() {
		// 서버와 통신하는 코드 작성
	}

	// 커피를 주문하는 메서드
	public void orderCoffee(BufferedReader stdIn, PrintWriter out) throws IOException {

		int menuNumber = 0;
		CoffeeOrder.CoffeeMenu selectedCoffee = null;
		List<CoffeeMenu> coffeeMenuList = getCoffeeMenuFromDatabase(); // 데이터베이스로부터 커피 메뉴를 가져옴

		// 커피 메뉴 출력
		System.out.println("주문할 커피 메뉴:");
		for (int i = 0; i < coffeeMenuList.size(); i++) {
			CoffeeMenu coffee = coffeeMenuList.get(i);
			System.out.println((i) + coffee.getNumber() + ". " + coffee.getName() + "-" + coffee.getPrice() + "원");
		}

		// 메뉴 번호 입력 및 유효성 검증
		while (true) {
			// 사용자로부터 주문할 메뉴를 입력 받습니다.
			System.out.println("주문할 커피 메뉴 번호를 입력하세요. >>");
			try {

				menuNumber = Integer.parseInt(stdIn.readLine());

				// 선택된 메뉴를 주문합니다.
				if (menuNumber < 1 || menuNumber > coffeeMenuList.size()) {
					System.out.println("잘못된 메뉴 번호입니다. 다시 입력해주세요.");
				} else {
					selectedCoffee = coffeeMenuList.get(menuNumber - 1);
					break; // 유요한 주문일경우 반복문 종료
				}
			} catch (NumberFormatException e) {
				System.out.println("숫자를 입력해주세요.");
			}
		}
		// 메뉴번호가 유효한 경우, 추가 주문 정보를 입력받습니다.

		System.out.println("커피 사이즈 선택 (쇼트 / 톨 / 그란데 / 벤티:) >>");
		String size = stdIn.readLine();
		System.out.println("아이스로 하시겠습니까?>> (네/아니오): ");
		boolean isIced = "네".equals(stdIn.readLine());
		System.out.println("시럽을 추가하시겠습니까?>> (네/아니오): ");
		boolean hasSyrup = "네".equals(stdIn.readLine());
		System.out.println("테이크아웃으로 하시겠습니까?>> (네/아니오): ");
		boolean isTakeout = "네".equals(stdIn.readLine());

		// 주문 정보 JSON 객체로 생성
		JSONObject orderDetails = new JSONObject();
		orderDetails.put("type", "order");
		orderDetails.put("number", selectedCoffee.getNumber());
		orderDetails.put("name", selectedCoffee.getName());
		orderDetails.put("price", selectedCoffee.getPrice());
		orderDetails.put("size", size);
		orderDetails.put("isIced", isIced);
		orderDetails.put("hasSyrup", hasSyrup);
		orderDetails.put("isTakeout", isTakeout);

		out.println(orderDetails.toString()); // 서버로 주문 정보 전송
		System.out.println((selectedCoffee.getName()) + (isIced ? ",아이스" : "핫") + (hasSyrup ? ", 시럽 추가" : "")
				+ (isTakeout ? ", 테이크아웃" : "") + "를 주문하셨습니다."); // 사용자에게 주문정보 전송
		// 주문 처리 등의 작업을 이어서 수행할 수 있습니다.
		
		// 추가 : 주문 정보를 데이터베이스에 저장
	    saveOrderlistToDatabase(selectedCoffee.getName(), size, selectedCoffee.getPrice(), isIced, hasSyrup, isTakeout);
	}
	// 추가 : 주문 내역을 데이터베이스에 저장하는 메서드
	private void saveOrderlistToDatabase(String menuName, String size, int price, boolean isIced, boolean hasSyrup, boolean isTakeout) {
	    try {
	        // 데이터베이스 연결
	        Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);

	        // SQL 쿼리 작성
	        String insertQuery = "INSERT INTO ORDERLIST (COFFEENAME, COFFEESIZE, PRICE, ISICED, HASSYRUP, ISTAKEOUT) VALUES (?, ?, ?, ?, ?, ?)";
	        PreparedStatement insertStatement = connection.prepareStatement(insertQuery);

	        // 쿼리에 주문 정보 설정
	        insertStatement.setString(1, menuName);
	        insertStatement.setString(2, size);
	        insertStatement.setInt(3, price);
	        insertStatement.setInt(4, isIced ? 1 : 0);
	        insertStatement.setInt(5, hasSyrup ? 1 : 0);
	        insertStatement.setInt(6, isTakeout ? 1 : 0);

	        // 쿼리 실행
	        insertStatement.executeUpdate();

	        // 리소스 해제
	        insertStatement.close();
	        connection.close();
	    } catch (SQLException e) {
	        logger.log(Level.SEVERE, "주문 정보를 데이터베이스에 저장하는 도중 오류 발생", e);
	    }
	}

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