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
public class BayesianPredictor {
	List<Issue> issues;
	Map<Integer, List<Issue>> hypothesesSpace;
	Map<Integer, Double> beliefs;
	Integer best;
	boolean debug = true;
	
	/**
	 * Initialize the bayesian predictor by calculating the hypotheses-space and set the beliefs.
	 */
	public BayesianPredictor(List<Issue> issues) {
		println("Initializing BayesianPredictor");
		this.hypothesesSpace = new HashMap<Integer, List<Issue>>();
		this.beliefs = new HashMap<Integer, Double>();
		this.issues = issues;
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
	public void updateBeliefs(Bid bid) {
		println("Updating beliefs");
		Double highest = beliefs.get(best);
		Double newBelief;
		try {
			for (Integer h : beliefs.keySet()) {
				newBelief = beliefs.get(h)*calculateUtilityOpponent(getWeights(hypothesesSpace.get(h)), bid);
				beliefs.put(h, newBelief);
				if(highest < beliefs.get(h)) {
					best = h;
				}
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
		for (int i = 0; i < ranking.size(); i++) {
			weights.put(ranking.get(i), ((double) i)/(ranking.size()-1));
		}
		return weights;
	}
	
	/**
	 * Calculates the utility of the opponent given a bid and a weight distribution.
	 * 
	 * @param weights
	 * @param bid
	 * @return utility
	 * @throws Exception
	 * 			If an issue in the bid is not of type Real or Integer, throw an exception.
	 */
	public Double calculateUtilityOpponent(Map<Issue, Double> weights, Bid bid) throws Exception {
		println("Calculating utility of opponent...");
		
		double u = 0.0;
		double max = 0.0;
		HashMap<Integer, Value> values = bid.getValues();
		println("Start loop over issues...");
		for (Issue issue : weights.keySet()) {
			switch (values.get(issue.getNumber()).getType()) {
			case REAL:
				println("REAL issue");
				IssueReal issueReal = (IssueReal) issue;
				ValueReal valReal = (ValueReal) values.get(issue);
				max += weights.get(issue);
				u += weights.get(issue) * normalize((issueReal.getUpperBound() - valReal.getValue()), issueReal.getUpperBound(), issueReal.getLowerBound());
			case INTEGER:
				println("INTEGER issue");
				IssueInteger issueInt = (IssueInteger) issue;
				ValueInteger valInt = (ValueInteger) values.get(issue);
				max += weights.get(issue);
				u += weights.get(issue) * normalize((issueInt.getUpperBound() - valInt.getValue()), issueInt.getUpperBound(), issueInt.getLowerBound());
			default:
				throw new Exception("value type " + values.get(issue).getType() + " not supported by BayesianPredictor");
			}
		}		
		return normalize(u, max, 0.0);
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
	
	
	/**
	 * Return unity-based normalized value.
	 *  
	 * @param val
	 * @param max
	 * @param min
	 * @return normalized val
	 */
	private double normalize(double val, double max, double min) {
		return (val - min) / (max - min);
	}
	
	/**
	 * Return the square a value.
	 * 
	 * @param x
	 * @return
	 */
	private double sq(double x) {
		return x * x;
	}
	
	/**
	 * Convenient print procedure for tracing the process.
	 */
	void print(String s) {
		System.out.print(s);
	}
	
	/**
	 * Convenient print procedure for tracing the process.
	 */
	void println(String s) {
		if (debug)
			System.out.println("############ " + s);
	}
}
