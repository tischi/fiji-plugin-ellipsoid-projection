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

import bdv.tools.boundingbox.BoxSelectionPanel.Box;
import bdv.util.ModifiableInterval;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import bdv.tools.boundingbox.BoundingBoxOverlay;
import bdv.tools.boundingbox.BoundingBoxOverlay.BoundingBoxOverlaySource;
import bdv.tools.boundingbox.BoundingBoxUtil;
import bdv.tools.boundingbox.BoxRealRandomAccessible;
import bdv.tools.boundingbox.BoxSelectionPanel;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;

public class BoundingBoxTab extends JPanel
{
	private final ModifiableInterval interval;
	private final BoxRealRandomAccessible< UnsignedShortType > boxRealRandomAccessible;
	private final ViewerPanel viewer;
	private final SetupAssignments setupAssignments;
	private final SourceAndConverter< UnsignedShortType > boxSourceAndConverter;
	private final BoundingBoxOverlay boxOverlay;
	private final RealARGBColorConverterSetup boxConverterSetup;
	private final OvariesProjectionDialog dialog;

	private int boxSourceIndex;

	public BoundingBoxTab(
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final int boxSetupId,
			final Data data,
			final OvariesProjectionDialog ovariesProjectionDialog )
	{
		super( new BorderLayout() );

		this.viewer = viewer;
		this.setupAssignments = setupAssignments;
		this.dialog = ovariesProjectionDialog;

		final RealInterval globalRangeInterval = BoundingBoxUtil.getSourcesBoundingBoxReal( viewer.getState() );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		viewer.getState().getSources().get( 0 ).getSpimSource().getSourceTransform( 0, 0, sourceTransform );
		final Interval rangeInterval = Intervals.smallestContainingInterval( BoundingBoxUtil.transformBoundingBoxReal( globalRangeInterval, sourceTransform.inverse() ) );
//		final Interval rangeInterval = BoundingBoxUtil.getSourcesBoundingBox( viewer.getState() );
		final long[] min = new long[ 3 ];
		final long[] max = new long[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			min[ d ] = rangeInterval.min( d ) + rangeInterval.dimension( d ) / 4;
			max[ d ] = rangeInterval.max( d ) - rangeInterval.dimension( d ) / 4;
		}
		final Interval initialInterval = new FinalInterval( min, max );

		// create a procedural RealRandomAccessible that will render the bounding box
		final UnsignedShortType insideValue = new UnsignedShortType( 1000 ); // inside the box pixel value is 1000
		final UnsignedShortType outsideValue = new UnsignedShortType( 0 ); // outside is 0

		interval = new ModifiableInterval( initialInterval );
		boxRealRandomAccessible = new BoxRealRandomAccessible<>( interval, insideValue, outsideValue );

		// create a bdv.viewer.Source providing data from the bbox RealRandomAccessible
		final RealRandomAccessibleSource< UnsignedShortType > boxSource = new RealRandomAccessibleSource< UnsignedShortType >(
				boxRealRandomAccessible, new UnsignedShortType(), "selection" )
		{
			@Override
			public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
			{
				transform.set( sourceTransform );
			}

			@Override
			public Interval getInterval( final int t, final int level )
			{
				return interval;
			}
		};

		// set up a converter from the source type (UnsignedShortType in this case) to ARGBType
		final RealARGBColorConverter< UnsignedShortType > converter = new RealARGBColorConverter.Imp1< UnsignedShortType >( 0, 5000 );
		converter.setColor( new ARGBType( 0x00994499 ) ); // set bounding box color to magenta

		boxConverterSetup = new RealARGBColorConverterSetup( boxSetupId, converter );
		boxConverterSetup.setViewer( viewer );

		// create a SourceAndConverter (can be added to the viewer for display)
		final TransformedSource< UnsignedShortType > ts = new TransformedSource< UnsignedShortType >( boxSource );
		boxSourceAndConverter = new SourceAndConverter< UnsignedShortType >( ts, converter );

		boxOverlay = new BoundingBoxOverlay( new BoundingBoxOverlaySource()
		{
			@Override
			public void getIntervalTransform( final AffineTransform3D transform )
			{
				ts.getSourceTransform( 0, 0, transform );
			}

			@Override
			public Interval getInterval()
			{
				return interval;
			}
		} )
		{
			@Override
			public void drawOverlays( final Graphics g )
			{
				if ( viewer.getVisibilityAndGrouping().isSourceVisible( boxSourceIndex ) )
					super.drawOverlays( g );
			}
		};

		// create a JPanel with sliders to modify the bounding box interval (boxRealRandomAccessible.getInterval())
		final BoxSelectionPanel boxSelectionPanel = new BoxSelectionPanel(
				new Box()
				{
					@Override
					public void setInterval( final Interval i )
					{
						interval.set( i );
						viewer.requestRepaint();
					}

					@Override
					public Interval getInterval()
					{
						return interval;
					}
				},
				rangeInterval );

		final JButton setButton = new JButton( "set" );
		setButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final AffineTransform3D transform = new AffineTransform3D();
				ts.getSourceTransform( 0, 0, transform );
				data.setBoundingBox( interval, transform );
				dialog.bboxDone();
			}
		} );

		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				final VisibilityAndGrouping vg = viewer.getVisibilityAndGrouping();
				if ( vg.getDisplayMode() != DisplayMode.FUSED )
				{
					for ( int i = 0; i < boxSourceIndex; ++i )
						vg.setSourceActive( i, vg.isSourceVisible( i ) );
					vg.setDisplayMode( DisplayMode.FUSED );
				}
				vg.setSourceActive( boxSourceIndex, true );
				vg.setCurrentSource( boxSourceIndex );
			}
		} );

		add( boxSelectionPanel, BorderLayout.NORTH );
		add( setButton, BorderLayout.SOUTH );
	}

	void addOverlays()
	{
		viewer.addSource( boxSourceAndConverter );
		boxSourceIndex = viewer.getState().numSources() - 1;
		setupAssignments.addSetup( boxConverterSetup );
		viewer.getDisplay().addOverlayRenderer( boxOverlay );
		viewer.addRenderTransformListener( boxOverlay );
	}

	void removeOverlays()
	{
		viewer.removeSource( boxSourceAndConverter.getSpimSource() );
		setupAssignments.removeSetup( boxConverterSetup );
		viewer.getDisplay().removeOverlayRenderer( boxOverlay );
		viewer.removeTransformListener( boxOverlay );
	}
}
