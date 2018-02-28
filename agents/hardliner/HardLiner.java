package masters.agents.hardliner;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import agents.SimpleAgent;
import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;
import negotiator.timeline.Timeline;

public class HardLiner extends Agent {
    Bid optimalBid;
    double optimalUtility;
    Action actionOfPartner;
    Bid lastPartnerBid;
    
    @Override
    public void init() {
        try {
            optimalBid = utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        optimalUtility = getUtility(optimalBid);
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getName() {
        return "Hardliner by MASters";
    }

    @Override
    public void ReceiveMessage(Action opponentAction) {
        actionOfPartner = opponentAction;
        if (actionOfPartner instanceof Offer) {
            lastPartnerBid = ((Offer) actionOfPartner).getBid();
        }
    }

    @Override
    public Action chooseAction() {
        Action action = null;

        try {
            if (actionOfPartner != null &&
                actionOfPartner instanceof Offer &&
                getUtility(((Offer) actionOfPartner).getBid()) >= optimalUtility) {
                
                action = new Accept(getAgentID(), lastPartnerBid);
            } else {
                action = new Offer(getAgentID(), optimalBid);
            }
        } catch (Exception e) {
            System.out.println("Exception in ChooseAction:" + e.getMessage());
            if (lastPartnerBid != null) {
                action = new Accept(getAgentID(), lastPartnerBid);
            } else {
                action = new EndNegotiation(getAgentID());
            }
        }
        return action;
    }
}
