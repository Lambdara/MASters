package masters.agents.extendedbayesian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import negotiator.Bid;
import negotiator.issue.Issue;

public class ExtendedBayesianPredictor extends PreferenceEstimator {
	Map<Integer, List<Double>> hypothesesSpace;
	Map<Integer, Double> beliefs;
	Integer best;
	boolean debug = true;
	
	/**
	 * Initialize the bayesian predictor by calculating the hypotheses-space and set the beliefs.
	 */
	public ExtendedBayesianPredictor(List<Issue> issues, Map<Issue, Integer> agentEvaluationAim) {
		super(issues, agentEvaluationAim);
		this.hypothesesSpace = new HashMap<Integer, List<Double>>();
		this.beliefs = new HashMap<Integer, Double>();
		this.best = 0;
		
		ArrayList<Double> values = new ArrayList<Double>();
		for (int i = 0; i < issues.size()*2 + 1; i++) {
			values.add((double) i/(issues.size()*2));
		}
		
		ArrayList<ArrayList<Double>> permutations = getPerms(values, issues.size());
		
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
	private Map<Issue, Double> getWeights(List<Double> h) {
		HashMap<Issue, Double> weights = new HashMap<Issue, Double>();
		for (Issue issue : issues) {
			weights.put(issue, h.get(issue.getNumber()-1));
		}
		return weights;
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
	 * This function generates all possible weight distributions. Given the values list
	 * which presents all distict weights that are possible in the weight distribution.
	 * @param values
	 * @param permSize
	 * @return
	 */
	public ArrayList<ArrayList<Double>> getPerms(ArrayList<Double> values, Integer permSize) {
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < values.size(); i++) {
			ArrayList<ArrayList<Double>> permutations = recursion(values, permSize, i, 1, values.get(i));
			for (ArrayList<Double> perm : permutations) {
				result.add(perm);
			}
		}
		return result;
	}
	
	/**
	 * Recursive function used to generate the hypothesesSpace.
	 * @param values
	 * @param permSize
	 * @param index
	 * @param curSize
	 * @param sum
	 * @return
	 */
	public ArrayList<ArrayList<Double>> recursion(ArrayList<Double> values, Integer permSize, Integer index, Integer curSize, Double sum) {
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();		
		if (permSize == curSize) {
			ArrayList<Double> perm = new ArrayList<Double>();
			perm.add(values.get(index));
			result.add(perm);
		} else {
			ArrayList<Double> newValues = new ArrayList<Double>();
			for (Double val : values) {
				if (val + sum <= 1.0 && !(curSize == permSize-1)) {
					newValues.add(val);
				} else if (val + sum == 1.0) {
					newValues.add(val);
				}
			}
			
			for (int i = 0; i < newValues.size(); i++) {
				ArrayList<ArrayList<Double>> permutations = recursion(values, permSize, values.indexOf(newValues.get(i)), curSize + 1, values.get(i) + sum);
				for (ArrayList<Double> perm : permutations) {
					perm.add(values.get(index));
					result.add(perm);
				}
			}
		}
		return result;
	}
}
