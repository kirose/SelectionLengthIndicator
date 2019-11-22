/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Angelo Zerr <angelo.zerr@gmail.com> - [CodeMining] Provide extension point for CodeMining - Bug 528419
 *******************************************************************************/
package org.eclipse.ui.internal.texteditor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.BundleContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.internal.texteditor.codemining.CodeMiningProviderRegistry;
import org.eclipse.ui.internal.texteditor.quickdiff.QuickDiffExtensionsRegistry;
import org.eclipse.ui.internal.texteditor.spelling.SpellingEngineRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plug-in runtime class for the text editor UI plug-in (id <code>"org.eclipse.ui.workbench.texteditor"</code>).
 * This class provides static methods for:
 * <ul>
 *  <li> getting the last editor position.</li>
 * </ul>
 * <p>
 * This class provides static methods and fields only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 *
 * @since 2.1
 */
public final class TextEditorPlugin extends AbstractUIPlugin implements IRegistryChangeListener {

	/** The plug-in instance */
	private static TextEditorPlugin fgPlugin;

	/** The last edit position */
	private EditPosition fLastEditPosition;
	/** The action which goes to the last edit position */
	private Set<IAction> fLastEditPositionDependentActions;

	/**
	 * The quick diff extension registry.
	 * @since 3.0
	 */
	private QuickDiffExtensionsRegistry fQuickDiffExtensionRegistry;

	/**
	 * The spelling engine registry
	 * @since 3.1
	 */
	private SpellingEngineRegistry fSpellingEngineRegistry;

	/**
	 * The codemining provider registry
	 *
	 * @since 3.10
	 */
	private CodeMiningProviderRegistry fCodeMiningProviderRegistry;

	/**
	 * Creates a plug-in instance.
	 */
	public TextEditorPlugin() {
		super();
		Assert.isTrue(fgPlugin == null);
		fgPlugin= this;
	}

	/**
	 * Returns the plug-in instance.
	 *
	 * @return the text editor plug-in instance
	 * @since 3.0
	 */
	public static TextEditorPlugin getDefault() {
		return fgPlugin;
	}

	/**
	 * Text editor UI plug-in Id (value <code>"org.eclipse.ui.workbench.texteditor"</code>).
	 */
	public static final String PLUGIN_ID= "org.eclipse.ui.workbench.texteditor"; //$NON-NLS-1$

	/**
	 * Extension Id of quick diff reference provider extension point.
	 * (value <code>"quickDiffReferenceProvider"</code>).
	 * @since 3.0
	 */
	public static final String REFERENCE_PROVIDER_EXTENSION_POINT= "quickDiffReferenceProvider"; //$NON-NLS-1$

	/**
	 * Returns the last edit position.
	 *
	 * @return	the last edit position or <code>null</code>	if there is no last edit position
	 * @see EditPosition
	 */
	public EditPosition getLastEditPosition() {
		return fLastEditPosition;
	}

	/**
	 * Sets the last edit position.
	 *
	 * @param lastEditPosition	the last edit position
	 * @see EditPosition
	 */
	public void setLastEditPosition(EditPosition lastEditPosition) {
		fLastEditPosition= lastEditPosition;
		if (fLastEditPosition != null && fLastEditPositionDependentActions != null) {
			Iterator<IAction> iter= fLastEditPositionDependentActions.iterator();
			while (iter.hasNext())
				iter.next().setEnabled(true);
			fLastEditPositionDependentActions= null;
		}
	}

	/**
	 * Adds the given action to the last edit position dependent actions.
	 *
	 * @param action the goto last edit position action
	 */
	public void addLastEditPositionDependentAction(IAction action) {
		if (fLastEditPosition != null)
			return;
		if (fLastEditPositionDependentActions == null)
			fLastEditPositionDependentActions= new HashSet<>();
		fLastEditPositionDependentActions.add(action);
	}

	/**
	 * Removes the given action from the last edit position dependent actions.
	 *
	 * @param action the action that depends on the last edit position
	 */
	public void removeLastEditPositionDependentAction(IAction action) {
		if (fLastEditPosition != null)
			return;
		if (fLastEditPositionDependentActions != null)
			fLastEditPositionDependentActions.remove(action);
	}


	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fQuickDiffExtensionRegistry= new QuickDiffExtensionsRegistry();
		fSpellingEngineRegistry= new SpellingEngineRegistry();
		fCodeMiningProviderRegistry = new CodeMiningProviderRegistry();
		Platform.getExtensionRegistry().addRegistryChangeListener(this, PLUGIN_ID);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		Platform.getExtensionRegistry().removeRegistryChangeListener(this);
		fQuickDiffExtensionRegistry= null;
		fSpellingEngineRegistry= null;
		fCodeMiningProviderRegistry = null;
		super.stop(context);
	}

	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		if (fQuickDiffExtensionRegistry != null && event.getExtensionDeltas(PLUGIN_ID, REFERENCE_PROVIDER_EXTENSION_POINT).length > 0)
			fQuickDiffExtensionRegistry.reloadExtensions();
		if (fSpellingEngineRegistry != null && event.getExtensionDeltas(PLUGIN_ID, SpellingEngineRegistry.SPELLING_ENGINE_EXTENSION_POINT).length > 0)
			fSpellingEngineRegistry.reloadExtensions();
		if (fCodeMiningProviderRegistry != null && event.getExtensionDeltas(PLUGIN_ID,
				CodeMiningProviderRegistry.CODEMINING_PROVIDERS_EXTENSION_POINT).length > 0)
			fCodeMiningProviderRegistry.reloadExtensions();
	}

	/**
	 * Returns this plug-ins quick diff extension registry.
	 *
	 * @return the quick diff extension registry or <code>null</code> if this plug-in has been shutdown
	 * @since 3.0
	 */
	public QuickDiffExtensionsRegistry getQuickDiffExtensionRegistry() {
		return fQuickDiffExtensionRegistry;
	}

	/**
	 * Returns this plug-ins spelling engine registry.
	 *
	 * @return the spelling engine registry or <code>null</code> if this plug-in has been shutdown
	 * @since 3.1
	 */
	public SpellingEngineRegistry getSpellingEngineRegistry() {
		return fSpellingEngineRegistry;
	}

	/**
	 * Returns this plug-ins codemining provider registry.
	 *
	 * @return the codemining provider registry or <code>null</code> if this plug-in
	 *         has been shutdown
	 * @since 3.10
	 */
	public CodeMiningProviderRegistry getCodeMiningProviderRegistry() {
		return fCodeMiningProviderRegistry;
	}
}
