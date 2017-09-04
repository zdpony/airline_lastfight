package sghku.tianchi.IntelligentAviation.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import sghku.tianchi.IntelligentAviation.entity.*;

public class FourthStagePassengerInfor {
	public Map<String, Integer> flightConnectionTimeLimit = new HashMap<>();

	public void generateFourthStagePassengerInfor(Scenario sce, String secondTrsfrFileName, String normalSignChangeFileName) {
		List<Flight> flightList = sce.flightList;
		for(Flight f:flightList) {

			f.earliestTimeDecidedBySignChange = f.initialTakeoffT-360;  //初始化为最早提前6个小时
			if(f.isDomestic) {
				f.latestTimeDecidedBySignChange = f.initialTakeoffT + Parameter.MAX_DELAY_DOMESTIC_TIME;
			}else {
				f.latestTimeDecidedBySignChange = f.initialTakeoffT + Parameter.MAX_DELAY_INTERNATIONAL_TIME;
			}

			//计算有多少selfPassenger
			if(f.isCancelled)continue;   //cancel 航班不用计算乘客信息

			f.selfPassengerNumber = f.connectedPassengerNumber + f.normalPassengerNumber + f.transferPassengerNumber;
			
			
			if(f.isStraightened) {  //拉直航班只能乘坐 联程乘客
				f.selfPassengerNumber -= f.normalPassengerNumber;
				f.selfPassengerNumber -= f.transferPassengerNumber;
			}else {  //没有straighten， 计算哪些乘客需要减去
				
				if(f.isIncludedInConnecting && f.brotherFlight.isCancelled) {  //联程另一截被cancel，则联程乘客不能坐
					f.selfPassengerNumber -= f.connectedPassengerNumber;
				}
				
				
				for(TransferPassenger tp:f.secondPassengerTransferList) {
					if(tp.inFlight.isCancelled || tp.inFlight.isStraightened) {  //transfer 对应的第一截删除或拉直,则乘客不会来坐 第二截
						f.selfPassengerNumber -= tp.volume;

					}else if(tp.outFlight.actualTakeoffT - tp.inFlight.actualLandingT < tp.minTurnaroundTime){  //不够时间中转，不会来坐
						f.selfPassengerNumber -= tp.volume;
					}
				}			
			}
		}


		try {
			Scanner sn = new Scanner(new File(secondTrsfrFileName));

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
				
				if(innerSn.hasNext()) {
					String transferNumber = innerSn.next();
					if(!transferNumber.trim().equals("")) {
						String[] transferNumberArray = transferNumber.split("&");
						for(String transferNumberStr:transferNumberArray) {
							String[] transferNumberStrArray = transferNumberStr.split(":");
							int signChangeToFlightId = Integer.parseInt(transferNumberStrArray[0]);
							int volume = Integer.parseInt(transferNumberStrArray[1]);
							
							Flight signChangeToFlight = flightList.get(signChangeToFlightId-1);

							String key = inFlight.id+"_"+signChangeToFlight.id;   //更新 实际需要的transfer turnaround time
							Integer connT = flightConnectionTimeLimit.get(key);

							if(connT == null) {
								flightConnectionTimeLimit.put(key, minTurnaroundTime);
							}else {
								if(minTurnaroundTime > connT) {
									flightConnectionTimeLimit.put(key, minTurnaroundTime);
								}
							}

							//这些乘客加入到signChangeToFlight.signChangePassengerNumber
							signChangeToFlight.signInPassengerNumber += volume;

							//update 每个flight的signChangeMap
							Integer alreadyBearSignChangeVolume = signChangeToFlight.signChangeMap.get(outFlight.id);

							if(alreadyBearSignChangeVolume == null) {
								signChangeToFlight.signChangeMap.put(outFlight.id, volume);
							}else {
								signChangeToFlight.signChangeMap.put(outFlight.id, alreadyBearSignChangeVolume+volume);
							}
							//update 每个flight的最早最晚时间
							if(outFlight.initialTakeoffT > signChangeToFlight.earliestTimeDecidedBySignChange) {
								signChangeToFlight.earliestTimeDecidedBySignChange = outFlight.initialTakeoffT;
							}
							if(outFlight.initialTakeoffT + 48*60 < signChangeToFlight.latestTimeDecidedBySignChange) {
								signChangeToFlight.latestTimeDecidedBySignChange = outFlight.initialTakeoffT + 48*60;
							}

						}
					}					
				}


			}

			sn = new Scanner(new File(normalSignChangeFileName));

			while(sn.hasNextLine()) {
				String nextLine = sn.nextLine().trim();
				if(nextLine.equals("")) {
					break;
				}

				Scanner innerSn = new Scanner(nextLine);
				innerSn.useDelimiter(",");

				int currentFlightId = innerSn.nextInt();
				Flight currentFlight = flightList.get(currentFlightId-1);
				

				if(innerSn.hasNext()) {
					String signChangeNum = innerSn.next();
					if(!signChangeNum.trim().equals("")) {
						String[] signChangeNumberArray = signChangeNum.split("&");
						for(String signChangeNumStr:signChangeNumberArray) {
							String[] scStrArray = signChangeNumStr.split(":");
							int signChangeFromFlightId = Integer.parseInt(scStrArray[0]);
							int volume = Integer.parseInt(scStrArray[1]);
						
							Flight signChangeFromFlight = flightList.get(signChangeFromFlightId-1);

							//这些乘客加入到signChangeToFlight.signChangePassengerNumber
							currentFlight.signInPassengerNumber += volume;

							//update 每个current flight的signChangeMap
							Integer alreadyBearSignChangeVolume = currentFlight.signChangeMap.get(signChangeFromFlight.id);

							if(alreadyBearSignChangeVolume == null) {
								currentFlight.signChangeMap.put(signChangeFromFlight.id, volume);
							}else {
								currentFlight.signChangeMap.put(signChangeFromFlight.id, alreadyBearSignChangeVolume+volume);
							}
							//update 每个flight的最早最晚时间
							if(signChangeFromFlight.initialTakeoffT > currentFlight.earliestTimeDecidedBySignChange) {
								currentFlight.earliestTimeDecidedBySignChange = signChangeFromFlight.initialTakeoffT;
							}
							if(signChangeFromFlight.initialTakeoffT + 48*60 < currentFlight.latestTimeDecidedBySignChange) {
								currentFlight.latestTimeDecidedBySignChange = signChangeFromFlight.initialTakeoffT + 48*60;
							}

						}
					}					
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//根据signChange和passengerCapacity的情况，确定flight上最终乘坐的乘客数量
		for(Flight f:flightList) {
			if(f.isCancelled)continue;   //cancel 航班不用计算乘客信息
			if(f.signInPassengerNumber >0 && f.selfPassengerNumber + f.signInPassengerNumber > f.aircraft.passengerCapacity+1e-5) {
				System.out.println("error! selfPassenger has no seat but some signInPassengers! f_id:"+f.id+" seflNum:"+ f.selfPassengerNumber
						+"signInNum:"+f.signInPassengerNumber+" seatNum:"+f.aircraft.passengerCapacity);
				System.exit(1);	
			}
			
			
			f.totalPassengerNumber = f.selfPassengerNumber + f.signInPassengerNumber; 
			
			if(f.totalPassengerNumber > f.aircraft.passengerCapacity) {
				System.out.println("Self passenger exeeds capacity! f_id:"+f.id+" seflNum:"+ f.selfPassengerNumber
						+"signInNum:"+f.signInPassengerNumber+" seatNum:"+f.aircraft.passengerCapacity);
			}
			
			f.totalPassengerNumber = (f.totalPassengerNumber > f.aircraft.passengerCapacity)?f.aircraft.passengerCapacity : f.totalPassengerNumber;
		}


	}

	public void writeFourthStagePassengerInfor(Scenario sce, String outputflightInforName, String outputMinConnectionTimeMap) {

		StringBuilder sb = new StringBuilder();

		try {
			File file = new File(outputflightInforName);
			if(file.exists()){
				file.delete();
			}
			
			MyFile.creatTxtFile(outputflightInforName);

			for(Flight f:sce.flightList) {
				StringBuilder signChangeSb = new StringBuilder();
				signChangeSb.append(f.id+","+f.earliestTimeDecidedBySignChange+","+
				f.latestTimeDecidedBySignChange+","+f.selfPassengerNumber+","+f.signInPassengerNumber+","+f.totalPassengerNumber+",");
				boolean hasSignIn = false;
				for(Integer fromFlightId:f.signChangeMap.keySet()) {
					signChangeSb.append(fromFlightId+":"+(int)Math.round(f.signChangeMap.get(fromFlightId))+"&");  //fromFlightId : signChangeNum
					hasSignIn = true;
				}
				if(hasSignIn){
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
		
		sb = new StringBuilder();
		try {
			File file = new File(outputMinConnectionTimeMap);
			if(file.exists()){
				file.delete();
			}
			
			MyFile.creatTxtFile(outputMinConnectionTimeMap);

			for(String key:flightConnectionTimeLimit.keySet()) {
				sb.append(key+","+flightConnectionTimeLimit.get(key)+"\n");
			}
			
			sb.deleteCharAt(sb.length()-1);  //去掉最后一个 \n

			MyFile.writeTxtFile(sb.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}



}
