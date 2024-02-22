package jeongsik;

public class CountCoffee {
	// 커피 종류별 카운트 변수
	public static int countAmericano = 0;
	public static int countCafeLatte = 0;
	public static int countCafeMoca = 0;
	public static int countCaramel = 0;
	public static int countVanila = 0;
	
	public static int countSales = 0;

	public static void countTotal(String menuName) {
		switch (menuName) {
		case "아메리카노":
			countAmericano++;
			countSales+=4000;
			break;
		case "카페라떼":
			countCafeLatte++;
			countSales+=5000;
			break;
		case "카페모카":
			countCafeMoca++;
			countSales+=5000;
			break;
		case "카라멜마끼아또":
			countCaramel++;
			countSales+=5000;
			break;
		case "바닐라라떼":
			countVanila++;
			countSales+=5000;
			break;
		default:
			System.out.println("해당하는 메뉴가 없습니다.");
			break;

		}
		System.out.println("주문 현황 : 아메리카노" + countAmericano + " 카페라떼" + countCafeLatte + " 카페모카" + countCafeMoca
				+ " 카라멜마끼아또" + countCaramel + " 바닐라라떼" + countVanila);
		System.out.println("매출 현환 : " + countSales);
	}
}