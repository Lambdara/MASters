package masters.agents.uniformaccepter;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.lang.Math;

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

/* NormalAccepter is a really cool agent which estimates bidding behavior by a
   normal distribution and then uses that to estimate the highest bid to expect */
public class UniformAccepter extends Agent {
    Bid optimalBid;
    double optimalUtility;
    double worstUtility;
    int offerAmount;

    Action lastPartnerAction;
    Bid lastPartnerBid;
    double lastPartnerUtility;

    int MINIMUM_HISTORY_LENGTH = 10;
    int MAX_SAMPLE_SIZE = 10000;

    Random random;

    @Override
    public void init() {
        Bid minimalBid;
        try {
            optimalBid = utilitySpace.getMaxUtilityBid();
            minimalBid = utilitySpace.getMinUtilityBid();
            optimalUtility = getUtility(optimalBid);
            worstUtility = getUtility(minimalBid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        random = new Random();
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getName() {
        return "UniformAccepter by MASters";
    }

    @Override
    public void ReceiveMessage(Action opponentAction) {
        lastPartnerAction = opponentAction;
        if (lastPartnerAction instanceof Offer) {
            lastPartnerBid = ((Offer) lastPartnerAction).getBid();
            offerAmount++;
        }
        lastPartnerUtility = getUtility(lastPartnerBid);
    }

    @Override
    public Action chooseAction() {
        double expectedMaximum = getExpectedMaximum();
        System.out.println("Expected maximum: " + Double.toString(expectedMaximum));

        Action action = null;

        if (lastPartnerAction == null)
            return new Offer(getAgentID(), optimalBid);

        try {
            if (lastPartnerAction instanceof Offer &&
                lastPartnerUtility >= expectedMaximum) {

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

    public double getExpectedMaximum() {
        // We can't calculate it in this case so just give the upper bound
        if (offerAmount == 0)
            return optimalUtility;

        double time = timeline.getTime();
        double turnsLeft = offerAmount * (1 - time) / time;

        return turnsLeft/(turnsLeft+1) * (optimalUtility - worstUtility)
            + worstUtility;
    }
}
