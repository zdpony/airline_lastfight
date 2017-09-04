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

public class FourthhalfStageRecovery {
	public static void main(String[] args) {

		Parameter.isPassengerCostConsidered = false;
		Parameter.isReadFixedRoutes = true;
		Parameter.gap = 5;
		Parameter.fixFile = "fourthstagefiles/tempschedule";
		
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
		
		List<Aircraft> fixedAircraftList = new ArrayList<>();
		
		for(Aircraft a:scenario.aircraftList) {
			if(!a.isFixed) {
				System.out.println("error there exists one aircraft not fixed!");
				System.exit(1);
			}else{
				fixedAircraftList.add(a);
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
		solver(scenario, scenario.aircraftList, scenario.flightList, mustSelectFlightList, connMap);		
		
	}
	
	//求解线性松弛模型或者整数规划模型
	public static void solver(Scenario scenario, List<Aircraft> candidateAircraftList, List<Flight> candidateFlightList, Set<Integer> mustSelectFlightList, Map<String,Integer> connMap) {
		buildNetwork(scenario, candidateAircraftList, 5);
		
		FourthStageCplexModel model = new FourthStageCplexModel();
		model.run(candidateAircraftList, candidateFlightList, scenario.airportList, scenario, mustSelectFlightList, connMap);

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
				f.isFixed = false;
				List<FlightArc> faList = networkConstructorBasedOnDelayAndEarlyLimit.generateArcForFlight(aircraft, f, scenario);
				totalFlightArcList.addAll(faList);
			}
			
			networkConstructorBasedOnDelayAndEarlyLimit.eliminateArcs(aircraft, scenario.airportList, totalFlightArcList, totalConnectingArcList, scenario);
			
		}
				
		NetworkConstructor networkConstructor = new NetworkConstructor();
		networkConstructor.generateNodes(candidateAircraftList, scenario.airportList, scenario);
	}
	
}
