package sghku.tianchi.IntelligentAviation.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Param.MIP.Strategy;
import sghku.tianchi.IntelligentAviation.common.ExcelOperator;
import sghku.tianchi.IntelligentAviation.common.MyFile;
import sghku.tianchi.IntelligentAviation.common.Parameter;
import sghku.tianchi.IntelligentAviation.comparator.FlightComparator2;
import sghku.tianchi.IntelligentAviation.entity.Aircraft;
import sghku.tianchi.IntelligentAviation.entity.Airport;
import sghku.tianchi.IntelligentAviation.entity.ConnectingArc;
import sghku.tianchi.IntelligentAviation.entity.ConnectingFlightpair;
import sghku.tianchi.IntelligentAviation.entity.Flight;
import sghku.tianchi.IntelligentAviation.entity.FlightArc;
import sghku.tianchi.IntelligentAviation.entity.FlightArcItinerary;
import sghku.tianchi.IntelligentAviation.entity.FlightItinerary;
import sghku.tianchi.IntelligentAviation.entity.FlightSection;
import sghku.tianchi.IntelligentAviation.entity.FlightSectionItinerary;
import sghku.tianchi.IntelligentAviation.entity.GroundArc;
import sghku.tianchi.IntelligentAviation.entity.Itinerary;
import sghku.tianchi.IntelligentAviation.entity.Node;
import sghku.tianchi.IntelligentAviation.entity.ParkingInfo;
import sghku.tianchi.IntelligentAviation.entity.PassengerTransfer;
import sghku.tianchi.IntelligentAviation.entity.Path;
import sghku.tianchi.IntelligentAviation.entity.Scenario;
import sghku.tianchi.IntelligentAviation.entity.Solution;
import sghku.tianchi.IntelligentAviation.entity.TransferPassenger;

public class ThirdStageCplexModel {
	public IloCplex cplex;

	public void run(Scenario sce) {
		try {
			cplex = new IloCplex();

			List<Itinerary> iteList = sce.thirdStageIteList;
			List<FlightItinerary> flightIteList = new ArrayList<>();
			List<Flight> flightList = sce.flightList;

			for(int i=0;i<iteList.size();i++) {  
				flightIteList.addAll(iteList.get(i).flightIteList);
			}

			IloNumVar[] x = new IloNumVar[flightList.size()];    //标记flight是否有签转进来的旅客,==1 -> 有签转

			IloNumVar[] y = new IloNumVar[iteList.size()];   //标记Itinerary是否有签转出去的旅客，==1 -> 有签转

			IloNumVar[] passX = new IloNumVar[flightIteList.size()];   //标记flightItinerary有多少成功选择

			IloNumVar[] passCancel = new IloNumVar[iteList.size()];  //标记itinerary有多少被cancel了



			IloLinearNumExpr obj = cplex.linearNumExpr();

			for (int i = 0; i < flightList.size(); i++) {
				Flight f = flightList.get(i);
				f.idInCplexModel = i;
				x[i] = cplex.boolVar();
				if(f.id==11) {
					//System.out.println("f_id-"+f.id+" remainSeatNum:"+f.remainingSeatNum+" flightIteSize:"+f.iteList.get(0).flightIteList.size());
					for(FlightItinerary fi:f.iteList.get(0).flightIteList) {
						
					}
				}
			}
			for (int i = 0; i < iteList.size(); i++) {
				Itinerary ite = iteList.get(i);
				y[i] = cplex.boolVar();
			}
			for (int i = 0; i < flightIteList.size(); i++) {
				FlightItinerary fi = flightIteList.get(i);
				fi.id = i;
				passX[i] = cplex.intVar(0, fi.thirdStageite.volume); // 可以最多转成volume的乘客量
				obj.addTerm(fi.unitCost, passX[i]);
			}
			for (int i = 0; i < iteList.size(); i++) {
				Itinerary ite = iteList.get(i);
				ite.id = i;
				passCancel[i] = cplex.intVar(0, ite.volume);
				obj.addTerm(Parameter.passengerCancelCost, passCancel[i]);  
			}		
			
			/*Map<String, FlightItinerary> fiMap = new HashMap<>();
			for(FlightItinerary fi:flightIteList) {
				if(fi.thirdStageite.tp == null) {
					fiMap.put(fi.thirdStageite.flight.id+"_"+fi.flight.id, fi);
				}
			}
			
			//读入预先设定的decision variable值
			try {
				Scanner sn = new Scanner(new File("rachelresult/secondstageresult"));
				
				while(sn.hasNextLine()) {
					String nextLine = sn.nextLine().trim();
					if(nextLine.equals("")) {
						break;
					}
					String[] nextLineArray = nextLine.split(",");
					String key = nextLineArray[0];
					double value = Double.parseDouble(nextLineArray[1]);
					
					fiMap.get(key).volume = value;
					fiMap.get(key).thirdStageite.signChangeFlag = 1;
					fiMap.get(key).flight.signChangeFlag = 1;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for (Itinerary ite : iteList) {
				double sum1 = 0;
				double sum2 = 0;
				// 转签的乘客数量
				for (FlightItinerary fi : ite.flightIteList) {
					sum1 += fi.volume;
					sum2 += fi.volume;
				}
				// cancel的乘客数量
				ite.cancel = ite.volume - sum1;
				
				if(ite.cancel < -1e-5) {
					System.out.println("error 1  "+ite.volume+" "+sum1+" "+ite.flight.id);
				}
				
				sum2 = sum2 - ite.volume * ite.signChangeFlag;
				if(sum2 > 1e-5) {
					System.out.println("error 2");
				}
				
			}

			// 2. flight的remainingSeatNum
			for (Flight f: flightList) {
				
				double sum1 = 0;
				double sum2 = 0;
				
				for (FlightItinerary fi : f.flightIteList) {  //所有可能签转到此flight的flightItinerary
					sum1 += fi.volume;
					sum2 += fi.volume;
				}
				
				sum2 = sum2 - f.remainingSeatNum*f.signChangeFlag;
				
				if(sum1 > f.remainingSeatNum+1e-5) {
					System.out.println("error 3: "+sum1);
				}
				if(sum2 > 1e-5) {
					System.out.println("error 4");
				}
			}

			// 3. x_flight, y_itinerary, 辨识 -> sum(y_ite) - iteNum*(1 - x_f) <= 0
			for (Flight f: flightList) {
				double sum1 = 0;

				for (Itinerary ite: f.iteList) {
					sum1 += ite.signChangeFlag;
				}

				sum1 += iteList.size() * f.signChangeFlag;

				if(sum1 > iteList.size()+1e-5) {
					System.out.println("error 5");
				}
			}
			
			double objValue = 0;
			
			for (int i = 0; i < flightIteList.size(); i++) {
				FlightItinerary fi = flightIteList.get(i);
				fi.id = i;
				objValue += fi.unitCost*fi.volume;
			}
			for (int i = 0; i < iteList.size(); i++) {
				Itinerary ite = iteList.get(i);
				ite.id = i;
				objValue += Parameter.passengerCancelCost * ite.cancel; 
			}
			
			System.out.println("objValue:"+objValue);
			
			System.exit(1);*/
			
			cplex.addMinimize(obj);


			// 1. Itinerary Volume
			for (Itinerary ite : iteList) {
				IloLinearNumExpr iteNumConstraint = cplex.linearNumExpr();
				IloLinearNumExpr whetherSignChangeConstraint = cplex.linearNumExpr();
				// 转签的乘客数量
				for (FlightItinerary fi : ite.flightIteList) {
					iteNumConstraint.addTerm(1, passX[fi.id]);
					whetherSignChangeConstraint.addTerm(1, passX[fi.id]);
				}
				// cancel的乘客数量
				iteNumConstraint.addTerm(1, passCancel[ite.id]);
				whetherSignChangeConstraint.addTerm(-ite.volume, y[ite.id]);
				// 加起来等于总数量
				cplex.addEq(iteNumConstraint, ite.volume);
				cplex.addLe(whetherSignChangeConstraint,0);
			}

			// 2. flight的remainingSeatNum
			for (Flight f: flightList) {
				
				IloLinearNumExpr seatConstraint = cplex.linearNumExpr();
				IloLinearNumExpr whetherSignChangeConstraint = cplex.linearNumExpr();
				for (FlightItinerary fi : f.flightIteList) {  //所有可能签转到此flight的flightItinerary
					seatConstraint.addTerm(1, passX[fi.id]);
					whetherSignChangeConstraint.addTerm(1, passX[fi.id]);
				}
				whetherSignChangeConstraint.addTerm(-f.remainingSeatNum, x[f.idInCplexModel]);
				cplex.addLe(seatConstraint, f.remainingSeatNum);
				cplex.addLe(whetherSignChangeConstraint,0);
			}

			// 3. x_flight, y_itinerary, 辨识 -> sum(y_ite) - iteNum*(1 - x_f) <= 0
			for (Flight f: flightList) {
				if(f.id==806)System.out.println("806 itelistSize "+f.iteList.size());
				IloLinearNumExpr signChangeConstraint = cplex.linearNumExpr();

				for (Itinerary ite: f.iteList) {
					signChangeConstraint.addTerm(1, y[ite.id]);
				}

				signChangeConstraint.addTerm(iteList.size(), x[f.idInCplexModel]);

				cplex.addLe(signChangeConstraint,iteList.size());
			}


			if (cplex.solve()) {
				for(Flight f:flightList) {  
					if(f.itinerary!=null)f.itinerary.flightArcItineraryList.clear();  //清空签转（出去的）信息，用来装新生成的信息
				}			
				
				System.out.println("solve: obj = " + cplex.getObjValue());

				double totalSignChangeDelayCost = 0;
				double totalNormalAndSecondTrsfrCancelCost = 0;
				
				
				for (int i = 0; i < flightIteList.size(); i++) {
					FlightItinerary fi = flightIteList.get(i);
					if (cplex.getValue(passX[i]) > 1e-5) {
						//System.out.println("some signchange!");
						// 更新具体转签行程信息
						fi.volume = cplex.getValue(passX[i]);
						//fi.flight.flightIteList.add(fi);
						totalSignChangeDelayCost += fi.volume * fi.unitCost;
						
						// 信息装入flight，方便最后output结果
						Itinerary ite = new Itinerary();
						ite.flight = fi.thirdStageite.flight;  //从这个Flight签转出去
						
						FlightArc fa = new FlightArc();
						fa.flight = fi.flight;   //签转到这个flight

						FlightArcItinerary fai = new FlightArcItinerary();			
						fai.itinerary = ite;
						fai.flightArc = fa;
						fai.volume = fi.volume;
						
						boolean isFaiExisting = false;
						for(FlightArcItinerary existingFai:fi.thirdStageite.flight.itinerary.flightArcItineraryList) {
							if(existingFai.flightArc.flight.id == fa.flight.id) {  //已经有这个arc
								existingFai.volume += fai.volume;
								isFaiExisting = true;
								break;
							}
						}
						if(!isFaiExisting)fi.thirdStageite.flight.itinerary.flightArcItineraryList.add(fai);  //没有对应arc，加入新建的这个
						
						fi.flight.testCost += fi.volume * fi.unitCost;
					}
					
				}
				//单独输出TP的中转信息
				for (int i = 0; i < flightIteList.size(); i++) {
					FlightItinerary fi = flightIteList.get(i);
					if (cplex.getValue(passX[i]) > 1e-5) {  //有签转发生
						if(fi.thirdStageite.tp!=null) {  //普通乘客不考虑
							fi.volume = cplex.getValue(passX[i]);
							fi.thirdStageite.tp.flightIteList.add(fi);
						}else {  //普通乘客的放进signInflight对应的normalSignInFIList
							fi.volume = cplex.getValue(passX[i]);
							fi.flight.normalSignInFIList.add(fi);
						}
					}					
				}
				
				for (int i = 0; i < iteList.size(); i++) {
					Itinerary ite = iteList.get(i);
					if (cplex.getValue(passCancel[i]) > 1e-5) {
						if(ite.tp!=null) {
							ite.tp.cancelNum = (int)Math.round(cplex.getValue(passCancel[i]));
						}
						
					}				
				}
				
				
				System.out.println("totalSignChangeDelayCost:" + totalSignChangeDelayCost);

				for (int i = 0; i < iteList.size(); i++) {
					Itinerary ite = iteList.get(i);
					if (cplex.getValue(passCancel[i]) > 1e-5) {
						totalNormalAndSecondTrsfrCancelCost += cplex.getValue(passCancel[i])*4.0;
						//把每个itinerary被cancel掉的存进它对应的flight的normalPassengerCancelNum 属性
						iteList.get(i).flight.normalAndSecondTrsfrPassengerCancelNum = cplex.getValue(passCancel[i]); 
					}
				}
				
				System.out.println("totalNormalAndSecondTrsfrCancelCost:" + totalNormalAndSecondTrsfrCancelCost);
				
				double totalCancelNum = 0;
				for(Flight f:flightList) {
					if(f.isIncludedInTimeWindow) {
						double totalSignOutChangePassNum = 0;
						double passengerCancelNum = 0;
						if(f.itinerary!=null) {
							for(FlightArcItinerary fai:f.itinerary.flightArcItineraryList) {
								totalSignOutChangePassNum += fai.volume;
							}
						}
						
						
						if(f.isCancelled) {
							if(f.isIncludedInConnecting && f.connectingFlightpair.firstFlight.id == f.id && f.passengerNumber+f.connectedPassengerNumber - totalSignOutChangePassNum > 0) {
								passengerCancelNum += (f.passengerNumber + f.connectedPassengerNumber - totalSignOutChangePassNum);
							}else {
								if(f.passengerNumber - totalSignOutChangePassNum > 0) {
									passengerCancelNum += (f.passengerNumber - totalSignOutChangePassNum);
								}
							}
							
						}else if(f.isStraightened) {
							passengerCancelNum += (f.passengerNumber - totalSignOutChangePassNum);
							
						}else {
							int totalPassengerNum = f.passengerNumber + f.connectedPassengerNumber;
						    //获得中转到当前航班的取消旅客数量
							int totalCancelPassenger = 0;
		                    //获得中转失败的旅客数量
		                    int transferFailPassNum =  0;
		                
		                    int transferCancelPassNum =  0;
		                    for(TransferPassenger tp:sce.transferPassengerList) {
		                    	if(tp.outFlight.id==f.id) {
		                    		if(tp.inFlight.isCancelled || tp.inFlight.isStraightened) {
		                    			transferCancelPassNum += tp.volume;
		                    		}else if(f.actualTakeoffT - tp.inFlight.actualLandingT < tp.minTurnaroundTime) {
		                    			transferFailPassNum += tp.volume;
		                    		}          			
		                    	}	                    	          
		                    }        
		                    
		                    totalPassengerNum -= transferCancelPassNum;
		                    totalCancelPassenger += transferCancelPassNum;
          
		                    if(transferFailPassNum > 0){ //中转失败
		                        totalPassengerNum -= transferFailPassNum;
		                        totalCancelPassenger += transferFailPassNum;
		                    }
		                    if(f.isIncludedInConnecting){//如果是没有拉直的联程航班
		                        if(f.brotherFlight.isCancelled) { //联程航班取消
		                            totalPassengerNum -= f.connectedPassengerNumber;
		                            if(f.id == f.connectingFlightpair.firstFlight.id){
		                                totalCancelPassenger += f.connectedPassengerNumber;
		                            }
		                        }
		                    }
		                    int seatNum = f.aircraft.passengerCapacity;
		                    if(totalPassengerNum > seatNum){//换飞机、机型变化、超售（其实就是乘客数量大于座位数量）
		                        int overSeatNum = totalPassengerNum - seatNum;
		                        totalPassengerNum -= overSeatNum;
		                        totalCancelPassenger += overSeatNum;
		                    }else {
		                    	int remainingSeatNum = seatNum - totalPassengerNum;
		                    	if(remainingSeatNum > 0) {
		                    		for (int i = 0; i < iteList.size(); i++) {
		            					Itinerary ite = iteList.get(i);
		            					if (cplex.getValue(passCancel[i]) > 1e-5) {
		            						if(ite.flight.leg.equals(f.leg) && ite.flight.initialTakeoffT <= f.actualTakeoffT &&f.actualTakeoffT<= ite.flight.initialTakeoffT+48*60) {
		            							if(ite.tp==null) {
		            								//System.out.println("we find a candidate flight for a normal itinerary@!@ way 1:"+remainingSeatNum+" "+f.id+" "+ite.flight.id+" can "+cplex.getValue(passCancel[i])+" previous volume:"+ite.volume+" "+seatNum+" "+totalPassengerNum);
		            							}else if(f.actualTakeoffT>=ite.tp.inFlight.actualLandingT+ite.tp.minTurnaroundTime) {
		            								//System.out.println("we find a candidate flight for a transfer itinerary@!@ way 2:"+remainingSeatNum+" "+f.id+" "+ite.flight.id);
		            							}
		            							
		            						}
		            					}
		            				}
		                    	}
		                    
		                    	//System.out.println("seats not fully occupied : "+f.id+" "+totalPassengerNum+" "+seatNum);
		                    }
		                    if(totalSignOutChangePassNum <= totalCancelPassenger){//签转旅客数量不能大于可以取消的旅客数量
		                    	passengerCancelNum += (totalCancelPassenger - totalSignOutChangePassNum);
		                    }
		                    
						}
						
						if(passengerCancelNum>0)System.out.println("cancelNum "+f.id+" "+passengerCancelNum);
						totalCancelNum += passengerCancelNum;
					}
					
				}
				System.out.println("totalCancelNum:"+totalCancelNum +"   then show my calculation:");

				
				
			} else {
				System.out.println("Infeasible!!!");
			}

			cplex.end();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
