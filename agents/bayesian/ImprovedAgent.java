import java.util.List;

import negotiator.Agent;
import negotiator.issue.Issue;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;


public abstract class ImprovedAgent extends Agent {
	boolean debug = true;
	
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
	 * Get the value of a value object.
	 * @param value
	 * @return
	 * @throws Exception
	 */
	protected double getValue(Value value) throws Exception {
		switch (value.getType()) {
		case REAL:
			ValueReal valueReal = (ValueReal) value;
			return valueReal.getValue();
		case INTEGER:
			ValueInteger valueInt = (ValueInteger) value;
			return valueInt.getValue();
		default:
			throw new Exception("value type " + value.getType() + " not supported.");
		}
	}
	
	/**
	 * Get the normalized value of  the Value object.
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
			return normalize(valueReal.getValue(), issueReal.getUpperBound(), issueReal.getLowerBound());
		case INTEGER:
			ValueInteger valueInt = (ValueInteger) value;
			IssueInteger issueInt = (IssueInteger) issue;
			return normalize(valueInt.getValue(), issueInt.getUpperBound(), issueInt.getLowerBound());
		default:
			throw new Exception("value type " + value.getType() + " not supported.");
		}
	}
	
	/**
	 * Get the normalized value of  the Value object.
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
			return normalize(valueReal.getValue(), issueReal.getUpperBound(), issueReal.getLowerBound());
		case INTEGER:
			ValueInteger valueInt = (ValueInteger) value;
			IssueInteger issueInt = (IssueInteger) issue;
			return normalize(valueInt.getValue(), issueInt.getUpperBound(), issueInt.getLowerBound());
		default:
			throw new Exception("value type " + value.getType() + " not supported.");
		}
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
