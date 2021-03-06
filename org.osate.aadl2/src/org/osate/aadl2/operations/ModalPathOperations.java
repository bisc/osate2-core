/**
 * <copyright>
 * Copyright  2011 by Carnegie Mellon University, all rights reserved.
 * 
 * Use of the Open Source AADL Tool Environment (OSATE) is subject to the terms of the license set forth
 * at http://www.eclipse.org/org/documents/epl-v10.html.
 * 
 * NO WARRANTY
 * 
 * ANY INFORMATION, MATERIALS, SERVICES, INTELLECTUAL PROPERTY OR OTHER PROPERTY OR RIGHTS GRANTED OR PROVIDED BY
 * CARNEGIE MELLON UNIVERSITY PURSUANT TO THIS LICENSE (HEREINAFTER THE ''DELIVERABLES'') ARE ON AN ''AS-IS'' BASIS.
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
 * </copyright>
 * 
 */
package org.osate.aadl2.operations;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.BasicInternalEList;
import org.osate.aadl2.ModalPath;
import org.osate.aadl2.Mode;
import org.osate.aadl2.ModeFeature;
import org.osate.aadl2.ModeTransition;
import org.osate.aadl2.RefinableElement;

/**
 * <!-- begin-user-doc -->
 * A static utility class that provides operations related to '<em><b>Modal Path</b></em>' model objects.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following operations are supported:
 * <ul>
 *   <li>{@link org.osate.aadl2.ModalPath#getInModes() <em>Get In Modes</em>}</li>
 *   <li>{@link org.osate.aadl2.ModalPath#getInModeTransitions() <em>Get In Mode Transitions</em>}</li>
 *   <li>{@link org.osate.aadl2.ModalPath#getAllInModeTransitions() <em>Get All In Mode Transitions</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ModalPathOperations extends ModalElementOperations {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ModalPathOperations() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public static EList<Mode> getInModes(ModalPath modalPath) {
		// DONE: implement this method
		EList<Mode> inModes = new BasicInternalEList<Mode>(Mode.class);
		for (ModeFeature mf : modalPath.getInModeOrTransitions()) {
			if (mf instanceof Mode) {
				inModes.add((Mode) mf);
			}
		}
		return inModes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public static EList<ModeTransition> getInModeTransitions(ModalPath modalPath) {
		// DONE: implement this method
		EList<ModeTransition> inModeTransitions = new BasicInternalEList<ModeTransition>(
				ModeTransition.class);
		for (ModeFeature mf : modalPath.getInModeOrTransitions()) {
			if (mf instanceof ModeTransition) {
				inModeTransitions.add((ModeTransition) mf);
			}
		}
		return inModeTransitions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public static EList<ModeTransition> getAllInModeTransitions(
			ModalPath modalPath) {
		// DONE: implement this method
		EList<ModeTransition> intransitions = null;
		// inmodes will be an empty list (all modes) if we do not find a non-empty list
		while (modalPath != null) {
			intransitions = modalPath.getInModeTransitions();
			// we stop when we find the first occurrence of a non-empty inmodes list
			if (intransitions != null && !intransitions.isEmpty())
				return intransitions;
			if (modalPath instanceof RefinableElement)
				modalPath = (ModalPath) ((RefinableElement) modalPath)
						.getRefinedElement();
			else
				modalPath = null;
		}
		return intransitions;
	}

} // ModalPathOperations