package sghku.tianchi.IntelligentAviation.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

public class FourthStageCplexModel {
	public IloCplex cplex;

	public Solution run(List<Aircraft> aircraftList, List<Flight> flightList, List<Airport> airportList, Scenario sce, Set<Integer> mustSelectFlightList){
		Solution solution = new Solution();
		solution.involvedAircraftList.addAll(aircraftList);

		
		
		try {

			cplex = new IloCplex();

			List<FlightArc> flightArcList = new ArrayList<>();
			List<ConnectingArc> connectingArcList = new ArrayList<>();
			List<GroundArc> groundArcList = new ArrayList<>();
			List<Node> nodeList = new ArrayList<>();

			for(Aircraft aircraft:aircraftList){
				flightArcList.addAll(aircraft.flightArcList);
				connectingArcList.addAll(aircraft.connectingArcList);
				groundArcList.addAll(aircraft.groundArcList);

				for(int i=0;i<Parameter.TOTAL_AIRPORT_NUM;i++){
					nodeList.addAll(aircraft.nodeListArray[i]);
				}

				nodeList.add(aircraft.sourceNode);
				nodeList.add(aircraft.sinkNode);
			}

			System.out.println("start solving:"+flightArcList.size()+" "+connectingArcList.size()+" "+groundArcList.size()+" "+nodeList.size()+" "+ flightList.size());


			IloNumVar[] x = new IloNumVar[flightArcList.size()];
			IloNumVar[] beta = new IloNumVar[connectingArcList.size()];
			IloNumVar[] y = new IloNumVar[groundArcList.size()];
			IloNumVar[] z = new IloNumVar[flightList.size()];

			IloLinearNumExpr obj = cplex.linearNumExpr();

			for(int i=0;i<flightArcList.size();i++){
				x[i] = cplex.boolVar();

				obj.addTerm(flightArcList.get(i).cost, x[i]);
				flightArcList.get(i).id = i;
			}
			for(int i=0;i<connectingArcList.size();i++){
				beta[i] = cplex.boolVar();		
				
				obj.addTerm(connectingArcList.get(i).cost, beta[i]);
				connectingArcList.get(i).id = i;
			}
			for(int i=0;i<groundArcList.size();i++){
				//y[i] = cplex.boolVar();
				y[i] = cplex.numVar(0, 1);
				groundArcList.get(i).id = i;
			}
			for(int i=0;i<flightList.size();i++){
				Flight f = flightList.get(i);
				//z[i] = cplex.boolVar();
				f.idInCplexModel = i;
				z[i] = cplex.numVar(0, 1);

				obj.addTerm(f.importance*Parameter.COST_CANCEL, z[i]);		
			}

			cplex.addMinimize(obj);
			
			
			//1. flow balance constraints
			for(Node n:nodeList){
				IloLinearNumExpr flowBalanceConstraint = cplex.linearNumExpr();

				for(FlightArc arc:n.flowinFlightArcList){
					flowBalanceConstraint.addTerm(1, x[arc.id]);
				}
				for(FlightArc arc:n.flowoutFlightArcList){
					flowBalanceConstraint.addTerm(-1, x[arc.id]);
				}

				for(ConnectingArc arc:n.flowinConnectingArcList){
					flowBalanceConstraint.addTerm(1, beta[arc.id]);
				}
				for(ConnectingArc arc:n.flowoutConnectingArcList){
					flowBalanceConstraint.addTerm(-1, beta[arc.id]);
				}

				for(GroundArc arc:n.flowinGroundArcList){
					flowBalanceConstraint.addTerm(1, y[arc.id]);
				}
				for(GroundArc arc:n.flowoutGroundArcList){
					flowBalanceConstraint.addTerm(-1, y[arc.id]);
				}

				cplex.addEq(flowBalanceConstraint, 0);
			}

			//2. turn-around arc flow
			for(Aircraft aircraft:aircraftList){
				IloLinearNumExpr turnaroundConstraint = cplex.linearNumExpr();

				turnaroundConstraint.addTerm(1, y[aircraft.turnaroundArc.id]);

				cplex.addEq(turnaroundConstraint, 1);
			}

			//3. for each flight, at least one arc can be selected, the last flight can not be cancelled
			for(int i=0;i<flightList.size();i++){
				Flight f = flightList.get(i);

				IloLinearNumExpr flightSelectionConstraint = cplex.linearNumExpr();

				for(FlightArc arc:f.flightarcList){
					flightSelectionConstraint.addTerm(1, x[arc.id]);
				}

				for(ConnectingArc arc:f.connectingarcList){
					flightSelectionConstraint.addTerm(1, beta[arc.id]);
				}

				if(f.isIncludedInTimeWindow){
					flightSelectionConstraint.addTerm(1, z[i]);	
				}	

				cplex.addEq(flightSelectionConstraint, 1);
			}

			//4. base balance constraints
			for(int i=0;i<airportList.size();i++){
				Airport airport = airportList.get(i);

				for(int j=0;j<Parameter.TOTAL_AIRCRAFTTYPE_NUM;j++) {
					IloLinearNumExpr baseConstraint = cplex.linearNumExpr();
					for(GroundArc arc:airport.sinkArcList[j]){

						baseConstraint.addTerm(1, y[arc.id]);
					}

					cplex.addEq(baseConstraint, airport.finalAircraftNumber[j]);
				}			
			}

			//8. 机场起降约束
			for(String key:sce.keyList) {
				IloLinearNumExpr airportConstraint = cplex.linearNumExpr();
				List<FlightArc> faList = sce.airportTimeFlightArcMap.get(key);
				List<ConnectingArc> caList = sce.airportTimeConnectingArcMap.get(key);

				for(FlightArc arc:faList) {
					airportConstraint.addTerm(1, x[arc.id]);
				}
				for(ConnectingArc arc:caList) {
					airportConstraint.addTerm(1, beta[arc.id]);
				}

				cplex.addLe(airportConstraint, sce.affectAirportLdnTkfCapacityMap.get(key));
			}

			//9. 停机约束
			for(Integer airport:sce.affectedAirportSet) {
				IloLinearNumExpr parkingConstraint = cplex.linearNumExpr();
				List<GroundArc> gaList = sce.affectedAirportCoverParkLimitGroundArcMap.get(airport);
				for(GroundArc ga:gaList) {
					parkingConstraint.addTerm(1, y[ga.id]);					
				}

				cplex.addLe(parkingConstraint, sce.affectedAirportParkingLimitMap.get(airport));
			}

			//10. 25 和 67 停机约束
			IloLinearNumExpr parkingConstraint25 = cplex.linearNumExpr();
			for(GroundArc ga:sce.airport25ClosureGroundArcList){
				parkingConstraint25.addTerm(1, y[ga.id]);
			}
			for(FlightArc arc:sce.airport25ParkingFlightArcList){
				parkingConstraint25.addTerm(1, x[arc.id]);
			}
			for(ConnectingArc arc:sce.airport25ClosureConnectingArcList){
				parkingConstraint25.addTerm(1, beta[arc.id]);
			}
			cplex.addLe(parkingConstraint25, sce.airport25ParkingLimit);

			IloLinearNumExpr parkingConstraint67 = cplex.linearNumExpr();
			for(GroundArc ga:sce.airport67ClosureGroundArcList){
				parkingConstraint67.addTerm(1, y[ga.id]);
			}
			for(FlightArc arc:sce.airport67ParkingFlightArcList){
				parkingConstraint67.addTerm(1, x[arc.id]);
			}
			for(ConnectingArc arc:sce.airport67ClosureConnectingArcList){
				parkingConstraint67.addTerm(1, beta[arc.id]);
			}
			cplex.addLe(parkingConstraint67, sce.airport67ParkingLimit);

			
			for(int id:mustSelectFlightList) {
				Flight f = sce.flightList.get(id-1);
				IloLinearNumExpr cont = cplex.linearNumExpr();
				cont.addTerm(1, z[f.idInCplexModel]);
				cplex.addEq(cont, 0);
			}
			//fix values for each flight arc and connecting arc
			/*for(FlightArc arc:flightArcList){
				IloLinearNumExpr cont = cplex.linearNumExpr();
				cont.addTerm(1, x[arc.id]);
				cplex.addEq(cont, arc.fractionalFlow);
			}
			
			for(ConnectingArc arc:connectingArcList){
				IloLinearNumExpr cont = cplex.linearNumExpr();
				cont.addTerm(1, beta[arc.id]);
				cplex.addEq(cont, arc.fractionalFlow);
			}*/
			
			if(cplex.solve()){

				
					//if we solve it integrally 
					System.out.println("solve: obj = "+cplex.getObjValue());

					solution.objValue = cplex.getObjValue();
					Parameter.objective += cplex.getObjValue();
	
					double totalArcCost = 0;
		
					for(FlightArc fa:flightArcList){

						
						if(cplex.getValue(x[fa.id])>1e-5){

							solution.selectedFlightArcList.add(fa);

							//更新flight arc的时间
							fa.flight.actualTakeoffT = fa.takeoffTime;
							fa.flight.actualLandingT = fa.landingTime;

							fa.flight.aircraft = fa.aircraft;

							fa.fractionalFlow = cplex.getValue(x[fa.id]);							
							//System.out.println("fa:"+fa.fractionalFlow+"  "+fa.cost+" "+fa.delay+" "+fa.aircraft.id+" "+fa.flight.initialAircraft.id+"  "+fa.aircraft.type+" "+fa.flight.initialAircraftType+" "+fa.flight.id+" "+fa.flight.isIncludedInConnecting);
							/*totalOriginalPassengerDelayCost += fa.delayCost;
							totalPassengerCancelCost += fa.connPssgrCclDueToSubseqCclCost;
							totalPassengerCancelCost += fa.connPssgrCclDueToStraightenCost;*/

							/*if(fa.aircraft.id == 141) {
								System.out.println("aircraft 141 fa:"+fa.flight.id+" "+fa.takeoffTime+"->"+fa.landingTime);
							}*/
							if(fa.aircraft.id == 142) {
								System.out.println("aircraft 142 fa:"+fa.flight.id+" "+fa.takeoffTime+"->"+fa.landingTime+" "+fa.cost);
							}
							/*totalArcCost += fa.cost * cplex.getValue(x[fa.id]);
							fa.aircraft.totalCost +=  fa.cost * cplex.getValue(x[fa.id]);*/
						}
					}
					
										
					for(ConnectingArc arc:connectingArcList){
						/*if(arc.aircraft.id == 141) {
							System.out.println("aircraft 141 ca:"+arc.firstArc.flight.id+" "+arc.secondArc.flight.id+" "+arc.firstArc.takeoffTime+" "+arc.secondArc.takeoffTime+" "+arc.firstArc.flight.isIncludedInTimeWindow);
						}*/
						if(cplex.getValue(beta[arc.id]) > 1e-5){

							solution.selectedConnectingArcList.add(arc);
							//更新flight arc的时间

							arc.connectingFlightPair.firstFlight.actualTakeoffT = arc.firstArc.takeoffTime;
							arc.connectingFlightPair.firstFlight.actualLandingT = arc.firstArc.landingTime;

							arc.connectingFlightPair.secondFlight.actualTakeoffT = arc.secondArc.takeoffTime;
							arc.connectingFlightPair.secondFlight.actualLandingT = arc.secondArc.landingTime;

							arc.connectingFlightPair.firstFlight.aircraft = arc.aircraft;
							arc.connectingFlightPair.secondFlight.aircraft = arc.aircraft;
							
							arc.fractionalFlow = cplex.getValue(beta[arc.id]);
							
							/*totalPassengerCancelCost += arc.pssgrCclCostDueToInsufficientSeat;
							totalOriginalPassengerDelayCost += arc.delayCost;*/
							
							/*if(arc.aircraft.id == 141) {
								System.out.println("aircraft 141 ca:"+arc.firstArc.flight.id+" "+arc.secondArc.flight.id+" "+arc.firstArc.takeoffTime+" "+arc.secondArc.takeoffTime);
							}*/
							if(arc.aircraft.id == 142) {
								System.out.println("aircraft 142 fa:"+arc.firstArc.flight.id+" "+arc.secondArc.flight.id+" "+arc.firstArc.takeoffTime+" "+arc.secondArc.takeoffTime+" "+arc.cost);
							}
							/*totalArcCost += arc.cost * cplex.getValue(beta[arc.id]);
							arc.aircraft.totalCost += arc.cost * cplex.getValue(beta[arc.id]);*/
						}
					}
					System.out.println("arc cost:"+totalArcCost);
					
					for(Aircraft a:aircraftList) {
						System.out.print(a.totalCost+",");
					}
					System.out.println();

					double cancelCost = 0;
					for(Flight f:flightList) {
						if(cplex.getValue(z[f.idInCplexModel]) > 1e-5) {
							System.out.print(f.id+",");
							cancelCost += f.importance * Parameter.COST_CANCEL * cplex.getValue(z[f.idInCplexModel]);
						}
					}
					System.out.println();
					System.out.println("cancelCost:"+cancelCost);
					
					for(GroundArc ga:groundArcList){
						if(cplex.getValue(y[ga.id]) > 1e-5){
							ga.fractionalFlow = cplex.getValue(y[ga.id]);
						}
					}
					
					for(String key:sce.keyList) {
						if(key.equals("49_11105")){
							List<FlightArc> faList = sce.airportTimeFlightArcMap.get(key);
							List<ConnectingArc> caList = sce.airportTimeConnectingArcMap.get(key);

							for(FlightArc arc:faList) {
								if(arc.fractionalFlow > 1e-5){
									System.out.println("flight:"+arc.flight.id);
								}
							}
							for(ConnectingArc arc:caList) {
								if(arc.fractionalFlow > 1e-5){
									System.out.println("ca:"+arc.firstArc.flight.id+" "+arc.secondArc.flight.id);
								}
							}
						}
					}
					
										

					for(int i=0;i<flightList.size();i++){
						Flight f = flightList.get(i);

						if(cplex.getValue(z[i]) > 1e-5){

							solution.cancelledFlightList.add(f);

							//totalPassengerCancelCost += f.connectedPassengerNumber*Parameter.passengerCancelCost+Parameter.passengerCancelCost*f.firstTransferPassengerNumber*2;

							f.isCancelled = true;

						}
					}

				}

			else{
				System.out.println("Infeasible!!!");
			}

			cplex.end();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return solution;
	}

}
