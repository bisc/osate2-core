/*
 * <copyright>
 * Copyright  2012 by Carnegie Mellon University, all rights reserved.
 *
 * Use of the Open Source AADL Tool Environment (OSATE) is subject to the terms of the license set forth
 * at http://www.eclipse.org/org/documents/epl-v10.html.
 *
 * NO WARRANTY
 *
 * ANY INFORMATION, MATERIALS, SERVICES, INTELLECTUAL PROPERTY OR OTHER PROPERTY OR RIGHTS GRANTED OR PROVIDED BY
 * CARNEGIE MELLON UNIVERSITY PURSUANT TO THIS LICENSE (HEREINAFTER THE "DELIVERABLES") ARE ON AN "AS-IS" BASIS.
 * CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED AS TO ANY MATTER INCLUDING,
 * BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABILITY, INFORMATIONAL CONTENT,
 * NONINFRINGEMENT, OR ERROR-FREE OPERATION. CARNEGIE MELLON UNIVERSITY SHALL NOT BE LIABLE FOR INDIRECT, SPECIAL OR
 * CONSEQUENTIAL DAMAGES, SUCH AS LOSS OF PROFITS OR INABILITY TO USE SAID INTELLECTUAL PROPERTY, UNDER THIS LICENSE,
 * REGARDLESS OF WHETHER SUCH PARTY WAS AWARE OF THE POSSIBILITY OF SUCH DAMAGES. LICENSEE AGREES THAT IT WILL NOT
 * MAKE ANY WARRANTY ON BEHALF OF CARNEGIE MELLON UNIVERSITY, EXPRESS OR IMPLIED, TO ANY PERSON CONCERNING THE
 * APPLICATION OF OR THE RESULTS TO BE OBTAINED WITH THE DELIVERABLES UNDER THIS LICENSE.
 *
 * Licensee hereby agrees to defend, indemnify, and hold harmless Carnegie Mellon University, its trustees, officers,
 * employees, and agents from all claims or demands made against them (and any related losses, expenses, or
 * attorney's fees) arising out of, or relating to Licensee's and/or its sub licensees' negligent use or willful
 * misuse of or negligent conduct or willful misconduct regarding the Software, facilities, or other rights or
 * assistance granted by Carnegie Mellon University under this License, including, but not limited to, any claims of
 * product liability, personal injury, death, damage to property, or violation of any laws or regulations.
 *
 * Carnegie Mellon Carnegie Mellon University Software Engineering Institute authored documents are sponsored by the U.S. Department
 * of Defense under Contract F19628-00-C-0003. Carnegie Mellon University retains copyrights in all material produced
 * under this contract. The U.S. Government retains a non-exclusive, royalty-free license to publish or reproduce these
 * documents, or allow others to do so, for U.S. Government purposes only pursuant to the copyright license
 * under the contract clause at 252.227.7013.
 * </copyright>
 */
package org.osate.xtext.aadl2.formatting;

import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.formatting.impl.AbstractDeclarativeFormatter;
import org.eclipse.xtext.formatting.impl.FormattingConfig;
import org.eclipse.xtext.util.Pair;
import org.osate.xtext.aadl2.services.Aadl2GrammarAccess;

/**
 * This class contains custom formatting description.
 *
 * see : http://www.eclipse.org/Xtext/documentation/latest/xtext.html#formatting
 * on how and when to use it
 *
 * Also see {@link org.eclipse.xtext.xtext.XtextFormattingTokenSerializer} as an example
 */
public class Aadl2Formatter extends AbstractDeclarativeFormatter {

	@Override
	protected void configureFormatting(FormattingConfig c) {
		Aadl2GrammarAccess f = (Aadl2GrammarAccess) getGrammarAccess();
	    c.setAutoLinewrap(120);

	    // find common keywords an specify formatting for them
	    for (Pair<Keyword, Keyword> pair : f.findKeywordPairs("(", ")")) {
		      c.setNoSpace().after(pair.getFirst());
		      c.setNoSpace().before(pair.getSecond());
		    }
	    for (Pair<Keyword, Keyword> pair : f.findKeywordPairs("[", "]")) {
		      c.setNoSpace().after(pair.getFirst());
		      c.setNoSpace().before(pair.getSecond());
		    }
	    for (Pair<Keyword, Keyword> pair : f.findKeywordPairs("{**", "**}")) {
		      c.setIndentationIncrement().after(pair.getFirst());
		      c.setLinewrap().after(pair.getFirst());
		      c.setIndentationDecrement().before(pair.getSecond());
		      c.setLinewrap().before(pair.getSecond());
		    }
	    for (Pair<Keyword, Keyword> pair : f.findKeywordPairs("{", "}")) {
		      c.setIndentationIncrement().after(pair.getFirst());
		      c.setLinewrap().after(pair.getFirst());
		      c.setIndentationDecrement().before(pair.getSecond());
		      c.setLinewrap().before(pair.getSecond());
		    }
	    for (Keyword comma : f.findKeywords(",")) {
		      c.setNoSpace().before(comma);
		    }
	    for (Keyword semi : f.findKeywords(";")) {
		      c.setNoSpace().before(semi);
		      c.setLinewrap().after(semi);
		    }
	    for (Keyword dot : f.findKeywords(".")) {
		      c.setNoSpace().around(dot);
		    }
	    for (Keyword doublecolon : f.findKeywords("::")) {
		      c.setNoSpace().around(doublecolon);
		    }
	    for (Keyword fea : f.findKeywords("features")) {
		      c.setLinewrap().around(fea);
		      c.setIndentationDecrement().before(fea);
		      c.setIndentationIncrement().after(fea);
		    }
	    for (Keyword sub : f.findKeywords("subcomponents")) {
		      c.setLinewrap().around(sub);
		      c.setIndentationDecrement().before(sub);
		      c.setIndentationIncrement().after(sub);
		    }
	    for (Keyword conns : f.findKeywords("connections")) {
		      c.setLinewrap().around(conns);
		      c.setIndentationDecrement().before(conns);
		      c.setIndentationIncrement().after(conns);
		    }
	    for (Keyword proto : f.findKeywords("prototypes")) {
		      c.setLinewrap().around(proto);
		      c.setIndentationDecrement().before(proto);
		      c.setIndentationIncrement().after(proto);
		    }
	    for (Keyword flows : f.findKeywords("flows")) {
		      c.setLinewrap().around(flows);
		      c.setIndentationDecrement().before(flows);
		      c.setIndentationIncrement().after(flows);
		    }
	    for (Keyword calls : f.findKeywords("calls")) {
		      c.setLinewrap().around(calls);
		      c.setIndentationDecrement().before(calls);
		      c.setIndentationIncrement().after(calls);
		    }
	    for (Keyword prop : f.findKeywords("properties")) {
		      c.setLinewrap().around(prop);
		      c.setIndentationDecrement().before(prop);
		      c.setIndentationIncrement().after(prop);
		    }
	    for (Keyword end : f.findKeywords("end")) {
		      c.setLinewrap().before(end);
		      c.setIndentationDecrement().before(end);
		    }
	    for (Keyword is : f.findKeywords("is")) {
		      c.setIndentationIncrement().after(is);
		      c.setLinewrap().after(is);
		    }
	    for (Keyword applies : f.findKeywords("applies")) {
		      c.setIndentationIncrement().before(applies);
		      c.setLinewrap().before(applies);
		      c.setIndentationDecrement().after(applies);
		    }
	    for (Keyword requires : f.findKeywords("requires")) {
		      c.setIndentationDecrement().before(requires);
		      c.setLinewrap().before(requires);
		      c.setLinewrap(0,0,0).after(requires);
		      c.setIndentationIncrement().after(requires);
		    }
	    for (Keyword modes : f.findKeywords("modes")) {
		      c.setIndentationDecrement().before(modes);
		      c.setLinewrap().before(modes);
		      c.setLinewrap().after(modes);
		      c.setIndentationIncrement().after(modes);
		    }
	    for (Keyword in : f.findKeywords("in")) {
		      c.setLinewrap(0,0,0).after(in);
		    }

	      c.setLinewrap().around(f.getPublicPackageSectionAccess().getPublicKeyword_1());
	      c.setLinewrap().around(f.getPrivatePackageSectionAccess().getPrivateKeyword_1());

	      c.setLinewrap().before(f.getPublicPackageSectionAccess().getWithKeyword_2_0_0());


	      // WE NEED IT THIS WAY BECAUSE "IN MODES" should not have new lines
	      // modes, requires modes and in modes
//	      c.setIndentationDecrement().before(f.getComponentTypeAccess().getModesKeyword_3_0());
//	      c.setLinewrap().around(f.getComponentTypeAccess().getModesKeyword_3_0());
//	      c.setIndentationIncrement().after(f.getComponentTypeAccess().getModesKeyword_3_0());
//
//	      c.setIndentationDecrement().before(f.getComponentImplementationAccess().getModesKeyword_3_0());
//	      c.setLinewrap().around(f.getComponentImplementationAccess().getModesKeyword_3_0());
//	      c.setIndentationIncrement().after(f.getComponentImplementationAccess().getModesKeyword_3_0());
//
//	      // requires modes
//	      c.setLinewrap().before(f.getComponentTypeAccess().getRequiresKeyword_2_0());
//	      c.setLinewrap().after(f.getComponentTypeAccess().getModesKeyword_2_1());
//	      c.setIndentationDecrement().before(f.getComponentTypeAccess().getRequiresKeyword_2_0());
//	      c.setIndentationIncrement().after(f.getComponentTypeAccess().getModesKeyword_2_1());

	      // component types and implementations
	      c.setIndentationIncrement().before(f.getClassifierRule());
	      c.setIndentationDecrement().after(f.getClassifierRule());
	      c.setLinewrap(2).after(f.getClassifierRule());
//
//	      c.setIndentationIncrement().before(f.getComponentImplementationRule());
//	      c.setIndentationDecrement().after(f.getComponentImplementationRule());
//	      c.setLinewrap(2).after(f.getComponentImplementationRule());


	      c.setIndentationIncrement().after(f.getSystemTypeAccess().getSystemKeyword_0());
	      c.setIndentationIncrement().after(f.getSystemImplementationAccess().getImplementationKeyword_1());
//	      c.setLinewrap().after(f.getSystemImplementationRule());

	      // Need to leave category specific rules in those cases where the rule is not part of the Component Type/Impl rule
	      // This is the case for categories that have special rules for the common subclause sections

	      c.setIndentationIncrement().after(f.getAbstractTypeAccess().getAbstractKeyword_0());
	      c.setIndentationIncrement().after(f.getAbstractImplementationAccess().getAbstractKeyword_0());

	      c.setIndentationIncrement().after(f.getProcessTypeAccess().getProcessKeyword_0());
	      c.setIndentationIncrement().after(f.getProcessImplementationAccess().getProcessKeyword_0());

	      c.setIndentationIncrement().after(f.getThreadGroupTypeAccess().getThreadKeyword_0());
	      c.setIndentationIncrement().after(f.getThreadGroupImplementationAccess().getThreadKeyword_0());

	      c.setIndentationIncrement().after(f.getThreadTypeAccess().getThreadKeyword_0());
	      c.setIndentationIncrement().after(f.getThreadImplementationAccess().getThreadKeyword_0());

	      c.setIndentationIncrement().after(f.getDataTypeAccess().getDataKeyword_0());
	      c.setIndentationIncrement().after(f.getDataImplementationAccess().getDataKeyword_1());

	      c.setIndentationIncrement().after(f.getSubprogramTypeAccess().getSubprogramKeyword_0());
	      c.setIndentationIncrement().after(f.getSubprogramImplementationAccess().getSubprogramKeyword_0());

	      c.setIndentationIncrement().after(f.getSubprogramGroupTypeAccess().getSubprogramKeyword_0());
	      c.setIndentationIncrement().after(f.getSubprogramGroupImplementationAccess().getSubprogramKeyword_0());

	      c.setIndentationIncrement().after(f.getProcessorTypeAccess().getProcessorKeyword_0());
	      c.setIndentationIncrement().after(f.getProcessorImplementationAccess().getProcessorKeyword_0());

	      c.setIndentationIncrement().after(f.getMemoryTypeAccess().getMemoryKeyword_0());
	      c.setIndentationIncrement().after(f.getMemoryImplementationAccess().getMemoryKeyword_0());

	      c.setIndentationIncrement().after(f.getBusTypeAccess().getBusKeyword_0());
	      c.setIndentationIncrement().after(f.getBusImplementationAccess().getBusKeyword_0());

	      c.setIndentationIncrement().after(f.getDeviceTypeAccess().getDeviceKeyword_0());
	      c.setIndentationIncrement().after(f.getDeviceImplementationAccess().getDeviceKeyword_0());

	      c.setIndentationIncrement().after(f.getVirtualProcessorTypeAccess().getVirtualKeyword_0());
	      c.setIndentationIncrement().after(f.getVirtualProcessorImplementationAccess().getVirtualKeyword_0());

	      c.setIndentationIncrement().after(f.getVirtualBusTypeAccess().getVirtualKeyword_0());
	      c.setIndentationIncrement().after(f.getVirtualBusImplementationAccess().getVirtualKeyword_0());

	      c.setIndentationIncrement().after(f.getFeatureGroupTypeAccess().getFeatureKeyword_0());


// It's usually a good idea to activate the following three statements.
// They will add and preserve newlines around comments
			c.setLinewrap(0, 1, 2).before(f.getSL_COMMENTRule());
//			c.setLinewrap(0, 1, 2).before(getGrammarAccess().getML_COMMENTRule());
//			c.setLinewrap(0, 1, 1).after(getGrammarAccess().getML_COMMENTRule());

	}
}
