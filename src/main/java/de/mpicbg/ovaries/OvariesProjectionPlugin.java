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

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bdv.BigDataViewer;
import bdv.ij.util.ProgressWriterIJ;
import bdv.tools.ToggleDialogAction;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import ij.ImageJ;
import ij.Prefs;
import mpicbg.spim.data.SpimDataException;

@Plugin(type = Command.class,
menuPath = "Plugins>BigDataViewer>Ellipsoid Surface Projection")
public class OvariesProjectionPlugin implements Command
{
	static String lastDatasetPath = "";

	@Override
	public void run()
	{
		File file = null;

		if ( Prefs.useJFileChooser )
		{
			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setSelectedFile( new File( lastDatasetPath ) );
			fileChooser.setFileFilter( new FileFilter()
			{
				@Override
				public String getDescription()
				{
					return "xml files";
				}

				@Override
				public boolean accept( final File f )
				{
					if ( f.isDirectory() )
						return true;
					if ( f.isFile() )
					{
				        final String s = f.getName();
				        final int i = s.lastIndexOf('.');
				        if (i > 0 &&  i < s.length() - 1) {
				            final String ext = s.substring(i+1).toLowerCase();
				            return ext.equals( "xml" );
				        }
					}
					return false;
				}
			} );

			final int returnVal = fileChooser.showOpenDialog( null );
			if ( returnVal == JFileChooser.APPROVE_OPTION )
				file = fileChooser.getSelectedFile();
		}
		else // use FileDialog
		{
			final FileDialog fd = new FileDialog( ( Frame ) null, "Open", FileDialog.LOAD );
			fd.setDirectory( new File( lastDatasetPath ).getParent() );
			fd.setFile( new File( lastDatasetPath ).getName() );
			final AtomicBoolean workedWithFilenameFilter = new AtomicBoolean( false );
			fd.setFilenameFilter( new FilenameFilter()
			{
				private boolean firstTime = true;

				@Override
				public boolean accept( final File dir, final String name )
				{
					if ( firstTime )
					{
						workedWithFilenameFilter.set( true );
						firstTime = false;
					}

					final int i = name.lastIndexOf( '.' );
					if ( i > 0 && i < name.length() - 1 )
					{
						final String ext = name.substring( i + 1 ).toLowerCase();
						return ext.equals( "xml" );
					}
					return false;
				}
			} );
			fd.setVisible( true );
			if ( isMac() && !workedWithFilenameFilter.get() )
			{
				fd.setFilenameFilter( null );
				fd.setVisible( true );
			}
			final String filename = fd.getFile();
			if ( filename != null )
			{
				file = new File( fd.getDirectory() + filename );
			}
		}

		if ( file != null )
		{
			try
			{
				lastDatasetPath = file.getAbsolutePath();
				open( file );
			}
			catch ( final SpimDataException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	private void open( final File file ) throws SpimDataException
	{
		final BigDataViewer bdv = BigDataViewer.open( file.getAbsolutePath(), file.getName(), new ProgressWriterIJ(), ViewerOptions.options() );
		final ViewerFrame viewerFrame = bdv.getViewerFrame();
		final ViewerPanel viewer = bdv.getViewer();
		final SetupAssignments setupAssignments = bdv.getSetupAssignments();

		// =============== the dialog =====================================

		final OvariesProjectionDialog ovariesDialog = new OvariesProjectionDialog( viewerFrame, viewer, setupAssignments );

		// =============== install shortcut O for dialog ==================

		final Actions actions = new Actions( new InputTriggerConfig() );
		actions.install( viewerFrame.getKeybindings(), "ovaries" );
		final ToggleDialogAction ovariesDialogAction = new ToggleDialogAction( "ovaries projection", ovariesDialog );
		actions.namedAction( ovariesDialogAction, "B" );

		final JMenuBar menuBar = viewerFrame.getJMenuBar();
		for ( int i = 0; i < menuBar.getMenuCount(); ++i )
		{
			final JMenu menu = menuBar.getMenu( i );
			if ( "Tools".equals( menu.getText() ) )
			{
				final JMenuItem miOvaries = new JMenuItem( ovariesDialogAction );
				miOvaries.setText( "Ellipsoid Surface Projection" );
				menu.add( miOvaries );
			}
		}

		ovariesDialog.setVisible( true );
	}

	private boolean isMac()
	{
		final String OS = System.getProperty( "os.name", "generic" ).toLowerCase( Locale.ENGLISH );
		return ( OS.indexOf( "mac" ) >= 0 ) || ( OS.indexOf( "darwin" ) >= 0 );
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new OvariesProjectionPlugin().run();
	}
}
