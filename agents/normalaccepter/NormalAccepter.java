package masters.agents.normalaccepter;

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
public class NormalAccepter extends Agent {
    Bid optimalBid;
    double optimalUtility;

    Action lastPartnerAction;
    Bid lastPartnerBid;
    double lastPartnerUtility;

    ArrayList<Double> history;

    int MINIMUM_HISTORY_LENGTH = 10;
    int MAX_SAMPLE_SIZE = 10000;
    int SAMPLE_REPEATS = 25;

    Random random;

    @Override
    public void init() {
        try {
            optimalBid = utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        optimalUtility = getUtility(optimalBid);
        history = new ArrayList<Double>();
        random = new Random();
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getName() {
        return "NormalAccepter by MASters";
    }

    @Override
    public void ReceiveMessage(Action opponentAction) {
        lastPartnerAction = opponentAction;
        if (lastPartnerAction instanceof Offer) {
            lastPartnerBid = ((Offer) lastPartnerAction).getBid();
        }
        lastPartnerUtility = getUtility(lastPartnerBid);
        history.add(lastPartnerUtility);
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
        if (history.size() < MINIMUM_HISTORY_LENGTH)
            return optimalUtility;

        double mean = 0;
        for (double doub : history) {
            mean += doub;
        }
        mean /= history.size();

        double sd = 0;
        for (double doub : history) {
            sd += Math.pow(doub - mean, 2);
        }
        sd /= history.size();
        sd = Math.pow(sd, 0.5);

        double maximum = 0;

        double time = timeline.getTime();
        int sample_size = (int) (history.size() * (1 - time) / time);
        if (sample_size > MAX_SAMPLE_SIZE)
            sample_size = MAX_SAMPLE_SIZE;
        if (sample_size < 1)
            sample_size = 1;

        for (int i = 0; i < SAMPLE_REPEATS; i++) {
            double tempMaximum = 0;
            for (int j = 0; j < sample_size; j++) {
                double example = random.nextGaussian() * sd + mean;
                if (example > tempMaximum)
                    tempMaximum = example;
            }
            maximum += tempMaximum / SAMPLE_REPEATS;
        }

        return maximum;
    }
}
