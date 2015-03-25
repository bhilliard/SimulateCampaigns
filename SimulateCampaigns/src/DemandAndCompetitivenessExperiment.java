import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * 
 */

/**
 * This class handles collecting and printing the results of an experiment which consists of a certain number
 * of trials under a certain set of parameters. 
 * 
 * @author betsy
 *
 */
public class DemandAndCompetitivenessExperiment {

	String baseFolder = "/Users/betsy/TACOutput/";

	boolean calcDailyFlows = true;


	//these could be read from a file?
	int numDays = 60;
	int numAgents=8;
	String[] segments = {"M","F","Y","O","H","L","MY","MO","ML","MH","FY","FO","FL","FH","YL","YH","OL","OH","MYH","MYL",
			"MOH","MOL","FYH","FYL","FOH","FOL"};

	String[] userTypes = {"MYH","MYL","MOH","MOL","FYH","FYL","FOH","FOL"};
	int[] sizeUserTypes = {517, 1836, 808, 1795, 256, 1980, 407, 2401};


	//set the parameters you want
	//int[] lengths = {3,5,10}; //3,5,10 current spec //try 7,10,14
	//int[] percents = {20,50,80}; //20,50,80 from TAC document try 30 not 20;

	//these parameters are set by the constructor
	long seed = -1; //set to value to control randomness
	Random random;
	int numTrials = 1;
	String experimentName = "Unset";
	String experParamName = "Unset";


	//these stats should be reset before running a new experiment
	double avgNumCampaignsPerDay = 0.0;
	int numUnderMet = 0;
	double totalPercentUnmet = 0.0;
	int totalShort = 0;
	int totalDemanded = 0;


	public DemandAndCompetitivenessExperiment(String name, String baseFolder, int numTrials, long seed, boolean calcDailyFlows){

		this.experimentName = name;
		this.baseFolder = baseFolder;
		this.numTrials = numTrials;
		this.seed = seed;
		if(seed!=-1){
			random = new Random(seed);
		}else{
			random = new Random();
		}
		this.calcDailyFlows = calcDailyFlows;

	}

	/**
	 * collectAndOutputStats adds the results recorded in a 
	 * GameResult to the experiment's stats
	 * 
	 * @param result
	 */
	public void collectAndOutputStats(GameResult result){
		//prints this result's values and matrices
		result.printGameResult(baseFolder+experParamName);
		avgNumCampaignsPerDay+=result.getAverageNumCampaigns();
		int[] numCampShort = new int[numDays];
		int[] sizeOfCamp = new int[numDays];
		//itterate through the calculated unused flow matrices and add values to stats
		for(int a=0;a<result.getMultiDayUnsuedFlow().length;a++){
			for(int b=0;b<result.getMultiDayUnsuedFlow()[0].length;b++){
				//if going into the sink, add total available weight to total Demanded
				if(b==result.getMultiDayUnsuedFlow()[0].length-1){
					totalDemanded+=result.getMultiDayAdjacencyMatrix()[a][b];
				}
				//if there is unused flow and it is going from the source or to the sink, add to stats
				if(result.getMultiDayUnsuedFlow()[a][b]>0 && 
						(a==0 || b==result.getMultiDayUnsuedFlow()[0].length-1)){
					//add source to user type stats here int he future??

					//System.out.println(result.getMultiDayNodes().get(a)+" to "+result.getMultiDayNodes().get(b)+": "+result.getMultiDayUnsuedFlow()[a][b]);
					//we are only carring about campaign to sink stats at the moment
					if(a!=0){
						numUnderMet+=1; //count number that didn't meet reach
						int day = Integer.parseInt(result.getMultiDayNodes().get(a).split("_")[1]);
						numCampShort[day]=result.getMultiDayUnsuedFlow()[a][b]; //get number under camp
						sizeOfCamp[day]=result.getMultiDayAdjacencyMatrix()[a][b]; //get number available
						totalPercentUnmet+=(double)numCampShort[day]/(double)sizeOfCamp[day]; //calc the percent unmet and add to total
						totalShort+=numCampShort[day]; //add to the total short
						//System.out.println("TPU: "+totalPercentUnmet);
					}
				}
			}
		}
	}

	/**
	 * runExperiment takes in the parameters for the 
	 * @param percents array with all percents options
	 * @param lengths array with all length options
	 */
	public void runExperiment(int[] percents,int[] lengths){



		experParamName = experimentName+"/";
		//construct filename
		if(numTrials>1){
			experParamName = experParamName+"Avg_"+numTrials;
		}else{
			experParamName = experParamName+"Single";
		}
		experParamName=experParamName+"_Lengths_"+lengths[0]+"_"+lengths[1]+"_"+lengths[2]+"_Percents_"+percents[0]+"_"+percents[1]+"_"+percents[2];

		//run numTrials trials
		for(int t = 1;t<=numTrials;t++){
			long newSeed = random.nextLong();
			//construct and run a trial
			DemandAndCompetitivenessTrial trial = new DemandAndCompetitivenessTrial(lengths, segments, userTypes, sizeUserTypes, 
					percents, numDays, numAgents,newSeed);

			//run trial t
			GameResult result = trial.runExperiment(t);

			//if requested, run the max flow algorithm on every day of every trial
			//these will only be printed if they are run
			if(calcDailyFlows){
				trial.calculateAllMaxFlows();
			}
			//construct and solve a large maxflow problem for the multiday problem
			trial.calculateAllMultidayMaxFlows();
			trial.calculateAllMultidayUnusedFlow();

			//add this trial's result to the stats and output files for it
			collectAndOutputStats(result);
		}


	}

	/**
	 * outputOverallResults
	 */
	public void outputOverallResults(FileWriter overallWriter){
		String overallRes = baseFolder+experParamName+"/finalResults.csv";
		System.out.println("overall Res file: "+ overallRes);

		try {
			FileWriter writer = new FileWriter(overallRes);

			double avgNumCamp =avgNumCampaignsPerDay/(double)numTrials;
			double avgUnableMeet = (double)numUnderMet/(double)numTrials;
			double avgPercUnmet = (double)totalPercentUnmet/(double)numUnderMet;
			double percExperienceComp = (double)totalShort/(double)totalDemanded;

			System.out.println("Average NumCampaigns Per Day: "+avgNumCamp);
			System.out.println("Average Num Unable to Meet Demand per game: "+avgUnableMeet);
			System.out.println("Average Percent of a Campaign Unmet: "+avgPercUnmet);
			writer.append(avgNumCamp+","+avgUnableMeet+","+avgPercUnmet+"\n");
			System.out.println("perc: "+percExperienceComp+" Total Short: "+totalShort+" total demanded: "+totalDemanded);
			overallWriter.write(experParamName+","+avgNumCamp+","+avgUnableMeet+","+avgPercUnmet+"\n");
			//TODO set these from main
			if(avgUnableMeet>20 && avgPercUnmet<.40 && percExperienceComp>=.20){
				System.out.println("____________________________________________________");
			}

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * resets the statistics values for another experiment
	 */
	private void resetStats() {
		avgNumCampaignsPerDay = 0.0;
		numUnderMet = 0;
		totalPercentUnmet = 0.0;
		totalShort = 0;
		totalDemanded = 0;

	}

	public static void main(String[] args){

		/*
		 * Set values and parameters
		 */
		
		//set a name for your set of experiments *******************
		String experimentName = "LargeSearch";
		
		//designate a folder where the files should be output to *******************
		String baseFolder = "/Users/betsy/TACOutput/";
		
		//set to true if you want to calculate and store the daily max flow problems as well *******************
		//this will slow things down. I also don't have any stats collected for these at the moment.
		boolean calcDailyFlows = false;

		//set a number of trials per experimental settings *******************
		int numTrials = 10;

		//-1 means no seed will be set, set a seed to run the same experiment repeatedly *******************
		long seed = -1;

		/*
		 * these should be set to control the space of parameters over which the 
		 * we will search for better parameters
		 * 
		 * Set all of these to search over a space. If you set min==max then it will only run that param value *******************
		 */
		//length parameters (inclusive)
		int minl1=3;
		int maxl1=8;
		int minl2=6;
		int maxl2=10;
		int minl3=10;
		int maxl3=14;

		int lInc = 2; //increments through the search space by this value *******************

		//percent parameters (inclusive)
		int minp1=20;
		int maxp1=45;
		int minp2=30;
		int maxp2=60;
		int minp3=40;
		int maxp3=80;

		int pInc = 5;//increments through the search space by this value *******************

		int[]percents = new int[3];
		int[]lengths = new int[3];
		
		DemandAndCompetitivenessExperiment experiment = new DemandAndCompetitivenessExperiment(experimentName, baseFolder, numTrials,seed, calcDailyFlows);

		FileWriter overallWriter;
		try {

			File file = new File(baseFolder+experimentName);
			file.mkdirs();
			overallWriter = new FileWriter(baseFolder+experimentName+"/summaryRes.csv");

			for(int l1=minl1;l1<=maxl1;l1+=lInc){
				for(int l2=minl2;l2<=maxl2;l2+=lInc){
					for(int l3=minl3;l3<=maxl3;l3+=lInc){

						for(int p1=minp1;p1<=maxp1;p1+=pInc){
							for(int p2=minp2;p2<=maxp2;p2+=pInc){
								for(int p3=minp3;p3<=maxp3;p3+=pInc){

									System.out.println(l1+" "+l2+" "+l3+" "+p1+" "+p2+" "+p3);
									percents[0] = p1;
									percents[1] = p2;
									percents[2] = p3;
									lengths[0] = l1;
									lengths[1] = l2;
									lengths[2] = l3;

									experiment.resetStats();
									experiment.runExperiment(percents, lengths);
									experiment.outputOverallResults(overallWriter);

								}
							}
						}
					}
				}
			}
			overallWriter.flush();
			overallWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
