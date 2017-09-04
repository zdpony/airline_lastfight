package sghku.tianchi.IntelligentAviation.algorithm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sghku.tianchi.IntelligentAviation.common.Parameter;
import sghku.tianchi.IntelligentAviation.entity.Airport;
import sghku.tianchi.IntelligentAviation.entity.ClosureInfo;
import sghku.tianchi.IntelligentAviation.entity.Flight;
import sghku.tianchi.IntelligentAviation.entity.FlightArc;
import sghku.tianchi.IntelligentAviation.entity.Scenario;

public class FlightDelayLimitGeneratorFullDelay {
	private Set<Integer> AirportID_49_50_61 = new HashSet(Arrays.asList(49, 50, 61)); //contains id
	private Map<Integer, Set<Integer>> formerAirportMap = new HashMap<>();
	private List<Integer> AirportID_5_6_22_49_76 = Arrays.asList(5, 6, 22, 49, 76); //contains id
	private Set<Integer> indirectTyphoonAffectedAirportSet = new HashSet<>();
	private Set<Integer> AirportID_25_67 = new HashSet(Arrays.asList(25, 67)); //contains id
		
	//设定flight delay限制
	public void setFlightDelayLimit(Scenario scenario){

		for(Flight f:scenario.flightList){
			if(f.initialTakeoffT >= 1440*8){
				f.timeLimitList.add(new int[] {f.initialTakeoffT,f.initialTakeoffT+180}); //delay 3小时
			}else if(f.initialTakeoffT >= 6*1440 && f.initialTakeoffT <= 7*1440){
				if(f.initialTakeoffT <= 6*1440 + 14*60){
					f.timeLimitList.add(new int[] {f.initialTakeoffT,f.initialTakeoffT+180}); //delay 3小时
				}else{
					if(AirportID_49_50_61.contains(f.leg.originAirport.id)){
						
					}else if(AirportID_25_67.contains(f.leg.destinationAirport.id)){
						
					}else{
						f.timeLimitList.add(new int[] {f.initialTakeoffT,f.initialTakeoffT+180}); //delay 3小时
					}
				}
			}
		}
		
		for(Flight f:scenario.flightList){
			if(f.timeLimitList.size() == 0){
				int et = f.initialTakeoffT;
				int lt = f.initialTakeoffT;
				
				if(f.isAllowtoBringForward){
					et = et - Parameter.MAX_LEAD_TIME;
				}
				if(f.isDomestic){
					lt = lt + Parameter.MAX_DELAY_DOMESTIC_TIME;
				}else{
					lt = lt + Parameter.MAX_DELAY_INTERNATIONAL_TIME;
				}
				
				f.timeLimitList.add(new int[] {et,lt}); //按照原来的延误限制
			}
		}
		
	}
	
	//设置联程拉直航班的flight delay limit
	public void setFlightDelayLimitForStraightenedFlight(Flight f, Scenario scenario){	
		f.timeLimitList.add(new int[] {f.initialTakeoffT,f.initialTakeoffT+540});
	}	
}