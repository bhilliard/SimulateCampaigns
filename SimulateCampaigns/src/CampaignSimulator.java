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
			results.add(simulateGame(t));
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
	public GameResult simulateGame(int gameNumber){

		int[] numCampaignsPerDay = new int[numDays];

		HashMap<Integer,ArrayList<String>> segmentsHeld = new HashMap<Integer,ArrayList<String>>();
		HashMap<Integer,ArrayList<Integer>> percentSegment = new HashMap<Integer,ArrayList<Integer>>();
		HashMap<Integer,ArrayList<Integer>> numDaysCampRun = new HashMap<Integer,ArrayList<Integer>>();
		//simulate the campaigns that are initially passed to the agents
		simulateFirstCampaigns(segmentsHeld, percentSegment, numDaysCampRun,numCampaignsPerDay);

		//add a campaign for every day
		for(int d=0;d<numDays;d++){
			addCampaign(d,segmentsHeld, percentSegment, numDaysCampRun,numCampaignsPerDay);
		}

		//store and return the sim. results
		GameResult result = new GameResult(segmentsHeld, percentSegment, numDaysCampRun,numCampaignsPerDay,gameNumber);
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
		HashMap<Integer, ArrayList<Integer>> numDaysCampRun = result.getNumDaysCampRun();

		//calc number of nodes in the graph
		int numNodes = userTypes.length+segmentsHeld.get(day).size()+2;

		//source,markets,segmentsHeld,sink
		int[][] graph = new int[numNodes][numNodes];
		ArrayList<String> nodes = new ArrayList<String>();

		//add the source
		nodes.add("S");
		//add all user types to the nodes
		for(int i = 0; i<userTypes.length;i++){
			nodes.add(userTypes[i]+"_"+day);
		}

		//add all the active campaign segments for the day
		for(int j=0;j<segmentsHeld.get(day).size();j++){
			nodes.add(segmentsHeld.get(day).get(j));
		}
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
	 * 
	 * TODO figure out this method
	 * Do I need to do a refactoring...can't just increment an array
	 * 
	 * @param result
	 */
	protected void constructMultiDayAdjacencyMatrix(GameResult result){

		HashMap<Integer,ArrayList<String>> segmentsHeld = result.getMarketsHeld(); 
		HashMap<Integer,ArrayList<Integer>> percentSegment = result.getPercentMarket();
		HashMap<Integer,ArrayList<Integer>> numDaysCampRun = result.getNumDaysCampRun();




		ArrayList<String> nodes = new ArrayList<String>();

		//add the source
		nodes.add("S");
		//add all user types to the nodes
		for(int d = 0;d<numDays;d++){
			for(int i = 0; i<userTypes.length;i++){
				nodes.add(userTypes[i]+"_"+d);
			}

		}
		for(int d =0;d<numDays;d++){
			for(String seg : segmentsHeld.get(d)){
				if(!nodes.contains(seg)){
					nodes.add(seg);
				}
			}
		}



		//add sink and add to GameResult data
		nodes.add("T");
		result.addMultiDayNodes(nodes);

		int[][] graph = new int[nodes.size()][nodes.size()];

		for(int n=0;n<nodes.size();n++){
			for(int m = 0;m<nodes.size();m++){
				//if usertype and campaign matches...
				if(isUserType(nodes.get(n)) && isCamp(nodes.get(m))){
					//System.out.println(nodes.get(n)+", "+nodes.get(m));
					//System.out.println("MATCH BOTH: "+isMatch(nodes.get(n),nodes.get(m))+", "+(getDay(nodes.get(n))>=getStartDay(nodes.get(m)))
					//+", "+(getDay(nodes.get(n))<=getEndDay(nodes.get(m))));
					//AND they are of overlapping types
					if(isMatch(nodes.get(n),nodes.get(m)) && getDay(nodes.get(n))>=getStartDay(nodes.get(m)) && getDay(nodes.get(n))<=getEndDay(nodes.get(m))){
						//System.out.println("ut to camp");
						graph[n][m] = Integer.MAX_VALUE;
					}
					//camp to sink
				}else if(isCamp(nodes.get(n)) && nodes.get(m).compareTo("T")==0){
					//sizeUT*( de-ds)*perc
					int campNum = 0;
					int campDay = 0;
					for(int c=0;c<segmentsHeld.size();c++){
						for(int sh=0;sh<segmentsHeld.get(c).size();sh++){
							if(segmentsHeld.get(c).get(sh).compareTo(nodes.get(n))==0){
								campNum = sh;
								campDay = c;
							}
						}
					}
					double percent = percentSegment.get(campDay).get(campNum)/100.00;
					int sizeTarget = (int) (getSizeTargetSegment(segmentsHeld.get(campDay).get(campNum))*1.423);
					int numDays = Integer.parseInt(nodes.get(n).split("_")[2])-Integer.parseInt(nodes.get(n).split("_")[1])+1;
					//System.out.println(nodes.get(n)+" perc: "+percent+" size: "+sizeTarget+" days: "+numDays);
					//System.out.println("camp to T: "+((int) (sizeTarget*(Integer.parseInt(nodes.get(n).split("_")[2])-Integer.parseInt(nodes.get(n).split("_")[1]))
					//*percent)));
					graph[n][m]= (int) (sizeTarget*numDays*percent);
					//source to userType
				}else if(nodes.get(n).compareTo("S")==0 && isUserType(nodes.get(m))){
					for(int ut =0;ut<userTypes.length;ut++){
						if(userTypes[ut].compareTo(nodes.get(m).split("_")[0])==0){
							//System.out.println("n: "+n);
							//System.out.println("m: "+m);
							//System.out.println("S to UT: "+(sizeUserTypes[ut]*1.423));
							graph[n][m]=(int) (sizeUserTypes[ut]*1.423);
						}

					}

				}
			}
		}



		result.addMultiDayAdjacencyMatrix(graph);

	}

	private boolean isCamp(String n) {
		String[] parts =n.split("_");
		if(parts.length ==3){
			return true;
		}
		return false;
	}


	private int getEndDay(String node) {
		String[] parts =node.split("_");

		return Integer.parseInt(parts[2]);
	}


	private int getStartDay(String node) {
		String[] parts =node.split("_");

		return Integer.parseInt(parts[1]);
	}


	private int getDay(String node) {
		String[] parts =node.split("_");
		return Integer.parseInt(parts[1]);
	}


	private boolean isUserType(String node) {
		String[] parts =node.split("_");
		if(parts.length==2){
			return true;
		}
		return false;
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
		String[] mktSegArray=mktSeg.split("_");
		//System.out.println("MS: "+mktSegArray[0]);
		//System.out.println("UT: "+userType);
		for(char c : mktSegArray[0].toCharArray()){
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
	 * @param numDaysCampRun 
	 * @param numCampaignsPerDay
	 */
	public void addCampaign(int day,HashMap<Integer,ArrayList<String>> segmentsHeld, 
			HashMap<Integer,ArrayList<Integer>> percentSegment, HashMap<Integer, ArrayList<Integer>> numDaysCampRun, int[] numCampaignsPerDay){

		//pick length, market and percent uniformly
		int length = rand.nextInt(lengths.length);
		int market = rand.nextInt(segments.length);
		int percent = rand.nextInt(reachPercents.length); 

		//for every day the campaign is running, add to the count, segment and percent
		for(int c=day+2;c<(day+2+lengths[length]);c++){
			//handles first campaign on a day case
			if(!segmentsHeld.containsKey(c) && c<numDays){
				segmentsHeld.put(c, new ArrayList<String>());
				percentSegment.put(c, new ArrayList<Integer>());
				numDaysCampRun.put(c, new ArrayList<Integer>());
			}
			if(c<numDays){
				numCampaignsPerDay[c]=numCampaignsPerDay[c]+1;
				numDaysCampRun.get(c).add(lengths[length]);
				segmentsHeld.get(c).add(segments[market]+"_"+(day+2)+"_"+(Math.min(day+2+lengths[length]-1,numDays-1)));
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
			HashMap<Integer,ArrayList<Integer>> percentSegment, HashMap<Integer,ArrayList<Integer>> numDaysCampRun, int[] numCampaignsPerDay){
		for(int a=1;a<=numAgents;a++){
			//pick  of campaign, market (F,MY,FYL..), percent goal uniformly at random
			int length = (int)Math.ceil((lengths.length-1)/2);
			int market = rand.nextInt(segments.length);
			String mkt = segments[market];
			int count = 1;
			while(mkt.toCharArray().length!=2 || count>=15){
				market = rand.nextInt(segments.length);
				mkt = segments[market];
				count++;
			}
			int percent = (int)Math.ceil((reachPercents.length-1)/2);

			//for all days the campaign will occur...
			for(int j=0;j<lengths[length];j++){
				//if this is the first for a day, make new list
				if(!segmentsHeld.containsKey(j)){
					segmentsHeld.put(j, new ArrayList<String>());
					percentSegment.put(j, new ArrayList<Integer>());
					numDaysCampRun.put(j, new ArrayList<Integer>());
				}
				//add the chosen market
				if(j<lengths[lengths.length-1]){
					numCampaignsPerDay[j]=numCampaignsPerDay[j]+1;
					segmentsHeld.get(j).add(segments[market]+"_0_"+(lengths[length]-1));
					numDaysCampRun.get(j).add(lengths[length]);
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
