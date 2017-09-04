package sghku.tianchi.IntelligentAviation;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;

import javax.swing.plaf.synth.SynthSpinnerUI;

import checker.ArcChecker;
import sghku.tianchi.IntelligentAviation.algorithm.FlightDelayLimitGenerator;
import sghku.tianchi.IntelligentAviation.algorithm.NetworkConstructor;
import sghku.tianchi.IntelligentAviation.algorithm.NetworkConstructorBasedOnDelayAndEarlyLimit;
import sghku.tianchi.IntelligentAviation.clique.Clique;
import sghku.tianchi.IntelligentAviation.common.OutputResult;
import sghku.tianchi.IntelligentAviation.common.OutputResultWithPassenger;
import sghku.tianchi.IntelligentAviation.common.Parameter;
import sghku.tianchi.IntelligentAviation.entity.Aircraft;
import sghku.tianchi.IntelligentAviation.entity.Airport;
import sghku.tianchi.IntelligentAviation.entity.ClosureInfo;
import sghku.tianchi.IntelligentAviation.entity.ConnectingArc;
import sghku.tianchi.IntelligentAviation.entity.ConnectingFlightpair;
import sghku.tianchi.IntelligentAviation.entity.Flight;
import sghku.tianchi.IntelligentAviation.entity.FlightArc;
import sghku.tianchi.IntelligentAviation.entity.FlightSection;
import sghku.tianchi.IntelligentAviation.entity.FlightSectionItinerary;
import sghku.tianchi.IntelligentAviation.entity.GroundArc;
import sghku.tianchi.IntelligentAviation.entity.Itinerary;
import sghku.tianchi.IntelligentAviation.entity.Scenario;
import sghku.tianchi.IntelligentAviation.entity.Solution;
import sghku.tianchi.IntelligentAviation.model.CplexModel;
import sghku.tianchi.IntelligentAviation.model.CplexModelForPureAircraft;
import sghku.tianchi.IntelligentAviation.model.FourthStageCplexModel;
import sghku.tianchi.IntelligentAviation.model.IntegratedCplexModel;
import sghku.tianchi.IntelligentAviation.model.PushForwardCplexModel;

public class FourthStageRecovery {
	public static void main(String[] args) {

		Parameter.isPassengerCostConsidered = false;
		Parameter.isReadFixedRoutes = true;
		Parameter.gap = 5;
		Parameter.fixFile = "fourthstagefiles/fixschedule";
		
		Parameter.linearsolutionfilename = "gap5_delay9_flightsection1h/linearsolutionwithpassenger_0902_stage1.csv";
		runOneIteration(true, 70);
		/*Parameter.linearsolutionfilename = "linearsolution_0829_stage2.csv";
		runOneIteration(true, 40);*/
	}
		
	public static void runOneIteration(boolean isFractional, int fixNumber){
		Scenario scenario = new Scenario(Parameter.EXCEL_FILENAME);
				
		FlightDelayLimitGenerator flightDelayLimitGenerator = new FlightDelayLimitGenerator();
		
		List<Flight> candidateFlightList = new ArrayList<>();
		List<ConnectingFlightpair> candidateConnectingFlightList = new ArrayList<>();
		List<Flight> candidateStraightenedFlightList = new ArrayList<>();
		
		try {
			Scanner sn = new Scanner(new File("fourthstagefiles/unfixschedule"));
			while(sn.hasNextLine()) {
				String nextLine = sn.nextLine().trim();
				if(nextLine.equals("")) {
					break;
				}
				
				Scanner innerSn = new Scanner(nextLine);
				innerSn.useDelimiter(",");
				innerSn.next();
				innerSn.next();
				while(innerSn.hasNext()) {
					String str = innerSn.next();
					String[] strArray = str.split("_");
					
					if(strArray[0].equals("n")) {
						int fId = Integer.parseInt(strArray[1]);
						Flight f = scenario.flightList.get(fId-1);
						
						candidateFlightList.add(f);
					}else if(strArray[0].equals("c")) {
						int fId1 = Integer.parseInt(strArray[1]);
						int fId2 = Integer.parseInt(strArray[2]);
						Flight f1 = scenario.flightList.get(fId1-1);
						Flight f2 = scenario.flightList.get(fId2-1);
						
						candidateConnectingFlightList.add(scenario.connectingFlightMap.get(f1.id+"_"+f2.id));
					}else if(strArray[0].equals("s")) {
						int fId1 = Integer.parseInt(strArray[1]);
						int fId2 = Integer.parseInt(strArray[2]);
						
						Flight f1 = scenario.flightList.get(fId1-1);
						Flight f2 = scenario.flightList.get(fId2-1);
						
						//生成联程拉直航班
						
						Flight straightenedFlight = new Flight();
						straightenedFlight.isStraightened = true;
						straightenedFlight.connectingFlightpair = scenario.connectingFlightMap.get(f1.id+"_"+f2.id);
						straightenedFlight.leg = straightenedFlight.connectingFlightpair.straightenLeg;
						
						straightenedFlight.flyTime = 0;
								
						straightenedFlight.initialTakeoffT = f1.initialTakeoffT;
						straightenedFlight.initialLandingT = straightenedFlight.initialTakeoffT + straightenedFlight.flyTime;
						
						straightenedFlight.isAllowtoBringForward = f1.isAllowtoBringForward;
						straightenedFlight.isAffected = f1.isAffected;
						straightenedFlight.isDomestic = true;
						straightenedFlight.earliestPossibleTime = f1.earliestPossibleTime;
						straightenedFlight.latestPossibleTime = f1.latestPossibleTime;
						
						flightDelayLimitGenerator.setFlightDelayLimitForStraightenedFlight(straightenedFlight, scenario);
						candidateStraightenedFlightList.add(straightenedFlight);
					}
				}
				
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		List<Aircraft> candidateAircraftList= new ArrayList<>();
		List<Aircraft> fixedAircraftList = new ArrayList<>();
		
		for(Aircraft a:scenario.aircraftList) {
			if(!a.isFixed) {
				candidateAircraftList.add(a);
			}else{
				fixedAircraftList.add(a);
			}
		}
		
		for(Aircraft a:candidateAircraftList){
			for(Flight f:candidateFlightList){
				if(!a.tabuLegs.contains(f.leg)){
					a.singleFlightList.add(f);
				}
			}
			for(Flight f:candidateStraightenedFlightList) {
				if(!a.tabuLegs.contains(f.leg)) {
					a.straightenedFlightList.add(f);
				}
			}
			for(ConnectingFlightpair cf:candidateConnectingFlightList){
				if(!a.tabuLegs.contains(cf.firstFlight.leg) && !a.tabuLegs.contains(cf.secondFlight.leg)){
					a.connectingFlightList.add(cf);
				}
			}
		}
		
		Set<Integer> mustSelectFlightList = new HashSet<>();
		for(Aircraft a:fixedAircraftList) {
			for(Flight f:a.fixedFlightList) {
				if(f.isStraightened) {
					mustSelectFlightList.add(f.connectingFlightpair.firstFlight.id);
					mustSelectFlightList.add(f.connectingFlightpair.secondFlight.id);
				}else {
					mustSelectFlightList.add(f.id);
				}
			}
		}
		
		for(Flight f:candidateFlightList) {
			mustSelectFlightList.add(f.id);
		}
		for(ConnectingFlightpair cf:candidateConnectingFlightList) {
			mustSelectFlightList.add(cf.firstFlight.id);
			mustSelectFlightList.add(cf.secondFlight.id);
		}
		for(Flight f:candidateStraightenedFlightList) {
			mustSelectFlightList.add(f.connectingFlightpair.firstFlight.id);
			mustSelectFlightList.add(f.connectingFlightpair.secondFlight.id);
		}
		
		System.out.println("mustSelectFlightList:"+mustSelectFlightList.size());
		
		//加入fix aircraft航班
		for(int i=0;i<fixedAircraftList.size();i++){
			Aircraft a = fixedAircraftList.get(i);
			
			for(Flight f:a.fixedFlightList){
				a.singleFlightList.add(f);
			}
		}
		
		for(Aircraft a:fixedAircraftList){
			for(int i=0;i<a.fixedFlightList.size()-1;i++){
				Flight f1 = a.fixedFlightList.get(i);
				Flight f2 = a.fixedFlightList.get(i+1);

				f1.isShortConnection = false;
				f2.isShortConnection = false;

				Integer connT = scenario.shortConnectionMap.get(f1.id+"_"+f2.id);
				if(connT != null){
					f1.isShortConnection = true;
					f1.shortConnectionTime = connT;
				}
				
				if(f1.isIncludedInConnecting && f2.isIncludedInConnecting && f1.brotherFlight.id == f2.id){

					ConnectingFlightpair cf = scenario.connectingFlightMap.get(f1.id+"_"+f2.id);
					f1.isConnectionFeasible = true;
					f2.isConnectionFeasible = true;
					
				}
			}
		}
		
		//更新base information
		for(Aircraft a:scenario.aircraftList) {
			a.flightList.get(a.flightList.size()-1).leg.destinationAirport.finalAircraftNumber[a.type-1]++;
		}
			
		//基于目前固定的飞机路径来进一步求解线性松弛模型
		solver(scenario, scenario.aircraftList, scenario.flightList, candidateConnectingFlightList, mustSelectFlightList);		
		
		//根据线性松弛模型来确定新的需要固定的飞机路径
		AircraftPathReader scheduleReader = new AircraftPathReader();
		scheduleReader.fixAircraftRoute(scenario, fixNumber);	
		
		if(!isFractional){
			OutputResultWithPassenger outputResultWithPassenger = new OutputResultWithPassenger();
			outputResultWithPassenger.writeResult(scenario, "firstresult828.csv");
		}
	}
	
	//求解线性松弛模型或者整数规划模型
	public static void solver(Scenario scenario, List<Aircraft> candidateAircraftList, List<Flight> candidateFlightList, List<ConnectingFlightpair> candidateConnectingFlightList, Set<Integer> mustSelectFlightList) {
		buildNetwork(scenario, candidateAircraftList, 5);
		
		FourthStageCplexModel model = new FourthStageCplexModel();
		model.run(candidateAircraftList, candidateFlightList, scenario.airportList, scenario, mustSelectFlightList);

	}

	// 构建时空网络流模型
	public static void buildNetwork(Scenario scenario, List<Aircraft> candidateAircraftList, int gap) {
		
		// 每一个航班生成arc

		// 为每一个飞机的网络模型生成arc
		NetworkConstructorBasedOnDelayAndEarlyLimit networkConstructorBasedOnDelayAndEarlyLimit = new NetworkConstructorBasedOnDelayAndEarlyLimit();
		//ArcChecker.init();
		//System.out.println("total cost："+ArcChecker.totalCost+"  "+ArcChecker.totalCancelCost);
	
		for (Aircraft aircraft : candidateAircraftList) {	
			List<FlightArc> totalFlightArcList = new ArrayList<>();
			List<ConnectingArc> totalConnectingArcList = new ArrayList<>();
		
			for (Flight f : aircraft.singleFlightList) {
				List<FlightArc> faList = networkConstructorBasedOnDelayAndEarlyLimit.generateArcForFlight(aircraft, f, scenario);
				totalFlightArcList.addAll(faList);
			}
			
			for (Flight f : aircraft.straightenedFlightList) {
				List<FlightArc> faList = networkConstructorBasedOnDelayAndEarlyLimit.generateArcForFlight(aircraft, f, scenario);
				totalFlightArcList.addAll(faList);
			}
			
			for(ConnectingFlightpair cf:aircraft.connectingFlightList){				
				List<ConnectingArc> caList = networkConstructorBasedOnDelayAndEarlyLimit.generateArcForConnectingFlightPair(aircraft, cf, scenario);
				totalConnectingArcList.addAll(caList);
			}
			
			networkConstructorBasedOnDelayAndEarlyLimit.eliminateArcs(aircraft, scenario.airportList, totalFlightArcList, totalConnectingArcList, scenario);
			
		}
		
		for(Flight f:scenario.flightList) {
			if(f.isStraightened) {
				
			}
		}
				
		NetworkConstructor networkConstructor = new NetworkConstructor();
		networkConstructor.generateNodes(candidateAircraftList, scenario.airportList, scenario);
	}
	
}
