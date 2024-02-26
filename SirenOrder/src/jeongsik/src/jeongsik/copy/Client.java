package jeongsik.copy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Client {
   // 서버 주소와 포트 번호 설정
   private static final String serverAddress = "192.168.0.33";
   private static final int serverPort = 9999;
   private static String userId;
   private static final ConcurrentHashMap<String, PrintWriter> sessions = new ConcurrentHashMap<>();
   // 세션을 관리하는 Map

   public static void main(String[] args) throws ParseException {
      try (Socket socket = new Socket(serverAddress, serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

         System.out.println("사이렌오더 서버에 연결되었습니다.");

         // 사용자 입력을 통해 로그인, 회원가입, 종료 중 하나를 선택할 수 있는 메뉴 반복
         Thread userInputThread = new Thread(() -> {
            String userInput;
            try {
               while (true) {
                  displayMenu();
                  userInput = stdIn.readLine();

                  switch (userInput) {
                  case "1":
                     login(stdIn, out, in); // 로그인 처리
                     break;
                  case "2":
                     signup(stdIn, out, in); // 회원가입 처리
                     break;
                  case "3":
                     System.out.println("사이렌오더 서버 연결을 종료합니다."); // 프로그램 종료
                     socket.close();
                     return;
                  default:
                     System.out.println("잘못된 입력입니다. 다시 선택해주세요."); // 잘못된 입력 처리
                  }
               }
            } catch (IOException e) {
               System.out.println("서버 연결에 실패했습니다: " + e.getMessage()); // 서버 연결 실패 처리
            } catch (ParseException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
         });
         userInputThread.start();

         // 메인 스레드가 종료되지 않도록 대기
         userInputThread.join();

      } catch (IOException | InterruptedException e) {
         System.err.println("서버 연결에 실패했습니다: " + e.getMessage()); // 서버 연결 실패 처리
      }
   }

   // 사용자에게 메뉴 옵션을 표시하는 메서드
   private static void displayMenu() {
      System.out.println("1. [로그인]");
      System.out.println("2. [회원가입]");
      System.out.println("3. [종료]");
      System.out.println("메뉴를 선택하세요. >> ");
   }

   // 서버로부터의 응답을 받아들이는 쓰레드
   static class ResponseReceiver extends Thread {
      private BufferedReader in;

      public ResponseReceiver(BufferedReader in) {
         this.in = in;
      }
   }

   // 로그인 기능을 처리하는 메서드
   private static void login(BufferedReader stdIn, PrintWriter out, BufferedReader in)
         throws IOException, ParseException {
      JSONObject json = new JSONObject();
      json.put("type", "login");
      json.put("userId", promptForInput(stdIn, "아이디를 입력하세요>>"));
      json.put("password", promptForInput(stdIn, "패스워드를 입력하세요>>"));

      out.println(json.toString()); // 서버의 로그인 요청 전송
      handleServerResponse(stdIn, out, in); // 로그인 응답 처리

   }

   // 회원가입 기능을 처리하는 메서드
   private static void signup(BufferedReader stdIn, PrintWriter out, BufferedReader in)
         throws IOException, ParseException {
      JSONObject json = new JSONObject();
      json.put("type", "signup");
      json.put("userId", promptForInput(stdIn, "사용할 아이디를 입력하세요"));
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
            if (response.containsKey("로그인상태")) {
               String loginStatus = (String) response.get("로그인상태");
               ;
               String userId = (String) response.get("userId");
               ;

               // 로그인 상태가 "성공"인 경우에만 로그인메뉴 호출
               if ("성공".equals(loginStatus)) {
                  handleLoginSuccess(stdIn, out, in, userId);
                  sendSessionAuthRequest(out, userId);
               } else {
                  // 로그인 실패 또는 다른 상태에 대한 처리
                  System.out.println("로그인실패: " + loginStatus);
               }

            } else {
               // 로그인 외 다른 응답에 대한 처리
               System.out.println("응답 처리 불가: 응답 형식이 예상과 다릅니다.");
            }
         } catch (ParseException e) {
            System.out.println("서버로부터 응답을 파싱하는 중 오류가 발생했습니다: " + e.getMessage());
         } catch (NullPointerException e) {
            System.out.println("서버 응답에서 필요한 정보가 없습니다.");
         }
      }
   }

   private static void handleLoginSuccess(BufferedReader stdIn, PrintWriter out, BufferedReader in, String userId)
         throws IOException, ParseException {
      boolean keepRunning = true;
      while (keepRunning) {
         startMessageReceiverThread(in); // 서버로부터 메시지 수신 스레드 시작
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
            pointCharge(stdIn, out, in, userId); // 포인트 충전 처리
            break;
         case "6":
            enterOneChatRoom(stdIn, out, in, userId, sessions); // 채팅 처리
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
      payment();
   }

   private static void coupon(BufferedReader stdIn, PrintWriter out, BufferedReader in)
         throws IOException, ParseException {
      JSONObject json = new JSONObject();
      json.put("type", "couponRegistration");
      json.put("coupon", promptForInput(stdIn, "쿠폰을 입력해주세요"));

      out.println(json.toString());
      handlCouponResponse(stdIn, out, in, userId); // 서버로부터의 응답 처리
   }

   private static void handlCouponResponse(BufferedReader stdIn, PrintWriter out, BufferedReader in, String userId)
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
                  handleLoginSuccess(stdIn, out, in, (String) response.get("userId"));
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
   
   // 결제 완료를 출력하는 메서드
      private static void payment()throws IOException {
              Scanner scanner = new Scanner(System.in);

              int payChoice = 0;
              do {
                  String pay;
                  System.out.println("결제 수단 :");
                  System.out.println("1. 스타벅스 카드");
                  System.out.println("2. 신용카드");
                  System.out.println("3. 쿠폰");
                  System.out.println("메뉴를 선택하세요. >> ");
                  payChoice = scanner.nextInt();
                  switch (payChoice) {
                      case 1:
                          pay = "스타벅스 카드";
                          System.out.println(pay+ "결제가 완료되었습니다.");
                          break;
                      case 2:
                          pay = "신용카드";
                          System.out.println(pay+ "결제가 완료되었습니다.");
                          break;
                      case 3:
                          pay = "쿠폰";
                          System.out.println(pay+ "결제가 완료되었습니다.");
                          break;
                      default:
                          System.out.println("결제 중 오류가 발생했습니다.");
                          scanner.nextLine();
                          break;
                  }
              } while (payChoice != 1 && payChoice != 2 && payChoice != 3);
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

   private static void enterOneChatRoom(BufferedReader stdIn, PrintWriter out, BufferedReader in, String userId,
         ConcurrentHashMap<String, PrintWriter> sessions) {
      JSONObject message = new JSONObject();
      message.put("type", "enterOneChatRoom");
      message.put("userId", userId); // 아이디 추가

      out.println(message.toJSONString());
      out.flush();
      // 채팅방 기능 구현
      OneChatRoom oneChatRoom = new OneChatRoom(sessions, stdIn, out, in, userId);
      oneChatRoom.startChat();

   }

   // SelectStroe 를 추가하는 메서드 작성
   private static void selectStore(BufferedReader stdIn, PrintWriter out, BufferedReader in)
         throws IOException, ParseException {
      SelectStore selectStore = new SelectStore();
      selectStore.displayStoreList();

   }

   // 추가 : pointCharge 메서드 작성
   private static void pointCharge(BufferedReader stdIn, PrintWriter out, BufferedReader in, String userId)
         throws IOException, ParseException {

      // 포인트 충전 객체 생성
      PointCharge json = new PointCharge(sessions);
      // 포인트 충전 메서드 호출
      json.chargePoint(stdIn, out, userId);
      // 서버로부터의 응답처리
      handleServerResponse(stdIn, out, in); // 서버로부터의 응답처리
      // 포인트 충전 결과에 관한 JSON 응답을 생성하여 서버로 전송
      JSONObject jsonPointCharge = new JSONObject();
      jsonPointCharge.put("type", "point_charge_response");
      jsonPointCharge.put("userId", userId);
//      jsonPointCharge.put("message", "포인트가 성공적으로 충전되었습니다.");
      out.println(jsonPointCharge.toJSONString());
      out.flush(); // 데이터 전송을 완료하기 위해 버퍼를 비움

   }

   // 세션 인증 요청을 보내는 메서드
   private static void sendSessionAuthRequest(PrintWriter out, String userId) {
      JSONObject json = new JSONObject();
      json.put("type", "session_auth"); // 세션 인증을 요청하는 타입으로 지정
      json.put("userId", userId); // 클라이언트가 가지고 있는 세션 아이디 전송

      out.print(json.toString()); // 서버에 세션 인증 요청
      out.flush(); // 데이터 전송을 완료하기 위해 버퍼를 비움

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
   // 서버로부터 메시지를 수신하는 스레드 시작
   private static void startMessageReceiverThread(BufferedReader in) {
       Thread receiveThread = new Thread(() -> {
           try {
               StringBuilder messageBuilder = new StringBuilder();
               int character;
               while ((character = in.read()) != -1) {
                   if (character == '\n') { // 개행 문자가 나오면 메시지를 처리
                       String serverMessage = messageBuilder.toString().trim(); // 공백 제거
                       if (!serverMessage.isEmpty()) { // 메시지가 비어있지 않으면 출력
                           System.out.println(serverMessage); // 개행 문자를 추가하지 않고 출력
                       }
                       messageBuilder.setLength(0); // StringBuilder 초기화
                   } else {
                       messageBuilder.append((char) character); // 문자열 빌더에 문자 추가
                   }
               }
           } catch (IOException e) {
               System.err.println("서버와의 연결이 종료되었습니다.");
           }
       });
       receiveThread.start();
   }
}