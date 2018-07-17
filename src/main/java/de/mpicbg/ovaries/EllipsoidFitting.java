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

import static de.mpicbg.ovaries.ellipsoid.FitEllipsoid.yuryPetrov;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.RealPoint;

import de.mpicbg.ovaries.ellipsoid.Ellipsoid;

public class EllipsoidFitting
{
	public static void main( final String[] args )
	{
		final Ellipsoid ellipsoid = getDefaultEllipsoid();
		System.out.println( ellipsoid );
	}

	public static Ellipsoid getDefaultEllipsoid()
	{
		final ArrayList< RealPoint > detections = loadDetections( "/Users/pietzsch/Desktop/detections.txt" );
		final double[][] coordinates = new double[ detections.size() ][ 3 ];
		for ( int i = 0; i < detections.size(); ++i )
			for ( int d = 0; d < 3; ++d )
				coordinates[ i ][ d ] = detections.get( i ).getDoublePosition( d );
		return yuryPetrov( coordinates );
	}

	private static ArrayList< RealPoint > loadDetections( final String fn )
	{
		final ArrayList< RealPoint > detections = new ArrayList< RealPoint >();
		try
		{
			final BufferedReader r = new BufferedReader( new FileReader( fn ) );
			String line = r.readLine();
			while ( line != null )
			{
				final String[] split = line.split( " " );
				detections.add( new RealPoint(
						Double.parseDouble( split[ 0 ] ),
						Double.parseDouble( split[ 1 ] ),
						Double.parseDouble( split[ 2 ] ) ) );
				line = r.readLine();
			}
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		return detections;
	}
}
