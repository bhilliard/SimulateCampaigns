import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	private HashMap<Integer,ArrayList<Integer>> numDaysCampRun;
	private int[] numCampaignsPerDay;

	//These are calculated after simulator runs
	private double averageNumCampaigns;
	private HashMap<Integer,int[][]> dailyAdjacencies;
	private HashMap<Integer,int[][]> dailyFlows;
	private HashMap<Integer,int[][]> unusedFlows;
	private HashMap<Integer,ArrayList<String>> nodes;
	private ArrayList<String> multiDayNodes;
	private int[][] multiDayAdjacencyMatrix;
	private int[][] multiDayFlow;
	private int[][] multiDayUnusedFlow;

	int resNumber;


	public GameResult(HashMap<Integer,ArrayList<String>> marketsHeld, 
			HashMap<Integer,ArrayList<Integer>> percentMarket,HashMap<Integer,ArrayList<Integer>> numDaysCampRun, int[] numCampaignsPerDay,
			int resNumber){
		this.resNumber = resNumber;
		this.marketsHeld = marketsHeld;
		this.percentMarket = percentMarket;
		this.numDaysCampRun = numDaysCampRun;
		this.numCampaignsPerDay = numCampaignsPerDay;

		dailyAdjacencies = new HashMap<Integer,int[][]>();
		dailyFlows = new HashMap<Integer,int[][]>();
		unusedFlows = new HashMap<Integer,int[][]>();
		nodes = new HashMap<Integer,ArrayList<String>>();

		this.averageNumCampaigns = calculateAvgNumCampaigns();

	}

	public void printGameResult(String baseDirectory){
		/*to print
		 *
		 * HashMap<Integer,ArrayList<String>> nodes;
		 * private HashMap<Integer,int[][]> dailyAdjacencies;
		 * private HashMap<Integer,int[][]> dailyFlows;
		 *
		 * ArrayList<String> multiDayNodes;
		 * int[][] multiDayAdjacencyMatrix;
		 * int[][] multiDayFlow;
		 *
		 */
		baseDirectory = baseDirectory+"/"+resNumber;
		
		File file = new File(baseDirectory);
		
		boolean success = file.mkdirs();
		//System.out.println("Printing? : "+success+" BD: "+baseDirectory);
		//try {
			//FileWriter writer = new FileWriter(baseDirectory+"/simResults.csv");

			//make writer for base/ResultNum/simResults.csv
			//System.out.println(marketsHeld.size());
			for(int day = 0; day<marketsHeld.size();day++){
				//writer.append(day+", markets");
				for(String mkt : marketsHeld.get(day)){
					//writer.append(","+mkt);
				}
				//writer.append('\n');

				//writer.append(day+", percents");
				for(Integer perc : percentMarket.get(day)){
					//print
					//writer.append(","+perc);
				}
				//writer.append('\n');
				//print ln

				//writer.append(day+", length");
				for(Integer length : numDaysCampRun.get(day)){
					//print
					//writer.append(","+length);
				}
				//writer.append('\n');

				//System.out.println(nodes.size());
				//System.out.println(day);
				//System.out.println(marketsHeld.size());
				if(dailyAdjacencies.get(day)!=null && dailyFlows.get(day)!=null){
					//printNodeMatrix(dailyAdjacencies.get(day), nodes.get(day), baseDirectory+"adjacency_"+day+".csv");
					//printNodeMatrix(dailyFlows.get(day), nodes.get(day), baseDirectory+"flow_"+day+".csv");
				}

			}
			//writer.flush();
			//writer.close();
		//} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
		printNodeMatrix(multiDayAdjacencyMatrix, multiDayNodes, baseDirectory+"/multidayAdjacency.csv");
		printNodeMatrix(multiDayFlow, multiDayNodes, baseDirectory+"/multidayFlow.csv");


	}

	private void printNodeMatrix(int[][] matrix, ArrayList<String> matrixNodes, String fileName){
		//print top line
		try {
			FileWriter writer = new FileWriter(fileName);

			for(String name : matrixNodes){
				writer.append(","+name);
			}
			writer.append('\n');
			//print matrix
			for(int i =0;i<matrix.length;i++){
				writer.append(matrixNodes.get(i));
				for(int j =0;j<matrix.length;j++){
					writer.append(","+matrix[i][j]);
				}
				writer.append('\n');
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	/**
	 *  calculates this GameResults avg num of campaigns per day
	 * @return
	 */
	protected double calculateAvgNumCampaigns() {
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
	 * runs the max flow algorithm to determine the demand on user types 
	 * and the campaigns whose reach might not be met.
	 * @param day
	 */
	public void calculateMultiDayFlow(){

		MinCostMaxFlow maxFlow = new MinCostMaxFlow();

		//construct a cost matrix that is all 1s as required by max flow code
		int[][] cost = new int[multiDayAdjacencyMatrix.length][multiDayAdjacencyMatrix.length];
		for(int r = 0;r<cost.length;r++){
			Arrays.fill(cost[r], 1);
		}
		//run max flow algorithm
		multiDayFlow =  maxFlow.getMaxFlow(multiDayAdjacencyMatrix, cost, 0, multiDayAdjacencyMatrix.length-1);

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

	public void calcMultidayUnusedFlow() {
		int[][] unusedFlow = new int[multiDayAdjacencyMatrix.length][multiDayAdjacencyMatrix.length];
		//adj-flow >0 means excess demand
		for(int i=0;i<multiDayAdjacencyMatrix.length;i++){
			for(int j=0;j<multiDayAdjacencyMatrix.length;j++){
				unusedFlow[i][j] = multiDayAdjacencyMatrix[i][j]-multiDayFlow[i][j];
			}
		}
		multiDayUnusedFlow = unusedFlow;

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


	public void addMultiDayNodes(ArrayList<String> nodes) {
		multiDayNodes = nodes;

	}

	public ArrayList<String> getMultiDayNodes() {
		return multiDayNodes;

	}



	public void addMultiDayAdjacencyMatrix(int[][] graph) {
		multiDayAdjacencyMatrix = graph;

	}

	public int[][] getMultiDayAdjacencyMatrix() {
		return multiDayAdjacencyMatrix;

	}


	public HashMap<Integer, ArrayList<Integer>> getNumDaysCampRun() {

		return numDaysCampRun;
	}


	public int[][] getMultiDayFlow() {

		return multiDayFlow;
	}


	public int[][] getMultiDayUnsuedFlow() {
		return multiDayUnusedFlow;

	}

}
