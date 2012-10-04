/*
 *	UCopy.java -- file-copy utility for MOSS (incomplete)
 *	Copyright (C) 2005   **INSERT YOUR NAME AND EMAIL HERE!**
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 2 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program; if not, write to the Free Software
 *	Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package moss.modules;

import java.util.*;
import moss.user.*;
import moss.fs.MFileOps;


/**
 * this class provides a basic file-copy utility for MOSS.
 * It should have the following usage/operation:
 *
 * /bin/copy [options] SOURCE DEST
 *
 * Where SOURCE is the file to copy from, and DEST is the file or directory
 * to copy to.  If a directory is given, a file with the same name as that in
 * SOURCE should be created in the DEST directory.
 *
 * Options include:
 *    -i      to be interactive: prompt the user if the destination file already exists
 *    -a      to append to the destination file, rather than truncate/overwrite it
 *    --help  to produce some on-line help
 */

public class UCopy implements MUserProcess
{
	/**
	 * process entry-point.
	 *
	 * @param argv array of command-line arguments, including program name
	 * @param envp process environment
	 *
	 * @return 0 on success, or 1 on error
	 */
	public int main (String argv[], MEnv envp)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "hello world from " + argv[0] + "! :)\n");

		return 0;
	}


	/**
	 * signal handler.  You can safely ignore this.
	 *
	 * @param signo signal number
	 * @param sigdata signal data
	 */
	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDERR, "UCopy signalled with signal " + signo + "!  exiting..\n");
		MPosixIf.exit (1);
	}
}


