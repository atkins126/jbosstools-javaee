/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.seam.core;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.jboss.tools.common.model.util.EclipseResourceUtil;
import org.jboss.tools.seam.internal.core.project.facet.ISeamFacetDataModelProperties;

/**
 * Helper class that collects related J2EE projects for 
 * a given WAR project.
 * 
 * @author Viacheslav Kabanovich
 */
public class SeamProjectsSet {
	IProject ear;
	IProject war;
	IProject ejb;
	IProject test;
	IProject[] childProjects = new IProject[0];
	IEclipsePreferences prefs;

	/**
	 * @param project EAR, EJB or Web Seam project
	 * @return
	 */
	public static SeamProjectsSet create(IProject project) {
		return new SeamProjectsSet(project);
	}

	/**
	 * @param project EAR, EJB or Web Seam project
	 */
	public SeamProjectsSet(IProject project) {
		ISeamProject seamProject = SeamCorePlugin.getSeamProject(project, false);
		if(seamProject==null) {
			// maybe the project is EAR? If so it doesn't have seam nature.
			ISeamProject warProject = SeamUtil.findReferencingSeamWarProjectForProject(project);
			if(warProject!=null) {
				initWebProject(warProject.getProject());
			}
		} else {
			IScopeContext projectScope = new ProjectScope(project);
			IEclipsePreferences preferences = projectScope.getNode(SeamCorePlugin.PLUGIN_ID);
			if(preferences!=null) {
				String parentProjectName = preferences.get(ISeamFacetDataModelProperties.SEAM_PARENT_PROJECT, null);
				if(parentProjectName==null) {
					// The project is main web seam project.
					initWebProject(project);
				} else {
					IProject warProject = ResourcesPlugin.getWorkspace().getRoot().getProject(parentProjectName);
					if(warProject!=null) {
						initWebProject(warProject);
					}
				}
			}
		}
	}

	public void initWebProject(IProject warProject) {
		IScopeContext projectScope = new ProjectScope(warProject);
		prefs = projectScope.getNode(SeamCorePlugin.PLUGIN_ID);

		war = warProject;

		if(prefs!=null) {
			String earName = prefs.get(
					ISeamFacetDataModelProperties.SEAM_EAR_PROJECT, warProject.getName()+"-ear"); //$NON-NLS-1$
			if(earName!=null && !"".equals(earName.trim())) { //$NON-NLS-1$
				ear = (IProject)warProject.getWorkspace().getRoot().findMember(earName);
			}
			String ejbName = prefs.get(
					ISeamFacetDataModelProperties.SEAM_EJB_PROJECT, warProject.getName()+"-ejb"); //$NON-NLS-1$
			if(ejbName!=null && !"".equals(ejbName.trim())) { //$NON-NLS-1$
				ejb = (IProject)warProject.getWorkspace().getRoot().findMember(ejbName);
			}
			String testName = prefs.get(
					ISeamFacetDataModelProperties.SEAM_TEST_PROJECT, warProject.getName()+"test"); //$NON-NLS-1$
			if(testName!=null && !"".equals(testName)) { //$NON-NLS-1$
				test = (IProject)warProject.getWorkspace().getRoot().findMember(testName);
			}

			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			Set<IProject> childProjectSet = new HashSet<IProject>();
			for (int i = 0; i < projects.length; i++) {
				ISeamProject seamProject = SeamCorePlugin.getSeamProject(projects[i], false);
				if(seamProject!=null) {
					projectScope = new ProjectScope(projects[i]);
					IEclipsePreferences preferences = projectScope.getNode(SeamCorePlugin.PLUGIN_ID);
					if(preferences!=null) {
						String parentProjectName = preferences.get(ISeamFacetDataModelProperties.SEAM_PARENT_PROJECT, null);
						if(parentProjectName!=null && parentProjectName.equals(warProject.getName())) {
							childProjectSet.add(projects[i]);
						}
					}
				}
			}
			childProjects = childProjectSet.toArray(new IProject[childProjectSet.size()]);
		}
	}


	/**
	 * @return all child projects of main web seam project.
	 */
	public IProject[] getChildProjects() {
		return childProjects;
	}

	/**
	 * @return all Seam projects of one application
	 */
	public IProject[] getAllProjects() {
		IProject[] projects = new IProject[childProjects.length + 1];
		for (int i = 0; i < childProjects.length; i++) {
			projects[i] = childProjects[i];
		}
		projects[childProjects.length] = war;
		return projects;
	}

	/**
	 * @return default deploy type for Seam project set
	 */
	public String getDefaultDeployType() {
		if(ejb!=null && war!=ejb) {
			return ISeamFacetDataModelProperties.DEPLOY_AS_EAR;
		}
		return ISeamFacetDataModelProperties.DEPLOY_AS_WAR;
	}

	public boolean isWarConfiguration() {
		if(prefs==null) {
			return false;
		}
		return prefs.get(ISeamFacetDataModelProperties.JBOSS_AS_DEPLOY_AS, ISeamFacetDataModelProperties.DEPLOY_AS_WAR).equals(ISeamFacetDataModelProperties.DEPLOY_AS_WAR);
	}

	/**
	 * Returns list of WAR projects.
	 * @return
	 */
	public IProject getWarProject() {
		return war;
	}

	/**
	 * Returns EAR project or null, if WAR project is not used by EAR.
	 * @return
	 */
	public IProject getEarProject() {
		return ear;
	}

	/**
	 * Returns list of EJB projects.
	 * @return
	 */
	public IProject getEjbProject() {
		return ejb;
	}

	/**
	 * 
	 * @return
	 */
	public IProject getTestProject() {
		return test;
	}

	public IContainer getDefaultActionFolder() {
		return getDefaultEjbSourceFolder();
	}

	/**
	 * 
	 * @return the action folder (this folder is not guaranteed to exist!)
	 */	
	public IContainer getActionFolder() {
		String folderPath = null;
		if(prefs!=null) {
			folderPath = prefs.get(ISeamFacetDataModelProperties.SESSION_BEAN_SOURCE_FOLDER, null);
		}
		if(folderPath==null || folderPath.length()==0) {
			return getDefaultActionFolder();
		}

		return (IContainer)ResourcesPlugin.getWorkspace().getRoot().findMember(folderPath);
	}

	/**
	 * @return the model folder if exists (this folder is not guaranteed to exist!)
	 */
	public IContainer getDefaultModelFolder() {
		return getDefaultEjbSourceFolder();
	}

	/**
	 * @return the model folder if exists (this folder is not guaranteed to exist!)
	 */
	public IContainer getModelFolder() {
		String folderPath = null;
		if(prefs!=null) {
			folderPath = prefs.get(ISeamFacetDataModelProperties.ENTITY_BEAN_SOURCE_FOLDER, null);
		}
		if(folderPath==null || folderPath.length()==0) {
			return getDefaultModelFolder();
		}

		return (IContainer)ResourcesPlugin.getWorkspace().getRoot().findMember(folderPath);
	}

	public IContainer getDefaultEjbSourceFolder() {
		return getSrc(ejb); 
	}

	public IContainer getDefaultTestSourceFolder() {
		return getSrc(test);
	}

	public IContainer getDefaultWarSourceFolder() {
		IFolder webSrcFolder = findWebSrcFolder();
		if(webSrcFolder!=null) {
			return webSrcFolder;
		}
		return getSrc(war);
	}

	private IContainer getSrc(IProject project) {
		if(project==null) {
			project = war;
		}
		IResource resource = EclipseResourceUtil.getJavaSourceRoot(project);
		if(resource!=null) {
			return (IContainer) resource;
		}
		return project;
	}

	/**
	 * Returns default web contents folder.
	 * @return
	 */
	public IContainer getDefaultViewsFolder() {
		IVirtualComponent com = ComponentCore.createComponent(war);
		if(com!=null) {
			IVirtualFolder webRootFolder = com.getRootFolder().getFolder(new Path("/")); //$NON-NLS-1$
			if(webRootFolder!=null) {
				return (IFolder)webRootFolder.getUnderlyingFolder();
			}
		}
		return getWarProject();
	}
	
	/**
	 * Returns default ear contents folder.
	 * @return
	 */
	public IContainer getDefaultEarViewsFolder() {
		IVirtualComponent com = ComponentCore.createComponent(ear);
		if(com!=null) {
			IVirtualFolder webRootFolder = com.getRootFolder().getFolder(new Path("/")); //$NON-NLS-1$
			if(webRootFolder!=null) {
				return (IFolder)webRootFolder.getUnderlyingFolder();
			}
		}
		return getWarProject();
	}

	/**
	 * Returns web contents folder.
	 * @return
	 */
	public IContainer getViewsFolder() {
		String folderPath = null;
		if(prefs!=null) {
			folderPath = prefs.get(ISeamFacetDataModelProperties.WEB_CONTENTS_FOLDER, null);
		}
		if(folderPath==null || folderPath.length()==0) {
			return getDefaultViewsFolder();
		}

		return (IContainer)ResourcesPlugin.getWorkspace().getRoot().findMember(folderPath);
	}

	/**
	 * Returns source folder for test cases.
	 * @return
	 */
	public IContainer getTestsFolder() {
		String folderPath = null;
		if(prefs!=null) {
			folderPath = prefs.get(ISeamFacetDataModelProperties.TEST_SOURCE_FOLDER, null);
		}
		if(folderPath==null || folderPath.length()==0) {
			return getDefaultTestSourceFolder();
		}

		return (IContainer)ResourcesPlugin.getWorkspace().getRoot().findMember(folderPath);
	}

	public String getEntityPackage(){
		if(prefs==null) {
			return "entity";
		}
		return prefs.get(ISeamFacetDataModelProperties.ENTITY_BEAN_PACKAGE_NAME, "entity"); //$NON-NLS-1$
	}

	public void refreshLocal(IProgressMonitor monitor) throws CoreException {
		if(ejb!=null) { 
			ejb.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		if(test!=null) {
			test.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		if(war!=null) {
			war.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
	}

	private IFolder findWebSrcFolder() {
		IVirtualComponent component = ComponentCore.createComponent(war);
		if(component!=null) {
			IVirtualFolder vFolder = component.getRootFolder().getFolder("WEB-INF/classes");
			return (IFolder)vFolder.getUnderlyingFolder();
		}
		return null;
	}
}