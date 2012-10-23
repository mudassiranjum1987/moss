/*
 *	UMkdir.java -- make directory utility
 *	Copyright (C) 2004 Fred Barnes <frmb@kent.ac.uk>
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


public class UMkdir implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		String path = null;
		String modestr = null;
		int rmode = 0;
		int x;
		
		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": missing argument, --help for help.\n");
			MPosixIf.exit (1);
		}
		for (int i = 1; i < argv.length; i++) {
			if (argv[i].equals ("--help") || argv[i].equals ("-h")) {
				String help[] = {"Usage: " + argv[0] + " [options] <path> [mode]\n",
						"where [options] are:\n",
						"    -h | --help    shows this help\n"};

				for (int j = 0; j < help.length; j++) {
					MPosixIf.writestring (MPosixIf.STDOUT, help[j]);
				}
				MPosixIf.exit (0);
			} else if (argv[i].charAt(0) == '-') {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": unrecognised option: " + argv[i] + "\n");
				MPosixIf.exit (1);
			} else if (path == null) {
				path = argv[i];
			} else if (modestr == null) {
				modestr = argv[i];
			} else {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": too many arguments.\n");
				MPosixIf.exit (1);
			}
		}
		if (path == null) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": <path> argument required.\n");
			MPosixIf.exit (1);
		}
		if (modestr == null) {
			rmode = 0755;
		} else {
			try {
				rmode = Integer.parseInt (modestr, 8);
			} catch (NumberFormatException e) {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": bad mode -- should be octal.\n");
				MPosixIf.exit (1);
			}
		}
		x = MPosixIf.mkdir (path, rmode);
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": failed to mkdir(): " + MStdLib.strerror (x) + "\n");
			MPosixIf.exit (0);
		}
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDERR, "UMkdir signalled with " + signo + "!\n");
	}

}

