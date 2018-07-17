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

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.mpicbg.ovaries.ellipsoid.Ellipsoid;
import de.mpicbg.ovaries.ellipsoid.HyperEllipsoid;
import ij.IJ;
import ij.ImagePlus;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccess;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.SphericalToCartesianTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class SphericalProjection
{
	private final Ellipsoid ellipsoid;

	private final int width;

	private final int height;

	private final Source< UnsignedShortType > source;

	private final double[][] cylAxes;

	private final double minProjectDistance;

	private final double maxProjectDistance;

	private final double sliceDistance;

	private final int minTimepoint;

	private final int maxTimepoint;

	public SphericalProjection(
			final Ellipsoid ellipsoid,
			final int width,
			final int height,
			final double minProjectDistance,
			final double maxProjectDistance,
			final double sliceDistance,
			final int minTimepoint,
			final int maxTimepoint,
			final boolean flipZ,
			final boolean alignY,
			final Source< UnsignedShortType > source )
	{
		this.ellipsoid = ellipsoid;
		this.width = width;
		this.height = height;
		this.minProjectDistance = minProjectDistance;
		this.maxProjectDistance = maxProjectDistance;
		this.sliceDistance = sliceDistance;
		this.minTimepoint = minTimepoint;
		this.maxTimepoint = maxTimepoint;
		this.source = source;
		this.cylAxes = getCylinderAxes( ellipsoid, flipZ, alignY );
	}

	public static double[][] getCylinderAxes( final Ellipsoid ellipsoid, final boolean flipZ, final boolean alignY )
	{
		final double[] r = ellipsoid.getRadii();
		final double[] L;
		if ( alignY )
			L = new double[] { 0, -1, 0 };
		else
		{
			final int biggestRadiusIndex = ( r[ 0 ] > r[ 1 ] ) ?
					( r[ 0 ] > r[ 2 ] ? 0 : 2 ) :
					( r[ 1 ] > r[ 2 ] ? 1 : 2 );
			L = ellipsoid.getAxes()[ biggestRadiusIndex ].clone();
			if ( L[ 1 ] > 0 )
				LinAlgHelpers.scale( L, -1, L );
		}

		final double[] Z = new double[] { 0, 0, 1 };
		final double[] A = new double[ 3 ];
		final double[] tmp = new double[ 3 ];
		LinAlgHelpers.cross( Z, L, tmp );
		LinAlgHelpers.cross( L, tmp, A );
		if ( flipZ )
		{
			if ( A[ 2 ] < 0 )
				LinAlgHelpers.scale( A, -1, A );
		}
		else
		{
			if ( A[ 2 ] > 0 )
				LinAlgHelpers.scale( A, -1, A );
		}

		LinAlgHelpers.cross( L, A, Z );

		return new double[][] { A, Z, L };
	}

	public ImagePlus project()
	{
		final StringBuilder sb = new StringBuilder("\n\nSphericalProjection\n");
		sb.append( "Ellipsoid = " + ellipsoid + "\n");
		sb.append( "cylAxes = " + LinAlgHelpers.toString( cylAxes ) + "\n");
		sb.append( "width = " + width + "\n");
		sb.append( "height = " + height + "\n");
		sb.append( "minProjectDistance = " + minProjectDistance + "\n");
		sb.append( "maxProjectDistance = " + maxProjectDistance + "\n");
		sb.append( "sliceDistance = " + sliceDistance + "\n");
		sb.append( "minTimepoint = " + minTimepoint + "\n");
		sb.append( "maxTimepoint = " + maxTimepoint + "\n\n\n");
		IJ.log( sb.toString() );
		ImagePlus imp = null;
		try
		{
			final int depth = ( int ) Math.ceil( ( maxProjectDistance - minProjectDistance ) / sliceDistance ) + 1;
			final int numTimepoints = maxTimepoint - minTimepoint + 1;
			final FloatImagePlus< FloatType > floats = ImagePlusImgs.floats( width, height, depth, numTimepoints );
			imp = floats.getImagePlus();
			imp.setDimensions( 1, depth, numTimepoints );
			imp.show();

			for ( int t = 0; t < numTimepoints; ++t )
			{
				for ( int z = 0; z < depth; ++z )
				{
					final double distance = minProjectDistance + z * sliceDistance;
					final IntervalView< FloatType > slice = Views.hyperSlice( Views.hyperSlice( floats, 3, t ), 2, z );
					projectPlane( distance, t + minTimepoint, slice );
				}
			}
		}
		catch ( final ImgLibException e )
		{}
		return imp;
	}

	public void projectPlane( final double distance, final int t, final RandomAccessible< FloatType > plane )
	{
		final RandomAccess< FloatType > out = plane.randomAccess();
		final AffineTransform3D transform = new AffineTransform3D();
		source.getSourceTransform( t, 0, transform );
		final RealRandomAccess< UnsignedShortType > in = RealViews.affineReal( source.getInterpolatedSource( t, 0, Interpolation.NLINEAR ), transform ).realRandomAccess();

		final double[] spherical = new double[ 3 ];
		final double[] unit = new double[ 3 ];
		final double[] cyl = new double[ 3 ];
		// point on ellipsoid
		final double[] pe = new double[ 3 ];
		// unit normal at pe
		final double[] ne = new double[ 3 ];

		final long t0 = System.currentTimeMillis();
		spherical[ 0 ] = 1.0; // radius
		for ( int ypi = 0; ypi < height; ++ypi )
		{
			out.setPosition( ypi, 1 );
			spherical[ 1 ] = ypi * Math.PI / height; // inclination
			for ( int xpi = 0; xpi < width; ++xpi )
			{
				out.setPosition( xpi, 0 );
				spherical[ 2 ] = xpi * 2 * Math.PI / width; // azimuth
				SphericalToCartesianTransform3D.getInstance().apply( spherical, cyl );
				LinAlgHelpers.multT( cylAxes, cyl, unit );
				LinAlgHelpers.normalize( unit );

				LinAlgHelpers.mult( ellipsoid.getPrecision(), unit, ne );
				LinAlgHelpers.scale( unit, Math.sqrt( 1.0 / LinAlgHelpers.dot( unit, ne ) ), pe );
				LinAlgHelpers.mult( ellipsoid.getPrecision(), pe, ne );
				LinAlgHelpers.normalize( ne );
				LinAlgHelpers.add( pe, ellipsoid.getCenter(), pe );

				LinAlgHelpers.scale( ne, distance, ne );
				LinAlgHelpers.add( pe, ne, pe );
				in.setPosition( pe );
				final double v = in.get().getRealDouble();
				out.get().setReal( v );
			}
		}
		final long t1 = System.currentTimeMillis();
		System.out.println( (t1 - t0) + " ms" );
	}

	protected static void pointInDirection( final HyperEllipsoid ellipsoid, final double[] unit, final double[] point )
	{
		final double[] x = new double[ ellipsoid.numDimensions() ];
		LinAlgHelpers.mult( ellipsoid.getPrecision(), unit, x );
		LinAlgHelpers.scale( unit, 1.0 / LinAlgHelpers.dot( unit, x ), point );
		LinAlgHelpers.add( point, ellipsoid.getCenter(), point );
	}
}
