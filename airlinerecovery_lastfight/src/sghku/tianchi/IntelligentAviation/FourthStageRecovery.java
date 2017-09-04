package sghku.tianchi.IntelligentAviation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
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
import sghku.tianchi.IntelligentAviation.common.MyFile;
import sghku.tianchi.IntelligentAviation.common.OutputResult;
import sghku.tianchi.IntelligentAviation.common.OutputResultDigitTime;
import sghku.tianchi.IntelligentAviation.common.OutputResultWithPassenger;
import sghku.tianchi.IntelligentAviation.common.Parameter;
import sghku.tianchi.IntelligentAviation.comparator.FlightComparator2;
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
		
	}
	
	public static void readPassengerInformation(Scenario scenario) {
		try {
			Scanner sn = new Scanner(new File("fourthstagefiles/fourStageFlightInfor.csv"));
			while(sn.hasNextLine()) {
				String nextLine = sn.nextLine().trim();
				if(nextLine.equals("")) {
					break;
				}
				System.out.println(nextLine);
				Scanner innerSn = new Scanner(nextLine);
				innerSn.useDelimiter(",");
				Flight f = scenario.flightList.get(innerSn.nextInt()-1);
				f.earliestTimeDecidedBySignChange = innerSn.nextInt();
				f.latestTimeDecidedBySignChange = innerSn.nextInt();
				f.selfPassengerNumber = innerSn.nextInt();
				f.signInPassengerNumber = innerSn.nextInt();
				f.totalPassengerNumber = innerSn.nextInt();
				if(innerSn.hasNext()) {
					String signInMap = innerSn.next();
					if(!signInMap.trim().equals("")) {
						String[] signMapArray = signInMap.split("&");
						for(String signNumberStr:signMapArray) {
							String[] signNumberStrArray = signNumberStr.split(":");
							int signFromFlightId = Integer.parseInt(signNumberStrArray[0]);
							int volume = Integer.parseInt(signNumberStrArray[1]);
							
							f.signChangeMap.put(signFromFlightId, volume);
						}
					}
				}
				
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Map<String,Integer> readTransferConnectionMap() {
		Map<String,Integer> connMap = new HashMap<>();
		
		try {
			Scanner sn = new Scanner(new File("fourthstagefiles/fourStageMinConnectionTime.csv"));
			while(sn.hasNextLine()) {
				String nextLine = sn.nextLine().trim();
				if(nextLine.equals("")) {
					break;
				}
				
				Scanner innerSn = new Scanner(nextLine);
				innerSn.useDelimiter(",");
				
				connMap.put(innerSn.next(), innerSn.nextInt());
			}
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return connMap;
	}
		
	public static void runOneIteration(boolean isFractional, int fixNumber){
		Scenario scenario = new Scenario(Parameter.EXCEL_FILENAME);
		
		readPassengerInformation(scenario);
		Map<String,Integer> connMap = readTransferConnectionMap();
		
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
		solver(scenario, scenario.aircraftList, scenario.flightList, candidateConnectingFlightList, mustSelectFlightList, connMap);		
		
	}
	
	//求解线性松弛模型或者整数规划模型
	public static void solver(Scenario scenario, List<Aircraft> candidateAircraftList, List<Flight> candidateFlightList, List<ConnectingFlightpair> candidateConnectingFlightList, Set<Integer> mustSelectFlightList, Map<String,Integer> connMap) {
		buildNetwork(scenario, candidateAircraftList, 5);
		
		FourthStageCplexModel model = new FourthStageCplexModel();
		model.run(candidateAircraftList, candidateFlightList, scenario.airportList, scenario, mustSelectFlightList, connMap);

		try {
			File file = new File("fourthstagefiles/tempschedule");
			if(file.exists()){
				file.delete();
			}
			MyFile.creatTxtFile("fourthstagefiles/tempschedule");
			StringBuilder sb = new StringBuilder();
			for(Aircraft a:scenario.aircraftList) {
				Collections.sort(a.flightList, new FlightComparator2());
				for(int i=0;i<a.flightList.size()-1;i++){
					Flight f1 = a.flightList.get(i);
					Flight f2 = a.flightList.get(i+1);
					int connT = f2.actualTakeoffT - f1.actualLandingT;
					if(connT < 50){
						Integer shortConnT = scenario.shortConnectionMap.get(f1.id+"_"+f2.id);
						if(shortConnT == null){
							System.out.println("over use short connection");
						}else if(shortConnT > connT){
							System.out.println("error "+shortConnT+" "+connT);
						}
					}
				}
				sb.append(a.id+","+1.0+",");
				for(Flight f:a.flightList) {
					if(f.isStraightened) {
						sb.append("s_"+f.connectingFlightpair.firstFlight.id+"_"+f.connectingFlightpair.secondFlight.id+"_"+f.actualTakeoffT+"_"+f.actualLandingT+",");
					}else {
						sb.append("n_"+f.id+"_"+f.actualTakeoffT+"_"+f.actualLandingT+",");
					}
				}
				sb.append("\n");
			}
			MyFile.writeTxtFile(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		OutputResultDigitTime outputResult = new OutputResultDigitTime();
		outputResult.writeResult(scenario, "fourthstagefiles/finalschedule.csv");
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
