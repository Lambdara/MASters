package masters.agents.bayesian;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;


/**
 * Abstract class for predicting the preference of your opponent.
 * A preference is defined by a weight Map<Issue, Double>. This map maps the issues
 * to their weights. 
 * 
 * @author Mees
 * 
 */
public abstract class PreferenceEstimator {
	List<Issue> issues;
	Map<Issue, Integer> agentEvaluationAim;
	boolean debug = true;
	
	/**
	 * Initialize the PreferenceEstimator with the issues in the domain.
	 * @param issues
	 */
	public PreferenceEstimator(List<Issue> issues, Map<Issue, Integer> agentEvaluationAim){
		this.issues = issues;
		this.agentEvaluationAim = agentEvaluationAim;
	}
	
	/**
	 * Update the model of the predictor with the given offer.
	 * @param offer
	 */
	public abstract void updateModel(Bid bid);
	
	/**
	 * Retrieve the currently most plausible preference of the opponent.
	 * @return
	 */
	public abstract Map<Issue, Double> getPreferenceWeights();
	
	
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
		double u = 0.0;
		double max = 0.0;
		double v;
		HashMap<Integer, Value> values = bid.getValues();
		for (Issue issue : issues) {
			switch (issue.getType()) {
			case REAL:
				IssueReal issueReal = (IssueReal) issue;
				ValueReal valReal = (ValueReal) values.get(issue.getNumber());
				if (agentEvaluationAim.get(issue) == -1) {
					v = valReal.getValue();
				} else {
					v = issueReal.getUpperBound() - valReal.getValue();
				}
				max += weights.get(issue);
				u += weights.get(issue) * normalize(v, issueReal.getUpperBound(), issueReal.getLowerBound());
			case INTEGER:
				IssueInteger issueInt = (IssueInteger) issue;
				ValueInteger valInt = (ValueInteger) values.get(issue.getNumber());
				if (agentEvaluationAim.get(issue) == -1) {
					v = (double) valInt.getValue();
				} else {
					v = (double) issueInt.getUpperBound() - valInt.getValue();
				}
				max += weights.get(issue);
				u += weights.get(issue) * normalize(v, (double) issueInt.getUpperBound(), (double) issueInt.getLowerBound());
			default:
				throw new Exception("issue type " + issue.getType()+ ", value type " + values.get(issue.getNumber()).getType() + " not supported by BayesianPredictor");
			}
		}		
		return normalize(u, max, 0.0);
	}
	
	/**
	 * Return unity-based normalized value.
	 *  
	 * @param val
	 * @param max
	 * @param min
	 * @return normalized val
	 */
	public double normalize(double val, double max, double min) {
		return (val - min) / (max - min);
	}
	
	/**
	 * Return the square a value.
	 * 
	 * @param x
	 * @return
	 */
	public double sq(double x) {
		return x * x;
	}
	
	/**
	 * Convenient print procedure for tracing the process.
	 */
	protected final void print(String s) {
		if (debug)
			System.out.print(s);
	}
	
	/**
	 * Convenient print procedure for tracing the process.
	 */
	protected final void println(String s) {
		if (debug)
			System.out.println("############ " + s);
	}
}