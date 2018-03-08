package masters.agents.bayesian;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Action;
import negotiator.issue.Issue;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;
import negotiator.timeline.DiscreteTimeline;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorReal;
import negotiator.utility.EvaluatorInteger;
import negotiator.utility.AdditiveUtilitySpace;

public abstract class AbstractAgent extends Agent {
	PreferenceEstimator predictor;
	List<Issue> issues;
	
	Action actionOfOpponent = null;
	Bid lastBidOpponent;
	Bid optimalBid;
	
	Map<Issue, Integer> agentEvaluationAim;
	boolean debug = true;
	
	/** 
	 * Get the aim of the agent, whether it want to maximize/minimize the value of an issue.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected Map<Issue, Integer> getAgentEvaluationAim() throws Exception {
		HashMap<Issue, Integer> result = new HashMap<Issue, Integer>();
		
		Evaluator evaluator;
		for (Issue issue : issues) {
			evaluator = ((AdditiveUtilitySpace) utilitySpace).getEvaluator(issue.getNumber());
			
			if (evaluator instanceof EvaluatorInteger) {
				EvaluatorInteger evaluatorInt = (EvaluatorInteger) evaluator;
				if (evaluatorInt.getUtilLowestValue() < evaluatorInt.getUtilHighestValue()) {
					result.put(issue, 1);
				} else {
					result.put(issue, -1);
				}
			} else if (evaluator instanceof EvaluatorReal) {
				EvaluatorReal evaluatorReal = (EvaluatorReal) evaluator;
				if (evaluatorReal.getLowerBound() < evaluatorReal.getUpperBound()) {
					result.put(issue, 1);
				} else {
					result.put(issue, -1);
				}
			} else {
				throw new Exception("Evaluator type " + evaluator.getType() + " not supported.");
			}
		}
		return result;
	}
	
	/**
	 * Create a new value of type of given value, with newValue as its value.
	 * 
	 * @param value
	 * @param newValue
	 * @return
	 * @throws Exception
	 */
	protected Value getNewValue(Value value, double newValue) throws Exception {
		switch(value.getType()) {
		case REAL:
			return new ValueReal(newValue);
		case INTEGER:
			return new ValueInteger((int) newValue);
		default:
			throw new Exception("value type " + value.getType() + " not supported.");
		}
	}
	
	protected List<Double> getLowerAndUpperBound(Issue issue) throws Exception {
		ArrayList<Double> result = new ArrayList<Double>();
		switch(issue.getType()) {
		case REAL:
			IssueReal issueReal = (IssueReal) issue;
			result.add(issueReal.getLowerBound());
			result.add(issueReal.getUpperBound());
			return result;
		case INTEGER:
			IssueInteger issueInt = (IssueInteger) issue;
			result.add((double) issueInt.getLowerBound());
			result.add((double) issueInt.getUpperBound());
			return result;
		default:
			throw new Exception("value type " + issue.getType() + " not supported.");
		}
	}
	
	/**
	 * Get the lowerbound of an issue.
	 * 
	 * @param issue
	 * @return
	 * @throws Exception
	 */
	protected double getLowerBound(Issue issue) throws Exception {
		switch(issue.getType()) {
		case REAL:
			IssueReal issueReal = (IssueReal) issue;
			return issueReal.getLowerBound();
		case INTEGER:
			IssueInteger issueInt = (IssueInteger) issue;
			return issueInt.getLowerBound();
		default:
			throw new Exception("value type " + issue.getType() + " not supported.");
		}
	}
	
	/**
	 * Get the upperbound of an issue.
	 * 
	 * @param issue
	 * @return
	 * @throws Exception
	 */
	protected double getUpperBound(Issue issue) throws Exception {
		switch(issue.getType()) {
		case REAL:
			IssueReal issueReal = (IssueReal) issue;
			return issueReal.getUpperBound();
		case INTEGER:
			IssueInteger issueInt = (IssueInteger) issue;
			return issueInt.getUpperBound();
		default:
			throw new Exception("value type " + issue.getType() + " not supported.");
		}
	}
	
	/**
	 * Get the Issue object given the ID of the issue.
	 * @param id
	 * @return
	 */
	protected Issue getIssue(int id) {
		List<Issue> issues = utilitySpace.getDomain().getIssues();
		for (Issue i : issues) {
			if (i.getNumber() == id)
				return i;
		}
		return null;
	}
	
	/**
	 * Get the value of a value object, this also takes the evaluation aim into account.
	 * @param value
	 * @return
	 * @throws Exception
	 */
	protected double getValue(Value value, Issue issue) throws Exception {
		switch (value.getType()) {
		case REAL:
			ValueReal valueReal = (ValueReal) value;
			if (agentEvaluationAim.get(issue) == 1)
				return valueReal.getValue();
			return getUpperBound(issue) - valueReal.getValue();
		case INTEGER:
			ValueInteger valueInt = (ValueInteger) value;
			if (agentEvaluationAim.get(issue) == 1)
				return valueInt.getValue();
			return getUpperBound(issue) - valueInt.getValue();
		default:
			throw new Exception("value type " + value.getType() + " not supported.");
		}
	}
	
	/**
	 * Get the normalized value of the Value object, this also takes the evaluation aim into account.
	 * 
	 * @param issue
	 * @param value
	 * @return
	 * @throws Exception
	 */
	protected double getNormalizedValue(Issue issue, Value value) throws Exception {
		switch (value.getType()) {
		case REAL:
			ValueReal valueReal = (ValueReal) value;
			IssueReal issueReal = (IssueReal) issue;
			if (agentEvaluationAim.get(issue) == 1)
				return normalize(valueReal.getValue(), issueReal.getUpperBound(), issueReal.getLowerBound());
			return normalize(issueReal.getUpperBound() - valueReal.getValue(), issueReal.getUpperBound(), issueReal.getLowerBound());
		case INTEGER:
			ValueInteger valueInt = (ValueInteger) value;
			IssueInteger issueInt = (IssueInteger) issue;
			if (agentEvaluationAim.get(issue) == 1)
				return normalize(valueInt.getValue(), issueInt.getUpperBound(), issueInt.getLowerBound());
			return normalize((double) issueInt.getUpperBound() - valueInt.getValue(), issueInt.getUpperBound(), issueInt.getLowerBound());
		default:
			throw new Exception("value type " + value.getType() + " not supported.");
		}
	}
	
	/**
	 * Get the normalized value of  the Value object, this also takes the evaluation aim into account.
	 * 
	 * @param issueId
	 * @param value
	 * @return
	 * @throws Exception
	 */
	protected double getNormalizedValue(int issueId, Value value) throws Exception {
		Issue issue = getIssue(issueId);
		switch (value.getType()) {
		case REAL:
			ValueReal valueReal = (ValueReal) value;
			IssueReal issueReal = (IssueReal) issue;
			if (agentEvaluationAim.get(issue) == 1)
				return normalize(valueReal.getValue(), issueReal.getUpperBound(), issueReal.getLowerBound());
			return normalize(issueReal.getUpperBound() - valueReal.getValue(), issueReal.getUpperBound(), issueReal.getLowerBound());
		case INTEGER:
			ValueInteger valueInt = (ValueInteger) value;
			IssueInteger issueInt = (IssueInteger) issue;
			if (agentEvaluationAim.get(issue) == 1)
				return normalize(valueInt.getValue(), issueInt.getUpperBound(), issueInt.getLowerBound());
			return normalize((issueInt.getUpperBound() - valueInt.getValue()), issueInt.getUpperBound(), issueInt.getLowerBound());
		default:
			throw new Exception("value type " + value.getType() + " not supported.");
		}
	}
	
	public int getRound() {
		return ((DiscreteTimeline) timeline).getRound();
	}

	public int getRoundsLeft() {
		return ((DiscreteTimeline) timeline).getRoundsLeft();
	}

	public int getOwnRoundsLeft() {
		return ((DiscreteTimeline) timeline).getOwnRoundsLeft();
	}

	public int getTotalRounds() {
		return ((DiscreteTimeline) timeline).getTotalRounds();
	}

	public double getTotalTime() {
		return ((DiscreteTimeline) timeline).getTotalTime();
	}
	
	/**
	 * Return unity-based normalized value.
	 *  
	 * @param val
	 * @param max
	 * @param min
	 * @return normalized val
	 */
	protected double normalize(double val, double max, double min) {
		return (val - min) / (max - min);
	}
	
	void printPreference(Map<Issue, Double> preference) {
		println("Estimated preference:");
		for (Issue issue : preference.keySet()) {
			println(issue.getName() + " : " + preference.get(issue));
		}
	}
	
	/**
	 * Convenient print procedure for tracing the process.
	 */
	void print(String s) {
		if (debug)
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
