package masters.agents.bayesian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.lang.Math;

import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.Value;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.timeline.Timeline;

/**
 * BayesianAgent uses the bayesian rule to predict the preference of the opponent.
 * This prediction is in turn used to create a counter-offer. An accepting strategy is required
 * to be able to create a counter-offer. The accepting strategy of NormalAccepter is used for this.
 *
 * @author MASters
 */
public class NormalBayesianAgent extends AbstractAgent {

        double bestUtility, worstUtility;
        Bid optimalBid;
        double optimalUtility;

        Action lastPartnerAction;
        Bid lastPartnerBid;
        double lastPartnerUtility;
        ArrayList<Double> history;
        
        int MINIMUM_HISTORY_LENGTH = 10;
        int MAXIMUM_HISTORY_SIZE = 25;
        
        int MAX_SAMPLE_SIZE = 10000;
        int SAMPLE_REPEATS = 25;
        Random random;      
  
	/**
	 * init is called when a next session starts with the same opponent.
	 */
	@Override
    public void init() {
        try {
            optimalBid = utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        optimalUtility = getUtility(optimalBid);
        history = new ArrayList<Double>();
        random = new Random();
    }
	/**
	 * Version of the agent
	 */
	@Override
	public String getVersion() {
		return "1.0 (Genius v7.1.8)";
	}
	
	/**
	 * Name of the agent
	 */
	@Override
	public String getName() {
		return "Normal Bayesian Agent";
	}
	
	/**
	 * Receive the action of the opponent in the previous turn.
	 */
	@Override
	public void ReceiveMessage(Action opponentAction) {		
		actionOfOpponent = opponentAction;
		if (actionOfOpponent instanceof Offer) {
			println("Received Offer");
			lastBidOpponent = ((Offer) actionOfOpponent).getBid();
			predictor.updateModel(lastBidOpponent);
		}
	     lastPartnerUtility = getUtility(lastPartnerBid);
	        history.add(lastPartnerUtility);
	        if (history.size() > MAXIMUM_HISTORY_SIZE)
	            history.remove(0);
	    }

	
	/**
	 * Choose an action given the time and previous offers of the opponent.
	 * 
	 * @return the chosen action
	 */
	@Override
	public Action chooseAction() {
		Action action = null;
		
		try {
			double time = timeline.getTime();
			
			if (actionOfOpponent == null) {
				// Initial offer will be an optimal bid.
				action = (new Offer(getAgentID(), optimalBid));
			} else if (actionOfOpponent instanceof Offer) {
				if (time < 1 && time > 0.98) {
					// If the last turn is reached accept the offer.
					action = new Accept(getAgentID(), lastBidOpponent);				
				} else {
					// Calculate offer using opponents preference.
					Map<Issue, Double> preferenceOpponent = predictor.getPreferenceWeights();
					printPreference(preferenceOpponent);
					
					Bid counterOffer = getBid(preferenceOpponent);
					action = (new Offer(getAgentID(), counterOffer));
				}
			}
		} catch (Exception e) {
			if (lastBidOpponent != null) {
				action = new Accept(getAgentID(), lastBidOpponent);
			} else {
				action = new EndNegotiation(getAgentID());
			}
			e.printStackTrace();
		}
		return action;
	}
	private Bid getBid(Map<Issue, Double> preferenceOpponent) {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * Create a Bid using the predicted preference of the opponent.
	 * This is achieved using the Counter-offer proposition of Zhang.
	 * This proposition uses a weight ratio to determine the issues that increase the agents utility
	 * the most while minimizing the reduction of the opponents utility.
	 *  
	 * @param preference
	 * 			The preference of the opponent.
	 * @return bid
	 */
	
		// TODO: Add target utility function to the agent.

    
	    
		 public double getExpectedMaximum() {
		        if (history.size() < MINIMUM_HISTORY_LENGTH)
		            return optimalUtility;

		        double mean = 0;
		        for (double doub : history) {
		            mean += doub;
		        }
		        mean /= history.size();

		        double sd = 0;
		        for (double doub : history) {
		            sd += Math.pow(doub - mean, 2);
		        }
		        sd /= history.size();
		        sd = Math.pow(sd, 0.5);

		        double maximum = 0;

		        double time = timeline.getTime();
		        int sample_size = (int) (history.size() * (1 - time) / time);
		        if (sample_size > MAX_SAMPLE_SIZE)
		            sample_size = MAX_SAMPLE_SIZE;
		        if (sample_size < 1)
		            sample_size = 1;

		        for (int i = 0; i < SAMPLE_REPEATS; i++) {
		            double tempMaximum = 0;
		            for (int j = 0; j < sample_size; j++) {
		                double example = random.nextGaussian() * sd + mean;
		                if (example > tempMaximum)
		                    tempMaximum = example;
		            }
		            maximum += tempMaximum / SAMPLE_REPEATS;
		        }

		        return maximum;
		    }	
	
	/**
	 * Calculate the weight ratios.
	 * 
	 * @param preference
	 * @return weightRatioMap
	 */
	private Map<Issue, Double> getWeightRatio(Map<Issue, Double> preference) {
		AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
		
		HashMap<Issue, Double> weightRatio = new HashMap<Issue, Double>();
		for (Issue issue : issues) {
			double ratio = ((AdditiveUtilitySpace) utilitySpace).getWeight(issue.getNumber()) / preference.get(issue);
			weightRatio.put(issue, ratio);
		}
		return weightRatio;
	}
	
	/**
	 * Return the Issue with the highest value.
	 * 
	 * @param map
	 * @return
	 */
	private Issue getMax(Map<Issue, Double> map) {
		Issue issue = null;
		double highest = 0.0;
		
		for (Map.Entry<Issue, Double> entry : map.entrySet()) {
			if (entry.getValue() > highest) {
				issue = entry.getKey();
				highest = entry.getValue();
			}
		}
		return issue;
	}
	
	/**
	 * Order the map on issues by their value.
	 * 
	 * @param map
	 * @return orderedList
	 * 			List of Issues ordered on their value in the given map.
	 */
	private List<Issue> orderIssues(Map<Issue, Double> map) {
		ArrayList<Issue> orderedList = new ArrayList<Issue>();
		Issue issue;
		while (!(map.size() == 0)) {
			issue = getMax(map);
			orderedList.add(issue);
			map.remove(issue);
		}
		return orderedList;
	}
	
	/**
	 * Calculate the utility of a bid.
	 * 
	 * @param bid
	 * @return utility
	 * @throws Exception
	 */
	public double calculateUtility(Bid bid) throws Exception {
		HashMap<Integer, Value> values = bid.getValues();
		
		double u = 0.0;
		double max = 0.0;
		
		for (Issue issue : issues) {
			double weight = ((AdditiveUtilitySpace) utilitySpace).getWeight(issue.getNumber());
			double normVal = getNormalizedValue(issue, values.get(issue.getNumber()));
			
			max += weight;
			u += weight * normVal;
		}
		
		return normalize(u, max, 0.0);
	}
}
