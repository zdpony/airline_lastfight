package sghku.tianchi.IntelligentAviation.common;

import java.io.File;
import java.io.IOException;

import sghku.tianchi.IntelligentAviation.entity.ConnectingFlightpair;
import sghku.tianchi.IntelligentAviation.entity.Flight;
import sghku.tianchi.IntelligentAviation.entity.FlightArc;
import sghku.tianchi.IntelligentAviation.entity.Scenario;

public class OutputResultDigitTime {
	
	
	public void writeResult(Scenario scenario, String outputName){
		for(ConnectingFlightpair cp:scenario.connectingFlightList) {
			cp.firstFlight.isIncludedInConnecting = true;
			cp.secondFlight.isIncludedInConnecting = true;
		}
		
		StringBuilder sb = new StringBuilder();

		try {
			File file = new File(outputName);
			if(file.exists()){
				file.delete();
			}
			
			MyFile.creatTxtFile(outputName);

			int deadheadIndex = 9001;
			
			for(Flight f:scenario.flightList){
				if(f.isDeadhead && f.isCancelled) {
					continue;
				}
				
			
				
				//System.out.println(f.id+","+f.isCancelled+", "+f.isStraightened+","+f.isDeadhead);
				sb.append((f.isDeadhead?deadheadIndex++:f.id)+","+f.leg.originAirport.id+","+f.leg.destinationAirport.id+",");		
				sb.append(f.actualTakeoffT+","+f.actualLandingT+","+f.aircraft.id+",");
						
				if(f.isCancelled){
					sb.append("1,");
				}else{
					sb.append("0,");
				}
				if(f.isStraightened){
					sb.append("1,");
				}else{
					sb.append("0,");
				}
				if(f.isDeadhead){
					sb.append("1"+"\n");
				}else{
					sb.append("0"+"\n");
				}
			}
			
	
			sb.deleteCharAt(sb.length()-1);

			MyFile.writeTxtFile(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
