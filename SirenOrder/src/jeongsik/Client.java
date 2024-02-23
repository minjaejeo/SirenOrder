package jeongsik;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Client {
	// 서버 주소와 포트 번호 설정
	private static final String serverAddress = "localhost";
	private static final int serverPort = 9999;
	private static String sessionID;
	private static final Map<String, String> sessions = new HashMap<>(); // 세션을 관리하는 Map

	// 클라이언트에서 서버에게 보내는 메시지를 로깅하는 메서드
	private static void logSentMessage(String message) {
		System.out.println("클라이언트로부터의 메시지 전송: " + message);
	}

	// 서버로부터 받은 응답을 로깅하는 메서드
	private static void logReceivedResponse(String response) {
		System.out.println("서버로부터의 응답: " + response);
	}

	public static void main(String[] args) throws ParseException {
		try (Socket socket = new Socket(serverAddress, serverPort);
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {
			System.out.println("사이렌오더 서버에 연결되었습니다.");

			// 사용자 입력을 통해 로그인, 회원가입, 종료 중 하나를 선택할 수 있는 메뉴 반복
			while (true) {
				displayMenu();
				String userInput = stdIn.readLine();

				switch (userInput) {
				case "1":
					login(stdIn, out, in); // 로그인 처리
					break;
				case "2":
					signup(stdIn, out, in); // 회원가입 처리
					break;
				case "3":
					System.out.println("사이렌오더 서버 연결을 종료합니다."); // 프로그램 종료
					return;
				default:
					System.out.println("잘못된 입력입니다. 다시 선택해주세요."); // 잘못된 입력 처리
				}
			}
		} catch (IOException e) {
			System.out.println("서버 연결에 실패했습니다: " + e.getMessage()); // 서버 연결 실패 처리
		}
	}

	// 사용자에게 메뉴 옵션을 표시하는 메서드
	private static void displayMenu() {
		System.out.println("1. [로그인]");
		System.out.println("2. [회원가입]");
		System.out.println("3. [종료]");
		System.out.println("메뉴를 선택하세요. >> ");
	}

	// 로그인 기능을 처리하는 메서드
	private static void login(BufferedReader stdIn, PrintWriter out, BufferedReader in)
			throws IOException, ParseException {
		JSONObject json = new JSONObject();
		json.put("type", "login");
		json.put("userid", promptForInput(stdIn, "아이디를 입력하세요>>"));
		json.put("password", promptForInput(stdIn, "패스워드를 입력하세요>>"));

		out.println(json.toString()); // 서버의 로그인 요청 전송

		handleServerResponse(stdIn, out, in); // 로그인 응답 처리

	}

	// 회원가입 기능을 처리하는 메서드
	private static void signup(BufferedReader stdIn, PrintWriter out, BufferedReader in)
			throws IOException, ParseException {
		JSONObject json = new JSONObject();
		json.put("type", "signup");
		json.put("userid", promptForInput(stdIn, "사용할 아이디를 입력하세요"));
		json.put("password", promptForInput(stdIn, "사용할 패스워드를 입력하세요"));

		out.println(json.toString());
		handleServerResponse(stdIn, out, in); // 서버로부터의 응답 처리
	}

	// 사용자로부터 입력을 요청하는 메서드
	private static String promptForInput(BufferedReader stdIn, String prompt) throws IOException {
		System.out.println(prompt);
		return stdIn.readLine();
	}

	public static void handleServerResponse(BufferedReader stdIn, PrintWriter out, BufferedReader in)
			throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		String responseString = in.readLine();

		if (responseString != null) {
			try {
				JSONObject response = (JSONObject) parser.parse(responseString);
				System.out.println("서버 응답: " + response.toJSONString());

				// 로그인 응답 처리
				if (response.containsKey("로그인상태")
						|| (response.containsKey("type") && response.get("type").equals("login_response"))) {
					String loginStatus;
					String sessionID;

					// "로그인 상태"와 "세션 ID" 추출
					if (response.containsKey("로그인상태")) {
						loginStatus = (String) response.get("로그인상태");
						sessionID = (String) response.get("sessionID");
					} else {
						JSONObject resultObj = (JSONObject) new JSONParser().parse((String) response.get("result"));
						loginStatus = (String) resultObj.get("로그인상태");
						sessionID = (String) response.get("sessionID");
					}

					// 로그인 상태에 따른 처리
					handleLoginResponse(loginStatus, sessionID, stdIn, out, in);
					// 로그인 상태에 따른 처리
//	                if (loginStatus.equals("성공")) {
//	                    System.out.println("로그인 상태: " + loginStatus);
//	                    System.out.println("세션 ID: " + sessionID);
//	                } else {
//	                    // 실패 시 처리
//	                    System.out.println("로그인에 실패했습니다.");

				} else {
					// 올바르지 않은 응답이면 처리하지 않음
					// System.out.println("올바르지 않은 서버 응답입니다.");
				}
			} catch (ParseException e) {
				System.out.println("서버로부터 응답을 파싱하는 중 오류가 발생했습니다: " + e.getMessage());
			} catch (NullPointerException e) {
				System.out.println("서버 응답에서 필요한 정보가 없습니다.");
			}
		}
	}

	// 로그인 응답 처리 메서드
	private static void handleLoginResponse(String loginStatus, String sessionID, BufferedReader stdIn, PrintWriter out,
			BufferedReader in) throws IOException, ParseException {
		// 로그인 성공시 처리
		if (loginStatus.equals("성공")) {
			System.out.println("로그인에 성공했습니다.");
			handleLoginSuccess(stdIn, out, in, sessionID);
		} else {
			// 로그인 실패시 처리
			switch (loginStatus) {
			case "존재하지 않는 아이디":
				System.out.println("해당 아이디는 가입된 아이디가 아닙니다.");
				break;
			case "비밀번호 오류":
				System.out.println("비밀번호가 틀렸습니다. 다시 확인해주세요.");
				break;
			default:
				System.out.println("로그인에 실패했습니다. 다시 시도해주세요.");
				break;
			}
		}
	}

//	public static void handleServerResponse(BufferedReader stdIn, PrintWriter out, BufferedReader in)
//			throws IOException, ParseException {
//		JSONParser parser = new JSONParser();
//		String responseString = in.readLine();
//
//		if (responseString != null) {
//		    try {
//		        JSONObject response = (JSONObject) parser.parse(responseString);
//		        System.out.println("서버 응답: " + response.toJSONString());
//		        // 로그인 응답 처리
//		        if (response.containsKey("로그인상태") || (response.containsKey("type") && response.get("type").equals("login_response"))) {
//		            String loginStatus;
//		            String sessionID;
//
//		            // "로그인 상태"와 "세션 ID" 추출
//		            if (response.containsKey("로그인상태")) {
//		                loginStatus = (String) response.get("로그인상태");
//		                sessionID = (String) response.get("sessionID");
//		            } else {
//		                JSONObject resultObj = (JSONObject) new JSONParser().parse((String) response.get("result"));
//		                loginStatus = (String) resultObj.get("로그인상태");
//		                sessionID = (String) response.get("sessionID");
//		            }
//
//		            // 로그인 성공시 처리
//		            if (loginStatus.equals("성공")) {
//		                System.out.println("로그인에 성공했습니다.");
//		                handleLoginSuccess(stdIn, out, in, sessionID);
//		            } else {
//		                // 로그인 실패시 처리
//		                if (loginStatus.equals("존재하지 않는 아이디")) {
//		                    System.out.println("해당 아이디는 가입된 아이디가 아닙니다.");
//		                } else if (loginStatus.equals("비밀번호 오류")) {
//		                    System.out.println("비밀번호가 틀렸습니다. 다시 확인해주세요.");
//		                } else {
//		                    System.out.println("로그인에 실패했습니다. 다시 시도해주세요.");
//		                }
//		            }
//		        } else {
//		            // 올바르지 않은 응답이면 처리하지 않음
//		            //System.out.println("올바르지 않은 서버 응답입니다.");
//		        }
//		    } catch (ParseException e) {
//		        System.out.println("서버로부터 응답을 파싱하는 중 오류가 발생했습니다: " + e.getMessage());
//		    }
//		}
//	}

//		if (responseString != null) {
//			try {
//				JSONObject response = (JSONObject) parser.parse(responseString);
//				
//				// 로그인 응답 처리
//				if (response.containsKey("로그인상태")) {
//					String loginStatus = (String) response.get("로그인상태");
//					// 로그인 성공시 처리
//					if (loginStatus.equals("성공")) {
//						handleLoginSuccess(stdIn, out, in, (String) response.get("sessionID"));
//						// 세션 아이디를 전달하여 로그인 성공 처리
//
//					} else {
//						// 로그인 실패시 처리
//						System.out.println("로그인에 실패했습니다. 다시 시도해주세요.");
//
//					}
//				} else if (response.containsKey("type") && response.get("type").equals("login_response")) {
//
//					String result = (String) response.get("result");
//					JSONObject resultObj = (JSONObject) new JSONParser().parse(result);
//					String loginStatus = (String) resultObj.get("로그인상태");
//
//					if (loginStatus.equals("성공")) {
//						System.out.println("로그인에 성공했습니다.");
//						handleLoginSuccess(stdIn, out, in, (String) response.get("sessionID"));
//					} else {
//						//로그인 실패시 처리
//						System.out.println("로그인에 실패했습니다. 다시 시도해주세요.");
//					}
//				}else {
//					// 올바른 응답이 아닌 경우
//	                //System.out.println("올바르지 않은 서버 응답입니다.");
//					
//				}
//			} catch (ParseException e) {
//				System.out.println("서버로부터 응답을 파싱하는 중 오류가 발생했습니다: " + e.getMessage());
//			}
//		}
//	}

	private static void handleLoginSuccess(BufferedReader stdIn, PrintWriter out, BufferedReader in, String sessionID)
			throws IOException, ParseException {
		boolean keepRunning = true;
		while (keepRunning) {

			displayPostLoginMenu(); // 로그인 성공 후 메뉴 표시

			String userInput = stdIn.readLine(); // 사용자 입력 받기

			switch (userInput) {
			case "1":
				orderCoffee(stdIn, out, in); // 커피 주문 및 결제 처리
				break;
			case "2":
				viewOrder(stdIn, out, in); // 커피 주문 내역 조회 처리
				break;
			case "3":
				coupon(stdIn, out, in); // 쿠폰등록 처리
				break;
			case "4":
				selectStore(stdIn, out, in);
				break;
			case "5":
				pointCharge(stdIn, out, in, sessionID); // 포인트 충전 처리
				break;
			case "6":
				enterOneChatRoom(stdIn, out, in, sessionID, sessions); // 포인트 충전 처리
				break;
			case "7":
				System.out.println("서비스를 종료합니다.");
				keepRunning = false; // 반복문 종료
				break;
			default:
				System.out.println("잘못된 입력입니다. 다시 선택해주세요.");
				break;
			}
		}
	}

	private static void displayPostLoginMenu() {
		System.out.println("\n로그인중입니다.");
		System.out.println("1. 커피 주문");
		System.out.println("2. 커피 주문 내역 조회");
		System.out.println("3. 쿠폰 등록");
		System.out.println("4. 매장 조회");
		System.out.println("5. 포인트 충전");
		System.out.println("6. 채팅방 이동");
		System.out.println("7. 종료");
		System.out.println("메뉴를 선택하세요. >> ");

	}

	private static void orderCoffee(BufferedReader stdIn, PrintWriter out, BufferedReader in)
			throws IOException, ParseException {
		CoffeeOrder coffeeOrder = new CoffeeOrder(); // CoffeeOrder 객체 생성
		coffeeOrder.orderCoffee(stdIn, out); // 커피 주문 메서드 호출
		// 추가 : 주문 처리 후 결제를 진행합니다.
	}

	private static void coupon(BufferedReader stdIn, PrintWriter out, BufferedReader in)
			throws IOException, ParseException {
		JSONObject json = new JSONObject();
		json.put("type", "couponRegistration");
		json.put("coupon", promptForInput(stdIn, "쿠폰을 입력해주세요"));

		out.println(json.toString());
		handlCouponResponse(stdIn, out, in, sessionID); // 서버로부터의 응답 처리
	}

	private static void handlCouponResponse(BufferedReader stdIn, PrintWriter out, BufferedReader in, String sessionID)
			throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		try {
			String responseString = in.readLine();
			if (responseString != null) {
				JSONObject response = (JSONObject) parser.parse(responseString);
				System.out.println("서버 응답" + response.toJSONString());

				// 로그인 응답 처리
				if (response.containsKey("로그인상태")) {
					String loginStatus = (String) response.get("로그인상태");
					if (loginStatus.equals("성공")) {
						handleLoginSuccess(stdIn, out, in, (String) response.get("sessionID"));
					} else {
						System.out.println("로그인에 실패했습니다. 다시 시도해주세요.");
					}
				}
			} else {
				System.out.println("서버로부터 응답을 받지 못했습니다.");
			}
		} catch (ParseException e) {
			System.out.println("서버로부터 응답을 파싱하는 중 오류가 발생했습니다: " + e.getMessage());
		}

	}

	// 추가 : 커피 주문 조회 메서드
	private static void viewOrder(BufferedReader stdIn, PrintWriter out, BufferedReader in) {
		try {
			ViewOrder viewOrder = new ViewOrder(); // ViewOrder 클래스의 인스턴스 생성
			viewOrder.viewOrders(); // 주문 조회 메서드 호출
		} catch (Exception e) {
			System.out.println("주문 조회 중 오류 발생: " + e.getMessage());
		}
	}

	private static void enterOneChatRoom(BufferedReader stdIn, PrintWriter out, BufferedReader in, String sessionID, Map<String, String> sessions) {
	    // 채팅방 기능 구현
		OneChatRoom oneChatRoom = new OneChatRoom(sessions, stdIn, out, in, sessionID);
		oneChatRoom.startChat();
	}

	// SelectStroe 를 추가하는 메서드 작성
	private static void selectStore(BufferedReader stdIn, PrintWriter out, BufferedReader in)
			throws IOException, ParseException {
		SelectStore selectStore = new SelectStore();
		selectStore.displayStoreList();

	}

	// 추가 : pointCharge 메서드 작성
	private static void pointCharge(BufferedReader stdIn, PrintWriter out, BufferedReader in, String sessionID)
			throws IOException, ParseException {

		// 포인트 충전 객체 생성
		PointCharge json = new PointCharge(sessions);
		// 포인트 충전 메서드 호출
		json.chargePoint(stdIn, out, sessionID);
		// 서버로부터의 응답처리
		handleServerResponse(stdIn, out, in); // 서버로부터의 응답처리
		// 포인트 충전 결과에 관한 JSON 응답을 생성하여 서버로 전송
		JSONObject jsonPointCharge = new JSONObject();
		jsonPointCharge.put("type", "point_charge_response");
		jsonPointCharge.put("status", "success");
		jsonPointCharge.put("message", "포인트가 성공적으로 충전되었습니다.");
		out.println(jsonPointCharge.toJSONString());
		
		
	}

	// 세션 인증 요청을 보내는 메서드
	private static void sendSessionAuthRequest(PrintWriter out, String sessionID) {
		JSONObject json = new JSONObject();
		json.put("type", "session_auth"); // 세션 인증을 요청하는 타입으로 지정
		json.put("sessionID", sessionID); // 클라이언트가 가지고 있는 세션 아이디 전송

		out.print(json.toString()); // 서버에 세션 인증 요청

	}

	// 상태 변수 선언
	private static boolean isSessionAuthenticated = true;

	// isSessionAuthenticated 변수의 값을 반환하는 메서드
	public static boolean getIsSessionAuthenticated() {
		return isSessionAuthenticated;
	}

	// isSessionAuthenticated 변수의 값을 설정하는 메서드
	public static void setIsSessionAuthenticated(boolean authenticated) {
		isSessionAuthenticated = authenticated;
	}

}