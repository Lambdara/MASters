package masters.agents.bayesian;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


/**
 * @author MASGroup1
 * 
 * 			Bayesian preference predictor in Bilateral Multi-issue Negotiation.
 * 
 * Assumptions:
 * 	- All issues are Real.
 * 	- All issues are conflict issues.
 */
public class BayesianPredictor extends PreferenceEstimator {
	Map<Integer, List<Issue>> hypothesesSpace;
	Map<Integer, Double> beliefs;
	Integer best;
	boolean debug = true;
	
	/**
	 * Initialize the bayesian predictor by calculating the hypotheses-space and set the beliefs.
	 */
	public BayesianPredictor(List<Issue> issues, Map<Issue, Integer> agentEvaluationAim) {
		super(issues, agentEvaluationAim);
		this.hypothesesSpace = new HashMap<Integer, List<Issue>>();
		this.beliefs = new HashMap<Integer, Double>();
		this.best = 0;
		
		List<List<Issue>> permutations = generatePerm(issues);
		
		for (int i = 0; i < permutations.size(); i++) {
			this.hypothesesSpace.put(i, permutations.get(i));
			this.beliefs.put(i, 1/(double)permutations.size());
		}
	}
	
	/**
	 * Update the current beliefs of the hypotheses given the new offer.
	 * 
	 * @param bid
	 * 			The bid of the opponent.
	 */
	public void updateModel(Bid bid) {
		Double highest = (double) 0;
		Double newBelief;
                Double total = (double) 0;
		try {
			for (Integer h : beliefs.keySet()) {
				newBelief = beliefs.get(h)*calculateUtilityOpponent(getWeights(hypothesesSpace.get(h)), bid);
				println("Belief " + h.toString() + " has chance " + newBelief.toString());
				beliefs.put(h, newBelief);
				total += newBelief;
				if(highest <= beliefs.get(h)) {
					best = h;
					highest = beliefs.get(h);
				}
			}
			for (Integer h : beliefs.keySet()) {
				newBelief = beliefs.put(h, beliefs.get(h) / total);
			}
		} catch (Exception e) {
			System.out.println("Problem while updating bayesian beliefs:" + e.getMessage());
			e.printStackTrace();
		}
		println("Best hypothesis : " + best);
	}
	
	/**
	 * Get the weights, given a ranking of the issues.
	 * Example: ranking: [3, 1, 2]
	 * 			weights: {3:0, 1:0.5, 2:1}
	 * 
	 * @param ranking
	 * @return weights
	 */
	private Map<Issue, Double> getWeights(List<Issue> ranking) {
		HashMap<Issue, Double> weights = new HashMap<Issue, Double>();
		int n = ranking.size();
		for (int i = 0; i < n; i++) {
			weights.put(ranking.get(i), 2 * ((double) i + 1)/(n * (n + 1)));
		}
		return weights;
	}
	
	/**
	 * Calculate all the permutations of the given list.
	 * 
	 * @param original
	 * 			The original list that will be used to calculate all permutations of it.
	 * @return List of all permutations of the original list.
	 */
	private <E> List<List<E>> generatePerm(List<E> original) {
		if (original.size() == 0) {
			List<List<E>> result = new ArrayList<List<E>>(); 
			result.add(new ArrayList<E>()); 
			return result; 
		}
		E firstElement = original.remove(0);
		List<List<E>> returnValue = new ArrayList<List<E>>();
		List<List<E>> permutations = generatePerm(original);
		for (List<E> smallerPermutated : permutations) {
			for (int index=0; index <= smallerPermutated.size(); index++) {
				List<E> temp = new ArrayList<E>(smallerPermutated);
				temp.add(index, firstElement);
				returnValue.add(temp);
			}
		}
		return returnValue;
	}
	
	/**
	 * Get the most likely preference of the opponent represented in weights per issue.
	 * 
	 * @return weightMap
	 * 			A Map<Integer, Double>, the integer is the ID of an issue and 
	 * 			the double is the opponents weight of that issue.
	 */
	public Map<Issue, Double> getPreferenceWeights() {
		return getWeights(hypothesesSpace.get(best));
	}
	
	/**
	 * Get the most likely preference of the opponent represented as a ranking.
	 * 
	 * @return rankingList
	 */
	public List<Issue> getPreferenceRanking() {
		return hypothesesSpace.get(best);
	}
}
