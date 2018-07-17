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

import java.util.ArrayList;
import java.util.List;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.dog.DogDetection.ExtremaType;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import bdv.util.Affine3DHelpers;
import bdv.util.IntervalBoundingBox;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.SourceState;
import de.mpicbg.ovaries.ellipsoid.Ellipsoid;
import de.mpicbg.ovaries.ellipsoid.SampleEllipsoids;

public class Data
{
	private int dataSourceIndex;

	private final ArrayList< Integer > projectionSourceIndices;

	private final ViewerPanel viewer;

	private FinalInterval bbInterval;

	private final AffineTransform3D bbTransform;

	private ArrayList< RealPoint > detections;

	private Ellipsoid ellipsoid;

	public Data( final ViewerPanel viewer )
	{
		this.viewer = viewer;
		bbTransform = new AffineTransform3D();

		projectionSourceIndices = new ArrayList< Integer >();
		final List< SourceState< ? > > sources = viewer.getState().getSources();
		for ( int i = 0; i < sources.size(); ++i )
		{
			final Source< ? > s = sources.get( i ).getSpimSource();
			if ( UnsignedShortType.class.isInstance( s.getType() ) )
				projectionSourceIndices.add( i );
		}

		dataSourceIndex =  projectionSourceIndices.get( 0 );
	}

	public void setImageDataSourceIndex( final int index )
	{
		dataSourceIndex = index;
	}

	public void setBoundingBox(
			final Interval interval,
			final AffineTransform3D transform )
	{
		System.out.println( "Data.setBoundingBox" );

		bbInterval = new FinalInterval( interval );
		bbTransform.set( transform );
	}

	public FinalInterval getBoundingBox()
	{
		return bbInterval;
	}

	public void computeDetections(
			final TDoubleArrayList sigmas,
			final TDoubleArrayList minPeakValues,
			final TIntArrayList timepoints )
	{
		System.out.println( "Data.computeDetections" );

		detections = new ArrayList< RealPoint >();
		for ( int ti = 0; ti < timepoints.size(); ++ti )
		{
			final int timepoint = timepoints.get( ti );
			for ( int si = 0; si < sigmas.size(); ++si )
			{
				final double sigma = sigmas.get( si );
				for ( int mi = 0; mi < minPeakValues.size(); ++mi )
				{
					final double minPeakValue = minPeakValues.get( mi );

					System.out.println( "timepoint = " + timepoint );
					System.out.println( "sigma = " + sigma );
					System.out.println( "minPeakValue = " + minPeakValue );

					computeDoGDetections( sigma, minPeakValue, timepoint, dataSourceIndex );
				}
			}
		}
	}

	public ArrayList< RealPoint > getDetections()
	{
		return detections;
	}

	public Ellipsoid getEllipsoid()
	{
		return ellipsoid;
	}

	@SuppressWarnings( "unchecked" )
	private Source< UnsignedShortType > getDoGSource( final int sourceIndex )
	{
		final Source< ? > s = viewer.getState().getSources().get( sourceIndex ).getSpimSource();
		if ( UnsignedShortType.class.isInstance( s.getType() ) )
			return ( Source< UnsignedShortType > ) s;
		else
		{
			System.err.println( "Error: Can only use UnsignedShortType sources for computing blobs" );
			return null;
		}
	}

	private void computeDoGDetections( final double sigma, final double minPeakValue, final int timepoint, final int sourceIndex )
	{
		final Source< UnsignedShortType > dogSource = getDoGSource( sourceIndex );
		if ( dogSource == null )
			return;

		// get interval
		final int n = 3;

		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		dogSource.getSourceTransform( timepoint, 0, sourceToGlobal );

		final ArrayList< RealPoint > sourceCorners = new ArrayList< RealPoint >();
		for ( final RealLocalizable corner : IntervalBoundingBox.getCorners( bbInterval ) )
		{
			final RealPoint sourceCorner = new RealPoint( n );
			bbTransform.apply( corner, sourceCorner );
			sourceToGlobal.applyInverse( sourceCorner, sourceCorner );
			sourceCorners.add( sourceCorner );
		}
		final RandomAccessibleInterval< UnsignedShortType > img = dogSource.getSource( timepoint, 0 );
		final Interval sourceInterval = Intervals.smallestContainingInterval( IntervalBoundingBox.getBoundingBox( sourceCorners ) );
		final Interval interval = Intervals.intersect( sourceInterval, img );

		// get detections
		final int stepsPerOctave = 4;
		final double k = Math.pow( 2.0, 1.0 / stepsPerOctave );
		final double sigma1 = sigma;
		final double sigma2 = k * sigma1;

		final double xs = Affine3DHelpers.extractScale( sourceToGlobal, 0 );
		final double ys = Affine3DHelpers.extractScale( sourceToGlobal, 1 );
		final double zs = Affine3DHelpers.extractScale( sourceToGlobal, 2 );
		final double[] pixelSize = new double[] { 1, ys / xs, zs / xs };

		final RealFloatConverter< UnsignedShortType > converter = new RealFloatConverter< UnsignedShortType >();
		final DogDetection< FloatType > DOG = new DogDetection< FloatType >(
				Views.extendMirrorSingle( Converters.convert( img, converter, new FloatType() ) ),
				interval,
				pixelSize,
				sigma1,
				sigma2,
				ExtremaType.MINIMA,
				minPeakValue,
				true );
		final ArrayList< RefinedPeak< Point > > refinedPeaksSource = DOG.getSubpixelPeaks();
		System.out.println( "found " + refinedPeaksSource.size() + " peaks" );

		for ( final RefinedPeak< Point > p : refinedPeaksSource )
		{
			final RealPoint sp = new RealPoint( 3 );
			sourceToGlobal.apply( p, sp );
			detections.add( sp );
		}
	}

	public void fitEllipsoid( final int numRandomSamples, final double outsideCutoffDistance, final double insideCutoffDistance )
	{
		ellipsoid = SampleEllipsoids.sample( detections, numRandomSamples, outsideCutoffDistance, insideCutoffDistance );
	}

	public void project(
			final int width,
			final int height,
			final double minProjectDistance,
			final double maxProjectDistance,
			final double sliceDistance,
			final int minTimepoint,
			final int maxTimepoint,
			final boolean flipZ,
			final boolean alignY,
			final boolean isMethodCylindrical )
	{
		if ( isMethodCylindrical )
		{
			for ( final int i : projectionSourceIndices )
			{
				final CylindricalProjection proj = new CylindricalProjection(
						ellipsoid, width, height, minProjectDistance, maxProjectDistance,
						sliceDistance, minTimepoint, maxTimepoint, flipZ, alignY,
						getDoGSource( i ) );
				proj.project();
			}
		}
		else
		{
			for ( final int i : projectionSourceIndices )
			{
				final SphericalProjection proj = new SphericalProjection(
						ellipsoid, width, height, minProjectDistance, maxProjectDistance,
						sliceDistance, minTimepoint, maxTimepoint, flipZ, alignY,
						getDoGSource( i ) );
				proj.project();
			}
		}
	}
}
