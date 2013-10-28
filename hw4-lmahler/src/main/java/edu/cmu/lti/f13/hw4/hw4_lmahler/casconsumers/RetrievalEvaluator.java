package edu.cmu.lti.f13.hw4.hw4_lmahler.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_lmahler.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_lmahler.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_lmahler.utils.Utils;



public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and answer relevant values **/
	public ArrayList<Integer> relList;

	 /** query and text relevant values **/
  public ArrayList<ArrayList<Token>> tokenList;

 /** query and text relevant values **/
  public ArrayList<ProcessedDocument> pdList;

  public class ProcessedDocument {
    public Integer queryId;
    public Integer relevantNum;
    public HashMap<String, Integer> tokenVector;
    public double cosineScore;
    public Integer cosineRank;

    public ProcessedDocument() {
      super();
      this.queryId = 0;
      this.relevantNum = 0;
      this.tokenVector = new HashMap<String, Integer>();
      this.cosineScore = 0;
      this.cosineRank = 0;
    }
    
    
  }
		
  public class PdComparator implements Comparator<ProcessedDocument> {
    @Override
    public int compare(ProcessedDocument pd1, ProcessedDocument pd2) {
        if (pd1.cosineScore > pd2.cosineScore) return -1;
        if (pd1.cosineScore < pd2.cosineScore) return 1;
        return 0;
    }
  }
	
  public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();
		
		tokenList = new ArrayList<ArrayList<Token>>();
		
		pdList = new ArrayList<ProcessedDocument>();

	}

	/**
	 * TODO :: 1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();
			ProcessedDocument pd = new ProcessedDocument();
			
			//Store documents to "pdList" - a list of processed documents
			FSList fsTokenList = doc.getTokenList();
			ArrayList<Token>tList = Utils.fromFSListToCollection(fsTokenList, Token.class);
			for (Token t : tList) {
			  pd.tokenVector.put(t.getText(), t.getFrequency());
			}
			
			pd.queryId = doc.getQueryID();
			pd.relevantNum = doc.getRelevanceValue();
			pdList.add(pd);

		}

	}

	/**
	 * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);

		Integer currQId;
		Map<String, Integer> currQueryVector = null;
    Map<String, Integer> currAnswerVector = null;
    //Compute cosine similarity measure
    for (ProcessedDocument q : pdList) {
      if (q.relevantNum == 99) {
        currQId = q.queryId;
        currQueryVector = q.tokenVector;

        for (ProcessedDocument a : pdList){
          if (a.relevantNum != 99 && a.queryId == q.queryId){
            currAnswerVector = a.tokenVector;
            a.cosineScore= computeCosineSimilarity(currQueryVector, currAnswerVector);
          }
        }
        
      }
    }

    //Compute rank of retrieved sentences
    PdComparator pdc = new PdComparator(); 
    Collections.sort(pdList, pdc);
    for (ProcessedDocument q : pdList) {
      int rank = 0;
      if (q.relevantNum == 99) {
        currQId = q.queryId;
        rank = 1;
        System.out.println("\nQuestion: " + q.queryId);
        
        for (ProcessedDocument a : pdList){
          if (a.relevantNum != 99 && a.queryId == q.queryId){
            a.cosineRank = rank;
            rank += 1;
            System.out.println("Score: " + a.cosineScore + "\t" + "Rank: " + a.cosineRank + "\t" + "Rel: " + a.relevantNum);
          }
        }
      }
    }

		
		//Compute mean reciprocal rank (MRR)
		double metric_mrr = compute_mrr();
		System.out.println("\nMean Reciprocal Rank (MRR)::" + metric_mrr);
	}

	/**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> ansVector) {
		double cosine_similarity=0.0;

    Map<String, Integer> mergeVector = new HashMap<String, Integer>();
    for (Map.Entry<String, Integer> entry: queryVector.entrySet()) {
        mergeVector.put(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, Integer> entry: ansVector.entrySet()) {
      mergeVector.put(entry.getKey(), entry.getValue());
  }

		int totalNumerator = 0;
    int totalLhDenominator = 0;
    int totalRhDenominator = 0;
    for (Map.Entry<String, Integer> entry : mergeVector.entrySet()) {
      String term = entry.getKey();
      Integer freq = entry.getValue();

      // Calculate numerator term
      int numerator = (queryVector.containsKey(term) && ansVector.containsKey(term)) ? 
              queryVector.get(term) * ansVector.get(term) : 0;
      totalNumerator += numerator;
      
      // Calculate denominator terms
      int lhDenominator = (int) (queryVector.containsKey(term) ? Math.pow(queryVector.get(term), 2) : 0);
      int rhDenominator = (int) (ansVector.containsKey(term) ? Math.pow(ansVector.get(term), 2) : 0);      
      totalLhDenominator += lhDenominator;
      totalRhDenominator += rhDenominator;
      
    }
    
    cosine_similarity = totalNumerator / (Math.sqrt(totalLhDenominator) * Math.sqrt(totalRhDenominator));

		return cosine_similarity;
	}

	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr=0.0;

    double rr = 0;
    double rrTotal = 0;
    int questionCount = 0;
    for (ProcessedDocument q : pdList) {
      if (q.relevantNum == 99) {
        questionCount += 1;
        
        for (ProcessedDocument a : pdList){
          if (a.relevantNum == 1 && a.queryId == q.queryId){
            rr = (double) 1/a.cosineRank;
          }
        }
        rrTotal += rr;
      }
    }
		
    metric_mrr = rrTotal / questionCount;
		return metric_mrr;
	}

}
