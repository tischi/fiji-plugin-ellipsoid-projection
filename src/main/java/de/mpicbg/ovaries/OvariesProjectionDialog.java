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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.ViewerPanel;

public class OvariesProjectionDialog extends JDialog
{
	private final ViewerPanel viewer;

	private final SetupAssignments setupAssignments;

	private final Data data;

	private final JTabbedPane tabs;

	private final BoundingBoxTab bboxTab;

	private final BlobsTab blobsTab;

	private final FitEllipsoidTab fitTab;

	private final ProjectionTab projTab;

	public OvariesProjectionDialog( final Frame owner, final ViewerPanel viewer, final SetupAssignments setupAssignments )
	{
		super( owner, "Ovaries Projection", false );
		this.viewer = viewer;
		this.setupAssignments = setupAssignments;

		data = new Data( viewer );

		bboxTab = new BoundingBoxTab( viewer, setupAssignments, 99, data, this );
		blobsTab = new BlobsTab( viewer, setupAssignments, 100, data, this );
		fitTab = new FitEllipsoidTab( viewer, setupAssignments, 101, data, this);
		projTab = new ProjectionTab( viewer, setupAssignments, 102, data, this );

		tabs = new JTabbedPane();
		tabs.addTab( "bounding box", bboxTab );
		tabs.addTab( "find blobs", blobsTab );
		tabs.addTab( "fit ellipsoid", fitTab );
		tabs.addTab( "projection", projTab );

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentHidden( final ComponentEvent e )
			{
				bboxTab.removeOverlays();
				blobsTab.removeOverlays();
				fitTab.removeOverlays();
				projTab.removeOverlays();
			}

			@Override
			public void componentShown( final ComponentEvent e )
			{
				bboxTab.addOverlays();
				blobsTab.addOverlays();
				fitTab.addOverlays();
				projTab.addOverlays();
			}
		} );

		final Container content = getContentPane();
		content.add( tabs, BorderLayout.CENTER );
		pack();
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
	}

	protected void bboxDone()
	{
		tabs.setSelectedComponent( blobsTab );
	}

	protected void blobsDone()
	{
		tabs.setSelectedComponent( fitTab );
	}

	protected void fitEllipsoidDone()
	{
		tabs.setSelectedComponent( projTab );
	}
}
