package jeongsik;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.json.simple.JSONObject;

public class OneChatRoom {
	private Map<String, String> sessions;
	private BufferedReader stdIn;
	private PrintWriter out;
	private BufferedReader in;
	private String sessionID;

	public OneChatRoom(Map<String, String> sessions,BufferedReader stdIn, PrintWriter out, BufferedReader in, String sessionID) {
		this.sessions = sessions;
		this.stdIn = stdIn;
		this.out = out;
		this.in = in;
		this.sessionID = sessionID;

	}

	public void startChat() {
		
			System.out.println("전체 채팅방에 오신걸 환영합니다. 매너있는 채팅부탁드립니다.");
			System.out.println("/채팅인원보기, /종료, /귓속말 이용할수있습니다.");
		try {
			while(true) {
				//사용자로부터 채팅 입력 받기
				System.out.println(sessionID+": ");
				String message = stdIn.readLine();
						
				//사용자가 입력한 채팅 서버로 전송
				JSONObject json = new JSONObject();
				json.put("type", "chat_message");
				json.put("sessionID",sessionID);
				json.put("message", message);
				out.print(json.toJSONString());
				out.flush();
				
				//입력이 "/종료"이면 채팅 종료
				if("/종료".equals(message)) {
					break;
				}
			}
		}catch (IOException e) {
			System.out.println("채팅 종료 오류 발생: " + e.getMessage());
		}
	}
}
