package jeongsik;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server {
    private static final int PORT = 9999; // 서버가 수신 대기할 포트 번호
    private static final Logger logger = Logger.getLogger(Server.class.getName()); // 로깅을 위한 로거
    
 // 세션을 위한 해시맵 선언
 	private static HashMap<String, String> sessions = new HashMap<>();
 	
    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool(); // 클라이언트 처리를 위한 스레드 풀

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("사이렌 오더 서버 시작"); // 서버 시작 로깅

            while (true) { // 무한 루프로 클라이언트 접속 대기
                Socket clientSocket = serverSocket.accept(); // 클라이언트 연결 수락
                logger.info("클라이언트가 접속함: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                executor.execute(clientHandler); // 클라이언트 처리를 위한 새 스레드 할당
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "서버 실행 중 오류 발생", e);
        } finally {
            executor.shutdown(); // 서버 종료 시 스레드 풀 종료
        }
    }

    // 클라이언트 요청을 처리하는 핸들러 클래스
    static class ClientHandler implements Runnable {
        private Socket clientSocket; // 클라이언트와의 연결 소켓

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) { // 클라이언트로부터 메시지 수신
                    logger.info("클라이언트로부터의 메시지: " + inputLine);

                    JSONParser parser = new JSONParser();
                    try {
                        JSONObject jsonRequest = (JSONObject) parser.parse(inputLine);
                        String command = (String) jsonRequest.get("type");
                        processCommand(command, jsonRequest, out); // 수신된 명령 처리
                    } catch (ParseException e) {
                        logger.log(Level.SEVERE, "JSON 파싱 중 오류 발생", e);
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("에러", "JSON 파싱 오류");
                        out.println(jsonResponse.toJSONString());
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "클라이언트 핸들링 중 오류 발생", e);
            } finally {
                try {
                    clientSocket.close(); // 클라이언트 소켓 종료
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "클라이언트 소켓 닫기 중 오류 발생", e);
                }
            }
        }

     // 클라이언트로부터 받은 명령을 처리하는 메서드
        private void processCommand(String command, JSONObject jsonRequest, PrintWriter out) {
            // 클라이언트로부터 받은 응답을 저장할 JSON 객체
            JSONObject jsonResponse = new JSONObject();
            
            if (command != null) {
                // 클라이언트로부터 받은 명령에 따라 적절한 처리 수행
            // 클라이언트로부터 받은 명령에 따라 적절한 처리 수행
            switch (command) {
            case "login":
				JSONObject loginResponse = new JSONObject();
				// 로그인 명령 처리
				// jsonRequest에서 username과 password를 추출
				String usernameLogin = (String) jsonRequest.get("userid");
				String passwordLogin = (String) jsonRequest.get("password");
				// Login 클래스의 login 메서드를 호출하여 로그인 처리
				// 결과는 JSON 문자열 형태로 반환됨
				String loginResult = Login.login(usernameLogin, passwordLogin);
				if (loginResult.contains("성공")) {
					// 세션 생성 및 저장
					String sessionID = usernameLogin; // 클라이언트의 아이디를 세션 ID로 사용
					sessions.put(sessionID, usernameLogin); // 세션을 Map에 저장

					// JSON 응답 구성
					
					loginResponse.put("type", "login_response");
					loginResponse.put("로그인상태", "성공");
					//loginResponse.put("result", loginResult);
					loginResponse.put("sessionID", sessionID);
				}else {
					//로그인 실패 처리
					//json 응답 구성
					//loginResponse.put("type","login_response");
					loginResponse.put("로그인상태", "실패");
					loginResponse.put("message","잘못된 아이디 또는 비빌번호입니다.");
				}
				// 처리 결과를 클라이언트에게 전송
				out.println(loginResponse.toJSONString());
					break;
					
                case "signup":
                    // 회원가입 명령 처리
                    // jsonRequest에서 username과 password를 추출
                    String usernameSignup = (String) jsonRequest.get("userid");
                    String passwordSignup = (String) jsonRequest.get("password");
                    // Signup 클래스의 register 메서드를 호출하여 회원가입 처리
                    // 결과는 JSON 문자열 형태로 반환됨
                    String signupResult = Signup.register(usernameSignup, passwordSignup);
                    // 처리 결과를 클라이언트에게 전송
                    out.println(signupResult);
                    break;
                    
                case "order":
                	//커피 주문 명령 처리
                	// jsonRequest에서 주문 정보 추출
                	String menuName = (String) jsonRequest.get("menuName");
                    String size = (String) jsonRequest.get("size");
                    boolean isIced = (boolean) jsonRequest.get("isIced");
                    boolean hasSyrup = (boolean) jsonRequest.get("hasSyrup");
                    boolean isTakeout = (boolean) jsonRequest.get("isTakeout");
                	
                    // 여기에 주문 정보에 대한 처리 로직 추가
                    // 예를 들어 주문 정보를 데이터베이스에 저장하거나 주문을 처리하는 기능을 수행할 수 있습니다.
                    
                    // 처리 결과를 클라이언트에게 전송
                    JSONObject orderResponse = new JSONObject();
                    orderResponse.put("type", "order_response");
                    orderResponse.put("message", "주문이 성공적으로 접수되었습니다.");
                    out.println(orderResponse.toJSONString());
                    break;
                    
                case "couponRegistration":
                    // 회원가입 명령 처리
                    // jsonRequest에서 coffeecoupon을 추출
                    String cc = (String) jsonRequest.get("coupon");
                    // CouponRegistation 클래스의 Registration 메서드를 호출하여 쿠폰등록 처리
                    // 결과는 JSON 문자열 형태로 반환됨
                    String ca = CouponRegistration.Registration(cc);
                    // 처리 결과를 클라이언트에게 전송
                    out.println(ca);
                    break;
                    
                case "view_order":
                	
                case "select_store":
                	// 4. 매장 선택 명령 처리
                	String selectedStoreName = (String)jsonRequest.get("storeName");
                	String selectedStoreLocation = (String) jsonRequest.get("location");
                	
                	// 처리 결과를 클라이언트에게 전송
                	jsonResponse.put("type", "select_store_response");
                	jsonResponse.put("message", "매장이 성공적으로 선택되었습니다.");
                	out.println(jsonResponse.toJSONString());
                	break;
                case "point_charge":
					// 포인트 충전 명령 처리
					String sessionID = (String) jsonRequest.get("sessionID");
					int chargeAmount = Integer.parseInt((String) jsonRequest.get("amount"));

					boolean chargeSuccess = true; // 포인트 충전 성공 여부를 나타내닌 변수
					JSONObject pointResponse = new JSONObject();
					if (chargeSuccess) {
						
						pointResponse.put("type", "point_charge_response");
						pointResponse.put("status", "success");
						pointResponse.put("message", "포인트가 성공적으로 충전되었습니다.");
					} else {
						pointResponse.put("type", "point_charge_response");
						pointResponse.put("status", "failure");
						pointResponse.put("message", "포인트 충전에 실패했습니다.");
					}

					// 처리 결과를 클라이언트에게 전송
					out.println(pointResponse.toJSONString());
					break;
	
                    
                default:
                    // 알 수 없는 명령에 대한 처리
                    // 에러 메시지를 JSON 객체에 추가
                    jsonResponse.put("에러", "알 수 없는 명령");
                    // 에러 메시지를 클라이언트에게 전송
                    out.println(jsonResponse.toJSONString());
                    break;
            }
            } else {
                // 명령이 null인 경우 오류 처리
                // 에러 메시지를 JSON 객체에 추가
                jsonResponse.put("에러", "명령이 null입니다.");
                // 에러 메시지를 클라이언트에게 전송
                out.println(jsonResponse.toJSONString());
            }
        }
    }
    // 클라이언트로부터 받은 메시지를 처리하는 메서드
    private void handleClientInput(String input, PrintWriter out) throws ParseException {
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(input);

            // 클라이언트로부터 받은 메시지를 처리하는 로직을 추가하세요
            // 이 예제에서는 간단히 클라이언트에게 다시 응답을 보내는 것으로 대체합니다.
            out.println("서버로부터의 응답: " + input);
        } catch (ParseException e) {
            System.out.println("클라이언트로부터 받은 메시지를 파싱하는 중 오류 발생: " + e.getMessage());
        }
    }
}