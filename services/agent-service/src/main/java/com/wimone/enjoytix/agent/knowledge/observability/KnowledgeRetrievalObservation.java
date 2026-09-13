package com.wimone.enjoytix.agent.knowledge.observability;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;
public record KnowledgeRetrievalObservation(KnowledgeRetrievalOutcome outcome,int candidateCount,int finalCount,double citationCoverage,long latencyMillis,String conversationId,String runId) {
 public KnowledgeRetrievalObservation { if(outcome==null||candidateCount<0||finalCount<0||citationCoverage<0||citationCoverage>1||latencyMillis<0) throw new IllegalArgumentException("invalid retrieval observation"); }
}