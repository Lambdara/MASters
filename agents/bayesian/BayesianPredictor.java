import negotiator.Bid;
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
	private Map<Integer, List<Integer>> hypothesesSpace;
	private Map<Integer, Double> beliefs;
	private Integer best;
	
	/**
	 * Initialize the bayesian predictor by calculating the hypotheses-space and set the beliefs.
	 */
	public BayesianPredictor(List<Integer> issues) {
		this.hypothesesSpace = new HashMap<Integer, List<Integer>>();
		this.beliefs = new HashMap<Integer, Double>();
		this.best = 0;
		
		List<List<Integer>> permutations = generatePerm(issues);
		
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
		Double highest = beliefs.get(best);
		Double newBelief;
		try {
			for (Integer h : beliefs.keySet()) {
				newBelief = beliefs.get(h)*calculateUtility(getWeights(hypothesesSpace.get(h)), bid);
				beliefs.put(h, newBelief);
				if(highest < beliefs.get(h)) {
					best = h;
				}
			}
		} catch (Exception e) {
			System.out.println("Problem while updating bayesian beliefs:" + e.getMessage());
		}
	}
	
	/**
	 * Get the weights, given a ranking of the issues.
	 * Example: ranking: [3, 1, 2]
	 * 			weights: {3:0, 1:0.5, 2:1}
	 * 
	 * @param ranking
	 * @return weights
	 */
	private Map<Integer, Double> getWeights(List<Integer> ranking) {
		HashMap<Integer, Double> weights = new HashMap<Integer, Double>();
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
	private Double calculateUtility(Map<Integer, Double> weights, Bid bid) throws Exception {
		double u = 0.0;
		HashMap<Integer, Value> values = bid.getValues();
		for (int issue : weights.keySet()) {
			switch (values.get(issue).getType()) {
			case REAL:
				ValueReal valReal = (ValueReal) values.get(issue);
				u += weights.get(issue) * valReal.getValue();
			case INTEGER:
				ValueInteger valInt = (ValueInteger) values.get(issue);
				u += weights.get(issue) * valInt.getValue();
			default:
				throw new Exception("value type " + values.get(issue).getType() + " not supported by BayesianPredictor");
			}
		}		
		return u;
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
	 * Get the most likely preference of the opponent. A preference is 
	 * 
	 * @return The preference of the opponent.
	 * 			A Map<Integer, Double>, the integer is the ID of an issue and 
	 * 			the double is the opponents weight of that issue.
	 */
	public Map<Integer, Double> getPreference() {
		return getWeights(hypothesesSpace.get(best));
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
}
