import java.util.ArrayList;

/**
 * DemandAndCompetitivenessExperiment handles the application of
 * a simulator and the Max Flow algorithm in order to experiment with the 
 * competitiveness of the TAC game with different parameters.
 * 
 * @author betsy betsy@cs.brown.edu
 *
 */
public class DemandAndCompetitivenessExperiment {

	CampaignSimulator simulator;
	
	int numTrials;
	ArrayList<GameResult> results; //all results stored here.


	public DemandAndCompetitivenessExperiment(int[] lengths,String[]segments,String[] userTypes, int[] sizeUserTypes, 
			int[] percents, int numDays, int numAgents, int numTrials, long seed){
		this.numTrials = numTrials;
		simulator = new CampaignSimulator(lengths, segments, userTypes, sizeUserTypes, 
				percents, numDays, numAgents, seed);
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

	
	/**
	 * runs an experiment that consists of:
	 * 1) for every trial requested, simulating the creation of campaigns
	 * 2) calculates the average num of campaigns per day
	 * 
	 * Note: The GameResults objects can then be used to calculate the max flow which
	 * determines what markets are over demanded and which campaigns don't meet their reach goals.
	 * @return results an array list of GameResult objects that store the sim. results
	 */
	public ArrayList<GameResult> runExperiment(){
		//create a simulator for your parameters

		//simulate numTrials number of games and calc average num campaigns
		results = simulator.simulateMultipleGames(numTrials);
		double avgNum = simulator.getAverageNumCampaigns(results);

		System.out.println("Average Num Campaigns per Day: " +avgNum);

		return results;
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


	public static void main(String[] args){

		long seed = -1; //set to value to control randomness
		int numTrials = 20;
		//set the parameters you want
		int[] lengths = {3,5,10}; //3,5,10 current spec

		String[] segments = {"M","F","Y","O","H","L","MY","MO","ML","MH","FY","FO","FL","FH","YL","YH","OL","OH","MYH","MYL",
				"MOH","MOL","FYH","FYL","FOH","FOL"};
		String[] userTypes = {"MYH","MYL","MOH","MOL","FYH","FYL","FOH","FOL"};

		int[] sizeUserTypes = {100,200,300,400,500,600,700,800}; //get Tae's numbers for this
		int[] percents = {20,50,80}; //20,50,80 from TAC document
		int numDays = 60;
		int numAgents=8;

		//construct and run experiment
		DemandAndCompetitivenessExperiment experiment = new DemandAndCompetitivenessExperiment(lengths, segments, userTypes, sizeUserTypes, 
				percents, numDays, numAgents, numTrials, seed);

		ArrayList<GameResult> results = experiment.runExperiment();
		
		//run the max flow algorithm on every day of every trial
		experiment.calculateAllMaxFlows();

		
		/* This pulls out a specific day and trial and synthesizes/prints the max flow results
		 * 
		 *TODO: this part needs to be extended to create statistics of these results
		 */
		
		int trial = 10; //must be less than numTrials
		int day = 20; //must be less than num days
		
		results.get(trial).calcUnusedFlow(day);
		experiment.printUnusedFlow(day,results.get(trial));
		System.out.println("Unsed Impression Opportunities:"+experiment.calcUnusedImpressions(day,results.get(trial)));
		System.out.println("Unmet Campaigns Reach:"+experiment.calcUnmetReach(day,results.get(trial)));
		
	}

}
