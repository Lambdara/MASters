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

/**
 * BayesianAgent uses the bayesian rule to predict the preference of the opponent.
 * This prediction is in turn used to create a counter-offer. An accepting strategy is required
 * to be able to create a counter-offer. The accepting strategy of NormalAccepter is used for this.
 * 
 * @author Mees
 */
public class BayesianAgent extends AbstractAgent {
	
	/**
	 * init is called when a next session starts with the same opponent.
	 */
	@Override
	public void init() {
		println("Initializing Agent...");
		try {
			optimalBid = utilitySpace.getMaxUtilityBid();			
			predictor = new BayesianPredictor(utilitySpace.getDomain().getIssues(), agentEvaluationAim);
			issues = utilitySpace.getDomain().getIssues();
			agentEvaluationAim = getAgentEvaluationAim();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		return "Bayesian Agent";
	}
	
	/**
	 * Receive the action of the opponent in the previous turn.
	 */
	@Override
	public void ReceiveMessage(Action opponentAction) {
		println("Receiving opponents action...");
		
		actionOfOpponent = opponentAction;
		if (actionOfOpponent instanceof Offer) {
			println("Received Offer");
			lastBidOpponent = ((Offer) actionOfOpponent).getBid();
			predictor.updateModel(lastBidOpponent);
		}
	}
	
	/**
	 * Choose an action given the time and previous offers of the opponent.
	 * 
	 * @return the chosen action
	 */
	@Override
	public Action chooseAction() {
		println("Choosing action...");
		Action action = null;
		
		try {
			double time = timeline.getTime();
			
			if (actionOfOpponent == null) {
				println("First action of the negotiation...");
				// Initial offer will be an optimal bid.
				action = (new Offer(getAgentID(), optimalBid));
			} else if (actionOfOpponent instanceof Offer) {
				if (time < 1 && time > 0.98) {
					println("End of negotiation is reached...");
					// If the last turn is reached accept the offer.
					action = new Accept(getAgentID(), lastBidOpponent);				
				} else {
					println("Creating counter-offer...");
					// Calculate offer using opponents preference.
					Map<Issue, Double> preferenceOpponent = predictor.getPreferenceWeights();			
					Bid counterOffer = getBid(preferenceOpponent);
					action = (new Offer(getAgentID(), counterOffer));
				}
			}
		} catch (Exception e) {
			System.out.println("Exception in ChooseAction:" + e.getMessage());
			if (lastBidOpponent != null) {
				action = new Accept(getAgentID(), lastBidOpponent);
			} else {
				action = new EndNegotiation(getAgentID());
			}
			e.printStackTrace();
		}
		return action;
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
	private Bid getBid(Map<Issue, Double> preference) throws Exception {
		println("Creating bid...");
		Bid bid = new Bid(utilitySpace.getDomain(), lastBidOpponent.getValues());
		Map<Issue, Double> weightRatio = getWeightRatio(preference);
		List<Issue> rankedWeightRatio = orderIssues(weightRatio);		
		
		// TODO: Add target utility function to the agent.
		double targetUtility = 0.8;
		
		HashMap<Integer, Value> values = bid.getValues();
		Issue issue = rankedWeightRatio.get(0);
		Value newValueObject;
		Value value = values.get(issue.getNumber());
		double newValue = getValue(value, issue);
		double max = getUpperBound(issue);
		double min = getLowerBound(issue);
		
		println("Iterating over issues...");
		println("Issue : " + issue.getName());
		while(calculateUtility(bid) < targetUtility && !(issue == null)) {
			if (agentEvaluationAim.get(issue) == 1) {
				newValue = newValue + (max - min)/100;
			} else {
				newValue = newValue - (max - min)/100;
			}
			
			if ((newValue > max && agentEvaluationAim.get(issue) == 1) || (newValue < min && agentEvaluationAim.get(issue) == -1)) {
				println("Maximum value of issue is reached");
				// Maximum value of issue is reached
				if (agentEvaluationAim.get(issue) == 1) {
					newValue = max;
				} else {
					newValue = min;
				}
				newValueObject = getNewValue(value, newValue);
				values.put(issue.getNumber(), newValueObject);
				
				// Setup new issue to increase
				rankedWeightRatio.remove(0);
				if (rankedWeightRatio.size() == 0) {
					println("Iterated over all issues, no issue left to iterate over...");
					issue = null;
				} else {
					println("Get new issue...");
					issue = rankedWeightRatio.get(0);
					println("Issue : " + issue.getName());
					value = values.get(issue.getNumber());
					newValue = getValue(value, issue);
					max = getUpperBound(issue);
					min = getLowerBound(issue);
				}
			} else {
				println("NewValue : " + newValue);
				newValueObject = getNewValue(value, newValue);
				values.put(issue.getNumber(), newValueObject);
			}
			bid = new Bid(utilitySpace.getDomain(), values);
		}

		return bid;
	}
	
	/**
	 * Calculate the weight ratios.
	 * 
	 * @param preference
	 * @return weightRatioMap
	 */
	private Map<Issue, Double> getWeightRatio(Map<Issue, Double> preference) {
		println("Get weight ratio list...");
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
		println("Order issues on weight ratio...");
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
