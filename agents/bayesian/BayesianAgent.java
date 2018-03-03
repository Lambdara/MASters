import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;
import negotiator.issue.Issue;

public class BayesianAgent extends Agent {
	private BayesianPredictor predictor;
	private Action actionOfOpponent = null;
	private Bid lastBidOpponent;
	
	private static final long serialVersionUID = 2L;
	
	/**
	 * init is called when a next session starts with the same opponent.
	 */
	@Override
	public void init() {
		predictor = new BayesianPredictor(getIssueIds());
	}
	
	/**
	 * Version of the agent
	 */
	@Override
	public String getVersion() {
		return "1.0";
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
		actionOfOpponent = opponentAction;
		if (actionOfOpponent instanceof Offer) {
			lastBidOpponent = ((Offer) actionOfOpponent).getBid();
			predictor.updateBeliefs(lastBidOpponent);
		}
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
				// TODO: Initiate negotiation (no offer of opponent received yet)
				// action = ...
			} else if (actionOfOpponent instanceof Offer) {
				if (time < 1 || time > 0.98) {
					action = new Accept(getAgentID(), lastBidOpponent);				
				} else {
					Map<Integer, Double> preferenceOpponent = predictor.getPreference();
					
					// TODO: Create offer out of opponents preference.
					// action = ...
				}
			}
		} catch (Exception e) {
			System.out.println("Exception in ChooseAction:" + e.getMessage());
			if (lastBidOpponent != null) {
				action = new Accept(getAgentID(), lastBidOpponent);
			} else {
				action = new EndNegotiation(getAgentID());
			}
		}
		return action;
	}
	
	/**
	 * Get the issue numbers (id's) of all the issues in the domain.
	 * 
	 * @return List of issues.
	 */
	private List<Integer> getIssueIds() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (Issue issue : utilitySpace.getDomain().getIssues()) {
			result.add(issue.getNumber());
		}
		return result;
	}
}
