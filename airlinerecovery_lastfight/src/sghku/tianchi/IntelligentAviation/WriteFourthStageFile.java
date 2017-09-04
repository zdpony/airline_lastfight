package sghku.tianchi.IntelligentAviation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import sghku.tianchi.IntelligentAviation.algorithm.FlightDelayLimitGenerator;
import sghku.tianchi.IntelligentAviation.common.MyFile;
import sghku.tianchi.IntelligentAviation.common.Parameter;
import sghku.tianchi.IntelligentAviation.comparator.FlightComparator2;
import sghku.tianchi.IntelligentAviation.entity.Aircraft;
import sghku.tianchi.IntelligentAviation.entity.ConnectingFlightpair;
import sghku.tianchi.IntelligentAviation.entity.Flight;
import sghku.tianchi.IntelligentAviation.entity.Scenario;

public class WriteFourthStageFile {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Scenario scenario = new Scenario(Parameter.EXCEL_FILENAME);
		List<Integer> fixAircraftIdList = new ArrayList<>();
		List<Integer> unfixAircraftIdList = new ArrayList<>();
		//int number = scenario.aircraftList.size();
		
		for(int i=1;i<=20;i++) {
			fixAircraftIdList.add(i);
		}
		for(int i=21;i<=60;i++) {
			unfixAircraftIdList.add(i);
		}
		for(int i=61;i<=scenario.aircraftList.size();i++) {
			fixAircraftIdList.add(i);
		}
		readOptimalSchedule(scenario, "fourthstagefiles/result_0904.csv", fixAircraftIdList, unfixAircraftIdList);
	}

	// 1. 读取之前的schedule
	public static void readOptimalSchedule(Scenario scenario, String fileName, List<Integer> fixAircraftIdList, List<Integer> unfixAircraftIdList) {
		FlightDelayLimitGenerator delayLimitGenerator = new FlightDelayLimitGenerator();
		try {
			for (Aircraft a : scenario.aircraftList) {
				a.flightList.clear();
			}

			Scanner sn = new Scanner(new File(fileName));

			while (sn.hasNextLine()) {
				String nextLine = sn.nextLine().trim();
				if (nextLine.equals("")) {
					break;
				}

				Scanner innerSn = new Scanner(nextLine);
				innerSn.useDelimiter(",");

				int flightId = innerSn.nextInt();
				int originId = innerSn.nextInt();
				int destId = innerSn.nextInt();

				String takeoffString = innerSn.next();
				String[] dayandtime = takeoffString.split(" ");
				int takeoffT = Integer.parseInt(dayandtime[0].split("/")[2]) * 1440
						+ Integer.parseInt(dayandtime[1].split(":")[0]) * 60
						+ Integer.parseInt(dayandtime[1].split(":")[1]);

				String landingString = innerSn.next();
				dayandtime = landingString.split(" ");
				int landingT = Integer.parseInt(dayandtime[0].split("/")[2]) * 1440
						+ Integer.parseInt(dayandtime[1].split(":")[0]) * 60
						+ Integer.parseInt(dayandtime[1].split(":")[1]);

				int aircraftId = innerSn.nextInt();

				int isCancelled = innerSn.nextInt();
				int isStraightend = innerSn.nextInt();
				int isDeadhead = innerSn.nextInt();

				innerSn.next();
				String signChangeStr = "";
				if (innerSn.hasNext()) {
					signChangeStr = innerSn.next().trim();
				}

				Aircraft aircraft = scenario.aircraftList.get(aircraftId - 1);

				if (flightId < 9001) {
					Flight f = scenario.flightList.get(flightId - 1);

					if (isCancelled == 0) {
						if (isStraightend == 1) {
							// 生成联程拉直航班

							ConnectingFlightpair cp = f.connectingFlightpair;

							int flyTime = cp.straightenLeg.flytimeArray[aircraft.type - 1];

							if (flyTime <= 0) { // if cannot retrieve fly time
								flyTime = cp.firstFlight.initialLandingT - cp.firstFlight.initialTakeoffT
										+ cp.secondFlight.initialLandingT - cp.secondFlight.initialTakeoffT;
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
						} else {
							f.actualTakeoffT = takeoffT;
							f.actualLandingT = landingT;
							f.aircraft = aircraft;
							aircraft.flightList.add(f);

						}
					}
				} else {

					System.out.println("this is a dead head arc");
					System.exit(1);
				}
			}

			for (Aircraft a : scenario.aircraftList) {
				for (Flight f : a.flightList) {
					if (f.isStraightened) {
						f.connectingFlightpair.firstFlight.isCancelled = false;
						f.connectingFlightpair.secondFlight.isCancelled = false;

					} else {
						f.isCancelled = false;

					}

				}

				Collections.sort(a.flightList, new FlightComparator2());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			File file = new File("fourthstagefiles/fixschedule");
			if(file.exists()) {
				file.delete();
			}
			MyFile.creatTxtFile("fourthstagefiles/fixschedule");
			StringBuilder sb = new StringBuilder();
			for(int id:fixAircraftIdList) {
				Aircraft a = scenario.aircraftList.get(id-1);
				sb.append(a.id+",1,");
				for(Flight f:a.flightList) {
					if(f.isStraightened) {
						sb.append("s_"+f.connectingFlightpair.firstFlight.id+"_"+f.connectingFlightpair.secondFlight.id+"_"+f.connectingFlightpair.firstFlight.actualTakeoffT+"_"+f.connectingFlightpair.secondFlight.actualTakeoffT+",");
					}else {
						sb.append("n_"+f.id+"_"+f.actualTakeoffT+"_"+f.actualLandingT+",");
					}
				}
				sb.append("\n");
			}
			
			MyFile.writeTxtFile(sb.toString());
			
			file = new File("fourthstagefiles/unfixschedule");
			if(file.exists()) {
				file.delete();
			}
			MyFile.creatTxtFile("fourthstagefiles/unfixschedule");
			sb = new StringBuilder();
			//Set<Integer> visitedFlightSet = new HashSet<>();
			
			for(Aircraft a:scenario.aircraftList) {
				for(Flight f:a.flightList) {
					f.isFixed = false;
				}
			}
			for(int id:unfixAircraftIdList) {
				Aircraft a = scenario.aircraftList.get(id-1);
				sb.append(a.id+",1,");
				
				for(int i=0;i<a.flightList.size();i++) {
					Flight f = a.flightList.get(i);
					if(!f.isFixed) {
						if(f.isStraightened) {
							sb.append("s_"+f.connectingFlightpair.firstFlight.id+"_"+f.connectingFlightpair.secondFlight.id+"_"+f.connectingFlightpair.firstFlight.actualTakeoffT+"_"+f.connectingFlightpair.secondFlight.actualTakeoffT+",");
							f.isFixed = true;
						}else {
							if(i != a.flightList.size()-1) {
								if(f.isIncludedInConnecting) {
									Flight nextFlight = a.flightList.get(i+1);
									if(nextFlight.isIncludedInConnecting && f.brotherFlight.id == nextFlight.id) {
										sb.append("c_"+f.id+"_"+nextFlight.id+"_"+f.actualTakeoffT+"_"+nextFlight.actualTakeoffT+",");
										f.isFixed = true;
										nextFlight.isFixed = true;
									}else {
										sb.append("n_"+f.id+"_"+f.actualTakeoffT+",");		
										f.isFixed = true;
									}
								}else {
									sb.append("n_"+f.id+"_"+f.actualTakeoffT+",");	
									f.isFixed = true;
								}								
							}else {
								sb.append("n_"+f.id+"_"+f.actualTakeoffT+",");							
								f.isFixed = true;
							}
						}
					}
				}
				sb.append("\n");
			}
			
			MyFile.writeTxtFile(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
