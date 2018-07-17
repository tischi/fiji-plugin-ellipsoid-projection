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
import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.Interval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;

public class FitEllipsoidTab extends JPanel
{
	private final ViewerPanel viewer;
	private final SetupAssignments setupAssignments;
	private final OvariesProjectionDialog dialog;
	private final EllipsoidOverlay ellipsoidOverlay;
	private int ellipsoidSourceIndex;
	private final RealARGBColorConverterSetup ellipsoidConverterSetup;
	private final SourceAndConverter< UnsignedShortType > ellipsoidSourceAndConverter;

	private int numRandomSamples;
	private double outsideCutoffDistance;
	private double insideCutoffDistance;

	public FitEllipsoidTab(
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final int ellipsoidSetupId,
			final Data data,
			final OvariesProjectionDialog ovariesProjectionDialog )
	{
		super( new BorderLayout() );

		this.viewer = viewer;
		this.setupAssignments = setupAssignments;
		this.dialog = ovariesProjectionDialog;

		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

		numRandomSamples = 10000;
		final JSpinner numSamplesSpinner = new JSpinner( new SpinnerNumberModel( numRandomSamples, 100.0, 10000000.0, 1.0 ) );
		numSamplesSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				numRandomSamples = ( int ) ( ( Double ) numSamplesSpinner.getValue() ).doubleValue();
			}
		} );
		final JPanel numSamplesSpinnerPanel = new JPanel();
		numSamplesSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		numSamplesSpinnerPanel.add( new JLabel( "random samples" ), BorderLayout.LINE_START );
		numSamplesSpinnerPanel.add( numSamplesSpinner, BorderLayout.CENTER );
		panel.add( numSamplesSpinnerPanel );

		outsideCutoffDistance = 10.0;
		final JSpinner ousideCutoffSpinner = new JSpinner( new SpinnerNumberModel( outsideCutoffDistance, 1.0, 1000.0, 1.0 ) );
		ousideCutoffSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				outsideCutoffDistance = ( ( Double ) ousideCutoffSpinner.getValue() ).doubleValue();
			}
		} );
		final JPanel ousideCutoffSpinnerPanel = new JPanel();
		ousideCutoffSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		ousideCutoffSpinnerPanel.add( new JLabel( "outside cutoff distance" ), BorderLayout.LINE_START );
		ousideCutoffSpinnerPanel.add( ousideCutoffSpinner, BorderLayout.CENTER );
		panel.add( ousideCutoffSpinnerPanel );

		insideCutoffDistance = 10.0;
		final JSpinner insideCutoffSpinner = new JSpinner( new SpinnerNumberModel( insideCutoffDistance, 1.0, 1000.0, 1.0 ) );
		insideCutoffSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				insideCutoffDistance = ( ( Double ) insideCutoffSpinner.getValue() ).doubleValue();
			}
		} );
		final JPanel insideCutoffSpinnerPanel = new JPanel();
		insideCutoffSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		insideCutoffSpinnerPanel.add( new JLabel( "inside cutoff distance" ), BorderLayout.LINE_START );
		insideCutoffSpinnerPanel.add( insideCutoffSpinner, BorderLayout.CENTER );
		panel.add( insideCutoffSpinnerPanel );

		final JButton bcomputeButton = new JButton( "compute" );
		bcomputeButton.addActionListener( new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				data.fitEllipsoid( numRandomSamples, outsideCutoffDistance, insideCutoffDistance );
				ellipsoidOverlay.setEllipsoid( data.getEllipsoid() );
				dialog.fitEllipsoidDone();
			}
		} );

		add( panel, BorderLayout.NORTH );
		add( bcomputeButton, BorderLayout.SOUTH );

		final Source< UnsignedShortType > ellipsoidFakeSource = new RealRandomAccessibleSource< UnsignedShortType >( null, new UnsignedShortType(), "ellipsoid", null )
		{
			@Override
			public boolean isPresent( final int t )
			{
				return false;
			}

			@Override
			public Interval getInterval( final int t, final int level )
			{
				return null;
			}
		};

		final RealARGBColorConverter< UnsignedShortType > converter = new RealARGBColorConverter.Imp1< UnsignedShortType >( 0, 5000 );
		converter.setColor( new ARGBType( Color.cyan.getRGB() ) );
		ellipsoidSourceAndConverter = new SourceAndConverter< UnsignedShortType >( ellipsoidFakeSource, converter );

		ellipsoidConverterSetup = new RealARGBColorConverterSetup( ellipsoidSetupId, converter );
		ellipsoidConverterSetup.setViewer( viewer );

		ellipsoidOverlay = new EllipsoidOverlay( viewer, ellipsoidConverterSetup );
	}

	void addOverlays()
	{
		viewer.addSource( ellipsoidSourceAndConverter );
		ellipsoidSourceIndex = viewer.getState().numSources() - 1;
		ellipsoidOverlay.setEllipsoidSourceIndex( ellipsoidSourceIndex );
		setupAssignments.addSetup( ellipsoidConverterSetup );
		viewer.getDisplay().addOverlayRenderer( ellipsoidOverlay );
		viewer.addRenderTransformListener( ellipsoidOverlay );
	}

	void removeOverlays()
	{
		viewer.removeSource( ellipsoidSourceAndConverter.getSpimSource() );
		setupAssignments.removeSetup( ellipsoidConverterSetup );
		viewer.getDisplay().removeOverlayRenderer( ellipsoidOverlay );
		viewer.removeTransformListener( ellipsoidOverlay );
	}
}
