/*-
 * #%L
 * Ellipsoid Surface Projection
 * %%
 * Copyright (C) 2016 - 2018 Tobias Pietzsch
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.mpicbg.ovaries;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import org.scijava.ui.behaviour.KeyStrokeAdder;

import bdv.BigDataViewer;
import bdv.tools.ToggleDialogAction;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.NavigationActions;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import ij.ImageJ;
import mpicbg.spim.data.SpimDataException;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

public class OvariesProjectionExample
{
	protected OvariesProjectionExample( final String xmlFilename ) throws SpimDataException
	{
		final BigDataViewer bdv = BigDataViewer.open( xmlFilename, "blob detection", null, ViewerOptions.options() );
		final ViewerFrame viewerFrame = bdv.getViewerFrame();
		final ViewerPanel viewer = bdv.getViewer();
		final SetupAssignments setupAssignments = bdv.getSetupAssignments();

		// =============== the dialog =====================================

		final OvariesProjectionDialog interactiveDoGDialog = new OvariesProjectionDialog( viewerFrame, viewer, setupAssignments );

		// =============== install shortcut B for dialog ==================

		final Actions actions = new Actions( new InputTriggerConfig() );
		actions.install( viewerFrame.getKeybindings(), "ovaries" );
		actions.namedAction( new ToggleDialogAction( "ovaries projection", interactiveDoGDialog ), "B" );
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );

//		final String fn = "/Users/pietzsch/Desktop/HisYFP-SPIM/dataset.xml";
		final String fn = "/Users/pietzsch/Desktop/data/Ivana/H5data2/15_Sqh/dataset.xml";

		try
		{
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			new OvariesProjectionExample( fn );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
