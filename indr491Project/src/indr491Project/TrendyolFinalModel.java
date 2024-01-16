// May 29,2020 Indr491 Project 
// Trendyol Allocation of Orders to the Sorter Programming Model
// prepared by Caner Kayalı

// external cplex.jar library referencing is required to run

package indr491Project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public class TrendyolFinalModel {

	public static void main(String[] args) throws IOException, IloException {

		// reading input data from csv format, converting it into Triplet format
		// string argument should be changed according to csv file path
		List<Triplet> inputList = fromCSVtoTripletList("/Users/canerkayali/Desktop/Indr491FinalSubmission/data.csv");

		// number of chutes in the system
		int K = 10;

		// number of maximum allowed batch assignments to a chute
		int chuteCap = 7;

		// parametrization constant for objective function pieces. alpha acts as the
		// importance of single batch assignmets while 1-alpha acts as the importance of
		// total number of item assignments.
		double alpha = 0.5;

		// running the model until all boutiques and batches are processed as single
		// assignments
		SorterAllocation(inputList, K, chuteCap, alpha);

	}

	// function for the model being solved step by step
	private static void SorterAllocation(List<Triplet> inputList, int K, int chuteCap, double alpha)
			throws IloException {

		// this HashMap stores boutiqueID as key and a list of integers representing
		// number of items in that boutique as value
		HashMap<Integer, List<Integer>> boutiqueIDNumOfItems = new HashMap<Integer, List<Integer>>();

		// store the initial boutiqueID from inputList
		int startingBoutiqueID = inputList.get(0).getboutiqueID();
		// store the last boutiqueID from inputList
		int endingBoutiqueID = inputList.get(inputList.size() - 1).getboutiqueID();

		// starting from first boutiqueID until the last, putting elements into the
		// HashMap boutiqueIDNumOfItems
		while (startingBoutiqueID != endingBoutiqueID + 1) {
			List<Integer> tempList = new ArrayList<Integer>();

			for (int i = 0; i < inputList.size(); i++) {
				if (inputList.get(i).getboutiqueID() == startingBoutiqueID) {
					tempList.add(inputList.get(i).getnumOfItems());
				}
			}
			boutiqueIDNumOfItems.put(startingBoutiqueID, tempList);
			startingBoutiqueID++;
		}

		// initializing numberOfItems: each integer array represents the number of items
		// in the batches of the boutique of that index
		ArrayList<int[]> numberOfItems = new ArrayList<int[]>();

		// looping through values of the HashMap to add them to ArrayList<int[]>
		// numberOfItems
		for (List<Integer> myList : boutiqueIDNumOfItems.values()) {
			int[] intarr = new int[myList.size()];
			for (int i = 0; i < myList.size(); i++) {
				intarr[i] = myList.get(i);
			}
			numberOfItems.add(intarr);
		}

		// creating an array list of integers to represent boutique sizes at each index.
		// The number of integers at each index of numberOfItems gives that value.
		ArrayList<Integer> numberOfBatches = new ArrayList<Integer>();

		// calculating the total number of items within the system as condition of stage
		// looping. Storing this as sum As long as there are items to process the
		// overall while loop continues
		int sum = 0;
		for (int i = 0; i < numberOfItems.size(); i++) {
			for (int j = 0; j < numberOfItems.get(i).length; j++) {
				sum += numberOfItems.get(i)[j];
			}
		}

		// initializing the stage number
		int stage = 0;

		// the model runs through many stages as long as there still remains batches and
		// items to be processed. this is checked by calculating the sum value after
		// each stage.
		while (sum > 0) {
			// incrementing stage number and printing it
			stage++;
			System.out.println("Stage " + stage + " beginning! ");
			System.out.println();

			// each entry in numberOfBatches represents the total number of batches in a
			// certain boutique
			for (int[] intarr : numberOfItems) {
				numberOfBatches.add(intarr.length);
			}

			// writing lines for the input data for the specific stage number
			System.out.println("Solving model for the following: ");
			System.out.println();
			System.out.println("numberOfBatches: ");
			System.out.println(numberOfBatches);
			System.out.println();
			System.out.println("numberOfItems: ");
			for (int[] intarr : numberOfItems) {
				System.out.print("[ ");
				for (int j : intarr) {
					System.out.print(j);
					System.out.print(" , ");
				}
				System.out.println("] ");
			}
			System.out.println();
			System.out.println();

			// extract I and J to represent total number of boutiques and batches,
			// respectively.
			// size of numberOfBatches gives total number of boutiques.
			// J selected as maximum element in numberOfBatches as only so many variables
			// representing batches are needed regardless of boutique number.
			int I = numberOfBatches.size();
			int J = Collections.max(numberOfBatches);

			///// create the variable sets
			IloIntVar[][][] X = new IloIntVar[I][J][K];
			IloIntVar[] T = new IloIntVar[I];
			IloIntVar[] Y = new IloIntVar[K];
			IloIntVar[] Z = new IloIntVar[K];

			///// create the dummy variable sets
			IloIntVar[] t = new IloIntVar[I];
			IloIntVar[][] s = new IloIntVar[I][K];

			// the model tends to have slight calculation errors. These extra arrays will
			// hold true solution values of exact boolean and integer values instead of
			// double values since these are required in update procedure after each stage
			int[][][] XBool = new int[I][J][K];
			int[] YInt = new int[K];
			int[] ZBool = new int[K];
			int[] tInt = new int[I];

			///// create arrays for holding the constraint equations
			IloLinearNumExpr[] XY_Relationship = new IloLinearNumExpr[K];
			IloLinearNumExpr[] XT_Relationship = new IloLinearNumExpr[I];
			IloLinearNumExpr[] YZ_Relationship_1 = new IloLinearNumExpr[K];
			IloLinearNumExpr[] YZ_Relationship_2 = new IloLinearNumExpr[K];

			IloLinearNumExpr[][] batchAssignedToOneChute = new IloLinearNumExpr[I][J];
			IloLinearNumExpr[][] batchAssignedFromSameBoutique_1 = new IloLinearNumExpr[I][K];
			IloLinearNumExpr[][] batchAssignedFromSameBoutique_2 = new IloLinearNumExpr[I][K];
			IloLinearNumExpr[] T_Either_Or_1 = new IloLinearNumExpr[I];
			IloLinearNumExpr[] T_Either_Or_2 = new IloLinearNumExpr[I];

			// build the model
			IloCplex model = new IloCplex();

			// DECISION VARIABLES

			// build binary X
			// X_i, j, k is equal to 1 if boutique i's batch j is assigned to chute k
			for (int i = 0; i < I; i++) {
				for (int j = 0; j < numberOfItems.get(i).length; j++) {
					for (int k = 0; k < K; k++) {
						X[i][j][k] = model.boolVar("X_" + (i + 1) + "," + (j + 1) + "," + (k + 1));
					}
				}
			}

			// build nonnegative integers Y and binary Z
			// Y_k represents the total number of batches assigned to chute k
			// Z_k is equal to 1 if Y_k is equal to 1, meaning that a single assignment was
			// made to chute k
			for (int k = 0; k < K; k++) {
				Y[k] = model.intVar(0, chuteCap, "Y_" + (k + 1));
				Z[k] = model.boolVar("Z_" + (k + 1));
			}

			// build nonnegative integers T and dummy binary t
			// T_i represents the total assignments made from boutique i
			// t_i equals 1 if T_i is equal to numberOfBatches[i]
			// t_i equals 0 if T_i is equal to 0
			for (int i = 0; i < I; i++) {
				T[i] = model.intVar(0, numberOfBatches.get(i), "T_" + (i + 1));
				t[i] = model.boolVar("t_" + (i + 1));
			}

			// build dummy binary s
			// s_i,k equals 1 if no batch was assigned from boutiques other than i to chute
			// k
			// s_i,k equals 0 if no batch was assigned from boutique i to chute k
			for (int i = 0; i < I; i++) {
				for (int k = 0; k < K; k++) {
					s[i][k] = model.boolVar("s_" + (i + 1) + "," + (k + 1));
				}
			}

			/////////////////

			// OBJECTIVE FUNCTION

			// build the objective function
			IloLinearNumExpr objTerms = model.linearNumExpr();

			// objective function part1 : sum of z values (single assignments)
			for (int k = 0; k < K; k++) {
				objTerms.addTerm(alpha, Z[k]);
			}

			// objective function part2 : sum of number of items assigned
			for (int i = 0; i < I; i++) {
				for (int j = 0; j < numberOfItems.get(i).length; j++) {
					for (int k = 0; k < K; k++) {
						objTerms.addTerm((1 - alpha) * numberOfItems.get(i)[j], X[i][j][k]);
					}
				}
			}

			// set objective function to be maximized
			model.addMaximize(objTerms);

			///////////////////////////

			// CONSTRAINTS

			// construct x-y relationship constraints
			for (int k = 0; k < K; k++) {
				XY_Relationship[k] = model.linearNumExpr();
				for (int i = 0; i < I; i++) {
					for (int j = 0; j < numberOfItems.get(i).length; j++) {
						XY_Relationship[k].addTerm(1, X[i][j][k]);
					}
				}
				XY_Relationship[k].addTerm(-1, Y[k]);
				model.addEq(XY_Relationship[k], 0);
			}

			// construct x-t relationship constraints
			for (int i = 0; i < I; i++) {
				XT_Relationship[i] = model.linearNumExpr();
				for (int j = 0; j < numberOfItems.get(i).length; j++) {
					for (int k = 0; k < K; k++) {
						XT_Relationship[i].addTerm(1, X[i][j][k]);
					}
				}
				XT_Relationship[i].addTerm(-1, T[i]);
				model.addEq(XT_Relationship[i], 0);
			}

			// create bigM as a large enough value for bigM type constraints
			int bigM = 1000;

			// construct y-z relationship_1 and y-z relationship_2 constraints
			for (int k = 0; k < K; k++) {
				YZ_Relationship_1[k] = model.linearNumExpr();
				YZ_Relationship_1[k].addTerm(1, Z[k]);
				YZ_Relationship_1[k].addTerm(-1, Y[k]);
				model.addLe(YZ_Relationship_1[k], 0);

				YZ_Relationship_2[k] = model.linearNumExpr();
				YZ_Relationship_2[k].addTerm(1, Y[k]);
				YZ_Relationship_2[k].addTerm(bigM, Z[k]);
				model.addLe(YZ_Relationship_2[k], bigM + 1);
			}

			// construct batchAssignedToOneChute constraints
			for (int i = 0; i < I; i++) {
				for (int j = 0; j < numberOfItems.get(i).length; j++) {
					batchAssignedToOneChute[i][j] = model.linearNumExpr();
					for (int k = 0; k < K; k++) {
						batchAssignedToOneChute[i][j].addTerm(1, X[i][j][k]);
					}
					model.addLe(batchAssignedToOneChute[i][j], 1);
				}
			}

			// construct batchAssignedFromSameBoutique_1 constraints
			for (int i = 0; i < I; i++) {
				for (int k = 0; k < K; k++) {
					batchAssignedFromSameBoutique_1[i][k] = model.linearNumExpr();
					for (int j = 0; j < numberOfItems.get(i).length; j++) {
						batchAssignedFromSameBoutique_1[i][k].addTerm(1, X[i][j][k]);
					}
					batchAssignedFromSameBoutique_1[i][k].addTerm(-bigM, s[i][k]);
					model.addLe(batchAssignedFromSameBoutique_1[i][k], 0);
				}
			}

			// construct batchAssignedFromSameBoutique_2 constraints
			for (int i = 0; i < I; i++) {
				for (int k = 0; k < K; k++) {
					batchAssignedFromSameBoutique_2[i][k] = model.linearNumExpr();
					for (int ii = 0; ii < I; ii++) {
						if (ii != i) {
							for (int j = 0; j < numberOfItems.get(ii).length; j++) {
								batchAssignedFromSameBoutique_2[i][k].addTerm(1, X[ii][j][k]);
							}
						}
					}
					batchAssignedFromSameBoutique_2[i][k].addTerm(bigM, s[i][k]);
					model.addLe(batchAssignedFromSameBoutique_2[i][k], bigM);
				}
			}

			// construct T_Either_Or_1 and T_Either_Or_2 constraints
			for (int i = 0; i < I; i++) {
				T_Either_Or_1[i] = model.linearNumExpr();
				T_Either_Or_1[i].addTerm(1, T[i]);
				T_Either_Or_1[i].addTerm(-bigM, t[i]);
				model.addLe(T_Either_Or_1[i], 0);

				T_Either_Or_2[i] = model.linearNumExpr();
				T_Either_Or_2[i].addTerm(-1, T[i]);
				T_Either_Or_2[i].addTerm(bigM, t[i]);
				model.addLe(T_Either_Or_2[i], bigM - numberOfBatches.get(i));
			}

			// this avoids unnecessary console output lines during calculations
			model.setOut(null);

			// checking whether the model is solved. All following commands depends on this
			// condition
			if (model.solve()) {
				System.out.println("Stage " + stage + " model solved! ");

				// print the objective function in open form and its optimal solution value
				System.out.println("Optimal Solution Found!");
				System.out.println();
				System.out.println("Objective Function: " + objTerms);

				double solVal = model.getObjValue();
				System.out.println();
				System.out.println("Optimal Solution: " + solVal);
				System.out.println();

//				// print the constraints: comment in for enabling console output
//
//				System.out.println(" ----- Constraints ----- ");
//				System.out.println();
//				System.out.println();
//
//				// XY_Relationship constraints
//				System.out.println(" --- XY_Relationship --- ");
//				System.out.println();
//				for (int k = 0; k < K; k++) {
//					System.out.println(XY_Relationship[k]);
//				}
//				System.out.println();
//
//				// XT_Relationship constraints
//				System.out.println(" --- XT_Relationship --- ");
//				System.out.println();
//				for (int i = 0; i < I; i++) {
//					System.out.println(XT_Relationship[i]);
//				}
//				System.out.println();
//
//				// YZ_Relationship_1 constraints
//				System.out.println(" --- YZ_Relationship_1 --- ");
//				System.out.println();
//				for (int k = 0; k < K; k++) {
//					System.out.println(YZ_Relationship_1[k]);
//				}
//				System.out.println();
//
//				// YZ_Relationship_2 constraints
//				System.out.println(" --- YZ_Relationship_2 --- ");
//				System.out.println();
//				for (int k = 0; k < K; k++) {
//					System.out.println(YZ_Relationship_2[k]);
//				}
//				System.out.println();
//
//				// batchAssignedToOneChute constraints
//				System.out.println(" --- batchAssignedToOneChute --- ");
//				System.out.println();
//				for (int i = 0; i < I; i++) {
//					for (int j = 0; j < numberOfItems.get(i).length; j++) {
//						System.out.println(batchAssignedToOneChute[i][j]);
//					}
//					System.out.println();
//				}
//				System.out.println();
//
//				// batchAssignedFromSameBoutique_1 constraints
//				System.out.println(" --- batchAssignedFromSameBoutique_1 --- ");
//				System.out.println();
//				for (int i = 0; i < I; i++) {
//					for (int k = 0; k < K; k++) {
//						System.out.println(batchAssignedFromSameBoutique_1[i][k]);
//					}
//					System.out.println();
//				}
//				System.out.println();
//
//				// batchAssignedFromSameBoutique_2 constraints
//				System.out.println(" --- batchAssignedFromSameBoutique_2 --- ");
//				System.out.println();
//				for (int i = 0; i < I; i++) {
//					for (int k = 0; k < K; k++) {
//						System.out.println(batchAssignedFromSameBoutique_2[i][k]);
//					}
//					System.out.println();
//				}
//				System.out.println();
//
//				// T_Either_Or_1 constraints
//				System.out.println(" --- T_Either_Or_1 --- ");
//				System.out.println();
//				for (int i = 0; i < I; i++) {
//					System.out.println(T_Either_Or_1[i]);
//				}
//				System.out.println();
//
//				// T_Either_Or_2 constraints
//				System.out.println(" --- T_Either_Or_2 --- ");
//				System.out.println();
//				for (int i = 0; i < I; i++) {
//					System.out.println(T_Either_Or_2[i]);
//				}
//				System.out.println();

				System.out.println();
				System.out.println();

				// print the variables and store variable solution values into required integer
				// formats instead of double solution values from the model. Comment in printing
				// command lines when wanting to see model output values

				System.out.println(" ----- Variables -----");
				System.out.println();
				System.out.println(" --- Assignments in stage " + stage + " --- ");

				// print X_i,j,k values and convert solution values into true integers (only 0-1
				// in this case)
				// XBool array prevents holding unnecessary values such as 1.0000024 or
				// 0.00000024 by forcing them into 1 and 0, respectively.
				System.out.println(" X_i,j,k : Boutique i's batch j assigned to chute k ");
				System.out.println();
				for (int i = 0; i < I; i++) {
					for (int j = 0; j < numberOfItems.get(i).length; j++) {
						for (int k = 0; k < K; k++) {
							XBool[i][j][k] = (int) model.getValue(X[i][j][k]);

							if (XBool[i][j][k] == 1) { // comment in for printing only assigned values of X variables
								System.out.print(X[i][j][k]);
								System.out.println();
//								System.out.print(X[i][j][k] + " = ");
//								System.out.println(model.getValue(X[i][j][k]));
							}
						}
					}
					System.out.println();
				}

				System.out.println();

				// print Y_k values and convert solution values into true integers
				for (int k = 0; k < K; k++) {
					YInt[k] = (int) model.getValue(Y[k]);
//					System.out.print(Y[k] + " = ");
//					System.out.println(YInt[k]);
				}

				System.out.println();

				System.out.println(" Z_k : Chute k has received single assignment if Z_k = 1 ");
				// print Z_k values
				// ZBool array prevents holding unnecessary values such as 1.0000024 or
				// 0.00000024 by forcing them into 1 and 0, respectively.
				for (int k = 0; k < K; k++) {
					ZBool[k] = (int) model.getValue(Z[k]);
					System.out.print(Z[k] + " = ");
					System.out.println(ZBool[k]);
				}

				System.out.println();
				// Print t_i values

				for (int i = 0; i < I; i++) {
					tInt[i] = (int) model.getValue(t[i]);
//					System.out.print(t[i] + " : ");
//					System.out.println(tInt[i]);
				}
				System.out.println();

			} else {
				System.out.println("Model Could NOT Be Solved!");
				System.exit(0);
			}

			//////////////////////

			// by this point a stage is completed
			// updating model inputs
			// batches that belong to used boutiques but have not been assigned as a single
			// assignment are redefined as a new boutique

			System.out.println("----Updating parameters----- ");
			System.out.println();

			// printing numberOfItems before update
			System.out.println("numberOfItems before update: ");
			for (int[] intarr : numberOfItems) {
				System.out.print("[ ");
				for (int j : intarr) {
					System.out.print(j);
					System.out.print(" , ");
				}
				System.out.println("] ");
			}
			System.out.println();

			// cloning numberOfItems in tempItems
			ArrayList<int[]> tempItems = new ArrayList<int[]>();
			for (int i = 0; i < I; i++) {
				tempItems.add(numberOfItems.get(i));
			}

			// clearing numberOfItems and numberOfBatches to be filled once again later on
			numberOfItems.clear();
			numberOfBatches.clear();

			// adding previously unused boutiques into numberOfItems
			for (int i = 0; i < I; i++) {
				if (tInt[i] == 0) { // this condition ensures that the i'th boutique was not used
					numberOfItems.add(tempItems.get(i));
				}
			}

			// print numberOfItems after inserting previously unused boutiques
			System.out.println();
			System.out.println("numberOfItems after inserting previously unused boutiques: ");
			for (int[] intarr : numberOfItems) {
				System.out.print("[ ");
				for (int i : intarr) {
					System.out.print(i);
					System.out.print(" , ");
				}
				System.out.println("] ");
			}
			System.out.println();

			// creating a new array list of integers to hold batches that will be defined as
			// a new boutique. The condition is that these batches were not assigned as
			// single assignment to the chutes.
			ArrayList<Integer> newBoutiqueList = new ArrayList<Integer>();

			for (int i = 0; i < I; i++) {
				if (tInt[i] == 1) { // this condition ensures that the i'th boutique was used
					for (int j = 0; j < tempItems.get(i).length; j++) {
						for (int k = 0; k < K; k++) {
							if (ZBool[k] == 0 && XBool[i][j][k] == 1) {
								// this condition ensures that boutique i's batch j was assigned to chute k
								// but was not a single assignment
								newBoutiqueList.add(tempItems.get(i)[j]);
							}
						}
					}
				}
			}

			// converting this array list of integers into an integer array to match its
			// type with numberOfItems
			int[] newBoutiqueArray = new int[newBoutiqueList.size()];
			for (int i = 0; i < newBoutiqueArray.length; i++) {
				newBoutiqueArray[i] = newBoutiqueList.get(i).intValue();
			}

			// the newly defined integer array is added to numberOfItems as a new boutique
			numberOfItems.add(newBoutiqueArray);

			// printing numberOfItems after complete update
			System.out.println();
			System.out.println(
					"numberOfItems after complete update after stage " + stage + " with new boutique definitions: ");
			for (int[] intarr : numberOfItems) {
				System.out.print("[ ");
				for (int i : intarr) {
					System.out.print(i);
					System.out.print(" , ");
				}
				System.out.println("] ");
			}
			System.out.println();

			// calculating the total sum of items to be processed as in the beginning to be
			// used as looping condition in the while loop
			sum = 0;
			for (int i = 0; i < numberOfItems.size(); i++) {
				for (int j = 0; j < numberOfItems.get(i).length; j++) {
					sum += numberOfItems.get(i)[j];
				}
			}

			// stage completed here after all required updates, while loop continues from
			// the top

		}

		// after exitting the while loop, allocation is completed
		System.out.println();
		System.out.println();

		// printing the total number of stages spent within the system
		System.out.println("Allocation of orders completed after " + stage + " stages!");

	}

	// this method changes csv file formats into a list containing triplets
	// (triplets written as separate class)
	// 0: boutiqueID - 1: batchID - 2: numOfItems
	public static List<Triplet> fromCSVtoTripletList(String pathName) throws IOException {

		String tempLine = "";
		List<Triplet> returnList = new ArrayList<Triplet>();
		BufferedReader br = new BufferedReader(new FileReader(pathName));

		// writing a readLine command and not using it to ignore first line of titles in
		// csv file
		String headerLine = br.readLine();

		// read through csv file
		while ((tempLine = br.readLine()) != null) {
			String[] eachLine = tempLine.split(",");

			// storing first value of boutiqueID as an integer, second value of batchID as a
			// String, third value of NumOfItems as an integer
			Triplet tempTriplet = new Triplet(Integer.parseInt(eachLine[0]), eachLine[1],
					Integer.parseInt(eachLine[2]));
			returnList.add(tempTriplet);
		}
		return returnList;
	}
}