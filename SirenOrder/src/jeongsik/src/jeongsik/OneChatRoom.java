package jeongsik;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class OneChatRoom {
    private Map<String, String> sessions;
    private BufferedReader stdIn;
    private PrintWriter out;
    private BufferedReader in;
    private String sessionID;

    public OneChatRoom(Map<String, String> sessions, BufferedReader stdIn, PrintWriter out, BufferedReader in, String sessionID) {
        this.sessions = sessions;
        this.stdIn = stdIn;
        this.out = out;
        this.in = in;
        this.sessionID = sessionID;
    }

    // 채팅방에서 전체 메시지를 받아 처리하는 쓰레드
    class MessageReceiver extends Thread {
        private BufferedReader in;

        public MessageReceiver(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = in.readLine();
                    if (message != null) {
                        JSONObject json = (JSONObject) JSONValue.parse(message);
                        if (json.get("type").equals("chat_message")) {
                            String sessionID = (String) json.get("sessionID");
                            String chatMessage = (String) json.get("message");
                            System.out.println("[" + sessionID + "]: " + chatMessage);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("메시지 수신 중 오류 발생: " + e.getMessage());
            }
        }
    }

    // 채팅방에서 귓속말을 보내는 기능
    private void sendWhisperMessage(String receiverID) throws IOException {
        System.out.print("귓속말 대상 [" + receiverID + "]에게 전송할 메시지 입력: ");
        String message = stdIn.readLine();

        JSONObject json = new JSONObject();
        json.put("type", "whisper_message");
        json.put("sender", sessionID);
        json.put("receiver", receiverID);
        json.put("message", message);

        out.println(json.toJSONString());
    }

    // 사용자가 채팅방을 나가는 기능
    private void leaveChatRoom() throws IOException {
        JSONObject json = new JSONObject();
        json.put("type", "leave_chat_room");
        json.put("sessionID", sessionID);

        out.println(json.toJSONString());
        out.flush();
    }

    // 채팅방 클라이언트 코드에 메시지 수신 쓰레드를 생성하여 실행하는 부분
    public void startChat() {
        try {
            System.out.println("전체 채팅방에 오신걸 환영합니다. 매너있는 채팅 부탁드립니다.");
            System.out.println("명령어 사용 가능: /누구 (대화상대 보기), /종료 (채팅방 종료), /[상대방세션ID] (귓속말)");

            // 채팅방에 접속한 클라이언트들끼리 서로 메시지 주고받기 위한 쓰레드 생성 및 실행
            MessageReceiver messageReceiver = new MessageReceiver(in);
            messageReceiver.start();

            while (true) {
                // 사용자 입력 처리
                System.out.print("[" + sessionID + "]: ");
                String message = stdIn.readLine();

                // 사용자가 입력한 메시지가 명령어인지 확인
                if (message.startsWith("/")) {
                    handleCommand(message);
                    continue;
                }

                // 채팅 메시지를 서버로 전송
                JSONObject json = new JSONObject();
                json.put("type", "chat_message");
                json.put("sessionID", sessionID);
                json.put("message", message);

                out.println(json.toJSONString());
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("채팅 오류 발생: " + e.getMessage());
        }
    }

    // 사용자가 입력한 명령어를 처리하는 메서드
    private void handleCommand(String command) throws IOException {
        switch (command) {
            case "/누구":
                displayParticipants();
                break;
            case "/종료":
                leaveChatRoom();
                break;
            default:
                if (command.startsWith("/")) {
                    String receiverID = command.substring(1); // 명령어에서 상대방 세션 아이디 추출
                    sendWhisperMessage(receiverID);
                } else {
                    System.out.println("잘못된 명령어입니다.");
                }
                break;
        }
    }

    // 대화 상대 전체 보기
    private void displayParticipants() {
        System.out.println("채팅방에 참여한 대화 상대:");
        for (String session : sessions.keySet()) {
            System.out.println("- " + session);
        }
    }
}
