/*
 *
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

package org.osate.xtext.aadl2.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.BasicInternalEList;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.impl.HiddenLeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IGlobalScopeProvider;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.CheckType;
import org.osate.aadl2.*;
import org.osate.aadl2.impl.DataPortImpl;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.aadl2.properties.PropertyLookupException;
import org.osate.aadl2.properties.PropertyNotPresentException;
import org.osate.aadl2.util.Aadl2InstanceUtil;
import org.osate.aadl2.util.Aadl2Util;
import org.osate.internal.workspace.AadlWorkspace;
import org.osate.workspace.IAadlProject;
import org.osate.workspace.IAadlWorkspace;
import org.osate.workspace.WorkspacePlugin;
import org.osate.xtext.aadl2.properties.util.AadlProject;
import org.osate.xtext.aadl2.properties.util.EMFIndexRetrieval;
import org.osate.xtext.aadl2.properties.util.GetProperties;
import org.osate.xtext.aadl2.properties.util.MemoryProperties;
import org.osate.xtext.aadl2.properties.util.ModelingProperties;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;
import org.osate.xtext.aadl2.scoping.Aadl2GlobalScopeProvider;

import com.google.inject.Inject;

public class Aadl2JavaValidator extends AbstractAadl2JavaValidator {

	@Check(CheckType.FAST)
	public void caseComponentImplementation(ComponentImplementation componentImplementation) {
		checkEndId(componentImplementation);
		checkComponentImplementationUniqueNames(componentImplementation);
		checkComponentImplementationInPackageSection(componentImplementation);
		checkComponentImplementationModes(componentImplementation);
		checkFlowImplementationModeCompatibilityWithRefinedFlowSegments(componentImplementation);
		checkModeSpecificFlowImplementations(componentImplementation);
	}

	@Check(CheckType.FAST)
	public void caseTypeExtension(TypeExtension typeExtension) {
		checkTypeExtensionCategory(typeExtension);
		checkFeaturesOfExtendedAbstractType((ComponentType) typeExtension.getSpecific());
		checkClassifierReferenceInWith(typeExtension.getExtended(), typeExtension);
	}

	@Check(CheckType.FAST)
	public void caseComponentType(ComponentType componentType) {
		checkEndId(componentType);
		checkComponentTypeUniqueNames(componentType);
		checkComponentTypeModes(componentType);
		checkForInheritedFeatureArrays(componentType);
	}

	@Check(CheckType.FAST)
	public void caseImplementationExtension(ImplementationExtension implementationExtension) {
		checkExtensionAndRealizationHierarchy(implementationExtension);
		checkImplementationExtensionCategory(implementationExtension);
		checkSubcomponentsOfExtendedAbstractImplementation((ComponentImplementation) implementationExtension
				.getSpecific());
		checkClassifierReferenceInWith(implementationExtension.getExtended(), implementationExtension);
	}

	@Check(CheckType.FAST)
	public void caseRealization(Realization realization) {
		checkRealizationCategory(realization);
	}

	@Check(CheckType.FAST)
	public void caseComponentTypeRename(ComponentTypeRename componentTypeRename) {
		checkComponentTypeRenameCategory(componentTypeRename);
		checkClassifierReferenceInWith(componentTypeRename.getRenamedComponentType(), componentTypeRename);
	}

	@Check(CheckType.FAST)
	public void caseFeatureGroupTypeRename(FeatureGroupTypeRename fgtRename) {
		checkClassifierReferenceInWith(fgtRename.getRenamedFeatureGroupType(), fgtRename);
	}

	@Check(CheckType.FAST)
	public void caseSubcomponent(Subcomponent subcomponent) {
		checkSubcomponentCategory(subcomponent);
		checkSubcomponentRefinementCategory(subcomponent);
		checkSubcomponentRefinementClassifierSubstitution(subcomponent);
		checkSubcomponentsHierarchy(subcomponent);
		checkClassifierReferenceInWith(subcomponent.getClassifier(), subcomponent);
//		checkPropertyAssocs(subcomponent);
	}

	@Check(CheckType.FAST)
	public void caseComponentPrototype(ComponentPrototype prototype) {
		checkComponentPrototypeCategory(prototype);
		checkRefinedOfComponentPrototype(prototype);
		checkCategoryOfRefinedComponentPrototype(prototype);
		checkArrayOfRefinedComponentPrototype(prototype);
		checkClassifierReferenceInWith(prototype.getConstrainingClassifier(), prototype);
	}

	@Check(CheckType.FAST)
	public void caseComponentPrototypeBinding(ComponentPrototypeBinding binding) {
		checkComponentPrototypeBindingCategory(binding);
		checkFormalOfComponentPrototypeBinding(binding);
	}

	@Check(CheckType.FAST)
	public void caseComponentPrototypeActual(ComponentPrototypeActual prototypeActual) {
		checkComponentPrototypeActualComponentCategory(prototypeActual);
		if (prototypeActual.getSubcomponentType() instanceof Classifier) {
			checkClassifierReferenceInWith((Classifier) prototypeActual.getSubcomponentType(), prototypeActual);
		}
	}

	@Check(CheckType.FAST)
	public void caseFeaturePrototypeBinding(FeaturePrototypeBinding binding) {
		checkFeaturePrototypeBindingDirection(binding);
		checkFormalOfFeaturePrototypeBinding(binding);
	}

	@Check(CheckType.FAST)
	public void caseFeatureGroupPrototypeBinding(FeatureGroupPrototypeBinding binding) {
		checkFormalOfFeatureGroupPrototypeBinding(binding);
	}

	@Check(CheckType.FAST)
	public void caseFeatureGroupPrototypeActual(FeatureGroupPrototypeActual prototypeActual) {
		if (prototypeActual.getFeatureType() instanceof Classifier) {
			checkClassifierReferenceInWith((Classifier) prototypeActual.getFeatureType(), prototypeActual);
		}
	}

	@Check(CheckType.FAST)
	public void caseFeatureGroupPrototype(FeatureGroupPrototype prototype) {
		checkRefinedOfFeatureGroupPrototype(prototype);
	}

	@Check(CheckType.FAST)
	public void caseFeaturePrototype(FeaturePrototype prototype) {
		checkRefinedOfFeaturePrototype(prototype);
		checkDirectionOfRefinedFeaturePrototype(prototype);
	}

	@Check(CheckType.FAST)
	public void casePortSpecification(PortSpecification portspec) {
		checkClassifierReferenceInWith(portspec.getClassifier(), portspec);
	}

	@Check(CheckType.FAST)
	public void caseAccessSpecification(AccessSpecification accessspec) {
		checkClassifierReferenceInWith(accessspec.getClassifier(), accessspec);
	}

	@Check(CheckType.FAST)
	public void caseComponentImplementationReference(ComponentImplementationReference ciref) {
		checkClassifierReferenceInWith(ciref.getImplementation(), ciref);
	}

	@Check(CheckType.FAST)
	public void caseDataType(DataType dataType) {
		checkForInheritedFlowsAndModesFromAbstractType(dataType);

	}

	@Check(CheckType.FAST)
	public void caseDataImplementation(DataImplementation dataImplementation) {
		checkForInheritedFlowsAndModesFromAbstractImplementation(dataImplementation);

	}

	@Check(CheckType.FAST)
	public void caseThreadGroupImplementation(ThreadGroupImplementation threadGroupImplementation) {
		checkForInheritedCallSequenceFromAbstractImplementation(threadGroupImplementation);

	}

	@Check(CheckType.FAST)
	public void caseProcessorImplementation(ProcessorImplementation processorImplementation) {
		checkForInheritedCallSequenceFromAbstractImplementation(processorImplementation);

	}

	@Check(CheckType.FAST)
	public void caseVirtualProcessorImplementation(VirtualProcessorImplementation virtualProcessorImplementation) {
		checkForInheritedCallSequenceFromAbstractImplementation(virtualProcessorImplementation);

	}

	@Check(CheckType.FAST)
	public void caseMemoryType(MemoryType memoryType) {
		checkForInheritedFlowsFromAbstractType(memoryType);

	}

	@Check(CheckType.FAST)
	public void caseMemoryImplementation(MemoryImplementation memoryImplementation) {
		checkForInheritedFlowsAndCallSequenceFromAbstractImplementation(memoryImplementation);

	}

	@Check(CheckType.FAST)
	public void caseBusType(BusType busType) {
		checkForInheritedFlowsFromAbstractType(busType);

	}

	@Check(CheckType.FAST)
	public void caseBusImplementation(BusImplementation busImplementation) {
		checkForInheritedConnectionsFlowsAndCallsFromAbstractImplementation(busImplementation);

	}

	@Check(CheckType.FAST)
	public void caseVirtualBusType(VirtualBusType virtualBusType) {
		checkForInheritedFlowsFromAbstractType(virtualBusType);

	}

	@Check(CheckType.FAST)
	public void caseVirtualBusImplementation(VirtualBusImplementation virtualBusImplementation) {
		checkForInheritedConnectionsFlowsAndCallsFromAbstractImplementation(virtualBusImplementation);

	}

	@Check(CheckType.FAST)
	public void caseDeviceImplementation(DeviceImplementation deviceImplementation) {
		checkForInheritedCallsFromAbstractImplementation(deviceImplementation);

	}

	@Check(CheckType.FAST)
	public void caseFeature(Feature feature) {
		checkTypeOfFeatureRefinement(feature);
		checkFeatureRefinementClassifierSubstitution(feature);
		checkForFeatureArrays(feature);
		checkForArraysInRefinedFeature(feature);
		checkForArrayDimensionSizeInRefinedFeature(feature);
		if (feature instanceof FeatureGroup){
			checkClassifierReferenceInWith(((FeatureGroup)feature).getFeatureGroupType(), feature);
		} else {
		checkClassifierReferenceInWith(feature.getClassifier(), feature);
		}
//		checkPropertyAssocs(feature);
	}

	@Check(CheckType.FAST)
	public void caseConnection(Connection connection) {
		checkDefiningID(connection);

	}

	@Check(CheckType.FAST)
	public void casePortConnection(PortConnection connection) {
		checkPortConnectionClassifiers(connection);
		checkPortConnectionDirection(connection);
		checkPortConnectionEnds(connection);

	}
	
	@Check(CheckType.FAST)
	public void caseParameterConnection(ParameterConnection connection) {
		checkParameterConnectionClassifiers(connection);
	}

	@Check(CheckType.FAST)
	public void caseAccessConnection(AccessConnection connection) {
		checkAccessConnectionCategory(connection);
		checkAccessConnectionProvidesRequires(connection);
		checkAccessConnectionClassifiers(connection);
	}

	@Check(CheckType.FAST)
	public void caseFeatureGroupConnection(FeatureGroupConnection connection) {
		checkFeatureGroupConnectionDirection(connection);
		checkFeatureGroupConnectionClassifiers(connection);
	}

	@Check(CheckType.FAST)
	public void caseFlowSpecification(FlowSpecification flow) {
		checkFlowFeatureType(flow);
		checkFlowFeatureDirection(flow);
	}

	@Check(CheckType.FAST)
	public void caseFlowImplementation(FlowImplementation flow) {
		if (flow.getKind().equals(FlowKind.SOURCE) || flow.getKind().equals(FlowKind.PATH))
			checkOutFeatureIdentifier(flow);
		if (flow.getKind().equals(FlowKind.SINK) || flow.getKind().equals(FlowKind.PATH))
			checkInFeatureIdentifier(flow);
		checkConsistentFlowKind(flow);
		checkFlowConnectionOrder(flow);
		checkFlowConnectionEnds(flow);
		checkFlowSegmentModes(flow);
	}
	
	@Check(CheckType.FAST)
	public void caseEndToEndFlow(EndToEndFlow flow) {
		checkEndToEndFlowSegments(flow);
		checkFlowConnectionEnds(flow);
		checkNestedEndToEndFlows(flow);
		checkEndToEndFlowModes(flow);
	}

	@Check(CheckType.FAST)
	public void caseDirectedFeature(DirectedFeature feature) {
		checkFeatureDirectionInRefinement(feature);

	}

	@Check(CheckType.FAST)
	public void caseAbstractFeature(AbstractFeature feature) {
		checkAbstractFeatureAndPrototypeDirectionConsistency(feature);
		checkForAddedDirectionInAbstractFeatureRefinement(feature);
		checkForAddedPrototypeOrClassifierInAbstractFeatureRefinement(feature);

	}

	@Check(CheckType.FAST)
	public void caseFeatureGroupType(FeatureGroupType featureGroupType) {
		checkEndId(featureGroupType);
		checkForChainedInverseFeatureGroupTypes(featureGroupType);
		checkFeatureGroupTypeUniqueNames(featureGroupType);
	}

	@Check(CheckType.FAST)
	public void caseGroupExtension(GroupExtension extension) {
		checkForExtendingAnInverseFeatureGroupType(extension);
		checkForInverseInFeatureGroupTypeExtension(extension);
		checkForRequiredInverseInFeatureGroupTypeExtension(extension);
		checkClassifierReferenceInWith(extension.getExtended(), extension);
	}

	@Check(CheckType.FAST)
	public void caseFeatureGroup(FeatureGroup featureGroup) {
		checkForInverseInFeatureGroup(featureGroup);
		checkDirectionOfFeatureGroupMembers(featureGroup);
		checkLegalFeatureGroup(featureGroup);
	}

	@Check(CheckType.FAST)
	public void caseSubprogramAccess(SubprogramAccess subprogramAccess) {
		checkSubprogramAccessPrototypeReference(subprogramAccess);
	}

	@Check(CheckType.FAST)
	public void caseSubprogramGroupAccess(SubprogramGroupAccess subprogramGroupAccess) {
		checkSubprogramGroupAccessPrototypeReference(subprogramGroupAccess);
	}

	@Check(CheckType.FAST)
	public void caseAccess(Access access) {
		checkForAbstractFeatureDirectionInAccessRefinement(access);
		checkForAccessTypeInAccessRefinement(access);
	}

	@Check(CheckType.FAST)
	public void caseDataAccess(DataAccess dataAccess) {
		checkDataAccessPrototypeReference(dataAccess);
	}

	@Check(CheckType.FAST)
	public void caseSubprogramCall(SubprogramCall callSpec) {
		if (callSpec.getCalledSubprogram() instanceof Classifier) {
			checkClassifierReferenceInWith((Classifier) callSpec.getCalledSubprogram(), callSpec);
		}
		if (callSpec.getContext() instanceof Classifier) {
			checkClassifierReferenceInWith((Classifier) callSpec.getContext(), callSpec);
		}
	}

	@Check(CheckType.NORMAL)
	public void caseAadlPackage(AadlPackage pack) {
		String findings;

		findings = hasDuplicatesAadlPackage(pack);
		if (findings != null)
		{
			error(pack, "Package " + pack.getName()+" has duplicates "+findings);
		}
	}

	@Check(CheckType.NORMAL)
	public void casePropertySet(PropertySet propSet) {
		String findings;

		findings = hasDuplicatesPropertySet(propSet);
		if (findings != null)
		{
			error(propSet, "Property set " + propSet.getName()+" has duplicates "+findings);
		}
//		
//		if (((Aadl2GlobalScopeProvider)scopeProvider).hasDuplicates(propSet))
//		{
//			if (propSet.getName().equals("AADL_Project"))
//			{
//				IAadlWorkspace workspace;
//				workspace = AadlWorkspace.getAadlWorkspace();
//				IAadlProject[] aadlProjects = workspace.getOpenAadlProjects();
//				for (int i = 0 ; i < aadlProjects.length ; i++)
//				{
//					IAadlProject aadlProject = aadlProjects[i];
//					if (aadlProject.getAadlProjectFile() != null)
//					{
//						return;
//					}		
//				}
//				
//			}
//			error(propSet, "Property set " + propSet.getName()+" has duplicates in this or dependent projects");
//		}
	}

	@Check(CheckType.NORMAL)
	public void caseModelUnit(ModelUnit pack) {
		checkEndId(pack);
	}

	@Check(CheckType.FAST)
	public void caseClassifier(Classifier cl) {
		checkExtendCycles(cl);
	}


	@Check(CheckType.FAST)
	public void caseUnitsType(final UnitsType ut) {
		final EList<EnumerationLiteral> literals = ut.getOwnedLiterals();
		EList<NamedElement> doubles = AadlUtil.findDoubleNamedElementsInList(literals);
		if (doubles.size() > 0) {
			for (NamedElement ne : doubles) {
				error(ne, "Unit '" + ne.getName() + "' previously declared in enumeration");
			}
		}
	}

	@Check(CheckType.FAST)
	public void caseEnumerationType(final EnumerationType et) {
		final EList<EnumerationLiteral> literals = et.getOwnedLiterals();
		EList<NamedElement> doubles = AadlUtil.findDoubleNamedElementsInList(literals);
		if (doubles.size() > 0) {
			for (NamedElement ne : doubles) {
				error(ne, "Literal '" + ne.getName() + "' previously declared in enumeration");
			}
		}
	}

	@Check(CheckType.FAST)
	public void caseRangeType(RangeType nt) {
//		checkRangeType(nt);
		if (nt.getNumberType() != nt.getOwnedNumberType()) {
			checkPropertySetElementReference(nt.getNumberType(), nt);
		}
	}

	@Check(CheckType.FAST)
	public void caseBasicProperty(BasicProperty bp) {
		if (bp.getPropertyType() != bp.getOwnedPropertyType()) {
			checkPropertySetElementReference(bp.getPropertyType(), bp);
		}
	}

	@Check(CheckType.FAST)
	public void caseProperty(Property bp) {
		if (bp.getPropertyType() != bp.getOwnedPropertyType()) {
			checkPropertySetElementReference(bp.getPropertyType(), bp);
		}
		checkPropertyDefinition(bp);
	}

	@Check(CheckType.FAST)
	public void caseListType(ListType bp) {
		if (bp.getElementType() != bp.getOwnedElementType()) {
			checkPropertySetElementReference(bp.getElementType(), bp);
		}
	}

	@Check(CheckType.FAST)
	public void casePropertyConstant(PropertyConstant bp) {
		if (bp.getPropertyType() != bp.getOwnedPropertyType()) {
			checkPropertySetElementReference(bp.getPropertyType(), bp);
		}
		checkPropertyConstant(bp);
	}

	@Check(CheckType.FAST)
	public void caseNumberType(NumberType nt) {
		checkNumberType(nt);
		if (nt.getUnitsType() != nt.getOwnedUnitsType()) {
			checkPropertySetElementReference(nt.getUnitsType(), nt);
		}
	}

	@Check(CheckType.FAST)
	public void caseAadlinteger(final AadlInteger ai) {
		checkAadlinteger(ai);
	}

	/**
	 * check ID at after 'end'
	 */
	public void checkEndId(Classifier cl) {
		ICompositeNode n = NodeModelUtils.getNode(cl);
		INode lln = getLastLeaf(n).getPreviousSibling();
		while (lln instanceof HiddenLeafNode) {
			lln = lln.getPreviousSibling();
		}
		if (lln == null) return;
		String ss = lln.getText().replaceAll("--.*(\\r|\\n)", "").replaceAll(" ", "").replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "");
		if (!ss.equalsIgnoreCase(cl.getName())) {
			error(cl, "Ending '" + ss + "' does not match defining identifier '" + cl.getName() + "'");
		}
	}

	public void checkEndId(ModelUnit mu) {
		ICompositeNode n = NodeModelUtils.getNode(mu);
		INode lln = getPreviousNode(getLastLeaf(n));
		String ss = lln.getText().replaceAll(" ", "").replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "");
//		lln = getPreviousNode(lln);
//		while (lln.getText().equalsIgnoreCase("::")) {
//			lln = getPreviousNode(lln);
//			ss = lln.getText() + "::" + ss;
//		}
//		ss = ss.replaceAll(" ", "");
		if (!ss.equalsIgnoreCase(mu.getName())) {
			error(mu, "Ending '" + ss + "' does not match defining identifier '" + mu.getName() + "'");
		}
	}

	/**
	 * Checks the second part of legality rule 3 in section 10.2 (Flow Implementations) on page 188.
	 * "The out_flow feature of a flow implementation must be identical to the
	 * out_flow feature of the corresponding flow specification."
	 */
	private void checkOutFeatureIdentifier(FlowImplementation flow) {
		if (Aadl2Util.isNull(flow.getSpecification())||
				Aadl2Util.isNull(flow.getSpecification().getAllOutEnd())	){
			return;
		}
		ICompositeNode n = NodeModelUtils.getNode(flow);
		INode lln = getLastLeaf(n);
		String outFeatureName = lln.getText().replaceAll(" ","").replaceAll("\t","").replaceAll("\r", "").replaceAll("\n","");
		lln = getPreviousNode(lln);
		String outContextName = null;
		if (lln != null && lln.getText().replaceAll(" ","").replaceAll("\t","").replaceAll("\r", "").replaceAll("\n","").equals(".")) {
			lln = getPreviousNode(lln);
			outContextName = lln.getText().replaceAll(" ","").replaceAll("\t","").replaceAll("\r", "").replaceAll("\n","");
		}
		FlowSpecification spec = flow.getSpecification();
		if (Aadl2Util.isNull(spec)) return;
		FlowEnd outEnd = spec.getAllOutEnd();
		if (Aadl2Util.isNull(outEnd)) return;
		Context specContext = outEnd.getContext();
		Feature specFeature = outEnd.getFeature();
		if (Aadl2Util.isNull(specFeature)||(specContext!= null &&Aadl2Util.isUnresolved(specContext))){
			// the feature is unresolved or null; or the context is unresolved. 
			// the context could be null but should not be unresolved for the checking to occur
			return;
		}
		//if the feature names don't match
		if (!(outFeatureName.equalsIgnoreCase(specFeature.getName()) ||
		//if the spec has a context, but the impl doesn't: flow spec picks an element from a FG
				(outContextName == null && !Aadl2Util.isNull(specContext)) ||
				//if the impl has a context (FG), but the spec doesn't (feature is FG)
				(outContextName != null && Aadl2Util.isNull(specContext)&&outContextName.equalsIgnoreCase(specFeature.getName())) ||
				//if the context names don't match
				(outContextName != null && !outContextName.equalsIgnoreCase(specContext.getName())))) {
			error(flow,
					'\'' + (outContextName != null ? outContextName + '.' : "") + outFeatureName
							+ "' does not match the out flow feature identifier '"
							+ (specContext != null ? specContext.getName() + '.' : "") + specFeature.getName()
							+ "' in the flow specification.");
		}
	}

	/**
	 * Checks the first part of legality rule 3 in section 10.2 (Flow Implementations) on page 188.
	 * "The in_flow feature of a flow implementation must be identical to the
	 * in_flow feature of the corresponding flow specification."
	 */
	private void checkInFeatureIdentifier(FlowImplementation flow) {
		ICompositeNode n = NodeModelUtils.getNode(flow);
		INode lln = n.getFirstChild();
		while (lln instanceof HiddenLeafNode)
			lln = lln.getNextSibling();
		lln = getNextNode(getNextNode(getNextNode(getNextNode(lln))));
		String inFeatureName = lln.getText().replaceAll(" ","").replaceAll("\t","").replaceAll("\r", "").replaceAll("\n","");
		String inContextName = null;
		int idx = inFeatureName.indexOf(".");
		if (idx >= 0) {
			inContextName = inFeatureName.substring(0, idx);
			inFeatureName = inFeatureName.substring(idx+1, inFeatureName.length());
		}
		FlowSpecification spec = flow.getSpecification();
		if (Aadl2Util.isNull(spec)) return;
		FlowEnd inEnd = spec.getAllInEnd();
		if (Aadl2Util.isNull(inEnd)) return;
		Context specContext = inEnd.getContext();
		Feature specFeature = inEnd.getFeature();
		if (Aadl2Util.isNull(specFeature)||(specContext!= null &&Aadl2Util.isUnresolved(specContext))){
			// the feature is unresolved or null; or the context is unresolved. 
			// the context could be null but should not be unresolved for the checking to occur
			return;
		}
		//if the feature names don't match
		if (!(inFeatureName.equalsIgnoreCase(specFeature.getName()) ||
		//if the spec has a context, but the impl doesn't
				(inContextName == null && !Aadl2Util.isNull(specContext)) ||
				//if the impl has a context, but the spec doesn't
				(inContextName != null && Aadl2Util.isNull(specContext)&&inContextName.equalsIgnoreCase(specFeature.getName())) ||
				//if the context names don't match
				(inContextName != null && !inContextName.equalsIgnoreCase(specContext.getName())))) {
			error(flow,
					'\'' + (inContextName != null ? inContextName + '.' : "") + inFeatureName
							+ "' does not match the in flow feature identifier '"
							+ (specContext != null ? specContext.getName() + '.' : "") + specFeature.getName()
							+ "' in the flow specification.");
		}
	}
	
	private void checkConsistentFlowKind(FlowImplementation flowimpl){
		FlowKind implkind = flowimpl.getKind();
		FlowSpecification spec = flowimpl.getSpecification();
		if (spec != null){
		FlowKind speckind = spec.getKind();
		if (implkind != speckind){
			error(flowimpl,"Flow implementation "+spec.getName()+" must be a flow "+ speckind.getName()+" (same as its flow spec)");
		}
		}
	}
	

	/**
	 * Checks legality rule 1 in section 10.2 (Flow Implementations) on page 188.
	 * "The source of a connection named in a flow implementation declaration must
	 * be the same as the in_flow feature of the flow implementation, or as the
	 * out flow feature of the directly preceding subcomponent flow specification,
	 * if present."
	 * 
	 * Checks legality rule 2 in section 10.2 (Flow Implementations) on page 188.
	 * "The destination of a connection named in a flow implementation declaration
	 * must be the same as the out flow feature of the flow implementation, or as
	 * the in_flow feature of the directly succeeding subcomponent flow
	 * specification, if present.
	 */
	private void checkFlowConnectionOrder(FlowImplementation flow) {
		if (Aadl2Util.isNull(flow.getSpecification())) return;
		EList<FlowSegment> segs = flow.getOwnedFlowSegments();
		boolean connNext = true;
		if (flow.getKind().equals(FlowKind.SOURCE)){
			// the first element in a flow source should be a subcomponent/flow spec
			connNext = false;
		}
		for (FlowSegment flowSegment : segs) {
			FlowElement fe = flowSegment.getFlowElement();
			if (connNext){
				// expecting a connection
				connNext = ! connNext;
				if (!(fe instanceof Connection)){
					error(flow, "Expected connection, found "+ (fe instanceof FlowSpecification?"flow spec ":"subcomponent ")+ fe.getName());
				}
			} else {
				// expecting a component and flow spec
				connNext = ! connNext;
				if (!(fe instanceof Subcomponent || (fe instanceof FlowSpecification && flowSegment.getContext() instanceof Subcomponent))){
					error(flow, "Expected subcomponent/flow spec, found connection "+
				(Aadl2Util.isNull(flowSegment.getContext())?"":flowSegment.getContext().getName())+"."+fe.getName());
				}
				
			}
		}
	}


	/**
	 * Checks legality rule 1 in section 10.2 (Flow Implementations) on page 188.
	 * "The source of a connection named in a flow implementation declaration must
	 * be the same as the in_flow feature of the flow implementation, or as the
	 * out flow feature of the directly preceding subcomponent flow specification,
	 * if present."
	 * 
	 * Checks legality rule 2 in section 10.2 (Flow Implementations) on page 188.
	 * "The destination of a connection named in a flow implementation declaration
	 * must be the same as the out flow feature of the flow implementation, or as
	 * the in_flow feature of the directly succeeding subcomponent flow
	 * specification, if present.
	 */
	private void checkFlowConnectionEnds(FlowImplementation flow) {
		if (Aadl2Util.isNull(flow.getSpecification())) return;
		for (int i = 0; i < flow.getOwnedFlowSegments().size(); i++) {
			ConnectionEnd ce = null;
			Context cxt = null;
			if (flow.getOwnedFlowSegments().get(i).getFlowElement() instanceof Connection) {
				Connection connection = (Connection) flow.getOwnedFlowSegments().get(i).getFlowElement();
				ce = connection.getAllSource();
				cxt = connection.getAllSourceContext();
				boolean didReverse = false;
				if (i == 0) {
					FlowEnd inEnd = flow.getSpecification().getAllInEnd();
					if (Aadl2Util.isNull(inEnd)) return;
					if (!isMatchingConnectionPoint(inEnd.getFeature(), inEnd.getContext(),ce,cxt)) {
						if (connection.isBidirectional()){
							didReverse = true;
							ce = connection.getAllDestination();
							cxt = connection.getAllDestinationContext();
							if(!isMatchingConnectionPoint(inEnd.getFeature(), inEnd.getContext(),ce,cxt)) {
								error(flow.getOwnedFlowSegments().get(i), "The source of connection '" + connection.getName()
										+ "' does not match the in flow feature '"
										+ (inEnd.getContext() != null ? inEnd.getContext().getName() + '.' : "")
										+ inEnd.getFeature().getName() + '\'');
							}
						}
					}
				} else {
					if (flow.getOwnedFlowSegments().get(i - 1).getFlowElement() instanceof FlowSpecification) {
						FlowSpecification previousFlowSegment = (FlowSpecification) flow.getOwnedFlowSegments()
								.get(i - 1).getFlowElement();
						FlowEnd outEnd = previousFlowSegment.getAllOutEnd();
						if (Aadl2Util.isNull(outEnd)) return;
						if (!isMatchingConnectionPoint(outEnd.getFeature(), outEnd.getContext(),ce,cxt)) {
							if (connection.isBidirectional()){
								didReverse = true;
								ce = connection.getAllDestination();
								cxt = connection.getAllDestinationContext();
								if(!isMatchingConnectionPoint(outEnd.getFeature(), outEnd.getContext(),ce,cxt)){
									error(flow.getOwnedFlowSegments().get(i),
											"The source of connection '"
													+ connection.getName()
													+ "' does not match the out flow feature of the preceding subcomponent flow specification '"
													+ flow.getOwnedFlowSegments().get(i - 1).getContext().getName() + '.'
													+ previousFlowSegment.getName() + '\'');
								}
							}
						}
					}
				}
				if (didReverse){
					ce = connection.getAllSource();
					cxt = connection.getAllSourceContext();
				} else {
					ce = connection.getAllDestination();
					cxt = connection.getAllDestinationContext();
				}
				if (i == flow.getOwnedFlowSegments().size() - 1) {
					FlowEnd outEnd = flow.getSpecification().getAllOutEnd();
					if (Aadl2Util.isNull(outEnd)) return;
					if (ce instanceof Feature){
						if (!isMatchingConnectionPoint(outEnd.getFeature(), outEnd.getContext(),ce,cxt)) {
							error(flow.getOwnedFlowSegments().get(i), 
									"The destination of connection '" + connection.getName()
									+ "' does not match the out flow feature '"
									+ (outEnd.getContext() != null ? outEnd.getContext().getName() + '.' : "")
									+ outEnd.getFeature().getName() + '\'');
						}
					}
				} else {
					FlowElement felem = flow.getOwnedFlowSegments().get(i + 1).getFlowElement();
					if (felem instanceof FlowSpecification) {
						FlowSpecification nextFlowSegment = (FlowSpecification) felem;
						FlowEnd inEnd = nextFlowSegment.getAllInEnd();
						if (Aadl2Util.isNull(inEnd)) return;
						if (ce instanceof Feature){
						if (!isMatchingConnectionPoint(inEnd.getFeature(), inEnd.getContext(),ce,cxt)){
							error(flow.getOwnedFlowSegments().get(i),
									"The destination of connection '"
											+ connection.getName()
											+ "' does not match the in flow feature of the succeeding subcomponent flow specification '"
											+ flow.getOwnedFlowSegments().get(i + 1).getContext().getName() + '.'
											+ nextFlowSegment.getName() + '\'');
						}
						}
					}
				}
			}
		}
	}
	
	/**
	 * see if the endpoints of the connection and the flow spec point to the same thing.
	 * They may be refinements of the other. They may be features of feature groups on one or the other side.
	 * @param fsFeature
	 * @param fsContext
	 * @param connEnd
	 * @param connContext
	 * @return
	 */
	private boolean isMatchingConnectionPoint(Feature fsFeature, Context fsContext, ConnectionEnd connEnd, Context connContext){
		if (!(connEnd instanceof Feature)) return true;
		Feature connFeature = (Feature) connEnd;
		return AadlUtil.isSameOrRefines(fsFeature,connFeature) 
		||AadlUtil.isSameOrRefines(connFeature, fsFeature)
		// flow spec points to feature within fg, connection points to fg
		|| (fsContext instanceof FeatureGroup && connFeature instanceof FeatureGroup &&
				(AadlUtil.isSameOrRefines((FeatureGroup)connFeature,(FeatureGroup)fsContext )|| AadlUtil.isSameOrRefines((FeatureGroup)fsContext, (FeatureGroup)connFeature)))
		// both contexts are feature groups. Let's check for features lining up
		||(fsContext instanceof FeatureGroup && connContext instanceof FeatureGroup&&
				(AadlUtil.isSameOrRefines(fsFeature,connFeature )|| AadlUtil.isSameOrRefines(connFeature, fsFeature)))
		// the flow spec has a FG as context and a feature within. The connection can only point to FG.
			||(fsFeature instanceof FeatureGroup && connContext instanceof FeatureGroup&&
				(AadlUtil.isSameOrRefines((FeatureGroup)connContext,(Feature)fsFeature )|| AadlUtil.isSameOrRefines((Feature)fsFeature, (FeatureGroup)connContext)))
		;
	}

	
	/**
	 * Checks legality rule 5 in section 10.2 (Flow Implementations) on page 189.
	 * "In case of a mode-specific flow implementation, the connections and the
	 * subcomponents named in the flow implementation must be declared at least
	 * for the modes listed in the in modes statement of the flow implementation."
	 */
	private void checkFlowSegmentModes(FlowImplementation flow) {
		if (flow.getContainingComponentImpl().getAllModes().isEmpty())
			return;
		EList<Mode> flowModes = flow.getAllInModes();
		if (flowModes.isEmpty())
			flowModes = flow.getContainingComponentImpl().getAllModes();
		for (FlowSegment flowSegment : flow.getOwnedFlowSegments()) {
			if (flowSegment.getContext() instanceof Subcomponent) {
				Subcomponent subcomponent = (Subcomponent)flowSegment.getContext();
				EList<Mode> subcomponentModes = subcomponent.getAllInModes();
				if (subcomponentModes.isEmpty())
					subcomponentModes = subcomponent.getContainingComponentImpl().getAllModes();
				for (Mode flowMode : flowModes) {
					if (!subcomponentModes.contains(flowMode)) {
						error(flowSegment, "Subcomponent '" + subcomponent.getName() + "' does not exist in mode '" + flowMode.getName() + '\'');
					}
				}
			}
			else if (flowSegment.getContext() == null && flowSegment.getFlowElement() instanceof Connection) {
				Connection connection = (Connection)flowSegment.getFlowElement();
				EList<Mode> connectionModes = connection.getAllInModes();
				if (connectionModes.isEmpty())
					connectionModes = connection.getContainingComponentImpl().getAllModes();
				for (Mode flowMode : flowModes) {
					if (!connectionModes.contains(flowMode)) {
						error(flowSegment, "Connection '" + connection.getName() + "' does not exist in mode '" + flowMode.getName() + '\'');
					}
				}
			}
		}
	}
	
	/**
	 * Partially checks legality rule 7 in section 10.2 (Flow Implementations) on page 189.
	 * "Component type extensions may refine flow specifications and component implementation
	 * extensions may refine subcomponents and connections with in modes statements.  A flow
	 * implementation that is inherited by the extension must be consistent with the modes of
	 * the refined flow specifications, subcomponents, and connections if named in the flow
	 * implementation according to rules (L4) and (L5).  Otherwise, the flow implementation
	 * has to be defined again in the component implementation extension and satisfy rules
	 * (L4) and (L5)."
	 * This method checks the (L5) portion of (L7).
	 */
	private void checkFlowImplementationModeCompatibilityWithRefinedFlowSegments(ComponentImplementation componentImplementation) {
		if (componentImplementation.getAllModes().isEmpty())
			return;
		ArrayList<Subcomponent> subcomponentRefinements = new ArrayList<Subcomponent>();
		for (Subcomponent subcomponent : componentImplementation.getOwnedSubcomponents()) {
			if (subcomponent.getRefined() != null) {
				subcomponentRefinements.add(subcomponent);
			}
		}
		ArrayList<Connection> connectionRefinements = new ArrayList<Connection>();
		for (Connection connection : componentImplementation.getOwnedConnections()) {
			if (connection.getRefined() != null) {
				connectionRefinements.add(connection);
			}
		}
		if (subcomponentRefinements.size() == 0 && connectionRefinements.size() == 0)
			return;
		ArrayList<FlowImplementation> inheritedFlows = new ArrayList<FlowImplementation>();
		for (FlowImplementation flow : removeOverridenFlowImplementations(componentImplementation.getAllFlowImplementations())) {
			if (!componentImplementation.getOwnedFlowImplementations().contains(flow)) {
				inheritedFlows.add(flow);
			}
		}
		if (inheritedFlows.size() == 0)
			return;
		for (FlowImplementation flow : inheritedFlows) {
			EList<Mode> flowModes = flow.getAllInModes();
			if (flowModes.isEmpty())
				componentImplementation.getAllModes();
			for (FlowSegment flowSegment : flow.getOwnedFlowSegments()) {
				if (flowSegment.getContext() instanceof Subcomponent) {
					Subcomponent subcomponentRefinement = findSubcomponentRefinement((Subcomponent)flowSegment.getContext(), subcomponentRefinements);
					if (subcomponentRefinement != null) {
						EList<Mode> subcomponentModes = subcomponentRefinement.getAllInModes();
						if (subcomponentModes.isEmpty())
							subcomponentModes = componentImplementation.getAllModes();
						for (Mode flowMode : flowModes) {
							if (!subcomponentModes.contains(flowMode)) {
								error(componentImplementation, "Inherited flow implementation '" + flow.getSpecification().getName() + "' refers to subcomponent refinement '" +
										subcomponentRefinement.getName() + "' which does not exist in mode '" + flowMode.getName() + '\'');
							}
						}
					}
				}
				else if (flowSegment.getContext() == null && flowSegment.getFlowElement() instanceof Connection) {
					Connection connectionRefinement = findConnectionRefinement((Connection)flowSegment.getFlowElement(), connectionRefinements);
					if (connectionRefinement != null) {
						EList<Mode> connectionModes = connectionRefinement.getAllInModes();
						if (connectionModes.isEmpty())
							connectionModes = componentImplementation.getAllModes();
						for (Mode flowMode : flowModes) {
							if (!connectionModes.contains(flowMode)) {
								error(componentImplementation, "Inherited flow implementation '" + flow.getSpecification().getName() + "' refers to connection refinement '" +
										connectionRefinement.getName() + "' which does not exist in mode '" + flowMode.getName() + '\'');
							}
						}
					}
				}
			}
		}
	}
	
	private ArrayList<FlowImplementation> removeOverridenFlowImplementations(EList<FlowImplementation> flows) {
		ArrayList<FlowImplementation> flowsToReturn = new ArrayList<FlowImplementation>();
		HashSet<Integer> indiciesToIgnore = new HashSet<Integer>();
		for (int i = 0; i < flows.size(); i++) {
			if (!indiciesToIgnore.contains(i)) {
				FlowImplementation correctFlow = flows.get(i);
				for (int j = i + 1; j < flows.size(); j++) {
					if (!indiciesToIgnore.contains(j)) {
						if (correctFlow.getSpecification() == flows.get(j).getSpecification()) {
							if (flows.get(j).getContainingClassifier().isDescendentOf(correctFlow.getContainingClassifier())) {
								correctFlow = flows.get(j);
							}
							indiciesToIgnore.add(j);
						}
					}
				}
				flowsToReturn.add(correctFlow);
			}
		}
		return flowsToReturn;
	}
	
	private Subcomponent findSubcomponentRefinement(Subcomponent baseSubcomponent, ArrayList<Subcomponent> subcomponentRefinements) {
		for (Subcomponent refinement : subcomponentRefinements) {
			for (Subcomponent currentParent = refinement.getRefined(); currentParent != null; currentParent = currentParent.getRefined()) {
				if (currentParent == baseSubcomponent) {
					return refinement;
				}
			}
		}
		return null;
	}
	
	private Connection findConnectionRefinement(Connection baseConnection, ArrayList<Connection> connectionRefinements) {
		for (Connection refinement : connectionRefinements) {
			for (Connection currentParent = refinement.getRefined(); currentParent != null; currentParent = currentParent.getRefined()) {
				if (currentParent == baseConnection) {
					return refinement;
				}
			}
		}
		return null;
	}
	
	/**
	 * Checks legality rule 4 in section 10.2 (Flow Implementations) on page 189.
	 * "If the component implementation provides mode-specific flow implementations,
	 * as indicated by the in modes statement, then the set of modes in the in modes
	 * statement of all flow implementations for a given flow specification must
	 * include all the modes for which the flow specification is declared."
	 * 
	 * Partially checks legality rule 7 in section 10.2 (Flow Implementations) on page 189.
	 * "Component type extensions may refine flow specifications and component implementation
	 * extensions may refine subcomponents and connections with in modes statements.  A flow
	 * implementation that is inherited by the extension must be consistent with the modes of
	 * the refined flow specifications, subcomponents, and connections if named in the flow
	 * implementation according to rules (L4) and (L5).  Otherwise, the flow implementation
	 * has to be defined again in the component implementation extension and satisfy rules
	 * (L4) and (L5).
	 * This method checks the (L4) portion of (L7).
	 */
	private void checkModeSpecificFlowImplementations(ComponentImplementation componentImplementation) {
		EList<Mode> componentModes = componentImplementation.getAllModes();
		if (componentModes.isEmpty())
			return;
		HashMap<FlowSpecification, HashSet<Mode>> allFlowImplementationModes = new HashMap<FlowSpecification, HashSet<Mode>>();
		for (FlowImplementation flowImplementation : componentImplementation.getAllFlowImplementations()) {
			HashSet<Mode> flowModesSet = allFlowImplementationModes.get(flowImplementation.getSpecification());
			if (flowModesSet == null) {
				flowModesSet = new HashSet<Mode>();
				allFlowImplementationModes.put(flowImplementation.getSpecification(), flowModesSet);
			}
			if (flowImplementation.getAllInModes().isEmpty())
				flowModesSet.addAll(componentModes);
			else
				flowModesSet.addAll(flowImplementation.getAllInModes());
		}
		for (Entry<FlowSpecification, HashSet<Mode>> entry : allFlowImplementationModes.entrySet()) {
			EList<Mode> flowSpecificationModes = entry.getKey().getAllInModes();
			if (flowSpecificationModes.isEmpty())
				flowSpecificationModes = componentImplementation.getAllModes();
			for (Mode flowSpecificationMode : flowSpecificationModes) {
				if (!entry.getValue().contains(flowSpecificationMode)) {
					error(componentImplementation, "Flow implementation '" + entry.getKey().getName() + "' needs to be declared for mode '" + flowSpecificationMode.getName() + '\'');
				}
			}
		}
	}
	
	/**
	 * Partially checks naming rule 3 in section 10.3 (End-To-End Flows) on page 191.
	 * "The subcomponent flow identifier of an end-to-end flow declaration must name
	 * an optional flow specification in the component type of the named subcomponent
	 * or to a data component in the form of a data subcomponent, provides data access,
	 * or requires data access."
	 * This method only checks if the reference is to a connection.
	 * 
	 * Checks legality rule 1 in section 10.3 (End-To-End Flows) on page 191.
	 * "The flow specifications identified by the flow_path_subcomponent_flow_identifier
	 * must be flow paths, if present."
	 * 
	 * Checks legality rule 2 in section 10.3 (End-To-End Flows) on page 191.
	 * "The start_subcomponent_flow_identifier must refer to a flow path or  flow source,
	 * or to a data component."
	 * 
	 * Checks legality rule 3 in section 10.3 (End-To-End Flows) on page 191.
	 * "The end_subcomponent_flow_identifier must refer to a flow path or a flow sink, or
	 * to a data component."
	 */
	private void checkEndToEndFlowSegments(EndToEndFlow flow) {
		for (int i = 0; i < flow.getOwnedEndToEndFlowSegments().size(); i++) {
			EndToEndFlowSegment segment = flow.getOwnedEndToEndFlowSegments().get(i);
			if (i % 2 == 0) {
				if (segment.getFlowElement() instanceof Connection && segment.getContext() == null) {
					error(segment, "Illegal reference to connection '" + segment.getFlowElement().getName() + "'.  Expecting subcomponent flow or end-to-end flow reference.");
				}
				else if (i == 0) {
					// first element of an ETEF
					if (segment.getFlowElement() instanceof FlowSpecification) {
						if (segment.getContext() == null) {
							error(segment, "Illegal reference to '" + segment.getFlowElement().getName() + "'.  Cannot refer to a flow specification in the local classifier's namespace.");
						}
						else if (((FlowSpecification)segment.getFlowElement()).getKind() == FlowKind.SINK) {
							error(segment, "Illegal reference to '" + segment.getContext().getName() + '.' + segment.getFlowElement().getName() +
									"'.  First segment of end-to-end flow cannot refer to a flow sink.");
						}
					}
				}
				else if (i == flow.getOwnedEndToEndFlowSegments().size() - 1) {
					// last element of ETEF
					if (segment.getFlowElement() instanceof FlowSpecification) {
						if (segment.getContext() == null) {
							error(segment, "Illegal reference to '" + segment.getFlowElement().getName() + "'.  Cannot refer to a flow specification in the local classifier's namespace.");
						}
						else if (((FlowSpecification)segment.getFlowElement()).getKind() == FlowKind.SOURCE) {
							error(segment, "Illegal reference to '" + segment.getContext().getName() + '.' + segment.getFlowElement().getName() +
									"'.  Last segment of end-to-end flow cannot refer to a flow source.");
						}
					}
				}
				else {
					// an intermediate ETEF
					if (segment.getFlowElement() instanceof DataAccess && segment.getContext() == null) {
						error(segment, "Illegal reference to '" + segment.getFlowElement().getName() + "'.  Cannot refer to a data access except for the first and last segment of an end-to-end flow.");
					}
					else if (segment.getFlowElement() instanceof FlowSpecification) {
						if (segment.getContext() == null) {
							error(segment, "Illegal reference to '" + segment.getFlowElement().getName() + "'.  Cannot refer to a flow specification in the local classifier's namespace.");
						}
						else if (((FlowSpecification)segment.getFlowElement()).getKind() == FlowKind.SOURCE) {
							error(segment, "Illegal reference to '" + segment.getContext().getName() + '.' + segment.getFlowElement().getName() +
									"'.  Cannot refer to a flow source except for the first segment of an end-to-end flow.");
						}
						else if (((FlowSpecification)segment.getFlowElement()).getKind() == FlowKind.SINK) {
							error(segment, "Illegal reference to '" + segment.getContext().getName() + '.' + segment.getFlowElement().getName() +
									"'.  Cannot refer to a flow sink except for the last segment of an end-to-end flow.");
						}
					}
				}
			}
		}
	}

	private void checkFlowConnectionEnds(EndToEndFlow flow) {
		int size = flow.getOwnedEndToEndFlowSegments().size();
		for (int i = 0; i < size; i++) {
			ConnectionEnd ce = null;
			Context cxt = null;
			if (flow.getOwnedEndToEndFlowSegments().get(i).getFlowElement() instanceof Connection) {
				// for connection (every even element) check that it matches up with the preceding flow specification
				Connection connection = (Connection) flow.getOwnedEndToEndFlowSegments().get(i).getFlowElement();
				ce = connection.getAllSource();
				cxt = connection.getAllSourceContext();
				boolean didReverse = false;
				if (i>0&&flow.getOwnedEndToEndFlowSegments().get(i - 1).getFlowElement() instanceof FlowSpecification) {
					FlowSpecification previousFlowSegment = (FlowSpecification) flow.getOwnedEndToEndFlowSegments()
							.get(i - 1).getFlowElement();
					FlowEnd outEnd = previousFlowSegment.getAllOutEnd();
					if (Aadl2Util.isNull(outEnd)) return;
					if (! isMatchingConnectionPoint(outEnd.getFeature(),outEnd.getContext(),ce,cxt)) {
						if (connection.isBidirectional()){
							ce = connection.getAllDestination();
							cxt = connection.getAllDestinationContext();
							if(! isMatchingConnectionPoint(outEnd.getFeature(),outEnd.getContext(),ce,cxt)){
								error(flow.getOwnedEndToEndFlowSegments().get(i),
										"The source of connection '"
												+ connection.getName()
												+ "' does not match the out flow feature of the preceding subcomponent flow specification '"
												+ flow.getOwnedEndToEndFlowSegments().get(i - 1).getContext().getName() + '.'
												+ previousFlowSegment.getName() + '\'');
							} else {
								didReverse = true;
							}
						}
					}
				}
				if (didReverse){
					ce = connection.getAllSource();
					cxt = connection.getAllSourceContext();
				} else {
					ce = connection.getAllDestination();
					cxt = connection.getAllDestinationContext();
				}
				if (i+1< size){
					EndToEndFlowElement felem = flow.getOwnedEndToEndFlowSegments().get(i + 1).getFlowElement();
					if (felem instanceof FlowSpecification) {
						FlowSpecification nextFlowSegment = (FlowSpecification) felem;
						FlowEnd inEnd = nextFlowSegment.getAllInEnd();
						if (Aadl2Util.isNull(inEnd)) return;
						if (ce instanceof Feature){
							if (!isMatchingConnectionPoint(inEnd.getFeature(),inEnd.getContext(),ce,cxt)){
								error(flow.getOwnedEndToEndFlowSegments().get(i),
										"The destination of connection '"
												+ connection.getName()
												+ "' does not match the in flow feature of the succeeding subcomponent flow specification '"
												+ flow.getOwnedEndToEndFlowSegments().get(i + 1).getContext().getName() + '.'
												+ nextFlowSegment.getName() + '\'');
							}
						}
					}
				}
			}
		}
	}

	
	/**
	 * Checks legality rule 4 in section 10.3 (End-To-End Flows) on page 191.
	 * "If an end-to-end flow is referenced in an end-to-end flow declaration, then its
	 * first and last subcomponent flow must name the same port as the preceding or
	 * succeeding connection."
	 * 
	 * Checks a proposed legality rule for end-to-end flows.
	 * "If the start_subcomponent_flow_identifier or flow_path_subcomponent_flow_identifier
	 * refers to an end-to-end flow, then the referenced flow's last subcomponent flow
	 * cannot be a flow sink."
	 * 
	 * Checks a proposed legality rule for end-to-end flows.
	 * "If the end_subcomponent_flow_identifier or flow_path_subcomponent_flow_identifier
	 * refers to an end-to-end flow, then the referenced flows's first subcomponent flow
	 * cannot be a flow source."
	 */
	private void checkNestedEndToEndFlows(EndToEndFlow flow) {
		for (int i = 0; i < flow.getOwnedEndToEndFlowSegments().size(); i++) {
			EndToEndFlowSegment segment = flow.getOwnedEndToEndFlowSegments().get(i);
			if (segment.getFlowElement() instanceof EndToEndFlow) {
				EndToEndFlow referencedFlow = (EndToEndFlow)segment.getFlowElement();
				if (i < flow.getOwnedEndToEndFlowSegments().size() - 1) {
					if (referencedFlow.getOwnedEndToEndFlowSegments().get(referencedFlow.getOwnedEndToEndFlowSegments().size() - 1).getFlowElement() instanceof FlowSpecification) {
						FlowSpecification referencedEndFlowSpec =
								(FlowSpecification)referencedFlow.getOwnedEndToEndFlowSegments().get(referencedFlow.getOwnedEndToEndFlowSegments().size() - 1).getFlowElement();
						if (referencedEndFlowSpec.getKind() == FlowKind.SINK) {
							error(segment, "The last subcomponent flow of '" + referencedFlow.getName() + "' cannot be a flow sink.");
						}
						else if (referencedEndFlowSpec.getKind() == FlowKind.PATH && flow.getOwnedEndToEndFlowSegments().get(i + 1).getFlowElement() instanceof Connection) {
							Connection nextConnection = (Connection)flow.getOwnedEndToEndFlowSegments().get(i + 1).getFlowElement();
							if (referencedEndFlowSpec.getAllOutEnd().getFeature() != nextConnection.getAllSource()) {
								error(segment, "The last subcomponent flow of '" + referencedFlow.getName() + "' does not name the same feature as the source of the succeeding connection '" +
										nextConnection.getName() + "'.");
							}
						}
					}
				}
				if (i > 0) {
					if (referencedFlow.getOwnedEndToEndFlowSegments().get(0).getFlowElement() instanceof FlowSpecification) {
						FlowSpecification referencedStartFlowSpec = (FlowSpecification)referencedFlow.getOwnedEndToEndFlowSegments().get(0).getFlowElement();
						if (referencedStartFlowSpec.getKind() == FlowKind.SOURCE) {
							error(segment, "The first subcomponent flow of '" + referencedFlow.getName() + "' cannot be a flow source.");
						}
						else if (referencedStartFlowSpec.getKind() == FlowKind.PATH && flow.getOwnedEndToEndFlowSegments().get(i - 1).getFlowElement() instanceof Connection) {
							Connection previousConnection = (Connection)flow.getOwnedEndToEndFlowSegments().get(i - 1).getFlowElement();
							if (referencedStartFlowSpec.getAllInEnd().getFeature() != previousConnection.getAllDestination()) {
								error(segment, "The first subcomponent flow of '" + referencedFlow.getName() + "' does not name the same feature as the destination of the preceding connection '" +
										previousConnection.getName() + "'.");
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Checks legality rule 5 in section 10.3 (End-To-End Flows) on page 192.
	 * "In case of a mode specific end-to-end flow declarations, the named connections
	 * and the subcomponents of the named flow specifications must be declared for the
	 * modes listed in the in modes statement."
	 */
	private void checkEndToEndFlowModes(EndToEndFlow flow) {
		EList<Mode> neededModes = flow.getAllInModes();
		if (neededModes.isEmpty())
			neededModes = flow.getContainingComponentImpl().getAllModes();
		if (neededModes.isEmpty())
			return;
		for (EndToEndFlowSegment segment : flow.getAllFlowSegments()) {
			EList<Mode> segmentModes = null;
			if (segment.getContext() != null && segment.getContext() instanceof ModalElement) {
				segmentModes = ((ModalElement)segment.getContext()).getAllInModes();
			}
			else if (segment.getContext() == null && segment.getFlowElement() instanceof ModalElement) {
				segmentModes = ((ModalElement)segment.getFlowElement()).getAllInModes();
			}
			if (segmentModes != null && !segmentModes.isEmpty()) {
				for (Mode neededMode : neededModes) {
					if (!segmentModes.contains(neededMode)) {
						error(segment, "'" + (segment.getContext() == null ? "" : segment.getContext().getName() + '.') + segment.getFlowElement().getName() + "' does not exist in mode '" +
								neededMode.getName() + "'.");
					}
				}
			}
		}
	}

	public void checkExtendCycles(Classifier cl) {
		if (hasExtendCycles(cl)) {
			error(cl, "The extends hierarchy of " + cl.getName() + " has a cycle.");
		}
	}

	public void checkPackageReference(AadlPackage pack, Element context) {
		if (Aadl2Util.isNull(pack))
			return;
		Namespace contextNS = AadlUtil.getContainingTopLevelNamespace(context);
		if (!AadlUtil.isImportedPackage(pack, contextNS)) {
			error(context, "The referenced package '" + pack.getName() + "' is not listed in a with clause.");
		}
	}

	protected INode getLastLeaf(INode node) {
		INode result = node;
		while (result instanceof ICompositeNode)
			result = ((ICompositeNode) result).getLastChild();
		return result != null ? result : node;
	}

	protected INode getPreviousNode(INode node) {
		INode lln = node.getPreviousSibling();
		while (lln instanceof HiddenLeafNode) {
			lln = lln.getPreviousSibling();
		}
		return lln;
	}

	protected INode getNextNode(INode node) {
		INode lln = node.getNextSibling();
		while (lln instanceof HiddenLeafNode) {
			lln = lln.getNextSibling();
		}
		return lln;
	}

	/**
	 * check for unique names in component type
	 */
	public void checkComponentTypeUniqueNames(ComponentType type) {
		// process in core package
		EList<NamedElement> l = new BasicEList<NamedElement>();
		l.addAll(type.getAllFlowSpecifications());
		l.addAll(type.getAllFeatures());
		l.addAll(type.getAllModes());
		l.addAll(type.getAllModeTransitions());
		l.addAll(type.getAllPrototypes());
		EList<NamedElement> doubles = AadlUtil.findDoubleNamedElementsInList(l);
		if (doubles.size() > 0) {
			for (NamedElement ne : doubles) {
				error(ne, ne.eClass().getName() + " identifier '" + ne.getName()
						+ "' previously defined. Maybe you forgot 'refined to'");
			}
		}
	}

	/**
	 * check for unique names in implementation
	 */
	public void checkComponentImplementationUniqueNames(ComponentImplementation impl) {
		// process in core package
		EList<NamedElement> usedNames = new BasicEList<NamedElement>();
		usedNames.addAll(impl.getAllPrototypes());
		usedNames.addAll(impl.getAllFeatures());
		usedNames.addAll(impl.getAllSubcomponents());
		usedNames.addAll(impl.getAllConnections());
		usedNames.addAll(impl.getAllModes());
		usedNames.addAll(impl.getAllModeTransitions());
		if (!Aadl2Util.isNull(impl.getType()))
			usedNames.addAll(impl.getType().getAllFlowSpecifications());
		usedNames.addAll(impl.getAllEndToEndFlows());
		EList<SubprogramCallSequence> csl = null;
		if (impl instanceof ThreadImplementation) {
			csl = ((ThreadImplementation) impl).getOwnedSubprogramCallSequences();
		} else if (impl instanceof SubprogramImplementation) {
			csl = ((SubprogramImplementation) impl).getOwnedSubprogramCallSequences();
		}
		if (csl != null) {
			usedNames.addAll(csl);
			for (SubprogramCallSequence subprogramCallSequence : csl) {
				usedNames.addAll(subprogramCallSequence.getOwnedCallSpecifications());
			}
		}

		EList<NamedElement> doubles = AadlUtil.findDoubleNamedElementsInList(usedNames);
		if (doubles.size() > 0) {
			for (NamedElement ne : doubles) {
				error(impl,
						"Identifier '" + ne.getName() + "' has previously been defined in implementation '"
								+ impl.getQualifiedName() + "' or in type '" + impl.getTypeName() + "'");
			}
		}
		EList<FlowImplementation> fimpllist = impl.getAllFlowImplementations();
		final Set<String> seen = new HashSet<String>();
		for (FlowImplementation flowImplementation : fimpllist) {
			if (flowImplementation.getInModeOrTransitions().isEmpty()){
				// check of previously declared
				FlowSpecification spec = flowImplementation.getSpecification();
				if (!Aadl2Util.isNull(spec)){
					if (!seen.add(spec.getName())){
						error(flowImplementation,"Flow implementation "+spec.getName()+" declared more than once.");
					}
				}
			}
		}
	}
	

	/**
	 * check for unique names in component type
	 */
	public void checkFeatureGroupTypeUniqueNames(FeatureGroupType type) {
		// process in core package
		EList<NamedElement> l = new BasicEList<NamedElement>();
		l.addAll(type.getAllFeatures());
		l.addAll(type.getAllPrototypes());
		EList<NamedElement> doubles = AadlUtil.findDoubleNamedElementsInList(l);
		if (doubles.size() > 0) {
			for (NamedElement ne : doubles) {
				error(ne, ne.eClass().getName() + " identifier '" + ne.getName()
						+ "' previously defined. Maybe you forgot 'refined to'");
			}
		}
	}


	/*
	 * supporting semantic check methods They can on the error reporter thus
	 * reside in here
	 */

	/**
	 * Checks legality rule 3 in section 4.2 (Packages) on page 32. "A component
	 * implementation may be declared in both the public and private part of a
	 * package. In that case the declaration in the public part may only contain
	 * a properties subclause and a modes subclause."
	 * 
	 * Checks semantic rule 9 in section 4.2 (Packages) on page 33. "A component
	 * implementation can be declared in both the public and private section of
	 * a package. If it is declared in both, then the public declaration is
	 * limited to containing property associations and modes and only those
	 * items are visible outside the package. This allows component
	 * implementation to be made visible to other packages as variants of the
	 * same component type, while the details of the implementation, i.e., its
	 * realization expressed by the subcomponents and connections subclauses,
	 * are hidden in the private part. The two declarations represent the same
	 * component implementation.
	 */
	private void checkComponentImplementationInPackageSection(ComponentImplementation componentImplementation) {
		if (componentImplementation.getOwner() instanceof PublicPackageSection
				&& ((AadlPackage) componentImplementation.getElementRoot()).getPrivateSection() != null
				&& ((AadlPackage) componentImplementation.getElementRoot()).getPrivateSection().findNamedElement(
						componentImplementation.getName()) instanceof ComponentImplementation) {
			for (EObject child : componentImplementation.getOwnedElements()) {
				if (child instanceof ClassifierFeature && !(child instanceof ModeFeature)) {
					error("When a component implementation is declared in both the public section and the private"
							+ " section of a package, the implementation declaration in the public section can only contain a"
							+ " properties subclause and a modes subclause.", (Element) child,
							Aadl2Package.eINSTANCE.getComponentClassifier_OwnedMode());
				}
			}
		}
	}

	/**
	 * Checks that the category of the component type rename is identical to the
	 * category of the specified component type. This requirement is not in the
	 * standard yet. Peter has been informed and it should be in a future
	 * errata.
	 */
	private void checkComponentTypeRenameCategory(ComponentTypeRename componentTypeRename) {
		if (Aadl2Util.isNull(componentTypeRename.getRenamedComponentType())) {
//			error(componentTypeRename,"Component type rename reference could not be resolved.");
			return;
		}
		if (!componentTypeRename.getCategory().equals(componentTypeRename.getRenamedComponentType().getCategory())) {
			error("The category of '" + componentTypeRename.getRenamedComponentType().getQualifiedName() + "' is not "
					+ componentTypeRename.getCategory().getName(), componentTypeRename,
					Aadl2Package.eINSTANCE.getComponentTypeRename_RenamedComponentType());
		}
	}

	/**
	 * Checks legality rule 3 in section 4.3 (Component Types) on page 36. "The
	 * category of the component type being extended must match the category of
	 * the extending component type, i.e., they must be identical or the
	 * category being extended must be abstract."
	 */
	private void checkTypeExtensionCategory(TypeExtension typeExtension) {
		ComponentType parent = typeExtension.getExtended();
		ComponentType child = (ComponentType) typeExtension.getSpecific();
		if (!canExtend(parent, child))
			error("Cannot extend '" + parent.getQualifiedName() + "'.  Incompatible categories.", parent,
					Aadl2Package.eINSTANCE.getComponentType_OwnedExtension());
	}

	/**
	 * Checks legality rule 5 in section 4.3 (Component Types) on page 36.
	 * "A component type must not contain both a requires_modes_subcluase and a modes_subclause."
	 * 
	 * Checks legality rule 6 in section 4.3 (Component Types) on page 36. "If
	 * the extended component type and an ancestor component type in the extends
	 * hierarchy contain modes subclauses, they must both be
	 * requires_modes_subclause or modes_subclause."
	 */
	private void checkComponentTypeModes(ComponentType componentType) {
		boolean containsModes = false;
		boolean containsRequiresModes = false;
		if (hasExtendCycles(componentType))
			return;
		for (ComponentType currentType = componentType; currentType != null; currentType = currentType.getExtended()) {
			for (Mode currentMode : currentType.getOwnedModes()) {
				if (currentMode.isDerived())
					containsRequiresModes = true;
				else
					containsModes = true;
			}
		}
		if (containsModes && containsRequiresModes)
			error(componentType, "Component types cannot contain both modes and requires modes.");
	}

	/**
	 * Checks naming rule 6 in section 4.4 (Component Implementations) on page
	 * 41. "In a component implementation extension, the component type
	 * identifier of the component implementation being extended, which appears
	 * after the reserved word extends, must be the same as or an ancestor of
	 * the component type of the extension. The component implementation being
	 * extended may exist in another package. In this case the component
	 * implementation name is qualified with the package name."
	 */
	private void checkExtensionAndRealizationHierarchy(ImplementationExtension implementationExtension) {
		ComponentImplementation parent = implementationExtension.getExtended();
		ComponentImplementation child = (ComponentImplementation) implementationExtension.getSpecific();
		ComponentType typeOfParent = parent.getType();
		ComponentType typeOfChild = child.getType();
		boolean isAncestor = false;
		if (Aadl2Util.isNull(typeOfParent)) return;
		if (Aadl2Util.isNull(typeOfChild)) return;
		if (hasExtendCycles(typeOfChild))
			return;
		for (ComponentType currentType = typeOfChild; currentType != null && !isAncestor; currentType = currentType
				.getExtended())
			if (currentType.equals(typeOfParent))
				isAncestor = true;
		if (!isAncestor)
			error(implementationExtension, '\'' + typeOfParent.getQualifiedName() + "' is not an ancestor of '"
					+ typeOfChild.getQualifiedName() + "'.");
	}

	/**
	 * Checks legality rule 3 in section 4.4 (Component Implementations) on page
	 * 42. "The category of the component implementation must be identical to
	 * the category of the component type for which the component implementation
	 * is declared."
	 */
	private void checkRealizationCategory(Realization realization) {
		ComponentType type = realization.getImplemented();
		if (Aadl2Util.isNull(type)) return; // unresolved type. has been reported already as such. no need to check category 
		ComponentImplementation implementation = (ComponentImplementation) realization.getSpecific();
		if (!type.getCategory().equals(implementation.getCategory()))
			error(realization,
					"The category of '" + type.getQualifiedName() + "' is not " + implementation.getCategory() + '.');
	}

	/**
	 * Checks legality rule 4 in section 4.4 (Component Implementation) on page
	 * 42. "If the component implementation extends another component
	 * implementation, the category of both must match, i.e., they must be
	 * identical or the category being extended must be abstract."
	 */
	private void checkImplementationExtensionCategory(ImplementationExtension implementationExtension) {
		ComponentImplementation parent = implementationExtension.getExtended();
		ComponentImplementation child = (ComponentImplementation) implementationExtension.getSpecific();
		if (!canExtend(parent, child))
			error(implementationExtension, "Cannot extend '" + parent.getQualifiedName()
					+ "'.  Incompatible categories.");
	}

	/**
	 * Checks legality rule 6 in section 4.4 (Component Implementations) on page
	 * 42. "If the component type of the component implementation contains a
	 * requires_modes_subclause then the component implementation must not
	 * contain any modes subclause."
	 * 
	 * Checks legality rule 7 in section 4.4 (Component Implementations) on page
	 * 42. "If modes are declared in the component type, then modes cannot be
	 * declared in component implementations."
	 */
	private void checkComponentImplementationModes(ComponentImplementation componentImplementation) {
		if (!componentImplementation.getOwnedModes().isEmpty()) {
			boolean typeHasModes = false;
			if (hasExtendCycles(componentImplementation.getType()))
				return;
			for (ComponentType currentType = componentImplementation.getType(); currentType != null && !typeHasModes; currentType = currentType
					.getExtended()) {
				if (!currentType.getOwnedModes().isEmpty())
					typeHasModes = true;
			}
			if (typeHasModes) {
				error(componentImplementation,
						"Implementation cannot contain modes because modes or requires modes are inherited from the type.");
			}
		}
	}

	/**
	 * Checks legality rule 9 in section 4.4 (Component Implementations) on page
	 * 42. "The category of a subcomponent being refined must match the category
	 * of the refining subcomponent declaration, i.e., they must be identical or
	 * the category being refined must be abstract."
	 * 
	 * Checks legality rule 3 in section 4.5 (Subcomponents) on page 47. "In a
	 * subcomponent refinement declaration the component category may be refined
	 * from abstract to one of the concrete component categories. Otherwise the
	 * category must be the same as that of the subcomponent being refined."
	 */
	private void checkSubcomponentRefinementCategory(Subcomponent subcomponent) {
		ComponentCategory subcomponentCategory = subcomponent.getCategory();
		if (subcomponent.getRefined() != null) {
			ComponentCategory refinedCategory = subcomponent.getRefined().getCategory();
			if (!subcomponentCategory.equals(refinedCategory) && !refinedCategory.equals(ComponentCategory.ABSTRACT))
				error(subcomponent, "Cannot refine subcomponent.  Incompatible categories.");
		}
	}

	/**
	 * Checks legality rule 1 in section 4.5 (Subcomponents) on page 47. "The
	 * category of the subcomponent declaration must match the category of its
	 * corresponding component classifier reference or its prototype reference,
	 * i.e., they must be identical, or in the case of a classifier reference
	 * the referenced classifier category may be abstract."
	 */
	private void checkSubcomponentCategory(Subcomponent subcomponent) {
		ComponentCategory subcomponentCategory = subcomponent.getCategory();
		ComponentClassifier componentClassifier = subcomponent.getClassifier();
		ComponentPrototype componentPrototype = subcomponent.getPrototype();
		if (componentClassifier != null) {
			if (!subcomponentCategory.equals(componentClassifier.getCategory())
					&& !componentClassifier.getCategory().equals(ComponentCategory.ABSTRACT)) {
				error(subcomponent,
						"The category of the subcomponent is incompatible with the category of the classifier");
			}
		} else if (componentPrototype != null) {
			if (!subcomponentCategory.equals(getComponentPrototypeCategory(componentPrototype)))
				error(subcomponent,
						"The category of the subcomponent is incompatible with the category of the prototype");
		}
	}

	/**
	 * Checks legality rule 7 in section 4.6 (Abstract Components) on page 53.
	 * "If an abstract component type is refined to a concrete category, the
	 * features of the abstract component type must be acceptable for the
	 * concrete component type."
	 */
	private void checkFeaturesOfExtendedAbstractType(ComponentType componentType) {
		ComponentCategory typeCategory = null;
		if (componentType instanceof AbstractType)
			return;
		else if (componentType instanceof DataType)
			typeCategory = ComponentCategory.DATA;
		else if (componentType instanceof SubprogramType)
			typeCategory = ComponentCategory.SUBPROGRAM;
		else if (componentType instanceof SubprogramGroupType)
			typeCategory = ComponentCategory.SUBPROGRAM_GROUP;
		else if (componentType instanceof ThreadType)
			typeCategory = ComponentCategory.THREAD;
		else if (componentType instanceof ThreadGroupType)
			typeCategory = ComponentCategory.THREAD_GROUP;
		else if (componentType instanceof ProcessType)
			typeCategory = ComponentCategory.PROCESS;
		else if (componentType instanceof ProcessorType)
			typeCategory = ComponentCategory.PROCESSOR;
		else if (componentType instanceof VirtualProcessorType)
			typeCategory = ComponentCategory.VIRTUAL_PROCESSOR;
		else if (componentType instanceof MemoryType)
			typeCategory = ComponentCategory.MEMORY;
		else if (componentType instanceof BusType)
			typeCategory = ComponentCategory.BUS;
		else if (componentType instanceof VirtualBusType)
			typeCategory = ComponentCategory.VIRTUAL_BUS;
		else if (componentType instanceof DeviceType)
			typeCategory = ComponentCategory.DEVICE;
		else if (componentType instanceof SystemType)
			typeCategory = ComponentCategory.SYSTEM;
		Set<FeatureType> acceptableFeatureTypes = acceptableFeaturesForTypes.get(typeCategory);
		HashSet<FeatureType> typesOfInheritedFeatures = new HashSet<FeatureType>();
		if (hasExtendCycles(componentType))
			return;
		for (ComponentType parent = componentType.getExtended(); parent instanceof AbstractType; parent = parent
				.getExtended())
			for (Feature feature : parent.getOwnedFeatures())
				typesOfInheritedFeatures.add(getFeatureType(feature));
		for (FeatureType featureType : typesOfInheritedFeatures) {
			if (!acceptableFeatureTypes.contains(featureType)) {
				error(componentType.getOwnedExtension(),
						"A " + typeCategory.getName() + " type cannot extend an abstract type that contains "
								+ featureType.getNameWithIndefiniteArticle() + '.');
			}
		}
	}

	/**
	 * Checks legality rule 8 in section 4.6 (Abstract Components) on page 53.
	 * "If an abstract component implementation is refined to a concrete
	 * category, the subcomponents of the abstract component implementation must
	 * be acceptable for the concrete component implementation."
	 */
	private void checkSubcomponentsOfExtendedAbstractImplementation(ComponentImplementation componentImplementation) {
		ComponentCategory implementationCategory = null;
		if (componentImplementation instanceof AbstractImplementation)
			return;
		else if (componentImplementation instanceof DataImplementation)
			implementationCategory = ComponentCategory.DATA;
		else if (componentImplementation instanceof SubprogramImplementation)
			implementationCategory = ComponentCategory.SUBPROGRAM;
		else if (componentImplementation instanceof SubprogramGroupImplementation)
			implementationCategory = ComponentCategory.SUBPROGRAM_GROUP;
		else if (componentImplementation instanceof ThreadImplementation)
			implementationCategory = ComponentCategory.THREAD;
		else if (componentImplementation instanceof ThreadGroupImplementation)
			implementationCategory = ComponentCategory.THREAD_GROUP;
		else if (componentImplementation instanceof ProcessImplementation)
			implementationCategory = ComponentCategory.PROCESS;
		else if (componentImplementation instanceof ProcessorImplementation)
			implementationCategory = ComponentCategory.PROCESSOR;
		else if (componentImplementation instanceof VirtualProcessorImplementation)
			implementationCategory = ComponentCategory.VIRTUAL_PROCESSOR;
		else if (componentImplementation instanceof MemoryImplementation)
			implementationCategory = ComponentCategory.MEMORY;
		else if (componentImplementation instanceof BusImplementation)
			implementationCategory = ComponentCategory.BUS;
		else if (componentImplementation instanceof VirtualBusImplementation)
			implementationCategory = ComponentCategory.VIRTUAL_BUS;
		else if (componentImplementation instanceof DeviceImplementation)
			implementationCategory = ComponentCategory.DEVICE;
		else if (componentImplementation instanceof SystemImplementation)
			implementationCategory = ComponentCategory.SYSTEM;
		Set<ComponentCategory> acceptableSubcomponentCategories = acceptableSubcomponentCategoriesForImplementations
				.get(implementationCategory);
		HashSet<ComponentCategory> categoriesOfInheritedSubcomponents = new HashSet<ComponentCategory>();
		if (hasExtendCycles(componentImplementation))
			return;
		for (ComponentImplementation parent = componentImplementation.getExtended(); parent instanceof AbstractImplementation; parent = parent
				.getExtended()) {
			for (Subcomponent subcomponent : parent.getOwnedSubcomponents())
				categoriesOfInheritedSubcomponents.add(subcomponent.getCategory());
		}
		for (ComponentCategory subcomponentCategory : categoriesOfInheritedSubcomponents) {
			if (!acceptableSubcomponentCategories.contains(subcomponentCategory)) {
				error(componentImplementation.getOwnedExtension(), "A " + implementationCategory.getName()
						+ " implementation cannot extend an abstract implementation that contains a "
						+ subcomponentCategory.getName() + " subcomponent.");
			}
		}
	}

	/**
	 * Checks legality rule 7 in section 4.6 (Abstract Components) on page 53.
	 * "If an abstract component type is refined to a concrete category, the
	 * features of the abstract component type must be acceptable for the
	 * concrete component type."
	 * 
	 * Checks legality rule 8 in section 4.6 (Abstract Components) on page 53.
	 * "If an abstract component implementation is refined to a concrete
	 * category, the subcomponents of the abstract component implementation must
	 * be acceptable for the concrete component implementation."
	 * Note: this also covers rule L5 in section 4.6.
	 */
	private void checkSubcomponentsHierarchy(Subcomponent subcomponent) {
		if (subcomponent.getCategory().equals(ComponentCategory.ABSTRACT))
			return;
		Subcomponent refinedSubcomponent = subcomponent;
		while (refinedSubcomponent.getClassifier() == null && refinedSubcomponent.getPrototype() == null
				&& refinedSubcomponent.getRefined() != null) {
			refinedSubcomponent = refinedSubcomponent.getRefined();
		}
		if (refinedSubcomponent.getClassifier() instanceof AbstractImplementation) {
			Set<ComponentCategory> acceptableSubcomponentCategories = acceptableSubcomponentCategoriesForImplementations
					.get(subcomponent.getCategory());
			HashSet<ComponentCategory> categoriesOfNestedSubcomponents = new HashSet<ComponentCategory>();
			if (hasExtendCycles(refinedSubcomponent.getClassifier()))
				return;
			for (ComponentImplementation impl = (ComponentImplementation) refinedSubcomponent.getClassifier(); impl instanceof AbstractImplementation; impl = impl
					.getExtended()) {
				for (Subcomponent nestedSubcomponent : impl.getOwnedSubcomponents())
					categoriesOfNestedSubcomponents.add(nestedSubcomponent.getCategory());
			}
			for (ComponentCategory nestedSubcomponentCategory : categoriesOfNestedSubcomponents) {
				if (!acceptableSubcomponentCategories.contains(nestedSubcomponentCategory)) {
					error(subcomponent, "A " + subcomponent.getCategory().getName()
							+ " subcomponent cannot refer to an abstract implementation that contains a "
							+ nestedSubcomponentCategory.getName() + " subcomponent.");
				}
			}
		}
		AbstractType abstractType;
		if (refinedSubcomponent.getClassifier() instanceof AbstractType)
			abstractType = (AbstractType) refinedSubcomponent.getClassifier();
		else if (refinedSubcomponent.getClassifier() instanceof AbstractImplementation &&
		// This second part of this if expression will not be false for
		// semantically correct models.
				((AbstractImplementation) refinedSubcomponent.getClassifier()).getOwnedRealization().getImplemented() instanceof AbstractType) {
			abstractType = ((AbstractImplementation) refinedSubcomponent.getClassifier()).getType();
		} else
			abstractType = null;
		if (abstractType != null) {
			Set<FeatureType> acceptableFeatureTypes = acceptableFeaturesForTypes.get(subcomponent.getCategory());
			HashSet<FeatureType> typesOfNestedFeatures = new HashSet<FeatureType>();
			if (hasExtendCycles(abstractType))
				return;
			for (ComponentType type = abstractType; type instanceof AbstractType; type = type.getExtended()) {
				for (Feature nestedFeature : type.getOwnedFeatures())
					typesOfNestedFeatures.add(getFeatureType(nestedFeature));
			}
			for (FeatureType nestedFeatureType : typesOfNestedFeatures) {
				if (!acceptableFeatureTypes.contains(nestedFeatureType)) {
					error(subcomponent,
							"A " + subcomponent.getCategory().getName()
									+ " subcomponent cannot refer to an abstract type that contains "
									+ nestedFeatureType.getNameWithIndefiniteArticle() + '.');
				}
			}
		}
	}

	/**
	 * Checks that the category of the prototype is identical to the category of
	 * the specified component classifier. 
	 * Rule L2 in section 4.7 (revised AADLV2)
	 * "The component category of the optional component classifier reference in the component prototype declaration must match
	 * the category in the prototype declaration."
	 */
	private void checkComponentPrototypeCategory(ComponentPrototype prototype) {
		if (prototype.getConstrainingClassifier() != null
				&& !getComponentPrototypeCategory(prototype)
						.equals(prototype.getConstrainingClassifier().getCategory())) {
			error(prototype, "The category of '" + prototype.getConstrainingClassifier().getQualifiedName()
					+ "' is not " + getComponentPrototypeCategory(prototype).getName());
		}
	}

	/**
	 * Checks legality rule 1 in section 4.7 (Prototypes) on page 56. "The
	 * component category declared in the component prototype binding must match
	 * the component category of the prototype, or the declared category component category
	 * of the prototype must be abstract." 
	 */
	private void checkComponentPrototypeBindingCategory(ComponentPrototypeBinding binding) {
		if (binding.getFormal() instanceof ComponentPrototype) {
			ComponentCategory formalCategory = getComponentPrototypeCategory((ComponentPrototype) binding.getFormal());
			if (!formalCategory.equals(ComponentCategory.ABSTRACT)) {
				for (ComponentPrototypeActual actual : binding.getActuals()) {
					if (!formalCategory.equals(actual.getCategory())) {
						error(actual,
								"The category of the formal prototype is not compatible with the category specified in the"
										+ " prototype binding.");
					}
				}
			}
		}
	}

	/**
	 * Checks legality rule 10 in section 4.7 (Prototypes) on page 56. "(L10)	The component category of the classifier reference 
	 * or prototype reference in a prototype binding declaration must match the category of the prototype."
	 */
	private void checkComponentPrototypeActualComponentCategory(ComponentPrototypeActual actual) {
		if (actual.eIsProxy()) {
			error(actual, "The prototype actual could not be found.");
		} else {
			SubcomponentType st = actual.getSubcomponentType();
			if (st == null) {
				error(actual, "The classifier or prototype of the prototype actual could not be found.");
			} else if (!actual.getCategory().equals(ComponentCategory.ABSTRACT)
					&& !actual.getCategory().equals(
							st instanceof ComponentClassifier ? ((ComponentClassifier) st).getCategory()
									: getComponentPrototypeCategory((ComponentPrototype) st))) {
				error(actual,
						"The category of the referenced classifier is not compatible the category specified in the prototype binding.");
			}
		}
	}

	/**
	 * Checks legality rule 6 in section 4.7 (Prototypes) on page 56. "If the
	 * direction is declared for feature prototypes, then the prototype actual
	 * satisfies the direction according to the same rules as for feature
	 * refinements (see Section 8); in the case of ports the direction must be
	 * in or out; in the case of data access, the access right must be read-only
	 * for in and write-only for out; in the case of bus access, subprogram
	 * access and subprogram group access the direction must not be in nor out."
	 * Peter is going to change the wording of this to make parts of it less
	 * restrictive.
	 * 
	 * Checks legality rule 11 in section 4.7 (Prototypes) on page 57.
	 * "(L11)	If a direction is specified for an abstract feature in a prototype declaration, 
	 * then the direction of the prototype actual must match that declared in the prototype."
	 */
	private void checkFeaturePrototypeBindingDirection(FeaturePrototypeBinding binding) {
		if (binding.getFormal() instanceof FeaturePrototype) {
			DirectionType formalDirection = ((FeaturePrototype) binding.getFormal()).getDirection();
			if (formalDirection != null && !formalDirection.equals(DirectionType.IN_OUT)
					&& !(binding.getActual() instanceof AccessSpecification)) {
				DirectionType actualDirection;
				if (binding.getActual() instanceof FeaturePrototypeReference)
					actualDirection = ((FeaturePrototypeReference) binding.getActual()).getDirection();
				else
					actualDirection = ((PortSpecification) binding.getActual()).getDirection();
				if (!formalDirection.equals(DirectionType.IN_OUT)
						&& !formalDirection.equals(actualDirection)) {
					error(binding.getActual(),
							"The direction specified in the binding is inconsistent with the direction of the formal prototype.");
				}
			}
		}
	}

	/**
	 * Checks that the formal prototype of a ComponentPrototypeBinding is a
	 * ComponentPrototype. Rule L12 in Section 4.7.
	 */
	private void checkFormalOfComponentPrototypeBinding(ComponentPrototypeBinding binding) {
		Prototype formal = binding.getFormal();
		// formal might be null or a proxy if it could not be resolved.
		// in that case that is already reported.
		if (!Aadl2Util.isNull(formal) && !(binding.getFormal() instanceof ComponentPrototype))
			error(binding, '\'' + binding.getFormal().getName() + "' is not a component prototype.");
	}

	/**
	 * Checks that the formal prototype of a FeatureGroupPrototypeBinding is a
	 * FeatureGroupPrototype. Rule L12 in Section 4.7.
	 */
	private void checkFormalOfFeatureGroupPrototypeBinding(FeatureGroupPrototypeBinding binding) {
		if (!(binding.getFormal() instanceof FeatureGroupPrototype))
			error(binding, '\'' + binding.getFormal().getName() + "' is not a feature group prototype.");
	}

	/**
	 * Checks that the formal prototype of a FeaturePrototypeBinding is a
	 * FeaturePrototype. Rule L12 in Section 4.7.
	 */
	private void checkFormalOfFeaturePrototypeBinding(FeaturePrototypeBinding binding) {
		if (!(binding.getFormal() instanceof FeaturePrototype))
			error(binding, '\'' + binding.getFormal().getName() + "' is not a feature prototype.");
	}

	/**
	 * Checks that component prototype refinements only refine component
	 * prototypes. Rule L13 in Section 4.7.
	 */
	private void checkRefinedOfComponentPrototype(ComponentPrototype prototype) {
		if (prototype.getRefined() != null && !(prototype.getRefined() instanceof ComponentPrototype))
			error(prototype, '\'' + prototype.getName() + "' is not a component prototype.");
	}

	/**
	 * Checks that feature group prototype refinements only refine feature group
	 * prototypes. Rule L12 in Section 4.7.
	 */
	private void checkRefinedOfFeatureGroupPrototype(FeatureGroupPrototype prototype) {
		if (prototype.getRefined() != null && !(prototype.getRefined() instanceof FeatureGroupPrototype))
			error(prototype, '\'' + prototype.getName() + "' is not a feature group prototype.");
	}

	/**
	 * Checks that feature prototype refinements only refine feature prototypes.
	 * Rule L12 in Section 4.7.
	 */
	private void checkRefinedOfFeaturePrototype(FeaturePrototype prototype) {
		if (prototype.getRefined() != null && !(prototype.getRefined() instanceof FeaturePrototype))
			error(prototype, '\'' + prototype.getName() + "' is not a feature prototype.");
	}

	/**
	 * Checks semantic rule 6 in section 4.7 (Prototypes) on page 57. "A
	 * prototype refinement can increase the constraints on classifiers to be
	 * supplied. The newly specified category, classifier, and array dimensions
	 * must satisfy the same matching rules as the prototype bindings." This
	 * method checks the component category only.
	 */
	private void checkCategoryOfRefinedComponentPrototype(ComponentPrototype prototype) {
		if (prototype.getRefined() != null && prototype.getRefined() instanceof ComponentPrototype) {
			ComponentCategory refinedPrototypeCategory = getComponentPrototypeCategory((ComponentPrototype) prototype
					.getRefined());
			if (!refinedPrototypeCategory.equals(ComponentCategory.ABSTRACT)
					&& !refinedPrototypeCategory.equals(getComponentPrototypeCategory(prototype)))
				error(prototype, "Incompatible category for prototype refinement.");
		}
	}

	/**
	 * Checks semantic rule 6 in section 4.7 (Prototypes) on page 57. "A
	 * prototype refinement can increase the constraints on classifiers to be
	 * supplied. The newly specified category, classifier, and array dimensions
	 * must satisfy the same matching rules as the prototype bindings." This
	 * method checks for array compatibility only.
	 */
	private void checkArrayOfRefinedComponentPrototype(ComponentPrototype prototype) {
		if (prototype.getRefined() != null && prototype.getRefined() instanceof ComponentPrototype) {
			ComponentPrototype refinedPrototype = (ComponentPrototype) prototype.getRefined();
			if (refinedPrototype.isArray() && !prototype.isArray())
				error(prototype, "Prototype must be an array because the refined prototype is an array.");
		}
	}

	/**
	 * Checks semantic rule 6 in section 4.7 (Prototypes) on page 57. "A
	 * prototype refinement can increase the constraints on classifiers to be
	 * supplied. The newly specified category, classifier, and array dimensions
	 * must satisfy the same matching rules as the prototype bindings." This
	 * method checks for feature direction only.
	 */
	private void checkDirectionOfRefinedFeaturePrototype(FeaturePrototype prototype) {
		if (prototype.getRefined() != null && prototype.getRefined() instanceof FeaturePrototype) {
			DirectionType refinedPrototypeDirection = ((FeaturePrototype) prototype.getRefined()).getDirection();
			if (refinedPrototypeDirection != null && !refinedPrototypeDirection.equals(DirectionType.IN_OUT)
					&& !refinedPrototypeDirection.equals(prototype.getDirection())) {
				error(prototype, "Incompatible direction for prototype refinement.");
			}
		}
	}

	/**
	 * Checks legality rule 2 in section 5.1 (Data) on page 62.
	 * "A data type declaration must not contain a flow specification or modes subclause."
	 * This rule is partly checked by the parser. This method checks for
	 * inherited members from an AbstractType.
	 */
	private void checkForInheritedFlowsAndModesFromAbstractType(DataType dataType) {
		boolean parentHasFlowSpecs = false;
		boolean parentHasModes = false;
		boolean parentHasModeTransitions = false;
		if (hasExtendCycles(dataType))
			return;
		for (ComponentType parentType = dataType.getExtended(); parentType instanceof AbstractType; parentType = parentType
				.getExtended()) {
			if (!parentType.getOwnedFlowSpecifications().isEmpty())
				parentHasFlowSpecs = true;
			if (!parentType.getOwnedModes().isEmpty())
				parentHasModes = true;
			if (!parentType.getOwnedModeTransitions().isEmpty())
				parentHasModeTransitions = true;
		}
		if (parentHasFlowSpecs)
			error(dataType.getOwnedExtension(),
					"A data type cannot extend an abstract type that contains a flow specification.");
		if (parentHasModes)
			error(dataType.getOwnedExtension(), "A data type cannot extend an abstract type that contains modes.");
		if (parentHasModeTransitions)
			error(dataType.getOwnedExtension(),
					"A data type cannot extend an abstract type that contains a mode transition.");
	}

	/**
	 * Checks legality rule 4 in section 5.1 (Data) on page 62. "A data
	 * implementation must not contain a flow implementation, an end-to-end flow
	 * specification, or a modes subclause." This rule is partly checked by the
	 * parser. This method checks for inherited members from an
	 * AbstractImplementation.
	 */
	private void checkForInheritedFlowsAndModesFromAbstractImplementation(DataImplementation dataImplementation) {
		boolean parentHasFlowImpl = false;
		boolean parentHasETEFlow = false;
		boolean parentHasModes = false;
		boolean parentHasModeTransition = false;
		if (hasExtendCycles(dataImplementation))
			return;
		for (ComponentImplementation parentImplementation = dataImplementation.getExtended(); parentImplementation instanceof AbstractImplementation; parentImplementation = parentImplementation
				.getExtended()) {
			if (!parentImplementation.getOwnedFlowImplementations().isEmpty())
				parentHasFlowImpl = true;
			if (!parentImplementation.getOwnedEndToEndFlows().isEmpty())
				parentHasETEFlow = true;
			if (!parentImplementation.getOwnedModes().isEmpty())
				parentHasModes = true;
			if (!parentImplementation.getOwnedModeTransitions().isEmpty())
				parentHasModeTransition = true;
		}
		if (parentHasFlowImpl) {
			error(dataImplementation.getOwnedExtension(),
					"A data implementation cannot extend an abstract implementation that contains a flow implementation.");
		}
		if (parentHasETEFlow) {
			error(dataImplementation.getOwnedExtension(),
					"A data implementation cannot extend an abstract implementation that contains an end to end flow.");
		}
		if (parentHasModes) {
			error(dataImplementation.getOwnedExtension(),
					"A data implementation cannot extend an abstract implementation that contains modes.");
		}
		if (parentHasModeTransition) {
			error(dataImplementation.getOwnedExtension(),
					"A data implementation cannot extend an abstract implementation that contains a mode transition.");
		}
	}

	/**
	 * Checks legality rule 4 in section 5.5 (Thread Groups) on page 95.
	 * "A thread group must not contain a subprogam calls subclause." This rule
	 * is partly checked by the parser. This method checks for inherited members
	 * from an AbstractImplementation.
	 */
	private void checkForInheritedCallSequenceFromAbstractImplementation(
			ThreadGroupImplementation threadGroupImplementation) {
		boolean parentHasCallSequence = false;
		if (hasExtendCycles(threadGroupImplementation))
			return;
		for (ComponentImplementation parentImplementation = threadGroupImplementation.getExtended(); parentImplementation instanceof AbstractImplementation; parentImplementation = parentImplementation
				.getExtended()) {
			if (!((AbstractImplementation) parentImplementation).getOwnedSubprogramCallSequences().isEmpty())
				parentHasCallSequence = true;
		}
		if (parentHasCallSequence) {
			error(threadGroupImplementation.getOwnedExtension(),
					"A thread group implementation cannot extend an abstract implementation that contains subprogram calls.");
		}
	}

	/**
	 * Checks legality rule 5 in section 6.1 (Processors) on page 102.
	 * "A processor implementation must not contain a subprogram calls subclause."
	 * This rule is partly checked by the parser. This method checks for
	 * inherited members from an AbstractImplementation.
	 */
	private void checkForInheritedCallSequenceFromAbstractImplementation(ProcessorImplementation processorImplementation) {
		boolean parentHasCallSequence = false;
		if (hasExtendCycles(processorImplementation))
			return;
		for (ComponentImplementation parentImplementation = processorImplementation.getExtended(); parentImplementation instanceof AbstractImplementation; parentImplementation = parentImplementation
				.getExtended()) {
			if (!((AbstractImplementation) parentImplementation).getOwnedSubprogramCallSequences().isEmpty())
				parentHasCallSequence = true;
		}
		if (parentHasCallSequence) {
			error(processorImplementation.getOwnedExtension(),
					"A processor implementation cannot extend an abstract implementation that contains subprogram calls.");
		}
	}

	/**
	 * Checks legality rule 4 in section 6.2 (Virtual Processors) on page 106.
	 * "A virtual processor implementation must not contain a subprogram calls subclause."
	 * This rule is partly checked by the parser. This method checks for
	 * inherited members from an AbstractImplementation.
	 */
	private void checkForInheritedCallSequenceFromAbstractImplementation(
			VirtualProcessorImplementation virtualProcessorImplementation) {
		boolean parentHasCallSequence = false;
		if (hasExtendCycles(virtualProcessorImplementation))
			return;
		for (ComponentImplementation parentImplementation = virtualProcessorImplementation.getExtended(); parentImplementation instanceof AbstractImplementation; parentImplementation = parentImplementation
				.getExtended()) {
			if (!((AbstractImplementation) parentImplementation).getOwnedSubprogramCallSequences().isEmpty())
				parentHasCallSequence = true;
		}
		if (parentHasCallSequence) {
			error(virtualProcessorImplementation.getOwnedExtension(),
					"A virtual processor implementation cannot extend an abstract implementation that contains subprogram calls.");
		}
	}

	/**
	 * Checks legality rule 1 in section 6.3 (Memory) on page 109. "A memory
	 * type can contain bus access declarations, feature groups, a modes
	 * subclause, and property associations. It must not contain flow
	 * specifications." This rule is partly checked by the parser. This method
	 * checks for inherited members from an AbstractType.
	 */
	private void checkForInheritedFlowsFromAbstractType(MemoryType memoryType) {
		boolean parentHasFlowSpec = false;
		if (hasExtendCycles(memoryType))
			return;
		for (ComponentType parentType = memoryType.getExtended(); parentType instanceof AbstractType; parentType = parentType
				.getExtended())
			if (!parentType.getOwnedFlowSpecifications().isEmpty())
				parentHasFlowSpec = true;
		if (parentHasFlowSpec)
			error(memoryType.getOwnedExtension(),
					"A memory type cannot extend an abstract type that contains a flow specification.");
	}

	/**
	 * Checks legality rule 5 in section 6.3 (Memory) on page 110.
	 * "A memory implementation must not contain flows subclause, or subprogram calls subclause."
	 * This rule is partly checked by the parser. This method checks for
	 * inherited members from an AbstractImplementation.
	 */
	private void checkForInheritedFlowsAndCallSequenceFromAbstractImplementation(
			MemoryImplementation memoryImplementation) {
		boolean parentHasFlowImpl = false;
		boolean parentHasETEFlow = false;
		boolean parentHasCallSequence = false;
		if (hasExtendCycles(memoryImplementation))
			return;
		for (ComponentImplementation parentImplementation = memoryImplementation.getExtended(); parentImplementation instanceof AbstractImplementation; parentImplementation = parentImplementation
				.getExtended()) {
			if (!parentImplementation.getOwnedFlowImplementations().isEmpty())
				parentHasFlowImpl = true;
			if (!parentImplementation.getOwnedEndToEndFlows().isEmpty())
				parentHasETEFlow = true;
			if (!((AbstractImplementation) parentImplementation).getOwnedSubprogramCallSequences().isEmpty())
				parentHasCallSequence = true;
		}
		if (parentHasFlowImpl) {
			error(memoryImplementation.getOwnedExtension(),
					"A memory implementation cannot extend an abstract implementation that contains a flow implementation.");
		}
		if (parentHasETEFlow) {
			error(memoryImplementation.getOwnedExtension(),
					"A memory implementation cannot extend an abstract implementation that contains an end to end flow.");
		}
		if (parentHasCallSequence) {
			error(memoryImplementation.getOwnedExtension(),
					"A memory implementation cannot extend an abstract implementation that contains subprogram calls.");
		}
	}

	/**
	 * Checks legality rule 2 in section 6.4 (Buses) on page 111.
	 * "A bus type must not contain any flow specifications." This rule is
	 * partly checked by the parser. This method checks for inherited members
	 * from an AbstractType.
	 */
	private void checkForInheritedFlowsFromAbstractType(BusType busType) {
		boolean parentHasFlowSpec = false;
		if (hasExtendCycles(busType))
			return;
		for (ComponentType parentType = busType.getExtended(); parentType instanceof AbstractType; parentType = parentType
				.getExtended())
			if (!parentType.getOwnedFlowSpecifications().isEmpty())
				parentHasFlowSpec = true;
		if (parentHasFlowSpec)
			error(busType.getOwnedExtension(),
					"A bus type cannot extend an abstract type that contains a flow specification.");
	}

	/**
	 * Checks legality rule 5 in section 6.4 (Buses) on page 111. "A bus
	 * implementation must not contain a connections subclause, flows subclause,
	 * or subprogram calls subclause." This rule is partly checked by the
	 * parser. This method checks for inherited members from an
	 * AbstractImplementation.
	 */
	private void checkForInheritedConnectionsFlowsAndCallsFromAbstractImplementation(BusImplementation busImplementation) {
		boolean parentHasConnections = false;
		boolean parentHasFlowImpl = false;
		boolean parentHasETEFlow = false;
		boolean parentHasCallSequence = false;
		if (hasExtendCycles(busImplementation))
			return;
		for (ComponentImplementation parentImplementation = busImplementation.getExtended(); parentImplementation instanceof AbstractImplementation; parentImplementation = parentImplementation
				.getExtended()) {
			if (!parentImplementation.getOwnedConnections().isEmpty())
				parentHasConnections = true;
			if (!parentImplementation.getOwnedFlowImplementations().isEmpty())
				parentHasFlowImpl = true;
			if (!parentImplementation.getOwnedEndToEndFlows().isEmpty())
				parentHasETEFlow = true;
			if (!((AbstractImplementation) parentImplementation).getOwnedSubprogramCallSequences().isEmpty())
				parentHasCallSequence = true;
		}
		if (parentHasConnections) {
			error(busImplementation.getOwnedExtension(),
					"A bus implementation cannot extend an abstract implementation that contains a connection.");
		}
		if (parentHasFlowImpl) {
			error(busImplementation.getOwnedExtension(),
					"A bus implementation cannot extend an abstract implementation that contains a flow implementation.");
		}
		if (parentHasETEFlow) {
			error(busImplementation.getOwnedExtension(),
					"A bus implementation cannot extend an abstract implementation that contains an end to end flow.");
		}
		if (parentHasCallSequence) {
			error(busImplementation.getOwnedExtension(),
					"A bus implementation cannot extend an abstract implementation that contains subprogram calls.");
		}
	}

	/**
	 * Checks legality rule 2 in section 6.5 (Virtual Buses) on page 114.
	 * "A virtual bus type must not contain flow specifications." This rule is
	 * partly checked by the parser. This method checks for inhertied members
	 * from an AbstractType.
	 */
	private void checkForInheritedFlowsFromAbstractType(VirtualBusType virtualBusType) {
		boolean parentHasFlowSpec = false;
		if (hasExtendCycles(virtualBusType))
			return;
		for (ComponentType parentType = virtualBusType.getExtended(); parentType instanceof AbstractType; parentType = parentType
				.getExtended()) {
			if (!parentType.getOwnedFlowSpecifications().isEmpty())
				parentHasFlowSpec = true;
		}
		if (parentHasFlowSpec) {
			error(virtualBusType.getOwnedExtension(),
					"A virtual bus type cannot extend an abstract type that contains a flow specification.");
		}
	}

	/**
	 * Checks legality rule 5 in section 6.5 (Virtual Buses) on page 114. "A
	 * virtual bus implementation must not contain a connections subclause,
	 * flows subclause, or subprogram calls subclause." This rule is partly
	 * checked by the parser. This method checks for inherited members from an
	 * AbstractImplementation.
	 */
	private void checkForInheritedConnectionsFlowsAndCallsFromAbstractImplementation(
			VirtualBusImplementation virtualBusImplementation) {
		boolean parentHasConnections = false;
		boolean parentHasFlowImpl = false;
		boolean parentHasETEFlow = false;
		boolean parentHasCallSequence = false;
		if (hasExtendCycles(virtualBusImplementation))
			return;
		for (ComponentImplementation parentImplementation = virtualBusImplementation.getExtended(); parentImplementation instanceof AbstractImplementation; parentImplementation = parentImplementation
				.getExtended()) {
			if (!parentImplementation.getOwnedConnections().isEmpty())
				parentHasConnections = true;
			if (!parentImplementation.getOwnedFlowImplementations().isEmpty())
				parentHasFlowImpl = true;
			if (!parentImplementation.getOwnedEndToEndFlows().isEmpty())
				parentHasETEFlow = true;
			if (!((AbstractImplementation) parentImplementation).getOwnedSubprogramCallSequences().isEmpty())
				parentHasCallSequence = true;
		}
		if (parentHasConnections) {
			error(virtualBusImplementation.getOwnedExtension(),
					"A virtual bus implementation cannot extend an abstract implementation that contains a connection.");
		}
		if (parentHasFlowImpl) {
			error(virtualBusImplementation.getOwnedExtension(),
					"A virtual bus implementation cannot extend an abstract implementation that contains a flow implementation.");
		}
		if (parentHasETEFlow) {
			error(virtualBusImplementation.getOwnedExtension(),
					"A virtual bus implementation cannot extend an abstract implementation that contains an end to end flow.");
		}
		if (parentHasCallSequence) {
			error(virtualBusImplementation.getOwnedExtension(),
					"A virtual bus implementation cannot extend an abstract implementation that contains subprogram calls.");
		}
	}

	/**
	 * Checks legality rule 2 in section 6.6 (Devices) on page 117.
	 * "A device component implementation must not contain a subprogram calls subclause."
	 * This rule is partly checked by the parser. This method checks for
	 * inherited members from an AbstractImplementation.
	 */
	private void checkForInheritedCallsFromAbstractImplementation(DeviceImplementation deviceImplementation) {
		boolean parentHasCallSequence = false;
		if (hasExtendCycles(deviceImplementation))
			return;
		for (ComponentImplementation parentImplementation = deviceImplementation.getExtended(); parentImplementation instanceof AbstractImplementation; parentImplementation = parentImplementation
				.getExtended()) {
			if (!((AbstractImplementation) parentImplementation).getOwnedSubprogramCallSequences().isEmpty())
				parentHasCallSequence = true;
		}
		if (parentHasCallSequence) {
			error(deviceImplementation.getOwnedExtension(),
					"A device implementation cannot extend an abstract implementation that contains subprogram calls.");
		}
	}

	/**
	 * Checks legality rule 2 in section 8 (Features and Shared Access) on page
	 * 125. "A feature refinement declaration of a feature and the original
	 * feature must both be declared as port, parameter, access feature, or
	 * feature group, or the original feature must be declared as abstract
	 * feature."
	 * 
	 * Checks legality rule 4 in section 8.3 (Ports) on page 132. "The port
	 * category of a port refinement must be the same as the category of the
	 * port being refined, or the port being refined must be an abstract
	 * feature." This method was not written with L4 specifically in mind, but
	 * it does properly check L4. This is because there are separate meta-model
	 * classes for the various port categories (DataPort, EventPort, and
	 * EventDataPort). If this data were stored in a field in Port, then this
	 * method would not be sufficient for L4.
	 */
	private void checkTypeOfFeatureRefinement(Feature feature) {
		Feature refined = feature.getRefined();
		if (!Aadl2Util.isNull(refined) && !(feature.getRefined() instanceof AbstractFeature)
				&& !feature.eClass().equals(refined.eClass())) {
			error(feature, "Cannot refine " + FEATURE_CLASS_NAMES_WITH_ARTICLE.get(refined.eClass()) + " into "
					+ FEATURE_CLASS_NAMES_WITH_ARTICLE.get(feature.eClass()) + '.');
		}
	}

	/**
	 * Checks legality rule 3 in section 8 (Features and Shared Access) on page
	 * 125.
	 * "Feature arrays must only be declared for threads, devices, and processors."
	 */
	private void checkForFeatureArrays(Feature feature) {
		Element e = feature.getOwner();
		if (e instanceof ComponentType){
			ComponentType componentType = (ComponentType) e;
			if (!(componentType instanceof AbstractType) && !(componentType instanceof ThreadType) && !(componentType instanceof DataType)&& !(componentType instanceof MemoryType)
					&& !(componentType instanceof BusType)&& !(componentType instanceof DeviceType) && !(componentType instanceof ProcessorType)) {
				if (!feature.getArrayDimensions().isEmpty()) {
					error(feature,
							"Feature arrays can only be declared for abstract, thread, device, bus, data, memory, and processor classifiers.");
				} else if (feature instanceof FeatureGroup){
					FeatureGroup fg = (FeatureGroup) feature;
					FeatureGroupType fgt = fg.getAllFeatureGroupType();
					if (containsFeatureArrays(fgt)){
						error(feature,
								"Feature group contains feature arrays. They are can only be declared for abstract, thread, device, bus, data, memory, and processor classifiers.");
					}
				}
			}
		}
	}
	
	private void checkLegalFeatureGroup(FeatureGroup fg){
		Element e = fg.getOwner();
		if (e instanceof SubprogramGroupType){
			if (containsNonSubprogramGroupFeatures(fg.getAllFeatureGroupType())){
				error(fg,
						"Feature group in Subprogram Group Type can only contain Subprogram Access, Subprogram Group Access, or Abstract features.");
			}
		} else if (e instanceof SubprogramType){
			if (containsNonSubprogramFeatures(fg.getAllFeatureGroupType())){
				error(fg,
						"Feature group in Subprogram Type can only contain Subprogram Access, Subprogram Group Access, Data Access, Abstract, Parameter, Event Port, Event Data Port features.");
			}
		}
		
	}
	
	private boolean containsFeatureArrays(FeatureGroupType fgt){
		if (Aadl2Util.isNull(fgt)) return false;
		EList<Feature> fl = fgt.getAllFeatures();
		for (Feature feature : fl) {
			if (!feature.getArrayDimensions().isEmpty()) {
				return true;
			} else if (feature instanceof FeatureGroup){
				if (containsFeatureArrays(((FeatureGroup)feature).getAllFeatureGroupType())){
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean containsNonSubprogramGroupFeatures(FeatureGroupType fgt){
		if (Aadl2Util.isNull(fgt)) return false;
		EList<Feature> fl = fgt.getAllFeatures();
		for (Feature feature : fl) {
			if (feature instanceof FeatureGroup){
				if (containsNonSubprogramGroupFeatures(((FeatureGroup)feature).getAllFeatureGroupType())){
					return true;
				} 
			} else if (!(feature instanceof SubprogramAccess || feature instanceof SubprogramGroupAccess
					|| feature instanceof AbstractFeature)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean containsNonSubprogramFeatures(FeatureGroupType fgt){
		if (Aadl2Util.isNull(fgt)) return false;
		EList<Feature> fl = fgt.getAllFeatures();
		for (Feature feature : fl) {
			if (feature instanceof FeatureGroup){
				if (containsNonSubprogramFeatures(((FeatureGroup)feature).getAllFeatureGroupType())){
					return true;
				} 
			} else if (!(feature instanceof SubprogramAccess || feature instanceof SubprogramGroupAccess
					|| feature instanceof AbstractFeature || feature instanceof Parameter
					|| feature instanceof DataAccess || feature instanceof EventPort || feature instanceof EventDataPort
					)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks legality rule 3 in section 8 (Features and Shared Access) on page
	 * 125.
	 * "Feature arrays must only be declared for threads, devices, and processors."
	 */
	private void checkForInheritedFeatureArrays(ComponentType componentType) {
		if (!(componentType instanceof AbstractType) && !(componentType instanceof ThreadType)
				&& !(componentType instanceof DeviceType) && !(componentType instanceof ProcessorType)) {
			boolean parentHasFeatureArray = false;
			if (hasExtendCycles(componentType))
				return;
			for (ComponentType parentType = componentType.getExtended(); parentType instanceof AbstractType; parentType = parentType
					.getExtended()) {
				for (Feature inheritedFeature : parentType.getOwnedFeatures())
					if (!inheritedFeature.getArrayDimensions().isEmpty())
						parentHasFeatureArray = true;
			}
			if (parentHasFeatureArray) {
				error(componentType.getOwnedExtension(), "A " + componentType.getCategory()
						+ " type cannot extend an abstract type that contains feature arrays.");
			}
		}
	}

	/**
	 * Checks legality rule 4 in section 8 (Features and Shared Access) on page
	 * 125. "If the feature refinement specifies an array dimension, then the
	 * feature being refined must have an array dimension."
	 */
	private void checkForArraysInRefinedFeature(Feature feature) {
		if (!feature.getArrayDimensions().isEmpty() && feature.getRefined() != null
				&& feature.getRefined().getArrayDimensions().isEmpty())
			error(feature,
					"Cannot specify an array dimension because the refined feature doesn't have an array dimension.");
	}

	/**
	 * Checks legality rule 5 in section 8 (Features and Shared Access) on page
	 * 125. "If the refinement specifies an array dimension size, then the
	 * feature being refined must not have an array dimension size."
	 */
	private void checkForArrayDimensionSizeInRefinedFeature(Feature feature) {
		// TODO-phf: Now we have to check the list of ArrayDimensions if each
		// has the size field set.
		// if (!feature.getArrayDimensions().isEmpty() &&
		// !feature.getArraySpecification().getSizes().isEmpty() &&
		// feature.getRefined() != null &&
		// !feature.getRefined().getArrayDimensions().isEmpty() &&
		// !feature.getRefined().getArraySpecification().getSizes().isEmpty())
		// {
		// error(feature.getArraySpecification().getSizes().get(0),
		// "Cannot specify an array dimension size because the refined feature already specified the array dimension size.");
		// }
	}

	/**
	 * Checks legality rule 1 in section 8.1 (Abstract Features) on page 126.
	 * "The feature direction in a refined feature declaration must be identical
	 * to the feature direction in the feature declaration being refined, or the
	 * feature being refined must not have a direction."
	 * 
	 * Checks legality rule 2 in section 8.1 (Abstract Features) on page 127.
	 * "If the direction of an abstract feature is specified, then the direction
	 * must be satisfied by the refinement (see also the rules for feature
	 * prototypes in Section 4.7); in the case of ports the direction must be in
	 * or out; in the case of data access, the access right must be read-only
	 * for in and write-only for out; in the case of bus access, subprogram
	 * access and subprogram group access the direction must not be in nor out."
	 * This method does not check access features.
	 * 
	 * Checks legality rule 5 in section 8.3 (Ports) on page 133. "The port
	 * direction of a port refinement must be the same as the direction of the
	 * feature being refined. If the feature being refined is an abstract
	 * feature without direction, then all port directions are acceptable."
	 * 
	 * Checks legality rule 4 in section 8.5 (Subprogram Parameters) on page
	 * 148. "The parameter direction of a parameter refinement must be the same
	 * as the direction of the feature being refined. If the feature being
	 * refined is an abstract feature without direction, then all parameter
	 * directions are acceptable."
	 */
	private void checkFeatureDirectionInRefinement(DirectedFeature feature) {
		DirectionType direction = feature.getDirection();
		if (feature.getRefined() instanceof DirectedFeature) {
			DirectionType refinedDirection = ((DirectedFeature) feature.getRefined()).getDirection();
			// For ports and parameters, the directions must be the same value.
			// All other DirectedFeatures have the option of refining from
			// IN_OUT to IN or OUT.
			if (!direction.equals(refinedDirection)
					&& (feature.getRefined() instanceof Port || feature.getRefined() instanceof Parameter || !refinedDirection
							.equals(DirectionType.IN_OUT))) {
				error(feature,
						"Incompatible direction in feature refinement.  The direction of the refined feature is '"
								+ refinedDirection.getName() + "'.");
			}
		}
	}

	/**
	 * Checks legality rule 3 in section 8.1 (Abstract Features) on page 127.
	 * "An abstract feature with a feature prototype identifier and the
	 * prototype being referenced must both specify the same direction or no
	 * direction."
	 */
	private void checkAbstractFeatureAndPrototypeDirectionConsistency(AbstractFeature feature) {
		if (feature.getPrototype() instanceof FeaturePrototype) {
			DirectionType featureDirection = feature.getDirection();
			DirectionType prototypeDirection = ((FeaturePrototype) feature.getPrototype()).getDirection();
			if (!featureDirection.equals(prototypeDirection)) {
				if (prototypeDirection.equals(DirectionType.IN_OUT)) {
					error(feature,
							"A direction cannot be specified on the abstract feature because its prototype does not specify a direction.");
				} else {
					error(feature, "The direction of the abstract feature must match the direction of its prototype."
							+ "  The prototype's direction is '" + prototypeDirection.getName() + "'.");
				}
			}
		}
	}

	/**
	 * Checks legality rule 4 in section 8.1 (Abstract Features) on page 127.
	 * "An abstract feature refinement declaration of a feature with a feature
	 * prototype reference must only add property associations."
	 */
	private void checkForAddedDirectionInAbstractFeatureRefinement(AbstractFeature feature) {
		AbstractFeature refinedFeature = (AbstractFeature) feature.getRefined();
		while (refinedFeature != null && !(refinedFeature.getPrototype() instanceof FeaturePrototype))
			refinedFeature = (AbstractFeature) refinedFeature.getRefined();
		if (refinedFeature != null) {
			if (refinedFeature.getDirection().equals(DirectionType.IN_OUT)
					&& !feature.getDirection().equals(DirectionType.IN_OUT)) {
				error(feature,
						"The refined feature refers to a feature prototype.  Therefore, a direction cannot be added in the"
								+ " refinement because the direction will be specified in the prototype binding.");
			}
		}
	}

	/**
	 * Checks legality rule 4 in section 8.1 (Abstract Features) on page 127.
	 * "An abstract feature refinement declaration of a feature with a feature
	 * prototype reference must only add property associations."
	 */
	private void checkForAddedPrototypeOrClassifierInAbstractFeatureRefinement(AbstractFeature feature) {
		AbstractFeature refinedFeature = (AbstractFeature) feature.getRefined();
		while (refinedFeature != null && !(refinedFeature.getPrototype() instanceof FeaturePrototype))
			refinedFeature = (AbstractFeature) refinedFeature.getRefined();
		if (refinedFeature != null) {
			if (feature.getClassifier() != null)
				error(feature,
						"Cannot refer to a classifier because the refined feature refers to a feature prototype.");
			else if (feature.getPrototype() != null && !feature.getPrototype().equals(refinedFeature.getPrototype()))
				error(feature,
						"The refiend feature already refers to a prototype.  The prototype cannot be changed in the refinement.");
		}
	}

	/**
	 * Checks legality rule 2 in section 8.2 (Feature Groups and Feature Group
	 * Types) on page 129. "A feature group type can be declared to be the
	 * inverse of another feature group type, as indicated by the reserved words
	 * inverse of and the name of a feature group type. Any feature group type
	 * named in an inverse of statement cannot itself contain an inverse of
	 * statement. This means that several feature groups can be declared to be
	 * the inverse of one feature group, e.g., B inverse of A and C inverse of A
	 * is acceptable. However, chaining of inverses is not permitted, e.g., B
	 * inverse of A and C inverse of B is not acceptable."
	 */
	private void checkForChainedInverseFeatureGroupTypes(FeatureGroupType featureGroupType) {
		if (featureGroupType.getInverse() != null && featureGroupType.getInverse().getInverse() != null) {
			error(featureGroupType,
					"A feature group type cannot be an inverse of another feature group type that already contains an"
							+ " 'inverse of' declaration.");
		}
	}

	/**
	 * Checks legality rule 3 in section 8.2 (Feature Groups and Feature Group
	 * Types) on page 129. "Only feature group types without inverse of or
	 * feature group types with features and inverse of can be extended."
	 */
	private void checkForExtendingAnInverseFeatureGroupType(GroupExtension extension) {
		FeatureGroupType extended = extension.getExtended();
		if (extended.getInverse() != null && extended.getOwnedFeatures().isEmpty()) {
			error(extension, "Cannot extend a feature group type that contains an 'inverse of' declaration,"
					+ " but does not contain any locally defined features.");
		}
	}

	/**
	 * Checks legality rule 4 in section 8.2 (Feature Groups and Feature Group
	 * Types) on page 129. "A feature group type that is an extension of another
	 * feature group type without an inverse of cannot contain an inverse of
	 * statement."
	 */
	private void checkForInverseInFeatureGroupTypeExtension(GroupExtension extension) {
		FeatureGroupType extended = extension.getExtended();
		FeatureGroupType extending = (FeatureGroupType) extension.getSpecific();
		if (extending.getInverse() != null && extended.getInverse() == null) {
			error(extension,
					"A feature group type with an 'inverse of' declaration cannot extend a feature group type without an"
							+ " 'inverse of' declaration.");
		}
	}

	/**
	 * Checks legality rule 5 in section 8.2 (Feature Groups and Feature Group
	 * Types) on page 129. "The feature group type that is an extension of
	 * another feature group type with features and inverse of that adds
	 * features must have an inverse of to identify the feature group type whose
	 * inverse it is."
	 */
	private void checkForRequiredInverseInFeatureGroupTypeExtension(GroupExtension extension) {
		FeatureGroupType extended = extension.getExtended();
		FeatureGroupType extending = (FeatureGroupType) extension.getSpecific();
		if (!extended.getOwnedFeatures().isEmpty() && extended.getInverse() != null
				&& !extending.getOwnedFeatures().isEmpty() && extending.getInverse() == null) {
// XXX: phf commented out because we may allow independnet refinement and then pick up the inverseof from the ancestor
			//			warning(extending,
//					"Must specify inverse because local features are defined and the extended feature group type has an"
//							+ " 'inverse of' declaration.");
		}
	}

	/**
	 * Checks legality rule 6 in section 8.2 (Feature Groups and Feature Group
	 * Types) on page 129. "A feature group declaration with an inverse of
	 * statement must only reference feature group types without an inverse of
	 * statement."
	 */
	private void checkForInverseInFeatureGroup(FeatureGroup featureGroup) {
		if (featureGroup.isInverse() && featureGroup.getFeatureGroupType() != null
				&& featureGroup.getFeatureGroupType().getInverse() != null) {
			error(featureGroup,
					"Cannot specify 'inverse of' in the feature group because the referenced feature group type already"
							+ " contains an 'inverse of' declaration.");
		}
	}

	/**
	 * Checks legality rule 13 in section 8.2 (Feature Groups and Feature Group
	 * Types) on page 129. "If an in or out direction is specified as part of a
	 * feature group declaration, then all features inside the feature group
	 * must satisfy this direction."
	 */
	private void checkDirectionOfFeatureGroupMembers(FeatureGroup featureGroup) {
		if (!featureGroup.getDirection().equals(DirectionType.IN_OUT) && featureGroup.getFeatureGroupType() != null) {
			for (Feature feature : featureGroup.getFeatureGroupType().getAllFeatures()) {
				if (feature instanceof DirectedFeature
						&& !((DirectedFeature) feature).getDirection().equals(featureGroup.getDirection())) {
					error(featureGroup,
							"All ports, parameters, feature groups, and abstract features in the referenced feature group"
									+ " type must satisfy the direction specified in the feature group.");
				}
			}
		}
	}

	/**
	 * Checks legality rule L1 for section 8.4 (Subprogram and
	 * Subprogram Group Access) "If a subprogram access refers to a component
	 * classifier or a component prototype, then the category of the classifier
	 * or prototype must be subprogram." For references to classifiers, the
	 * meta-model only allows for a subprogram classifier. Therefore, this
	 * method only checks the prototype reference.
	 */
	private void checkSubprogramAccessPrototypeReference(SubprogramAccess subprogramAccess) {
		Prototype sp = subprogramAccess.getPrototype();
		if (sp != null && !(subprogramAccess.getPrototype() instanceof SubprogramPrototype)) {
			error(subprogramAccess, "The category of the referenced component prototype must be subprogram.");
		}
	}

	/**
	 * Checks legality rule L2 for section 8.4 (Subprogram and
	 * Subprogram Group Access) "If a subprogram group access refers to a
	 * component classifier or a component prototype, then the category of the
	 * classifier or prototype must be subprogram group." For references to
	 * classifiers, the meta-model only allows for a subprogram group
	 * classifier. Therefore, this method only checks the prototype reference.
	 */
	private void checkSubprogramGroupAccessPrototypeReference(SubprogramGroupAccess subprogramGroupAccess) {
		Prototype sp = subprogramGroupAccess.getPrototype();
		if (sp != null && !(sp instanceof SubprogramGroupPrototype)) {
			error(subprogramGroupAccess, "The category of the referenced component prototype must be subprogram group.");
		}
	}

	/**
	 * Checks legality rule L3 for section 8.4 (Subprogram and
	 * Subprogram Group Access) "An abstract feature can be refined into a
	 * subprogram access or a subprogram group access. In this case, the
	 * abstract feature must not have a direction specified."
	 * 
	 * Checks legality rule L4 for section 8.6 (Data Component Access)
	 * "An abstract feature can be refined into a data access. In this case, the
	 * abstract feature must not have a direction specified."
	 * 
	 * Checks legality rule L4 for section 8.7 (Bus Component Access)
	 * "An abstract feature can be refined into a bus access. In this case, the
	 * abstract feature must not have a direction specified."
	 */
	private void checkForAbstractFeatureDirectionInAccessRefinement(Access access) {
		if (access.getRefined() instanceof AbstractFeature
				&& !((AbstractFeature) access.getRefined()).getDirection().equals(DirectionType.IN_OUT)) {
			error(access, "An abstract feature with a direction specified cannot be refined into an access feature.");
		}
	}

	/**
	 * Checks legality rule L6 for section 8.4 (Subprogram and
	 * Subprogram Group Access) "A provides subprogram access cannot be refined
	 * to a requires subprogram access and a requires subprogram access cannot
	 * be refined to a provides subprogram access. Similarly, a provides
	 * subprogram group access cannot be refined to a requires subprogram group
	 * access and a requires subprogram group access cannot be refined to a
	 * provides subprogram group access."
	 * 
	 * Checks legality rule L3 for section 8.6 (Data Component Access)
	 * "A provides data access cannot be refined to a requires data access and a
	 * requires data access cannot be refined to a provides data access."
	 * 
	 * Checks rule L3 for section 8.7 (Bus Component Access) "A
	 * provides bus access cannot be refined to a requires bus access and a
	 * requires bus access cannot be refined to a provides bus access."
	 */
	private void checkForAccessTypeInAccessRefinement(Access access) {
		if (access.getRefined() instanceof Access && !access.getKind().equals(((Access) access.getRefined()).getKind())) {
			StringBuilder errorMessage = new StringBuilder("A ");
			errorMessage.append(getKeywordForAccessType(((Access) access.getRefined()).getKind()));
			errorMessage.append(" access cannot be refined into a ");
			errorMessage.append(getKeywordForAccessType(access.getKind()));
			errorMessage.append(" access.");
			error(access, errorMessage.toString());
		}
	}

	/**
	 * Checks legality rule L1 for section 8.6 (Data Component Access)
	 * "If a data access refers to a component classifier or a component
	 * prototype, then the category of the classifier or prototype must be
	 * data." For references to classifiers, the meta-model only allows for a
	 * data classifier. Therefore, this method only checks the prototype
	 * reference.
	 */
	private void checkDataAccessPrototypeReference(DataAccess dataAccess) {
		Prototype dp = dataAccess.getPrototype();
		if (dp != null && !(dp instanceof DataPrototype)) {
			error(dataAccess, "The category of the referenced component prototype must be data.");
		}
	}

	private void checkDefiningID(Connection conn) {
		// TODO enable for 2.1 compatibility checking
//		String name = conn.getName();
//		if (name == null || name.isEmpty()){
//			warning(conn, "Connection is missing defining identifier. Required in AADL V2.1");
//		}
	}
	
	/**
	 * Checks legality rule L13 for section 9.2 (Port Connections)
	 * "For connections between data ports, event data ports, and data access,
	 * the data classifier of the source port must match the data type of the
	 * destination port.  The Classifier_Matching_Rule property specifies the
	 * rule to be applied to match the data classifier of a connection source
	 * to the data classifier of a connection destination."
	 * 
	 * Checks legality rule L14 for section 9.2 (Port Connections)
	 * "The following rules are supported:
	 * 
	 * -Classifier_Match: The source data type and data implementation must be
	 * identical to the data type or data implementation of the destination.
	 * If the destination classifier is a component type, then any
	 * implementation of the source matches.  This is the default rule.
	 * 
	 * -Equivalence: An indication that the two classifiers of a connection are
	 * considered to match if they are listed in the
	 * Supported_Classifier_Equivalence_Matches property.  Acceptable data
	 * classifiers matches are specified as
	 * Supported_Classifier_Equivalence_Matches property with pairs of
	 * classifier values representing acceptable matches.  Either element of
	 * the pair can be the source or destination classifier.  Equivalence is
	 * intended to be used when the data types are considered to be identical,
	 * i.e., no conversion is necessary.  The
	 * Supported_Classifier_Equivalence_Matches property is declared globally
	 * as a property constant.
	 * 
	 * -Subset: A mapping of (a subset of) data elements of the source port
	 * data type to all data elements of the destination port data type.
	 * Acceptable data classifier matches are specified as
	 * Supported_Classifier_Subset_Matches property with pairs of classifier
	 * values representing acceptable matches.  The first element of each pair
	 * specifies the acceptable source classifier, while the second element
	 * specifies the acceptable destination classifier.  The
	 * Supported_Classifier_Subset_Matches property is declared globally as a
	 * property constant.  A virtual bus or bus must represent a protocol that
	 * supports subsetting, such as OMG DDS.
	 * 
	 * -Conversion: A mapping of the source port data type to the destination
	 * port data type, where the source port data type requires a conversion to
	 * the destination port data type.  Acceptable data classifier matches are
	 * specified as Supported_Type_Conversions property with pairs of
	 * classifier values representing acceptable matches.  The first element of
	 * each pair specifies the acceptable source classifier, while the second
	 * element specifies the acceptable destination classifier.  The
	 * Supported_Type_Conversions property may be declared globally as a
	 * property constant.  A virtual bus or bus must support the conversion
	 * from the source data classifier to the destination classifier."
	 */
	private void checkPortConnectionClassifiers(PortConnection connection) {
		ConnectionEnd source = connection.getAllSource();
		ConnectionEnd destination = connection.getAllDestination();
		if ((source instanceof DataAccess || source instanceof DataSubcomponent || source instanceof DataPort || source instanceof EventDataPort) &&
				(destination instanceof DataAccess || destination instanceof DataSubcomponent || destination instanceof DataPort || destination instanceof EventDataPort)) {
			Classifier sourceClassifier;
			Classifier destinationClassifier;
			if (source instanceof DataSubcomponent)
				sourceClassifier = ((DataSubcomponent)source).getAllClassifier();
			else
				sourceClassifier = ((Feature)source).getAllClassifier();
			if (destination instanceof DataSubcomponent)
				destinationClassifier = ((DataSubcomponent)destination).getAllClassifier();
			else
				destinationClassifier = ((Feature)destination).getAllClassifier();
			if (sourceClassifier == null && destinationClassifier != null)
				warning(connection, '\'' + source.getName() + "' is missing a classifier.");
			else if (sourceClassifier != null && destinationClassifier == null)
				warning(connection, '\'' + destination.getName() + "' is missing a classifier.");
			else if (sourceClassifier != null && destinationClassifier != null) {
				Property classifierMatchingRuleProperty = GetProperties.lookupPropertyDefinition(connection, ModelingProperties._NAME, ModelingProperties.CLASSIFIER_MATCHING_RULE);
				EnumerationLiteral classifierMatchingRuleValue = null;
				if (classifierMatchingRuleProperty != null){
					try {
						classifierMatchingRuleValue = PropertyUtils.getEnumLiteral(connection, classifierMatchingRuleProperty);
					}
					catch (PropertyNotPresentException e) {
						classifierMatchingRuleValue = null;
					}
				}
				if (classifierMatchingRuleValue == null || ModelingProperties.CLASSIFIER_MATCH.equalsIgnoreCase(classifierMatchingRuleValue.getName()) ||
						classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.COMPLEMENT)) {
					if (classifierMatchingRuleValue != null && ModelingProperties.COMPLEMENT.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
						warning(connection, "The classifier matching rule '" + ModelingProperties.COMPLEMENT + "' is not supported for port connections. Using rule '" + ModelingProperties.CLASSIFIER_MATCH +
								"' instead.");
					}
					if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier))
						error(connection, '\'' + source.getName() + "' and '" + destination.getName() + "' have incompatible classifiers.");
				}
				else if (ModelingProperties.EQUIVALENCE.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
					if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
							!classifiersFoundInSupportedClassifierEquivalenceMatchesProperty(connection, sourceClassifier, destinationClassifier)) {
						error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
								destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
								AadlProject.SUPPORTED_CLASSIFIER_EQUIVALENCE_MATCHES + "'.");
					}
				}
				else if (ModelingProperties.SUBSET.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
					if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
							!classifiersFoundInSupportedClassifierSubsetMatchesProperty(connection, sourceClassifier, destinationClassifier)) {
						error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
								destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
								AadlProject.SUPPORTED_CLASSIFIER_SUBSET_MATCHES + "'.");
					}
				}
				else if (ModelingProperties.CONVERSION.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
					if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
							!classifiersFoundInSupportedTypeConversionsProperty(connection, sourceClassifier, destinationClassifier)) {
						error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
								destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
								AadlProject.SUPPORTED_TYPE_CONVERSIONS + "'.");
					}
				}
			}
		}
	}
	
	private boolean testClassifierMatchRule(Connection connection, ConnectionEnd source, Classifier sourceClassifier, ConnectionEnd destination, Classifier destinationClassifier) {
		if (sourceClassifier != destinationClassifier) {
			if (sourceClassifier instanceof ComponentType && destinationClassifier instanceof ComponentImplementation) {
				if (!sourceClassifier.equals(((ComponentImplementation)destinationClassifier).getType())) {
					warning(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' do not match.");
				}
			}
			else if (sourceClassifier instanceof ComponentImplementation && destinationClassifier instanceof ComponentType) {
				if (!destinationClassifier.equals(((ComponentImplementation)sourceClassifier).getType())) {
					warning(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' do not match.");
				}
			}
			else
				return false;
		}
		return true;
	}
	
	private boolean classifiersFoundInSupportedClassifierEquivalenceMatchesProperty(Connection connection, Classifier source, Classifier destination) {
		PropertyConstant matchesPropertyConstant = GetProperties.lookupPropertyConstant(connection, AadlProject.SUPPORTED_CLASSIFIER_EQUIVALENCE_MATCHES);
		if (matchesPropertyConstant == null)
			return false;
		PropertyExpression constantValue = matchesPropertyConstant.getConstantValue();
		if (!(constantValue instanceof ListValue))
			return false;
		for (PropertyExpression classifierPair : ((ListValue)constantValue).getOwnedListElements()) {
			if (classifierPair instanceof ListValue) {
				EList<PropertyExpression> innerListElements = ((ListValue)classifierPair).getOwnedListElements();
				if (innerListElements.size() == 2 && innerListElements.get(0) instanceof ClassifierValue && innerListElements.get(1) instanceof ClassifierValue) {
					Classifier firstPairElement = ((ClassifierValue)innerListElements.get(0)).getClassifier();
					Classifier secondPairElement = ((ClassifierValue)innerListElements.get(1)).getClassifier();
					if ((firstPairElement == source && secondPairElement == destination) || (firstPairElement == destination && secondPairElement == source))
						return true;
				}
			}
		}
		return false;
	}
	
	private boolean classifiersFoundInSupportedClassifierSubsetMatchesProperty(Connection connection, Classifier source, Classifier destination) {
		PropertyConstant matchesPropertyConstant = GetProperties.lookupPropertyConstant(connection, AadlProject.SUPPORTED_CLASSIFIER_SUBSET_MATCHES);
		if (matchesPropertyConstant == null)
			return false;
		PropertyExpression constantValue = matchesPropertyConstant.getConstantValue();
		if (!(constantValue instanceof ListValue))
			return false;
		for (PropertyExpression classifierPair : ((ListValue)constantValue).getOwnedListElements()) {
			if (classifierPair instanceof ListValue) {
				EList<PropertyExpression> innerListElements = ((ListValue)classifierPair).getOwnedListElements();
				if (innerListElements.size() == 2 && innerListElements.get(0) instanceof ClassifierValue && innerListElements.get(1) instanceof ClassifierValue) {
					Classifier firstPairElement = ((ClassifierValue)innerListElements.get(0)).getClassifier();
					Classifier secondPairElement = ((ClassifierValue)innerListElements.get(1)).getClassifier();
					if (firstPairElement == source && secondPairElement == destination)
						return true;
				}
			}
		}
		return false;
	}
	
	private boolean classifiersFoundInSupportedTypeConversionsProperty(Connection connection, Classifier source, Classifier destination) {
		PropertyConstant conversionsPropertyConstant = GetProperties.lookupPropertyConstant(connection, AadlProject.SUPPORTED_TYPE_CONVERSIONS);
		if (conversionsPropertyConstant == null)
			return false;
		PropertyExpression constantValue = conversionsPropertyConstant.getConstantValue();
		if (!(constantValue instanceof ListValue))
			return false;
		for (PropertyExpression classifierPair : ((ListValue)constantValue).getOwnedListElements()) {
			if (classifierPair instanceof ListValue) {
				EList<PropertyExpression> innerListElements = ((ListValue)classifierPair).getOwnedListElements();
				if (innerListElements.size() == 2 && innerListElements.get(0) instanceof ClassifierValue && innerListElements.get(1) instanceof ClassifierValue) {
					Classifier firstPairElement = ((ClassifierValue)innerListElements.get(0)).getClassifier();
					Classifier secondPairElement = ((ClassifierValue)innerListElements.get(1)).getClassifier();
					if (firstPairElement == source && secondPairElement == destination)
						return true;
				}
			}
		}
		return false;
	}
	
	
	private void checkFeatureRefinementClassifierSubstitution(Feature feature){
		if (!Aadl2Util.isNull(feature.getRefined() )){
			 Classifier refinedCl = feature.getClassifier();
			 Classifier originalCl = feature.getRefined().getClassifier();
			if (!Aadl2Util.isNull(refinedCl)&&!Aadl2Util.isNull(originalCl)){
				checkClassifierSubstitutionMatch(feature,originalCl,refinedCl);
			}
		}
	}

	
	private void checkSubcomponentRefinementClassifierSubstitution(Subcomponent subcomponent){
		if (!Aadl2Util.isNull(subcomponent.getRefined() )){
			 ComponentClassifier refinedCl = subcomponent.getClassifier();
			 ComponentClassifier originalCl = subcomponent.getRefined().getClassifier();
			if (!Aadl2Util.isNull(refinedCl)&&!Aadl2Util.isNull(originalCl)){
				checkClassifierSubstitutionMatch(subcomponent,originalCl,refinedCl);
			}
		}
	}
	
	private void checkClassifierSubstitutionMatch(NamedElement target, Classifier originalClassifier,Classifier refinedClassifier){
		Property classifierMatchingRuleProperty = GetProperties.lookupPropertyDefinition(target, ModelingProperties._NAME, ModelingProperties.CLASSIFIER_SUBSTITUTION_RULE);
		EnumerationLiteral classifierMatchingRuleValue;
		try {
			classifierMatchingRuleValue = PropertyUtils.getEnumLiteral(target, classifierMatchingRuleProperty);
		}
		catch (PropertyLookupException e) {
			classifierMatchingRuleValue = null;
		}
		if (classifierMatchingRuleValue == null || ModelingProperties.CLASSIFIER_MATCH.equalsIgnoreCase(classifierMatchingRuleValue.getName()) ) {
			if (!AadlUtil.isokClassifierSubstitutionMatch(originalClassifier, refinedClassifier))
				error(target, "Classifier " + originalClassifier.getName() + " refined to " + refinedClassifier.getName() + " does not satisfy 'Classifier Match'");
		}
		else if (classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.TYPE_EXTENSION)) {
			if (!AadlUtil.isokClassifierSubstitutionTypeExtension(originalClassifier, refinedClassifier))
				error(target, "Classifier " + originalClassifier.getName() + " refined to " + refinedClassifier.getName() + " does not satisfy 'Type Extension'");
		}
		else if (ModelingProperties.SIGNATURE_MATCH.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
			warning(target, "Signature Match checking in clasifier substitution of refinement check not implemented yet.");
		}
	}


	/**
	 * Check direction of ConnectionEnd in port connections
	 */
	private void checkPortConnectionDirection(PortConnection connection) {
		ConnectionEnd source = connection.getAllSource();
		ConnectionEnd destination = connection.getAllDestination();
		DirectionType srcDirection = DirectionType.IN_OUT;
		DirectionType dstDirection = DirectionType.IN_OUT;
		if (source instanceof DirectedFeature)
			srcDirection = ((DirectedFeature) source).getDirection();
		if (destination instanceof DirectedFeature)
			dstDirection = ((DirectedFeature) destination).getDirection();
		if (source instanceof DataSubcomponent || source instanceof DataAccess) {
			// TODO check access right to limit to in or out
		}
		if (destination instanceof DataSubcomponent || destination instanceof DataAccess) {
			// TODO check access right to limit to in or out
		}
		Context srcContext = connection.getAllSourceContext();
		Context dstContext = connection.getAllDestinationContext();
		if (srcContext instanceof FeatureGroup) {
			if (((FeatureGroup) srcContext).isInverse()) {
				srcDirection = srcDirection.getInverseDirection();
			}
			FeatureGroupType srcFGT = ((FeatureGroup) srcContext).getFeatureGroupType();
			FeatureGroupType contsrcFGT = (FeatureGroupType) ((Feature) source).getContainingClassifier();
			if (srcFGT != contsrcFGT && !Aadl2Util.isNull(srcFGT) && srcFGT.getInverse() != null) {
				// feature group type has inverse and feature is defined in the inverse FGT
				srcDirection = srcDirection.getInverseDirection();
			}
		}
		if (dstContext instanceof FeatureGroup) {
			if (((FeatureGroup) dstContext).isInverse()) {
				dstDirection = dstDirection.getInverseDirection();
			}
			FeatureGroupType dstFGT = ((FeatureGroup) dstContext).getFeatureGroupType();
			FeatureGroupType contdstFGT = (FeatureGroupType) ((Feature) destination).getContainingClassifier();
			if (dstFGT != contdstFGT && !Aadl2Util.isNull(dstFGT)  && dstFGT.getInverse() != null) {
				dstDirection = dstDirection.getInverseDirection();
			}
		}
		if ((srcContext instanceof Subcomponent && dstContext instanceof Subcomponent)
		// between ports of subcomponents
				|| (srcContext == null && source instanceof DataSubcomponent && dstContext instanceof Subcomponent)
				// from a data subcomponent to a port
				|| (dstContext == null && destination instanceof DataSubcomponent && srcContext instanceof Subcomponent)
		// from a data subcomponent to a port
		) {
			if (srcDirection == DirectionType.IN && dstDirection == DirectionType.IN
					|| srcDirection == DirectionType.OUT && dstDirection == DirectionType.OUT) {
				error(connection, "Source and destination directions do not allow any flow.");
			}
		} else if ((srcContext instanceof Subcomponent || dstContext instanceof Subcomponent)||
			       (srcContext instanceof SubprogramCall || dstContext instanceof SubprogramCall)) {
			// going up or down hierarchy
			if (!sameDirection(srcDirection, dstDirection)) {
				error(connection,
						"Source feature '" + source.getName() + "' and destination feature '" + destination.getName()
								+ "' must have same direction.");
			}
			if ((srcContext instanceof Subcomponent) || (srcContext instanceof SubprogramCall)) {
				if (!(srcDirection.outgoing()))
					error(connection, "Outgoing connection requires outgoing feature '" + srcContext.getName() + "."
							+ source.getName() + "'.");
				if (!(dstDirection.outgoing()))
					error(connection, "Outgoing connection requires outgoing feature '" + destination.getName() + "'.");
			}
			if ((dstContext instanceof Subcomponent) || (dstContext instanceof SubprogramCall)) {
				if (!(dstDirection.incoming()))
					error(connection, "Incoming connection requires incoming feature '" + dstContext.getName() + "."
							+ destination.getName() + "'.");
				if (!(srcDirection.incoming()))
					error(connection, "Incoming connection requires incoming feature '" + source.getName() + "'.");
			}
		} else {

			// we have a connection a component implementation going directly from its incoming feature to an outgoing feature
			if (!(srcDirection.incoming() && dstDirection.outgoing())) {
				error(connection, "Source feature '" + source.getName()
						+ "' must2   be incoming and destination feature '" + destination.getName()
						+ "' must be outgoing.");
			}
		}
	}

	/**
	 * Check connection ends of port connections
	 * Section 9.2 Legality rule L5
	 */
	private void checkPortConnectionEnds(PortConnection connection) {
		ConnectionEnd source = connection.getAllSource();
		ConnectionEnd destination = connection.getAllDestination();
		if (source instanceof EventPort && !(destination instanceof EventPort)) {
			error(connection, "Source event port '" + source.getName()
					+ "' must be connected to an event port destination.");
			return;
		}
		if (source instanceof DataPort
				&& !(destination instanceof EventPort || destination instanceof DataPort
						|| destination instanceof EventDataPort || destination instanceof DataSubcomponent || destination instanceof DataAccess)) {
			error(connection,
					"Source data port '"
							+ source.getName()
							+ "' must be connected to an event, data, or event data port, data subcomponent or data access destination.");
			return;
		}
		if (source instanceof EventDataPort
				&& !(destination instanceof EventPort || destination instanceof DataPort
						|| destination instanceof EventDataPort || destination instanceof DataSubcomponent || destination instanceof DataAccess)) {
			error(connection,
					"Source event data port '"
							+ source.getName()
							+ "' must be connected to an event, data, or event data port, data subcomponent or data access destination.");
			return;
		}
		if (source instanceof DataSubcomponent
				&& !(destination instanceof EventPort || destination instanceof DataPort || destination instanceof EventDataPort)) {
			error(connection, "Source data subcomponent '" + source.getName()
					+ "' must be connected to an event, data, or event data port destination.");
			return;
		}
		if (source instanceof DataAccess
				&& !(destination instanceof EventPort || destination instanceof DataPort || destination instanceof EventDataPort)) {
			error(connection, "Source data access feature '" + source.getName()
					+ "' must be connected to an event, data, or event data port destination.");
			return;
		}
	}
	
	/**
	 * Checks legality rule L4 for section 9.3 (Parameter Connections)
	 * "The data classifier of the source and destination must match.  The matching
	 * rules as specified by the Classifier_Matching_Rule property apply (see
	 * Section 9.2 (L13)).  By default the data classifiers must be match."
	 */
	private void checkParameterConnectionClassifiers(ParameterConnection connection) {
		ConnectionEnd source = connection.getAllSource();
		ConnectionEnd destination = connection.getAllDestination();
		Classifier sourceClassifier;
		Classifier destinationClassifier;
		if (source instanceof DataSubcomponent)
			sourceClassifier = ((DataSubcomponent)source).getAllClassifier();
		else
			sourceClassifier = ((Feature)source).getAllClassifier();
		if (destination instanceof DataSubcomponent)
			destinationClassifier = ((DataSubcomponent)destination).getAllClassifier();
		else
			destinationClassifier = ((Feature)destination).getAllClassifier();
		if (sourceClassifier == null && destinationClassifier != null)
			warning(connection, '\'' + source.getName() + "' is missing a classifier.");
		else if (sourceClassifier != null && destinationClassifier == null)
			warning(connection, '\'' + destination.getName() + "' is missing a classifier.");
		else if (sourceClassifier != null && destinationClassifier != null) {
			Property classifierMatchingRuleProperty = GetProperties.lookupPropertyDefinition(connection, ModelingProperties._NAME, ModelingProperties.CLASSIFIER_MATCHING_RULE);
			EnumerationLiteral classifierMatchingRuleValue;
			try {
				classifierMatchingRuleValue = PropertyUtils.getEnumLiteral(connection, classifierMatchingRuleProperty);
			}
			catch (PropertyNotPresentException e) {
				classifierMatchingRuleValue = null;
			}
			if (classifierMatchingRuleValue == null || classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.CLASSIFIER_MATCH) ||
					classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.COMPLEMENT)) {
				if (classifierMatchingRuleValue != null && classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.COMPLEMENT)) {
					warning(connection, "The classifier matching rule '" + ModelingProperties.COMPLEMENT + "' is not supported for parameter connections. Using rule '" + ModelingProperties.CLASSIFIER_MATCH +
							"' instead.");
				}
				if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier)) {
					error(connection, '\'' + source.getName() + "' and '" + destination.getName() + "' have incompatible classifiers.");
				}
			}
			else if (classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.EQUIVALENCE)) {
				if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
						!classifiersFoundInSupportedClassifierEquivalenceMatchesProperty(connection, sourceClassifier, destinationClassifier)) {
					error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
							destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
							AadlProject.SUPPORTED_CLASSIFIER_EQUIVALENCE_MATCHES + "'.");
				}
			}
			else if (classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.SUBSET)) {
				if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
						!classifiersFoundInSupportedClassifierSubsetMatchesProperty(connection, sourceClassifier, destinationClassifier)) {
					error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
							destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
							AadlProject.SUPPORTED_CLASSIFIER_SUBSET_MATCHES + "'.");
				}
			}
			else if (classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.CONVERSION)) {
				if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
						!classifiersFoundInSupportedTypeConversionsProperty(connection, sourceClassifier, destinationClassifier)) {
					error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
							destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
							AadlProject.SUPPORTED_TYPE_CONVERSIONS + "'.");
				}
			}
		}
	}

	/**
	 * Check category of source and destination
	 * Section 9.4 Legality rule L11
	 */
	private void checkAccessConnectionCategory(AccessConnection connection) {
		AccessCategory connectionCategory = connection.getAccessCategory();

		ConnectionEnd source = (ConnectionEnd) connection.getAllSource();
		AccessCategory sourceCategory = null;
		if (Aadl2Util.isNull(source) && connection.getSource() instanceof ProcessorSubprogram)
			sourceCategory = AccessCategory.SUBPROGRAM;
		if (source instanceof Access)
			sourceCategory = ((Access) source).getCategory();
		else if (source instanceof BusSubcomponent)
			sourceCategory = AccessCategory.BUS;
		else if (source instanceof DataSubcomponent)
			sourceCategory = AccessCategory.DATA;
		else if (source instanceof SubprogramSubcomponent)
			sourceCategory = AccessCategory.SUBPROGRAM;
		else if (source instanceof SubprogramGroupSubcomponent)
			sourceCategory = AccessCategory.SUBPROGRAM_GROUP;

		ConnectionEnd destination = (ConnectionEnd) connection.getAllDestination();
		AccessCategory destinationCategory = null;
		if (Aadl2Util.isNull(destination) && connection.getDestination() instanceof ProcessorSubprogram)
			destinationCategory = AccessCategory.SUBPROGRAM;
		if (destination instanceof Access)
			destinationCategory = ((Access) destination).getCategory();
		else if (destination instanceof BusSubcomponent)
			destinationCategory = AccessCategory.BUS;
		else if (destination instanceof DataSubcomponent)
			destinationCategory = AccessCategory.DATA;
		else if (destination instanceof SubprogramSubcomponent)
			destinationCategory = AccessCategory.SUBPROGRAM;
		else if (destination instanceof SubprogramGroupSubcomponent)
			destinationCategory = AccessCategory.SUBPROGRAM_GROUP;

		if (!connectionCategory.equals(sourceCategory)) {
			error(connection, "The source of a " + connectionCategory.getName() + " access connection must be a "
					+ connectionCategory.getName() + " access feature or a " + connectionCategory.getName()
					+ " subcomponent.");
		}

		if (!connectionCategory.equals(destinationCategory)) {
			error(connection, "The destination of a " + connectionCategory.getName() + " access connection must be a "
					+ connectionCategory.getName() + " access feature or a " + connectionCategory.getName()
					+ " subcomponent.");
		}
	}

	/**
	 * Check provides/requires of access connection ends
	 * Section 9.4 Legality rules L5, L6, and L7
	 */
	private void checkAccessConnectionProvidesRequires(AccessConnection connection) {
		ConnectionEnd source = connection.getAllSource();
		ConnectionEnd destination = connection.getAllDestination();
		AccessType sourceType = null;
		AccessType destinationType = null;
		Context srcContext = connection.getAllSourceContext();
		Context dstContext = connection.getAllDestinationContext();

		if (source instanceof Access) {
			sourceType = ((Access) source).getKind();
			if (srcContext instanceof FeatureGroup) {
				if (((FeatureGroup) srcContext).isInverse())
					sourceType = sourceType.getInverseType();
				FeatureGroupType srcFGT = ((FeatureGroup) srcContext).getAllFeatureGroupType();
				FeatureGroupType contsrcFGT = (FeatureGroupType) ((Access) source).getContainingClassifier();
				if (!srcFGT.equals(contsrcFGT) && srcFGT.getInverse() != null) {
					// feature group type has inverse and feature is defined in the inverse FGT
					sourceType = sourceType.getInverseType();
				}
			}
		}

		if (destination instanceof Access) {
			destinationType = ((Access) destination).getKind();
			if (dstContext instanceof FeatureGroup) {
				if (((FeatureGroup) dstContext).isInverse())
					destinationType = destinationType.getInverseType();
				FeatureGroupType dstFGT = ((FeatureGroup) dstContext).getAllFeatureGroupType();
				FeatureGroupType contdstFGT = (FeatureGroupType) ((Access) destination).getContainingClassifier();
				if (!dstFGT.equals(contdstFGT) && dstFGT.getInverse() != null) {
					//feature group type has inverse and feature is defined in the inverse FGT
					destinationType = destinationType.getInverseType();
				}
			}
		}

		//Test for L5: connection between access features of sibling components
		if (srcContext instanceof Subcomponent && dstContext instanceof Subcomponent && source instanceof Access
				&& destination instanceof Access) {
			if (sourceType.equals(AccessType.PROVIDES) && destinationType.equals(AccessType.PROVIDES))
				error(connection,
						"Source and destination of access connections between sibling components cannot both be 'provides'.");
			if (sourceType.equals(AccessType.REQUIRES) && destinationType.equals(AccessType.REQUIRES))
				error(connection,
						"Source and destination of access connections between sibling components cannot both be 'requires'.");
		}
		//Test for the common case of L6 and L7: connection between an access feature in the containing component and an access feature in a subcomponent.
		else if (source instanceof Access
				&& destination instanceof Access
				&& ((srcContext instanceof Subcomponent && (dstContext == null || dstContext instanceof FeatureGroup)) || (dstContext instanceof Subcomponent && (srcContext == null || srcContext instanceof FeatureGroup)))) {
			if (!sourceType.equals(destinationType))
				error(connection,
						"Source and destination must both be provides or requires for a connection mapping features up or down the containment hierarchy.");
		}
		//Test for L6: connection between subcomponent and access feature
		else if (source instanceof Subcomponent && destination instanceof Access
				&& (dstContext == null || dstContext instanceof FeatureGroup)) {
			if (!destinationType.equals(AccessType.PROVIDES))
				error(connection, '\'' + destination.getName()
						+ "' must be a provides access feature for a connection from an accessed subcomponent.");
		}
		//Test for L6: connection between access feature and subcomponent
		else if (destination instanceof Subcomponent && source instanceof Access
				&& (srcContext == null || srcContext instanceof FeatureGroup)) {
			if (!sourceType.equals(AccessType.PROVIDES))
				error(connection, '\'' + source.getName()
						+ "' must be a provides access feature for a connection to a accessed subcomponent.");
		}
		//Test for L7: connection between subcomponent and access feature of subcomponent
		else if (source instanceof Subcomponent && destination instanceof Access && dstContext instanceof Subcomponent) {
			if (!destinationType.equals(AccessType.REQUIRES))
				error(connection, '\'' + destination.getName()
						+ "' must be a requires access feature for a connection from an accessed subcomponent.");
		}
		//Test for L7: connection between access feature of subcomponent and subcomponent
		else if (destination instanceof Subcomponent && source instanceof Access && srcContext instanceof Subcomponent) {
			if (!sourceType.equals(AccessType.REQUIRES))
				error(connection, '\'' + source.getName()
						+ "' must be a requires access feature for a connection to an accessed subcomponent.");
		}
	}
	
	/**
	 * Checks legality rule L9 for section 9.4 (Access Connections)
	 * "For access connections the classifier of the provider access must match
	 * to the classifier of the requires access according to the
	 * Classifier_Matching_Rules property.  By default the classifiers must be
	 * the same (see Section 9.1)."
	 */
	private void checkAccessConnectionClassifiers(AccessConnection connection) {
		ConnectionEnd source = connection.getAllSource();
		ConnectionEnd destination = connection.getAllDestination();
		if (source instanceof AccessConnectionEnd && destination instanceof AccessConnectionEnd) {
			Classifier sourceClassifier;
			Classifier destinationClassifier;
			if (source instanceof Access)
				sourceClassifier = ((Access)source).getAllClassifier();
			else
				sourceClassifier = ((Subcomponent)source).getAllClassifier();
			if (destination instanceof Access)
				destinationClassifier = ((Access)destination).getAllClassifier();
			else
				destinationClassifier = ((Subcomponent)destination).getAllClassifier();
			if (sourceClassifier == null && destinationClassifier != null)
				warning(connection, '\'' + source.getName() + "' is missing a classifier.");
			else if (sourceClassifier != null && destinationClassifier == null)
				warning(connection, '\'' + destination.getName() + "' is missing a classifier.");
			else if (sourceClassifier != null && destinationClassifier != null) {
				Property classifierMatchingRuleProperty = GetProperties.lookupPropertyDefinition(connection, ModelingProperties._NAME, ModelingProperties.CLASSIFIER_MATCHING_RULE);
				EnumerationLiteral classifierMatchingRuleValue;
				try {
					classifierMatchingRuleValue = PropertyUtils.getEnumLiteral(connection, classifierMatchingRuleProperty);
				}
				catch (PropertyNotPresentException e) {
					classifierMatchingRuleValue = null;
				}
				if (classifierMatchingRuleValue == null || classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.CLASSIFIER_MATCH) ||
						classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.COMPLEMENT)) {
					if (classifierMatchingRuleValue != null && classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.COMPLEMENT)) {
						warning(connection, "The classifier matching rule '" + ModelingProperties.COMPLEMENT + "' is not supported for access connections. Using rule '" + ModelingProperties.CLASSIFIER_MATCH +
								"' instead.");
					}
					if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier))
						error(connection, '\'' + source.getName() + "' and '" + destination.getName() + "' have incompatible classifiers.");
				}
				else if (classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.EQUIVALENCE)) {
					if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
							!classifiersFoundInSupportedClassifierEquivalenceMatchesProperty(connection, sourceClassifier, destinationClassifier)) {
						error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
								destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
								AadlProject.SUPPORTED_CLASSIFIER_EQUIVALENCE_MATCHES + "'.");
					}
				}
				else if (classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.SUBSET)) {
					if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
							!classifiersFoundInSupportedClassifierSubsetMatchesProperty(connection, sourceClassifier, destinationClassifier)) {
						error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
								destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
								AadlProject.SUPPORTED_CLASSIFIER_SUBSET_MATCHES + "'.");
					}
				}
				else if (classifierMatchingRuleValue.getName().equalsIgnoreCase(ModelingProperties.CONVERSION)) {
					if (!testClassifierMatchRule(connection, source, sourceClassifier, destination, destinationClassifier) &&
							!classifiersFoundInSupportedTypeConversionsProperty(connection, sourceClassifier, destinationClassifier)) {
						error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceClassifier.getQualifiedName() + "' and '" +
								destinationClassifier.getQualifiedName() + "') are incompatible and they are not listed as matching classifiers in the property constant '" +
								AadlProject.SUPPORTED_TYPE_CONVERSIONS + "'.");
					}
				}
			}
		}
	}

	/**
	 * Checks that the feature of a flow is a DataAccess, FeatureGroup, Parameter, or Port.
	 * Section 10.1 Naming Rule N2.
	 */
	private void checkFlowFeatureType(FlowSpecification flow) {
		Feature inFeature = null;
		if (flow.getInEnd() != null)
			inFeature = flow.getInEnd().getFeature();
		Feature outFeature = null;
		if (flow.getOutEnd() != null)
			outFeature = flow.getOutEnd().getFeature();
		if (inFeature instanceof BusAccess || inFeature instanceof SubprogramAccess
				|| inFeature instanceof SubprogramGroupAccess || inFeature instanceof AbstractFeature) {
			error(flow.getInEnd(), '\''
					+ (flow.getInEnd().getContext() != null ? flow.getInEnd().getContext().getName() + '.' : "")
					+ inFeature.getName() + "' must be a port, parameter, data access, or feature group.");
		}
		if (outFeature instanceof BusAccess || outFeature instanceof SubprogramAccess
				|| outFeature instanceof SubprogramGroupAccess || outFeature instanceof AbstractFeature) {
			error(flow.getOutEnd(), '\''
					+ (flow.getOutEnd().getContext() != null ? flow.getOutEnd().getContext().getName() + '.' : "")
					+ outFeature.getName() + "' must be a port, parameter, data access, or feature group.");
		}
	}

	/**
	 * Checks the direction of features used in flow specs.
	 * Section 10.1 Legality Rules L1, L2, L3, L4, L5, L6, L7, L8, L9, and L10
	 * @param flow
	 */
	private void checkFlowFeatureDirection(FlowSpecification flow) {
		FlowEnd inEnd = flow.getInEnd();
		if (inEnd != null){
			Feature inFeature = inEnd.getFeature();
			if (!Aadl2Util.isNull(inFeature)){
				Context inCxt = inEnd.getContext();
				boolean oppositeDirection = false;
				if (inCxt instanceof FeatureGroup){
					FeatureGroup fg = (FeatureGroup) inCxt;
					if (fg.isInverse()){
						oppositeDirection = ! oppositeDirection;
					}
					FeatureGroupType fgt = fg.getAllFeatureGroupType();
					if (!Aadl2Util.isNull(fgt)){
						if (!Aadl2Util.isNull(fgt.getInverse())&&fgt.getOwnedFeatures().isEmpty()){
							// change direction since the FGT is an inverse and does not have features, i.e., the inEnd points to a
							// feature in the inverse of FGT
							oppositeDirection = ! oppositeDirection;
						}
					}
				}
				checkIncomingFeatureDirection(inFeature, flow, oppositeDirection,true);
			}
		}
		FlowEnd outEnd = flow.getOutEnd();
		if (outEnd != null){
			Feature outFeature = outEnd.getFeature();
			if (!Aadl2Util.isNull(outFeature)){
				Context outCxt = outEnd.getContext();
				boolean oppositeDirection = false;
				if (outCxt instanceof FeatureGroup){
					FeatureGroup fg = (FeatureGroup) outCxt;
					if (fg.isInverse()){
						oppositeDirection = ! oppositeDirection;
					}
					FeatureGroupType fgt = fg.getAllFeatureGroupType();
					if (!Aadl2Util.isNull(fgt)){
						if (!Aadl2Util.isNull(fgt.getInverse())&&fgt.getOwnedFeatures().isEmpty()){
							// change direction since the FGT is an inverse and does not have features, i.e., the inEnd points to a
							// feature in the inverse of FGT
							oppositeDirection = ! oppositeDirection;
						}
					}
				}
				String hi = "hi";
				checkOutgoingFeatureDirection(outFeature, flow, oppositeDirection,true);
			}
		}
	}
	
	private boolean checkIncomingFeatureDirection(Feature inFeature, FlowSpecification flow, boolean inverseOf, boolean report){
		//Test for L2
		if (inFeature instanceof Port || inFeature instanceof Parameter) {
			DirectionType fDirection = ((DirectedFeature) inFeature).getDirection();
			if (inverseOf) 	fDirection = fDirection.getInverseDirection();
			if (!fDirection.incoming()) {
				if (report)
				error(flow.getInEnd(), '\''
						+ (flow.getInEnd().getContext() != null ? flow.getInEnd().getContext().getName() + '.' : "")
						+ inFeature.getName() + "' must be an in or in out feature.");
				return false;
			} else {
				return true;
			}
		}
		//Test for L4
		else if (inFeature instanceof DataAccess) {
			Property accessRightProperty = GetProperties.lookupPropertyDefinition(flow, MemoryProperties._NAME,
					MemoryProperties.ACCESS_RIGHT);
			EnumerationLiteral accessRightValue = PropertyUtils.getEnumLiteral(inFeature, accessRightProperty);
			String accessrightname = accessRightValue.getName();
			if (inverseOf) 	accessrightname = MemoryProperties.getInverseDirection(accessrightname);
			if (!accessrightname.equalsIgnoreCase(MemoryProperties.READ_ONLY)
					&& !accessrightname.equalsIgnoreCase(MemoryProperties.READ_WRITE)) {
				if (report)
				error(flow.getInEnd(), '\''
						+ (flow.getInEnd().getContext() != null ? flow.getInEnd().getContext().getName() + '.' : "")
						+ inFeature.getName() + "' must have an access right of Read_Only or Read_Write.");
				return false;
			} else {
				return true;
			}
		}
		//Test for L6
		else if (inFeature instanceof FeatureGroup) {
			FeatureGroupType fgt = ((FeatureGroup) inFeature).getAllFeatureGroupType();
			boolean inInverseof = ((FeatureGroup)inFeature).isInverse();
			if (!Aadl2Util.isNull(fgt)) {
				if (!Aadl2Util.isNull(fgt.getInverse())&& fgt.getOwnedFeatures().isEmpty()){
					inInverseof = ! inInverseof;
				}
				if( fgt.getAllFeatures().isEmpty()) return true;
				for (Feature f : fgt.getAllFeatures()) {
					// check to see if there is at least one incoming feature in the feature group
					if (checkIncomingFeatureDirection(f, flow,inInverseof?!inverseOf:inverseOf,false)){
						return true;
					}
				}
				if (report) {
					error(flow.getInEnd(),
							'\''
									+ (flow.getInEnd().getContext() != null ? flow.getInEnd().getContext().getName() + '.'
											: "")
									+ inFeature.getName()
									+ "' must contain at least one in or in out port or parameter, at least data access with an access right of Read_Only or Read_Write, or be empty.");
					return false;
				}
			}
			return true;
		}
		return false;

	}
	
	private boolean checkOutgoingFeatureDirection(Feature outFeature, FlowSpecification flow, boolean inverseOf,boolean report){
		//Test for L3
		if (outFeature instanceof Port || outFeature instanceof Parameter) {
			DirectionType fDirection = ((DirectedFeature) outFeature).getDirection();
			if (inverseOf) 	fDirection = fDirection.getInverseDirection();

			if (!fDirection.outgoing()) {
				if (report) 
					error(flow.getOutEnd(), '\''
						+ (flow.getOutEnd().getContext() != null ? flow.getOutEnd().getContext().getName() + '.' : "")
						+ outFeature.getName() + "' must be an out or in out feature.");
				return false;
			} else {
				return true;
			}
		}
		//Test for L5
		else if (outFeature instanceof DataAccess) {
			Property accessRightProperty = GetProperties.lookupPropertyDefinition(flow, MemoryProperties._NAME,
					MemoryProperties.ACCESS_RIGHT);
			EnumerationLiteral accessRightValue = PropertyUtils.getEnumLiteral(outFeature, accessRightProperty);
			String accessrightname = accessRightValue.getName();

			if (!accessrightname.equalsIgnoreCase(MemoryProperties.WRITE_ONLY)
					&& !accessrightname.equalsIgnoreCase(MemoryProperties.READ_WRITE)) {
				if (report)
					error(flow.getOutEnd(), '\''
						+ (flow.getOutEnd().getContext() != null ? flow.getOutEnd().getContext().getName() + '.' : "")
						+ outFeature.getName() + "' must have an access right of Write_Only or Read_Write.");
				return false;
			} else {
				return true;
			}
		}
		//Test for L7
		else if (outFeature instanceof FeatureGroup) {
			FeatureGroupType fgt = ((FeatureGroup) outFeature).getAllFeatureGroupType();
			boolean outInverseof = ((FeatureGroup)outFeature).isInverse();
			if (fgt != null) {
				if (!Aadl2Util.isNull(fgt.getInverse())&& fgt.getOwnedFeatures().isEmpty()){
					// change direction only if inverse of and no features. Otherwise, we check features in this fgt
					outInverseof = ! outInverseof;
					// set up inverse fgt to be examined for features of the correct direction
					fgt = fgt.getInverse();
				}
				if( fgt.getAllFeatures().isEmpty()) return true;
				for (Feature f : fgt.getAllFeatures()) {
					if (checkOutgoingFeatureDirection(f, flow,outInverseof?!inverseOf:inverseOf,false)){
						return true;
					}
				}
				if (report) 
					error(flow.getOutEnd(),
							'\''
							+ (flow.getOutEnd().getContext() != null ? flow.getOutEnd().getContext().getName() + '.'
									: "")
									+ outFeature.getName()
									+ "' must contain at least one out or in out port or parameter, at least one data access with an access right of Write_Only or Read_Write, or be empty.");
				return false;
			} else {
				return true;
			}
		}
		return false;

	}

	/**
	 * @param pn
	 */
	private void checkPropertyDefinition(final Property pn) {
		// Check the type correctness of the default value, if any
		typeCheckPropertyValues(pn.getPropertyType(), pn.getDefaultValue(),pn,pn.getQualifiedName());
		checkAppliesTo(pn);
	}
	
	/**
	 * check that the Meta model names exist
	 * @param pd
	 */
	private void checkAppliesTo(final Property pd){
		for (PropertyOwner appliesTo : pd.getAppliesTos()) {
			//	for (MetaclassReference metaclassReference : property.getAppliesToMetaclasses())
			try {
				if (appliesTo instanceof MetaclassReference
						&& ((MetaclassReference) appliesTo).getMetaclass() != null) {
				}
			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
				String msg = e.getMessage();
				error(pd, msg);//"Meta class reference "+((MetaclassReference) appliesTo).getMetaclass().get+" not found in Meta model.");
			}
		}
	}

	private void checkPropertyConstant(final PropertyConstant pc) {
		/*
		 * Check the type correctness of the values. The parser enforces some of
		 * this, but can't do it if the type is given by reference, and it
		 * cannot check that a int or real is within range.
		 */
		typeCheckPropertyValues(pc.getPropertyType(), pc.getConstantValue(),pc,pc.getQualifiedName());
	}

//
//	/**
//	 * check property associations for the aObject element
//	 * @param element aObject. It may not have a Properties object.
//	 */
//	private void checkPropertyAssocs(
//		final NamedElement element, final boolean isSubcomponent) {
//		final List assocs;
//		if (element instanceof Classifier) {
//			/* 15 February 2007: Not sure this what should really be done.
//			 * This breaks list +=> for one thing.  Taking this out for
//			 * the moment; it was put in on 6 Feb 2007.
//			 */
////			assocs = ((Classifier) element).getAllPropertyAssociations();
//			assocs = element.getOwnedPropertyAssociations();
//		} else {
//			assocs = element.getOwnedPropertyAssociations();
//		}
//		if (assocs == null) return;
//
//		// map: PropertyDeclaration -> set of mode-binding pairs
//		final Map propsToModes = new HashMap();
//		// 2 level map: PropertyDeclaration -> EList of property holders -> set of mode-binding pairs
//		final Map containedPropsToModes = new HashMap();
//		for (final Iterator iter = assocs.iterator(); iter.hasNext(); ) {
//			final PropertyAssociation pa = (PropertyAssociation) iter.next();
//			// check only if property name was resolved
//			final Property pd = pa.getProperty();
//			if (pd != null)	{
//				/* This is where we should check constant property
//				 * associations, but it's too much work to do that.
//				 */
//				if (pa.isConstant()) {
//					warning(pa,
//							"Constant property associations are not checked");
//				}
//				
//				/* Check that the association follows the prop's  "applies to".
//				 */
//				checkAssociationAppliesTo(element, pa);
//
//				/* Check that this PA hasn't already associated a value with
//				 * the property (modulo modes).
//				 */
//				checkUniqueAssociation(element, propsToModes, pa);
//				checkUniqueContainedAssociation(element, containedPropsToModes, pa);
//				
//				// Check that the association has good "in modes"
//				checkInModes(element, pa);
//				
//				// Check that the association is type correct
//				typeCheckPropertyValues(pd.getPropertyType(), pd.isList(), pa.getOwnedValues());
//
//				// Check "list of" issues
//				checkListOf(pa);
//
//				// Check that Overflow_handling_protocol and Queue_size only
//				// appear on in event ports and in event data ports
//				checkPortProperties(element, pa);
//			}
//		}
//	}
//
//	private static class ModeBindingPair {
//		private final Mode mode;
//		private final ComponentClassifier binding;
//		
//		public ModeBindingPair(final Mode m, final ComponentClassifier cc) {
//			mode = m;
//			binding = cc;
//		}
//		
//		public boolean equals(final Object o) {
//			if (o instanceof ModeBindingPair) {
//				final ModeBindingPair mbp = (ModeBindingPair) o;
//				return ((mode == null && mbp.mode == null) || mode.equals(mbp.mode))
//					&& ((binding == null && mbp.binding == null) || binding.equals(mbp.binding));
//			} else {
//				return false;
//			}
//		}
//		
//		public int hashCode() {
//			final int hc1 = (mode == null) ? 0 : mode.hashCode();
//			final int hc2 = (binding == null) ? 0 : binding.hashCode();
//			return hc1 * 13 + hc2;
//		}
//		
//		public String toString() {
//			final String s1 = (mode == null) ? "none" : mode.getName();
//			final String s2 = (binding == null) ? "none" : binding.getQualifiedName();
//			return "(" + s1 + ", " + s2 + ")";
//		}
//	}
//	
//	private static final List NULL_LIST = Collections.singletonList(null);
//	
//	private static Set createPairs(final List modes, final List bindings) {
//		final Set pairs = new HashSet();
//		
//		final List modesList = (modes == null || modes.isEmpty()) ? NULL_LIST : modes;
//		final List bindingsList = (bindings == null || bindings.isEmpty()) ? NULL_LIST : bindings;		
//		for (final Iterator i = modesList.iterator(); i.hasNext();) {
//			final Mode mode = (Mode) i.next();
//			for (final Iterator j = bindingsList.iterator(); j.hasNext();) {
//				final ComponentClassifier binding = (ComponentClassifier) j.next();
//				pairs.add(new ModeBindingPair(mode, binding));
//			}
//		}
//		return pairs;
//	}
//	
//	private void checkUniqueAssociation(
//			final NamedElement ph, final Map propsToModes, final PropertyAssociation pa) {
//		if (pa.getAppliesTos() == null || pa.getAppliesTos().size() == 0) {
//			final Property pd = pa.getProperty();
//			Set definedInModes = (Set) propsToModes.get(pd);
//			if (definedInModes == null) {
//				definedInModes = new HashSet();
//				propsToModes.put(pd, definedInModes);
//			}
//			checkForDuplicateAssociation(ph, pa, definedInModes);
//		}
//	}
//
//	private void checkUniqueContainedAssociation(
//			final NamedElement ph, 
//			final Map containedPropsToModes, final PropertyAssociation pa) {
//		final String appliesTo = unparseContainedAppliesToPath(pa);
//		if (appliesTo.length() > 0) {
//			final Property pd = pa.getProperty();
//			Map subMap = (Map) containedPropsToModes.get(pd);
//			if (subMap == null) {
//				subMap = new HashMap();
//				containedPropsToModes.put(pd, subMap);
//			}
//			Set definedInModes = (Set) subMap.get(appliesTo);
//			if (definedInModes == null) {
//				definedInModes = new HashSet();
//				subMap.put(appliesTo, definedInModes);
//			}
//			checkForDuplicateAssociation(ph, pa, definedInModes);
//		}
//	}
//
//	/**
//	 * @param pa
//	 * @param definedInModes
//	 */
//	private void checkForDuplicateAssociation(
//			final NamedElement ph, 
//			final PropertyAssociation pa, Set definedInModes) {
//		final EList inBinding = pa.getInBindings();
//		final Set pairs = createPairs(inModes, inBinding);
//		final Set alreadyDefined = new HashSet();
//		for (final Iterator i = pairs.iterator(); i.hasNext(); ) {
//			final ModeBindingPair mbp = (ModeBindingPair) i.next();
//			if (definedInModes.contains(mbp)) {
//				alreadyDefined.add(mbp);
//			}
//		}
//		definedInModes.addAll(pairs);
//		
//		if (!alreadyDefined.isEmpty()) {
//			final String appliesTo = unparseContainedAppliesToPath(pa);
//			for (final Iterator i = alreadyDefined.iterator(); i.hasNext();) {
//				final ModeBindingPair mbp = (ModeBindingPair) i.next();
//				error(ph,
//						"\"" + ph.getName() + "\" has multiple property associations for \"" + 
//						pa.getProperty().getQualifiedName() + "\"" +
//						((appliesTo.length() == 0) ? "" : (" applying to " + appliesTo)) +
//						((mbp.mode == null) ? "" : (" in mode " + mbp.mode.getName())) +
//						((mbp.binding == null) ? "" : (" in binding " + mbp.binding.getQualifiedName())));
//			}
//		}
//	}
//
//	private String unparseContainedAppliesToPath(final PropertyAssociation pa) {
//		final List appliesTo = pa.getAppliesTos();
//		final StringBuffer sb = new StringBuffer();
//		for (final Iterator i = appliesTo.iterator(); i.hasNext();) {
//			final NamedElement ne = (NamedElement) i.next();
//			sb.append(ne.getName());
//			if (i.hasNext()) sb.append('.');
//		}
//		return sb.toString();
//	}
//	
//	/**
//	 * Checks legality rule from Section 4.5 Subcomponents:
//	 *
//	 * <blockquote>If the subcomponent declaration contains an in_modes
//	 * statement and any of its property associations also contains an in_modes
//	 * statement, then the modes named in the property association must be a
//	 * subset of those named in the subcomponent declaration. </blockquote>
//	 *
//	 * @param element
//	 *            The property holder whose in_modes should be checked. This
//	 *            method is only interested in SubprogramSubcomponent,
//	 *            Subcomponent, Connection, and FlowSequence elements.
//	 *
//	 * @param pa
//	 *            The particular property association whose in_modes must be a
//	 *            subset of the modes in which <code>element</code> exists.
//	 */
//	private void checkInModes(final NamedElement element, final PropertyAssociation pa) {
//		final EList innerModes = pa.getInModes();
//		if (innerModes != null) {
//			EList outerModes = null;
//			if (element instanceof SubprogramSubcomponent) {
//				/* We need to treat SubprogramSubcomponents specially because
//				 * the modes they exist in are controlled by the CallSequence.
//				 */
//				final SubprogramCallSequence cs = (SubprogramCallSequence) element.eContainer();
//				outerModes = cs.getInModes();
//			} else if (element instanceof Subcomponent
//					|| element instanceof Connection
//					|| element instanceof FlowElement) {
//				outerModes = ((ModalElement) element).getInModes();
//			}
//
//			if (outerModes != null) {
//				// Empty set means that no modes were declared, i.e., all modes
//				if (!outerModes.isEmpty()
//						&& !outerModes.containsAll(innerModes)) {
//					error(pa,
//							"Property association has more modes than its container");
//				}
//			}
//		}
//	}
//
//	/**
//	 * Check that non-list properties do not get associated with a list of
//	 * values:
//	 *
//	 * <blockquote>
//	 * If the property declaration for the associated property name
//	 * does not contain the reserved words list of, the property_value must be a
//	 * single_property_value. If the property declaration for the associated
//	 * property name contains the reserved words list of, the property_value can
//	 * be a single_property_value, which is interpreted to be a list of one
//	 * value.
//	 *
//	 * The property association operator +=> must only be used if the property
//	 * declaration for the associated property name contains the reserved words
//	 * list of.
//	 * </blockquote>
//	 *
//	 * @param pa
//	 */
//	private void checkListOf(final PropertyAssociation pa) {
//		final Property pn = pa.getProperty();
//		if (pn == null)
//			return;
//		final EList value = pa.getOwnedValues();
//		if (!pn.isList()) {
//			// Must only have one value associated with the property
//			if (value == null || value.size() == 0) {
//				error(pa,
//						"Scalar properties must have a value");
//			} else if (value.size() > 1) {
//				error(pa,
//						"Scalar properties cannot be associated with a list");
//			}
//			if (pa.isAppend()) {
//				error(pa,
//						"Cannot append to a scalar property");
//			}
//		}
//	}
//
//	/**
//	 * Checks contraints on the <code>Overflow_Handling_Protocol</code>,
//	 * <code>Queue_Processing_Protocol</code>, <code>Dequeue_Protocol</code>,
//	 * <code>Queue_Size</code> properties as specificed in Section 8.1 Ports:
//	 *
//	 * <blockquote>The property names Overflow_Handling_Protocol,
//	 * Queue_Processing_Protocol, Dequeue_Protocol, and Queue_Size
//	 * may only appear in property associations for in event ports and in event
//	 * data ports. </blockquote>
//	 * 
//	 * <p>The <code>applies to</code> clause in the property definition
//	 * already makes sure they only appear on <code>event port</code> 
//	 * and <code>event data port</code> (and <code>subprogram</code> for all
//	 * but <code>Dequeue_Protocol</code>).  So here we check that the port 
//	 * is an <code>in port</code>.
//	 *
//	 * @param ph
//	 *            The property holder
//	 * @param pa
//	 *            The property association to check
//	 */
//	private void checkPortProperties(final NamedElement ph, final PropertyAssociation pa) {
//		if (ph instanceof EventPort || ph instanceof EventDataPort) {
//			final Property pd = pa.getProperty();
//			if (pd == PropertiesLinkingService.getPropertiesLinkingService(ph).findPropertyDefinition(ph,CommunicationProperties.OVERFLOW_HANDLING_PROTOCOL) ||
//					pd.getName().equalsIgnoreCase(CommunicationProperties.QUEUE_SIZE) ||
//					pdgetName().equalsIgnoreCase(CommunicationProperties.UE_PROCESSING_PROTOTOCOL) ||
//					pd == DEQUEUE_PROTOCOL_PD) {
//				final DirectionType dir = ((Port) ph).getDirection();
//				if (dir != DirectionType.IN) {
//					error(pa,
//							"Property \"" + pd.getName() +
//							"\" is only allowed on in event ports and in event data ports");
//				}
//			}
//		}
//	}
//
//

	private static FeatureType getFeatureType(Feature feature) {
		if (feature instanceof DataPort) {
			switch (((DataPort) feature).getDirection()) {
			case IN:
				return FeatureType.IN_DATA_PORT;
			case OUT:
				return FeatureType.OUT_DATA_PORT;
			case IN_OUT:
				return FeatureType.IN_OUT_DATA_PORT;
			}
		} else if (feature instanceof EventPort) {
			switch (((EventPort) feature).getDirection()) {
			case IN:
				return FeatureType.IN_EVENT_PORT;
			case OUT:
				return FeatureType.OUT_EVENT_PORT;
			case IN_OUT:
				return FeatureType.IN_OUT_EVENT_PORT;
			}
		} else if (feature instanceof EventDataPort) {
			switch (((EventDataPort) feature).getDirection()) {
			case IN:
				return FeatureType.IN_EVENT_DATA_PORT;
			case OUT:
				return FeatureType.OUT_EVENT_DATA_PORT;
			case IN_OUT:
				return FeatureType.IN_OUT_EVENT_DATA_PORT;
			}
		} else if (feature instanceof FeatureGroup)
			return FeatureType.FEATURE_GROUP;
		else if (feature instanceof DataAccess) {
			switch (((DataAccess) feature).getKind()) {
			case PROVIDES:
				return FeatureType.PROVIDES_DATA_ACCESS;
			case REQUIRES:
				return FeatureType.REQUIRES_DATA_ACCESS;
			}
		} else if (feature instanceof SubprogramAccess) {
			switch (((SubprogramAccess) feature).getKind()) {
			case PROVIDES:
				return FeatureType.PROVIDES_SUBPROGRAM_ACCESS;
			case REQUIRES:
				return FeatureType.REQUIRES_SUBPROGRAM_ACCESS;
			}
		} else if (feature instanceof SubprogramGroupAccess) {
			switch (((SubprogramGroupAccess) feature).getKind()) {
			case PROVIDES:
				return FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS;
			case REQUIRES:
				return FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS;
			}
		} else if (feature instanceof BusAccess) {
			switch (((BusAccess) feature).getKind()) {
			case PROVIDES:
				return FeatureType.PROVIDES_BUS_ACCESS;
			case REQUIRES:
				return FeatureType.REQUIRES_BUS_ACCESS;
			}
		} else if (feature instanceof AbstractFeature)
			return FeatureType.ABSTRACT_FEATURE;
		else if (feature instanceof Parameter)
			return FeatureType.PARAMETER;
		return null;
	}

	private static String getKeywordForAccessType(AccessType accessType) {
		switch (accessType) {
		case PROVIDES:
			return "provides";
		case REQUIRES:
			return "requires";
		default:
			throw new AssertionError("Unhandled enum literal: " + accessType);
		}
	}

	static {
		HashMap<ComponentCategory, Set<FeatureType>> featuresForTypes = new HashMap<ComponentCategory, Set<FeatureType>>();

		// Abstract Types
		FeatureType[] featureTypesArray = new FeatureType[] { FeatureType.IN_DATA_PORT, FeatureType.OUT_DATA_PORT,
				FeatureType.IN_OUT_DATA_PORT, FeatureType.IN_EVENT_PORT, FeatureType.OUT_EVENT_PORT,
				FeatureType.IN_OUT_EVENT_PORT, FeatureType.IN_EVENT_DATA_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.IN_OUT_EVENT_DATA_PORT, FeatureType.FEATURE_GROUP, FeatureType.PROVIDES_DATA_ACCESS,
				FeatureType.PROVIDES_SUBPROGRAM_ACCESS, FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS,
				FeatureType.PROVIDES_BUS_ACCESS, FeatureType.REQUIRES_DATA_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_ACCESS, FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS,
				FeatureType.REQUIRES_BUS_ACCESS, FeatureType.ABSTRACT_FEATURE };
		Set<FeatureType> featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.ABSTRACT, Collections.unmodifiableSet(featureTypesSet));

		// Data Types
		featureTypesArray = new FeatureType[] { FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_ACCESS, FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS,
				FeatureType.FEATURE_GROUP, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.DATA, Collections.unmodifiableSet(featureTypesSet));

		// Subprogram Types
		featureTypesArray = new FeatureType[] { FeatureType.OUT_EVENT_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.FEATURE_GROUP, FeatureType.REQUIRES_DATA_ACCESS, FeatureType.REQUIRES_SUBPROGRAM_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS, FeatureType.PARAMETER, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.SUBPROGRAM, Collections.unmodifiableSet(featureTypesSet));

		// Subprogram Group Types
		featureTypesArray = new FeatureType[] { FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_ACCESS, FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS,
				FeatureType.FEATURE_GROUP, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.SUBPROGRAM_GROUP, Collections.unmodifiableSet(featureTypesSet));

		// Thread Types
		featureTypesArray = new FeatureType[] { FeatureType.IN_DATA_PORT, FeatureType.OUT_DATA_PORT,
				FeatureType.IN_OUT_DATA_PORT, FeatureType.IN_EVENT_PORT, FeatureType.OUT_EVENT_PORT,
				FeatureType.IN_OUT_EVENT_PORT, FeatureType.IN_EVENT_DATA_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.IN_OUT_EVENT_DATA_PORT, FeatureType.FEATURE_GROUP, FeatureType.PROVIDES_DATA_ACCESS,
				FeatureType.REQUIRES_DATA_ACCESS, FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_ACCESS, FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.THREAD, Collections.unmodifiableSet(featureTypesSet));

		// Thread Group Types
		featureTypesArray = new FeatureType[] { FeatureType.IN_DATA_PORT, FeatureType.OUT_DATA_PORT,
				FeatureType.IN_OUT_DATA_PORT, FeatureType.IN_EVENT_PORT, FeatureType.OUT_EVENT_PORT,
				FeatureType.IN_OUT_EVENT_PORT, FeatureType.IN_EVENT_DATA_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.IN_OUT_EVENT_DATA_PORT, FeatureType.FEATURE_GROUP, FeatureType.PROVIDES_DATA_ACCESS,
				FeatureType.REQUIRES_DATA_ACCESS, FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_ACCESS, FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.THREAD_GROUP, Collections.unmodifiableSet(featureTypesSet));

		// Process Types
		featureTypesArray = new FeatureType[] { FeatureType.IN_DATA_PORT, FeatureType.OUT_DATA_PORT,
				FeatureType.IN_OUT_DATA_PORT, FeatureType.IN_EVENT_PORT, FeatureType.OUT_EVENT_PORT,
				FeatureType.IN_OUT_EVENT_PORT, FeatureType.IN_EVENT_DATA_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.IN_OUT_EVENT_DATA_PORT, FeatureType.FEATURE_GROUP, FeatureType.PROVIDES_DATA_ACCESS,
				FeatureType.REQUIRES_DATA_ACCESS, FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_ACCESS, FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.PROCESS, Collections.unmodifiableSet(featureTypesSet));

		// Processor Types
		featureTypesArray = new FeatureType[] { FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS, FeatureType.IN_DATA_PORT, FeatureType.OUT_DATA_PORT,
				FeatureType.IN_OUT_DATA_PORT, FeatureType.IN_EVENT_PORT, FeatureType.OUT_EVENT_PORT,
				FeatureType.IN_OUT_EVENT_PORT, FeatureType.IN_EVENT_DATA_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.IN_OUT_EVENT_DATA_PORT, FeatureType.FEATURE_GROUP, FeatureType.REQUIRES_BUS_ACCESS,
				FeatureType.PROVIDES_BUS_ACCESS, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.PROCESSOR, Collections.unmodifiableSet(featureTypesSet));

		// Virtual Processor Types
		featureTypesArray = new FeatureType[] { FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS, FeatureType.IN_DATA_PORT, FeatureType.OUT_DATA_PORT,
				FeatureType.IN_OUT_DATA_PORT, FeatureType.IN_EVENT_PORT, FeatureType.OUT_EVENT_PORT,
				FeatureType.IN_OUT_EVENT_PORT, FeatureType.IN_EVENT_DATA_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.IN_OUT_EVENT_DATA_PORT, FeatureType.FEATURE_GROUP, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.VIRTUAL_PROCESSOR, Collections.unmodifiableSet(featureTypesSet));

		// Memory Types
		featureTypesArray = new FeatureType[] { FeatureType.REQUIRES_BUS_ACCESS, FeatureType.PROVIDES_BUS_ACCESS,
				FeatureType.FEATURE_GROUP, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.MEMORY, Collections.unmodifiableSet(featureTypesSet));

		// Bus Types
		featureTypesArray = new FeatureType[] { FeatureType.REQUIRES_BUS_ACCESS, FeatureType.FEATURE_GROUP,
				FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.BUS, Collections.unmodifiableSet(featureTypesSet));

		// Virtual Bus Types
		featureTypesSet = Collections.emptySet();
		featuresForTypes.put(ComponentCategory.VIRTUAL_BUS, featureTypesSet);

		// Device Types
		featureTypesArray = new FeatureType[] { FeatureType.IN_DATA_PORT, FeatureType.OUT_DATA_PORT,
				FeatureType.IN_OUT_DATA_PORT, FeatureType.IN_EVENT_PORT, FeatureType.OUT_EVENT_PORT,
				FeatureType.IN_OUT_EVENT_PORT, FeatureType.IN_EVENT_DATA_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.IN_OUT_EVENT_DATA_PORT, FeatureType.FEATURE_GROUP, FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS, FeatureType.REQUIRES_BUS_ACCESS,
				FeatureType.PROVIDES_BUS_ACCESS, FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.DEVICE, Collections.unmodifiableSet(featureTypesSet));

		// System Types
		featureTypesArray = new FeatureType[] { FeatureType.IN_DATA_PORT, FeatureType.OUT_DATA_PORT,
				FeatureType.IN_OUT_DATA_PORT, FeatureType.IN_EVENT_PORT, FeatureType.OUT_EVENT_PORT,
				FeatureType.IN_OUT_EVENT_PORT, FeatureType.IN_EVENT_DATA_PORT, FeatureType.OUT_EVENT_DATA_PORT,
				FeatureType.IN_OUT_EVENT_DATA_PORT, FeatureType.FEATURE_GROUP, FeatureType.PROVIDES_SUBPROGRAM_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_ACCESS, FeatureType.PROVIDES_SUBPROGRAM_GROUP_ACCESS,
				FeatureType.REQUIRES_SUBPROGRAM_GROUP_ACCESS, FeatureType.PROVIDES_BUS_ACCESS,
				FeatureType.REQUIRES_BUS_ACCESS, FeatureType.PROVIDES_DATA_ACCESS, FeatureType.REQUIRES_DATA_ACCESS,
				FeatureType.ABSTRACT_FEATURE };
		featureTypesSet = new HashSet<FeatureType>();
		for (FeatureType featureType : featureTypesArray)
			featureTypesSet.add(featureType);
		featuresForTypes.put(ComponentCategory.SYSTEM, Collections.unmodifiableSet(featureTypesSet));

		acceptableFeaturesForTypes = Collections.unmodifiableMap(featuresForTypes);

		HashMap<ComponentCategory, Set<ComponentCategory>> categoriesForImplementations = new HashMap<ComponentCategory, Set<ComponentCategory>>();

		// Abstract Implementations
		HashSet<ComponentCategory> categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory currentCategory : ComponentCategory.values())
			categoriesSet.add(currentCategory);
		categoriesForImplementations.put(ComponentCategory.ABSTRACT, Collections.unmodifiableSet(categoriesSet));

		// Data Implementations
		ComponentCategory[] categoriesArray = new ComponentCategory[] { ComponentCategory.DATA,
				ComponentCategory.SUBPROGRAM, ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.DATA, Collections.unmodifiableSet(categoriesSet));

		// Subprogram Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.DATA, ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.SUBPROGRAM, Collections.unmodifiableSet(categoriesSet));

		// Subprogram Group Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.SUBPROGRAM, ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations
				.put(ComponentCategory.SUBPROGRAM_GROUP, Collections.unmodifiableSet(categoriesSet));

		// Thread Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.DATA, ComponentCategory.SUBPROGRAM,
				ComponentCategory.SUBPROGRAM_GROUP, ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.THREAD, Collections.unmodifiableSet(categoriesSet));

		// Thread Group Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.DATA, ComponentCategory.SUBPROGRAM,
				ComponentCategory.SUBPROGRAM_GROUP, ComponentCategory.THREAD, ComponentCategory.THREAD_GROUP,
				ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.THREAD_GROUP, Collections.unmodifiableSet(categoriesSet));

		// Process Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.DATA, ComponentCategory.SUBPROGRAM,
				ComponentCategory.SUBPROGRAM_GROUP, ComponentCategory.THREAD, ComponentCategory.THREAD_GROUP,
				ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.PROCESS, Collections.unmodifiableSet(categoriesSet));

		// Processor Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.VIRTUAL_PROCESSOR, ComponentCategory.MEMORY,
				ComponentCategory.BUS, ComponentCategory.VIRTUAL_BUS, ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.PROCESSOR, Collections.unmodifiableSet(categoriesSet));

		// Virtual Processor Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.VIRTUAL_PROCESSOR, ComponentCategory.VIRTUAL_BUS,
				ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.VIRTUAL_PROCESSOR,
				Collections.unmodifiableSet(categoriesSet));

		// Memory Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.MEMORY, ComponentCategory.BUS,
				ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.MEMORY, Collections.unmodifiableSet(categoriesSet));

		// Bus Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.VIRTUAL_BUS, ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.BUS, Collections.unmodifiableSet(categoriesSet));

		// Virtual Bus Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.VIRTUAL_BUS, ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.VIRTUAL_BUS, Collections.unmodifiableSet(categoriesSet));

		// Device Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.BUS, ComponentCategory.VIRTUAL_BUS,
				ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.DEVICE, Collections.unmodifiableSet(categoriesSet));

		// System Implementations
		categoriesArray = new ComponentCategory[] { ComponentCategory.DATA, ComponentCategory.SUBPROGRAM,
				ComponentCategory.SUBPROGRAM_GROUP, ComponentCategory.PROCESS, ComponentCategory.PROCESSOR,
				ComponentCategory.VIRTUAL_PROCESSOR, ComponentCategory.MEMORY, ComponentCategory.BUS,
				ComponentCategory.VIRTUAL_BUS, ComponentCategory.DEVICE, ComponentCategory.SYSTEM,
				ComponentCategory.ABSTRACT };
		categoriesSet = new HashSet<ComponentCategory>();
		for (ComponentCategory category : categoriesArray)
			categoriesSet.add(category);
		categoriesForImplementations.put(ComponentCategory.SYSTEM, Collections.unmodifiableSet(categoriesSet));

		acceptableSubcomponentCategoriesForImplementations = Collections.unmodifiableMap(categoriesForImplementations);

		Map<EClass, String> featureClassNamesWithArticle = new HashMap<EClass, String>();
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getAbstractFeature(), "an abstract feature");
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getBusAccess(), "a bus access");
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getDataAccess(), "a data access");
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getSubprogramAccess(), "a subprogram access");
		featureClassNamesWithArticle
				.put(Aadl2Package.eINSTANCE.getSubprogramGroupAccess(), "a subprogram group access");
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getFeatureGroup(), "a feature group");
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getParameter(), "a parameter");
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getDataPort(), "a data port");
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getEventDataPort(), "an event data port");
		featureClassNamesWithArticle.put(Aadl2Package.eINSTANCE.getEventPort(), "an event port");
		FEATURE_CLASS_NAMES_WITH_ARTICLE = Collections.unmodifiableMap(featureClassNamesWithArticle);
	}

	private static enum FeatureType {
		IN_DATA_PORT("an in data port"), OUT_DATA_PORT("an out data port"), IN_OUT_DATA_PORT("an in out data port"), IN_EVENT_PORT(
				"an in event port"), OUT_EVENT_PORT("an out event port"), IN_OUT_EVENT_PORT("an in out event port"), IN_EVENT_DATA_PORT(
				"an in event data port"), OUT_EVENT_DATA_PORT("an out event data port"), IN_OUT_EVENT_DATA_PORT(
				"an in out event data port"), FEATURE_GROUP("a feature group"), PROVIDES_DATA_ACCESS(
				"a provides data access"), REQUIRES_DATA_ACCESS("a requires data access"), PROVIDES_SUBPROGRAM_ACCESS(
				"a provides subprogram access"), REQUIRES_SUBPROGRAM_ACCESS("a requires subprogram access"), PROVIDES_SUBPROGRAM_GROUP_ACCESS(
				"a provides subprogram group access"), REQUIRES_SUBPROGRAM_GROUP_ACCESS(
				"a requires subprogram group access"), PROVIDES_BUS_ACCESS("a provides bus access"), REQUIRES_BUS_ACCESS(
				"a requires bus access"), ABSTRACT_FEATURE("an abstract feature"), PARAMETER("a parameter");

		private final String nameWithIndefiniteArticle;

		private FeatureType(String nameWithIndefiniteArticle) {
			this.nameWithIndefiniteArticle = nameWithIndefiniteArticle;
		}

		public String getNameWithIndefiniteArticle() {
			return nameWithIndefiniteArticle;
		}
	}

	private static final Map<ComponentCategory, Set<FeatureType>> acceptableFeaturesForTypes;
	private static final Map<ComponentCategory, Set<ComponentCategory>> acceptableSubcomponentCategoriesForImplementations;
	private static final Map<EClass, String> FEATURE_CLASS_NAMES_WITH_ARTICLE;

	/**
	 * Checks if {@code child} can extend {@code parent}.
	 */
	public static boolean canExtend(ComponentType parent, ComponentType child) {
		// null test to handle unresolved parent reference
		return (!Aadl2Util.isNull(parent))&&(parent.eClass() == child.eClass() || parent instanceof AbstractType);
	}

	/**
	 * Checks if {@code child} can extend {@code parent}.
	 */
	public static boolean canExtend(ComponentImplementation parent, ComponentImplementation child) {
		// null test to handle unresolved parent reference
		return (!Aadl2Util.isNull(parent))&&( parent.eClass() == child.eClass() || parent instanceof AbstractImplementation);
	}

	public static ComponentCategory getComponentPrototypeCategory(ComponentPrototype prototype) {
		String eClassname = prototype.eClass().getName();
		String s = eClassname.substring(0, eClassname.length() - 9);
		ComponentCategory prototypeCategory = ComponentCategory.get(s.toLowerCase());
		return prototypeCategory;
	}

	/**
	 * Check that a number type is well formed.  The range values (if any)
	 * should be such that the lower bound is not greater than the upper bound.
	 * Satisfies legality rule from Section 10.1.1:
	 *
	 * <blockquote>
	 * The value of the first numeric literal that appears in a range of a
	 * number_type must not be greater than the value of the second numeric
	 * literal.
	 * </blockquote>
	 */
	protected void checkNumberType(final NumberType nt) {
		/*
		 * NOTE: NumericResolver + Parser already make sure the bounds are both
		 * reals or both integers, as appropriate.
		 */
		final NumericRange range = nt.getRange();
		if (range == null)
			return;
		PropertyExpression lowerPE = (PropertyExpression) range.getLowerBound();
		PropertyExpression upperPE = (PropertyExpression) range.getUpperBound();
		// TODO : handle NamedValue
		if (lowerPE instanceof NamedValue) {
			if (((NamedValue) lowerPE).getNamedValue() instanceof PropertyConstant) {
				lowerPE = ((PropertyConstant) ((NamedValue) lowerPE).getNamedValue()).getConstantValue();
			}
		}
		if (upperPE instanceof NamedValue) {
			if (((NamedValue) upperPE).getNamedValue() instanceof PropertyConstant) {
				upperPE = ((PropertyConstant) ((NamedValue) upperPE).getNamedValue()).getConstantValue();
			}
		}
		NumberValue lowerNV = lowerPE instanceof NumberValue ? (NumberValue) lowerPE : null;
		NumberValue upperNV = upperPE instanceof NumberValue ? (NumberValue) upperPE : null;
		if (lowerNV != null && upperNV != null) {
			/*
			 * Check: (1) the bounds have units if the type has units; (2) the
			 * lower bounds is <= the upper bound.
			 */
			if (lowerNV instanceof NumberValue) {

			}
			if (nt.getUnitsType() != null) {
				if (lowerNV.getUnit() == null) {
					error(nt, "lower bound is missing a unit");
				}
				if (upperNV.getUnit() == null) {
					error(nt, "upper bound is missing a unit");
				}
			}
			final double lower = lowerNV.getScaledValue();
			final double upper = upperNV.getScaledValue();
			if (lower > upper) {
				error(nt, "Range lower bound is greater than range upper bound");
			}
		}
	}

	/**
	 * Check that if an aadlinteger type has units that the units have only
	 * integer multipliers.
	 */
	protected void checkAadlinteger(final AadlInteger ai) {
		final UnitsType units = ai.getUnitsType();
		if (units != null) {
			for (Iterator<EnumerationLiteral> i = units.getOwnedLiterals().iterator(); i.hasNext();) {
				final UnitLiteral ul = (UnitLiteral) i.next();
				final NumberValue factor = ul.getFactor();
				if (factor != null && !(factor instanceof IntegerLiteral)) {
					error(ai, "Integer type has unit (" + ul.getName() + ") with non-integer factor ("
							+ ul.getFactor().toString() + ")");
				}
			}
		}
	}
	
	
	@Inject
	private IGlobalScopeProvider scopeProvider;


	/**
	 * check whether there are duplicate names
	 */
	public String hasDuplicatesAadlPackage(AadlPackage context) {
			// project dependency based global scope
			List<IEObjectDescription> findings = ((Aadl2GlobalScopeProvider)scopeProvider).getDuplicates(context);
			if (!findings.isEmpty()) {
				return getNames(findings);
			}
			return null;
//			// workspace is global namespace
//			String crossRefString = ((NamedElement) context).getName();
//			List <IEObjectDescription> ielist = new Stack<IEObjectDescription>();
//			EList<IEObjectDescription> plist = EMFIndexRetrieval.getAllPackagesInWorkspace(context);
//			for (IEObjectDescription ieObjectDescription : plist) {
//				String s = ieObjectDescription.getQualifiedName().toString();
//				if (crossRefString.equalsIgnoreCase(s)) {
//					if (ieObjectDescription.getEObjectOrProxy() != context){
//						ielist.add(ieObjectDescription);
//					}
//				}
//			}
//			if( !ielist.isEmpty())  {
//				return getNames(ielist);
//			}
//		return null;
	}


	/**
	 * check whether there are duplicate names
	 */
	public String hasDuplicatesPropertySet(PropertySet propSet) {
			// project dependency based global scope
			List<IEObjectDescription> findings = ((Aadl2GlobalScopeProvider)scopeProvider).getDuplicates(propSet);
			if (!findings.isEmpty()) {
//				if (propSet.getName().equals("AADL_Project"))
//				{
//					IAadlWorkspace workspace;
//					workspace = AadlWorkspace.getAadlWorkspace();
//					IAadlProject[] aadlProjects = workspace.getOpenAadlProjects();
//					for (int i = 0 ; i < aadlProjects.length ; i++)
//					{
//						IAadlProject aadlProject = aadlProjects[i];
//						if (aadlProject.getAadlProjectFile() != null)
//						{
////							return;
//						}		
//					}
//					
//				}
				return getNames(findings);
			}
			return null;
	}

	
protected String getNames(List<IEObjectDescription> findings){
	String res = "";
	boolean doComma = false;
	for (IEObjectDescription ieObjectDescription : findings) {
		URI uri = ieObjectDescription.getEObjectURI().trimFragment();
		String pack = uri.path().replaceFirst("/resource/", "");
		res = res + (doComma?", ":"")+pack;
		doComma = true;
	}
	return res;
}


public EList<Classifier> getSelfPlusAncestors(Classifier cl) {
	EList<Classifier> cls = new BasicInternalEList<Classifier>(Classifier.class);
	cls.add(cl);
	while (cl.getExtended() != null) {
		if (cls.contains(cl.getExtended())) {
			return cls;
		}
		cl = cl.getExtended();
		cls.add(cl);
	}
	return cls;
}

public boolean hasExtendCycles(Classifier cl) {
	EList<Classifier> cls = new BasicInternalEList<Classifier>(Classifier.class);
	cls.add(cl);
	while (cl.getExtended() != null) {
		if (cls.contains(cl.getExtended())) {
			return true;
		}
		cl = cl.getExtended();
		cls.add(cl);
	}
	return false;
}
	
	public boolean sameDirection(DirectionType srcDirection,DirectionType destDirection){
		return (srcDirection.incoming() && destDirection.incoming()) ||
				(srcDirection.outgoing() && destDirection.outgoing());
	}
	

	/**
	 * Check category of source and destination
	 * Section 9.5 Legality rules L5-8
	 */
	private void checkFeatureGroupConnectionDirection(FeatureGroupConnection connection) {
		if (connection.isBidirectional()) return;
		ConnectionEnd source = (ConnectionEnd) connection.getAllSource();
		ConnectionEnd destination = (ConnectionEnd) connection.getAllDestination();
		Context srccxt = connection.getAllSourceContext();
		Context dstcxt = connection.getAllDestinationContext();
		if (!(source instanceof FeatureGroup) || !(destination instanceof FeatureGroup)) {
			error(connection, "The both ends of a feature group connection must be a feature group");
			return;
		}
		if (srccxt instanceof Subcomponent && dstcxt instanceof Subcomponent){
			// sibling to sibling
			if(((FeatureGroup)source).getDirection().equals(DirectionType.IN)){
				error(connection, "The direction of the source "+source.getName()+" of a directional feature group connection must not be in");
			} else if (((FeatureGroup)source).getDirection().equals(DirectionType.IN_OUT)){
				checkDirectionOfFeatureGroupMembers((FeatureGroup)source, DirectionType.IN,connection);
			}
			if(((FeatureGroup)destination).getDirection().equals(DirectionType.OUT)){
				error(connection, "The direction of the destination "+destination.getName()+" of a directional feature group connection must not be in");
			} else if (((FeatureGroup)destination).getDirection().equals(DirectionType.IN_OUT)){
				checkDirectionOfFeatureGroupMembers((FeatureGroup)destination, DirectionType.OUT,connection);
			}
		} else if (!(srccxt instanceof Subcomponent) ){
			// going down
			if(((FeatureGroup)source).getDirection().equals(DirectionType.OUT)){
				error(connection, "The direction of the source "+source.getName()+" of this incoming directional feature group connection must not be out");
			} else if (((FeatureGroup)source).getDirection().equals(DirectionType.IN_OUT)){
				checkDirectionOfFeatureGroupMembers((FeatureGroup)source, DirectionType.OUT,connection);
			}
			if(((FeatureGroup)destination).getDirection().equals(DirectionType.OUT)){
				error(connection, "The direction of the destination "+destination.getName()+" of this incoming directional feature group connection must not be out");
			} else if (((FeatureGroup)destination).getDirection().equals(DirectionType.IN_OUT)){
				checkDirectionOfFeatureGroupMembers((FeatureGroup)destination, DirectionType.OUT,connection);
			}
		} else if (!( dstcxt instanceof Subcomponent)){
			// going up
			if(((FeatureGroup)source).getDirection().equals(DirectionType.IN)){
				error(connection, "The direction of the source "+source.getName()+" of this outgoing directional feature group connection must not be in");
			} else if (((FeatureGroup)source).getDirection().equals(DirectionType.IN_OUT)){
				checkDirectionOfFeatureGroupMembers((FeatureGroup)source, DirectionType.IN,connection);
			}
			if(((FeatureGroup)destination).getDirection().equals(DirectionType.IN)){
				error(connection, "The direction of the destination "+destination.getName()+" of this outgoing directional feature group connection must not be in");
			} else if (((FeatureGroup)destination).getDirection().equals(DirectionType.IN_OUT)){
				checkDirectionOfFeatureGroupMembers((FeatureGroup)destination, DirectionType.IN,connection);
			}
		}
	}
	

	/**
	 * Checks legality rule 8 in section 9.5 the endpoints of a directional feature group must be consistent with the direction.
	 */
	private void checkDirectionOfFeatureGroupMembers(FeatureGroup featureGroup, DirectionType notDir, Connection conn) {
		FeatureGroupType fgt = featureGroup.getFeatureGroupType();
		if (fgt == null ) return;
		for (Feature feature : fgt.getAllFeatures()) {
			boolean invfg = featureGroup.isInverse();
			boolean invfgt = fgt.getInverse() != null &&
					fgt.getOwnedFeatures().isEmpty() &&
					Aadl2Util.isNull(fgt.getExtended());
			boolean inverse = (invfg && !invfgt)||(!invfg&& invfgt);
			if (feature instanceof DirectedFeature){
				if (((DirectedFeature) feature).getDirection() == DirectionType.IN_OUT ) break;
				boolean dirEquals = ((DirectedFeature) feature).getDirection().equals(notDir) ;
			if ((!inverse && dirEquals)||(inverse&&!dirEquals)){
					error(conn,
							"Feature "+feature.getName()+" in the referenced feature group "+featureGroup.getName()+" must not be "+notDir.getName() +" due to the direction of the connection");
				}
			}
		}
	}

	/**
	 * Checks legality rule L3 for section 9.5 (Feature Group Connections)
	 * "The following rules are supported for feature group connection declarations
	 * that represent a connection up or down the containment hierarchy:
	 * 
	 * -Classifier_Match: The source feature group type must be identical to the
	 * feature group type of the destination.  This is the default rule.
	 * 
	 * -Equivalence: An indication that the two classifiers of a connection are
	 * considered to match if they are listed in the
	 * Supported_Classifier_Equivalence_Matches property.  Matching feature group
	 * types are specified by the Supported_Classifier_Equivalence_Matches property
	 * with pairs of classifier values representing acceptable matches.  Either
	 * element of the pair can be the source or destination classifier.  Equivalence
	 * is intended to be used when the feature group types are considered to be
	 * identical, i.e., their elements match.  The
	 * Supported_Classifier_Equivalence_Matches property is declared globally as a
	 * property constant.
	 * 
	 * -Subset: An indication that the two classifiers of a connection are considered
	 * to match if the outer feature group has outcoming features that are a subset
	 * of outgoing features of the inner feature group, and if the inner feature
	 * group has incoming features that are a subset of incoming features of the
	 * outer feature group.  The pairs of features are expected to have the same name."
	 * 
	 * Checks legality rule L4 for section 9.5 (Feature Group Connections)
	 * "The following rules are supported for feature group connection declarations
	 * that represent a connection between two subcomponents, i.e., sibling component:
	 * 
	 * -Classifier_Match: The source feature group type must be the complement of the
	 * feature group type of the destination.  This is the default rule.
	 * 
	 * -Complement: An indication that the two classifiers of a connection are
	 * considered to complement if they are listed in the
	 * Supported_Classifier_Complement_Matches property.  Matching feature group types
	 * are specified by the Supported_Classifier_Complement_Matches property with pairs
	 * of classifier values representing acceptable matches.  Either element of the
	 * pair can be the source or destination classifier.  Complement is intended to be
	 * used when the feature group types are considered to be identical, i.e., their
	 * elements match.  The Supported_Classifier_Complement_Matches property is
	 * declared globally as a property constant.
	 * 
	 * -Subset: An indication that the two classifiers of a connection are considered
	 * to match if each has incoming features that are a subset of outgoing features
	 * of the other.  The pairs of features are expected to have the same name."
	 */
	private void checkFeatureGroupConnectionClassifiers(FeatureGroupConnection connection) {
		if (!(connection.getAllSource() instanceof FeatureGroup) || !(connection.getAllDestination() instanceof FeatureGroup))
			return;
		FeatureGroup source = (FeatureGroup)connection.getAllSource();
		FeatureGroup destination = (FeatureGroup)connection.getAllDestination();
		FeatureGroupType sourceType = source.getAllFeatureGroupType();
		FeatureGroupType destinationType = destination.getAllFeatureGroupType();
		if (sourceType == null || destinationType == null)
			return;
		Property classifierMatchingRuleProperty = GetProperties.lookupPropertyDefinition(connection, ModelingProperties._NAME, ModelingProperties.CLASSIFIER_MATCHING_RULE);
		EnumerationLiteral classifierMatchingRuleValue;
		try {
			classifierMatchingRuleValue = PropertyUtils.getEnumLiteral(connection, classifierMatchingRuleProperty);
		}
		catch (PropertyNotPresentException e) {
			classifierMatchingRuleValue = null;
		}
		//sibling connection
		if (connection.getAllSourceContext() instanceof Subcomponent && connection.getAllDestinationContext() instanceof Subcomponent) {
			if (classifierMatchingRuleValue == null || ModelingProperties.CLASSIFIER_MATCH.equalsIgnoreCase(classifierMatchingRuleValue.getName()) ||
					ModelingProperties.EQUIVALENCE.equalsIgnoreCase(classifierMatchingRuleValue.getName()) || ModelingProperties.CONVERSION.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
				if (classifierMatchingRuleValue != null && ModelingProperties.EQUIVALENCE.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
					warning(connection, "The classifier matching rule '" + ModelingProperties.EQUIVALENCE + "' is not supported for feature group connections between two subcomponents. Using rule '" +
							ModelingProperties.CLASSIFIER_MATCH + "' instead.");
				}
				if (classifierMatchingRuleValue != null && ModelingProperties.CONVERSION.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
					warning(connection, "The classifier matching rule '" + ModelingProperties.CONVERSION + "' is not supported for feature group connections. Using rule '" +
							ModelingProperties.CLASSIFIER_MATCH + "' instead.");
				}
				if (!testIfFeatureGroupTypesAreInverses(source, sourceType, destination, destinationType)) {
					if (testIfFeatureGroupTypeExtensionsAreInverses(source, sourceType, destination, destinationType)){
						warning(connection,"The feature group type of '" + source.getName() + "' and '" + destination.getName() + "' do not match, but their ancestors are inverse types.");
					} else {
						error(connection, "The feature group types of '" + source.getName() + "' and '" + destination.getName() + "' are not inverse types.");
					}
				}
			}
			else if (ModelingProperties.COMPLEMENT.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
				if (!testIfFeatureGroupTypesAreInverses(source, sourceType, destination, destinationType) &&
						!classifiersFoundInSupportedClassifierComplementMatchesProperty(connection, sourceType, destinationType)) {
					error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceType.getQualifiedName() + "' and '" + destinationType.getQualifiedName() +
							"') are not inverse types and they are not listed as matching classifiers in the property constant '" + AadlProject.SUPPORTED_CLASSIFIER_COMPLEMENT_MATCHES + "'.");
				}
			}
			else if (ModelingProperties.SUBSET.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
				if (!testIfFeatureGroupTypesAreInverses(source, sourceType, destination, destinationType) &&
						!checkIfFeatureGroupTypesAreSiblingSubsets(sourceType, source.isInverse(), destinationType, destination.isInverse())) {
					error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceType.getQualifiedName() + "' and '" + destinationType.getQualifiedName() +
							"') are not inverse types and they do not satisfy the Subset rule for classifier matching.  In order to satisfy this rule, the incoming features of each feature group must be a" +
							" subset of the outgoing features in the opposite feature group.");
				}
			}
		}
		else { //up or down hierarchy
			boolean cxtFGIsInverse = false;
			if (connection.getAllSourceContext() instanceof FeatureGroup){
				cxtFGIsInverse = ((FeatureGroup)connection.getAllSourceContext()).isInverse();
			} else if (connection.getAllDestinationContext() instanceof FeatureGroup){
				cxtFGIsInverse = ((FeatureGroup)connection.getAllDestinationContext()).isInverse();
			}
			if (classifierMatchingRuleValue == null || ModelingProperties.CLASSIFIER_MATCH.equalsIgnoreCase(classifierMatchingRuleValue.getName()) ||
					ModelingProperties.CONVERSION.equalsIgnoreCase(classifierMatchingRuleValue.getName()) || ModelingProperties.COMPLEMENT.equalsIgnoreCase(classifierMatchingRuleValue.getName())
				) {
				if (classifierMatchingRuleValue != null && ModelingProperties.CONVERSION.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
					warning(connection, "The classifier matching rule '" + ModelingProperties.CONVERSION + "' is not supported for feature group connections. Using rule '" +
							ModelingProperties.CLASSIFIER_MATCH + "' instead.");
				}
				if (classifierMatchingRuleValue != null && ModelingProperties.COMPLEMENT.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
					warning(connection, "The classifier matching rule '" + ModelingProperties.COMPLEMENT +
							"' is not supported for feature group connections that connect up or down the containment hierarchy. Using rule '" + ModelingProperties.CLASSIFIER_MATCH + "' instead.");
				}
				if (sourceType == destinationType) {
					if (cxtFGIsInverse?source.isInverse() == destination.isInverse():source.isInverse() != destination.isInverse()) {
						error(connection, "For connections that connect up or down the containment hierarchy, the feature group types of the source and destination must be identical." +
								" They cannot be inverses of each other.");
					}
				}
				else {
					if (isSameInExtends(sourceType, destinationType)){
						if (cxtFGIsInverse?source.isInverse() == destination.isInverse():source.isInverse() != destination.isInverse()) {
							error(connection, "Ancestor feature group types match, but feature group '" + source.getName() + "' and '" + destination.getName() + "' differ in inverse of.");
						} else {
							warning(connection,"The feature group type of '" + source.getName() + "' and '" + destination.getName() + "' do not match, but their ancestors do.");
						}
					} else {
						error(connection, "The feature group types of the source and destination feature groups must be identical for connections that connect up or down the containment hierarchy.");
					}
				}
			}
			else if (ModelingProperties.EQUIVALENCE.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
				if (!testIfFeatureGroupTypesAreIdentical(source, sourceType, destination, destinationType) &&
						!classifiersFoundInSupportedClassifierEquivalenceMatchesProperty(connection, sourceType, destinationType)) {
					error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceType.getQualifiedName() + "' and '" + destinationType.getQualifiedName() +
							"') are not identical and they are not listed as matching classifiers in the property constant '" + AadlProject.SUPPORTED_CLASSIFIER_EQUIVALENCE_MATCHES + "'.");
				}
			}
			else if (ModelingProperties.SUBSET.equalsIgnoreCase(classifierMatchingRuleValue.getName())) {
				FeatureGroup innerFeatureGroup;
				FeatureGroupType innerFeatureGroupType;
				FeatureGroup outerFeatureGroup;
				FeatureGroupType outerFeatureGroupType;
				if (connection.getAllSourceContext() instanceof Subcomponent) {
					innerFeatureGroup = source;
					innerFeatureGroupType = sourceType;
					outerFeatureGroup = destination;
					outerFeatureGroupType = destinationType;
				}
				else {
					outerFeatureGroup = source;
					outerFeatureGroupType = sourceType;
					innerFeatureGroup = destination;
					innerFeatureGroupType = destinationType;
				}
				if (!testIfFeatureGroupTypesAreIdentical(source, sourceType, destination, destinationType) &&
						!checkIfFeatureGroupTypesAreUpAndDownSubsets(innerFeatureGroupType, innerFeatureGroup.isInverse(), outerFeatureGroupType, outerFeatureGroup.isInverse())) {
					error(connection, "The types of '" + source.getName() + "' and '" + destination.getName() + "' ('" + sourceType.getQualifiedName() + "' and '" + destination.getQualifiedName() +
							"') are not identical and they do not satisfy the Subset rule for classifier matching.  In order to satisfy this rule, the incoming features of the inner feature group must be a" +
							" subset of the incoming features of the outer feature group and the outgoing features of the outer feature group must be a subset of the outgoing features of the inner feature" +
							" group.");
				}
			}
		}
	}
	
	private boolean testIfFeatureGroupTypesAreInverses(FeatureGroup source, FeatureGroupType sourceType, FeatureGroup destination, FeatureGroupType destinationType) {
		if (sourceType.isInverseOf(destinationType)) {
			if (source.isInverse() || destination.isInverse())
				return false;
		}
		else if (sourceType == destinationType) {
			if (source.isInverse() == destination.isInverse())
				return false;
		}
		else
			return false;
		return true;
	}
	
	private boolean testIfFeatureGroupTypeExtensionsAreInverses(FeatureGroup source, FeatureGroupType sourceType, FeatureGroup destination, FeatureGroupType destinationType) {
		// one is the ancestor of the other
		// then the fg must have opposite inverses
		if (AadlUtil.isSameOrExtends(sourceType, destinationType)||AadlUtil.isSameOrExtends(destinationType, sourceType)){
			return (source.isInverse() && !destination.isInverse())||(!source.isInverse() && destination.isInverse());
		}
		if (isInverseOfInExtends(sourceType,destinationType)) {
			// fg must be the same (both inverse or both not inverse)
			return (source.isInverse() && destination.isInverse())||(!source.isInverse() && !destination.isInverse());
		}
		if (isSameInExtends(sourceType, sourceType)){
			// they have a common FGT root
			return (source.isInverse() && !destination.isInverse())||(!source.isInverse() && destination.isInverse());
		}
		return true;
	}
	
	public boolean isInverseOfInExtends(FeatureGroupType srcpgt,FeatureGroupType dstpgt) {
		EList<Classifier> srcancestors = getSelfPlusAncestors(srcpgt);
		FeatureGroupType dstfgt = dstpgt;
		while ( !Aadl2Util.isNull(dstfgt) ){
			if (!Aadl2Util.isNull(dstfgt.getInverse()) ){
				if(srcancestors.contains(dstfgt.getInverse())){
					return true;
				}
			}
			dstfgt = dstfgt.getExtended();
		}
		EList<Classifier> dstancestors = getSelfPlusAncestors(dstpgt);
		FeatureGroupType srcfgt = srcpgt;
		while ( !Aadl2Util.isNull(srcfgt) ){
			if (!Aadl2Util.isNull(srcfgt.getInverse()) ){
				if(dstancestors.contains(srcfgt.getInverse())){
					return true;
				}
			}
			srcfgt = srcfgt.getExtended();
		}
		return false;
	}
	
	public boolean isSameInExtends(FeatureGroupType srcpgt,FeatureGroupType dstpgt) {
		EList<Classifier> srcancestors = getSelfPlusAncestors(srcpgt);
		FeatureGroupType dstfgt = dstpgt;
		while ( !Aadl2Util.isNull(dstfgt) ){
			if(srcancestors.contains(dstfgt)){
				return true;
			}
			dstfgt = dstfgt.getExtended();
		}
		return false;
	}

	
	private boolean testIfFeatureGroupTypesAreIdentical(FeatureGroup source, FeatureGroupType sourceType, FeatureGroup destination, FeatureGroupType destinationType) {
		if (sourceType == destinationType) {
			if (source.isInverse() != destination.isInverse())
				return false;
			else
				return true;
		}
		else
			return false;
	}
	
	/**
	 * Complies with the criteria for complementing feature group types specified in legality
	 *  rules L8 through L12 in section 8.2 (Feature Groups and Feature Group Types)
	 * "(L8)	The number of feature or feature groups contained in the feature group and
	 * 			its complement must be identical;
	 * (L9)		Each of the decalred features or feature groups in a feature group must be a
	 * 			pair-wise complement with that in the feature group complement, with pairs
	 * 			determined by declaration order.  In the case of feature group types
	 * 			extensions, the feature and feature group declarations in the extension are
	 * 			considered to be declared after the declarations in the feature group type
	 * 			being extended;
	 * (L10)	If both feature group types have zero features, then they are considered to
	 * 			complement each other;
	 * (L11)	Ports are pair-wise complementary if they satisfy the port connection rules
	 * 			specified in Section 9.2.1.  This includes appropriate port direction and
	 * 			matching of data component classifier references according to classifier
	 * 			matching rules (see Section 9.5 legality rules (L3) and (L4);
	 * (L12)	Access features are pair-wise complementary if they satisfy the access
	 * 			connection rules in Section 9.4."
	 */
	//It seems that this method may not be needed anymore.  It is left here just in case it will
	//be needed in the future.  It is currently incomplete.
//	private boolean checkIfFeatureGroupTypesComplementEachOther(FeatureGroupType sourceType, boolean isSourceFGInverse, FeatureGroupType destinationType, boolean isDestinationFGInverse) {
//		EList<Feature> allSourceFeatures = sourceType.getAllFeatures();
//		EList<Feature> allDestinationFeatures = destinationType.getAllFeatures();
//		if (allSourceFeatures.size() == 0 && allDestinationFeatures.size() == 0)
//			return true;
//		if (allSourceFeatures.size() != allDestinationFeatures.size())
//			return false;
//		Iterator<Feature> sourceFeaturesIterator = allSourceFeatures.iterator();
//		Iterator<Feature> destinationFeaturesIterator = allDestinationFeatures.iterator();
//		while (sourceFeaturesIterator.hasNext() && destinationFeaturesIterator.hasNext()) {
//			Feature sourceFeature = sourceFeaturesIterator.next();
//			Feature destinationFeature = destinationFeaturesIterator.next();
//			if (sourceFeature instanceof Port && destinationFeature instanceof Port) {
//				//Check port types
//				if (sourceFeature instanceof EventPort && !(destinationFeature instanceof EventPort))
//					return false;
//				
//				//Check direction
//				DirectionType sourceDirection = ((Port)sourceFeature).getDirection();
//				DirectionType destinationDirection = ((Port)destinationFeature).getDirection();
//				if (isSourceFGInverse)
//					sourceDirection = sourceDirection.getInverseDirection();
//				if (sourceType != sourceFeature.getContainingClassifier() && sourceType.getInverse() != null) {
//					//feature group type has inverse and feature is defined in the inverse FGT
//					sourceDirection = sourceDirection.getInverseDirection();
//				}
//				if (isDestinationFGInverse)
//					destinationDirection = destinationDirection.getInverseDirection();
//				if (destinationType != destinationFeature.getContainingClassifier() && destinationType.getInverse() != null) {
//					//feature group type has inverse and feature is defined in the inverse FGT
//					destinationDirection = destinationDirection.getInverseDirection();
//				}
//				if ((sourceDirection == DirectionType.IN && destinationDirection == DirectionType.IN) || (sourceDirection == DirectionType.OUT && destinationDirection == DirectionType.OUT))
//					return false;
//				
//				//Check classifier
//				ComponentClassifier sourceClassifier = sourceFeature.getAllClassifier();
//				ComponentClassifier destinationClassifier = destinationFeature.getAllClassifier();
//				if (sourceClassifier != null && destinationClassifier != null && sourceClassifier != destinationClassifier) {
//					if ((sourceClassifier instanceof ComponentType && destinationClassifier instanceof ComponentType) ||
//							(sourceClassifier instanceof ComponentImplementation && destinationClassifier instanceof ComponentImplementation)) {
//						return false;
//					}
//				}
//			}
//		}
//		return true;
//	}
	
	private boolean checkIfFeatureGroupTypesAreUpAndDownSubsets(FeatureGroupType innerType, boolean isInnerFGInverse, FeatureGroupType outerType, boolean isOuterFGInverse) {
		for (Feature innerFeature : innerType.getAllFeatures()) {
			if (innerFeature instanceof DirectedFeature) {
				DirectionType innerDirection = ((DirectedFeature)innerFeature).getDirection();
				if (isInnerFGInverse)
					innerDirection = innerDirection.getInverseDirection();
				if (innerType != innerFeature.getContainingClassifier() && innerType.getInverse() != null) {
					//feature group type has inverse and feature is defined in the inverse FGT
					innerDirection = innerDirection.getInverseDirection();
				}
				if (innerDirection.incoming()) {
					//need to find incoming feature in outer feature group
					boolean matchingFeatureFound = false;
					for (Feature outerFeature : outerType.getAllFeatures()) {
						if (innerFeature.getName().equalsIgnoreCase(outerFeature.getName())) {
							matchingFeatureFound = true;
							if (outerFeature instanceof DirectedFeature) {
								DirectionType outerDirection = ((DirectedFeature)outerFeature).getDirection();
								if (isOuterFGInverse)
									outerDirection = outerDirection.getInverseDirection();
								if (outerType != outerFeature.getContainingClassifier() && outerType.getInverse() != null) {
									//feature group type has inverse and feature is defined in the inverse FGT
									outerDirection = outerDirection.getInverseDirection();
								}
								if (!outerDirection.incoming())
									return false;
								if (!innerFeature.eClass().equals(outerFeature.eClass()))
									return false;
							}
							else
								return false;
						}
					}
					if (!matchingFeatureFound)
						return false;
				}
			}
		}
		
		for (Feature outerFeature : outerType.getAllFeatures()) {
			if (outerFeature instanceof DirectedFeature) {
				DirectionType outerDirection = ((DirectedFeature)outerFeature).getDirection();
				if (isOuterFGInverse)
					outerDirection = outerDirection.getInverseDirection();
				if (outerType != outerFeature.getContainingClassifier() && outerType.getInverse() != null) {
					//feature group type has inverse and feature is defined in the inverse FGT
					outerDirection = outerDirection.getInverseDirection();
				}
				if (outerDirection.outgoing()) {
					//need to find outgoing feature in inner feature group
					boolean matchingFeatureFound = false;
					for (Feature innerFeature : innerType.getAllFeatures()) {
						if (outerFeature.getName().equalsIgnoreCase(innerFeature.getName())) {
							matchingFeatureFound = true;
							if (innerFeature instanceof DirectedFeature) {
								DirectionType innerDirection = ((DirectedFeature)innerFeature).getDirection();
								if (isInnerFGInverse)
									innerDirection = innerDirection.getInverseDirection();
								if (innerType != innerFeature.getContainingClassifier() && innerType.getInverse() != null) {
									//feature group type has inverse and feature is defined in the inverse FGT
									innerDirection = innerDirection.getInverseDirection();
								}
								if (!innerDirection.outgoing())
									return false;
								if (!outerFeature.eClass().equals(innerFeature.eClass()))
									return false;
							}
							else
								return false;
						}
					}
					if (!matchingFeatureFound)
						return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean checkIfFeatureGroupTypesAreSiblingSubsets(FeatureGroupType sourceType, boolean isSourceFGInverse, FeatureGroupType destinationType, boolean isDestinationFGInverse) {
		for (Feature sourceFeature : sourceType.getAllFeatures()) {
			if (sourceFeature instanceof DirectedFeature) {
				DirectionType sourceDirection = ((DirectedFeature)sourceFeature).getDirection();
				if (isSourceFGInverse)
					sourceDirection = sourceDirection.getInverseDirection();
				if (sourceType != sourceFeature.getContainingClassifier() && sourceType.getInverse() != null) {
					//feature group type has inverse and feature is defined in the inverse FGT
					sourceDirection = sourceDirection.getInverseDirection();
				}
				if (sourceDirection.incoming()) {
					//need to find outgoing feature in destination
					boolean matchingFeatureFound = false;
					for (Feature destinationFeature : destinationType.getAllFeatures()) {
						if (sourceFeature.getName().equalsIgnoreCase(destinationFeature.getName())) {
							matchingFeatureFound = true;
							if (destinationFeature instanceof DirectedFeature) {
								DirectionType destinationDirection = ((DirectedFeature)destinationFeature).getDirection();
								if (isDestinationFGInverse)
									destinationDirection = destinationDirection.getInverseDirection();
								if (destinationType != destinationFeature.getContainingClassifier() && destinationType.getInverse() != null) {
									//feature group type has inverse and feature is defined in the inverse FGT
									destinationDirection = destinationDirection.getInverseDirection();
								}
								if (!destinationDirection.outgoing())
									return false;
								if (!sourceFeature.eClass().equals(destinationFeature.eClass()))
									return false;
							}
							else
								return false;
						}
					}
					if (!matchingFeatureFound)
						return false;
				}
			}
		}
		
		for (Feature destinationFeature : destinationType.getAllFeatures()) {
			if (destinationFeature instanceof DirectedFeature) {
				DirectionType destinationDirection = ((DirectedFeature)destinationFeature).getDirection();
				if (isDestinationFGInverse)
					destinationDirection = destinationDirection.getInverseDirection();
				if (destinationType != destinationFeature.getContainingClassifier() && destinationType.getInverse() != null) {
					//feature group type has inverse and feature is defined in the inverse FGT
					destinationDirection = destinationDirection.getInverseDirection();
				}
				if (destinationDirection.incoming()) {
					//need to find outgoing feature in source
					boolean matchingFeatureFound = false;
					for (Feature sourceFeature : sourceType.getAllFeatures()) {
						if (destinationFeature.getName().equalsIgnoreCase(sourceFeature.getName())) {
							matchingFeatureFound = true;
							if (sourceFeature instanceof DirectedFeature) {
								DirectionType sourceDirection = ((DirectedFeature)sourceFeature).getDirection();
								if (isSourceFGInverse)
									sourceDirection = sourceDirection.getInverseDirection();
								if (sourceType != sourceFeature.getContainingClassifier() && sourceType.getInverse() != null) {
									//feature group type has inverse and feature is defined in the inverse FGT
									sourceDirection = sourceDirection.getInverseDirection();
								}
								if (!sourceDirection.outgoing())
									return false;
								if (!destinationFeature.eClass().equals(sourceFeature.eClass()))
									return false;
							}
							else
								return false;
						}
					}
					if (!matchingFeatureFound)
						return false;
				}
			}
		}
		
		return true;
	}

	private boolean classifiersFoundInSupportedClassifierComplementMatchesProperty(FeatureGroupConnection connection, FeatureGroupType source, FeatureGroupType destination) {
		PropertyConstant matchesPropertyConstant = GetProperties.lookupPropertyConstant(connection, AadlProject.SUPPORTED_CLASSIFIER_COMPLEMENT_MATCHES);
		if (matchesPropertyConstant == null)
			return false;
		PropertyExpression constantValue = matchesPropertyConstant.getConstantValue();
		if (!(constantValue instanceof ListValue))
			return false;
		for (PropertyExpression classifierPair : ((ListValue)constantValue).getOwnedListElements()) {
			if (classifierPair instanceof ListValue) {
				EList<PropertyExpression> innerListElements = ((ListValue)classifierPair).getOwnedListElements();
				if (innerListElements.size() == 2 && innerListElements.get(0) instanceof ClassifierValue && innerListElements.get(1) instanceof ClassifierValue) {
					Classifier firstPairElement = ((ClassifierValue)innerListElements.get(0)).getClassifier();
					Classifier secondPairElement = ((ClassifierValue)innerListElements.get(1)).getClassifier();
					if ((firstPairElement == source && secondPairElement == destination) || (firstPairElement == destination && secondPairElement == source))
						return true;
				}
			}
		}
		return false;
	}
}
