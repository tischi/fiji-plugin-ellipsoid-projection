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
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import bdv.tools.boundingbox.BoundingBoxUtil;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;

public class ProjectionTab extends JPanel
{
	private final ViewerPanel viewer;
	private final SetupAssignments setupAssignments;
	private final OvariesProjectionDialog dialog;
	private final RealARGBColorConverterSetup bandConverterSetup;
	private final SourceAndConverter< UnsignedShortType > bandSourceAndConverter;

	private double minProjectDistance;
	private double maxProjectDistance;
	private double sliceDistance;
	private int minTimepoint;
	private int maxTimepoint;
	private int projectWidth;
	private int projectHeight;
	private boolean flipZ;
	private boolean alignY;
	private boolean isMethodCylindrical;

	public ProjectionTab(
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final int bandSetupId,
			final Data data,
			final OvariesProjectionDialog ovariesProjectionDialog )
	{
		super( new BorderLayout() );

		this.viewer = viewer;
		this.setupAssignments = setupAssignments;
		this.dialog = ovariesProjectionDialog;

		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

		final Interval rangeInterval = BoundingBoxUtil.getSourcesBoundingBox( viewer.getState() );
		final EllipsoidDistanceSource bandSource = new EllipsoidDistanceSource( "distance", rangeInterval, data );

		minProjectDistance = 5.0;
		bandSource.setMinProjectionDistance( minProjectDistance );
		final JSpinner minProjectSpinner = new JSpinner( new SpinnerNumberModel( minProjectDistance, -100.0, 1000.0, 1.0 ) );
		minProjectSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				minProjectDistance = ( ( Double ) minProjectSpinner.getValue() ).doubleValue();
				bandSource.setMinProjectionDistance( minProjectDistance );
				viewer.requestRepaint();
			}
		} );
		final JPanel minProjectSpinnerPanel = new JPanel();
		minProjectSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		minProjectSpinnerPanel.add( new JLabel( "min projection distance" ), BorderLayout.LINE_START );
		minProjectSpinnerPanel.add( minProjectSpinner, BorderLayout.CENTER );
		panel.add( minProjectSpinnerPanel );

		maxProjectDistance = 50.0;
		bandSource.setMaxProjectionDistance( maxProjectDistance );
		final JSpinner maxProjectSpinner = new JSpinner( new SpinnerNumberModel( maxProjectDistance, -100.0, 1000.0, 1.0 ) );
		maxProjectSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				maxProjectDistance = ( ( Double ) maxProjectSpinner.getValue() ).doubleValue();
				bandSource.setMaxProjectionDistance( maxProjectDistance );
				viewer.requestRepaint();
			}
		} );
		final JPanel maxProjectSpinnerPanel = new JPanel();
		maxProjectSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		maxProjectSpinnerPanel.add( new JLabel( "max projection distance" ), BorderLayout.LINE_START );
		maxProjectSpinnerPanel.add( maxProjectSpinner, BorderLayout.CENTER );
		panel.add( maxProjectSpinnerPanel );

		sliceDistance = 1.0;
		final JSpinner sliceDistanceSpinner = new JSpinner( new SpinnerNumberModel( sliceDistance, 0.1, 5.0, 0.1 ) );
		sliceDistanceSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				sliceDistance = ( ( Double ) sliceDistanceSpinner.getValue() ).doubleValue();
			}
		} );
		final JPanel sliceDistanceSpinnerPanel = new JPanel();
		sliceDistanceSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		sliceDistanceSpinnerPanel.add( new JLabel( "slice distance" ), BorderLayout.LINE_START );
		sliceDistanceSpinnerPanel.add( sliceDistanceSpinner, BorderLayout.CENTER );
		panel.add( sliceDistanceSpinnerPanel );

		projectWidth = 800;
		final JSpinner projectWidthSpinner = new JSpinner( new SpinnerNumberModel( projectWidth, 10, 10000, 1 ) );
		projectWidthSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				projectWidth = ( ( Integer ) projectWidthSpinner.getValue() ).intValue();
			}
		} );
		final JPanel projectWidthSpinnerPanel = new JPanel();
		projectWidthSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		projectWidthSpinnerPanel.add( new JLabel( "output width" ), BorderLayout.LINE_START );
		projectWidthSpinnerPanel.add( projectWidthSpinner, BorderLayout.CENTER );
		panel.add( projectWidthSpinnerPanel );

		projectHeight = 400;
		final JSpinner projectHeightSpinner = new JSpinner( new SpinnerNumberModel( projectHeight, 10, 10000, 1 ) );
		projectHeightSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				projectHeight = ( ( Integer ) projectHeightSpinner.getValue() ).intValue();
			}
		} );
		final JPanel projectHeightSpinnerPanel = new JPanel();
		projectHeightSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		projectHeightSpinnerPanel.add( new JLabel( "output height" ), BorderLayout.LINE_START );
		projectHeightSpinnerPanel.add( projectHeightSpinner, BorderLayout.CENTER );
		panel.add( projectHeightSpinnerPanel );

		final int numTimepoints = viewer.getState().getNumTimepoints();
		minTimepoint = 0;
		final JSpinner minTimepointSpinner = new JSpinner( new SpinnerNumberModel( minTimepoint, 0, numTimepoints - 1, 1 ) );
		minTimepointSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				minTimepoint = ( ( Integer ) minTimepointSpinner.getValue() ).intValue();
			}
		} );
		final JPanel minTimepointSpinnerPanel = new JPanel();
		minTimepointSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		minTimepointSpinnerPanel.add( new JLabel( "from timepoint" ), BorderLayout.LINE_START );
		minTimepointSpinnerPanel.add( minTimepointSpinner, BorderLayout.CENTER );
		panel.add( minTimepointSpinnerPanel );

		maxTimepoint = 0;
		final JSpinner maxTimepointSpinner = new JSpinner( new SpinnerNumberModel( maxTimepoint, 0, numTimepoints - 1, 1 ) );
		maxTimepointSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				maxTimepoint = ( ( Integer ) maxTimepointSpinner.getValue() ).intValue();
			}
		} );
		final JPanel maxTimepointSpinnerPanel = new JPanel();
		maxTimepointSpinnerPanel.setLayout( new BorderLayout( 10, 10 ) );
		maxTimepointSpinnerPanel.add( new JLabel( "to timepoint" ), BorderLayout.LINE_START );
		maxTimepointSpinnerPanel.add( maxTimepointSpinner, BorderLayout.CENTER );
		panel.add( maxTimepointSpinnerPanel );

		isMethodCylindrical = true;
		final JComboBox projectionMethodComboBox = new JComboBox();
		final String CYLINDRICAL = "cylindrical projection";
		final String SPHERICAL = "spherical projection";
		projectionMethodComboBox.addItem( CYLINDRICAL );
		projectionMethodComboBox.addItem( SPHERICAL );
		projectionMethodComboBox.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( final ItemEvent ie )
			{
				isMethodCylindrical = CYLINDRICAL.equals( ie.getItem() );
			}
		} );
		panel.add( projectionMethodComboBox );

		flipZ = false;
		final JCheckBox flipZCheckBox = new JCheckBox( "flip Z" );
		flipZCheckBox.setSelected( flipZ );
		flipZCheckBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				flipZ = flipZCheckBox.isSelected();
			}
		} );
		panel.add( flipZCheckBox );

		alignY = false;
		final JCheckBox alignYCheckBox = new JCheckBox( "align Y" );
		alignYCheckBox.setSelected( alignY );
		alignYCheckBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				alignY = alignYCheckBox.isSelected();
			}
		} );
		panel.add( alignYCheckBox );

		final JButton bcomputeButton = new JButton( "compute" );
		bcomputeButton.addActionListener( new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				if ( maxTimepoint < minTimepoint )
					maxTimepoint = minTimepoint;
				if ( maxProjectDistance < minProjectDistance )
					maxProjectDistance = minProjectDistance;
				data.project( projectWidth, projectHeight, minProjectDistance, maxProjectDistance, sliceDistance, minTimepoint, maxTimepoint, flipZ, alignY, isMethodCylindrical );
			}
		} );

		add( panel, BorderLayout.NORTH );
		add( bcomputeButton, BorderLayout.SOUTH );

		final RealARGBColorConverter< UnsignedShortType > converter = new RealARGBColorConverter.Imp1< UnsignedShortType >( 0, 5000 );
		converter.setColor( new ARGBType( Color.magenta.getRGB() ) );
		bandSourceAndConverter = new SourceAndConverter< UnsignedShortType >( bandSource, converter );

		bandConverterSetup = new RealARGBColorConverterSetup( bandSetupId, converter );
		bandConverterSetup.setViewer( viewer );
	}

	void addOverlays()
	{
		viewer.addSource( bandSourceAndConverter );
		setupAssignments.addSetup( bandConverterSetup );
	}

	void removeOverlays()
	{
		viewer.removeSource( bandSourceAndConverter.getSpimSource() );
		setupAssignments.removeSetup( bandConverterSetup );
	}
}
