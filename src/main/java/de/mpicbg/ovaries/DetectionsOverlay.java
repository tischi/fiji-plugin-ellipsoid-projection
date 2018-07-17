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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.ViewerState;

public class DetectionsOverlay implements OverlayRenderer, TransformListener< AffineTransform3D >
{
	private final AffineTransform3D transform;

	private final ViewerPanel viewer;

	private int detectionsSourceIndex;

	private List< ? extends RealLocalizable > detections;

	private Color col;

	private final ConverterSetup converterSetup;

	public DetectionsOverlay( final ViewerPanel viewer, final ConverterSetup detectionsConverterSetup )
	{
		this.viewer = viewer;
		this.converterSetup = detectionsConverterSetup;
		this.transform = new AffineTransform3D();
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		if ( detections == null )
			return;

		boolean show = false;
		final ViewerState state = viewer.getState();
		switch ( state.getDisplayMode() )
		{
		case SINGLE:
			show = ( detectionsSourceIndex == state.getCurrentSource() );
			break;
		case GROUP:
			show = state.getSourceGroups().get( state.getCurrentGroup() ).getSourceIds().contains( detectionsSourceIndex );
			break;
		case FUSED:
			show = state.getSources().get( detectionsSourceIndex ).isActive();
			break;
		case FUSEDGROUP:
		default:
			for ( final SourceGroup group : state.getSourceGroups() )
				if ( group.isActive() && group.getSourceIds().contains( detectionsSourceIndex ) )
					show = true;
		}
		if ( !show )
			return;

		col = new Color( converterSetup.getColor().get() );

		final Graphics2D graphics = ( Graphics2D ) g;
		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		for ( final RealLocalizable p : detections )
		{
			p.localize( lPos );
			transform.apply( lPos, gPos );
			final double size = getPointSize( gPos );
			final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
			final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
			final int w = ( int ) size;
			graphics.setColor( getColor( gPos ) );
			graphics.fillOval( x, y, w, w );
		}
	}

	/** screen pixels [x,y,z] **/
	private Color getColor( final double[] gPos )
	{
		int alpha = 255 - ( int ) Math.round( Math.abs( gPos[ 2 ] ) );

		if ( alpha < 64 )
			alpha = 64;

		return new Color( col.getRed(), col.getGreen(), col.getBlue(), alpha );
	}

	private double getPointSize( final double[] gPos )
	{
		if ( Math.abs( gPos[ 2 ] ) < 3 )
			return 5.0;
		else
			return 3.0;
	}

	@Override
	public void transformChanged( final AffineTransform3D t )
	{
		transform.set( t );
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{}

	public < P extends RealLocalizable > void setDetections( final List< P > detections )
	{
		this.detections = detections;
	}

	public void setDetectionsSourceIndex( final int detectionsSourceIndex )
	{
		this.detectionsSourceIndex = detectionsSourceIndex;
	}
}
