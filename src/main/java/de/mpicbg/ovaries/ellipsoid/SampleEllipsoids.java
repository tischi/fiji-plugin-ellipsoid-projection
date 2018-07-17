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
package de.mpicbg.ovaries.ellipsoid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.imglib2.RealLocalizable;

import de.mpicbg.ovaries.ellipsoid.DistPointHyperEllipsoid.Result;

public class SampleEllipsoids
{
	public static Ellipsoid sample( final List< ? extends RealLocalizable > points )
	{
		final int numSamples = 10000;
		final double outsideCutoffDistance = 5.0;
		final double insideCutoffDistance = 5.0;
		return sample( points, numSamples, outsideCutoffDistance, insideCutoffDistance );
	}

	public static Ellipsoid sample(
			final List< ? extends RealLocalizable > points,
			final int numSamples,
			final double outsideCutoffDistance,
			final double insideCutoffDistance )
	{
		final int numPointsPerSample = 9;

		final Random rand = new Random( System.currentTimeMillis() );
		final ArrayList< Integer > indices = new ArrayList< Integer >();
		final double[][] coordinates = new double[ numPointsPerSample ][ 3 ];

		Ellipsoid bestEllipsoid = null;
		double bestCost = Double.POSITIVE_INFINITY;
		final Cost costFunction = new AbsoluteDistanceCost( outsideCutoffDistance, insideCutoffDistance );

		for ( int sample = 0; sample < numSamples; ++sample )
		{
			try
			{
				indices.clear();
				for ( int s = 0; s < numPointsPerSample; ++s )
				{
					int i = rand.nextInt( points.size() );
					while ( indices.contains( i ) )
						i = rand.nextInt( points.size() );
					indices.add( i );
					points.get( i ).localize( coordinates[ s ] );
				}
				final Ellipsoid ellipsoid = de.mpicbg.ovaries.ellipsoid.FitEllipsoid.yuryPetrov( coordinates );

				final double cost = costFunction.compute( ellipsoid, points );
				if ( cost < bestCost )
				{
					bestCost = cost;
					bestEllipsoid = ellipsoid;
				}
			}
			catch ( final IllegalArgumentException e )
			{
				e.printStackTrace();
				System.out.println( "oops" );
			}
			catch ( final RuntimeException e )
			{
				System.out.println( "psd" );
			}
		}

		final Ellipsoid refined = fitToInliers( bestEllipsoid, points, outsideCutoffDistance, insideCutoffDistance );
		if ( refined == null )
		{
			System.err.println( "refined ellipsoid == null! This shouldn't happen!");
			return bestEllipsoid;
		}
		return refined;
	}

	public static Ellipsoid fitToInliers(
			final Ellipsoid guess,
			final List< ? extends RealLocalizable > points,
			final double outsideCutoffDistance,
			final double insideCutoffDistance )
	{
		final ArrayList< RealLocalizable > inliers = new ArrayList< RealLocalizable >();
		for ( final RealLocalizable point : points )
		{
			final Result result = DistPointHyperEllipsoid.distPointHyperEllipsoid( point, guess );
			final double d = result.distance;
			final boolean inside = guess.contains( point );
			if ( ( inside && d <= insideCutoffDistance ) || ( !inside && d <= outsideCutoffDistance ) )
				inliers.add( point );
		}

		final double[][] coordinates = new double[ inliers.size() ][ 3 ];
		for ( int i = 0; i < inliers.size(); ++i )
			inliers.get( i ).localize( coordinates[ i ] );

		final Ellipsoid ellipsoid = de.mpicbg.ovaries.ellipsoid.FitEllipsoid.yuryPetrov( coordinates );
		return ellipsoid;
	}

	static interface Cost
	{
		double compute( final Ellipsoid ellipsoid, final List< ? extends RealLocalizable > points );
	}

	static class AbsoluteDistanceCost implements Cost
	{
		private final double outsideCutoff;
		private final double insideCutoff;

		public AbsoluteDistanceCost( final double outsideCutoffDistance, final double insideCutoffDistance )
		{
			outsideCutoff = outsideCutoffDistance;
			insideCutoff = insideCutoffDistance;
		}

		@Override
		public double compute( final Ellipsoid ellipsoid, final List< ? extends RealLocalizable > points )
		{
			double cost = 0;
			for ( final RealLocalizable point : points )
			{
				final Result result = DistPointHyperEllipsoid.distPointHyperEllipsoid( point, ellipsoid );
				final double d = result.distance;
				if ( ellipsoid.contains( point ) )
					cost += Math.min( d, insideCutoff );
				else
					cost += Math.min( d, outsideCutoff );
			}
			return cost;
		}

	}

	static class SquaredDistanceCost implements Cost
	{
		private final double outsideCutoff;
		private final double insideCutoff;

		public SquaredDistanceCost( final double outsideCutoffDistance, final double insideCutoffDistance )
		{
			outsideCutoff = outsideCutoffDistance * outsideCutoffDistance;
			insideCutoff = insideCutoffDistance * insideCutoffDistance;
		}

		@Override
		public double compute( final Ellipsoid ellipsoid, final List< ? extends RealLocalizable > points )
		{
			double cost = 0;
			for ( final RealLocalizable point : points )
			{
				final Result result = DistPointHyperEllipsoid.distPointHyperEllipsoid( point, ellipsoid );
				final double d = result.distance * result.distance;
				if ( ellipsoid.contains( point ) )
					cost += Math.min( d, insideCutoff );
				else
					cost += Math.min( d, outsideCutoff );
			}
			return cost;
		}

	}
}
