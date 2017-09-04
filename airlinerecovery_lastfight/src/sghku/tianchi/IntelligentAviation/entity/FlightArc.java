package sghku.tianchi.IntelligentAviation.entity;

import java.util.ArrayList;
import java.util.List;

import sghku.tianchi.IntelligentAviation.common.ExcelOperator;
import sghku.tianchi.IntelligentAviation.common.Parameter;

public class FlightArc {
	public int id;
	//头尾点
	public Node fromNode;  
	public Node toNode;  
	//该arc对应的成本
	public double cost;   
	//该arc的提前时间
	public int earliness;     
	//该arc的延误时间
	public int delay;   
	//该arc对应的航班
	public Flight flight;    //flight that flies this arc
	//该arc对应的执行飞机
	public Aircraft aircraft;  //aircraft that uses this arc

	//该arc对应的起飞时间
	public int takeoffTime;
	//该arc对应的降落时间
	public int landingTime;
	//该arc对应的下次准备好的飞行时间
	public int readyTime;
	
	public int flow;
	public double fractionalFlow;
		
	/****************ych******************/
 	public String writtenTimeTk;
 	public String writtenTimeLd;
 	
 	public int passengerCapacity;
 	
 	public double delayRelatedCost = 0;
 	public double cancelRelatedCost = 0;
 	
 		
 	//标记一个arc是否属于一个connecting arc
	//public boolean isIncludedInConnecting = false;
	//public List<ConnectingArc> connectingArcList = new ArrayList<>();
	
	public boolean isWithinAffectedRegionOrigin = false;
	public boolean isWithinAffectedRegionDestination = false;
	
	public int fulfilledNormalPassenger = 0;
	
	public boolean isVisited = false;

	public List<FlightArcItinerary> flightArcItineraryList = new ArrayList<>();  //连接itinerary，标记转签乘客是否选择此flight arc
	
	//打印信息
	public String getTime(){
		return "["+takeoffTime+","+landingTime+","+readyTime+"]";
	}
	
	public String toString(){
	
		return flight.id+","+takeoffTime+"->"+landingTime+"->"+readyTime+"  "+flight.leg.originAirport.id+":"+flight.leg.destinationAirport.id;
	}
	
	//计算该arc的成本
	public void calculateCost(Scenario scenario){
		if(flight.isStraightened){
			cost += ExcelOperator.getFlightTypeChangeParam(flight.connectingFlightpair.firstFlight.initialAircraftType, aircraft.type)*flight.connectingFlightpair.firstFlight.importance;
			
			if(flight.connectingFlightpair.firstFlight.initialAircraft.id != aircraft.id) {
				if(flight.connectingFlightpair.firstFlight.initialTakeoffT <= Parameter.aircraftChangeThreshold) {
					cost += Parameter.aircraftChangeCostLarge*flight.connectingFlightpair.firstFlight.importance;
				}else {
					cost += Parameter.aircraftChangeCostSmall*flight.connectingFlightpair.firstFlight.importance;
				}
			}
			
			cost += Parameter.COST_EARLINESS/60.0*earliness*flight.connectingFlightpair.firstFlight.importance;
			cost += Parameter.COST_DELAY/60.0*delay*flight.connectingFlightpair.firstFlight.importance;
			cost += Parameter.COST_STRAIGHTEN*(flight.connectingFlightpair.firstFlight.importance+flight.connectingFlightpair.secondFlight.importance);
			
			cost += ExcelOperator.getPassengerDelayParameter(delay) * flight.connectingFlightpair.firstFlight.selfPassengerNumber;
			
			if(Parameter.isPassengerCostConsidered) {
				//如果是联程拉直航班，则只需要考虑联程拉直的乘客对应的delay和cancel cost，普通乘客则不需要考虑（因为在cancel flight和signChange那里会考虑）
				int actualNum =  Math.min(aircraft.passengerCapacity, flight.connectingFlightpair.firstFlight.connectedPassengerNumber);
				int cancelNum = flight.connectingFlightpair.firstFlight.connectedPassengerNumber - actualNum;
				
				cost += actualNum*ExcelOperator.getPassengerDelayParameter(delay);
				delayRelatedCost += actualNum*ExcelOperator.getPassengerDelayParameter(delay);
				
				cost += cancelNum*Parameter.passengerCancelCost;			
				cancelRelatedCost += cancelNum*Parameter.passengerCancelCost;
				
				//计算中转乘客
				if(Parameter.stageIndex == 1) {
					cost += flight.connectingFlightpair.firstFlight.firstTransferPassengerNumber * Parameter.passengerCancelCost * 2 + flight.connectingFlightpair.firstFlight.secondTransferPassengerNumber * Parameter.passengerCancelCost;  //不乘2是低估，乘2是高估
					cost += flight.connectingFlightpair.secondFlight.transferPassengerNumber * Parameter.passengerCancelCost * 2 + flight.connectingFlightpair.secondFlight.secondTransferPassengerNumber * Parameter.passengerCancelCost;
					
					cancelRelatedCost += flight.connectingFlightpair.firstFlight.firstTransferPassengerNumber * Parameter.passengerCancelCost * 2 + flight.connectingFlightpair.firstFlight.secondTransferPassengerNumber * Parameter.passengerCancelCost;  //不乘2是低估，乘2是高估
					cancelRelatedCost += flight.connectingFlightpair.secondFlight.transferPassengerNumber * Parameter.passengerCancelCost * 2 + flight.connectingFlightpair.secondFlight.secondTransferPassengerNumber * Parameter.passengerCancelCost;
				}else {
					//每截作为transfer第一段必须cancel
					cost += flight.connectingFlightpair.firstFlight.firstTransferPassengerNumber * Parameter.passengerCancelCost * 2;  
					cancelRelatedCost += flight.connectingFlightpair.firstFlight.firstTransferPassengerNumber * Parameter.passengerCancelCost;
					
					cost += flight.connectingFlightpair.secondFlight.firstTransferPassengerNumber * Parameter.passengerCancelCost * 2; 
					cancelRelatedCost += flight.connectingFlightpair.secondFlight.firstTransferPassengerNumber * Parameter.passengerCancelCost;
					
					//每截作为第二段加入到itinerary考虑签转
				}

				//普通旅客的取消成本不在此考虑，而在模型中考虑
				//delayCost += actualNum*ExcelOperator.getPassengerDelayParameter(delay);  //record delay cost of connecting pssgr on flight
				//connPssgrCclDueToStraightenCost += cancelNum*Parameter.passengerCancelCost; //record cancel cost due to straighten
				
			}		
		}else if(flight.isDeadhead){
			cost += Parameter.COST_DEADHEAD;
		}else{
			cost += earliness/60.0*Parameter.COST_EARLINESS*flight.importance;
			cost += delay/60.0*Parameter.COST_DELAY*flight.importance;


			if(flight.id == 1083 && takeoffTime == 9590) {
				System.out.println("1083 f:"+cost+" "+takeoffTime+" "+aircraft.id+" "+earliness+" "+delay);				
			}
			
			cost += ExcelOperator.getFlightTypeChangeParam(flight.initialAircraftType, aircraft.type)*flight.importance;
			
			if(flight.initialAircraft.id != aircraft.id) {
				if(flight.initialTakeoffT <= Parameter.aircraftChangeThreshold) {
					cost += Parameter.aircraftChangeCostLarge*flight.importance;
				}else {
					cost += Parameter.aircraftChangeCostSmall*flight.importance;
				}
			}
			
			cost += ExcelOperator.getPassengerDelayParameter(delay) * flight.selfPassengerNumber;
			for(int fId:flight.signChangeMap.keySet()) {
				Flight fromFlight = scenario.flightList.get(fId-1);
				int volume = flight.signChangeMap.get(fId);
				
				int signChangeDelay = takeoffTime - fromFlight.initialTakeoffT;
				cost += Scenario.getSignChangePassengerDelayParam(signChangeDelay/60.)*volume;
			}
					
		}
	}
	
	//检查该arc是否违反约束
	public boolean checkViolation(){
		boolean vio = false;
		//check airport fault
		
		Leg leg = flight.leg;
		
		//判断台风场景限制(起飞和降落限制)
		List<Failure> failureList = new ArrayList<>();
		failureList.addAll(leg.originAirport.failureList);
		failureList.addAll(leg.destinationAirport.failureList);
		
		for(Failure scene:failureList){
        	if(scene.isInScene(0, 0, leg.originAirport, leg.destinationAirport, takeoffTime, landingTime)) {
                vio = true;
                break;
            }
		}
		
		/*if(!vio){
			//判断停机时间是否在台风停机故障内
			for(Failure scene:leg.destinationAirport.failureList){
				if(scene.isStopInScene(0, 0, leg.destinationAirport, landingTime, readyTime)){
					vio = true;
					break;
				}
			}
		}*/
		if(!vio){
			//判断是否在机场关闭时间内
			for(ClosureInfo ci:leg.originAirport.closedSlots){
				if(takeoffTime>ci.startTime && takeoffTime<ci.endTime){
					vio = true;
					break;
				}
			}
			
			for(ClosureInfo ci:leg.destinationAirport.closedSlots){
				if(landingTime>ci.startTime && landingTime<ci.endTime){
					vio = true;
					break;
				}
			}
		}
		
		return vio;
	}

	
	//如果该arc选择，更新对应的aircraft， flight的信息
	public void update() {

		this.aircraft.flightList.add(this.flight);
		this.flight.aircraft = this.aircraft;
		
		this.flight.isCancelled = false;
		//更新航班时间
		this.flight.actualTakeoffT = takeoffTime;
		this.flight.actualLandingT = landingTime;
		
	}
}
