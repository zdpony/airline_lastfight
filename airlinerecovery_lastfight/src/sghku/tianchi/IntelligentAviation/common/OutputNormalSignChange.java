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

public class OutputNormalSignChange {
	
	
	public void writeResult(Scenario sce, String outputName){
		
		
		StringBuilder sb = new StringBuilder();

		try {
			File file = new File(outputName);
			if(file.exists()){
				file.delete();
			}
			
			MyFile.creatTxtFile(outputName);

			for(Flight f:sce.flightList) {
				StringBuilder signChangeSb = new StringBuilder();
				signChangeSb.append(f.id+",");
				boolean isSignChange = false;
				for(FlightItinerary fi:f.normalSignInFIList) {
					signChangeSb.append(fi.thirdStageite.flight.id +":"+(int)Math.round(fi.volume)+"&");  //fromFlightId : signChangeNum
					isSignChange = true;
				}
				if(isSignChange){
					signChangeSb.deleteCharAt(signChangeSb.length()-1);  //delete the last &
				}		
				signChangeSb.append("\n");
				sb.append(signChangeSb.toString());
			}
			
			sb.deleteCharAt(sb.length()-1);

			MyFile.writeTxtFile(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
