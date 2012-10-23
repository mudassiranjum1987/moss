/*
 *	UCat.java -- concatanate files
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

public class UCat implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int x, i;
		
		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": missing argument, --help for help.\n");
			MPosixIf.exit (1);
		}
		for (i = 1; i < argv.length; i++) {
			if (argv[i].equals ("--help") || argv[i].equals ("-h")) {
				String help[] = {"Usage: " + argv[0] + " [options] <file> [file ...]\n",
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

		/* do file reads */
		{
			int fd;
			int buflen = 1024;
			byte buf[] = new byte[buflen];

			for (; i < argv.length; i++) {
				fd = MPosixIf.open (argv[i], MFileOps.OPEN_READ);
				if (fd < 0) {
					MPosixIf.writestring (MPosixIf.STDERR, argv[0] +
							": failed to open " + argv[i] + " for reading: " +
							MStdLib.strerror (fd) + "\n");
				} else {
					int count;

					do {
						count = MPosixIf.read (fd, buf, buflen);
						if (count < 0) {
							MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": failed to read from " + argv[i] + ": " + MStdLib.strerror (count) + "\n");
							MPosixIf.exit (0);
						} else if (count == 0) {
							/* end of file */
						} else {
							int r = 0;

							while (r < count) {
								int v;

								v = MPosixIf.write (MPosixIf.STDOUT, buf, count - r);
								if (v < 0) {
									MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": failed to write to standard output: " + MStdLib.strerror (v) + "\n");
									MPosixIf.exit (1);
								}

								r += v;
								if (r < count) {
									/* advance buffer */
									System.arraycopy (buf, v, buf, 0, count - r);
								}
							}
						}
					} while (count > 0);
								
					MPosixIf.close (fd);
				}
			}
		}

		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDERR, "UCat signalled with " + signo + "!\n");
	}

}

