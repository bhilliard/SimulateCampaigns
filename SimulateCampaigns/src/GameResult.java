import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * GameResult is a data class that holds all computed results for a game.
 * Some results are simulated and others are calculated using Max Flow 
 * to determine the competitiveness of the game.
 * 
 * @author betsy betsy@cs.brown.edu
 *
 */

public class GameResult {

	//These come from the simulator
	private HashMap<Integer,ArrayList<String>> marketsHeld;
	private HashMap<Integer,ArrayList<Integer>> percentMarket;
	private int[] numCampaignsPerDay;

	//These are calculated after simulator runs
	private double averageNumCampaigns;
	private HashMap<Integer,int[][]> dailyAdjacencies;
	private HashMap<Integer,int[][]> dailyFlows;
	private HashMap<Integer,int[][]> unusedFlows;
	HashMap<Integer,ArrayList<String>> nodes;

	public GameResult(HashMap<Integer,ArrayList<String>> marketsHeld, 
			HashMap<Integer,ArrayList<Integer>> percentMarket, int[] numCampaignsPerDay){

		this.marketsHeld = marketsHeld;
		this.percentMarket = percentMarket;
		this.numCampaignsPerDay = numCampaignsPerDay;

		dailyAdjacencies = new HashMap<Integer,int[][]>();
		dailyFlows = new HashMap<Integer,int[][]>();
		unusedFlows = new HashMap<Integer,int[][]>();
		nodes = new HashMap<Integer,ArrayList<String>>();

		this.averageNumCampaigns = calculateAvgNumCampaigns();

	}


	/**
	 *  calculates this GameResults avg num of campaigns per day
	 * @return
	 */
	private double calculateAvgNumCampaigns() {
		double sum = 0.0;
		for(int i=0;i<numCampaignsPerDay.length;i++){
			sum=sum+numCampaignsPerDay[i];

		}
		return sum/(double)numCampaignsPerDay.length;
	}


	/**
	 * runs the max flow algorithm to determine the demand on user types 
	 * and the campaigns whose reach might not be met.
	 * @param day
	 */
	public void calculateDaysFlow(int day){

		MinCostMaxFlow maxFlow = new MinCostMaxFlow();
		
		//construct a cost matrix that is all 1s as required by max flow code
		int[][] cost = new int[dailyAdjacencies.get(day).length][dailyAdjacencies.get(day).length];
		for(int r = 0;r<cost.length;r++){
			Arrays.fill(cost[r], 1);
		}
		//run max flow algorithm
		dailyFlows.put(day, maxFlow.getMaxFlow(dailyAdjacencies.get(day), cost, 0, dailyAdjacencies.get(day).length-1));

	}

	/**
	 * prints the resulting flow matrix for a day
	 * @param day
	 */
	@SuppressWarnings("unused")
	private void printDailyFlow(int day){
		System.out.println("Day: "+day);
		for(int i=0;i<dailyAdjacencies.get(day).length;i++){
			for(int j=0;j<dailyAdjacencies.get(day).length;j++){
				if(j>0){
					System.out.print(",");
				}
				System.out.print(dailyFlows.get(day)[i][j]);
			}
			System.out.println();
			System.out.println();
		}
	}

	
	/**
	 * subtract the flow from the adjacency matrix to find where the excess capacity is in the graph.
	 * The excess capacity corresponds to unused impressions when coming out of the source and un-met
	 * reach when coming into the sink. Links between user types and campaigns don't have semantic meaning.
	 * 
	 * Note: to get the number of imps flowing between user type and campaign, look at the flow matrix not
	 * the unused flow matrix.
	 * @param day
	 */
	public void calcUnusedFlow(int day){
		int[][] unusedFlow = new int[dailyAdjacencies.get(day).length][dailyAdjacencies.get(day).length];
		//adj-flow >0 means excess demand
		for(int i=0;i<dailyAdjacencies.get(day).length;i++){
			for(int j=0;j<dailyAdjacencies.get(day).length;j++){
				unusedFlow[i][j] = dailyAdjacencies.get(day)[i][j]-dailyFlows.get(day)[i][j];
			}
		}
		unusedFlows.put(day,unusedFlow);
	}

	/**
	 * calculates the flow matrix for every day
	 */
	public void calculateAllDaysFlow(){
		for(int d=0;d<dailyAdjacencies.size();d++){
			calculateDaysFlow(d);
		}
	}
	

	/*
	 * The following methods are getters and setters.
	 */
	public ArrayList<String> getNodes(int day) {
		return nodes.get(day);
	}

	public void addNodes(int day, ArrayList<String>dayNodes){
		nodes.put(day,dayNodes);
	}

	public void addAdjacencyMatrix(int day, int[][] matrix){
		dailyAdjacencies.put(day, matrix);
	}

	public void addFlowMatrix(int day, int[][] matrix){
		dailyFlows.put(day, matrix);
	}

	public double getAverageNumCampaigns() {

		return averageNumCampaigns;
	}

	public HashMap<Integer, ArrayList<String>> getMarketsHeld() {
		return marketsHeld;
	}

	public HashMap<Integer, ArrayList<Integer>> getPercentMarket() {
		return percentMarket;
	}

	public int[][] getDaysAdjacencyMatrix(int day) { 
		return dailyAdjacencies.get(day);
	}

	public int[][] getUnusedFlow(int day) {
		return unusedFlows.get(day);
	}


}
