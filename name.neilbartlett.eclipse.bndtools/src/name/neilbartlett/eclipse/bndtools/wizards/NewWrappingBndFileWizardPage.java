/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import name.neilbartlett.eclipse.bndtools.utils.PathUtils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Version;

import aQute.lib.osgi.Constants;

public class NewWrappingBndFileWizardPage extends NewBndFileWizardPage {
	
	private static final String LIST_SEPARATOR = ",\\\n\t"; //$NON-NLS-1$
	private static final Object ASSIGNMENT_SEPARATOR = ": "; //$NON-NLS-1$

	private Collection<? extends IPath> paths;
	private Text txtBSN;
	private Text txtVersion;
	
	private String bsn = "";
	private Version version = new Version(0,0,0);
	private String error = null;

	public NewWrappingBndFileWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle("New Wrapper Script");
		setMessage("Specify a file name for the new wrapper script");
	}
	
	public void setPaths(Collection<? extends IPath> paths) {
		this.paths = paths;
	}
	
	@Override
	protected void createAdvancedControls(Composite parent) {
		// Override the existing advanced controls
		Composite composite = new Composite(parent, SWT.NONE);
		new Label(composite, SWT.NONE).setText("Bundle Symbolic Name:");
		txtBSN = new Text(composite, SWT.BORDER);
		new Label(composite, SWT.NONE).setText("Bundle Version:");
		txtVersion = new Text(composite, SWT.BORDER);
		
		txtBSN.setText(bsn);
		txtVersion.setText(version.toString());
		
		txtBSN.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				bsn = txtBSN.getText();
				getContainer().updateButtons();
				getContainer().updateMessage();
			}
		});
		txtVersion.addListener(SWT.Modify, new Listener() {;
			public void handleEvent(Event event) {
				try {
					version = Version.parseVersion(txtVersion.getText());
					error = null;
				} catch (IllegalArgumentException e) {
					version = null;
					error = "Invalid version format";
				}
				getContainer().updateButtons();
				getContainer().updateMessage();
			}
		});
		
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		txtBSN.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtVersion.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		super.createAdvancedControls(parent);
	}
	@Override
	protected IStatus validateLinkedResource() {
		return super.validateLinkedResource(); //Status.OK_STATUS;
	}
	@Override
	public boolean isPageComplete() {
		return super.isPageComplete() && version != null;
	}
	@Override
	public String getErrorMessage() {
		String error = super.getErrorMessage();
		if(error != null)
			return error;
		
		return this.error;
	}
	@Override
	protected InputStream getInitialContents() {
		StringBuilder builder = new StringBuilder();
		
		// Do -classpath
		IPath containerPath = getContainerFullPath();
		builder.append(Constants.CLASSPATH).append(ASSIGNMENT_SEPARATOR);
		for (Iterator<? extends IPath> iterator = paths.iterator(); iterator.hasNext();) {
			IPath path = iterator.next();
			
			if(path.isAbsolute()) {
				builder.append(path.toString());
			} else {
				IPath relative = PathUtils.makeRelativeTo(path, containerPath); //path.makeRelativeTo(containerPath);
				builder.append(relative.toString());
			}
			if(iterator.hasNext())
				builder.append(LIST_SEPARATOR);
		}
		builder.append('\n');
		
		// Do BSN and Bundle-Version
		if(bsn != null && bsn.length() > 0)
			builder.append(Constants.BUNDLE_SYMBOLICNAME).append(ASSIGNMENT_SEPARATOR).append(bsn).append('\n');
		builder.append(Constants.BUNDLE_VERSION).append(ASSIGNMENT_SEPARATOR).append(version.toString()).append('\n');
		
		// Do Export Package
		builder.append(Constants.EXPORT_PACKAGE).append(ASSIGNMENT_SEPARATOR).append("*;") //$NON-NLS-1$
		       .append(Constants.VERSION_ATTRIBUTE).append('=')
		       .append("${").append(Constants.BUNDLE_VERSION).append("}\n"); //$NON-NLS-1$ //$NON-NLS-2$
		
		return new ByteArrayInputStream(builder.toString().getBytes());
	}

}
