import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Campaign Simulator lets the user set different parameters that affect the 
 * competitiveness of the TAC AdX campaign/impressions market. It can be used 
 * to evaluate different parameter settings. It's output can be used in the 
 * MaxFlow algorithm (in MinCostMaxFlow) to determine what markets 
 * will be in high or low demand.
 * 
 * @author betsy betsy@cs.brown.edu
 *
 */

public class CampaignSimulator {

	
	Random rand;
	long seed;

	//game parameters
	int[] lengths; //campaign lengths3,5,10 current spec
	int[] reachPercents; //the percent of the market a campaign might want to reach
	int[] sizeUserTypes; //the size of each user type

	//basic game parameters
	int numDays;
	int numAgents;

	//game definitions
	String[] segments; //all markets that a campaign might require (F, MO, FYH, etc.)
	String[] userTypes; //all user types (FOL, MYH, etc.)
	

	public CampaignSimulator(int[] lengths,String[]segments,String[] userTypes, int[] sizeUserTypes, 
			int[] percents, int numDays, int numAgents, long seed){
		this.seed = seed;
		this.lengths = lengths;
		this.segments = segments;
		this.userTypes = userTypes;
		this.sizeUserTypes = sizeUserTypes;
		this.reachPercents = percents;
		this.numDays = numDays;
		this.numAgents = numAgents;
		
		if(seed==-1){
			rand = new Random();
		}else{
			rand = new Random(seed);
		}

	}

	
	/**
	 * simulates numTrials number of games and calculates the average
	 * number of campaigns per day
	 * 
	 * @param numTrials
	 */
	public ArrayList<GameResult> simulateMultipleGames(int numTrials){

		ArrayList<GameResult> results = new ArrayList<GameResult>();

		for(int t=1;t<=numTrials;t++){
			results.add(simulateGame());
		}
	
		return results;

	}

	
	/**
	 * Calculates the average number of campaigns per day over a set of 
	 * numGame number GameResults
	 * 
	 * @param results
	 * @return
	 */
	public double getAverageNumCampaigns(ArrayList<GameResult> results){

		double total=0.0;
		for(int t=0;t<results.size();t++){
			total=total+results.get(t).getAverageNumCampaigns();
		}
		return total/(double)results.size();
	}

	
	/**
	 * simulates a single numDays number of days and returns the average
	 * number of campaigns per day
	 * 
	 * @return
	 */
	public GameResult simulateGame(){

		int[] numCampaignsPerDay = new int[numDays];

		HashMap<Integer,ArrayList<String>> segmentsHeld = new HashMap<Integer,ArrayList<String>>();
		HashMap<Integer,ArrayList<Integer>> percentSegment = new HashMap<Integer,ArrayList<Integer>>();

		//simulate the campaigns that are initially passed to the agents
		simulateFirstCampaigns(segmentsHeld, percentSegment, numCampaignsPerDay);

		//add a campaign for every day
		for(int d=0;d<=numDays;d++){
			addCampaign(d,segmentsHeld, percentSegment, numCampaignsPerDay);
		}

		//store and return the sim. results
		GameResult result = new GameResult(segmentsHeld, percentSegment, numCampaignsPerDay);
		return result;
	}

	
	/**
	 * Constructs an adjacency matrix for every day
	 * 
	 * Note: adjacency matrices must be created in the Campaign Simulator because they 
	 * use game parameters.
	 * 
	 * @param result
	 */
	public void constructAllAdjacencyMatricies(GameResult result){
		for(int d = 0;d<numDays;d++){
			constructDayAdjacencyMatrix(d, result);
		}
	}
	
	
	/**
	 * Constructs and adjacency matrix for a given day of a game simulation result
	 * @param day
	 * @param result
	 */
	private void constructDayAdjacencyMatrix(int day, GameResult result){

		HashMap<Integer,ArrayList<String>> segmentsHeld = result.getMarketsHeld(); 
		HashMap<Integer,ArrayList<Integer>> percentSegment = result.getPercentMarket();
		
		//calc number of nodes in the graph
		int numNodes = userTypes.length+segmentsHeld.get(day).size()+2;
		
		//source,markets,segmentsHeld,sink
		int[][] graph = new int[numNodes][numNodes];
		ArrayList<String> nodes = new ArrayList<String>();
		
		//add the source
		nodes.add("S");
		//add all user types to the nodes
		for(int i = 0; i<userTypes.length;i++){
			nodes.add(userTypes[i]);
		}
		
		//add all the active campaign segments for the day
		nodes.addAll(segmentsHeld.get(day));
		
		//add sink and add to GameResult data
		nodes.add("T");
		result.addNodes(day, nodes);

		for(int i = 0;i<graph.length;i++){
			for(int j = 0;j<graph[0].length;j++){
				//source to Type
				if(i==0 && j>0 && j<=userTypes.length){
					graph[i][j]=(int)Math.floor(sizeUserTypes[j-1]*1.423);
				}
				//type to camp
				else if(i>0 && i<=userTypes.length && j>userTypes.length && j<graph.length-1){
					if(isMatch(userTypes[i-1],segmentsHeld.get(day).get(j-userTypes.length-1))){
						graph[i][j]=Integer.MAX_VALUE; //allow as many imps as available.
					}
				//camp to sink
				}else if(j==graph.length-1 && i>userTypes.length && i<graph.length-1){ 
					//calculate the size of the campaign
					double percent = (double)percentSegment.get(day).get(i-userTypes.length-1)/100.00;
					int sizeTarget = getSizeTargetSegment(segmentsHeld.get(day).get(i-userTypes.length-1));
					graph[i][j]=(int)(percent*sizeTarget);
				}
			}
		}
		
		result.addAdjacencyMatrix(day, graph);
	}


	/**
	 * Returns the sum of the sizes of all user types that match a
	 *  campaign segment campSeg
	 * @param campSeg
	 * @return
	 */
	private int getSizeTargetSegment(String mktSeg) {
		int total = 0;
		for(int ut = 0; ut<userTypes.length;ut++){
			if(isMatch(userTypes[ut], mktSeg)){
				total+=sizeUserTypes[ut];
			}
		}
		return total;
	}

	/**
	 * Returns true if a campaign's market segment( mktSeg) matches a userType
	 * @param userType
	 * @param mktSeg
	 * @return
	 */
	private boolean isMatch(String userType, String mktSeg){

		for(char c : mktSeg.toCharArray()){
			boolean match = false;
			for(char m : userType.toCharArray()){
				if(m==c){
					match = true;
				}
			}
			/*
			 * if the market segment requires a user characteristic not in the user type,
			 * return false
			 */
			if(!match){
				return false;
			}
		}
		return true;
	}


	/**
	 * adds a campaign for a day
	 * @param day
	 * @param segmentsHeld
	 * @param percentSegment
	 * @param numCampaignsPerDay
	 */
	public void addCampaign(int day,HashMap<Integer,ArrayList<String>> segmentsHeld, 
			HashMap<Integer,ArrayList<Integer>> percentSegment, int[] numCampaignsPerDay){

		//pick length, market and percent uniformly
		int length = rand.nextInt(lengths.length);
		int market = rand.nextInt(segments.length);
		int percent = rand.nextInt(reachPercents.length);

		//for every day the campaign is running, add to the count, segment and percent
		for(int c=day+2;c<(day+2+lengths[length]);c++){
			//handles first campaign on a day case
			if(!segmentsHeld.containsKey(c)){
				segmentsHeld.put(c, new ArrayList<String>());
				percentSegment.put(c, new ArrayList<Integer>());
			}
			if(c<numDays){
				numCampaignsPerDay[c]=numCampaignsPerDay[c]+1;
				segmentsHeld.get(c).add(segments[market]);
				percentSegment.get(c).add(reachPercents[percent]);
			}
		}
	}

	
	/**
	 * runs the slightly different process of adding the first day's campaigns
	 * @param segmentsHeld
	 * @param percentSegment
	 * @param numCampaignsPerDay
	 */
	public void simulateFirstCampaigns(HashMap<Integer,ArrayList<String>> segmentsHeld, 
			HashMap<Integer,ArrayList<Integer>> percentSegment, int[] numCampaignsPerDay){
		for(int a=1;a<=numAgents;a++){
			//pick  of campaign, market (F,MY,FYL..), percent goal uniformly at random
			int length = rand.nextInt(lengths.length);
			int market = rand.nextInt(segments.length);
			int percent = rand.nextInt(reachPercents.length);

			//for all days the campaign will occur...
			for(int j=0;j<lengths[length];j++){
				//if this is the first for a day, make new list
				if(!segmentsHeld.containsKey(j)){
					segmentsHeld.put(j, new ArrayList<String>());
					percentSegment.put(j, new ArrayList<Integer>());
				}
				//add the chosen market
				if(j<lengths[lengths.length-1]){
					numCampaignsPerDay[j]=numCampaignsPerDay[j]+1;
					segmentsHeld.get(j).add(segments[market]);
					percentSegment.get(j).add(reachPercents[percent]);
				}
			}
		}
	}
	
	
	
	public static void main(String[] args){

		long seed = 283280;
		//set the parameters you want
		int[] lengths = {3,5,10}; //3,5,10 current spec

		String[] segments = {"M","F","Y","O","H","L","MY","MO","ML","MH","FY","FO","FL","FH","YL","YH","OL","OH"," MYH","MYL",
				"MOH","MOL","FYH","FYL","FOH","FOL"};
		String[] userTypes = {" MYH","MYL","MOH","MOL","FYH","FYL","FOH","FOL"};

		int[] sizeUserTypes = {100,200,300,400,500,600,700,800}; //get Tae's numbers for this
		int[] percents = {20,50,80}; //20,50,80 from TAC document
		int numDays = 60;
		int numAgents=8;

		int numTrials = 1;

		//create a simulator for your parameters
		CampaignSimulator simulator = new CampaignSimulator(lengths, segments, userTypes, sizeUserTypes, 
				percents, numDays, numAgents, seed);

		//simulate numTrials number of games and calc average num campaigns
		ArrayList<GameResult> trialsResults = simulator.simulateMultipleGames(numTrials);
		double avgNum = simulator.getAverageNumCampaigns(trialsResults);
		System.out.println("Average Num Campaigns per Day:" +avgNum);

	}
	
}
