/*
 * <copyright>
 * Copyright  2009 by Carnegie Mellon University, all rights reserved.
 *
 * Use of the Open Source AADL Tool Environment (OSATE) is subject to the terms of the license set forth
 * at http://www.eclipse.org/legal/cpl-v10.html.
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
 * Carnegie Mellon University Software Engineering Institute authored documents are sponsored by the U.S. Department
 * of Defense under Contract F19628-00-C-0003. Carnegie Mellon University retains copyrights in all material produced
 * under this contract. The U.S. Government retains a non-exclusive, royalty-free license to publish or reproduce these
 * documents, or allow others to do so, for U.S. Government purposes only pursuant to the copyright license
 * under the contract clause at 252.227.7013.
 *
 * </copyright>
 *
 * $Id: CreateEndToEndFlowsSwitch.java,v 1.39 2010-06-14 01:21:51 lwrage Exp $
 *
 */
package org.osate.aadl2.instantiation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.osate.aadl2.ComponentCategory;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.Connection;
import org.osate.aadl2.Context;
import org.osate.aadl2.DataAccess;
import org.osate.aadl2.Element;
import org.osate.aadl2.EndToEndFlow;
import org.osate.aadl2.EndToEndFlowElement;
import org.osate.aadl2.EndToEndFlowSegment;
import org.osate.aadl2.Feature;
import org.osate.aadl2.FeatureGroup;
import org.osate.aadl2.FlowElement;
import org.osate.aadl2.FlowEnd;
import org.osate.aadl2.FlowImplementation;
import org.osate.aadl2.FlowSegment;
import org.osate.aadl2.FlowSpecification;
import org.osate.aadl2.ModalElement;
import org.osate.aadl2.Mode;
import org.osate.aadl2.ModeTransition;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionReference;
import org.osate.aadl2.instance.EndToEndFlowInstance;
import org.osate.aadl2.instance.FeatureCategory;
import org.osate.aadl2.instance.FeatureInstance;
import org.osate.aadl2.instance.FlowElementInstance;
import org.osate.aadl2.instance.FlowSpecificationInstance;
import org.osate.aadl2.instance.InstanceFactory;
import org.osate.aadl2.instance.InstanceObject;
import org.osate.aadl2.instance.ModeInstance;
import org.osate.aadl2.instance.ModeTransitionInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instance.SystemOperationMode;
import org.osate.aadl2.instance.util.InstanceSwitch;
import org.osate.aadl2.instance.util.InstanceUtil;
import org.osate.aadl2.instance.util.InstanceUtil.InstantiatedClassifier;
import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
import org.osate.aadl2.modelsupport.modeltraversal.AadlProcessingSwitchWithProgress;
import org.osate.aadl2.modelsupport.util.AadlUtil;

/**
 * @author lwrage
 */
public class CreateEndToEndFlowsSwitch extends AadlProcessingSwitchWithProgress {

	static class FlowIterator implements Iterator<Element> {

		private EList<EndToEndFlowSegment> eteSegments;

		private EList<FlowSegment> flowSegments;

		private int size;

		private int index;

		public FlowIterator(EndToEndFlow ete) {
			this.eteSegments = ete.getAllFlowSegments();
			size = this.eteSegments.size();
			index = 0;
		}

		public FlowIterator(FlowImplementation flowImpl) {
			this.flowSegments = flowImpl.getOwnedFlowSegments();
			size = this.flowSegments.size();
			index = 0;
		}

		private FlowIterator(EList<EndToEndFlowSegment> eteSegments, EList<FlowSegment> flowSegments, int index) {
			this.eteSegments = eteSegments;
			this.flowSegments = flowSegments;
			size = eteSegments != null ? eteSegments.size() : flowSegments.size();
			this.index = index;
		}

		public boolean hasNext() {
			return index < size;
		}

		public Element next() {
			return eteSegments != null ? eteSegments.get(index++) : flowSegments.get(index++);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#clone()
		 */
		@Override
		protected FlowIterator clone() {
			return new FlowIterator(eteSegments, flowSegments, index);
		}
	}

	static class ETEInfo implements Cloneable {
		List<Connection> preConns;
		EndToEndFlowInstance etei;
		List<Connection> postConns = new ArrayList<Connection>();

		/**
		 * @param etei
		 */
		public ETEInfo(EndToEndFlowInstance etei) {
			preConns = new ArrayList<Connection>();
			this.etei = etei;
		}

		public ETEInfo(List<Connection> preConns, EndToEndFlowInstance etei) {
			this.preConns 	= preConns;
			this.etei 		= etei;
		}
	}

	private ETEInfo myInfo;

	private Stack<FlowIterator> state = new Stack<FlowIterator>();

	/**
	 * A classifier for an instance object when it is a prototype in the
	 * declarative model. The classifier is the result of resolving the
	 * prototype. It's either the classifier that is instantiated as a
	 * subcomponent or feature group instance or the classifier referenced by a
	 * feature or subprogram call. If the classifier is anonymous, then its
	 * bindings are included also.
	 */
	private HashMap<InstanceObject, InstantiatedClassifier> classifierCache = null;

	/**
	 * The current list of declarative connections referenced in an ETE
	 */
	private List<Connection> connections = new ArrayList<Connection>();

	/**
	 * All end to end flow instances created for an end to end flow.
	 */
	private List<ETEInfo> created;

	private HashMap<EndToEndFlow, List<ETEInfo>> ete2info;

	/**
	 * Create a new instance.
	 * 
	 * @param pm the progress monitor
	 * @param errMgr the error manager
	 * @param classifierCache cache of known instantiated classifiers, may be
	 *            null
	 */
	public CreateEndToEndFlowsSwitch(final IProgressMonitor pm, final AnalysisErrorReporterManager errMgr,
			HashMap<InstanceObject, InstantiatedClassifier> classifierCache) {
		super(pm, PROCESS_PRE_ORDER_ALL, errMgr);
		this.classifierCache = classifierCache;
	}

	protected final void initSwitches() {
		instanceSwitch = new InstanceSwitch<String>() {
			HashMap<EndToEndFlow, List<ETEInfo>> ete2info = new HashMap<EndToEndFlow, List<ETEInfo>>();

			public String caseComponentInstance(final ComponentInstance ci) {
				ComponentImplementation impl;

				if (ci.getContainingComponentInstance() instanceof SystemInstance) {
					monitor.subTask("Creating end-to-end flows in " + ci.getName());
				}
				try {
					impl = InstanceUtil.getComponentImplementation(ci, 0, classifierCache);
					if (impl != null) {
						for (EndToEndFlow ete : impl.getAllEndToEndFlows()) {
							if (!ete2info.containsKey(ete)) {
								instantiateEndToEndFlow(ci, ete, ete2info);
							}
						}
					}
				} finally {
					ete2info.clear();
				}
				return DONE;
			}
		};
	}

	protected void instantiateEndToEndFlow(ComponentInstance ci, EndToEndFlow ete,
			HashMap<EndToEndFlow, List<ETEInfo>> ete2info) {
		EndToEndFlowInstance etei = InstanceFactory.eINSTANCE.createEndToEndFlowInstance();

		created = new ArrayList<ETEInfo>();
		ete2info.put(ete, created);
		this.ete2info = ete2info;

		etei.setName(ete.getName());
		etei.setEndToEndFlow(ete);
		ci.getEndToEndFlows().add(etei);
		created.add(myInfo = new ETEInfo(etei));
		EList<EList<ModeInstance>> ml = etei.getModesList();
		ml.clear();
		ml.add(getModeInstances(ci, ete));
		processETE(ci, etei, ete);
		ml.clear();
	}

	protected void processETE (final ComponentInstance ci, 
							   final EndToEndFlowInstance etei,
							   final EndToEndFlow ete)
	{
		FlowIterator iter = new FlowIterator(ete);
		
		// TODO-LW: is this loop necessary?
		while (iter.hasNext()) 
		{
			EndToEndFlowSegment fe = (EndToEndFlowSegment) iter.next();

			processETESegment(ci, etei, fe, iter, ete);
			break;
		}
	}

	/**
	 * Add all flow instances that continue through the next flow element.
	 * 
	 * @param ci the component instance we're in
	 * @param etei the current flow instance
	 * @param fe the next flow element
	 * @param iter the position in the current ETE declaration
	 * @param errorElement the model element that we attach errors to
	 */
	protected void processETESegment	(final ComponentInstance ci, 
										 final EndToEndFlowInstance etei, 
										 final Element fs, 
										 final FlowIterator iter,
										 final NamedElement errorElement) 
	{
		final Element fe;

		if (fs instanceof FlowSegment)
		{
			fe = ((FlowSegment) fs).getFlowElement();
		}
		else
		{
			fe = ((EndToEndFlowSegment) fs).getFlowElement();
		}
		
		if (fe instanceof Connection) 
		{
			if (etei.getFlowElements() == null || etei.getFlowElements().isEmpty()) 
			{
				myInfo.preConns.add((Connection) fe);
			} 
			else 
			{
				connections.add((Connection) fe);
			}
		} 
		else 
		{
			if (fe instanceof FlowSpecification) 
			{
				final Subcomponent sc = (Subcomponent) (fs instanceof FlowSegment ? ((FlowSegment) fs).getContext()
						: ((EndToEndFlowSegment) fs).getContext());
				final ComponentInstance sci = ci.findSubcomponentInstance(sc);
				if (sci != null)
				{
					processSubcomponentFlow(sci, etei, (FlowSpecification) fe, iter);
				} 
				else 
				{
					error(errorElement, "Incomplete End-to-end flow instance " + etei.getName()
							+ ": Could not find component instance for subcomponent " + sc.getName()
							+ " in flow implementation " + errorElement.getName());
				}
			} 
			else if (fe instanceof Subcomponent) 
			{
				ComponentInstance sci = ci.findSubcomponentInstance((Subcomponent) fe);
				processFlowStep(sci, etei, fe, iter);
			} 
			else if (fe instanceof DataAccess) 
			{
				processDataAccess(ci, etei, (DataAccess) fe, iter);
			} 
			else if (fe instanceof EndToEndFlow) 
			{
				processEndToEndFlow(ci, etei, (EndToEndFlow) fe, iter);
			}
		}
	}

	/**
	 * Instantiate a flow specification by recursively following its
	 * implementation until a leaf element is reached In case of a leaf element
	 * add it as a flow step
	 * 
	 * @param ci the component whose flow specification is to be processed
	 * @param etei the end to end flow instance
	 * @param fs the flow specification to be processed
	 */
	protected void processSubcomponentFlow(final ComponentInstance ci,  EndToEndFlowInstance etei, final FlowSpecification fs,
			FlowIterator iter) {
		final Subcomponent subComp = ci.getSubcomponent();
		final ComponentImplementation subImpl = subComp.getComponentImplementation();
		final EList<FlowImplementation> flowImpls = new BasicEList<FlowImplementation>(10);

		// Collect flow impls for this flow spec
		if (subImpl != null) 
		{
			for (FlowImplementation fl : subImpl.getAllFlowImplementations())
			{
				if (fl.getSpecification() == fs) 
				{
					flowImpls.add(fl);
				}
			}
		}

		if (flowImpls.isEmpty()) {
			// we are at a leaf
			processFlowStep(ci, etei, fs, iter);
			if (subImpl != null && AadlUtil.hasPortComponents(subImpl)) {
				warning(etei, "End-to-end flow " + etei.getName() + " contains component " + ci.getName()
						+ " with subcomponents, but no flow implementation " + fs.getName() + " to them");
			}
			// if (isLast()) {
			// fillinModes(etei);
			// }
		} else {
			int index = 1;
			Iterator<FlowImplementation> itt = flowImpls.iterator();
			String basename = etei.getName();

			state.push(iter);
			while (itt.hasNext()) {
				EndToEndFlowInstance eteiClone = null;
				Stack<FlowIterator> stateClone = null;
				FlowImplementation flowImpl = itt.next();
				boolean prepareNext = itt.hasNext();

				if (prepareNext) {
					stateClone = clone(state);
					eteiClone = (EndToEndFlowInstance) EcoreUtil.copy(etei);
					eteiClone.setName(basename + "_" + index++);
					eteiClone.getModesList().addAll(etei.getModesList());
				}

				// add all ete instances that continue through flow impl
				if (!processFlowImpl(ci, etei, flowImpl)) {
					processFlowStep(ci, etei, fs, iter);
					// if (isLast()) {
					// fillinModes(etei);
					// }
				}

				if (prepareNext) {
					// add clone
					etei.getContainingComponentInstance().getEndToEndFlows().add(eteiClone);
					etei = eteiClone;
					state = stateClone;
					if (etei.getFlowElements() == null || etei.getFlowElements().isEmpty()) {
						created.add(myInfo = new ETEInfo(etei));
					} else {
						created.add(myInfo = new ETEInfo(myInfo.preConns, etei));
					}
				}
			}
		}
	}

	/**
	 * Continue the current ETE instance with a flow implementation.
	 * 
	 * @param ci the component instance whose flow implementation is being
	 *            processed
	 * @param etei the end to end flow instance
	 * @param flowImpl the flow implementation to be processed
	 * @return if elements were added to the end to end flow instance
	 */
	protected boolean processFlowImpl(ComponentInstance ci, EndToEndFlowInstance etei, FlowImplementation flowImpl) {
		etei.getModesList().add(getModeInstances(ci, flowImpl));

		if (flowImpl.getOwnedFlowSegments().size() < 2) {
			// the flow impl doesn't include a subcomponent, nothing to do
			state.pop();
			return false;
		}

		continueFlow(ci, etei, new FlowIterator(flowImpl), flowImpl);
		return true;
	}

	/**
	 * Add all ETE instances that continue through a given leaf flow element.
	 * One instance per matching connection.
	 * 
	 * @param ci
	 * @param etei the current end to end flow instance
	 * @param leaf the next ETE element
	 * @param iter the position in the current end to end flow declaration
	 */
	protected void processFlowStep(ComponentInstance ci, EndToEndFlowInstance etei, Element leaf, FlowIterator iter) {
		// add connection(s), will be empty when starting the ETE
		if (connections.isEmpty()) {
			addLeafElement(ci, etei, leaf);
			continueFlow(ci.getContainingComponentInstance(), etei, iter, ci);
		} else {
			List<ConnectionInstance> connis = collectConnectionInstances(ci.getSystemInstance(), etei);

			if (connis.isEmpty()) {
				error(etei, "Incomplete end-to-end flow instance " + etei.getName()
						+ ": Missing connection instance to " + ((NamedElement) leaf).getName());
				connections.clear();
			} else {
				int index = 1;
				Iterator<ConnectionInstance> connIter = connis.iterator();
				String basename = etei.getName();

				while (connIter.hasNext()) {
					EndToEndFlowInstance eteiClone = null;
					Stack<FlowIterator> stateClone = null;
					ConnectionInstance conni = connIter.next();
					boolean prepareNext = connIter.hasNext();

					if (prepareNext) {
						stateClone = clone(state);
						eteiClone = (EndToEndFlowInstance) EcoreUtil.copy(etei);
						eteiClone.setName(basename + "_" + index++);
						eteiClone.getModesList().addAll(etei.getModesList());
					}

					etei.getFlowElements().add(conni);
					addLeafElement(ci, etei, leaf);

					// prepare next connection filter
					connections.clear();
					if (iter.hasNext()) {
						Element obj = iter.next();
						Connection conn = null;
						if (obj instanceof FlowSegment) {
							FlowElement fe = ((FlowSegment) obj).getFlowElement();
							if (fe instanceof Connection)
								conn = (Connection) fe;
						} else if (obj instanceof EndToEndFlowSegment) {
							EndToEndFlowElement fe = ((EndToEndFlowSegment) obj).getFlowElement();
							if (fe instanceof Connection)
								conn = (Connection) fe;
						}
						if (conn != null)
							connections.add(conn);
					}

					continueFlow(ci.getContainingComponentInstance(), etei, iter, ci);

					if (prepareNext) {
						// add clone
						etei.getContainingComponentInstance().getEndToEndFlows().add(eteiClone);
						etei = eteiClone;
						state = stateClone;
						if (etei.getFlowElements() == null || etei.getFlowElements().isEmpty()) {
							created.add(myInfo = new ETEInfo(etei));
						} else {
							created.add(myInfo = new ETEInfo(myInfo.preConns, etei));
						}
					}
				}
			}
		}
	}

	/**
	 * Add the ETE instance that goes through a data access feature. Instead of
	 * the data access feature, add the accessed object to the ETE instance. The
	 * access feature uniquely determines the accessed object.
	 * 
	 * @param ci
	 * @param etei
	 * @param fe
	 * @param iter
	 */
	private void processDataAccess(ComponentInstance ci, EndToEndFlowInstance etei, DataAccess da, FlowIterator iter) {
		// add connection(s), will be empty when starting the ETE
		if (connections.isEmpty()) {
			addLeafElement(ci, etei, da);
			continueFlow(ci.getContainingComponentInstance(), etei, iter, ci);
		} else {
			List<ConnectionInstance> connis = collectConnectionInstances(ci.getSystemInstance(), etei);

			if (connis.isEmpty()) {
				error(etei, "Incomplete end-to-end flow instance " + etei.getName()
						+ ": Missing connection instance to " + ((NamedElement) da).getName());
				connections.clear();
			} else {
				int index = 1;
				Iterator<ConnectionInstance> connIter = connis.iterator();
				String basename = etei.getName();
				boolean errorReported = false;

				state.push(iter);
				while (connIter.hasNext()) {
					EndToEndFlowInstance eteiClone = null;
					Stack<FlowIterator> stateClone = null;
					ConnectionInstance conni = connIter.next();
					boolean prepareNext = connIter.hasNext();
					EndToEndFlowElement leaf = null;
					ComponentInstance target = null;

					if (conni.getDestination() instanceof ComponentInstance) {
						target = (ComponentInstance) conni.getDestination();

						if (target.getCategory() == ComponentCategory.DATA) {
							leaf = target.getSubcomponent();
						}
//					} else if (conni.getDestination() instanceof FeatureInstance) {
//						FeatureInstance target = (FeatureInstance) conni.getDestination();
//
//						if (target.getCategory() == FeatureCategory.DATA_ACCESS) {
//							leaf = (DataAccess) target.getFeature();
//						}
					} else {
						if (!errorReported) {
							errorReported = true;
							error(etei, "Data access feature " + da.getQualifiedName()
									+ " is not a proxy for a data component.");
						}
					}

					if (leaf != null) {
						if (prepareNext) {
							stateClone = clone(state);
							eteiClone = (EndToEndFlowInstance) EcoreUtil.copy(etei);
							eteiClone.setName(basename + "_" + index++);
							eteiClone.getModesList().addAll(etei.getModesList());
						}

						etei.getFlowElements().add(conni);
						addLeafElement(target, etei, leaf);

						// prepare next connection filter
						Connection lastConn = connections.get(connections.size() - 1);

						connections.clear();
						if (iter.hasNext()) {
							Element obj = iter.next();
							Connection nextConn = null;
							if (obj instanceof FlowSegment) {
								FlowElement fe = ((FlowSegment) obj).getFlowElement();
								if (fe instanceof Connection)
									nextConn = (Connection) fe;
							} else if (obj instanceof EndToEndFlowSegment) {
								EndToEndFlowElement fe = ((EndToEndFlowSegment) obj).getFlowElement();
								if (fe instanceof Connection)
									nextConn = (Connection) fe;
							}
							if (nextConn != null) {
								int i = conni.getConnectionReferences().size() - 1;
								Connection preConn = null;

								while (i > 0 && preConn != lastConn) {
									preConn = conni.getConnectionReferences().get(i--).getConnection();
									if (preConn != lastConn) {
										connections.add(preConn);
									}
								}
								connections.add(nextConn);
							}
						}

						continueFlow(ci, etei, state.pop(), ci);

						if (prepareNext) {
							// add clone
							etei.getContainingComponentInstance().getEndToEndFlows().add(eteiClone);
							etei = eteiClone;
							state = stateClone;
							if (etei.getFlowElements() == null || etei.getFlowElements().isEmpty()) {
								created.add(myInfo = new ETEInfo(etei));
							} else {
								created.add(myInfo = new ETEInfo(myInfo.preConns, etei));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param ci
	 * @param etei
	 * @param ete
	 * @param iter
	 */
	// TODO-LW: Detect cyclic dependencies between ETEs
	// restore iterator after each nested
	// add preConn before addNested
	private void processEndToEndFlow(ComponentInstance ci, EndToEndFlowInstance etei, EndToEndFlow ete,
			FlowIterator iter) {
		List<ETEInfo> nestedETEs;

		// instantiate the nested ete if that hasn't been done already
		if (!ete2info.containsKey(ete)) {
			new CreateEndToEndFlowsSwitch(monitor, getErrorManager(), classifierCache).instantiateEndToEndFlow(ci, ete,
					ete2info);
		}
		nestedETEs = ete2info.get(ete);

		if (nestedETEs.isEmpty()) {
			error(etei, "No nested end to end flows instantiated for " + ete.getQualifiedName());
			connections.clear();
			return;
		}
		// add connection(s), will be empty when starting the ETE
		if (connections.isEmpty()) {
			int index = 1;
			String basename = etei.getName();
			EndToEndFlowInstance eteiClone = null;
			Stack<FlowIterator> stateClone = null;
			Iterator<ETEInfo> nestedIter = nestedETEs.iterator();

			state.push(iter);
			while (nestedIter.hasNext()) {
				ETEInfo nested = nestedIter.next();
				boolean prepareNext = nestedIter.hasNext();

				if (prepareNext) {
					stateClone = clone(state);
					eteiClone = (EndToEndFlowInstance) EcoreUtil.copy(etei);
					eteiClone.setName(basename + "_" + index++);
					eteiClone.getModesList().addAll(etei.getModesList());
				}

				addNestedETE(etei, nested);

				// prepare next connection filter
				connections.clear();
				connections.addAll(nested.postConns);
				if (iter.hasNext()) {
					Element obj = iter.next();
					Connection conn = null;
					if (obj instanceof FlowSegment) {
						FlowElement fe = ((FlowSegment) obj).getFlowElement();
						if (fe instanceof Connection)
							conn = (Connection) fe;
					} else if (obj instanceof EndToEndFlowSegment) {
						EndToEndFlowElement fe = ((EndToEndFlowSegment) obj).getFlowElement();
						if (fe instanceof Connection)
							conn = (Connection) fe;
					}
					if (conn != null)
						connections.add(conn);
				}

				continueFlow(ci, etei, state.pop(), ci);

				if (prepareNext) {
					// add clone
					etei.getContainingComponentInstance().getEndToEndFlows().add(eteiClone);
					etei = eteiClone;
					state = stateClone;
					if (etei.getFlowElements() == null || etei.getFlowElements().isEmpty()) {
						created.add(myInfo = new ETEInfo(etei));
					} else {
						created.add(myInfo = new ETEInfo(myInfo.preConns, etei));
					}
				}
			}
		} else {
			List<ConnectionInstance> connis = collectConnectionInstances(ci.getSystemInstance(), etei);

			if (connis.isEmpty()) {
				error(etei, "Incomplete end-to-end flow instance " + etei.getName()
						+ ": Missing connection instance to " + ((NamedElement) ete).getName());
				connections.clear();
			} else {
				int index = 1;
				Iterator<ConnectionInstance> connIter = connis.iterator();
				String basename = etei.getName();

				state.push(iter);
				while (connIter.hasNext()) {
					EndToEndFlowInstance eteiClone = null;
					Stack<FlowIterator> stateClone = null;
					ConnectionInstance conni = connIter.next();
					Iterator<ETEInfo> nestedIter = nestedETEs.iterator();

					while (nestedIter.hasNext()) {
						ETEInfo nested = nestedIter.next();
						boolean prepareNext = nestedIter.hasNext() || connIter.hasNext();

						if (prepareNext) {
							stateClone = clone(state);
							eteiClone = (EndToEndFlowInstance) EcoreUtil.copy(etei);
							eteiClone.setName(basename + "_" + index++);
							eteiClone.getModesList().addAll(etei.getModesList());
						}

						etei.getFlowElements().add(conni);
						addNestedETE(etei, nested);

						// prepare next connection filter
						connections.clear();
						connections.addAll(nested.postConns);
						if (iter.hasNext()) {
							connections.add((Connection) iter.next());
						}

						continueFlow(ci, etei, state.pop(), ci);

						if (prepareNext) {
							// add clone
							etei.getContainingComponentInstance().getEndToEndFlows().add(eteiClone);
							etei = eteiClone;
							state = stateClone;
							if (etei.getFlowElements() == null || etei.getFlowElements().isEmpty()) {
								created.add(myInfo = new ETEInfo(etei));
							} else {
								created.add(myInfo = new ETEInfo(myInfo.preConns, etei));
							}
						}
					}
				}
			}
		}
	}

	private void addNestedETE(EndToEndFlowInstance etei, ETEInfo nested) {
		etei.getFlowElements().add(nested.etei);
	}

	/**
	 * @param ci
	 * @param etei
	 * @param leaf
	 */
	private void addLeafElement(ComponentInstance ci, EndToEndFlowInstance etei, Element leaf) {
		FlowSpecification fs;
		FlowSpecificationInstance fsi;
		if (leaf instanceof FlowSpecification) {
			// append a flow specification instance
			fs = (FlowSpecification) leaf;
			fsi = ci.findFlowSpecInstance(fs);
			if (fsi == null) {
				doFlowSpecInstances(ci);
				fsi = ci.findFlowSpecInstance(fs);
			}
			if (fsi != null) {
				etei.getFlowElements().add(fsi);
			} else if (fs != null) {
				error(etei, "Incomplete End-to-end flow instance " + etei.getName() + ": Could not find flow spec "
						+ fs.getName() + " of component " + ci.getName());
			}
		} else if (leaf instanceof Subcomponent) {
			// append a subcomponent instance
			etei.getFlowElements().add(ci);
//		} else if (leaf instanceof DataAccess) {
//			if (ci != null) {
//				// append a data instance
//				etei.getFlowElements().add(ci);
//			} else {
//				error(etei, "Incomplete End-to-end flow instance " + etei.getName()
//						+ ": No data component connected to data access feature " + ((DataAccess) leaf).getName());
//			}
		}
	}

	/**
	 * add flowspec instances to the component instance
	 * 
	 */
	private void doFlowSpecInstances(ComponentInstance ci) {
		EList<FlowSpecification> flowspecs = InstanceUtil.getComponentType(ci, 0, null).getAllFlowSpecifications();
		for (Iterator<FlowSpecification> it = flowspecs.iterator(); it.hasNext();) {
			FlowSpecification f = it.next();
			FlowSpecificationInstance speci = InstanceFactory.eINSTANCE.createFlowSpecificationInstance();
			speci.setFlowSpecification(f);
			speci.setName(f.getName());
			FlowEnd inend = f.getAllInEnd();
			if (inend != null) {
				Feature srcfp = inend.getFeature();
				Context srcpg = inend.getContext();
				if (srcpg == null) {
					FeatureInstance fi = ci.findFeatureInstance(srcfp);
					if (fi != null)
						speci.setSource(fi);
				} else if (srcpg instanceof FeatureGroup) {
					FeatureInstance pgi = ci.findFeatureInstance((FeatureGroup) srcpg);
					if (pgi != null) {
						FeatureInstance fi = pgi.findFeatureInstance(srcfp);
						if (fi != null)
							speci.setSource(fi);
					}
				}
			}
			FlowEnd outend = f.getAllOutEnd();
			if (outend != null) {
				Feature dstfp = outend.getFeature();
				Context dstpg = outend.getContext();
				if (dstpg == null) {
					FeatureInstance fi = ci.findFeatureInstance(dstfp);
					if (fi != null)
						speci.setDestination(fi);
				} else if (dstpg instanceof FeatureGroup) {
					FeatureInstance pgi = ci.findFeatureInstance((FeatureGroup) dstpg);
					if (pgi != null) {
						FeatureInstance fi = pgi.findFeatureInstance(dstfp);
						if (fi != null)
							speci.setDestination(fi);
					}
				}
			}
			ci.getFlowSpecifications().add(speci);
			for (Mode mode : f.getAllInModes()) {
				ModeInstance mi = ci.findModeInstance(mode);
				if (mi != null) {
					speci.getInModes().add(mi);
				}
			}

			for (ModeTransition mt : f.getInModeTransitions()) {
				ModeTransitionInstance ti = ci.findModeTransitionInstance(mt);

				if (ti != null) {
					speci.getInModeTransitions().add(ti);
				}
			}
		}
	}

	private void continueFlow(ComponentInstance ci, EndToEndFlowInstance etei, FlowIterator iter,
			NamedElement errorElement) {
		while (true) {
			if (ci == null) {
				error(errorElement, "Flow instance leaves system instance for flow " + etei.getInstanceObjectPath());
				connections.clear();
				return;
			}
			while (iter.hasNext()) {
				Element e = iter.next();
				processETESegment(ci, etei, e, iter, errorElement);
			}
			if (state.size() == 0) {
				// a flow is done
				fillinModes(etei);
				myInfo.postConns.addAll(connections);
				connections.clear();
				break;
			}
			iter = state.pop();
			ci = ci.getContainingComponentInstance();
		}
	}

	// -------------------------------------------------------------------------
	// Helper methods
	// -------------------------------------------------------------------------

	/**
	 * Get all connection instances that pass through the sequence of
	 * declarative connections.
	 */
	private List<ConnectionInstance> collectConnectionInstances(ComponentInstance ci, EndToEndFlowInstance etei) {
		List<ConnectionInstance> result = new ArrayList<ConnectionInstance>();

		for (ConnectionInstance conni : ci.getSystemInstance().getAllConnectionInstances()) {
			if (testConnection(conni, etei)) {
				result.add(conni);
			}
		}
		return result;
	}

	/**
	 * @param conni
	 * @param etei
	 * @param result
	 */
	private boolean testConnection(ConnectionInstance conni, EndToEndFlowInstance etei) {
		Iterator<ConnectionReference> refIter = conni.getConnectionReferences().iterator();
		boolean match = false;

		while (refIter.hasNext()) {
			if (refIter.next().getConnection() == connections.get(0)) {
				Iterator<Connection> connIter = connections.iterator();

				connIter.next();
				match = true;
				while (match && refIter.hasNext() && connIter.hasNext()) {
					match &= refIter.next().getConnection() == connIter.next();
				}
				if (!refIter.hasNext() && connIter.hasNext()) {
					match = false;
				}
			}
		}
		if (match && connections.size() == 1) {
			// make sure connection instance goes in the same direction as the flow
			ComponentInstance connci = conni.getSource().getComponentInstance();
			FlowElementInstance fei = etei;

			while (fei instanceof EndToEndFlowInstance) {
				fei = ((EndToEndFlowInstance) fei).getFlowElements().get(
						((EndToEndFlowInstance) fei).getFlowElements().size() - 1);
			}
			if (fei instanceof FlowSpecificationInstance) {
				fei = fei.getComponentInstance();
			}
			ComponentInstance flowci = (ComponentInstance) fei;

			match = false;
			ComponentInstance ci = connci;
			while (!(ci instanceof SystemInstance)) {
				if (ci == flowci) {
					match = true;
					break;
				}
				ci = ci.getContainingComponentInstance();
			}
		}
		return match;
	}

	// -------------------------------------------------------------------------
	// Mode utilities
	// -------------------------------------------------------------------------

	/**
	 * build mode instance list from mode list relative to the component
	 * instance ci
	 * 
	 * @param ci Component Instance
	 * @param mlist mode list
	 * @return list of mode instances
	 */
	protected EList<ModeInstance> getModeInstances(ComponentInstance ci, ModalElement e) {
		EList<ModeInstance> mis = new BasicEList<ModeInstance>();
		List<Mode> mlist = e.getAllInModes();

		if (!mlist.isEmpty()) {
			for (Mode m : mlist) {
				ModeInstance mi = ci.findModeInstance(m);

				if (mi != null) {
					mis.add(mi);
				}
			}
		} else {
			// get modes form containment hierarchy
			while (!(ci instanceof SystemInstance)) {
				if (ci.getInModes().isEmpty()) {
					ci = ci.getContainingComponentInstance();
				} else {
					mis = ci.getInModes();
					break;
				}
			}
		}
		return mis;
	}

	protected void fillinModes(EndToEndFlowInstance etei) {

		if (etei.getSystemInstance().getSystemOperationModes().size() <= 1) {
			return;
		}

		// first, calculate intersection of all connection and ete instance SOMs
		EList<FlowElementInstance> feis = etei.getFlowElements();
		List<SystemOperationMode> soms = new ArrayList<SystemOperationMode>(etei.getSystemInstance()
				.getSystemOperationModes());

		for (FlowElementInstance fei : feis) {
			List<SystemOperationMode> newSoms = new ArrayList<SystemOperationMode>();

			if (fei instanceof ConnectionInstance) {
				ConnectionInstance conni = (ConnectionInstance) fei;

				if (conni.getInSystemOperationModes().isEmpty()) {
					continue;
				}
				for (SystemOperationMode som : soms) {
					if (conni.getInSystemOperationModes().contains(som)) {
						newSoms.add(som);
					}
				}
			} else if (fei instanceof EndToEndFlowInstance) {
				EndToEndFlowInstance efi = (EndToEndFlowInstance) fei;

				if (efi.getInSystemOperationModes().isEmpty()) {
					continue;
				}
				for (SystemOperationMode som : soms) {
					if (efi.getInSystemOperationModes().contains(som)) {
						newSoms.add(som);
					}
				}
			} else {
				continue;
			}
			soms = newSoms;
		}

		// then, keep those SOMs where all other flow elements are active
		for (FlowElementInstance fei : feis) {
			List<SystemOperationMode> newSoms = new ArrayList<SystemOperationMode>();
			if (fei instanceof FlowSpecificationInstance) {
				FlowSpecificationInstance fsi = (FlowSpecificationInstance) fei;

				for (SystemOperationMode som : soms) {
					if (fsi.isActive(som)) {
						newSoms.add(som);
					}
				}
			} else if (fei instanceof ComponentInstance) {
				ComponentInstance ci = (ComponentInstance) fei;

				for (SystemOperationMode som : soms) {
					if (ci.isActive(som)) {
						newSoms.add(som);
					}
				}
			} else {
				continue;
			}
			soms = newSoms;
		}

		// finally, keep those SOMs where the ete and used flow implementations are active
		for (SystemOperationMode som : soms) {
			if (containsModeInstances(som, etei.getModesList())) {
				etei.getInSystemOperationModes().add(som);
			}
		}

		etei.getModesList().clear();
	}

	private boolean containsModeInstances(SystemOperationMode som, List<EList<ModeInstance>> modeLists) {
		outer: for (List<ModeInstance> mis : modeLists) {
			for (ModeInstance mi : mis) {
				if (som.getCurrentModes().contains(mi)) {
					continue outer;
				}
			}
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------
	// Stack utilities
	// -------------------------------------------------------------------------

	private Stack<FlowIterator> clone(Stack<FlowIterator> state) {
		Stack<FlowIterator> clone = new Stack<FlowIterator>();
		for (int i = 0; i < state.size(); i++) {
			clone.push(state.get(i).clone());
		}
		return clone;
	}

}
