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

import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import bdv.util.RealRandomAccessibleSource;
import de.mpicbg.ovaries.ellipsoid.DistPointHyperEllipsoid;
import de.mpicbg.ovaries.ellipsoid.Ellipsoid;
import de.mpicbg.ovaries.ellipsoid.DistPointHyperEllipsoid.Result;

public class EllipsoidDistanceSource extends RealRandomAccessibleSource< UnsignedShortType >
{
	protected Interval interval;

	protected AffineTransform3D sourceTransform = new AffineTransform3D();

	public EllipsoidDistanceSource( final String name, final Interval interval, final Data data )
	{
		super( new EllipsoidDistanceRealRandomAccessible( data ), new UnsignedShortType(), name );
		this.interval = interval;
	}

	@Override
	public Interval getInterval( final int t, final int level )
	{
		return interval;
	}

	public void setMinProjectionDistance( final double minProjectionDistance )
	{
		( ( EllipsoidDistanceRealRandomAccessible ) accessible ).setMinProjectionDistance( minProjectionDistance );
	}

	public void setMaxProjectionDistance( final double maxProjectionDistance )
	{
		( ( EllipsoidDistanceRealRandomAccessible ) accessible ).setMaxProjectionDistance( maxProjectionDistance );
	}

	static class EllipsoidDistanceRealRandomAccessible implements RealRandomAccessible< UnsignedShortType >
	{
		private final int n = 3;

		private final Data data;

		private double minProjectionDistance;

		private double maxProjectionDistance;

		public EllipsoidDistanceRealRandomAccessible( final Data data )
		{
			this.data = data;
		}

		@Override
		public int numDimensions()
		{
			return n;
		}

		@Override
		public RealRandomAccess< UnsignedShortType > realRandomAccess()
		{
			final Ellipsoid ellipsoid = data.getEllipsoid();
			return new Access( ellipsoid );
		}

		@Override
		public RealRandomAccess< UnsignedShortType > realRandomAccess( final RealInterval interval )
		{
			final Ellipsoid ellipsoid = data.getEllipsoid();
			return new Access( ellipsoid );
		}

		public void setMinProjectionDistance( final double minProjectionDistance )
		{
			this.minProjectionDistance = minProjectionDistance;
		}

		public void setMaxProjectionDistance( final double maxProjectionDistance )
		{
			this.maxProjectionDistance = maxProjectionDistance;
		}

		public class Access extends RealPoint implements RealRandomAccess< UnsignedShortType >
		{
			private final UnsignedShortType type;

			private final Ellipsoid ellipsoid;

			public Access( final Ellipsoid ellipsoid )
			{
				super( EllipsoidDistanceRealRandomAccessible.this.n );
				this.ellipsoid = ellipsoid;
				type = new UnsignedShortType();
				type.setZero();
			}

			protected Access( final Access a )
			{
				super( a );
				type = new UnsignedShortType();
				type.setZero();
				ellipsoid = a.ellipsoid;
			}

			@Override
			public UnsignedShortType get()
			{
				if ( ellipsoid != null )
				{
					final Result result = DistPointHyperEllipsoid.distPointHyperEllipsoid( this, ellipsoid );
					final double dist = ellipsoid.contains( this ) ? -result.distance : result.distance;
					if ( dist >= minProjectionDistance && dist <= maxProjectionDistance )
						type.set( 1000 );
					else
						type.set( 0 );
				}
				return type;
			}

			@Override
			public Access copy()
			{
				return new Access( this );
			}

			@Override
			public Access copyRealRandomAccess()
			{
				return copy();
			}
		}
	}
}
