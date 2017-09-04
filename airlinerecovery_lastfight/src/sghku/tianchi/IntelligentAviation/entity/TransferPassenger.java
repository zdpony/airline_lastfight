package sghku.tianchi.IntelligentAviation.entity;

import java.util.ArrayList;
import java.util.List;

public class TransferPassenger {
	public int id;
	public Flight inFlight;
	public Flight outFlight;
	public int minTurnaroundTime;
	public int volume;
	public List<FlightItinerary> flightIteList = new ArrayList<>();   //用于第三阶段记录签转信息，第四阶段用
	public int cancelNum = 0;  //第三阶段写信息，用于output
}
