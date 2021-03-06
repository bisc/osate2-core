package org.osate.xtext.aadl2.serializer;

import com.google.inject.Inject;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.serializer.analysis.GrammarAlias.AbstractElementAlias;
import org.eclipse.xtext.serializer.analysis.GrammarAlias.GroupAlias;
import org.eclipse.xtext.serializer.analysis.GrammarAlias.TokenAlias;
import org.eclipse.xtext.serializer.analysis.ISyntacticSequencerPDAProvider.ISynNavigable;
import org.eclipse.xtext.serializer.analysis.ISyntacticSequencerPDAProvider.ISynTransition;
import org.eclipse.xtext.serializer.sequencer.AbstractSyntacticSequencer;
import org.osate.xtext.aadl2.services.Aadl2GrammarAccess;

@SuppressWarnings("all")
public abstract class AbstractAadl2SyntacticSequencer extends AbstractSyntacticSequencer {

	protected Aadl2GrammarAccess grammarAccess;
	protected AbstractElementAlias match_AadlPackage___PropertiesKeyword_3_0_NoneKeyword_3_1_1_0_SemicolonKeyword_3_1_1_1__q;
	
	@Inject
	protected void init(IGrammarAccess access) {
		grammarAccess = (Aadl2GrammarAccess) access;
		match_AadlPackage___PropertiesKeyword_3_0_NoneKeyword_3_1_1_0_SemicolonKeyword_3_1_1_1__q = new GroupAlias(false, true, new TokenAlias(false, false, grammarAccess.getAadlPackageAccess().getPropertiesKeyword_3_0()), new TokenAlias(false, false, grammarAccess.getAadlPackageAccess().getNoneKeyword_3_1_1_0()), new TokenAlias(false, false, grammarAccess.getAadlPackageAccess().getSemicolonKeyword_3_1_1_1()));
	}
	
	@Override
	protected String getUnassignedRuleCallToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if(ruleCall.getRule() == grammarAccess.getFLOWINRule())
			return getFLOWINToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getFLOWOUTRule())
			return getFLOWOUTToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getFULLINAMERule())
			return getFULLINAMEToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getIDRule())
			return getIDToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getPNAMERule())
			return getPNAMEToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getSTARRule())
			return getSTARToken(semanticObject, ruleCall, node);
		return "";
	}
	
	/**
	 * FLOWIN: (ID '.')? ID
	 * ;
	 */
	protected String getFLOWINToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "";
	}
	
	/**
	 * FLOWOUT: (ID '.')? ID
	 * ;
	 */
	protected String getFLOWOUTToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "";
	}
	
	/**
	 * FULLINAME:
	 * 	ID '.' ID;
	 */
	protected String getFULLINAMEToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return ".";
	}
	
	/**
	 * terminal ID:	('a'..'z'
	 *         |'A'..'Z'
	 *         ) ( ('_')? ('a'..'z'
	 *         |'A'..'Z'
	 *         |'0'..'9'))*;
	 */
	protected String getIDToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "";
	}
	
	/**
	 * PNAME:
	 * 	ID ('::' ID)*;
	 */
	protected String getPNAMEToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "";
	}
	
	/**
	 * STAR : '*';
	 */
	protected String getSTARToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "*";
	}
	
	@Override
	protected void emitUnassignedTokens(EObject semanticObject, ISynTransition transition, INode fromNode, INode toNode) {
		if (transition.getAmbiguousSyntaxes().isEmpty()) return;
		List<INode> transitionNodes = collectNodes(fromNode, toNode);
		for (AbstractElementAlias syntax : transition.getAmbiguousSyntaxes()) {
			List<INode> syntaxNodes = getNodesFor(transitionNodes, syntax);
			if(match_AadlPackage___PropertiesKeyword_3_0_NoneKeyword_3_1_1_0_SemicolonKeyword_3_1_1_1__q.equals(syntax))
				emit_AadlPackage___PropertiesKeyword_3_0_NoneKeyword_3_1_1_0_SemicolonKeyword_3_1_1_1__q(semanticObject, getLastNavigableState(), syntaxNodes);
			else acceptNodes(getLastNavigableState(), syntaxNodes);
		}
	}

	/**
	 * Syntax:
	 *     ('properties' 'none' ';')?
	 */
	protected void emit_AadlPackage___PropertiesKeyword_3_0_NoneKeyword_3_1_1_0_SemicolonKeyword_3_1_1_1__q(EObject semanticObject, ISynNavigable transition, List<INode> nodes) {
		acceptNodes(transition, nodes);
	}
	
}
