package edu.cmu.lti.f13.hw4.hw4_lmahler.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.*;

import java.util.*; 

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*; 
import edu.stanford.nlp.ling.CoreAnnotations.*;  
import edu.cmu.lti.f13.hw4.hw4_lmahler.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_lmahler.typesystems.Token;


/**
 * Creates word feature vectors (lemmatized terms and the frequency of each term) for each document. 
 * NOTE: This component uses a stoplist to remove certain words ("a", "the", etc) from the word 
 * feature vectors. To modify the stoplist, edit the inStoplist private function contained within this component.
 */
public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  private boolean inStoplist(String lemma) {
    String[] stopList = {"be", "have", "to", "from", "with", "of", "the", "a", "an", "it", ",", ";"};
    boolean inList = false;
    for (int i = 0; i < stopList.length; i++) {
      if (lemma.equals(stopList[i])) {
        inList = true;
        break;
      }
    }
    return inList;
  }

  @Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {
	  

		//Count each token and its frequency
    //String [] stringList  = doc.getText().toLowerCase().split(" ");
	  //Map<String, Integer> tokenCount = new HashMap<String, Integer>();
	  //for (int i = 0; i < stringList.length; i++) {
	  //    int count = tokenCount.containsKey(stringList[i]) ? tokenCount.get(stringList[i]) : 0;
	  //    tokenCount.put(stringList[i], count + 1);
	  //}

    //---------------New code
    Map<String, Integer> tokenCount = new HashMap<String, Integer>();
    Properties props = new Properties(); 
    props.put("annotators", "tokenize, ssplit, pos, lemma"); 
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props, false);
    String text = doc.getText().toLowerCase(); 
    edu.stanford.nlp.pipeline.Annotation document = pipeline.process(text);  
    for(edu.stanford.nlp.util.CoreMap sentence: document.get(SentencesAnnotation.class)) {    
      for(CoreLabel token: sentence.get(TokensAnnotation.class)) {       
        String lemma = token.get(LemmaAnnotation.class); 
        if (!inStoplist(lemma)) {
          int count = tokenCount.containsKey(lemma) ? tokenCount.get(lemma) : 0;
          tokenCount.put(lemma, count + 1);          
        }
      } 
    }
    //---------------New code
    

    //Create Token annotations and create a temp list of Tokens
    List<Token> tokenList = new ArrayList<Token>();
    for (Map.Entry<String, Integer> entry : tokenCount.entrySet()) {
      String term = entry.getKey();
      Integer freq = entry.getValue();
      //Create Token object
      Token token = new Token(jcas);
      token.setText(term);
      token.setFrequency(freq);
      token.addToIndexes(jcas);
      //Update tokenList
      tokenList.add(token);
    }
    
    //Update Document.tokenList
    FSList fsTokenList = FSCollectionFactory.createFSList(jcas, tokenList);
    doc.setTokenList(fsTokenList);

	}

}
