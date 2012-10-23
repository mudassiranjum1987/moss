/*
 *	UBusyLoop.java -- busy looping test program for MOSS
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
 * this class creates several instances of itself at different priorities, that
 * loop a few (10) thousand times printing out every 2000 loops.  If the priority
 * mechanism works, the higher-priority processes should complete before the
 * lower-priority ones even print out their first line.
 */

public class UBusyLoop implements MUserProcess
{
	/**
	 * program entry-point.
	 *
	 * @param argv command-line arguments
	 * @param envp process environment
	 *
	 * @return 0 on success, or non-zero on failure
	 */
	public int main (String argv[], MEnv envp)
	{
		int i, x, mypri;

		if (argv.length == 2) {
			mypri = 0;
			try {
				mypri = Integer.parseInt (argv[1]);
			} catch (NumberFormatException e) {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " (" + MPosixIf.getpid() + "): must supply priority argument when invoked this way!\n");
				MPosixIf.exit (0);
			}

			MPosixIf.reschedule ();
			/* set priority and do loops */
			MPosixIf.setpriority (mypri);

			for (x=0; x<5; x++) {
				for (i=0; i<2000; i++) {
					MPosixIf.reschedule ();
				}
				if (x < 4) {
					MPosixIf.writestring (MPosixIf.STDOUT, "PID " + MPosixIf.getpid() + " at " + x + "\n");
				}
			}
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " (" + MPosixIf.getpid() + "): I'm done, exiting!\n");
		} else {
			/* start processes that will actually do stuff */
			for (i=0; i<4; i++) {
				mypri = (i * 4) + 1;

				x = MPosixIf.forkexecc (argv[0], new String[] {argv[0], "" + mypri});
				if (x < 0) {
					MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " (" + MPosixIf.getpid() + "): failed to start instance of self: " + MStdLib.strerror (x) + "\n");
					MPosixIf.exit (1);
				}
			}

			/* now wait for them (this is nicer than just exiting
			 * and letting the init-task scoop them up)
			 */

			for (x=0; x<4; ) {
				int ra[];

				ra = MPosixIf.wait (false);
				if (ra != null) {
					x += (ra.length >> 1);
				}
			}
		}

		return 0;
	}


	/**
	 * signal handler
	 *
	 * @param signo signal number
	 * @param sigdata signal specific data
	 */
	public void signal (int signo, Object sigdata)
	{
		return;
	}

}


