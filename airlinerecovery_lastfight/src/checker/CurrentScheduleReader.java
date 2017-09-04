package checker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import sghku.tianchi.IntelligentAviation.algorithm.FlightDelayLimitGenerator;
import sghku.tianchi.IntelligentAviation.common.MyFile;
import sghku.tianchi.IntelligentAviation.common.Parameter;
import sghku.tianchi.IntelligentAviation.comparator.FlightComparator;
import sghku.tianchi.IntelligentAviation.comparator.FlightComparator2;
import sghku.tianchi.IntelligentAviation.entity.Aircraft;
import sghku.tianchi.IntelligentAviation.entity.ConnectingFlightpair;
import sghku.tianchi.IntelligentAviation.entity.Flight;
import sghku.tianchi.IntelligentAviation.entity.Scenario;

public class CurrentScheduleReader {
	public void read(Scenario scenario) {
		try {
			for(Aircraft a:scenario.aircraftList) {
				a.flightList.clear();
			}
			
			Scanner sn = new Scanner(new File("result\\pony_594325.9_50.csv"));
			
			while(sn.hasNextLine()) {
				String nextLine = sn.nextLine().trim();
				if(nextLine.equals("")) {
					break;
				}
				
				Scanner innerSn = new Scanner(nextLine);
				innerSn.useDelimiter(",");
				
				int flightId = innerSn.nextInt();
				int originId = innerSn.nextInt();
				int destId = innerSn.nextInt();
				
				String takeoffString = innerSn.next();
				String[] dayandtime = takeoffString.split(" ");
				int takeoffT = Integer.parseInt(dayandtime[0].split("/")[2])*1440+Integer.parseInt(dayandtime[1].split(":")[0])*60+Integer.parseInt(dayandtime[1].split(":")[1]);
				
				
				String landingString = innerSn.next();
				dayandtime = landingString.split(" ");
				int landingT = Integer.parseInt(dayandtime[0].split("/")[2])*1440+Integer.parseInt(dayandtime[1].split(":")[0])*60+Integer.parseInt(dayandtime[1].split(":")[1]);
				
				int aircraftId = innerSn.nextInt();
				
				int isCancelled = innerSn.nextInt();
				int isStraightend = innerSn.nextInt();
				int isDeadhead = innerSn.nextInt();
				
				Aircraft aircraft = scenario.aircraftList.get(aircraftId-1);
				
				if(flightId < 9001) {
					Flight f = scenario.flightList.get(flightId-1);
					if(isCancelled == 0) {
						if(isStraightend == 1) {
							//生成联程拉直航班
							
							ConnectingFlightpair cp = f.connectingFlightpair;
							
							int flyTime = cp.straightenLeg.flytimeArray[aircraft.type-1];
							
							if(flyTime <= 0){  // if cannot retrieve fly time
								flyTime = cp.firstFlight.initialLandingT-cp.firstFlight.initialTakeoffT+ cp.secondFlight.initialLandingT-cp.secondFlight.initialTakeoffT;
							}
							
							Flight straightenedFlight = new Flight();
							straightenedFlight.isStraightened = true;
							straightenedFlight.connectingFlightpair = cp;
							straightenedFlight.leg = cp.straightenLeg;
							
							straightenedFlight.flyTime = flyTime;
									
							straightenedFlight.initialTakeoffT = cp.firstFlight.initialTakeoffT;
							straightenedFlight.initialLandingT = straightenedFlight.initialTakeoffT + flyTime;
							
							straightenedFlight.isAllowtoBringForward = cp.firstFlight.isAllowtoBringForward;
							straightenedFlight.isAffected = cp.firstFlight.isAffected;
							straightenedFlight.isDomestic = true;
							straightenedFlight.earliestPossibleTime = cp.firstFlight.earliestPossibleTime;
							straightenedFlight.latestPossibleTime = cp.firstFlight.latestPossibleTime;
						
							straightenedFlight.actualTakeoffT = takeoffT;
							straightenedFlight.actualLandingT = landingT;
							
							straightenedFlight.aircraft = aircraft;
							aircraft.flightList.add(straightenedFlight);
						}else {
							f.actualTakeoffT = takeoffT;
							f.actualLandingT = landingT;
							f.aircraft = aircraft;
							aircraft.flightList.add(f);
						}
					}
				}else {
					
					System.out.println("this is a dead head arc");
					System.exit(1);
				}
			}
			
			for(Flight f:scenario.flightList) {
				f.isCancelled = true;
			}
			for(Aircraft a:scenario.aircraftList) {
				for(Flight f:a.flightList) {
					if(f.isStraightened) {
						f.connectingFlightpair.firstFlight.isCancelled = false;
						f.connectingFlightpair.secondFlight.isCancelled = false;
					}else {
						f.isCancelled = false;
					}
				}
				
				Collections.sort(a.flightList, new FlightComparator2());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args){
		Parameter.isReadFixedRoutes = false;
		Scenario scenario = new Scenario(Parameter.EXCEL_FILENAME);
		
		FlightDelayLimitGenerator flightDelayLimitGenerator = new FlightDelayLimitGenerator();
		flightDelayLimitGenerator.setFlightDelayLimit(scenario);
		
		CurrentScheduleReader csr = new CurrentScheduleReader();
		csr.read(scenario);
		
		System.out.println("------------------analyze delay--------------------");
		try {
			MyFile.creatTxtFile("result/analyze.csv");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Set<Integer> affAirportSet = new HashSet<>();
		affAirportSet.add(49);
		affAirportSet.add(50);
		affAirportSet.add(61);
		
		for(Flight f:scenario.flightList){
			if(f.initialTakeoffT >= 6*1440 && f.initialTakeoffT <= 7*1440){
				int delay = f.actualTakeoffT - f.initialTakeoffT;
				
				int meaningfulDelay1 = f.actualTakeoffT - 7*1440-17*60;
				int meaningfulDelay2 = f.actualLandingT - 7*1440-17*60;

				if(delay > 60){
					//if(!affAirportSet.contains(f.leg.originAirport.id) && !affAirportSet.contains(f.leg.destinationAirport.id)){
						String str = "large delay:"+delay+", ("+meaningfulDelay1+" "+meaningfulDelay2+"),  f:"+f.id+", schedule:"+f.initialTakeoffT+" to "+f.actualTakeoffT+", leg:"+f.leg.originAirport+"->"+f.leg.destinationAirport+",  [";
						for(int[] timeLimit:f.timeLimitList){
							str += timeLimit[0]+","+timeLimit[1]+" ";
						}
						str += "],  "+f.takeoffTime+" -> "+f.landingTime;
						System.out.println(str);
					//}
					

					/*try {
						MyFile.writeTxtFile(str);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					/*System.out.print("large delay:"+delay+" ("+meaningfulDelay1+","+meaningfulDelay2+")  f:"+f.id+" schedule:"+","+f.initialTakeoffT+" to "+f.actualTakeoffT+" leg:"+f.leg.originAirport+"->"+f.leg.destinationAirport+"  [");
					for(int[] timeLimit:f.timeLimitList){
						System.out.print(timeLimit[0]+","+timeLimit[1]+" ");
					}
					System.out.println("]  "+f.takeoffTime+" -> "+f.landingTime);*/
				}
			}
		}
	}
}
