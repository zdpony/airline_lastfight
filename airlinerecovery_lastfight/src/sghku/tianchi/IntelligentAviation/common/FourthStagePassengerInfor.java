package sghku.tianchi.IntelligentAviation.common;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import sghku.tianchi.IntelligentAviation.entity.*;

public class FourthStagePassengerInfor {
	public Map<String, Integer> flightConnectionTimeLimit = new HashMap<>();
	
	public void generateFouthStagePassengerInfor(Scenario sce) {
		/*List<Flight> flightList = sce.flightList;
		for(Flight f:flightList) {
		
			f.earliestTakeoffTimeForTransferPasseneger = f.initialTakeoffT-360;
			if(f.isDomestic) {
				f.latestTakeoffTimeForTransferPassenger = f.initialTakeoffT + Parameter.MAX_DELAY_DOMESTIC_TIME;
			}else {
				f.latestTakeoffTimeForTransferPassenger = f.initialTakeoffT + Parameter.MAX_DELAY_INTERNATIONAL_TIME;
			}
	
			if(!f.isCancelled && !f.isStraightened) {
				f.occupiedSeatsByTransferPassenger += f.firstTransferPassengerNumber;
			}
		}
		
		
		try {
			Scanner sn = new Scanner(new File("rachelresult/rachel_tra_0903.csv"));
			
			while(sn.hasNextLine()) {
				String nextLine = sn.nextLine().trim();
				if(nextLine.equals("")) {
					break;
				}

				Scanner innerSn = new Scanner(nextLine);
				innerSn.useDelimiter(",");
				
				int inFlightId = innerSn.nextInt();
				Flight inFlight = flightList.get(inFlightId-1);
				int outFlightId = innerSn.nextInt();
				Flight outFlight = flightList.get(outFlightId-1);
				int minTurnaroundTime = innerSn.nextInt();
				int initialVolume = innerSn.nextInt();
				int signChangeVolume = 0;
				
				if(innerSn.hasNext()) {
					String transferNumber = innerSn.next();
					if(!transferNumber.trim().equals("")) {
						String[] transferNumberArray = transferNumber.split("&");
						for(String transferNumberStr:transferNumberArray) {
							String[] transferNumberStrArray = transferNumberStr.split(":");
							int candidateFlightId = Integer.parseInt(transferNumberStrArray[0]);
							int volume = Integer.parseInt(transferNumberStrArray[1]);
							signChangeVolume += volume;
							Flight candidateFlight = flightList.get(candidateFlightId-1);
							
							String key = inFlight.id+"_"+candidateFlight.id;
							Integer connT = connectionMap.get(key);
							
							if(connT == null) {
								connectionMap.put(key, minTurnaroundTime);
							}else {
								if(minTurnaroundTime > connT) {
									connectionMap.put(key, minTurnaroundTime);
								}
							}
							
							//????????
							Integer d = candidateFlight.occupiedSeatsBySignChangeTransferPassengerMapInFourStage.get(outFlight.initialTakeoffT);
							if(d == null) {
								candidateFlight.occupiedSeatsBySignChangeTransferPassengerMapInFourStage.put(outFlight.initialTakeoffT, volume);
							}else {
								candidateFlight.occupiedSeatsBySignChangeTransferPassengerMapInFourStage.put(outFlight.initialTakeoffT, d+volume);
							}
							
							if(outFlight.initialTakeoffT > candidateFlight.earliestTakeoffTimeForTransferPasseneger) {
								candidateFlight.earliestPossibleTime = outFlight.initialTakeoffT;
							}
							if(outFlight.initialTakeoffT + 48*60 < candidateFlight.latestTakeoffTimeForTransferPassenger) {
								candidateFlight.latestTakeoffTimeForTransferPassenger = outFlight.initialTakeoffT + 48*60;
							}
							
							outFlight.isAllowedToSignIn = false;
							candidateFlight.isAllowedToSignOut = false;
							FlightItinerary fi = new FlightItinerary();
							fi.volume = volume;
							fi.flight = outFlight;
							
							candidateFlight.fIList.add(fi);
							
							Integer previousValue = outFlight.previousSignChangeMap.get(candidateFlight.id);
							if(previousValue == null) {
								System.out.println("previous flight in a transfer pair error:"+outFlight.id+" "+candidateFlight.id);
								System.exit(1);
							}else {
								outFlight.previousSignChangeMap.put(candidateFlight.id, previousValue-volume);								
							}
						}
					}					
				}
				
				if(innerSn.hasNext()) {
					int cancelNum = innerSn.nextInt();
					totalTransferCancelCost += cancelNum * Parameter.passengerCancelCost;
					int remainingNum = initialVolume - cancelNum - signChangeVolume;
					
					if(inFlight.isCancelled || inFlight.isStraightened) {
						
					}else {
						if(!inFlight.isCancelled && !outFlight.isCancelled) {							
							outFlight.occupiedSeatsByTransferPassenger += remainingNum;							
							if(outFlight.id == 1402) {
								System.out.println("flight 1402:"+initialVolume+" "+signChangeVolume+" "+inFlight.isCancelled+" "+inFlight.id);
							}
						}
					}					
				}	
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
			
	
	
	
	
}
