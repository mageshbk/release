/* 
 * JBoss, Home of Professional Open Source 
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved. 
 * See the copyright.txt in the distribution for a 
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use, 
 * modify, copy, or redistribute it subject to the terms and conditions 
 * of the GNU Lesser General Public License, v. 2.1. 
 * This program is distributed in the hope that it will be useful, but WITHOUT A 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details. 
 * You should have received a copy of the GNU Lesser General Public License, 
 * v.2.1 along with this distribution; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA  02110-1301, USA.
 */
package org.switchyard.as7.extension.deployment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.apache.log4j.Logger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.switchyard.as7.extension.SwitchYardDeploymentMarker;
import org.switchyard.common.cdi.CDIUtil;
import org.switchyard.common.type.Classes;

/**
 * Deployment processor that installs the SwitchYard CDI extension.
 * 
 * @author Magesh Kumar B <mageshbk@jboss.com> (C) 2011 Red Hat Inc.
 */
public class SwitchYardCdiIntegrationProcessor implements DeploymentUnitProcessor {

    private static final String SWITCHYARD_CDI_EXTENSION = "org.switchyard.component.bean.SwitchYardCDIServiceDiscovery";
    private static final String DELTASPIKE_CDI_EXTENSION = "org.apache.deltaspike.core.api.provider.BeanManagerProvider";

    private static Logger _logger = Logger.getLogger(SwitchYardCdiIntegrationProcessor.class);

    /* (non-Javadoc)
     * @see org.jboss.as.server.deployment.DeploymentUnitProcessor#deploy(org.jboss.as.server.deployment.DeploymentPhaseContext)
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!SwitchYardDeploymentMarker.isSwitchYardDeployment(deploymentUnit)) {
            return;
        }

        if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            // Add the Weld portable extension
            final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
            synchronized (parent) {
                checkExtension(SWITCHYARD_CDI_EXTENSION, deploymentUnit, parent);
                checkExtension(DELTASPIKE_CDI_EXTENSION, deploymentUnit, parent);
            }
        } else {
            _logger.debug("SwitchYard Application for deployment unit '" + deploymentUnit.getName() + "' does not appear to contain CDI Beans "
                    + "(no META-INF/beans.xml file in unit).  Not attaching SwitchYard CDI Discovery Extension to deployment.");
        }
    }

    private void checkExtension(final String extensionName, DeploymentUnit deploymentUnit, DeploymentUnit parent) throws DeploymentUnitProcessingException {
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final List<Metadata<Extension>> extensions = parent.getAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS);

        boolean found = false;
        for (Metadata<Extension> extension : extensions) {
            if (extension.getLocation().equals(extensionName)) {
                found = true;
                break;
            }
        }

        if (!found) {
            _logger.debug("SwitchYard Application for deployment unit '" + deploymentUnit.getName() + "' contains CDI Beans.  "
                    + "Attaching SwitchYard CDI Discovery (" + extensionName + ") to deployment.");

            try {
                Class<?> extensionClass = module.getClassLoader().loadClass(extensionName);
                Extension extensionInstance = null;
                if (DELTASPIKE_CDI_EXTENSION.equals(extensionName)) {
                    // Deltaspike BeanManagerProvider is singleton
                    Method m = extensionClass.getMethod("getInstance");
                    try {
                        extensionInstance = (Extension) m.invoke(null);
                    } catch (InvocationTargetException ite) {
                        if (ite.getCause() instanceof IllegalStateException) {
                            extensionInstance = (Extension) extensionClass.newInstance();
                            m = extensionClass.getDeclaredMethod("setBeanManagerProvider", extensionClass);
                            m.setAccessible(true);
                            m.invoke(null, extensionInstance);
                            m.setAccessible(false);
                        }
                    }
                    if (deploymentUnit.getParent() != null) {
                        // Trick DeltaSpike to create a BeanManagerInfo based on this deployment module
                        ClassLoader old = Classes.setTCCL(module.getClassLoader());
                        CDIUtil.lookupBeanManager();
                        Classes.setTCCL(old);
                    }
                } else {
                    extensionInstance = (Extension) extensionClass.newInstance();
                }

                Metadata<Extension> metadata = new MetadataImpl<Extension>(extensionInstance, deploymentUnit.getName());
                parent.addToAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS, metadata);
            } catch (InstantiationException ie) {
                throw new DeploymentUnitProcessingException(ie);
            } catch (IllegalAccessException iae) {
                throw new DeploymentUnitProcessingException(iae);
            } catch (ClassNotFoundException cnfe) {
                throw new DeploymentUnitProcessingException(cnfe);
            } catch (NoSuchMethodException nsme) {
                throw new DeploymentUnitProcessingException(nsme);
            } catch (InvocationTargetException ite) {
                throw new DeploymentUnitProcessingException(ite);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.server.deployment.DeploymentUnitProcessor#undeploy(org.jboss.as.server.deployment.DeploymentUnit)
     */
    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private class MetadataImpl<T> implements Metadata<T> {

        private T _extensionInstance;
        private String _location;

        public MetadataImpl(T extensionInstance, String location) {
            _extensionInstance = extensionInstance;
            _location = location;
        }

        @Override
        public T getValue() {
            return _extensionInstance;
        }

        @Override
        public String getLocation() {
            return _location;
        }
    }

}
