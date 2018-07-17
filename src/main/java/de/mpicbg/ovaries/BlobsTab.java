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

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

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

public class BlobsTab extends JPanel
{
	private final ViewerPanel viewer;
	private final SetupAssignments setupAssignments;
	private final OvariesProjectionDialog dialog;
	private final DetectionsOverlay detectionsOverlay;
	private int detectionsSourceIndex;
	private final RealARGBColorConverterSetup detectionsConverterSetup;
	private final SourceAndConverter< UnsignedShortType > detectionsSourceAndConverter;

	private double sigma;
	private double minPeakValue;

	public BlobsTab(
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final int detectionsSetupId,
			final Data data,
			final OvariesProjectionDialog ovariesProjectionDialog )
	{
		super( new BorderLayout() );

		this.viewer = viewer;
		this.setupAssignments = setupAssignments;
		this.dialog = ovariesProjectionDialog;

		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		sigma = 3;
		final JSpinner sigmaSpinner = new JSpinner( new SpinnerNumberModel( sigma, 0.01, 30, 0.01 ) );
		sigmaSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				sigma = ( ( Double ) sigmaSpinner.getValue() ).doubleValue();
			}
		} );
		final JPanel sigmaSpinnerPanel = new JPanel();
		sigmaSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		sigmaSpinnerPanel.add( new JLabel( "sigma" ), BorderLayout.LINE_START );
		sigmaSpinnerPanel.add( sigmaSpinner, BorderLayout.CENTER );
		panel.add( sigmaSpinnerPanel );
		minPeakValue = 250;
		final JSpinner minPeakSpinner = new JSpinner( new SpinnerNumberModel( minPeakValue, 0.0, 2000.0, 1.0 ) );
		minPeakSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				minPeakValue = ( ( Double ) minPeakSpinner.getValue() ).doubleValue();
			}
		} );
		final JPanel minPeakSpinnerPanel = new JPanel();
		minPeakSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		minPeakSpinnerPanel.add( new JLabel( "min peak value" ), BorderLayout.LINE_START );
		minPeakSpinnerPanel.add( minPeakSpinner, BorderLayout.CENTER );
		panel.add( minPeakSpinnerPanel );

		final Source< UnsignedShortType > detectionsFakeSource = new RealRandomAccessibleSource< UnsignedShortType >( null, new UnsignedShortType(), "detections", null )
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
		converter.setColor( new ARGBType( Color.green.getRGB() ) );
		detectionsSourceAndConverter = new SourceAndConverter< UnsignedShortType >( detectionsFakeSource, converter );

		detectionsConverterSetup = new RealARGBColorConverterSetup( detectionsSetupId, converter );
		detectionsConverterSetup.setViewer( viewer );

		detectionsOverlay = new DetectionsOverlay( viewer, detectionsConverterSetup );

		final JButton bcomputeButton = new JButton( "compute" );
		bcomputeButton.addActionListener( new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final TDoubleArrayList sigmas = new TDoubleArrayList();
				sigmas.add( sigma );
				final TDoubleArrayList minPeakValues = new TDoubleArrayList();
				minPeakValues.add( minPeakValue );
				final TIntArrayList timepoints = new TIntArrayList();
				timepoints.add( 0 );

				data.computeDetections( sigmas, minPeakValues, timepoints );
				detectionsOverlay.setDetections( data.getDetections() );
				dialog.blobsDone();
			}
		} );

		add( panel, BorderLayout.NORTH );
		add( bcomputeButton, BorderLayout.SOUTH );
	}

	void addOverlays()
	{
		viewer.addSource( detectionsSourceAndConverter );
		detectionsSourceIndex = viewer.getState().numSources() - 1;
		detectionsOverlay.setDetectionsSourceIndex( detectionsSourceIndex );
		setupAssignments.addSetup( detectionsConverterSetup );
		viewer.getDisplay().addOverlayRenderer( detectionsOverlay );
		viewer.addRenderTransformListener( detectionsOverlay );
	}

	void removeOverlays()
	{
		viewer.removeSource( detectionsSourceAndConverter.getSpimSource() );
		setupAssignments.removeSetup( detectionsConverterSetup );
		viewer.getDisplay().removeOverlayRenderer( detectionsOverlay );
		viewer.removeTransformListener( detectionsOverlay );
	}
}
