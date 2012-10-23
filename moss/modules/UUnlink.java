/*
 *	UUnlink.java -- unlink utility for MOSS
 *	Copyright (C) 2005 Fred Barnes <frmb@kent.ac.uk>
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

import moss.user.*;


/**
 * this class provides an "unlink" utility for MOSS.  When used on files,
 * unlink will remove them;  when used on a directory, unlink will remove
 * the directory only if it not empty (except for the special . and .. entries)
 */

public class UUnlink implements MUserProcess
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
		int i;

		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDOUT, "Usage:\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "    " + argv[0] + " <path>\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "where <path> is some file/directory to unlink (remove)\n");
			return 1;
		}

		for (i=1; i<argv.length; i++) {
			int v;

			v = MPosixIf.unlink (argv[i]);
			if (v < 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": failed to unlink " + argv[i] + ": " + MStdLib.strerror (v) + "\n");
				return 1;
			}
		}

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
		return;
	}
}


