package sghku.tianchi.IntelligentAviation.common;

import java.io.File;
import java.io.IOException;

import sghku.tianchi.IntelligentAviation.entity.ConnectingFlightpair;
import sghku.tianchi.IntelligentAviation.entity.Flight;
import sghku.tianchi.IntelligentAviation.entity.FlightArc;
import sghku.tianchi.IntelligentAviation.entity.FlightArcItinerary;
import sghku.tianchi.IntelligentAviation.entity.FlightItinerary;
import sghku.tianchi.IntelligentAviation.entity.FlightSectionItinerary;
import sghku.tianchi.IntelligentAviation.entity.Itinerary;
import sghku.tianchi.IntelligentAviation.entity.Scenario;
import sghku.tianchi.IntelligentAviation.entity.TransferPassenger;

public class OutputSecondTransferInfor {
	
	
	public void writeResult(Scenario sce, String outputName){
		
		
		StringBuilder sb = new StringBuilder();

		try {
			File file = new File(outputName);
			if(file.exists()){
				file.delete();
			}
			
			MyFile.creatTxtFile(outputName);

			for(TransferPassenger tp:sce.transferPassengerList) {	
		
				StringBuilder secondTrsfrsignChangeSb = new StringBuilder();
				secondTrsfrsignChangeSb.append(tp.inFlight.id+","+tp.outFlight.id+","+tp.minTurnaroundTime+","+tp.volume+",");
				boolean isSignChange = false;
				for(FlightItinerary fi:tp.flightIteList) {
					secondTrsfrsignChangeSb.append(fi.flight.id +":"+(int)Math.round(fi.volume)+"&");
					isSignChange = true;
				}
				if(isSignChange){
					secondTrsfrsignChangeSb.deleteCharAt(secondTrsfrsignChangeSb.length()-1);  //delete the last &
				}		
				secondTrsfrsignChangeSb.append(",");
				secondTrsfrsignChangeSb.append(tp.cancelNum+",");
				
				
				secondTrsfrsignChangeSb.append("\n");
				sb.append(secondTrsfrsignChangeSb.toString());
			}
			sb.deleteCharAt(sb.length()-1);

			MyFile.writeTxtFile(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
