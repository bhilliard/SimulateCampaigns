import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * DemandAndCompetitivenessExperiment handles the application of
 * a simulator and the Max Flow algorithm in order to experiment with the 
 * competitiveness of the TAC game with different parameters.
 * 
 * @author betsy betsy@cs.brown.edu
 *
 */
public class DemandAndCompetitivenessTrial {

	CampaignSimulator simulator;

	int numDays;
	int numUserTypes;
	int numSegments;
	ArrayList<GameResult> results; //all results stored here.

	double avgNumCampaigns;


	public DemandAndCompetitivenessTrial(int[] lengths,String[]segments,String[] userTypes, int[] sizeUserTypes, 
			int[] percents, int numDays, int numAgents, long seed){
		this.numDays = numDays;
		this.numUserTypes = sizeUserTypes.length;
		this.numSegments = segments.length;
		simulator = new CampaignSimulator(lengths, segments, userTypes, sizeUserTypes, 
				percents, numDays, numAgents, seed);
		results = new ArrayList<GameResult>();
	}


	/**
	 * calculates all MaxFlows for all days, for all trials
	 * @param results
	 */
	public void calculateAllMaxFlows(){
		constructAllAdjacencyMatrices(results);
		for(int t=0;t<results.size();t++){
			results.get(t).calculateAllDaysFlow();
		}
	}

	public void calculateAllMultidayMaxFlows() {
		constructAllMultiDayAdjacencyMatrices(results);
		for(int t=0;t<results.size();t++){
			results.get(t).calculateMultiDayFlow();
		}

	}

	private void calculateAllMultidayMaxFlows(ArrayList<GameResult> results) {
		constructAllMultiDayAdjacencyMatrices(results);
		for(int t=0;t<results.size();t++){
			results.get(t).calculateMultiDayFlow();
		}

	}

	public void calculateAllMultidayUnusedFlow() {
		for(GameResult result : results){
			result.calcMultidayUnusedFlow();
		}

	}

	/**
	 * calculates all MaxFlows for all days, for all trials
	 * @param results
	 */
	public void calculateAllMaxFlows(ArrayList<GameResult> subsetResults){
		constructAllAdjacencyMatrices(subsetResults);
		for(int t=0;t<subsetResults.size();t++){
			subsetResults.get(t).calculateAllDaysFlow();
		}
	}

	/**
	 * constructs all adjacency matrices for for all days, for all trials
	 * 
	 * Note: calls a method in the simulator because game simulation parameters are required
	 * @param trialsResults
	 */
	public void constructAllAdjacencyMatrices(ArrayList<GameResult> trialsResults) {
		for(GameResult result :trialsResults){
			simulator.constructAllAdjacencyMatricies(result);
		}
	}

	public void constructAllMultiDayAdjacencyMatrices(ArrayList<GameResult> trialsResults) {
		for(GameResult result :trialsResults){
			simulator.constructMultiDayAdjacencyMatrix(result);
		}
	}


	/**
	 * runs an experiment that consists of:
	 * 1) for every trial requested, simulating the creation of campaigns
	 * 2) calculates the average num of campaigns per day
	 * 
	 * Note: The GameResults objects can then be used to calculate the max flow which
	 * determines what markets are over demanded and which campaigns don't meet their reach goals.
	 * @param trial 
	 * @return results an array list of GameResult objects that store the sim. results
	 */
	public GameResult runExperiment(int trial){
		//create a simulator for your parameters

		//simulate numTrials number of games and calc average num campaigns


		GameResult result = simulator.simulateGame(trial);
		results.add(result);

		avgNumCampaigns = result.calculateAvgNumCampaigns();

		return result;
	}


	/**
	 * prints a 2D array (or debugging purposes)
	 * @param matrix
	 */
	@SuppressWarnings("unused")
	private void print2DIntArray(int[][] matrix) {
		for(int i = 0; i<matrix.length;i++){
			for(int j = 0;j<matrix[0].length;j++){
				if(j>0){
					System.out.print(",");
				}
				System.out.print(matrix[i][j]);
			}
			System.out.println();
		}
		System.out.println();
		System.out.println();
	}


	/**
	 * Sums the total number of "wasted" impressions.
	 * i.e. available from source but can't get to sink
	 * @param day
	 * @param result
	 * @return
	 */
	private int calcUnusedImpressions(int day, GameResult result) {
		int unused = 0;
		int[][] unusedFlow = result.getUnusedFlow(day);

		//sum along first row to get edges out of source
		for(int j = 0;j<unusedFlow[0].length;j++){
			if(unusedFlow[0][j]>0){
				unused+=unusedFlow[0][j];
			}
		}
		return unused;
	}


	/**
	 * Sums the total un-met reach that campaigns demanded but didn't receive.
	 * i.e. potentially available along campaign node to sink edge, but not received at campaign node.
	 * @param day
	 * @param result
	 * @return
	 */
	private int calcUnmetReach(int day, GameResult result) {
		int unmet = 0;
		int[][] unusedFlow = result.getUnusedFlow(day);

		//sum along last col. to get edges into sink
		for(int i = 0; i<unusedFlow.length;i++){
			if(unusedFlow[i][unusedFlow[0].length-1]>0){
				unmet+=unusedFlow[i][unusedFlow[0].length-1];
			}
		}
		return unmet;
	}


	/**
	 * prints the unused flow from source to userType and camp. to sink
	 * Includes the user type or segment in question
	 * @param day
	 * @param result
	 */
	private void printUnusedFlow(int day, GameResult result) {
		int[][] unusedFlow = result.getUnusedFlow(day);
		System.out.println();
		System.out.println("User Types  Not Shown Ads:");

		//sum along first row to get edges out of source
		for(int j = 0;j<unusedFlow[0].length;j++){
			if(unusedFlow[0][j]>0){
				System.out.println(result.getNodes(day).get(j)+": "+unusedFlow[0][j]);
			}
		}
		System.out.println();
		System.out.println();
		System.out.println("Campaigns Wanting Impressions:");

		//sum along last col. to get edges into sink
		for(int i = 0; i<unusedFlow.length;i++){
			if(unusedFlow[i][unusedFlow[0].length-1]>0){
				System.out.println(result.getNodes(day).get(i)+": "+unusedFlow[i][unusedFlow[0].length-1]);
			}
		}
		System.out.println();

	}

	private ArrayList<String> getTargetSegmentsUnder(int day, GameResult result) {
		int[][] unusedFlow = result.getUnusedFlow(day);
		ArrayList<String> targetSegsUnder = new ArrayList<String>();

		//sum along last col. to get edges into sink
		for(int i = 0; i<unusedFlow.length;i++){
			if(unusedFlow[i][unusedFlow[0].length-1]>0){
				targetSegsUnder.add(result.getNodes(day).get(i));
			}
		}

		return targetSegsUnder;

	}

	private ArrayList<String> getUserTypesUnder(int day, GameResult result) {
		ArrayList<String> userTypesUnder = new ArrayList<String>();
		int[][] unusedFlow = result.getUnusedFlow(day);

		//sum along first row to get edges out of source
		for(int j = 0;j<unusedFlow[0].length;j++){
			if(unusedFlow[0][j]>0){
				userTypesUnder.add(result.getNodes(day).get(j));
			}
		}

		return userTypesUnder;

	}

	public void runExperimentsPrintResults(){



	}


}
