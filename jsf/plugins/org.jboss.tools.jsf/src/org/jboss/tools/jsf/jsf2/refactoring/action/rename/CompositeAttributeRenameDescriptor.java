/*******************************************************************************
 * Copyright (c) 2007-2010 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.jsf.jsf2.refactoring.action.rename;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;

/**
 * 
 * @author yzhishko
 *
 */

@SuppressWarnings("restriction")
public class CompositeAttributeRenameDescriptor implements IRenameDescriptor{

	private String currentAttrName;
	private RenameRefactoring refactoring;
	private CompositeAttributeRenameProcessor processor;
	
	public CompositeAttributeRenameDescriptor(IDOMAttr attr, IFile file, String URI) {
		this.setCurrentAttrName(attr.getValue());
		refactoring = new RenameRefactoring();
		processor = new CompositeAttributeRenameProcessor(file.getProject());
		processor.setCurrentAttrName(getCurrentName());
		processor.setAttrToRename(attr);
		processor.setBaseFile(file);
		processor.setURI(URI);
	}
	
	public RenameUserInterfaceManager getInterfaceManager() {
		return RenameUserInterfaceManager.getDefault();
	}

	public RenameRefactoring getRenameRefactoring() {
		return refactoring;
	}

	public void setCurrentAttrName(String currentAttrName) {
		this.currentAttrName = currentAttrName;
	}

	public String getCurrentName() {
		return currentAttrName;
	}

	public RefactoringProcessor getRefactoringProcessor() {
		return processor;
	}
	
}
