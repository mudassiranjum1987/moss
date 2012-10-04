/*
 *	UKeyTest.java -- test program to print out things received from the keyboard
 *	Copyright (C) 2005 Fred Barnes  <frmb@kent.ac.uk>
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
 * this class provides a utility that simply prints out the various characters
 * typed into it.  The intended purpose is to see what characters certain keys
 * generate (e.g. up-arrow).  Exit the utility by pressing return twice.
 */

public class UKeyTest implements MUserProcess
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
		String hexbuf = "0123456789ABCDEF";
		boolean running = true;
		char lastch = 'x';

		MPosixIf.writestring (MPosixIf.STDOUT, "Key-test program, press return twice to exit.\n");
		MPosixIf.writestring (MPosixIf.STDOUT, "setting non-blocking option on input..\n");
		{
			int v, flags;

			flags = MPosixIf.fcntl (MPosixIf.STDIN, MFileOps.F_GETFL, 0);
			if (flags < 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": failed to get input flags: " + MStdLib.strerror (flags) + ", exiting\n");
				MPosixIf.exit (1);
			}
			v = MPosixIf.fcntl (MPosixIf.STDIN, MFileOps.F_SETFL, flags | MFileOps.O_NONBLOCK);
			if (v < 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": failed to set input flags: " + MStdLib.strerror (v) + ", continuing anyway\n");
			}
		}
		while (running) {
			byte buf[] = new byte[2];
			int v;

			v = MPosixIf.read (MPosixIf.STDIN, buf, 1);
			if (v == -MSystem.EINTR) {
				MPosixIf.writestring (MPosixIf.STDOUT, "**INTERRUPTED**\n");
			} else if (v <= 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": unexpected error " + MStdLib.strerror (v) + ", exiting.\n");
				MPosixIf.exit (0);
			} else if (v == 1) {
				int hval = ((buf[0] >> 4) & 0x0f);
				int lval = (buf[0] & 0x0f);

				MPosixIf.writestring (MPosixIf.STDOUT, "byte: 0x" + hexbuf.charAt (hval) + hexbuf.charAt (lval) + "\n");
				if (((char)(buf[0]) == '\n') && (lastch == '\n')) {
					running = false;
				}
				lastch = (char)(buf[0]);
			}
		}

		return 0;
	}


	/**
	 * signal handler
	 *
	 * @param signo signal number
	 * @param sigdata signal data
	 */
	public void signal (int signo, Object sigdata)
	{
		return;
	}
}


