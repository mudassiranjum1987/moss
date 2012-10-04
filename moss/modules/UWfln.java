/*
 *	UWfln.java -- write file line
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
import moss.fs.MFileOps;


public class UWfln implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int x, i;
		int fd;
		
		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": missing argument, --help for help.\n");
			MPosixIf.exit (1);
		}
		for (i = 1; i < argv.length; i++) {
			if (argv[i].equals ("--help") || argv[i].equals ("-h")) {
				String help[] = {"Usage: " + argv[0] + " [options] <file> <string> [string ...]\n",
						"where [options] are:\n",
						"    -h | --help    shows this help\n"};

				for (int j = 0; j < help.length; j++) {
					MPosixIf.writestring (MPosixIf.STDOUT, help[j]);
				}
				MPosixIf.exit (0);
			} else if (argv[i].charAt(0) == '-') {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": unrecognised option: " + argv[i] + "\n");
				MPosixIf.exit (1);
			} else {
				break;		/* for() */
			}
		}
		if (i == argv.length) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": <file> argument required.\n");
			MPosixIf.exit (1);
		}

		/* open file for writing (create if it doesn't exist already) */
		fd = MPosixIf.open (argv[i], MFileOps.OPEN_WRITE | MFileOps.OPEN_CREAT);

		if (fd < 0) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": failed to open " + argv[i] + " for writing: " + MStdLib.strerror (fd) + "\n");
			MPosixIf.exit (1);
		}

		/* otherwise write stuff to the file */
		for (i++; i<argv.length; i++) {
			byte data[] = argv[i].getBytes();
			int r;

			r = MPosixIf.write (fd, data, data.length);
			if (r < 0) {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": failed to write: " + MStdLib.strerror (r) + "\n");
				MPosixIf.exit (1);
			} else if (r < data.length) {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": wrote " + r + " of " + data.length + " bytes\n");
			}
		}

		MPosixIf.close (fd);

		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDERR, "UWfln signalled with " + signo + "!\n");
	}

}

