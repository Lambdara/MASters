package masters.agents.frequency;

import masters.agents.bayesian.*;

import java.util.*;
import java.lang.Math;

import agents.SimpleAgent;
import com.sun.msv.datatype.xsd.Comparator;
import javafx.util.Pair;
import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;
import negotiator.analysis.pareto.IssueValue;
import negotiator.issue.*;
import negotiator.timeline.Timeline;
import negotiator.utility.AdditiveUtilitySpace;
import org.omg.CORBA.INTERNAL;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class AgentFrequencyUniform extends AbstractAgent {
    Bid optimalBid, worstBid;
    double optimalUtility, worstUtility;

    Action lastPartnerAction;
    Bid lastPartnerBid;
    double lastPartnerUtility;

    int MINIMUM_HISTORY_LENGTH = 5;
    int SAMPLE_SIZE = 10;

    Random random;
    AdditiveUtilitySpace additiveUtilitySpace;
    Map<Integer, ArrayList<Value>> issueValues;
    Map<Integer, Double> ownWeights;

    @Override
    public void init() {
        try {
            issues = utilitySpace.getDomain().getIssues();
            optimalBid = utilitySpace.getMaxUtilityBid();
            worstBid = utilitySpace.getMinUtilityBid();
            agentEvaluationAim = getAgentEvaluationAim();
            additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
            ownWeights = new HashMap<Integer, Double>();
        } catch (Exception e) {
            e.printStackTrace();
        }
        optimalUtility = getUtility(optimalBid);
        worstUtility = getTargetUtil()

        random = new Random();
        issueValues = new HashMap<Integer, ArrayList<Value>>();
        for(Issue issue : utilitySpace.getDomain().getIssues()){
            ArrayList<Value> values = new ArrayList<>();
            issueValues.put(issue.getNumber(), values);
            double ownWeight = additiveUtilitySpace.getWeight(issue.getNumber());
            ownWeights.put(issue.getNumber(), ownWeight);
        }
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getName() {
        return "AgentFrequencyUniform";
    }

    @Override
    public void ReceiveMessage(Action opponentAction) {
        lastPartnerAction = opponentAction;
        if (lastPartnerAction instanceof Offer) {
            lastPartnerBid = ((Offer) lastPartnerAction).getBid();
            addToIssueValues(lastPartnerBid);
        }
        lastPartnerUtility = getUtility(lastPartnerBid);
    }

    //Updates the hashmap where all the values of the issues are stored during the negotiation
    private void addToIssueValues(Bid bid){
        for(Issue issue : bid.getIssues()){
            int issueNumber = issue.getNumber();
            Value value = null;
            switch (issue.getType()){
                case REAL:
                    IssueReal issueReal = (IssueReal)issue;
                    value = (ValueReal) bid.getValue(issueNumber);
                    break;
                case INTEGER:
                    IssueInteger issueInteger = (IssueInteger)issue;
                    value = (ValueInteger) bid.getValue(issueNumber);
                    break;
                case DISCRETE:
                    IssueDiscrete issueDiscrete = (IssueDiscrete)issue;
                    value = (ValueDiscrete) bid.getValue(issueNumber);
                    break;
            }
            ArrayList<Value> values = issueValues.get(issueNumber);
            values.add(value);
        }
    }

    @Override
    public Action chooseAction() {
        Action action = null;

        if (lastPartnerAction == null)
            return new Offer(getAgentID(), optimalBid);
        try {
            if (lastPartnerAction instanceof Offer &&
                    lastPartnerUtility >= getTargetUtil()) {
                action = new Accept(getAgentID(), lastPartnerBid);
            } else {
                Bid bid = createBid();
                action = new Offer(getAgentID(), bid);
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

    //Returns the issue from the last bid done by the opponent
    private Issue getIssueInPartnerBid(int issueNumber){
        for(int i = 0; i < lastPartnerBid.getIssues().size(); i++){
            if(lastPartnerBid.getIssues().get(i).getNumber() == issueNumber){
                return lastPartnerBid.getIssues().get(i);
            }
        }
        return null;
    }

    //Creates a bid
    public Bid createBid() throws Exception {
        //Determines the standard deviations
        Map<Integer, Double> sds = new HashMap<Integer, Double>();
        for(Integer issueNumber : issueValues.keySet()){
            double sd = calculateSD(issueNumber);
            System.out.println("standard deviation of issue " + getIssueInPartnerBid(issueNumber) + ": " + sd);
            sds.put(issueNumber, sd);
        }
        //Determines the order of the ratios
        ArrayList<Integer> issuesRanking = sortByValue(sds);
        System.out.println("Calculated preference of opponent: " + issuesRanking.stream().map(i -> getIssueInPartnerBid(i)).collect(Collectors.toList()));
        Map<Integer, Double> opponentWeights = getWeights(issuesRanking);
        System.out.println("opp weights: " + opponentWeights);
        Map<Integer, Double> ratios = calculateRatios(opponentWeights);
        ArrayList<Pair<Integer, Double>> sortedRatios = sortedRatios(ratios);
        System.out.println("sorted ratios: " + sortedRatios);

        System.out.println("Current target utility: " + getTargetUtil());

        double currentUtility = 0;
        Bid bid = new Bid(utilitySpace.getDomain(), lastPartnerBid.getValues());
        HashMap<Integer, Value> values = bid.getValues();
        //Loops through all the issues and adjusts the value of the issue one by one, until the target utility is reached
        for(Pair pair : sortedRatios){
            Issue issue = getIssueInPartnerBid((Integer)pair.getKey());
            Integer issueNumber = issue.getNumber();
            IssueInteger issueInteger = (IssueInteger) issue;
            int step, start = (int)getValue(lastPartnerBid.getValue(issueNumber), issue), end;
            if(agentEvaluationAim.get(issue) == 1){
                step = 1;
                end = (int)issueInteger.getUpperBound();
            }else{
                step = -1;
                end = (int)issueInteger.getLowerBound();
            }
            //Adjusts the value of the issue step by step, until the target utility is reached
            //TODO
            for(int i = start; agentEvaluationAim.get(issue) == 1 ? i <= end : i >= end ; i += step){
                Value value = new ValueInteger(i);
                values.put(issueNumber, value);
                bid = new Bid(utilitySpace.getDomain(), values);
                currentUtility = calculateUtility(bid);
                if(currentUtility >= getTargetUtil()){
                    break;
                }
            }
            if(currentUtility >= getTargetUtil()){
                break;
            }
        }
        return bid;
    }

    //Determines the target utility, this can vary
    private double getTargetUtil() throws Exception{
        if (offerAmount == 0)
            return bestUtility;
        double time = timeline.getTime();
        double turnsLeft = offerAmount * (1 - time) / time;
        return turnsLeft/(turnsLeft+1) * (optimalUtility - worstUtility) + worstUtility;
    }

    //Calculates the ratio between your own weights and the opponent weights
    private Map<Integer, Double> calculateRatios(Map<Integer, Double> opponentWeights){
        Map<Integer, Double> ratios = new HashMap<Integer, Double>();
        for(Integer issueNumber : opponentWeights.keySet()){
            double opponentWeight = opponentWeights.get(issueNumber);
            double ownWeight = ownWeights.get(issueNumber);
            double ratio = ownWeight / opponentWeight;
            ratios.put(issueNumber, ratio);
        }
        return ratios;
    }

    //Calculates the utility for a bid
    public double calculateUtility(Bid bid) throws Exception {
        HashMap<Integer, Value> values = bid.getValues();

        double u = 0.0;
        double max = 0.0;

        for (Issue issue : issues) {
            double weight = ((AdditiveUtilitySpace) utilitySpace).getWeight(issue.getNumber());
            double normVal = getNormalizedValue(issue, values.get(issue.getNumber()));

            max += weight;
            u += weight * normVal;
        }

        return normalize(u, max, 0.0);
    }

    //Determines which weight each issue gets, according to a ranking of the issues.
    private Map<Integer, Double> getWeights(ArrayList<Integer> ranking) {
        Map<Integer, Double> weights = new HashMap<Integer, Double>();
        int n = ranking.size();
        for (int i = 0; i < n; i++) {
            weights.put(ranking.get(i), 2 * ((double) i + 1)/(n * (n + 1)));
        }
        return weights;
    }

    //Sorts the ratios of the weights, starting with the largest ratio
    private ArrayList<Pair<Integer, Double>> sortedRatios(Map<Integer, Double> ratios){
        ArrayList<Pair<Integer, Double>> sortedRatios = new ArrayList<Pair<Integer, Double>>();

        for(int i = 0; i < ratios.size(); i++) {
            Integer maxIssue = null;
            double maxRatio = Double.MIN_VALUE;
            for (Integer issueNumber : ratios.keySet()) {
                Pair<Integer, Double> pair = new Pair<Integer, Double>(issueNumber, ratios.get(issueNumber));
                if (!sortedRatios.contains(pair) && ratios.get(issueNumber) > maxRatio) {
                    maxIssue = issueNumber;
                    maxRatio = ratios.get(issueNumber);
                }
            }
            sortedRatios.add(new Pair<Integer, Double>(maxIssue, maxRatio));
        }
        return sortedRatios;
    }

    //Sorts the issuenumbers, looking at the standard deviation, starting with the largest
    private ArrayList<Integer> sortByValue(Map<Integer, Double> sds){
        ArrayList<Integer> sortedArray = new ArrayList<Integer>();

        for(int i = 0; i < sds.size(); i++){
            Integer minIssue = 0;
            double minSd = 0;
            for(Integer issueNumber: sds.keySet()){
                if(!sortedArray.contains(issueNumber) && sds.get(issueNumber) >= minSd){
                    minIssue = issueNumber;
                    minSd = sds.get(issueNumber);
                }
            }
            sortedArray.add(minIssue);
        }
        return sortedArray;
    }

    //Calculates the standard deviation over all values of a certain issue that has been offered
    private double calculateSD(Integer issueNumber) throws Exception {
        ArrayList<Value> values = issueValues.get(issueNumber);
        ArrayList<Double> normalizedValues = new ArrayList<Double>();
        Issue issue = getIssueInPartnerBid(issueNumber);

        if(issue == null){
            throw new Exception("issue is null");
        }
        double min = getLowerBound(issue);
        double max = getUpperBound(issue);

        for(Value value : values){
            double integerValue = getValue(value, issue);
            double normalizedValue = normalize(integerValue, min, max);
            normalizedValues.add(normalizedValue);
        }

        double mean = 0;
        for(double value : normalizedValues){
            mean += value;
        }
        mean /= values.size();

        double sd = 0;
        for(double value : normalizedValues){
            sd += Math.pow(value - mean, 2);
        }
        sd /= values.size();
        sd = Math.pow(sd, 0.5);
        return sd;
    }
}
